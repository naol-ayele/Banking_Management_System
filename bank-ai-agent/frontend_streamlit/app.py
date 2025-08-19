import os
import requests
import streamlit as st
import google.generativeai as genai
import matplotlib.pyplot as plt

# --- Configuration and Setup ---
BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080")

# --- NEW: Gemini API Configuration ---
# Load API key from Streamlit secrets
try:
    genai.configure(api_key=st.secrets["GOOGLE_API_KEY"])
    GEMINI_AVAILABLE = True
except (FileNotFoundError, KeyError):
    GEMINI_AVAILABLE = False

# --- NEW: Persona for the AI Agent ---
# This instruction guides the Gemini model's behavior for general queries.
AGENT_PERSONA = """
You are a helpful and friendly AI Banking Agent. Your primary goal is to assist users with their general banking questions. 
You must be professional, clear, and prioritize user safety in your communication.

IMPORTANT RULES:
1.  **NEVER ask for or store Personal Identifiable Information (PII)** like passwords, full account numbers, social security numbers, or addresses.
2.  Your role is to answer **general knowledge questions** about banking, finance, and the services offered.
3.  If a user asks to perform an action on their account (e.g., "check my balance," "show my transaction history," "analyze my spending," "can I get a loan?"), you MUST NOT attempt to do it yourself. Instead, you must inform the user that this is a secure action and they need to use the app's specific features (like the 'Analytics Dashboard') to proceed.
"""

# Initialize the Gemini model
if GEMINI_AVAILABLE:
    model = genai.GenerativeModel(
        model_name="gemini-1.5-flash",
        system_instruction=AGENT_PERSONA
    )

st.set_page_config(page_title="AI Banking Agent", page_icon="💳", layout="wide")
st.title("💬 AI Banking Agent")


# --- HELPER FUNCTION: To decide if a query is general or account-specific ---
def is_account_specific(query: str) -> bool:
    """
    Simple keyword-based router to check if the query requires backend access.
    """
    query = query.lower()
    # Keywords that indicate a need for personal data from the backend
    action_keywords = [
        "my account", "my balance", "financial summary", "spending", 
        "transactions", "loan eligibility", "my savings", "income",
        "check", "analyze", "get", "show me"
    ]
    return any(keyword in query for keyword in action_keywords)


# --- 1. Modern Chat Interface ---
with st.container(border=True):
    st.subheader("Chat with your AI Assistant")

    if "messages" not in st.session_state:
        st.session_state.messages = []

    for message in st.session_state.messages:
        with st.chat_message(message["role"]):
            st.markdown(message["content"])

    if prompt := st.chat_input("Ask a question about your finances..."):
        st.session_state.messages.append({"role": "user", "content": prompt})
        with st.chat_message("user"):
            st.markdown(prompt)

        with st.chat_message("assistant"):
            reply = "" # Initialize reply variable
            # --- MODIFIED: Main chat logic with routing ---
            if is_account_specific(prompt):
                # If it's an account action, call the backend
                reply_placeholder = st.empty()
                reply_placeholder.markdown("Connecting to the secure backend to access your account...")
                try:
                    res = requests.post(f"{BACKEND_URL}/chat", json={"message": prompt})
                    res.raise_for_status()
                    reply = res.json().get("response", "Sorry, I couldn't get a response from the backend.")
                    reply_placeholder.markdown(reply)
                except Exception as e:
                    # Provide a much clearer error message to the user
                    reply = (
                        "**Could not connect to the banking service.** "
                        "This action requires a secure connection to the backend, which appears to be offline. "
                        "Please ensure the service is running to perform account-specific actions."
                    )
                    st.error(reply)
            else:
                # --- NEW: If it's a general question, call Gemini directly ---
                if not GEMINI_AVAILABLE:
                    reply = "Gemini API key is not configured. Please add it to your Streamlit secrets to enable general chat."
                    st.error(reply)
                else:
                    with st.spinner("Thinking..."):
                        try:
                            # Start a chat session with Gemini and send the prompt
                            chat = model.start_chat()
                            response_stream = chat.send_message(prompt, stream=True)
                            
                            # Stream the response to the UI for the "live typing" effect
                            # and capture the final response object once it's done.
                            final_response_object = st.write_stream(response_stream)

                            # ★★★ FIX IS HERE ★★★
                            # After streaming, extract the plain text from the final response object.
                            try:
                                reply = final_response_object.candidates[0].content.parts[0].text
                            except (AttributeError, IndexError):
                                # Provide a fallback if the response structure is unexpected
                                reply = "There was an issue generating the response."
                                
                        except Exception as e:
                            reply = f"An error occurred with the AI model: {e}"
                            st.error(reply)
        
        # This now appends the clean text for both backend and Gemini responses
        st.session_state.messages.append({"role": "assistant", "content": reply})

st.divider()

# --- 2. Analytics Section (No changes needed here) ---
st.subheader("📊 Analytics Dashboard")

user_id = st.number_input(
    "Select User ID to analyze", 
    min_value=1, 
    value=1, 
    step=1,
    help="This User ID will be used for all analytics tabs below."
)

tab1, tab2 = st.tabs(["💰 Financial Summary", "🏦 Loan Eligibility"])

with tab1:
    st.header("Financial Summary")
    if st.button("Analyze Financial Summary", type="primary"):
        with st.spinner("Fetching summary..."):
            try:
                r = requests.get(f"{BACKEND_URL}/analytics/financial-summary", params={"user_id": user_id})
                r.raise_for_status()
                data = r.json()
                st.write("#### Key Monthly Metrics")
                col1, col2, col3 = st.columns(3)
                col1.metric("Total Income", f"${data['income_monthly']:,.2f}", delta_color="off")
                col2.metric("Total Spend", f"${data['spend_monthly']:,.2f}", delta_color="off")
                col3.metric("Savings Rate", f"{data['savings_rate']*100:.1f}%", help="Percentage of income saved per month.")
                st.divider()
                with st.expander("View Top Spending Categories"):
                    cats = data["top_categories"]
                    fig = plt.figure(figsize=(10, 4))
                    plt.bar(list(cats.keys()), list(cats.values()), color='#2a9d8f')
                    plt.title("Average Monthly Spend by Category")
                    plt.ylabel("Amount ($)")
                    plt.xticks(rotation=45, ha='right')
                    st.pyplot(fig)
            except Exception as e:
                st.error(f"Failed to retrieve summary: {e}")

with tab2:
    st.header("Loan Eligibility Check")
    desired = st.number_input("Desired Loan Amount ($)", min_value=0.0, value=10000.0, step=1000.0)
    
    if st.button("Check Loan Eligibility", type="primary"):
        with st.spinner("Checking eligibility..."):
            try:
                params = {"user_id": user_id}
                if desired > 0:
                    params["desired_amount"] = desired
                r = requests.get(f"{BACKEND_URL}/analytics/loan-eligibility", params=params)
                r.raise_for_status()
                data = r.json()
                if data["eligible"]:
                    st.success(f"✅ Congratulations! This user is eligible for a loan.")
                else:
                    st.warning(f"❌ Based on our analysis, this user is not currently eligible.")
                colA, colB = st.columns(2)
                colA.metric("Estimated Max Loan Amount", f"${data['max_amount']:,.2f}")
                colB.metric("Estimated APR", f"{data['apr_percent']}%")
                if data.get("reasons"):
                    with st.container(border=True):
                        st.write("**Factors influencing this decision:**")
                        for reason in data["reasons"]:
                            st.write(f"- {reason}")
            except Exception as e:
                st.error(f"Failed to check eligibility: {e}")
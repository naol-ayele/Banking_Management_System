import streamlit as st
import requests
import pandas as pd

API = "http://localhost:8000"

st.set_page_config(page_title="AI Banking Assistant", page_icon="💳", layout="wide")
st.title("AI Banking Assistant")

if "token" not in st.session_state:
    st.session_state.token = None
    st.session_state.role = None
    st.session_state.user = None

def do_login():
    st.subheader("🔐 Login")
    with st.form("login"):
        username = st.text_input("Username")
        password = st.text_input("Password", type="password")
        role = st.selectbox("Role", ["customer", "staff", "admin"])
        s = st.form_submit_button("Login")
        if s:
            r = requests.post(f"{API}/auth/signup", json={"username": username, "password": password, "role": role})
            if r.status_code not in (200, 400):
                st.error(r.text); return
            if r.status_code == 400:
                r = requests.post(f"{API}/auth/login", json={"username": username, "password": password})
            if r.status_code == 200:
                tok = r.json()["access_token"]
                st.session_state.token = tok
                st.session_state.role = role
                st.session_state.user = username
                st.success("Logged in!")
            else:
                st.error("Invalid credentials.")

if not st.session_state.token:
    do_login()
    st.stop()

st.sidebar.success(f"Logged in as {st.session_state.user} ({st.session_state.role})")

st.subheader("💬 Assistant")
user_msg = st.text_input("Ask a question (account or policy)")
if st.button("Send") and user_msg:
    headers = {"Authorization": f"Bearer {st.session_state.token}"}
    r = requests.post(f"{API}/chat/", json={"message": user_msg}, headers=headers)
    if r.status_code == 200:
        st.write("**Assistant:**")
        st.write(r.json()["reply"])
    else:
        st.error("Chat failed")

st.subheader("📊 Analytics: Default Risk (Logistic Regression)")
if st.button("Run demo training"):
    headers = {"Authorization": f"Bearer {st.session_state.token}"}
    r = requests.get(f"{API}/analytics/default-risk", headers=headers)
    if r.status_code == 200:
        data = r.json()
        st.write(data["info"])
        st.write("Coefficients:", data["coefficients"])
        st.write("Intercept:", data["intercept"])
    else:
        st.error(r.text)

st.subheader("📈 Transactions Summary Chart")
with st.expander("Generate from sample transactions"):
    sample = [{"date": "2025-08-01", "amount": 200, "type": "credit"},
              {"date": "2025-08-02", "amount": -50, "type": "debit"},
              {"date": "2025-08-02", "amount": 100, "type": "credit"},
              {"date": "2025-08-03", "amount": 60, "type": "credit"}]
    st.code(sample, language="python")
    if st.button("Render chart"):
        headers = {"Authorization": f"Bearer {st.session_state.token}"}
        r = requests.post(f"{API}/analytics/summary-chart", json=sample, headers=headers)
        if r.status_code == 200:
            hexpng = r.json()["png_bytes"]
            png_bytes = bytes.fromhex(hexpng)
            st.image(png_bytes)
        else:
            st.error(r.text)

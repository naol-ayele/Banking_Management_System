import os
from dotenv import load_dotenv
load_dotenv()

DATABASE_URL = os.getenv("DATABASE_URL", "postgresql://aiagent:aiagentpass@localhost:5432/aiagentdb")
JWT_SECRET = os.getenv("JWT_SECRET", "supersecret")
JWT_ALGO = "HS256"
CORE_BACKEND_URL = os.getenv("CORE_BACKEND_URL", "").rstrip("/")

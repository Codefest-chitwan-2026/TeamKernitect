import base64
import hashlib
import hmac
import json
import os
import time

import bcrypt


def normalize_email(value: str) -> str:
    return value.strip().lower()


def hash_password(password: str) -> str:
    return bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt()).decode("ascii")


def verify_password(password: str, password_hash: str) -> bool:
    try:
        return bcrypt.checkpw(password.encode("utf-8"), password_hash.encode("ascii"))
    except (ValueError, TypeError):
        return False


def create_access_token(responder_id: str, team_id: str) -> str:
    payload = {"sub": responder_id, "teamId": team_id, "iat": int(time.time())}
    encoded = base64.urlsafe_b64encode(json.dumps(payload, separators=(",", ":")).encode()).rstrip(b"=")
    secret_value = os.getenv("RESPONDER_AUTH_SECRET") or os.getenv("MONGODB_URL")
    if not secret_value:
        raise RuntimeError("RESPONDER_AUTH_SECRET is not configured.")
    secret = secret_value.encode()
    signature = base64.urlsafe_b64encode(hmac.new(secret, encoded, hashlib.sha256).digest()).rstrip(b"=")
    return f"{encoded.decode()}.{signature.decode()}"

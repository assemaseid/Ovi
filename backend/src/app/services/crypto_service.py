import base64
import hmac
import json
import logging
import secrets
from pathlib import Path

from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives.asymmetric.ec import (
    ECDSA,
    EllipticCurvePrivateKey,
    EllipticCurvePublicKey,
)

from src.config import settings

logger = logging.getLogger(__name__)


class CryptoService:
    def __init__(self) -> None:
        self._private_key, self._public_key = self._load_or_generate_keys()
        self.public_key_pem: str = self._public_key.public_bytes(
            serialization.Encoding.PEM,
            serialization.PublicFormat.SubjectPublicKeyInfo,
        ).decode()

    def _load_or_generate_keys(self) -> tuple[EllipticCurvePrivateKey, EllipticCurvePublicKey]:
        # TODO добавить ключи с certs в settings и сгенерить
        private_path = settings.server_private_key_pem
        public_path = settings.server_public_key_pem

        if private_path and Path(private_path).exists():
            logger.info("Loading server key pair from disk")
            with open(private_path, "rb") as f:
                private_key = serialization.load_pem_private_key(f.read(), password=None)
            return private_key, private_key.public_key()

        logger.warning("Server key pair not found — generating ephemeral key pair")
        private_key = ec.generate_private_key(ec.SECP256R1())
        public_key = private_key.public_key()

        if private_path:
            Path(private_path).parent.mkdir(parents=True, exist_ok=True)
            with open(private_path, "wb") as f:
                f.write(
                    private_key.private_bytes(
                        serialization.Encoding.PEM,
                        serialization.PrivateFormat.TraditionalOpenSSL,
                        serialization.NoEncryption(),
                    )
                )
        if public_path:
            with open(public_path, "wb") as f:
                f.write(
                    public_key.public_bytes(
                        serialization.Encoding.PEM,
                        serialization.PublicFormat.SubjectPublicKeyInfo,
                    )
                )

        return private_key, public_key

    def sign(self, data: bytes) -> str:
        sig = self._private_key.sign(data, ECDSA(hashes.SHA256()))
        return base64.b64encode(sig).decode()

    def sign_dict(self, payload: dict) -> str:
        data = json.dumps(payload, sort_keys=True, separators=(",", ":")).encode()
        return self.sign(data)

    def verify_with_pem(self,
                        public_key_pem: str,
                        data: bytes,
                        signature_b64: str
                        ) -> bool:
        try:
            pub_key = serialization.load_pem_public_key(public_key_pem.encode())
            sig = base64.b64decode(signature_b64)
            pub_key.verify(sig, data, ECDSA(hashes.SHA256()))  # type: ignore[arg-type]
            return True
        except (InvalidSignature, Exception) as exc:
            logger.debug("Signature verification failed: %s", exc)
            return False

    def load_public_key(self, pem_or_der: str) -> EllipticCurvePublicKey:
        data = pem_or_der.encode()
        try:
            return serialization.load_pem_public_key(data)
        except Exception:
            raw = base64.b64decode(pem_or_der)
            return serialization.load_der_public_key(raw)

    @staticmethod
    def generate_nonce(length: int = 16) -> str:
        return secrets.token_hex(length)

    @staticmethod
    def constant_time_compare(a: str, b: str) -> bool:
        return hmac.compare_digest(a.encode(), b.encode())


crypto_service = CryptoService()

import re


def validate_firmware_version(v: str) -> str:
    if not re.match(r"^\d+\.\d+\.\d+$", v):
        raise ValueError(f"fw_version must be semver like '1.2.3', got: {v!r}")
    return v


def validate_battery(v: int) -> int:
    if not (0 <= v <= 100):
        raise ValueError("battery_level must be 0–100")
    return v


def validate_public_key(v: str) -> str:
    if len(v) < 20:
        raise ValueError("public_key looks too short to be valid")
    return v

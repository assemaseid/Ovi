from enum import Enum


class DeviceType(str, Enum):
    SMART_LOCK_V1 = "smart_lock_v1"
    SMART_LOCK_V2 = "smart_lock_v2"


class DeviceCapability(str, Enum):
    BLE = "ble"
    WIFI = "wifi"
    KEYPAD = "keypad"
    NFC = "nfc"


class HardwareVersion(str, Enum):
    V1 = "v1.0"
    V2 = "v2.0"
    V3 = "v3.0"


class BleProtocol(str, Enum):
    V1 = "ble_v1"
    V2 = "ble_v2"


class CommandType(str, Enum):
    UNLOCK = "unlock"
    LOCK = "lock"
    GET_STATUS = "get_status"
    REBOOT = "reboot"


class EventType(str, Enum):
    UNLOCK_SUCCESS = "unlock_success"
    UNLOCK_FAILED = "unlock_failed"
    LOCK_SUCCESS = "lock_success"
    TAMPER_DETECTED = "tamper_detected"
    LOW_BATTERY = "low_battery"
    BOOT = "boot"


class UnlockMethod(str, Enum):
    BLE = "ble"
    PIN = "pin"
    NFC = "nfc"
    REMOTE = "remote"

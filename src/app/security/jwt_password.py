import bcrypt

def hash_password(password: str):
    salt = bcrypt.gensalt()
    pwd_bytes: bytes = password.encode()
    hashed = bcrypt.hashpw(pwd_bytes, salt)
    return hashed.decode('utf-8')


def validate_pwd(password: str, hashed_password: str) -> bool:

    hashed_pwd = hashed_password.encode('utf-8')
    return bcrypt.checkpw(
        password=password.encode(),
        hashed_password=hashed_pwd,
    )

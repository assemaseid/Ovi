from typing import Annotated
from annotated_types import MinLen, MaxLen
from pydantic import (
    BaseModel,
    EmailStr,
    ConfigDict,
    )

# for incoming data (registration, creating user)
class UserCreateSchema(BaseModel):
    username: Annotated[str, MinLen(3), MaxLen(20)]
    email: EmailStr
    password: Annotated[str, MinLen(8)]

# for outgoing data (what is returned to the client)
class UserResponseSchema(BaseModel):
    id: int
    username: str
    email: EmailStr | None = None
    is_active: bool

    model_config = ConfigDict(from_attributes=True)



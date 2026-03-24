from typing import Annotated
from uuid import UUID

from annotated_types import MinLen, MaxLen
from pydantic import (
    BaseModel,
    EmailStr,
    ConfigDict,
    )

# for incoming data (registration, creating user)
class UserCreateSchema(BaseModel):
    email: EmailStr
    hashed_password: Annotated[str, MinLen(8)]

# for outgoing data (what is returned to the client)
class UserResponseSchema(BaseModel):
    user_uuid: UUID
    email: EmailStr | None = None
    is_active: bool

    model_config = ConfigDict(from_attributes=True)



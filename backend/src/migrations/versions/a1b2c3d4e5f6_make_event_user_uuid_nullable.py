"""make event user_uuid nullable

Revision ID: a1b2c3d4e5f6
Revises: d837553be788
Create Date: 2026-05-02 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

revision: str = 'a1b2c3d4e5f6'
down_revision: Union[str, Sequence[str], None] = '2310db995664'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.alter_column('events', 'user_uuid',
                    existing_type=sa.dialects.postgresql.UUID(as_uuid=True),
                    nullable=True)


def downgrade() -> None:
    op.alter_column('events', 'user_uuid',
                    existing_type=sa.dialects.postgresql.UUID(as_uuid=True),
                    nullable=False)

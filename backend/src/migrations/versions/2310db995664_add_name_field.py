"""add_name_field

Revision ID: 2310db995664
Revises: 03804516f33e
Create Date: 2026-04-20 12:15:55.764597

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

revision: str = '2310db995664'
down_revision: Union[str, Sequence[str], None] = '03804516f33e'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column('users', sa.Column('name', sa.String(), nullable=False))


def downgrade() -> None:
    op.drop_column('users', 'name')

"""add msg_id to events

Revision ID: 03804516f33e
Revises: 42ee729042ab
Create Date: 2026-04-12 04:02:25.384456

"""
from typing import Sequence, Union

from alembic import op

revision: str = '03804516f33e'
down_revision: Union[str, Sequence[str], None] = '42ee729042ab'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.alter_column('events', 'msq_id', new_column_name='msg_id')


def downgrade() -> None:
    op.alter_column('events', 'msg_id', new_column_name='msq_id')

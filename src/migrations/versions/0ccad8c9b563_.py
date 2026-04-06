"""create device tables

Revision ID: 0ccad8c9b563
Revises: 90147984ee38
Create Date: 2026-04-06 10:18:38.371174

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision: str = '0ccad8c9b563'
down_revision: Union[str, Sequence[str], None] = '90147984ee38'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    op.create_table(
        'devices',
        sa.Column('device_uuid', sa.UUID(), nullable=False),
        sa.Column('hardware_id', sa.String(), nullable=False),
        sa.Column('public_key', sa.Text(), nullable=False),
        sa.Column('user_uuid', sa.UUID(), nullable=False),
        sa.Column('config', postgresql.JSONB(astext_type=sa.Text()), nullable=False),
        sa.Column('firmware_version', sa.String(), nullable=True),
        sa.Column('last_seen', sa.DateTime(timezone=True), nullable=True),
        sa.Column('last_time_sync', sa.DateTime(timezone=True), nullable=True),
        sa.Column('battery_level', sa.Integer(), nullable=True),
        sa.Column('wifi_ssid', sa.String(length=64), nullable=True),
        sa.Column('ip_address', postgresql.INET(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.ForeignKeyConstraint(['user_uuid'], ['users.user_uuid']),
        sa.PrimaryKeyConstraint('device_uuid'),
        sa.UniqueConstraint('hardware_id'),
    )
    op.create_table(
        'grants',
        sa.Column('grant_uuid', sa.UUID(), nullable=False),
        sa.Column('device_uuid', sa.UUID(), nullable=False),
        sa.Column('user_uuid', sa.UUID(), nullable=False),
        sa.Column('permissions', postgresql.JSONB(astext_type=sa.Text()), nullable=False),
        sa.Column('valid_from', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('valid_until', sa.DateTime(timezone=True), nullable=True),
        sa.Column('created_by', sa.UUID(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.ForeignKeyConstraint(['created_by'], ['users.user_uuid']),
        sa.ForeignKeyConstraint(['device_uuid'], ['devices.device_uuid']),
        sa.ForeignKeyConstraint(['user_uuid'], ['users.user_uuid']),
        sa.PrimaryKeyConstraint('grant_uuid'),
    )
    op.create_table(
        'events',
        sa.Column('event_uuid', sa.UUID(), nullable=False),
        sa.Column('msq_id', sa.String(length=64), nullable=False),
        sa.Column('device_uuid', sa.UUID(), nullable=False),
        sa.Column('user_uuid', sa.UUID(), nullable=False),
        sa.Column('event_type', sa.String(length=50), nullable=False),
        sa.Column('event_data', postgresql.JSONB(astext_type=sa.Text()), nullable=False),
        sa.Column('signature', sa.Text(), nullable=True),
        sa.Column('verified', sa.Boolean(), nullable=False),
        sa.Column('source_ip', postgresql.INET(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.ForeignKeyConstraint(['device_uuid'], ['devices.device_uuid']),
        sa.ForeignKeyConstraint(['user_uuid'], ['users.user_uuid']),
        sa.PrimaryKeyConstraint('event_uuid'),
        sa.UniqueConstraint('msq_id'),
    )
    op.create_table(
        'pin_states',
        sa.Column('device_uuid', sa.UUID(), nullable=False),
        sa.Column('rotation_counter', sa.Integer(), nullable=False),
        sa.Column('last_rotation_slot', sa.Integer(), nullable=True),
        sa.Column('last_rotation_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.ForeignKeyConstraint(['device_uuid'], ['devices.device_uuid'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('device_uuid'),
    )
    op.create_table(
        'offline_events_queue',
        sa.Column('queue_uuid', sa.UUID(), nullable=False),
        sa.Column('device_uuid', sa.UUID(), nullable=False),
        sa.Column('event_data', postgresql.JSONB(astext_type=sa.Text()), nullable=False),
        sa.Column('signature', sa.Text(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('synced_at', sa.DateTime(timezone=True), nullable=True),
        sa.ForeignKeyConstraint(['device_uuid'], ['devices.device_uuid']),
        sa.PrimaryKeyConstraint('queue_uuid'),
    )


def downgrade() -> None:
    """Downgrade schema."""
    op.drop_table('offline_events_queue')
    op.drop_table('pin_states')
    op.drop_table('events')
    op.drop_table('grants')
    op.drop_table('devices')

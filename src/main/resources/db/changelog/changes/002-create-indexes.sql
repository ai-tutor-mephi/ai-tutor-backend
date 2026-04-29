-- liquibase formatted sql

--changeset vlmb:002-create-indexes
ALTER TABLE users
    ADD CONSTRAINT uq_users_user_name UNIQUE (user_name);

ALTER TABLE users
    ADD CONSTRAINT uq_users_email UNIQUE (email);

ALTER TABLE user_roles
    ADD CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role);

CREATE INDEX idx_user_roles_role ON user_roles (role);

CREATE UNIQUE INDEX uq_refresh_tokens_token ON refresh_tokens (token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

CREATE UNIQUE INDEX uq_files_storage_file_name ON files (storage_file_name);
CREATE INDEX idx_files_dialog_id ON files (dialog_id);

CREATE INDEX idx_dialogs_user_id_created_at ON dialogs (user_id, created_at DESC);
CREATE INDEX idx_messages_dialog_id_created_at ON messages (dialog_id, created_at DESC);

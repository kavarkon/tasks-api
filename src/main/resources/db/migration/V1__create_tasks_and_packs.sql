CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    CONSTRAINT ck_users_email_canonical CHECK (email = lower(btrim(email))),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE packs (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    user_id INTEGER NOT NULL,
    CONSTRAINT fk_packs_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
);

CREATE TABLE tasks (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    completed BOOLEAN NOT NULL,
    pack_id INTEGER NOT NULL,
    CONSTRAINT fk_tasks_pack
        FOREIGN KEY (pack_id)
        REFERENCES packs (id)
        ON DELETE CASCADE
);

CREATE INDEX ix_packs_user_id_id ON packs (user_id, id);
CREATE INDEX ix_tasks_pack_id_id ON tasks (pack_id, id);

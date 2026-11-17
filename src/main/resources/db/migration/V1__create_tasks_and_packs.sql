CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX uk_users_email ON users (lower(email));

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

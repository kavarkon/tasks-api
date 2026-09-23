CREATE TABLE packs (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL
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

CREATE TABLE IF NOT EXISTS users (
   id               UUID PRIMARY KEY,
   email            VARCHAR(255) NOT NULL UNIQUE,
   full_name        VARCHAR(255) NOT NULL,
   password_hash    VARCHAR(255) NOT NULL,
   created_at       TIMESTAMP NOT NULL,
   role          VARCHAR(20) NOT NULL
);

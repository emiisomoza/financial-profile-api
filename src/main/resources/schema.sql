DROP TABLE IF EXISTS users;

CREATE TABLE users (
   id               UUID PRIMARY KEY,
   email            VARCHAR(255) NOT NULL,
   full_name        VARCHAR(255),
   password_hash    VARCHAR(255) NOT NULL,
   created_at       TIMESTAMP NOT NULL
);

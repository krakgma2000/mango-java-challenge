CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER DATABASE products SET timezone TO 'Europe/Madrid';

CREATE ROLE products_app LOGIN PASSWORD 'products_app';

CREATE TABLE products (
    id          bigserial PRIMARY KEY,
    name        text NOT NULL,
    description text
);

CREATE TABLE prices (
    id         bigserial PRIMARY KEY,
    product_id bigint  NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    value      numeric NOT NULL,
    currency   text    NOT NULL,
    init_date  date    NOT NULL,
    end_date   date,
    CONSTRAINT prices_no_overlap EXCLUDE USING gist (
        product_id WITH =,
        daterange(init_date, end_date, '[]') WITH &&
    )
);

CREATE TABLE app_users (
    id            bigserial PRIMARY KEY,
    username      text NOT NULL UNIQUE,
    password_hash text NOT NULL
);

CREATE TABLE app_user_grants (
    user_id  bigint NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    endpoint text   NOT NULL,
    PRIMARY KEY (user_id, endpoint)
);


GRANT CONNECT ON DATABASE products TO products_app;
GRANT USAGE ON SCHEMA public TO products_app;
GRANT SELECT, INSERT, UPDATE ON products TO products_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON prices TO products_app;
GRANT SELECT ON app_users, app_user_grants TO products_app;
GRANT USAGE ON SEQUENCE products_id_seq, prices_id_seq TO products_app;


INSERT INTO app_users (username, password_hash)
VALUES ('mango_test', '$2a$10$56R7q5NEItf2Fbgvtzz8NOyNtDhGmOJTO51O2e1B6uIqfFHQFvepK')
ON CONFLICT (username) DO NOTHING;

INSERT INTO app_user_grants (user_id, endpoint)
SELECT id, '/products/**' FROM app_users WHERE username = 'mango_test'
ON CONFLICT DO NOTHING;

INSERT INTO products (id, name, description)
VALUES (1, 'Sports Shoes', '2026 Limited Edition'),
       (2, 'Basic Shirt',       'Eco-Friendly')
ON CONFLICT (id) DO NOTHING;

INSERT INTO prices (product_id, value, currency, init_date, end_date)
VALUES (1,  99.99, 'EUR', '2024-01-01', '2024-06-30'),
       (1, 129.99, 'EUR', '2024-07-01', '2024-12-31'),
       (1, 199.99, 'EUR', '2025-01-01', NULL),
       (2,  19.99, 'EUR', '2024-01-01', NULL)
ON CONFLICT DO NOTHING;

SELECT setval('products_id_seq', (SELECT max(id) FROM products));

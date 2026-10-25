\set ON_ERROR_STOP on
BEGIN;
DO $$
BEGIN
    IF current_user <> 'northstar_migrator' THEN
        RAISE EXCEPTION 'Use the Chapter 8 migration identity';
    END IF;
END $$;
LOCK TABLE northstar.product, northstar.category IN ACCESS EXCLUSIVE MODE;
DO $$
BEGIN
    IF (SELECT string_agg(version, ',' ORDER BY installed_rank)
        FROM northstar.flyway_schema_history WHERE success) IS DISTINCT FROM '1,2,2.1'
       OR (SELECT count(*) FROM northstar.flyway_schema_history WHERE NOT success) <> 0
       OR (SELECT count(*) FROM northstar.product) <> 1
       OR NOT EXISTS (SELECT FROM northstar.product
           WHERE product_id = 42 AND name = 'Trail Map Set'
             AND description = 'Water-resistant regional trail maps'
             AND price = 18.50 AND active AND category_id = 1)
       OR (SELECT count(*) FROM northstar.category) <> 1
       OR NOT EXISTS (SELECT FROM northstar.category
                     WHERE category_id = 1 AND name = 'Maps and Navigation') THEN
        RAISE EXCEPTION 'Destination must be a fresh Chapter 8 database with only its unchanged seed rows';
    END IF;
END $$;
SET LOCAL ROLE northstar_owner;
CREATE TEMP TABLE incoming_product (
    product_id bigint PRIMARY KEY CHECK (product_id > 0 AND product_id < 9223372036854775807),
    name varchar(120) NOT NULL,
    description varchar(1000),
    price numeric NOT NULL,
    active boolean NOT NULL
) ON COMMIT DROP;
\copy incoming_product FROM 'products.csv' WITH (FORMAT csv, HEADER MATCH, ENCODING 'UTF8')
INSERT INTO northstar.product(product_id,name,description,price,active,category_id)
SELECT product_id,name,description,price,active,1 FROM incoming_product
ON CONFLICT (product_id) DO UPDATE SET
    name = EXCLUDED.name, description = EXCLUDED.description,
    price = EXCLUDED.price, active = EXCLUDED.active;
-- Source row 42 wins over the disposable destination seed. Other existing
-- destination data cannot reach this statement because the guard rejects it.
SELECT count(*) AS copied_products FROM incoming_product;
SELECT setval('northstar.product_product_id_seq',
              GREATEST(42, (SELECT max(product_id) FROM northstar.product)), true);
COMMIT;

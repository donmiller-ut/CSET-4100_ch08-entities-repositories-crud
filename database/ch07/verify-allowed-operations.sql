\set ON_ERROR_STOP on

BEGIN;

INSERT INTO northstar.product (name, description, price)
VALUES ('Database Verification Product', 'Rolled back after verification', 12.50);

UPDATE northstar.product
SET active = false
WHERE name = 'Database Verification Product';

SELECT product_id, name, price, active
FROM northstar.product
WHERE name = 'Database Verification Product';

DELETE FROM northstar.product
WHERE name = 'Database Verification Product';

ROLLBACK;


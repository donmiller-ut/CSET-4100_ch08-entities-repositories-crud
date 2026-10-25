-- Forward correction. Do not rewrite the applied V1 or V2 files.
SET ROLE northstar_owner;

ALTER TABLE northstar.product
    DROP CONSTRAINT product_price_scale,
    ADD CONSTRAINT product_price_scale CHECK (min_scale(price) <= 2),
    ADD CONSTRAINT product_price_finite CHECK (
        price NOT IN ('NaN'::numeric, 'Infinity'::numeric, '-Infinity'::numeric));

-- V1's default table grant also affected the Category created in V2.
-- Category is read-only for the Chapter 8 runtime.
REVOKE INSERT, UPDATE, DELETE ON northstar.category FROM northstar_app;

RESET ROLE;

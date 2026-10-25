\set ON_ERROR_STOP on
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;
DO $$
BEGIN
    IF current_user <> 'northstar_app'
       OR to_regclass('northstar.flyway_schema_history') IS NOT NULL
       OR EXISTS (SELECT FROM information_schema.columns
                  WHERE table_schema = 'northstar' AND table_name = 'product'
                    AND column_name = 'category_id') THEN
        RAISE EXCEPTION 'Expected the Chapter 7 runtime identity and pre-Flyway Product schema';
    END IF;
END $$;
\copy (SELECT product_id,name,description,price,active FROM northstar.product ORDER BY product_id) TO 'products.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8')
COMMIT;

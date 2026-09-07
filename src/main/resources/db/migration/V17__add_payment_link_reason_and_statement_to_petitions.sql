
ALTER TABLE petitions ALTER COLUMN resolver_admin_id DROP NOT NULL;


ALTER TABLE petitions 
    ADD COLUMN payment_link VARCHAR(500) NOT NULL,
    ADD COLUMN reason VARCHAR(50) NOT NULL,
    ADD COLUMN statement TEXT NOT NULL;

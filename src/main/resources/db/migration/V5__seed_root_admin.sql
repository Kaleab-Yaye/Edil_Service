INSERT INTO accounts (id, email, password_hash, role, is_active, created_at, updated_at)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'admin@edil.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ROOT_ADMIN',
    TRUE,
    NOW(),
    NOW()
);

INSERT INTO admin_profiles (id, account_id, full_name)
VALUES (
    'b0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'Root Administrator'
);

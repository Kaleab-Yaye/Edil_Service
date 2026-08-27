CREATE TABLE creator_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL UNIQUE REFERENCES accounts(id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    channel_link VARCHAR(255) NOT NULL,
    about_section TEXT,
    payout_bank_account VARCHAR(50) NOT NULL,
    onboarding_status VARCHAR(50) NOT NULL DEFAULT 'PENDING'
);

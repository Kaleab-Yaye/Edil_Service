CREATE TABLE creator_onboarding_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL UNIQUE REFERENCES accounts(id) ON DELETE CASCADE,
    submitted_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_onboarding_queue_submitted ON creator_onboarding_queue(submitted_at ASC);

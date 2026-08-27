CREATE TABLE campaigns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    about_campaign TEXT,
    ticket_price DECIMAL(10, 2) NOT NULL,
    target_entries INT NOT NULL,
    joined_users INT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    target_reached_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_campaigns_status ON campaigns(status);
CREATE INDEX idx_campaigns_creator ON campaigns(creator_id);
CREATE INDEX idx_campaigns_end_date ON campaigns(end_date);

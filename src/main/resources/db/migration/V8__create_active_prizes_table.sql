CREATE TABLE active_campaign_prizes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    prize_order INT NOT NULL,
    image_url VARCHAR(500) NOT NULL
);

CREATE INDEX idx_active_prizes_campaign ON active_campaign_prizes(campaign_id);

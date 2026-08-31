CREATE TABLE campaign_participants(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    campaign_id UUID NOT NULL REFERENCES  campaigns(id) ON DELETE CASCADE,
    receipt_hash VARCHAR(225) UNIQUE,
    added_at TIMESTAMP NOT NULL DEFAULT NOW()
);
ALTER TABLE   campaign_participants ADD CONSTRAINT account_id_and_campaign_id_must_be_unique_combo UNIQUE (account_id, campaign_id);
CREATE  INDEX  campaign_participants_account_id_index ON campaign_participants(account_id);
CREATE  INDEX  campaign_participants_campaign_id_index ON campaign_participants(campaign_id);

CREATE TABLE receipts (
    id varchar(25) PRIMARY KEY,
    reference_number VARCHAR(50) NOT NULL UNIQUE
)
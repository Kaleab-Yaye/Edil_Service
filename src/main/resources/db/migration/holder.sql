create table  archived_Campaign_participant(
   id UUID PRIMARY KEY,
   account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
   campaign_id UUID NOT NULL REFERENCES  campaigns(id) ON DELETE CASCADE,
   receipt_hash VARCHAR(225) UNIQUE,
   archived_at TIMESTAMP NOT NULL DEFAULT NOW()
)
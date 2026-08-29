-- Enforce that a creator can only have one active/running/pending campaign at a time
CREATE UNIQUE INDEX idx_unique_active_creator_campaign 
ON campaigns (creator_id) 
WHERE status IN ('PENDING', 'APPROVED');

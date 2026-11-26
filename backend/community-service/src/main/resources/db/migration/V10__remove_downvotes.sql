DELETE FROM votes
WHERE vote_value < 0;

UPDATE posts p
SET vote_score = (
    SELECT COUNT(*)
    FROM votes v
    WHERE v.target_type = 'POST'
      AND v.target_id = p.id
);

UPDATE comments c
SET vote_score = (
    SELECT COUNT(*)
    FROM votes v
    WHERE v.target_type = 'COMMENT'
      AND v.target_id = c.id
);

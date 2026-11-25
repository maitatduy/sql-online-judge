ALTER TABLE submissions ADD COLUMN code_hash BINARY(32) NULL;

UPDATE submissions SET code_hash = UNHEX(SHA2(code, 256));

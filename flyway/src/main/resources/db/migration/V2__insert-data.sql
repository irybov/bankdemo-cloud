BEGIN;

INSERT INTO bankdemo.accounts(is_active, birthday, name, password, phone, email, surname, created_at, roles)
VALUES('1', '2001-01-01', 'Admin', 'superadmin', '0000000000', 'adminov@greenmail.io', 'Adminov', now(), ARRAY['ROLE_ADMIN'])
ON CONFLICT DO NOTHING;
INSERT INTO bankdemo.accounts(is_active, birthday, name, password, phone, email, surname, created_at, roles)
VALUES('1', '1985-08-31', 'Kae', 'supervixen', '1111111111', 'yukawa@greenmail.io', 'Yukawa', now(), ARRAY['ROLE_CLIENT']),
('1', '1974-07-28', 'Hannah', 'bustyblonde', '2222222222', 'waddingham@greenmail.io', 'Waddingham', now(), ARRAY['ROLE_CLIENT']),
('1', '1995-06-13', 'Ella', 'gingerchick', '3333333333', 'hughes@greenmail.io', 'Hughes', now(), ARRAY['ROLE_ADMIN', 'ROLE_CLIENT'])
ON CONFLICT DO NOTHING;

SET @target_id = SELECT id FROM bankdemo.accounts WHERE phone='1111111111';
INSERT INTO bankdemo.bills(is_active, balance, currency, owner, created_at)
VALUES('1', '10.00', 'USD', @target_id, now()), ('1', '10.00', 'SEA', @target_id, now());
SET @idx_array = ARRAY(SELECT id FROM bankdemo.bills WHERE owner = @target_id);
UPDATE bankdemo.accounts SET bills = @idx_array WHERE phone='1111111111';

COMMIT;
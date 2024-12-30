INSERT INTO bankdemo.accounts(is_active, birthday, name, password, phone, email, surname, created_at, roles)
VALUES('1', '2001-01-01', 'Admin', 'superadmin', '0000000000', 'adminov@greenmail.io', 'Adminov', now(), ARRAY['ROLE_ADMIN']);
INSERT INTO bankdemo.accounts(is_active, birthday, name, password, phone, email, surname, created_at, roles)
VALUES('1', '1985-08-31', 'Kae', 'supervixen', '1111111111', 'yukawa@greenmail.io', 'Yukawa', now(), ARRAY['ROLE_CLIENT']),
('1', '1974-07-28', 'Hannah', 'bustyblonde', '2222222222', 'waddingham@greenmail.io', 'Waddingham', now(), ARRAY['ROLE_CLIENT']),
('1', '1995-06-13', 'Ella', 'gingerchick', '3333333333', 'hughes@greenmail.io', 'Hughes', now(), ARRAY['ROLE_ADMIN', 'ROLE_CLIENT'])
ON CONFLICT DO NOTHING;

UPDATE bankdemo.accounts SET bills = ARRAY[1,2] WHERE phone='1111111111';
UPDATE bankdemo.accounts SET bills = ARRAY[3] WHERE phone='3333333333';

--UPDATE bankdemo.accounts SET password = HASH('SHA256', password, 4);
INSERT INTO bankdemo.operations(created_at, action, amount, currency, sender, recipient, bank)
VALUES(now(), 'deposit', '500.00' , 'RUB', '0', '1', 'Demo'),
(now(), 'withdraw', '100.00' , 'RUB', '1', '0', 'Demo'),
(now(), 'transfer', '300.00' , 'RUB', '1', '2', 'Demo'),
(now(), 'deposit', '400.00' , 'RUB', '0', '3', 'Demo'),
(now(), 'withdraw', '700.00' , 'RUB', '3', '0', 'Demo'),
(now(), 'transfer', '800.00' , 'RUB', '2', '3', 'Demo');
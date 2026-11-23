MERGE INTO bankdemo.operations_outbox (id, payload, status, attempts, next_attempt_at)
KEY (id)
VALUES 
    -- 1. Fresh PENDING record due immediately (1 minute ago)
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        '{"1001": 500.00}',
        'PENDING',
        0,
        CURRENT_TIMESTAMP - INTERVAL '1' MINUTE
    ),
    -- 2. PENDING record due immediately (10 seconds ago)
    (
        'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
        '{"1001": -1250.50, "1002": 1250.50}',
        'PENDING',
        2,
        CURRENT_TIMESTAMP - INTERVAL '10' SECOND
    ),
    -- 3. PENDING record in future backoff (+10 minutes)
    (
        'c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a33',
        '{"1003": 99.99}',
        'PENDING',
        1,
        CURRENT_TIMESTAMP + INTERVAL '10' MINUTE
    ),
    -- 4. Already SENT
    (
        'd3eebc99-9c0b-4ef8-bb6d-6bb9bd380a44',
        '{"1001": 10000.00}',
        'SENT',
        1,
        CURRENT_TIMESTAMP - INTERVAL '1' HOUR
    ),
    -- 5. Marked FAILED
    (
        'e4eebc99-9c0b-4ef8-bb6d-6bb9bd380a55',
        '{"999999": -50.00}',
        'FAILED',
        0,
        CURRENT_TIMESTAMP - INTERVAL '30' MINUTE
    );
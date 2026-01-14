INSERT INTO users (id, username, email, password, first_name, last_name, role, enabled, created_at, updated_at) VALUES
    (1, 'admin', 'admin@hotel.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.Mrq4H8P9GvR6z1h9n0nLW1nK4qrVJ1q', 'Admin', 'User', 'ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'john_doe', 'john@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.Mrq4H8P9GvR6z1h9n0nLW1nK4qrVJ1q', 'John', 'Doe', 'USER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'jane_smith', 'jane@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.Mrq4H8P9GvR6z1h9n0nLW1nK4qrVJ1q', 'Jane', 'Smith', 'USER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 'bob_wilson', 'bob@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.Mrq4H8P9GvR6z1h9n0nLW1nK4qrVJ1q', 'Bob', 'Wilson', 'USER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO bookings (id, booking_reference, user_id, room_id, hotel_id, check_in_date, check_out_date, guest_count, total_price, status, special_requests, version, created_at, updated_at) VALUES
    (1, 'BK-ABC12345', 2, 1, 1, DATEADD('DAY', -10, CURRENT_DATE), DATEADD('DAY', -7, CURRENT_DATE), 2, 450.00, 'COMPLETED', NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'BK-DEF67890', 2, 7, 2, DATEADD('DAY', -5, CURRENT_DATE), DATEADD('DAY', -2, CURRENT_DATE), 2, 360.00, 'COMPLETED', 'Late check-out requested', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'BK-GHI11111', 3, 3, 1, DATEADD('DAY', 5, CURRENT_DATE), DATEADD('DAY', 8, CURRENT_DATE), 3, 750.00, 'CONFIRMED', 'Quiet room preferred', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 'BK-JKL22222', 4, 12, 3, DATEADD('DAY', -3, CURRENT_DATE), DATEADD('DAY', 0, CURRENT_DATE), 2, 270.00, 'CANCELLED', NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

ALTER TABLE users ALTER COLUMN id RESTART WITH 100;
ALTER TABLE bookings ALTER COLUMN id RESTART WITH 100;

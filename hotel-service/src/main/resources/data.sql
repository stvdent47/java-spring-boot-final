INSERT INTO hotels (id, name, address, city, country, star_rating, created_at, updated_at) VALUES
    (1, 'Grand Plaza Hotel', '123 Main Street', 'New York', 'USA', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'Seaside Resort', '456 Beach Road', 'Miami', 'USA', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'Mountain View Lodge', '789 Alpine Way', 'Denver', 'USA', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 'City Center Inn', '101 Downtown Ave', 'Chicago', 'USA', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (5, 'Royal Palace Hotel', '202 Luxury Lane', 'Las Vegas', 'USA', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO rooms (id, hotel_id, room_number, room_type, price_per_night, max_occupancy, available, times_booked, version, created_at, updated_at) VALUES
   (1, 1, '101', 'STANDARD', 150.00, 2, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (2, 1, '102', 'STANDARD', 150.00, 2, true, 2, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (3, 1, '201', 'DELUXE', 250.00, 3, true, 5, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (4, 1, '202', 'DELUXE', 250.00, 3, true, 3, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (5, 1, '301', 'SUITE', 450.00, 4, true, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (6, 1, '302', 'SUITE', 450.00, 4, false, 10, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO rooms (id, hotel_id, room_number, room_type, price_per_night, max_occupancy, available, times_booked, version, created_at, updated_at) VALUES
   (7, 2, 'A1', 'STANDARD', 120.00, 2, true, 4, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (8, 2, 'A2', 'STANDARD', 120.00, 2, true, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (9, 2, 'B1', 'DELUXE', 200.00, 3, true, 7, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (10, 2, 'B2', 'DELUXE', 200.00, 3, true, 2, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (11, 2, 'P1', 'SUITE', 350.00, 4, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO rooms (id, hotel_id, room_number, room_type, price_per_night, max_occupancy, available, times_booked, version, created_at, updated_at) VALUES
   (12, 3, '1A', 'STANDARD', 90.00, 2, true, 3, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (13, 3, '1B', 'STANDARD', 90.00, 2, true, 6, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (14, 3, '2A', 'DELUXE', 140.00, 3, true, 2, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (15, 3, '2B', 'DELUXE', 140.00, 3, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO rooms (id, hotel_id, room_number, room_type, price_per_night, max_occupancy, available, times_booked, version, created_at, updated_at) VALUES
   (16, 4, 'R101', 'STANDARD', 85.00, 2, true, 8, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (17, 4, 'R102', 'STANDARD', 85.00, 2, true, 4, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (18, 4, 'R201', 'DELUXE', 130.00, 3, true, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO rooms (id, hotel_id, room_number, room_type, price_per_night, max_occupancy, available, times_booked, version, created_at, updated_at) VALUES
   (19, 5, 'GOLD-1', 'DELUXE', 300.00, 2, true, 5, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (20, 5, 'GOLD-2', 'DELUXE', 300.00, 2, true, 3, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (21, 5, 'PLATINUM-1', 'SUITE', 600.00, 4, true, 2, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (22, 5, 'PLATINUM-2', 'SUITE', 600.00, 4, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (23, 5, 'ROYAL-1', 'PRESIDENTIAL', 1200.00, 6, true, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

ALTER TABLE hotels ALTER COLUMN id RESTART WITH 100;
ALTER TABLE rooms ALTER COLUMN id RESTART WITH 100;

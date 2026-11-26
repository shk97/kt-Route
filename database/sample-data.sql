-- RouteSync - Sample Data
-- Run after schema.sql

USE routesync_db;

-- Roles
INSERT IGNORE INTO roles (name) VALUES ('ROLE_ADMIN'), ('ROLE_PASSENGER');

-- Admin user (password: Admin@1234)
INSERT IGNORE INTO users (full_name, email, phone, password, is_active)
VALUES ('System Administrator', 'admin@ktroute.com', '9000000000',
        '$2a$10$q/Ps0wL4cS5T5KL1X/sKkO1A0b8k5e1CnMHxCFZBOSXqFnLMfUzZW', 1);
-- Note: run the app to auto-seed properly; above hash is for Admin@1234

-- Passenger users (password: Pass@1234)
INSERT IGNORE INTO users (full_name, email, phone, password, is_active) VALUES
('Arjun Sharma',    'arjun@example.com',   '9876543210', '$2a$10$hashed_pass', 1),
('Priya Patel',     'priya@example.com',   '9876543211', '$2a$10$hashed_pass', 1),
('Rahul Kumar',     'rahul@example.com',   '9876543212', '$2a$10$hashed_pass', 1),
('Sunita Singh',    'sunita@example.com',  '9876543213', '$2a$10$hashed_pass', 1),
('Mohan Das',       'mohan@example.com',   '9876543214', '$2a$10$hashed_pass', 1);

-- Stations (is_active=1 so they appear in dropdowns immediately)
INSERT IGNORE INTO stations (name, code, location, city, state, is_active) VALUES
('Chennai Central',          'CHN',  'Park Town, Chennai',        'Chennai',         'Tamil Nadu', 1),
('Coimbatore Bus Terminal',  'CBE',  'Gandhipuram, Coimbatore',   'Coimbatore',      'Tamil Nadu', 1),
('Madurai New Bus Stand',    'MDU',  'New Bus Stand, Madurai',    'Madurai',         'Tamil Nadu', 1),
('Trichy Central Bus Stand', 'TRY',  'Chatram Bus Stand, Trichy', 'Tiruchirappalli', 'Tamil Nadu', 1),
('Salem Bus Terminal',       'SLM',  'New Bus Stand, Salem',      'Salem',           'Tamil Nadu', 1),
('Tirunelveli Bus Stand',    'TNV',  'Tirunelveli Jn, TNV',       'Tirunelveli',     'Tamil Nadu', 1),
('Vellore Bus Stand',        'VLR',  'Katpadi Road, Vellore',     'Vellore',         'Tamil Nadu', 1),
('Bangalore Majestic',       'BLR',  'Majestic, Bangalore',       'Bangalore',       'Karnataka',  1),
('Hyderabad MGBS',           'HYD',  'Imlibun, Hyderabad',        'Hyderabad',       'Telangana',  1),
('Erode Bus Stand',          'ERD',  'EVN Road, Erode',           'Erode',           'Tamil Nadu', 1);

-- Buses
INSERT IGNORE INTO buses (bus_number, registration_no, bus_type, total_seats, status, manufacturer, model, manufacturing_year) VALUES
('KT-001', 'TN01AB1001', 'AC Sleeper',     40, 'ACTIVE', 'Volvo',  'B11R',  2022),
('KT-002', 'TN01AB1002', 'AC Semi-Sleeper',45, 'ACTIVE', 'Volvo',  'B9R',   2021),
('KT-003', 'TN01AB1003', 'Non-AC Seater',  52, 'ACTIVE', 'TATA',   'LP',    2020),
('KT-004', 'TN01AB1004', 'AC Seater',      44, 'ACTIVE', 'Ashok',  'Viking',2022),
('KT-005', 'TN01AB1005', 'Non-AC Seater',  55, 'ACTIVE', 'TATA',   'LP',    2019),
('KT-006', 'TN01AB1006', 'AC Sleeper',     40, 'ACTIVE', 'Scania', 'K410',  2023),
('KT-007', 'TN01AB1007', 'AC Semi-Sleeper',45, 'ACTIVE', 'Volvo',  'B9R',   2022),
('KT-008', 'TN01AB1008', 'Non-AC Seater',  52, 'MAINTENANCE', 'TATA', 'LP', 2018);

-- Drivers (is_active=1)
INSERT IGNORE INTO drivers (full_name, license_no, phone, email, is_active) VALUES
('Rajan Muthu',     'TN01DL001', '9111111101', 'rajan@ktroute.com',  1),
('Selvam Kumar',    'TN01DL002', '9111111102', 'selvam@ktroute.com', 1),
('Balu Krishnan',   'TN01DL003', '9111111103', 'balu@ktroute.com',   1),
('Durai Pandian',   'TN01DL004', '9111111104', 'durai@ktroute.com',  1),
('Elan Murugan',    'TN01DL005', '9111111105', 'elan@ktroute.com',   1),
('Faris Ahmed',     'TN01DL006', '9111111106', 'faris@ktroute.com',  1);

-- Routes (using station IDs: CHN=1, CBE=2, MDU=3, TRY=4, SLM=5, TNV=6, VLR=7, BLR=8, HYD=9, ERD=10)
INSERT IGNORE INTO routes (name, route_code, start_station_id, end_station_id, distance_km, estimated_duration_minutes, status) VALUES
('Chennai - Coimbatore Express',     'RT001', 1, 2,  495, 480, 'ACTIVE'),
('Chennai - Madurai Direct',         'RT002', 1, 3,  461, 420, 'ACTIVE'),
('Chennai - Bangalore Overnight',    'RT003', 1, 8,  348, 360, 'ACTIVE'),
('Coimbatore - Bangalore Express',   'RT004', 2, 8,  362, 330, 'ACTIVE'),
('Chennai - Trichy Express',         'RT005', 1, 4,  320, 300, 'ACTIVE'),
('Chennai - Tirunelveli',            'RT006', 1, 6,  638, 600, 'ACTIVE'),
('Coimbatore - Madurai',             'RT007', 2, 3,  210, 210, 'ACTIVE'),
('Chennai - Hyderabad',              'RT008', 1, 9,  630, 600, 'ACTIVE');

-- Route Stops
INSERT IGNORE INTO route_stops (route_id, station_id, stop_order, arrival_offset_minutes, departure_offset_minutes, distance_from_start_km) VALUES
-- RT001: Chennai - Coimbatore via Salem, Erode
(1, 1,  0,   0,   0,   0),    -- Chennai Central (start)
(1, 5,  1, 120, 125, 160),    -- Salem
(1, 10, 2, 200, 205, 280),    -- Erode
(1, 2,  3, 480, 480, 495),    -- Coimbatore (end)

-- RT002: Chennai - Madurai via Trichy
(2, 1,  0,   0,   0,   0),
(2, 4,  1, 180, 185, 320),
(2, 3,  2, 420, 420, 461),

-- RT003: Chennai - Bangalore via Vellore
(3, 1,  0,   0,   0,   0),
(3, 7,  1,  90,  95, 134),
(3, 8,  2, 360, 360, 348),

-- RT005: Chennai - Trichy
(5, 1, 0,   0,   0,   0),
(5, 4, 1, 300, 300, 320);

-- Trips (dates are relative to NOW() so they stay in the future after any fresh load)
INSERT IGNORE INTO trips (route_id, bus_id, driver_id, departure_time, arrival_time, base_price, status) VALUES
-- RT001 Chennai-Coimbatore (today, today, +1, +2, +3)
(1, 1, 1, CURDATE() + INTERVAL  6 HOUR, CURDATE() + INTERVAL 14 HOUR, 850.00, 'SCHEDULED'),
(1, 2, 2, CURDATE() + INTERVAL 18 HOUR, DATE_ADD(CURDATE(), INTERVAL 1 DAY) + INTERVAL  2 HOUR, 950.00, 'SCHEDULED'),
(1, 3, 3, DATE_ADD(CURDATE(), INTERVAL 1 DAY) + INTERVAL  6 HOUR, DATE_ADD(CURDATE(), INTERVAL 1 DAY) + INTERVAL 14 HOUR, 850.00, 'SCHEDULED'),
(1, 1, 1, DATE_ADD(CURDATE(), INTERVAL 2 DAY) + INTERVAL  6 HOUR, DATE_ADD(CURDATE(), INTERVAL 2 DAY) + INTERVAL 14 HOUR, 850.00, 'SCHEDULED'),
(1, 4, 4, DATE_ADD(CURDATE(), INTERVAL 3 DAY) + INTERVAL 20 HOUR, DATE_ADD(CURDATE(), INTERVAL 4 DAY) + INTERVAL  4 HOUR, 950.00, 'SCHEDULED'),

-- RT002 Chennai-Madurai (today, today, +1)
(2, 5, 5, CURDATE() + INTERVAL  7 HOUR, CURDATE() + INTERVAL 14 HOUR, 720.00, 'SCHEDULED'),
(2, 6, 6, CURDATE() + INTERVAL 21 HOUR, DATE_ADD(CURDATE(), INTERVAL 1 DAY) + INTERVAL  4 HOUR, 820.00, 'SCHEDULED'),
(2, 5, 5, DATE_ADD(CURDATE(), INTERVAL 1 DAY) + INTERVAL  7 HOUR, DATE_ADD(CURDATE(), INTERVAL 1 DAY) + INTERVAL 14 HOUR, 720.00, 'SCHEDULED'),

-- RT003 Chennai-Bangalore (today, +1)
(3, 7, 1, CURDATE() + INTERVAL 22 HOUR, DATE_ADD(CURDATE(), INTERVAL 1 DAY) + INTERVAL  4 HOUR, 650.00, 'SCHEDULED'),
(3, 7, 2, DATE_ADD(CURDATE(), INTERVAL 1 DAY) + INTERVAL 22 HOUR, DATE_ADD(CURDATE(), INTERVAL 2 DAY) + INTERVAL  4 HOUR, 650.00, 'SCHEDULED'),

-- RT005 Chennai-Trichy (today)
(5, 3, 3, CURDATE() + INTERVAL  8 HOUR, CURDATE() + INTERVAL 13 HOUR, 450.00, 'SCHEDULED'),
(5, 4, 4, CURDATE() + INTERVAL 14 HOUR, CURDATE() + INTERVAL 19 HOUR, 450.00, 'SCHEDULED'),

-- Historical completed trips (for analytics — fixed past dates are fine here)
(1, 1, 1, DATE_SUB(CURDATE(), INTERVAL 32 DAY) + INTERVAL  6 HOUR, DATE_SUB(CURDATE(), INTERVAL 32 DAY) + INTERVAL 14 HOUR, 850.00, 'COMPLETED'),
(1, 2, 2, DATE_SUB(CURDATE(), INTERVAL 28 DAY) + INTERVAL  6 HOUR, DATE_SUB(CURDATE(), INTERVAL 28 DAY) + INTERVAL 14 HOUR, 850.00, 'COMPLETED'),
(2, 5, 5, DATE_SUB(CURDATE(), INTERVAL 30 DAY) + INTERVAL  7 HOUR, DATE_SUB(CURDATE(), INTERVAL 30 DAY) + INTERVAL 14 HOUR, 720.00, 'COMPLETED'),
(3, 7, 1, DATE_SUB(CURDATE(), INTERVAL 26 DAY) + INTERVAL 22 HOUR, DATE_SUB(CURDATE(), INTERVAL 25 DAY) + INTERVAL  4 HOUR, 650.00, 'COMPLETED'),
(1, 3, 3, DATE_SUB(CURDATE(), INTERVAL 49 DAY) + INTERVAL  6 HOUR, DATE_SUB(CURDATE(), INTERVAL 49 DAY) + INTERVAL 14 HOUR, 850.00, 'COMPLETED'),
(2, 6, 6, DATE_SUB(CURDATE(), INTERVAL 46 DAY) + INTERVAL 21 HOUR, DATE_SUB(CURDATE(), INTERVAL 45 DAY) + INTERVAL  4 HOUR, 820.00, 'COMPLETED'),
(1, 4, 4, DATE_SUB(CURDATE(), INTERVAL 75 DAY) + INTERVAL  6 HOUR, DATE_SUB(CURDATE(), INTERVAL 75 DAY) + INTERVAL 14 HOUR, 850.00, 'COMPLETED'),
(3, 7, 2, DATE_SUB(CURDATE(), INTERVAL 73 DAY) + INTERVAL 22 HOUR, DATE_SUB(CURDATE(), INTERVAL 72 DAY) + INTERVAL  4 HOUR, 650.00, 'COMPLETED');

-- Route Operating Costs (sample monthly data)
INSERT IGNORE INTO route_operating_costs (route_id, month, year, fuel_cost, driver_cost, maintenance_cost, toll_cost, other_costs) VALUES
(1, 10, 2026, 45000.00, 18000.00, 8000.00, 5000.00, 3000.00),
(1,  9, 2026, 43000.00, 18000.00, 7500.00, 5000.00, 2500.00),
(1,  8, 2026, 42000.00, 18000.00, 9000.00, 5000.00, 2000.00),
(2, 10, 2026, 38000.00, 16000.00, 7000.00, 4500.00, 2500.00),
(2,  9, 2026, 36000.00, 16000.00, 6500.00, 4500.00, 2000.00),
(3, 10, 2026, 32000.00, 15000.00, 6000.00, 4000.00, 2000.00),
(3,  9, 2026, 31000.00, 15000.00, 5500.00, 4000.00, 1800.00),
(5, 10, 2026, 25000.00, 12000.00, 5000.00, 3000.00, 1500.00);

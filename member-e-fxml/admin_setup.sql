-- RoomBuddy Admin Module: admin account + TEST data. Run in phpMyAdmin > roombuddy > SQL tab.
-- Safe to run once. Do NOT run on a database with real data you care about (adds sample rows).

-- 1) Admin account: email admin@roombuddy.com, password admin123 (SHA-256 hash below)
INSERT INTO users (full_name, email, password_hash, auth_provider, is_email_confirmed, role, status)
SELECT 'System Admin', 'admin@roombuddy.com',
       '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
       'local', 1, 'admin', 'active'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@roombuddy.com');

-- 2) Test listings (owners = users 1 to 4 from your sample data)
INSERT INTO listings (owner_id, property_name, property_type, description, barangay, address, price, room_type, house_rules, status) VALUES
(1, 'Sunrise Dormitory', 'dormitory', 'Clean dorm near the university.', 'Barangay 1', '12 Sample Street', 2500.00, 'bedspace', 'No smoking. Curfew 10 PM.', 'pending'),
(2, 'Casa Marie Boarding House', 'boarding_house', 'Quiet boarding house with a study area.', 'Barangay 2', '45 Sample Avenue', 3500.00, 'single', 'No pets. Visitors until 8 PM.', 'pending'),
(3, 'Dave Residences', 'boarding_house', 'Spacious shared rooms.', 'Barangay 3', '7 Sample Road', 2000.00, 'shared', 'Keep quiet after 9 PM.', 'verified'),
(4, 'Nico Bedspace', 'dormitory', 'Affordable bedspaces.', 'Barangay 4', '88 Sample Lane', 1800.00, 'bedspace', 'No cooking in rooms.', 'pending');

-- Look up the new listing ids by name, so this works even if listings already existed
SET @l1 = (SELECT MAX(listing_id) FROM listings WHERE property_name = 'Sunrise Dormitory');
SET @l2 = (SELECT MAX(listing_id) FROM listings WHERE property_name = 'Casa Marie Boarding House');
SET @l3 = (SELECT MAX(listing_id) FROM listings WHERE property_name = 'Dave Residences');
SET @l4 = (SELECT MAX(listing_id) FROM listings WHERE property_name = 'Nico Bedspace');

INSERT INTO listing_amenities (listing_id, amenity_id, extra_cost) VALUES
(@l1, 1, NULL), (@l1, 3, NULL), (@l2, 2, NULL), (@l2, 3, 150.00), (@l2, 10, NULL), (@l3, 1, NULL), (@l4, 4, 300.00);

INSERT INTO listing_photos (listing_id, file_path, is_cover) VALUES
(@l1, 'uploads/listings/sunrise1.jpg', 1), (@l1, 'uploads/listings/sunrise2.jpg', 0),
(@l2, 'uploads/listings/casa1.jpg', 1);

INSERT INTO listing_documents (listing_id, document_type, file_path) VALUES
(@l1, 'land_title', 'uploads/documents/land_title_1.pdf'),
(@l2, 'business_permit', 'uploads/documents/permit_2.pdf');

-- 3) Test ID submissions and reports
INSERT INTO id_verifications (user_id, id_type, front_image_path, back_image_path) VALUES
(1, 'National ID', 'uploads/ids/u1_front.jpg', 'uploads/ids/u1_back.jpg'),
(2, 'Driver License', 'uploads/ids/u2_front.jpg', 'uploads/ids/u2_back.jpg'),
(3, 'Passport', 'uploads/ids/u3_front.jpg', 'uploads/ids/u3_back.jpg');

INSERT INTO reports (reporter_id, reported_user_id, listing_id, reason) VALUES
(4, 1, @l1, 'Photos in the listing look misleading.'),
(2, 3, NULL, 'Rude behavior in chat.'),
(1, NULL, @l3, 'Listing price differs from the actual price.');

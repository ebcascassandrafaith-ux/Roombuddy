-- =====================================================================
-- RoomBuddy: "one house, many rooms"  (the Figma property page + room page)
-- Run in phpMyAdmin: roombuddy database > SQL tab > paste > Go.
-- Safe to run more than once. It does NOT delete or change your existing listings' data
-- (except keeping `listings.price` equal to the cheapest room, see step 5).
--
--   listings  = the HOUSE   (name, address, barangay, house photos, host)   <- already exists
--   rooms     = each ROOM   (name, price, room type, advance/deposit, occupied)   <- new
--   room_photos = photos of one room                                         <- new
-- =====================================================================

-- 1) the new tables (same engine / charset as the rest of the database)
CREATE TABLE IF NOT EXISTS `rooms` (
  `room_id` int(11) NOT NULL AUTO_INCREMENT,
  `listing_id` int(11) NOT NULL,
  `room_name` varchar(100) NOT NULL,
  `description` text DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `room_type` enum('single','double','shared','bedspace') NOT NULL DEFAULT 'single',
  `advance_months` tinyint(4) NOT NULL DEFAULT 1,
  `deposit_months` tinyint(4) NOT NULL DEFAULT 1,
  `is_occupied` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`room_id`),
  KEY `listing_id` (`listing_id`),
  CONSTRAINT `rooms_ibfk_1` FOREIGN KEY (`listing_id`) REFERENCES `listings` (`listing_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `room_photos` (
  `photo_id` int(11) NOT NULL AUTO_INCREMENT,
  `room_id` int(11) NOT NULL,
  `file_path` varchar(255) NOT NULL,
  `is_cover` tinyint(4) NOT NULL DEFAULT 0,
  `uploaded_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`photo_id`),
  KEY `room_id` (`room_id`),
  CONSTRAINT `room_photos_ibfk_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`room_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2) every existing listing becomes a house with ONE room, so nothing you already built stops working
INSERT INTO `rooms` (`listing_id`, `room_name`, `price`, `room_type`, `is_occupied`)
SELECT l.listing_id, 'Room 1', l.price, l.room_type, l.is_occupied
FROM `listings` l
WHERE NOT EXISTS (SELECT 1 FROM `rooms` r WHERE r.listing_id = l.listing_id);

-- 3) sample: "Ang Balay ni Libay Libat" gets the three rooms from the Figma (listing 1 in the sample data)
UPDATE `rooms` SET `room_name` = 'Room 1 - Dodong', `advance_months` = 3, `deposit_months` = 2,
       `description` = 'Bright room with a window facing the garden. Shared kitchen and bathroom.'
WHERE `listing_id` = 1 AND `room_name` = 'Room 1';

INSERT INTO `rooms` (`listing_id`, `room_name`, `description`, `price`, `room_type`, `advance_months`, `deposit_months`)
SELECT 1, 'Room 2 - Inday', 'Quiet room on the second floor.', 3500.00, 'single', 3, 2 FROM DUAL
WHERE EXISTS (SELECT 1 FROM `listings` WHERE listing_id = 1)
  AND NOT EXISTS (SELECT 1 FROM `rooms` WHERE listing_id = 1 AND room_name = 'Room 2 - Inday');

INSERT INTO `rooms` (`listing_id`, `room_name`, `description`, `price`, `room_type`, `advance_months`, `deposit_months`)
SELECT 1, 'Room 3 - Baduday', 'Corner room with extra space for a study table.', 3500.00, 'single', 3, 2 FROM DUAL
WHERE EXISTS (SELECT 1 FROM `listings` WHERE listing_id = 1)
  AND NOT EXISTS (SELECT 1 FROM `rooms` WHERE listing_id = 1 AND room_name = 'Room 3 - Baduday');

-- 4) give every room that has no photos yet a copy of its house's photos (so the room page is not empty).
--    Later, owners upload real room photos; the house photos stay in listing_photos.
INSERT INTO `room_photos` (`room_id`, `file_path`, `is_cover`)
SELECT r.room_id, p.file_path, p.is_cover
FROM `rooms` r JOIN `listing_photos` p ON p.listing_id = r.listing_id
WHERE NOT EXISTS (SELECT 1 FROM `room_photos` rp WHERE rp.room_id = r.room_id);

-- 5) listings.price now means "starting from" = the cheapest room. The home cards, search and price filter
--    keep working without any change. (The app code must repeat this whenever rooms are added or edited.)
UPDATE `listings` l
SET l.price = (SELECT MIN(r.price) FROM `rooms` r WHERE r.listing_id = l.listing_id)
WHERE EXISTS (SELECT 1 FROM `rooms` r WHERE r.listing_id = l.listing_id);

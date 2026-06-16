-- ============================================================
-- FixItNow -- Database Schema (MySQL 8)
-- ============================================================
-- Creates the `fixitnow_db` database and every table used by the backend.
--
-- IMPORTANT: The application normally creates this schema AUTOMATICALLY on first
-- run (spring.jpa.hibernate.ddl-auto=update + createDatabaseIfNotExist=true), so
-- you usually do NOT need to run this manually. It is provided so developers can:
--   * provision the database explicitly (e.g. on a locked-down MySQL), and
--   * read/understand the full schema in one place.
--
-- Generated from a live Hibernate-built schema via mysqldump.
--
-- Usage (from this folder):
--   mysql -u root -p < schema.sql
-- ============================================================


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `fixitnow_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `fixitnow_db`;
DROP TABLE IF EXISTS `bookings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bookings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cancellation_reason` text,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text,
  `estimated_cost` decimal(10,2) DEFAULT NULL,
  `scheduled_date` datetime(6) NOT NULL,
  `service_type` varchar(255) DEFAULT NULL,
  `status` enum('PENDING','ACCEPTED','IN_PROGRESS','COMPLETED','CANCELLED') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `homeowner_id` bigint NOT NULL,
  `provider_id` bigint DEFAULT NULL,
  `service_id` bigint NOT NULL,
  `booking_type` varchar(255) DEFAULT NULL,
  `eta_minutes` int DEFAULT NULL,
  `final_cost` decimal(10,2) DEFAULT NULL,
  `service_address` text,
  `service_latitude` double DEFAULT NULL,
  `service_longitude` double DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKgxdp85vb6opai4j1bm61yk8vy` (`homeowner_id`),
  KEY `FKnuv5epx29ao9njgi1cosrcsjr` (`provider_id`),
  KEY `FKjcwbou2jlblfwu14uoxs65b25` (`service_id`),
  CONSTRAINT `FKgxdp85vb6opai4j1bm61yk8vy` FOREIGN KEY (`homeowner_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKjcwbou2jlblfwu14uoxs65b25` FOREIGN KEY (`service_id`) REFERENCES `services` (`id`),
  CONSTRAINT `FKnuv5epx29ao9njgi1cosrcsjr` FOREIGN KEY (`provider_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_type` varchar(255) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text,
  `is_active` bit(1) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_5le3ghmfrckg818rfx1xja57o` (`category_type`)
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `category_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_notes` text,
  `contact` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `demand` varchar(255) DEFAULT NULL,
  `description` text,
  `icon` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `platform_fee` varchar(255) DEFAULT NULL,
  `price_range` varchar(255) DEFAULT NULL,
  `providers_waiting` int DEFAULT NULL,
  `requested_by` varchar(255) DEFAULT NULL,
  `requester_email` varchar(255) DEFAULT NULL,
  `status` varchar(255) NOT NULL,
  `subtitle` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `disputes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `disputes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `raised_at` datetime(6) DEFAULT NULL,
  `reason` text NOT NULL,
  `resolution` text,
  `resolved_at` datetime(6) DEFAULT NULL,
  `status` enum('OPEN','UNDER_REVIEW','RESOLVED','ESCALATED') NOT NULL,
  `booking_id` bigint NOT NULL,
  `raised_by` bigint NOT NULL,
  `resolved_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKmbmkglcru85cdq73ih2ko4m1k` (`booking_id`),
  KEY `FKbw5x3hkfwrtdcljq5v9jcine4` (`raised_by`),
  KEY `FK1mpt52omc4k0co66p6e5i5nq7` (`resolved_by`),
  CONSTRAINT `FK1mpt52omc4k0co66p6e5i5nq7` FOREIGN KEY (`resolved_by`) REFERENCES `users` (`id`),
  CONSTRAINT `FKbw5x3hkfwrtdcljq5v9jcine4` FOREIGN KEY (`raised_by`) REFERENCES `users` (`id`),
  CONSTRAINT `FKmbmkglcru85cdq73ih2ko4m1k` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `news_articles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `news_articles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `author_id` bigint DEFAULT NULL,
  `body` longtext,
  `category` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `excerpt` text,
  `image_url` varchar(1024) DEFAULT NULL,
  `publish_at` varchar(255) DEFAULT NULL,
  `status` varchar(255) NOT NULL,
  `tag` varchar(255) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `channel` enum('EMAIL','IN_APP','BOTH') DEFAULT NULL,
  `is_read` bit(1) DEFAULT NULL,
  `message` text NOT NULL,
  `sent_at` datetime(6) DEFAULT NULL,
  `type` varchar(80) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9y21adhxn0ayjhfocscqox7bh` (`user_id`),
  CONSTRAINT `FK9y21adhxn0ayjhfocscqox7bh` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `payments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(10,2) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `invoice_path` varchar(255) DEFAULT NULL,
  `method` varchar(50) DEFAULT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `status` enum('PENDING','COMPLETED','FAILED','REFUNDED') NOT NULL,
  `stripe_payment_intent` varchar(255) DEFAULT NULL,
  `transaction_ref` varchar(255) DEFAULT NULL,
  `booking_id` bigint NOT NULL,
  `invoice_no` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_nuscjm6x127hkb15kcb8n56wo` (`booking_id`),
  UNIQUE KEY `UK2bxee5dao70nu8wdys6hg8q6j` (`invoice_no`),
  CONSTRAINT `FKc52o2b1jkxttngufqp3t7jr3h` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `provider_certifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `provider_certifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `provider_id` bigint NOT NULL,
  `document_name` varchar(255) NOT NULL,
  `document_path` varchar(500) NOT NULL,
  `verified` tinyint(1) DEFAULT '0',
  `uploaded_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `provider_id` (`provider_id`),
  CONSTRAINT `provider_certifications_ibfk_1` FOREIGN KEY (`provider_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `provider_locations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `provider_locations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `latitude` double NOT NULL,
  `longitude` double NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `provider_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_im4hdvxrpqd6e6n05x0kn7hdm` (`provider_id`),
  CONSTRAINT `FKinis5i8lqv2i30j3mhqkpcya4` FOREIGN KEY (`provider_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `provider_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `provider_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_notes` text,
  `availability` varchar(255) DEFAULT NULL,
  `category` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `display_name` varchar(255) DEFAULT NULL,
  `district` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `experience` varchar(255) DEFAULT NULL,
  `full_name` varchar(255) DEFAULT NULL,
  `hourly_rate` varchar(255) DEFAULT NULL,
  `nic` varchar(255) DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `score` int DEFAULT NULL,
  `score_label` varchar(255) DEFAULT NULL,
  `status` varchar(255) NOT NULL,
  `sub_speciality` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `applicant_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK14j6iqg3gwqoaeut3astp7s98` (`applicant_id`),
  CONSTRAINT `FK14j6iqg3gwqoaeut3astp7s98` FOREIGN KEY (`applicant_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `provider_services`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `provider_services` (
  `provider_id` bigint NOT NULL,
  `service_id` bigint NOT NULL,
  PRIMARY KEY (`provider_id`,`service_id`),
  KEY `service_id` (`service_id`),
  CONSTRAINT `provider_services_ibfk_1` FOREIGN KEY (`provider_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `provider_services_ibfk_2` FOREIGN KEY (`service_id`) REFERENCES `services` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `refresh_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `refresh_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `expires_at` datetime(6) NOT NULL,
  `revoked` bit(1) NOT NULL,
  `token` varchar(512) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_refresh_token` (`token`),
  KEY `FK1lih5y2npsf8u5o3vhdb9y0os` (`user_id`),
  CONSTRAINT `FK1lih5y2npsf8u5o3vhdb9y0os` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `reviews`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reviews` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment` text,
  `created_at` datetime(6) DEFAULT NULL,
  `is_flagged` bit(1) DEFAULT NULL,
  `rating` int NOT NULL,
  `booking_id` bigint NOT NULL,
  `homeowner_id` bigint NOT NULL,
  `provider_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_3p9j9vyr1qofbcxju65es206r` (`booking_id`),
  KEY `FKkfbsuvdpu35pnk707xrwj6o2p` (`homeowner_id`),
  KEY `FK6v6isw4stf5vu1fktr1whlx06` (`provider_id`),
  CONSTRAINT `FK28an517hrxtt2bsg93uefugrm` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`),
  CONSTRAINT `FK6v6isw4stf5vu1fktr1whlx06` FOREIGN KEY (`provider_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKkfbsuvdpu35pnk707xrwj6o2p` FOREIGN KEY (`homeowner_id`) REFERENCES `users` (`id`),
  CONSTRAINT `reviews_chk_1` CHECK (((`rating` <= 5) and (`rating` >= 1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `services`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `services` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `day_payment` decimal(10,2) NOT NULL,
  `description` text,
  `is_active` bit(1) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `category_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKhv7d5p40ipfq91065vlmqk8xv` (`category_id`),
  CONSTRAINT `FKhv7d5p40ipfq91065vlmqk8xv` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `user_type` varchar(31) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `access_level` int DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `badge_level` enum('NONE','BRONZE','SILVER','GOLD','TOP_RATED') DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `department` varchar(255) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `is_blacklisted` bit(1) DEFAULT NULL,
  `is_verified` bit(1) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `photo` longblob,
  `rating` double DEFAULT NULL,
  `service_category` varchar(255) DEFAULT NULL,
  `username` varchar(255) NOT NULL,
  `availability_status` varchar(255) DEFAULT NULL,
  `certifications` text,
  `preferred_services` text,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UK_r43af9ap4edm43mmtq01oddj6` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `wallet_transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wallet_transactions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(12,2) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `reference` varchar(255) DEFAULT NULL,
  `tone` varchar(255) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKlycqjd43jd0oy9inqj5xmw3h2` (`reference`),
  KEY `FKrtsa3qtjhd0rn4xb92na03vd` (`user_id`),
  CONSTRAINT `FKrtsa3qtjhd0rn4xb92na03vd` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


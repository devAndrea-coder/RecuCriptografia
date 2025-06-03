-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Jun 03, 2025 at 02:26 AM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `medico`
--

-- --------------------------------------------------------

--
-- Table structure for table `medico`
--

CREATE TABLE `medico` (
  `codiMedi` int(11) NOT NULL,
  `ndniMedi` varchar(8) DEFAULT NULL,
  `appaMedi` varchar(50) DEFAULT NULL,
  `apmaMedi` varchar(50) DEFAULT NULL,
  `nombMedi` varchar(50) DEFAULT NULL,
  `fechNaciMedi` date DEFAULT NULL,
  `logiMedi` varchar(100) DEFAULT NULL,
  `passMedi` varchar(500) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `medico`
--

INSERT INTO `medico` (`codiMedi`, `ndniMedi`, `appaMedi`, `apmaMedi`, `nombMedi`, `fechNaciMedi`, `logiMedi`, `passMedi`) VALUES
(1, '12345678', 'ROMERO', 'SANCHEZ', 'JUAN CARLOS', '1974-11-20', 'kike', '$2a$12$D3m1gIpklx5FiUgmeOftQeM2NFGdIAkTJKlWZVIjBS4GKVf5agFt2'),
(2, '71574446', 'SEVÁN', 'YARLEQUÉ', 'ANDREA YURIKO', '2005-12-14', 'andrea', '$2a$12$D3m1gIpklx5FiUgmeOftQeM2NFGdIAkTJKlWZVIjBS4GKVf5agFt2');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `medico`
--
ALTER TABLE `medico`
  ADD PRIMARY KEY (`codiMedi`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `medico`
--
ALTER TABLE `medico`
  MODIFY `codiMedi` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;

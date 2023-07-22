-- Crea il database "sensors" se non esiste già
DROP DATABASE IF EXISTS iot;
CREATE DATABASE iot;

-- Seleziona il database "sensors"
USE iot;

CREATE TABLE IF NOT EXISTS oxygen_sensor (
    id INT NOT NULL,
    timestamp DATETIME,
    value DECIMAL(5, 2) CHECK (value >= 0 AND value <= 100),
    PRIMARY KEY (id, timestamp)
);

CREATE TABLE IF NOT EXISTS heartbeat_sensor (
    id INT NOT NULL,
    timestamp DATETIME,
    value DECIMAL(5, 2) CHECK (value >= 0 AND value <= 100),
    PRIMARY KEY (id, timestamp)
);

CREATE TABLE IF NOT EXISTS temperature_sensor (
    id INT NOT NULL,
    timestamp DATETIME,
    value DECIMAL(5, 2) CHECK (value >= 0 AND value <= 100),
    PRIMARY KEY (id, timestamp)
);

CREATE TABLE `actuator` (
    `ip`        INET6 PRIMARY KEY,
    `type`      VARCHAR(40) NOT NULL
);
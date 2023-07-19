-- Crea il database "sensors" se non esiste già
DROP DATABASE IF EXISTS iot;
CREATE DATABASE iot;

-- Seleziona il database "sensors"
USE iot;

CREATE TABLE IF NOT EXISTS sensor_1 (
    id INT NOT NULL,
    timestamp DATETIME,
    value DECIMAL(5, 2) CHECK (value >= 0 AND value <= 100),
    PRIMARY KEY (id, timestamp)
);

CREATE TABLE IF NOT EXISTS sensor_2 (
    id INT NOT NULL,
    timestamp DATETIME,
    value DECIMAL(5, 2) CHECK (value >= 0 AND value <= 100),
    PRIMARY KEY (id, timestamp)
);

CREATE TABLE IF NOT EXISTS sensor_3 (
    id INT NOT NULL,
    timestamp DATETIME,
    value DECIMAL(5, 2) CHECK (value >= 0 AND value <= 100),
    PRIMARY KEY (id, timestamp)
);

-- Crea il database "iot" se non esiste già
DROP DATABASE IF EXISTS iot;
CREATE DATABASE iot;

-- Seleziona il database "iot"
USE iot;

CREATE TABLE IF NOT EXISTS oxygen_sensor (
    id INT NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    value DECIMAL(5, 2),
    PRIMARY KEY (id, timestamp)
);

CREATE TABLE IF NOT EXISTS heartbeat_sensor (
    id INT NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    value INTEGER(3),
    PRIMARY KEY (id, timestamp)
);

CREATE TABLE IF NOT EXISTS temperature_sensor (
    id INT NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    value DECIMAL(5, 2),
    PRIMARY KEY (id, timestamp)
);

CREATE TABLE IF NOT EXISTS actuator (
    ip VARCHAR(45) PRIMARY KEY,
    type VARCHAR(40) NOT NULL
);

-- Crea il database "iot" se non esiste già
DROP DATABASE IF EXISTS iot;
CREATE DATABASE iot;

-- Seleziona il database "iot"
USE iot;

CREATE TABLE IF NOT EXISTS oxygen_sensor (
    id VARCHAR(5) NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    value DECIMAL(5, 2),
    PRIMARY KEY (id, timestamp)
);

CREATE TABLE IF NOT EXISTS heartbeat_sensor (
    id VARCHAR(5) NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    value INTEGER,
    PRIMARY KEY (id, timestamp)
);

CREATE TABLE IF NOT EXISTS temperature_sensor (
    id VARCHAR(5) NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    value DECIMAL(5, 2),
    PRIMARY KEY (id, timestamp)
);

CREATE TABLE IF NOT EXISTS actuator (
    ip VARCHAR(45) PRIMARY KEY,
    type VARCHAR(40) NOT NULL,
    status INT NOT NULL DEFAULT 0
);

DELIMITER //
CREATE TRIGGER check_oxygen_sensor_id
BEFORE INSERT ON oxygen_sensor
FOR EACH ROW 
BEGIN
    IF NOT (NEW.id LIKE 'o%') THEN 
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'ID must start with "o"';
    END IF;
    IF NEW.value < 40 OR NEW.value > 100 THEN 
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Value out of range';
    END IF;
END;
//
DELIMITER ;

DELIMITER //
CREATE TRIGGER check_heartbeat_sensor_id
BEFORE INSERT ON heartbeat_sensor
FOR EACH ROW 
BEGIN
    IF NOT (NEW.id LIKE 'h%') THEN 
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'ID must start with "h"';
    END IF;
    IF NEW.value < 50 OR NEW.value > 140 THEN 
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Value out of range';
    END IF;
END;
//
DELIMITER ;

DELIMITER //
CREATE TRIGGER check_temperature_sensor_id
BEFORE INSERT ON temperature_sensor
FOR EACH ROW 
BEGIN
    IF NOT (NEW.id LIKE 't%') THEN 
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'ID must start with "t"';
    END IF;
    IF NEW.value < 34 OR NEW.value > 45 THEN 
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Value out of range';
    END IF;
END;
//
DELIMITER ;

-- Inserisci 10 valori nella tabella oxygen_sensor
INSERT INTO oxygen_sensor (id, timestamp, value) VALUES 
('o1', NOW(), 40 + RAND() * 60),
('o1', DATE_ADD(NOW(), INTERVAL 1 SECOND), 40 + RAND() * 60),
('o1', DATE_ADD(NOW(), INTERVAL 2 SECOND), 40 + RAND() * 60),
('o1', DATE_ADD(NOW(), INTERVAL 3 SECOND), 40 + RAND() * 60),
('o1', DATE_ADD(NOW(), INTERVAL 4 SECOND), 40 + RAND() * 60),
('o1', DATE_ADD(NOW(), INTERVAL 5 SECOND), 40 + RAND() * 60),
('o1', DATE_ADD(NOW(), INTERVAL 6 SECOND), 40 + RAND() * 60),
('o1', DATE_ADD(NOW(), INTERVAL 7 SECOND), 40 + RAND() * 60),
('o1', DATE_ADD(NOW(), INTERVAL 8 SECOND), 40 + RAND() * 60),
('o1', DATE_ADD(NOW(), INTERVAL 9 SECOND), 40 + RAND() * 60);

-- Inserisci 10 valori nella tabella heartbeat_sensor
INSERT INTO heartbeat_sensor (id, timestamp, value) VALUES 
('h1', NOW(), FLOOR(50 + RAND() * 90)),
('h1', DATE_ADD(NOW(), INTERVAL 1 SECOND), FLOOR(50 + RAND() * 90)),
('h1', DATE_ADD(NOW(), INTERVAL 2 SECOND), FLOOR(50 + RAND() * 90)),
('h1', DATE_ADD(NOW(), INTERVAL 3 SECOND), FLOOR(50 + RAND() * 90)),
('h1', DATE_ADD(NOW(), INTERVAL 4 SECOND), FLOOR(50 + RAND() * 90)),
('h1', DATE_ADD(NOW(), INTERVAL 5 SECOND), FLOOR(50 + RAND() * 90)),
('h1', DATE_ADD(NOW(), INTERVAL 6 SECOND), FLOOR(50 + RAND() * 90)),
('h1', DATE_ADD(NOW(), INTERVAL 7 SECOND), FLOOR(50 + RAND() * 90)),
('h1', DATE_ADD(NOW(), INTERVAL 8 SECOND), FLOOR(50 + RAND() * 90)),
('h1', DATE_ADD(NOW(), INTERVAL 9 SECOND), FLOOR(50 + RAND() * 90));

-- Inserisci 10 valori nella tabella temperature_sensor
INSERT INTO temperature_sensor (id, timestamp, value) VALUES 
('t1', NOW(), 34 + RAND() * 11),
('t1', DATE_ADD(NOW(), INTERVAL 1 SECOND), 34 + RAND() * 11),
('t1', DATE_ADD(NOW(), INTERVAL 2 SECOND), 34 + RAND() * 11),
('t1', DATE_ADD(NOW(), INTERVAL 3 SECOND), 34 + RAND() * 11),
('t1', DATE_ADD(NOW(), INTERVAL 4 SECOND), 34 + RAND() * 11),
('t1', DATE_ADD(NOW(), INTERVAL 5 SECOND), 34 + RAND() * 11),
('t1', DATE_ADD(NOW(), INTERVAL 6 SECOND), 34 + RAND() * 11),
('t1', DATE_ADD(NOW(), INTERVAL 7 SECOND), 34 + RAND() * 11),
('t1', DATE_ADD(NOW(), INTERVAL 8 SECOND), 34 + RAND() * 11),
('t1', DATE_ADD(NOW(), INTERVAL 9 SECOND), 34 + RAND() * 11);

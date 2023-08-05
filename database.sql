-- Crea il database "iot" se non esiste già
DROP DATABASE IF EXISTS iot;
CREATE DATABASE iot;

-- Seleziona il database "iot"
USE iot;

CREATE TABLE IF NOT EXISTS oxygen_sensor (
    id VARCHAR(5) NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    value INTEGER NOT NULL,
    PRIMARY KEY (id, timestamp)
);

CREATE TABLE IF NOT EXISTS cardio_sensor (
    id VARCHAR(5) NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    value INTEGER NOT NULL,
    PRIMARY KEY (id, timestamp)
);

CREATE TABLE IF NOT EXISTS troponin_sensor (
    id VARCHAR(5) NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    value DECIMAL(5, 2) NOT NULL,
    PRIMARY KEY (id, timestamp)
);

CREATE TABLE IF NOT EXISTS actuator (
    ip VARCHAR(45) PRIMARY KEY,
    type VARCHAR(40) NOT NULL,
    status INTEGER NOT NULL DEFAULT 0
);
/*
DELIMITER //
CREATE TRIGGER check_oxygen_sensor_id
BEFORE INSERT ON oxygen_sensor
FOR EACH ROW 
BEGIN
    IF NOT (NEW.id LIKE 'o%') THEN 
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'ID must start with "o"';
    END IF;
END;//
DELIMITER ;

DELIMITER //
CREATE TRIGGER check_cardio_sensor_id
BEFORE INSERT ON cardio_sensor
FOR EACH ROW 
BEGIN
    IF NOT (NEW.id LIKE 'c%') THEN 
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'ID must start with "c"';
    END IF;
END;//
DELIMITER ;

DELIMITER //
CREATE TRIGGER check_troponin_sensor_id
BEFORE INSERT ON troponin_sensor
FOR EACH ROW 
BEGIN
    IF NOT (NEW.id LIKE 't%') THEN 
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'ID must start with "t"';
    END IF;
END;//
DELIMITER ;
*/
-- Inserisci 10 valori nella tabella oxygen_sensor
INSERT INTO oxygen_sensor (id, timestamp, value) VALUES 
('o001', NOW(), 55),
('o001', DATE_ADD(NOW(), INTERVAL 1 SECOND), 55),
('o001', DATE_ADD(NOW(), INTERVAL 2 SECOND), 55),
('o001', DATE_ADD(NOW(), INTERVAL 3 SECOND), 55),
('o001', DATE_ADD(NOW(), INTERVAL 4 SECOND), 55),
('o001', DATE_ADD(NOW(), INTERVAL 5 SECOND), 55),
('o001', DATE_ADD(NOW(), INTERVAL 6 SECOND), 55),
('o001', DATE_ADD(NOW(), INTERVAL 7 SECOND), 55),
('o001', DATE_ADD(NOW(), INTERVAL 8 SECOND), 55),
('o001', DATE_ADD(NOW(), INTERVAL 9 SECOND), 55);

-- Inserisci 10 valori nella tabella cardio_sensor
INSERT INTO cardio_sensor (id, timestamp, value) VALUES 
('c001', NOW(), 90),
('c001', DATE_ADD(NOW(), INTERVAL 1 SECOND), 90),
('c001', DATE_ADD(NOW(), INTERVAL 2 SECOND), 90),
('c001', DATE_ADD(NOW(), INTERVAL 3 SECOND), 90),
('c001', DATE_ADD(NOW(), INTERVAL 4 SECOND), 90),
('c001', DATE_ADD(NOW(), INTERVAL 5 SECOND), 90),
('c001', DATE_ADD(NOW(), INTERVAL 6 SECOND), 90),
('c001', DATE_ADD(NOW(), INTERVAL 7 SECOND), 90),
('c001', DATE_ADD(NOW(), INTERVAL 8 SECOND), 90),
('c001', DATE_ADD(NOW(), INTERVAL 9 SECOND), 90);

-- Inserisci 10 valori nella tabella troponin_sensor
INSERT INTO troponin_sensor (id, timestamp, value) VALUES 
('t001', NOW(), 0.05),
('t001', DATE_ADD(NOW(), INTERVAL 1 SECOND), 0.05),
('t001', DATE_ADD(NOW(), INTERVAL 2 SECOND), 0.05),
('t001', DATE_ADD(NOW(), INTERVAL 3 SECOND), 0.05),
('t001', DATE_ADD(NOW(), INTERVAL 4 SECOND), 0.05),
('t001', DATE_ADD(NOW(), INTERVAL 5 SECOND), 0.05),
('t001', DATE_ADD(NOW(), INTERVAL 6 SECOND), 0.05),
('t001', DATE_ADD(NOW(), INTERVAL 7 SECOND), 0.05),
('t001', DATE_ADD(NOW(), INTERVAL 8 SECOND), 0.05),
('t001', DATE_ADD(NOW(), INTERVAL 9 SECOND), 0.05);


/*INSERT INTO oxygen_sensor (id, timestamp, value) VALUES
('o001', NOW(), 50),
('o001', DATE_ADD(NOW(), INTERVAL 1 SECOND), 50),
('o001', DATE_ADD(NOW(), INTERVAL 2 SECOND), 50),
('o001', DATE_ADD(NOW(), INTERVAL 3 SECOND), 50),
('o001', DATE_ADD(NOW(), INTERVAL 4 SECOND), 50),
('o001', DATE_ADD(NOW(), INTERVAL 5 SECOND), 50),
('o001', DATE_ADD(NOW(), INTERVAL 6 SECOND), 50),
('o001', DATE_ADD(NOW(), INTERVAL 7 SECOND), 50),
('o001', DATE_ADD(NOW(), INTERVAL 8 SECOND), 50),
('o001', DATE_ADD(NOW(), INTERVAL 9 SECOND), 50);*/

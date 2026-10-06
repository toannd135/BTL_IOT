CREATE TABLE IF NOT EXISTS users (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  username    VARCHAR(255) NOT NULL UNIQUE,
  password    VARCHAR(255) NOT NULL,
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS devices (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(255) NOT NULL,
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sensors (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(255) NOT NULL,
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS data_sensors (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  sensor_id   INT NOT NULL,
  value       FLOAT NOT NULL,
  time        DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_data_sensors_sensor FOREIGN KEY (sensor_id) REFERENCES sensors(id),
  INDEX idx_data_sensors_time (time),
  INDEX idx_data_sensors_sensor_time (sensor_id, time)
);

CREATE TABLE IF NOT EXISTS actions (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  device_id   INT NOT NULL,
  user_id     INT NOT NULL,
  action      VARCHAR(255) NOT NULL,
  status      VARCHAR(255) NOT NULL,
  time        DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_actions_device FOREIGN KEY (device_id) REFERENCES devices(id),
  CONSTRAINT fk_actions_user   FOREIGN KEY (user_id)   REFERENCES users(id),
  INDEX idx_actions_time (time),
  INDEX idx_actions_device_status (device_id, status)
);

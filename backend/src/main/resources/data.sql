INSERT IGNORE INTO users (id, username, password) VALUES
  (1, 'admin', 'admin123'),
  (2, 'toanndc', 'b23dccn831');

INSERT IGNORE INTO devices (id, name) VALUES
  (1, 'LED 1'),
  (2, 'LED 2'),
  (3, 'LED 3');

INSERT IGNORE INTO sensors (id, name) VALUES
  (1, 'temperature'),
  (2, 'humidity'),
  (3, 'light');

db.host=localhost
db.port=3306
db.name=bluelocker_db
db.user=bluelocker_user
db.password=StrongPassword!
db.useSSL=false
db.serverTimezone=UTC

# HikariCP (optionnel)
db.maximumPoolSize=10
db.minimumIdle=2
db.connectionTimeout=30000
db.idleTimeout=600000
-- users table
CREATE TABLE IF NOT EXISTS users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(100) NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  salt VARCHAR(255), -- facultatif si tu utilises Argon2
  first_name VARCHAR(100),
  last_name VARCHAR(100),
  email VARCHAR(255),
  role VARCHAR(32) DEFAULT 'USER',
  public_key TEXT, -- Base64 X.509
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- secure_items table (fichiers chiffrés / notes)
CREATE TABLE IF NOT EXISTS secure_items (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  title VARCHAR(255) NOT NULL,
  item_type VARCHAR(32) NOT NULL,
  file_size BIGINT DEFAULT 0,
  encrypted_data LONGBLOB NOT NULL,
  iv VARBINARY(16) NOT NULL,           -- 12 bytes expected, 16 for safety
  encrypted_dek VARBINARY(512) NOT NULL,
  dek_alg VARCHAR(64) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_secure_items_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
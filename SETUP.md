# Hướng dẫn cài đặt và chạy Color Guessing Game

## 🔧 Yêu cầu hệ thống

### Phần mềm cần thiết
- **Java Development Kit (JDK) 17 trở lên**
- **MySQL Server 8.0 trở lên**
- **Apache Maven 3.6 trở lên**

### Kiểm tra yêu cầu hệ thống

```bash
# Kiểm tra Java version
java -version
javac -version

# Kiểm tra Maven
mvn -version

# Kiểm tra MySQL
mysql --version
```

## 📥 Cài đặt từng bước

### Bước 1: Download và cài đặt Java JDK 17+

#### Windows:
1. Tải Oracle JDK 17+ từ [Oracle website](https://www.oracle.com/java/technologies/downloads/)
2. Chạy file installer và làm theo hướng dẫn
3. Thiết lập biến môi trường `JAVA_HOME`

#### macOS:
```bash
# Sử dụng Homebrew
brew install openjdk@17

# Thiết lập JAVA_HOME
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' >> ~/.zshrc
source ~/.zshrc
```

#### Ubuntu/Linux:
```bash
# Cài đặt OpenJDK 17
sudo apt update
sudo apt install openjdk-17-jdk

# Thiết lập JAVA_HOME
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
source ~/.bashrc
```

### Bước 2: Cài đặt Apache Maven

#### Windows:
1. Tải Maven từ [Maven website](https://maven.apache.org/download.cgi)
2. Giải nén và thêm `bin` folder vào PATH

#### macOS:
```bash
brew install maven
```

#### Ubuntu/Linux:
```bash
sudo apt install maven
```

### Bước 3: Cài đặt MySQL Server

#### Windows:
1. Tải MySQL Installer từ [MySQL website](https://dev.mysql.com/downloads/installer/)
2. Chạy installer và chọn "Server only" hoặc "Developer Default"
3. Thiết lập root password

#### macOS:
```bash
brew install mysql
brew services start mysql

# Thiết lập root password
mysql_secure_installation
```

#### Ubuntu/Linux:
```bash
sudo apt update
sudo apt install mysql-server

# Thiết lập MySQL
sudo mysql_secure_installation

# Khởi động MySQL service
sudo systemctl start mysql
sudo systemctl enable mysql
```

### Bước 4: (Bỏ qua) — Client dùng Swing, không cần JavaFX

## 🗄️ Thiết lập Database

### Bước 1: Đăng nhập MySQL

```bash
mysql -u root -p
```

### Bước 2: Tạo database và user

```sql
-- Tạo database
CREATE DATABASE color_guessing_game CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo user cho game (tuỳ chọn)
CREATE USER 'gameuser'@'localhost' IDENTIFIED BY 'gamepassword';
GRANT ALL PRIVILEGES ON color_guessing_game.* TO 'gameuser'@'localhost';
FLUSH PRIVILEGES;

-- Sử dụng database
USE color_guessing_game;
```

### Bước 3: Import schema

```bash
# Từ thư mục gốc của project
mysql -u root -p color_guessing_game < database/schema.sql
```

Hoặc copy-paste nội dung file `database/schema.sql` vào MySQL console.

### Bước 4: Kiểm tra tables đã được tạo

```sql
USE color_guessing_game;
SHOW TABLES;

-- Kết quả sẽ hiển thị:
-- +--------------------------------+
-- | Tables_in_color_guessing_game  |
-- +--------------------------------+
-- | users                          |
-- | games                          |
-- | game_rounds                    |
-- | game_invitations              |
-- | user_sessions                 |
-- | ranking_history               |
-- +--------------------------------+
```

## 📁 Download và setup project

### Option 1: Clone từ Git (nếu có)
```bash
git clone [repository-url]
cd color-guessing-game
```

### Option 2: Extract từ ZIP file
```bash
unzip color-guessing-game.zip
cd color-guessing-game
```

## 🔨 Build project

### Bước 1: Build với Maven

```bash
# Từ thư mục gốc của project
mvn clean compile

# Nếu có lỗi dependency, thử:
mvn clean install -U
```

### Bước 2: Kiểm tra build thành công

```bash
# Kiểm tra target directory đã được tạo
ls -la target/

# Kiểm tra classes đã được compile
ls -la target/classes/com/ncs/
```

## 🚀 Chạy ứng dụng

### Chạy Server

#### Option 1: Với Maven
```bash
mvn exec:java -Dexec.mainClass="com.ncs.server.GameServer"

# Hoặc chỉ định port
mvn exec:java -Dexec.mainClass="com.ncs.server.GameServer" -Dexec.args="8888"
```

#### Option 2: Với Java command
```bash
java -cp target/classes:target/lib/* com.ncs.server.GameServer 8888
```

#### Option 3: Với custom database config
```bash
java -Ddb.url=jdbc:mysql://localhost:3306/color_guessing_game \
     -Ddb.username=gameuser \
     -Ddb.password=gamepassword \
     -cp target/classes:target/lib/* \
     com.ncs.server.GameServer 8888
```

**Server sẽ hiển thị:**
```
[INFO] Color Guessing Game Server đã khởi động trên port 8888
[INFO] Đang chờ client kết nối...
```

### Chạy Client (Swing)

```bash
mvn -q -DskipTests exec:java -Dexec.mainClass="com.ncs.client.SwingClientApplication"
```

## 🧪 Test kết nối

### Bước 1: Chạy server
```bash
mvn exec:java -Dexec.mainClass="com.ncs.server.GameServer"
```

### Bước 2: Chạy client đầu tiên
```bash
mvn -q -DskipTests exec:java -Dexec.mainClass="com.ncs.client.SwingClientApplication"
```

### Bước 3: Đăng ký tài khoản
1. Tại LoginFrame, nhập username và password
2. Nhấn "Đăng ký"

### Bước 4: Đăng nhập
1. Nhập thông tin đã đăng ký
2. Nhấn "Đăng nhập"

### Bước 5: Chạy client thứ hai (terminal khác)
```bash
mvn -q -DskipTests exec:java -Dexec.mainClass="com.ncs.client.SwingClientApplication"
```

### Bước 6: Test multiplayer
1. Đăng ký user thứ hai với username khác
2. Từ một client, gửi lời mời tới user kia
3. Chấp nhận lời mời và bắt đầu game

## ⚙️ Cấu hình nâng cao

### Thay đổi cấu hình Database

Chỉnh sửa file `src/main/java/com/ncs/dao/DatabaseConnection.java`:

```java
private static final String DEFAULT_URL = "jdbc:mysql://your-host:3306/your-database";
private static final String DEFAULT_USERNAME = "your-username";  
private static final String DEFAULT_PASSWORD = "your-password";
```

### Thay đổi cấu hình Server

Chỉnh sửa file `src/main/java/com/ncs/server/GameServer.java`:

```java
private static final int DEFAULT_PORT = 8888; // Thay đổi port
```

### Thay đổi thời gian game

Chỉnh sửa file `src/main/java/com/ncs/server/GameSession.java`:

```java
private static final int SHOW_COLORS_DURATION = 5000; // 5 giây hiển thị màu
private static final int GUESSING_DURATION = 10000;   // 10 giây đoán màu
```

## 🐛 Xử lý sự cố

### Lỗi "Connection refused"
```
- Kiểm tra server đã chạy chưa
- Kiểm tra port có bị block bởi firewall không
- Thử thay đổi port khác
```

### Lỗi "Access denied for user"
```sql
-- Kiểm tra user và password MySQL
mysql -u root -p

-- Reset password nếu cần
ALTER USER 'root'@'localhost' IDENTIFIED BY 'new-password';
```

### Lỗi "Table doesn't exist"
```bash
# Import lại schema
mysql -u root -p color_guessing_game < database/schema.sql
```

### Lỗi giao diện
Đảm bảo dùng JDK 17+ (Swing có sẵn trong JDK).

### Lỗi Maven build
```bash
# Clean và build lại
mvn clean
mvn compile

# Update dependencies
mvn dependency:resolve
```

### Lỗi "Port already in use"
```bash
# Tìm process đang sử dụng port
lsof -i :8888  # Linux/macOS
netstat -ano | findstr :8888  # Windows

# Kill process
kill -9 <PID>  # Linux/macOS
taskkill /PID <PID> /F  # Windows
```

## 📚 Tài liệu tham khảo

- [Java Documentation](https://docs.oracle.com/en/java/)
- [Maven Documentation](https://maven.apache.org/guides/)
- [MySQL Documentation](https://dev.mysql.com/doc/)

## 📞 Hỗ trợ

Nếu gặp vấn đề trong quá trình cài đặt, hãy:

1. Kiểm tra lại từng bước trong hướng dẫn
2. Xem phần xử lý sự cố ở trên
3. Kiểm tra log errors trong console
4. Liên hệ nhóm phát triển

---

**Chúc bạn cài đặt thành công! 🎉**

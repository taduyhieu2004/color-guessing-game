# Color Guessing Game - Game Đoán Màu Online

Đây là dự án bài tập lớn môn Lập trình mạng - Game đoán màu online với kiến trúc client-server sử dụng Java Socket Programming, Swing, và MySQL.

## 📋 Mô tả Game

Game đoán màu online là một trò chơi nhiều người chơi nơi người chơi phải ghi nhớ và đoán đúng bộ màu sắc được hiển thị trong thời gian ngắn.

### 🎮 Luật chơi

1. **Đăng ký/Đăng nhập**: Người chơi phải tạo tài khoản và đăng nhập để tham gia
2. **Danh sách người chơi**: Sau khi đăng nhập, hiển thị danh sách người chơi online với thông tin:
   - Tên người chơi
   - Điểm xếp hạng hiện tại  
   - Trạng thái (online/offline/đang chơi)
3. **Thách đấu**: Click vào tên đối thủ để gửi lời mời thách đấu
4. **Phản hồi**: Người được mời có thể chấp nhận (OK) hoặc từ chối (Reject)
5. **Trận đấu**: Mỗi trận gồm 5 lượt chơi
   - **Phase 1** (5 giây): Server hiển thị 3 màu ngẫu nhiên
   - **Phase 2** (10 giây): Hiển thị bảng màu với nhiều màu khác nhau, người chơi chọn đúng 3 màu đã thấy
6. **Tính điểm**:
   - Đúng và nhanh nhất: +1 điểm
   - Cả hai đúng, thời gian bằng nhau: +0.5 điểm mỗi người  
   - Sai: 0 điểm
7. **Kết thúc trận**: Sau 5 lượt, người có điểm cao hơn thắng
   - Thắng: +10 điểm xếp hạng
   - Thua: -5 điểm xếp hạng
   - Hòa: +5 điểm xếp hạng
8. **Rời giữa chừng**: Người rời bị trừ 0.5 điểm, đối thủ +1 điểm
9. **Rematch**: Sau trận có thể tái đấu nếu cả hai đồng ý trong 10 giây

## 🏗️ Kiến trúc hệ thống

### Mô hình MVC (Model-View-Controller)

```
├── Model (com.ncs.model)
│   ├── User.java - Thông tin người dùng
│   ├── Game.java - Thông tin trận đấu  
│   ├── GameRound.java - Thông tin lượt chơi
│   ├── GameColor.java - Màu sắc trong game
│   └── GameInvitation.java - Lời mời thách đấu
│
├── View (Swing)
│   ├── LoginFrame.java - Giao diện đăng nhập
│   ├── MainFrame.java - Giao diện chính
│   └── GamePlayFrame.java - Giao diện chơi game
│
├── Controller (com.ncs.client)
│   ├── LoginController.java - Xử lý đăng nhập
│   ├── MainGameController.java - Xử lý giao diện chính
│   └── GamePlayController.java - Xử lý gameplay
│
├── Server (com.ncs.server)
│   ├── GameServer.java - Server chính
│   ├── ClientHandler.java - Xử lý từng client
│   ├── GameManager.java - Quản lý game logic
│   └── GameSession.java - Quản lý từng trận đấu
│
└── DAO (com.ncs.dao)
    ├── DatabaseConnection.java - Kết nối database
    ├── UserDAO.java - Truy cập dữ liệu user
    └── GameDAO.java - Truy cập dữ liệu game
```

### Luồng hoạt động

```
1. Client kết nối → Server
2. Đăng nhập/Đăng ký → Xác thực
3. Lấy danh sách user online
4. Gửi/nhận lời mời thách đấu
5. Bắt đầu game session
6. Thực hiện 5 rounds:
   ├── Hiển thị màu (5s)
   ├── Đoán màu (10s)
   └── Tính kết quả
7. Kết thúc game + cập nhật điểm
8. Rematch hoặc về menu
```

## 🛠️ Công nghệ sử dụng

- **Ngôn ngữ**: Java 17
- **Build Tool**: Maven
- **Database**: MySQL 8.0
- **GUI Framework**: Java Swing
- **Database Access**: JDBC
- **JSON Processing**: Jackson
- **Logging**: SLF4J + Logback
- **Architecture**: Socket Programming (TCP)

## 📦 Cài đặt và chạy

### Yêu cầu hệ thống

- Java 17+ 
- MySQL 8.0+
- Maven 3.6+
  

### Bước 1: Cài đặt Database

```sql
-- Tạo database
CREATE DATABASE color_guessing_game CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Import schema
mysql -u root -p color_guessing_game < database/schema.sql
```

### Bước 2: Cấu hình Database

Chỉnh sửa thông tin kết nối database trong `DatabaseConnection.java` hoặc sử dụng system properties:

```bash
-Ddb.url=jdbc:mysql://localhost:3306/color_guessing_game
-Ddb.username=root  
-Ddb.password=yourpassword
```

### Bước 3: Build Project

```bash
mvn clean compile
```

### Bước 4: Chạy Server

```bash
# Chạy với Maven
mvn exec:java -Dexec.mainClass="com.ncs.server.GameServer" -Dexec.args="8888"

# Hoặc chạy trực tiếp
java -cp target/classes com.ncs.server.GameServer 8888
```

### Bước 5: Chạy Client (Swing)

```bash
# Chạy với Maven exec plugin
mvn -q -DskipTests exec:java -Dexec.mainClass="com.ncs.client.SwingClientApplication"
```

## 🎯 Hướng dẫn sử dụng

### Đăng ký tài khoản

1. Chạy client application
2. Chọn tab "Đăng ký"
3. Nhập thông tin: username, password, họ tên (optional), email (optional)
4. Click "Đăng ký"

### Đăng nhập

1. Chọn tab "Đăng nhập"  
2. Nhập username và password
3. Cấu hình server nếu cần (mặc định localhost:8888)
4. Click "Đăng nhập"

### Chơi game

1. Sau khi đăng nhập, xem danh sách người chơi online
2. Click vào tên đối thủ để gửi lời mời
3. Đợi đối thủ chấp nhận
4. Trong game:
   - Quan sát 3 màu trong 5 giây
   - Chọn đúng 3 màu đó từ bảng màu trong 10 giây
   - Repeat cho 5 rounds
5. Xem kết quả và chọn rematch nếu muốn

## 🗂️ Cấu trúc Database

### Bảng `users`
- Lưu thông tin người dùng, điểm xếp hạng, thống kê

### Bảng `games`  
- Lưu thông tin các trận đấu

### Bảng `game_rounds`
- Lưu chi tiết từng lượt chơi

### Bảng `game_invitations`
- Lưu lời mời thách đấu

### Bảng `ranking_history`
- Lưu lịch sử thay đổi điểm

## 🔧 Cấu hình

### Server Configuration

```java
// Trong GameServer.java
private static final int DEFAULT_PORT = 8888;
private static final int SHOW_COLORS_DURATION = 5000; // 5 giây
private static final int GUESSING_DURATION = 10000;   // 10 giây
```

### Client Configuration

```java
// Trong GameClient.java  
private static final String DEFAULT_HOST = "localhost";
private static final int DEFAULT_PORT = 8888;
```

### Database Configuration

```java
// Trong DatabaseConnection.java
private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/color_guessing_game";
private static final String DEFAULT_USERNAME = "root";
private static final String DEFAULT_PASSWORD = "";
```

## 🐛 Xử lý lỗi thường gặp

### Lỗi kết nối database
```
Solution: Kiểm tra MySQL service, username/password, database tồn tại
```

### Lỗi giao diện
Đảm bảo chạy trên JDK có hỗ trợ Swing (JDK 17+).

### Lỗi port đã sử dụng
```
Solution: Thay đổi port server hoặc kill process đang sử dụng port
```

### Lỗi build Maven
```bash
# Clean và build lại
mvn clean compile
```

## 📝 API Protocol

### Message Format (JSON)

```json
{
  "type": "REQUEST|RESPONSE|NOTIFICATION|ERROR",
  "action": "login|register|send_invitation|...",  
  "data": {},
  "success": true,
  "message": "...",
  "timestamp": 1234567890
}
```

### Các Actions chính

- `login`, `register`, `logout` - Authentication
- `get_online_users`, `get_leaderboard` - User lists  
- `send_invitation`, `accept_invitation`, `reject_invitation` - Invitations
- `show_colors`, `start_guessing`, `submit_guess` - Gameplay
- `game_started`, `game_ended`, `round_result` - Game events

## 👥 Thành viên nhóm

**NHÓM 6**
- Sinh viên thực hiện: [Tên sinh viên]
- MSSV: [Mã số sinh viên]  
- Lớp: [Lớp học]
- Môn: Lập trình mạng

## 📄 License

Dự án được phát triển cho mục đích học tập tại trường Đại học.

---

## 🚀 Tính năng nâng cao (có thể mở rộng)

- [ ] Chat trong game
- [ ] Spectator mode  
- [ ] Tournament system
- [ ] Custom color themes
- [ ] Sound effects
- [ ] Game replay
- [ ] Mobile client
- [ ] Web client

---

*Cảm ơn bạn đã quan tâm đến dự án Color Guessing Game! 🎨*

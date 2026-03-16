# Stream Platform

Một hệ thống quản lý và phát trực tuyến (streaming platform) toàn diện, bao gồm đầy đủ Frontend và Backend. Hệ thống cho phép quản lý các luồng phát (streams) và tập tin video tự động hóa cao, bao gồm tải và đồng bộ video từ Google Drive, tích hợp YouTube API, cũng như cung cấp một giao diện quản trị chi tiết.

## 🌟 Tính năng nổi bật

- **Quản lý Stream (Admin Dashboard)**: Giao diện trực quan để theo dõi và xem chi tiết quản lý các luồng phát. Thống kê stream được cung cấp dưới dạng scroll liên tục thay vì phân trang.
- **Tích hợp Google Drive & YouTube**: Tự động tải video từ Google Drive thông qua tiện ích (`googleDriveDownloader.js`) và tương tác trực tiếp qua YouTube API.
- **Tự động hóa Quản lý Video**:
  - Tải file tự động với quy tắc đặt tên tuần tự chuyên nghiệp (ví dụ: `ddmmyy_xx.mp4`).
  - Hệ thống tự động dọn dẹp các video "mồ côi" (orphaned videos) trên ổ đĩa VPS định kỳ, giúp tối ưu và giải phóng dung lượng cho máy chủ.
- **Bảo mật và Xác thực**: Xác thực và phân quyền người dùng thông qua Spring Security, đồng bộ hóa mã hoá mật khẩu mới bằng BCrypt, hỗ trợ migration cho mật khẩu cũ.
- **Sẵn sàng triển khai VPS**: Cấu hình network binding (listen trên IP `0.0.0.0`) giúp hệ thống dễ dàng được truy cập trực tiếp khi triển khai trên thiết bị hoặc VPS thật.

## 🛠 Công nghệ sử dụng

### Frontend
- **Framework nền tảng**: React 19 + kết hợp với Vite (Build Tool cực nhanh)
- **Ngôn ngữ/Styling**: JavaScript, SASS/CSS, UI components tùy biến
- **Routing**: React Router DOM (Hỗ trợ Admin routes chuyên biệt)
- **Tương tác HTTP**: Axios để kết nối RESTful API

### Backend
- **Nền tảng chính**: Spring Boot 3.5.11 chạy trên môi trường Java 17
- **Cơ sở dữ liệu**: MySQL kết nối thông qua Spring Data JPA
- **Bảo mật**: Cơ chế tự động của Spring Security + Mã hoá Bcrypt
- **API của bên thứ ba**: Google API Client, Google OAuth Client, YouTube Data API v3
- **Dev-Tools**: Lombok, Spring Boot Actuator, DevTools

## 📂 Tổ chức mã nguồn cơ bản

```text
Stream-Platform/
├── Backend/                    # Mã nguồn cho server (Java/Spring Boot)
│   ├── src/main/java...        # Logic backend (Controllers, Utils ví dụ: PasswordMigration)
│   ├── pom.xml                 # Cấu hình cài đặt phụ thuộc (Maven)
│   └── videos/                 # Dữ liệu video tạm thời tải về từ nguồn bên ngoài
├── Frontend/                   # Mã nguồn cho client (React/Vite)
│   ├── src/                    # Chứa components, pages (Auth login, Admin Streams), utils
│   ├── package.json            # Cấu hình cài đặt phụ thuộc (NPM/Node.js)
│   ├── vite.config.js          # Hệ thống biên dịch Vite
│   └── eslint.config.js        # Cấu hình kiểm tra phong cách code
└── README.md                   # Tài liệu mô tả dự án (File này)
```

## 🚀 Hướng dẫn cài đặt và chạy thử (Môi trường phát triển)

### Yêu cầu hệ thống
- **Java 17** tối thiểu cùng **Maven** (có thể sử dụng `mvnw` có sẵn trong dự án).
- **Node.js** phiên bản LTS (nên ưu tiên >= 18).
- Cơ sở dữ liệu **MySQL** đã khởi chạy cục bộ.

### Bước 1: Khởi động Backend
1. Đi tới thư mục `Backend/`.
2. Kiểm tra/thay đổi thông số cơ sở dữ liệu (Tên DB, user, password) trong các file cấu hình tại `src/main/resources/application.properties`.
3. Chạy các lệnh sau:
```bash
cd Backend
mvn clean install # Tải các thư viện và build dự án
mvn spring-boot:run # Chạy server ở chế độ phát triển
```

### Bước 2: Khởi động Frontend
1. Đi tới thư mục `Frontend/`.
2. Chạy lệnh cài đặt thư viện Node.
3. Chạy lệnh start vite development server:
```bash
cd Frontend
npm install # Cài đặt các gói phụ thuộc dựa trên package.json
npm run dev # Khởi động giao diện trên máy tính cá nhân
```

### Bước 3: Truy cập
- Khi cả hai server đã chạy, truy cập đường dẫn xuất hiện ở Terminal của `npm run dev` (ví dụ `http://localhost:5173/`).
- Quản trị viên sử dụng trang Đăng nhập để truy cập vào Bảng quản khiển Stream.

## 📋 Đăng ký bản quyền
*Nội dung này thuộc hệ thống quản lý nội bộ Stream Platform.*

# BTL_IOT — Hệ thống giám sát thiết bị IoT

Đồ án giám sát & điều khiển thiết bị IoT (nhiệt độ, độ ẩm, ánh sáng, LED) qua ESP8266 + MQTT,
với backend Spring Boot và frontend React.

```
ESP8266 ──MQTT──▶ Mosquitto ──▶ Backend (Spring Boot) ──JPA──▶ MySQL
                                       │
                                       └──REST + STOMP/WebSocket──▶ Frontend (React + Vite)
```

## Cấu trúc thư mục

```
BTL_IOT/
├── backend/          Spring Boot 3.3 / Java 21 — REST API, MQTT, WebSocket, MySQL
│   └── README.md     Chi tiết cấu hình, API, MQTT contract
├── UI/frontend/       React 19 + Vite — Dashboard, Sensor Data, History, Profile
│   ├── INSTRUCTION.md Hướng dẫn đọc code cho intern
│   └── DATA_FLOW.md   Luồng dữ liệu từng trang
└── test/sketch_aug17a/  Firmware ESP8266 (Arduino) dùng trong đồ án
```

## Chạy nhanh (quick start)

**1. Hạ tầng** — MySQL (`localhost:3306`) và Mosquitto MQTT (`localhost:1865`, có auth) phải chạy trước.
`backend/docker-compose.yml` dựng sẵn MySQL nếu máy chưa có; Mosquitto tham khảo
`backend/docker/mosquitto/mosquitto.conf`.

**2. Backend**

```bash
cd backend
cp .env.example .env     # sửa mật khẩu MySQL / broker nếu khác
./run.sh                  # http://localhost:8080
```

Không có board thật thì chạy giả lập ESP8266:

```bash
backend/tools/esp8266-simulator.sh
```

Swagger UI: `http://localhost:8080/swagger-ui/index.html`
Chi tiết API, biến môi trường, MQTT topic: xem [backend/README.md](backend/README.md).

**3. Frontend**

```bash
cd UI/frontend
npm install
npm run dev                # http://localhost:5173
```

`VITE_BACKEND_URL` (mặc định `http://localhost:8080`) trỏ frontend về backend — xem `UI/.env`.
Giao diện: Dashboard (số liệu realtime, chart, điều khiển LED), Sensor Data, History, Profile.
Chi tiết luồng dữ liệu từng trang: xem [UI/frontend/DATA_FLOW.md](UI/frontend/DATA_FLOW.md) và
[UI/frontend/INSTRUCTION.md](UI/frontend/INSTRUCTION.md).

## Realtime

Dashboard nhận dữ liệu cảm biến và trạng thái thiết bị qua REST (poll định kỳ) và qua
WebSocket/STOMP (`/ws/devices`, `/ws/sensors`) để phản ánh đúng thời điểm thiết bị thật phản hồi
(PENDING → SUCCESS/FAILED), không phụ thuộc chu kỳ poll.

## Tech stack

| | |
|---|---|
| Backend | Spring Boot 3.3, Java 21, Spring Data JPA, MySQL, Eclipse Paho (MQTT), STOMP/WebSocket, springdoc-openapi |
| Frontend | React 19, Vite, React Router 7, Chart.js, Axios, @stomp/stompjs |
| Firmware | ESP8266 (Arduino), MQTT |

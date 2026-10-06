# INSTRUCTION.md — Hướng dẫn đọc code cho Intern

Tài liệu này giúp bạn (intern) hiểu nhanh cấu trúc và luồng hoạt động của project **frontend IoT Monitoring System** trước khi bắt đầu code.

---

## 1. Tổng quan project

- **Loại project**: Web app quản lý/giám sát thiết bị IoT (nhiệt độ, độ ẩm, ánh sáng) và điều khiển thiết bị (LED).
- **Stack**: React 19 + Vite + React Router 7 + Chart.js (qua `react-chartjs-2`) + Axios. Có cài `antd` và `@ant-design/icons` trong `package.json` nhưng **hiện chưa dùng ở đâu trong code** — icon trong UI hiện tại là SVG viết tay.
- **Trạng thái hiện tại**: Toàn bộ dữ liệu hiển thị (dashboard, bảng lịch sử, chart...) đang lấy từ **file mock tĩnh** `src/services/mockData.json`, chưa gọi API backend thật. Lớp gọi API (`api.service.js`, `axios.customize.js`) đã có sẵn nhưng **chưa được import/sử dụng** ở bất kỳ page nào.

## 2. Cách chạy project

```bash
npm install
npm run dev       # chạy dev server (Vite)
npm run build     # build production vào dist/
npm run preview   # xem thử bản build
npm run lint      # eslint
```

Backend URL cấu hình qua biến môi trường `VITE_BACKEND_URL` (mặc định `http://localhost:8080`), xem `src/services/axios.customize.js`.

## 3. Cấu trúc thư mục

```
src/
├── main.jsx              # entry point, khai báo router (react-router-dom)
├── App.jsx                # layout khung: Sidebar + Header + <Outlet/>
├── components/
│   └── layout/
│       ├── Sidebar.jsx     # menu điều hướng bên trái
│       ├── Header.jsx      # thanh tiêu đề trên cùng, đổi theo route
│       ├── header.css       # (không được import ở đâu — có thể là file thừa)
│       └── footer.css       # (không được import ở đâu — có thể là file thừa)
├── pages/
│   ├── Dashboard.jsx       # trang chủ "/" — số liệu realtime giả lập + chart + điều khiển thiết bị
│   ├── SensorData.jsx      # "/datasensor" — bảng log dữ liệu cảm biến (search/sort/filter/phân trang)
│   ├── History.jsx         # "/history" — bảng lịch sử bật/tắt thiết bị (search/sort/filter/phân trang)
│   └── Profile.jsx         # "/profile" — trang thông tin cá nhân (tĩnh, chưa có logic)
├── services/
│   ├── mockData.json       # dữ liệu giả lập dùng cho toàn bộ UI hiện tại
│   ├── api.service.js       # các hàm gọi API user (create/fetch/update) — CHƯA được dùng ở page nào
│   └── axios.customize.js   # instance axios dùng chung (baseURL, interceptor log request/response)
├── styles/
│   └── global.css           # toàn bộ CSS của app (1 file duy nhất, ~1100 dòng, dùng CSS variables)
└── assets/                  # ảnh, svg tĩnh
```

`src/routers/` tồn tại nhưng **rỗng** — routing thực tế được khai báo trực tiếp trong `main.jsx`, không tách file riêng.

## 4. Luồng khởi động & routing

`main.jsx` tạo router bằng `createBrowserRouter`:

| Path | Component | Ghi chú |
|---|---|---|
| `/` (index) | `Dashboard` | trang mặc định |
| `/datasensor` | `SensorData` | |
| `/history` | `History` | |
| `/profile` | `Profile` | |

Tất cả các route con nằm trong `App.jsx` (element gốc `"/"`), `App.jsx` render `Sidebar` + `Header` + `<Outlet />` (nơi các page con được render). `Header.jsx` tự suy ra tiêu đề trang bằng cách so khớp `location.pathname` (không dùng route meta/config), nên **nếu thêm route mới phải sửa thêm if/else trong `Header.jsx`**.

## 5. Đọc từng page

### `pages/Dashboard.jsx`
- 3 thẻ thống kê đầu trang (nhiệt độ/độ ẩm/ánh sáng) lấy từ `mockData.liveValues`, kèm hàm `conditionFor()` để chọn ảnh minh họa + nhãn tình trạng (vd: "Hot", "Very humid").
- Biểu đồ (`Line` của chart.js) đọc từ `mockData.CHART_SERIES`, có toggle "Reload chart" chỉ đổi qua bộ dữ liệu `*Alt` giả lập (không gọi API thật).
- Panel "Device Control": danh sách thiết bị lấy từ `mockData.DEVICES`, trạng thái bật/tắt lưu trong `useState` cục bộ (`deviceState`) — **không persist, không gọi API**, refresh trang là mất trạng thái.

### `pages/SensorData.jsx` và `pages/History.jsx`
- Hai file gần như giống hệt nhau về logic (component `Th` để sort cột, hàm `compareRows`, `handleSort`, filter + search + phân trang thủ công). Nếu sửa logic bảng, khả năng cao **phải sửa cả 2 file** vì chưa được tách thành component/hook dùng chung.
- Dữ liệu lấy từ `mockData.sensorLog` / `mockData.historyLog`.
- Phân trang: chia đều số dòng cho 3 "trang" cố định (`PAGE_COUNT = 3`) chứ không phải page-size cố định — cần đọc kỹ nếu định sửa.

### `pages/Profile.jsx`
- Hoàn toàn tĩnh (hardcode thông tin cá nhân, các nút "Edit Profile"/"Change Password" chưa có handler).

## 6. Lớp services (đã có sẵn, chưa dùng)

- `axios.customize.js`: tạo instance axios dùng chung, tự log request, tự unwrap `response.data`, tự chuẩn hoá lỗi trả về.
- `api.service.js`: `createUserAPI`, `fetchAllUsersAPI`, `updateUserAPI` — gọi các endpoint `/api/v1/user`. Chưa có API cho sensor/device/history dù UI đã có sẵn các trang tương ứng.

→ Nếu nhiệm vụ của bạn là **nối API thật**, đây là chỗ cần mở rộng: thêm hàm gọi API cho sensor/device/history theo mẫu `api.service.js`, rồi thay các chỗ `import mockData from '../services/mockData.json'` trong page bằng `useEffect` + state gọi API.

## 7. Styling

- Toàn bộ style nằm trong `src/styles/global.css`, dùng CSS variables (custom properties) khai báo ở đầu file cho theme (màu sắc, spacing...). Không dùng CSS module hay styled-components.
- `header.css` và `footer.css` trong `components/layout/` không được import ở bất kỳ đâu — nhiều khả năng là file cũ còn sót lại, không cần động vào trừ khi được yêu cầu dọn dẹp.

## 8. Vài điểm cần lưu ý khi code tiếp

1. **Dữ liệu đang toàn bộ là mock** — đừng nhầm là app đã kết nối IoT thật.
2. **`SensorData.jsx` và `History.jsx` trùng logic gần như 100%** — nếu sửa bug phân trang/sort ở 1 file, kiểm tra luôn file còn lại.
3. **`Header.jsx` suy tiêu đề bằng match path thủ công** — thêm route mới nhớ cập nhật thêm ở đây.
4. **`antd` đã cài nhưng chưa dùng** — hỏi lại người phụ trách trước khi thêm mới UI framework, tránh trộn lẫn 2 style (SVG thủ công hiện tại vs antd component).
5. **`deviceState` ở Dashboard chỉ là state cục bộ**, không gọi API bật/tắt thiết bị thật — nếu task là điều khiển thiết bị thật, đây là chỗ cần nối logic gọi API + có thể cần thêm loading/error state.

# Luồng dữ liệu trên UI

Tài liệu này chỉ ra từng mảng dữ liệu đang hiển thị trên giao diện (React app trong `src/`) đến từ đâu và đi qua những dòng code nào, để dễ tra cứu khi cần thay bằng API thật.

**Tóm tắt quan trọng nhất:** hiện tại **toàn bộ dữ liệu trên UI đều là mock**, đọc tĩnh từ `src/services/mockData.json`. Lớp gọi API thật (`src/services/api.service.js` + `src/services/axios.customize.js`) đã được viết sẵn nhưng **chưa có trang nào import và dùng** — đây sẽ là điểm cần nối dây khi có backend thật.

## 1. Điểm khởi động & routing

- `src/main.jsx:1-38` — tạo router bằng `createBrowserRouter`, map URL → component trang:
  - `/` → `Dashboard`
  - `/datasensor` → `SensorData`
  - `/history` → `History`
  - `/profile` → `Profile`
- `src/App.jsx:1-21` — layout khung: `Sidebar` bên trái, `Header` + `<Outlet />` (nơi render trang con theo route) bên phải.
- `src/components/layout/Sidebar.jsx` — menu điều hướng và khối tài khoản ở cuối sidebar. Tên "Nguyễn Đức Toàn" / email `toannd.b23cn831@ptit.edu.vn` ở dòng 40-41 là **hard-code trực tiếp trong JSX**, không lấy từ đâu khác.
- `src/components/layout/Header.jsx:4-8` — tiêu đề trên header không lấy từ API, mà suy ra từ `location.pathname` hiện tại (if/else theo URL).

## 2. Trang Dashboard (`src/pages/Dashboard.jsx`)

Nguồn dữ liệu duy nhất: `import mockData from '../services/mockData.json'` (dòng 13).

### 2.1 Ba thẻ chỉ số (nhiệt độ / độ ẩm / ánh sáng)

- Lặp qua `mockData.SENSOR_TYPES` (mảng `{key, label, unit}` trong `mockData.json:2-18`) — dòng 192.
- Với mỗi loại, giá trị hiện tại lấy từ `mockData.liveValues[t.key]` (`mockData.json:36-40`) — dòng 193.
- Giá trị đó được đưa qua `conditionFor(t.key, val)` (dòng 70-74) → gọi `tempCondition` / `humidityCondition` / `lightCondition` (dòng 50-69) để suy ra **ảnh nền + nhãn điều kiện** (VD: "Hot", "Humid", "Bright daylight") dựa trên các ngưỡng số hard-code trong code, ảnh lấy từ `src/assets/weather/*.jpg` qua map `CONDITION_IMAGES` (dòng 34-48).
- Hiển thị ra `stat-label`, `stat-value` + `unit`, `stat-condition` (dòng 200-203).

### 2.2 Biểu đồ cảm biến (Sensor Chart)

- Dropdown `chartType` (state, dòng 119) chọn hiển thị 1 hay cả 3 loại cảm biến.
- Nút "Reload chart" (`reloadChart`, dòng 132-136) chỉ **đảo cờ `useAltData`** để chuyển dữ liệu hiển thị sang bộ dữ liệu "Alt" có sẵn trong file mock — không gọi API nào, chỉ là dữ liệu giả lập thứ hai.
- `getDatasets()` (dòng 138-157) đọc từ `mockData.CHART_SERIES[key]` (`mockData.json:75-193`, mỗi sensor có `label/color/data/dataAlt`) và `mockData.SENSOR_TYPES` để lấy đơn vị.
- Trục X (nhãn thời gian) lấy từ `mockData.CHART_LABELS` hoặc `mockData.CHART_LABELS_ALT` (`mockData.json:41-74`) — dòng 160.
- `chartData`/`chartOptions` (dòng 159-187) được truyền vào component `<Line>` của `react-chartjs-2` (dòng 229) để vẽ.
- Tooltip tùy chỉnh khi hover điểm trên biểu đồ: `renderChartTooltip` (dòng 86-116) đọc lại giá trị điểm (`dp.parsed.y`) và gọi `conditionFor` để hiện thêm nhãn điều kiện trong tooltip.

### 2.3 Device Control panel

- Danh sách thiết bị lấy từ `mockData.DEVICES` (`mockData.json:19-35`, mỗi thiết bị có `name/type/icon`) — dòng 236.
- Trạng thái ON/OFF là **state cục bộ trong component** `deviceState` (dòng 122-126, khởi tạo cứng `LED 1: true, LED 2/3: false`), đổi bằng `toggleDevice` (dòng 128-130) khi bấm switch — **không gửi lệnh gì ra ngoài**, chỉ đổi UI tại chỗ.

## 3. Trang Sensor Data (`src/pages/SensorData.jsx`)

- Nguồn dữ liệu: `mockData.sensorLog` (`mockData.json:194-501` — mảng log cảm biến với `id/type/value/time`).
- Lọc theo loại cảm biến + ô tìm kiếm: `filtered = mockData.sensorLog.filter(...)` — dòng 43-53.
- Sắp xếp theo cột đang chọn (`field`/`dir` state) qua `compareRows` — dòng 26-31, 55.
- Phân trang: `PAGE_SIZE` tự tính theo chiều cao khung nhìn qua hook `useFitPageSize` (`src/hooks/useFitPageSize.js`, đo DOM bằng `getBoundingClientRect` — dòng 11-35) hoặc người dùng tự nhập ở ô "Rows/page" (`rowsInput` state) — dòng 57-75.
- Mỗi dòng bảng (`pageRows.map`, dòng 104-111) render trực tiếp `row.id/type/value` và `fmtDate(row.time)` (định dạng lại ISO date, dòng 21-24).

## 4. Trang History (`src/pages/History.jsx`)

- Nguồn dữ liệu: `mockData.historyLog` (`mockData.json:502-911` — log thao tác thiết bị: `id/device/action/performedBy/status/time`).
- Lọc theo thiết bị, hành động (ON/OFF), trạng thái (success/failed/pending/loading), từ khóa, và mốc thời gian — `filtered = mockData.historyLog.filter(...)` dòng 41-52.
- Sắp xếp/phân trang dùng cùng cơ chế như `SensorData.jsx` (`compareRows`, `useFitPageSize`, `rowsInput`) — dòng 24-29, 56-74.
- Mỗi dòng bảng (dòng 133-142) render `row.id/device/performedBy/action/status` và `fmtDate(row.time)` (dòng 76-79).

## 5. Trang Profile (`src/pages/Profile.jsx`)

- **Không đọc từ `mockData.json` hay API nào cả** — toàn bộ nội dung (tên, MSSV, email, trường, mô tả "About Me", các link GitHub/Figma/Postman/PDF...) là **text hard-code thẳng trong JSX** (dòng 7, 44, 56, 68, 79, 90, và các `href="#"` placeholder ở dòng 98-139).
- Các nút "Edit Profile" / "Change Password" (dòng 10-22) chưa gắn `onClick`, chỉ là UI tĩnh.

## 6. Lớp gọi API (đã viết nhưng chưa được dùng ở đâu)

- `src/services/axios.customize.js` — tạo instance `axios` với `baseURL` lấy từ biến môi trường `VITE_BACKEND_URL` (mặc định `http://localhost:8080`), có interceptor log request và unwrap `response.data` / chuẩn hóa lỗi.
- `src/services/api.service.js` — export 3 hàm: `createUserAPI`, `fetchAllUsersAPI`, `updateUserAPI`, gọi tới `/api/v1/user`.
- Hiện **không có file nào trong `src/pages` hoặc `src/components` import các hàm này** — nghĩa là các trang Dashboard/SensorData/History/Profile đều đang chạy hoàn toàn bằng dữ liệu tĩnh, chưa nối với backend.

## 7. Sơ đồ tổng quát

```
mockData.json ──> Dashboard.jsx    ──> thẻ chỉ số / biểu đồ / device control
             ├──> SensorData.jsx  ──> bảng log cảm biến
             └──> History.jsx     ──> bảng lịch sử thao tác thiết bị

Profile.jsx                        ──> text hard-code (không có nguồn dữ liệu)
Sidebar.jsx / Header.jsx           ──> text hard-code + location.pathname

api.service.js + axios.customize.js ──> viết sẵn, CHƯA được gọi ở trang nào
```

## 8. Ghi chú cho `giaodien/` và `figma-export/`

Hai thư mục `giaodien/` và `figma-export/` ở gốc dự án là các bản HTML/JS tĩnh tách biệt (không phải React app đang chạy qua Vite) — có `data.js`/`app.js` riêng, không liên quan đến luồng dữ liệu của `src/` mô tả ở trên.

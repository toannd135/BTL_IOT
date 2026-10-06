# IoT Monitor Backend

Spring Boot 3.3 / Java 21 backend for the *Hệ thống giám sát thiết bị IoT* project.
Implements the REST, MQTT and WebSocket contracts from the design document (chapter 3.3).

```
ESP8266 ──MQTT──▶ Mosquitto ──▶ Backend ──JPA──▶ MySQL
                                   │
                                   └──STOMP/WebSocket──▶ React frontend
```

## Run

```bash
cp .env.example .env     # edit if your MySQL password or broker differ
./run.sh                 # loads .env, then mvn spring-boot:run
```

`run.sh` only loads the environment; `mvn spring-boot:run` works too if you export
the variables yourself.

### Infrastructure

This machine already runs both services, so `docker compose up -d` is only a
fallback (and needs Docker Hub access, which a proxy may block):

| Service | Where | Notes |
|---|---|---|
| MySQL | `localhost:3306` | user `root`, password `123456` on this machine |
| Mosquitto | `localhost:1865` | **requires auth** — user `toannd135`, password `b23dccn831` |

Port 1865 and those credentials are the ones the ESP8266 firmware uses
(`../test/sketch_aug17a/sketch_aug17a.ino`), so backend and board meet on the same
broker. The bundled `docker-compose.yml` instead starts an anonymous broker on
1883; if you use it, change `MQTT_BROKER_URL` and clear the credentials.

When the board runs on real hardware, point both the firmware and
`MQTT_BROKER_URL` at this machine's LAN address rather than `localhost`.

### Demo without hardware

`tools/esp8266-simulator.sh` stands in for the board: it publishes a sensor
reading every few seconds and answers every control command with `SUCCESS`.
Without it (and without the real board) every command stays PENDING and is
marked FAILED after `PENDING_TIMEOUT` seconds.

```bash
tools/esp8266-simulator.sh        # reads .env for the broker settings
SIM_INTERVAL=3 tools/esp8266-simulator.sh
```

The schema (`src/main/resources/schema.sql`) and seed rows (`data.sql`: users `admin`/`toanndc`,
devices `LED 1..3`, sensors `temperature/humidity/light`) are applied automatically on start.

Run tests with `mvn test`.

## Configuration (env vars)

| Variable | Default | Meaning |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | localhost / 3306 / iot_monitor / root / root | MySQL |
| `MQTT_BROKER_URL` | `tcp://localhost:1883` | Broker the ESP8266 publishes to (this project: `tcp://localhost:1865`) |
| `MQTT_USERNAME` / `MQTT_PASSWORD` | empty | Broker credentials |
| `MQTT_ENABLED` | `true` | Set `false` to boot without a broker |
| `CORS_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Allowed frontend origins |
| `DEFAULT_USER_ID` | `1` | `user_id` recorded when the control request omits it |
| `PENDING_TIMEOUT` | `10` | Seconds before a PENDING action is marked FAILED |

## API docs (Swagger)

Swagger UI: `http://localhost:8080/swagger-ui/index.html`
Raw OpenAPI spec: `http://localhost:8080/v3/api-docs`

## REST API (`/api/v1`)

Every response uses the envelope `{ status, code, message, pagination?, data, error_detail? }`.

| Method | Endpoint | Notes |
|---|---|---|
| GET | `/sensors/latest` | `{ temperature, humidity, light, timestamp }` |
| GET | `/sensors/chart?type=&limit=20` | `type` = `temperature|humidity|light|<sensor_id>|all`. Points ascending by time. |
| GET | `/sensors/sensor-data` | Query: `name`, `search`, `from`, `to`, `page`, `limit`, `sort_by` (`id|value|time|sensor`), `sort_dir` |
| POST | `/devices/control` | Body `{ "device_id": 1, "action": "ON", "user_id"?: 1 }` → publishes MQTT, logs PENDING action |
| GET | `/devices` | Devices with current state (from the last SUCCESS action) |
| GET | `/devices/{id}` | |
| GET | `/actions/history` | Query: `status` (`success|failed|pending`), `device_id`, `action`, `search`, `from`, `to`, `page`, `limit`, `sort_by` (`id|action|status|time|device|user`), `sort_dir` |
| GET | `/actions/{id}` | |
| GET | `/health` | `{ mqtt_connected }` |

Errors: `400` → `status: "fail"`, `404`, `500` → `status: "error"` with `error_detail`.

## MQTT

| Topic | Direction | Payload |
|---|---|---|
| `/esp8266/sensors-data` | ESP → backend | `{ "data": [ { "temperature", "humidity", "light", "timestamp" } ] }` |
| `/esp8266/device-control` | backend → ESP | `{ "device_id", "action", "user_id" }` |
| `/esp8266/device-response` | ESP → backend | `{ "device_id", "user_id", "action", "status", "time"? }` |

The client reconnects automatically; the HTTP API still starts when the broker is down
(control requests then return `500 Device is offline or MQTT broker connection failed`).

## WebSocket (STOMP)

Connect to `ws://localhost:8080/ws/sensors` or `/ws/devices` (SockJS fallback on the same paths), then subscribe:

| Destination | Message |
|---|---|
| `/topic/sensors` | `{ temperature, humidity, light, timestamp }` on every ingested reading |
| `/topic/devices/{device_id}/status` | `{ action_id, device_id, user_id, action, status, timestamp }` when the device answers, or the command times out |

Frontend example (`@stomp/stompjs`):

```js
const client = new Client({ brokerURL: 'ws://localhost:8080/ws/sensors' });
client.onConnect = () => {
  client.subscribe('/topic/sensors', m => setLive(JSON.parse(m.body)));
  client.subscribe('/topic/devices/1/status', m => setLedStatus(JSON.parse(m.body)));
};
client.activate();
```

## Manual test without hardware

Either run the simulator above, or drive the topics by hand (add
`-h localhost -p 1865 -u toannd135 -P b23dccn831` to each mosquitto command when
using the project broker):

```bash
mosquitto_pub -t /esp8266/sensors-data -m '{"data":[{"temperature":30.8,"humidity":86.0,"light":49,"timestamp":"2026-08-26T10:33:34"}]}'
curl -s localhost:8080/api/v1/sensors/latest
curl -s -X POST localhost:8080/api/v1/devices/control -H 'Content-Type: application/json' -d '{"device_id":1,"action":"ON"}'
mosquitto_pub -t /esp8266/device-response -m '{"device_id":1,"user_id":1,"action":"ON","status":"SUCCESS"}'
curl -s 'localhost:8080/api/v1/actions/history?status=success'
```

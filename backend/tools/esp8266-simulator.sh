#!/usr/bin/env bash
# Stands in for the ESP8266 board so the system can be demoed without hardware.
#   - publishes a sensor reading every few seconds on /esp8266/sensors-data
#   - answers every command on /esp8266/device-control with SUCCESS on /esp8266/device-response
#
# Usage: tools/esp8266-simulator.sh            (reads ../.env for broker settings)
#        MQTT_BROKER_URL=tcp://host:1865 MQTT_USERNAME=u MQTT_PASSWORD=p tools/esp8266-simulator.sh
set -uo pipefail
cd "$(dirname "$0")/.."

if [ -f .env ]; then set -a; source .env; set +a
elif [ -f .env.example ]; then set -a; source .env.example; set +a
fi

BROKER="${MQTT_BROKER_URL:-tcp://localhost:1883}"
HOST="$(echo "$BROKER" | sed -E 's#^[a-z]+://##; s#:[0-9]+$##')"
PORT="$(echo "$BROKER" | sed -E 's#.*:([0-9]+)$#\1#')"
AUTH=()
[ -n "${MQTT_USERNAME:-}" ] && AUTH=(-u "$MQTT_USERNAME" -P "${MQTT_PASSWORD:-}")

SENSOR_TOPIC=/esp8266/sensors-data
CONTROL_TOPIC=/esp8266/device-control
RESPONSE_TOPIC=/esp8266/device-response
INTERVAL="${SIM_INTERVAL:-5}"

echo "ESP8266 simulator -> $HOST:$PORT (interval ${INTERVAL}s). Ctrl+C to stop."

cleanup() { kill 0 2>/dev/null; }
trap cleanup EXIT INT TERM

# --- sensor loop: temperature / humidity / light, like the real firmware ---
(
  while true; do
    payload=$(python3 - <<'PY'
import json, random, datetime
print(json.dumps({"data": [{
    "temperature": round(random.uniform(24, 34), 1),
    "humidity": round(random.uniform(45, 85), 1),
    "light": random.randint(50, 900),
    "timestamp": datetime.datetime.now().strftime("%Y-%m-%dT%H:%M:%S")
}]}))
PY
)
    mosquitto_pub -h "$HOST" -p "$PORT" "${AUTH[@]}" -t "$SENSOR_TOPIC" -m "$payload" \
      || echo "[sim] publish failed - is the broker up?"
    sleep "$INTERVAL"
  done
) &

# --- control loop: acknowledge every command the backend sends ---
mosquitto_sub -h "$HOST" -p "$PORT" "${AUTH[@]}" -t "$CONTROL_TOPIC" | while read -r line; do
  [ -z "$line" ] && continue
  echo "[sim] command: $line"
  response=$(python3 -c '
import json, sys
cmd = json.loads(sys.argv[1])
print(json.dumps({
    "device_id": cmd.get("device_id"),
    "user_id": cmd.get("user_id", 1),
    "action": cmd.get("action"),
    "status": "SUCCESS",
}))' "$line" 2>/dev/null) || { echo "[sim] unreadable command, ignored"; continue; }
  mosquitto_pub -h "$HOST" -p "$PORT" "${AUTH[@]}" -t "$RESPONSE_TOPIC" -m "$response"
  echo "[sim] replied: $response"
done

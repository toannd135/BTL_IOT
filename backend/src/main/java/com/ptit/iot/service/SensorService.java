package com.ptit.iot.service;

import com.ptit.iot.common.BadRequestException;
import com.ptit.iot.config.AppProperties;
import com.ptit.iot.dto.ChartPoint;
import com.ptit.iot.dto.LatestSensorResponse;
import com.ptit.iot.dto.SensorDataResponse;
import com.ptit.iot.entity.DataSensor;
import com.ptit.iot.entity.Sensor;
import com.ptit.iot.mqtt.SensorDataPayload;
import com.ptit.iot.repository.DataSensorRepository;
import com.ptit.iot.repository.SensorRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class SensorService {
    private static final Logger log = LoggerFactory.getLogger(SensorService.class);
    public static final String TEMPERATURE = "temperature";
    public static final String HUMIDITY = "humidity";
    public static final String LIGHT = "light";
    private static final Set<String> SORTABLE = Set.of("id", "value", "time", "sensor");

    private final SensorRepository sensorRepository;
    private final DataSensorRepository dataSensorRepository;
    private final RealtimePublisher realtime;
    private final AppProperties props;

    public SensorService(SensorRepository sensorRepository, DataSensorRepository dataSensorRepository,
                         RealtimePublisher realtime, AppProperties props) {
        this.sensorRepository = sensorRepository;
        this.dataSensorRepository = dataSensorRepository;
        this.realtime = realtime;
        this.props = props;
    }

    // ---------- ingest (MQTT) ----------

    /** Stores one reading (3 rows in data_sensors) and pushes it over WebSocket. */
    @Transactional
    public LatestSensorResponse ingest(SensorDataPayload.Reading reading) {
        LocalDateTime time = TimeParser.parseOrNow(reading.timestamp());
        Map<String, Sensor> sensors = sensorsByName();
        List<DataSensor> rows = new ArrayList<>(3);
        if (reading.temperature() != null) rows.add(new DataSensor(sensors.get(TEMPERATURE), reading.temperature(), time));
        if (reading.humidity() != null)    rows.add(new DataSensor(sensors.get(HUMIDITY), reading.humidity(), time));
        if (reading.light() != null)       rows.add(new DataSensor(sensors.get(LIGHT), reading.light(), time));
        if (rows.isEmpty()) {
            log.warn("Sensor payload contained no values: {}", reading);
            return null;
        }
        dataSensorRepository.saveAll(rows);
        LatestSensorResponse latest = new LatestSensorResponse(reading.temperature(), reading.humidity(), reading.light(), time);
        realtime.publishSensorData(latest);
        return latest;
    }

    private Map<String, Sensor> sensorsByName() {
        Map<String, Sensor> map = new HashMap<>();
        for (Sensor s : sensorRepository.findAll()) map.put(s.getName().toLowerCase(), s);
        for (String required : List.of(TEMPERATURE, HUMIDITY, LIGHT)) {
            if (!map.containsKey(required)) {
                Sensor s = new Sensor();
                s.setName(required);
                map.put(required, sensorRepository.save(s));
            }
        }
        return map;
    }

    // ---------- 3.3.1 latest ----------

    @Transactional(readOnly = true)
    public LatestSensorResponse latest() {
        Map<String, Sensor> sensors = sensorsByName();
        DataSensor t = dataSensorRepository.findTopBySensorIdOrderByTimeDescIdDesc(sensors.get(TEMPERATURE).getId()).orElse(null);
        DataSensor h = dataSensorRepository.findTopBySensorIdOrderByTimeDescIdDesc(sensors.get(HUMIDITY).getId()).orElse(null);
        DataSensor l = dataSensorRepository.findTopBySensorIdOrderByTimeDescIdDesc(sensors.get(LIGHT).getId()).orElse(null);
        LocalDateTime ts = null;
        for (DataSensor d : Arrays.asList(t, h, l)) {
            if (d != null && (ts == null || d.getTime().isAfter(ts))) ts = d.getTime();
        }
        return new LatestSensorResponse(
                t == null ? null : t.getValue(),
                h == null ? null : h.getValue(),
                l == null ? null : l.getValue(),
                ts);
    }

    // ---------- 3.3.2 chart ----------

    /**
     * Returns the last {@code limit} time points, ascending by time.
     * {@code type} may be a sensor id ("1") or name ("temperature"); null/"all" returns every sensor.
     */
    @Transactional(readOnly = true)
    public List<ChartPoint> chart(String type, Integer limit) {
        int points = (limit == null || limit <= 0) ? props.chart().defaultPoints() : Math.min(limit, 1000);
        Sensor only = resolveSensorFilter(type);

        List<DataSensor> rows = only == null
                ? dataSensorRepository.findLatest(PageRequest.of(0, points * 3))
                : dataSensorRepository.findLatestBySensor(only.getId(), PageRequest.of(0, points));

        // group by timestamp (all three values of a reading share the same time)
        TreeMap<LocalDateTime, double[]> grouped = new TreeMap<>();
        for (DataSensor d : rows) {
            double[] slot = grouped.computeIfAbsent(d.getTime(), k -> new double[]{Double.NaN, Double.NaN, Double.NaN});
            switch (d.getSensor().getName().toLowerCase()) {
                case TEMPERATURE -> slot[0] = d.getValue();
                case HUMIDITY -> slot[1] = d.getValue();
                case LIGHT -> slot[2] = d.getValue();
                default -> { }
            }
        }
        // keep only the newest `points` timestamps
        while (grouped.size() > points) grouped.pollFirstEntry();

        List<ChartPoint> result = new ArrayList<>(grouped.size());
        grouped.forEach((time, v) -> result.add(new ChartPoint(time, nanToNull(v[0]), nanToNull(v[1]), nanToNull(v[2]))));
        return result;
    }

    private static Double nanToNull(double v) {
        return Double.isNaN(v) ? null : v;
    }

    private Sensor resolveSensorFilter(String type) {
        if (type == null || type.isBlank() || type.equalsIgnoreCase("all")) return null;
        Optional<Sensor> found;
        if (type.chars().allMatch(Character::isDigit)) {
            found = sensorRepository.findById(Integer.parseInt(type));
        } else {
            found = sensorRepository.findByNameIgnoreCase(type.trim());
        }
        return found.orElseThrow(() -> new BadRequestException("Invalid request parameters",
                "Unknown sensor type '" + type + "'. Use a sensor id or one of: temperature, humidity, light"));
    }

    // ---------- 3.3.4 sensor-data ----------

    /**
     * Paginated, filterable list of raw measurements.
     * @param name   sensor name or id filter (optional)
     * @param search matches id or value text (optional)
     * @param from/to time range (optional)
     */
    @Transactional(readOnly = true)
    public Page<SensorDataResponse> sensorData(String name, String search, LocalDateTime from, LocalDateTime to,
                                               int page, int limit, String sortBy, String sortDir) {
        Sensor only = resolveSensorFilter(name);
        String sortField = (sortBy == null || !SORTABLE.contains(sortBy)) ? "time" : sortBy;
        Sort.Direction dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = sortField.equals("sensor")
                ? Sort.by(dir, "sensor.name").and(Sort.by(Sort.Direction.DESC, "time"))
                : Sort.by(dir, sortField).and(Sort.by(Sort.Direction.DESC, "id"));

        Specification<DataSensor> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (only != null) ps.add(cb.equal(root.get("sensor").get("id"), only.getId()));
            if (from != null) ps.add(cb.greaterThanOrEqualTo(root.get("time"), from));
            if (to != null) ps.add(cb.lessThanOrEqualTo(root.get("time"), to));
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(root.get("id").as(String.class), like),
                        cb.like(root.get("value").as(String.class), like),
                        cb.like(cb.lower(root.get("sensor").get("name")), like)));
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };

        return dataSensorRepository.findAll(spec, PageRequest.of(Math.max(page - 1, 0), clampLimit(limit), sort))
                .map(SensorDataResponse::from);
    }

    static int clampLimit(int limit) {
        if (limit <= 0) return 10;
        return Math.min(limit, 1000);
    }
}

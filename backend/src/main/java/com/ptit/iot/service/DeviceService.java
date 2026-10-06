package com.ptit.iot.service;

import com.ptit.iot.common.BadRequestException;
import com.ptit.iot.common.DeviceControlException;
import com.ptit.iot.common.NotFoundException;
import com.ptit.iot.config.AppProperties;
import com.ptit.iot.dto.DeviceControlRequest;
import com.ptit.iot.dto.DeviceControlResponse;
import com.ptit.iot.dto.DeviceResponse;
import com.ptit.iot.dto.DeviceStatusMessage;
import com.ptit.iot.entity.Action;
import com.ptit.iot.entity.ActionStatus;
import com.ptit.iot.entity.Device;
import com.ptit.iot.entity.User;
import com.ptit.iot.mqtt.DeviceControlPayload;
import com.ptit.iot.mqtt.DeviceResponsePayload;
import com.ptit.iot.mqtt.MqttGateway;
import com.ptit.iot.repository.ActionRepository;
import com.ptit.iot.repository.DeviceRepository;
import com.ptit.iot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class DeviceService {
    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);
    private static final Set<String> VALID_ACTIONS = Set.of("ON", "OFF");

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final ActionRepository actionRepository;
    private final MqttGateway mqtt;
    private final RealtimePublisher realtime;
    private final AppProperties props;

    public DeviceService(DeviceRepository deviceRepository, UserRepository userRepository,
                         ActionRepository actionRepository, MqttGateway mqtt,
                         RealtimePublisher realtime, AppProperties props) {
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
        this.actionRepository = actionRepository;
        this.mqtt = mqtt;
        this.realtime = realtime;
        this.props = props;
    }

    // ---------- GET /devices ----------

    @Transactional(readOnly = true)
    public List<DeviceResponse> listDevices() {
        return deviceRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DeviceResponse getDevice(Integer id) {
        return toResponse(requireDevice(id));
    }

    /**
     * {@code state} is the device's last confirmed ON/OFF (from the last SUCCESS action), so it
     * doesn't flicker while a newer command is still in flight. {@code last_status}/{@code
     * last_action_id}/{@code updated_at} describe the truly most recent action instead (which may
     * be PENDING or FAILED), so the frontend can show a pending/spinner state on the switch.
     */
    private DeviceResponse toResponse(Device d) {
        List<Action> lastSuccess = actionRepository.findLastSuccess(d.getId(), PageRequest.of(0, 1));
        String state = lastSuccess.isEmpty() ? "OFF" : lastSuccess.get(0).getAction();

        List<Action> lastAny = actionRepository.findLastAny(d.getId(), PageRequest.of(0, 1));
        if (lastAny.isEmpty()) return new DeviceResponse(d.getId(), d.getName(), state, null, null, null);
        Action a = lastAny.get(0);
        return new DeviceResponse(d.getId(), d.getName(), state, a.getId(), a.getStatus(), a.getTime());
    }

    // ---------- 3.3.3 POST /devices/control ----------

    /**
     * Validates, logs a PENDING action, publishes the command over MQTT and returns immediately.
     * The final SUCCESS/FAILED state arrives asynchronously via {@link #handleDeviceResponse}.
     */
    @Transactional(noRollbackFor = DeviceControlException.class)
    public DeviceControlResponse control(DeviceControlRequest req) {
        String action = req.action() == null ? "" : req.action().trim().toUpperCase();
        if (!VALID_ACTIONS.contains(action)) {
            throw new BadRequestException("Invalid request parameters", "The 'action' field must be either 'ON' or 'OFF'");
        }
        Device device = requireDevice(req.deviceId());
        Integer userId = req.userId() != null ? req.userId() : props.defaultUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Invalid request parameters", "Unknown user_id " + userId));

        LocalDateTime now = LocalDateTime.now().withNano(0);
        Action pending = actionRepository.save(new Action(device, user, action, ActionStatus.PENDING, now));

        try {
            mqtt.publishDeviceControl(new DeviceControlPayload(device.getId(), action, user.getId()));
        } catch (DeviceControlException e) {
            pending.setStatus(ActionStatus.FAILED);
            actionRepository.save(pending);
            realtime.publishDeviceStatus(DeviceStatusMessage.from(pending));
            throw e;
        }

        log.info("Control command queued: action#{} device={} action={} user={}", pending.getId(), device.getId(), action, user.getId());
        return new DeviceControlResponse(device.getId(), action, now, pending.getId(), pending.getStatus());
    }

    // ---------- 3.3.8 MQTT device-response ----------

    /**
     * Resolves the oldest matching PENDING action for the device (or creates a row when the command
     * did not originate from this backend), updates status/time and pushes the result over WebSocket.
     */
    @Transactional
    public void handleDeviceResponse(DeviceResponsePayload p) {
        if (p.deviceId() == null) {
            log.warn("device-response without device_id ignored: {}", p);
            return;
        }
        ActionStatus status = ActionStatus.fromString(p.status());
        if (status == null || status == ActionStatus.PENDING) status = ActionStatus.FAILED;
        String action = p.action() == null ? "UNKNOWN" : p.action().trim().toUpperCase();
        LocalDateTime time = TimeParser.parseOrNow(p.time());

        Device device = deviceRepository.findById(p.deviceId()).orElse(null);
        if (device == null) {
            log.warn("device-response for unknown device {} ignored", p.deviceId());
            return;
        }

        List<Action> pendings = actionRepository.findByDeviceAndStatus(device.getId(), ActionStatus.PENDING.name(), PageRequest.of(0, 20));
        Action target = pendings.stream()
                .filter(a -> a.getAction().equalsIgnoreCase(action))
                .filter(a -> p.userId() == null || a.getUser().getId().equals(p.userId()))
                .reduce((first, second) -> second) // oldest matching (list is newest-first)
                .orElse(null);

        if (target == null) {
            User user = (p.userId() == null ? userRepository.findById(props.defaultUserId()) : userRepository.findById(p.userId()))
                    .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));
            if (user == null) {
                log.warn("No user available to attribute device-response {}", p);
                return;
            }
            target = new Action(device, user, action, status, time);
        } else {
            target.setStatus(status);
            target.setTime(time);
        }
        target = actionRepository.save(target);
        log.info("Device response: action#{} device={} action={} status={}", target.getId(), device.getId(), action, status);
        realtime.publishDeviceStatus(DeviceStatusMessage.from(target));
    }

    // ---------- pending timeout ----------

    /** Marks commands that never got a device response as FAILED so the UI leaves the "Pending" state. */
    @Scheduled(fixedDelayString = "${app.device.pending-timeout-seconds:10}000", initialDelay = 10_000)
    @Transactional
    public void expirePendingActions() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(props.device().pendingTimeoutSeconds());
        List<Action> stale = actionRepository.findStale(ActionStatus.PENDING.name(), cutoff);
        for (Action a : stale) {
            a.setStatus(ActionStatus.FAILED);
            actionRepository.save(a);
            realtime.publishDeviceStatus(DeviceStatusMessage.from(a));
            log.warn("Action#{} timed out waiting for device {} -> FAILED", a.getId(), a.getDevice().getId());
        }
    }

    private Device requireDevice(Integer id) {
        if (id == null) throw new BadRequestException("Invalid request parameters", "device_id is required");
        return deviceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Device " + id + " not found"));
    }
}

package com.ptit.iot.service;

import com.ptit.iot.common.BadRequestException;
import com.ptit.iot.common.NotFoundException;
import com.ptit.iot.dto.ActionHistoryResponse;
import com.ptit.iot.entity.Action;
import com.ptit.iot.entity.ActionStatus;
import com.ptit.iot.repository.ActionRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ActionService {
    private static final Set<String> SORTABLE = Set.of("id", "action", "status", "time", "device", "user");

    private final ActionRepository actionRepository;

    public ActionService(ActionRepository actionRepository) {
        this.actionRepository = actionRepository;
    }

    /** 3.3.5 GET /api/v1/actions/history with optional filters. */
    @Transactional(readOnly = true)
    public Page<ActionHistoryResponse> history(String status, Integer deviceId, String action, String search,
                                               LocalDateTime from, LocalDateTime to,
                                               int page, int limit, String sortBy, String sortDir) {
        ActionStatus statusFilter = null;
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("all")) {
            statusFilter = ActionStatus.fromString(status);
            if (statusFilter == null) {
                throw new BadRequestException("Invalid request parameters",
                        "status must be one of: success, failed, pending");
            }
        }
        String actionFilter = (action == null || action.isBlank() || action.equalsIgnoreCase("all")) ? null : action.trim().toUpperCase();

        String sortField = (sortBy == null || !SORTABLE.contains(sortBy)) ? "time" : sortBy;
        Sort.Direction dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = switch (sortField) {
            case "device" -> Sort.by(dir, "device.name");
            case "user" -> Sort.by(dir, "user.username");
            default -> Sort.by(dir, sortField);
        };
        sort = sort.and(Sort.by(Sort.Direction.DESC, "id"));

        final ActionStatus st = statusFilter;
        Specification<Action> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (st != null) ps.add(cb.equal(root.get("status"), st.name()));
            if (deviceId != null) ps.add(cb.equal(root.get("device").get("id"), deviceId));
            if (actionFilter != null) ps.add(cb.equal(root.get("action"), actionFilter));
            if (from != null) ps.add(cb.greaterThanOrEqualTo(root.get("time"), from));
            if (to != null) ps.add(cb.lessThanOrEqualTo(root.get("time"), to));
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(root.get("id").as(String.class), like),
                        cb.like(cb.lower(root.get("user").get("username")), like),
                        cb.like(cb.lower(root.get("device").get("name")), like)));
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };

        return actionRepository.findAll(spec, PageRequest.of(Math.max(page - 1, 0), SensorService.clampLimit(limit), sort))
                .map(ActionHistoryResponse::from);
    }

    @Transactional(readOnly = true)
    public ActionHistoryResponse get(Integer id) {
        return actionRepository.findById(id).map(ActionHistoryResponse::from)
                .orElseThrow(() -> new NotFoundException("Action " + id + " not found"));
    }
}

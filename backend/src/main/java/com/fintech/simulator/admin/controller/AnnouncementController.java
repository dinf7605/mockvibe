package com.fintech.simulator.admin.controller;

import com.fintech.simulator.admin.audit.Auditable;
import com.fintech.simulator.admin.domain.Announcement;
import com.fintech.simulator.admin.domain.AnnouncementLevel;
import com.fintech.simulator.admin.repository.AnnouncementRepository;
import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

/**
 * 관리자 - 공지사항 CRUD (PRD FR-10.20).
 */
@RestController
@RequestMapping("/admin/announcements")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementRepository announcementRepository;

    @GetMapping
    public Page<Announcement> list(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return announcementRepository.findByOrderByCreatedAtDesc(PageRequest.of(page, Math.min(size, 100)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Auditable(action = "CREATE_ANNOUNCEMENT", targetType = "ANNOUNCEMENT")
    @Transactional
    public Announcement create(@AuthenticationPrincipal String adminUserId,
                               @Valid @RequestBody UpsertRequest req) {
        return announcementRepository.save(
                Announcement.create(adminUserId, req.title(), req.content(),
                        req.level(), req.startsAt(), req.endsAt()));
    }

    @PutMapping("/{id}")
    @Auditable(action = "UPDATE_ANNOUNCEMENT", targetType = "ANNOUNCEMENT", targetIdParam = "id")
    @Transactional
    public Announcement update(@PathVariable Long id, @Valid @RequestBody UpsertRequest req) {
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));
        a.update(req.title(), req.content(), req.level(), req.startsAt(), req.endsAt());
        return a;
    }

    @PostMapping("/{id}/toggle")
    @Auditable(action = "TOGGLE_ANNOUNCEMENT", targetType = "ANNOUNCEMENT", targetIdParam = "id")
    @Transactional
    public Announcement toggle(@PathVariable Long id) {
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));
        a.toggleActive(!a.isActive());
        return a;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Auditable(action = "DELETE_ANNOUNCEMENT", targetType = "ANNOUNCEMENT", targetIdParam = "id")
    @Transactional
    public void delete(@PathVariable Long id) {
        if (!announcementRepository.existsById(id))
            throw new BusinessException(ErrorCode.ANNOUNCEMENT_NOT_FOUND);
        announcementRepository.deleteById(id);
    }

    public record UpsertRequest(
            @NotBlank String title,
            @NotBlank String content,
            AnnouncementLevel level,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt
    ) {}
}

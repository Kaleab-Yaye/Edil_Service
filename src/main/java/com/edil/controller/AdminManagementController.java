package com.edil.controller;

import com.edil.dto.request.CreateAdminRequest;
import com.edil.dto.response.AdminUserResponse;
import com.edil.service.AdminManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/management")
@PreAuthorize("hasRole('ROOT_ADMIN')")
@RequiredArgsConstructor
public class AdminManagementController {

    private final AdminManagementService adminManagementService;

    @GetMapping("/admins")
    public ResponseEntity<List<AdminUserResponse>> getAllAdmins() {
        return ResponseEntity.ok(adminManagementService.getAllAdmins());
    }

    @PostMapping("/admins")
    public ResponseEntity<AdminUserResponse> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminManagementService.createAdmin(request));
    }

    @PatchMapping("/admins/{accountId}/status")
    public ResponseEntity<Void> toggleStatus(@PathVariable UUID accountId, @RequestParam boolean active) {
        adminManagementService.toggleAdminStatus(accountId, active);
        return ResponseEntity.ok().build();
    }
}

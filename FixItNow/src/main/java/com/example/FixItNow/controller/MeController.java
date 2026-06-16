package com.example.FixItNow.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.FixItNow.dto.v1.ProfileMeV1;
import com.example.FixItNow.service.ProfileV1Service;
import com.example.FixItNow.util.SecurityUtil;

import lombok.RequiredArgsConstructor;

/**
 * Current-user profile + dashboard stats consumed by the axios http client
 * (services/profile.ts) at root paths: /me, /homeowners/me/stats, /admin/me/stats.
 * Raw JSON bodies (no ApiResponse envelope). All require an authenticated user.
 */
@RestController
@RequiredArgsConstructor
public class MeController {

    private final ProfileV1Service profileService;

    @GetMapping("/me")
    public ResponseEntity<ProfileMeV1> me() {
        return ResponseEntity.ok(profileService.me(SecurityUtil.getCurrentUserId()));
    }

    @PatchMapping("/me")
    public ResponseEntity<ProfileMeV1> update(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(profileService.update(SecurityUtil.getCurrentUserId(), payload));
    }

    @GetMapping("/homeowners/me/stats")
    public ResponseEntity<Map<String, Object>> homeownerStats() {
        return ResponseEntity.ok(profileService.homeownerStats(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/admin/me/stats")
    public ResponseEntity<Map<String, Object>> adminStats() {
        return ResponseEntity.ok(profileService.adminStats(SecurityUtil.getCurrentUserId()));
    }
}

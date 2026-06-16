package com.example.FixItNow.service;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.FixItNow.repository.RefreshTokenRepository;
import com.example.FixItNow.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Security posture overview (services/security.ts) and system health (services/system.ts).
 * Security checks reflect the actual app configuration; runtime metrics come from the JVM
 * and a DB ping. Infra-level fields (incidents, per-service uptime, throughput) are
 * placeholders until a real monitoring backend is wired in.
 */
@Service
@RequiredArgsConstructor
public class SystemMonitorV1Service {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    // ===== Security =====

    public Map<String, Object> securityOverview() {
        List<Map<String, Object>> checks = new ArrayList<>();
        checks.add(check("JWT Authentication", "pass", "Stateless bearer tokens validated on every request"));
        checks.add(check("Password Hashing", "pass", "BCrypt password encoder in use"));
        checks.add(check("CORS Policy", "pass", "Restricted to configured origins"));
        checks.add(check("Account Enumeration", "pass", "Generic auth error messages"));
        checks.add(check("HTTPS Enforcement", "warn", "Not enforced at the application layer"));
        checks.add(check("Rate Limiting", "warn", "Login throttling not yet implemented"));
        checks.add(check("Secret Management", "warn", "Falls back to dev defaults when env vars unset"));

        long pass = checks.stream().filter(c -> "pass".equals(c.get("status"))).count();
        long warnings = checks.stream().filter(c -> "warn".equals(c.get("status"))).count();
        long critical = checks.stream().filter(c -> "fail".equals(c.get("status"))).count();
        int score = (int) Math.round(100.0 * pass / checks.size());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("score", score);
        m.put("warnings", warnings);
        m.put("critical", critical);
        m.put("active_sessions", refreshTokenRepository.count());
        m.put("checks", checks);
        m.put("incidents", List.of());
        m.put("last_scan", LocalDateTime.now().toString());
        return m;
    }

    public Map<String, Object> runScan() {
        // No external scanner yet — recompute the current posture with a fresh timestamp.
        return securityOverview();
    }

    // ===== System health =====

    public Map<String, Object> systemHealth() {
        Runtime rt = Runtime.getRuntime();
        long maxMb = rt.maxMemory() / (1024 * 1024);
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        int memPct = maxMb > 0 ? (int) Math.round(100.0 * usedMb / maxMb) : 0;

        double load = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        String cpuValue = load >= 0 ? Math.round(load * 100) / 100.0 + " load" : "—";

        boolean dbUp;
        try {
            userRepository.count();
            dbUp = true;
        } catch (Exception e) {
            dbUp = false;
        }
        String dbStatus = dbUp ? "Operational" : "Down";

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", dbUp ? "Operational" : "Degraded");
        m.put("cpu", metric(cpuValue, "system load avg"));
        m.put("memory", metric(memPct + "%", usedMb + " MB / " + maxMb + " MB"));
        m.put("db", metric(dbUp ? "Connected" : "Unreachable", dbStatus));
        m.put("throughput", metric("—", "requests/min not tracked"));
        m.put("services", List.of(
                service("API Gateway", "Operational", "99.9%", "—"),
                service("Database", dbStatus, dbUp ? "99.9%" : "0%", "—"),
                service("Email (SMTP)", "Operational", "99.0%", "—"),
                service("WebSocket Tracker", "Operational", "99.5%", "—")));
        m.put("events", List.of());
        m.put("jobs", List.of(List.of("Database backup", "Idle"), List.of("Email queue", "Running")));
        return m;
    }

    // ===== helpers =====

    private Map<String, Object> check(String name, String status, String desc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("status", status);
        m.put("desc", desc);
        return m;
    }

    private Map<String, Object> metric(String value, String sub) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("value", value);
        m.put("sub", sub);
        return m;
    }

    private Map<String, Object> service(String name, String status, String uptime, String latency) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("status", status);
        m.put("uptime", uptime);
        m.put("latency", latency);
        return m;
    }
}

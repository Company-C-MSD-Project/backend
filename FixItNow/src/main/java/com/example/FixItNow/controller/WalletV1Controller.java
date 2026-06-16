package com.example.FixItNow.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.FixItNow.service.WalletV1Service;
import com.example.FixItNow.util.SecurityUtil;

import lombok.RequiredArgsConstructor;

/** Provider wallet endpoints (services/wallet.ts). Provider self-service. */
@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SERVICE_PROVIDER')")
public class WalletV1Controller {

    private final WalletV1Service walletService;

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> balance() {
        return ResponseEntity.ok(walletService.balance(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(walletService.stats(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Map<String, Object>>> transactions() {
        return ResponseEntity.ok(walletService.transactions(SecurityUtil.getCurrentUserId()));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, Object>> withdraw(@RequestBody Map<String, Object> body) {
        BigDecimal amount = body.get("amount") != null
                ? new BigDecimal(String.valueOf(body.get("amount"))) : null;
        String accountId = body.get("account_id") != null ? String.valueOf(body.get("account_id")) : null;
        return ResponseEntity.ok(walletService.withdraw(SecurityUtil.getCurrentUserId(), amount, accountId));
    }
}

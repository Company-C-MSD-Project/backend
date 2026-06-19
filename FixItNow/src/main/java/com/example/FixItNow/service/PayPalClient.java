package com.example.FixItNow.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.example.FixItNow.exception.ExternalServiceException;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class PayPalClient {

    private final RestClient restClient;
    private final String clientId;
    private final String secret;

    public PayPalClient(
            RestClient.Builder restClientBuilder,
            @Value("${paypal.base-url}") String baseUrl,
            @Value("${paypal.client-id}") String clientId,
            @Value("${paypal.secret}") String secret) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.clientId = clientId;
        this.secret = secret;
    }

    public JsonNode createOrder(Long bookingId, BigDecimal amount, String currency) {
        Map<String, Object> amountValue = Map.of(
                "currency_code", currency,
                "value", amount.setScale(2, RoundingMode.HALF_UP).toPlainString());
        Map<String, Object> purchaseUnit = new LinkedHashMap<>();
        purchaseUnit.put("reference_id", bookingId.toString());
        purchaseUnit.put("custom_id", bookingId.toString());
        purchaseUnit.put("amount", amountValue);

        return postJson("/v2/checkout/orders", Map.of(
                "intent", "CAPTURE",
                "purchase_units", java.util.List.of(purchaseUnit)), "booking-" + bookingId);
    }

    public JsonNode captureOrder(String orderId) {
        return postJson("/v2/checkout/orders/" + orderId + "/capture", Map.of(), "capture-" + orderId);
    }

    public boolean verifyWebhook(Map<String, String> headers, JsonNode event, String webhookId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("auth_algo", requiredHeader(headers, "paypal-auth-algo"));
        body.put("cert_url", requiredHeader(headers, "paypal-cert-url"));
        body.put("transmission_id", requiredHeader(headers, "paypal-transmission-id"));
        body.put("transmission_sig", requiredHeader(headers, "paypal-transmission-sig"));
        body.put("transmission_time", requiredHeader(headers, "paypal-transmission-time"));
        body.put("webhook_id", webhookId);
        body.put("webhook_event", event);
        return "SUCCESS".equals(postJson("/v1/notifications/verify-webhook-signature", body, null)
                .path("verification_status").asText());
    }

    private JsonNode postJson(String path, Object body, String requestId) {
        validateCredentials();
        try {
            RestClient.RequestBodySpec request = restClient.post()
                    .uri(path)
                    .header("Authorization", "Bearer " + accessToken())
                    .contentType(MediaType.APPLICATION_JSON);
            if (requestId != null) {
                request.header("PayPal-Request-Id", requestId);
            }
            return request
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            throw new ExternalServiceException(
                    "PayPal request failed (" + ex.getStatusCode() + "): " + ex.getResponseBodyAsString(), ex);
        } catch (RuntimeException ex) {
            throw new ExternalServiceException("PayPal request failed", ex);
        }
    }

    private String accessToken() {
        String basic = Base64.getEncoder().encodeToString(
                (clientId + ":" + secret).getBytes(StandardCharsets.UTF_8));
        try {
            JsonNode response = restClient.post()
                    .uri("/v1/oauth2/token")
                    .header("Authorization", "Basic " + basic)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=client_credentials")
                    .retrieve()
                    .body(JsonNode.class);
            String token = response == null ? null : response.path("access_token").asText(null);
            if (token == null || token.isBlank()) {
                throw new ExternalServiceException("PayPal did not return an access token");
            }
            return token;
        } catch (RestClientResponseException ex) {
            throw new ExternalServiceException("PayPal authentication failed", ex);
        }
    }

    private void validateCredentials() {
        if (clientId == null || clientId.isBlank() || secret == null || secret.isBlank()) {
            throw new ExternalServiceException("PayPal credentials are not configured");
        }
    }

    private String requiredHeader(Map<String, String> headers, String name) {
        String value = headers.get(name);
        if (value == null || value.isBlank()) {
            throw new ExternalServiceException("Missing PayPal webhook header: " + name);
        }
        return value;
    }
}

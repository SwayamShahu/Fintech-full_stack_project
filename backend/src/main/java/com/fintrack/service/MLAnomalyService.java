package com.fintrack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Service to integrate with Python ML Anomaly Detection Service.
 * Falls back to rule-based detection if ML service is unavailable.
 */
@Service
public class MLAnomalyService {

    private static final Logger logger = LoggerFactory.getLogger(MLAnomalyService.class);

    @Value("${ml.service.url:http://localhost:5001}")
    private String mlServiceUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public MLAnomalyService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Result from ML anomaly detection
     */
    public static class MLAnomalyResult {
        public boolean isAnomaly = false;
        public BigDecimal score = BigDecimal.ZERO;
        public String explanation = null;
        public String type = null;
        public String severity = "NORMAL";
        public boolean usedMlService = false;
        public Map<String, Double> modelScores = new HashMap<>();
    }

    /**
     * Check if ML service is healthy
     */
    public boolean isServiceHealthy() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                mlServiceUrl + "/health", 
                String.class
            );
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            logger.debug("ML service health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Detect anomaly using ML service
     * 
     * @param userId User ID
     * @param amount Transaction amount
     * @param categoryName Category name
     * @param expenseDate Date of expense
     * @param paymentMode Payment mode
     * @return MLAnomalyResult with predictions
     */
    public MLAnomalyResult detectAnomaly(
            Long userId,
            BigDecimal amount,
            String categoryName,
            LocalDate expenseDate,
            String paymentMode
    ) {
        MLAnomalyResult result = new MLAnomalyResult();

        try {
            // Build request payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("user_id", userId);
            payload.put("amount", amount.doubleValue());
            payload.put("category", categoryName);
            payload.put("expense_date", expenseDate.toString());
            payload.put("payment_mode", paymentMode != null ? paymentMode : "Cash");

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            // Call ML service
            ResponseEntity<String> response = restTemplate.postForEntity(
                mlServiceUrl + "/predict",
                request,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());

                // Check for fallback flag
                if (json.has("fallback") && json.get("fallback").asBoolean()) {
                    logger.warn("ML service returned fallback flag");
                    return result; // Return default (not anomaly)
                }

                result.usedMlService = true;
                result.isAnomaly = json.get("is_anomaly").asBoolean();
                result.score = BigDecimal.valueOf(json.get("anomaly_score").asDouble());
                result.severity = json.has("severity") ? json.get("severity").asText() : "NORMAL";

                if (json.has("anomaly_type") && !json.get("anomaly_type").isNull()) {
                    result.type = json.get("anomaly_type").asText();
                }

                if (json.has("explanation") && !json.get("explanation").isNull()) {
                    result.explanation = json.get("explanation").asText();
                }

                // Parse model scores
                if (json.has("model_scores")) {
                    JsonNode scores = json.get("model_scores");
                    if (scores.has("isolation_forest")) {
                        result.modelScores.put("isolation_forest", scores.get("isolation_forest").asDouble());
                    }
                    if (scores.has("autoencoder")) {
                        result.modelScores.put("autoencoder", scores.get("autoencoder").asDouble());
                    }
                    if (scores.has("lstm")) {
                        result.modelScores.put("lstm", scores.get("lstm").asDouble());
                    }
                }

                logger.info("ML detection for user {}: isAnomaly={}, score={}, type={}", 
                    userId, result.isAnomaly, result.score, result.type);
            }

        } catch (Exception e) {
            logger.warn("ML service call failed: {}. Falling back to rule-based detection.", e.getMessage());
            result.usedMlService = false;
        }

        return result;
    }

    /**
     * Trigger model retraining for a specific user
     */
    public boolean triggerUserTraining(Long userId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("user_id", userId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                mlServiceUrl + "/train",
                request,
                String.class
            );

            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            logger.error("Failed to trigger user training: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get ML service status
     */
    public Map<String, Object> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                mlServiceUrl + "/models/status",
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());
                status.put("available", true);
                status.put("models", objectMapper.convertValue(json, Map.class));
            }
        } catch (Exception e) {
            status.put("available", false);
            status.put("error", e.getMessage());
        }

        return status;
    }
}

package com.example.jobautomate.dto;

import java.time.OffsetDateTime;

public record ApplyResponse(String status, OffsetDateTime appliedAt) {
}

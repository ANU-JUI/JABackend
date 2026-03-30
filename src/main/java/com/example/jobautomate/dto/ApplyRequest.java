package com.example.jobautomate.dto;

import jakarta.validation.constraints.NotBlank;

public record ApplyRequest(@NotBlank String userId) {
}

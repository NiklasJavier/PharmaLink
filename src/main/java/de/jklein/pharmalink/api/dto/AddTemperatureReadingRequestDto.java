package de.jklein.pharmalink.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddTemperatureReadingRequestDto {

    @NotBlank(message = "Temperature must not be blank.")
    private String temperature;

    @NotBlank(message = "Timestamp must not be blank.")
    private String timestamp;
}
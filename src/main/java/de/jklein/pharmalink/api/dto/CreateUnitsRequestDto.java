package de.jklein.pharmalink.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.Map;

@Data
public class CreateUnitsRequestDto {

    @NotBlank(message = "Batch designation must not be blank.")
    private String chargeBezeichnung;

    @Min(value = 1, message = "At least one unit must be created.")
    private int anzahl;

    private Map<String, Object> ipfsData;
}
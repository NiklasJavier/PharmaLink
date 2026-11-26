package de.jklein.pharmalink.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.Map;

@Data
public class CreateMedikamentRequestDto {

    @NotBlank(message = "Designation must not be blank.")
    private String bezeichnung;

    private String infoblattHash;

    private Map<String, Object> ipfsData;
}
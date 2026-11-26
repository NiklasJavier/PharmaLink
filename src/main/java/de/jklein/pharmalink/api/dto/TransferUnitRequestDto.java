package de.jklein.pharmalink.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TransferUnitRequestDto {

    @NotBlank(message = "The new owner ID (newOwnerActorId) must not be blank.")
    private String newOwnerActorId;
}
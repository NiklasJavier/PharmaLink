package de.jklein.pharmalink.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateMedicationStatusRequestDto {

    @NotBlank(message = "The new status must not be blank.")
    @Pattern(regexp = "freigegeben|abgelehnt", message = "Status must be 'freigegeben' or 'abgelehnt'.")
    private String newStatus;
}
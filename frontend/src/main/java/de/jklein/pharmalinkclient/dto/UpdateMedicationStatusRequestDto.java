package de.jklein.pharmalinkclient.dto; 

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;

public class UpdateMedicationStatusRequestDto {
    @NotBlank(message = "Status must not be empty.")
    @Pattern(regexp = "freigegeben|abgelehnt", message = "Status must be 'freigegeben' or 'abgelehnt'.")
    private String newStatus;

    public UpdateMedicationStatusRequestDto() {
    }

    public UpdateMedicationStatusRequestDto(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }
}
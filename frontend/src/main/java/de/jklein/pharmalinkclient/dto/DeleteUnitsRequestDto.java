package de.jklein.pharmalinkclient.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;

public class DeleteUnitsRequestDto {
    @NotEmpty(message = "The list of unit IDs must not be empty.")
    private List<String> unitIds;

    public DeleteUnitsRequestDto() {
    }

    public DeleteUnitsRequestDto(List<String> unitIds) {
        this.unitIds = unitIds;
    }

    public List<String> getUnitIds() {
        return unitIds;
    }

    public void setUnitIds(List<String> unitIds) {
        this.unitIds = unitIds;
    }
}
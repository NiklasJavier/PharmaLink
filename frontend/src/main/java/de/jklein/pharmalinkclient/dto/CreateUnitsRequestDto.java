package de.jklein.pharmalinkclient.dto; 
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public class CreateUnitsRequestDto {
    @NotBlank(message = "Batch name must not be empty.")
    private String chargeBezeichnung;

    @Min(value = 1, message = "Quantity must be at least 1.")
    private int anzahl;

    private Map<String, Object> ipfsData;

    public CreateUnitsRequestDto() {
    }

    public CreateUnitsRequestDto(String chargeBezeichnung, int anzahl, Map<String, Object> ipfsData) {
        this.chargeBezeichnung = chargeBezeichnung;
        this.anzahl = anzahl;
        this.ipfsData = ipfsData;
    }

    public String getChargeBezeichnung() {
        return chargeBezeichnung;
    }

    public void setChargeBezeichnung(String chargeBezeichnung) {
        this.chargeBezeichnung = chargeBezeichnung;
    }

    public int getAnzahl() {
        return anzahl;
    }

    public void setAnzahl(int anzahl) {
        this.anzahl = anzahl;
    }

    public Map<String, Object> getIpfsData() {
        return ipfsData;
    }

    public void setIpfsData(Map<String, Object> ipfsData) {
        this.ipfsData = ipfsData;
    }
}
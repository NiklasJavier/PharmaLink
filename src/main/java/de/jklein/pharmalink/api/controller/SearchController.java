package de.jklein.pharmalink.api.controller;

import de.jklein.pharmalink.api.dto.ActorResponseDto;
import de.jklein.pharmalink.api.dto.MedikamentResponseDto;
import de.jklein.pharmalink.api.dto.UnitResponseDto;
import de.jklein.pharmalink.api.mapper.ActorMapper;
import de.jklein.pharmalink.api.mapper.MedikamentMapper;
import de.jklein.pharmalink.api.mapper.UnitMapper;
import de.jklein.pharmalink.domain.Unit;
import de.jklein.pharmalink.service.search.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;
    private final MedikamentMapper medikamentMapper;
    private final UnitMapper unitMapper;
    private final ActorMapper actorMapper;

    public SearchController(SearchService searchService, MedikamentMapper medikamentMapper,
                            UnitMapper unitMapper, ActorMapper actorMapper) {
        this.searchService = searchService;
        this.medikamentMapper = medikamentMapper;
        this.unitMapper = unitMapper;
        this.actorMapper = actorMapper;
    }

    @GetMapping("/actors")
    @Operation(summary = "Search for actors", description = "Searches for actors by role and/or name. All parameters are optional.")
    public ResponseEntity<List<ActorResponseDto>> searchActors(
            @Parameter(description = "Filter by a specific role.") @RequestParam(required = false) String role,
            @Parameter(description = "Search query for the actor's name.") @RequestParam(required = false) String bezeichnung,
            @Parameter(description = "Filter by a specific actor ID.") @RequestParam(required = false) String actorId) {
        var foundActors = searchService.searchActors(role, bezeichnung, actorId);
        return ResponseEntity.ok(actorMapper.toDtoList(foundActors));
    }

    @GetMapping("/medikamente")
    @Operation(summary = "Search for medications", description = "Searches for medications with optional filters for name/manufacturer, status, tags, and ownership.")
    public ResponseEntity<List<MedikamentResponseDto>> searchMedikamente(
            @Parameter(description = "Search query for the medication or manufacturer name.")
            @RequestParam(required = false) String query,
            @Parameter(description = "Filter by a specific medication status (e.g., 'angelegt', 'freigegeben').")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by a list of tags. The medication must contain at least one tag.")
            @RequestParam(required = false) List<String> tags,
            @Parameter(description = "If 'true', only medications owned by the current actor are returned.")
            @RequestParam(required = false, defaultValue = "false") boolean ownedByMe) {

        var foundMedikamente = searchService.searchMedikamente(query, status, tags, ownedByMe);
        return ResponseEntity.ok(medikamentMapper.toDtoList(foundMedikamente));
    }

    @GetMapping("/units")
    @Operation(summary = "Search units by batch", description = "Searches within the user's own units by their batch designation.")
    public ResponseEntity<List<UnitResponseDto>> searchUnitsByCharge(
            @Parameter(description = "The search query for the batch designation.")
            @RequestParam String query) {
        var foundUnits = searchService.searchUnitsByCharge(query);
        return ResponseEntity.ok(unitMapper.toDtoList(foundUnits));
    }

    @GetMapping("/my-units")
    @Operation(summary = "Get own cached units", description = "Returns a list of all cached units owned by the current user.")
    public ResponseEntity<List<UnitResponseDto>> getMyCachedUnits() {
        List<Unit> myUnits = searchService.getMyUnits();
        List<UnitResponseDto> myUnitsDto = unitMapper.toDtoList(myUnits);
        return ResponseEntity.ok(myUnitsDto);
    }
}
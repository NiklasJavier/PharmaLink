package de.jklein.pharmalink.api.controller;

import de.jklein.pharmalink.api.dto.ActorResponseDto;
import de.jklein.pharmalink.api.dto.MedikamentResponseDto;
import de.jklein.pharmalink.api.dto.SystemStateDto;
import de.jklein.pharmalink.api.dto.SystemStatsDto;
import de.jklein.pharmalink.api.dto.UnitResponseDto;
import de.jklein.pharmalink.api.mapper.ActorMapper;
import de.jklein.pharmalink.api.mapper.MedikamentMapper;
import de.jklein.pharmalink.api.mapper.UnitMapper;
import de.jklein.pharmalink.domain.Actor;
import de.jklein.pharmalink.domain.Medikament;
import de.jklein.pharmalink.domain.Unit;
import de.jklein.pharmalink.repository.ActorRepository;
import de.jklein.pharmalink.repository.MedikamentRepository;
import de.jklein.pharmalink.repository.UnitRepository;
import de.jklein.pharmalink.service.state.SystemStateService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final SystemStateService systemStateService;
    private final ActorRepository actorRepository;
    private final MedikamentRepository medikamentRepository;
    private final UnitRepository unitRepository;
    private final ActorMapper actorMapper;
    private final MedikamentMapper medikamentMapper;
    private final UnitMapper unitMapper;

    public SystemController(SystemStateService systemStateService, ActorRepository actorRepository,
                            MedikamentRepository medikamentRepository, UnitRepository unitRepository,
                            ActorMapper actorMapper, MedikamentMapper medikamentMapper, UnitMapper unitMapper) {
        this.systemStateService = systemStateService;
        this.actorRepository = actorRepository;
        this.medikamentRepository = medikamentRepository;
        this.unitRepository = unitRepository;
        this.actorMapper = actorMapper;
        this.medikamentMapper = medikamentMapper;
        this.unitMapper = unitMapper;
    }

    @GetMapping("/current-actor-id")
    @Operation(summary = "Get current actor ID", description = "Retrieves the ID of the current actor.")
    public ResponseEntity<Map<String, String>> getCurrentActorId() {
        String actorId = systemStateService.getCurrentActorId().get();

        if (!StringUtils.hasText(actorId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(Map.of("actorId", actorId));
    }

    @GetMapping("/cache/stats")
    @Operation(summary = "Get statistics", description = "Returns a quick summary of the data stored in the database.")
    public ResponseEntity<SystemStatsDto> getCacheStats() {
        String currentActorId = systemStateService.getCurrentActorId().get();
        long myUnitsCount = 0;
        if (StringUtils.hasText(currentActorId)) {
            myUnitsCount = unitRepository.countByCurrentOwnerActorId(currentActorId);
        }

        SystemStatsDto statsDto = new SystemStatsDto(
                (int) actorRepository.count(),
                (int) medikamentRepository.count(),
                (int) myUnitsCount
        );
        return ResponseEntity.ok(statsDto);
    }

    @GetMapping("/cache/state")
    @Operation(summary = "Get database state", description = "Retrieves a snapshot of the core data.")
    public ResponseEntity<SystemStateDto> getCacheState() {
        String actorId = systemStateService.getCurrentActorId().get();

        // Load data directly from the repositories
        List<Actor> allActors = actorRepository.findAll();
        List<Medikament> allMedikamente = medikamentRepository.findAll();
        List<Unit> myUnits = StringUtils.hasText(actorId) ?
                unitRepository.findByCurrentOwnerActorId(actorId) : Collections.emptyList();

        // Map to DTOs
        List<ActorResponseDto> actorsDto = actorMapper.toDtoList(allActors);
        List<MedikamentResponseDto> medikamenteDto = medikamentMapper.toDtoList(allMedikamente);
        List<UnitResponseDto> unitsDto = unitMapper.toDtoList(myUnits);

        SystemStateDto stateDto = new SystemStateDto(
                actorId,
                allActors.size(),
                allMedikamente.size(),
                myUnits.size(),
                actorsDto,
                medikamenteDto,
                unitsDto
        );

        return ResponseEntity.ok(stateDto);
    }
}
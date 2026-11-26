package de.jklein.pharmalink.service.state;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jklein.pharmalink.client.fabric.FabricClient;
import de.jklein.pharmalink.domain.Actor;
import de.jklein.pharmalink.domain.Medikament;
import de.jklein.pharmalink.domain.Unit;
import de.jklein.pharmalink.domain.audit.ChaincodeEventLog;
import de.jklein.pharmalink.domain.system.SystemState;
import de.jklein.pharmalink.repository.ActorRepository;
import de.jklein.pharmalink.repository.MedikamentRepository;
import de.jklein.pharmalink.repository.UnitRepository;
import de.jklein.pharmalink.repository.audit.ChaincodeEventLogRepository;
import de.jklein.pharmalink.repository.system.SystemStateRepository;
import de.jklein.pharmalink.service.fabric.ActorFabricService;
import de.jklein.pharmalink.service.fabric.MedicationFabricService;
import de.jklein.pharmalink.service.fabric.UnitFabricService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.hyperledger.fabric.client.ChaincodeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Getter
public class SystemStateService {

    private static final Logger logger = LoggerFactory.getLogger(SystemStateService.class);
    private static final String SYSTEM_STATE_ID = "pharmalink-system-state";

    private static final String ACTOR_INITIALIZED_EVENT = "ActorInitialized";
    private static final String ACTOR_CREATED_EVENT = "ActorCreated";
    private static final String ACTOR_UPDATED_EVENT = "ActorUpdated";
    private static final String ACTOR_IPFS_LINK_UPDATED_EVENT = "ActorIpfsLinkUpdated";
    private static final String ACTOR_DELETED_EVENT = "ActorDeleted";
    private static final String MEDIKAMENT_CREATED_EVENT = "MedikamentCreated";
    private static final String MEDIKAMENT_STATUS_UPDATED_EVENT = "MedikamentStatusUpdated";
    private static final String MEDIKAMENT_UPDATED_EVENT = "MedikamentUpdated";
    private static final String MEDIKAMENT_TAG_ADDED_EVENT = "MedikamentTagAdded";
    private static final String MEDIKAMENT_DELETED_EVENT = "MedikamentDeleted";
    private static final String UNIT_CREATED_EVENT = "UnitCreated";
    private static final String UNIT_TEMPERATURE_ADDED_EVENT = "UnitTemperatureAdded";
    private static final String UNIT_TRANSFERRED_EVENT = "UnitTransferred";
    private static final String UNIT_DELETED_EVENT = "UnitDeleted";

    private final SystemStateRepository systemStateRepository;
    private final ActorRepository actorRepository;
    private final MedikamentRepository medikamentRepository;
    private final UnitRepository unitRepository;
    private final ChaincodeEventLogRepository eventLogRepository;
    private final FabricClient fabricClient;
    private final ActorFabricService actorFabricService;
    private final MedicationFabricService medicationFabricService;
    private final UnitFabricService unitFabricService;
    private final ObjectMapper objectMapper;

    @Value("${fabric.chaincode-name}")
    private String chaincodeName;

    private final AtomicReference<String> currentActorId = new AtomicReference<>();
    private final AtomicLong lastProcessedBlock = new AtomicLong(0L);

    public SystemStateService(SystemStateRepository systemStateRepository, FabricClient fabricClient,
                              ActorFabricService actorFabricService, MedicationFabricService medicationFabricService,
                              UnitFabricService unitFabricService, ObjectMapper objectMapper,
                              ChaincodeEventLogRepository eventLogRepository, ActorRepository actorRepository,
                              MedikamentRepository medikamentRepository, UnitRepository unitRepository) {
        this.systemStateRepository = systemStateRepository;
        this.fabricClient = fabricClient;
        this.actorFabricService = actorFabricService;
        this.medicationFabricService = medicationFabricService;
        this.unitFabricService = unitFabricService;
        this.objectMapper = objectMapper;
        this.eventLogRepository = eventLogRepository;
        this.actorRepository = actorRepository;
        this.medikamentRepository = medikamentRepository;
        this.unitRepository = unitRepository;
    }

    @PostConstruct
    public void init() {
        logger.info("Initializing System State Service...");
        loadStateFromDatabase();
        synchronizeWithChaincode();
        startEventListening();
    }

    private void loadStateFromDatabase() {
        systemStateRepository.findById(SYSTEM_STATE_ID).ifPresent(state -> {
            currentActorId.set(state.getCurrentActorId());
            lastProcessedBlock.set(state.getLastProcessedBlockNumber());
            logger.info("Loaded persisted system state. Current actor ID: {}, Last block: {}",
                    state.getCurrentActorId(), state.getLastProcessedBlockNumber());
        });
    }

    @Transactional
    public void synchronizeWithChaincode() {
        logger.info("Synchronizing global state (actors, medications) with chaincode...");
        try {
            List<Actor> actorsFromChaincode = actorFabricService.getAllActors();
            actorRepository.deleteAll();
            actorRepository.saveAll(actorsFromChaincode);
            logger.info("{} actors successfully synchronized with the database.", actorsFromChaincode.size());

            List<Medikament> medikamenteFromChaincode = medicationFabricService.getAllMedikamente();
            medikamentRepository.deleteAll();
            medikamentRepository.saveAll(medikamenteFromChaincode);
            logger.info("{} medications successfully synchronized with the database.", medikamenteFromChaincode.size());

            if (StringUtils.hasText(currentActorId.get())) {
                synchronizeUnitsForActor(currentActorId.get());
            }
        } catch (Exception e) {
            logger.error("CRITICAL: Failed to synchronize global state with chaincode. Reason: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down System State Service...");
        saveStateToDatabase();
        fabricClient.shutdownEventExecutor();
    }

    private void startEventListening() {
        try {
            logger.info("Starting fault-tolerant chaincode event listener...");
            fabricClient.startEventListeningWithRetry(chaincodeName, () -> this.lastProcessedBlock.get() + 1, this::handleChaincodeEvent);
            logger.info("Fault-tolerant chaincode event listener for chaincode '{}' started successfully.", chaincodeName);
        } catch (Exception e) {
            logger.error("Error starting chaincode event listener: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void handleChaincodeEvent(ChaincodeEvent event) {
        logEventForAudit(event);
        try {
            if (event.getPayload() == null || event.getPayload().length == 0) {
                logger.warn("Received event '{}' with empty payload. Skipping.", event.getEventName());
                return;
            }

            JsonNode payload = objectMapper.readTree(event.getPayload());

            switch (event.getEventName()) {
                case ACTOR_INITIALIZED_EVENT, ACTOR_CREATED_EVENT, ACTOR_UPDATED_EVENT, ACTOR_IPFS_LINK_UPDATED_EVENT ->
                        handleActorUpdate(getIdFromPayload(payload, "actorId"));
                case ACTOR_DELETED_EVENT ->
                        handleActorDelete(getIdFromPayload(payload, "actorId"));
                case MEDIKAMENT_CREATED_EVENT, MEDIKAMENT_STATUS_UPDATED_EVENT, MEDIKAMENT_UPDATED_EVENT, MEDIKAMENT_TAG_ADDED_EVENT ->
                        handleMedikamentUpdate(getIdFromPayload(payload, "medId"));
                case MEDIKAMENT_DELETED_EVENT ->
                        handleMedikamentDelete(getIdFromPayload(payload, "medId"));
                case UNIT_CREATED_EVENT ->
                        handleUnitBatchCreation(payload);
                case UNIT_TEMPERATURE_ADDED_EVENT ->
                        handleUnitUpdate(getIdFromPayload(payload, "unitId"));
                case UNIT_TRANSFERRED_EVENT ->
                        handleUnitTransfer(payload);
                case UNIT_DELETED_EVENT ->
                        handleUnitDelete(getIdFromPayload(payload, "unitId"));
                default -> logger.warn("Received unhandled event: {}. Payload: {}", event.getEventName(), payload.toString());
            }

            updateAndPersistCheckpoint(event.getBlockNumber());

        } catch (IOException e) {
            logger.error("Error processing event payload for event '{}': {}", event.getEventName(), e.getMessage(), e);
        }
    }

    private void updateAndPersistCheckpoint(long blockNumber) {
        this.lastProcessedBlock.set(blockNumber);
        saveStateToDatabase();
    }

    private Optional<String> getIdFromPayload(JsonNode payload, String fieldName) {
        return payload.has(fieldName) ? Optional.of(payload.get(fieldName).asText()) : Optional.empty();
    }

    private void logEventForAudit(ChaincodeEvent event) {
        try {
            String payloadAsString = new String(event.getPayload(), StandardCharsets.UTF_8);
            ChaincodeEventLog logEntry = new ChaincodeEventLog(event.getEventName(), event.getTransactionId(), event.getBlockNumber(), payloadAsString);
            eventLogRepository.save(logEntry);
        } catch (Exception e) {
            logger.error("!!! ERROR AUDITING CHAINCODE EVENT !!! Tx ID: {}. Reason: {}", event.getTransactionId(), e.getMessage(), e);
        }
    }

    private void handleActorUpdate(Optional<String> actorIdOpt) {
        actorIdOpt.ifPresent(actorId -> actorFabricService.getEnrichedActorById(actorId).ifPresent(actorFromChaincode -> {
            Optional<Actor> existingActorOpt = actorRepository.findByActorId(actorId);
            Actor actorToSave = existingActorOpt.orElse(actorFromChaincode);
            if (existingActorOpt.isPresent()) {
                actorToSave.setBezeichnung(actorFromChaincode.getBezeichnung());
                actorToSave.setRole(actorFromChaincode.getRole());
                actorToSave.setEmail(actorFromChaincode.getEmail());
                actorToSave.setIpfsLink(actorFromChaincode.getIpfsLink());
                actorToSave.setIpfsData(actorFromChaincode.getIpfsData());
            }
            actorRepository.save(actorToSave);
            logger.info("Actor {} created/updated in the database.", actorId);
        }));
    }

    private void handleActorDelete(Optional<String> actorIdOpt) {
        actorIdOpt.ifPresent(actorId -> {
            actorRepository.deleteByActorId(actorId);
            logger.info("Actor {} removed from the database.", actorId);
        });
    }

    private void handleMedikamentUpdate(Optional<String> medIdOpt) {
        medIdOpt.ifPresent(medId -> medicationFabricService.getEnrichedMedikamentById(medId).ifPresent(medikamentFromChaincode -> {
            Optional<Medikament> existingMedikamentOpt = medikamentRepository.findByMedId(medId);
            Medikament medikamentToSave = existingMedikamentOpt.orElse(medikamentFromChaincode);
            if(existingMedikamentOpt.isPresent()){
                medikamentToSave.setBezeichnung(medikamentFromChaincode.getBezeichnung());
                medikamentToSave.setStatus(medikamentFromChaincode.getStatus());
                medikamentToSave.setInfoblattHash(medikamentFromChaincode.getInfoblattHash());
                medikamentToSave.setIpfsLink(medikamentFromChaincode.getIpfsLink());
                medikamentToSave.setApprovedById(medikamentFromChaincode.getApprovedById());
                medikamentToSave.setTags(medikamentFromChaincode.getTags());
                medikamentToSave.setIpfsData(medikamentFromChaincode.getIpfsData());
            }
            medikamentRepository.save(medikamentToSave);
            logger.info("Medication {} created/updated in the database.", medId);
        }));
    }

    private void handleMedikamentDelete(Optional<String> medIdOpt) {
        medIdOpt.ifPresent(medId -> {
            medikamentRepository.deleteByMedId(medId);
            logger.info("Medication {} removed from the database.", medId);
        });
    }

    private void handleUnitBatchCreation(JsonNode payload) {
        Optional<String> unitIdOpt = getIdFromPayload(payload, "unitId");
        if (unitIdOpt.isEmpty()) {
            logger.error("Received UnitCreated event without unitId. Payload: {}", payload.toString());
            return;
        }

        String exampleUnitId = unitIdOpt.get();
        int lastDash = exampleUnitId.lastIndexOf('-');
        if (lastDash == -1) {
            logger.error("Invalid unitId format for batch creation: {}", exampleUnitId);
            handleUnitUpdate(Optional.of(exampleUnitId));
            return;
        }

        try {
            String idPrefix = exampleUnitId.substring(0, lastDash + 1);
            int count = Integer.parseInt(exampleUnitId.substring(lastDash + 1));

            logger.info("Starting batch creation for {} units with prefix '{}'.", count, idPrefix);

            List<Unit> batch = new ArrayList<>(count);
            for (int i = 1; i <= count; i++) {
                Unit newUnit = objectMapper.treeToValue(payload, Unit.class);
                newUnit.setUnitId(idPrefix + String.format("%04d", i));
                batch.add(newUnit);
            }

            unitRepository.saveAll(batch);
            logger.info("{} units successfully created in the database.", batch.size());

        } catch (NumberFormatException | IOException e) {
            logger.error("Error parsing unitId for batch, attempting fallback. ID: {}, Error: {}", exampleUnitId, e.getMessage());
            handleUnitUpdate(Optional.of(exampleUnitId));
        }
    }

    private void handleUnitTransfer(JsonNode payload) {
        String newOwnerId = getIdFromPayload(payload, "currentOwnerActorId").orElse(null);
        String fromActorId = null;

        if (payload.has("transferHistory") && payload.get("transferHistory").isArray()) {
            JsonNode historyArray = payload.get("transferHistory");
            if (historyArray.size() > 0) {
                JsonNode lastTransfer = historyArray.get(historyArray.size() - 1);
                fromActorId = getIdFromPayload(lastTransfer, "fromActorId").orElse(null);
            }
        }

        if (StringUtils.hasText(fromActorId) && StringUtils.hasText(newOwnerId)) {
            logger.info("Unit transfer detected. Synchronizing inventory for sender {} and recipient {}.", fromActorId, newOwnerId);
            synchronizeUnitsForActor(fromActorId);
            synchronizeUnitsForActor(newOwnerId);
        } else {
            logger.warn("Could not extract sender or recipient from transfer event. Payload: {}", payload.toString());
            handleUnitUpdate(getIdFromPayload(payload, "unitId"));
        }
    }

    private void handleUnitUpdate(Optional<String> unitIdOpt) {
        unitIdOpt.ifPresent(unitId -> unitFabricService.getEnrichedUnitById(unitId).ifPresent(unitFromChaincode -> {
            Optional<Unit> existingUnitOpt = unitRepository.findByUnitId(unitId);
            Unit unitToSave = existingUnitOpt.orElse(unitFromChaincode);
            if (existingUnitOpt.isPresent()) {
                unitToSave.setChargeBezeichnung(unitFromChaincode.getChargeBezeichnung());
                unitToSave.setIpfsLink(unitFromChaincode.getIpfsLink());
                unitToSave.setCurrentOwnerActorId(unitFromChaincode.getCurrentOwnerActorId());
                unitToSave.setTransferHistory(unitFromChaincode.getTransferHistory());
                unitToSave.setTemperatureReadings(unitFromChaincode.getTemperatureReadings());
                unitToSave.setConsumed(unitFromChaincode.isConsumed());
                unitToSave.setConsumedRefId(unitFromChaincode.getConsumedRefId());
                unitToSave.setIpfsData(unitFromChaincode.getIpfsData());
            }
            unitRepository.save(unitToSave);
            logger.info("Unit {} created/updated in the database.", unitId);
        }));
    }

    private void handleUnitDelete(Optional<String> unitIdOpt) {
        unitIdOpt.ifPresent(unitId -> {
            unitRepository.deleteByUnitId(unitId);
            logger.info("Unit {} removed from the database.", unitId);
        });
    }

    @Transactional
    public void reconcileAndCacheActorId(String actorId) {
        if (!Objects.equals(currentActorId.get(), actorId)) {
            logger.info("Current actor ID changed from {} to {}.", currentActorId.get(), actorId);
            currentActorId.set(actorId);
            saveStateToDatabase();

            if (StringUtils.hasText(actorId)) {
                synchronizeUnitsForActor(actorId);
            }
        }
    }

    @Transactional
    public void synchronizeUnitsForActor(String actorId) {
        if (!StringUtils.hasText(actorId)) return;

        try {
            logger.info("Starting fast synchronization of units for actor {}.", actorId);

            List<Unit> unitsFromChaincode = unitFabricService.getUnitsByOwner(actorId);

            unitRepository.deleteByCurrentOwnerActorId(actorId);

            if (!unitsFromChaincode.isEmpty()) {
                unitRepository.saveAll(unitsFromChaincode);
            }

            logger.info("{} units successfully synchronized for actor {}.", unitsFromChaincode.size(), actorId);

        } catch (Exception e) {
            logger.error("Error synchronizing units for actor {}: {}", actorId, e.getMessage(), e);
        }
    }

    public void saveStateToDatabase() {
        SystemState state = systemStateRepository.findById(SYSTEM_STATE_ID)
                .orElse(new SystemState(SYSTEM_STATE_ID, null));
        state.setCurrentActorId(currentActorId.get());
        state.setLastProcessedBlockNumber(lastProcessedBlock.get());
        systemStateRepository.save(state);
        logger.info("System state (currentActorId: {}, lastBlock: {}) saved to the database.",
                currentActorId.get(), lastProcessedBlock.get());
    }
}
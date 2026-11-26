package de.jklein.pharmalink.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.jklein.pharmalink.domain.Actor;
import de.jklein.pharmalink.domain.Medikament;
import de.jklein.pharmalink.domain.Unit;
import de.jklein.pharmalink.repository.ActorRepository;
import de.jklein.pharmalink.repository.MedikamentRepository;
import de.jklein.pharmalink.repository.UnitRepository;
import de.jklein.pharmalink.service.audit.AuditService;
import de.jklein.pharmalink.service.search.SearchService;
import de.jklein.pharmalink.service.state.SystemStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.*;

@Controller
@RequestMapping("/app")
public class AppController {

    private static final Logger logger = LoggerFactory.getLogger(AppController.class);

    // Required services and repositories
    private final SystemStateService systemStateService;
    private final SearchService searchService;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;
    private final ActorRepository actorRepository;
    private final MedikamentRepository medikamentRepository;
    private final UnitRepository unitRepository;

    public AppController(SystemStateService systemStateService, SearchService searchService,
                         ObjectMapper objectMapper, AuditService auditService,
                         ActorRepository actorRepository, MedikamentRepository medikamentRepository,
                         UnitRepository unitRepository) {
        this.systemStateService = systemStateService;
        this.searchService = searchService;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
        this.actorRepository = actorRepository;
        this.medikamentRepository = medikamentRepository;
        this.unitRepository = unitRepository;
    }

    @GetMapping("/login")
    public String showLoginForm() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/app/dashboard";
        }
        return "auth/login";
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model, RedirectAttributes redirectAttributes) throws IOException {
        String currentActorId = systemStateService.getCurrentActorId().get();
        if (!StringUtils.hasText(currentActorId)) {
            redirectAttributes.addFlashAttribute("error", "No actor ID initialized.");
            return "redirect:/app/errors/unknown-actor";
        }

        Optional<Actor> actorOpt = actorRepository.findByActorId(currentActorId);

        if (actorOpt.isEmpty()) {
            logger.error("Inconsistent state: Actor with ID {} not found in the database!", currentActorId);
            redirectAttributes.addFlashAttribute("error", "Your actor ID " + currentActorId + " could not be found.");
            return "redirect:/app/errors/unknown-actor";
        }

        Actor currentActor = actorOpt.get();
        model.addAttribute("currentActorInfo", currentActor);
        model.addAttribute("pageTitle", "Dashboard");

        model.addAttribute("apiTransactionsJson", auditService.getAllApiTransactionsAsJson());
        model.addAttribute("grpcTransactionsJson", auditService.getAllGrpcTransactionsAsJson());
        model.addAttribute("loginAttemptsJson", auditService.getAllLoginAttemptsAsJson());

        List<Unit> myUnits = unitRepository.findByCurrentOwnerActorId(currentActorId);
        String getMyUnitsJson = removeKeysFromJsonNode(objectMapper.writeValueAsString(myUnits), Set.of(""));
        model.addAttribute("getMyUnits", getMyUnitsJson);

        List<Actor> allActors = actorRepository.findAll();
        String getAllActorsJson = removeKeysFromJsonNode(objectMapper.writeValueAsString(allActors), Set.of("actorId", "ipfsLink", "docType"));
        model.addAttribute("getAllActors", getAllActorsJson);

        List<Medikament> allMedikamente = medikamentRepository.findAll();
        String getAllMedikamenteJson = removeKeysFromJsonNode(objectMapper.writeValueAsString(allMedikamente), Set.of("medId", "herstellerId", "infoblattHash", "approvedById", "ipfsLink", "docType", "ipfsData"));
        model.addAttribute("getAllMedikamente", getAllMedikamenteJson);

        try {
            Map<String, Object> kpiData = new LinkedHashMap<>();
            List<?> mainTableData;
            String tableTitle = "";

            switch (currentActor.getRole().toLowerCase()) {
                case "hersteller":
                    mainTableData = medikamentRepository.findByHerstellerId(currentActorId);
                    tableTitle = "My Medications";
                    kpiData.put("Own Medications", mainTableData.size());
                    kpiData.put("My Units", unitRepository.findByCurrentOwnerActorId(currentActorId).size());
                    kpiData.put("All Wholesalers", searchService.searchActors("grosshaendler", null, null).size());
                    break;
                case "grosshaendler":
                case "apotheke":
                    mainTableData = unitRepository.findByCurrentOwnerActorId(currentActorId);
                    tableTitle = "My Inventory (Units)";
                    kpiData.put("Owned Units", mainTableData.size());
                    kpiData.put("All Pharmacies", searchService.searchActors("apotheke", null, null).size());
                    kpiData.put("All Wholesalers", searchService.searchActors("grosshaendler", null, null).size());
                    break;
                case "behoerde":
                    mainTableData = medikamentRepository.findAll();
                    tableTitle = "All Medications in the System";
                    kpiData.put("Registered Medications", mainTableData.size());
                    kpiData.put("Registered Actors", actorRepository.count());
                    kpiData.put("All Units in Circulation", unitRepository.count());
                    break;
                default:
                    mainTableData = List.of();
                    tableTitle = "No data for your role";
                    kpiData.put("Info", "No specific view is available for your role.");
                    break;
            }

            model.addAttribute("kpiDataJson", objectMapper.writeValueAsString(kpiData));
            model.addAttribute("mainTableDataJson", objectMapper.writeValueAsString(mainTableData));
            model.addAttribute("mainTableTitle", tableTitle);

        } catch (JsonProcessingException e) {
            logger.error("Error serializing dashboard data for actor {}: {}", currentActorId, e.getMessage(), e);
            model.addAttribute("dashboardError", "Error preparing view.");
        } catch (Exception e) {
            logger.error("An unexpected error occurred while loading the dashboard for actor {}: {}", currentActorId, e.getMessage(), e);
            model.addAttribute("dashboardError", "An unexpected error occurred.");
        }

        return "dashboard/overview";
    }

    @GetMapping("/errors/unknown-actor")
    public String showUnknownActorErrorPage() {
        return "errors/unknown-actor";
    }

    // Helper methods for removing JSON keys remain unchanged
    private String removeKeysFromJsonNode(String jsonString, Set<String> keysToRemove) throws IOException {
        JsonNode rootNode = objectMapper.readTree(jsonString);
        JsonNode filteredNode = removeKeysFromJsonNode(rootNode, keysToRemove);
        return objectMapper.writeValueAsString(filteredNode);
    }

    private JsonNode removeKeysFromJsonNode(JsonNode node, Set<String> keysToRemove) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            List<String> fieldNames = new ArrayList<>();
            objectNode.fieldNames().forEachRemaining(fieldNames::add);

            for (String fieldName : fieldNames) {
                if (keysToRemove.contains(fieldName)) {
                    objectNode.remove(fieldName);
                } else {
                    JsonNode childNode = objectNode.get(fieldName);
                    if (childNode != null) {
                        objectNode.set(fieldName, removeKeysFromJsonNode(childNode, keysToRemove));
                    }
                }
            }
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (int i = 0; i < arrayNode.size(); i++) {
                arrayNode.set(i, removeKeysFromJsonNode(arrayNode.get(i), keysToRemove));
            }
        }
        return node;
    }
}
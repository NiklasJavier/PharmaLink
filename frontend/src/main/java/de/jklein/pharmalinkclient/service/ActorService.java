package de.jklein.pharmalinkclient.service;

import de.jklein.pharmalinkclient.config.BackendConfig;
import de.jklein.pharmalinkclient.dto.ActorResponseDto;
import de.jklein.pharmalinkclient.dto.ActorUpdateRequestDto;
import de.jklein.pharmalinkclient.dto.MedikamentResponseDto;
import de.jklein.pharmalinkclient.dto.ActorIdResponse;
import de.jklein.pharmalinkclient.security.UserSession;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringComponent
@UIScope
public class ActorService {

    private final RestTemplate restTemplate;
    private final BackendConfig backendConfig;
    private final UserSession userSession;

    public ActorService(BackendConfig backendConfig, UserSession userSession) {
        this.restTemplate = new RestTemplate();
        this.backendConfig = backendConfig;
        this.userSession = userSession;
    }

    private HttpEntity<String> createHttpEntityWithJwt() {
        HttpHeaders headers = new HttpHeaders();
        String jwt = userSession.getJwt();
        if (jwt != null && !jwt.isEmpty()) {
            headers.setBearerAuth(jwt);
        } else {
            System.err.println("No JWT token available in UserSession. Authentication required.");
        }
        return new HttpEntity<>(headers);
    }

    private <T> HttpEntity<T> createHttpEntityWithJwtAndBody(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jwt = userSession.getJwt();
        if (jwt != null && !jwt.isEmpty()) {
            headers.setBearerAuth(jwt);
        } else {
            System.err.println("No JWT token available in UserSession. Authentication required.");
        }
        return new HttpEntity<>(body, headers);
    }

    public List<ActorResponseDto> searchActors(String role, String bezeichnung, String actorId) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(backendConfig.getBaseUrl() + "/v1/search/actors");

        if (role != null && !role.isEmpty()) {
            uriBuilder.queryParam("role", role);
        }
        if (bezeichnung != null && !bezeichnung.isEmpty()) {
            uriBuilder.queryParam("bezeichnung", bezeichnung);
        }
        if (actorId != null && !actorId.isEmpty()) {
            uriBuilder.queryParam("actorId", actorId);
        }

        String url = uriBuilder.toUriString();
        HttpEntity<String> entity = createHttpEntityWithJwt();

        try {
            ResponseEntity<ActorResponseDto[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ActorResponseDto[].class
            );
            if (response.getBody() != null) {
                return Arrays.asList(response.getBody());
            }
        } catch (HttpClientErrorException e) {
            System.err.println("Error searching/filtering actors: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("General error searching/filtering actors: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    public ActorResponseDto getHerstellerById(String herstellerId) {
        String url = backendConfig.getBaseUrl() + "/v1/hersteller/" + herstellerId; // Corrected URI path
        HttpEntity<String> entity = createHttpEntityWithJwt();

        try {
            ResponseEntity<ActorResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ActorResponseDto.class
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            System.err.println("Error loading manufacturer details for ID '" + herstellerId + "': " + e.getStatusCode() + " " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("General error loading manufacturer details by ID: " + e.getMessage());
        }
        return null;
    }

    public List<MedikamentResponseDto> getMedicationsByHersteller(String herstellerId) { // NEW: Method added
        String url = backendConfig.getBaseUrl() + "/v1/hersteller/" + herstellerId + "/medications"; // Corrected URI path
        HttpEntity<String> entity = createHttpEntityWithJwt();

        try {
            ResponseEntity<MedikamentResponseDto[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    MedikamentResponseDto[].class
            );
            if (response.getBody() != null) {
                return Arrays.asList(response.getBody());
            }
        } catch (HttpClientErrorException e) {
            System.err.println("Error loading medications for manufacturer ID '" + herstellerId + "': " + e.getStatusCode() + " " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("General error loading medications for manufacturer: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    public ActorResponseDto getMyHerstellerInfo() {
        String url = backendConfig.getBaseUrl() + "/v1/hersteller/me";
        HttpEntity<String> entity = createHttpEntityWithJwt();

        try {
            ResponseEntity<ActorResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ActorResponseDto.class
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            System.err.println("Error loading own manufacturer information: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("General error loading own manufacturer information: " + e.getMessage());
        }
        return null;
    }

    public ActorIdResponse getMyActorId() {
        String url = backendConfig.getBaseUrl() + "/v1/hersteller/id";
        HttpEntity<String> entity = createHttpEntityWithJwt();

        try {
            ResponseEntity<ActorIdResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ActorIdResponse.class
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            System.err.println("Error loading own actor ID: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("General error loading own actor ID: " + e.getMessage());
        }
        return null;
    }

    public boolean updateActor(String actorId, ActorUpdateRequestDto requestDto) {
        String url = backendConfig.getBaseUrl() + "/v1/actors/" + actorId;
        HttpEntity<ActorUpdateRequestDto> entity = createHttpEntityWithJwtAndBody(requestDto);

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
            String jsonOutput = mapper.writeValueAsString(requestDto);
            System.out.println("----- Outgoing JSON for actor update -----");
            System.out.println(jsonOutput);
            System.out.println("-------------------------------------------");
        } catch (JsonProcessingException jsonEx) {
            System.err.println("Error serializing DTO to JSON: " + jsonEx.getMessage());
        }

        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    Void.class
            );
            System.out.println("Actor with ID " + actorId + " successfully updated.");
            return true;
        } catch (HttpClientErrorException e) {
            System.err.println("Error updating actor with ID '" + actorId + "': " + e.getStatusCode() + " " + e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            System.err.println("General error updating actor: " + e.getMessage());
            return false;
        }
    }
}
package de.jklein.pharmalink.runner;

import de.jklein.pharmalink.client.fabric.FabricClient;
import de.jklein.pharmalink.domain.Actor;
import de.jklein.pharmalink.service.state.SystemStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AppDataInitializer implements CommandLineRunner {

    private final FabricClient fabricClient;
    private final SystemStateService systemStateService;

    @Override
    public void run(String... args) {
        log.info("Starting application data initialization...");
        try {
            log.info("Executing 'initCall' on chaincode to identify the current actor...");

            String actorJson = fabricClient.submitGenericTransaction("initCall");
            Actor initializedActor = fabricClient.getGson().fromJson(actorJson, Actor.class);
            String actorIdFromChaincode = initializedActor.getActorId();

            systemStateService.reconcileAndCacheActorId(actorIdFromChaincode);

            log.info("Application data initialization completed. Current actor: {}", actorIdFromChaincode);

        } catch (Exception e) {
            log.error("Could not identify the current actor via 'initCall' from chaincode. Continuing application without a specific actor context.", e);
        }
    }
}
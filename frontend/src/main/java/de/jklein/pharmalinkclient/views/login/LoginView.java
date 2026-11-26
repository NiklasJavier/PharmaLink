package de.jklein.pharmalinkclient.views.login;

import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.jklein.pharmalinkclient.security.UserSession;
import de.jklein.pharmalinkclient.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Route("login")
@PageTitle("Login | PharmaLink")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger log = LoggerFactory.getLogger(LoginView.class);
    private final UserSession userSession;
    private final LoginOverlay loginOverlay = new LoginOverlay();

    public LoginView(AuthService authService, UserSession userSession) {
        this.userSession = userSession;

        loginOverlay.setTitle("PharmaLink");
        loginOverlay.setDescription("Secure Supply Chain Authentication");
        loginOverlay.setOpened(false);

        loginOverlay.setForgotPasswordButtonVisible(false);

        add(loginOverlay);

        loginOverlay.addLoginListener(event -> {
            final String username = event.getUsername();
            final String password = event.getPassword();

            log.info("Login attempt for user '{}'", username);

            loginOverlay.setEnabled(false);

            authService.fetchToken(username, password)
                    .whenComplete((tokenOptional, ex) -> {
                        getUI().ifPresent(ui -> ui.access(() -> {
                            loginOverlay.setEnabled(true);
                            if (tokenOptional != null && tokenOptional.isPresent()) {
                                log.info("Token received. Redirecting to session creation for user '{}'.", username);
                                String url = String.format("login/token/%s/%s", username, tokenOptional.get());
                                ui.getPage().setLocation(url);
                            } else {
                                log.warn("Login failed for user: {}", username);
                                loginOverlay.setError(true);
                            }
                        }));
                    });
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (userSession.isLoggedIn()) {
            event.forwardTo("");
        } else {
            loginOverlay.setOpened(true);
        }
    }
}
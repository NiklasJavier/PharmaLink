package de.jklein.pharmalink.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    private void addGlobalAttributesForErrorPage(Model model) {
        String backendVersion = "1.0.1";
        model.addAttribute("backendVersion", backendVersion);
    }

    @GetMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        addGlobalAttributesForErrorPage(model);

        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object errorType = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        if (model.containsAttribute("flashErrorForUnknownActor")) {
            model.addAttribute("errorMessage", model.getAttribute("flashErrorForUnknownActor"));
            if (model.containsAttribute("initialActorId")) {
                model.addAttribute("path", model.getAttribute("initialActorId"));
            }
            model.addAttribute("pageTitle", "Actor Not Found");
            return "errors/unknown-actor";
        }

        String statusString = (status != null) ? status.toString() : "Unknown";
        model.addAttribute("status", statusString);
        model.addAttribute("error", (errorType != null) ? errorType.toString() : "Unknown Error");
        model.addAttribute("path", (path != null && !path.toString().isEmpty()) ? path.toString() : "Unknown Path");
        model.addAttribute("timestamp", new java.util.Date());

        if (exception instanceof Throwable) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            ((Throwable) exception).printStackTrace(pw);
            model.addAttribute("exceptionStackTrace", sw.toString());
            model.addAttribute("exceptionMessage", ((Throwable) exception).getMessage());
        }

        if (status instanceof Integer) {
            int statusCode = (Integer) status;
            if (statusCode == HttpStatus.UNAUTHORIZED.value()) { // 401 Unauthorized
                model.addAttribute("pageTitle", "Unauthorized");
                model.addAttribute("errorMessage", "You are not signed in or your session has expired.");
                model.addAttribute("message", "Please sign in to continue."); // Additional instruction
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) { // 403 Forbidden
                model.addAttribute("pageTitle", "Access Denied");
                model.addAttribute("errorMessage", "You do not have permission to access this resource.");
                model.addAttribute("message", null); // No additional message for 403
            } else if (statusCode == HttpStatus.NOT_FOUND.value()) { // 404 Not Found
                model.addAttribute("pageTitle", "Page Not Found");
                model.addAttribute("errorMessage", "The requested URL was not found on this server.");
                model.addAttribute("message", (message != null) ? message.toString() : null); // Keep original message or null
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) { // 500 Internal Server Error
                model.addAttribute("pageTitle", "Internal Server Error");
                model.addAttribute("errorMessage", "We apologize for the inconvenience. An unexpected server error occurred.");
                model.addAttribute("message", "Our team has been notified and is already working on a solution.");
            } else { // Other status codes
                model.addAttribute("pageTitle", "Error " + statusCode);
                model.addAttribute("errorMessage", "An error occurred: " + (message != null ? message.toString() : "Details unknown."));
                model.addAttribute("message", null);
            }
        } else { // Fallback when status is null or not an Integer
            model.addAttribute("pageTitle", "Error");
            model.addAttribute("errorMessage", "An unexpected error occurred.");
            model.addAttribute("message", null);
        }

        return "errors/error";
    }

    @GetMapping("/web/error/{code}")
    public String showSpecificErrorPage(@PathVariable int code, Model model) {
        addGlobalAttributesForErrorPage(model);
        model.addAttribute("status", code);
        model.addAttribute("path", "/web/error/" + code);
        model.addAttribute("timestamp", new java.util.Date());

        if (code == HttpStatus.UNAUTHORIZED.value()) {
            model.addAttribute("pageTitle", "Unauthorized");
            model.addAttribute("errorMessage", "You are not signed in or your session has expired.");
        } else if (code == HttpStatus.FORBIDDEN.value()) {
            model.addAttribute("pageTitle", "Access Denied");
            model.addAttribute("errorMessage", "You do not have permission to access this resource.");
        } else if (code == HttpStatus.NOT_FOUND.value()) {
            model.addAttribute("pageTitle", "Page Not Found");
            model.addAttribute("errorMessage", "The requested URL could not be found.");
        } else if (code == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            model.addAttribute("pageTitle", "Internal Server Error");
            model.addAttribute("errorMessage", "An unexpected server error occurred.");
        } else {
            model.addAttribute("pageTitle", "Error " + code);
            model.addAttribute("errorMessage", "A specific error occurred.");
        }
        return "errors/error";
    }
}
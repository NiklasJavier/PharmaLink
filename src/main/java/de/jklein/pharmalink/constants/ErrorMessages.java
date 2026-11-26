package de.jklein.pharmalink.constants;

public final class ErrorMessages {

    private ErrorMessages() { }

    public static final String DEFAULT_TITLE = "An error occurred.";
    public static final String DEFAULT_MESSAGE = "We apologize for the inconvenience. There was a problem processing your request. Please try again later or return to the home page.";

    public static final String TITLE_404 = "Page Not Found";
    public static final String MESSAGE_404 = "The requested page could not be found. The address may have been entered incorrectly or the page no longer exists.";

    public static final String TITLE_500 = "Internal Server Error";
    public static final String MESSAGE_500 = "We apologize for the inconvenience. An unexpected server error occurred. Our team has been notified and is already working on a solution.";

    public static final String NAV_HOME = "Home";

    public static String getTitleForCode(int code) {
        return switch (code) {
            case 404 -> TITLE_404;
            case 500 -> TITLE_500;
            default -> DEFAULT_TITLE;
        };
    }

    public static String getMessageForCode(int code) {
        return switch (code) {
            case 404 -> MESSAGE_404;
            case 500 -> MESSAGE_500;
            default -> DEFAULT_MESSAGE;
        };
    }
}
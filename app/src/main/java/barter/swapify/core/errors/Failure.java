package barter.swapify.core.errors;

public class Failure {
    private final String message;

    public Failure(String message) {
        this.message = message != null ? message : "An unexpected error occurred.";
    }

    public String getErrorMessage() {
        return message;
    }
}

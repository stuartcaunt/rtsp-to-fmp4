package eu.ill.rtsptofmp4.models.exceptions;

public class StreamingException extends Exception {

    public StreamingException(String message) {
        super(message);
    }

    public StreamingException(String messageFormat, Object... args) {
        super(String.format(messageFormat, args));
    }
}

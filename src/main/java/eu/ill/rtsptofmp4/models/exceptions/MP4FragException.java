package eu.ill.rtsptofmp4.models.exceptions;

public class MP4FragException extends Exception {

    public MP4FragException(String message) {
        super(message);
    }

    public MP4FragException(String messageFormat, Object... args) {
        super(String.format(messageFormat, args));
    }
}

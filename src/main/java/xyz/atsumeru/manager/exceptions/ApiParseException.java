package xyz.atsumeru.manager.exceptions;

import lombok.Getter;

public class ApiParseException extends RuntimeException {
    @Getter private String errorTitle;
    @Getter private String errorDescription;

    public ApiParseException(String errorMessage) {
        super(errorMessage);
    }

    public ApiParseException(String errorMessage, String errorDescription) {
        super(errorMessage);
        this.errorDescription = errorDescription;
    }

    public ApiParseException(String errorMessage, String errorTitle, String errorDescription) {
        super(errorMessage);
        this.errorTitle = errorTitle;
        this.errorDescription = errorDescription;
    }
}
package api.common;

public class InvalidQueryParameterException extends RuntimeException {

    public InvalidQueryParameterException(String parameterName) {
        super("Invalid value for parameter '" + parameterName + "'");
    }
}

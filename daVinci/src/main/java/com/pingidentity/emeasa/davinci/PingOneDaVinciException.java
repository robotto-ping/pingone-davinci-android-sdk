package com.pingidentity.emeasa.davinci;

public class PingOneDaVinciException extends Exception{

    public static final String ERROR_INVALID_HANDLER_CLASS = "Invalid Handler Class name - does not implement interface";
    public static final String NO_HANDLER_FOR_CAPABILITY= "No Handler class configured for capability %s";
    public static final String NO_HANDLER_FOR_ACTION= "No Handler class configured for action %s";
    public static final String CANNOT_INSTANTIATE_HANDLER = "Cannot create instance of handler class";
    public static final String FLOW_ERROR = "Received error response from DaVinci: %s %s";
    public static final String TOO_MANY_ACTIONS = "Too many non-form actions. Check flow definition";
    public static final String  NO_MAPPER_FOR_TEMPLATE = "No Mapper class configured for template %s";
    public static final String  MISSING_PERMISSION = "Permission required but has not been granted";
    public static final String NO_FIDO_AUTHENTICATOR = "No FIDO Authenticator available";
    public PingOneDaVinciException(String errorMessage) {
        super(errorMessage);
    }
}

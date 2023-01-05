package com.pingidentity.emeasa.davinci.api;

public class Field {
    public static final String TEXT = "text" ;
    public static final String INPUT = "input" ;
    public static final String HIDDEN = "hidden" ;
    public static final String DATA = "data";

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isMasked() {
        return masked;
    }

    public void setMasked(boolean masked) {
        this.masked = masked;
    }
    private String type;
    private String parameterName;
    private String value;
    private boolean masked;

}

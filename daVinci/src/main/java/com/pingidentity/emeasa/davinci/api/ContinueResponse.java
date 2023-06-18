package com.pingidentity.emeasa.davinci.api;

import java.util.ArrayList;
import java.util.List;


public class ContinueResponse implements Cloneable {
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    public boolean hasDataFields() {
        for (Field f: this.fields) {
            if (f.getType().equalsIgnoreCase(Field.DATA)) {
                return true;
            }
        }
        return false;
    }
    public boolean hasDataField(String fieldName)  {
        for (Field f: this.fields) {
            if (f.getType().equalsIgnoreCase(Field.DATA) && f.getParameterName().equalsIgnoreCase(fieldName)) {
                return true;
            }
        }
        return false;
    }

    public String getDataFieldValue(String fieldName) {
        for (Field f: this.fields) {
            if (f.getType().equalsIgnoreCase(Field.DATA) && f.getParameterName().equalsIgnoreCase(fieldName)) {
                return f.getValue();
            }
        }
        return null;
    }

    public List<Action> getFormSubmitActions() {
        List<Action> formSubmitActions = new ArrayList<>();
        for (Action a: this.getActions()) {
            if (a.getType().equalsIgnoreCase(Action.SUBMIT_FORM)) {
                formSubmitActions.add(a);
            }
        }
        return formSubmitActions;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public boolean hasAutoSubmitAction() {
        for (Action a : getActions()) {
            if (a.getType().equalsIgnoreCase(Action.SUBMIT_FORM)) {
                if (a.getInputData() != null && a.getInputData().get(Action.AUTO_SUBMIT_INTERVAL) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public Action getAutoSubmitAction() {
        for (Action a : getActions()) {
            if (a.getType().equalsIgnoreCase(Action.SUBMIT_FORM)) {
                if (a.getInputData() != null && a.getInputData().get(Action.AUTO_SUBMIT_INTERVAL) != null) {
                    return a;
                }
            }
        }
        return null;
    }

    public int getAutoSubmitDelay() {
        Action a = getAutoSubmitAction();
        if (a != null) {
            return (int) a.getInputData().get(Action.AUTO_SUBMIT_INTERVAL);
        }
        return 0;
    }

    public int getPollRetries() {
        Action a = getAutoSubmitAction();
        if (a != null) {
            return (int) a.getInputData().get(Action.MAX_RETRIES);
        }
        return 0;
    }

    private String title;
    private List<Field> fields;
    private List<Action> actions;
}

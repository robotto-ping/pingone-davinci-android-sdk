package com.pingidentity.emeasa.davinci.api;

import org.json.JSONException;
import org.json.JSONObject;

public class NextPayload {

    private String id;
    private String eventName;
    private JSONObject nextEvent;
    private JSONObject parameters;

    public NextPayload(String id, String eventName, JSONObject nextEvent) {
        this.id = id;
        this.eventName = eventName;
        this.nextEvent = nextEvent;

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;

    }

    public JSONObject getNextEvent() {
        return nextEvent;
    }

    public void setNextEvent(JSONObject nextEvent) {
        this.nextEvent = nextEvent;
    }

    public JSONObject getParameters() {
        return parameters;
    }

    public void setParameters(JSONObject parameters) {
        this.parameters = parameters;
    }

    public JSONObject toJSON(JSONObject parameters) throws JSONException {
        JSONObject result = new JSONObject()
                .put("id", getId())
                .put("eventName", getEventName())
                .put("nextEvent", getNextEvent())
                .put("parameters", parameters);
        return  result;

    }


}

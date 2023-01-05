package com.pingidentity.emeasa.davinci;

import com.pingidentity.emeasa.davinci.api.NextPayload;

import org.json.JSONObject;

public class DaVinciFlowContext {

    private String responseId;
    private String interactionId;
    private String interactionToken;
    private String connectionId;
    private String capabilityName;


    private NextPayload nextPayload;




    public DaVinciFlowContext(String responseId, String interactionId, String interactionToken, String capabilityName, String connectionId) {
        this.responseId = responseId;
        this.interactionId = interactionId;
        this.interactionToken = interactionToken;
        this.capabilityName = capabilityName;
        this.connectionId = connectionId;
    }

    public DaVinciFlowContext(JSONObject apiResponse) {
        try {
            if (apiResponse.has("id"))
                responseId = apiResponse.getString("id");
            if (apiResponse.has("interactionId"))
                interactionId = apiResponse.getString("interactionId");
            if (apiResponse.has("interactionToken"))
                interactionToken = apiResponse.getString("interactionToken");
            if (apiResponse.has("capabilityName"))
                capabilityName = apiResponse.getString("capabilityName");
            if (apiResponse.has("connectionId"))
                connectionId = apiResponse.getString("connectionId");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getCapabilityName() {
        return capabilityName;
    }

    public void setCapabilityName(String capabilityName) {
        this.capabilityName = capabilityName;
    }

    public String getResponseId() {
        return responseId;
    }

    public void setResponseId(String responseId) {
        this.responseId = responseId;
    }

    public String getInteractionId() {
        return interactionId;
    }

    public void setInteractionId(String interactionId) {
        this.interactionId = interactionId;
    }

    public String getInteractionToken() {
        return interactionToken;
    }

    public void setInteractionToken(String interactionToken) {
        this.interactionToken = interactionToken;
    }

    public NextPayload getNextPayload() {
        return nextPayload;
    }

    public void setNextPayload(NextPayload nextPayload) {
        this.nextPayload = nextPayload;
    }


}

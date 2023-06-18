package com.pingidentity.emeasa.davinci.payloadhandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingidentity.emeasa.davinci.DaVinciResponsePayloadHandler;
import com.pingidentity.emeasa.davinci.PingOneDaVinciException;
import com.pingidentity.emeasa.davinci.api.ContinueResponse;
import com.pingidentity.emeasa.davinci.api.FlowResponse;
import com.pingidentity.emeasa.davinci.api.NextPayload;


import org.json.JSONObject;

public class CustomHTMLTemplateResponsePayloadHandler  implements DaVinciResponsePayloadHandler {

    private ContinueResponse continueResposne;
    private NextPayload nextPayload;

    @Override
    public void initialise(JSONObject response) throws PingOneDaVinciException {
        try {
            JSONObject flowResponseValue = response.getJSONObject("screen").getJSONObject("properties").getJSONObject("customHTML").getJSONObject("value").getJSONObject("properties");
            ObjectMapper mapper = new ObjectMapper();
            this.continueResposne = mapper.readValue(flowResponseValue.toString(), ContinueResponse.class);

            String id = response.getString("id");
            JSONObject onClickObject =  response.getJSONObject("screen").getJSONObject("properties").getJSONObject("button").getJSONObject("onClick");
            this.nextPayload = new NextPayload(id, onClickObject.getString("eventName"), onClickObject );

        } catch (Exception e) {
            throw new PingOneDaVinciException("Cannot initialise Response Payload Handler ::" + e.getMessage());
        }
    }

    @Override
    public boolean isTerminalStep() {
        return false;
    }

    @Override
    public FlowResponse getFlowResponse() {
        return null;
    }

    @Override
    public NextPayload getNextPayload() {
        return nextPayload;
    }

    @Override
    public ContinueResponse getContinueResponse() {
        return continueResposne;
    }
}

package com.pingidentity.emeasa.davinci.payloadhandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingidentity.emeasa.davinci.DaVinciResponsePayloadHandler;
import com.pingidentity.emeasa.davinci.PingOneDaVinciException;
import com.pingidentity.emeasa.davinci.api.Action;
import com.pingidentity.emeasa.davinci.api.ContinueResponse;
import com.pingidentity.emeasa.davinci.api.Field;
import com.pingidentity.emeasa.davinci.api.FlowResponse;
import com.pingidentity.emeasa.davinci.api.NextPayload;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DaVinciScreenResponsePayloadHandler implements DaVinciResponsePayloadHandler {

    private ContinueResponse continueResposne;
    private NextPayload nextPayload;

    @Override
    public void initialise(JSONObject response) throws PingOneDaVinciException {
        try {
            String id = response.getString("id");
            JSONObject nextEventObject =  response.getJSONObject("screen").getJSONObject("properties").getJSONObject("screen0Config").getJSONObject("properties").getJSONObject("nextEvent");
            this.nextPayload = new NextPayload(id, nextEventObject.getString("eventName"), nextEventObject );
            this.continueResposne = new ContinueResponse();
            JSONObject props = response.getJSONObject("screen").getJSONObject("properties").getJSONObject("screen0Config").getJSONObject("properties");
            continueResposne.setTitle(props.getJSONObject("navTitle").getString("value"));
            JSONArray screenComponents = props.getJSONObject("screenComponentList").getJSONArray("value");
            List<Field> fields = new ArrayList<>();
            for (int i = 0; i < screenComponents.length(); i++ ) {
                JSONObject component = screenComponents.getJSONObject(i);
                if ("label".equalsIgnoreCase(component.getString("preferredControlType"))) {
                    Field f = new Field();
                    f.setType(Field.TEXT);
                    f.setValue(component.getString("value"));
                    fields.add(f);

                } else  if ("textField".equalsIgnoreCase(component.getString("preferredControlType"))) {
                    Field f = new Field();
                    f.setType(Field.INPUT);
                    f.setValue(component.getString("value"));
                    f.setParameterName(component.getString("propertyName"));
                    fields.add(f);
                }
            }
            this.continueResposne.setFields(fields);
            Action a = new Action();
            a.setActionValue("submit");
            a.setType(Action.SUBMIT_FORM);
            a.setDescriptionText(props.getJSONObject("nextButtonText").getString("value"));
            this.continueResposne.setActions(Collections.singletonList(a));
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

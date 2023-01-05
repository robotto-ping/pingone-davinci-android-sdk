package com.pingidentity.emeasa.davinci.actionhandler;

import android.content.Context;

import com.pingidentity.emeasa.davinci.DaVinciFlowActionHandler;
import com.pingidentity.emeasa.davinci.PingOneDaVinci;
import com.pingidentity.emeasa.davinci.PingOneDaVinciException;
import com.pingidentity.emeasa.davinci.api.Action;
import com.pingidentity.emeasa.davinci.api.ContinueResponse;
import com.pingidentity.emeasa.davinci.api.Field;

import org.json.JSONException;
import org.json.JSONObject;

public class FormSubmitActionHandler implements DaVinciFlowActionHandler {
    private static final String ACTION_VALUE = "actionValue";

    @Override
    public void handle(ContinueResponse continueResponse, Context context, PingOneDaVinci pingOneDaVinci) throws PingOneDaVinciException {
        JSONObject parameters = new JSONObject();
        try {
            for (Field f: continueResponse.getFields()) {
                if (f.getType().equalsIgnoreCase(Field.INPUT) || f.getType().equalsIgnoreCase(Field.HIDDEN)) {

                        parameters.put(f.getParameterName(), f.getValue());

                }
            }
            for (Action a: continueResponse.getActions()) {
                if (a.getType().equalsIgnoreCase(Action.SUBMIT_FORM)) {
                    parameters.put(ACTION_VALUE, a.getActionValue());
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            throw new PingOneDaVinciException(e.getMessage());
        }
        pingOneDaVinci.continueFlow(parameters, context);
    }
}

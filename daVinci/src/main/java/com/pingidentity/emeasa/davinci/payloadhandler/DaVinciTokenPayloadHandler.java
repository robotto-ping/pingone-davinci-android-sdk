package com.pingidentity.emeasa.davinci.payloadhandler;

import com.pingidentity.emeasa.davinci.DaVinciResponsePayloadHandler;
import com.pingidentity.emeasa.davinci.PingOneDaVinciException;
import com.pingidentity.emeasa.davinci.api.ContinueResponse;
import com.pingidentity.emeasa.davinci.api.FlowResponse;
import com.pingidentity.emeasa.davinci.api.NextPayload;

import org.json.JSONObject;



public class DaVinciTokenPayloadHandler  implements DaVinciResponsePayloadHandler {

    private FlowResponse flowResponse;
    @Override
    public void initialise(JSONObject response) throws PingOneDaVinciException {
         flowResponse = new FlowResponse();
         try {
             if (response.has("access_token"))
                 flowResponse.setAccessToken(response.getString("access_token"));
             if (response.has("id_token"))
                 flowResponse.setIdToken(response.getString("id_token"));
             if(response.has("success")) {
                 flowResponse.setSuccess(response.getBoolean("success"));
             }
         }catch (Exception e) {
             throw new PingOneDaVinciException(e.getMessage());
         }

    }

    @Override
    public boolean isTerminalStep() {
        return true;
    }

    @Override
    public FlowResponse getFlowResponse() {
        return flowResponse;
    }

    @Override
    public NextPayload getNextPayload() {
        return null;
    }

    @Override
    public ContinueResponse getContinueResponse() {
        return null;
    }
}

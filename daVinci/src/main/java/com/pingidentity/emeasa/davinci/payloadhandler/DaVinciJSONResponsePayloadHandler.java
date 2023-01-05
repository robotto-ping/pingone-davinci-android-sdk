package com.pingidentity.emeasa.davinci.payloadhandler;

import com.pingidentity.emeasa.davinci.DaVinciResponsePayloadHandler;
import com.pingidentity.emeasa.davinci.PingOneDaVinciException;
import com.pingidentity.emeasa.davinci.api.ContinueResponse;
import com.pingidentity.emeasa.davinci.api.FlowResponse;
import com.pingidentity.emeasa.davinci.api.NextPayload;

import org.json.JSONObject;

public class DaVinciJSONResponsePayloadHandler  implements DaVinciResponsePayloadHandler {

    private FlowResponse flowResponse;

    @Override
    public void initialise(JSONObject response) throws PingOneDaVinciException {
        flowResponse = new FlowResponse();
        try {
           /* if (response.has("access_token"))
                flowResponse.setAccessToken(response.getString("access_token"));
            if (response.has("id_token"))
                flowResponse.setIdToken(response.getString("id_token"));
            if(response.has("success")) {
                flowResponse.setSuccess(response.getBoolean("success"));
            } */
            if (response.has("capabilityName") && response.getString("capabilityName").equalsIgnoreCase("createErrorResponse")) {
                flowResponse.setSuccess(false);
            }
            if (response.has("capabilityName") && response.getString("capabilityName").equalsIgnoreCase("createSuccessResponse")) {
                flowResponse.setSuccess(true);
            }
            if (response.has("additionalProperties")) {
                flowResponse.setData(response.getJSONObject("additionalProperties"));
                if (flowResponse.hasDataField("access_token"))
                    flowResponse.setAccessToken(flowResponse.getDataFieldValue("access_token"));
                if (flowResponse.hasDataField("accessToken"))
                    flowResponse.setAccessToken(flowResponse.getDataFieldValue("accessToken"));
                if (flowResponse.hasDataField("id_token"))
                    flowResponse.setIdToken(flowResponse.getDataFieldValue("id_token"));
                if (flowResponse.hasDataField("idToken"))
                    flowResponse.setIdToken(flowResponse.getDataFieldValue("idToken"));
                if (flowResponse.hasDataField("IDToken"))
                    flowResponse.setIdToken(flowResponse.getDataFieldValue("IDToken"));
                if (flowResponse.hasDataField("ID_token"))
                    flowResponse.setIdToken(flowResponse.getDataFieldValue("ID_token"));
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

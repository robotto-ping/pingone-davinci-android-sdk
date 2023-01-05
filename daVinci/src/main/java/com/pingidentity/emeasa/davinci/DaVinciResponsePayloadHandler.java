package com.pingidentity.emeasa.davinci;

import com.pingidentity.emeasa.davinci.api.ContinueResponse;
import com.pingidentity.emeasa.davinci.api.FlowResponse;
import com.pingidentity.emeasa.davinci.api.NextPayload;

import org.json.JSONObject;

public interface DaVinciResponsePayloadHandler {

    void initialise(JSONObject response) throws PingOneDaVinciException;

    boolean isTerminalStep();

    FlowResponse getFlowResponse();

    NextPayload getNextPayload();

    ContinueResponse getContinueResponse();
}

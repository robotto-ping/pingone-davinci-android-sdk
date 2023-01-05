package com.pingidentity.emeasa.davinci;


import com.pingidentity.emeasa.davinci.api.ContinueResponse;
import com.pingidentity.emeasa.davinci.api.FlowResponse;

public interface DaVinciFlowUI {

    void onDaVinciHelperReady();
    void onDaVinciError(Throwable t);
    void onFlowComplete(FlowResponse flowResponse);
    void onFlowContinue(ContinueResponse continueResponse);
}

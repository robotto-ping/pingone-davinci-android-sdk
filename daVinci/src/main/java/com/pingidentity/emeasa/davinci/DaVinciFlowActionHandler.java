package com.pingidentity.emeasa.davinci;

import android.content.Context;

import com.pingidentity.emeasa.davinci.api.ContinueResponse;

public interface DaVinciFlowActionHandler {
    void handle(ContinueResponse continueResponse, Context context, PingOneDaVinci pingOneDaVinci) throws PingOneDaVinciException;
}

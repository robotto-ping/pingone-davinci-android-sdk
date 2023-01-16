package com.pingidentity.emeasa.davinci.actionhandler;

import android.content.Context;

import androidx.annotation.NonNull;

import com.pingidentity.emeasa.davinci.DaVinciFlowActionHandler;
import com.pingidentity.emeasa.davinci.PingOneDaVinci;
import com.pingidentity.emeasa.davinci.PingOneDaVinciException;
import com.pingidentity.emeasa.davinci.api.Action;
import com.pingidentity.emeasa.davinci.api.ContinueResponse;
import com.pingidentity.signalssdk.sdk.GetDataCallback;
import com.pingidentity.signalssdk.sdk.InitCallback;
import com.pingidentity.signalssdk.sdk.POSilentInitParams;
import com.pingidentity.signalssdk.sdk.PingOneSignals;

import org.json.JSONException;
import org.json.JSONObject;

public class PingOneRiskSignalsActionHandler implements DaVinciFlowActionHandler, InitCallback {

    private static final String ACTION_VALUE = "actionValue";

    private PingOneDaVinci pingOneDaVinci;
    private Context context;
    private ContinueResponse continueResponse;


    @Override
    public void handle(ContinueResponse continueResponse, Context context, PingOneDaVinci pingOneDaVinci) throws PingOneDaVinciException {
        this.pingOneDaVinci = pingOneDaVinci;
        this.context = context;
        this.continueResponse = continueResponse;
        PingOneSignals.setInitCallback(this);
        POSilentInitParams initParams = new POSilentInitParams(pingOneDaVinci.getEnvironmentId());
        PingOneSignals.initSilent(context, initParams);
    }

    @Override
    public void onError(@NonNull String message, @NonNull String code, @NonNull String id) {
        pingOneDaVinci.handleAsyncException(new PingOneDaVinciException(message));
    }

    @Override
    public void onInitialized() {
        PingOneSignals.getData(new GetDataCallback() {
            @Override
            public void onSuccess(@NonNull String data) {
                JSONObject parameters = new JSONObject();
                try {
                    for (Action a: continueResponse.getActions()) {
                        if (a.getType().equalsIgnoreCase("riskSignals")) {
                            parameters.put(ACTION_VALUE, a.getActionValue());
                            parameters.put(a.getParameterName(), data);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    pingOneDaVinci.handleAsyncException(new PingOneDaVinciException(e.getMessage()));
                }
                pingOneDaVinci.continueFlow(parameters, context);
            }

            @Override
            public void onFailure(@NonNull String reason) {
                pingOneDaVinci.handleAsyncException(new PingOneDaVinciException(reason));
            }
        });
    }
}

package com.pingidentity.emeasa.davinci.actionhandler;



import android.content.Context;


import androidx.annotation.Nullable;

import com.google.firebase.messaging.FirebaseMessaging;
import com.pingidentity.emeasa.davinci.DaVinciFlowActionHandler;
import com.pingidentity.emeasa.davinci.PingOneDaVinci;
import com.pingidentity.emeasa.davinci.PingOneDaVinciException;
import com.pingidentity.emeasa.davinci.api.Action;
import com.pingidentity.emeasa.davinci.api.ContinueResponse;
import com.pingidentity.pingidsdkv2.PingOne;
import com.pingidentity.pingidsdkv2.PingOneSDKError;
import com.pingidentity.pingidsdkv2.types.NotificationProvider;
import com.pingidentity.pingidsdkv2.types.PairingInfo;

import org.json.JSONException;
import org.json.JSONObject;

public class PingOneMFAActionHandler implements DaVinciFlowActionHandler {

    private static final String ACTION_VALUE = "actionValue";

    private PingOneDaVinci pingOneDaVinci;
    private Context context;
    private ContinueResponse continueResponse;


    @Override
    public void handle(ContinueResponse continueResponse, Context context, PingOneDaVinci pingOneDaVinci) throws PingOneDaVinciException {

        this.pingOneDaVinci = pingOneDaVinci;
        this.context = context;
        this.continueResponse = continueResponse;
        checkFCMRegistrationToken();

    }

    private void performAction() {
        for (Action a: continueResponse.getActions()) {
            if (a.getType().equalsIgnoreCase("devicePayload")) {
                sendDevicePayload(a);
            }
            else if (a.getType().equalsIgnoreCase("pairDevice")) {
                pairDevice(a);
            }
        }
    }

    private void pairDevice(Action action) {
        String pingOnePairingKey = (String) action.getInputData().get("pingOnePairingKey");
        PingOne.pair(context, pingOnePairingKey, new PingOne.PingOneSDKPairingCallback() {
            @Override
            public void onComplete(@Nullable PairingInfo pairingInfo, @Nullable PingOneSDKError pingOneSDKError) {
                JSONObject parameters = new JSONObject();
                try {
                    parameters.put(ACTION_VALUE, action.getActionValue());
                    pingOneDaVinci.continueFlow(parameters, context);
                } catch (JSONException e) {
                    e.printStackTrace();
                    pingOneDaVinci.handleAsyncException(new PingOneDaVinciException(e.getMessage()));
                }
            }

            @Override
            public void onComplete(@Nullable PingOneSDKError pingOneSDKError) {
                pingOneDaVinci.handleAsyncException(new PingOneDaVinciException(pingOneSDKError.getMessage()));
            }
        });


    }

    private void sendDevicePayload(Action action) {
        String mobilePayload = PingOne.generateMobilePayload(context);
        JSONObject parameters = new JSONObject();
        try {
            parameters.put(ACTION_VALUE, action.getActionValue());
            parameters.put(action.getParameterName(), mobilePayload);
            pingOneDaVinci.continueFlow(parameters, context);
        } catch (JSONException e) {
            e.printStackTrace();
            pingOneDaVinci.handleAsyncException(new PingOneDaVinciException(e.getMessage()));
        }
    }


    private void checkFCMRegistrationToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            PingOne.setDeviceToken(context, task.getResult(), NotificationProvider.FCM, new PingOne.PingOneSDKCallback() {
                @Override
                public void onComplete(@Nullable PingOneSDKError pingOneSDKError) {
                    performAction();
                }
            });
        });

    }
}

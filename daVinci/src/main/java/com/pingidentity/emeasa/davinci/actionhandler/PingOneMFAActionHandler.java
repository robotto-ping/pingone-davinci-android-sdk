package com.pingidentity.emeasa.davinci.actionhandler;



import android.app.Activity;
import android.content.Context;
import android.util.Log;


import androidx.annotation.Nullable;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.JsonObject;
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

    protected static final String ACTION_VALUE = "actionValue";

    public static final String GET_PAYLOAD_ACTION = "devicePayload";
    public static final String GET_INFO_ACTION = "getInfo";
    public static final String PAIR_DEVICE_ACTION = "pairDevice";

    protected static final String PING_ONE_PAIRING_KEY = "pingOnePairingKey" ;

    protected PingOneDaVinci pingOneDaVinci;
    protected Context context;
    protected ContinueResponse continueResponse;


    @Override
    public void handle(ContinueResponse continueResponse, Context context, PingOneDaVinci pingOneDaVinci) throws PingOneDaVinciException {

        this.pingOneDaVinci = pingOneDaVinci;
        this.context = context;
        this.continueResponse = continueResponse;
        checkFCMRegistrationToken();

    }

    protected void performAction() {

        for (Action a: continueResponse.getActions()) {
            if (a.getType().equalsIgnoreCase(GET_PAYLOAD_ACTION)) {
                sendDevicePayload(a);
            }
            else if (a.getType().equalsIgnoreCase(PAIR_DEVICE_ACTION)) {
                pairDevice(a);
            }else if (a.getType().equalsIgnoreCase(GET_INFO_ACTION)) {
                getInfo(a);
            }
        }
    }



    private void pairDevice(Action action) {

        String pingOnePairingKey = (String) action.getInputData().get(PING_ONE_PAIRING_KEY);
        PingOne.pair(context, pingOnePairingKey, new PingOne.PingOneSDKPairingCallback() {
            @Override
            public void onComplete(@Nullable PingOneSDKError pingOneSDKError) {

            }

            @Override
            public void onComplete(@Nullable PairingInfo pairingInfo, @Nullable PingOneSDKError pingOneSDKError) {

                if (pingOneSDKError != null) {

                    pingOneDaVinci.handleAsyncException(new PingOneDaVinciException(pingOneSDKError.getMessage()));
                } else {
                    ((Activity) context).runOnUiThread(() -> {
                        JSONObject parameters = new JSONObject();
                        try {
                            parameters.put(ACTION_VALUE, action.getActionValue());
                            pingOneDaVinci.continueFlow(parameters, context);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            pingOneDaVinci.handleAsyncException(new PingOneDaVinciException(e.getMessage()));
                        }
                    });
                }
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

    private void getInfo(Action action) {
        PingOne.getInfo(context, (jsonObject, pingOneSDKError) -> ((Activity)context).runOnUiThread(() -> {
            try {
                JSONObject parameters = new JSONObject();
                try {
                    parameters.put(ACTION_VALUE, action.getActionValue());
                    parameters.put(action.getParameterName(), jsonObject);
                    pingOneDaVinci.continueFlow(parameters, context);
                } catch (JSONException e) {
                    e.printStackTrace();
                    pingOneDaVinci.handleAsyncException(new PingOneDaVinciException(e.getMessage()));
                }
            } catch (Exception e) {
                pingOneDaVinci.handleAsyncException(e);
            }
        }));
    }

    private void checkFCMRegistrationToken() {

        try {
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {

                PingOne.setDeviceToken(context, task.getResult(), NotificationProvider.FCM, new PingOne.PingOneSDKCallback() {
                    @Override
                    public void onComplete(@Nullable PingOneSDKError pingOneSDKError) {
                        performAction();
                    }
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
            pingOneDaVinci.handleAsyncException(e);
        }


    }
}

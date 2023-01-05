package com.pingidentity.emeasa.davinci.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.pingidentity.emeasa.davinci.DaVinciFlowActionHandler;
import com.pingidentity.emeasa.davinci.PingOneDaVinci;
import com.pingidentity.emeasa.davinci.PingOneDaVinciException;
import com.pingidentity.emeasa.davinci.actionhandler.FIDOAttestationActionHandler;

import java.util.HashMap;
import java.util.Map;

public class DaVinciActivity extends AppCompatActivity {

    private static DaVinciActivity instance;
    private PingOneDaVinci pingOneDaVinci;

    public static int ATTESTATION_REQUEST = 1566;
    public static int ASSERTION_REQUEST = 1567;

    private Map<String, DaVinciFlowActionHandler> handlers;

    {
        handlers = new HashMap<>();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
    }

    public static DaVinciActivity getInstance() {
        return instance;
    }

    public void registerHandlerInstance(String className, DaVinciFlowActionHandler handler) {
        handlers.put(className, handler);
    }


    @Override
    public void onActivityResult(int requestCode,
                                 int resultCode,
                                 Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (ATTESTATION_REQUEST == requestCode) {
            if (handlers.containsKey("FIDOAttestationActionHandler")) {
                FIDOAttestationActionHandler handler = (FIDOAttestationActionHandler) handlers.get("FIDOAttestationActionHandler");
                handler.handleAttestationResponse(resultCode, data);
                handlers.remove("FIDOAttestationActionHandler");
            } else {
                pingOneDaVinci.handleAsyncException(new PingOneDaVinciException("No Handler Registered"));
            }

        }
    }

    public void setDaVinci(PingOneDaVinci pingOneDaVinci) {
        this.pingOneDaVinci = pingOneDaVinci;
    }
}
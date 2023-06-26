package com.pingidentity.emeasa.davincisample;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;


import com.google.firebase.messaging.FirebaseMessaging;
import com.pingidentity.emeasa.davinci.DaVinciFlowUI;
import com.pingidentity.emeasa.davinci.DaVinciForm;
import com.pingidentity.emeasa.davinci.PingOneDaVinci;
import com.pingidentity.emeasa.davinci.PingOneDaVinciException;
import com.pingidentity.emeasa.davinci.api.ContinueResponse;
import com.pingidentity.emeasa.davinci.api.FlowResponse;

import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements DaVinciFlowUI {

    private PingOneDaVinci daVinci;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            String token = task.getResult();
            Log.d("Foo","FCM Token = " + task.getResult());
        });

        setContentView(R.layout.activity_main);
        daVinci = new PingOneDaVinci(this, DaVinciEnvironment.COMPANY_ID, DaVinciEnvironment.REGION, this) ;
        daVinci.requestNotificationPermission();
        daVinci.initialise(DaVinciEnvironment.API_KEY);
    }

    @Override
    public void onDaVinciReady() {
        if (daVinci.hasValidToken()) {
            daVinci.startFlowPolicy(DaVinciEnvironment.MAIN_POLICY_ID, this);
        }
    }

    @Override
    public void onDaVinciError(Throwable t) {
        Toast.makeText(this, "ERROR::" + t.getMessage(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onFlowComplete(FlowResponse flowResponse) {
        System.out.println(flowResponse);
        findViewById(R.id.davContainer).setVisibility(GONE);
        findViewById(R.id.tokenLabel).setVisibility(VISIBLE);
        String id = flowResponse.getIdToken();
        ( (TextView)findViewById(R.id.tokenLabel)).setText("Token:: " + id);
       /* if (flowResponse.getUserSession() != null) {
            daVinci.startFlowPolicy(DaVinciEnvironment.MAIN_POLICY_ID, this,flowResponse.getUserSession() );
        } */
    }

    @Override
    public void onFlowContinue(ContinueResponse continueResponse) {
        ViewGroup layout = (ViewGroup) findViewById(R.id.davContainer);
        DaVinciForm form = new DaVinciForm(daVinci, layout, this);

       form.setTitleContainerStyle(R.style.Theme_DaVinciSDKTestApp_TitleContainer);
        form.setFieldContainerStyle(R.style.Theme_DaVinciSDKTestApp_FieldContainer);
       form.setButtonStyle(R.style.Theme_DaVinciSDKTestApp_Button);
        form.setButtonContainerStyle(R.style.Theme_DaVinciSDKTestApp_ButtonContainer);
        form.setEditViewStyle(R.style.Theme_DaVinciSDKTestApp_EditText);
        form.setHeaderTextStyle(R.style.Theme_DaVinciSDKTestApp_HeaderText);
        form.setTextStyle(R.style.Theme_DaVinciSDKTestApp_Text);
        form.setSpinnerContainerStyle(R.style.Theme_DaVinciSDKTestApp_FieldContainer);
       form.setSpinnerStyle(R.style.Theme_DaVinciSDKTestApp_Spinner);
        try {
            form.buildView(continueResponse);
        } catch (PingOneDaVinciException e) {
            e.printStackTrace();
        }
    }


}
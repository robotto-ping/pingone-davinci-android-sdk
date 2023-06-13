package com.pingidentity.emeasa.davincisample;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;


import com.pingidentity.emeasa.davinci.DaVinciFlowUI;
import com.pingidentity.emeasa.davinci.DaVinciForm;
import com.pingidentity.emeasa.davinci.PingOneDaVinci;
import com.pingidentity.emeasa.davinci.PingOneDaVinciException;
import com.pingidentity.emeasa.davinci.api.ContinueResponse;
import com.pingidentity.emeasa.davinci.api.FlowResponse;

import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements DaVinciFlowUI {

    private PingOneDaVinci daVinci;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        daVinci = new PingOneDaVinci(this, DaVinciEnvironment.COMPANY_ID, DaVinciEnvironment.REGION, this) ;
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
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(i);
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
        try {
            form.buildView(continueResponse);
        } catch (PingOneDaVinciException e) {
            e.printStackTrace();
        }
    }


}
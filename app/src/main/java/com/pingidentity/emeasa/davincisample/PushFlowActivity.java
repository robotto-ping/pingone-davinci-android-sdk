package com.pingidentity.emeasa.davincisample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Toast;

import com.pingidentity.emeasa.davinci.DaVinciFlowUI;
import com.pingidentity.emeasa.davinci.DaVinciForm;
import com.pingidentity.emeasa.davinci.PingOneDaVinci;
import com.pingidentity.emeasa.davinci.PingOneDaVinciException;
import com.pingidentity.emeasa.davinci.api.ContinueResponse;
import com.pingidentity.emeasa.davinci.api.FlowResponse;

import org.json.JSONException;
import org.json.JSONObject;

public class PushFlowActivity extends AppCompatActivity implements DaVinciFlowUI {

    private PingOneDaVinci daVinci;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push_flow);
        daVinci = new PingOneDaVinci(this, DaVinciEnvironment.COMPANY_ID, DaVinciEnvironment.REGION, this) ;
        daVinci.initialise(DaVinciEnvironment.API_KEY);

    }

    @Override
    public void onDaVinciReady() {
        if (daVinci.hasValidToken()) {
            daVinci.startFlowPolicyFromIntent( this.getIntent(), "ed1b8c7d-5bce-4a56-8cb4-5549c0db119b",this);
        }
    }

    @Override
    public void onDaVinciError(Throwable t) {
        Toast.makeText(this, "ERROR::" + t.getMessage(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onFlowComplete(FlowResponse flowResponse) {
        finishAffinity();
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
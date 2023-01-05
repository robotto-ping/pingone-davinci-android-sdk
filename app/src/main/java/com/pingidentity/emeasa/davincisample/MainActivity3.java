package com.pingidentity.emeasa.davincisample;

import static com.pingidentity.emeasa.davinci.PingOneDaVinci.EU;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;

import com.pingidentity.emeasa.davinci.DaVinciFlowUI;
import com.pingidentity.emeasa.davinci.DaVinciForm;
import com.pingidentity.emeasa.davinci.PingOneDaVinci;
import com.pingidentity.emeasa.davinci.PingOneDaVinciException;
import com.pingidentity.emeasa.davinci.api.ContinueResponse;
import com.pingidentity.emeasa.davinci.api.FlowResponse;

public class MainActivity3 extends AppCompatActivity implements DaVinciFlowUI {
    private PingOneDaVinci daVinci;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        daVinci = new PingOneDaVinci(MainActivity3.this, "16902052-7d1e-4cea-9ccd-6333bfccb544", EU);
        daVinci.initialise("bbbef0051834a64f043f97dcba81f7f73b244fe83e1b65ea9160fb1c7d58d5d66dd329eac2a69724c75758c7ebe96cb940bd438b49d84c1b4dd7eba9f8cfa8bbe857e7d8c79e81efd3bb53eddcba4ef14f12e5b2f06b16f280136ea3a63377043638ca1f3668b911c309f887af40f84ebd5b14fe81a38a2c7b86d9444126e0d0");

    }

    @Override
    public void onDaVinciHelperReady() {
        if (daVinci.hasValidToken()) {
            daVinci.startFlowPolicy("4969f5866ef278d744f845c659b105d0", this);
        }
    }

    @Override
    public void onDaVinciError(Throwable t) {

    }

    @Override
    public void onFlowComplete(FlowResponse flowResponse) {
        System.out.println(flowResponse);
        Intent i = new Intent(this, MainActivity3.class);
        i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(i);
    }

    @Override
    public void onFlowContinue(ContinueResponse continueResponse) {
        ViewGroup layout = (ViewGroup) findViewById(R.id.mainLayout);
        DaVinciForm form = new DaVinciForm(daVinci, layout, this);
        form.setButtonStyle(R.style.DaVinci_Button);
        form.setEditViewStyle(R.style.DaVinci_EditView);
        try {
            form.buildView(continueResponse);
        } catch (PingOneDaVinciException e) {
            e.printStackTrace();
        }
    }
}
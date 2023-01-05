package com.pingidentity.emeasa.davincisample;

import static com.pingidentity.emeasa.davinci.PingOneDaVinci.EU;

import android.content.Intent;
import android.os.Bundle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.pingidentity.emeasa.davinci.DaVinciFlowUI;
import com.pingidentity.emeasa.davinci.PingOneDaVinci;
import com.pingidentity.emeasa.davinci.api.ContinueResponse;
import com.pingidentity.emeasa.davinci.api.FlowResponse;
import com.pingidentity.emeasa.davincisample.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity implements DaVinciFlowUI {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private PingOneDaVinci daVinci;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 daVinci = new PingOneDaVinci(MainActivity.this, "16902052-7d1e-4cea-9ccd-6333bfccb544", EU);
                daVinci.initialise("bbbef0051834a64f043f97dcba81f7f73b244fe83e1b65ea9160fb1c7d58d5d66dd329eac2a69724c75758c7ebe96cb940bd438b49d84c1b4dd7eba9f8cfa8bbe857e7d8c79e81efd3bb53eddcba4ef14f12e5b2f06b16f280136ea3a63377043638ca1f3668b911c309f887af40f84ebd5b14fe81a38a2c7b86d9444126e0d0");
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onDaVinciReady() {
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
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(i);

    }

    @Override
    public void onFlowContinue(ContinueResponse continueResponse) {

    }


}
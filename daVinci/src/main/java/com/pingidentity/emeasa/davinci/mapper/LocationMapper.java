package com.pingidentity.emeasa.davinci.mapper;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import static com.pingidentity.emeasa.davinci.PingOneDaVinciException.MISSING_PERMISSION;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.pingidentity.emeasa.davinci.DaVinciValueMapper;
import com.pingidentity.emeasa.davinci.PingOneDaVinci;
import com.pingidentity.emeasa.davinci.PingOneDaVinciException;
import com.pingidentity.emeasa.davinci.api.Field;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class LocationMapper implements DaVinciValueMapper {

    private Field mField;
    private Context mContext;
    private PingOneDaVinci mPingOneDaVinci;

    @Override
    public void performSubstitution(Field f, Context context, PingOneDaVinci pingOneDaVinci) throws PingOneDaVinciException {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
            //f.setValue("Cannot get location");
            //pingOneDaVinci.mapperComplete(f.getParameterName(),context);
            mField = f;
            mContext = context;
            mPingOneDaVinci = pingOneDaVinci;
            pingOneDaVinci.requestMapperPermissions(Manifest.permission.ACCESS_FINE_LOCATION, this);

        } else {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener((Activity) context, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                f.setValue(String.format("%f,%f", location.getLatitude(), location.getLongitude()));
                            } else {
                                f.setValue("");
                            }
                            pingOneDaVinci.mapperComplete(f.getParameterName(), context);
                        }


                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            e.printStackTrace();
                            f.setValue("");
                            pingOneDaVinci.mapperComplete(f.getParameterName(),context);
                        }
                    });
        }
    }

    @Override
    public void retryMapping() {
        try {
            this.performSubstitution(mField, mContext, mPingOneDaVinci);
        } catch (PingOneDaVinciException e) {
            this.failMapping();
        }
    }

    @Override
    public void failMapping() {
        mField.setValue("");
        mPingOneDaVinci.mapperComplete(mField.getParameterName(), mContext);
    }
}

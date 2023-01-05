package com.pingidentity.emeasa.davinci.mapper;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import static com.pingidentity.emeasa.davinci.PingOneDaVinciException.MISSING_PERMISSION;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.location.Location;

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
    @Override
    public void performSubstitution(Field f, Context context, PingOneDaVinci pingOneDaVinci) throws PingOneDaVinciException {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
            f.setValue("Cannot get location");
            pingOneDaVinci.mapperComplete(f.getParameterName(),context);
        } else {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener((Activity) context, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                f.setValue(String.format("%f,%f", location.getLatitude(), location.getLongitude()));
                            } else {
                                f.setValue("None");
                            }
                            pingOneDaVinci.mapperComplete(f.getParameterName(), context);
                        }


                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            e.printStackTrace();
                            f.setValue("Location Not Available");
                            pingOneDaVinci.mapperComplete(f.getParameterName(),context);
                        }
                    });
        }
    }
}

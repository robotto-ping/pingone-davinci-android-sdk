package com.pingidentity.emeasa.davinci.mapper;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import static com.pingidentity.emeasa.davinci.PingOneDaVinciException.MISSING_PERMISSION;

import android.Manifest;
import android.content.Context;
import android.telephony.TelephonyManager;

import androidx.core.app.ActivityCompat;

import com.pingidentity.emeasa.davinci.DaVinciValueMapper;
import com.pingidentity.emeasa.davinci.PingOneDaVinci;
import com.pingidentity.emeasa.davinci.PingOneDaVinciException;
import com.pingidentity.emeasa.davinci.api.Field;

public class PhoneNumberMapper implements DaVinciValueMapper {


    @Override
    public void performSubstitution(Field f, Context context, PingOneDaVinci pingOneDaVinci) throws PingOneDaVinciException {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PERMISSION_GRANTED) {
          throw new PingOneDaVinciException(MISSING_PERMISSION);
        }
        f.setValue(telephonyManager.getLine1Number());
        pingOneDaVinci.mapperComplete(f.getParameterName(),context);
    }
}

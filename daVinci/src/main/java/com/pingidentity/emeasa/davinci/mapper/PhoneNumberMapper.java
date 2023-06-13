package com.pingidentity.emeasa.davinci.mapper;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import static com.pingidentity.emeasa.davinci.PingOneDaVinciException.MISSING_PERMISSION;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.TelephonyManager;

import androidx.core.app.ActivityCompat;

import com.pingidentity.emeasa.davinci.DaVinciValueMapper;
import com.pingidentity.emeasa.davinci.PingOneDaVinci;
import com.pingidentity.emeasa.davinci.PingOneDaVinciException;
import com.pingidentity.emeasa.davinci.api.Field;

public class PhoneNumberMapper implements DaVinciValueMapper {

    private Field mField;
    private Context mContext;
    private PingOneDaVinci mPingOneDaVinci;


    @Override
    public void performSubstitution(Field f, Context context, PingOneDaVinci pingOneDaVinci) throws PingOneDaVinciException {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PERMISSION_GRANTED) {
            mField = f;
            mContext = context;
            mPingOneDaVinci = pingOneDaVinci;
            pingOneDaVinci.requestMapperPermissions(Manifest.permission.READ_PHONE_NUMBERS, this);
        } else {
            f.setValue(telephonyManager.getLine1Number());
            pingOneDaVinci.mapperComplete(f.getParameterName(), context);
        }
    }

    @Override
    public void retryMapping() {
        TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED ) {
            mField.setValue(telephonyManager.getLine1Number());
            mPingOneDaVinci.mapperComplete(mField.getParameterName(), mContext);
        } else {
            failMapping();
        }
    }

    @Override
    public void failMapping() {
        mField.setValue("");
        mPingOneDaVinci.mapperComplete(mField.getParameterName(), mContext);
    }
}

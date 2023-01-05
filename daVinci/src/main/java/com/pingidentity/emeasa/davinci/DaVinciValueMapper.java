package com.pingidentity.emeasa.davinci;

import android.content.Context;

import com.pingidentity.emeasa.davinci.api.Field;

public interface DaVinciValueMapper {
    void performSubstitution(Field f, Context context, PingOneDaVinci pingOneDaVinci) throws PingOneDaVinciException ;
}

package com.pingidentity.emeasa.davincisample;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.pingidentity.emeasa.davinci.PingOneDaVinciPushNotifications;

public class PushNotificationService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        PingOneDaVinciPushNotifications daVinciPush = new PingOneDaVinciPushNotifications(this, "Test", "Test", "11111", R.drawable.ic_launcher_foreground);
        if (daVinciPush.shouldHandleMessage(remoteMessage)) {
            daVinciPush.handleMessage(remoteMessage, this, PushFlowActivity.class);
        } else {
            super.onMessageReceived(remoteMessage);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        PingOneDaVinciPushNotifications.updateDeviceToken(this, token);
    }
}

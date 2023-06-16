package com.pingidentity.emeasa.davinci;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.pingidentity.pingidsdkv2.NotificationObject;
import com.pingidentity.pingidsdkv2.PingOne;
import com.pingidentity.pingidsdkv2.PingOneSDKError;
import com.pingidentity.pingidsdkv2.types.NotificationProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class PingOneDaVinciPushNotifications {

    private static final String PINGONE_KEY = "PingOne";
    private static final String PINGONE_NOTIFICATION_KEY = "PingOneNotification";
    private static final String APS_KEY = "aps";
    private static final String TITLE_KEY = "title";
    private static final String BODY_KEY = "body";

    private static final int PINGONE_DAVINCI_PUSH_NOTIFICATION_ID = 9031;

    private String mNotificationChannelName;
    private String mNotificationChannelDescription;
    private String mNotificationChannelID;
    private int mIconID;

    private Context mContext;


    public static void updateDeviceToken(Context context, String token) {
        PingOne.setDeviceToken(context, token, NotificationProvider.FCM, new PingOne.PingOneSDKCallback() {
            @Override
            public void onComplete(@Nullable PingOneSDKError pingOneSDKError) {
                //check for an error and re-schedule service update
            }
        });
    }

    public  PingOneDaVinciPushNotifications(Context context, String notificationChannelName, String notificationChannelDescription, String notificationChannelID, int iconID) {
        this.mNotificationChannelID = notificationChannelID;
        this.mNotificationChannelDescription = notificationChannelDescription;
        this.mNotificationChannelName = notificationChannelName;
        this.mIconID = iconID;
        this.mContext = context;
        createNotificationChannel();
    }

    public boolean shouldHandleMessage(RemoteMessage remoteMessage) {
        String value = remoteMessage.getData().getOrDefault(PINGONE_KEY, null);
        return (value != null);

    }

    public void handleMessage(RemoteMessage remoteMessage, FirebaseMessagingService service, Class activityClass) {
        PingOne.processRemoteNotification(service, remoteMessage, new PingOne.PingOneNotificationCallback() {
            @Override
            public void onComplete(@Nullable NotificationObject notificationObject, @Nullable PingOneSDKError pingOneSDKError) {
                Intent handlePushChallengeIntent = new Intent(service, activityClass);
                handlePushChallengeIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                handlePushChallengeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                handlePushChallengeIntent.putExtra(PINGONE_NOTIFICATION_KEY, notificationObject);
                if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                    service.startActivity(handlePushChallengeIntent);
                } else {
                    if(remoteMessage.getData().containsKey(APS_KEY)){
                        try {
                            JSONObject jsonObject = new JSONObject(Objects.requireNonNull(remoteMessage.getData().get("aps")));
                            handlePushChallengeIntent.putExtra(TITLE_KEY, ((JSONObject)jsonObject.get("alert")).get("title").toString());
                            handlePushChallengeIntent.putExtra(BODY_KEY, ((JSONObject)jsonObject.get("alert")).get("body").toString());
                        } catch (JSONException | NullPointerException e) {
                            e.printStackTrace();
                        }
                    } else {
                        handlePushChallengeIntent.putExtra(TITLE_KEY, "Approval Request");
                        handlePushChallengeIntent.putExtra(BODY_KEY, "You have a new request to review");
                    }
                    handlePushChallengeIntent.putExtra(PINGONE_NOTIFICATION_KEY, notificationObject);
                    buildAndShowActionsNotification(handlePushChallengeIntent, activityClass);

                }
            }
        });
    }


    private void createNotificationChannel() {
        /*
         * Create the NotificationChannel, but only on API 26+ because
         * the NotificationChannel class is new and not in the support library
         */
        CharSequence name = mNotificationChannelName;
        String description = mNotificationChannelDescription;
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(mNotificationChannelID, name, importance);
        channel.setDescription(description);
        NotificationManager notificationManager = mContext.getSystemService(NotificationManager.class);
        assert notificationManager != null;
        notificationManager.createNotificationChannel(channel);
    }

    private void buildAndShowActionsNotification(Intent notificationIntent, Class activityClass) {
        if (ActivityCompat.checkSelfPermission(mContext, "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, mNotificationChannelID);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);
        builder.setSmallIcon(mIconID);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        if (notificationIntent.hasExtra(TITLE_KEY)) {
            builder.setContentTitle(notificationIntent.getStringExtra(TITLE_KEY));
        }
        if (notificationIntent.hasExtra(BODY_KEY)) {
            builder.setContentText(notificationIntent.getStringExtra(BODY_KEY));
        }

        NotificationObject notificationObject = notificationIntent.getParcelableExtra(PINGONE_NOTIFICATION_KEY);
        Bundle extra = new Bundle();
        extra.putParcelable(PINGONE_NOTIFICATION_KEY, notificationObject);
        builder.setContentIntent(createOnTapPendingIntent(notificationIntent, activityClass));
        Notification newMessageNotification = builder.build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);

        notificationManager.notify(PINGONE_DAVINCI_PUSH_NOTIFICATION_ID, newMessageNotification);
    }

    private PendingIntent createOnTapPendingIntent(Intent notificationIntent, Class activityClass) {
        NotificationObject notificationObject = notificationIntent.getParcelableExtra("PingOneNotification");

        Intent intent = new Intent(mContext, activityClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        Bundle data = new Bundle();
        data.putParcelable(PINGONE_KEY, notificationObject);
        if (notificationIntent.hasExtra(TITLE_KEY)) {
            data.putString(TITLE_KEY, notificationIntent.getStringExtra(TITLE_KEY));
        }
        if (notificationIntent.hasExtra(BODY_KEY)) {
            data.putString(BODY_KEY, notificationIntent.getStringExtra(BODY_KEY));
        }
        intent.putExtras(data);
        return PendingIntent.getActivity(mContext, (int) (System.currentTimeMillis() & 0xfffffff), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

}

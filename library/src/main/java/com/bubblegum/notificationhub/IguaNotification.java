package com.bubblegum.notificationhub;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;

/**
 * Author: swechhaprakash
 * Created on: 6/21/16.
 */
public class IguaNotification {
    public StatusBarNotification sbn;
    public String actionTitle;
    public NotificationCompat.Action action;
    public RemoteInput[] remoteInputs;
    public String packageName;
    public String notificationTitle;
    public String notificationSnippet;
    public boolean isQuickReplySupported = false;

    /**
     * If quick reply is supported, send a quick reply with given reply text
     * @param context
     * @param replyText
     * @return Boolean to indicate whether or not the operation succeeded
     */
    public boolean quickReplyWithText(Context context, String replyText) {
        if (!isQuickReplySupported) {
            return false;
        }

        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        for (RemoteInput remoteIn : remoteInputs) {
            bundle.putCharSequence(remoteIn.getResultKey(), replyText);
        }
        RemoteInput.addResultsToIntent(remoteInputs, intent, bundle);
        try {
            action.actionIntent.send(context, 0, intent);
            return true;
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
            return false;
        }
    }
}

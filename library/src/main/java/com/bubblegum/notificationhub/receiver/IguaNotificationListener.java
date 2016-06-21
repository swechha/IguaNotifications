package com.bubblegum.notificationhub.receiver;

import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.bubblegum.notificationhub.IguaNotification;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Author: swechhaprakash
 * Created on: 6/21/16.
 */
public abstract class IguaNotificationListener extends NotificationListenerService {
    private static final String TAG = "bubble_notif";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!sbn.isOngoing()) {
            postDelayed(sbn);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
    }

    private ArrayList<PendingNotification> pendingNotifications = new ArrayList<>();
    private ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();

    /**
     * Cancel all duplicate notifications.
     * (Multiple notifications might be received by onNotificationPosted due to wear notifications)
     * @param sbn
     */
    private void postDelayed(final StatusBarNotification sbn) {
        PendingNotification pendingNotification = new PendingNotification(sbn);
        int index = pendingNotifications.indexOf(pendingNotification);
        if (index >= 0) {
            boolean shouldRemove = false;

            /* Ignore duplicate and grouped summary notifications */
            if (NotificationCompat.isGroupSummary(sbn.getNotification())) {
                return;
            } else if (NotificationCompat.isGroupSummary(pendingNotifications.get(index).getSbn().getNotification())) {
                shouldRemove = true;
            }

            if (shouldRemove) {
                pendingNotifications.get(index).getScheduledFuture().cancel(false);
                pendingNotifications.remove(index);
            }
        }

        /** Do something with the intercepted notification here */
        Runnable task = new Runnable() {
            @Override
            public void run() {
                if (pendingNotifications.size() >= 0) {
                    PendingNotification notification = pendingNotifications.get(0);
                    pendingNotifications.remove(0);

                    IguaNotification extractedNotif = extractNotification(sbn);
                    if (extractedNotif != null) {
                        //Notify about the notification
                        onIguaNotificationReceived(extractedNotif);
                    }
                }
            }
        };

        ScheduledFuture<?> scheduledFuture = worker.schedule(task, 200, TimeUnit.MILLISECONDS);
        pendingNotification.setScheduledFuture(scheduledFuture);
        pendingNotifications.add(pendingNotification);
    }

    @Nullable
    private IguaNotification extractNotification(StatusBarNotification sbn) {
        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender(sbn.getNotification());
        for (NotificationCompat.Action action : wearableExtender.getActions()) {

            IguaNotification extractedNotification = new IguaNotification();
            Bundle extras = sbn.getNotification().extras;
            String title = extras.getString("android.title");
            extractedNotification.packageName = sbn.getPackageName();
            extractedNotification.notificationSnippet = sbn.getNotification().tickerText.toString();
            extractedNotification.notificationTitle = title;
            extractedNotification.actionTitle = action.title.toString();
            extractedNotification.action = action;
            extractedNotification.sbn = sbn;

            if (action.getRemoteInputs().length > 0) {
                extractedNotification.remoteInputs = action.getRemoteInputs();
                if (action.title.toString().toLowerCase().contains("reply")) {
                    extractedNotification.isQuickReplySupported = true;
                }
                return extractedNotification;
            }
        }
        return null;
    }

    public abstract void onIguaNotificationReceived(IguaNotification notification);

    private static class PendingNotification {
        public PendingNotification(StatusBarNotification sbn) {
            setSbn(sbn);
        }

        private StatusBarNotification sbn;
        private ScheduledFuture<?> scheduledFuture;

        public StatusBarNotification getSbn() {
            return sbn;
        }

        public void setSbn(StatusBarNotification sbn) {
            this.sbn = sbn;
        }

        public ScheduledFuture<?> getScheduledFuture() {
            return scheduledFuture;
        }

        public void setScheduledFuture(ScheduledFuture<?> scheduledFuture) {
            this.scheduledFuture = scheduledFuture;
        }
    }
}

package lk.sa.foodmart.worker;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.concurrent.ExecutionException;

import lk.sa.foodmart.R;

public class SellerOrderReminderWorker extends Worker {

    private static final String CHANNEL_ID = "seller_order_reminder_channel";
    private static final String CHANNEL_NAME = "Seller Order Reminder";
    private static final String PREF_NAME = "seller_worker_prefs";
    private static final String KEY_LAST_CHECK = "last_order_check_time";

    public SellerOrderReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            String currentUserId = getInputData().getString("sellerId");

            if (currentUserId == null || currentUserId.isEmpty()) {
                return Result.success();
            }

            SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            long lastCheckTime = prefs.getLong(KEY_LAST_CHECK, 0);


            long filterTime = lastCheckTime > 0 ? lastCheckTime : (System.currentTimeMillis() - 86400000);

            QuerySnapshot ordersSnapShot = Tasks.await(
                    firestore.collection("orders")
                            .whereGreaterThan("createdAt", filterTime)
                            .orderBy("createdAt", Query.Direction.DESCENDING)
                            .get()
            );

            boolean foundNewOrder = false;
            long newestOrderTime = lastCheckTime;

            for (QueryDocumentSnapshot orderDoc : ordersSnapShot) {
                Long createdAt = orderDoc.getLong("createdAt");
                if (createdAt != null && createdAt > newestOrderTime) {
                    newestOrderTime = createdAt;
                }


                QuerySnapshot itemSnapShot = Tasks.await(
                        firestore.collection("orders")
                                .document(orderDoc.getId())
                                .collection("items")
                                .whereEqualTo("sellerId", currentUserId)
                                .get()
                );

                if (!itemSnapShot.isEmpty()) {
                    foundNewOrder = true;

                }
            }

            if (foundNewOrder) {
                showNotification("New Order Received!", "You have new food orders to prepare.");
            }


            prefs.edit().putLong(KEY_LAST_CHECK, newestOrderTime).apply();

            return Result.success();

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return Result.retry();
        }
    }

    private void showNotification(String title, String message) {
        createNotificationChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_notifications_24)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        notificationManagerCompat.notify(3001, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for seller order reminders");
            NotificationManager manager = getApplicationContext().getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}

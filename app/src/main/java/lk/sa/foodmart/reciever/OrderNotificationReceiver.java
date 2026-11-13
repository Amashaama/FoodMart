package lk.sa.foodmart.reciever;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import lk.sa.foodmart.activity.MainActivity;

public class OrderNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

       Intent openintent = new Intent(context, MainActivity.class);
       openintent.putExtra("open_fragment","my_orders");
       openintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
       context.startActivity(openintent);

    }
}

package com.example.walker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyBroadcastReceiver extends BroadcastReceiver {
    private TimeReached timeReached;
    public MyBroadcastReceiver(TimeReached timeReached){
        this.timeReached=timeReached;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(BroadcastValue.IDLENOTIFICATION)) {
            String idleTime = intent.getStringExtra(BroadcastValue.IDLETIME);
//            Log.e("Walker"," we get "+idleTime);
            timeReached.onIdleLimitReached(idleTime);
        }else if (intent.getAction().equals(BroadcastValue.ENDOFDAYNOTIFICATION)) {
            timeReached.onEndOfDay();
        }
    }
    public interface TimeReached{
        void onIdleLimitReached(String idleTime);
        void onEndOfDay();
    }
}

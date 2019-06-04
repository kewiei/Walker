package com.example.walker;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    TextView textView_isStepCounterAvailable;
    TextView textView_stepDetectorAvailability;
    TextView textView_stepsNumber;
    Button button_resetSteps;
    TextView textView_todayStepsNumber;
    Spinner spinner_dayDuration;
    Spinner spinner_milestone;
    Spinner spinner_IdleNotificationInterval;
    TextView textView_log;
    RecyclerView recyclerView_localWalker;

    static Handler handler ;

    String currentMilestone="1000 feet";
    String currentIdleTime="1h";
    String currentDayDuration="1d";

    /**
     * I would use Alarm to make Idle notification and refreshment on daily step counter.
     */
    AlarmManager myAlarmManager;
    MyBroadcastReceiver myBroadcastReceiver;
    //Notification
    NotificationManager myNotificationManager;
    NotificationChannel mChannel;

    /**
     * I assume that our user would use device with Android version upper than 4.4, which means we have step Counter
     * When the phone reboot, the counter would be killed and the number in it become 0
     * So we would send the data to server or try to listen to power-off broadcast to counter that problem
     * (I don't have a server but )
     */
    SensorManager sensorManager;
    Sensor stepCounter;
    Sensor stepDetector;
    SimpleDateFormat simpleDateFormat;
    SensorEventListener stepCounterListener;
    SensorEventListener stepDetectorListener;

    String lastUpdateTime;
    //This value is used in distance update
    float old_distance;
    float new_distance;
    float today_old_distance=0;
    float today_new_distance=0;

    boolean isMilestoneOfTodayPosted=false;

    /**
     * Android SDK does not offer direct method for measuring distance by motion sensors
     * But I assume that 1 step would cover 1 foot, so step2feet=1.0
     * In the future, we may develop a learning system with the help of Map API to find the actual value of each users'step & feet ratio
     * Then the measurement of the distance would be more accurate
     */
    float step2feet=1;

    /**
     * bias is prepared for reboot and reset. When the device reboot, the number in the step counter is 0.
     * In this case, the bias would be history steps that must be add on the final result and it would be positive
     * When users reset their history, the bias would be minus to keep the result 0.
     */
    float bias=0;
    float today_bias=0;

    /**
     * I would use SharedPreferences as tool for saving.This is not a safe way to save something.
     * If allowed,we would have backup of settings and account information on server in the future
     */
    SharedPreferences sp;

    WalkerListAdapter walkerListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = this.getSharedPreferences("data", MODE_PRIVATE);
        handler = new Handler();
        setContentView(R.layout.activity_main);
        initViews();
        initSensor();
        initListener();
        registerSensor();
        initNotification();
        initReceiver();
        initAlarm();
    }

    @Override
    protected void onPause() {
        /**
         * There will be two scenarios when app call this function:
         * 1. the phone has been through a reboot. And the record need to be undated
         * 2. the app has just been closed but now reopened. The distance value in step counter need
         * to be updated into app UI.
         *
         * The lines below would update the recorded value anyway. Then when the listen on step
         * counter is called, the value would be compared and updated.
         */
        super.onPause();
        //Put new record into SharedPreferences
        SharedPreferences.Editor editor = sp.edit();
        editor.putFloat("total_distance", Float.parseFloat(textView_stepsNumber.getText().toString()));
        editor.putFloat("today_distance", Float.parseFloat(textView_todayStepsNumber.getText().toString()));
        editor.putFloat("today_bias", today_bias);
        editor.putFloat("bias", bias);
        editor.apply();
        Log.e("Walker","------->data saved");
    }

    @Override
    protected void onResume() {
        super.onResume();
        super.onStart();
        old_distance = sp.getFloat("total_distance", 0);
        today_old_distance =  sp.getFloat("today_distance", 0);
        today_bias = sp.getFloat("today_bias",0);
        bias = sp.getFloat("bias",0);
        String distance = getResources().getString(R.string.distance);
        textView_stepsNumber.setText(String.format(distance, old_distance));
        textView_todayStepsNumber.setText(String.format(distance, today_old_distance));
        Log.e("Walker","------->data retrieved");
    }

    private void initViews(){
        //Get Views
        textView_isStepCounterAvailable= findViewById(R.id.textView_stepCounterAvailability);
        textView_stepDetectorAvailability= findViewById(R.id.textView_stepDetectorAvailability);
        textView_stepsNumber= findViewById(R.id.textView_stepsNumber);
        button_resetSteps= findViewById(R.id.button_resetSteps);
        textView_todayStepsNumber= findViewById(R.id.textView_todayStepsNumber);
        spinner_dayDuration= findViewById(R.id.spinner_dayDuration);
        spinner_milestone= findViewById(R.id.spinner_milestone);
        spinner_IdleNotificationInterval= findViewById(R.id.spinner_IdleNotificationInterval);
        textView_log= findViewById(R.id.textView_log);
        recyclerView_localWalker= findViewById((R.id.recyclerView_localWalker));

        button_resetSteps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        resetDistance();
                    }
                });
            }
        });

        spinner_dayDuration.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentDayDuration = spinner_dayDuration.getSelectedItem().toString();
                Log.e("Walker","Day duration changed to "+currentDayDuration);
                refreshEndOfDayNotificationAlarm(MainActivity.this,currentDayDuration);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        spinner_IdleNotificationInterval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentIdleTime = spinner_IdleNotificationInterval.getSelectedItem().toString();
                Log.e("Walker","Idle time changed to "+currentIdleTime);
                refreshIdleNotificationAlarm(MainActivity.this,currentIdleTime);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        spinner_milestone.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentMilestone = spinner_milestone.getSelectedItem().toString();
                Log.e("Walker","Milestone changed to "+currentMilestone);
                isMilestoneOfTodayPosted=false;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        /**
         * The button 'reset distance' to call the function resetDistance().
         * The detailed information would be shown at said function place
         */
        //Retrieve spinner items from sources
        String[] DayDurationItems = getResources().getStringArray(R.array.DayDuration);
        String[] MilestoneItems = getResources().getStringArray(R.array.Milestone);
        String[] IdleNotificationIntervalItems = getResources().getStringArray(R.array.IdleNotificationInterval);

        //Build Adapters for spinners
        ArrayAdapter<String> dayDurationSpinnerAdapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, DayDurationItems);
        ArrayAdapter<String> milestoneSpinnerAdapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, MilestoneItems);
        ArrayAdapter<String> idleNotificationIntervalSpinnerAdapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, IdleNotificationIntervalItems);

        //Apply Adapter
        spinner_dayDuration.setAdapter(dayDurationSpinnerAdapter);
        spinner_milestone.setAdapter(milestoneSpinnerAdapter);
        spinner_IdleNotificationInterval.setAdapter(idleNotificationIntervalSpinnerAdapter);

        //Allow testView to scroll vertically
        textView_log.setMovementMethod(ScrollingMovementMethod.getInstance());

        //init and set Adapter for recyclerView
        walkerListAdapter = new WalkerListAdapter(this);
        recyclerView_localWalker.setLayoutManager(new LinearLayoutManager(this));
        recyclerView_localWalker.setAdapter(walkerListAdapter);
        /**
         * this is just an example of retrieve date for recyclerView
         */
        walkerListAdapter.refreshdata();
    }

    private void initSensor() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        String step_counter_availability = getResources().getString(R.string.step_counter_availability);
        String step_detector_availability = getResources().getString(R.string.step_detector_availability);

        step_counter_availability = String.format(step_counter_availability, getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER));
        step_detector_availability = String.format(step_detector_availability, getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_DETECTOR));

        textView_isStepCounterAvailable.setText(step_counter_availability);
        textView_stepDetectorAvailability.setText(step_detector_availability);

        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
    }

    protected void initListener() {
        stepCounterListener=new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                String distance = getResources().getString(R.string.distance);
                new_distance = event.values[0]*step2feet;
                /**
                 *if the distance retrieved from the TextView is larger,
                 * the phone must had been through a reboot.
                 * So the bias is recorded
                 */
                if (old_distance>new_distance){
                    bias=old_distance-new_distance;
                }

                today_new_distance =new_distance;
                new_distance = new_distance+bias;
                old_distance = new_distance;
                /**
                 * Milestone is calculated according to today's distance.
                 * So if 'today' pass, the milestone counter would be refreshed.
                 * I assume there will be not repeated milestones. They (5 feet, 10 feet, 1000 feet) would sequentially appear in one 'day'.
                 * (But not in this design now, we can select a choice in spinner and only one milestone would appear once in a day.)
                 * currently, milestones are set to be 5 feet, 10 feet, and 1000 feet
                 */
                if (today_old_distance>today_new_distance){
                    today_bias=today_old_distance-today_new_distance;
                }
                today_new_distance = today_new_distance+today_bias;
                today_old_distance = today_new_distance;
                if (!isMilestoneOfTodayPosted && today_new_distance > BroadcastValue.map.get(currentMilestone)){
                    sentMilestoneNotification(currentMilestone);
                    isMilestoneOfTodayPosted=true;
                }

                textView_stepsNumber.setText(String.format(distance,new_distance));
                textView_todayStepsNumber.setText(String.format(distance,today_new_distance));
                lastUpdateTime = simpleDateFormat.format(event.timestamp/1000000);
                Log.e("Walker","distance altered  "+new_distance);
                refreshIdleNotificationAlarm(MainActivity.this,currentIdleTime);
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        stepDetectorListener=new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {

            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy){

            }
        };
    }

    private void registerSensor(){
        //注册传感器事件监听器
        if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER)&&
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_DETECTOR)){
            sensorManager.registerListener(stepDetectorListener,stepDetector,SensorManager.SENSOR_DELAY_FASTEST);
            sensorManager.registerListener(stepCounterListener,stepCounter,SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    private void unregisterSensor(){
        //解注册传感器事件监听器
        if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER)&&
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_DETECTOR)){
            sensorManager.unregisterListener(stepCounterListener);
            sensorManager.unregisterListener(stepDetectorListener);
        }
    }

    /**
     * 'resetDistance' function would reset the distance that were stored in the SharedPreferences
     * Also, the bias is recorded to make sure we have correct distance
     */
    private void resetDistance(){
        SharedPreferences.Editor editor = sp.edit();
        editor.putFloat("total_distance", 0);
        editor.apply();
        bias += -old_distance;
        Log.e("Walker","bias  "+bias);
        textView_stepsNumber.setText("0");
        resetTodayDistance();
    }
    /**
     * This method would always be called by non UI thread, but I expect to use handler elsewhere.
     */
    private void resetTodayDistance(){
        SharedPreferences.Editor editor = sp.edit();
        editor.putFloat("today_distance", 0);
        editor.apply();
        today_bias += -today_old_distance;
        today_old_distance=0;
        Log.e("Walker","today_bias  "+today_bias);
        textView_todayStepsNumber.setText("0");
    }

    /**
     * initAlarm would retrieve the AlarmManager and would setup the broadcast to change the
     */
    private void initAlarm(){
        myAlarmManager = (AlarmManager)getSystemService(MainActivity.ALARM_SERVICE);
    }
    private void refreshIdleNotificationAlarm(Context context,String idleTime){
        Intent myIntent = new Intent();
//        Log.e("Walker","we put "+idleTime);
        myIntent.putExtra(BroadcastValue.IDLETIME,idleTime);
        myIntent.setAction(BroadcastValue.IDLENOTIFICATION);
        PendingIntent sender = PendingIntent.getBroadcast(context, BroadcastValue.IDLENOTIFICATION_ALARMID, myIntent,0);
        long triggerTime = System.currentTimeMillis() + BroadcastValue.map.get(idleTime);
        myAlarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, sender);
        Log.e("Walker","Idle Notification Alarm refreshed");
    }
    private void refreshEndOfDayNotificationAlarm(Context context,String dayDuration){
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long timestampat0 = cal.getTimeInMillis()/1000;
        int timeInterval = BroadcastValue.map.get(dayDuration);
        long nowTime = System.currentTimeMillis();
        long triggerTime = nowTime-(nowTime-timestampat0)%timeInterval+timeInterval;

        Intent myIntent = new Intent();
        myIntent.setAction(BroadcastValue.ENDOFDAYNOTIFICATION);
        PendingIntent sender = PendingIntent.getBroadcast(context, BroadcastValue.ENDOFDAYNOTIFICATION_ALARMID, myIntent,0);
        myAlarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, sender);
        Log.e("Walker","EndOfDay Alarm refreshed");
    }


    /**
     * 1. After the idle time limit has been reached, the app would instantly post another alarm,
     * which means that users would be alarm again after another time limit has been reached.
     * 2. The end of day alarm would be posted again after received.
     */
    private void initReceiver(){
        myBroadcastReceiver = new MyBroadcastReceiver(new MyBroadcastReceiver.TimeReached() {
            @Override
            public void onIdleLimitReached(String idleTime) {
                sentIdleNotification(idleTime);
                refreshIdleNotificationAlarm(MainActivity.this,currentIdleTime);
            }
            @Override
            public void onEndOfDay() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        textView_log.append("\nToday you have accomplished: "+today_old_distance);
                        int offset=textView_log.getLineCount()*textView_log.getLineHeight();
                        if(offset>textView_log.getHeight()){
                            textView_log.scrollTo(0,offset-textView_log.getHeight());
                        }
                        resetTodayDistance();
                        refreshEndOfDayNotificationAlarm(MainActivity.this,currentDayDuration);
                        isMilestoneOfTodayPosted=false;
                    }
                });
            }
        });
        IntentFilter filter = new IntentFilter();
        filter.addAction(BroadcastValue.IDLENOTIFICATION);
        filter.addAction(BroadcastValue.ENDOFDAYNOTIFICATION);
        registerReceiver(myBroadcastReceiver,filter);
    }

    private void initNotification(){
        myNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mChannel = new NotificationChannel("01", "name", NotificationManager.IMPORTANCE_LOW);
        myNotificationManager.createNotificationChannel(mChannel);
    }
    private void sentMilestoneNotification(String milestone){
        Notification milestoneNotification =new Notification.Builder(this,"default")
                .setSmallIcon(R.mipmap.walker)
                .setContentTitle("Milestone Achieved")
                .setContentText("You have walked "+milestone)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true)
                .setChannelId("01")
                .build();
        myNotificationManager.notify(1, milestoneNotification );
        Log.e("Walker","Milestone notification send");
    }
    private void sentIdleNotification(String idleTime) {
        NotificationChannel mChannel = new NotificationChannel("0.1", "name", NotificationManager.IMPORTANCE_LOW);
        myNotificationManager.createNotificationChannel(mChannel);
        Notification IdleNotification = new Notification.Builder(this, "default")
                .setSmallIcon(R.mipmap.walker)
                .setContentTitle("Idle Warning")
                .setContentText("You have stayed still for " + currentIdleTime + ". It is time for some walk.")
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true)
                .setChannelId("01")
                .build();
        myNotificationManager.notify(2, IdleNotification);
        Log.e("Walker","Idle notification send");
    }

    /**
     * how to keep the alarm alive when the app is killed is not solved. What I can do here is to keep the
     * MainActivity alive.
     * @param keyCode
     * @param event
     * @return
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            home.addCategory(Intent.CATEGORY_HOME);
            startActivity(home);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}

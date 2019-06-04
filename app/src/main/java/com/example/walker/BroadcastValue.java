package com.example.walker;

import java.util.HashMap;
import java.util.Map;

public class BroadcastValue {
    public static final String IDLENOTIFICATION ="com.example.walker.idleNotification";
    public static final String ENDOFDAYNOTIFICATION ="com.example.walker.endOfDayNotification";

    public static final int IDLENOTIFICATION_ALARMID = 0;
    public static final int ENDOFDAYNOTIFICATION_ALARMID = 1;

    public static final String IDLETIME = "IDLETIME";

    public static Map<String, Integer> map = getMap();

    private static Map<String, Integer> getMap(){
        Map<String, Integer> map = new HashMap<>();
        map.put("5s", 5*1000);
        map.put("10s",10*1000);
        map.put("1h",3600*1000);
        map.put("1d",24*3600*1000);
        map.put("5 feet",5);
        map.put("10 feet",10);
        map.put("1000 feet",1000);
        return map;
    }
}

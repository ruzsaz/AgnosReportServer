package hu.agnos.report.server.service.cache;


import java.util.Timer;
import java.util.TimerTask;

public class CacheInvalidator {

    public void init() {

        Timer timer = new Timer();
        TimerTask tasknew = new TimerTask() {
            @Override
            public void run() {

            }
        };

        timer.schedule(tasknew, 0, 1000*60);
    }


}

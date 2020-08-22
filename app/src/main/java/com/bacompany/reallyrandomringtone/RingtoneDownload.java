package com.bacompany.reallyrandomringtone;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

public class RingtoneDownload extends Service {

    MyDownloadTask myDownloadTask;


    public RingtoneDownload() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        myDownloadTask = new MyDownloadTask(getApplicationContext());
        myDownloadTask.execute();

        if ((flags & START_FLAG_RETRY) == 0) {
            // TODO Если это повторный запуск, выполнить какие-то действия.
        }
        else {
            // TODO Альтернативные действия в фоновом режиме.
        }
        return Service.START_STICKY;
    }
    public void onDestroy() {
        super.onDestroy();
        myDownloadTask.cancel(true);
    }
}

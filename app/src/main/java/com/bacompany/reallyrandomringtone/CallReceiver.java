package com.bacompany.reallyrandomringtone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Random;

/**
 * Created by andre on 01.05.2017.
 */


public class CallReceiver extends BroadcastReceiver {
    private String intpath;
    private String extpath;
    private String nextringtone="";

    static MediaPlayer myMediaPlayer;

    private static boolean incomingCall = false;
    private static int old_ringer_mode;

    public CallReceiver() {
        Map<String, File> externalLocations = ExternalStorage.getAllStorageLocations();
        intpath = Environment.getExternalStorageDirectory().getAbsolutePath();
        extpath = "";
        if (externalLocations.get(ExternalStorage.EXTERNAL_SD_CARD)!=null)
            extpath = externalLocations.get(ExternalStorage.EXTERNAL_SD_CARD).getAbsolutePath();
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) ||
            intent.getAction().equals("android.intent.action.QUICKBOOT_POWERON") ||
            intent.getAction().equals("com.htc.intent.action.QUICKBOOT_POWERON")) {
            context.startService(new Intent(context, RingtoneDownload.class));
        }

        if (intent.getAction().equals("android.intent.action.PHONE_STATE")) {
            String phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (phoneState.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                //Трубка не поднята, телефон звонит
                String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                incomingCall = true;

/*                AudioManager aM = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                old_ringer_mode = aM.getRingerMode();
                aM.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                nextringtone = GetNextRingtone();

                if (!nextringtone.isEmpty()) {
                    myMediaPlayer = MediaPlayer.create(context, Uri.parse(nextringtone));
                    myMediaPlayer.setLooping(true);
                    myMediaPlayer.start();
                }
*/
            } else if (phoneState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
/*                if (myMediaPlayer!=null) {
                    myMediaPlayer.stop();
                    myMediaPlayer = null;
                }
*/
                //Телефон находится в режиме звонка (набор номера при исходящем звонке / разговор)
                if (incomingCall) {
                    incomingCall = false;
                }
            } else if (phoneState.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
//                if (myMediaPlayer!=null) {
//                    myMediaPlayer.stop();
//                    myMediaPlayer = null;
                ChangeRingtone(context);
//                }
                //Телефон находится в ждущем режиме - это событие наступает по окончанию разговора
                //или в ситуации "отказался поднимать трубку и сбросил звонок".
//                AudioManager aM = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
//                aM.setRingerMode(old_ringer_mode);

                if (incomingCall) {
                    incomingCall = false;
                }
            }
        }
    }

    public void ChangeRingtone(Context context){
        try {
            String nextfile = GetNextRingtone();
            if (nextfile!=null && !nextfile.isEmpty()) {
                String c = GetCurrentRingtone();
                File fc = new File(c);
                String nfile = GetRingtoneStorePath() + "/played/" + c.substring(c.lastIndexOf("/") + 1);
                File fnf = new File(nfile);
                fc.renameTo(fnf);
            }

            String tofile = GetRingtoneStorePath()+"/"+nextfile.substring(nextfile.lastIndexOf("/")+1);
            String curfile = GetRingtoneStorePath() + "/default.mp3";
            File f_nextfile = new File(nextfile);
            File f_tofile = new File(tofile);
            File f_curfile = new File(curfile);

            MyDownloadTask.CopyFile(f_nextfile,f_tofile);
            f_nextfile.renameTo(f_curfile);


            File f = new File(GetRingtoneStorePath()+"/download/");
            File[] fList = f.listFiles(new FilenameFilter(){
                public boolean accept(File dir, String name)
                {
                    return name.endsWith(".mp3");
                }
            });

            File f2 = new File(GetRingtoneStorePath()+"/played/");
            File[] fList2 = f2.listFiles(new FilenameFilter(){
                public boolean accept(File dir, String name)
                {
                    return name.endsWith(".mp3");
                }
            });
            if (fList2 != null && fList2.length > 1) {
                Arrays.sort(fList2, new Comparator<File>() {
                    @Override
                    public int compare(File object1, File object2) {
                        return (int) ((object1.lastModified() > object2.lastModified()) ? object1.lastModified(): object2.lastModified());
                    }
                });
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            int max = Integer.parseInt(prefs.getString("DownloadMaxFiles","5"));
            int mincache = Integer.parseInt(prefs.getString("MinCachedFiles","1"));

            // Если количество файлов из кеша меньше доступных но нескачанных файлов плюс минимум в кеше
            // Чтобы кеш наполняся, если скачивание не получается
            if (fList2 == null || fList2.length <= max-fList.length+1+mincache) return;
            int rc = fList2.length - (max-fList.length+1+mincache);
            for (int i=fList2.length-rc;i<fList2.length;i++)
                fList2[i].delete();


        }
        catch(Exception e){  }
    }

    public String GetCurrentRingtone(){
        String path = GetRingtoneStorePath()+"/";

        File f = new File(path);
        File[] fList = f.listFiles(new FilenameFilter(){
            public boolean accept(File dir, String name)
            {
                return name.endsWith(".mp3");
            }
        });
        if (fList.length<2) return "";
        if (fList[0].getName().equals("default.mp3")) return fList[1].getAbsolutePath();
        return fList[0].getAbsolutePath();
    }

    public String GetNextRingtone(){
        String path = GetRingtoneStorePath()+"/download/";

        File f = new File(path);
        File[] fList = f.listFiles(new FilenameFilter(){
            public boolean accept(File dir, String name)
            {
                return name.endsWith(".mp3");
            }
        });
        File[] sortedByDate = f.listFiles();

        if (sortedByDate != null && sortedByDate.length > 1) {
            Arrays.sort(sortedByDate, new Comparator<File>() {
                @Override
                public int compare(File object1, File object2) {
                    return (int) ((object1.lastModified() > object2.lastModified()) ? object1.lastModified(): object2.lastModified());
                }
            });
        }

        if (sortedByDate != null && sortedByDate.length > 0)
            return sortedByDate[sortedByDate.length-1].getAbsolutePath();

        path = GetRingtoneStorePath()+"/played/";

        f = new File(path);

        fList = f.listFiles(new FilenameFilter(){
            public boolean accept(File dir, String name)
            {
                return name.endsWith(".mp3");
            }
        });

        if (fList==null || fList.length==0) return "";

        Random r = new Random();
        int v = r.nextInt(fList.length);
        return fList[v].getAbsolutePath();
    }
    public String GetRingtoneStorePath(){
        String p = intpath;
        if (true && isSdCardeAvailable() && !extpath.isEmpty())
            p = extpath;
        p += "/RRR";
        File f = new File(p+"/download");
        if (!f.exists()) f.mkdirs();
        f = new File(p+"/played");
        if (!f.exists()) f.mkdirs();
        f = new File(p+"/favorites");
        if (!f.exists()) f.mkdirs();
        return p;
    }
    public static boolean isSdCardeAvailable(){
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state))
            return true;
        return false;
    }
}

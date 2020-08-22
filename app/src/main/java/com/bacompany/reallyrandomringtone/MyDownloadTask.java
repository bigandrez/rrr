package com.bacompany.reallyrandomringtone;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by andre on 01.05.2017.
 */
public class MyDownloadTask extends AsyncTask<Void,Void,Void>
{
    private LocalBroadcastManager bm;
    private String intpath;
    private String extpath;
    private Context mContext;

    public void sendResult(String message) {
        Intent intent = new Intent("rrr.manager");
        if(message != null)
            intent.putExtra("rrr.manager", message);
        bm.sendBroadcast(intent);
    }

    public MyDownloadTask(Context context){
        mContext = context;
        bm = LocalBroadcastManager.getInstance(mContext);
    }

    protected void onPreExecute() {
        //display progress dialog.

    }



    protected long GetLastPlayedFileTime(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        return prefs.getLong("LastTime",0);
    }

    protected void SetLastPlayedFileTime(long time){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        prefs.edit().putLong("LastPlayedFileTime",time).commit();
    }

    protected int GetDownloadMaxFiles(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        return Integer.parseInt(prefs.getString("DownloadMaxFiles","30"));
    }

    protected int GetDownloadLimit(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        return prefs.getInt("DownloadLimit",30);
    }

    protected int DecDownloadLimit(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        int i = prefs.getInt("DownloadLimit",30);
        i--;if(i<0)i=0;
        prefs.edit().putInt("DownloadLimit",i).commit();
        return i;
    }


    protected boolean IsWifiOnly(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        return prefs.getBoolean("IsWifiOnly",true);
    }

    // Если интернет доступен
    private boolean isNetworkAvailable(@NonNull Context context) {
        return isWifiConnected(context) || isMobileConnected(context);
    }

    public static boolean isWifiConnected(@NonNull Context context) {
        return isConnected(context, ConnectivityManager.TYPE_WIFI);
    }

    public static boolean isMobileConnected(@NonNull Context context) {
        return isConnected(context, ConnectivityManager.TYPE_MOBILE);
    }

    private static boolean isConnected(@NonNull Context context, int type) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            NetworkInfo networkInfo = connMgr.getNetworkInfo(type);
            return networkInfo != null && networkInfo.isConnected();
        } else {
            return isConnected(connMgr, type);
        }
    }
    public static boolean isConnected(@NonNull Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static boolean isConnected(@NonNull ConnectivityManager connMgr, int type) {
        Network[] networks = connMgr.getAllNetworks();
        NetworkInfo networkInfo;
        for (Network mNetwork : networks) {
            networkInfo = connMgr.getNetworkInfo(mNetwork);
            if (networkInfo != null && networkInfo.getType() == type && networkInfo.isConnected()) {
                return true;
            }
        }
        return false;
    }

    protected int GetMp3Count(){
        String path = GetRingtoneStorePath()+"/download/";

        File dir = new File(path);

        String[] names = dir.list(
                new FilenameFilter()
                {
                    public boolean accept(File dir, String name)
                    {
                        return name.endsWith(".mp3");
                    }
                });
        if (names==null) return 0;
        return names.length;
    }


    protected Void doInBackground(Void... params) {

        Map<String, File> externalLocations = ExternalStorage.getAllStorageLocations();
        intpath = Environment.getExternalStorageDirectory().getAbsolutePath();
        extpath="";
        if (externalLocations.get(ExternalStorage.EXTERNAL_SD_CARD)!=null)
            extpath = externalLocations.get(ExternalStorage.EXTERNAL_SD_CARD).getAbsolutePath();

        //GetNextRingtone();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        Random r = new Random();

        while (!isCancelled()) {
            try {
                Thread.sleep(5000);
                if (!MainActivity.isPro() && GetDownloadLimit()<=0) continue;
                if (IsWifiOnly() && !isWifiConnected(mContext)) continue;
                int max = GetDownloadMaxFiles();
                int count = GetMp3Count();
                while (count < max) {

                    ArrayList<SiteParser> sites = new ArrayList<>();
                    if (prefs.getBoolean("use_myrington",true))
                        sites.add(new SiteMyrington());
                    if (prefs.getBoolean("use_ringon",true))
                        sites.add(new SiteRingon());
                    if (sites.size()==0) break;
                    int i = r.nextInt(sites.size());


                    String[] s = sites.get(i).GetRandomRingtone(mContext);
//                    String[] s = GetFromRingonRu();
                    if (s==null || s.length<2 || s[0].isEmpty()) break;
                    if (s[1].isEmpty()) {
                        s[1] = "file" + r.nextInt(1000000);
                    }
                    String fn = DownloadMp3(s[0], s[1]);
                    if (fn!=null && !fn.isEmpty()) {
                        count++;

                        int sd = prefs.getInt("SummaryDownload",0);
                        prefs.edit().putInt("SummaryDownload",sd+1).commit();

                        sendResult("bmm");
                        CheckFirstRun(fn);
                        RemoveUnusedRingtones();
                    }
                }
            } catch (InterruptedException e) {}
        }

         return null;
    }

    public static  void CopyFile(File src, File dst) throws IOException {
        FileInputStream inStream = new FileInputStream(src);
        FileOutputStream outStream = new FileOutputStream(dst);
        FileChannel inChannel = inStream.getChannel();
        FileChannel outChannel = outStream.getChannel();
        inChannel.transferTo(0, inChannel.size(), outChannel);
        inStream.close();
        outStream.close();
    }

    public void CheckFirstRun(String mp3file){
        try {
            String curfile = GetRingtoneStorePath() + "/default.mp3";
            File f_curfile = new File(curfile);
            if (f_curfile.exists()) return;

            File f_mp3File = new File(mp3file);

            String nfile = GetRingtoneStorePath() + "/"+mp3file.substring(mp3file.lastIndexOf("/")+1);
            File f_nfile = new File(nfile);
            CopyFile(f_mp3File,f_nfile);
            f_nfile=null;

            boolean success = f_mp3File.renameTo(f_curfile);
            f_curfile=null;
            f_mp3File=null;
            f_curfile = new File(curfile);

            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DATA, f_curfile.getAbsolutePath());
                values.put(MediaStore.MediaColumns.TITLE, "RRR default ringtone");
                values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp3");
//                values.put(MediaStore.MediaColumns.SIZE, f_curfile.length());
                values.put(MediaStore.Audio.Media.ARTIST, R.string.app_name);
                values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
                values.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
                values.put(MediaStore.Audio.Media.IS_ALARM, false);
                values.put(MediaStore.Audio.Media.IS_MUSIC, false);

                Uri uri = MediaStore.Audio.Media.getContentUriForPath(f_curfile.getAbsolutePath());

                mContext.getContentResolver().delete(
                        uri,
                        MediaStore.MediaColumns.DATA + "=\""
                                + f_curfile.getAbsolutePath() + "\"", null);

                Uri newUri = mContext.getContentResolver().insert(uri, values);

                RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_RINGTONE, newUri);
//                ret = newUri.toString();
            } catch (Throwable t) {

            }


        }
        catch(Exception e){  }
    }

    public String DownloadMp3(String url, String filename) {
        filename = GetRingtoneStorePath()+"/download/"+filename;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        try {
            String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:11.0) " + "Gecko/20100101 Firefox/11.0";
//            text = URLEncoder.encode(text, "utf-8");


            URL u = new URL(url);

            // Etablish connection
            HttpURLConnection connection = (HttpURLConnection) u.openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(10000);
            connection.addRequestProperty("User-Agent", USER_AGENT);
            connection.connect();

            // Get content
            BufferedInputStream bufIn =
                    new BufferedInputStream(connection.getInputStream());
            byte[] buffer = new byte[1024];
            int n;
            ByteArrayOutputStream bufOut = new ByteArrayOutputStream();
            boolean first = true;
            while ((n = bufIn.read(buffer)) > 0) {
                if (first)
                    if (buffer[0]!=73 || buffer[1]!=68 || buffer[2]!=51)
                        return "";
                first = false;
                bufOut.write(buffer, 0, n);
            }

            File output = new File(filename+".___");

            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(output));

            // Done, save data;
            out.write(bufOut.toByteArray());
            out.flush();
            long time= System.currentTimeMillis();
            output.setLastModified(time);
        }   catch (Throwable e) {
            return "";
    }

        File file = new File(filename+".___");
        long length = file.length();

        long sdl = prefs.getLong("SummaryDownloadSize",0);
        prefs.edit().putLong("SummaryDownloadSize",sdl+length).commit();


        if (length<200000) {
            file.delete();
            file=null;
            return "";
        }

        File mp3File = new File(filename+".mp3");
        boolean success = file.renameTo(mp3File);
        mp3File.setLastModified(System.currentTimeMillis());

        if(success) {
            file = null;
            if (!MainActivity.isPro())
                DecDownloadLimit();
            return filename+".mp3";
        }
        file.delete();
        file=null;

        return "";
    }

    public void RemoveUnusedRingtones(){
        try {

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
            if (fList2 != null && fList2.length > 1)
                Arrays.sort(fList2, new Comparator<File>() {
                    public int compare(File f1, File f2) {
                        return Long.compare(f1.lastModified(), f2.lastModified());
                    }
                });


            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            int max = Integer.parseInt(prefs.getString("DownloadMaxFiles","30"));
            int mincache = Integer.parseInt(prefs.getString("MinCachedFiles","10"));

            // Если количество файлов из кеша меньше доступных но нескачанных файлов плюс минимум в кеше
            // Чтобы кеш наполняся, если скачивание не получается
            if (fList2 == null || fList2.length <= max-fList.length+1+mincache) return;
            int rc = fList2.length - (max-fList.length+1+mincache);
            for (int i=fList2.length-rc;i<fList2.length;i++)
                fList2[i].delete();
        }
        catch(Exception e){  }
    }


    protected void onPostExecute(Void result) {
        // dismiss progress dialog and update ui
    }



    // Путь, куда складывать скачиваемые рингтоны. Зависит от галочки "use external storage"
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

    // Если SdCard доступна
    public static boolean isSdCardeAvailable(){
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state))
            return true;
        return false;
    }

}

class SiteParser{
    public String[] GetRandomRingtone(Context context) {
        return null;
    }
    public String DownloadPage(String strUrl) {
        String result="";
        String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:11.0) " + "Gecko/20100101 Firefox/11.0";
        try {
            URL url = new URL(strUrl);

            // Etablish connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // Get method
            connection.setRequestMethod("GET");
            connection.setReadTimeout(10000);
            // Set User-Agent to "mimic" the behavior of a web browser. In this
            // example, I used my browser's info
            connection.addRequestProperty("User-Agent", USER_AGENT);
//            connection.setDoOutput(false);
            connection.connect();

            // Get content
            StringBuffer sb = new StringBuffer();
            InputStream is = null;

            try {
                is = new BufferedInputStream(connection.getInputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String inputLine = "";
                while ((inputLine = br.readLine()) != null) {
                    sb.append(inputLine);
                }
                result = sb.toString();
            }
            catch (Exception e) {
            }
            finally {
                if (is != null) {
                    try {
                        is.close();
                    }
                    catch (IOException e) {
                    }
                }
            }

        }   catch (Throwable e) {
            String a = e.getMessage();
            a+="";
        }
        return result;
    }
}

class SiteMyrington extends SiteParser{
    public String[] GetRandomRingtone(Context context) {
        String[] v = {"",""};
        try {
            Random r = new Random();
            int i = r.nextInt(853);

            String ss = DownloadPage("http://myrington.ru/load/?page"+i);
            ArrayList links = new ArrayList();

            Pattern p = Pattern.compile("mp3:\\s\\\"(\\/[^\\/]+[^\"]*\\.mp3)\\\"|imgvidscr\\\"\\>\\<img src=\\\"[^\\\"]*\\\"\\salt=\\\"([^\"]*)\\\"", Pattern.DOTALL);
            Matcher m = p.matcher(ss);

            String[] u=new String[2];
            u[0]=u[1]="";
            while(m.find()) {
                String urlStr = m.group(1);
                String nameStr = m.group(2);
                if (urlStr!=null) {
                    if (!u[0].isEmpty()){
                        links.add(u.clone());
                        u[0]=u[1]="";
                    }
                    u[0] = "http://myrington.ru"+urlStr.trim();
                }
                if (nameStr!=null)
                    u[1] = nameStr.replaceAll("&quot;","").replaceAll("&quot;","").replaceAll("\\\\","").replaceAll("/","").replaceAll("&#39;","").trim();
            }
            if (!u[0].isEmpty()){
                links.add(u.clone());
            }

            u[0]=u[1]="";
            if (links.size()==0)
                return v;

            i = r.nextInt(links.size());
            v = (String[])links.get(i);
        }   catch (Throwable e) {
        }

        return v;
    }
}

class SiteRingon extends SiteParser{
    public String[] GetRandomRingtone(Context context) {
        String[] v = {"",""};
        try {
            Random r = new Random();
            int i = r.nextInt(1277);

            String ss = DownloadPage("https://ringon.ru/novye&page="+i);
            ArrayList links = new ArrayList();

            Pattern p = Pattern.compile("mp3:\\s\\\"([^\\\"]+)\\\"|track-name\\\"\\>([^\\<]*)\\<", Pattern.DOTALL);
            Matcher m = p.matcher(ss);

            String[] u=new String[2];
            u[0]=u[1]="";
            while(m.find()) {
                String urlStr = m.group();
                if (urlStr.substring(0,3).equals("mp3")) {
                    if (!u[0].isEmpty()){
                        links.add(u.clone());
                        u[0]=u[1]="";
                    }
                    u[0] = urlStr.substring(6, urlStr.length() - 1).trim();
                }
                else
                if (urlStr.substring(0,5).equals("track"))
                    u[1] = urlStr.substring(12,urlStr.length()-1).replaceAll("&quot;","").replaceAll("\\\\","").replaceAll("/","").replaceAll("&#39;","").trim();
            }
            if (!u[0].isEmpty()){
                links.add(u.clone());
            }

            u[0]=u[1]="";
            if (links.size()==0)
                return v;

            i = r.nextInt(links.size());
            v = (String[])links.get(i);
        }   catch (Throwable e) {
        }

        return v;
    }
}

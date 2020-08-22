package com.bacompany.reallyrandomringtone;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.app.ActivityManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.TimeUtils;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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

import static android.R.attr.flipInterval;
import static android.R.attr.fragment;
import static android.view.View.GONE;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    private TextView LV;
    private String intpath;
    private String extpath;
    static MediaPlayer myMediaPlayer;
    private BroadcastReceiver receiver, callreceiver;
    static  private String CurrentPlayingFile;
    public String Mp3ForContact="";


    static public  boolean isPro(){
        return false;
    }

    public void onCompletion(MediaPlayer mp){
        ToggleButton b = (ToggleButton) findViewById(R.id.playstop);
        b.setChecked(true);
        StopPlayFavButtons();
        SetAllDelFavButtonVisible();
//        MovePlayedRingtone(getApplicationContext(),CurrentPlayingFile);
        UpdateStatistics();
    }

    public boolean onError(MediaPlayer mp, int what, int extra){
        ToggleButton b = (ToggleButton) findViewById(R.id.playstop);
        b.setChecked(true);
        StopPlayFavButtons();
        SetAllDelFavButtonVisible();
//        MovePlayedRingtone(getApplicationContext(),CurrentPlayingFile);
        UpdateStatistics();
        return false;
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

    }

    public void OnAdlayerClick(View view) {
        ResetDownloadLimit();
        UpdateStatistics();
    }


    protected int ResetDownloadLimit(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        prefs.edit().putInt("DownloadLimit",30).commit();
        return 30;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Map<String, File> externalLocations = ExternalStorage.getAllStorageLocations();
        intpath = Environment.getExternalStorageDirectory().getAbsolutePath();
        extpath = "";
        if (externalLocations.get(ExternalStorage.EXTERNAL_SD_CARD)!=null)
            extpath = externalLocations.get(ExternalStorage.EXTERNAL_SD_CARD).getAbsolutePath();

        if (isPro()) {
            findViewById(R.id.favlayout).setVisibility(GONE);
//            findViewById(R.id.getpro).setVisibility(GONE);
        } else{
            MobileAds.initialize(getApplicationContext(), "ca-app-pub-5368195271545700~8384614074");

            AdView mAdView = (AdView) findViewById(R.id.adView);
            AdRequest asRequest = new AdRequest.Builder().build();
            mAdView.loadAd(asRequest);
            mAdView.setAdListener(new AdListener() {
                public void onAdLeftApplication() {
                    ResetDownloadLimit();
                    UpdateStatistics();
                }
            });
        }

        List<String> permissions = new ArrayList<String>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.READ_CONTACTS);
            }
            if (checkSelfPermission(android.Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED)
                permissions.add(android.Manifest.permission.WRITE_CONTACTS);
            if (checkSelfPermission(android.Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
                permissions.add(android.Manifest.permission.INTERNET);
            if (checkSelfPermission(android.Manifest.permission.WRITE_SETTINGS) != PackageManager.PERMISSION_GRANTED)
                permissions.add(android.Manifest.permission.WRITE_SETTINGS);
            if (checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
                permissions.add(android.Manifest.permission.READ_PHONE_STATE);
            if (checkSelfPermission(android.Manifest.permission.MODIFY_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
                permissions.add(android.Manifest.permission.MODIFY_PHONE_STATE);
            if (checkSelfPermission(android.Manifest.permission.PROCESS_OUTGOING_CALLS) != PackageManager.PERMISSION_GRANTED)
                permissions.add(android.Manifest.permission.PROCESS_OUTGOING_CALLS);
            if (checkSelfPermission(android.Manifest.permission.RECEIVE_BOOT_COMPLETED) != PackageManager.PERMISSION_GRANTED)
                permissions.add(android.Manifest.permission.RECEIVE_BOOT_COMPLETED);
            String[] perm = new String[ permissions.size() ];
            permissions.toArray( perm );
            try {requestPermissions(perm, 1);} catch (Throwable t) {}
        }

/*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
*/
        LV = (TextView) findViewById(R.id.textView2);
        startService(new Intent(this, RingtoneDownload.class));

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra("rrr.manager");
                if (s=="bmm")
                    UpdateStatistics();
                // do something here.
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver),
                new IntentFilter("rrr.manager")
        );
/*

        callreceiver = new CallReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver((callreceiver),
                new IntentFilter("android.intent.action.PHONE_STATE")
        );
*/


        UpdateStatistics();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setElevation(0); // or other

        UpdateFavorites();

        Button b1 = (Button) findViewById(R.id.content_main_btn);
        Button b2 = (Button) findViewById(R.id.content_favorites_btn);
        Button b3 = (Button) findViewById(R.id.content_last_btn);
        b1.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view) {
                OnHomeClick(view);
            }
        });
        b2.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view) {
                OnFavoritesClick(view);
            }
        });
        b3.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view) {
                OnHistoryClick(view);
            }
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int wc = prefs.getInt("WarnCount", 3);
        if (wc>0) {
            prefs.edit().putInt("WarnCount",wc-1).commit();
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(getString(R.string.warning))
                    .setMessage(getString(R.string.helpstr))
                    .setCancelable(false)
                    .setNegativeButton("Ok",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
            AlertDialog alert = builder.create();
            alert.show();
        }
        // Specify that tabs should be displayed in the action bar.

    }

    public boolean isServiceRunning(String serviceClassName){
        ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            if (runningServiceInfo.service.getClassName().equals(serviceClassName)){
                return true;
            }
        }
        return false;
    }

    public void UpdateStatistics() {

        String ServiceState = "остановлена";
        if (isServiceRunning("com.bacompany.reallyrandomringtone.RingtoneDownload"))
            ServiceState = "запущена";

        String s = getString(R.string.statistics);
        if (isPro())
            s = getString(R.string.statistics_pro);

        String path = "";
        File[] fList;

        int Dcount = 0;
        float Dsize = 0.0f;
        path = GetRingtoneStorePath() + "/download/";
        fList = (new File(path)).listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".mp3");
            }
        });
        if (fList != null) {
            Dcount = fList.length;
            for (File tf : fList) Dsize += (float) tf.length();
        }

        int Pcount = 0;
        float Psize = 0.0f;
        path = GetRingtoneStorePath() + "/played/";
        fList = (new File(path)).listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".mp3");
            }
        });
        if (fList != null) {
            Pcount = fList.length;
            for (File tf : fList) Psize += (float) tf.length();
        }

        int Fcount = 0;
        float Fsize = 0.0f;
        path = GetRingtoneStorePath() + "/favorites/";
        fList = (new File(path)).listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".mp3");
            }
        });
        if (fList != null) {
            Fcount = fList.length;
            for (File tf : fList) Fsize += (float) tf.length();
        }

        Dsize = Math.round(Dsize * 10.0f / 1024.0f / 1024.0f) / 10.0f;
        Psize = Math.round(Psize * 10.0f / 1024.0f / 1024.0f) / 10.0f;
        Fsize = Math.round(Fsize * 10.0f / 1024.0f / 1024.0f) / 10.0f;


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int sd = prefs.getInt("SummaryDownload", 0);
        long sds = prefs.getLong("SummaryDownloadSize", 0);
        float ss = Math.round(((float) sds) * 10 / 1024 / 1024) / 10.0f;
        int DownloadLimid = prefs.getInt("DownloadLimit",30);
        String DownloadLimidAdd = "";
        if (DownloadLimid<5) DownloadLimidAdd = getString(R.string.downloadalimittext);

        if (!isPro())
            s = String.format(s, sd, ss, Dcount, Dsize, Pcount, Psize, Fcount, Fsize, DownloadLimid, DownloadLimidAdd);
        else
            s = String.format(s, sd, ss, Dcount, Dsize, Pcount, Psize, Fcount, Fsize);
        TextView v = (TextView) findViewById(R.id.statistics);
        v.setText(Html.fromHtml(s), TextView.BufferType.SPANNABLE);

        TextView crn = (TextView) findViewById(R.id.CustomRingtoneName);
        String nr = GetCurrentRingtone();
        if (!nr.isEmpty())
            nr = nr.substring(nr.lastIndexOf("/") + 1);
        else
            nr = "Нет рингтона";
        crn.setText(nr);


    }

    public void UpdateFavorites(){

        String path = GetRingtoneStorePath() + "/favorites/";

        File f = new File(path);
        File[] favList = f.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".mp3");
            }
        });


        LinearLayout table = (LinearLayout) findViewById(R.id.favtable);
        table.removeAllViews();

        if (favList!=null) for (File fl : favList) {
            LinearLayout ll = new LinearLayout(this);

            ll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            ll.setGravity(Gravity.CENTER_VERTICAL);

            LinkTextView t = new LinkTextView(this,fl.getName().substring(0,fl.getName().length()-4));
            t.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1f));
            t.setClickable(true);
            t.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LinkTextView t = (LinkTextView)v;
                    openWebPage("https://m.youtube.com/results?search_query="+t.ss);

                }
            });
            t.setText(fl.getName());
            ll.addView(t);

            MyDeleteButton b2 = new MyDeleteButton(this,fl.getAbsolutePath());
            b2.setBackgroundResource(R.drawable.ic_to_remove);
            b2.setLayoutParams(new TableRow.LayoutParams(70, LinearLayout.LayoutParams.WRAP_CONTENT));
            b2.setGravity(Gravity.RIGHT);
            if (CurrentPlayingFile!=null && CurrentPlayingFile.equals(fl.getAbsolutePath()))
                b2.setVisibility(GONE);
            ll.addView(b2);

            MySetToContactButton b = new MySetToContactButton(this,fl.getAbsolutePath());
            b.setBackgroundResource(R.drawable.ic_to_contact);
            b.setLayoutParams(new TableRow.LayoutParams(70, LinearLayout.LayoutParams.WRAP_CONTENT));
            b.setGravity(Gravity.RIGHT);
            ll.addView(b);

            MyPlayStopButton tb = new MyPlayStopButton(this,fl.getAbsolutePath(),b2);
            tb.setBackgroundResource(R.drawable.playcheck);
            tb.setTextOff("");
            tb.setTextOn("");
            tb.setText("");
            tb.setLayoutParams(new TableRow.LayoutParams(70, LinearLayout.LayoutParams.WRAP_CONTENT));
            if (CurrentPlayingFile!=null && CurrentPlayingFile.equals(fl.getAbsolutePath()))
                tb.setChecked(false);
            else
                tb.setChecked(true);
            //        tb.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT,0.15f));
            tb.setGravity(Gravity.RIGHT);
            ll.addView(tb);
            table.addView(ll);
        }
    }

    public void UpdateHistory(){

        String path = GetRingtoneStorePath() + "/played/";

        File f = new File(path);
        File[] favList = f.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".mp3");
            }
        });

        if (favList != null && favList.length > 1)
            Arrays.sort(favList, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    return Long.compare(f2.lastModified(), f1.lastModified());
                }
            });


        LinearLayout table = (LinearLayout) findViewById(R.id.lasttable);
        table.removeAllViews();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int mincache = Integer.parseInt(prefs.getString("MinCachedFiles","10"));

        for (File fl : favList) {
            LinearLayout ll = new LinearLayout(this);

            ll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            ll.setGravity(Gravity.CENTER_VERTICAL);

            LinkTextView t = new LinkTextView(this,fl.getName().substring(0,fl.getName().length()-4));
            t.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1f));
            t.setClickable(true);
            t.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LinkTextView t = (LinkTextView)v;
                    openWebPage("https://m.youtube.com/results?search_query="+t.ss);

                }
            });
            t.setText(fl.getName());
            ll.addView(t);

            MymoveButton b = new MymoveButton(this,fl.getAbsolutePath());
            b.setBackgroundResource(R.drawable.ic_to_favorites);
            b.setLayoutParams(new TableRow.LayoutParams(70, LinearLayout.LayoutParams.WRAP_CONTENT));
            b.setGravity(Gravity.RIGHT);
            if (CurrentPlayingFile!=null && CurrentPlayingFile.equals(fl.getAbsolutePath()))
                b.setVisibility(GONE);
            ll.addView(b);

            MyPlayStopButton tb = new MyPlayStopButton(this,fl.getAbsolutePath(),b);
            tb.setBackgroundResource(R.drawable.playcheck);
            tb.setTextOff("");
            tb.setTextOn("");
            tb.setText("");
            tb.setLayoutParams(new TableRow.LayoutParams(70, LinearLayout.LayoutParams.WRAP_CONTENT));
            if (CurrentPlayingFile!=null && CurrentPlayingFile.equals(fl.getAbsolutePath()))
                tb.setChecked(false);
            else
                tb.setChecked(true);
            //        tb.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT,0.15f));
            tb.setGravity(Gravity.RIGHT);
            ll.addView(tb);
            table.addView(ll);
            mincache--;
            if (mincache<1) break;
        }
    }

    public void MoveToFavorites(String file) {
        final String movefile = file;

        AlertDialog.Builder altBx = new AlertDialog.Builder(this);
        altBx.setTitle(getString(R.string.are_you_sure));
        altBx.setMessage(file.substring(file.lastIndexOf("/") + 1)+"\n\n"+getString(R.string.are_you_sure_move_mp3));
        altBx.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
            }

        });
        altBx.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                try{
                    String path = GetRingtoneStorePath() + "/favorites/" + movefile.substring(movefile.lastIndexOf("/") + 1);

                    File fc = new File(movefile);
                    File fnf = new File(path);
                    fc.renameTo(fnf);
                    fnf.setLastModified(System.currentTimeMillis());


                    UpdateStatistics();
                    UpdateHistory();
                }catch(Exception e){}
            }
        });
        altBx.show();
    }

    public void DeleteMp3(String ffile) {

        final String delfile = ffile;

        AlertDialog.Builder altBx = new AlertDialog.Builder(this);
        altBx.setTitle(getString(R.string.are_you_sure));
        altBx.setMessage(ffile.substring(ffile.lastIndexOf("/") + 1)+"\n\n"+getString(R.string.are_you_sure_delete_mp3));
        altBx.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                //show any message
            }

        });
        altBx.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                try{
                    File f = new File(delfile);
                    if (f.exists()) {
                        f.delete();
                        UpdateFavorites();
                    }
                }catch(Exception e){}
            }
        });
        altBx.show();
    }

    public void PlayFav(String file){
        if (myMediaPlayer!=null) {
            if (myMediaPlayer.isPlaying()){
                myMediaPlayer.stop();
                myMediaPlayer = null;
                CurrentPlayingFile="";
            }
        }

        if (!(new File(file)).exists())
            return;

        ToggleButton tb = (ToggleButton)findViewById(R.id.playstop);
        tb.setChecked(true);

        CurrentPlayingFile = file;
        myMediaPlayer = MediaPlayer.create(getApplicationContext(), Uri.parse(file));
        myMediaPlayer.setLooping(false);
        myMediaPlayer.setOnCompletionListener(this);
        myMediaPlayer.setOnErrorListener(this);
        myMediaPlayer.start();

    }

    public void StopPlayFavButtons(){
        LinearLayout ml = ((LinearLayout)findViewById(R.id.favtable));
        for (int i=0;i<ml.getChildCount();i++){
            LinearLayout ll = (LinearLayout)ml.getChildAt(i);
            ToggleButton tb = (ToggleButton)ll.getChildAt(3);
            tb.setChecked(true);
        }
        ml = ((LinearLayout)findViewById(R.id.lasttable));
        for (int i=0;i<ml.getChildCount();i++){
            LinearLayout ll = (LinearLayout)ml.getChildAt(i);
            ToggleButton tb = (ToggleButton)ll.getChildAt(2);
            tb.setChecked(true);
        }
    }

    public void StopPlayFav(){
        if (myMediaPlayer!=null) {
            if (myMediaPlayer.isPlaying()){
                myMediaPlayer.stop();
                myMediaPlayer = null;
                CurrentPlayingFile="";
            }
        }
    }

    public void SetAllDelFavButtonVisible(){
        LinearLayout ml = ((LinearLayout)findViewById(R.id.favtable));
        for (int i=0;i<ml.getChildCount();i++){
            LinearLayout ll = (LinearLayout)ml.getChildAt(i);
            Button b = (Button)ll.getChildAt(1);
            b.setVisibility(View.VISIBLE);
        }
        ml = ((LinearLayout)findViewById(R.id.lasttable));
        for (int i=0;i<ml.getChildCount();i++){
            LinearLayout ll = (LinearLayout)ml.getChildAt(i);
            Button b = (Button)ll.getChildAt(1);
            b.setVisibility(View.VISIBLE);
        }
    }
    public class LinkTextView extends TextView {
        public String ss;
        public LinkTextView(Context context, String search_string){
            super(context, null);
            ss = search_string;
        }
    }

    public class MyPlayStopButton extends ToggleButton implements View.OnClickListener{
        public String mp3;
        public Context mContext;
        public Button btn;
        public MyPlayStopButton(Context context, String file, Button button) {
            super(context, null);
            mContext = context;
            mp3 = file;
            btn = button;
            this.setOnClickListener(this);
        }
        public void onClick(View view) {
            if (((ToggleButton)view).isChecked()){
                ((MainActivity)mContext).StopPlayFav();
                ((MainActivity)mContext).SetAllDelFavButtonVisible();
//                ((ToggleButton)view).setChecked(true);
            } else {
                ((MainActivity)mContext).StopPlayFavButtons();
                ((ToggleButton)view).setChecked(false);
                ((MainActivity)mContext).PlayFav(mp3);
                ((MainActivity)mContext).SetAllDelFavButtonVisible();
                btn.setVisibility(GONE);
            }
        }

    }

    public class MySetToContactButton extends Button implements View.OnClickListener{
        public String mp3;
        public Context mContext;
        public MySetToContactButton(Context context, String file) {
            super(context, null);
            mContext = context;
            mp3 = file;
            this.setOnClickListener(this);
        }
        public void onClick(View view) {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(intent, 1);
            ((MainActivity)mContext).Mp3ForContact = mp3;
        }

    }

    public class MyDeleteButton extends Button implements View.OnClickListener{
        public String mp3;
        public Context mContext;
        public MyDeleteButton(Context context, String file) {
            super(context, null);
            mContext = context;
            mp3 = file;
            this.setOnClickListener(this);
        }
        public void onClick(View view) {
            ((MainActivity)mContext).DeleteMp3(mp3);
        }

    }

    public class MymoveButton extends Button implements View.OnClickListener{
        public String mp3;
        public Context mContext;
        public MymoveButton(Context context, String file) {
            super(context, null);
            mContext = context;
            mp3 = file;
            this.setOnClickListener(this);
        }
        public void onClick(View view) {
            ((MainActivity)mContext).MoveToFavorites(mp3);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home/back button
            case android.R.id.home:
                moveTaskToBack(true);
                finishAffinity();
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
                finish();
                break;
            default:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }
        return true;

/*
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
*/
    }

    public void HideAllFrames(){
        ((ToggleButton) findViewById(R.id.content_main_btn)).setChecked(false);
        ((ToggleButton) findViewById(R.id.content_favorites_btn)).setChecked(false);
        ((ToggleButton) findViewById(R.id.content_last_btn)).setChecked(false);
    }

    public void OnHomeClick(View view){
        setTitle(getString(R.string.app_name));
        HideAllFrames();
        ((ToggleButton) findViewById(R.id.content_main_btn)).setChecked(true);

        findViewById(R.id.content_main).setVisibility(View.VISIBLE);
        findViewById(R.id.content_favorites).setVisibility(GONE);
        findViewById(R.id.content_last).setVisibility(GONE);
    }
    public void OnFavoritesClick(View view){
        setTitle("Избранные рингтоны");
        HideAllFrames();
        UpdateFavorites();
        ((ToggleButton) findViewById(R.id.content_favorites_btn)).setChecked(true);

        findViewById(R.id.content_main).setVisibility(GONE);
        findViewById(R.id.content_favorites).setVisibility(View.VISIBLE);
        findViewById(R.id.content_last).setVisibility(GONE);
    }
    public void OnHistoryClick(View view){
        setTitle("Последние проигранные рингтоны");
        HideAllFrames();
        UpdateHistory();
        ((ToggleButton) findViewById(R.id.content_last_btn)).setChecked(true);

        findViewById(R.id.content_main).setVisibility(GONE);
        findViewById(R.id.content_favorites).setVisibility(GONE);
        findViewById(R.id.content_last).setVisibility(View.VISIBLE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Mp3ForContact==null || Mp3ForContact.equals("")) return;
        if (!(new File(Mp3ForContact)).exists()) return;
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                Uri contactData = data.getData();
                String number = "";
                Cursor cursor = getContentResolver().query(contactData, null, null, null, null);
                cursor.moveToFirst();
                String contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                cursor.close();

                try {
                    File k = new File(Mp3ForContact);

                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DATA, k.getAbsolutePath());
                    values.put(MediaStore.MediaColumns.TITLE, k.getName());
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp3");
                    values.put(MediaStore.MediaColumns.SIZE, k.length());
                    values.put(MediaStore.Audio.Media.ARTIST, R.string.app_name);
                    values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
                    values.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
                    values.put(MediaStore.Audio.Media.IS_ALARM, false);
                    values.put(MediaStore.Audio.Media.IS_MUSIC, false);

                    Uri uri = MediaStore.Audio.Media.getContentUriForPath(k.getAbsolutePath());

                    getContentResolver().delete(
                            uri,
                            MediaStore.MediaColumns.DATA + "=\""
                                    + k.getAbsolutePath() + "\"", null);
                    Uri newUri = getContentResolver().insert(uri, values);

                    Uri lookupUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId);
                    ContentValues localContentValues = new ContentValues();
                    localContentValues.put(ContactsContract.Data.RAW_CONTACT_ID, contactId);
                    localContentValues.put(ContactsContract.Data.CUSTOM_RINGTONE, newUri.toString());
                    getContentResolver().update(lookupUri, localContentValues,null, null);
                } catch (Throwable t) {

                }


            }
        }
    }

    public void openWebPage(String url) {
        Uri webpage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }
    public void OnClick(View view){

        openWebPage("https://youtube.com");

//        stopService(new Intent(MainActivity.this, RingtoneDownload.class));
//        UpdateStatistics();

//        GetRingtoneStorePath();

//        LV.setText(s);
    }

    public void onNextClick(View v){
        ToggleButton b = (ToggleButton) findViewById(R.id.playstop);
        if (myMediaPlayer!=null) {
            if (myMediaPlayer.isPlaying()){
                myMediaPlayer.stop();
                myMediaPlayer = null;
                CurrentPlayingFile="";
            }
        }
        ChangeRingtone();
        UpdateStatistics();

        CurrentPlayingFile = GetCurrentRingtone();
        if (CurrentPlayingFile.isEmpty()) {
            b.setChecked(true);
            return;
        }
        b.setChecked(false);
        myMediaPlayer = MediaPlayer.create(getApplicationContext(), Uri.parse(CurrentPlayingFile));
        myMediaPlayer.setLooping(false);
        myMediaPlayer.setOnCompletionListener(this);
        myMediaPlayer.setOnErrorListener(this);
        myMediaPlayer.start();
    }

    public void onPlayStopClick(View v)
    {
        ToggleButton b = (ToggleButton) findViewById(R.id.playstop);
        if (!b.isChecked()) {
            String nextringtone = GetCurrentRingtone();
            if (nextringtone.isEmpty()) {
                b.setChecked(true);
                return;
            }
            if (myMediaPlayer!=null) myMediaPlayer.stop();
            CurrentPlayingFile = nextringtone;
            myMediaPlayer = null;
            myMediaPlayer = MediaPlayer.create(getApplicationContext(), Uri.parse(nextringtone));
            myMediaPlayer.setLooping(false);
            myMediaPlayer.setOnCompletionListener(this);
            myMediaPlayer.setOnErrorListener(this);
            myMediaPlayer.start();
        } else
            if (myMediaPlayer!=null) {
                myMediaPlayer.stop();
                CurrentPlayingFile="";
//                MoveOldRingtone(getApplicationContext());
                UpdateStatistics();
            }

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
        if (fList==null || fList.length<2) return "";
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

        if (sortedByDate != null && sortedByDate.length > 1)
            Arrays.sort(sortedByDate, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    return Long.compare(f2.lastModified(), f1.lastModified());
                }
            });

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

    public void ChangeRingtone(){
        try {
            String nextfile = GetNextRingtone();
            if (nextfile!=null && !nextfile.isEmpty()) {
                String c = GetCurrentRingtone();
                File fc = new File(c);
                String nfile = GetRingtoneStorePath() + "/played/" + c.substring(c.lastIndexOf("/") + 1);
                File fnf = new File(nfile);
                fc.renameTo(fnf);
                fnf.setLastModified(System.currentTimeMillis());
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
            if (fList2!=null) Arrays.sort(fList2, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    return Long.compare(f1.lastModified(), f2.lastModified());
                }
            });

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
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

}


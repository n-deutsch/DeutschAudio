package com.deutschgmail.nate.deutschaudio;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    //toggle pause/unpause
    private boolean paused;

    //background color
    private int backgroundColor;

    //buttons at the top of activity_main
    private Button artistBtn;
    private Button trackBtn;
    private Button albumBtn;
    private Button historyBtn;
    private Button searchBtn;

    //index of the currently pressed button!
    int button_selection;

    //text fields on the bottom of our screen
    private TextView navTitle;
    private TextView navSubtitle;

    //timestamp for current location AND max song length
    private TextView runningTime;
    private TextView maxTime;

    //seekBar on bottom of screen
    private SeekBar progressBar;

    //mediaPlayer responsible for playing audio
    private MediaPlayer mp;
    //current playing song accessible from everywhere in MainActivity
    private String nowPlaying;
    //current time (int)
    private int timeStamp;
    //boolean values to catch errors
    private boolean playing_audio;
    private boolean song_loaded;

    //determine how we organize masterlist
    private String master_filter;

    //reference to central list in activity_main
    private ListView MasterList;

    //keep track of where we are in our masterList
    private int masterListIndex;

    private boolean playing_history;
    //linked list of history
    private DisplayList historyList;
    //linked list of values to display
    private DisplayList displayList;
    //linked sublist of songs
    private DisplayList subList;

    //linked list of search values
    private DisplayList searchList;

    //list of tracks to be played
    private DisplayList hotList;
    private int hotIndex;

    //displayList for randomized elements of hotList
    private DisplayList shuffleList;
    private boolean shuffling;

    //boolean values to catch errors
    private boolean displaying_sublist;
    private boolean displaying_searchbar;

    //remove bug where we skip too many times
    private boolean enable_skipping;

    //int value to check SD card reading permission
    private static final int PERMISSION_REQUEST = 1;

    //array of all audio files on SD card
    ArrayList<SongData> songList;

    //background taskss (update progressBAR and historyList)
    private Handler mHandler = new Handler();
    private MyAsyncTask task;
    private SaveTask saveTask;

    //makes sure we only call initialize() once
    private boolean startup = true;

    //int used for notification broadcast
    private int notification_action;

    //required to listen for headset unplug
    private MusicIntentReceiver myReceiver;

    /*shouldAskPermissions() gets the current build version to see if
    we need permission to read and writefrom text files*/
    protected boolean shouldAskPermissions() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    /*shouldAskPermissions() asks the user if we can read/write to text files*/
    @TargetApi(23)
    protected void askPermissions() {
        String[] permissions = {
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"
        };
        int requestCode = 200;
        requestPermissions(permissions, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (shouldAskPermissions()) {
            askPermissions();
        }
        initialize();
    }

    /*initialize() was created to stop crowding the onCreate function. Happens once on startup.*/
    public void initialize()
    {
        //only call this once!
        if(!startup)
        {return;}

        //identify activity_main UI components
        progressBar = findViewById(R.id.progressBar);
        artistBtn = findViewById(R.id.artistBtn);
        trackBtn =  findViewById(R.id.trackBtn);
        albumBtn = findViewById(R.id.albumBtn);
        historyBtn = findViewById(R.id.historyBtn);
        searchBtn = findViewById(R.id.searchBtn);
        navTitle = findViewById(R.id.navTitle);
        navSubtitle = findViewById(R.id.navSubtitle);
        runningTime = findViewById(R.id.runningTime);
        maxTime = findViewById(R.id.maxTime);
        navTitle.setText("-");
        navSubtitle.setText("-");

        //populate historyList from text file
        loadHistory();

        //async tasks for progress bar and history update
        saveTask = new SaveTask();
        task = new MyAsyncTask();
        myReceiver = new MusicIntentReceiver();

        //constant color for app background
        backgroundColor = Color.rgb(50,50,75);
        //set default color scheme
        ConstraintLayout search = findViewById(R.id.searchBarHeader);
        search.setBackgroundColor(backgroundColor);
        ConstraintLayout cl = findViewById(R.id.navButtons);
        cl.setBackgroundColor(backgroundColor);
        ConstraintLayout topButtons = findViewById(R.id.buttons);
        topButtons.setBackgroundColor(backgroundColor);
        ConstraintLayout master = findViewById(R.id.master);
        master.setBackgroundColor(backgroundColor);


        MasterList = findViewById(R.id.MasterList);

        //ARTIST/TRACK/HISTORY/ALBUM/SEARCH on top of UI
        bringToFront();

        //change to 'artist' list on startup
        adjustButtons("artist");
        master_filter = "artist";
        button_selection = 0;

        //unpaused and unshuffled by default
        paused = false;
        shuffling = false;

        //hide searchbar and subList header
        hideHeader(-1);
        hideSearchBar(-1);

        //UI values set to default
        displaying_sublist = false;
        displaying_searchbar = false;
        playing_history = false;
        playing_audio = false;
        song_loaded = false;
        enable_skipping=true;

        //new arraylist for open_storage()
        songList = new ArrayList<>();

        //nothing playing on startup
        nowPlaying = "";

        //listen for headset unplug
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(myReceiver, filter);

        //populate songList with every audio file on SD card
        openStorage();
        //default to "artist" list
        SetList();
        //merge progressbar with mediaplayer
        initializeSeekBar();
        //begin saving historyList
        historyUpdate();

        //gotta call this or else keyboard opens up on activity start
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        startup = false;
    }

    /*playTrack() is called when the user clicks on masterList when masterList is displaying tracks. */
    public void playTrack(int index)
    {
        int i = 0;
        int len = 0;
        String substring = "";
        DataNode dn;

        //reset hotList and shuffleList
        hotList = new DisplayList();
        shuffleList = new DisplayList();
        //index is what we clicked from masterList
        hotIndex = index;

        playing_history = false;
        if(master_filter.equals("history"))
        {hotList.copy(historyList); playing_history = true;}
        else if(displaying_searchbar==true)
        {hotList.copy(searchList); hotIndex--;}
        else if(displaying_sublist == false)
        {hotList.copy(displayList);}
        else
        {hotList.copy(subList);}

        //create a randomized version of hotList for shuffle...
        generateShuffleList();

        //if shuffle is on, adjust INDEX
        if(shuffling == true)
        {
            //find the selected song in our shuffleList
            dn = hotList.get(index);
            hotIndex = shuffleList.find(dn.location);
        }

        startPlaying(0);

        return;
    }

    /*startPlaying() begins playing a new song. Can be called from playTrack(), skipForward(), OR skipBackward()*/
    public void startPlaying(int attempts)
    {
        String substring = "";
        DataNode dn = null;

        //case:we skipped past the end of our list
        if(hotIndex >= hotList.getLength())
        {
            //set to start of list
            //Toast.makeText(this,"OUT OF RANGE - Jump to start",Toast.LENGTH_SHORT).show();
            hotIndex = 0;
        }
        else if(hotIndex < 0) //case: we skipped before the start
        {
            //set to end of list
            //Toast.makeText(this,"OUT OF RANGE - Jump to end",Toast.LENGTH_SHORT).show();
            hotIndex = hotList.getLength();
            hotIndex--;
        }


        if(shuffling == true) //grab out datanode from shuffle
        {dn = shuffleList.get(hotIndex);}
        else //grab our datanode from hotlist
        {dn = hotList.get(hotIndex);}

        //something went wrong, exit function
        if(dn==null)
        {return;}

        //kill the previous song
        if(playing_audio == true)
        {mp.pause();}

        mp = new MediaPlayer();
        //play the NEXT song when this one finishes
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Toast.makeText(MainActivity.this,"song completion event",Toast.LENGTH_SHORT).show();
                songSkipForward();
            }
        });
        //mp.reset();

        try
        {
            mp.setDataSource(dn.location);
            nowPlaying = dn.location;
        }catch (Exception e)
        {
            //retry five times on error
            if(attempts<5) {attempts++; startPlaying(attempts);}
            return;
        }

        try
        {
            mp.prepare();
        }catch(Exception e)
        {
            //retry five times on error
            if(attempts<5) {attempts++; startPlaying(attempts);}
            return;
        }

        //skip to whatever timestamp if we loaded history
        if(master_filter.equals("history"))
        {mp.seekTo(Integer.parseInt(dn.subData));}
        else //not playing history? go to start
        {mp.seekTo(0);}

        //FINALLY start playing the song
        mp.start();

        //update bottom nav fields
        //adjust the title and subtitle for bottom navigation
        TextView navTitle = findViewById(R.id.navTitle);
        TextView navSubtitle = findViewById(R.id.navSubtitle);
        TextView runTime = findViewById(R.id.runningTime);
        TextView maxTime = findViewById(R.id.maxTime);

        substring = dn.data;
        if(substring.length() > 40)
        {substring = substring.substring(0,40) + "...";}

        navTitle.setText(substring);
        navSubtitle.setText(dn.artist);
        runTime.setText("00:00");
        maxTime.setText(millisecondsToHours(dn.duration));

        //make changes to seekbar
        progressBar.setMax(mp.getDuration());
        progressBar.setProgress(0);

        //begin updating seekbar every second
        progressBarUpdate();

        //swap the pause button if we're currently paused
        if(paused == true)
        {togglePause();}

        //begin playing audio
        playing_audio = true;
        song_loaded = true;

        //set up controls OUTSIDE of the app
        customNotification();

        return;
    }

    /*generateShuffleList() called every time hotList is reset. Randomly generates a list of values from hotList.
    hotList and shuffleList have the same lengths and every element in hotList appears in shuffleList exactly once*/
    public void generateShuffleList()
    {
        int i = 0;
        int j = 0;
        int len = 0;
        int generated = 0;
        boolean in_use = false;

        String currentFilePath = "";
        String newFilePath = "";

        DataNode dn = null;
        DataNode compare = null;

        boolean[] used = null;

        shuffleList = new DisplayList();

        Random ran = new Random();

        //this will be the size of shuffleList
        len = hotList.getLength();

        used = new boolean[len];
        //set all values to FALSE
        for(i=0; i<len; i++)
        {used[i]=false;}

        //add one element to shuffleList per loop
        for(i=0; i<len; i++)
        {
            //generate random number
            do {
                //pull a random node out of hotList
                generated = ran.nextInt(len);
                //scan through shuffleList - see if we already have this
                in_use = used[generated];
            }while(in_use == true);

            //add song to usedlist
            used[generated] = true;
            dn = hotList.get(generated);
            shuffleList.addToEnd(dn);
        }
        return;
    }

    /*songPause() called when user clicks the pause button AND we are playing audio*/
    public void songPause()
    {
        //if no audio, exit
        if(playing_audio == false || song_loaded == false)
        {
           return;
        }

        playing_audio = false;
        mp.pause();

        return;
    }

    /*songResume() called when user clicks the pause button AND we aren't playing audio*/
    public void songResume()
    {
        if(playing_audio == false && song_loaded == true)
        {
            playing_audio = true;
            mp.start();
        }
    }

    /*songRewind() rewinds the current song by 30 seconds*/
    private void songRewind()
    {
        int timestamp = 0;

        //exit early if nothing loaded
        if(song_loaded == false)
        {return;}

        timestamp = mp.getCurrentPosition();

        //skip forward in time by 30 seconds
        timestamp = timestamp - 30000;

        //check to see if we went negative
        if(timestamp < 0)
        {
            timestamp = 0;
        }

        progressBar.setProgress(timestamp);
        mp.seekTo(timestamp);


        return;
    }

    /*songFastForward() fastforwards the current song by 30 seconds*/
    private void songFastForward()
    {
        int timestamp = 0;
        int length = 0;

        //exit early if nothing loaded
        if(song_loaded == false)
        {return;}

        timestamp = mp.getCurrentPosition();
        length = mp.getDuration();

        timestamp = timestamp + 30000;

        if(timestamp >= length)
        {
            mp.pause();
            songSkipForward();
            return;
        }

        progressBar.setProgress(timestamp);
        mp.seekTo(timestamp);

        return;
    }

    /*songSkipBack() go to the PREVIOUS song in our hotList/shuffleList*/
    private void songSkipBack()
    {
        //do nothing if we have nothing loaded
        if(song_loaded == false)
        {return;}

        if(enable_skipping==false)
        {return;}
        enable_skipping = false;

        hotIndex--;
        mp.seekTo(0);
        progressBar.setProgress(0);
        startPlaying(0);

        //boolean variable to avoid repeated skipping
        enable_skipping = true;
    }

    /*songSkipBack() go to the NEXT song in our hotList/shuffleList*/
    private void songSkipForward()
    {
        //do nothing if we have nothing loaded
        if(song_loaded == false)
        {return;}

        if(enable_skipping==false)
        {return;}
        enable_skipping = false;

        hotIndex++;
        mp.seekTo(0);
        progressBar.setProgress(0);
        startPlaying(0);

        //boolean variable to avoid repeated skipping
        enable_skipping = true;
    }

    /*applyFilter() fills displayList based on our master_filter. ARTIST,ALBUM,TRACK,ect*/
    public ArrayList<GenericDisplay> applyFilter(ArrayList<SongData> list)
    {
        int i = 0;
        int len = 0;
        int timeDif = 0;

        //reset displayList
        DataNode dn = null;
        displayList = new DisplayList();

        //arrayList we use for musicAdapter
        GenericDisplay gd = null;
        ArrayList<GenericDisplay> display = new ArrayList<>();

        String title = "";
        String subtitle = "";

        if(displaying_sublist == true)
        {
            showButtons();
            hideHeader(0);
            displaying_sublist = false;
        }

        //special case for history
        if(master_filter.equals("history"))
        {
            //for each value in historyList
            len = historyList.getLength();
            for(i=0;i<len;i++)
            {
                //add to display arrayList
                dn = historyList.get(i);
                title = dn.data;

                try {
                    timeDif = Integer.parseInt(dn.duration) - Integer.parseInt(dn.subData);
                }catch(Exception e){timeDif = 0;}

                subtitle = millisecondsToHours(Integer.toString(timeDif));
                subtitle = subtitle + " remaining";
                gd = new GenericDisplay(title,subtitle);
                display.add(gd);
            }
            return display;
        }

        //for all songs on SD card
        for(i=0; i<list.size();i++)
        {
            //skip everything in the 'Notifications' album...these are system files and NOT music
            if(list.get(i).getAlbum().equals("Notifications"))
            {continue;}
            //skip the RINGTONES album as well...
            if(list.get(i).getAlbum().equals("Ringtones"))
            {continue;}

            dn = new DataNode();
            if(master_filter.equals("artist"))
            {dn.data = list.get(i).getArtist();}
            else if(master_filter.equals("track"))
            {dn.data = list.get(i).getTitle(); dn.subData = list.get(i).getArtist();}
            else if(master_filter.equals("album"))
            {dn.data = list.get(i).getAlbum(); dn.subData = list.get(i).getArtist();}

            //we need these values regardless of master_filter
            dn.location = list.get(i).getLocation();
            dn.duration = list.get(i).getDuration();
            dn.artist = list.get(i).getArtist();

            //add node in alphabetical order
            displayList.addAlphabetical(dn);
        }

        //for items in displayList...
        for(i=0; i<displayList.getLength(); i++)
        {
            //get datanode at index i
            dn = displayList.get(i);
            if(master_filter.equals("artist"))
            {
                //if artist subData = # tracks
                title = dn.data;
                if(dn.count == 1)
                {subtitle = dn.count + " track";}
                else
                {subtitle = dn.count + " tracks";}
            }
            else if(master_filter.equals("track"))
            {
                //if track, subtitle = ARTIST and song length
                title = dn.data;
                subtitle = dn.subData + " - " + millisecondsToHours(dn.duration);
            }
            else if(master_filter.equals("album"))
            {
                //if ALBUM, subtitle = artist and number of tracks
                title = dn.data;
                subtitle = dn.subData;
                if(dn.count == 1)
                {subtitle = subtitle + " - " + dn.count + " track";}
                else
                {subtitle = subtitle + " - " + dn.count + " tracks";}
            }

            //turn title,subtitle into a GenericDisplay
            gd = new GenericDisplay(title, subtitle);
            display.add(gd);
        }

        return display;
    }

    /*SetList() populates masterList based on artist/track/album/history/search buttons*/
    public void SetList()
    {
        ArrayList<GenericDisplay> display;

        //we only need to read from the SD card once - saves phone battery and lots of time
        if(songList.size() == 0)
        {getMusic();}

        if(displaying_searchbar==true)
        {hideSearchBar(0);}

        display = applyFilter(songList);
        displaying_sublist = false;

        MasterList.setAdapter(new MusicAdapter(this,display));

        MasterList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                //Toast.makeText(MainActivity.this,"clicked item at position " + i,Toast.LENGTH_SHORT).show();
                masterListClick(i);
            }
        });

        //long click to remove something from history
        if(master_filter.equals("history"))
        {
            MasterList.setLongClickable(true);
            MasterList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {
                    historyList.removeAt(pos);
                    SetList();
                    return true;
                }
            });
        }
        else
        {
            MasterList.setLongClickable(false);
        }

        return;
    }

    /*masterListClick() will play a track OR create a sublist depending on the contents of masterList*/
    public void masterListClick(int index)
    {
        if(master_filter.equals("artist") || master_filter.equals("album"))
        {
            //create a sublist based on ARTIST
            DataNode selected = displayList.get(index);
            createSubList(selected.data);
            masterListIndex = index;
        }
        else if(master_filter.equals("track") || master_filter.equals("history"))
        {playTrack(index);}

        return;
    }

    /*createSubList() will reset the values of masterList based on ALBUM or ARTIST*/
    public void createSubList(String arg)
    {
        int i = 0;
        int len = 0;

        String compare = "";
        String headerText = "";
        String title = "";
        String subtitle = "";

        DataNode dn;
        DataNode selected;

        GenericDisplay gd;

        //display is how we convert subList into musicAdapter
        ArrayList<GenericDisplay> display = new ArrayList<>();
        //reset sublist
        subList = new DisplayList();

        //set the header to the current arg
        TextView header = findViewById(R.id.headerText);

        //if arg is stupid long, abbreviate that bad boy
        headerText = arg;
        if(headerText.length() > 27)
        {headerText = headerText.substring(0,27) + "...";}
        header.setText(headerText);

        displaying_sublist = true;

        //update UI
        showHeader();
        hideButtons();

        //for all songs on SD card...
        for(i=0; i<songList.size();i++)
        {
            if(master_filter.equals("artist"))
            {compare = songList.get(i).getArtist();}
            else if(master_filter.equals("album"))
            {compare = songList.get(i).getAlbum();}

            //we found a match! song has the same artist/album that we're searching for
            if(compare.equals(arg))
            {
                dn = new DataNode();
                dn.data = songList.get(i).getTitle();
                dn.subData = songList.get(i).getDuration();
                dn.location = songList.get(i).getLocation();
                dn.duration = songList.get(i).getDuration();
                dn.artist = songList.get(i).getArtist();

                subList.addToEnd(dn);
            }
        }

        len = subList.getLength();

        //for each value in sublist...
        for(i=0; i<len; i++)
        {
            //update display arrayList
            dn = subList.get(i);
            title = dn.data;
            subtitle = dn.subData;
            subtitle = millisecondsToHours(subtitle);
            gd = new GenericDisplay(title,subtitle);
            display.add(gd);
        }

        //change masterList based on display arrayList
        MasterList.setAdapter(new MusicAdapter(this,display));

        MasterList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                playTrack(i);
            }
        });

        MasterList.setLongClickable(false);

        return;
    }

    /*generateSearchList() changes the searchList based on what was entered in search_field*/
    public void generateSearchList(String search)
    {
        int i = 0;
        boolean found_match = false;

        searchList = new DisplayList();
        //arrayList for musicAdapter
        ArrayList<GenericDisplay> searchDisplay = new ArrayList<>();
        GenericDisplay gd = null;

        DataNode first = null;
        DataNode dn = null;

        SongData song = null;

        //don't search for an empty string
        if(search.equals(""))
        {MasterList.setAdapter(new MusicAdapter(this,searchDisplay)); return;}

        //we need to put a useless hidden value at the START of our list...for UI purposes
        gd = new GenericDisplay("ignore","ignore");
        searchDisplay.add(gd);

        //for each song in songList...
        for(i=0; i<songList.size(); i++)
        {
            //grab each song from the SD card
            song = songList.get(i);

            //check if the song TITLE contains our search query
            found_match = song.containsString(search);
            if(found_match==true) {
                //we found a match!
                dn = new DataNode();
                dn.data = song.getTitle();
                dn.subData = song.getArtist();
                dn.artist = song.getArtist();
                dn.location = song.getLocation();
                dn.duration = song.getDuration();
                dn.count = 1;
                dn.next = null;
                //add to searchList
                searchList.addToEnd(dn);
                //add to display arrayList
                gd = new GenericDisplay(dn.data, dn.subData);
                searchDisplay.add(gd);
            }
        }

        //update masterList with songs containing our string search
        MasterList.setAdapter(new MusicAdapter(this,searchDisplay));

        MasterList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                playTrack(i);
            }
        });

        return;
    }

    /*showSearchBar() takes the searchbar out of hiding*/
    public void showSearchBar()
    {
        displaying_searchbar = true;

        View divider = findViewById(R.id.topNavDivider);
        ConstraintLayout searchBarLayout = findViewById(R.id.searchBarHeader);

        //show searchbar
        searchBarLayout.setAlpha(1);
        divider.setAlpha(1);
        //enable editText
        EditText searchField = findViewById(R.id.searchField);
        searchField.setEnabled(true);
        searchBarLayout.bringToFront();

        //restore masterList
        generateSearchList(searchField.getText().toString());

        //add on event listener to editText
        searchField.addTextChangedListener(new TextWatcher(){
            public void afterTextChanged(Editable s) {}
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchText = s.toString();
                if(count>0)
                {generateSearchList(searchText);}
            }
        });

        return;
    }

    /*hideSearchBar() removes the search bar from the UI*/
    public void hideSearchBar(int arg)
    {
        displaying_searchbar = false;

        View divider = findViewById(R.id.topNavDivider);
        ConstraintLayout searchBarLayout = findViewById(R.id.searchBarHeader);

        //make searchbar invivisible
        searchBarLayout = findViewById(R.id.searchBarHeader);
        searchBarLayout.setAlpha(0);
        divider.setAlpha(0);
        //editText disable
        EditText searchField = findViewById(R.id.searchField);
        searchField.setEnabled(false);

        ListView masterList = findViewById(R.id.MasterList);
        masterList.bringToFront();

        return;
    }

    /*showHeader() displays the subList title and a back button*/
    public void showHeader()
    {
        ConstraintLayout masterLayout = findViewById(R.id.master);
        ConstraintLayout header = findViewById(R.id.SublistHeader);

        int top = masterLayout.getTop();
        int left = header.getLeft();

        TranslateAnimation slide = new TranslateAnimation(left, left, top-300, top);
        slide.setDuration(200);
        slide.setInterpolator(new AccelerateInterpolator());

        header.setAlpha(1);
        header.startAnimation(slide);

        ImageView back = findViewById(R.id.backButton);
        back.setClickable(true);

        return;
    }

    /*hideHeader() displays the regular set of buttons and hides the subList header*/
    public void hideHeader(int arg)
    {
        Animation alpha;
        ConstraintLayout cl = findViewById(R.id.SublistHeader);
        //this method is called ONCE during initialization...special case
        if(arg==-1)
        {
            //fast transition fade out
            alpha = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fast_fade);
        }
        else
        {
            alpha = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.slow_fade);
        }

        alpha.setAnimationListener(new Animation.AnimationListener()
                                   {
                                       @Override
                                       public void onAnimationStart(Animation animation)
                                       {/**/}

                                       //disable our buttons and HIDE the layout when animation finishes
                                       @Override
                                       public void onAnimationEnd(Animation animation)
                                       {
                                           ConstraintLayout topnav = findViewById(R.id.SublistHeader);
                                           topnav.setAlpha(0);
                                           ImageView back = findViewById(R.id.backButton);
                                           back.setClickable(false);
                                       }

                                       @Override
                                       public void onAnimationRepeat(Animation animation)
                                       {/**/}
                                   }
        );

        cl.startAnimation(alpha);

        return;
    }

    /*showButtons() will display the default set of buttons on the top of our UI*/
    public void showButtons()
    {
        ConstraintLayout masterLayout = findViewById(R.id.master);
        ConstraintLayout buttons = findViewById(R.id.buttons);

        int top = masterLayout.getTop();
        int left = buttons.getLeft();

        TranslateAnimation slide = new TranslateAnimation(left, left, top-300, top);
        slide.setDuration(200);
        slide.setInterpolator(new AccelerateInterpolator());


        buttons.setAlpha(1);
        buttons.startAnimation(slide);

        artistBtn.setClickable(true);
        trackBtn.setClickable(true);
        albumBtn.setClickable(true);
        historyBtn.setClickable(true);

        return;
    }

    /*hideButtons() removes the default set of buttons from the top of our UI*/
    public void hideButtons()
    {
        Animation alpha = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.slow_fade);

        ConstraintLayout buttons = findViewById(R.id.buttons);

        alpha.setAnimationListener(new Animation.AnimationListener()
                                   {
                                       @Override
                                       public void onAnimationStart(Animation animation)
                                       {/**/}

                                       //disable our buttons and HIDE the layout when animation finishes
                                       @Override
                                       public void onAnimationEnd(Animation animation)
                                       {
                                           ConstraintLayout buttons = findViewById(R.id.buttons);
                                           buttons.setAlpha(0);
                                           artistBtn.setClickable(false);
                                           trackBtn.setClickable(false);
                                           albumBtn.setClickable(false);
                                           historyBtn.setClickable(false);
                                       }

                                       @Override
                                       public void onAnimationRepeat(Animation animation)
                                       {/**/}
                                   }
        );

        buttons.startAnimation(alpha);

        return;
    }

    /*openStorage() asks the user for permission to read from SD card. Called once on initialize()*/
    public void openStorage()
    {
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE))
            {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},PERMISSION_REQUEST);
            }
            else
            {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},PERMISSION_REQUEST);
            }
        }
        else
        {
            SetList();
        }
    }

    /*getMusic() fills songList with every audio file we can find on our SD card*/
    public void getMusic()
    {
        int songTitle = 0;
        int songArtist = 0;
        int songAlbum = 0;
        int songLocation = 0;
        int songDuration = 0;
        String fullstr = "";

        songList = new ArrayList<SongData>();

        SongData sd;

        ContentResolver contentResolver = getContentResolver();
        Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor songCursor = contentResolver.query(songUri,null,null,null,null);

        if(songCursor != null && songCursor.moveToFirst())
        {
            songTitle = songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            songArtist = songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            songAlbum = songCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            songLocation = songCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            songDuration = songCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
        }

        do {
            String currentTitle = songCursor.getString(songTitle);
            String currentArtist = songCursor.getString(songArtist);
            String currentAlbum = songCursor.getString(songAlbum);
            String currentLocation = songCursor.getString(songLocation);
            String currentDuration = songCursor.getString(songDuration);

            //fullstr = "TITLE: " + currentTitle + "\n" + "ARTIST: " + currentArtist + "\n" + "ALBUM: " + currentAlbum + "\n" + "LOCATION: " + currentLocation;

            sd = new SongData(currentTitle,currentArtist,currentAlbum,currentLocation,currentDuration);
            songList.add(sd);
        } while(songCursor.moveToNext());

        //androidstudio told me to free this after I was done
        songCursor.close();

        return;
    }

    //we need permission to read and write to external storage - this is critical to creating and maintaining our history list
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        switch(requestCode)
        {
            case PERMISSION_REQUEST:
            {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                    {
                        Toast.makeText(this,"permission granted",Toast.LENGTH_SHORT).show();
                        SetList();
                    }
                }
                else
                {
                    Toast.makeText(this,"no permission", Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
            }
        }
    }

    /*artistClick() is called when the ARTIST button is pressed*/
    public void artistClick(View v)
    {
        //Toast.makeText(this,"Artist button click",Toast.LENGTH_SHORT).show();
        button_selection = 0;
        master_filter="artist";
        adjustButtons(master_filter);

        SetList();
    }

    /*nameClick() is called when the TRACK button is pressed*/
    public void nameClick(View v)
    {
        //Toast.makeText(this,"Name button click",Toast.LENGTH_SHORT).show();
        button_selection = 1;
        master_filter="track";
        adjustButtons(master_filter);

        SetList();
    }

    /*albumClick() is called when the ALBUM button is pressed*/
    public void albumClick(View v)
    {
        //Toast.makeText(this,"Album button click",Toast.LENGTH_SHORT).show();
        button_selection = 2;
        master_filter="album";
        adjustButtons(master_filter);

        SetList();
    }

    /*historyClick() is called when the HISTORY button is pressed*/
    public void historyClick(View v)
    {
        //Toast.makeText(this,"Resume button click",Toast.LENGTH_SHORT).show();
        button_selection = 3;
        master_filter="history";
        adjustButtons(master_filter);

        SetList();
    }

    /*searchClick() is called when the SEARCH button is pressed*/
    public void searchClick(View v)
    {
        master_filter = "search";
        adjustButtons(master_filter);
        button_selection = 4;
        if(displaying_searchbar==false)
        {showSearchBar();}

        return;
    }

    /*skipBackClick() is called when the skipBack button is pressed*/
    public void skipBackClick(View v)
    {
        int i = 0;

        Toast.makeText(MainActivity.this,"skip backward click",Toast.LENGTH_SHORT).show();

        ImageView skip = findViewById(R.id.skipBack);
        Animation backAnimation = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.backward);
        skip.startAnimation(backAnimation);

        songSkipBack();
    }

    /*rewindClick() is called when the rewind button is pressed*/
    public void rewindClick(View v)
    {
        int i = 0;

        ImageView rewind = findViewById(R.id.rewind);
        Animation backAnimation = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.backward);
        rewind.startAnimation(backAnimation);

        songRewind();
    }

    /*pauseClick() is called when the pause button is pressed*/
    public void pauseClick(View v)
    {
        togglePause();
    }

    //submethod so we can call it outside of pausebtn's OnClick() method
    public void togglePause()
    {
        ImageView pause = findViewById(R.id.pause);
        if(paused == false)
        {
            pause.setImageResource(R.drawable.play);
            paused = true;
            songPause();
        }
        else
        {
            pause.setImageResource(R.drawable.pause);
            paused = false;
            songResume();
        }
    }

    /*fastForwardClick() is called when the fastForward button is pressed*/
    public void fastForwardClick(View v)
    {
        int i = 0;

        ImageView fastforward = findViewById(R.id.fastforward);
        Animation forwardAnimation = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.forward);
        fastforward.startAnimation(forwardAnimation);

        songFastForward();
    }

    /*skipForwardClick() is called when the skipForward button is pressed*/
    public void skipForwardClick(View v)
    {
        int i = 0;

        Toast.makeText(MainActivity.this,"skip forward click",Toast.LENGTH_SHORT).show();

        ImageView skip = findViewById(R.id.skipforward);
        Animation forwardAnimation = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.forward);
        skip.startAnimation(forwardAnimation);

        songSkipForward();
    }

    /*shuffleClick() is called when the shuffle button is pressed*/
    public void shuffleClick(View v)
    {
        toggleShuffle();
    }

    //submethod so we can call this outside of UI events
    public void toggleShuffle()
    {
        ImageView shuffle = findViewById(R.id.shuffle);
        if(shuffling == false)
        {
            shuffle.setImageResource(R.drawable.shuffle_on);
            shuffling = true;
            swapHotIndex();
        }
        else
        {
            shuffle.setImageResource(R.drawable.shuffle_off);
            shuffling = false;
            swapHotIndex();
        }
    }

    /*backButtonClick() is called when the BACK button is pressed*/
    public void backButtonClick(View v)
    {
        hideHeader(69);
        showButtons();
        SetList();
        MasterList.setSelection(masterListIndex);
        return;
    }

    /*swapHotIndex() converts the hotlist index to the shuffle index, or vise versa*/
    public void swapHotIndex()
    {
        DisplayList source = new DisplayList();
        DisplayList destination = new DisplayList();
        DataNode playing = null;
        int searchIndex = 0;

        //don't do ANYTHING unless we have a hotlist populated.
        if(song_loaded == false || hotList.getLength() == 0)
        {return;}


        if(shuffling == true)
        {
            //get current index of hotList - set to index of shuffleList
            source.copy(hotList);
            destination.copy(shuffleList);

            playing = source.get(hotIndex);
            if(playing==null){hotIndex = 0; return;}

            hotIndex = destination.find(playing.location);
        }
        else
        {
            //get current index of shuffleList - set to index of hotList
            source.copy(shuffleList);
            destination.copy(hotList);

            playing = source.get(hotIndex);
            if(playing==null){hotIndex = 0; return;}

            hotIndex = destination.find(playing.location);
        }

        //something went wrong...set hotIndex to default
        if(hotIndex==-1)
        {hotIndex = 0;}

        return;
    }

    /*bringToFront() makes sure our default buttons are on the TOP of the UI.*/
    public void bringToFront()
    {
        artistBtn.bringToFront();
        trackBtn.bringToFront();
        albumBtn.bringToFront();
        historyBtn.bringToFront();
        searchBtn.bringToFront();
    }

    /*adjustButtons() changes the text AND background color or our default buttons*/
    public void adjustButtons(String arg)
    {
        if(arg.equals("artist"))
        {
            artistBtn.setBackgroundColor(Color.WHITE);
            artistBtn.setTextColor(Color.BLACK);

            trackBtn.setBackgroundColor(backgroundColor);
            albumBtn.setBackgroundColor(backgroundColor);
            historyBtn.setBackgroundColor(backgroundColor);
            searchBtn.setBackgroundColor(backgroundColor);

            trackBtn.setTextColor(Color.WHITE);
            albumBtn.setTextColor(Color.WHITE);
            historyBtn.setTextColor(Color.WHITE);
            searchBtn.setTextColor(Color.WHITE);
        }
        else if(arg.equals("track"))
        {
            trackBtn.setBackgroundColor(Color.WHITE);
            trackBtn.setTextColor(Color.BLACK);

            artistBtn.setBackgroundColor(backgroundColor);
            albumBtn.setBackgroundColor(backgroundColor);
            historyBtn.setBackgroundColor(backgroundColor);
            searchBtn.setBackgroundColor(backgroundColor);

            artistBtn.setTextColor(Color.WHITE);
            albumBtn.setTextColor(Color.WHITE);
            historyBtn.setTextColor(Color.WHITE);
            searchBtn.setTextColor(Color.WHITE);
        }
        else if(arg.equals("album"))
        {
            albumBtn.setBackgroundColor(Color.WHITE);
            albumBtn.setTextColor(Color.BLACK);

            artistBtn.setBackgroundColor(backgroundColor);
            trackBtn.setBackgroundColor(backgroundColor);
            historyBtn.setBackgroundColor(backgroundColor);
            searchBtn.setBackgroundColor(backgroundColor);

            artistBtn.setTextColor(Color.WHITE);
            trackBtn.setTextColor(Color.WHITE);
            historyBtn.setTextColor(Color.WHITE);
            searchBtn.setTextColor(Color.WHITE);
        }
        else if(arg.equals("history"))
        {
            historyBtn.setBackgroundColor(Color.WHITE);
            historyBtn.setTextColor(Color.BLACK);

            artistBtn.setBackgroundColor(backgroundColor);
            trackBtn.setBackgroundColor(backgroundColor);
            albumBtn.setBackgroundColor(backgroundColor);
            searchBtn.setBackgroundColor(backgroundColor);

            artistBtn.setTextColor(Color.WHITE);
            trackBtn.setTextColor(Color.WHITE);
            albumBtn.setTextColor(Color.WHITE);
            searchBtn.setTextColor(Color.WHITE);
        }
        else if(arg.equals("search"))
        {
            searchBtn.setBackgroundColor(Color.WHITE);
            searchBtn.setTextColor(Color.BLACK);

            artistBtn.setBackgroundColor(backgroundColor);
            trackBtn.setBackgroundColor(backgroundColor);
            albumBtn.setBackgroundColor(backgroundColor);
            historyBtn.setBackgroundColor(backgroundColor);

            artistBtn.setTextColor(Color.WHITE);
            trackBtn.setTextColor(Color.WHITE);
            albumBtn.setTextColor(Color.WHITE);
            historyBtn.setTextColor(Color.WHITE);
        }
    }

    /*millisecondsToHours() is a helper function that turns # of milliseconds into hh:mm:ss format*/
    public String millisecondsToHours(String arg)
    {
        int value = 0;
        int hours = 0;
        int minutes = 0;
        int seconds = 0;
        String outString = "";
        try {
            value = Integer.parseInt(arg);
        } catch(Exception e) {return "--:--";}

        //find our how many hours (if any) are in our string!
        hours = value / 3600000;
        if(hours!=0 && hours < 10)
        {
            outString = "0" + hours + ":";
        }
        else if(hours !=0)
        {
            outString = hours + ":";
        }
        value = value % 3600000;

        //find out the NUMBER OF MINUTES
        minutes = value / 60000;
        if(minutes < 10)
        {
            outString = outString + "0";
        }
        outString = outString + minutes + ":";
        value = value % 60000;

        //finally, get NUMBER OF SECONDS!
        seconds = value / 1000;
        if(seconds < 10)
        {
            outString = outString + "0";
        }
        outString = outString + seconds;

        return outString;
    }

    /*setBottomDefaults() called on initialization - changes UI elements below masterList to their default value*/
    public void setBottomDefaults()
    {
        //this function appears to never be used
        TextView title = findViewById(R.id.navTitle);
        TextView subtitle = findViewById(R.id.navSubtitle);
        TextView startTime = findViewById(R.id.runningTime);
        TextView endTime = findViewById(R.id.maxTime);

        SeekBar bar = findViewById(R.id.progressBar);
        bar.setProgress(0);

        title.setText("-");
        subtitle.setText("-");
        startTime.setText("--:--");
        endTime.setText("--:--");
        return;
    }

    /*initializeSeekBar() makes the progressBar responsive to changes*/
    public void initializeSeekBar()
    {
        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(song_loaded == true)
                {
                    int milliseconds = 0;
                    String runTime = "";
                    String strMilliseconds = "";

                    if(fromUser)
                    {mp.seekTo(progress);}

                    //update RUNTIME
                    TextView tv = findViewById(R.id.runningTime);
                    strMilliseconds = Integer.toString(progress);
                    runTime = millisecondsToHours(strMilliseconds);
                    tv.setText(runTime);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        return;

    }

    /*progressBarUpdate() updates the progress bar every second from an asyncrhonous task*/
    public void progressBarUpdate()
    {
        task.cancel(true);
        task = new MyAsyncTask();
        task.execute();
    }

    /*historyUpdate() updates the history list every five seconds from an asyncrhonous task*/
    public void historyUpdate()
    {
        saveTask.cancel(true);
        saveTask = new SaveTask();
        saveTask.execute();
    }

    //updates PROGRESSBAR every second
    public class MyAsyncTask extends android.os.AsyncTask {
        @Override
        protected Object doInBackground(Object[] objects) {
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    progressBar.setProgress(mp.getCurrentPosition());
                }
            },0,1000); //1 second break!
            return null;
        }
    }


    //updates the HISTORYLIST every 30 seconds
    public class SaveTask extends android.os.AsyncTask{
        @Override
        protected Object doInBackground(Object[] objects) {
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                        updateHistory();
                }
            },0,5000); //5 second break!
            return null;
        }
    }

    //this class detects headphone unplug. Intended to spare the user from humiliation if headphones slip out in public.
    private class MusicIntentReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        if(paused == false)
                        {togglePause();}
                        break;
                }
            }
        }
    }

    /*customNotification launches every time a new song starts playing. Intended to give the user controls when the app is minimized
    OR when the phone is locked*/
    public void customNotification()
    {
        //if only I knew how notifications worked...
        //some small change

        TextView navTitle = findViewById(R.id.navTitle);
        TextView navSubtitle = findViewById(R.id.navSubtitle);

        String track = navTitle.getText().toString();
        String artist = navSubtitle.getText().toString();

        /*
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.music)
                .setContentTitle(track)
                .setContentText(artist)
                .setAutoCancel(true);

        //WE DON'T USE INTENT
        //Intent notificationIntent = new Intent(this, MainActivity.class);
        //PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        //builder.setContentIntent(contentIntent);

        // Add as notification
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
        */

        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager = (NotificationManager) getSystemService(ns);
        RemoteViews notificationView = new RemoteViews(getPackageName(), R.layout.music_controls_notification);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.music).setContent(notificationView);

        //intents get called when 3 buttons are clicked
        Intent skipBackIntent = new Intent("notification_back");
        PendingIntent pendingSkipBackIntent = PendingIntent.getBroadcast(this, 100, skipBackIntent, 0);

        Intent pauseIntent = new Intent("notification_pause");
        PendingIntent pendingPauseIntent = PendingIntent.getBroadcast(this, 100, pauseIntent, 0);

        Intent skipForwardIntent = new Intent("notification_forward");
        PendingIntent pendingSkipForwardIntent = PendingIntent.getBroadcast(this, 100, skipForwardIntent, 0);

        notificationView.setOnClickPendingIntent(R.id.notificationSkipBack, pendingSkipBackIntent);
        notificationView.setOnClickPendingIntent(R.id.notificationPause, pendingPauseIntent);
        notificationView.setOnClickPendingIntent(R.id.notificationSkipForward, pendingSkipForwardIntent);

        notificationManager.notify(1, builder.build());

        return;
    }

    //listen for notification skipBack
    public static class skipBackListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, "Skip forward click", Toast.LENGTH_SHORT).show();
            //songSkipBack();
        }
    }

    //listen for notification pause
    public static class pauseListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, "Pause click", Toast.LENGTH_SHORT).show();
            //togglePause();
        }
    }

    //listen for notification skipForward
    public static class skipForwardListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, "Skip back click", Toast.LENGTH_SHORT).show();
            //songSkipForward();
        }
    }

    /*updateHistory() updates historyList from a background thread every five seconds*/
    public void updateHistory()
    {
        DataNode dn = null;
        int searchIndex = 0;
        int remainingTime = 0;
        int currentTime = 0;

        //no updates unless we're playing something
        if(playing_audio==false || song_loaded == false)
        {return;}

        //grab current node from hotList OR shuffleList
        if(shuffling==true)
        {dn = shuffleList.get(hotIndex);}
        else
        {dn = hotList.get(hotIndex);}

        //can't get currently playing
        if(dn==null)
        {return;}

        searchIndex = historyList.find(dn.location);

        currentTime = mp.getCurrentPosition();
        remainingTime = Integer.parseInt(dn.duration);
        remainingTime = remainingTime - currentTime;

        //less than 5 minutes
        if(remainingTime < 300000)
        {
            //currently exists in list
            if(searchIndex!=-1)
            {
                historyList.removeAt(searchIndex);
                saveHistory();
            }
            return;
        }

        if(searchIndex==-1)
        {
            //not found, insert into FRONT
            dn.subData = Integer.toString(mp.getCurrentPosition());
            historyList.addToFront(dn);
        }
        else
        {
            //already exists in node, update TIME
            historyList.updateTime(searchIndex, currentTime);
        }

        //save historyList to a text file
        saveHistory();

        return;
    }

    /*saveHistory() exists to save historyList to some text file*/
    public void saveHistory()
    {
        //save historyList to history.txt
        int i = 0;
        String pathname = "";
        String line = "";
        DataNode dn = null;

        try {
            pathname = Environment.getExternalStorageDirectory() + "/history.txt";

            File myFile = new File(pathname);
            myFile.createNewFile();

            FileOutputStream outStream = new FileOutputStream(myFile);
            OutputStreamWriter outWriter = new OutputStreamWriter(outStream);

            //convert each node in historyList to a string
            for (i = 0; i < historyList.getLength(); i++) {
                dn = historyList.get(i);
                line = dn.data + ":";
                line = line + dn.subData + ":";
                line = line + dn.location + ":";
                line = line + dn.duration + ":";
                line = line + dn.artist + ":";
                line = line + dn.count + "\n";

                //write line to file
                outWriter.append(line);
            }

            outWriter.close();
            outStream.close();
        }catch(Exception e) {
            String douglas = e.getMessage();
        }

        return;
    }

    /*loadHistory() exists to load historyList from some text file*/
    public void loadHistory()
    {
        //load historyList from history.txt
        DataNode dn = null;
        String pathname = "";
        String line = "";
        String[] split = null;
        boolean nextLine = false;

        historyList = new DisplayList();

        try {
            pathname = Environment.getExternalStorageDirectory() + "/history.txt";

            File myFile = new File(pathname);

            FileInputStream inStream = new FileInputStream(myFile);
            BufferedReader myReader = new BufferedReader(new InputStreamReader(inStream));

            while ((line = myReader.readLine()) != null) {
                split = line.split(":");

                dn = new DataNode();
                dn.data = split[0];
                dn.subData = split[1];
                dn.location = split[2];
                dn.duration = split[3];
                dn.artist = split[4];
                dn.count = 1;

                historyList.addToEnd(dn);
            }

            myReader.close();
        } catch (Exception e) {
            String douglas = e.getMessage();
        }

        return;
    }

}
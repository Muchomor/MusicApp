package lab4zimny.pwr.musicapp;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.MediaController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements MediaController.MediaPlayerControl {
    @BindView(R.id.song_list) ListView songView;
    private ArrayList<Song> songList;

    private MusicService musicService;
    private Intent playIntent;
    private boolean musicBound=false;
    private boolean isShown = false;

    private MusicController controller;
    private boolean paused=false, playbackPaused=false;
    public static final String TITLE_KEY = "title";
    public static final String ID_KEY = "_id";
    public static final String ARTIST_KEY = "artist";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
                return;
            }
        }
        ButterKnife.bind(this);
        initSongList();
        setAdapter();
        setController();
    }

    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
            musicService = binder.getService();
            musicService.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if(playIntent==null){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    private void setAdapter() {
        SongAdapter Adapter = new SongAdapter(this, songList);
        songView.setAdapter(Adapter);
    }

    private void setController() {
        controller = new MusicController(this);
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNextSong();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrevSong();
            }
        });
        controller.setMediaPlayer(this);
        controller.setAnchorView(songView);
        controller.setEnabled(true);
    }

    public void songPicked(View view){
        SongAdapter.ViewHolder holder = (SongAdapter.ViewHolder) view.getTag();
        musicService.setSong(Integer.parseInt(Integer.toString(holder.pos)));

        musicService.playSong();
        if(!isShown) {
            controller.requestFocus();
            controller.show(0);
            isShown=true;
        }
        if(playbackPaused){
            controller.requestFocus();
            playbackPaused=false;
        }
        controller.requestFocus();
    }

    private void playNextSong(){
        musicService.playNext();
        if(playbackPaused){
            controller.requestFocus();
            playbackPaused=false;
        }
        controller.requestFocus();
    }

    private void playPrevSong(){
        musicService.playPrevios();
        if(playbackPaused){
            controller.requestFocus();
            playbackPaused=false;
        }
        controller.requestFocus();
    }

    public ArrayList<Song> getSongList() {
        ArrayList<Song> songs = new ArrayList();
        Cursor musicCursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        if (musicCursor == null || !musicCursor.moveToFirst()) {
            return songs;
        }
        int titleColumn = musicCursor.getColumnIndex(TITLE_KEY);
        int idColumn = musicCursor.getColumnIndex(ID_KEY);
        int artistColumn = musicCursor.getColumnIndex(ARTIST_KEY);
        do {
            long thisId = musicCursor.getLong(idColumn);
            songs.add(new Song(thisId, musicCursor.getString(titleColumn), musicCursor.getString(artistColumn)));
        } while (musicCursor.moveToNext());
        return songs;
    }

    private void initSongList() {
        songList = new ArrayList<>();
        songList = getSongList();
        Collections.sort(songList, new Comparator<Song>(){
            public int compare(Song a, Song b){
                return a.getTitle().compareTo(b.getTitle());
            }
        });
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        if(musicService !=null && musicBound && musicService.isPlaying())
            return musicService.getPosition();
        else return 0;
    }

    @Override
    public int getDuration() {
        if(musicService !=null && musicBound && musicService.isPlaying())
            return musicService.getDuration();
        else return 0;
    }

    @Override
    public boolean isPlaying() {
        if(musicService !=null && musicBound)
            return musicService.isPlaying();
        return false;
    }

    @Override
    public void pause() {
        playbackPaused=true;
        musicService.pausePlayer();
    }

    @Override
    public void seekTo(int pos) {
        musicService.seek(pos);
    }

    @Override
    public void start() {
        musicService.go();
    }

    @Override
    protected void onPause(){
        super.onPause();
        paused=true;
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(paused){
//            setController();
            controller.requestFocus();
            paused=false;
        }
    }

    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicService=null;
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_end:
                stopService(playIntent);
                musicService =null;
                finishAffinity();
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}


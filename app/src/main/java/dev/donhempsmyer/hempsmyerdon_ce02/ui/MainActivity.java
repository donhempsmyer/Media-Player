package dev.donhempsmyer.hempsmyerdon_ce02.ui;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import dev.donhempsmyer.hempsmyerdon_ce02.R;
import dev.donhempsmyer.hempsmyerdon_ce02.service.AudioPlaybackService;

public class MainActivity extends AppCompatActivity implements ServiceConnection {
    private static final String TAG = "MainActivity";

    private AudioPlaybackService audioService;
    private boolean isBound;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            Log.i(TAG, "Launching PlayerFragment");
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new PlayerFragment(), "player")
                    .commitNow();
        }

        // Start service immediately
        Log.i(TAG, "Starting AudioPlaybackService");
        Intent startIntent = new Intent(this, AudioPlaybackService.class);
        androidx.core.content.ContextCompat.startForegroundService(this, startIntent);
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart(), bindService");
        super.onStart();
        Intent bindIntent = new Intent(this, AudioPlaybackService.class);
        bindService(bindIntent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop(), unbindService if bound");
        super.onStop();
        if (isBound) {
            unbindService(this);
            isBound = false;
            audioService = null;
        }
    }

    // ServiceConnection callbacks
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.i(TAG, "onServiceConnected()");
        AudioPlaybackService.AudioServiceBinder binder =
                (AudioPlaybackService.AudioServiceBinder) service;
        audioService = binder.getService();
        isBound = true;

        // Notify fragment after binding
        PlayerFragment fragment = (PlayerFragment) getSupportFragmentManager()
                .findFragmentByTag("player");
        if (fragment != null) {
            Log.i(TAG, "Notifying PlayerFragment that service is bound");
            fragment.onServiceReady(audioService);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.i(TAG, "onServiceDisconnected()");
        isBound = false;
        audioService = null;
    }

    // Exposing the service to the fragment
    @Nullable
    public AudioPlaybackService getAudioService() {
        return audioService;
    }

    public boolean isServiceBound() {
        return isBound;
    }
}
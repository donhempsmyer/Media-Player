package dev.donhempsmyer.hempsmyerdon_ce02.ui;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import dev.donhempsmyer.hempsmyerdon_ce02.R;
import dev.donhempsmyer.hempsmyerdon_ce02.service.AudioPlaybackService;

public class PlayerFragment extends Fragment {
    private static final String TAG = "PlayerFragment";

    private TextView textTrackTitle;
    private ImageButton buttonPlay, buttonPause, buttonStop, buttonNext, buttonPrevious;
    private ImageView imageAlbumArt;

    private AudioPlaybackService audioService;


    private ProgressBar progressBar;
    private TextView textElapsed, textRemaining;
    private final android.os.Handler progressHandler = new android.os.Handler();

    //simple progress bar
    private final Runnable progressTick = new Runnable() {
        @Override public void run() {
            Log.i(TAG, "progressTick()");
            if (audioService != null && isAdded()) {
                int dur = Math.max(0, audioService.getDurationMs());
                int pos = Math.max(0, Math.min(audioService.getPositionMs(), dur));
                if (dur > 0) {
                    int scaled = (int) (pos * 1000L / dur);
                    progressBar.setProgress(scaled);
                    textElapsed.setText(formatTime(pos));
                    textRemaining.setText("-" + formatTime(dur - pos));
                } else {
                    progressBar.setProgress(0);
                    textElapsed.setText("0:00");
                    textRemaining.setText("-0:00");
                }
                progressHandler.postDelayed(this, 500);
            }
        }
    };
    private String formatTime(int ms) {
        int totalSec = ms / 1000;
        int min = totalSec / 60;
        int sec = totalSec % 60;
        return String.format(java.util.Locale.US, "%d:%02d", min, sec);
    }



    private final AudioPlaybackService.OnStateChangedListener stateListener = new AudioPlaybackService.OnStateChangedListener() {
        @Override
        public void onTrackChanged(String trackTitle, int trackIndex) {
            Log.i(TAG, "onTrackChanged() title=" + trackTitle + " index=" + trackIndex);
            if (textTrackTitle != null) textTrackTitle.setText(trackTitle);
            if (imageAlbumArt != null && audioService != null) {
                int resId = audioService.getCurrentAlbumArtResId();
                imageAlbumArt.setImageResource(resId);
                imageAlbumArt.setContentDescription(trackTitle);
                Log.i(TAG, "Album art set (change) res=" + resId);
            }
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            Log.i(TAG, "onIsPlayingChanged() isPlaying=" + isPlaying);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView()");
        View root = inflater.inflate(R.layout.fragment_player, container, false);
        textTrackTitle = root.findViewById(R.id.textTrackTitle);
        buttonPlay = root.findViewById(R.id.buttonPlay);
        buttonPause = root.findViewById(R.id.buttonPause);
        buttonStop = root.findViewById(R.id.buttonStop);
        buttonNext = root.findViewById(R.id.buttonNext);
        buttonPrevious = root.findViewById(R.id.buttonPrevious);
        imageAlbumArt = root.findViewById(R.id.imageAlbumArt);
        progressBar = root.findViewById(R.id.progressBar);
        textElapsed = root.findViewById(R.id.textElapsed);
        textRemaining = root.findViewById(R.id.textRemaining);

        buttonPlay.setOnClickListener(v -> {
            Log.i(TAG, "Play clicked");
            if (audioService != null) audioService.play();
        });
        buttonPause.setOnClickListener(v -> {
            Log.i(TAG, "Pause clicked");
            if (audioService != null) audioService.pause();
        });
        buttonStop.setOnClickListener(v -> {
            Log.i(TAG, "Stop clicked");
            if (audioService != null) audioService.stopPlayback();
        });
        buttonNext.setOnClickListener(v -> {
            Log.i(TAG, "Next clicked");
            if (audioService != null) audioService.next();
        });
        buttonPrevious.setOnClickListener(v -> {
            Log.i(TAG, "Previous clicked");
            if (audioService != null) audioService.previous();
        });

        return root;
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume()");
        super.onResume();
        // Try to attach to service if Activity is already bound
        MainActivity activity = (MainActivity) requireActivity();
        if (activity.isServiceBound() && activity.getAudioService() != null) {
            Log.i(TAG, "onResume: attaching to existing service");
            onServiceReady(activity.getAudioService());
        }
        progressHandler.postDelayed(progressTick, 300);
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause()");
        super.onPause();
        progressHandler.removeCallbacks(progressTick);
        if (audioService != null) {
            Log.i(TAG, "Removing state listener");
            audioService.setOnStateChangedListener(null);
        }
    }

    // Called by Activity immediately after binding completes
    public void onServiceReady(AudioPlaybackService service) {
        Log.i(TAG, "onServiceReady()");
        this.audioService = service;
        audioService.setOnStateChangedListener(stateListener);
        textTrackTitle.setText(audioService.getCurrentTrackTitle());
        progressHandler.removeCallbacks(progressTick);
        progressHandler.post(progressTick);
        if (imageAlbumArt != null) {
            imageAlbumArt.setImageResource(audioService.getCurrentAlbumArtResId());
            imageAlbumArt.setContentDescription(audioService.getCurrentTrackTitle());
            Log.i(TAG, "Album art set (initial) res=" + audioService.getCurrentAlbumArtResId());
        }
    }
}
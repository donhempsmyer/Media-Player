package dev.donhempsmyer.hempsmyerdon_ce02.service;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;

import dev.donhempsmyer.hempsmyerdon_ce02.R;
import dev.donhempsmyer.hempsmyerdon_ce02.ui.MainActivity;

public class AudioPlaybackService extends Service {
    private static final String TAG = "AudioPlaybackService";

    // Notification
    private static final String CHANNEL_ID = "playback_channel";
    private static final int NOTIFICATION_ID = 1001;

    // Actions for notification buttons
    public static final String ACTION_PLAY = "dev.donhempsmyer.media_player.action.PLAY";
    public static final String ACTION_PAUSE = "dev.donhempsmyer.media_player.action.PAUSE";
    public static final String ACTION_STOP = "dev.donhempsmyer.media_player.action.STOP";
    public static final String ACTION_NEXT = "dev.donhempsmyer.media_player.action.NEXT";
    public static final String ACTION_PREV = "dev.donhempsmyer.media_player.action.PREV";


    private MediaSessionCompat mediaSession;


    private final int[] playlist = new int[] {
            R.raw.barry_under_by_uberan_mino_unminus,
            R.raw.d_entreprise_en_feu_by_kevin_shrout_unminus,
            R.raw.elf_beat_by_realtime_project_unminus
    };
    private final String[] playlistTitles = new String[] {
            "Barry Under",
            "D'entreprise en feu",
            "Elf Beat by Realtime Project"
    };

    private final int[] playlistArtResIds = new int[] {
            R.drawable.barry_under_by_uberan_mino_pano,
            R.drawable.dentreprise_en_feu_pano,
            R.drawable.elf_beat_by_realtime_project_unminus_pano
    };

    private int currentIndex = 0;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying;

    private final IBinder binder = new AudioServiceBinder();
    private OnStateChangedListener stateChangedListener;

    public class AudioServiceBinder extends Binder {
        public AudioPlaybackService getService() {
            Log.i(TAG, "AudioServiceBinder.getService()");
            return AudioPlaybackService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate()");
        super.onCreate();
        createNotificationChannel();
        mediaSession = new MediaSessionCompat(this, "AudioSession");
        mediaSession.setActive(true);
        Log.i(TAG, "MediaSession created and active");
        initPlayer();
        updateMediaSessionMetadata();

        updateNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand() intent=" + (intent != null ? intent.getAction() : "null"));
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_PLAY:
                    play();
                    break;
                case ACTION_PAUSE:
                    pause();
                    break;
                case ACTION_STOP:
                    stopPlayback();
                    break;
                case ACTION_NEXT:
                    next();
                    break;
                case ACTION_PREV:
                    previous();
                    break;
            }
        }
        // Keep running until explicitly stopped
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind()");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind()");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        releasePlayer();
        if (mediaSession != null) {
            mediaSession.release();
            Log.i(TAG, "MediaSession released");
            mediaSession = null;
        }
        super.onDestroy();
    }

    public int getCurrentAlbumArtResId() {
        Log.i("AudioPlaybackService", "getCurrentAlbumArtResId() index=" + currentIndex);
        int resId = playlistArtResIds[currentIndex];
        Log.i("AudioPlaybackService", "getCurrentAlbumArtResId() -> " + resId);
        return resId;
    }

    private void updateMediaSessionMetadata() {
        Log.i(TAG, "updateMediaSessionMetadata()");
        Bitmap art = BitmapFactory.decodeResource(getResources(), getCurrentAlbumArtResId());

        MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, getCurrentTrackTitle())
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, art)
                .build();

        mediaSession.setMetadata(metadata);
        Log.i(TAG, "MediaSession metadata set with album art");
    }


    //UI controls
    public void play() {
        Log.i(TAG, "play()");
        if (mediaPlayer == null) {
            initPlayer();
        }
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPlaying = true;
            notifyIsPlayingChanged();
            startForeground(NOTIFICATION_ID, buildNotification());
        } else {
            Log.i(TAG, "play() ignored, already playing");
        }
        updateNotification();
    }

    public void pause() {
        Log.i(TAG, "pause()");
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            notifyIsPlayingChanged();
            // Stay in foreground so notification remains ongoing
            updateNotification();
        }
    }

    public void stopPlayback() {
        Log.i(TAG, "stopPlayback()");
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            prepareCurrent();
            isPlaying = false;
            notifyIsPlayingChanged();
            stopForeground(false);
            updateNotification();
        }
    }

    public void next() {
        Log.i(TAG, "next()");
        currentIndex = (currentIndex + 1) % playlist.length;
        notifyTrackChanged();
        switchToIndexAndPlayIfNeeded();
    }

    public void previous() {
        Log.i(TAG, "previous()");
        currentIndex = (currentIndex - 1 + playlist.length) % playlist.length;
        notifyTrackChanged();
        switchToIndexAndPlayIfNeeded();
    }



    public String getCurrentTrackTitle() {
        String title = playlistTitles[currentIndex];
        Log.i(TAG, "getCurrentTrackTitle() -> " + title);
        return title;
    }



    public void setOnStateChangedListener(@Nullable OnStateChangedListener listener) {
        Log.i(TAG, "setOnStateChangedListener() listener=" + (listener != null));
        this.stateChangedListener = listener;
        if (listener != null) {
            listener.onTrackChanged(getCurrentTrackTitle(), currentIndex);
            listener.onIsPlayingChanged(isPlaying);
        }
    }

    public int getDurationMs() {
        Log.i("AudioPlaybackService", "getDurationMs()");
        return (mediaPlayer != null) ? mediaPlayer.getDuration() : 0;
    }
    public int getPositionMs() {
        Log.i("AudioPlaybackService", "getPositionMs()");
        return (mediaPlayer != null) ? mediaPlayer.getCurrentPosition() : 0;
    }


    private void initPlayer() {
        Log.i(TAG, "initPlayer()");
        releasePlayer();
        mediaPlayer = MediaPlayer.create(this, playlist[currentIndex]);
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
        );
        mediaPlayer.setOnCompletionListener(mp -> {
            Log.i(TAG, "onCompletion, auto next()");
            next();
        });
        isPlaying = false;
        notifyTrackChanged();
    }

    private void prepareCurrent() {
        Log.i(TAG, "prepareCurrent()");
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = MediaPlayer.create(this, playlist[currentIndex]);
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
        );
        mediaPlayer.setOnCompletionListener(mp -> {
            Log.i(TAG, "onCompletion, auto next()");
            next();
        });
    }

    private void switchToIndexAndPlayIfNeeded() {
        Log.i(TAG, "switchToIndexAndPlayIfNeeded() index=" + currentIndex);
        boolean resumePlay = isPlaying;
        prepareCurrent();
        if (resumePlay) {
            play();
        } else {
            updateNotification();
        }
    }

    private void releasePlayer() {
        Log.i(TAG, "releasePlayer()");
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void notifyTrackChanged() {
        Log.i(TAG, "notifyTrackChanged() -> " + getCurrentTrackTitle());
        updateMediaSessionMetadata();
        if (stateChangedListener != null) {
            stateChangedListener.onTrackChanged(getCurrentTrackTitle(), currentIndex);
        }
    }

    private void notifyIsPlayingChanged() {
        Log.i(TAG, "notifyIsPlayingChanged(), " + isPlaying);
        if (stateChangedListener != null) {
            stateChangedListener.onIsPlayingChanged(isPlaying);
        }
    }



    // Notification feature

    private void createNotificationChannel() {
        Log.i(TAG, "createNotificationChannel()");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.channel_desc));
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }

    private PendingIntent mainActivityPendingIntent() {
        Log.i(TAG, "mainActivityPendingIntent()");
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(this, 1, openIntent, PendingIntent.FLAG_IMMUTABLE);
    }

    private PendingIntent actionPendingIntent(String action, int requestCode) {
        Log.i(TAG, "actionPendingIntent() action=" + action);
        Intent intent = new Intent(this, AudioPlaybackService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    private Notification buildNotification() {
        Log.i(TAG, "buildNotification() without MediaStyle");

        NotificationCompat.Action actionPrev = new NotificationCompat.Action(
                android.R.drawable.ic_media_previous, "Previous",
                actionPendingIntent(ACTION_PREV, 101));

        NotificationCompat.Action actionPlay = new NotificationCompat.Action(
                android.R.drawable.ic_media_play, "Play",
                actionPendingIntent(ACTION_PLAY, 102));

        NotificationCompat.Action actionPause = new NotificationCompat.Action(
                android.R.drawable.ic_media_pause, "Pause",
                actionPendingIntent(ACTION_PAUSE, 103));

        NotificationCompat.Action actionStop = new NotificationCompat.Action(
                android.R.drawable.ic_menu_close_clear_cancel, "Stop",
                actionPendingIntent(ACTION_STOP, 104));

        NotificationCompat.Action actionNext = new NotificationCompat.Action(
                android.R.drawable.ic_media_next, "Next",
                actionPendingIntent(ACTION_NEXT, 105));

        Bitmap art = loadAlbumArtBitmap();

        String status = isPlaying ? "Playing" : "Paused";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getCurrentTrackTitle())
                .setContentText(status)
                .setContentIntent(mainActivityPendingIntent())
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setLargeIcon(art)
                .addAction(actionPrev)
                .addAction(isPlaying ? actionPause : actionPlay)
                .addAction(actionNext);

        NotificationCompat.BigPictureStyle big =
                new NotificationCompat.BigPictureStyle()
                        .bigPicture(art)
                        .setBigContentTitle(getCurrentTrackTitle())
                        .setSummaryText(status)
                        .bigLargeIcon((Bitmap) null);

        builder.setStyle(big);

        return builder.build();
    }

    private Bitmap loadAlbumArtBitmap() {
        Log.i(TAG, "loadAlbumArtBitmap()");
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), getCurrentAlbumArtResId());
        if (bmp == null) {
            Log.i(TAG, "loadAlbumArtBitmap() ic_launcher fallback");
            return BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        }
        final int max = 1024;
        int w = bmp.getWidth(), h = bmp.getHeight();
        if (w > max || h > max) {
            float scale = Math.min(max / (float) w, max / (float) h);
            int nw = Math.max(1, Math.round(w * scale));
            int nh = Math.max(1, Math.round(h * scale));
            Log.i(TAG, "loadAlbumArtBitmap() scaling " + w + "x" + h + " -> " + nw + "x" + nh);
            bmp = Bitmap.createScaledBitmap(bmp, nw, nh, true);
        }
        return bmp;
    }

    private void updateNotification() {
        Log.i(TAG, "updateNotification()");
        Notification notification = buildNotification();
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (isPlaying) {
            Log.i(TAG, "updateNotification(): startForeground");
            startForeground(NOTIFICATION_ID, notification);
        } else {
            Log.i(TAG, "updateNotification(): notify (not foreground)");
            nm.notify(NOTIFICATION_ID, notification);
        }
    }

    // state listener

    public interface OnStateChangedListener {
        void onTrackChanged(String trackTitle, int trackIndex);
        void onIsPlayingChanged(boolean isPlaying);
    }
}

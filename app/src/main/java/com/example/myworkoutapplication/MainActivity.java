package com.example.myworkoutapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

public class MainActivity extends AppCompatActivity {

    private EditText workoutDurationEditText;
    private EditText restDurationEditText;
    private TextView workoutTimerTextView;
    private TextView restTimerTextView;
    private ProgressBar progressBar;
    private Button startButton;
    private Button stopButton;
    private long workoutDuration = 0;
    private long restDuration = 0;
    private long workoutElapsedTime = 0;
    private long restElapsedTime = 0;

    private CountDownTimer workoutTimer;
    private CountDownTimer restTimer;
    private boolean isWorkoutPhase = true;
    private boolean isTimerRunning = false;

    private Handler handler;
    private Runnable runnable;

    private MediaPlayer mediaPlayer;
    private static final String CHANNEL_ID = "workout_channel";
    private static final String CHANNEL_NAME = "Workout Channel";
    private static final String CHANNEL_DESCRIPTION = "Channel for workout notifications";
    private static final int PERMISSION_REQUEST_NOTIFICATION_POLICY = 1;

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_DESCRIPTION);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the notification channel (if running on Android Oreo or later)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
//
//        // Check if the app has the required permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NOTIFICATION_POLICY) != PackageManager.PERMISSION_GRANTED) {

//            // Request the permission if it hasn't been granted yet
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY}, PERMISSION_REQUEST_NOTIFICATION_POLICY);
        }



        // Initialize UI components
        workoutDurationEditText = findViewById(R.id.workout_duration_edittext);
        restDurationEditText = findViewById(R.id.rest_duration_edittext);
        workoutTimerTextView = findViewById(R.id.workout_timer_textview);
        restTimerTextView = findViewById(R.id.rest_timer_textview);
        progressBar = findViewById(R.id.progress_bar);
        startButton = findViewById(R.id.start_button);
        stopButton = findViewById(R.id.stop_button);

        // Initialize Handler and Runnable to update UI periodically
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                updateUI();
                handler.postDelayed(this, 1000);
            }
        };

        // Initialize MediaPlayer to play a sound when each phase ends
        mediaPlayer = MediaPlayer.create(this, R.raw.beep);

        // Set listeners for start and stop buttons
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTimers();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTimer();
            }
        });
    }

    private void startTimers() {
        // Get the workout and rest durations from the EditTexts and convert to milliseconds
        workoutDuration = Long.parseLong(workoutDurationEditText.getText().toString()) * 1000;
        restDuration = Long.parseLong(restDurationEditText.getText().toString()) * 1000;

        // Create a new CountDownTimer for the workout phase
        workoutTimer = new CountDownTimer(workoutDuration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                workoutElapsedTime = workoutDuration - millisUntilFinished;
//                if (workoutElapsedTime == 0) {
//                    showNotification();
//                }
                updateUI();
            }

            @Override
            public void onFinish() {
                isWorkoutPhase = false;
                mediaPlayer.start();
               // showNotification();
                restElapsedTime = 0; // Reset the elapsed time for the rest phase
                restTimer.start();
            }
        };

        // Create a new CountDownTimer for the rest phase
        restTimer = new CountDownTimer(restDuration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                restElapsedTime = restDuration - millisUntilFinished;
                updateUI();
            }

            @Override
            public void onFinish() {
                mediaPlayer.start();
                showNotification();

                if (isWorkoutPhase) {
                    isWorkoutPhase = false;
                    restElapsedTime = 0;
                    restTimer.start();
                    workoutTimerTextView.setText("Rest Time Remaining: ");
                    restTimerTextView.setText("Workout Time...");
                } else {
                    isWorkoutPhase = true;
                    workoutElapsedTime = 0;
                    workoutTimerTextView.setText("Workout Time Remaining: ");
                    restTimerTextView.setText("Rest Time...");
                }

                isTimerRunning = false;
                workoutTimer.cancel();
                restTimer.cancel();
            }

        };

        // Start the workout timer
        isWorkoutPhase = true;
        workoutTimer.start();
        isTimerRunning = true;
        workoutTimerTextView.setText("Workout Time Remaining: ");
        restTimerTextView.setText("Rest Time Remaining: ");
    }


    private void stopTimer() {
        if (isTimerRunning) {
            // Cancel the CountDownTimers and reset the UI
            workoutTimer.cancel();
            restTimer.cancel();
            handler.removeCallbacks(runnable);
            isTimerRunning = false;
            isWorkoutPhase = true;
            workoutTimerTextView.setText("Workout Time Remaining: ");
            restTimerTextView.setText("Rest Time Remaining: ");
            progressBar.setProgress(0);
        }
    }

    private String formatTime(long millis) {
        int minutes = (int) (millis / 1000) / 60;
        int seconds = (int) (millis / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void updateUI() {
        if (isWorkoutPhase) {
            long remainingTime = workoutDuration - workoutElapsedTime;
            progressBar.setProgress((int) (remainingTime / (float) workoutDuration * 100));
            workoutTimerTextView.setText("Workout Time Remaining: " + formatTime(remainingTime));
        } else {
            long remainingTime = restDuration - restElapsedTime;
            progressBar.setProgress((int) (remainingTime / (float) restDuration * 100));
            restTimerTextView.setText("Rest Time Remaining: " + formatTime(remainingTime));

        }
    }

    private void showNotification() {
        // Create an intent to launch when the notification is clicked
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // Create a notification using the NotificationCompat.Builder class
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("My Workout App")
                .setContentText("Your workout is complete!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Show the notification using the NotificationManager
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainActivity.this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NOTIFICATION_POLICY) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(0, builder.build());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}

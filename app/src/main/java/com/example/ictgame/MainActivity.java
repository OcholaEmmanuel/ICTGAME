package com.example.ictgame;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private int score = 0;
    private int highScore = 0;
    private int lastScore = 0;

    private TextView scoreText;
    private TextView highScoreText;
    private TextView lastScoreText;
    private TextView timerText;
    private TextView messageTextView;
    private Button startButton;

    private boolean gameRunning = false;
    private float originalX;
    private float originalY;
    private boolean positionCaptured = false;

    private CountDownTimer gameTimer;
    private static final String PREFS_NAME = "GamePrefs";
    private static final String HIGH_SCORE_KEY = "HighScore";
    private static final String TAG = "MainActivity";
    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        highScoreText = findViewById(R.id.highScoreText);
        lastScoreText = findViewById(R.id.lastScoreText);
        scoreText = findViewById(R.id.scoreText);
        timerText = findViewById(R.id.timerText);
        startButton = findViewById(R.id.startButton);
        messageTextView = findViewById(R.id.messageTextView);

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        highScore = preferences.getInt(HIGH_SCORE_KEY, 0);
        highScoreText.setText("High Score: " + highScore);

        startButton.post(() -> {
            if (!positionCaptured) {
                originalX = startButton.getX();
                originalY = startButton.getY();
                positionCaptured = true;
            }
        });

        startButton.setOnClickListener(v -> {
            if (!gameRunning) {
                startGame();
            } else {
                score++;
                scoreText.setText("Score: " + score);
                moveButtonRandomly();
            }
        });
    }

    private void startGame() {
        score = 0;
        gameRunning = true;
        startButton.setText("Tap Me!");
        scoreText.setText("Score: 0");
        messageTextView.setVisibility(View.GONE);

        gameTimer = new CountDownTimer(20000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerText.setText("Time: " + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                endGame();
            }
        }.start();
    }

    private void endGame() {
        gameRunning = false;
        timerText.setText("Time's up!");
        resetButtonPosition();

        lastScore = score;
        lastScoreText.setText("Last Score: " + lastScore);
        updateHighScore();

        scoreText.setText("Final Score: " + score);
        showScoreMessage(score);

        startButton.setEnabled(false);
        new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {}

            @Override
            public void onFinish() {
                startButton.setEnabled(true);
                startButton.setText("Start");
            }
        }.start();
    }

    private void showScoreMessage(int currentScore) {
        String message;

        if (currentScore < 30) {
            message = "Failed! ❌";
            messageTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        } else if (currentScore > 50) {
            message = "Good Challenge! 👍 Entering Draft...";
            messageTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_light));

            Intent intent = new Intent(MainActivity.this, DraftActivity.class);
            startActivity(intent);
        } else if (currentScore > highScore) {
            message = "Top Notch! 🌟";
            messageTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light));
        } else {
            message = "Well Done!";
            messageTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark));
        }

        messageTextView.setText(message);
        messageTextView.setVisibility(View.VISIBLE);
    }

    private void moveButtonRandomly() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int maxX = metrics.widthPixels - startButton.getWidth() - 32;
        int maxY = metrics.heightPixels - startButton.getHeight() - 100;

        if (maxX > 0 && maxY > 0) {
            float randomX = random.nextInt(maxX);
            float randomY = Math.max(100, random.nextInt(maxY));
            startButton.animate().x(randomX).y(randomY).setDuration(150).start();
        }
    }

    private void resetButtonPosition() {
        startButton.animate().x(originalX).y(originalY).setDuration(300).start();
    }

    private void updateHighScore() {
        if (score > highScore) {
            highScore = score;
            highScoreText.setText("High Score: " + highScore);

            SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            preferences.edit().putInt(HIGH_SCORE_KEY, highScore).apply();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameTimer != null) {
            gameTimer.cancel();
        }
    }
}
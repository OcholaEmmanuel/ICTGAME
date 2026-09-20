package com.example.ictgame;

import android.content.ClipData;
import android.content.ClipDescription;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DraftActivity extends AppCompatActivity {

    private GridLayout draftGrid;
    private TextView turnIndicator;
    private TextView timerText;
    private TextView player1ScoreText;
    private TextView player2ScoreText;
    private Button resetGameButton;

    private boolean isPlayer1Turn = true;
    private final int[][] board = new int[9][6]; // 9 rows x 6 columns
    private int player1Score = 0;
    private int player2Score = 0;

    private CountDownTimer turnTimer;
    private static final long TURN_DURATION = 20000; // 20 Seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draft);

        draftGrid = findViewById(R.id.draftGrid);
        turnIndicator = findViewById(R.id.turnIndicator);
        timerText = findViewById(R.id.timerText);
        player1ScoreText = findViewById(R.id.player1ScoreText);
        player2ScoreText = findViewById(R.id.player2ScoreText);
        resetGameButton = findViewById(R.id.resetGameButton);

        Uri data = getIntent().getData();
        if (data != null) {
            String path = data.getPath();
            Log.d("DraftActivity", "Opened via Deep Link path: " + path);
        }

        resetGameButton.setOnClickListener(v -> resetGame());

        setupBoard();
        startTurnTimer();
    }

    private void setupBoard() {
        draftGrid.removeAllViews();
        draftGrid.setRowCount(9);
        draftGrid.setColumnCount(6);

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 6; j++) {
                ImageView cell = new ImageView(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 110;
                params.height = 110;
                params.setMargins(2, 2, 2, 2);
                cell.setLayoutParams(params);

                if (i < 3) {
                    cell.setImageResource(R.drawable.download11);
                    cell.setTag("Player 1");
                    board[i][j] = 1;
                } else if (i >= 6) {
                    cell.setImageResource(R.drawable.download);
                    cell.setTag("Player 2");
                    board[i][j] = 2;
                } else {
                    cell.setImageResource(R.drawable.ic_launcher_background);
                    cell.setTag("empty");
                    board[i][j] = 0;
                }

                cell.setOnTouchListener(new CellTouchListener());
                cell.setOnDragListener(new CellDragListener(i, j));

                draftGrid.addView(cell);
            }
        }
    }

    private void startTurnTimer() {
        if (turnTimer != null) turnTimer.cancel();

        turnTimer = new CountDownTimer(TURN_DURATION, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerText.setText("Time: " + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                switchTurn();
            }
        }.start();
    }

    private void switchTurn() {
        isPlayer1Turn = !isPlayer1Turn;
        turnIndicator.setText(isPlayer1Turn ? "Player 1's Turn" : "Player 2's Turn");
        startTurnTimer();
    }

    private void resetGame() {
        player1Score = 0;
        player2Score = 0;
        isPlayer1Turn = true;
        player1ScoreText.setText("Player 1: 0");
        player2ScoreText.setText("Player 2: 0");
        turnIndicator.setText("Player 1's Turn");
        setupBoard();
        startTurnTimer();
    }

    private class CellTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                String tag = (String) view.getTag();
                if ((isPlayer1Turn && "Player 1".equals(tag)) || (!isPlayer1Turn && "Player 2".equals(tag))) {
                    ClipData.Item item = new ClipData.Item((CharSequence) view.getTag());
                    ClipData dragData = new ClipData(
                            (CharSequence) view.getTag(),
                            new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN},
                            item
                    );
                    View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                    view.startDragAndDrop(dragData, shadowBuilder, view, 0);
                    return true;
                }
            }
            return false;
        }
    }

    private class CellDragListener implements View.OnDragListener {
        private final int row, col;

        public CellDragListener(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public boolean onDrag(View v, DragEvent event) {
            if (event.getAction() == DragEvent.ACTION_DROP) {
                String targetTag = (String) v.getTag();
                if (!"empty".equals(targetTag)) return false;

                ImageView source = (ImageView) event.getLocalState();
                int sourceRow = -1, sourceCol = -1;

                for (int i = 0; i < 9; i++) {
                    for (int j = 0; j < 6; j++) {
                        if (draftGrid.getChildAt(i * 6 + j) == source) {
                            sourceRow = i;
                            sourceCol = j;
                            break;
                        }
                    }
                }

                if (sourceRow == -1 || sourceCol == -1) return false;

                int direction = isPlayer1Turn ? 1 : -1;

                // Move forward 1 row
                if (row == sourceRow + direction && col == sourceCol) {
                    movePiece(source, v, sourceRow, sourceCol);
                    switchTurn();
                    return true;
                }
                else if (row == sourceRow + (2 * direction) && col == sourceCol) {
                    int capturedRow = sourceRow + direction;
                    int opponent = isPlayer1Turn ? 2 : 1;

                    if (board[capturedRow][col] == opponent) {
                        movePiece(source, v, sourceRow, sourceCol);
                        board[capturedRow][col] = 0;

                        ImageView capturedCell = (ImageView) draftGrid.getChildAt(capturedRow * 6 + col);
                        capturedCell.setImageResource(R.drawable.ic_launcher_background);
                        capturedCell.setTag("empty");

                        if (isPlayer1Turn) {
                            player1Score++;
                            player1ScoreText.setText("Player 1: " + player1Score);
                        } else {
                            player2Score++;
                            player2ScoreText.setText("Player 2: " + player2Score);
                        }

                        switchTurn();
                        return true;
                    }
                }
                return false;
            }
            return true;
        }

        private void movePiece(ImageView source, View target, int sourceRow, int sourceCol) {
            board[row][col] = isPlayer1Turn ? 1 : 2;
            board[sourceRow][sourceCol] = 0;

            ((ImageView) target).setImageResource(isPlayer1Turn ? R.drawable.download11 : R.drawable.download);
            target.setTag(isPlayer1Turn ? "Player 1" : "Player 2");

            source.setImageResource(R.drawable.ic_launcher_background);
            source.setTag("empty");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (turnTimer != null) {
            turnTimer.cancel();
        }
    }
}
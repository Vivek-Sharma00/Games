import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class FollingBlock extends JPanel implements ActionListener, KeyListener {
 
    // Game Settings
    private final int WIDTH = 600;
    private final int HEIGHT = 400;

    // Player State
    private int playerX = 250;
    private int playerY = 300;
    private final int PLAYER_SIZE = 30;

    // Enemy State
    private int enemyX = 0;
    private int enemyY = 0;
    private final int ENEMY_SIZE = 40;
    private float enemySpeed = 5;

    // Game Logic
    private Timer timer;
    private boolean isGameOver = false;
    private int score = 0;

    public FollingBlock() {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.addKeyListener(this);

        // The Game Loop: Runs every 10ms
        timer = new Timer(10, this);
        timer.start();

        // Spawn first enemy
        resetEnemy();
    }

    // --- 1. The "Update" Phase (Logic) ---
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isGameOver) {
            // Move Enemy
            enemyY += enemySpeed;

            // Check if enemy hit bottom
            if (enemyY > HEIGHT) {
                resetEnemy();
                score++;
                enemySpeed++; // Increase difficulty
            }

            // Check Collision
            if (new Rectangle(playerX, playerY, PLAYER_SIZE, PLAYER_SIZE)
                    .intersects(new Rectangle(enemyX, enemyY, ENEMY_SIZE, ENEMY_SIZE))) {
                isGameOver = true;
                timer.stop();
            }
        }

        // Redraw screen
        repaint();
    }

    // --- 2. The "Render" Phase (Drawing) ---
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw Player (Green)
        g.setColor(Color.GREEN);
        g.fillRect(playerX, playerY, PLAYER_SIZE, PLAYER_SIZE);

        // Draw Enemy (Red)
        g.setColor(Color.RED);
        g.fillRect(enemyX, enemyY, ENEMY_SIZE, ENEMY_SIZE);

        // Draw Text
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Score: " + score, 10, 25);

        if (isGameOver) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.drawString("GAME OVER", WIDTH / 2 - 120, HEIGHT / 2);
        }
    }

    // Helper: Reset enemy to random top position
    private void resetEnemy() {
        enemyY = -50;
        enemyX = (int) (Math.random() * (WIDTH - ENEMY_SIZE));
    }

    // --- 3. Input Handling ---
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT && playerX > 0) {
            playerX -= 20;
        }
        if (key == KeyEvent.VK_RIGHT && playerX < WIDTH - PLAYER_SIZE) {
            playerX += 20;
        }
        // Restart logic
        if (isGameOver && key == KeyEvent.VK_ENTER) {
            isGameOver = false;
            score = 0;
            enemySpeed = 5;
            resetEnemy();
            timer.start();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    // Main entry point
    public static void main(String[] args) {
        JFrame frame = new JFrame("Simple Java Game");
        FollingBlock game = new FollingBlock();

        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
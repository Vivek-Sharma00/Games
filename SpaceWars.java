import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SpaceWars extends JFrame {
    public SpaceWars() {
        setTitle("Space Wars");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        
        GamePanel gamePanel = new GamePanel();
        add(gamePanel);
        pack();
        
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SpaceWars().setVisible(true);
        });
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    // Virtual resolution rules
    private static final int V_WIDTH = 800;
    private static final int V_HEIGHT = 500;
    
    // Game States
    private enum State { PLAYING, GAME_OVER }
    private State currentState = State.PLAYING;
    
    // Core Loop & 90 FPS Target Timing
    private final Timer gameTimer;
    private long lastTime;
    
    // Scoring
    private int score = 0;
    private int highScore = 0;
    
    // Gameplay Entities
    private double shipY = V_HEIGHT / 2.0;
    private final double shipWidth = 40;
    private final double shipHeight = 25;
    private final double shipSpeed = 300.0; // Pixels per second
    
    // Movement inputs
    private boolean moveUp = false;
    private boolean moveDown = false;
    
    // Parallax Starfield
    private final double[] starsX = new double[50];
    private final double[] starsY = new double[50];
    private final double[] starsSpeed = new double[50];
    
    // Lists for bullets and enemies
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    
    // Systems Timers & Resources
    private double ammoTimer = 0.0;
    private int ammo = 3;
    private final int MAX_AMMO = 3;
    
    private double bombTimer = 0.0;
    private int bombCount = 0;
    private int bombsUsed = 0;
    
    private double enemySpawnTimer = 0.0;
    private double survivalTime = 0.0;
    
    // Visual Bomb Flash Animation Tracker
    private boolean bombActive = false;
    private double bombRadius = 0.0;
    private final double maxBombRadius = Math.max(V_WIDTH, V_HEIGHT) * 0.9;
    
    // UI Restart Button Bounds
    private final Rectangle restartButtonRect = new Rectangle(V_WIDTH / 2 - 60, V_HEIGHT / 2 + 40, 120, 40);

    public GamePanel() {
        setPreferredSize(new Dimension(V_WIDTH, V_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        
        // Mouse listener purely for the Game Over restart button click
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentState == State.GAME_OVER) {
                    Point p = getVirtualPoint(e.getPoint());
                    if (restartButtonRect.contains(p)) {
                        resetGame();
                    }
                }
            }
        });
        
        // Initialize starfield positions
        for (int i = 0; i < starsX.length; i++) {
            starsX[i] = Math.random() * V_WIDTH;
            starsY[i] = Math.random() * V_HEIGHT;
            starsSpeed[i] = 50 + Math.random() * 100; // Background scroll speed range
        }
        
        // Setup 90 FPS target loop (~11ms interval)
        gameTimer = new Timer(11, this);
        lastTime = System.nanoTime();
        gameTimer.start();
    }

    private void resetGame() {
        score = 0;
        survivalTime = 0.0;
        shipY = V_HEIGHT / 2.0;
        ammo = 3;
        ammoTimer = 0.0;
        bombCount = 0;
        bombsUsed = 0;
        bombTimer = 0.0;
        bombActive = false;
        bullets.clear();
        enemies.clear();
        moveUp = false;
        moveDown = false;
        currentState = State.PLAYING;
        lastTime = System.nanoTime();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        long now = System.nanoTime();
        double dt = (now - lastTime) / 1_000_000_000.0;
        lastTime = now;
        
        // Cap dt to avoid massive frame jumps during system hitches
        if (dt > 0.1) dt = 0.1; 

        if (currentState == State.PLAYING) {
            updateGame(dt);
        }
        
        repaint();
    }

    private void updateGame(double dt) {
        survivalTime += dt;
        score = (int) (survivalTime * 10); // 10 points per second survived
        
        // 1. Ship Movement Logic (Angular States mapped to vertical clamping)
        if (moveUp && !moveDown) {
            // Moving up at 45 degrees
            shipY -= shipSpeed * dt;
        } else if (moveDown && !moveUp) {
            // Moving down at 135 degrees
            shipY += shipSpeed * dt;
        }
        
        // Clamp bounds: Cannot exit top or bottom screen boundaries
        if (shipY < 0) {
            shipY = 0;
        }
        if (shipY > V_HEIGHT - shipHeight) {
            shipY = V_HEIGHT - shipHeight;
        }
        
        // 2. Parallax Starfield Update
        for (int i = 0; i < starsX.length; i++) {
            starsX[i] -= starsSpeed[i] * dt;
            if (starsX[i] < 0) {
                starsX[i] = V_WIDTH;
                starsY[i] = Math.random() * V_HEIGHT;
            }
        }
        
        // 3. System Timers: Ammo Recharge Rules (Max 3, 5 seconds per charge)
        if (ammo < MAX_AMMO) {
            ammoTimer += dt;
            if (ammoTimer >= 5.0) {
                ammo++;
                ammoTimer = 0.0;
            }
        } else {
            ammoTimer = 0.0;
        }
        
        // 4. System Timers: Bomb Cooldown Scaler Formula (20 + 2n)
        double currentBombCooldownTarget = 20.0 + (2.0 * bombsUsed);
        if (bombCount == 0) { // Only recharges if player does not currently hold a bomb
            bombTimer += dt;
            if (bombTimer >= currentBombCooldownTarget) {
                bombCount = 1;
                bombTimer = 0.0;
            }
        }
        
        // 5. Bomb VFX Expansion Logic
        if (bombActive) {
            bombRadius += 1500.0 * dt; // Rapid flash outward expansion speed
            if (bombRadius >= maxBombRadius) {
                bombActive = false;
                bombRadius = 0.0;
            }
        }
        
        // 6. Spawn Dynamic Enemies
        enemySpawnTimer += dt;
        double spawnRate = Math.max(0.4, 1.5 - (survivalTime * 0.02)); // Spawns accelerate over time
        if (enemySpawnTimer >= spawnRate) {
            enemies.add(new Enemy(V_WIDTH, Math.random() * (V_HEIGHT - 30)));
            enemySpawnTimer = 0.0;
        }
        
        // 7. Process Bullets
        Iterator<Bullet> bulletIt = bullets.iterator();
        while (bulletIt.hasNext()) {
            Bullet b = bulletIt.next();
            b.x += b.speed * dt;
            if (b.x > V_WIDTH) {
                bulletIt.remove();
            }
        }
        
        // 8. Process Enemies & Collisions
        Iterator<Enemy> enemyIt = enemies.iterator();
        while (enemyIt.hasNext()) {
            Enemy enemy = enemyIt.next();
            enemy.x -= enemy.speed * dt;
            
            // Player collision check
            Rectangle shipBounds = new Rectangle((int) 50, (int) shipY, (int) shipWidth, (int) shipHeight);
            Rectangle enemyBounds = new Rectangle((int) enemy.x, (int) enemy.y, (int) enemy.width, (int) enemy.height);
            
            if (shipBounds.intersects(enemyBounds)) {
                currentState = State.GAME_OVER;
                if (score > highScore) {
                    highScore = score;
                }
                return;
            }
            
            // Off-screen cleanup
            if (enemy.x + enemy.width < 0) {
                enemyIt.remove();
                continue;
            }
            
            // Bullet vs Enemy Collision (Horizontal line cross check)
            Iterator<Bullet> bIt = bullets.iterator();
            boolean enemyDestroyed = false;
            while (bIt.hasNext()) {
                Bullet b = bIt.next();
                Rectangle bulletBounds = new Rectangle((int) b.x, (int) b.y, (int) b.width, (int) b.height);
                if (bulletBounds.intersects(enemyBounds)) {
                    bIt.remove();
                    enemyDestroyed = true;
                    score += 50; // Bonus points for clean eliminations
                    break;
                }
            }
            
            if (enemyDestroyed) {
                enemyIt.remove();
            }
        }
    }

    private void fireBullet() {
        if (ammo > 0) {
            // Bullet fires horizontally from the front tip center of the spaceship
            bullets.add(new Bullet(50 + shipWidth, shipY + (shipHeight / 2) - 2));
            ammo--;
        }
    }

    private void triggerBomb() {
        if (bombCount > 0) {
            enemies.clear(); // 100% Instant tactical clear logic
            bombActive = true;
            bombRadius = 0.0;
            bombCount--;
            bombsUsed++;
            bombTimer = 0.0; // Reset timer instance to track the next step scaling duration
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Apply Global Scale Transform to maintain native virtual resolution scaling ratio
        double scaleX = (double) getWidth() / V_WIDTH;
        double scaleY = (double) getHeight() / V_HEIGHT;
        g2d.scale(scaleX, scaleY);
        
        // Draw Starfield
        g2d.setColor(Color.WHITE);
        for (int i = 0; i < starsX.length; i++) {
            g2d.fillRect((int) starsX[i], (int) starsY[i], 2, 2);
        }
        
        // Draw Bullets (Laser lines)
        g2d.setColor(Color.CYAN);
        for (Bullet b : bullets) {
            g2d.fillRect((int) b.x, (int) b.y, (int) b.width, (int) b.height);
        }
        
        // Draw Enemies
        for (Enemy enemy : enemies) {
            g2d.setColor(enemy.color);
            g2d.fillRect((int) enemy.x, (int) enemy.y, (int) enemy.width, (int) enemy.height);
            // Wing details
            g2d.setColor(Color.BLACK);
            g2d.drawRect((int) enemy.x, (int) enemy.y, (int) enemy.width, (int) enemy.height);
        }
        
        // Draw Spaceship Player (Vector look)
        g2d.setColor(Color.GREEN);
        int[] xPoints = {50, 50 + (int) shipWidth, 50};
        int[] yPoints = {(int) shipY, (int) shipY + ((int) shipHeight / 2), (int) shipY + (int) shipHeight};
        g2d.fillPolygon(xPoints, yPoints, 3);
        
        // Draw Expanding Bomb Circle Flash Visual effect
        if (bombActive) {
            g2d.setColor(new Color(255, 255, 255, 180));
            double centerX = V_WIDTH / 2.0;
            double centerY = V_HEIGHT / 2.0;
            Ellipse2D.Double circle = new Ellipse2D.Double(
                    centerX - bombRadius, 
                    centerY - bombRadius, 
                    bombRadius * 2, 
                    bombRadius * 2
            );
            g2d.fill(circle);
        }
        
        // Draw Heads-Up Display (HUD) Info Panel
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 14));
        g2d.drawString("SCORE: " + score, 20, 30);
        g2d.drawString("HI-SCORE: " + highScore, 160, 30);
        g2d.drawString("AMMO: " + ammo + "/" + MAX_AMMO + (ammo < MAX_AMMO ? String.format(" (Recharging: %.1fs)", 5.0 - ammoTimer) : ""), 320, 30);
        
        double nextCooldown = 20.0 + (2.0 * bombsUsed);
        String bombStatus = (bombCount > 0) ? "READY [X]" : String.format("CHARGING: %.1fs / %.1fs", bombTimer, nextCooldown);
        g2d.drawString("BOMB: " + bombStatus, 20, 60);
        
        // Draw Game Over Screen Matrix Overlay
        if (currentState == State.GAME_OVER) {
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRect(0, 0, V_WIDTH, V_HEIGHT);
            
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            FontMetrics fm = g2d.getFontMetrics();
            String gameOverText = "GAME OVER";
            g2d.drawString(gameOverText, (V_WIDTH - fm.stringWidth(gameOverText)) / 2, V_HEIGHT / 2 - 40);
            
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.PLAIN, 18));
            fm = g2d.getFontMetrics();
            String scoreText = "Final Score: " + score + " | High Score: " + highScore;
            g2d.drawString(scoreText, (V_WIDTH - fm.stringWidth(scoreText)) / 2, (int) (V_HEIGHT / 2.0));
            
            // Virtual Button drawing
            g2d.setColor(Color.DARK_GRAY);
            g2d.fill(restartButtonRect);
            g2d.setColor(Color.WHITE);
            g2d.draw(restartButtonRect);
            
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            fm = g2d.getFontMetrics();
            String btnText = "RESTART";
            g2d.drawString(btnText, restartButtonRect.x + (restartButtonRect.width - fm.stringWidth(btnText)) / 2, 
                           restartButtonRect.y + (restartButtonRect.height + fm.getAscent() - fm.getDescent()) / 2);
        }
    }

    // Transform raw display coordinate point to target virtual space rectangle bounds mapping configuration
    private Point getVirtualPoint(Point screenPoint) {
        double scaleX = (double) getWidth() / V_WIDTH;
        double scaleY = (double) getHeight() / V_HEIGHT;
        return new Point((int) (screenPoint.x / scaleX), (int) (screenPoint.y / scaleY));
    }

    // Key input parsing handlers
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (currentState == State.PLAYING) {
            if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP) {
                moveUp = true;
            }
            if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) {
                moveDown = true;
            }
            if (key == KeyEvent.VK_SPACE) {
                fireBullet();
            }
            if (key == KeyEvent.VK_X) {
                triggerBomb();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP) {
            moveUp = false;
        }
        if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) {
            moveDown = false;
        }
    }

    @Override public void keyTyped(KeyEvent e) {}

    // Inner Entities Model Schemas
    private static class Bullet {
        double x, y;
        double width = 15;
        double height = 4;
        double speed = 600.0;
        
        Bullet(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class Enemy {
        double x, y;
        double width = 30;
        double height = 30;
        double speed;
        Color color;
        
        Enemy(double x, double y) {
            this.x = x;
            this.y = y;
            this.speed = 150.0 + Math.random() * 150.0; // Random horizontal speed profiles per enemy
            // Random variant coloring selection
            int type = (int) (Math.random() * 3);
            if (type == 0) this.color = Color.RED;
            else if (type == 1) this.color = Color.ORANGE;
            else this.color = Color.MAGENTA;
        }
    }
}
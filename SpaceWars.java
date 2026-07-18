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
    private static final int V_WIDTH = 800;
    private static final int V_HEIGHT = 500;
    
    // Game States
    private enum State { MENU, PLAYING, GAME_OVER }
    private State currentState = State.MENU;
    
    // Difficulty Settings
    private enum Difficulty { EASY, MEDIUM, HARD }
    private Difficulty selectedDifficulty = Difficulty.MEDIUM;
    
    // Core Loop & Timing
    private final Timer gameTimer;
    private long lastTime;
    
    // Scoring
    private int score = 0;
    private int highScore = 0;
    
    // Gameplay Entities
    private double shipY = V_HEIGHT / 2.0;
    private final double shipWidth = 40;
    private final double shipHeight = 25;
    private final double shipSpeed = 300.0;
    
    private boolean moveUp = false;
    private boolean moveDown = false;
    
    // Parallax Starfield
    private final double[] starsX = new double[50];
    private final double[] starsY = new double[50];
    private final double[] starsSpeed = new double[50];
    
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    
    // Dynamic Difficulty Tuners
    private double ammoRechargeTarget = 5.0;
    private int maxAmmo = 5;
    private double bombBaseCooldown = 15.0;
    private double bombMultiplier = 2.0;
    private double enemySpeedModifier = 1.0;
    
    // Systems Resources
    private double ammoTimer = 0.0;
    private int ammo = 5;
    private double bombTimer = 0.0;
    private int bombCount = 0;
    private int bombsUsed = 0;
    
    private double enemySpawnTimer = 0.0;
    private double survivalTime = 0.0;
    
    // Visual Bomb Flash
    private boolean bombActive = false;
    private double bombRadius = 0.0;
    private final double maxBombRadius = Math.max(V_WIDTH, V_HEIGHT) * 0.9;
    
    // Interactive UI Clickable Bounds
    private final Rectangle easyBtn = new Rectangle(V_WIDTH / 2 - 60, 200, 120, 40);
    private final Rectangle mediumBtn = new Rectangle(V_WIDTH / 2 - 60, 260, 120, 40);
    private final Rectangle hardBtn = new Rectangle(V_WIDTH / 2 - 60, 320, 120, 40);
    private final Rectangle restartButtonRect = new Rectangle(V_WIDTH / 2 - 60, V_HEIGHT / 2 + 40, 120, 40);

    public GamePanel() {
        setPreferredSize(new Dimension(V_WIDTH, V_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point p = getVirtualPoint(e.getPoint());
                
                if (currentState == State.MENU) {
                    if (easyBtn.contains(p)) {
                        applyDifficultyAndStart(Difficulty.EASY);
                    } else if (mediumBtn.contains(p)) {
                        applyDifficultyAndStart(Difficulty.MEDIUM);
                    } else if (hardBtn.contains(p)) {
                        applyDifficultyAndStart(Difficulty.HARD);
                    }
                } else if (currentState == State.GAME_OVER) {
                    if (restartButtonRect.contains(p)) {
                        currentState = State.MENU;
                    }
                }
            }
        });
        
        for (int i = 0; i < starsX.length; i++) {
            starsX[i] = Math.random() * V_WIDTH;
            starsY[i] = Math.random() * V_HEIGHT;
            starsSpeed[i] = 50 + Math.random() * 100;
        }
        
        gameTimer = new Timer(11, this);
        lastTime = System.nanoTime();
        gameTimer.start();
    }

    private void applyDifficultyAndStart(Difficulty diff) {
        selectedDifficulty = diff;
        switch (diff) {
            case EASY:
                ammoRechargeTarget = 3.0;
                maxAmmo = 7;
                bombBaseCooldown = 10.0;
                bombMultiplier = 2.0;
                enemySpeedModifier = 0.8;
                break;
            case MEDIUM:
                ammoRechargeTarget = 5.0;
                maxAmmo = 5;
                bombBaseCooldown = 15.0;
                bombMultiplier = 2.0;
                enemySpeedModifier = 1.0;
                break;
            case HARD:
                ammoRechargeTarget = 6.0;
                maxAmmo = 3; // Tight ammunition limits
                bombBaseCooldown = 25.0;
                bombMultiplier = 3.0; // Dynamic growth penalty: 25 + 3n
                enemySpeedModifier = 1.4; // 40% speed escalation
                break;
        }
        resetGame();
    }

    private void resetGame() {
        score = 0;
        survivalTime = 0.0;
        shipY = V_HEIGHT / 2.0;
        ammo = maxAmmo;
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
        
        if (dt > 0.1) dt = 0.1; 

        // Parallax stars animate even on menus
        if (currentState == State.PLAYING || currentState == State.MENU) {
            for (int i = 0; i < starsX.length; i++) {
                starsX[i] -= starsSpeed[i] * dt;
                if (starsX[i] < 0) {
                    starsX[i] = V_WIDTH;
                    starsY[i] = Math.random() * V_HEIGHT;
                }
            }
        }

        if (currentState == State.PLAYING) {
            updateGame(dt);
        }
        
        repaint();
    }

    private void updateGame(double dt) {
        survivalTime += dt;
        score = (int) (survivalTime * 10);
        
        if (moveUp && !moveDown) {
            shipY -= shipSpeed * dt;
        } else if (moveDown && !moveUp) {
            shipY += shipSpeed * dt;
        }
        
        if (shipY < 0) shipY = 0;
        if (shipY > V_HEIGHT - shipHeight) shipY = V_HEIGHT - shipHeight;
        
        // Ammo Recharge System
        if (ammo < maxAmmo) {
            ammoTimer += dt;
            if (ammoTimer >= ammoRechargeTarget) {
                ammo++;
                ammoTimer = 0.0;
            }
        } else {
            ammoTimer = 0.0;
        }
        
        // Dynamic Bomb Cooldown Calculation: Base + (Multiplier * n)
        double currentBombCooldownTarget = bombBaseCooldown + (bombMultiplier * bombsUsed);
        if (bombCount == 0) {
            bombTimer += dt;
            if (bombTimer >= currentBombCooldownTarget) {
                bombCount = 1;
                bombTimer = 0.0;
            }
        }
        
        if (bombActive) {
            bombRadius += 1500.0 * dt;
            if (bombRadius >= maxBombRadius) {
                bombActive = false;
                bombRadius = 0.0;
            }
        }
        
        // Enemy Spawning
        enemySpawnTimer += dt;
        double baseSpawnRate = (selectedDifficulty == Difficulty.HARD) ? 0.8 : 1.5;
        double spawnRate = Math.max(0.3, baseSpawnRate - (survivalTime * 0.02));
        if (enemySpawnTimer >= spawnRate) {
            enemies.add(new Enemy(V_WIDTH, Math.random() * (V_HEIGHT - 30), enemySpeedModifier));
            enemySpawnTimer = 0.0;
        }
        
        // Process Bullets
        Iterator<Bullet> bulletIt = bullets.iterator();
        while (bulletIt.hasNext()) {
            Bullet b = bulletIt.next();
            b.x += b.speed * dt;
            if (b.x > V_WIDTH) bulletIt.remove();
        }
        
        // Process Enemies & Collisions
        Iterator<Enemy> enemyIt = enemies.iterator();
        while (enemyIt.hasNext()) {
            Enemy enemy = enemyIt.next();
            enemy.x -= enemy.speed * dt;
            
            Rectangle shipBounds = new Rectangle(50, (int) shipY, (int) shipWidth, (int) shipHeight);
            Rectangle enemyBounds = new Rectangle((int) enemy.x, (int) enemy.y, (int) enemy.width, (int) enemy.height);
            
            if (shipBounds.intersects(enemyBounds)) {
                currentState = State.GAME_OVER;
                if (score > highScore) highScore = score;
                return;
            }
            
            if (enemy.x + enemy.width < 0) {
                enemyIt.remove();
                continue;
            }
            
            Iterator<Bullet> bIt = bullets.iterator();
            boolean enemyDestroyed = false;
            while (bIt.hasNext()) {
                Bullet b = bIt.next();
                Rectangle bulletBounds = new Rectangle((int) b.x, (int) b.y, (int) b.width, (int) b.height);
                if (bulletBounds.intersects(enemyBounds)) {
                    bIt.remove();
                    enemyDestroyed = true;
                    score += 50;
                    break;
                }
            }
            
            if (enemyDestroyed) enemyIt.remove();
        }
    }

    private void fireBullet() {
        if (ammo > 0) {
            bullets.add(new Bullet(50 + shipWidth, shipY + (shipHeight / 2) - 2));
            ammo--;
        }
    }

    private void triggerBomb() {
        if (bombCount > 0) {
            enemies.clear();
            bombActive = true;
            bombRadius = 0.0;
            bombCount--;
            bombsUsed++;
            bombTimer = 0.0;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        double scaleX = (double) getWidth() / V_WIDTH;
        double scaleY = (double) getHeight() / V_HEIGHT;
        g2d.scale(scaleX, scaleY);
        
        // Stars
        g2d.setColor(Color.WHITE);
        for (int i = 0; i < starsX.length; i++) {
            g2d.fillRect((int) starsX[i], (int) starsY[i], 2, 2);
        }
        
        if (currentState == State.MENU) {
            drawMenu(g2d);
            return;
        }
        
        // Gameplay Rendering Elements
        g2d.setColor(Color.CYAN);
        for (Bullet b : bullets) {
            g2d.fillRect((int) b.x, (int) b.y, (int) b.width, (int) b.height);
        }
        
        for (Enemy enemy : enemies) {
            g2d.setColor(enemy.color);
            g2d.fillRect((int) enemy.x, (int) enemy.y, (int) enemy.width, (int) enemy.height);
        }
        
        g2d.setColor(Color.GREEN);
        int[] xPoints = {50, 50 + (int) shipWidth, 50};
        int[] yPoints = {(int) shipY, (int) shipY + ((int) shipHeight / 2), (int) shipY + (int) shipHeight};
        g2d.fillPolygon(xPoints, yPoints, 3);
        
        if (bombActive) {
            g2d.setColor(new Color(255, 255, 255, 180));
            Ellipse2D.Double circle = new Ellipse2D.Double(V_WIDTH / 2.0 - bombRadius, V_HEIGHT / 2.0 - bombRadius, bombRadius * 2, bombRadius * 2);
            g2d.fill(circle);
        }
        
        // HUD Setup
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 14));
        g2d.drawString("SCORE: " + score, 20, 30);
        g2d.drawString("MODE: " + selectedDifficulty, 160, 30);
        g2d.drawString("AMMO: " + ammo + "/" + maxAmmo + (ammo < maxAmmo ? String.format(" (%.1fs)", ammoRechargeTarget - ammoTimer) : ""), 320, 30);
        
        double nextCooldown = bombBaseCooldown + (bombMultiplier * bombsUsed);
        String bombStatus = (bombCount > 0) ? "READY [X]" : String.format("CHARGING: %.1fs / %.1fs", bombTimer, nextCooldown);
        g2d.drawString("BOMB: " + bombStatus, 20, 60);
        
        if (currentState == State.GAME_OVER) {
            drawGameOverScreen(g2d);
        }
    }

    private void drawMenu(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 42));
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString("SPACE WARS", (V_WIDTH - fm.stringWidth("SPACE WARS")) / 2, 120);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        fm = g2d.getFontMetrics();
        g2d.drawString("SELECT DIFFICULTY TO PLAY", (V_WIDTH - fm.stringWidth("SELECT DIFFICULTY TO PLAY")) / 2, 160);
        
        drawButton(g2d, easyBtn, "EASY", Color.GREEN);
        drawButton(g2d, mediumBtn, "MEDIUM", Color.ORANGE);
        drawButton(g2d, hardBtn, "HARD", Color.RED);
    }

    private void drawButton(Graphics2D g2d, Rectangle rect, String text, Color textColor) {
        g2d.setColor(Color.DARK_GRAY);
        g2d.fill(rect);
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.draw(rect);
        g2d.setColor(textColor);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(text, rect.x + (rect.width - fm.stringWidth(text)) / 2, rect.y + (rect.height + fm.getAscent() - fm.getDescent()) / 2);
    }

    private void drawGameOverScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, V_WIDTH, V_HEIGHT);
        
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString("GAME OVER", (V_WIDTH - fm.stringWidth("GAME OVER")) / 2, V_HEIGHT / 2 - 40);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        fm = g2d.getFontMetrics();
        String scoreText = "Final Score: " + score + " | High Score: " + highScore;
        g2d.drawString(scoreText, (V_WIDTH - fm.stringWidth(scoreText)) / 2, (int) (V_HEIGHT / 2.0));
        
        drawButton(g2d, restartButtonRect, "MAIN MENU", Color.WHITE);
    }

    private Point getVirtualPoint(Point screenPoint) {
        double scaleX = (double) getWidth() / V_WIDTH;
        double scaleY = (double) getHeight() / V_HEIGHT;
        return new Point((int) (screenPoint.x / scaleX), (int) (screenPoint.y / scaleY));
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (currentState == State.PLAYING) {
            if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP) moveUp = true;
            if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) moveDown = true;
            if (key == KeyEvent.VK_SPACE) fireBullet();
            if (key == KeyEvent.VK_X) triggerBomb();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP) moveUp = false;
        if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) moveDown = false;
    }

    @Override public void keyTyped(KeyEvent e) {}

    private static class Bullet {
        double x, y;
        double width = 15;
        double height = 4;
        double speed = 600.0;
        Bullet(double x, double y) { this.x = x; this.y = y; }
    }

    private static class Enemy {
        double x, y, width = 30, height = 30, speed;
        Color color;
        Enemy(double x, double y, double speedMod) {
            this.x = x;
            this.y = y;
            this.speed = (150.0 + Math.random() * 150.0) * speedMod;
            int type = (int) (Math.random() * 3);
            this.color = (type == 0) ? Color.RED : (type == 1) ? Color.ORANGE : Color.MAGENTA;
        }
    }
}
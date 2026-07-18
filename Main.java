import java.awt.*;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class Main extends JPanel {

    // Game Settings
    private final int TARGET_WIDTH = 800;
    private final int TARGET_HEIGHT = 500;

    // Image Setting
    private Image playerImage;
    private final int IMAGE_WIDTH=800;
    private final int IMAGE_HEIGHT=40;

    // Position the image right in the center of the 800x500 virtual canvas
    private int ImageX = 0;
private int ImageY =  (TARGET_HEIGHT)-(IMAGE_HEIGHT); // Adjusted to place the image at the bottom of the canvas

    public Main() {
        this.setPreferredSize(new Dimension(TARGET_WIDTH, TARGET_HEIGHT));
        this.setBackground(Color.WHITE);
        this.setFocusable(true);

        playerImage = new ImageIcon("C:\\Games\\character.jpg").getImage();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); 
        
        Graphics2D g2d = (Graphics2D) g;

        int currentWindowWidth = getWidth();
        int currentWindowHeight = getHeight();

        // Find the scale factor for both width and height
        double scaleX = (double) currentWindowWidth / TARGET_WIDTH;
        double scaleY = (double) currentWindowHeight / TARGET_HEIGHT;
        
        // Use the smaller scale factor for BOTH axes to prevent stretching/distortion
        double uniformScale = Math.min(scaleX, scaleY);

        // Optional: Center the scaled game canvas within the larger window
        int offsetX = (int) ((currentWindowWidth - (TARGET_WIDTH * uniformScale)) / 2);
        int offsetY = (int) ((currentWindowHeight - (TARGET_HEIGHT * uniformScale)) / 2);
        
        g2d.translate(offsetX, offsetY); // Move the drawing space to the center of the window
        g2d.scale(uniformScale, uniformScale); // Scale up perfectly without stretching

        // Draw the image
        if (playerImage != null) {
            g2d.drawImage(playerImage, ImageX, ImageY, IMAGE_WIDTH, IMAGE_HEIGHT, this);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Image in Java Block");
        Main game = new Main();

        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
// create a blank screen ✅Done
// insert an image ✅Done
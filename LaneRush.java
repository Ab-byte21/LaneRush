import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

// Main game class
public class LaneRush extends JPanel implements ActionListener, KeyListener {
    // Game settings
    private Timer timer; // Timer to control game updates
    private final int DELAY = 20; // Timer delay in milliseconds
    private final int ROAD_WIDTH = 800; // Road width
    private final int ROAD_HEIGHT = 600; // Road height
    private final int PLAYER_CAR_WIDTH = 50; // Player car width
    private final int PLAYER_CAR_HEIGHT = 100; // Player car height
    private final int TRAFFIC_CAR_HEIGHT = 100; // Traffic car height
    private final int TRAFFIC_SPEED = 2; // Speed of traffic cars
    private final int MIN_GAP = 300; // Minimum gap for spawning traffic cars

    // Game state
    private Car playerCar1; // First player car
    private Car playerCar2; // Second player car
    private List<Car> trafficCars; // List to hold traffic cars
    private boolean gameOver; // Flag to check if the game is over
    private int score; // Player's score
    private int highScore; // High score
    private int spawnTimer; // Timer to control traffic car spawning
    private Car failedCar; // Car that failed (collided with the player)
    private boolean[] occupiedLanes; // Array to track occupied lanes
    private Clip backgroundMusic; // Clip for background music

    // Constructor
    public LaneRush() {
        setPreferredSize(new Dimension(ROAD_WIDTH, ROAD_HEIGHT)); // Set panel size
        setBackground(Color.GRAY); // Set background color
        setFocusable(true); // Make panel focusable
        addKeyListener(this); // Add key listener for player controls

        initializeGame(); // Initialize game settings and state
        playBackgroundMusic("/Users/hughie1/Desktop/FinalGame/background_music.wav"); // Play background music
    }

    // Initialize game settings and state
    private void initializeGame() {
        int laneWidth = ROAD_WIDTH / 4; // Calculate width of each lane
        playerCar1 = new Car(laneWidth / 2 - PLAYER_CAR_WIDTH / 2, ROAD_HEIGHT - PLAYER_CAR_HEIGHT - 10, PLAYER_CAR_WIDTH, PLAYER_CAR_HEIGHT, Color.RED, "Player 1"); // Initialize first player car
        playerCar2 = new Car(5 * laneWidth / 2 - PLAYER_CAR_WIDTH / 2, ROAD_HEIGHT - PLAYER_CAR_HEIGHT - 10, PLAYER_CAR_WIDTH, PLAYER_CAR_HEIGHT, Color.RED, "Player 2"); // Initialize second player car
        trafficCars = new ArrayList<>(); // Initialize traffic cars list
        occupiedLanes = new boolean[4]; // Initialize occupied lanes array
        timer = new Timer(DELAY, this); // Create a timer with the specified delay
        timer.start(); // Start the timer
        gameOver = false; // Set game over flag to false
        if (score > highScore) {
            highScore = score; // Update high score if current score is higher
        }
        score = 0; // Reset score
        spawnTimer = 0; // Reset spawn timer
        failedCar = null; // Reset failed car
    }

    // Add a new traffic car to the game
    private void addNewTrafficCar() {
        List<Integer> availableLanes = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (!occupiedLanes[i]) {
                availableLanes.add(i); // Add empty lanes to the list
            }
        }

        if (availableLanes.size() <= 2) return; // If fewer than 2 lanes are available, do not spawn a new car

        int lane = availableLanes.get((int) (Math.random() * availableLanes.size())); // Randomly select an available lane
        int laneWidth = ROAD_WIDTH / 4; // Calculate lane width
        int x = lane * laneWidth + (laneWidth - PLAYER_CAR_WIDTH) / 2; // Calculate x position for the new car

        boolean canSpawn = true;
        for (Car car : trafficCars) {
            if (car.getY() > -TRAFFIC_CAR_HEIGHT && car.getY() < MIN_GAP) {
                canSpawn = false; // Prevent spawning if there is not enough space
                break;
            }
        }

        if (canSpawn) {
            trafficCars.add(new Car(x, -TRAFFIC_CAR_HEIGHT, PLAYER_CAR_WIDTH, TRAFFIC_CAR_HEIGHT, Color.BLUE, null)); // Add new traffic car to the list
            occupiedLanes[lane] = true; // Mark lane as occupied
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawRoad(g); // Draw the road

        if (gameOver) { // Check if the game is over
            g.setColor(Color.BLACK); // Set color for game over text
            g.setFont(new Font("Arial", Font.BOLD, 36)); // Set font for game over text
            g.drawString("Game Over", ROAD_WIDTH / 2 - 100, ROAD_HEIGHT / 2); // Draw game over message
            g.drawString("Score: " + score, ROAD_WIDTH / 2 - 100, ROAD_HEIGHT / 2 + 40); // Draw current score
            g.drawString("High Score: " + highScore, ROAD_WIDTH / 2 - 100, ROAD_HEIGHT / 2 + 80); // Draw high score
            g.drawString("Press R to Restart", ROAD_WIDTH / 2 - 150, ROAD_HEIGHT / 2 + 120); // Draw restart message

            if (failedCar != null) {
                failedCar.drawWithHeadlights(g); // Draw the failed car with headlights
            }
        } else {
            playerCar1.draw(g); // Draw the first player car
            playerCar2.draw(g); // Draw the second player car
            for (Car car : trafficCars) {
                car.draw(g); // Draw each traffic car
            }
            g.setColor(Color.WHITE); // Set color for score display
            g.drawString("Score: " + score, 10, 20); // Draw current score
            g.drawString("High Score: " + highScore, 10, 40); // Draw high score
        }
    }

    // Draw the road and lanes
    private void drawRoad(Graphics g) {
        g.setColor(Color.DARK_GRAY); // Set color for road
        g.fillRect(0, 0, ROAD_WIDTH, ROAD_HEIGHT); // Fill road area

        g.setColor(Color.WHITE); // Set color for lane markings
        for (int i = 1; i < 4; i++) {
            int x = i * (ROAD_WIDTH / 4); // Calculate x position for lane markings
            for (int y = 0; y < ROAD_HEIGHT; y += 40) {
                g.fillRect(x - 2, y, 4, 20); // Draw lane markings
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            spawnTimer++;
            if (spawnTimer >= 60) {
                addNewTrafficCar(); // Add a new traffic car every 60 timer ticks
                spawnTimer = 0; // Reset spawn timer
            }
            for (int i = 0; i < 4; i++) {
                occupiedLanes[i] = false; // Reset occupied lanes
            }

            for (int i = trafficCars.size() - 1; i >= 0; i--) {
                Car car = trafficCars.get(i);
                car.move(0, TRAFFIC_SPEED); // Move each traffic car down
                if (car.getY() > ROAD_HEIGHT) {
                    trafficCars.remove(i); // Remove traffic car if it moves off the screen
                    score++; // Increase score
                } else {
                    int lane = (int) (car.getX() / (ROAD_WIDTH / 4)); // Determine the lane of the car
                    occupiedLanes[lane] = true; // Mark lane as occupied
                }
                if (car.intersects(playerCar1)) {
                    gameOver = true; // End the game if player car 1 collides with a traffic car
                    failedCar = playerCar1; // Set the failed car
                
                }
                if (car.intersects(playerCar2)) {
                    gameOver = true; // End the game if player car 2 collides with a traffic car
                    failedCar = playerCar2; // Set the failed car
        
                }
            }
            repaint(); // Repaint the panel to update the display
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_A) playerCar1.moveToLane(-1, ROAD_WIDTH); // Move player car 1 left
        if (key == KeyEvent.VK_D) playerCar1.moveToLane(1, ROAD_WIDTH); // Move player car 1 right
        if (key == KeyEvent.VK_LEFT) playerCar2.moveToLane(-1, ROAD_WIDTH); // Move player car 2 left
        if (key == KeyEvent.VK_RIGHT) playerCar2.moveToLane(1, ROAD_WIDTH); // Move player car 2 right

        if (gameOver && key == KeyEvent.VK_R) {
            initializeGame(); // Restart the game if 'R' is pressed
            repaint(); // Repaint the panel to reset the display
        }
        
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    // Play background music
    private void playBackgroundMusic(String soundFile) {
        try {
            backgroundMusic = AudioSystem.getClip(); // Get audio clip
            backgroundMusic.open(AudioSystem.getAudioInputStream(new File("/Users/hughie1/Desktop/FinalGame/background_music.wav"))); // Open the background music file
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY); // Loop background music continuously
        } catch (Exception e) {
            e.printStackTrace(); // Print error if music file fails to load
        }
    }
    private void stopBackgroundMusic() {
        if (backgroundMusicClip != null && backgroundMusicClip.isRunning()) {
            backgroundMusicClip.stop();
        }
    }

    // Main method to start the game
    public static void main(String[] args) {
        JFrame frame = new JFrame("Lane Rush"); // Create the main game window
        LaneRush game = new LaneRush(); // Create game instance
        frame.add(game); // Add game panel to the frame
        frame.pack(); // Pack the frame
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Set default close operation
        frame.setVisible(true); // Make the frame visible
    }
}

// Car class extending Rectangle for player and traffic cars
class Car extends Rectangle {
    private Color color; // Color of the car
    private String label; // Label of the car (for player cars)
    private int currentLane; // Current lane of the car

    // Constructor for Car class
    public Car(int x, int y, int width, int height, Color color, String label) {
        super(x, y, width, height); // Initialize rectangle
        this.color = color; // Set car color
        this.label = label; // Set car label
        this.currentLane = x / (800 / 4); // Calculate current lane
    }

    // Draw the car
    public void draw(Graphics g) {
        g.setColor(color); // Set car color
        g.fillRoundRect(x, y, width, height, 10, 10); // Draw car body

        g.setColor(Color.LIGHT_GRAY); // Set color for car windows
        g.fillRoundRect(x + 5, y + 10, width - 10, height / 3, 5, 5); // Draw car windows

        g.setColor(Color.BLACK); // Set color for car wheels
        g.fillOval(x - 5, y + height - 25, 20, 20); // Draw left wheel
        g.fillOval(x + width - 15, y + height - 25, 20, 20); // Draw right wheel

        g.setColor(Color.YELLOW); // Set color for car headlights
        g.fillOval(x + 5, y + height - 15, 10, 10); // Draw left headlight
        g.fillOval(x + width - 15, y + height - 15, 10, 10); // Draw right headlight

        if (label != null) {
            g.setColor(Color.WHITE); // Set color for car label
            g.setFont(new Font("Arial", Font.BOLD, 12)); // Set font for car label
            FontMetrics fm = g.getFontMetrics(); // Get font metrics
            int textWidth = fm.stringWidth(label); // Calculate label width
            g.drawString(label, x + (width - textWidth) / 2, y + height + 15); // Draw car label
        }
    }

    // Draw the car with headlights
    public void drawWithHeadlights(Graphics g) {
        draw(g); // Draw the car body
        g.setColor(new Color(255, 255, 0, 100)); // Set color for headlights
        int beamWidth = 30; // Width of headlight beams
        int beamHeight = 100; // Height of headlight beams
        g.fillArc(x - beamWidth / 2, y - beamHeight, beamWidth, beamHeight, 0, 90); // Draw left headlight beam
        g.fillArc(x + width - beamWidth / 2, y - beamHeight, beamWidth, beamHeight, 90, 90); // Draw right headlight beam
    }

    // Move the car by dx and dy
    public void move(int dx, int dy) {
        x += dx; // Update x position
        y += dy; // Update y position
    }

    // Move the car to a specific lane
    public void moveToLane(int direction, int roadWidth) {
        int laneWidth = roadWidth / 4; // Calculate lane width
        currentLane += direction; // Update lane
        if (currentLane < 0) currentLane = 0; // Prevent moving to invalid lane
        if (currentLane > 3) currentLane = 3; // Prevent moving to invalid lane
        x = currentLane * laneWidth + (laneWidth - width) / 2; // Update x position based on lane
    }
}

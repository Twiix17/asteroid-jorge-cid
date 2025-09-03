import greenfoot.*;           // World, Actor, GreenfootImage, Greenfoot
import java.util.List;
import java.util.Random;
import java.awt.Color;

public class AsteroidsWorld extends World {

    public static final int WIDTH  = 900;
    public static final int HEIGHT = 700;
    public static final int CELL   = 1;

    private boolean gameStarted = false;
    private boolean waveClearedBanner = false;

    private int score = 0;
    private int lives = 3;
    private int wave  = 0;

    private int nextWaveDelayFrames = 0;
    private final Random rng = new Random();
    private int baseLargeAsteroids = 5;
    private float waveBudgetFactor   = 1.25f;
    private int safeSpawnRadius    = 140;
    private int respawnDelayFrames = 45;
    private int respawnTimer = 0;

    private boolean waitingForStart = true;

    public AsteroidsWorld() {
        super(WIDTH, HEIGHT, CELL, false);
        buildStarfieldBackground();
        drawTitleScreen();
    }

    @Override
    public void act() {
        // --- PANTALLA DE INICIO ---
        if (waitingForStart) {
            showCenteredMessage("ASTEROIDS\nENTER para comenzar", 42);
            if (Greenfoot.isKeyDown("enter")) {
                waitingForStart = false;
                startGame();
            }
            return;
        }

        // --- JUEGO EN CURSO ---
        drawHUD();
        tickRespawn();

        if (getObjects(Asteroid.class).isEmpty() && respawnTimer == 0) {
            if (nextWaveDelayFrames > 0) {
                nextWaveDelayFrames--;
                if (nextWaveDelayFrames == 0) {
                    spawnNextWave();
                }
            }
        }

        // --- GAME OVER ---
        if (lives <= 0 && getObjects(PlayerShip.class).isEmpty()) {
            waitingForStart = true;
            gameStarted = false;
            showCenteredMessage("ENTER para comenzar", 36);
        }
    }

    private void startGame() {
        gameStarted = true;
        waveClearedBanner = false;
        score = 0;
        lives = 3;
        wave  = 0;
        nextWaveDelayFrames = 0;
        respawnTimer = 0;

        removeObjects(getObjects(Actor.class));
        buildStarfieldBackground();
        clearCenterMessage();

        spawnPlayerSafely();
        spawnNextWave();
    }

    private void tickRespawn() {
        if (respawnTimer > 0) {
            respawnTimer--;
            if (respawnTimer == 0 && lives > 0 && getObjects(PlayerShip.class).isEmpty()) {
                spawnPlayerSafely();
            }
        }
    }

    private void spawnNextWave() {
        wave++;
        int budget = Math.max(1, Math.round((float)baseLargeAsteroids * (float)Math.pow(waveBudgetFactor, wave - 1)));
        int numLarge = Math.max(3, budget);
        for (int i = 0; i < numLarge; i++) {
            spawnAsteroidSafely(Asteroid.Size.LARGE);
        }

        nextWaveDelayFrames = 45;
        waveClearedBanner = false;

        if (rng.nextDouble() < 0.15) {
            UFO.Type type = (wave >= 4 && rng.nextBoolean()) ? UFO.Type.SMALL : UFO.Type.LARGE;
            double acc   = (type == UFO.Type.SMALL) ? 0.65 + 0.1*wave : 0.30 + 0.07*wave;
            acc = Math.min(0.95, acc);
            UFO ufo = new UFO(type, acc);
            int y = 40 + rng.nextInt(getHeight() - 80);
            int x = rng.nextBoolean() ? -1 : getWidth() + 1;
            addObject(ufo, x, y);
        }
    }

    public void addScore(int points) {
        score = Math.max(0, score + points);
    }

    public void loseLife() {
        if (lives <= 0) return;
        lives--;
        if (lives > 0) {
            respawnTimer = respawnDelayFrames;
        } else {
            respawnTimer = 0;
            waitingForStart = true;
            clearCenterMessage();
            showCenteredMessage("GAME OVER\nENTER para comenzar", 36);
        }
    }

    private void spawnPlayerSafely() {
        int cx = WIDTH / 2;
        int cy = HEIGHT / 2;
        int[] pos = findSafeSpawnPosition(cx, cy, safeSpawnRadius);
        PlayerShip ship = new PlayerShip();
        addObject(ship, pos[0], pos[1]);
        clearCenterMessage();
    }

    private void spawnAsteroidSafely(Asteroid.Size size) {
        int[][] candidates = new int[][] {
            {rng.nextInt(WIDTH), 0},
            {rng.nextInt(WIDTH), HEIGHT-1},
            {0, rng.nextInt(HEIGHT)},
            {WIDTH-1, rng.nextInt(HEIGHT)},
            {rng.nextInt(WIDTH), rng.nextInt(HEIGHT)}
        };

        int[] pos = null;
        for (int[] c : candidates) {
            if (isSafeFromPlayer(c[0], c[1], safeSpawnRadius)) {
                pos = c;
                break;
            }
        }
        if (pos == null) pos = findSafeSpawnPosition(rng.nextInt(WIDTH), rng.nextInt(HEIGHT), safeSpawnRadius);

        Asteroid a = new Asteroid(size);
        addObject(a, pos[0], pos[1]);
    }

    private boolean isSafeFromPlayer(int x, int y, int radius) {
        List<PlayerShip> players = getObjects(PlayerShip.class);
        if (players.isEmpty()) return true;
        PlayerShip p = players.get(0);
        int dx = x - p.getX();
        int dy = y - p.getY();
        return (dx*dx + dy*dy) >= (radius * radius);
    }

    private int[] findSafeSpawnPosition(int seedX, int seedY, int radius) {
        int attempts = 80;
        int bestX = seedX;
        int bestY = seedY;
        for (int i = 0; i < attempts; i++) {
            int x = rng.nextInt(WIDTH);
            int y = rng.nextInt(HEIGHT);
            if (isSafeFromPlayer(x, y, radius)) return new int[]{x, y};
            bestX = x; bestY = y;
        }
        return new int[]{bestX, bestY};
    }

    private void drawHUD() {
        showText("Puntaje: " + score, 90, 20);
        showText("Vidas: " + lives,   90, 40);
        showText("Oleada: " + wave,   90, 60);
    }

    private void drawTitleScreen() {
        buildStarfieldBackground();
        showCenteredMessage("ASTEROIDS\nENTER para comenzar", 42);
        showText("", 90, 20);
        showText("", 90, 40);
        showText("", 90, 60);
    }

    private void showCenteredMessage(String msg, int fontSize) {
        String[] lines = msg.split("\\n");
        int lineHeight = fontSize + 6;
        int startY = HEIGHT/2 - (lines.length * lineHeight)/2;
        for (int i = 0; i < lines.length; i++) {
            showText(lines[i], WIDTH/2 + 1, startY + i*lineHeight + 1);
            showText(lines[i], WIDTH/2,     startY + i*lineHeight);
        }
    }

    private void clearCenterMessage() {
        for (int dy = -3; dy <= 3; dy++) {
            showText("", WIDTH/2, HEIGHT/2 + dy*18);
        }
    }

    private void buildStarfieldBackground() {
        GreenfootImage bg = new GreenfootImage(WIDTH, HEIGHT);
        bg.setColor(greenfoot.Color.BLACK);
        bg.fill();
        int stars = 420;
        for (int i = 0; i < stars; i++) {
            int x = rng.nextInt(WIDTH);
            int y = rng.nextInt(HEIGHT);
            bg.setColor(greenfoot.Color.WHITE);
            bg.drawRect(x, y, 1, 1);
            if (rng.nextFloat() < 0.07f) bg.drawRect(x+1, y, 1, 1);
        }
        for (int i = 0; i < 12; i++) {
            int cx = rng.nextInt(WIDTH);
            int cy = rng.nextInt(HEIGHT);
            int rad = 80 + rng.nextInt(140);
            int alpha = 20 + rng.nextInt(30);
            greenfoot.Color haze = new greenfoot.Color(120 + rng.nextInt(80), 120 + rng.nextInt(80), 200, alpha);
            drawFilledCircle(bg, cx, cy, rad, haze);
        }
        setBackground(bg);
    }

    private void drawFilledCircle(GreenfootImage img, int cx, int cy, int r, greenfoot.Color col) {
        img.setColor(col);
        for (int y = -r; y <= r; y++) {
            int span = (int)Math.sqrt(r*r - y*y);
            img.drawLine(cx - span, cy + y, cx + span, cy + y);
        }
    }

    public int getScore() { return score; }
    public int getLives() { return lives; }
    public int getWave()  { return wave;  }
    public Random rng() { return rng; }
}

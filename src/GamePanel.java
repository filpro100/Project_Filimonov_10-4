import javax.swing.*;
import java.awt.*;

class GamePanel extends JPanel {
    private static final double GRAVITY = 19.62;
    private static final double PIXELS_PER_METER = 100;
    private static final double MOVE_ACCELERATION = 12;
    private static final double GROUND_MOVE_ACCELERATION = 16;
    private static final double GROUND_FRICTION = 45;
    private static final double JUMP_HEIGHT_BLOCKS = 4.2;
    private static final double MAX_HORIZONTAL_SPEED = 2.2;
    private static final double MAX_GROUND_SPEED = 3.0;
    private static final double BLOCK_PLACEMENT_CLEARANCE = 0.50;

    private final Image floor = Class1.loadImage("assets/platform-floor.png").getImage();
    private final Image blockImage = Class1.loadImage("assets/block.png").getImage();
    private final Player redPlayer = new Player(
            Class1.loadImage("assets/player.png").getImage(), true);
    private final Player bluePlayer = new Player(
            Class1.loadImage("assets/player-blue.png").getImage(), false);
    private final Timer timer;
    private Block[] blocks = new Block[64];
    private int blockCount;

    private long previousFrameTime;

    GamePanel() {
        setBackground(new Color(135, 206, 235));
        setFocusable(true);
        setupControls();

        timer = new Timer(16, event -> updateGame());
        timer.start();
    }

    private void setupControls() {
        bindKey(java.awt.event.KeyEvent.VK_W, false, () -> jump(redPlayer));
        bindKey(java.awt.event.KeyEvent.VK_S, false, () -> placeBlockUnderPlayer(redPlayer));
        bindKey(java.awt.event.KeyEvent.VK_A, false, () -> redPlayer.movingLeft = true);
        bindKey(java.awt.event.KeyEvent.VK_A, true, () -> redPlayer.movingLeft = false);
        bindKey(java.awt.event.KeyEvent.VK_D, false, () -> redPlayer.movingRight = true);
        bindKey(java.awt.event.KeyEvent.VK_D, true, () -> redPlayer.movingRight = false);

        bindKey(java.awt.event.KeyEvent.VK_UP, false, () -> jump(bluePlayer));
        bindKey(java.awt.event.KeyEvent.VK_DOWN, false, () -> placeBlockUnderPlayer(bluePlayer));
        bindKey(java.awt.event.KeyEvent.VK_LEFT, false, () -> bluePlayer.movingLeft = true);
        bindKey(java.awt.event.KeyEvent.VK_LEFT, true, () -> bluePlayer.movingLeft = false);
        bindKey(java.awt.event.KeyEvent.VK_RIGHT, false, () -> bluePlayer.movingRight = true);
        bindKey(java.awt.event.KeyEvent.VK_RIGHT, true, () -> bluePlayer.movingRight = false);
    }

    private void bindKey(int keyCode, boolean onRelease, Runnable action) {
        String name = keyCode + "-" + onRelease;
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(keyCode, 0, onRelease), name);
        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                action.run();
            }
        });
    }

    private void jump(Player player) {
        if (player.onGround) {
            player.velocityX = Math.max(-MAX_HORIZONTAL_SPEED,
                    Math.min(MAX_HORIZONTAL_SPEED, player.velocityX));
            player.velocityY = -jumpSpeed();
            player.onGround = false;
        }
    }

    private double jumpSpeed() {
        double height = JUMP_HEIGHT_BLOCKS * blockHeight();
        return Math.sqrt(2 * GRAVITY * height / PIXELS_PER_METER);
    }

    private void updateGame() {
        long now = System.nanoTime();
        if (previousFrameTime == 0) {
            initializePlayer(redPlayer, getWidth() / 10.0);
            initializePlayer(bluePlayer, getWidth() * 9 / 10.0);
            previousFrameTime = now;
            return;
        }

        double elapsed = Math.min((now - previousFrameTime) / 1000000000.0, 0.05);
        previousFrameTime = now;
        updatePlayer(redPlayer, elapsed);
        updatePlayer(bluePlayer, elapsed);
        repaint();
    }

    private void updatePlayer(Player player, double elapsed) {
        updateHorizontalVelocity(player, elapsed);
        player.velocityY += GRAVITY * elapsed;

        int height = blockHeight();
        int width = playerWidth(player, height);
        double moveX = player.velocityX * PIXELS_PER_METER * elapsed;
        double moveY = player.velocityY * PIXELS_PER_METER * elapsed;
        int steps = collisionSteps(moveX, moveY, height);
        double stepX = moveX / steps;
        double stepY = moveY / steps;
        boolean wasOnGround = player.onGround;
        player.onGround = false;

        for (int i = 0; i < steps; i++) {
            movePlayerHorizontally(player, stepX, width, height);
            movePlayerVertically(player, stepY, width, height, !wasOnGround);
        }
    }

    private void updateHorizontalVelocity(Player player, double elapsed) {
        double direction = (player.movingRight ? 1 : 0) - (player.movingLeft ? 1 : 0);
        if (direction != 0) {
            double acceleration = player.onGround
                    ? GROUND_MOVE_ACCELERATION : MOVE_ACCELERATION;
            double maxSpeed = player.onGround ? MAX_GROUND_SPEED : MAX_HORIZONTAL_SPEED;
            player.velocityX = Math.max(-maxSpeed,
                    Math.min(maxSpeed, player.velocityX + direction * acceleration * elapsed));
            player.facingRight = direction > 0;
        } else if (player.onGround) {
            double slowdown = GROUND_FRICTION * elapsed;
            if (Math.abs(player.velocityX) <= slowdown) {
                player.velocityX = 0;
            } else {
                player.velocityX -= Math.signum(player.velocityX) * slowdown;
            }
        }
    }

    private int collisionSteps(double moveX, double moveY, int playerHeight) {
        double maxStep = Math.max(1, playerHeight / 4.0);
        return Math.max(1, (int) Math.ceil(
                Math.max(Math.abs(moveX), Math.abs(moveY)) / maxStep));
    }

    private void movePlayerHorizontally(
            Player player, double movement, int playerWidth, int playerHeight) {
        double oldX = player.x;
        double newX = oldX + movement;
        newX = Math.max(playerWidth / 2.0,
                Math.min(getWidth() - playerWidth / 2.0, newX));

        for (int i = 0; i < blockCount; i++) {
            Block block = blocks[i];
            double left = block.column * blockWidth();
            double right = left + blockWidth();
            double top = blockTop(block);
            double bottom = top + blockHeight();
            boolean verticalOverlap = player.y < bottom && player.y + playerHeight > top;
            if (!verticalOverlap) {
                continue;
            }

            if (movement > 0 && oldX + playerWidth / 2.0 <= left
                    && newX + playerWidth / 2.0 > left) {
                newX = left - playerWidth / 2.0;
            } else if (movement < 0 && oldX - playerWidth / 2.0 >= right
                    && newX - playerWidth / 2.0 < right) {
                newX = right + playerWidth / 2.0;
            }
        }
        player.x = newX;
    }

    private void movePlayerVertically(
            Player player, double movement, int playerWidth,
            int playerHeight, boolean stopOnLanding) {
        double oldY = player.y;
        double newY = oldY + movement;
        double landingTop = groundTop();

        for (int i = 0; i < blockCount; i++) {
            Block block = blocks[i];
            double left = block.column * blockWidth();
            double right = left + blockWidth();
            double top = blockTop(block);
            double bottom = top + blockHeight();
            boolean horizontalOverlap = player.x + playerWidth / 2.0 > left
                    && player.x - playerWidth / 2.0 < right;
            if (!horizontalOverlap) {
                continue;
            }

            if (movement > 0 && oldY + playerHeight <= top
                    && newY + playerHeight >= top) {
                landingTop = Math.min(landingTop, top);
            } else if (movement < 0 && oldY >= bottom && newY <= bottom) {
                newY = bottom;
                player.velocityY = 0;
            }
        }

        if (movement > 0 && oldY + playerHeight <= landingTop
                && newY + playerHeight >= landingTop) {
            newY = landingTop - playerHeight;
            player.velocityY = 0;
            if (stopOnLanding) {
                player.velocityX = 0;
            }
            player.onGround = true;
        }
        player.y = newY;
    }

    private int blockHeight() {
        return Math.max(1, getHeight() / 24);
    }

    private int playerWidth(Player player, int playerHeight) {
        return Math.max(1,
                playerHeight * player.image.getWidth(null) / player.image.getHeight(null));
    }

    private int groundTop() {
        return (int) Math.round(getHeight() * 23.0 / 24.0);
    }

    private double blockWidth() {
        return getWidth() / (double) blockColumns();
    }

    private int blockColumns() {
        return Math.max(1, getWidth() / blockHeight());
    }

    private int blockTop(Block block) {
        return block.row * blockHeight();
    }

    private Block findBlock(int column, int row) {
        for (int i = 0; i < blockCount; i++) {
            Block block = blocks[i];
            if (block.column == column && block.row == row) {
                return block;
            }
        }
        return null;
    }

    private void placeBlockUnderPlayer(Player player) {
        if (!player.initialized || getWidth() == 0 || getHeight() == 0) {
            return;
        }

        int height = blockHeight();
        int row = player.velocityY < 0
                ? getPlacementRowWhileRising(player, height)
                : getPlacementRowWhileFalling(player, height);
        int column = Math.max(0, Math.min(
                (int) (player.x / blockWidth()), blockColumns() - 1));
        if (row < 0 || row >= 23 || findBlock(column, row) != null) {
            return;
        }

        Block newBlock = new Block(column, row);
        if (player.velocityY < 0 && !canRiseAboveBlock(player, newBlock, height)) {
            return;
        } else if (player.velocityY >= 0
                && !isHighEnoughToPlaceWhileFalling(player, newBlock, height)) {
            return;
        }
        addBlock(newBlock);
        repaint();
    }

    private void addBlock(Block block) {
        if (blockCount == blocks.length) {
            Block[] biggerBlocks = new Block[blocks.length * 2];
            for (int i = 0; i < blockCount; i++) {
                biggerBlocks[i] = blocks[i];
            }
            blocks = biggerBlocks;
        }
        blocks[blockCount] = block;
        blockCount++;
    }

    private int getPlacementRowWhileRising(Player player, int height) {
        return (int) Math.floor((player.y + height) / height);
    }

    private int getPlacementRowWhileFalling(Player player, int height) {
        double exactHeightInBlocks = (groundTop() - player.y - height) / height;
        int heightInBlocks = (int) Math.floor(exactHeightInBlocks);
        int groundRow = (int) Math.round(groundTop() / (double) height);
        return groundRow - heightInBlocks;
    }

    private boolean isHighEnoughToPlaceWhileFalling(Player player, Block block, int height) {
        double heightAboveBlockBottom = blockTop(block) + height - player.y;
        return heightAboveBlockBottom >= height * BLOCK_PLACEMENT_CLEARANCE;
    }

    private boolean canRiseAboveBlock(Player player, Block block, int height) {
        double riseNeeded = player.y + height - blockTop(block);
        if (riseNeeded <= 0) {
            return true;
        }
        double riseAvailable = player.velocityY * player.velocityY * PIXELS_PER_METER
                / (2 * GRAVITY);
        return riseAvailable >= riseNeeded;
    }

    private void initializePlayer(Player player, double startX) {
        int height = blockHeight();
        int width = playerWidth(player, height);
        player.x = Math.max(width / 2.0,
                Math.min(getWidth() - width / 2.0, startX));
        player.y = groundTop() - height;
        player.initialized = true;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }
        if (!redPlayer.initialized || !bluePlayer.initialized) {
            initializePlayer(redPlayer, getWidth() / 10.0);
            initializePlayer(bluePlayer, getWidth() * 9 / 10.0);
        }

        Graphics2D g = (Graphics2D) graphics.create();
        drawFloor(g);
        drawBlocks(g);
        drawPlayer(g, redPlayer);
        drawPlayer(g, bluePlayer);
        g.dispose();
    }

    private void drawFloor(Graphics2D g) {
        int top = groundTop();
        g.drawImage(floor, 0, top, getWidth(), getHeight() - top, this);
    }

    private void drawBlocks(Graphics2D g) {
        int height = blockHeight();
        double width = blockWidth();
        for (int i = 0; i < blockCount; i++) {
            Block placedBlock = blocks[i];
            int left = (int) Math.round(placedBlock.column * width);
            int right = (int) Math.round((placedBlock.column + 1) * width);
            g.drawImage(blockImage, left, blockTop(placedBlock),
                    right - left, height, this);
        }
    }

    private void drawPlayer(Graphics2D g, Player player) {
        int height = blockHeight();
        int width = playerWidth(player, height);
        int left = (int) (player.x - width / 2.0);

        if (player.facingRight == player.imageFacesRight) {
            g.drawImage(player.image, left, (int) player.y, width, height, this);
            return;
        }

        java.awt.geom.AffineTransform transform = new java.awt.geom.AffineTransform();
        transform.translate(player.x + width / 2.0, player.y);
        transform.scale(-1, 1);
        transform.scale(width / (double) player.image.getWidth(null),
                height / (double) player.image.getHeight(null));
        g.drawImage(player.image, transform, this);
    }
}

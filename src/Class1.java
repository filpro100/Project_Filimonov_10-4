import javax.swing.*;
import java.awt.*;

public class Class1 {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Class1::showMenu);
    }

    private static void showMenu() {
        JFrame frame = new JFrame("Игра");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setUndecorated(true);

        ImageIcon backgroundImage = loadImage("assets/menu-background.png");
        JPanel background = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                graphics.drawImage(backgroundImage.getImage(), 0, 0,
                        getWidth(), getHeight(), this);
            }
        };

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));

        JButton startButton = createButton("assets/start-button.png");
        JButton exitButton = createButton("assets/exit-button.png");
        buttons.add(startButton);
        buttons.add(Box.createRigidArea(new Dimension(0, 16)));
        buttons.add(exitButton);
        background.add(buttons);
        frame.setContentPane(background);

        startButton.addActionListener(event -> {
            frame.setContentPane(new GamePanel());
            frame.revalidate();
        });
        exitButton.addActionListener(event -> System.exit(0));

        GraphicsDevice screen = GraphicsEnvironment
                .getLocalGraphicsEnvironment().getDefaultScreenDevice();
        screen.setFullScreenWindow(frame);
    }

    private static ImageIcon loadImage(String path) {
        ImageIcon image = new ImageIcon(path);
        return image;
    }

    private static JButton createButton(String imagePath) {
        ImageIcon image = loadImage(imagePath);
        JButton button = new JButton(image);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setPreferredSize(new Dimension(image.getIconWidth(), image.getIconHeight()));
        button.setMaximumSize(new Dimension(image.getIconWidth(), image.getIconHeight()));
        button.setAlignmentX(JButton.CENTER_ALIGNMENT);
        return button;
    }

    private static class GamePanel extends JPanel {
        private static final double GRAVITY = 19.62;
        private static final double PIXELS_PER_METER = 100;
        private static final double MOVE_ACCELERATION = 12;
        private static final double GROUND_FRICTION = 45;
        private static final double JUMP_HEIGHT_BLOCKS = 4.2;
        private static final double MAX_HORIZONTAL_SPEED = 2.0;
        private static final double BLOCK_PLACEMENT_CLEARANCE = 0.50;

        private final Image floor = loadImage("assets/platform-floor.png").getImage();
        private final Image block = loadImage("assets/block.png").getImage();
        private final Image player = loadImage("assets/player.png").getImage();
        private final Timer timer;
        private final java.util.ArrayList<Block> blocks = new java.util.ArrayList<>();

        private double playerX;
        private double playerY;
        private double velocityX;
        private double velocityY;
        private long previousFrameTime;
        private boolean movingLeft;
        private boolean movingRight;
        private boolean facingRight = true;
        private boolean onGround = true;
        private boolean initialized;

        GamePanel() {
            setBackground(new Color(135, 206, 235));
            setFocusable(true);
            setupControls();

            timer = new Timer(16, event -> updateGame());
            timer.start();
        }

        private void setupControls() {
            bindKey(java.awt.event.KeyEvent.VK_W, false, this::jump);
            bindKey(java.awt.event.KeyEvent.VK_S, false, this::placeBlockUnderPlayer);
            bindMovementKey(java.awt.event.KeyEvent.VK_A, true);
            bindMovementKey(java.awt.event.KeyEvent.VK_D, false);
        }

        private void bindMovementKey(int keyCode, boolean left) {
            bindKey(keyCode, false, () -> {
                if (left) movingLeft = true;
                else movingRight = true;
            });
            bindKey(keyCode, true, () -> {
                if (left) movingLeft = false;
                else movingRight = false;
            });
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

        private void jump() {
            if (onGround) {
                velocityY = -jumpSpeed();
                onGround = false;
            }
        }

        private double jumpSpeed() {
            double height = JUMP_HEIGHT_BLOCKS * blockHeight();
            return Math.sqrt(2 * GRAVITY * height / PIXELS_PER_METER);
        }

        private void updateGame() {
            long now = System.nanoTime();
            if (previousFrameTime == 0) {
                if (!initialized) {
                    initializePlayer();
                }
                previousFrameTime = now;
                return;
            }
            double elapsed = Math.min((now - previousFrameTime) / 1_000_000_000.0, 0.05);
            previousFrameTime = now;

            updateHorizontalVelocity(elapsed);
            velocityY += GRAVITY * elapsed;
            int playerHeight = blockHeight();
            int playerWidth = playerWidth(playerHeight);
            double moveX = velocityX * PIXELS_PER_METER * elapsed;
            double moveY = velocityY * PIXELS_PER_METER * elapsed;
            int steps = collisionSteps(moveX, moveY, playerHeight);
            double stepX = moveX / steps;
            double stepY = moveY / steps;
            onGround = false;

            for (int i = 0; i < steps; i++) {
                movePlayerHorizontally(stepX, playerWidth, playerHeight);
                movePlayerVertically(stepY, playerWidth, playerHeight);
            }
            repaint();
        }

        private void updateHorizontalVelocity(double elapsed) {
            double direction = (movingRight ? 1 : 0) - (movingLeft ? 1 : 0);
            if (direction != 0) {
                velocityX = Math.max(-MAX_HORIZONTAL_SPEED,
                        Math.min(MAX_HORIZONTAL_SPEED,
                                velocityX + direction * MOVE_ACCELERATION * elapsed));
                facingRight = direction > 0;
            } else if (onGround) {
                double slowdown = GROUND_FRICTION * elapsed;
                if (Math.abs(velocityX) <= slowdown) {
                    velocityX = 0;
                } else {
                    velocityX -= Math.signum(velocityX) * slowdown;
                }
            }
        }

        private int collisionSteps(double moveX, double moveY, int playerHeight) {
            double maxStep = Math.max(1, playerHeight / 4.0);
            return Math.max(1, (int) Math.ceil(
                    Math.max(Math.abs(moveX), Math.abs(moveY)) / maxStep));
        }

        private void movePlayerHorizontally(double movement, int playerWidth, int playerHeight) {
            double oldX = playerX;
            double newX = oldX + movement;
            newX = Math.max(playerWidth / 2.0,
                    Math.min(getWidth() - playerWidth / 2.0, newX));

            for (Block block : blocks) {
                double left = block.column * blockWidth();
                double right = left + blockWidth();
                double top = blockTop(block);
                double bottom = top + blockHeight();
                boolean verticalOverlap = playerY < bottom && playerY + playerHeight > top;
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
            playerX = newX;
        }

        private void movePlayerVertically(double movement, int playerWidth, int playerHeight) {
            double oldY = playerY;
            double newY = oldY + movement;
            double landingTop = groundTop();

            for (Block block : blocks) {
                double left = block.column * blockWidth();
                double right = left + blockWidth();
                double top = blockTop(block);
                double bottom = top + blockHeight();
                boolean horizontalOverlap = playerX + playerWidth / 2.0 > left
                        && playerX - playerWidth / 2.0 < right;
                if (!horizontalOverlap) {
                    continue;
                }

                if (movement > 0 && oldY + playerHeight <= top
                        && newY + playerHeight >= top) {
                    landingTop = Math.min(landingTop, top);
                } else if (movement < 0 && oldY >= bottom && newY <= bottom) {
                    newY = bottom;
                    velocityY = 0;
                }
            }

            if (movement > 0 && oldY + playerHeight <= landingTop
                    && newY + playerHeight >= landingTop) {
                newY = landingTop - playerHeight;
                velocityY = 0;
                onGround = true;
            }
            playerY = newY;
        }

        private int blockHeight() {
            return Math.max(1, getHeight() / 24);
        }

        private int playerWidth(int playerHeight) {
            return Math.max(1, playerHeight * player.getWidth(null) / player.getHeight(null));
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
            for (Block block : blocks) {
                if (block.column == column && block.row == row) {
                    return block;
                }
            }
            return null;
        }

        private void placeBlockUnderPlayer() {
            if (!initialized || getWidth() == 0 || getHeight() == 0) {
                return;
            }

            double exactHeightInBlocks =
                    (groundTop() - playerY - blockHeight()) / blockHeight();
            int heightInBlocks = (int) Math.floor(exactHeightInBlocks);
            int groundRow = (int) Math.round(groundTop() / (double) blockHeight());
            int row = groundRow - heightInBlocks;
            int column = Math.max(0, Math.min(
                    (int) (playerX / blockWidth()), blockColumns() - 1));
            if (row < 0 || row >= 23 || findBlock(column, row) != null) {
                return;
            }

            Block newBlock = new Block(column, row);
            double heightAboveBlockBottom =
                    blockTop(newBlock) + blockHeight() - playerY;
            if (heightAboveBlockBottom < blockHeight() * BLOCK_PLACEMENT_CLEARANCE) {
                return;
            }
            blocks.add(newBlock);
            repaint();
        }

        private void initializePlayer() {
            int playerHeight = blockHeight();
            playerX = getWidth() / 2.0;
            playerY = groundTop() - playerHeight;
            initialized = true;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (getWidth() == 0 || getHeight() == 0) {
                return;
            }
            if (!initialized) {
                initializePlayer();
            }
            Graphics2D g = (Graphics2D) graphics.create();
            drawFloor(g);
            drawBlocks(g);
            drawPlayer(g);
            g.dispose();
        }

        private void drawFloor(Graphics2D g) {
            int top = groundTop();
            g.drawImage(floor, 0, top, getWidth(), getHeight() - top, this);
        }

        private void drawBlocks(Graphics2D g) {
            int height = blockHeight();
            double width = blockWidth();
            for (Block placedBlock : blocks) {
                int left = (int) Math.round(placedBlock.column * width);
                int right = (int) Math.round((placedBlock.column + 1) * width);
                g.drawImage(block, left, blockTop(placedBlock),
                        right - left, height, this);
            }
        }

        private void drawPlayer(Graphics2D g) {
            int height = blockHeight();
            int width = playerWidth(height);
            int left = (int) (playerX - width / 2.0);

            if (facingRight) {
                g.drawImage(player, left, (int) playerY, width, height, this);
                return;
            }

            java.awt.geom.AffineTransform transform = new java.awt.geom.AffineTransform();
            transform.translate(playerX + width / 2.0, playerY);
            transform.scale(-1, 1);
            transform.scale(width / (double) player.getWidth(null),
                    height / (double) player.getHeight(null));
            g.drawImage(player, transform, this);
        }
    }

    private static class Block {
        final int column;
        final int row;

        Block(int column, int row) {
            this.column = column;
            this.row = row;
        }
    }
}

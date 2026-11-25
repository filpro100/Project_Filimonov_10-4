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

    static ImageIcon loadImage(String path) {
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
}

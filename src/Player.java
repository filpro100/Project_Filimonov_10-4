import java.awt.Image;

class Player {
    final Image image;
    final boolean imageFacesRight;
    double x;
    double y;
    double velocityX;
    double velocityY;
    boolean movingLeft;
    boolean movingRight;
    boolean facingRight;
    boolean onGround = true;
    boolean initialized;

    Player(Image image, boolean imageFacesRight) {
        this.image = image;
        this.imageFacesRight = imageFacesRight;
        this.facingRight = imageFacesRight;
    }
}

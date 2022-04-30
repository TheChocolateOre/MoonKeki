package moonkeki.render;

import java.awt.geom.AffineTransform;

//TODO Add drawCommand() method
//TODO Remove the generic argument from DrawCommand's'
public abstract class PixmapRenderer extends Renderer {

    interface Builder<T extends Builder<T>> {
        T ofCanvas(Canvas canvas);
        T ofShader(ShaderProgram shader);
        T ofTransform(AffineTransform transform);
    }

    public interface DrawCommand {
        DrawCommand ofPixmap(Pixmap pixmap);
        DrawCommand atX(double x);
        DrawCommand atY(double y);
        DrawCommand atPosition(double x, double y);
        DrawCommand ofWidth(double width);
        DrawCommand ofHeight(double height);
        DrawCommand ofSize(double width, double height);
        DrawCommand ofMirroredX();
        DrawCommand ofMirroredY();
        DrawCommand unmirrorX();
        DrawCommand unmirrorY();
        DrawCommand withTransform(AffineTransform transform);
        void process();
    }

    public static abstract class AbstractDrawCommand implements DrawCommand {
        Pixmap pixmap;
        double x;
        double y;
        Double width;
        Double height;
        boolean isMirroredX;
        boolean isMirroredY;
        AffineTransform transform = new AffineTransform();

        AbstractDrawCommand() {}

        public DrawCommand ofPixmap(Pixmap pixmap) {
            this.pixmap = pixmap;
            if (null == this.width) {
                this.width = (double) pixmap.getWidth();
            }

            if (null == this.height) {
                this.height = (double) pixmap.getHeight();
            }

            return this;
        }

        public DrawCommand atX(double x) {
            return this.atPosition(x, this.y);
        }

        public DrawCommand atY(double y) {
            return this.atPosition(this.x, y);
        }

        public DrawCommand atPosition(double x, double y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public DrawCommand ofWidth(double width) {
            return this.ofSize(width, this.height);
        }

        public DrawCommand ofHeight(double height) {
            return this.ofSize(this.width, height);
        }

        public DrawCommand ofSize(double width, double height) {
            if (width < 0.0) {
                throw new IllegalArgumentException("Argument width can't " +
                        "be negative.");
            }//end if

            if (height < 0.0) {
                throw new IllegalArgumentException("Argument height " +
                        "can't be negative.");
            }//end if

            this.width = width;
            this.height = height;
            return this;
        }

        public DrawCommand ofMirroredX() {
            this.isMirroredX = true;
            return this;
        }

        public DrawCommand ofMirroredY() {
            this.isMirroredY = true;
            return this;
        }

        public DrawCommand unmirrorX() {
            this.isMirroredX = false;
            return this;
        }

        public DrawCommand unmirrorY() {
            this.isMirroredY = false;
            return this;
        }

        public DrawCommand withTransform(AffineTransform transform) {
            this.transform = transform;
            return this;
        }
    }

    public abstract DrawCommand drawCommand();

}

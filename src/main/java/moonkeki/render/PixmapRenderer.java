package moonkeki.render;

import java.awt.geom.AffineTransform;
import java.util.Objects;

public abstract class PixmapRenderer extends Renderer {

    public interface DrawCommand {
        static DrawCommand instance() {
            return new AbstractDrawCommand();
        }

        static DrawCommand unmodifiable(DrawCommand drawCommand) {
            return drawCommand.getClass() == UnmodifiableDrawCommand.class ?
                   drawCommand :
                   new UnmodifiableDrawCommand(drawCommand);
        }

        default DrawCommand ofSize(double width, double height) {
            return this.ofWidth(width).ofHeight(height);
        }

        DrawCommand ofPixmap(Pixmap pixmap);
        DrawCommand atX(double x);
        DrawCommand atY(double y);
        DrawCommand atPosition(double x, double y);
        DrawCommand ofWidth(double width);
        DrawCommand ofHeight(double height);
        DrawCommand ofMirroredX();
        DrawCommand ofMirroredY();
        DrawCommand unmirrorX();
        DrawCommand unmirrorY();
        DrawCommand withTransform(AffineTransform transform);

        Pixmap getPixmap();
        double getX();
        double getY();
        double getWidth();
        double getHeight();
        boolean isXMirrored();
        boolean isYMirrored();
        AffineTransform getTransform();
    }

    public interface AttachedDrawCommand extends DrawCommand {
        default AttachedDrawCommand ofSize(double width, double height) {
            return this.ofWidth(width).ofHeight(height);
        }

        AttachedDrawCommand ofPixmap(Pixmap pixmap);
        AttachedDrawCommand atX(double x);
        AttachedDrawCommand atY(double y);
        AttachedDrawCommand atPosition(double x, double y);
        AttachedDrawCommand ofWidth(double width);
        AttachedDrawCommand ofHeight(double height);
        AttachedDrawCommand ofMirroredX();
        AttachedDrawCommand ofMirroredY();
        AttachedDrawCommand unmirrorX();
        AttachedDrawCommand unmirrorY();
        AttachedDrawCommand withTransform(AffineTransform transform);
        void process();
    }

    interface Builder<T extends Builder<T>> {
        T ofCanvas(Canvas canvas);
        T ofShader(ShaderProgram shader);
        T ofTransform(AffineTransform transform);
    }

    static class AbstractDrawCommand implements DrawCommand {
        Pixmap pixmap = Pixmap.VOID;
        double x;
        double y;
        Double width;
        Double height;
        boolean isMirroredX;
        boolean isMirroredY;
        AffineTransform transform = new AffineTransform();

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
            if (width < 0.0) {
                throw new IllegalArgumentException("Argument width can't " +
                        "be negative.");
            }//end if

            this.width = width;
            return this;
        }

        public DrawCommand ofHeight(double height) {
            if (height < 0.0) {
                throw new IllegalArgumentException("Argument height " +
                        "can't be negative.");
            }//end if

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
            this.transform.setTransform(transform);
            return this;
        }

        @Override
        public Pixmap getPixmap() {
            return this.pixmap;
        }

        @Override
        public double getX() {
            return this.x;
        }

        @Override
        public double getY() {
            return this.y;
        }

        @Override
        public double getWidth() {
            return this.width != null ? this.width : 0;
        }

        @Override
        public double getHeight() {
            return this.height != null ? this.height : 0;
        }

        @Override
        public boolean isXMirrored() {
            return this.isMirroredX;
        }

        @Override
        public boolean isYMirrored() {
            return this.isMirroredY;
        }

        @Override
        public AffineTransform getTransform() {
            return new AffineTransform(this.transform);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            return obj instanceof AbstractDrawCommand c &&
                   Objects.equals(this.pixmap, c.pixmap) &&
                   this.x == c.x &&
                   this.y == c.y &&
                   Objects.equals(this.width, c.width) &&
                   Objects.equals(this.height, c.height) &&
                   this.isMirroredX == c.isMirroredX &&
                   this.isMirroredY == c.isMirroredY &&
                   this.transform.equals(c.transform);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.pixmap,
                                this.x, this.y,
                                this.width, this.height,
                                this.isMirroredX, this.isMirroredY,
                                this.transform);
        }

        @Override
        public String toString() {
            return ("DrawCommand[pixmap=%s, x=%s, y=%s, width=%s, height=%s," +
                    "mirroredX=%b, mirroredY=%b, transform=%s]").formatted(
                    this.pixmap,
                    this.x, this.y,
                    this.getWidth(), this.getHeight(),
                    this.isMirroredX, this.isMirroredY,
                    this.transform);
        }
    }

    private static final class UnmodifiableDrawCommand implements DrawCommand {
        final DrawCommand DRAW_COMMAND;
        
        private UnmodifiableDrawCommand(DrawCommand drawCommand) {
            this.DRAW_COMMAND = drawCommand;
        }

        @Override
        public DrawCommand ofPixmap(Pixmap pixmap) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DrawCommand atX(double x) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DrawCommand atY(double y) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DrawCommand atPosition(double x, double y) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DrawCommand ofWidth(double width) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DrawCommand ofHeight(double height) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DrawCommand ofMirroredX() {
            throw new UnsupportedOperationException();
        }

        @Override
        public DrawCommand ofMirroredY() {
            throw new UnsupportedOperationException();
        }

        @Override
        public DrawCommand unmirrorX() {
            throw new UnsupportedOperationException();
        }

        @Override
        public DrawCommand unmirrorY() {
            throw new UnsupportedOperationException();
        }

        @Override
        public DrawCommand withTransform(AffineTransform transform) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Pixmap getPixmap() {
            return this.DRAW_COMMAND.getPixmap();
        }

        @Override
        public double getX() {
            return this.DRAW_COMMAND.getX();
        }

        @Override
        public double getY() {
            return this.DRAW_COMMAND.getY();
        }

        @Override
        public double getWidth() {
            return this.DRAW_COMMAND.getWidth();
        }

        @Override
        public double getHeight() {
            return this.DRAW_COMMAND.getHeight();
        }

        @Override
        public boolean isXMirrored() {
            return this.DRAW_COMMAND.isXMirrored();
        }

        @Override
        public boolean isYMirrored() {
            return this.DRAW_COMMAND.isYMirrored();
        }

        @Override
        public AffineTransform getTransform() {
            return this.DRAW_COMMAND.getTransform();
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || this.DRAW_COMMAND.equals(obj);
        }

        @Override
        public int hashCode() {
            return this.DRAW_COMMAND.hashCode();
        }

        @Override
        public String toString() {
            return this.DRAW_COMMAND.toString();
        }
    }

    public abstract AttachedDrawCommand drawCommand();
    public abstract void process(DrawCommand drawCommand);

}

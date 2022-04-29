package moonkeki.render;

import java.awt.geom.AffineTransform;

public abstract class PixmapRenderer extends Renderer {

    interface Builder<T extends Builder<T>> {
        T ofCanvas(Canvas canvas);
        T ofShader(ShaderProgram shader);
        T ofTransform(AffineTransform transform);
    }

    public static abstract class DrawCommand<T extends DrawCommand<T>> {
        Pixmap pixmap;
        double x;
        double y;
        Double width;
        Double height;
        boolean isMirroredX;
        boolean isMirroredY;
        AffineTransform transform = new AffineTransform();

        DrawCommand() {}

        public T ofPixmap(Pixmap pixmap) {
            this.pixmap = pixmap;
            if (null == this.width) {
                this.width = (double) pixmap.getWidth();
            }

            if (null == this.height) {
                this.height = (double) pixmap.getHeight();
            }

            return this.getThis();
        }

        public T atX(double x) {
            return this.atPosition(x, this.y);
        }

        public T atY(double y) {
            return this.atPosition(this.x, y);
        }

        public T atPosition(double x, double y) {
            this.x = x;
            this.y = y;
            return this.getThis();
        }

        public T ofWidth(double width) {
            return this.ofSize(width, this.height);
        }

        public T ofHeight(double height) {
            return this.ofSize(this.width, height);
        }

        public T ofSize(double width, double height) {
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
            return this.getThis();
        }

        public T ofMirroredX() {
            this.isMirroredX = true;
            return this.getThis();
        }

        public T ofMirroredY() {
            this.isMirroredY = true;
            return this.getThis();
        }

        public T unmirrorX() {
            this.isMirroredX = false;
            return this.getThis();
        }

        public T unmirrorY() {
            this.isMirroredY = false;
            return this.getThis();
        }

        public T withTransform(AffineTransform transform) {
            this.transform = transform;
            return this.getThis();
        }

        abstract T getThis();
    }

    public static DrawCommand<?> createDrawCommand() {
        class DrawCmd extends DrawCommand<DrawCmd> {
            @Override
            protected DrawCmd getThis() {
                return this;
            }
        }
        return new DrawCmd();
    }

    public abstract void process(DrawCommand<?> drawCommand);

}

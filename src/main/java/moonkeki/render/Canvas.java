package moonkeki.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.awt.*;

/**
 * A destination of drawing operations, that has a 2-dimensional state of colors
 * (texels). Its characteristics are:
 * <ol>
 *     <li>Size (width, height) in texels.</li>
 *     <li>Origin, located at the bottom left corner.</li>
 * </ol>
 */
public abstract class Canvas {

    public record Size(int width, int height) {
        public Size {
            if (width < 0) {
                throw new IllegalArgumentException("Argument width can't be " +
                        "negative.");
            }

            if (height < 0) {
                throw new IllegalArgumentException("Argument height can't be " +
                        "negative.");
            }
        }
    }

    public static final Canvas VOID = new Canvas() {
        @Override
        public int getWidth() {
            return 0;
        }

        @Override
        public int getHeight() {
            return 0;
        }

        @Override
        public Canvas subRegion(int x, int y, int width, int height) {
            if (0 == x && 0 == y && this.getWidth() == width &&
                                    this.getHeight() == height) {
                return this;
            }//end if

            throw new IllegalArgumentException();
        }

        @Override
        Object getBackend() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isVoid() {
            return true;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return "Canvas.VOID";
        }

        @Override
        int getXOffset() {
            throw new UnsupportedOperationException();
        }

        @Override
        int getYOffset() {
            throw new UnsupportedOperationException();
        }

        @Override
        void clear(Color color, int framebufferId) {}

        @Override
        void copyTo(Pixmap destination, int sourceFramebufferId) {
            if (destination.getWidth() != 0 || destination.getHeight() != 0) {
                throw new IllegalArgumentException("Argument Pixmap " +
                        "destination must have 0 width and height.");
            }//end if
        }
    };

    //TODO Maybe make it a final package-private instance method of Canvas?
    static void validateRegion(Canvas canvas, int x, int y,
                               int width, int height) {
        if (x < 0) {
            throw new IllegalArgumentException("Argument x can't be " +
                    "negative.");
        }//end if

        if (y < 0) {
            throw new IllegalArgumentException("Argument y can't be " +
                    "negative.");
        }//end if

        if (width < 0) {
            throw new IllegalArgumentException("Argument width can't be " +
                    "negative.");
        }//end if

        final Size CANVAS_SIZE = canvas.getSize();
        if (Math.addExact(x, width) > CANVAS_SIZE.width) {
            throw new IllegalArgumentException("Arguments x + " +
                    "canvas.getWidth() can't be greater than the width of " +
                    "argument canvas.");
        }//end if

        if (height < 0) {
            throw new IllegalArgumentException("Argument height can't be " +
                    "negative.");
        }//end if

        if (Math.addExact(y, height) > CANVAS_SIZE.height) {
            throw new IllegalArgumentException("Arguments y + " +
                    "canvas.getHeight() can't be greater than the height of " +
                    "argument canvas.");
        }//end if
    }

    public Size getSize() {
        return new Size(this.getWidth(), this.getHeight());
    }

    public void clear(Color color) {
        final int CANVAS_FRAMEBUFFER_ID = GL30.glGenFramebuffers();
        this.clear(color, CANVAS_FRAMEBUFFER_ID);
        GL30.glDeleteFramebuffers(CANVAS_FRAMEBUFFER_ID);
    }

    public boolean isVoid() {
        return this.getWidth() == 0 || this.getHeight() == 0;
    }

    void setup(int framebufferId) {
        final Size SIZE = this.getSize();
        GL11.glViewport(this.getXOffset(), this.getYOffset(),
                        SIZE.width, SIZE.height);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(this.getXOffset(), this.getYOffset(),
                       SIZE.width, SIZE.height);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebufferId);
    }

    void clear(Color color, int framebufferId) {
        this.setup(framebufferId);
        GL11.glClearColor(color.getRed() / 255.0f,
                          color.getGreen() / 255.0f,
                          color.getBlue() / 255.0f,
                          color.getAlpha() / 255.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }

    public abstract int getWidth();
    public abstract int getHeight();
    public abstract Canvas subRegion(int x, int y, int width, int height);
    abstract int getXOffset();
    abstract int getYOffset();
    //Throws if destination is of different size
    abstract void copyTo(Pixmap destination, int sourceFramebufferId);
    abstract Object getBackend();

}//end class Canvas

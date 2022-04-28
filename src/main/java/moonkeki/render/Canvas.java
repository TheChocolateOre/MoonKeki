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
 *
 * <p>{@snippet :
 * Canvas c = WindowRegion.WINDOW;
 * try (InstantRenderer r = InstantRenderer.builder()
 *                                         .ofCanvas(c)
 *                                         .shortLived()) {
 *     .
 *     .
 * }
 * }
 *
 * The above snippet creates an {@link InstantRenderer} to draw onto {@link
 * Canvas} {@code c} that happens to be the application window on the screen.
 *
 * <p>A {@link Canvas} might also be distinguished based on being <i>void</i> or
 * not. A <i>void</i> {@link Canvas} has an area of 0, i.e. a 0 width <strong>or
 * </strong> height. Consequently, it has no texel state, and any drawing
 * operations on it, will have no effect.
 */
public abstract class Canvas {

    /**
     * Describes the width/height of a {@link Canvas} in texels.
     * @param width The width of a {@link Canvas} in texels. Can't be negative.
     * @param height The height of a {@link Canvas} in texels. Can't be
     *               negative.
     */
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

    /**
     * A {@link Canvas} without a texel state. All drawing operations on it
     * won't have any effect.
     *
     * <p>Its {@link Canvas#isVoid()} method will always return {@code true},
     * but not to be confused with this instance. That is: let c1, c2 {@link
     * Canvas} instances, then if their {@link Canvas#isVoid()} methods return
     * {@code true}, that does not imply that {@code c1 == c2} or even that
     * {@code c1.equals(c2)}. {@link Canvas#isVoid()} is concerned of whether a
     * {@link Canvas} has an area of 0, i.e. a width <strong>or</strong> height
     * of 0. This instance satisfies the above condition as of 0 width <strong>
     * and</strong> height.
     *
     */
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

    /**
     * Gets the size (width, height) of this {@link Canvas}.
     * @return The size of this {@link Canvas}.
     */
    public Size getSize() {
        return new Size(this.getWidth(), this.getHeight());
    }

    /**
     * Replaces the color of every texel of this {@link Canvas}, with a given
     * {@link Color}.
     * @param color The new texel color of this {@link Canvas}.
     */
    public void clear(Color color) {
        final int CANVAS_FRAMEBUFFER_ID = GL30.glGenFramebuffers();
        this.clear(color, CANVAS_FRAMEBUFFER_ID);
        GL30.glDeleteFramebuffers(CANVAS_FRAMEBUFFER_ID);
    }

    /**
     * Indicates if this {@link Canvas} has an area of 0, i.e. a 0 width
     * <strong>or</strong> height.
     *
     * <p>This method should not be confused with {@link Canvas#VOID}. Many
     * instances can be rendered as <i>void</i> as long as they have an area of
     * 0. Let c1, c2 {@link Canvas} instances, then if this method returns
     * {@code true} on both of them, that does not imply that {@code c1 == c2}
     * or even that {@code c1.equals(c2)}.
     * @return {@code true} if this {@link Canvas} has an area of 0, i.e. a 0
     * width <strong>or</strong> height, otherwise {@code false}.
     */
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

    /**
     * Gets the width of this {@link Canvas} in texels.
     * @return The width of this {@link Canvas} in texels.
     */
    public abstract int getWidth();

    /**
     * Gets the height of this {@link Canvas} in texels.
     * @return The height of this {@link Canvas} in texels.
     */
    public abstract int getHeight();

    /**
     * Specifies a {@link Canvas}, as a <i>lightweight view</i>, based on a
     * subregion of this {@link Canvas}. The subregion starts at position
     * ({@code x}, {@code y}) relative to the origin of this {@link Canvas} and
     * has a size of ({@code width}, {@code height}) in texels.
     * @param x The x-coordinate, relative to the origin of this {@link Canvas},
     *          that the subregion starts from.
     * @param y The y-coordinate, relative to the origin of this {@link Canvas},
     *          that the subregion starts from.
     * @param width The width of the subregion in texels. Can be 0.
     * @param height The height of the subregion in texels. Can be 0.
     * @return A view {@link Canvas} of the specified region within this {@link
     *         Canvas}. May or may not be a newly created instance.
     * @throws IllegalArgumentException If the given arguments do not specify a
     * subregion of this {@link Canvas}. The width and height can't be negative.
     */
    public abstract Canvas subRegion(int x, int y, int width, int height);

    abstract int getXOffset();
    abstract int getYOffset();
    //Throws if destination is of different size
    abstract void copyTo(Pixmap destination, int sourceFramebufferId);
    abstract Object getBackend();

}

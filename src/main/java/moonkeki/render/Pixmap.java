package moonkeki.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.awt.*;
import java.util.Objects;

/**
 * Represents a region of a 2d table of colors that resides inside the GPU
 * memory. The origin (0, 0) is in the bottom left corner, with the y-axis
 * (rows) pointing upwards and the x-axis (columns) pointing rightwards.
 * @apiNote Trying to use any method of a {@link Pixmap} that its {@link
 * Pixmap#isClosed()} method returns {@code true}, will result in {@link
 * IllegalStateException}.
 */
public abstract class Pixmap extends Canvas {

    /**
     * A {@link Pixmap} implementation that wraps a {@link Texture}.
     */
    private static final class Impl extends Pixmap {
        /**
         * The underlying {@link Texture} of this {@link Impl}.
         */
        private final Texture TEXTURE;

        /**
         * The minimum texel column (included), in the x-axis of the {@link
         * Texture} this {@link Impl} region starts from.
         */
        private final int X_OFFSET;

        /**
         * The minimum texel row (included), in the y-axis of the {@link
         * Texture} this {@link Impl} region starts from.
         */
        private final int Y_OFFSET;

        private final int WIDTH;
        private final int HEIGHT;

        public Impl(Texture texture, int xOffset, int yOffset,
                                     int width, int height) {
            this.TEXTURE = texture;
            this.X_OFFSET = xOffset;
            this.Y_OFFSET = yOffset;
            this.WIDTH = width;
            this.HEIGHT = height;
        }

        @Override
        public int getWidth() {
            this.ensureOpen();
            return this.WIDTH;
        }

        @Override
        public int getHeight() {
            this.ensureOpen();
            return this.HEIGHT;
        }

        @Override
        public boolean isClosed() {
            return this.TEXTURE.isClosed();
        }

        @Override
        Texture getTexture() {
            this.ensureOpen();
            return this.TEXTURE;
        }

        @Override
        int getXOffset() {
            this.ensureOpen();
            return this.X_OFFSET;
        }

        @Override
        int getYOffset() {
            this.ensureOpen();
            return this.Y_OFFSET;
        }

        /**
         * Ensures that this {@link Impl} is open, i.e. {@link #isClosed()}
         * returns {@code false}, by throwing an exception if its closed. If
         * this {@link Impl} is open, this method does nothing.
         * @throws IllegalStateException If {@link #isClosed()} returns {@code
         * true}.
         */
        private void ensureOpen() throws IllegalStateException {
            if (this.isClosed()) {
                throw new IllegalStateException("The underlying Texture of " +
                        "this Pixmap is closed.");
            }//end if
        }
    }

    /**
     * Represents a {@link Pixmap}, that has a region area of 0 texels^2, i.e.
     * contains no colors.
     */
    public static final Pixmap VOID = new Pixmap() {
        @Override
        public int getWidth() {
            return 0;
        }

        @Override
        public int getHeight() {
            return 0;
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return "Pixmap.VOID";
        }

        @Override
        public boolean isVoid() {
            return true;
        }

        @Override
        Texture getTexture() {
            throw new UnsupportedOperationException();
        }

        @Override
        void setup(int framebufferId) {
            throw new UnsupportedOperationException();
        }

        @Override
        void clear(Color color, int framebufferId) {}

        @Override
        int getXOffset() {
            throw new UnsupportedOperationException();
        }

        @Override
        int getYOffset() {
            throw new UnsupportedOperationException();
        }

        @Override
        float getMinU() {
            throw new UnsupportedOperationException();
        }

        @Override
        float getMaxU() {
            throw new UnsupportedOperationException();
        }

        @Override
        float getMinV() {
            throw new UnsupportedOperationException();
        }

        @Override
        float getMaxV() {
            throw new UnsupportedOperationException();
        }

        @Override
        void copyTo(Pixmap destination, int sourceFramebufferId) {
            if (destination.getWidth() != 0 || destination.getHeight() != 0) {
                throw new IllegalArgumentException("Argument Pixmap " +
                        "destination must have 0 width and height.");
            }//end if
        }
    };

    public Pixmap subRegion(int x, int y, int width, int height) {
        if (0 == x && 0 == y && this.getWidth() == width &&
                                this.getHeight() == height) {
            return this;
        }//end if

        Canvas.validateRegion(this, x, y, width, height);
        return new Pixmap.Impl(this.getTexture(),
                               this.getXOffset() + x,
                               this.getYOffset() + y,
                               width, height);
    }

    /**
     * Indicates if this {@link Pixmap} is equal to a given {@link Object}. Two
     * {@link Pixmap}s are said to be equal iff they have the same id and
     * represent the same region in their underlying {@link Texture}.
     * @apiNote For performance reasons, this method will return {@code false},
     * if two {@link Pixmap}s have different ids, even though they could contain
     * the same colors in the same order. So, a {@code false} value does not
     * mean that two {@link Pixmap}s have different colors, but a {@code true}
     * value means that they have the same colors in the same order.
     * @param obj An {@link Object} to check if it is equal to this {@link
     * Pixmap}.
     * @return {@code true} if this {@link Pixmap} is equal with obj, otherwise
     * false.
     * @throws IllegalStateException If this {@link Pixmap} is closed, i.e.
     * {@link #isClosed()} returns {@code true}.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (Pixmap.VOID == obj) {
            return false;
        }

        return obj instanceof Pixmap p &&
               this.getTexture().equals(p.getTexture()) &&
               this.getXOffset() == p.getXOffset() &&
               this.getYOffset() == p.getYOffset() &&
               this.getWidth() == p.getWidth() &&
               this.getHeight() == p.getHeight();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getTexture(),
                            this.getXOffset(), this.getYOffset(),
                            this.getWidth(), this.getHeight());
    }

    @Override
    public String toString() {
        return "%s[x=%d, y=%d, width=%d, height=%d]".formatted(
                this.getTexture(),
                this.getXOffset(), this.getYOffset(),
                this.getWidth(), this.getHeight());
    }

    @Override
    Object getBackend() {
        return this.getTexture();
    }

    /**
     * Gets the minimum texture coordinate in the x-axis, that defines the
     * region of this {@link Pixmap} on the underlying OpenGL texture.
     * @return The minimum texture coordinate in the x-axis, that defines the
     * region of this {@link Pixmap} on the underlying OpenGL texture.
     * @throws IllegalStateException If this {@link Pixmap} is closed, i.e.
     * {@link #isClosed()} returns {@code true}.
     */
    float getMinU() {
        return (float) this.getXOffset() / this.getTexture().getWidth();
    }

    /**
     * Gets the maximum texture coordinate in the x-axis, that defines the
     * region of this {@link Pixmap} on the underlying OpenGL texture.
     * @return The maximum texture coordinate in the x-axis, that defines the
     * region of this {@link Pixmap} on the underlying OpenGL texture.
     * @throws IllegalStateException If this {@link Pixmap} is closed, i.e.
     * {@link #isClosed()} returns {@code true}.
     */
    float getMaxU() {
        return (float) (this.getXOffset() + this.getWidth()) /
                        this.getTexture().getWidth();
    }

    /**
     * Gets the minimum texture coordinate in the y-axis, that defines the
     * region of this {@link Pixmap} on the underlying OpenGL texture.
     * @return The minimum texture coordinate in the y-axis, that defines the
     * region of this {@link Pixmap} on the underlying OpenGL texture.
     * @throws IllegalStateException If this {@link Pixmap} is closed, i.e.
     * {@link #isClosed()} returns {@code true}.
     */
    float getMinV() {
        return (float) this.getYOffset() / this.getTexture().getHeight();
    }

    /**
     * Gets the maximum texture coordinate in the y-axis, that defines the
     * region of this {@link Pixmap} on the underlying OpenGL texture.
     * @return The maximum texture coordinate in the y-axis, that defines the
     * region of this {@link Pixmap} on the underlying OpenGL texture.
     * @throws IllegalStateException If this {@link Pixmap} is closed, i.e.
     * {@link #isClosed()} returns {@code true}.
     */
    float getMaxV() {
        return (float) (this.getYOffset() + this.getHeight()) /
                        this.getTexture().getHeight();
    }

    @Override
    void setup(final int framebufferId) {
        super.setup(framebufferId);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER,
                                    GL30.GL_COLOR_ATTACHMENT0,
                                    GL11.GL_TEXTURE_2D,
                                    this.getTexture().getId(),
                                    0);
    }

    @Override
    void copyTo(Pixmap destination, int sourceFramebufferId) {
        this.ensureOpen();
        if (this == destination) {
            return;
        }//end if

        if (!this.getSize().equals(destination.getSize())) {
            throw new IllegalArgumentException("Argument Pixmap destination " +
                    "must have the same size as this Pixmap.");
        }

        if (this.getTexture().equals(destination.getTexture())) {
            throw new IllegalArgumentException("Argument Pixmap destination " +
                    "has the same backing Texture as this Pixmap.");
        }

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, sourceFramebufferId);
        GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER,
                                    GL30.GL_COLOR_ATTACHMENT0,
                                    GL11.GL_TEXTURE_2D,
                                    this.getTexture().getId(),
                                    0);

        destination.getTexture().bind();
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0,
                destination.getXOffset(), destination.getYOffset(),
                this.getXOffset(), this.getYOffset(), this.getWidth(),
                this.getHeight());
    }

    /**
     * Indicates if the OpenGL texture of this {@link Pixmap} is deleted.
     * @return {@code true} if the OpenGL texture of this {@link Pixmap} is
     * deleted, otherwise {@code false}.
     */
    public abstract boolean isClosed();

    /**
     * Gets the underlying {@link Texture} of this {@link Pixmap}.
     * @return The underlying {@link Texture} of this {@link Pixmap}.
     * @throws IllegalStateException If this {@link Pixmap} is closed, i.e.
     * {@link #isClosed()} returns {@code true}.
     */
    abstract Texture getTexture();

    /**
     * Ensures that this {@link Pixmap} is open, i.e. {@link #isClosed()}
     * returns {@code false}, by throwing an exception if it's closed. If this
     * {@link Pixmap} is open, this method does nothing.
     * @throws IllegalStateException If {@link #isClosed()} returns {@code
     * true}.
     */
    private void ensureOpen() throws IllegalStateException {
        if (this.isClosed()) {
            throw new IllegalStateException("The underlying Texture of this " +
                    "Pixmap is closed.");
        }//end if
    }

}

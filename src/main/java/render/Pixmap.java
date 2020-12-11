package render;

import org.lwjgl.opengl.GL11;

import java.util.Objects;

/**
 * Represents a region of a 2d table of colors that resides inside the GPU
 * memory. The origin (0, 0) is at the bottom left corner, with the y axis
 * (rows) pointing upwards and the x axis (columns) pointing rightwards.
 * @apiNote Trying to use any method of a {@link Pixmap} that its {@link
 * Pixmap#isClosed()} method returns {@code true}, will result in {@link
 * IllegalStateException}.
 */
public abstract class Pixmap {

    /**
     * A {@link Pixmap} implementation that wraps a {@link Texture}.
     */
    private static class Pxm extends Pixmap {

        /**
         * The underlying {@link Texture} of this {@link Pxm}.
         */
        private final Texture TEXTURE;

        /**
         * The minimum texel column (included), in the x-axis of the {@link
         * Texture} this {@link Pxm} region starts from.
         */
        private final int X_OFFSET;

        /**
         * The minimum texel row (included), in the y-axis of the {@link
         * Texture} this {@link Pxm} region starts from.
         */
        private final int Y_OFFSET;

        /**
         * Constructs a {@link Pxm}, given its underlying {@link Texture} and
         * the region of that {@link Texture} it represents. Trying to construct
         * an empty region will result in {@link IllegalArgumentException}. Use
         * {@link Pixmap#EMPTY} instead.
         * @param texture The underlying {@link Texture} of this {@link Pxm}.
         * Can't be null.
         * @param fromX The minimum texel column (included), in the x-axis of
         * the {@link Texture} this {@link Pxm} region will start from.
         * @param toX The maximum texel column (excluded), in the x-axis of the
         * {@link Texture} this {@link Pxm} region will end with.
         * @param fromY The minimum texel row (included), in the y-axis of the
         * {@link Texture} this {@link Pxm} region will start from.
         * @param toY The maximum texel row (excluded), in the y-axis of the
         * {@link Texture} this {@link Pxm} region will end with.
         * @throws IllegalArgumentException If {@code fromX < 0}.
         * @throws IllegalArgumentException If {@code toX > texture.getWidth()}.
         * @throws IllegalArgumentException If {@code fromX == toX}.
         * @throws IllegalArgumentException If {@code fromY < 0}.
         * @throws IllegalArgumentException If {@code toY >
         * texture.getHeight()}.
         * @throws IllegalArgumentException If {@code fromY == toY}.
         */
        public Pxm(Texture texture, int fromX, int toX, int fromY, int toY) {
            super(toX - fromX, toY - fromY);

            if (fromX < 0) {
                throw new IllegalArgumentException("Argument fromX can't be " +
                        "negative.");
            }//end if

            if (toX > texture.getWidth()) {
                throw new IllegalArgumentException("Argument toX can't be " +
                        "greater than the width of argument texture.");
            }//end if

            if (fromX == toX) {
                throw new IllegalArgumentException("Arguments fromX and toX " +
                        "can't be equal.");
            }//end for

            if (fromY < 0) {
                throw new IllegalArgumentException("Argument fromY can't be " +
                        "negative.");
            }//end if

            if (toY > texture.getHeight()) {
                throw new IllegalArgumentException("Argument toY can't be " +
                        "greater than the height of argument texture.");
            }//end if

            if (fromY == toY) {
                throw new IllegalArgumentException("Arguments fromY and toY " +
                        "can't be equal.");
            }//end for

            this.TEXTURE = texture;
            this.X_OFFSET = fromX;
            this.Y_OFFSET = fromY;
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
         * Ensures that this {@link Pxm} is open, i.e. {@link #isClosed()}
         * returns {@code false}, by throwing an exception if its closed. If
         * this {@link Pxm} is open, this method does nothing.
         * @throws IllegalStateException If {@link #isClosed()} returns {@code
         * true}.
         */
        private void ensureOpen() throws IllegalStateException {
            if (this.isClosed()) {
                throw new IllegalStateException("The underlying Texture of " +
                        "this Pixmap is closed.");
            }//end if
        }

    }//end inner class Pxm

    /**
     * Represents a {@link Pixmap}, that has a region area of 0 texels^2, i.e.
     * contains no colors.
     */
    public static final Pixmap EMPTY = new Pixmap(0, 0) {
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
            return "Pixmap.EMPTY";
        }

        @Override
        Texture getTexture() {
            throw new UnsupportedOperationException();
        }

        @Override
        int getId() {
            throw new UnsupportedOperationException();
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
    };

    /**
     * The width of this {@link Pixmap} in texels.
     */
    private final int WIDTH;

    /**
     * The height of this {@link Pixmap} in texels.
     */
    private final int HEIGHT;

    /**
     * Constructs a {@link Pixmap}, given its width and height.
     * @param width The width of this {@link Pixmap} in texels.
     * @param height The height of this {@link Pixmap} in texels.
     * @throws IllegalArgumentException If {@code width < 0}.
     * @throws IllegalArgumentException If {@code height < 0}.
     */
    Pixmap(int width, int height) {
        if (width < 0) {
            throw new IllegalArgumentException("Argument width can't be " +
                    "negative.");
        }//end if

        if (height < 0) {
            throw new IllegalArgumentException("Argument height can't be " +
                    "negative.");
        }//end if

        this.WIDTH = width;
        this.HEIGHT = height;
    }

    /**
     * Gets the width of this {@link Pixmap} in texels.
     * @return The width of this {@link Pixmap} in texels.
     * @throws IllegalStateException If this {@link Pixmap} is closed, i.e.
     * {@link #isClosed()} returns {@code true}.
     */
    public int getWidth() {
        this.ensureOpen();
        return this.WIDTH;
    }

    /**
     * Gets the height of this {@link Pixmap} in texels.
     * @return The height of this {@link Pixmap} in texels.
     * @throws IllegalStateException If this {@link Pixmap} is closed, i.e.
     * {@link #isClosed()} returns {@code true}.
     */
    public int getHeight() {
        this.ensureOpen();
        return this.HEIGHT;
    }

    /**
     * Creates a {@link Pixmap} that represents a subregion of this {@link
     * Pixmap}. If {@code formX == toX || fromY == toY} then {@link
     * Pixmap#EMPTY} will be returned.
     * @param fromX The minimum texel column (included), in the x-axis of this
     * {@link Pixmap}, that the new {@link Pixmap} region will start form.
     * @param toX The maximum texel column (excluded), in the x-axis of this
     * {@link Pixmap}, that the new {@link Pixmap} region will end with.
     * @param fromY The minimum texel row (included), in the y-axis of this
     * {@link Pixmap}, that the new {@link Pixmap} region will start form.
     * @param toY The maximum texel row (excluded), in the y-axis of this
     * {@link Pixmap}, that the new {@link Pixmap} region will end with.
     * @return A {@link Pixmap} that represents a subregion of this {@link
     * Pixmap}.
     * @apiNote There is no guarantee that the returned {@link Pixmap} will be a
     * newly created one.
     * @throws IllegalArgumentException If {@code fromX < 0}.
     * @throws IllegalArgumentException If {@code toX > this.getWidth()}.
     * @throws IllegalArgumentException If {@code fromX > toX}.
     * @throws IllegalArgumentException If {@code fromY < 0}.
     * @throws IllegalArgumentException If {@code toY > this.getHeight()}.
     * @throws IllegalArgumentException If {@code fromY > toY}.
     * @throws IllegalStateException If this {@link Pixmap} is closed, i.e.
     * {@link #isClosed()} returns {@code true}.
     */
    public Pixmap subRegion(int fromX, int toX, int fromY, int toY) {
        this.ensureOpen();

        if (0 == fromX && this.getWidth() == toX && 0 == fromY &&
                this.getHeight() == toY) {
            return this;
        }//end if

        return (toX - fromX != 0 && toY - fromY != 0) ? new Pxm(
                this.getTexture(),
                this.getXOffset() + fromX,
                this.getXOffset() + toX,
                this.getYOffset() + fromY,
                this.getYOffset() + toY) : Pixmap.EMPTY;
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
        this.ensureOpen();

        if (this == obj) {
            return true;
        }//end if

        if (null == obj) {
            return false;
        }//end if

        return (obj instanceof Pixmap p) &&
                this.getId() == p.getId() &&
                this.getWidth() == p.getWidth() &&
                this.getHeight() == p.getHeight() &&
                this.getXOffset() == p.getXOffset() &&
                this.getYOffset() == p.getYOffset();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getId(), this.getXOffset(), this.getWidth(),
                this.getYOffset(), this.getHeight());
    }

    @Override
    public String toString() {
        return String.format("Pixmap %d, x: [%d, %d), y: [%d, %d)",
                this.getId(), this.getXOffset(), this.getXOffset() +
                this.getWidth(), this.getYOffset(), this.getYOffset() +
                this.getHeight());
    }

    /**
     * Binds the OpenGL texture of this {@link Pixmap} to the current OpenGL
     * context.
     * @throws IllegalStateException If this {@link Pixmap} is closed, i.e.
     * {@link #isClosed()} returns {@code true}.
     */
    void bind() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.getId());
    }

    /**
     * Gets the id of the OpenGL texture of this {@link Pixmap}.
     * @return The id of the OpenGL texture of this {@link Pixmap}.
     * @throws IllegalStateException If this {@link Pixmap} is closed, i.e.
     * {@link #isClosed()} returns {@code true}.
     */
    int getId() {
        return this.getTexture().getId();
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

    /**
     * Indicates if the area of region of this {@link Pixmap} is 0 texels^2,
     * i.e. contains no colors.
     * @return {@code true} if the area of the region of this {@link Pixmap} is
     * 0 texels^2, otherwise {@code false}.
     * @throws IllegalStateException If this {@link Pixmap} is closed, i.e.
     * {@link #isClosed()} returns {@code true}.
     */
    boolean isEmpty() {
        this.ensureOpen();
        return this == Pixmap.EMPTY;
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
     * Gets the minimum texel column (included), in the x-axis of the {@link
     * Texture} this {@link Pixmap} region starts from.
     * @return The minimum texel column (included), in the x-axis of the {@link
     * Texture} this {@link Pixmap} region starts from.
     */
    abstract int getXOffset();

    /**
     * Gets the minimum texel row (included), in the y-axis of the {@link
     * Texture} this {@link Pixmap} region starts from.
     * @return The minimum texel row (included), in the y-axis of the {@link
     * Texture} this {@link Pixmap} region starts from.
     */
    abstract int getYOffset();

    /**
     * Ensures that this {@link Pixmap} is open, i.e. {@link #isClosed()}
     * returns {@code false}, by throwing an exception if its closed. If this
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

}//end class Pixmap
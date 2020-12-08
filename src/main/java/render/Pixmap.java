package render;

import org.lwjgl.opengl.GL11;

import java.util.Objects;

/**
 * Represents a region of a 2d table of colors that resides inside the GPU
 * memory. The origin (0, 0) is at the bottom left corner, with the y axis
 * (rows) pointing upwards and the x axis (columns) pointing rightwards.
 */
public abstract class Pixmap {

    private static class Pxm extends Pixmap {
        private final Texture TEXTURE;
        private final int X_OFFSET;
        private final int Y_OFFSET;

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

            if (fromY < 0) {
                throw new IllegalArgumentException("Argument fromY can't be " +
                        "negative.");
            }//end if

            if (toY > texture.getHeight()) {
                throw new IllegalArgumentException("Argument toY can't be " +
                        "greater than the height of argument texture.");
            }//end if

            this.TEXTURE = texture;
            this.X_OFFSET = fromX;
            this.Y_OFFSET = fromY;
        }

        @Override
        public boolean isClosed() {
            return this.getTexture().isClosed();
        }

        @Override
        Texture getTexture() {
            return this.TEXTURE;
        }

        @Override
        int getId() {
            return this.getTexture().getId();
        }

        @Override
        int getXOffset() {
            return this.X_OFFSET;
        }

        @Override
        int getYOffset() {
            return this.Y_OFFSET;
        }
    }//end inner class Pxm

    private static final Pixmap EMPTY = new Pixmap(0, 0) {
        @Override public boolean isClosed() {return false;}
        @Override Texture getTexture() {return null;}
        @Override int getId() {return 0;}
        @Override int getXOffset() {return 0;}
        @Override int getYOffset() {return 0;}
        @Override float getMinU() {return 0.0f;}
        @Override float getMaxU() {return 0.0f;}
        @Override float getMinV() {return 0.0f;}
        @Override float getMaxV() {return 0.0f;}
    };

    /**
     * The width of this {@link Pixmap} in texels.
     */
    private final int WIDTH;

    /**
     * The height of this {@link Pixmap} in texels.
     */
    private final int HEIGHT;

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
        if (this.isClosed()) {
            throw new IllegalStateException("The underlying Texture of this " +
                    "Pixmap is closed.");
        }//end if

        return this.WIDTH;
    }

    /**
     * Gets the height of this {@link Pixmap} in texels.
     * @return The height of this {@link Pixmap} in texels.
     * @throws IllegalStateException If this {@link Pixmap} is closed, i.e.
     * {@link #isClosed()} returns {@code true}.
     */
    public int getHeight() {
        if (this.isClosed()) {
            throw new IllegalStateException("The underlying Texture of this " +
                    "Pixmap is closed.");
        }//end if

        return this.HEIGHT;
    }

    public Pixmap subRegion(int fromX, int toX, int fromY, int toY) {
        return (toX - fromX != 0 && toY - fromY != 0) ? new Pxm(
                this.getTexture(),
                this.getXOffset() + fromX,
                this.getXOffset() + toX,
                this.getYOffset() + fromY,
                this.getYOffset() + toY) : Pixmap.EMPTY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }//end if

        if (null == o) {
            return false;
        }//end if

        if (o instanceof Pixmap p) {
            if (this.getWidth() != p.getWidth() ||
                    this.getHeight() != p.getHeight()) {
                return false;
            }//end if

            return this.getId() == p.getId() &&
                    this.getXOffset() == p.getXOffset() &&
                    this.getYOffset() == p.getYOffset();
        } else {
            return false;
        }//end if
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

    float getMinU() {
        return (float) this.getXOffset() / this.getTexture().getWidth();
    }

    float getMaxU() {
        return (float) (this.getXOffset() + this.getWidth()) /
                this.getTexture().getWidth();
    }

    float getMinV() {
        return (float) this.getYOffset() / this.getTexture().getHeight();
    }

    float getMaxV() {
        return (float) (this.getYOffset() + this.getHeight()) /
                this.getTexture().getHeight();
    }

    /**
     * Indicates if the OpenGL texture of this {@link Pixmap} is deleted.
     * @return {@code true} if the OpenGL texture of this {@link Pixmap} is
     * deleted, otherwise {@code false}.
     */
    public abstract boolean isClosed();

    /**
     * Gets the underlying {@link Texture} of this {@link Pixmap}.
     * @return The underlying {@link Texture} of this {@link Pixmap} or {@code
     * null} if this {@link Pixmap} is empty, i.e. it's total area is 0
     * texels^2.
     */
    abstract Texture getTexture();

    /**
     * Gets the id of the OpenGL texture of this {@link Pixmap}.
     * @return The id of the OpenGL texture of this {@link Pixmap}.
     * @throws IllegalStateException If this {@link Pixmap} is closed, i.e.
     * {@link #isClosed()} returns {@code true}.
     */
    abstract int getId();
    abstract int getXOffset();
    abstract int getYOffset();

}//end class Pixmap
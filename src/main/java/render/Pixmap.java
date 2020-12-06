package render;

import org.lwjgl.opengl.GL11;

/**
 * Represents a 2d table of colors that reside inside the GPU memory. Each cell,
 * also called texel is defined by an (i, j) tuple, where i is the row and j the
 * column. The origin (0, 0) is at the bottom left corner of the table, with the
 * y axis (rows) pointing upwards and the x axis (columns) pointing rightwards.
 */
public abstract sealed class Pixmap permits Texture {

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
     * Gets the width of this {@link Pixmap} in texels.
     * @return The width of this {@link Pixmap} in texels.
     * @throws IllegalStateException If this {@link Pixmap} is closed, i.e.
     * {@link #isClosed()} returns {@code true}.
     */
    public abstract int getWidth();

    /**
     * Gets the height of this {@link Pixmap} in texels.
     * @return The height of this {@link Pixmap} in texels.
     * @throws IllegalStateException If this {@link Pixmap} is closed, i.e.
     * {@link #isClosed()} returns {@code true}.
     */
    public abstract int getHeight();

    /**
     * Gets the id of the OpenGL texture of this {@link Pixmap}.
     * @return The id of the OpenGL texture of this {@link Pixmap}.
     * @throws IllegalStateException If this {@link Pixmap} is closed, i.e.
     * {@link #isClosed()} returns {@code true}.
     */
    abstract int getId();

    /**
     * Indicates if the OpenGL texture of this {@link Pixmap} is deleted.
     * @return {@code true} if the OpenGL texture of this {@link Pixmap} is
     * deleted, otherwise {@code false}.
     */
    public abstract boolean isClosed();

}//end class Pixmap
package moonkeki.render;

import org.lwjgl.opengl.GL11;

import java.awt.Color;

/**
 * A hardware-accelerated (typically via GPUs) mechanism to draw onto a {@link
 * Canvas}. Here draw, translates to altering the state (texels) of a {@link
 * Canvas}.
 *
 * <p>Even though not a general contract, most {@link Renderer}s acquire native
 * resources, and therefore implement {@link AutoCloseable}.
 *
 * <p>{@snippet :
 * Canvas c = ..
 * Pixmap p = ..
 * try (InstantRenderer r = InstantRenderer.builder()
 *                                         .ofCanvas(c)
 *                                         .shortLived()) {
 *     r.drawCommand()
 *      .ofPixmap(p)
 *      .atPosition(100, 100)
 *      .draw();
 *
 * }
 * }
 *
 * The above snippet makes use of an {@link InstantRenderer}, that specializes
 * in drawing {@link Pixmap}s. Other {@link Renderer}s may use {@link
 * ShaderProgram}s to perform the drawing, or other mechanisms that eventually
 * mutate the texels of the {@link Canvas}.
 *
 * <p>Even though we single-used the above {@link Renderer}, that's not a
 * contract, and it's up to the user to decide if caching or not. {@link
 * AutoCloseable} {@link Renderer}s may distinguish between short-lived vs
 * long-lived. Short-lived {@link Renderer}s are suited for ad-hoc single-use
 * drawing, while long-lived ones, allow persistence, and can remain open
 * throughout most of the application lifecycle, with closing at the end.
 */
public abstract class Renderer {

    /**
     * Replaces the color of every texel from the {@link Canvas} of this {@link
     * Renderer}, with a given {@link Color}.
     * @param color The new texel color for the underlying {@link Canvas} of
     * this {@link Renderer}.
     */
    public void clearCanvas(Color color) {
        this.setupCanvas();
        GL11.glClearColor(color.getRed() / 255.0f,
                          color.getGreen() / 255.0f,
                          color.getBlue() / 255.0f,
                          color.getAlpha() / 255.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }

    /**
     * Gets the {@link Canvas} that this {@link Renderer} performs the drawing
     * to.
     * @return The underlying {@link Canvas} of this {@link Renderer}.
     */
    public abstract Canvas getCanvas();
    abstract void setupCanvas();
    abstract void copyCanvasTo(Pixmap destination);

}

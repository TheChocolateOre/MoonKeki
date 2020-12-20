package render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.BiFunction;
import java.util.function.IntBinaryOperator;

/**
 * A {@link Pixmap} implementation that wraps an OpenGL texture.
 */
public final class Texture extends Pixmap implements AutoCloseable {

    /**
     * The id of this {@link Texture}, assigned by OpenGL.
     */
    private final int ID;

    /**
     * Indicates if the OpenGL texture of this {@link Texture} is deleted.
     * {@code true} if the OpenGL texture of this {@link Texture} is deleted,
     * otherwise {@code false}.
     */
    private boolean closed;

    /**
     * Converts a {@link BufferedImage} to an {@link IntBinaryOperator}, that
     * returns its texel colors as {@code int}s, as per {@link Color#getRGB()}.
     * The origin (0, 0) of the returned {@link IntBinaryOperator} will be at
     * the bottom left corner with the y axis pointing upwards. The color of a
     * texel at (i, j) in the {@link BufferedImage}, will be the same as the
     * color of the texel at (height - i - 1, j) of the returned {@link
     * IntBinaryOperator}, where height is that of the {@link BufferedImage}.
     * @param bufferedImage A {@link BufferedImage} that contains the data. The
     * origin must be at the top left texel with the y axis pointing downwards,
     * which is the default configuration of a {@link BufferedImage}.
     * @return An {@link IntBinaryOperator} that returns the texel colors of the
     * given {@link BufferedImage} as {@code int}, per {@link Color#getRGB()}.
     * Its origin (0, 0) will be at the bottom left corner with the y axis
     * pointing upwards. The color of a texel at (i, j) in the {@link
     * BufferedImage}, will be the same as the color of the texel at (height - i
     * - 1, j) of the returned {@link IntBinaryOperator}, where height is that
     * of the {@link BufferedImage}.
     */
    private static IntBinaryOperator asIntBinaryOperator(
            BufferedImage bufferedImage) {
        AffineTransform transform = new AffineTransform();
        transform.translate(0, bufferedImage.getHeight());
        transform.scale(1, -1);
        AffineTransformOp operation = new AffineTransformOp(transform,
                AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        final BufferedImage IMAGE = operation.filter(bufferedImage, null);
        final int[] TEXELS = new int[IMAGE.getWidth() * IMAGE.getHeight()];
        IMAGE.getRGB(0, 0, IMAGE.getWidth(), IMAGE.getHeight(),
                TEXELS, 0, IMAGE.getWidth());

        return (i, j) -> TEXELS[i * IMAGE.getWidth() + j];
    }

    /**
     * Creates a {@link Texture} from an image file and stores it in the GPU
     * memory.
     * @param path The path of the image to be loaded into this {@link Texture}.
     * @throws IOException If there is problem with reading from the file of the
     * given path.
     */
    public Texture(String path) throws IOException {
        this(ImageIO.read(new FileInputStream(path)));
    }

    /**
     * Creates a {@link Texture} form a {@link BufferedImage} and stores it in
     * the GPU memory.
     * @param image A {@link BufferedImage} that contains the data. The origin
     * must be at the top left texel with the y axis pointing downwards, which
     * is the default configuration of a {@link BufferedImage}.
     */
    public Texture(BufferedImage image) {
        this(image.getWidth(), image.getHeight(), Texture.asIntBinaryOperator(
                image));
    }

    /**
     * Creates a {@link Texture} from a {@link BiFunction} that contains the
     * data, and stores it in the GPU memory.
     * @param width The width of this {@link Texture} in texels.
     * @param height The height of this {@link Texture} in texels.
     * @param texels An {@link IntBinaryOperator} to retrieve the data of this
     * {@link Texture} as returned by {@link Color#getRGB()}. Its first argument
     * is the row, while the second being its column number. Its origin (0, 0)
     * must be at the bottom left corner with the y axis pointing upwards.
     * @throws IllegalArgumentException If {@code width <= 0}.
     * @throws IllegalArgumentException If {@code height <= 0}.
     * @throws ArithmeticException If {@code width * height * Integer.BYTES >
     * Integer.MAX_VALUE}.
     */
    public Texture(final int width, final int height, IntBinaryOperator
            texels) {
        super(width, height);

        if (width == 0) {
            throw new IllegalArgumentException("Argument width must be " +
                    "positive.");
        }//end if

        if (height == 0) {
            throw new IllegalArgumentException("Argument height must be " +
                    "positive.");
        }//end if

        final int BUFFER_SIZE = Math.multiplyExact(Math.multiplyExact(width,
                height), Integer.BYTES);
        ByteBuffer buffer = MemoryUtil.memAlloc(BUFFER_SIZE);
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                final int TEXEL = texels.applyAsInt(i, j);
                buffer.put((byte) (TEXEL >> 16 & 0xFF))  //Red
                      .put((byte) (TEXEL >> 8 & 0xFF))   //Green
                      .put((byte) (TEXEL & 0xFF))        //Blue
                      .put((byte) (TEXEL >> 24 & 0xFF)); //Alpha
            }//end for
        }//end for
        buffer.flip();

        final int TEXTURE_ID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, TEXTURE_ID);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S,
                GL13C.GL_CLAMP_TO_BORDER);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T,
                GL13C.GL_CLAMP_TO_BORDER);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER,
                GL13C.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER,
                GL13C.GL_NEAREST);

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width,
                height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        MemoryUtil.memFree(buffer);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        this.ID = TEXTURE_ID;
    }

    /**
     * Deletes the OpenGL texture of this {@link Texture}. After a call to this
     * method, this {@link Texture} must not be used, except its {@link
     * #isClosed()} method.
     * @apiNote This method is idempotent, so subsequent calls won't
     * have any effect.
     */
    @Override
    public void close() {
        if (this.isClosed()) {
            return;
        }//end if

        GL11.glDeleteTextures(this.getId());
        this.closed = true;
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    Texture getTexture() {
        this.ensureOpen();
        return this;
    }

    @Override
    int getId() {
        this.ensureOpen();
        return this.ID;
    }

    @Override
    int getXOffset() {
        this.ensureOpen();
        return 0;
    }

    @Override
    int getYOffset() {
        this.ensureOpen();
        return 0;
    }

    /**
     * Ensures that this {@link Texture} is open, i.e. {@link #isClosed()}
     * returns {@code false}, by throwing an exception if its closed. If this
     * {@link Texture} is open, this method does nothing.
     * @throws IllegalStateException If {@link #isClosed()} returns {@code
     * true}.
     */
    private void ensureOpen() throws IllegalStateException {
        if (this.isClosed()) {
            throw new IllegalStateException("This Texture is closed.");
        }//end if
    }

}//end class Texture
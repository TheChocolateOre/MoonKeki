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

/**
 * A {@link Pixmap} implementation that wraps an OpenGL texture.
 */
public class Texture extends Pixmap implements AutoCloseable {

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

    public static BiFunction<Integer, Integer, Integer> convert(final
            BiFunction<Integer, Integer, Color> colorFunction) {
        return (i, j) -> colorFunction.apply(i, j).getRGB();
    }

    private static BiFunction<Integer, Integer, Integer> asBiFunction(
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
     * is the default configuration of a {@link BufferedImage}. The
     * transformations will be handled by this constructor.
     */
    public Texture(BufferedImage image) {
        this(image.getWidth(), image.getHeight(), Texture.asBiFunction(image));
    }

    /**
     * Creates a {@link Texture} from a {@link BiFunction} that contains the
     * data, and stores it in the GPU memory.
     * @param width The width of this {@link Texture} in texels.
     * @param height The height of this {@link Texture} in texels.
     * @param colorFunction A {@link BiFunction} to retrieve the data of this
     * {@link Texture} as returned by {@link Color#getRGB()}. Its first argument
     * is the row, while the second being its column number. Its origin (0, 0)
     * must be at the bottom left corner with the y axis pointing upwards.
     * @throws IllegalArgumentException If {@code width <= 0}.
     * @throws IllegalArgumentException If {@code height <= 0}.
     */
    public Texture(final int width, final int height, BiFunction<Integer,
            Integer, Integer> colorFunction) {
        super(width, height);

        if (0 == width) {
            throw new IllegalArgumentException("Argument width must be " +
                    "positive.");
        }//end if

        if (0 == height) {
            throw new IllegalArgumentException("Argument height must be " +
                    "positive.");
        }//end if

        ByteBuffer buffer = MemoryUtil.memAlloc(width * height *
                Integer.BYTES);
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                final int TEXEL = colorFunction.apply(i, j);
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
     * #isClosed()} method. This method is idempotent, so subsequent calls won't
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

    /**
     * Indicates if the OpenGL texture of this {@link Texture} is deleted.
     * @return {@code true} if the OpenGL texture of this {@link Texture} is
     * deleted, otherwise {@code false}.
     */
    @Override
    public boolean isClosed() {
        return this.closed;
    }

    Texture getTexture() {
        this.ensureOpen();
        return this;
    }

    /**
     * Gets the id of the OpenGL texture of this {@link Texture}.
     * @return The id of the OpenGL texture of this {@link Texture}.
     * @throws IllegalStateException If this {@link Texture} is closed, i.e.
     * {@link #isClosed()} returns {@code true}.
     */
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

    private void ensureOpen() throws IllegalStateException {
        if (this.isClosed()) {
            throw new IllegalStateException("This Texture is closed.");
        }//end if
    }

}//end class Texture
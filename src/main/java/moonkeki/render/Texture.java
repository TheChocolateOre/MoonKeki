package moonkeki.render;

import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Path;
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
    private final int WIDTH;
    private final int HEIGHT;

    /**
     * Indicates if the OpenGL texture of this {@link Texture} is deleted.
     * {@code true} if the OpenGL texture of this {@link Texture} is deleted,
     * otherwise {@code false}.
     */
    private boolean closed;

    public static void write(Texture texture, Path path) throws IOException {
        ImageIO.write(texture.toBufferedImage(), "PNG", path.toFile());
    }

    static void unbind() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    /**
     * Converts a {@link BufferedImage} to an {@link IntBinaryOperator}, that
     * returns its texel colors as {@code int}s, as per {@link Color#getRGB()}.
     * The origin (0, 0) of the returned {@link IntBinaryOperator} will be in
     * the bottom left corner with the y-axis pointing upwards. The color of a
     * texel at (i, j) in the {@link BufferedImage}, will be the same as the
     * color of the texel at (height - i - 1, j) of the returned {@link
     * IntBinaryOperator}, where height is that of the {@link BufferedImage}.
     * @param bufferedImage A {@link BufferedImage} that contains the data. The
     * origin must be at the top left texel with the y-axis pointing downwards,
     * which is the default configuration of a {@link BufferedImage}.
     * @return An {@link IntBinaryOperator} that returns the texel colors of the
     * given {@link BufferedImage} as {@code int}, per {@link Color#getRGB()}.
     * Its origin (0, 0) will be in the bottom left corner with the y-axis
     * pointing upwards. The color of a texel at (i, j) in the {@link
     * BufferedImage}, will be the same as the color of the texel at (height - i
     * - 1, j) of the returned {@link IntBinaryOperator}, where height is that
     * of the {@link BufferedImage}.
     */
    @Deprecated
    private static IntBinaryOperator asIntBinaryOperator(
            BufferedImage bufferedImage) {
        final int HEIGHT = bufferedImage.getHeight();
        return (i, j) -> bufferedImage.getRGB(j, HEIGHT - i - 1);
    }

    private static ByteBuffer toByteBuffer(final int width, final int height,
                                           IntBinaryOperator texels) {
        if (width <= 0) {
            throw new IllegalArgumentException("Argument width must be " +
                    "positive.");
        }//end if

        if (height <= 0) {
            throw new IllegalArgumentException("Argument height must be " +
                    "positive.");
        }//end if

        final int BUFFER_SIZE = Math.multiplyExact(
                Math.multiplyExact(width, height), Integer.BYTES);
        ByteBuffer data = MemoryUtil.memAlloc(BUFFER_SIZE);
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                final int TEXEL = texels.applyAsInt(i, j);
                data.put((byte) (TEXEL >> 16 & 0xFF))  //Red
                    .put((byte) (TEXEL >> 8 & 0xFF))   //Green
                    .put((byte) (TEXEL & 0xFF))        //Blue
                    .put((byte) (TEXEL >> 24 & 0xFF)); //Alpha
            }//end for
        }//end for
        data.flip();

        return data;
    }

    private Texture(int width, int height, ByteBuffer data) {
        if (width == 0) {
            throw new IllegalArgumentException("Argument width must be " +
                    "positive.");
        }//end if

        if (height == 0) {
            throw new IllegalArgumentException("Argument height must be " +
                    "positive.");
        }//end if

        final int SIZE = Math.multiplyExact(Math.multiplyExact(width, height),
                4);
        if (data != null && SIZE != data.limit()) {
            throw new IllegalArgumentException("The number of elements in " +
                    "data must be equal to width * height.");
        }//end if

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

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0,
                          GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, data);
        MemoryUtil.memFree(data);
        Texture.unbind();

        this.ID = TEXTURE_ID;
        this.WIDTH = width;
        this.HEIGHT = height;
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
     * must be at the top left texel with the y-axis pointing downwards, which
     * is the default configuration of a {@link BufferedImage}.
     */
    public Texture(BufferedImage image) {
        this(image.getWidth(), image.getHeight(),
             Texture.asIntBinaryOperator(image));
    }

    public Texture(final int width, final int height) {
        this(width, height, (ByteBuffer) null);
    }

    public Texture(final int width, final int height, Color color) {
        final Texture TEXTURE = new Texture(width, height);
        TEXTURE.clear(color);

        this.ID = TEXTURE.ID;
        this.WIDTH = width;
        this.HEIGHT = height;
    }

    /**
     * Creates a {@link Texture} from a {@link BiFunction} that contains the
     * data, and stores it in the GPU memory.
     * @param width The width of this {@link Texture} in texels.
     * @param height The height of this {@link Texture} in texels.
     * @param texels An {@link IntBinaryOperator} to retrieve the data of this
     * {@link Texture} as returned by {@link Color#getRGB()}. Its first argument
     * is the row, while the second being its column number. Its origin (0, 0)
     * must be in the bottom left corner with the y-axis pointing upwards.
     * @throws IllegalArgumentException If {@code width <= 0}.
     * @throws IllegalArgumentException If {@code height <= 0}.
     * @throws ArithmeticException If {@code width * height * Integer.BYTES >
     * Integer.MAX_VALUE}.
     */
    public Texture(final int width, final int height, IntBinaryOperator
                   texels) {
        this(width, height, Texture.toByteBuffer(width, height, texels));
    }

    public Texture(Texture other) {
        if (other.isClosed()) {
            throw new IllegalArgumentException("Argument Texture other can't " +
                    "be closed.");
        }//end if

        final int FRAMEBUFFER_ID = GL30.glGenFramebuffers();
        final Texture TEXTURE = new Texture(other.getWidth(),
                                            other.getHeight());

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, FRAMEBUFFER_ID);
        GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER,
                                    GL30.GL_COLOR_ATTACHMENT0,
                                    GL11.GL_TEXTURE_2D,
                                    other.getId(),
                                    0);

        TEXTURE.bind();
        GL20.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0,
                                 TEXTURE.getWidth(), TEXTURE.getHeight());
        GL30.glDeleteFramebuffers(FRAMEBUFFER_ID);

        this.ID = TEXTURE.ID;
        this.WIDTH = other.getWidth();
        this.HEIGHT = other.getHeight();
    }

    public Texture(Canvas canvas) {
        final Texture TEXTURE = new Texture(canvas.getWidth(),
                                            canvas.getHeight());
        final int CANVAS_FRAMEBUFFER_ID = GL30.glGenFramebuffers();
        this.copyTo(TEXTURE, CANVAS_FRAMEBUFFER_ID);
        GL30.glDeleteFramebuffers(CANVAS_FRAMEBUFFER_ID);

        this.ID = TEXTURE.ID;
        this.WIDTH = canvas.getWidth();
        this.HEIGHT = canvas.getHeight();
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

    public BufferedImage toBufferedImage() {
        final int WIDTH = this.getWidth();
        final int HEIGHT = this.getHeight();

        final int SIZE = Math.multiplyExact(WIDTH, HEIGHT);
        final int[] DATA = new int[SIZE];
        this.bind();
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA,
                           GL11.GL_UNSIGNED_BYTE, DATA);
        Texture.unbind();

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT / 2; y++) {
                final int i0 = y * WIDTH + x;
                final int i1 = (HEIGHT - y - 1) * WIDTH + x;
                final int TEMP = DATA[i0];
                DATA[i0] = DATA[i1];
                DATA[i1] = TEMP;
            }
        }

        BufferedImage bufferedImage = new BufferedImage(WIDTH, HEIGHT,
                BufferedImage.TYPE_INT_ARGB);
        bufferedImage.setRGB(0, 0, bufferedImage.getWidth(),
                             bufferedImage.getHeight(), DATA, 0,
                             this.getWidth());

        return bufferedImage;
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
    public boolean equals(Object obj) {
        if (this == obj) {
            this.ensureOpen();
            return true;
        }//end if

        return obj instanceof Texture t &&
               this.getId() == t.getId() &&
               this.getWidth() == t.getWidth() &&
               this.getHeight() == t.getHeight();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.ID);
    }

    @Override
    public String toString() {
        return "Texture: %d {width: %d, height: %d}".formatted(this.getId(),
                this.getWidth(), this.getHeight());
    }

    @Override
    Texture getTexture() {
        this.ensureOpen();
        return this;
    }

    int getId() {
        this.ensureOpen();
        return this.ID;
    }

    /**
     * Binds the OpenGL texture of this {@link Texture} to the current OpenGL
     * context.
     * @throws IllegalStateException If this {@link Texture} is closed, i.e.
     * {@link #isClosed()} returns {@code true}.
     */
    void bind() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.getId());
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
     * returns {@code false}, by throwing an exception if it's closed. If this
     * {@link Texture} is open, this method does nothing.
     * @throws IllegalStateException If {@link #isClosed()} returns {@code
     * true}.
     */
    private void ensureOpen() throws IllegalStateException {
        if (this.isClosed()) {
            throw new IllegalStateException("This Texture is closed.");
        }//end if
    }

}

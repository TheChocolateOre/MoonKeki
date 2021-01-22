package render;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.List;

import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;

public class Renderer implements AutoCloseable {

    private static record TextureSize(int width, int height) {}

    public static final Canvas SCREEN = new Canvas() {
        private final int[] WIDTH_BUFFER = new int[1];
        private final int[] HEIGHT_BUFFER = new int[1];

        @Override
        void setup(int framebufferId) {
            super.setup(0);
        }

        @Override
        void copyTo(Pixmap destination, int sourceFramebufferId) {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
            Canvas.Bounds bounds = this.getBounds();
            destination.bind();
            GL20.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0,
                    destination.getXOffset(), destination.getYOffset(),
                    0, 0, bounds.width(), bounds.height());
        }

        @Override
        Canvas.Bounds getBounds() {
            GLFW.glfwGetFramebufferSize(GLFW.glfwGetCurrentContext(),
                    this.WIDTH_BUFFER, this.HEIGHT_BUFFER);
            return new Canvas.Bounds(0, 0, this.WIDTH_BUFFER[0],
                    this.HEIGHT_BUFFER[0]);
        }

        @Override
        boolean isVoid() {
            return false;
        }

        @Override
        int getId() {
            return -1;
        }
    };
    private static final int VERTICES_PER_QUAD = 6;
    private static final int FLOATS_PER_QUAD = 4 * Renderer.VERTICES_PER_QUAD;
    private static final int POST_TEXTURE_CACHE_SIZE = 5;
    //contains texture coordinates too
    public FloatBuffer vertices;
    private int size;
    private ShaderProgram baseProgram = ShaderProgram.DEFAULT;
    private final List<ShaderProgram> POST_PROGRAMS = new ArrayList<>();
    private final Map<Renderer.TextureSize, List<Texture>> POST_TEXTURES =
            new LinkedHashMap<>(Renderer.POST_TEXTURE_CACHE_SIZE, 0.75f, true);
    private Canvas canvas = Renderer.SCREEN;
    private final AffineTransform TRANSFORM = new AffineTransform();
    private final int CANVAS_FRAMEBUFFER_ID = GL30.glGenFramebuffers();
    private final int BUFFER_OBJECT_ID = GL15.glGenBuffers();
    private Pixmap currentPixmap; //Can be null
    private boolean closed;

    public Renderer() {
        this(1000); //96kB
    }

    //Capacity is the maximum number of non-empty draw() calls before flush(). A
    //non-empty draw() call has 24 floats
    public Renderer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Argument capacity must be " +
                    "positive.");
        }//end if

        this.vertices = MemoryUtil.memAllocFloat(Math.multiplyExact(
                Renderer.FLOATS_PER_QUAD, capacity));
    }

    public void clearCanvas(Color color) {
        this.ensureOpen();
        this.canvas.setup(this.CANVAS_FRAMEBUFFER_ID);
        GL11.glClearColor(color.getRed(), color.getGreen(), color.getBlue(),
                color.getAlpha());
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }

    public void draw(Pixmap pixmap) {
        this.draw(pixmap, 0.0f, 0.0f);
    }

    public void draw(Pixmap pixmap, float x, float y) {
        this.draw(pixmap, x, y, pixmap.getWidth(), pixmap.getHeight());
    }

    public void draw(Pixmap pixmap, float x, float y, float width, float
            height) {
        this.ensureOpen();

        if (width < 0.0f) {
            throw new IllegalArgumentException("Argument width can't be " +
                    "negative.");
        }//end if

        if (height < 0.0f) {
            throw new IllegalArgumentException("Argument width can't be " +
                    "negative.");
        }//end if

        if (pixmap.isVoid() || 0.0f == width || 0.0 == height) {
            return;
        }//end if

        if (this.canvas.getId() == pixmap.getId()) {
            throw new IllegalArgumentException("Argument pixmap is the " +
                    "canvas of this Renderer.");
        }//end if

        if (this.isFull() || (this.currentPixmap != null &&
                this.currentPixmap.getId() != pixmap.getId())) {
            this.flush();
        }//end if

                     //First triangle
        this.vertices.put(x).put(y + height)                      //top left
                     .put(pixmap.getMinU()).put(pixmap.getMaxV()) //top left
                     .put(x + width).put(y + height)              //top right
                     .put(pixmap.getMaxU()).put(pixmap.getMaxV()) //top right
                     .put(x).put(y)                               //bottom left
                     .put(pixmap.getMinU()).put(pixmap.getMinV()) //bottom left
                     //Second triangle
                     .put(x + width).put(y + height)              //top right
                     .put(pixmap.getMaxU()).put(pixmap.getMaxV()) //top right
                     .put(x).put(y)                               //bottom left
                     .put(pixmap.getMinU()).put(pixmap.getMinV()) //bottom left
                     .put(x + width).put(y)                       //bottom right
                     .put(pixmap.getMaxU()).put(pixmap.getMinV());//bottom right

        ++this.size;
        this.currentPixmap = pixmap;
    }

    public void setCanvas(Canvas canvas) {
        this.ensureOpen();
        if (canvas.isVoid()) {
            throw new IllegalArgumentException("Argument canvas can't be " +
                    "void.");
        }//end if

        if (this.currentPixmap != null && canvas.getId() ==
                this.currentPixmap.getId()) {
            throw new IllegalArgumentException("Argument canvas is the " +
                    "currently bound draw Pixmap of this Renderer.");
        }//end if

        this.canvas = canvas;
    }

    public void setCanvas(Canvas canvas, AffineTransform transform) {
        this.setCanvas(canvas);
        this.setTransform(transform);
    }

    public ShaderProgram getBaseProgram() {
        this.ensureOpen();
        return this.baseProgram;
    }

    public void setBaseProgram(ShaderProgram program) {
        this.ensureOpen();
        this.baseProgram = program;
    }

    public List<ShaderProgram> getPostPrograms() {
        this.ensureOpen();
        return Collections.unmodifiableList(this.POST_PROGRAMS);
    }

    public void setPostPrograms(List<ShaderProgram> programs) {
        this.ensureOpen();

        if (programs.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Argument List programs can't " +
                    "contain null values.");
        }//end if

        this.POST_PROGRAMS.clear();
        this.POST_PROGRAMS.addAll(programs);
    }

    public void setTransform(AffineTransform transform) {
        this.ensureOpen();
        this.TRANSFORM.setTransform(transform);
    }

    //Will not apply the post-processing shaders
    public void flush() {
        if (this.isEmpty()) {
            return;
        }//end if

        this.canvas.setup(this.CANVAS_FRAMEBUFFER_ID);
        this.currentPixmap.bind();

        this.vertices.flip();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.BUFFER_OBJECT_ID);
        GL15.glBufferData(GL_ARRAY_BUFFER, this.vertices, GL_STATIC_DRAW);

        this.baseProgram.setUniformMatrix("transformMatrix",
                this.getCombined());
        this.baseProgram.use();

        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, this.size() *
                Renderer.VERTICES_PER_QUAD);

        this.clear();
    }

    public void applyPost() {
        if (!this.isEmpty()) {
            throw new IllegalStateException("This Renderer must be empty, " +
                    "before applying post processing effects.");
        }//end if

        if (this.POST_PROGRAMS.isEmpty()) {
            return;
        }//end if

        final Canvas PREV_CANVAS = this.canvas;
        final ShaderProgram PREV_BASE_PROGRAM = this.baseProgram;
        final AffineTransform PREV_TRANSFORM = new AffineTransform(
                this.TRANSFORM);
        this.setTransform(new AffineTransform());

        Canvas.Bounds canvasBounds = this.canvas.getBounds();
        List<Texture> postTextures = this.getPostTextures(canvasBounds.width(),
                canvasBounds.height());

        this.canvas.copyTo(postTextures.get(0), this.CANVAS_FRAMEBUFFER_ID);

        int sourceIndex = 0;
        int destIndex = 1;
        for (ShaderProgram p : this.POST_PROGRAMS.subList(0,
                this.POST_PROGRAMS.size() - 1)) {
            this.setCanvas(postTextures.get(destIndex++));
            this.setBaseProgram(p);
            this.draw(postTextures.get(sourceIndex++));
            this.flush();
            sourceIndex %= 2;
            destIndex %= 2;
        }//end for

        this.setCanvas(PREV_CANVAS);
        this.setBaseProgram(this.POST_PROGRAMS.get(this.POST_PROGRAMS.size() -
                1));
        this.draw(postTextures.get(sourceIndex), canvasBounds.x(),
                canvasBounds.y());
        this.flush();

        this.setBaseProgram(PREV_BASE_PROGRAM);
        this.setTransform(PREV_TRANSFORM);
    }

    public int size() {
        this.ensureOpen();
        return this.size;
    }

    public int capacity() {
        this.ensureOpen();
        return this.vertices.capacity() / Renderer.FLOATS_PER_QUAD;
    }

    public int space() {
        return this.capacity() - this.size();
    }

    public boolean isEmpty() {
        return this.size() == 0;
    }

    public boolean isFull() {
        return this.space() == 0;
    }

    public void clear() {
        this.ensureOpen();
        this.vertices.clear();
        this.size = 0;
        this.currentPixmap = null;
    }

    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() {
        if (this.isClosed()) {
            return;
        }//end if

        MemoryUtil.memFree(this.vertices);
        this.POST_TEXTURES.values()
                          .stream()
                          .flatMap(Collection::stream)
                          .forEach(Texture::close);
        GL30.glDeleteFramebuffers(this.CANVAS_FRAMEBUFFER_ID);
        GL20.glDeleteBuffers(this.BUFFER_OBJECT_ID);

        this.closed = true;
    }

    private List<Texture> getPostTextures(int width, int height) {
        Renderer.TextureSize key = new Renderer.TextureSize(width, height);
        List<Texture> textures = this.POST_TEXTURES.get(key);
        if (textures != null) {
            return textures;
        }//end if

        if (this.POST_TEXTURES.size() == Renderer.POST_TEXTURE_CACHE_SIZE) {
            var itr = this.POST_TEXTURES.entrySet().iterator();
            itr.next().getValue().forEach(Texture::close);
            itr.remove();
        }//end if

        textures = List.of(new Texture(width, height), new Texture(width,
                height));
        this.POST_TEXTURES.put(key, textures);

        return textures;
    }

    private AffineTransform getCombined() {
        Canvas.Bounds canvasBounds = this.canvas.getBounds();
        final double CENTER_X = canvasBounds.x() + canvasBounds.width() / 2.0;
        final double CENTER_Y = canvasBounds.y() + canvasBounds.height() / 2.0;
        AffineTransform combined = AffineTransform.getScaleInstance(2.0 /
                canvasBounds.width(), 2.0 / canvasBounds.height());
        combined.translate(-CENTER_X, -CENTER_Y);
        combined.concatenate(this.TRANSFORM);
        return combined;
    }

    private void ensureOpen() throws IllegalStateException {
        if (this.isClosed()) {
            throw new IllegalStateException("This Renderer is closed.");
        }//end if
    }

}//end class Renderer
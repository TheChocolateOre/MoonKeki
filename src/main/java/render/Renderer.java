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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;

public class Renderer implements AutoCloseable {

    public static final Canvas SCREEN = new Canvas() {
        private final int[] WIDTH_BUFFER = new int[1];
        private final int[] HEIGHT_BUFFER = new int[1];

        @Override
        void setup(int framebufferId) {
            super.setup(0);
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
    //contains texture coordinates too
    public FloatBuffer vertices;
    private int size;
    private ShaderProgram baseProgram = ShaderProgram.DEFAULT;
    private final List<ShaderProgram> POST_PROGRAMS = new ArrayList<>();
    private Canvas canvas = Renderer.SCREEN;
    private final AffineTransform TRANSFORM = new AffineTransform();
    //The value at index 0 is never null, while at 1 may or may not be
    private final Integer[] FRAMEBUFFER_IDS = {GL30.glGenFramebuffers(), null};
    public final int BUFFER_OBJECT_ID = GL15.glGenBuffers();
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
        this.canvas.setup(this.getFramebufferId(0));
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
        if (pixmap.isVoid()) {
            return;
        }//end if

        if (this.canvas.getId() == pixmap.getId()) {
            throw new IllegalArgumentException("Argument pixmap is the " +
                    "canvas of this Renderer.");
        }//end if

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

        this.canvas.setup(this.getFramebufferId(0));
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
        throw new UnsupportedOperationException();
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
        GL30.glDeleteFramebuffers(this.FRAMEBUFFER_IDS[0]);
        if (this.FRAMEBUFFER_IDS[1] != null) {
            GL30.glDeleteFramebuffers(this.FRAMEBUFFER_IDS[1]);
        }//end if
        GL20.glDeleteBuffers(this.BUFFER_OBJECT_ID);

        this.closed = true;
    }

    private int getFramebufferId(final int index) {
        this.ensureOpen();
        Objects.checkIndex(index, this.FRAMEBUFFER_IDS.length);

        if (index != 0 && null == this.FRAMEBUFFER_IDS[index]) {
            this.FRAMEBUFFER_IDS[index] = GL30.glGenFramebuffers();
        }//end if

        return this.FRAMEBUFFER_IDS[index];
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
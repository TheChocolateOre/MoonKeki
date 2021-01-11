package render;

import org.lwjgl.system.MemoryUtil;

import java.awt.geom.AffineTransform;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Renderer implements AutoCloseable {

    private static final int VERTICES_PER_QUAD = 6;
    private static final int FLOATS_PER_QUAD = 4 * VERTICES_PER_QUAD;
    //contains texture coordinates too
    private FloatBuffer vertices;
    private int size;
    private ShaderProgram baseProgram = ShaderProgram.DEFAULT;
    private final List<ShaderProgram> POST_PROGRAMS = new ArrayList<>();
    @Deprecated
    private ShaderProgram activeProgram;
    private Pixmap destination; //null to render on screen
    private final AffineTransform TRANSFORM = new AffineTransform();
    private boolean closed;

    public Renderer() {
        this(1000); //96kB
    }

    //Capacity is the maximum number of draw() calls before flush(). A draw()
    //call has 24 floats
    public Renderer(int capacity) {
        this.vertices = MemoryUtil.memAllocFloat(Math.multiplyExact(
                FLOATS_PER_QUAD, capacity));
    }

    //TODO Clear screen/Pixmap mechanism?

    public void draw(Pixmap pixmap) {
        this.draw(pixmap, 0.0f, 0.0f);
    }

    public void draw(Pixmap pixmap, float x, float y) {
        this.draw(pixmap, 0.0f, 0.0f, pixmap.getWidth(),
                pixmap.getHeight());
    }

    public void draw(Pixmap pixmap, float x, float y, float width, float
            height) {
        if (this.isFull()) {
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
    }

    //null to render on screen
    public void setDestination(Pixmap pixmap) {
        this.ensureOpen();
        throw new UnsupportedOperationException();
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
        this.ensureOpen();

        if (this.isEmpty()) {
            return;
        }//end if

        //Stuff..
        //Don't forget to .flip() the buffer

        this.clear();
        throw new UnsupportedOperationException();
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
        return this.vertices.capacity() / FLOATS_PER_QUAD;
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
        this.closed = true;
    }

    @Deprecated
    private void useProgram(ShaderProgram program) {
        program.use();
        this.activeProgram = program;
    }

    private void ensureOpen() throws IllegalStateException {
        if (this.isClosed()) {
            throw new IllegalStateException("This Renderer is closed.");
        }//end if
    }

}//end class Renderer
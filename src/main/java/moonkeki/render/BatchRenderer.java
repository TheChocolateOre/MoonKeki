package moonkeki.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.awt.geom.AffineTransform;
import java.nio.FloatBuffer;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;

public class BatchRenderer extends PixmapRenderer implements AutoCloseable {

    private static abstract class AbstractBuilder<T extends AbstractBuilder<T>>
            implements PixmapRenderer.Builder<T> {
        Canvas canvas = WindowRegion.WINDOW;
        ShaderProgram shader = ShaderProgram.DEFAULT;
        AffineTransform transform;

        public T ofCanvas(Canvas canvas) {
            this.canvas = canvas;
            return this.getThis();
        }

        public T ofShader(ShaderProgram shader) {
            this.shader = shader;
            return this.getThis();
        }

        public T ofTransform(AffineTransform transform) {
            this.transform = transform;
            return this.getThis();
        }

        public abstract BatchRenderer build();
        abstract T getThis();
    }

    public static final class Builder extends AbstractBuilder<Builder> {
        private int bufferSize = BatchRenderer.FLOATS_PER_QUAD * 1000; //96kB
        private boolean shortLived;

        private Builder() {}

        public Builder ofCapacity(int capacity) {
            if (capacity < 1) {
                throw new IllegalArgumentException("Argument capacity must " +
                        "be positive.");
            }//end if

            this.bufferSize = Math.multiplyExact(
                    BatchRenderer.FLOATS_PER_QUAD, capacity);
            return this;
        }

        public Builder shortLived() {
            this.shortLived = true;
            return this;
        }

        public Builder longLived() {
            this.shortLived = false;
            return this;
        }

        @Override
        public BatchRenderer build() {
            if (null == this.transform) {
                this.transform = new AffineTransform();
            }//end if

            return new BatchRenderer(this);
        }

        @Override
        Builder getThis() {
            return this;
        }
    }

    public final class Rebuilder extends AbstractBuilder<Rebuilder> {
        private Rebuilder() {
            this.canvas = BatchRenderer.this.CANVAS;
            this.shader = BatchRenderer.this.SHADER;
            this.transform = BatchRenderer.this.TRANSFORM;
        }

        @Override
        public BatchRenderer build() {
            BatchRenderer.this.ensureOpen();
            if (this.isClean()) {
                return BatchRenderer.this;
            }

            final BatchRenderer NEW_BR = new BatchRenderer(this);
            BatchRenderer.this.vertices = null;
            BatchRenderer.this.dispenser = null;
            BatchRenderer.this.currentTexture = null;
            BatchRenderer.this.closed = true;

            return NEW_BR;
        }

        @Override
        Rebuilder getThis() {
            return this;
        }

        boolean isClean() {
            return this.canvas.equals(BatchRenderer.this.CANVAS) &&
                   this.shader.equals(BatchRenderer.this.SHADER) &&
                   this.transform.equals(BatchRenderer.this.TRANSFORM);
        }

        private BatchRenderer getBufferedRenderer() {
            return BatchRenderer.this;
        }
    }

    public class DrawCommand extends PixmapRenderer.AbstractDrawCommand
                             implements PixmapRenderer.AttachedDrawCommand {
        private DrawCommand() {}

        @Override
        public BatchRenderer.DrawCommand ofPixmap(Pixmap pixmap) {
            return (BatchRenderer.DrawCommand) super.ofPixmap(pixmap);
        }

        @Override
        public BatchRenderer.DrawCommand atX(double x) {
            return (BatchRenderer.DrawCommand) super.atX(x);
        }

        @Override
        public BatchRenderer.DrawCommand atY(double y) {
            return (BatchRenderer.DrawCommand) super.atY(y);
        }

        @Override
        public BatchRenderer.DrawCommand atPosition(double x, double y) {
            return (BatchRenderer.DrawCommand) super.atPosition(x, y);
        }

        @Override
        public BatchRenderer.DrawCommand ofWidth(double width) {
            return (BatchRenderer.DrawCommand) super.ofWidth(width);
        }

        @Override
        public BatchRenderer.DrawCommand ofHeight(double height) {
            return (BatchRenderer.DrawCommand) super.ofHeight(height);
        }

        @Override
        public BatchRenderer.DrawCommand ofSize(double width, double height) {
            return (BatchRenderer.DrawCommand) super.ofSize(width, height);
        }

        @Override
        public BatchRenderer.DrawCommand ofMirroredX() {
            return (BatchRenderer.DrawCommand) super.ofMirroredX();
        }

        @Override
        public BatchRenderer.DrawCommand ofMirroredY() {
            return (BatchRenderer.DrawCommand) super.ofMirroredY();
        }

        @Override
        public BatchRenderer.DrawCommand unmirrorX() {
            return (BatchRenderer.DrawCommand) super.unmirrorX();
        }

        @Override
        public BatchRenderer.DrawCommand unmirrorY() {
            return (BatchRenderer.DrawCommand) super.unmirrorY();
        }

        @Override
        public BatchRenderer.DrawCommand withTransform(AffineTransform transform) {
            return (BatchRenderer.DrawCommand) super.withTransform(transform);
        }

        @Override
        public void process() {
            this.queue();
        }

        public void queue() {
            BatchRenderer.this.queue(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    //returns a new BatchRenderer
    public static BatchRenderer getDefault() {
        return BatchRenderer.builder().build();
    }

    private static final int VERTICES_PER_QUAD = 6;
    private static final int FLOATS_PER_QUAD = 4 *
            BatchRenderer.VERTICES_PER_QUAD;

    //contains texture coordinates too
    private FloatBuffer vertices;
    private Consumer<FloatBuffer> dispenser;
    private int size;
    private final Canvas CANVAS;
    private final ShaderProgram SHADER;
    private final AffineTransform TRANSFORM;
    private final int CANVAS_FRAMEBUFFER_ID;
    private final int BUFFER_OBJECT_ID;
    private Texture currentTexture; //Can be null
    private boolean closed;

    private BatchRenderer(Builder builder) {
        if (builder.shortLived) {
            this.vertices = MemoryStack.stackPush()
                                       .mallocFloat(builder.bufferSize);
            this.dispenser = b -> MemoryStack.stackPop();
        } else {
            this.vertices = MemoryUtil.memAllocFloat(builder.bufferSize);
            this.dispenser = MemoryUtil::memFree;
        }//end if

        this.CANVAS = builder.canvas;
        this.SHADER = builder.shader;
        this.TRANSFORM = builder.transform;
        this.CANVAS_FRAMEBUFFER_ID = this.CANVAS.getBackend().equals(
                WindowRegion.WINDOW.getBackend()) ? 0 :
                GL30.glGenFramebuffers();
        this.BUFFER_OBJECT_ID = GL15.glGenBuffers();
    }

    private BatchRenderer(Rebuilder rebuilder) {
        this.vertices = rebuilder.getBufferedRenderer().vertices;
        this.dispenser = rebuilder.getBufferedRenderer().dispenser;
        this.size = rebuilder.getBufferedRenderer().size;
        this.CANVAS = rebuilder.canvas;
        this.SHADER = rebuilder.shader;
        this.TRANSFORM = rebuilder.transform;

        if (!this.CANVAS.getBackend().equals(WindowRegion.WINDOW.getBackend())
            && rebuilder.getBufferedRenderer().CANVAS_FRAMEBUFFER_ID == 0) {
            this.CANVAS_FRAMEBUFFER_ID = GL30.glGenFramebuffers();
        } else {
            this.CANVAS_FRAMEBUFFER_ID = rebuilder.getBufferedRenderer()
                                                  .CANVAS_FRAMEBUFFER_ID;
        }

        this.BUFFER_OBJECT_ID = rebuilder.getBufferedRenderer()
                .BUFFER_OBJECT_ID;
        this.currentTexture = rebuilder.getBufferedRenderer().currentTexture;
    }

    public Rebuilder rebuild() {
        this.ensureOpen();
        if (!this.isEmpty()) {
            throw new IllegalStateException("Can't rebuild a non-empty " +
                    "BatchRenderer.");
        }

        return new Rebuilder();
    }

    public BatchRenderer.DrawCommand drawCommand() {
        return new DrawCommand();
    }

    @Override
    public void process(PixmapRenderer.DrawCommand drawCommand) {
        this.queue(drawCommand);
    }

    public void queue(PixmapRenderer.DrawCommand drawCommand) {
        this.ensureOpen();

        final Pixmap PIXMAP = drawCommand.getPixmap();
        if (PIXMAP.isVoid() || 0.0 == drawCommand.getWidth() ||
                               0.0 == drawCommand.getHeight()) {
            return;
        }//end if

        if (this.CANVAS.getBackend().equals(PIXMAP.getBackend())) {
            throw new IllegalArgumentException("The pixmap of the " +
                    "DrawCommand is the canvas of this BufferedRenderer.");
        }//end if

        if (this.isFull() || !PIXMAP.getTexture().equals(this.currentTexture)) {
            this.flush();
        }//end if

        final float MIN_U;
        final float MAX_U;
        final float MIN_V;
        final float MAX_V;

        if (drawCommand.isXMirrored()) {
            MIN_U = PIXMAP.getMaxU();
            MAX_U = PIXMAP.getMinU();
        } else {
            MIN_U = PIXMAP.getMinU();
            MAX_U = PIXMAP.getMaxU();
        }//end if

        if (drawCommand.isYMirrored()) {
            MIN_V = PIXMAP.getMaxV();
            MAX_V = PIXMAP.getMinV();
        } else {
            MIN_V = PIXMAP.getMinV();
            MAX_V = PIXMAP.getMaxV();
        }//end if

        final double[] SRC_V = {
                drawCommand.getX(),                           //bot-left.x
                drawCommand.getY(),                           //bot-left.y
                drawCommand.getX(),                           //top-left.x
                drawCommand.getY() + drawCommand.getHeight(), //top-left.y
                drawCommand.getX() + drawCommand.getWidth(),  //top-right.x
                drawCommand.getY() + drawCommand.getHeight(), //top-right.y
                drawCommand.getX() + drawCommand.getWidth(),  //bot-right.x
                drawCommand.getY()                            //bot-right.y
        };
        final float[] DST_V = new float[SRC_V.length];
        drawCommand.getTransform().transform(SRC_V, 0, DST_V, 0,
                DST_V.length / 2);

                     //First triangle
        this.vertices.put(DST_V[2]).put(DST_V[3]) //top-left.xy
                     .put(MIN_U).put(MAX_V)       //top-left.uv
                     .put(DST_V[4]).put(DST_V[5]) //top-right.xy
                     .put(MAX_U).put(MAX_V)       //top-right.uv
                     .put(DST_V[0]).put(DST_V[1]) //bot-left.xy
                     .put(MIN_U).put(MIN_V)       //bot-left.uv
                     //Second triangle
                     .put(DST_V[4]).put(DST_V[5]) //top-right.xy
                     .put(MAX_U).put(MAX_V)       //top-right.uv
                     .put(DST_V[0]).put(DST_V[1]) //bot-left.xy
                     .put(MIN_U).put(MIN_V)       //bot-left.uv
                     .put(DST_V[6]).put(DST_V[7]) //bot-right.xy
                     .put(MAX_U).put(MIN_V);      //bot-right.uv

        this.currentTexture = PIXMAP.getTexture();
        ++this.size;
    }

    public void flush() {
        if (this.isEmpty()) {
            return;
        }//end if

        this.CANVAS.setup(this.CANVAS_FRAMEBUFFER_ID);
        this.currentTexture.bind();

        this.vertices.flip();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.BUFFER_OBJECT_ID);
        GL15.glBufferData(GL_ARRAY_BUFFER, this.vertices, GL_STATIC_DRAW);

        this.SHADER.setUniformMatrix("transformMatrix", this.getCombined());
        this.SHADER.use();
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0,
                          this.size() * BatchRenderer.VERTICES_PER_QUAD);

        this.clear();
    }

    @Override
    public Canvas getCanvas() {
        this.ensureOpen();
        return this.CANVAS;
    }

    public int size() {
        this.ensureOpen();
        return this.size;
    }

    public int capacity() {
        this.ensureOpen();
        return this.vertices.capacity() / BatchRenderer.FLOATS_PER_QUAD;
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
        this.currentTexture = null;
    }

    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() {
        if (this.isClosed()) {
            return;
        }//end if

        this.dispenser.accept(this.vertices);
        GL30.glDeleteFramebuffers(this.CANVAS_FRAMEBUFFER_ID);
        GL20.glDeleteBuffers(this.BUFFER_OBJECT_ID);

        this.closed = true;
    }

    @Override
    void setupCanvas() {
        this.getCanvas().setup(BatchRenderer.this.CANVAS_FRAMEBUFFER_ID);
    }

    @Override
    void copyCanvasTo(Pixmap destination) {
        this.getCanvas().copyTo(destination, this.CANVAS_FRAMEBUFFER_ID);
    }

    private AffineTransform getCombined() {
        final int CANVAS_WIDTH = this.CANVAS.getWidth();
        final int CANVAS_HEIGHT = this.CANVAS.getHeight();
        //The canvas offset must not be taken into account, as it is effectively
        //calculated through glViewport
        final double CENTER_X = CANVAS_WIDTH / 2.0;
        final double CENTER_Y = CANVAS_HEIGHT / 2.0;

        AffineTransform combined = AffineTransform.getScaleInstance(2.0 /
                CANVAS_WIDTH, 2.0 / CANVAS_HEIGHT);
        combined.translate(-CENTER_X, -CENTER_Y);
        combined.concatenate(this.TRANSFORM);

        return combined;
    }

    private void ensureOpen() throws IllegalStateException {
        if (this.isClosed()) {
            throw new IllegalStateException("This BufferedRenderer is closed.");
        }//end if
    }

}

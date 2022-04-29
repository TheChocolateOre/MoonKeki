package moonkeki.render;

import java.awt.geom.AffineTransform;

public class InstantRenderer extends PixmapRenderer implements AutoCloseable {

    public static final class Builder implements
            PixmapRenderer.Builder<Builder> {
        private final BatchRenderer.Builder BATCH_RENDERER_BUILDER =
                BatchRenderer.builder();

        private Builder() {
            this.BATCH_RENDERER_BUILDER.ofCapacity(1);
        }

        @Override
        public Builder ofCanvas(Canvas canvas) {
            this.BATCH_RENDERER_BUILDER.ofCanvas(canvas);
            return this;
        }

        @Override
        public Builder ofShader(ShaderProgram shader) {
            this.BATCH_RENDERER_BUILDER.ofShader(shader);
            return this;
        }

        @Override
        public Builder ofTransform(AffineTransform transform) {
            this.BATCH_RENDERER_BUILDER.ofTransform(transform);
            return this;
        }

        public Builder shortLived() {
            this.BATCH_RENDERER_BUILDER.shortLived();
            return this;
        }

        public Builder longLived() {
            this.BATCH_RENDERER_BUILDER.longLived();
            return this;
        }

        public InstantRenderer build() {
            return new InstantRenderer(this.BATCH_RENDERER_BUILDER.build());
        }
    }

    public final class Rebuilder implements PixmapRenderer.Builder<Rebuilder> {
        private final BatchRenderer.Rebuilder BATCH_RENDERER_REBUILDER =
                InstantRenderer.this.BATCH_RENDERER.rebuild();

        private Rebuilder() {}

        @Override
        public Rebuilder ofCanvas(Canvas canvas) {
            this.BATCH_RENDERER_REBUILDER.ofCanvas(canvas);
            return this;
        }

        @Override
        public Rebuilder ofShader(ShaderProgram shader) {
            this.BATCH_RENDERER_REBUILDER.ofShader(shader);
            return this;
        }

        @Override
        public Rebuilder ofTransform(AffineTransform transform) {
            this.BATCH_RENDERER_REBUILDER.ofTransform(transform);
            return this;
        }

        public InstantRenderer build() {
            if (this.isClean()) {
                return InstantRenderer.this;
            }

            return new InstantRenderer(this.BATCH_RENDERER_REBUILDER.build());
        }

        boolean isClean() {
            return this.BATCH_RENDERER_REBUILDER.isClean();
        }
    }

    public class DrawCommand extends PixmapRenderer.DrawCommand<DrawCommand> {
        private DrawCommand() {}

        public void draw() {
            InstantRenderer.this.draw(this);
        }

        @Override
        DrawCommand getThis() {
            return this;
        }
    }

    private final BatchRenderer BATCH_RENDERER;

    public static Builder builder() {
        return new Builder();
    }

    private InstantRenderer(BatchRenderer batchRenderer) {
        this.BATCH_RENDERER = batchRenderer;
    }

    public DrawCommand drawCommand() {
        return new DrawCommand();
    }

    public void draw(PixmapRenderer.DrawCommand<?> drawCommand) {
        this.BATCH_RENDERER.queue(drawCommand);
        this.BATCH_RENDERER.flush();
    }

    @Override
    public void process(PixmapRenderer.DrawCommand<?> drawCommand) {
        this.draw(drawCommand);
    }

    @Override
    public Canvas getCanvas() {
        return this.BATCH_RENDERER.getCanvas();
    }

    @Override
    void setupCanvas() {
        this.BATCH_RENDERER.setupCanvas();
    }

    @Override
    void copyCanvasTo(Pixmap destination) {
        this.BATCH_RENDERER.copyCanvasTo(destination);
    }

    public Rebuilder rebuild() {
        return new Rebuilder();
    }

    public boolean isClosed() {
        return this.BATCH_RENDERER.isClosed();
    }

    @Override
    public void close() {
        this.BATCH_RENDERER.close();
    }

}

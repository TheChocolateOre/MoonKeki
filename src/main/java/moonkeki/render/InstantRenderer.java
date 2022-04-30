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

    public class DrawCommand implements PixmapRenderer.DrawCommand {
        private final BatchRenderer.DrawCommand DRAW_COMMAND =
                InstantRenderer.this.BATCH_RENDERER.drawCommand();

        private DrawCommand() {}

        @Override
        public InstantRenderer.DrawCommand ofPixmap(Pixmap pixmap) {
            this.DRAW_COMMAND.ofPixmap(pixmap);
            return this;
        }

        @Override
        public InstantRenderer.DrawCommand atX(double x) {
            this.DRAW_COMMAND.atX(x);
            return this;
        }

        @Override
        public InstantRenderer.DrawCommand atY(double y) {
            this.DRAW_COMMAND.atY(y);
            return this;
        }

        @Override
        public InstantRenderer.DrawCommand atPosition(double x, double y) {
            this.DRAW_COMMAND.atPosition(x, y);
            return this;
        }

        @Override
        public InstantRenderer.DrawCommand ofWidth(double width) {
            this.DRAW_COMMAND.ofWidth(width);
            return this;
        }

        @Override
        public InstantRenderer.DrawCommand ofHeight(double height) {
            this.DRAW_COMMAND.ofHeight(height);
            return this;
        }

        @Override
        public InstantRenderer.DrawCommand ofSize(double width, double height) {
            this.DRAW_COMMAND.ofSize(width, height);
            return this;
        }

        @Override
        public InstantRenderer.DrawCommand ofMirroredX() {
            this.DRAW_COMMAND.ofMirroredX();
            return this;
        }

        @Override
        public InstantRenderer.DrawCommand ofMirroredY() {
            this.DRAW_COMMAND.ofMirroredY();
            return this;
        }

        @Override
        public InstantRenderer.DrawCommand unmirrorX() {
            this.DRAW_COMMAND.unmirrorX();
            return this;
        }

        @Override
        public InstantRenderer.DrawCommand unmirrorY() {
            this.DRAW_COMMAND.unmirrorY();
            return this;
        }

        @Override
        public InstantRenderer.DrawCommand withTransform(AffineTransform 
                                                        transform) {
            this.DRAW_COMMAND.withTransform(transform);
            return this;
        }

        @Override
        public void process() {
            this.draw();
        }

        public void draw() {
            this.DRAW_COMMAND.queue();
            InstantRenderer.this.BATCH_RENDERER.flush();
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

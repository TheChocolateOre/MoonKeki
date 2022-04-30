package moonkeki.render;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class ShaderRenderer extends Renderer implements AutoCloseable {

    @Deprecated
    private static abstract class AbstractBuilder<T extends
                                                  AbstractBuilder<T>> {
        Canvas canvas = WindowRegion.WINDOW;

        public T ofCanvas(Canvas canvas) {
            this.canvas = canvas;
            return this.getThis();
        }

        public abstract ShaderRenderer build();
        abstract T getThis();
    }

    public static final class Builder {
        private final InstantRenderer.Builder INSTANT_RENDERER_BUILDER =
                InstantRenderer.builder();

        private Builder() {}

        public Builder ofCanvas(Canvas canvas) {
            this.INSTANT_RENDERER_BUILDER.ofCanvas(canvas);
            return this;
        }

        public Builder shortLived() {
            this.INSTANT_RENDERER_BUILDER.shortLived();
            return this;
        }

        public Builder longLived() {
            this.INSTANT_RENDERER_BUILDER.longLived();
            return this;
        }

        public ShaderRenderer build() {
            return new ShaderRenderer(this.INSTANT_RENDERER_BUILDER.build());
        }
    }

    public final class Rebuilder {
        private final InstantRenderer.Rebuilder INSTANT_RENDERER_REBUILDER =
                ShaderRenderer.this.instantRenderer.rebuild();

        private Rebuilder() {}

        public Rebuilder ofCanvas(Canvas canvas) {
            this.INSTANT_RENDERER_REBUILDER.ofCanvas(canvas);
            return this;
        }

        public ShaderRenderer build() {
            if (this.isClean()) {
                return ShaderRenderer.this;
            }

            return new ShaderRenderer(this.INSTANT_RENDERER_REBUILDER.build());
        }

        boolean isClean() {
            return this.INSTANT_RENDERER_REBUILDER.isClean();
        }
    }

    public final class ShaderCommand {
        private record Entry(ShaderProgram shaderProgram, int count) {
            Entry {
                if (count < 1) {
                    throw new IllegalArgumentException("Argument count must " +
                            "be positive.");
                }
            }
        }

        private final List<Entry> entries = new LinkedList<>();
        private int size;

        private ShaderCommand() {}

        public ShaderCommand addShader(ShaderProgram shaderProgram) {
            return this.addShader(shaderProgram, 1);
        }

        public ShaderCommand addShader(ShaderProgram shaderProgram, int count) {
            if (0 == count) {
                return this;
            }

            this.entries.add(new Entry(shaderProgram, count));
            this.size = Math.addExact(this.size, count);
            return this;
        }

        public void apply() {
            ShaderRenderer.this.apply(this);
        }

        private int size() {
            return this.size;
        }

        //Has all the ShaderProgram's' except last
        private Stream<ShaderProgram> limitedShaderStream() {
            final Stream<ShaderProgram> BASE_STREAM =
                    this.entries.subList(0, this.entries.size() - 1)
                                .stream()
                                .flatMap(e -> Stream.generate(() ->
                                                              e.shaderProgram)
                                                    .limit(e.count));
            final ShaderCommand.Entry LAST_ENTRY =
                    this.entries.get(this.entries.size() - 1);
            final Stream<ShaderProgram> TAIL_STREAM =
                    Stream.generate(() -> LAST_ENTRY.shaderProgram)
                          .limit(LAST_ENTRY.count - 1);
            return Stream.concat(BASE_STREAM, TAIL_STREAM);
        }
    }

    private record TextureSize(int width, int height) {
        TextureSize {
            if (width < 1) {
                throw new IllegalArgumentException("Argument width must be " +
                        "positive.");
            }

            if (height < 1) {
                throw new IllegalArgumentException("Argument width must be " +
                        "positive.");
            }
        }
    }

    private static final int POST_TEXTURE_CACHE_SIZE = 5;

    public static Builder builder() {
        return new Builder();
    }

    private final Map<TextureSize, List<Texture>> POST_TEXTURES =
            new LinkedHashMap<>(POST_TEXTURE_CACHE_SIZE, 0.75f, true);
    private InstantRenderer instantRenderer;

    private ShaderRenderer(InstantRenderer instantRenderer) {
        this.instantRenderer = instantRenderer;
    }

    public ShaderCommand shaderCommand() {
        return new ShaderCommand();
    }

    public boolean isClosed() {
        return this.instantRenderer.isClosed();
    }

    @Override
    public void close() {
        this.instantRenderer.close();
    }

    @Override
    public Canvas getCanvas() {
        return this.instantRenderer.getCanvas();
    }

    public Rebuilder rebuild() {
        return new Rebuilder();
    }

    @Override
    void setupCanvas() {
        this.instantRenderer.setupCanvas();
    }

    @Override
    void copyCanvasTo(Pixmap destination) {
        this.instantRenderer.copyCanvasTo(destination);
    }

    private void apply(ShaderCommand shaderCommand) {
        if (shaderCommand.entries.isEmpty()) {
            return;
        }//end if

        final List<Texture> POST_TEXTURES = this.getPostTextures(
                shaderCommand.size > 1 ? 2 : 1);
        final Color CLEAR_COLOR = new Color(0, 0, 0, 0);
        this.copyCanvasTo(POST_TEXTURES.get(0));
        final Canvas OLD_CANVAS = this.getCanvas();

        shaderCommand.limitedShaderStream().forEach(new Consumer<>() {
            boolean srcIndex;

            @Override
            public void accept(ShaderProgram p) {
                ShaderRenderer.this.instantRenderer =
                        ShaderRenderer.this.instantRenderer.rebuild()
                        .ofCanvas(POST_TEXTURES.get(this.srcIndex ? 0 : 1))
                        .ofShader(p)
                        .build();
                ShaderRenderer.this.instantRenderer.clearCanvas(CLEAR_COLOR);
                ShaderRenderer.this.instantRenderer.drawCommand()
                        .ofPixmap(POST_TEXTURES.get(this.srcIndex ? 1 : 0))
                        .draw();
                this.srcIndex = !this.srcIndex;
            }
        });

        this.instantRenderer = this.instantRenderer.rebuild()
                .ofCanvas(OLD_CANVAS)
                .ofShader(shaderCommand.entries.get(
                        shaderCommand.entries.size() - 1).shaderProgram)
                .build();
        this.instantRenderer.clearCanvas(CLEAR_COLOR);
        this.instantRenderer.drawCommand()
                            .ofPixmap(POST_TEXTURES.get(
                                    (shaderCommand.size() - 1) % 2))
                            .draw();
    }

    private List<Texture> getPostTextures(int count) {
        if (count != 1 && count != 2) {
            throw new IllegalArgumentException("Argument count must be 1 or " +
                    "2.");
        }

        final int CANVAS_WIDTH = this.getCanvas().getWidth();
        final int CANVAS_HEIGHT = this.getCanvas().getHeight();

        TextureSize key = new TextureSize(CANVAS_WIDTH, CANVAS_HEIGHT);
        List<Texture> textures = this.POST_TEXTURES.get(key);
        if (textures != null) {
            if (textures.size() < count) {
                textures.add(new Texture(CANVAS_WIDTH, CANVAS_HEIGHT));
            }

            return Collections.unmodifiableList(textures.subList(0, count));
        }//end if

        if (this.POST_TEXTURES.size() ==
            ShaderRenderer.POST_TEXTURE_CACHE_SIZE) {
            var itr = this.POST_TEXTURES.entrySet().iterator();
            itr.next().getValue().forEach(Texture::close);
            itr.remove();
        }//end if

        textures = new ArrayList<>(2);
        for (int i = 1; i <= count; i++) {
            textures.add(new Texture(CANVAS_WIDTH, CANVAS_HEIGHT));
        }
        this.POST_TEXTURES.put(key, textures);

        return Collections.unmodifiableList(textures);
    }

}

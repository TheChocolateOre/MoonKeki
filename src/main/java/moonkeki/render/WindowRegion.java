package moonkeki.render;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.awt.*;
import java.util.Objects;

public abstract class WindowRegion extends Canvas {

    private static final class Impl extends WindowRegion {
        final int X_OFFSET;
        final int Y_OFFSET;
        final int WIDTH;
        final int HEIGHT;

        Impl(int xOffset, int yOffset, int width, int height) {
            this.X_OFFSET = xOffset;
            this.Y_OFFSET = yOffset;
            this.WIDTH = width;
            this.HEIGHT = height;
        }

        @Override
        public int getWidth() {
            return this.WIDTH;
        }

        @Override
        public int getHeight() {
            return this.HEIGHT;
        }

        @Override
        int getXOffset() {
            return this.X_OFFSET;
        }

        @Override
        int getYOffset() {
            return this.Y_OFFSET;
        }
    }

    public static final WindowRegion WINDOW = new WindowRegion() {
        private final int[] WIDTH_BUFFER = new int[1];
        private final int[] HEIGHT_BUFFER = new int[1];

        @Override
        public int getWidth() {
            GLFW.glfwGetFramebufferSize(GLFW.glfwGetCurrentContext(),
                                        this.WIDTH_BUFFER, null);
            return this.WIDTH_BUFFER[0];
        }

        @Override
        public int getHeight() {
            GLFW.glfwGetFramebufferSize(GLFW.glfwGetCurrentContext(), null,
                                        this.HEIGHT_BUFFER);
            return this.HEIGHT_BUFFER[0];
        }

        @Override
        public Size getSize() {
            GLFW.glfwGetFramebufferSize(GLFW.glfwGetCurrentContext(),
                                        this.WIDTH_BUFFER, this.HEIGHT_BUFFER);
            return new Size(this.WIDTH_BUFFER[0], this.HEIGHT_BUFFER[0]);
        }

        @Override
        public boolean isVoid() {
            final Size SIZE = this.getSize();
            return SIZE.width() == 0 || SIZE.height() == 0;
        }

        @Override
        int getXOffset() {
            return 0;
        }

        @Override
        int getYOffset() {
            return 0;
        }
    };

    public static final WindowRegion VOID = new WindowRegion() {
        @Override
        public int getWidth() {
            return 0;
        }

        @Override
        public int getHeight() {
            return 0;
        }

        @Override
        public boolean isVoid() {
            return true;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return "WindowRegion.VOID";
        }

        @Override
        void setup() {
            throw new UnsupportedOperationException();
        }

        @Override
        void setup(int framebufferId) {
            throw new UnsupportedOperationException();
        }

        @Override
        int getXOffset() {
            throw new UnsupportedOperationException();
        }

        @Override
        int getYOffset() {
            throw new UnsupportedOperationException();
        }

        @Override
        void clear(Color color, int framebufferId) {}

        @Override
        void copyTo(Pixmap destination, int sourceFramebufferId) {
            if (destination.getWidth() != 0 || destination.getHeight() != 0) {
                throw new IllegalArgumentException("Argument Pixmap " +
                        "destination must have 0 width and height.");
            }//end if
        }
    };

    private WindowRegion() {}

    @Override
    public WindowRegion subRegion(int x, int y, int width, int height) {
        final Size SIZE = this.getSize();
        if (0 == x && 0 == y && SIZE.width() == width &&
                                SIZE.height() == height) {
            return this;
        }//end if

        Canvas.validateRegion(this, x, y, width, height);
        return new Impl(this.getXOffset() + x, this.getYOffset() + y,
                        width, height);
    }

    @Override
    public void clear(Color color) {
        this.clear(color, 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (WindowRegion.VOID == obj) {
            return false;
        }

        return obj instanceof WindowRegion w &&
               this.getXOffset() == w.getXOffset() &&
               this.getYOffset() == w.getYOffset() &&
               this.getSize().equals(w.getSize());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getXOffset(), this.getYOffset(),
                            this.getWidth(), this.getHeight());
    }

    @Override
    public String toString() {
        return "WindowRegion[x=%d, y=%d, width=%d, height=%d]".formatted(
                this.getXOffset(), this.getYOffset(), this.getWidth(),
                this.getHeight());
    }

    void setup() {
        super.setup(0);
    }

    @Override
    void setup(int framebufferId) {
        super.setup(0);
    }

    @Override
    void clear(Color color, int framebufferId) {
        super.clear(color, 0);
    }

    @Override
    void copyTo(Pixmap destination, int sourceFramebufferId) {
        final Size SIZE = this.getSize();
        if (!SIZE.equals(destination.getSize())) {
            throw new IllegalArgumentException("Argument Pixmap destination " +
                    "must have the same size as this WindowRegion.");
        }

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
        destination.getTexture().bind();
        GL20.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0,
                                 destination.getXOffset(),
                                 destination.getYOffset(),
                                 this.getXOffset(), this.getYOffset(),
                                 SIZE.width(), SIZE.height());
    }

    @Override
    Object getBackend() {
        return WindowRegion.class;
    }

}

package moonkeki.app;

import moonkeki.render.WindowRegion;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import moonkeki.render.Canvas;

import java.nio.IntBuffer;
import java.util.*;
import java.util.List;
import java.util.function.BiFunction;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public final class Application {

    public interface Core extends AutoCloseable {
        void render(double dt);
        void onWindowResize(int windowWidth, int windowHeight);
        void pause();
        void resume();
        boolean isClosed();
        @Override
        void close();
    }

    //TODO Method names need renaming
    public static final class Builder {
        @FunctionalInterface
        public interface WindowPositionFunction {
            Position apply(int monitorWidth, int monitorHeight, int windowWidth,
                           int windowHeight);
        }

        private String windowTitle = "Untitled";
        private WindowPositionFunction windowPositionFunction =
                (mw, mh, ww, wh) -> new Application.Position((mw - ww) / 2,
                                                             (mh - wh) / 2);
        //(monitorWidth, monitorHeight) -> windowSize
        private BiFunction<Integer, Integer, Size> windowSizeFunction =
                Size::new;
        private BiFunction<Integer, Integer, Core> coreSupplier;
        private boolean windowDecorated = true;

        private Builder() {}

        public Builder setWindowPosition(WindowPositionFunction
                                                 windowPositionFunction) {
            this.windowPositionFunction = windowPositionFunction;
            return this;
        }

        //(monitorWidth, monitorHeight) -> Size
        public Builder setWindowSize(BiFunction<Integer, Integer, Size>
                                             windowSizeFunction) {
            this.windowSizeFunction = windowSizeFunction;
            return this;
        }

        public Builder setWindowTitle(String windowTitle) {
            this.windowTitle = windowTitle;
            return this;
        }

        public Builder setWindowDecoration(boolean decorated) {
            this.windowDecorated = decorated;
            return this;
        }

        //(windowWidth, windowHeight) -> Core
        public void build(BiFunction<Integer, Integer, Core> coreSupplier) {
            this.coreSupplier = coreSupplier;
            new Application(this).lifecycle();
        }
    }

    public record Position(int x, int y) {
        public static final Position ORIGIN = new Position(0, 0);
    }

    public record Size(int width, int height) {
        public Size {
            if (width < 1) {
                throw new IllegalArgumentException("The width must be " +
                        "positive.");
            }

            if (height < 1) {
                throw new IllegalArgumentException("The height must be " +
                        "positive.");
            }
        }
    }

    private static final List<AutoCloseable> CLOSE_LIST = new LinkedList<>();

    private long windowId;
    private Core core;

    private Application(Builder builder) {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Can't initialize GLFW.");
        }

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW_DECORATED, builder.windowDecorated ? GLFW_TRUE
                : GLFW.GLFW_FALSE);

        GLFWVidMode videoMode = GLFW.glfwGetVideoMode(
                GLFW.glfwGetPrimaryMonitor());
        Size windowSize = builder.windowSizeFunction.apply(
                videoMode.width(), videoMode.height());
        final long WINDOW_ID = GLFW.glfwCreateWindow(windowSize.width(),
                windowSize.height(), builder.windowTitle, MemoryUtil.NULL,
                MemoryUtil.NULL);
        if (WINDOW_ID == MemoryUtil.NULL) {
            throw new RuntimeException("Can't create GLFW window.");
        }

        try (MemoryStack stack = stackPush()) {
            IntBuffer windowWidthBuffer = stack.mallocInt(1);
            IntBuffer windowHeightBuffer = stack.mallocInt(1);
            GLFW.glfwGetWindowSize(WINDOW_ID, windowWidthBuffer,
                    windowHeightBuffer);

            Position windowPosition = builder.windowPositionFunction
                    .apply(videoMode.width(), videoMode.height(),
                            windowWidthBuffer.get(0), windowHeightBuffer.get(0));
            GLFW.glfwSetWindowPos(WINDOW_ID, windowPosition.x(),
                    windowPosition.y());
        }

        GLFW.glfwMakeContextCurrent(WINDOW_ID);
        GLFW.glfwSwapInterval(1); //Vsync on

        GLFW.glfwShowWindow(WINDOW_ID);
        GL.createCapabilities();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        final Canvas.Size SIZE = WindowRegion.WINDOW.getSize();
        this.core = builder.coreSupplier.apply(SIZE.width(), SIZE.height());
        this.windowId = WINDOW_ID;

        GLFW.glfwSetWindowFocusCallback(WINDOW_ID, (windowId, focused) -> {
            if (focused) {
                Application.this.core.resume();
            } else {
                Application.this.core.pause();
            }
        });
    }

    public static void closeOnExit(AutoCloseable closeable) {
        Application.CLOSE_LIST.add(closeable);
    }

    public static Application.Builder configuration() {
        return new Application.Builder();
    }

    private void lifecycle() {
        this.loop();
        this.close();
    }

    private void loop() {
        boolean loop = !GLFW.glfwWindowShouldClose(this.windowId) &&
                       !this.core.isClosed();
        if (!loop) {
            return;
        }//end if

        this.core.resume();
        int prevWindowWidth;
        int prevWindowHeight;
        {
            final Canvas.Size SIZE = WindowRegion.WINDOW.getSize();
            prevWindowWidth = SIZE.width();
            prevWindowHeight = SIZE.height();
        }

        Long prevTimeStamp = null;
        while (loop) {
            final long START_TIMESTAMP = System.nanoTime();

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            final int WINDOW_WIDTH;
            final int WINDOW_HEIGHT;
            {
                final Canvas.Size SIZE = WindowRegion.WINDOW.getSize();
                WINDOW_WIDTH = SIZE.width();
                WINDOW_HEIGHT = SIZE.height();
            }
            boolean windowSizeChanged = false;
            if (WINDOW_WIDTH != prevWindowWidth && WINDOW_WIDTH != 0) {
                prevWindowWidth = WINDOW_WIDTH;
                windowSizeChanged = true;
            }

            if (WINDOW_HEIGHT != prevWindowHeight && WINDOW_HEIGHT != 0) {
                prevWindowHeight = WINDOW_HEIGHT;
                windowSizeChanged = true;
            }

            if (windowSizeChanged) {
                this.core.onWindowResize(WINDOW_WIDTH, WINDOW_HEIGHT);
            }

            if (WINDOW_WIDTH != 0 && WINDOW_HEIGHT != 0) {
                final long CURRENT_TIMESTAMP = System.nanoTime();
                final double dt = prevTimeStamp != null ?
                        (CURRENT_TIMESTAMP - prevTimeStamp) / 1_000_000_000.0 :
                        0.0;
                prevTimeStamp = CURRENT_TIMESTAMP;
                this.core.render(dt);
            }

            GLFW.glfwSwapBuffers(this.windowId);
            GLFW.glfwPollEvents();

            loop = !GLFW.glfwWindowShouldClose(this.windowId) &&
                   !this.core.isClosed();
        }
    }

    private void close() {
        this.core.close();

        //this instance is effectively singleton, so we can "mutate" static
        //state
        Iterator<AutoCloseable> itr = Application.CLOSE_LIST.iterator();
        while (itr.hasNext()) {
            AutoCloseable c = itr.next();
            try {
                c.close();
            } catch (Exception ignored) {

            }
            itr.remove();
        }

        glfwFreeCallbacks(this.windowId);
        glfwDestroyWindow(this.windowId);

        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

}

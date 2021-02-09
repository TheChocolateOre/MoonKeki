package app;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import render.Canvas;

import java.nio.IntBuffer;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public final class Application {

    public static final class Builder {
        public static record Position(int x, int y) {}
        public static record Size(int width, int height) {
            public Size {
                if (width <= 0) {
                    throw new IllegalArgumentException("The width must be " +
                            "positive.");
                }//end if

                if (height <= 0) {
                    throw new IllegalArgumentException("The height must be " +
                            "positive.");
                }//end if
            }
        }//end static nested record Size
        @FunctionalInterface
        public interface WindowPositionFunction {
            Position apply(int monitorWidth, int monitorHeight, int windowWidth,
                    int windowHeight);
        }//end nested interface WindowPositionFunction

        private String windowTitle;
        private WindowPositionFunction windowPositionFunction;
        private BiFunction<Integer, Integer, Size> windowSizeFunction;
        private BiFunction<Integer, Integer, Screen> startScreenSupplier;

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

        public void build(BiFunction<Integer, Integer, Screen>
                startScreenSupplier) {
            this.startScreenSupplier = startScreenSupplier;
            new Application().start(this);
        }
    }//end static nested class Builder

    private static final List<AutoCloseable> CLOSE_LIST = new ArrayList<>();

    /**
     * A {@link Deque}, used as a stack, with the {@link Screen}s of this
     * application.
     */
    private final Deque<Screen> SCREENS = new ArrayDeque<>();
    private double prevFrameDurationSec;
    private long windowId;

    public static void closeOnExit(AutoCloseable closeable) {
        Application.CLOSE_LIST.add(closeable);
    }

    public static Application.Builder configuration() {
        return new Application.Builder();
    }

    private Application() {}

    /**
     * Adds a {@link Screen} on the top of the {@link Screen}s stack of this
     * {@link Application}. The newly added {@link Screen} will become the active
     * {@link Screen} of this application.
     * @param screen A {@link Screen} to be added on the top of the
     * {@link Screen}s stack of this {@link Application}. It will become the active
     * {@link Screen} of this application.
     */
    public void pushScreen(Screen screen) {
        this.pushScreen(screen, s -> {});
    }

    /**
     * Adds a {@link Screen} on the top of the {@link Screen}s stack of this
     * {@link Application}. The newly added {@link Screen} will become the active
     * {@link Screen} of this application. The given {@link Consumer} will
     * accept the given {@link Screen}, to perform an action on it.
     * @param screen A {@link Screen} to be added on the top of the
     * {@link Screen}s stack of this {@link Application}. It will become the active
     * {@link Screen} of this application.
     * @param action A {@link Consumer} that will accept the given
     * {@link Screen}, to perform an action on it.
     */
    public void pushScreen(Screen screen, Consumer<Screen> action) {
        this.SCREENS.push(screen);
        action.accept(screen);
    }

    /**
     * Removes the active {@link Screen} of this application.
     * @throws NoSuchElementException If there are no {@link Screen}s
     * in this application.
     */
    public void popScreen() {
        this.SCREENS.pop();
    }

    /**
     * Replaces the active {@link Screen} of this application, with a given one.
     * The current active {@link Screen} will be removed. If this application
     * has no {@link Screen}s, then the given {@link Screen} will simply be
     * added on the top of the {@link Screen}s stack of this Application, still
     * becoming the active {@link Screen} of this Application.
     * @param screen A {@link Screen} to replace the active {@link Screen} of
     * this application.
     */
    public void replaceScreen(Screen screen) {
        this.replaceScreen(screen, s -> {});
    }

    /**
     * Replaces the active {@link Screen} of this application, with a given one.
     * The current active {@link Screen} will be removed. If this application
     * has no {@link Screen}s, then the given {@link Screen} will simply be
     * added on the top of the {@link Screen}s stack of this Application, still
     * becoming the active {@link Screen} of this Application. The given
     * {@link Consumer} will accept the given {@link Screen}, to perform an
     * action on it.
     * @param screen A {@link Screen} to replace the active {@link Screen} of
     * this application.
     * @param action A {@link Consumer} that will accept the given
     * {@link Screen}, to perform an action on it.
     */
    public void replaceScreen(Screen screen, Consumer<Screen> action) {
        if (!this.SCREENS.isEmpty()) {
            this.popScreen();
        }//end if
        this.pushScreen(screen, action);
    }

    public double dt() {
        return this.prevFrameDurationSec;
    }

    private void start(Application.Builder builder) {
        this.init(builder);
        this.loop();
        this.close();
    }

    private void init(Application.Builder builder) {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Can't initialize GLFW.");
        }//end if

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);

        GLFWVidMode videoMode = GLFW.glfwGetVideoMode(
                GLFW.glfwGetPrimaryMonitor());
        Builder.Size windowSize = builder.windowSizeFunction.apply(
                videoMode.width(), videoMode.height());
        final long WINDOW_ID = GLFW.glfwCreateWindow(windowSize.width(),
                windowSize.height(), builder.windowTitle, MemoryUtil.NULL,
                MemoryUtil.NULL);
        if (WINDOW_ID == MemoryUtil.NULL) {
            throw new RuntimeException("Can't create GLFW window.");
        }//end if

        try (MemoryStack stack = stackPush()) {
            IntBuffer windowWidthBuffer = stack.mallocInt(1);
            IntBuffer windowHeightBuffer = stack.mallocInt(1);
            GLFW.glfwGetWindowSize(WINDOW_ID, windowWidthBuffer,
                    windowHeightBuffer);

            Builder.Position windowPosition = builder.windowPositionFunction
                    .apply(videoMode.width(), videoMode.height(),
                    windowWidthBuffer.get(0), windowHeightBuffer.get(0));
            GLFW.glfwSetWindowPos(WINDOW_ID, windowPosition.x(), windowPosition.y);
        }//end try-with-resources

        GLFW.glfwMakeContextCurrent(WINDOW_ID);
        GLFW.glfwSwapInterval(1); //Vsync on

        GLFW.glfwShowWindow(WINDOW_ID);
        GL.createCapabilities();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        this.pushScreen(builder.startScreenSupplier.apply(
                Canvas.WINDOW.getWidth(), Canvas.WINDOW.getHeight()),
                Screen::resume);

        GLFW.glfwSetWindowFocusCallback(WINDOW_ID, (windowId, focused) -> {
            if (focused) {
                Application.this.onActiveScreen(Screen::resume);
            } else {
                Application.this.onActiveScreen(Screen::pause);
            }//end if
        });

        this.windowId = WINDOW_ID;
    }

    private void loop() {
        int prevWindowWidth = Canvas.WINDOW.getWidth();
        int prevWindowHeight = Canvas.WINDOW.getHeight();
        boolean loop = !GLFW.glfwWindowShouldClose(this.windowId);
        while (loop) {
            final long START_TIMESTAMP = System.nanoTime();
            if (this.SCREENS.isEmpty()) {
                return;
            }//end if

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            final int WINDOW_WIDTH = Canvas.WINDOW.getWidth();
            final int WINDOW_HEIGHT = Canvas.WINDOW.getHeight();
            boolean windowSizeChanged = false;
            if (WINDOW_WIDTH != prevWindowWidth && WINDOW_WIDTH != 0) {
                prevWindowWidth = WINDOW_WIDTH;
                windowSizeChanged = true;
            }//end if

            if (WINDOW_HEIGHT != prevWindowHeight && WINDOW_HEIGHT != 0) {
                prevWindowHeight = WINDOW_HEIGHT;
                windowSizeChanged = true;
            }//end if

            if (windowSizeChanged) {
                this.SCREENS.forEach(s -> s.onWindowResize(WINDOW_WIDTH,
                        WINDOW_HEIGHT));
            }//end if

            if (WINDOW_WIDTH != 0 && WINDOW_HEIGHT != 0) {
                this.onActiveScreen(s -> s.render(this));
            }//end if

            GLFW.glfwSwapBuffers(this.windowId);
            GLFW.glfwPollEvents();

            loop = !GLFW.glfwWindowShouldClose(this.windowId);
            this.prevFrameDurationSec = (System.nanoTime() - START_TIMESTAMP) /
                    1_000_000_000.0;
        }//end while
    }

    private void close() {
        this.SCREENS.forEach(Screen::close);

        //this instance is effectively singleton, so we can "mutate" static
        //state
        for (AutoCloseable c : Application.CLOSE_LIST) {
            try {
                c.close();
            } catch (Exception ignored) {

            }//end try
        }//end for

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(this.windowId);
        glfwDestroyWindow(this.windowId);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    /**
     * Performs a given action, on the active {@link Screen} of this
     * {@link Application}. If there are no {@link Screen}s, it does nothing.
     * @param action A {@link Consumer} to perform an action on the active
     * {@link Screen} of this {@link Application}, if it exists.
     */
    private void onActiveScreen(Consumer<Screen> action) {
        this.onActiveScreen(action, () -> {});
    }

    /**
     * Performs a given action, on the active {@link Screen} of this
     * {@link Application}. If there are no {@link Screen}s, it performs a different
     * given action.
     * @param action A {@link Consumer} to perform an action on the active
     * {@link Screen} of this {@link Application}.
     * @param absent A {@link Runnable} to perform an action, if there are no
     * {@link Screen}s.
     */
    private void onActiveScreen(Consumer<Screen> action, Runnable absent) {
        Screen activeScreen = this.getActiveScreen();
        if (null == activeScreen) {
            absent.run();
            return;
        }//end if

        action.accept(activeScreen);
    }

    /**
     * Gets the active {@link Screen} of this {@link Application} or null if it has no
     * {@link Screen}s.
     * @return The active {@link Screen} of this {@link Application} or null if it has
     * no {@link Screen}s.
     */
    private Screen getActiveScreen() {
        return this.SCREENS.peek();
    }

}//end class Application

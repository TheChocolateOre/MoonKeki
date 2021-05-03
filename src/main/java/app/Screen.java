package app;

import geometry.Rectangle;
import org.lwjgl.glfw.GLFW;
import render.Canvas;
import render.Renderer;

import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.function.IntToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class Screen extends Canvas.ScreenRegion implements Drawable,
        Application.Core, AutoCloseable {

    public static abstract class Camera extends Rectangle {
        public static class Simple extends Screen.Camera {
            public Simple(double x, double y, double width, double height) {
                super(x, y, width, height);
            }

            @Override
            public void update() {}

            @Override
            public Rectangle compute() {
                return this;
            }
        }//end static nested class Simple
        public static class Trail extends Screen.Camera {
            private Queue<Rectangle> trail;
            private List<Double> weights;
            private IntToDoubleFunction weightFunction;
            private double weightSum;
            private int maxTrailSize;

            public Trail(double x, double y, double width, double height) {
                this(x, y, width, height, i -> 1, 1);
            }

            public Trail(double x, double y, double width, double height,
                    IntToDoubleFunction weightFunction, final int
                    maxTrailSize) {
                super(x, y, width, height);

                if (maxTrailSize < 1) {
                    throw new IllegalArgumentException("Argument " +
                            "maxTrailSize must be >= 1.");
                }//end if

                Queue<Rectangle> trail = new ArrayDeque<>(maxTrailSize);
                trail.add(new Rectangle(x, y, width, height));

                this.trail = trail;
                this.weights = IntStream.range(0, maxTrailSize)
                                        .mapToDouble(weightFunction)
                                        .boxed()
                                        .collect(Collectors.toList());
                this.weightFunction = weightFunction;
                this.weightSum = this.weights.stream()
                                             .mapToDouble(w -> w)
                                             .sum();
                this.maxTrailSize = maxTrailSize;
            }

            @Override
            public void update() {
                Rectangle next;
                if (this.trail.size() == this.maxTrailSize) {
                    next = this.trail.remove();
                    next.set(this);
                } else {
                    next = new Rectangle(this);
                }//end if

                this.trail.add(next);
            }

            @Override
            public Rectangle compute() {
                double x = 0.0;
                double y = 0.0;
                double width = 0.0;
                double height = 0.0;
                Iterator<Rectangle> trailItr = this.trail.iterator();
                ListIterator<Double> weightItr = this.weights.listIterator(
                        this.trail.size());
                while (trailItr.hasNext()) {
                    final Rectangle RECTANGLE = trailItr.next();
                    final double WEIGHT = weightItr.previous();
                    x += WEIGHT * RECTANGLE.getX();
                    y += WEIGHT * RECTANGLE.getY();
                    width += WEIGHT * RECTANGLE.getWidth();
                    height += WEIGHT * RECTANGLE.getHeight();
                }//end while

                return Rectangle.of(x / this.weightSum, y / this.weightSum,
                        width / this.weightSum, height / this.weightSum);
            }
        }//end static nested class Trail

        public Camera(double x, double y, double width, double height) {
            super(x, y, width, height);
        }

        public abstract void update();
        public abstract Rectangle compute();
    }//end static nested class Camera

    private final Renderer RENDERER;
    private int xOffset;
    private int yOffset;
    private int width;
    private int height;
    private Screen.Camera camera;

    /**
     * Indicates if this {@link Screen} is paused. True if this {@link Screen}
     * is paused, otherwise false. A paused {@link Screen} does not update its
     * state, but can be drawn. Therefore calling {@link #update(double)}
     * on a paused {@link Screen}, will have no effect.
     */
    private boolean paused = true;
    private boolean closed;

    public Screen() {
        final int[] WIDTH_BUFFER = new int[1];
        final int[] HEIGHT_BUFFER = new int[1];
        GLFW.glfwGetFramebufferSize(GLFW.glfwGetCurrentContext(), WIDTH_BUFFER,
                HEIGHT_BUFFER);

        this.RENDERER = new Renderer();
        this.width = WIDTH_BUFFER[0];
        this.height = HEIGHT_BUFFER[0];
        this.camera = new Camera.Simple(0.0, 0.0, this.width, this.height);
    }

    public Screen(int xOffset, int yOffset, int width, int height, Camera
            camera, int rendererCapacity) {
        if (width < 0) {
            throw new IllegalArgumentException("Argument width can't be " +
                    "negative.");
        }//end if

        if (height < 0) {
            throw new IllegalArgumentException("Argument height can't be " +
                    "negative.");
        }//end if

        this.RENDERER = new Renderer(rendererCapacity);
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.width = width;
        this.height = height;
        this.camera = camera;
    }

    /**
     * Updates the state of this {@link Screen}, if it is not paused and after
     * that draws it.
     */
    public final void render(double dt) {
        this.update(dt);
        this.RENDERER.setCanvas(this);
        this.RENDERER.setTransform(this.getTransform());
        this.draw(this.RENDERER);
    }

    /**
     * Pauses this {@link Screen}. Calling {@link #update(double)} on a
     * paused {@link Screen} will have no effect.
     */
    public final void pause() {
        this.ensureOpen();
        this.paused = true;
        this.onPause();
    }

    /**
     * Resumes this {@link Screen}, from the point it was paused.
     */
    public final void resume() {
        this.ensureOpen();
        this.paused = false;
        this.onResume();
    }

    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() {
        if (this.isClosed()) {
            return;
        }//end if

        this.RENDERER.close();
        this.closed = true;
    }

    protected Camera getCamera() {
        this.ensureOpen();
        return this.camera;
    }

    protected void setCamera(Camera camera) {
        this.ensureOpen();
        this.camera = Objects.requireNonNull(camera);
    }

    @Override
    protected final int getXOffset() {//TODO Maybe needs to be public?
        this.ensureOpen();
        return this.xOffset;
    }

    @Override
    protected final int getYOffset() {//TODO Maybe needs to be public?
        this.ensureOpen();
        return this.yOffset;
    }

    @Override
    public final int getWidth() {
        this.ensureOpen();
        return this.width;
    }

    @Override
    public final int getHeight() {
        this.ensureOpen();
        return this.height;
    }

    protected void setXOffset(int xOffset) {
        this.xOffset = xOffset;
    }

    protected void setYOffset(int yOffset) {
        this.yOffset = yOffset;
    }

    protected void setWidth(int width) {
        if (width < 0) {
            throw new IllegalArgumentException("Argument width can't be " +
                    "negative.");
        }//end if

        this.width = width;
    }

    protected void setHeight(int height) {
        if (height < 0) {
            throw new IllegalArgumentException("Argument height can't be " +
                    "negative.");
        }//end if

        this.height = height;
    }

    //TODO Maybe should be private?
    protected AffineTransform getTransform() {
        Camera camera = this.getCamera();
        AffineTransform transform = AffineTransform.getScaleInstance(
                this.getWidth() / camera.getWidth(), this.getHeight() /
                camera.getHeight());
        transform.translate(-camera.getX(), -camera.getY());
        return transform;
    }

    /**
     * Updates the state of this {@link Screen}, if it is not paused.
     */
    private void update(double dt) {
        this.ensureOpen();
        if (!this.paused) {
            this.onUpdate(dt);
        }//end if
    }

    private void ensureOpen() throws IllegalStateException {
        if (this.isClosed()) {
            throw new IllegalStateException("This Screen is closed.");
        }//end if
    }

    @Deprecated
    public abstract void onWindowResize(int windowWidth, int windowHeight);

    /**
     * Actions to be performed, when this {@link Screen} is updated.
     */
    protected abstract void onUpdate(double dt);

    /**
     * Actions to be performed, when this {@link Screen} is paused.
     */
    protected abstract void onPause();

    /**
     * Actions to be performed, when this {@link Screen} is resumed.
     */
    protected abstract void onResume();

}//end class Screen

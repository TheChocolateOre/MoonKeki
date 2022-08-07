package moonkeki.app.input;

import moonkeki.app.events.ClosureState;
import moonkeki.app.events.Event;
import moonkeki.app.events.InstantEventQueue;
import org.lwjgl.glfw.GLFW;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.time.Instant;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public final class Mouse {

    public record Position(double x, double y) {}

    public enum Button implements moonkeki.app.input.Button {
        LEFT(GLFW.GLFW_MOUSE_BUTTON_LEFT),
        MIDDLE(GLFW.GLFW_MOUSE_BUTTON_MIDDLE),
        RIGHT(GLFW.GLFW_MOUSE_BUTTON_RIGHT),

        B4(GLFW.GLFW_MOUSE_BUTTON_4), B5(GLFW.GLFW_MOUSE_BUTTON_5),
        B6(GLFW.GLFW_MOUSE_BUTTON_6), B7(GLFW.GLFW_MOUSE_BUTTON_7),
        B8(GLFW.GLFW_MOUSE_BUTTON_8);

        private static final Mouse.Button[] ID_TO_KEY;
        private static final int OFFSET;

        static {
            try {
                Class.forName("moonkeki.app.input.Mouse");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            final Mouse.Button[] VALUES = Mouse.Button.values();
            final IntSummaryStatistics STATS =
                    Arrays.stream(VALUES)
                          .collect(Collectors.summarizingInt(b -> b.ID));
            OFFSET = -STATS.getMin();
            ID_TO_KEY = new Mouse.Button[STATS.getMax() +
                                         Mouse.Button.OFFSET + 1];
            for (Mouse.Button b : VALUES) {
                Mouse.Button.ID_TO_KEY[b.ID + Mouse.Button.OFFSET] = b;
            }
        }

        private static Mouse.Button fromId(final int id) {
            final int INDEX = Objects.checkIndex(id + Mouse.Button.OFFSET,
                                                 Mouse.Button.ID_TO_KEY.length);
            return Mouse.Button.ID_TO_KEY[INDEX];
        }

        private final int ID;
        private final AbstractButton ABSTRACT_BUTTON = new AbstractButton() {
            @Override
            public Keyboard.Button.State getState() {
                return Mouse.Button.this.getState();
            }
        };;

        Button(final int id) {
            this.ID = id;
        }


        @Override
        public Event.Hub eventHub(State triggerState) {
            return this.ABSTRACT_BUTTON.eventHub(triggerState);
        }

        @Override
        public InstantEventQueue.Hub instantEventQueueHub(State triggerState) {
            return this.ABSTRACT_BUTTON.instantEventQueueHub(triggerState);
        }

        @Override
        public State getState() {
            return (GLFW.glfwGetMouseButton(GLFW.glfwGetCurrentContext(),
                                            this.ID) == GLFW.GLFW_PRESS) ?
                    Button.State.PRESSED :
                    Button.State.RELEASED;
        }
    }

    private static class PositionEventEntry {
        final BiPredicate<Double, Double> PREDICATE;
        // TODO trying to figure out how to close an event
        final Event.Signal SIGNAL = new Event.Signal();
        int index;
        boolean occurredOnPrev;

        PositionEventEntry(BiPredicate<Double, Double> predicate, int index) {
            this.PREDICATE = predicate;
            this.index = index;
        }

        synchronized void process(Position pos) {
            if (!this.PREDICATE.test(pos.x, pos.y)) {
                this.occurredOnPrev = false;
                return;
            }

            if (this.occurredOnPrev) {
                return;
            }

            this.SIGNAL.trigger();
            this.occurredOnPrev = true;
        }
    }

    private static final List<PositionEventEntry> POSITION_EVENT_ENTRIES =
            new ArrayList<>();

    static {
        GLFW.glfwSetMouseButtonCallback(GLFW.glfwGetCurrentContext(),
                                        Mouse::processButtonEvent);
        GLFW.glfwSetCursorPosCallback(GLFW.glfwGetCurrentContext(),
                                      Mouse::processPositionEvent);
    }

    //in framebuffer texels
    public static Position getPosition() {
        final double[] xCache = new double[1];
        final double[] yCache = new double[1];
        GLFW.glfwGetCursorPos(GLFW.glfwGetCurrentContext(), xCache, yCache);
        return Mouse.screenToFramebufferPosition(xCache[0], yCache[0]);
    }

    public static Event.Hub.Closeable positionEventHub(
            BiPredicate<Double, Double> positionPredicate) {
        final List<PositionEventEntry> ENTRIES = Mouse.POSITION_EVENT_ENTRIES;
        final PositionEventEntry ENTRY = new PositionEventEntry(
                positionPredicate, ENTRIES.size());
        ENTRIES.add(ENTRY);
        return new Event.Hub.Closeable() {
            @Override
            public void close() {
                if (ENTRY.SIGNAL.getClosureState() == ClosureState.CLOSED) {
                    return;
                }

                ENTRY.SIGNAL.close();
                final int LAST_INDEX = ENTRIES.size() - 1;
                Collections.swap(ENTRIES, ENTRY.index, LAST_INDEX);
                ENTRIES.get(ENTRY.index).index = ENTRY.index;
                ENTRIES.remove(LAST_INDEX);
            }

            @Override
            public boolean attachListener(Event.Listener listener) {
                return ENTRY.SIGNAL.getHub().attachListener(listener);
            }

            @Override
            public void detachListener(Event.Listener listener) {
                ENTRY.SIGNAL.getHub().detachListener(listener);
            }

            @Override
            public Event event() {
                return ENTRY.SIGNAL.getHub().event();
            }

            @Override
            public ClosureState getClosureState() {
                return ENTRY.SIGNAL.getHub().getClosureState();
            }
        };
    }

    //screen coordinates -> framebuffer
    private static AffineTransform screenToFramebufferTransform() {
        int[] winWidthCache = new int[1];
        int[] winHeightCache = new int[1];
        int[] fbWidthCache = new int[1];
        int[] fbHeightCache = new int[1];
        GLFW.glfwGetWindowSize(GLFW.glfwGetCurrentContext(),
                               winWidthCache,
                               winHeightCache);
        GLFW.glfwGetFramebufferSize(GLFW.glfwGetCurrentContext(),
                                    fbWidthCache,
                                    fbHeightCache);

        AffineTransform transform = AffineTransform.getScaleInstance(
                (double) fbWidthCache[0] / winWidthCache[0],
                (double) fbHeightCache[0] / winHeightCache[0]);
        transform.translate(0, winHeightCache[0]);
        transform.scale(1.0, -1.0);

        return transform;
    }

    private static void processButtonEvent(long window, int buttonId,
                                           int action, int mods) {
        final Instant TIMESTAMP = Instant.now();
        final Button.State STATE;
        switch (action) {
            case GLFW.GLFW_PRESS -> STATE = Button.State.PRESSED;
            case GLFW.GLFW_RELEASE -> STATE = Button.State.RELEASED;
            default -> {return;}
        }

        Mouse.Button.fromId(buttonId)
                    .ABSTRACT_BUTTON
                    .registerEvent(STATE, TIMESTAMP);
    }

    private static void processPositionEvent(long window, double x, double y) {
        final Position POS = Mouse.screenToFramebufferPosition(x, y);
        Mouse.POSITION_EVENT_ENTRIES.forEach(e -> e.process(POS));
    }

    private static Position screenToFramebufferPosition(double x, double y) {
        AffineTransform transform = Mouse.screenToFramebufferTransform();
        Point2D p = new Point2D.Double();
        transform.transform(new Point2D.Double(x, y), p);
        return new Position(p.getX(), p.getY());
    }

    private Mouse() {
        throw new UnsupportedOperationException("You shall not pass.");
    }

}

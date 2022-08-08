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

    private static abstract class AbstractPositionEventEntry implements
            AutoCloseable {
        final BiPredicate<Double, Double> PREDICATE;
        int index; //can be -1 to indicate that it is closed/removed
        boolean occurredOnPrev;

        AbstractPositionEventEntry(BiPredicate<Double, Double> predicate,
                                   int index) {
            this.PREDICATE = predicate;
            this.index = index;
        }

        void remove() {
            if (-1 == this.index) {
                return;
            }

            final List<AbstractPositionEventEntry> ENTRIES = this.getEntries();
            final int LAST_INDEX = ENTRIES.size() - 1;
            Collections.swap(ENTRIES, this.index, LAST_INDEX);
            ENTRIES.get(this.index).index = this.index;
            ENTRIES.remove(LAST_INDEX);
            this.index = -1;
        }

        //timestamp can be null
        synchronized void process(Position pos, Instant timestamp) {
            if (!this.PREDICATE.test(pos.x, pos.y)) {
                this.occurredOnPrev = false;
                return;
            }

            if (this.occurredOnPrev) {
                return;
            }

            this.trigger(timestamp);
            this.occurredOnPrev = true;
        }

        //timestamp can be null
        abstract void trigger(Instant timestamp);
        abstract List<AbstractPositionEventEntry> getEntries();
    }

    private static final class PositionEventEntry extends
            AbstractPositionEventEntry {
        static final List<AbstractPositionEventEntry> ENTRIES =
                new ArrayList<>();

        static PositionEventEntry of(BiPredicate<Double, Double> predicate) {
            final PositionEventEntry ENTRY = new PositionEventEntry(predicate,
                    PositionEventEntry.ENTRIES.size());
            PositionEventEntry.ENTRIES.add(ENTRY);
            return ENTRY;
        }

        static void process(long window, double x, double y) {
            final Position POS = Mouse.screenToFramebufferPosition(x, y);
            PositionEventEntry.ENTRIES.forEach(e -> e.process(POS, null));
        }

        final Event.Signal SIGNAL = new Event.Signal();

        PositionEventEntry(BiPredicate<Double, Double> predicate, int index) {
            super(predicate, index);
        }

        @Override
        public void close() {
            this.SIGNAL.close();
            this.remove();
        }

        @Override
        void trigger(Instant timestamp) {
            this.SIGNAL.trigger();
        }

        @Override
        List<AbstractPositionEventEntry> getEntries() {
            return PositionEventEntry.ENTRIES;
        }
    }

    private static final class PositionInstantEventQueueEntry extends
            AbstractPositionEventEntry {
        static final List<AbstractPositionEventEntry> ENTRIES =
                new ArrayList<>();

        static PositionInstantEventQueueEntry of(
                BiPredicate<Double, Double> predicate) {
            final PositionInstantEventQueueEntry ENTRY =
                    new PositionInstantEventQueueEntry(predicate,
                    PositionInstantEventQueueEntry.ENTRIES.size());
            PositionInstantEventQueueEntry.ENTRIES.add(ENTRY);
            return ENTRY;
        }

        static void process(long window, double x, double y) {
            final Instant NOW = Instant.now();
            final Position POS = Mouse.screenToFramebufferPosition(x, y);
            PositionInstantEventQueueEntry.ENTRIES.forEach(
                    e -> e.process(POS, NOW));
        }

        final InstantEventQueue.Signal SIGNAL = new InstantEventQueue.Signal();

        PositionInstantEventQueueEntry(BiPredicate<Double, Double> predicate,
                                       int index) {
            super(predicate, index);
        }

        @Override
        public void close() {
            this.SIGNAL.close();
            this.remove();
        }

        @Override
        void trigger(Instant timestamp) {
            this.SIGNAL.triggerElseNow(timestamp);
        }

        @Override
        List<AbstractPositionEventEntry> getEntries() {
            return PositionInstantEventQueueEntry.ENTRIES;
        }
    }

    static {
        GLFW.glfwSetMouseButtonCallback(GLFW.glfwGetCurrentContext(),
                                        Mouse::processButtonEvent);
        GLFW.glfwSetCursorPosCallback(GLFW.glfwGetCurrentContext(),
                                      Mouse.PositionEventEntry::process);
        GLFW.glfwSetCursorPosCallback(GLFW.glfwGetCurrentContext(),
                Mouse.PositionInstantEventQueueEntry::process);
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
        final PositionEventEntry ENTRY =
                PositionEventEntry.of(positionPredicate);
        return new Event.Hub.Closeable() {
            @Override
            public void close() {
                ENTRY.close();
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

    public static InstantEventQueue.Hub.Closeable positionInstantEventQueueHub(
            BiPredicate<Double, Double> positionPredicate) {
        final PositionInstantEventQueueEntry ENTRY =
                PositionInstantEventQueueEntry.of(positionPredicate);
        return new InstantEventQueue.Hub.Closeable() {
            @Override
            public void close() {
                ENTRY.close();
            }

            @Override
            public boolean attachListener(InstantEventQueue.Listener listener) {
                return ENTRY.SIGNAL.getHub().attachListener(listener);
            }

            @Override
            public void detachListener(InstantEventQueue.Listener listener) {
                ENTRY.SIGNAL.getHub().detachListener(listener);
            }

            @Override
            public InstantEventQueue unbounded() {
                return ENTRY.SIGNAL.getHub().unbounded();
            }

            @Override
            public InstantEventQueue.Builder eventBuilder() {
                return ENTRY.SIGNAL.getHub().eventBuilder();
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

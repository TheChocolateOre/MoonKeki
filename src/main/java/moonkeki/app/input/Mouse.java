package moonkeki.app.input;

import moonkeki.app.events.Event;
import moonkeki.app.events.InstantEventQueue;
import org.lwjgl.glfw.GLFW;

import java.time.Instant;
import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.Objects;
import java.util.stream.Collectors;

public final class Mouse {

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

    static {
        GLFW.glfwSetMouseButtonCallback(GLFW.glfwGetCurrentContext(),
                                        Mouse::processEvent);
    }

    private static void processEvent(long window, int buttonId, int action,
                                     int mods) {
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

    private Mouse() {
        throw new UnsupportedOperationException("You shall not pass.");
    }

}

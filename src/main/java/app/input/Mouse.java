package app.input;

import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class Mouse {

    public static final class Button extends app.input.Button {
        public static final Mouse.Button LEFT = new Button(
                GLFW.GLFW_MOUSE_BUTTON_LEFT);
        public static final Mouse.Button MIDDLE = new Button(
                GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
        public static final Mouse.Button RIGHT = new Button(
                GLFW.GLFW_MOUSE_BUTTON_RIGHT);
        public static final Mouse.Button N4 = new Button(
                GLFW.GLFW_MOUSE_BUTTON_4);
        public static final Mouse.Button N5 = new Button(
                GLFW.GLFW_MOUSE_BUTTON_5);
        public static final Mouse.Button N6 = new Button(
                GLFW.GLFW_MOUSE_BUTTON_6);
        public static final Mouse.Button N7 = new Button(
                GLFW.GLFW_MOUSE_BUTTON_7);
        public static final Mouse.Button N8 = new Button(
                GLFW.GLFW_MOUSE_BUTTON_8);

        private Button(final int id) {
            super(id);
        }

        @Override
        public boolean isPressed() {
            return GLFW.glfwGetMouseButton(GLFW.glfwGetCurrentContext(),
                    this.ID) == GLFW.GLFW_PRESS;
        }

        private static Optional<Mouse.Button> fromId(final int id) {
            return Optional.ofNullable(switch (id) {
                case GLFW.GLFW_MOUSE_BUTTON_LEFT -> Mouse.Button.LEFT;
                case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> Mouse.Button.MIDDLE;
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> Mouse.Button.RIGHT;
                case GLFW.GLFW_MOUSE_BUTTON_4 -> Mouse.Button.N4;
                case GLFW.GLFW_MOUSE_BUTTON_5 -> Mouse.Button.N5;
                case GLFW.GLFW_MOUSE_BUTTON_6 -> Mouse.Button.N6;
                case GLFW.GLFW_MOUSE_BUTTON_7 -> Mouse.Button.N7;
                case GLFW.GLFW_MOUSE_BUTTON_8 -> Mouse.Button.N8;
                default -> null;
            });
        }
    }//end static nested class Button

    //id -> button (only for non-named buttons, if exist)
    private static final Map<Integer, Mouse.Button> ID_TO_BUTTON =
            new ConcurrentHashMap<>();

    private static final ReadWriteLock BUTTON_EVENTS_LOCK =
            new ReentrantReadWriteLock(true);
    private static final Map<Mouse.Button, Set<ButtonEventOld>> BUTTON_EVENTS =
            new HashMap<>();

    private static final Queue<ButtonSeeker> pressedButtonSeekers =
            new ConcurrentLinkedQueue<>();
    private static final Queue<ButtonSeeker> releasedButtonSeekers =
            new ConcurrentLinkedQueue<>();

    static {
        GLFW.glfwSetMouseButtonCallback(GLFW.glfwGetCurrentContext(), (long
                window, int button, int action, int mods) -> {
            //We get the type of the event (released | pressed)
            final boolean PRESSED;
            if (GLFW.GLFW_RELEASE == action) {
                PRESSED = false;
            } else if (GLFW.GLFW_PRESS == action) {
                PRESSED = true;
            } else {
                return;
            }//end if

            //We get the Button associated with the current callback
            final Mouse.Button BUTTON = Mouse.Button.fromId(button)
                    .orElseGet(() -> Mouse.getButton(button));

            //We process the ButtonSeeker's'
            ButtonSeeker buttonSeeker = PRESSED ?
                    Mouse.pressedButtonSeekers.poll() :
                    Mouse.releasedButtonSeekers.poll();
            if (buttonSeeker != null) {
                buttonSeeker.putIfAbsent(BUTTON);
            }//end if

            //We process the ButtonEvent's'
            var buttonSnapshot = new Mouse.Button.Snapshot(BUTTON, PRESSED);
            Mouse.BUTTON_EVENTS_LOCK.readLock().lock();
            try {
                Mouse.BUTTON_EVENTS.getOrDefault(BUTTON, Collections.emptySet())
                                   .forEach(e -> e.update(buttonSnapshot));
            } finally {
                Mouse.BUTTON_EVENTS_LOCK.readLock().unlock();
            }//end try
        });
    }//end static initializer

    public static ButtonSeeker seekNextButton(final boolean pressed) {
        ButtonSeeker buttonSeeker = new ButtonSeeker();
        if (pressed) {
            Mouse.pressedButtonSeekers.add(buttonSeeker);
        } else {
            Mouse.releasedButtonSeekers.add(buttonSeeker);
        }//end if

        return buttonSeeker;
    }

    static void addButtonEvent(Mouse.Button button, ButtonEventOld event) {
        Mouse.BUTTON_EVENTS_LOCK.writeLock().lock();
        try {
            Mouse.BUTTON_EVENTS.computeIfAbsent(button, k ->
                    new LinkedHashSet<>()).add(event);
        } finally {
            Mouse.BUTTON_EVENTS_LOCK.writeLock().unlock();
        }//end try
    }

    static void removeButtonEvent(Mouse.Button button, ButtonEventOld event) {
        Mouse.BUTTON_EVENTS_LOCK.writeLock().lock();
        try {
            Mouse.BUTTON_EVENTS.getOrDefault(button, Collections.emptySet())
                    .remove(event);
        } finally {
            Mouse.BUTTON_EVENTS_LOCK.writeLock().unlock();
        }//end try
    }

    //trust-based, no argument validation
    private static Mouse.Button getButton(int id) {
        return Mouse.ID_TO_BUTTON.computeIfAbsent(id, Button::new);
    }

    private Mouse() {
        throw new UnsupportedOperationException("You shall not pass");
    }

}//end Mouse class

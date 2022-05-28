package moonkeki.app.input;

import moonkeki.app.events.Event;
import moonkeki.app.events.InstantEventQueue;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

abstract class AbstractButton implements Button {
    private final Map<State, Event.Signal> EVENT_SIGNALS =
            new EnumMap<>(Map.of(Keyboard.Button.State.RELEASED,
                                 new Event.Signal(),
                                 Keyboard.Button.State.PRESSED,
                                 new Event.Signal()));
    private final Map<Keyboard.Button.State, InstantEventQueue.Signal>
            INSTANT_EVENT_QUEUE_SIGNALS =
            new EnumMap<>(Map.of(Keyboard.Button.State.RELEASED,
                                 new InstantEventQueue.Signal(),
                                 Keyboard.Button.State.PRESSED,
                                 new InstantEventQueue.Signal()));

    @Override
    public Event.Hub eventHub(State triggerState) {
        return this.EVENT_SIGNALS.get(triggerState).getHub();
    }

    @Override
    public InstantEventQueue.Hub instantEventQueueHub(State triggerState) {
        return this.INSTANT_EVENT_QUEUE_SIGNALS.get(triggerState).getHub();
    }

    void registerEvent(Keyboard.Button.State state, Instant timestamp) {
        this.INSTANT_EVENT_QUEUE_SIGNALS.get(state)
                                        .triggerElseNow(timestamp);
        this.EVENT_SIGNALS.get(state)
                          .trigger();
    }
}

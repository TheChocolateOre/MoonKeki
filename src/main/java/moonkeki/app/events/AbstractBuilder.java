package moonkeki.app.events;

import java.util.Objects;

abstract class AbstractBuilder {

    Integer capacity; //if null, unbounded (the default)
    ReplacementRule replacementRule = ReplacementRule.NONE;

    AbstractBuilder ofCapacity(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Argument capacity can't be " +
                                               "negative.");
        }

        this.capacity = capacity;
        return this;
    }

    AbstractBuilder ofReplacementRule(ReplacementRule replacementRule) {
        this.replacementRule = Objects.requireNonNull(replacementRule);
        return this;
    }

}

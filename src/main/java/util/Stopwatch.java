package util;

/**
 * This class represents a stopwatch, to capture time durations and calculate
 * elapsed time. It can not capture time durations longer than 2^63 - 1
 * nanoseconds (~292 years), due to numerical overflow.
 * @author TheChocolateOre
 */
public class Stopwatch {

    /**
     * An arbitrary time stamp in nanoseconds of the time this Stopwatch started
     * capturing time. Null indicates that this Stopwatch has not been started
     * yet.
     */
    private Long startTimeStamp;

    /**
     * The captured duration of this Stopwatch, in nanoseconds.
     */
    private long duration;

    /**
     * Starts this Stopwatch, so it can capture time.
     */
    public void start() {
        //Checks if this Stopwatch has already been started
        if (this.isStarted()) {
            return;
        }//end if

        //Gets the system nano time of when this Stopwatch started
        this.startTimeStamp = System.nanoTime();
    }

    /**
     * Pauses this Stopwatch, but it reserves its captured duration so far, so
     * it can be started again from where it was left.
     */
    public void pause() {
        //Checks if this Stopwatch has not been started yet
        if (!this.isStarted()) {
            return;
        }//end if

        //Stores the duration so far
        this.duration += System.nanoTime() - this.startTimeStamp;
        //Resets the starting time stamp
        this.startTimeStamp = null;
    }

    /**
     * Stops this Stopwatch and clears its recorded duration.
     */
    public void reset() {
        //Resets the starting time stamp
        this.startTimeStamp = null;
        //Clears the captured duration
        this.duration = 0L;
    }

    /**
     * Refreshes the starting time stamp of this Stopwatch. It will not effect
     * the captured duration of this Stopwatch.
     */
    public void refresh() {
        //It pauses to store the duration so far
        this.pause();
        //It starts to get a new starting time stamp
        this.start();
    }

    /**
     * Stops this Stopwatch, clears its recorded duration and restarts.
     */
    public void restart() {
        //Stops and clears this Stopwatch
        this.reset();
        //Starts this Stopwatch
        this.start();
    }

    /**
     * Gets the captured duration of this Stopwatch so far, in nanoseconds.
     * @return The captured duration of this Stopwatch so far, in nanoseconds.
     */
    public long getDuration() {
        return this.duration + (this.isStarted() ? System.nanoTime() -
                this.startTimeStamp : 0L);
    }

    /**
     * Gets the captured duration of this Stopwatch so far, in seconds.
     * @return The captured duration of this Stopwatch so far, in seconds.
     */
    public double getDurationSec() {
        return this.getDuration() / 1_000_000_000.0;
    }

    /**
     * Indicates if this Stopwatch has been started.
     * @return True if this Stopwatch has been started, otherwise false.
     */
    public boolean isStarted() {
        return this.startTimeStamp != null;
    }

    /**
     * Indicates if this Stopwatch contains a duration greater or equal to a
     * given one.
     * @param duration A duration in nanoseconds.
     * @return True if this Stopwatch contains a duration greater or equal to
     * the given one, otherwise false.
     */
    public boolean hasElapsed(long duration) {
        return this.getDuration() >= duration;
    }

    /**
     * Indicates if this Stopwatch contains a duration greater or equal to a
     * given one.
     * @param duration A duration in seconds.
     * @return True if this Stopwatch contains a duration greater or equal to
     * the given one, otherwise false.
     */
    public boolean hasElapsed(double duration) {
        return this.getDurationSec() >= duration;
    }

}//end class Stopwatch

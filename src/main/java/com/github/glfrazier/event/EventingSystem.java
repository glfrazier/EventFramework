package com.github.glfrazier.event;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.github.glfrazier.objectpool.AbstractPooledObject;
import com.github.glfrazier.objectpool.ObjectPool;

public class EventingSystem implements Runnable {

	private TimeUnit finestTimeUnit = TimeUnit.MILLISECONDS;
	private boolean realTime = true;
	private Double realTimeMultiplier = null;
	private PriorityQueue<QueuedEvent> queue;
	private long currentTime;
	private long endTime;
	private long startTime;
	private boolean verbose;
	private boolean endWhenEmpty = false;

	private int threadCount;

	private long totalEventsDelivered = 0;
	private int maxQueueLength = 0;

	private Set<EndCondition> endConditionsForEmptyQueue = null;
	private Set<EndCondition> endConditionsForEventDelivery = null;

	private QueuedEventPool qePool = new QueuedEventPool();
	private String name;
	private boolean terminated;;

	/**
	 * Construct an EventingSystem with default attributes:
	 * <ul>
	 * <li><code>finestTimeUnit == {@link}TimeUnit.MILLISECONDS</code></li>
	 * <li><code>realTime = false</code></li>
	 * <li><code>realTimeMultiplier =</code> n/a</li>
	 * </ul>
	 * 
	 * @see #setFinestTimeUnit(TimeUnit)
	 */
	public EventingSystem() {
		queue = new PriorityQueue<QueuedEvent>();
	}

	public EventingSystem(String name) {
		this();
		this.name = name;
	}

	/**
	 * Schedule an event to be delivered <code>timeRelative</code> time units in the
	 * future.
	 * 
	 * @param target       the EventProcess to deliver the event to
	 * @param e            the Event to deliver
	 * @param timeRelative the distance in the future to deliver the event, in the
	 *                     system's {@link finestTimeUnit}.
	 * @throws IllegalArgumentException if <code>timeRelative</code> is negative or
	 *                                  zero
	 * @see #getFinestTimeUnit()
	 */
	public void scheduleEventRelative(EventProcessor target, Event e, long timeRelative) {
		scheduleEventRelative(target, e, timeRelative, finestTimeUnit);
	}

	/**
	 * Schedule an event to be delivered at time <code>time</code>. If the system is
	 * running in real time, this may be a clock time. If not-real-time, this may be
	 * in the EventingSystem's time frame (e.g., where the system started at time
	 * 0).
	 * 
	 * @param target the EventProcess to deliver the event to
	 * @param e      the Event to deliver
	 * @param time   the time to deliver the event, in the system's
	 *               {@link finestTimeUnit}.
	 * @see #getFinestTimeUnit()
	 */
	public void scheduleEventAbsolute(EventProcessor target, Event e, long time) {
		scheduleEventAbsolute(target, e, time, finestTimeUnit);
	}

	/**
	 * Schedule an event to be delivered <code>timeRelative timeUnits</code> in the
	 * future.
	 * 
	 * @param target       the EventProcess to deliver the event to
	 * @param e            the Event to deliver
	 * @param timeRelative the distance in the future to deliver the event, in the
	 *                     system's {@link finestTimeUnit}.
	 * @param timeUnit     the time unit of <code>timeRelative</code>. Should be
	 *                     equal or less granular than the {@link #finestTimeUnit}.
	 * @throws IllegalArgumentException if <code>timeRelative</code> is negative or
	 *                                  zero
	 * @see #getFinestTimeUnit()
	 */
	public void scheduleEventRelative(EventProcessor target, Event e, long timeRelative, TimeUnit timeUnit) {
		if (timeRelative < 0) {
			throw new IllegalArgumentException("Events must be scheduled for now or in the future (timeRelative >= 0)");
		}
		long now = getCurrentTime();
		scheduleEventAbsolute(target, e, now + finestTimeUnit.convert(timeRelative, timeUnit));
	}

	public void scheduleEventAbsolute(EventProcessor target, Event e, long time, TimeUnit timeUnit) {
		if (finestTimeUnit.convert(time, timeUnit) < getCurrentTime()) {
			throw new IllegalArgumentException("Event scheduled for " + finestTimeUnit.convert(time, timeUnit)
					+ ", current time = " + getCurrentTime());
		}
		QueuedEvent qe = qePool.allocate(target, e, finestTimeUnit.convert(time, timeUnit));
		appendQueuedEvent(qe);
	}

	/**
	 * Schedule an event to be delivered now.
	 * 
	 * @param target the receipient of the event
	 * @param e      the event to deliver
	 */
	public void scheduleEvent(EventProcessor target, Event e) {
		QueuedEvent qe = qePool.allocate(target, e);
		appendQueuedEvent(qe);
	}

	private void appendQueuedEvent(QueuedEvent qe) {
		if (verbose) {
			System.err.println(getCurrentTime() + ":\t" + this + " appending {" + qe + "}");
			System.err.flush();
		}
		synchronized (queue) {
			queue.add(qe);
			int l = queue.size();
			if (l > maxQueueLength) {
				maxQueueLength = l;
			}
			if (queue.peek() == qe) {
				queue.notifyAll();
			}
		}
	}

	/**
	 * Obtain the current time of the system. If the system is operating in real
	 * time, then this is either the clock time or, if the EventSystem is operating
	 * in its own timeframe, the number of {@link #finestTimeUnit} units that have
	 * expired since the EventSystem started. If the system is operating in
	 * simulation time, this is the time that the most-recently delivered event was
	 * delivered.
	 * 
	 * @return the current time, in {@link #finestTimeUnit} time units.
	 */
	public long getCurrentTime() {
		return getCurrentTime(finestTimeUnit);
	}
	
	public long getStartTime() {
		return startTime;
	}
	
	public long getElapsedTime() {
		return getCurrentTime() - startTime;
	}

	/**
	 * Get the current time in the specified time unit.
	 * 
	 * @param timeUnit the time unit of the result.
	 * @return the current time.
	 * @see #getCurrentTime()
	 */
	public long getCurrentTime(TimeUnit timeUnit) {
		if (realTime) {
			return timeUnit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
		}
		return timeUnit.convert(currentTime, finestTimeUnit);
	}

	public void setFinestTimeUnit(TimeUnit timeUnit) {
		this.finestTimeUnit = timeUnit;
	}

	public TimeUnit getFinestTimeUnit() {
		return finestTimeUnit;
	}

	/**
	 * Set the time at which the EventFramework terminates. This is an absolute
	 * time, but if the EventFramework is operating in non-realtime mode, the
	 * framework starts at time zero, and so this is also the offset from zero. Time
	 * is in the unit of the {@link #finestTimeUnit}, which is TimeUnit.MILLISECONDS
	 * by default.
	 * 
	 * @see #setFinestTimeUnit(TimeUnit)
	 * @see #setEndTime(long, TimeUnit)
	 * @see #setEndTimeRelative(long)
	 * @see #setEndTimeRelative(long, TimeUnit)
	 * 
	 * @param endTime no events will be delivered after this time
	 */
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	/**
	 * Set the time at which the EventFramework terminates. This is an absolute
	 * time, but if the EventFramework is operating in non-realtime mode, the
	 * framework starts at time zero.
	 * 
	 * @param endTime no events will be delivered after this time
	 * @param unit    the unit in which <code>endTime</code> is specified.
	 */
	public void setEndTime(long endTime, TimeUnit unit) {
		this.endTime = finestTimeUnit.convert(endTime, unit);
	}

	/**
	 * Set the time at which the EventFramework terminates. This is relative to the
	 * current time.
	 * 
	 * @param deltaTime the distance from the current time at which no events will
	 *                  be delivered after this time
	 * @param unit      the time units in which <code>deltaTime</code> is specified
	 */
	public void setEndTimeRelative(long deltaTime, TimeUnit unit) {
		endTime = getCurrentTime() + finestTimeUnit.convert(deltaTime, unit);
	}

	/**
	 * Set the time at which the EventFramework terminates.
	 * 
	 * @param deltaTime the relative endTime
	 */
	public void setEndTimeRelative(long deltaTime) {
		endTime = getCurrentTime() + deltaTime;
	}

	@Override
	public void run() {
		if (verbose) {
			System.err.println(this + " entered run().");
			System.err.flush();
		}
		synchronized (this) {
			threadCount++;
		}
		boolean running = true;
		startTime = getCurrentTime();
		QueuedEvent qe = null;
		while (running && !terminated) {
			if (qe != null) {
				qe.release();
			}
			synchronized (queue) {
				qe = queue.poll();
				while (qe == null && running && !terminated) {
					if (endWhenEmpty) {
						running = false;
						continue;
					}
					// else, wait for a new event to arrive
					while (queue.isEmpty()) {
						if (endConditionsForEmptyQueue != null) {
							for (EndCondition ec : endConditionsForEmptyQueue) {
								if (ec.taskIsComplete()) {
									running = false;
									break;
								}
							}
							if (!running || terminated) {
								break;
							}
						}
						if (verbose) {
							System.err.println(this + " waiting for the queue to become not-empty.");
							System.err.flush();
						}
						try {
							long waitMillis = 0;
							if (endConditionsForEmptyQueue != null) {
								waitMillis = 100; // check condition every 10th of a second
							}
							queue.wait(waitMillis, 0);
						} catch (InterruptedException e) {
							running = false;
							break;
						}
						if (verbose) {
							System.err.println(this + " woken up.");
							System.err.flush();
						}
					}
					qe = queue.poll();
				}
				if (qe == null || running == false) {
					running = false;
					continue;
				}

				if (realTime && qe.deliveryTime != null) {
					if (verbose) {
						System.out.println(this + " has an event to be delivered at " + qe.deliveryTime);
					}
					long now = System.currentTimeMillis();
					if (now < qe.deliveryTime) {
						long delta = qe.deliveryTime - now;
						if (verbose) {
							System.out.println("now=" + now + ", wait for " + delta + " ms");
						}
						queue.add(qe);
						qe = null;
						try {
							queue.wait(delta);
						} catch (InterruptedException e) {
							running = false;
							terminated = true;
							break;
						}
						continue;
					}
				}

			}
			if (endConditionsForEventDelivery != null) {
				for (EndCondition ec : endConditionsForEventDelivery) {
					if (ec.taskIsComplete()) {
						running = false;
						break;
					}
				}
				if (!running) {
					continue;
				}
			}
			totalEventsDelivered++;
			Long now = qe.getDeliveryTime();
			if (now != null) {
				currentTime = now;
			}
			if (endTime > 0 && currentTime > endTime) {
				return;
			}
			if (verbose) {
				System.err.println(
						getCurrentTime() + ":\t" + this + " delivering <" + qe.getEvent() + "> to " + qe.getTarget());
				System.err.flush();
			}
			qe.getTarget().process(qe.getEvent(), this);
		}
		if (verbose) {
			System.err.println(
					currentTime + ":\t" + this + " terminating the run loop. qe = " + qe + ", running = " + running);
			System.err.flush();
		}
		synchronized (this) {
			threadCount--;
		}
	}

	/**
	 * A struct used to hold the elements of a queued event. It is a
	 * {@link Comparable} class; the events are sorted in the queue based on their
	 * delivery times. A <code>null</code> delivery means deliver
	 * immediately&mdash;such objects move to the front of the queue.
	 * 
	 * <code>QueuedEvent</code> extends {@link AbstractPooledObject} and is pooled.
	 * 
	 * The class is declared <code>protected</code> because the constructor must be
	 * accessible to the {@link QueuedEventPool}.
	 * 
	 * @author Greg Frazier
	 *
	 */
	protected static class QueuedEvent extends AbstractPooledObject implements Comparable<QueuedEvent> {

		private EventProcessor target;
		private Event event;
		private Long deliveryTime;

		public QueuedEvent(ObjectPool<QueuedEvent> pool) {
			super(pool);
		}

		public void initialize(EventProcessor target, Event event, Long deliveryTime) {
			super.initialize();
			this.target = target;
			this.event = event;
			this.deliveryTime = deliveryTime;
		}

		public void initialize(EventProcessor target, Event event) {
			super.initialize();
			this.target = target;
			this.event = event;
			this.deliveryTime = null;
		}

		public EventProcessor getTarget() {
			return target;
		}

		public Long getDeliveryTime() {
			return deliveryTime;
		}

		public Event getEvent() {
			return event;
		}

		/**
		 * Sorts events by their delivery time.
		 * 
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(QueuedEvent qe) {
			if (deliveryTime == null) {
				if (qe.deliveryTime == null) {
					return 0;
				}
				return -1;
			}
			if (qe.deliveryTime == null) {
				return 1;
			}
			return deliveryTime.compareTo(qe.deliveryTime);
		}

		@Override
		public String toString() {
			return "event <" + event + "> to be delivered @" + deliveryTime + " to " + target;
		}

	}

	/**
	 * The {@link ObjectPool} that holds the {@link QueuedEvents}.
	 * 
	 * @author Greg Frazier
	 *
	 */
	private static class QueuedEventPool extends ObjectPool<QueuedEvent> {

		public QueuedEventPool() {
			super(QueuedEvent.class);
		}

		public QueuedEvent allocate(EventProcessor target, Event event, Long deliveryTime) {
			QueuedEvent r = super.getInstance();
			r.initialize(target, event, deliveryTime);
			return r;
		}

		public QueuedEvent allocate(EventProcessor target, Event event) {
			QueuedEvent r = super.getInstance();
			r.initialize(target, event);
			return r;
		}

	}

	/**
	 * Specify whether the eventing system should exit the run() method when the
	 * event queue is empty (<code>on = true</code>). The default behavior is for
	 * the run method to block, waiting for an event to arrive.
	 * 
	 * @param on when <code>true</code>, sets the eventing system to exit when the
	 *           event queue is empty
	 */
	public void exitOnEmptyQueue(boolean on) {
		endWhenEmpty = on;
	}

	public long getTotalEventsDelivered() {
		return totalEventsDelivered;
	}

	public void setVerbose(boolean v) {
		verbose = v;
	}

	@Override
	public String toString() {
		if (name != null) {
			return name;
		}
		return super.toString();
	}

	/**
	 * Register an end condition that is checked whenever the run thread encounters
	 * an empty event queue. Registering end conditions here is only relevant if the
	 * eventing system is set to not terminate when the queue is empty.
	 * 
	 * @see #waitOnEmptyQueue()
	 * @see #exitOnEmptyQueue()
	 * 
	 * @param ec The end condition to be checked.
	 */
	public void registerEndConditionOnEmptyQueue(EndCondition ec) {
		if (endConditionsForEmptyQueue == null) {
			endConditionsForEmptyQueue = new HashSet<EndCondition>();
		}
		endConditionsForEmptyQueue.add(ec);
	}

	/**
	 * Register an end condition that is checked prior to every event delivery. Note
	 * the potential performance impact.
	 * 
	 * @param ec The end condition to be checked.
	 */
	public void registerEndConditionOnEventDelivery(EndCondition ec) {
		if (endConditionsForEventDelivery == null) {
			endConditionsForEventDelivery = new HashSet<EndCondition>();
		}
		endConditionsForEventDelivery.add(ec);
	}

	/**
	 * Objects that can tell a processing thread when to terminate implement this
	 * interface.
	 * 
	 * @author Greg Frazier
	 *
	 */
	public static interface EndCondition {

		/**
		 * Note that there are two methods for registering end conditions; they differ
		 * in when the test is applied.
		 * 
		 * @return <code>true</code> if this processing thread should terminate.
		 */
		public boolean taskIsComplete();
	}

	public int getQueueLength() {
		synchronized (queue) {
			return queue.size();
		}
	}

	public int getMaxQueueLength() {
		synchronized (queue) {
			return maxQueueLength;
		}
	}

	public void terminate() {
		synchronized (queue) {
			terminated = true;
			queue.clear();
			queue.notifyAll();
		}
	}

	public boolean isTerminated() {
		// TODO Auto-generated method stub
		return false;
	}

	public void setRealTime(boolean b) {
		realTime = b;
	}
}

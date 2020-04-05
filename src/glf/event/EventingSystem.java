package glf.event;

import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

import example.Record;
import glf.objectpool.AbstractPooledObject;
import glf.objectpool.ObjectPool;

public class EventingSystem implements Runnable {

	private TimeUnit finestTimeUnit = TimeUnit.MILLISECONDS;
	private boolean realTime = false;
	private Double realTimeMultiplier = null;
	private PriorityQueue<QueuedEvent> queue;
	private long currentTime;
	private long endTime;
	private boolean verbose;
	private boolean running;
	private boolean endWhenEmpty = true;
	
	private QueuedEventPool qePool = new QueuedEventPool();

	/**
	 * Construct an EventingSystem with default attributes:
	 * <ul>
	 * <li><code>finestTimeUnit == {@link}TimeUnit.MILLISECONDS</code></li>
	 * <li><code>realTime = false</code></li>
	 * <li><code>realTimeMultiplier =</code> n/a</li>
	 * </ul>
	 * 
	 * @see #setFinestTimeUnit(TimeUnit)
	 * @see #setRealTime(boolean)
	 * @see #setRealTime(double)
	 */
	public EventingSystem() {
		queue = new PriorityQueue<QueuedEvent>();
	}

	public void scheduleEventRelative(EventProcessor target, Event e, long timeRelative) {
		scheduleEventRelative(target, e, timeRelative, finestTimeUnit);
	}

	public void scheduleEventAbsolute(EventProcessor target, Event e, long time) {
		scheduleEventAbsolute(target, e, time, finestTimeUnit);
	}

	public void scheduleEventRelative(EventProcessor target, Event e, long timeRelative, TimeUnit timeUnit) {
		long now = getCurrentTime();
		scheduleEventAbsolute(target, e, now + finestTimeUnit.convert(timeRelative, timeUnit));
	}

	public void scheduleEventAbsolute(EventProcessor target, Event e, long time, TimeUnit timeUnit) {
		QueuedEvent qe = qePool.allocate(target, e, finestTimeUnit.convert(time, timeUnit));
		appendQueuedEvent(qe);
	}

	public void scheduleEvent(EventProcessor target, Event e) {
		QueuedEvent qe = qePool.allocate(target, e);
		appendQueuedEvent(qe);
	}

	private void appendQueuedEvent(QueuedEvent qe) {
		synchronized (queue) {
			boolean wasEmpty = queue.isEmpty();
			queue.add(qe);
			if (wasEmpty) {
				queue.notifyAll();
			}
		}
	}

	public long getCurrentTime() {
		return getCurrentTime(finestTimeUnit);
	}

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
	 * framework starts at time zero. Time is in the unit of the
	 * <code>finestTimeUnit</code>, which is TimeUnit.MILLISECONDS by default.
	 * 
	 * @see #setFinestTimeUnit(TimeUnit)
	 * @see #setEndTime(long, TimeUnit)
	 * @see #setEndTimeRelative(long)
	 * @see #setEndTimeRelative(long, TimeUnit)
	 * 
	 * @param endTime
	 *            no events will be delivered after this time
	 */
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	/**
	 * Set the time at which the EventFramework terminates. This is an absolute
	 * time, but if the EventFramework is operating in non-realtime mode, the
	 * framework starts at time zero.
	 * 
	 * @param endTime
	 *            no events will be delivered after this time
	 */
	public void setEndTime(long endTime, TimeUnit unit) {
		this.endTime = finestTimeUnit.convert(endTime, unit);
	}

	/**
	 * Set the time at which the EventFramework terminates. This is an absolute
	 * time, but if the EventFramework is operating in non-realtime mode, the
	 * framework starts at time zero.
	 * 
	 * @param endTime
	 *            no events will be delivered after this time
	 */
	public void setEndTimeRelative(long deltaTime, TimeUnit unit) {
		endTime = getCurrentTime() + finestTimeUnit.convert(deltaTime, unit);
	}

	/**
	 * Set the time at which the EventFramework terminates.
	 * 
	 * @param endTime
	 */
	public void setEndTimeRelative(long deltaTime) {
		endTime = getCurrentTime() + deltaTime;
	}

	@Override
	public void run() {
		running = true;
		QueuedEvent qe = null;
		while (running) {
			if (qe != null) {
				qe.release();
			}
			synchronized (queue) {
				qe = queue.poll();
				if (qe == null) {
					if (endWhenEmpty) {
						running = false;
						return;
					}
					// else, wait for a new event to arrive
					while (queue.isEmpty()) {
						try {
							queue.wait();
						} catch (InterruptedException e) {
							running = false;
							return;
						}
					}
				}
			}
			Long now = qe.getDeliveryTime();
			if (now != null) {
				currentTime = now;
			}
			qe.getTarget().process(qe.getEvent(), this);
		}
	}

	private class QueuedEvent extends AbstractPooledObject implements Comparable<QueuedEvent> {

		private EventProcessor target;
		private Event event;
		private Long deliveryTime;

		public QueuedEvent(QueuedEventPool pool) {
			super(pool);
		}

		public void initialize(EventProcessor target, Event event, Long deliveryTime) {
			this.target = target;
			this.event = event;
			this.deliveryTime = deliveryTime;
		}

		public void initialize(EventProcessor target, Event event) {
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

	}

	private class QueuedEventPool extends ObjectPool<QueuedEvent> {

		public QueuedEventPool() {
			super(QueuedEvent.class);
		}

		public synchronized QueuedEvent allocate(EventProcessor target, Event event, Long deliveryTime) {
			QueuedEvent r = super.getInstance();
			r.initialize(target, event, deliveryTime);
			return r;
		}

		public synchronized QueuedEvent allocate(EventProcessor target, Event event) {
			QueuedEvent r = super.getInstance();
			r.initialize(target, event);
			return r;
		}

	}

}

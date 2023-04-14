package com.github.glfrazier.event.util;

import com.github.glfrazier.event.Event;
import com.github.glfrazier.event.EventProcessor;
import com.github.glfrazier.event.EventingSystem;

/**
 * Synchronizes two EventingSystems. The Synchronizer implements a barrier such
 * that all participating EventingSystems pause at the end of each time interval
 * until all of the EventingSystems reach the end of the time interval. Sample
 * code for using the Synchronizer is below. Be sure to only schedule the
 * synchronizer once for each participating thread; if you schedule it twice,
 * that thread will be double-counted, and some other thread(s) will not be
 * waited for. Note also that the EventingSystems will always have an event in
 * them, and so you cannot terminate an execution simply by ceasing to supply
 * events.
 * 
 * <pre>
 * EventingSystem es1 = new EventingSystem("ES1", NOT_REALTIME);
 * EventingSystem es2 = new EventingSystem("ES2", NOT_REALTIME);
 * Synchronizer sync = new Synchronizer(2, 100); // synchronize two eventing systems every 100 ms
 * es1.scheduleEventRelative(sync, Synchronizer.SYNC_EVENT, 100);
 * es2.scheduleEventRelative(sync, Synchronizer.SYNC_EVENT, 100);
 * Thread t1 = new Thread(es1);
 * Thread t2 = new Thread(es2);
 * t1.start();
 * t2.start();
 * </pre>
 * 
 * @author Greg Frazier
 *
 */
public class Synchronizer implements EventProcessor {

	private int numberOfEventingSystems;
	private long interval;
	private int threadCount;
	private boolean verbose;
	private Thread[] threads;
	private boolean[] threadSeenAlive;

	private boolean toggle;

	public Synchronizer(int numberOfEventingSystems, long interval) {
		this(numberOfEventingSystems, interval, false);
	}

	public Synchronizer(int numberOfEventingSystems, long interval, boolean verbose) {
		this.numberOfEventingSystems = numberOfEventingSystems;
		this.interval = interval;
		this.verbose = verbose;
	}

	public Synchronizer(Thread[] threads, long interval, EventingSystem eventingSystem) {
		this(threads, interval, eventingSystem, false);
	}

	public Synchronizer(Thread[] threads, long interval, EventingSystem eventingSystem, boolean verbose) {
		this.numberOfEventingSystems = threads.length;
		this.interval = interval;
		this.verbose = verbose;
		this.threads = threads;
		this.threadSeenAlive = new boolean[threads.length];
		for (int i = 0; i < threads.length; i++) {
			threadSeenAlive[i] = false;
			eventingSystem.scheduleEvent(this, Event.EVENT);
		}
	}

	@Override
	public void process(Event e, EventingSystem eventingSystem, long deliveryTime) {
		synchronized (this) {
			if (verbose) {
				System.out.println(this + ": receveived " + e + " from " + eventingSystem);
			}
			threadCount++;
			boolean localToggle = toggle;
			if (threadCount == numberOfEventingSystems) {
				threadCount = 0;
				toggle = !toggle;
				if (verbose) {
					System.out.println(this + ": =====================================");
				}
				notifyAll();
			} else {
				while (localToggle == toggle) {
					try {
						if (threads != null) {
							for (int i = 0; i < threads.length; i++) {
								if (!threads[i].isAlive()) {
									if (threadSeenAlive[i]) {
										System.out.println("SYNC: " + Thread.currentThread()
												+ " has detected that thread " + threads[i] + " is not alive.");
										return;
									}
									// we have not yet seen this thread alive, so ignore the fact that it is dead.
								} else {
									threadSeenAlive[i] = true;
								}
							}
						} else {
							System.out.println("SYNC: " + Thread.currentThread() + ": there is no threads array.");
						}
						wait(500);
					} catch (InterruptedException e1) {
						return;
					}
				}
			}
			if (verbose) {
				System.out.println(this + ": released " + eventingSystem);
			}
		}
		eventingSystem.scheduleEventAbsolute(this, e, deliveryTime + interval);
	}

	@Override
	public String toString() {
		return "Synchronizer (" + threadCount + " of " + numberOfEventingSystems + " waiting)";
	}

}

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

	public static final Event SYNC_EVENT = new Event() {
		public String toString() {
			return "SYNC_EVENT for the Synchronizer";
		}
	};

	private int numberOfEventingSystems;
	private long interval;
	private int threadCount;

	public Synchronizer(int numberOfEventingSystems, long interval) {
		this.numberOfEventingSystems = numberOfEventingSystems;
		this.interval = interval;
	}

	@Override
	public void process(Event e, EventingSystem eventingSystem) {
		process(e, eventingSystem, false);
	}

	public void process(Event e, EventingSystem eventingSystem, boolean verbose) {
		synchronized (this) {
			if (verbose) {
				System.out.println(this + ": receveived " + e + " from " + eventingSystem);
			}
			threadCount++;
			if (threadCount == numberOfEventingSystems) {
				threadCount = 0;
				if (verbose) {
					System.out.println(this + ": =====================================");
				}
				notifyAll();
			} else {
				try {
					wait();
				} catch (InterruptedException e1) {
					return;
				}
			}
			if (verbose) {
				System.out.println(this + ": released " + eventingSystem);
			}
		}
		eventingSystem.scheduleEventRelative(this, e, interval);
	}

	@Override
	public String toString() {
		return "Synchronizer (" + threadCount + " of " + numberOfEventingSystems + " waiting)";
	}

}

package com.github.glfrazier.event.util;

import com.github.glfrazier.event.Event;
import com.github.glfrazier.event.EventProcessor;
import com.github.glfrazier.event.EventingSystem;

/**
 * The first time the process() method is invoked, the watchdog is started.
 * Thereafter, if more than the interval seconds pass between invocations of
 * process(), the watchdog prints out the stack trace for the thread that first
 * invoked process().
 *
 */
public class WatchDog extends Thread implements EventProcessor {

	private long sleepInterval;
	private long alertInterval;
	private boolean running = false;
	private boolean toggle = false;
	private Thread threadToMonitor;
	private EventingSystem eventingSystem;

	public WatchDog(long sleepInterval, long alertInterval) {
		super("watchdog");
		setDaemon(true);
		this.sleepInterval = sleepInterval;
		this.alertInterval = alertInterval;
	}

	private synchronized void touched() {
		toggle = true;
	}

	private synchronized boolean wasTouched() {
		boolean result = toggle;
		toggle = false;
		return result;
	}

	@Override
	public void process(Event e, EventingSystem eventingSystem, long deliveryTime) {
		touched();
		if (!running) {
			threadToMonitor = Thread.currentThread();
			this.eventingSystem = eventingSystem;
			this.start();
		}
		eventingSystem.scheduleEventRelative(this, e, sleepInterval);
	}

	public void run() {
		running = true;
		while (true) {
			try {
				Thread.sleep(alertInterval);
			} catch (InterruptedException e) {
				running = false;
				return;
			}
			if (!wasTouched()) {
				StackTraceElement[] trace = threadToMonitor.getStackTrace();
				System.err.println("Watchdog alerting on thread " + threadToMonitor + " at time " + eventingSystem.getCurrentTime() + ":");
				if (trace.length == 0) {
					System.err.println(">> The thread appears to have terminated; there is no stack trace.");
					return;
				} else {
					for (StackTraceElement t : trace) {
						System.err.println("  >>  " + t);
					}
				}
			}
		}
	}

}

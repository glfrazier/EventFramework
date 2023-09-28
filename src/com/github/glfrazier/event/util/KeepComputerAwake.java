package com.github.glfrazier.event.util;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;

import com.github.glfrazier.event.Event;
import com.github.glfrazier.event.EventProcessor;
import com.github.glfrazier.event.EventingSystem;

/**
 * This class prevents the computer from going to sleep while the EventingSystem
 * is running. See {@link #keepComputerAwake(EventingSystem)}
 * 
 * @author Greg Frazier
 *
 */
public class KeepComputerAwake implements EventProcessor {

	private static final KeepComputerAwake SINGLETON = new KeepComputerAwake();

	private KeepComputerAwake() {

	}

	/**
	 * This method schedules the KeepComputerAwake event processor to run every five
	 * minutes. The processor toggles the NumLock key, which in turn prevents the
	 * computer from sleeping. This assumes that the power manager requires more
	 * than five minutes of idle time before putting the computer to sleep.
	 * 
	 * @param es
	 */
	public static void keepComputerAwake(EventingSystem es) {
		es.scheduleEvent(SINGLETON, Event.EVENT);
	}

	@Override
	public void process(Event e, EventingSystem eventingSystem, long deliveryTime) {
		boolean flag = Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_NUM_LOCK);
		Toolkit.getDefaultToolkit().setLockingKeyState(KeyEvent.VK_NUM_LOCK, !flag);
		Thread.yield();
		Toolkit.getDefaultToolkit().setLockingKeyState(KeyEvent.VK_NUM_LOCK, !flag);
		eventingSystem.scheduleEventRelative(this, e, 5 * 60 * 1000l);
	}

	public String toString() {
		return "Keep Computer Awake";
	}

	public static void main(String[] args) {
		EventingSystem es = new EventingSystem(EventingSystem.REALTIME);
		keepComputerAwake(es);
		es.run();
	}

}

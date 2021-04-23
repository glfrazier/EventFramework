package com.github.glfrazier.event;

/**
 * Implemented by objects that process events.
 * 
 * @author Greg Frazier
 *
 */
public interface EventProcessor {

	/**
	 * Process the event.
	 * 
	 * @param e              the event to process
	 * @param eventingSystem the eventing system that delivered the event
	 */
	public void process(Event e, EventingSystem eventingSystem);

}

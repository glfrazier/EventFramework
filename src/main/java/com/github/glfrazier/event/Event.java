package com.github.glfrazier.event;

/**
 * A tagging interface for objects that are passed to
 * {@link EventProcessor#process(Event, EventingSystem)}.
 * 
 * @author Greg Frazier
 * 
 */
public interface Event {
	
	/** A generic event, for ease of use. */
	public static final Event EVENT = new Event() {};

}

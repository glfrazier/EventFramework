package com.github.glfrazier.event.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.github.glfrazier.event.Event;
import com.github.glfrazier.event.EventProcessor;
import com.github.glfrazier.event.EventingSystem;

/**
 * Implements a simple pub-sub model for the eventing system. The basis for
 * publishing and subscription is the class of the event. Note that inheritance
 * (instanceof) is <emph>NOT</emph> honored; one must explicitly
 * subscribe/unsubscribe to/from the class of interest. Events are published by
 * sending the event to the depot in the same manner that one would send an
 * event to any event processor, e.g.:
 * 
 * <pre>
 * PubSubDepot depot = eventingSystem.getPubSubDepot();
 * eventingSystem.scheduleEvent(depot, event);
 * </pre>
 * 
 * Event processors that wish to subscribe (or unsubscribe) to events of
 * particular classes use the {@link #subscribe(EventProcessor, Class)} (or
 * {@link #unsubscribe(EventProcessor, Class)}) method, e.g.:
 * 
 * <pre>
 * PubSubDepot depot = eventingSystem.getPubSubDepot();
 * depot.subscribe(eventProcessorObject, EventOfInterest.class);
 * </pre>
 * 
 * @author Greg Frazier
 *
 */
public class PubSubDepot implements EventProcessor {

	private Map<Class<? extends Event>, Set<EventProcessor>> subscriptions;

	public PubSubDepot() {
		subscriptions = new HashMap<>();
	}

	/**
	 * The event is delivered to all event processors that have subscribed to the
	 * event class. Note that inheritance (instanceof) is <emph>NOT</emph> honored;
	 * one must explicitly subscribe to the class of interest.
	 */
	@Override
	public void process(Event e, EventingSystem eventingSystem, long currentTime) {
		Class<? extends Event> ec = e.getClass();
		Set<EventProcessor> targets = new HashSet<>();
		synchronized (this) {
			Set<EventProcessor> set = subscriptions.get(ec);
			if (set == null) {
				return;
			}
			targets.addAll(set);
		}
		for (EventProcessor target : targets) {
			target.process(e, eventingSystem, currentTime);
		}
	}

	/**
	 * Subscribe to a class of events. Every event of that class that is delivered
	 * to this depot will then be delivered to the subscriber. Note that inheritance
	 * (instanceof) is <emph>NOT</emph> honored; one must explicitly subscribe to
	 * the class of interest.
	 * 
	 * @param subscriber the event processor that will have events delivered to it
	 * @param eventClass the event class that the subscriber is subscribing to
	 */
	public synchronized void subscribe(EventProcessor subscriber, Class<? extends Event> eventClass) {
		Set<EventProcessor> targets = subscriptions.get(eventClass);
		if (eventClass == null) {
			targets = new HashSet<>();
			subscriptions.put(eventClass, targets);
		}
		targets.add(subscriber);
	}

	/**
	 * Unsubscribe from a class of events. The depot will cease to deliver events of
	 * the specified class to the subscriber.Note that inheritance (instanceof) is
	 * <emph>NOT</emph> honored; one must explicitly unsubscribe to individual
	 * classes.
	 * 
	 * @param subscriber the event processor that will cease to have events of the
	 *                   specified class delivered to it
	 * @param eventClass the event class that is being unsubscribed from
	 * @return <code>true</code> if the subscriber was subscribed to the event class
	 *         (and so was successfully removed as a subscriber); <code>false</code>
	 *         if the specified event processor was not a subscriber.
	 * @see Set#remove(Object)
	 */
	public synchronized boolean unsubscribe(EventProcessor subscriber, Class<? extends Event> eventClass) {
		Set<EventProcessor> targets = subscriptions.get(eventClass);
		if (eventClass == null) {
			return false;
		}
		return targets.remove(subscriber);
	}

}

package glf.event;

/**
 * Implemented by objects that process events.
 * 
 * @author glfrazier
 *
 */
public interface EventProcessor {

	public void process(Event e, EventingSystem eventingSystem);
	
}

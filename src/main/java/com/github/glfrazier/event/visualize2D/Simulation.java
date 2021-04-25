package com.github.glfrazier.event.visualize2D;

import java.awt.Graphics;

import javax.swing.JPanel;

import com.github.glfrazier.event.EventingSystem;

public interface Simulation {
	
	public EventingSystem getEventingSystem();

	/**
	 * Sets the simulation to its initial state.
	 */
	public void initialize();

	/**
	 * Starts the simulation running from its current state. If the simulation is
	 * already running, has no effect.
	 */
	public void run();

	/**
	 * Stops the simulation, but otherwise does not modify it. The simulation will
	 * resume execution from the stopped point in time if the {link #run()} method
	 * is invoked.
	 */
	public void stop();
	
	public void paint(Graphics g, JPanel panel);

	public void setGUI(SimulationGUI simulationGUI); 
}

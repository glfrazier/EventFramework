package com.github.glfrazier.event.monitor;

public enum Command {

	EXIT("x", "<x> (exit) exits the monitor"),
	DISPATCHED_EVENTS("de",
			"<de> (dispatched events) returns the total number of events dispatched by the eventing system"),
	EVENTQUEUE_LENGTH("ql", "<ql> (queue length) returns the current length of the event queue"),
	MAX_EVENTQUEUE_LENGTH("mql", "<mql> (maximum queue length) returns the maximum length of the event queue"),
	TERMINATE("terminate",
			"<terminate> (terminate) terminates the run() method of the eventing system, discarding all queued events"),
	HELP("?", "<?>/<h> (help) print usage information for each command"),
	HELP2("h", "<?>/<h> (help) print usage information for each command");

	private String cmd;
	private String usage;

	Command(String cmd, String usage) {
		this.cmd = cmd;
		this.usage = usage;
	}

	public String getCmd() {
		return cmd;
	}

	public boolean isCommand(String c) {
		return cmd.equalsIgnoreCase(c);
	}

	public String usage() {
		return usage;
	}
}

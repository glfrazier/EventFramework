package com.github.glfrazier.event.monitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.github.glfrazier.event.EventingSystem;

/**
 * A thread that reads {@link Command}s from System.in and executes those
 * commands against the EventingSystem. These can either be queries (e.g.,
 * obtaining progress information) or directives (e.g., shut down).
 * 
 * @author Greg Frazier
 *
 */
public class EventingSystemMonitor implements Runnable {

	public static final String EXITING = "EXITING";
	public static final String EVENTS_DISPATCHED = "ed";
//	public static final String 

	protected EventingSystem es;
	protected BufferedReader in;
	protected PrintStream out;
	private boolean running;

	public EventingSystemMonitor(EventingSystem es, InputStream in, PrintStream out) {
		this.es = es;
		this.in = new BufferedReader(new InputStreamReader(in));
		this.out = out;
	}

	public EventingSystemMonitor(EventingSystem es, InputStream in, OutputStream out) {
		this(es, in, new PrintStream(out));
	}

	public static Thread getTerminalMonitor(EventingSystem es) {
		EventingSystemMonitor monitor = new EventingSystemMonitor(es, System.in, System.out);
		Thread t = new Thread(monitor);
		t.setDaemon(true);
		return t;
	}

	public static Thread getSocketMonitor(EventingSystem es, int listenPort) throws IOException {
		ServerSocket ssocket = new ServerSocket(listenPort);
		Thread t = new Thread("Socket Monitor") {

			public void run() {
				while (!es.isTerminated()) {
					try {
						Socket socket = ssocket.accept();

						EventingSystemMonitor monitor = new EventingSystemMonitor(es, socket.getInputStream(),
								socket.getOutputStream());
						Thread t = new Thread(monitor);
						t.setDaemon(true);
						t.start();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				try {
					ssocket.close();
				} catch (IOException e) {
					// Do nothing about it
				}
			}
		};
		t.setDaemon(true);
		return t;
	}

	@Override
	public void run() {
		running = true;
		String input = null;
		while (running) {
			try {
				out.print(es.toString() + " > ");
				out.flush();
				input = in.readLine();
				if (input == null)
					break;
				execute(input);
			} catch (IOException e) {
				break;
			} catch (IllegalArgumentException e) {
				out.println("Unknown command or illegal arguments: <" + input + ">");
			}
		}
		out.println(EXITING);
		return;
	}

	protected void execute(String cmdline) throws IllegalArgumentException {
		if (cmdline == null || cmdline.trim().equals("")) {
			return;
		}
		String[] tokens = cmdline.trim().split("\\s+");
		String[] args = new String[tokens.length - 1];
		for (int i = 0; i < args.length; i++) {
			args[i] = tokens[i + 1];
		}
		Command cmd = null;
		for (Command c : Command.values()) {
			if (c.isCommand(tokens[0])) {
				cmd = c;
				break;
			}
		}
		if (cmd == null) {
			throw new IllegalArgumentException();
		}
		switch (cmd) {
		case DISPATCHED_EVENTS:
			out.println(es.getTotalEventsDelivered());
			break;
		case EVENTQUEUE_LENGTH:
			out.println(es.getQueueLength());
			break;
		case EXIT:
			out.println("Exiting the monitor.");
			running = false;
			break;
		case TERMINATE:
			out.println("Terminating the Eventing System");
			es.terminate();
			break;
		case MAX_EVENTQUEUE_LENGTH:
			out.println(es.getMaxQueueLength());
			break;
		case HELP:
		case HELP2:
			usage();
			break;
		}
		return;
	}

	private void usage() {
		out.println("The command parser for the " + es + " eventing system. Commands:");
		for (Command cmd : Command.values()) {
			out.println("\t" + cmd.usage());
		}
	}

	public static interface Handler {

		public void perform(Command c, String[] args);

	}

}

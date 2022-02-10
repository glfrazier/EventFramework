package com.github.glfrazier.test_event;

import static com.github.glfrazier.event.EventingSystem.NOT_REALTIME;

import java.util.ArrayList;
import java.util.List;

import com.github.glfrazier.event.Event;
import com.github.glfrazier.event.EventingSystem;
import com.github.glfrazier.event.util.Synchronizer;

public class TestSynchronizer {

	public static void main(String[] args) throws Exception {
		int numThreads = 5;
		List<Thread> threads = new ArrayList<>();
		Synchronizer sync = new Synchronizer(numThreads, 100) // synchronize two eventing systems every 100 ms
		{
			public void process(Event e, EventingSystem es) {
				super.process(e, es, true);
			}
		};
		for (int i = 0; i < numThreads; i++) {
			EventingSystem es = new EventingSystem("ES " + i, NOT_REALTIME);
//		    es.setVerbose(true);
			es.scheduleEventRelative(sync, Synchronizer.SYNC_EVENT, 100);
			es.setEndTime(10000);
			Thread t = new Thread(es);
			threads.add(t);
			t.start();
		}
		for (Thread t : threads) {
			t.join();
		}
		System.out.println("Test completed.");
	}
}

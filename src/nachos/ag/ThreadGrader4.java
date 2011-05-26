package nachos.ag;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.threads.KThread;
import nachos.threads.RoundRobinScheduler;
import nachos.threads.ThreadedKernel;

/**
 * <li> ThreadGrader4: <b>WaitUntil</b><br>
 * <ol type=a>
 * <li>Test ThreadGrader4.a: Tests waitUntil to ensure it waits at least
 * minimum amount of time
 * <li>Test ThreadGrader4.b: Tests whether waitUntil actually wakes up at
 * correct time
 * </ol>
 * </li>
 * 
 * @author Isaac
 * 
 */
public class ThreadGrader4 extends BasicTestGrader {
	static int count = 0;
	static int total = 0;

	public void run() {
		assertTrue(ThreadedKernel.scheduler instanceof RoundRobinScheduler,
				"this test requires roundrobin scheduler");

		/*
		 * Test ThreadGrader4.a: Tests waitUntil to ensure it waits at least
		 * minimum amount of time
		 */
		count = 0;
		total = 1;
		forkNewThread(new PingTest(500));
		while (count != total) {
			assertTrue(Machine.timer().getTime() < 2500,
					"Too many ticks wasted on \nTest ThreadGrader4.a");
			KThread.yield();
		}

		/*
		 * Test ThreadGrader4.b: Tests whether waitUntil actually wakes up at
		 * correct time
		 */
		count = 0;
		total = 100;
		for (int i = 0; i < 100; ++i)
			forkNewThread(new PingTest(Lib.random(1000)));
		while (count != total) {
			System.out.println(Machine.timer().getTime());
			assertTrue(Machine.timer().getTime() < 80000,
					"Too many ticks wasted on \nTest ThreadGrader4.b");
			KThread.yield();
		}
		done();
	}

	private class PingTest implements Runnable {
		private long wakeTick = 0;

		PingTest(int time) {
			wakeTick = Machine.timer().getTime() + time;
			ThreadedKernel.alarm.waitUntil(time);
		}

		public void run() {
			assertTrue(Machine.timer().getTime() > wakeTick,
					"wake up at a wrong time");
			++count;
		}
	}
}

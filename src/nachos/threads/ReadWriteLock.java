package nachos.threads;

import java.util.ArrayList;

import nachos.machine.Lib;
import nachos.machine.Machine;

/**
 * A <tt>ReadWriteLock</tt> is a synchronization primitive that used
 * for File in RealFileSystem.
 */
public class ReadWriteLock {
	/**
	 * Allocate a new ReadWriteLock. The lock will initially be <i>free</i>.
	 */
	public ReadWriteLock() {
	}

	/**
	 * Atomically acquire this lock. The current thread must not already hold
	 * either the read lock or the write lock
	 */
	public void acquireWrite() {
		Lib.assertTrue(!readHeldByCurrentThread() && !writeHeldByCurrentThread());

		boolean intStatus = Machine.interrupt().disable();
		KThread thread = KThread.currentThread();

		// held by a writer or multiple readers
		if (writeHolder != null || readHolder.size() > 0) {
			writeWaitQueue.waitForAccess(thread);
			++writeWaiting;
			KThread.sleep();
		}
		else {
			writeWaitQueue.acquire(thread);
			writeHolder = thread;
		}

		Lib.assertTrue(writeHolder == thread);

		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Atomically release this lock, allowing other threads to acquire it.
	 */
	public void releaseWrite() {
		Lib.assertTrue(writeHeldByCurrentThread());

		boolean intStatus = Machine.interrupt().disable();

		if ((writeHolder = writeWaitQueue.nextThread()) != null) {
			--writeWaiting;
			writeHolder.ready();
		}
		else {
			KThread reader = null;
			while ((reader = readWaitQueue.nextThread()) != null) {
				reader.ready();
				readHolder.add(reader);
			}
		}

		Machine.interrupt().restore(intStatus);

	}

	public void acquireRead() {
		Lib.assertTrue(!readHeldByCurrentThread() && !writeHeldByCurrentThread());

		boolean intStatus = Machine.interrupt().disable();
		KThread thread = KThread.currentThread();

		// held by a writer or multiple readers
		if (writeHolder != null || writeWaiting > 0) {
			readWaitQueue.waitForAccess(thread);
			KThread.sleep();
		}
		else {
			readWaitQueue.acquire(thread);
			readHolder.add(thread);
		}

		Lib.assertTrue(readHolder.contains(thread));

		Machine.interrupt().restore(intStatus);
	}

	public void releaseRead() {
		Lib.assertTrue(readHeldByCurrentThread());

		boolean intStatus = Machine.interrupt().disable();

		readHolder.remove(KThread.currentThread());
		if (readHolder.isEmpty()) {
			if ((writeHolder = writeWaitQueue.nextThread()) != null) {
				--writeWaiting;
				writeHolder.ready();
			}
		}

		Machine.interrupt().restore(intStatus);

	}

	/**
	 * Test if the current thread holds this lock.
	 * 
	 * @return true if the current thread holds this lock.
	 */
	public boolean writeHeldByCurrentThread() {
		return writeHolder == KThread.currentThread();
	}
	
	public boolean readHeldByCurrentThread() {
		return readHolder.contains(KThread.currentThread());
	}

	private ArrayList<KThread> readHolder = new ArrayList<KThread>();
	private KThread writeHolder = null;
	private ThreadQueue readWaitQueue = ThreadedKernel.scheduler
			.newThreadQueue(true);
	private ThreadQueue writeWaitQueue = ThreadedKernel.scheduler
			.newThreadQueue(true);
	int writeWaiting = 0;
}

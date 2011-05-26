package nachos.threads;

import java.util.LinkedList;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {
	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {
		lock = new Lock();
		Speaker = new LinkedList<ThreadInfo>();
		Listener = new LinkedList<ThreadInfo>();
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 * 
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 * 
	 * @param word
	 *            the integer to transfer.
	 */
	public void speak(int word) {

		lock.acquire();

		if (!Listener.isEmpty()) {
			ThreadInfo listen = Listener.removeFirst();
			listen.setWord(word);
			listen.getCondition().wake();
		}
		else {
			ThreadInfo speaker = new ThreadInfo();
			speaker.setWord(word);
			Speaker.add(speaker);
			speaker.getCondition().sleep();
		}
		lock.release();
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() {

		lock.acquire();

		int word = 0;

		if (!Speaker.isEmpty()) {
			ThreadInfo speaker = Speaker.removeFirst();
			word = speaker.getWord();
			speaker.getCondition().wake();
		}
		else {
			ThreadInfo listener = new ThreadInfo();
			Listener.add(listener);
			listener.getCondition().sleep();
			word = listener.getWord();
		}
		lock.release();
		return word;
	}

	/**
	 * @author mark A common lock to assure the atomity of <tt>listen</tt> and
	 *         <tt>speak</tt>
	 */
	private static Lock lock;

	private class ThreadInfo {
		int word;
		Condition condition;

		public ThreadInfo() {
			word = 0;
			condition = new Condition(lock);
		}

		public Condition getCondition() {
			return condition;
		}

		public int getWord() {
			return word;
		}

		public void setWord(int w) {
			this.word = w;
		}
	}

	private LinkedList<ThreadInfo> Speaker;
	private LinkedList<ThreadInfo> Listener;
}

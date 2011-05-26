package nachos.ag;

import nachos.threads.Boat;
import nachos.threads.KThread;
import nachos.machine.Lib;

/**
 * Boat Grader
 * 
 * @author crowwork
 */
public class BoatGrader extends BasicTestGrader {

	/**
	 * BoatGrader consists of functions to be called to show that your solution
	 * is properly synchronized. This version simply prints messages to standard
	 * out, so that you can watch it. You cannot submit this file, as we will be
	 * using our own version of it during grading.
	 * 
	 * Note that this file includes all possible variants of how someone can get
	 * from one island to another. Inclusion in this class does not imply that
	 * any of the indicated actions are a good idea or even allowed.
	 */
	void run() {

		final int adults = getIntegerArgument("adults");
		final int children = getIntegerArgument("children");
		Lib.assertTrue(adults >= 0 && children >= 0,
				"number can not be negative");

		 this.startTest(adults, children);
		done();
	}

	public void startTest(int adults, int children) {
		this.adultsOahu = adults;
		this.childrenOahu = children;
		this.adultsMolokai = this.childrenMolokai = 0;

		Boat.begin(this.adultsOahu, childrenOahu, this);
	}

	protected int adultsOahu, childrenOahu;
	protected int adultsMolokai, childrenMolokai;

	/**
	 */
	protected void check(boolean value, String msg) {
		Lib.assertTrue(value, msg);
	}

	/**
	 * all the passenger has been crossed
	 */
	private void AllCrossed() {
		check(adultsOahu == 0, "there are still " + adultsOahu
				+ " adults in Oahu");
		check(childrenOahu == 0, "there are still " + childrenOahu
				+ " children in Oahu");
	}

	private void doYield() {
		while (random.nextBoolean())
			KThread.yield();
	}

	/*
	 * ChildRowToMolokai should be called when a child pilots the boat from Oahu
	 * to Molokai
	 */
	public void ChildRowToMolokai() {
		doYield();
		check(childrenOahu > 0,
				"no children in Oahu,invalid operation ChildRowToMolokai");
		childrenOahu--;
		childrenMolokai++;
		// System.out.println("**Child rowing to Molokai.");
	}

	/*
	 * ChildRowToOahu should be called when a child pilots the boat from Molokai
	 * to Oahu
	 */
	public void ChildRowToOahu() {
		doYield();
		check(childrenMolokai > 0,
				"no children in Oahu , invalid operation ChildRowToOahu");
		childrenOahu++;
		childrenMolokai--;
		// System.out.println("**Child rowing to Oahu.");
	}

	/*
	 * ChildRideToMolokai should be called when a child not piloting the boat
	 * disembarks on Molokai
	 */
	public void ChildRideToMolokai() {
		doYield();
		check(childrenOahu > 0,
				"no children in Molokai , invalid operation ChildRideToMolokai");
		childrenOahu--;
		childrenMolokai++;
		// System.out.println("**Child arrived on Molokai as a passenger.");
	}

	/*
	 * ChildRideToOahu should be called when a child not piloting the boat
	 * disembarks on Oahu
	 */
	public void ChildRideToOahu() {
		doYield();
		check(childrenMolokai > 0,
				"no children in Molokai, invalid operation ChildRideToOahu");
		childrenOahu++;
		childrenMolokai--;
		// System.out.println("**Child arrived on Oahu as a passenger.");
	}

	/*
	 * AdultRowToMolokai should be called when a adult pilots the boat from Oahu
	 * to Molokai
	 */
	public void AdultRowToMolokai() {
		doYield();
		check(adultsOahu > 0,
				" no adult in Oahu , invalid operation AdultRowToMolokai");
		adultsOahu--;
		adultsMolokai++;
		// System.out.println("**Adult rowing to Molokai.");
	}

	/*
	 * AdultRowToOahu should be called when a adult pilots the boat from Molokai
	 * to Oahu
	 */
	public void AdultRowToOahu() {
		doYield();
		check(adultsMolokai > 0,
				"no adult in Molokai , invalid operation AdultRowToOahu");
		adultsOahu++;
		adultsMolokai--;
		// System.out.println("**Adult rowing to Oahu.");
	}

	/*
	 * AdultRideToMolokai should be called when an adult not piloting the boat
	 * disembarks on Molokai
	 */
	public void AdultRideToMolokai() {
		Lib.assertNotReached("invalid operation AdultRideToMolokai");
		// System.out.println("**Adult arrived on Molokai as a passenger.");
	}

	@Override
	public void readyThread(KThread thread) {
		if (thread==idleThread) {
			++idleReadyCount;
			if (idleReadyCount > 1000)
				AllCrossed();
		}
		else
			idleReadyCount=0;
	}

	/*
	 * AdultRideToOahu should be called when an adult not piloting the boat
	 * disembarks on Oahu
	 */
	public void AdultRideToOahu() {
		Lib.assertNotReached("invalid operation AdultRideToOahu");
		// System.out.println("**Adult arrived on Oahu as a passenger.");
	}

	@Override
	public void setIdleThread(KThread thread){
		thread=idleThread;
	}

	KThread idleThread;
	java.util.Random random = new java.util.Random();
	private static int idleReadyCount = 0;
}

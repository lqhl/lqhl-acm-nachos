package nachos.vm;

import java.util.Hashtable;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.TranslationEntry;
import nachos.userprog.UserKernel;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
	/**
	 * Allocate a new VM kernel.
	 */
	public VMKernel() {
		super();
	}

	/**
	 * Initialize this kernel.
	 */
	public void initialize(String[] args) {
		super.initialize(args);

		coreMap = new Page[Machine.processor().getNumPhysPages()];
		tlbManager = new TLBManager();
		memoryManager = new ClockPagingManager(); // new SecondChanceMemoryManager();
	}

	/**
	 * Test this kernel.
	 */
	public void selfTest() {
		super.selfTest();
	}

	/**
	 * Start running user programs.
	 */
	public void run() {
		super.run();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		Lib.debug(dbgVM, "VM terminate");

		getSwapper().close();
		
		Lib.debug(dbgVM, "Paging: page faults " + VMProcess.numPageFaults);
		
		super.terminate();
	}

	public static TranslationEntry getPageEntry(PageItem item) {
		Integer ppn = invertedPageTable.get(item);
		if (ppn == null)
			return null;
		Page result = coreMap[ppn];
		if (result == null || !result.entry.valid)
			return null;
		return result.entry;
	}
	
	public static SwapFile getSwapper() {
		if (swapFile == null)
			swapFile = new SwapFile();
		return swapFile;
	}

	private static final char dbgVM = 'v';

	protected static Hashtable<PageItem, Integer> invertedPageTable = new Hashtable<PageItem, Integer>();
	protected static Page[] coreMap;
	protected static TLBManager tlbManager;
	protected static MemoryManager memoryManager;
	protected static SwapFile swapFile;
}

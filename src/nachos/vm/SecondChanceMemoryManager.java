package nachos.vm;

import java.util.LinkedList;

import nachos.machine.Machine;
import nachos.machine.TranslationEntry;

public class SecondChanceMemoryManager extends MemoryManager {
	public SecondChanceMemoryManager() {
		for (int i = 0; i < Machine.processor().getNumPhysPages(); i++)
			freePages.add(i);
	}

	protected void removePage(int ppn) {
		usedQueue.remove(new Integer(ppn));
		freePages.add(ppn);
	}

	protected int nextPage() {
		if (!freePages.isEmpty())
			return freePages.removeFirst();

		int ppn = usedQueue.removeFirst();
		Page page = VMKernel.coreMap[ppn];
		while (page.entry.used) {
			page.entry.used = false;
			usedQueue.add(ppn);
			ppn = usedQueue.removeFirst();
			page = VMKernel.coreMap[ppn];
		}
		return ppn;
	}

	public TranslationEntry swapIn(PageItem item, LazyLoader lazyLoader) {
		VMKernel.tlbManager.flush();
		int ppn = nextPage();

		swapOut(ppn);

		TranslationEntry entry = lazyLoader.load(item, ppn);

		usedQueue.add(ppn);
		VMKernel.invertedPageTable.put(item, ppn);
		VMKernel.coreMap[ppn] = new Page(item, entry);
		return entry;
	}

	protected int seekInTLB(int vpn) {
		for (int i = 0; i < Machine.processor().getTLBSize(); i++) {
			TranslationEntry entry = Machine.processor().readTLBEntry(i);
			if (entry.valid && entry.vpn == vpn)
				return i;
		}
		return -1;
	}

	private LinkedList<Integer> usedQueue = new LinkedList<Integer>();
	private LinkedList<Integer> freePages = new LinkedList<Integer>();
}

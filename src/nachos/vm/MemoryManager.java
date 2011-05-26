package nachos.vm;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.Processor;
import nachos.machine.TranslationEntry;

public abstract class MemoryManager {

	public MemoryManager() {
	}

	protected abstract void removePage(int ppn);

	protected abstract int nextPage();

	public abstract TranslationEntry swapIn(PageItem item, LazyLoader lazyLoader);

	protected abstract int seekInTLB(int vpn);

	protected void swapOut(int ppn) {
		Page page = VMKernel.coreMap[ppn];
		if (page != null && page.entry.valid) { // swap out
			page.entry.valid = false;
			VMKernel.invertedPageTable.remove(page.item);
			int index = seekInTLB(page.entry.vpn);
			if (index != -1)
				VMKernel.tlbManager.invalid(index);
			if (page.entry.dirty) {
				SwapPage swapPage = VMKernel.getSwapper().newSwapPage(page);
				Lib.assertTrue(VMKernel.getSwapper().write(swapPage.frameNo,
						Machine.processor().getMemory(), Processor.makeAddress(
								ppn, 0)), "swap file write error");
			}
		}
	}
}

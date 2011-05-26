package nachos.vm;

import nachos.machine.TranslationEntry;

public class SwapPage extends Page {
	int frameNo;

	public SwapPage(PageItem item, TranslationEntry entry, int frameNo) {
		super(item, entry);
		this.frameNo = frameNo;
	}

}

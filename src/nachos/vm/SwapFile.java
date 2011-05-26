package nachos.vm;

import java.util.Hashtable;
import java.util.LinkedList;

import nachos.machine.OpenFile;
import nachos.machine.Processor;
import nachos.threads.ThreadedKernel;

public class SwapFile {
	public SwapFile() {
		size = 0;
		file = ThreadedKernel.fileSystem.open(swapFileName, true);
		byte[] buf = new byte[Processor.pageSize * numSwapPage];
		file.write(buf, 0, buf.length);
	}

	public void close() {
		file.close();
		ThreadedKernel.fileSystem.remove(swapFileName);
	}

	public boolean write(int frameNo, byte[] data, int offset) {
		return file.write(frameNo * Processor.pageSize, data, offset,
				Processor.pageSize) == Processor.pageSize;
	}

	public boolean read(int frameNo, byte[] data, int offset) {
		return file.read(frameNo * Processor.pageSize, data, offset,
				Processor.pageSize) == Processor.pageSize;
	}

	public int newFrameNo() {
		if (freeFrames.isEmpty())
			return size++;
		Integer result = freeFrames.removeFirst();
		return result;
	}

	public SwapPage newSwapPage(Page page) {
		SwapPage swapPage = swapPageTable.get(page.item);
		if (swapPage == null) {
			swapPage = new SwapPage(page.item, page.entry, newFrameNo());
			swapPageTable.put(page.item, swapPage);
		}
		return swapPage;
	}

	public SwapPage getSwapPage(PageItem pageItem) {
		return swapPageTable.get(pageItem);
	}

	public boolean deleteSwapPage(PageItem pageItem) {
		SwapPage swapPage = getSwapPage(pageItem);
		if (swapPage == null)
			return false;
		freeFrames.add(swapPage.frameNo);
		return true;
	}
	
	public OpenFile	getSwapFile() {
		return file;
	}

	public static final int numSwapPage = 32;
	public static final String swapFileName = "SWAP";

	private int size;
	private OpenFile file;
	private LinkedList<Integer> freeFrames = new LinkedList<Integer>();
	private Hashtable<PageItem, SwapPage> swapPageTable = new Hashtable<PageItem, SwapPage>();
}

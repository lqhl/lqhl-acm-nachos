package nachos.vm;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.TranslationEntry;

public class TLBManager {
	public void clear() {
		for (int i = 0; i < Machine.processor().getTLBSize(); i++) {
			TLB2PageTable(i);
			invalid(i);
		}
	}

	public void addEntry(TranslationEntry entry) {
		int index = -1;

		for (int i = 0; i < Machine.processor().getTLBSize(); i++)
			if (!Machine.processor().readTLBEntry(i).valid) {
				index = i;
				break;
			}

		if (index == -1)
			index = Lib.random(Machine.processor().getTLBSize());

		TLB2PageTable(index);

		Machine.processor().writeTLBEntry(index, entry);
	}

	public void flush() {
		for (int i = 0; i < Machine.processor().getTLBSize(); i++)
			TLB2PageTable(i);
	}

	public void TLB2PageTable(int index) {
		TranslationEntry entry = Machine.processor().readTLBEntry(index);
		if (entry.valid)
			VMKernel.coreMap[entry.ppn].entry = entry;
	}

	public void invalid(int index) {
		TranslationEntry entry = Machine.processor().readTLBEntry(index);
		if (!entry.valid)
			return;
		entry.valid = false;
		Machine.processor().writeTLBEntry(index, entry);
	}

	public TranslationEntry find(int vpn, boolean isWrite) {
		for (int i = 0; i < Machine.processor().getTLBSize(); i++) {
			TranslationEntry entry = Machine.processor().readTLBEntry(i);
			if (entry.valid && entry.vpn == vpn) {
				if (entry.readOnly && isWrite)
					return null;
				entry.dirty = entry.dirty || isWrite;
				entry.used = true;
				Machine.processor().writeTLBEntry(i, entry);
				return entry;
			}
		}
		return null;
	}
}

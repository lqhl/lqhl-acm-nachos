package nachos.filesys;

import nachos.machine.Disk;
import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.OpenFile;

/**
 * File provide some basic IO operations. Each File is associated with an INode
 * which stores the basic information for the file.
 * 
 * @author starforever
 */
public class File extends OpenFile {
	INode inode;

	private int pos;

	public File(INode inode) {
		this.inode = inode;
		pos = 0;
	}
	
	public int length() {
		return inode.file_size;
	}

	public void close() {
		if (inode == null)
			return;
		inode.use_count--;
		inode.tryFree();
		inode = null;
	}

	public void seek(int pos) {
		this.pos = pos;
	}

	public int tell() {
		return pos;
	}

	public int read(byte[] buffer, int offset, int length) {
		int ret = read(pos, buffer, offset, length);
		pos += ret;
		return ret;
	}

	public int write(byte[] buffer, int offset, int length) {
		int ret = write(pos, buffer, offset, length);
		pos += ret;
		return ret;
	}

	public int read(int pos, byte[] buffer, int offset, int length) {
		acquireRead();
		if (pos >= length()) {
			releaseRead();
			return -1;
		}
		length = Math.min(length() - pos, length);
		int firstSector = sectorFromPos(pos);
		int firstOffset = offsetFromPos(pos);
		int lastSector = sectorFromPos(pos + length);
		int amount = Math.min(Disk.SectorSize - firstOffset, length);
		byte[] data = new byte[Disk.SectorSize];
		Machine.synchDisk().readSector(inode.getSectorAddr(firstSector), data, 0);
		System.arraycopy(data, firstOffset, buffer, offset, amount);
		offset += amount;
		for (int i = firstSector + 1; i <= lastSector; i++) {
			Integer addr = inode.getSectorAddr(i);
			if (addr == null) {
				releaseRead();
				return amount;
			}
			int len = Math.min(length - amount, Disk.SectorSize);
			Machine.synchDisk().readSector(addr, data, 0);
			System.arraycopy(data, 0, buffer, offset, len);
			amount += len;
			offset += len;
		}
		releaseRead();
		return amount;
	}

	public int write(int pos, byte[] buffer, int offset, int length) {
		acquireWrite();
		if (pos + length >= length())
			inode.setFileSize(pos + length);
		int firstSector = sectorFromPos(pos);
		int firstOffset = offsetFromPos(pos);
		int lastSector = sectorFromPos(pos + length);
		int amount = Math.min(Disk.SectorSize - firstOffset, length);
		byte[] data = new byte[Disk.SectorSize];
		Machine.synchDisk().readSector(inode.getSectorAddr(firstSector), data, 0);
		System.arraycopy(buffer, offset, data, firstOffset, amount);
		Machine.synchDisk().writeSector(inode.getSectorAddr(firstSector), data, 0);
		offset += amount;
		for (int i = firstSector + 1; i <= lastSector; i++) {
			Integer addr = inode.getSectorAddr(i);
			if (addr == null) {
				releaseWrite();
				return amount;
			}
			int len = Math.min(length - amount, Disk.SectorSize);
			Machine.synchDisk().readSector(addr, data, 0);
			System.arraycopy(buffer, offset, data, 0, len);
			Machine.synchDisk().writeSector(addr, data, 0);
			amount += len;
			offset += len;
		}
		releaseWrite();
		return amount;
	}
	
	private static int sectorFromPos(int pos) {
		return pos / Disk.SectorSize;
	}

	private static int offsetFromPos(int pos) {
		return pos % Disk.SectorSize;
	}

	public String readString(int pos, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = read(pos, bytes, 0, maxLength + 1);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}
	
	public boolean writeString(int pos, String st) {
		byte[] bytes = new byte[st.length() + 1];
		System.arraycopy(st.getBytes(), 0, bytes, 0, st.length());
		bytes[st.length()] = 0;
		return write(pos, bytes, 0, bytes.length) == bytes.length;
	}

	protected void acquireRead() {
		if (!inode.readWriteLock.readHeldByCurrentThread()
				&& !inode.readWriteLock.writeHeldByCurrentThread())
			inode.readWriteLock.acquireRead();
	}

	protected void releaseRead() {
		if (inode.readWriteLock.readHeldByCurrentThread())
			inode.readWriteLock.releaseRead();
	}

	protected void acquireWrite() {
		if (!inode.readWriteLock.readHeldByCurrentThread()
				&& !inode.readWriteLock.writeHeldByCurrentThread())
			inode.readWriteLock.acquireWrite();
	}

	protected void releaseWrite() {
		if (inode.readWriteLock.writeHeldByCurrentThread())
			inode.readWriteLock.releaseWrite();
	}
}

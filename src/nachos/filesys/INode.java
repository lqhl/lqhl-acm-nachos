package nachos.filesys;

import java.util.LinkedList;

import nachos.machine.Disk;
import nachos.machine.Machine;
import nachos.threads.ReadWriteLock;

/**
 * INode contains detail information about a file. Most important among these is
 * the list of sector numbers the file occupied, it's necessary to find all the
 * pieces of the file in the filesystem.
 * 
 * @author starforever
 */
public class INode {
	/** represent a system file (free list) */
	public static final int TYPE_SYSTEM = 0;

	/** represent a folder */
	public static final int TYPE_FOLDER = 1;

	/** represent a normal file */
	public static final int TYPE_FILE = 2;

	/** represent a normal file that is marked as delete */
	public static final int TYPE_FILE_DEL = 3;

	/** represent a symbolic link file */
	public static final int TYPE_SYMLINK = 4;

	/** represent a folder that are not valid */
	public static final int TYPE_FOLDER_DEL = 5;

	ReadWriteLock readWriteLock = new ReadWriteLock();

	/** size of the file in bytes */
	int file_size;

	/** the type of the file */
	int file_type;
	
	/** the number of programs that have access on the file */
	int use_count;
	
	/** the number of links on the file */
	int link_count;

	/** maintain all the sector numbers this file used in order */
	private LinkedList<Integer> sec_addr;

	/** the first address */
	private int addr;

	/** the extended address */
	private LinkedList<Integer> addr_ext;

	public INode(int addr) {
		file_size = 0;
		file_type = TYPE_FILE;
		link_count = 0;
		sec_addr = new LinkedList<Integer>();
		this.addr = addr;
		addr_ext = new LinkedList<Integer>();
	}

	/** get the sector number of a position in the file */
	public Integer getSectorAddr(int i) {
		if (i >= sec_addr.size())
			return null;
		return sec_addr.get(i);
	}

	/** change the file size and adjust the content in the inode accordingly */
	public void setFileSize(int size) {
		if (file_size == size)
			return;
		file_size = size;
		int sectorNum = file_size / Disk.SectorSize
				+ (file_size % Disk.SectorSize != 0 ? 1 : 0);
		if (sectorNum == sec_addr.size())
			return;
		if (sectorNum > sec_addr.size())
			for (int i = sec_addr.size(); i < sectorNum; i++)
				sec_addr.add(FilesysKernel.realFileSystem.getFreeList()
						.allocate());
		else if (sectorNum < sec_addr.size())
			for (int i = sectorNum; i < sec_addr.size(); i++)
				FilesysKernel.realFileSystem.getFreeList().deallocate(
						sec_addr.remove(i));
	}

	/** free the disk space occupied by the file (including inode) */
	public void free() {
		file_size = 0;
		for (Integer secAddr : sec_addr)
			FilesysKernel.realFileSystem.getFreeList().deallocate(secAddr);
		sec_addr.clear();
		for (Integer addrExt : addr_ext)
			FilesysKernel.realFileSystem.getFreeList().deallocate(addrExt);
		addr_ext.clear();
		FilesysKernel.realFileSystem.getFreeList().deallocate(addr);
		RealFileSystem.removeInode(addr);
	}

	/** load inode content from the disk */
	public void load() {
		int curSec = 0;
		byte[] buffer = new byte[Disk.SectorSize];
		Machine.synchDisk().readSector(addr, buffer, 0);
		int pos = 0;
		// load reserve info
		file_size = Disk.intInt(buffer, pos);
		pos += 4;
		file_type = Disk.intInt(buffer, pos);
		pos += 4;
		link_count = Disk.intInt(buffer, pos);
		pos += 4;
		int addr_ext_size = Disk.intInt(buffer, pos);
		pos += 4;
		int sec_addr_size = Disk.intInt(buffer, pos);
		pos += 4;
		// load addr_ext
		for (int i = 0; i < addr_ext_size; i++, pos += 4) {
			if (pos == Disk.SectorSize) {
				pos = 0;
				Machine.synchDisk().readSector(addr_ext.get(curSec++), buffer,
						0);
			}
			addr_ext.add(Disk.intInt(buffer, pos));
		}
		// load sec_addr
		for (int i = 0; i < sec_addr_size; i++, pos += 4) {
			if (pos == Disk.SectorSize) {
				pos = 0;
				Machine.synchDisk().readSector(addr_ext.get(curSec++), buffer,
						0);
			}
			sec_addr.add(Disk.intInt(buffer, pos));
		}
	}

	/** save inode content to the disk */
	public void save() {
		if (addr == -1)
			return;
		int curSec = 0;
		int curAddr = addr;
		int len = (5 + addr_ext.size() + sec_addr.size()) * 4;
		while (len > (addr_ext.size() + 1) * Disk.SectorSize) {
			len += 4;
			addr_ext.add(FilesysKernel.realFileSystem.getFreeList().allocate());
		}
		byte[] buffer = new byte[Disk.SectorSize];
		int pos = 0;
		// save reserve info
		Disk.extInt(file_size, buffer, pos);
		pos += 4;
		Disk.extInt(file_type, buffer, pos);
		pos += 4;
		Disk.extInt(link_count, buffer, pos);
		pos += 4;
		Disk.extInt(addr_ext.size(), buffer, pos);
		pos += 4;
		Disk.extInt(sec_addr.size(), buffer, pos);
		pos += 4;
		// save addr_ext
		for (int i = 0; i < addr_ext.size(); i++, pos += 4) {
			if (pos == Disk.SectorSize) {
				pos = 0;
				Machine.synchDisk().writeSector(curAddr, buffer, 0);
				curAddr = addr_ext.get(curSec++);
			}
			Disk.extInt(addr_ext.get(i), buffer, pos);
		}
		// save sec_addr
		for (int i = 0; i < sec_addr.size(); i++, pos += 4) {
			if (pos == Disk.SectorSize) {
				pos = 0;
				Machine.synchDisk().writeSector(curAddr, buffer, 0);
				curAddr = addr_ext.get(curSec++);
			}
			Disk.extInt(sec_addr.get(i), buffer, pos);
		}
		Machine.synchDisk().writeSector(curAddr, buffer, 0);
	}

	public int sectorNum() {
		return sec_addr.size();
	}

	public void tryFree() {
		if (use_count == 0 && link_count == 0)
			free();
	}
}

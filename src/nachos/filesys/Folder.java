package nachos.filesys;

import java.util.Hashtable;

import nachos.machine.Disk;
import nachos.machine.Lib;

/**
 * Folder is a special type of file used to implement hierarchical filesystem.
 * It maintains a map from filename to the address of the file. There's a
 * special folder called root folder with pre-defined address. It's the origin
 * from where you traverse the entire filesystem.
 * 
 * @author starforever
 */
public class Folder extends File {
	/** the static address for root folder */
	public static int STATIC_ADDR = 1;

	private int size;

	/** mapping from filename to folder entry */
	private Hashtable<String, FolderEntry> entrySet;

	public Folder(INode inode) {
		super(inode);
		inode.file_type = INode.TYPE_FOLDER;
		size = 4;
		entrySet = new Hashtable<String, FolderEntry>();
	}

	/** open a file in the folder and return its address */
	public int open(String filename) {
		FolderEntry folderEntry = entrySet.get(filename);
		if (folderEntry == null)
			return -1;
		else
			return folderEntry.addr;
	}

	/** create a new file in the folder and return its address */
	public int create(String filename) {
		int addr = FilesysKernel.realFileSystem.getFreeList().allocate();
		INode inode = RealFileSystem.getInode(addr);
		inode.file_type = INode.TYPE_FILE;
		addEntry(filename, addr);
		return addr;
	}

	/** add an entry with specific filename and address to the folder */
	public void addEntry(String filename, int addr) {
		entrySet.put(filename, new FolderEntry(filename, addr));
		INode inode = RealFileSystem.getInode(addr);
		inode.link_count++;
		save();
	}

	/** remove an entry from the folder */
	public void removeEntry(String filename) {
		entrySet.remove(filename);
	}
	
	public boolean contains(String filename) {
		return entrySet.containsKey(filename);
	}
	
	public int getEntry(String filename) {
		FolderEntry entry = entrySet.get(filename);
		if (entry == null)
			return -1;
		return entry.addr;
	}

	/** save the content of the folder to the disk */
	public void save() {
		/*
		int pos = 0;
		byte[] buffer = new byte[4];
		Disk.extInt(entrySet.size(), buffer, 0);
		Lib.assertTrue(write(pos, buffer, 0, 4) == 4);
		pos += 4;
		for (FolderEntry folderEntry : entrySet.values()) {
			Lib.assertTrue(writeString(pos, folderEntry.name));
			pos += folderEntry.name.length() + 1;
			Disk.extInt(folderEntry.addr, buffer, 0);
			Lib.assertTrue(write(pos, buffer, 0, 4) == 4);
			pos += 4;
		}
		size = pos;
		inode.setFileSize(size);
		*/
		acquireWrite();
		size = 4;
		for (FolderEntry folderEntry : entrySet.values())
			size += folderEntry.name.length() + 1 + 4;
		byte[] buffer = new byte[size];
		int pos = 0;
		Disk.extInt(entrySet.size(), buffer, pos);
		pos += 4;
		for (FolderEntry folderEntry : entrySet.values()) {
			string2Buffer(folderEntry.name, buffer, pos);
			pos += folderEntry.name.length() + 1;
			Disk.extInt(folderEntry.addr, buffer, pos);
			pos += 4;
		}
		Lib.assertTrue(size == pos);
		write(0, buffer, 0, buffer.length);
		inode.setFileSize(size);
		inode.save();
		releaseWrite();
	}

	private void string2Buffer(String name, byte[] buffer, int pos) {
		System.arraycopy(name.getBytes(), 0, buffer, pos, name.length());
		buffer[pos + name.length()] = 0;
	}

	/** load the content of the folder from the disk */
	public void load() {
		/*
		int pos = 0;
		byte[] buffer = new byte[4];
		Lib.assertTrue(read(pos, buffer, 0, 4) == 4);
		pos += 4;
		int num = Disk.intInt(buffer, 0);
		for (int i = 0; i < num; i++) {
			String filename = readString(pos, RealFileSystem.maxNameLength);
			Lib.assertTrue(filename != null);
			pos += filename.length() + 1;
			Lib.assertTrue(read(pos, buffer, 0, 4) == 4);
			pos += 4;
			int addr = Disk.intInt(buffer, 0);
			entrySet.put(filename, new FolderEntry(filename, addr));
		}
		size = pos;
		Lib.assertTrue(size == length());
		*/
		acquireRead();
		int pos = 0;
		byte[] buffer = new byte[length()];
		Lib.assertTrue(read(pos, buffer, 0, buffer.length) == buffer.length);
		pos += 4;
		int num = Disk.intInt(buffer, 0);
		for (int i = 0; i < num; i++) {
			String filename = bytes2String(buffer, pos, RealFileSystem.maxNameLength);
			Lib.assertTrue(filename != null);
			pos += filename.length() + 1;
			int addr = Disk.intInt(buffer, pos);
			pos += 4;
			entrySet.put(filename, new FolderEntry(filename, addr));
		}
		Lib.assertTrue(pos == length());
		releaseRead();
	}

	private String bytes2String(byte[] buffer, int pos, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		for (int length = pos; length < Math.min(pos + maxLength + 1, buffer.length); length++) {
			if (buffer[length] == 0)
				return new String(buffer, pos, length - pos);
		}

		return null;
	}

	public boolean isEmpty() {
		return entrySet.isEmpty();
	}
	
	public String[] contents() {
		String[] result = new String[entrySet.size()];
		int i = 0;
		for (String st : entrySet.keySet())
			result[i++] = st;
		return result;
	}
}

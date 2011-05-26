package nachos.filesys;

import java.util.HashMap;

import nachos.machine.Lib;

public class FileStat {
	public static final int FILE_NAME_MAX_LEN = 60;
	public static final int NORMAL_FILE_TYPE = 0;
	public static final int DIR_FILE_TYPE = 1;
	public static final int LinkFileType = 2;
	public static final HashMap<Integer, Integer> TypeMap = new HashMap<Integer, Integer>();
	{
		TypeMap.put(INode.TYPE_FILE, NORMAL_FILE_TYPE);
		TypeMap.put(INode.TYPE_FOLDER, DIR_FILE_TYPE);
		TypeMap.put(INode.TYPE_SYMLINK, LinkFileType);
	}

	public String name;
	public int size;
	public int sectors;
	public int type;
	public int inode;
	public int links;
	
	public static final int STAT_SIZE = FILE_NAME_MAX_LEN + 4 * 5;

	public byte[] getBytes() {
		byte[] res = new byte[STAT_SIZE];
		int len = Math.min(FILE_NAME_MAX_LEN - 1, name.length());
		System.arraycopy(name.getBytes(), 0, res, 0, len);
		res[len] = 0;
		int pos = FILE_NAME_MAX_LEN;
		Lib.bytesFromInt(res, pos, size);
		pos += 4;
		Lib.bytesFromInt(res, pos, sectors);
		pos += 4;
		Lib.bytesFromInt(res, pos, type);
		pos += 4;
		Lib.bytesFromInt(res, pos, inode);
		pos += 4;
		Lib.bytesFromInt(res, pos, links);
		pos += 4;
		return res;
	}
}

package nachos.filesys;

import java.util.Hashtable;
import java.util.LinkedList;

import nachos.machine.FileSystem;
import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.OpenFile;
import nachos.vm.VMKernel;

/**
 * RealFileSystem provide necessary methods for filesystem syscall. The
 * FileSystem interface already define two basic methods, you should implement
 * your own to adapt to your task.
 * 
 * @author starforever
 */
public class RealFileSystem implements FileSystem {
	/** the free list */
	private static FreeList free_list;

	/** the root folder */
	private Folder root_folder;

	/** the string representation of the current folder */
	private LinkedList<String> cur_path = new LinkedList<String>();
	
	private LinkedList<Folder> curPath = new LinkedList<Folder>();
	
	
	private static String path_seperator = "/";
	private static String curDir = ".";
	private static String parentDir = "..";
	
	public static final int maxNameLength = 256;
	private static final int maxPathLength = maxNameLength * 4;
	
	private static Hashtable<Integer, INode> inodeTable = new Hashtable<Integer, INode>();
	
	public static INode getInode(int addr) {
		INode inode = inodeTable.get(addr);
		if (inode == null) {
			inode = new INode(addr);
			if (free_list.isUsed(addr))
				inode.load();
			inodeTable.put(addr, inode);
		}
		return inode;
	}

	public static void removeInode(int addr) {
		inodeTable.remove(addr);
	}

	/**
	 * initialize the file system
	 * 
	 * @param format
	 *            whether to format the file system
	 */
	public void init(boolean format) {
		if (format) {
			INode inode_free_list = new INode(FreeList.STATIC_ADDR);
			inodeTable.put(FreeList.STATIC_ADDR, inode_free_list);
			free_list = new FreeList(inode_free_list);
			free_list.init();
			
			INode inode_root_folder = new INode(Folder.STATIC_ADDR);
			inodeTable.put(Folder.STATIC_ADDR, inode_root_folder);
			root_folder = new Folder(inode_root_folder);
			root_folder.save();
			inode_root_folder.save();

			curPath.add(root_folder);
			
			importStub();
			
			Lib.debug(FilesysKernel.DEBUG_FLAG, "finish import stub file system");
		}
		else {
			INode inode_free_list = new INode(FreeList.STATIC_ADDR);
			inode_free_list.load();
			inodeTable.put(FreeList.STATIC_ADDR, inode_free_list);
			free_list = new FreeList(inode_free_list);
			free_list.load();

			INode inode_root_folder = new INode(Folder.STATIC_ADDR);
			inode_root_folder.load();
			inodeTable.put(Folder.STATIC_ADDR, inode_root_folder);
			root_folder = new Folder(inode_root_folder);
			root_folder.load();
			
			curPath.add(root_folder);
		}
		free_list.calcUsed();
	}

	public void finish() {
		VMKernel.getSwapper().close();
		free_list.save();
		for (INode inode : inodeTable.values())
			inode.save();
	}

	/** import from stub filesystem */
	private void importStub() {
		FileSystem stubFS = Machine.stubFileSystem();
		FileSystem realFS = FilesysKernel.realFileSystem;
		String[] file_list = Machine.stubFileList();
		for (int i = 0; i < file_list.length; ++i) {
			if (!file_list[i].endsWith(".coff"))
				continue;
			OpenFile src = stubFS.open(file_list[i], false);
			if (src == null) {
				continue;
			}
			OpenFile dst = realFS.open(file_list[i], true);
			int size = src.length();
			byte[] buffer = new byte[size];
			src.read(0, buffer, 0, size);
			dst.write(0, buffer, 0, size);
			src.close();
			dst.close();
			Lib.debug(FilesysKernel.DEBUG_FLAG, "copy " + file_list[i] + " to file system");
		}
	}

	/** get the only free list of the file system */
	public FreeList getFreeList() {
		return free_list;
	}

	/** get the only root folder of the file system */
	public Folder getRootFolder() {
		return root_folder;
	}

	class PathResult {
		boolean success = false;
		int addr = -1;
		Folder cur = null;
		String filename = null;
		LinkedList<String> _path = null;
		LinkedList<Folder> path = null;
	}
	
	private PathResult getPath(String name) {
		PathResult result = new PathResult();
		LinkedList<String> _path = new LinkedList<String>();
		LinkedList<Folder> path = new LinkedList<Folder>();
		
		while (name.length() > 1 && name.endsWith(path_seperator))
			name = name.substring(0, name.length() - 1);
		
		if (name.startsWith(path_seperator))
			path.add(root_folder);
		else {
			_path.addAll(cur_path);
			path.addAll(curPath);
		}
		
		if (name.equals(path_seperator)) {
			result.success = true;
			result.cur = null;
			result._path = _path;
			result.path = path;
			result.path.removeLast();
			result.addr = Folder.STATIC_ADDR;
			return result;
		}
		
		String[] dirName = name.split(path_seperator);
		String filename = dirName[dirName.length - 1];
		for (String st : dirName) {
			if (st.isEmpty() || st.equals(curDir) && st != filename)
				continue;
			if (st.equals(parentDir)) {
				path.removeLast();
				if (_path.isEmpty())
					return result;
				_path.removeLast();
				if (path.isEmpty())
					return result;
				if (st != filename)
					continue;
			}
			
			Folder cur = path.getLast();
			cur.load();
			if (st == filename) { // finally find it..
				result.success = true;
				result.cur = cur;
				result.filename = filename;
				result._path = _path;
				result.path = path;
				if (st.equals(curDir) || st.equals(parentDir)) {
					if (result.path.size() > 1) {
						result.cur = path.get(path.size() - 2);
						result.filename = _path.getLast();
						result.addr = result.cur.getEntry(result.filename);
						return result;
					}
					else if (result.path.size() == 1) {
						result.addr = Folder.STATIC_ADDR;
						result.cur = null;
						result.filename = null;
						result.path.clear();
						result._path.clear();
						return result;
					}
					else {
						result.success = false;
						return result;
					}
				}
				else if (cur.contains(st)) {
					result.addr = cur.getEntry(filename);
					return result;
				}
				else
					return result;
			}
			
			if (cur.contains(st)) {
				int inodeAddr = cur.getEntry(st);
				INode inode = getInode(inodeAddr);
				if (inode.file_type == INode.TYPE_FOLDER) {
					_path.add(st);
					Folder next = new Folder(inode);
					path.add(next);
				}
				else
					return result;
			}
			else
				return result;
		}
		
		Lib.assertNotReached();
		return result;
	}
	
	public OpenFile open(String name, boolean create) {
		PathResult result = getPath(name);
		if (result.success) {
			if (result.addr >= 0) {
				INode inode = getInode(result.addr);
				if (inode.file_type == INode.TYPE_FILE) {
					inode.use_count++;
					return new File(inode);
				}
				else if (inode.file_type == INode.TYPE_SYMLINK)
					return loadSymLink(inode);
			}
			else if (create) {
				INode inode = getInode(result.cur.create(result.filename));
				inode.use_count++;
				return new File(inode);
			}
		}
		
		return null;
	}

	private OpenFile loadSymLink(INode inode) {
		File file = new File(inode);
		String name = file.readString(0, maxPathLength);
		Lib.assertTrue(name != null);
		return openSym(name);
	}
	
	public OpenFile openSym(String name) {
		PathResult result = getPath(name);
		if (result.success)
			if (result.addr >= 0) {
				INode inode = getInode(result.addr);
				if (inode.file_type == INode.TYPE_FILE) {
					inode.use_count++;
					return new File(inode);
				}
			}
		
		return null;
	}

	public boolean remove(String name) {
		PathResult result = getPath(name);
		if (result.success && result.addr != -1) {
			INode inode = getInode(result.addr);
			if (inode.file_type == INode.TYPE_FILE || inode.file_type == INode.TYPE_SYMLINK) {
				result.cur.removeEntry(result.filename);
				result.cur.save();
				inode.link_count--;
				if (inode.link_count == 0)
					inode.file_type = INode.TYPE_FILE_DEL;
				inode.tryFree();
				return true;
			}
		}
		return false;
	}

	public boolean createFolder(String name) {
		PathResult result = getPath(name);
		if (!result.success || result.addr != -1)
			return false;
		int addr = free_list.allocate();
		INode inode = getInode(addr);
		Folder newFolder = new Folder(inode);
		newFolder.save();
		result.cur.addEntry(result.filename, addr);
		return true;
	}

	public boolean removeFolder(String name) {
		PathResult result = getPath(name);
		if (!result.success || result.addr == -1)
			return false;
		if (result.addr == Folder.STATIC_ADDR)
			return false;
		INode inode = getInode(result.addr);
		if (inode.file_type == INode.TYPE_FOLDER) {
			Folder folder = new Folder(inode);
			folder.load();
			if (folder.isEmpty()) {
				result.cur.removeEntry(result.filename);
				result.cur.save();
				inode.link_count--;
				if (inode == curPath.getLast().inode) {
					curPath.removeLast();
					cur_path.removeLast();
				}
				inode.tryFree();
				return true;
			}
			else
				return false;
		}
		return false;
	}

	public boolean changeCurFolder(String name) {
		PathResult result = getPath(name);
		if (!result.success || result.addr == -1)
			return false;
		INode inode = getInode(result.addr);
		if (inode.file_type == INode.TYPE_FOLDER) {
			Folder folder = new Folder(inode);
			folder.load();
			curPath = result.path;
			curPath.add(folder);
			cur_path = result._path;
			if (result.filename != null)
				cur_path.add(result.filename);
			return true;
		}
		return false;
	}

	public String[] readDir(String name) {
		PathResult result = getPath(name);
		if (!result.success || result.addr == -1)
			return null;
		INode inode = getInode(result.addr);
		if (inode.file_type == INode.TYPE_FOLDER) {
			Folder folder = new Folder(inode);
			folder.load();
			return folder.contents();
		}
		return null;
	}

	public FileStat getStat(String name) {
		PathResult result = getPath(name);
		if (!result.success || result.addr == -1)
			return null;
		INode inode = getInode(result.addr);
		if (inode.file_type == INode.TYPE_FILE_DEL || inode.file_type == INode.TYPE_FOLDER_DEL)
			return null;
		FileStat stat = new FileStat();
		stat.name = result.filename;
		if (stat.name == null)
			stat.name = "/";
		stat.size = inode.file_size;
		stat.sectors = inode.sectorNum();
		stat.inode = result.addr;
		stat.links = inode.link_count;
		stat.type = FileStat.TypeMap.get(inode.file_type);
		return stat;
	}

	public boolean createLink(String src, String dst) {
		PathResult resSrc = getPath(src);
		if (!resSrc.success || resSrc.addr == -1)
			return false;
		PathResult resDst = getPath(dst);
		if (!resDst.success || resDst.addr != -1)
			return false;
		resDst.cur.addEntry(resDst.filename, resSrc.addr);
		return true;
	}

	public boolean createSymlink(String src, String dst) {
		PathResult resDst = getPath(dst);
		if (!resDst.success || resDst.addr != -1)
			return false;
		int addr = free_list.allocate();
		INode inode = getInode(addr);
		File file = new File(inode);
		inode.file_type = INode.TYPE_SYMLINK;
		file.writeString(0, src);
		resDst.cur.addEntry(resDst.filename, addr);
		return true;
	}

	public int getSwapFileSectors() {
		INode inode = ((File) VMKernel.getSwapper().getSwapFile()).inode;
		return inode.file_size;
	}

	public int getFreeSize() {
		return free_list.size();
	}
	
	public String getCurPath() {
		return path2String(cur_path);
	}

	private String path2String(LinkedList<String> path) {
		StringBuffer result = new StringBuffer();
		for (String st : path)
			result.append("/" + st);
		if (result.length() == 0)
			result.append("/");
		return result.toString();
	}
}

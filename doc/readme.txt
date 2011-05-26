nachos readme - 刘勤 5080309105
1、完成度
	完成了4个phase的所有内容：
		Phase 1: Build a thread system
		Phase 2: Multiprogramming
		Phase 3: Caching and Virtual Memory
		Phase 5: File System
2、filesys相关事项：
	a. support absolute and relative path
	b. support "." and ".."
	c. support deletion of the current folder
	d. support exclusive write and concurrent read
		模仿Java中的ReadWriteLock实现的
	e. 工作目录只有一个
	f. 当前工作目录被删除会跳到上一级目录
	g. 不对symlink的目标进行检查，symlink指向文件夹是无效的
3、bonus
	见2.a, 2.b, 2.c, 2.d
4、其他
	希望能够在开始写每个phase的时候就能得到测试集，这样早点测试有利于发现错误。
	希望测试集不符合规范的数据能够放在一个地方标识清楚。
	deadline应该提前一周。
	
package nachos.ag;
import java.io.File;
import nachos.machine.*;
import nachos.threads.KThread;
import nachos.userprog.UserProcess;
/***
 * @author Tianqi Chen
 *	file system call auto grader for phase 2
 */
public class UserGrader1 extends CoffGrader {
	private static final String testfilenames[]={
		"f0","f1"
	};
	private static final int ActionFileExist 	= 8;
	private static final int ActionNumOpen 		= 9;
	private static final int ActionStartID 		= 10;
	private static final int ActionAssertID 	= 11;
	private static final int ActionFileCmp 		= 12;
	
	private String filename;
	private boolean fileexist;
	private nachos.threads.Lock 	fileExistLock; 
	
	public void run(){
		fileExistLock = new nachos.threads.Lock();
		for( int i = 0 ; i < testfilenames.length ; i ++ )
			fileRemove( testfilenames[i] );
		super.run();
	}
	
	/** whether the file exist */
	protected boolean fileExist( String name ){
		fileExistLock.acquire();
		this.filename = name;
		super.privilege.doPrivileged(new Runnable(){
			public void run(){
				File file = new File( testRoot,filename );
				fileexist = file.exists();
			}
		});
		boolean exist = fileexist;
		fileExistLock.release();
		return exist;
	}
	
	protected void fileRemove( String name ){
		fileExistLock.acquire();
		this.filename = name;
		super.privilege.doPrivileged(new Runnable(){
			public void run(){
				File file = new File( testRoot,filename );
				if( file.exists() ){
					Lib.assertTrue(file.delete(),"can't delete file "+ filename);
				}
			}
		});
		fileExistLock.release();
	}
	
	protected int handleTestSystemCall(int type, int a0, int a1, int a2){
		switch( type ){
			case ActionFileExist :	return fileExist( testfilenames[a0] ) ? 1:0;  
			case ActionNumOpen	 : 	return ((StubFileSystem)Machine.stubFileSystem()).getOpenCount();
			case ActionStartID	 :  System.out.println("start test #" + a0 ); break;
			case ActionAssertID  :
				System.out.println("\tassert %" + a0 + (a1 != 0 ? " true" : " false") );
				if( a1 == 0 ){ 
					System.out.println("Test failed");
					Machine.halt();
				}else break;
			default:	return super.handleTestSystemCall(type, a0, a1, a2);
		}
		return 0;
	}
	

}

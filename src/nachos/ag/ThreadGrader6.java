package nachos.ag;

import java.util.HashSet;
import java.util.Set;
import nachos.machine.Lib;
import nachos.threads.Lock;
import nachos.threads.PriorityScheduler;
import nachos.threads.ThreadedKernel;

/**
 * <li>ThreadGrader6: <b>More Priority Scheduling</b><br>
 * <ol type=a>
 * <li>Test ThreadGrader6.a: Tests priority donation
 * <li>Test ThreadGrader6.b: Tests priority donation with more locks and more
 * complicated resource allocation
 * </ol>
 * </li>
 * 
 * @author Isaac
 * 
 */
public class ThreadGrader6 extends BasicTestGrader
{
  static int total = 0;
  static int count = 0;
  Set<ThreadHandler> set = new HashSet<ThreadHandler>();
  Lock[] lock = null;
  static final int lockCount = 10;
  
  public void run ()
  {
    assertTrue(ThreadedKernel.scheduler instanceof PriorityScheduler,
      "this test requires priority scheduler");
    
    lock = new Lock[lockCount];
    for (int i = 0; i < lockCount; ++i)
      lock[i] = new Lock();
    
    /* Test ThreadGrader6.a: Tests priority donation */
    total = 200;
    count = 0;
    set.clear();
    for (int i = 0; i < total; ++i)
      set.add(forkNewThread(new a()));
    for (ThreadHandler t : set)
      t.thread.join();
    assertTrue(count == total,
      "not all threads finished in \nTest ThreadGrader6.a");
    
    /*
     * Test ThreadGrader6.b: Tests priority donation with more locks and more
     * complicated resource allocation
     */
    total = 200;
    count = 0;
    set.clear();
    for (int i = 0; i < total; ++i)
      set.add(forkNewThread(new a(), Lib
        .random(PriorityScheduler.priorityMaximum + 1)));
    for (ThreadHandler t : set)
      t.thread.join();
    assertTrue(count == total,
      "not all threads finished in \nTest ThreadGrader6.b");
    done();
  }
  
  private class a implements Runnable
  {
    int n = 0;
    
    public void run ()
    {
      n = Lib.random(lockCount);
      lock[n].acquire();
      lock[n].release();
      ++count;
    }
  }
}

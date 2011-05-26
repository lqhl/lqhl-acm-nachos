package nachos.ag;

import java.util.HashSet;
import java.util.Set;
import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.threads.KThread;
import nachos.threads.PriorityScheduler;
import nachos.threads.ThreadedKernel;

/**
 * <li>ThreadGrader5: <b>Priority Scheduling</b><br>
 * <ol type=a>
 * <li>Test ThreadGrader5.a: Tests priority scheduler without donation
 * <li>Test ThreadGrader5.b: Tests priority scheduler without donation, altering
 * priorities of threads after they've started running
 * </ol>
 * </li>
 * 
 * @author Isaac
 * 
 */
public class ThreadGrader5 extends BasicTestGrader
{
  static int total = 0;
  static int count = 0;
  Set<ThreadHandler> set = new HashSet<ThreadHandler>();
  
  public void run ()
  {
    assertTrue(ThreadedKernel.scheduler instanceof PriorityScheduler,
      "this test requires priority scheduler");
    
    /* Test ThreadGrader5.a: Tests priority scheduler without donation */
    total = 200;
    count = 0;
    set.clear();
    for (int i = 0; i < total; ++i)
      set.add(forkNewThread(new a()));
    for (ThreadHandler t : set)
      t.thread.join();
    assertTrue(count == total,
      "not all threads finished in \nTest ThreadGrader5.a");
    /*
     * Test ThreadGrader5.b: Tests priority scheduler without donation, altering
     * priorities of threads after they've started running
     */
    total = 200;
    count = 0;
    set.clear();
    boolean intStatus = Machine.interrupt().disable();
    for (int i = 0; i < total; ++i)
      set.add(forkNewThread(new a(), Lib
        .random(PriorityScheduler.priorityMaximum + 1)));
    Machine.interrupt().restore(intStatus);
    for (ThreadHandler t : set)
      t.thread.join();
    assertTrue(count == total,
      "not all threads finished \nTest ThreadGrader5.b");
    done();
  }
  
  private class a implements Runnable
  {
    public void run ()
    {
      KThread.yield();
      ++count;
    }
  }
}

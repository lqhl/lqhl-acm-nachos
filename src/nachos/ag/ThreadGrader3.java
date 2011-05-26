package nachos.ag;

import nachos.machine.Machine;
import nachos.threads.KThread;
import nachos.threads.RoundRobinScheduler;
import nachos.threads.ThreadedKernel;

/**
 * <li>ThreadGrader3: <b>Join</b><br>
 * <ol type=a>
 * <li>Test ThreadGrader3.a: Tries a join on thread x before x actually runs
 * <li>Test ThreadGrader3.b: Tries a join on thread x after x has completed
 * </ol>
 * </li>
 * 
 * @author Isaac
 * 
 */
public class ThreadGrader3 extends BasicTestGrader
{
  static StringBuffer buf = null;
  
  void run ()
  {
    assertTrue(ThreadedKernel.scheduler instanceof RoundRobinScheduler,
      "this test requires roundrobin scheduler");
    
    /* Test ThreadGrader3.a: Tries a join on thread x before x actually runs */
    buf = new StringBuffer();
    ThreadHandler t1 = forkNewThread(new PingTest(1));
    t1.thread.join();
    forkNewThread(new PingTest(0));
    while (buf.length() < 10)
    {
      assertTrue(Machine.timer().getTime() < 1500,
        "Too many ticks wasted on \nTest ThreadGrader3.a");
      KThread.yield();
    }
    assertTrue(buf.toString().equals("1111100000"),
      "sequence error in execution");
    
    /* Test ThreadGrader3.b: Tries a join on thread x after x has completed */
    buf = new StringBuffer();
    ThreadHandler t2 = forkNewThread(new PingTest(1));
    forkNewThread(new PingTest(0));
    t2.thread.join();
    while (buf.length() < 10)
    {
      assertTrue(Machine.timer().getTime() < 2000,
        "Too many ticks wasted on \nTest ThreadGrader3.b");
      System.out.println(buf.toString());
      KThread.yield();
    }
    assertTrue(buf.toString().equals("1010101010"),
      "sequence error in execution");
    done();
  }
  
  private static class PingTest implements Runnable
  {
    PingTest (int which)
    {
      this.which = which;
    }
    
    public void run ()
    {
      for (int i = 0; i < 5; i++)
      {
        buf.append(which);
        KThread.yield();
      }
    }
    
    private int which;
  }
}

package nachos.ag;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.threads.Condition2;
import nachos.threads.KThread;
import nachos.threads.Lock;
import nachos.threads.RoundRobinScheduler;
import nachos.threads.ThreadedKernel;

/**
 * <li>ThreadGrader1: <b>Condition2</b><br>
 * <ol type=a>
 * <li>Test ThreadGrader1.a: Tests your condition variables using a few threads
 * <li>Test ThreadGrader1.b: Tests your condition variables using many threads
 * </ol>
 * </li>
 * 
 * @author Isaac
 * 
 */
public class ThreadGrader1 extends BasicTestGrader
{
  
  static int turn = 0;
  static Lock lock = null;
  static Condition2[] cond = null;
  static int total = 0;
  static int totalMax = 100;
  static int count = 0;
  
  public void run ()
  {
    assertTrue(ThreadedKernel.scheduler instanceof RoundRobinScheduler,
      "this test requires roundrobin scheduler");
    
    lock = new Lock();
    cond = new Condition2[totalMax];
    for (int i = 0; i < totalMax; ++i)
    {
      cond[i] = new Condition2(lock);
    }
    
    /*
     * Test ThreadGrader1.a: Tests your condition variables using a few threads
     */
    total = 4;
    count = 0;
    turn = Lib.random(total);
    
    for (int i = 0; i < total; ++i)
      forkNewThread(new Printer(i));
    while (count != total)
    {
      assertTrue(Machine.timer().getTime() < 2000,
        "Too many ticks wasted on \nTest ThreadGrader1.a");
      KThread.yield();
    }
    
    /*
     * Test ThreadGrader1.b: Tests your condition variables using many threads
     */
    total = 100;
    count = 0;
    turn = Lib.random(total);
    
    for (int i = 0; i < total; ++i)
      forkNewThread(new Printer(i));
    while (count != total)
    {
      assertTrue(Machine.timer().getTime() < 20000,
        "Too many ticks wasted \nTest ThreadGrader1.b");
      KThread.yield();
    }
    
    done();
  }
  
  class Printer implements Runnable
  {
    private int n = 0;
    
    public Printer (int n)
    {
      this.n = n;
    }
    
    public void run ()
    {
      lock.acquire();
      while (turn != n)
      {
        cond[n].sleep();
      }
      // System.out.println(KThread.currentThread() + " print " + n);
      ++count;
      turn = (turn + 1) % total;
      cond[turn].wake();
      lock.release();
    }
  }
}

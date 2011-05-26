package nachos.ag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.threads.KThread;
import nachos.threads.PriorityScheduler;
import nachos.threads.ThreadedKernel;

/**
 * A naive grader for priority scheduling.
 * 
 * @author Xiangru Chen
 */
public class PriorityGrader extends BasicTestGrader
{
  
  Set<ThreadHandler> ready;
  Set<ThreadHandler> handlers;
  Map<ThreadHandler, Long> timeMap;
  
  @Override
  public void run ()
  {
    assertTrue(ThreadedKernel.scheduler instanceof PriorityScheduler,
      "This test need PriorityScheduler.");
    
    final long threads = getIntegerArgument("threads");
    final long times = getIntegerArgument("times");
    final long length = getIntegerArgument("length");
    
    assertTrue(threads > 0, "invalid argument: number of threads");
    assertTrue(times > 0, "invalid argument: times of test");
    
    for (int i = 0; i < times; ++i)
    {
      ready = new HashSet<ThreadHandler>();
      handlers = new HashSet<ThreadHandler>();
      timeMap = new HashMap<ThreadHandler, Long>();
      boolean intStatus = Machine.interrupt().disable();
      for (int j = 0; j < threads; ++j)
      {
        ThreadHandler handler = forkNewThread(new Runnable()
        {
          @Override
          public void run ()
          {
            for (int i = 0; i < length; ++i)
              KThread.yield();
          }
        });
        handler.thread.setName("testThread" + j);
        int p = Lib.random(PriorityScheduler.priorityMaximum);
        ThreadedKernel.scheduler.setPriority(handler.thread, p);
        // System.out.println(handler.thread.getName() + " " + p);
        handlers.add(handler);
      }
      Machine.interrupt().restore(intStatus);
      
      for (ThreadHandler handler : handlers)
      {
        handler.thread.join();
      }
      
      ready.clear();
      handlers.clear();
      timeMap.clear();
    }
    
    done();
  }
  
  @Override
  public void readyThread (KThread thread)
  {
    super.readyThread(thread);
    if (ready != null)
    {
      ThreadHandler handler = getThreadHandler(thread);
      ready.add(handler);
      timeMap.put(handler, Machine.timer().getTime());
    }
  }
  
  @Override
  public void runningThread (KThread thread)
  {
    if (ready != null)
    {
      assertTrue(ready.contains(getThreadHandler(thread)), thread.getName()
        + " is not in the ready queue");
    }
    super.runningThread(thread);
    if (ready != null)
    {
      ThreadHandler handler = getThreadHandler(thread);
      ready.remove(handler);
      int ep = ThreadedKernel.scheduler.getEffectivePriority(handler.thread);
      long time = timeMap.get(handler);
      for (ThreadHandler other : ready)
      {
        int oep = ThreadedKernel.scheduler.getEffectivePriority(other.thread);
        long otime = timeMap.get(other);
        assertTrue(ep >= oep, "Wrong scheduling! There is another thread ("
          + other.thread.getName() + ") with higher priority than "
          + handler.thread.getName() + " in the ready queue");
        if (ep == oep)
        {
          assertTrue(time <= otime,
            "Wrong scheduling! There is another thread ("
              + other.thread.getName() + ") has equal priority as "
              + handler.thread.getName() + " but came earlier.");
        }
      }
    }
  }
  
}

package nachos.ag;

import java.util.HashMap;
import java.util.Map;
import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.threads.KThread;
import nachos.threads.ThreadedKernel;

/**
 * Some utilty functions for TestGrader
 * 
 * @author Kang Zhang
 */
public abstract class BasicTestGrader extends AutoGrader
{
  
  Map<KThread, ThreadHandler> handlers = new HashMap<KThread, ThreadHandler>();
  ThreadHandler currentHandler = null;
  
  protected class ThreadHandler
  {
    KThread thread;
    public boolean finished = false;
    
    public ThreadHandler (KThread thread)
    {
      this.thread = thread;
      
      handlers.put(thread, this);
    }
  }
  
  protected ThreadHandler getThreadHandler (KThread thread)
  {
    ThreadHandler r = handlers.get(thread);
    
    if (r == null)
      r = new ThreadHandler(thread);
    return r;
  }
  
  @Override
  public void runningThread (KThread thread)
  {
    super.runningThread(thread);
    ThreadHandler handler = getThreadHandler(thread);
    currentHandler = handler;
  }
  
  @Override
  public void finishingCurrentThread ()
  {
    super.finishingCurrentThread();
    currentHandler.finished = true;
    handlers.remove(currentHandler.thread);
  }
  
  protected ThreadHandler forkNewThread (Runnable threadContent)
  {
    return forkNewThread(threadContent, 1);
  }
  
  protected ThreadHandler forkNewThread (Runnable threadContent, int priority)
  {
    KThread thread = new KThread(threadContent);
    ThreadHandler handler = getThreadHandler(thread);
    
    thread.setName("TestThread");
    
    boolean intStatus = Machine.interrupt().disable();
    ThreadedKernel.scheduler.setPriority(thread, priority);
    thread.fork();
    Machine.interrupt().restore(intStatus);
    
    return handler;
  }
  
  protected void assertTrue (boolean val, String errMsg)
  {
    Lib.assertTrue(val, errMsg);
  }
}

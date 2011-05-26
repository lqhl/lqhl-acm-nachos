package nachos.ag;

import nachos.threads.ThreadedKernel;

/**
 * A naive testgrader for join operation
 * 
 * @author Kang Zhang
 */
public class JoinGrader extends BasicTestGrader
{
  @Override
  void run ()
  {
    // check dependency
    assertTrue(ThreadedKernel.alarm != null, "This test require alarm.");
    
    final long waitTicks = getIntegerArgument("waitTicks");
    long times = getIntegerArgument("times");
    
    // check argument
    assertTrue(times > 0, "invalid numThreads argument");
    assertTrue(waitTicks > 0, "invalid waitTicks argument");
    
    for (int i = 0; i < times; i++)
    {
      
      ThreadHandler handler = forkNewThread(new Runnable()
      {
        public void run ()
        {
          ThreadedKernel.alarm.waitUntil(waitTicks);
        }
      });
      
      handler.thread.join();
      
      assertTrue(handler.finished,
        " join() returned but target thread is still running");
    }
    
    done();
  }
}

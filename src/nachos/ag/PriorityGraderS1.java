package nachos.ag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.LinkedList;
import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.threads.KThread;
import nachos.threads.PriorityScheduler;
import nachos.threads.ThreadedKernel;
import nachos.threads.Semaphore;
import nachos.threads.Lock;

/**
 * A grader for priority scheduling.
 * 
 * @author Sangxia Huang
 */
public class PriorityGraderS1 extends BasicTestGrader
{
  
  protected final static String thdPrefix = "testThread #";
  
  protected Set<ThreadHandler> ready;
  protected Set<ThreadHandler> handlers;
  protected Map<ThreadHandler, TreeNode> trees;
  
  Lock[] lockSet;
  ThreadHandler[] lockHolders;
  LinkedList<ThreadHandler>[] lockWaiters;
  int[] waitingForLockNo;
  Semaphore[] joinSemaphores;
  
  protected class TreeNode
  {
    protected LinkedList<TreeNode> children;
    protected TreeNode parent;
    protected int priority, currentDonate;
    protected long time;
    protected ThreadHandler thd;
    
    public TreeNode (int priority, TreeNode parent, ThreadHandler th)
    {
      children = new LinkedList<TreeNode>();
      this.parent = parent;
      this.priority = priority;
      currentDonate = 0;
      time = Machine.timer().getTime();
      thd = th;
      if (parent != null)
        parent.update();
    }
    
    public int getPriority ()
    {
      return Math.max(priority, currentDonate);
    }
    
    public void setPriority (int p)
    {
      priority = p;
      if (parent != null)
        parent.update();
    }
    
    public void addChild (TreeNode c)
    {
      children.add(c);
      c.parent = this;
      c.time = Machine.timer().getTime();
      if (c.getPriority() > currentDonate)
      {
        currentDonate = Math.max(currentDonate, c.getPriority());
        if (parent != null)
          parent.update();
      }
    }
    
    public long getTime ()
    {
      return time;
    }
    
    public void setTime (long t)
    {
      time = t;
    }
    
    public void update ()
    {
      int temp = 0;
      for (TreeNode child : children)
        temp = Math.max(temp, child.getPriority());
      currentDonate = temp;
      if (parent != null)
        parent.update();
    }
    
    public void releaseChild (TreeNode x)
    {
      Lib.assertTrue(parent == null);
      x.parent = null;
      children.remove(x);
      int temp = 0;
      for (TreeNode child : children)
        temp = Math.max(temp, child.getPriority());
      currentDonate = temp;
    }
    
    public void printChild (int layer)
    {
      for (int i = 0; i < layer; ++i)
        Lib.debug('Z', "\t");
      Lib.debug('Z', thd.thread.getName() + "(orignal=" + priority
        + ", donated=" + currentDonate + ", time=" + +getTime() + ")");
      for (TreeNode child : children)
        child.printChild(layer + 1);
    }
  }
  
  protected void acquireLock (int x)
  {
    assertTrue(0 <= x && x < lockSet.length, "acquire lock index invalid");
    privilege.interrupt.tick(false);
    if (lockHolders[x] != null)
    {
      assertTrue(!lockSet[x].isHeldByCurrentThread(), "acquiring lock #" + x
        + " which already holds");
      Lib.debug('Z', "lock holder of " + x + " is "
        + lockHolders[x].thread.getName());
      trees.get(lockHolders[x]).addChild(
        trees.get(getThreadHandler(KThread.currentThread())));
      lockWaiters[x].add(getThreadHandler(KThread.currentThread()));
      waitingForLockNo[getTestThreadId(KThread.currentThread().getName())] = x;
      lockSet[x].acquire();
    }
    else
    {
      Lib
        .debug('Z', KThread.currentThread().getName() + " acquired lock #" + x);
      lockHolders[x] = getThreadHandler(KThread.currentThread());
      lockSet[x].acquire();
    }
  }
  
  protected void releaseLock (int x)
  {
    assertTrue(0 <= x && x < lockSet.length, "release lock index invalid");
    assertTrue(lockSet[x].isHeldByCurrentThread(),
      "releasing an un-holding lock");
    lockHolders[x] = null;
    // ThreadHandler nextHolder=null;
    trees.get(getThreadHandler(KThread.currentThread())).printChild(1);
    for (ThreadHandler waiting : lockWaiters[x])
    {
      // if (nextHolder==null ||
      // (trees.get(waiting).getPriority()>trees.get(nextHolder).getPriority()
      // ||
      // (trees.get(waiting).getPriority()==trees.get(nextHolder).getPriority()
      // && trees.get(waiting).getTime()<trees.get(nextHolder).getTime())
      // ||
      // (trees.get(waiting).getPriority()==trees.get(nextHolder).getPriority()
      // && trees.get(waiting).getTime()==trees.get(nextHolder).getTime()
      // && waiting.thread.hashCode()>nextHolder.thread.hashCode()))
      // )
      // nextHolder=waiting;
      trees.get(getThreadHandler(KThread.currentThread())).releaseChild(
        trees.get(waiting));
    }
    // if (!lockWaiters[x].isEmpty())
    // assertTrue(nextHolder!=null, "nextholder?");
    // if (nextHolder!=null){
    // Lib.debug('Z', nextHolder.thread.getName() + " acquired lock #" + x);
    // assertTrue(lockWaiters[x].remove(nextHolder),
    // "not found in lockwaiters");
    // for (ThreadHandler waiting: lockWaiters[x]) {
    // long temp=trees.get(waiting).getTime();
    // trees.get(nextHolder)
    // .addChild(trees.get(waiting));
    // trees.get(waiting).setTime(temp);
    // }
    // lockHolders[x]=nextHolder;
    // }
    lockSet[x].release();
  }
  
  protected void runThread (int times, int locks)
  {
    int id = new Integer(KThread.currentThread().getName().substring(
      thdPrefix.length(), KThread.currentThread().getName().length()))
      .intValue();
    Lib.debug('Z', id + " " + KThread.currentThread().getName());
    int[] lockHolding = new int[locks + 1];
    int holdingCount = 1;
    lockHolding[0] = -1;
    for (int i = 0; i < times; ++i)
    {
      Lib.debug('Z', KThread.currentThread().getName() + ": " + i
        + "-th iteration.");
      int choice;
      if (holdingCount > 1)
        choice = Lib.random(3);
      else
        choice = Lib.random(2);
      switch (choice)
      {
        case 0:
          // set priority
          int p = getRandomPriority();
          Lib.debug('Z', Machine.timer().getTime() + ": "
            + KThread.currentThread().getName() + " setting priority to " + p);
          boolean status = Machine.interrupt().disable();
          ThreadedKernel.scheduler.setPriority(KThread.currentThread(), p);
          trees.get(getThreadHandler(KThread.currentThread())).setPriority(p);
          Machine.interrupt().restore(status);
          break;
        case 1:
          // try to acquire a lock
          if (lockHolding[holdingCount - 1] != lockSet.length - 1)
          {
            int index = Lib.random(lockSet.length
              - lockHolding[holdingCount - 1] - 1);
            lockHolding[holdingCount] = lockHolding[holdingCount - 1] + 1
              + index;
            Lib.debug('Z', Machine.timer().getTime()
              + ": "
              + KThread.currentThread().getName()
              + "(priority="
              + trees.get(getThreadHandler(KThread.currentThread()))
                .getPriority() + ")" + " acquiring lock #"
              + lockHolding[holdingCount]);
            acquireLock(lockHolding[holdingCount]);
            ++holdingCount;
            break;
          }
        case 2:
          // release a lock
          if (holdingCount > 1)
          {
            int index = Lib.random(holdingCount - 1) + 1;
            Lib
              .debug('Z', Machine.timer().getTime()
                + ": "
                + KThread.currentThread().getName()
                + "(priority="
                + trees.get(getThreadHandler(KThread.currentThread()))
                  .getPriority() + ")" + " releasing lock #"
                + lockHolding[index]);
            releaseLock(lockHolding[index]);
            System.arraycopy(lockHolding, index + 1, lockHolding, index,
              holdingCount - index - 1);
            --holdingCount;
          }
          break;
      }
      KThread.yield();
    }
    for (int i = 1; i < holdingCount; ++i)
    {
      releaseLock(lockHolding[i]);
      KThread.yield();
    }
    Lib.debug('Z', KThread.currentThread().getName() + " finished.");
    
    joinSemaphores[id].V();
  }
  
  public PriorityGraderS1.TreeNode getTreeNode (int priority, TreeNode parent,
    ThreadHandler th)
  {
    return new TreeNode(priority, parent, th);
  }
  
  public int getRandomPriority ()
  {
    return Lib.random(PriorityScheduler.priorityMaximum
      - PriorityScheduler.priorityMinimum)
      + PriorityScheduler.priorityMinimum;
  }
  
  public void assertCorrectScheduler ()
  {
    assertTrue(ThreadedKernel.scheduler.getClass().getSimpleName().equals(
      "PriorityScheduler"), "This test need PriorityScheduler.");
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public void run ()
  {
    assertCorrectScheduler();
    
    final int threads = getIntegerArgument("threads");
    final int times = getIntegerArgument("times");
    final int locks = getIntegerArgument("locks");
    
    assertTrue(threads > 0, "invalid argument: number of threads");
    assertTrue(times > 0, "invalid argument: times of test");
    assertTrue(locks > 0, "invalid argument: number of locks");
    
    lockSet = new Lock[locks];
    lockHolders = new ThreadHandler[locks];
    lockWaiters = new LinkedList[locks];
    joinSemaphores = new Semaphore[threads];
    waitingForLockNo = new int[threads];
    for (int i = 0; i < locks; ++i)
    {
      lockSet[i] = new Lock();
      lockWaiters[i] = new LinkedList<ThreadHandler>();
    }
    
    ready = new HashSet<ThreadHandler>();
    handlers = new HashSet<ThreadHandler>();
    trees = new HashMap<ThreadHandler, TreeNode>();
    
    for (int i = 0; i < threads; ++i)
    {
      joinSemaphores[i] = new Semaphore(0);
      waitingForLockNo[i] = -1;
      boolean intStatus = Machine.interrupt().disable();
      ThreadHandler handler = forkNewThread(new Runnable()
      {
        @Override
        public void run ()
        {
          runThread(times, locks);
        }
      });
      handler.thread.setName(thdPrefix + i);
      int p = getRandomPriority();
      ThreadedKernel.scheduler.setPriority(handler.thread, p);
      Lib.debug('Z', "created " + thdPrefix + i + " with priority " + p);
      trees.put(handler, getTreeNode(p, null, handler));
      handlers.add(handler);
      ready.add(handler);
      trees.get(handler).setTime(Machine.timer().getTime());
      Machine.interrupt().restore(intStatus);
    }
    
    // for (ThreadHandler handler : handlers) {
    // handler.thread.join();
    // }
    
    for (int i = 0; i < threads; ++i)
      joinSemaphores[i].P();
    
    ready.clear();
    handlers.clear();
    trees.clear();
    
    done();
  }
  
  public int getTestThreadId (String x)
  {
    if (x.startsWith(thdPrefix))
      return new Integer(x.substring(thdPrefix.length())).intValue();
    else
      return -1;
  }
  
  @Override
  public void readyThread (KThread thread)
  {
    super.readyThread(thread);
    if (ready != null)
    {
      if (!thread.getName().startsWith(thdPrefix))
        return;
      int id = getTestThreadId(thread.getName());
      ThreadHandler handler = getThreadHandler(thread);
      ready.add(handler);
      if (trees.get(handler) != null)
        trees.get(handler).setTime(Machine.timer().getTime());
      if (waitingForLockNo[id] >= 0)
      {
        int lockId = waitingForLockNo[id];
        waitingForLockNo[id] = -1;
        lockHolders[lockId] = handler;
        Lib.debug('Z', "removing " + lockId + "'s waiter #" + id);
        assertTrue(lockWaiters[lockId].remove(handler),
          "ready thread not found");
        for (ThreadHandler waiting : lockWaiters[lockId])
        {
          long temp = trees.get(waiting).getTime();
          trees.get(handler).addChild(trees.get(waiting));
          trees.get(waiting).setTime(temp);
        }
      }
    }
  }
  
  @Override
  public void runningThread (KThread thread)
  {
    runningThread(thread, true);
  }
  
  public void runningThread (KThread thread, boolean checkSchedule)
  {
    super.runningThread(thread);
    if (!thread.getName().startsWith(thdPrefix))
      return;
    if (ready != null)
    {
      assertTrue(ready.contains(getThreadHandler(thread)), thread.getName()
        + " is not in the ready queue");
    }
    if (ready != null)
    {
      ThreadHandler handler = getThreadHandler(thread);
      assertTrue(handler != null, "handler==null");
      ready.remove(handler);
      // Lib.debug('Z', handler.thread.getName());
      int ep = ThreadedKernel.scheduler.getEffectivePriority(handler.thread);
      long et = trees.get(handler).getTime();
      if (ep != trees.get(handler).getPriority())
      {
        trees.get(handler).printChild(1);
      }
      assertTrue(ep == trees.get(handler).getPriority(), thread.getName()
        + " does not have the correct" + " priority, have got " + ep
        + ", should be " + trees.get(handler).getPriority());
      for (ThreadHandler tempHandler : ready)
      {
        // TODO check whether they are in readyQueue
        int tp = ThreadedKernel.scheduler
          .getEffectivePriority(tempHandler.thread);
        // System.out.println(tempHandler.thread.getName());
        long tt = trees.get(tempHandler).getTime();
        // System.out.println("--" + tempHandler.thread.getName());
        if (!tempHandler.thread.getName().startsWith(thdPrefix))
          assertTrue(tp == trees.get(tempHandler).getPriority(),
            tempHandler.thread.getName()
              + " does not have the correct priority - have got " + tp
              + ", should be " + trees.get(tempHandler).getPriority());
        if (tp > ep)
        {
          trees.get(tempHandler).printChild(1);
          trees.get(handler).printChild(1);
        }
        if (checkSchedule)
        {
          assertTrue(tp <= ep, tempHandler.thread.getName()
            + " has a higher priority than the running thread "
            + handler.thread.getName() + " (" + tp + " vs. " + ep + ")");
          assertTrue(!(tp == ep && tt < et), tempHandler.thread.getName()
            + " arrived earlier than the running thread");
        }
      }
    }
  }
}

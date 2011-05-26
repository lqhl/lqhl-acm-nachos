package nachos.ag;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.threads.KThread;
import nachos.threads.ThreadedKernel;

/**
 * A grader for priority scheduling.
 * 
 * @author Sangxia Huang
 */
public class LotteryGraderS extends PriorityGraderS1
{
  
  protected class LotteryTreeNode extends PriorityGraderS1.TreeNode
  {
    
    public LotteryTreeNode (int priority, TreeNode parent, ThreadHandler th)
    {
      super(priority, parent, th);
    }
    
    public int getPriority ()
    {
      return priority + currentDonate;
    }
    
    public void addChild (TreeNode c)
    {
      children.add(c);
      c.parent = this;
      c.time = Machine.timer().getTime();
      currentDonate += c.getPriority();
      if (parent != null)
        parent.update();
    }
    
    public void update ()
    {
      int temp = 0;
      for (TreeNode child : children)
        temp += child.getPriority();
      currentDonate = temp;
      if (parent != null)
        parent.update();
    }
    
    public void releaseChild (TreeNode x)
    {
      Lib.assertTrue(parent == null);
      x.parent = null;
      currentDonate -= x.getPriority();
      children.remove(x);
    }
  }
  
  public PriorityGraderS1.TreeNode getTreeNode (int priority, TreeNode parent,
    ThreadHandler th)
  {
    return new LotteryTreeNode(priority, parent, th);
  }
  
  @Override
  public int getRandomPriority ()
  {
    return Lib.random(10) + 1;
  }
  
  @Override
  public void runningThread (KThread thread)
  {
    super.runningThread(thread, false);
  }
  
  @Override
  public void assertCorrectScheduler ()
  {
    assertTrue(ThreadedKernel.scheduler.getClass().getSimpleName().equals(
      "LotteryScheduler"), "This test need LotteryScheduler.");
  }
}

package nachos.ag;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import nachos.machine.Machine;

/**
 * @author Xiangru Chen
 * 
 */
public class VMGrader extends CoffGrader
{
  
  private static final String SwapFile = "swapFile";
  
  private static final int ActionPhyPages = 20;
  private static final int ActionGetSwapSize = 21;
  
  private String swapFile = null;
  
  @Override
  protected void init ()
  {
    super.init();
    
    System.out.println("\ninitializing VMGrader..");
    System.out.println("physical pages = "
      + Machine.processor().getNumPhysPages());
    
    if (hasArgument(SwapFile))
      swapFile = getStringArgument(SwapFile);
  }
  
  @Override
  protected int handleTestSystemCall (int type, int a0, int a1, int a2)
  {
    switch (type)
    {
      case ActionPhyPages:
        return Machine.processor().getNumPhysPages();
      case ActionGetSwapSize:
        return getSwapSize();
      default:
        return super.handleTestSystemCall(type, a0, a1, a2);
    }
  }
  
  int swapSize;
  
  private int getSwapSize ()
  {
    assertTrue(swapFile != null, "swap file unspecified.");
    privilege.doPrivileged(new Runnable()
    {
      @Override
      public void run ()
      {
        File swap = new File(testRoot.getAbsolutePath() + "/" + swapFile);
        assertTrue(swap.exists(), "swap file not exist");
        try
        {
          InputStream stream = new FileInputStream(swap);
          swapSize = stream.available();
          stream.close();
        }
        catch (IOException e)
        {
          swapSize = 0;
        }
      }
    });
    return swapSize;
  }
}

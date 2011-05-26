package nachos.ag;

import nachos.filesys.FilesysKernel;

/**
 * @author Bo Tang
 * 
 */
public class FilesysGrader extends CoffGrader
{
  private static final int ActionGetDiskFreeSize = 22;
  
  // Return the number of free sectors in your disk including swapFile sectors
  private int getFreeSize ()
  {
    int swapSize = FilesysKernel.realFileSystem.getSwapFileSectors();
    return FilesysKernel.realFileSystem.getFreeSize() + swapSize;
  }
  
  @Override
  protected int handleTestSystemCall (int type, int a0, int a1, int a2)
  {
    switch (type)
    {
      case ActionGetDiskFreeSize:
        return getFreeSize();
      default:
        return super.handleTestSystemCall(type, a0, a1, a2);
    }
  }
  
  @Override
  protected void init ()
  {
    super.init();
  }
}

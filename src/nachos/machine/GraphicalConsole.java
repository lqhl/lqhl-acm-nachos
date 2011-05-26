// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import nachos.security.Privilege;

/**
 * A graphical console that uses the AWT to put a console in a window.
 */
public final class GraphicalConsole implements SerialConsole
{
  /**
   * Allocate a new graphical console.
   */
  public GraphicalConsole (Privilege privilege)
  {
    System.out.print(" gconsole");
    this.init(privilege);
  }
  
  public void init (Privilege privilege)
  {
    this.privilege = privilege;
    
    receiveInterrupt = new Runnable()
    {
      public void run ()
      {
        receiveInterrupt();
      }
    };
    
    sendInterrupt = new Runnable()
    {
      public void run ()
      {
        sendInterrupt();
      }
    };
    
    privilege.doPrivileged(new Runnable()
    {
      @Override
      public void run ()
      {
        initGUI();
      }
    });
    
    incomingQueue = new LinkedList<Integer>();
    incomingKey = -1;
    scheduleReceiveInterrupt();
    
    outgoingKey = -1;
  }
  
  private void initGUI ()
  {
    
    textArea = new JTextArea(25, 100);
    textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    
    textArea.setEditable(false);
    textArea.addKeyListener(new KeyListener()
    {
      public void keyPressed (KeyEvent e)
      {
        GraphicalConsole.this.keyPressed(e);
      }
      
      public void keyReleased (KeyEvent e)
      {
      }
      
      public void keyTyped (KeyEvent e)
      {
      }
    });
    textArea.setLineWrap(true);
    
    scrollPane = new JScrollPane(textArea);
    
    frame = new JFrame("Nachos console");
    frame.getContentPane().add(scrollPane);
    
    frame.addWindowListener(new WindowAdapter()
    {
      public void windowClosing (WindowEvent e)
      {
        Machine.terminate();
      }
      
      // public void windowActivated(WindowEvent e) {
      // textArea.append("");
      // }
    });
    
    frame.pack();
    frame.setVisible(true);
    
  }
  
  public void setInterruptHandlers (Runnable receiveInterruptHandler,
    Runnable sendInterruptHandler)
  {
    this.receiveInterruptHandler = receiveInterruptHandler;
    this.sendInterruptHandler = sendInterruptHandler;
  }
  
  private void keyPressed (KeyEvent e)
  {
    char c = e.getKeyChar();
    if (c == 0x03)
      frame.dispose();
    
    if ((c < 0x20 || c >= 0x80) && c != '\t' && c != '\n' && c != '\b'
      && c != 0x1B)
      return;
    
    synchronized (incomingQueue)
    {
      incomingQueue.add(new Integer((int)c));
    }
  }
  
  private void scheduleReceiveInterrupt ()
  {
    privilege.interrupt.schedule(Stats.ConsoleTime, "console read",
      receiveInterrupt);
  }
  
  private void receiveInterrupt ()
  {
    Lib.assertTrue(incomingKey == -1);
    
    synchronized (incomingQueue)
    {
      if (incomingQueue.isEmpty())
      {
        scheduleReceiveInterrupt();
      }
      else
      {
        Integer i = incomingQueue.removeFirst();
        incomingKey = i.intValue();
        
        privilege.stats.numConsoleReads++;
        if (receiveInterruptHandler != null)
          receiveInterruptHandler.run();
      }
    }
  }
  
  public int readByte ()
  {
    int key = incomingKey;
    
    if (incomingKey != -1)
    {
      incomingKey = -1;
      scheduleReceiveInterrupt();
    }
    
    return key;
  }
  
  private void scheduleSendInterrupt ()
  {
    privilege.interrupt.schedule(Stats.ConsoleTime, "console write",
      sendInterrupt);
  }
  
  private void sendInterrupt ()
  {
    Lib.assertTrue(outgoingKey != -1);
    
    Runnable send = new Runnable()
    {
      public void run ()
      {
        if (outgoingKey == '\b')
        {
          // backspace
          textArea.replaceRange(null, textArea.getText().length() - 1, textArea
            .getText().length());
        }
        else if (outgoingKey == '\007')
        {
          // beep
        }
        else
        {
          textArea.append("" + (char)outgoingKey);
        }
      }
    };
    
    try
    {
      SwingUtilities.invokeAndWait(send);
    }
    catch (InvocationTargetException e)
    {
      Machine.terminate(e.getTargetException());
    }
    catch (Exception e)
    {
      Machine.terminate(e);
    }
    
    outgoingKey = -1;
    privilege.stats.numConsoleWrites++;
    if (sendInterruptHandler != null)
      sendInterruptHandler.run();
  }
  
  public void writeByte (int value)
  {
    if (outgoingKey == -1)
      scheduleSendInterrupt();
    outgoingKey = value & 0xFF;
  }
  
  private Privilege privilege;
  
  private Runnable receiveInterrupt;
  private Runnable sendInterrupt;
  
  private Runnable receiveInterruptHandler = null;
  private Runnable sendInterruptHandler = null;
  
  private JFrame frame;
  private JTextArea textArea;
  private JScrollPane scrollPane;
  
  private LinkedList<Integer> incomingQueue;
  private int incomingKey;
  private int outgoingKey;
}

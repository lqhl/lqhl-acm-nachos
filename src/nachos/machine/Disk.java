package nachos.machine;

// Disk.java
// Routines to simulate a physical disk device; reading and writing
// to the disk is simulated as reading and writing to a UNIX file.
//
// A physical disk
// can accept (one at a time) requests to read/write a disk sector;
// when the request is satisfied, the CPU gets an interrupt, and
// the next request can be sent to the disk.
//
// Disk contents are preserved across machine crashes, but if
// a file system operation (eg, create a file) is in progress when the
// system shuts down, the file system may be corrupted.
//
// Disk operations are asynchronous, so we have to invoke an interrupt
// handler when the simulated operation completes.
//
// DO NOT CHANGE -- part of the machine emulation

// The following class defines a physical disk I/O device. The disk
// has a single surface, split up into "tracks", and each track split
// up into "sectors" (the same number of sectors on each track, and each
// sector has the same number of bytes of storage).
//
// Addressing is by sector number -- each sector on the disk is given
// a unique number: track * SectorsPerTrack + offset within a track.
//
// As with other I/O devices, the raw physical disk is an asynchronous device --
// requests to read or write portions of the disk return immediately,
// and an interrupt is invoked later to signal that the operation completed.
//
// The physical disk is in fact simulated via operations on a UNIX file.
//
// To make life a little more realistic, the simulated time for
// each operation reflects a "track buffer" -- RAM to store the contents
// of the current track as the disk head passes by. The idea is that the
// disk always transfers to the track buffer, in case that data is requested
// later on. This has the benefit of eliminating the need for
// "skip-sector" scheduling -- a read request which comes in shortly after
// the head has passed the beginning of the sector can be satisfied more
// quickly, because its contents are in the track buffer. Most
// disks these days now come with a track buffer.
//
// The track buffer simulation can be disabled by compiling with -DNOTRACKBUF

import java.io.*;
import nachos.security.Privilege;

/**
 * @author Kang Zhang
 * 
 *         This file is derived from the code by 1998 Rice University and
 *         1992-1993 The Regents of the University of California.
 */

public class Disk
{
  
  // track buffer support
  private static final boolean NOTRACKBUF = true;
  
  // number of bytes per disk sector
  public static final int SectorSize = 128 * 4;
  
  // number of sectors per disk track
  private static final int SectorsPerTrack = 32 * 2;
  
  // number of tracks per disk
  private static final int NumTracks = 32 * 2;
  
  // total # of sectors per disk
  public static final int NumSectors = (SectorsPerTrack * NumTracks);
  
  private Privilege privilege;
  
  private RandomAccessFile file;// UNIX file for simulated disk
  
  private DiskIntHandler dskHandler; // internal interrupt handler
  
  private Runnable handler; // Interrupt handler, to be invoked when any disk
  // request finishes
  
  private boolean active; // Is a disk operation in progress?
  
  private int lastSector; // The previous disk request
  
  private int bufferInit; // When the track buffer started being loaded
  
  private int seek; // times computed by
  
  private int rotation; // timeToSeek()
  
  // We put this at the front of the UNIX file representing the
  // disk, to make it less likely we will accidentally treat a useful file
  // as a disk (which would probably trash the file's contents).
  private static final int MagicNumber = 0x456789ab;
  
  private static final int MagicSize = 4;
  
  public static final int DiskSize = MagicSize + (NumSectors * SectorSize);
  
  // ----------------------------------------------------------------------
  // Disk()
  // Create a simulated disk.
  // Invoke callWhenDone.run(callArg) every time a request completes.
  //
  // Open the UNIX file (creating it
  // if it doesn't exist), and check the magic number to make sure it's
  // ok to treat it as Nachos disk storage.
  //
  // "name" -- text name of the file simulating the Nachos disk
  // "callWhenDone" -- interrupt handler to be called when disk read/write
  // request completes
  // ----------------------------------------------------------------------
  
  public Disk (Privilege privilege, final String name, Runnable callWhenDone)
  {
    int magicNum;
    long len = 0;
    
    Lib.debug('d', "Initializing the disk\n");
    
    this.privilege = privilege;
    handler = callWhenDone;
    lastSector = 0;
    bufferInit = 0;
    
    dskHandler = new DiskIntHandler(this);
    
    privilege.doPrivileged(new Runnable()
    {
      public void run ()
      {
        try
        {
          file = new RandomAccessFile(name, "rw");
        }
        catch (IOException e)
        {
          file = null;
        }
      }
    });
    
    if (file != null)
      try
      {
        len = file.length();
      }
      catch (IOException e1)
      {
      }
    
    if (file != null && len != 0)
    {
      // file exists, check magic number
      try
      {
        magicNum = file.readInt();
      }
      catch (IOException e)
      {
        magicNum = 0;
      }
      
      Lib.assertTrue(magicNum == MagicNumber);
      
    }
    else
    {
      // file doesn't exist, create it
      try
      {
        // code ugly here --starforever
        // FileOutputStream fsFile;
        // fsFile = new FileOutputStream(name);
        // fsFile.close();
        // file = new RandomAccessFile(name, "rw");
        // file.writeInt(MagicNumber); // write magic number
        
        // need to write at end of file, so that reads will not return EOF
        // file.seek(DiskSize - 4);
        // file.writeInt(0);
        if (file == null)
          file = new RandomAccessFile(name, "rw");
        file.setLength(DiskSize);
        file.seek(0);
        file.writeInt(MagicNumber);
        file.seek(DiskSize - 4);
        file.writeInt(0);
      }
      catch (IOException e)
      {
        Lib.assertTrue(false, "Can't create Disk file");
      }
    }
    active = false;
  }
  
  // ----------------------------------------------------------------------
  // readRequest/writeRequest
  // These routines send a request to the disk and return immediately.
  //
  // Simulate a request to read/write a single disk sector
  // Do the read/write immediately to the UNIX file
  // Set up an interrupt handler to be called later,
  // that will notify the caller when the simulator says
  // the operation has completed.
  //
  // Note that a disk only allows an entire sector to be read/written,
  // not part of a sector.
  //
  // "sectorNumber" -- the disk sector to read/write
  // "data" -- the bytes to be written, the buffer to hold the incoming bytes
  // ----------------------------------------------------------------------
  
  public void readRequest (int sectorNumber, byte[] data, int index)
  {
    int ticks = computeLatency(sectorNumber, false);
    ticks = Math.max(ticks / 1000, 1);
    
    Lib.assertTrue(!active); // only one request at a time
    Lib.assertTrue((sectorNumber >= 0) && (sectorNumber < NumSectors));
    
    Lib.debug('d', "Reading from sector " + sectorNumber);
    
    try
    {
      file.seek(MagicSize + sectorNumber * SectorSize);
      file.read(data, index, SectorSize);
    }
    catch (IOException e)
    {
      Lib.assertTrue(false, "Can't read Disk file!");
    }
    
    // if (Debug.isEnabled('d'))
    // printSector(false, sectorNumber, data);
    
    active = true;
    updateLast(sectorNumber);
    privilege.stats.numDiskReads++;
    privilege.interrupt.schedule((long)ticks, "disk", dskHandler);
  }
  
  public void writeRequest (int sectorNumber, byte[] data, int index)
  {
    int ticks = computeLatency(sectorNumber, true);
    ticks = Math.max(ticks / 1000, 1);
    
    Lib.assertTrue(!active);
    Lib.assertTrue((sectorNumber >= 0) && (sectorNumber < NumSectors));
    
    Lib.debug('d', "Writing to sector " + sectorNumber);
    
    try
    {
      file.seek(SectorSize * sectorNumber + MagicSize);
      file.write(data, index, SectorSize);
    }
    catch (IOException e)
    {
      Lib.assertTrue(false, "Can't write Disk file!");
    }
    
    // if (Debug.isEnabled('d'))
    // printSector(true, sectorNumber, data);
    
    active = true;
    updateLast(sectorNumber);
    privilege.stats.numDiskWrites++;
    privilege.interrupt.schedule(ticks, "Disk", dskHandler);
  }
  
  // ----------------------------------------------------------------------
  // handleInterrupt()
  // Called when it is time to invoke the disk interrupt handler,
  // to tell the Nachos kernel that the disk request is done.
  // ----------------------------------------------------------------------
  
  public void handleInterrupt ()
  {
    active = false;
    handler.run();
  }
  
  // ----------------------------------------------------------------------
  // computeLatency()
  // Return how long will it take to read/write a disk sector, from
  // the current position of the disk head.
  //
  // Latency = seek time + rotational latency + transfer time
  // Disk seeks at one track per SeekTime ticks (cf. stats.h)
  // and rotates at one sector per RotationTime ticks
  //
  // To find the rotational latency, we first must figure out where the
  // disk head will be after the seek (if any). We then figure out
  // how long it will take to rotate completely past newSector after
  // that point.
  //
  // The disk also has a "track buffer"; the disk continuously reads
  // the contents of the current disk track into the buffer. This allows
  // read requests to the current track to be satisfied more quickly.
  // The contents of the track buffer are discarded after every seek to
  // a new track.
  // ----------------------------------------------------------------------
  
  @SuppressWarnings("all")
  public int computeLatency (int newSector, boolean writing)
  {
    timeToSeek(newSector); // computes seek and rotation
    int timeAfter = (int)(privilege.stats.totalTicks + seek + rotation);
    
    if (NOTRACKBUF == false)
    {
      // turn this on if you don't want the track buffer stuff
      
      // check if track buffer applies
      if ((writing == false)
        && (seek == 0)
        && (((timeAfter - bufferInit) / Stats.RotationTime) > moduloDiff(
          newSector, bufferInit / Stats.RotationTime)))
      {
        Lib.debug('d', "Request latency = " + Stats.RotationTime);
        return Stats.RotationTime; // time to transfer sector from the
        // track buffer
      }
    }
    
    rotation += moduloDiff(newSector, timeAfter / Stats.RotationTime)
      * Stats.RotationTime;
    
    Lib.debug('d', "Request latency = " + seek + rotation + Stats.RotationTime);
    return (seek + rotation + Stats.RotationTime);
  }
  
  // ----------------------------------------------------------------------
  // timeToSeek()
  // computes how long it will take to position the disk head over the correct
  // track on the disk. Since when we finish seeking, we are likely
  // to be in the middle of a sector that is rotating past the head,
  // we also return how long until the head is at the next sector boundary.
  // updates Disk.seek and and Disk.rotation
  // Disk seeks at one track per SeekTime ticks (cf. stats.h)
  // and rotates at one sector per RotationTime ticks
  // ----------------------------------------------------------------------
  
  private void timeToSeek (int newSector)
  {
    
    int newTrack = newSector / SectorsPerTrack;
    int oldTrack = lastSector / SectorsPerTrack;
    seek = Math.abs(newTrack - oldTrack) * Stats.SeekTime;
    // how long will seek take?
    int over = (int)((privilege.stats.totalTicks + seek) % Stats.RotationTime);
    // will we be in the middle of a sector when
    // we finish the seek?
    
    rotation = 0;
    if (over > 0) // if so, need to round up to next full sector
      rotation = Stats.RotationTime - over;
    
  }
  
  // ----------------------------------------------------------------------
  // moduloDiff()
  // Return number of sectors of rotational delay between target sector
  // "to" and current sector position "from"
  // ----------------------------------------------------------------------
  
  private int moduloDiff (int to, int from)
  {
    
    int toOffset = to % SectorsPerTrack;
    int fromOffset = from % SectorsPerTrack;
    
    return ((toOffset - fromOffset) + SectorsPerTrack) % SectorsPerTrack;
  }
  
  // ----------------------------------------------------------------------
  // updateLast
  // Keep track of the most recently requested sector. So we can know
  // what is in the track buffer.
  // ----------------------------------------------------------------------
  
  private void updateLast (int newSector)
  {
    timeToSeek(newSector); // computes seek and rotation
    if (seek != 0)
      bufferInit = (int)(privilege.stats.totalTicks + seek + rotation);
    lastSector = newSector;
    Lib.debug('d', "Updating last sector = " + lastSector + ", " + bufferInit);
  }
  
  // ----------------------------------------------------------------------
  // printSector()
  // Dump the data in a disk read/write request, for debugging.
  // ----------------------------------------------------------------------
  
  public static void printSector (boolean writing, int sector, byte[] data)
  {
    int val;
    
    if (writing)
      Lib.debug('+', "Writing sector: " + sector);
    else
      Lib.debug('+', "Reading sector: " + sector);
    for (int i = 0; i < (SectorSize / 4); i++)
    {
      val = intInt(data, i * 4);
      Lib.debug('+', val + " ");
    }
    Lib.debug('+', "\n");
    
  }
  
  // externalize an Integer
  public static void extInt (int val, byte[] buffer, int pos)
  {
    buffer[pos] = (byte)(val >> 24 & 0xff);
    buffer[pos + 1] = (byte)(val >> 16 & 0xff);
    buffer[pos + 2] = (byte)(val >> 8 & 0xff);
    buffer[pos + 3] = (byte)(val & 0xff);
  }
  
  // internalize an Integer
  public static int intInt (byte[] buffer, int pos)
  {
    return (buffer[pos] << 24) | ((buffer[pos + 1] << 16) & 0xff0000)
      | ((buffer[pos + 2] << 8) & 0xff00) | (buffer[pos + 3] & 0xff);
  }
  
}

// Disk interrupt handler class
//
class DiskIntHandler implements Runnable
{
  private Disk disk;
  
  public DiskIntHandler (Disk dsk)
  {
    disk = dsk;
  }
  
  public void run ()
  {
    disk.handleInterrupt();
  }
}

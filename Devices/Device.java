/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * I pledge my honor that all parts of this project were done by me   
 * individually and without collaboration with anybody else.
 * 
 *                                              109971346 Hung-Ruey Chen
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
package osp.Devices;
import osp.IFLModules.*;
import osp.Threads.*;
import osp.Utilities.*;
import osp.Hardware.*;
import osp.Memory.*;
import osp.FileSys.*;
import osp.Tasks.*;
import java.util.*;

public class Device extends IflDevice
{
    /**
        This constructor initializes a device with the provided parameters.
	As a first statement it must have the following:

	    super(id,numberOfBlocks);

	@param numberOfBlocks -- number of blocks on device

        @OSPProject Devices
    */
    public Device(int id, int numberOfBlocks)
    {
        super(id,numberOfBlocks);
        System.out.println("Device()");
        this.iorbQueue = new IOQueue();
    }

    /**
       This method is called once at the beginning of the
       simulation. Can be used to initialize static variables.

       @OSPProject Devices
    */
    public static void init()
    {
        // your code goes here
        System.out.println("Device.init ");
        
        System.out.println("Device.init2" );
    }

    /**
       Enqueues the IORB to the IORB queue for this device
       according to some kind of scheduling algorithm.
       
       This method must lock the page (which may trigger a page fault),
       check the device's state and call startIO() if the 
       device is idle, otherwise append the IORB to the IORB queue.

       @return SUCCESS or FAILURE.
       FAILURE is returned if the IORB wasn't enqueued 
       (for instance, locking the page fails or thread is killed).
       SUCCESS is returned if the IORB is fine and either the page was 
       valid and device started on the IORB immediately or the IORB
       was successfully enqueued (possibly after causing pagefault pagefault)
       
       @OSPProject Devices
    */
    public int do_enqueueIORB(IORB iorb)
    {

        iorb.getPage().lock(iorb);
        iorb.getOpenFile().incrementIORBCount();
        
        System.out.println("iorb " + iorb.toString());
        Disk disk = (Disk)Device.get(iorb.getID());
        
        int block_no = iorb.getBlockNumber();
        int bytes_per_block = (int)Math.pow(2,MMU.getVirtualAddressBits()-MMU.getPageAddressBits()); // bytes per block
        System.out.println("bytes_per_block "+bytes_per_block+" getBytesPerSector "+disk.getBytesPerSector());
        int sec_per_block = bytes_per_block / disk.getBytesPerSector();
        System.out.println("block_per_sec "+sec_per_block+" trackpercylinder "+disk.getPlatters() + " getSectorsPerTrack "+disk.getSectorsPerTrack());
        int cylinder = block_no * sec_per_block / disk.getSectorsPerTrack()/disk.getPlatters();
        System.out.println("cylinder " + cylinder);
        iorb.setCylinder(cylinder);
        IOQueue<Integer> qu=new IOQueue<Integer>();
        System.out.println("isEmpty "+qu.isEmpty());
        
        disk.getHeadPosition();   
        qu.insert(4);
        qu.insert(2);
        qu.insert(3);
        qu.insert(1);     
        System.out.println("list " + qu.toString());
        Collections.sort(qu.getSub(0,3));
        System.out.println("list " + qu.toString());
        if (iorb.getThread().getStatus() == ThreadKill)
            return FAILURE;
        if (!disk.isBusy())
            disk.startIO(iorb);
        else
            iorbQueue.isEmpty(); 
        return SUCCESS;
    }

    /**
       Selects an IORB (according to some scheduling strategy)
       and dequeues it from the IORB queue.

       @OSPProject Devices
    */
    public IORB do_dequeueIORB()
    {
        // your code goes here
        return null;
    }

    /**
        Remove all IORBs that belong to the given ThreadCB from 
	this device's IORB queue

        The method is called when the thread dies and the I/O 
        operations it requested are no longer necessary. The memory 
        page used by the IORB must be unlocked and the IORB count for 
	the IORB's file must be decremented.

	@param thread thread whose I/O is being canceled

        @OSPProject Devices
    */
    public void do_cancelPendingIO(ThreadCB thread)
    {
        // your code goes here

    }

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
	
	@OSPProject Devices
     */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.
	
	@OSPProject Devices
     */
    public static void atWarning()
    {
        // your code goes here

    }


}

/*
      Feel free to add local classes to improve the readability of your code
*/

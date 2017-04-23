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
    public Device(int id, int numberOfBlocks)
    {
        super(id,numberOfBlocks);
        this.iorbQueue = new IOQueue();
    }

    public static void init()
    {

    }

    public int do_enqueueIORB(IORB iorb)
    {

        System.out.println("iorb " + iorb.toString());
        iorb.getPage().lock(iorb);
        iorb.getOpenFile().incrementIORBCount();
        
        Disk disk = (Disk)Device.get(iorb.getID());
        int cylinder = CalcuteCylinder(iorb);
        System.out.println("cylinder " + cylinder);

        iorb.setCylinder(cylinder);
        disk.getHeadPosition();   

        if (iorb.getThread().getStatus() == ThreadKill)
            return FAILURE;
        if (!disk.isBusy())
            disk.startIO(iorb);
        else
        {
            if(cylinder>disk.getHeadPosition()) // put in current scan
            {
                System.out.println("Disk busy, insert current scan queue. " + cylinder);
                ((IOQueue)iorbQueue).addCurrentScan(iorb,cylinder);

            } 
            else // put in next scan
            {
                System.out.println("Disk busy, insert next scan queue. " + cylinder);    
                ((IOQueue)iorbQueue).addNextScan(iorb,cylinder);
            }
        }
        return SUCCESS;
    }


    public IORB do_dequeueIORB()
    {
        if(((IOQueue)iorbQueue).length() == 0)
            return null;
        
        if(((IOQueue)iorbQueue).current_ind>=((IOQueue)iorbQueue).length() ) // no current scan, go to head of queue
            ((IOQueue)iorbQueue).current_ind = 0;

        IORB next = ((IOQueue)iorbQueue).getIORB();
        ((IOQueue)iorbQueue).delete(0);
        return next;
    }

    public void do_cancelPendingIO(ThreadCB thread)
    {
        if(((IOQueue)iorbQueue).length() != 0)
            ((IOQueue)iorbQueue).dequeue_thread(thread);
    }

    public static void atError()
    {
        // your code goes here

    }

    public static void atWarning()
    {
        // your code goes here

    }

    public static int CalcuteCylinder(IORB iorb)
    {
        Disk disk = (Disk)Device.get(iorb.getID());
        int bytes_per_block = (int)Math.pow(2,MMU.getVirtualAddressBits()-MMU.getPageAddressBits()); // bytes per block
        int sec_per_block = bytes_per_block / disk.getBytesPerSector();
        return iorb.getBlockNumber() * sec_per_block / disk.getSectorsPerTrack()/disk.getPlatters();
    }
    

}

/*
      Feel free to add local classes to improve the readability of your code
*/

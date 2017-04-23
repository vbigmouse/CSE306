/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * I pledge my honor that all parts of this project were done by me   
 * individually and without collaboration with anybody else.
 * 
 *                                              109971346 Hung-Ruey Chen
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
package osp.Devices;
import java.util.*;
import osp.IFLModules.*;
import osp.Hardware.*;
import osp.Interrupts.*;
import osp.Threads.*;
import osp.Utilities.*;
import osp.Tasks.*;
import osp.Memory.*;
import osp.FileSys.*;

public class DiskInterruptHandler extends IflDiskInterruptHandler
{
    public void do_handleInterrupt()
    {
        IORB iorb = (IORB)InterruptVector.getEvent();
        //System.out.println("do_handleInterrupt " + iorb.toString());
        iorb.getPage().unlock();
        iorb.getOpenFile().decrementIORBCount();

        if(iorb.getOpenFile().closePending == true && iorb.getOpenFile().getIORBCount() == 0)
            iorb.getOpenFile().close();

        if(iorb.getThread().getTask().getStatus()!=TaskTerm) // if task alive
        {
            if(iorb.getDeviceID() != SwapDeviceID )         // not swap-in/out
            {   
                iorb.getPage().getFrame().setReferenced(true);
                if(iorb.getIOType()==FileRead)
                    iorb.getPage().getFrame().setDirty(true);
            }
            else if(iorb.getDeviceID() == SwapDeviceID )    // is swap-in/out
            {
                iorb.getPage().getFrame().setDirty(false);
            }
        }
        else if(iorb.getThread().getTask().getStatus()==TaskTerm)
        {
            if(iorb.getPage().getFrame().getReserved()==iorb.getThread().getTask())
                iorb.getPage().getFrame().setUnreserved(iorb.getThread().getTask());
        }
        iorb.notifyThreads();
        Device disk = Device.get(iorb.getDeviceID());
        if(disk.isBusy())
            disk.setBusy(false);
        IORB next = disk.dequeueIORB();
        if(next!=null)
            disk.startIO(next);
        iorb.getThread().dispatch();
    }

}



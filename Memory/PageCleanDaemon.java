/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * I pledge my honor that all parts of this project were done by me   
 * individually and without collaboration with anybody else.
 * 
 *                                              109971346 Hung-Ruey Chen
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package osp.Memory;
import java.util.*;
import com.sun.net.httpserver.Authenticator.Failure;
import osp.Hardware.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.FileSys.FileSys;
import osp.FileSys.OpenFile;
import osp.IFLModules.*;
import osp.Interrupts.*;
import osp.Utilities.*;
import osp.IFLModules.*;

public class PageCleanDaemon implements DaemonInterface
{   
    //public PageCleanDaemon(){};
    public void unleash(ThreadCB thread)
    {
        System.out.println("[PageCleanDaemon][unleash] " + this.toString() + " execute time " + HClock.get() );
        this.SwapOutDirtyPage(thread);
    }

    public void SwapOutDirtyPage(ThreadCB thread)
    {
        // check if there is available frame to swap.
        FrameTableEntry frame;
        List<FrameTableEntry> dirty_frame_list;
        dirty_frame_list = new LinkedList<>();
        int index = 0;
        do
        {
            frame = MMU.getFrame(MMU.frame_reference_queue.get(index)); // search available frame in reference queue head
            if(!frame.isReserved() && frame.getLockCount()==0 && frame.isDirty()) // if frame is not reserved/lock/clean
            {
                dirty_frame_list.add(frame);
                frame.setReserved(thread.getTask());  
                System.out.println("[PageCleanDaemon][SwapOutDirtyPage] found dirty frame " + frame.toString());
                if(frame.getPage() == null)
                    System.out.println("[PageCleanDaemon][SwapOutDirtyPage] page is null! " + frame.toString());
            }
            if(dirty_frame_list.size() == 6)
                break;    
            index++;
        }while(index < MMU.frame_table_size);

        for(FrameTableEntry dirty_frame: dirty_frame_list)
        {
            System.out.println("[PageCleanDaemon][SwapOutDirtyPage] Swap-out " + dirty_frame.toString());

            if(dirty_frame.getPage() != null )
            { 
                System.out.println("[PageCleanDaemon][SwapOutDirtyPage] dirty_frame " + dirty_frame.toString() + " " 
                                                + dirty_frame.getPage().toString() + " isDirty " + dirty_frame.isDirty());
                SwapOut(dirty_frame.getPage(),thread);
                System.out.println("[PageCleanDaemon][SwapOutDirtyPage] Swap-out " + dirty_frame.toString() + " finish");
                dirty_frame.setDirty(false);
                dirty_frame.setUnreserved(thread.getTask());
            }
            else
            {
                System.out.println("[PageCleanDaemon][SwapOutDirtyPage] dirty_frame page is null " + dirty_frame.toString() + " isDirty " + dirty_frame.isDirty() );
            }


        }

    }

    public static void SwapOut(PageTableEntry page, ThreadCB thread)
    {
        if(page.getTask().getSwapFile() == null)
            System.out.println("[PageFaultHandler][SwapOut] file null!! ");         
        // open file which the memory page will be write to
        OpenFile file = page.getTask().getSwapFile();
        System.out.println("[PageFaultHandler][SwapOut] file " + file.toString());            
        // block number = page id
        int block = page.getID();
        System.out.println("[PageFaultHandler][SwapOut] block no " + block);    
        
        file.write(block,page,thread);
        
    }
}
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

public class PageFaultHandler extends IflPageFaultHandler
{
    public static int do_handlePageFault(ThreadCB thread, 
					 int referenceType,
					 PageTableEntry page)
    {
        System.out.println("[PageFaultHandler][do_handlePageFault] start do_handlePageFault( " + thread.toString() 
                + " " + referenceType + " " + page + " )");
        // if already validating, return.
        if(page.getValidatingThread()!=null)
            return FAILURE;
        else
            page.setValidatingThread(thread);
        
        // check if there is available frame to swap.
        FrameTableEntry frame;
        int available_frame_index = 0;
        do
        {
            frame = MMU.getFrame(MMU.frame_reference_queue.get(available_frame_index)); // search available frame in reference queue head
            if(!frame.isReserved() && frame.getLockCount()==0)
                break;    
            available_frame_index++;
        }while(available_frame_index < MMU.frame_table_size);
        
        if(available_frame_index == MMU.frame_table_size)
        {
            page.setValidatingThread(null);
            return NotEnoughMemory;
        }
        System.out.println("[PageFaultHandler][do_handlePageFault] Found available frame ");

        // start pagefault handle process. suspend on system event.
        SystemEvent handle_page_fault_event = new SystemEvent("[PageFaultHandler][do_handlePageFault]");
        System.out.println("[PageFaultHandler][do_handlePageFault] " + thread.toString() + " suspend on system event ");
        thread.suspend(handle_page_fault_event);
        System.out.println("[PageFaultHandler][do_handlePageFault] " + thread.toString() + " rsesume from system event ");

        // reserve frame for page
        System.out.println("[PageFaultHandler][do_handlePageFault] reserve " + frame.toString() + " for " + page.toString() );
        frame.setReserved(thread.getTask());
        System.out.println("[PageFaultHandler][do_handlePageFault] set " + frame.toString() + " Reserved by " + frame.getReserved().toString());


        // check if frame is not free
        PageTableEntry victim_page = frame.getPage();
        if( victim_page!= null)
        {
            // check if frame is dirty, swap-out 
            System.out.println("[PageFaultHandler][do_handlePageFault] victim_page " + victim_page);
            if(frame.isDirty()) 
            {
                System.out.println("[PageFaultHandler][do_handlePageFault] " + frame.toString() + 
                                                                        " is dirty, do swap-out " + thread.toString() );
                SwapOut(victim_page,thread);
                System.out.println("[PageFaultHandler][do_handlePageFault] Swap-out " + frame.toString() + " " +
                                                                     victim_page.toString() + " " + thread.toString() + " finish");
            }

            if(thread.getStatus() == ThreadKill)
            {
                System.out.println("[PageFaultHandler][do_handlePageFault] Thread " + thread.toString()+ " already killed after swap-out");
                // unreserve frame for page
                if(frame.isReserved())
                    frame.setUnreserved(thread.getTask());
                page.setValidatingThread(null);
                return FAILURE;
            }    
            FreeFrame(frame);
            victim_page.setValid(false);
            victim_page.setFrame(null);         
        }
        System.out.println("[PageFaultHandler][do_handlePageFault] set " + page + " and " + frame);
        // set frame for page, set page for frame
        page.setFrame(frame);
        frame.setPage(page);

        if(!frame.isReserved())
            frame.setReserved(thread.getTask());
        // swap-in 
        System.out.println("[PageFaultHandler][do_handlePageFault] " + frame.toString() + " " + page.toString() + " do swap-in " + thread.toString() );
        SwapIn(page,thread);
        System.out.println("[PageFaultHandler][do_handlePageFault] " + frame.toString() + " " + page.toString() + " swap-in finish " + thread.toString() );


        // resume threads waiting for this page
        page.notifyThreads();
        // resume thread which cause pagefault  
        handle_page_fault_event.notifyThreads();

        if(thread.getStatus() == ThreadKill)
        {
            System.out.println("[PageFaultHandler][do_handlePageFault] Thread already killed after swap-in");
            // unreserve frame for page
            if(frame.isReserved())
                frame.setUnreserved(thread.getTask());
            page.setValidatingThread(null);
            return FAILURE;
        }        
        // set page valid
        page.setValid(true);
        // set referenced, move to end of ref. queue
        frame.setReferenced(true);
        MoveToLast(MMU.frame_reference_queue, available_frame_index);
        ThreadCB.dispatch();

        // unreserve frame for page
        if(frame.isReserved())
            frame.setUnreserved(thread.getTask());

        page.setValidatingThread(null);
        if(thread.getStatus() == ThreadKill)
        {
            System.out.println("[PageFaultHandler][do_handlePageFault] Thread already killed");
            return FAILURE;
        }
        else
        {
            System.out.println("[PageFaultHandler][do_handlePageFault] Thread do_handlePageFault Success");            
            return SUCCESS;
        }
    }

    public static void FreeFrame(FrameTableEntry frame)
    {
        frame.setPage(null);    
        frame.setDirty(false);
        frame.setReferenced(false);
    }

    public static int SwapOut(PageTableEntry page, ThreadCB thread)
    {
        // open file which the memory page will be write to
        OpenFile file = thread.getTask().getSwapFile();
        System.out.println("[PageFaultHandler][SwapOut] page " + page.toString() + " frame " + page.getFrame().toString() + "file " + file.toString());            
        // block number = page id
        int block = page.getID();
        System.out.println("[PageFaultHandler][SwapOut] block no " + block);    
        
        file.write(block,page,thread);
        return SUCCESS;
    }

    public static int SwapIn(PageTableEntry page, ThreadCB thread)
    {
        // open file which the memory page will be write to
        OpenFile file = thread.getTask().getSwapFile();
        
        // block number = page id
        int block = page.getID();
        
        file.read(block,page,thread);
        return SUCCESS;
    }

    public static void MoveToLast(List<Integer> list,int index)
    {
        int tmp = list.get(index);
        list.remove(index);
        list.add(tmp);
    }

}


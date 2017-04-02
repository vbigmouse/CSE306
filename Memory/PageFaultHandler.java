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

/**
    The page fault handler is responsible for handling a page
    fault.  If a swap in or swap out operation is required, the page fault
    handler must request the operation.

    @OSPProject Memory
*/
public class PageFaultHandler extends IflPageFaultHandler
{
    /**
        This method handles a page fault. 

        It must check and return if the page is valid, 

        It must check if the page is already being brought in by some other
	thread, i.e., if the page's has already pagefaulted
	(for instance, using getValidatingThread()).
        If that is the case, the thread must be suspended on that page.
        
        If none of the above is true, a new frame must be chosen 
        and reserved until the swap in of the requested 
        page into this frame is complete. 

	Note that you have to make sure that the validating thread of
	a page is set correctly. To this end, you must set the page's
	validating thread using setValidatingThread() when a pagefault
	happens and you must set it back to null when the pagefault is over.

        If a swap-out is necessary (because the chosen frame is
        dirty), the victim page must be dissasociated 
        from the frame and marked invalid. After the swap-in, the 
        frame must be marked clean. The swap-ins and swap-outs 
        must are preformed using regular calls read() and write().

        The student implementation should define additional methods, e.g, 
        a method to search for an available frame.

	Note: multiple threads might be waiting for completion of the
	page fault. The thread that initiated the pagefault would be
	waiting on the IORBs that are tasked to bring the page in (and
	to free the frame during the swapout). However, while
	pagefault is in progress, other threads might request the same
	page. Those threads won't cause another pagefault, of course,
	but they would enqueue themselves on the page (a page is also
	an Event!), waiting for the completion of the original
	pagefault. It is thus important to call notifyThreads() on the
	page at the end -- regardless of whether the pagefault
	succeeded in bringing the page in or not.

        @param thread the thread that requested a page fault
        @param referenceType whether it is memory read or write
        @param page the memory page 

	@return SUCCESS is everything is fine; FAILURE if the thread
	dies while waiting for swap in or swap out or if the page is
	already in memory and no page fault was necessary (well, this
	shouldn't happen, but...). In addition, if there is no frame
	that can be allocated to satisfy the page fault, then it
	should return NotEnoughMemory

        @OSPProject Memory
    */
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

    /*
       Feel free to add methods/fields to improve the readability of your code
    */
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
/*    public init()
    {
        frame_reference_queue = new LinkedList<>();
    }*/
}

/*
      Feel free to add local classes to improve the readability of your code
*/

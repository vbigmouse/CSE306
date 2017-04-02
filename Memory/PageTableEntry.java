/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * I pledge my honor that all parts of this project were done by me   
 * individually and without collaboration with anybody else.
 * 
 *                                              109971346 Hung-Ruey Chen
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package osp.Memory;

import osp.Hardware.*;
import osp.Tasks.*;
import osp.Threads.*;

import com.sun.org.apache.xml.internal.serializer.ElemDesc;

import osp.Devices.*;
import osp.Utilities.*;
import osp.IFLModules.*;
/**
   The PageTableEntry object contains information about a specific virtual
   page in memory, including the page frame in which it resides.
   
   @OSPProject Memory

*/

public class PageTableEntry extends IflPageTableEntry
{
    /**
       The constructor. Must call

       	   super(ownerPageTable,pageNumber);
	   
       as its first statement.

       @OSPProject Memory
    */
    public PageTableEntry(PageTable ownerPageTable, int pageNumber)
    {
        // your code goes here
        super(ownerPageTable, pageNumber);

    }

    /**
       This method increases the lock count on the page by one. 

	The method must FIRST increment lockCount, THEN  
	check if the page is valid, and if it is not and no 
	page validation event is present for the page, start page fault 
	by calling PageFaultHandler.handlePageFault().

	@return SUCCESS or FAILURE
	FAILURE happens when the pagefault due to locking fails or the 
	that created the IORB thread gets killed.

	@OSPProject Memory
     */
    public int do_lock(IORB iorb)
    {
        System.out.println("[Info][PageTableEntry][do_lock] iorb " + iorb.toString() + " this " + this.toString());
        // check page in main memory is valid
        if(!this.isValid())
        {
            System.out.println("[Info][PageTableEntry][do_lock] Invalid page. ");

            // check if already under pagefault handling
            ThreadCB validating_thread = this.getValidatingThread();
            ThreadCB requrest_thread = iorb.getThread();
            
            if(validating_thread != null)
                System.out.println("[Info][PageTableEntry][do_lock] validating_thread = " + validating_thread.toString());
            if(requrest_thread != null)
                System.out.println("[Info][PageTableEntry][do_lock] requrest_thread = " + requrest_thread.toString());

            // if not under pagefault, do pagefault handling
            if(validating_thread == null)
            {
                if(PageFaultHandler.handlePageFault(requrest_thread, MemoryLock, this) != SUCCESS);
                    System.out.println("[Error][PageTableEntry][do_lock] PageFaultHandler fail!");
            }
            // request pagefault by other task, suspend until page become valid or task is killed
            else if(validating_thread != requrest_thread)
            {
                System.out.println("[Info][PageTableEntry][do_lock] Suspend request thread");
                requrest_thread.suspend(this);
                if(!this.isValid())
                    return FAILURE;
                    
            }
            // request pagefault by same task, increase lock count
            System.out.println("[Info][PageTableEntry][do_lock] Request pagefault by same task, increase lock count");
            /*
            else
            {
                int lock_count=FrameTableEntry.getLockCount();
                FrameTableEntry.incrementLockCount();
                if (FrameTableEntry.getLockCount()!=lock_count)
                    return SUCCESS;
                else
                    return FAILURE;
            }
            */
        }
        
        FrameTableEntry frame_table_entry = this.getFrame();
        System.out.println("[Info][PageTableEntry][do_lock] Get frame " + frame_table_entry.toString());
        int lock_count = frame_table_entry.getLockCount();
        frame_table_entry.incrementLockCount();
        if (frame_table_entry.getLockCount() != lock_count)
            return SUCCESS;
        else
            return FAILURE;

    }

    /** This method decreases the lock count on the page by one. 

	This method must decrement lockCount, but not below zero.

	@OSPProject Memory
    */
    public void do_unlock()
    {
        // unlock frame
        System.out.println("[PageTableEntry][do_unlock] this " + this.toString());
        if( this.getFrame().getLockCount() == 0 )
            System.out.println("[Error][PageTableEntry][do_unlock] Invalid lock number!");
        else
            this.getFrame().decrementLockCount();
    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/

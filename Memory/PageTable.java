/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * I pledge my honor that all parts of this project were done by me   
 * individually and without collaboration with anybody else.
 * 
 *                                              109971346 Hung-Ruey Chen
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package osp.Memory;
/**
    The PageTable class represents the page table for a given task.
    A PageTable consists of an array of PageTableEntry objects.  This
    page table is of the non-inverted type.

    @OSPProject Memory
*/
import java.lang.Math;
import osp.Tasks.*;
import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Hardware.*;

public class PageTable extends IflPageTable
{
    /** 
	The page table constructor. Must call
	
	    super(ownerTask)

	as its first statement.

	@OSPProject Memory
    */
    public PageTable(TaskCB ownerTask)
    {
        // your code goes here
        super(ownerTask);
        int max_page = (int) Math.pow(2.0,MMU.getPageAddressBits());
        // PageTableEntry[] 
        this.pages = new PageTableEntry[max_page];
        for(int i=0; i<max_page; i++)
        {
            pages[i] = new PageTableEntry(this,i);
        }
    }

    public void do_deallocateMemory()
    {
        System.out.println("[PageTable][do_deallocateMemory] this " + this.toString());
        int max_page = (int) Math.pow(2.0,MMU.getPageAddressBits());
        for(int i=0; i<max_page; i++)
        {
            // clear flags
            PageTableEntry page_table_entry = this.pages[i]; 
            FrameTableEntry frame_table_entry = page_table_entry.getFrame();
            if(frame_table_entry != null) // not isValid since it ignores reserved but not valid frames.
            {
                PageFaultHandler.FreeFrame(frame_table_entry);
                System.out.println("[PageTable][do_deallocateMemory]  page_table_entry " + page_table_entry.toString() + 
                                                                    " frame_table_entry " + frame_table_entry.toString());
                TaskCB owner_task = frame_table_entry.getReserved();
                if(owner_task != null)
                {
                    frame_table_entry.setUnreserved(owner_task);
                    System.out.println("[PageTable][do_deallocateMemory] owner_task " + owner_task.toString());   
                }
                     
            }
        }
        
    }

}


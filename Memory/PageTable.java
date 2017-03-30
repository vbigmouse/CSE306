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
        int max_bits=MMU.getPageAddressBits();
        //PageTableEntry[] 
        this.pages = new PageTableEntry[max_bits];
        for(int i=0;i<max_bits;i++)
        {
            pages[i] = new PageTableEntry(this,i);
        }
    }

    /**
       Frees up main memory occupied by the task.
       Then unreserves the freed pages, if necessary.

       @OSPProject Memory
    */
    public void do_deallocateMemory()
    {
        // your code goes here
        int max_bits = MMU.getPageAddressBits();
        for(int i=0; i<max_bits; i++)
        {
            // clear flags
            PageTableEntry page_table_entry = this.pages[i]; 
            FrameTableEntry frame_table_entry = page_table_entry.getFrame();
            if(frame_table_entry != null) // not isValid since it ignores reserved but not valid frames.
            {
                frame_table_entry.setReferenced(false);
                frame_table_entry.setDirty(false);
                frame_table_entry.setPage(null);
                TaskCB owner_task = frame_table_entry.getReserved();
                if(owner_task != null)
                    frame_table_entry.setUnreserved(owner_task);
            }
        }

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/

/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * I pledge my honor that all parts of this project were done by me   
 * individually and without collaboration with anybody else.
 * 
 *                                              109971346 Hung-Ruey Chen
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package osp.Memory;

import java.util.*;

import com.sun.org.apache.xml.internal.serializer.ElemDesc;

import osp.IFLModules.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.Utilities.*;
import osp.Hardware.*;
import osp.Interrupts.*;

/**
    The MMU class contains the student code that performs the work of
    handling a memory reference.  It is responsible for calling the
    interrupt handler if a page fault is required.

    @OSPProject Memory
*/
public class MMU extends IflMMU
{
    /** 
        This method is called once before the simulation starts. 
	Can be used to initialize the frame table and other static variables.

        @OSPProject Memory
    */
    public static void init()
    {
        // your code goes here
        frame_table_size = MMU.getFrameTableSize();
        frame_reference_queue = new LinkedList<>();
        for(int i=0; i<frame_table_size; i++)
        {
            setFrame(i,new FrameTableEntry(i));
            frame_reference_queue.add(i);
        }
        virtual_address_bits = MMU.getVirtualAddressBits();
        page_address_bits = MMU.getPageAddressBits();
        //PageFaultHandler.init();

    }

    /**
       This method handlies memory references. The method must 
       calculate, which memory page contains the memoryAddress,
       determine, whether the page is valid, start page fault 
       by making an interrupt if the page is invalid, finally, 
       if the page is still valid, i.e., not swapped out by another 
       thread while this thread was suspended, set its frame
       as referenced and then set it as dirty if necessary.
       (After pagefault, the thread will be placed on the ready queue, 
       and it is possible that some other thread will take away the frame.)
       
       @param memoryAddress A virtual memory address
       @param referenceType The type of memory reference to perform 
       @param thread that does the memory access
       (e.g., MemoryRead or MemoryWrite).
       @return The referenced page.

       @OSPProject Memory
    */
    static public PageTableEntry do_refer(int memoryAddress,
					  int referenceType, ThreadCB thread)
    {
        // calculate which page and offset.
        //int virtual_address_bits = MMU.getVirtualAddressBits();
        //int page_address_bits = MMU.getPageAddressBits();
        int offset_bits = virtual_address_bits - page_address_bits;
        int page_no = memoryAddress >>> offset_bits;
        int offset = memoryAddress & ~(page_no << offset_bits);
        
        System.out.println("[MMU][do_refer] address = " + memoryAddress + " offset = " + offset + " page no = " + page_no);
        PageTableEntry page = MMU.getPTBR().pages[page_no];

        // if not valid, do pagefault handling 
        if(!page.isValid())
        {
            if(page.getValidatingThread() == null)
            {
                // setInterruptType fields.
                System.out.println("[MMU][do_refer] setInterruptType");
                InterruptVector.setInterruptType(PageFault);
                InterruptVector.setThread(thread);
                InterruptVector.setPage(page);
                InterruptVector.setReferenceType(referenceType);
                CPU.interrupt(PageFault);
                System.out.println("[MMU][do_refer] return from interrupt");
            }
            else
                thread.suspend(page); // suspend for event page
        
        }

        // check if thread was killed
        if(thread.getStatus() == ThreadKill)
            return page;
        
        // set frame flags
        FrameTableEntry frame = page.getFrame();
        switch(referenceType)
        {
            case MemoryRead:
                System.out.println("[MMU][do_refer] Do memory read.");
                frame.setReferenced(true);
            break;
            case MemoryWrite:
                System.out.println("[MMU][do_refer] Do memory write.");
                frame.setReferenced(true);
                frame.setDirty(true);
            break;
            case MemoryLock:
                System.out.println("[MMU][do_refer] Do memory lock.");
                frame.setReferenced(true);
            break;
            default:
                System.out.println("[Error][MMU][do_refer] Invalid reference type!");
            break;
        }
        
        return page;
    }

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
     
	@OSPProject Memory
     */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.
     
      @OSPProject Memory
     */
    public static void atWarning()
    {
        // your code goes here

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */
    public static int frame_table_size;// = MMU.getFrameTableSize();
    public static int virtual_address_bits;// = MMU.getVirtualAddressBits();
    public static int page_address_bits;// = MMU.getPageAddressBits();
    public static List<Integer> frame_reference_queue;
}

/*
      Feel free to add local classes to improve the readability of your code
*/

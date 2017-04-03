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

public class MMU extends IflMMU
{
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

        Daemon.create("Page Clean Daemon" , new PageCleanDaemon(),20000);

    }

    static public PageTableEntry do_refer(int memoryAddress,
					  int referenceType, ThreadCB thread)
    {
        // calculate which page and offset.
        int offset_bits = virtual_address_bits - page_address_bits;
        int page_no = memoryAddress >>> offset_bits;
        int offset = memoryAddress & ~(page_no << offset_bits);
        
        System.out.println("[MMU][do_refer] address = " + memoryAddress + " offset = " + offset + " page no = " + page_no + " " + thread.toString());
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

    public static void atError()
    {
        // your code goes here

    }

    public static void atWarning()
    {
        // your code goes here

    }


    public static int frame_table_size;// = MMU.getFrameTableSize();
    public static int virtual_address_bits;// = MMU.getVirtualAddressBits();
    public static int page_address_bits;// = MMU.getPageAddressBits();
    public static List<Integer> frame_reference_queue;
}

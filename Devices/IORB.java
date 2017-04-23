/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * I pledge my honor that all parts of this project were done by me   
 * individually and without collaboration with anybody else.
 * 
 *                                              109971346 Hung-Ruey Chen
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
package osp.Devices;
import osp.IFLModules.*;
import osp.FileSys.OpenFile;
import osp.Threads.ThreadCB;
import osp.Memory.PageTableEntry;

public class IORB extends IflIORB
{

    public IORB(ThreadCB thread, PageTableEntry page, 
		int blockNumber, int deviceID, 
		int ioType, OpenFile openFile) {
        super(thread,page,blockNumber,deviceID,ioType,openFile);

    }

}



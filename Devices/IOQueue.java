/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * I pledge my honor that all parts of this project were done by me   
 * individually and without collaboration with anybody else.
 * 
 *                                              109971346 Hung-Ruey Chen
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
package osp.Devices;
import osp.IFLModules.*;
import osp.Threads.*;
import osp.Utilities.*;
import osp.Hardware.*;
import osp.Memory.*;
import osp.FileSys.*;
import osp.Tasks.*;
import java.util.*;


/*
    This IO Queue is devide into two part. The first part is from index 0 to current_ind-1.
    The second part is from current_ind to end of list. 
    First part schdules iorb for the next scan by calling addNextScan().
    Second part schdules iorb for current scan procedure by callings addCurrentScan().
    Pop the element at current index for the IO handler by calling dequeue_thread().
*/
public class IOQueue implements GenericQueueInterface
{

    public class IORBNode implements Comparable<IORBNode>
    {
        IORB iorb;
        int cylinder;
        public IORBNode(IORB io, int cy)
        {
            this.iorb=io;
            this.cylinder=cy;
        }
        //@Override
        public int compare(IORBNode N1, IORBNode N2)
        {
            return N1.cylinder-N2.cylinder;
        } 
        //@Override
        public int compareTo(IORBNode N2)
        {
            return this.cylinder-N2.cylinder;
        } 
    }

    public List<IORBNode> cylinder_list = new ArrayList<IORBNode>();
    public int current_ind = 0;
    @Override
    public int length()
    { 
        return cylinder_list.size();
    }
    @Override
    public boolean isEmpty()
    {
        return cylinder_list.isEmpty();
    }
    @Override
    public boolean contains(Object obj)
    {
        return cylinder_list.contains(obj);
    }
    public void insert(IORB io, int cy)
    {
        IORBNode iorb_node = new IORBNode(io,cy);
        this.cylinder_list.add(iorb_node);
    }
    public void insert(IORB io, int cy, int ind)
    {
        IORBNode iorb_node = new IORBNode(io,cy);
        this.cylinder_list.add(ind,iorb_node);
    }
    public void delete()
    {
        this.cylinder_list.remove(this.current_ind);
    }
    public void delete(int ind)
    {
        this.cylinder_list.remove(ind);
    }
    public String toString()
    {
        String s="";
        for(IORBNode t : cylinder_list)
        {
            s=s+t.iorb.toString()+"\n";
        }
        return s;
    }
    public IORB getIORB()
    {
        return this.cylinder_list.get(this.current_ind).iorb;
    }
    public IORB getIORB(int ind)
    {
        return (this.cylinder_list.get(ind)).iorb;
    }
    public Integer getCylinder(int ind)
    {
        return this.cylinder_list.get(ind).cylinder;
    }
    public List<IORBNode> getSub(int fIndex, int toIndex)
    {
        return this.cylinder_list.subList(fIndex, toIndex);
    }
    public void sortSub(int fIndex, int toIndex)
    {
        
        Collections.sort(this.cylinder_list.subList(fIndex, toIndex));
    }

    // schdule iorb to next scan procedure
    public void addNextScan(IORB iorb, int cy)
    {   

        IORBNode obj = new IORBNode(iorb, cy);
        boolean addlast=true;
        for(int i=0;i<current_ind;i++)
        {
            IORBNode t=cylinder_list.get(i);
            if(t.cylinder>cy)
            {
                cylinder_list.add(i, obj);
                addlast=false;
                break;
            }
        }

        if (addlast)  //found suitable position
            cylinder_list.add(current_ind, obj);
        current_ind++;
    }

    // schdule iorb to current scan procedure
    public void addCurrentScan(IORB iorb, int cy)
    {
        // schdule to this scan
        IORBNode obj = new IORBNode(iorb, cy);
        boolean addlast=true;
        for(IORBNode t:cylinder_list.subList(current_ind, cylinder_list.size()))
        {
            if(t.compareTo(obj)>0)
            {
                cylinder_list.add(cylinder_list.indexOf(t), obj);
                addlast=false;
                break;
            }
        }

        if (addlast)  // found suitable position
            cylinder_list.add(obj);
    }

    // pop-out next iorb
    public void dequeue_thread(ThreadCB thread)
    {
        for(int i=cylinder_list.size()-1;i>=0;i--)
        {
            IORBNode t=cylinder_list.get(i);
            if(t.iorb.getThread()==thread)
            {
                t.iorb.getPage().unlock();
                t.iorb.getOpenFile().decrementIORBCount();
                if(t.iorb.getOpenFile().closePending==true && t.iorb.getOpenFile().getIORBCount()==0)
                    t.iorb.getOpenFile().close();
                if(i<current_ind) // remove iorb in next scan, current ind-1
                    current_ind--;
                cylinder_list.remove(t);
            }
        }
    }
}
    

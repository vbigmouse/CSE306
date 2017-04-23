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

import javax.net.ssl.ExtendedSSLSession;


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
    //private LinkedList<E> list = new LinkedList<E>();
    public List<IORBNode> cylinder_list = new ArrayList<IORBNode>();
    public int current_ind = 0;
    public int current_pos = 0;
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
    public void show()
    {
        System.out.println("class show :"+this.getClass().toString());
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
    public void delete(IORBNode iorb_node)
    {
        this.cylinder_list.remove(iorb_node);
    }
    public void delete(int ind)
    {
        this.cylinder_list.remove(ind);
    }
    public String toString()
    {
        return this.cylinder_list.toString();
    }
    public IORBNode getIORBNode()
    {
        return this.cylinder_list.get(this.current_ind);
    }
    public IORBNode getIORBNode(int ind)
    {
        return this.cylinder_list.get(ind);
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
    public void addNextScan(IORB iorb, int cy)
    {   
        IORBNode obj = new IORBNode(iorb, cy);
        for (IORBNode t:cylinder_list.subList(0, current_ind))
        {
            if(t.compareTo(obj)>0)
            {
                cylinder_list.add(cylinder_list.indexOf(t), obj);
                current_ind= -1 * current_ind;
                break;
            }
        }

        if (current_ind<0)  //found suitable position
            current_ind = -1 * current_ind + 1;
        else    // no suitable position or empty next queue, add to current index
        {
            cylinder_list.add(current_ind, obj);
            current_ind++;
        }   
    }
    public void addCurrentScan(IORB iorb, int cy)
    {
        IORBNode obj = new IORBNode(iorb, cy);
        for(IORBNode t:cylinder_list.subList(current_ind, cylinder_list.size()))
        {
            if(t.compareTo(obj)>0)
            {
                cylinder_list.add(cylinder_list.indexOf(t), obj);
                current_ind= -1 * current_ind;
                break;
            }
        }

        if (current_ind<0)  // found suitable position
            current_ind = -1 * current_ind;
        else                // current queue empty or no suitable position. add to last.
            cylinder_list.add(obj);
    }
    public void dequeue_thread(ThreadCB thread)
    {
        for(IORBNode t:cylinder_list)
        {
            if(t.iorb.getThread()==thread)
            {
                t.iorb.getPage().unlock();
                t.iorb.getOpenFile().decrementIORBCount();
                if(!t.iorb.getOpenFile().closePending)
                    t.iorb.getOpenFile().closePending=true;
                if(t.iorb.getOpenFile().getIORBCount()==0)
                    t.iorb.getOpenFile().close();
                
                cylinder_list.remove(t);
            }
        }
    }
}
    

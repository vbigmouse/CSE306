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

public class IOQueue<Integer> implements GenericQueueInterface
{
    //private LinkedList<E> list = new LinkedList<E>();
    private List<Integer> list = new ArrayList<Integer>();
/*    public IOQueue()
    {
        System.out.println("IOQueue.init");
        this.list = new LinkedList<E>();
    }*/
    @Override
    public int length()
    { 
        return list.size();
    }
    @Override
    public boolean isEmpty()
    {
        return list.isEmpty();
    }
    @Override
    public boolean contains(Object obj)
    {
        return list.contains(obj);
    }
    public void insert(Integer obj)
    {
        this.list.add(obj);
    }
    public void delete(Integer ind)
    {
        list.remove(ind);
    }
    public String toString()
    {
        return list.toString();
    }
    public Integer getValue(int ind)
    {
        return list.get(ind);
    }
    public List<Integer> getSub(int fIndex, int toIndex)
    {
        return list.subList(fIndex, toIndex);
    }
    
}
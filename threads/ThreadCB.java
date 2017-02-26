package osp.Threads;
//import java.util.Vector;
//import java.util.Enumeration;
import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Tasks.*;
import osp.EventEngine.*;
import osp.Hardware.*;
import osp.Devices.*;
import osp.Memory.*;
import osp.Resources.*;
import java.util.*;



/**
   This class is responsible for actions related to threads, including
   creating, killing, dispatching, resuming, and suspending threads.

   @OSPProject Threads
*/
public class ThreadCB extends IflThreadCB 
{
    /**
       The thread constructor. Must call 

       	   super();

       as its first statement.

       @OSPProject Threads
    */
    public ThreadCB()
    {
        super(); // for IflThreadCB constructor parameter;
        this.TimeOnCPU = 0;
        this.LastTimeOnCPU = 0;
        this.CreationTime = 0;
        this.Priority = 0;
        //this.Status = ThreadState.ThreadReady;
    }

    /**
       This method will be called once at the beginning of the
       simulation. The student can set up static variables here.
       
       @OSPProject Threads
    */
    public static void init()
    {
        // your code goes here 
        ThreadCB.ReadyQueue = new HashSet<ThreadCB>();
        ThreadCB.WaitQueue = new HashSet<ThreadCB>();
    }

    /** 
        Sets up a new thread and adds it to the given task. 
        The method must set the ready status 
        and attempt to add thread to task. If the latter fails 
        because there are already too many threads in this task, 
        so does this method, otherwise, the thread is appended 
        to the ready queue and dispatch() is called.

	The priority of the thread can be set using the getPriority/setPriority
	methods. However, OSP itself doesn't care what the actual value of
	the priority is. These methods are just provided in case priority
	scheduling is required.

	@return thread or null

        @OSPProject Threads
    */
    static public ThreadCB do_create(TaskCB task)
    {
        System.out.println("Maximum Threads in Task " + ThreadCB.MaxThreadsPerTask + 
                            " Current Threads in Task " + task.getThreadCount() + " TR:" + ThreadRunning );
                            
        if (ThreadCB.MaxThreadsPerTask<=task.getThreadCount())
            return null;

        ThreadCB newThread = new ThreadCB();
        
        if (task.addThread(newThread) == FAILURE) 
            return null;
        
        newThread.setTask(task);
        newThread.setStatus(ThreadReady);

        newThread.CreationTime = newThread.getCreationTime();
        newThread.TimeOnCPU = newThread.getTimeOnCPU();

        newThread.Priority = (int) newThread.TimeOnCPU;
        newThread.setPriority(newThread.Priority);

        ThreadCB.ReadyQueue.add(newThread); // add to read queue
        PreemptThread(null, null, ThreadReady);
        ThreadCB.dispatch();             // new thread has highest priority, call dispatch()
        return newThread;
    }

    /** 
	Kills the specified thread. 

	The status must be set to ThreadKill, the thread must be
	removed from the task's list of threads and its pending IORBs
	must be purged from all device queues.
        
	If some thread was on the ready queue, it must removed, if the 
	thread was running, the processor becomes idle, and dispatch() 
	must be called to resume a waiting thread.
	
	@OSPProject Threads
    */
    public void do_kill()
    {
        System.out.println("do_kill " + this);
        int CurrentStatus = this.getStatus();
        if(CurrentStatus == ThreadRunning )
        {
            PreemptThread(this.getTask(), this, ThreadKill);
        }
        else if(CurrentStatus == ThreadReady)
        {
            this.setStatus(ThreadKill);
            ReadyQueue.remove(this);
        }
        else if(CurrentStatus >= ThreadWaiting)
        {
            this.setStatus(ThreadKill);
            WaitQueue.remove(this);
            for(int i = 0; i<Device.getTableSize();i++)
                Device.get(i).cancelPendingIO(this);
        }

        ResourceCB.giveupResources(this);
        if(this.getTask().getThreadCount() == 0)
            this.getTask().kill();
        this.dispatch();


    }

    /** Suspends the thread that is currenly on the processor on the 
        specified event. 

        Note that the thread being suspended doesn't need to be
        running. It can also be waiting for completion of a pagefault
        and be suspended on the IORB that is bringing the page in.
	
	Thread's status must be changed to ThreadWaiting or higher,
        the processor set to idle, the thread must be in the right
        waiting queue, and dispatch() must be called to give CPU
        control to some other thread.

	@param event - event on which to suspend this thread.

        @OSPProject Threads
    */
    public void do_suspend(Event event)
    {   
        /* 
            set current running thread to wait and 
            PTBR, running null. All the event waiting queue
            Waiting level + 1. suspend() will be called automaticly, 
            so just call addThread(this) and correct setStatus 
        */
        System.out.println("do_suspend");
        for (ThreadCB s : ReadyQueue) System.out.println(s);
        event.addThread(this);
        int CurrentStatus = this.getStatus();
        if( CurrentStatus == ThreadRunning ) // suspend running thread
        {
            PreemptThread(this.getTask(),this,ThreadWaiting);
            ThreadCB.dispatch();
        }
        else if (CurrentStatus >= ThreadWaiting )
            this.setStatus(CurrentStatus+1);
    }

    /** Resumes the thread.
        
	Only a thread with the status ThreadWaiting or higher
	can be resumed.  The status must be set to ThreadReady or
	decremented, respectively.
	A ready thread should be placed on the ready queue.
	
	@OSPProject Threads
    */
    public void do_resume()
    {
        System.out.println("do_resume" + this);
        int CurrentState = this.getStatus();
        if(CurrentState == ThreadWaiting)  // resume to ReadyQueue
        {
            this.setStatus(ThreadReady);
            ReadyQueue.add(this);
            WaitQueue.remove(this);
        }
        else
        {
            this.setStatus(CurrentState - 1);
        }
        PreemptThread(null,null,ThreadReady);
        ThreadCB.dispatch();
    }

    /** 
        Selects a thread from the run queue and dispatches it. 

        If there is just one theread ready to run, reschedule the thread 
        currently on the processor.

        In addition to setting the correct thread status it must
        update the PTBR.
	
	@return SUCCESS or FAILURE

        @OSPProject Threads
    */
    public static int do_dispatch()
    {
        // pick highest priority from ready queue or running thread.
        System.out.println("do_dispatch");
        //PreempThread(null,null,null);
        DispatchThread();
        System.out.println("do_dispatch finish");
        return SUCCESS;
    }

    /**
       Called by OSP after printing an error message. The student can
       insert code here to print various tables and data structures in
       their state just after the error happened.  The body can be
       left empty, if this feature is not used.

       @OSPProject Threads
    */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
        can insert code here to print various tables and data
        structures in their state just after the warning happened.
        The body can be left empty, if this feature is not used.
       
        @OSPProject Threads
     */
    public static void atWarning()
    {
        // your code goes here

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */
    private long TimeOnCPU;
    private long LastTimeOnCPU;    // for counting time on cpu limit
    private long CreationTime;
    private int Priority;
    //private ThreadState Status; 

    public static Set<ThreadCB> ReadyQueue;
    public static Set<ThreadCB> WaitQueue;

    public static int PreemptThread(TaskCB CurrentTask, ThreadCB CurrentThread, int Case)
    {
        System.out.println("[Preempt Phase ]" + Case);
        if(CurrentThread == null)
        {
            PageTable CurrentPTBR;        
            if ( (CurrentPTBR = MMU.getPTBR()) == null )
            {
                System.out.println("No task is running, dispatch from ReadyQueue! ");
                return SUCCESS;
            }
            CurrentTask = CurrentPTBR.getTask();
            CurrentThread = CurrentTask.getCurrentThread();
        }
        
        long TotalTimeOnCPU = CurrentThread.getTimeOnCPU();
        System.out.println("TotalTimeOnCPU "+TotalTimeOnCPU +"- TimeOnCPU " +CurrentThread.TimeOnCPU);
        if(Case == ThreadReady)
        {
            // stop by exceeding time limit, put to readyqueue
            CurrentThread.setStatus(ThreadReady);
            System.out.println(CurrentThread+" Set status to ThreadReady ");
            ReadyQueue.add(CurrentThread);
        }
        else if (Case == ThreadWaiting)
        {
            // stop by event, put to waitqueue
            CurrentThread.setStatus(ThreadWaiting);
            System.out.println(CurrentThread+" Set status to ThreadWaiting ");
            WaitQueue.add(CurrentThread);
        }
        else if(Case == ThreadKill)
        {
            CurrentThread.setStatus(ThreadKill);
            System.out.println(CurrentThread+" Set status to ThreadKill ");
        }

        MMU.setPTBR(null); 
        CurrentTask.setCurrentThread(null);

        CurrentThread.TimeOnCPU = TotalTimeOnCPU; // update total time 
        CurrentThread.Priority = (int) CurrentThread.TimeOnCPU;
        CurrentThread.setPriority(CurrentThread.Priority);        

        return SUCCESS;
    }

    public static int DispatchThread()
    {
        System.out.println("[Dispatch Phase ]");
        if(ReadyQueue.isEmpty()) // go to idle
        {
            System.out.println("[DispatchThread] ReadyQueue Empty");
            return SUCCESS;
        }
        ThreadCB PriorThread = ReadyQueue.stream().min((t1, t2)->Integer.compare(t1.Priority,t2.Priority)).get();
        TaskCB PriorTask = PriorThread.getTask();
        System.out.println("DispatchThread "+PriorThread);
        PriorThread.setStatus(ThreadRunning);
        MMU.setPTBR(PriorTask.getPageTable());       //set PTBR to pagetable of prior thread
        PriorTask.setCurrentThread(PriorThread);
        System.out.println("Remove from ready queue"); 
        ReadyQueue.remove(PriorThread);
        return SUCCESS; 

    }
    
    /*public enum ThreadState
    {
        ThreadRunning(21),
        ThreadReady(20),
        ThreadWaiting(30),
        ThreadKill(22);

        private int value;

        ThreadState(int value)
        {
            this.value = value;
        }
        private int GetState()
        {
            return value;
        }
    }*/
}

/*
      Feel free to add local classes to improve the readability of your code
*/

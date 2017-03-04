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
        // initialize ready queue and waiting queue
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
        System.out.println("do_create");
        // check if task has maximum threads                    
        if (ThreadCB.MaxThreadsPerTask<=task.getThreadCount())
        {
            System.out.println("Maximum threads number in task: "+ task.getThreadCount() + "/" + ThreadCB.MaxThreadsPerTask  );
            ThreadCB.dispatch();
            return null;
        }

        ThreadCB newThread = new ThreadCB();
        
        if (task.addThread(newThread) == FAILURE) // create fail, just dispatch
        {
            ThreadCB.dispatch();
            return null;
        }
        
        newThread.setTask(task);
        newThread.setStatus(ThreadReady);

        newThread.CreationTime = newThread.getCreationTime();
        newThread.TimeOnCPU = newThread.getTimeOnCPU();

        newThread.Priority = (int) newThread.TimeOnCPU;
        newThread.setPriority(newThread.Priority);

        ThreadCB.ReadyQueue.add(newThread); // add to read queue
        //System.out.println("create "+newThread);
        ThreadCB.dispatch();             //  call dispatch()
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
        TaskCB CurrentTask = this.getTask();
        if(CurrentStatus == ThreadRunning )   // if thread to be killed is running, preempt it.
        {
            this.setStatus(ThreadKill);
            PreemptThread(CurrentTask, this);
        }
        else if(CurrentStatus == ThreadReady) // if thread to be killed is in ready queue, remove it.
        {
            this.setStatus(ThreadKill);
            ReadyQueue.remove(this);
        }
        else if(CurrentStatus >= ThreadWaiting) // if thread to be killed is in waiting queue, remove it.
        {
            this.setStatus(ThreadKill);
            WaitQueue.remove(this);
            for(int i = 0; i<Device.getTableSize();i++)
                Device.get(i).cancelPendingIO(this);
        }
        CurrentTask.removeThread(this);
        ResourceCB.giveupResources(this);
        if(CurrentTask.getThreadCount() == 0)
            CurrentTask.kill();
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
        System.out.println("do_suspend "+this);
        event.addThread(this);
        int CurrentStatus = this.getStatus();
        if( CurrentStatus == ThreadRunning ) // suspend running thread
        {
            this.setStatus(ThreadWaiting);
            //ThreadCB.dispatch();
            PreemptThread(this.getTask(),this);
        }
        else if (CurrentStatus >= ThreadWaiting )
            this.setStatus(CurrentStatus+1);
        ThreadCB.dispatch();
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
        System.out.println("do_resume " + this);
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
        //PreemptThread(null,null,ThreadReady);
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
        System.out.println("do_dispatch ");
        // check ready queue and waiting queue
        /*
        System.out.print("ReadyQueue:");
        for (ThreadCB s : ReadyQueue) System.out.println(s);
        System.out.print("WaitQueue:");
        for (ThreadCB s : WaitQueue) System.out.println(s);
        System.out.println("");
        */

        if(MMU.getPTBR() != null)  // if there is running thread, preempt it.
            PreemptThread(null,null);

        HTimer.set(100);  // set timer interrupt 100 time unit
        return DispatchThread();  // try to dispatch thread
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

    public static Set<ThreadCB> ReadyQueue;
    public static Set<ThreadCB> WaitQueue;

    public static int PreemptThread(TaskCB CurrentTask, ThreadCB CurrentThread)
    {
        System.out.println("[Preempt Phase ]");
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
        
        int Case = CurrentThread.getStatus();
        long TotalTimeOnCPU = CurrentThread.getTimeOnCPU();
        System.out.println("TotalTimeOnCPU "+TotalTimeOnCPU +"- TimeOnCPU " +CurrentThread.TimeOnCPU);
        // check the status of the thread to be preempt.
        if(Case == ThreadReady)
        {
            // if status is set to ready, add to ready queue
            System.out.println(CurrentThread+" Status to ThreadReady ");
            ReadyQueue.add(CurrentThread);
        }
        else if(Case >= ThreadWaiting)
        {
            // if status is set to waiting, add to waiting queue
            System.out.println(CurrentThread+" Status to ThreadWaiting ");
            WaitQueue.add(CurrentThread);
        }
        else if(Case == ThreadKill) 
        {
            // thread is killed
            System.out.println(CurrentThread+" Status to ThreadKill ");
        }
        else if(Case == ThreadRunning) 
        {
            // if thread status is still running, put to ready queue 
            System.out.println(CurrentThread+" Set status ThreadRunning to ThreadReady ");
            CurrentThread.setStatus(ThreadReady);
            ReadyQueue.add(CurrentThread);            
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
        if(ReadyQueue.isEmpty()) // idle
        {
            System.out.println("[DispatchThread] ReadyQueue Empty");
            return FAILURE;
        }
        /*
        System.out.print("ReadyQueue:");
        for (ThreadCB s : ReadyQueue) System.out.println(s);
        System.out.print("WaitQueue:");
        for (ThreadCB s : WaitQueue) System.out.println(s);
        System.out.println("");
        */
        // pick the highest priority from ready queue
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
    
}

/*
      Feel free to add local classes to improve the readability of your code
*/

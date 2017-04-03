========================================================================================================================
The statistics of Demo.jar:

	Device 0: Swap Device:	Number of pages read = 1243	Number of pages written = 296
	Device 1: Disk
:		Number of pages read = 30
	Number of pages written = 40
	Device 2: Disk:		Number of pages read = 6
	Number of pages written = 6
						Total= 1279			  Total = 342	
	CPU Utilization: 62.85%
  
	Average service time per thread: 31450.379
  
	Average normalized service time per thread: 0.06302221
========================================================================================================================
The statistics of My implementation (OSP.jar):

	Device 0: Swap Device
:	Number of pages read = 931
	Number of pages written = 467
	    
	Device 1: Disk
:		Number of pages read = 50
	Number of pages written = 35
	    
	Device 2: Disk:		Number of pages read = 37
	Number of pages written = 19
						Total= 1038			  Total = 521	
	CPU Utilization: 85.341606%
  
	Average service time per thread: 22710.303
  
	Average normalized service time per thread: 0.07051029
========================================================================================================================
With LRU page replacement algorithm, the number of pages read and average service time decreased.
The CPU utilization rised by around 22% since less I/O operation is needed.
The proactive page cleaning daemon caused the total pages written increased.
The average normalized service time increased since total turnaround time decreased. (normalized = turnaround time / total turnaround time)


/*******************************************************************************
 * Copyright 2012 Wim Vancroonenburg, Tony Wauters, CODeS research group, KAHO Sint-Lieven, Gent, Belgium
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package be.kahosl.roadef2012.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Assignment {
	
	// basic solution info
	private Problem problem;
	
	private final int[] initialAssignment;
	
	private int[] assignment;
	private Map<Integer,Set<Integer>> machineToProcMap; //[machine, list(processes)]
	
	//helper structures for efficient delta evaluation and constraint checking:
	
	//capacity constraint helper
	private long[][] usage; //[machineID;resourceID]
	private long[][] transientUsage; //[machineID;resourceID], when a process is moved this contains the transient usage it has on its orignal machine
	
	private int[][] neighborHoodCount; //[service,neighborhood], number of processes of a service that runs in a specific neighbourhood
	private int[][] locationCount; //[service,location], number of processes of a service that runs in a specific location
	private int[] serviceSpread; // [service], over how many locations a service is spread.
	
	private int[] serviceMoveCount; //[service]
	private int maxServiceMoveCount; // count of the service that was moved most
	private int serviceWithMaxMoveCount; // id of the service with with max move count
	
	private int[][] inverseDependencies;  // [serviceid] (list of services that depend on a specific service)
	
	private long[][] quadraticOverload; // [machine, resource]  (per machine, per resource, the quadratic overload (U_m,r - SC_m,r)²

	public Assignment(Problem problem, int[] initialAssignment) {
		this.problem = problem;
		this.initialAssignment = initialAssignment;
		this.assignment=Arrays.copyOf(initialAssignment, initialAssignment.length);
		
		initializeUsage();	
		initializeNeighborhoodCount(problem, initialAssignment);
		initializeLocationCount(problem, initialAssignment);
		initializeMachineToProcMap();
		serviceMoveCount=new int[problem.nrServices];
		initializeInverseDependencies(problem);
		initializeQuadraticOverload(problem);
		
	}

	public Assignment(Problem problem, int[] initialAssignment, int[] newAssignment) {
		this(problem,initialAssignment);
		
		//perform changes
		for (int i=0;i<assignment.length;i++){
			if (assignment[i]!=newAssignment[i]){
				doMove(i, newAssignment[i]);
			}
		}
		
	}
	
	public Assignment(Assignment ass){
		this(ass.getProblem(), ass.getInitialAssignment(), ass.getAssignment());
	}

	private void initializeQuadraticOverload(Problem problem) {
		quadraticOverload = new long[problem.nrMachines][problem.nrResources];
		
		for(int m = 0; m<problem.nrMachines; m++) {
			for(int r = 0; r<problem.nrResources; r++) {
				long temp = (usage[m][r]-problem.safetyCap[m][r]);
				quadraticOverload[m][r] = (temp*temp);
			}
		}
	}



	private void initializeMachineToProcMap() {
		machineToProcMap = new HashMap<Integer, Set<Integer>>();
		
		for(int m = 0; m<problem.nrMachines; m++) {
			machineToProcMap.put(m, new HashSet<Integer>());
		}
		
		for(int p = 0; p<problem.nrProcesses; p++) {
			int m = assignment[p];	
			machineToProcMap.get(m).add(p);
		}
	}
	private void initializeInverseDependencies(Problem problem) {
		inverseDependencies=new int[problem.nrServices][];
		HashMap<Integer, List<Integer>> inverseDependencyMap=new HashMap<Integer, List<Integer>>();
		for (int s=0;s<problem.nrServices;s++){
			for (int sDep:problem.serviceDependencies[s]){
				if (!inverseDependencyMap.containsKey(sDep)) inverseDependencyMap.put(sDep, new ArrayList<Integer>());
				inverseDependencyMap.get(sDep).add(s);
			}
		}
		for (int s=0;s<problem.nrServices;s++){
			if (inverseDependencyMap.containsKey(s)){
				inverseDependencies[s]=new int[inverseDependencyMap.get(s).size()];
			} else {
				inverseDependencies[s]=new int[0];
			}

			for (int j=0;j<inverseDependencies[s].length;j++){
				inverseDependencies[s][j]=inverseDependencyMap.get(s).get(j);
			}
		}
	}


	private void initializeNeighborhoodCount(Problem problem,
			int[] initialAssignment) {
		neighborHoodCount=new int[problem.nrServices][problem.nrNeighbourhoods];
		for (int s=0;s<problem.nrServices;s++){
			for (int p:problem.services[s]){
				int n=problem.machineNeighbourhoodMap[initialAssignment[p]];
				neighborHoodCount[s][n]++;
			}
		}
	}
	
	private void initializeLocationCount(Problem problem, int[]initialAssignment){
		locationCount=new int[problem.nrServices][problem.nrLocations];
		for (int s=0;s<problem.nrServices;s++){
			for (int p:problem.services[s]){
				int l=problem.machineLocationMap[initialAssignment[p]];
				locationCount[s][l]++;
			}
		}
		
		serviceSpread = new int[problem.nrServices];
		for (int s=0;s<problem.nrServices;s++){
			for (int l=0; l<problem.nrLocations; l++){
				if(locationCount[s][l] > 0) serviceSpread[s]++;
			}
		}
	}


	private void initializeUsage() {
		usage = new long[problem.nrMachines][problem.nrResources];
		
		// transientusage is initially 0 everywhere, because no process is moved
        transientUsage = new long[problem.nrMachines][problem.nrResources];
        
        for(int p = 0; p<problem.nrProcesses; p++) {
            
            int m = initialAssignment[p];
            
            for(int r = 0; r<problem.nrResources; r++) {
                usage[m][r] += problem.processReq[p][r]; 
            }
        }
	}
	
	/**
	 * Evaluates the delta of a single process machine reassignment
	 * @param process
	 * @param machine
	 * @return
	 */
	public long evaluateDelta(int process, int machine){
		long delta = 0;
		
		
		int service=problem.processServiceMap[process];
		int prevMachine=assignment[process];
		
		//load cost	
		long deltaLoadPrevMachine;
		long deltaLoadNewMachine;
		long deltaLoadResource;
		for(int r=0;r<problem.nrResources;r++){
			deltaLoadPrevMachine = Math.max(0, (usage[prevMachine][r]-problem.processReq[process][r])-problem.safetyCap[prevMachine][r]) - Math.max(0, usage[prevMachine][r]-problem.safetyCap[prevMachine][r]);
			deltaLoadNewMachine = Math.max(0, (usage[machine][r]+problem.processReq[process][r])-problem.safetyCap[machine][r]) - Math.max(0, usage[machine][r]-problem.safetyCap[machine][r]);
			deltaLoadResource=deltaLoadPrevMachine+deltaLoadNewMachine;
			delta+=deltaLoadResource*problem.resourceLoadCostWeight[r];
		}

		//balance cost
		for(int b = 0; b<problem.nrBalanceObj; b++) {
          int r1 = problem.balanceObj[b][0];
          int r2 = problem.balanceObj[b][1];
          int target = problem.balanceObj[b][2];
          
          long bCostDeltaPrevMachine = Math.max(0, target*(problem.cap[prevMachine][r1]-(usage[prevMachine][r1]-problem.processReq[process][r1])) - (problem.cap[prevMachine][r2]-(usage[prevMachine][r2]-problem.processReq[process][r2]))) 
        		  - Math.max(0, target*(problem.cap[prevMachine][r1]-(usage[prevMachine][r1])) - (problem.cap[prevMachine][r2]-(usage[prevMachine][r2])));
          
          long bCostDeltaNewMachine = Math.max(0, target*(problem.cap[machine][r1]-(usage[machine][r1]+problem.processReq[process][r1])) - (problem.cap[machine][r2]-(usage[machine][r2]+problem.processReq[process][r2]))) 
        		  - Math.max(0, target*(problem.cap[machine][r1]-(usage[machine][r1])) - (problem.cap[machine][r2]-(usage[machine][r2])));
          
          delta += (bCostDeltaNewMachine + bCostDeltaPrevMachine) * problem.balanceObjWeight[b];
		}

		//process move cost
		if (prevMachine==initialAssignment[process] && machine !=initialAssignment[process]){
			delta+= problem.processMoveCost[process]*problem.processMoveCostWeight;
		} else if (prevMachine!=initialAssignment[process] && machine==initialAssignment[process]){
			delta-= problem.processMoveCost[process]*problem.processMoveCostWeight;
		}

		//service move cost
		if (prevMachine==initialAssignment[process] && machine!=initialAssignment[process]){

			if (serviceMoveCount[service]+1>maxServiceMoveCount){
				delta+=problem.serviceMoveCostWeight;
			}
		} else if (prevMachine!=initialAssignment[process] && machine==initialAssignment[process]){			
			if (service==serviceWithMaxMoveCount){
				int maxCount=0;
				for (int s=0;s<problem.nrServices;s++){
					if (service!=s){
						if (serviceMoveCount[s]>maxCount){
							maxCount=serviceMoveCount[s];
						}
					}
				}
				if (maxCount<serviceMoveCount[service]){
					delta-=problem.serviceMoveCostWeight;
				}
			}
		}
		
		//machine move cost	
		delta+= problem.machineMoveCostWeight * (problem.machineMoveCost[initialAssignment[process]][machine] - problem.machineMoveCost[initialAssignment[process]][prevMachine]);
		
		return delta;
	}
	
	/**
	 * Performs a single reassignment of a process. Updates all maps for the change
	 * @param process
	 * @param machine
	 */
	public void doMove(int process, int machine){
		int previousMachine=assignment[process];
		assignment[process]=machine;
		
		// hold the usage on the previous machine before the change (necessary for quadratic overload delta update)
		long[] previousMachineOldUsage = Arrays.copyOf(usage[previousMachine], problem.nrResources);
		
		// hold the usage on the new machine before the change (necessary for quadratic overload delta update)
		long[] newMachineOldUsage = Arrays.copyOf(usage[machine], problem.nrResources);
		
		
		// update usage and transient usage
		for (int r=0;r<problem.nrResources;r++){   
		    if(problem.transientResourceMap[r]) {
		        if(initialAssignment[process]==previousMachine) {
		            transientUsage[previousMachine][r] += problem.processReq[process][r];
		        }
		        if(initialAssignment[process]==machine){
		            transientUsage[machine][r] -= problem.processReq[process][r];
		        }
		    }
			usage[previousMachine][r]-=problem.processReq[process][r];
			usage[machine][r]+=problem.processReq[process][r];
		}
		
		//update neighborhood count
		int service=problem.processServiceMap[process];
		int nbefore=problem.machineNeighbourhoodMap[previousMachine];
		int nNew=problem.machineNeighbourhoodMap[machine];
		if (nbefore!=nNew){
			neighborHoodCount[service][nbefore]--;
			neighborHoodCount[service][nNew]++;
		}
		
		//update location count
		int lbefore =problem.machineLocationMap[previousMachine];
		int lNew=problem.machineLocationMap[machine];
		if (lbefore!=lNew){
			if(locationCount[service][lbefore] == 1) serviceSpread[service]--;
			if(locationCount[service][lNew] == 0) serviceSpread[service]++;

			locationCount[service][lbefore]--;
			locationCount[service][lNew]++;
		}
		
		//update service move count
		if (previousMachine==initialAssignment[process] && machine!=initialAssignment[process]){
			serviceMoveCount[service]++;
			if (serviceMoveCount[service]>maxServiceMoveCount){
				maxServiceMoveCount=serviceMoveCount[service];
				serviceWithMaxMoveCount=service;			
			}
		} else if (previousMachine!=initialAssignment[process] && machine==initialAssignment[process]){
			serviceMoveCount[service]--;
			if (service==serviceWithMaxMoveCount){
				maxServiceMoveCount=serviceMoveCount[service];
				int maxCount=-1;
				int maxService=-1;
				for (int s=0;s<problem.nrServices;s++){
					if (service!=s){
						if (serviceMoveCount[s]>maxCount){
							maxCount=serviceMoveCount[s];
							maxService=s;
						}
					}
				}
				if (maxCount>serviceMoveCount[service]){
					serviceWithMaxMoveCount=maxService;
					maxServiceMoveCount=maxCount;
				}
			}
		}
		
		//update machinemap
		machineToProcMap.get(previousMachine).remove(process);
		machineToProcMap.get(machine).add(process);
		
		//update quadraticoverload
		for(int r = 0; r<problem.nrResources; r++) {
			long delta = problem.processReq[process][r];
			quadraticOverload[previousMachine][r] += (-delta)*(2*(previousMachineOldUsage[r]-problem.safetyCap[previousMachine][r]) - delta);
			quadraticOverload[machine][r] += delta*(2*(newMachineOldUsage[r]-problem.safetyCap[machine][r]) + delta);
		}
		
	}
	
	public boolean tryMachineByCapacity(int process, int machine){
		for (int r=0;r<problem.nrResources;r++){
			if (initialAssignment[process]==machine && problem.transientResourceMap[r]) continue;
			if(usage[machine][r]+transientUsage[machine][r]+problem.processReq[process][r]>problem.cap[machine][r]) return false;		
		}
		return true;
	}
	
	public boolean tryMachineByConflict(int process, int machine){
		int service=problem.processServiceMap[process];
		for (int p:problem.services[service]){
			if (p!=process){
				if (assignment[p]==machine) return false;
			}
		}
		
		return true;
	}
	
	public boolean tryMachineBySpread(int process, int machine){
		int service=problem.processServiceMap[process];
		
		int lbefore =problem.machineLocationMap[assignment[process]];
		int lNew=problem.machineLocationMap[machine];
		
		if (lbefore==lNew) return true;
		
		int deltaCount=0;
		if (locationCount[service][lbefore]==1) deltaCount--;
		
		if (locationCount[service][lNew] == 0){
			deltaCount++;
		}
		
        if(serviceSpread[service]+deltaCount < problem.serviceSpreadMin[service]) return false;
        return true;
	}
	
	public boolean tryMachineByDependency(int process, int machine){
		int service=problem.processServiceMap[process];
		
		
		int nPrev = problem.machineNeighbourhoodMap[assignment[process]];
		int nNew = problem.machineNeighbourhoodMap[machine];
		
		if (nPrev==nNew) return true;
		

		// check forward dependency
	    for (int sDep : problem.serviceDependencies[service]) {
	    	if(neighborHoodCount[sDep][nNew] == 0) return false;
	    }
		
        //check inverse dependency
        if (neighborHoodCount[service][nPrev] > 1) return true;
        for (int sInvDep: inverseDependencies[service]){
        	for (int pa : problem.services[sInvDep]){
        		int na = problem.machineNeighbourhoodMap[assignment[pa]];
        		if (nPrev==na){
        			return false;
        		}
        	}
        }
        
		return true;
	}

	// multi process reassignemnt, delta evaluation, domove and feasibility checks
	public long evaluateDelta(int[] processes, int[] machines){
		long delta=0;
		
		int[] oldMachines=new int[machines.length];
		for (int i = 0; i < oldMachines.length; i++) {
			oldMachines[i]=assignment[processes[i]];
		}
		for (int i = 0; i < machines.length; i++) {
			delta+=evaluateDelta(processes[i], machines[i]);
			if (i!=machines.length-1) doMove(processes[i], machines[i]);
		}
		for (int i = 0; i < machines.length-1; i++) {
			doMove(processes[i], oldMachines[i]);
		}
		
		return delta;
	}
	
	public void doMove(int[] processes, int[] machines){
		for (int i = 0; i < processes.length; i++) {
			doMove(processes[i], machines[i]);
		}
	}
	
	public boolean tryMachineByCapacity(int[] processes, int[] machines){
		
		HashMap<Integer, Set<Integer>> machineToNewProcessesMap=new HashMap<Integer, Set<Integer>>();
		HashMap<Integer, Set<Integer>> machineToLeavingProcessesMap=new HashMap<Integer, Set<Integer>>();
		for (int i = 0; i < processes.length; i++) {
			int machine = machines[i];
			
			int process=processes[i];
			int prevMachine = assignment[process];
			
			if (!machineToNewProcessesMap.containsKey(machine)) machineToNewProcessesMap.put(machine, new HashSet<Integer>());
			machineToNewProcessesMap.get(machine).add(process);
			if (!machineToLeavingProcessesMap.containsKey(prevMachine)) machineToLeavingProcessesMap.put(prevMachine, new HashSet<Integer>());
			machineToLeavingProcessesMap.get(prevMachine).add(process);
		}
		for (int machine:machineToNewProcessesMap.keySet()){
			for (int r=0;r<problem.nrResources;r++){
				long sumOfRequests=0;
				for (int process:machineToNewProcessesMap.get(machine)){
					if (initialAssignment[process]==machine && problem.transientResourceMap[r]) continue;
					sumOfRequests+=problem.processReq[process][r];
				}
				if (machineToLeavingProcessesMap.containsKey(machine)){
					for(int process:machineToLeavingProcessesMap.get(machine)) {
						if (initialAssignment[process]==machine && problem.transientResourceMap[r]) continue;
						sumOfRequests-=problem.processReq[process][r];
					}
				}
				
				if(usage[machine][r]+transientUsage[machine][r]+sumOfRequests>problem.cap[machine][r]) return false;		
			}
		}

		return true;
	}
	
	
	public boolean tryMachineByConflict(int[] processes, int[] machines){
		HashMap<Integer,Integer> changingProcesses=new HashMap<Integer,Integer>(processes.length);
		for (int i = 0; i < machines.length; i++) {
			changingProcesses.put(processes[i], machines[i]);
		}
		
		for (int i = 0; i < processes.length; i++) {
			int process=processes[i];
			int machine=machines[i];
			int service=problem.processServiceMap[process];
			for (int p:problem.services[service]){
				if (p!=process ){
					if (!changingProcesses.containsKey(p)){
						if (assignment[p]==machine) return false;
					} else {
						if (machine==changingProcesses.get(p)) return false;
					}
				}
			}
		}
		return true;
	}
	
	public boolean tryMachineBySpread(int[] processes, int[] machines){
		
		HashMap<Integer, HashMap<Integer,Integer>> serviceToProcessMap=new HashMap<Integer, HashMap<Integer,Integer>>();
		for (int i = 0; i < processes.length; i++) {
			int service=problem.processServiceMap[processes[i]];
			if (!serviceToProcessMap.containsKey(service)) serviceToProcessMap.put(service, new HashMap<Integer,Integer>());
			serviceToProcessMap.get(service).put(processes[i],i);
		}
		
		for (int service:serviceToProcessMap.keySet()){
	        Set<Integer> locations = new HashSet<Integer>();
	        
	        for(int p : problem.services[service]) {
	        	if (!serviceToProcessMap.get(service).containsKey(p)){
	                int l = problem.machineLocationMap[assignment[p]];             
	                locations.add(l);
	        	} else {
	        		int machine = machines[serviceToProcessMap.get(service).get(p)];
	                int l = problem.machineLocationMap[machine];             
	                locations.add(l);
	        	}

	        }
	        if(locations.size() < problem.serviceSpreadMin[service]) return false;
		}
		

        return true;
	}
	
	public boolean tryMachineByDependency(int[] processes, int[] machines){
		
		for (int i = 0; i < processes.length; i++) {
			int process=processes[i];
			int machine=machines[i];
			int service=problem.processServiceMap[process];
			
			
			int nPrev = problem.machineNeighbourhoodMap[assignment[process]];
			int nNew = problem.machineNeighbourhoodMap[machine];
			
			if (nPrev==nNew) continue;
			

			// check forward dependency
		    for (int sDep : problem.serviceDependencies[service]) {
		    	int[] neighbourhoods = Arrays.copyOf(neighborHoodCount[sDep],neighborHoodCount[sDep].length);   
		    	for (int j = 0; j < processes.length; j++) {
					if (j!=i){
						if (problem.processServiceMap[processes[j]]==sDep){
							int nPrevJ=problem.machineNeighbourhoodMap[assignment[processes[j]]];
							int nNewJ=problem.machineNeighbourhoodMap[machines[j]];
							if (nPrevJ==nNewJ) continue;
							neighbourhoods[nPrevJ]--;
							neighbourhoods[nNewJ]++;
						}
					}
				}
		    	if (neighbourhoods[nNew] == 0) 
		    		return false;
		    }
			
	        //check inverse dependency
		    int prevNDecrease=1;
		    for (int j = 0; j < processes.length; j++) {
				if (i!=j){
					int nPrevJ=problem.machineNeighbourhoodMap[assignment[processes[j]]];
					int nNewJ=problem.machineNeighbourhoodMap[machines[j]];
					if (nPrevJ==nPrev && nPrevJ!=nNewJ && service==problem.processServiceMap[processes[j]]) prevNDecrease++;
					if (nPrevJ != nNewJ && nNewJ == nPrev && service==problem.processServiceMap[processes[j]]) prevNDecrease--;
				}
			}
	        if (neighborHoodCount[service][nPrev]-prevNDecrease>0) continue;
	        for (int sInvDep: inverseDependencies[service]){
	        	for (int pa : problem.services[sInvDep]){
	        		int na = problem.machineNeighbourhoodMap[assignment[pa]];
	        		int index = -1;
	        		if((index = contains(processes, pa)) != -1) {
	        			na = problem.machineNeighbourhoodMap[machines[index]];
	        		}
	        		if (nPrev==na){
	        			return false;
	        		}
	        	}
	        }
		}
        
		return true;
	}
	
	
	
	private int contains(int[] processes, int p) {
		for(int i = 0; i<processes.length; i++) {
			if(processes[i] == p)
				return i;
		}
		return -1;
	}
	
	
	// SPECIAL SWAP DELTA CHECKS
	public boolean tryMachineByCapacitySwap(int[] processes, int[] machines){
		
		for (int m=0;m<2;m++){
			int machine=machines[m];
			for (int r=0;r<problem.nrResources;r++){
				long sumOfRequests=0;
				int process=processes[m];
					if ( !(initialAssignment[process]==machine && problem.transientResourceMap[r])){
						sumOfRequests+=problem.processReq[process][r];
					}

					process=processes[m==0?1:0];
					
						if ( !(initialAssignment[process]==machine && problem.transientResourceMap[r])) {
							sumOfRequests-=problem.processReq[process][r];
						}
					
				
				
				if(usage[machine][r]+transientUsage[machine][r]+sumOfRequests>problem.cap[machine][r]) return false;		
			}
		}

		return true;
	}
	
	public boolean tryMachineByConflictSwap(int[] processes, int[] machines){
		int p1=processes[0];
		int p2=processes[1];
		int s1=problem.processServiceMap[p1];
		int s2=problem.processServiceMap[p2];
		if (s1==s2) return true;
		
		return tryMachineByConflict(p1, machines[0]) && tryMachineByConflict(p2, machines[1]);
	}
	
	public boolean tryMachineByDependencySwap(int[] processes, int[] machines){
		int p1=processes[0];
		int p2=processes[1];
		int s1=problem.processServiceMap[p1];
		int s2=problem.processServiceMap[p2];
		if (s1==s2) return true; //feasible if both swapped processes belong to the same service
		
		int m1new = machines[0];
		int m2new = machines[1];
		
		int n1new =  problem.machineNeighbourhoodMap[m1new];
		int n2new =  problem.machineNeighbourhoodMap[m2new];
		if (n1new==n2new) return true; //feasible if both swapped machines belong to the same neighbourhood
		
		boolean s1DependsOns2=false;
		for (int sDep : problem.serviceDependencies[s1]) {
			if (sDep==s2) {
				s1DependsOns2=true;
				break;
			}
		}
		boolean s2DependsOns1=false;
		for (int sDep : problem.serviceDependencies[s2]) {
			if (sDep==s1) {
				s2DependsOns1=true;
				break;
			}
		}
		if (!s1DependsOns2 && !s2DependsOns1){
			return tryMachineByDependency(p1, m1new) && tryMachineByDependency(p2, m2new);
		}
		
		
		
		for (int i = 0; i < processes.length; i++) {
			int process=processes[i];
			int machine=machines[i];
			int service=problem.processServiceMap[process];
			
			
			int nPrev = problem.machineNeighbourhoodMap[assignment[process]];
			int nNew = problem.machineNeighbourhoodMap[machine];
			
			if (nPrev==nNew) continue;
			

			// check forward dependency
		    for (int sDep : problem.serviceDependencies[service]) {
		    	int[] neighbourhoods = Arrays.copyOf(neighborHoodCount[sDep],neighborHoodCount[sDep].length);   
		    	for (int j = 0; j < processes.length; j++) {
					if (j!=i){
						if (problem.processServiceMap[processes[j]]==sDep){
							int nPrevJ=problem.machineNeighbourhoodMap[assignment[processes[j]]];
							int nNewJ=problem.machineNeighbourhoodMap[machines[j]];
							if (nPrevJ==nNewJ) continue;
							neighbourhoods[nPrevJ]--;
							neighbourhoods[nNewJ]++;
						}
					}
				}
		    	if (neighbourhoods[nNew] == 0) 
		    		return false;
		    }
			
	        //check inverse dependency
		    int prevNDecrease=1;
		    for (int j = 0; j < processes.length; j++) {
				if (i!=j){
					int nPrevJ=problem.machineNeighbourhoodMap[assignment[processes[j]]];
					int nNewJ=problem.machineNeighbourhoodMap[machines[j]];
					if (nPrevJ==nPrev && nPrevJ!=nNewJ && service==problem.processServiceMap[processes[j]]) prevNDecrease++;
					if (nPrevJ != nNewJ && nNewJ == nPrev && service==problem.processServiceMap[processes[j]]) prevNDecrease--;
				}
			}
	        if (neighborHoodCount[service][nPrev]-prevNDecrease>0) continue;
	        for (int sInvDep: inverseDependencies[service]){
	        	for (int pa : problem.services[sInvDep]){
	        		int na = problem.machineNeighbourhoodMap[assignment[pa]];
	        		int index = -1;
	        		if((index = contains(processes, pa)) != -1) {
	        			na = problem.machineNeighbourhoodMap[machines[index]];
	        		}
	        		if (nPrev==na){
	        			return false;
	        		}
	        	}
	        }
		}
		

        
		return true;
	}
		
	public boolean tryMachineBySpreadSwap(int[] processes, int[] machines){
		
		int p1=processes[0];
		int p2=processes[1];
		if (problem.processServiceMap[p1]==problem.processServiceMap[p2]) return true;
		else return tryMachineBySpread(p1, machines[0]) && tryMachineBySpread(p2, machines[1]);
	}
        
	
	/**GET**/
	
	public Problem getProblem() {
		return problem;
	}

	public int[] getInitialAssignment() {
		return initialAssignment;
	}

	public long[][] getTransientUsage() {
		return transientUsage;
	}
	

	public int[] getAssignment() {
		return assignment;
	}

	public long[][] getUsage() {
		return usage;
	}
	
	public Map<Integer, Set<Integer>> getMachineToProcMap() {
		return machineToProcMap;
	}
	
	public long[][] getQuadraticOverload() {
		return quadraticOverload;
	}
	

}

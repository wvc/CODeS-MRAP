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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Class for loading problem instances. Holds all instance specific, static, information
 *
 */
public class Problem {

    public int nrMachines;
    public int nrProcesses;
    public int nrResources;
    public int nrServices;
    public int nrNeighbourhoods;
    public int nrLocations;
    public int nrTransientResources;
    public int nrBalanceObj;
    public int totalNrOfDependencies;

    public long[][] cap; //[machine, resource] capacity of a specific resource on a machine 
    public long[][] safetyCap; //[machine, resource] safety capacity of a specific resource on a machine 
    public long[][] processReq; //[process, resource] resource requirement of a specific process

    public int[][] services;  // [service] list of processes (ids ) in a service
    public int[] processServiceMap; // [process] per process, the service id it belongs to

    public int[][] neighbourhoods;  // [neighbourhood], per neighbourhood, list of machines in the neighbourhood
    public int[] machineNeighbourhoodMap; // [machine], per machine the neighbourhood it belongs to

    public int[][] locations; // [location], per location, list of machines in the location
    public int[] machineLocationMap;  //[machine], per machine the location it belongs to

    public int[] transientResources;  // list of ids of transient resources
    public boolean[] transientResourceMap; // [resource] indicates if a specific resource is transient
    
    public int[][] balanceObj;

    public int[][] serviceDependencies;  // [service], per service, a list of services (ids) it depends on. 
    public int[] serviceSpreadMin;  // [service] the minimum spread for each service

    public int[] processMoveCost;  // [process] moveCost per process
    public int[][] machineMoveCost; // [machine, machine] moveCost between two machines

    public int[] resourceLoadCostWeight;
    public int[] balanceObjWeight;
    public int processMoveCostWeight;
    public int serviceMoveCostWeight;
    public int machineMoveCostWeight;

    public static Problem loadProblem(File file) throws IOException {
        
        Problem problem = new Problem();
        Scanner sc = null;
        
        try {
            sc = new Scanner(file);
            
            loadResources(problem, sc);        
            loadMachines(problem, sc);         
            loadServices(problem, sc);        
            loadProcesses(problem, sc);
            loadBalanceObjectives(problem, sc);           
            loadWeights(problem, sc);
            
            return problem;
        } finally {
            if(sc != null)
                sc.close();
        }
        
    }

    private static void loadWeights(Problem problem, Scanner sc) {
        problem.processMoveCostWeight = sc.nextInt();
        problem.serviceMoveCostWeight = sc.nextInt();
        problem.machineMoveCostWeight = sc.nextInt();
    }

    private static void loadBalanceObjectives(Problem problem, Scanner sc) {
        problem.nrBalanceObj = sc.nextInt();
        problem.balanceObj = new int[problem.nrBalanceObj][];
        problem.balanceObjWeight = new int[problem.nrBalanceObj];
        
        for(int i = 0; i<problem.nrBalanceObj; i++) {
            
            int r1 = sc.nextInt();
            int r2 = sc.nextInt();
            int target = sc.nextInt();
            
            problem.balanceObj[i] = new int[] {r1,r2,target};
            problem.balanceObjWeight[i] = sc.nextInt();
        }
    }

    private static void loadProcesses(Problem problem, Scanner sc) {
        problem.nrProcesses = sc.nextInt();
        problem.processServiceMap = new int[problem.nrProcesses];
        problem.processReq = new long[problem.nrProcesses][problem.nrResources];
        problem.processMoveCost = new int[problem.nrProcesses];
        problem.services = new int[problem.nrServices][];
        
        for(int i = 0; i<problem.nrProcesses; i++) {
            problem.processServiceMap[i] = sc.nextInt();
            
            for(int j = 0; j<problem.nrResources; j++) {
                problem.processReq[i][j] = sc.nextLong();
            }
            
            problem.processMoveCost[i] = sc.nextInt();
        }
        
        for(int i = 0; i<problem.nrServices; i++) {
            
            problem.services[i] = new int[problem.nrProcesses];
            
            int index = 0;
            for(int j = 0; j<problem.nrProcesses; j++) {
                if(problem.processServiceMap[j] == i) {
                    problem.services[i][index++] = j;
                }
            }
            
            problem.services[i] = Arrays.copyOf(problem.services[i], index);        
        }
    }

    private static void loadServices(Problem problem, Scanner sc) {
        problem.nrServices = sc.nextInt();
        problem.serviceSpreadMin = new int[problem.nrServices];
        problem.serviceDependencies = new int[problem.nrServices][];
        
        for(int i = 0; i<problem.nrServices; i++) {
            problem.serviceSpreadMin[i] = sc.nextInt();
            
            int dependencies = sc.nextInt();
            problem.totalNrOfDependencies+=dependencies;
            problem.serviceDependencies[i] = new int[dependencies];
            
            for(int j = 0; j<dependencies; j++) {
                problem.serviceDependencies[i][j] = sc.nextInt();
            }
            
        }
    }

    private static void loadMachines(Problem problem, Scanner sc) {
        problem.nrMachines = sc.nextInt();
        problem.machineNeighbourhoodMap = new int[problem.nrMachines];
        problem.machineLocationMap = new int[problem.nrMachines];
        problem.cap = new long[problem.nrMachines][problem.nrResources];
        problem.safetyCap = new long[problem.nrMachines][problem.nrResources];
        problem.machineMoveCost = new int[problem.nrMachines][problem.nrMachines];
        
        problem.nrLocations = 0;
        problem.nrNeighbourhoods = 0;
        
        for(int i = 0; i<problem.nrMachines; i++) {
            problem.machineNeighbourhoodMap[i] = sc.nextInt();
            problem.machineLocationMap[i] = sc.nextInt();
            
            problem.nrNeighbourhoods = Math.max(problem.nrNeighbourhoods, problem.machineNeighbourhoodMap[i]+1);
            problem.nrLocations = Math.max(problem.nrLocations, problem.machineLocationMap[i]+1);
            
            for(int j = 0; j<problem.nrResources; j++) {
                problem.cap[i][j] = sc.nextLong();
            }
            for(int j = 0; j<problem.nrResources; j++) {
                problem.safetyCap[i][j] = sc.nextLong();
            }
            for(int j = 0; j<problem.nrMachines; j++) {
                problem.machineMoveCost[i][j] = sc.nextInt();
            }
        }
        
        problem.neighbourhoods = new int[problem.nrNeighbourhoods][];
        problem.locations = new int[problem.nrLocations][];
        
        Map<Integer, List<Integer>> nMap = new HashMap<Integer, List<Integer>>();
        Map<Integer, List<Integer>> lMap = new HashMap<Integer, List<Integer>>();
        
        for(int i = 0; i<problem.nrMachines; i++) {
            int n = problem.machineNeighbourhoodMap[i];
            int l = problem.machineLocationMap[i];
            
            if(!nMap.containsKey(n)) {
                nMap.put(n, new ArrayList<Integer>());
            }
            if(!lMap.containsKey(l)) {
                lMap.put(l, new ArrayList<Integer>());
            }
            
            nMap.get(n).add(i);
            lMap.get(l).add(i);
        }
        
        for(int n = 0; n<problem.nrNeighbourhoods; n++) {
            List<Integer> machineIds = nMap.get(n);
            if(machineIds == null) {
                machineIds = Collections.emptyList();
            }
            problem.neighbourhoods[n] = new int[machineIds.size()];
            
            int index = 0;
            for(int i : machineIds) {
                problem.neighbourhoods[n][index++] = i;
            }
        }
        
        for(int l = 0; l<problem.nrLocations; l++) {
            List<Integer> machineIds = lMap.get(l);
            if(machineIds == null) {
                machineIds = Collections.emptyList();
            }
            problem.locations[l] = new int[machineIds.size()];
            
            int index = 0;
            for(int i : machineIds) {
                problem.locations[l][index++] = i;
            }
        }
    }

    private static void loadResources(Problem problem, Scanner sc) {
        problem.nrResources = sc.nextInt();
        problem.transientResources = new int[problem.nrResources];
        problem.transientResourceMap = new boolean[problem.nrResources];
        problem.resourceLoadCostWeight = new int[problem.nrResources];
        
        int index = 0;
        for(int i = 0; i<problem.nrResources; i++) {
            if(sc.nextInt() == 1) {
                problem.transientResources[index++] = i;
                problem.transientResourceMap[i] = true;
            } else {
                problem.transientResourceMap[i] = false;
            }
            
            problem.resourceLoadCostWeight[i] = sc.nextInt();
            
        }
        problem.transientResources = Arrays.copyOf(problem.transientResources, index);
        problem.nrTransientResources = index;
    }
    
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Problem [");
		builder.append("\n\tnrMachines=");
		builder.append(nrMachines);
		builder.append("\n\tnrProcesses=");
		builder.append(nrProcesses);
		builder.append("\n\tnrResources=");
		builder.append(nrResources);
		builder.append("\n\tnrServices=");
		builder.append(nrServices);
		builder.append("\n\tnrNeighbourhoods=");
		builder.append(nrNeighbourhoods);
		builder.append("\n\tnrLocations=");
		builder.append(nrLocations);
		builder.append("\n\tnrTransientResources=");
		builder.append(nrTransientResources);
		builder.append("\n\tnrBalanceObj=");
		builder.append(nrBalanceObj);
		builder.append("\n\ttotalNrOfDependencies=");
		builder.append(totalNrOfDependencies);
		builder.append("\n]");
		return builder.toString();
	}    
    
}

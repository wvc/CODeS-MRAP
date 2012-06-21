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
package be.kahosl.roadef2012.lora.moves;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import be.kahosl.lora.localsearch.move.Move;
import be.kahosl.lora.localsearch.move.MoveFactory;
import be.kahosl.roadef2012.lora.MRAPSolution;
import be.kahosl.roadef2012.model.Problem;

public class SwapMachineMoveFactory implements MoveFactory<MRAPSolution> {
	
	private boolean fullneighborhood;
	private int tournamentFactor;
	private Random rand;
	
	public SwapMachineMoveFactory() {
		fullneighborhood=true;
	}
	
	public SwapMachineMoveFactory(boolean fullNeighborhood, int tournamentFactor,Random rand) {
		this.fullneighborhood=fullNeighborhood;
		this.tournamentFactor=tournamentFactor;
		this.rand=rand;
	}

	@Override
	public List<? extends Move<MRAPSolution>> createMoves(MRAPSolution solution) {
		List<Move<MRAPSolution>> moveList=new ArrayList<Move<MRAPSolution>>();
		Problem problem = solution.getProblem();
		
		int p1;
		int p2;
		int m1;
		int m2;
		if (fullneighborhood){

			for(p1=0;p1<problem.nrProcesses-1;p1++) {
				for(p2=p1+1;p2<problem.nrProcesses;p2++) {
					m1=solution.getAssignment()[p1];
					m2=solution.getAssignment()[p2];
					if (m1!=m2){
						int[] processes=new int[]{p1,p2};
						int[] machines=new int[]{m2,m1};
						if (solution.tryMachineByCapacitySwap(processes, machines) && solution.tryMachineByConflictSwap(processes, machines) && solution.tryMachineByDependencySwap(processes, machines) && solution.tryMachineBySpreadSwap(processes, machines)){
							moveList.add(new SwapMachineMove(processes, machines));
						}
					}
				}		
			}
		}else {
			do {
				p1=rand.nextInt(problem.nrProcesses);
				do {
					p2=rand.nextInt(problem.nrProcesses);
				} while (p1==p2 || solution.getAssignment()[p1]==solution.getAssignment()[p2]);
				m1=solution.getAssignment()[p1];
				m2=solution.getAssignment()[p2];
				
				int[] processes=new int[]{p1,p2};
				int[] machines=new int[]{m2,m1};
				if (solution.tryMachineByCapacitySwap(processes, machines) && solution.tryMachineByConflictSwap(processes, machines) && solution.tryMachineByDependencySwap(processes, machines) && solution.tryMachineBySpreadSwap(processes, machines)){
					moveList.add(new SwapMachineMove(processes, machines));
				}
				
			}while(moveList.size()<tournamentFactor);
		}
		
		return moveList;
	}


}

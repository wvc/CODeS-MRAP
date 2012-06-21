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
import java.util.Set;

import be.kahosl.lora.localsearch.move.Move;
import be.kahosl.lora.localsearch.move.MoveFactory;
import be.kahosl.roadef2012.lora.MRAPSolution;
import be.kahosl.roadef2012.model.Problem;

public class ProbabilisticReAssignmentMoveFactoryFast implements MoveFactory<MRAPSolution> {
	
	private int tournamentFactor;
	private Random rand;
	
	public ProbabilisticReAssignmentMoveFactoryFast() {
	}
	
	public ProbabilisticReAssignmentMoveFactoryFast(int tournamentFactor,Random rand) {
		this.tournamentFactor=tournamentFactor;
		this.rand=rand;
	}

	@Override
	public List<? extends Move<MRAPSolution>> createMoves(MRAPSolution solution) {
		List<Move<MRAPSolution>> moveList=new ArrayList<Move<MRAPSolution>>();
		Problem problem = solution.getProblem();
		
		// determine quadratic overload cost per machine (delta based)
		long[] cost = new long[problem.nrMachines];
		long totalCost = 0;
		
		for(int m = 0; m<problem.nrMachines; m++) {
			for(int r = 0; r<problem.nrResources; r++) {
				long d =  solution.getQuadraticOverload()[m][r]+1;
				totalCost+=d;
				cost[m] += d;
			}
		}

		
		// select probabilistic moves
		do {
			// roulette wheel select a problematic machine
			int machine=rouletteWheel(cost,totalCost);
			Set<Integer> processes = solution.getMachineToProcMap().get(machine);
			for(int p : processes) {
				
				// find anohter random machine, not equal to the already selected one, to move the process to
				int m = machine;
				do {
					m = rand.nextInt(problem.nrMachines);
				}
				while(m==machine);
				
				// check feasibilty of the move
				if (solution.tryMachineByCapacity(p, m) && solution.tryMachineByConflict(p, m) && solution.tryMachineByDependency(p, m) && solution.tryMachineBySpread(p, m)){
					moveList.add(new SingleMachineReassignmentMove(p, m));
				}
			}
			
			// if we selected to many moves, delete the remainder
			int toMany = Math.max(0, moveList.size()-tournamentFactor);
			for (int i=0;i<toMany;i++){
				moveList.remove(rand.nextInt(moveList.size()));
			}
			
		}while(moveList.size()<tournamentFactor);
		
		
		return moveList;
	}
	
	/**
	 * Roulette wheel selection using absolute values (which is faster than with probabilities between 0 and 1.0)
	 * @param costs
	 * @param totalcost
	 * @return
	 */
	private int rouletteWheel(long[] costs, long totalcost){
		// return action with highest probability
		long randomProb = (long)(rand.nextDouble()*totalcost);
		long cumProb = 0; 
		for (int i = 0; i < costs.length; i++) {
			if (cumProb <= randomProb
					&& randomProb <= cumProb + costs[i]) {
				return i;
			}
			cumProb = cumProb + costs[i];
		}
		return 0;
	}

}

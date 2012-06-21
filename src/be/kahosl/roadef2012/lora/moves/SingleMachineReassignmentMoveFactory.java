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

public class SingleMachineReassignmentMoveFactory implements
		MoveFactory<MRAPSolution> {

	private boolean fullneighborhood;
	private int tournamentFactor;
	private Random rand;

	public SingleMachineReassignmentMoveFactory() {
		fullneighborhood = true;
	}

	public SingleMachineReassignmentMoveFactory(boolean fullNeighborhood,
			int tournamentFactor, Random rand) {
		this.fullneighborhood = fullNeighborhood;
		this.tournamentFactor = tournamentFactor;
		this.rand = rand;
	}

	@Override
	public List<? extends Move<MRAPSolution>> createMoves(MRAPSolution solution) {
		List<Move<MRAPSolution>> moveList = new ArrayList<Move<MRAPSolution>>();
		Problem problem = solution.getProblem();

		if (fullneighborhood) {
			for (int p = 0; p < problem.nrProcesses; p++) {
				for (int m = 0; m < problem.nrMachines; m++) {
					if (solution.getAssignment()[p] != m) {
						// check feasibility
						if (solution.tryMachineByCapacity(p, m)
								&& solution.tryMachineByConflict(p, m)
								&& solution.tryMachineByDependency(p, m)
								&& solution.tryMachineBySpread(p, m)) {
							moveList.add(new SingleMachineReassignmentMove(p, m));
						}
					}
				}
			}
		} else {
			// random samples
			do {
				int p = rand.nextInt(problem.nrProcesses);
				int m = 0;
				do {
					m = rand.nextInt(problem.nrMachines);
				} while (m == solution.getAssignment()[p]);

				// check feasibility of move
				if (solution.tryMachineByCapacity(p, m)
						&& solution.tryMachineByConflict(p, m)
						&& solution.tryMachineByDependency(p, m)
						&& solution.tryMachineBySpread(p, m)) {
					moveList.add(new SingleMachineReassignmentMove(p, m));
				}

			} while (moveList.size() < tournamentFactor);
		}

		return moveList;
	}

}

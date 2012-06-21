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
package be.kahosl.roadef2012.lora;

import be.kahosl.lora.localsearch.move.Move;
import be.kahosl.lora.localsearch.objective.ObjectiveFunction;
import be.kahosl.roadef2012.lora.moves.SingleMachineReassignmentMove;
import be.kahosl.roadef2012.model.AssignmentHelper;

public class MRAPObjective implements ObjectiveFunction<MRAPSolution>{

	private int nrOfEvaluations;
	
	public MRAPObjective() {
		this.nrOfEvaluations=0;
	}
	
	@Override
	public double evaluate(MRAPSolution solution) {
		nrOfEvaluations++;
		return AssignmentHelper.evaluate(solution.getProblem(), solution.getInitialAssignment(), solution.getAssignment());
	}

	@Override
	public double evaluateDelta(MRAPSolution solution, double currentValue,
			Move<MRAPSolution> move) {
		MRAPMove mrapMove=(MRAPMove) move;
		nrOfEvaluations++;
		
		long delta=0;
		
		//faster evaluation for single moves
		if (move instanceof SingleMachineReassignmentMove) delta=solution.evaluateDelta(mrapMove.getProcesses()[0], mrapMove.getMachines()[0]);
		else delta=solution.evaluateDelta(mrapMove.getProcesses(), mrapMove.getMachines());
		
		return delta;
	}

	@Override
	public int getNrOfEvaluations() {
		return nrOfEvaluations;
	}

	@Override
	public void resetNrOfEvaluations() {
		nrOfEvaluations=0;		
	}

}

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
package be.kahosl.lora.localsearch.hyperheuristic.heuristicselection;

import java.util.Random;

public class RandomHeuristicSelection implements HeuristicSelection{

	private Random rand;
	private int nrOfHeuristics;
	
	public RandomHeuristicSelection(Random rand) {
		this.rand=rand;
	}
	
	@Override
	public int selectHeuristic(double currentScore, double bestScore) {
		return rand.nextInt(nrOfHeuristics);
	}

	@Override
	public void setNrOfHeuristics(int nrOfHeuristics) {
		this.nrOfHeuristics = nrOfHeuristics;	
	}

	@Override
	public String getShortName() {
		return "SR";
	}

}

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

import be.kahosl.lora.localsearch.Solution;
import be.kahosl.roadef2012.model.Assignment;
import be.kahosl.roadef2012.model.Problem;

public class MRAPSolution extends Assignment implements Solution{

	public MRAPSolution(Problem problem, int[] initialAssignment) {
		super(problem, initialAssignment);
	}
	
	public MRAPSolution(Problem problem, int[] initialAssignment,int [] existingAssignment) {
		super(problem, initialAssignment,existingAssignment);
	}
	
	public MRAPSolution(Assignment assignment){
		super(assignment);
	}

	@Override
	public Solution cloneSolution() {
		return new MRAPSolution(this);
	}

}

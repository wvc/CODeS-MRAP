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

import be.kahosl.roadef2012.lora.MRAPMove;
import be.kahosl.roadef2012.lora.MRAPSolution;

public class SwapMachineMove extends MRAPMove {

	public SwapMachineMove(int[] processes, int[] machines) {
		super(processes, machines);
	}

	@Override
	public void doMove(MRAPSolution solution) {
		solution.doMove(processes, machines);
	}

	@Override
	public void undoMove(MRAPSolution solution) {
		// not necessary because not used in our implementation
		throw new RuntimeException("Not implemented!");

	}

}

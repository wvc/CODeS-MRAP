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
package be.kahosl.lora.localsearch.hyperheuristic.acceptance;

import be.kahosl.lora.localsearch.move.Move;


public class LateAcceptanceCriterion implements AcceptanceCriterion {

	private double[] acceptanceList;
	private int laListLength;
	
	private int iteration;
	
	private boolean isInitial;
	
	
	public LateAcceptanceCriterion(int laListLength) {
		acceptanceList=new double[laListLength];
		this.laListLength=laListLength;
		iteration=0;
		isInitial=true;
	}
	
	@Override
	public boolean acceptMove(Move move,double newScore, double currentScore, double bestScore) {
		boolean accept=false;

		if (isInitial) {
			isInitial = false;
			for (int i = 0; i < laListLength; i++) {
				acceptanceList[i] = bestScore;
			}
		}


		int placeToLook = iteration % laListLength;
		if (newScore <= acceptanceList[placeToLook]) {
			accept = true;
			acceptanceList[placeToLook] = newScore;
		} else
			acceptanceList[placeToLook] = currentScore;

		iteration++;
		
		return accept;
	}

	@Override
	public String getShortName() {
		return "LA_"+laListLength;
	}

}

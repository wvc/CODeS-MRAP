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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import be.kahosl.lora.localsearch.LocalSearchListener;
import be.kahosl.lora.localsearch.hyperheuristic.acceptance.AcceptanceCriterion;
import be.kahosl.lora.localsearch.hyperheuristic.heuristicselection.HeuristicSelection;
import be.kahosl.lora.localsearch.move.Move;
import be.kahosl.lora.localsearch.move.MoveFactory;
import be.kahosl.lora.localsearch.objective.ObjectiveFunction;
import be.kahosl.lora.localsearch.termination.TerminationCriterion;

public class FastHyperHeuristic <S extends MRAPSolution> {
	
	private List<LocalSearchListener<S>> listeners;
	private List<MoveFactory<S>> moveFactoryList;
	private AtomicBoolean stopped;

	private HeuristicSelection heuristicSelection;
	private AcceptanceCriterion acceptanceCriterion;
	
	private boolean showScore;
	private int scoreInterval;
	private long currentIteration;
	
	
	public void setShowScore(boolean showScore, int scoreInterval){
		this.showScore=showScore;
		this.scoreInterval=scoreInterval;
	}

	public FastHyperHeuristic(List<MoveFactory<S>> moveFactoryList, HeuristicSelection heuristicSelection,
			AcceptanceCriterion acceptanceCriterion) {
		this.moveFactoryList = moveFactoryList;
		this.stopped = new AtomicBoolean(true);
		this.listeners = new ArrayList<LocalSearchListener<S>>();
		this.heuristicSelection=heuristicSelection;
		heuristicSelection.setNrOfHeuristics(moveFactoryList.size());
		this.acceptanceCriterion=acceptanceCriterion;
		showScore=false;
		scoreInterval=1;
	}



	
	public int[] startSearch(
			S initialSolution, 
			ObjectiveFunction<S> objectiveFunction,
			TerminationCriterion terminationCriterion) {
		
		//S bestSolution=(S)initialSolution.cloneSolution();
		S currentSolution=(S)initialSolution.cloneSolution();
		int[] bestSolution=currentSolution.getAssignment();
		
		double bestScore= objectiveFunction.evaluate(currentSolution);
		double currentScore = bestScore;
		
		stopped.set(false);
		
		long startTime = System.currentTimeMillis();
		long elapsedTime = 0;
		currentIteration = 0;
		long objectiveFunctionEvaluations = 0;
		
		
		while(!stopped.get() && !terminationCriterion.isFinished(currentIteration, elapsedTime, objectiveFunctionEvaluations,currentScore)) {
			for(LocalSearchListener<S> listener : listeners) {
				listener.foundNewCurrentSolution(currentSolution,currentScore);
			}
			//selection
			int heuristicNr = heuristicSelection.selectHeuristic(currentScore, bestScore);
			MoveFactory<S> chosenHeuristic = moveFactoryList.get(heuristicNr);
			
			//best move in neighborhood generation
			List<? extends Move<S>>  moves =  chosenHeuristic.createMoves(currentSolution);
			if (moves == null || moves.isEmpty()) {
				if (showScore) System.out.println("[Warning] No moves created for "+chosenHeuristic.toString());
				continue;
			}
			
			Move<S> bestMove = null;
			double bestDeltaScore = Double.POSITIVE_INFINITY;
			
			for(Move<S> move : moves) {
				double deltaScore = objectiveFunction.evaluateDelta(currentSolution, currentScore, move);
				objectiveFunctionEvaluations++;
				
				if(deltaScore < bestDeltaScore) {
					bestMove = move;
					bestDeltaScore = deltaScore;
				}
			}
			
			double newScore=currentScore+bestDeltaScore;
			if(bestMove != null) {
				if (acceptanceCriterion.acceptMove(bestMove,newScore, currentScore, bestScore)){
					bestMove.doMove(currentSolution);
					currentScore += bestDeltaScore;
				}
				if (newScore<bestScore){
					bestScore=newScore;
					
					// don't do clone, just a fast copy of assignment array
					bestSolution=Arrays.copyOf(currentSolution.getAssignment(), currentSolution.getAssignment().length);
					for(LocalSearchListener<S> listener : listeners) {
						listener.foundNewBestSolution(currentSolution,bestScore,bestMove);
					}
				}
				if (showScore && currentIteration%scoreInterval==0) System.out.println(currentIteration+" currentScore: "+currentScore+" bestScore: "+bestScore);
			} else {
				throw new RuntimeException("Best move was null!");
			}
			
			elapsedTime = System.currentTimeMillis() - startTime;
			currentIteration++;
		}
		return bestSolution;
	}
	
	public void stopSearch() {
		stopped.set(true);	
	}
		
	public void addLocalSearchListener(LocalSearchListener<S> listener) {
		listeners.add(listener);
		
	}	
	
	public void setAcceptanceCriterion(AcceptanceCriterion acceptanceCriterion) {
		this.acceptanceCriterion = acceptanceCriterion;
	}

}

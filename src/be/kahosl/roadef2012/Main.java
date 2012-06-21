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
package be.kahosl.roadef2012;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import be.kahosl.lora.localsearch.hyperheuristic.acceptance.AcceptanceCriterion;
import be.kahosl.lora.localsearch.hyperheuristic.acceptance.LateAcceptanceCriterion;
import be.kahosl.lora.localsearch.hyperheuristic.heuristicselection.HeuristicSelection;
import be.kahosl.lora.localsearch.hyperheuristic.heuristicselection.RandomHeuristicSelection;
import be.kahosl.lora.localsearch.move.MoveFactory;
import be.kahosl.lora.localsearch.termination.MaxExecutionTimeTerminationCriterion;
import be.kahosl.roadef2012.lora.FastHyperHeuristic;
import be.kahosl.roadef2012.lora.MRAPObjective;
import be.kahosl.roadef2012.lora.MRAPSolution;
import be.kahosl.roadef2012.lora.moves.ProbabilisticReAssignmentMoveFactoryFast;
import be.kahosl.roadef2012.lora.moves.SwapMachineMoveFactory;
import be.kahosl.roadef2012.model.AssignmentHelper;
import be.kahosl.roadef2012.model.Problem;

public class Main {

	/**
	 * The entry point for the ROADEF competition executable .. 
	 * This is called from a BAT file which starts the jvm and passes along all command line arguments
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		long startTime=System.currentTimeMillis()+100;
		
		//arguments
		long timeLimit=0;
		String instanceFileName="";
		String originalSolutionFileName="";
		String newSolutionFileName="";
		int seed=0;
		
		
		//read arguments
		int argPos=0;
		while (argPos<=args.length-1){
			if (args[argPos].compareTo("-t")==0){
				timeLimit=Integer.parseInt(args[argPos+1]);
				//System.out.println("Set timelimit to: "+timeLimit+" s");
				argPos+=2;
				continue;
			}
			if (args[argPos].compareTo("-p")==0){
				instanceFileName=args[argPos+1];
				//System.out.println("Set instance filename to: "+instanceFileName);
				argPos+=2;
				continue;
			}
			if (args[argPos].compareTo("-i")==0){
				originalSolutionFileName=args[argPos+1];
				//System.out.println("Set original solution filename to: "+originalSolutionFileName);
				argPos+=2;
				continue;
			}
			if (args[argPos].compareTo("-o")==0){
				newSolutionFileName=args[argPos+1];
				//System.out.println("Set new solution filename to: "+newSolutionFileName);
				argPos+=2;
				continue;
			}
			if (args[argPos].compareTo("-name")==0){
				System.out.println("J17");
				argPos+=1;
				if (args.length==1) System.exit(0);
				continue;
			}
			if (args[argPos].compareTo("-s")==0){
				seed=Integer.parseInt(args[argPos+1]);
				//System.out.println("Set seed to: "+seed);
				argPos+=2;
				continue;
			}
		}
		
		// load problem and initial assignment
		Problem problem = Problem.loadProblem(new File(instanceFileName));		
		int[] a0=AssignmentHelper.loadSolution(new File(originalSolutionFileName));
		

		//run lateacceptance (two threads)
		int[] solution = runLA(startTime, timeLimit*1000, seed, problem, a0);	
		
		//write solution
		File result = new File(newSolutionFileName);
		result.getAbsoluteFile().getParentFile().mkdirs();
		AssignmentHelper.writeSolution(result, solution);
		
		
	}

	private static int[] runLA(final long startTime, final long timeLimit, final int seed,
			final Problem problem, final int[] a0) {
		
		// create threadpool
		ExecutorService es = Executors.newFixedThreadPool(2);
		
		// set params for different algorithm configurations
		final int listLength1 = 2000, listLength2 = 500;
		final int tf1 = 1, tf2 = 1000;
		
		// first thread
		Callable<int[]> run1 = new Callable<int[]>() {

			@Override
			public int[] call() throws Exception {
				final Random rand1 = new Random(seed+1);

				MRAPSolution initSol = new MRAPSolution(problem, a0);
				AcceptanceCriterion acceptanceCriterion = new LateAcceptanceCriterion(listLength1);
							
				MRAPObjective objective = new MRAPObjective();
				List<MoveFactory<MRAPSolution>> moveFactories = new ArrayList<MoveFactory<MRAPSolution>>();
				
				moveFactories.add(new SwapMachineMoveFactory(false, tf1, rand1));
				moveFactories.add(new ProbabilisticReAssignmentMoveFactoryFast(tf1, rand1));
						
				HeuristicSelection hs = new RandomHeuristicSelection(rand1);

				FastHyperHeuristic<MRAPSolution> hh = 
						new FastHyperHeuristic<MRAPSolution>(moveFactories, hs	, acceptanceCriterion);
				
				long runtime = timeLimit-(System.currentTimeMillis()-startTime);
				int[] sol = hh.startSearch(initSol, objective, new MaxExecutionTimeTerminationCriterion(runtime));	
				
				return sol;
			}
		};
		
		// second thread
		Callable<int[]> run2 = new Callable<int[]>() {
			
			@Override
			public int[] call() throws Exception {
				final Random rand2 = new Random(seed+1);

				MRAPSolution initSol = new MRAPSolution(problem, a0);
				AcceptanceCriterion acceptanceCriterion = new LateAcceptanceCriterion(listLength2);
							
				MRAPObjective objective = new MRAPObjective();
				List<MoveFactory<MRAPSolution>> moveFactories = new ArrayList<MoveFactory<MRAPSolution>>();
				
				moveFactories.add(new SwapMachineMoveFactory(false, tf2, rand2));
				moveFactories.add(new ProbabilisticReAssignmentMoveFactoryFast(tf2, rand2));
						
				HeuristicSelection hs = new RandomHeuristicSelection(rand2);

				FastHyperHeuristic<MRAPSolution> hh = 
						new FastHyperHeuristic<MRAPSolution>(moveFactories, hs	, acceptanceCriterion);
				
				long runtime = timeLimit-(System.currentTimeMillis()-startTime);
				int[] sol = hh.startSearch(initSol, objective, new MaxExecutionTimeTerminationCriterion(runtime));	
				
				return sol;
			}
		};
		
		Future<int[]> result1 = es.submit(run1);
		Future<int[]> result2 = es.submit(run2);
		try {
			int[] sol1 = result1.get();
			int[] sol2 = result2.get();
			es.shutdown();
			long score1 = AssignmentHelper.evaluate(problem, a0, sol1);
			long score2 = AssignmentHelper.evaluate(problem, a0, sol2);
			
			// return best result out of the two threads
			if(score1 < score2) {
				return sol1;
			} else {
				return sol2;
			}
			

		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
		// something wrong
		return null;
		
	}

}

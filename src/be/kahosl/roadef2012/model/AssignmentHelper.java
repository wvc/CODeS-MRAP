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
package be.kahosl.roadef2012.model;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * Helper class for working with solutions (loading them from file, checking
 * feasibility, evaluation, writing to a file)
 * 
 */
public class AssignmentHelper {

	public static boolean debug = false;

	public static boolean checkFeasibility(Problem problem,
			int[] initialAssignment, int[] assignment) {

		boolean feasible = true;

		feasible = feasible & checkCapacityConstraint(problem, assignment);
		feasible = feasible & checkConflictConstraint(problem, assignment);
		feasible = feasible & checkSpreadConstraint(problem, assignment);
		feasible = feasible & checkDependencyConstraint(problem, assignment);
		feasible = feasible
				& checkTransientUsageConstraint(problem, initialAssignment,
						assignment);

		return feasible;
	}

	public static boolean checkTransientUsageConstraint(Problem problem,
			int[] initialAssignment, int[] assignment) {
		boolean feasible = true;

		int[][] usage = new int[problem.nrMachines][problem.nrResources];

		for (int p = 0; p < problem.nrProcesses; p++) {

			int m = assignment[p];

			for (int r = 0; r < problem.nrResources; r++) {
				usage[m][r] += problem.processReq[p][r];

				if (problem.transientResourceMap[r]) {
					int m0 = initialAssignment[p];
					if (m0 != m) {
						usage[m0][r] += problem.processReq[p][r];
					}
				}

			}
		}

		for (int m = 0; m < problem.nrMachines; m++) {
			for (int tr : problem.transientResources) {

				if (usage[m][tr] > problem.cap[m][tr]) {

					if (debug)
						System.out.println("Transient usage constraint "
								+ "violation on machine " + m + " resource "
								+ tr + ": transient_usage = " + usage[m][tr]
								+ ", capacity = " + problem.cap[m][tr]);
					feasible = false;
				}
			}
		}

		return feasible;
	}

	public static boolean checkDependencyConstraint(Problem problem,
			int[] assignment) {

		boolean feasible = true;

		for (int s = 0; s < problem.nrServices; s++) {

			for (int sDep : problem.serviceDependencies[s]) {

				Set<Integer> neighbourhoods = new HashSet<Integer>();
				for (int pb : problem.services[sDep]) {
					int nb = problem.machineNeighbourhoodMap[assignment[pb]];

					neighbourhoods.add(nb);

				}
				for (int pa : problem.services[s]) {

					int na = problem.machineNeighbourhoodMap[assignment[pa]];

					if (!neighbourhoods.contains(na)) {
						if (debug)
							System.out
									.println("Dependency constraint violation: s_a = "
											+ s
											+ ", s_b = "
											+ sDep
											+ ", p_a = " + pa);
						feasible = false;
					}
				}
			}

		}

		return feasible;
	}

	public static boolean checkSpreadConstraint(Problem problem,
			int[] assignment) {

		boolean feasible = true;

		for (int s = 0; s < problem.nrServices; s++) {

			Set<Integer> locations = new HashSet<Integer>();

			for (int p : problem.services[s]) {
				int l = problem.machineLocationMap[assignment[p]];

				locations.add(l);
			}

			if (locations.size() < problem.serviceSpreadMin[s]) {
				if (debug) {
					System.out
							.println("Spread constraint violated for service "
									+ s + ":");
					for (int p : problem.services[s]) {
						System.out.println("\tM(" + p + ") = " + assignment[p]
								+ ", location = "
								+ problem.machineLocationMap[assignment[p]]);
					}
				}
				feasible = false;
			}

		}

		return feasible;
	}

	public static boolean checkConflictConstraint(Problem problem,
			int[] assignment) {

		boolean feasible = true;
		for (int s = 0; s < problem.nrServices; s++) {
			Set<Integer> machines = new HashSet<Integer>();
			for (int p : problem.services[s]) {
				machines.add(assignment[p]);
			}

			if (machines.size() < problem.services[s].length) {
				if (debug) {
					System.out
							.println("Conflict constraint violated for service "
									+ s + ":");

					for (int p : problem.services[s]) {
						System.out.println("\tM(" + p + ") = " + assignment[p]);
					}
				}
				feasible = false;
			}
		}

		return feasible;
	}

	public static boolean checkCapacityConstraint(Problem problem,
			int[] assignment) {

		int[][] usage = new int[problem.nrMachines][problem.nrResources];

		for (int p = 0; p < problem.nrProcesses; p++) {

			int m = assignment[p];

			for (int r = 0; r < problem.nrResources; r++) {
				usage[m][r] += problem.processReq[p][r];
			}
		}

		boolean feasible = true;
		for (int m = 0; m < problem.nrMachines; m++) {
			for (int r = 0; r < problem.nrResources; r++) {

				if (usage[m][r] > problem.cap[m][r]) {

					if (debug)
						System.out
								.println("Capacity constraint violation on machine "
										+ m
										+ ", resource "
										+ r
										+ ": usage = "
										+ usage[m][r]
										+ ", capacity = "
										+ problem.cap[m][r]);
					feasible = false;
				}
			}
		}
		return feasible;
	}

	public static long evaluate(Problem problem, int[] initialAssignment,
			int[] assignment) {

		long[][] usage = new long[problem.nrMachines][problem.nrResources];

		for (int p = 0; p < problem.nrProcesses; p++) {

			int m = assignment[p];

			for (int r = 0; r < problem.nrResources; r++) {
				usage[m][r] += problem.processReq[p][r];
			}
		}

		// determine totalLoadCost
		long totalLoadCost = 0;
		for (int r = 0; r < problem.nrResources; r++) {

			long loadCostR = 0;
			for (int m = 0; m < problem.nrMachines; m++) {
				loadCostR += Math.max(0, usage[m][r] - problem.safetyCap[m][r]);
			}

			totalLoadCost += problem.resourceLoadCostWeight[r] * loadCostR;
		}

		// determine totalBalanceCost
		long totalBalanceCost = 0;
		for (int b = 0; b < problem.nrBalanceObj; b++) {

			long balanceCostB = 0;

			int r1 = problem.balanceObj[b][0];
			int r2 = problem.balanceObj[b][1];
			int target = problem.balanceObj[b][2];

			for (int m = 0; m < problem.nrMachines; m++) {
				balanceCostB += Math.max(0, target
						* (problem.cap[m][r1] - usage[m][r1])
						- (problem.cap[m][r2] - usage[m][r2]));
			}

			totalBalanceCost += problem.balanceObjWeight[b] * balanceCostB;
		}

		// determine processMoveCost
		long totalProcessMoveCost = 0;
		for (int p = 0; p < problem.nrProcesses; p++) {
			if (initialAssignment[p] != assignment[p]) {
				totalProcessMoveCost += problem.processMoveCost[p];
			}
		}

		// determine serviceMoveCost
		long totalServiceMoveCost = 0;
		for (int s = 0; s < problem.nrServices; s++) {

			int movesForS = 0;

			for (int p : problem.services[s]) {
				if (initialAssignment[p] != assignment[p])
					movesForS++;
			}

			totalServiceMoveCost = Math.max(movesForS, totalServiceMoveCost);
		}

		// determine machineMoveCost
		long totalMachineMoveCost = 0;
		for (int p = 0; p < problem.nrProcesses; p++) {
			totalMachineMoveCost += problem.machineMoveCost[initialAssignment[p]][assignment[p]];
		}

		long totalCost = totalLoadCost + totalBalanceCost
				+ totalProcessMoveCost * problem.processMoveCostWeight
				+ totalServiceMoveCost * problem.serviceMoveCostWeight
				+ totalMachineMoveCost * problem.machineMoveCostWeight;

		return totalCost;
	}

	public static int[] loadSolution(File file) throws IOException {

		Scanner sc = null;
		try {
			sc = new Scanner(file);
			List<Integer> temp = new ArrayList<Integer>();

			while (sc.hasNextInt()) {
				temp.add(sc.nextInt());
			}

			int[] assignments = new int[temp.size()];

			for (int i = 0; i < temp.size(); i++) {
				assignments[i] = temp.get(i);
			}

			return assignments;
		} finally {
			if (sc != null)
				sc.close();
		}
	}

	public static void writeSolution(File file, int[] assignments)
			throws IOException {

		PrintWriter pw = new PrintWriter(file);
		try {
			for (int i : assignments) {
				pw.write(i + " ");
			}
		} finally {
			if (pw != null)
				pw.close();
		}
	}

}

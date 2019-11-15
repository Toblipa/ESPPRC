package launcher;

import java.io.IOException;
import java.util.ArrayList;

import model.ESPPRCResult;
import model.EspprcInstance;
import model.Label;
import model.VRPTWResult;
import reader.SolomonReader;
import solver.EspprcSolver;
import solver.LabellingSolver;
import solver.VrptwSolver;
import writer.ResultWriter;

public class Launcher {
	
	/**
	 * 
	 * @param directory
	 *            The path where the instance files should be found
	 * @param instanceType
	 *            The nature of the data found in the instace. This is for result
	 *            files labelling purposes
	 * @param nbCustomers
	 *            The number of clients we should read. It stops automatically if
	 *            there are no more clients
	 * @param timeLimit
	 *            The algorithm resolution time limit
	 * @param labelLimit
	 *            The limit of labels we can assign to a single node
	 * @param solomonInstances
	 *            An array with the name of the instances to run
	 * @param writeColumns
	 *            Set to "true" if you would like to generate a file with all the
	 *            columns added to de Master Problem
	 * @throws IOException
	 *             File names could not be found
	 */
	public static void runMasterSolver(
			String directory,
			String instanceType,
			int nbCustomers,
			int timeLimit,
			int labelLimit,
			String[] solomonInstances,
			boolean writeColumns
			)throws IOException {

		// Stock results in a file
		ResultWriter resultWriter = new ResultWriter("results_vrptw_" + instanceType + "_" + nbCustomers);
		resultWriter.createFile();

		resultWriter.writeMasterTitles();

		for (String instanceName : solomonInstances) {
			// Creating the instance
			EspprcInstance instance = new EspprcInstance();
			
			instance.setDuplicateOrigin(true);
			
			// Reading the instances
			SolomonReader reader = new SolomonReader(instance, directory + instanceName);
			reader.read( nbCustomers );
			
			// Preprocessing nodes
			instance.buildEdges(false);
			instance.buildSuccessors();
			instance.setName(instanceName.substring(0, instanceName.length() - 4));

			System.out.println("\n>>> Solving instance " + instanceName);

			// Introduction
			System.out.println("Solving the instance for " + instance.getNodes().length + " nodes");

			System.out.println("");
			VrptwSolver mp = new VrptwSolver(instance);

			long startTime = System.nanoTime();

			VRPTWResult result = mp.runColumnGeneration(timeLimit, labelLimit, writeColumns, false);

			long endTime = System.nanoTime();
			long timeElapsed = endTime - startTime;

			System.out.println("--------------------------------------");

			resultWriter.writeMasterResult(instance, result, timeElapsed / 1000000);
		}
		
		resultWriter.close();
	}

	/**
	 * It solves the pricing problem and writes all the labels generated through its
	 * resolution
	 * 
	 * @param directory
	 * @param nbCustomers
	 * @param timeLimit
	 * @param labelLimit
	 * @param solomonInstances
	 * @throws IOException
	 */
	public static void runLabelWriter(
			String directory,
			int nbCustomers,
			int timeLimit,
			int labelLimit,
			String[] solomonInstances
			) throws IOException {

		for (String instanceName : solomonInstances) {
			// Creating the instance
			EspprcInstance instance = new EspprcInstance();
			instance.setDuplicateOrigin(true);

			// Reading the instances
			SolomonReader reader = new SolomonReader(instance, directory + instanceName);
			reader.read(nbCustomers);

			// Preprocessing nodes
			instance.buildEdges(true);
			instance.buildSuccessors();

			// Introduction
			System.out.println("\n>>> Solving instance " + instanceName + "\n" + "Solving the instance for "
					+ instance.getNodes().length + " nodes");

			LabellingSolver solver = new LabellingSolver(instance);

			// Measure the labelling algorithm elapsed time
			long startTime = System.nanoTime();
			ArrayList<Label>[] nodeLabels = solver.genFeasibleRoutes(timeLimit, labelLimit);
			long endTime = System.nanoTime();
			long timeElapsed = endTime - startTime;

			// Get solution information
			ArrayList<Label> depotLabels = nodeLabels[nodeLabels.length - 1];

			Label minCostRoute = depotLabels.get(0);
			for (Label currentLabel : depotLabels) {
				if (currentLabel.getCost() < minCostRoute.getCost()) {
					minCostRoute = currentLabel;
				}
			}

			// Create directory
			String folderName = instanceName.substring(0, instanceName.length() - 4);
			folderName = folderName + "-" + nbCustomers;

			// Writing label lists for each node
			for (ArrayList<Label> labelList : nodeLabels) {
				// Node information
				int nodeId = labelList.get(0).getCurrent().getId();

				// Create file
				ResultWriter resultWriter = new ResultWriter("Node_" + nodeId + "-" + labelList.size(), folderName);
				resultWriter.createFile();
				
				resultWriter.writeLabels( labelList );
			}

			// Log results
			System.out.println(minCostRoute);
			System.out.println(minCostRoute.getCost());
			System.out.println("Algorithm has finished in " + (timeElapsed / 1000000) + " milliseconds");

			System.out.println("--------------------------------------");
		}
	}

	/**
	 * Given the specifications, it solves the pricing problem of the given instance
	 * generating random negative reduced costs
	 * 
	 * @param directory
	 * @param instanceType
	 * @param nbCustomers
	 * @param timeLimit
	 * @param labelLimit
	 * @param useCplex
	 * @param solomonInstances
	 * @throws IOException
	 */
	public static void runPricingSolver(String directory, String instanceType, int nbCustomers, int timeLimit,
			int labelLimit, int useCplex, String[] solomonInstances) throws IOException {
		
		// Create the file
		ResultWriter resultWriter = new ResultWriter("results_" + instanceType + "_" + nbCustomers);
		resultWriter.createFile();

		// Write the titles to file
		resultWriter.writePricingTitles(useCplex == 1);

		// Stock results in a list
		ESPPRCResult[] cplexResults = new ESPPRCResult[solomonInstances.length];
		ESPPRCResult[] labellingResults = new ESPPRCResult[solomonInstances.length];

		for (int i = 0; i < solomonInstances.length; i++) {
			// Creating the instance
			EspprcInstance instance = new EspprcInstance();
			instance.setDuplicateOrigin(true);

			// Reading the instances
			SolomonReader reader = new SolomonReader(instance, directory + solomonInstances[i]);
			reader.read(nbCustomers);

			// Preprocessing nodes
			instance.buildEdges(true);
			instance.buildSuccessors();
			instance.setName(solomonInstances[i].substring(0, solomonInstances[i].length() - 4));
			
			// Introduction
			System.out.println("\n>>> Solving instance " + solomonInstances[i] + "\n" + "Solving the instance for "
					+ instance.getNodes().length + " nodes");

			// Solving
			if (useCplex == 1) {
				cplexResults[i] = solveESPPRC(instance, timeLimit);
			}

			System.out.println("");

			labellingResults[i] = labellingAlgorithm(instance, timeLimit, labelLimit);

			// Log results
			if (useCplex == 1) {
				System.out.println(cplexResults[i].getRoute());
				System.out.println(cplexResults[i].getCost());
			}
			System.out.println(labellingResults[i].getRoute());
			System.out.println(labellingResults[i].getCost());

			System.out.println("--------------------------------------");

			// Write results in a file
			resultWriter.writePricingResults(instance, cplexResults[i], labellingResults[i]);
		}

		resultWriter.close();
	}

	/**
	 * Run the labelling algorithm described in (Feillet D, 2004)
	 * 
	 * @param instance
	 * @param timeLimit
	 * @param labelLimit
	 * @return
	 */
	private static ESPPRCResult labellingAlgorithm(EspprcInstance instance, int timeLimit, int labelLimit) {
		// We start the label correcting algorithm
		System.out.println("START: Generating feasible routes");

		// We initialize the solver
		LabellingSolver solver = new LabellingSolver(instance);

		// We start measuring the algorithm elapsed time
		long startTime = System.nanoTime();
		ArrayList<Label>[] nodeLabels = solver.genFeasibleRoutes(timeLimit, labelLimit);
		long endTime = System.nanoTime();

		// Label correcting algorithm has finished
		System.out.println("END: Generating feasible routes");

		// Get difference of two nanoTime values
		long timeElapsed = endTime - startTime;

		// Get solution information
		int depotIndex = instance.isDuplicateOrigin() ? nodeLabels.length - 1 : 0;
		ArrayList<Label> depotLabels = nodeLabels[depotIndex];

		int nbFeasibleRoutes = depotLabels.size();

		Label minCostRoute = depotLabels.get(0);
		for (Label currentLabel : depotLabels) {
			if (currentLabel.getCost() < minCostRoute.getCost()) {
				minCostRoute = currentLabel;
			}
		}

		int nbGeneratedLabels = 0;
		for (ArrayList<Label> labelList : nodeLabels) {
			nbGeneratedLabels += labelList.size();
		}

		System.out.println("Generated " + nbFeasibleRoutes + " routes");
		System.out.println("Algorithm has finished in " + (timeElapsed / 1000000) + " milliseconds");
		
		// Return
		return new ESPPRCResult(minCostRoute.getRoute(), minCostRoute.getCost(), timeElapsed / 1000000,
				minCostRoute.getNbVisitedNodes(), nbFeasibleRoutes, nbGeneratedLabels);
	}

	/**
	 * Run the linear program to solve an ESPPRC using cplex
	 * 
	 * @param instance
	 * @param timeLimit
	 * @return
	 */
	private static ESPPRCResult solveESPPRC(EspprcInstance instance, int timeLimit) {

		// Solving the instance
		EspprcSolver solver = new EspprcSolver(instance);
		ESPPRCResult result = solver.solveESPPRC(timeLimit);

		return result;
	}
}

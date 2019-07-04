package main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import model.ESPPRCResult;
import model.EspprcInstance;
import model.Label;
import model.VRPTWResult;
import model.Schedule;
import reader.SolomonReader;
import solver.EspprcSolver;
import solver.IopSolver;
import solver.LabellingSolver;
import solver.SchedulingSolver;
import solver.VrptwSolver;

public class Main {

	public static void main(String[] args) throws IOException {

		// Default options
		int nbClients = 10;
		int useCplex = 0;
		int timeLimit = 600;
		int labelLimit = 0;
		String instanceType = "C101";
		String directory = "./instances/solomon/";
		String problem = "iop";
		boolean writeColumns = false;

		// Reading arguments
		if (args.length > 0) {
			for (String arg : args) {
				if (arg.contains("-d")) {
					directory = arg.substring(3);
				} else if (arg.contains("-instance")) {
					instanceType = arg.substring(10);
				} else if (arg.contains("-clients")) {
					nbClients = Integer.parseInt(arg.substring(9));
				} else if (arg.contains("-useCplex")) {
					useCplex = Integer.parseInt(arg.substring(10));
				} else if (arg.contains("-timeLimit")) {
					timeLimit = Integer.parseInt(arg.substring(11));
				} else if (arg.contains("-labelLimit")) {
					labelLimit = Integer.parseInt(arg.substring(12));
				} else if (arg.contains("-problem")) {
					problem = arg.substring(9);
				}
			}
		}

		// Reading instance option
		String[] solomonInstances = getSelectedInstances(instanceType);

		switch (problem.toUpperCase()) {
		case "MASTER":
			runMasterSolver(directory, instanceType, nbClients, timeLimit, labelLimit, solomonInstances, writeColumns);
			break;
		case "PRICING":
			runPricingSolver(directory, instanceType, nbClients, timeLimit, labelLimit, useCplex, solomonInstances);
			break;
		case "LABEL":
			runLabelWriter(directory, nbClients, timeLimit, labelLimit, solomonInstances);
			break;
		case "MTPRICING":
			runMultiTrip(directory, nbClients, timeLimit, labelLimit, solomonInstances);
			break;
		case "SCHEDULING":
			runSchedulingSolver(directory, instanceType, nbClients, timeLimit, labelLimit, solomonInstances);
			break;
		case "IOP":
			runIopSolver(directory, instanceType, nbClients, timeLimit, labelLimit, solomonInstances, writeColumns);
			break;
		default:
			System.err.println("Could not recognise problem");
		}
	}
	
	private static void runIopSolver(String directory, String instanceType, int nbClients, int timeLimit,
			int labelLimit, String[] solomonInstances, boolean writeColumns) throws IOException {
		
		File file = new File("results_iop_" + instanceType + "_" + nbClients + ".csv");
		file.createNewFile();

		writeMasterTitles(file);

		FileWriter writer = new FileWriter(file, true);

		for (String instanceName : solomonInstances) {
			// Creating the instance
			EspprcInstance instance = new EspprcInstance();
			
			instance.setDuplicateOrigin(true);
			
			// Reading the instances
			SolomonReader reader = new SolomonReader(instance, directory + instanceName);
			reader.read(nbClients);
			
			// Preprocessing nodes
			instance.buildEdges(false);
			instance.buildSuccessors();
			instance.setName(instanceName.substring(0, instanceName.length() - 4));
			instance.setVehicles(25);

			System.out.println("\n>>> Solving instance " + instanceName);

			// Introduction
			System.out.println("Solving the instance for " + instance.getNodes().length + " nodes");

			System.out.println("");
			IopSolver mp = new IopSolver(instance);

			long startTime = System.nanoTime();

			VRPTWResult result = mp.runColumnGeneration(timeLimit, labelLimit, writeColumns, false);

			long endTime = System.nanoTime();
			long timeElapsed = endTime - startTime;

			System.out.println("--------------------------------------");

			writeMasterResult(writer, instance, result, timeElapsed / 1000000);

		}

		writer.close();
	}

	/**
	 * 
	 * @param directory
	 *            The path where the instance files should be found
	 * @param nbClients
	 *            The number of clients we should read. It stops automatically if
	 *            there are no more clients
	 * @param timeLimit
	 *            The algorithm resolution time limit
	 * @param labelLimit
	 *            The limit of labels we can assign to a single node
	 * @param solomonInstances
	 *            An array with the name of the instances to run
	 */
	private static void runMultiTrip(String directory, int nbClients, int timeLimit, int labelLimit,
			String[] solomonInstances) {

		// Stock results in a list
		ESPPRCResult[] labellingResults = new ESPPRCResult[solomonInstances.length];

		for (int i = 0; i < solomonInstances.length; i++) {
			// Creating the instance
			EspprcInstance instance = new EspprcInstance();
			instance.setDuplicateOrigin(false);

			// Reading the instances
			SolomonReader reader = new SolomonReader(instance, directory + solomonInstances[i]);
			reader.read(nbClients);

			// Preprocessing nodes
			instance.buildEdges(true);
			instance.buildSuccessors();
			instance.setName(solomonInstances[i].substring(0, solomonInstances[i].length() - 4));

			// Introduction
			System.out.println("\n>>> Solving instance " + solomonInstances[i] + "\n" + "Solving the instance for "
					+ instance.getNodes().length + " nodes");

			System.out.println("");

			labellingResults[i] = labellingAlgorithm(instance, timeLimit, labelLimit);

			// Log results
			System.out.println(labellingResults[i].getRoute());
			System.out.println(labellingResults[i].getCost());

			System.out.println("--------------------------------------");
		}
	}

	/**
	 * 
	 * @param directory
	 *            The path where the instance files should be found
	 * @param instanceType
	 *            The nature of the data found in the instace. This is for result
	 *            files labelling purposes
	 * @param nbClients
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
	private static void runMasterSolver(String directory, String instanceType, int nbClients, int timeLimit,
			int labelLimit, String[] solomonInstances, boolean writeColumns) throws IOException {

		// Stock results in a list
		File file = new File("results_vrptw_" + instanceType + "_" + nbClients + ".csv");
		file.createNewFile();

		writeMasterTitles(file);

		FileWriter writer = new FileWriter(file, true);

		for (String instanceName : solomonInstances) {
			// Creating the instance
			EspprcInstance instance = new EspprcInstance();
			
			instance.setDuplicateOrigin(true);
			
			// Reading the instances
			SolomonReader reader = new SolomonReader(instance, directory + instanceName);
			reader.read(nbClients);
			
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

			writeMasterResult(writer, instance, result, timeElapsed / 1000000);

		}

		writer.close();
	}

	/**
	 * It solves the pricing problem and writes all the labels generated through its
	 * resolution
	 * 
	 * @param directory
	 * @param nbClients
	 * @param timeLimit
	 * @param labelLimit
	 * @param solomonInstances
	 * @throws IOException
	 */
	private static void runLabelWriter(String directory, int nbClients, int timeLimit, int labelLimit,
			String[] solomonInstances) throws IOException {

		for (String instanceName : solomonInstances) {
			// Creating the instance
			EspprcInstance instance = new EspprcInstance();
			instance.setDuplicateOrigin(true);

			// Reading the instances
			SolomonReader reader = new SolomonReader(instance, directory + instanceName);
			reader.read(nbClients);

			// Preprocessing nodes
			instance.buildEdges(true);
			instance.buildSuccessors();

			// Introduction
			System.out.println("\n>>> Solving instance " + instanceName + "\n" + "Solving the instance for "
					+ instance.getNodes().length + " nodes");

			LabellingSolver solver = new LabellingSolver(instance);

			// We measure the algorithm elapsed time
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
			folderName = folderName + "-" + nbClients;
			File folder = new File(folderName);
			folder.mkdirs();

			// Writing label lists for each node
			for (ArrayList<Label> labelList : nodeLabels) {
				// Node information
				int nodeId = labelList.get(0).getCurrent().getId();

				// Create file
				File file = new File(folderName + File.separator + "Node_" + nodeId + "-" + labelList.size() + ".csv");
				file.createNewFile();

				FileWriter writer = new FileWriter(file, true);

				// Write the titles to file
				writer.write("Cost" + "\t" + "N. Nodes" + "\t" + "Time" + "\t" + "Demand" + "\t" + "N. Unreachable"
						+ "\t" + "Route" + "\n");
				writer.flush();

				// Write every label
				for (Label nodeLabel : labelList) {
					writer.write(nodeLabel.getCost() + "\t" + nodeLabel.getNbVisitedNodes() + "\t"
							+ nodeLabel.getResources().getTime() + "\t" + nodeLabel.getResources().getDemand() + "\t"
							+ nodeLabel.getResources().getNbUnreachableNodes() + "\t" + nodeLabel.getRoute() + "\n");
					writer.flush();
				}

				writer.write("Total" + "\t" + labelList.size());
				writer.close();
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
	 * @param nbClients
	 * @param timeLimit
	 * @param labelLimit
	 * @param useCplex
	 * @param solomonInstances
	 * @throws IOException
	 */
	private static void runPricingSolver(String directory, String instanceType, int nbClients, int timeLimit,
			int labelLimit, int useCplex, String[] solomonInstances) throws IOException {
		// Create the file
		File file = new File("results_" + instanceType + "_" + nbClients + ".csv");
		file.createNewFile();

		// Write the titles to file
		writePricingTitles(file, useCplex == 1);

		FileWriter writer = new FileWriter(file, true);

		// Stock results in a list
		ESPPRCResult[] cplexResults = new ESPPRCResult[solomonInstances.length];
		ESPPRCResult[] labellingResults = new ESPPRCResult[solomonInstances.length];

		for (int i = 0; i < solomonInstances.length; i++) {
			// Creating the instance
			EspprcInstance instance = new EspprcInstance();
			instance.setDuplicateOrigin(true);

			// Reading the instances
			SolomonReader reader = new SolomonReader(instance, directory + solomonInstances[i]);
			reader.read(nbClients);

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
			writePricingResults(writer, instance, cplexResults[i], labellingResults[i]);
		}

		writer.close();
	}

	/**
	 * Given the specifications, it solves the pricing problem of the given instance
	 * generating random negative reduced costs
	 * 
	 * @param directory
	 * @param instanceType
	 * @param nbClients
	 * @param timeLimit
	 * @param labelLimit
	 * @param useCplex
	 * @param solomonInstances
	 * @throws IOException
	 */
	private static void runSchedulingSolver(String directory, String instanceType, int nbClients, int timeLimit,
			int labelLimit, String[] solomonInstances) throws IOException {
		// Create the file
		File file = new File("results_scheduling_" + instanceType + "_" + nbClients + ".csv");
		file.createNewFile();

		// Write the titles to file
		writePricingTitles(file, false);

		FileWriter writer = new FileWriter(file, true);

		// Stock results in a list
		model.Schedule[] labellingResults = new Schedule[solomonInstances.length];

		for (int i = 0; i < solomonInstances.length; i++) {
			// Creating the instance
			EspprcInstance instance = new EspprcInstance();
			instance.setDuplicateOrigin(true);

			// Reading the instances
			SolomonReader reader = new SolomonReader(instance, directory + solomonInstances[i]);
			reader.read( nbClients );

			// Adapting the instance for the scheduling problem
			instance.buildScheduling( true );
			
			// Preprocessing nodes
			instance.buildSuccessors();
			instance.setName(solomonInstances[i].substring(0, solomonInstances[i].length() - 4));
			
			instance.printCostMatrix();
			instance.printSuccessors();

			// Introduction
			System.out.println("\n>>> Solving instance " + solomonInstances[i] + "\n" + "Solving the instance for "
					+ instance.getNodes().length + " nodes");

			// Solving
//			labellingResults[i] = labellingAlgorithm(instance, timeLimit, labelLimit);
			labellingResults[i] = solveSchedule(instance, timeLimit);

			// Log results
			System.out.println(labellingResults[i].getPath());
			System.out.println(labellingResults[i].getCost());

			System.out.println("--------------------------------------");

			// Write results in a file
//			writePricingResults(writer, instance, null, labellingResults[i]);
		}

		writer.close();
	}

	/**
	 * Returns an array of string containg all the instances of the given type
	 * 
	 * @param instanceType
	 *            The nature of the instances to read
	 * @return An array of strings containing file names
	 */
	private static String[] getSelectedInstances(String instanceType) {

		if (instanceType.equals("R")) {
			return SolomonReader.getRInstances();
		}

		if (instanceType.equals("C")) {
			return SolomonReader.getCInstances();
		}

		if (instanceType.equals("RC")) {
			return SolomonReader.getRCInstances();
		}

		if (instanceType.equals("All")) {
			return SolomonReader.getAllInstances();
		}

		if (instanceType.equals("Test")) {
			return SolomonReader.getTestInstances();
		}

		if (instanceType.equals("Test2")) {
			return SolomonReader.getTestInstances2();
		}

		return SolomonReader.getInstace(instanceType);
	}

	/**
	 * Writes the first line of a result file containing the column titles
	 * 
	 * @param file
	 * @param useCplex
	 * @throws IOException
	 */
	private static void writePricingTitles(File file, boolean useCplex) throws IOException {
		FileWriter writer = new FileWriter(file);
		String cplexTitles = "";
		if (useCplex) {
			cplexTitles = "Cplex Cost" + "\t" + "Cplex Eº Time [ms]" + "\t" + "Cplex Nº Visited Nodes" + "\t"
					+ "Cplex Route" + "\t";
		}

		writer.write("File name" + "\t" + "Nº Edges" + "\t" + "Density" + "\t" + "% Neg. Edges" + "\t" + cplexTitles
				+ "Label. Cost" + "\t" + "Label. E. Time [ms]" + "\t" + "Label. Nº Visited" + "\t" + "Label. Route"
				+ "\t" + "Label. Nº Feasible Routes" + "\t" + "Label. Nº Generated Routes" + "\n");

		writer.close();
	}

	/**
	 * Writes a line on a given file containg the results and the instance
	 * information
	 * 
	 * @param writer
	 * @param instance
	 * @param cplexResult
	 * @param labellingResult
	 * @throws IOException
	 */
	private static void writePricingResults(FileWriter writer, EspprcInstance instance, ESPPRCResult cplexResult,
			ESPPRCResult labellingResult) throws IOException {

		writer.write(instance.getName() + "\t");

		writer.write(instance.getNbEdges() + "\t");
		writer.write(instance.getDensity() + "\t");
		writer.write((instance.getNbNegativeEdges() / instance.getNbEdges()) + "\t");

		if (cplexResult != null) {
			writer.write(cplexResult.getCost() + "\t");
			writer.write(cplexResult.getTimeElapsed() + "\t");
			writer.write(cplexResult.getNbVisitedNodes() + "\t");
			writer.write(cplexResult.getRoute() + "\t");
		}

		writer.write(labellingResult.getCost() + "\t");
		writer.write(labellingResult.getTimeElapsed() + "\t");
		writer.write((labellingResult.getNbVisitedNodes() - 1) + "\t");
		writer.write(labellingResult.getRoute() + "\t");
		writer.write(labellingResult.getNbFeasibleRoutes() + "\t");
		writer.write(labellingResult.getNbTotalRoutes() + "\n");

		writer.flush();

	}

	/**
	 * 
	 * @param file
	 *            The instance file
	 * @throws IOException
	 *             Throws exception if file is not found or writer has not been
	 *             created properly
	 */
	private static void writeMasterTitles(File file) throws IOException {
		FileWriter writer = new FileWriter(file);

		writer.write("Instance" + "\t" +

				"Nº Edges" + "\t" + "Density" + "\t" +

				"Lower Bound" + "\t" + "Upper Bound" + "\t" + "Gap" + "\t" + "Relative Gap" + "\t" + "E. Time [ms]"
				+ "\t" + "Decision Var. Sum" + "\t" + "R. Sol. Set" + "\t" + "Int. Sol. Set" + "\t" + "Nº Intit. Routes"
				+ "\t" + "Nº Gen. Routes" + "\t" + "Nº Iterations" + "\t" + "Nº Nodes" + "\n");

		writer.close();
	}

	/**
	 * 
	 * @param writer
	 * @param instance
	 * @param result
	 * @param timeElapsed
	 * @throws IOException
	 */
	private static void writeMasterResult(FileWriter writer, EspprcInstance instance, VRPTWResult result,
			long timeElapsed) throws IOException {
		boolean solved = result.isFinished();

		writer.write(instance.getName() + "\t");

		writer.write(instance.getNbEdges() + "\t");
		writer.write(instance.getDensity() + "\t");

		writer.write(solved ? result.getLowerBound() + "\t" : "-\t");
		writer.write(result.getUpperBound() + "\t");
		writer.write(solved ? result.getGap() + "\t" : "-\t");
		writer.write(result.getMipGap() + "\t");
		writer.write(timeElapsed + "\t");
		writer.write(solved ? result.getxSum() + "\t" : "-\t");
		writer.write(solved ? result.getRelaxedSolution().size() + "\t" : "-\t");
		writer.write(result.getIntegerSolution().size() + "\t");
		writer.write(result.getInitialRoutes() + "\t");
		writer.write(result.getGeneratedRoutes() + "\t");
		writer.write(result.getIterations() + "\t");
		String routes = "";
		int visited = 0;
		for (Label solution : result.getIntegerSolution()) {
			visited += solution.getNbVisitedNodes() - 2;
			routes += "\t" + solution.getRoute();
		}

		writer.write(visited + "");
		writer.write(routes);
		// End line
		writer.write("\n");
		// Write down results
		writer.flush();
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
			System.out.println(currentLabel.getRoute());
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
	
	/**
	 * Run inear program to solve the scheduling pricing problem
	 * @param instance
	 * @param timeLimit
	 * @return
	 */
	private static Schedule solveSchedule(EspprcInstance instance, int timeLimit) {

		// Solving the instance
		SchedulingSolver solver = new SchedulingSolver(instance);
		Schedule result = solver.solveSchedule(timeLimit);

		return result;
	}
}

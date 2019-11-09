package launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import model.Customer;
import model.ESPPRCResult;
import model.EspprcInstance;
import model.Label;
import model.VRPTWResult;
import reader.SolomonReader;
import solver.EspprcSolver;
import solver.LabellingSolver;
import solver.VrptwSolver;

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

		// Stock results in a list
		File file = new File("results_vrptw_" + instanceType + "_" + nbCustomers + ".csv");
		file.createNewFile();

		writeMasterTitles(file);

		FileWriter writer = new FileWriter(file, true);

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

			writeMasterResult(writer, instance, result, timeElapsed / 1000000);

		}

		writer.close();
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
		File file = new File("results_" + instanceType + "_" + nbCustomers + ".csv");
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
			writePricingResults(writer, instance, cplexResults[i], labellingResults[i]);
		}

		writer.close();
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
	 * Writes down instance data
	 * @param instance
	 * @throws FileNotFoundException
	 */
	public static void debugData(EspprcInstance instance, String trio) throws FileNotFoundException {
		// Create directory
		String folderName = "data-"+trio;
		File folder = new File(folderName);
		folder.mkdirs();
		
        // Creating a File object that represents the disk file.
        PrintStream o = new PrintStream(new File(folderName + File.separator + instance.getName()+"_"+instance.getNbNodes()+"_data.txt"));
  
        // Store current System.out before assigning a new value
        PrintStream console = System.out;
  
        // Assign o to output stream
        System.setOut(o);
        
        // Print data
		System.out.println("Cost matrix");
		instance.printCostMatrix();
		System.out.println("");
		System.out.println("Distance matrix");
		instance.printDistanceMatrix();
		System.out.println("");
		System.out.println("nº: S. Time, [start : end]");
		for(Customer n : instance.getNodes()) {
			System.out.println(n.getId()+": "+n.getServiceTime()+", ["+n.getStart()+" : "+n.getEnd()+"]");
		}
		
		System.setOut(console);
	}
}

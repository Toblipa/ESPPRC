package main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import model.ESPPRCResult;
import model.EspprcInstance;
import model.Label;
import reader.SolomonReader;
import solver.EspprcSolver;
import solver.LabellingSolver;

public class Main {

	public static void main(String[] args) throws IOException {
		
		// Default options
		int nbClients = 50;
		int useCplex = 0;
		int timeLimit = 300;
		String instanceType = "Test";
		String directory = "./instances/solomon_"+nbClients+"/";
		
		// Reading arguments
		if(args.length  > 0) {
			for(String arg : args) {
				if(arg.contains("-d")) {
					directory = arg.substring(3);
				}
				else if(arg.contains("-instance")) { 
					instanceType = arg.substring(10);
				}
				else if(arg.contains("-clients")) {
					nbClients = Integer.parseInt(arg.substring(9));
				}
				else if(arg.contains("-useCplex")) {
					useCplex = Integer.parseInt(arg.substring(10));
				}
				else if(arg.contains("-timeLimit")) {
					timeLimit = Integer.parseInt(arg.substring(11));
				}
			}
		}
		
		// Reading instance option
		String[] solomonInstances;
		if(instanceType.equals("R")) {
			solomonInstances = getRInstances();
		}
		else if(instanceType.equals("C")) {
			solomonInstances = getCInstances();
		}
		else if(instanceType.equals("RC")) {
			solomonInstances = getRCInstances();
		}
		else if(instanceType.equals("Test")) {
			solomonInstances = getTestInstances();
		}
		else if(instanceType.equals("Test2")) {
			solomonInstances = getTestInstances2();
		}
		else {
			solomonInstances = getRInstances();
		}
		
		// Create the file
		File file = new File("results_"+instanceType+"_"+nbClients+".txt");
		file.createNewFile();

		// Write the titles to file
		writeColTitles(file);
		
		FileWriter writer = new FileWriter(file, true);
		
		// Stock results in a list
		ESPPRCResult[] cplexResults = new ESPPRCResult[solomonInstances.length];
		ESPPRCResult[] labellingResults = new ESPPRCResult[solomonInstances.length];
		
		for(int i = 0; i < solomonInstances.length; i++) {
			// Creating the instance
			EspprcInstance instance = new EspprcInstance();
			instance.setDuplicateOrigin(true);
			
			// Reading the instances
			SolomonReader reader = new SolomonReader(instance, directory+solomonInstances[i]);
			reader.read();
			
			// Preprocessing nodes
			instance.buildArcs();
			instance.buildSuccessors();
			
			System.out.println("\n>>> Solving instance "+solomonInstances[i]);
			
	        // Introduction
	        System.out.println("Solving the instance for "+instance.getNodes().length+" nodes");
	        
			// Solving
	        if(useCplex == 1) {
				cplexResults[i] = solveESPPRC(instance, timeLimit);
	        }
	        else {
	        	cplexResults[i] = new ESPPRCResult("",0,0,0);
	        }
	        
			System.out.println("");
			
			labellingResults[i] = labellingAlgorithm(instance, timeLimit);
			
			// Log results
			System.out.println(cplexResults[i].getRoute());
			System.out.println(cplexResults[i].getCost());
			
			System.out.println(labellingResults[i].getRoute());
			System.out.println(labellingResults[i].getCost());
			
			System.out.println("--------------------------------------");
			
			// Write results in a file
			writeResults(writer, solomonInstances[i], instance, cplexResults[i], labellingResults[i]);
		}
		
		writer.close();
	}

	private static void writeColTitles(File file) throws IOException {
		FileWriter writer = new FileWriter(file);
		
		writer.write( "File name"+"\t" +
				"N. Arcs" + "\t" +
				"Density" + "\t" +
				"% Neg. Arcs" + "\t" +
				"Cplex Cost" + "\t" +
				"Cplex E. Time [ms]" + "\t" +
				"Cplex N. Visited Nodes" + "\t" +
				"Cplex Route" + "\t" +
				"Label. Cost" + "\t" +
				"Label. E. Time [ms]" + "\t" +
				"Label. N. Visited" + "\t" +
				"Label. Route" + "\t" + 
				"Label. N. Feasible Routes" + "\t" +
				"Label. N. Generated Routes" + "\n" );
		
		writer.close();
		
	}

	private static void writeResults(FileWriter writer, String solomonInstance, EspprcInstance instance,
			ESPPRCResult cplexResult, ESPPRCResult labellingResult) throws IOException {
		
		writer.write( solomonInstance + "\t" );
		
		writer.write( instance.getNbArcs() + "\t" );
		writer.write( instance.getDensity() + "\t" );
		writer.write( ( instance.getNbNegativeArcs()/instance.getNbArcs() ) + "\t" );
		
		writer.write( cplexResult.getCost() + "\t" );
		writer.write( cplexResult.getTimeElapsed() + "\t" );
		writer.write( cplexResult.getNbVisitedNodes() + "\t" );
		writer.write( cplexResult.getRoute() + "\t" );
		
		writer.write( labellingResult.getCost() + "\t" );
		writer.write( labellingResult.getTimeElapsed() + "\t" );
		writer.write( (labellingResult.getNbVisitedNodes()-1) + "\t" );
		writer.write( labellingResult.getRoute() + "\t" );
		writer.write( labellingResult.getNbFeasibleRoutes() + "\t" );
		writer.write( labellingResult.getNbTotalRoutes() + "\n" );
		
		writer.flush();
		
	}

	/**
	 * Run the labelling algorithm described in (Feillet D, 2004)
	 * @param fileName
	 * @return
	 */
	public static ESPPRCResult labellingAlgorithm(EspprcInstance instance, int timeLimit) {
        
        // We start the label correcting algorithm
        System.out.println("START: Generating feasible routes");
        
        // We initialize the solver
        LabellingSolver solver = new LabellingSolver(instance);
        
        // We start measuring the algorithm elapsed time
		long startTime = System.nanoTime();
		
        ArrayList<Label>[] nodeLabels = solver.genFeasibleRoutes(timeLimit);

		long endTime = System.nanoTime();

    	// Label correcting algorithm has finished
        System.out.println("END: Generating feasible routes");
        
		// Get difference of two nanoTime values
		long timeElapsed = endTime - startTime;
		
    	ArrayList<Label> depotLabels = nodeLabels[nodeLabels.length - 1];

    	int nbFeasibleRoutes = depotLabels.size();
		
    	Label minCostRoute = depotLabels.get(0);
		for ( Iterator<Label> iterator = depotLabels.iterator(); iterator.hasNext(); ) {
			Label currentLabel = iterator.next();
    		if(currentLabel.getCost() < minCostRoute.getCost()) {
    			minCostRoute = currentLabel;
    		}
    	}
    	
    	int nbGeneratedLabels = 0;
    	for(ArrayList<Label> labelList : nodeLabels) {
    		nbGeneratedLabels += labelList.size();
    	}
    	
        System.out.println("Generated "+nbFeasibleRoutes+" routes");
    	System.out.println("Algorithm has finished in "+(timeElapsed/1000000)+" milliseconds");
		
		return new ESPPRCResult(minCostRoute.getRoute(), minCostRoute.getCost(), timeElapsed/1000000,
				minCostRoute.getNbVisitedNodes(), nbFeasibleRoutes, nbGeneratedLabels);
	}
	
	/**
	 * Run the linear program to solve an ESPPRC using cplex
	 * @param fileName
	 * @return
	 */
	public static ESPPRCResult solveESPPRC(EspprcInstance instance, int timeLimit) {
		
		// Solving the instance
		EspprcSolver solver = new EspprcSolver(instance);
		ESPPRCResult result = solver.solveESPPRC(timeLimit);
		
		return result;
	}
	
	private static String[] getCInstances() {
		String[] instances = new String[9];
		
		instances[0] = "C101.txt";
		instances[1] = "C102.txt";
		instances[2] = "C103.txt";
		instances[3] = "C104.txt";
		instances[4] = "C105.txt";
		instances[5] = "C106.txt";
		instances[6] = "C107.txt";
		instances[7] = "C108.txt";
		instances[8] = "C109.txt";
		
		return instances;
	}
	
	private static String[] getRInstances() {
		String[] instances = new String[12];
		
		instances[0] = "R101.txt";
		instances[1] = "R102.txt";
		instances[2] = "R103.txt";
		instances[3] = "R104.txt";
		instances[4] = "R105.txt";
		instances[5] = "R106.txt";
		instances[6] = "R107.txt";
		instances[7] = "R108.txt";
		instances[8] = "R109.txt";
		instances[9] = "R110.txt";
		instances[10] = "R111.txt";
		instances[11] = "R112.txt";

		return instances;
	}
	
	private static String[] getRCInstances() {
		String[] instances = new String[8];
		
		instances[0] = "RC101.txt";
		instances[1] = "RC102.txt";
		instances[2] = "RC103.txt";
		instances[3] = "RC104.txt";
		instances[4] = "RC105.txt";
		instances[5] = "RC106.txt";
		instances[6] = "RC107.txt";
		instances[7] = "RC108.txt";

		return instances;
	}
	
	private static String[] getTestInstances() {
		String[] instances = new String[80];
		for(int i = 0 ; i < 10 ; i++) {
			instances[i] = "RC103.txt";
		}
		for(int i = 10 ; i < 20 ; i++) {
			instances[i] = "RC108.txt";
		}
		for(int i = 20 ; i < 30 ; i++) {
			instances[i] = "C102.txt";
		}
		for(int i = 30 ; i < 40 ; i++) {
			instances[i] = "R102.txt";
		}
		for(int i = 40 ; i < 50 ; i++) {
			instances[i] = "R106.txt";
		}
		for(int i = 50 ; i < 60 ; i++) {
			instances[i] = "R101.txt";
		}
		for(int i = 60 ; i < 70 ; i++) {
			instances[i] = "C101.txt";
		}
		for(int i = 70 ; i < 80 ; i++) {
			instances[i] = "RC101.txt";
		}
		return instances;
	}
	
	private static String[] getTestInstances2() {
		String[] instances = new String[10];
		for(int i = 0 ; i < 5 ; i++) {
			instances[i] = "C103.txt";
		}
		for(int i = 5 ; i < 10 ; i++) {
			instances[i] = "C104.txt";
		}
		return instances;
	}
}

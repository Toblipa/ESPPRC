package main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import model.Customer;
import model.ESPPRCResult;
import model.EspprcInstance;
import model.Label;
import reader.SolomonReader;
import solver.EspprcSolver;

public class Main {

	public static void main(String[] args) throws IOException {
		int nbClients = 50;
		int useCplex = 1;
		int timeLimit = 600;
		String instanceType = "C";
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
				else if(arg.contains("-tiCplex")) {
					timeLimit = Integer.parseInt(arg.substring(9));
				}
			}
		}
		
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
		else {
			solomonInstances = getRInstances();
		}

		ESPPRCResult[] cplexResults = new ESPPRCResult[solomonInstances.length];
		ESPPRCResult[] labelingResults = new ESPPRCResult[solomonInstances.length];

		File file = new File("results_"+instanceType+"_"+nbClients+".txt");

		// creates the file
		file.createNewFile();

		// creates a FileWriter Object
		FileWriter writer = new FileWriter(file);
		
		writer.write( "File name"+"\t" +
				"Cplex Cost" + "\t" +
				"Cplex Elapsed Time [ms]" + "\t" +
				"Cplex N. Visited Nodes" + "\t" +
				"Cplex Route" + "\t" +
				"Labeling Cost" + "\t" +
				"Labeling Elapsed Time [ms]" + "\t" +
				"Labeling N. Visited Nodes" + "\t" +
				"Labeling Route" + "\t" + 
				"Labeling N. Feasible Routes" + "\t" +
				"Labeling N. Generated Routes" + "\n" );
		
		writer.close();
		
		writer = new FileWriter(file, true);
		
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
			
			labelingResults[i] = labelingAlgorithm(instance);
			
			// Log results
			System.out.println(cplexResults[i].getRoute());
			System.out.println(cplexResults[i].getCost());
			
			System.out.println(labelingResults[i].getRoute());
			System.out.println(labelingResults[i].getCost());
			
			System.out.println("--------------------------------------");
			
			// Write results in a file			
			writer.write(solomonInstances[i]+"\t");
			
			writer.write(cplexResults[i].getCost()+"\t");
			writer.write(cplexResults[i].getTimeElapsed()+"\t");
			writer.write(cplexResults[i].getNbVisitedNodes()+"\t");
			writer.write(cplexResults[i].getRoute()+"\t");
			
			writer.write(labelingResults[i].getCost()+"\t");
			writer.write(labelingResults[i].getTimeElapsed()+"\t");
			writer.write(labelingResults[i].getNbVisitedNodes()+"\t");
			writer.write(labelingResults[i].getRoute()+"\t");
			writer.write(labelingResults[i].getNbFeasibleRoutes()+"\t");
			writer.write(labelingResults[i].getNbTotalRoutes()+"\n");
			
			writer.flush();
		}
		
		writer.close();
	}

	/**
	 * Run the labeling algorithm described in (Feillet D, 2004)
	 * @param fileName
	 * @return
	 */
	public static ESPPRCResult labelingAlgorithm(EspprcInstance instance) {
        
        // We start the label correcting algorithm
        System.out.println("START: Generating feasible routes");
        
        // We start measuring the algorithm elapsed time
		long startTime = System.nanoTime();
		
        ArrayList<Label>[] nodeLabels = instance.genFeasibleRoutes();
        
		long endTime = System.nanoTime();

    	// Label correcting algorithm has finished
        System.out.println("END: Generating feasible routes");
        
		// Get difference of two nanoTime values
		long timeElapsed = endTime - startTime;
		
    	ArrayList<Label> depotLabels = nodeLabels[nodeLabels.length - 1];
    	int nbFeasibleRoutes = depotLabels.size();
		
    	Label minCostRoute = depotLabels.get(0);
    	for(int l=1; l < nbFeasibleRoutes; l++) {
    		if(depotLabels.get(l).getCost() < minCostRoute.getCost()) {
    			minCostRoute = depotLabels.get(l);
    		}
    	}
    	
    	int nbGeneratedLabels = 0;
    	for(int i = 0; i < nodeLabels.length; i++) {
    		nbGeneratedLabels += nodeLabels[i].size();
    	}
    	
        System.out.println("Generated "+nbFeasibleRoutes+" routes");
    	System.out.println("Algorithm has finished in "+(timeElapsed/1000000)+" milliseconds");
		
		return new ESPPRCResult(minCostRoute.getRoute(), minCostRoute.getCost(), timeElapsed/1000000, minCostRoute.getNbVisitedNodes(), nbFeasibleRoutes, nbGeneratedLabels);
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
	
	/**
	 * For debug purposes
	 * @param successors
	 */
	@SuppressWarnings("unused")
	private static void printSuccessors(ArrayList<Customer>[] successors) {
		for(int i = 0; i < successors.length; i++) {
			System.out.print("Node "+i+": ");
			for(int s = 0; s < successors[i].size(); s++) {
				System.out.print(successors[i].get(s).getCustomerId()+", ");
			}
			System.out.println("");
		}
	}
	
	/**
	 * For debug purposes
	 * @param cost
	 */
	@SuppressWarnings("unused")
	private static void printCostMatrix(double[][] cost) {
		for( int i = 0; i < cost.length; i++ ) {
			for( int j= 0; j < cost[i].length; j++ ) {
				System.out.print( Math.floor(cost[i][j]*10)/10+" ");
			}
			System.out.println("");
		}
	}
	
	@SuppressWarnings("unused")
	private static String[] getCInstances() {
		String[] instances = new String[9];
		
		instances[0] = "C101.txt";
		instances[1] = "C102.txt";
		instances[2] = "C108.txt";
		instances[3] = "C109.txt";
		instances[4] = "C105.txt";
		instances[5] = "C106.txt";
		instances[6] = "C107.txt";
		instances[7] = "C104.txt";
		instances[8] = "C103.txt";
		
//		instances[9] = "C201.txt";
//		instances[10] = "C202.txt";
//		instances[11] = "C203.txt";
//		instances[12] = "C204.txt";
//		instances[13] = "C205.txt";
//		instances[14] = "C206.txt";
//		instances[15] = "C207.txt";
//		instances[16] = "C208.txt";
		
		return instances;
	}
	
	@SuppressWarnings("unused")
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
		
//		instances[12] = "R201.txt";
//		instances[13] = "R202.txt";
//		instances[14] = "R203.txt";
//		instances[15] = "R204.txt";
//		instances[16] = "R205.txt";
//		instances[17] = "R206.txt";
//		instances[18] = "R207.txt";
//		instances[19] = "R208.txt";
//		instances[20] = "R209.txt";
//		instances[21] = "R210.txt";
//		instances[22] = "R211.txt";

		return instances;
	}
	
	@SuppressWarnings("unused")
	private static String[] getRCInstances() {
		String[] instances = new String[8];
		
		instances[0] = "RC101.txt";
		instances[1] = "RC102.txt";
		instances[2] = "RC108.txt";
		instances[3] = "RC107.txt";
		instances[4] = "RC105.txt";
		instances[5] = "RC106.txt";
		instances[6] = "RC104.txt";
		instances[7] = "RC103.txt";
		
//		instances[8] = "RC201.txt";
//		instances[9] = "RC202.txt";
//		instances[10] = "RC203.txt";
//		instances[11] = "RC204.txt";
//		instances[12] = "RC205.txt";
//		instances[13] = "RC206.txt";
//		instances[14] = "RC207.txt";
//		instances[15] = "RC208.txt";

		return instances;
	}
}

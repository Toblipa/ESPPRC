package main;

import java.io.BufferedWriter;
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

	public static void main(String[] args) {
		String[] solomonInstances = getCInstances(25);
		ESPPRCResult[] cplexResults = new ESPPRCResult[solomonInstances.length];
		ESPPRCResult[] labelingResults = new ESPPRCResult[solomonInstances.length];
		
		for(int i = 0; i < solomonInstances.length; i++) {
			
			// Creating the instance
			EspprcInstance instance = new EspprcInstance();
			instance.setDuplicateOrigin(true);
			
			// Reading the instances
			SolomonReader reader = new SolomonReader(instance, solomonInstances[i]);
			reader.read();
			
			// Preprocessing nodes
			instance.buildCosts();
			instance.buildSuccessors();
			
			System.out.println("\n>>> Solving instance "+solomonInstances[i]);

			// Solving
			cplexResults[i] = solveESPPRC(instance);
			
			System.out.println("");
			
			labelingResults[i] = labelingAlgorithm(instance);
			
			System.out.println(cplexResults[i].getRoute());
			System.out.println(cplexResults[i].getCost());
			
			System.out.println(labelingResults[i].getRoute());
			System.out.println(labelingResults[i].getCost());
			
			System.out.println("--------------------------------------");

		}
		
		try {
			writeResults(solomonInstances, cplexResults, labelingResults);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Writes cplex and labeling results into a file named "results.txt"
	 * @param solomonInstances
	 * @param cplexResults
	 * @param labelingResults
	 * @throws IOException
	 */
	private static void writeResults(String[] solomonInstances, ESPPRCResult[] cplexResults, ESPPRCResult[] labelingResults) throws IOException {
	     
	    BufferedWriter writer;
		writer = new BufferedWriter(new FileWriter("results.txt"));
		
		writer.write( "File name"+"\t" +
					"Cplex Cost" + "\t" +
					"Labeling Cost" + "\t" +
					"Cplex Elapsed Time [ms]" + "\t" +
					"Labeling Elapsed Time [ms]" + "\n" );
		for(int i = 0; i < solomonInstances.length; i++) {
			writer.write(solomonInstances[i]+"\t");
			
			writer.write(cplexResults[i].getCost()+"\t");
			writer.write(labelingResults[i].getCost()+"\t");

			writer.write(cplexResults[i].getTimeElapsed()+"\t");
			writer.write(labelingResults[i].getTimeElapsed()+"");

			writer.write("\n");
		}
		writer.close();
	}

	/**
	 * Run the labeling algorithm described in (Feillet D, 2004)
	 * @param fileName
	 * @return
	 */
	public static ESPPRCResult labelingAlgorithm(EspprcInstance instance) {
		
        // Introduction
        System.out.println("Solving the instance for "+instance.getNodes().length+" nodes");
        
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
    	
        System.out.println("Generated "+nbFeasibleRoutes+" routes");
    	System.out.println("Algorithm has finished in "+(timeElapsed/1000000)+" milliseconds");
		
		return new ESPPRCResult(minCostRoute.getRoute(), minCostRoute.getCost(), timeElapsed/1000000);
	}
	
	/**
	 * Run the linear program to solve an ESPPRC using cplex
	 * @param fileName
	 * @return
	 */
	public static ESPPRCResult solveESPPRC(EspprcInstance instance) {
		
		// Solving the instance
		EspprcSolver solver = new EspprcSolver(instance);
		ESPPRCResult result = solver.solveESPPRC();
		
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
	private static String[] getCInstances(int nbClients) {
		String[] instances = new String[17];
		
		instances[0] = "instances/solomon_"+nbClients+"/C101.txt";
		instances[1] = "instances/solomon_"+nbClients+"/C102.txt";
		instances[2] = "instances/solomon_"+nbClients+"/C103.txt";
		instances[3] = "instances/solomon_"+nbClients+"/C104.txt";
		instances[4] = "instances/solomon_"+nbClients+"/C105.txt";
		instances[5] = "instances/solomon_"+nbClients+"/C106.txt";
		instances[6] = "instances/solomon_"+nbClients+"/C107.txt";
		instances[7] = "instances/solomon_"+nbClients+"/C108.txt";
		instances[8] = "instances/solomon_"+nbClients+"/C109.txt";
		
		instances[9] = "instances/solomon_"+nbClients+"/C201.txt";
		instances[10] = "instances/solomon_"+nbClients+"/C202.txt";
		instances[11] = "instances/solomon_"+nbClients+"/C203.txt";
		instances[12] = "instances/solomon_"+nbClients+"/C204.txt";
		instances[13] = "instances/solomon_"+nbClients+"/C205.txt";
		instances[14] = "instances/solomon_"+nbClients+"/C206.txt";
		instances[15] = "instances/solomon_"+nbClients+"/C207.txt";
		instances[16] = "instances/solomon_"+nbClients+"/C208.txt";
		
		return instances;
	}
	
	@SuppressWarnings("unused")
	private static String[] getRInstances(int nbClients) {
		String[] instances = new String[23];
		
		instances[0] = "instances/solomon_"+nbClients+"/R101.txt";
		instances[1] = "instances/solomon_"+nbClients+"/R102.txt";
		instances[2] = "instances/solomon_"+nbClients+"/R103.txt";
		instances[3] = "instances/solomon_"+nbClients+"/R104.txt";
		instances[4] = "instances/solomon_"+nbClients+"/R105.txt";
		instances[5] = "instances/solomon_"+nbClients+"/R106.txt";
		instances[6] = "instances/solomon_"+nbClients+"/R107.txt";
		instances[7] = "instances/solomon_"+nbClients+"/R108.txt";
		instances[8] = "instances/solomon_"+nbClients+"/R109.txt";
		instances[9] = "instances/solomon_"+nbClients+"/R110.txt";
		instances[10] = "instances/solomon_"+nbClients+"/R111.txt";
		instances[11] = "instances/solomon_"+nbClients+"/R112.txt";
		
		instances[12] = "instances/solomon_"+nbClients+"/R201.txt";
		instances[13] = "instances/solomon_"+nbClients+"/R202.txt";
		instances[14] = "instances/solomon_"+nbClients+"/R203.txt";
		instances[15] = "instances/solomon_"+nbClients+"/R204.txt";
		instances[16] = "instances/solomon_"+nbClients+"/R205.txt";
		instances[17] = "instances/solomon_"+nbClients+"/R206.txt";
		instances[18] = "instances/solomon_"+nbClients+"/R207.txt";
		instances[19] = "instances/solomon_"+nbClients+"/R208.txt";
		instances[20] = "instances/solomon_"+nbClients+"/R209.txt";
		instances[21] = "instances/solomon_"+nbClients+"/R210.txt";
		instances[22] = "instances/solomon_"+nbClients+"/R211.txt";

		return instances;
	}
	
	@SuppressWarnings("unused")
	private static String[] getRCInstances(int nbClients) {
		String[] instances = new String[16];
		
		instances[0] = "instances/solomon_"+nbClients+"/RC101.txt";
		instances[1] = "instances/solomon_"+nbClients+"/RC102.txt";
		instances[2] = "instances/solomon_"+nbClients+"/RC103.txt";
		instances[3] = "instances/solomon_"+nbClients+"/RC104.txt";
		instances[4] = "instances/solomon_"+nbClients+"/RC105.txt";
		instances[5] = "instances/solomon_"+nbClients+"/RC106.txt";
		instances[6] = "instances/solomon_"+nbClients+"/RC107.txt";
		instances[7] = "instances/solomon_"+nbClients+"/RC108.txt";
		instances[8] = "instances/solomon_"+nbClients+"/RC201.txt";
		instances[9] = "instances/solomon_"+nbClients+"/RC202.txt";
		instances[10] = "instances/solomon_"+nbClients+"/RC203.txt";
		instances[11] = "instances/solomon_"+nbClients+"/RC204.txt";
		instances[12] = "instances/solomon_"+nbClients+"/RC205.txt";
		instances[13] = "instances/solomon_"+nbClients+"/RC206.txt";
		instances[14] = "instances/solomon_"+nbClients+"/RC207.txt";
		instances[15] = "instances/solomon_"+nbClients+"/RC208.txt";

		return instances;
	}
}

package main;

import model.EspprcInstance;
import reader.SolomonReader;
//import solver.Solver;

public class Main {

	public static void main(String[] args) {
		solomonESPPRC();
	}
	
	public static void solomonESPPRC() {
		EspprcInstance instance = new EspprcInstance();
		String file = "instances/solomon_25/R101.txt";
		
		SolomonReader reader = new SolomonReader(instance, file);		
		reader.read();
		instance.buildCosts();
		
//		Solver solver = new Solver(instance);		
//		solver.solveVRP();
	}

}

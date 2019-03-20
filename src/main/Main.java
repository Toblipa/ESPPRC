package main;

import model.EspprcInstance;
import reader.SolomonReader;
import solver.Solver;

public class Main {

	public static void main(String[] args) {
		solomonESPPRC();
	}
	
	public static void solomonESPPRC() {
		EspprcInstance instance = new EspprcInstance();
		instance.setDuplicateOrigin(true);
		String file = "instances/solomon_25/C101.txt";
		
		SolomonReader reader = new SolomonReader(instance, file);		
		reader.read();
		instance.buildCosts();
		instance.buildSuccessors();
		
		Solver solver = new Solver(instance);		
		solver.solveESPPRC();
	}

}

package main;

import java.io.IOException;

import launcher.Launcher;
import reader.SolomonReader;

public class Main {

	public static void main(String[] args) throws IOException {

		// Default options
		int nbCustomers = 25;
		int useCplex = 1;
		int timeLimit = 300;
		int labelLimit = 0;
		String instanceType = "All";
		String directory = "./instances/solomon/";
		String problem = "pricing";
		boolean writeColumns = true;

		// Reading arguments
		if (args.length > 0) {
			for (String arg : args) {
				if (arg.contains("-d")) {
					directory = arg.substring(3);
				} else if (arg.contains("-instance")) {
					instanceType = arg.substring(10);
				} else if (arg.contains("-customers")) {
					nbCustomers = Integer.parseInt(arg.substring(9));
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
			Launcher.runMasterSolver(directory, instanceType, nbCustomers, timeLimit, labelLimit, solomonInstances, writeColumns);
			break;
		case "PRICING":
			Launcher.runPricingSolver(directory, instanceType, nbCustomers, timeLimit, labelLimit, useCplex, solomonInstances);
			break;
		case "LABEL":
			Launcher.runLabelWriter(directory, nbCustomers, timeLimit, labelLimit, solomonInstances);
			break;
		default:
			System.err.println("Could not recognise problem");
		}
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

		return SolomonReader.getInstace(instanceType);
	}
	
}

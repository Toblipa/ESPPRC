package writer;

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

public class ResultWriter {
	
	String fileName;
	String filePath;
	File file;
	FileWriter writer;
	
	/** Empty object constructor */
	public ResultWriter() {
		fileName = null;
		filePath = null;
		file = null;
		writer = null;
	}
	
	public ResultWriter(String fileName) {
		this.fileName = fileName;
		this.filePath = "";
	}
	
	public ResultWriter(String fileName, String filePath) {
		this.fileName = fileName;
		this.filePath = filePath;
		
	}
	
	public void createFile() {
		try{
			if( filePath != null && !filePath.equals("") ) {
				File folder = new File(filePath);
				folder.mkdirs();
				file = new File(filePath + File.separator + fileName + ".csv");
			}
			else {
				file = new File(fileName + ".csv");
			}
			file.createNewFile();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Writes the first line of a result file containing the column titles
	 * 
	 * @param file
	 * @param useCplex
	 * @throws IOException
	 */
	public void writePricingTitles(boolean useCplex) throws IOException {
		writer = new FileWriter(file);
		String cplexTitles = "";
		if (useCplex) {
			cplexTitles = "Cplex Cost" + "\t" + "Cplex Eº Time [ms]" + "\t" + "Cplex Nº Visited Nodes" + "\t"
					+ "Cplex Route" + "\t";
		}

		writer.write("File name" + "\t" + "Nº Edges" + "\t" + "Density" + "\t" + "% Neg. Edges" + "\t" + cplexTitles
				+ "Label. Cost" + "\t" + "Label. E. Time [ms]" + "\t" + "Label. Nº Visited" + "\t" + "Label. Route"
				+ "\t" + "Label. Nº Feasible Routes" + "\t" + "Label. Nº Generated Routes" + "\n");

		writer.close();
		
		writer = new FileWriter(file, true);
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
	public void writePricingResults(EspprcInstance instance, ESPPRCResult cplexResult, ESPPRCResult labellingResult)
			throws IOException {

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
	public void writeMasterTitles() throws IOException {
		writer = new FileWriter(file);

		writer.write("Instance" + "\t" +

				"Nº Edges" + "\t" + "Density" + "\t" +

				"Lower Bound" + "\t" + "Upper Bound" + "\t" + "Gap" + "\t" + "Relative Gap" + "\t" + "E. Time [ms]"
				+ "\t" + "Decision Var. Sum" + "\t" + "R. Sol. Set" + "\t" + "Int. Sol. Set" + "\t" + "Nº Intit. Routes"
				+ "\t" + "Nº Gen. Routes" + "\t" + "Nº Iterations" + "\t" + "Nº Nodes" + "\n");

		writer.close();
		
		writer = new FileWriter(file, true);
	}

	/**
	 * 
	 * @param writer
	 * @param instance
	 * @param result
	 * @param timeElapsed
	 * @throws IOException
	 */
	public void writeMasterResult(EspprcInstance instance, VRPTWResult result, long timeElapsed)
			throws IOException {
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

	public void writeLabels(ArrayList<Label> labelList) throws IOException {
		writer = new FileWriter(file);
		
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
	
	public void close() {
		if( writer == null ) {
			return;
		}
		
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	/**
	 * Writes down instance data
	 * @param instance
	 * @throws FileNotFoundException
	 */
	public static void debugData(EspprcInstance instance, String testName) throws FileNotFoundException {
		// Create directory
		String folderName = "data-"+testName;
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

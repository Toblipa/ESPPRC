package reader;

import model.Customer;
import model.EspprcInstance;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class SolomonReader {
	
	/**
	 * The instance to stock file data
	 */
	private EspprcInstance instance;
	
	/**
	 * The name of the file for reading
	 */
	private String file;
	
	/**
	 * 
	 * @param instance
	 * @param file
	 */
	public SolomonReader(EspprcInstance instance, String file){
		this.instance = instance;
		this.file = file;
	}
	
	/**
	 * 
	 * @param nbClients
	 */
	public void read(int nbClients) {
		try {
			FileReader reader = new FileReader(this.file);
			BufferedReader br = new BufferedReader(reader);

			int counter = 0;
			String line;
			while ((line = br.readLine()) != null && counter < nbClients+10) {
				counter++;
				this.readInstace(line, counter, nbClients);
			}

			br.close();
			reader.close();
		} catch (FileNotFoundException ex) {
			System.out.println("File " + file + " not found!");
		} catch (IOException ex) {
			System.out.println(ex);
		}
	}

	/**
	 * 
	 * @param line
	 * @param counter
	 */
	private void readInstace(String line, int counter, int nbClients) {

		line = line.replace("\r", "");
		line = line.trim();
		String[] tokens = line.split(" +");

		if (counter == 5) {
			this.readVehicle(tokens);
		}
		else if (counter == 10) {
			// origin node
			this.instance.setNodes(new Customer[this.instance.isDuplicateOrigin() ? nbClients+2 : nbClients+1]);
			
			this.readOrigin(tokens);
		}
		else if (counter > 10 && tokens.length == 7){
			// customers
			this.readCustomer(tokens);
		}
	}
	
	/**
	 * 
	 * @param tokens
	 */
	private void readOrigin(String[] tokens) {
		// The origin node
		this.readCustomer(tokens);
		
		if( this.instance.isDuplicateOrigin() ) {
			// We add a duplicate of the depot at the end
			int depotId = this.instance.getNodes().length - 1;
			
			Customer depot = new Customer( Double.parseDouble(tokens[1]), Double.parseDouble(tokens[2]) );
			
			depot.setId( depotId );
			depot.setDemand( Integer.parseInt(tokens[3]) );
			depot.setStart( Double.parseDouble(tokens[4]) );
			depot.setEnd( Double.parseDouble(tokens[5]) );
			depot.setServiceTime( Double.parseDouble(tokens[6]) );
			depot.setDepot(true);
			
			// Add the node to the instance
			this.instance.getNodes()[depotId] = depot;
		}
		
	}
	
	/**
	 * 
	 * @param tokens
	 */
	private void readCustomer(String[] tokens) {
		Customer customer = new Customer( Double.parseDouble(tokens[1]), Double.parseDouble(tokens[2]) );
		
		customer.setId( Integer.parseInt(tokens[0]) );
		customer.setDemand( Integer.parseInt(tokens[3]) );
		customer.setStart( Double.parseDouble(tokens[4]) );
		customer.setEnd( Double.parseDouble(tokens[5]) );
		customer.setServiceTime( Double.parseDouble(tokens[6]) );
		
		// Add the node to the instance
		this.instance.getNodes()[customer.getId()] = customer;
	}

	/**
	 * 
	 * @param tokens
	 */
	private void readVehicle(String[] tokens) {
		this.instance.setVehicles( Integer.parseInt(tokens[0]) );
		this.instance.setCapacity( Double.parseDouble(tokens[1]) );
	}
	
	/**
	 * 
	 * @return list of C instances
	 */
	public static String[] getCInstances() {
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
	
	/**
	 * 
	 * @return list of R instances
	 */
	public static String[] getRInstances() {
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
	
	/**
	 * 
	 * @return list of RC instances
	 */
	public static String[] getRCInstances() {
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
	
	/**
	 * 
	 * @return list of R, C and RC instances in that order
	 */
	public static String[] getAllInstances() {
		String[] instances = new String[29];

		System.arraycopy(getRInstances(), 0, instances, 0, 12);
		System.arraycopy(getCInstances(), 0, instances, 12, 9);
		System.arraycopy(getRCInstances(), 0, instances, 21, 8);

		return instances;
	}
	
	/**
	 * 
	 * @param instanceType
	 * @return
	 */
	public static String[] getInstace(String instanceType) {
		String[] instances = new String[1];
		instances[0] = instanceType + ".txt";
		return instances;
	}
	
	/**
	 * 
	 * @return
	 */
	public EspprcInstance getInstance() {
		return instance;
	}
	
	/**
	 * 
	 * @param instance
	 */
	public void setInstance(EspprcInstance instance) {
		this.instance = instance;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getFile() {
		return file;
	}

	/**
	 * 
	 * @param file
	 */
	public void setFile(String file) {
		this.file = file;
	}
}

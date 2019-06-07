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

package reader;

import model.Customer;
import model.EspprcInstance;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class SolomonReader {
	private EspprcInstance instance;
	private String file;

	public SolomonReader(EspprcInstance instance, String file){
		this.instance = instance;
		this.file = file;
	}

	public void read() {
		try {
			FileReader reader = new FileReader(this.file);
			BufferedReader br = new BufferedReader(reader);

			int counter = 0;
			String line;
			while ((line = br.readLine()) != null) {
				counter++;
				this.readInstace(line, counter);
			}

			br.close();
			reader.close();
		} catch (FileNotFoundException ex) {
			System.out.println("File " + file + " not found!");
		} catch (IOException ex) {
			System.out.println(ex);
		}
	}

	private void readInstace(String line, int counter) {

		line = line.replace("\r", "");
		line = line.trim();
		String[] tokens = line.split(" +");

		if (counter == 5) {
			this.readVehicle(tokens);
		}
		else if (counter == 10) {
			// origin node
			if(this.file.contains("_100")){
				this.instance.setNodes(new Customer[this.instance.isDuplicateOrigin() ? 102:101]);
			}
			else if(this.file.contains("_50")) {
				this.instance.setNodes(new Customer[this.instance.isDuplicateOrigin() ? 52:51]);
			}
			else if(this.file.contains("_25")) {
				this.instance.setNodes(new Customer[this.instance.isDuplicateOrigin() ? 27:26]);
			}
			else if(this.file.contains("_15")) {
				this.instance.setNodes(new Customer[this.instance.isDuplicateOrigin() ? 17:16]);
			}
			else if(this.file.contains("_10")) {
				this.instance.setNodes(new Customer[this.instance.isDuplicateOrigin() ? 12:11]);
			}
			else if(this.file.contains("_5")){
				this.instance.setNodes(new Customer[this.instance.isDuplicateOrigin() ? 7:6]);
			}
			
			this.readOrigin(tokens);
		}
		else if (counter > 10 && tokens.length == 7){
			// customers
			this.readCustomer(tokens);
		}
	}
	
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

	private void readVehicle(String[] tokens) {
		this.instance.setVehicles( Integer.parseInt(tokens[0]) );
		this.instance.setCapacity( Double.parseDouble(tokens[1]) );
	}

	public EspprcInstance getInstance() {
		return instance;
	}

	public void setInstance(EspprcInstance instance) {
		this.instance = instance;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}
}

package reader;

import model.Customer;
import model.VrpInstance;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class SolomonReader {
	private VrpInstance instance;
	private String file;
	
	public SolomonReader(VrpInstance instance){
		this.instance = instance;
	}

	public VrpInstance getInstance() {
		return instance;
	}

	public void setInstance(VrpInstance instance) {
		this.instance = instance;
	}
	
	// TODO: à tout faire
	public void read(String file) {
		
        try {
            FileReader reader = new FileReader(file);
            BufferedReader br = new BufferedReader(reader);
            
            this.readInstaceType(br);
            
            this.readVehicle(br);
            
            this.readCustomer(br);
            
            br.close();
            reader.close();
        } catch (FileNotFoundException ex) {
            System.out.println("File " + file + " not found!");
        } catch (IOException ex) {
            System.out.println(ex);
        }
	}

	private void readCustomer(BufferedReader br) {
		// TODO Auto-generated method stub
		
	}

	private void readVehicle(BufferedReader br) {
		// TODO Auto-generated method stub
		
	}

	private void readInstaceType(BufferedReader br) {
		// TODO Auto-generated method stub
		
	}
}

package model;

public class EspprcInstance extends AbstractInstance {
	
	// the cost to go from node_i to node_j
	private double[][] cost;
	
	// the quantity of vehicles
	private int vehicles;
	
	// the capacity of the uniform float of vehicles
	private double capacity;
	
	public EspprcInstance() {
		super();
	}
	
	public void buildCosts() {
		// We introduce a cost factor to model time costs
		double costFactor = 1;
        
		int nbNodes = this.getNodes().length;
        this.cost = new double[nbNodes][nbNodes];
        for(int i=0; i < nbNodes; i++) {
            for(int j=0; j < nbNodes; j++) {
                this.cost[i][j] = this.getNodes()[i].distance(this.getNodes()[j]) * costFactor;
            }
        }
	}
	
	public int getVehicles() {
		return vehicles;
	}

	public void setVehicles(int vehicles) {
		this.vehicles = vehicles;
	}

	public double[][] getCost() {
		return cost;
	}

	public void setCost(double[][] cost) {
		this.cost = cost;
	}

	public double getCapacity() {
		return capacity;
	}

	public void setCapacity(double capacity) {
		this.capacity = capacity;
	}
	
}

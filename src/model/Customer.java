
package model;

public class Customer extends AbstractNode implements Comparable<Customer>{
    
	/**
	 * The given node id
	 */
    private int id;
    
    /**
     * The required demand on this node
     */
    private int demand;
    
    /**
     * Time needed to produce the required product of the client
     */
    private double productionTime;
    
    /**
     * The start time of the time window
     */
    private double start;
    
    /**
     * The end time of the time window
     */
    private double end;
    
    /**
     * The service time required on this node
     */
    private double serviceTime;
    
	/**
	 * Flag to check if it is the last node
	 */
	private boolean isDepot = false;
	
	/**
	 * 
	 * @param x
	 * @param y
	 */
	public Customer(double x, double y) {
		super(x, y);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getDemand() {
		return demand;
	}

	public void setDemand(int demand) {
		this.demand = demand;
	}

	public double getStart() {
		return start;
	}

	public void setStart(double start) {
		this.start = start;
	}

	public double getEnd() {
		return end;
	}

	public void setEnd(double end) {
		this.end = end;
	}

	public double getServiceTime() {
		return serviceTime;
	}

	public void setServiceTime(double serviceTime) {
		this.serviceTime = serviceTime;
	}

	public double getProductionTime() {
		return productionTime;
	}

	public void setProductionTime(double productionTime) {
		this.productionTime = productionTime;
	}

	public boolean isDepot() {
		return isDepot;
	}

	public void setDepot(boolean isDepot) {
		this.isDepot = isDepot;
	}


	@Override
	public int compareTo(Customer that) {
		if ( isDepot ) { return 1; }
		if ( that.isDepot() ) { return -1; }
		
		double comparison = this.start - that.getStart();
		return (int) Math.signum(comparison);
	}
}

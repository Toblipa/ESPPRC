
package model;

public class Customer extends AbstractNode{
    
    private int customerId;
    
    private int demand;
    
    private double start;
    
    private double end;
    
    private double serviceTime;

	public Customer(double x, double y) {
		super(x, y);
	}

	public int getCustomerId() {
		return customerId;
	}

	public void setCustomerId(int customerId) {
		this.customerId = customerId;
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

	
    /**
     * Calcul de la distance euclidienne entre deux points (this et p).
     * @param p le point avec lequel on cherche la distance
     * @return la distance euclidienne entre les points, infini si p est null.
     */
	@Override
    public double distance (AbstractNode p) {
        if(p==null) {
            return Double.MAX_VALUE;
        }
        double dx = this.getX() - p.getX();
        double dy = this.getY() - p.getY();
        return Math.sqrt(dx*dx + dy*dy);
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

/**
 * Represente un point en 2D defini par son abscisse et son ordonnee.
 * @author Maxime Ogier
 */
public class Customer {
    /**
     * Abscisse du point
     */
    private double x;
    /**
     * Ordonnee du point
     */
    private double y;
    
    private int customerId;
    
    private int demand;
    
    private double start;
    
    private double end;
    
    private double serviceTime;
    
    /**
     * Constructeur par defaut : abscisse et ordonnee sont nuls
     */
    public Customer() {
        this.x = 0;
        this.y = 0;
    }
    
    /**
     * Constructeur par copie
     * @param x l'abscisse du point
     * @param y l'ordonnee du point
     */
    public Customer (double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Calcul de la distance euclidienne entre deux points (this et p).
     * @param p le point avec lequel on cherche la distance
     * @return la distance euclidienne entre les points, infini si p est null.
     */
    public double distance (Customer p) {
        if(p==null) {
            return Double.MAX_VALUE;
        }
        double dx = this.x - p.getX();
        double dy = this.y - p.getY();
        return Math.sqrt(dx*dx + dy*dy);
    }

    /**
     * @return l'abscisse du point
     */
    public double getX() {
        return x;
    }

    /**
     * @return l'ordonnée du point
     */
    public double getY() {
        return y;
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
	
    @Override
    public String toString() {
        return "Point{" + "x=" + x + ", y=" + y + '}';
    }
    
	/**
     * Tests unitaires
     * @param args 
     */
    public static void main(String[] args) {
        Customer p1 = new Customer();
        Customer p2 = new Customer(2.5, -3.2);
        System.out.println(p1);
        System.out.println(p2);
        System.out.println("distance = "+p1.distance(p2));
    }
}

package model;

/**
 * Represente un point en 2D defini par son abscisse et son ordonnee.
 * @author Maxime Ogier
 */
public abstract class AbstractNode {
    /**
     * Abscisse du point
     */
    private double x;
    /**
     * Ordonnee du point
     */
    private double y;
    
    /**
     * Constructeur par defaut : abscisse et ordonnee sont nuls
     */
    public AbstractNode() {
        this.x = 0;
        this.y = 0;
    }
    
    /**
     * Constructeur par copie
     * @param x l'abscisse du point
     * @param y l'ordonnee du point
     */
    public AbstractNode(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
	/**
     * Calcul de la distance euclidienne entre deux points (this et p).
     * @param p le point avec lequel on cherche la distance
     * @return la distance euclidienne entre les points, infini si p est null.
     */
    public double distance (AbstractNode p) {
        if(p==null) {
            return Double.MAX_VALUE;
        }
        double dx = this.getX() - p.getX();
        double dy = this.getY() - p.getY();
        return Math.sqrt(dx*dx + dy*dy);
    }
    
//    public abstract double distance(AbstractNode p);
    
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
    
    @Override
    public String toString() {
        return "Point{" + "x=" + x + ", y=" + y + '}';
    }
}

package model;

public class VrpInstance {
    /**
     * Les points du réseau.
     * Ils sont stockes dans un tableau (mais on pourrait aussi utiliser une structure de type ArrayList.
     */
    private Customer[] customers;
    /**
     * Les distances entre chaque paire de points.
     * Elles sont stockees en dur pour eviter de refaire constemment le calcul.
     */
    private double[][] distance;
}

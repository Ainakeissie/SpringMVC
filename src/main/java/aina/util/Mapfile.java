package aina.util;

public class Mapfile {
    private String nom;
    private String extension;
    private byte[] tableau_byte;
    
    public String getNom() {
        return nom;
    }
    public void setNom(String nom) {
        this.nom = nom;
    }
    public String getExtension() {
        return extension;
    }
    public void setExtension(String extension) {
        this.extension = extension;
    }
    public byte[] getTableau_byte() {
        return tableau_byte;
    }
    public void setTableau_byte(byte[] tableau_byte) {
        this.tableau_byte = tableau_byte;
    }
}

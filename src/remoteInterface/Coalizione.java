package remoteInterface;

import java.io.Serializable;
import java.util.ArrayList;

/**
 *
 * @author Trappola
 */
public class Coalizione implements Serializable {
    private String nome;
    private ArrayList<Integer> idListe;
    
    public Coalizione(String nome, ArrayList<Integer> idListe){
        this.nome = nome;
        this.idListe = idListe;
    }
    public String getNome() {        return nome;    }
    public ArrayList<Integer> getIdListe() {        return idListe;    }    
}

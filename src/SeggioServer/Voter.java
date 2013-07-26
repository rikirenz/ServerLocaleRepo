package SeggioServer;

import java.util.GregorianCalendar;

/**
 *rappresenta l'elettore
 * @author lele
 */
public class Voter {
    private Integer id;
    private String nome;
    private String cognome;
    private GregorianCalendar dataNascita;
    private Boolean authorizedToVote;       //indica se l'elettore è mai stato abilitato alla votazione
    private Boolean senatoAllowed;          //indica se l'elettore è abilitato alla votazione per il senato
    
    public Voter( Integer id, String nome, String cognome, GregorianCalendar dataNascita, Boolean senatoAllowed){
        this.nome = nome;
        this.cognome = cognome;
        this.dataNascita = dataNascita;
        this.id = id;
        this.authorizedToVote = false;
        this.senatoAllowed = senatoAllowed;
    }

    /**
     * @return the id
     */
    public Integer getId() {
        return id;
    }

    /**
     * @return the nome
     */
    public String getNome() {
        return nome;
    }

    /**
     * @return the cognome
     */
    public String getCognome() {
        return cognome;
    }

    /**
     * @return the dataNascita
     */
    public GregorianCalendar getDataNascita() {
        return dataNascita;
    }

    /**
     * @return the authorizedToVote
     */
    public Boolean authorizedToVote() {
        return authorizedToVote;
    }

    /**
     * @param authorizedToVote authorizedToVote to set
     */
    public void setAuthorizedToVote(Boolean authorized) {
        this.authorizedToVote = authorized;
    }

    /**
     * @return senatoAllowed
     */
    public Boolean isSenatoAllowed() {
        return senatoAllowed;
    }
}

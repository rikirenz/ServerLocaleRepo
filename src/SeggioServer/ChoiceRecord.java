/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package SeggioServer;

import java.io.Serializable;

/**
 *
 * @author lele
 */
public class ChoiceRecord implements Serializable{
    private Integer idVoter;
    private Integer[] choices;
    private Boolean finishedVoting;
    
    public ChoiceRecord(Integer idVoter, Integer[] choices){
        this.idVoter = idVoter;
        this.choices = choices;
        this.finishedVoting = false;
    }

    /**
     * @return the idVoter
     */
    public int getIdVoter() {
        return idVoter;
    }

    /**
     * @param idVoter the idVoter to set
     */
    public void setIdVoter(Integer idVoter) {
        this.idVoter = idVoter;
    }

    /**
     * @return the choices
     */
    public Integer[] getChoices() {
        return choices;
    }

    /**
     * @param choices the choices to set
     */
    public void setChoices(Integer[] choices){
        this.choices = choices;
    }

    /**
     * @return the finishedVoting
     */
    public boolean finishedVoting(){
        return finishedVoting;
    }

    /**
     * @param finishedVoting the finishedVoting to set
     */
    public void setFinishedVoting(Boolean finishedVoting){
        this.finishedVoting = finishedVoting;
    }
}

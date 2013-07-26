/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package remoteInterface;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author lele
 */
public interface ICabinaRemote extends Remote {
    
    boolean isFree() throws RemoteException;
    /** @param listaElettorale: contiene tutte le coalizioni */
    void setListeElettorale(String[] lstElettorale)throws RemoteException;
    /** @param cognome: cognome elettore @param nome: nome elettore*/
    void setVotazioneElettore(String cognome, String nome) throws RemoteException;
    /** @param lstCoalizioni : array di coalizioni che si presentano alla camera */
    void setVotazioneCamera(Coalizione[] lstCoalizioni)throws RemoteException;
    /** @param lstCoalizioni : array di coalizioni che si presentano al senato della rep. */
    void setVotazioneSenato(Coalizione[] lstCoalizioni)throws RemoteException;
    /** @param lstReferendum : array di quesiti referendari proposti all'elettore */
    void setVotazioneReferendum(String[] lstReferendum)throws RemoteException;
    /** @param accepted : valore boolean di conferma avvenuto riconoscimento da parte di seggio*/
    void notificaRegistrazione(boolean accepted) throws RemoteException;
    /** avvia la votazione */
    Integer[] startVotazione() throws RemoteException;
    
}

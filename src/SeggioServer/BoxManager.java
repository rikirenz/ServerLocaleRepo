/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 * CIAO LELE HO AGGIUNTO UNA ROBA
 */
package SeggioServer;

import java.io.IOException;
import java.rmi.ConnectException;
import remoteInterface.ICabinaRemote;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lele
 */
public class BoxManager implements Runnable {
    private ICabinaRemote myBox;        //cabina per la votazione
    private Voter el;                   //elettore
    private Integer[] voterChoices;     //scelte del votante
    private String[] refToSend;         //quesiti referendari da inviare alla cabina 
    private Integer[] result;           //preferenze espresse 
    private ServerLocale localServer;
    
    BoxManager(ICabinaRemote box,Voter el, Integer[] vc, ServerLocale sl) throws RemoteException{
        System.out.println("> creato gestore per la votazione");
        this.myBox = box;
        
        this.el = el;
        this.voterChoices = vc;
        this.refToSend = new String[vc.length-2];
        this.localServer = sl;
    }
    
    /**
     * voterChoices[i] = {-1,0,1} 
     * ---
     * -1 = non può votare
     * 0 = ha scelto di non votare
     * 1 = ha scelto di votare
     * ---
     * voterChoices[0] -> votazione camera
     * voterChoices[1] -> votazione senato
     * voterChoices[2,3....n] -> votazione referendum
     */
    @Override
    public void run(){
        try {
            
            myBox.setVotazioneElettore(el.getCognome(), el.getNome());
            if (voterChoices[0]==1){
                myBox.setVotazioneCamera(localServer.getCoaCamera());
            }
            
            if (voterChoices[1]==1){
                myBox.setVotazioneSenato(localServer.getCoaSenato());
            } 
            
            if (atLeastAref()){
                for (int i=2; i<voterChoices.length; i++){
                    if ( voterChoices[i] == 1){
                       refToSend[i-2] = localServer.getReferendum()[i-2];
                    }
                }
                myBox.setVotazioneReferendum(refToSend);
            }
           
            result = myBox.startVotazione();        //parte la votazione in cabina, preferenze restituite come return
            System.out.println("> votante ha terminato: risultati: ");
            stampaResult(result);
            localServer.updateResults(result);      //
            localServer.serializeCamResults();
            localServer.serializeSenResults();
            localServer.serializeRefResults();
            localServer.deserializeResults();
            System.out.println("> una cabina si è liberata");
            localServer.getBoxesManager().decreaseTaken();
            localServer.getVoteManager().setFinishedVoting(el.getId());
            System.out.println("-----------RISULTATI TOTALI-----------");
            localServer.printCameraResults();
            localServer.printSenatoResults();
            localServer.printRefResults();
            System.out.println("--------------------------------------");
            
        } catch (UnmarshalException ex) {
            System.err.println("> crash di una cabina! l' elettore "+el.getNome() +" "+ el.getCognome()+ " [id = "+el.getId()+" ] non ha terminato la votazione correttamente");
            
        } catch (ConnectException ex){
            System.err.println("> cabina non contattabile!");
            
        } catch (RemoteException ex) {
            Logger.getLogger(BoxManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(BoxManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(BoxManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    /**
     * controlla che ci sia almeno un referendum per cui votare
     * @return 
     */
    private boolean atLeastAref() {
        return refToSend.length > 0;
    }

    private void stampaResult(Integer[] result) {
        for (int i=0; i<result.length; i++){
            System.out.println(result[i]);
        }
    }
}

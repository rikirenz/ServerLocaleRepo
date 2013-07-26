/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package remoteInterface;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 *
 * @author Trappola
 */
public interface InternoRemoteInterface extends Remote{    
    
    public boolean registraSeggio(String inSeggio, int numEleCam, int numEleSen) throws RemoteException;
    public String[] getListEle() throws RemoteException;
    public Coalizione[] getCoaCam(String idSeggio) throws RemoteException;
    public Coalizione[] getCoaSen(String idSeggio) throws RemoteException;
    public String[] getReferendum() throws RemoteException;
    
    public void setScelteEspresse(String idSeggio,Integer[] scelte) throws RemoteException; 
    public void getPreferenzeEspresse(String idSeggio, int[] preferenzeEspresseCamera, int[] preferenzeEspresseSenato, ArrayList<Integer[]> preferenzeEspresseReferendum) throws RemoteException;
    public void terminato(String idSeggio) throws RemoteException; 
}

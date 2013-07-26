package SeggioServer;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import remoteInterface.ICabinaRemote;
import java.util.logging.Level;
import java.util.logging.Logger;
import remoteInterface.IRegistraCabinaRemote;

/**
 *Gestisce le cabine libere (riferimenti ad oggetti remoti), l'implementazione è molto simile al problema del bounded buffer
 * -viene rimossa (occupata) una cabina quando un elettore deve effettuare la votazione
 * -viene reinserita (liberata) una cabina quando un elettore ha terminato la votazione
 * @author lele
 */
public class BoxesManager extends UnicastRemoteObject implements IRegistraCabinaRemote {
    private final int BOXES_NUM = 4;                     //numero massimo di cabine
    private int in, taken;             //variabili per la gestione 
    private ArrayList<ICabinaRemote> boxes;      //riferimenti alle cabine/
    private ServerLocale localServer;
    private boolean allTaken;
    
    public BoxesManager (ServerLocale sl)throws RemoteException{
        super();
        boxes = new ArrayList<>(BOXES_NUM);
        localServer = sl;
    }
    
    /**
     * registra una nuova cabina (se possibile)
     * @param box
     * @return 
     */
    public synchronized boolean registerNewBox(ICabinaRemote box) {
        if (!allRegistered()){
            boxes.add(box);
            return true;
        } 
        return false;
    }
    
    /**
     * elimina il riferimento ad una cabina dalla lista delle cabine
     * @param box
     * @return 
     */
    public synchronized boolean unregisterBox(ICabinaRemote box){
        int index = boxes.indexOf(box);     // indice dell'oggetto da rimuovere dalla lista (se -1, oggetto non presente)
        if (index == -1) return false;      // se oggetto non presente ritorna false
        boxes.remove(index);                // rimuove l'oggetto
        return true;
    }
    
    public synchronized ICabinaRemote getFreeBox() throws RemoteException{
        checkBoxesConsistence();
        while (allTaken || noBoxAvailable()){
            System.out.println("> tutte le cabine sono momentaneamente occupate o non disponibili: attendere che se ne liberi/registri una");
            try {
                wait();
            } catch (InterruptedException ex) {
                Logger.getLogger(BoxesManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        int i;
        for (i=0; i < boxes.size(); i++){
            if (boxes.get(i).isFree()){
                increaseTaken();
                break;
            }
        }
        
        return boxes.get(i);
    }
    
//    public synchronized ICabinaRemote getFreeBox() throws RemoteException{
//        //boolean noBox = checkBoxesPresence();
//        while (allTaken){
//            System.out.println("> tutte le cabine sono momentaneamente occupate: attendere che se ne liberi una");
//            try {
//                wait();
//            } catch (InterruptedException ex) {
//                Logger.getLogger(BoxesManager.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//        
//        int j;
//        for (j=0; j<; j++){
//            if (boxes[j] != null && boxes[j].isFree()){
//                break;
//            }
//        }
//        
//        increaseTaken();
//        return boxes[j];
//    }
    
//    /**
//     * inserisce una nuova cabina o ne reinserisce una che si è appena liberata
//     * @param freeCab 
//     */
//    public synchronized void insert(ICabinaRemote freeCab){
//        while (count == boxNum){
//            try {
//                wait();
//            } catch (InterruptedException ex) {
//                Logger.getLogger(BoxesManager.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//        freeBoxes[in] = freeCab;
//        in = (in+1) % boxNum;
//        count++;
//        
//        notify();
//    }
    /**
     * estrae una cabina libera per renderla disponibile ad un elettore
     * @return 
     */
//    public synchronized ICabinaRemote getFreeBox(){
//        ICabinaRemote freeCab;
//        
//        while (count == 0){
//            try {
//                System.out.println("> tutte le cabine sono momentaneamente occupate: attendere che se ne liberi una");
//                wait();
//            } catch (InterruptedException ex) {
//                Logger.getLogger(BoxesManager.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//        freeCab = boxes[out];
//        out = (out+1) % boxesNum;
//        count--;
//        
//        notify();
//        
//        return freeCab;
//    }

    /**
     * @return registeredBoxes
     */
    public synchronized int getRegisteredBoxes() {
        return boxes.size();
    }
    
    public void printBoxes(){
        for (ICabinaRemote cab: boxes){
            System.out.println(cab);
        }
    }
    
    public synchronized boolean allRegistered(){
        return boxes.size() == BOXES_NUM;
    }

    @Override
    public synchronized void setCabina(ICabinaRemote box) throws RemoteException {
        boolean registered = registerNewBox(box);
        if (registered){
            System.out.println("> cabina aggiunta con successo: numero cabine = "+boxes.size());
            box.notificaRegistrazione(true);
            box.setListeElettorale(localServer.getListeElettorali());
            if (allRegistered()){
                System.out.println("allRegistered!");
                localServer.getVoteManager().notifica();
            }
        } else{
            System.err.println("> non è possibile aggiungere un'altra cabina");
            box.notificaRegistrazione(false);
        }
    }

    /**
     * incrementa contatore cabine occupate
     */
    private synchronized void increaseTaken(){
        taken++;
        if (taken == BOXES_NUM){
            allTaken = true;
        }
    }
    
    /**
     * decrementa contatore cabine occupate
     */
    public synchronized void decreaseTaken(){
        System.out.println("decreaseTaken");
        allTaken = false;
        taken--;
        notify();
    }

    // synchronized ?
    private void checkBoxesConsistence() throws RemoteException {
        int i=0;
        while (i < boxes.size()){
            try{
                System.out.println(i);
                boolean boo = boxes.get(i).isFree();
                i++;
            } catch (ConnectException ex){
                System.err.println(">>> box crash: "+i);
                boxes.remove(i);
                i = 0;
            }
        }
    }

    private boolean noBoxAvailable() {
        return boxes.size()==0;
    }
 }

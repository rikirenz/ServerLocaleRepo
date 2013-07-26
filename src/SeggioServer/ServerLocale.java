/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package SeggioServer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.AlreadyBoundException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import remoteInterface.Coalizione;
import remoteInterface.InternoRemoteInterface;

/**
 *
 * @author lele
 */
public class ServerLocale {
    private final int PORT = 1099;
    private Registry internoRegistry;
    private Registry registry;
    private VoteManager voteMngr;
    private BoxesManager boxesMngr;
    private final int BOX_NUMBER = 4;
    private Voter[] voters;
    private int maxId;
    private int minId;
    private String HOST_INTERNO;
    private int PORT_INTERNO;
    private String IP = "192.168.0.1";
    private String ID;
    private String[] listeElettorali;
    private Coalizione[] coaCamera;
    private Coalizione[] coaSenato;
    private String[] referendum;
    private Integer[] cameraResults;
    private Integer[] senatoResults;
    private ArrayList<Integer[]> referendumResults;
    private final int VENTICINQUE_IN_DAYS = 25*365;
    private int senatoCount = 0;
    private InternoRemoteInterface stub;
    /**
     *
     * @throws NotBoundException
     */
    public void start() throws NotBoundException {
        try {
            configureSeggio();
            loadElettori(); 
            setIdRange();
            //stampaElettori();
            
            //connessione con Interno per ottenere rif. all'oggetto remoto e registrarsi come nuovo seggio
            internoRegistry = LocateRegistry.getRegistry(HOST_INTERNO, PORT_INTERNO);
            stub = (InternoRemoteInterface) internoRegistry.lookup("Interno");
            boolean confirmed = stub.registraSeggio(ID, voters.length, senatoCount);
            
            // se riconosciuto, il seggio si fa inviare i dati per la votazione (liste, coalizioni e referendum)
            if (confirmed){
                listeElettorali = stub.getListEle();
                coaCamera = stub.getCoaCam(ID);
                coaSenato = stub.getCoaSen(ID);
                referendum = stub.getReferendum();
            } else{
                System.out.println(">> seggio non riconosciuto, esecuzione termina");
                System.exit(1);
            }
            
            printElectionDayData();
            initializeResultsStructures();
            printRefResults();
            // boxesManager si occupa della gestione delle cabine libere
            boxesMngr = new BoxesManager(this);
            
            //voteManager gestisce la votazione (arrivo degli elettori, identificazione, riconoscimento, abilitazione al voto, etc...)
            voteMngr = new VoteManager(maxId, minId,this);
            
            //viene messo a disposizione un oggetto remoto per permettere alle cabine di registrarsi al seggio
//            seggio = new RegistraCabinaImpl(this);
            startRegistry();
            registerObject("seggio", boxesMngr); 
            
            //parte la votazione
            Thread votationThread = new Thread(voteMngr);
            votationThread.start();
        } catch (AlreadyBoundException ex1) {
            System.err.println("Server exception: " + ex1.toString());
        } catch (ConnectException ex3) {
            System.err.print("Errore: non è stato possibile contattare il server centrale, assicurarsi che "
                    + "il server centrale sia avviato\n");
            System.exit(1);
        } catch (RemoteException ex) {
            Logger.getLogger(ServerLocale.class.getName()).log(Level.SEVERE, null, ex);
        }
            /*catch (NotBoundException e) {
            System.err.println("Client exception: " + e.toString()); 
        }*/
    }
    
    /**
     * carica la lista di elettori da file: crea un nuovo oggetto Elettore per ogni elettore e lo aggiunge all'array
     * @return array di oggetti Record [elettore, stato elettore]
     */
    private void loadElettori() {
        try {
            String elettoreLine;
            FileReader reader = new FileReader("Elettori.txt");
            Scanner scanFile = new Scanner(reader);
            int votersNum = scanFile.nextInt();
            voters = new Voter[votersNum];
            Voter el;
            scanFile.nextLine();
            //"estrae" l'elettore, effettua il parsing e aggiunge all'array  
            for (int i=0; i<votersNum; i++){
                elettoreLine = scanFile.nextLine();
                el = parsingElettore(elettoreLine);
                voters[i] = el;
                if (el.isSenatoAllowed()){
                    senatoCount++;
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ServerLocale.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void startRegistry() throws RemoteException {
        // create in server registry
        registry = java.rmi.registry.LocateRegistry.createRegistry(PORT);
    }
    
    private void registerObject(String name, Remote remoteObj) throws RemoteException, AlreadyBoundException {
        // Collego l'oggetto remoto sul rmiregistry
        registry.bind(name, remoteObj);
        System.out.println("Registered: " + name + " -> " + remoteObj.getClass().getName() + "[" + remoteObj + "]");
    }
    
    
    /**
     * estrae i singoli token dalla stringa per "creare" l'oggetto Record (elettore-stato)
     * @param elettoreLine stringa elettore
     * @return 
     */
    private Voter parsingElettore(String elettoreLine) {
        Scanner forNextToken = new Scanner(elettoreLine);
        int id = 0;
        String nome = null; String cognome = null;
        int day = 0; int month = 0; int year = 0;
        int eta;
        boolean senatoAllowed;
        GregorianCalendar dataNascita;
        
        while (forNextToken.hasNext()){
            id = forNextToken.nextInt();
            nome = forNextToken.next();
            cognome = forNextToken.next();
            day = forNextToken.nextInt();
            month = forNextToken.nextInt();
            year = forNextToken.nextInt();
        }
        dataNascita = new GregorianCalendar(year, month, day);
        eta = calcolaEta(dataNascita);
        senatoAllowed = (eta >= VENTICINQUE_IN_DAYS) ? true : false;
        
        return new Voter(id, nome, cognome, dataNascita, senatoAllowed);
    }

    private void stampaElettori() {
        for(Voter e: getVoters()){
            System.out.println(e.getNome()+" "+e.getNome()+" "+e.getCognome()+" "+e.getDataNascita()+" "+e.authorizedToVote());
        }
    }

    /**
     * @return listaElettori
     */
    public Voter[] getVoters() {
        return voters;
    }

    /**
     * @return boxesManager
     */
    public BoxesManager getBoxesManager() {
        return boxesMngr;
    }

    /**
     * configura il seggio leggendo i parametri da file
     */
    private void configureSeggio() {
        try {
            Scanner scanFile = new Scanner(new FileReader("seggioConfig.txt"));
            HOST_INTERNO = scanFile.next();
            PORT_INTERNO = scanFile.nextInt();
            ID = scanFile.next();
            System.out.println("> ID seggio: "+ID);
        } catch (FileNotFoundException ex){
            Logger.getLogger(ServerLocale.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @return referendum
     */
    public String[] getReferendum() {
        return referendum;
    }

    /**
     * @return liste Elettorali
     */
    public String[] getListeElettorali() {
        return listeElettorali;
    }

    /**
     * @return coalizioni Camera
     */
    public Coalizione[] getCoaCamera() {
        return coaCamera;
    }

    /**
     * @return the coalizioni Senato
     */
    public Coalizione[] getCoaSenato() {
        return coaSenato;
    }

    /**
     * in seguito ad una votazione vengono aggiornati i risultati per la Camera
     * @param id corrispondente alla lista votata
     */
    public void addCameraResult(int id) {
        synchronized(cameraResults){
            cameraResults[id]++;
        }
    }

    /**
     * in seguito ad una votazione vengono aggiornati i risultati per il Senato
     * @param id corrispondente alla lista votata
     */
    public void addSenatoResult(int id) {
        synchronized(senatoResults){
            senatoResults[id]++;
        }    
    }

    /**
     *  in seguito ad una votazione vengono aggiornati i risultati per i Referendum
     * @param id id del referendum per il quale è stato effettuato il voto
     * @param result risultato della votazione (0=no, 1=si)
     */
    public void addReferendumResult(int id, int result) {
        synchronized(referendumResults){
            referendumResults.get(id)[result]++;
        }
    }

    /**
     * @return voteManager
     */
    public VoteManager getVoteManager() {
        return voteMngr;
    }

    /**
     * @return the BOX_NUMBER
     */
    public int getBoxNumber() {
        return BOX_NUMBER;
    }
    
    public synchronized void printCameraResults(){
        System.out.println(">> risultati camera:");
        for (int i=0; i<cameraResults.length; i++){
            System.out.println(i+") "+listeElettorali[i]+" --> "+cameraResults[i]);
        }
        System.out.println();
    } 

    public synchronized void printSenatoResults(){
        System.out.println(">> risultati senato:");
        for (int i=0; i<senatoResults.length; i++){
            System.out.println(i+") "+listeElettorali[i]+" --> "+senatoResults[i]);
        }
        System.out.println();
    }
    
    public synchronized void printRefResults(){
        System.out.println("> risultati referendum:");
        for (int i=0; i<referendum.length; i++){
            System.out.println("ref n° "+i+" [ "+referendum[i]+" ] SI: "+ referendumResults.get(i)[1]+" NO: "+referendumResults.get(i)[0]);
        }
        System.out.println();
    }

    /**
     * in base al scelte espresse e ai risultati di una votazione vengono aggiornate le relative strutture dati dei risultati
     * @param results 
     */
    public synchronized void updateResults(Integer[] results) {
        if (results[0]!=-1){
            addCameraResult(results[0]);
        }
        if (results[1]!=-1){
            addSenatoResult(results[1]);
        }
        for (int i=2; i<results.length; i++){
            if (results[i] >=0){
                addReferendumResult(i-2,results[i]);
            }
        }
    }
    
    /**
     * stampa dati per la votazione
     */
    private void printElectionDayData() {
        System.out.println("\nLISTE:");
        for (int i=0; i<getListeElettorali().length; i++){
            System.out.println(getListeElettorali()[i]);
        }
        System.out.println("\nCOALIZIONI CAMERA:");
        for (int i=0; i<coaCamera.length; i++){
            System.out.println(coaCamera[i].getNome());
        }
        System.out.println("\nCOALIZIONI SENATO:");
        for (int i=0; i<coaSenato.length; i++){
            System.out.println(coaSenato[i].getNome());
        }
        System.out.println("\nREFERENDUM:");
        for (int i=0; i<referendum.length; i++){
            System.out.println(referendum[i]);
        }
    }

    /**
     * imposta il range degli id accettabili (se un elettore ha il suo id fuori dal range, non è assegnato al determinato seggio
     */
    private void setIdRange() {
        minId = voters[0].getId();
        maxId = minId + voters.length -1;
        System.out.println("> minId: "+minId);
        System.out.println("> maxId: "+maxId);
    }

    /**
     * inizializza le strutture dati usate per memorizzare i risultati
     */
    private void initializeResultsStructures() {
        cameraResults = new Integer[listeElettorali.length];
        senatoResults = new Integer[listeElettorali.length];
        referendumResults = new ArrayList<Integer[]>();
        for (int i=0; i<listeElettorali.length; i++){
            cameraResults[i] = senatoResults[i] = 0;
        }
        for (int i=0; i<referendum.length; i++){
            Integer[] tmp= {0,0};
            referendumResults.add(i, tmp);
        }
    }
    
    /**
     * calcola l'età dell'elettore in giorni
     * @param el
     * @return 
     */
    private int calcolaEta(GregorianCalendar birthDate) {
        GregorianCalendar now = new GregorianCalendar();
        long etaInMillis = now.getTimeInMillis() - birthDate.getTimeInMillis();
        int etaInDays = (int) (etaInMillis/86400000);
        return etaInDays;
    }
    
    /**
     * invia le scelte di un elettore al server centrale
     * @param choices scelte espresse
     * @throws RemoteException 
     */
    public void sendChoices(Integer[] choices) throws RemoteException{
        stub.setScelteEspresse(ID, choices);
        System.out.println("> scelte del votante inviate al server centrale");
    }
    
    public synchronized void serializeCamResults() throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("cameraResults.dat",false));
        out.writeObject(cameraResults);
        System.out.println("> effettuato backup dei risultati per la CAMERA");
    }
    
    public synchronized void serializeSenResults() throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("senatoResults.dat",false));
        out.writeObject(senatoResults);
        System.out.println("> effettuato backup dei risultati per il SENATO");
    }
    
    public synchronized void serializeRefResults() throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("referendumResults.dat",false));
        out.writeObject(referendumResults);
        System.out.println("> effettuato backup dei risultati per i REFERENDUM");
    }
    /**
     * DA CANCELLARE
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    public synchronized void deserializeResults() throws IOException, ClassNotFoundException{
        ObjectInputStream in = new ObjectInputStream(new FileInputStream("cameraResults.dat"));
        Integer[] tmpCam = (Integer[]) in.readObject();
        in = new ObjectInputStream(new FileInputStream("senatoResults.dat"));
        Integer[] tmpSen = (Integer[]) in.readObject();
        in = new ObjectInputStream(new FileInputStream("referendumResults.dat"));
        ArrayList<Integer[]> tmpRef = (ArrayList<Integer[]>) in.readObject();
        System.out.println("cam size "+tmpCam.length);
        System.out.println("sen size "+tmpSen.length);
        System.out.println("ref size "+tmpRef.size());
        System.out.println(">> risultati cameraSER:");
        for (int i=0; i<tmpCam.length; i++){
            System.out.println(i+") "+listeElettorali[i]+" --> "+tmpCam[i]);
        }
        System.out.println();
        System.out.println(">> risultati senatoSER:");
        for (int i=0; i<tmpSen.length; i++){
            System.out.println(i+") "+listeElettorali[i]+" --> "+tmpSen[i]);
        }
        System.out.println();
        System.out.println("> risultati referendumSER:");
        for (int i=0; i<tmpRef.size(); i++){
            System.out.println("ref n° "+i+" [ "+referendum[i]+" ] SI: "+ tmpRef.get(i)[1]+" NO: "+tmpRef.get(i)[0]);
        }
        System.out.println();
        
    }
}

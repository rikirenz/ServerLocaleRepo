package SeggioServer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import remoteInterface.ICabinaRemote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Si occupa della gestione delle operazioni necessarie per rendere possibile la votazione per un elettore.
 * Riceve gli elettori, li identifica, riconosce ed eventualmente li abilita per la votazione
 * @author lele
 */
public class VoteManager implements Runnable {
    private ArrayList<ChoiceRecord> votersChoices;  //raccoglie tutte le scelte effettuate dagli elettori per la votazione (non per cosa hanno votato)
    private Scanner forInput;
    private boolean presentInDB;
    private int maxId;
    private int minId;
    private ServerLocale localServer;

    VoteManager(int max, int min, ServerLocale sl) {
        forInput = new Scanner(System.in);
        maxId = max;
        minId = min;
        votersChoices = new ArrayList<>();
        localServer = sl;
    }
    
    @Override
    public void run() {
        checkRegisteredBox();
        
        System.out.println(">>> VOTAZIONE PARTITA <<<");
        Voter el;
        Integer idVoter;
        boolean identified;
        Integer[] voterChoices;
        
        
        // arrivano sempre dei votanti
        while (true){
            try {
                
                //viene identificato l'elettore e viene verificata la sua presenza nel DB degli elettori
                idVoter = voterIdentification(); 
                presentInDB = checkVoter(idVoter);
                
                // se elettore è presente nel db ed è confermata la sua identità
                if (presentInDB){
                    identified = confirmedIdentity(idVoter);
                    if (identified){
                        el = localServer.getVoters()[idVoter-minId];

                        // è stato già abilitato al voto in precedenza && non ha terminato la votazione 
                        if (authorizedToVote(idVoter) && !finishedVoting(idVoter)){
                            System.out.println("> elettore era andato in cabina ma non ha terminato la votazione correttamente");
                            voterChoices = lookForVoterChoices(idVoter);
                            if (voterChoices.length != 0){
                                enableVoter(el, voterChoices);
                            }
                        }

                        // è stato già abilitato al voto in precedenza && ha terminato la votazione 
                        if (authorizedToVote(idVoter) && finishedVoting(idVoter)){
                            System.out.println("> elettore ha già effettuato e terminato la votazione");
                            //non viene intrapresa nessuna azione, l'elettore ha già votato
                        }

                        // non è mai stato abilitato al voto, quindi è la prima volta che si presenta al seggio
                        if (!authorizedToVote(idVoter)){
                            System.out.println("> elettore non ha ancora votato");
                            voterChoices = getVoterChoices(el);
                            
                            votersChoices.add(new ChoiceRecord(el.getId(),voterChoices));
                            System.out.println("> aggiunto nuovo record di scelte");
                            serializeVotersChoices();
                            
                            printChoiceRecord(votersChoices.get(votersChoices.size()-1));
                            System.out.println("--dim AL serializzato-");
                            System.out.println(readVotersChoices().size());
                            System.out.println("----------------------");
                            
                            localServer.sendChoices(voterChoices);
                            
                            enableVoter(el, voterChoices);
                        }
                    }
                } else{
                    System.out.println("> elettore respinto");
                }   
            } catch (RemoteException ex) {
                Logger.getLogger(VoteManager.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(VoteManager.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(VoteManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * invocato per notificare che tutte le cabine si sono registrate, parte la votazione
     */
    public synchronized void notifica(){
        notify();
    }
    
    /**
     * DA SPOSTARE, FORSE
     */
    private synchronized void checkRegisteredBox(){
        if (!localServer.getBoxesManager().allRegistered()){
            try {
                System.out.println("> ci sono meno di "+ localServer.getBoxNumber() + " cabine registrate: attendere che si registrino tutte");
                wait();
            } catch (InterruptedException ex) {
                Logger.getLogger(VoteManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * richiede input per l'id dell'elettore
     * @return valore input
     */
    private int voterIdentification() {
        System.out.println("\n> inserire id elettore: ");
        return forInput.nextInt();
    }

    /**
     * verifica che l'elettore sia assegnato al seggio, cioè controlla il suo id
     * @param idVoter id del votante 
     * @return presenza del votante nel database
     */
    private boolean checkVoter(int idVoter) {
        if (idVoter > maxId || idVoter < minId){
            System.out.println("> elettore NON presente nel database");
            return false;
        } else{
            System.out.println("> elettore presente nel database");
            return true;
        }
    }

    /**
     * converte una stringa in una data
     * @param date
     * @return 
     */
    private GregorianCalendar parseBirthDate(String date) {
        Scanner forParsing = new Scanner(date).useDelimiter("/");
        int day = forParsing.nextInt();
        int month = forParsing.nextInt();
        int year = forParsing.nextInt();
        
        return new GregorianCalendar(year, month, day);
    }
    
    /**
     * viene richiesto di confermare l'identità del votante
     * @param idVoter identificativo dell'elettore
     * @return conferma dell'identità dell'elettore
     * @throws RemoteException 
     */
    private boolean confirmedIdentity(int idVoter) throws RemoteException {
        String response;
        int voterIndex = idVoter - minId;
        Voter el = localServer.getVoters()[voterIndex];
        GregorianCalendar dataNascita = el.getDataNascita();
        System.out.println("-nome elettore :"+el.getNome()+
                           "\n-cognome elettore:"+el.getCognome()+
                           "\n-data di nascita "+dataNascita+
                           "\n> conferma l'identità dell'elettore?(s/n): ");
        
        while(true){
            response = forInput.next();
            if (response.equals("s")){
                System.out.println("> identità dell'elettore confermata");
                return true;
            } else if (response.equals("n")){
                System.out.println("> identità dell'elettore NON confermata");
                return false;
            }
            System.out.println("> attenzione!input errato: ritentare");
        }
    }
    
    /**
     * viene richiesto all'elettore per cosa vuole votare
     * @param el elettore
     * @return lista di scelte effettuate (-1 = non può votare, 0 = non vuole votare, 1 = vuole votare) 
     */
    private Integer[] getVoterChoices(Voter el) {
        String[] quesitiReferendum = localServer.getReferendum();
        boolean isSenatoAllowed = el.isSenatoAllowed();
        Integer[] choices = new Integer[2+quesitiReferendum.length];
        System.out.println(isSenatoAllowed?"> elettore ha più di 25 anni, può votare per il senato":"> elettore ha meno di 25 anni, non può votare per il senato");
        System.out.println("> elettore desidera votare per tutto? (s/n)");
        if (forInput.next().equals("s")?true:false){
            for(int i=0; i<choices.length; i++){
                choices[i] = 1;
            }
            if (!isSenatoAllowed){
                choices[1] = -1;
            }
        } else{ 
            System.out.println("> elettore desidera votare per la Camera? (s/n)");
            choices[0] = forInput.next().equals("s")?1:0;

            if (isSenatoAllowed){
                System.out.println("> elettore desidera votare per il Senato? (s/n)");
                choices[1] = forInput.next().equals("s")?1:0;
            } else{
                choices[1] = -1;
            }
            
            for (int i=2; i<choices.length; i++){
                System.out.println("> elettore desidera votare per quesito n°"+(i-1)+": "+ localServer.getReferendum()[i-2]+"? (s/n)");
                choices[i] = forInput.next().equals("s")?1:0;
            }
        }
        
        return choices;
    }
    
    /**
     * controlla che sia stata effettuata almeno una scelta per la votazione
     * @param choices
     * @return 
     */
    private boolean noChoice(Integer[] choices){
        boolean noChoice = true;
        for (int c:choices){
            if (c==1){
                noChoice = false;
            }
        }
        return noChoice;
    }

    /**
     * controlla se l'elettore è stato già abilitato al voto in precedenza; controllo effettuato accedendo al db dei votanti
     * @param idVoter identificativo elettore
     * @return risposta bool
     */
    private boolean authorizedToVote(int idVoter) {
        Voter el = localServer.getVoters()[idVoter-minId];
        return el.authorizedToVote();
    }
    
    /**
     * controlla se l'elettore ha terminato con successo la votazione
     * @param idVoter id dell'elettore
     * @return boolean
     */
    private boolean finishedVoting(int idVoter) {
        ChoiceRecord tmpChoiceRecord;
        
        for (int i=0; i< votersChoices.size(); i++){
            tmpChoiceRecord = votersChoices.get(i);
            if (tmpChoiceRecord.getIdVoter() == idVoter && tmpChoiceRecord.finishedVoting()){
                return true;
            }
        }
        return false;
    }

    /**
     * abilita l'elettore ad effettuare una votazione, trovandogli una cabina libera ed inizializzandola adeguatamente
     * @param el elettore
     * @param voterChoices scelte per la votazione fatte dall'elettore
     * @throws RemoteException 
     */
    private void enableVoter(Voter el, Integer[] voterChoices) throws RemoteException, IOException, ClassNotFoundException{
        if (noChoice(voterChoices)){
            System.out.println("> elettore è stato identificato e riconosciuto, ma ha scelto di non votare per nulla");
            el.setAuthorizedToVote(true); 
            setFinishedVoting(el.getId());
        } else{
            System.out.println("> elettore abilitato alla votazione");
            /* DA SISTEMARE */ 
            System.out.println("TEST");
            ICabinaRemote box = localServer.getBoxesManager().getFreeBox();
            System.out.println("> elettore può andare in cabina a votare");
            el.setAuthorizedToVote(true); 
            //ora l'elettore è abilitato al voto e un boxManager si occupa della gestione della sua "sessione" di voto
            BoxManager boxMngr = new BoxManager(box, el, voterChoices, localServer);
            Thread boxMngrThread = new Thread(boxMngr);
            boxMngrThread.start();
        }
    }

    /**
     * invocato quando l'elettore ha terminato con successo la votazione, non potrà più tornare in cabina a votare
     * @param idVoter id dell'elettore
     */
    public void setFinishedVoting(int idVoter) {
        ChoiceRecord tmpRecord;
        synchronized (votersChoices){
           for (int i=0; i<votersChoices.size(); i++){
                tmpRecord = votersChoices.get(i);

                if (tmpRecord.getIdVoter() == idVoter){
                    tmpRecord.setFinishedVoting(true);
                }
            } 
        }
    }

    /**
     * cerca le scelte (per cosa votare) fatte da un determinato votante
     * @param idVoter
     * @return scelte effettuate in precedenza per la votazione
     */
    private Integer[] lookForVoterChoices(Integer idVoter) {
        int tmpId;
        
        for (int i=0; i<votersChoices.size(); i++){
            tmpId = votersChoices.get(i).getIdVoter();
            
            if (tmpId == idVoter){
                return votersChoices.get(i).getChoices();
            }
        }
        return null;
    }

    /**
     * stampa le scelte di un elettore
     * @param r 
     */
    private void printChoiceRecord(ChoiceRecord r) {
        System.out.println(r.getIdVoter());
        Integer[] choices= r.getChoices();
        for (int i=0; i<choices.length; i++){
            System.out.println(choices[i]);
        }
    }
    /**
     * serializza l'ArrayList di ChoiceRecord (scelte effettuate dagli elettori, per cosa hanno scelto di votare)
     * *non viene effettuato l'append
     * @throws IOException 
     */
    private void serializeVotersChoices() throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("votersChoicesBackup.dat",false));
        out.writeObject(votersChoices);
        System.out.println("> salvata copia del database delle scelte effettuate");
    }
    
    private ArrayList<ChoiceRecord> readVotersChoices() throws IOException, ClassNotFoundException{
        ObjectInputStream in = new ObjectInputStream(new FileInputStream("votersChoicesBackup.dat"));
        ArrayList<ChoiceRecord> tmp = (ArrayList<ChoiceRecord>) in.readObject();
        return tmp;
    }
}

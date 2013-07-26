/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package SeggioServer;

import java.rmi.NotBoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lele
 */
public class Main {

    public static void main(String[] args){
        try {
            ServerLocale serverSeggio = new ServerLocale();
            serverSeggio.start();
        } catch (NotBoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

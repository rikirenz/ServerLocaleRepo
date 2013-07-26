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
public interface IRegistraCabinaRemote extends Remote{
    public void setCabina(ICabinaRemote cab)throws RemoteException;
}

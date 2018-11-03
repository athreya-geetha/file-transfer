//@author Geetha R. Satyanarayana geetrish@iupui.edu
package server;

import java.util.HashMap;

/*
 * The FileTransferServer starts the Server Listener Thread.
 */
public class FileTransferServer{

	// This is a hashmap of all usernames and passwords.
	private static HashMap <String, String> registeredUsers ;
	
	public static void main(String[] args) {
		
		registeredUsers = new HashMap <String, String> ();
		
		// Start a new ServerListener thread.
		Runnable server = new ServerListener ();
		
		new Thread (server).start();
		
	}

	public static void setUsers_(HashMap <String, String> registeredUsers) {
		FileTransferServer.registeredUsers = registeredUsers;
	}

	public static HashMap <String, String> getUsers() {
		return registeredUsers;
	}

}

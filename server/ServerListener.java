package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * @ServerListener
 * 
 * The server listener class accepts requests from clients and passes it 
 * to the server worker that processes the request.
 */

public class ServerListener implements Runnable {

	// The port of this server
	protected int port = 19899;
	
	// The server socket object
	protected ServerSocket server = null;
	
	// Thread pool for handling multiple concurrent requests. 
	protected ExecutorService workerPool = Executors.newFixedThreadPool(10);

	public void run() {
		
		// Flags to stop the server.
		int i = 0;
		Boolean flag = true;
		
		// Open the server socket.
		openServerSocket ();
		System.out.println("Server started");
		while (flag)
		{
			try {
				
				// Accept a client request.
				Socket client = server.accept ();
				
				// Pass on the client request to a worker thread in the pool to handle the request.
				this.workerPool.execute(new ServerWorker (client));
				
			} catch (IOException e) {
				System.out.println("Error while accepting client request.");
			}
			
			// Stopping the server
			if (i == 5)
				flag = false;		
			i++;
		}
		
		// Close the server socket.
		closeServerSocket ();
	}

	/*
	 * Close the server socket.
	 */
	private void closeServerSocket() {
		
		try {
			// Close the server.
			server.close ();
		} catch (IOException e) {
			System.out.println ("Error while closing the server.");
		}
		
		// Shut down the worker pool
		workerPool.shutdown();
		
	}

	/*
	 * Open the server socket.
	 */
	private void openServerSocket() {
		
		try {
			 //  The server socket is created at the port. Maximum of 10 clients
			 //  can be waiting to connect to the server while the server is 
			 //  responding to other clients.
			server = new ServerSocket (port, 10);
		} 
		catch (IOException e) {
			System.out.println("Unable to start the server at port " + port);
		}
		
	}
}

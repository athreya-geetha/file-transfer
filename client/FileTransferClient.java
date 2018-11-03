//@author Geetha R. Satyanarayana geetrish@iupui.edu
package client;

import java.io.*;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/*
 * The FileTransferClient connects to the server with a user name and passowrd,
 * requests for a file and downloads it. 
 */
public class FileTransferClient implements Runnable{

	// Client socket.
	Socket client = null;

	// Server address
	String serverAddress;

	// Port address to be connected.
	int port = 19899;

	// The username to be connect to server
	String username;

	// The password to connect to server
	String password;

	// Mode for the client to register or login with client.
	char mode;

	// Filename for the download.
	String filename;

	// The output stream for send info to the server.
	DataOutputStream outStream = null;

	// The input stream for reading info from the server.
	DataInputStream inStream = null;

	// The secret key used for encryption or decryption
	String secretKey = "7392230989250788";

	// Constructor.
	FileTransferClient(String serverAddress, char mode, String username, String password, String filename)
	{
		this.serverAddress = serverAddress;
		this.mode = mode;
		this.username = username;
		this.password = password;
		this.filename = filename;
	}

	@Override
	public void run() {

		try {
			// Connect to the server.
			connecttoServer();

			System.out.println ("Connected to server.");
			
			// Mode for logging in or registering.
			outStream.writeChar(mode);
			outStream.flush();

			// Send username
			outStream.writeUTF(username);
			outStream.flush();

			// Send password
			outStream.writeUTF (password);
			outStream.flush();

			// Response for authentication.
			Boolean response = inStream.readBoolean();
			if(response)
			{
				System.out.println("Authentication successful. Requesting file...");

				// Send file name
				outStream.writeUTF(filename);
				outStream.flush();

				// Message for file found or not
				String message = inStream.readUTF();
				
				if(message.equals("Starting"))
					receiveFile();
				else if (message.equals("File Not Found"))
					System.out.println("File not found on server");
			}
			else
			{
				System.out.println("Invalid credentials");
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			closeClient();
		}
	}

	/*
	 * Receive the file.
	 */
	private void receiveFile() throws IOException {

		FileOutputStream fout = null;
		BufferedOutputStream bout = null;
		int numBytesRead = 0;
		int totalBytesRead = 0;
		
		Checksum check = new CRC32();
		long checkSum;

		try
		{
			System.out.println("Downloading file..");
			
			// Get the length of the file
			int length = inStream.readInt();

			fout = new FileOutputStream("downloaded-"+filename);
			bout = new BufferedOutputStream (fout);
			
			System.out.println("Length of file " + length + " Downloading... Please wait.. ");

			String message;
			do
			{
				// Get the contents of the file.
				byte[] byteFile = new byte[1024+16];
				numBytesRead = inStream.read(byteFile);
				
				// Decrypt the contents
				byte[] decryptedFile = decryptFile(byteFile);
				
				// Compute the checkSum
				check.update(decryptedFile, 0, decryptedFile.length);
				checkSum = check.getValue();
				
				//Send the checksum to server.
				outStream.writeLong(checkSum);
				outStream.flush();
				
				//Get the message from server if correctly received.
				message = inStream.readUTF();
						
				// If ok, correctly recieved, so write to file.
				if(message.equals("Ok"))
				{
					bout.write(decryptedFile, 0, decryptedFile.length);
					bout.flush();
					
					if(numBytesRead >= 0)
						totalBytesRead += numBytesRead;
				}
				// If not ok, Retry the download
				else if(message.equals("Not Ok"))
					System.out.println("Retrying download..");
				// IF max retries have crossed, abort.
				else if(message.equals("Aborting"))
				{
					System.out.println("Max retries exceeded. Aborting file download. File may be incorrectly downloaded");
					break;
				}

			}while (totalBytesRead < length + ((length / 1024) * 16));	

			// Get the final message.
			message = inStream.readUTF();

			if(message.equals("End"))
				System.out.println("File downloaded as downloaded-" + filename + " Bytes read: " + totalBytesRead);	

		}
		catch (IOException e) {
			System.out.println ("Caught exception: " + e.getMessage());
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		finally
		{
			// Close the file output stream and buffered output stream.
			if (bout != null)
				bout.close();

			if (fout != null)
				fout.close();
		}
	}

	/*
	 * Decrypt the contents of the file.
	 */
	private byte[] decryptFile(byte[] byteFile) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		byte[] key = secretKey.getBytes();

		SecretKeySpec secret = new SecretKeySpec(key, "AES");

		Cipher decryptCipher;
		decryptCipher = Cipher.getInstance("AES");
		decryptCipher.init(Cipher.DECRYPT_MODE, secret);

		byte[] decryptedFile = decryptCipher.doFinal(byteFile); 
		return decryptedFile;
	}

	/*
	 * Close the client.
	 */
	private void closeClient() {
		try {
			client.close ();
			outStream.close();
			inStream.close();
		} catch (IOException e) {
			System.out.println("Unable to close client.");
		}

	}
	
	/*
	 * Connect to the server
	 */
	private void connecttoServer() throws UnknownHostException {
		try {
			client = new Socket (serverAddress, port);

			outStream = new DataOutputStream(client.getOutputStream());

			inStream = new DataInputStream (client.getInputStream());

		} catch (UnknownHostException e) {
			System.out.println("Unable to connect to Server: " + serverAddress + " at port: " + port);
			throw e;
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}

	}

	/*
	 * I tried to encrypt/decrypt the password, but this was throwing many exceptions.4
	 * So I left it here.
	 */
	/*private void encryptPassword(String password) {

		byte[] key = secretKey.getBytes();

		SecretKeySpec secret = new SecretKeySpec(key, "AES");

		try {
			Cipher encryptCipher = Cipher.getInstance("AES");
			encryptCipher.init(Cipher.ENCRYPT_MODE, secret);

			byte[] encrptedPass = encryptCipher.doFinal(password.getBytes());
			//encrptedPass = BASE64EncoderStream.encode(encrptedPass);
			outStream.writeUTF(new String(encrptedPass));
			outStream.flush();

		} catch (NoSuchAlgorithmException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} catch (BadPaddingException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}*/

	/*
	 * Main method for the client to read the command line arguments and initialize the client.
	 */
	public static void main(String[] args) {

		// Parse the input arguments
		if(args[0].equals("-help"))
		{
			System.out.println("Specify program arguements in the following order");
			System.out.println("-s <serverAddress> -m <register/login> -us <username> -p <password> -f <filename>");
			return;
		}
		else if(args.length != 10)
		{
			System.out.println("Invalid number of arguments");
			return;
		}
		else if( !args[0].equals("-s") || !args[2].equals("-m") || !args[4].equals("-u") || !args[6].equals("-p") || !args[8].equals("-f"))
		{
			System.out.println("Invalid order of program arguemts. Please specify in this order.");
			System.out.println("-s <serverAddress> -m <register/login> -us <username> -p <password> -f <filename>");
			return;
		}
		else if (!args[3].equals("register") && !args[3].equals("login"))
		{
			System.out.println("Invalid mode. Please give register or login.");
		}
		else
		{
			// Create a client thread with the arguements specified and start the thread.
			/*
			 * args[1] contains server address
			 * args[3] contains mode. Take r for register a new client and l for login.
			 * args[5] contains username
			 * args[7] contains password
			 * args[9] contains filename to downloaded.
			 */
			Runnable client = new FileTransferClient(args[1], args[3].charAt(0), args[5], args[7], args[9]);

			new Thread (client).start();
		}

	}

}

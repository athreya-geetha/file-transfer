package server;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
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
 * @class ServerWorker
 * This is the worker class for the server. It handles individual client requests,
 * authenticates it and transfers the requested file.
 */

public class ServerWorker implements Runnable{

	// The client socket this thread will work on.
	protected Socket client = null;

	// The secret key used for encryption or decryption
	String secretKey = "7392230989250788";

	// Input stream for the socket.
	DataInputStream inStream;

	// Input stream for the socket.
	DataOutputStream outStream;

	// Constructor
	ServerWorker (Socket client)
	{		
		this.client = client;
	}

	@Override
	public void run() {

		try {

			inStream = new DataInputStream (client.getInputStream());

			outStream = new DataOutputStream (client.getOutputStream());

			// Mode for authentication
			char mode = inStream.readChar();

			// User name
			String username = inStream.readUTF();

			// Password
			String password = inStream.readUTF();

			// Registering a new client
			if(mode == 'r')
			{
				registerClient(username, password);
				outStream.writeBoolean(true);
			}
			// Logging in a new client.
			else if(mode == 'l')
			{
				if(loginClient(username, password))
					outStream.writeBoolean(true);
				else
				{
					outStream.writeBoolean(false);
					return;
				}
			}
			else
			{
				outStream.writeBoolean(false);
				return;
			}

			// Get the file name to be downloaded.
			String filename = inStream.readUTF();
			System.out.println("Opening file " + filename);

			// Send the file.
			sendfile(filename);

		} catch (IOException e) {
			System.out.println ("Caught exception: " + e.getMessage());
			e.printStackTrace();
		} 
		finally {
			// Close the connection at the end.
			closeClient();
		}
	}

	/*
	 * Closes the client connection, its input and output streams.
	 */
	private void closeClient() {
		try {
			client.close();
			inStream.close();
			outStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	/*
	 * Sending the file to the client
	 */
	private void sendfile(String filename) throws IOException {

		File file = null;

		FileInputStream fin = null;
		BufferedInputStream bin = null;

		int fileLength;
		int readByteCount = 0;
		
		Checksum check = new CRC32();
		long checkSum;
		
		Boolean sentCorrectly = false;
		int numRetries = 0;

		try {
			// Open the file.
			file = new File (filename);

			// Notify client that it is starting the download
			outStream.writeUTF("Starting");
			outStream.flush();
			
			// Notify client of the length of the file.		
			fileLength = (int) file.length();
			outStream.writeInt(fileLength);
			outStream.flush();

			fin = new FileInputStream (file);

			bin = new BufferedInputStream (fin);

			while(readByteCount <= fileLength)
			{
				// Read the file 1024 bytes at a time.
				byte[] byteFile = new byte [1024];
				int numOfBytes = bin.read(byteFile);
				if(numOfBytes < 0)
					break;
				
				readByteCount += numOfBytes;
				
				// Calculate the checksum.
				check.update(byteFile, 0, byteFile.length);
				checkSum = check.getValue();
				
				// Encrypt the contents.
				byte[] encryptedFile = encryptFile(byteFile);

				// Try sending the contents.
				do
				{
					sentCorrectly = false;
					
					// Send the contents
					outStream.write(encryptedFile, 0, encryptedFile.length);
					outStream.flush();
					
					// Get client check sum
					long clientCheckSum = inStream.readLong();
					
					// Check if the sent check sum is same as computed checksum.
					// If it is same, then it is sent correctly. Else increment 
					// the number of retries.
					if(clientCheckSum == checkSum)
					{
						sentCorrectly = true;
						outStream.writeUTF("Ok");
						outStream.flush();
					}
					else
					{
						++numRetries;
						outStream.writeUTF("Not Ok");
						outStream.flush();
					}
					
					// If we have 5 times, then abort.
					if(numRetries == 5)
					{
						outStream.writeUTF("Aborting");
						outStream.flush();
						break;
					}
				} while(sentCorrectly == false);
			}
			
			// Notify the client about end of sending.
			if(sentCorrectly == true)
			{
				System.out.println("File sent to client.");
				outStream.writeUTF("End");
				outStream.flush();
			}
			else
			{
				System.out.println("File sending aborted.");
				outStream.writeUTF("Aborted");
				outStream.flush();
			}
			
		} catch (FileNotFoundException e) {
			outStream.writeUTF("File Not Found");
			throw e;
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
			// Finally close the file and buffered input streams.
			if (bin != null)
				bin.close();

			if (fin != null)
				fin.close();
		}
	}

	/*
	 * Encrypt the file based on AES algorithm.
	 */
	private byte[] encryptFile(byte[] byteFile) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IOException, IllegalBlockSizeException, BadPaddingException {

		byte[] key = secretKey.getBytes();

		SecretKeySpec secret = new SecretKeySpec(key, "AES");

		Cipher encryptCipher = Cipher.getInstance("AES");
		encryptCipher.init(Cipher.ENCRYPT_MODE, secret);

		byte[] encryptedFile = encryptCipher.doFinal(byteFile);
		
		return encryptedFile;
	}

	/*
	 * Login the client
	 */
	private Boolean loginClient(String username, String password) {

		// Check if the user name and password exits in the server map.
		if(FileTransferServer.getUsers().containsKey(username) && FileTransferServer.getUsers().get(username).equals(password))
			return true;
		else
			return false;

	}

	private void registerClient(String username, String password) {
		
		// Add a new user to the server map.
		FileTransferServer.getUsers().put(username, password);

	}

	/*
	 * I tried to encrypt/decrypt the password, but this was throwing many exceptions.4
	 * So I left it here.
	 */
	/*private String decryptPassword(String password) {

		byte[] key = secretKey.getBytes();

		SecretKeySpec secret = new SecretKeySpec(key, "AES");

		try {
			Cipher decryptCipher;
			decryptCipher = Cipher.getInstance("AES");
			decryptCipher.init(Cipher.DECRYPT_MODE, secret);

			byte[] decryptedPass = decryptCipher.doFinal(password.getBytes()); 
			//decryptedPass = BASE64DecoderStream.decode(password.getBytes());

			return new String(decryptedPass);

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
		}

		return null;
	}*/

}

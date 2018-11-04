# Encrypted File Transfer Program using Java Sockets

The goal of this project is to transfer a file from a server to a client using Java Socket Streams. The server first authenticates the client, searches for the requested file and sends it to the client, if it is available. The contents of the file are encrypted for a secure transfer of the file. A check is performed to verify that the client has received the file correctly, i.e. its contents are same as that on the server. The detailed design is explained below.

## Detailed Design

### File Transfer Server

The server is implemented using the Socket abstraction and Listener-Worker design for multi- threaded server. By using the Listener-Worker design, the server can accept multiple concurrent requests at the same time. When the server is started, it simple creates the Listener thread and executes it. The server also has a map of all clients that are currently registered with the server. This can be used for authenticating the clients to the server for multiple sessions.
The Server Listener is responsible for creating the Server Socket at a port and waiting for client requests. The listener has a pool of worker threads. When the server receives a new client request, it hands off the request to an idle thread in the worker pool. If all threads in the pool are busy, the client will be made to wait until any threads become idle. IIt is responsible for creating and closing the server socket.

The Server Worker is responsible for handling a specific client request. It is responsible for opening and closing a specific client socket communication. It communicates with the client for honoring its request. It does the following:
  1. Reads the mode of authentication from the client, ‘r’ for registering a new client and ‘l’ for logging in an existing      client.
  
      a. If the mode is ‘r’, it adds a new client username/password to the registered users map in the server. This can be used later on to login in the same client again.
    
      b. If the mode is ‘l’, the worker checks if the username exists in the server map and password matches. If it does not, the client is not authorized and further processing cannot be done.

  2. If the client is authenticated from the above step, then it gets the filename from the client and tries to open the file. If the file is found, a message “Starting” is sent to communicate the beginning of the file transfer. If the file is not found, then a message “File not found” is sent and the request is completed.
 
  3. If the file is found, it first communicates the length of the file to the client.
  4. It then reads the file in blocks of 1024 bytes and computes a CRC checksum.
  5. The block of bytes is then encrypted using the AES algorithm.
  6. The encrypted block of bytes is sent to the client.
  7. It waits for the client to send back a checksum, which is then compared with the checksum calculated before.
      a. If both the checksums match, then it sends a message “Ok” and continues sending the rest of the file (from step iv.), until end of file is reached.
      b. If the checksums don’t match, then it sends a message “Not Ok” and retries to send the same block of bytes (from step 6.).
      c. The server retries to send the file a maximum of 5 times, after which it sends a message “Aborting”, to indicate that the file transfer is being aborted.
  8. If the file is sent is correctly, then a message “End” is sent to indicate that the entire file has been sent. Else, a message “Aborted” is sent to indicate the file was not sent fully.
  
  9. At the end, the socket communication for this client request is closed.

### File Transfer Client

The client is responsible for connecting to the server with valid credentials, requesting the file from the server and downloading it. It takes input in the following way:
-s <server address> -m <register/login> -u <username> -p <password> -f <filename> 
  
Its functionality is as described below:

  1. It validates the command line input from the user to check the command line parameters are correct.
  2. It connects to the server at the provided ‘server address’ at the port.
  3. It sends the authentication mode as ‘r’ or ‘l’ based on the mode of authentication
register/login respectively. It also sends the username and password.
  4. If it is authenticated, it requests the file to be downloaded by sending the file name. If it
receives a message “Starting” from the server, it starts the downloading.
  5. It first receives the length of the file, from the server so that it can use it for downloading
in blocks.
  6. It receives the file in blocks of 1024+16 (the extra 16 bytes are added because of
encryption on the server side), decrypts it using the DES algorithm, computes a checksum and sends it to the server.
      a. If it receives a message “Ok” from the server, it writes the received block of bytes to the file and continues the download until file-length + ((file-length / 1024) * 16) bytes are received. The extra bytes while reading are because of the encryption padding. Bad padding exception is thrown if this is not done.
      b. If it receives a message “Not Ok”, it retries to receive, decrypt, compute checksum and send it to the server, until a message “Ok” or “Aborting” is received. If the message “Aborting” is received from the server, then the download is aborted and the user is notified.
  7. The connection is closed after the process.

### Handling concurrent client requests

Since the server is implemented like a Listener-Worker, it is a multi-threaded server. For the scope of this assignment, the worker thread pool is limited to 10 threads and up to 10 clients can be waiting for getting a connection with the server.

The listener accepts the client requests and passes it on the worker thread to honor the client request and transfer the file. This means that listener is just waiting for the client request, most of the times. Every incoming connection is wrapped into a Runnable and handed off to the worker thread pool, which keeps the requests in a queue. When a thread in the pool becomes available, a Runnable from the queue is taken and executed. In this way, the server can handle many concurrent requests.

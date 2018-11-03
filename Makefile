JCC = javac

JAVA = java

RM = rm

default: FileTransferClient.class FileTransferServer.class ServerListener.class ServerWorker.class

FileTransferClient.class: client/FileTransferClient.java
	$(JCC) client/FileTransferClient.java

FileTransferServer.class: server/FileTransferServer.java
	$(JCC)  server/FileTransferServer.java

ServerListener.class: server/ServerListener.java
	$(JCC) server/ServerListener.java

ServerWorker.class: server/ServerWorker.java
	$(JCC) server/ServerWorker.java

runClient:
	$(JAVA) client/FileTransferClient -s 10.234.140.27 -m register -u geetha -p password -f A1.pdf

runServer:
	$(JAVA) server/FileTransferServer

clean: 
	$(RM) com/dcs/*.class com/dcs/beans/*.class

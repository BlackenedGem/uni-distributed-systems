## Overview
RMI is used to show how we can easily implement redundant servers. We have 1 front, multiple servers, and a client (multiple clients out of spec). The client has no knowledge of the servers, only the front end. We support basic operations such as listing files, uploading files, downloading files, and deleting files. These operations occur on all servers for redundancy. The exception is uploading files where we have a 'high reliability' mode that uploads to all servers rather than one.

## Usage
Port 1099 is used by default and is hardcoded to the frontend (but not server/client). I should have made the frontend changeable in retrospect. Server files are stored in server_files_id where id is server dependent. 

* Start the RMI registry in the appropriate directory (eg. where the class files are located). This can be done via ```start rmiregistry 1099```
* Start the front end (FrontEnd.class) (no arguments required)
* Start the servers (Server.class). The server id, IP, and port should be specified. For example ```1 localhost 1099``` for server 1.
* Start the client (ClientGUI.class)

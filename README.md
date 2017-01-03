[![Build Status](https://travis-ci.org/tresata/akka-http-spnego.svg?branch=master)](https://travis-ci.org/tresata/akka-http-spnego)

# akka-http-spnego
akka-http-spnego provides Kerberos based authentication for akka-http using SPNEGO. SPNEGO is a way to do GSSAPI authentication between clients and servers using the HTTP authentication header.

The included test-server project is both an example of how to use this project and a way to quickly test it within your Kerberos environment. It provides a secure ping-pong service (you do a GET to endpoint /ping and it responds with pong for the authenticated user).
To run it first make sure:
* Kerberos is properly installed and configured on both server and client
* DNS is properly configured for the server (it's hostname resolves correctly to its ip-address and the other way around).

Next do the following: 
* Create a keytab for HTTP/yourserver@YOURDOMAIN on the server
* Edit test-server/src/main/resources/application.conf to set tresata.akka.http.spnego.kerberos.principal and tresata.akka.http.spnego.kerberos.keytab 
* Launch the test server with: ./launch-test-server
* On the client make sure you are logged into Kerberos (with kinit)
* On the client create a test connection with: curl -k --negotiate -u : -b ~/cookiejar.txt -c ~/cookiejar.txt https://yourserver:12345/ping

Have fun!
Team @ Tresata

 1. Drop Synapse.war file in dist folder into wepapp foloder in tomcat (or any other application server)
 2. Restart tomcat
 3. Then you can see tomcat will expand war file for you , but that exploded war file does not complete
 4. Copy ALL Axis2 dependecy lib files into WEB-INF/lib folder and
 5. Restart tomcat again
 6. Then run Clinet.java file (core/src/client)
 7. In tomcat console you can see some souts, depending on rules


I will drop a mail by describing the toy soon

Sorry I did not have time to crtea an ant buider file (I just spent about three hours on this)
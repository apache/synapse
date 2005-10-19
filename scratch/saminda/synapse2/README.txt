Please follow the instruction to Run the code
-------------------------------------------------------

1. Add all the relevant Axis2 libraries to “lib” and “server/lib” folders.

2. Change the build.xml as follows 

In “build” file there is an entry,

<copy file="/home/saminda/myprojects/synapse2/build/services/mediation.aar"
        todir="/home/saminda/myprojects/synapse2/server/services" overwrite="true"/>

Say you have get a checkout to /usr/tmp in your system, Linux system. 

Then replace the “bold” text with following, 

<copy file="/usr/tmp/synapse2/build/services/mediation.aar"
        todir="/usr/tmp/synapse2/server/services" overwrite="true"/> 

Window system, if you take a checkout to C:\usr\tmp

<copy file="C:/usr/tmp/synapse2/build/services/mediation.aar"
        todir="C:/usr/tmp/synapse2/server/services" overwrite="true"/> 

Just change the other 3 entries as there as mentioned above. 

3.Then type “ant”, which will manipulate the rest.

4.In your favorite IDE, go to Class “SimpleServer” and make the repository as 
	SimpleHTTPServer sas = new SimpleHTTPServer(
                    "/usr/tmp/server", 8081); // Linux
	Or	            
	SimpleHTTPServer sas = new SimpleHTTPServer(
                    "C:\usr\tmp\server", 8081); // Windows 

	and run it.
 
5.Then run the client “SimpleClient”.

{Optional}
6.If you have configured Axis2 in Tomcat, which runs in port 8080, with MyService.aar on it. Start tomcat.

7.Once you run the sample you will see the mediation components being executed and Message is routed to Original service. 


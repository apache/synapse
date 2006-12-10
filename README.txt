==========================================================================
Apache Sandesha2 @VERSION@ build

http://ws.apache.org/sandesha/sandesha2
---------------------------------------------------------------------------

Apache Sandesha2 is a WS-ReliableMessaging implementation on top of Apache 
Axis2. If you are looking for a WS-ReliableMessaging implementation for 
Apache Axis 1.x, please go for Sandesha 1.0 which is located at
http://svn.apache.org/repos/asf/webservices/sandesha/branches/sandesha_1_0/

----------------------------------------------------------------------------

Installation
============
Download and install Apache Axis2. (see http://ws.apache.org/axis2 for more
details).
Add a user phase named RMPhase to the all four flows of the axis2.xml.
Get the binary distribution of Sandesha2 and extract it. You will find the 
sandesha2-@VERSION@.mar file inside that. This is the current Sandesha2 module 
file.
Put Sandesha2 module file to <Axis2_webapp>/WEB-INF/modules directory.
Put sandesha2-policy-@VERSION@.jar file that comes with the distribution to the <Axis2_webapp>/WEB-INF/lib directory.
 
Using Sandesha2 in the server side
===================================
Put a module reference for the Sandesha module in the services.xml files of the
services to which you hope to give the RM capability.
For e.g.
<service>
    <module ref="sandesha2" />
    ...........
    ...........
</service>

Using Sandeshsa2 in the client side
===================================

Engage sandesha2 and addressing modules to the ServiceClient object before
doing any invocation. If you are not using any advance features add the 
sandesha2-client-@VERSION@.jar to your classpath. Otherwise you will have to add
the sandesha2-@VERSION@.jar file to the classpath. Add the sandesha2-policy-@VERSION@.jar 
to your classpath.

Please see Sandesha2 user guide for more details and sample code on using 
Sandesha2.


Documentation
=============
Documentation for Sandesha2 can be found in xdocs directory in the Sandesha2 
distribution.

Support
=======
Please post any problem you encounter to the sandesha developer list 
(sandesha-dev@ws.apache.org). Please remember to mark the subject with the [Sandesha2]
prefix. Your comments are highly appreciated and really needed to make this distribution
a successful one.

Apache Sandesha2 team.

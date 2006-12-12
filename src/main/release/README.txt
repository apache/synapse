Apache Synapse 0.90 build  (December 2006)

http://incubator.apache.org/synapse/
------------------------------------------------------

Apache Synapse is an effort undergoing incubation at the Apache Software Foundation (ASF),
sponsored by the Web Services PMC. Incubation is required of all newly accepted projects 
until a further review indicates that the infrastructure, communications, and decision 
making process have stabilized in a manner consistent with other successful ASF projects. 
While incubation status is not necessarily a reflection of the completeness or stability 
of the code, it does indicate that the project has yet to be fully endorsed by the ASF.

-------------------
Documentation
===================
 
Documentation can be found in the 'docs' directory included with the 
binary distribution and in the 'src/site/resources' directory in the source 
distribution. 

For examples on Apache Synapse message mediation please see the Synapse_Samples.html

For more information on the Synapse Configuration language syntax and useage refer to
Synapse_Configuration_Language.html

-------------------
Getting Started
===================

Refer to the Synapse_Samples.html document for examples of Synapse usage and configuration.

The actual sample Synapse configurations could be found at <SYNAPSE>/respository/conf/sample.
The resources sub-directory contains the sample XSLT transformations, XSD schemas, WS policies
and all other resources required to demonstrate various aspects of Synapse.

Synapse uses Log4J for its logging, and the default log level on the code is set to INFO. If you
are trying out the samples and is new to Synapse, you may enable DEBUG level logging on the 
log4j.properties found in the lib sub directory as follows. This would help you see what actually
takes place during Synapse mediation.

	e.g. log4j.category.org.apache.synapse=DEBUG


Support
===================
 
Any issues with this release can be reported to Apache Synapse mailing list 
or in the JIRA issue tracker.

Mailing list subscription:
    synapse-dev-subscribe@ws.apache.org

Jira:
    http://issues.apache.org/jira/browse/Synapse

Thank you for using Synapse!

The Synapse Team. 

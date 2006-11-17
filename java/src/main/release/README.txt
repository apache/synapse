======================================================
#   Licensed to the Apache Software Foundation (ASF) under one
#   or more contributor license agreements.  See the NOTICE file
#   distributed with this work for additional information
#   regarding copyright ownership.  The ASF licenses this file
#   to you under the Apache License, Version 2.0 (the
#   "License"); you may not use this file except in compliance
#   with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing,
#   software distributed under the License is distributed on an
#    #  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#   KIND, either express or implied.  See the License for the
#   specific language governing permissions and limitations
#   under the License.

Apache Synapse 0.90 build  (November, 2006)

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
binary distribution and in the 'xdocs' directory in the source 
distribution. Documentation on samples are available in the samples directory.

For examples on Apache Synapse message mediation please see the User Guide
(http://wiki.apache.org/ws/Synapse/UserGuide). 

For more information on the Synapse Configuration language syntax and useage refer to
the wiki page at http://wiki.apache.org/incubator/Synapse/SynapseConfigurationLanguage

-------------------
Getting Started
===================

Synapse is typically configured using a synapse.xml file in the repository directory. 
In the binary distribution this is the <SYNAPSE>\synapse_repository directory, where
<SYNAPSE> is the directory you installed Apache Synapse into. 

You can start the sample Apache Synapse configuration using the bin\synapse command (bat or sh)
This will pick up the Synapse configuration at <SYNAPSE>\synapse_repository\conf\synapse.xml
and the Axis2 configuration from <SYNAPSE>\synapse_repository\conf\axis2.xml

More sample Synapse configurations could be found at <SYNAPSE>\synapse_respository\conf\sample.
This directory contains sample XSLT transformations, XSD schemas for validation and resources
required to demonstrate support for Spring beans, and programatic creation of a Synapse
configuration in addition to sample Synapse configuration XML files. 

The Synapse configuration language is specified at the Wiki page given below.
http://wiki.apache.org/incubator/Synapse/SynapseConfigurationLanguage

See the README.txt in the samples directory for more information about the samples.

Support
===================
 
Any problem with this release can be reported to Apache Synapse mailing list 
or in the JIRA issue tracker.

Mailing list subscription:
    synapse-dev-subscribe@ws.apache.org

Jira:
    http://issues.apache.org/jira/browse/Synapse

Thank you for using Synapse!

The Synapse Team. 

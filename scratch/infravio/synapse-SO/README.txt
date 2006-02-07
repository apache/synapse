Features:
1) Create SynapseObject from SynapseObject-XML
2) Create SynapseObject from any xml using BO xslt
3) SynapseObject can have a SynapseObject (self aggregation)
4) SynapseObject can have 0 or more attributes
5) To set attribute for a SynapseMediatorObject there are setters/getters
   methods such as setString(String name,String value), setLong(String name,String value),etc.
   eg. SetString(foo,value)
6) SynapseObject has a getXMLFragment method which spits out an xml
   representing the BO itself
7) SynapseObject have finder methods based on attribute names, values as
   well as businessObjects.
<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="2.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:fn="http://www.w3.org/2005/02/xpath-functions"
	xmlns:m0="http://www.apache-synapse.org/test"
	exclude-result-prefixes="m0 fn">
<xsl:output method="xml" omit-xml-declaration="yes" indent="yes"/>

<xsl:template match="/">
  <xsl:apply-templates select="//m0:CheckPriceRequest" /> 
</xsl:template>
  
<xsl:template match="m0:CheckPriceRequest">

<m:getQuote xmlns:m="http://services.samples/xsd">
	<m:request>
		<m:symbol><xsl:value-of select="m0:Code"/></m:symbol>
	</m:request>
</m:getQuote>

</xsl:template>
</xsl:stylesheet>
<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="2.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:fn="http://www.w3.org/2005/02/xpath-functions"
	xmlns:m0="http://ws.invesbot.com/"
	exclude-result-prefixes="m0 fn">
<xsl:output method="xml" omit-xml-declaration="yes" indent="yes"/>

<xsl:template match="/">
  <xsl:apply-templates select="//m0:StockQuote" /> 
</xsl:template>
  
<xsl:template match="m0:StockQuote">

<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
<soap:Body>
<m:CheckPriceResponse xmlns:m="http://www.apache-synapse.org/test">
	<m:Code><xsl:value-of select="m0:Symbol"/></m:Code>
	<m:Price><xsl:value-of select="m0:Price"/></m:Price>
</m:CheckPriceResponse>
</soap:Body>
</soap:Envelope>

</xsl:template>
</xsl:stylesheet>
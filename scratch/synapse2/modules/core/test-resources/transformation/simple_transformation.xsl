<xsl:stylesheet version='1.0'
                xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
    <xsl:template match="/">
        <transformedText>
            <xsl:value-of select="//text"/>
            <xsl:text>Test Being Transformed</xsl:text>
        </transformedText>
    </xsl:template>
</xsl:stylesheet>

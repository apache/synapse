<?xml version="1.0" encoding="ISO-8859-1"?>
<!--
  ~  Licensed to the Apache Software Foundation (ASF) under one
  ~  or more contributor license agreements.  See the NOTICE file
  ~  distributed with this work for additional information
  ~  regarding copyright ownership.  The ASF licenses this file
  ~  to you under the Apache License, Version 2.0 (the
  ~  "License"); you may not use this file except in compliance
  ~  with the License.  You may obtain a copy of the License at
  ~
  ~   http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~  Unless required by applicable law or agreed to in writing,
  ~  software distributed under the License is distributed on an
  ~   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~  KIND, either express or implied.  See the License for the
  ~  specific language governing permissions and limitations
  ~  under the License.
  -->

<!--
This is the synapse migration xslt which will migrate the configuration from the 1.x version to the 2.x compatible version 
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:syn="http://ws.apache.org/ns/synapse"
                xmlns:synNew="http://synapse.apache.org/ns/2010/04/configuration"
                exclude-result-prefixes="syn">

    <xsl:output method="xml" indent="yes" encoding="UTF-8"/>

    <xsl:template match="syn:*" priority="0">
        <xsl:call-template name="convertNS"/>
    </xsl:template>

    <xsl:template match="syn:definitions/syn:sequence | syn:definitions/syn:localEntry | syn:definitions/syn:proxy | syn:definitions/syn:task | syn:definitions/syn:endpoint" priority="2">
        <xsl:call-template name="convertNS"/>
    </xsl:template>

    <xsl:template match="syn:definitions | synNew:definitions" priority="1">
        <xsl:element name="definitions" namespace="http://synapse.apache.org/ns/2010/04/configuration">
            <xsl:element name="sequence" namespace="http://synapse.apache.org/ns/2010/04/configuration">
                <xsl:attribute name="name">main</xsl:attribute>
                <xsl:for-each select="syn:* | synNew:*">
                    <xsl:if test="local-name()!='sequence' and local-name()!='localEntry' and local-name()!='proxy' and local-name()!='task' and local-name()!='endpoint'">
                        <xsl:call-template name="convertNS"/>
                    </xsl:if>
                </xsl:for-each>
            </xsl:element>
            <xsl:for-each select="syn:* | synNew:*">
                <xsl:if test="local-name()='sequence' or local-name()='localEntry' or local-name()='proxy' or local-name()='task' or local-name()='endpoint'">
                    <xsl:apply-templates select="."/>
                </xsl:if>
            </xsl:for-each>
        </xsl:element>
    </xsl:template>

    <xsl:template match="/ | @* | node() | text() | processing-instruction()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="comment()" xml:space="preserve">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()" />
        </xsl:copy>
    </xsl:template>

    <xsl:template name="convertNS">
        <xsl:element name="{local-name()}" namespace="http://synapse.apache.org/ns/2010/04/configuration">
            <xsl:copy-of select="@*"/>
            <xsl:apply-templates/>
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>

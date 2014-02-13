<?xml version="1.0" encoding="UTF-8"?>

<!--
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
-->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" encoding="ISO-8859-1"/>

<xsl:param name="author">xwiki:XWiki.Admin</xsl:param>

<xsl:template match="@* | node()">

   <xsl:copy>
   <xsl:apply-templates select="@* | node()" />
   </xsl:copy>
</xsl:template>

<xsl:template match="xwikidoc/*[self::author or self::creator or self::contentAuthor]">
  <!-- I will admit my incompetence and say that I don't understand why I cannot just call local-name(.) 
       in the name attribute of the xsl-element, so I put it in a var before to be used as the element name -->
  <xsl:variable name="currentNodeName"><xsl:value-of select="local-name(.)" /></xsl:variable>
  <xsl:element name="{$currentNodeName}"><xsl:value-of select="$author" /></xsl:element>
</xsl:template>
</xsl:stylesheet>
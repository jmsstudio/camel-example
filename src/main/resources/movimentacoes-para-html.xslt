<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="/movimentacoes">

        <html>
            <body>
                <table>
                    <xsl:for-each select="movimentacao">
                        <tr>
                            <td><xsl:value-of select="valor"/></td>
                            <td><xsl:value-of select="data"/></td>
                            <td><xsl:value-of select="tipo"/></td>
                        </tr>
                    </xsl:for-each>
                </table>
            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>
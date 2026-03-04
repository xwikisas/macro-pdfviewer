/*
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
 */
package com.xwiki.pdfviewer.po;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents a PDF Viewer macro and provides access to its attributes.
 *
 * @version $Id$
 * @since 2.6.3
 */
public class PDFViewerMacro extends BaseElement
{
    private final WebElement macro;

    public PDFViewerMacro(WebElement macro)
    {
        this.macro = macro;
    }

    public String getText()
    {

        getDriver().switchTo().frame(macro);
        WebElement textLayer = getDriver().findElement(By.cssSelector(".textLayer"));

        String text = textLayer.getText();
        getDriver().switchTo().defaultContent();

        return text;
    }

    public String getWidth()
    {
        return macro.getAttribute("width").trim();
    }

    public String getHeight()
    {
        return macro.getCssValue("height");
    }

    public String getPdfUrl()
    {
        String src = macro.getAttribute("src");

        String encodedPdf = src.split("file=")[1].split("&")[0];
        return URLDecoder.decode(encodedPdf, StandardCharsets.UTF_8);
    }
}

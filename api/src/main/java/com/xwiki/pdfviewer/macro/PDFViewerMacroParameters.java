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
package com.xwiki.pdfviewer.macro;

import org.xwiki.properties.annotation.PropertyAdvanced;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyDisplayType;
import org.xwiki.properties.annotation.PropertyMandatory;

import com.xwiki.pdfviewer.PDFResourceReference;

public class PDFViewerMacroParameters
{
    /**
     * To add.
     */
    private String file;

    /**
     * To add.
     */
    private String document;

    /**
     * To add.
     */
    private String width;

    /**
     * To add.
     */
    private int height;

    /**
     * To add.
     */
    private boolean asauthor;

    /**
     * To add.
     * 
     * @return to add
     */
    public String getFile()
    {
        return this.file;
    }

    /**
     * To add.
     * 
     * @param file to add
     */
    @PropertyDescription("File or URL path to the PDF file")
    @PropertyDisplayType(PDFResourceReference.class)
    @PropertyMandatory
    public void setFile(String file)
    {
        this.file = file;
    }

    /**
     * To add.
     * 
     * @return to add
     */
    public String getDocument()
    {
        return document;
    }

    /**
     * To add.
     * 
     * @param document to add
     */
    @PropertyDescription("Reference of the document containing the file (not used if file is an URL)")
    public void setDocument(String document)
    {
        this.document = document;
    }

    /**
     * To add.
     * 
     * @return to add
     */
    public String getWidth()
    {
        return width;
    }

    /**
     * To add.
     * 
     * @param width to add
     */
    public void setWidth(String width)
    {
        this.width = width;
    }

    /**
     * To add.
     * 
     * @return to add
     */
    public int getHeight()
    {
        return height;
    }

    /**
     * To add.
     * 
     * @param height to add
     */
    public void setHeight(int height)
    {
        this.height = height;
    }

    /**
     * To add.
     * 
     * @return to add
     */
    public boolean getAsauthor()
    {
        return asauthor;
    }

    /**
     * To add.
     * 
     * @param asauthor to add
     */
    @PropertyDescription("If this value is true (or 1 or yes) and the user has no access to the  document to which "
        + "the PDF file is attached, the PDF file could still be viewed on behalf of the author of the document "
        + "containing the macro (if that author has access to the containing document obviously).")
    @PropertyAdvanced
    public void setAsauthor(boolean asauthor)
    {
        this.asauthor = asauthor;
    }

}

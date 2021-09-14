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
import org.xwiki.properties.annotation.PropertyDisplayType;
import org.xwiki.properties.annotation.PropertyMandatory;

import com.xwiki.pdfviewer.PDFResourceReference;

public class PDFViewerMacroParameters
{
    /**
     * The PDF file to be viewer. Use the full attachment reference to specify the PDF file, an absolute URL or only the
     * name of the attachment when is used along with the document parameter.
     */
    private String file;

    /**
     * The viewer width, in pixels. Uses a percentage value, for example: 25%, 50%, 100%.
     */
    private String width = "100%";

    /**
     * The viewer height, in pixels.
     */
    private int height = 1000;

    /**
     * String reference of the document that contains the file. It is ignored if file is an URL or the resulted
     * attachment reference does not actually exists.
     */
    private String document;

    /**
     * If this value is true (or 1 or yes) and the user has no access to the  document to which the PDF file is
     * attached, the PDF file could still be viewed on behalf of the author of the document containing the macro (if
     * that author has access to the containing document). TODO: change the type to boolean and add a migration for the
     * old values.
     */
    private String asAuthor = "0";

    /**
     * @return the reference to the PDF file, an absolute URL or simply the file name
     */
    public String getFile()
    {
        return this.file;
    }

    /**
     * Set the value of the PDF file by using a full attachment reference, an absolute URL or simply the file name.
     * 
     * @param file the reference to the PDF file, an absolute URL or simply the file name
     */
    @PropertyDisplayType(PDFResourceReference.class)
    @PropertyMandatory
    public void setFile(String file)
    {
        this.file = file;
    }

    /**
     * @return the display width of the PDF file
     */
    public String getWidth()
    {
        return width;
    }

    /**
     * Set the display width of the PDF file.
     * 
     * @param width display width of the PDF file
     */
    public void setWidth(String width)
    {
        this.width = width;
    }

    /**
     * @return the display height of the PDF file
     */
    public int getHeight()
    {
        return height;
    }

    /**
     * Set the display height of the PDF file.
     * 
     * @param height display height of the PDF file
     */
    public void setHeight(int height)
    {
        this.height = height;
    }

    /**
     * @return the reference of the document that contains the PDF file to be viewed
     */
    public String getDocument()
    {
        return document;
    }

    /**
     * Set the reference of the document that contains the PDF file to be viewed.
     * 
     * @param document reference of the document that contains the PDF file
     */
    @PropertyAdvanced
    public void setDocument(String document)
    {
        this.document = document;
    }

    /**
     * @return true (or 1 or yes) if the view right of the PDF file document should be delegated to the users that
     *         requests to see it through the macro, false (or 0 or no) otherwise
     */
    public String getAsAuthor()
    {
        return asAuthor;
    }

    /**
     * Set whether to delegate the view right of document where the PDF file is located to the user that wants to see it
     * through the pdfviewer macro.
     * 
     * @param asAuthor true (or 1 or yes) if the view right of the PDF file document should be delegated to the users
     *            that requests to see it through the macro, false (or 0 or no) otherwise
     */
    @PropertyAdvanced
    public void setAsAuthor(String asAuthor)
    {
        this.asAuthor = asAuthor;
    }

}

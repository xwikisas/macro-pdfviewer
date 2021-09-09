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
import org.xwiki.properties.annotation.PropertyName;

import com.xwiki.pdfviewer.PDFResourceReference;

public class PDFViewerMacroParameters
{
    /**
     * The PDF file to be viewer. Use the full attachment reference to specify the PDF file, an absolute URL or only the
     * name of the attachment when is used along with the document parameter.
     */
    private String file;

    /**
     * The viewer width, in pixels. Use a percentage value, example: 25%, 50%, 100%.
     */
    private String width = "100%";

    /**
     * The viewer height, in pixels.
     */
    private int height = 1000;

    /**
     * String reference of the document that contains the file. It is not used if file is an URL or the resulted
     * attachment reference does not actually exists.
     */
    private String document;

    /**
     * If this value is true and the user has no access to the  document to which the PDF file is attached, the PDF file
     * could still be viewed on behalf of the author of the document containing the macro (if that author has access to
     * the containing document).
     */
    private boolean asauthor;

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
    @PropertyDescription("The full PDF file reference, an absolute URL or only the name of the attachment when is "
        + "used along with the document parameter.")
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
    @PropertyDescription("The viewer width, in pixels. If not defined the default value will be used")
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
    @PropertyDescription("The viewer height, in pixels. If not defined the default value will be used")
    public void setHeight(int height)
    {
        this.height = height;
    }

    /**
     * @return the reference of the document that contains the file to be viewed
     */
    public String getDocument()
    {
        return document;
    }

    /**
     * Set the reference of the document that contains the file to be viewed.
     * 
     * @param document reference of the document that contains the file
     */
    @PropertyDescription("Reference to the XWiki document where the file is attached. If not defined, the current "
        + "document or only the file parameter are used. If file argument is an absolute URL or the attachment does "
        + "not exists, this argument is ignored.")
    @PropertyAdvanced
    public void setDocument(String document)
    {
        this.document = document;
    }

    /**
     * @return {@code true} if the view right of the PDF file document should be delegated to the users that requests to
     *         see it through the macro, {@code false} otherwise
     */
    public boolean getAsauthor()
    {
        return asauthor;
    }

    /**
     * Set whether to delegate the view right of document where the PDF file is located to the user that wants to see it
     * through the pdfviewer macro.
     * 
     * @param asauthor {@code true} if the view right of the PDF file document should be delegated to the users that
     *            requests to see it through the macro, {@code false} otherwise
     */
    @PropertyDescription("If this value is true and the user has no access to the  document to which the PDF file is "
        + "attached, the PDF file could still be viewed on behalf of the author of the document containing the macro "
        + "(if that author has access to the containing document).")
    @PropertyAdvanced
    @PropertyName("Delegate my view right")
    public void setAsauthor(boolean asauthor)
    {
        this.asauthor = asauthor;
    }

}

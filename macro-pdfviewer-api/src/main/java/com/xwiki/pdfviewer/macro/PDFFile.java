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

import java.util.Optional;

import org.xwiki.model.reference.AttachmentReference;

/**
 * Represents the resources for a given attachment.
 *
 * @version $Id$
 * @since 2.6
 */
public class PDFFile
{
    private AttachmentReference attachmentReference;

    private String url;

    /**
     * Default constructor.
     *
     * @param attachmentReference the {@link AttachmentReference} of the PDF file.
     * @param url file download url.
     */
    public PDFFile(AttachmentReference attachmentReference, String url)
    {
        this.url = url;
        this.attachmentReference = attachmentReference;
    }

    /**
     * Get the file download url.
     *
     * @return the file download url.
     */
    public String getURL()
    {
        return url;
    }

    /**
     * Get PDF file reference.
     *
     * @return an {@link Optional} {@link AttachmentReference}, if the PDF file is attached to a wiki page.
     */
    public Optional<AttachmentReference> getAttachmentReference()
    {
        return Optional.ofNullable(attachmentReference);
    }
}

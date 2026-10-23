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

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PDFFile}.
 *
 * @version $Id$
 */
class PDFFileTest
{
    private final AttachmentReference attachmentReference =
        new AttachmentReference("file.pdf", new DocumentReference("wiki", "Space", "Page"));

    @Test
    void emptyConstructor()
    {
        PDFFile pdfFile = new PDFFile();

        assertEquals("", pdfFile.getURL());
        assertFalse(pdfFile.getAttachmentReference().isPresent());
        assertFalse(pdfFile.hasViewRights());
        assertFalse(pdfFile.areViewRightsDelegated());
    }

    @Test
    void defaultConstructor()
    {
        PDFFile pdfFile = new PDFFile(this.attachmentReference, "url", true);

        assertEquals("url", pdfFile.getURL());
        assertEquals(this.attachmentReference, pdfFile.getAttachmentReference().get());
        assertTrue(pdfFile.hasViewRights());
        assertFalse(pdfFile.areViewRightsDelegated());
    }
}

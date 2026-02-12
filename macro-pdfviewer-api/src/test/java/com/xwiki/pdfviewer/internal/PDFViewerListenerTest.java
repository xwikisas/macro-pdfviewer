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
package com.xwiki.pdfviewer.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.event.Event;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.AttachmentDeletedEvent;
import com.xwiki.pdfviewer.internal.token.DelegatedTokenManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link PDFViewerListener}
 *
 * @version $Id$
 */
@ComponentTest
class PDFViewerListenerTest
{
    @InjectMockComponents
    private PDFViewerListener pdfViewerListener;

    @MockComponent
    private DelegatedTokenManager tokenManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.DEBUG);

    @Mock
    private DocumentReference documentReference;

    @Mock
    private XWikiDocument xWikiDocument;

    @Test
    void onAttachmentDeletedEventSuccess()
    {
        Event event = new AttachmentDeletedEvent("test document name", "test attachment name");
        when(xWikiDocument.getDocumentReference()).thenReturn(documentReference);
        pdfViewerListener.onEvent(event, xWikiDocument, null);
        assertEquals("Successfully removed all tokens granted for [test attachment name]", logCapture.getMessage(0));
        verify(tokenManager, times(1)).clearAttachmentTokens("test attachment name", documentReference);
    }

    @Test
    void onAttachmentDeletedEventFail()
    {
        Event event = new AttachmentDeletedEvent("test document name", "test attachment name");
        doThrow(new RuntimeException("test error message")).when(tokenManager)
            .clearAttachmentTokens("test attachment name", null);
        RuntimeException exception =
            assertThrows(RuntimeException.class, () -> this.pdfViewerListener.onEvent(event, xWikiDocument, null));
        assertEquals("java.lang.RuntimeException: test error message", exception.getMessage());
        assertEquals(
            "An error occurred while removing PDF Viewer access tokens. Root cause is: [RuntimeException: test error "
                + "message]", logCapture.getMessage(0));
    }
}

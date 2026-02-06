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
package com.xwiki.pdfviewer.internal.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.pdfviewer.internal.token.DelegatedTokenManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ComponentTest
public class DefaultPDFMacroResourceTest
{
    private static final String TOKEN = "this_is_a_test_token";

    private static final String FORM_TOKEN = "this_is_a_test_form_token";

    private static final String ATTACHMENT_CONTENT = "This is the attachment content";

    private static final String UNAUTHORIZED_ERROR_MESSAGE =
        "Failed to get content of requested file due to invalid token or restricted rights.";

    @InjectMockComponents
    private DefaultPDFMacroResource defaultPDFMacroResource;

    @MockComponent
    private DelegatedTokenManager tokenManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @MockComponent
    private CSRFToken csrf;

    @Mock
    private XWikiContext wikiContext;

    @Mock
    private XWiki wiki;

    @Mock
    private AttachmentReference attachmentReference;

    @Mock
    private DocumentReference docRef;

    @Mock
    private XWikiDocument document;

    @Mock
    private XWikiAttachment attachment;

    @BeforeEach
    void setup()
    {
        when(contextProvider.get()).thenReturn(wikiContext);
        when(wikiContext.getWiki()).thenReturn(wiki);
        when(tokenManager.isInvalid(TOKEN)).thenReturn(false);
        when(tokenManager.hasAccess(TOKEN)).thenReturn(true);
        when(csrf.isTokenValid(FORM_TOKEN)).thenReturn(true);
    }

    @Test
    void getContents() throws XWikiException, XWikiRestException, IOException
    {

        when(tokenManager.getTokenAttachmentReference(TOKEN)).thenReturn(attachmentReference);
        when(attachmentReference.getDocumentReference()).thenReturn(docRef);
        when(wiki.getDocument(docRef, wikiContext)).thenReturn(document);
        when(document.getAttachment(null)).thenReturn(attachment);
        when(attachment.getContentInputStream(wikiContext)).thenReturn(
            new ByteArrayInputStream(ATTACHMENT_CONTENT.getBytes()));

        Response response =
            defaultPDFMacroResource.getContents(new String(Base64.getEncoder().encode(TOKEN.getBytes())), FORM_TOKEN);
        assertEquals(200, response.getStatus());
        assertEquals(ATTACHMENT_CONTENT,
            IOUtils.toString((ByteArrayInputStream) response.getEntity(), StandardCharsets.UTF_8));
    }

    @Test
    void getContentsInvalidToken() throws XWikiRestException
    {
        when(tokenManager.isInvalid(TOKEN)).thenReturn(true);

        Response response =
            defaultPDFMacroResource.getContents(new String(Base64.getEncoder().encode(TOKEN.getBytes())), FORM_TOKEN);
        assertEquals(401, response.getStatus());
        assertEquals(UNAUTHORIZED_ERROR_MESSAGE, logCapture.getMessage(0));
    }

    @Test
    void getContentsNoAccess() throws XWikiRestException
    {
        when(tokenManager.hasAccess(TOKEN)).thenReturn(false);

        Response response =
            defaultPDFMacroResource.getContents(new String(Base64.getEncoder().encode(TOKEN.getBytes())), FORM_TOKEN);
        assertEquals(401, response.getStatus());
        assertEquals(UNAUTHORIZED_ERROR_MESSAGE, logCapture.getMessage(0));
    }

    @Test
    void getContentsInvalidCSRF() throws XWikiRestException
    {
        when(csrf.isTokenValid(FORM_TOKEN)).thenReturn(false);

        Response response =
            defaultPDFMacroResource.getContents(new String(Base64.getEncoder().encode(TOKEN.getBytes())), FORM_TOKEN);
        assertEquals(401, response.getStatus());
        assertEquals(UNAUTHORIZED_ERROR_MESSAGE, logCapture.getMessage(0));
    }

    @Test
    void getContentsAttachmentError() throws XWikiException
    {

        when(tokenManager.getTokenAttachmentReference(TOKEN)).thenReturn(attachmentReference);
        when(attachmentReference.getDocumentReference()).thenReturn(docRef);
        when(wiki.getDocument(docRef, wikiContext)).thenThrow(
            new XWikiException("test exception", new RuntimeException()));
        WebApplicationException exception = assertThrows(WebApplicationException.class,
            () -> defaultPDFMacroResource.getContents(new String(Base64.getEncoder().encode(TOKEN.getBytes())),
                FORM_TOKEN));
        assertEquals(500, exception.getResponse().getStatus());
        assertEquals("An error occurred while attempting to retrieve file content.", logCapture.getMessage(0));
    }
}

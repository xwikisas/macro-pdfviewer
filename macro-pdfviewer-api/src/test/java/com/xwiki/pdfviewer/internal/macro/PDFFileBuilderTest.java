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
package com.xwiki.pdfviewer.internal.macro;

import java.util.Base64;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.EntityType;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiRequest;
import com.xwiki.pdfviewer.internal.macro.authorization.PDFFileAuthorization;
import com.xwiki.pdfviewer.internal.macro.authorization.PDFViewerAuthorizationManager;
import com.xwiki.pdfviewer.internal.token.DelegatedTokenManager;
import com.xwiki.pdfviewer.macro.PDFFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link PDFFileBuilder}
 *
 * @version $Id$
 */
@ComponentTest
class PDFFileBuilderTest
{
    private static final String PDF_CONTENT_FORMAT = "%s/rest/pdfmacro/contents?access_token=%s";

    private static final String PDF_URL_EXTERNAL = "https://some_attachment";

    private static final String PDF_INTERNAL = "some internal attachment";

    private static final String PDF_INTERNAL_URL = "some internal attachment URL";

    private static final String TOKEN_ID = "tokenID";

    private static final String CONTEXT_PATH = "some path";

    private static final String OWNER_DOC_REF = "owner doc ref";

    private static final String EXPECTED_PATH =
        String.format(PDF_CONTENT_FORMAT, CONTEXT_PATH, Base64.getUrlEncoder().encodeToString(TOKEN_ID.getBytes()));

    private final DocumentReference docRef = new DocumentReference("wiki", "space", "page");

    private final AttachmentReference attachmentRef = new AttachmentReference(PDF_INTERNAL, docRef);

    @InjectMockComponents
    private PDFFileBuilder pdfFileBuilder;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    @Named("resource/standardURL")
    private EntityReferenceResolver<String> urlEntityReferenceResolver;

    @MockComponent
    @Named("current")
    private EntityReferenceResolver<String> entityReferenceResolver;

    @MockComponent
    private Provider<XWikiContext> wikiContextProvider;

    @MockComponent
    private DelegatedTokenManager tokenManager;

    @MockComponent
    private PDFViewerAuthorizationManager pdfViewerAuthManager;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentUserSerializer;

    @Mock
    private DocumentReference userSdocRef;

    @Mock
    private DocumentReference userDocRef;

    @Mock
    private XWikiContext wikiContext;

    @Mock
    private XWiki xwiki;

    @Mock
    private XWikiDocument wikiDocument;

    @Mock
    private XWikiDocument wikiDocument2;

    @Mock
    private XWikiDocument sdoc;

    @Mock
    private XWikiAttachment wikiAttachment;

    @Mock
    private XWikiRequest request;

    @Mock
    private DocumentAuthors documentAuthors;

    @Mock
    private UserReference sdocUserRef;

    @BeforeEach
    void setup() throws XWikiException
    {
        when(wikiContextProvider.get()).thenReturn(wikiContext);
        when(wikiContext.getWiki()).thenReturn(xwiki);
        when(wikiContext.get(XWikiDocument.CKEY_SDOC)).thenReturn(sdoc);
        when(wikiContext.getRequest()).thenReturn(request);
        when(wikiContext.getDoc()).thenReturn(wikiDocument);
        when(wikiContext.getUserReference()).thenReturn(userDocRef);
        when(request.getContextPath()).thenReturn(CONTEXT_PATH);
        when(wikiAttachment.getFilename()).thenReturn(PDF_INTERNAL);
        when(wikiAttachment.getReference()).thenReturn(attachmentRef);
        when(wikiDocument.getAttachment(PDF_URL_EXTERNAL)).thenReturn(null);
        when(wikiDocument.getAttachment(PDF_INTERNAL)).thenReturn(wikiAttachment);
        when(wikiDocument.getDocumentReference()).thenReturn(docRef);
        when(wikiDocument.getAttachmentURL(PDF_INTERNAL, wikiContext)).thenReturn(PDF_INTERNAL_URL);
        when(xwiki.getDocument(docRef, wikiContext)).thenReturn(wikiDocument);
        when(sdoc.getAuthors()).thenReturn(documentAuthors);
        when(documentAuthors.getContentAuthor()).thenReturn(sdocUserRef);
        when(documentUserSerializer.serialize(sdocUserRef)).thenReturn(userSdocRef);
        when(sdoc.getDocumentReference()).thenReturn(docRef);
        when(authorizationManager.hasAccess(Right.VIEW, userDocRef, docRef)).thenReturn(true);

        when(entityReferenceResolver.resolve(OWNER_DOC_REF, EntityType.DOCUMENT)).thenReturn(docRef);
        when(entityReferenceResolver.resolve(PDF_INTERNAL, EntityType.ATTACHMENT, docRef)).thenReturn(attachmentRef);
        when(urlEntityReferenceResolver.resolve(PDF_URL_EXTERNAL, EntityType.ATTACHMENT)).thenReturn(attachmentRef);
        when(entityReferenceResolver.resolve(PDF_INTERNAL, EntityType.ATTACHMENT)).thenReturn(attachmentRef);
        when(tokenManager.getToken(userSdocRef, attachmentRef, docRef)).thenReturn(TOKEN_ID);
    }

    @Test
    void handleExternalURLExternalTest() throws XWikiException
    {
        AttachmentReference attachmentReference1 = new AttachmentReference(PDF_URL_EXTERNAL, docRef);
        when(urlEntityReferenceResolver.resolve(PDF_URL_EXTERNAL, EntityType.ATTACHMENT)).thenReturn(
            attachmentReference1);

        PDFFile pdfFile = pdfFileBuilder.handleExternalURL(PDF_URL_EXTERNAL, false);

        assertEquals(PDF_URL_EXTERNAL, pdfFile.getURL());
        assertTrue(pdfFile.getAttachmentReference().isPresent());
        assertTrue(pdfFile.hasViewRights());
        assertEquals(attachmentReference1, pdfFile.getAttachmentReference().get());
    }

    @Test
    void handleExternalURLInternalTest() throws XWikiException
    {
        PDFFileAuthorization fileAuthorization = new PDFFileAuthorization();
        fileAuthorization.setHasViewRights(true);
        when(pdfViewerAuthManager.hasViewRights(attachmentRef, false)).thenReturn(fileAuthorization);

        PDFFile pdfFile = pdfFileBuilder.handleExternalURL(PDF_URL_EXTERNAL, false);

        assertEquals(PDF_URL_EXTERNAL, pdfFile.getURL());
        assertTrue(pdfFile.getAttachmentReference().isPresent());
        assertTrue(pdfFile.hasViewRights());
        assertEquals(attachmentRef, pdfFile.getAttachmentReference().get());
    }

    @Test
    void handleExternalURLInternalDelegatedTest() throws XWikiException
    {
        PDFFileAuthorization fileAuthorization = new PDFFileAuthorization();
        fileAuthorization.setDelegatedViewRights(true);
        fileAuthorization.setHasViewRights(true);
        when(pdfViewerAuthManager.hasViewRights(attachmentRef, true)).thenReturn(fileAuthorization);

        PDFFile pdfFile = pdfFileBuilder.handleExternalURL(PDF_URL_EXTERNAL, true);

        assertEquals(EXPECTED_PATH, pdfFile.getURL());
        assertTrue(pdfFile.getAttachmentReference().isPresent());
        assertTrue(pdfFile.hasViewRights());
        assertTrue(pdfFile.areViewRightsDelegated());
        assertEquals(attachmentRef, pdfFile.getAttachmentReference().get());
    }

    @Test
    void handleExternalURLInternalNoRightsTest() throws XWikiException
    {
        PDFFileAuthorization fileAuthorization = new PDFFileAuthorization();
        when(pdfViewerAuthManager.hasViewRights(attachmentRef, false)).thenReturn(fileAuthorization);

        PDFFile pdfFile = pdfFileBuilder.handleExternalURL(PDF_URL_EXTERNAL, false);

        assertEquals("", pdfFile.getURL());
        assertTrue(pdfFile.getAttachmentReference().isPresent());
        assertFalse(pdfFile.hasViewRights());
        assertEquals(attachmentRef, pdfFile.getAttachmentReference().get());

        when(wikiDocument.getAttachment(PDF_INTERNAL)).thenReturn(null);

        pdfFile = pdfFileBuilder.handleExternalURL(PDF_URL_EXTERNAL, false);
        assertEquals("", pdfFile.getURL());
        assertFalse(pdfFile.hasViewRights());
    }

    @Test
    void handleInternalAttachmentNoOwnerDocTestFound() throws XWikiException
    {
        PDFFile pdfFile = pdfFileBuilder.handleInternalAttachment(PDF_INTERNAL, false, "");

        assertEquals(PDF_INTERNAL_URL, pdfFile.getURL());
        assertTrue(pdfFile.getAttachmentReference().isPresent());
        assertTrue(pdfFile.hasViewRights());
        assertEquals(attachmentRef, pdfFile.getAttachmentReference().get());
    }

    @Test
    void handleInternalAttachmentTest() throws XWikiException
    {

        when(wikiAttachment.getReference()).thenReturn(attachmentRef);
        PDFFileAuthorization fileAuthorization = new PDFFileAuthorization();
        fileAuthorization.setHasViewRights(true);
        fileAuthorization.setDelegatedViewRights(true);
        when(pdfViewerAuthManager.hasViewRights(attachmentRef, true)).thenReturn(fileAuthorization);
        when(xwiki.getDocument(docRef, wikiContext)).thenReturn(wikiDocument);

        PDFFile pdfFile = pdfFileBuilder.handleInternalAttachment(PDF_INTERNAL, true, OWNER_DOC_REF);

        assertEquals(EXPECTED_PATH, pdfFile.getURL());
        assertTrue(pdfFile.getAttachmentReference().isPresent());
        assertTrue(pdfFile.hasViewRights());
        assertEquals(attachmentRef, pdfFile.getAttachmentReference().get());
    }

    @Test
    void handleInternalAttachmentNoOwnerDocTest() throws XWikiException
    {
        when(wikiDocument.getAttachment(PDF_INTERNAL)).thenReturn(null);
        when(xwiki.getDocument(docRef, wikiContext)).thenReturn(wikiDocument2);
        when(wikiDocument2.getAttachment(PDF_INTERNAL)).thenReturn(wikiAttachment);
        when(wikiDocument2.getAttachmentURL(PDF_INTERNAL, wikiContext)).thenReturn(PDF_INTERNAL_URL);
        PDFFileAuthorization fileAuthorization = new PDFFileAuthorization();
        fileAuthorization.setHasViewRights(true);
        when(pdfViewerAuthManager.hasViewRights(attachmentRef, false)).thenReturn(fileAuthorization);
        PDFFile pdfFile = pdfFileBuilder.handleInternalAttachment(PDF_INTERNAL, false, "");

        assertEquals(PDF_INTERNAL_URL, pdfFile.getURL());
        assertTrue(pdfFile.getAttachmentReference().isPresent());
        assertTrue(pdfFile.hasViewRights());
        assertEquals(attachmentRef, pdfFile.getAttachmentReference().get());
    }

    @Test
    void handleInternalAttachmentNoOwnerDocDelegatedTest() throws XWikiException
    {
        String pdfFileReference = "pdfFileReference";
        DocumentReference docRef2 = new DocumentReference("wiki2", "space2", "page2");
        AttachmentReference attachmentRef2 = new AttachmentReference(PDF_INTERNAL, docRef2);
        when(entityReferenceResolver.resolve(pdfFileReference, EntityType.ATTACHMENT, docRef)).thenReturn(
            attachmentRef);
        when(authorizationManager.hasAccess(Right.VIEW, userDocRef, docRef)).thenReturn(false);
        when(entityReferenceResolver.resolve(pdfFileReference, EntityType.ATTACHMENT)).thenReturn(attachmentRef2);
        when(wikiDocument.getAttachment(pdfFileReference)).thenReturn(null);

        when(xwiki.getDocument(docRef2, wikiContext)).thenReturn(wikiDocument2);
        when(wikiDocument2.getAttachment(PDF_INTERNAL)).thenReturn(wikiAttachment);
        PDFFileAuthorization fileAuthorization = new PDFFileAuthorization();
        fileAuthorization.setDelegatedViewRights(true);
        when(pdfViewerAuthManager.hasViewRights(attachmentRef, true)).thenReturn(fileAuthorization);

        PDFFileAuthorization fileAuthorization2 = new PDFFileAuthorization();
        fileAuthorization2.setHasViewRights(true);
        fileAuthorization2.setDelegatedViewRights(true);
        when(pdfViewerAuthManager.hasViewRights(attachmentRef2, true)).thenReturn(fileAuthorization2);

        when(tokenManager.getToken(userSdocRef, attachmentRef2, docRef)).thenReturn(TOKEN_ID);

        PDFFile pdfFile = pdfFileBuilder.handleInternalAttachment(pdfFileReference, true, OWNER_DOC_REF);

        assertEquals(EXPECTED_PATH, pdfFile.getURL());
        assertTrue(pdfFile.getAttachmentReference().isPresent());
        assertTrue(pdfFile.hasViewRights());
        assertEquals(attachmentRef2, pdfFile.getAttachmentReference().get());
    }
}

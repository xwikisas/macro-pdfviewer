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
package com.xwiki.pdfviewer.internal.macro.authorization;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
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
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link PDFViewerAuthorizationManager}
 *
 * @version $Id$
 */
@ComponentTest
class PDFViewerAuthorizationManagerTest
{
    @InjectMockComponents
    private PDFViewerAuthorizationManager pdfAuthorizationManager;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentUserSerializer;

    @Mock
    private XWikiContext wikiContext;

    @Mock
    private XWiki wiki;

    @Mock
    private AttachmentReference attachmentReference;

    @Mock
    private DocumentReference documentReference;

    @Mock
    private DocumentReference userRef;

    @Mock
    private UserReference sdocUserRef;

    @Mock
    private DocumentReference sdocUserDocRef;

    @Mock
    private XWikiDocument sdoc;

    @Mock
    private DocumentAuthors documentAuthors;

    @BeforeEach
    void setup() throws XWikiException
    {
        when(contextProvider.get()).thenReturn(wikiContext);
        when(wikiContext.getWiki()).thenReturn(wiki);
        when(wikiContext.getUserReference()).thenReturn(userRef);
        when(wiki.exists(documentReference, wikiContext)).thenReturn(true);
        when(wikiContext.get(XWikiDocument.CKEY_SDOC)).thenReturn(sdoc);
        when(sdoc.getAuthors()).thenReturn(documentAuthors);
        when(documentAuthors.getContentAuthor()).thenReturn(sdocUserRef);
        when(documentUserSerializer.serialize(sdocUserRef)).thenReturn(sdocUserDocRef);
        when(attachmentReference.getDocumentReference()).thenReturn(documentReference);
        when(authorizationManager.hasAccess(Right.VIEW, userRef, documentReference)).thenReturn(false);
    }

    @Test
    void hasViewRightsInvalidRef() throws XWikiException
    {
        when(attachmentReference.getDocumentReference()).thenReturn(null);
        PDFFileAuthorization fileAuthorization = pdfAuthorizationManager.hasViewRights(attachmentReference, false);

        assertFalse(fileAuthorization.hasViewRights());
        assertFalse(fileAuthorization.areViewRightsDelegated());
    }

    @Test
    void hasViewRights() throws XWikiException
    {
        when(authorizationManager.hasAccess(Right.VIEW, userRef, documentReference)).thenReturn(true);

        PDFFileAuthorization fileAuthorization = pdfAuthorizationManager.hasViewRights(attachmentReference, false);
        assertTrue(fileAuthorization.hasViewRights());
        assertFalse(fileAuthorization.areViewRightsDelegated());
    }

    @Test
    void hasViewRightsNoRightNotDelegated() throws XWikiException
    {

        PDFFileAuthorization fileAuthorization = pdfAuthorizationManager.hasViewRights(attachmentReference, false);
        assertFalse(fileAuthorization.hasViewRights());
        assertFalse(fileAuthorization.areViewRightsDelegated());
    }

    @Test
    void hasViewRightsNoRightDelegated() throws XWikiException
    {
        when(authorizationManager.hasAccess(Right.VIEW, sdocUserDocRef, documentReference)).thenReturn(false);

        PDFFileAuthorization fileAuthorization = pdfAuthorizationManager.hasViewRights(attachmentReference, true);
        assertFalse(fileAuthorization.hasViewRights());
        assertTrue(fileAuthorization.areViewRightsDelegated());
    }

    @Test
    void hasViewRightsDelegated() throws XWikiException
    {
        when(authorizationManager.hasAccess(Right.VIEW, sdocUserDocRef, documentReference)).thenReturn(true);

        PDFFileAuthorization fileAuthorization = pdfAuthorizationManager.hasViewRights(attachmentReference, true);
        assertTrue(fileAuthorization.hasViewRights());
        assertTrue(fileAuthorization.areViewRightsDelegated());
    }
}

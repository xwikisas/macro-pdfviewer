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
package com.xwiki.pdfviewer.internal.token;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DelegatedTokenManager}
 *
 * @version $Id$
 */
@ComponentTest
class DelegatedTokenManagerTest
{
    private static final String ATTACH_NAME = "attach name";

    @InjectMockComponents
    private DelegatedTokenManager tokenManager;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.DEBUG);

    @Mock
    private DocumentReference userRef;

    @Mock
    private DocumentReference userRef2;

    @Mock
    private AttachmentReference attachmentReference;

    @Mock
    private AttachmentReference attachmentReference2;

    @Mock
    private DocumentReference documentReference;

    @Mock
    private DocumentReference documentReference2;

    @Mock
    private DocumentReference macroOrigin;

    @Mock
    private DocumentReference macroOrigin2;

    @BeforeEach
    void setup()
    {
        when(attachmentReference.getDocumentReference()).thenReturn(documentReference);
        when(attachmentReference2.getDocumentReference()).thenReturn(documentReference2);
        when(authorizationManager.hasAccess(Right.VIEW, userRef, documentReference2)).thenReturn(true);
        when(authorizationManager.hasAccess(Right.VIEW, userRef2, documentReference2)).thenReturn(true);
    }

    @Test
    void getTokenCreate()
    {
        createToken(userRef, attachmentReference, macroOrigin, true);
        assertEquals("New token created for file [attachmentReference] on origin [macroOrigin] and user [userRef].",
            logCapture.getMessage(0));
    }

    @Test
    void getTokenNoAccess()
    {
        createToken(userRef, attachmentReference, macroOrigin, false);
        assertEquals("Failed to create token for file [attachmentReference] on origin [macroOrigin] and user "
            + "[userRef] due to insufficient rights.", logCapture.getMessage(0));
    }

    @Test
    void getTokenExists()
    {
        String token = createToken(userRef, attachmentReference, macroOrigin, true);
        assertEquals("New token created for file [attachmentReference] on origin [macroOrigin] and user [userRef].",
            logCapture.getMessage(0));

        String token2 = tokenManager.getToken(userRef, attachmentReference, macroOrigin);
        assertEquals(token, token2);
    }

    @Test
    void getTokenExistsDifferentAuthor()
    {
        createToken(userRef, attachmentReference, macroOrigin, true);
        assertEquals("New token created for file [attachmentReference] on origin [macroOrigin] and user [userRef].",
            logCapture.getMessage(0));

        createToken(userRef2, attachmentReference, macroOrigin, true);
        assertEquals("Found token author [userRef] is different from the given author [userRef2]. Removing token and "
            + "attempting to create a new one.", logCapture.getMessage(1));
        assertEquals("New token created for file [attachmentReference] on origin [macroOrigin] and user [userRef2].",
            logCapture.getMessage(2));
    }

    @Test
    void getTokenExistsNoRights()
    {
        createToken(userRef, attachmentReference, macroOrigin, true);
        assertEquals("New token created for file [attachmentReference] on origin [macroOrigin] and user [userRef].",
            logCapture.getMessage(0));

        createToken(userRef, attachmentReference, macroOrigin, false);
        assertEquals("Deleted delegated token for file [attachmentReference] from macro origin document [macroOrigin].",
            logCapture.getMessage(1));
    }

    @Test
    void clearToken()
    {
        createToken(userRef, attachmentReference, macroOrigin, true);
        assertEquals("New token created for file [attachmentReference] on origin [macroOrigin] and user [userRef].",
            logCapture.getMessage(0));
        tokenManager.clearToken(attachmentReference, macroOrigin);
        assertEquals("Deleted delegated token for file [attachmentReference] from macro origin document [macroOrigin].",
            logCapture.getMessage(1));
    }

    @Test
    void clearAttachmentTokens()
    {
        DocumentReference docRef = new DocumentReference("wiki", "space", "page");
        AttachmentReference attachRef = new AttachmentReference(ATTACH_NAME, docRef);

        DocumentReference docRef2 = new DocumentReference("wiki2", "space2", "page2");
        AttachmentReference attachRef2 = new AttachmentReference(ATTACH_NAME, docRef2);

        when(authorizationManager.hasAccess(any(), any(), any(DocumentReference.class))).thenReturn(true);
        String token1 = createToken(userRef, attachRef, macroOrigin, true);
        String token2 = createToken(userRef2, attachRef, macroOrigin2, true);
        String token3 = createToken(userRef2, attachRef2, macroOrigin2, true);
        assertEquals(
            "New token created for file [Attachment wiki:space.page@attach name] on origin [macroOrigin] and user [userRef].",
            logCapture.getMessage(0));
        assertEquals(
            "New token created for file [Attachment wiki:space.page@attach name] on origin [macroOrigin2] and user [userRef2].",
            logCapture.getMessage(1));
        assertEquals(
            "New token created for file [Attachment wiki2:space2.page2@attach name] on origin [macroOrigin2] and user "
                + "[userRef2].", logCapture.getMessage(2));

        tokenManager.clearAttachmentTokens(ATTACH_NAME, docRef);
        String lastLogs = logCapture.getMessage(3) + logCapture.getMessage(4);
        assertTrue(lastLogs.contains(token1) && lastLogs.contains(token2));
        assertFalse(tokenManager.isInvalid(token3));
    }

    @Test
    void getTokenAttachmentReferenceTest()
    {
        when(contextualAuthorizationManager.hasAccess(Right.VIEW, macroOrigin)).thenReturn(true);
        String token = createToken(userRef2, attachmentReference2, macroOrigin, true);
        createToken(userRef, attachmentReference, macroOrigin, true);
        createToken(userRef2, attachmentReference, macroOrigin2, true);
        createToken(userRef, attachmentReference2, macroOrigin2, true);

        assertEquals("New token created for file [attachmentReference2] on origin [macroOrigin] and user [userRef2].",
            logCapture.getMessage(0));
        assertEquals("New token created for file [attachmentReference] on origin [macroOrigin] and user [userRef].",
            logCapture.getMessage(1));
        assertEquals("New token created for file [attachmentReference] on origin [macroOrigin2] and user [userRef2].",
            logCapture.getMessage(2));
        assertEquals("New token created for file [attachmentReference2] on origin [macroOrigin2] and user [userRef].",
            logCapture.getMessage(3));

        assertEquals(attachmentReference2, tokenManager.getTokenAttachmentReference(token));
    }

    @Test
    void hasAccessTest()
    {
        when(contextualAuthorizationManager.hasAccess(Right.VIEW, macroOrigin)).thenReturn(false);
        String token = createToken(userRef2, attachmentReference2, macroOrigin, true);
        createToken(userRef, attachmentReference, macroOrigin, true);
        createToken(userRef2, attachmentReference, macroOrigin2, true);
        createToken(userRef, attachmentReference2, macroOrigin2, true);

        assertEquals("New token created for file [attachmentReference2] on origin [macroOrigin] and user [userRef2].",
            logCapture.getMessage(0));
        assertEquals("New token created for file [attachmentReference] on origin [macroOrigin] and user [userRef].",
            logCapture.getMessage(1));
        assertEquals("New token created for file [attachmentReference] on origin [macroOrigin2] and user [userRef2].",
            logCapture.getMessage(2));
        assertEquals("New token created for file [attachmentReference2] on origin [macroOrigin2] and user [userRef].",
            logCapture.getMessage(3));
        assertFalse(tokenManager.hasAccess(token));
    }

    private String createToken(DocumentReference userRef, AttachmentReference attachmentReference,
        DocumentReference macroOrigin, boolean hasView)
    {
        when(authorizationManager.hasAccess(Right.VIEW, userRef, documentReference)).thenReturn(hasView);
        String token = tokenManager.getToken(userRef, attachmentReference, macroOrigin);
        if (hasView) {
            assertTrue(token.contains(String.format("wopi_%s_%s_%s_", userRef, attachmentReference, macroOrigin)));
        } else {
            assertEquals("", token);
        }
        return token;
    }
}

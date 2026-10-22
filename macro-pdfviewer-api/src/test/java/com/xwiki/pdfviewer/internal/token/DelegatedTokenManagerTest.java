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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.util.ReflectionUtils;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DelegatedTokenManager}.
 *
 * @version $Id$
 */
@ComponentTest
class DelegatedTokenManagerTest
{
    private static final String ATTACH_NAME = "file.pdf";

    private static final Instant NOW = Instant.parse("2026-10-22T10:00:00Z");

    private final DocumentReference author = new DocumentReference("wiki", "XWiki", "Author");

    private final DocumentReference otherAuthor = new DocumentReference("wiki", "XWiki", "OtherAuthor");

    private final DocumentReference attachmentDoc = new DocumentReference("wiki", "Space", "Attachments");

    private final DocumentReference otherAttachmentDoc = new DocumentReference("wiki2", "Space", "Attachments");

    private final AttachmentReference attachment = new AttachmentReference(ATTACH_NAME, attachmentDoc);

    private final AttachmentReference otherAttachment = new AttachmentReference("other.pdf", attachmentDoc);

    private final AttachmentReference sameNameAttachment = new AttachmentReference(ATTACH_NAME, otherAttachmentDoc);

    private final DocumentReference macroOrigin = new DocumentReference("wiki", "Space", "MacroOrigin");

    private final DocumentReference otherMacroOrigin = new DocumentReference("wiki", "Space", "OtherMacroOrigin");

    @InjectMockComponents
    private DelegatedTokenManager tokenManager;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeEach
    void setup()
    {
        setNow(NOW);
        setAuthorViewRight(this.author, this.attachmentDoc, true);
        setAuthorViewRight(this.otherAuthor, this.attachmentDoc, true);
        setAuthorViewRight(this.author, this.otherAttachmentDoc, true);
        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, this.macroOrigin)).thenReturn(true);
        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, this.otherMacroOrigin)).thenReturn(true);
    }

    @Test
    void getTokenCreatesValidToken()
    {
        String token = this.tokenManager.getToken(this.author, this.attachment, this.macroOrigin);

        assertFalse(token.isEmpty());
        assertFalse(this.tokenManager.isInvalid(token));
        assertTrue(this.tokenManager.hasAccess(token));
        assertEquals(this.attachment, this.tokenManager.getTokenAttachmentReference(token));
    }

    @Test
    void getTokenWithoutAuthorViewRight()
    {
        setAuthorViewRight(this.author, this.attachmentDoc, false);

        assertEquals("", this.tokenManager.getToken(this.author, this.attachment, this.macroOrigin));
        assertEquals("Failed to create token for file [Attachment wiki:Space.Attachments@file.pdf] on origin "
            + "[wiki:Space.MacroOrigin] and user [wiki:XWiki.Author] due to insufficient rights.",
            this.logCapture.getMessage(0));
    }

    @Test
    void getTokenReusesExistingToken()
    {
        String token = this.tokenManager.getToken(this.author, this.attachment, this.macroOrigin);

        assertEquals(token, this.tokenManager.getToken(this.author, this.attachment, this.macroOrigin));
    }

    @Test
    void getTokenIsBoundToTheMacroOrigin()
    {
        String token = this.tokenManager.getToken(this.author, this.attachment, this.macroOrigin);
        String otherToken = this.tokenManager.getToken(this.author, this.attachment, this.otherMacroOrigin);

        assertNotEquals(token, otherToken);
        assertFalse(this.tokenManager.isInvalid(token));
        assertFalse(this.tokenManager.isInvalid(otherToken));
    }

    @Test
    void getTokenWithDifferentAuthorReplacesToken()
    {
        String token = this.tokenManager.getToken(this.author, this.attachment, this.macroOrigin);

        String newToken = this.tokenManager.getToken(this.otherAuthor, this.attachment, this.macroOrigin);

        assertNotEquals(token, newToken);
        assertTrue(this.tokenManager.isInvalid(token));
        assertFalse(this.tokenManager.isInvalid(newToken));
    }

    @Test
    void getTokenAfterAuthorLostViewRight()
    {
        String token = this.tokenManager.getToken(this.author, this.attachment, this.macroOrigin);
        setAuthorViewRight(this.author, this.attachmentDoc, false);

        assertEquals("", this.tokenManager.getToken(this.author, this.attachment, this.macroOrigin));
        assertTrue(this.tokenManager.isInvalid(token));
    }

    @Test
    void isInvalidWithUnknownToken()
    {
        assertTrue(this.tokenManager.isInvalid("unknown"));
        assertFalse(this.tokenManager.hasAccess("unknown"));
        assertNull(this.tokenManager.getTokenAttachmentReference("unknown"));
    }

    @Test
    void hasAccessWithoutViewRightOnMacroOrigin()
    {
        String token = this.tokenManager.getToken(this.author, this.attachment, this.macroOrigin);
        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, this.macroOrigin)).thenReturn(false);

        assertFalse(this.tokenManager.hasAccess(token));
        assertNull(this.tokenManager.getTokenAttachmentReference(token));
        // The token belongs to the author, so another viewer of the macro origin can still use it.
        assertFalse(this.tokenManager.isInvalid(token));
    }

    @Test
    void hasAccessAfterAuthorLostViewRight()
    {
        String token = this.tokenManager.getToken(this.author, this.attachment, this.macroOrigin);
        setAuthorViewRight(this.author, this.attachmentDoc, false);

        assertFalse(this.tokenManager.hasAccess(token));
        assertTrue(this.tokenManager.isInvalid(token));
    }

    @Test
    void clearToken()
    {
        String token = this.tokenManager.getToken(this.author, this.attachment, this.macroOrigin);
        String otherOriginToken = this.tokenManager.getToken(this.author, this.attachment, this.otherMacroOrigin);

        this.tokenManager.clearToken(this.attachment, this.macroOrigin);

        assertTrue(this.tokenManager.isInvalid(token));
        assertFalse(this.tokenManager.isInvalid(otherOriginToken));
    }

    @Test
    void clearAttachmentTokens()
    {
        String token = this.tokenManager.getToken(this.author, this.attachment, this.macroOrigin);
        String otherOriginToken = this.tokenManager.getToken(this.otherAuthor, this.attachment, this.otherMacroOrigin);
        String otherAttachmentToken = this.tokenManager.getToken(this.author, this.otherAttachment, this.macroOrigin);
        String sameNameToken = this.tokenManager.getToken(this.author, this.sameNameAttachment, this.macroOrigin);

        this.tokenManager.clearAttachmentTokens(ATTACH_NAME, this.attachmentDoc);

        assertTrue(this.tokenManager.isInvalid(token));
        assertTrue(this.tokenManager.isInvalid(otherOriginToken));
        assertFalse(this.tokenManager.isInvalid(otherAttachmentToken));
        assertFalse(this.tokenManager.isInvalid(sameNameToken));
    }

    @Test
    void tokenExpires()
    {
        String token = this.tokenManager.getToken(this.author, this.attachment, this.macroOrigin);

        setNow(NOW.plus(Duration.ofHours(1)));

        assertFalse(this.tokenManager.hasAccess(token));
        assertTrue(this.tokenManager.isInvalid(token));
    }

    @Test
    void tokenRenewedWhenIssuedAgain()
    {
        String token = this.tokenManager.getToken(this.author, this.attachment, this.macroOrigin);

        setNow(NOW.plus(Duration.ofMinutes(50)));
        assertEquals(token, this.tokenManager.getToken(this.author, this.attachment, this.macroOrigin));

        setNow(NOW.plus(Duration.ofMinutes(100)));
        assertFalse(this.tokenManager.isInvalid(token));
    }

    @Test
    void expiredTokenReplaced()
    {
        String token = this.tokenManager.getToken(this.author, this.attachment, this.macroOrigin);

        setNow(NOW.plus(Duration.ofHours(2)));
        String newToken = this.tokenManager.getToken(this.author, this.attachment, this.macroOrigin);

        assertNotEquals(token, newToken);
        assertTrue(this.tokenManager.isInvalid(token));
        assertFalse(this.tokenManager.isInvalid(newToken));
    }

    @Test
    void expiredTokensPurgedWhenCreatingToken()
    {
        String token = this.tokenManager.getToken(this.author, this.attachment, this.macroOrigin);

        setNow(NOW.plus(Duration.ofHours(2)));
        this.tokenManager.getToken(this.author, this.otherAttachment, this.otherMacroOrigin);

        // The expired token is gone from the store, not only rejected.
        setNow(NOW);
        assertTrue(this.tokenManager.isInvalid(token));
    }

    @Test
    void clearOriginTokens()
    {
        String token = this.tokenManager.getToken(this.author, this.attachment, this.macroOrigin);
        String otherAttachmentToken = this.tokenManager.getToken(this.author, this.otherAttachment, this.macroOrigin);
        String otherOriginToken = this.tokenManager.getToken(this.author, this.attachment, this.otherMacroOrigin);

        this.tokenManager.clearOriginTokens(this.macroOrigin);

        assertTrue(this.tokenManager.isInvalid(token));
        assertTrue(this.tokenManager.isInvalid(otherAttachmentToken));
        assertFalse(this.tokenManager.isInvalid(otherOriginToken));
    }

    @Test
    void tokenHasRandomValue()
    {
        String token = this.tokenManager.getToken(this.author, this.attachment, this.macroOrigin);
        this.tokenManager.clearToken(this.attachment, this.macroOrigin);
        String token2 = this.tokenManager.getToken(this.author, this.attachment, this.macroOrigin);

        String prefix = String.format("wopi_%s_%s_%s_", this.author, this.attachment, this.macroOrigin);
        String randomValue = token.substring(prefix.length());
        // 128 random bits, URL-safe Base64 encoded without padding.
        assertEquals(22, randomValue.length());
        assertNotEquals(randomValue, token2.substring(prefix.length()));
    }

    private void setNow(Instant now)
    {
        ReflectionUtils.setFieldValue(this.tokenManager, "clock", Clock.fixed(now, ZoneOffset.UTC));
    }

    private void setAuthorViewRight(DocumentReference user, DocumentReference document, boolean allowed)
    {
        when(this.authorizationManager.hasAccess(Right.VIEW, user, document)).thenReturn(allowed);
    }
}

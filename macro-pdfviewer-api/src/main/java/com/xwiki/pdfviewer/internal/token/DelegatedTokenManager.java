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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Manages the token actions for PDF viewer macro.
 *
 * @version $Id$
 * @since 2.7
 */
@Component(roles = DelegatedTokenManager.class)
@Singleton
public class DelegatedTokenManager
{
    private static final String TOKEN_DELETE_FORMAT =
        "Deleted delegated token for file [{}] from macro origin document [{}].";

    private final Map<String, DelegatedToken> tokens = new HashMap<>();

    @Inject
    private Logger logger;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    /**
     * Get {@link DelegatedToken} corresponding to the given user, {@link AttachmentReference} and macro origin
     * {@link DocumentReference}, or create a new token in case it does not exist or there is a mismatch between authors
     * expired.
     *
     * @param userReference current user reference
     * @param fileId {@link AttachmentReference} of the target attachment
     * @param macroOrigin origin of the PDF macro
     * @return existing {@link DelegatedToken}, or a new one
     */
    public String getToken(DocumentReference userReference, AttachmentReference fileId, DocumentReference macroOrigin)
    {
        DelegatedToken token = getExistingToken(fileId, macroOrigin);
        if (token != null) {
            // If the token author is different from the current one, or if the author lacks the rights to view the
            // attachment, we delete the token.
            if (!userReference.equals(token.getUser())) {
                logger.debug("Found token author [{}] is different from the given author [{}]. Removing token and "
                    + "attempting to create a new one.", token.getUser(), userReference);
                tokens.remove(token.toString());
            } else if (!checkAuthorViewRights(token)) {
                return "";
            } else {
                return token.toString();
            }
        }

        return createNewToken(userReference, fileId, macroOrigin);
    }

    /**
     * Check if the given token exists.
     *
     * @param token {@code String} representation of a token
     * @return {@code false} if the token is valid, {@code true} otherwise
     */
    public boolean isInvalid(String token)
    {
        DelegatedToken foundToken = tokens.get(token);
        return foundToken == null;
    }

    /**
     * Remove the token.
     *
     * @param fileId {@link AttachmentReference} of the target attachment
     * @param macroOrigin {@link DocumentReference} of the macro origin document
     */
    public void clearToken(AttachmentReference fileId, DocumentReference macroOrigin)
    {
        DelegatedToken foundToken = getExistingToken(fileId, macroOrigin);
        if (foundToken != null) {
            tokens.remove(foundToken.toString());
            logger.debug(TOKEN_DELETE_FORMAT, fileId, macroOrigin);
        }
    }

    /**
     * Remove the token.
     *
     * @param token {@link DelegatedToken} to be removed.
     */
    public void clearToken(DelegatedToken token)
    {
        if (token != null) {
            tokens.remove(token.toString());
            logger.debug(TOKEN_DELETE_FORMAT, token.getFileReference(), token.getMacroOrigin());
        }
    }

    /**
     * Remove all tokens for a given attachment.
     *
     * @param fileName id of the edited file
     * @param attachmentDoc {@link DocumentReference} attachment parent document
     */
    public void clearAttachmentTokens(String fileName, DocumentReference attachmentDoc)
    {
        List<DelegatedToken> foundTokens = getAttachmentTokens(fileName, attachmentDoc);
        for (DelegatedToken token : foundTokens) {
            String tokenName = token.toString();
            tokens.remove(tokenName);
            logger.debug("Deleted delegated token [{}] for file [{}] from document [{}].", tokenName, fileName,
                attachmentDoc);
        }
    }

    /**
     * Remove all tokens for a given document.
     *
     * @param attachmentDoc {@link DocumentReference} target document
     */
    public void clearDocumentTokens(DocumentReference attachmentDoc)
    {
        List<DelegatedToken> foundTokens = getDocumentTokens(attachmentDoc);
        for (DelegatedToken token : foundTokens) {
            String tokenName = token.toString();
            tokens.remove(tokenName);
            logger.debug("Deleted delegated token [{}] for attachments from document [{}].", tokenName, attachmentDoc);
        }
    }

    /**
     * Get the {@link AttachmentReference} of the given token representation.
     *
     * @param tokenId token id
     * @return the {@link AttachmentReference} of the given token representation.
     */
    public AttachmentReference getTokenAttachmentReference(String tokenId)
    {
        DelegatedToken fileToken = tokens.get(tokenId);
        if (fileToken.getUser() == null) {
            return null;
        }
        return fileToken.getFileReference();
    }

    /**
     * @param token string representation of the token
     * @return {@code true} if the author still has view rights on the delegated attachment and if the context user has
     *     view rights on the macro origin document, or {@code false} otherwise.
     */
    public boolean hasAccess(String token)
    {
        DelegatedToken fileToken = tokens.getOrDefault(token, null);
        return fileToken != null && checkAuthorViewRights(fileToken) && checkUserViewRights(fileToken);
    }

    private boolean checkAuthorViewRights(DelegatedToken fileToken)
    {
        boolean hasViewRights = this.authorizationManager.hasAccess(Right.VIEW, fileToken.getUser(),
            fileToken.getFileReference().getDocumentReference());
        if (!hasViewRights) {
            clearToken(fileToken);
        }

        return hasViewRights;
    }

    private boolean checkUserViewRights(DelegatedToken fileToken)
    {
        return contextualAuthorizationManager.hasAccess(Right.VIEW, fileToken.getMacroOrigin());
    }

    private DelegatedToken getExistingToken(AttachmentReference fileId, DocumentReference macroOrigin)
    {
        Optional<Map.Entry<String, DelegatedToken>> tokenEntry = this.tokens.entrySet().stream().filter(
                x -> x.getValue().getFileReference().equals(fileId) && x.getValue().getMacroOrigin().equals(macroOrigin))
            .findFirst();

        return tokenEntry.map(Map.Entry::getValue).orElse(null);
    }

    private List<DelegatedToken> getAttachmentTokens(String fileId, DocumentReference fileDoc)
    {
        return this.tokens.values().stream().filter(
            x -> x.getFileReference().getName().equals(fileId) && x.getFileReference().getDocumentReference()
                .equals(fileDoc)).collect(Collectors.toList());
    }

    private List<DelegatedToken> getDocumentTokens(DocumentReference fileDoc)
    {
        return this.tokens.values().stream().filter(
                x -> x.getMacroOrigin().equals(fileDoc) || x.getFileReference().getDocumentReference().equals(fileDoc))
            .collect(Collectors.toList());
    }

    private String createNewToken(DocumentReference user, AttachmentReference fileId, DocumentReference macroOrigin)
    {
        String tokenId = "";
        boolean canView = this.authorizationManager.hasAccess(Right.VIEW, user, fileId.getDocumentReference());
        if (canView) {
            DelegatedToken token = new DelegatedToken(user, fileId, macroOrigin);
            tokens.put(token.toString(), token);
            logger.debug("New token created for file [{}] on origin [{}] and user [{}].", fileId, macroOrigin, user);
            tokenId = token.toString();
        } else {
            logger.warn("Failed to create token for file [{}] on origin [{}] and user [{}] due to insufficient rights.",
                fileId, macroOrigin, user);
        }
        return tokenId;
    }
}

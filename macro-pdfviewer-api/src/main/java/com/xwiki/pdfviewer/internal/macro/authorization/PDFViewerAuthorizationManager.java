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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.pdfviewer.internal.token.DelegatedTokenManager;

/**
 * Handles the evaluation of view permissions on the given attachment.
 *
 * @version $Id$
 * @since 2.7
 */
@Component(roles = PDFViewerAuthorizationManager.class)
@Singleton
public class PDFViewerAuthorizationManager
{
    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Inject
    private DelegatedTokenManager tokenManager;

    @Inject
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentUserSerializer;

    /**
     * Evaluates whether an attachment can be viewed, either through direct user permissions or delegated (author)
     * permissions.
     *
     * @param attachmentReference the attachment for which we check the rights
     * @param delegatedRights {@code true} if the view rights have been delegated by the author, or {@code false}
     *     otherwise
     * @return a {@link PDFFileAuthorization} describing whether the attachment can be viewed and if access was
     *     delegated
     * @throws XWikiException if an error occurs while attempting to check if the attachment parent document exists
     */
    public PDFFileAuthorization hasViewRights(AttachmentReference attachmentReference, boolean delegatedRights)
        throws XWikiException
    {
        XWikiContext wikiContext = wikiContextProvider.get();
        XWiki wiki = wikiContext.getWiki();
        DocumentReference documentReference = attachmentReference.getDocumentReference();
        PDFFileAuthorization fileAuthorization = new PDFFileAuthorization();
        if (documentReference == null || !wiki.exists(documentReference, wikiContext)) {
            return fileAuthorization;
        }

        if (hasViewAccess(wikiContext.getUserReference(), documentReference)) {
            fileAuthorization.setHasViewRights(true);
            return fileAuthorization;
        }

        XWikiDocument sdoc = (XWikiDocument) wikiContext.get(XWikiDocument.CKEY_SDOC);
        if (!delegatedRights) {
            tokenManager.clearToken(attachmentReference, sdoc.getDocumentReference());
            return fileAuthorization;
        }
        hasDelegatedViewRights(attachmentReference, sdoc, fileAuthorization);
        return fileAuthorization;
    }

    private void hasDelegatedViewRights(AttachmentReference attachmentReference, XWikiDocument sdoc,
        PDFFileAuthorization fileAuthorization)
    {
        DocumentReference author = documentUserSerializer.serialize(sdoc.getAuthors().getContentAuthor());
        // We check the view rights of the author.
        boolean hasViewRights = hasViewAccess(author, attachmentReference.getDocumentReference());
        fileAuthorization.setDelegatedViewRights(true);
        fileAuthorization.setHasViewRights(hasViewRights);
        if (!hasViewRights) {
            tokenManager.clearToken(attachmentReference, sdoc.getDocumentReference());
        }
    }

    private boolean hasViewAccess(DocumentReference user, DocumentReference target)
    {
        return authorizationManager.hasAccess(Right.VIEW, user, target);
    }
}

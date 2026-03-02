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
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.pdfviewer.internal.macro.authorization.PDFFileAuthorization;
import com.xwiki.pdfviewer.internal.macro.authorization.PDFViewerAuthorizationManager;
import com.xwiki.pdfviewer.internal.token.DelegatedTokenManager;
import com.xwiki.pdfviewer.macro.PDFFile;

/**
 * Builds {@link PDFFile} objects for PDF attachments by resolving references, while also validating view permissions.
 *
 * @version $Id$
 * @since 2.7
 */
@Component(roles = PDFFileBuilder.class)
@Singleton
public class PDFFileBuilder
{
    private static final String PDF_CONTENT_FORMAT = "%s/rest/pdfmacro/contents?access_token=%s";

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    @Named("resource/standardURL")
    private EntityReferenceResolver<String> urlEntityReferenceResolver;

    @Inject
    @Named("current")
    private EntityReferenceResolver<String> entityReferenceResolver;

    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Inject
    private DelegatedTokenManager tokenManager;

    @Inject
    private PDFViewerAuthorizationManager pdfViewerAuthManager;

    @Inject
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentUserSerializer;

    /**
     * Builds a {@link PDFFile} from a given URL. If the URL refers to an attachment within the current XWiki instance,
     * the view rights are processed. If the URL points outside the XWiki instance, the URL is used as it is and no view
     * rights checks are done.
     *
     * @param pdfURL the attachment URL or external PDF URL
     * @param delegatedRights {@code true} if the view rights have been delegated by the author, or {@code false}
     *     otherwise
     * @return a {@link PDFFile} containing the resolved attachment reference, access URL, and view rights
     * @throws XWikiException if an error occurs while resolving references or during the authorization check
     */
    public PDFFile handleExternalURL(String pdfURL, boolean delegatedRights) throws XWikiException
    {
        AttachmentReference attachmentReference =
            new AttachmentReference(urlEntityReferenceResolver.resolve(pdfURL, EntityType.ATTACHMENT));

        //  If the attachment reference name is the same as the given url, it means that the url directs to a file
        //  outside of XWiki instance and there is no need to check the user view right or delegated rights. We still
        //  send the attachment reference to be able to extract the name for it to be displayed in a tab in the case
        //  of multiple attachments.
        PDFFile pdfFile = new PDFFile();
        pdfFile.setAttachmentReference(attachmentReference);
        if (attachmentReference.getName().equals(pdfURL)) {
            pdfFile.setUrl(pdfURL);
            pdfFile.setHasViewRights(true);
        } else {
            PDFFileAuthorization fileAuth = pdfViewerAuthManager.hasViewRights(attachmentReference, delegatedRights);
            if (attachmentExists(attachmentReference) && fileAuth.hasViewRights()) {
                pdfFile.setHasViewRights(fileAuth.hasViewRights());
                pdfFile.setDelegatedViewRights(fileAuth.areViewRightsDelegated());
                if (pdfFile.areViewRightsDelegated()) {
                    pdfFile.setUrl(getTokenURL(attachmentReference));
                } else {
                    pdfFile.setUrl(pdfURL);
                }
            }
        }
        return pdfFile;
    }

    /**
     * Builds a {@link PDFFile} from an internal attachment reference. The method first attempts to resolve the
     * attachment relative to the current document or to the provided owner document (for backwards compatibility). If
     * the attachment is not found in those contexts, the {@code pdfFileReference} parameter is treated as a full
     * attachment reference.
     *
     * @param pdfFileReference the attachment name or full attachment reference
     * @param delegatedRights {@code true} if the view rights have been delegated by the author, or {@code false}
     *     otherwise
     * @param ownerDocumentReference reference of the document that contains the file
     * @return a {@link PDFFile} containing the resolved attachment reference, access URL, and view rights
     * @throws XWikiException if an error occurs while resolving the document, attachment, or during the
     *     authorization check
     */
    public PDFFile handleInternalAttachment(String pdfFileReference, boolean delegatedRights,
        String ownerDocumentReference) throws XWikiException
    {
        Optional<PDFFile> fileOptional =
            processFromOwnerDocument(ownerDocumentReference, pdfFileReference, delegatedRights);

        // If the attachment is not in the current or given document, the file parameter is used as a full reference.
        if (fileOptional.isEmpty()) {
            AttachmentReference attachmentReference =
                new AttachmentReference(entityReferenceResolver.resolve(pdfFileReference, EntityType.ATTACHMENT));
            return getPDFFile(attachmentReference, delegatedRights);
        }
        return fileOptional.get();
    }

    private PDFFile getPDFFile(AttachmentReference attachmentRef, boolean delegatedRights) throws XWikiException
    {
        PDFFile pdfFile = new PDFFile();
        PDFFileAuthorization fileAuthorization = pdfViewerAuthManager.hasViewRights(attachmentRef, delegatedRights);
        if (attachmentExists(attachmentRef) && fileAuthorization.hasViewRights()) {
            pdfFile.setHasViewRights(fileAuthorization.hasViewRights());
            pdfFile.setDelegatedViewRights(fileAuthorization.areViewRightsDelegated());
            pdfFile.setAttachmentReference(attachmentRef);
            pdfFile.setUrl(buildAttachmentURL(attachmentRef, pdfFile));
        }
        return pdfFile;
    }

    private Optional<PDFFile> processFromOwnerDocument(String ownerDocumentReference, String pdfFileReference,
        boolean delegatedRights) throws XWikiException
    {
        // For backwards compatibility with the macros added in older versions, first it is checked if the file name
        // specified exists on the defined document or on the current one before considering the file parameter as the
        // full reference.
        Optional<PDFFile> optionalPDFFile =
            handleGivenDocument(ownerDocumentReference, pdfFileReference, delegatedRights);
        if (optionalPDFFile.isEmpty()) {
            DocumentReference docRef = wikiContextProvider.get().getDoc().getDocumentReference();
            AttachmentReference localAttachRef = new AttachmentReference(
                this.entityReferenceResolver.resolve(pdfFileReference, EntityType.ATTACHMENT, docRef));
            if (attachmentExists(localAttachRef)) {
                optionalPDFFile = Optional.of(generatePDFFile(delegatedRights, localAttachRef));
            }
        }
        return optionalPDFFile;
    }

    private Optional<PDFFile> handleGivenDocument(String ownerDocumentReference, String pdfFileReference,
        boolean delegatedRights) throws XWikiException
    {
        PDFFile pdfFile = null;
        if (ownerDocumentReference != null && !ownerDocumentReference.isEmpty()) {
            DocumentReference givenDocumentReference =
                new DocumentReference(entityReferenceResolver.resolve(ownerDocumentReference, EntityType.DOCUMENT));
            AttachmentReference attachRef = new AttachmentReference(
                this.entityReferenceResolver.resolve(pdfFileReference, EntityType.ATTACHMENT, givenDocumentReference));

            if (attachmentExists(attachRef)) {
                pdfFile = generatePDFFile(delegatedRights, attachRef);
            }
        }
        return Optional.ofNullable(pdfFile);
    }

    private PDFFile generatePDFFile(boolean delegatedRights, AttachmentReference attachRef) throws XWikiException
    {
        XWikiContext wikiContext = wikiContextProvider.get();
        PDFFile pdfFile = new PDFFile();
        PDFFileAuthorization fileAuthorization = pdfViewerAuthManager.hasViewRights(attachRef, delegatedRights);
        pdfFile.setAttachmentReference(attachRef);
        pdfFile.setHasViewRights(fileAuthorization.hasViewRights());
        pdfFile.setDelegatedViewRights(fileAuthorization.areViewRightsDelegated());
        // To view the attachment, the user needs to have view rights, no matter if they are delegated (author's
        // view rights) or the user itself has access.
        if (pdfFile.areViewRightsDelegated() && pdfFile.hasViewRights()) {
            pdfFile.setUrl(getTokenURL(attachRef));
        } else if (pdfFile.hasViewRights()) {
            XWikiDocument attachmentDocument =
                wikiContext.getWiki().getDocument(attachRef.getDocumentReference(), wikiContext);
            String url = attachmentDocument.getAttachmentURL(attachRef.getName(), wikiContext);
            pdfFile.setUrl(url);
        }
        return pdfFile;
    }

    private boolean attachmentExists(AttachmentReference attachmentReference) throws XWikiException
    {
        XWikiContext wikiContext = wikiContextProvider.get();
        XWiki wiki = wikiContext.getWiki();
        XWikiDocument wikiDocument = wiki.getDocument(attachmentReference.getDocumentReference(), wikiContext);
        return wikiDocument.getAttachment(attachmentReference.getName()) != null;
    }

    private String buildAttachmentURL(AttachmentReference attachmentReference, PDFFile pdfFile) throws XWikiException
    {
        if (pdfFile.areViewRightsDelegated()) {
            return getTokenURL(attachmentReference);
        } else {
            XWikiContext wikiContext = this.wikiContextProvider.get();
            DocumentReference parentDocRef = new DocumentReference(attachmentReference.getParent());
            XWikiDocument attachmentDocument = wikiContext.getWiki().getDocument(parentDocRef, wikiContext);
            return attachmentDocument.getAttachmentURL(attachmentReference.getName(), wikiContext);
        }
    }

    private String getTokenURL(AttachmentReference attachmentReference)
    {
        XWikiContext wikiContext = wikiContextProvider.get();
        XWikiDocument sdoc = (XWikiDocument) wikiContext.get(XWikiDocument.CKEY_SDOC);
        DocumentReference currentAuthor = documentUserSerializer.serialize(sdoc.getAuthors().getContentAuthor());
        String tokenId = tokenManager.getToken(currentAuthor, attachmentReference, sdoc.getDocumentReference());
        String encodedToken = Base64.getUrlEncoder().encodeToString(tokenId.getBytes());
        return String.format(PDF_CONTENT_FORMAT, wikiContext.getRequest().getContextPath(), encodedToken);
    }
}

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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.AttachmentDeletedEvent;
import com.xwiki.pdfviewer.internal.token.DelegatedTokenManager;

/**
 * Listens to attachment delete events and attempts to remove the existing token, if any.
 *
 * @version $Id$
 * @since 2.7
 */
@Component
@Named(PDFViewerListener.HINT)
@Singleton
public class PDFViewerListener extends AbstractEventListener
{
    /**
     * The hint for the component.
     */
    public static final String HINT = "PDFViewerListener";

    @Inject
    protected Logger logger;

    @Inject
    private DelegatedTokenManager delegatedTokenManager;

    /**
     * Creates an event-listener filtering for AttachmentDeletedEvent.
     */
    public PDFViewerListener()
    {
        super(HINT, new AttachmentDeletedEvent(), new DocumentDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        try {
            if (event instanceof AttachmentDeletedEvent) {
                String attachmentName = ((AttachmentDeletedEvent) event).getName();
                delegatedTokenManager.clearAttachmentTokens(attachmentName,
                    ((XWikiDocument) source).getDocumentReference());
                logger.debug("Successfully removed all tokens granted for [{}]", attachmentName);
            } else if (event instanceof DocumentDeletedEvent) {
                XWikiDocument document = (XWikiDocument) source;
                if (document != null) {
                    delegatedTokenManager.clearDocumentTokens(document.getDocumentReference());
                }
            }
        } catch (Exception e) {
            logger.error("An error occurred while removing PDF Viewer access tokens. Root cause is: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            throw new RuntimeException(e);
        }
    }
}

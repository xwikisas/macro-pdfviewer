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

import java.lang.reflect.Type;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.properties.converter.AbstractConverter;
import org.xwiki.rendering.listener.reference.ResourceType;

import com.xwiki.pdfviewer.PDFResourceReference;

/**
 * Converts {@link String} into {@link PDFResourceReference} objects.
 * TODO: This is needed to avoid https://jira.xwiki.org/browse/XWIKI-17768 and should be removed after
 * the application starts depending on a version of XWiki >= the version where it's fixed.
 * 
 * @version $Id$
 * @since 2.3
 */
@Component
@Singleton
public class PDFResourceReferenceConverter extends AbstractConverter<PDFResourceReference>
{
    @SuppressWarnings("unchecked")
    @Override
    protected PDFResourceReference convertToType(Type type, Object value)
    {
        if (value == null) {
            return null;
        }

        return new PDFResourceReference(value.toString(), ResourceType.ATTACHMENT);
    }

}

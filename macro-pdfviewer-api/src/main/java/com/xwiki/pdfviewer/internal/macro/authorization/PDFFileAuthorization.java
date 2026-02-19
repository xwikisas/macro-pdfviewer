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

import com.xwiki.pdfviewer.macro.PDFFile;

/**
 * Stores information regarding the view rights on a {@link PDFFile}.
 *
 * @version $Id$
 * @since 2.7
 */
public class PDFFileAuthorization
{
    private boolean delegatedViewRights;

    private boolean hasViewRights;

    /**
     * Default constructor.
     */
    public PDFFileAuthorization()
    {
        hasViewRights = false;
        delegatedViewRights = false;
    }

    /**
     * See {@link #hasViewRights()}.
     *
     * @param hasViewRights {@code true} if the view rights are given, or {@code false} otherwise
     */
    public void setHasViewRights(boolean hasViewRights)
    {
        this.hasViewRights = hasViewRights;
    }

    /**
     * Check if the view rights are given.
     *
     * @return {@code true} if the view rights are given, or {@code false} otherwise
     */
    public boolean hasViewRights()
    {
        return hasViewRights;
    }

    /**
     * Return the delegated view rights.
     *
     * @return {@code true} if the view rights have been delegated and {@code false} otherwise.
     */
    public boolean areViewRightsDelegated()
    {
        return delegatedViewRights;
    }

    /**
     * See {@link #areViewRightsDelegated()}.
     *
     * @param delegatedViewRights the given view rights.
     */
    public void setDelegatedViewRights(boolean delegatedViewRights)
    {
        this.delegatedViewRights = delegatedViewRights;
    }
}

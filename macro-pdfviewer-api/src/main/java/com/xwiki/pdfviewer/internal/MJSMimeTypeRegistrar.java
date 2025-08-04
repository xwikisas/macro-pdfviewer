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

import java.lang.reflect.Field;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypes;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.tika.internal.TikaUtils;

/**
 * If the correct mime type for files with the .mjs extension is missing, register the appropriate mime type. To be
 * removed after used platform Tika version includes this fix:
 * https://github.com/apache/tika/commit/ae737cd2625b5e2659c20c27713785df8bfc1957
 *
 * @version $Id$
 * @since 2.6
 */
@Component(roles = MJSMimeTypeRegistrar.class)
@Singleton
public class MJSMimeTypeRegistrar
{
    @Inject
    private Logger logger;

    /**
     * If the correct mime type for files with ".mjs" extension is not detected, reflection is used to modify the
     * {@link TikaUtils} {@link Tika} object with the right mime type configuration for ".mjs" files.
     */
    public void maybeRegisterMJSMimeType()
    {
        try {
            String mjsMimeType = TikaUtils.detect("test.mjs");
            if (!mjsMimeType.equalsIgnoreCase("application/javascript") && !mjsMimeType.equalsIgnoreCase(
                "text/javascript"))
            {
                Field tikaField = TikaUtils.class.getDeclaredField("tika");
                tikaField.setAccessible(true);
                TikaConfig tikaConfig = new TikaConfig(TikaUtils.class.getResource("/tika-config.xml"));
                tikaConfig.getMimeRepository()
                    .addPattern(MimeTypes.getDefaultMimeTypes().forName(MediaType.application("javascript").toString()),
                        "*.mjs");

                // Create a new Tika instance with an associated mime type for the "*.mjs" pattern.
                Tika newTika = new Tika(tikaConfig);
                tikaField.set(null, newTika);
            }
        } catch (Exception e) {
            logger.warn("Failed to register the mime type for ECMAScript modules (.mjs). Root cause: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            throw new RuntimeException("Failed to register the mime type for ECMAScript modules (.mjs). "
                + "Please refer to the PDF Viewer Macro documentation regarding how to add the required mime type.");
        }
    }
}

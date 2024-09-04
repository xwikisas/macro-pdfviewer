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
package com.xwiki.pdfviewer.internal.filter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypes;
import org.apache.tika.mime.MimeTypesReader;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.tika.internal.TikaUtils;

/**
 * If the correct mime type for files with the .mjs extension is missing, register the appropriate mime type.
 * To be removed after used platform Tika version includes this fix:
 * https://github.com/apache/tika/commit/ae737cd2625b5e2659c20c27713785df8bfc1957
 *
 * @version $Id$
 * @since 2.6
 */
@Component(roles = MJSMimeTypeRegistrar.class)
@Singleton
public class MJSMimeTypeRegistrar
{
    private static final String EXCLAMATION_MARK = "!";

    private static final String CUSTOM_TYPE = "custom-mimetypes.xml";

    @Inject
    private Logger logger;

    /**
     * If the correct mime type for .mjs files is not detected, uses reflection to modify the {@link TikaUtils}
     * {@link Tika} object with the right mime type configuration for .mjs files. Also attempts to automatically
     * modify the appropriate jar file and add the custom-mimetypes.xml file, so that after a future restart,
     * reflection is no longer needed.
     */
    public void maybeRegisterMJSMimeType()
    {
        try {
            String mjsMimeType = TikaUtils.detect("test.mjs");
            if (!mjsMimeType.equalsIgnoreCase("application/javascript") && !mjsMimeType.equalsIgnoreCase(
                "text/javascript"))
            {
                boolean uploadedCustomMimeType = uploadCustomMimeType();
                boolean modifiedTikaField = modifyTikaField();

                int val = 0;
                if (uploadedCustomMimeType) {
                    val |= 0x1;
                }
                if (modifiedTikaField) {
                    val |= 0x2;
                }
                switch (val) {
                    case 0:
                        throw new RuntimeException("Failed to automatically add .mjs file extension mime type. "
                            + "Please refer to the documentation for more help.");
                    case 1:
                        throw new RuntimeException("Failed to dynamically add .mjs file extension mime type. Please "
                            + "restart the XWiki instance and refer to the documentation if the issue persists.");
                    default:
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean modifyTikaField()
    {
        try {
            Field tikaField = TikaUtils.class.getDeclaredField("tika");
            tikaField.setAccessible(true);
            TikaConfig tikaConfig = new TikaConfig(TikaUtils.class.getResource("/tika-config.xml"));
            tikaConfig.getMimeRepository()
                .addPattern(MimeTypes.getDefaultMimeTypes().forName(MediaType.application("javascript").toString()),
                    "*.mjs");

            // Create a new Tika instance with an associated mime type for the "*.mjs" pattern.
            Tika newTika = new Tika(tikaConfig);
            tikaField.set(null, newTika);
            return true;
        } catch (Exception e) {
            logger.error("Failed to modify Tika field. Root cause: [{}]", ExceptionUtils.getRootCauseMessage(e));
            return false;
        }
    }

    private boolean uploadCustomMimeType()
    {
        ClassLoader classLoader = MimeTypesReader.class.getClassLoader();
        String pathInJarWithJarPath =
            classLoader.getResource(MimeTypesReader.class.getPackage().getName().replace('.', '/') + "/").getPath();
        String jarFilePath =
            URLDecoder.decode(pathInJarWithJarPath.substring(5, pathInJarWithJarPath.indexOf(EXCLAMATION_MARK)),
                StandardCharsets.UTF_8);
        String pathInJar =
            pathInJarWithJarPath.substring(pathInJarWithJarPath.indexOf(EXCLAMATION_MARK) + 2) + CUSTOM_TYPE;
        File tempJarFile = new File(jarFilePath + ".tmp");

        try (JarFile jarFile = new JarFile(jarFilePath)) {
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tempJarFile));
            // Copy the content of the tika jar file.
            jarFile.stream().forEach(entry -> {
                try {
                    jarOutputStream.putNextEntry(new JarEntry(entry.getName()));
                    if (!entry.isDirectory()) {
                        InputStream entryInputStream = jarFile.getInputStream(entry);
                        entryInputStream.transferTo(jarOutputStream);
                    }
                    jarOutputStream.closeEntry();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            // Add the custom-mimetypes.xml file.
            JarEntry newEntry = new JarEntry(pathInJar);
            jarOutputStream.putNextEntry(newEntry);
            InputStream customFileInputStream =
                MJSMimeTypeRegistrar.class.getClassLoader().getResourceAsStream(pathInJar);
            customFileInputStream.transferTo(jarOutputStream);
            jarOutputStream.closeEntry();
        } catch (Exception e) {
            if (tempJarFile.exists()) {
                tempJarFile.delete();
            }
            logger.error("Failed to add the custom-mimetypes.xml to the tika jar file. Root cause: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            return false;
        }

        if (tempJarFile.exists()) {
            File originalJar = new File(jarFilePath);
            if (!originalJar.delete() || !tempJarFile.renameTo(originalJar)) {
                tempJarFile.delete();
                logger.error("Failed to replace the original tika jar file.");
                return false;
            }
        }
        return true;
    }
}

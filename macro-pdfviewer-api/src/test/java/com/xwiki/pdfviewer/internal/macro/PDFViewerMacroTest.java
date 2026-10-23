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

import java.util.List;
import java.util.Map;

import javax.inject.Provider;
import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xwiki.licensing.Licensor;
import com.xwiki.pdfviewer.internal.MJSMimeTypeRegistrar;
import com.xwiki.pdfviewer.macro.PDFFile;
import com.xwiki.pdfviewer.macro.PDFViewerMacroParameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link PDFViewerMacro}.
 *
 * @version $Id$
 */
@ComponentTest
class PDFViewerMacroTest
{
    private static final String TEMPLATE_NAME = "pdfviewer/pdfviewer.vm";

    private static final DocumentReference LICENSE_DOCUMENT =
        new DocumentReference("wiki", List.of("PDFViewer", "Code"), "WebHome");

    @InjectMockComponents
    private PDFViewerMacro macro;

    @MockComponent
    private TemplateManager templateManager;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @MockComponent
    private Licensor licensor;

    @MockComponent
    private Provider<XWikiContext> wikiContextProvider;

    @MockComponent
    private MJSMimeTypeRegistrar mjsMimeTypeRegistrar;

    @MockComponent
    private PDFFileBuilder fileBuilder;

    @Mock
    private XWikiContext wikiContext;

    @Mock
    private Template template;

    private final ScriptContext scriptContext = new SimpleScriptContext();

    private final MacroTransformationContext macroContext = new MacroTransformationContext();

    private final List<Block> templateBlocks = List.of(new WordBlock("viewer"));

    private PDFViewerMacroParameters parameters;

    @BeforeEach
    void setup() throws Exception
    {
        when(this.wikiContextProvider.get()).thenReturn(this.wikiContext);
        when(this.wikiContext.getWikiId()).thenReturn("wiki");
        when(this.licensor.hasLicensure(LICENSE_DOCUMENT)).thenReturn(true);
        when(this.scriptContextManager.getScriptContext()).thenReturn(this.scriptContext);
        when(this.templateManager.getTemplate(TEMPLATE_NAME)).thenReturn(this.template);
        when(this.templateManager.execute(this.template)).thenReturn(new XDOM(this.templateBlocks));
        when(this.fileBuilder.handleInternalAttachment(anyString(), anyBoolean(), any())).thenReturn(new PDFFile());
        when(this.fileBuilder.handleExternalURL(anyString(), anyBoolean())).thenReturn(new PDFFile());

        this.parameters = new PDFViewerMacroParameters();
    }

    @Test
    void executeWithoutLicense() throws Exception
    {
        when(this.licensor.hasLicensure(LICENSE_DOCUMENT)).thenReturn(false);
        this.parameters.setFile("file.pdf");

        List<Block> result = this.macro.execute(this.parameters, null, this.macroContext);

        assertEquals(1, result.size());
        MacroBlock licenseMessage = (MacroBlock) result.get(0);
        assertEquals("missingLicenseMessage", licenseMessage.getId());
        assertEquals("proMacros.extension.name", licenseMessage.getParameter("extensionName"));
        verifyNoInteractions(this.fileBuilder, this.mjsMimeTypeRegistrar, this.templateManager);
    }

    @Test
    void executeWithInternalAttachment() throws Exception
    {
        PDFFile pdfFile = new PDFFile();
        when(this.fileBuilder.handleInternalAttachment("file.pdf", false, "Space.Page")).thenReturn(pdfFile);
        this.parameters.setFile("file.pdf");
        this.parameters.setDocument("Space.Page");
        this.parameters.setWidth("50%");
        this.parameters.setHeight(500);

        List<Block> result = this.macro.execute(this.parameters, null, this.macroContext);

        assertEquals(this.templateBlocks, result);
        verify(this.mjsMimeTypeRegistrar).maybeRegisterMJSMimeType();
        assertEquals(List.of(pdfFile), this.scriptContext.getAttribute("files", ScriptContext.ENGINE_SCOPE));
        assertEquals(Map.of("width", "50%", "height", "500"),
            this.scriptContext.getAttribute("params", ScriptContext.ENGINE_SCOPE));
    }

    @Test
    void executeSplitsFilesAfterThePDFExtensionOnly() throws Exception
    {
        PDFFile first = new PDFFile();
        PDFFile second = new PDFFile();
        PDFFile third = new PDFFile();
        PDFFile fourth = new PDFFile();
        when(this.fileBuilder.handleInternalAttachment("my,file.pdf", false, null)).thenReturn(first);
        when(this.fileBuilder.handleInternalAttachment("Space.Page@other.pdf", false, null)).thenReturn(second);
        when(this.fileBuilder.handleExternalURL("https://example.org/a,b.pdf", false)).thenReturn(third);
        when(this.fileBuilder.handleExternalURL("http://example.org/c.pdf", false)).thenReturn(fourth);
        this.parameters.setFile("my,file.pdf,Space.Page@other.pdf");
        this.parameters.setFileFromExternalUrl("https://example.org/a,b.pdf,http://example.org/c.pdf");

        this.macro.execute(this.parameters, null, this.macroContext);

        // Files from the "file" parameter come first, followed by the ones from "fileFromExternalUrl".
        assertEquals(List.of(first, second, third, fourth),
            this.scriptContext.getAttribute("files", ScriptContext.ENGINE_SCOPE));
    }

    @Test
    void executeWithURLInFileParameter() throws Exception
    {
        this.parameters.setFile("https://example.org/file.pdf");

        this.macro.execute(this.parameters, null, this.macroContext);

        verify(this.fileBuilder).handleExternalURL("https://example.org/file.pdf", false);
        verify(this.fileBuilder, never()).handleInternalAttachment(anyString(), anyBoolean(), any());
    }

    @Test
    void executeWithoutFiles() throws Exception
    {
        this.parameters.setFile(" ");

        List<Block> result = this.macro.execute(this.parameters, null, this.macroContext);

        assertEquals(this.templateBlocks, result);
        assertEquals(List.of(), this.scriptContext.getAttribute("files", ScriptContext.ENGINE_SCOPE));
        verifyNoInteractions(this.fileBuilder);
    }

    @ParameterizedTest
    @ValueSource(strings = { "1", "true", "yes", "TRUE", "Yes" })
    void executeWithDelegatedRights(String asAuthor) throws Exception
    {
        this.parameters.setFile("file.pdf");
        this.parameters.setAsAuthor(asAuthor);

        this.macro.execute(this.parameters, null, this.macroContext);

        verify(this.fileBuilder).handleInternalAttachment("file.pdf", true, null);
    }

    @Test
    void executeWithDelegatedRightsInRestrictedContext() throws Exception
    {
        // Restricted renderings, such as comments, display content that wasn't written by the author of the page.
        this.macroContext.getTransformationContext().setRestricted(true);
        this.parameters.setFile("file.pdf");
        this.parameters.setAsAuthor("true");

        this.macro.execute(this.parameters, null, this.macroContext);

        verify(this.fileBuilder).handleInternalAttachment("file.pdf", false, null);
    }

    @ParameterizedTest
    @ValueSource(strings = { "0", "false", "no", "", "on" })
    void executeWithoutDelegatedRights(String asAuthor) throws Exception
    {
        this.parameters.setFile("file.pdf");
        this.parameters.setAsAuthor(asAuthor);

        this.macro.execute(this.parameters, null, this.macroContext);

        verify(this.fileBuilder).handleInternalAttachment("file.pdf", false, null);
    }

    @Test
    void executeWhenFileResolutionFails() throws Exception
    {
        XWikiException cause = new XWikiException();
        when(this.fileBuilder.handleInternalAttachment("file.pdf", false, null)).thenThrow(cause);
        this.parameters.setFile("file.pdf");

        MacroExecutionException exception = assertThrows(MacroExecutionException.class,
            () -> this.macro.execute(this.parameters, null, this.macroContext));

        assertEquals("Failed to render the PDF Viewer template.", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void executeWhenMJSMimeTypeRegistrationFails()
    {
        RuntimeException cause = new RuntimeException("registration failure");
        doThrow(cause).when(this.mjsMimeTypeRegistrar).maybeRegisterMJSMimeType();
        this.parameters.setFile("file.pdf");

        MacroExecutionException exception = assertThrows(MacroExecutionException.class,
            () -> this.macro.execute(this.parameters, null, this.macroContext));

        assertSame(cause, exception.getCause());
        verifyNoInteractions(this.fileBuilder);
    }

    @Test
    void supportsInlineMode()
    {
        assertFalse(this.macro.supportsInlineMode());
        assertTrue(this.macro.getDescriptor().getDefaultCategories().contains("Content"));
    }
}

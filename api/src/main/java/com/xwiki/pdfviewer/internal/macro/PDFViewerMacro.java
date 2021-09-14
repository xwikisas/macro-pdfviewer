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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;

import com.xwiki.pdfviewer.macro.PDFViewerMacroParameters;

@Component
@Named("pdfviewer")
@Singleton
public class PDFViewerMacro extends AbstractMacro<PDFViewerMacroParameters>
{
    protected static final String FILE_BIND_NAME = "file";

    protected static final String DOCUMENT_BIND_NAME = "docname";

    protected static final String WIDTH_BIND_NAME = "width";

    protected static final String HEIGHT_BIND_NAME = "height";

    protected static final String ASAUTHOR_BIND_NAME = "asAuthor";

    @Inject
    private TemplateManager templateManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    private Logger logger;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public PDFViewerMacro()
    {
        super("PDF Viewer", "View PDF attachments inside wiki pages without downloading or importing them.",
            PDFViewerMacroParameters.class);
    }

    @Override
    public List<Block> execute(PDFViewerMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        Template customTemplate = this.templateManager.getTemplate("pdfviewer/pdfviewer.vm");

        try {
            this.bindParameters(parameters);

            // This conversion was added to avoid the problem described by
            // https://jira.xwiki.org/browse/XRENDERING-615 and should be removed after the application starts depending
            // on a version of XWiki >= the version where it's fixed.
            List<Block> modifiedBlocks = new ArrayList<Block>();
            for (Block block : this.templateManager.execute(customTemplate).getChildren()) {
                RawBlock xdomBlock = (RawBlock) block;
                modifiedBlocks.add(new RawBlock(xdomBlock.getRawContent(), Syntax.XHTML_1_0));
            }

            return modifiedBlocks;
        } catch (Exception e) {
            logger.warn("Failed to render PDFViewer macro template. Root cause is: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
        }
        return null;
    }

    private void bindParameters(PDFViewerMacroParameters parameters)
    {
        ScriptContext scriptContext = scriptContextManager.getScriptContext();

        scriptContext.setAttribute(FILE_BIND_NAME, parameters.getFile(), ScriptContext.ENGINE_SCOPE);
        scriptContext.setAttribute(WIDTH_BIND_NAME, parameters.getWidth(), ScriptContext.ENGINE_SCOPE);
        scriptContext.setAttribute(HEIGHT_BIND_NAME, parameters.getHeight(), ScriptContext.ENGINE_SCOPE);
        scriptContext.setAttribute(DOCUMENT_BIND_NAME, parameters.getDocument(), ScriptContext.ENGINE_SCOPE);
        scriptContext.setAttribute(ASAUTHOR_BIND_NAME, parameters.getAsauthor(), ScriptContext.ENGINE_SCOPE);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return false;
    }
}

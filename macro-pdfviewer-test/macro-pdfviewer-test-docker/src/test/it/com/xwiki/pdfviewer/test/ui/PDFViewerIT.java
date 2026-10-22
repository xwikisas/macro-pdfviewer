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
package com.xwiki.pdfviewer.test.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.docker.junit5.ExtensionOverride;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import com.xwiki.pdfviewer.po.PDFViewerMacro;
import com.xwiki.pdfviewer.po.PDFViewerMacroPage;
import com.xwiki.pdfviewer.po.TabLayoutPDFMacro;
import com.xwiki.pdfviewer.po.TabLayoutPDFMacroPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI tests for the PDF Viewer Macro.
 *
 * @version $Id$
 * @since 2.7
 */
@UITest(extensionOverrides = { @ExtensionOverride(extensionId = "com.google.code.findbugs:jsr305", overrides = {
    "features=com.google.code.findbugs:annotations" }),
    @ExtensionOverride(extensionId = "org.bouncycastle:bcprov-jdk18on", overrides = {
        "features=org.bouncycastle:bcprov-jdk15on" }),
    @ExtensionOverride(extensionId = "org.bouncycastle:bcpkix-jdk18on", overrides = {
        "features=org.bouncycastle:bcpkix-jdk15on" }),
    @ExtensionOverride(extensionId = "org.bouncycastle:bcmail-jdk18on", overrides = {
        "features=org.bouncycastle:bcmail-jdk15on" }) })
class PDFViewerIT
{
    private static final String PDF_TEXT = "PDF file for testing the pdf viewer macro.";

    private static final String PDF_TEXT_FORMAT = "PDF file for testing the pdf viewer macro-%d.";

    private static final String NO_ACCESS_ERROR =
        "Error: The document does not exist, or you have no access to that document.";

    private static final String SPECIAL_CHARACTERS_PDF =
        "Te.t.with.special,.chrs.-.test.It.s.a.test.Test.1.and.2.3.4.100.a.test.1.2.3.5.pdf";

    /**
     * A non-terminal page holding {@code PDFTest.pdf}, shared by the tests displaying a PDF attached to another page.
     */
    private static final DocumentReference ATTACHMENTS_PAGE =
        new DocumentReference("xwiki", List.of("PDFViewerMacro", "Attachments"), "WebHome");

    private static final String ATTACHMENTS_PAGE_REFERENCE = "PDFViewerMacro.Attachments.WebHome";

    @BeforeAll
    void beforeAll(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        setup.createUser("UserTest", "UserTest", "");

        setup.createPage(ATTACHMENTS_PAGE, "Page holding a PDF attachment");
        attachFile(setup, ATTACHMENTS_PAGE, "PDFTest.pdf");
    }

    @Test
    @Order(1)
    void pdfAttachedToCurrentPage(TestUtils setup, TestReference testReference) throws Exception
    {
        // The PDF is attached to the page holding the macro, with default and custom sizes.
        setup.createPage(testReference, getMacroContent("macroContent.vm") + "\n\n{{pdfviewer file=\""
            + SPECIAL_CHARACTERS_PDF + "\"/}}");
        attachFile(setup, testReference, "PDFTest.pdf");
        attachFile(setup, testReference, SPECIAL_CHARACTERS_PDF);

        PDFViewerMacroPage page = gotoPDFViewerMacroPage(setup, testReference);
        assertEquals(4, page.getPDFViewerMacrosCount());

        PDFViewerMacro defaultSize = page.getPDFViewer(0);
        assertEquals("100%", defaultSize.getWidth());
        assertEquals("1000px", defaultSize.getHeight());
        assertEquals(PDF_TEXT, defaultSize.getText());

        PDFViewerMacro customSize = page.getPDFViewer(1);
        assertEquals("50%", customSize.getWidth());
        assertEquals("500px", customSize.getHeight());
        assertEquals(PDF_TEXT, customSize.getText());

        // A file name with special characters, including a comma that must not be taken as a file separator.
        assertThat(page.getPDFViewer(3).getText(), containsString("NEW TEST!"));
    }

    @Test
    @Order(2)
    void pdfAttachedToAnotherPage(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.createPage(testReference, String.join("\n\n",
            // The "document" parameter, used by the macros created before the attachment picker.
            "{{pdfviewer file=\"PDFTest.pdf\" document=\"" + ATTACHMENTS_PAGE_REFERENCE + "\"/}}",
            // A full attachment reference, as produced by the attachment picker.
            "{{pdfviewer file=\"" + ATTACHMENTS_PAGE_REFERENCE + "@PDFTest.pdf\"/}}",
            // An attachment missing from the given document falls back to the one of the current page.
            "{{pdfviewer file=\"PDFTest-2.pdf\" document=\"" + ATTACHMENTS_PAGE_REFERENCE + "\"/}}"));
        attachFile(setup, testReference, "PDFTest-2.pdf");

        PDFViewerMacroPage page = gotoPDFViewerMacroPage(setup, testReference);
        assertEquals(3, page.getPDFViewerMacrosCount());
        assertEquals(PDF_TEXT, page.getPDFViewer(0).getText());
        assertEquals(PDF_TEXT, page.getPDFViewer(1).getText());
        assertEquals(String.format(PDF_TEXT_FORMAT, 2), page.getPDFViewer(2).getText());
    }

    @Test
    @Order(3)
    void tabLayout(TestUtils setup, TestReference testReference) throws Exception
    {
        // Three macros displaying several PDFs each: file="PDFTest-1.pdf,PDFTest-2.pdf,PDFTest-3.pdf", the first
        // two only with a custom size, and the three in a different order.
        setup.createPage(testReference, getMacroContent("tabLayout.vm"));
        attachFile(setup, testReference, "PDFTest-1.pdf");
        attachFile(setup, testReference, "PDFTest-2.pdf");
        attachFile(setup, testReference, "PDFTest-3.pdf");

        setup.gotoPage(testReference);
        TabLayoutPDFMacroPage page = new TabLayoutPDFMacroPage();
        assertEquals(3, page.getPDFViewerMacrosCount());

        TabLayoutPDFMacro allFiles = page.getPDFViewer(0);
        assertEquals(List.of("PDFTest-1.pdf", "PDFTest-2.pdf", "PDFTest-3.pdf"), allFiles.getTabsNames());
        // The first tab is active by default.
        assertActiveTab(allFiles, 0, "PDFTest-1.pdf");
        assertEquals(String.format(PDF_TEXT_FORMAT, 1), allFiles.getText());
        assertEquals("1000", allFiles.getHeight());
        assertEquals("100%", allFiles.getWidth());

        TabLayoutPDFMacro twoFiles = page.getPDFViewer(1);
        assertEquals(List.of("PDFTest-1.pdf", "PDFTest-2.pdf"), twoFiles.getTabsNames());
        assertEquals(0, twoFiles.getActiveTab());
        assertEquals("500", twoFiles.getHeight());
        assertEquals("50%", twoFiles.getWidth());

        // Selecting a tab selects the same PDF in all the macros of the page, through the page URL.
        page = allFiles.clickTab(1);
        assertEquals("PDFTest-2.pdf", page.getSelectedFile());

        allFiles = page.getPDFViewer(0);
        assertActiveTab(allFiles, 1, "PDFTest-2.pdf");
        assertEquals(String.format(PDF_TEXT_FORMAT, 2), allFiles.getText());

        assertActiveTab(page.getPDFViewer(1), 1, "PDFTest-2.pdf");

        // The third macro lists the PDFs in a different order: the selected PDF is in its last tab.
        TabLayoutPDFMacro reordered = page.getPDFViewer(2);
        assertEquals(List.of("PDFTest-1.pdf", "PDFTest-3.pdf", "PDFTest-2.pdf"), reordered.getTabsNames());
        assertActiveTab(reordered, 2, "PDFTest-2.pdf");
        assertEquals(String.format(PDF_TEXT_FORMAT, 2), reordered.getText());

        page = allFiles.clickTab(2);
        assertEquals("PDFTest-3.pdf", page.getSelectedFile());

        allFiles = page.getPDFViewer(0);
        assertActiveTab(allFiles, 2, "PDFTest-3.pdf");
        assertEquals(String.format(PDF_TEXT_FORMAT, 3), allFiles.getText());

        // A macro that doesn't list the selected PDF has no active tab.
        assertEquals(-1, page.getPDFViewer(1).getActiveTab());
    }

    @Test
    @Order(4)
    void pdfFromAttachmentURL(TestUtils setup, TestReference testReference)
    {
        // A URL pointing to an attachment of this wiki is displayed as is, after checking the view right on it.
        String attachmentURL = setup.getBaseBinURL() + "download/PDFViewerMacro/Attachments/WebHome/PDFTest.pdf";
        setup.createPage(testReference, "{{pdfviewer file=\"" + attachmentURL + "\"/}}");

        PDFViewerMacro viewer = new PDFViewerMacroPage().getPDFViewer(0);
        assertEquals(attachmentURL, viewer.getPdfUrl());
        assertEquals(PDF_TEXT, viewer.getText());
    }

    @Test
    @Order(5)
    void asAuthor(TestUtils setup, TestReference testReference) throws Exception
    {
        // With asauthor="1", a user who can't view the page holding the PDF can still view it, using the view right
        // of the content author of the page holding the macro.
        DocumentReference noRightsPage = new DocumentReference("xwiki", "PDFViewerMacro", "PDFNoRights");
        setup.createPage(noRightsPage, "no view rights for UserTest", "PDFNoRights");
        attachFile(setup, noRightsPage, "PDFTest.pdf");
        setup.setRights(noRightsPage, "XWiki.XWikiAllGroup", "UserTest", "view", false);

        DocumentReference noRightsPage2 = new DocumentReference("xwiki", "PDFViewerMacro", "PDFNoRights2");
        setup.createPage(noRightsPage2, "no view rights for UserTest", "PDFNoRights2");
        attachFile(setup, noRightsPage2, "PDFTest-2.pdf");
        setup.setRights(noRightsPage2, "XWiki.XWikiAllGroup", "UserTest", "view", false);

        // The first macro uses asauthor="1", the second the default asauthor="0", and the third the URL of the
        // restricted attachment, which must be recognized as an attachment of this wiki to have its view right checked.
        String restrictedURL = setup.getBaseBinURL() + "download/PDFViewerMacro/PDFNoRights2/PDFTest-2.pdf";
        setup.createPage(testReference,
            getMacroContent("asAuthor.vm") + "\n\n{{pdfviewer file=\"" + restrictedURL + "\"/}}");

        PDFViewerMacroPage page = new PDFViewerMacroPage();
        assertEquals(3, page.getPDFViewerMacrosCount());
        assertEquals(PDF_TEXT, page.getPDFViewer(0).getText());
        assertEquals(String.format(PDF_TEXT_FORMAT, 2), page.getPDFViewer(1).getText());
        assertEquals(String.format(PDF_TEXT_FORMAT, 2), page.getPDFViewer(2).getText());

        setup.login("UserTest", "UserTest");
        page = gotoPDFViewerMacroPage(setup, testReference);

        assertEquals(1, page.getPDFViewerMacrosCount());
        assertEquals(PDF_TEXT, page.getPDFViewer(0).getText());
        assertTrue(page.hasErrorMessage());
        assertEquals(NO_ACCESS_ERROR, page.getErrorMessage(0));
        assertEquals(NO_ACCESS_ERROR, page.getErrorMessage(1));

        setup.loginAsSuperAdmin();
    }

    private void assertActiveTab(TabLayoutPDFMacro macro, int expectedIndex, String expectedFile)
    {
        int activeTab = macro.getActiveTab();
        assertEquals(expectedIndex, activeTab);
        assertEquals(expectedFile, macro.getTabName(activeTab));
        assertThat(macro.getTabHref(activeTab), containsString(expectedFile));
    }

    private PDFViewerMacroPage gotoPDFViewerMacroPage(TestUtils setup, EntityReference reference)
    {
        setup.gotoPage(reference);
        return new PDFViewerMacroPage();
    }

    private void attachFile(TestUtils setup, EntityReference page, String fileName) throws Exception
    {
        try (InputStream content = getClass().getResourceAsStream("/pdfmacro/" + fileName)) {
            setup.attachFile(page, fileName, content, false);
        }
    }

    private String getMacroContent(String filename)
    {
        try (InputStream inputStream = getClass().getResourceAsStream("/pdfmacro/" + filename)) {
            if (inputStream == null) {
                throw new RuntimeException("Failed to load " + filename + " from resources.");
            }

            return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
                .filter(line -> !line.trim().startsWith("##")).collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read macro file: " + filename, e);
        }
    }
}

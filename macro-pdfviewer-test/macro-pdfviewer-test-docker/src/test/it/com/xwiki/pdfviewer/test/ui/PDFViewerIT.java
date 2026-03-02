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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.flamingo.skin.test.po.AttachmentsPane;
import org.xwiki.flamingo.skin.test.po.AttachmentsViewPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.ExtensionOverride;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CreatePagePage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.EditPage;

import com.xwiki.pdfviewer.po.PDFViewerMacro;
import com.xwiki.pdfviewer.po.PDFViewerMacroPage;
import com.xwiki.pdfviewer.po.TabLayoutPDFMacro;
import com.xwiki.pdfviewer.po.TabLayoutPDFMacroPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI tests for the PDF Viewer Macro.
 *
 * @version $Id$
 * @since 2.6.3
 */
@UITest(extensionOverrides = { @ExtensionOverride(extensionId = "com.google.code.findbugs:jsr305", overrides = {
    "features=com.google.code.findbugs:annotations" }),
    @ExtensionOverride(extensionId = "org.bouncycastle:bcprov-jdk18on", overrides = {
        "features=org.bouncycastle:bcprov-jdk15on" }),
    @ExtensionOverride(extensionId = "org.bouncycastle:bcpkix-jdk18on", overrides = {
        "features=org.bouncycastle:bcpkix-jdk15on" }),
    @ExtensionOverride(extensionId = "org.bouncycastle:bcmail-jdk18on", overrides = {
        "features=org.bouncycastle:bcmail-jdk15on" }) })

public class PDFViewerIT
{
    @BeforeAll
    void beforeAll(TestUtils testUtils)
    {
        testUtils.loginAsSuperAdmin();
        testUtils.createUser("UserTest", "UserTest", "");
    }

    @Test
    @Order(1)
    void pdfAttachedToCurrentPageTest(TestUtils setup, TestConfiguration testConfiguration)
    {
        // Checks that the macro works when the pdf is attached to the current page, file="PDFTest.pdf".
        createPage(setup, getMacroContent("macroContent.vm"), "pdfAttachedToCurrentPageTest");

        uploadFile("PDFTest.pdf", testConfiguration);
        PDFViewerMacroPage page = new PDFViewerMacroPage();
        page.reloadPage();

        assertEquals(3, page.getPDFViewerMacrosCount());

        PDFViewerMacro viewer0 = page.getPDFViewer(0);

        // Checks the default height and width.
        assertEquals("100%", viewer0.getWidth());
        assertEquals("1000px", viewer0.getHeight());
        assertEquals("PDF file for testing the pdf viewer macro.", viewer0.getText());

        // Checks the personalized values for height and width.
        PDFViewerMacro viewer1 = page.getPDFViewer(1);
        assertEquals("50%", viewer1.getWidth());
        assertEquals("500px", viewer1.getHeight());
        assertEquals("PDF file for testing the pdf viewer macro.", viewer1.getText());
    }

    @Test
    @Order(2)
    void pdfAttachedToAnotherPageTest(TestUtils setup, TestConfiguration testConfiguration)
    {
        // Checks that the macro works when the pdf is attached to another page and the "document" parameter is
        // used.
        createPage(setup, "normal page with a pdf attached", "PageWithAttachedPDF");
        uploadFile("PDFTest.pdf", testConfiguration);

        createPage(setup, "{{pdfviewer file=\"PDFTest.pdf\" document=\"PDFViewerMacro.PageWithAttachedPDF\"/}}",
            "pdfAttachedToAnotherPageTest");

        PDFViewerMacroPage page = new PDFViewerMacroPage();

        assertEquals(1, page.getPDFViewerMacrosCount());
        PDFViewerMacro viewer0 = page.getPDFViewer(0);
        assertEquals("PDF file for testing the pdf viewer macro.", viewer0.getText());
    }

    @Test
    @Order(3)
    void pdfAttachedToTerminalPageTest(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        // Checks that the macro works when the pdf is attached to another page, which is terminal, and the
        // "document" parameter is used.
        createTerminalPageWithPDFAttached(setup);
        createPage(setup, "{{pdfviewer file=\"PDFTest.pdf\" document=\"PDFViewerMacro.TerminalPageWithPDF\"/}}",
            "pdfAttachedToTerminalPageTest");
        PDFViewerMacroPage page = new PDFViewerMacroPage();

        assertEquals(1, page.getPDFViewerMacrosCount());
        PDFViewerMacro viewer0 = page.getPDFViewer(0);
        assertEquals("PDF file for testing the pdf viewer macro.", viewer0.getText());
    }

    @Test
    @Order(4)
    void tabLayoutTest(TestUtils setup, TestConfiguration testConfiguration)
    {
        // Checks that the macro works when the tab layout is used, file="PDFTest-1.pdf,PDFTest-2.pdf,PDFTest-3.pdf".
        createPage(setup, getMacroContent("tabLayout.vm"), "tabLayoutTest");

        uploadFile("PDFTest-1.pdf", testConfiguration);
        uploadFile("PDFTest-2.pdf", testConfiguration);
        uploadFile("PDFTest-3.pdf", testConfiguration);

        TabLayoutPDFMacroPage page = new TabLayoutPDFMacroPage();
        page.reloadPage();

        // Check that there are 3 PDF Viewer macros on the page.
        assertEquals(3, page.getPDFViewerMacrosCount());

        TabLayoutPDFMacro viewer0 = page.getPDFViewer(0);

        // Check that the 1st macro has 3 tabs (3 pdfs).
        assertEquals(3, viewer0.getTabs().size());
        assertEquals(Arrays.asList("PDFTest-1.pdf", "PDFTest-2.pdf", "PDFTest-3.pdf"), viewer0.getTabsNames());

        int activeTab = viewer0.getActiveTab();

        // Checks that the default active tab is the first one.
        assertEquals(0, activeTab);
        assertEquals("PDFTest-1.pdf", viewer0.getTabName(activeTab));
        assertTrue(viewer0.getTabHref(activeTab).contains("PDFTest-1.pdf"));

        // Checks that the corresponding text is visible.
        assertEquals("PDF file for testing the pdf viewer macro-1.", viewer0.getText());

        // Checks the default height and width.
        assertEquals("1000", viewer0.getHeight());
        assertEquals("100%", viewer0.getWidth());

        // Checks the 2nd macro's number of tabs and their names.
        TabLayoutPDFMacro viewer3 = page.getPDFViewer(1);
        assertEquals(2, viewer3.getTabs().size());
        assertEquals(Arrays.asList("PDFTest-1.pdf", "PDFTest-2.pdf"), viewer3.getTabsNames());

        // Checks that the 2nd macro's active tab is the first one. There can't be different pdfs shown at the same
        // time if you have multiple PDF Viewer macros on the same page.
        int activeTab2 = viewer3.getActiveTab();
        assertEquals(0, activeTab2);

        // Checks the personalized values for height and width.
        assertEquals("500", viewer3.getHeight());
        assertEquals("50%", viewer3.getWidth());

        // Changes the tab.
        viewer0.clickTab(1);

        // Checks that the URL also changes with the tab.
        assertTrue(setup.getDriver().getCurrentUrl().contains("file=PDFTest-2.pdf"));

        TabLayoutPDFMacroPage page2 = new TabLayoutPDFMacroPage();
        TabLayoutPDFMacro viewer1 = page2.getPDFViewer(0);

        // Checks that the active tab has changed.
        activeTab = viewer1.getActiveTab();
        assertEquals(1, activeTab);
        assertEquals("PDFTest-2.pdf", viewer1.getTabName(activeTab));
        assertTrue(viewer1.getTabHref(activeTab).contains("PDFTest-2.pdf"));

        // Checks that the corresponding text is visible.
        assertEquals("PDF file for testing the pdf viewer macro-2.", viewer1.getText());

        TabLayoutPDFMacro viewer4 = page2.getPDFViewer(1);
        assertEquals(1, viewer4.getActiveTab());
        assertEquals("PDFTest-2.pdf", viewer4.getTabName(activeTab));

        // Checks the 3rd macro, with the same pdfs as the first one, but in a different order.
        TabLayoutPDFMacro viewer6 = page2.getPDFViewer(2);
        assertEquals(3, viewer6.getTabs().size());
        assertEquals(Arrays.asList("PDFTest-1.pdf", "PDFTest-3.pdf", "PDFTest-2.pdf"), viewer6.getTabsNames());

        // Checks that the shown pdf is the same one as in the first and second macro, even if it's not in the same
        // tab (different order).
        assertEquals(2, viewer6.getActiveTab());
        assertEquals("PDF file for testing the pdf viewer macro-2.", viewer6.getText());

        viewer1.clickTab(2);

        assertTrue(setup.getDriver().getCurrentUrl().contains("file=PDFTest-3.pdf"));
        TabLayoutPDFMacroPage page3 = new TabLayoutPDFMacroPage();
        TabLayoutPDFMacro viewer2 = page3.getPDFViewer(0);

        activeTab = viewer2.getActiveTab();
        assertEquals(2, activeTab);
        assertEquals("PDFTest-3.pdf", viewer2.getTabName(activeTab));
        assertTrue(viewer2.getTabHref(activeTab).contains("PDFTest-3.pdf"));
        assertEquals("PDF file for testing the pdf viewer macro-3.", viewer2.getText());

        // Checks that if one macro doesn't have the same pdf in its list, which is active in the other macros,
        // there will be no pdf shown or tab active.
        TabLayoutPDFMacro viewer5 = page3.getPDFViewer(1);
        assertEquals(-1, viewer5.getActiveTab());
    }

    @Test
    @Order(5)
    void externalPDFTest(TestUtils setup, TestConfiguration testConfiguration)
    {
        // Checks that the macro works when the pdf is set as an external link.
        createPage(setup, "normal page with a pdf attached", "NormalPageWithPDF");
        uploadFile("PDFTest.pdf", testConfiguration);

        String externalLink = getModifiedURL(setup.getDriver().getCurrentUrl(),
            "download/PDFViewerMacro/NormalPageWithPDF/PDFTest.pdf?rev=1.1");
        String content = "{{pdfviewer file=\"" + externalLink + "\"/}}";

        createPage(setup, content, "PageWithExternalPDFTest");
        PDFViewerMacroPage page = new PDFViewerMacroPage();
        PDFViewerMacro viewer0 = page.getPDFViewer(0);

        assertTrue(viewer0.getPdfUrl().contains("PDFTest.pdf"));
        assertTrue(viewer0.getPdfUrl().contains("PDFViewerMacro/NormalPageWithPDF"));
    }

    @Test
    @Order(6)
    void asAuthorTest(TestUtils setup) throws Exception
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "PDFViewerMacro", "PDFNoRights");
        setup.createPage(documentReference, "no view rights for UserTest", "PDFNoRights");
        setup.attachFile(documentReference, "PDFTest.pdf", getClass().getResourceAsStream("/pdfmacro/PDFTest.pdf"),
            false);
        setup.setRights(documentReference, "XWiki.XWikiAllGroup", "UserTest", "view", false);

        DocumentReference documentReference2 = new DocumentReference("xwiki", "PDFViewerMacro", "PDFNoRights2");
        setup.createPage(documentReference2, "no view rights for UserTest", "PDFNoRights2");
        setup.attachFile(documentReference2, "PDFTest-2.pdf", getClass().getResourceAsStream("/pdfmacro/PDFTest-2.pdf"),
            false);
        setup.setRights(documentReference2, "XWiki.XWikiAllGroup", "UserTest", "view", false);

        createPage(setup, getMacroContent("asAuthor.vm"), "asAuthorTest");

        PDFViewerMacroPage page = new PDFViewerMacroPage();

        assertEquals(2, page.getPDFViewerMacrosCount());
        PDFViewerMacro viewer0 = page.getPDFViewer(0);
        assertEquals("PDF file for testing the pdf viewer macro.", viewer0.getText());

        PDFViewerMacro viewer1 = page.getPDFViewer(1);
        assertEquals("PDF file for testing the pdf viewer macro-2.", viewer1.getText());

        setup.login("UserTest", "UserTest");
        setup.gotoPage("PDFViewerMacro", "asAuthorTest");

        PDFViewerMacroPage page2 = new PDFViewerMacroPage();

        assertEquals(1, page2.getPDFViewerMacrosCount());
        PDFViewerMacro viewer2 = page2.getPDFViewer(0);
        assertEquals("PDF file for testing the pdf viewer macro.", viewer2.getText());

        assertTrue(page2.hasErrorMessage());
        assertEquals("Error: The document does not exist, or you have no access to that document.",
            page2.getErrorMessage(0));

        setup.loginAsSuperAdmin();
    }

    @Test
    @Order(6)
    void pdfAttachedToAnotherPageNotFoundTest(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {

        DocumentReference documentReference = new DocumentReference("xwiki", "PDFViewerMacro", "PageWithIncorrectPDF");
        setup.createPage(documentReference, "page with a pdf ", "PageWithIncorrectPDF");
        setup.attachFile(documentReference, "PDFTest.pdf", getClass().getResourceAsStream("/pdfmacro/PDFTest.pdf"),
            false);

        DocumentReference documentReference2 =
            new DocumentReference("xwiki", "PDFViewerMacro", "pdfAttachedToAnotherPageNotFoundTest");
        setup.createPage(documentReference2,
            "{{pdfviewer file=\"PDFTest-2.pdf\" document=\"PDFViewerMacro.PageWithIncorrectPDF\"/}}\n",
            "pdfAttachedToAnotherPageNotFoundTest");
        setup.attachFile(documentReference2, "PDFTest-2.pdf", getClass().getResourceAsStream("/pdfmacro/PDFTest-2.pdf"),
            false);

        PDFViewerMacroPage page = new PDFViewerMacroPage();

        page.reloadPage();

        assertEquals(1, page.getPDFViewerMacrosCount());
        PDFViewerMacro viewer0 = page.getPDFViewer(0);
        assertEquals("PDF file for testing the pdf viewer macro.", viewer0.getText());
    }

    @Test
    @Order(7)
    void pdfWithSpecialCharactersTest(TestUtils setup, TestConfiguration testConfiguration)
    {
        createPage(setup,
            "{{pdfviewer file=\"Te.t.with.special,.chrs.-.test.It.s.a.test.Test.1.and.2.3.4.100.a.test.1.2.3.5.pdf\"/}}",
            "pdfWithSpecialCharactersTest");
        uploadFile("Te.t.with.special,.chrs.-.test.It.s.a.test.Test.1.and.2.3.4.100.a.test.1.2.3.5.pdf",
            testConfiguration);
        PDFViewerMacroPage page = new PDFViewerMacroPage();
        page.reloadPage();

        PDFViewerMacro viewer0 = page.getPDFViewer(0);

        // Checks the default height and width.
        assertEquals("100%", viewer0.getWidth());
        assertEquals("1000px", viewer0.getHeight());
        assertTrue(viewer0.getText().contains("NEW TEST!"));
    }

    @Test
    @Order(8)
    void pdfReferenceTest(TestUtils setup, TestConfiguration testConfiguration)
    {
        createPage(setup, "normal page with a pdf attached", "NormalPageWithPDF");
        uploadFile("PDFTest.pdf", testConfiguration);

        createPage(setup, "{{pdfviewer file=\"PDFViewerMacro.NormalPageWithPDF@PDFTest.pdf\"/}}",
            "pdfReferenceTest");
        PDFViewerMacroPage page = new PDFViewerMacroPage();
        PDFViewerMacro viewer0 = page.getPDFViewer(0);
        assertEquals("PDF file for testing the pdf viewer macro.", viewer0.getText());
    }

    private void createTerminalPageWithPDFAttached(TestUtils setup) throws Exception
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "PDFViewerMacro", "TerminalPageWithPDF");
        setup.deletePage(documentReference);

        ViewPage viewPage = setup.gotoPage(documentReference);
        CreatePagePage cpage = viewPage.createPage();
        cpage.setTerminalPage(true);
        cpage.clickCreate();
        EditPage ep = new EditPage();
        ep.clickSaveAndView();
        setup.attachFile(documentReference, "PDFTest.pdf", getClass().getResourceAsStream("/pdfmacro/PDFTest.pdf"),
            false);
    }

    private void uploadFile(String attachmentName, TestConfiguration testConfiguration)
    {
        String attachmentPath = new File(new File(testConfiguration.getBrowser().getTestResourcesPath(), "pdfmacro"),
            attachmentName).getAbsolutePath();
        AttachmentsPane sourceAttachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        sourceAttachmentsPane.setFileToUpload(attachmentPath);
        sourceAttachmentsPane.waitForUploadToFinish(attachmentName);
    }

    private ViewPage createPage(TestUtils setup, String content, String pageName)
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "PDFViewerMacro", pageName);
        return setup.createPage(documentReference, content);
    }

    private String getMacroContent(String filename)
    {
        try (InputStream inputStream = getClass().getResourceAsStream("/pdfmacro/" + filename)) {
            if (inputStream == null) {
                throw new RuntimeException("Failed to load " + filename + " from resources.");
            }

            return new BufferedReader(new InputStreamReader(inputStream)).lines()
                .filter(line -> !line.trim().startsWith("##")).collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read macro file: " + filename, e);
        }
    }

    private String getModifiedURL(String url, String newString)
    {
        int index = url.indexOf("bin/");
        if (index == -1) {
            return url;
        }
        return url.substring(0, index + 4) + newString;
    }
}

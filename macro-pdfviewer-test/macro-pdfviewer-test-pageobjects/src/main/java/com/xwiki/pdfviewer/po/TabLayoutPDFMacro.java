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
package com.xwiki.pdfviewer.po;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents a PDF Viewer macro that uses the tab layout and provides access to its attributes.
 *
 * @version $Id$
 * @since 2.6.3
 */
public class TabLayoutPDFMacro extends BaseElement
{
    private final WebElement macro;

    private final WebElement tabs;

    public TabLayoutPDFMacro(WebElement macro, WebElement tabs)
    {
        this.macro = macro;
        this.tabs = tabs;
    }

    public void clickTab(int index)
    {
        List<WebElement> tabElements = tabs.findElements(By.cssSelector("li a"));
        tabElements.get(index).click();
    }

    public int getActiveTab()
    {
        List<WebElement> tabElements = tabs.findElements(By.tagName("li"));
        for (int i = 0; i < tabElements.size(); i++) {
            String classes = tabElements.get(i).getAttribute("class");
            if (classes != null && classes.contains("active")) {
                return i;
            }
        }
        return -1;
    }

    public String getTabHref(int index)
    {
        List<WebElement> tabLinks = tabs.findElements(By.cssSelector("li a"));
        return tabLinks.get(index).getAttribute("href");
    }

    public String getTabName(int index)
    {
        List<WebElement> tabLinks = tabs.findElements(By.cssSelector("li a"));
        return tabLinks.get(index).getText();
    }

    public String getText()
    {
        WebElement iframe = getIFrame();
        getDriver().switchTo().frame(iframe);

        try {
            WebElement textLayer = getDriver().findElement(By.cssSelector(".textLayer"));
            return textLayer.getText();
        } finally {
            getDriver().switchTo().defaultContent();
        }
    }

    public String getHeight()
    {
        return getIFrame().getAttribute("height");
    }

    public String getWidth()
    {
        return getIFrame().getAttribute("width").trim();
    }

    public List<WebElement> getTabs()
    {
        return tabs.findElements(By.tagName("li"));
    }

    public List<String> getTabsNames()
    {
        List<String> names = new ArrayList<>();
        for (WebElement tab : getTabs()) {
            WebElement link = tab.findElement(By.tagName("a"));
            names.add(link.getText());
        }
        return names;
    }

    private WebElement getIFrame()
    {
        return macro.findElement(By.cssSelector("iframe.pdfviewer"));
    }
}

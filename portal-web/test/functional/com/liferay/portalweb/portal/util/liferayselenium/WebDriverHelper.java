/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portalweb.portal.util.liferayselenium;

import com.liferay.portal.kernel.util.CharPool;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portalweb.portal.BaseTestCase;
import com.liferay.portalweb.portal.util.TestPropsValues;

import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

import net.jsourcerer.webdriver.jserrorcollector.JavaScriptError;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.WrapsDriver;

/**
 * @author Kenji Heigel
 */
public class WebDriverHelper {

	public static void assertJavaScriptErrors(
			WebDriver webDriver, String ignoreJavaScriptError)
		throws Exception {

		if (!TestPropsValues.TEST_ASSERT_JAVASCRIPT_ERRORS) {
			return;
		}

		String location = getLocation(webDriver);

		if (!location.contains("localhost")) {
			return;
		}

		String pageSource = null;

		try {
			pageSource = webDriver.getPageSource();
		}
		catch (Exception e) {
			WebDriver.TargetLocator targetLocator = webDriver.switchTo();

			targetLocator.window(_defaultWindowHandle);

			pageSource = webDriver.getPageSource();
		}

		if (pageSource.contains(
				"html id=\"feedHandler\" xmlns=" +
					"\"http://www.w3.org/1999/xhtml\"")) {

			return;
		}

		WebElement webElement = getWebElement(webDriver, "//body");

		WrapsDriver wrapsDriver = (WrapsDriver)webElement;

		WebDriver wrappedWebDriver = wrapsDriver.getWrappedDriver();

		List<JavaScriptError> javaScriptErrors = JavaScriptError.readErrors(
			wrappedWebDriver);

		if (!javaScriptErrors.isEmpty()) {
			for (JavaScriptError javaScriptError : javaScriptErrors) {
				String javaScriptErrorValue = javaScriptError.toString();

				System.out.println("JS_ERROR: " + javaScriptErrorValue);

				if (Validator.isNotNull(ignoreJavaScriptError) &&
					javaScriptErrorValue.contains(ignoreJavaScriptError)) {

					continue;
				}

				// LPS-41634

				if (javaScriptErrorValue.contains(
						"TypeError: d.config.doc.defaultView is null")) {

					continue;
				}

				// LPS-41634

				if (javaScriptErrorValue.contains(
						"NS_ERROR_NOT_INITIALIZED:")) {

					continue;
				}

				// LPS-42469

				if (javaScriptErrorValue.contains(
						"Permission denied to access property 'type'")) {

					continue;
				}

				throw new Exception(javaScriptErrorValue);
			}
		}
	}

	public static void click(WebDriver webDriver, String locator) {
		WebElement webElement = getWebElement(webDriver, locator);

		try {
			webElement.click();
		}
		catch (Exception e) {
			if (!webElement.isDisplayed()) {
				scrollWebElementIntoView(webDriver, webElement);
			}

			webElement.click();
		}
	}

	public static String getAttribute(
		WebDriver webDriver, String attributeLocator) {

		int pos = attributeLocator.lastIndexOf(CharPool.AT);

		String locator = attributeLocator.substring(0, pos);

		WebElement webElement = getWebElement(webDriver, locator);

		String attribute = attributeLocator.substring(pos + 1);

		return webElement.getAttribute(attribute);
	}

	public static String getDefaultWindowHandle() {
		return _defaultWindowHandle;
	}

	public static String getEval(WebDriver webDriver, String script) {
		WebElement webElement = getWebElement(webDriver, "//body");

		WrapsDriver wrapsDriver = (WrapsDriver)webElement;

		WebDriver wrappedWebDriver = wrapsDriver.getWrappedDriver();

		JavascriptExecutor javascriptExecutor =
			(JavascriptExecutor)wrappedWebDriver;

		return (String)javascriptExecutor.executeScript(script);
	}

	public static String getLocation(WebDriver webDriver) {
		return webDriver.getCurrentUrl();
	}

	public static String getText(
		WebDriver webDriver, String locator, String timeout) {

		WebElement webElement = getWebElement(webDriver, locator, timeout);

		if (!webElement.isDisplayed()) {
			scrollWebElementIntoView(webDriver, webElement);
		}

		String text = webElement.getText();

		text = text.trim();

		return text.replace("\n", " ");
	}

	public static boolean isElementPresent(
		WebDriver webDriver, String locator) {

		List<WebElement> webElements = getWebElements(webDriver, locator, "1");

		return !webElements.isEmpty();
	}

	public static boolean isVisible(WebDriver webDriver, String locator) {
		WebElement webElement = getWebElement(webDriver, locator, "1");

		if (!webElement.isDisplayed()) {
			scrollWebElementIntoView(webDriver, webElement);
		}

		return webElement.isDisplayed();
	}

	public static void makeVisible(WebDriver webDriver, String locator) {
		WebElement bodyWebElement = getWebElement(webDriver, "//body");

		WrapsDriver wrapsDriver = (WrapsDriver)bodyWebElement;

		WebDriver wrappedWebDriver = wrapsDriver.getWrappedDriver();

		JavascriptExecutor javascriptExecutor =
			(JavascriptExecutor)wrappedWebDriver;

		StringBundler sb = new StringBundler(4);

		sb.append("var element = arguments[0];");
		sb.append("element.style.cssText = 'display:inline !important';");
		sb.append("element.style.overflow = 'visible';");
		sb.append("element.style.minHeight = '1px';");
		sb.append("element.style.minWidth = '1px';");
		sb.append("element.style.opacity = '1';");
		sb.append("element.style.visibility = 'visible';");

		WebElement locatorWebElement = getWebElement(webDriver, locator);

		javascriptExecutor.executeScript(sb.toString(), locatorWebElement);
	}

	public static void open(WebDriver webDriver, String url) {
		String targetURL = "";

		if (url.startsWith("/")) {
			targetURL = TestPropsValues.PORTAL_URL + url;
		}
		else {
			targetURL = url;
		}

		for (int second = 0;; second++) {
			if (second >= TestPropsValues.TIMEOUT_IMPLICIT_WAIT) {
				break;
			}

			try {
				webDriver.get(targetURL);

				if (TestPropsValues.BROWSER_TYPE.equals("*iehta") ||
					TestPropsValues.BROWSER_TYPE.equals("*iexplore")) {

					refresh(webDriver);
				}

				if (targetURL.equals(getLocation(webDriver))) {
					break;
				}

				Thread.sleep(1000);
			}
			catch (Exception e) {
			}
		}
	}

	public static void refresh(WebDriver webDriver) {
		WebDriver.Navigation navigation = webDriver.navigate();

		navigation.refresh();
	}

	public static void selectFrame(WebDriver webDriver, String locator) {
		WebDriver.TargetLocator targetLocator = webDriver.switchTo();

		if (locator.equals("relative=parent")) {
			targetLocator.window(_defaultWindowHandle);

			if (!_frameWebElements.isEmpty()) {
				_frameWebElements.pop();

				if (!_frameWebElements.isEmpty()) {
					targetLocator.frame(_frameWebElements.peek());
				}
			}
		}
		else if (locator.equals("relative=top")) {
			_frameWebElements = new Stack<WebElement>();

			targetLocator.window(_defaultWindowHandle);
		}
		else {
			_frameWebElements.push(getWebElement(webDriver, locator));

			targetLocator.frame(_frameWebElements.peek());
		}
	}

	public static void selectWindow(WebDriver webDriver, String windowID) {
		Set<String> windowHandles = webDriver.getWindowHandles();

		if (windowID.equals("name=undefined")) {
			String title = webDriver.getTitle();

			for (String windowHandle : windowHandles) {
				WebDriver.TargetLocator targetLocator = webDriver.switchTo();

				targetLocator.window(windowHandle);

				if (!title.equals(webDriver.getTitle())) {
					return;
				}
			}

			BaseTestCase.fail(
				"Unable to find the window ID \"" + windowID + "\"");
		}
		else if (windowID.equals("null")) {
			WebDriver.TargetLocator targetLocator = webDriver.switchTo();

			targetLocator.window(_defaultWindowHandle);
		}
		else {
			String targetWindowTitle = windowID;

			if (targetWindowTitle.startsWith("title=")) {
				targetWindowTitle = targetWindowTitle.substring(6);
			}

			for (String windowHandle : windowHandles) {
				WebDriver.TargetLocator targetLocator = webDriver.switchTo();

				targetLocator.window(windowHandle);

				if (targetWindowTitle.equals(webDriver.getTitle())) {
					return;
				}
			}

			BaseTestCase.fail(
				"Unable to find the window ID \"" + windowID + "\"");
		}
	}

	public static void setDefaultTimeoutImplicit(WebDriver webDriver) {
		int timeout = TestPropsValues.TIMEOUT_IMPLICIT_WAIT * 1000;

		setTimeoutImplicit(webDriver, String.valueOf(timeout));
	}

	public static void setDefaultWindowHandle(String defaultWindowHandle) {
		_defaultWindowHandle = defaultWindowHandle;
	}

	public static void setTimeoutImplicit(WebDriver webDriver, String timeout) {
		WebDriver.Options options = webDriver.manage();

		WebDriver.Timeouts timeouts = options.timeouts();

		timeouts.implicitlyWait(
			GetterUtil.getInteger(timeout), TimeUnit.MILLISECONDS);
	}

	public static void type(WebDriver webDriver, String locator, String value) {
		WebElement webElement = getWebElement(webDriver, locator);

		if (!webElement.isEnabled()) {
			return;
		}

		webElement.clear();

		webElement.sendKeys(value);
	}

	protected static WebElement getWebElement(
		WebDriver webDriver, String locator) {

		return getWebElement(webDriver, locator, null);
	}

	protected static WebElement getWebElement(
		WebDriver webDriver, String locator, String timeout) {

		List<WebElement> webElements = getWebElements(
			webDriver, locator, timeout);

		if (!webElements.isEmpty()) {
			return webElements.get(0);
		}

		return null;
	}

	protected static List<WebElement> getWebElements(
		WebDriver webDriver, String locator) {

		return getWebElements(webDriver, locator, null);
	}

	protected static List<WebElement> getWebElements(
		WebDriver webDriver, String locator, String timeout) {

		if (timeout != null) {
			setTimeoutImplicit(webDriver, timeout);
		}

		try {
			if (locator.startsWith("//")) {
				return webDriver.findElements(By.xpath(locator));
			}
			else if (locator.startsWith("class=")) {
				locator = locator.substring(6);

				return webDriver.findElements(By.className(locator));
			}
			else if (locator.startsWith("css=")) {
				locator = locator.substring(4);

				return webDriver.findElements(By.cssSelector(locator));
			}
			else if (locator.startsWith("link=")) {
				locator = locator.substring(5);

				return webDriver.findElements(By.linkText(locator));
			}
			else if (locator.startsWith("name=")) {
				locator = locator.substring(5);

				return webDriver.findElements(By.name(locator));
			}
			else if (locator.startsWith("tag=")) {
				locator = locator.substring(4);

				return webDriver.findElements(By.tagName(locator));
			}
			else if (locator.startsWith("xpath=") ||
					 locator.startsWith("xPath=")) {

				locator = locator.substring(6);

				return webDriver.findElements(By.xpath(locator));
			}
			else {
				return webDriver.findElements(By.id(locator));
			}
		}
		finally {
			if (timeout != null) {
				setDefaultTimeoutImplicit(webDriver);
			}
		}
	}

	protected static void scrollWebElementIntoView(
		WebDriver webDriver, WebElement webElement) {

		WrapsDriver wrapsDriver = (WrapsDriver)webElement;

		WebDriver wrappedWebDriver = wrapsDriver.getWrappedDriver();

		JavascriptExecutor javascriptExecutor =
			(JavascriptExecutor)wrappedWebDriver;

		javascriptExecutor.executeScript(
			"arguments[0].scrollIntoView();", webElement);
	}

	private static String _defaultWindowHandle;
	private static Stack<WebElement> _frameWebElements =
		new Stack<WebElement>();

}
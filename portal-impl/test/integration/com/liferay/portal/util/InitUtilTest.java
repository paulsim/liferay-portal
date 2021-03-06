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

package com.liferay.portal.util;

import com.liferay.portal.kernel.util.PropsKeys;

import org.junit.Test;

/**
 * @author André de Oliveira
 */
public class InitUtilTest {

	@Test
	public void testBaseSeleniumTestCaseSpringConfigs() {
		String springConfigs = PropsUtil.get(PropsKeys.SPRING_CONFIGS);

		try {
			String baseSeleniumTestCaseSpringConfigs =
				"META-INF/management-spring.xml,META-INF/util-spring.xml";

			PropsUtil.set(
				PropsKeys.SPRING_CONFIGS, baseSeleniumTestCaseSpringConfigs);

			InitUtil.initWithSpringAndModuleFramework();
		}
		finally {
			PropsUtil.set(PropsKeys.SPRING_CONFIGS, springConfigs);
		}
	}

}
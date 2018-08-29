package com.sun.glass.ui.monocle;

import java.lang.reflect.Field;

import javafx.application.Application;

public class MonocleLauncher {

	//---------------------------------------------------------------------------------------------
	// CONSTANTS.
	//---------------------------------------------------------------------------------------------

	private static final String PLATFORM_FACTORY_CLASS = "com.sun.glass.ui.PlatformFactory";
	private static final String PLATFORM_FACTORY_MONOCLE_IMPL = "com.sun.glass.ui.monocle.MonoclePlatformFactory";

	private static final String NATIVE_PLATFORM_FACTORY_CLASS = "com.sun.glass.ui.monocle.NativePlatformFactory";
	private static final String NATIVE_PLATFORM_HEADLESS_IMPL = "com.sun.glass.ui.monocle.HeadlessPlatform";

	//---------------------------------------------------------------------------------------------
	// METHODS.
	//---------------------------------------------------------------------------------------------

	public void launch(Class<? extends Application> appClass, String... appArgs) {
		if (appArgs[2].equalsIgnoreCase("true")) {
			initMonocleHeadless();
		}
		Application.launch(appClass, appArgs);
	}

	//---------------------------------------------------------------------------------------------
	// PRIVATE METHODS.
	//---------------------------------------------------------------------------------------------

	private void initMonocleHeadless() {
		try {
			assignMonoclePlatform();
			assignHeadlessPlatform();
		} catch (final Exception exception) {
			throw new RuntimeException(exception);
		}
	}

	private void assignMonoclePlatform() throws Exception {
		final Class<?> platformFactoryClass = Class.forName(PLATFORM_FACTORY_CLASS);
		final Object platformFactoryImpl = Class.forName(PLATFORM_FACTORY_MONOCLE_IMPL).newInstance();
		assignPrivateStaticField(platformFactoryClass, "instance", platformFactoryImpl);
	}

	private void assignHeadlessPlatform() throws Exception {
		final Class<?> nativePlatformFactoryClass = Class.forName(NATIVE_PLATFORM_FACTORY_CLASS);
		final Object nativePlatformImpl = Class.forName(NATIVE_PLATFORM_HEADLESS_IMPL).newInstance();
		assignPrivateStaticField(nativePlatformFactoryClass, "platform", nativePlatformImpl);
	}

	private void assignPrivateStaticField(Class<?> cls, String name, Object value) throws Exception {
		final Field field = cls.getDeclaredField(name);
		field.setAccessible(true);
		field.set(cls, value);
		field.setAccessible(false);
	}

}
package com.sun.glass.ui.monocle;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import javafx.application.Application;

public class MonocleLauncher {

	public void launch(Class<? extends Application> appClass, String... appArgs) {
		if (appArgs[2].equalsIgnoreCase("true")) {
			initMonocleHeadless();
		}
		Application.launch(appClass, appArgs);
	}

	private void initMonocleHeadless() {
		try {
			assignMonoclePlatform();
			assignHeadlessPlatform();
		} catch (final Exception exception) {
			throw new RuntimeException(exception);
		}
	}

	private void assignMonoclePlatform() throws Exception {
		final Class<?> platformFactoryClass = Class.forName("com.sun.glass.ui.PlatformFactory");
		final Object platformFactoryImpl = Class.forName("com.sun.glass.ui.monocle.MonoclePlatformFactory").getDeclaredConstructor().newInstance();
		assignPrivateStaticField(platformFactoryClass, "instance", platformFactoryImpl);
	}

	private void assignHeadlessPlatform() throws Exception {
		final Class<?> nativePlatformFactoryClass = Class.forName("com.sun.glass.ui.monocle.NativePlatformFactory");
		try {
			final Constructor<?> nativePlatformCtor = Class.forName("com.sun.glass.ui.monocle.HeadlessPlatform").getDeclaredConstructor();
			nativePlatformCtor.setAccessible(true);
			assignPrivateStaticField(nativePlatformFactoryClass, "platform", nativePlatformCtor.newInstance());
		} catch (final ClassNotFoundException exception) {
			// Before Java 8u40 HeadlessPlatform was located inside of a "headless" package.
			final Constructor<?> nativePlatformCtor = Class.forName("com.sun.glass.ui.monocle.headless.HeadlessPlatform").getDeclaredConstructor();
			nativePlatformCtor.setAccessible(true);
			assignPrivateStaticField(nativePlatformFactoryClass, "platform", nativePlatformCtor.newInstance());
		}
	}

	private void assignPrivateStaticField(Class<?> cls, String name, Object value) throws Exception {
		final Field field = cls.getDeclaredField(name);
		field.setAccessible(true);
		field.set(cls, value);
		field.setAccessible(false);
	}

}
/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.glass.ui.monocle;

import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.CommonDialogs.ExtensionFilter;
import com.sun.glass.ui.CommonDialogs.FileChooserResult;
import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Robot;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.Size;
import com.sun.glass.ui.Timer;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;

import javafx.scene.control.TextInputDialog;

public final class MonocleApplication extends Application {

	private final NativePlatform platform = NativePlatformFactory.getNativePlatform();
	private final RunnableProcessor runnableProcessor = platform.getRunnableProcessor();

	/** Bit to indicate that a device has touch support */
	private static final int DEVICE_TOUCH = 0;
	/** Bit to indicate that a device has multitouch support */
	private static final int DEVICE_MULTITOUCH = 1;
	/** Bit to indicate that a device has relative motion pointer support */
	private static final int DEVICE_POINTER = 2;
	/** Bit to indicate that a device has arrow keys and a select key */
	private static final int DEVICE_5WAY = 3;
	/** Bit to indicate that a device has a full PC keyboard */
	private static final int DEVICE_PC_KEYBOARD = 4;
	/** Largest bit used in device capability bitmasks */
	private static final int DEVICE_MAX = 4;
	/** A running count of the numbers of devices with each device capability */
	private final int[] deviceFlags = new int[DEVICE_MAX + 1];
	private Thread shutdownHookThread;
	private final Runnable renderEndNotifier = () -> platform.getScreen().swapBuffers();

	MonocleApplication() {
	}

	@Override
	protected void runLoop(Runnable launchable) {
		runnableProcessor.invokeLater(launchable);
		final long stackSize = AccessController.doPrivileged((PrivilegedAction<Long>) () -> Long.getLong("monocle.stackSize", 0));
		final Thread t = new Thread(new ThreadGroup("Event"), runnableProcessor, "Event Thread", stackSize);
		setEventThread(t);
		t.start();
		shutdownHookThread = new Thread("Monocle shutdown hook") {
			@Override
			public void run() {
				platform.shutdown();
			}
		};
		Runtime.getRuntime().addShutdownHook(shutdownHookThread);
	}

	@Override
	protected void _invokeAndWait(Runnable runnable) {
		runnableProcessor.invokeAndWait(runnable);
	}

	@Override
	protected void _invokeLater(Runnable runnable) {
		runnableProcessor.invokeLater(runnable);
	}

	@Override
	protected Object _enterNestedEventLoop() {
		return runnableProcessor.enterNestedEventLoop();
	}

	@Override
	protected void _leaveNestedEventLoop(Object retValue) {
		runnableProcessor.leaveNestedEventLoop(retValue);
	}

	@Override
	public Window createWindow(Window owner, Screen screen, int styleMask) {
		return new MonocleWindow(owner, screen, styleMask);
	}

	@Override
	public Window createWindow(long parent) {
		return new MonocleWindow(parent);
	}

	@Override
	public View createView() {
		return new MonocleView();
	}

	@Override
	public Cursor createCursor(int type) {
		return new MonocleCursor(type);
	}

	@Override
	public Cursor createCursor(int x, int y, Pixels pixels) {
		return new MonocleCursor(x, y, pixels);
	}

	@Override
	protected void staticCursor_setVisible(boolean visible) {
		// Only needed by superclass
	}

	@Override
	protected Size staticCursor_getBestSize(int width, int height) {
		// Only needed by superclass
		return new Size(10, 10);
	}

	@Override
	public Pixels createPixels(int width, int height, ByteBuffer data) {
		return new MonoclePixels(width, height, data);
	}

	@Override
	public Pixels createPixels(int width, int height, IntBuffer data) {
		return new MonoclePixels(width, height, data);
	}

	//  @Override
	public Pixels createPixels(int width, int height, IntBuffer data, float scalex, float scaley) {
		return new MonoclePixels(width, height, data);//Java 8 and Java 9 constructors conflict--work around: use a constructor they both have
	}

	//  @Override
	@Override
	public Pixels createPixels(int width, int height, IntBuffer data, float scale) {
		return new MonoclePixels(width, height, data);//Java 8 and Java 9 constructors conflict--work around: use a constructor they both have
	}

	@Override
	protected int staticPixels_getNativeFormat() {
		return platform.getScreen().getNativeFormat();
	}

	@Override
	protected double staticScreen_getVideoRefreshPeriod() {
		return 0.0;
	}

	@Override
	protected Screen[] staticScreen_getScreens() {
		Screen screen = null;
		try {
			final NativeScreen ns = platform.getScreen();
			final AtomicBoolean java9 = new AtomicBoolean();
			final Constructor c = AccessController.doPrivileged((PrivilegedAction<Constructor>) () -> {
				try {
					final Constructor c1 = Screen.class.getDeclaredConstructor(Long.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE,
							Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Float.TYPE);
					c1.setAccessible(true);
					return c1;
				} catch (final Exception e) {
					try {
						final Constructor c2 = Screen.class.getDeclaredConstructor(Long.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE,
								Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE,
								Integer.TYPE, Integer.TYPE, Integer.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE);
						c2.setAccessible(true);
						java9.set(true);
						return c2;
					} catch (final Exception e2) {
						e2.printStackTrace();
						return null;
					}
				}
			});
			if (c != null) {
				if (java9.get()) {
					screen = (Screen) c.newInstance(1l, // dummy native pointer;
							ns.getDepth(), 0, 0, ns.getWidth(), ns.getHeight(), 0, 0, ns.getWidth(), ns.getHeight(), 0, 0, ns.getWidth(),
							ns.getHeight(), ns.getDPI(), ns.getDPI(), ns.getScale(), ns.getScale(), ns.getScale(), ns.getScale());
				} else {
					screen = (Screen) c.newInstance(1l, // dummy native pointer;
							ns.getDepth(), 0, 0, ns.getWidth(), ns.getHeight(), 0, 0, ns.getWidth(), ns.getHeight(), ns.getDPI(), ns.getDPI(),
							ns.getScale());
				}
			}
		} catch (final Throwable t) {
			t.printStackTrace();
		}
		return new Screen[] { screen };
	}

	@Override
	public Timer createTimer(Runnable runnable) {
		return new MonocleTimer(runnable);
	}

	@Override
	protected int staticTimer_getMinPeriod() {
		return MonocleTimer.getMinPeriod_impl();
	}

	@Override
	protected int staticTimer_getMaxPeriod() {
		return MonocleTimer.getMaxPeriod_impl();
	}

	@Override
	public boolean hasWindowManager() {
		return false;
	}

	@Override
	protected FileChooserResult staticCommonDialogs_showFileChooser(Window owner, String folder, String filename, String title, int type,
			boolean multipleMode, ExtensionFilter[] extensionFilters, int defaultFilterIndex) {
		final TextInputDialog dialog = new TextInputDialog();
		dialog.showAndWait();
		File[] files;
		final String filePaths = dialog.getEditor().getText();
		if (filePaths != null && !filePaths.isEmpty()) {
			final String[] filePathParts = filePaths.split("\t");
			files = new File[filePathParts.length];
			for (int i = 0; i < filePathParts.length; i++) {
				files[i] = new File(filePathParts[i]);
			}
		} else {
			files = new File[0];
		}
		return new FileChooserResult(Arrays.asList(files), null);
	}

	@Override
	protected File staticCommonDialogs_showFolderChooser(Window owner, String folder, String title) {
		Thread.dumpStack();
		throw new UnsupportedOperationException();
	}

	@Override
	protected long staticView_getMultiClickTime() {
		return MonocleView._getMultiClickTime();
	}

	@Override
	protected int staticView_getMultiClickMaxX() {
		return MonocleView._getMultiClickMaxX();
	}

	@Override
	protected int staticView_getMultiClickMaxY() {
		return MonocleView._getMultiClickMaxY();
	}

	@Override
	protected boolean _supportsTransparentWindows() {
		return true;
	}

	@Override
	protected boolean _supportsUnifiedWindows() {
		return false;
	}

	@Override
	public boolean hasTwoLevelFocus() {
		return deviceFlags[DEVICE_PC_KEYBOARD] == 0 && deviceFlags[DEVICE_5WAY] > 0;
	}

	@Override
	public boolean hasVirtualKeyboard() {
		return deviceFlags[DEVICE_PC_KEYBOARD] == 0 && deviceFlags[DEVICE_TOUCH] > 0;
	}

	@Override
	public boolean hasTouch() {
		return deviceFlags[DEVICE_TOUCH] > 0;
	}

	@Override
	public boolean hasMultiTouch() {
		return deviceFlags[DEVICE_MULTITOUCH] > 0;
	}

	@Override
	public boolean hasPointer() {
		return deviceFlags[DEVICE_POINTER] > 0;
	}

	//@Override
	@Override
	public void notifyRenderingFinished() {
		invokeLater(renderEndNotifier);
	}

	@Override
	protected void finishTerminating() {
		//if this method is getting called, we don't need the shutdown hook
		Runtime.getRuntime().removeShutdownHook(shutdownHookThread);
		setEventThread(null);
		platform.shutdown();
		super.finishTerminating();
	}

	void enterDnDEventLoop() {
		_enterNestedEventLoop();
	}

	void leaveDndEventLoop() {
		_leaveNestedEventLoop(null);
	}

	@Override
	public Robot createRobot() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected int _getKeyCodeForChar(char c) {
		// TODO Auto-generated method stub
		return 0;
	}

}

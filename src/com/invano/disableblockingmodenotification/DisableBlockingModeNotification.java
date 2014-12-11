package com.invano.disableblockingmodenotification;

import android.content.Context;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XposedHelpers;


public class DisableBlockingModeNotification implements IXposedHookLoadPackage {

	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
		
		if (!lpparam.packageName.equals("com.android.settings"))
			return;
		
		XposedHelpers.findAndHookMethod(
				"com.android.settings.dormantmode.DormantModeNotiReceiver",
				lpparam.classLoader,
				"notificationCreate",
				Context.class,
				XC_MethodReplacement.DO_NOTHING
				);
	}
}


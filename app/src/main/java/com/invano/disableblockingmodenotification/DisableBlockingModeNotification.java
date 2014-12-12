package com.invano.disableblockingmodenotification;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XposedHelpers;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;


public class DisableBlockingModeNotification implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    static XSharedPreferences prefs;
    public static String THIS_PKG_NAME = DisableBlockingModeNotification.class.getPackage().getName();

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

        if (!lpparam.packageName.equals("com.android.settings"))
            return;

        if (prefs.hasFileChanged())
            prefs.reload();

        final Class<?> clDM = XposedHelpers.findClass("com.android.settings.dormantmode.DormantModeNotiReceiver", lpparam.classLoader);

        if (prefs.getBoolean(Common.PREF_BLOCK_KEY, false)) {
            findAndHookMethod(clDM, "notificationCreate", Context.class, XC_MethodReplacement.DO_NOTHING);
        }
        else {

            final Class<?> clDU = XposedHelpers.findClass("com.android.settings.dormantmode.DormantModeUtils", lpparam.classLoader);

            findAndHookMethod(clDM, "onReceive", Context.class, Intent.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {

                    Context context = (Context) methodHookParam.args[0];
                    Intent intent = (Intent) methodHookParam.args[1];

                    String str = intent.getAction();
                    if (("android.settings.DORMANTMODE_SWITCH_CHANGED".equals(str)) ||
                            ("android.intent.action.BOOT_COMPLETED".equals(str)) ||
                            ("android.settings.DORMANTMODE_CHANGED".equals(str)) ||
                            ("android.intent.action.TIME_SET".equals(str)) ||
                            ("android.intent.action.LOCALE_CHANGED".equals(str))) {

                        boolean optionsDisabled = (boolean) callStaticMethod(clDU, "isAllOptionDisabled", context);

                        if ((1 == Settings.System.getInt(context.getContentResolver(), "dormant_switch_onoff", 0)) &&
                                (!optionsDisabled)) {

                            boolean isTime = (boolean) callMethod(methodHookParam.thisObject, "isSetTime", context);

                            if (1 == Settings.System.getInt(context.getContentResolver(), "dormant_always", 0) || isTime) {
                                callMethod(methodHookParam.thisObject, "notificationCreate", context);
                            }
                            else {
                                callMethod(methodHookParam.thisObject, "setOnGoingAlarm", context);
                                callMethod(methodHookParam.thisObject, "notificationClear", context);
                            }
                        }
                        else {
                            callMethod(methodHookParam.thisObject, "cancelOnGoingAlarm", context);
                            callMethod(methodHookParam.thisObject, "notificationClear", context);
                        }
                    }
                    else if ("android.settings.DORMANTMODE_ONGOING_ALARM_START".equals(str)) {
                        callMethod(methodHookParam.thisObject, "notificationCreate", context);

                    }
                    else if (("android.settings.DORMANTMODE_ONGOING_ALARM_END".equals(str))) {
                        callMethod(methodHookParam.thisObject, "notificationClear", context);
                    }

                    while (!"android.settings.DORMANTMODE_ACTIVITY_LAUNCH".equals(str))
                    {
                        return null;
                    }
                    Intent localIntent = new Intent();
                    localIntent.setAction("android.intent.action.MAIN");
                    localIntent.setClassName("com.android.settings", "com.android.settings.Settings$DormantmodeSettingsActivity")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    context.startActivity(localIntent);
                    return null;
                }
            });
        }

    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        prefs = new XSharedPreferences(THIS_PKG_NAME, THIS_PKG_NAME);
    }
}


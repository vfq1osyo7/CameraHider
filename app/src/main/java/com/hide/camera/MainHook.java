package com.hide.camera;

import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraCharacteristics;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "CameraHider";

    // 目标包名：抖音
    private static final String TARGET_PACKAGE = "com.ss.android.ugc.aweme";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        Log.d(TAG, "已注入抖音进程，开始 Hook 相机接口");

        // Hook 1: getCameraIdList — 过滤虚拟相机 ID
        XposedHelpers.findAndHookMethod(
            CameraManager.class,
            "getCameraIdList",
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String[] original = (String[]) param.getResult();
                    if (original == null) return;

                    List<String> filtered = new ArrayList<>();
                    CameraManager manager = (CameraManager) param.thisObject;

                    for (String id : original) {
                        try {
                            CameraCharacteristics chars = manager.getCameraCharacteristics(id);
                            Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
                            // 只保留前置(0)和后置(1)，过滤掉其他（虚拟相机通常是2+或特殊值）
                            if (facing != null && (facing == CameraCharacteristics.LENS_FACING_FRONT
                                    || facing == CameraCharacteristics.LENS_FACING_BACK)) {
                                filtered.add(id);
                                Log.d(TAG, "保留相机 ID=" + id + " facing=" + facing);
                            } else {
                                Log.d(TAG, "过滤虚拟相机 ID=" + id + " facing=" + facing);
                            }
                        } catch (Exception e) {
                            // 读取特征失败的 ID 也过滤掉
                            Log.d(TAG, "过滤异常相机 ID=" + id);
                        }
                    }

                    param.setResult(filtered.toArray(new String[0]));
                    Log.d(TAG, "相机列表过滤完成，原始=" + original.length + " 过滤后=" + filtered.size());
                }
            }
        );

        // Hook 2: getCameraIdListNoLazy — 部分系统会走这个内部方法
        try {
            XposedHelpers.findAndHookMethod(
                CameraManager.class,
                "getCameraIdListNoLazy",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String[] original = (String[]) param.getResult();
                        if (original == null) return;

                        List<String> filtered = new ArrayList<>();
                        CameraManager manager = (CameraManager) param.thisObject;

                        for (String id : original) {
                            try {
                                CameraCharacteristics chars = manager.getCameraCharacteristics(id);
                                Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
                                if (facing != null && (facing == CameraCharacteristics.LENS_FACING_FRONT
                                        || facing == CameraCharacteristics.LENS_FACING_BACK)) {
                                    filtered.add(id);
                                }
                            } catch (Exception e) {
                                // 过滤异常 ID
                            }
                        }
                        param.setResult(filtered.toArray(new String[0]));
                    }
                }
            );
        } catch (Throwable ignored) {
            // 该方法不存在时忽略
        }

        Log.d(TAG, "相机 Hook 注册完成");
    }
}

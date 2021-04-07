/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xys.libzxing.zxing.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.zxing.Result;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.xys.libzxing.R;
import com.xys.libzxing.zxing.camera.CameraManager;
import com.xys.libzxing.zxing.decode.DecodeThread;
import com.xys.libzxing.zxing.utils.BeepManager;
import com.xys.libzxing.zxing.utils.CaptureActivityHandler;
import com.xys.libzxing.zxing.utils.InactivityTimer;
import com.xys.libzxing.zxing.utils.OnPhotoTakeListener;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * This activity opens the camera and does the actual scanning on a background
 * thread. It draws a viewfinder to help the user place the barcode correctly,
 * shows feedback as the image processing is happening, and then overlays the
 * results when a scan is successful.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class CaptureActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final String TAG = CaptureActivity.class.getSimpleName();

    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;

    private SurfaceView scanPreview = null;
    private RelativeLayout scanContainer;
    private RelativeLayout scanCropView;
    private ImageView scanLine, ivBack, ivCamera, ivGallery;
    public static String GALLERY = "gallery";
    public static String BACK = "BACK";
    public static String CAMERA = "camera";
    private Rect mCropRect = null;
    private boolean isHasSurface = false;
    private RelativeLayout.LayoutParams scanViewLayoutParams;
    private float bottomMargin = 300;
    private int animationMinus = 2;
    private int animationCount = 100;
    private int scanViewHeight = 0;
    View indicator;


    public Handler getHandler() {
        return handler;
    }

    private Handler animationHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (bottomMargin <= 0) {
                bottomMargin = scanViewHeight;
            }
            float scale = scanViewHeight / animationCount;
            bottomMargin = bottomMargin - scale;
            scanViewLayoutParams.bottomMargin = (int) bottomMargin;
            scanLine.setLayoutParams(scanViewLayoutParams);
            animationHandler.sendEmptyMessageDelayed(0, animationMinus * 10);
            return false;
        }
    });

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    public void initview() {
        scanPreview = (SurfaceView) findViewById(R.id.capture_preview);
        scanContainer = (RelativeLayout) findViewById(R.id.capture_container);
        scanCropView = (RelativeLayout) findViewById(R.id.capture_crop_view);
        scanLine = (ImageView) findViewById(R.id.capture_scan_line);

        ivBack = (ImageView) findViewById(R.id.iv_back);
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("result", BACK);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
        inactivityTimer = new InactivityTimer(this);
        beepManager = new BeepManager(this);

        ivGallery = findViewById(R.id.iv_gallery);
        ivGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("result", GALLERY);
                setResult(RESULT_OK, resultIntent);
                CaptureActivity.this.finish();
            }
        });
        ivCamera = findViewById(R.id.iv_camera);
        ivCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraManager.takePhoto();
            }
        });


        List<String> list = getIntent().getStringArrayListExtra("list");
//        list.add("身份证");
//        list.add("银行卡");
//        list.add("名片");
        indicator = findViewById(R.id.indicator);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int indicatorWidth = screenWidth / list.size();
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) indicator.getLayoutParams();
        layoutParams.width = indicatorWidth;
        indicator.setLayoutParams(layoutParams);

        ViewPager view_pager = findViewById(R.id.view_pager);
        view_pager.setAdapter(new MyPagerAdapter(this, list));
        TabLayout tablayout = findViewById(R.id.tab);
        tablayout.setupWithViewPager(view_pager);

        tablayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                indicator.setTranslationX(tab.view.getX());
                handler.setIsDecodingMode(tab.getPosition() == 0);
                ivCamera.setVisibility(tab.getPosition() == 0 ? View.INVISIBLE : View.VISIBLE);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_capture);
        Disposable d = new RxPermissions(this).request(Manifest.permission.CAMERA).subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {
                if (aBoolean) {
                    initview();
                } else {
                    Toast.makeText(CaptureActivity.this, "请赋予相机权限!", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: ");
        Intent resultIntent = new Intent();
        resultIntent.putExtra("result", BACK);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    public int dip2px(float dip) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (dip * scale + 0.5f);
    }

    private void initScanView() {

        scanLine.post(new Runnable() {
            @Override
            public void run() {
                scanViewHeight= dip2px(300);

                bottomMargin= scanViewHeight;
                scanViewLayoutParams = (RelativeLayout.LayoutParams) scanLine.getLayoutParams();
                animationHandler.sendEmptyMessageDelayed(0,animationMinus*10);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        initScanView();

        // CameraManager must be initialized here, not in onCreate(). This is
        // necessary because we don't
        // want to open the camera driver and measure the screen size if we're
        // going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the
        // wrong size and partially
        // off screen.
        cameraManager = new CameraManager(getApplication());
        cameraManager.setOnPhotoTakeListener(new OnPhotoTakeListener() {
            @Override
            public void onPhotoTake(String path) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("result", CAMERA);
                resultIntent.putExtra("photoPath", path);
                setResult(RESULT_OK, resultIntent);
                CaptureActivity.this.finish();
            }
        });
        handler = null;
        if (scanPreview != null) {
            if (isHasSurface) {
                // The activity was paused but not stopped, so the surface still
                // exists. Therefore
                // surfaceCreated() won't be called, so init the camera here.
                initCamera(scanPreview.getHolder());
            } else {
                // Install the callback and wait for surfaceCreated() to init the
                // camera.
                scanPreview.getHolder().addCallback(CaptureActivity.this);
            }
        }


        if (inactivityTimer != null) {
            inactivityTimer.onResume();
        }


    }

    @Override
    protected void onPause() {
        if (animationHandler != null) {
            animationHandler.removeCallbacksAndMessages(this);
            animationHandler.removeMessages(0);
        }

        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        if (inactivityTimer != null) {
            inactivityTimer.onPause();
            beepManager.close();
            cameraManager.closeDriver();
        }

        if (!isHasSurface&&scanPreview!=null) {
            scanPreview.getHolder().removeCallback(CaptureActivity.this);
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (inactivityTimer!=null){
            inactivityTimer.shutdown();

        }
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!isHasSurface) {
            isHasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isHasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    /**
     * A valid barcode has been found, so give an indication of success and show
     * the results.
     *
     * @param rawResult The contents of the barcode.
     * @param bundle    The extras
     */
    public void handleDecode(Result rawResult, Bundle bundle) {
        inactivityTimer.onActivity();
        beepManager.playBeepSoundAndVibrate();

        Intent resultIntent = new Intent();
        bundle.putInt("width", mCropRect.width());
        bundle.putInt("height", mCropRect.height());
        bundle.putString("result", rawResult.getText());

        resultIntent.putExtras(bundle);
        this.setResult(RESULT_OK, resultIntent);
        CaptureActivity.this.finish();

    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            if (handler == null) {
                handler = new CaptureActivityHandler(this, cameraManager, DecodeThread.ALL_MODE);
            }
            initCrop();
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {

            displayFrameworkBugMessageAndExit();
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        Toast.makeText(this, "相机出现问题,请重试", Toast.LENGTH_SHORT).show();
    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
        }
    }

    public Rect getCropRect() {
        return mCropRect;
    }

    /**
     * 初始化截取的矩形区域
     */
    private void initCrop() {
        int cameraWidth = cameraManager.getCameraResolution().y;
        int cameraHeight = cameraManager.getCameraResolution().x;

        /** 获取布局中扫描框的位置信息 */
        int[] location = new int[2];
        scanCropView.getLocationInWindow(location);

        int cropLeft = location[0];
        int cropTop = location[1] - getStatusBarHeight();

        int cropWidth = scanCropView.getWidth();
        int cropHeight = scanCropView.getHeight();

        /** 获取布局容器的宽高 */
        int containerWidth = scanContainer.getWidth();
        int containerHeight = scanContainer.getHeight();

        /** 计算最终截取的矩形的左上角顶点x坐标 */
        int x = cropLeft * cameraWidth / containerWidth;
        /** 计算最终截取的矩形的左上角顶点y坐标 */
        int y = cropTop * cameraHeight / containerHeight;

        /** 计算最终截取的矩形的宽度 */
        int width = cropWidth * cameraWidth / containerWidth;
        /** 计算最终截取的矩形的高度 */
        int height = cropHeight * cameraHeight / containerHeight;

        /** 生成最终的截取的矩形 */
        mCropRect = new Rect(0, 0, width, height);
    }

    private int getStatusBarHeight() {
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = Integer.parseInt(field.get(obj).toString());
            return getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
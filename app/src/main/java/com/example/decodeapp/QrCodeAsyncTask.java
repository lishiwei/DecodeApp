package com.example.decodeapp;

import android.app.Activity;
import android.os.AsyncTask;

import com.xys.libzxing.zxing.decode.QRCodeParseUtils;

import java.lang.ref.WeakReference;

/**
     * AsyncTask 静态内部类，防止内存泄漏
     */
  public    class QrCodeAsyncTask extends AsyncTask<String, Integer, String> {
        private WeakReference<Activity> mWeakReference;
        private String path;

        public QrCodeAsyncTask(Activity activity, String path) {
            mWeakReference = new WeakReference<>(activity);
            this.path = path;
        }

        @Override
        protected String doInBackground(String... strings) {
            // 解析二维码/条码
            return QRCodeParseUtils.syncDecodeQRCode(path);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //识别出图片二维码/条码，内容为s
           MainActivity activity = (MainActivity) mWeakReference.get();
            if (activity != null) {
                activity.handleQrCode(s);
            }
        }
    }
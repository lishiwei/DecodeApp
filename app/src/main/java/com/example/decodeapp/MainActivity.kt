package com.example.decodeapp

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.drawee.backends.pipeline.Fresco
import com.imnjh.imagepicker.PickerConfig
import com.imnjh.imagepicker.SImagePicker
import com.tbruyelle.rxpermissions2.RxPermissions
import com.xys.libzxing.zxing.activity.CaptureActivity
import io.reactivex.functions.Consumer
import java.util.*

class MainActivity : AppCompatActivity() {
    var REQUEST_CODE_SCAN = 100
    var REQUEST_SELECT_IMAGE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val d: io.reactivex.disposables.Disposable =
                RxPermissions(this).request(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA)
                        .subscribe(Consumer {
                            if (it) {
                                SImagePicker.init(
                                        PickerConfig.Builder().setAppContext(this@MainActivity)
                                                .setToolbaseColor(Color.parseColor("#FFFFFF"))
                                                .setImageLoader(FrescoImageLoader((this@MainActivity))).build()
                                )
                                var list = mutableListOf<String>();
                                list.add("扫码")
                                list.add("身份证")
                                list.add("银行卡")

                                var intent = Intent(this@MainActivity, CaptureActivity::class.java);
                                intent.putStringArrayListExtra("list", list as ArrayList<String>);
                                startActivityForResult(intent, REQUEST_CODE_SCAN);


                            }

                        })


    }

    /**
     * 处理图片二维码解析的数据
     *
     * @param s
     */
    fun handleQrCode(s: String?) {
        if (null == s) {
            finish()
            Toast.makeText(this, "该图片无法识别二维码", Toast.LENGTH_SHORT).show()
        } else {
            handleResult(s)
        }
    }

    private fun handleResult(result: String) {
        Toast.makeText(this@MainActivity, result, Toast.LENGTH_SHORT).show()
    }

    /**
     * 启动线程解析二维码图片
     *
     * @param path
     */
    private fun parsePhoto(path: String) {
        //启动线程完成图片扫码
        QrCodeAsyncTask(this, path).execute(path)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 扫描二维码/条码回传
        if (requestCode == REQUEST_CODE_SCAN && resultCode == RESULT_OK) {
            if (data != null) {

                var content = data.getStringExtra("result");
                if (content.equals(CaptureActivity.GALLERY)) {
                    val i = Intent()

                    i.action = AppConstant.ACTION_IMAGE_GET_ALL
                    i.setPackage(getPackageName())
                    i.putExtra(ImageSelectActivity.KEY_NEED_CROP, false)
                    i.putExtra(ImageSelectActivity.KEY_NEED_CAMERA, true)
                    startActivityForResult(i, REQUEST_SELECT_IMAGE)
                    return
                }
                if (content.equals(CaptureActivity.CAMERA)) {
                    var content = data.getStringExtra("photoPath");
                    Toast.makeText(this@MainActivity, content, Toast.LENGTH_SHORT).show()
                    return
                }
                if (content.equals(CaptureActivity.BACK)) {
                    return
                }

                Toast.makeText(this, content, Toast.LENGTH_SHORT).show()
            }
        }
        if (requestCode == REQUEST_SELECT_IMAGE && resultCode == RESULT_OK) {
            Log.d("aaaaa", "onActivityResult: RESULT_OK")
            val pathList = data!!.getStringArrayListExtra(ImageSelectActivity.RETURN_DATA_FILE)
            parsePhoto(pathList?.get(0)!!)
        }
        if (requestCode == REQUEST_SELECT_IMAGE && resultCode != RESULT_OK) {
            Toast.makeText(this@MainActivity, "您未选择图片!", Toast.LENGTH_SHORT).show()
        }
    }
}
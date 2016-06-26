/**
 *
 */
package com.ll.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.widget.Toast;

public class ToastUtil {
    private static ProgressDialog progDialog=null;

    public static void show(Context context, String info) {
        Toast.makeText(context, info, Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示进度条对话框
     */
    public static void showDialog(Context context,String msg){
        if (progDialog == null)
            progDialog = new ProgressDialog(context);;
        progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progDialog.setMessage(msg);
        progDialog.setCancelable(true);
        progDialog.setIndeterminate(false);
        progDialog.show();
    }

    /**
     * 显示进度条对话框
     */
    public static void showPrograss(Context context,String msg){
        if (progDialog == null)
            progDialog = new ProgressDialog(context);;
        progDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progDialog.setMessage(msg);
        progDialog.setMax(100);
        progDialog.setCancelable(true);
        progDialog.setIndeterminate(false);
        progDialog.show();
    }

    public static void changeDialog(String msg){
        if(progDialog!=null){
            progDialog.setMessage(msg);
        }
    }

    /**
     * 隐藏进度条对话框
     */
    public static void dismissDialog() {
        if (progDialog != null) {
            progDialog.dismiss();
            progDialog=null;
        }
    }

    public static boolean isShowing(){
        if(progDialog!=null){
            return progDialog.isShowing();
        }else{
            return false;
        }

    }


}

/**
 * 
 */
package com.ll.utils;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import android.text.Html;
import android.text.Spanned;
import android.widget.EditText;

import com.amap.api.location.AMapLocation;
import com.amap.api.maps.model.LatLng;

public class AMapUtil {
	/**
	 * 判断edittext是否null
	 */
	public static String checkEditText(EditText editText) {
		if (editText != null && editText.getText() != null
				&& !(editText.getText().toString().trim().equals(""))) {
			return editText.getText().toString().trim();
		} else {
			return "";
		}
	}

	public static Spanned stringToSpan(String src) {
		return src == null ? null : Html.fromHtml(src.replace("\n", "<br />"));
	}

	public static String colorFont(String src, String color) {
		StringBuffer strBuf = new StringBuffer();

		strBuf.append("<font color=").append(color).append(">").append(src)
				.append("</font>");
		return strBuf.toString();
	}

	public static String makeHtmlNewLine() {
		return "<br />";
	}

	public static String makeHtmlSpace(int number) {
		final String space = "&nbsp;";
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < number; i++) {
			result.append(space);
		}
		return result.toString();
	}

	public static String getFriendlyLength(int lenMeter) {
		if (lenMeter > 10000) // 10 km
		{
			int dis = lenMeter / 1000;
			return dis + ChString.Kilometer;
		}

		if (lenMeter > 1000) {
			float dis = (float) lenMeter / 1000;
			DecimalFormat fnum = new DecimalFormat("##0.0");
			String dstr = fnum.format(dis);
			return dstr + ChString.Kilometer;
		}

		if (lenMeter > 100) {
			int dis = lenMeter / 50 * 50;
			return dis + ChString.Meter;
		}

		int dis = lenMeter / 10 * 10;
		if (dis == 0) {
			dis = 10;
		}

		return dis + ChString.Meter;
	}

	public static boolean IsEmptyOrNullString(String s) {
		return (s == null) || (s.trim().length() == 0);
	}


	/**
	 * 把AMapLocation对象转化为LatLon对象
	 */
	public static LatLng convertToLatLng(AMapLocation aMapLoc) {
		return new LatLng(aMapLoc.getLatitude(), aMapLoc.getLongitude());
	}


	/**
     * 判断两个位置点是否相同	
     * @param prevPoint
     * @param currPoint
     * @return
     */
    public static boolean diff(LatLng prevPoint,LatLng currPoint){
        Double prevLng=prevPoint.longitude;
        Double prevLat=prevPoint.latitude;
        Double currLng=currPoint.longitude;
        Double currLat=currPoint.latitude;
        Double diffLng=Math.abs(currLng-prevLng);
        Double diffLat=Math.abs(currLat-prevLat);
        if(diffLng>0.0000005||diffLat>0.0000005){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 获取系统当前时间并转换成一定格式字符串
     * @return  时间字符串
     */
	public static String getSystemTime() {
		Date current_time=new Date(System.currentTimeMillis());
        SimpleDateFormat formatter=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(current_time);
	}
	
	/**
     * 生成长度为length的随机字符串，包括数字和大小写字母
     */
    public static String getCharAndNum(int length){
        String val="";
        Random random=new Random(System.currentTimeMillis());
        for(int i=0;i<length;i++){
            String charOrnum=random.nextInt(2)%2==0 ? "char":"num";
            if("char".equalsIgnoreCase(charOrnum)){
                int upperOrlower=random.nextInt(2)%2==0 ? 65:97;
                val+=(char)(upperOrlower+random.nextInt(26));  //convert int to char
            }else if("num".equalsIgnoreCase(charOrnum)){
                val+=String.valueOf(random.nextInt(10));   //parse int to string
            }
        }
        return val;
    }
    
    
	
	public static final String HtmlBlack = "#000000";
	public static final String HtmlGray = "#808080";
}

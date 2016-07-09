package com.ll.utils;

import java.io.File;
import java.io.FileInputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.ll.data.TabhostActivity;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class FileUtil {
	public static String TAG = FileUtil.class.getName();
	public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_AUDIO = 2;
    private static final String FILE_PATH = "/MapAppForHZ/";
    private static final String PHOTO_PATH = "/photos";
    private static final String AUDIO_PATH = "/audios";
	
    /**
     * 根据type和user_id生成对应的文件（图片或语音）
     * @param type
     * @param user_id
     * @return
     */
    public static File getOutputMediaFile(int type, String user_id) {
    	LogUtil.d(TAG, "getOutputMediaFile");
    	if(!isSdCardExist()){
    		return null;
    	}
        return createFile(type, user_id);
    }

    /**
     * 判断SDCard是否存在【当没有外挂SD卡时，内置ROM也被识别为存在sd卡】
     * @return
     */
    public static boolean isSdCardExist(){
    	LogUtil.d(TAG, "isSdCardExist");
    	return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }
    
    /**
     * 获取SD卡根目录路径
     */
    public static String getSdCardPath(){
    	LogUtil.d(TAG, "getSdCardPath");
    	boolean exist = isSdCardExist();
    	String sdPath = "";
    	if(exist){
    		//getExternalStorageDirectory()会返回主要的外存地址
    		sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
    		LogUtil.d(FileUtil.class.getName(), sdPath);
    	}
    	return sdPath;
    }
    
    /**
     * 根据filetype和user_id构造文件（图片或语音）绝对路径
     * @param filetype
     * @param user_id
     * @return
     */
    public static String getFilePath(int filetype, String user_id){
    	LogUtil.d(TAG, "getFilePath");
    	if(!getSdCardPath().equals("") && user_id != null && !user_id.equals("")){
    		//Create an image file name
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm", Locale.CHINA).format(new Date());
    		StringBuilder filepath = new StringBuilder(getSdCardPath()+FILE_PATH + user_id );
    		if(filetype == MEDIA_TYPE_IMAGE){
    			filepath.append(PHOTO_PATH+File.separator
    					 + "IMG_" + user_id + ".jpg");
    		}else if(filetype == MEDIA_TYPE_AUDIO){
    			filepath.append(AUDIO_PATH+File.separator
    					 + "AUD_" + user_id +  ".amr");
    		}
    		return filepath.toString();
    	}
    	return null;
    }
    /**
     * 根据filetype和user_id构建文件绝对路径，并检查文件是否存在
     * @param filetype
     * @param user_id
     * @return  文件存在时返回文件，否则返回null
     */
    public static File isFileExist(int filetype, String user_id){
    	String filepath = getFilePath(filetype, user_id);
    	if(filepath !=null && !filepath.equals("")){
    		try{
    			File file = new File(filepath);
        		if(file.exists()){
        			return file;
        		}
    		}catch (Exception E){
    			E.printStackTrace();
    		}
    	}
    	return null;
    }
    /**
     * 根据文件路径获取文件大小
     * @param filepath
     * @return
     */
    public static String getFileSize(String filepath){
    	LogUtil.d(TAG, "getFileSize");
    	File file = isFileExist(filepath);
    	long fileS = 0;
    	if(file != null){
    		try{
    			FileInputStream fis = new FileInputStream(file);
        		fileS = fis.available();
    		}catch(Exception e) {
    			e.printStackTrace();
    		}
    	}
        DecimalFormat df = new DecimalFormat("#.00"); 
        String fileSizeString ; 
        if (fileS < 1024) { 
            fileSizeString = df.format((double) fileS) + "B"; 
        } else if (fileS < 1048576) { 
            fileSizeString = df.format((double) fileS / 1024) + "KB"; 
        } else if (fileS < 1073741824) { 
            fileSizeString = df.format((double) fileS / 1048576) + "MB"; 
        } else { 
            fileSizeString = df.format((double) fileS / 1073741824) + "GB"; 
        } 
        return fileSizeString; 
    }
    
    /**
     * 根据文件路径获取文件上次更新日期
     * @param filepath
     * @return
     */
    public static String getFileDate(String filepath){
    	LogUtil.d(TAG, "getFileDate");
    	String date = null;
    	if(filepath != null && filepath.contains("_")){
    		int start = filepath.lastIndexOf("_");
    		int end = filepath.lastIndexOf(".");
    		if(start != -1 && end != -1 && start < end){
    			date = filepath.substring(start+1, end);
    			String[] ds = date.split("-");
    			if(ds != null && ds.length == 5){
    				date = ds[0]+"-"+ds[1]+"-"+ds[2]+" "+ds[3]+"："+ds[4];
    			}
    		}
    	}
        return date; 
    }
    
    /**
     * 根据filepath检查文件是否存在
     * @return  文件存在时返回文件，否则返回null
     */
    public static File isFileExist(String filepath){
    	LogUtil.d(TAG, "isFileExist2");
    	if(filepath !=null && !filepath.equals("")){
    		try{
    			File file = new File(filepath);
        		if(file.exists()){
        			return file;
        		}
    		}catch (Exception E){
    			E.printStackTrace();
    		}
    	}
    	return null;
    }
    /**
     * 若该文件已经存在，则直接返回
     */
    public static File createFile(int filetype, String user_id){
    	LogUtil.d(TAG, "createFile");
    	File file = isFileExist(filetype, user_id);
    	if(file == null){
    		String filepath = getFilePath(filetype, user_id);
        	if(filepath !=null && !filepath.equals("")){
        		try{
        			file = new File(filepath);
            		if(!file.mkdirs()){
            			return null;
            		}
        		}catch (Exception E){
        			E.printStackTrace();
        		}
        	}
		}
    	file.setLastModified(System.currentTimeMillis());
		return file;
    }
    
}

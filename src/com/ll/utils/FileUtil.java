package com.ll.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Formatter.BigDecimalLayoutForm;

import com.ll.data.TabhostActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class FileUtil {
	public static String TAG = FileUtil.class.getName();
	public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_AUDIO = 2;
    private static final String FILE_PATH = "/HZ/";
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
     * 根据filetype和user_id构造文件（图片或语音）存储的路径
     * @param filetype
     * @param user_id
     * @return
     */
    public static String getFileDir(int filetype, String user_id){
		StringBuilder filepath = new StringBuilder(getSdCardPath()+FILE_PATH + user_id + File.separator);

		return filepath.toString();
    }
    /**
     * 根据filetype和user_id构造文件（图片或语音）存储的路径
     * @param filetype
     * @param user_id
     * @return
     */
    public static String getFilePath(int filetype, String user_id){
		StringBuilder filepath = new StringBuilder(getFileDir(filetype, user_id));
		if(filetype == MEDIA_TYPE_IMAGE){
			filepath.append("IMG_" + user_id + ".jpg");
		}else if(filetype == MEDIA_TYPE_AUDIO){
			filepath.append("AUD_" + user_id +  ".amr");
		}
		return filepath.toString();
    }
    /**
     * 根据filetype和user_id构建文件绝对路径，并检查文件是否存在
     * @param filetype
     * @param user_id
     * @return  文件存在时返回文件，否则返回null
     */
    public static File isFileExist(int filetype, String user_id){
//    	String dirPath = getFileDir(filetype, user_id);
    	
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
    	File file = isFileExist(filetype, user_id);
    	String filepath = getFileDir(filetype, user_id);
    	if(file == null){
        	if(filepath !=null && !filepath.equals("")){
        		try{
        			file = new File(filepath);
        			if(!file.exists()){
        				file.mkdirs();
        			}
        		}catch (Exception E){
        			E.printStackTrace();
        		}
        	}
		}
    	try{
	    	if(filetype == MEDIA_TYPE_IMAGE){
				file = new File(filepath, "IMG_" + user_id + ".jpg");
			}else if(filetype == MEDIA_TYPE_AUDIO){
				file = new File(filepath, "AUD_" + user_id +  ".amr");
			}
			if(file.exists()){
				file.delete();
			}
			file.setWritable(true);
			file.createNewFile();
			file.setLastModified(System.currentTimeMillis());
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    	LogUtil.d(TAG, "createFile="+file.getAbsolutePath());
    	LogUtil.d(TAG, "isFileExist="+file.exists()+"isFile="+file.isFile());
		return file;
    }
    
    /**
     * 将一个InputStream里面的数据写入到SD卡中
     */
    public static File write2SDFromInput(File file, InputStream input) {
//        File file = null;
    	if(file == null || !file.exists()){
    		LogUtil.e(TAG, "file is null");
    		return null;
    	}
        OutputStream output = null;
        try {
//            file = createFile(type, userId);
            output = new FileOutputStream(file);
            LogUtil.d(TAG, "write2SDFromInput= "+ file.getAbsolutePath());
            byte buffer[] = new byte[4 * 1024];
 
            while (true) {
                int temp = input.read(buffer, 0, buffer.length);
                if (temp == -1) {
                    break;
                }
                output.write(buffer, 0, temp);
            }
 
            output.flush();
            file.setLastModified(System.currentTimeMillis());
            LogUtil.d(TAG, "name="+file.getName()+", time="+file.lastModified());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
            	input.close();
                output.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return file;
    }
    
    public static void saveFile(Bitmap bm, String filename) throws Exception{
    	File file = new File(filename);
    	if(file.exists()){
    		file.delete();
    	}
    	file = new File(filename);
    	BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
    	bm.compress(Bitmap.CompressFormat.JPEG, 30, bos);
    	bos.flush();
    	bos.close();
    }
}

package com.pq.imageloaderdemo.bean;

import android.text.TextUtils;

import java.io.File;

/**
 * Created by pan on 2018/4/27.
 * 文件夹 属性
 */

public class FolderBean {
    public String dir;
    /*
       文件夹名称
     */
    private String name;
    public int count;
    public String firstImgPath;


    public String getName(){
        if (!TextUtils.isEmpty(dir)){
//            int lastIndex = dir.lastIndexOf("/");
//            return dir.substring(lastIndex+1);
            return new File(dir).getName();
        }
        return "unknown";

    }
}

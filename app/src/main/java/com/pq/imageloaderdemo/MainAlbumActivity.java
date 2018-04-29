package com.pq.imageloaderdemo;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.pq.imageloaderdemo.anno.ClickInject;
import com.pq.imageloaderdemo.anno.InjectUtil;
import com.pq.imageloaderdemo.anno.ViewInject;
import com.pq.imageloaderdemo.bean.FolderBean;
import com.pq.imageloaderdemo.util.ImageLoader;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by pan on 2018/4/27.
 */

public class MainAlbumActivity extends AppCompatActivity {

    private final static int SCAN_ALBUM_FINISH = 0X0321;



    @ClickInject(R.id.iv_back)
    public void back(View view) {
        finish();
    }

    @ClickInject(R.id.iv_send)
    public void send(View view) {

    }

    @ClickInject(R.id.tv_dirname)
    public void openPopWindow(View view){
        //set animation
        mPopUpWindow.setAnimationStyle(R.style.popwindowanim);
        mPopUpWindow.showAsDropDown(view,0,0);
        lightOff();
    }

    /*
     当前文件夹目录名
    */
    @ViewInject(R.id.tv_dirname)
    private TextView mDirName;

    /*
       当前文件夹总图片个数
     */
    @ViewInject(R.id.tv_dircount)
    private TextView mDirCount;

    @ViewInject(R.id.grid_view)
    private GridView mGridView;

    private List<String> mImgs;
    private File mCurrentDir;
    private int mCurrentCount;

    private ProgressDialog mProgressDialog;

    private AllFolderPopUpWindow mPopUpWindow;

    private ImageAdapter mAdapter;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
//            super.handleMessage(msg);
            switch (msg.what) {
                case SCAN_ALBUM_FINISH:
                    if (null!= mProgressDialog){
                        mProgressDialog.dismiss();
                    }
                    data2view();

                    initPopWindow();

                    break;
                default:
                    break;
            }

        }
    };

    /*
       文件夹 列表
     */
    private List<FolderBean> mFolders = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_album);
        InjectUtil.inject(this);

        initData();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * 获取本地图片数据
     */
    private void initData() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "读取存储卡出现异常", Toast.LENGTH_SHORT).show();
            return;
        }

        mProgressDialog = ProgressDialog.show(this, null, "正在扫描图片");

        new Thread() {
            @Override
            public void run() {
//                super.run();
                //扫描所有图片
                Uri mImgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver contentResolver = getContentResolver();

                Cursor cursor = contentResolver.query(mImgUri, null, MediaStore.Images.Media.MIME_TYPE
                                + "=? or " + MediaStore.Images.Media.MIME_TYPE + "=?", new String[]{"image/jpeg", "image/png"},
                        MediaStore.Images.Media.DATE_MODIFIED);

                // 过滤，获取 文件夹！！！
                Set<String> mdirPaths = new HashSet<>();

                while (null != cursor && cursor.moveToNext()) {
                    // 获取每张图片
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    //获取图片的父文件夹路径
                    File parentFile = new File(path).getParentFile();
                    if (null != parentFile) {
                        //获取父文件的列表
                        String dirPath = parentFile.getAbsolutePath();
                        FolderBean folderBean = null;

                        if (!mdirPaths.contains(dirPath)) {
                            mdirPaths.add(dirPath);
                            folderBean = new FolderBean();
                            folderBean.dir = dirPath;
                            folderBean.firstImgPath = path;
                            if (null != parentFile.list()) {
                                folderBean.count = parentFile.list(new FilenameFilter() {
                                    @Override
                                    public boolean accept(File dir, String fileName) {
                                        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")
                                                || fileName.endsWith("png")) {
                                            return true;
                                        }
                                        return false;
                                    }
                                }).length;

                            }

                            mFolders.add(folderBean);

                            //获取文件夹中最大图片的作为当前的
                            if (folderBean.count > mCurrentCount) {
                                mCurrentCount = folderBean.count;
                                mCurrentDir = parentFile;
                            }
                        }
                    }
                }
                cursor.close();

                mHandler.sendEmptyMessage(SCAN_ALBUM_FINISH);

            }
        }.start();


    }

    private void data2view(){
        if (null == mCurrentDir){
            Toast.makeText(this,"未扫描到任何图片",Toast.LENGTH_SHORT).show();
        }else {
            mImgs = Arrays.asList(mCurrentDir.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String fileName) {
                    if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")
                            || fileName.endsWith("png")) {
                        return true;
                    }
                    return false;
                }
            }));
            //加载
            mAdapter = new ImageAdapter(this,mImgs,mCurrentDir.getAbsolutePath());
            mGridView.setAdapter(mAdapter);

            mDirName.setText(mCurrentDir.getName());
            mDirCount.setText(String.format("共 %d 张",mCurrentCount));
        }
    }

    private void initPopWindow(){
        mPopUpWindow = new AllFolderPopUpWindow(this, mFolders);
        mPopUpWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                lightOn();
            }
        });

        mPopUpWindow.setOnItemSelectedListener(new AllFolderPopUpWindow.OnItemSelectedListener() {
            @Override
            public void onSelected(View view, int pos) {
                mPopUpWindow.dismiss();
                FolderBean currentFolder = mFolders.get(pos);
                mCurrentDir= new File(currentFolder.dir);
                mCurrentCount=currentFolder.count;

                data2view();
            }
        });
    }


    private static class ImageAdapter extends BaseAdapter{

        private LayoutInflater layoutInflater;
        private List<String> mImgPaths;
        private String mDirPath;

        private Set<String> mCheckedImgs=new HashSet<>();

        public ImageAdapter(Context context, List<String> datas, String dirPath){
            this.mDirPath =dirPath;
            this.mImgPaths=datas;
            layoutInflater=LayoutInflater.from(context);

        }
        @Override
        public int getCount() {
            return mImgPaths.size();
        }

        @Override
        public Object getItem(int position) {
            return mImgPaths.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final String imgPath = mImgPaths.get(position);
            ViewHolder viewHolder=null;
            if (null==convertView){
                convertView = layoutInflater.inflate(R.layout.item_album,parent,false);
                viewHolder=new ViewHolder();
                viewHolder.imageView=convertView.findViewById(R.id.iv_image);
                viewHolder.radioButton=convertView.findViewById(R.id.rb_check);
                convertView.setTag(viewHolder);

            }else {
                viewHolder=(ViewHolder)convertView.getTag();
            }
            //reset
            viewHolder.imageView.setImageResource(R.mipmap.ic_launcher_round);

            viewHolder.radioButton.setChecked(false);
            viewHolder.imageView.setColorFilter(null);

//            if (mCheckedImgs.contains(imgPath)){
//             viewHolder.radioButton.setChecked(true);
//             viewHolder.imageView.setColorFilter(Color.parseColor("#77000000"));
//            }else {
//
//            }



            //set value
            ImageLoader.getInstance(3, ImageLoader.Type.LIFO)
                    .loadImage(mDirPath+File.separator+imgPath,viewHolder.imageView);


            final ViewHolder finalVH=viewHolder;
            viewHolder.radioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked){
                        mCheckedImgs.add(imgPath);
                        finalVH.imageView.setColorFilter(Color.parseColor("#77000000"));
                    }else {
                        mCheckedImgs.remove(imgPath);
                        finalVH.imageView.setColorFilter(null);
                    }
                    finalVH.radioButton.setChecked(isChecked);
                }
            });

            return convertView;
        }
    }

    private static class ViewHolder{
        ImageView imageView;
        CheckBox radioButton;
    }

    private void lightOn(){
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.alpha=1.0f;
        getWindow().setAttributes(layoutParams);
    }

    private void lightOff(){
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.alpha=0.5f;
        getWindow().setAttributes(layoutParams);
    }
}

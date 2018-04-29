package com.pq.imageloaderdemo.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by pan on 2018/4/27.
 * 图片加载类
 */

public class ImageLoader {

    /*
       通知后台调度线程 执行 新任务
     */
    private static final int NOTIFY_TASK_ARRIVE = 0X0321;
    private static ImageLoader mInstance;

    /**
     * 图片的缓存
     */
    private LruCache<String, Bitmap> mLruCache;

    /*
      线程池
     */
    private ExecutorService mThreadPool;

    /*
    默认的线程池 线程个数
     */
    private final static int DEFAULT_THREAD_COUNT = 1;

    /*
      任务队列
     */
    private LinkedList<Runnable> mTaskQueue;

    /*
     队列的调度方式： 先进先出，后进先出
     */
    private Type mType = Type.LIFO;

    /*
     后台调度线程
     */
    private Thread mPoolThread;

    /*
      调度线程的处理
     */
    private Handler mPoolThreadHandler;

    /*
      更新ui线程的handler
     */
    private Handler mUIHandler;

    /*
       防止handler未创建
     */
    private Semaphore mPoolThreadHandlerSemaphore = new Semaphore(0);

    /*
      线程池 防止lifo策略不生效
     */
    private Semaphore mThreadPoolSemaphore;


    public enum Type {
        FIFO, LIFO;
    }


    public static ImageLoader getInstance() {
        return getInstance(DEFAULT_THREAD_COUNT, Type.LIFO);
    }

    public static ImageLoader getInstance(int threadCount, Type type) {
        if (null == mInstance) {
            synchronized (ImageLoader.class) {
                if (null == mInstance) {
                    mInstance = new ImageLoader(threadCount, type);
                }
            }
        }
        return mInstance;
    }

    private ImageLoader(int threadCount, Type type) {
        init(threadCount, type);
    }

    /**
     * 初始化
     *
     * @param threadCount
     * @param type
     */
    private void init(int threadCount, Type type) {

        // 设置 后台调度线程
        mPoolThread = new Thread() {
            @Override
            public void run() {
                Looper.prepare();

                mPoolThreadHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
//                        super.handleMessage(msg);
                        switch (msg.what) {
                            case NOTIFY_TASK_ARRIVE:
                                mThreadPool.execute(getTask());
//                                try {
//                                    mThreadPoolSemaphore.acquire();
//                                }catch (Exception e){
//                                    e.printStackTrace();
//                                }
                                break;
                            default:
                                break;
                        }
                    }
                };
                mPoolThreadHandlerSemaphore.release();
                Looper.loop();

            }
        };
        mPoolThread.start();

        // 设置最大缓存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxMemory / 8;
        mLruCache = new LruCache<String, Bitmap>(cacheMemory) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getRowBytes() * bitmap.getHeight();
            }
        };

        // 设置线程池
        mThreadPool = Executors.newFixedThreadPool(threadCount);

        mTaskQueue = new LinkedList<>();

        mType = type;

        mThreadPoolSemaphore = new Semaphore(threadCount);
    }


    /**
     * 加载图片接口
     *
     * @param path
     * @param imageView
     */
    public void loadImage(final String path, final ImageView imageView) {
        //
        imageView.setTag(path);

        if (null == mUIHandler) {
            mUIHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
//                    super.handleMessage(msg);
                    ImgBeanHolder holder = (ImgBeanHolder) msg.obj;
                    ImageView tmp_imageView = holder.imageView;
                    Bitmap tmp_bitmap = holder.bitmap;
                    String tmp_path = holder.path;

                    if (tmp_imageView.getTag().toString().equals(tmp_path)) {
                        tmp_imageView.setImageBitmap(tmp_bitmap);
                    }
                }
            };
        }

        Bitmap bm = getBitmapFromLruCache(path);
        if (null != bm) {
            refreshUIBitmap(path,imageView,bm);

//            refreshImageView(bm);
        } else {
            addTasks(new Runnable() {
                @Override
                public void run() {
                    // 计算图片大小
                    Point size = getImageViewSize(imageView);

                    //压缩图片
                    Bitmap bitmap = decodeSampleImage(path,size);

                    //加入缓存 并显示图片
                    addBitmapToLrucache(path,bitmap);
                    refreshUIBitmap(path,imageView,bitmap);

                    //  释放信号量
                    mThreadPoolSemaphore.release();

                }
            });
        }
    }

    private void refreshUIBitmap(String path , ImageView imageView, Bitmap bitmap){
        ImgBeanHolder holder = new ImgBeanHolder();
        holder.bitmap = bitmap;
        holder.imageView = imageView;
        holder.path = path;
        Message message = Message.obtain(mUIHandler);
        message.obj = holder;
        mUIHandler.sendMessage(message);
    }


    /**
     * 从任务队列中取任务
     * @return
     */
    private Runnable getTask(){
        try {
            mThreadPoolSemaphore.acquire();
        }catch (Exception e){
            e.printStackTrace();
        }
        if (mType == Type.LIFO){
            return mTaskQueue.removeLast();
        }else if (mType == Type.FIFO){
            return mTaskQueue.removeFirst();
        }

        return null;
    }


    /**
     *  加入任务队列，并通知线程池取任务
     * @param runnable
     */
    private synchronized void addTasks(Runnable runnable) {
        mTaskQueue.add(runnable);

        try{
            if (null == mPoolThreadHandler){
                mPoolThreadHandlerSemaphore.acquire();
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        mPoolThreadHandler.sendEmptyMessage(NOTIFY_TASK_ARRIVE);
    }

    private Bitmap getBitmapFromLruCache(String key) {
        return mLruCache.get(key);
    }

    /**
     * 加入缓存
     * @param path
     * @param bitmap
     */
    private void addBitmapToLrucache(String path, Bitmap bitmap){
        if (null == getBitmapFromLruCache(path)){
            if (null != bitmap){
                mLruCache.put(path,bitmap);
            }
        }
    }

    private Point getImageViewSize(ImageView imageView){
        DisplayMetrics displayMetrics= imageView.getResources().getDisplayMetrics();
        Point point = new Point();
        ViewGroup.LayoutParams layoutParams=imageView.getLayoutParams();
        int width = imageView.getWidth();

        if (width<=0){
            width=layoutParams.width;
        }
        if (width <= 0){
            width = imageView.getMaxWidth();
        }
        if (width  <= 0){
            width = displayMetrics.widthPixels;
        }

        int height = imageView.getHeight();

        if (height<=0){
            height=layoutParams.height;
        }
        if (height <= 0){
            height = imageView.getMaxHeight();
        }
        if (height  <= 0){
            height = displayMetrics.heightPixels;
        }

        point.x=width;
        point.y=height;


        return point;
    }

    /**
     * 压缩图片
     * @param path
     * @param size
     * @return
     */
    private Bitmap decodeSampleImage(String path, Point size){
        int reqWidth = size.x;
        int reqHeight = size.y;

        //获取图片尺寸，但并不把图片放到内存中
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inJustDecodeBounds=true;
        BitmapFactory.decodeFile(path,options);

        options.inSampleSize = calculateInSampleSize(options,reqWidth,reqHeight);
        options.inJustDecodeBounds=false;
        return BitmapFactory.decodeFile(path,options);

    }

    /**
     * 获取合适的缩放比例
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private int calculateInSampleSize(BitmapFactory.Options options,int reqWidth, int reqHeight){
        int width = options.outWidth;
        int height = options.outHeight;

        int inSampleSize=1;
        if (width > reqHeight || height > reqHeight){
            int widthRatio = Math.round(1.0f * width / reqWidth);
            int heightRatio = Math.round(1.0f * height /reqHeight);
            inSampleSize = Math.max(widthRatio,heightRatio);
        }
        return inSampleSize;
    }

    private static class ImgBeanHolder {
        ImageView imageView;
        String path;
        Bitmap bitmap;
    }


}

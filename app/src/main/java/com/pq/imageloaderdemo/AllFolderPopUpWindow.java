package com.pq.imageloaderdemo;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.pq.imageloaderdemo.bean.FolderBean;
import com.pq.imageloaderdemo.util.ImageLoader;

import java.util.List;

/**
 * Created by pan on 2018/4/29.
 */

public class AllFolderPopUpWindow extends PopupWindow {

    private Context context;
    private List<FolderBean> mBeans;
    private RecyclerView mRecyclerView;

    private OnItemSelectedListener mListener;

    public void setOnItemSelectedListener(OnItemSelectedListener _listener) {
        this.mListener = _listener;
    }

    public interface OnItemSelectedListener {
        void onSelected(View view, int pos);
    }

    public AllFolderPopUpWindow(Context context, List<FolderBean> beans) {

        this.context = context;
        this.mBeans = beans;

        int width = context.getResources().getDisplayMetrics().widthPixels;
        int height = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.7f);

        View contentView = LayoutInflater.from(context).inflate(R.layout.layout_popwindow, null);

        setContentView(contentView);

        setWidth(width);
        setHeight(height);

        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);

        setBackgroundDrawable(new BitmapDrawable());
        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();
                    return true;
                }
                return false;
            }
        });

        initView(contentView);
    }

    private void initView(View rootView) {
        mRecyclerView = rootView.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        InnerAdapter adapter = new InnerAdapter(context, mBeans);
        mRecyclerView.setAdapter(adapter);
    }


    private class InnerAdapter extends RecyclerView.Adapter<InnerViewHolder> {
        private LayoutInflater layoutInflater;
        private List<FolderBean> mBeans;

        public InnerAdapter(Context context, List<FolderBean> beans) {
            layoutInflater = LayoutInflater.from(context);
            this.mBeans = beans;
        }

        @Override
        public InnerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = layoutInflater.inflate(R.layout.item_popwindow, parent, false);
            return new InnerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final InnerViewHolder holder, final int position) {
            holder.update(mBeans.get(position));
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (null != mListener) {
                        mListener.onSelected(holder.itemView, position);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mBeans.size();
        }
    }


    private static class InnerViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageView;
        private TextView nameView, countView;

        public InnerViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
            nameView = itemView.findViewById(R.id.tv_name);
            countView = itemView.findViewById(R.id.tv_count);
        }

        private void update(FolderBean bean) {
            ImageLoader.getInstance()
                    .loadImage(bean.firstImgPath, imageView);

            nameView.setText(bean.getName());

            countView.setText(String.format("共 %d  张", bean.count));

        }
    }
}

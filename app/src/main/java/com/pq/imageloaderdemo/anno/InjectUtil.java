package com.pq.imageloaderdemo.anno;

import android.app.Activity;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by pan on 2018/4/27.
 */

public class InjectUtil {
    public static void inject(final Activity activity){

        Field[] declaredFields = activity.getClass().getDeclaredFields();
        for(int i=0;i<declaredFields.length;i++){
            Field field = declaredFields[i];
            field.setAccessible(true);
            ViewInject annotation = field.getAnnotation(ViewInject.class);
            if (annotation!=null){
                int id = annotation.value();
                View view = activity.findViewById(id);
                try {
                    field.set(activity, view);
                }catch (Exception e){

                }
            }
        }


        Method[] declaredMethods = activity.getClass().getDeclaredMethods();
        for(int i=0;i<declaredMethods.length;i++){
            final Method method = declaredMethods[i];
            method.setAccessible(true);
            ClickInject annotation = method.getAnnotation(ClickInject.class);
            if (null != annotation){
                int[] ids = annotation.value();
                for(int j=0;j<ids.length;j++){
                    final View button = activity.findViewById(ids[j]);
                    button.setOnClickListener(new View.OnClickListener(){
                        @Override
                        public void onClick(View v) {
                            try {
                                method.invoke(activity,button);
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        }
    }
}

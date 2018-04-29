package com.pq.imageloaderdemo;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.GridView;

import com.pq.imageloaderdemo.anno.ClickInject;
import com.pq.imageloaderdemo.anno.InjectUtil;
import com.pq.imageloaderdemo.anno.ViewInject;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import com.yanzhenjie.permission.PermissionNo;
import com.yanzhenjie.permission.PermissionYes;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.RationaleListener;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSION_STORAGE = 200;
    private static final int REQUEST_CODE_SETTING = 300;


    @ClickInject(R.id.btn_open)
    public void openAlbum(View view){
        AndPermission.with(this)
                .requestCode(REQUEST_CODE_PERMISSION_STORAGE)
                .permission(Permission.STORAGE)
                .callback(this)
                // rationale作用是：用户拒绝一次权限，再次申请时先征求用户同意，再打开授权对话框；
                // 这样避免用户勾选不再提示，导致以后无法申请权限。
                // 你也可以不设置。
                .rationale(new RationaleListener() {
                    @Override
                    public void showRequestPermissionRationale(int i, Rationale rationale) {
                        AndPermission.rationaleDialog(MainActivity.this, rationale).show();
//                                                rationale.resume();
                    }
                })
                .start();
    }

    @ViewInject(R.id.grid_view)
    private GridView mGridView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        InjectUtil.inject(this);


    }

    @PermissionYes(REQUEST_CODE_PERMISSION_STORAGE)
    private void getStoragePermissionGranted(@NonNull List<String> grantedPermissions) {
//        ToastUtil.shortToast(this, "storage permission granted");
        Intent intent = new Intent(this,MainAlbumActivity.class);
        startActivity(intent);
    }

    @PermissionNo(REQUEST_CODE_PERMISSION_STORAGE)
    private void getStoragePermissionDenied(@NonNull List<String> deniedPermissions) {
//        ToastUtil.shortToast(this, "storage permission denied");
        if (AndPermission.hasAlwaysDeniedPermission(this,deniedPermissions)){
            AndPermission.defaultSettingDialog(this,REQUEST_CODE_SETTING).show();
        }
    }
}

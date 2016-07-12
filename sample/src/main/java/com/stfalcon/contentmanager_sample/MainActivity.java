package com.stfalcon.contentmanager_sample;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.stfalcon.contentmanager.ContentManager;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, ContentManager.PickContentListener {

    private ImageView ivPicture;
    private ContentManager contentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contentManager = new ContentManager(this, this);

        Button btnTake = (Button) findViewById(R.id.btn_take);
        btnTake.setOnClickListener(this);
        Button btnPick = (Button) findViewById(R.id.btn_pick);
        btnPick.setOnClickListener(this);

        ivPicture = (ImageView) findViewById(R.id.iv_picture);


        ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(this));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_pick:
                contentManager.pickContent(ContentManager.Content.IMAGE);
                break;
            case R.id.btn_take:
                contentManager.takePhoto();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        contentManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        contentManager.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        contentManager.onSaveInstanceState(outState);
    }

    @Override
    public void onContentLoaded(Uri uri, String contentType) {
        ImageLoader.getInstance().displayImage(uri.toString(), ivPicture);
    }

    @Override
    public void onLoadContentProgress(int loadPercent) {
        //Show loader or something like that
    }

    @Override
    public void onError(String error) {
        //Show error
    }

    @Override
    public void onCanceled() {
        //User canceled
    }
}

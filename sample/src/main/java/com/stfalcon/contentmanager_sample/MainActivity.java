package com.stfalcon.contentmanager_sample;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

        //Create instance of ContentManager
        contentManager = new ContentManager(this, this);

        //Init views
        Button btnTake = (Button) findViewById(R.id.btn_take);
        Button btnPick = (Button) findViewById(R.id.btn_pick);
        ivPicture = (ImageView) findViewById(R.id.iv_picture);

        //Set on click listeners for buttons
        btnTake.setOnClickListener(this);
        btnPick.setOnClickListener(this);

        //Must be in Application class, but it is just for sample
        ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(this));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_pick:
                //Pick photo from gallery or cloud
                contentManager.pickContent(ContentManager.Content.IMAGE);
                break;
            case R.id.btn_take:
                //Take photo from camera
                contentManager.takePhoto();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Need for handle result
        contentManager.onActivityResult(requestCode, resultCode, data);
    }


    /**
     * Need for fix bug with some Samsung and Sony devices, when taking photo in landscape mode
     */
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
    /**
     *--------------------------------------------------------------------
     */


    /**
     * Success result callback
     *
     * @param uri         Content uri
     * @param contentType If you pick content can be Image, File or Video, if take only Image
     */
    @Override
    public void onContentLoaded(Uri uri, String contentType) {
        if (contentType.equals(ContentManager.Content.IMAGE.toString())) {
            //You can use any library for display image Fresco, Picasso, ImageLoader
            //For sample:
            ImageLoader.getInstance().displayImage(uri.toString(), ivPicture);
        }  else {
            //handle video result if needed
        }
    }

    /**
     * Return progress of load content from cloud
     *
     * @param loadPercent load progress in percent
     */
    @Override
    public void onLoadContentProgress(int loadPercent) {
        //Show loader or something like that
    }

    /**
     * Call if have some problem with getting content
     *
     * @param error message
     */
    @Override
    public void onError(String error) {
        //Show error
    }

    /**
     * Call if user manual cancel picking or taking content
     */
    @Override
    public void onCanceled() {
        //User canceled
    }

    /**
     * need for real time permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        contentManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}

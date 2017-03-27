package com.stfalcon.contentmanager_sample;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.stfalcon.contentmanager.ContentManager;

import java.io.File;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, ContentManager.PickContentListener {

    private ImageView ivPicture;
    private TextView tvUri;
    private ProgressBar progressBar;
    private String filePath;
    private ContentManager contentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Create instance of ContentManager
        contentManager = new ContentManager(this, this);

        //Init views
        progressBar = (ProgressBar) findViewById(R.id.progress);
        Button btnTake = (Button) findViewById(R.id.btn_take);
        Button btnPickImage = (Button) findViewById(R.id.btn_pick_image);
        Button btnPickFile = (Button) findViewById(R.id.btn_pick_file);
        ivPicture = (ImageView) findViewById(R.id.iv_picture);
        tvUri = (TextView) findViewById(R.id.tv_uri);
        tvUri.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (filePath != null) {
                    MimeTypeMap myMime = MimeTypeMap.getSingleton();
                    Intent newIntent = new Intent(Intent.ACTION_VIEW);
                    String substring = fileExt(filePath);
                    String mimeType = myMime.getMimeTypeFromExtension(substring);
                    newIntent.setDataAndType(Uri.fromFile(new File(filePath)), mimeType);
                    newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        MainActivity.this.startActivity(newIntent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(MainActivity.this, "No handler for this type of file.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });


        //Set on click listeners for buttons
        btnTake.setOnClickListener(this);
        btnPickImage.setOnClickListener(this);
        btnPickFile.setOnClickListener(this);

        //Must be in Application class, but it is just for sample
        ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(this));
    }

    private String fileExt(String url) {
        if (url.indexOf("?") > -1) {
            url = url.substring(0, url.indexOf("?"));
        }
        if (url.lastIndexOf(".") == -1) {
            return null;
        } else {
            String ext = url.substring(url.lastIndexOf(".") + 1);
            if (ext.indexOf("%") > -1) {
                ext = ext.substring(0, ext.indexOf("%"));
            }
            if (ext.indexOf("/") > -1) {
                ext = ext.substring(0, ext.indexOf("/"));
            }
            return ext.toLowerCase();

        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_pick_image:
                //Pick photo from gallery or cloud
                contentManager.pickContent(ContentManager.Content.IMAGE);
                break;
            case R.id.btn_take:
                //Take photo from camera
                contentManager.takePhoto();
                break;
            case R.id.btn_pick_file:
                //Pick file with any type
                contentManager.pickContent(ContentManager.Content.FILE);
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

    /*
     *------------------Callbacks------------------------------
     */

    /**
     * Call when loading started
     */
    @Override
    public void onStartContentLoading() {
        //Show loader or something like that
        progressBar.setVisibility(View.VISIBLE);
    }

    /**
     * Success result callback
     *
     * @param uri         Content uri
     * @param contentType If you pick content can be Image, File or Video, if take only Image
     */
    @Override
    public void onContentLoaded(Uri uri, String contentType) {
        progressBar.setVisibility(View.GONE);
        if (contentType.equals(ContentManager.Content.IMAGE.toString())) {
            //You can use any library for display image Fresco, Picasso, ImageLoader
            //For sample:
            ImageLoader.getInstance().displayImage(uri.toString(), ivPicture);
            tvUri.setVisibility(View.GONE);
            ivPicture.setVisibility(View.VISIBLE);
        } else if (contentType.equals(ContentManager.Content.FILE.toString())) {
            //Show file path in textView
            tvUri.setText(getString(R.string.tap_to_open, uri.toString()));
            filePath = uri.toString();
            tvUri.setVisibility(View.VISIBLE);
            ivPicture.setVisibility(View.GONE);
        }
    }

    /**
     * Call if have some problem with getting content
     *
     * @param error message
     */
    @Override
    public void onError(String error) {
        progressBar.setVisibility(View.GONE);
        //Show error
    }

    /**
     * Call if user manual cancel picking or taking content
     */
    @Override
    public void onCanceled() {
        progressBar.setVisibility(View.GONE);
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

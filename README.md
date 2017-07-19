# ContentManager
Library for getting photos, videos or files of any type from a device gallery, external storage, cloud(Google Drive, Dropbox and etc) or camera. With asynchronous load from the cloud and fixed bugs for some problem devices like Samsung or Sony.

### Who we are
Need iOS and Android apps, MVP development or prototyping? Contact us via info@stfalcon.com. We develop software since 2009, and we're known experts in this field. Check out our [portfolio](https://stfalcon.com/en/portfolio) and see more libraries from [stfalcon-studio](https://stfalcon-studio.github.io/).

### Download

Download via Gradle:
```gradle
compile 'com.github.stfalcon:contentmanager:0.5'
```

or Maven:
```xml
<dependency>
  <groupId>com.github.stfalcon</groupId>
  <artifactId>contentmanager</artifactId>
  <version>0.5</version>
  <type>pom</type>
</dependency>
```

### Migration to version 0.5

In version 0.5 we have removed callback ```onLoadContentProgress(int loadPercent)```(because it is very hard to calculate loadPercent correctly) and replaced it with callback ```onStartContentLoading()``` to handle a start of loading content. So if you are using ContentManager previous version, you need to make some correction after updating ContentManager version to 0.5.
Also, we have added new cool feature: picking files with any types.

### Usage

Add the folowing permission to AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

Implement callback interface:
```java
public class MainActivity extends AppCompatActivity implements ContentManager.PickContentListener {
```

Then implement PickContentListener methods:
```java
/**
* Success result callback
*
* @param uri         Content uri
* @param contentType If you pick content can be Image or Video, if take - only Image
*/
@Override
public void onContentLoaded(Uri uri, String contentType) {
   if (contentType.equals(ContentManager.Content.IMAGE.toString())) {
       //You can use any library for display image Fresco, Picasso, ImageLoader
       //For sample:
       ImageLoader.getInstance().displayImage(uri.toString(), ivPicture);
   } else if (contentType.equals(ContentManager.Content.FILE.toString())) {
       //handle file result
       tvUri.setText(uri.toString());
   }
}
        
/**
* Call when loading started
*/
@Override
public void onStartContentLoading() {
  //Show loader or something like that
  progressBar.setVisibility(View.VISIBLE);
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
```


Declare field:
```java
private ContentManager contentManager;
```

Create instance where "this" is your activity:
```java
contentManager = new ContentManager(this, this);
```

Override onActivityResult method of activity. It is needed for handling the result:
```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
  super.onActivityResult(requestCode, resultCode, data);
  contentManager.onActivityResult(requestCode, resultCode, data);
}
```

Override onRequestPermissionsResult method to handle realtime permissions:
```java
@Override
public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    contentManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
}
```

Override onSaveInstanceState, onRestoreInstanceState. It is needed for fixing bugs with some Samsung and Sony devices when taking photo in a landscape mode:
```java
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
```

Pick image: 
```java
contentManager.pickContent(ContentManager.Content.IMAGE);
```

Pick video:
```jave
contentManager.pickContent(ContentManager.Content.VIDEO);
```

Pick file:
```java
contentManager.pickContent(ContentManager.Content.FILE);
```

Take photo from camera:
```
contentManager.takePhoto();
```

Take a look at the [sample project](sample) for more information

### Thanks
Thanks to [@coomar2841](https://github.com/coomar2841) and his [Android Multipicker Library](https://github.com/coomar2841/android-multipicker-library). We peeked at him some points in the implementation of picking files.

### License 

```
Copyright 2017 stfalcon.com

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```



[sample]: <https://github.com/stfalcon-studio/ContentManager/tree/master/sample>




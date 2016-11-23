# ContentManager
Library for getting photo or video from a device gallery, cloud or camera. With asynchronous load from the cloud and fixed bugs for some problem devices like Samsung or Sony.

### Download

Download via Gradle:
```gradle
compile 'com.github.stfalcon:contentmanager:0.4.2'
```

or Maven:
```xml
<dependency>
  <groupId>com.github.stfalcon</groupId>
  <artifactId>contentmanager</artifactId>
  <version>0.4.2</version>
  <type>pom</type>
</dependency>
```

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
   } else {
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
If you select content from cloud, it will be loaded asynchronously and you can track progress with onLoadContentProgress method.

Take photo from camera:
```
contentManager.takePhoto();
```

Take a look at [Sample projects] [sample] for more information

### License 

Copyright 2016 stfalcon.com

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.



[sample]: <https://github.com/stfalcon-studio/ContentManager/tree/master/sample>




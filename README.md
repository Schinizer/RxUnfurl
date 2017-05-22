# RxUnfurl

<img src='https://giant.gfycat.com/WearyNecessaryFattaileddunnart.gif' height="400" align="right" hspace="20"/>

[![Download](https://api.bintray.com/packages/schinizer/maven/RxUnfurl/images/download.svg) ](https://bintray.com/schinizer/maven/RxUnfurl/_latestVersion)
[![Build Status](https://travis-ci.org/Schinizer/RxUnfurl.svg?branch=develop)](https://travis-ci.org/Schinizer/RxUnfurl)

A reactive extension to generate URL previews.

This library parses available open graph data from the provided url and returns them as a simple model class. Otherwise, the library searches for other fallbacks and returns them instead.

Image dimensions are read from its uri by fetching the required bytes and then sorted according to their resolution.

## Gradle dependency
To use the library, add the following dependency to your `build.gradle`
```groovy
dependencies {
	compile 'com.schinizer:rxunfurl:0.3.0'
}
```

## RxJava 2 notice
`RxUnfurl.generatePreview()` is now a `Single` instead of an `Observable`

RxJava 1 is dropped entirely and no longer supported.

## Usage
To generate previews, simply subscribe to `RxUnfurl.generatePreview(yourURL)`.

If you are on android, you will need `RxAndroid` to subscribe to network calls on another thread.
```Java
OkHttpClient okhttpClient = new OkHttpClient();

RxUnfurl inst = new RxUnfurl.Builder()
		.client(okhttpClient) // You can supply your okhttp client here
		.scheduler(Schedulers.io())
		.build();
		
inst.generatePreview("http://9gag.com")
    .observeOn(AndroidSchedulers.mainThread())
    .subscribeWith(new DisposableSingleObserver<PreviewData>() {
        @Override
        public void onSuccess(PreviewData previewData) { // Observable has been changed to Single

        }

        @Override
        public void onError(Throwable e) {

        }
    });
```

## Sample App
You may find a sample android implementation in `/app`.

# License
```
Copyright 2016 Schinizer

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

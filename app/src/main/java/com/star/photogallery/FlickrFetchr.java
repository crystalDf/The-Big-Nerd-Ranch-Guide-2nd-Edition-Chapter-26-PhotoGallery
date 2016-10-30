package com.star.photogallery;


import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.star.photogallery.model.Photo;
import com.star.photogallery.model.Recent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetchr {

    private static final String TAG = "FlickrFetchr";

    private static final String API_KEY = "d68bdef910c8657f4cbb1332f196a6c3";

    private static final String GET_RECENT_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";

    private static final String METHOD_KEY = "method";
    private static final String API_KEY_KEY = "api_key";
    private static final String API_KEY_VALUE = API_KEY;
    private static final String FORMAT_KEY = "format";
    private static final String FORMAT_VALUE = "json";
    private static final String NO_JSON_CALL_BACK_KEY = "nojsoncallback";
    private static final String NO_JSON_CALL_BACK_VALUE = "1";
    private static final String EXTRAS_KEY = "extras";
    private static final String EXTRAS_VALUE = "url_s";
    private static final String PAGE_KEY = "page";

    private static final int START_PAGE = 1;

    private static final String TEXT = "text";

    private static final Uri ENDPOINT = Uri
            .parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter(API_KEY_KEY, API_KEY_VALUE)
            .appendQueryParameter(FORMAT_KEY, FORMAT_VALUE)
            .appendQueryParameter(NO_JSON_CALL_BACK_KEY, NO_JSON_CALL_BACK_VALUE)
            .appendQueryParameter(EXTRAS_KEY, EXTRAS_VALUE)
            .build();

    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = httpURLConnection.getInputStream();

            if (httpURLConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(httpURLConnection.getResponseMessage() +
                        ": with " + urlSpec);
            }

            int bytesRead;
            byte[] buffer = new byte[1024];

            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }

            out.close();
            return out.toByteArray();
        } finally {
            httpURLConnection.disconnect();
        }
    }

    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    public List<GalleryItem> getRecentPhotos(int page) {
        String url = buildUrl(GET_RECENT_METHOD, page, null);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> getRecentPhotos() {
        return getRecentPhotos(START_PAGE);
    }

    public List<GalleryItem> searchPhotos(int page, String query) {
        String url = buildUrl(SEARCH_METHOD, page, query);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos(String query) {
        return searchPhotos(START_PAGE, query);
    }

    private String buildUrl(String method, int page, String query) {
        Uri.Builder uriBuilder = ENDPOINT.buildUpon()
                .appendQueryParameter(METHOD_KEY, method)
                .appendQueryParameter(PAGE_KEY, page + "");

        if (method.equals(SEARCH_METHOD)) {
            uriBuilder.appendQueryParameter(TEXT, query);
        }

        return uriBuilder.build().toString();
    }

    public List<GalleryItem> downloadGalleryItems(String url) {

        List<GalleryItem> items = new ArrayList<>();

        try {

            String jsonString = getUrlString(url);

            Log.i(TAG, "Received JSON: " + jsonString);

            parseItems(items, jsonString);
        } catch (IOException e) {
            Log.e(TAG, "Failed to fetch items", e);
        }

        return items;
    }

    private void parseItems(List<GalleryItem> items, String jsonString) {
        Gson gson = new Gson();

        Recent recent = gson.fromJson(jsonString, Recent.class);

        List<Photo> photos = recent.getPhotos().getPhoto();

        for (int i = 0; i < photos.size(); i++) {
            Photo photo = photos.get(i);

            GalleryItem item = new GalleryItem();
            item.setId(photo.getId());
            item.setCaption(photo.getTitle());

            if (photo.getUrlS() != null) {
                item.setUrl(photo.getUrlS());
                items.add(item);
            }
        }
    }

}

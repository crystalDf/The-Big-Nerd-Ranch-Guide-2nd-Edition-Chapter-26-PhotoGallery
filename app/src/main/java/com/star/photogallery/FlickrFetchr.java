package com.star.photogallery;


import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetchr {

    private static final String TAG = "FlickrFetchr";

    private static final String API_KEY = "03e55c312c15c20d0b02b48dbf58e646";

    private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";

    private static final String METHOD_KEY = "method";
    private static final String METHOD_VALUE = "flickr.photos.getRecent";
    private static final String API_KEY_KEY = "api_key";
    private static final String API_KEY_VALUE = API_KEY;
    private static final String FORMAT_KEY = "format";
    private static final String FORMAT_VALUE = "json";
    private static final String NO_JSON_CALL_BACK_KEY = "nojsoncallback";
    private static final String NO_JSON_CALL_BACK_VALUE = "1";
    private static final String EXTRAS_KEY = "extras";
    private static final String EXTRAS_VALUE = "url_s";
    private static final String TEXT = "text";

    private static final Uri ENDPOINT = Uri
            .parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter(METHOD_KEY, METHOD_VALUE)
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

            if (httpURLConnection.getResponseCode() != httpURLConnection.HTTP_OK) {
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

    public List<GalleryItem> fetchRecentPhotos() {
        String url = buildUrl(FETCH_RECENTS_METHOD, null);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos(String query) {
        String url = buildUrl(SEARCH_METHOD, query);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> downloadGalleryItems(String url) {

        List<GalleryItem> items = new ArrayList<>();

        try {
//            String url = Uri.parse("https://api.flickr.com/services/rest/")
//                    .buildUpon()
//                    .appendQueryParameter(METHOD_KEY, METHOD_VALUE)
//                    .appendQueryParameter(API_KEY_KEY, API_KEY_VALUE)
//                    .appendQueryParameter(FORMAT_KEY, FORMAT_VALUE)
//                    .appendQueryParameter(NO_JSON_CALL_BACK_KEY, NO_JSON_CALL_BACK_VALUE)
//                    .appendQueryParameter(EXTRAS_KEY, EXTRAS_VALUE)
//                    .build().toString();

            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);
            JSONObject jsonBody = new JSONObject(jsonString);
            parseItems(items, jsonBody);
        } catch (IOException e) {
            Log.e(TAG, "Failed to fetch items", e);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse JSON", e);
        }

        return items;
    }

    private String buildUrl(String method, String query) {
        Uri.Builder uriBuilder = ENDPOINT.buildUpon().appendQueryParameter(METHOD_KEY, method);

        if (method.equals(SEARCH_METHOD)) {
            uriBuilder.appendQueryParameter(TEXT, query);
        }

        return uriBuilder.build().toString();
    }

    private void parseItems(List<GalleryItem> items, JSONObject jsonBody) throws JSONException {
        JSONObject photosJSONObject = jsonBody.getJSONObject("photos");
        JSONArray photoJSONArray = photosJSONObject.getJSONArray("photo");

        for (int i = 0; i < photoJSONArray.length(); i++) {
            JSONObject photoJSONObject = photoJSONArray.getJSONObject(i);

            GalleryItem item = new GalleryItem();
            item.setId(photoJSONObject.getString("id"));
            item.setCaption(photoJSONObject.getString("title"));

            if (!photoJSONObject.has("url_s")) {
                continue;
            }

            item.setUrl(photoJSONObject.getString("url_s"));

            items.add(item);
        }

    }
}

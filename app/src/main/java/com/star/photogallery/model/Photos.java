package com.star.photogallery.model;


import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Photos {

    @SerializedName("photo")
    private List<Photo> mPhoto;

    @SerializedName("page")
    private int mPage;

    public List<Photo> getPhoto() {
        return mPhoto;
    }

    public void setPhoto(List<Photo> photo) {
        mPhoto = photo;
    }

    public int getPage() {
        return mPage;
    }

    public void setPage(int page) {
        mPage = page;
    }

}

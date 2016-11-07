package com.star.photogallery;


import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.squareup.picasso.Picasso;

import java.util.List;

import static com.star.photogallery.PollJobService.isServiceScheduledOn;


public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "PhotoGalleryFragment";

    private static final int DEFAULT_COLUMN_NUM = 3;
    private static final int ITEM_WIDTH = 100;

    private RecyclerView mPhotoRecyclerView;
    private GridLayoutManager mGridLayoutManager;
    private List<GalleryItem> mGalleryItems;

    private SearchView mSearchView;

    private ProgressBar mProgressBar;

    private int mCurrentPage;
    private int mFetchedPage;
    private int mCurrentPosition;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        updateItems();

        Log.i(TAG, "Background thread started");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPhotoRecyclerView = (RecyclerView)
                view.findViewById(R.id.fragment_photo_gallery_recycler_view);

        mProgressBar = (ProgressBar) view.findViewById(R.id.fragment_progress_bar);

        mGridLayoutManager = new GridLayoutManager(getActivity(), DEFAULT_COLUMN_NUM);

        mPhotoRecyclerView.setLayoutManager(mGridLayoutManager);

        mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {

                        int spanCount = convertPxToDp(mPhotoRecyclerView.getWidth()) / ITEM_WIDTH;
                        mGridLayoutManager.setSpanCount(spanCount);
                    }
                });

        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                updateCurrentPage();
            }
        });

        if (savedInstanceState != null) {
            showProgressBar(false);
        } else {
            showProgressBar(true);
        }

        setupAdapter();
        
        return view;
    }

    private int convertPxToDp(float sizeInPx) {
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();

        return (int) (sizeInPx / displayMetrics.density);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.fragment_photo_gallery, menu);

        final MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "QueryTextSubmit: " + query);

                QueryPreferences.setStoredQuery(getActivity(), query);

                showProgressBar(true);

                updateItems();

                mSearchView.onActionViewCollapsed();

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "QueryTextChange: " + newText);

                return true;
            }
        });

        mSearchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                mSearchView.setQuery(query, false);
            }
        });

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);

        if (isServiceOn(getActivity())) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }

    }

    private boolean isServiceOn(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return isServiceScheduledOn(context);
        } else {
            return PollIntentService.isServiceAlarmOn(context);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);

                showProgressBar(true);

                updateItems();

                mSearchView.onActionViewCollapsed();

                return true;
            case R.id.menu_item_toggle_polling:
                setService(getActivity());

                getActivity().supportInvalidateOptionsMenu();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void setService(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            boolean shouldStartSchedule = !isServiceScheduledOn(context);

            PollJobService.setServiceScheduled(getActivity(), shouldStartSchedule);

        } else {
            boolean shouldStartAlarm = !PollIntentService.isServiceAlarmOn(context);

            PollIntentService.setServiceAlarm(getActivity(), shouldStartAlarm);
        }
    }

    private void updateItems() {
        init();

        String query = QueryPreferences.getStoredQuery(getActivity());

        new FetchItemsTask(query).execute(mCurrentPage);
    }

    private void init() {
        mCurrentPage = 1;
        mFetchedPage = 0;
        mCurrentPosition = 0;

        if (mGalleryItems != null) {
            mGalleryItems.clear();
            mGalleryItems = null;
        }

    }

    private void setupAdapter() {
        if (isAdded()) {
            if (mGalleryItems != null) {
                mPhotoRecyclerView.setAdapter(new PhotoAdapter(mGalleryItems));
            } else {
                mPhotoRecyclerView.setAdapter(null);
            }
            mPhotoRecyclerView.scrollToPosition(mCurrentPosition);
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {

        private ImageView mItemImageView;

        public PhotoHolder(View itemView) {
            super(itemView);

            mItemImageView = (ImageView)
                    itemView.findViewById(R.id.fragment_photo_gallery_image_view);
        }

        public void bindGalleryItem(GalleryItem galleryItem) {
            Picasso.with(getActivity())
                    .load(galleryItem.getUrl())
                    .placeholder(R.drawable.emma)
                    .into(mItemImageView);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, parent, false);

            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);

            photoHolder.bindGalleryItem(galleryItem);

        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    private class FetchItemsTask extends AsyncTask<Integer, Void, List<GalleryItem>> {

        private String mQuery;

        public FetchItemsTask(String query) {
            mQuery = query;
        }

        @Override
        protected List<GalleryItem> doInBackground(Integer... params) {

            if (mQuery == null) {
                return new FlickrFetchr().getRecentPhotos(params[0]);
            } else {
                return new FlickrFetchr().searchPhotos(params[0], mQuery);
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            if (mGalleryItems == null) {
                mGalleryItems = items;
            } else {
                if (items != null) {
                    mGalleryItems.addAll(items);
                }
            }

            mFetchedPage++;

            showProgressBar(false);

            setupAdapter();

        }
    }

    private void updateCurrentPage() {
        int firstVisibleItemPosition = mGridLayoutManager.findFirstVisibleItemPosition();
        int lastVisibleItemPosition = mGridLayoutManager.findLastVisibleItemPosition();

        if (lastVisibleItemPosition == (mGridLayoutManager.getItemCount() - 1) &&
                mCurrentPage == mFetchedPage ) {
            mCurrentPosition = firstVisibleItemPosition + 3;
            mCurrentPage++;

            String query = QueryPreferences.getStoredQuery(getActivity());

            new FetchItemsTask(query).execute(mCurrentPage);
        }
    }

    private void showProgressBar(boolean isShown) {
        if (isShown) {
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }

}

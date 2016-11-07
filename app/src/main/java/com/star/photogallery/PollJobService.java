package com.star.photogallery;


import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class PollJobService extends JobService {

    private static final String TAG = "PollJobService";

    private static final int JOB_ID = 1;

    private static final long POLL_INTERVAL = 1000 * 60;

    private PollTask mCurrentTask;

    @Override
    public boolean onStartJob(JobParameters params) {

        mCurrentTask = new PollTask();
        mCurrentTask.execute(params);

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {

        if (mCurrentTask != null) {
            mCurrentTask.cancel(true);
        }

        return true;
    }

    public static void setServiceScheduled(Context context, boolean isOn) {
        JobScheduler jobScheduler = (JobScheduler)
                context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if (isOn) {
            JobInfo jobInfo = new JobInfo.Builder(JOB_ID,
                    new ComponentName(context, PollJobService.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                    .setPeriodic(POLL_INTERVAL)
                    .setPersisted(true)
                    .build();

            jobScheduler.schedule(jobInfo);
        } else {
            jobScheduler.cancel(JOB_ID);
        }
    }

    public static boolean isServiceScheduledOn(Context context) {
        JobScheduler jobScheduler = (JobScheduler)
                context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        boolean hasBeenScheduled = false;

        for (JobInfo jobInfo : jobScheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == JOB_ID) {
                hasBeenScheduled = true;
            }
        }

        return hasBeenScheduled;
    }

    private class PollTask extends AsyncTask<JobParameters, Void, Void> {

        @Override
        protected Void doInBackground(JobParameters... params) {

            JobParameters jobParameters = params[0];

            Log.i(TAG, "Poll Flickr for new images");

            if (!isNetworkAvailableAndConnected()) {
                return null;
            }

            String query = QueryPreferences.getStoredQuery(PollJobService.this);
            String lastResultId = QueryPreferences.getLastResultId(PollJobService.this);

            List<GalleryItem> items;

            if (query == null) {
                items = new FlickrFetchr().getRecentPhotos();
            } else {
                items = new FlickrFetchr().searchPhotos(query);
            }

            if (items.size() == 0) {
                return null;
            }

            String resultId = items.get(0).getId();
            if (resultId.equals(lastResultId)) {
                Log.i(TAG, "Got an old result: " + resultId);
            } else {
                Log.i(TAG, "Got an new result: " + resultId);

                Resources resources = getResources();
                Intent i = PhotoGalleryActivity.newIntent(PollJobService.this);
                PendingIntent pendingIntent =
                        PendingIntent.getActivity(PollJobService.this, 0, i, 0);

                Notification notification = new NotificationCompat.Builder(PollJobService.this)
                        .setTicker(resources.getString(R.string.new_pictures_title))
                        .setSmallIcon(android.R.drawable.ic_menu_report_image)
                        .setContentTitle(resources.getString(R.string.new_pictures_title))
                        .setContentText(resources.getString(R.string.new_pictures_text))
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build();

                NotificationManagerCompat notificationManagerCompat =
                        NotificationManagerCompat.from(PollJobService.this);
                notificationManagerCompat.notify(0, notification);
            }

            QueryPreferences.setLastResultId(PollJobService.this, resultId);

            jobFinished(jobParameters, false);

            return null;
        }
    }

    private boolean isNetworkAvailableAndConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                getSystemService(CONNECTIVITY_SERVICE);

        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}

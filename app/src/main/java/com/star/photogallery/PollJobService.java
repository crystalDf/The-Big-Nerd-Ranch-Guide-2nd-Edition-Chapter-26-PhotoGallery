package com.star.photogallery;


import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

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

            new PollServiceUtils(TAG).pollFlickr(PollJobService.this);

            jobFinished(jobParameters, false);

            return null;
        }
    }

}

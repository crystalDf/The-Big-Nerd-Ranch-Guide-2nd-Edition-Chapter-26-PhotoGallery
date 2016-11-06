package com.star.photogallery;


import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class PollJobService extends JobService {

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

    private class PollTask extends AsyncTask<JobParameters, Void, Void> {

        @Override
        protected Void doInBackground(JobParameters... params) {

            JobParameters jobParameters = params[0];

            jobFinished(jobParameters, false);

            return null;
        }
    }
}

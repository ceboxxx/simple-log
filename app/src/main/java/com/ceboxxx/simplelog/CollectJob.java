package com.ceboxxx.simplelog;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class CollectJob extends JobService {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private Future<?> task;
    static void schedule(Context c) {
        JobScheduler scheduler = c.getSystemService(JobScheduler.class);
        if (scheduler.getPendingJob(1) == null) {
            scheduler.schedule(new JobInfo.Builder(1, new ComponentName(c, CollectJob.class))
                .setPeriodic(15 * 60 * 1000L).build());
        }
    }
    @Override public boolean onStartJob(JobParameters params) {
        task = worker.submit(() -> {
            boolean retry = false;
            try { LogStore.collect(getApplicationContext()); }
            catch (RuntimeException error) { retry = true; }
            jobFinished(params, retry);
        });
        return true;
    }
    @Override public boolean onStopJob(JobParameters params) {
        if (task != null) task.cancel(true);
        return true;
    }
    @Override public void onDestroy() { worker.shutdownNow(); super.onDestroy(); }
}

/*
 * Copyright (C) 2017 Ace Explorer owned by Siju Sakaria
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.siju.acexplorer.storage.model.task;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.text.format.Formatter;
import android.util.Log;

import com.siju.acexplorer.R;
import com.siju.acexplorer.logging.Logger;
import com.siju.acexplorer.main.model.helper.FileOperations;
import com.siju.acexplorer.main.model.helper.FileUtils;
import com.siju.acexplorer.main.model.helper.MediaStoreHelper;
import com.siju.acexplorer.storage.model.operations.OperationProgress;
import com.siju.acexplorer.storage.modules.zip.ZipUtils;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.siju.acexplorer.main.model.helper.SdkHelper.isAtleastOreo;
import static com.siju.acexplorer.storage.model.operations.OperationUtils.ACTION_OP_FAILED;
import static com.siju.acexplorer.storage.model.operations.OperationUtils.ACTION_RELOAD_LIST;
import static com.siju.acexplorer.storage.model.operations.OperationUtils.KEY_END;
import static com.siju.acexplorer.storage.model.operations.OperationUtils.KEY_FILENAME;
import static com.siju.acexplorer.storage.model.operations.OperationUtils.KEY_FILEPATH;
import static com.siju.acexplorer.storage.model.operations.OperationUtils.KEY_FILEPATH2;
import static com.siju.acexplorer.storage.model.operations.OperationUtils.KEY_OPERATION;
import static com.siju.acexplorer.storage.model.operations.Operations.EXTRACT;
import static com.siju.acexplorer.storage.model.operations.ProgressUtils.EXTRACT_PROGRESS;
import static com.siju.acexplorer.storage.model.operations.ProgressUtils.KEY_COMPLETED;
import static com.siju.acexplorer.storage.model.operations.ProgressUtils.KEY_PROGRESS;
import static com.siju.acexplorer.storage.model.operations.ProgressUtils.KEY_TOTAL;
import static com.siju.acexplorer.storage.modules.zip.ZipUtils.EXT_TAR;
import static com.siju.acexplorer.storage.modules.zip.ZipUtils.EXT_TAR_GZ;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ExtractService extends Service {
    private static final String TAG = "ExtractService";

    private final int    NOTIFICATION_ID = 1000;
    private final String CHANNEL_ID      = "operation";
    private Context                    context;
    private NotificationManager        notificationManager;
    private NotificationCompat.Builder builder;
    private long copiedbytes = 0, totalbytes = 0;
    private ServiceHandler serviceHandler;
    private boolean        stopService;
    private boolean        isCompleted;


    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        createNotification();
        startThread();
    }

    private void createNotification() {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
        createChannelId();
        builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        builder.setContentTitle(getResources().getString(R.string.extracting))
                .setSmallIcon(R.drawable.ic_doc_compressed);
        builder.setOnlyAlertOnce(true);
        builder.setDefaults(0);
        Intent cancelIntent = new Intent(context, ExtractService.class);
        cancelIntent.setAction(OperationProgress.ACTION_STOP);
        PendingIntent pendingCancelIntent =
                PendingIntent.getService(context, NOTIFICATION_ID, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(new NotificationCompat.Action(R.drawable.ic_cancel, getString(R.string.dialog_cancel), pendingCancelIntent));

        Notification notification = builder.build();
        startForeground(NOTIFICATION_ID, notification);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createChannelId() {
        if (isAtleastOreo()) {
            CharSequence name = getString(R.string.operation);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void startThread() {
        HandlerThread thread = new HandlerThread("ExtractService",
                                                 Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        Looper serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.log(TAG, "onStartCommand: " + intent + "startId:" + startId);
        if (intent == null) {
            Logger.log(this.getClass().getSimpleName(), "Null intent");
            stopService();
            return START_NOT_STICKY;
        }
        String action = intent.getAction();
        if (action != null && action.equals(OperationProgress.ACTION_STOP)) {
            stopService = true;
            stopSelf();
            return START_NOT_STICKY;
        }
        String file = intent.getStringExtra(KEY_FILEPATH);
        String newFile = intent.getStringExtra(KEY_FILEPATH2);

        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        Bundle bundle = new Bundle();
        bundle.putString(KEY_FILEPATH, file);
        bundle.putString(KEY_FILEPATH2, newFile);
        msg.setData(bundle);
        serviceHandler.sendMessage(msg);
        return START_STICKY;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void stopService() {
        stopSelf();
    }

    private final class ServiceHandler extends Handler {

        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.log(TAG, "handleMessage: " + msg.arg1);
            Bundle bundle = msg.getData();
            String file = bundle.getString(KEY_FILEPATH);
            String newFile = bundle.getString(KEY_FILEPATH2);
            start(file, newFile);
            stopSelf();
        }


    }

    private void start(String zipFilePath, String newFile) {
        if (zipFilePath != null) {
            File zipFile = new File(zipFilePath);
            if (ZipUtils.isZipViewable(zipFilePath)) {
                extract(zipFile, newFile);
            } else if (zipFilePath.toLowerCase().endsWith(EXT_TAR) || zipFile.getName().toLowerCase().endsWith
                    (EXT_TAR_GZ)) {
                extractTar(zipFile, newFile);
            }
        }

        Logger.log(TAG, "ZIp file=" + zipFilePath + "new file=" + newFile);
    }

    private void extract(File archive, String destinationPath) {
        try {
            ArrayList<ZipEntry> arrayList = new ArrayList<>();
            ZipFile zipfile = new ZipFile(archive);
            for (Enumeration e = zipfile.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                arrayList.add(entry);
            }
            for (ZipEntry entry : arrayList) {
                totalbytes = totalbytes + entry.getSize();
            }
            for (ZipEntry entry : arrayList) {
                if (stopService) {
                    publishCompletedResult();
                    break;
                }
                unzipEntry(zipfile, entry, destinationPath);
            }
            Intent intent = new Intent(ACTION_RELOAD_LIST);
            intent.putExtra(KEY_OPERATION, EXTRACT);
            sendBroadcast(intent);
            calculateProgress(archive.getName(), copiedbytes, totalbytes);
            zipfile.close();
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "Error while extracting file " + archive, e);
            Intent intent = new Intent(ACTION_OP_FAILED);
            intent.putExtra(KEY_OPERATION, EXTRACT);
            sendBroadcast(intent);
            publishResults(archive.getName(), 100, totalbytes, copiedbytes);
        }
    }

    private void extractTar(File archive, String destinationPath) {
        try {
            ArrayList<TarArchiveEntry> archiveEntries = new ArrayList<>();
            TarArchiveInputStream inputStream;
            if (archive.getName().endsWith(EXT_TAR)) {
                inputStream = new TarArchiveInputStream(new BufferedInputStream(new FileInputStream(archive)));
            } else {
                inputStream = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(archive)));
            }
            publishResults(archive.getName(), 0, totalbytes, copiedbytes);
            TarArchiveEntry tarArchiveEntry = inputStream.getNextTarEntry();
            while (tarArchiveEntry != null) {
                if (stopService) {
                    publishCompletedResult();
                    break;
                }
                archiveEntries.add(tarArchiveEntry);
                tarArchiveEntry = inputStream.getNextTarEntry();
            }
            for (TarArchiveEntry entry : archiveEntries) {
                totalbytes = totalbytes + entry.getSize();
            }
            for (TarArchiveEntry entry : archiveEntries) {
                unzipTAREntry(inputStream, entry, destinationPath, archive.getName());
            }

            inputStream.close();

            Intent intent = new Intent(ACTION_RELOAD_LIST);
            intent.putExtra(KEY_OPERATION, EXTRACT);
            sendBroadcast(intent);
            publishResults(archive.getName(), 100, totalbytes, copiedbytes);

        } catch (Exception e) {
            Intent intent = new Intent(ACTION_RELOAD_LIST);
            intent.putExtra(KEY_OPERATION, EXTRACT);
            sendBroadcast(intent);
            publishResults(archive.getName(), 100, totalbytes, copiedbytes);

        }

    }


    private void publishResults(String fileName, int progress, long total, long done) {
        builder.setContentTitle(getResources().getString(R.string.extracting));
        builder.setProgress(100, progress, false);
        builder.setOngoing(true);
        builder.setContentText(new File(fileName).getName() + " " + Formatter.formatFileSize
                (context, done) + "/" + Formatter.formatFileSize(context, total));
        notificationManager.notify(NOTIFICATION_ID, builder.build());
        if (progress == 100) {
            publishCompletedResult();
        }

        if (stopService) {
            endNotification();
        }

        Logger.log(ExtractService.this.getClass().getSimpleName(), "Progress=" + progress + " done=" + done + " total="
                + total);
        Intent intent = new Intent(EXTRACT_PROGRESS);
        intent.putExtra(KEY_PROGRESS, progress);
        intent.putExtra(KEY_COMPLETED, done);
        intent.putExtra(KEY_TOTAL, total);
        intent.putExtra(KEY_FILENAME, fileName);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void createDir(File dir) {
        FileOperations.INSTANCE.mkdir(dir);
    }


    private void calculateProgress(final String name, final long
            copiedbytes, final long totalbytes) {

        int progress = (int) ((copiedbytes / (float) totalbytes) * 100);
        publishResults(name, progress, totalbytes, copiedbytes);
    }


    private long time = System.nanoTime() / 500000000;

    private void unzipEntry(ZipFile zipfile, ZipEntry entry, String outputDir)
            throws Exception {
        if (entry.isDirectory()) {
            createDir(new File(outputDir, entry.getName()));
            return;
        }
        File outputFile = new File(outputDir, entry.getName());
        Logger.log(TAG, "unzipEntry: " + outputFile);
        if (!outputFile.getParentFile().exists()) {
            createDir(outputFile.getParentFile());
        }

        BufferedInputStream inputStream = new BufferedInputStream(
                zipfile.getInputStream(entry));
        BufferedOutputStream outputStream = new BufferedOutputStream(
                FileUtils.getOutputStream(outputFile, context));

        try {
            int len;
            byte buf[] = new byte[20480];
            while ((len = inputStream.read(buf)) > 0) {
                if (stopService) {
                    new File(outputDir).delete();
                    publishCompletedResult();
                    break;
                }

                outputStream.write(buf, 0, len);
                copiedbytes = copiedbytes + len;

                long time1 = System.nanoTime() / 500000000;
                if (((int) time1) > ((int) (time))) {
                    calculateProgress(zipfile.getName(), copiedbytes, totalbytes);
                    time = System.nanoTime() / 500000000;
                }
            }
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                //no-op
            }

            try {
                outputStream.close();
            } catch (IOException e) {
                //no-op
            }

        }

        MediaStoreHelper.scanFile(context, outputFile.getAbsolutePath());
    }

    private void unzipTAREntry(TarArchiveInputStream zipfile, TarArchiveEntry entry, String outputDir,
                               String fileName)
            throws Exception {
        String name = entry.getName();
        if (entry.isDirectory()) {
            createDir(new File(outputDir, name));
            return;
        }
        File outputFile = new File(outputDir, name);
        if (!outputFile.getParentFile().exists()) {
            createDir(outputFile.getParentFile());
        }

        BufferedOutputStream outputStream = new BufferedOutputStream(
                FileUtils.getOutputStream(outputFile, getBaseContext()));
        try {
            int len;
            byte buf[] = new byte[20480];
            while ((len = zipfile.read(buf)) > 0) {
                if (stopService) {
                    outputFile.delete();
                    publishCompletedResult();
                }
                outputStream.write(buf, 0, len);
                copiedbytes = copiedbytes + len;
                long time1 = System.nanoTime() / 500000000;
                if (((int) time1) > ((int) (time))) {
                    calculateProgress(fileName, copiedbytes, totalbytes);
                    time = System.nanoTime() / 500000000;
                }

            }
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                //close
            }

        }
    }

    private void dismissProgressDialog() {
        Intent intent = new Intent(EXTRACT_PROGRESS);
        intent.putExtra(KEY_END, true);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void endNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }



    private void publishCompletedResult() {
        if (isCompleted) {
            return;
        }
        isCompleted = true;
        endNotification();
        if (stopService) {
            dismissProgressDialog();
        }
    }
}



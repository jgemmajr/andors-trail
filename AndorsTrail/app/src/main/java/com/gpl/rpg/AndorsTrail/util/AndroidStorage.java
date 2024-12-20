package com.gpl.rpg.AndorsTrail.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import android.os.Handler;
import android.os.Looper;

import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.controller.Constants;
import com.gpl.rpg.AndorsTrail.util.BackgroundWorker.BackgroundWorkerCallback;
import com.gpl.rpg.AndorsTrail.view.CustomDialogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class AndroidStorage {
    public static File getStorageDirectory(Context context, String name) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return context.getExternalFilesDir(name);
        } else {
            File root = Environment.getExternalStorageDirectory();
            return new File(root, name);
        }
    }

    public static boolean shouldMigrateToInternalStorage(Context context) {
        boolean ret = false;
        File externalSaveGameDirectory = new File(Environment.getExternalStorageDirectory(),
                                                  Constants.FILENAME_SAVEGAME_DIRECTORY);
        File internalSaveGameDirectory = getStorageDirectory(context, Constants.FILENAME_SAVEGAME_DIRECTORY);

        if (externalSaveGameDirectory.exists()
            && externalSaveGameDirectory.isDirectory()
            && externalSaveGameDirectory.listFiles().length > 0
            && (!internalSaveGameDirectory.exists()
                || internalSaveGameDirectory.isDirectory()
                   && internalSaveGameDirectory.listFiles().length < 2)) {
            ret = true;
        }
        return ret;
    }

    public static boolean migrateToInternalStorage(Context context) {
        try {
            copy(new File(Environment.getExternalStorageDirectory(), Constants.CHEAT_DETECTION_FOLDER),
                 getStorageDirectory(context, Constants.CHEAT_DETECTION_FOLDER));
            copy(new File(Environment.getExternalStorageDirectory(), Constants.FILENAME_SAVEGAME_DIRECTORY),
                 getStorageDirectory(context, Constants.FILENAME_SAVEGAME_DIRECTORY));
        } catch (IOException e) {
            L.log("Error migrating data: " + e);
            return false;
        }
        return true;
    }

    private static void copy(File sourceLocation, File targetLocation) throws IOException {
        if (!sourceLocation.exists()) {
            return;
        }
        if (sourceLocation.isDirectory()) {
            copyDirectory(sourceLocation, targetLocation);
        } else {
            copyFile(sourceLocation, targetLocation);
        }
    }

    private static void copyDirectory(File source, File target) throws IOException {
        if (!target.exists()) {
            target.mkdir();
        }

        for (String f : source.list()) {
            copy(new File(source, f), new File(target, f));
        }
    }

    public static void copyFile(File source, File target) throws IOException {
        try (InputStream in = new FileInputStream(source); OutputStream out = new FileOutputStream(target)) {
            copyStream(in, out);
        }
    }

    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[1024];
        int length;
        while ((length = in.read(buf)) > 0) {
            out.write(buf, 0, length);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    public static void createZipDocumentFileFromFilesAsync(File[] files,
                                                           Context context,
                                                           DocumentFile targetDirectory,
                                                           String fileName,
                                                           String loadingMessage,
                                                           Consumer<Boolean> callback) {

        BackgroundWorker<Boolean> worker = new BackgroundWorker<>();
        CustomDialogFactory.CustomDialog progressDialog = getLoadingDialog(context, loadingMessage);
        progressDialog.setOnCancelListener(dialog -> worker.cancel());
        ContentResolver resolver = context.getContentResolver();
        Handler handler = Handler.createAsync(Looper.getMainLooper());


        worker.setTask(workerCallback -> {
            try {
                workerCallback.onInitialize();

                //region create zip file
                File zip = File.createTempFile("temp_worldmap", ".zip");
                try (OutputStream out = new FileOutputStream(zip)) {
                    ZipOutputStream zipOut = new ZipOutputStream(out);
                    for (int i = 0; i < files.length; i++) {
                        File file = files[i];
                        try (FileInputStream fis = new FileInputStream(file)) {
                            workerCallback.onProgress((float) i / files.length);
                            zipOut.putNextEntry(new ZipEntry(file.getName()));
                            copyStream(fis, zipOut);
                            zipOut.closeEntry();
                        }
                    }
                    zipOut.close();
                }
                //endregion

                DocumentFile worldmapZip = DocumentFile.fromFile(zip);
                DocumentFile worldmapTarget = targetDirectory.createFile("application/zip", fileName);
                if (worldmapTarget != null && worldmapTarget.exists()) {
                    AndroidStorage.copyDocumentFile(worldmapZip, resolver, worldmapTarget);
                    workerCallback.onComplete(true);
                } else {
                    throw new FileNotFoundException("Could not create File");
                }
            } catch (NullPointerException e) {
                if (worker.isCancelled()) {
                    workerCallback.onFailure(new CancellationException("Cancelled"));
                } else {
                    workerCallback.onFailure(e);
                }
            } catch (Exception e) {
                workerCallback.onFailure(e);
            }
        });

        worker.setCallback(getDefaultBackgroundWorkerCallback(handler, progressDialog, callback));
        worker.run();


    }

    public static void unzipToDirectory(File zipFile,
                                        File targetDirectory,
                                        boolean overwriteNotSkip) throws IOException {

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            unzipStreamToDirectory(targetDirectory, overwriteNotSkip, zis);
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    public static void unzipDocumentFileToDirectoryAsync(DocumentFile zipFile,
                                                         Context context,
                                                         File targetDirectory,
                                                         boolean overwriteNotSkip,
                                                         String loadingMessage,
                                                         Consumer<Boolean> callback) {

        BackgroundWorker<Boolean> worker = new BackgroundWorker<>();
        CustomDialogFactory.CustomDialog progressDialog = getLoadingDialog(context, loadingMessage);
        progressDialog.setOnCancelListener(dialog -> worker.cancel());
        ContentResolver resolver = context.getContentResolver();
        Handler handler = Handler.createAsync(Looper.getMainLooper());

        worker.setTask(workerCallback -> {
            try {
                workerCallback.onInitialize();
                workerCallback.onProgress(-1);//set dummy progress since we don't know the
                // progress of the unzip
                unzipDocumentFileToDirectory(zipFile, resolver, targetDirectory, overwriteNotSkip);
                workerCallback.onComplete(true);
            } catch (IOException e) {
                workerCallback.onFailure(e);
            }
        });

        worker.setCallback(getDefaultBackgroundWorkerCallback(handler, progressDialog, callback));
        worker.run();

    }

    public static void unzipDocumentFileToDirectory(DocumentFile zipFile,
                                                    ContentResolver resolver,
                                                    File targetDirectory,
                                                    boolean overwriteNotSkip) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(resolver.openInputStream(zipFile.getUri()))) {
            unzipStreamToDirectory(targetDirectory, overwriteNotSkip, zis);
        }
    }

    private static void unzipStreamToDirectory(File targetDirectory,
                                               boolean overwriteNotSkip,
                                               ZipInputStream zis) throws IOException {
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            File file = new File(targetDirectory, entry.getName());

            if (entry.isDirectory()) {
                file.mkdirs();
            } else {
                file.getParentFile().mkdirs();
                if (file.exists() && !overwriteNotSkip) {
                    continue;
                }

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    copyStream(zis, fos);
                }
            }
        }
    }

    public static void copyDocumentFileToNewOrExistingFile(DocumentFile sourceFile,
                                                           ContentResolver resolver,
                                                           DocumentFile targetFolder) throws IOException {
        copyDocumentFileToNewOrExistingFile(sourceFile,
                                            resolver,
                                            targetFolder,
                                            Constants.NO_FILE_EXTENSION_MIME_TYPE);
    }


    public static void copyDocumentFileToNewOrExistingFile(DocumentFile sourceFile,
                                                           ContentResolver resolver,
                                                           DocumentFile targetFolder,
                                                           String mimeType) throws IOException {
        String fileName = sourceFile.getName();
        DocumentFile file = targetFolder.findFile(fileName);
        if (file == null) {
            file = targetFolder.createFile(mimeType, fileName);
        }
        if (file == null) {
            return;
        }

        AndroidStorage.copyDocumentFile(sourceFile, resolver, file);
    }

    public static void copyDocumentFile(DocumentFile sourceFile,
                                        ContentResolver resolver,
                                        DocumentFile targetFile) throws IOException {
        try (OutputStream outputStream = resolver.openOutputStream(targetFile.getUri());
             InputStream inputStream = resolver.openInputStream(sourceFile.getUri())) {
            copyStream(inputStream, outputStream);
        }
    }

    public static String getUrlForFile(Context context, File worldmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String applicationId = context.getPackageName();
            Uri uri = FileProvider.getUriForFile(context, applicationId + ".fileprovider", worldmap);
            return uri.toString();
        } else {
            return "file://" + worldmap.getAbsolutePath();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static Intent getNewOpenDirectoryIntent() {
        return new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static Intent getNewSelectMultipleSavegameFilesIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType(Constants.SAVEGAME_FILE_MIME_TYPE);
        return intent;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static Intent getNewSelectZipIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        return intent;
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    public static void copyDocumentFilesFromToAsync(DocumentFile[] sources,
                                                    Context context,
                                                    DocumentFile[] targets,
                                                    String loadingMessage,
                                                    Consumer<Boolean> callback) {
        if (sources.length != targets.length) {
            throw new IllegalArgumentException("Both arrays, target & source have to have the same size");
        }

        BackgroundWorker<Boolean> worker = new BackgroundWorker<>();

        CustomDialogFactory.CustomDialog progressDialog = getLoadingDialog(context, loadingMessage);
        progressDialog.setOnCancelListener(dialog -> worker.cancel());

        ContentResolver resolver = context.getContentResolver();
        Handler handler = Handler.createAsync(Looper.getMainLooper());

        worker.setTask(workerCallback -> {
            try {
                workerCallback.onInitialize();
                for (int i = 0; i < sources.length; i++) {
                    if (worker.isCancelled()) {
                        workerCallback.onFailure(new CancellationException("Cancelled"));
                        return;
                    }
                    DocumentFile source = sources[i];
                    DocumentFile target = targets[i];

                    if (source == null || target == null) {
                        continue;
                    }

                    copyDocumentFile(source, resolver, target);
                    float progress = i / (float) sources.length;
                    workerCallback.onProgress(progress);
                }
                workerCallback.onComplete(true);
            } catch (NullPointerException e) {
                if (worker.isCancelled()) {
                    workerCallback.onFailure(new CancellationException("Cancelled"));
                    return;
                }
            } catch (Exception e) {
                workerCallback.onFailure(e);
            }
        });
        worker.setCallback(getDefaultBackgroundWorkerCallback(handler, progressDialog, callback));
        worker.run();
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    public static void copyDocumentFilesToDirAsync(DocumentFile[] files,
                                                   Context context,
                                                   DocumentFile targetDirectory,
                                                   String loadingMessage,
                                                   Consumer<Boolean> callback) {
        BackgroundWorker<Boolean> worker = new BackgroundWorker<>();
        CustomDialogFactory.CustomDialog progressDialog = getLoadingDialog(context, loadingMessage);
        progressDialog.setOnCancelListener(dialog -> worker.cancel());
        ContentResolver resolver = context.getContentResolver();
        Handler handler = Handler.createAsync(Looper.getMainLooper());

        worker.setTask(workerCallback -> {
            try {
                workerCallback.onInitialize();
                for (int i = 0; i < files.length; i++) {
                    if (worker.isCancelled()) {
                        workerCallback.onFailure(new CancellationException("Cancelled"));
                        return;
                    }
                    DocumentFile file = files[i];
                    if (file == null) {
                        continue;
                    }

                    copyDocumentFileToNewOrExistingFile(file, resolver, targetDirectory);
                    float progress = i / (float) files.length;
                    workerCallback.onProgress(progress);
                }
                workerCallback.onComplete(true);
            } catch (NullPointerException e) {
                if (worker.isCancelled()) {
                    workerCallback.onFailure(new CancellationException("Cancelled"));
                }
            } catch (Exception e) {
                workerCallback.onFailure(e);
            }
        });
        worker.setCallback(getDefaultBackgroundWorkerCallback(handler, progressDialog, callback));
        worker.run();
    }

    private static BackgroundWorkerCallback<Boolean> getDefaultBackgroundWorkerCallback(Handler handler,
                                                                                        CustomDialogFactory.CustomDialog progressDialog,
                                                                                        Consumer<Boolean> callback) {
        return new BackgroundWorkerCallback<Boolean>() {
            private int progress = -1;

            @Override
            public void onInitialize() {
                handler.post(() -> {
                    CustomDialogFactory.show(progressDialog);
                });
            }

            @Override
            public void onProgress(float progress) {
                handler.post(() -> {
                    int intProgress = (int) (progress * 100);
                    if (this.progress == intProgress) {
                        return;
                    }

                    this.progress = intProgress;

                    if (progress == -1) {
                        CustomDialogFactory.setDesc(progressDialog, null);
                        return;
                    }

                    CustomDialogFactory.setDesc(progressDialog, intProgress + "%");
                });
            }

            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onFailure(Exception e) {
                this.onComplete(false);
            }

            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onComplete(Boolean result) {
                handler.post(() -> {
                    progressDialog.dismiss();
                    callback.accept(result);
                });
            }
        };
    }

    private static CustomDialogFactory.CustomDialog getLoadingDialog(Context context) {
        return getLoadingDialog(context, null);
    }

    private static CustomDialogFactory.CustomDialog getLoadingDialog(Context context, String message) {
        if (message == null) {
            message = context.getResources().getString(R.string.dialog_loading_message);
        }

        CustomDialogFactory.CustomDialog dialog = CustomDialogFactory.createDialog(context,
                                                                                   message,
                                                                                   context.getResources()
                                                                                           .getDrawable(R.drawable.loading_anim),
                                                                                   null,
                                                                                   null,
                                                                                   true,
                                                                                   false);
        CustomDialogFactory.addCancelButton(dialog, android.R.string.no);
        return dialog;
    }

}

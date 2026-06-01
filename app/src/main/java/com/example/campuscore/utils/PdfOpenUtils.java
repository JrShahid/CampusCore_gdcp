package com.example.campuscore.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class PdfOpenUtils {
    public static final String TAG = "PDF_DEBUG";
    private static final String PDF_MIME_TYPE = "application/pdf";
    private static final String PDF_CACHE_DIR = "remote_pdfs";
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private PdfOpenUtils() {
    }

    public interface OpenPdfCallback {
        void onDownloadStart();

        void onViewerLaunch();

        void onError();
    }

    public static void openRemotePdf(Context context, String noteId, String pdfUrl, OpenPdfCallback callback) {
        String safeUrl = pdfUrl == null ? "" : pdfUrl.trim();
        Log.d(TAG, "openRemotePdf noteId=" + safe(noteId) + " pdfUrl=" + safeUrl);
        if (safeUrl.isEmpty()) {
            Log.e(TAG, "Stopping before download: empty pdfUrl for noteId=" + safe(noteId));
            callback.onError();
            return;
        }

        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                MAIN_HANDLER.post(callback::onDownloadStart);
                File pdfFile = downloadToCache(appContext, noteId, safeUrl);
                MAIN_HANDLER.post(() -> openCachedPdf(context, noteId, pdfFile, callback));
            } catch (IOException error) {
                Log.e(TAG, "Download failed noteId=" + safe(noteId) + " message=" + safe(error.getMessage()), error);
                MAIN_HANDLER.post(callback::onError);
            }
        });
    }

    private static File downloadToCache(Context context, String noteId, String pdfUrl) throws IOException {
        Log.d(TAG, "Download start noteId=" + safe(noteId) + " url=" + pdfUrl);
        Request request = new Request.Builder()
                .url(pdfUrl)
                .build();
        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            ResponseBody body = response.body();
            Log.d(TAG, "Download response noteId=" + safe(noteId)
                    + " code=" + response.code()
                    + " contentType=" + (body == null || body.contentType() == null ? "" : body.contentType()));
            if (!response.isSuccessful() || body == null) {
                String errorBody = body == null ? "" : body.string();
                Log.e(TAG, "Download rejected noteId=" + safe(noteId)
                        + " code=" + response.code()
                        + " body=" + trimForLog(errorBody));
                throw new IOException("Unable to download PDF");
            }

            File cacheDir = new File(context.getCacheDir(), PDF_CACHE_DIR);
            if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                throw new IOException("Unable to prepare PDF cache");
            }

            byte[] pdfBytes = body.bytes();
            Log.d(TAG, "Download complete noteId=" + safe(noteId) + " bytes=" + pdfBytes.length);
            if (!startsWithPdfSignature(pdfBytes)) {
                throw new IOException("Downloaded file is not a PDF");
            }

            File pdfFile = new File(cacheDir, "note_" + Integer.toHexString(pdfUrl.hashCode()) + ".pdf");
            try (FileOutputStream outputStream = new FileOutputStream(pdfFile)) {
                outputStream.write(pdfBytes);
            }
            Log.d(TAG, "File created noteId=" + safe(noteId)
                    + " path=" + pdfFile.getAbsolutePath()
                    + " size=" + pdfFile.length());
            return pdfFile;
        }
    }

    private static boolean startsWithPdfSignature(byte[] bytes) {
        int scanLimit = Math.min(bytes.length - 3, 1024);
        for (int index = 0; index < scanLimit; index++) {
            if (bytes[index] == '%'
                    && bytes[index + 1] == 'P'
                    && bytes[index + 2] == 'D'
                    && bytes[index + 3] == 'F') {
                return true;
            }
        }
        return false;
    }

    private static void openCachedPdf(Context context, String noteId, File pdfFile, OpenPdfCallback callback) {
        try {
            Uri pdfUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    pdfFile
            );
            Log.d(TAG, "Generated URI noteId=" + safe(noteId) + " uri=" + pdfUri);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, PDF_MIME_TYPE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Log.d(TAG, "Launching PDF viewer noteId=" + safe(noteId)
                    + " resolved=" + (intent.resolveActivity(context.getPackageManager()) != null));
            callback.onViewerLaunch();
            context.startActivity(intent);
        } catch (ActivityNotFoundException | IllegalArgumentException error) {
            Log.e(TAG, "Viewer launch failed noteId=" + safe(noteId) + " message=" + safe(error.getMessage()), error);
            callback.onError();
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String trimForLog(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}

package com.example.lenovonhg.apitest;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import static android.support.v4.app.ActivityCompat.requestPermissions;

public class BookDownload {
    private ProgressDialog pDialog;
    private Activity activityContext;
    String BOOK_TITLE,bookPath,dirPath;
    BookDownload(){super();}
    public void getDownload (String downloadLink,String bookTitle ,Activity context)
    {
        activityContext = context;
        BOOK_TITLE = bookTitle;
        new DownloadFileFromURL().execute(downloadLink);
    }
    private boolean shouldAskPermissions() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    @TargetApi(23)
    private void askPermissions() {
        String[] permissions = {
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"
        };
        int requestCode = 200;
        requestPermissions(activityContext,permissions, requestCode);
    }
    private class DownloadFileFromURL extends AsyncTask<String, String, String>
    {
        /**
         * Before starting background thread
         * Show Progress Bar Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(activityContext);
            pDialog.setMessage("Downloading file. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setMax(100);
            pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        /**
         * Downloading file in background thread
         * */
        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {
                URL url = new URL(f_url[0]);
                URLConnection conection = url.openConnection();
                conection.connect();
                // getting file length
                int lenghtOfFile = conection.getContentLength();

                // input stream to read file - with 8k buffer
                InputStream input = new BufferedInputStream(url.openStream(), 8192);


                if (shouldAskPermissions()) {
                    askPermissions();
                    File myDir = activityContext.getFilesDir();
                    // Download Path
                    String downloads = "Download/Helen";
                    File documentsFolder = new File(myDir, downloads);
                    documentsFolder.mkdirs(); // this line creates Helen folder at downloads directory if it doesn't exist
                    dirPath = documentsFolder.toString();
                    //Creating a new file with the book title
                    File newBook  = new File(dirPath,BOOK_TITLE+".pdf");
                    // Output stream to write file
                    OutputStream output = new FileOutputStream(newBook);
                    byte data[] = new byte[10485760 ];

                    long total = 0;

                    while ((count = input.read(data)) != -1) {
                        total += count;
                        // publishing the progress....
                        // After this onProgressUpdate will be called
                        publishProgress(""+(int)((total*100)/lenghtOfFile));

                        // writing data to file
                        output.write(data, 0, count);
                    }

                    // flushing output
                    output.flush();

                    // closing streams
                    output.close();
                    input.close();

                }
            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }

            return null;
        }

        /**
         * Updating progress bar
         * */
        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            pDialog.setProgress(Integer.parseInt(progress[0]));
        }

        /**
         * After completing background task
         * Dismiss the progress dialog
         * **/
        @Override
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after the file was downloaded
            pDialog.dismiss();
            bookPath = dirPath + "/"+BOOK_TITLE+".pdf";
        }
    }
}

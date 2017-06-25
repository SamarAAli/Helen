package com.example.lenovonhg.apitest;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import com.squareup.picasso.Picasso;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class BookDetailsActivityfragment extends Fragment {
    private BookDetailsAdapter detailsAdapter;
    private String BOOK_TITLE;
    List<JSONObject> Adapterinput = new ArrayList<JSONObject>();
    private AlertDialog alertDialog;
    private ProgressDialog pDialog;
    String downloadLink,bookPath;
    private View headerview;
    public  BookDetailsActivityfragment() {}
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootview = inflater.inflate(R.layout.book_details_body, container, false);
        headerview = inflater.inflate(R.layout.book_details_header,null);
        detailsAdapter = new BookDetailsAdapter(getActivity(),new ArrayList<JSONObject>());
        ListView listView = (ListView) rootview.findViewById(R.id.Book_Content_List_view);
        String bookDataString = getArguments().getString("JSONObject");
        try {
            JSONObject BookDataObj = new JSONObject(bookDataString);
            parseBookDataFromObj(BookDataObj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        listView.addHeaderView(headerview);
        listView.setAdapter(detailsAdapter);
        return rootview;
    }
    public boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
    }
    private void showDialogMsg(String msg) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setMessage(msg);

        alertDialogBuilder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                getActivity().finish();
            }
        });

        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }


    private void updateDetailView() {
        Context context = getActivity();
        if(isOnline(context)) {
            FetchBookReviews fetchReviews = new FetchBookReviews();
            fetchReviews.execute();
        }else
            showDialogMsg("Please check your internet connection!");
    }
    @Override
    public void onStart() {
        super.onStart();
        updateDetailView();
    }
    private void parseBookDataFromObj(JSONObject bookObj) throws JSONException{
        JSONObject volInfoObj = bookObj.getJSONObject("volumeInfo");
        JSONObject accInfoObj = bookObj.getJSONObject("accessInfo");
        JSONObject imageLinks = volInfoObj.getJSONObject("imageLinks");
        JSONArray Authors = volInfoObj.getJSONArray("authors");
        String imageURL = imageLinks.getString("thumbnail");
        String SYNOPSIS = volInfoObj.getString("description");
        String REL_DATE = volInfoObj.getString("publishedDate");
        String RATING = volInfoObj.getString("averageRating");
        BOOK_TITLE = volInfoObj.getString("title");

        TextView authors = (TextView) headerview.findViewById(R.id.object_author);
        String authorsNames = "Written by: ";

        for (int i = 0; i < Authors.length(); i++)
        {
            if(i != Authors.length()-1)
                authorsNames = authorsNames + Authors.getString(i)+",";
            else
                authorsNames = authorsNames + Authors.getString(i);
        }
        //Log.d("authornames",authorsNames);
        authors.setText(authorsNames);

        TextView Title = (TextView) headerview.findViewById(R.id.object_title);
        Title.setText(BOOK_TITLE);

        TextView OverView = (TextView) headerview.findViewById(R.id.object_desc);
        OverView.setText(SYNOPSIS);

        TextView ReleaseDate = (TextView) headerview.findViewById(R.id.object_release_date);
        ReleaseDate.setText("Released in: "+REL_DATE);

        RatingBar rating = (RatingBar) headerview.findViewById(R.id.object_rating);
        float rate = Float.parseFloat(RATING);
        rating.setRating(rate);

        ImageView Poster = (ImageView) headerview.findViewById(R.id.object_poster);
        Picasso.with(getContext()).load(imageURL).into(Poster);

        final JSONObject pdfObj = accInfoObj.getJSONObject("pdf");
        if (pdfObj.has("downloadLink"))
        {
            //TODO "download link will be retrieved from the server instead of google books"
            Button downloadButton = (Button) headerview.findViewById(R.id.download_button);
            downloadButton.setVisibility(View.VISIBLE);
            downloadLink = pdfObj.getString("downloadLink");
            downloadButton.setOnClickListener(new  View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DownloadFileFromURL().execute(downloadLink);
            }
        });
        }
        else
        {
            Button downloadButton = (Button) headerview.findViewById(R.id.download_button);
            downloadButton.setVisibility(View.INVISIBLE);
        }
    }
    private List<JSONObject> getBooksDetailsFromJson(String BooksJsonStr) throws JSONException {
        JSONObject booksJson = new JSONObject(BooksJsonStr);
        JSONObject reviewObj = booksJson.getJSONObject("book");
        JSONArray reviewsArray = reviewObj.getJSONArray("critic_reviews");
        List<JSONObject> resultStrs = new ArrayList<JSONObject>() ;
        for(int i = 0; i < reviewsArray.length(); i++)
        {
            JSONObject bookObj = reviewsArray.getJSONObject(i);
            resultStrs.add(bookObj);
        }
        return resultStrs;
    }
    protected boolean shouldAskPermissions() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    @TargetApi(23)
    protected void askPermissions() {
        String[] permissions = {
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"
        };
        int requestCode = 200;
        requestPermissions(permissions, requestCode);
    }
   private class DownloadFileFromURL extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread
         * Show Progress Bar Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(getActivity());
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

                // Output stream to write file
                if (shouldAskPermissions()) {
                    askPermissions();
                    OutputStream output = new FileOutputStream("/sdcard/"+BOOK_TITLE+".pdf");
                    byte data[] = new byte[3145728];

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
            bookPath = Environment.getExternalStorageDirectory().toString() + "/"+BOOK_TITLE+".pdf";
        }
    }
    private class FetchBookReviews extends AsyncTask<String, Void, List<JSONObject>> {
        private final String LOG_TAG = FetchBookReviews.class.getSimpleName();
        @Override
        protected List<JSONObject> doInBackground(String... params) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            // Will contain the raw JSON response as a string.
            String ReviewDetialsJsonStr = null;
            String appkey = getString(R.string.api_key);
            try {

                final String _BASE_URL = "https://idreambooks.com/api/books/reviews.json";
                final String ID_PARAM = "key";

                Uri builtUri = Uri.parse(_BASE_URL).buildUpon()
                        .appendQueryParameter("q",BOOK_TITLE)
                        .appendQueryParameter(ID_PARAM, appkey)
                        .build();
                URL url = new URL(builtUri.toString());

                // Create the request to themoviedb, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                ReviewDetialsJsonStr = buffer.toString();
                try {
                    Adapterinput.addAll(getBooksDetailsFromJson(ReviewDetialsJsonStr));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Could not connect ", e);
                showDialogMsg("Please check your internet connection!");
                return null;
            }
            finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            return Adapterinput;
        }

        @Override
        protected void onPostExecute(List<JSONObject> resultList) {
            if (resultList != null) {
                detailsAdapter.clear();
                detailsAdapter.addAll(resultList);
            }
        }
    }
}

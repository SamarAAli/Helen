package com.example.lenovonhg.apitest;

import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivityFragment extends Fragment {
    private AlertDialog alertDialog;
    private FragmentListener flistener;
    public MainActivityFragment() {
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        Button search_Button = (Button) rootView.findViewById(R.id.retrieve_button);
        search_Button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
            EditText search_Title = (EditText) rootView.findViewById(R.id.search_title);
            EditText search_Author = (EditText) rootView.findViewById(R.id.search_author);
            String search_title = "";
            String search_author = "";

                if(search_Title != null && search_Author != null)
                {
                    Context context = getActivity();
                    search_title = search_Title.getText().toString();
                    search_author = search_Author.getText().toString();
                    if(isOnline(context)) {
                        FetchBookTask fetchBookTask = new FetchBookTask();
                        fetchBookTask.execute(search_title,search_author);
                    }else
                        showDialogMsg("Please check your internet connection!");
                }
                else if (search_Title != null)
                {
                    Context context = getActivity();
                    search_title = search_Title.getText().toString();
                    if(isOnline(context)) {
                        FetchBookTask fetchBookTask = new FetchBookTask();
                        fetchBookTask.execute(search_title,search_author);
                    }else
                        showDialogMsg("Please check your internet connection!");
                }
                else if (search_Author != null)
                {
                    Context context = getActivity();
                    search_author = search_Author.getText().toString();
                    if(isOnline(context)) {
                        FetchBookTask fetchBookTask = new FetchBookTask();
                        fetchBookTask.execute(search_title,search_author);
                    }else
                        showDialogMsg("Please check your internet connection!");
                }
                else
                    showDialogMsg("Please enter a book title or an auther name!");
            }
        });
        return rootView;
    }
    public void setFragmentListner(FragmentListener fragmentListener) {
        flistener = fragmentListener;
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
    public class FetchBookTask extends AsyncTask<String, Void, JSONObject>{
        private final String LOG_TAG = FetchBookTask.class.getSimpleName();
        private JSONObject getBooksDataFromJson(String bookJsonStr,String queryparam) throws JSONException {
            JSONObject booksJson = new JSONObject(bookJsonStr);
            booksJson.put("queryparam",queryparam);
            return  booksJson;
        }
        @Override
        protected JSONObject doInBackground(String... params) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String BooksJsonStr = null;
            String appkey = getString(R.string.api_key);
            try {
                final String _BASE_URL = "https://www.googleapis.com/books/v1/volumes";
                final String _QUERY_PARAM = params[0]+"+inauthor:"+params[1];
                final String ID_PARAM = "key";

                Uri builtUri = Uri.parse(_BASE_URL).buildUpon()
                        .appendQueryParameter("q",_QUERY_PARAM)
                        .appendQueryParameter("download","epub")
                        .appendQueryParameter("printType","books")
                        .appendQueryParameter(ID_PARAM, appkey)
                        .build();
                URL url = new URL(builtUri.toString());

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
                BooksJsonStr = buffer.toString();
                //BooksJsonStr = BooksJsonStr.substring(0,BooksJsonStr.length()-1)+paramObject.toString()+BooksJsonStr.charAt(BooksJsonStr.length()-1);
                //Log.d(LOG_TAG,"bookJson: "+BooksJsonStr);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Could not connect ", e);
                showDialogMsg("Please check your internet connection!");
                return null;
            } finally {
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
            try{
                return getBooksDataFromJson(BooksJsonStr,params[0]);
            } catch (JSONException e)
            {
                Log.e(LOG_TAG,e.getMessage(),e);
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(JSONObject result) {
            if (result != null) {
                flistener.setDetailsData(result);
            }
        }
    }
}

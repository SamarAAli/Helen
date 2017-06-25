package com.example.lenovonhg.apitest;

import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class BookListActivityFragment extends Fragment {
    private BookListAdapter BooksAdapter;
    private FragmentListener flistener;
    private List<JSONObject> BookList ;
    private AlertDialog alertDialog;
    public BookListActivityFragment() {}
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.book_item_list, container, false);
        BooksAdapter = new BookListAdapter(getActivity(),new ArrayList<JSONObject>());
        ListView listView = (ListView) rootView.findViewById(R.id.Book_List_view);
        listView.setAdapter(BooksAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                JSONObject Bookdetails = BooksAdapter.getItem(position);
                flistener.setDetailsData(Bookdetails);
            }
        });
        return rootView;
    }
    public void setFragmentListner(FragmentListener fragmentListener) {
        flistener = fragmentListener;
    }
    @Override
    public void onStart()
    {
        super.onStart();
        updateListView();
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
    public void updateListView(){
        Context context = getActivity();
        if(isOnline(context)) {
            String BooksDataString = getArguments().getString("JSONObject");
            try {
                BookList = getBooksDataFromJson(BooksDataString);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (BookList != null) {
                BooksAdapter.clear();
                BooksAdapter.addAll(BookList);
            }
        }else
            showDialogMsg("Please check your internet connection!");
    }

    private List<JSONObject> getBooksDataFromJson(String BooksDataString)throws JSONException {
        final String _ITEMS = "items";
        JSONObject booksJson = new JSONObject(BooksDataString);
        JSONArray booksArray = booksJson.getJSONArray(_ITEMS);
        String queryTitle = booksJson.getString("queryparam");
        List<JSONObject> resultStrs = new ArrayList<JSONObject>() ;
        for(int i = 0; i < booksArray.length(); i++)
        {
            JSONObject bookObj = booksArray.getJSONObject(i);
            if (!queryTitle.equals("")) {
                JSONObject volInfo =  bookObj.getJSONObject("volumeInfo");
                String title = volInfo.getString("title").toLowerCase();
                queryTitle.toLowerCase();
                if (title.equals(queryTitle) && i != 0)
                    continue;
            }
            resultStrs.add(bookObj);
        }
        return resultStrs;
    }
}

package de.android.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import de.android.MainActivity;
import de.android.R;
import de.android.inherit.PermissionsFragment;
import de.android.util.Helper;

public class WallpapersFragment extends Fragment implements PermissionsFragment {
    ArrayList<TumblrItem> tumblrItems;
    private ImageAdapter imageAdapter = null;
    Activity mAct;
    private GridView listView;
    private LinearLayout ll;
    RelativeLayout pDialog;
    String perpage = "25";
    Integer curpage = 0;
    Integer total_posts;
    String baseurl;
    Boolean initialload = true;
    Boolean isLoading = true;
    SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ll = (LinearLayout) inflater.inflate(R.layout.fragment_wallpapers, container, false);
        setHasOptionsMenu(true);
        String username = this.getArguments().getStringArray(MainActivity.FRAGMENT_DATA)[0];
        baseurl = "https://"+username+".tumblr.com/api/read/json?type=photo&num=" + perpage + "&start=";
        listView = (GridView) ll.findViewById(R.id.gridview);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startImagePagerActivity(position);
            }
        });
        listView.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                if (imageAdapter == null)
                    return ;
                if (imageAdapter.getCount() == 0)
                    return ;
                int l = visibleItemCount + firstVisibleItem;
                if (l >= totalItemCount && !isLoading && (curpage * Integer.parseInt(perpage)) <= total_posts) {
                    isLoading = true;
                    new InitialLoadGridView().execute(baseurl);
                }
            }

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // unused
            }
        });
        mSwipeRefreshLayout = (SwipeRefreshLayout) ll.findViewById(R.id.swipe_refresh);
        mSwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_light,
                android.R.color.holo_red_light, android.R.color.holo_green_light);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (!isLoading){
                    initialload = true;
                    isLoading = true;
                    curpage = 1;
                    tumblrItems.clear();
                    listView.setAdapter(null);
                    new InitialLoadGridView().execute(baseurl);
                } else {
                    Toast.makeText(mAct, getString(R.string.already_loading), Toast.LENGTH_LONG).show();
                }
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                }, 4000);
            }
        });
        return ll;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAct = getActivity();
        new InitialLoadGridView().execute(baseurl);
    }

    private void startImagePagerActivity(int position) {
        Intent intent = new Intent(mAct, TumblrPagerActivity.class);
        ArrayList<TumblrItem> underlying =  new ArrayList<TumblrItem>();
        for (int i = 0; i < imageAdapter.getCount(); i++)
            underlying.add(imageAdapter.getItem(i));
        Bundle b = new Bundle();
        b.putParcelableArrayList(Constants.Extra.IMAGES, underlying);
        intent.putExtras(b);
        intent.putExtra(Constants.Extra.IMAGE_POSITION, position);
        startActivity(intent);
    }

    public void updateList() {
        if (initialload){
            imageAdapter = new ImageAdapter(mAct, 0, tumblrItems);
            listView.setAdapter(imageAdapter);
            initialload = false;
        } else {
            imageAdapter.addAll(tumblrItems);
            imageAdapter.notifyDataSetChanged();
        }
        isLoading = false;
    }

    @Override
    public String[] requiredPermissions() {
        return new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    }


    private class InitialLoadGridView extends AsyncTask<String, Void, Void> {
        @Override
        protected void onPreExecute() {
            // Showing progress dialog before sending http request
            if (initialload){
                pDialog = (RelativeLayout) ll.findViewById(R.id.progressBarHolder);
            }
        }

        protected Void doInBackground(String... params) {
            String geturl = params[0];
            geturl = geturl + Integer.toString((curpage) *  Integer.parseInt(perpage));
            curpage = curpage + 1;
            String jsonString = Helper.getDataFromUrl(geturl);
            System.out.println("Return: " + jsonString);
            JSONObject json= null;
            try {
                jsonString = jsonString.replace("var tumblr_api_read = ", "");
                json = new JSONObject(jsonString);
            } catch (JSONException e) {

            }
            ArrayList<TumblrItem> images = new ArrayList<TumblrItem>();
            try {
                String success = json.getString("posts-total");
                total_posts = Integer.parseInt(success);
                if (0 < Integer.parseInt(success)) {
                    JSONArray products;
                    products = json.getJSONArray("posts");
                    for (int i = 0; i < products.length(); i++) {
                        JSONObject c = products.getJSONObject(i);
                        String id = c.getString("id");
                        String link = c.getString("url");
                        String url;
                        try {
                            url = c.getString("photo-url-1280");
                        } catch (JSONException e){
                            try {
                                url = c.getString("photo-url-500");
                            } catch (JSONException r){
                                try {
                                    url = c.getString("photo-url-250");
                                } catch (JSONException l){
                                    url = null;
                                }
                            }
                        }
                        if (url != null){
                            TumblrItem item = new TumblrItem(id, link, url);
                            images.add(item);
                        }
                    }
                    tumblrItems = images;
                } else {
                    Log.v("INFO", "No items found");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            return (null);
        }

        protected void onPostExecute(Void unused) {
            if (null != tumblrItems) {
                updateList();
            }
            if (pDialog.getVisibility() == View.VISIBLE) {
                pDialog.setVisibility(View.GONE);
                Helper.revealView(listView,ll);
            }
        }
    }





}

package com.dmplayer.fragments;

import android.content.AsyncQueryHandler;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dmplayer.R;
import com.dmplayer.activities.DMPlayerBaseActivity;
import com.dmplayer.adapter.CursorRecyclerViewAdapter;
import com.dmplayer.manager.MediaController;
import com.dmplayer.models.SongDetail;
import com.dmplayer.phonemidea.DMPlayerUtility;
import com.dmplayer.phonemidea.PhoneMediaControl;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.apache.http.NameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static com.dmplayer.phonemidea.PhoneMediaControl.SonLoadFor.All;

/**
 * Created by sHIVAM on 12/7/2016.
 */
public class FragmentMoodPredictor extends Fragment{

    private RecyclerView recyclerView;
    private RecyclerViewAdapter adapter;
    private ArrayList<SongDetail> songsList = new ArrayList<SongDetail>();
    private WeakReference<MoodTagAsyncTask> asyncTaskWeakReference;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSongList();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_moodpredictor, null);
        setupInitialViews(rootView, savedInstanceState);
        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void setupInitialViews(View rootView, Bundle savedInstanceState) {
        recyclerView = (RecyclerView)rootView.findViewById(R.id.music_list);
        populateData(savedInstanceState);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

    }

    private void populateData(Bundle savedInstanceState) {
        adapter = (RecyclerViewAdapter) getActivity().getLastCustomNonConfigurationInstance();
        Log.d("populate mood", (adapter == null)+"");
        if (adapter == null) {
            adapter = new RecyclerViewAdapter(getActivity(), songsList);
            recyclerView.setAdapter(adapter);
        } else {
            recyclerView.setAdapter(adapter);
        }
    }

    private void getSongList() {
        PhoneMediaControl mPhoneMediaControl = PhoneMediaControl.getInstance();
        PhoneMediaControl.setPhonemediacontrolinterface(new PhoneMediaControl.PhoneMediaControlINterface() {

            @Override
            public void loadSongsComplete(ArrayList<SongDetail> songsList_) {
                Log.d("mood", "data loaded!");
                songsList = songsList_;
                Log.d("data loaded mood", (adapter == null)+"");
                if(adapter != null){
                    adapter.swap(songsList);
                } else {
                    populateData(new Bundle());
                }
                executeAsyncTaskForMoodTags();
            }
        });
        mPhoneMediaControl.loadMusicList(getActivity(), -1, PhoneMediaControl.SonLoadFor.All, "");
    }

    private void executeAsyncTaskForMoodTags() {
        String url = "http://192.168.0.50:5000/api/v1.0/predict/";
        MoodTagAsyncTask asyncTask = new MoodTagAsyncTask(this, songsList);
        this.asyncTaskWeakReference = new WeakReference<MoodTagAsyncTask>(asyncTask);
        asyncTask.execute(url);
    }

    private boolean isAsyncTaskPendingorRunning() {
        return this.asyncTaskWeakReference != null &&
                this.asyncTaskWeakReference.get() != null &&
                !this.asyncTaskWeakReference.get().getStatus().equals(AsyncTask.Status.FINISHED);
    }


    public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

        private Context context = null;
        private LayoutInflater layoutInflater;
        private DisplayImageOptions options;
        private ImageLoader imageLoader = ImageLoader.getInstance();
        private ArrayList<SongDetail> data = new ArrayList<>();


        RecyclerViewAdapter(Context context, ArrayList<SongDetail>data) {
            this.context = null;
            this.layoutInflater = LayoutInflater.from(context);
            this.options = new DisplayImageOptions.Builder().showImageOnLoading(R.drawable.bg_default_album_art)
                    .showImageForEmptyUri(R.drawable.bg_default_album_art).showImageOnFail(R.drawable.bg_default_album_art).cacheInMemory(true)
                    .cacheOnDisk(true).considerExifParams(true).bitmapConfig(Bitmap.Config.RGB_565).build();
            this.data = data;
            Log.d("mood", "inside recycler view constructor");
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Log.d("mood", "inside on create view holder");
            return new ViewHolder(layoutInflater.inflate(R.layout.moodpredictor_items, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder mViewHolder, int position) {
            SongDetail mDetail = data.get(position);
            String audioDuration = "";
            try {
                audioDuration = DMPlayerUtility.getAudioDuration(Long.parseLong(mDetail.getDuration()));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            mViewHolder.textViewSongArtisNameAndDuration.setText((audioDuration.isEmpty() ? "" : audioDuration + " | ") + mDetail.getArtist());
            mViewHolder.textViewSongName.setText(mDetail.getTitle());
            String contentURI = "content://media/external/audio/media/" + mDetail.getId() + "/albumart";
            imageLoader.displayImage(contentURI, mViewHolder.imageSongThm, options);
            mViewHolder.imagemore.setColorFilter(Color.DKGRAY);
            if (Build.VERSION.SDK_INT > 15) {
                mViewHolder.imagemore.setImageAlpha(255);
            } else {
                mViewHolder.imagemore.setAlpha(255);
            }
            if(mDetail.getMood() != null) {
                switch (mDetail.getMood()) {
                    case "angry":
                        mViewHolder.mood_tag.setTextColor(getResources().getColor(R.color.mood_angry));
                        break;
                    case "calm":
                        mViewHolder.mood_tag.setTextColor(getResources().getColor(R.color.mood_calm));
                        break;
                    case "happy":
                        mViewHolder.mood_tag.setTextColor(getResources().getColor(R.color.mood_happy));
                        break;
                    case "sad":
                        mViewHolder.mood_tag.setTextColor(getResources().getColor(R.color.mood_sad));
                        break;

                }
                mViewHolder.mood_tag.setVisibility(View.VISIBLE);
                mViewHolder.mood_tag.setText(mDetail.getMood());
            }


        }

        @Override
        public int getItemCount() {
            Log.d("mood", "inside getItemCount "+data.size());
            return data.size();
        }

        public void swap(ArrayList<SongDetail> list){
            if (data != null) {
                data.clear();
                data.addAll(list);
            }
            else {
                data = list;
            }
            notifyDataSetChanged();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView textViewSongName;
            ImageView imageSongThm, imagemore;
            TextView textViewSongArtisNameAndDuration;
            LinearLayout song_row;
            TextView mood_tag;

            public ViewHolder(View itemView) {
                super(itemView);
                Log.d("mood", "inside on create view holder");
                if(itemView instanceof LinearLayout) {
                    setUpView(itemView);
                    setUpClickListeners(itemView);
                }
            }

            private void setUpClickListeners(View convertView) {
                imagemore.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        try {
                            PopupMenu popup = new PopupMenu(context, v);
                            popup.getMenuInflater().inflate(R.menu.list_item_option, popup.getMenu());
                            popup.show();
                            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {

                                    switch (item.getItemId()) {
                                        case R.id.playnext:
                                            break;
                                        case R.id.addtoque:
                                            break;
                                        case R.id.addtoplaylist:
                                            break;
                                        case R.id.gotoartis:
                                            break;
                                        case R.id.gotoalbum:
                                            break;
                                        case R.id.delete:
                                            break;
                                        default:
                                            break;
                                    }

                                    return true;
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                convertView.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        SongDetail mDetail = data.get(getAdapterPosition());
                        ((DMPlayerBaseActivity) getActivity()).loadSongsDetails(mDetail);

                        if (mDetail != null) {
                            if (MediaController.getInstance().isPlayingAudio(mDetail) && !MediaController.getInstance().isAudioPaused()) {
                                MediaController.getInstance().pauseAudio(mDetail);
                            } else {
                                MediaController.getInstance().setPlaylist(data, mDetail, PhoneMediaControl.SonLoadFor.All.ordinal(), -1);
                            }
                        }

                    }
                });
            }

            private void setUpView(View convertView) {
                song_row = (LinearLayout) convertView.findViewById(R.id.inflate_allsong_row);
                textViewSongName = (TextView) convertView.findViewById(R.id.inflate_allsong_textsongname);
                textViewSongArtisNameAndDuration = (TextView) convertView.findViewById(R.id.inflate_allsong_textsongArtisName_duration);
                imageSongThm = (ImageView) convertView.findViewById(R.id.inflate_allsong_imgSongThumb);
                imagemore = (ImageView) convertView.findViewById(R.id.img_moreicon);
                mood_tag = (TextView) convertView.findViewById(R.id.mood_tag);
            }


        }
    }

    private static class MoodTagAsyncTask extends AsyncTask<String, Void, ArrayList<SongDetail>> {

        private final ArrayList<SongDetail> songsList;
        private WeakReference<FragmentMoodPredictor> fragmentWeakRef;
        private MoodTagAsyncTask(FragmentMoodPredictor fragment, ArrayList<SongDetail> songsList) {
            this.fragmentWeakRef = new WeakReference<FragmentMoodPredictor>(fragment);
            this.songsList = songsList;
        }

        @Override
        protected ArrayList<SongDetail> doInBackground(String... address) {
            ArrayList<SongDetail> res = new ArrayList<>();
            try {
                String url = address[0]+"?";
                for(SongDetail ele : songsList) {
                    String song = ele.getTitle();
                    String artist = ele.getArtist();

                    String uri = Uri
                                .parse(url)
                                .buildUpon()
                                .appendQueryParameter("track", song)
                                .appendQueryParameter("artist", artist)
                                .build()
                                .toString();
                    Log.d("mood", uri);
                    URL urls = new URL(uri);
                    HttpURLConnection connection = (HttpURLConnection) urls.openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();
                    Log.d("mood", "connected");
                    JSONObject ob = get_json(connection);
                    if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        Log.d("mood", ob.optString("mood_tag"));
                        ele.setMood(ob.optString("mood_tag"));
                        res.add(ele);
                    }
                }
//                res.add(ob.optString("mood_tag"));
                return res;

            } catch (Exception e) {
                e.printStackTrace();
            }
            return res;
        }

        @Override
        protected void onPostExecute(ArrayList<SongDetail> strings) {
            super.onPostExecute(strings);
            try{
                Context context = fragmentWeakRef.get().getActivity();
                Toast.makeText(context, "Mood Prediction Successful!", Toast.LENGTH_SHORT).show();
                fragmentWeakRef.get().adapter.swap(songsList);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        private JSONObject get_json(HttpURLConnection connection) throws JSONException {
            JSONObject error = new JSONObject("{}");
            try {
                InputStream inputStream = connection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null)
                    return error;
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }

                JSONObject jsonObject = new JSONObject(buffer + "");
                if (jsonObject != null)
                    return jsonObject;

            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("error", "JSON error");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return error;
        }
    }
}

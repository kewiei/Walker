package com.example.walker;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class WalkerListAdapter extends RecyclerView.Adapter<WalkerListAdapter.ViewHolder> {
    private Context context;
    private static final String[] names={"A","B","C","D","E","F"};
    private static final String[] scores={"10 feet","9 feet","8 feet","7 feet","6 feet","5 feet"};
    private static ArrayList<WalkerInfo> walkerInfos = initdata();
    private WalkerListDataAsyncTask walkerListDataAsyncTask;

    private static ArrayList<WalkerInfo> initdata(){
        walkerInfos = new ArrayList<>();
        for(int i=0;i<names.length;i++){
            walkerInfos.add(new WalkerInfo(names[i],scores[i]));
        }
        return walkerInfos;
    }
    public WalkerListAdapter(Context context){
        this.context=context;
        walkerListDataAsyncTask = new WalkerListDataAsyncTask();
    }
    public void refreshdata(){
        String serviceString = Context.LOCATION_SERVICE;
        LocationManager locationManager = (LocationManager) context.getSystemService(serviceString);
        String provider = LocationManager.GPS_PROVIDER;
        Location location=null;
        try{
            location = locationManager.getLastKnownLocation(provider);
        }catch (SecurityException e){
            Log.e("Walker","Location permission denied");
        }catch (Exception e){
            Log.e("Walker",e.toString());
        }
        if (location!=null)
            this.walkerListDataAsyncTask.execute(location);
    }


    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView_icon;
        TextView textView_name;
        TextView textView_score;

        // TODO Auto-generated method stub
        ViewHolder(View v) {
            super(v);
            imageView_icon = v.findViewById(R.id.imageView_icon);
            textView_name = v.findViewById(R.id.textView_name);
            textView_score = v.findViewById(R.id.textView_score);
        }
        void setData(String name,String score){
            textView_name.setText(name);
            textView_score.setText(score);
        }
        void setIcon(){
            //icon waited to be updated
        }
    }
    @Override
    public WalkerListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull WalkerListAdapter.ViewHolder holder, int position) {
        WalkerInfo w=walkerInfos.get(position);
        holder.setData(w.name,w.score);
    }

    @Override
    public int getItemCount() {
        return walkerInfos.size();
    }
    public class WalkerListDataAsyncTask extends AsyncTask<Location,Void, ArrayList<WalkerInfo>>{
        @Override
        protected ArrayList<WalkerInfo> doInBackground(Location... locations) {
            ArrayList<WalkerInfo> walkerInfos = new ArrayList<>();

            double lat = locations[0].getLatitude();
            double lng = locations[0].getLongitude();
            //Some internet coding
            //HttpClient httpClient = new DefaultHttpClient()
            //......
            //Bitmap bitmap = BitmapFactory.decodeByteArray(result, 0, result.length);
            //......

            return walkerInfos;
        }

        @Override
        protected void onPostExecute(ArrayList<WalkerInfo> new_walkerInfos) {
            super.onPostExecute(walkerInfos);
            walkerInfos.clear();
            walkerInfos.addAll(new_walkerInfos);
            notifyDataSetChanged();
        }
    }

    public static class WalkerInfo{
        public Bitmap icon;
        public String name;
        public String score;
        public WalkerInfo(String name,String score){
            this .name=name;
            this.score=score;
        }
    }
}

package com.lyftoxi.lyftoxi;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lyftoxi.lyftoxi.dao.Location;
import com.lyftoxi.lyftoxi.dao.TakeRide;
import com.lyftoxi.lyftoxi.singletons.CurrentUserInfo;
import com.lyftoxi.lyftoxi.util.HttpRestUtil;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by DhimanZ on 4/16/2016.
 */
public class RideListingAdapterNoImage extends ArrayAdapter<RideListingInfo>{

    List<RideListingInfo> rides;

    SessionManager session;

    //private View v;

    public RideListingAdapterNoImage(Context context, int textViewResourceId, List<RideListingInfo> rides){
        super(context, textViewResourceId, rides);
        this.rides=rides;
        this.session = new SessionManager(context);
    }

    public RideListingInfo getItem(int position){

        return rides.get(position);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        View v = convertView;
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //v = inflater.inflate(R.layout.ride_listing_no_user_image, null);
            v = inflater.inflate(R.layout.ride_listing_no_user_image, parent, false);
        }

        final RideListingInfo i = rides.get(position);
        final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy h:mm a");
        final DecimalFormat df = new DecimalFormat("##.######");

        if (i != null) {


            TextView from = (TextView) v.findViewById(R.id.rideListingNoImageFrom);
            TextView to = (TextView) v.findViewById(R.id.rideListingNoImageTo);
            TextView fare = (TextView) v.findViewById(R.id.rideListingNoImageFare);
            TextView startingTime = (TextView) v.findViewById(R.id.rideListingNoImageStartingTime);
            final TextView cancelled = (TextView)v.findViewById(R.id.rideListingNoImageCancelled);
            final ImageButton cancelRideBtn = (ImageButton) v.findViewById(R.id.rideListingNoImageCancel);
            final View progressBar = v.findViewById(R.id.rideListingNoImageProgress);

            from.setText(i.getSourceName());
            to.setText(i.getDestinationName());
            String fareStr = "Rs. "+i.getFare();
            fare.setText(fareStr);
            startingTime.setText(sdf.format(i.getStarTime().getTime()));
            if("C".equals(i.getStatus()))
            {
                cancelled.setVisibility(View.VISIBLE);
                cancelRideBtn.setVisibility(View.GONE);
            }
            else{
                cancelled.setVisibility(View.GONE);
                cancelRideBtn.setVisibility(View.VISIBLE);
            }



        cancelRideBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                new AlertDialog.Builder(view.getContext())

                        .setMessage(R.string.cancel_ride)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {

                                DeleteRideTask deleteRide = new DeleteRideTask();
                                deleteRide.progressBar = progressBar;
                                deleteRide.cancelledText = cancelled;
                                deleteRide.cancelBtn = cancelRideBtn;
                                deleteRide.execute(i.getId());
                            }})
                        .setNegativeButton(android.R.string.no, null).show();

            }
        });
        }


        return v;

    }

    public class DeleteRideTask extends AsyncTask<String, Void,Boolean>
    {

        Gson gson = new GsonBuilder().setDateFormat("dd-MM-yyyy").create();
        View progressBar;
        TextView cancelledText;
        ImageButton cancelBtn;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            cancelBtn.setVisibility(View.GONE);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String rideId = params[0];
            try {

                HttpRestUtil httpRestUtil = new HttpRestUtil(getContext());
                String response = httpRestUtil.httpDelete("shareRideService/ride?id="+rideId);
                if(null!=response)
                {
                    return true;
                }



            }catch (IOException ioex)
            {
                Log.d("gog.debug","Error occurred in REST WS call url cannot be reached "+ioex.getMessage());
            }
            catch (Exception ex)
            {
                Log.d("gog.debug","Error occurred in REST WS call "+ex.getMessage());
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            progressBar.setVisibility(View.GONE);
            Toast toast;
            if (success) {
                toast = Toast.makeText(getContext(), "Cancelled ride successful", Toast.LENGTH_LONG);
                cancelledText.setVisibility(View.VISIBLE);
               // RideListingAdapterNoImage.this.notifyDataSetChanged();

            } else {
                toast = Toast.makeText(getContext(), "Canceling ride failed. Try Again", Toast.LENGTH_LONG);
                cancelBtn.setVisibility(View.VISIBLE);

            }
            toast.show();
            //finish();
            //startHomeActivity();

        }
    }

}

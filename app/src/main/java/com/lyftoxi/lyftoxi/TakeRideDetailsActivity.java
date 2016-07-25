package com.lyftoxi.lyftoxi;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lyftoxi.lyftoxi.dao.Location;
import com.lyftoxi.lyftoxi.dao.Ride;
import com.lyftoxi.lyftoxi.dao.TakeRide;
import com.lyftoxi.lyftoxi.singletons.CurrentUserInfo;
import com.lyftoxi.lyftoxi.singletons.CurrentUserInterestedRides;
import com.lyftoxi.lyftoxi.singletons.RideInfo;
import com.lyftoxi.lyftoxi.util.Constants;
import com.lyftoxi.lyftoxi.util.HttpRestUtil;
import com.lyftoxi.lyftoxi.util.LyftoxiFirebase;
import com.lyftoxi.lyftoxi.util.RoundImage;
import com.lyftoxi.lyftoxi.util.Util;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class TakeRideDetailsActivity extends BaseActivity {

    private TextView takeRideDetailsPrice, takeRideDetailsSource, takeRideDetailsDestination,
            takeRideDetailsStartTime, takeRideDetailsCarBrand, takeRideDetailsCarModel, takeRideDetailsCarNumber,
            takeRideDetailsUserMessage, takeRideDetailsCarColor;

    private CheckBox takeRideDetailsRadioAc,takeRideDetailsRadioMusic,takeRideDetailsRadioSmoking,
            takeRideDetailsRadioAirbag, takeRideDetailsLuggage;

    private SimpleDateFormat sdf =  new SimpleDateFormat(Constants.DATE_TIME_FORMAT_12HR_FORMAT);

    private Button takeRideDetailsCall, takeRideDetailSms;

    private ImageView takeRideDetailsCarLogo;

    // private Button takeRideInterested;

    private RideInfo seletctedRide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_ride_details);

        takeRideDetailsPrice = (TextView) findViewById(R.id.takeRideDetailsPrice);
        takeRideDetailsSource = (TextView) findViewById(R.id.takeRideDetailsSource);
        takeRideDetailsDestination = (TextView) findViewById(R.id.takeRideDetailsDestination);
        takeRideDetailsStartTime = (TextView) findViewById(R.id.takeRideDetailsStartTime);
        takeRideDetailsCarBrand = (TextView) findViewById(R.id.takeRideDetailsCarBrand);
        takeRideDetailsCarModel = (TextView) findViewById(R.id.takeRideDetailsCarModel);
        takeRideDetailsCarNumber = (TextView) findViewById(R.id.takeRideDetailsCarNumber);
        takeRideDetailsUserMessage = (TextView) findViewById(R.id.takeRideDetailsUserMessage);
        takeRideDetailsCarColor = (TextView) findViewById(R.id.takeRideDetailsCarColor);
        //takeRideDetailsPhone = (TextView) findViewById(R.id.takeRideDetailsPhone);

        takeRideDetailsCarLogo = (ImageView) findViewById(R.id.takeRideDetailsCarLogo);

        takeRideDetailsRadioAc = (CheckBox) findViewById(R.id.takeRideDetailsRadioAc);
        takeRideDetailsRadioAc.setEnabled(false);
        takeRideDetailsRadioMusic = (CheckBox) findViewById(R.id.takeRideDetailsRadioMusic);
        takeRideDetailsRadioMusic.setEnabled(false);
        takeRideDetailsRadioSmoking = (CheckBox) findViewById(R.id.takeRideDetailsRadioSmoking);
        takeRideDetailsRadioSmoking.setEnabled(false);
        takeRideDetailsRadioAirbag = (CheckBox) findViewById(R.id.takeRideDetailsRadioAirbag);
        takeRideDetailsRadioAirbag.setEnabled(false);
        takeRideDetailsLuggage = (CheckBox) findViewById(R.id.takeRideDetailsLuggage);
        takeRideDetailsLuggage.setEnabled(false);

        takeRideDetailsCall = (Button) findViewById(R.id.takeRideDetailsCall);
        takeRideDetailSms = (Button) findViewById(R.id.takeRideDetailsSms);

        // takeRideInterested = (Button) findViewById(R.id.takeRideInterested);

        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.sample_profile_pic);
        collapsingToolbarLayout.setBackground(new BitmapDrawable(getResources(), bm));

        seletctedRide = RideInfo.getInstance();
        if (null != seletctedRide) {

            downloadUserProfilePic(seletctedRide.getRideOf().getUID());
            getSupportActionBar().setTitle(seletctedRide.getRideOf().getName()+" ("+
                    Constants.genderLookup.get(seletctedRide.getRideOf().getSex())+")");
            takeRideDetailsPrice.setText(seletctedRide.getFare() + "");
            takeRideDetailsSource.setText(seletctedRide.getSourceName());
            takeRideDetailsDestination.setText(seletctedRide.getDestinationName());
            takeRideDetailsStartTime.setText(sdf.format(seletctedRide.getStarTime()));
            takeRideDetailsCarBrand.setText(seletctedRide.getCar().getCarBrand());
            takeRideDetailsCarModel.setText(seletctedRide.getCar().getCarModel());
            takeRideDetailsCarNumber.setText(seletctedRide.getCar().getCarNo());
            takeRideDetailsCarColor.setText(seletctedRide.getCar().getCarColor());
            takeRideDetailsUserMessage.setText(seletctedRide.getUserMessage());
            //takeRideDetailsPhone.setText(seletctedRide.getRideOf().getPhNo());


            takeRideDetailsRadioAc.setChecked(seletctedRide.getCar().isAcAvailable());
            takeRideDetailsRadioMusic.setChecked(seletctedRide.getCar().isMusicAvailable());
            takeRideDetailsRadioSmoking.setChecked(seletctedRide.getCar().isSmokingAllowed());
            takeRideDetailsRadioAirbag.setChecked(seletctedRide.getCar().isAirbagAvailable());
            takeRideDetailsLuggage.setChecked(seletctedRide.getCar().isLuggageAllowed());


            Bitmap carLogo;
            if(null!=seletctedRide.getCar().getCarBrand() && !seletctedRide.getCar().getCarBrand().trim().equals(""))
            {
                int id;
                if(seletctedRide.getCar().getCarBrand().contains("Rented"))
                {
                    id= getResources().getIdentifier("rented", "drawable",getPackageName());
                }
                else {
                    id = getResources().getIdentifier(Util.getResourceNameFromDisplayName(seletctedRide.getCar().getCarBrand()), "drawable", getPackageName());
                }
                carLogo = BitmapFactory.decodeResource(getResources(),id);
                if(null==carLogo)
                {
                    carLogo = BitmapFactory.decodeResource(getResources(),R.drawable.my_brand);
                }

            }
            else
            {
                carLogo = BitmapFactory.decodeResource(getResources(),R.drawable.my_brand);
            }

            takeRideDetailsCarLogo.setImageBitmap(carLogo);

            takeRideDetailsCall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + seletctedRide.getRideOf().getPhNo()));
                    startActivity(intent);
                }
            });

            takeRideDetailSms.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    StringBuffer sb =  new StringBuffer();
                    sb.append("Hi ");
                    sb.append(seletctedRide.getRideOf().getName());
                    sb.append(". I am interested in your ride from ");
                    sb.append(seletctedRide.getSourceName());
                    sb.append(" to ");
                    sb.append(seletctedRide.getDestinationName());
                    sb.append(" @ ");
                    sb.append(sdf.format(seletctedRide.getStarTime()));
                    sb.append(" shared by http://www.lyftoxi.com APP");

                    Intent sendIntent = new Intent(Intent.ACTION_VIEW);
                    sendIntent.putExtra("address", seletctedRide.getRideOf().getPhNo());
                    sendIntent.putExtra("sms_body", sb.toString());
                    sendIntent.setType("vnd.android-dir/mms-sms");
                    startActivity(sendIntent);
                }
            });

/*            Log.d("lyftoxi.debug","interested "+seletctedRide.isInterested());
            if(seletctedRide.isInterested())
            {
                takeRideInterested.setText(R.string.not_interested);
            }

            takeRideInterested.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(seletctedRide.isInterested())
                    {
                      new  RemoveInterestedRide().execute(seletctedRide.getId(),CurrentUserInfo.getInstance().getId());
                    }
                    else {
                        new AddInterestedRide().execute();
                    }
                }
            });*/
        }
    }

    private void downloadUserProfilePic(String userId)
    {
        if(null==userId || userId.trim().equals(""))
        {
            return;
        }
        final String profilePicFileName = userId+"_profile_pic.jpg";
        Log.d("gog.debug ","profilePicFileName "+profilePicFileName);
        StorageReference storageRef = LyftoxiFirebase.storageRef;
        StorageReference profileImageRef = storageRef.child("userProfilePics/"+profilePicFileName);
        // profileImageRef.getDownloadUrl();
        final long ONE_MEGABYTE = 500 * 500;
        profileImageRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                collapsingToolbarLayout.setBackground(new BitmapDrawable(getResources(),bitmap));
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception exception) {
                Log.d("lyftoxi.debug","Firebase: profile pic download failed");
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.profile_pic_placeholder_large);
                collapsingToolbarLayout.setBackground(new BitmapDrawable(getResources(),bitmap));
            }
        });
    }


    private void startMyInterestedRideActivity()
    {
        Intent myInterestedRides = new Intent(this,MyInterestedRides.class);
        startActivity(myInterestedRides);
    }


}
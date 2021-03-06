package com.shaukan.gabriel.caripacar;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lorentzos.flingswipe.SwipeFlingAdapterView;
import com.onesignal.OneSignal;
import com.shaukan.gabriel.caripacar.Cards.Cards;
import com.shaukan.gabriel.caripacar.Cards.arrayAdapter;
import com.shaukan.gabriel.caripacar.Matches.MatchesActivity;
import com.shaukan.gabriel.caripacar.Utils.SendNotification;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Cards cards_data[];
    private com.shaukan.gabriel.caripacar.Cards.arrayAdapter arrayAdapter;
    private int i;
    private FirebaseAuth mAuth;

    private  String currentUId;

    private DatabaseReference usersDb;

    ListView listView;
    List<Cards> rowItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Subscribes user to Onesignal
        OneSignal.startInit(this).init();
        OneSignal.setSubscription(true);
        OneSignal.idsAvailable(new OneSignal.IdsAvailableHandler() {
            @Override
            public void idsAvailable(String userId, String registrationId) {
                FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).child("notificationKey").setValue(userId);
            }
        });
        OneSignal.setInFocusDisplaying(OneSignal.OSInFocusDisplayOption.None);

        usersDb = FirebaseDatabase.getInstance().getReference().child("Users");
        mAuth = FirebaseAuth.getInstance();
        currentUId = mAuth.getCurrentUser().getUid();

        checkUserSex();

        rowItems = new ArrayList<Cards>();

        arrayAdapter = new arrayAdapter(this, R.layout.item, rowItems);

        SwipeFlingAdapterView flingContainer = (SwipeFlingAdapterView) findViewById(R.id.frame);

        flingContainer.setAdapter(arrayAdapter);
        flingContainer.setFlingListener(new SwipeFlingAdapterView.onFlingListener() {
            @Override
            public void removeFirstObjectInAdapter() {
                // this is the simplest way to delete an object from the Adapter (/AdapterView)
                Log.d("LIST", "removed object!");
                rowItems.remove(0);
                arrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onLeftCardExit(Object dataObject) {
                //Do something on the left!
                //You also have access to the original object.
                //If you want to use it just cast it (String) dataObject
                Cards obj = (Cards) dataObject;
                String userId = obj.getUserId();
                usersDb.child(userId).child("connections").child("nope").child(currentUId).setValue(true);
            }

            @Override
            public void onRightCardExit(Object dataObject) {
                Cards obj = (Cards) dataObject;
                String userId = obj.getUserId();
                String notificationKey = obj.getNotificationKey();
                usersDb.child(userId).child("connections").child("yep").child(currentUId).setValue(true);
                isConnectionMatch(userId, notificationKey);

            }

            @Override
            public void onAdapterAboutToEmpty(int itemsInAdapter) {
            }

            @Override
            public void onScroll(float scrollProgressPercent) {

            }
        });


        // Optionally add an OnItemClickListener
        flingContainer.setOnItemClickListener(new SwipeFlingAdapterView.OnItemClickListener() {
            @Override
            public void onItemClicked(int itemPosition, Object dataObject) {
            }
        });
    }

    //Checks if there is a match or not
    private void isConnectionMatch(String userId, final String notificationKey) {
        DatabaseReference currentUserConnectionsDb = usersDb.child(currentUId).child("connections").child("yep").child(userId);
        currentUserConnectionsDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                    Toast.makeText(MainActivity.this, "Anda dapat match baru", Toast.LENGTH_LONG).show();

                    String key = FirebaseDatabase.getInstance().getReference().child("Chat").push().getKey();


                    usersDb.child(dataSnapshot.getKey()).child("connections").child("matches").child(currentUId).child("ChatId").setValue(key);
                    usersDb.child(currentUId).child("connections").child("matches").child(dataSnapshot.getKey()).child("ChatId").setValue(key);

                    new SendNotification("Anda dapat match baru", "Ada yang mau kenalan", notificationKey);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    //checks user sex and gets opposite sex to display in main activity
    private String userSex;
    private String oppositeUserSex;
    public void checkUserSex() {
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference userDb = usersDb.child(user.getUid());
        userDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        if (dataSnapshot.child("sex").getValue() != null) {
                            userSex = dataSnapshot.child("sex").getValue().toString();
                            switch (userSex) {
                                case "Male":
                                    oppositeUserSex = "Female";
                                    break;
                                case "Female":
                                    oppositeUserSex = "Male";
                                    break;
                            }
                            getOppositeSexUsers();
                        }
                    }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    public void getOppositeSexUsers() {
        usersDb.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded (@NonNull DataSnapshot dataSnapshot, @Nullable String s){
                if (dataSnapshot.child("sex").getValue() != null) {
                    if (dataSnapshot.exists() && !dataSnapshot.child("connections").child("nope").hasChild(currentUId) && !dataSnapshot.child("connections").child("yep").hasChild(currentUId) && dataSnapshot.child("sex").getValue().toString().equals(oppositeUserSex)) {
                        String profileImageUrl = "default";
                        if (!dataSnapshot.child("profileImageUrl").getValue().equals("default")) {
                            profileImageUrl = dataSnapshot.child("profileImageUrl").getValue().toString();
                        }
                        Cards item = new Cards(dataSnapshot.getKey(), dataSnapshot.child("Name").getValue().toString(), profileImageUrl, dataSnapshot.child("Occupation").getValue().toString(), dataSnapshot.child("Age").getValue().toString(), dataSnapshot.child("notificationKey").getValue().toString() );
                        rowItems.add(item);
                        arrayAdapter.notifyDataSetChanged();
                    }
                }

            }
            @Override
            public void onChildChanged (@NonNull DataSnapshot dataSnapshot, @Nullable String s){
            }
            @Override
            public void onChildRemoved (@NonNull DataSnapshot dataSnapshot){
            }
            @Override
            public void onChildMoved (@NonNull DataSnapshot dataSnapshot, @Nullable String s){
            }
            @Override
            public void onCancelled (@NonNull DatabaseError databaseError){
            }
        });
    }


    //Logs out user
    public void logoutUser(View view) {
        mAuth.signOut();
        OneSignal.setSubscription(false);
        Intent intent = new Intent(MainActivity.this, ChooseLoginRegistrationActivity.class);
        startActivity(intent);
        finish();
        return;
    }

    //Intent for settings activity
    public void goToSettings(View view) {
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(intent);
        return;
    }

    //Intent for matches activity
    public void goToMatches(View view) {
        Intent intent = new Intent(MainActivity.this, MatchesActivity.class);
        startActivity(intent);
        return;
    }
}
package com.example.drivy1;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DriverSettingsActivity extends AppCompatActivity {

    private EditText nameField,phoneField,carField;
    private Button back,confirm;
    private RadioGroup radioGroup;

    private FirebaseAuth auth;
    private DatabaseReference driverDatabase;

    private String userId;
    private String mName;
    private String mPhone;
    private String mCar;
    private String service;

    //Image :
    private String profileUrl;
    private ImageView profileImage ;
    private Uri resultUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_settings);

        nameField = (EditText) findViewById(R.id.name);
        phoneField = (EditText) findViewById(R.id.phone);
        carField = (EditText) findViewById(R.id.car);

        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);

        profileImage = (ImageView) findViewById(R.id.profileImage);

        back = (Button) findViewById(R.id.back);
        confirm = (Button) findViewById(R.id.confirm);

        auth = FirebaseAuth.getInstance();
        userId = auth.getCurrentUser().getUid();

        driverDatabase= FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(userId);
        getUserInfo();

        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,1);
            }
        });

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserInformation();
               Intent intent = new Intent(DriverSettingsActivity.this, DriverMapActivity.class);
                startActivity(intent);
                return;
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DriverSettingsActivity.super.onBackPressed();
                Intent intent = new Intent(DriverSettingsActivity.this, DriverMapActivity.class);
                startActivity(intent);
                return;
            }
        });

    }

    private void getUserInfo()
    {
        driverDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0)
                {
                    Map<String,Object> map = (Map<String,Object>) dataSnapshot.getValue();
                    if(map.get("name")!=null)
                    {
                        mName=map.get("name").toString();
                        nameField.setText(mName);
                    }
                    if(map.get("phone")!=null)
                    {
                        mPhone=map.get("phone").toString();
                        phoneField.setText(mPhone);
                    }
                    if(map.get("car")!=null)
                    {
                        mCar=map.get("car").toString();
                        carField.setText(mCar);
                    } if(map.get("service")!=null)
                    {
                        service=map.get("service").toString();
                        switch (service){
                            case "DrivyX":
                                radioGroup.check(R.id.DrivyX);
                            case "DrivyBlack":
                                radioGroup.check(R.id.DrivyBlack);
                        }
                        carField.setText(service);
                    }
                    if(map.get("profileImageUrl")!=null)
                    {
                        profileUrl = map.get("profileImageUrl").toString();
                        Glide.with(getApplication()).load(profileUrl).into(profileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    private void saveUserInformation() {

        mName = nameField.getText().toString();
        mPhone = phoneField.getText().toString();
        mCar = carField.getText().toString();

        int selectID = radioGroup.getCheckedRadioButtonId();
        final RadioButton radioButton = (RadioButton) findViewById(selectID);
        if(radioButton.getText() == null)
            return;
        service = radioButton.getText().toString();
        Map userInfo = new HashMap();
        userInfo.put("name", mName);
        userInfo.put("phone", mPhone);
        userInfo.put("car", mCar);
        userInfo.put("service", service);
        driverDatabase.updateChildren(userInfo);

        if (resultUri != null)
        {
            StorageReference filePath = FirebaseStorage.getInstance().getReference().child("profile_images").child(userId);
            Bitmap bitmap = null;
            try{
                bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(),resultUri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG,20,baos);
            byte[] data = baos.toByteArray();
            UploadTask uploadTask = filePath.putBytes(data);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    //finish();
                    return;

                }
            });

            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Task<Uri> urlTask = taskSnapshot.getStorage().getDownloadUrl();
                    while (!urlTask.isSuccessful());
                    Uri downloadUrl = urlTask.getResult();
                    Map newImage = new HashMap();
                    newImage.put("profileImageUrl",downloadUrl.toString());
                    driverDatabase.updateChildren(newImage);

                    //finish();
                    return;
                }
            });

        }else{
            //finish();
            return;}
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1  && resultCode == Activity.RESULT_OK)
        {
            final Uri imageUri = data.getData();
            resultUri = imageUri;
            profileImage.setImageURI(resultUri);
        }
    }
}

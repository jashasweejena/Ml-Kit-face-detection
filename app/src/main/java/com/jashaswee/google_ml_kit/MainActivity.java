package com.jashaswee.google_ml_kit;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;

import java.io.File;
import java.util.List;

import pl.aprilapps.easyphotopicker.DefaultCallback;
import pl.aprilapps.easyphotopicker.EasyImage;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    ImageView imageView;
    TextView details;
    Button gallery, camera;
    File mImageFile;
    float mFinalProb;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        details = findViewById(R.id.details);

        imageView = findViewById(R.id.imageView);

        gallery = findViewById(R.id.gallery);
        camera = findViewById(R.id.camera);

        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Opens camera dialog
                EasyImage.openCamera(MainActivity.this, 100);
            }
        });

        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Opens gallery picker
                EasyImage.openGallery(MainActivity.this, 100);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        EasyImage.handleActivityResult(requestCode, resultCode, data, this, new DefaultCallback() {
            @Override
            public void onImagePickerError(Exception e, EasyImage.ImageSource source, int type) {
                Toast.makeText(MainActivity.this, "Image picker error", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onImagePickerError: " + "Image picker error");
            }

            @Override
            public void onImagePicked(final File imageFile, EasyImage.ImageSource source, int type) {

                mImageFile = imageFile;

                FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(new BitmapFactory().decodeFile(imageFile.getAbsolutePath()));

                FirebaseVisionFaceDetectorOptions options =
                        new FirebaseVisionFaceDetectorOptions.Builder()
                                .setModeType(FirebaseVisionFaceDetectorOptions.ACCURATE_MODE)
                                .setLandmarkType(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                                .setClassificationType(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                                .setMinFaceSize(0.15f)
                                .setTrackingEnabled(true)
                                .build();


                FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
                        .getVisionFaceDetector(options);

                Task<List<FirebaseVisionFace>> result =
                        detector.detectInImage(image)
                                .addOnSuccessListener(
                                        new OnSuccessListener<List<FirebaseVisionFace>>() {
                                            @Override
                                            public void onSuccess(List<FirebaseVisionFace> faces) {

                                                for (FirebaseVisionFace face : faces) {
                                                    Rect bounds = face.getBoundingBox();
                                                    float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
                                                    float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees

                                                    // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                                                    // nose available):
                                                    FirebaseVisionFaceLandmark leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR);
                                                    if (leftEar != null) {
                                                        FirebaseVisionPoint leftEarPos = leftEar.getPosition();
                                                    }

                                                    // If classification was enabled:
                                                    if (face.getSmilingProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                                                        float smileProb = face.getSmilingProbability();
                                                        float finalProb = smileProb * 100;
                                                        mFinalProb = finalProb;
                                                        String prob = "";
                                                        if (smileProb != 0) {
                                                            prob = String.valueOf(finalProb) + "%" + "Happy";
                                                        } else {
                                                            Toast.makeText(MainActivity.this, "Put up a smile :) ", Toast.LENGTH_SHORT).show();
                                                        }

                                                        showAlertDialog();

                                                    }
                                                    if (face.getRightEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                                                        float rightEyeOpenProb = face.getRightEyeOpenProbability();
                                                    }

                                                    // If face tracking was enabled:
                                                    if (face.getTrackingId() != FirebaseVisionFace.INVALID_ID) {
                                                        int id = face.getTrackingId();
                                                    }
                                                }
                                            }
                                        })
                                .addOnFailureListener(
                                        new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                // Task failed with an exception
                                                // ...
                                                Log.d(TAG, "onFailure: " + "Firebase failed to detect face");
                                                Toast.makeText(MainActivity.this, "Firebase failed to detect face", Toast.LENGTH_SHORT).show();
                                            }
                                        });

                FirebaseVisionFace faces;

            }

        });
    }

    private void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View dialog = inflater.inflate(R.layout.dialog_face, null, false);

        builder.setView(dialog)
                .setTitle("Be happy :)");

        ImageView smilingFace = dialog.findViewById(R.id.image_face);
        TextView smileCoefficient = dialog.findViewById(R.id.happiness_text);

        smilingFace.setImageBitmap(new BitmapFactory().decodeFile(mImageFile.getAbsolutePath()));
        smileCoefficient.setText(" " + String.valueOf(mFinalProb) + "%" + " happy!");

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


}

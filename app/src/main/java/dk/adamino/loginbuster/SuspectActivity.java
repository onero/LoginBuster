package dk.adamino.loginbuster;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Adamino.
 */

public class SuspectActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_PATH = "dk.adamino.LoginBuster.image_path";
    public static final String EXTRA_CRIME_TIME = "dk.adamino.LoginBuster.crime_time";

    private ImageView mSuspectView;
    private TextView mCrimeTime, mBlameText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suspect);

        mSuspectView = findViewById(R.id.imgSuspect);
        mCrimeTime = findViewById(R.id.txtCrimeTime);
        mBlameText = findViewById(R.id.txtBlame);

        Intent intent = getIntent();
        String suspectImageLocation = intent.getStringExtra(EXTRA_IMAGE_PATH);
        String crimeTimeAsString = intent.getStringExtra(EXTRA_CRIME_TIME);

        Bitmap suspectImage = BitmapFactory.decodeFile(suspectImageLocation);
        mSuspectView.setImageBitmap(suspectImage);

        mCrimeTime.setText(crimeTimeAsString);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        MainActivity.sNavigatedBack = true;
    }

    /**
     * Get Suspect intent
     * @param content
     * @param imagePath
     * @return SuspectActivity intent
     */
    public static Intent newIntent(Context content, String imagePath, String crimeTime) {
        Intent intent = new Intent(content, SuspectActivity.class);
        intent.putExtra(EXTRA_IMAGE_PATH, imagePath);
        intent.putExtra(EXTRA_CRIME_TIME, crimeTime);
        return intent;
    }
}

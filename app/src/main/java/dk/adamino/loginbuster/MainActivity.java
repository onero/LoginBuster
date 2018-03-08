package dk.adamino.loginbuster;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import dk.adamino.loginbuster.BLL.MediaService;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "Adamino";
    public static final int MY_PERMISSIONS_REQUEST_ACCESS_CODE = 1;
    private static final int PIN_CODE_LENGTH = 4;
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final List<Integer> PINCODE = new ArrayList<>(Arrays.asList(1, 2, 3, 4));

    public static boolean sNavigatedBack = false;

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private LinearLayout mPincodeLayout;
    private ImageView mFirstFailedAttempt, mSecondFailedAttempt;
    private TextView mPincode, mBadPassword;

    private MediaService mMediaService;
    private SimpleDateFormat mCrimeTimeFormatter;
    private List<Integer> mUserInput;
    private int mAttempts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPincodeLayout = findViewById(R.id.linearPincode);
        mFirstFailedAttempt = findViewById(R.id.imgFirst_attempt_failed);
        mFirstFailedAttempt.setVisibility(View.INVISIBLE);
        mSecondFailedAttempt = findViewById(R.id.imgSecond_attempt_failed);
        mSecondFailedAttempt.setVisibility(View.INVISIBLE);
        mPincode = findViewById(R.id.txtPincode);
        mPincode.setTextSize(32);
        mBadPassword = findViewById(R.id.txtBadPassword);
        mBadPassword.setVisibility(View.INVISIBLE);

        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        mMediaService = new MediaService();
        mCrimeTimeFormatter = new SimpleDateFormat("dd-MM-yyyy - HH:mm", Locale.getDefault());
        mUserInput = new ArrayList<>();
        mAttempts = 0;

        setupPincodeLayout();
        setupCamera();
    }

    public void restart() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sNavigatedBack) {
            sNavigatedBack = false;
            restart();
        }
    }

    private void setupPincodeLayout() {
        mPincodeLayout.removeAllViews();
        int number = 1;
        for (int i = 0; i < 3; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            for (int j = 0; j < 3; j++) {
                Digit digit = new Digit(this, number++);
                digit.setTextSize(32);
                row.addView(digit);
            }
            mPincodeLayout.addView(row);
        }
    }

    /**
     * Add user input to mUserInput
     *
     * @param number
     */
    private void registerUserInput(int number) {
        clearPincode();
        mUserInput.add(number);
        String asterisks = "";
        for (int input : mUserInput) {
            asterisks += "*";
        }
        mPincode.setText(asterisks);
        // Check if user has input max length
        if (mUserInput.size() == PIN_CODE_LENGTH) {
            // Account for login attempts
            mAttempts++;
            boolean pincodeCorrect = verifyPincode();

            if (pincodeCorrect) {
                // TODO ALH: Consider overkill implementation...
                Log.d(TAG, "Success!");
                // Check if we should name and shame!
            } else if (!pincodeCorrect && mAttempts == MAX_FAILED_ATTEMPTS) {
                try {
                    // Take picture and frame the bastard in new activity!
                    executeOrderSixtySix();
                    // Reset attempts
                    mAttempts = 0;
                } catch (CameraAccessException e) {
                    // Something seriously wrong that seriously shouldn't happen occurred... No Seriously!
                    Log.e(TAG, e.getMessage());
                }
                // Indicate failed attempt to user
            } else {
                mBadPassword.setVisibility(View.VISIBLE);
                addFailedAttemptToView();
            }
            // Reset user input
            mUserInput.clear();
            clearPincode();
        }
    }

    private void clearPincode() {
        mPincode.setText("");
    }

    /**
     * Indicate failure to user
     */
    private void addFailedAttemptToView() {
        switch (mAttempts) {
            case 1:
                mFirstFailedAttempt.setVisibility(View.VISIBLE);
                break;
            case 2:
                mSecondFailedAttempt.setVisibility(View.VISIBLE);
            default:
                break;
        }
    }

    // Check user input against the incredible secret pincode...
    private boolean verifyPincode() {
        for (int i = 0; i < mUserInput.size(); i++) {
            if (!Objects.equals(mUserInput.get(i), PINCODE.get(i)))
                return false;
        }
        return true;
    }


    private class Digit extends android.support.v7.widget.AppCompatButton implements View.OnClickListener {

        private int mNumber;

        public Digit(Context context, int number) {
            super(context);
            setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
            mNumber = number;
            setText(mNumber + "");
            setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            registerUserInput(mNumber);
        }
    }

    /**
     * Called when the picture is finalized and persisted on disk
     */
    public final CameraCaptureSession.CaptureCallback suspectCaptureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Intent suspectIntent = SuspectActivity.newIntent(MainActivity.this,
                    mMediaService.getSuspectFileLocation(),
                    mCrimeTimeFormatter.format(new Date()));
            startActivity(suspectIntent);
        }
    };

    /**
     * Called when image is created and ready to be saved
     */
    public final ImageReader.OnImageAvailableListener onImageAvailableListener = (ImageReader imReader) -> {
        final Image image = imReader.acquireLatestImage();
        final ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        final byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        mMediaService.saveImageToDisk(bytes);
        image.close();
    };


    private void executeOrderSixtySix() throws CameraAccessException {
        final CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCameraDevice.getId());

        // Prepare image
        Size[] jpegSizes = null;
        StreamConfigurationMap streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (streamConfigurationMap != null) {
            jpegSizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
        }
        final boolean jpegSizesNotEmpty = jpegSizes != null && 0 < jpegSizes.length;
        int width = jpegSizesNotEmpty ? jpegSizes[0].getWidth() : 640;
        int height = jpegSizesNotEmpty ? jpegSizes[0].getHeight() : 480;
        final ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
        final List<Surface> outputSurfaces = new ArrayList<>();
        outputSurfaces.add(reader.getSurface());

        // Setup CaptureRequest to take picture
        final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        captureBuilder.addTarget(reader.getSurface());
        captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation(characteristics));
        reader.setOnImageAvailableListener(onImageAvailableListener, null);
        mCameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        try {
                            session.capture(captureBuilder.build(), suspectCaptureCallbackListener, null);
                        } catch (final CameraAccessException e) {
                            Log.e(TAG, " exception occurred while accessing camera", e);
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    }
                }
                , null);
    }

    /**
     * Open connection to camera
     */
    private void setupCamera() {
        try {
            String cameraId = mCameraManager.getCameraIdList()[1];
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // For API 23 and up
                checkPermissions();
            }
            mCameraManager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Handle CameraDevice callbacks
     * Used to open and close connection to camera!
     */
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, "Camera Opened");
            mCameraDevice = camera;
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d(TAG, "Camera Closed");
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.d(TAG, "Camera Closed");
            camera.close();
            mCameraDevice = null;
        }
    };

    /**
     * Calculate and return the need JPEG orientation
     *
     * @param c
     * @return
     */
    private int getJpegOrientation(CameraCharacteristics c) {
        int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN)
            return 0;
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;

        // Reverse device orientation for front-facing cameras
        boolean facingFront = c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
        if (facingFront) deviceOrientation = -deviceOrientation;

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        int jpegOrientation = (sensorOrientation + deviceOrientation + 360) % 360;

        return jpegOrientation;
    }


    /**
     * Verify that user allowed usage of camera
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_CODE: {
                if (!(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    checkPermissions();
                }
            }
        }
    }

    /**
     * checking  permissions at Runtime.
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        final String[] requiredPermissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
        };
        final List<String> neededPermissions = new ArrayList<>();
        for (final String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    permission) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(permission);
            }
        }
        if (!neededPermissions.isEmpty()) {
            requestPermissions(neededPermissions.toArray(new String[]{}),
                    MY_PERMISSIONS_REQUEST_ACCESS_CODE);
        }
    }

}

package dk.adamino.loginbuster.BLL;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Adamino.
 */

public class MediaService {
    private static final File MEDIA_STORAGE_DIR = new File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES), "LoginBuster");

    private static final File SUSPECT_FILE = new File(MEDIA_STORAGE_DIR.getPath() + File.separator +
            "IMG_"+ "SUSPECT" + ".jpg");

    private static final String TAG = "Media";

    public void saveImageToDisk(final byte[] bytes) {
        // Create the storage directory if it does not exist
        if (! MEDIA_STORAGE_DIR.exists()){
            MEDIA_STORAGE_DIR.mkdirs();
        }
        // Write SUSPECT_FILE
        try (final OutputStream output = new FileOutputStream(SUSPECT_FILE)) {
            output.write(bytes);
        } catch (final IOException e) {
            Log.e(TAG, "Exception occurred while saving picture to external storage ", e);
        }
    }

    public String getSuspectFileLocation() {
        return SUSPECT_FILE.getAbsolutePath();
    }
}

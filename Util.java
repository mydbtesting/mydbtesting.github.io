package com.albums.gallery.folder.photo.picasa.app.photogallery;

import static java.lang.String.format;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.albums.gallery.folder.photo.picasa.app.photogallery.activity.Image_Pager_Activity;
import com.google.gson.Gson;
import com.albums.gallery.folder.photo.picasa.app.photogallery.BuildConfig;
import com.albums.gallery.folder.photo.picasa.app.photogallery.R;
import com.albums.gallery.folder.photo.picasa.app.photogallery.model_data.AlbumData;
import com.albums.gallery.folder.photo.picasa.app.photogallery.model_data.ImageData;
import com.albums.gallery.folder.photo.picasa.app.photogallery.model_data.VideoData;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.CharacterIterator;
import java.text.SimpleDateFormat;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Util {
/*TODO                       SI     BINARY
TODO
                   0:        0 B        0 B
                  27:       27 B       27 B
                 999:      999 B      999 B
                1000:     1.0 kB     1000 B
                1023:     1.0 kB     1023 B
                1024:     1.0 kB    1.0 KiB
                1728:     1.7 kB    1.7 KiB
              110592:   110.6 kB  108.0 KiB
             7077888:     7.1 MB    6.8 MiB
           452984832:   453.0 MB  432.0 MiB
         28991029248:    29.0 GB   27.0 GiB
       1855425871872:     1.9 TB    1.7 TiB
 9223372036854775807:     9.2 EB    8.0 EiB   (Long.MAX_VALUE)*/

    String[] size_projection = {MediaStore.MediaColumns.DATA, MediaStore.Images.Media.SIZE};
    SimpleDateFormat title_formatter = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    //SI (1 k = 1,000)
    public String humanReadableByteCountSI(long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return format(Locale.getDefault(), "%.1f %cB", bytes / 1000.0, ci.current());
    }

    //Binary (1 Ki = 1,024)
    public String humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format(Locale.getDefault(), "%.1f %ciB", value / 1024.0, ci.current());
    }

    public String getRealPathFromURI(Activity activity, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = activity.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public Dialog init_loading_dialog(Activity activity) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_loading);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        dialog.getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }

    public void hide_loading_dialog(Dialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public void show_loading_dialog(Dialog dialog) {
        if (dialog != null && !dialog.isShowing()) {
            dialog.show();
        }
    }

    public void get_images_from_bucket(Activity activity, String bucket_id, ArrayList<ImageData> image_dataArrayList, String short_order) {
        image_dataArrayList.clear();
        Log.e("TAG", "get_images_from_bucket: " + image_dataArrayList.size());
        String[] projection = {MediaStore.Images.Media._ID, MediaStore.MediaColumns.DATA, MediaStore.Images.Media.BUCKET_ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.SIZE, MediaStore.Images.Media.HEIGHT, MediaStore.Images.Media.WIDTH, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.MediaColumns.DATE_ADDED, MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.DATE_MODIFIED};
        Cursor cursor = activity.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, MediaStore.Images.Media.BUCKET_ID + "=?", new String[]{bucket_id}, short_order);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String image_path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA));
                long imageSize = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE));
                long date_added = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED));
                long datetaken = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN));
                String height = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT));
                String width = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));
                String folder_name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME));
                long _id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));

                ImageData albumImage = new ImageData();
                albumImage.setPath(image_path);
                albumImage.setSize(imageSize);
                albumImage.setHeight(height);
                albumImage.setWidth(width);
                albumImage.setName(name);
                albumImage.setDatetaken(datetaken);
                albumImage.setDate_added(date_added);
                albumImage.setFolder_name(folder_name);
                albumImage.set_id(_id);
                image_dataArrayList.add(albumImage);
            }
            cursor.close();
        }
    }

    public void get_all_buckets(Activity activity, ArrayList<AlbumData> arrayAlbumDetails, ArrayList<String> ids, String order) {
        ids.clear();
        arrayAlbumDetails.clear();
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.BUCKET_ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.MediaColumns.SIZE};
        Cursor cursor = activity.getContentResolver().query(uri, projection, null, null, order);
        if (cursor != null) {

            int column_bucket_id = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID);
            int column_bucket_display_name = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
            int column_image_path = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);

            while (cursor.moveToNext()) {
                String bucket_id = cursor.getString(column_bucket_id);
                if (!ids.contains(bucket_id)) {
                    String bucket_display_name = cursor.getString(column_bucket_display_name);
                    String image_path = cursor.getString(column_image_path);
                    AlbumData albumDetails = new AlbumData();
                    albumDetails.setBucket_display_name(bucket_display_name);
                    albumDetails.setBucket_id(bucket_id);
                    albumDetails.setPath(image_path);
                    arrayAlbumDetails.add(albumDetails);
                    ids.add(bucket_id);
                }
            }
            cursor.close();
        }
    }

    public void get_all_buckets_by_size(Activity activity, ArrayList<AlbumData> arrayAlbumDetails, ArrayList<String> ids, String short_order) {
        ids.clear();
        arrayAlbumDetails.clear();
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.BUCKET_ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.SIZE};
        Cursor cursor = activity.getContentResolver().query(uri, projection, null, null, short_order);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String bucket_id = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID));
                if (!ids.contains(bucket_id)) {
                    String bucket_display_name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME));
                    String image_path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA));

                    long size = get_size_details(uri, activity, bucket_id);

                    AlbumData albumDetails = new AlbumData();
                    albumDetails.setBucket_display_name(bucket_display_name);
                    albumDetails.setBucket_id(bucket_id);
                    albumDetails.setPath(image_path);
                    albumDetails.setBucket_size(size);
                    arrayAlbumDetails.add(albumDetails);
                    ids.add(bucket_id);
                }
            }
            cursor.close();

            for (int i = 0; i < arrayAlbumDetails.size(); i++) {
                Log.d("TAG", "get_all_buckets_by_size: " + arrayAlbumDetails.get(i).getBucket_size());
            }

            short_size(arrayAlbumDetails);
        }
    }

    private long get_size_details(Uri uri, Activity activity, @NonNull final String bucketId) {
        long size = 0;
        Cursor cursor = activity.getContentResolver().query(uri, size_projection, MediaStore.Video.Media.BUCKET_ID + "=?", new String[]{bucketId}, MediaStore.Images.Media.DATE_TAKEN + " DESC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                size = size + cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE));
            }
            cursor.close();
            return size;
        }
        return 0;
    }

    public void get_all_images(Activity activity, List<ImageData> image_dataArrayList, String short_order_) {
        image_dataArrayList.clear();
        String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.Images.Media._ID, MediaStore.Images.Media.BUCKET_ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.SIZE, MediaStore.Images.Media.HEIGHT, MediaStore.Images.Media.WIDTH, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.MediaColumns.DATE_ADDED, MediaStore.Images.Media.DATE_TAKEN};
        Cursor cursor = activity.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, short_order_);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long imageSize = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE));
                long date_added = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED));
                long datetaken = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN));
                String image_path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA));
                String height = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT));
                String width = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));
                String folder_name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME));
                long _id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));


              /*  String date=title_formatter.format(datetaken);
                Log.d("TAG", "get_all_images: "+date );*/

                ImageData albumImage = new ImageData();
                albumImage.setPath(image_path);
                albumImage.setSize(imageSize);
                albumImage.setHeight(height);
                albumImage.setWidth(width);
                albumImage.setName(name);
                albumImage.setDatetaken(datetaken);
                albumImage.setDate_added(date_added);
                albumImage.setFolder_name(folder_name);
                albumImage.set_id(_id);
                image_dataArrayList.add(albumImage);
            }
            cursor.close();
            Log.d("TAG", "get_all_images: " + image_dataArrayList.size());
        }
    }

    public void get_all_video(Activity activity, ArrayList<VideoData> video_dataArrayList, String short_order) {
        video_dataArrayList.clear();
        String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.Video.Media.SIZE, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.HEIGHT, MediaStore.Video.Media.WIDTH, MediaStore.Video.Media.RESOLUTION, MediaStore.Video.VideoColumns.DURATION};
        Cursor cursor = activity.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null, short_order);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String image_path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA));
                long video_size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE));
                String video_name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME));
                String height = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT));
                String width = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH));
                long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION));

                VideoData video_data = new VideoData();
                video_data.setPath(image_path);
                video_data.setSize(video_size);
                video_data.setName(video_name);
                video_data.setHeight(height);
                video_data.setWidth(width);
                video_data.setDuration(duration);
                video_dataArrayList.add(video_data);
            }
            cursor.close();
        }
    }

    public void hide_status_bar(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            //TODO: Cut Display status bar
            activity.getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;

            //TODO: this will show navigation bar
            /*activity.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);*/

            //TODO: this will hide navigation bar
            activity.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);


            // activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.getWindow().setDecorFitsSystemWindows(false);
            } else {
                activity.getWindow().setFlags(
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                );
            }
        } else {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    public void show_status_bar(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            //TODO: this will show navigation bar
            activity.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

            //TODO: this will hide navigation bar
            /*activity.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);*/

            activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
            activity.getWindow().setNavigationBarColor(Color.TRANSPARENT);
        } else {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
            //  activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }


    /* <style name="Theme.Gallery_Pager" parent="Theme.MaterialComponents.Light.NoActionBar">
        <!-- Primary brand color. -->
        <item name="colorPrimary">@color/purple_500</item>
        <item name="colorPrimaryVariant">@color/purple_700</item>
        <item name="colorOnPrimary">@color/white</item>
        <!-- Secondary brand color. -->
        <item name="colorSecondary">@color/teal_200</item>
        <item name="colorSecondaryVariant">@color/teal_700</item>
        <item name="colorOnSecondary">@color/teal_700</item>
        <!--status bar-->
        <!--<item name="android:statusBarColor" tools:targetApi="l">@android:color/transparent</item>
        <item name="android:windowLightStatusBar" tools:targetApi="m">false</item>-->
        <item name="android:windowBackground">@null</item>

        <!--<item name="android:windowLayoutInDisplayCutoutMode" tools:targetApi="o_mr1">
            shortEdges &lt;!&ndash; default, shortEdges, or never &ndash;&gt;
        </item>-->

        <item name="fontFamily">@font/gothammedium</item>

        https://stackoverflow.com/questions/68164496/how-to-delete-an-image-in-android-10-scoped-storage-mediastore-entry-file
        https://medium.com/@vishrut.goyani9/scoped-storage-in-android-writing-deleting-media-files-ee6235d30117
    </style>
*/

    public void short_z_a(ArrayList<AlbumData> ArrayAlbumDetails) {
        Collections.sort(ArrayAlbumDetails, (v1, v2) -> {
            if (v1.getBucket_display_name() == null) {
                return (v2.getBucket_display_name() == null) ? 0 : -1;
            }
            if (v2.getBucket_display_name() == null) {
                return 1;
            }
            return v2.getBucket_display_name().compareToIgnoreCase(v1.getBucket_display_name());
        });
    }

    public void short_a_z(ArrayList<AlbumData> ArrayAlbumDetails) {
        Collections.sort(ArrayAlbumDetails, (v1, v2) -> {
            if (v2.getBucket_display_name() == null) {
                return (v1.getBucket_display_name() == null) ? 0 : -1;
            }
            if (v1.getBucket_display_name() == null) {
                return 1;
            }
            return v1.getBucket_display_name().compareToIgnoreCase(v2.getBucket_display_name());
        });
    }

    public void short_size(ArrayList<AlbumData> ArrayAlbumDetails) {
        Collections.sort(ArrayAlbumDetails, (lhs, rhs) -> {
            // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
            //return lhs.bucket_size > rhs.bucket_size ? -1 : (lhs.bucket_size < rhs.bucket_size) ? 1 : 0;
            return Long.compare(rhs.bucket_size, lhs.bucket_size);
        });
    }

    public void save_short_order_name(Activity activity, String short_order) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences("my_shared_preferences", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("short_order_name", short_order);
        editor.apply();
    }

    public void save_albums_view_type(Activity activity, String view_type) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences("my_shared_preferences", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("albums_view_type", view_type);
        editor.apply();
    }

    public void save_privacy_pin(Activity activity, ArrayList<Integer> list) {
        SharedPreferences prefs = activity.getSharedPreferences("my_shared_preferences", 0);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString("privacy_pin", json);
        editor.apply();
    }

    public void save_privacy_question(Activity activity, String question) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences("my_shared_preferences", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("privacy_question", question);
        editor.apply();
    }

    public void save_privacy_answer(Activity activity, String answer) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences("my_shared_preferences", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("privacy_answer", answer);
        editor.apply();
    }

    public void save_is_privacy_warning_shown(Activity activity, boolean is_shown) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences("my_shared_preferences", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("is_privacy_warning_shown", is_shown);
        editor.apply();
    }

    public void passVideo(Activity activity, String video_path) {
        final File videoFile = new File(video_path);
        Uri fileUri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID, videoFile);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, "video/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(intent);
    }

    public void addMedia(Context c, File f) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(f));
        c.sendBroadcast(intent);
    }

    public void removeMedia(Context c, File f) {
        ContentResolver resolver = c.getContentResolver();
        resolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.DATA + "=?", new String[]{f.getAbsolutePath()});
    }

    /* public boolean delete(final Activity activity, final File file) {
         if (file.exists()) {
             boolean delete = file.delete();
             if (delete) {
                 util.removeMedia(activity, file);
             }
             return delete;
         }
         return false;
     }*/

    /*File file = new File(image_dataArrayList.get(binding.imagePager.getCurrentItem()).getPath());
            Log.e(TAG,"trash_dialog: "+file);
            try

    {
        boolean deleted = file.delete();
        boolean delete_media = util.delete(Image_Pager_Activity.this, file);

        Log.e(TAG, "trash_dialog: " + deleted);
        Log.e(TAG, "trash_dialog: " + delete_media);
        FileUtils.delete(file);
    } catch(
    IOException e)

    {
        Log.e(TAG, "trash_dialog: " + e.getMessage());
    }*/

    public boolean delete(final Activity activity, final File file) {
        final String where = MediaStore.MediaColumns.DATA + "=?";
        final String[] selectionArgs = new String[]{
                file.getAbsolutePath()
        };
        final ContentResolver contentResolver = activity.getContentResolver();
        final Uri filesUri = MediaStore.Files.getContentUri("external");

        contentResolver.delete(filesUri, where, selectionArgs);

        if (file.exists()) {

            contentResolver.delete(filesUri, where, selectionArgs);
        }
        return !file.exists();
    }
}
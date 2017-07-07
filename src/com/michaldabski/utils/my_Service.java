package com.michaldabski.utils;

/**
 * Created by Yassine TAOUIL on 06/07/2017.
 */


        import android.app.Service;
        import android.content.Intent;
        import android.os.Bundle;
        import android.os.Handler;
        import android.os.IBinder;
        import android.util.Log;
        import android.widget.Toast;

        import com.michaldabski.filemanager.FileManagerApplication;
        import com.michaldabski.filemanager.R;
        import com.michaldabski.filemanager.favourites.FavouriteFolder;
        import com.michaldabski.filemanager.favourites.FavouritesManager;
        import com.michaldabski.filemanager.folders.FolderActivity;

        import java.io.File;

public class my_Service extends Service {

    private static final String TAG = "HelloService";

    private boolean isRunning  = false;

    @Override
    public void onCreate() {
        Log.i(TAG, "Service onCreate");
     //   Toast.makeText(getBaseContext(), "Service onCreate",Toast.LENGTH_SHORT ).show();
        isRunning = true;
    }

    private Handler handler = new Handler();
    private Runnable timedTask = new Runnable(){

        @Override
        public void run() {


            File[] files = null;
            File file = new File("/storage");// /storage/emulated
            if (file.exists()) {
                files = file.listFiles();
            }
            FileManagerApplication application = (FileManagerApplication) getApplication();
            FavouritesManager favouritesManager = application.getFavouritesManager();
            if (null != files)
                for (int j = 0; j < files.length; j++) {

                    if (!files[j].toString().contains("emulated")
                            &&!files[j].toString().contains("self")) {
                        try{
                            FavouriteFolder my_file;
                            my_file=new FavouriteFolder(files[j].getAbsolutePath(), FileUtils.DISPLAY_NAME_SD_CARD);
                            if(!favouritesManager.isFolderFavourite(files[j])) {
                                favouritesManager.addFavourite(my_file);
                            //    Toast.makeText(getBaseContext(), "new",Toast.LENGTH_SHORT ).show();
                                Intent intent = new Intent(getApplicationContext(), FolderActivity.class);
                                Bundle b = new Bundle();
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                                b.putString("new_directory", files[j].getAbsolutePath());
                                intent.putExtras(b);

                                application.getAppPreferences().setStartFolder(files[j]).saveChanges(getApplicationContext());

                                startActivity(intent);

                            }
                            //			delete(my_file);
                        }catch(Exception e){
                            e.getMessage();
                        }

                    }
                }
            favouritesManager.removeFavourite_olde_file();
            handler.postDelayed(timedTask, 500);
        }};

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "Service onStartCommand");
    //    Toast.makeText(getBaseContext(), "Service onStartCommand",Toast.LENGTH_SHORT ).show();

        handler.post(timedTask);
        //Creating new thread for my service
        //Always write your long running tasks in a separate thread, to avoid ANR
     /*   new Thread(new Runnable() {
            @Override
            public void run() {


                //Your logic that service will perform will be placed here
                //In this example we are just looping and waits for 1000 milliseconds in each loop.
                for (int i = 0; i < 5; i++) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                    }

                    if(isRunning){
                        Log.i(TAG, "Service running");
                    }
                }

                //Stop service once it finishes its task
                stopSelf();
            }
        }).start();*/

        return Service.START_STICKY;
    }


    @Override
    public IBinder onBind(Intent arg0) {
        Log.i(TAG, "Service onBind");
     //   Toast.makeText(getBaseContext(), "Service onBind",Toast.LENGTH_SHORT ).show();
        return null;
    }

    @Override
    public void onDestroy() {

        isRunning = false;
     //   Toast.makeText(getBaseContext(), "Service onDestroy",Toast.LENGTH_SHORT ).show();
        Log.i(TAG, "Service onDestroy");
    }
}
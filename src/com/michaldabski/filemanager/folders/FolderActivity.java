/*******************************************************************************
 * Copyright (c) 2014 Michal Dabski
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ******************************************************************************/
package com.michaldabski.filemanager.folders;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;


import com.michaldabski.filemanager.FileManagerApplication;
import com.michaldabski.filemanager.R;
import com.michaldabski.filemanager.about.AboutActivity;
import com.michaldabski.filemanager.clipboard.Clipboard;
import com.michaldabski.filemanager.clipboard.Clipboard.ClipboardListener;
import com.michaldabski.filemanager.clipboard.ClipboardFileAdapter;
import com.michaldabski.filemanager.favourites.FavouriteFolder;
import com.michaldabski.filemanager.favourites.FavouritesManager;
import com.michaldabski.filemanager.favourites.FavouritesManager.FavouritesListener;
import com.michaldabski.filemanager.nav_drawer.NavDrawerAdapter;
import com.michaldabski.filemanager.nav_drawer.NavDrawerAdapter.NavDrawerItem;
import com.michaldabski.filemanager.sqlite.SQLiteHelper;
import com.michaldabski.utils.FileUtils;
import com.michaldabski.utils.FontApplicator;
import com.michaldabski.utils.ListViewUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import  com.michaldabski.utils.UsbMonitor;
import  com.michaldabski.utils.my_Service;

public class FolderActivity extends Activity implements OnItemClickListener, ClipboardListener, FavouritesListener, UsbMonitor.Listener
{

    @Override
    public void deviceAdded(UsbDevice device) {
        Toast.makeText(getBaseContext(),getResources().getString(R.string.branche)+" "+device.getProductName().toString(),Toast.LENGTH_SHORT ).show();
    }

    @Override
    public void deviceRemoved(UsbDevice device) {
		//Toast.makeText(getBaseContext(),usb_path,Toast.LENGTH_SHORT ).show();
        Toast.makeText(getBaseContext(),getResources().getString(R.string.debranche)+" "+device.getProductName().toString(),Toast.LENGTH_SHORT ).show();
        FileManagerApplication application = (FileManagerApplication) getApplication();
        FavouritesManager favouritesManager = application.getFavouritesManager();
		favouritesManager.removeFavourite_olde_file();
		this.lastFolder=new File(Environment.getExternalStorageDirectory().getAbsolutePath());
		this.finish();
    }

    public static class FolderNotOpenException extends Exception
	{
		
	}
	
	private static final String LOG_TAG = "Main Activity";

	public static final String EXTRA_DIR = FolderFragment.EXTRA_DIR;
	
	DrawerLayout drawerLayout;
	ActionBarDrawerToggle actionBarDrawerToggle;
	File lastFolder=null;
	private FontApplicator fontApplicator;

    private UsbMonitor mUsbMonitor;
	private static final int EXTERNAL_STORAGE_PERMISSION_CONSTANT = 100;
	private static final int REQUEST_PERMISSION_SETTING = 101;
	private boolean sentToSettings = false;
	private SharedPreferences permissionStatus;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
	/*	if (android.os.Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
			requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
		}
/*		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED){
			Toast.makeText(getBaseContext(), "checkSelfPermission", Toast.LENGTH_SHORT).show();
			if (ActivityCompat.shouldShowRequestPermissionRationale(this,
					Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
				Toast.makeText(getBaseContext(), "shouldShowRequestPermissionRationale", Toast.LENGTH_SHORT).show();
			}else
				Toast.makeText(getBaseContext(), "shouldShowRequestPermissionRationale false", Toast.LENGTH_SHORT).show();
		}else
			Toast.makeText(getBaseContext(), "checkSelfPermission false", Toast.LENGTH_SHORT).show();
*/
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Intent intent2 = new Intent(this, my_Service.class);
		startService(intent2);

		mUsbMonitor = new UsbMonitor(getApplicationContext().getApplicationContext());
        mUsbMonitor.addListener(this);

		setupDrawers();
		Clipboard.getInstance().addListener(this);
		
		fontApplicator = new FontApplicator(getApplicationContext(), "Roboto_Light.ttf").applyFont(getWindow().getDecorView());



	}

	public FontApplicator getFontApplicator()
	{
		return fontApplicator;
	}
	
	@Override
	protected void onDestroy()
	{
		Clipboard.getInstance().removeListener(this);
		FileManagerApplication application = (FileManagerApplication) getApplication();
		application.getFavouritesManager().removeFavouritesListener(this);
		super.onDestroy();
	}
    @Override
    public void onResume() {
        super.onResume();
        mUsbMonitor.resume();
    }
	public void setLastFolder(File lastFolder)
	{
		this.lastFolder = lastFolder;
	}
	
	@Override
	protected void onPause()
	{
		if (lastFolder != null)
		{
			FileManagerApplication application = (FileManagerApplication) getApplication();
			application.getAppPreferences().setStartFolder(lastFolder).saveChanges(getApplicationContext());
			Log.d(LOG_TAG, "Saved last folder "+lastFolder.toString());
		}
        mUsbMonitor.pause();
		super.onPause();
	}

    public void setActionbarVisible(boolean visible)
	{
        ActionBar actionBar = getActionBar();
        if (actionBar == null) return;
		if (visible)
		{
			actionBar.show();
            setSystemBarTranslucency(false);
		}
		else
		{
			actionBar.hide();
            setSystemBarTranslucency(true);
		}
	}

    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected void setSystemBarTranslucency(boolean translucent)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return;

        if (translucent)
        {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        else
        {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.flags &= (~WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().setAttributes(params);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }
	
	private void setupDrawers()
	{
		this.drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.open_drawer, R.string.close_drawer)
		{
			boolean actionBarShown = false;
			
			@Override
			public void onDrawerOpened(View drawerView)
			{
				super.onDrawerOpened(drawerView);
				setActionbarVisible(true);
				invalidateOptionsMenu();
			}
			
			@Override
			public void onDrawerClosed(View drawerView)
			{
				actionBarShown=false;
				super.onDrawerClosed(drawerView);
				invalidateOptionsMenu();
			}
			
			@Override
			public void onDrawerSlide(View drawerView, float slideOffset)
			{
				super.onDrawerSlide(drawerView, slideOffset);
				if (slideOffset > 0 && actionBarShown == false)
				{
					actionBarShown = true;
					setActionbarVisible(true);
				}
				else if (slideOffset <= 0) actionBarShown = false;
			}
		};
		drawerLayout.setDrawerListener(actionBarDrawerToggle);
		drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);
        drawerLayout.setFocusableInTouchMode(false);
//		drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.END);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        setupNavDrawer();
		setupClipboardDrawer();
	}

    @Override
    public void onBackPressed()
    {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START);
        else if (drawerLayout.isDrawerOpen(GravityCompat.END))
            drawerLayout.closeDrawer(GravityCompat.END);
        else
            super.onBackPressed();
    }

    void setupNavDrawer()
	{
		FileManagerApplication application = (FileManagerApplication) getApplication();
        
		// add listview header to push items below the actionbar
		ListView navListView = (ListView) findViewById(R.id.listNavigation);
		ListViewUtils.addListViewPadding(navListView, this, true);

		loadFavourites(application.getFavouritesManager());
        application.getFavouritesManager().addFavouritesListener(this);

	}
	
	void setupClipboardDrawer()
	{
		// add listview header to push items below the actionbar
		ListView clipboardListView = (ListView) findViewById(R.id.listClipboard);
		ListViewUtils.addListViewHeader(clipboardListView, this);
		onClipboardContentsChange(Clipboard.getInstance());
	}
	
	void loadFavourites(FavouritesManager favouritesManager)
	{
		ListView listNavigation = (ListView) findViewById(R.id.listNavigation);
		NavDrawerAdapter navDrawerAdapter = new NavDrawerAdapter(this, new ArrayList<NavDrawerAdapter.NavDrawerItem>(favouritesManager.getFolders()));
		navDrawerAdapter.setFontApplicator(fontApplicator);
		listNavigation.setAdapter(navDrawerAdapter);
		listNavigation.setOnItemClickListener(this);
	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);
		actionBarDrawerToggle.syncState();
		
		if (getFragmentManager().findFragmentById(R.id.fragment) == null)
		{
			FolderFragment folderFragment = new FolderFragment();
			if (getIntent().hasExtra(EXTRA_DIR))
			{
				Bundle args = new Bundle();
				args.putString(FolderFragment.EXTRA_DIR, getIntent().getStringExtra(EXTRA_DIR));
				folderFragment.setArguments(args);
			}
			
			getFragmentManager()
				.beginTransaction()
				.replace(R.id.fragment, folderFragment)
				.commit();
		}
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		actionBarDrawerToggle.onConfigurationChanged(newConfig);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (actionBarDrawerToggle.onOptionsItemSelected(item))
			return true;
		switch (item.getItemId())
		{
			case R.id.menu_about:
				startActivity(new Intent(getApplicationContext(), AboutActivity.class));
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void showFragment(Fragment fragment)
	{
		getFragmentManager()
			.beginTransaction()
			.addToBackStack(null)
			.replace(R.id.fragment, fragment)
			.commit();
	}
	
	public void goBack()
	{
		getFragmentManager().popBackStack();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

    public FolderFragment getFolderFragment()
	{
		Fragment fragment = getFragmentManager().findFragmentById(R.id.fragment);
		if (fragment instanceof FolderFragment)
			return (FolderFragment) fragment;
		else return null;
		
	}
	
	public File getCurrentFolder() throws FolderNotOpenException
	{
		FolderFragment folderFragment = getFolderFragment();
		if (folderFragment == null)
			throw new FolderNotOpenException();
		else return folderFragment.currentDir;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
	{
		switch (arg0.getId())
		{
			case R.id.listNavigation:
				NavDrawerItem item = (NavDrawerItem) arg0.getItemAtPosition(arg2);
				if (item.onClicked(this))
					drawerLayout.closeDrawers();
				break;
				
			case R.id.listClipboard:
				FolderFragment folderFragment = getFolderFragment();
				if (folderFragment != null)
				{
					// TODO: paste single file
				}
				break;
			
			default:
				break;
		}
	}
	
	public File getLastFolder()
	{
		return lastFolder;
	}

	@Override
	public void onClipboardContentsChange(Clipboard clipboard)
	{
		invalidateOptionsMenu();
		
		ListView clipboardListView = (ListView) findViewById(R.id.listClipboard);
		
		if (clipboard.isEmpty() && drawerLayout != null)
			drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.END);
		else 
		{
			drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.END);
			FileManagerApplication application = (FileManagerApplication) getApplication();
			if (clipboardListView != null)
			{
				ClipboardFileAdapter clipboardFileAdapter = new ClipboardFileAdapter(this, clipboard, application.getFileIconResolver());
				clipboardFileAdapter.setFontApplicator(fontApplicator);
				clipboardListView.setAdapter(clipboardFileAdapter);
			}
		}
	}

	@Override
	public void onFavouritesChanged(FavouritesManager favouritesManager)
	{
		loadFavourites(favouritesManager);
	}

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event)
    {
        Log.d("Key Long Press", event.toString());
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            finish();
            return true;
        }
        else return super.onKeyLongPress(keyCode, event);
    }

}

package com.example.user.myapplication.Activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.user.myapplication.Adapter.Interface.ISiteAdapterListener;
import com.example.user.myapplication.Fragment.Interface.IAddNewSiteListener;
import com.example.user.myapplication.Fragment.Interface.IOrientationListener;
import com.example.user.myapplication.Manager.DatabaseManager;
import com.example.user.myapplication.Presenter.DatabasePresenter;
import com.example.user.myapplication.Presenter.DeepLinksPresenter;
import com.example.user.myapplication.R;
import com.example.user.myapplication.View.IDatabaseView;
import com.example.user.myapplication.View.IDeepLinksView;
import com.example.user.myapplication.Fragment.NewsFragment;
import com.example.user.myapplication.Fragment.ProfileFragment;
import com.example.user.myapplication.Fragment.RssItemListFragment;
import com.example.user.myapplication.Model.User;
import com.example.user.myapplication.Utils.PermissionsHelper;
import com.example.user.myapplication.Utils.SharedPref;
import com.google.android.material.navigation.NavigationView;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import es.dmoral.toasty.Toasty;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_NOSENSOR;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR;


public class MainActivity extends AppCompatActivity  implements
        IDeepLinksView,
        IDatabaseView,
        IAddNewSiteListener,
        ISiteAdapterListener,
        IOrientationListener {

    public NavController navController;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;

    private MenuItem logout_item;
    private MenuItem about_item;
    private MenuItem add_site_item;

    PermissionsHelper permissionsHelper;

    private DatabasePresenter databasePresenter;
    private SharedPref sharedPref;
    private DatabaseManager databaseManager;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        sharedPref = new SharedPref(this);
        initializeTheme();
        initializeNavigation();

        databasePresenter = new DatabasePresenter();
        databasePresenter.attachView(this);
        permissionsHelper = PermissionsHelper.getInstance();
        databaseManager = DatabaseManager.getInstance();
        initializeDatabase();

        DeepLinksPresenter deepLinksPresenter = DeepLinksPresenter.getInstance();
        deepLinksPresenter.attachView(this);
        deepLinksPresenter.uriNavigate();
        // adb shell am start -W -a android.intent.action.VIEW -d "sdapp://by.myapp/page"

        if(!permissionsHelper.hasAllPermissions(this))
            permissionsHelper.requestAllPerms(this);

    }

    private void initializeDatabase(){
        if(databaseManager.getAuthUser() == null){
            finish();
            startActivity(new Intent(this, LoginActivity.class));
        }
    }

    private void initializeTheme(){
        if (sharedPref.loadNightModeState()){
            setTheme(R.style.darktheme);
        }
    }

    private void validNeedToSaveUser(final int fragment_id){
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        ((ProfileFragment) getCurrentFragment()).saveUser();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
                navigateTo(fragment_id);
            }
        };
        drawerLayout.closeDrawers();
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("Save user information?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    private Fragment getCurrentFragment(){
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        return navHostFragment.getChildFragmentManager().getFragments().get(0);
    }

    private void initializeNavigation(){
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        final NavigationView navigationView = findViewById(R.id.nav_view);

        NavigationUI.setupWithNavController(navigationView, navController);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                int id = menuItem.getItemId();
                Fragment fragment = getCurrentFragment();
                dispatchTouchEvent();
                switch (id) {
                    case R.id.rssItemListFragment:
                        onNewsFragment(true);
                        if(fragment instanceof ProfileFragment && ((ProfileFragment)fragment).checkNeedToUpdateUser())
                            validNeedToSaveUser(id);
                        else
                            navigateTo(id);
                        return true;
                    case R.id.logout_item:
                        onNewsFragment(false);
                        databaseManager.getAuth().signOut();
                        finish();
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        return true;
                    case R.id.settingsFragment:
                        onNewsFragment(false);
                        if(fragment instanceof ProfileFragment && ((ProfileFragment)fragment).checkNeedToUpdateUser())
                            validNeedToSaveUser(id);
                        else
                            navigateTo(id);
                        return true;
                    case R.id.homeFragment:
                        onNewsFragment(false);
                        if(fragment instanceof ProfileFragment && ((ProfileFragment) fragment).checkNeedToUpdateUser())
                            validNeedToSaveUser(id);
                        else
                            navigateTo(id);
                        return true;
                }
                return true;
            }
        });


        drawerLayout = findViewById(R.id.activity_view);

        View headerview = navigationView.getHeaderView(0);
        LinearLayout profile_click_place = headerview.findViewById(R.id.profile_click_place);
        initialToggle();
        profile_click_place.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                onNewsFragment(false);
                onProfileClick(v);
            }
        });
    }

    private void dispatchTouchEvent() {
        View v = getCurrentFocus();
        if (v instanceof EditText) {
            v.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        Fragment fragment = getCurrentFragment();
        dispatchTouchEvent();
        switch (id) {
            case R.id.about_toolbar_button:
                if(fragment instanceof ProfileFragment && ((ProfileFragment) fragment).checkNeedToUpdateUser())
                    validNeedToSaveUser(R.id.aboutFragment);
                else
                    navigateTo(R.id.aboutFragment);
                return true;
            case R.id.add_new_site_item:
                add_site_item.setVisible(false);
                if(fragment instanceof ProfileFragment && ((ProfileFragment) fragment).checkNeedToUpdateUser())
                    validNeedToSaveUser(R.id.addNewSiteFragment);
                else
                    navigateTo(R.id.addNewSiteFragment);
                return true;
            case R.id.logout_item:
                databaseManager.getAuth().signOut();
                finish();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                return true;
        }
        if (toggle.onOptionsItemSelected(item)){
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initialToggle(){
        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void onNewsFragment(Boolean isOnNews){
        if (isOnNews){
            logout_item.setVisible(false);
            about_item.setVisible(false);
            add_site_item.setVisible(true);
        } else {
            logout_item.setVisible(true);
            about_item.setVisible(true);
            add_site_item.setVisible(false);
        }
    }

    @Override
    public void disableOrientation(){
        setRequestedOrientation(SCREEN_ORIENTATION_NOSENSOR);
    }

    @Override
    public void unableOrientation(){
        setRequestedOrientation(SCREEN_ORIENTATION_SENSOR);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main_toolbar_menu, menu);
        logout_item = menu.findItem(R.id.logout_item);
        about_item = menu.findItem(R.id.about_toolbar_button);
        add_site_item = menu.findItem(R.id.add_new_site_item);
        add_site_item.setVisible(false);

        Fragment fragment = getCurrentFragment();
        if (fragment instanceof RssItemListFragment || fragment instanceof NewsFragment){
            onNewsFragment(true);
        }

        return true;
    }

    @Override
    public void onSuccessMessage(String message) {
        Toasty.success(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onErrorMessage(String message) {
        Toasty.error(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void setUserInfo(User userInfo) {
        TextView textViewFullName = findViewById(R.id.full_name_txt);
        TextView textViewEmail = findViewById(R.id.email_txt);
        if (textViewFullName != null)
            textViewFullName.setText(String.format("%s %s", userInfo.getName(), userInfo.getSurname()));
        if (textViewEmail != null)
            textViewEmail.setText(userInfo.getEmail());

        File imgFile = new File(userInfo.getImg_path());

        if (imgFile.exists()) {
            Bitmap iconBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            ImageView profileImgView = findViewById(R.id.profileImgView);
            if (profileImgView != null)
                profileImgView.setImageBitmap(iconBitmap);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        databasePresenter.loadUserInformationMenu();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsHelper.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    public void onProfileClick(View view) {
        navController.popBackStack();
        navController.navigate(R.id.profileFragment);
        drawerLayout.closeDrawers();
    }

    @Override
    public Uri getUri() {
        return getIntent().getData();
    }

    @Override
    public void navigateTo(int fragment_id) {
        navController.navigate(fragment_id);
        drawerLayout.closeDrawers();
    }

    @Override
    public void onSubmitSiteClick(){
        navController.navigate(R.id.rssItemListFragment);
        add_site_item.setVisible(true);
        dispatchTouchEvent();
    }

    @Override
    public void onRssItemClick(){
        navController.navigate(R.id.newsFragment);
        add_site_item.setVisible(true);
    }

    @Override
    public void putExtra(String tag, String message){
        Intent intent = getIntent();
        intent.putExtra(tag, message);
    }

    //<editor-fold desc="Empty implement methods">
    @Override
    public ProgressDialog getProgressDialog() {
        return null;
    }
    @Override
    public void setProfileImg(Bitmap bmp) {}

    @Override
    public User getUserInfo() {
        return null;
    }

    @Override
    public void validUserName(String message) {

    }

    @Override
    public void validUserSurname(String message) {

    }

    @Override
    public void validUserPhone(String message) {

    }
    //</editor-fold">
}

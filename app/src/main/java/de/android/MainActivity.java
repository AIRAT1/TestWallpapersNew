package de.android;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import de.android.drawer.MenuItemCallback;
import de.android.drawer.NavItem;
import de.android.drawer.SimpleMenu;
import de.android.drawer.TabAdapter;
import de.android.inherit.BackPressFragment;
import de.android.inherit.PermissionsFragment;
import de.android.util.DisableableViewPager;
import de.android.util.Helper;

public class MainActivity extends AppCompatActivity implements MenuItemCallback, ConfigParser.CallBack {
    private Toolbar mToolbar;
    private TabLayout tabLayout;
    private DisableableViewPager viewPager;
    private NavigationView navigationView;
    private TabAdapter adapter;
    private static SimpleMenu menu;
    private DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;
    private int interstitialCount = -1;
    public static String FRAGMENT_DATA = "transaction_data";
    public static String FRAGMENT_CLASS = "transation_target";
    public static boolean TABLET_LAYOUT = true;
    List<NavItem> queueItem;
    MenuItem queueMenuItem;

    @Override
    public void configLoaded(boolean facedException) {
        if (facedException || menu.getFirstMenuItem() == null){
            if (Helper.isOnlineShowDialog(MainActivity.this))
                Toast.makeText(this, de.android.R.string.invalid_configuration, Toast.LENGTH_LONG).show();
        } else {
            menuItemClicked(menu.getFirstMenuItem().getValue(), menu.getFirstMenuItem().getKey(), false);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MobileAds.initialize(this, getString(de.android.R.string.admob_app_id));
        if (useTabletMenu()){
            setContentView(de.android.R.layout.activity_main_tablet);
            Helper.setStatusBarColor(MainActivity.this,
                    ContextCompat.getColor(this, de.android.R.color.myPrimaryDarkColor));
        } else {
            setContentView(de.android.R.layout.activity_main);
        }
        mToolbar = (Toolbar) findViewById(de.android.R.id.toolbar);
        setSupportActionBar(mToolbar);
        if (!useTabletMenu())
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        else {
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        }
        if (!useTabletMenu()) {
            drawer = (DrawerLayout) findViewById(de.android.R.id.drawer);
            toggle = new ActionBarDrawerToggle(
                    this, drawer, mToolbar, de.android.R.string.drawer_open, de.android.R.string.drawer_close);
            drawer.setDrawerListener(toggle);
            toggle.syncState();
        }
        tabLayout = (TabLayout) findViewById(de.android.R.id.tabs);
        viewPager = (DisableableViewPager) findViewById(de.android.R.id.viewpager);
        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(FRAGMENT_CLASS)) {
            try {
                Class<? extends Fragment> fragmentClass = (Class<? extends Fragment>) getIntent().getExtras().getSerializable(FRAGMENT_CLASS);
                if (fragmentClass != null) {
                    String[] extra = getIntent().getExtras().getStringArray(FRAGMENT_DATA);
                    HolderActivity.startActivity(this, fragmentClass, extra);
                    finish();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //Menu items
        navigationView = (NavigationView) findViewById(de.android.R.id.nav_view);
        menu = new SimpleMenu(navigationView.getMenu(), this);
        if (Config.USE_HARDCODED_CONFIG) {
            Config.configureMenu(menu, this);
        } else if (!Config.CONFIG_URL.isEmpty() && Config.CONFIG_URL.contains("http"))
            new ConfigParser(Config.CONFIG_URL, menu, this, this).execute();
        else
            new ConfigParser("config.json", menu, this, this).execute();
        tabLayout.setupWithViewPager(viewPager);
        if (!useTabletMenu()) {
            drawer.setStatusBarBackgroundColor(
                    ContextCompat.getColor(this, de.android.R.color.myPrimaryDarkColor));
        }
        applyDrawerLocks();
        Helper.admobLoader(this, getResources(), findViewById(de.android.R.id.adView));
        Helper.updateAndroidSecurityProvider(this);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {}
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
            public void onPageSelected(int position) {
                onTabBecomesActive(position);
            }
        });
    }

    @SuppressLint({"NewApi", "WrongConstant"})
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                boolean foundfalse = false;
                int res = 0;
                for (String perms : permissions){
                    res = checkCallingOrSelfPermission(perms);
                    if (!(res == PackageManager.PERMISSION_GRANTED)){
                        foundfalse = true;
                    }
                }
                if (!foundfalse){
                    menuItemClicked(queueItem, queueMenuItem, false);
                } else {
                    Toast.makeText(MainActivity.this, getResources().getString(de.android.R.string.permissions_required), Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void menuItemClicked(List<NavItem> actions, MenuItem item, boolean requiresPurchase) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean openOnStart = prefs.getBoolean("menuOpenOnStart", false);
        if (drawer != null)
            if (openOnStart && !useTabletMenu() && adapter == null) {
                drawer.openDrawer(GravityCompat.START);
            } else {
                drawer.closeDrawer(GravityCompat.START);
            }
        if (requiresPurchase && !isPurchased()) return;
        if (!checkPermissionsHandleIfNeeded(actions, item)) return;
        if (item != null) {
            for (MenuItem menuItem : menu.getMenuItems())
                menuItem.setChecked(false);
            item.setChecked(true);
        }
        adapter = new TabAdapter(getSupportFragmentManager(), actions, this);
        viewPager.setAdapter(adapter);
        if (actions.size() == 1) {
            tabLayout.setVisibility(View.GONE);
            viewPager.setPagingEnabled(false);
        } else {
            tabLayout.setVisibility(View.VISIBLE);
            viewPager.setPagingEnabled(true);
        }
        showInterstitial(false);
        onTabBecomesActive(0);
    }

    private void onTabBecomesActive(int position){
        if (position != 0)
            showInterstitial(true);
    }

    private void showInterstitial(boolean fromPager){
        if (getResources().getString(de.android.R.string.admob_interstitial_id).length() == 0) return;
        if (SettingsFragment.getIsPurchased(this)) return;
        if (interstitialCount == (Config.INTERSTITIAL_INTERVAL - 1)) {
            final InterstitialAd mInterstitialAd = new InterstitialAd(this);
            mInterstitialAd.setAdUnitId(getResources().getString(de.android.R.string.admob_interstitial_id));
            AdRequest adRequestInter = new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build();
            mInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    mInterstitialAd.show();
                }
            });
            mInterstitialAd.loadAd(adRequestInter);
            interstitialCount = 0;
        } else {
            interstitialCount++;
        }
    }

    private boolean isPurchased(){
        String license = getResources().getString(de.android.R.string.google_play_license);
        if (!SettingsFragment.getIsPurchased(this) && !license.equals("")) {
            String[] extra = new String[] {SettingsFragment.SHOW_DIALOG};
            HolderActivity.startActivity(this, SettingsFragment.class, extra);
            return false;
        }
        return true;
    }

    private boolean checkPermissionsHandleIfNeeded(List<NavItem> tabs, MenuItem item){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        List<String> allPermissions = new ArrayList<>();
        for (NavItem tab : tabs){
            if (PermissionsFragment.class.isAssignableFrom(tab.getFragment())) {
                try {
                    allPermissions.addAll(Arrays.asList(((PermissionsFragment) tab.getFragment().newInstance()).requiredPermissions()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (allPermissions.size() > 1) {
            boolean allGranted = true;
            for (String permission : allPermissions) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
                    allGranted = false;
            }
            if (!allGranted) {
                requestPermissions(allPermissions.toArray(new String[0]), 1);
                queueItem = tabs;
                queueMenuItem = item;
                return false;
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(de.android.R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case de.android.R.id.settings:
                HolderActivity.startActivity(this, SettingsFragment.class, null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        Fragment activeFragment = null;
        if (adapter != null)
            activeFragment = adapter.getCurrentFragment();
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (activeFragment instanceof BackPressFragment){
            boolean handled = ((BackPressFragment) activeFragment).handleBackPress();
            if (!handled){
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null)
            for (Fragment frag : fragments)
                if (frag != null)
                    frag.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getWindow().getDecorView().findViewById(android.R.id.content).invalidate();
    }


    public boolean useTabletMenu(){
        return (getResources().getBoolean(de.android.R.bool.isWideTablet) && TABLET_LAYOUT);
    }


    public void applyDrawerLocks(){
        if (drawer == null) {
            if (Config.HIDE_DRAWER)
                navigationView.setVisibility(View.GONE);
            return;
        }
        if (Config.HIDE_DRAWER){
            toggle.setDrawerIndicatorEnabled(false);
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        } else {
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }



}

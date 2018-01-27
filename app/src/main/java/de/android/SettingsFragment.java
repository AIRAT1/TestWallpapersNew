package de.android;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment implements BillingProcessor.IBillingHandler {
    boolean HIDE_RATE_MY_APP = false;
    String start;
    String menu;
    BillingProcessor bp;
    Preference preferencepurchase;
    AlertDialog dialog;
    private static String PRODUCT_ID_BOUGHT = "item_1_bought";
    public static String SHOW_DIALOG = "show_dialog";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(de.android.R.xml.activity_settings);
        Preference preferencerate = findPreference("rate");
        preferencerate
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Uri uri = Uri.parse("market://details?id="
                                + getActivity().getPackageName());
                        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                        try {
                            startActivity(goToMarket);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(getActivity(),
                                    de.android.R.string.not_open_playstore,
                                    Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        return true;
                    }
                });

        preferencepurchase = findPreference("purchase");
        String license = getResources().getString(de.android.R.string.google_play_license);
        if (null != license && !license.equals("")){
            bp = new BillingProcessor(getActivity(),
                    license, this);
            bp.loadOwnedPurchasesFromGoogle();
            preferencepurchase
                    .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            bp.purchase(getActivity(), PRODUCT_ID());
                            return true;
                        }
                    });
            if (getIsPurchased(getActivity())){
                preferencepurchase.setIcon(de.android.R.drawable.ic_action_action_done);
            }
        } else {
            PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference("preferenceScreen");
            PreferenceCategory billing = (PreferenceCategory) findPreference("billing");
            preferenceScreen.removePreference(billing);
        }
        String[] extra = getArguments().getStringArray(MainActivity.FRAGMENT_DATA);
        if (null != extra && extra.length != 0 && extra[0].equals(SHOW_DIALOG)){
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setPositiveButton(de.android.R.string.settings_purchase, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    bp.purchase(getActivity(), PRODUCT_ID());
                }
            });
            builder.setNegativeButton(de.android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                }
            });
            builder.setTitle(getResources().getString(de.android.R.string.dialog_purchase_title));
            builder.setMessage(getResources().getString(de.android.R.string.dialog_purchase));
            dialog = builder.create();
            dialog.show();
        }
        if (HIDE_RATE_MY_APP){
            PreferenceCategory other = (PreferenceCategory) findPreference("other");
            Preference preference = findPreference("rate");
            other.removePreference(preference);
        }
    }

    @Override
    public void onBillingInitialized() {

    }

    @Override
    public void onProductPurchased(String productId, TransactionDetails details) {
        if (productId.equals(PRODUCT_ID())){
            setIsPurchased(true, getActivity());
            preferencepurchase.setIcon(de.android.R.drawable.ic_action_action_done);
            Toast.makeText(getActivity(), getResources().getString(de.android.R.string.settings_purchase_success), Toast.LENGTH_LONG).show();
        }
        Log.v("INFO", "Purchase purchased");
    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {
        Toast.makeText(getActivity(), getResources().getString(de.android.R.string.settings_purchase_fail), Toast.LENGTH_LONG).show();
        Log.v("INFO", "Error");
    }

    @Override
    public void onPurchaseHistoryRestored() {
        if (bp.isPurchased(PRODUCT_ID())){
            setIsPurchased(true, getActivity());
            Log.v("INFO", "Purchase actually restored");
            preferencepurchase.setIcon(de.android.R.drawable.ic_action_action_done);
            if (dialog != null) dialog.cancel();
            Toast.makeText(getActivity(), getResources().getString(de.android.R.string.settings_restore_purchase_success), Toast.LENGTH_LONG).show();
        }
        Log.v("INFO", "Purchase restored called");
    }

    public void setIsPurchased(boolean purchased, Context c){
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor= prefs.edit();
        editor.putBoolean(PRODUCT_ID_BOUGHT, purchased);
        editor.apply();
    }

    public static boolean getIsPurchased(Context c){
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(c);
        boolean prefson = prefs.getBoolean(PRODUCT_ID_BOUGHT, false);
        return prefson;
    }

    private String PRODUCT_ID(){
        return getResources().getString(de.android.R.string.product_id);
    }


    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        bp.handleActivityResult(requestCode, resultCode, intent);
    }


    @Override
    public void onDestroy() {
        if (bp != null)
            bp.release();
        super.onDestroy();
    }
}

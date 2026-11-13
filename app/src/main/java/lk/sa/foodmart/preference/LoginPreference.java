package lk.sa.foodmart.preference;

import android.content.Context;
import android.content.SharedPreferences;

public class LoginPreference {
    private static final String PREF_NAME = "login_pref";
    private static final String KEY_SAVED_EMAIL ="saved_email";

    private final SharedPreferences preferences;
    private final SharedPreferences.Editor editor;

    public LoginPreference(Context context){
        preferences = context.getSharedPreferences(PREF_NAME,Context.MODE_PRIVATE);
        editor = preferences.edit();
    }

    public void saveEmail(String email){
        editor.putString(KEY_SAVED_EMAIL,email);
        editor.apply();
    }

    public String getSavedEmail(){
        return preferences.getString(KEY_SAVED_EMAIL,"");
    }

    public void clearSavedEmail(){
        editor.remove(KEY_SAVED_EMAIL);
        editor.apply();
    }
}

package com.example.walker;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    /**
     * in the password certify precess, I should send hashed password to server to check if user have
     * the right password. But in this case, we check them locally
     */
    private static String defaultPassword="202cb962ac59075b964b07152d234b70";
    private static String defaultUsername="123";
    static ArrayList<String> available_users=new ArrayList<>();
    UserLoginAuthentication userLoginAuthentication;

    AutoCompleteTextView autoCompleteTextView_username;
    EditText editText_userName;
    Button button_signIn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initView();
    }

    public void initView(){
        autoCompleteTextView_username = findViewById(R.id.autoCompleteTextView_username);
        editText_userName = findViewById(R.id.editText_userName);
        button_signIn = findViewById(R.id.button_signIn);

        button_signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userLoginAuthentication= new UserLoginAuthentication(autoCompleteTextView_username.getText().toString(),editText_userName.getText().toString());
                userLoginAuthentication.execute();
            }
        });
        available_users.add(defaultUsername);
        addEmailsToAutoComplete(available_users);
    }

    public class UserLoginAuthentication extends AsyncTask<Void,Void,Boolean>{

        private String username;
        private String password;

        UserLoginAuthentication(String username,String password){
            this.password=password;
            this.username=username;
        }
        @Override
        protected Boolean doInBackground(Void... voids) {
            //it should be some internet operation, but here we check them locally.
            //.....
            if (username.equals(defaultUsername) && comparePassword(password,defaultPassword)){
                return true;
            }else{
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if (aBoolean){
                Toast toast= Toast.makeText(getApplicationContext(), "Sign in successful",
                        Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                Intent intent =new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
            }else{
                Toast toast= Toast.makeText(getApplicationContext(), "Invalid username or password",
                        Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        }
    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        autoCompleteTextView_username.setAdapter(adapter);
    }

    public boolean comparePassword(String password, String md5_password){
//        Log.e("Walker",md5(password));
        return md5_password.equals(md5(password));
    }

    private static String md5(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result.append(temp);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

}

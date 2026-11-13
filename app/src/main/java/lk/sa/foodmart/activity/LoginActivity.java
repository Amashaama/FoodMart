package lk.sa.foodmart.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import lk.sa.foodmart.R;
import lk.sa.foodmart.databinding.ActivityLoginBinding;
import lk.sa.foodmart.preference.LoginPreference;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    private FirebaseAuth firebaseAuth;
    private LoginPreference loginPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding =ActivityLoginBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        firebaseAuth =FirebaseAuth.getInstance();
        loginPreference = new LoginPreference(this);


        String savedEmail = loginPreference.getSavedEmail();
        if(!savedEmail.isEmpty()){
            binding.loginEmail.setText(savedEmail);
        }

        binding.loginRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                startActivity(intent);
                finish();
            }
        });

        binding.btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = binding.loginEmail.getText().toString().trim();
                String password = binding.loginPassword.getText().toString().trim();

                if(email.isEmpty()){
                    binding.loginEmail.setError("Email is Required");
                    binding.loginEmail.requestFocus();
                    return;
                }

                if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    binding.loginEmail.setError("Enter valid Email");
                    binding.loginEmail.requestFocus();
                    return;
                }

                if(password.isEmpty()){
                    binding.loginPassword.setError("Password Required");
                    binding.loginPassword.requestFocus();
                    return;
                }

                if(password.length() <6){
                    binding.loginPassword.setError("Password must be 6 characters long");
                    binding.loginPassword.requestFocus();
                    return;
                }

                firebaseAuth.signInWithEmailAndPassword(email,password)
                        .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if(task.isSuccessful()){
                                    loginPreference.saveEmail(email);
                                    updateUI(firebaseAuth.getCurrentUser());
                                }else{
                                    Toast.makeText(LoginActivity.this,"Authentication Failed",Toast.LENGTH_SHORT).show();
                                }
                            }
                        });


            }
        });


    }

    private void updateUI(FirebaseUser user){
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
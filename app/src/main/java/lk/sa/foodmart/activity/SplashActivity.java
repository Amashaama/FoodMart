package lk.sa.foodmart.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.bumptech.glide.Glide;
import com.squareup.picasso.Picasso;

import lk.sa.foodmart.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){

            getWindow().setDecorFitsSystemWindows(false);

          WindowInsetsController controller = getWindow().getInsetsController();

          if(controller != null){
              controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
          }

        }else{

            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            );

        }

        setContentView(R.layout.activity_splash);

        ImageView imageView = findViewById(R.id.ivLogo);



        Glide.with(this)
                .asBitmap()
                .load(R.drawable.food_mart_logo)
                .override(800)
                .into(imageView);


        new Handler(Looper.getMainLooper())
                .postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                    }
                },1000);

        new Handler(Looper.getMainLooper())
                .postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
                        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                },5000);


    }


}
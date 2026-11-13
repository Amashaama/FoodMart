package lk.sa.foodmart.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.sa.foodmart.R;
import lk.sa.foodmart.databinding.ActivityMainBinding;
import lk.sa.foodmart.databinding.SideNavHeaderBinding;
import lk.sa.foodmart.fragment.CartFragment;
import lk.sa.foodmart.fragment.HomeFragment;
import lk.sa.foodmart.fragment.MyOrdersFragment;
import lk.sa.foodmart.fragment.MyProfileFragment;
import lk.sa.foodmart.fragment.SearchFragment;
import lk.sa.foodmart.fragment.SellerAddItemFragment;
import lk.sa.foodmart.fragment.SellerMyShopFragment;
import lk.sa.foodmart.fragment.SellerOrdersFragment;
import lk.sa.foodmart.model.User;


import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;
import lk.sa.foodmart.worker.SellerOrderReminderWorker;

public class MainActivity extends AppCompatActivity
    implements NavigationView.OnNavigationItemSelectedListener,
        NavigationBarView.OnItemSelectedListener {

  private ActivityMainBinding binding;
  private DrawerLayout drawerLayout;
  private MaterialToolbar toolbar;

  private NavigationView navigationView;

  private BottomNavigationView bottomNavigationView;

  private SideNavHeaderBinding sideNavHeaderBinding;

  private FirebaseAuth firebaseAuth;
  private FirebaseFirestore firebaseFirestore;

  private boolean isProgrammaticSelection = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    EdgeToEdge.enable(this);

    super.onCreate(savedInstanceState);
    binding = ActivityMainBinding.inflate(getLayoutInflater());

    View headerView = binding.sideNavigationView.getHeaderView(0);
    sideNavHeaderBinding = SideNavHeaderBinding.bind(headerView);
    firebaseAuth = FirebaseAuth.getInstance();
    firebaseFirestore = FirebaseFirestore.getInstance();


    drawerLayout = binding.drawerLayout;
    toolbar = binding.toolbar;
    navigationView = binding.sideNavigationView;
    bottomNavigationView = binding.bottomNavigationView;

    ViewCompat.setOnApplyWindowInsetsListener(
        binding.drawerLayout,
        (v, insets) -> {
          Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
          binding.appBar.setPadding(0, systemBars.top, 0, 0);
          binding.sideNavigationView.setPadding(0, systemBars.top, 0, 0);
          return insets;
        });


    setSupportActionBar(toolbar);
    ActionBarDrawerToggle toggle =
        new ActionBarDrawerToggle(
            MainActivity.this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
    drawerLayout.addDrawerListener(toggle);
    toggle.syncState();

    getOnBackPressedDispatcher()
        .addCallback(
            this,
            new OnBackPressedCallback(true) {
              @Override
              public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                  drawerLayout.closeDrawer(GravityCompat.START);
                } else if (bottomNavigationView.getSelectedItemId() != R.id.bottom_nav_home) {
                    navigateToFragment(new HomeFragment(), R.id.bottom_nav_home, R.id.side_nav_home, false);
                    bottomNavigationView.setSelectedItemId(R.id.bottom_nav_home);
                } else {
                  finish();
                }
              }
            });

    navigationView.setNavigationItemSelectedListener(this);
    bottomNavigationView.setOnItemSelectedListener(this);

    loadProfileDetailsOnSideNav();
    countCartItems();

    binding.toolBarBtnSearch.setOnClickListener(
        v -> {
          navigateToFragment(new SearchFragment(),R.id.bottom_nav_search,-1,false);
        });

    binding.toolBarBtnCart.setOnClickListener(
        v -> {
          navigateToFragment(new CartFragment(),-1,R.id.side_nav_cart,false);
        });

      setContentView(binding.getRoot());

    navigateToFragment(new HomeFragment(),R.id.bottom_nav_home,R.id.side_nav_home,false);

    handleNotificationIntent(getIntent());

    scheduleSellerOrderReminder();

  }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    private void handleNotificationIntent(Intent intent){
        if (intent != null) {
            intent.hasExtra("open_fragment");
        }
        assert intent != null;
        String fragmentName = intent.getStringExtra("open_fragment");
      if("my_orders".equals(fragmentName)){
          navigateToFragment(new MyOrdersFragment(),-1,R.id.side_nav_my_orders,true);
      }
  }

    public void countCartItems() {
      if(firebaseAuth.getCurrentUser() == null){
          binding.toolBarBtnCart.setVisibility(View.GONE);
          binding.toolBarTvCartBadge.setVisibility(View.GONE);
          return;
      }

      String userId = firebaseAuth.getCurrentUser().getUid();

      firebaseFirestore.collection("carts")
              .document(userId)
              .collection("items")
              .get()
              .addOnSuccessListener(queryDocumentSnapshots -> {
                  if (firebaseAuth.getCurrentUser() == null) {
                      binding.toolBarBtnCart.setVisibility(View.GONE);
                      binding.toolBarTvCartBadge.setVisibility(View.GONE);
                      return;
                  }
                  int count = queryDocumentSnapshots.size();

                  binding.toolBarBtnCart.setVisibility(View.VISIBLE);
                  if (count > 0) {
                      binding.toolBarTvCartBadge.setVisibility(View.VISIBLE);
                      binding.toolBarTvCartBadge.setText(String.valueOf(count));
                  } else {
                      binding.toolBarTvCartBadge.setVisibility(View.GONE);
                  }
              }).addOnFailureListener(e->{
                  binding.toolBarBtnCart.setVisibility(View.GONE);
                  binding.toolBarTvCartBadge.setVisibility(View.GONE);
              });
    }

    public void loadProfileDetailsOnSideNav() {
    FirebaseUser currentUser = firebaseAuth.getCurrentUser();
    boolean isLoggedIn = (currentUser != null);

    // Toggle Side Nav Menu Visibilities
    Menu sideMenu = navigationView.getMenu();
    sideMenu.findItem(R.id.side_nav_login).setVisible(!isLoggedIn);
    sideMenu.findItem(R.id.side_nav_logout).setVisible(isLoggedIn);
    sideMenu.findItem(R.id.side_nav_my_orders).setVisible(isLoggedIn);
    sideMenu.findItem(R.id.side_nav_cart).setVisible(isLoggedIn);
    sideMenu.findItem(R.id.side_nav_seller).setVisible(isLoggedIn);
    sideMenu.findItem(R.id.side_nav_account).setVisible(isLoggedIn);

    // Toggle Bottom Nav Menu Visibilities
    Menu bottomMenu = bottomNavigationView.getMenu();
    bottomMenu.findItem(R.id.bottom_nav_my_shop).setVisible(isLoggedIn);
    bottomMenu.findItem(R.id.bottom_nav_profile).setVisible(isLoggedIn);

    if (isLoggedIn) {
      firebaseFirestore
          .collection("users")
          .document(currentUser.getUid())
          .get()
          .addOnSuccessListener(
              documentSnapshot -> {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                  sideNavHeaderBinding.sideNavHeaderUserName.setText(user.getName());
                  sideNavHeaderBinding.sideNavHeaderUserEmail.setText(user.getEmail());
                  Glide.with(MainActivity.this)
                      .load(user.getProfilePicUrl())
                      .circleCrop()
                      .into(sideNavHeaderBinding.sideNavHeaderProfilePic);
                }
              })
          .addOnFailureListener(e -> Log.e("Firestore", "error " + e.getMessage()));
    } else {
      sideNavHeaderBinding.sideNavHeaderUserName.setText("Welcome");
      sideNavHeaderBinding.sideNavHeaderUserEmail.setText("Log In for better access..");
      sideNavHeaderBinding.sideNavHeaderProfilePic.setImageResource(R.drawable.baseline_person_outline_24);
      sideNavHeaderBinding.sideNavHeaderProfilePic.setBackground(null);

      sideNavHeaderBinding.userVerifiedBadge.setVisibility(View.GONE);

      binding.toolBarBtnCart.setVisibility(View.GONE);
      binding.toolBarTvCartBadge.setVisibility(View.GONE);
    }
  }

  @Override
  public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
    if (isProgrammaticSelection) {
      return true;
    }

    int itemId = menuItem.getItemId();

    if (itemId == R.id.side_nav_home || itemId == R.id.bottom_nav_home) {
        navigateToFragment(new HomeFragment(), R.id.bottom_nav_home, R.id.side_nav_home, false);
    } else if (itemId == R.id.side_nav_seller_my_shop || itemId == R.id.bottom_nav_my_shop) {
        navigateToFragment(new SellerMyShopFragment(), R.id.bottom_nav_my_shop, R.id.side_nav_seller_my_shop, false);
    } else if (itemId == R.id.side_nav_my_orders || itemId == R.id.bottom_nav_my_orders) {
        navigateToFragment(new MyOrdersFragment(),-1, R.id.side_nav_my_orders, false);
    } else if (itemId == R.id.side_nav_profile || itemId == R.id.bottom_nav_profile) {
        navigateToFragment(new MyProfileFragment(), R.id.bottom_nav_profile, R.id.side_nav_profile, false);
    } else if (itemId == R.id.bottom_nav_search) {
        navigateToFragment(new SearchFragment(), R.id.bottom_nav_search, -1, false);
    }  else if (itemId == R.id.side_nav_cart) {
        navigateToFragment(new CartFragment(), -1, R.id.side_nav_cart, false);
    } else if (itemId == R.id.side_nav_seller_add_item) {
        navigateToFragment(new SellerAddItemFragment(), -1, R.id.side_nav_seller_add_item, true);
    } else if (itemId == R.id.side_nav_seller_orders) {
        navigateToFragment(new SellerOrdersFragment(), -1, R.id.side_nav_seller_orders, true);
    }else if (itemId == R.id.side_nav_login) {
      startActivity(new Intent(MainActivity.this, LoginActivity.class));
    } else if (itemId == R.id.side_nav_logout) {
      firebaseAuth.signOut();
      loadProfileDetailsOnSideNav();
      countCartItems();
        navigateToFragment(new HomeFragment(), R.id.bottom_nav_home, R.id.side_nav_home, false);
      isProgrammaticSelection = true;
      bottomNavigationView.setSelectedItemId(R.id.bottom_nav_home);
      isProgrammaticSelection = false;
      Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }

    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
      drawerLayout.closeDrawer(GravityCompat.START);
    }

    return true;
  }

  public void openSearchFragmentWithCategory(String categoryId, String categoryName) {
    SearchFragment searchFragment = new SearchFragment();
    Bundle bundle = new Bundle();
    bundle.putString("categoryId", categoryId);
    bundle.putString("categoryName", categoryName);

    searchFragment.setArguments(bundle);

  navigateToFragment(searchFragment,R.id.bottom_nav_search,-1,false);

    isProgrammaticSelection = true;
    bottomNavigationView.setSelectedItemId(R.id.bottom_nav_search);
    isProgrammaticSelection = false;
  }

  public void navigateToFragment(Fragment fragment,int bottomNavItemId, int sideNavItemId,boolean addToBackStack){
      FragmentTransaction transaction = getSupportFragmentManager()
              .beginTransaction()
              .replace(R.id.fragment_container,fragment);

      if(addToBackStack){
          transaction.addToBackStack(null);
      }

      transaction.commit();

      clearAllNavigationChecks();

      if(bottomNavItemId != -1){
          MenuItem bottomItem = bottomNavigationView.getMenu().findItem(bottomNavItemId);
          if(bottomItem != null){
              bottomItem.setChecked(true);
          }
      }

      if(sideNavItemId != -1){
          MenuItem sideItem = navigationView.getMenu().findItem(sideNavItemId);
          if(sideItem != null){
              sideItem.setChecked(true);
          }
      }
  }

    private void clearAllNavigationChecks() {
      Menu navMenu = navigationView.getMenu();
      Menu bottomNavMenu = bottomNavigationView.getMenu();

      for(int i=0; i< navMenu.size();i++){
          navMenu.getItem(i).setChecked(false);
      }

      for(int i= 0; i< bottomNavMenu.size();i++){
          bottomNavMenu.getItem(i).setChecked(false);
      }
    }


    private void scheduleSellerOrderReminder() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            Data inputData = new Data.Builder()
                    .putString("sellerId", currentUser.getUid())
                    .build();

            PeriodicWorkRequest reminderRequest =
                    new PeriodicWorkRequest.Builder(SellerOrderReminderWorker.class, 15, TimeUnit.MINUTES)
                            .setInputData(inputData)
                            .build();

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "SellerOrderReminder",
                    ExistingPeriodicWorkPolicy.KEEP,
                    reminderRequest
            );
        }
    }

    private void cancelSellerOrderReminder() {
        WorkManager.getInstance(this).cancelUniqueWork("SellerOrderReminder");
    }

    @Override
    protected void onResume() {
        super.onResume();
        countCartItems();
        loadProfileDetailsOnSideNav();
    }
}

package lk.sa.foodmart.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;

import lk.sa.foodmart.R;
import lk.sa.foodmart.activity.MainActivity;
import lk.sa.foodmart.adapter.SellerMyShopAdapter;
import lk.sa.foodmart.databinding.FragmentSellerMyShopBinding;
import lk.sa.foodmart.model.FoodItem;


public class SellerMyShopFragment extends Fragment {


    private FragmentSellerMyShopBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private final ArrayList<FoodItem> myItems = new ArrayList<>();
    private SellerMyShopAdapter adapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding =FragmentSellerMyShopBinding.inflate(inflater,container,false);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        setupRecyclerView();
        setupClicks();
        loadMyItems();



        // Inflate the layout for this fragment
        return binding.getRoot();
    }

    private void setupRecyclerView() {
    binding.rvMyItems.setLayoutManager(new GridLayoutManager(requireContext(),2));

    adapter =
        new SellerMyShopAdapter(
            myItems,
            item -> {
              SellerAddItemFragment fragment = new SellerAddItemFragment();
              Bundle bundle = new Bundle();
              bundle.putBoolean("isEdit", true);
              bundle.putString("itemId", item.getItemId());
              fragment.setArguments(bundle);

              if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity())
                    .navigateToFragment(
                     fragment,
                        -1,
                        -1,
                        false);
              }
            });
    binding.rvMyItems.setAdapter(adapter);
    }

    private void setupClicks() {
        binding.btnBack.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity())
                        .navigateToFragment(new HomeFragment(), R.id.bottom_nav_home, R.id.side_nav_home, false);
            }
        });

        binding.btnAddItem.setOnClickListener(v -> {
            SellerAddItemFragment fragment = new SellerAddItemFragment();
            Bundle bundle = new Bundle();
            bundle.putBoolean("isEdit",false);
            fragment.setArguments(bundle);

            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity())
                        .navigateToFragment(
                                new SellerAddItemFragment(),
                                -1,
                                -1,
                                false);
            }


        });
    }

    private void loadMyItems() {
        if (binding == null) return;

        if(auth.getCurrentUser() == null){
            showEmptyState();
            return;
        }

        binding.progressLoading.setVisibility(View.VISIBLE);
        binding.layoutEmpty.setVisibility(View.GONE);
        String sellerId = auth.getCurrentUser().getUid();

        firestore.collection("food_items")
                .whereEqualTo("sellerId",sellerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (binding == null) return;
                    myItems.clear();

                    for(QueryDocumentSnapshot document: queryDocumentSnapshots){
                        FoodItem item = document.toObject(FoodItem.class);
                        myItems.add(item);
                    }

                    Collections.sort(myItems,
                            (a,b)-> Long.compare(b.getCreatedAt(),a.getCreatedAt()));

                    adapter.notifyDataSetChanged();
                    binding.progressLoading.setVisibility(View.GONE);

                    if(myItems.isEmpty()){
                        showEmptyState();
                    }else{
                        binding.layoutEmpty.setVisibility(View.GONE);
                        binding.rvMyItems.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    binding.progressLoading.setVisibility(View.GONE);

                    if (getContext() != null) {
                        Toast.makeText(requireContext(), "Failed to load shop items", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showEmptyState(){
        if (binding == null) return;
        binding.layoutEmpty.setVisibility(View.VISIBLE);
        binding.rvMyItems.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
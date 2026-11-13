package lk.sa.foodmart.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.StatusResponse;
import lk.sa.foodmart.BuildConfig;
import lk.sa.foodmart.R;
import lk.sa.foodmart.activity.MainActivity;
import lk.sa.foodmart.databinding.FragmentCheckoutBinding;
import lk.sa.foodmart.model.CartItem;
import lk.sa.foodmart.model.User;
import lk.sa.foodmart.reciever.OrderNotificationReceiver;

public class CheckoutFragment extends Fragment {

    private FragmentCheckoutBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    private final ArrayList<CartItem> cartItems = new ArrayList<>();
    private User savedUser;

    private double subtotal = 0.0;
    private double deliveryFee = 500.0;
    private double total = 0.0;

    private static final double COLOMBO_FEE = 300.0;
    private static final double OUTSIDE_COLOMBO_FEE = 500.0;

    private static final String CHANNEL_ID ="order_channel";
    private static final String CHANNEL_NAME ="Order Notifications";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCheckoutBinding.inflate(inflater, container, false);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();


        requestNotificationPermissionIfNeeded();

        setupClicks();
        setupListeners();
        loadUserDetails();
        loadCartSummary();
        updateSummaryUI();

        return binding.getRoot();
    }

    private void setupClicks() {
        binding.btnBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        binding.btnPlaceOrder.setOnClickListener(v -> {
            if (auth.getCurrentUser() == null) {
                Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!validateInputs()) return;

            recalculateDeliveryFee();
            startPayHerePayment();
        });

        binding.layoutDeliveryHeader.setOnClickListener(v -> {
            if (binding.layoutDeliveryBody.getVisibility() == View.VISIBLE) {
                binding.layoutDeliveryBody.setVisibility(View.GONE);
                binding.ivDeliveryArrow.setRotation(0f);
            } else {
                binding.layoutDeliveryBody.setVisibility(View.VISIBLE);
                binding.ivDeliveryArrow.setRotation(180f);
            }
        });
    }

    private void setupListeners() {
        binding.cbUseSavedDetails.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                fillFieldsWithSavedDetails();
            }
            recalculateDeliveryFee();
        });

        binding.etCity.addTextChangedListener(simpleWatcher);
        binding.etAddressLine1.addTextChangedListener(simpleWatcher);
        binding.etAddressLine2.addTextChangedListener(simpleWatcher);
    }

    private final TextWatcher simpleWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            recalculateDeliveryFee();
        }

        @Override
        public void afterTextChanged(Editable s) { }
    };

    private void loadUserDetails() {
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    savedUser = documentSnapshot.toObject(User.class);

                    if (savedUser != null && binding.cbUseSavedDetails.isChecked()) {
                        fillFieldsWithSavedDetails();
                    }

                    recalculateDeliveryFee();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed to load user details", Toast.LENGTH_SHORT).show()
                );
    }

    private void fillFieldsWithSavedDetails() {
        if (savedUser == null) return;

        binding.etFullName.setText(savedUser.getName() != null ? savedUser.getName() : "");
        binding.etEmail.setText(savedUser.getEmail() != null ? savedUser.getEmail() : "");
        binding.etPhone.setText(savedUser.getPhone() != null ? savedUser.getPhone() : "");
        binding.etAddressLine1.setText(savedUser.getAddress() != null ? savedUser.getAddress() : "");
        binding.etCity.setText(savedUser.getCity() != null ? savedUser.getCity() : "");
    }

    private void loadCartSummary() {
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        firestore.collection("carts")
                .document(uid)
                .collection("items")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    cartItems.clear();
                    subtotal = 0.0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        CartItem item = document.toObject(CartItem.class);
                        if (item != null) {
                            cartItems.add(item);
                            subtotal += item.getItemPrice() * item.getQuantity();
                        }
                    }

                    updateSummaryUI();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed to load cart", Toast.LENGTH_SHORT).show()
                );
    }

    private void recalculateDeliveryFee() {
        String enteredCity = getText(binding.etCity).trim();
        String address1 = getText(binding.etAddressLine1).trim();
        String address2 = getText(binding.etAddressLine2).trim();

        String finalCity = enteredCity;

        if (TextUtils.isEmpty(finalCity) && savedUser != null && savedUser.getCity() != null) {
            finalCity = savedUser.getCity().trim();
        }

        String combinedAddress = (address1 + " " + address2 + " " + finalCity)
                .toLowerCase(Locale.getDefault());

        if (!TextUtils.isEmpty(finalCity) && finalCity.equalsIgnoreCase("Colombo")) {
            deliveryFee = COLOMBO_FEE;
        } else if (combinedAddress.contains("colombo")) {
            deliveryFee = COLOMBO_FEE;
        } else {
            deliveryFee = OUTSIDE_COLOMBO_FEE;
        }

        updateSummaryUI();
    }

    private void updateSummaryUI() {
        total = subtotal + deliveryFee;

        binding.tvItemsTotal.setText(String.format(Locale.getDefault(), "LKR %.2f", subtotal));
        binding.tvSubtotal.setText(String.format(Locale.getDefault(), "LKR %.2f", subtotal));
        binding.tvDeliveryFee.setText(String.format(Locale.getDefault(), "LKR %.2f", deliveryFee));
        binding.tvTotal.setText(String.format(Locale.getDefault(), "LKR %.2f", total));
        binding.tvPaymentLabel.setText("Pay via: Card Payment");
    }

    private boolean validateInputs() {
        boolean valid = true;

        if (TextUtils.isEmpty(getText(binding.etFullName))) {
            binding.tilFullName.setError("Full name is required");
            valid = false;
        } else {
            binding.tilFullName.setError(null);
        }

        if (TextUtils.isEmpty(getText(binding.etEmail))) {
            binding.tilEmail.setError("Email is required");
            valid = false;
        } else {
            binding.tilEmail.setError(null);
        }

        if (TextUtils.isEmpty(getText(binding.etPhone))) {
            binding.tilPhone.setError("Phone Number is required");
            valid = false;
        } else {
            binding.tilPhone.setError(null);
        }

        if (TextUtils.isEmpty(getText(binding.etAddressLine1))) {
            binding.tilAddressLine1.setError("Address is required");
            valid = false;
        } else {
            binding.tilAddressLine1.setError(null);
        }

        if (TextUtils.isEmpty(getText(binding.etCity))) {
            binding.tilCity.setError("City is required");
            valid = false;
        } else {
            binding.tilCity.setError(null);
        }

        if (TextUtils.isEmpty(getText(binding.etPostal))) {
            binding.tilPostal.setError("Postal code is required");
            valid = false;
        } else {
            binding.tilPostal.setError(null);
        }

        if (cartItems.isEmpty()) {
            Toast.makeText(requireContext(), "Cart is empty", Toast.LENGTH_SHORT).show();
            valid = false;
        }

        return valid;
    }

    private void startPayHerePayment() {
        if (total <= 0) {
            Toast.makeText(requireContext(), "Invalid total amount", Toast.LENGTH_SHORT).show();
            return;
        }

        InitRequest req = new InitRequest();

        req.setSandBox(true);
        req.setMerchantId(BuildConfig.PAYHERE_MERCHANT_ID);
        req.setMerchantSecret(BuildConfig.PAYHERE_MERCHANT_SECRET);
        req.setCurrency("LKR");
        req.setAmount(total);
        req.setOrderId("ORD-" + System.currentTimeMillis());
        req.setItemsDescription("FoodMart Order");

        String fullName = getText(binding.etFullName);
        String firstName = "Customer";
        String lastName = "User";

        if (!TextUtils.isEmpty(fullName)) {
            if (fullName.contains(" ")) {
                String[] parts = fullName.split(" ", 2);
                firstName = parts[0];
                lastName = parts[1];
            } else {
                firstName = fullName;
            }
        }

        req.getCustomer().setFirstName(firstName);
        req.getCustomer().setLastName(lastName);
        req.getCustomer().setEmail(getText(binding.etEmail));
        req.getCustomer().setPhone(formatPhoneForPayHere(getText(binding.etPhone)));
        req.getCustomer().getAddress().setAddress(getText(binding.etAddressLine1));
        req.getCustomer().getAddress().setCity(getText(binding.etCity));
        req.getCustomer().getAddress().setCountry("Sri Lanka");

        Intent intent = new Intent(requireActivity(), PHMainActivity.class);
        intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);
        payHereLauncher.launch(intent);
    }

    private String formatPhoneForPayHere(String phone) {
        phone = phone.replaceAll("\\s+", "");

        if (phone.startsWith("0") && phone.length() == 10) {
            return "+94" + phone.substring(1);
        } else if (phone.startsWith("94")) {
            return "+" + phone;
        }
        return phone;
    }

    private void placePaidOrder(String paymentReference) {
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        DocumentReference orderRef = firestore.collection("orders").document();
        String orderId = orderRef.getId();

        String deliveryType = deliveryFee == COLOMBO_FEE ? "Within Colombo" : "Outside Colombo";

        Map<String, Object> orderMap = new HashMap<>();
        orderMap.put("orderId", orderId);
        orderMap.put("userId", uid);
        orderMap.put("fullName", getText(binding.etFullName));
        orderMap.put("email", getText(binding.etEmail));
        orderMap.put("phone", getText(binding.etPhone));
        orderMap.put("addressLine1", getText(binding.etAddressLine1));
        orderMap.put("addressLine2", getText(binding.etAddressLine2));
        orderMap.put("city", getText(binding.etCity));
        orderMap.put("postalCode", getText(binding.etPostal));
        orderMap.put("deliveryType", deliveryType);
        orderMap.put("deliveryFee", deliveryFee);
        orderMap.put("paymentMethod", "Card Payment");
        orderMap.put("paymentReference", paymentReference);
        orderMap.put("subtotal", subtotal);
        orderMap.put("total", total);
        orderMap.put("status", "PAID");
        orderMap.put("createdAt", System.currentTimeMillis());

        orderRef.set(orderMap)
                .addOnSuccessListener(unused -> saveOrderItems(orderId))
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed to place order", Toast.LENGTH_SHORT).show());
    }

    private void saveOrderItems(String orderId) {
        if (cartItems.isEmpty()) {
            clearCartAfterOrder(orderId);
            return;
        }

        final int totalItems = cartItems.size();
        final int[] savedCount = {0};

        for (CartItem item : cartItems) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("itemId", item.getItemId());
            itemMap.put("sellerId", item.getSellerId());
            itemMap.put("itemName", item.getItemName());
            itemMap.put("itemPrice", item.getItemPrice());
            itemMap.put("quantity", item.getQuantity());
            itemMap.put("portionSize", item.getPortionSize());
            itemMap.put("imageUrl", item.getImageUrl());

            firestore.collection("orders")
                    .document(orderId)
                    .collection("items")
                    .document(item.getItemId())
                    .set(itemMap)
                    .addOnSuccessListener(unused -> {
                        reduceFoodItemStock(item);
                        increaseSoldCount(item);
                        savedCount[0]++;
                        if (savedCount[0] == totalItems) {
                            clearCartAfterOrder(orderId);
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), "Failed to save order items", Toast.LENGTH_SHORT).show());
        }
    }

    private void reduceFoodItemStock(CartItem cartItem){
        firestore.collection("food_items")
                .document(cartItem.getItemId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if(documentSnapshot.exists()){
                        Long stockLong = documentSnapshot.getLong("quantity");
                        int currentStock = stockLong != null ? stockLong.intValue():0;

                        int newStock = currentStock - cartItem.getQuantity();
                        if(newStock <0) newStock = 0;

                        Map<String ,Object> updateMap = new HashMap<>();
                        updateMap.put("quantity",newStock);
                        updateMap.put("available", newStock>0);

                        firestore.collection("food_items")
                                .document(cartItem.getItemId())
                                .update(updateMap);

                    }
                });
    }

    private void clearCartAfterOrder(String orderId) {
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        firestore.collection("carts")
                .document(uid)
                .collection("items")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        document.getReference().delete();
                    }

                    Toast.makeText(requireContext(), "Order placed successfully", Toast.LENGTH_LONG).show();
saveOrderSummaryLocally(orderId);
                    showOrderPlacedNotification(orderId);

                    if (requireActivity() instanceof MainActivity) {
                        ((MainActivity) requireActivity()).navigateToFragment(new HomeFragment(),R.id.bottom_nav_home,R.id.side_nav_home,false);
                    }



                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed to clear cart", Toast.LENGTH_SHORT).show());
    }

    private final ActivityResultLauncher<Intent> payHereLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();

                    if (data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
                        @SuppressWarnings("unchecked")
                        PHResponse<StatusResponse> response =
                                (PHResponse<StatusResponse>) data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);

                        if (response != null && response.isSuccess()) {
                            StatusResponse statusResponse = response.getData();

                            Toast.makeText(requireContext(), "Payment Success", Toast.LENGTH_SHORT).show();

                            String paymentReference = "";
                            if (statusResponse != null) {
                                paymentReference = statusResponse.toString();
                            }

                            placePaidOrder(paymentReference);

                        } else {
                            Toast.makeText(requireContext(), "Payment failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    Toast.makeText(requireContext(), "Payment cancelled", Toast.LENGTH_SHORT).show();
                }
            });

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription("Notification for order updates");

            NotificationManager manager = requireContext().getSystemService(NotificationManager.class);
            if(manager != null){
                manager.createNotificationChannel(channel);
            }
        }
    }

  private final ActivityResultLauncher<String> notificationPermissionLauncher =
      registerForActivityResult(
          new ActivityResultContracts.RequestPermission(),
          isGranted -> {
            if (isGranted) {
              Toast.makeText(
                      requireContext(), "Notification permission granted", Toast.LENGTH_SHORT)
                  .show();
            } else {
              Toast.makeText(requireContext(), "Notification permission denied", Toast.LENGTH_SHORT)
                  .show();
            }
          });

    private void showOrderPlacedNotification(String orderId){
        createNotificationChannel();

        Intent recieverIntent = new Intent(requireContext(), OrderNotificationReceiver.class);
        recieverIntent.putExtra("orderId",orderId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                1001,
                recieverIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        @SuppressLint("NotificationTrampoline") NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(),CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_notifications_24)
                .setContentTitle("Order Placed Successfully")
                .setContentText("Your order #"+orderId+ " has been placed")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(1001,builder.build());
    }


    private void requestNotificationPermissionIfNeeded() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if(ContextCompat.checkSelfPermission((requireContext()),Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED){
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void increaseSoldCount(CartItem cartItem){
        firestore.collection("food_items")
                .document(cartItem.getItemId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if(documentSnapshot.exists()){
                        Long soldCount =documentSnapshot.getLong("soldCount");

                        int currentSoldCount = soldCount != null ? soldCount.intValue():0;

                        int newSoldCount = currentSoldCount+ cartItem.getQuantity();

                        firestore.collection("food_items")
                                .document(cartItem.getItemId())
                                .update("soldCount",newSoldCount);
                    }
                });
    }


    private void saveOrderSummaryLocally(String orderId) {
        String content = "--- FOOD MART RECEIPT ---\n" +
                "Order ID: " + orderId + "\n" +
                "Date: " + new java.util.Date() + "\n" +
                "Total Amount: LKR " + total + "\n" +
                "Payment: Card Payment\n" +
                "Status: PAID\n" +
                "-------------------------";
        try (java.io.FileOutputStream fos = requireContext().openFileOutput("last_order_receipt.txt", android.content.Context.MODE_PRIVATE)) {
            fos.write(content.getBytes());
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

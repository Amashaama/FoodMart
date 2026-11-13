package lk.sa.foodmart.activity;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lk.sa.foodmart.R;
import lk.sa.foodmart.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final String CONFIRM_LOCATION_TEXT = "Confirm Location";

    private final ExecutorService geocoderExecutor = Executors.newFixedThreadPool(2);

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private LatLng selectedLatLang;
    private String selectedAddress = "";
    private String selectedAddressCity = "";
    private boolean addressLookupPending;
    private boolean userSelectedMapPoint;
    private int addressLookupRequestId;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private static final class AddressLookupResult {
        final String address;
        final String city;

        AddressLookupResult(String address, String city) {
            this.address = address;
            this.city = city;
        }

        static AddressLookupResult empty() {
            return new AddressLookupResult("", "");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        binding.btnConfirmLocation.setOnClickListener(
                v -> {
                    if (selectedLatLang != null) {
                        if (addressLookupPending) {
                            Toast.makeText(
                                            this,
                                            "Address is still loading. You can enter it manually after returning.",
                                            Toast.LENGTH_SHORT)
                                    .show();
                        }

                        Intent intent = new Intent();
                        intent.putExtra("latitude", selectedLatLang.latitude);
                        intent.putExtra("longitude", selectedLatLang.longitude);
                        intent.putExtra("address", selectedAddress);
                        intent.putExtra("city", selectedAddressCity);
                        setResult(RESULT_OK, intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.setOnMapLongClickListener(
                latLng -> {
                    userSelectedMapPoint = true;
                    selectLocation(latLng, "Selected Location", true);
                });

        enableCurrentLocation();
    }

    private void selectLocation(LatLng location, String markerTitle, boolean animateCamera) {
        selectedLatLang = location;
        selectedAddress = "";
        selectedAddressCity = "";
        addressLookupPending = true;
        int requestId = ++addressLookupRequestId;

        if (mMap != null) {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(location).title(markerTitle));
            if (animateCamera) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
            } else {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
            }
        }

        binding.btnConfirmLocation.setText("Finding address...");

        geocoderExecutor.execute(
                () -> {
                    AddressLookupResult result = reverseGeocode(location);
                    runOnUiThread(
                            () -> {
                                if (isFinishing()
                                        || isDestroyed()
                                        || requestId != addressLookupRequestId) {
                                    return;
                                }

                                selectedAddress = result.address;
                                selectedAddressCity = result.city;
                                addressLookupPending = false;
                                binding.btnConfirmLocation.setText(CONFIRM_LOCATION_TEXT);

                                if (selectedAddress.isEmpty()) {
                                    Toast.makeText(
                                                    this,
                                                    "No address was found. You can type it manually on the next screen.",
                                                    Toast.LENGTH_LONG)
                                            .show();
                                }
                            });
                });
    }

    private void enableCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        if (mMap != null) {
            mMap.setMyLocationEnabled(true);
            fusedLocationProviderClient
                    .getLastLocation()
                    .addOnSuccessListener(
                            this,
                            location -> {
                                if (location != null && !userSelectedMapPoint) {
                                    LatLng currentLocation =
                                            new LatLng(location.getLatitude(), location.getLongitude());
                                    selectLocation(currentLocation, "Current Location", false);
                                }
                            });
        }
    }

    private AddressLookupResult reverseGeocode(LatLng location) {
        if (!Geocoder.isPresent()) {
            return AddressLookupResult.empty();
        }

        try {
            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
            List<Address> results =
                    geocoder.getFromLocation(location.latitude, location.longitude, 5);
            Address bestResult = chooseMostDetailedAddress(results);
            if (bestResult == null) {
                return AddressLookupResult.empty();
            }

            String address = formatAddress(bestResult);
            String city =
                    firstNonEmpty(
                            bestResult.getLocality(),
                            bestResult.getSubAdminArea(),
                            bestResult.getAdminArea());
            return new AddressLookupResult(address, city);
        } catch (IOException | RuntimeException e) {
            return AddressLookupResult.empty();
        }
    }

    private Address chooseMostDetailedAddress(List<Address> results) {
        if (results == null || results.isEmpty()) {
            return null;
        }

        Address best = results.get(0);
        int bestScore = addressDetailScore(best);
        for (int i = 1; i < results.size(); i++) {
            Address candidate = results.get(i);
            int candidateScore = addressDetailScore(candidate);
            if (candidateScore > bestScore) {
                best = candidate;
                bestScore = candidateScore;
            }
        }
        return best;
    }

    private int addressDetailScore(Address address) {
        int score = 0;
        if (!isBlank(firstAddressLine(address))) score += 2;
        if (!isBlank(address.getThoroughfare())) score += 4;
        if (!isBlank(address.getSubThoroughfare())) score += 3;
        if (!isBlank(address.getSubLocality())) score += 2;
        if (!isBlank(address.getLocality())) score += 2;
        if (!isBlank(address.getPostalCode())) score += 1;
        return score;
    }

    private String formatAddress(Address address) {
        List<String> parts = new ArrayList<>();
        int lastAddressLine = address.getMaxAddressLineIndex();
        for (int i = 0; i <= lastAddressLine; i++) {
            addAddressPart(parts, address.getAddressLine(i));
        }

        if (parts.isEmpty()) {
            addAddressPart(parts, address.getSubThoroughfare());
            addAddressPart(parts, address.getThoroughfare());
            addAddressPart(parts, address.getSubLocality());
            addAddressPart(parts, address.getLocality());
            addAddressPart(parts, address.getSubAdminArea());
            addAddressPart(parts, address.getAdminArea());
            addAddressPart(parts, address.getPostalCode());
            addAddressPart(parts, address.getCountryName());
        }

        return String.join(", ", parts);
    }

    private String firstAddressLine(Address address) {
        return address.getMaxAddressLineIndex() >= 0 ? address.getAddressLine(0) : "";
    }

    private void addAddressPart(List<String> parts, String value) {
        if (isBlank(value)) {
            return;
        }

        String candidate = value.trim();
        for (String existing : parts) {
            if (existing.equalsIgnoreCase(candidate)) {
                return;
            }
        }
        parts.add(candidate);
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    protected void onDestroy() {
        geocoderExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

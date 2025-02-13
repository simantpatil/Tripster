package com.martynaroj.traveljournal.view.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.snackbar.Snackbar;
import com.martynaroj.traveljournal.R;
import com.martynaroj.traveljournal.databinding.FragmentWeatherBinding;
import com.martynaroj.traveljournal.services.others.GooglePlaces;
import com.martynaroj.traveljournal.view.base.BaseFragment;
import com.martynaroj.traveljournal.view.others.classes.RequestPermissionsHandler;
import com.martynaroj.traveljournal.view.others.interfaces.Constants;
import com.martynaroj.traveljournal.viewmodels.AddressViewModel;
import com.martynaroj.traveljournal.viewmodels.UserViewModel;
import com.martynaroj.traveljournal.viewmodels.WeatherViewModel;
import androidx.lifecycle.ViewModelProvider;


public class WeatherFragment extends BaseFragment implements View.OnClickListener {

    private FragmentWeatherBinding binding;
    private AddressViewModel addressViewModel;
    private WeatherViewModel weatherViewModel;
    private UserViewModel userViewModel;

    private FindCurrentPlaceRequest request;
    private PlacesClient placesClient;
    private Place deviceLocation;
    private AutocompleteSupportFragment autocompleteFragment;

    // FusedLocationProviderClient instance
    private FusedLocationProviderClient fusedLocationClient;

    public static WeatherFragment newInstance() {
        return new WeatherFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWeatherBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        initViewModels();
        initGooglePlaces();
        initLocation();

        setListeners();
        observeUserChanges();

        return view;
    }

    // Initialize ViewModels
    private void initViewModels() {
        if (getActivity() != null) {
            addressViewModel = new ViewModelProvider(getActivity()).get(AddressViewModel.class);
            weatherViewModel = new ViewModelProvider(getActivity()).get(WeatherViewModel.class);
            userViewModel = new ViewModelProvider(getActivity()).get(UserViewModel.class);
        }
    }

    // Initialize Google Places API
    private void initGooglePlaces() {
        if (getContext() != null) {
            GooglePlaces.init(getContext());
            placesClient = GooglePlaces.initClient(getContext());
            request = GooglePlaces.initRequest();
            autocompleteFragment = GooglePlaces.initAutoComplete(
                    getContext(),
                    R.id.weather_search_view,
                    getChildFragmentManager()
            );
        }
    }

    // Initialize FusedLocationProviderClient and request location
    private void initLocation() {
        if (RequestPermissionsHandler.isFineLocationGranted(getContext())) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());
            detectLocation();
        } else {
            RequestPermissionsHandler.requestFineLocation(this);
        }
    }

    // Observe user data changes
    private void observeUserChanges() {
        userViewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user == null)
                back();
        });
    }

    // Set listeners for buttons
    private void setListeners() {
        binding.weatherArrowButton.setOnClickListener(this);
        binding.weatherSwitchTempUnits.setOnClickListener(this);
        setAutoCompleteListeners();
    }

    // Set listeners for the Autocomplete Fragment
    private void setAutoCompleteListeners() {
        if (autocompleteFragment != null && autocompleteFragment.getView() != null) {
            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    getCurrentWeather(place.getLatLng());
                }

                @Override
                public void onError(@NonNull Status status) {
                }
            });
            autocompleteFragment.getView()
                    .findViewById(R.id.places_autocomplete_clear_button)
                    .setOnClickListener(view -> autocompleteFragment.setText(""));
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.weather_arrow_button:
                back();
                break;
            case R.id.weather_switch_temp_units:
                changeTempUnits();
                break;
        }
    }

    // Handle location detection using FusedLocationProviderClient
    private void detectLocation() {
        startProgressBar();
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                getCurrentWeather(latLng);  // Method that handles weather data based on location
                            } else {
                                showSnackBar(getResources().getString(R.string.messages_error_localize), Snackbar.LENGTH_LONG);
                                getCurrentWeather(Constants.LAT_LNG_LONDON);  // Fallback location (London)
                            }
                        }
                    });
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    // Fetch weather data based on LatLng
    private void getCurrentWeather(LatLng latLng) {
        startProgressBar();
        weatherViewModel.getWeather(latLng);
        weatherViewModel.getWeatherResultData().observe(getViewLifecycleOwner(), weatherResult -> {
            if (weatherResult != null) {
                binding.setWeatherResult(weatherResult);
            } else {
                showSnackBar(getResources().getString(R.string.messages_error_localize), Snackbar.LENGTH_LONG);
                getCurrentWeather(Constants.LAT_LNG_LONDON);
            }
            stopProgressBar();
        });

        weatherViewModel.getWeatherForecast(latLng);
        weatherViewModel.getWeatherForecastResultData().observe(getViewLifecycleOwner(), weatherForecastResult -> {
            if (weatherForecastResult != null) {
                binding.setForecastResult(weatherForecastResult);
            }
            stopProgressBar();
        });
    }

    // Change temperature units based on switch state
    private void changeTempUnits() {
        binding.setTempUnits(binding.weatherSwitchTempUnits.isChecked());
    }

    // Handle back navigation
    private void back() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0)
            getParentFragmentManager().popBackStack();
    }

    // Show progress bar
    private void startProgressBar() {
        getProgressBarInteractions().startProgressBar(binding.getRoot(),
                binding.weatherProgressbarLayout, binding.weatherProgressbar);
    }

    // Stop progress bar
    private void stopProgressBar() {
        getProgressBarInteractions().stopProgressBar(binding.getRoot(),
                binding.weatherProgressbarLayout, binding.weatherProgressbar);
    }

    // Show Snackbar
    private void showSnackBar(String message, int duration) {
        getSnackBarInteractions().showSnackBar(binding.getRoot(), getActivity(), message, duration);
    }

    // Handle permissions result for location access
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (RequestPermissionsHandler.isOnResultGranted(requestCode, grantResults)) {
            detectLocation();
        } else {
            getCurrentWeather(Constants.LAT_LNG_LONDON);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

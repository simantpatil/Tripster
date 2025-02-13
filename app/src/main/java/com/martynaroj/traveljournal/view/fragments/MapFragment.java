package com.martynaroj.traveljournal.view.fragments;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import com.martynaroj.traveljournal.databinding.FragmentMapBinding;
import com.martynaroj.traveljournal.view.base.BaseFragment;

public class MapFragment extends BaseFragment {

    private FragmentMapBinding binding;
    private MapView mapView;  // Declare MapView

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMapBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            initOSMMap();
        }
        return view;
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void initOSMMap() {
        Configuration.getInstance().setUserAgentValue(getContext().getPackageName());

        mapView = binding.mapView; // Reference MapView from the layout

        // Set up the map with a specific tile source and zoom level
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.getController().setZoom(15);

        GeoPoint startPoint = new GeoPoint(48.8588443, 2.2943506); // Example: Eiffel Tower
        mapView.getController().setCenter(startPoint); // Set map center to the starting point

        addMarker(startPoint, "Default Location");
    }

    private void addMarker(GeoPoint point, String title) {
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(title);
        mapView.getOverlays().add(marker); // Add marker to the map
        mapView.invalidate(); // Refresh the map
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

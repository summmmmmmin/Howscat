package com.example.howscat;

import android.Manifest;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;

import com.example.howscat.dto.HospitalNearbyResponse;
import com.example.howscat.network.ApiService;
import com.example.howscat.network.RetrofitClient;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HospitalFragment extends Fragment {

    private static final int REQ_LOC = 7021;
    private static final double DEFAULT_RADIUS_KM = 2.0;

    private RecyclerView recyclerHospitals;
    private ProgressBar progressHospitals;

    private final List<HospitalNearbyResponse> hospitals = new ArrayList<>();
    private HospitalAdapter adapter;
    private ExecutorService ioPool;

    public HospitalFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_hospital, container, false);

        ioPool = Executors.newSingleThreadExecutor();

        recyclerHospitals = view.findViewById(R.id.recyclerHospitals);
        progressHospitals = view.findViewById(R.id.progressHospitals);

        recyclerHospitals.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HospitalAdapter();
        recyclerHospitals.setAdapter(adapter);

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, REQ_LOC);
        }

        loadNearbyHospitals();
        return view;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOC && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadNearbyHospitals();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ioPool.shutdown();
    }

    private void loadNearbyHospitals() {
        if (!isAdded() || getContext() == null) return;

        progressHospitals.setVisibility(View.VISIBLE);

        // 위치 조회는 백그라운드에서, API 호출은 UI 스레드(Retrofit enqueue)
        ioPool.execute(() -> {
            Location current = getBestLastKnownLocation();
            final double lat = current != null ? current.getLatitude() : 37.5665;
            final double lng = current != null ? current.getLongitude() : 126.9780;
            final boolean usingFallback = current == null;

            if (getActivity() == null || !isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded() || getContext() == null) return;

                ApiService api = RetrofitClient.getApiService(requireContext());
                api.listNearbyHospitals(lat, lng, DEFAULT_RADIUS_KM, null, null, null)
                        .enqueue(new Callback<List<HospitalNearbyResponse>>() {
                            @Override
                            public void onResponse(Call<List<HospitalNearbyResponse>> call,
                                                   Response<List<HospitalNearbyResponse>> response) {
                                if (!isAdded() || getContext() == null) return;
                                progressHospitals.setVisibility(View.GONE);
                                if (response.isSuccessful() && response.body() != null) {
                                    hospitals.clear();
                                    hospitals.addAll(response.body());
                                    sortHospitals();
                                    adapter.notifyDataSetChanged();
                                    if (usingFallback) {
                                        Toast.makeText(requireContext(),
                                                "현재 위치를 가져올 수 없어 서울 기준으로 표시합니다.",
                                                Toast.LENGTH_LONG).show();
                                    } else if (hospitals.isEmpty()) {
                                        Toast.makeText(requireContext(),
                                                "주변 동물병원을 찾지 못했습니다.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(requireContext(),
                                            "병원 검색 실패 (HTTP " + response.code() + ")",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<List<HospitalNearbyResponse>> call, Throwable t) {
                                if (!isAdded() || getContext() == null) return;
                                progressHospitals.setVisibility(View.GONE);
                                Toast.makeText(requireContext(),
                                        "병원 검색 실패: " + t.getClass().getSimpleName(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            });
        });
    }

    private void sortHospitals() {
        Collections.sort(hospitals, Comparator
                .comparing((HospitalNearbyResponse h) -> !Boolean.TRUE.equals(h.getFavorited()))
                .thenComparing(h -> h.getDistanceKm() == null ? Double.MAX_VALUE : h.getDistanceKm()));
    }

    private Location getBestLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        LocationManager lm = (LocationManager) requireContext().getSystemService(android.content.Context.LOCATION_SERVICE);
        if (lm == null) return null;
        Location gps = null;
        Location net = null;
        try {
            gps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } catch (Exception ignored) {
        }
        try {
            net = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {
        }
        if (gps == null) return net;
        if (net == null) return gps;
        return gps.getTime() >= net.getTime() ? gps : net;
    }

    private class HospitalAdapter extends RecyclerView.Adapter<HospitalViewHolder> {
        @NonNull
        @Override
        public HospitalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_hospital, parent, false);
            return new HospitalViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull HospitalViewHolder holder, int position) {
            holder.bind(hospitals.get(position));
            holder.itemView.setAlpha(0f);
            holder.itemView.setTranslationY(10f);
            holder.itemView.animate().alpha(1f).translationY(0f).setDuration(180).start();
            holder.itemView.setOnTouchListener((v, ev) -> {
                int action = ev.getActionMasked();
                if (action == android.view.MotionEvent.ACTION_DOWN) {
                    v.animate().scaleX(0.985f).scaleY(0.985f).setDuration(85).start();
                } else if (action == android.view.MotionEvent.ACTION_UP
                        || action == android.view.MotionEvent.ACTION_CANCEL) {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(110).start();
                }
                return false;
            });
        }

        @Override
        public int getItemCount() {
            return hospitals.size();
        }
    }

    private class HospitalViewHolder extends RecyclerView.ViewHolder {
        private final TextView textHospitalName;
        private final TextView textHospitalAddress;
        private final TextView textHospitalMeta;
        private final TextView textFavoriteBadge;
        private final MaterialButton btnFavorite;

        HospitalViewHolder(@NonNull View itemView) {
            super(itemView);
            textHospitalName = itemView.findViewById(R.id.textHospitalName);
            textHospitalAddress = itemView.findViewById(R.id.textHospitalAddress);
            textHospitalMeta = itemView.findViewById(R.id.textHospitalMeta);
            textFavoriteBadge = itemView.findViewById(R.id.textFavoriteBadge);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }

        void bind(HospitalNearbyResponse item) {
            textHospitalName.setText(item.getName() != null ? item.getName() : "-");
            textHospitalAddress.setText(item.getAddress() != null ? item.getAddress() : "-");

            itemView.setOnClickListener(v -> {
                if (item.getPlaceUrl() != null && !item.getPlaceUrl().isEmpty()) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(item.getPlaceUrl())));
                    return;
                }
                String q = (item.getName() != null ? item.getName() : "")
                        + " " + (item.getAddress() != null ? item.getAddress() : "");
                if (q.trim().isEmpty()) return;
                String query = Uri.encode(q.trim());
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://map.kakao.com/link/search/" + query)));
            });

            StringBuilder meta = new StringBuilder();
            if (item.getDistanceKm() != null) {
                meta.append("거리 약 ").append(
                        String.format(java.util.Locale.getDefault(), "%.2fkm", item.getDistanceKm()));
            }
            if (item.getPhone() != null && !item.getPhone().isEmpty()) {
                if (meta.length() > 0) meta.append("\n");
                meta.append(item.getPhone());
            }
            textHospitalMeta.setText(meta.length() > 0 ? meta.toString() : "카카오맵에서 자세히 보기");

            // 서버 DB id가 없으면 즐겨찾기 버튼 숨김
            if (item.getId() == null) {
                btnFavorite.setVisibility(View.GONE);
                textFavoriteBadge.setVisibility(View.GONE);
                return;
            }
            btnFavorite.setVisibility(View.VISIBLE);

            boolean fav = Boolean.TRUE.equals(item.getFavorited());
            updateFavoriteUi(fav);

            btnFavorite.setOnClickListener(v -> {
                boolean before = Boolean.TRUE.equals(item.getFavorited());
                boolean after = !before;

                // Optimistic update — 먼저 UI 반영
                item.setFavorited(after);
                updateFavoriteUi(after);
                sortHospitals();
                adapter.notifyDataSetChanged();

                // 서버 반영
                ApiService api = RetrofitClient.getApiService(requireContext());
                Call<Void> call = after
                        ? api.addHospitalFavorite(item.getId())
                        : api.removeHospitalFavorite(item.getId());

                call.enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> c, Response<Void> response) {
                        if (!isAdded() || getContext() == null) return;
                        if (!response.isSuccessful()) {
                            // 서버 실패 시 롤백
                            item.setFavorited(before);
                            sortHospitals();
                            adapter.notifyDataSetChanged();
                            Toast.makeText(requireContext(),
                                    "찜 변경 실패 (HTTP " + response.code() + ")",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> c, Throwable t) {
                        if (!isAdded() || getContext() == null) return;
                        // 네트워크 실패 시 롤백
                        item.setFavorited(before);
                        sortHospitals();
                        adapter.notifyDataSetChanged();
                        Toast.makeText(requireContext(),
                                "네트워크 오류로 찜 변경에 실패했습니다.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        private void updateFavoriteUi(boolean fav) {
            textFavoriteBadge.setVisibility(fav ? View.VISIBLE : View.GONE);
            textFavoriteBadge.setText(fav ? "찜한 병원" : "찜");
            btnFavorite.setIconResource(fav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            int tint = ContextCompat.getColor(itemView.getContext(),
                    fav ? R.color.semantic_warning : R.color.app_on_surface_variant);
            btnFavorite.setIconTint(ColorStateList.valueOf(tint));
            itemView.setScaleX(fav ? 1.005f : 1f);
            itemView.setScaleY(fav ? 1.005f : 1f);
            if (itemView instanceof MaterialCardView) {
                MaterialCardView card = (MaterialCardView) itemView;
                card.setStrokeColor(ContextCompat.getColor(itemView.getContext(),
                        fav ? R.color.semantic_warning : R.color.app_outline));
                card.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                        fav ? R.color.app_secondary_container : R.color.app_surface));
            }
        }
    }
}

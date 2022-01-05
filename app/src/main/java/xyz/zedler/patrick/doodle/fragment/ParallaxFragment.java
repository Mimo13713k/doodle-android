/*
 * This file is part of Doodle Android.
 *
 * Doodle Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Doodle Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Doodle Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2019-2022 by Patrick Zedler
 */

package xyz.zedler.patrick.doodle.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.slider.Slider;
import com.google.android.material.slider.Slider.OnChangeListener;
import java.util.Locale;
import xyz.zedler.patrick.doodle.Constants.DEF;
import xyz.zedler.patrick.doodle.Constants.PREF;
import xyz.zedler.patrick.doodle.R;
import xyz.zedler.patrick.doodle.activity.MainActivity;
import xyz.zedler.patrick.doodle.behavior.ScrollBehavior;
import xyz.zedler.patrick.doodle.behavior.SystemBarBehavior;
import xyz.zedler.patrick.doodle.databinding.FragmentParallaxBinding;
import xyz.zedler.patrick.doodle.util.ResUtil;
import xyz.zedler.patrick.doodle.util.SensorUtil;
import xyz.zedler.patrick.doodle.util.ViewUtil;

public class ParallaxFragment extends BaseFragment
    implements OnClickListener, OnCheckedChangeListener, OnChangeListener {

  private static final String TAG = ParallaxFragment.class.getSimpleName();

  private FragmentParallaxBinding binding;
  private MainActivity activity;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
  ) {
    try {
      binding = FragmentParallaxBinding.inflate(inflater, container, false);
    } catch (Exception e) {
      // NumberFormatException and NotFoundException on Android 5?
      Log.e(TAG, "onCreateView: ", e);
    }
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBarParallax);
    systemBarBehavior.setScroll(binding.scrollParallax, binding.linearParallaxContainer);
    systemBarBehavior.setAdditionalBottomInset(activity.getFabTopEdgeDistance());
    systemBarBehavior.setUp();

    new ScrollBehavior(activity).setUpScroll(
        binding.appBarParallax, binding.scrollParallax, true
    );

    binding.toolbarParallax.setNavigationOnClickListener(v -> {
      if (getViewUtil().isClickEnabled()) {
        performHapticClick();
        navigateUp();
      }
    });
    binding.toolbarParallax.setOnMenuItemClickListener(item -> {
      int id = item.getItemId();
      if (id == R.id.action_share) {
        ResUtil.share(activity, R.string.msg_share);
        performHapticClick();
        return true;
      } else if (id == R.id.action_feedback) {
        performHapticClick();
        navigate(ParallaxFragmentDirections.actionParallaxToFeedbackDialog());
        return true;
      } else {
        return false;
      }
    });

    binding.sliderParallaxIntensity.setValue(getSharedPrefs().getInt(PREF.PARALLAX, DEF.PARALLAX));
    binding.sliderParallaxIntensity.addOnChangeListener(this);
    binding.sliderParallaxIntensity.setLabelFormatter(value -> {
      if (value == 0) {
        return getString(R.string.parallax_none);
      } else {
        return String.valueOf((int) value);
      }
    });

    binding.linearParallaxTiltContainer.setVisibility(
        SensorUtil.hasAccelerometer(activity) ? View.VISIBLE : View.GONE
    );

    binding.switchParallaxTilt.setChecked(getSharedPrefs().getBoolean(PREF.TILT, DEF.TILT));

    ViewUtil.setEnabledAlpha(
        binding.switchParallaxTilt.isChecked(),
        false,
        binding.linearParallaxRefreshRate,
        binding.linearParallaxDamping,
        binding.linearParallaxThreshold
    );
    ViewUtil.setEnabled(
        binding.switchParallaxTilt.isChecked(),
        binding.sliderParallaxRefreshRate,
        binding.sliderParallaxDamping,
        binding.sliderParallaxThreshold
    );

    binding.sliderParallaxRefreshRate.setValue(
        getSharedPrefs().getInt(PREF.REFRESH_RATE, DEF.REFRESH_RATE)
    );
    binding.sliderParallaxRefreshRate.addOnChangeListener(this);
    binding.sliderParallaxRefreshRate.setLabelFormatter(
        value -> getString(
            R.string.label_ms, String.format(Locale.getDefault(), "%.0f", value / 1000)
        )
    );

    binding.sliderParallaxDamping.setValue(
        getSharedPrefs().getInt(PREF.DAMPING_TILT, DEF.DAMPING_TILT)
    );
    binding.sliderParallaxDamping.addOnChangeListener(this);
    binding.sliderParallaxDamping.setLabelFormatter(
        value -> String.format(Locale.getDefault(), "%.0f", value)
    );

    binding.sliderParallaxThreshold.setValue(
        getSharedPrefs().getInt(PREF.THRESHOLD, DEF.THRESHOLD)
    );
    binding.sliderParallaxThreshold.addOnChangeListener(this);
    binding.sliderParallaxThreshold.setLabelFormatter(
        value -> String.format(Locale.getDefault(), "%.0f", value)
    );

    ViewUtil.setOnClickListeners(
        this,
        binding.linearParallaxTilt
    );

    ViewUtil.setOnCheckedChangeListeners(
        this,
        binding.switchParallaxTilt
    );
  }

  @Override
  public void onResume() {
    super.onResume();
    binding.cardParallaxTouchWiz.setVisibility(activity.isTouchWiz() ? View.VISIBLE : View.GONE);
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.linear_parallax_tilt) {
      binding.switchParallaxTilt.setChecked(!binding.switchParallaxTilt.isChecked());
    }
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    int id = buttonView.getId();
    if (id == R.id.switch_parallax_tilt) {
      getSharedPrefs().edit().putBoolean(PREF.TILT, isChecked).apply();
      activity.requestSettingsRefresh();
      performHapticClick();
      ViewUtil.startIcon(binding.imageParallaxTilt);
      ViewUtil.setEnabledAlpha(
          isChecked,
          true,
          binding.linearParallaxRefreshRate,
          binding.linearParallaxDamping,
          binding.linearParallaxThreshold
      );
      ViewUtil.setEnabled(
          isChecked,
          binding.sliderParallaxRefreshRate,
          binding.sliderParallaxDamping,
          binding.sliderParallaxThreshold
      );
    }
  }

  @Override
  public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
    if (!fromUser) {
      return;
    }
    int id = slider.getId();
    if (id == R.id.slider_parallax_intensity) {
      getSharedPrefs().edit().putInt(PREF.PARALLAX, (int) value).apply();
      ViewUtil.startIcon(binding.imageParallaxIntensity);
      activity.requestSettingsRefresh();
      performHapticClick();
    } else if (id == R.id.slider_parallax_refresh_rate) {
      getSharedPrefs().edit().putInt(PREF.REFRESH_RATE, (int) value).apply();
      ViewUtil.startIcon(binding.imageParallaxRefreshRate);
      activity.requestSettingsRefresh();
      performHapticClick();
    } else if (id == R.id.slider_parallax_damping) {
      getSharedPrefs().edit().putInt(PREF.DAMPING_TILT, (int) value).apply();
      ViewUtil.startIcon(binding.imageParallaxDamping);
      activity.requestSettingsRefresh();
      performHapticClick();
    } else if (id == R.id.slider_parallax_threshold) {
      getSharedPrefs().edit().putInt(PREF.THRESHOLD, (int) value).apply();
      ViewUtil.startIcon(binding.imageParallaxThreshold);
      activity.requestSettingsRefresh();
      performHapticClick();
    }
  }
}
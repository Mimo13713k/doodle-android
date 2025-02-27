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

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Build;
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
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.snackbar.Snackbar.Callback;
import java.util.Locale;
import xyz.zedler.patrick.doodle.Constants.DEF;
import xyz.zedler.patrick.doodle.Constants.EXTRA;
import xyz.zedler.patrick.doodle.Constants.PREF;
import xyz.zedler.patrick.doodle.Constants.THEME;
import xyz.zedler.patrick.doodle.NavMainDirections;
import xyz.zedler.patrick.doodle.R;
import xyz.zedler.patrick.doodle.activity.LauncherActivity;
import xyz.zedler.patrick.doodle.activity.MainActivity;
import xyz.zedler.patrick.doodle.behavior.ScrollBehavior;
import xyz.zedler.patrick.doodle.behavior.SystemBarBehavior;
import xyz.zedler.patrick.doodle.databinding.FragmentOtherBinding;
import xyz.zedler.patrick.doodle.model.Language;
import xyz.zedler.patrick.doodle.service.LiveWallpaperService;
import xyz.zedler.patrick.doodle.util.LocaleUtil;
import xyz.zedler.patrick.doodle.util.ResUtil;
import xyz.zedler.patrick.doodle.util.SystemUiUtil;
import xyz.zedler.patrick.doodle.util.ViewUtil;
import xyz.zedler.patrick.doodle.view.SelectionCardView;

public class OtherFragment extends BaseFragment
    implements OnClickListener, OnCheckedChangeListener {

  private static final String TAG = OtherFragment.class.getSimpleName();

  private FragmentOtherBinding binding;
  private MainActivity activity;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
  ) {
    binding = FragmentOtherBinding.inflate(inflater, container, false);
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
    systemBarBehavior.setAppBar(binding.appBarOther);
    systemBarBehavior.setScroll(binding.scrollOther, binding.constraintOther);
    systemBarBehavior.setAdditionalBottomInset(activity.getFabTopEdgeDistance());
    systemBarBehavior.setUp();

    new ScrollBehavior(activity).setUpScroll(
        binding.appBarOther, binding.scrollOther, true
    );

    ViewUtil.centerToolbarTitleOnLargeScreens(binding.toolbarOther);
    binding.toolbarOther.setNavigationOnClickListener(getNavigationOnClickListener());
    binding.toolbarOther.setOnMenuItemClickListener(getOnMenuItemClickListener());

    binding.textOtherLanguage.setText(getLanguage());

    setUpThemeSelection();

    boolean gpuOptionEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    ViewUtil.setEnabledAlpha(gpuOptionEnabled, false, binding.linearOtherGpu);
    ViewUtil.setEnabled(gpuOptionEnabled, binding.switchOtherGpu);
    binding.cardOtherGpu.setVisibility(gpuOptionEnabled ? View.GONE : View.VISIBLE);
    if (gpuOptionEnabled) {
      binding.linearOtherGpu.setOnClickListener(this);
    }
    binding.switchOtherGpu.setChecked(
        gpuOptionEnabled && getSharedPrefs().getBoolean(PREF.GPU, DEF.GPU)
    );

    binding.switchOtherLauncher.setChecked(
        activity.getPackageManager().getComponentEnabledSetting(
            new ComponentName(activity, LauncherActivity.class)
        ) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    );

    int id;
    switch (getSharedPrefs().getInt(PREF.MODE, DEF.MODE)) {
      case AppCompatDelegate.MODE_NIGHT_NO:
        id = R.id.button_other_theme_light;
        break;
      case AppCompatDelegate.MODE_NIGHT_YES:
        id = R.id.button_other_theme_dark;
        break;
      default:
        id = R.id.button_other_theme_auto;
        break;
    }
    binding.toggleOtherTheme.check(id);
    binding.toggleOtherTheme.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
      if (!isChecked) {
        return;
      }
      int pref;
      if (checkedId == R.id.button_other_theme_light) {
        pref = AppCompatDelegate.MODE_NIGHT_NO;
      } else if (checkedId == R.id.button_other_theme_dark) {
        pref = AppCompatDelegate.MODE_NIGHT_YES;
      } else {
        pref = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
      }
      getSharedPrefs().edit().putInt(PREF.MODE, pref).apply();
      performHapticClick();
      activity.restartToApply(
          0, getInstanceState(), false, true
      );
    });

    ViewUtil.setOnClickListeners(
        this,
        binding.linearOtherLanguage,
        binding.linearOtherLauncher,
        binding.linearOtherReset
    );

    ViewUtil.setOnCheckedChangeListeners(
        this,
        binding.switchOtherGpu,
        binding.switchOtherLauncher
    );
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.linear_other_language) {
      ViewUtil.startIcon(binding.imageOtherLanguage);
      performHapticClick();
      navigate(OtherFragmentDirections.actionOtherToLanguagesDialog());
    } else if (id == R.id.linear_other_gpu) {
      ViewUtil.startIcon(binding.imageOtherGpu);
      binding.switchOtherGpu.setChecked(!binding.switchOtherGpu.isChecked());
    } else if (id == R.id.linear_other_launcher) {
      ViewUtil.startIcon(binding.imageOtherLauncher);
      binding.switchOtherLauncher.setChecked(!binding.switchOtherLauncher.isChecked());
    } else if (id == R.id.linear_other_reset && getViewUtil().isClickEnabled(id)) {
      ViewUtil.startIcon(binding.imageOtherReset);
      performHapticClick();
      activity.showSnackbar(
          activity.getSnackbar(
              R.string.msg_reset, Snackbar.LENGTH_LONG
          ).setAction(
              getString(R.string.action_reset), view -> {
                performHapticHeavyClick();
                activity.reset();
                activity.restartToApply(
                    100,
                    getInstanceState(),
                    LiveWallpaperService.isMainEngineRunning(),
                    false
                );
              }
          )
      );
    }
  }

  @SuppressLint("ShowToast")
  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    int id = buttonView.getId();
    if (id == R.id.switch_other_gpu) {
      getSharedPrefs().edit().putBoolean(PREF.GPU, isChecked).apply();
      performHapticClick();
      activity.showForceStopRequest(NavMainDirections.actionGlobalApplyDialog());
    } else if (id == R.id.switch_other_launcher) {
      performHapticClick();
      if (isChecked) {
        activity.showSnackbar(
            activity.getSnackbar(
                R.string.msg_hide, Snackbar.LENGTH_LONG
            ).setAction(
                getString(R.string.action_hide), view -> {
                  performHapticHeavyClick();
                  activity.getPackageManager().setComponentEnabledSetting(
                      new ComponentName(activity, LauncherActivity.class),
                      PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                      PackageManager.DONT_KILL_APP
                  );
                }
            ).addCallback(new Callback() {
              @Override
              public void onDismissed(Snackbar transientBottomBar, int event) {
                if (binding == null || activity == null
                    || event == BaseCallback.DISMISS_EVENT_CONSECUTIVE) {
                  return;
                }
                try {
                  binding.switchOtherLauncher.setOnCheckedChangeListener(null);
                  binding.switchOtherLauncher.setChecked(
                      activity.getPackageManager().getComponentEnabledSetting(
                          new ComponentName(activity, LauncherActivity.class)
                      ) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                  );
                  binding.switchOtherLauncher.setOnCheckedChangeListener(OtherFragment.this);
                } catch (NullPointerException e) {
                  Log.e(TAG, "onDismissed: error when the snackbar was dismissed", e);
                }
              }
            })
        );
      } else {
        activity.getPackageManager().setComponentEnabledSetting(
            new ComponentName(activity, LauncherActivity.class),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        );
      }
    }
  }

  public void setLanguage(Language language) {
    Locale locale = language != null
        ? LocaleUtil.getLocaleFromCode(language.getCode())
        : LocaleUtil.getNearestSupportedLocale(activity, LocaleUtil.getDeviceLocale());
    binding.textOtherLanguage.setText(
        language != null
            ? locale.getDisplayName()
            : getString(R.string.other_language_system, locale.getDisplayName())
    );
  }

  public String getLanguage() {
    String code = getSharedPrefs().getString(PREF.LANGUAGE, DEF.LANGUAGE);
    Locale locale = code != null
        ? LocaleUtil.getLocaleFromCode(code)
        : LocaleUtil.getNearestSupportedLocale(activity, LocaleUtil.getDeviceLocale());
    return code != null
        ? locale.getDisplayName()
        : getString(R.string.other_language_system, locale.getDisplayName());
  }

  private void setUpThemeSelection() {
    boolean hasDynamic = DynamicColors.isDynamicColorAvailable();
    ViewGroup container = binding.linearOtherThemeContainer;
    int colorsCount = 8;
    for (int i = hasDynamic ? -1 : 0; i <= colorsCount; i++) {
      String name;
      int resId;
      if (i == -1) {
        name = THEME.DYNAMIC;
        resId = -1;
      } else if (i == 0) {
        name = THEME.RED;
        resId = R.style.Theme_Doodle_Red;
      } else if (i == 1) {
        name = THEME.YELLOW;
        resId = R.style.Theme_Doodle_Yellow;
      } else if (i == 2) {
        name = THEME.LIME;
        resId = R.style.Theme_Doodle_Lime;
      } else if (i == 3) {
        name = THEME.GREEN;
        resId = R.style.Theme_Doodle_Green;
      } else if (i == 4) {
        name = THEME.TURQUOISE;
        resId = R.style.Theme_Doodle_Turquoise;
      } else if (i == 5) {
        name = THEME.TEAL;
        resId = R.style.Theme_Doodle_Teal;
      } else if (i == 6) {
        name = THEME.BLUE;
        resId = R.style.Theme_Doodle_Blue;
      } else if (i == 7) {
        name = THEME.PURPLE;
        resId = R.style.Theme_Doodle_Purple;
      } else if (i == 8) {
        name = THEME.AMOLED;
        resId = R.style.Theme_Doodle_Amoled;
      } else {
        name = THEME.BLUE;
        resId = R.style.Theme_Doodle_Blue;
      }

      SelectionCardView card = new SelectionCardView(activity);
      card.setScrimEnabled(false, false);
      int color;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && i == -1) {
        color = ContextCompat.getColor(
            activity,
            SystemUiUtil.isDarkModeActive(activity)
                ? android.R.color.system_accent1_700
                : android.R.color.system_accent1_100
        );
      } else if (i == colorsCount) {
        // Amoled theme selection card
        color = SystemUiUtil.isDarkModeActive(activity) ? 0x484848 : 0xe3e3e3;
      } else {
        color = ResUtil.getColorAttr(
            new ContextThemeWrapper(activity, resId), R.attr.colorPrimaryContainer
        );
      }
      card.setCardBackgroundColor(color);
      card.setOnClickListener(v -> {
        if (!card.isChecked()) {
          card.startCheckedIcon();
          ViewUtil.startIcon(binding.imageOtherTheme);
          performHapticClick();
          ViewUtil.uncheckAllChildren(container);
          card.setChecked(true);
          getSharedPrefs().edit().putString(PREF.THEME, name).apply();
          activity.restartToApply(
              100, getInstanceState(), false, true
          );
        }
      });

      String selected = getSharedPrefs().getString(PREF.THEME, DEF.THEME);
      boolean isSelected;
      if (selected.isEmpty()) {
        isSelected = hasDynamic ? name.equals(THEME.DYNAMIC) : name.equals(THEME.BLUE);
      } else {
        isSelected = selected.equals(name);
      }
      card.setChecked(isSelected);
      container.addView(card);
    }

    Bundle bundleInstanceState = activity.getIntent().getBundleExtra(EXTRA.INSTANCE_STATE);
    if (bundleInstanceState != null) {
      binding.scrollHorizOtherTheme.scrollTo(
          bundleInstanceState.getInt(EXTRA.SCROLL_POSITION, 0),
          0
      );
    }
  }

  private Bundle getInstanceState() {
    Bundle bundle = new Bundle();
    if (binding != null) {
      bundle.putInt(EXTRA.SCROLL_POSITION, binding.scrollHorizOtherTheme.getScrollX());
    }
    return bundle;
  }
}
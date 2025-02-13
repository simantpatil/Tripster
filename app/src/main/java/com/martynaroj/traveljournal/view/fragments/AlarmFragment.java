package com.martynaroj.traveljournal.view.fragments;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.martynaroj.traveljournal.R;
import com.martynaroj.traveljournal.databinding.DialogAlarmBinding;
import com.martynaroj.traveljournal.databinding.DialogCustomBinding;
import com.martynaroj.traveljournal.databinding.FragmentAlarmBinding;
import com.martynaroj.traveljournal.services.others.NotificationBroadcast;
import com.martynaroj.traveljournal.view.base.BaseFragment;
import com.martynaroj.traveljournal.view.others.classes.DialogHandler;
import com.martynaroj.traveljournal.view.others.classes.PickerColorize;
import com.martynaroj.traveljournal.view.others.classes.SharedPreferencesUtils;
import com.martynaroj.traveljournal.view.others.interfaces.Constants;
import com.martynaroj.traveljournal.viewmodels.UserViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class AlarmFragment extends BaseFragment implements View.OnClickListener {

    private FragmentAlarmBinding binding;
    private UserViewModel userViewModel;
    private Intent broadcastIntent;
    private Timer timer;
    private boolean isAlarmCanceled;



    public static AlarmFragment newInstance() {
        return new AlarmFragment();
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAlarmBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        initViewModels();
        observeUserChanges();

        initBroadcastIntent();
        checkBroadcast();
        createNotificationChannel();

        checkPermissionsDialog();

        setListeners();

        return view;
    }


    //INIT DATA-------------------------------------------------------------------------------------


    private void initViewModels() {
        if (getActivity() != null) {
            userViewModel = new ViewModelProvider(getActivity()).get(UserViewModel.class);
        }
    }


    private void observeUserChanges() {
        userViewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user == null)
                back();
        });
    }


    private void initBroadcastIntent() {
        broadcastIntent = new Intent(getContext(), NotificationBroadcast.class);
    }


    private void checkBroadcast() {
        boolean isBroadcastWorking = (PendingIntent.getBroadcast(getContext(), Constants.RC_BROADCAST,
                broadcastIntent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE) != null);

        binding.setIsBroadcastWorking(isBroadcastWorking);

        if (isBroadcastWorking) {
            getAlarmSet(); // Update the UI with alarm data
        }
    }



    private void createNotificationChannel() {
        if (getContext() != null) {
            NotificationBroadcast.createNotificationChannel(getContext());
        }
    }


    private void initTimer(long alarmTime) {
        Log.d("AlarmFragment", "Initializing timer with alarm time: " + alarmTime);

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Calendar now = Calendar.getInstance();

                Log.d("AlarmFragment", "Timer running...");
                Log.d("AlarmFragment", "Alarm time: " + alarmTime);
                Log.d("AlarmFragment", "Current time: " + now.getTimeInMillis());

                if (alarmTime < now.getTimeInMillis() || isAlarmCanceled) {
                    Log.d("AlarmFragment", "Alarm expired or canceled. Stopping timer.");

                    if (binding != null) {
                        binding.setIsBroadcastWorking(false);
                    }
                    timer.cancel();
                }
            }
        }, 0, 1000); // Run every second to check the condition
    }



    private void checkPermissionsDialog() {
        if (!SharedPreferencesUtils.getBoolean(getContext(), Constants.ALARM_DIALOG, false)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            showPermissionsDialog();
        }
    }


    //SHARED PREFS----------------------------------------------------------------------------------


    private void getAlarmSet() {
        if (getContext() != null) {
            String alarmNote = SharedPreferencesUtils.getString(getContext(), Constants.ALARM_NOTE, "");
            Calendar alarmDate = Calendar.getInstance();
            alarmDate.setTimeInMillis(SharedPreferencesUtils.getLong(getContext(), Constants.ALARM_TIME, 0));
            setAlarmData(alarmDate, alarmNote);
            initTimer(alarmDate.getTimeInMillis());
        }
    }


    @SuppressLint("SimpleDateFormat")
    private void setAlarmData(Calendar alarmDate, String alarmNote) {
        binding.alarmAlarmSetDate.setText(new SimpleDateFormat("dd.MM.yyyy").format(alarmDate.getTime()));
        binding.alarmAlarmSetTime.setText(new SimpleDateFormat("hh:mm a").format(alarmDate.getTime()));
        binding.alarmAlarmSetNote.setText(alarmNote);
    }


    private void saveShownAlarmDialog() {
        if (getContext() != null) {
            SharedPreferencesUtils.setBoolean(getContext(), Constants.ALARM_DIALOG, true);
        }
    }


    //ALARM-----------------------------------------------------------------------------------------


    private void setAlarm(Dialog dialog, DatePicker datePicker, TimePicker timePicker) {
        try {
            int dMin, dHour;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                dMin = timePicker.getMinute();
                dHour = timePicker.getHour();
            } else {
                dMin = timePicker.getCurrentMinute();
                dHour = timePicker.getCurrentHour();
            }
            int dDay = datePicker.getDayOfMonth();
            int dMonth = datePicker.getMonth();
            int dYear = datePicker.getYear();

            // Validate date and time
            Calendar dateStart = Calendar.getInstance();
            Calendar dateEnd = Calendar.getInstance();
            dateEnd.set(dYear, dMonth, dDay, dHour, dMin, 0);

            long timeDifference = dateEnd.getTimeInMillis() - dateStart.getTimeInMillis();

            if (timeDifference <= 0) {
                dialog.findViewById(R.id.dialog_alarm_error).setVisibility(View.VISIBLE);
                Log.e("AlarmFragment", "Invalid timeDifference: " + timeDifference);
                return;
            }

            // Get note input
            TextInputEditText editText = dialog.findViewById(R.id.dialog_alarm_note_input);
            String note = (editText != null && editText.getText() != null) ? editText.getText().toString() : "No note";

            // Save alarm data
            isAlarmCanceled = false;
            try {
                SharedPreferencesUtils.saveAlarmSet(getContext(), dateEnd.getTimeInMillis(), note);
            } catch (Exception e) {
                Log.e("AlarmFragment", "Error saving alarm data", e);
            }

            // Set notification broadcast
            setNotificationBroadcast(note, timeDifference);

            dialog.dismiss();
            Log.d("AlarmFragment", "Alarm set successfully for: " + dateEnd.getTime());
        } catch (Exception e) {
            Log.e("AlarmFragment", "Error in setAlarm method", e);
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private void setNotificationBroadcast(String note, long timeDifference) {
        if (getContext() != null) {
            try {
                // Get AlarmManager instance
                AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);

                // Create a PendingIntent for the broadcast
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        getContext(),
                        Constants.RC_BROADCAST,
                        broadcastIntent.putExtra(Constants.ALARM_NOTE, note),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE // Ensure proper flags
                );

                // Schedule the alarm
                long triggerAtMillis = System.currentTimeMillis() + timeDifference;
                if (alarmManager != null) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                }

                Log.d("AlarmFragment", "Alarm set for: " + triggerAtMillis);

                // Update UI and notify user
                showSnackBar(getString(R.string.messages_alarm_set_success), Snackbar.LENGTH_SHORT);
                binding.setIsBroadcastWorking(true);

            } catch (Exception e) {
                Log.e("AlarmFragment", "Error in setNotificationBroadcast", e);
                showSnackBar(getString(R.string.messages_alarm_set_success), Snackbar.LENGTH_LONG);
            }
        }
    }




    private void cancelAlarm() {
        if (getContext() != null) {
            AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    getContext(),
                    Constants.RC_BROADCAST,
                    broadcastIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );

            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
                isAlarmCanceled = true;

                // Show Snackbar
                Snackbar.make(binding.getRoot(), "Alarm canceled successfully!", Snackbar.LENGTH_SHORT).show();
            }

            // Navigate back to the Alarm Fragment
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();  // Navigate back to the previous fragment
            } else {
                // If there are no fragments on the back stack, you can navigate to the AlarmFragment explicitly
                FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_alarm, new AlarmFragment()); // Use the correct container view ID
                transaction.addToBackStack(null); // Add the transaction to the back stack so you can pop it later
                transaction.commit();
            }
        }
    }





    //LISTENERS-------------------------------------------------------------------------------------


    private void setListeners() {
        binding.alarmArrowButton.setOnClickListener(this);
        binding.alarmSetButton.setOnClickListener(this);
        binding.alarmAlarmSetCancelButton.setOnClickListener(this);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.alarm_arrow_button:
                back();
                break;
            case R.id.alarm_set_button:
                showSetAlarmDialog();
                break;
            case R.id.alarm_alarm_set_cancel_button:
                cancelAlarm();
                break;
        }
    }


    //DIALOGS---------------------------------------------------------------------------------------


    private void showSetAlarmDialog() {
        if (getContext() != null) {
            Dialog dialog = DialogHandler.createDialog(getContext(), true);
            DialogAlarmBinding binding = DialogAlarmBinding.inflate(LayoutInflater.from(getContext()));
            dialog.setContentView(binding.getRoot());
            PickerColorize.colorizeDatePicker(binding.dialogAlarmDatePicker, getResources().getColor(R.color.light_gray));
            PickerColorize.colorizeTimePicker(binding.dialogAlarmTimePicker, getResources().getColor(R.color.light_gray));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                binding.dialogAlarmDatePicker.setOnDateChangedListener(
                        (datePicker1, i, i1, i2) -> binding.dialogAlarmError.setVisibility(View.GONE)
                );
                binding.dialogAlarmTimePicker.setOnTimeChangedListener(
                        (timePicker1, i, i1) -> binding.dialogAlarmError.setVisibility(View.GONE)
                );
            }
            binding.dialogAlarmDatePicker.setMinDate(new Date().getTime() - 1000);
            binding.dialogAlarmButtomPositive.setOnClickListener(v ->
                    setAlarm(dialog, binding.dialogAlarmDatePicker, binding.dialogAlarmTimePicker)
            );
            binding.dialogAlarmButtonNegative.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        }
    }


    private void showPermissionsDialog() {
        if (getContext() != null) {
            Dialog dialog = DialogHandler.createDialog(getContext(), true);
            DialogCustomBinding binding = DialogCustomBinding.inflate(LayoutInflater.from(getContext()));
            dialog.setContentView(binding.getRoot());
            DialogHandler.initContent(getContext(), binding.dialogCustomTitle, R.string.dialog_alarm_perms_title,
                    binding.dialogCustomDesc, R.string.dialog_alarm_perms_desc,
                    binding.dialogCustomButtonPositive, R.string.dialog_button_settings,
                    binding.dialogCustomButtonNegative, R.string.dialog_button_no_thanks,
                    R.color.main_violet, R.color.violet_bg_light);
            binding.dialogCustomButtonPositive.setOnClickListener(v -> {
                dialog.dismiss();
                saveShownAlarmDialog();
                openSettings();
            });
            binding.dialogCustomButtonNegative.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        }
    }


    //OTHERS----------------------------------------------------------------------------------------


    private void openSettings() {
        if (getContext() != null) {
            Intent intent = new Intent();
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("app_package", getContext().getPackageName());
            intent.putExtra("app_uid", getContext().getApplicationInfo().uid);
            intent.putExtra("android.provider.extra.APP_PACKAGE", getContext().getPackageName());
            startActivity(intent);
        }
    }


    private void back() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0)
            getParentFragmentManager().popBackStack();
    }


    private void showSnackBar(String message, int duration) {
        getSnackBarInteractions().showSnackBar(binding.getRoot(), getActivity(), message, duration);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}

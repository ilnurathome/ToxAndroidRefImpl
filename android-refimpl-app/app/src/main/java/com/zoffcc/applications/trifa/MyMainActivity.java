/**
 * [TRIfA], Java part of Tox Reference Implementation for Android
 * Copyright (C) 2017 - 2020 Zoff <zoff@zoff.cc>
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301, USA.
 */

package com.zoffcc.applications.trifa;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothHeadset;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.github.gfx.android.orma.AccessThreadConstraint;
import com.github.gfx.android.orma.encryption.EncryptedDatabase;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.ios.IosEmojiProvider;
import com.yariksoffice.lingver.Lingver;

import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;

import info.guardianproject.iocipher.VirtualFileSystem;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import info.guardianproject.netcipher.proxy.StatusCallback;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

import static com.zoffcc.applications.trifa.CallingActivity.initializeScreenshotSecurity;
import static com.zoffcc.applications.trifa.HelperGeneric.del_g_opts;
import static com.zoffcc.applications.trifa.HelperGeneric.get_g_opts;
import static com.zoffcc.applications.trifa.HelperGeneric.set_g_opts;
import static com.zoffcc.applications.trifa.MainActivity.AppCrashC;
import static com.zoffcc.applications.trifa.MainActivity.DB_ENCRYPT;
import static com.zoffcc.applications.trifa.MainActivity.FRAME_SIZE_FIXED;
import static com.zoffcc.applications.trifa.MainActivity.MAIN_DB_NAME;
import static com.zoffcc.applications.trifa.MainActivity.MAIN_VFS_NAME;
import static com.zoffcc.applications.trifa.MainActivity.MIN_AUDIO_SAMPLINGRATE_OUT;
import static com.zoffcc.applications.trifa.MainActivity.ORMA_TRACE;
import static com.zoffcc.applications.trifa.MainActivity.PREF__DB_secrect_key;
import static com.zoffcc.applications.trifa.MainActivity.PREF__UV_reversed;
import static com.zoffcc.applications.trifa.MainActivity.PREF__U_keep_nospam;
import static com.zoffcc.applications.trifa.MainActivity.PREF__X_audio_recording_frame_size;
import static com.zoffcc.applications.trifa.MainActivity.PREF__X_battery_saving_mode;
import static com.zoffcc.applications.trifa.MainActivity.PREF__X_battery_saving_timeout;
import static com.zoffcc.applications.trifa.MainActivity.PREF__X_misc_button_enabled;
import static com.zoffcc.applications.trifa.MainActivity.PREF__X_zoom_incoming_video;
import static com.zoffcc.applications.trifa.MainActivity.PREF__allow_screen_off_in_audio_call;
import static com.zoffcc.applications.trifa.MainActivity.PREF__auto_accept_all_upto;
import static com.zoffcc.applications.trifa.MainActivity.PREF__auto_accept_image;
import static com.zoffcc.applications.trifa.MainActivity.PREF__auto_accept_video;
import static com.zoffcc.applications.trifa.MainActivity.PREF__camera_get_preview_format;
import static com.zoffcc.applications.trifa.MainActivity.PREF__conference_show_system_messages;
import static com.zoffcc.applications.trifa.MainActivity.PREF__force_udp_only;
import static com.zoffcc.applications.trifa.MainActivity.PREF__fps_half;
import static com.zoffcc.applications.trifa.MainActivity.PREF__h264_encoder_use_intra_refresh;
import static com.zoffcc.applications.trifa.MainActivity.PREF__higher_audio_quality;
import static com.zoffcc.applications.trifa.MainActivity.PREF__higher_video_quality;
import static com.zoffcc.applications.trifa.MainActivity.PREF__local_discovery_enabled;
import static com.zoffcc.applications.trifa.MainActivity.PREF__min_audio_samplingrate_out;
import static com.zoffcc.applications.trifa.MainActivity.PREF__notification;
import static com.zoffcc.applications.trifa.MainActivity.PREF__notification_sound;
import static com.zoffcc.applications.trifa.MainActivity.PREF__notification_vibrate;
import static com.zoffcc.applications.trifa.MainActivity.PREF__orbot_enabled;
import static com.zoffcc.applications.trifa.MainActivity.PREF__set_fps;
import static com.zoffcc.applications.trifa.MainActivity.PREF__software_echo_cancel;
import static com.zoffcc.applications.trifa.MainActivity.PREF__udp_enabled;
import static com.zoffcc.applications.trifa.MainActivity.PREF__use_incognito_keyboard;
import static com.zoffcc.applications.trifa.MainActivity.PREF__use_native_audio_play;
import static com.zoffcc.applications.trifa.MainActivity.PREF__video_call_quality;
import static com.zoffcc.applications.trifa.MainActivity.PREF__video_cam_resolution;
import static com.zoffcc.applications.trifa.MainActivity.PREF__window_security;
import static com.zoffcc.applications.trifa.MainActivity.SAMPLE_RATE_FIXED;
import static com.zoffcc.applications.trifa.MainActivity.SD_CARD_FILES_EXPORT_DIR;
import static com.zoffcc.applications.trifa.MainActivity.SD_CARD_STATIC_DIR;
import static com.zoffcc.applications.trifa.MainActivity.SD_CARD_TMP_DIR;
import static com.zoffcc.applications.trifa.MainActivity.SD_CARD_TMP_DUMMYFILE;
import static com.zoffcc.applications.trifa.MainActivity.VFS_ENCRYPT;
import static com.zoffcc.applications.trifa.MainActivity.app_files_directory;
import static com.zoffcc.applications.trifa.MainActivity.audio_manager_s;
import static com.zoffcc.applications.trifa.MainActivity.channelId_newmessage_silent;
import static com.zoffcc.applications.trifa.MainActivity.channelId_newmessage_sound;
import static com.zoffcc.applications.trifa.MainActivity.channelId_newmessage_sound_and_vibrate;
import static com.zoffcc.applications.trifa.MainActivity.channelId_newmessage_vibrate;
import static com.zoffcc.applications.trifa.MainActivity.channelId_toxservice;
import static com.zoffcc.applications.trifa.MainActivity.getNativeLibAPI;
import static com.zoffcc.applications.trifa.MainActivity.jnictoxcore_version;
import static com.zoffcc.applications.trifa.MainActivity.native_lib_loaded;
import static com.zoffcc.applications.trifa.MainActivity.nmn3;
import static com.zoffcc.applications.trifa.MainActivity.notification_channel_newmessage_silent;
import static com.zoffcc.applications.trifa.MainActivity.notification_channel_newmessage_sound;
import static com.zoffcc.applications.trifa.MainActivity.notification_channel_newmessage_sound_and_vibrate;
import static com.zoffcc.applications.trifa.MainActivity.notification_channel_newmessage_vibrate;
import static com.zoffcc.applications.trifa.MainActivity.notification_channel_toxservice;
import static com.zoffcc.applications.trifa.MainActivity.packageInfo_s;
import static com.zoffcc.applications.trifa.MainActivity.selected_conference_messages;
import static com.zoffcc.applications.trifa.MainActivity.selected_messages;
import static com.zoffcc.applications.trifa.MainActivity.selected_messages_incoming_file;
import static com.zoffcc.applications.trifa.MainActivity.selected_messages_text_only;
import static com.zoffcc.applications.trifa.MainActivity.tox_self_set_status;
import static com.zoffcc.applications.trifa.MainActivity.tox_service_fg;
import static com.zoffcc.applications.trifa.MainActivity.tox_version_major;
import static com.zoffcc.applications.trifa.MainActivity.tox_version_minor;
import static com.zoffcc.applications.trifa.MainActivity.tox_version_patch;
import static com.zoffcc.applications.trifa.MainActivity.versionCode;
import static com.zoffcc.applications.trifa.MainActivity.versionName;
import static com.zoffcc.applications.trifa.TRIFAGlobals.DELETE_SQL_AND_VFS_ON_ERROR;
import static com.zoffcc.applications.trifa.TRIFAGlobals.GLOBAL_AUDIO_BITRATE;
import static com.zoffcc.applications.trifa.TRIFAGlobals.GLOBAL_VIDEO_BITRATE;
import static com.zoffcc.applications.trifa.TRIFAGlobals.HIGHER_GLOBAL_AUDIO_BITRATE;
import static com.zoffcc.applications.trifa.TRIFAGlobals.LOWER_GLOBAL_AUDIO_BITRATE;
import static com.zoffcc.applications.trifa.TRIFAGlobals.LOWER_GLOBAL_VIDEO_BITRATE;
import static com.zoffcc.applications.trifa.TRIFAGlobals.NORMAL_GLOBAL_AUDIO_BITRATE;
import static com.zoffcc.applications.trifa.TRIFAGlobals.NOTIFICATION_TOKEN_DB_KEY;
import static com.zoffcc.applications.trifa.TRIFAGlobals.NOTIFICATION_TOKEN_DB_KEY_NEED_ACK;
import static com.zoffcc.applications.trifa.TRIFAGlobals.PREF__DB_secrect_key__user_hash;
import static com.zoffcc.applications.trifa.TRIFAGlobals.TRIFA_MSG_TYPE.TRIFA_MSG_FILE;
import static com.zoffcc.applications.trifa.TRIFAGlobals.VFS_PREFIX;
import static com.zoffcc.applications.trifa.TRIFAGlobals.VIDEO_FRAME_RATE_INCOMING;
import static com.zoffcc.applications.trifa.TRIFAGlobals.VIDEO_FRAME_RATE_OUTGOING;
import static com.zoffcc.applications.trifa.TRIFAGlobals.bootstrapping;
import static com.zoffcc.applications.trifa.TRIFAGlobals.count_video_frame_received;
import static com.zoffcc.applications.trifa.TRIFAGlobals.count_video_frame_sent;
import static com.zoffcc.applications.trifa.TRIFAGlobals.global_showing_anygroupview;
import static com.zoffcc.applications.trifa.TRIFAGlobals.global_showing_messageview;
import static com.zoffcc.applications.trifa.TRIFAGlobals.global_tox_self_status;
import static com.zoffcc.applications.trifa.TRIFAGlobals.last_video_frame_received;
import static com.zoffcc.applications.trifa.TRIFAGlobals.last_video_frame_sent;
import static com.zoffcc.applications.trifa.TRIFAGlobals.orbot_is_really_running;
import static com.zoffcc.applications.trifa.ToxVars.TOX_USER_STATUS.TOX_USER_STATUS_AWAY;
import static com.zoffcc.applications.trifa.ToxVars.TOX_USER_STATUS.TOX_USER_STATUS_BUSY;
import static com.zoffcc.applications.trifa.ToxVars.TOX_USER_STATUS.TOX_USER_STATUS_NONE;
import static com.zoffcc.applications.trifa.TrifaToxService.TOX_SERVICE_STARTED;
import static com.zoffcc.applications.trifa.TrifaToxService.is_tox_started;
import static com.zoffcc.applications.trifa.TrifaToxService.orma;
import static com.zoffcc.applications.trifa.TrifaToxService.vfs;

// import static com.zoffcc.applications.loggingstdout.LoggingStdout.start_logging;

/*

first actually relayed message via ToxProxy

2019-08-28 22:20:43.286148 [D] friend_message_v2_cb:
fn=1 res=1 msg=🍔👍😜👍😜 @%\4äö ubnc Ovid n JB von in BK ni ubvzv8 ctcitccccccizzvvcvvv        u  tiigi gig i g35667u 6 66

 */

@RuntimePermissions
public class MyMainActivity extends AppCompatActivity
{
    private static final String TAG = "trifa.MyMainActivity";

    static TextView mt = null;
    ImageView top_imageview = null;

    // static boolean stop_me = false;
    // static Thread ToxServiceThread = null;
    static Semaphore semaphore_videoout_bitmap = new Semaphore(1);
    static Semaphore semaphore_tox_savedata = new Semaphore(1);
    Handler main_handler = null;
    static Handler main_handler_s = null;
    static Context context_s = null;
    static MyMainActivity main_activity_s = null;
    static Resources resources = null;
    static DisplayMetrics metrics = null;
    static RemoteViews notification_view = null;
    static FriendListFragment friend_list_fragment = null;
    static MessageListFragment message_list_fragment = null;
    static MessageListActivity message_list_activity = null;
    static ConferenceMessageListFragment conference_message_list_fragment = null;
    static ConferenceMessageListActivity conference_message_list_activity = null;
    static ConferenceAudioActivity conference_audio_activity = null;

    static int PREF__global_font_size = 2;
    static boolean PREF__tox_set_do_not_sync_av = false;
    public static int PREF__X_eac_delay_ms = 80;
    static int PREF__X_audio_play_buffer_custom = 0;
    public static float PREF_mic_gain_factor = 2.0f;

    final static int AddFriendActivity_ID = 10001;
    final static int CallingActivity_ID = 10002;
    final static int ProfileActivity_ID = 10003;
    final static int SettingsActivity_ID = 10004;
    final static int AboutpageActivity_ID = 10005;
    final static int MaintenanceActivity_ID = 10006;
    final static int WhiteListFromDozeActivity_ID = 10008;
    final static int SelectFriendSingleActivity_ID = 10009;
    final static int SelectLanguageActivity_ID = 10010;
    final static int Notification_new_message_ID = 10023;
    static long Notification_new_message_last_shown_timestamp = -1;
    final static long Notification_new_message_every_millis = 2000; // ~2 seconds between notifications
    final static long UPDATE_MESSAGES_WHILE_FT_ACTIVE_MILLIS = 30000; // ~30 seconds
    final static long UPDATE_MESSAGES_NORMAL_MILLIS = 500; // ~0.5 seconds
    static String temp_string_a = "";
    static ByteBuffer video_buffer_1 = null;
    static ByteBuffer video_buffer_2 = null;
    final static int audio_in_buffer_max_count = 2; // how many out play buffers? [we are now only using buffer "0" !!]
    public final static int audio_out_buffer_mult = 1;
    static ByteBuffer audio_buffer_2 = null; // given to JNI with set_JNI_audio_buffer2() for incoming audio (group and call)
    static long debug__audio_pkt_incoming = 0;
    static long debug__audio_frame_played = 0;
    static long debug__audio_play_buf_count_max = -1;
    static long debug__audio_play_buf01 = 0;
    static long debug__audio_play_buf02 = 0;
    static long debug__audio_play_buf03 = 0;
    static long debug__audio_play_buf04 = 0;
    static long debug__audio_play_buf05 = 0;
    static long debug__audio_play_buf06 = 0;
    static long debug__audio_play_factor = 0;
    static long debug__audio_play_iter = 0;
    // public static long[] audio_buffer_2_ts = new long[n_audio_in_buffer_max_count];
    // static ByteBuffer audio_buffer_play = null;
    static int audio_buffer_play_length = 0;
    static int[] audio_buffer_2_read_length = new int[audio_in_buffer_max_count];
    static MainActivity tox_mainactivity_fg = null;
    static long update_all_messages_global_timestamp = -1;
    static long last_updated_fps = -1;
    final static long update_fps_every_ms = 1500L; // update every 1.5 seconds
    //
    IntentFilter receiverFilter1 = null;
    IntentFilter receiverFilter2 = null;
    IntentFilter receiverFilter3 = null;
    IntentFilter receiverFilter4 = null;
    static HeadsetStateReceiver receiver1 = null;
    static HeadsetStateReceiver receiver2 = null;
    static HeadsetStateReceiver receiver3 = null;
    static HeadsetStateReceiver receiver4 = null;

    static TextView waiting_view = null;
    static ProgressBar waiting_image = null;
    static ViewGroup normal_container = null;
    static ClipboardManager clipboard;
    private ClipData clip;
//    static List<Long> selected_messages = new ArrayList<Long>();
//    static List<Long> selected_messages_text_only = new ArrayList<Long>();
//    static List<Long> selected_messages_incoming_file = new ArrayList<Long>();
//    static List<Long> selected_conference_messages = new ArrayList<Long>();
    //

    // main drawer ----------
    Drawer main_drawer = null;
    AccountHeader main_drawer_header = null;
    ProfileDrawerItem profile_d_item = null;
    // main drawer ----------

    Spinner spinner_own_status = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.i(TAG, "M:STARTUP:super onCreate");
        super.onCreate(savedInstanceState);
        Log.i(TAG, "M:STARTUP:onCreate");
        Log.i(TAG, "onCreate");

        Log.i(TAG, "M:STARTUP:Lingver set");
        Log.d(TAG, "Lingver_Locale: " + Lingver.getInstance().getLocale());
        Log.d(TAG, "Lingver_Language: " + Lingver.getInstance().getLanguage());
        // Log.d(TAG, "Actual_Language: " + resources.configuration.getLocaleCompat());

        resources = this.getResources();
        metrics = resources.getDisplayMetrics();
        global_showing_messageview = false;
        global_showing_anygroupview = false;

        Log.i(TAG, "M:STARTUP:setContentView start");
        setContentView(R.layout.activity_main);
        Log.i(TAG, "M:STARTUP:setContentView end");

        mt = (TextView) this.findViewById(R.id.main_maintext);
        mt.setText("...");
        mt.setVisibility(View.VISIBLE);
        if (native_lib_loaded)
        {
            Log.i(TAG, "M:STARTUP:native_lib_loaded OK");
            mt.setText("successfully loaded native library");
        }
        else
        {
            Log.i(TAG, "M:STARTUP:native_lib_loaded failed");
            mt.setText("loadLibrary jni-c-toxcore failed!");
            show_wrong_credentials();
            finish();
            return;
        }

        Log.i(TAG, "M:STARTUP:toolbar");
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Log.i(TAG, "M:STARTUP:EmojiManager install");
        EmojiManager.install(new IosEmojiProvider());
        // EmojiManager.install(new EmojiOneProvider());


        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        PREF__DB_secrect_key = settings.getString("DB_secrect_key", "");

        if (PREF__DB_secrect_key.isEmpty())
        {
            // ok, use hash of user entered password
            PREF__DB_secrect_key = PREF__DB_secrect_key__user_hash;
        }

        main_handler = new Handler(getMainLooper());
        main_handler_s = main_handler;
        context_s = this.getBaseContext();
        main_activity_s = this;
        TRIFAGlobals.CONFERENCE_CHAT_BG_CORNER_RADIUS_IN_PX = (int) HelperGeneric.dp2px(10);
        TRIFAGlobals.CONFERENCE_CHAT_DRAWER_ICON_CORNER_RADIUS_IN_PX = (int) HelperGeneric.dp2px(20);


        if ((!TOX_SERVICE_STARTED) || (orma == null))
        {
            Log.i(TAG, "M:STARTUP:init DB");

            try
            {
                String dbs_path = getDir("dbs", MODE_PRIVATE).getAbsolutePath() + "/" + MAIN_DB_NAME;
                // Log.i(TAG, "db:path=" + dbs_path);
                File database_dir = new File(new File(dbs_path).getParent());
                database_dir.mkdirs();
                OrmaDatabase.Builder builder = OrmaDatabase.builder(this);

                if (DB_ENCRYPT)
                {
                    builder = builder.provider(new EncryptedDatabase.Provider(PREF__DB_secrect_key));
                }

                orma = builder.name(dbs_path).
                        readOnMainThread(AccessThreadConstraint.NONE).
                        writeOnMainThread(AccessThreadConstraint.NONE).
                        trace(ORMA_TRACE).
                        build();
                // Log.i(TAG, "db:open=OK:path=" + dbs_path);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Log.i(TAG, "M:STARTUP:init DB:EE1");
                Log.i(TAG, "db:EE1:" + e.getMessage());
                String dbs_path = getDir("dbs", MODE_PRIVATE).getAbsolutePath() + "/" + MAIN_DB_NAME;

                if (DELETE_SQL_AND_VFS_ON_ERROR)
                {
                    try
                    {
                        // Log.i(TAG, "db:deleting database:" + dbs_path);
                        new File(dbs_path).delete();
                    }
                    catch (Exception e3)
                    {
                        e3.printStackTrace();
                        Log.i(TAG, "db:EE3:" + e3.getMessage());
                    }
                }

                // Log.i(TAG, "db:path(2)=" + dbs_path);
                OrmaDatabase.Builder builder = OrmaDatabase.builder(this);

                if (DB_ENCRYPT)
                {
                    builder = builder.provider(new EncryptedDatabase.Provider(PREF__DB_secrect_key));
                }

                try
                {
                    orma = builder.name(dbs_path).
                            readOnMainThread(AccessThreadConstraint.WARNING).
                            writeOnMainThread(AccessThreadConstraint.WARNING).
                            trace(ORMA_TRACE).
                            build();
                }
                catch (Exception e4)
                {
                    Log.i(TAG, "M:STARTUP:init DB:EE4");
                    Log.i(TAG, "db:EE4:" + e4.getMessage());
                    show_wrong_credentials();
                    finish();
                    return;
                }
                // Log.i(TAG, "db:open(2)=OK:path=" + dbs_path);
            }

            // ----- Clear all messages from DB -----
            // ----- Clear all messages from DB -----
            // ----- Clear all messages from DB -----
            // ** // ** // orma.deleteFromMessage().execute();
            // ----- Clear all messages from DB -----
            // ----- Clear all messages from DB -----
            // ----- Clear all messages from DB -----
        }


        try
        {
            if (FriendListHolder.progressDialog != null)
            {
                if (FriendListHolder.progressDialog.isShowing())
                {
                    FriendListHolder.progressDialog.dismiss();
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            if (FriendListHolder.progressDialog != null)
            {
                FriendListHolder.progressDialog = null;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            if (ConferenceListHolder.progressDialog != null)
            {
                if (ConferenceListHolder.progressDialog.isShowing())
                {
                    ConferenceListHolder.progressDialog.dismiss();
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            if (ConferenceListHolder.progressDialog != null)
            {
                ConferenceListHolder.progressDialog = null;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (PREF__window_security)
        {
            // prevent screenshots and also dont show the window content in recent activity screen
            initializeScreenshotSecurity(this);
        }

        //        try
        //        {
        //            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        //        }
        //        catch (Exception e)
        //        {
        //            e.printStackTrace();
        //            Log.i(TAG, "onCreate:setThreadPriority:EE:" + e.getMessage());
        //        }

        Log.i(TAG, "M:STARTUP:getVersionInfo");
        getVersionInfo();

        try
        {
            packageInfo_s = getPackageManager().getPackageInfo(getPackageName(), 0);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        //        if (canceller == null)
        //        {
        //            canceller = new EchoCanceller();
        //        }


        //        try
        //        {
        //            ((Toolbar) getSupportActionBar().getCustomView().getParent()).setContentInsetsAbsolute(0, 0);
        //        }
        //        catch (Exception e)
        //        {
        //            e.printStackTrace();
        //        }
        bootstrapping = false;
        waiting_view = (TextView) findViewById(R.id.waiting_view);
        waiting_image = (ProgressBar) findViewById(R.id.waiting_image);
        normal_container = (ViewGroup) findViewById(R.id.normal_container);
        waiting_view.setVisibility(View.GONE);
        waiting_image.setVisibility(View.GONE);
        normal_container.setVisibility(View.VISIBLE);
        SD_CARD_TMP_DIR = getExternalFilesDir(null).getAbsolutePath() + "/tmpdir/";
        SD_CARD_STATIC_DIR = getExternalFilesDir(null).getAbsolutePath() + "/_staticdir/";
        SD_CARD_FILES_EXPORT_DIR = getExternalFilesDir(null).getAbsolutePath() + "/vfs_export/";
        // Log.i(TAG, "SD_CARD_FILES_EXPORT_DIR:" + SD_CARD_FILES_EXPORT_DIR);
        SD_CARD_TMP_DUMMYFILE = HelperGeneric.make_some_static_dummy_file(this.getBaseContext());
        audio_manager_s = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        Log.i(TAG, "java.library.path:" + System.getProperty("java.library.path"));
        nmn3 = (NotificationManager) context_s.getSystemService(NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
        {
            Log.i(TAG, "M:STARTUP:notification channels");
            String channelName;
            // ---------------------
            channelId_newmessage_sound_and_vibrate = "trifa_new_message_sound_and_vibrate";
            channelName = "New Message Sound and Vibrate";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            notification_channel_newmessage_sound_and_vibrate = new NotificationChannel(
                    channelId_newmessage_sound_and_vibrate, channelName, importance);
            notification_channel_newmessage_sound_and_vibrate.setDescription(channelId_newmessage_sound_and_vibrate);
            notification_channel_newmessage_sound_and_vibrate.enableVibration(true);
            nmn3.createNotificationChannel(notification_channel_newmessage_sound_and_vibrate);
            // ---------------------
            channelId_newmessage_sound = "trifa_new_message_sound";
            channelName = "New Message Sound";
            importance = NotificationManager.IMPORTANCE_DEFAULT;
            notification_channel_newmessage_sound = new NotificationChannel(channelId_newmessage_sound, channelName,
                    importance);
            notification_channel_newmessage_sound.setDescription(channelId_newmessage_sound);
            notification_channel_newmessage_sound.enableVibration(false);
            nmn3.createNotificationChannel(notification_channel_newmessage_sound);
            // ---------------------
            channelId_newmessage_vibrate = "trifa_new_message_vibrate";
            channelName = "New Message Vibrate";
            importance = NotificationManager.IMPORTANCE_DEFAULT;
            notification_channel_newmessage_vibrate = new NotificationChannel(channelId_newmessage_vibrate, channelName,
                    importance);
            notification_channel_newmessage_vibrate.setDescription(channelId_newmessage_vibrate);
            notification_channel_newmessage_vibrate.setSound(null, null);
            notification_channel_newmessage_vibrate.enableVibration(true);
            nmn3.createNotificationChannel(notification_channel_newmessage_vibrate);
            // ---------------------
            channelId_newmessage_silent = "trifa_new_message_silent";
            channelName = "New Message Silent";
            importance = NotificationManager.IMPORTANCE_DEFAULT;
            notification_channel_newmessage_silent = new NotificationChannel(channelId_newmessage_silent, channelName,
                    importance);
            notification_channel_newmessage_silent.setDescription(channelId_newmessage_silent);
            notification_channel_newmessage_silent.setSound(null, null);
            notification_channel_newmessage_silent.enableVibration(false);
            nmn3.createNotificationChannel(notification_channel_newmessage_silent);
            // ---------------------
            channelId_toxservice = "trifa_tox_service";
            channelName = "Tox Service";
            importance = NotificationManager.IMPORTANCE_LOW;
            notification_channel_toxservice = new NotificationChannel(channelId_toxservice, channelName, importance);
            notification_channel_toxservice.setDescription(channelId_toxservice);
            notification_channel_toxservice.setSound(null, null);
            notification_channel_toxservice.enableVibration(false);
            nmn3.createNotificationChannel(notification_channel_toxservice);
        }

        // prefs ----------
        PREF__UV_reversed = settings.getBoolean("video_uv_reversed", true);
        PREF__notification_sound = settings.getBoolean("notifications_new_message_sound", true);
        PREF__notification_vibrate = settings.getBoolean("notifications_new_message_vibrate", false);
        PREF__notification = settings.getBoolean("notifications_new_message", true);
        PREF__software_echo_cancel = settings.getBoolean("software_echo_cancel", false);
        PREF__fps_half = settings.getBoolean("fps_half", false);
        PREF__h264_encoder_use_intra_refresh = settings.getBoolean("h264_encoder_use_intra_refresh", true);
        PREF__U_keep_nospam = settings.getBoolean("U_keep_nospam", false);
        PREF__set_fps = settings.getBoolean("set_fps", false);
        PREF__conference_show_system_messages = settings.getBoolean("conference_show_system_messages", false);
        PREF__X_battery_saving_mode = settings.getBoolean("X_battery_saving_mode", false);
        PREF__X_misc_button_enabled = settings.getBoolean("X_misc_button_enabled", false);
        PREF__local_discovery_enabled = settings.getBoolean("local_discovery_enabled", false);
        PREF__force_udp_only = settings.getBoolean("force_udp_only", false);
        PREF__use_incognito_keyboard = settings.getBoolean("use_incognito_keyboard", true);
        PREF__use_native_audio_play = settings.getBoolean("X_use_native_audio_play", true);

        try
        {
            if (settings.getString("X_battery_saving_timeout", "15").compareTo("15") == 0)
            {
                PREF__X_battery_saving_timeout = 15;
            }
            else
            {
                PREF__X_battery_saving_timeout = Integer.parseInt(settings.getString("X_battery_saving_timeout", "15"));
                Log.i(TAG, "PREF__X_battery_saving_timeout:1:=" + PREF__X_battery_saving_timeout);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__X_battery_saving_timeout = 15;
        }

        boolean tmp1 = settings.getBoolean("udp_enabled", false);

        if (tmp1)
        {
            PREF__udp_enabled = 1;
        }
        else
        {
            PREF__udp_enabled = 0;
        }

        PREF__higher_video_quality = 0;
        GLOBAL_VIDEO_BITRATE = LOWER_GLOBAL_VIDEO_BITRATE;

        try
        {
            PREF__video_call_quality = Integer.parseInt(settings.getString("video_call_quality", "0"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__video_call_quality = 0;
        }


        try
        {
            PREF__higher_audio_quality = Integer.parseInt(settings.getString("higher_audio_quality", "1"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__higher_audio_quality = 1;
        }

        if (PREF__higher_audio_quality == 2)
        {
            GLOBAL_AUDIO_BITRATE = HIGHER_GLOBAL_AUDIO_BITRATE;
        }
        else if (PREF__higher_audio_quality == 1)
        {
            GLOBAL_AUDIO_BITRATE = NORMAL_GLOBAL_AUDIO_BITRATE;
        }
        else
        {
            GLOBAL_AUDIO_BITRATE = LOWER_GLOBAL_AUDIO_BITRATE;
        }

        // ------- access the clipboard -------
        clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        // ------- access the clipboard -------
        PREF__orbot_enabled = false;
        boolean PREF__orbot_enabled__temp = settings.getBoolean("orbot_enabled", false);

        if (PREF__orbot_enabled__temp)
        {
            Log.i(TAG, "M:STARTUP:wait for orbot");
            boolean orbot_installed = OrbotHelper.isOrbotInstalled(this);

            if (orbot_installed)
            {
                boolean orbot_running = orbot_is_really_running; // OrbotHelper.isOrbotRunning(this);
                Log.i(TAG, "waiting_for_orbot_info:orbot_running=" + orbot_running);

                if (orbot_running)
                {
                    PREF__orbot_enabled = true;
                    Log.i(TAG, "waiting_for_orbot_info:F1");
                    HelperGeneric.waiting_for_orbot_info(false);
                    OrbotHelper.get(this).statusTimeout(120 * 1000).
                            addStatusCallback(new StatusCallback()
                            {
                                @Override
                                public void onEnabled(Intent statusIntent)
                                {
                                }

                                @Override
                                public void onStarting()
                                {
                                }

                                @Override
                                public void onStopping()
                                {
                                }

                                @Override
                                public void onDisabled()
                                {
                                    // we got a broadcast with a status of off, so keep waiting
                                }

                                @Override
                                public void onStatusTimeout()
                                {
                                    // throw new RuntimeException("Orbot status request timed out");
                                    Log.i(TAG, "waiting_for_orbot_info:EEO1:" + "Orbot status request timed out");
                                }

                                @Override
                                public void onNotYetInstalled()
                                {
                                }
                            }).
                            init(); // allow 60 seconds to connect to Orbot
                }
                else
                {
                    orbot_is_really_running = false;

                    if (OrbotHelper.requestStartTor(this))
                    {
                        PREF__orbot_enabled = true;
                        Log.i(TAG, "waiting_for_orbot_info:*T2");
                        HelperGeneric.waiting_for_orbot_info(true);
                    }
                    else
                    {
                        // should never get here
                        Log.i(TAG, "waiting_for_orbot_info:F3");
                        HelperGeneric.waiting_for_orbot_info(false);
                    }

                    OrbotHelper.get(this).statusTimeout(120 * 1000).
                            addStatusCallback(new StatusCallback()
                            {
                                @Override
                                public void onEnabled(Intent statusIntent)
                                {
                                }

                                @Override
                                public void onStarting()
                                {
                                }

                                @Override
                                public void onStopping()
                                {
                                }

                                @Override
                                public void onDisabled()
                                {
                                    // we got a broadcast with a status of off, so keep waiting
                                }

                                @Override
                                public void onStatusTimeout()
                                {
                                    // throw new RuntimeException("Orbot status request timed out");
                                    Log.i(TAG, "waiting_for_orbot_info:EEO2:" + "Orbot status request timed out");
                                }

                                @Override
                                public void onNotYetInstalled()
                                {
                                }
                            }).
                            init(); // allow 60 seconds to connect to Orbot
                }
            }
            else
            {
                Log.i(TAG, "waiting_for_orbot_info:F4");
                HelperGeneric.waiting_for_orbot_info(false);
                Intent orbot_get = OrbotHelper.getOrbotInstallIntent(this);

                try
                {
                    startActivity(orbot_get);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
        else
        {
            Log.i(TAG, "waiting_for_orbot_info:F5");
            HelperGeneric.waiting_for_orbot_info(false);
        }

        Log.i(TAG, "PREF__UV_reversed:2=" + PREF__UV_reversed);
        Log.i(TAG, "PREF__notification_sound:2=" + PREF__notification_sound);
        Log.i(TAG, "PREF__notification_vibrate:2=" + PREF__notification_vibrate);

        try
        {
            if (settings.getString("min_audio_samplingrate_out", "8000").compareTo("Auto") == 0)
            {
                PREF__min_audio_samplingrate_out = 8000;
            }
            else
            {
                PREF__min_audio_samplingrate_out = Integer.parseInt(
                        settings.getString("min_audio_samplingrate_out", "" + MIN_AUDIO_SAMPLINGRATE_OUT));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__min_audio_samplingrate_out = MIN_AUDIO_SAMPLINGRATE_OUT;
        }

        // ------- FIXED -------
        PREF__min_audio_samplingrate_out = SAMPLE_RATE_FIXED;
        // ------- FIXED -------

        try
        {
            PREF__allow_screen_off_in_audio_call = settings.getBoolean("allow_screen_off_in_audio_call", true);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__allow_screen_off_in_audio_call = true;
        }

        try
        {
            PREF__auto_accept_image = settings.getBoolean("auto_accept_image", true);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__auto_accept_image = true;
        }

        try
        {
            PREF__auto_accept_video = settings.getBoolean("auto_accept_video", false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__auto_accept_video = false;
        }

        try
        {
            PREF__auto_accept_all_upto = settings.getBoolean("auto_accept_all_upto", false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__auto_accept_all_upto = false;
        }

        try
        {
            PREF__X_zoom_incoming_video = settings.getBoolean("X_zoom_incoming_video", false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__X_zoom_incoming_video = false;
        }

        try
        {
            PREF__X_audio_recording_frame_size = Integer.parseInt(
                    settings.getString("X_audio_recording_frame_size", "" + 40));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__X_audio_recording_frame_size = 40;
        }

        // ------- FIXED -------
        PREF__X_audio_recording_frame_size = FRAME_SIZE_FIXED;
        // ------- FIXED -------

        try
        {
            PREF__video_cam_resolution = Integer.parseInt(settings.getString("video_cam_resolution", "" + 0));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__video_cam_resolution = 0;
        }

        try
        {
            PREF__global_font_size = Integer.parseInt(settings.getString("global_font_size", "" + 2));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__global_font_size = 2;
        }

        PREF__camera_get_preview_format = settings.getString("camera_get_preview_format", "YV12");

        // prefs ----------

        // TODO: remake this into something nicer ----------
        top_imageview = (ImageView) this.findViewById(R.id.main_maintopimage);
        top_imageview.setVisibility(View.GONE);

        if (PREF__U_keep_nospam == true)
        {
            top_imageview.setBackgroundColor(Color.TRANSPARENT);
            // top_imageview.setBackgroundColor(Color.parseColor("#C62828"));
            final Drawable d1 = new IconicsDrawable(this).
                    icon(FontAwesome.Icon.faw_exclamation_circle).
                    paddingDp(20).
                    color(getResources().getColor(R.color.md_red_600)).
                    sizeDp(100);
            top_imageview.setImageDrawable(d1);
        }
        else
        {
            top_imageview.setBackgroundColor(Color.TRANSPARENT);
            top_imageview.setImageResource(R.drawable.web_hi_res_512);
        }

        fadeInAndShowImage(top_imageview, 5000);
        fadeOutAndHideImage(mt, 4000);
        // TODO: remake this into something nicer ----------
        // --------- status spinner ---------
        spinner_own_status = (Spinner) findViewById(R.id.spinner_own_status);
        ArrayList<String> own_online_status_string_values = new ArrayList<String>(
                Arrays.asList(getString(R.string.MyMainActivity_available), getString(R.string.MyMainActivity_away),
                        getString(R.string.MyMainActivity_busy)));
        ArrayAdapter<String> myAdapter = new OwnStatusSpinnerAdapter(this, R.layout.own_status_spinner_item,
                own_online_status_string_values);

        if (spinner_own_status != null)
        {
            spinner_own_status.setAdapter(myAdapter);
            spinner_own_status.setSelection(global_tox_self_status);
            spinner_own_status.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View v, int position, long id)
                {
                    if (is_tox_started)
                    {
                        try
                        {
                            if (id == 0)
                            {
                                // status: available
                                tox_self_set_status(TOX_USER_STATUS_NONE.value);
                                global_tox_self_status = TOX_USER_STATUS_NONE.value;
                            }
                            else if (id == 1)
                            {
                                // status: away
                                tox_self_set_status(TOX_USER_STATUS_AWAY.value);
                                global_tox_self_status = TOX_USER_STATUS_AWAY.value;
                            }
                            else if (id == 2)
                            {
                                // status: busy
                                tox_self_set_status(TOX_USER_STATUS_BUSY.value);
                                global_tox_self_status = TOX_USER_STATUS_BUSY.value;
                            }
                        }
                        catch (Exception e2)
                        {
                            e2.printStackTrace();
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView)
                {
                    // your code here
                }
            });
        }

        // --------- status spinner ---------
        // get permission ----------
        Log.i(TAG, "M:STARTUP:permissions");
        MyMainActivityPermissionsDispatcher.dummyForPermissions001WithPermissionCheck(this);
        // get permission ----------
        // -------- drawer ------------
        // -------- drawer ------------
        // -------- drawer ------------
        Log.i(TAG, "M:STARTUP:drawer");
        PrimaryDrawerItem item1 = new PrimaryDrawerItem().withIdentifier(1).withName(
                R.string.MyMainActivity_profile).withIcon(GoogleMaterial.Icon.gmd_face);
        PrimaryDrawerItem item2 = new PrimaryDrawerItem().withIdentifier(2).withName(
                R.string.MyMainActivity_settings).withIcon(GoogleMaterial.Icon.gmd_settings);
        PrimaryDrawerItem item3 = new PrimaryDrawerItem().withIdentifier(3).withName(
                R.string.MyMainActivity_logout_login).withIcon(GoogleMaterial.Icon.gmd_refresh);
        PrimaryDrawerItem item4 = new PrimaryDrawerItem().withIdentifier(4).withName(
                R.string.MyMainActivity_maint).withIcon(GoogleMaterial.Icon.gmd_build);
        PrimaryDrawerItem item5 = new PrimaryDrawerItem().withIdentifier(5).withName(
                R.string.MyMainActivity_about).withIcon(GoogleMaterial.Icon.gmd_info);
        PrimaryDrawerItem item6 = new PrimaryDrawerItem().withIdentifier(6).withName(
                R.string.MyMainActivity_exit).withIcon(GoogleMaterial.Icon.gmd_exit_to_app);
        final Drawable d1 = new IconicsDrawable(this).icon(FontAwesome.Icon.faw_lock).
                color(getResources().getColor(R.color.colorPrimaryDark)).sizeDp(100);
        profile_d_item = new ProfileDrawerItem().
                withName("me").
                withIcon(d1);
        // Create the AccountHeader
        main_drawer_header = new AccountHeaderBuilder().
                withSelectionListEnabledForSingleProfile(false).
                withActivity(this).
                withCompactStyle(true).
                addProfiles(profile_d_item).
                withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener()
                {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile)
                    {
                        return false;
                    }
                }).build();
        // create the drawer and remember the `Drawer` result object
        main_drawer = new DrawerBuilder().
                withActivity(this).
                withInnerShadow(false).
                withRootView(R.id.drawer_container).
                withShowDrawerOnFirstLaunch(false).
                withActionBarDrawerToggleAnimated(true).
                withActionBarDrawerToggle(true).
                withToolbar(toolbar).
                addDrawerItems(item1, new DividerDrawerItem(), item2, item3, item4, item5, new DividerDrawerItem(),
                        item6).
                withTranslucentStatusBar(false).
                withAccountHeader(main_drawer_header).
                withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener()
                {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem)
                    {
                        Log.i(TAG, "drawer:item=" + position);

                        if (position == 1)
                        {
                            // profile
                            try
                            {
                                if (Callstate.state == 0)
                                {
                                    Log.i(TAG, "start profile activity");
                                    Intent intent = new Intent(context_s, ProfileActivity.class);
                                    startActivityForResult(intent, ProfileActivity_ID);
                                }
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                        else if (position == 3)
                        {
                            // settings
                            try
                            {
                                if (Callstate.state == 0)
                                {
                                    Log.i(TAG, "start settings activity");
                                    Intent intent = new Intent(context_s, SettingsActivity.class);
                                    startActivityForResult(intent, SettingsActivity_ID);
                                }
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                        else if (position == 4)
                        {
                            // logout/login
                            try
                            {
                                if (is_tox_started)
                                {
                                    if(tox_mainactivity_fg != null) {
                                        tox_mainactivity_fg.global_stop_tox();
                                    }
                                }
                                else
                                {
                                    if(tox_mainactivity_fg != null) {
                                        tox_mainactivity_fg.global_start_tox();
                                    }
                                }
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                        else if (position == 6)
                        {
                            // About
                            try
                            {
                                Log.i(TAG, "start aboutpage activity");
                                Intent intent = new Intent(context_s, Aboutpage.class);
                                startActivityForResult(intent, AboutpageActivity_ID);
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                        else if (position == 5)
                        {
                            // Maintenance
                            try
                            {
                                Log.i(TAG, "start Maintenance activity");
                                Intent intent = new Intent(context_s, MaintenanceActivity.class);
                                startActivityForResult(intent, MaintenanceActivity_ID);
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }

                            // -- clear Glide cache --
                            // -- clear Glide cache --
                            // clearCache();
                            // -- clear Glide cache --
                            // -- clear Glide cache --
                        }
                        else if (position == 8)
                        {
                            // Exit
                            try
                            {
                                GroupAudioService.stop_me();
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }

                            try
                            {
                                if (is_tox_started)
                                {
                                    tox_service_fg.stop_tox_fg(true);
                                    tox_service_fg.stop_me(true);
                                }
                                else
                                {
                                    // just exit
                                    tox_service_fg.stop_me(true);
                                }
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }

                        return true;
                    }
                }).build();
        //        DrawerLayout drawer_layout = (DrawerLayout) findViewById(R.id.material_drawer_layout);
        //        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.faw_envelope_open, R.string.faw_envelope_open);
        //
        //        drawer_layout.setDrawerListener(drawerToggle);
        //
        //        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //        getSupportActionBar().setHomeButtonEnabled(true);
        //        drawerToggle.syncState();
        // show hambuger icon -------
        // getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        // main_drawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(true);
        // show back icon -------
        // main_drawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(false);
        // getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // -------- drawer ------------
        // -------- drawer ------------
        // -------- drawer ------------
        // reset calling state
        Callstate.state = 0;
        Callstate.tox_call_state = ToxVars.TOXAV_FRIEND_CALL_STATE.TOXAV_FRIEND_CALL_STATE_NONE.value;
        Callstate.call_first_video_frame_received = -1;
        Callstate.call_first_audio_frame_received = -1;
        VIDEO_FRAME_RATE_OUTGOING = 0;
        last_video_frame_sent = -1;
        VIDEO_FRAME_RATE_INCOMING = 0;
        last_video_frame_received = -1;
        count_video_frame_received = 0;
        count_video_frame_sent = 0;
        Callstate.friend_pubkey = "-1";
        Callstate.audio_speaker = true;
        Callstate.other_audio_enabled = 1;
        Callstate.other_video_enabled = 1;
        Callstate.my_audio_enabled = 1;
        Callstate.my_video_enabled = 1;

        String native_api = getNativeLibAPI();
        mt.setText(mt.getText() + "\n" + native_api);
        mt.setText(mt.getText() + "\n" + "c-toxcore:v" + tox_version_major() + "." + tox_version_minor() + "." +
                tox_version_patch());
        mt.setText(mt.getText() + ", " + "jni-c-toxcore:v" + jnictoxcore_version());
        Log.i(TAG, "loaded:c-toxcore:v" + tox_version_major() + "." + tox_version_minor() + "." + tox_version_patch());
        Log.i(TAG, "loaded:jni-c-toxcore:v" + jnictoxcore_version());

        if ((!TOX_SERVICE_STARTED) || (vfs == null))
        {
            Log.i(TAG, "M:STARTUP:init VFS");

            if (VFS_ENCRYPT)
            {
                try
                {
                    String dbFile = getDir("vfs", MODE_PRIVATE).getAbsolutePath() + "/" + MAIN_VFS_NAME;
                    File database_dir = new File(new File(dbFile).getParent());
                    database_dir.mkdirs();
                    // Log.i(TAG, "vfs:path=" + dbFile);
                    vfs = VirtualFileSystem.get();

                    try
                    {
                        if (!vfs.isMounted())
                        {
                            Log.i(TAG, "VFS:mount:[1]:start:" + Thread.currentThread().getId() + ":" +
                                    Thread.currentThread().getName());
                            vfs.mount(dbFile, PREF__DB_secrect_key);
                            Log.i(TAG, "VFS:mount:[1]:end");
                        }
                    }
                    catch (Exception ee)
                    {
                        Log.i(TAG, "vfs:EE1:" + ee.getMessage());
                        ee.printStackTrace();
                        Log.i(TAG, "VFS:mount:[2]:start:" + Thread.currentThread().getId() + ":" +
                                Thread.currentThread().getName());
                        vfs.mount(dbFile, PREF__DB_secrect_key);
                        Log.i(TAG, "VFS:mount:[2]:end");
                    }

                    // Log.i(TAG, "vfs:open(1)=OK:path=" + dbFile);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    Log.i(TAG, "vfs:EE2:" + e.getMessage());
                    String dbFile = getDir("vfs", MODE_PRIVATE).getAbsolutePath() + "/" + MAIN_VFS_NAME;

                    if (DELETE_SQL_AND_VFS_ON_ERROR)
                    {
                        try
                        {
                            Log.i(TAG, "vfs:**deleting database**:" + dbFile);
                            Log.i(TAG, "vfs:**deleting database**:" + dbFile);
                            Log.i(TAG, "vfs:**deleting database**:" + dbFile);
                            Log.i(TAG, "vfs:**deleting database**:" + dbFile);
                            Log.i(TAG, "vfs:**deleting database**:" + dbFile);
                            Log.i(TAG, "vfs:**deleting database**--------:" + dbFile);
                            new File(dbFile).delete();
                            Log.i(TAG, "vfs:**deleting database**--------:" + dbFile);
                            Log.i(TAG, "vfs:**deleting database**:" + dbFile);
                            Log.i(TAG, "vfs:**deleting database**:" + dbFile);
                            Log.i(TAG, "vfs:**deleting database**:" + dbFile);
                            Log.i(TAG, "vfs:**deleting database**:" + dbFile);
                            Log.i(TAG, "vfs:**deleting database**:" + dbFile);
                        }
                        catch (Exception e3)
                        {
                            e3.printStackTrace();
                            Log.i(TAG, "vfs:EE3:" + e3.getMessage());
                        }
                    }

                    try
                    {
                        // Log.i(TAG, "vfs:path=" + dbFile);
                        vfs = VirtualFileSystem.get();
                        vfs.createNewContainer(dbFile, PREF__DB_secrect_key);
                        Log.i(TAG, "VFS:mount:[3]:start:" + Thread.currentThread().getId() + ":" +
                                Thread.currentThread().getName());
                        vfs.mount(PREF__DB_secrect_key);
                        Log.i(TAG, "VFS:mount:[3]:end");
                        // Log.i(TAG, "vfs:open(2)=OK:path=" + dbFile);
                    }
                    catch (Exception e2)
                    {
                        e2.printStackTrace();
                        Log.i(TAG, "vfs:EE4:" + e.getMessage());
                    }
                }

                // Log.i(TAG, "vfs:encrypted:(1)prefix=" + VFS_PREFIX);
            }
            else
            {
                // VFS not encrypted -------------
                VFS_PREFIX = getExternalFilesDir(null).getAbsolutePath() + "/vfs/";
                // Log.i(TAG, "vfs:not_encrypted:(2)prefix=" + VFS_PREFIX);
                // VFS not encrypted -------------
            }
        }

        // cleanup temp dirs --------
        if (!TOX_SERVICE_STARTED)
        {
            Log.i(TAG, "M:STARTUP:cleanup_temp_dirs (background)");
            HelperGeneric.cleanup_temp_dirs();
        }

        // cleanup temp dirs --------
        // ---------- DEBUG, just a test ----------
        // ---------- DEBUG, just a test ----------
        // ---------- DEBUG, just a test ----------
        //        if (VFS_ENCRYPT)
        //        {
        //            if (vfs.isMounted())
        //            {
        //                vfs_listFilesAndFilesSubDirectories("/", 0, "");
        //            }
        //        }
        //        // ---------- DEBUG, just a test ----------
        //        // ---------- DEBUG, just a test ----------
        //        // ---------- DEBUG, just a test ----------
        app_files_directory = getFilesDir().getAbsolutePath();
        // --- forground service ---
        // --- forground service ---
        // --- forground service ---
//        Intent i = new Intent(this, TrifaToxService.class);
//
//        if (!TOX_SERVICE_STARTED)
//        {
//            Log.i(TAG, "M:STARTUP:start ToxService");
//            Log.i(TAG, "set_all_conferences_inactive:005");
//            HelperConference.set_all_conferences_inactive();
//            startService(i);
//        }

        Intent tox_i = new Intent(this, MainActivity.class);
        if (!TOX_SERVICE_STARTED)
        {
            Log.i(TAG, "M:STARTUP:start ToxThread");

            startService(tox_i);
//            tox_thread_start();
        }

        // --- forground service ---
        // --- forground service ---
        // --- forground service ---
        receiverFilter1 = new IntentFilter(AudioManager.ACTION_HEADSET_PLUG);
        receiver1 = new HeadsetStateReceiver();
        registerReceiver(receiver1, receiverFilter1);
        receiverFilter2 = new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        receiver2 = new HeadsetStateReceiver();
        registerReceiver(receiver2, receiverFilter2);
        // --
        receiverFilter3 = new IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        receiver3 = new HeadsetStateReceiver();
        registerReceiver(receiver3, receiverFilter3);
        // --
        receiverFilter4 = new IntentFilter(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
        receiver4 = new HeadsetStateReceiver();
        registerReceiver(receiver4, receiverFilter4);
        // --
        MainActivity.set_av_call_status(Callstate.state);

        Log.i(TAG, "M:STARTUP:-- DONE --");
    }

    public void vfs_listFilesAndFilesSubDirectories(String directoryName, int depth, String parent)
    {
        if (VFS_ENCRYPT)
        {
            info.guardianproject.iocipher.File directory1 = new info.guardianproject.iocipher.File(directoryName);
            info.guardianproject.iocipher.File[] fList1 = directory1.listFiles();

            for (info.guardianproject.iocipher.File file : fList1)
            {
                if (file.isFile())
                {
                    // final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    // final String human_datetime = df.format(new Date(file.lastModified()));
                    Log.i(TAG, "VFS:f:" + parent + "/" + file.getName() + " bytes=" + file.length());
                }
                else if (file.isDirectory())
                {
                    Log.i(TAG, "VFS:d:" + parent + "/" + file.getName() + "/");
                    vfs_listFilesAndFilesSubDirectories(file.getAbsolutePath(), depth + 1,
                            parent + "/" + file.getName());
                }
            }
        }
        else
        {
            java.io.File directory1 = new java.io.File(directoryName);
            java.io.File[] fList1 = directory1.listFiles();

            for (File file : fList1)
            {
                if (file.isFile())
                {
                    // final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    // final String human_datetime = df.format(new Date(file.lastModified()));
                    Log.i(TAG, "VFS:f:" + parent + "/" + file.getName() + " bytes=" + file.length());
                }
                else if (file.isDirectory())
                {
                    Log.i(TAG, "VFS:d:" + parent + "/" + file.getName() + "/");
                    vfs_listFilesAndFilesSubDirectories(file.getAbsolutePath(), depth + 1,
                            parent + "/" + file.getName());
                }
            }
        }
    }


    // ------- for runtime permissions -------
    // ------- for runtime permissions -------
    // ------- for runtime permissions -------
    @NeedsPermission({Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA})
    void dummyForPermissions001()
    {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        MyMainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }
    // ------- for runtime permissions -------
    // ------- for runtime permissions -------
    // ------- for runtime permissions -------

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        try
        {
            unregisterReceiver(receiver1);
        }
        catch (Exception e)
        {
        }

        try
        {
            unregisterReceiver(receiver2);
        }
        catch (Exception e)
        {
        }

        try
        {
            unregisterReceiver(receiver3);
        }
        catch (Exception e)
        {
        }

        try
        {
            unregisterReceiver(receiver4);
        }
        catch (Exception e)
        {
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        // just in case, update own activity pointer!
        main_activity_s = this;
    }

    @Override
    protected void onPause()
    {
        Log.i(TAG, "onPause");
        super.onPause();
        MyMainActivity.friend_list_fragment = null;
    }

    @Override
    protected void onResume()
    {
        Log.i(TAG, "onResume");
        super.onResume();
        // prefs ----------
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        PREF__UV_reversed = settings.getBoolean("video_uv_reversed", true);
        PREF__notification_sound = settings.getBoolean("notifications_new_message_sound", true);
        PREF__notification_vibrate = settings.getBoolean("notifications_new_message_vibrate", true);
        PREF__notification = settings.getBoolean("notifications_new_message", true);
        PREF__software_echo_cancel = settings.getBoolean("software_echo_cancel", false);
        PREF__fps_half = settings.getBoolean("fps_half", false);
        PREF__h264_encoder_use_intra_refresh = settings.getBoolean("h264_encoder_use_intra_refresh", true);
        PREF__U_keep_nospam = settings.getBoolean("U_keep_nospam", false);
        PREF__set_fps = settings.getBoolean("set_fps", false);
        PREF__conference_show_system_messages = settings.getBoolean("conference_show_system_messages", false);
        PREF__X_battery_saving_mode = settings.getBoolean("X_battery_saving_mode", false);
        PREF__X_misc_button_enabled = settings.getBoolean("X_misc_button_enabled", false);
        PREF__local_discovery_enabled = settings.getBoolean("local_discovery_enabled", false);
        PREF__force_udp_only = settings.getBoolean("force_udp_only", false);
        PREF__use_incognito_keyboard = settings.getBoolean("use_incognito_keyboard", true);
        PREF__use_native_audio_play = settings.getBoolean("X_use_native_audio_play", true);
        PREF__tox_set_do_not_sync_av = settings.getBoolean("X_tox_set_do_not_sync_av", false);

        try
        {
            if (settings.getString("X_battery_saving_timeout", "15").compareTo("15") == 0)
            {
                PREF__X_battery_saving_timeout = 15;
            }
            else
            {
                PREF__X_battery_saving_timeout = Integer.parseInt(settings.getString("X_battery_saving_timeout", "15"));
                Log.i(TAG, "PREF__X_battery_saving_timeout:2:=" + PREF__X_battery_saving_timeout);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__X_battery_saving_timeout = 15;
        }

        try
        {
            PREF__X_eac_delay_ms = Integer.parseInt(settings.getString("X_eac_delay_ms_2", "80"));
        }
        catch (Exception e)
        {
            PREF__X_eac_delay_ms = 80;
            e.printStackTrace();
        }

        try
        {
            PREF__X_audio_play_buffer_custom = Integer.parseInt(settings.getString("X_audio_play_buffer_custom", "0"));
        }
        catch (Exception e)
        {
            PREF__X_audio_play_buffer_custom = 0;
            e.printStackTrace();
        }


        try
        {
            PREF_mic_gain_factor = (float) (settings.getInt("mic_gain_factor", 1));
            Log.i(TAG, "PREF_mic_gain_factor:1=" + PREF_mic_gain_factor);
            PREF_mic_gain_factor = PREF_mic_gain_factor + 1.0f;

            if (PREF_mic_gain_factor < 1.0f)
            {
                PREF_mic_gain_factor = 1.0f;
            }
            else if (PREF_mic_gain_factor > 30.0f)
            {
                PREF_mic_gain_factor = 30.0f;
            }
            Log.i(TAG, "PREF_mic_gain_factor:2=" + PREF_mic_gain_factor);
        }
        catch (Exception e)
        {
            PREF_mic_gain_factor = 2.0f;
            Log.i(TAG, "PREF_mic_gain_factor:E=" + PREF_mic_gain_factor);
            e.printStackTrace();
        }

        if (PREF__U_keep_nospam == true)
        {
            top_imageview.setBackgroundColor(Color.TRANSPARENT);
            // top_imageview.setBackgroundColor(Color.parseColor("#C62828"));
            final Drawable d1 = new IconicsDrawable(this).
                    icon(FontAwesome.Icon.faw_exclamation_circle).
                    paddingDp(20).
                    color(getResources().getColor(R.color.md_red_600)).
                    sizeDp(100);
            top_imageview.setImageDrawable(d1);
        }
        else
        {
            top_imageview.setBackgroundColor(Color.TRANSPARENT);
            top_imageview.setImageResource(R.drawable.web_hi_res_512);
        }

        boolean tmp1 = settings.getBoolean("udp_enabled", false);

        if (tmp1)
        {
            PREF__udp_enabled = 1;
        }
        else
        {
            PREF__udp_enabled = 0;
        }

        PREF__higher_video_quality = 0;
        GLOBAL_VIDEO_BITRATE = LOWER_GLOBAL_VIDEO_BITRATE;

        try
        {
            PREF__video_call_quality = Integer.parseInt(settings.getString("video_call_quality", "0"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__video_call_quality = 0;
        }

        try
        {
            PREF__higher_audio_quality = Integer.parseInt(settings.getString("higher_audio_quality", "1"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__higher_audio_quality = 1;
        }

        if (PREF__higher_audio_quality == 2)
        {
            GLOBAL_AUDIO_BITRATE = HIGHER_GLOBAL_AUDIO_BITRATE;
        }
        else if (PREF__higher_audio_quality == 1)
        {
            GLOBAL_AUDIO_BITRATE = NORMAL_GLOBAL_AUDIO_BITRATE;
        }
        else
        {
            GLOBAL_AUDIO_BITRATE = LOWER_GLOBAL_AUDIO_BITRATE;
        }

        try
        {
            if (settings.getString("min_audio_samplingrate_out", "8000").compareTo("Auto") == 0)
            {
                PREF__min_audio_samplingrate_out = 8000;
            }
            else
            {
                PREF__min_audio_samplingrate_out = Integer.parseInt(
                        settings.getString("min_audio_samplingrate_out", "" + MIN_AUDIO_SAMPLINGRATE_OUT));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__min_audio_samplingrate_out = MIN_AUDIO_SAMPLINGRATE_OUT;
        }

        // ------- FIXED -------
        PREF__min_audio_samplingrate_out = SAMPLE_RATE_FIXED;
        // ------- FIXED -------


        Log.i(TAG, "PREF__UV_reversed:2=" + PREF__UV_reversed);
        Log.i(TAG, "PREF__min_audio_samplingrate_out:2=" + PREF__min_audio_samplingrate_out);

        try
        {
            PREF__allow_screen_off_in_audio_call = settings.getBoolean("allow_screen_off_in_audio_call", true);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__allow_screen_off_in_audio_call = true;
        }

        try
        {
            PREF__auto_accept_image = settings.getBoolean("auto_accept_image", true);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__auto_accept_image = true;
        }

        try
        {
            PREF__auto_accept_video = settings.getBoolean("auto_accept_video", false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__auto_accept_video = false;
        }

        try
        {
            PREF__auto_accept_all_upto = settings.getBoolean("auto_accept_all_upto", false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__auto_accept_all_upto = false;
        }

        try
        {
            PREF__X_zoom_incoming_video = settings.getBoolean("X_zoom_incoming_video", false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__X_zoom_incoming_video = false;
        }

        try
        {
            PREF__X_audio_recording_frame_size = Integer.parseInt(
                    settings.getString("X_audio_recording_frame_size", "" + 40));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__X_audio_recording_frame_size = 40;
        }

        // ------- FIXED -------
        PREF__X_audio_recording_frame_size = FRAME_SIZE_FIXED;
        // ------- FIXED -------

        try
        {
            PREF__video_cam_resolution = Integer.parseInt(settings.getString("video_cam_resolution", "" + 0));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__video_cam_resolution = 0;
        }

        try
        {
            PREF__global_font_size = Integer.parseInt(settings.getString("global_font_size", "" + 2));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__global_font_size = 2;
        }

        PREF__camera_get_preview_format = settings.getString("camera_get_preview_format", "YV12");

        // prefs ----------

        try
        {
            profile_d_item.withIcon(
                    HelperGeneric.get_drawable_from_vfs_image(HelperGeneric.get_vfs_image_filename_own_avatar()));
            main_drawer_header.updateProfile(profile_d_item);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.i(TAG, "onResume:EE1:" + e.getMessage());

            try
            {
                final Drawable d1 = new IconicsDrawable(this).icon(FontAwesome.Icon.faw_lock).color(
                        getResources().getColor(R.color.colorPrimaryDark)).sizeDp(50);
                profile_d_item.withIcon(d1);
                main_drawer_header.updateProfile(profile_d_item);
            }
            catch (Exception e2)
            {
                Log.i(TAG, "onResume:EE2:" + e2.getMessage());
                e2.printStackTrace();
            }
        }

        spinner_own_status.setSelection(global_tox_self_status);
        // just in case, update own activity pointer!
        main_activity_s = this;

        try
        {
            // ask user to whitelist app from DozeMode/BatteryOptimizations
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                SharedPreferences settings2 = PreferenceManager.getDefaultSharedPreferences(this);
                boolean asked_for_whitelist_doze_already = settings2.getBoolean("asked_whitelist_doze", false);

                if (!asked_for_whitelist_doze_already)
                {
                    settings2.edit().putBoolean("asked_whitelist_doze", true).commit();
                    final Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    ResolveInfo resolve_activity = getPackageManager().resolveActivity(intent, 0);

                    if (resolve_activity != null)
                    {
                        AlertDialog ad = new AlertDialog.Builder(this).
                                setNegativeButton(R.string.MyMainActivity_no_button, new DialogInterface.OnClickListener()
                                {
                                    public void onClick(DialogInterface dialog, int id)
                                    {
                                        return;
                                    }
                                }).
                                setPositiveButton(R.string.MyMainActivity_ok_take_me_there_button,
                                        new DialogInterface.OnClickListener()
                                        {
                                            public void onClick(DialogInterface dialog, int id)
                                            {
                                                startActivity(intent);
                                            }
                                        }).create();
                        ad.setTitle(getString(R.string.MyMainActivity_info_dialog_title));
                        ad.setMessage(getString(R.string.MyMainActivity_add_to_batt_opt));
                        ad.setCancelable(false);
                        ad.setCanceledOnTouchOutside(false);
                        ad.show();
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // ACK new Notification token --------------------------
        if (get_g_opts(NOTIFICATION_TOKEN_DB_KEY_NEED_ACK) != null)
        {
            // ok we have a new token, show the user a dialog to ask if we should use it
            AlertDialog ad = new AlertDialog.Builder(this).
                    setNegativeButton(R.string.MyMainActivity_no_button, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            try
                            {
                                del_g_opts(NOTIFICATION_TOKEN_DB_KEY_NEED_ACK);
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                                Log.i(TAG, "del_NOTIFICATION_TOKEN_DB_KEY_NEED_ACK:EE01:" + e.getMessage());
                            }
                        }
                    }).
                    setPositiveButton(R.string.MyMainActivity_yes_button, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            try
                            {
                                String new_token = get_g_opts(NOTIFICATION_TOKEN_DB_KEY_NEED_ACK);
                                set_g_opts(NOTIFICATION_TOKEN_DB_KEY, new_token);
                                del_g_opts(NOTIFICATION_TOKEN_DB_KEY_NEED_ACK);
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                                Log.i(TAG, "update_NOTIFICATION_TOKEN_DB_KEY_NEED_ACK:EE01:" + e.getMessage());
                            }
                        }
                    }).create();
            ad.setTitle(getString(R.string.MyMainActivity_new_noti_token_dialog_title));
            ad.setMessage(getString(R.string.MyMainActivity_new_noti_token_dialog_text));
            ad.setCancelable(false);
            ad.setCanceledOnTouchOutside(false);
            ad.show();
        }
        // ACK new Notification token --------------------------
    }

    @Override
    protected void onNewIntent(Intent i)
    {
        Log.i(TAG, "onNewIntent:i=" + i);
        super.onNewIntent(i);
    }

    @Override
    public void onBackPressed()
    {
        if (main_drawer.isDrawerOpen())
        {
            main_drawer.closeDrawer();
        }
        else
        {
            super.onBackPressed();
        }
    }

    static
    {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    public void show_add_friend(View view)
    {
        Intent intent = new Intent(this, AddFriendActivity.class);
        // intent.putExtra("key", value);
        startActivityForResult(intent, AddFriendActivity_ID);
    }

    public void show_wrong_credentials()
    {
        Intent intent = new Intent(this, WrongCredentials.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AddFriendActivity_ID)
        {
            if (resultCode == RESULT_OK)
            {
                String friend_tox_id1 = data.getStringExtra("toxid");
                String friend_tox_id = "";
                friend_tox_id = friend_tox_id1.toUpperCase().replace(" ", "").replaceFirst("tox:", "").replaceFirst(
                        "TOX:", "").replaceFirst("Tox:", "");
                HelperFriend.add_friend_real(friend_tox_id);
            }
            else
            {
                // (resultCode == RESULT_CANCELED)
            }
        }
    }


    void sendEmailWithAttachment(Context c, final String recipient, final String subject, final String message, final String full_file_name, final String full_file_name_suppl)
    {
        try
        {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", recipient, null));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            ArrayList<Uri> uris = new ArrayList<>();
            uris.add(Uri.parse("file://" + full_file_name));
            Log.i(TAG, "email:full_file_name=" + full_file_name);
            File ff = new File(full_file_name);
            Log.i(TAG, "email:full_file_name exists:" + ff.exists());

            try
            {
                if (new File(full_file_name_suppl).length() > 0)
                {
                    uris.add(Uri.parse("file://" + full_file_name_suppl));
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Log.i(TAG, "email:EE1:" + e.getMessage());
            }

            List<ResolveInfo> resolveInfos = getPackageManager().queryIntentActivities(emailIntent, 0);
            List<LabeledIntent> intents = new ArrayList<>();

            if (resolveInfos.size() != 0)
            {
                for (ResolveInfo info : resolveInfos)
                {
                    Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                    Log.i(TAG, "email:" + "comp=" + info.activityInfo.packageName + " " + info.activityInfo.name);
                    intent.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{recipient});

                    if (subject != null)
                    {
                        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
                    }

                    if (message != null)
                    {
                        intent.putExtra(Intent.EXTRA_TEXT, message);
                        // ArrayList<String> extra_text = new ArrayList<String>();
                        // extra_text.add(message);
                        // intent.putStringArrayListExtra(android.content.Intent.EXTRA_TEXT, extra_text);
                        // Log.i(TAG, "email:" + "message=" + message);
                        // Log.i(TAG, "email:" + "intent extra_text=" + extra_text);
                    }

                    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                    intents.add(new LabeledIntent(intent, info.activityInfo.packageName,
                            info.loadLabel(getPackageManager()), info.icon));
                }

                try
                {
                    Intent chooser = Intent.createChooser(intents.remove(intents.size() - 1),
                            "Send email with attachments");
                    chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toArray(new LabeledIntent[intents.size()]));
                    startActivity(chooser);
                }
                catch (Exception email_app)
                {
                    email_app.printStackTrace();
                    Log.i(TAG, "email:" + "Error starting Email App");
                    new AlertDialog.Builder(c).setMessage(
                            R.string.MyMainActivity_error_starting_email_app).setPositiveButton(
                            R.string.MyMainActivity_button_ok, null).show();
                }
            }
            else
            {
                Log.i(TAG, "email:" + "No Email App found");
                new AlertDialog.Builder(c).setMessage(R.string.MyMainActivity_no_email_app_found).setPositiveButton(
                        R.string.MyMainActivity_button_ok, null).show();
            }
        }
        catch (ActivityNotFoundException e)
        {
            // cannot send email for some reason
            e.printStackTrace();
            Log.i(TAG, "email:EE2:" + e.getMessage());
        }
    }

    static String safe_string_XX(byte[] in)
    {
        Log.i(TAG, "safe_string:in=" + in);
        String out = "";

        try
        {
            out = new String(in, "UTF-8");  // Best way to decode using "UTF-8"
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.i(TAG, "safe_string:EE:" + e.getMessage());

            try
            {
                out = new String(in);
            }
            catch (Exception e2)
            {
                e2.printStackTrace();
                Log.i(TAG, "safe_string:EE2:" + e2.getMessage());
            }
        }

        Log.i(TAG, "safe_string:out=" + out);
        return out;
    }

    void getVersionInfo()
    {
        try
        {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = packageInfo.versionName;
            versionCode = packageInfo.versionCode;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    static class delete_selected_messages_asynchtask extends AsyncTask<Void, Void, String>
    {
        ProgressDialog progressDialog2;
        private WeakReference<Context> weakContext;
        boolean update_message_list = false;
        boolean update_friend_list = false;
        String dialog_text = "";

        delete_selected_messages_asynchtask(Context c, ProgressDialog progressDialog2, boolean update_message_list, boolean update_friend_list, String dialog_text)
        {
            this.weakContext = new WeakReference<>(c);
            this.progressDialog2 = progressDialog2;
            this.update_message_list = update_message_list;
            this.update_friend_list = update_friend_list;
            this.dialog_text = dialog_text;
        }

        @Override
        protected String doInBackground(Void... voids)
        {
            // sort ascending (lowest ID on top)
            Collections.sort(selected_messages, new Comparator<Long>()
            {
                public int compare(Long o1, Long o2)
                {
                    return o1.compareTo(o2);
                }
            });
            Iterator i = selected_messages.iterator();

            while (i.hasNext())
            {
                try
                {
                    long mid = (Long) i.next();
                    final Message m_to_delete = orma.selectFromMessage().idEq(mid).get(0);

                    // ---------- delete fileDB if this message is an outgoing file ----------
                    if (m_to_delete.TRIFA_MESSAGE_TYPE == TRIFA_MSG_FILE.value)
                    {
                        if (m_to_delete.direction == 1)
                        {
                            try
                            {
                                // FileDB file_ = orma.selectFromFileDB().idEq(m_to_delete.filedb_id).get(0);
                                orma.deleteFromFileDB().idEq(m_to_delete.filedb_id).execute();
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                                Log.i(TAG, "delete_selected_messages_asynchtask:EE4:" + e.getMessage());
                            }
                        }
                    }

                    // ---------- delete fileDB if this message is an outgoing file ----------

                    // ---------- delete fileDB and VFS file if this message is an incoming file ----------
                    if (m_to_delete.TRIFA_MESSAGE_TYPE == TRIFA_MSG_FILE.value)
                    {
                        if (m_to_delete.direction == 0)
                        {
                            try
                            {
                                FileDB file_ = orma.selectFromFileDB().idEq(m_to_delete.filedb_id).get(0);

                                try
                                {
                                    info.guardianproject.iocipher.File f_vfs = new info.guardianproject.iocipher.File(
                                            file_.path_name + "/" + file_.file_name);

                                    if (f_vfs.exists())
                                    {
                                        f_vfs.delete();
                                    }
                                }
                                catch (Exception e6)
                                {
                                    e6.printStackTrace();
                                    Log.i(TAG, "delete_selected_messages_asynchtask:EE5:" + e6.getMessage());
                                }

                                orma.deleteFromFileDB().idEq(m_to_delete.filedb_id).execute();
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                                Log.i(TAG, "delete_selected_messages_asynchtask:EE4:" + e.getMessage());
                            }
                        }
                    }

                    // ---------- delete fileDB and VFS file if this message is an incoming file ----------

                    // ---------- delete the message itself ----------
                    try
                    {
                        long message_id_to_delete = m_to_delete.id;

                        try
                        {
                            if (update_message_list)
                            {
                                Runnable myRunnable = new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        try
                                        {
                                            MyMainActivity.message_list_fragment.adapter.remove_item(m_to_delete);
                                        }
                                        catch (Exception e)
                                        {
                                            e.printStackTrace();
                                        }
                                    }
                                };

                                if (main_handler_s != null)
                                {
                                    main_handler_s.post(myRunnable);
                                }
                            }

                            // let message delete animation finish (maybe use yet another asynctask here?) ------------
                            try
                            {
                                if (update_message_list)
                                {
                                    Thread.sleep(50);
                                }
                            }
                            catch (Exception sleep_ex)
                            {
                                sleep_ex.printStackTrace();
                            }

                            // let message delete animation finish (maybe use yet another asynctask here?) ------------
                            orma.deleteFromMessage().idEq(message_id_to_delete).execute();
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                            Log.i(TAG, "delete_selected_messages_asynchtask:EE1:" + e.getMessage());
                        }
                    }
                    catch (Exception e2)
                    {
                        e2.printStackTrace();
                        Log.i(TAG, "delete_selected_messages_asynchtask:EE2:" + e2.getMessage());
                    }

                    // ---------- delete the message itself ----------
                }
                catch (Exception e2)
                {
                    e2.printStackTrace();
                    Log.i(TAG, "delete_selected_messages_asynchtask:EE3:" + e2.getMessage());
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result)
        {
            selected_messages.clear();
            selected_messages_incoming_file.clear();
            selected_messages_text_only.clear();

            try
            {
                progressDialog2.dismiss();
                Context c = weakContext.get();
                Toast.makeText(c, R.string.MyMainActivity_toast_msg_deleted, Toast.LENGTH_SHORT).show();
            }
            catch (Exception e4)
            {
                e4.printStackTrace();
                Log.i(TAG, "save_selected_messages_asynchtask:EE3:" + e4.getMessage());
            }
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();

            if (this.progressDialog2 == null)
            {
                try
                {
                    Context c = weakContext.get();
                    progressDialog2 = ProgressDialog.show(c, "", dialog_text);
                    progressDialog2.setCanceledOnTouchOutside(false);
                    progressDialog2.setOnCancelListener(new DialogInterface.OnCancelListener()
                    {
                        @Override
                        public void onCancel(DialogInterface dialog)
                        {
                        }
                    });
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    Log.i(TAG, "onPreExecute:start:EE:" + e.getMessage());
                }
            }
        }
    }

    static class save_selected_messages_asynchtask extends AsyncTask<Void, Void, String>
    {
        ProgressDialog progressDialog2;
        private WeakReference<Context> weakContext;
        private String export_directory = "";

        save_selected_messages_asynchtask(Context c, ProgressDialog progressDialog2)
        {
            this.weakContext = new WeakReference<>(c);
            this.progressDialog2 = progressDialog2;
        }

        @Override
        protected String doInBackground(Void... voids)
        {
            Iterator i = selected_messages_incoming_file.iterator();

            while (i.hasNext())
            {
                try
                {
                    long mid = (Long) i.next();
                    Message m = orma.selectFromMessage().idEq(mid).get(0);
                    FileDB file_ = orma.selectFromFileDB().idEq(m.filedb_id).get(0);
                    HelperGeneric.export_vfs_file_to_real_file(file_.path_name, file_.file_name,
                            SD_CARD_FILES_EXPORT_DIR + "/" + m.tox_friendpubkey +
                                    "/", file_.file_name);

                    export_directory = SD_CARD_FILES_EXPORT_DIR + "/" + m.tox_friendpubkey + "/";
                }
                catch (Exception e2)
                {
                    e2.printStackTrace();
                    Log.i(TAG, "save_selected_messages_asynchtask:EE1:" + e2.getMessage());
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result)
        {
            selected_messages.clear();
            selected_messages_incoming_file.clear();
            selected_messages_text_only.clear();

            try
            {
                // need to redraw all items again here, to remove the selections
                MyMainActivity.message_list_fragment.adapter.redraw_all_items();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Log.i(TAG, "save_selected_messages_asynchtask:EE2:" + e.getMessage());
            }

            try
            {
                progressDialog2.dismiss();
                Context c = weakContext.get();
                Toast.makeText(c, "Files exported to:" + "\n" + export_directory, Toast.LENGTH_SHORT).show();
            }
            catch (Exception e4)
            {
                e4.printStackTrace();
                Log.i(TAG, "save_selected_messages_asynchtask:EE3:" + e4.getMessage());
            }
        }

        @Override
        protected void onPreExecute()
        {
        }
    }

    static class delete_selected_conference_messages_asynchtask extends AsyncTask<Void, Void, String>
    {
        ProgressDialog progressDialog2;
        private WeakReference<Context> weakContext;
        boolean update_conf_message_list = false;
        String dialog_text = "";

        delete_selected_conference_messages_asynchtask(Context c, ProgressDialog progressDialog2, boolean update_conf_message_list, String dialog_text)
        {
            this.weakContext = new WeakReference<>(c);
            this.progressDialog2 = progressDialog2;
            this.update_conf_message_list = update_conf_message_list;
            this.dialog_text = dialog_text;
        }

        @Override
        protected String doInBackground(Void... voids)
        {
            // sort ascending (lowest ID on top)
            Collections.sort(selected_conference_messages, new Comparator<Long>()
            {
                public int compare(Long o1, Long o2)
                {
                    return o1.compareTo(o2);
                }
            });
            Iterator i = selected_conference_messages.iterator();

            while (i.hasNext())
            {
                try
                {
                    long mid = (Long) i.next();
                    final ConferenceMessage m_to_delete = orma.selectFromConferenceMessage().idEq(mid).get(0);

                    // ---------- delete the message itself ----------
                    try
                    {
                        long message_id_to_delete = m_to_delete.id;

                        try
                        {
                            if (update_conf_message_list)
                            {
                                Runnable myRunnable = new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        try
                                        {
                                            MyMainActivity.conference_message_list_fragment.adapter.remove_item(
                                                    m_to_delete);
                                        }
                                        catch (Exception e)
                                        {
                                            e.printStackTrace();
                                        }
                                    }
                                };

                                if (main_handler_s != null)
                                {
                                    main_handler_s.post(myRunnable);
                                }
                            }

                            // let message delete animation finish (maybe use yet another asynctask here?) ------------
                            try
                            {
                                if (update_conf_message_list)
                                {
                                    Thread.sleep(50);
                                }
                            }
                            catch (Exception sleep_ex)
                            {
                                sleep_ex.printStackTrace();
                            }

                            // let message delete animation finish (maybe use yet another asynctask here?) ------------
                            orma.deleteFromConferenceMessage().idEq(message_id_to_delete).execute();
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                            Log.i(TAG, "delete_selected_conference_messages_asynchtask:EE1:" + e.getMessage());
                        }
                    }
                    catch (Exception e2)
                    {
                        e2.printStackTrace();
                        Log.i(TAG, "delete_selected_conference_messages_asynchtask:EE2:" + e2.getMessage());
                    }

                    // ---------- delete the message itself ----------
                }
                catch (Exception e2)
                {
                    e2.printStackTrace();
                    Log.i(TAG, "delete_selected_conference_messages_asynchtask:EE3:" + e2.getMessage());
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result)
        {
            selected_conference_messages.clear();

            try
            {
                progressDialog2.dismiss();
                Context c = weakContext.get();
                Toast.makeText(c, R.string.MyMainActivity_toast_msgs_deleted, Toast.LENGTH_SHORT).show();
            }
            catch (Exception e4)
            {
                e4.printStackTrace();
                Log.i(TAG, "delete_selected_conference_messages_asynchtask:EE3:" + e4.getMessage());
            }
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();

            if (this.progressDialog2 == null)
            {
                try
                {
                    Context c = weakContext.get();
                    progressDialog2 = ProgressDialog.show(c, "", dialog_text);
                    progressDialog2.setCanceledOnTouchOutside(false);
                    progressDialog2.setOnCancelListener(new DialogInterface.OnCancelListener()
                    {
                        @Override
                        public void onCancel(DialogInterface dialog)
                        {
                        }
                    });
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    Log.i(TAG, "onPreExecute:start:EE:" + e.getMessage());
                }
            }
        }
    }

    static class send_message_result
    {
        long msg_num;
        boolean msg_v2;
        String msg_hash_hex;
        String raw_message_buf_hex;
        long error_num;
    }

    /*************************************************************************/
    /* this function now really sends a 1:1 to a friend (or a friends relay) */
    private void fadeInAndShowImage(final View img, long start_after_millis)
    {
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new AccelerateInterpolator());
        fadeIn.setDuration(1000);
        fadeIn.setStartOffset(start_after_millis);
        fadeIn.setAnimationListener(new Animation.AnimationListener()
        {
            public void onAnimationEnd(Animation animation)
            {
            }

            public void onAnimationRepeat(Animation animation)
            {
            }

            public void onAnimationStart(Animation animation)
            {
                img.setVisibility(View.VISIBLE);
            }
        });
        img.startAnimation(fadeIn);
    }

    private void fadeOutAndHideImage(final View img, long start_after_millis)
    {
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setDuration(1000);
        fadeOut.setStartOffset(start_after_millis);
        fadeOut.setAnimationListener(new Animation.AnimationListener()
        {
            public void onAnimationEnd(Animation animation)
            {
                img.setVisibility(View.GONE);
            }

            public void onAnimationRepeat(Animation animation)
            {
            }

            public void onAnimationStart(Animation animation)
            {
            }
        });
        img.startAnimation(fadeOut);
    }

    // --------- make app crash ---------
    // --------- make app crash ---------
    // --------- make app crash ---------
    public static void crash_app_java(int type)
    {
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======================+++++");
        System.out.println("+++++======= TYPE:J =======+++++");
        System.out.println("+++++======================+++++");

        if (type == 1)
        {
            Java_Crash_001();
        }
        else if (type == 2)
        {
            Java_Crash_002();
        }
        else
        {
            stackOverflow();
        }
    }

    public static void Java_Crash_001()
    {
        Integer i = null;
        i.byteValue();
    }

    public static void Java_Crash_002()
    {
        View v = null;
        v.bringToFront();
    }

    public static void stackOverflow()
    {
        stackOverflow();
    }

    public static void crash_app_C()
    {
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======================+++++");
        System.out.println("+++++======= TYPE:C =======+++++");
        System.out.println("+++++======================+++++");
        AppCrashC();
    }

    // --------- make app crash ---------
    // --------- make app crash ---------
    // --------- make app crash ---------

}


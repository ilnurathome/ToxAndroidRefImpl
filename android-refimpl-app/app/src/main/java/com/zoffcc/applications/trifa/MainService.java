/**
 * 
 * 
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

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import info.guardianproject.iocipher.VirtualFileSystem;

import static com.zoffcc.applications.trifa.HelperFriend.tox_friend_by_public_key__wrapper;
import static com.zoffcc.applications.trifa.HelperGeneric.replyMessenger;
import static com.zoffcc.applications.trifa.HelperGeneric.tox_friend_send_message_wrapper;
import static com.zoffcc.applications.trifa.HelperMessage.insert_into_message_db;
import static com.zoffcc.applications.trifa.MainActivity.VFS_ENCRYPT;
import static com.zoffcc.applications.trifa.MainActivity.tox_service_fg;
import static com.zoffcc.applications.trifa.TRIFAGlobals.PREF__DB_secrect_key__user_hash;
import static com.zoffcc.applications.trifa.TRIFAGlobals.TRIFA_MSG_TYPE.TRIFA_MSG_TYPE_TEXT;
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

public class MainService extends Service {
    private static final String TAG = "trifa.MainService";

    public static final int MSG_REQ = 0;
    public static final int MSG_REPLY = 1;
    public static final int MSG_SUB = 2;
    public static final int MSG_PUB = 3;
    public static final String MSG_REQ_FUNC = "F";
    public static final String MSG_REPLY_FUNC = "R";

    Messenger mMessenger = new Messenger(new IncomingHandler(this));

    Map<String, Command> commandMap = new HashMap<>();

    public MainService() {
        commandMap.put("vfsIsMounted", new Command() {
            @Override
            public Bundle runCommand(Bundle req) {
                Bundle res = new Bundle();
                VirtualFileSystem v = getVFS();
                if (v != null) {
                    res.putBoolean(MSG_REPLY_FUNC, v.isMounted());
                } else {
                    res.putBoolean(MSG_REPLY_FUNC, false);
                }
                return res;
            }
        });

        commandMap.put("TOX_SERVICE_STARTED", new Command() {
            @Override
            public Bundle runCommand(Bundle req) {
                Bundle res = new Bundle();
                res.putBoolean(MSG_REPLY_FUNC, TOX_SERVICE_STARTED());
                return res;
            }
        });

        commandMap.put("get_PREF__DB_secrect_key__user_hash", new Command() {
            @Override
            public Bundle runCommand(Bundle req) {
                Bundle res = new Bundle();
                res.putString(MSG_REPLY_FUNC, get_PREF__DB_secrect_key__user_hash());
                return res;
            }
        });

        commandMap.put("getUnProccedMsgs", new Command() {
            @Override
            public Bundle runCommand(Bundle bundle) {
                Bundle res = new Bundle();
                long timestamp = bundle.getLong("timestamp");
                res.putByteArray(MSG_REPLY_FUNC, getUnProccedMsgs(timestamp));
                return res;
            }
        });

        commandMap.put("getFriendList", new Command() {
            @Override
            public Bundle runCommand(Bundle bundle) {
                Bundle res = new Bundle();
                res.putByteArray(MSG_REPLY_FUNC, getFriendList());
                return res;
            }
        });

        commandMap.put("getFriendByPublicKey", new Command() {
            @Override
            public Bundle runCommand(Bundle bundle) {
                Bundle res = new Bundle();
                res.putByteArray(MSG_REPLY_FUNC, getFriendByPublicKey(bundle.getString("friend_pubkey")));
                return res;
            }
        });

        commandMap.put("getFriendListIds", new Command() {
            @Override
            public Bundle runCommand(Bundle bundle) {
                Bundle res = new Bundle();
                List<String> ids = bundle.getStringArrayList("ids");
                res.putByteArray(MSG_REPLY_FUNC, getFriendList(ids));
                return res;
            }
        });

        commandMap.put("processMsg", new Command() {
            @Override
            public Bundle runCommand(Bundle bundle) {
                Bundle res = new Bundle();
                long id = bundle.getLong("id");
                processMsg(id);
                return res;
            }
        });

        commandMap.put("toxStop", new Command() {
            @Override
            public Bundle runCommand(Bundle bundle) {
                Bundle res = new Bundle();
                toxStop();
                return res;
            }
        });

        commandMap.put("sendToxMsg", new Command() {
            @Override
            public Bundle runCommand(Bundle bundle) {
                Bundle res = new Bundle();
                String id = bundle.getString("id");
                String str = bundle.getString("msg");
                send(id, str);
                return res;
            }
        });
    }

//    private final IBinder binder = new LocalBinder();

    static class IncomingHandler extends Handler {
        private MainService mainService;

        IncomingHandler(MainService mainService) {
            this.mainService = mainService;
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            Log.d(TAG, "Handle message");
            switch (msg.what) {
                case MSG_REQ:
                    Bundle bundle = msg.getData();
                    android.os.Message reply = android.os.Message.obtain(null, MSG_REPLY, 0, msg.arg2);;
                    if(!mainService.commandMap.containsKey(bundle.getString(MSG_REQ_FUNC))) {
                        Log.e(TAG, "Not found command: " + bundle.getString(MSG_REQ_FUNC));
                    }
                    reply.setData(mainService.commandMap.get(bundle.getString(MSG_REQ_FUNC)).runCommand(bundle));
                    try {
                        msg.replyTo.send(reply);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Send reply fail", e);
                    }
                    break;
                case MSG_SUB:
                    Messenger messenger = msg.replyTo;
                    mainService.setReplyMessenger(messenger);
                    break;
                default:
                    Log.d(TAG, "Unknown message, what is " + msg.what);
                    super.handleMessage(msg);
            }
        }
    }


    public void vfs_listFilesAndFilesSubDirectories(String directoryName, int depth, String parent) {
        if (VFS_ENCRYPT) {
            info.guardianproject.iocipher.File directory1 = new info.guardianproject.iocipher.File(directoryName);
            info.guardianproject.iocipher.File[] fList1 = directory1.listFiles();

            for (info.guardianproject.iocipher.File file : fList1) {
                if (file.isFile()) {
                    // final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    // final String human_datetime = df.format(new Date(file.lastModified()));
                    Log.i(TAG, "VFS:f:" + parent + "/" + file.getName() + " bytes=" + file.length());
                } else if (file.isDirectory()) {
                    Log.i(TAG, "VFS:d:" + parent + "/" + file.getName() + "/");
                    vfs_listFilesAndFilesSubDirectories(file.getAbsolutePath(), depth + 1,
                            parent + "/" + file.getName());
                }
            }
        } else {
            java.io.File directory1 = new java.io.File(directoryName);
            java.io.File[] fList1 = directory1.listFiles();

            for (File file : fList1) {
                if (file.isFile()) {
                    // final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    // final String human_datetime = df.format(new Date(file.lastModified()));
                    Log.i(TAG, "VFS:f:" + parent + "/" + file.getName() + " bytes=" + file.length());
                } else if (file.isDirectory()) {
                    Log.i(TAG, "VFS:d:" + parent + "/" + file.getName() + "/");
                    vfs_listFilesAndFilesSubDirectories(file.getAbsolutePath(), depth + 1,
                            parent + "/" + file.getName());
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    public class LocalBinder extends Binder {
        public MainService getService() {
            return MainService.this;
        }
    }

    public byte[] getFriendByPublicKey(String tox_public_key_string) {
        ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
        objectMapper.findAndRegisterModules();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        byte[] bytes = new byte[0];

        if (TOX_SERVICE_STARTED) {
            FriendList f;
            List<FriendList> fl = orma.selectFromFriendList().tox_public_key_stringEq(
                    tox_public_key_string).toList();

            if (fl.size() > 0)
            {
                f = fl.get(0);
            }
            else
            {
                f = null;
            }

            try {
                bytes = objectMapper.writeValueAsBytes(f);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        return bytes;
    }

    public byte[] getFriendList(Collection<String> ids) {
        ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
        objectMapper.findAndRegisterModules();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        byte[] bytes = new byte[0];

        if (TOX_SERVICE_STARTED) {
            List<FriendList> fl = orma.selectFromFriendList().
                    tox_public_key_stringIn(ids).
                    orderByTOX_CONNECTION_on_offDesc().
                    orderByNotification_silentAsc().
                    orderByLast_online_timestampDesc().
                    toList();

            try {
                bytes = objectMapper.writeValueAsBytes(fl);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        return bytes;
    }

    public byte[] getUnProccedMsgs(long timestamp) {
        byte[] bytes = new byte[0];

        if (TOX_SERVICE_STARTED) {
            ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
            objectMapper.findAndRegisterModules();
            objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

//            orma.selectFromMessageShoppingList().where(orma.relationOfMessageShoppingList().schema.message_id.associationSchema.rcvd_timestamp.getQualifiedName() +">" + timestamp).toList();

//            List<Message> msgs = orma.selectFromMessageShoppingList()
//                    .where(orma.relationOfMessageShoppingList().schema.message_id.associationSchema.rcvd_timestamp.getQualifiedName() + ">" + timestamp)
//                    .where(orma.relationOfMessageShoppingList().schema.message_id.associationSchema.direction.getQualifiedName() + "=" + 0)
//                    .where(orma.relationOfMessageShoppingList().schema.id.getQualifiedName() + " is null")
//                    .orderBy(orma.relationOfMessageShoppingList().schema.message_id.associationSchema.id.getQualifiedName())
//                    .getRawValuesAndMap(new Function1<Cursor, Message>() {
//                        @Override
//                        public Message apply(Cursor cursor) {
//                            return orma.selectFromMessage().newModelFromCursor(cursor);
//                        }
//                    });

            Cursor cursor = orma.getConnection().rawQuery("SELECT `m2`.`message_id`, `m2`.`tox_friendpubkey`, `m2`.`direction`, `m2`.`TOX_MESSAGE_TYPE`, `m2`.`TRIFA_MESSAGE_TYPE`, `m2`.`state`, `m2`.`ft_accepted`, `m2`.`ft_outgoing_started`, `m2`.`filedb_id`, `m2`.`filetransfer_id`, `m2`.`sent_timestamp`, `m2`.`sent_timestamp_ms`, `m2`.`rcvd_timestamp`, `m2`.`rcvd_timestamp_ms`, `m2`.`read`, `m2`.`send_retries`, `m2`.`is_new`, `m2`.`text`, `m2`.`filename_fullpath`, `m2`.`msg_id_hash`, `m2`.`raw_msgv2_bytes`, `m2`.`msg_version`, `m2`.`resend_count`, `m2`.`id` FROM `Message` AS `m2` LEFT OUTER JOIN `MessageShoppingList` AS `m1` ON `m1`.`message_id` = `m2`.`id` WHERE (`m2`.`rcvd_timestamp`>?) AND (`m2`.`direction`=0) AND (`m1`.`id` is null) ORDER BY `m2`.`id`", String.valueOf(timestamp));
                List<Message> msgs;
                try {
                    msgs = new ArrayList<>(cursor.getCount());
                    Log.i(TAG, cursor.getColumnNames().toString());
                    Log.d(TAG, "count=" + cursor.getCount());
                    for (int pos = 0; cursor.moveToPosition(pos); pos++) {
                        msgs.add(orma.selectFromMessage().newModelFromCursor(cursor));
                    }
                } finally {
                    cursor.close();
                }


//            List<MessageShoppingList> msl = orma.selectFromMessageShoppingList().toList();
//            List<Long> messageIds = new ArrayList<Long>();
//            for(MessageShoppingList m : msl) {
//                messageIds.add(m.getMsg_id());
//            }

//            List<Message> msgs = orma.selectFromMessage().directionEq(0).rcvd_timestampGt(timestamp).idNotIn(messageIds).orderByIdAsc().toList();
            if (msgs.size() > 0) {
                try {
                    bytes = objectMapper.writeValueAsBytes(msgs);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
        return bytes;
    }

    public void processMsg(Long id) {
        if (TOX_SERVICE_STARTED) {
            MessageShoppingList m = new MessageShoppingList();
            m.message_id = orma.selectFromMessage().idEq(id).valueOrNull();
            orma.insertIntoMessageShoppingList(m);
        }
    }

    public byte[] getFriendList() {
        ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
        objectMapper.findAndRegisterModules();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        byte[] bytes = new byte[0];

        if (TOX_SERVICE_STARTED) {
            List<FriendList> fl = orma.selectFromFriendList().
                    is_relayNotEq(true).
                    orderByTOX_CONNECTION_on_offDesc().
                    orderByNotification_silentAsc().
                    orderByLast_online_timestampDesc().
                    toList();

            try {
                bytes = objectMapper.writeValueAsBytes(fl);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        return bytes;
    }

    public info.guardianproject.iocipher.VirtualFileSystem getVFS() {
        return vfs;
    }

    public OrmaDatabase getOrma() {
        return orma;
    }

    public boolean TOX_SERVICE_STARTED() {
        return TOX_SERVICE_STARTED;
    }

    public void send(String tox_public_key_string, String msg) {
        if (TOX_SERVICE_STARTED) {
            Long friendnum = tox_friend_by_public_key__wrapper(tox_public_key_string);

            // send typed message to friend
            Message m = new Message();
            m.tox_friendpubkey = tox_public_key_string;
            m.direction = 1; // msg sent
            m.TOX_MESSAGE_TYPE = 0;
            m.TRIFA_MESSAGE_TYPE = TRIFA_MSG_TYPE_TEXT.value;
            m.rcvd_timestamp = 0L;
            m.is_new = false; // own messages are always "not new"
            m.sent_timestamp = System.currentTimeMillis();
            m.read = false;
            m.text = msg;
            m.msg_version = 0;
            m.resend_count = 0; // we have tried to resend this message "0" times

            if ((msg != null) && (!msg.equalsIgnoreCase("")) && friendnum >=0 ) {
                MainActivity.send_message_result result = tox_friend_send_message_wrapper(friendnum, 0, msg);
                long res = result.msg_num;
                Log.i(TAG, "tox_friend_send_message_wrapper:result=" + res + " m=" + m);

                if (res > -1) // sending was OK
                {
                    m.message_id = res;
                    if (!result.msg_hash_hex.equalsIgnoreCase("")) {
                        // msgV2 message -----------
                        m.msg_id_hash = result.msg_hash_hex;
                        m.msg_version = 1;
                        // msgV2 message -----------
                    }

                    if (!result.raw_message_buf_hex.equalsIgnoreCase("")) {
                        // save raw message bytes of this v2 msg into the database
                        // we need it if we want to resend it later
                        m.raw_msgv2_bytes = result.raw_message_buf_hex;
                    }

                    m.resend_count = 1; // we sent the message successfully

                    long row_id = insert_into_message_db(m, true);
                    m.id = row_id;
                } else {
                    // sending was NOT ok

                    Log.i(TAG, "tox_friend_send_message_wrapper:store pending message" + m);

                    m.message_id = -1;
                    long row_id = insert_into_message_db(m, true);
                    m.id = row_id;
                }
            }
//                        }
//                    }
//                }
//            }
        }
    }

    public void setReplyMessenger(Messenger m) {
        replyMessenger = m;
    }

    public void toxStop() {
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

    public String get_PREF__DB_secrect_key__user_hash() {
        return PREF__DB_secrect_key__user_hash;
    }

    interface Command {
        Bundle runCommand(Bundle bundle);
    }
}


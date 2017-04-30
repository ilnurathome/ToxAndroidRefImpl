/**
 * [TRIfA], JNI part of Tox Reference Implementation for Android
 * Copyright (C) 2017 Zoff <zoff@zoff.cc>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301, USA.
 */


#include <ctype.h>
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>
#include <time.h>
#include <dirent.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
#include <unistd.h>

#include <fcntl.h>
#include <errno.h>

#include <sodium/utils.h>
#include <tox/tox.h>
#include <tox/toxav.h>

#include <linux/videodev2.h>
#include <vpx/vpx_image.h>
#include <sys/mman.h>

// ------- Android/JNI stuff -------
// #include <android/log.h>
#include <jni.h>
// ------- Android/JNI stuff -------

#define CLEAR(x) memset(&(x), 0, sizeof(x))
#define c_sleep(x) usleep(1000*x)

#define CURRENT_LOG_LEVEL 9 // 0 -> error, 1 -> warn, 2 -> info, 9 -> debug
#define MAX_LOG_LINE_LENGTH 1000
#define MAX_FULL_PATH_LENGTH 1000

const char *savedata_filename = "savedata.tox";
const char *savedata_tmp_filename = "savedata.tox.tmp";
int tox_loop_running = 1;
TOX_CONNECTION my_connection_status = TOX_CONNECTION_NONE;

// ----- JNI stuff -----
JNIEnv *jnienv;
JavaVM *cachedJVM = NULL;
jobject *android_activity;

char *app_data_dir = NULL;
jclass MainActivity = NULL;
jmethodID logger_method = NULL;

// -------- _callbacks_ --------
jmethodID android_tox_callback_self_connection_status_cb_method = NULL;
jmethodID android_tox_callback_friend_name_cb_method = NULL;
jmethodID android_tox_callback_friend_status_message_cb_method = NULL;
jmethodID android_tox_callback_friend_status_cb_method = NULL;
jmethodID android_tox_callback_friend_connection_status_cb_method = NULL;
jmethodID android_tox_callback_friend_typing_cb_method = NULL;
jmethodID android_tox_callback_friend_read_receipt_cb_method = NULL;
jmethodID android_tox_callback_friend_request_cb_method = NULL;
jmethodID android_tox_callback_friend_message_cb_method = NULL;
// -------- _callbacks_ --------

// ----- JNI stuff -----



typedef struct DHT_node {
    const char *ip;
    uint16_t port;
    const char key_hex[TOX_PUBLIC_KEY_SIZE*2 + 1];
    unsigned char key_bin[TOX_PUBLIC_KEY_SIZE];
} DHT_node;




// functions -----------
// functions -----------
// functions -----------
void self_connection_status_cb(Tox *tox, TOX_CONNECTION connection_status, void *user_data);
void friend_name_cb(Tox *tox, uint32_t friend_number, const uint8_t *name, size_t length, void *user_data);
void friend_status_message_cb(Tox *tox, uint32_t friend_number, const uint8_t *message, size_t length, void *user_data);
void friend_status_cb(Tox *tox, uint32_t friend_number, TOX_USER_STATUS status, void *user_data);
void friend_connection_status_cb(Tox *tox, uint32_t friend_number, TOX_CONNECTION connection_status, void *user_data);
void friend_typing_cb(Tox *tox, uint32_t friend_number, bool is_typing, void *user_data);
void friend_read_receipt_cb(Tox *tox, uint32_t friend_number, uint32_t message_id, void *user_data);
void friend_request_cb(Tox *tox, const uint8_t *public_key, const uint8_t *message, size_t length, void *user_data);
void friend_message_cb(Tox *tox, uint32_t friend_number, TOX_MESSAGE_TYPE type, const uint8_t *message, size_t length, void *user_data);

void android_logger(int level, const char* logtext);
// functions -----------
// functions -----------
// functions -----------





void dbg(int level, const char *fmt, ...)
{
	char *level_and_format = NULL;
	char *fmt_copy = NULL;
	char *log_line_str = NULL;

	if (fmt == NULL)
	{
		return;
	}

	if (strlen(fmt) < 1)
	{
		return;
	}

	if ((level < 0) || (level > 9))
	{
		level = 0;
	}

	level_and_format = malloc(strlen(fmt) + 3);

	if (!level_and_format)
	{
		return;
	}

	fmt_copy = level_and_format + 2;
	strcpy(fmt_copy, fmt);
	level_and_format[1] = ':';
	if (level == 0)
	{
		level_and_format[0] = 'E';
	}
	else if (level == 1)
	{
		level_and_format[0] = 'W';
	}
	else if (level == 2)
	{
		level_and_format[0] = 'I';
	}
	else
	{
		level_and_format[0] = 'D';
	}

	if (level <= CURRENT_LOG_LEVEL)
	{
		log_line_str = malloc((size_t)MAX_LOG_LINE_LENGTH);
		va_list ap;
		va_start(ap, fmt);
		vsnprintf(log_line_str, (size_t)MAX_LOG_LINE_LENGTH, level_and_format, ap);
		// send "log_line_str" to android
		android_logger(level, log_line_str);
		va_end(ap);
		free(log_line_str);
	}

	if (level_and_format)
	{
		free(level_and_format);
	}
}


Tox *create_tox()
{
	Tox *tox;
	struct Tox_Options options;

	tox_options_default(&options);

	uint16_t tcp_port = 33776; // act as TCP relay

	options.ipv6_enabled = true;
	options.udp_enabled = true;
	options.local_discovery_enabled = true;
	options.hole_punching_enabled = true;
	options.tcp_port = tcp_port;

	char *full_path_filename = malloc(MAX_FULL_PATH_LENGTH);
	snprintf(full_path_filename, (size_t)MAX_FULL_PATH_LENGTH, "%s/%s", app_data_dir, savedata_filename);


    FILE *f = fopen(full_path_filename, "rb");
    if (f)
	{
        fseek(f, 0, SEEK_END);
        long fsize = ftell(f);
        fseek(f, 0, SEEK_SET);

        uint8_t *savedata = malloc(fsize);

        size_t dummy = fread(savedata, fsize, 1, f);
		if (dummy < 1)
		{
			dbg(0, "reading savedata failed\n");
		}
        fclose(f);

        options.savedata_type = TOX_SAVEDATA_TYPE_TOX_SAVE;
        options.savedata_data = savedata;
        options.savedata_length = fsize;

        tox = tox_new(&options, NULL);

        free((void *)savedata);
    }
	else
	{
        tox = tox_new(&options, NULL);
    }

	bool local_discovery_enabled = tox_options_get_local_discovery_enabled(&options);
	dbg(9, "local discovery enabled = %d\n", (int)local_discovery_enabled);

	free(full_path_filename);

    return tox;
}


void update_savedata_file(const Tox *tox)
{
    size_t size = tox_get_savedata_size(tox);
    char *savedata = malloc(size);
    tox_get_savedata(tox, (uint8_t *)savedata);

	char *full_path_filename = malloc(MAX_FULL_PATH_LENGTH);
	snprintf(full_path_filename, (size_t)MAX_FULL_PATH_LENGTH, "%s/%s", app_data_dir, savedata_filename);

	char *full_path_filename_tmp = malloc(MAX_FULL_PATH_LENGTH);
	snprintf(full_path_filename_tmp, (size_t)MAX_FULL_PATH_LENGTH, "%s/%s", app_data_dir, savedata_tmp_filename);

    FILE *f = fopen(full_path_filename_tmp, "wb");
    fwrite(savedata, size, 1, f);
    fclose(f);

    rename(full_path_filename_tmp, full_path_filename);

	free(full_path_filename);
	free(full_path_filename_tmp);
    free(savedata);
}


int bin_id_to_string(const char *bin_id, size_t bin_id_size, char *output, size_t output_size)
{
    if (bin_id_size != TOX_ADDRESS_SIZE || output_size < (TOX_ADDRESS_SIZE * 2 + 1))
    {
        return -1;
    }

    size_t i;
    for (i = 0; i < TOX_ADDRESS_SIZE; ++i)
    {
        snprintf(&output[i * 2], output_size - (i * 2), "%02X", bin_id[i] & 0xff);
    }

	return 0;
}

void bootstrap(Tox *tox)
{
    DHT_node nodes[] =
    {
        {"178.62.250.138",             33445, "788236D34978D1D5BD822F0A5BEBD2C53C64CC31CD3149350EE27D4D9A2F9B6B", {0}},
        {"2a03:b0c0:2:d0::16:1",       33445, "788236D34978D1D5BD822F0A5BEBD2C53C64CC31CD3149350EE27D4D9A2F9B6B", {0}},
        {"tox.zodiaclabs.org",         33445, "A09162D68618E742FFBCA1C2C70385E6679604B2D80EA6E84AD0996A1AC8A074", {0}},
        {"163.172.136.118",            33445, "2C289F9F37C20D09DA83565588BF496FAB3764853FA38141817A72E3F18ACA0B", {0}},
        {"2001:bc8:4400:2100::1c:50f", 33445, "2C289F9F37C20D09DA83565588BF496FAB3764853FA38141817A72E3F18ACA0B", {0}},
        {"128.199.199.197",            33445, "B05C8869DBB4EDDD308F43C1A974A20A725A36EACCA123862FDE9945BF9D3E09", {0}},
        {"2400:6180:0:d0::17a:a001",   33445, "B05C8869DBB4EDDD308F43C1A974A20A725A36EACCA123862FDE9945BF9D3E09", {0}},
        {"biribiri.org",               33445, "F404ABAA1C99A9D37D61AB54898F56793E1DEF8BD46B1038B9D822E8460FAB67", {0}}
    };

    for (size_t i = 0; i < sizeof(nodes)/sizeof(DHT_node); i ++) {
        sodium_hex2bin(nodes[i].key_bin, sizeof(nodes[i].key_bin),
                       nodes[i].key_hex, sizeof(nodes[i].key_hex)-1, NULL, NULL, NULL);
        tox_bootstrap(tox, nodes[i].ip, nodes[i].port, nodes[i].key_bin, NULL);
    }
}

// fill string with toxid in upper case hex.
// size of toxid_str needs to be: [TOX_ADDRESS_SIZE*2 + 1] !!
void get_my_toxid(Tox *tox, char *toxid_str)
{
    uint8_t tox_id_bin[TOX_ADDRESS_SIZE];
    tox_self_get_address(tox, tox_id_bin);

	char tox_id_hex_local[TOX_ADDRESS_SIZE*2 + 1];
    sodium_bin2hex(tox_id_hex_local, sizeof(tox_id_hex_local), tox_id_bin, sizeof(tox_id_bin));

    for (size_t i = 0; i < sizeof(tox_id_hex_local)-1; i ++)
	{
        tox_id_hex_local[i] = toupper(tox_id_hex_local[i]);
    }

	snprintf(toxid_str, (size_t)(TOX_ADDRESS_SIZE*2 + 1), "%s", (const char*)tox_id_hex_local);
}

void toxid_hex_to_bin(const uint8_t *public_key, char *toxid_str)
{
	sodium_hex2bin(public_key, TOX_ADDRESS_SIZE, toxid_str, (TOX_ADDRESS_SIZE*2), NULL, NULL, NULL);
}

void toxid_bin_to_hex(const uint8_t *public_key, char *toxid_str)
{
	char tox_id_hex_local[TOX_ADDRESS_SIZE*2 + 1];
    sodium_bin2hex(tox_id_hex_local, sizeof(tox_id_hex_local), public_key, TOX_ADDRESS_SIZE);

    for (size_t i = 0; i < sizeof(tox_id_hex_local)-1; i ++)
	{
        tox_id_hex_local[i] = toupper(tox_id_hex_local[i]);
    }

	snprintf(toxid_str, (size_t)(TOX_ADDRESS_SIZE*2 + 1), "%s", (const char*)tox_id_hex_local);
}

void print_tox_id(Tox *tox)
{
    char tox_id_hex[TOX_ADDRESS_SIZE*2 + 1];
	get_my_toxid(tox, tox_id_hex);

	dbg(2, "MyToxID:%s\n", tox_id_hex);
}



void _main_()
{
	Tox *tox = create_tox();

	const char *name = "TRIfA";
	tox_self_set_name(tox, (uint8_t *)name, strlen(name), NULL);

	const char *status_message = "This is TRIfA";
	tox_self_set_status_message(tox, (uint8_t *)status_message, strlen(status_message), NULL);


	bootstrap(tox);
	print_tox_id(tox);

	// -------- _callbacks_ --------
	tox_callback_self_connection_status(tox, self_connection_status_cb);
	tox_callback_friend_name(tox, friend_name_cb);
	tox_callback_friend_status_message(tox, friend_status_message_cb);
	tox_callback_friend_status(tox, friend_status_cb);
	tox_callback_friend_connection_status(tox, friend_connection_status_cb);
	tox_callback_friend_typing(tox, friend_typing_cb);
	tox_callback_friend_read_receipt(tox, friend_read_receipt_cb);
	tox_callback_friend_request(tox, friend_request_cb);
	tox_callback_friend_message(tox, friend_message_cb);
// tox_callback_file_recv_control(tox, tox_file_recv_control_cb *callback);
// tox_callback_file_chunk_request(tox, tox_file_chunk_request_cb *callback);
// tox_callback_file_recv(tox, tox_file_recv_cb *callback);
// tox_callback_file_recv_chunk(tox, tox_file_recv_chunk_cb *callback);
// tox_callback_conference_invite(tox, tox_conference_invite_cb *callback);
// tox_callback_conference_message(tox, tox_conference_message_cb *callback);
// tox_callback_conference_title(tox, tox_conference_title_cb *callback);
// tox_callback_conference_namelist_change(tox, tox_conference_namelist_change_cb *callback);
// tox_callback_friend_lossy_packet(tox, tox_friend_lossy_packet_cb *callback);
// tox_callback_friend_lossless_packet(tox, tox_friend_lossless_packet_cb *callback);
	// -------- _callbacks_ --------

	update_savedata_file(tox);

	long long unsigned int cur_time = time(NULL);
	uint8_t off = 1;
	while (1)
	{
	        tox_iterate(tox, NULL);
	        usleep(tox_iteration_interval(tox) * 1000);
	        if (tox_self_get_connection_status(tox) && off)
		{
	        	dbg(2, "Tox online, took %llu seconds\n", time(NULL) - cur_time);
	        	off = 0;
			break;
        	}
        	c_sleep(20);
	}

	tox_loop_running = 1;

    	while (tox_loop_running)
    	{
        	tox_iterate(tox, NULL);
        	usleep(tox_iteration_interval(tox) * 1000);
	}

	// does not reach here now!
	tox_kill(tox);
}


// ------------- JNI -------------
// ------------- JNI -------------
// ------------- JNI -------------


JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved)
{
	JNIEnv *env_this;
	cachedJVM = jvm;
	if ((*jvm)->GetEnv(jvm, (void**) &env_this, JNI_VERSION_1_6))
	{
		// dbg(0,"Could not get JVM\n");
		return JNI_ERR;
	}

	// dbg(0,"++ Found JVM ++\n");
	return JNI_VERSION_1_6;
}

JNIEnv* jni_getenv()
{
	JNIEnv* env_this;
	(*cachedJVM)->GetEnv(cachedJVM, (void**) &env_this, JNI_VERSION_1_6);
	return env_this;
}


JNIEnv *AttachJava()
{
	JavaVMAttachArgs args = {JNI_VERSION_1_6, 0, 0};
	JNIEnv *java;
	(*cachedJVM)->AttachCurrentThread(cachedJVM, &java, &args);
	return java;
}

int android_find_class_global(char *name, jclass *ret)
{
	JNIEnv *jnienv2;
	jnienv2 = jni_getenv();

	*ret = (*jnienv2)->FindClass(jnienv2, name);
	if (!*ret)
	{
		return 0;
	}

	*ret = (*jnienv2)->NewGlobalRef(jnienv2, *ret);
	return 1;
}

int android_find_method(jclass class, char *name, char *args, jmethodID *ret)
{
	JNIEnv *jnienv2;
	jnienv2 = jni_getenv();

	*ret = (*jnienv2)->GetMethodID(jnienv2, class, name, args);
	if (*ret == NULL)
	{
		return 0;
	}

	return 1;
}


int android_find_static_method(jclass class, char *name, char *args, jmethodID *ret)
{
	JNIEnv *jnienv2;
	jnienv2 = jni_getenv();

	*ret = (*jnienv2)->GetStaticMethodID(jnienv2, class, name, args);
	if (*ret == NULL)
	{
		return 0;
	}

	return 1;
}

// -------- _callbacks_ --------
void android_tox_callback_self_connection_status_cb(int a_TOX_CONNECTION)
{
	JNIEnv *jnienv2;
	jnienv2 = jni_getenv();
	(*jnienv2)->CallStaticVoidMethod(jnienv2, MainActivity,
          android_tox_callback_self_connection_status_cb_method, a_TOX_CONNECTION);
}

void self_connection_status_cb(Tox *tox, TOX_CONNECTION connection_status, void *user_data)
{
    switch (connection_status)
    {
        case TOX_CONNECTION_NONE:
            dbg(2, "Offline\n");
            my_connection_status = TOX_CONNECTION_NONE;
            android_tox_callback_self_connection_status_cb((int)my_connection_status);
            break;
        case TOX_CONNECTION_TCP:
            dbg(2, "Online, using TCP\n");
            my_connection_status = TOX_CONNECTION_TCP;
            android_tox_callback_self_connection_status_cb((int)my_connection_status);
            break;
        case TOX_CONNECTION_UDP:
            dbg(2, "Online, using UDP\n");
            my_connection_status = TOX_CONNECTION_UDP;
            android_tox_callback_self_connection_status_cb((int)my_connection_status);
            break;
    }
}

void android_tox_callback_friend_name_cb(uint32_t friend_number, const uint8_t *name, size_t length)
{
	JNIEnv *jnienv2;
	jnienv2 = jni_getenv();

	jstring js1 = (*jnienv2)->NewStringUTF(jnienv2, (char *)name);

	(*jnienv2)->CallStaticVoidMethod(jnienv2, MainActivity,
          android_tox_callback_friend_name_cb_method, (jlong)(unsigned long long)friend_number, js1, (jlong)(unsigned long long)length);

	(*jnienv2)->DeleteLocalRef(jnienv2, js1);
}

void friend_name_cb(Tox *tox, uint32_t friend_number, const uint8_t *name, size_t length, void *user_data)
{
	android_tox_callback_friend_name_cb(friend_number, name, length);
}

void android_tox_callback_friend_status_message_cb(uint32_t friend_number, const uint8_t *message, size_t length)
{
	JNIEnv *jnienv2;
	jnienv2 = jni_getenv();

	jstring js1 = (*jnienv2)->NewStringUTF(jnienv2, (char *)message);

	(*jnienv2)->CallStaticVoidMethod(jnienv2, MainActivity,
          android_tox_callback_friend_status_message_cb_method, (jlong)(unsigned long long)friend_number, js1, (jlong)(unsigned long long)length);

	(*jnienv2)->DeleteLocalRef(jnienv2, js1);
}

void friend_status_message_cb(Tox *tox, uint32_t friend_number, const uint8_t *message, size_t length, void *user_data)
{
	android_tox_callback_friend_status_message_cb(friend_number, message, length);
}

void android_tox_callback_friend_status_cb(uint32_t friend_number, TOX_USER_STATUS status)
{
	JNIEnv *jnienv2;
	jnienv2 = jni_getenv();

	(*jnienv2)->CallStaticVoidMethod(jnienv2, MainActivity,
          android_tox_callback_friend_status_cb_method, (jlong)(unsigned long long)friend_number, (int)status);
}

void friend_status_cb(Tox *tox, uint32_t friend_number, TOX_USER_STATUS status, void *user_data)
{
	android_tox_callback_friend_status_cb(friend_number, status);
}

void android_tox_callback_friend_connection_status_cb(uint32_t friend_number, TOX_CONNECTION connection_status)
{
	JNIEnv *jnienv2;
	jnienv2 = jni_getenv();

    switch (connection_status)
    {
        case TOX_CONNECTION_NONE:
            dbg(2, "friend# %d Offline\n", (int)friend_number);
			(*jnienv2)->CallStaticVoidMethod(jnienv2, MainActivity,
				android_tox_callback_friend_connection_status_cb_method, (jlong)(unsigned long long)friend_number, (int)connection_status);
            break;
        case TOX_CONNECTION_TCP:
            dbg(2, "friend# %d Online, using TCP\n", (int)friend_number);
			(*jnienv2)->CallStaticVoidMethod(jnienv2, MainActivity,
				android_tox_callback_friend_connection_status_cb_method, (jlong)(unsigned long long)friend_number, (int)connection_status);
            break;
        case TOX_CONNECTION_UDP:
            dbg(2, "friend# %d Online, using UDP\n", (int)friend_number);
			(*jnienv2)->CallStaticVoidMethod(jnienv2, MainActivity,
				android_tox_callback_friend_connection_status_cb_method, (jlong)(unsigned long long)friend_number, (int)connection_status);
            break;
    }
}

void friend_connection_status_cb(Tox *tox, uint32_t friend_number, TOX_CONNECTION connection_status, void *user_data)
{
	android_tox_callback_friend_connection_status_cb(friend_number, connection_status);
}

void android_tox_callback_friend_typing_cb(uint32_t friend_number, bool is_typing)
{
	JNIEnv *jnienv2;
	jnienv2 = jni_getenv();
}

void friend_typing_cb(Tox *tox, uint32_t friend_number, bool is_typing, void *user_data)
{
	android_tox_callback_friend_typing_cb(friend_number, is_typing);
}

void android_tox_callback_friend_read_receipt_cb(uint32_t friend_number, uint32_t message_id)
{
	JNIEnv *jnienv2;
	jnienv2 = jni_getenv();
}

void friend_read_receipt_cb(Tox *tox, uint32_t friend_number, uint32_t message_id, void *user_data)
{
	android_tox_callback_friend_read_receipt_cb(friend_number, message_id);
}

void android_tox_callback_friend_request_cb(const uint8_t *public_key, const uint8_t *message, size_t length)
{
	JNIEnv *jnienv2;
	jnienv2 = jni_getenv();

    char tox_id_hex[TOX_ADDRESS_SIZE*2 + 1];
	toxid_bin_to_hex(public_key, tox_id_hex);

	dbg(9, "pubkey string=%s", tox_id_hex);

	jstring js1 = (*jnienv2)->NewStringUTF(jnienv2, tox_id_hex);
	jstring js2 = (*jnienv2)->NewStringUTF(jnienv2, message);

	(*jnienv2)->CallStaticVoidMethod(jnienv2, MainActivity,
		android_tox_callback_friend_request_cb_method, js1, js2, (jlong)(unsigned long long)length);

	(*jnienv2)->DeleteLocalRef(jnienv2, js1);
	(*jnienv2)->DeleteLocalRef(jnienv2, js2);

}

void friend_request_cb(Tox *tox, const uint8_t *public_key, const uint8_t *message, size_t length, void *user_data)
{
	android_tox_callback_friend_request_cb(public_key, message, length);

	// ------------------- auto add any friend -------------------
    uint32_t friendnum = tox_friend_add_norequest(tox, public_key, NULL);
    dbg(2, "add friend:friendnum=%d\n", friendnum);
	update_savedata_file(tox);
	// ------------------- auto add any friend -------------------
}

void android_tox_callback_friend_message_cb(uint32_t friend_number, TOX_MESSAGE_TYPE type, const uint8_t *message, size_t length)
{
	JNIEnv *jnienv2;
	jnienv2 = jni_getenv();

	jstring js1 = (*jnienv2)->NewStringUTF(jnienv2, (char *)message);

	(*jnienv2)->CallStaticVoidMethod(jnienv2, MainActivity,
          android_tox_callback_friend_message_cb_method, (jlong)(unsigned long long)friend_number, (int) type, js1, (jlong)(unsigned long long)length);

	(*jnienv2)->DeleteLocalRef(jnienv2, js1);
}

void friend_message_cb(Tox *tox, uint32_t friend_number, TOX_MESSAGE_TYPE type, const uint8_t *message, size_t length, void *user_data)
{
	android_tox_callback_friend_message_cb(friend_number, type, message, length);
}
// -------- _callbacks_ --------


void android_logger(int level, const char* logtext)
{
	if ((MainActivity) && (logger_method) && (logtext))
	{
		if (strlen(logtext) > 0)
		{
			JNIEnv *jnienv2;
			jnienv2 = jni_getenv();

			jstring js2 = (*jnienv2)->NewStringUTF(jnienv2, logtext);
			(*jnienv2)->CallStaticVoidMethod(jnienv2, MainActivity, logger_method, level, js2);
			(*jnienv2)->DeleteLocalRef(jnienv2, js2);
		}
	}
}

JNIEXPORT void JNICALL
Java_com_zoffcc_applications_trifa_MainActivity_init(JNIEnv* env, jobject thiz, jobject datadir)
{
	const char *s = NULL;

	// SET GLOBAL JNIENV here, this is bad!!
	// SET GLOBAL JNIENV here, this is bad!!
	// SET GLOBAL JNIENV here, this is bad!!
	// jnienv = env;
	// dbg(0,"jnienv=%p\n", env);
	// SET GLOBAL JNIENV here, this is bad!!
	// SET GLOBAL JNIENV here, this is bad!!
	// SET GLOBAL JNIENV here, this is bad!!

	jclass cls_local = (*env)->GetObjectClass(env, thiz);
	MainActivity = (*env)->NewGlobalRef(env, cls_local);
	logger_method = (*env)->GetStaticMethodID(env, MainActivity, "logger", "(ILjava/lang/String;)V");

	dbg(9, "cls_local=%p\n", cls_local);
	dbg(9, "MainActivity=%p\n", MainActivity);

	dbg(9, "Logging test ---***---");

	int thread_id = gettid();
	dbg(9, "THREAD ID=%d\n", thread_id);

	s =  (*env)->GetStringUTFChars(env, datadir, NULL);
	app_data_dir = strdup(s);
	dbg(9, "app_data_dir=%s\n", app_data_dir);
	(*env)->ReleaseStringUTFChars(env, datadir, s);

        jclass class2 = NULL;
	android_find_class_global("com/zoffcc/applications/trifa/MainActivity", &class2);
	dbg(9, "class2=%p\n", class2);

	jmethodID test_method = NULL;

	android_find_method(class2, "test", "(I)V", &test_method);
	dbg(9, "test_method=%p\n", test_method);


	dbg(9, "100");
	(*env)->CallVoidMethod(env, thiz, test_method, 79);

	// -------- _callbacks_ --------
	dbg(9, "101");
	android_tox_callback_self_connection_status_cb_method = (*env)->GetStaticMethodID(env, MainActivity, "android_tox_callback_self_connection_status_cb_method", "(I)V");
	dbg(9, "102");
	android_tox_callback_friend_name_cb_method = (*env)->GetStaticMethodID(env, MainActivity, "android_tox_callback_friend_name_cb_method", "(JLjava/lang/String;J)V");
	dbg(9, "103");
	android_tox_callback_friend_status_message_cb_method = (*env)->GetStaticMethodID(env, MainActivity, "android_tox_callback_friend_status_message_cb_method", "(JLjava/lang/String;J)V");
	dbg(9, "104");
	android_tox_callback_friend_status_cb_method = (*env)->GetStaticMethodID(env, MainActivity, "android_tox_callback_friend_status_cb_method", "(JI)V");
	dbg(9, "105");
	android_tox_callback_friend_connection_status_cb_method = (*env)->GetStaticMethodID(env, MainActivity, "android_tox_callback_friend_connection_status_cb_method", "(JI)V");
	dbg(9, "106");
	android_tox_callback_friend_typing_cb_method = (*env)->GetStaticMethodID(env, MainActivity, "android_tox_callback_friend_typing_cb_method", "(JI)V");
	dbg(9, "107");
	android_tox_callback_friend_read_receipt_cb_method = (*env)->GetStaticMethodID(env, MainActivity, "android_tox_callback_friend_read_receipt_cb_method", "(JJ)V");
	dbg(9, "108");
	android_tox_callback_friend_request_cb_method = (*env)->GetStaticMethodID(env, MainActivity, "android_tox_callback_friend_request_cb_method", "(Ljava/lang/String;Ljava/lang/String;J)V");
	dbg(9, "109");
	android_tox_callback_friend_message_cb_method = (*env)->GetStaticMethodID(env, MainActivity, "android_tox_callback_friend_message_cb_method", "(JILjava/lang/String;J)V");
	dbg(9, "110");
	// -------- _callbacks_ --------

}




// --------------- _toxfuncs_ ---------------
// --------------- _toxfuncs_ ---------------
// --------------- _toxfuncs_ ---------------
JNIEXPORT void JNICALL
Java_com_zoffcc_applications_trifa_MainActivity_update_savedata_file(JNIEnv* env, jobject thiz)
{
	update_savedata_file(tox);
}

JNIEXPORT jlong JNICALL
Java_com_zoffcc_applications_trifa_MainActivity_tox_friend_add_norequest(JNIEnv* env, jobject thiz, jobject public_key_str)
{
	unsigned char public_key_bin[TOX_PUBLIC_KEY_SIZE];
	char *public_key_str2 = NULL;

	s =  (*env)->GetStringUTFChars(env, public_key_str, NULL);
	public_key_str2 = strdup(s);
	dbg(9, "public_key_str2=%s\n", public_key_str2);
	(*env)->ReleaseStringUTFChars(env, public_key_str, s);

	toxid_hex_to_bin(public_key_bin, public_key_str2);
    uint32_t friendnum = tox_friend_add_norequest(tox, public_key_bin, NULL);

	if (public_key_str2)
	{
		free(public_key_str2);
	}

    dbg(2, "add friend:friendnum=%d\n", friendnum);
	return (jlong)(unsigned long long)friendnum;
}
// --------------- _toxfuncs_ ---------------
// --------------- _toxfuncs_ ---------------
// --------------- _toxfuncs_ ---------------




JNIEXPORT void JNICALL
Java_com_zoffcc_applications_trifa_MainActivity_toxloop(JNIEnv* env, jobject thiz)
{
	_main_();
}

// ------------------------------------------------------------------------------------------------
// taken from:
// https://github.com/googlesamples/android-ndk/blob/master/hello-jni/app/src/main/cpp/hello-jni.c
// ------------------------------------------------------------------------------------------------

JNIEXPORT jstring JNICALL
Java_com_zoffcc_applications_trifa_MainActivity_getNativeLibAPI(JNIEnv* env, jobject thiz)
{
#if defined(__arm__)
    #if defined(__ARM_ARCH_7A__)
    #if defined(__ARM_NEON__)
      #if defined(__ARM_PCS_VFP)
        #define ABI "armeabi-v7a/NEON (hard-float)"
      #else
        #define ABI "armeabi-v7a/NEON"
      #endif
    #else
      #if defined(__ARM_PCS_VFP)
        #define ABI "armeabi-v7a (hard-float)"
      #else
        #define ABI "armeabi-v7a"
      #endif
    #endif
  #else
   #define ABI "armeabi"
  #endif
#elif defined(__i386__)
#define ABI "x86"
#elif defined(__x86_64__)
#define ABI "x86_64"
#elif defined(__mips64)  /* mips64el-* toolchain defines __mips__ too */
#define ABI "mips64"
#elif defined(__mips__)
#define ABI "mips"
#elif defined(__aarch64__)
#define ABI "arm64-v8a"
#else
#define ABI "unknown"
#endif

    return (*env)->NewStringUTF(env, "Native Code Compiled with ABI:" ABI "");
}


// ------------- JNI -------------
// ------------- JNI -------------
// ------------- JNI -------------




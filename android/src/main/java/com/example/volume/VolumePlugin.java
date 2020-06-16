package com.example.volume;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;

import java.util.HashMap;
import java.util.List;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.view.FlutterNativeView;

/**
 * VolumePlugin
 */
public class VolumePlugin extends ContentObserver implements MethodCallHandler {

    private final MethodChannel channel;
    private Activity activity;
    AudioManager audioManager;
    private int streamType;

    private volatile  boolean draggable = false;

    /**
     * Plugin registration.
     */
    public static void registerWith(final Registrar registrar) {
        if (registrar.activity() == null) {
            return;
        }

        final MethodChannel channel = new MethodChannel(registrar.messenger(), "volume");
        final VolumePlugin volumePlugin = new VolumePlugin(registrar.activity(), channel);
        channel.setMethodCallHandler(volumePlugin);


        Context context = registrar.activeContext();
        final ContentResolver contentResolver = context.getContentResolver();//

        contentResolver.registerContentObserver(android.provider.Settings.System.CONTENT_URI,//
                        true, volumePlugin);

        registrar.addViewDestroyListener(new PluginRegistry.ViewDestroyListener() {
            @Override
            public boolean onViewDestroy(FlutterNativeView v) {
                contentResolver.unregisterContentObserver(volumePlugin);
                return true;
            }
        });

    }

    private VolumePlugin(Activity activity, MethodChannel channel) {
        super(new Handler());
        this.activity = activity;
        this.channel = channel;

        audioManager = (AudioManager) this.activity.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (call.method.equals("controlVolume")) {
            streamType = call.argument("streamType");
            controlVolume(streamType);
            result.success(null);
        } else if (call.method.equals("getMaxVol")) {
            result.success(getMaxVol());
        } else if (call.method.equals("getVol")) {
            result.success(getVol());
        } else if (call.method.equals("setVol")) {
            int i = call.argument("newVol");
            setVol(i);
            result.success(null);
        } else {
            result.notImplemented();
        }
    }

    void controlVolume(int i) {
        this.activity.setVolumeControlStream(i);
    }


    int getMaxVol() {
        return audioManager.getStreamMaxVolume(streamType);
    }

    int getVol() {
        return audioManager.getStreamVolume(streamType);
    }

    int setVol(int i) {
        draggable = true;
        audioManager.setStreamVolume(streamType, i, 0);
        return audioManager.getStreamVolume(streamType);
    }

    @Override
    public boolean deliverSelfNotifications() {
        return false;
    }

    @Override
    public void onChange(boolean selfChange) {
        if (draggable){
            this.draggable = false;
            return;
        }
        final int currentVolume = audioManager.getStreamVolume(streamType);
        final int maxVolume = audioManager.getStreamMaxVolume(streamType);
        channel.invokeMethod("volumeChanged", new HashMap<String, Integer>() {
            {
                put("currentVolume", Integer.valueOf(currentVolume));
                put("maxVolume", Integer.valueOf(maxVolume));
            }
        });

        Log.d("VOLUME", "Volume now " + currentVolume);
    }
}

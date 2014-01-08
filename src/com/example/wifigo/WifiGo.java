package com.example.wifigo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.example.wifigo.BasicFragment.DeviceActionListener;

/*Main Function*/
public class WifiGo extends Activity implements ChannelListener, DeviceActionListener {

    public static final String TAG = "wifigo";
    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;
    public boolean lock = false;
    private final IntentFilter intentFilter = new IntentFilter();
    private Channel channel;
    private BroadcastReceiver receiver = null;
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifigo);

        // add necessary intent values to be matched.

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
    }
    @Override
    public void onResume() {
        super.onResume();
        receiver = new Receiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }
    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }
    public void setlock(){
    	lock = true;
    }
    //重置時將資料從表單清除，BroadcastReceiver用到
    public void resetData() {
    	lock = false;
        BasicFragment fragmentList = (BasicFragment) getFragmentManager()
                .findFragmentById(R.id.frag_list);
        AdvanceFragment fragmentDetails = (AdvanceFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        if (fragmentList != null) {
            fragmentList.clearPeers();
        }
        if (fragmentDetails != null) {
            fragmentDetails.resetViews();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_items, menu);
        return true;
    }
    //
    
    //
    
    //
    
    //偵測手指動作
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	//if(lock==false){
    		
    	if (event.getAction() == MotionEvent.ACTION_UP ){
    		if (!isWifiP2pEnabled) {
                Toast.makeText(WifiGo.this, "Make sure you have turned on your Wifi.",
                        Toast.LENGTH_SHORT).show();
                return true;
            }
            final BasicFragment fragment = (BasicFragment) getFragmentManager()
                    .findFragmentById(R.id.frag_list);
            fragment.onInitiateDiscovery();
            manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            //ActionListener一定要有onSuccess onFailure兩個ｆｕｎｃｔｉｏｎ
                @Override
                public void onSuccess() {
                    Toast.makeText(WifiGo.this, "Discovery start",
                            Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(int reasonCode) {
                    Toast.makeText(WifiGo.this, "Discovery Fail ",
                            Toast.LENGTH_SHORT).show();
                }
            });
            return true;    
    	}
        //}
//    	if (event.getAction() == MotionEvent.ACTION_POINTER_1_UP ){
//    		disconnect();
//    	}
    	return super.onTouchEvent(event);
    }
    //
    
    //
    
    //
    //彈出detail fragment內容 
    @Override
    public void showDetails(WifiP2pDevice device) {
        AdvanceFragment fragment = (AdvanceFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.showDetails(device);

    }
    //連線動作
    //成功動作在BroadcastReceiver
    @Override
    public void connect(WifiP2pConfig config) {
        manager.connect(channel, config, new ActionListener() {

            @Override
            public void onSuccess() {
            	Toast.makeText(WifiGo.this, "Connect success.",
                        Toast.LENGTH_SHORT).show();
            	lock = true;
            }
            
            @Override
            public void onFailure(int reason) {
                Toast.makeText(WifiGo.this, "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    //離線動作
    @Override
    public void disconnect() {
        final AdvanceFragment fragment = (AdvanceFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.resetViews();
        manager.removeGroup(channel, new ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);

            }

            @Override
            public void onSuccess() {
                fragment.getView().setVisibility(View.GONE);
                lock = false;
            }

        });
    }
    //debug
    @Override
    public void onChannelDisconnected() {
        if (manager != null && !retryChannel) {
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
            resetData();
            retryChannel = true;
            manager.initialize(this, getMainLooper(), this);
        } else {
            Toast.makeText(this,
                    "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();
        }
    }
    //一端斷線後另一端重製
    @Override
    public void cancelDisconnect() {
        if (manager != null) {
            final BasicFragment fragment = (BasicFragment) getFragmentManager()
                    .findFragmentById(R.id.frag_list);
            if (fragment.getDevice() == null
                    || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
            } else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE
                    || fragment.getDevice().status == WifiP2pDevice.INVITED) {

                manager.cancelConnect(channel, new ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(WifiGo.this, "Aborting connection",
                                Toast.LENGTH_SHORT).show();
                        lock = false;
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WifiGo.this,
                                "Connect abort request failed. Reason Code: " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

    }
}

package com.example.wifigo;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import com.example.wifigo.BasicFragment.DeviceActionListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class AdvanceFragment extends Fragment implements ConnectionInfoListener {

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    ProgressDialog progressDialog = null;
    private VideoView video;
    private MediaController ctlr;
    public Uri cdata;
    public String caddress;
    public String tmp;
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }
  //建立連線INFLATE按鈕和CONNECT按鈕和進度對話框
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContentView = inflater.inflate(R.layout.advancefrag, null);
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

        	//wait
//        	public void onwait(){
//        		if (progressDialog != null && progressDialog.isShowing()) {
//                    progressDialog.dismiss();
//                }
//                progressDialog = ProgressDialog.show(getActivity(), "please wait",
//                        "receiving files...");
//        	}
            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;//wifi protected setup & push button config
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + device.deviceName, true, true
                        );
                ((DeviceActionListener) getActivity()).connect(config);

            }
        });

        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ((DeviceActionListener) getActivity()).disconnect();
                    }
                });
      //讓CLIENT選影片用
        mContentView.findViewById(R.id.btn_start_Sender).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("video/*");
                        startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
                        //////////////////////////go to video
                         
//                            Intent it = new Intent();
//                            it.setAction(android.content.Intent.ACTION_VIEW);
//                            it.setDataAndType(Uri.parse("file://" + CHOOSE_FILE_RESULT_CODE), "video/*");
//                            context.startActivity(it);
//                            

                        
                        //go_to();
                        /*Intent go_to = new Intent();
                        go_to.setClass(AdvanceFragment.this, MainActivity.class);
                        startActivity(intent);*/
                    }
                });

        return mContentView;
    }

/*    public void go_to() {
		// TODO Auto-generated method stub
    	Intent intent = new Intent();
        intent.setClass(this, MainActivity.class);
        startActivity(intent);
	}*/

	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

		// 將影片傳給ｓｅｒｖｅｒ，詳細見 Transference.
        Uri uri = data.getData();
        cdata = uri;
        //tmp = uri.toString();
        Intent serviceIntent = new Intent(getActivity(), Transference.class);
        serviceIntent.setAction(Transference.ACTION_SEND_FILE);
        ///
        //serviceIntent.putExtra(tmp, true);
        ///
        serviceIntent.putExtra(Transference.EXTRAS_FILE_PATH, uri.toString());
        serviceIntent.putExtra(Transference.EXTRAS_GROUP_OWNER_ADDRESS,
                info.groupOwnerAddress.getHostAddress());
        caddress = info.groupOwnerAddress.getHostAddress();
        serviceIntent.putExtra(Transference.EXTRAS_GROUP_OWNER_PORT, 5555);
        getActivity().startService(serviceIntent);
        //
        //
        //put play here
        //post activity for Sender
        //
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(uri.toString()), "video/*");
        startActivity(intent);
        //
        //
        //done
        //
        //
    }
	//顯示傳輸資訊，具顯示之後消失功能
    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);
        ((TextView) mContentView.findViewById(R.id.status_text)).setText("");
      //Receiver 和 Sender 底端告知文字所呈現之不同
        if (info.groupFormed && info.isGroupOwner) {
            new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text))
                    .execute();
            ((TextView) mContentView.findViewById(R.id.status_text)).setText("Receiver");
        } else if (info.groupFormed) {
            mContentView.findViewById(R.id.btn_start_Sender).setVisibility(View.VISIBLE);
            ((TextView) mContentView.findViewById(R.id.status_text)).setText("Sender");
        }
        // hide the connect button
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }

    //顯示 fragment
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
    }

    //RESET用
    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        mContentView.findViewById(R.id.btn_start_Sender).setVisibility(View.GONE);       
        //TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        this.getView().setVisibility(View.GONE);
    }

//    public void setvideo(File f){
//    	video=(VideoView)mContentView.findViewById(R.id.videoView1);
//        video.setVideoPath(f.getAbsolutePath());
//        
//        //ctlr=new MediaController(this);
//        //ctlr.setMediaPlayer(video);
//        //video.setMediaController(ctlr);
//        //video.requestFocus();
//        video.start();
//    }
    
    //以下從 WiFiP2P FileServerAsyncTask 參考
    /**
     * A simple Receiver socket that accepts connection and writes some data on
     * the stream.
     */
    public /*static*/ class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private TextView statusText;

        /**
         * @param context
         * @param statusText
         */
        public FileServerAsyncTask(Context context, View statusText) {
            this.context = context;
            this.statusText = (TextView) statusText;
        }

        @Override
        protected String doInBackground(Void... params) {
        	String passvalue;
            try {
                ServerSocket ReceiverSocket = new ServerSocket(5555);
//                Log.d(WifiGo.TAG, "Server: Socket opened");
                Socket Sender = ReceiverSocket.accept();               
//                Log.d(WifiGo.TAG, "Server: connection done");
                final File f = new File(Environment.getExternalStorageDirectory() + "/"
                        + "wifigo"/*context.getPackageName() */ + "/" + System.currentTimeMillis()
                        + ".mp4");
                //((TextView) mContentView.findViewById(R.id.status_text)).setText("File transfering...");
//                passvalue = f.getAbsolutePath();
//                onPostExecute(passvalue);
                //deliverToNextActitity(passvalue);
                //
                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();

                Log.d(WifiGo.TAG, "Receiver: copying files " + f.toString());
                InputStream inputstream = Sender.getInputStream();
                
//                if (f.exists()) {
//                	setvideo(f);
//                    
//                  }
                //
                //
                //for demo
                //((TextView) mContentView.findViewById(R.id.status_text)).setText(tmp);
                //onPostExecute(f);
                //
                //
                
                
                //
                //
                //for stream
                //
                //
                
//                String videoUrl = "http://dl.dropbox.com/u/80419/santa.mp4";
//                Intent i = new Intent(Intent.ACTION_VIEW);
//                i.setData(Uri.parse(videoUrl));
//                startActivity(i);
                //
                //
                //
                //
                //
                copyFile(inputstream, new FileOutputStream(f));

                ReceiverSocket.close();
//                Log.d(WifiGo.TAG, "File get!");
                //((TextView) mContentView.findViewById(R.id.status_text)).setText("Server");
                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e(WifiGo.TAG, e.getMessage());
                return null;
            }
        }
        
//        public String getAddress(){
//			
//        	return device.deviceAddress;
//        	
//        }
        ////////////////////for demo
//        protected void onPostExecute(File f) {
//           
//                //((TextView) mContentView.findViewById(R.id.status_text)).setText(tmp);
//               Intent intent = new Intent();
//                intent.setAction(android.content.Intent.ACTION_VIEW);
//                intent.setDataAndType(Uri.parse("https://dl.dropboxusercontent.com/u/67657720/demo.mp4"), "video/*");
////                intent.setDataAndType(Uri.parse(Environment.getExternalStorageDirectory()
////                 + "/" + "1388371021478.mp4"), "video/*");
////                //
////                intent.setDataAndType(Uri.parse(device.deviceAddress + "/" + tmp), "video/*");
////                //
////                //
////                //
//                context.startActivity(intent);
//            
//
//        }
         @Override
        protected void onPostExecute(String result) {
        	 if (progressDialog != null && progressDialog.isShowing()) {
                 progressDialog.dismiss();
             }
            if (result != null) {
                statusText.setText("File copied - " + result);
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + result), "video/*");
                context.startActivity(intent);
            }

        }
        
//        public void deliverToNextActitity(String passvalue){
//        	final EditText nameEdit;
//        	//建立一個bundle物件，並將要傳遞的參數放到bundle裡
//        	Bundle bundle = new Bundle();
//        	bundle.putString(passvalue, nameEdit.getText().toString());
//        	Intent intent = new Intent();
//        	//設定下一個Activity
//        	intent.setClass(WiFiDirectActivity.this, MainActivity.class);
//        	intent.putExtras(bundle);
//        	//開啟Activity
//        	startActivity(intent);
//        }




        
        @Override
        protected void onPreExecute() {
            statusText.setText("Ready");
        }

    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(WifiGo.TAG, e.toString());
            return false;
        }
        return true;
    }

}

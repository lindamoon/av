package com.lixb.camera1demo;

import android.content.pm.FeatureInfo;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, View.OnClickListener {

    private static final String TAG = "camera1";
    public static final int UPDATE_RECORD_DURATION = 1020;
    public static final int FINISH_RECORD = 1021;
    public static final int START_RECORD = 1022;
    private SurfaceView mSurfaceView;
    private Camera mCamera;
    private int curCameraId;
    private Button mBtnCapture;
    private Button mBtnRecord;
    private Button mBtnSwitchCamera;
    private boolean mRecording;
    private MediaRecorder mRecorder;
    private int durationSeconds;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            Log.d(TAG, "handleMessage: "+msg);
            switch (msg.what) {
                case UPDATE_RECORD_DURATION:
                    if (mTvRecordDuration.getVisibility() != View.VISIBLE) {
                        mTvRecordDuration.setVisibility(View.VISIBLE);
                    }
                    durationSeconds++;
                    int minutes = durationSeconds / 60;
                    int seconds = durationSeconds % 60;
                    String ms = minutes < 10 ? "0" + minutes : String.valueOf(minutes);
                    String ss = seconds < 10 ? "0" + seconds : String.valueOf(seconds);
                    mTvRecordDuration.setText(ms+":"+ss);
                    sendEmptyMessageDelayed(UPDATE_RECORD_DURATION, 1000);
                    break;
                case START_RECORD:
                    durationSeconds = 0;
                    sendEmptyMessage(UPDATE_RECORD_DURATION);
                    break;
                case FINISH_RECORD:
                    durationSeconds = 0;
                    mTvRecordDuration.setVisibility(View.INVISIBLE);
                    removeMessages(UPDATE_RECORD_DURATION);
                    break;
            }
        }
    };
    private TextView mTvRecordDuration;
    private int mWidth = 1280;
    private int mHeight = 720;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        printFeature();
        mSurfaceView = findViewById(R.id.surface_view);
        mBtnSwitchCamera = findViewById(R.id.btn_switch_cam);
        mBtnCapture = findViewById(R.id.btn_capture);
        mBtnRecord = findViewById(R.id.btn_record);
        mTvRecordDuration = findViewById(R.id.tv_record_duration);

        mSurfaceView.getHolder().addCallback(this);
        mBtnSwitchCamera.setOnClickListener(this);
        mBtnCapture.setOnClickListener(this);
        mBtnRecord.setOnClickListener(this);
    }

    private void printFeature() {
        FeatureInfo[] systemAvailableFeatures = getPackageManager().getSystemAvailableFeatures();
        for (FeatureInfo info : systemAvailableFeatures) {
            Log.i(TAG, "printFeature: " + info);
        }
    }


    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated: " + holder);
        //打开相机
        openCamera(curCameraId, holder);
    }

    private void openCamera(int curCameraId, SurfaceHolder holder) {
        try {
            if (null == mCamera) {
                mCamera = Camera.open(curCameraId);
                Camera.Parameters parameters = mCamera.getParameters();
                mCamera.setParameters(parameters);
                Log.d(TAG, "openCamera: id:" + curCameraId);
            }
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged: holder :" + holder + ",format:" + format + ",width:" + width + ",height:" + height);
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        //关闭相机
        Log.d(TAG, "surfaceDestroyed: " + holder);
        if (null != mCamera) {
            mCamera.stopPreview();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_switch_cam:
                doSwitchCamera();

                break;
            case R.id.btn_capture:
                doCapture();

                break;
            case R.id.btn_record:
                doRecord();

                break;
        }
    }


    private void doSwitchCamera() {
        if (mRecording) {
            Toast.makeText(this, "录制中请先停止录像", Toast.LENGTH_SHORT).show();
            return;
        }
        //关闭之前的camera
        if (null != mCamera) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        //打开新的camera
        int numberOfCameras = Camera.getNumberOfCameras();
        curCameraId += 1;
        if (curCameraId >= numberOfCameras) {
            curCameraId = 0;
        }

        openCamera(curCameraId, mSurfaceView.getHolder());
    }

    private void doCapture() {
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                camera.startPreview();

                //保存图片
                File pictureDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                File imgFile = new File(pictureDir, "IMG_" + dateStr + ".jpg");
                Log.d(TAG, "onPictureTaken: imgFile : " + imgFile.getAbsolutePath());
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(imgFile);
                    fos.write(data);
                    fos.flush();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (null != fos) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        fos = null;
                    }
                }
            }
        });
    }

    private void doRecord() {
        if (mRecording) {
            //stop recording
            if (null != mRecorder) {
                mRecorder.stop();
                mRecorder.reset();
                mRecorder.release();
                mRecorder = null;
            }
            try {
                mCamera.reconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mRecording = false;
            mBtnRecord.setText("开始录像");
            mHandler.sendEmptyMessage(FINISH_RECORD);
        } else {
            try {

                mRecorder = new MediaRecorder();
                mCamera.unlock();
                mRecorder.setCamera(mCamera);
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                mRecorder.setVideoEncodingBitRate(4 * 1024 * 1024);
                mRecorder.setVideoFrameRate(30);
                mRecorder.setVideoSize(mWidth, mHeight);
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                File movDirs = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
                String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                File videoFile = new File(movDirs, "VIDEO_" + dateStr + ".mp4");
                mRecorder.setOutputFile(videoFile.getAbsolutePath());
                mRecorder.setPreviewDisplay(mSurfaceView.getHolder().getSurface());
                mRecorder.prepare();
                mRecorder.start();
                mRecording = true;
                mBtnRecord.setText("结束录像");
                mHandler.sendEmptyMessage(START_RECORD);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

}

package com.example.mftool1126;


import android.app.ActionBar;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.w3c.dom.Text;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Vector;



import android.app.ActionBar;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.widget.TextView;
import android.provider.MediaStore;

public class MainActivity extends AppCompatActivity
{

    String msgText;

    //Camera variables
    CameraDevice camera;


    // Get the widgets reference from XML layout
    TextureView textureView;
    private ImageButton cam_btn = (ImageButton) findViewById(R.id.camera_image_btn);
    private ImageButton gallery_btn = (ImageButton) findViewById(R.id.gallery_image_btn);


    private static final int CAMERA_PIC_REQUEST = 1337;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textureView = (TextureView) findViewById(R.id.texture_view);
        textureView.setSurfaceTextureListener(textureListener);

        cam_btn.setOnClickListener(new View.OnClickListener()
        {
           @Override
           public void onClick(View v)
           {
                takePicture();
           }
        });
    }

    protected void takePicture()
    {
        if (camera == null) {
            Log.e(msgText, "No camera detected");
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(camera.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            final File file = new File(Environment.getExternalStorageDirectory() + "/pic.jpg");
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }

                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(MainActivity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }
            };
            camera.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }



/*    CameraBridgeViewBase cameraBridgeViewBase;
    BaseLoaderCallback baseLoaderCallback;
    boolean startCanny = false;
    boolean viewFrame = true;
    double lineDist = 2.00;
    double lineSlope = 0.0;
    double lineIntercept1 = 0.0;
    double lineIntercept2 = 0.0;
    int canny1_Min, canny2_Max, hough_Max,hough_Min ;


//    boolean collectFrames = false;




    public void Canny(View Button){

        if (startCanny == false){
            startCanny = true;
            SeekBar seekBar_Canny1 = (SeekBar)findViewById(R.id.seekBar_Canny1);
            canny1_Min = seekBar_Canny1.getProgress();
            SeekBar seekBar_Canny2 = (SeekBar)findViewById(R.id.seekBar2_Canny2);
            canny2_Max = seekBar_Canny2.getProgress();
            SeekBar seekBar_HougMax = (SeekBar)findViewById(R.id.seekBar_HougMax);
            hough_Max = seekBar_HougMax.getProgress();
            SeekBar seekBar_HougMin = (SeekBar)findViewById(R.id.seekBar_HougMin);
            hough_Min = seekBar_HougMin.getProgress();

        }

        else{

            startCanny = false;
 //

        }


    }
    public void Seeframe (View Button2){

        if (viewFrame == true){
 //           cameraBridgeViewBase.disableView();
//
            viewFrame = false;
            onPause();
        }

        else{
//            cameraBridgeViewBase.enableView();


            onResume();
            viewFrame = true;

        }


    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        cameraBridgeViewBase = (JavaCameraView)findViewById(R.id.CameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);


        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);

                switch(status){

                    case BaseLoaderCallback.SUCCESS:
                        cameraBridgeViewBase.enableView();

                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }


            }

        };


    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

            Mat frame = inputFrame.rgba();
            Mat imagCaputred = new Mat();
            Mat lines = new Mat();
            imagCaputred = frame;
            Point oldStart = new Point(2,3);
            Point oldEnd = new Point(2,3);
            int lineCount = 0;
            Bitmap bm = Bitmap.createBitmap( imagCaputred.cols(),imagCaputred.rows(), Bitmap.Config.ARGB_8888);

        if (startCanny == true ) {

                Imgproc.cvtColor(imagCaputred, imagCaputred, Imgproc.COLOR_RGBA2GRAY);


*//*                    Utils.matToBitmap(imagCaputred, bm);
                    cameraBridgeViewBase.disableView();
                    ImageView processedimage = (ImageView) findViewById(R.id.CameraView);
                    processedimage.setImageBitmap(bm);
                    Button btn = (Button)findViewById(R.id.button2);
                    btn.performClick();
                   cameraBridgeViewBase.enableView();
*//*
                  Imgproc.medianBlur(imagCaputred, imagCaputred, 3);
//                  Imgproc.GaussianBlur(imagCaputred, imagCaputred, new Size (1,1),1, 1);





                    Imgproc.dilate(imagCaputred, imagCaputred, new Mat(), new Point(-1, -1), 3);
                    Imgproc.erode(imagCaputred, imagCaputred, new Mat(), new Point(-1, -1), 1);
//              Imgproc.threshold(imagCaputred, imagCaputred, threshValue, 179.0, Imgproc.THRESH_BINARY);
                 Utils.matToBitmap(imagCaputred, bm);
//              ImageView processedimage = (ImageView) findViewById(R.id.CameraView);
//                processedimage.setImageBitmap(bm);



                Imgproc.Canny(imagCaputred, imagCaputred, canny1_Min, canny2_Max);
               Utils.matToBitmap(imagCaputred, bm);
   //             processedimage.setImageBitmap(bm);


            Imgproc.HoughLinesP(imagCaputred, lines, 1, Math.PI / 180, 97,hough_Min, hough_Max);

                for (int i = 0; i < lines.rows(); i++) {
                    lineCount++;
                    double[] vec = lines.get(i, 0);
                    double x1 = vec[0],
                            y1 = vec[1],
                            x2 = vec[2],
                            y2 = vec[3];

                    Point start = new Point(x1, y1);
                    Point end = new Point(x2, y2);

                    if ((lineCount == 2) && (startCanny == true)) {

                        lineSlope = ((end.y-start.y) / (end.x-start.x));
                        lineIntercept1 = (start.y-(lineSlope*start.x));
                        lineIntercept2 = (oldStart.y-(lineSlope*oldEnd.x));
                        lineDist =  (Math.abs (lineIntercept1-lineIntercept2))/(Math.sqrt((lineSlope*lineSlope)+1));
                        lineDist = lineDist /5;

                        startCanny = false;
 //                       lineCount = 0;
                    }
                    oldStart = start;
                    oldEnd = end;


                    Imgproc.line(frame, start, end, new Scalar(255, 255, 255), 1);

                    double dx = x2 - x1;
                    double dy = y2 - y1;

                }
                Utils.matToBitmap(imagCaputred, bm);



             final TextView mainview = (TextView)findViewById(R.id.textView2);
                  TextView countview = (TextView)findViewById(R.id.textView7);
            String distString = String.format("%.2f", lineDist);
             mainview.setText(distString);
            countview.setText(String.valueOf(lineCount));



            }

        cameraBridgeViewBase.enableView();
        return imagCaputred;

    }




    @Override
    public void onCameraViewStarted(int width, int height) {

    }


    @Override
    public void onCameraViewStopped() {

    }


    @Override
    protected void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()){
            Toast.makeText(getApplicationContext(),"There's a problem, yo!", Toast.LENGTH_SHORT).show();
        }

        else
        {
            baseLoaderCallback.onManagerConnected(baseLoaderCallback.SUCCESS);
        }



    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraBridgeViewBase!=null){

            cameraBridgeViewBase.disableView();
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraBridgeViewBase!=null){
            cameraBridgeViewBase.disableView();
        }
    }*/


}


package com.example.mftool1126;


import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
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


import java.util.Random;
import java.util.Vector;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    CameraBridgeViewBase cameraBridgeViewBase;
    BaseLoaderCallback baseLoaderCallback;
    boolean startCanny = false;
    double lineDist = 2.00;
    double lineSlope = 0.0;
    double lineIntercept1 = 0.0;
    double lineIntercept2 = 0.0;

//    boolean collectFrames = false;
//    Mat measuredImage = new Mat();
 //   ImageView image = (ImageView) findViewById(R.id.CameraView);




    public void Canny(View Button){

        if (startCanny == false){
            startCanny = true;

        }

        else{

            startCanny = false;



         /*   cameraBridgeViewBase.disableView();
            Bitmap bitmap1 = Bitmap.createBitmap(measuredImage.cols(), measuredImage.rows(),Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(measuredImage, bitmap1 );
            image.setImageBitmap(bitmap1);
*/

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

        if (startCanny == true ) {


                Imgproc.cvtColor(imagCaputred, imagCaputred, Imgproc.COLOR_RGBA2GRAY);

                Imgproc.Canny(imagCaputred, imagCaputred, 70, 42);
                Imgproc.GaussianBlur(imagCaputred, imagCaputred, new Size (5,5),2);

                Imgproc.HoughLinesP(imagCaputred, lines, 1, Math.PI / 180, 80, 100, 30);

                for (int i = 0; i < lines.cols(); i++) {

                    double[] vec = lines.get(0, i);
                    double x1 = vec[0],
                            y1 = vec[1],
                            x2 = vec[2],
                            y2 = vec[3];



                    Point start = new Point(x1, y1);
                    Point end = new Point(x2, y2);
                    lineSlope = ((end.y-start.y) / (end.x-end.y));
                    lineIntercept1 = (start.y-(lineSlope*start.x));
                    lineIntercept2 = (oldStart.y-(lineSlope*oldEnd.x));
                    lineDist =  (Math.abs (lineIntercept2-lineIntercept1))/((lineSlope*lineSlope)-1);
                    lineDist = lineDist /19;
                    oldStart = start;
                    oldEnd = end;



                    Imgproc.line(frame, start, end, new Scalar(255, 255, 255), 5);

                    double dx = x2 - x1;
                    double dy = y2 - y1;




                /*               String ldText = Double.toString(dx); ?? move distance to screen
                     */
                }

                final TextView mainview = (TextView)findViewById(R.id.textView2);
                String distString = String.format("%.2f", lineDist);
//                String.format("%.2f",distString);
                mainview.setText(distString);



            }
 //           measuredImage = imagCaputred;

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
    }
}

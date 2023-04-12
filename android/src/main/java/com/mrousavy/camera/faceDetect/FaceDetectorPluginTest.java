package com.mrousavy.camera.faceDetect;


import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;

import com.facebook.react.bridge.ReadableMap;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.common.internal.ImageConvertUtils;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.mrousavy.camera.frameprocessor.FrameProcessorPlugin;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;


public class FaceDetectorPluginTest extends FrameProcessorPlugin {


    //    private Number tempFaceId;
    static Double tempFaceId = -1.00;

    public FaceDetectorPluginTest(@NonNull String name, FaceDetectorOptions options, FaceDetector faceDetector) {
        super(name);
        this.options = options;
        this.faceDetector = faceDetector;
    }

    public FaceDetectorPluginTest(@NonNull String name) {
        super(name);
    }

    FaceDetectorOptions options =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                    .setMinFaceSize(0.15f)
                    .enableTracking()
                    .build();
    FaceDetector faceDetector = FaceDetection.getClient(options);


    public static void register(@NotNull ReadableMap options) {
        Log.d("Third Step", String.valueOf(options));
        JSONObject jsonObject = new JSONObject(options.toHashMap());
        try {
            tempFaceId = (Double) jsonObject.get("FaceId");
            Log.d("Third Step Success", String.valueOf(tempFaceId));
        } catch (JSONException e) {
            Log.d("Third Step Failed", e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private WritableMap processBoundingBox(Rect boundingBox) {
        WritableMap bounds = Arguments.createMap();

        bounds.putDouble("x", boundingBox.left);
        bounds.putDouble("y", boundingBox.top);

        bounds.putDouble("width", boundingBox.width());
        bounds.putDouble("height", boundingBox.height());

        return bounds;
    }

    private WritableMap processFaceContours(Face face) {
        // All faceContours
        int[] faceContoursTypes =
                new int[]{
                        FaceContour.FACE,
                        FaceContour.LEFT_EYEBROW_TOP,
                        FaceContour.LEFT_EYEBROW_BOTTOM,
                        FaceContour.RIGHT_EYEBROW_TOP,
                        FaceContour.RIGHT_EYEBROW_BOTTOM,
                        FaceContour.LEFT_EYE,
                        FaceContour.RIGHT_EYE,
                        FaceContour.UPPER_LIP_TOP,
                        FaceContour.UPPER_LIP_BOTTOM,
                        FaceContour.LOWER_LIP_TOP,
                        FaceContour.LOWER_LIP_BOTTOM,
                        FaceContour.NOSE_BRIDGE,
                        FaceContour.NOSE_BOTTOM,
                        FaceContour.LEFT_CHEEK,
                        FaceContour.RIGHT_CHEEK
                };
        String[] faceContoursTypesStrings = {
                "FACE",
                "LEFT_EYEBROW_TOP",
                "LEFT_EYEBROW_BOTTOM",
                "RIGHT_EYEBROW_TOP",
                "RIGHT_EYEBROW_BOTTOM",
                "LEFT_EYE",
                "RIGHT_EYE",
                "UPPER_LIP_TOP",
                "UPPER_LIP_BOTTOM",
                "LOWER_LIP_TOP",
                "LOWER_LIP_BOTTOM",
                "NOSE_BRIDGE",
                "NOSE_BOTTOM",
                "LEFT_CHEEK",
                "RIGHT_CHEEK"
        };
        WritableMap faceContoursTypesMap = new WritableNativeMap();
        for (int i = 0; i < faceContoursTypesStrings.length; i++) {
            FaceContour contour = face.getContour(faceContoursTypes[i]);
            List<PointF> points = contour.getPoints();
            WritableNativeArray pointsArray = new WritableNativeArray();
            for (int j = 0; j < points.size(); j++) {
                WritableMap currentPointsMap = new WritableNativeMap();
                currentPointsMap.putDouble("x", points.get(j).x);
                currentPointsMap.putDouble("y", points.get(j).y);
                pointsArray.pushMap(currentPointsMap);
            }
            faceContoursTypesMap.putArray(faceContoursTypesStrings[contour.getFaceContourType() - 1], pointsArray);
        }
        return faceContoursTypesMap;
    }

    @SuppressLint("NewApi")
    @Override
    public Object callback(ImageProxy frame, Object[] params) {
        @SuppressLint("UnsafeOptInUsageError")
        Image mediaImage = frame.getImage();

        if (mediaImage != null) {
            try {
                InputImage image = InputImage.fromMediaImage(mediaImage, frame.getImageInfo().getRotationDegrees());
                Task<List<Face>> task = faceDetector.process(image);
                WritableNativeArray array = new WritableNativeArray();
                List<Face> faces = Tasks.await(task);
                Bitmap bmpFrameResult = ImageConvertUtils.getInstance().getUpRightBitmap(image);
                int paddingTop = (int) (150 / 1.5);
                int paddingBottom = (int) (100 / 1.5);
                int paddingRight = (int) (40 / 1.5);
                int paddingLeft = (int) (40 / 1.5);

                for (Face face : faces) {
                    WritableMap map = new WritableNativeMap();
                    final RectF faceBB = new RectF(face.getBoundingBox());
                    int tempHeight = (int) faceBB.height() + paddingTop + paddingBottom;
                    int tempWidth = (int) faceBB.width() + paddingRight + paddingLeft;
                    Bitmap bmpFaceResult = Bitmap.createBitmap(tempWidth, tempHeight, Bitmap.Config.ARGB_8888);
                    final Canvas cvFace = new Canvas(bmpFaceResult);
                    float sx = ((float) tempWidth / 2) / faceBB.width();
                    float sy = ((float) tempHeight / 2) / faceBB.height();
                    Matrix matrix = new Matrix();
                    matrix.postTranslate((-faceBB.left + paddingLeft), (-faceBB.top + paddingTop));
                    cvFace.drawBitmap(bmpFrameResult, matrix, null);

                    map.putDouble("rollAngle", face.getHeadEulerAngleZ()); // Head is rotated to the left rotZ degrees
                    map.putDouble("pitchAngle", face.getHeadEulerAngleX()); // Head is rotated to the right rotX degrees
                    map.putDouble("yawAngle", face.getHeadEulerAngleY());  // Head is tilted sideways rotY degrees
                    WritableMap bounds = processBoundingBox(face.getBoundingBox());
                    map.putMap("bounds", bounds);
                    Log.d("asdsdg","hello");
                    map.putInt("faceId", face.getTrackingId());
                    if (tempFaceId.equals(Double.valueOf(face.getTrackingId()))) {
                        String temp = new Convert().getBase64Image(bmpFaceResult);
                        String imageResult = temp.replaceAll("\\s", "");
                        map.putString("imageResult", imageResult);
                        tempFaceId = -1.00;
                    }
                    array.pushMap(map);
                }
                return array;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }


//    FaceDetectorPluginTest(String s) {
//        super("facesScanned");
//    }
}
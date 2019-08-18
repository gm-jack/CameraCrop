//
// Created by qiulinmin on 8/1/17.
//
#include <jni.h>
#include <string>
#include <android_utils.h>
#include <Scanner.h>

using namespace std;

static const char* const kClassDocScanner = "me/pqpo/smartcropperlib/SmartCropper";

static struct {
    jclass jClassPoint;
    jmethodID jMethodInit;
    jfieldID jFieldIDX;
    jfieldID jFieldIDY;
} gPointInfo;

static void initClassInfo(JNIEnv *env) {
    gPointInfo.jClassPoint = reinterpret_cast<jclass>(env -> NewGlobalRef(env -> FindClass("android/graphics/Point")));
    gPointInfo.jMethodInit = env -> GetMethodID(gPointInfo.jClassPoint, "<init>", "(II)V");
    gPointInfo.jFieldIDX = env -> GetFieldID(gPointInfo.jClassPoint, "x", "I");
    gPointInfo.jFieldIDY = env -> GetFieldID(gPointInfo.jClassPoint, "y", "I");
}

static jobject createJavaPoint(JNIEnv *env, Point point_) {
    return env -> NewObject(gPointInfo.jClassPoint, gPointInfo.jMethodInit, point_.x, point_.y);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_pqpo_smartcropperlib_SmartCropper_nativeScan(JNIEnv *env, jclass type, jobject srcBitmap, jobjectArray outPoint_, jboolean canny) {
     if (env -> GetArrayLength(outPoint_) != 4) {
         return;
     }
     Mat srcBitmapMat;
     bitmap_to_mat(env, srcBitmap, srcBitmapMat);
     Mat bgrData(srcBitmapMat.rows, srcBitmapMat.cols, CV_8UC3);
     cvtColor(srcBitmapMat, bgrData, COLOR_RGBA2BGR);
     scanner::Scanner docScanner(bgrData, canny);
     std::vector<Point> scanPoints = docScanner.scanPoint();
     if (scanPoints.size() == 4) {
         for (int i = 0; i < 4; ++i) {
             env -> SetObjectArrayElement(outPoint_, i, createJavaPoint(env, scanPoints[i]));
         }
     }
}

static vector<Point> pointsToNative(JNIEnv *env, jobjectArray points_) {
    int arrayLength = env->GetArrayLength(points_);
    vector<Point> result;
    for(int i = 0; i < arrayLength; i++) {
        jobject point_ = env -> GetObjectArrayElement(points_, i);
        int pX = env -> GetIntField(point_, gPointInfo.jFieldIDX);
        int pY = env -> GetIntField(point_, gPointInfo.jFieldIDY);
        result.push_back(Point(pX, pY));
    }
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_me_pqpo_smartcropperlib_SmartCropper_nativeCrop(JNIEnv *env, jclass type, jobject srcBitmap, jobjectArray points_, jobject outBitmap) {
    std::vector<Point> points = pointsToNative(env, points_);
    if (points.size() != 4) {
        return;
    }
    Point leftTop = points[0];
    Point rightTop = points[1];
    Point rightBottom = points[2];
    Point leftBottom = points[3];

    Mat srcBitmapMat;
    bitmap_to_mat(env, srcBitmap, srcBitmapMat);

    AndroidBitmapInfo outBitmapInfo;
    AndroidBitmap_getInfo(env, outBitmap, &outBitmapInfo);
    Mat dstBitmapMat;
    int newHeight = outBitmapInfo.height;
    int newWidth = outBitmapInfo.width;
    dstBitmapMat = Mat::zeros(newHeight, newWidth, srcBitmapMat.type());

    std::vector<Point2f> srcTriangle;
    std::vector<Point2f> dstTriangle;

    srcTriangle.push_back(Point2f(leftTop.x, leftTop.y));
    srcTriangle.push_back(Point2f(rightTop.x, rightTop.y));
    srcTriangle.push_back(Point2f(leftBottom.x, leftBottom.y));
    srcTriangle.push_back(Point2f(rightBottom.x, rightBottom.y));

    dstTriangle.push_back(Point2f(0, 0));
    dstTriangle.push_back(Point2f(newWidth, 0));
    dstTriangle.push_back(Point2f(0, newHeight));
    dstTriangle.push_back(Point2f(newWidth, newHeight));

    Mat transform = getPerspectiveTransform(srcTriangle, dstTriangle);
    warpPerspective(srcBitmapMat, dstBitmapMat, transform, dstBitmapMat.size());

    mat_to_bitmap(env, dstBitmapMat, outBitmap);
}

extern "C"
JNIEXPORT jdouble JNICALL
Java_me_pqpo_smartcropperlib_SmartCropper_nativeBlurry(JNIEnv *env, jclass type, jobject srcBitmap) {
    Mat srcBitmapMat;
    Mat sobelBitmapMat;
    bitmap_to_mat(env, srcBitmap, srcBitmapMat);
    Mat bgrData(srcBitmapMat.rows, srcBitmapMat.cols, CV_8UC3);
    cvtColor(srcBitmapMat, bgrData, COLOR_RGB2GRAY);
    Laplacian(bgrData,sobelBitmapMat,CV_16U);
    return mean(sobelBitmapMat).val[0];
}

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_me_pqpo_smartcropperlib_SmartCropper_nativeLight(JNIEnv *env, jclass type, jobject srcBitmap) {
    Mat srcBitmapMat;
    Mat GRAYimg;
    bitmap_to_mat(env, srcBitmap, srcBitmapMat);
    Mat bgrData(srcBitmapMat.rows, srcBitmapMat.cols, CV_8UC3);
    cvtColor(bgrData, GRAYimg, COLOR_BGR2GRAY);
    float a = 0;
    //da大于0表示过亮，da小于0表示过暗
    float da = 0;
    float cast = 1;
    int Hist[256];
    for (int i = 0; i < 256; i++){
        Hist[i] = 0;
    }
    for (int i = 0; i < GRAYimg.rows; i++) {
        for (int j = 0; j < GRAYimg.cols; j++) {
            a += int(GRAYimg.at<uchar>(i, j)-128);//在计算过程中，考虑128为亮度均值点
            int x = GRAYimg.at<uchar>(i, j);
            Hist[x]++;
        }
    }
    da = a /float(GRAYimg.rows * bgrData.cols);
    float D = abs(da);
    float Ma = 0;
    for (int i = 0; i < 256; i++) {
        Ma += abs(i - 128 - da) * Hist[i];
    }
    Ma /=float((GRAYimg.rows * GRAYimg.cols));
    float M = abs(Ma);
    float K = D / M;
    cast = K;
    jfloatArray result;
    result = env->NewFloatArray(2);
    if (result == NULL) {
        return NULL; /* out of memory error thrown */
    }
    jfloat data[2];
    data[0] = cast;
    data[1] = da;
    env->SetFloatArrayRegion(result, 0, 2, data);
    return result;
}

extern "C"
JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv *env = NULL;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        return JNI_FALSE;
    }
    initClassInfo(env);
    return JNI_VERSION_1_4;
}

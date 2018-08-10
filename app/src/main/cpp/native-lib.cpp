#ifdef PRODUCT_RIDDELLOO
#include <jni.h>
#include <string>
#include <android/bitmap.h>
#include <RenderScript.h>
#include <vector>

#endif
using namespace android::RSC;
namespace android::RSC {
    void compute(void *input, uint32_t X, uint32_t Y, void *&outputPtr) {
        sp<RS> rs = new RS();
        sp<const Type> yuvType = Type::create(rs, Element::U8(rs), X, Y, 0);

        sp<Allocation> inputAlloc = Allocation::createTyped(rs, yuvType, RS_ALLOCATION_MIPMAP_NONE,
                                                            RS_ALLOCATION_USAGE_SCRIPT);

        sp<const Type> rgbaType = Type::create(rs, Element::RGBA_8888(rs), X, Y, 0);
        sp<Allocation> outAlloc = Allocation::createTyped(rs, rgbaType, RS_ALLOCATION_MIPMAP_NONE,
                                                          RS_ALLOCATION_USAGE_SCRIPT);


        inputAlloc->copy2DRangeFrom(0, 0, X, Y, input);
        sp<ScriptIntrinsicYuvToRGB> yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB::create(rs,
                                                                                        Element::U8_4(
                                                                                                rs));
        yuvToRgbIntrinsic->setInput(inputAlloc);
        yuvToRgbIntrinsic->forEach(outAlloc);

        outAlloc->copy2DRangeTo(0, 0, X, Y, outputPtr);

    }
}

extern "C" {

JNIEXPORT jstring JNICALL
Java_liewjuntung_org_ndkrenderscript_MainActivity_getStringFromJNI(
        JNIEnv *env,
        jobject instance) {
    return env->NewStringUTF("Hello From JNI");
}



JNIEXPORT void JNICALL
Java_liewjuntung_org_ndkrenderscript_MainActivity_convertBitmap(
        JNIEnv *env,
        jobject instance,
        jstring pathObj, jint X,
        jint Y, jobject jbitmapIn,
        jobject jbitmapOut) {
    void *inputPtr = NULL;
    void *outputPtr = NULL;

    AndroidBitmap_lockPixels(env, jbitmapIn, &inputPtr);
    AndroidBitmap_lockPixels(env, jbitmapOut, &outputPtr);

    const char *path = env->GetStringUTFChars(pathObj, NULL);

    sp<RS> rs = new RS();
    rs->init(path);
    env->ReleaseStringUTFChars(pathObj, path);

    sp<const Element> e = Element::RGBA_8888(rs);

    sp<const Type> t = Type::create(rs, e, static_cast<uint32_t>(X), static_cast<uint32_t>(Y), 0);

    sp<Allocation> inputAlloc = Allocation::createTyped(rs, t, RS_ALLOCATION_MIPMAP_NONE,
                                                        RS_ALLOCATION_USAGE_SHARED |
                                                        RS_ALLOCATION_USAGE_SCRIPT,
                                                        inputPtr);
    sp<Allocation> outputAlloc = Allocation::createTyped(rs, t, RS_ALLOCATION_MIPMAP_NONE,
                                                         RS_ALLOCATION_USAGE_SHARED |
                                                         RS_ALLOCATION_USAGE_SCRIPT,
                                                         outputPtr);

    sp<ScriptIntrinsicBlur> sc = ScriptIntrinsicBlur::create(rs, Element::U8_4(rs));

    inputAlloc->copy2DRangeFrom(0, 0, static_cast<uint32_t>(X), static_cast<uint32_t>(Y), inputPtr);
    //    ScriptC_mono* sc = new ScriptC_mono(rs);//new ScriptC_mono(rs);
    sc->setRadius(23.0f);
    sc->setInput(inputAlloc);
    sc->forEach(outputAlloc);
    outputAlloc->copy2DRangeTo(0, 0, static_cast<uint32_t>(X), static_cast<uint32_t>(Y), outputPtr);


    AndroidBitmap_unlockPixels(env, jbitmapIn);
    AndroidBitmap_unlockPixels(env, jbitmapOut);
}
extern "C" {
JNIEXPORT void JNICALL
Java_liewjuntung_org_ndkrenderscript_MainActivity_convertYuvBitmap(
        JNIEnv *env,
        jobject instance,
        jstring pathObj, jfloat size, jint X,
        jint Y, jobject jbyteBuffer,
        jobject jbitmapOut) {

    void *outputPtr = NULL;
    AndroidBitmap_lockPixels(env, jbitmapOut, &outputPtr);
    void *byteBuffer = env->GetDirectBufferAddress(jbyteBuffer);

    const char *path = env->GetStringUTFChars(pathObj, NULL);

    android::RSC::sp<android::RSC::RS> rs = new android::RSC::RS();
    rs->init(path);

    android::RSC::sp<android::RSC::ScriptIntrinsicYuvToRGB> yuvToRgbIntrinsic = android::RSC::ScriptIntrinsicYuvToRGB::create(
            rs, android::RSC::Element::U8_4(rs));


    android::RSC::sp<const android::RSC::Type> yuvType = android::RSC::Type::create(rs,
                                                                                    android::RSC::Element::U8(
                                                                                            rs),
                                                                                    static_cast<uint32_t>(size),
                                                                                    0, 0);

    android::RSC::sp<android::RSC::Allocation> inputAlloc = android::RSC::Allocation::createTyped(
            rs, yuvType, RS_ALLOCATION_MIPMAP_FULL, RS_ALLOCATION_USAGE_SCRIPT);

    android::RSC::sp<const android::RSC::Type> rgbaType = android::RSC::Type::create(rs,
                                                                                     android::RSC::Element::RGBA_8888(
                                                                                             rs),
                                                                                     static_cast<uint32_t>(X),
                                                                                     static_cast<uint32_t>(Y),
                                                                                     0);

    android::RSC::sp<android::RSC::Allocation> outAlloc = android::RSC::Allocation::createTyped(rs,
                                                                                                rgbaType,
                                                                                                RS_ALLOCATION_MIPMAP_FULL,
                                                                                                RS_ALLOCATION_USAGE_SCRIPT);

    inputAlloc->copy1DFrom(byteBuffer);
    //
    // inputAlloc->copy2DRangeFrom(0, 0, X, Y, yuvInput);

    yuvToRgbIntrinsic->setInput(inputAlloc);
    yuvToRgbIntrinsic->forEach(outAlloc);
    outAlloc->copy2DRangeTo(0, 0, static_cast<uint32_t>(X), static_cast<uint32_t>(Y), outputPtr);
    AndroidBitmap_unlockPixels(env, jbitmapOut);
}

JNIEXPORT jobject JNICALL
Java_liewjuntung_org_ndkrenderscript_MainActivity_convertYuv(
        JNIEnv *env,
        jobject instance,
        jstring pathObj, jfloat size, jint X,
        jint Y, jobject jbyteBuffer) {

    void *outputPtr = NULL;
//    AndroidBitmap_lockPixels(env, jbitmapOut, &outputPtr);
    void *byteBuffer = env->GetDirectBufferAddress(jbyteBuffer);

    const char *path = env->GetStringUTFChars(pathObj, NULL);

    android::RSC::sp<android::RSC::RS> rs = new android::RSC::RS();
    rs->init(path);

    android::RSC::sp<android::RSC::ScriptIntrinsicYuvToRGB> yuvToRgbIntrinsic = android::RSC::ScriptIntrinsicYuvToRGB::create(
            rs, android::RSC::Element::U8_4(rs));


    android::RSC::sp<const android::RSC::Type> yuvType = android::RSC::Type::create(rs,
                                                                                    android::RSC::Element::U8(
                                                                                            rs),
                                                                                    static_cast<uint32_t>(size),
                                                                                    0, 0);

    android::RSC::sp<android::RSC::Allocation> inputAlloc = android::RSC::Allocation::createTyped(
            rs, yuvType, RS_ALLOCATION_MIPMAP_NONE, RS_ALLOCATION_USAGE_SCRIPT);

    android::RSC::sp<const android::RSC::Type> rgbaType = android::RSC::Type::create(rs,
                                                                                     android::RSC::Element::RGBA_8888(
                                                                                             rs),
                                                                                     static_cast<uint32_t>(X),
                                                                                     static_cast<uint32_t>(Y),
                                                                                     0);

    android::RSC::sp<android::RSC::Allocation> outAlloc = android::RSC::Allocation::createTyped(rs,
                                                                                                rgbaType,
                                                                                                RS_ALLOCATION_MIPMAP_NONE,
                                                                                                RS_ALLOCATION_USAGE_SCRIPT);

    inputAlloc->copy1DFrom(byteBuffer);
    //
    // inputAlloc->copy2DRangeFrom(0, 0, X, Y, yuvInput);

    yuvToRgbIntrinsic->setInput(inputAlloc);
    yuvToRgbIntrinsic->forEach(outAlloc);
    outAlloc->copy2DRangeTo(0, 0, static_cast<uint32_t>(X), static_cast<uint32_t>(Y), outputPtr);
//    AndroidBitmap_unlockPixels(env, jbitmapOut);
    jobject jbuffer = env->NewDirectByteBuffer(outputPtr, (X * Y * 2));
    return jbuffer;
}
android::RSC::sp<android::RSC::RS> rs = new android::RSC::RS();
void *outputPtr = new char[30000000];

JNIEXPORT void JNICALL
Java_liewjuntung_org_ndkrenderscript_MainActivity_convertSplitYuvBitmap(
        JNIEnv *env,
        jobject instance,
        jstring pathObj,
        jint X,
        jint Y,
        jobject yBuffer,
        jobject uBuffer,
        jobject vBuffer,
        jint ySize,
        jint uSize,
        jint vSize
) {
//    void *bitmapOutputPtr = NULL;
//    AndroidBitmap_lockPixels(env, jbitmapOut, &bitmapOutputPtr);
    void *yByteBuffer = env->GetDirectBufferAddress(yBuffer);
    void *uByteBuffer = env->GetDirectBufferAddress(uBuffer);
    void *vByteBuffer = env->GetDirectBufferAddress(vBuffer);

    const char *path = env->GetStringUTFChars(pathObj, NULL);

    //vector
    static std::vector<char> yuvVector(ySize + uSize + vSize);

    //yuvVector.reserve(static_cast<unsigned int>(ySize + uSize + vSize));
    //step 2: concat the vector
    void *yuvInput = yuvVector.data();
    memcpy(yuvVector.data(), yByteBuffer, ySize);
    memcpy(yuvVector.data() + ySize, uByteBuffer, uSize);
    memcpy(yuvVector.data() + ySize + uSize, vByteBuffer, vSize);
    //step 3: convert vector to void*


    rs->init(path);

    android::RSC::sp<android::RSC::ScriptIntrinsicYuvToRGB> yuvToRgbIntrinsic = android::RSC::ScriptIntrinsicYuvToRGB::create(
            rs, android::RSC::Element::U8_4(rs));


    android::RSC::sp<const android::RSC::Type> yuvType = android::RSC::Type::create(rs,
                                                                                    android::RSC::Element::U8(
                                                                                            rs),
                                                                                    static_cast<uint32_t>(yuvVector.size()),
                                                                                    0, 0);

    android::RSC::sp<android::RSC::Allocation> inputAlloc = android::RSC::Allocation::createTyped(
            rs, yuvType, RS_ALLOCATION_MIPMAP_NONE, RS_ALLOCATION_USAGE_SCRIPT);

    android::RSC::sp<const android::RSC::Type> rgbaType = android::RSC::Type::create(rs,
                                                                                     android::RSC::Element::RGBA_8888(
                                                                                             rs),
                                                                                     static_cast<uint32_t>(X),
                                                                                     static_cast<uint32_t>(Y),
                                                                                     0);

    android::RSC::sp<android::RSC::Allocation> outAlloc = android::RSC::Allocation::createTyped(rs,
                                                                                                rgbaType,
                                                                                                RS_ALLOCATION_MIPMAP_NONE,
                                                                                                RS_ALLOCATION_USAGE_SCRIPT);

    inputAlloc->copy1DFrom(yuvInput);

    yuvToRgbIntrinsic->setInput(inputAlloc);
    yuvToRgbIntrinsic->forEach(outAlloc);
    outAlloc->copy2DRangeTo(0, 0, static_cast<uint32_t>(X), static_cast<uint32_t>(Y), outputPtr);
//    return env->NewDirectByteBuffer(outputPtr, (X * Y) * 4);
}
}
}




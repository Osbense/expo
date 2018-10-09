// Copyright 2018-present 650 Industries. All rights reserved.

package expo.modules.ar;

import android.content.Context;
import android.os.Bundle;

import java.util.Map;

import expo.core.ExportedModule;
import expo.core.ModuleRegistry;
import expo.core.Promise;
import expo.core.interfaces.ExpoMethod;
import expo.core.interfaces.ModuleRegistryConsumer;
import expo.core.interfaces.services.UIManager;
import expo.modules.gl.GLView;

public class ARModule extends ExportedModule implements ModuleRegistryConsumer, ARSessionManagerDelegate {
  private static final String TAG = "ExpoAR";
  private static final String ERROR_TAG = "E_AR";

  private GLView mGLView;
  private ARSessionManager mARSessionManager;
  private ModuleRegistry mModuleRegistry;
  private UIManager mUImanager;

  ARModule(Context context) {
    super(context);
  }

  @Override
  public String getName() {
    return TAG;
  }

  @Override
  public void setModuleRegistry(ModuleRegistry moduleRegistry) {
    mModuleRegistry = moduleRegistry;
    mUImanager = mModuleRegistry.getModule(UIManager.class);
  }

  @ExpoMethod
  public void startAsync(final int glViewTag, final String configuration, final Promise promise) {
    if (mUImanager == null) {
      promise.reject(ERROR_TAG + "_INVALID", new IllegalStateException("Implementation of " + UIManager.class.getName() + " is null. Are you sure you've included a proper Expo adapter for your platform?"));
      return;
    }

    mUImanager.addUIBlock(glViewTag, new UIManager.UIBlock<GLView>() {
      @Override
      public void resolve(GLView view) {
        mGLView = view;

        if (mARSessionManager != null) {
          mARSessionManager.stop();
          mARSessionManager = null;
        }

        mARSessionManager = new ARSessionManager(mModuleRegistry);
        mARSessionManager.startWithGLView(mGLView, promise);


//        mARSessionManager.mDelegate = this;
//        view.setARSessionManager(mARSessionManager); // TODO handle stopping upon destruction
      }

      @Override
      public void reject(Throwable throwable) {
        promise.reject(ERROR_TAG + "_BAD_VIEW_TAG", "ExponentGLObjectManager.createCameraTextureAsync: Expected a GLView");
      }
    }, GLView.class);
  }

  @ExpoMethod
  public void stopAsync(Promise promise) {
    if (!sessionExistsOrReject(promise)) return;
    mARSessionManager.stop();
    promise.resolve(null);
  }

  @ExpoMethod
  public void pauseAsync(Promise promise) {
    if (!sessionExistsOrReject(promise)) return;
    mARSessionManager.pause();
    promise.resolve(null);
  }

  @ExpoMethod
  public void resumeAsync(Promise promise) {
    if (!sessionExistsOrReject(promise)) return;
    mARSessionManager.resume(promise);
  }

  @ExpoMethod
  public void resetAsync(Promise promise) {
    if (!sessionExistsOrReject(promise)) return;
    //TODO:Bacon:...
    promise.resolve(null);
  }
//
//
//  @ExpoMethod
//  public void getARCoreStatus(Promise promise) {
//    String status = "unknown";
//    Boolean isSupported = false;
//
//    switch (ArCoreApk.getInstance().checkAvailability(getContext())) {
//      case UNSUPPORTED_DEVICE_NOT_CAPABLE:
//        status = "notCapable";
//        break;
//      case SUPPORTED_NOT_INSTALLED:
//        status = "notInstalled";
//        isSupported = true;
//        break;
//      case SUPPORTED_INSTALLED:
//        status = "installed";
//        isSupported = true;
//        break;
//    }
//
//    Bundle map = new Bundle();
//    map.putString("status", status);
//    map.putBoolean("isSupported", isSupported);
//    promise.resolve(map);
//  }


  @ExpoMethod
  public void performHitTestAsync(Map<String, Number> point, String types, Promise promise) {
    if (!sessionExistsOrReject(promise)) return;

    if (point.containsKey("x") && point.containsKey("y")) {
      mARSessionManager.performHitTestAsync(point.get("x").floatValue(), point.get("y").floatValue(), types, promise);
    } else {
      //TODO:Bacon...
      promise.reject("", "");
    }

  }
//
  @ExpoMethod
  public void getCurrentFrameAsync(Map<String, Object> attributes, Promise promise) {
    if (!sessionExistsOrReject(promise)) {
      return;
    }

    mARSessionManager.getCurrentFrameAsync(attributes, promise);
  }
//

  private boolean sessionExistsOrReject(Promise promise) {
    if (mARSessionManager != null) {
      return true;
    }
    promise.reject("E_NO_SESSION", "AR Session is not initialized");
    return false;
  }

  @ExpoMethod
  public void getMatricesAsync(Number zNear, Number zFar, Promise promise) {
    if (!sessionExistsOrReject(promise)) return;
    mARSessionManager.getProjectionMatrix(zNear.floatValue(), zFar.floatValue(), promise);
  }
//
  @ExpoMethod
  public void getCameraTextureAsync(Promise promise) {
    if (!sessionExistsOrReject(promise)) return;
    mARSessionManager.getCameraTextureAsync(promise);
  }
//
//  @ExpoMethod
//  public void setDetectionImagesAsync(Bundle images, Promise promise) {
//    promise.resolve(new Bundle());
//  }

  @Override
  public void didUpdateWithEvent(String eventName, Bundle payload) {

  }
}

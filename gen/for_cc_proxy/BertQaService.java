/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: ./tools/out/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation -Werror -Wno-mixed-oneway --min_sdk_version current --ninja -o out/external/odcc-tf-lite-bert-qa/service-app/java/org/tensorflow/lite/examples/bertqaservice -N . -N ./tools/src/external/odcc-tf-lite-bert-qa/common/aidl ./tools/src/external/odcc-tf-lite-bert-qa/common/aidl/org/tensorflow/lite/examples/bertqaservice/IBertQaInterface.aidl
 */
package org.tensorflow.lite.examples.bertqaservice;
  
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import com.islet.binder.service.Reader;
import com.islet.binder.service.Logger;
import com.islet.binder.service.VmManager;
import com.islet.binder.service.IRealmService;
import org.tensorflow.lite.examples.bertqaservice.IBertQaInterface;

public class BertQaService extends Service implements VmManager.VmServiceCallback
{
  private static final String TAG = "BertQaService";
  private Logger mLogger;
  private VmManager mVmManager;
  private IBertQaInterface mTargetService = null;

  private final IBertQaInterface.Stub mBinder = new IBertQaInterface.Stub() {
  @Override public void setOptions(int currentDelegate, int numThreads) throws RemoteException
  {
    mLogger.write(String.format("Entering %s(%d, %d)", "mBinder.setOptions", currentDelegate, numThreads));
    if (mTargetService != null) {
      mTargetService.setOptions(currentDelegate, numThreads);
    } else {
      mLogger.write("Target service is not available yet");
      throw new RemoteException("Target service is not available");
    }
    mLogger.write("mBinder.setOptions()");
  }
  @Override public java.util.List<org.tensorflow.lite.examples.bertqaservice.QaAnswerData> answer(java.lang.String context, java.lang.String question) throws RemoteException
  {
    mLogger.write(String.format("Entering %s(%s, %s)", "mBinder.answer", context, question));
    if (mTargetService != null) {
      java.util.List<org.tensorflow.lite.examples.bertqaservice.QaAnswerData> ret = mTargetService.answer(context, question);
      mLogger.write("answer() returning: " + ret);
      return ret;
    } else {
      mLogger.write("Target service is not available yet");
      throw new RemoteException("Target service is not available");
    }
  }
  };

  @Override
  public void onCreate() {
      mLogger = new Logger(this, TAG, "service2.txt");
      mVmManager = new VmManager(getApplication(), mLogger);
      mLogger.write("Entering onCreate();");

      // Set VM service callback to receive notifications
      mVmManager.setVmServiceCallback(this);

      mVmManager.vmRun();
      // we cannot call mVmManager.getVMService().onCreate() here as the service is not connected yet

      mLogger.write("Returning onCreate();");
  }

  @Override
  public IBinder onBind(Intent intent) {
      mLogger.write("Entering onBind();");

      // we cannot call mVmManager.getVMService().onBind() here as the service is not connected yet

      mLogger.write("Returning onBind();");
      return mBinder;
  }

  @Override
  public boolean onUnbind(Intent intent) {
      mLogger.write("Entering onUnbind();");
      mLogger.write("Returning onUnbind();");
      return false; // return true if you want onRebind() to be called later
  }

  @Override
  public void onDestroy() {
      mLogger.write("Entering onDestroy();");

      mVmManager.vmStop();

      mLogger.write("Returning onDestroy();");
  }

    // VmServiceCallback implementation
    @Override
    public void onVmServiceReady(IRealmService vmService) {
      mLogger.write("onVmServiceReady is called, please wait for binding the target service..");

      // You can perform additional initialization work here when VM service is ready.
      // For example: call specific VM service methods, set up states, etc.
      try {
        // Call onBindForTargetService to get the target service binder
        IBinder targetBinder = vmService.onBindForTargetService();
        if (targetBinder != null) {
                  mTargetService = IBertQaInterface.Stub.asInterface(targetBinder);

          mLogger.write("Target service binder successfully obtained and converted.");
        } else {
          mLogger.write("Failed to obtain target service binder - binder is null.");
        }
      } catch (Exception e) {
        mLogger.write("Error in onVmServiceReady: " + e.getMessage());
      }
    }

    @Override
    public void onVmServiceError(String errorMessage) {
      mLogger.write("VM service error occurred: " + errorMessage);

      // You can implement error handling logic here when VM service connection fails.
      // For example: retry logic, notify user, fallback behavior, etc.
      mLogger.write("Handling VM service connection error...");
    }
}

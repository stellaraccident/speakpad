package net.rcode.speakpad;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;

/**
 * Stuff that should have been in the platform.
 * @author stella
 *
 */
public class AbstractActivity extends Activity {
  public static final String LOG_TAG = "SPEAKPAD";
  

  protected int nextCorrelationId=1;
  protected Map<Integer, ActivityResultCallback> subActivityCallbacks;
  
  public static interface ActivityResultCallback {
    public void onResult(int resultCode, Intent data);
  }
  
  /**
   * Launch a sub activity with a callback registered inline vs crazy correlation id foo.
   * @param subActivityClass
   * @param callback
   */
  public void launchSubActivity(Intent intent, ActivityResultCallback callback) {
    if (subActivityCallbacks==null) subActivityCallbacks=new HashMap<Integer, AbstractActivity.ActivityResultCallback>();
    int correlationId=nextCorrelationId++;
    subActivityCallbacks.put(Integer.valueOf(correlationId), callback);
    startActivityForResult(intent, correlationId);
  }
  
  @Override
  protected void onActivityResult(int correlationId, int resultCode, Intent data) {
    ActivityResultCallback callback=null;
    if (subActivityCallbacks!=null) {
      callback=subActivityCallbacks.remove(Integer.valueOf(correlationId));
    }
    
    if (callback!=null) {
      callback.onResult(resultCode, data);
    }
  }
}

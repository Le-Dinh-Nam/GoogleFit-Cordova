import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import android.util.Log;
import android.provider.Settings;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DataSourcesResult;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
 
public class GFitPlugin extends CordovaPlugin {
     
    public static final String TAG = "Google Fit Plugin";

	private GoogleApiClient googleApiClient;
     
    /**
    * Constructor.
    */
    public GFitPlugin() {}
     
    /**
    * Sets the context of the Command. This can then be used to do things like
    * get file paths associated with the Activity.
    *
    * @param cordova The context of the main Activity.
    * @param webView The CordovaWebView Cordova is running in.
    */
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Log.v(TAG,"Init GFitPlugin");
    }

	protected void connect(final CallbackContext callback) {
		if (googleApiClient != null && googleApiClient.isConnecting()) {
			// TODO
		}

		if (googleApiClient != null && googleApiClient.isConnected()) {
			Log.i(TAG, "Already connected successfully.");
			callback.success();
			return;
		}

		if (googleApiClient == null) {
			googleApiClient = new GoogleApiClient.Builder(getActivity())
					.addApi(Fitness.RECORDING_API)
					.addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
					.build();
		}

		googleApiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
			@Override
			public void onConnected(Bundle bundle) {
				Log.i(TAG, "Connected successfully. Bundle: " + bundle);
				subscribe();
				callback.success();
			}

			@Override
			public void onConnectionSuspended(int statusCode) {
				Log.i(TAG, "Connection suspended.");
				callback.error(Connection.getStatusString(statusCode));
			}
		});

		googleApiClient.registerConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
			@Override
			public void onConnectionFailed(ConnectionResult connectionResult) {
				Log.i(TAG, "Connection failed: " + connectionResult);

				if (connectionResult.hasResolution()) {
					try {
						Log.i(TAG, "Start oauth login...");

						cordova.setActivityResultCallback(GFitPlugin.this);
						connectionResult.startResolutionForResult(getActivity(), REQUEST_OAUTH);

					} catch (IntentSender.SendIntentException e) {
						Log.i(TAG, "OAuth login failed", e);

						callback.error(Connection.getStatusString(connectionResult.getErrorCode()));
					}
				} else {
					// Show the localized error dialog
					Log.i(TAG, "Show error dialog!");

					GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), getActivity(), 0).show();

					callback.error(Connection.getStatusString(connectionResult.getErrorCode()));
				}
			}
		});

		Log.i(TAG, "Will connect...");
		googleApiClient.connect();
	}


	public void subscribe() {
        // To create a subscription, invoke the Recording API. As soon as the subscription is
        // active, fitness data will start recording.
        Fitness.RecordingApi.subscribe(client, DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            if (status.getStatusCode()
                                    == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                                Log.i("TAG", "Existing subscription for activity detected.");
                            } else {
                                Log.i("TAG", "Successfully subscribed!");
                            }
                        } else {
                            Log.w("TAG", "There was a problem subscribing.");
                        }
                    }
                });
    }
     

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.w(TAG, "onActivityResult requestCode: " + requestCode + ", resultCode: " + resultCode + ", data: " + data);

		if (requestCode == REQUEST_OAUTH && resultCode == Activity.RESULT_OK) {
			googleApiClient.connect(); // TODO: We need to call the previously success/failire callbacks here...
		}
	}

	@Override
	public void onStop() {
		if(googleApiClient!= null)
			Fitness.RecordingApi.unsubscribe(googleApiClient, DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.i("TAG", "Successfully unsubscribed for data type ");
                        } else {
                            // Subscription not removed
                            Log.i("TAG", "Failed to unsubscribe for data type ");
                        }
                    }
                });
    }

    public boolean execute(final String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
             
        // Shows a toast
        Log.v(TAG,"Google Fit Plugin received: "+ action);
         
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
               connect(callbackContext);
            }
        });
         
        return true;
    }


	private Activity getActivity() {
		return cordova.getActivity();
	}

	private Context getApplicationContext() {
		return cordova.getActivity().getApplicationContext();
	}
}

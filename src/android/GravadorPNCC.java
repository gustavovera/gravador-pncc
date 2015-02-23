package br.com.agm.gravador.pncc;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import android.media.AudioFormat;
import android.media.MediaRecorder.AudioSource;
import br.com.agm.gravador.capture.MicRecorder;

public class GravadorPNCC extends CordovaPlugin {

	private PNCCNative pnccNative;

	private MicRecorder micRecorder;

	private String filename;

	private boolean isRecording;

	private static CordovaWebView webView = null;

	@Override
	protected void pluginInitialize() {
		GravadorPNCC.webView = super.webView;
		pnccNative = new PNCCNative();
		micRecorder = new MicRecorder(AudioSource.VOICE_RECOGNITION, 16000, AudioFormat.CHANNEL_IN_MONO,
				AudioFormat.ENCODING_PCM_16BIT);
	}

	/**
	 * Executes the request and returns PluginResult.
	 *
	 * @param action
	 *            The action to execute.
	 * @param args
	 *            JSONArray of arguments for the plugin.
	 * @param callbackContext
	 *            The callback context used when calling back into JavaScript.
	 * @return True if the action was valid, false otherwise.
	 */
	public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
		if (action.equals("start")) {
			filename = args.getString(0);
			isRecording = true;
			micRecorder.startRecording(filename);
		} else if (action.equals("stop")) {
			if (isRecording) {
				micRecorder.stopRecording();
				cordova.getThreadPool().execute(new Runnable() {

					@Override
					public void run() {
						pnccNative.converWavToPNCC(micRecorder.getFinalWaveFilename(),
								micRecorder.getFinalPNCCFilename());
						callbackContext.success(micRecorder.getFinalPNCCFilename());
					}
				});

			}

		}
		return true;
	}
}

package br.com.agm.gravador.capture;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Environment;
import android.util.Log;

public class MicRecorder {

	private AudioRecord recorder;

	private boolean mIsRecording;

	private int mMinBufferSize;

	private String mFilename;

	private static final String PCM_EXT = ".pcm";

	private static final String WAVE_EXT = ".wav";

	private static final String PNCC_EXT = ".pncc";

	private int sampleRateHz;

	private int mBitsPerSample;

	private int mNumberChannels;

	private int audioSource;

	public MicRecorder(int audioSource, int sampleRateHz, int channelConfig, int audioFormat) {
		this.sampleRateHz = sampleRateHz;
		this.audioSource = audioSource;
		if (channelConfig == AudioFormat.CHANNEL_IN_MONO) {
			mNumberChannels = 1;
		} else {
			mNumberChannels = 2;
		}

		if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) {
			mBitsPerSample = 16;
		} else {
			mBitsPerSample = 8;
		}

		mMinBufferSize = AudioRecord.getMinBufferSize(this.sampleRateHz, channelConfig, audioFormat);
	}

	public void startRecording(String filename) {
		Log.i("STORAGE", Environment.getExternalStorageDirectory().getPath());
		if (isExternalStorageWritable()) {
			mFilename = Environment.getExternalStorageDirectory().getPath() + File.separator + filename;
		} else {
			mFilename = Environment.getExternalStorageDirectory().getPath() + File.separator + filename;
		}
		mIsRecording = true;
		recorder = new AudioRecord(audioSource, this.sampleRateHz, mNumberChannels == 1 ? AudioFormat.CHANNEL_IN_MONO
				: AudioFormat.CHANNEL_IN_STEREO, mBitsPerSample == 16 ? AudioFormat.ENCODING_PCM_16BIT
				: AudioFormat.ENCODING_PCM_8BIT, mMinBufferSize);
		recorder.startRecording();
		new Thread(new AudioRecordThread()).start();
	}

	public void stopRecording() {
		if (recorder != null) {
			mIsRecording = false;
			recorder.stop();
			recorder.release();
			recorder = null;
		}
	}

	class AudioRecordThread implements Runnable {

		@Override
		public void run() {
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
			writeBytesToPCMFile();
			addWaveHeaders();
		}

	}

	private void writeBytesToPCMFile() {
		byte[] audioData = new byte[mMinBufferSize];
		FileOutputStream fos = null;
		int readSize = 0;
		try {
			File file = new File(mFilename + PCM_EXT);
			if (file.exists()) {
				file.delete();
			}
			fos = new FileOutputStream(file);//
		} catch (Exception e) {
			e.printStackTrace();
		}
		while (mIsRecording == true) {
			readSize = recorder.read(audioData, 0, mMinBufferSize);
			if (AudioRecord.ERROR_INVALID_OPERATION != readSize) {
				try {
					fos.write(audioData);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		try {
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

	private void addWaveHeaders() {
		String pcmFilename = mFilename + PCM_EXT;
		String waveFilename = mFilename + WAVE_EXT;
		FileInputStream in = null;
		FileOutputStream out = null;
		long totalAudioLen = 0;
		long totalDataLen = totalAudioLen + 36;
		long longSampleRate = sampleRateHz;
		long byteRate = mBitsPerSample * sampleRateHz * mNumberChannels / 8;
		byte[] data = new byte[mMinBufferSize];
		try {
			in = new FileInputStream(pcmFilename);
			out = new FileOutputStream(waveFilename);
			totalAudioLen = in.getChannel().size();
			totalDataLen = totalAudioLen + 36;
			writeWaveHeaders(out, totalAudioLen, totalDataLen, longSampleRate, byteRate);
			while (in.read(data) != -1) {
				out.write(data);
			}
			in.close();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getFinalWaveFilename() {
		return mFilename + WAVE_EXT;
	}

	public String getFinalPNCCFilename() {
		return mFilename + PNCC_EXT;
	}

	private void writeWaveHeaders(FileOutputStream out, long totalAudioLen, long totalDataLen, long longSampleRate,
			long byteRate) throws IOException {
		byte[] header = new byte[44];
		header[0] = 'R'; // RIFF/WAVE header
		header[1] = 'I';
		header[2] = 'F';
		header[3] = 'F';
		header[4] = (byte) (totalDataLen & 0xff);
		header[5] = (byte) ((totalDataLen >> 8) & 0xff);
		header[6] = (byte) ((totalDataLen >> 16) & 0xff);
		header[7] = (byte) ((totalDataLen >> 24) & 0xff);
		header[8] = 'W';
		header[9] = 'A';
		header[10] = 'V';
		header[11] = 'E';
		header[12] = 'f'; // 'fmt ' chunk
		header[13] = 'm';
		header[14] = 't';
		header[15] = ' ';
		header[16] = 16; // 4 bytes: size of 'fmt ' chunk
		header[17] = 0;
		header[18] = 0;
		header[19] = 0;
		header[20] = 1; // format = 1
		header[21] = 0;
		header[22] = (byte) mNumberChannels;
		header[23] = 0;
		header[24] = (byte) (longSampleRate & 0xff);
		header[25] = (byte) ((longSampleRate >> 8) & 0xff);
		header[26] = (byte) ((longSampleRate >> 16) & 0xff);
		header[27] = (byte) ((longSampleRate >> 24) & 0xff);
		header[28] = (byte) (byteRate & 0xff);
		header[29] = (byte) ((byteRate >> 8) & 0xff);
		header[30] = (byte) ((byteRate >> 16) & 0xff);
		header[31] = (byte) ((byteRate >> 24) & 0xff);
		header[32] = (byte) (mNumberChannels * mBitsPerSample / 8); // block
																	// align
		header[33] = 0;
		header[34] = (byte) mBitsPerSample; // bits per sample
		header[35] = 0;
		header[36] = 'd';
		header[37] = 'a';
		header[38] = 't';
		header[39] = 'a';
		header[40] = (byte) (totalAudioLen & 0xff);
		header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
		header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
		header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
		out.write(header, 0, 44);
	}
}

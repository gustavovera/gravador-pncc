package br.com.agm.gravador.pncc;

public class PNCCNative {

	static {
		System.loadLibrary("gravador");
	}

	public native String converWavToPNCC(final String wavFile, final String pnccFile);
}

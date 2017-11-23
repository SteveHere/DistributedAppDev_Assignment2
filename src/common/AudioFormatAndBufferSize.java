package common;

import javax.sound.sampled.AudioFormat;

public class AudioFormatAndBufferSize {

	public static final int bufferSize = 60000;
	
	public static AudioFormat getAudioFormat() {
	    float sampleRate = 48000.0F;
	    int sampleInbits = 16;
	    int channels = 1;
	    boolean signed = true;
	    boolean bigEndian = false;
	    return new AudioFormat(sampleRate, sampleInbits, channels, signed, bigEndian);
	}

}

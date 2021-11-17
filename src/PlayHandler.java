import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;

public class PlayHandler implements ActionListener, Runnable {

	/** This ActionListener will play back all or part of a given
		array of data. If it's called as the result of an
		MouseEvent, it will ask its client AudioGraph what to
		play. Alternatively, the static method can be called to
		play an arbitrary array slice. **/

	// Number of bytes written at each communication with the AudioSystem
	private static final int bytesPerBlock = 16384;
	private static final int blocksPerBuffer = 4;

	private AudioView client = null;

	private static int playFrom, playFor;
	private static float[] data;
	private static SourceDataLine line;

	private Thread playbackThread;

	private static boolean stopNow = false;
	private static Object audioLock = new Object();


	public PlayHandler(AudioView av) {
		this.client = av;
	}

	public void actionPerformed(ActionEvent e) {
		if (playbackThread == null) { // start playing
			client.setPlayButtonText("Stop");
			if (client.getPlayAll())
				playSlice(client.getViewData());
			else 
				playSlice(client.getViewData(),
						  client.getViewStart(),
						  client.getViewLength());
			}
		else { // stop playing
			stop();
			client.setPlayButtonText("Play");
		}
	}

	public void playSlice(float[] d) { playSlice(d, 0, d.length); }

	public void playSlice(float[] d, int start, int len) {
		data     = d;
		playFrom = start;
		playFor  = len;
		stopNow  = false;
		playbackThread = new Thread((Runnable)this);
		playbackThread.start();
	}

	public void stop() {
		stopNow = true;
		playbackThread = null;
	}

	public void run() {
		synchronized(audioLock) { // Only allow one thread to play at a time
			AudioFormat fmt = new AudioFormat((float)AVPC.sampleRate,
											  16,     // 16b
											  1,      // Mono
											  true,   // Signed
											  true); // Big-endian
			byte[] audioData = new byte[2*playFor];
			for (int i = 0; i < playFor; i++) {
				int s = (int)(32767*data[i+playFrom]);
				//s = (int)(Short.MAX_VALUE*Math.cos(2000*Math.PI*i/44100.0));
				audioData[2*i] = (byte)(s / 256);
				audioData[2*i+1] = (byte)(s & 255);
			}

			DataLine.Info info =
				new DataLine.Info(SourceDataLine.class, fmt);
			System.out.println(info);
			// Open the audio line
			try {
				line = (SourceDataLine)AudioSystem.getLine(info);
				line.open(fmt, blocksPerBuffer*bytesPerBlock);
			} catch (Exception e) {
				System.out.println("Problem opening audio output line: " + e);
				return;
			}

			// write data to it
			line.addLineListener(new MyLineListener());
			line.start();
			int bytesToGo = audioData.length;
			int writeFrom = 0;
			try {
				while (bytesToGo > 0 && !stopNow) {
					int written =
						line.write(audioData, writeFrom,
								   bytesToGo > bytesPerBlock ? 
								               bytesPerBlock : bytesToGo);
					bytesToGo -= written;
					writeFrom += written;
					//System.out.println("Written "+written+" bytes. "+line.available()+" available"); System.out.flush();
					//Thread.yield();
				}
			} catch (Exception e) {
				System.out.println("Exception thrown in playback: " + e);
			}

			// Finish up
			if (stopNow) // abort as quickly as possible
				line.flush();
			else         // wait for the line to finish samples
				line.drain();
			line.stop();
			line.close();
			line = null;
			playbackThread = null;
			client.setPlayButtonText("Play");
		}
	}

	class MyLineListener implements LineListener {
		public void update (LineEvent le) {
			System.out.println("PlayHander: Received "+le);
		}
	}
}

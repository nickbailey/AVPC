import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;

/**
   Generate a waveform by reading it from an audio file.
   The audio file already exists at 44k1Hz sample rate,
   mono, wav format, in the current jar.
**/

class AudioFileReader extends Generator {

	private int clipLength;
	private WavFile wav;
	private int count, blockSize;
	private File tempFile;          // Jar'd audio unpacked here.

	public AudioFileReader(final String fileName) {
		try {
		
			// The wav file will have to be unpacked from the jar
			tempFile=File.createTempFile("AVPC", ".wav");
			tempFile.deleteOnExit();

			FileOutputStream out = new FileOutputStream(tempFile);
			InputStream in = this.getClass().getResourceAsStream(fileName);
			byte[] buffer = new byte[1024];
			int len = in.read(buffer);
			while (len != -1) {
				out.write(buffer, 0, len);
				len = in.read(buffer);
			}
			
			wav = WavFile.openWavFile(tempFile);
			blockSize = 512;
			targetLength = (int)wav.getNumFrames();
			count = 1 + targetLength/blockSize;
		} catch (Exception e) {
			System.err.println(e);
		}
	}
	
	public void generate(float[] target, ProgressWatcher pw, Thread pl) {
		int rbuf[] = new int[blockSize];
		for (int c = 0; c < count; c++) { // For each block
			try {
				// Read a frame into the target
				int framesRead = wav.readFrames(rbuf, blockSize);
				for (int i = 0; i < framesRead; i++)
					target[i + c*blockSize] = (float)(rbuf[i])/32767.0f;

				// Update superclass Generators data
				complete = (c+1) * blockSize;
				// But the last block mgiht not be full
				if (complete > targetLength)
					complete = targetLength;
				// Tell the progress listener that a block has been completed
				if (pw != null)
					pw.progress((double)complete/(100.0*count));
				// If there's a thread further down the pipeline, restart it.
				if (pl != null)
					pl.interrupt();
			} catch (Exception e) {
				System.err.println(e);
			}
		}
		// Inform superclass data structure we're done
		finished = true;
		if (pl != null) pl.interrupt();
	}
	
	public void finalize() {
		if (tempFile != null) {
			System.out.println("AudioFileReader: releasing tempFile");
			tempFile.delete();
			tempFile = null;
		}
	}
}

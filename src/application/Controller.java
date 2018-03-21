package application;
import application.Histogram;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import utilities.Utilities;

public class Controller {
	
	@FXML
	private ImageView imageView; // the image display window in the GUI
	
	private Mat image;
	
	private Histogram[] currentFrame;
	private Histogram[] prevFrame;
	
	private VideoCapture capture;
	private ScheduledExecutorService timer;
	private String fileName;
	
	@FXML
	private void initialize() throws UnsupportedAudioFileException, IOException, LineUnavailableException {	
	}
	
	private String getImageFilename() {
		// This method should return the filename of the image to be played
		// You should insert your code here to allow user to select the file
		FileChooser fc = new FileChooser();
		fc.setInitialDirectory(new java.io.File("."));
		fc.setTitle("Open Video");
		File file = fc.showOpenDialog(null);
		return file.getAbsolutePath();
	}
	
	@FXML
	protected void openImage(ActionEvent event) throws InterruptedException {
		try {
			fileName = getImageFilename();
		} catch (Exception e) {
			System.out.println(e);
			fileName = null;
		}
		if (fileName != null) {
			closeThreads();
			capture = new VideoCapture(fileName);
			if (capture.isOpened()) {
				createFrameGrabber();
			}
		}
	}
	
	private void createFrameGrabber() throws InterruptedException {
		if (capture != null && capture.isOpened()) {
			double framePerSecond = capture.get(Videoio.CAP_PROP_FPS);
			Runnable frameGrabber = new Runnable() {
				@Override
				public void run() {
					Mat frame = new Mat();
					if (capture.read(frame)) { // Analyze each frame and compare.
						Image im = Utilities.mat2Image(frame);
						Utilities.onFXThread(imageView.imageProperty(), im);
						double currentFrameNumber = capture.get(Videoio.CAP_PROP_POS_FRAMES);
						double totalFrameCount = capture.get(Videoio.CAP_PROP_FRAME_COUNT);
						try {
							image = frame;
						} catch (Exception e) {
							System.out.println(e);
						}
					} else {
						capture.set(Videoio.CAP_PROP_POS_FRAMES, 0);
					}
				}
			};
			if (timer != null && timer.isShutdown()) {
				timer.shutdown();
				timer.awaitTermination(Math.round(1000/framePerSecond), TimeUnit.MILLISECONDS);
			}
			timer = Executors.newSingleThreadScheduledExecutor();
			timer.scheduleAtFixedRate(frameGrabber, 0, Math.round(1000/framePerSecond), TimeUnit.MILLISECONDS);
		}
	}
	
	protected void closeThreads() {
		if (timer != null) {
			timer.shutdownNow();
		}
	}
}

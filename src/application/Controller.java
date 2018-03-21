package application;
import application.Histogram;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import utilities.Utilities;

public class Controller {
	
	@FXML
	private ImageView imageView; // the image display window in the GUI
	
	private Mat image;
	
	private int width;
	private int height;
	private int sampleRate; // sampling frequency
	private int sampleSizeInBits;
	private int numberOfChannels;
	
	private Histogram[] currentFrame;
	private Histogram[] prevFrame;
	
	private int numberOfSamplesPerColumn;
	private VideoCapture capture;
	private ScheduledExecutorService timer;
	private String fileName;
	private SourceDataLine currentAudio;
	private boolean pause = false;
	private Clip click;
	private float volume;
	private FloatControl clickVolume;
	private FloatControl mainVolume;
	
	@FXML private Slider slider;
	@FXML private Slider vSlider;
	@FXML private Button pButton;
	@FXML private TextField widthSetting;
	@FXML private TextField heightSetting;
	@FXML private TextField rateSetting;
	@FXML private TextField sizeSetting;
	@FXML private TextField samplePerColSetting;
	
	@FXML
	private void initialize() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
		pButton.setDisable(true);
		// Optional: You should modify the logic so that the user can change these values
		// You may also do some experiments with different values
		width = 64;
		height = 64;
		sampleRate = 8000;
		sampleSizeInBits = 8;
		numberOfChannels = 1;
		numberOfSamplesPerColumn = 500;
		
		widthSetting.setText(Integer.toString(width));
		heightSetting.setText(Integer.toString(height));
		rateSetting.setText(Integer.toString(sampleRate));
		sizeSetting.setText(Integer.toString(sampleSizeInBits));
		samplePerColSetting.setText(Integer.toString(numberOfSamplesPerColumn));
		
	}
	
	private int validateSetting(int original, TextField input) {
		try {
			int convertedInput = Integer.parseInt(input.getText());
			if (convertedInput <= 0) {
				input.setText(Integer.toString(original));
				convertedInput = original;
			}
			return convertedInput;
		} catch(NumberFormatException e) {
			input.setText(Integer.toString(original));
			return original;
		}
	}
	
	@FXML
	protected void updateSettings(ActionEvent event) {
		int original = height;
		width = validateSetting(width, widthSetting);
		height = validateSetting(height, heightSetting);
		sampleRate = validateSetting(sampleRate, rateSetting);
		sampleSizeInBits = validateSetting(sampleSizeInBits, sizeSetting);
		numberOfSamplesPerColumn = validateSetting(numberOfSamplesPerColumn, samplePerColSetting);
	}
	
	@FXML
	protected void positionSlider(MouseEvent event) {
		if (capture != null) {
			double sliderPosition = slider.getValue()/(slider.getMax()-slider.getMin());
			double totalFrameCount = capture.get(Videoio.CAP_PROP_FRAME_COUNT);
			capture.set(Videoio.CAP_PROP_POS_FRAMES, sliderPosition*totalFrameCount);
			switchAudio(true, true);
		}
	}
	
	@FXML
	protected void volumeSlider(MouseEvent event) {
		double sliderPosition = vSlider.getValue()/(vSlider.getMax()-vSlider.getMin());
		volume = (float) (sliderPosition*Math.abs(clickVolume.getMinimum()) - Math.abs(clickVolume.getMinimum()));
		clickVolume.setValue(volume);
		if (currentAudio != null) {
			mainVolume.setValue(volume);
		}
	}
	
	@FXML
	protected void selectPlayOrPause(ActionEvent event) {
		if (currentAudio != null) {
			if (pause) {
				switchAudio(false, false);
				pause = false;
				pButton.setText("Pause");
			} else {
				switchAudio(true, false);
				pause = true;
				pButton.setText("Play");
			}
		}
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
		// This method opens an image and display it using the GUI
		// You should modify the logic so that it opens and displays a video
		pButton.setDisable(true);
		if (currentAudio != null) {
			switchAudio(true, false);
			pause = true;
		}
		pButton.setText("Play");
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
				pButton.setDisable(false);
				pButton.setText("Pause");
				pause = false;
				createFrameGrabber();
			}
		} else if (currentAudio != null) {
			pButton.setDisable(false);
		}
		// You don't have to understand how mat2Image() works. 
		// In short, it converts the image from the Mat format to the Image format
		// The Mat format is used by the opencv library, and the Image format is used by JavaFX
		// BTW, you should be able to explain briefly what opencv and JavaFX are after finishing this assignment
	}

	private void playImage() throws LineUnavailableException {
		// This method "plays" the image opened by the user
		// You should modify the logic so that it plays a video rather than an image
		if (image != null) {
			// convert the image from RGB to grayscale
			Mat grayImage = new Mat();
			Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
			
			// resize the image
			Mat resizedImage = new Mat();
			Imgproc.resize(grayImage, resizedImage, new Size(width, height));
			
			
			// I used an AudioFormat object and a SourceDataLine object to perform audio output. Feel free to try other options
	        AudioFormat audioFormat = new AudioFormat(sampleRate, sampleSizeInBits, numberOfChannels, true, true);
            SourceDataLine sourceDataLine = AudioSystem.getSourceDataLine(audioFormat);
            currentAudio = sourceDataLine;
            sourceDataLine.open(audioFormat, sampleRate);
            mainVolume = (FloatControl) sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
            mainVolume.setValue(volume);
            sourceDataLine.start();
            
            for (int col = 0; col < width; col++) {
            	while (pause) {
					try {
						Thread.sleep(100);
					} catch (Exception e) {
						closeThreads();
						break;
					}
				}
            }
            sourceDataLine.drain();
            click.setMicrosecondPosition(0);
            click.start();
            sourceDataLine.close();
		} else {
			// What should you do here?
			System.out.println("Error: no image detected");
		}
	}
	
	private void createFrameGrabber() throws InterruptedException {
		if (capture != null && capture.isOpened()) {
			double framePerSecond = capture.get(Videoio.CAP_PROP_FPS);
			Runnable frameGrabber = new Runnable() {
				@Override
				public void run() {
					Mat frame = new Mat();
					while (pause) {
						try {
							Thread.sleep(100);
						} catch (Exception e) {
							closeThreads();
						}
					}
					if (capture.read(frame)) { // Analyze each frame and compare.
						Image im = Utilities.mat2Image(frame);
						Utilities.onFXThread(imageView.imageProperty(), im);
						double currentFrameNumber = capture.get(Videoio.CAP_PROP_POS_FRAMES);
						double totalFrameCount = capture.get(Videoio.CAP_PROP_FRAME_COUNT);
						slider.setValue(currentFrameNumber/totalFrameCount*(slider.getMax() - slider.getMin()));
						try {
							image = frame;
							playImage();
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
	
	private void switchAudio(boolean switchOff, boolean terminate) {
		// Controls the currently running audio, to stop/pause/play
		if (currentAudio != null) {
			if (switchOff) {
				currentAudio.stop();
				if (terminate) {
					currentAudio.close();
				}
			} else {
				currentAudio.start();
			}
		}
	}
	
	protected void closeThreads() {
		switchAudio(true, true);
		if (timer != null) {
			timer.shutdownNow();
		}
	}
}

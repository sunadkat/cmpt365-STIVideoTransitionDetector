package application;
import application.Histogram;

import java.io.File;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import utilities.Utilities;

public class Controller {
	
	@FXML private ImageView imageView; // the image display window in the GUI
	@FXML private ToggleGroup sti;
	@FXML private ToggleGroup computation;
	@FXML private CheckBox isScaled;
	@FXML private Text videoName;
	@FXML private TextField thresholdInput;
	@FXML private Button runButton;
	@FXML private Button saveButton;
	
	private Mat image;
	
	private Histogram[] currentFrame;
	private Histogram[] prevFrame;
	private float[] columnVector;
	
	private VideoCapture capture;
	private String fileName;
	private int computationOption;
	private int stiOption;
	private int stiHeight;
	private int scaledWidth = 32;
	private int scaledHeight = 32;
	private double threshold = 0.0;
	
	@FXML
	private void initialize() {
		thresholdInput.setText(Double.toString(threshold));
		thresholdInput.setDisable(true);
		runButton.setDisable(true);
		saveButton.setDisable(true);
	}
	
	@FXML
	private void selectVideo(ActionEvent event) {
		try {
			FileChooser fc = new FileChooser();
			fc.setInitialDirectory(new java.io.File("."));
			fc.setTitle("Open Video");
			File file = fc.showOpenDialog(null);
			fileName = file.getAbsolutePath();
			String[] filePath = fileName.split("\\\\");
			videoName.setText("Loaded Video: " + filePath[filePath.length -1]);
			runButton.setDisable(false);
		} catch (Exception e) {
			System.out.println(e);
			fileName = null;
		}
	}
	
	@FXML
	private void selectCopyPixel(ActionEvent event) {
		isScaled.setDisable(false);
		thresholdInput.setDisable(true);
		stiOption = 0;
	}
	
	@FXML
	private void selectHistogram(ActionEvent event) {
		isScaled.setSelected(true);
		isScaled.setDisable(true);
		thresholdInput.setDisable(false);
		stiOption = 1;
	}
	
	@FXML
	private void runSTIComputation(ActionEvent event) {
		RadioButton computationRadio = (RadioButton) computation.getSelectedToggle();
		if (computationRadio.getText().equals("Row")) {
			computationOption = 0;
		} else {
			computationOption = 1;
		}
		if (fileName != null) {
			capture = new VideoCapture(fileName);
			if (capture.isOpened()) {
				scanFrames();
				saveButton.setDisable(false);
			}
		}
	}
	
	@FXML
	private void saveImage(ActionEvent event) {
		if (image != null) {
			try {
				FileChooser fc = new FileChooser();
				fc.setInitialDirectory(new java.io.File("."));
				fc.setTitle("Save Image");
				FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("JPEG files (*.jpg)", "*.jpg");
				fc.getExtensionFilters().add(extFilter);
				File file = fc.showSaveDialog(null);
				if (file != null) {
					Imgcodecs.imwrite(file.getAbsolutePath(), image);
				}
			} catch (Exception e) {
				System.out.println(e);
			}
		}
	}
	
	private void scanFrames() {
		if (capture != null && capture.isOpened()) {
			Mat frame = new Mat();
			Mat scaledFrame = new Mat();
			capture.read(frame);
			setOptions(frame);
			
			int totalFrames = (int) capture.get(Videoio.CAP_PROP_FRAME_COUNT);
			if (stiOption == 1) totalFrames -= 1;
			capture.set(Videoio.CAP_PROP_POS_FRAMES, 0);
			
			image = new Mat(stiHeight, totalFrames, frame.type());
			
			while(capture.read(frame)) { // Go through each frame
				if (frame != null) {
					if (isScaled.isSelected()) {
						Imgproc.resize(frame, scaledFrame, new Size(scaledWidth, scaledHeight));
						if (stiOption == 0) {
							computeCopyPixel(scaledFrame);
						} else {// Histogram Selected
							generateHistColumns(scaledFrame);
							compareHistFrames();
							drawHistogramSTI();
						}
					} else {
						if (stiOption == 0) {
							computeCopyPixel(frame);
						}
					}
				}
			}
			// Output STI to GUI
			Image im = Utilities.mat2Image(image);
            Utilities.onFXThread(imageView.imageProperty(), im);
		}
	}
	
	private void setOptions(Mat frame) {
		if (stiOption == 0) {
			if (computationOption == 0) {
				if (isScaled.isSelected()) {
					stiHeight = scaledWidth;
				} else {
					stiHeight = frame.width();
				}
			} else {
				if (isScaled.isSelected()) {
					stiHeight = scaledHeight;
				} else {
					stiHeight = frame.height();
				}
			}
		} else {
			validateThreshold();
			if (computationOption == 0) {
				stiHeight = scaledHeight;
			} else {
				stiHeight = scaledWidth;
			}
			initHistogram();
		}
	}
	
	private void validateThreshold() {
		double original = threshold;
		try {
			double convertedInput = Double.parseDouble(thresholdInput.getText());
			if (convertedInput < 0.0 || convertedInput > 1.0) {
				thresholdInput.setText(Double.toString(original));
			} else {
				threshold = convertedInput;
			}
		} catch(NumberFormatException e) {
			thresholdInput.setText(Double.toString(original));
		}
	}
	
	private void computeCopyPixel(Mat frame) { // Moves the middle col/row of current frame to STI[current frame -1]
		if (computationOption == 0) {
			(frame.row(stiHeight/2)).t().copyTo(image.col((int) capture.get(Videoio.CAP_PROP_POS_FRAMES) -1));
		} else {
			(frame.col(stiHeight/2)).copyTo(image.col((int) capture.get(Videoio.CAP_PROP_POS_FRAMES) -1));
		}
	}
	
	private void initHistogram() {
		currentFrame = new Histogram[stiHeight];
		prevFrame = new Histogram[stiHeight];
		int bins = (int) Math.floor(1 + (Math.log(scaledHeight)/Math.log(2)));
		for (int i = 0; i < stiHeight; i++) {
			currentFrame[i] = new Histogram(bins, stiHeight);
			prevFrame[i] = new Histogram(bins, stiHeight);
		}
	}
	
	private void generateHistColumns(Mat frame) {
		int frameWidth;
		if (computationOption == 0) {
			frameWidth = frame.width();
		} else {
			frameWidth = frame.height();
		}
		for (int i = 0; i < stiHeight; i++) {
			if ((int) capture.get(Videoio.CAP_PROP_POS_FRAMES) -1 != 0) {
				prevFrame[i].copyColumn(currentFrame[i]);
			}
			currentFrame[i].computeHistogram(frame, frameWidth, i, computationOption);
			currentFrame[i].normalize();
		}
	}
	
	
	private void compareHistFrames() {
		columnVector = new float[stiHeight];
		for (int i = 0; i < stiHeight; i++) {
			columnVector[i] = currentFrame[i].compareColumn(prevFrame[i], threshold);
		}
	}
	
	private void drawHistogramSTI() {
		double[] intersectionPixel = new double[3];
		int currentFrame = (int) capture.get(Videoio.CAP_PROP_POS_FRAMES) -1;
		for (int i = 0; i < stiHeight; i++) {
			for (int j = 0; j < 3; j++) {
				if (currentFrame == 0) {
					intersectionPixel[j] = 255;
				} else {
					intersectionPixel[j] = columnVector[i];
				}
			}
			image.put(i, currentFrame, intersectionPixel);
		}
	}
}

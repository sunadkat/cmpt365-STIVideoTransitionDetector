package application;
import application.Histogram;

import java.io.File;
import java.io.IOException;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import utilities.Utilities;

public class Controller {
	
	@FXML private ImageView imageView; // the image display window in the GUI
	@FXML private ToggleGroup sti;
	@FXML private ToggleGroup computation;
	
	private Mat image;
	
	private Histogram[] currentFrame;
	private Histogram[] prevFrame;
	
	private VideoCapture capture;
	private String fileName;
	
	@FXML
	private void initialize() throws IOException {	
	}
	
	@FXML
	private void selectVideo(ActionEvent event) {
		try {
			FileChooser fc = new FileChooser();
			fc.setInitialDirectory(new java.io.File("."));
			fc.setTitle("Open Video");
			File file = fc.showOpenDialog(null);
			fileName = file.getAbsolutePath();
		} catch (Exception e) {
			System.out.println(e);
			fileName = null;
		}
	}
	
	@FXML
	private void runSTIComputation(ActionEvent event) {
		RadioButton stiRadio = (RadioButton) sti.getSelectedToggle();
		String stiOption = stiRadio.getText();
		RadioButton computationRadio = (RadioButton) computation.getSelectedToggle();
		String computationOption = computationRadio.getText();
		
		System.out.println(stiOption);
		System.out.println(computationOption);
		
		if (fileName != null) {
			capture = new VideoCapture(fileName);
			if (capture.isOpened()) {
				scanFrames(stiOption, computationOption);
			}
		}
	}
	
	private void scanFrames(String stiOption, String computationOption) {
		if (capture != null && capture.isOpened()) {
			Mat frame = new Mat();
			capture.read(frame);
			int width = frame.width();
			int height = frame.height();
			capture.set(Videoio.CAP_PROP_POS_FRAMES, 0);
			double totalFrames = capture.get(Videoio.CAP_PROP_FRAME_COUNT);
			image = new Mat(height, (int) totalFrames, frame.type());
			System.out.println(image.width() + " " + image.height());
			while(capture.read(frame)) {
				computeCopyPixel(frame, width, height);
			}
			// Output STI to GUI
			Image im = Utilities.mat2Image(image);
            Utilities.onFXThread(imageView.imageProperty(), im);
			
		}
	}
	
	private void computeCopyPixel(Mat frame, int width, int height) {
		if (frame != null) {
			(frame.col(width/2)).copyTo(image.col((int) capture.get(Videoio.CAP_PROP_POS_FRAMES) -1));
		}
	}
}

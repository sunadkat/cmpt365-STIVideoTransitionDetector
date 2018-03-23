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
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
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
	
	private Mat image;
	
	private Histogram[] currentFrame;
	private Histogram[] prevFrame;
	
	private VideoCapture capture;
	private String fileName;
	private int computationOption;
	private int stiOption;
	private int scaledWidth = 32;
	private int scaledHeight = 32;
	
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
			String[] filePath = fileName.split("\\\\");
			videoName.setText("Loaded Video: " + filePath[filePath.length -1]);
		} catch (Exception e) {
			System.out.println(e);
			fileName = null;
		}
	}
	
	@FXML
	private void runSTIComputation(ActionEvent event) {
		RadioButton stiRadio = (RadioButton) sti.getSelectedToggle();
		RadioButton computationRadio = (RadioButton) computation.getSelectedToggle();
		
		if (computationRadio.getText().equals("Row")) {
			computationOption = 0;
		} else {
			computationOption = 1;
		}
		
		if (stiRadio.getText().equals("Copying Pixels")) {
			stiOption = 0;
		} else {
			stiOption = 1;
		}
		
		if (fileName != null) {
			capture = new VideoCapture(fileName);
			if (capture.isOpened()) {
				scanFrames();
			}
		}
	}
	
	private void scanFrames() {
		if (capture != null && capture.isOpened()) {
			Mat frame = new Mat();
			Mat scaledFrame = new Mat();
			capture.read(frame);
			int stiHeight;
			
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
			
			if (stiOption == 1) { // Create Histograms
				
			}
			
			double totalFrames = capture.get(Videoio.CAP_PROP_FRAME_COUNT);
			capture.set(Videoio.CAP_PROP_POS_FRAMES, 0);
			
			image = new Mat(stiHeight, (int) totalFrames, frame.type());
			
			while(capture.read(frame)) { // Go through each frame
				if (isScaled.isSelected()) {
					Imgproc.resize(frame, scaledFrame, new Size(scaledWidth, scaledHeight));
					if (stiOption == 0) {
						computeCopyPixel(scaledFrame, stiHeight);
					} else {
						// histogram here
					}
				} else {
					if (stiOption == 0) {
						computeCopyPixel(frame, stiHeight);
					} else {
						// histogram here
					}
				}
			}
			// Output STI to GUI
			Image im = Utilities.mat2Image(image);
            Utilities.onFXThread(imageView.imageProperty(), im);
			
		}
	}
	
	private void computeCopyPixel(Mat frame, int stiHeight) { // Moves the middle col/row of current frame to STI[current frame -1]
		if (frame != null) {
			if (computationOption == 0) {
				(frame.row(stiHeight/2)).t().copyTo(image.col((int) capture.get(Videoio.CAP_PROP_POS_FRAMES) -1));
			} else {
				(frame.col(stiHeight/2)).copyTo(image.col((int) capture.get(Videoio.CAP_PROP_POS_FRAMES) -1));
			}
		}
	}
	
	private void initHistogram() {
		
	}
}

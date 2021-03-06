package application;

import java.awt.Color;
import java.awt.image.BufferedImage;
import org.opencv.core.Mat;
import utilities.Utilities;

class Histogram {
	private float[][] entry;
	private int size;
	private int sum;
	
	protected Histogram(int x, int y) {
		size = x;
		sum = y;
		entry = new float[size][size];
	}
	
	protected float[][] getHist() {
		return entry;
	}
	
	protected float getHistEntry(int x, int y) {
		return entry[x][y];
	}
	
	protected int getSize() {
		return size;
	}
	
	protected void add(int x, int y) { // increase the entry by 1
		entry[x][y] += 1.0;
	}
	
	protected void printHistogram() {
		for(int i = 0; i<size; ++i) {
			for(int j = 0; j < size; ++j)
			{
				System.out.print(entry[i][j]);
				System.out.print(",");
			}
			System.out.println(" ");
		}
		System.out.println("/Histogram End");
	}
	
	protected void computeHistogram(Mat original, int frameWidth, int column, int operation) {
		BufferedImage image = Utilities.matToBufferedImage(original);
		this.clearHistogram();
		for(int i = 0; i < frameWidth; ++i) {
			Color rgb;
			if (operation == 0) { // Computation for row
				rgb = new Color(image.getRGB(column,i));
			} else { //compute column
				rgb = new Color(image.getRGB(i,column));
			}
			float[] rg = getRg(rgb);
			int[] position = getPosition(rg, size);
			this.add(position[0], position[1]);
		}
	}
	
	protected void copyColumn(Histogram original) {
		for (int i = 0; i < size; i++) {
			System.arraycopy(original.getHist()[i], 0, entry[i], 0, size);
		}
	}
	
	protected void normalize() {
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				entry[i][j] /= sum; 
			}
		}
	}
	
	protected float compareColumn(Histogram x, double threshold) {
		float summation = 0;
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				summation += Math.min(entry[i][j], x.getHistEntry(i, j));
			}
		}
		if (summation < threshold) return 255;
		return summation*255;
	}
	
	private void clearHistogram() {
		float[] initEntry = new float[size];
		for (int i = 0; i < size; i++) {
			System.arraycopy(initEntry, 0, entry[i], 0, size);
		}
	}
	
	private int[] getPosition(float[] rg, int size) {// Calculates where pixel should be in our histogram
		int[] position = new int[2];
		position[0] = (int) Math.ceil(rg[0]*size) - 1;
		position [1] = (int) Math.ceil(rg[1]*size) - 1;
		
		if (position[0] ==-1) position[0] = 0;
		if (position[1] == -1) position[1] = 0;
		
		return position;
	}
	
	private float[] getRg(Color rgb) { // Converts Chromaticity
		float[] rg = new float[2];
		float r;
		float g;
		
		int red = rgb.getRed();
		int green = rgb.getGreen();
		int blue = rgb.getBlue();
		
		int sum = red + blue + green;
		if(sum == 0) { // Case 1 : Black (0,0,0)
			r = 0;
			g = 0;
		} else { // Case 2 : Non-Black Pixel
			r = ((float)red)/ sum;
			g = ((float)green)/ sum;
		}
		rg[0] = r;
		rg[1] = g;
		return rg;
	}
}

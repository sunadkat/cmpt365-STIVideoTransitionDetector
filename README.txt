STI Video Transition Detector

How to use:

The open button gives the user the option to select a video to generate an STI image.
The run button will generate an STI using the options the user currently has set.
The save button will allow the user to save the current image that was generated (and in display).

The STI Methods:

Copy Pixel - copy the middle row or column of a frame to the resulting STI in the order of the frames.
Histogram Difference - Generate a histogram for each row or column of each frame and compare frame by frame. Output the histogram's intersection on the resulting STI in the order of the frames.

Downscale - converts video to a 32x32 video before applying methods.
Threshold - input a value between 0 and 1 of what the Histogram Difference's intersection should be to be displayed.

Controller.java

selectVideo(ActionEvent event)
	- Selects a video to open and saves the filepath
	- Enables the run button when a file is successfully selected for the first time

selectCopyPixel(ActionEvent event)
	- Runs when the user selects the Copy Pixel option
	- Disables the threshold option since it is not used for Copy Pixel
	- Enables the ability to scale the video or not

selectHistogram(ActionEvent event)
	- Runs when the user selects the Histogram Difference option
	- Enables the threshold option
	- Enforces that the video is always scaled

runSTIComputation(ActionEvent event)
	- Saves values of certain user choices (comparing or using rows or columns)
	- Calls scanFrames() to scan each frame
	- Enables the save button when it completes an STI image for the first time

saveImage(ActionEvent event)
	- Allows the user to save an image of the STI when outputted

scanFrames()
	- Scans each frame and applies a different function depending on the options the user selected
	- Outputs the STI image to display when ready
	- Calls computeCopyPixel(Mat frame) for copy pixel option
	- Calls generateHistColumns(Mat frame), compareHistFrames(), and drawHistogramSTI() for histogram difference option

setOptions(Mat frame)
	- Sets the user's options to the global variables

validateThreshold()
	- Checks if the given threshold is between 0-1 and a number

computeCopyPixel(Mat frame)
	- Copies the middle row or column of the frame to the output STI image

initHistogram()
	- Initialize the histograms, currentFrame = frame we are currently analyzing, prevFrame = frame that was looked at prior

generateHistColumns(Mat frame)
	- Copy currentFrame to prevFrame
	- Generate a histogram for each of the current frame's rows or columns to currentFrame
	- Normalize the histogram

compareHistFrames()
	- Get the intersection for the currentFrame and prevFrame's histograms

drawHistogramSTI()
	- Draw the image for the STI based on values received after the intersection

Histogram.java

A class that was made to create histogram (2D array) objects which consist of a 2D array called entry, a int called size which is the length n of entry (entry is a nxn array), and the sum which is the total number of values in the histogram summed up

getHist()
	- Returns the histogram (variable entry is returned)

getHistEntry(int x, int y)
	- Returns a particular entry in the histogram (entry[x][y])

getSize()
	- Returns size

add(int x, int y)
	- increments the entry[x][y] by 1

printHistogram()
	- prints entry
	- used for testing and debugging

computeHistogram(Mat original, int frameWidth, int column, int operation)
	- Clears the current entry (clearHistogram())
	- For each pixel (iterate through int frameWidth) of the specified row or column (given by int column for the row or column position and int operation determines whether to use rows or columns) we process the color using the chromaticity (compareColumn(Histogram x, double threshold)) and increment the corresponding bin in the histogram (add(int x, int y))

copyColumn(Histogram original)
	- Copy the histogram (original) to current (this) histogram
	- Used to copy old contents of currentFrame to prevFrame

normalize()
	- Normalizes every entry of the histogram
	- entry[x][y]/sum

compareColumn(Histogram x, double threshold)
	- compares Histogram x with current (this) histogram and assumes the values are normalized already
	- returns the intersect*255

clearHistogram()
	- empties the histogram

getPosition(float[] rg, int size)
	- calculates which bins to increment in the histogram

getRg(Color  rgb)
	- Calulates the chromaticity of a given pixel (Color rgb)
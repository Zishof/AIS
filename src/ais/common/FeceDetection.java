package ais.common;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

public class FeceDetection {

	public static void main(String[] args) {

		// Core.NATIVE_LIBRARY_NAME must be loaded before
		// calling any of the opencv methods
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		// Face detector creation by loading source cascade
		// xml file using CascadeClassifier and must be
		// placed in same directory of the source java file

		// File is available here on git as mentioned above
		// prior to code
		CascadeClassifier faceDetector = new CascadeClassifier();
		faceDetector.load("haarcascade_frontalface_alt.xml");

		// TODO Auto-generated method stub
		// Reading the input image
		Mat image = Imgcodecs.imread("E:\\input.jpg");

		// Detecting faces
		MatOfRect faceDetections = new MatOfRect();
		faceDetector.detectMultiScale(image, faceDetections);

		// Creating a rectangular box which represents for
		// faces detected
		for (Rect rect : faceDetections.toArray()) {
			Imgproc.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height),
					new Scalar(0, 255, 0));
		}

		// Saving the output image
		String filename = "Ouput.jpg";

		Imgcodecs.imwrite("E:\\" + filename, image);

		// Display message for successful execution of
		// program
		System.out.print("Face Detected");
	}

}

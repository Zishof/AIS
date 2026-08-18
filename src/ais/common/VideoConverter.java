package ais.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.zkoss.zul.Label;

/**
 * Created by Fauzi on 1/10/2017.
 */
public class VideoConverter {

	private String ffmpegApp;

	public VideoConverter(String ffmpegApp) {
		this.ffmpegApp = ffmpegApp;
	}

	public int convert(String filenameIn, String filenameOut, Label label) throws IOException, InterruptedException {
		ProcessBuilder processBuilder;
		System.out.println("Temporary file -> " + filenameOut);
		processBuilder = new ProcessBuilder(ffmpegApp, "-i", filenameIn, "-vcodec", "h264", "-acodec", "aac",
				"-nostdin", "-strict", "-2", filenameOut);
		Process process = processBuilder.start();
		InputStream stderr = process.getErrorStream();
		InputStreamReader isr = new InputStreamReader(stderr);
		BufferedReader br = new BufferedReader(isr);
		String line;
		while ((line = br.readLine()) != null) {
			label.setValue(line);
			System.out.println(line);
		}

		return process.waitFor();

	}
}
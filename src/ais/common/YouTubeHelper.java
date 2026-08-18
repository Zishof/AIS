package ais.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTubeHelper {

	public static String pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*";

	public static String extractVideoIdFromUrl(String url) {

		Pattern compiledPattern = Pattern.compile(pattern);
		Matcher matcher = compiledPattern.matcher(url); // url is youtube url
														// for which you want to
														// extract the id.

		String id = "";
		if (matcher.find()) {
			id = matcher.group();
		}
		return id;
	}

	// https://youtu.be/xzQb7G_-UYE

	public static void main(String[] argv) {
		String url = "https://youtu.be/xzQb7G_-UYE";

		Pattern compiledPattern = Pattern.compile(pattern);
		Matcher matcher = compiledPattern.matcher(url); // url is youtube url
														// for which you want to
														// extract the id.

		String id = "";
		if (matcher.find()) {
			id = matcher.group();
		}

		System.out.println("ID = " + id);
	}

	public static String convertoToEmbed(String url) {

		String id = extractVideoIdFromUrl(url);

		String v = "<iframe style=\"width:100%;min-height: 315px;\" "
				+ " src=\"https://www.youtube.com/embed/" + id
				+ "\" frameborder=\"0\" allow=\"accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture\" allowfullscreen></iframe>";

		return v;
	}
}
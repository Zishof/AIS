package ais.ui.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.util.PDFTextStripper;

public class MyPDFTextStripper extends PDFTextStripper {

	public MyPDFTextStripper() throws IOException {
		super();
	}

	public String getText(PDDocument doc, int page) throws IOException {
		StringWriter outputStream = new StringWriter();
		writeText(doc, outputStream, page);
		return outputStream.toString();
	}

	public void writeText(PDDocument doc, Writer outputStream, int page)
			throws IOException {
		// super.writeText(arg0, arg1);
		List<COSObjectable> list = new ArrayList<COSObjectable>();

		list.add((COSObjectable) document.getDocumentCatalog().getAllPages().get(page));

		processPages(list);
		endDocument(document);
	}

}

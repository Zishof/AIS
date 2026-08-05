/*
 * XLSContentStyleFactory.java
 *
 * Created on September 22, 2007, 11:11 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package ais.common.xls;

import ais.database.model.ReportContentVO;
import jxl.format.Alignment;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.format.Colour;
import jxl.format.Orientation;
import jxl.format.Pattern;
import jxl.format.VerticalAlignment;
import jxl.write.NumberFormats;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WriteException;

/**
 * 
 * @author M. Fauzi Murtadlo
 */
public class XLSContentStyleFactory {

	private static XLSContentStyleFactory xLSContentStyleFactory = new XLSContentStyleFactory();

	public static XLSContentStyleFactory getInstance() {
		return xLSContentStyleFactory;
	}

	/** Creates a new instance of XLSContentStyleFactory */
	private XLSContentStyleFactory() {
	}

	public synchronized WritableCellFormat getDataFormat(ReportContentVO example)
			throws WriteException {
		if (example.getData() instanceof String) {
			WritableCellFormat format1;
			if (example.getType() == 3) {
				format1 = new WritableCellFormat(new WritableFont(
						WritableFont.ARIAL, 16));
			} else if (example.getType() == 4) {
				format1 = new WritableCellFormat(new WritableFont(
						WritableFont.ARIAL, 12));
			} else if (example.getType() == 2) {
				format1 = new WritableCellFormat(new WritableFont(
						WritableFont.ARIAL, 10, WritableFont.BOLD));
			} else {
				format1 = new WritableCellFormat(new WritableFont(
						WritableFont.ARIAL, 10));
			}

			if (example.getType() == 1) {
				format1.setBackground(example.getColour(), Pattern.SOLID);
				format1.setBorder(Border.ALL, BorderLineStyle.THIN,
						Colour.BLACK);
				format1.setAlignment(example.getAlignment());
				format1.setWrap(false);
			} else if (example.getType() == 2) {
				format1.setBackground(Colour.WHITE, Pattern.SOLID);
				format1.setBorder(Border.ALL, BorderLineStyle.THIN,
						Colour.BLACK);
				format1.setAlignment(Alignment.CENTRE);
				format1.setWrap(false);
			} else {
				format1.setBorder(Border.NONE, BorderLineStyle.NONE,
						Colour.BLACK);
				format1.setBackground(example.getColour(), Pattern.SOLID);
				format1.setAlignment(example.getAlignment());
				format1.setWrap(false);
			}
			format1.setLocked(true);
			format1.setOrientation(Orientation.HORIZONTAL);
			format1.setShrinkToFit(true);
			format1.setIndentation(0);
			format1.setVerticalAlignment(VerticalAlignment.CENTRE);

			return format1;
		}
		if (example.getData() instanceof Integer
				|| example.getData() instanceof Long
				|| example.getData() instanceof Double) {
			WritableCellFormat format2;
			if (example.getType() == 3) {
				if (example.getFormat() == 1) {
					format2 = new WritableCellFormat(new WritableFont(
							WritableFont.ARIAL, 16), NumberFormats.FORMAT1);
				} else if (example.getFormat() == 2) {
					format2 = new WritableCellFormat(new WritableFont(
							WritableFont.ARIAL, 16),
							NumberFormats.PERCENT_INTEGER);
				} else {
					format2 = new WritableCellFormat(new WritableFont(
							WritableFont.ARIAL, 16), NumberFormats.INTEGER);
				}
			} else if (example.getType() == 4) {
				if (example.getFormat() == 1) {
					format2 = new WritableCellFormat(new WritableFont(
							WritableFont.ARIAL, 12), NumberFormats.FORMAT1);
				} else if (example.getFormat() == 2) {
					format2 = new WritableCellFormat(new WritableFont(
							WritableFont.ARIAL, 12),
							NumberFormats.PERCENT_INTEGER);
				} else {
					format2 = new WritableCellFormat(new WritableFont(
							WritableFont.ARIAL, 12), NumberFormats.INTEGER);
				}
			} else if (example.getType() == 2) {
				if (example.getFormat() == 1) {
					format2 = new WritableCellFormat(new WritableFont(
							WritableFont.ARIAL, 10, WritableFont.BOLD),
							NumberFormats.FORMAT1);
				} else if (example.getFormat() == 2) {
					format2 = new WritableCellFormat(new WritableFont(
							WritableFont.ARIAL, 10, WritableFont.BOLD),
							NumberFormats.PERCENT_FLOAT);
				} else {
					format2 = new WritableCellFormat(new WritableFont(
							WritableFont.ARIAL, 10, WritableFont.BOLD),
							NumberFormats.INTEGER);
				}
			} else {
				if (example.getFormat() == 1) {
					format2 = new WritableCellFormat(new WritableFont(
							WritableFont.ARIAL, 10), NumberFormats.FORMAT1);
				} else if (example.getFormat() == 2) {
					format2 = new WritableCellFormat(new WritableFont(
							WritableFont.ARIAL, 10),
							NumberFormats.PERCENT_FLOAT);
				} else {
					format2 = new WritableCellFormat(new WritableFont(
							WritableFont.ARIAL, 10), NumberFormats.INTEGER);
				}
			}

			if (example.getType() == 1) {
				format2.setBackground(example.getColour(), Pattern.SOLID);
				format2.setBorder(Border.ALL, BorderLineStyle.THIN,
						Colour.BLACK);
				format2.setAlignment(example.getAlignment());
				format2.setWrap(false);
			} else if (example.getType() == 2) {
				format2.setBackground(Colour.WHITE, Pattern.SOLID);
				format2.setBorder(Border.ALL, BorderLineStyle.THIN,
						Colour.BLACK);
				format2.setAlignment(Alignment.CENTRE);
				format2.setWrap(false);
			} else {
				format2.setBorder(Border.NONE, BorderLineStyle.NONE,
						Colour.BLACK);
				format2.setBackground(example.getColour(), Pattern.SOLID);
				format2.setAlignment(example.getAlignment());
				format2.setWrap(false);
			}
			format2.setLocked(true);
			format2.setOrientation(Orientation.HORIZONTAL);
			format2.setShrinkToFit(true);
			format2.setIndentation(0);
			format2.setVerticalAlignment(VerticalAlignment.CENTRE);
			return format2;
		}

		return new WritableCellFormat(new WritableFont(WritableFont.ARIAL));
	}

}

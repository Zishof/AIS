/*
 * XLSContentVO.java
 *
 * Created on September 22, 2007, 10:30 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package ais.database.model;

import java.io.Serializable;
import jxl.format.Colour;
import jxl.format.Alignment;

/**
 *
 * @author M. Fauzi Murtadlo
 */
@SuppressWarnings("serial")
public class ReportContentVO implements Serializable {

	/** Creates a new instance of XLSContentVO */
	public ReportContentVO() {
	}

	public ReportContentVO(Object data) {
		this.data = data;
	}

	public ReportContentVO(Object data, int type) {
		this.data = data;
		this.type = type;
	}

	public ReportContentVO(Object data, int type, int format) {
		this.data = data;
		this.type = type;
		this.format = format;
	}

	public ReportContentVO(Object data, int type, int format, Colour colour) {
		this.data = data;
		this.type = type;
		this.format = format;
		this.colour = colour;
	}

	public ReportContentVO(Object data, int type, int format, Colour colour, Alignment alignment) {
		this.data = data;
		this.type = type;
		this.format = format;
		this.colour = colour;
		this.alignment = alignment;
	}

	private Object data;
	private int type = 0; // 0 = biasa, 1 = data, 2 = header, 3 = main header, 4 = lower main header
	private int format = 0; // 0 = biasa, 1 = 0,00, 2 = %
	private Colour colour = Colour.WHITE;
	private Alignment alignment = Alignment.GENERAL;

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getFormat() {
		return format;
	}

	public void setFormat(int format) {
		this.format = format;
	}

	public Colour getColour() {
		return colour;
	}

	public void setColour(Colour colour) {
		this.colour = colour;
	}

	public Alignment getAlignment() {
		return alignment;
	}

	public void setAlignment(Alignment alignment) {
		this.alignment = alignment;
	}

}

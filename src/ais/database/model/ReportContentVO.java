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
 * Value object (bukan entitas Hibernate) satu sel/konten laporan Excel (JXL):
 * membungkus nilai mentah {@link #getData()} beserta metadata tampilan —
 * peran sel ({@link #getType()}: biasa/data/header/main-header/lower-main-header),
 * format angka ({@link #getFormat()}: biasa/desimal/persen), warna latar
 * ({@link #getColour()}), dan perataan ({@link #getAlignment()}). Dipakai oleh
 * generator laporan Excel untuk merender satu sel dengan gaya yang konsisten.
 *
 * @author M. Fauzi Murtadlo
 */
@SuppressWarnings("serial")
public class ReportContentVO implements Serializable {

	/** Creates a new instance of XLSContentVO */
	public ReportContentVO() {
	}

	/**
	 * @param data nilai mentah sel (biasanya String/Number/Date).
	 */
	public ReportContentVO(Object data) {
		this.data = data;
	}

	/**
	 * @param data nilai mentah sel.
	 * @param type peran sel: 0=biasa, 1=data, 2=header, 3=main header, 4=lower main header.
	 */
	public ReportContentVO(Object data, int type) {
		this.data = data;
		this.type = type;
	}

	/**
	 * @param data   nilai mentah sel.
	 * @param type   peran sel: 0=biasa, 1=data, 2=header, 3=main header, 4=lower main header.
	 * @param format format angka: 0=biasa, 1="0,00", 2=persen.
	 */
	public ReportContentVO(Object data, int type, int format) {
		this.data = data;
		this.type = type;
		this.format = format;
	}

	/**
	 * @param data   nilai mentah sel.
	 * @param type   peran sel: 0=biasa, 1=data, 2=header, 3=main header, 4=lower main header.
	 * @param format format angka: 0=biasa, 1="0,00", 2=persen.
	 * @param colour warna latar sel.
	 */
	public ReportContentVO(Object data, int type, int format, Colour colour) {
		this.data = data;
		this.type = type;
		this.format = format;
		this.colour = colour;
	}

	/**
	 * @param data      nilai mentah sel.
	 * @param type      peran sel: 0=biasa, 1=data, 2=header, 3=main header, 4=lower main header.
	 * @param format    format angka: 0=biasa, 1="0,00", 2=persen.
	 * @param colour    warna latar sel.
	 * @param alignment perataan teks sel.
	 */
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

	/**
	 * @return nilai mentah sel.
	 */
	public Object getData() {
		return data;
	}

	/**
	 * @param data nilai mentah sel.
	 */
	public void setData(Object data) {
		this.data = data;
	}

	/**
	 * @return kode peran sel: 0=biasa, 1=data, 2=header, 3=main header, 4=lower main header.
	 */
	public int getType() {
		return type;
	}

	/**
	 * @param type kode peran sel.
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * @return kode format angka: 0=biasa, 1="0,00", 2=persen.
	 */
	public int getFormat() {
		return format;
	}

	/**
	 * @param format kode format angka.
	 */
	public void setFormat(int format) {
		this.format = format;
	}

	/**
	 * @return warna latar sel (default {@code Colour.WHITE}).
	 */
	public Colour getColour() {
		return colour;
	}

	/**
	 * @param colour warna latar sel.
	 */
	public void setColour(Colour colour) {
		this.colour = colour;
	}

	/**
	 * @return perataan teks sel (default {@code Alignment.GENERAL}).
	 */
	public Alignment getAlignment() {
		return alignment;
	}

	/**
	 * @param alignment perataan teks sel.
	 */
	public void setAlignment(Alignment alignment) {
		this.alignment = alignment;
	}

}

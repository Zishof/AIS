package ais.ui.util;

import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zul.Doublebox;


public class MyDoublebox extends Doublebox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5925156244581127226L;
	private boolean paksa = false;

	public MyDoublebox() {
		super();
		init();
	}

	public MyDoublebox(Double value) throws WrongValueException {
		super(value);
		init();
	}

	private void init() {
		/*
		 * Lebar otomatis mengikuti lebar wadah/kolom: pakai 85% dengan !important agar
		 * mengalahkan lebar tetap bawaan tema ZK, sehingga kotak input menyesuaikan
		 * ukuran kolom yang tersedia (tidak lagi sempit di kolom yang lebar).
		 *
		 * Catatan: bila pemanggil menimpa style via setStyle(...) setelah konstruksi,
		 * sertakan kembali "width:85% !important;" agar lebar tetap mengikuti kolom.
		 */
		setStyle("text-align: right; width:85% !important;");
		setFormat("#,##0.####");
	}

	public void setValue(Double val) {
		super.setValue(val);
	}

	public void disabledPaksa(boolean paksa) {
		this.paksa = paksa;
		super.setDisabled(paksa);
	}

	public void setDisabled(boolean disabled) {
		if (paksa) {
			super.setDisabled(true);
		} else {
			super.setDisabled(disabled);
		}
	}

}

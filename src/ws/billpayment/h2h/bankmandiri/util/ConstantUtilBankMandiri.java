/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ws.billpayment.h2h.bankmandiri.util;

import ws.billpayment.h2h.bankmandiri.Status;

/**
 * 
 * @author Fauzi
 */
public class ConstantUtilBankMandiri extends ais.action.ws.util.ConstantUtil {

	public static final Status SUCCESS_MANDIRI = new Status(false, "00",
			"Sukses");
	public static final Status SYSTEM_TIDAK_BISA_MELAYANI_TRANSAKSI = new Status(
			true, "01", "System tidak bisa melayani transaksi");
	public static final Status TAGIHAN_TIDAK_DITEMUKAN = new Status(true, "B5",
			"Tagihan tidak ditemukan / Nomor Referensi salah");
	public static final Status TAGIHAN_SUDAH_DIBAYAR = new Status(true, "B8",
			"Tagihan sudah dibayar");
	public static final Status TAGIHAN_DIBLOKIR = new Status(true, "C0",
			"Tagihan diblokir, harap hubungi perusahaan");
	public static final Status TRANSAKSI_TIDAK_BISA_DIBATAKAN = new Status(
			true, "86", "Transaksi tidak bisa dibatalkan");
	public static final Status PROVIDER_DATABASE_PROBLEM = new Status(true,
			"87", "Provider Database Problem");
	public static final Status TIMEOUT = new Status(true, "89", "Time Out");
	public static final Status LINK_DOWN = new Status(true, "91", "Link Down");

}

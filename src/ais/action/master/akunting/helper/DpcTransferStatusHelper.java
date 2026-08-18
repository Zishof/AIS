package ais.action.master.akunting.helper;

import java.util.Date;

import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.ProsesTransfer;

/**
 * <h3>DpcTransferStatusHelper — Perkakas Status Pembayaran DPC / Pengajuan Transfer (dapat dipakai ulang)</h3>
 *
 * <p><b>Untuk apa kelas ini.</b> Menyeragamkan cara membaca "sudah sampai mana" sebuah pengajuan
 * transfer dana (DPC — Daftar Pengajuan Cair/Transfer) dari satu {@link DaftarPengajuanTransfer}, agar
 * berbagai dashboard/monitor keuangan (Kas Kecil, Kas Besar, Uang Muka, Pertanggungjawaban) menampilkan
 * informasi yang sama: <i>apakah dananya sudah dibayar</i>, <i>lewat bank apa</i>, dan <i>status
 * prosesnya</i>. Dengan begitu logika ini tidak ditulis ulang di tiap dashboard (reuse), dan bila
 * aturannya berubah cukup diubah di satu tempat.</p>
 *
 * <p><b>Tangga status (dari awal ke akhir):</b></p>
 * <ol>
 *   <li>{@link #BELUM_AJU} — belum ada pengajuan transfer sama sekali.</li>
 *   <li>{@link #DIAJUKAN} — sudah diajukan, tetapi belum masuk proses transfer (DPC).</li>
 *   <li>{@link #PROSES} — sudah masuk proses transfer, menunggu persetujuan.</li>
 *   <li>{@link #DISETUJUI} — sudah disetujui, tetapi dananya belum benar-benar dibayar/direalisasi.</li>
 *   <li>{@link #DIBAYAR} — sudah dibayar/direalisasi (uang keluar). Ini yang dianggap "lunas di DPC".</li>
 * </ol>
 *
 * <p><b>Sumber data.</b> {@code DaftarPengajuanTransfer.getBankSumber()} → bank pembayar;
 * {@code getProsesTransfer()} → proses transfernya; {@code ProsesTransfer.getDisetujuiOleh()} menandai
 * sudah disetujui; {@code getRealisasikanOleh()} menandai sudah benar-benar dibayar. Semua akses lazy
 * dibungkus {@code try/catch} agar data cacat/relasi kosong tidak menggagalkan pemanggil; kelas
 * {@code final} tanpa state, aman dipakai bersama.</p>
 */
public final class DpcTransferStatusHelper {

	public static final String BELUM_AJU = "Belum diajukan transfer";
	public static final String DIAJUKAN = "Diajukan (menunggu proses)";
	public static final String PROSES = "Dalam proses transfer";
	public static final String DISETUJUI = "Disetujui, belum dibayar";
	public static final String DIBAYAR = "Sudah dibayar (DPC)";

	private DpcTransferStatusHelper() {
	}

	/** Ringkasan status pembayaran DPC untuk satu pengajuan transfer. */
	public static class Info {
		/** True bila dana sudah benar-benar dibayar/direalisasi (lunas di DPC). */
		public boolean sudahDibayar;
		/** Salah satu tangga status di atas. */
		public String status = BELUM_AJU;
		/** Nama bank pembayar (kosong bila belum ada). */
		public String bank = "";
		/** Kode proses transfer (kosong bila belum ada). */
		public String kodeTransfer = "";
		/** Tanggal dana direalisasi/dibayar (null bila belum). */
		public Date tanggalBayar;
	}

	/**
	 * Menghitung {@link Info} dari sebuah {@link DaftarPengajuanTransfer}. Aman terhadap {@code null}
	 * (mengembalikan status {@link #BELUM_AJU}).
	 */
	public static Info dari(DaftarPengajuanTransfer dpt) {
		Info in = new Info();
		try {
			if (dpt == null) {
				in.status = BELUM_AJU;
				return in;
			}
			try {
				in.bank = dpt.getBankSumber() == null ? "" : dpt.getBankSumber().getNama();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DpcTransferStatusHelper.java:71");
			}
			ProsesTransfer pt = null;
			try {
				pt = dpt.getProsesTransfer();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DpcTransferStatusHelper.java:76");
			}
			if (pt == null) {
				in.status = DIAJUKAN;
				return in;
			}
			try {
				in.kodeTransfer = pt.getKode();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DpcTransferStatusHelper.java:84");
			}
			boolean setuju = false, bayar = false;
			try {
				setuju = pt.getDisetujuiOleh() != null;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DpcTransferStatusHelper.java:89");
			}
			try {
				bayar = pt.getRealisasikanOleh() != null;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DpcTransferStatusHelper.java:93");
			}
			try {
				in.tanggalBayar = pt.getTanggalRealisasikan();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DpcTransferStatusHelper.java:97");
			}
			if (bayar) {
				in.sudahDibayar = true;
				in.status = DIBAYAR;
			} else if (setuju) {
				in.status = DISETUJUI;
			} else {
				in.status = PROSES;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DpcTransferStatusHelper.java:107");
		}
		return in;
	}
}

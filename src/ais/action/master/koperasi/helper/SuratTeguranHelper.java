package ais.action.master.koperasi.helper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.TransaksiKoperasiDetail;
import ais.ui.util.DashboardUiKit;

/**
 * <h2>SuratTeguranHelper — Pembuat Surat Teguran Tunggakan Angsuran Koperasi</h2>
 *
 * <p>
 * Utilitas statis ini menyusun <b>surat teguran</b> (dalam bentuk HTML siap cetak) bagi anggota yang
 * memiliki angsuran menunggak. Sesuai SOM USPK BAB III tentang pembinaan anggota, pengurus wajib
 * mengirimkan surat teguran apabila anggota terlambat membayar. Alih-alih menulis surat satu per
 * satu, helper ini menghasilkan surat rapi dan seragam langsung dari data tunggakan yang sudah ada,
 * sehingga proses pembinaan menjadi cepat, konsisten, dan tidak ada anggota yang terlewat.
 * </p>
 *
 * <h3>Isi surat</h3>
 * <p>
 * Setiap surat memuat kepala surat (nama koperasi), tanggal, tujuan (nama &amp; alamat anggota),
 * kalimat pembuka yang santun, <b>tabel rincian angsuran yang menunggak</b> (angsuran ke-berapa,
 * jatuh tempo, jumlah tertunggak, dan berapa hari terlambat), total tunggakan, imbauan untuk segera
 * melunasi, serta ruang tanda tangan pengurus. Seluruh tampilan memakai HTML/CSS sederhana yang enak
 * dibaca dan ramah cetak (browser: Ctrl+P) — tanpa pustaka grafik apa pun.
 * </p>
 *
 * <h3>Cara pemakaian &amp; desain</h3>
 * <p>
 * Pemanggil menyediakan objek {@link AnggotaKoperasi} beserta daftar {@link TransaksiKoperasiDetail}
 * angsurannya yang belum dibayar dan sudah lewat jatuh tempo; helper mengembalikan potongan HTML satu
 * surat. Untuk banyak anggota, pemanggil cukup menggabungkan hasilnya dengan pemisah halaman
 * ({@link #pemisahHalaman()}). Kelas final berkonstruktor privat (murni util), tanpa akses basis
 * data, ringan, dan kompatibel Java 1.7. Seluruh pembacaan relasi di-guard agar aman terhadap data
 * yang tidak lengkap.
 * </p>
 *
 * @see KolektibilitasUtil
 */
public final class SuratTeguranHelper {

	private static final long SATU_HARI_MS = 1000L * 60 * 60 * 24;

	private SuratTeguranHelper() {
		// util statis
	}

	/** Pemisah antar-surat agar tiap surat tercetak pada halaman berbeda. */
	public static String pemisahHalaman() {
		return "<div style='page-break-after:always;height:1px;'></div>";
	}

	/**
	 * Susun satu surat teguran (HTML) untuk seorang anggota beserta daftar angsuran menunggaknya.
	 *
	 * @param anggota   anggota yang ditegur
	 * @param menunggak angsuran belum dibayar &amp; sudah lewat jatuh tempo milik anggota tersebut
	 * @param now       tanggal acuan (untuk tanggal surat &amp; hitung hari terlambat)
	 * @return potongan HTML satu surat
	 */
	public static String buildSurat(AnggotaKoperasi anggota, List<TransaksiKoperasiDetail> menunggak, Date now) {
		String namaKoperasi = "Koperasi";
		String nama = "-";
		String alamat = "-";
		try {
			if (anggota != null) {
				if (anggota.getNama() != null) {
					nama = anggota.getNama();
				}
				if (anggota.getAlamat() != null && anggota.getAlamat().trim().length() > 0) {
					alamat = anggota.getAlamat();
				}
				if (anggota.getKoperasi() != null && anggota.getKoperasi().getNama() != null
						&& anggota.getKoperasi().getNama().trim().length() > 0) {
					namaKoperasi = anggota.getKoperasi().getNama();
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/SuratTeguranHelper.java:81");
			// pakai nilai cadangan
		}

		Date acuan = now == null ? new Date() : now;
		SimpleDateFormat sdfPanjang = new SimpleDateFormat("dd MMMM yyyy");
		SimpleDateFormat sdfPendek = new SimpleDateFormat("dd-MM-yyyy");

		StringBuilder baris = new StringBuilder();
		double total = 0.0;
		int no = 1;
		if (menunggak != null) {
			for (TransaksiKoperasiDetail d : menunggak) {
				try {
					if (d == null) {
						continue;
					}
					double jumlah = d.getPokok() + d.getMargin();
					total += jumlah;
					long hari = 0;
					if (d.getTanggal() != null && d.getTanggal().before(acuan)) {
						hari = (acuan.getTime() - d.getTanggal().getTime()) / SATU_HARI_MS;
					}
					baris.append("<tr>")
							.append("<td style='padding:4px 8px;border:1px solid #cbd5e1;text-align:center;'>")
							.append(no++).append("</td>")
							.append("<td style='padding:4px 8px;border:1px solid #cbd5e1;text-align:center;'>Ke-")
							.append(d.getKe() == null ? 0 : d.getKe()).append("</td>")
							.append("<td style='padding:4px 8px;border:1px solid #cbd5e1;text-align:center;'>")
							.append(d.getTanggal() == null ? "-" : sdfPendek.format(d.getTanggal())).append("</td>")
							.append("<td style='padding:4px 8px;border:1px solid #cbd5e1;text-align:right;'>Rp ")
							.append(DashboardUiKit.money(jumlah)).append("</td>")
							.append("<td style='padding:4px 8px;border:1px solid #cbd5e1;text-align:center;'>")
							.append(hari).append(" hari</td>").append("</tr>");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/SuratTeguranHelper.java:115");
					// lewati baris bermasalah
				}
			}
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<div style=\"font-family:'Segoe UI',Arial,sans-serif;color:#0f172a;max-width:720px;")
				.append("margin:0 auto;padding:24px;line-height:1.6;\">");
		// Kop surat
		sb.append("<div style='text-align:center;border-bottom:3px double #0f172a;padding-bottom:8px;margin-bottom:16px;'>")
				.append("<div style='font-size:18px;font-weight:800;'>").append(DashboardUiKit.esc(namaKoperasi))
				.append("</div><div style='font-size:12px;color:#475569;'>Unit Simpan Pinjam</div></div>");
		// Tanggal & nomor
		sb.append("<div style='text-align:right;font-size:13px;margin-bottom:8px;'>")
				.append(sdfPanjang.format(acuan)).append("</div>");
		sb.append("<div style='font-size:14px;font-weight:700;text-align:center;margin:6px 0 14px;")
				.append("text-decoration:underline;'>SURAT TEGURAN PEMBAYARAN ANGSURAN</div>");
		// Tujuan
		sb.append("<div style='font-size:13px;margin-bottom:12px;'>Kepada Yth.<br><b>")
				.append(DashboardUiKit.esc(nama)).append("</b><br>").append(DashboardUiKit.esc(alamat))
				.append("</div>");
		// Isi
		sb.append("<div style='font-size:13px;margin-bottom:10px;'>Dengan hormat,<br>")
				.append("Berdasarkan catatan kami, sampai dengan tanggal surat ini Bapak/Ibu masih memiliki ")
				.append("tunggakan angsuran sebagai berikut:</div>");
		// Tabel
		sb.append("<table style='border-collapse:collapse;width:100%;font-size:12px;margin-bottom:10px;'>")
				.append("<thead><tr style='background:#f1f5f9;'>")
				.append("<th style='padding:4px 8px;border:1px solid #cbd5e1;'>No</th>")
				.append("<th style='padding:4px 8px;border:1px solid #cbd5e1;'>Angsuran</th>")
				.append("<th style='padding:4px 8px;border:1px solid #cbd5e1;'>Jatuh Tempo</th>")
				.append("<th style='padding:4px 8px;border:1px solid #cbd5e1;'>Jumlah</th>")
				.append("<th style='padding:4px 8px;border:1px solid #cbd5e1;'>Terlambat</th></tr></thead><tbody>");
		if (baris.length() == 0) {
			sb.append("<tr><td colspan='5' style='padding:6px;border:1px solid #cbd5e1;text-align:center;'>")
					.append("Tidak ada tunggakan.</td></tr>");
		} else {
			sb.append(baris);
		}
		sb.append("<tr style='background:#fef2f2;font-weight:800;'>")
				.append("<td colspan='3' style='padding:4px 8px;border:1px solid #cbd5e1;text-align:right;'>TOTAL TUNGGAKAN</td>")
				.append("<td style='padding:4px 8px;border:1px solid #cbd5e1;text-align:right;color:#dc2626;'>Rp ")
				.append(DashboardUiKit.money(total)).append("</td>")
				.append("<td style='padding:4px 8px;border:1px solid #cbd5e1;'></td></tr>");
		sb.append("</tbody></table>");
		// Imbauan
		sb.append("<div style='font-size:13px;margin-bottom:16px;'>")
				.append("Sehubungan dengan hal tersebut, kami mengimbau Bapak/Ibu untuk segera menyelesaikan ")
				.append("kewajiban di atas guna menjaga kelancaran usaha koperasi dan kualitas pinjaman Bapak/Ibu. ")
				.append("Apabila pembayaran telah dilakukan, mohon abaikan surat ini. Atas perhatian dan kerja ")
				.append("samanya, kami ucapkan terima kasih.</div>");
		// Tanda tangan
		sb.append("<div style='text-align:right;font-size:13px;margin-top:24px;'>Hormat kami,<br>Pengurus ")
				.append(DashboardUiKit.esc(namaKoperasi)).append("<br><br><br><b>( ................................ )</b></div>");
		sb.append("</div>");
		return sb.toString();
	}
}

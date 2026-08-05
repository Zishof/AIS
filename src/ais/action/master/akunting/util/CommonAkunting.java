package ais.action.master.akunting.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.Messagebox;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.Deposit;
import ais.database.model.DetailKegiatan;
import ais.database.model.ItemBiaya;
import ais.database.model.Kegiatan;
import ais.database.model.LogPembayaran;
import ais.database.model.PengeluaranMahasiswa;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.Closing;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.DanaTalangan;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.JenisKasKecil;
import ais.database.model.akunting.JenisTransaksi;
import ais.database.model.akunting.KasBesar;
import ais.database.model.akunting.KasKecil;
import ais.database.model.akunting.Pajak;
import ais.database.model.akunting.PenggantianKasKecil;
import ais.database.model.akunting.Pertangungjawaban;
import ais.database.model.akunting.PertangungjawabanKasBesar;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.akunting.Transaksi;
import ais.database.model.akunting.Transitori;
import ais.database.model.akunting.UangMuka;
import ais.database.model.asset.PembayaranDpMasterAssetDetail;
import ais.database.model.asset.PembayaranPengadaanMasterAssetDetail;
import ais.database.model.asset.PembayaranTerminMasterAssetDetail;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAssetDetail;
import ais.database.model.asset.PenyusutanAsset;
import ais.database.model.asset.PerjanjianKerjasamaMasterAsset;
import ais.database.model.asset.SaldoAwalMasterAsset;
import ais.database.model.asset.SaldoAwalMasterAssetDetail;
import ais.database.model.payroll.PembayaranGaji;
import ais.database.model.payroll.PembayaranGajiPunyaPegawai;
import ais.database.model.payroll.TransaksiPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.DepositSiswa;
import ais.database.model.sekolah.PembayaranSiswaDetail;
import ais.database.model.sekolah.Tagihan;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.WaktuUtil;

public class CommonAkunting {

	public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMDD");

	/**
	 * Kunci & penghitung in-memory untuk Nomor Jurnal (kode GrupTransaksi) agar DIJAMIN UNIK
	 * walau posting dilakukan MASSAL/PARALEL. Tanpa ini, beberapa thread membaca max(id) yang
	 * sama (insert belum commit) sehingga menghasilkan kode KEMBAR dan memicu pelanggaran unique
	 * constraint grup_transaksi_kode_key. Penghitung tidak pernah mundur dan selalu diselaraskan
	 * dengan max(id) DB sebagai dasar.
	 */
	private static final Object KODE_JURNAL_LOCK = new Object();
	private static long lastNoJurnalSeq = 0L;

	public static String generateNoJurnal(JenisTransaksi jenisTransaksi, boolean tambah) {
		if (jenisTransaksi == null || jenisTransaksi.getNomorSurat() == null) {
			synchronized (KODE_JURNAL_LOCK) {
				Session session = HibernateUtil.currentSession();
				Long max = (Long) session.createCriteria(GrupTransaksi.class).setProjection(Projections.max("id"))
						.uniqueResult();

				long base = (max == null) ? 1L : (max.longValue() + 1L);
				// Jangan pernah memakai nomor lebih kecil dari yang sudah pernah diterbitkan, agar aman
				// saat banyak thread posting bersamaan sebelum ada yang commit (cegah kode kembar).
				long seq = Math.max(base, lastNoJurnalSeq + 1L);
				lastNoJurnalSeq = seq;

				String kode = "00000000000000000000000" + seq;

				Calendar calendar = Calendar.getInstance();

				return dateFormat.format(calendar.getTime()) + kode.substring(kode.length() - 8, kode.length());
			}
		} else if (jenisTransaksi != null && jenisTransaksi.getNomorSurat() != null) {
			Long index = jenisTransaksi.getNomorSurat().getGunakanIndexUrut()
					? jenisTransaksi.getNomorSurat().getNomorIndex()
					: getindex(jenisTransaksi.getNomorSurat());
			if (tambah) {
				NomorSurat.tambahIndexNomorSurat(jenisTransaksi.getNomorSurat());
			}
			String noAgenda = jenisTransaksi.getNomorSurat().format(index, WaktuUtil.getDate());
			return noAgenda;
		} else {
			return "";
		}
	}

	public static Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(GrupTransaksi.class)
				.createAlias("jenisTransaksi", "jenisTransaksi", Criteria.LEFT_JOIN)
				.createAlias("jenisTransaksi.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN)

				.add(nomorSurat.getUrutBerdasarkanNomor() ? Restrictions.eq("jenisTransaksi.nomorSurat", nomorSurat)

						: (nomorSurat.getUrutBerdasarkanKelompok() && nomorSurat.getKelompokNomorSurat() != null
								? Restrictions.eq("nomorSurat.kelompokNomorSurat", nomorSurat.getKelompokNomorSurat())
								: Restrictions.sqlRestriction("true")))

				.add(nomorSurat.getResetUrutanTiapTahun() ? Restrictions.eq("tahun", tahun)
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetUrutanTiapBulan()
						? Restrictions.and(Restrictions.eq("tahun", tahun), Restrictions.eq("bulan", bulan))
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetTiap() != null && (Common.dateFormat8.get().format(nomorSurat.getResetTiap())
						.equals(Common.dateFormat8.get().format(sekarang)) || nomorSurat.getResetTiap().before(sekarang))
								? Restrictions.ge("tanggalPembuatan", nomorSurat.getResetTiap())
								: Restrictions.sqlRestriction("true"))

				.setProjection(Projections.rowCount()).uniqueResult();

		Long index = indexO == null ? null : indexO.longValue();
		if (index == null) {
			index = 0L;
		}
		return ++index;
	}

	/**
	 * Bangun pesan "Transaksi tidak valid" GENERIK yang RINCI untuk kolom jurnal di
	 * layar-layar Posting (Draft Jurnal) — dipakai di SEMUA layar Posting Pembayaran/
	 * Piutang/Dibayar Dimuka/Tabungan/Pengeluaran/Biaya Admin/Biaya Payment Gateway
	 * (mahasiswa, siswa, kas kecil/besar, asset, payroll, dst), bukan cuma menyebut
	 * "Akun debet/kredit tidak ada" tapi juga MENJELASKAN kemungkinan penyebab dan KE MENU
	 * MANA admin harus melengkapi pemetaan akunnya.
	 *
	 * <p>Mekanisme resolusi akun BERBEDA-BEDA antar layar (sebagian pakai pemetaan
	 * {@code ItemBiayaPunyaAkun} per Jurusan/Fakultas — pakai overload
	 * {@link #jelaskanTransaksiTidakValid(Akun, Akun, ItemBiaya, Kegiatan)}; sebagian pakai
	 * Konfigurasi kode akun per gateway pembayaran; sebagian pakai Jenis Kas Kecil/Kas
	 * Besar/Jenis Pembayaran), sehingga hint "ke mana harus benerin" TIDAK BISA digeneralisasi
	 * secara otomatis — caller yang paling tahu mekanisme resolusinya sendiri WAJIB menyuplai
	 * {@code hintDebetKosong}/{@code hintKreditKosong} sendiri (boleh null/kosong bila tidak
	 * ada petunjuk spesifik, pesan tetap tampil tanpa hint tambahan).</p>
	 *
	 * @param akunDebet        akun debet yang sudah di-resolve caller (null jika tidak ketemu)
	 * @param akunKredit       akun kredit yang sudah di-resolve caller (null jika tidak ketemu)
	 * @param hintDebetKosong  penjelasan tambahan KHUSUS layar ini bila akunDebet null (boleh
	 *                         null/kosong)
	 * @param hintKreditKosong penjelasan tambahan KHUSUS layar ini bila akunKredit null (boleh
	 *                         null/kosong)
	 * @return pesan siap tampil di Label kolom "Transaksi" grid Posting
	 */
	public static String jelaskanTransaksiTidakValid(Akun akunDebet, Akun akunKredit, String hintDebetKosong,
			String hintKreditKosong) {
		StringBuilder sb = new StringBuilder("Transaksi tidak valid.");

		if (akunDebet != null) {
			sb.append(" Debet: ").append(akunDebet.getKode()).append("-").append(akunDebet.getNama()).append(".");
		} else {
			sb.append(" Akun debet tidak ada.");
			if (hintDebetKosong != null && !hintDebetKosong.trim().isEmpty()) {
				sb.append(" ").append(hintDebetKosong.trim());
			}
		}

		if (akunKredit != null) {
			sb.append(" Kredit: ").append(akunKredit.getKode()).append("-").append(akunKredit.getNama()).append(".");
		} else {
			sb.append(" Akun kredit tidak ada.");
			if (hintKreditKosong != null && !hintKreditKosong.trim().isEmpty()) {
				sb.append(" ").append(hintKreditKosong.trim());
			}
		}

		return sb.toString();
	}

	/**
	 * Varian {@link #jelaskanTransaksiTidakValid(Akun, Akun, String, String)} KHUSUS untuk
	 * layar yang men-resolve akun kredit via pemetaan {@link ItemBiaya} per Jurusan/Fakultas/
	 * Program/Angkatan (tabel {@code ItemBiayaPunyaAkun}/{@code ItemBiayaPunyaPiutang}) — pola
	 * yang dipakai mayoritas layar Posting Pembayaran/Piutang/Dibayar Dimuka/Tabungan/
	 * Pengeluaran mahasiswa &amp; siswa. Menyertakan kombinasi Jurusan/Program/Angkatan
	 * mahasiswa/calon-mahasiswa ybs di pesan (dari {@code kegiatan}) supaya admin tak perlu
	 * menebak, lalu menunjuk ke menu <i>Item Biaya</i> &gt; item bersangkutan &gt; tab
	 * "Akun Pendapatan"/"Akun Piutang" untuk melengkapinya.
	 *
	 * <p>JANGAN dipakai untuk layar yang resolusi akunnya BUKAN via ItemBiayaPunyaAkun (mis.
	 * Konfigurasi kode akun per gateway pembayaran, Jenis Kas Kecil/Besar) — untuk itu pakai
	 * overload generik dengan hint sendiri.</p>
	 *
	 * @param itemBiaya item biaya terkait transaksi ini
	 * @param kegiatan  kegiatan (utk ambil mahasiswa/calon-mahasiswa, dipakai membentuk
	 *                  deskripsi Fakultas/Jurusan/Program/Angkatan); boleh null
	 */
	public static String jelaskanTransaksiTidakValid(Akun akunDebet, Akun akunKredit, ItemBiaya itemBiaya,
			Kegiatan kegiatan) {
		String hintDebet = langkahLengkapiKolomAkun("Jenis Pembayaran",
				"Jenis Pembayaran/rekening bank yang dipakai", "Akun");
		String hintKredit = hintPemetaanItemBiaya(itemBiaya, kegiatan, "Akun Pendapatan\" atau \"Akun Piutang");

		return jelaskanTransaksiTidakValid(akunDebet, akunKredit, hintDebet, hintKredit);
	}

	/**
	 * Bangun petunjuk LANGKAH-PER-LANGKAH "belum ada pemetaan Akun ... buka menu Item Biaya
	 * &gt; tab X" untuk dipakai sebagai {@code hintDebetKosong}/{@code hintKreditKosong} pada
	 * {@link #jelaskanTransaksiTidakValid(Akun, Akun, String, String)} — dipakai layar yang
	 * akun DEBET/KREDIT-nya di-resolve via pemetaan {@link ItemBiaya} per Jurusan/Fakultas/
	 * Program/Angkatan (tabel {@code ItemBiayaPunyaAkun}/{@code ItemBiayaPunyaPiutang}/dst),
	 * mis. {@code ItemBiaya.ambilDibayarDimuka(Kegiatan)} untuk "Akun Pendapatan Dibayar
	 * Dimuka" atau {@code ambilPendapatanDenda(Kegiatan)} untuk "Akun Pendapatan Denda".
	 *
	 * @param itemBiaya   item biaya terkait
	 * @param kegiatan    dipakai membentuk deskripsi Fakultas/Jurusan/Program/Angkatan
	 * @param namaTabAkun nama tab di menu Item Biaya yang harus dilengkapi, mis.
	 *                    {@code "Akun Pendapatan Dibayar Dimuka"} atau
	 *                    {@code "Akun Pendapatan Denda"}
	 */
	public static String hintPemetaanItemBiaya(ItemBiaya itemBiaya, Kegiatan kegiatan, String namaTabAkun) {
		String konteks = konteksItemBiayaKegiatan(itemBiaya, kegiatan);
		String namaItem = itemBiaya != null && itemBiaya.getNama() != null && !itemBiaya.getNama().trim().isEmpty()
				? itemBiaya.getNama().trim()
				: null;

		StringBuilder sb = new StringBuilder("Langkah perbaikan:");
		if (!konteks.isEmpty()) {
			sb.append(" Belum ada pemetaan Akun untuk ").append(konteks).append(".");
		}
		sb.append(" (1) Buka menu \"Item Biaya\".");
		sb.append(" (2) Cari & buka item biaya").append(namaItem != null ? " \"" + namaItem + "\"." : " terkait.");
		sb.append(" (3) Klik tab \"").append(namaTabAkun).append("\".");
		sb.append(" (4) Klik tombol Tambah/Baru, lalu isi kombinasi Fakultas/Jurusan/Program/Angkatan tersebut "
				+ "(atau biarkan Jurusan & Program kosong agar baris itu berlaku sebagai default/fallback untuk "
				+ "semua kombinasi lain).");
		sb.append(" (5) Pilih Akun yang sesuai pada baris tsb.");
		sb.append(" (6) Klik Simpan.");
		sb.append(" Setelah itu, muat ulang (refresh) halaman Posting ini — transaksi akan otomatis terhitung valid.");
		return sb.toString();
	}

	/**
	 * Petunjuk LANGKAH-PER-LANGKAH untuk kasus PALING UMUM: satu kolom Akun pada satu master
	 * data (bukan tabel pemetaan Jurusan/Fakultas seperti ItemBiaya) yang belum diisi, mis.
	 * Jenis Pembayaran, Jenis Tabungan, Jenis Pengeluaran Mahasiswa, Akun Pembayaran Siswa,
	 * Item Biaya Sekolah. Dipakai sebagai {@code hintDebetKosong}/{@code hintKreditKosong} pada
	 * {@link #jelaskanTransaksiTidakValid(Akun, Akun, String, String)} — SATU SUMBER format teks
	 * langkah agar SEMUA layar Posting (Draft Jurnal) konsisten, tidak perlu menulis ulang
	 * kalimat langkah di tiap file Posting*Action.
	 *
	 * @param namaMenu    nama menu master data yang harus dibuka, mis. {@code "Jenis Pembayaran"}
	 *                    atau {@code "Item Biaya Sekolah"}
	 * @param namaEntitas deskripsi baris/entitas spesifik yang harus dicari di menu itu, mis.
	 *                    {@code "Jenis Pembayaran 'BSI'"} atau {@code "Item Biaya Sekolah 'SPP'"}
	 *                    (boleh menyertakan nama spesifik agar admin tak perlu menebak)
	 * @param namaKolom   nama kolom/field Akun yang harus dilengkapi, mis. {@code "Akun"} atau
	 *                    {@code "Akun Piutang"}
	 */
	public static String langkahLengkapiKolomAkun(String namaMenu, String namaEntitas, String namaKolom) {
		StringBuilder sb = new StringBuilder("Langkah perbaikan:");
		sb.append(" (1) Buka menu \"").append(namaMenu).append("\".");
		sb.append(" (2) Cari & buka ").append(namaEntitas).append(".");
		sb.append(" (3) Isi/lengkapi kolom \"").append(namaKolom).append("\" dengan Akun yang sesuai.");
		sb.append(" (4) Klik Simpan.");
		sb.append(" Setelah itu, muat ulang (refresh) halaman Posting ini — transaksi akan otomatis terhitung valid.");
		return sb.toString();
	}

	/**
	 * Varian {@link #langkahLengkapiKolomAkun(String, String, String)} khusus untuk akun yang
	 * di-resolve dari KONFIGURASI UMUM (parameter global per gateway pembayaran), bukan dari
	 * kolom pada suatu master data — mis. layar Posting Biaya Administrasi/Payment Gateway yang
	 * membaca {@code Common.getKonfigurasi("kode_akun_...")}.
	 *
	 * @param kodeParameter kode parameter Konfigurasi yang harus diisi, mis.
	 *                      {@code "kode_akun_manual_biaya_administrasi"}
	 * @param labelParameter deskripsi singkat kegunaan parameter itu utk ditampilkan ke admin
	 */
	public static String langkahIsiKonfigurasiAkun(String kodeParameter, String labelParameter) {
		StringBuilder sb = new StringBuilder("Langkah perbaikan:");
		sb.append(" (1) Buka menu \"Konfigurasi Umum\".");
		sb.append(" (2) Cari parameter \"").append(kodeParameter).append("\"")
				.append(labelParameter != null && !labelParameter.trim().isEmpty()
						? " (" + labelParameter.trim() + ")." : ".");
		sb.append(" (3) Isi nilainya dengan KODE Akun yang sesuai (harus SAMA PERSIS dengan kolom Kode di menu "
				+ "Akun, bukan nama akun).");
		sb.append(" (4) Klik Simpan.");
		sb.append(" Setelah itu, muat ulang (refresh) halaman Posting ini — transaksi akan otomatis terhitung valid.");
		return sb.toString();
	}

	/** Deskripsi singkat "Item Biaya 'X' utk Jurusan Y / Program Z / Angkatan W" dari Kegiatan; string kosong bila info tak lengkap. */
	public static String konteksItemBiayaKegiatan(ItemBiaya itemBiaya, Kegiatan kegiatan) {
		try {
			String namaItem = itemBiaya == null || itemBiaya.getNama() == null ? null : itemBiaya.getNama().trim();
			if (kegiatan == null) {
				return namaItem == null || namaItem.isEmpty() ? "" : "Item Biaya '" + namaItem + "'";
			}

			String jurusan = null;
			String program = null;
			String angkatan = null;

			if (kegiatan.getMahasiswa() != null) {
				program = kegiatan.getMahasiswa().getProgram();
				angkatan = kegiatan.getMahasiswa().getTahunangkatan() == null ? null
						: kegiatan.getMahasiswa().getTahunangkatan().toString();
				if (kegiatan.getMahasiswa().getJurusan() != null) {
					jurusan = kegiatan.getMahasiswa().getJurusan().getNama();
				}
			} else if (kegiatan.getCalonMahasiswa() != null) {
				program = kegiatan.getCalonMahasiswa().getProgram();
				angkatan = kegiatan.getCalonMahasiswa().getTahun() == null ? null
						: kegiatan.getCalonMahasiswa().getTahun().toString();
				if (kegiatan.getCalonMahasiswa().getProdiLulus() != null) {
					jurusan = kegiatan.getCalonMahasiswa().getProdiLulus().getNama();
				} else if (kegiatan.getCalonMahasiswa().getProdi1() != null) {
					jurusan = kegiatan.getCalonMahasiswa().getProdi1().getNama();
				}
			}

			StringBuilder k = new StringBuilder();
			if (namaItem != null && !namaItem.isEmpty()) {
				k.append("Item Biaya '").append(namaItem).append("'");
			}
			if (jurusan != null && !jurusan.trim().isEmpty()) {
				k.append(k.length() > 0 ? " pada Jurusan '" : "Jurusan '").append(jurusan.trim()).append("'");
			}
			if (program != null && !program.trim().isEmpty()) {
				k.append(k.length() > 0 ? " / Program '" : "Program '").append(program.trim()).append("'");
			}
			if (angkatan != null && !angkatan.trim().isEmpty()) {
				k.append(k.length() > 0 ? " / Angkatan " : "Angkatan ").append(angkatan.trim());
			}
			return k.toString();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/util/CommonAkunting.java:konteksItemBiayaKegiatan");
			return "";
		}
	}

	private static long ids = 100000L;

	public static boolean saveTransaksi(TransaksiPegawai transaksiPegawai, Akun akunDebet,
			PostingHistory postingHistory, boolean apakahUangMasuk, SatuanKerja satuanKerja, Session session)
			throws Exception {

		String ket = "Transaksi pegawai (" + transaksiPegawai.getPegawai().getNama() + ") jenis ("
				+ transaksiPegawai.getJenisTransaksiPegawai().toString() + "), Ket: " + transaksiPegawai.getKeterangan()
				+ ", Tanggal " + Common.dateFormat3.get().format(transaksiPegawai.getTanggal()) + ", Nilai "
				+ Common.numberFormat.get().format(transaksiPegawai.getNilai());

		Akun akunKredit = transaksiPegawai.getJenisTransaksiPegawai().getAkun();

		Long milis = ais.ui.util.WaktuUtil.getDate().getTime() + (++ids);
		String parentCode = "PARENT-" + Long.toHexString(milis).toUpperCase();

		// -------DEBET
		Transaksi transaksiAkunDebet = new Transaksi();
		transaksiAkunDebet.setPostingHistory(postingHistory);

		transaksiAkunDebet.setTanggalTransaksi(transaksiPegawai.getTanggal());

		transaksiAkunDebet.setAkun(akunDebet);

		if (apakahUangMasuk) {
			transaksiAkunDebet.setDebet(Math.abs(transaksiPegawai.getNilai()));
			transaksiAkunDebet.setKredit(0.0);
		} else {
			transaksiAkunDebet.setDebet(0.0);
			transaksiAkunDebet.setKredit(Math.abs(transaksiPegawai.getNilai()));
		}

		transaksiAkunDebet.setKeterangan(ket);

		transaksiAkunDebet.setJumlahTransaksi(
				transaksiAkunDebet.getKredit() < 1.0 ? transaksiAkunDebet.getKredit() : transaksiAkunDebet.getDebet());

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(transaksiAkunDebet.getTanggalTransaksi());
		transaksiAkunDebet.setBulan(calendar.get(Calendar.MONTH) + 1);
		transaksiAkunDebet.setTahun(calendar.get(Calendar.YEAR));
		transaksiAkunDebet.setTanggalDimasukkan(calendar.getTime());
		transaksiAkunDebet.setTanggalTransaksi(calendar.getTime());

		transaksiAkunDebet.setMerupakanDebet(true);
		transaksiAkunDebet.setJenisJurnal(Transaksi.JURNAL_TRANSAKSI);
		transaksiAkunDebet.setSimpan(true);
		transaksiAkunDebet.setStatusPosting(Transaksi.STATUS_POSTING_SELESAI);
		transaksiAkunDebet.setTanggalPosting(ais.ui.util.WaktuUtil.getDate());

		// -------KREDIT
		Transaksi transaksiAkunKredit = new Transaksi();
		transaksiAkunKredit.setPostingHistory(postingHistory);

		transaksiAkunKredit.setTanggalTransaksi(transaksiPegawai.getTanggal());

		transaksiAkunKredit.setAkun(akunKredit);

		if (apakahUangMasuk) {
			transaksiAkunKredit.setKredit(Math.abs(transaksiPegawai.getNilai()));
			transaksiAkunKredit.setDebet(0.0);
		} else {
			transaksiAkunKredit.setKredit(0.0);
			transaksiAkunKredit.setDebet(Math.abs(transaksiPegawai.getNilai()));
		}

		transaksiAkunKredit.setKeterangan(ket);

		transaksiAkunKredit.setJumlahTransaksi(transaksiAkunKredit.getKredit() < 1.0 ? transaksiAkunKredit.getKredit()
				: transaksiAkunKredit.getKredit());

		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(transaksiAkunKredit.getTanggalTransaksi());
		transaksiAkunKredit.setBulan(calendar.get(Calendar.MONTH) + 1);
		transaksiAkunKredit.setTahun(calendar.get(Calendar.YEAR));
		transaksiAkunKredit.setTanggalDimasukkan(calendar.getTime());
		transaksiAkunKredit.setTanggalTransaksi(calendar.getTime());

		transaksiAkunKredit.setMerupakanDebet(false);
		transaksiAkunKredit.setJenisJurnal(Transaksi.JURNAL_TRANSAKSI);
		transaksiAkunKredit.setSimpan(true);
		transaksiAkunKredit.setStatusPosting(Transaksi.STATUS_POSTING_SELESAI);
		transaksiAkunKredit.setTanggalPosting(ais.ui.util.WaktuUtil.getDate());

		GrupTransaksi grupTransaksi = new GrupTransaksi();
		grupTransaksi.setPostingHistory(postingHistory);
		grupTransaksi.setKode(CommonAkunting.generateNoJurnal(JenisTransaksi.DEFAULT, true));
		grupTransaksi.setKeterangan(transaksiAkunDebet.getKeterangan());
		grupTransaksi.setTbmuser(Common.getCurrentUser());
		grupTransaksi.setTanggalTransaksi(transaksiAkunDebet.getTanggalTransaksi());
		grupTransaksi.setParentCode(parentCode);
		grupTransaksi.setSatuanKerja(satuanKerja);
		grupTransaksi.setTransaksiPegawai(transaksiPegawai);
		grupTransaksi.setTotalDebet(transaksiAkunDebet.getDebet());
		grupTransaksi.setTotalKredit(transaksiAkunKredit.getKredit());

		transaksiAkunDebet.setParentCode(parentCode);
		transaksiAkunKredit.setParentCode(parentCode);

		transaksiAkunDebet.setKode(grupTransaksi.getKode());
		transaksiAkunDebet.setGrupTransaksi(grupTransaksi);

		transaksiAkunKredit.setKode(grupTransaksi.getKode());
		transaksiAkunKredit.setGrupTransaksi(grupTransaksi);

		session.save(grupTransaksi);
		session.save(transaksiAkunDebet);
		session.save(transaksiAkunKredit);

		return true;
	}

	public static boolean saveTransaksi(Akun akunDebet, Akun akunKredit, Akun akunDenda, Akun akunPiutangDenda,
			PostingHistory postingHistory, boolean apakahUangMasuk, String ket, Date tanggal, Double nilaiDebet,
			Double nilaiKredit, Double denda, Object reference, SatuanKerja satuanKerja, Session session)
			throws Exception {
		String ref = null;
		return saveTransaksi(akunDebet, akunKredit, akunDenda, akunPiutangDenda, postingHistory, apakahUangMasuk, ket,
				tanggal, nilaiDebet, nilaiKredit, denda, reference, satuanKerja, ref, session);
	}

	public static boolean saveTransaksi(Akun akunDebet, Akun akunKredit, Akun akunDenda, Akun akunPiutangDenda,
			PostingHistory postingHistory, boolean apakahUangMasuk, String ket, Date tanggal, Double nilaiDebet,
			Double nilaiKredit, Double denda, Object reference, SatuanKerja satuanKerja, String ref, Session session)
			throws Exception {

		if (akunDebet == null || akunKredit == null) {
			return true;
		}

		if (nilaiDebet == null || nilaiDebet.equals(0.0)) {
			return true;
		}

		if (nilaiKredit == null || nilaiKredit.equals(0.0)) {
			return true;
		}

		return saveTransaksi(new Akun[] { akunDebet }, new Akun[] { akunKredit }, akunDenda, akunPiutangDenda,
				postingHistory, apakahUangMasuk, ket, tanggal, new Double[] { nilaiDebet },
				new Double[] { nilaiKredit }, denda, reference, satuanKerja, ref, session);
	}

	public static boolean saveTransaksi(Akun akunDebet, Akun akunKredit, Akun akunDenda, Akun akunPiutangDenda,
			PostingHistory postingHistory, boolean apakahUangMasuk, String ket, Date tanggal, Double nilai,
			Double denda, Object reference, SatuanKerja satuanKerja, Session session) throws Exception {
		String ref = null;
		return saveTransaksi(akunDebet, akunKredit, akunDenda, akunPiutangDenda, postingHistory, apakahUangMasuk, ket,
				tanggal, nilai, denda, reference, satuanKerja, ref, session);
	}

	public static boolean saveTransaksi(Akun akunDebet, Akun akunKredit, Akun akunDenda, Akun akunPiutangDenda,
			PostingHistory postingHistory, boolean apakahUangMasuk, String ket, Date tanggal, Double nilai,
			Double denda, Object reference, SatuanKerja satuanKerja, String ref, Session session) throws Exception {

		if (akunDebet == null || akunKredit == null) {
			return true;
		}

		if (nilai == null || nilai.equals(0.0)) {
			return true;
		}

		return saveTransaksi(new Akun[] { akunDebet }, new Akun[] { akunKredit }, akunDenda, akunPiutangDenda,
				postingHistory, apakahUangMasuk, ket, tanggal, new Double[] { nilai }, new Double[] { nilai }, denda,
				reference, satuanKerja, ref, session);
	}

	public static boolean saveTransaksi(Akun[] akunDebets, Akun[] akunKredits, Akun akunDenda, Akun akunPiutangDenda,
			PostingHistory postingHistory, boolean apakahUangMasuk, String ket, Date tanggal, Double[] nilaiDebets,
			Double[] nilaiKredits, Double denda, Object reference, SatuanKerja satuanKerja, Session session)
			throws Exception {
		String ref = null;
		return saveTransaksi(akunDebets, akunKredits, akunDenda, akunPiutangDenda, postingHistory, apakahUangMasuk, ket,
				tanggal, nilaiDebets, nilaiKredits, denda, reference, satuanKerja, ref, session);
	}

	public static boolean saveTransaksi(List<Akun> akunDebets, List<Akun> akunKredits, Akun akunDenda,
			Akun akunPiutangDenda, PostingHistory postingHistory, boolean apakahUangMasuk, String ket, Date tanggal,
			List<Double> nilaiDebets, List<Double> nilaiKredits, Double denda, Object reference,
			SatuanKerja satuanKerja, Session session) throws Exception {
		String ref = null;
		return saveTransaksi(akunDebets.toArray(new Akun[] {}), akunKredits.toArray(new Akun[] {}), akunDenda,
				akunPiutangDenda, postingHistory, apakahUangMasuk, ket, tanggal, nilaiDebets.toArray(new Double[] {}),
				nilaiKredits.toArray(new Double[] {}), denda, reference, satuanKerja, ref, session);
	}

	public static boolean saveTransaksi(Akun[] akunDebets, Akun[] akunKredits, Akun akunDenda, Akun akunPiutangDenda,
			PostingHistory postingHistory, boolean apakahUangMasuk, String ket, Date tanggal, Double[] nilaiDebets,
			Double[] nilaiKredits, Double denda, Object reference, SatuanKerja satuanKerja, String ref, Session session)
			throws Exception {

		if (akunDebets == null || akunKredits == null) {
			return true;
		}

		Date maxClosing = (Date) session.createCriteria(Closing.class).setProjection(Projections.max("tanggal"))
				.uniqueResult();
		if (maxClosing != null && tanggal != null && tanggal.before(maxClosing)) {

			try {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, tanggal jurnal yang Bapak/Ibu masukkan ({V2}) telah melewati tanggal closing (tutup buku), yaitu {V1}. Data pada periode yang sudah closing tidak dapat diubah. Langkah yang dapat dilakukan: (1) gunakan tanggal jurnal setelah tanggal closing {V1}; (2) apabila memang perlu mengubah periode yang telah closing, mohon menghubungi bagian keuangan atau administrator untuk membuka kembali periode tersebut; (3) periksa kembali tanggal transaksi sebelum menyimpan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
						Common.dateFormat4.get().format(maxClosing), Common.dateFormat4.get().format(tanggal));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/util/CommonAkunting.java:351");
				// TODO: handle exception
			}

			return false;
		}

		Long milis = ais.ui.util.WaktuUtil.getDate().getTime() + (++ids);
		String parentCode = "PARENT-" + Long.toHexString(milis).toUpperCase();

		GrupTransaksi grupTransaksi = new GrupTransaksi();

		if (reference != null && reference instanceof CicilanPembayaran) {
			grupTransaksi.setCicilanPembayaran((CicilanPembayaran) reference);
		} else if (reference != null && reference instanceof PembayaranSiswaDetail) {
			grupTransaksi.setPembayaranSiswaDetail((PembayaranSiswaDetail) reference);
		} else if (reference != null && reference instanceof DepositSiswa) {
			grupTransaksi.setDepositSiswa((DepositSiswa) reference);
		} else if (reference != null && reference instanceof LogPembayaran) {
			grupTransaksi.setLogPembayaran((LogPembayaran) reference);
		} else if (reference != null && reference instanceof DetailKegiatan) {
			grupTransaksi.setDetailKegiatan((DetailKegiatan) reference);
		} else if (reference != null && reference instanceof PenerimaanPengadaanMasterAssetDetail) {
			grupTransaksi.setPenerimaanPengadaanMasterAssetDetail((PenerimaanPengadaanMasterAssetDetail) reference);
		} else if (reference != null && reference instanceof PenyusutanAsset) {
			grupTransaksi.setPenyusutanAsset((PenyusutanAsset) reference);
		} else if (reference != null && reference instanceof Deposit) {
			grupTransaksi.setDeposit((Deposit) reference);
		} else if (reference != null && reference instanceof Tagihan) {
			grupTransaksi.setTagihan((Tagihan) reference);
		} else if (reference != null && reference instanceof Pertangungjawaban) {
			grupTransaksi.setPertangungjawaban((Pertangungjawaban) reference);
		} else if (reference != null && reference instanceof UangMuka) {
			grupTransaksi.setUangMuka((UangMuka) reference);
		} else if (reference != null && reference instanceof KasKecil) {
			grupTransaksi.setKasKecil((KasKecil) reference);
		} else if (reference != null && reference instanceof KasBesar) {
			grupTransaksi.setKasBesar((KasBesar) reference);
		} else if (reference != null && reference instanceof PenggantianKasKecil) {
			grupTransaksi.setPenggantianKasKecil((PenggantianKasKecil) reference);
		} else if (reference != null && reference instanceof JenisKasKecil) {
			grupTransaksi.setJenisKasKecil((JenisKasKecil) reference);
		} else if (reference != null && reference instanceof SaldoAwalMasterAssetDetail) {
			grupTransaksi.setSaldoAwalMasterAssetDetail((SaldoAwalMasterAssetDetail) reference);
		} else if (reference != null && reference instanceof PemesananPengadaanMasterAsset) {
			grupTransaksi.setPemesananPengadaanMasterAsset((PemesananPengadaanMasterAsset) reference);
		} else if (reference != null && reference instanceof PengeluaranMahasiswa) {
			grupTransaksi.setPengeluaranMahasiswa((PengeluaranMahasiswa) reference);
		} else if (reference != null && reference instanceof DanaTalangan) {
			grupTransaksi.setDanaTalangan((DanaTalangan) reference);
		} else if (reference != null && reference instanceof PembayaranPengadaanMasterAssetDetail) {
			grupTransaksi.setPembayaranPengadaanMasterAssetDetail((PembayaranPengadaanMasterAssetDetail) reference);
		} else if (reference != null && reference instanceof PembayaranDpMasterAssetDetail) {
			grupTransaksi.setPembayaranDpMasterAssetDetail((PembayaranDpMasterAssetDetail) reference);
		} else if (reference != null && reference instanceof PembayaranTerminMasterAssetDetail) {
			grupTransaksi.setPembayaranTerminMasterAssetDetail((PembayaranTerminMasterAssetDetail) reference);
		} else if (reference != null && reference instanceof PerjanjianKerjasamaMasterAsset) {
			grupTransaksi.setPerjanjianKerjasamaMasterAsset((PerjanjianKerjasamaMasterAsset) reference);
		} else if (reference != null && reference instanceof PembayaranGajiPunyaPegawai) {
			grupTransaksi.setPembayaranGajiPunyaPegawai((PembayaranGajiPunyaPegawai) reference);
		} else if (reference != null && reference instanceof DaftarPengajuanTransfer) {
			grupTransaksi.setDaftarPengajuanTransfer((DaftarPengajuanTransfer) reference);
		} else if (reference != null && reference instanceof Transitori) {
			grupTransaksi.setTransitori((Transitori) reference);
		} else if (reference != null && reference instanceof Pajak) {
			grupTransaksi.setPajak((Pajak) reference);
		} else if (reference != null && reference instanceof PenerimaanPengadaanMasterAsset) {
			grupTransaksi.setPenerimaanPengadaanMasterAsset((PenerimaanPengadaanMasterAsset) reference);
		} else if (reference != null && reference instanceof PertangungjawabanKasBesar) {
			grupTransaksi.setPertangungjawabanKasBesar((PertangungjawabanKasBesar) reference);
		} else if (reference != null && reference instanceof PembayaranGaji) {
			grupTransaksi.setPembayaranGaji((PembayaranGaji) reference);
		} else if (reference != null && reference instanceof SaldoAwalMasterAsset) {
			grupTransaksi.setSaldoAwalMasterAsset((SaldoAwalMasterAsset) reference);
		}
		grupTransaksi.setRef(ref);
		GrupTransaksi apakahSudahada = (GrupTransaksi) session.createCriteria(GrupTransaksi.class)
				.add(Restrictions.eq("kodeUnik", grupTransaksi.getKodeUnik())).setMaxResults(1).uniqueResult();
		if (apakahSudahada != null) {
			apakahSudahada.setPostingHistory(postingHistory);
			Common.refreshUpdate(session, apakahSudahada);
		} else {

			JenisTransaksi jenisTransaksi = null;
			String kode = null;
			for (Object jenis : ConstantValues.ambilBerdasarClass(JenisTransaksi.class).values()) {
				jenisTransaksi = (JenisTransaksi) jenis;
				if (jenisTransaksi != null && jenisTransaksi.getAkun() != null) {
					for (Akun akunDebet : akunDebets) {
						if (akunDebet != null && akunDebet.getId().equals(jenisTransaksi.getAkun().getId())) {
							kode = CommonAkunting.generateNoJurnal(jenisTransaksi, true);
							break;
						}
					}

					if (kode == null || kode.trim().isEmpty()) {
						for (Akun akunKredit : akunKredits) {
							if (akunKredit != null && akunKredit.getId().equals(jenisTransaksi.getAkun().getId())) {
								kode = CommonAkunting.generateNoJurnal(jenisTransaksi, true);
								break;
							}
						}
					}

					if (kode == null || kode.trim().isEmpty()) {
						if (akunPiutangDenda != null
								&& akunPiutangDenda.getId().equals(jenisTransaksi.getAkun().getId())) {
							kode = CommonAkunting.generateNoJurnal(jenisTransaksi, true);
						}
					}
				}

				if (kode != null && !kode.trim().isEmpty()) {
					break;
				}
			}

			if (kode == null || kode.trim().isEmpty()) {
				kode = CommonAkunting.generateNoJurnal(JenisTransaksi.DEFAULT, true);
			}
			grupTransaksi.setRef(ref);
			grupTransaksi.setJenisTransaksi(jenisTransaksi);
			grupTransaksi.setPostingHistory(postingHistory);
			grupTransaksi.setKode(kode);
			grupTransaksi.setKeterangan(ket);
			grupTransaksi.setTbmuser(Common.getCurrentUser());
			grupTransaksi.setTanggalTransaksi(tanggal);
			grupTransaksi.setParentCode(parentCode);
			grupTransaksi.setSatuanKerja(satuanKerja);

			// -------DEBET
			Double totalDebet = 0.0;
			int index = 0;
			for (Akun akunDebet : akunDebets) {
				if (akunDebet != null) {
					Double nilai = nilaiDebets[index];
//				if (nilai > 0.01) {
					index++;
					Transaksi transaksiAkunDebet = new Transaksi();
					transaksiAkunDebet.setPostingHistory(postingHistory);

					transaksiAkunDebet.setTanggalTransaksi(tanggal);

					transaksiAkunDebet.setAkun(akunDebet);

					if (apakahUangMasuk) {
						transaksiAkunDebet.setDebet(Math.abs(nilai));
						transaksiAkunDebet.setKredit(0.0);
					} else {
						transaksiAkunDebet.setDebet(0.0);
						transaksiAkunDebet.setKredit(Math.abs(nilai));
					}

					transaksiAkunDebet.setKeterangan(ket);

					transaksiAkunDebet
							.setJumlahTransaksi(transaksiAkunDebet.getKredit() < 1.0 ? transaksiAkunDebet.getKredit()
									: transaksiAkunDebet.getDebet());

					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.setTime(transaksiAkunDebet.getTanggalTransaksi());
					transaksiAkunDebet.setBulan(calendar.get(Calendar.MONTH) + 1);
					transaksiAkunDebet.setTahun(calendar.get(Calendar.YEAR));
					transaksiAkunDebet.setTanggalDimasukkan(calendar.getTime());
					transaksiAkunDebet.setTanggalTransaksi(calendar.getTime());

					transaksiAkunDebet.setMerupakanDebet(true);
					transaksiAkunDebet.setJenisJurnal(Transaksi.JURNAL_TRANSAKSI);
					transaksiAkunDebet.setSimpan(true);
					transaksiAkunDebet.setStatusPosting(Transaksi.STATUS_POSTING_SELESAI);
					transaksiAkunDebet.setTanggalPosting(ais.ui.util.WaktuUtil.getDate());
					transaksiAkunDebet.setParentCode(parentCode);

					transaksiAkunDebet.setKode(grupTransaksi.getKode());
					transaksiAkunDebet.setGrupTransaksi(grupTransaksi);
					session.save(transaksiAkunDebet);

					totalDebet += transaksiAkunDebet.getDebet();
//				}
				}
			}

			// -------KREDIT
			Double totalKredit = 0.0;
			index = 0;
			for (Akun akunKredit : akunKredits) {
				if (akunKredit != null) {
					Double nilai = nilaiKredits[index] - (denda == null ? 0.0 : denda);
//				if (nilai > 0.01) {
					index++;
					Transaksi transaksiAkunKredit = new Transaksi();
					transaksiAkunKredit.setPostingHistory(postingHistory);

					transaksiAkunKredit.setTanggalTransaksi(tanggal);

					transaksiAkunKredit.setAkun(akunKredit);

					if (apakahUangMasuk) {
						transaksiAkunKredit.setKredit(Math.abs(nilai));
						transaksiAkunKredit.setDebet(0.0);
					} else {
						transaksiAkunKredit.setKredit(0.0);
						transaksiAkunKredit.setDebet(Math.abs(nilai));
					}

					transaksiAkunKredit.setKeterangan(ket);

					transaksiAkunKredit
							.setJumlahTransaksi(transaksiAkunKredit.getKredit() < 1.0 ? transaksiAkunKredit.getKredit()
									: transaksiAkunKredit.getKredit());

					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.setTime(transaksiAkunKredit.getTanggalTransaksi());
					transaksiAkunKredit.setBulan(calendar.get(Calendar.MONTH) + 1);
					transaksiAkunKredit.setTahun(calendar.get(Calendar.YEAR));
					transaksiAkunKredit.setTanggalDimasukkan(calendar.getTime());
					transaksiAkunKredit.setTanggalTransaksi(calendar.getTime());

					transaksiAkunKredit.setMerupakanDebet(false);
					transaksiAkunKredit.setJenisJurnal(Transaksi.JURNAL_TRANSAKSI);
					transaksiAkunKredit.setSimpan(true);
					transaksiAkunKredit.setStatusPosting(Transaksi.STATUS_POSTING_SELESAI);
					transaksiAkunKredit.setTanggalPosting(ais.ui.util.WaktuUtil.getDate());

					transaksiAkunKredit.setParentCode(parentCode);

					transaksiAkunKredit.setKode(grupTransaksi.getKode());
					transaksiAkunKredit.setGrupTransaksi(grupTransaksi);

					session.save(transaksiAkunKredit);
					totalKredit += transaksiAkunKredit.getKredit();
//				}
				}
			}

			grupTransaksi.setTotalDebet(totalDebet);
			grupTransaksi.setTotalKredit(totalKredit);
			session.save(grupTransaksi);

			if (denda != null && denda > 0.1 && akunDenda != null) {

				Transaksi transaksiAkunDenda = new Transaksi();
				transaksiAkunDenda.setPostingHistory(postingHistory);

				transaksiAkunDenda.setTanggalTransaksi(tanggal);

				transaksiAkunDenda.setAkun(akunDenda);

				if (apakahUangMasuk) {
					transaksiAkunDenda.setKredit(Math.abs(denda));
					transaksiAkunDenda.setDebet(0.0);
				} else {
					transaksiAkunDenda.setKredit(0.0);
					transaksiAkunDenda.setDebet(Math.abs(denda));
				}

				transaksiAkunDenda.setKeterangan("Denda untuk : " + ket);

				transaksiAkunDenda.setJumlahTransaksi(denda);

				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(transaksiAkunDenda.getTanggalTransaksi());
				transaksiAkunDenda.setBulan(calendar.get(Calendar.MONTH) + 1);
				transaksiAkunDenda.setTahun(calendar.get(Calendar.YEAR));
				transaksiAkunDenda.setTanggalDimasukkan(calendar.getTime());
				transaksiAkunDenda.setTanggalTransaksi(calendar.getTime());

				transaksiAkunDenda.setMerupakanDebet(false);
				transaksiAkunDenda.setJenisJurnal(Transaksi.JURNAL_TRANSAKSI);
				transaksiAkunDenda.setSimpan(true);
				transaksiAkunDenda.setStatusPosting(Transaksi.STATUS_POSTING_SELESAI);
				transaksiAkunDenda.setTanggalPosting(ais.ui.util.WaktuUtil.getDate());
				transaksiAkunDenda.setParentCode(parentCode);

				transaksiAkunDenda.setKode(grupTransaksi.getKode());
				transaksiAkunDenda.setGrupTransaksi(grupTransaksi);
				session.save(transaksiAkunDenda);

				if (akunPiutangDenda != null) {
					Transaksi transaksiAkunPiutangDenda = new Transaksi();
					transaksiAkunPiutangDenda.setPostingHistory(postingHistory);

					transaksiAkunPiutangDenda.setTanggalTransaksi(tanggal);

					transaksiAkunPiutangDenda.setAkun(akunPiutangDenda);

					if (!apakahUangMasuk) {
						transaksiAkunPiutangDenda.setKredit(Math.abs(denda));
						transaksiAkunPiutangDenda.setDebet(0.0);
					} else {
						transaksiAkunPiutangDenda.setKredit(0.0);
						transaksiAkunPiutangDenda.setDebet(Math.abs(denda));
					}

					transaksiAkunPiutangDenda.setKeterangan("Piutang Denda untuk : " + ket);

					transaksiAkunPiutangDenda.setJumlahTransaksi(denda);

					calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.setTime(transaksiAkunPiutangDenda.getTanggalTransaksi());
					transaksiAkunPiutangDenda.setBulan(calendar.get(Calendar.MONTH) + 1);
					transaksiAkunPiutangDenda.setTahun(calendar.get(Calendar.YEAR));
					transaksiAkunPiutangDenda.setTanggalDimasukkan(calendar.getTime());
					transaksiAkunPiutangDenda.setTanggalTransaksi(calendar.getTime());

					transaksiAkunPiutangDenda.setMerupakanDebet(false);
					transaksiAkunPiutangDenda.setJenisJurnal(Transaksi.JURNAL_TRANSAKSI);
					transaksiAkunPiutangDenda.setSimpan(true);
					transaksiAkunPiutangDenda.setStatusPosting(Transaksi.STATUS_POSTING_SELESAI);
					transaksiAkunPiutangDenda.setTanggalPosting(ais.ui.util.WaktuUtil.getDate());
					transaksiAkunPiutangDenda.setParentCode(parentCode);

					transaksiAkunPiutangDenda.setKode(grupTransaksi.getKode());
					transaksiAkunPiutangDenda.setGrupTransaksi(grupTransaksi);
					session.save(transaksiAkunPiutangDenda);
				}
			}
		}
		return true;
	}

}

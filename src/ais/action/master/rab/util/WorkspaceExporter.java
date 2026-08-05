package ais.action.master.rab.util;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Sessions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.rab.Satuan;
import ais.database.model.rab.Workspace;

/**
 * <h2>Ekspor &amp; impor-ulang Perencanaan Anggaran (Workspace) berbasis kunci ID Workspace</h2>
 *
 * <p>
 * Kelas utilitas terpusat ini menjembatani fitur <b>Download</b> dan <b>Upload-ulang</b> pada halaman
 * Anggaran/RAB. Tujuannya: pengguna dapat mengunduh seluruh item perencanaan anggaran ke berkas Excel
 * (<code>.xlsx</code>) yang <b>langsung dapat diedit</b> di aplikasi spreadsheet biasa, lalu
 * mengunggahnya kembali. <b>Yang menjadi kunci pencocokan adalah kolom pertama "ID Workspace"</b>:
 * setiap baris membawa id basis-data item bersangkutan, sehingga saat diunggah kembali, perubahan
 * (volume, harga satuan, satuan, kode/nama) dapat ditautkan tepat ke item aslinya — bukan membuat
 * data acak baru. Dengan memusatkan logika ini, baik tombol Download maupun handler Upload cukup
 * memanggil satu tempat, sehingga format ekspor dan impor selalu konsisten dan mudah dirawat.
 * </p>
 *
 * <h3>Tata letak kolom (selaras dengan {@link RabImporter})</h3>
 * <p>
 * Susunan kolom sengaja dibuat <b>kompatibel</b> dengan format yang sudah dibaca {@link RabImporter}
 * sehingga berkas hasil Download tetap bisa diunggah lewat jalur upload yang ada. Indeks kolom
 * (berbasis-0):
 * </p>
 * <ol start="0">
 *   <li><b>ID Workspace</b> — kunci pencocokan saat unggah-ulang. Pada {@link RabImporter} kolom ini
 *       hanya dipakai sebagai penanda "baris ada" dan diabaikan sebagai data, sehingga aman.</li>
 *   <li>No — nomor urut tampilan (informatif).</li>
 *   <li>Kode.</li>
 *   <li>Nama / uraian item.</li>
 *   <li>Qty 1 (kuantitas pertama).</li>
 *   <li>Satuan 1.</li>
 *   <li>Pemisah "X" (informatif).</li>
 *   <li>Qty 2 (kuantitas kedua).</li>
 *   <li>Satuan 2.</li>
 *   <li>Volume (umumnya Qty1 &times; Qty2).</li>
 *   <li>Satuan Volume.</li>
 *   <li>Harga Satuan.</li>
 *   <li>(spasi).</li>
 *   <li>Kode Akun.</li>
 * </ol>
 *
 * <h3>Alur pemakaian</h3>
 * <ul>
 *   <li><b>Download</b> — {@link #exportXlsx(List, Integer, String)} menulis seluruh item ke berkas
 *       <code>.xlsx</code> di folder <code>/temp</code> dan mengembalikan {@link File}-nya. Pemanggil
 *       (mis. {@code WorkspaceRevisiAction}) tinggal menyalurkannya ke {@code Filedownload.save(...)}.</li>
 *   <li><b>Upload-ulang berbasis ID</b> — {@link #updateByIdFromXlsx(File)} membaca berkas yang sudah
 *       diedit, lalu untuk tiap baris yang kolom ID-nya menunjuk Workspace yang masih ada, memperbarui
 *       field yang boleh diubah (kode, nama, qty, satuan, volume, harga satuan) dan menghitung ulang
 *       harga total leaf. Mengembalikan daftar Workspace yang berhasil diperbarui agar pemanggil dapat
 *       menggulirkan ulang total induknya (mis. lewat {@code WorkspaceTreeModel.ubahHargaTotalParents}),
 *       memakai kembali logika agregasi yang sudah terbukti. Baris tanpa ID valid dilewati dengan aman
 *       (tidak mengubah data lain), sehingga tidak mengganggu jalur upload penuh yang sudah ada.</li>
 * </ul>
 *
 * <h3>Pertimbangan keamanan data</h3>
 * <p>
 * Karena modul ini menyangkut keuangan, {@code updateByIdFromXlsx} sengaja bersifat <b>aditif dan
 * konservatif</b>: ia hanya menyentuh item yang id-nya cocok, membungkus tiap penyimpanan dalam
 * transaksi tersendiri, dan tidak menghapus apa pun. Resolusi satuan memakai pencarian nama persis
 * (membuat baru bila belum ada, selaras perilaku importer). Volume dihitung ulang dari Qty1 &times;
 * Qty2 agar konsisten dengan cara hitung pada importer. Bila sebuah sel kosong/janggal, nilai aman
 * dipakai (mis. 1.0 untuk kuantitas, 0.0 untuk harga) sehingga proses tidak gagal total.
 * </p>
 *
 * <h3>Contoh</h3>
 * <pre>{@code
 * // Download:
 * File f = WorkspaceExporter.exportXlsx(items, tahun, satuanKerja.getNama());
 * Filedownload.save(new FileInputStream(f),
 *     "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", f.getName());
 *
 * // Upload-ulang berbasis ID (di handler onUpload):
 * List<Workspace> updated = WorkspaceExporter.updateByIdFromXlsx(file);
 * for (Workspace w : updated) { if (treeModel.isLeaf(w)) treeModel.ubahHargaTotalParents(w); }
 * }</pre>
 *
 * @author e-Campus RAB Team
 */
public final class WorkspaceExporter {

	/** Judul kolom berkas ekspor TAHUNAN (urutan = indeks kolom yang dibaca {@link RabImporter}). */
	public static final String[] HEADER = { "ID Workspace", "No", "Kode", "Nama", "Qty", "Satuan", "X", "Qty",
			"Satuan", "Volume", "Sat. Volume", "Harga Satuan", "", "Kode Akun" };

	/**
	 * Judul kolom berkas ekspor BULANAN (urutan = indeks kolom yang dibaca {@code RabBulananImporter}):
	 * kolom 0 ID Workspace (kunci), 2 Kode, 3 Nama, 4..15 nilai per bulan Jan..Des.
	 */
	public static final String[] HEADER_BULANAN = { "ID Workspace", "No", "Kode", "Nama", "Jan", "Feb", "Mar", "Apr",
			"Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nop", "Des" };

	private WorkspaceExporter() {
		// utilitas statis — tidak untuk diinstansiasi
	}

	/**
	 * Menulis seluruh item perencanaan anggaran ke berkas {@code .xlsx} yang siap diunduh dan diedit.
	 *
	 * @param items      daftar {@link Workspace} yang akan diekspor (urut sesuai tampilan pohon)
	 * @param tahun      tahun anggaran (untuk penamaan berkas)
	 * @param namaSatker nama satuan kerja (untuk penamaan berkas; boleh {@code null})
	 * @return berkas {@code .xlsx} yang sudah ditulis di folder {@code /temp}
	 * @throws Exception bila penulisan berkas gagal
	 */
	public static File exportXlsx(List<Workspace> items, Integer tahun, String namaSatker) throws Exception {
		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFSheet sheet = workbook.createSheet("PERENCANAAN ANGGARAN");

		XSSFRow head = sheet.createRow(0);
		for (int c = 0; c < HEADER.length; c++) {
			head.createCell(c).setCellValue(HEADER[c]);
		}

		int r = 1;
		if (items != null) {
			for (Workspace w : items) {
				if (w == null) {
					continue;
				}
				XSSFRow row = sheet.createRow(r);
				row.createCell(0).setCellValue(w.getId() == null ? "" : String.valueOf(w.getId()));
				row.createCell(1).setCellValue(String.valueOf(r));
				row.createCell(2).setCellValue(aman(w.getKode()));
				row.createCell(3).setCellValue(aman(w.getNama()));
				row.createCell(4).setCellValue(w.getQty() == null ? 1.0 : w.getQty());
				row.createCell(5).setCellValue(w.getSatuan() == null ? "" : aman(w.getSatuan().getNama()));
				row.createCell(6).setCellValue("X");
				row.createCell(7).setCellValue(w.getJmlWaktu() == null ? 1.0 : w.getJmlWaktu());
				row.createCell(8).setCellValue(w.getSatuan1() == null ? "" : aman(w.getSatuan1().getNama()));
				row.createCell(9).setCellValue(w.getVolume() == null ? 0.0 : w.getVolume());
				row.createCell(10).setCellValue(aman(w.getSatuanVolume()));
				row.createCell(11).setCellValue(w.getHargaSatuan() == null ? 0.0 : w.getHargaSatuan());
				row.createCell(13).setCellValue(w.getAkun() == null ? "" : aman(w.getAkun().getKode()));
				r++;
			}
		}

		String namaBerkas = "anggaran_" + (tahun == null ? "" : tahun + "_")
				+ (namaSatker == null ? "" : namaSatker.replaceAll("[^A-Za-z0-9]+", "_") + "_") + Common.randLong()
				+ ".xlsx";
		File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + namaBerkas));
		file.getParentFile().mkdirs();
		FileOutputStream out = new FileOutputStream(file);
		try {
			workbook.write(out);
		} finally {
			out.close();
		}
		return file;
	}

	/**
	 * Membaca berkas hasil edit lalu memperbarui item yang id-nya (kolom 0) cocok dengan Workspace
	 * yang masih ada. Field yang diperbarui: kode, nama, Qty1, Satuan1, Qty2, Satuan2, satuan volume,
	 * harga satuan; volume dihitung ulang = Qty1 &times; Qty2 dan harga total = volume &times; harga
	 * satuan. Baris tanpa id valid dilewati.
	 *
	 * @param file berkas {@code .xlsx} hasil unduhan yang telah diedit
	 * @return daftar {@link Workspace} yang berhasil diperbarui (untuk pengguliran ulang total induk)
	 * @throws Exception bila berkas tak dapat dibaca
	 */
	public static List<Workspace> updateByIdFromXlsx(File file) throws Exception {
		XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
		XSSFSheet sheet = workbook.getSheetAt(0);

		List<Workspace> updated = new ArrayList<Workspace>();
		int i = 1;
		while (true) {
			String idCell;
			try {
				idCell = Common.getCellContent(Common.getCell(sheet, 0, i));
			} catch (Exception e) {
				break;
			}
			if (idCell == null || idCell.trim().isEmpty()) {
				break;
			}

			Long id = null;
			try {
				id = Long.parseLong(idCell.trim().replaceAll("[^0-9-]", ""));
			} catch (Exception e) {
				i++;
				continue;
			}

			Session session = HibernateUtil.currentNativeSession();
			Workspace w = (Workspace) session.get(Workspace.class, id);
			if (w == null) {
				HibernateUtil.closeSession();
				i++;
				continue;
			}

			String kode = sel(sheet, 2, i);
			String nama = sel(sheet, 3, i);
			double q1 = angka(sel(sheet, 4, i), 1.0);
			double q2 = angka(sel(sheet, 7, i), 1.0);
			String satNama1 = sel(sheet, 5, i);
			String satNama2 = sel(sheet, 8, i);
			String satVol = sel(sheet, 10, i);
			double harga = angka(sel(sheet, 11, i), 0.0);
			double volume = q1 * q2;

			Satuan satuan1 = resolveSatuan(session, satNama1);
			Satuan satuan2 = resolveSatuan(session, satNama2);

			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			if (!kode.isEmpty()) {
				w.setKode(kode);
			}
			if (!nama.isEmpty()) {
				w.setNama(nama);
			}
			w.setQty(q1);
			w.setJmlWaktu(q2);
			if (satuan1 != null) {
				w.setSatuan(satuan1);
			}
			if (satuan2 != null) {
				w.setSatuan1(satuan2);
			}
			if (satVol != null && !satVol.isEmpty()) {
				w.setSatuanVolume(satVol);
			}
			w.setHargaSatuan(harga);
			w.setVolume(volume);
			w.setHargaTotal(volume * harga);
			session.update(w);
			session.getTransaction().commit();
			HibernateUtil.closeSession();

			updated.add(w);
			i++;
		}
		return updated;
	}

	/**
	 * Versi BULANAN dari {@link #exportXlsx(List, Integer, String)} untuk halaman Anggaran Per Bulan.
	 * Tiap baris memuat ID Workspace (kunci), Kode, Nama, lalu 12 nilai belanja per bulan (Jan..Des)
	 * yang langsung dapat diedit dan diunggah ulang. Susunan kolom selaras dengan
	 * {@code RabBulananImporter} sehingga berkas tetap kompatibel dengan jalur impor bulanan.
	 *
	 * @param items      daftar {@link Workspace} (urut sesuai tampilan pohon)
	 * @param tahun      tahun anggaran (penamaan berkas)
	 * @param namaSatker nama satuan kerja (penamaan berkas; boleh {@code null})
	 * @return berkas {@code .xlsx} yang sudah ditulis di folder {@code /temp}
	 * @throws Exception bila penulisan gagal
	 */
	public static File exportBulananXlsx(List<Workspace> items, Integer tahun, String namaSatker) throws Exception {
		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFSheet sheet = workbook.createSheet("ANGGARAN PER BULAN");

		XSSFRow head = sheet.createRow(0);
		for (int c = 0; c < HEADER_BULANAN.length; c++) {
			head.createCell(c).setCellValue(HEADER_BULANAN[c]);
		}

		int r = 1;
		if (items != null) {
			for (Workspace w : items) {
				if (w == null) {
					continue;
				}
				XSSFRow row = sheet.createRow(r);
				row.createCell(0).setCellValue(w.getId() == null ? "" : String.valueOf(w.getId()));
				row.createCell(1).setCellValue(String.valueOf(r));
				row.createCell(2).setCellValue(aman(w.getKode()));
				row.createCell(3).setCellValue(aman(w.getNama()));
				row.createCell(4).setCellValue(nz(w.getBulan1()));
				row.createCell(5).setCellValue(nz(w.getBulan2()));
				row.createCell(6).setCellValue(nz(w.getBulan3()));
				row.createCell(7).setCellValue(nz(w.getBulan4()));
				row.createCell(8).setCellValue(nz(w.getBulan5()));
				row.createCell(9).setCellValue(nz(w.getBulan6()));
				row.createCell(10).setCellValue(nz(w.getBulan7()));
				row.createCell(11).setCellValue(nz(w.getBulan8()));
				row.createCell(12).setCellValue(nz(w.getBulan9()));
				row.createCell(13).setCellValue(nz(w.getBulan10()));
				row.createCell(14).setCellValue(nz(w.getBulan11()));
				row.createCell(15).setCellValue(nz(w.getBulan12()));
				r++;
			}
		}

		String namaBerkas = "anggaran_bulanan_" + (tahun == null ? "" : tahun + "_")
				+ (namaSatker == null ? "" : namaSatker.replaceAll("[^A-Za-z0-9]+", "_") + "_") + Common.randLong()
				+ ".xlsx";
		File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + namaBerkas));
		file.getParentFile().mkdirs();
		FileOutputStream out = new FileOutputStream(file);
		try {
			workbook.write(out);
		} finally {
			out.close();
		}
		return file;
	}

	/**
	 * Versi BULANAN dari {@link #updateByIdFromXlsx(File)}: untuk tiap baris yang kolom 0-nya menunjuk
	 * Workspace yang masih ada, memperbarui kode, nama, dan 12 nilai bulanan (Jan..Des), lalu menghitung
	 * ulang harga satuan/total = jumlah seluruh bulan. Baris tanpa id valid dilewati.
	 *
	 * @param file berkas {@code .xlsx} hasil unduhan bulanan yang telah diedit
	 * @return daftar {@link Workspace} yang berhasil diperbarui (untuk pengguliran ulang total induk)
	 * @throws Exception bila berkas tak dapat dibaca
	 */
	public static List<Workspace> updateBulananByIdFromXlsx(File file) throws Exception {
		XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
		XSSFSheet sheet = workbook.getSheetAt(0);

		List<Workspace> updated = new ArrayList<Workspace>();
		int i = 1;
		while (true) {
			String idCell;
			try {
				idCell = Common.getCellContent(Common.getCell(sheet, 0, i));
			} catch (Exception e) {
				break;
			}
			if (idCell == null || idCell.trim().isEmpty()) {
				break;
			}

			Long id = null;
			try {
				id = Long.parseLong(idCell.trim().replaceAll("[^0-9-]", ""));
			} catch (Exception e) {
				i++;
				continue;
			}

			Session session = HibernateUtil.currentNativeSession();
			Workspace w = (Workspace) session.get(Workspace.class, id);
			if (w == null) {
				HibernateUtil.closeSession();
				i++;
				continue;
			}

			String kode = sel(sheet, 2, i);
			String nama = sel(sheet, 3, i);
			double[] bln = new double[12];
			double total = 0.0;
			for (int b = 0; b < 12; b++) {
				bln[b] = angka(sel(sheet, 4 + b, i), 0.0);
				total += bln[b];
			}

			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			if (!kode.isEmpty()) {
				w.setKode(kode);
			}
			if (!nama.isEmpty()) {
				w.setNama(nama);
			}
			w.setBulan1(bln[0]);
			w.setBulan2(bln[1]);
			w.setBulan3(bln[2]);
			w.setBulan4(bln[3]);
			w.setBulan5(bln[4]);
			w.setBulan6(bln[5]);
			w.setBulan7(bln[6]);
			w.setBulan8(bln[7]);
			w.setBulan9(bln[8]);
			w.setBulan10(bln[9]);
			w.setBulan11(bln[10]);
			w.setBulan12(bln[11]);
			w.setHargaSatuan(total);
			w.setHargaTotal(total);
			session.update(w);
			session.getTransaction().commit();
			HibernateUtil.closeSession();

			updated.add(w);
			i++;
		}
		return updated;
	}

	/** Mencari Satuan berdasarkan nama persis; membuat baru bila belum ada (selaras importer). */
	private static Satuan resolveSatuan(Session session, String nama) {
		if (nama == null || nama.trim().isEmpty()) {
			return null;
		}
		Session s = HibernateUtil.currentNativeSession();
		Satuan satuan = (Satuan) s.createCriteria(Satuan.class)
				.add(Restrictions.ilike("nama", nama.trim(), MatchMode.EXACT)).setMaxResults(1).uniqueResult();
		if (satuan == null) {
			satuan = new Satuan();
			satuan.setNama(nama.trim());
			satuan.setKeterangan(nama.trim());
			s.getTransaction().begin();
			s.save(satuan);
			s.getTransaction().commit();
		}
		HibernateUtil.closeSession();
		return satuan;
	}

	private static String sel(XSSFSheet sheet, int col, int row) {
		try {
			String v = Common.getCellContent(Common.getCell(sheet, col, row));
			return v == null ? "" : v.trim();
		} catch (Exception e) {
			return "";
		}
	}

	private static double angka(String v, double def) {
		if (v == null || v.trim().isEmpty()) {
			return def;
		}
		try {
			return Double.parseDouble(v.replaceAll("[^0-9,.-]", "").replaceAll(",", "."));
		} catch (Exception e) {
			return def;
		}
	}

	private static String aman(String v) {
		return v == null ? "" : v;
	}

	private static double nz(Double v) {
		return v == null ? 0.0 : v;
	}
}

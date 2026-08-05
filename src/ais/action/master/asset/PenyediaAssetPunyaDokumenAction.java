package ais.action.master.asset;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.asset.PenyediaAssetPunyaDokumen;
import ais.database.model.file.LampiranLain;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * <h3>PenyediaAssetPunyaDokumenAction — Kontroler Monitor Dokumen Milik Penyedia Aset</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini merupakan kontroler ZK (ZKoss 5.5) untuk halaman monitor dan verifikasi
 * dokumen persyaratan yang dimiliki oleh penyedia aset (vendor/supplier). Dalam
 * proses pengadaan aset, setiap penyedia aset diwajibkan untuk melengkapi dokumen-dokumen
 * tertentu sesuai dengan template dokumen yang telah dikonfigurasi pada master
 * {@code DokumenPenyediaAsset}. Halaman ini menampilkan status kelengkapan dokumen
 * dari setiap penyedia aset, memungkinkan operator untuk: (1) memverifikasi dokumen
 * yang telah diunggah oleh penyedia, (2) mengubah status dokumen (Belum/Verifikasi/Revisi),
 * (3) mengunduh berkas lampiran dokumen, dan (4) mengunduh semua lampiran sekaligus
 * dalam format ZIP yang diorganisir berdasarkan nama dan kode penyedia aset.
 * Data ini mendukung proses administrasi pengadaan yang patuh terhadap regulasi
 * kelengkapan dokumen vendor.
 *
 * <b>Cara kerja:</b><br>
 * Kontroler ini mengimplementasikan {@code DataCriteria} dan {@code DataSearchDefault}
 * tetapi tidak mengimplementasikan {@code DataInitDefault} karena tidak ada operasi
 * tambah/ubah data dari sisi administrator (data {@code PenyediaAssetPunyaDokumen}
 * dibuat secara otomatis ketika dokumen diunggah oleh penyedia). Inisialisasi
 * meliputi pengaturan combobox filter status (Belum/Verifikasi/Revisi/Semua),
 * pencarian awal, paginasi, dan pendaftaran tombol ekspor dan unduh lampiran ZIP.
 * Grid menggunakan {@code PenyediaAssetPunyaDokumenRenderer} yang menampilkan
 * informasi penyedia (nama, keterangan, kontak), nama dokumen, keterangan,
 * status wajib, status verifikasi (combobox langsung simpan), dan kontrol unggah/unduh
 * file via {@code LampiranLain.createDownloadUploadFileLain}. File unggah diblokir
 * jika status dokumen sudah "Verifikasi". Fitur unduh ZIP mengumpulkan semua lampiran
 * dari hasil pencarian saat ini dan mengompresinya ke file ZIP sementara.
 *
 * <b>Threading:</b><br>
 * Seluruh interaksi basis data berjalan di thread event ZK. Operasi unduh ZIP
 * melibatkan operasi I/O file yang berpotensi lambat untuk set data besar, namun
 * tetap dieksekusi di thread event ZK. Tidak ada thread latar belakang. Sesi
 * Hibernate dikelola per-thread oleh konteks ZK. Direktori sementara untuk ZIP
 * dibuat di {@code /opt/ecampus/lampiran_[timestamp]}.
 *
 * <b>Pemeliharaan:</b><br>
 * Status dokumen didefinisikan sebagai konstanta pada kelas {@code PenyediaAssetPunyaDokumen}:
 * {@code BELUM}, {@code VERIFIKASI}, dan {@code REVISI}. Jika ada status baru,
 * tambahkan Comboitem di {@code doAfterCompose} dan di renderer. Unggah file
 * menggunakan {@code LampiranLain} dengan entity class name sebagai kunci relasi.
 * Untuk menambah filter pencarian baru, tambahkan komponen UI di ZUL dan tambahkan
 * {@code Restrictions} di {@code initCriteria}. Direktori temp ZIP tidak dibersihkan
 * secara otomatis; pertimbangkan penambahan mekanisme cleanup untuk produksi.
 *
 * @author Sistem AIS
 * @version 1.0
 * @see PenyediaAssetPunyaDokumen
 * @see PenyediaAsset
 * @see LampiranLain
 * @see DataCriteria
 * @see DataSearchDefault
 */
public class PenyediaAssetPunyaDokumenAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault {

	/**
	 * Nomor versi serialisasi untuk kompatibilitas serialisasi Java.
	 * Nilai ini tidak boleh diubah kecuali terjadi perubahan struktural pada kelas.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Komponen paginasi ZK untuk navigasi halaman pada grid data utama. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar dokumen milik penyedia aset. */
	private MyGrid grid;

	/** Kotak teks untuk mencari berdasarkan nama dokumen yang dimiliki penyedia. */
	private Textbox searchnama;

	/** Kotak teks untuk mencari berdasarkan nama atau kode penyedia aset. */
	private Textbox searchcalnama;

	/**
	 * Combobox filter status dokumen, memiliki pilihan: Belum, Verifikasi, Revisi,
	 * dan Semua. Nilai null pada item "Semua" berarti tidak ada filter status.
	 */
	private Combobox searchstatus;

	/** Tombol referensi pada toolbar yang digunakan sebagai anchor untuk tombol tambahan. */
	private MyToolbarbuttonConfig find;

	/**
	 * <b>Tujuan:</b> Memeriksa keamanan akses sebelum komponen ZK dikompilasi oleh
	 * framework, memastikan hanya pengguna terautentikasi yang dapat mengakses
	 * halaman monitor dokumen penyedia aset.
	 *
	 * <b>Cara kerja:</b> Memanggil {@code Common.doCheckSecurity()} untuk verifikasi
	 * autentikasi sesi. Jika sesi tidak valid, pengguna dialihkan ke login.
	 * Kemudian memanggil {@code super.doBeforeCompose} untuk melanjutkan proses
	 * perakitan komponen ZK secara normal sesuai implementasi induk.
	 *
	 * <b>Parameter:</b><br>
	 * - {@code page}: halaman ZK yang sedang dikompose<br>
	 * - {@code parent}: komponen induk dalam hierarki ZK<br>
	 * - {@code compInfo}: metadata komponen dari file ZUL
	 *
	 * <b>Return:</b> {@code ComponentInfo} dari pemanggilan super yang diperlukan
	 * oleh framework ZK.
	 *
	 * <b>Penanganan error:</b> Redirect otomatis oleh {@code Common.doCheckSecurity()}
	 * jika autentikasi gagal, sehingga eksekusi tidak lanjut ke {@code super}.
	 *
	 * <b>Pemeliharaan:</b> Jangan hapus pemanggilan keamanan ini karena merupakan
	 * lapisan proteksi halaman yang kritis.
	 *
	 * @param page     halaman ZK yang sedang dikompose
	 * @param parent   komponen induk dalam hierarki ZK
	 * @param compInfo informasi metadata komponen
	 * @return ComponentInfo hasil dari super.doBeforeCompose
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b> Menginisialisasi seluruh komponen UI setelah framework ZK
	 * selesai merakit komponen ZUL, termasuk pengisian combobox status, pengaturan
	 * paginasi, pencarian awal, pendaftaran tombol ekspor data, dan pendaftaran
	 * tombol unduh lampiran ZIP.
	 *
	 * <b>Cara kerja:</b> Urutan inisialisasi: (1) super dan inisialisasi bahasa,
	 * (2) pengisian combobox {@code searchstatus} dengan pilihan BELUM, VERIFIKASI,
	 * REVISI, dan Semua; item "Semua" dipilih secara default (nilai null = tidak
	 * filter), (3) menyetel properti combobox (width 90%, readonly untuk navigasi
	 * keyboard), (4) menjalankan pencarian awal, (5) memasang listener paginasi,
	 * (6) mendaftarkan tombol ekspor data dengan array kolom yang mencakup informasi
	 * penyedia dan dokumen, (7) mendaftarkan tombol "Lampiran" yang saat diklik
	 * mengumpulkan semua lampiran dari hasil pencarian saat ini, menyalinnya ke
	 * direktori sementara dengan nama file yang mengandung kode dan nama penyedia,
	 * mengompresi direktori tersebut menjadi ZIP, dan mengirimkan ZIP ke browser
	 * pengguna via {@code Filedownload.save}.
	 *
	 * <b>Parameter:</b> {@code comp} adalah komponen root halaman ZUL.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Melempar {@code Exception}. Pembuatan direktori media
	 * dibungkus dalam try-catch kosong untuk mencegah gagal jika direktori sudah ada.
	 * Error I/O saat menyalin file tidak ditangani dan akan menghentikan proses ZIP.
	 *
	 * <b>Pemeliharaan:</b> Direktori sementara ZIP dibuat di {@code /opt/ecampus/}
	 * dan tidak dihapus setelah unduhan selesai. Pertimbangkan menambahkan
	 * {@code fileFolderLampiran.deleteOnExit()} untuk cleanup otomatis pada JVM
	 * shutdown. Jika ada status dokumen baru, tambahkan Comboitem setelah baris
	 * REVISI di blok inisialisasi combobox.
	 *
	 * @param comp komponen root halaman ZUL yang telah selesai dirakit
	 * @throws Exception jika terjadi kesalahan inisialisasi komponen atau I/O
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		Comboitem comboitem = new Comboitem(PenyediaAssetPunyaDokumen.BELUM);
		if (comboitem != null) { comboitem.setValue(PenyediaAssetPunyaDokumen.BELUM); }
		searchstatus.appendChild(comboitem);
		comboitem = new Comboitem(PenyediaAssetPunyaDokumen.VERIFIKASI);
		if (comboitem != null) { comboitem.setValue(PenyediaAssetPunyaDokumen.VERIFIKASI); }
		searchstatus.appendChild(comboitem);
		comboitem = new Comboitem(PenyediaAssetPunyaDokumen.REVISI);
		if (comboitem != null) { comboitem.setValue(PenyediaAssetPunyaDokumen.REVISI); }
		searchstatus.appendChild(comboitem);
		comboitem = new Comboitem("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		searchstatus.appendChild(comboitem);
		if (searchstatus != null) { searchstatus.setSelectedItem(comboitem); }
		if (searchstatus != null) { searchstatus.setWidth("90%"); }
		if (searchstatus != null) { searchstatus.setReadonly(true); }

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "penyediaAsset.kode", "penyediaAsset.nama",
				"dokumenPenyediaAsset.kode", "dokumenPenyediaAsset.nama", "dokumenPenyediaAsset.wajib",
				"dokumenPenyediaAsset.status", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		MyToolbarbuttonConfig downloadLampiran = new MyToolbarbuttonConfig("Lampiran", "/img/attachment-icon.png");
		downloadLampiran.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<PenyediaAssetPunyaDokumen> penyediaAssetPunyaDokumens = initCriteria(true).list();
				File fileFolderLampiran = new File(
						"/opt/ecampus/lampiran_" + ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis());
				fileFolderLampiran.mkdirs();
				System.out.println("fileFolderLampiran => " + fileFolderLampiran.getAbsolutePath());
				File folderOut = new File(Common.REAL_PATH + "/media/");
				try {
					folderOut.mkdirs();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PenyediaAssetPunyaDokumenAction.java:251");
					// direktori sudah ada atau tidak dapat dibuat, lanjutkan
				}
				for (PenyediaAssetPunyaDokumen penyediaAssetPunyaDokumen : penyediaAssetPunyaDokumens) {
					LampiranLain lam = LampiranLain.ambil(penyediaAssetPunyaDokumen.getId(),
							PenyediaAssetPunyaDokumen.class.getName());
					if (lam != null) {
						File file;
						if (lam.getGdrive() != null && !lam.getGdrive().trim().isEmpty()) {
							file = new File(folderOut.getAbsolutePath() + "/"
									+ URLEncoder.encode(lam.getNama(), "UTF-8") + ".txt");
							FileUtils.writeStringToFile(file, lam.forwardGDriveUrl());
						} else if (lam.getLink() != null && !lam.getLink().trim().isEmpty()) {
							file = new File(folderOut.getAbsolutePath() + "/"
									+ URLEncoder.encode(lam.getNama(), "UTF-8") + ".txt");
							FileUtils.writeStringToFile(file, lam.getLink().trim());
						} else {
							file = lam.ambilFile();
						}

						File fileCopy = new File(fileFolderLampiran.getAbsolutePath() + "/"
								+ URLEncoder.encode(penyediaAssetPunyaDokumen.getPenyediaAsset().getKode() + "_"
										+ penyediaAssetPunyaDokumen.getPenyediaAsset().getNama(), "UTF-8")
								+ "_" + file.getName());
						System.out.println("fileCopy => " + fileCopy.getAbsolutePath());
						FileOutputStream fileOutputStream = new FileOutputStream(fileCopy);
						FileInputStream fileInputStream = new FileInputStream(file);
						IOUtils.copyLarge(fileInputStream, fileOutputStream);
						fileInputStream.close();
						fileOutputStream.close();
					}
				}
				penyediaAssetPunyaDokumens.clear();
				penyediaAssetPunyaDokumens = null;
				File fileFolderLampiranZip = new File(fileFolderLampiran.getAbsolutePath() + ".zip");
				Common.zipDir(fileFolderLampiranZip.getAbsolutePath(), fileFolderLampiran.getAbsolutePath());
				Filedownload.save(fileFolderLampiranZip, "application/zip");
			}
		});
		Common.appendKeToolbar(downloadLampiran, find, comp);
	}

	/**
	 * <b>Tujuan:</b> Kelas renderer baris grid yang merender satu entitas
	 * {@code PenyediaAssetPunyaDokumen} ke dalam komponen {@code Row} ZK,
	 * menampilkan informasi lengkap penyedia, dokumen, lampiran, dan kontrol
	 * status verifikasi yang dapat diubah langsung.
	 *
	 * <b>Cara kerja:</b> Renderer ini lebih kompleks dibandingkan renderer CRUD
	 * biasa karena menggabungkan informasi dari dua entitas terkait
	 * ({@code PenyediaAsset} dan {@code DokumenPenyediaAsset}). Setiap baris
	 * menampilkan: (1) Detail toggle ({@code MyDetail} open) yang berisi kontrol
	 * unggah/unduh lampiran dan keterangan, (2) Vbox penyedia dengan nama (revisi),
	 * keterangan kecil-bold, nomor HP, dan email, (3) Vbox nama dokumen (revisi),
	 * (4) Textbox keterangan yang menyimpan langsung saat onChange, (5) Label wajib
	 * (Ya/Tidak), (6) Combobox status (Belum/Verifikasi/Revisi) yang menyimpan
	 * langsung saat onChange. Kontrol unggah diblokir jika status "Verifikasi" via
	 * flag {@code tampilUpload}. Keterangan disimpan saat onChange ke entitas.
	 *
	 * <b>Parameter:</b><br>
	 * - {@code arg0}: baris {@code Row} ZK yang akan diisi<br>
	 * - {@code arg1}: objek {@code PenyediaAssetPunyaDokumen} yang akan dirender
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Melempar {@code Exception}. Lampiran yang tidak
	 * ada tidak akan menyebabkan error karena {@code LampiranLain.ambil} mengembalikan
	 * null yang ditangani oleh kontrol lampiran.
	 *
	 * <b>Pemeliharaan:</b> Jika ada status baru, tambahkan Comboitem baru pada
	 * combobox status di dalam metode {@code render} ini, sebelum pemanggilan
	 * {@code Common.selectComboItem}.
	 */
	class PenyediaAssetPunyaDokumenRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu baris data {@code PenyediaAssetPunyaDokumen}
		 * ke dalam komponen {@code Row} ZK dengan semua kolom, kontrol interaktif
		 * status verifikasi, dan fasilitas unggah/unduh lampiran dokumen.
		 *
		 * <b>Cara kerja:</b> Metode ini dipanggil oleh framework ZK untuk setiap
		 * baris yang perlu dirender. Detail penyedia (nama, keterangan, HP, email)
		 * ditampilkan via RevisiHelper dan metode helper entitas. Kontrol unggah
		 * berkas memanggil {@code LampiranLain.createDownloadUploadFileLain} dengan
		 * flag tampilUpload yang false ketika status dokumen sudah "Verifikasi".
		 * Combobox status memiliki listener onChange yang langsung memperbarui
		 * entitas di basis data hanya jika entitas sudah tersimpan (ID tidak null).
		 * Textbox keterangan juga menyimpan langsung saat onChange.
		 *
		 * <b>Parameter:</b><br>
		 * - {@code arg0}: baris Row ZK target<br>
		 * - {@code arg1}: objek PenyediaAssetPunyaDokumen yang akan ditampilkan
		 *
		 * <b>Return:</b> Tidak ada nilai kembalian (void).
		 *
		 * <b>Penanganan error:</b> Melempar {@code Exception}. Null handling diterapkan
		 * pada lampiran dan komponen yang mungkin null.
		 *
		 * <b>Pemeliharaan:</b> Urutan pemanggilan {@code setParent} harus sesuai
		 * urutan kolom di file ZUL terkait. Flag {@code tampilUpload} mengontrol
		 * visibilitas tombol unggah untuk mencegah penggantian dokumen yang sudah
		 * terverifikasi.
		 *
		 * @param arg0 baris Row ZK yang akan diisi komponen
		 * @param arg1 objek data PenyediaAssetPunyaDokumen yang akan dirender
		 * @throws Exception jika terjadi kesalahan pembuatan komponen atau akses data
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PenyediaAssetPunyaDokumen penyediaAssetPunyaDokumen = (PenyediaAssetPunyaDokumen) arg1;
			PenyediaAsset penyediaAsset = penyediaAssetPunyaDokumen.getPenyediaAsset();

			MyDetail detail = new MyDetail();
			detail.setOpen(true);
			detail.setParent(arg0);

			Vbox aa;
			(aa = RevisiHelper.createNewRevisi(PenyediaAsset.class, penyediaAsset, penyediaAsset.getNama()))
					.setParent(arg0);
			aa.appendChild(new MyLabelAgakKecilBold(penyediaAsset.getKeterangan()));

			penyediaAsset.tampilkanHp(aa);
			penyediaAsset.tampilkanEmail(aa);

			(RevisiHelper.createNewRevisi(PenyediaAssetPunyaDokumen.class, penyediaAssetPunyaDokumen,
					penyediaAssetPunyaDokumen.getDokumenPenyediaAsset().getNama())).setParent(arg0);

			final Vbox myvbox = new Vbox();
			myvbox.setParent(detail);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			hbox.setWidth("100%");

			Boolean tampilUpload = !penyediaAssetPunyaDokumen.getStatus()
					.equalsIgnoreCase(PenyediaAssetPunyaDokumen.VERIFIKASI);

			LampiranLain.createDownloadUploadFileLain(hbox, penyediaAssetPunyaDokumen.getId(),
					PenyediaAssetPunyaDokumen.class.getName(),
					penyediaAssetPunyaDokumen.getDokumenPenyediaAsset().getNama(), false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, tampilUpload);

			final Textbox keterangan = new Textbox(penyediaAssetPunyaDokumen.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setRows(2);
			keterangan.setParent(arg0);
			new Label(penyediaAssetPunyaDokumen.getDokumenPenyediaAsset().getWajib() ? "Ya" : "Tidak").setParent(arg0);

			Combobox combobox = new Combobox();
			Comboitem comboitem = new Comboitem(PenyediaAssetPunyaDokumen.BELUM);
			comboitem.setValue(PenyediaAssetPunyaDokumen.BELUM);
			combobox.appendChild(comboitem);
			comboitem = new Comboitem(PenyediaAssetPunyaDokumen.VERIFIKASI);
			comboitem.setValue(PenyediaAssetPunyaDokumen.VERIFIKASI);
			combobox.appendChild(comboitem);
			comboitem = new Comboitem(PenyediaAssetPunyaDokumen.REVISI);
			comboitem.setValue(PenyediaAssetPunyaDokumen.REVISI);
			combobox.appendChild(comboitem);
			Common.selectComboItem(combobox, penyediaAssetPunyaDokumen.getStatus());
			combobox.setWidth("90%");
			combobox.setReadonly(true);

			// Satu sel: status pemeriksaan + tanggal "Berlaku s/d" (masa berlaku dokumen).
			// Digabung dalam Vbox agar tidak perlu menambah kolom grid di ZUL.
			Vbox statusBox = new Vbox();
			statusBox.setSpacing("3px");
			statusBox.setWidth("100%");
			statusBox.appendChild(combobox);
			Hbox berlakuBox = new Hbox();
			berlakuBox.setAlign("center");
			berlakuBox.appendChild(new Label("Berlaku s/d:"));
			final org.zkoss.zul.Datebox berlakuSampai = new org.zkoss.zul.Datebox(
					penyediaAssetPunyaDokumen.getTanggalBerlakuSampai());
			berlakuSampai.setFormat("dd-MM-yyyy");
			berlakuSampai.setWidth("130px");
			berlakuBox.appendChild(berlakuSampai);
			statusBox.appendChild(berlakuBox);
			arg0.appendChild(statusBox);

			berlakuSampai.addEventListener("onChange", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					penyediaAssetPunyaDokumen.setTanggalBerlakuSampai(berlakuSampai.getValue());
					if (penyediaAssetPunyaDokumen.getId() != null) {
						Common.refreshUpdate(penyediaAssetPunyaDokumen);
					}
				}
			});

			combobox.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Combobox combobox = (Combobox) arg0.getTarget();
					penyediaAssetPunyaDokumen.setStatus((String) combobox.getSelectedItem().getValue());
					if (penyediaAssetPunyaDokumen.getId() != null) {
						Common.refreshUpdate(penyediaAssetPunyaDokumen);
					}
				}
			});

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					penyediaAssetPunyaDokumen.setKeterangan(keterangan.getValue());
					Common.refreshSaveOrUpdate(penyediaAssetPunyaDokumen);

				}
			};

			keterangan.addEventListener("onChange", eventListener);
		}

	}

	/**
	 * <b>Tujuan:</b> Membangun objek {@code Criteria} Hibernate untuk pencarian data
	 * dokumen milik penyedia aset berdasarkan filter status, nama dokumen, dan
	 * nama/kode penyedia yang aktif pada antarmuka pencarian.
	 *
	 * <b>Cara kerja:</b> Metode ini membangun kriteria dengan join alias ke entitas
	 * terkait: {@code createAlias("penyediaAsset", "penyediaAsset")} dan
	 * {@code createAlias("dokumenPenyediaAsset", "dokumenPenyediaAsset")}. Tiga
	 * filter diterapkan: (1) Filter status: jika item combobox status memiliki nilai
	 * tidak null, diterapkan {@code Restrictions.eq("status", nilai)}; jika null
	 * (pilihan "Semua"), semua data ditampilkan, (2) Filter nama dokumen: pencarian
	 * case-insensitive pada {@code dokumenPenyediaAsset.nama} menggunakan
	 * {@code MatchMode.ANYWHERE} jika tidak kosong, (3) Filter penyedia: pencarian
	 * case-insensitive pada {@code penyediaAsset.nama} atau {@code penyediaAsset.kode}
	 * menggunakan OR logic. Jika {@code order} true, diurutkan berdasarkan
	 * {@code tanggal_dirubah} secara descending (terbaru di atas).
	 *
	 * <b>Parameter:</b> {@code order} menentukan apakah ada klausa ORDER BY.
	 *
	 * <b>Return:</b> Objek {@code Criteria} Hibernate yang siap dieksekusi,
	 * termasuk untuk query list lengkap (tanpa limit) saat unduh ZIP.
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan error khusus. Jika alias
	 * tidak ditemukan, Hibernate akan melempar exception.
	 *
	 * <b>Pemeliharaan:</b> Perhatikan bahwa field urutan menggunakan nama kolom
	 * database {@code tanggal_dirubah} (snake_case) bukan nama properti Java.
	 * Jika nama kolom berubah di basis data, perbarui string ini.
	 *
	 * @param order jika true, hasil diurutkan berdasarkan tanggal diubah descending
	 * @return objek Criteria Hibernate yang sudah dikonfigurasi dengan semua filter
	 */
	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(PenyediaAssetPunyaDokumen.class)

				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()))

				.createAlias("penyediaAsset", "penyediaAsset")

				.createAlias("dokumenPenyediaAsset", "dokumenPenyediaAsset")

		;

		if (order)
			criteria.addOrder(Order.desc("tanggal_dirubah"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("dokumenPenyediaAsset.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchcalnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("penyediaAsset.nama", searchcalnama.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("penyediaAsset.kode", searchcalnama.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("penyediaAsset.kode", searchcalnama.getValue().trim(),
												MatchMode.ANYWHERE))));
		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Melakukan pencarian data dokumen milik penyedia aset dan
	 * memperbarui tampilan grid utama beserta paginasi sesuai filter yang aktif
	 * dan halaman yang sedang aktif.
	 *
	 * <b>Cara kerja:</b> Metode pertama memanggil {@code Common.initPaging} dengan
	 * kriteria tanpa urutan untuk menghitung total halaman berdasarkan jumlah
	 * data yang sesuai filter. Kemudian mengambil data halaman saat ini dengan
	 * batasan {@code ROWS_COUNT_ON_PAGE} baris dimulai dari offset berdasarkan
	 * halaman aktif paginasi. Hasil dibungkus dalam {@code SimpleListModel} dan
	 * ditetapkan ke grid bersama renderer baru. Metode ini dipanggil saat
	 * inisialisasi halaman, saat paginasi berubah, dan saat filter pencarian
	 * diubah oleh pengguna.
	 *
	 * <b>Parameter:</b> {@code event} adalah event ZK pemicu pencarian, dapat null
	 * jika dipanggil secara programatik dari inisialisasi.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan error eksplisit. Anotasi
	 * {@code @SuppressWarnings("unchecked")} digunakan untuk type-safety.
	 *
	 * <b>Pemeliharaan:</b> Metode ini juga digunakan secara implisit oleh
	 * {@code Common.cetakData} saat mengekspor data (yang mengambil semua
	 * data tanpa batas halaman). Pastikan {@code initCriteria} konsisten antara
	 * pemanggilan untuk paginasi dan untuk ekspor.
	 *
	 * @param event event ZK pemicu pencarian, dapat null jika dipanggil programatik
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PenyediaAssetPunyaDokumen> penyediaAssetPunyaDokumen = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(penyediaAssetPunyaDokumen);
		grid.setRowRenderer(new PenyediaAssetPunyaDokumenRenderer());
		grid.setModelCheckMobile(strset);

	}

}

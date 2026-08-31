package ais.action.master;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;

import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.model.Konfigurasi;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Sekolah;
import ais.ui.util.MyButtonTabbox;
import ais.ui.util.MyLabelStyled;

/**
 * Controller/action ZK untuk konfigurasi sekolah. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * KonfigurasiNewAction}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Div mbtArea}, {@code MyButtonTabbox
 * mbt}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code buatPanelRows()});
 * pembacaan/pencarian ({@code onTampil()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see KonfigurasiNewAction
 */
public class KonfigurasiSekolahAction extends KonfigurasiNewAction {

	private static final long serialVersionUID = -5779730267402400328L;

	/** Autowired dari ZUL: <div id="mbtArea"> */
	private Div mbtArea;

	private MyButtonTabbox mbt;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterComposeOri(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		mbt = MyButtonTabbox.buat(mbtArea, "100%", null);
		onTampil();
	}

	/** Buat panel tab + grid + rows. Menggantikan createSpan() lama. */
	private Rows buatPanelRows(int index, String label) {
		jadwalkanPencarianKonfigurasi();

		boolean aktif = Common
				.getKonfigurasi("aktifkan_konfigurasi_" + label.replaceAll(" ", "_").toLowerCase(),
						Konfigurasi.AKTIF)
				.getNilai().trim().equalsIgnoreCase(Konfigurasi.AKTIF);

		Div panel = mbt.tambahTab(index, label);
		panel.setVisible(aktif);

		Div scrollWrap = new Div();
		scrollWrap.setWidth("100%");
		scrollWrap.setStyle("min-height:10000px; overflow:visible; box-sizing:border-box;");
		scrollWrap.setParent(panel);

		Grid grid = new Grid();
		grid.setSclass("fgrid");
		grid.setWidth("100%");
		grid.setParent(scrollWrap);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow titleRow = new MyFormRow();
		titleRow.setValign("top");
		titleRow.setParent(rows);
		titleRow.appendChild(new MyLabelStyled(Common.getBahasaConfig(label)));

		return rows;
	}

	public void onTampil() {

		Rows rows = buatPanelRows(1, "Konfigurasi Sekolah");

		rows.appendChild(createRowActiveDefault("Apakah modul sekolah / pesanren diaktifkan ?",
				"apakah_aktifkan_modul_sekolah", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Apakah modul perguruan tinggi diaktifkan ?",
				"apakah_aktifkan_modul_perguruan_tinggi", Konfigurasi.AKTIF));

		rows.appendChild(createRowNilai("Label Instansi / Yayasan", "label_instansi_sekolah", "Instansi / Yayasan"));
		rows.appendChild(createRowNilai("Label Alamat Instansi / Yayasan", "alamat_instansi_sekolah",
				"Alamat Instansi / Yayasan"));
		rows.appendChild(createRowNilai("Telp. Instansi / Yayasan", "label_telp_instansi_sekolah", "Telp. "));

		rows.appendChild(
				createRowActive("Siswa boleh mengganti foto profile sendiri", "siswa_boleh_mengubah_foto_profile"));
		rows.appendChild(
				createRowActive("Guru boleh mengganti foto profile sendiri", "guru_boleh_mengubah_foto_profile"));

		// ---- Tab 2: PSB ----
		rows = buatPanelRows(2, "PSB");

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		Groupbox groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Alur Pendaftaran Penerimaan Siswa Baru (PDF)"));
		Hbox hbox = new Hbox();
		Sekolah sekolah = SekolahUtil.getSekolah();
		LampiranLain.createDownloadUploadFileLain(hbox,
				sekolah == null ? LampiranLain.ID_ALUR_REGISTRASI_PSB : sekolah.getId(),
				LampiranLain.ALUR_REGISTRASI_PSB, "Alur", true, null);
		hbox.setParent(groupbox);

		rows.appendChild(createRowNilai("Apa saja info pertanyaan yang ditampilkan ?", "info_dari_mana_ppdb",
				"Website,Teman,Radio,Koran,Lain-lain"));

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption(LampiranLain.BACKGROUND_DEPAN_PESANTREN_STR));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.BACKGROUND_DEPAN_PESANTREN,
				LampiranLain.BACKGROUND_DEPAN_PESANTREN_STR, LampiranLain.BACKGROUND_DEPAN_PESANTREN_STR, false,
				new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.checkLogoUpload();
					}
				});
		hbox.setParent(groupbox);

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption(LampiranLain.LOGO_DEPAN_PESANTREN_STR));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.LOGO_DEPAN_PESANTREN,
				LampiranLain.LOGO_DEPAN_PESANTREN_STR, LampiranLain.LOGO_DEPAN_PESANTREN_STR, false,
				new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.checkLogoUpload();
					}
				});
		hbox.setParent(groupbox);

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption(LampiranLain.LOGO_DEPAN_PSB_STR));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.LOGO_DEPAN_PSB, LampiranLain.LOGO_DEPAN_PSB_STR,
				LampiranLain.LOGO_DEPAN_PSB_STR, false, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.checkLogoUpload();
					}
				});
		hbox.setParent(groupbox);

		row = new MyFormRow();
		groupbox = new Groupbox();
		groupbox.setParent(row);
		row.setParent(rows);
		groupbox.appendChild(new Caption(LampiranLain.BANNER_DEPAN_PSB_STR));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.BANNER_DEPAN_PSB,
				LampiranLain.BANNER_DEPAN_PSB_STR, LampiranLain.BANNER_DEPAN_PSB_STR, false, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.checkLogoUpload();
					}
				});
		hbox.setParent(groupbox);

		rows.appendChild(createRowNilai("Nomor Whatsapp yang bisa dihubungi", "no_whatsapp_operator", ""));

		rows.appendChild(
				createRowNilai("Tanya Whatsapp", "tanya_whatsapp_psb", "Salamat Datang, apa yang bisa kami bantu?"));

		rows.appendChild(createRowNilai("Jawab Whatsapp", "jawab_whatsapp_psb",
				"Saya ingin menanyakan tentang informasi penerimaan siswa baru, apakah Anda bisa membantu?"));

		rows.appendChild(
				createRowActive("Tampilkan Tulisan Teks penerimaan siswa baru di banner", "tampilkan_psb_di_banner"));

		String defaultValue = "Kegiatan seleksi penerimaan siswa baru merupakan kegiatan yang bertujuan mendapatkan calon siswa yang berkualitas dan memiliki kompetensi dasar yang baik sesuai dengan standar yang ditetapkan. Kegiatan ini merupaka kegiatan rutin bagi "
				+ ais.common.Common.getKonfigurasi("label_universitas", "").getNilai()
				+ ", karena itu penyelenggaraannya harus profesional, terjamin, terukur dan efesien.";

		rows.appendChild(createRowNilai("Informasi yang muncul di banner penerimaan siswa baru", "info_banner_psb",
				defaultValue, 5, null));

		rows.appendChild(createRowNilai("Tinggi banner penerimaan siswa baru", "tinggi_banner_psb", ""));

		rows.appendChild(
				createRowNilai("Tinggi halaman utama penerimaan siswa baru", "tinggi_halaman_utama_psb", "850"));

		rows.appendChild(createRowNilai("Informasi header", "label_psb_sekolah",
				"Penerimaan Peserta Didik Baru (PPDB) Tahun Pelajaran 2022-2023", 5, null));

		rows.appendChild(createRowNilai("Informasi yang muncul di kelulusan siswa baru", "informasi_kelulusan_sekolah",
				"NIS Anda [nis], nis ini bisa Anda gunakan untuk login ke http://ecampus dengan username NIS password NIS.",
				5, null));
		rows.appendChild(createRowNilai("Informasi tambahan yang muncul di kelulusan siswa baru",
				"informasi_kelulusan_tambahan_sekolah",
				"Jika Anda belum melakukan pembayaran, silahkan lakukan pembayaran di ....(tanya ke akademik);Kode pembayaran dapat dilihat di ....(tanya ke akademik)",
				5, null));

		// ---- Tab 3: Kartu Siswa ----
		rows = buatPanelRows(3, "Kartu Siswa");

		defaultValue = "1. Kartu ini ditertibkan oleh ....... Segala penggunaan kartu oleh ....... sesuai ketentuan dan syarat yang berlaku.\n"
				+ "2. Kartu ini harus dibawa sebagai identitas siswa.\n"
				+ "3. Kartu ini hanya berlaku bagi pemilik dan tidak untuk orang lain.\n"
				+ "4. Siswa harus mematuhi semua tata tertib .......\n"
				+ "5. Bila menemukan kartu ini mohon mengembalikan ke .......\n" + "\n\n\n" + " .......\n"
				+ "website : " + Common.getRequestHostWithProtocol();

		rows.appendChild(createRowNilai("Tata Tertib Kartu Siswa", "tata_tertib_kartu_siswa", defaultValue, 15, null));

		row = new MyFormRow();
		groupbox = new Groupbox();
		groupbox.setParent(row);
		row.setParent(rows);
		groupbox.appendChild(new Caption("Tanda Tangan Untuk Kartu Siswa (PNG)"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.TANDA_TANGAN_KARTU_SISWA_PERPUSTAKAAN,
				LampiranLain.TTD_KARTU_SISWA_PERPUSTAKAAN_STR, "Tanda Tangan", false, null);
		hbox.setParent(groupbox);

		row = new MyFormRow();
		groupbox = new Groupbox();
		groupbox.setParent(row);
		row.setParent(rows);
		groupbox.appendChild(new Caption("Stempel Untuk Kartu Siswa (PNG)"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.STEMPEL_KARTU_SISWA_PERPUSTAKAAN,
				LampiranLain.STEMPEL_KARTU_SISWA_PERPUSTAKAAN_STR, "Stempel", false, null);
		hbox.setParent(groupbox);

		rows.appendChild(createRowNilai("Label Jabatan Kartu Siswa", "label_jabatan_kartu_siswa", "Rektor"));
		rows.appendChild(createRowNilai("Label TTD Kartu Siswa", "label_ttd_kartu_siswa", "...................."));

		rows.appendChild(createRowNilai("NIP Kartu Siswa", "nip_ttd_kartu_siswa", "...................."));

		row = new MyFormRow();
		groupbox = new Groupbox();
		groupbox.setParent(row);
		row.setParent(rows);
		groupbox.appendChild(new Caption("Background Depan kartu Siswa"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.BG_1_KARTU_SISWA_PERPUSTAKAAN,
				LampiranLain.BG_1_KARTU_SISWA_PERPUSTAKAAN_STR, LampiranLain.BG_1_KARTU_SISWA_PERPUSTAKAAN_STR, false,
				null);
		hbox.setParent(groupbox);

		row = new MyFormRow();
		groupbox = new Groupbox();
		groupbox.setParent(row);
		row.setParent(rows);
		groupbox.appendChild(new Caption("Background Belakang kartu Siswa"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.BG_2_KARTU_SISWA_PERPUSTAKAAN,
				LampiranLain.BG_2_KARTU_SISWA_PERPUSTAKAAN_STR, LampiranLain.BG_2_KARTU_SISWA_PERPUSTAKAAN_STR, false,
				null);
		hbox.setParent(groupbox);

		rows.appendChild(createRowNilai("Masa berlaku kartu siswa", "masa_berlaku_kartu_siswa", "4"));

		rows.appendChild(createRowActiveDefault("Tamilkan CR Code di belakang kartu", "apakah_tampilan_cr_code",
				Konfigurasi.AKTIF));
	}
}

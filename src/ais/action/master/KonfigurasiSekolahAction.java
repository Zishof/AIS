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
import ais.action.master.konfigurasi.SkemaKonfigurasi;
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

		rows.appendChild(baris(SkemaKonfigurasi.SEKOLAH, "apakah_aktifkan_modul_sekolah"));

		rows.appendChild(baris(SkemaKonfigurasi.SEKOLAH, "apakah_aktifkan_modul_perguruan_tinggi"));

		rows.appendChild(baris(SkemaKonfigurasi.SEKOLAH, "label_instansi_sekolah"));
		rows.appendChild(baris(SkemaKonfigurasi.SEKOLAH, "alamat_instansi_sekolah"));
		rows.appendChild(baris(SkemaKonfigurasi.SEKOLAH, "label_telp_instansi_sekolah"));

		rows.appendChild(
				baris(SkemaKonfigurasi.SEKOLAH, "siswa_boleh_mengubah_foto_profile"));
		rows.appendChild(
				baris(SkemaKonfigurasi.SEKOLAH, "guru_boleh_mengubah_foto_profile"));

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

		rows.appendChild(baris(SkemaKonfigurasi.SEKOLAH, "info_dari_mana_ppdb"));

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

		rows.appendChild(baris(SkemaKonfigurasi.SEKOLAH, "no_whatsapp_operator"));

		rows.appendChild(
				baris(SkemaKonfigurasi.SEKOLAH, "tanya_whatsapp_psb"));

		rows.appendChild(baris(SkemaKonfigurasi.SEKOLAH, "jawab_whatsapp_psb"));

		rows.appendChild(
				baris(SkemaKonfigurasi.SEKOLAH, "tampilkan_psb_di_banner"));

		rows.appendChild(baris(SkemaKonfigurasi.SEKOLAH, "info_banner_psb"));

		rows.appendChild(baris(SkemaKonfigurasi.SEKOLAH, "tinggi_banner_psb"));

		rows.appendChild(
				baris(SkemaKonfigurasi.SEKOLAH, "tinggi_halaman_utama_psb"));

		rows.appendChild(baris(SkemaKonfigurasi.SEKOLAH, "label_psb_sekolah"));

		rows.appendChild(baris(SkemaKonfigurasi.SEKOLAH, "informasi_kelulusan_sekolah"));
		rows.appendChild(baris(SkemaKonfigurasi.SEKOLAH, "informasi_kelulusan_tambahan_sekolah"));

		// ---- Tab 3: Kartu Siswa ----
		rows = buatPanelRows(3, "Kartu Siswa");

		rows.appendChild(baris(SkemaKonfigurasi.SEKOLAH, "tata_tertib_kartu_siswa"));

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

		rows.appendChild(baris(SkemaKonfigurasi.SEKOLAH, "label_jabatan_kartu_siswa"));
		rows.appendChild(baris(SkemaKonfigurasi.SEKOLAH, "label_ttd_kartu_siswa"));

		rows.appendChild(baris(SkemaKonfigurasi.SEKOLAH, "nip_ttd_kartu_siswa"));

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

		rows.appendChild(baris(SkemaKonfigurasi.SEKOLAH, "masa_berlaku_kartu_siswa"));

		rows.appendChild(baris(SkemaKonfigurasi.SEKOLAH, "apakah_tampilan_cr_code"));
	}

	/**
	 * Bangun satu baris konfigurasi dari skema bersama.
	 *
	 * <p>Label dan nilai bawaannya TIDAK ditulis di sini melainkan dibaca dari
	 * {@link SkemaKonfigurasi}, karena {@code Common.getKonfigurasi} menyimpan
	 * bawaan yang disebut pemanggil ketika barisnya belum ada — bila layar ini
	 * dan kontrak native menyebut bawaan berbeda, yang dibuka lebih dulu akan
	 * menetapkannya secara permanen.</p>
	 */
	private Row baris(java.util.List<SkemaKonfigurasi.Butir> skema, String kunci) {
		SkemaKonfigurasi.Butir b = SkemaKonfigurasi.cari(skema, kunci);
		if (b == null) {
			throw new IllegalStateException("Kunci konfigurasi tidak ada di skema: " + kunci);
		}
		if (SkemaKonfigurasi.SAKLAR.equals(b.tipe)) {
			return createRowActiveDefault(b.label, b.kunci, b.bawaan());
		}
		if (SkemaKonfigurasi.TEKS_PANJANG.equals(b.tipe)) {
			return createRowNilai(b.label, b.kunci, b.bawaan(), b.baris, null);
		}
		return createRowNilai(b.label, b.kunci, b.bawaan());
	}
}

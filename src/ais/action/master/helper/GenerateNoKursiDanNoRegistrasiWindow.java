package ais.action.master.helper;

import java.util.Map;

import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Image;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.PendaftaranWisuda;
import ais.database.model.Tbmuser;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyPanel;
import ais.ui.util.MyWindow;

/**
 * Window ZK (dialog) modul wisuda untuk men-generate NOMOR REGISTRASI WISUDA dan NOMOR KURSI
 * WISUDA sekaligus dalam satu layar, lalu mencetak kartu/label nomor kursi hasil generate.
 * Satu-satunya varian di paket ini yang menggabungkan kedua nomor dalam satu window — bandingkan
 * dengan {@link GenerateNoKursiWindow} (hanya nomor kursi) dan {@link LaporanRegistrasiWisudaWindow}
 * (hanya nomor registrasi).
 *
 * <p><b>Alur data:</b> mahasiswa dipilih lewat {@link AmbilDataMahasiswaBanbox} (atau otomatis
 * dari user login bila constructor tanpa argumen dipakai dan user tsb adalah mahasiswa). Untuk
 * mahasiswa terpilih, window meng-query satu baris {@link PendaftaranWisuda} via
 * {@code Restrictions.eq("mahasiswa", mahasiswa)} dengan {@code setMaxResults(1)} (asumsi implisit
 * satu mahasiswa hanya mendaftar wisuda sekali, tanpa order by eksplisit bila ternyata ada lebih
 * dari satu baris). Form menampilkan NIM/Nama/Fakultas/Prodi (readonly, dari {@link Mahasiswa})
 * beserta foto profil ({@link ais.common.CommonMedia#getUrlFotoPengguna}), serta status lima tahap
 * persetujuan (Administrasi, Administrasi Fakultas, Keuangan, Perpustakaan, Perpustakaan Fakultas —
 * masing-masing field {@code int} 0/1 pada {@link PendaftaranWisuda}) dan status persetujuan wisuda
 * keseluruhan ({@code pendaftaranWisuda.getPersetujuanWisuda()}, field {@code Boolean}).</p>
 *
 * <p><b>Kuirk penting — "generate nomor" bukan penomoran berurutan:</b> baik No. Registrasi
 * maupun No. Kursi TIDAK dihasilkan dari counter/sequence terpisah; keduanya sama-sama diambil
 * dari {@code pendaftaranWisuda.getId().toString()} (primary key baris {@link PendaftaranWisuda}
 * itu sendiri) yang di-pad nol di depan sampai 8 digit, di {@code onGenerateLaporanRegistrasiWisuda()}
 * maupun {@code onGenerateNoKursiWisuda()}. Akibatnya, untuk satu mahasiswa, No. Registrasi Wisuda
 * dan No. Kursi Wisuda akan berupa STRING YANG SAMA PERSIS (sama-sama id di-pad nol) — ini perilaku
 * kode saat ini, dicatat apa adanya; kemungkinan besar bukan maksud bisnisnya (dua nomor dengan
 * tujuan berbeda memakai formula identik), tapi TIDAK diubah di sini karena tugas ini hanya
 * memperkaya Javadoc, bukan mengubah logika.</p>
 *
 * <p><b>Tombol toolbar:</b> "Generate No Reg" ({@code onGenerateLaporanRegistrasiWisuda}) aktif
 * hanya bila No. Registrasi belum ada; "Generate No Kursi" ({@code onGenerateNoKursiWisuda}) aktif
 * hanya bila No. Kursi belum ada DAN No. Registrasi SUDAH ada (No. Registrasi jadi prasyarat
 * urutan pengisian No. Kursi — dicek lewat ekspresi boolean gabungan yang agak sulit dibaca, lihat
 * komentar di {@code init()}); "Cetak" ({@code onCetakLaporanNoKursiWisuda}) aktif hanya bila kedua
 * nomor sudah terisi. Ketiga tombol dipaksa nonaktif total bila {@code persetujuanWisuda} belum
 * disetujui, terlepas dari status field individual di atas.</p>
 *
 * <p><b>Efek samping:</b> kedua method generate memanggil {@link Common#refreshSaveOrUpdate(Object)}
 * untuk menyimpan perubahan field {@link PendaftaranWisuda} ke database — berbeda dari
 * {@link GenerateNoKursiWindow} dan {@link LaporanRegistrasiWisudaWindow} yang memanggil
 * {@code PendaftaranWisudaDao} langsung; ada beberapa cara berbeda menyimpan entity yang sama
 * tersebar di window-window sejenis paket ini. Method cetak memanggil
 * {@link Report#generatePDFReport} dengan format PDF tetap (tidak ada pilihan format seperti window
 * lain yang punya {@code Combobox reportType}) dan nama file dasar {@code "Kursi_Wisuda"}. Validasi
 * lima status persetujuan pada {@code onGenerateLaporanRegistrasiWisuda()}/
 * {@code onGenerateNoKursiWisuda()} diduplikasi persis (copy-paste) di window-window sejenis lain
 * di paket ini — tidak ada helper validasi bersama; perubahan aturan bisnis harus disinkronkan
 * manual ke semua window bila diperlukan.</p>
 *
 * @see MyWindow
 * @see GenerateNoKursiWindow
 * @see LaporanRegistrasiWisudaWindow
 * @see GenerateUndanganWisudaWindow
 */
public class GenerateNoKursiDanNoRegistrasiWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6770886576623664442L;
	private Textbox nim;
	private Textbox nama;
	private Textbox fakultas;
	private Textbox jurusan;
	private Textbox noKursiWisuda;

	private Mahasiswa mahasiswa;
	private PendaftaranWisuda pendaftaranWisuda;
	private Toolbar toolbar;

	private MyButtonConfig generateNoreg;
	private MyButtonConfig generate;
	private MyButtonConfig cetak;
	// private MyButtonConfig batal;

	private AmbilDataMahasiswaBanbox bandboxMahasiswa;
	private Textbox noRegistrasiWisuda;
	private Image foto;

	/**
	 * Membuka window untuk mahasiswa yang sedang login, diambil dari
	 * {@link Common#getCurrentUser()}.{@code getMahasiswa()}. Dipakai saat mahasiswa mengakses
	 * menu ini sendiri dari portal mahasiswa. Exception saat inisialisasi ditelan dan hanya
	 * ditampilkan bila user yang login adalah admin (lihat
	 * {@link Common#tampilErrorJikaAdmin(Exception)}) — bagi mahasiswa biasa, kegagalan diam-diam
	 * meninggalkan window kosong tanpa pesan.
	 */
	public GenerateNoKursiDanNoRegistrasiWindow() {
		super();
		try {
			Tbmuser tbmuser = Common.getCurrentUser();

			init(tbmuser.getMahasiswa());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Membuka window untuk {@code mahasiswa} tertentu yang sudah diketahui pemanggil (mis. dipilih
	 * dari layar admin/petugas registrasi wisuda), tanpa lewat pemilihan Bandbox.
	 *
	 * @param mahasiswa mahasiswa target generate nomor registrasi/kursi wisuda; disimpan ke field
	 *        {@link #mahasiswa} sebelum {@code init} dipanggil
	 */
	public GenerateNoKursiDanNoRegistrasiWindow(Mahasiswa mahasiswa) {
		super();
		this.mahasiswa = mahasiswa;
		try {
			init(mahasiswa);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Membangun ulang seluruh isi window (form data mahasiswa, status persetujuan wisuda, dan
	 * toolbar tombol generate/cetak) untuk {@code mahasiswa} yang diberikan. Dipanggil dari kedua
	 * constructor maupun dari listener Bandbox mahasiswa setiap kali pilihan mahasiswa berubah
	 * (re-entrant: memanggil dirinya sendiri secara rekursif via {@code init()} lagi).
	 *
	 * <p>Langkah: (1) {@link Common#clear(org.zkoss.zk.ui.Component)} membuang seluruh child lama
	 * window; (2) membangun ulang layout Borderlayout (foto di West, form+toolbar di Center/South);
	 * (3) memuat {@link PendaftaranWisuda} milik mahasiswa (query {@code setMaxResults(1)}); bila
	 * tidak ditemukan, tampilkan pesan peringatan dan window berhenti dibangun (form kosong); (4)
	 * mengisi textbox readonly NIM/Nama/Fakultas/Prodi/No. Registrasi/No. Kursi serta label status
	 * lima tahap persetujuan; (5) menghitung status enable/disable tiga tombol toolbar berdasarkan
	 * apakah No. Registrasi/No. Kursi sudah terisi dan apakah {@code persetujuanWisuda} sudah
	 * disetujui.</p>
	 *
	 * @param mahasiswa mahasiswa yang datanya wisuda-nya ditampilkan; boleh {@code null} secara
	 *        teknis tapi lanjutannya akan melempar {@link NullPointerException} lebih dulu lewat
	 *        {@code Common.getCurrentUser()...getMahasiswa()} atau field lain, tergantung jalur
	 *        pemanggilan
	 * @throws Exception diteruskan dari operasi Hibernate/ZK; ditangkap oleh pemanggil (constructor
	 *         atau listener Bandbox) dan hanya ditampilkan ke admin
	 */
	private void init(final Mahasiswa mahasiswa) throws Exception {

		Common.clear(this);

		this.mahasiswa = mahasiswa;
		setClosable(true);
		setTitle("Generate Nomor Registrasi dan Nomor Kursi Wisuda");
		// setWidth("500px");
		// setHeight("250px");
		setPosition("center");

		MyPanel panel = new MyPanel();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Generate Nomor Registrasi dan Nomor Kursi Wisuda");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);

		West west = new West();
		west.setStyle("border:0px;");
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("250px");
		west.setParent(borderlayout);

		Vbox vbox = new Vbox();
		vbox.setPack("center");
		vbox.setAlign("center");
		vbox.setHeight("100%");
		vbox.setWidth("100%");
		vbox.setParent(west);
		vbox.appendChild(foto = new Image("/img/administrator-icon_default.png"));
		// foto.setHeight("300px");
		foto.setWidth("250px");
		if (mahasiswa != null && mahasiswa.getId() != null)
			foto.setSrc(CommonMedia.getUrlFotoPengguna(new Tbmuser(mahasiswa), 300, 250));

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("30%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("70%");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setVisible(Common.getCurrentUser() != null && Common.getCurrentUser().getMahasiswa() == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_mahasiswa")));
		row.appendChild(bandboxMahasiswa = new AmbilDataMahasiswaBanbox());
		bandboxMahasiswa.setWidth("90%");

		if (Common.getCurrentUser() != null && Common.getCurrentUser().getMahasiswa() != null
				|| this.mahasiswa != null) {
			bandboxMahasiswa.setAttribute("mahasiswa", mahasiswa);
			bandboxMahasiswa.setAttribute("myValue", mahasiswa);
			bandboxMahasiswa.setValue(mahasiswa.getNim() + " - " + mahasiswa.getNama());
			bandboxMahasiswa.setId("mhs_" + mahasiswa.getId());
			// bandboxMahasiswa.setDisabled(true);
		}
		bandboxMahasiswa.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				init((Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa"));
			}
		});

		if (bandboxMahasiswa.getAttribute("mahasiswa") == null || bandboxMahasiswa.getValue().trim().equals("")) {
			return;
		}

		pendaftaranWisuda = (PendaftaranWisuda) HibernateUtil.currentSession().createCriteria(PendaftaranWisuda.class)
				.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1).uniqueResult();

		if (pendaftaranWisuda == null) {
			MyMessageboxConfig.show(
					"Mahasiswa ini belum bisa mendapatkan nomor registrasi wisuda, karena belum mendaftar wisuda, segera hubungi admin",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM"));
		row.appendChild(nim = new Textbox(mahasiswa.getNim() == null ? "" : mahasiswa.getNim()));
		nim.setWidth("90%");
		nim.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox(mahasiswa.getNama() == null ? "" : mahasiswa.getNama()));
		nama.setWidth("90%");
		nama.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas = new Textbox(mahasiswa.getJurusan().getFakultas().getNama() == null ? ""
				: mahasiswa.getJurusan().getFakultas().getNama()));
		fakultas.setWidth("90%");
		fakultas.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan = new Textbox(
				mahasiswa.getJurusan().getNama() == null ? "" : mahasiswa.getJurusan().getNama()));
		jurusan.setWidth("90%");
		jurusan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Registrasi"));
		row.appendChild(noRegistrasiWisuda = new Textbox(
				pendaftaranWisuda.getNoRegistrasiWisuda() == null ? "" : pendaftaranWisuda.getNoRegistrasiWisuda()));
		noRegistrasiWisuda.setWidth("90%");
		noRegistrasiWisuda.setReadonly(true);
		noRegistrasiWisuda.setStyle("background-color: rgba(169,169,169,0.4);font-weight: bold;font-size: large;font-style: italic;");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Kursi"));
		row.appendChild(noKursiWisuda = new Textbox(
				pendaftaranWisuda.getNoKursi() == null ? "" : pendaftaranWisuda.getNoKursi()));
		noKursiWisuda.setWidth("90%");
		noKursiWisuda.setReadonly(true);
		noKursiWisuda.setStyle("background-color: rgba(169,169,169,0.4);font-weight: bold;font-size: large;font-style: italic;");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Pendaftaran Wisuda"));
		row.appendChild(new ais.ui.util.MyLabelConfig(pendaftaranWisuda.getTanggalDaftarWisuda() == null
				? "Belum Terdaftar Wisuda" : "Sudah Terdaftar Wisuda"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Bagian Keuangan"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				pendaftaranWisuda.getStatusPersetujuanKeuangan() == 0 ? "Belum Menyetujui" : "Sudah Menyetujui"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Bagian Administrasi"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				pendaftaranWisuda.getStatusPersetujuanAdministrasi() == 0 ? "Belum Menyetujui" : "Sudah Menyetujui"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Bagian Administrasi " + "Fakultas"));
		row.appendChild(new ais.ui.util.MyLabelConfig(pendaftaranWisuda.getStatusPersetujuanAdministrasiFakultas() == 0
				? "Belum Menyetujui" : "Sudah Menyetujui"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Bagian Perpustakaan"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				pendaftaranWisuda.getStatusPersetujuanPerpustakaan() == 0 ? "Belum Menyetujui" : "Sudah Menyetujui"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Bagian Perpustakaan " + "Fakultas"));
		row.appendChild(new ais.ui.util.MyLabelConfig(pendaftaranWisuda.getStatusPersetujuanPerpustakaanFakultas() == 0
				? "Belum Menyetujui" : "Sudah Menyetujui"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Persetujuan Wisuda"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				pendaftaranWisuda.getPersetujuanWisuda() == null || !pendaftaranWisuda.getPersetujuanWisuda()
						? "Belum Menyetujui" : "Sudah Menyetujui"));

		// row = new MyFormRow();
		//		// row.setParent(rows);
		// row.appendChild(new ais.ui.util.MyLabelConfig("Format Laporan"));
		// row.appendChild(reportType = CommonReport.generateReportType());
		// reportType.setWidth("90%");

		// row = new MyFormRow();
		//		// row.setParent(rows);
		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		generateNoreg = new MyButtonConfig("Generate No Reg");
		generate = new MyButtonConfig("Generate No Kursi");
		cetak = new MyButtonConfig("Cetak");
		// batal = new MyButtonConfig("Batal");

		if (pendaftaranWisuda != null && pendaftaranWisuda.getNoKursi() != null
				&& !pendaftaranWisuda.getNoKursi().trim().equals("")
				|| (pendaftaranWisuda == null || pendaftaranWisuda.getNoRegistrasiWisuda() == null
						|| pendaftaranWisuda.getNoRegistrasiWisuda().trim().equals(""))) {
			generate.setDisabled(true);
		}

		if (pendaftaranWisuda != null && pendaftaranWisuda.getNoRegistrasiWisuda() != null
				&& !pendaftaranWisuda.getNoRegistrasiWisuda().trim().equals("")) {
			generateNoreg.setDisabled(true);
		}

		if ((pendaftaranWisuda == null || pendaftaranWisuda.getNoKursi() == null
				|| pendaftaranWisuda.getNoKursi().trim().equals(""))
				|| (pendaftaranWisuda == null || pendaftaranWisuda.getNoRegistrasiWisuda() == null
						|| pendaftaranWisuda.getNoRegistrasiWisuda().trim().equals(""))) {
			cetak.setDisabled(true);
		} else {
			cetak.setDisabled(false);
		}

		generateNoreg.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onGenerateLaporanRegistrasiWisuda(event);
			}
		});
		generateNoreg.setParent(toolbar);

		generate.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onGenerateNoKursiWisuda(event);
			}
		});
		generate.setParent(toolbar);
		cetak.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onCetakLaporanNoKursiWisuda(event);
			}
		});
		cetak.setParent(toolbar);

		if (pendaftaranWisuda.getPersetujuanWisuda() == null || !pendaftaranWisuda.getPersetujuanWisuda()) {
			generateNoreg.setDisabled(true);
			generate.setDisabled(true);
			cetak.setDisabled(true);
		}

	}

	/**
	 * Handler tombol "Generate No Reg". Memvalidasi bahwa kelima status persetujuan
	 * (Administrasi, Administrasi Fakultas, Keuangan, Perpustakaan, Perpustakaan Fakultas) pada
	 * {@link #pendaftaranWisuda} sudah bernilai 1 (disetujui); bila ada yang belum, tampilkan
	 * pesan peringatan berisi daftar bagian yang belum menyetujui dan batalkan proses. Bila lolos,
	 * hasilkan No. Registrasi Wisuda = {@code pendaftaranWisuda.getId()} yang di-pad nol jadi 8
	 * digit (lihat kuirk generate nomor di Javadoc kelas), set ke textbox dan ke
	 * {@link PendaftaranWisuda#setNoRegistrasiWisuda(String)}, simpan lewat
	 * {@link Common#refreshSaveOrUpdate(Object)}, lalu nonaktifkan tombol ini dan aktifkan tombol
	 * "Generate No Kursi".
	 *
	 * @param event event {@code onClick} dari tombol "Generate No Reg" (tidak dipakai isinya)
	 * @throws Exception diteruskan dari operasi Hibernate/ZK
	 */
	@SuppressWarnings({})
	public void onGenerateLaporanRegistrasiWisuda(Event event) throws Exception {

		String info = "";
		if (pendaftaranWisuda.getStatusPersetujuanAdministrasi() == 0) {
			info += "\n Belum mendapat persetujuan dari Administrasi";
		}
		if (pendaftaranWisuda.getStatusPersetujuanAdministrasiFakultas() == 0) {
			info += "\n Belum mendapat persetujuan dari Administrasi " + "Fakultas";
		}
		if (pendaftaranWisuda.getStatusPersetujuanKeuangan() == 0) {
			info += "\n Belum mendapat persetujuan dari Keuangan";
		}
		if (pendaftaranWisuda.getStatusPersetujuanPerpustakaan() == 0) {
			info += "\n Belum mendapat persetujuan dari Perpustakaan";
		}
		if (pendaftaranWisuda.getStatusPersetujuanPerpustakaanFakultas() == 0) {
			info += "\n Belum mendapat persetujuan dari Perpustakaan " + "Fakultas";
		}

		if (!info.equals("")) {
			MyMessageboxConfig.show("Mahasiswa ini tidak dapat generate no registrasi wisuda karena," + info,
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		} else {
			String noRegistrasi = pendaftaranWisuda.getId().toString();

			while (noRegistrasi.length() < 8) {
				noRegistrasi = "0" + noRegistrasi;
			}

			noRegistrasiWisuda.setValue(noRegistrasi);
			pendaftaranWisuda.setNoRegistrasiWisuda(noRegistrasi);

			Common.refreshSaveOrUpdate(pendaftaranWisuda);

			generateNoreg.setDisabled(true);
			generate.setDisabled(false);
			cetak.setDisabled(true);

			MyMessageboxConfig.show("Berhasil generate No. Registrasi Wisuda", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
		}

	}

	/**
	 * Handler tombol "Generate No Kursi". Sama seperti {@link #onGenerateLaporanRegistrasiWisuda},
	 * memvalidasi kelima status persetujuan plus memastikan No. Registrasi sudah pernah di-generate
	 * (No. Registrasi kosong ditolak dengan pesan "Belum mendapatkan nomor registrasi wisuda") —
	 * jadi urutan kerja yang benar wajib generate No. Reg dahulu. Bila lolos, hasilkan No. Kursi
	 * Wisuda dari {@code pendaftaranWisuda.getId()} yang di-pad nol jadi 8 digit — dengan formula
	 * IDENTIK dengan No. Registrasi (lihat kuirk di Javadoc kelas), set ke textbox dan ke
	 * {@link PendaftaranWisuda#setNoKursi(String)}, simpan lewat
	 * {@link Common#refreshSaveOrUpdate(Object)}, lalu nonaktifkan tombol ini dan aktifkan tombol
	 * "Cetak".
	 *
	 * @param event event {@code onClick} dari tombol "Generate No Kursi" (tidak dipakai isinya)
	 * @throws Exception diteruskan dari operasi Hibernate/ZK
	 */
	@SuppressWarnings({})
	public void onGenerateNoKursiWisuda(Event event) throws Exception {

		String info = "";
		if (pendaftaranWisuda == null) {
			info += "\n Belum Daftar";
		}
		if (noRegistrasiWisuda.getValue().trim().equals("")) {
			info += "\n Belum mendapatkan nomor registrasi wisuda";
		}
		if (pendaftaranWisuda.getStatusPersetujuanAdministrasi() == 0) {
			info += "\n Belum mendapat persetujuan dari Administrasi";
		}
		if (pendaftaranWisuda.getStatusPersetujuanAdministrasiFakultas() == 0) {
			info += "\n Belum mendapat persetujuan dari Administrasi " + "Fakultas";
		}
		if (pendaftaranWisuda.getStatusPersetujuanKeuangan() == 0) {
			info += "\n Belum mendapat persetujuan dari Keuangan";
		}
		if (pendaftaranWisuda.getStatusPersetujuanPerpustakaan() == 0) {
			info += "\n Belum mendapat persetujuan dari Perpustakaan";
		}
		if (pendaftaranWisuda.getStatusPersetujuanPerpustakaanFakultas() == 0) {
			info += "\n Belum mendapat persetujuan dari Perpustakaan " + "Fakultas";
		}

		if (!info.equals("")) {
			MyMessageboxConfig.show("Mahasiswa ini tidak dapat generate no kursi wisuda karena:\n" + info, "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
		} else {
			String noKursi = pendaftaranWisuda.getId().toString();

			while (noKursi.length() < 8) {
				noKursi = "0" + noKursi;
			}

			noKursiWisuda.setValue(noKursi);
			pendaftaranWisuda.setNoKursi(noKursi);

			Common.refreshSaveOrUpdate(pendaftaranWisuda);

			generate.setDisabled(true);
			cetak.setDisabled(false);

			MyMessageboxConfig.show("Berhasil generate No. Kursi Wisuda", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
		}

	}

	/**
	 * Handler tombol "Cetak". Mencetak laporan/kartu nomor kursi wisuda dalam format PDF tetap
	 * (bukan pilihan pengguna — tidak ada {@code Combobox reportType} di window ini, berbeda dari
	 * {@link GenerateNoKursiWindow}), memakai template report dengan basis nama {@code "Kursi_Wisuda"}
	 * dan parameter {@code mahasiswa} = id mahasiswa terpilih. Tidak melakukan validasi ulang di
	 * method ini (validasi kelengkapan nomor sudah dilakukan lewat status disabled tombol di
	 * {@code init()}).
	 *
	 * @param event event {@code onClick} dari tombol "Cetak" (tidak dipakai isinya)
	 * @throws Exception diteruskan dari {@link Report#generatePDFReport}
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetakLaporanNoKursiWisuda(Event event) throws Exception {
		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("mahasiswa", mahasiswa.getId());

		Report.generatePDFReport(Report.PDF, parameters, "Kursi_Wisuda", ais.ui.util.WaktuUtil.getDate());

	}

}

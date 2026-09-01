package ais.action.master.helper;

import java.util.Map;

import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PendaftaranWisuda;
import ais.database.model.Tbmuser;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * Window ZK (dialog) modul wisuda untuk MENCETAK UNDANGAN WISUDA seorang mahasiswa dalam format
 * PDF/lain, setelah No. Registrasi dan No. Kursi Wisuda-nya sudah ada. Berbeda dari tiga window
 * sejenis lain di paket ini ({@link GenerateNoKursiDanNoRegistrasiWindow},
 * {@link GenerateNoKursiWindow}, {@link LaporanRegistrasiWisudaWindow}), kelas ini TIDAK
 * men-generate nomor apa pun — perannya murni cetak, dan justru menolak mencetak bila nomor
 * belum lengkap.
 *
 * <p><b>Alur data:</b> mahasiswa dipilih lewat {@link AmbilDataMahasiswaBanbox} atau otomatis
 * dari user login. {@link PendaftaranWisuda} dimuat via
 * {@code Restrictions.eq("mahasiswa", mahasiswa)} dengan {@code setMaxResults(1)}; window juga
 * memuat {@link BiodataMahasiswa} milik mahasiswa yang sama (entity tambahan yang tidak dipakai
 * oleh tiga window sejenis lain) karena nama ayah kandung dibutuhkan sebagai salah satu isi
 * undangan cetak. Bila {@code pendaftaranWisuda} tidak ditemukan, window menampilkan peringatan
 * dan berhenti dibangun; bila {@code mahasiswa} sendiri {@code null}, {@code init()} berhenti
 * lebih awal tanpa pesan (tidak seperti window sejenis lain yang selalu lanjut sampai query
 * PendaftaranWisuda).</p>
 *
 * <p><b>Validasi sebelum cetak</b> ({@code onCetakUndanganWisuda()}) — bukan lewat status
 * enable/disable tombol seperti window lain, melainkan pengecekan eksplisit di awal method dengan
 * {@code return} dini dan pesan peringatan berbeda untuk tiap kondisi: (1) {@code biodataMahasiswa}
 * kosong atau nama ayah belum diisi → arahkan ke menu Biodata Mahasiswa; (2) No. Registrasi Wisuda
 * belum ada → arahkan untuk registrasi wisuda; (3) No. Kursi belum ada → arahkan untuk generate
 * nomor kursi (lihat {@link GenerateNoKursiWindow}/{@link GenerateNoKursiDanNoRegistrasiWindow}).
 * Tombol "Cetak" sendiri hanya dinonaktifkan berdasarkan No. Kursi kosong di {@code init()}; dua
 * validasi lain (biodata, No. Registrasi) baru dicek saat tombol benar-benar diklik.</p>
 *
 * <p><b>Efek samping:</b> mencetak lewat {@link Report#generatePDFReport} dengan basis nama
 * {@code "Undangan_Wisuda"}, parameter {@code mahasiswa} (id) dan {@code nama_ayah} dari
 * {@link BiodataMahasiswa#getNamaAyah()}; format mengikuti pilihan {@code Combobox reportType}
 * dengan fallback {@link Report#PDF}. Ada baris debug {@code System.out.println("nama ayah : " +
 * ...)} yang tersisa di kode produksi (tidak dihapus, hanya dicatat). Tombol "Batal" pada window
 * ini men-detach seluruh {@link Tabpanel} induk beserta tab-nya (menutup tab), bukan sekadar
 * menutup window seperti {@link GenerateNoKursiDanNoRegistrasiWindow}/{@link GenerateNoKursiWindow}
 * — sama seperti perilaku "Batal" pada {@link LaporanRegistrasiWisudaWindow}, mengindikasikan
 * kedua window ini biasa dibuka sebagai tab, bukan dialog lepas.</p>
 *
 * @see MyWindow
 * @see GenerateNoKursiDanNoRegistrasiWindow
 * @see GenerateNoKursiWindow
 * @see LaporanRegistrasiWisudaWindow
 */
public class GenerateUndanganWisudaWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6770886576623664442L;
	private Textbox nim;
	private Textbox nama;
	private Textbox fakultas;
	private Textbox jurusan;
	private Combobox reportType;
	// private Textbox noKursiWisuda;

	private Mahasiswa mahasiswa;
	private PendaftaranWisuda pendaftaranWisuda;
	private BiodataMahasiswa biodataMahasiswa;
	private Textbox noKursiWisuda;
	private Textbox noRegistrasiWisuda;
	private Toolbar toolbar;
	// private MyButtonConfig generate;
	private MyButtonConfig cetak;
	private MyButtonConfig batal;
	private AmbilDataMahasiswaBanbox bandboxMahasiswa;

	/**
	 * Membuka window untuk mahasiswa yang sedang login, diambil dari
	 * {@link Common#getCurrentUser()}.{@code getMahasiswa()}. Exception saat inisialisasi ditelan
	 * dan hanya ditampilkan bila user yang login adalah admin (lihat
	 * {@link Common#tampilErrorJikaAdmin(Exception)}).
	 */
	public GenerateUndanganWisudaWindow() {
		super();
		try {
			Tbmuser tbmuser = Common.getCurrentUser();

			init(tbmuser.getMahasiswa());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

	}

	/**
	 * Membangun ulang seluruh isi window (form data mahasiswa, No. Registrasi/No. Kursi saat ini,
	 * pilihan format laporan, dan toolbar) untuk {@code mahasiswa} yang diberikan. Dipanggil dari
	 * constructor dan dari listener Bandbox mahasiswa setiap kali pilihan mahasiswa berubah.
	 *
	 * <p>Langkah: (1) {@link Common#clear(org.zkoss.zk.ui.Component)} membuang child lama; (2)
	 * membangun ulang layout Borderlayout; (3) berhenti dini bila {@code mahasiswa == null}; (4)
	 * memuat {@link PendaftaranWisuda} milik mahasiswa (query {@code setMaxResults(1)}); bila tidak
	 * ditemukan, tampilkan peringatan dan window berhenti dibangun; (5) memuat
	 * {@link BiodataMahasiswa} milik mahasiswa yang sama (dipakai untuk nama ayah pada undangan);
	 * (6) mengisi textbox readonly NIM/Nama/Fakultas/Prodi/No. Registrasi/No. Kursi serta
	 * {@code Combobox reportType}; (7) menghitung status enable/disable tombol "Cetak" berdasarkan
	 * apakah No. Kursi sudah terisi.</p>
	 *
	 * @param mahasiswa mahasiswa yang undangan wisudanya akan dicetak; bila {@code null}, method
	 *        berhenti setelah membangun Bandbox pemilih mahasiswa tanpa mengisi sisa form
	 * @throws Exception diteruskan dari operasi Hibernate/ZK; ditangkap oleh pemanggil dan hanya
	 *         ditampilkan ke admin
	 */
	private void init(Mahasiswa mahasiswa) throws Exception {
		this.mahasiswa = mahasiswa;
		Common.clear(this);

		setClosable(true);
		setTitle("Undangan Wisuda");
		// setWidth("500px");
		// setHeight("260px");
		setPosition("center");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Cetak Undangan Wisuda");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();grid.setWidth("100%");
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
		row.setVisible(Common.getCurrentUser() != null
				&& Common.getCurrentUser().getMahasiswa() == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_mahasiswa")));
		row.appendChild(bandboxMahasiswa = new AmbilDataMahasiswaBanbox());
		bandboxMahasiswa.setWidth("90%");

		if (Common.getCurrentUser() != null
				&& Common.getCurrentUser().getMahasiswa() != null
				|| this.mahasiswa != null) {
			bandboxMahasiswa.setAttribute("mahasiswa", mahasiswa);
			bandboxMahasiswa.setAttribute("myValue", mahasiswa);
			bandboxMahasiswa.setValue(mahasiswa.getNim() + " - "
					+ mahasiswa.getNama());
			bandboxMahasiswa.setId("mhs_" + mahasiswa.getId());
			// bandboxMahasiswa.setDisabled(true);
		}
		bandboxMahasiswa.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				init((Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa"));

			}
		});

		if (mahasiswa == null) {
			return;
		}

		pendaftaranWisuda = (PendaftaranWisuda) HibernateUtil.currentSession()
				.createCriteria(PendaftaranWisuda.class)
				.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1)
				.uniqueResult();

		if (pendaftaranWisuda == null) {
			MyMessageboxConfig
					.show("Mahasiswa ini belum bisa mendapatkan nomor registrasi karena belum mendaftar wisuda, segera hubungi admin",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		biodataMahasiswa = (BiodataMahasiswa) HibernateUtil.currentSession()
				.createCriteria(BiodataMahasiswa.class)
				.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1)
				.uniqueResult();

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM"));
		row.appendChild(nim = new Textbox(mahasiswa.getNim() == null ? ""
				: mahasiswa.getNim()));
		nim.setWidth("90%");
		nim.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox(mahasiswa.getNama() == null ? ""
				: mahasiswa.getNama()));
		nama.setWidth("90%");
		nama.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas = new Textbox(mahasiswa.getJurusan()
				.getFakultas().getNama() == null ? "" : mahasiswa.getJurusan()
				.getFakultas().getNama()));
		fakultas.setWidth("90%");
		fakultas.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan = new Textbox(
				mahasiswa.getJurusan().getNama() == null ? "" : mahasiswa
						.getJurusan().getNama()));
		jurusan.setWidth("90%");
		jurusan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Registrasi Wisuda"));
		row.appendChild(noRegistrasiWisuda = new Textbox(pendaftaranWisuda
				.getNoRegistrasiWisuda() == null ? "" : pendaftaranWisuda
				.getNoRegistrasiWisuda()));
		noRegistrasiWisuda.setWidth("90%");
		noRegistrasiWisuda.setReadonly(true);
		noRegistrasiWisuda
				.setStyle("background-color: rgba(169,169,169,0.4);font-weight: bold;font-size: large;font-style: italic;");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Kursi"));
		row.appendChild(noKursiWisuda = new Textbox(pendaftaranWisuda
				.getNoKursi() == null ? "" : pendaftaranWisuda.getNoKursi()));
		noKursiWisuda.setWidth("90%");
		noKursiWisuda.setReadonly(true);
		noKursiWisuda
				.setStyle("background-color: rgba(169,169,169,0.4);font-weight: bold;font-size: large;font-style: italic;");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Format Laporan"));
		row.appendChild(reportType = CommonReport.generateReportType());
		reportType.setWidth("90%");

		// row = new MyFormRow();
		//		// row.setParent(rows);
		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		// generate = new MyButtonConfig("Generate");
		cetak = new MyButtonConfig("Cetak");
		batal = new MyButtonConfig("Batal");

		/*
		 * if (pendaftaranWisuda != null && pendaftaranWisuda.getNoKursi() !=
		 * null && !pendaftaranWisuda.getNoKursi().trim().equals("")) {
		 * generate.setDisabled(true); } else if (pendaftaranWisuda == null ||
		 * pendaftaranWisuda.getNoKursi() == null || pendaftaranWisuda.getId()
		 * == null) { generate.setDisabled(false); cetak.setDisabled(true); }
		 * else { generate.setDisabled(false); }
		 */

		if (pendaftaranWisuda.getNoKursi() == null) {
			cetak.setDisabled(true);
		} else {
			cetak.setDisabled(false);
		}

		cetak.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onCetakUndanganWisuda(event);
			}
		});
		cetak.setParent(toolbar);
		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				((Tabpanel) GenerateUndanganWisudaWindow.this.getParent())
						.getLinkedTab().detach();
				((Tabpanel) GenerateUndanganWisudaWindow.this.getParent())
						.detach();
			}
		});
		batal.setParent(toolbar);
	}

	/**
	 * Handler tombol "Cetak". Memvalidasi tiga prasyarat berurutan sebelum mencetak, masing-masing
	 * dengan {@code return} dini dan pesan peringatan berbeda: (1) {@link #biodataMahasiswa} harus
	 * ada dan {@link BiodataMahasiswa#getNamaAyah()} harus terisi; (2) {@link PendaftaranWisuda}
	 * harus sudah punya No. Registrasi Wisuda; (3) harus sudah punya No. Kursi. Bila lolos ketiganya,
	 * cetak undangan lewat {@link Report#generatePDFReport} dengan basis nama
	 * {@code "Undangan_Wisuda"}, parameter {@code mahasiswa} (id) dan {@code nama_ayah}, format dari
	 * {@link #reportType} atau fallback {@link Report#PDF}. Mencetak baris debug nama ayah ke
	 * {@code System.out} sebelum generate laporan (sisa debugging, tidak dihapus).
	 *
	 * @param event event {@code onClick} dari tombol "Cetak" (tidak dipakai isinya)
	 * @throws Exception diteruskan dari {@link Report#generatePDFReport}
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetakUndanganWisuda(Event event) throws Exception {

		if (biodataMahasiswa == null || biodataMahasiswa.getNamaAyah() == null) {
			MyMessageboxConfig
					.show("Nama ayah belum diisi, silahkan isi di menu biodata mahasiswa",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		if (pendaftaranWisuda.getNoRegistrasiWisuda() == null
				|| pendaftaranWisuda.getNoRegistrasiWisuda().trim().equals("")) {
			MyMessageboxConfig
					.show("Mahasiswa ini belum mendapatkan nomor registrasi wisuda, silahkan melakukan registrasi wisuda",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (pendaftaranWisuda.getNoKursi() == null
				|| pendaftaranWisuda.getNoKursi().trim().equals("")) {
			MyMessageboxConfig
					.show("Mahasiswa ini belum mendapatkan nomor kursi wisuda, silahkan generate nomor kursi wisuda",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("mahasiswa", mahasiswa.getId());
		parameters.put("nama_ayah", biodataMahasiswa.getNamaAyah() == null ? ""
				: biodataMahasiswa.getNamaAyah());
		System.out.println("nama ayah : " + biodataMahasiswa.getNamaAyah());

		Report.generatePDFReport(
				reportType == null || reportType.getSelectedItem() == null ? Report.PDF
						: reportType.getSelectedItem().getValue().toString(),
				parameters, "Undangan_Wisuda", ais.ui.util.WaktuUtil.getDate());

	}
}

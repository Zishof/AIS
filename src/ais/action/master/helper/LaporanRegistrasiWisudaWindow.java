package ais.action.master.helper;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
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
import ais.common.Common;
import ais.database.dao.DaoFactory;
import ais.database.dao.PendaftaranWisudaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.PendaftaranWisuda;
import ais.database.model.Tbmuser;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * Window ZK (dialog/tab) modul wisuda untuk men-generate NOMOR REGISTRASI WISUDA seorang mahasiswa
 * dan mencetak laporannya — nama kelasnya menyebut "laporan" tapi fungsi utamanya, seperti tiga
 * window sejenis lain di paket ini, tetap men-generate nomor lalu mencetak, BUKAN sekadar menyaji
 * laporan pasif. Padanan yang lebih sempit dari {@link GenerateNoKursiDanNoRegistrasiWindow} (yang
 * men-generate No. Registrasi DAN No. Kursi sekaligus); bandingkan juga dengan
 * {@link GenerateNoKursiWindow} (khusus No. Kursi) dan {@link GenerateUndanganWisudaWindow} (cetak
 * undangan, tanpa generate nomor).
 *
 * <p><b>Alur data:</b> mahasiswa hanya bisa diambil dari user yang sedang login (constructor tanpa
 * argumen) atau dipilih lewat {@link AmbilDataMahasiswaBanbox} — sama seperti
 * {@link GenerateNoKursiWindow}, kelas ini TIDAK punya constructor dengan parameter
 * {@link Mahasiswa} eksplisit seperti {@link GenerateNoKursiDanNoRegistrasiWindow}.
 * {@link PendaftaranWisuda} dimuat via {@code Restrictions.eq("mahasiswa", mahasiswa)} dengan
 * {@code setMaxResults(1)}; bila {@code mahasiswa == null}, {@code init()} berhenti dini sebelum
 * query dijalankan; bila {@code pendaftaranWisuda} tidak ditemukan, window menampilkan peringatan
 * dan berhenti dibangun.</p>
 *
 * <p><b>Kuirk generate nomor:</b> sama seperti window sejenis lain di paket ini, No. Registrasi
 * bukan hasil counter/sequence terpisah — nilainya adalah
 * {@code pendaftaranWisuda.getId().toString()} yang di-pad nol jadi 8 digit
 * ({@code onGenerateLaporanRegistrasiWisuda()}), formula yang PERSIS SAMA dengan No. Kursi di
 * {@link GenerateNoKursiWindow} dan {@link GenerateNoKursiDanNoRegistrasiWindow} — untuk mahasiswa
 * yang sama, kedua nomor akan bernilai string identik. Berbeda dari {@link GenerateNoKursiWindow},
 * di kelas ini nama method {@code onGenerateLaporanRegistrasiWisuda()} SESUAI dengan isinya (memang
 * men-generate No. Registrasi, bukan No. Kursi) — jadi bila ingin melihat contoh yang "benar" dari
 * pola penamaan yang sama, rujuk ke sini.</p>
 *
 * <p><b>Efek samping:</b> {@code onGenerateLaporanRegistrasiWisuda()} menyimpan lewat
 * {@code PendaftaranWisudaDao.getInstance()... .update(pendaftaranWisuda)} TANPA membungkus
 * {@code beginTransaction()}/{@code commitTransaction()} eksplisit (baris-baris itu dikomentari),
 * sama seperti {@link GenerateNoKursiWindow} — berbeda dari
 * {@link GenerateNoKursiDanNoRegistrasiWindow} yang memakai
 * {@link Common#refreshSaveOrUpdate(Object)}. Method cetak ({@code onCetakLaporanRegistrasiWisuda})
 * SELALU memakai format PDF hardcode (literal string {@code "pdf"}, bukan konstanta
 * {@link Report#PDF} atau {@code Combobox reportType} seperti window lain) dengan basis nama
 * {@code "Registrasi_Wisuda"}. Tombol "Batal" men-detach seluruh {@link Tabpanel} induk beserta
 * tab-nya (menutup tab) — sama seperti {@link GenerateUndanganWisudaWindow}, berbeda dari
 * {@link GenerateNoKursiDanNoRegistrasiWindow}/{@link GenerateNoKursiWindow} yang hanya menutup
 * window itu sendiri. Validasi lima status persetujuan wisuda diduplikasi persis dari window
 * sejenis lain di paket ini — tidak ada helper validasi bersama.</p>
 *
 * @see MyWindow
 * @see GenerateNoKursiDanNoRegistrasiWindow
 * @see GenerateNoKursiWindow
 * @see GenerateUndanganWisudaWindow
 */
public class LaporanRegistrasiWisudaWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5620991583788581962L;

	private Textbox nim;
	private Textbox nama;
	private Textbox fakultas;
	private Textbox jurusan;
	private Textbox noRegistrasiWisuda;

	private Mahasiswa mahasiswa;
	private PendaftaranWisuda pendaftaranWisuda;
	private Toolbar toolbar;
	private MyButtonConfig generate;
	private MyButtonConfig cetak;
	private MyButtonConfig batal;

	private AmbilDataMahasiswaBanbox bandboxMahasiswa;

	/**
	 * Membuka window untuk mahasiswa yang sedang login, diambil dari
	 * {@link Common#getCurrentUser()}.{@code getMahasiswa()}. Exception saat inisialisasi ditelan
	 * dan hanya ditampilkan bila user yang login adalah admin (lihat
	 * {@link Common#tampilErrorJikaAdmin(Exception)}).
	 */
	public LaporanRegistrasiWisudaWindow() {
		super();
		try {
			Tbmuser tbmuser = Common.getCurrentUser();

			init(tbmuser.getMahasiswa());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Membangun ulang seluruh isi window (form data mahasiswa, No. Registrasi saat ini, dan
	 * toolbar) untuk {@code mahasiswa} yang diberikan. Dipanggil dari constructor dan dari listener
	 * Bandbox mahasiswa setiap kali pilihan mahasiswa berubah.
	 *
	 * <p>Langkah: (1) {@link Common#clear(org.zkoss.zk.ui.Component)} membuang child lama; (2)
	 * membangun ulang layout Borderlayout; (3) berhenti dini bila {@code mahasiswa == null}; (4)
	 * memuat {@link PendaftaranWisuda} milik mahasiswa (query {@code setMaxResults(1)}); bila tidak
	 * ditemukan, tampilkan peringatan dan window berhenti dibangun; (5) mengisi textbox readonly
	 * NIM/Nama/Fakultas/Prodi/No. Registrasi; (6) menghitung status enable/disable tombol
	 * "Generate" dan "Cetak" berdasarkan apakah No. Registrasi sudah terisi.</p>
	 *
	 * @param mahasiswa mahasiswa yang No. Registrasi Wisuda-nya digenerate/ditampilkan; bila
	 *        {@code null}, method berhenti setelah membangun Bandbox pemilih mahasiswa tanpa
	 *        mengisi sisa form
	 * @throws Exception diteruskan dari operasi Hibernate/ZK; ditangkap oleh pemanggil dan hanya
	 *         ditampilkan ke admin
	 */
	private void init(Mahasiswa mahasiswa) throws Exception {
		this.mahasiswa = mahasiswa;
		Common.clear(this);

		setClosable(true);
		setTitle("No. Registrasi Wisuda");
		// setWidth("500px");
		// setHeight("250px");
		setPosition("center");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Generate No. Registrasi Wisuda");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("80%");

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

		if (mahasiswa == null) {
			return;
		}

		pendaftaranWisuda = (PendaftaranWisuda) HibernateUtil.currentSession().createCriteria(PendaftaranWisuda.class)
				.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1).uniqueResult();

		if (pendaftaranWisuda == null) {
			MyMessageboxConfig.show(
					"Mahasiswa ini belum bisa mendapatkan nomor registrasi karena belum mendaftar wisuda, segera hubungi admin",
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
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Registrasi Wisuda"));
		row.appendChild(noRegistrasiWisuda = new Textbox(
				pendaftaranWisuda.getNoRegistrasiWisuda() == null ? "" : pendaftaranWisuda.getNoRegistrasiWisuda()));
		noRegistrasiWisuda.setWidth("90%");
		noRegistrasiWisuda.setReadonly(true);
		noRegistrasiWisuda.setStyle("background-color: rgba(169,169,169,0.4);font-weight: bold;font-size: large;font-style: italic;");

		// row = new MyFormRow();
		//		// row.setParent(rows);
		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		generate = new MyButtonConfig("Generate");
		cetak = new MyButtonConfig("Cetak");
		batal = new MyButtonConfig("Batal");

		if (pendaftaranWisuda != null && pendaftaranWisuda.getNoRegistrasiWisuda() != null
				&& !pendaftaranWisuda.getNoRegistrasiWisuda().trim().equals("")) {
			generate.setDisabled(true);
		} else if (pendaftaranWisuda == null || pendaftaranWisuda.getNoRegistrasiWisuda() == null
				|| pendaftaranWisuda.getId() == null) {
			generate.setDisabled(false);
			cetak.setDisabled(true);
		} else {
			generate.setDisabled(false);
		}
		generate.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onGenerateLaporanRegistrasiWisuda(event);
			}
		});
		generate.setParent(toolbar);

		if (pendaftaranWisuda.getNoRegistrasiWisuda() == null) {
			cetak.setDisabled(true);
		} else {
			cetak.setDisabled(false);
		}
		cetak.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onCetakLaporanRegistrasiWisuda(event);
			}
		});
		cetak.setParent(toolbar);

		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				// LaporanRegistrasiWisudaWindow.this.detach();

				((Tabpanel) LaporanRegistrasiWisudaWindow.this.getParent()).getLinkedTab().detach();
				((Tabpanel) LaporanRegistrasiWisudaWindow.this.getParent()).detach();
			}
		});
		batal.setParent(toolbar);

	}

	/**
	 * Handler tombol "Generate". Memvalidasi bahwa kelima status persetujuan (Administrasi,
	 * Administrasi Fakultas, Keuangan, Perpustakaan, Perpustakaan Fakultas) pada
	 * {@link #pendaftaranWisuda} sudah bernilai 1; bila ada yang belum, tampilkan pesan peringatan
	 * berisi daftar bagian yang belum menyetujui dan batalkan proses. Bila lolos, hasilkan No.
	 * Registrasi Wisuda = {@code pendaftaranWisuda.getId()} di-pad nol jadi 8 digit (lihat kuirk
	 * generate nomor di Javadoc kelas — formula identik dengan No. Kursi di
	 * {@link GenerateNoKursiWindow}), set ke textbox dan ke
	 * {@link PendaftaranWisuda#setNoRegistrasiWisuda(String)}, lalu simpan lewat
	 * {@code PendaftaranWisudaDao.update(pendaftaranWisuda)} (transaksi eksplisit dikomentari,
	 * mengandalkan transaksi ambien). Menonaktifkan tombol "Generate" dan mengaktifkan "Cetak"
	 * setelah berhasil.
	 *
	 * @param event event {@code onClick} dari tombol "Generate" (tidak dipakai isinya)
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
			MyMessageboxConfig.show("Mahasiswa ini tidak dapat generate laporan registrasi wisuda karena," + info,
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		} else {
			String noRegistrasi = pendaftaranWisuda.getId().toString();

			while (noRegistrasi.length() < 8) {
				noRegistrasi = "0" + noRegistrasi;
			}

			noRegistrasiWisuda.setValue(noRegistrasi);
			pendaftaranWisuda.setNoRegistrasiWisuda(noRegistrasi);

			PendaftaranWisudaDao PendaftaranWisudaDao = DaoFactory.getInstance().getPendaftaranWisudaDao();
			// PendaftaranWisudaDao.beginTransaction();
			PendaftaranWisudaDao.update(pendaftaranWisuda);
			// PendaftaranWisudaDao.commitTransaction();

			generate.setDisabled(true);
			cetak.setDisabled(false);

			MyMessageboxConfig.show("Berhasil generate No. Registrasi Wisuda", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
		}

	}

	/**
	 * Handler tombol "Cetak". Mencetak laporan No. Registrasi Wisuda dengan format PDF hardcode
	 * (literal string {@code "pdf"} — tidak ada Combobox pilihan format di window ini, dan bukan
	 * memakai konstanta {@link Report#PDF}), memakai template report dengan basis nama
	 * {@code "Registrasi_Wisuda"} dan parameter {@code mahasiswa} = id mahasiswa terpilih. Tidak
	 * melakukan validasi ulang di method ini (validasi kelengkapan No. Registrasi sudah dilakukan
	 * lewat status disabled tombol di {@code init()}).
	 *
	 * @param event event {@code onClick} dari tombol "Cetak" (tidak dipakai isinya)
	 * @throws Exception diteruskan dari {@link Report#generatePDFReport}
	 */
	@SuppressWarnings({})
	public void onCetakLaporanRegistrasiWisuda(Event event) throws Exception {
		Map<String, Serializable> parameters = ais.common.HashMapGenerator.getRandStringSerializable();
		parameters.put("mahasiswa", mahasiswa.getId());

		Report.generatePDFReport("pdf", parameters, "Registrasi_Wisuda", ais.ui.util.WaktuUtil.getDate());

	}
}

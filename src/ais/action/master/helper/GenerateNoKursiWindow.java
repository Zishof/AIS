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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
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
 * Window ZK (dialog) modul wisuda untuk men-generate NOMOR KURSI WISUDA seorang mahasiswa (setelah
 * No. Registrasi Wisuda-nya sudah ada) dan mencetak laporannya. Padanan yang lebih sempit dari
 * {@link GenerateNoKursiDanNoRegistrasiWindow} (yang men-generate kedua nomor sekaligus); bandingkan
 * juga dengan {@link LaporanRegistrasiWisudaWindow} (khusus No. Registrasi) dan
 * {@link GenerateUndanganWisudaWindow} (cetak undangan, tidak men-generate nomor apa pun).
 *
 * <p><b>Kuirk penamaan method:</b> handler tombol "Generate" di kelas ini bernama
 * {@code onGenerateLaporanRegistrasiWisuda()} — namanya menyiratkan "generate laporan registrasi",
 * tapi isinya justru men-generate DAN MENYIMPAN No. Kursi Wisuda (bukan No. Registrasi). Ini
 * kemungkinan besar sisa copy-paste dari {@link LaporanRegistrasiWisudaWindow}/
 * {@link GenerateNoKursiDanNoRegistrasiWindow} yang namanya tidak diperbarui; nama method
 * dipertahankan apa adanya (bukan bug baru untuk didokumentasikan sebagai fitur) sesuai tugas ini
 * yang hanya memperkaya Javadoc, tidak mengubah kode.</p>
 *
 * <p><b>Alur data:</b> mahasiswa hanya bisa diambil dari user yang sedang login (constructor
 * tanpa argumen) atau dipilih lewat {@link AmbilDataMahasiswaBanbox} — kelas ini TIDAK punya
 * constructor dengan parameter {@link Mahasiswa} eksplisit seperti
 * {@link GenerateNoKursiDanNoRegistrasiWindow}. {@link PendaftaranWisuda} dimuat via
 * {@code Restrictions.eq("mahasiswa", mahasiswa)} dengan {@code setMaxResults(1)}; bila tidak
 * ditemukan, window menampilkan peringatan dan berhenti dibangun.</p>
 *
 * <p><b>Kuirk generate nomor:</b> sama seperti window sejenis lain di paket ini, No. Kursi bukan
 * hasil counter/sequence terpisah — nilainya adalah {@code pendaftaranWisuda.getId().toString()}
 * yang di-pad nol jadi 8 digit, formula yang PERSIS SAMA dengan No. Registrasi Wisuda di
 * {@link LaporanRegistrasiWisudaWindow} dan {@link GenerateNoKursiDanNoRegistrasiWindow} — untuk
 * mahasiswa yang sama, kedua nomor akan bernilai string identik. Ada juga blok kode mati (dikomentari,
 * di dalam {@code onGenerateLaporanRegistrasiWisuda()}) yang menyimpan implementasi lama: nomor
 * kursi dihitung dari {@code Projections.rowCount()} atas baris {@code PendaftaranWisuda} yang
 * sudah disetujui penuh, dipad jadi 4 digit, dan disimpan lewat
 * {@code PendaftaranWisudaDao.beginTransaction()/commitTransaction()} eksplisit — pendekatan itu
 * sudah ditinggalkan demi skema id-8-digit yang dipakai sekarang, tapi kodenya belum dibersihkan.</p>
 *
 * <p><b>Efek samping:</b> {@code onGenerateLaporanRegistrasiWisuda()} menyimpan lewat
 * {@code PendaftaranWisudaDao.getInstance()... .update(pendaftaranWisuda)} TANPA membungkus
 * {@code beginTransaction()}/{@code commitTransaction()} eksplisit (baris-baris itu dikomentari) —
 * update berjalan mengandalkan transaksi ambien/konteks request, berbeda dari
 * {@link GenerateNoKursiDanNoRegistrasiWindow} yang memakai {@link Common#refreshSaveOrUpdate(Object)}.
 * Method cetak memakai format dari {@code Combobox reportType} (hasil
 * {@link ais.action.report.helper.CommonReport#generateReportType()}), fallback ke PDF bila belum
 * dipilih. Validasi lima status persetujuan wisuda diduplikasi persis dari window sejenis lain di
 * paket ini — tidak ada helper validasi bersama.</p>
 *
 * @see MyWindow
 * @see GenerateNoKursiDanNoRegistrasiWindow
 * @see LaporanRegistrasiWisudaWindow
 * @see GenerateUndanganWisudaWindow
 */
public class GenerateNoKursiWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6770886576623664442L;
	private Textbox nim;
	private Textbox nama;
	private Textbox fakultas;
	private Textbox jurusan;
	private Combobox reportType;
	private Textbox noKursiWisuda;

	private Mahasiswa mahasiswa;
	private PendaftaranWisuda pendaftaranWisuda;
	private Toolbar toolbar;
	private MyButtonConfig generate;
	private MyButtonConfig cetak;
	private MyButtonConfig batal;

	private AmbilDataMahasiswaBanbox bandboxMahasiswa;

	/**
	 * Membuka window untuk mahasiswa yang sedang login, diambil dari
	 * {@link Common#getCurrentUser()}.{@code getMahasiswa()}. Ini satu-satunya cara masuk ke window
	 * ini secara langsung dengan mahasiswa spesifik — pemilihan mahasiswa lain (mis. oleh admin)
	 * dilakukan lewat Bandbox {@link #bandboxMahasiswa} setelah window terbuka, bukan lewat
	 * constructor lain. Exception saat inisialisasi ditelan dan hanya ditampilkan bila user yang
	 * login adalah admin (lihat {@link Common#tampilErrorJikaAdmin(Exception)}).
	 */
	public GenerateNoKursiWindow() {
		super();
		try {
			Tbmuser tbmuser = Common.getCurrentUser();

			init(tbmuser.getMahasiswa());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Membangun ulang seluruh isi window (form data mahasiswa, No. Kursi saat ini, pilihan format
	 * laporan, dan toolbar) untuk {@code mahasiswa} yang diberikan. Dipanggil dari constructor dan
	 * dari listener Bandbox mahasiswa setiap kali pilihan mahasiswa berubah.
	 *
	 * <p>Langkah: (1) {@link Common#clear(org.zkoss.zk.ui.Component)} membuang child lama; (2)
	 * membangun ulang layout Borderlayout; (3) memuat {@link PendaftaranWisuda} milik mahasiswa
	 * (query {@code setMaxResults(1)}); bila tidak ditemukan, tampilkan peringatan dan window
	 * berhenti dibangun; (4) mengisi textbox readonly NIM/Nama/Fakultas/Prodi/No. Kursi serta
	 * {@code Combobox reportType} untuk format cetak; (5) menghitung status enable/disable tombol
	 * "Generate" dan "Cetak" berdasarkan apakah No. Kursi sudah terisi.</p>
	 *
	 * @param mahasiswa mahasiswa yang datanya wisuda-nya ditampilkan
	 * @throws Exception diteruskan dari operasi Hibernate/ZK; ditangkap oleh pemanggil dan hanya
	 *         ditampilkan ke admin
	 */
	private void init(Mahasiswa mahasiswa) throws Exception {

		Common.clear(this);

		this.mahasiswa = mahasiswa;
		setClosable(true);
		setTitle("Nomor Kursi Wisuda");
		// setWidth("500px");
		// setHeight("250px");
		setPosition("center");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Generate Nomor Kursi Wisuda");
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

		pendaftaranWisuda = (PendaftaranWisuda) HibernateUtil.currentSession().createCriteria(PendaftaranWisuda.class)
				.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1).uniqueResult();

		if (pendaftaranWisuda == null) {
			MyMessageboxConfig.show(
					"Mahasiswa ini belum bisa mendapatkan nomor kursi karena belum mendaftar wisuda, segera hubungi admin",
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
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Kursi"));
		row.appendChild(noKursiWisuda = new Textbox(
				pendaftaranWisuda.getNoKursi() == null ? "" : pendaftaranWisuda.getNoKursi()));
		noKursiWisuda.setWidth("90%");
		noKursiWisuda.setReadonly(true);
		noKursiWisuda.setStyle("background-color: rgba(169,169,169,0.4);font-weight: bold;font-size: large;font-style: italic;");

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
		generate = new MyButtonConfig("Generate");
		cetak = new MyButtonConfig("Cetak");
		batal = new MyButtonConfig("Batal");

		if (pendaftaranWisuda != null && pendaftaranWisuda.getNoKursi() != null
				&& !pendaftaranWisuda.getNoKursi().trim().equals("")) {
			generate.setDisabled(true);
		} else if (pendaftaranWisuda == null || pendaftaranWisuda.getNoKursi() == null
				|| pendaftaranWisuda.getId() == null) {
			generate.setDisabled(false);
			cetak.setDisabled(true);
		} else {
			generate.setDisabled(false);
		}

		if (pendaftaranWisuda.getNoKursi() == null) {
			cetak.setDisabled(true);
		} else {
			cetak.setDisabled(false);
		}

		// generate.setVisible(true);
		generate.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onGenerateLaporanRegistrasiWisuda(event);
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
		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				GenerateNoKursiWindow.this.detach();
			}
		});
		batal.setParent(toolbar);
	}

	/**
	 * Handler tombol "Generate". Meski namanya {@code onGenerateLaporanRegistrasiWisuda} (sisa
	 * copy-paste, lihat Javadoc kelas), method ini men-generate NO. KURSI, bukan No. Registrasi.
	 * Memvalidasi bahwa No. Registrasi Wisuda sudah ada dan kelima status persetujuan
	 * (Administrasi, Administrasi Fakultas, Keuangan, Perpustakaan, Perpustakaan Fakultas) sudah
	 * bernilai 1; bila ada yang belum, tampilkan pesan peringatan berisi daftar penyebab dan
	 * batalkan proses. Bila lolos, hasilkan No. Kursi = {@code pendaftaranWisuda.getId()} di-pad
	 * nol jadi 8 digit (lihat blok kode mati di Javadoc kelas untuk skema lama berbasis
	 * {@code rowCount()}), set ke textbox dan ke {@link PendaftaranWisuda#setNoKursi(String)}, lalu
	 * simpan lewat {@code PendaftaranWisudaDao.update(pendaftaranWisuda)} (transaksi eksplisit
	 * dikomentari, mengandalkan transaksi ambien). Menonaktifkan tombol "Generate" dan mengaktifkan
	 * "Cetak" setelah berhasil.
	 *
	 * @param event event {@code onClick} dari tombol "Generate" (tidak dipakai isinya)
	 * @throws Exception diteruskan dari operasi Hibernate/ZK
	 */
	@SuppressWarnings({})
	public void onGenerateLaporanRegistrasiWisuda(Event event) throws Exception {

		String info = "";
		if (pendaftaranWisuda == null) {
			info += "\n Belum Daftar";
		}
		if (pendaftaranWisuda.getNoRegistrasiWisuda() == null
				|| pendaftaranWisuda.getNoRegistrasiWisuda().trim().equals("")) {
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
			MyMessageboxConfig.show("Mahasiswa ini tidak dapat generate laporan registrasi wisuda karena:\n" + info,
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
		} /*
			 * else { Number noKursi = (Number) session
			 * .createCriteria(PendaftaranWisuda.class)
			 * .add(Restrictions.eq("statusPersetujuanKeuangan", 1))
			 * .add(Restrictions.eq("statusPersetujuanAdministrasi", 1))
			 * .add(Restrictions.eq("statusPersetujuanPerpustakaan", 1)) .add(
			 * Restrictions.eq( "statusPersetujuanPerpustakaan"+"Fakultas", 1)) .add(
			 * Restrictions.eq( "statusPersetujuanAdministrasi"+"Fakultas", 1))
			 * .add(Restrictions.eq("wisuda", wisuda)).setProjection(
			 * Projections.rowCount()).uniqueResult(); noKursi = noKursi == null ? 1 :
			 * noKursi; String temp = "00000" + noKursi; System.out.println("temp = " +
			 * temp); String myNo = temp.substring(temp.length() - 4, temp.length());
			 * 
			 * noKursiWisuda.setValue(myNo); pendaftaranWisuda.setNoRegistrasiWisuda(myNo);
			 * 
			 * PendaftaranWisudaDao PendaftaranWisudaDao = DaoFactory
			 * .getInstance().getPendaftaranWisudaDao();
			 * PendaftaranWisudaDao.beginTransaction();
			 * PendaftaranWisudaDao.update(pendaftaranWisuda);
			 * PendaftaranWisudaDao.commitTransaction();
			 * 
			 * generate.setDisabled(true); cetak.setDisabled(false);
			 * 
			 * MyMessageboxConfig.show("Berhasil generate Nomor Kursi Wisuda", "Peringatan",
			 * MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			 */
		else {
			String noKursi = pendaftaranWisuda.getId().toString();

			while (noKursi.length() < 8) {
				noKursi = "0" + noKursi;
			}

			noKursiWisuda.setValue(noKursi);
			pendaftaranWisuda.setNoKursi(noKursi);

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
	 * Handler tombol "Cetak". Mencetak laporan No. Kursi Wisuda dengan format dari
	 * {@link #reportType} bila sudah dipilih pengguna, atau fallback {@link Report#PDF} bila belum
	 * ada pilihan (Combobox {@code null} atau belum ada item terpilih); memakai template report
	 * dengan basis nama {@code "Kursi_Wisuda"} dan parameter {@code mahasiswa} = id mahasiswa
	 * terpilih. Tidak melakukan validasi ulang di method ini (validasi kelengkapan No. Kursi sudah
	 * dilakukan lewat status disabled tombol di {@code init()}).
	 *
	 * @param event event {@code onClick} dari tombol "Cetak" (tidak dipakai isinya)
	 * @throws Exception diteruskan dari {@link Report#generatePDFReport}
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetakLaporanNoKursiWisuda(Event event) throws Exception {
		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("mahasiswa", mahasiswa.getId());

		Report.generatePDFReport(
				reportType == null || reportType.getSelectedItem() == null ? Report.PDF
						: reportType.getSelectedItem().getValue().toString(),
				parameters, "Kursi_Wisuda", ais.ui.util.WaktuUtil.getDate());

	}

}

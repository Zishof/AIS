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
 * Tipe khusus untuk generate no kursi window. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Textbox nim}, {@code Textbox nama},
 * {@code Textbox fakultas}, {@code Textbox jurusan}, {@code Combobox reportType}, {@code Textbox noKursiWisuda},
 * {@code Mahasiswa mahasiswa}, {@code PendaftaranWisuda pendaftaranWisuda}; inisialisasi/lifecycle ({@code
 * init()}); pelaporan/ekspor ({@code onCetakLaporanNoKursiWisuda()}); operasi domain lain ({@code
 * onGenerateLaporanRegistrasiWisuda()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
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

	public GenerateNoKursiWindow() {
		super();
		try {
			Tbmuser tbmuser = Common.getCurrentUser();

			init(tbmuser.getMahasiswa());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

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

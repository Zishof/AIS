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

	public GenerateNoKursiDanNoRegistrasiWindow() {
		super();
		try {
			Tbmuser tbmuser = Common.getCurrentUser();

			init(tbmuser.getMahasiswa());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	public GenerateNoKursiDanNoRegistrasiWindow(Mahasiswa mahasiswa) {
		super();
		this.mahasiswa = mahasiswa;
		try {
			init(mahasiswa);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetakLaporanNoKursiWisuda(Event event) throws Exception {
		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("mahasiswa", mahasiswa.getId());

		Report.generatePDFReport(Report.PDF, parameters, "Kursi_Wisuda", ais.ui.util.WaktuUtil.getDate());

	}

}

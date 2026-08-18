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

	public LaporanRegistrasiWisudaWindow() {
		super();
		try {
			Tbmuser tbmuser = Common.getCurrentUser();

			init(tbmuser.getMahasiswa());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

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

	@SuppressWarnings({})
	public void onCetakLaporanRegistrasiWisuda(Event event) throws Exception {
		Map<String, Serializable> parameters = ais.common.HashMapGenerator.getRandStringSerializable();
		parameters.put("mahasiswa", mahasiswa.getId());

		Report.generatePDFReport("pdf", parameters, "Registrasi_Wisuda", ais.ui.util.WaktuUtil.getDate());

	}
}

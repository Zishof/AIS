package ais.action.report.format1.employ;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.employ.helper.AmbilDataJenisPelatihanBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.StatusPegawai;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

public class LaporanDaftarUrutKepangkatan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1550813616089440767L;

	private Combobox golongan;
	private Combobox statusPegawai;
	private Combobox kelamin;

	private Combobox pendidikan;
	private AmbilDataJenisPelatihanBanbox jenisPelatihan;
	private Combobox jenisTandaJasa;

	private Center center;

	private Toolbar toolbar;

	private Combobox statusPerkawinan;

	public LaporanDaftarUrutKepangkatan() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Daftar Urut Kepangkatan", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanDaftarUrutKepangkatan(String title, String border,
			boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private void init() throws Exception {

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onTranskrip(event);

			}
		};

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

		MyGrid grid = new MyGrid();grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("30%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		/*
		 * MyFormRow row = new MyFormRow();row.setValign("top");
		 *		 * row.setParent(rows); row.appendChild(new
		 * Label("Pangkat / Golongan")); row.appendChild(golongan = new
		 * Combobox()); golongan.setWidth("90%");
		 * golongan.addEventListener("onChange", eventListener);
		 * Common.insertCombo(golongan, "nama", Golongan.class, Restrictions.eq("aktif", true));
		 */

		MyFormRow row = new MyFormRow();row.setValign("top");
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Pegawai"));
		row.appendChild(statusPegawai = new Combobox());
		statusPegawai.setWidth("90%");
		statusPegawai.addEventListener("onChange", eventListener);
		Common.insertCombo(statusPegawai, "nama", StatusPegawai.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(statusPegawai, ConstantValues.AKTIF_PEGAWAI);
		statusPegawai.addEventListener("onChange", eventListener);

		/*
		 * kelamin = new Combobox(); org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		 * comboitem.setLabel("Laki-laki"); comboitem.setValue("Laki-laki");
		 * kelamin.appendChild(comboitem); comboitem = new MyComboitemConfig();
		 * comboitem.setLabel("Perempuan"); comboitem.setValue("Perempuan");
		 * kelamin.appendChild(comboitem); row = new MyFormRow();
		 *		 * row.setParent(rows); row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Kelamin"));
		 * row.appendChild(kelamin); kelamin.addEventListener("onChange",
		 * eventListener); kelamin.setWidth("90%");
		 * 
		 * statusPerkawinan = new Combobox(); comboitem = new MyComboitemConfig();
		 * comboitem.setLabel("Belum kawin"); comboitem.setValue("Belum kawin");
		 * statusPerkawinan.appendChild(comboitem); comboitem = new MyComboitemConfig();
		 * comboitem.setLabel("Kawin"); comboitem.setValue("Kawin");
		 * statusPerkawinan.appendChild(comboitem); comboitem = new MyComboitemConfig();
		 * comboitem.setLabel("Janda"); comboitem.setValue("Janda");
		 * statusPerkawinan.appendChild(comboitem); comboitem = new MyComboitemConfig();
		 * comboitem.setLabel("Duda"); comboitem.setValue("Duda");
		 * statusPerkawinan.appendChild(comboitem); row = new MyFormRow();
		 *		 * row.setParent(rows); row.appendChild(new ais.ui.util.MyLabelConfig("Status Perkawinan"));
		 * row.appendChild(statusPerkawinan);
		 * statusPerkawinan.addEventListener("onChange", eventListener);
		 * statusPerkawinan.setWidth("90%");
		 * 
		 * row = new MyFormRow();		 * row.setParent(rows); row.appendChild(new
		 * Label("Pendidikan Terakhir")); row.appendChild(pendidikan = new
		 * Combobox()); pendidikan.setWidth("90%");
		 * pendidikan.addEventListener("onChange", eventListener);
		 * Common.insertCombo(pendidikan, "nama", Pendidikan.class);
		 * 
		 * row = new MyFormRow();		 * row.setParent(rows); row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pelatihan"));
		 * row.appendChild(jenisPelatihan = new
		 * AmbilDataJenisPelatihanBanbox()); jenisPelatihan.setWidth("90%");
		 * jenisPelatihan.setEventListener(eventListener);
		 * 
		 * row = new MyFormRow();		 * row.setParent(rows); row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Tanda Jasa"));
		 * row.appendChild(jenisTandaJasa = new Combobox());
		 * jenisTandaJasa.setWidth("90%");
		 * jenisTandaJasa.addEventListener("onChange", eventListener);
		 * Common.insertCombo(jenisTandaJasa, "nama", JenisTandaJasa.class);
		 */

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(
				new ParameterListener() {

					@SuppressWarnings({ "unchecked", "rawtypes" })
					@Override
					public Map<String, Serializable> generateParameters()
							throws Exception {

						Map parameters = generateParameter();
						return parameters;
					}
				}, "employ/duk", null, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onTranskrip(arg0);
					}
				}));

		onTranskrip(null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		/*
		 * Golongan golongan = (Golongan) (this.golongan.getSelectedItem() ==
		 * null ? null : this.golongan.getSelectedItem().getValue());
		 * 
		 * Pendidikan pendidikan = (Pendidikan)
		 * (this.pendidikan.getSelectedItem() == null ? null :
		 * this.pendidikan.getSelectedItem().getValue());
		 */
		StatusPegawai statusPegawai = (StatusPegawai) (this.statusPegawai
				.getSelectedItem() == null ? null : this.statusPegawai
				.getSelectedItem().getValue());
		/*
		 * String kelamin = (String) (this.kelamin.getSelectedItem() == null ?
		 * "" : this.kelamin.getSelectedItem().getValue());
		 * 
		 * String statusPerkawinan = (String) (this.statusPerkawinan
		 * .getSelectedItem() == null ? "" : this.statusPerkawinan
		 * .getSelectedItem().getValue());
		 * 
		 * JenisTandaJasa jenisTandaJasa = (JenisTandaJasa) (this.jenisTandaJasa
		 * .getSelectedItem() == null ? null : this.jenisTandaJasa
		 * .getSelectedItem().getValue());
		 * 
		 * JenisPelatihan jenisPelatihan = (JenisPelatihan) (this.jenisPelatihan
		 * .getAttribute("jenisPelatihan"));
		 */
		final Map parameters = ais.common.HashMapGenerator.getRand();
		/*
		 * parameters.put("kelamin", kelamin);
		 * parameters.put("statusPerkawinan", statusPerkawinan);
		 * parameters.put("golongan", golongan == null ? -1L :
		 * golongan.getId()); parameters.put("pendidikan", pendidikan == null ?
		 * -1L : pendidikan.getId());
		 * 
		 * parameters.put("jenisPelatihan", jenisPelatihan == null ? -1L :
		 * jenisPelatihan.getId());
		 * 
		 * parameters.put("jenisTandaJasa", jenisTandaJasa == null ? -1L :
		 * jenisTandaJasa.getId());
		 */
		parameters.put("statusPegawai", statusPegawai == null || statusPegawai.getId() == null ? -1L : statusPegawai.getId());
		System.out.print("statusPegawai : " + statusPegawai);

		return parameters;
	}

	@SuppressWarnings({ "unchecked" })
	public void onTranskrip(Event event) throws Exception {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF,
					generateParameter(), "employ/duk", ais.ui.util.WaktuUtil.getDate(),
							toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Daftar Urut Kepangkatan", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}

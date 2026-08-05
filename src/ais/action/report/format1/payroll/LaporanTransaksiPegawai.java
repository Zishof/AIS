package ais.action.report.format1.payroll;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;

import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.LayoutRegion;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanTransaksiPegawai extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Combobox yayasan;
	private Combobox sekolah;
	private Textbox siswa;
	private MyDatebox mulai;
	private MyDatebox sampai;
	private Toolbar toolbar;

	private Tabbox tabbox;

	private Siswa selectedSiswa;

	private Center center;

	public LaporanTransaksiPegawai() throws Exception {
		super();
		init();
	}

	public LaporanTransaksiPegawai(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private void init() throws Exception {

		if (ExecutionsCtrl.getCurrent().getParameter("siswa") != null) {
			selectedSiswa = (Siswa) HibernateUtil.currentSession().createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah"))
					.add(Restrictions.idEq(Long.parseLong(ExecutionsCtrl.getCurrent().getParameter("siswa"))))
					.uniqueResult();
		}

		yayasan = new Combobox();
		sekolah = new Combobox();

		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		LayoutRegion west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("120px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setVisible(selectedSiswa == null);
		row.setParent(rows);
		Vbox vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		vbox.appendChild(yayasan);
		yayasan.setCols(5);

		row = new MyFormRow();
		row.setVisible(selectedSiswa == null);
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		vbox.appendChild(sekolah);
		sekolah.setCols(5);

		row = new MyFormRow();

		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Pegawai"));
		vbox.appendChild(siswa = new Textbox());
		siswa.setCols(5);

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.ambilPegawai() != null) {
			siswa.setValue(tbmuser.ambilPegawai().getNama());
			siswa.setDisabled(true);
		}

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Waktu"));
		Box hbox = new Vbox();
		vbox.appendChild(hbox);
		hbox.appendChild(mulai = new MyDatebox(calendar.getTime()));
		hbox.appendChild(sampai = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		mulai.setCols(5);
		sampai.setCols(5);
		mulai.setReadonly(true);
		sampai.setReadonly(true);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		final EventListener listener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				onCetak(null, center);
			}
		};

		row = new MyFormRow();
		row.setParent(rows);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", listener);
		print.setParent(row);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setVisible(selectedSiswa == null);
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "payroll/laporan_transaksi_pegawai", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Tabpanel tabpanel = tabbox.getSelectedPanel();
				Common.clear(tabpanel);
				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(tabpanel);

				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);
				onCetak(null, center);
			}
		}));

		listener.onEvent(null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		Map parameters = ais.common.HashMapGenerator.getRand();

		Sekolah mySekolah = selectedSiswa != null ? selectedSiswa.getSekolah()
				: (Sekolah) (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null ? null
						: sekolah.getSelectedItem().getValue());
		Yayasan myYayasan = selectedSiswa != null ? selectedSiswa.getYayasan()
				: (Yayasan) (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null ? null
						: yayasan.getSelectedItem().getValue());

		parameters.put("header",
				mySekolah == null
						? (myYayasan == null ? Common.ambilREAL_PATH_REPORT() + "/wood.jpg"
								: Common.getRequestHostWithProtocol() + "/AmbilLampiran?ref=" + (myYayasan.getId())
										+ "&jenis=KOP+Yayasan&usingId=false")
						: Common.getRequestHostWithProtocol() + "/AmbilLampiran?ref=" + (mySekolah.getId())
								+ "&jenis=KOP+Sekolah&usingId=false");

		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		if (mySekolah != null && mySekolah.getId() != null) {
			LampiranLain lampiranLain = LampiranLain.ambil(mySekolah.getId(), LampiranLain.KOP_SEKOLAH);
			if (lampiranLain != null) {
				parameters.put("header", lampiranLain.ambilFile().getAbsolutePath());
			} else {
				if (perguruanTinggi != null) {
					lampiranLain = LampiranLain.ambil(perguruanTinggi.getId(), LampiranLain.KOP_PT);
					if (lampiranLain != null) {
						parameters.put("header", lampiranLain.ambilFile().getAbsolutePath());
					}
					lampiranLain = LampiranLain.ambil(perguruanTinggi.getId(), LampiranLain.KOP_BAWAH_PT);
					if (lampiranLain != null) {
						parameters.put("footer", lampiranLain.ambilFile().getAbsolutePath());
					}
				}
			}
			
			lampiranLain = LampiranLain.ambil(mySekolah.getId(), LampiranLain.KOP_BAWAH_SEKOLAH);
			if (lampiranLain != null) {
				parameters.put("footer", lampiranLain.ambilFile().getAbsolutePath());
			}
			
		} else if (myYayasan != null && myYayasan.getId() != null) {
			LampiranLain lampiranLain = LampiranLain.ambil(myYayasan.getId(), LampiranLain.KOP_YAYASAN);
			if (lampiranLain != null) {
				parameters.put("header", lampiranLain.ambilFile().getAbsolutePath());
			} else {
				if (perguruanTinggi != null) {
					lampiranLain = LampiranLain.ambil(perguruanTinggi.getId(), LampiranLain.KOP_PT);
					if (lampiranLain != null) {
						parameters.put("header", lampiranLain.ambilFile().getAbsolutePath());
					}
					lampiranLain = LampiranLain.ambil(perguruanTinggi.getId(), LampiranLain.KOP_BAWAH_PT);
					if (lampiranLain != null) {
						parameters.put("footer", lampiranLain.ambilFile().getAbsolutePath());
					}
				}
			}
		} else {
			if (perguruanTinggi != null) {
				LampiranLain lampiranLain = LampiranLain.ambil(perguruanTinggi.getId(), LampiranLain.KOP_PT);
				if (lampiranLain != null) {
					parameters.put("header", lampiranLain.ambilFile().getAbsolutePath());
				}
				lampiranLain = LampiranLain.ambil(perguruanTinggi.getId(), LampiranLain.KOP_BAWAH_PT);
				if (lampiranLain != null) {
					parameters.put("footer", lampiranLain.ambilFile().getAbsolutePath());
				}
			}

		}

		parameters.put("label_mulai", Common.dateFormat4.get().format(mulai.getValue()));
		parameters.put("label_sampai", Common.dateFormat4.get().format(sampai.getValue()));

		parameters.put("siswa", selectedSiswa == null ? siswa.getValue().trim() : selectedSiswa.getNomorInduk());

		parameters.put("yayasan", myYayasan == null || myYayasan.getId() == null ? -1L : myYayasan.getId());
		parameters.put("sekolah", mySekolah == null || mySekolah.getId() == null ? -1L : mySekolah.getId());

		parameters.put("mulai", Common.databaseDateFormat.get().format(mulai.getValue()));
		parameters.put("sampai", Common.databaseDateFormat.get().format(sampai.getValue()));
		return parameters;
	}

	@SuppressWarnings({})
	public void onCetak(Event event, final Center center) {

		try {
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(),
							"payroll/laporan_transaksi_pegawai", ais.ui.util.WaktuUtil.getDate(),
							toolbar);
					CommonReport.tampilkanReportPDF(center, file);
				}
			});

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Transaksi Pegawai", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

}

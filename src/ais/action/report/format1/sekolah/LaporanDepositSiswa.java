package ais.action.report.format1.sekolah;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;

import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.LayoutRegion;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataAsramaBanbox;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.helper.AmbilDataKelasSiswaBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.AkunPembayaranSiswa;
import ais.database.model.sekolah.AsramaSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan deposit siswa. Kelas ini mengubah data domain menjadi
 * bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke
 * lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox yayasan}, {@code Combobox
 * sekolah}, {@code Textbox siswa}, {@code MyDatebox mulai}, {@code MyDatebox sampai}, {@code Toolbar toolbar},
 * {@code AmbilDataKelasSiswaBanbox kelas}, {@code AmbilDataAsramaBanbox asrama}; inisialisasi/lifecycle ({@code
 * init()}); pelaporan/ekspor ({@code onCetak()}); operasi domain lain ({@code generateParameter()}). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanDepositSiswa extends MyWindow {

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

	private AmbilDataKelasSiswaBanbox kelas;
	private AmbilDataAsramaBanbox asrama;

	private Combobox akunPembayaranSiswa;

	private Tabbox tabbox;

	private Siswa selectedSiswa;

	private Combobox angkatan;

	public LaporanDepositSiswa() throws Exception {
		super();
		init();
	}

	public LaporanDepositSiswa(String title, String border, boolean closable) throws Exception {
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

		LayoutRegion west = Common.isMobile() ? new North() : new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		if (Common.isMobile()) {
			west.setHeight("250px");
			west.setOpen(false);
		} else {
			west.setWidth("120px");
		}

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
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Siswa"));
		vbox.appendChild(siswa = new Textbox());
		siswa.setCols(5);

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getSiswa() != null) {
			siswa.setValue(tbmuser.getSiswa().getNomorInduk());
			siswa.setDisabled(true);
		}
		if (selectedSiswa != null) {
			siswa.setValue(selectedSiswa.getNomorInduk());
			siswa.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		vbox.appendChild(angkatan = new Combobox());
		angkatan.setReadonly(true);
		angkatan.setCols(5);

		Integer currTahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		Comboitem comboitem;
		for (int i = currTahun - 20; i < currTahun + 5; i++) {
			comboitem = new Comboitem(i + "");
			comboitem.setValue(i);
			angkatan.appendChild(comboitem);
		}

		comboitem = new Comboitem("Semua");
		comboitem.setValue(null);
		angkatan.appendChild(comboitem);
		angkatan.setSelectedItem(comboitem);

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Cara Pembayaran"));
		vbox.appendChild(akunPembayaranSiswa = new Combobox());
		akunPembayaranSiswa.setCols(5);
		akunPembayaranSiswa.setReadonly(true);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());

				Common.insertComboDanSemua(akunPembayaranSiswa, new String[] { "nama", "akun", "bank" }, "sekolah",
						AkunPembayaranSiswa.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
				Common.selectComboItem(akunPembayaranSiswa, null);

			}
		};

		sekolah.addEventListener("onChange", eventListener);
		Common.createDefaultTimer(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		vbox.appendChild(this.kelas = new AmbilDataKelasSiswaBanbox());
		kelas.setCols(5);

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Asrama"));
		vbox.appendChild(this.asrama = new AmbilDataAsramaBanbox());
		asrama.setCols(5);

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

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		tabbox = new Tabbox();
		tabbox.setParent(center);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab1 = new MyTabConfig("Deposit Siswa");
		tab1.setParent(tabs);

		MyTabConfig tab2 = new MyTabConfig("Deposit Per Siswa");
		tab2.setParent(tabs);

		MyTabConfig tab3 = new MyTabConfig("Deposit Per Tanggal");
		tab3.setParent(tabs);

		MyTabConfig tab4 = new MyTabConfig("Tabungan Siswa");
		tab4.setParent(tabs);

		MyTabConfig tab5 = new MyTabConfig("Tabungan Per Tanggal");
		tab5.setParent(tabs);

		MyTabConfig tab6 = new MyTabConfig("Rekap Per Tanggal");
		tab6.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setAttribute("report", "sekolah/pembayaran/deposit_siswa");
		tabpanel1.setParent(tabpanels);
		tabpanel1.setHeight("100%");

		tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setAttribute("report", "sekolah/pembayaran/deposit_per_siswa");
		tabpanel1.setParent(tabpanels);
		tabpanel1.setHeight("100%");

		tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setAttribute("report", "sekolah/pembayaran/deposit_per_tanggal");
		tabpanel1.setParent(tabpanels);
		tabpanel1.setHeight("100%");

		tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setAttribute("report", "sekolah/pembayaran/tabungan_siswa");
		tabpanel1.setParent(tabpanels);
		tabpanel1.setHeight("100%");

		tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setAttribute("report", "sekolah/pembayaran/tabungan_siswa_per_tanggal");
		tabpanel1.setParent(tabpanels);
		tabpanel1.setHeight("100%");

		tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setAttribute("report", "sekolah/pembayaran/rekap_tabungan_siswa_per_tanggal");
		tabpanel1.setParent(tabpanels);
		tabpanel1.setHeight("100%");

		final EventListener listener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Tabpanel tabpanel = tabbox.getSelectedPanel();
				Common.clear(tabpanel);
				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(tabpanel);
				borderlayout.setHeight("100%");

				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);
				onCetak(null, center);
			}
		};

		final EventListener listenerJika = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Tabpanel tabpanel = tabbox.getSelectedPanel();
				if (tabpanel.getChildren().isEmpty()) {
					listener.onEvent(null);
				}
			}
		};

		row = new MyFormRow();
		row.setParent(rows);
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", listener);
		print.setParent(row);

		tab1.addEventListener("onClick", listenerJika);
		tab2.addEventListener("onClick", listenerJika);
		tab3.addEventListener("onClick", listenerJika);
		tab4.addEventListener("onClick", listenerJika);
		tab5.addEventListener("onClick", listenerJika);
		tab6.addEventListener("onClick", listenerJika);

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
		}, "sekolah/pembayaran/deposit_siswa", null, new EventListener() {

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
		Tabpanel tabpanel = tabbox.getSelectedPanel();
		parameters.put("nama_laporan", tabpanel.getAttribute("report"));

		Sekolah mySekolah = selectedSiswa != null ? selectedSiswa.getSekolah()
				: (Sekolah) (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null ? null
						: sekolah.getSelectedItem().getValue());
		Yayasan myYayasan = selectedSiswa != null ? selectedSiswa.getYayasan()
				: (Yayasan) (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null ? null
						: yayasan.getSelectedItem().getValue());

		AkunPembayaranSiswa myAkunPembayaranSiswa = (AkunPembayaranSiswa) (akunPembayaranSiswa.getSelectedItem() == null
				|| akunPembayaranSiswa.getSelectedItem().getValue() == null ? null
						: akunPembayaranSiswa.getSelectedItem().getValue());

		KelasSiswa myKelasSiswa = (KelasSiswa) kelas.getAttribute("kelas");
		AsramaSiswa myAsramaSiswa = (AsramaSiswa) asrama.getAttribute("asrama");

		Integer angkatan = (Integer) (this.angkatan.getSelectedItem() == null
				|| this.angkatan.getSelectedItem().getValue() == null ? null
						: this.angkatan.getSelectedItem().getValue());

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

		parameters.put("siswa", siswa.getValue().trim());
		parameters.put("akunPembayaranSiswa", myAkunPembayaranSiswa == null || myAkunPembayaranSiswa.getId() == null ? -1L : myAkunPembayaranSiswa.getId());

		parameters.put("kelas", myKelasSiswa == null || myKelasSiswa.getId() == null ? -1L : myKelasSiswa.getId());
		parameters.put("asrama", myAsramaSiswa == null || myAsramaSiswa.getId() == null ? -1L : myAsramaSiswa.getId());

		parameters.put("yayasan", myYayasan == null || myYayasan.getId() == null ? -1L : myYayasan.getId());
		parameters.put("sekolah", mySekolah == null || mySekolah.getId() == null ? -1L : mySekolah.getId());
		parameters.put("angkatan", angkatan == null ? -1 : angkatan);
		parameters.put("mulai", Common.databaseDateFormat.get().format(mulai.getValue()));
		parameters.put("sampai", Common.databaseDateFormat.get().format(sampai.getValue()));

		Tbmuser tbmuser = Common.getCurrentUser();
		Collection<Long> anaks = tbmuser != null && tbmuser.getOrangTua() != null
				? tbmuser.getOrangTua().ambilAnakSiswa()
				: new ArrayList<Long>();
		if (anaks.isEmpty()) {
			anaks.add(-1L);
		}
		parameters.put("anaks", anaks.toArray());

		return parameters;
	}

	@SuppressWarnings({})
	public void onCetak(Event event, final Center center) {

		try {

			center.setAttribute("desktopHeight", 700);

			if (Common.isMobile()) {
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(),
						"sekolah/pembayaran/deposit_siswa", ais.ui.util.WaktuUtil.getDate(), toolbar);
				CommonReport.tampilkanReportPDF(center, file);
			} else {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(),
								"sekolah/pembayaran/deposit_siswa", ais.ui.util.WaktuUtil.getDate(), toolbar);
						CommonReport.tampilkanReportPDF(center, file);
					}
				});
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Deposit Siswa", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

}

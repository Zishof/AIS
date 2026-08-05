package ais.action.report.format1.sekolah;
import ais.common.PesanFormalHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.Transformers;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.LayoutRegion;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataAsramaBanbox;
import ais.action.master.sapto.util.SaptoUtil;
import ais.action.master.sekolah.helper.AmbilDataKelasSiswaBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.AkunPembayaranSiswa;
import ais.database.model.sekolah.AsramaSiswa;
import ais.database.model.sekolah.JenisBiayaSekolah;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanRincianPembayaranSiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Combobox yayasan;
	private Combobox sekolah;
	private Combobox jenisBiayaSekolah;
	private Combobox tahun;
	private Combobox bulan;
	private Textbox siswa;
	private MyDatebox mulai;
	private MyDatebox sampai;
	private AmbilDataKelasSiswaBanbox kelas;
	private AmbilDataAsramaBanbox asrama;
	private Siswa selectedSiswa = null;
	private Combobox akunPembayaranSiswa;

	private Row rowBulan;

	private Row rowTahun;

	private Tabbox tabbox;

	private Combobox angkatan;

	private Textbox namaAkunPembayaranSiswa;

	private Combobox tahunAjaran;

	private Label label;

	public LaporanRincianPembayaranSiswa() {
		super();
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rincian Pembayaran Siswa", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRincianPembayaranSiswa(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		init();
	}

	private void init() throws Exception {

		if (ExecutionsCtrl.getCurrent().getParameter("siswa") != null) {
			selectedSiswa = (Siswa) HibernateUtil.currentSession().createCriteria(Siswa.class)
					.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
					.add(Restrictions.isNotNull("sekolah"))
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

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
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
		tahunAjaran = Common.generateTahunAjaranDanSemua(tahunAjaran);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Tahun Pelajaran"));
		vbox.appendChild(tahunAjaran);
		tahunAjaran.setCols(5);

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Siswa/Calon Siswa"));
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
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran"));
		vbox.appendChild(jenisBiayaSekolah = new Combobox());
		jenisBiayaSekolah.setCols(5);
		jenisBiayaSekolah.setReadonly(true);

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

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Nama Cara Pembayaran"));
		vbox.appendChild(namaAkunPembayaranSiswa = new Textbox());
		namaAkunPembayaranSiswa.setCols(5);

		rowBulan = new MyFormRow();
		rowBulan.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(rowBulan);
		vbox.setWidth("100%");
		rowBulan.setStyle("border:0px;background: transparent;");
		vbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Bulan")));
		vbox.appendChild(bulan = new Combobox());
		bulan.setReadonly(true);
		bulan.setCols(5);

		rowTahun = new MyFormRow();
		rowTahun.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(rowTahun);
		vbox.setWidth("100%");
		rowTahun.setStyle("border:0px;background: transparent;");
		vbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Tahun")));
		vbox.appendChild(tahun = new Combobox());
		tahun.setReadonly(true);
		tahun.setCols(5);

		for (int i = 0; i < 12; i++) {
			comboitem = new Comboitem(Common.BULAN[i]);
			comboitem.setValue(i + 1);
			bulan.appendChild(comboitem);
		}

		comboitem = new Comboitem("Semua");
		comboitem.setValue(null);
		bulan.appendChild(comboitem);

		Common.selectComboItem(bulan, null);

		for (int i = currTahun - 10; i < currTahun + 10; i++) {
			comboitem = new Comboitem(i + "");
			comboitem.setValue(i);
			tahun.appendChild(comboitem);
		}

		comboitem = new Comboitem("Semua");
		comboitem.setValue(null);
		tahun.appendChild(comboitem);

		Common.selectComboItem(tahun, null);

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());

				Common.insertComboDanSemua(jenisBiayaSekolah, new String[] { "kode", "nama", "periode" }, "sekolah",
						JenisBiayaSekolah.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
				Common.selectComboItem(jenisBiayaSekolah, null);

				Common.insertComboDanSemua(akunPembayaranSiswa, new String[] { "nama", "akun", "bank" }, "sekolah",
						AkunPembayaranSiswa.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
				Common.selectComboItem(akunPembayaranSiswa, null);

			}
		};

		Common.createDefaultTimer(eventListener);

		row = new MyFormRow();
		row.setVisible(selectedSiswa == null);
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		vbox.appendChild(this.kelas = new AmbilDataKelasSiswaBanbox());
		kelas.setCols(5);

		row = new MyFormRow();
		row.setVisible(selectedSiswa == null);
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Asrama"));
		vbox.appendChild(this.asrama = new AmbilDataAsramaBanbox());
		asrama.setCols(5);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 3);

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

		final MyTabConfig tab1 = new MyTabConfig("Per Item Biaya");
		tab1.setParent(tabs);

		final MyTabConfig tab11 = new MyTabConfig("Per Virtual Account");
		tab11.setParent(tabs);

		final MyTabConfig tab12 = new MyTabConfig("Per Item Detail");
		tab12.setParent(tabs);

		final MyTabConfig tab122 = new MyTabConfig("Per Item Rinci");
		tab122.setParent(tabs);

		final MyTabConfig tab13 = new MyTabConfig("Tagihan Item Detail");
		tab13.setParent(tabs);

		final MyTabConfig tab20 = new MyTabConfig("Tagihan Per Siswa");
		tab20.setParent(tabs);

		final MyTabConfig tab2 = new MyTabConfig("Laporan Penerimaan");
		tab2.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setHeight("1800px");
		tabpanel1.setParent(tabpanels);

		tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setHeight("1800px");
		tabpanel1.setParent(tabpanels);

		tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setHeight("1800px");
		tabpanel1.setParent(tabpanels);

		tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setHeight("1800px");
		tabpanel1.setParent(tabpanels);

		tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setHeight("1800px");
		tabpanel1.setParent(tabpanels);

		tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setHeight("1800px");
		tabpanel1.setParent(tabpanels);

		tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setHeight("1800px");
		tabpanel1.setParent(tabpanels);

		final EventListener listener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Tabpanel tabpanel = tabbox.getSelectedPanel();
				Common.clear(tabpanel);
				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(tabpanel);

				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);
				Tab tab = tabbox.getSelectedTab();
				if (tab == tab1) {
					generatePerItem(center);
				} else if (tab == tab11) {
					generatePerItemVa(center);
				} else if (tab == tab12) {
					generatePerItemRinci(center);
				} else if (tab == tab122) {
					generatePerItemRinciDetail(center);
				} else if (tab == tab13) {
					generatePerItemRinciTagihan(center);
				} else if (tab == tab2) {
					generatePerItemSummary(center);
				} else if (tab == tab20) {
					generatePerSiswaSummary(center);
				}
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

		sekolah.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				eventListener.onEvent(arg0);
				Common.createDefaultTimer(listener);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", listener);
		MyToolbarbuttonConfig printDownload = new MyToolbarbuttonConfig("Download Excel", "/img/excel.png");
		printDownload.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				((EventListener) label.getAttribute("downloadExcel")).onEvent(arg0);
			}
		});

		print.setParent(row);
		row = new MyFormRow();
		row.setParent(rows);
		printDownload.setParent(row);

		tab1.addEventListener("onClick", listenerJika);
		tab11.addEventListener("onClick", listenerJika);
		tab12.addEventListener("onClick", listenerJika);
		tab122.addEventListener("onClick", listenerJika);
		tab13.addEventListener("onClick", listenerJika);
		tab2.addEventListener("onClick", listenerJika);
		tab20.addEventListener("onClick", listenerJika);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void generatePerItem(Center center) {

		label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
		final Intbox ukuran = new Intbox(16);

		new Thread(new Runnable() {

			@Override
			public void run() {
				Sekolah mySekolah = (Sekolah) (sekolah.getSelectedItem() == null
						|| sekolah.getSelectedItem().getValue() == null ? null : sekolah.getSelectedItem().getValue());
				Yayasan myYayasan = (Yayasan) (yayasan.getSelectedItem() == null
						|| yayasan.getSelectedItem().getValue() == null ? null : yayasan.getSelectedItem().getValue());
				JenisBiayaSekolah myJenisBiayaSekolah = (JenisBiayaSekolah) (jenisBiayaSekolah.getSelectedItem() == null
						|| jenisBiayaSekolah.getSelectedItem().getValue() == null ? null
								: jenisBiayaSekolah.getSelectedItem().getValue());

				String ta = (String) (tahunAjaran.getSelectedItem() == null
						|| tahunAjaran.getSelectedItem().getValue() == null ? null
								: tahunAjaran.getSelectedItem().getValue());

				AkunPembayaranSiswa myAkunPembayaranSiswa = (AkunPembayaranSiswa) (akunPembayaranSiswa
						.getSelectedItem() == null || akunPembayaranSiswa.getSelectedItem().getValue() == null ? null
								: akunPembayaranSiswa.getSelectedItem().getValue());

				String namaAkun = namaAkunPembayaranSiswa.getValue().trim();

				KelasSiswa myKelasSiswa = (KelasSiswa) kelas.getAttribute("kelas");
				AsramaSiswa myAsramaSiswa = (AsramaSiswa) asrama.getAttribute("asrama");

				Integer tahun = (Integer) (LaporanRincianPembayaranSiswa.this.tahun.getSelectedItem() == null
						|| LaporanRincianPembayaranSiswa.this.tahun.getSelectedItem().getValue() == null ? null
								: LaporanRincianPembayaranSiswa.this.tahun.getSelectedItem().getValue());
				Integer bulan = (Integer) (LaporanRincianPembayaranSiswa.this.bulan.getSelectedItem() == null
						|| LaporanRincianPembayaranSiswa.this.bulan.getSelectedItem().getValue() == null ? null
								: LaporanRincianPembayaranSiswa.this.bulan.getSelectedItem().getValue());

				Integer angkatan = (Integer) (LaporanRincianPembayaranSiswa.this.angkatan.getSelectedItem() == null
						|| LaporanRincianPembayaranSiswa.this.angkatan.getSelectedItem().getValue() == null ? null
								: LaporanRincianPembayaranSiswa.this.angkatan.getSelectedItem().getValue());

				String sql1 = "select \n" + "d.id,d.nama\n" + "from sekolah.pembayaran_siswa_detail a \n"
						+ "inner join sekolah.pembayaran_siswa b on (a.pembayaran_siswa_id = b.id)\n"
						+ " left join sekolah.siswa c on (b.siswa_id = c.id)\n"
						+ " left join sekolah.calon_siswa cs on (b.calon_siswa_id = cs.id)\n"
						+ "inner join sekolah.item_biaya_sekolah d on (d.id = a.item_biaya_id)\n " + "\n"
						+ "left join sekolah.akun_pembayaran_siswa b3 on (b3.id = b.akun_pembayaran_siswa_id)\n "
						+ " inner join sekolah.tagihan t on (t.pembayaran_siswa_detail_id=a.id) "
						+ " inner join sekolah.pengaturan_biaya p on (t.pengaturan_biaya = p.id) \n"
						+ "where date(b.tanggal) between date('"
						+ Common.databaseDateFormat.get().format(mulai.getValue()) + "') and date('"
						+ Common.databaseDateFormat.get().format(sampai.getValue()) + "')"
						+ (myAkunPembayaranSiswa == null ? ""
								: " and b.akun_pembayaran_siswa_id = " + myAkunPembayaranSiswa.getId() + " \n")
						+ (myJenisBiayaSekolah == null ? ""
								: " and b.jenis_biaya_id = " + myJenisBiayaSekolah.getId() + " \n")
						+ (myYayasan == null ? ""
								: " and (c.yayasan_id = " + myYayasan.getId() + " or cs.yayasan_id = "
										+ myYayasan.getId() + ")  \n")
						+ (mySekolah == null ? ""
								: " and (c.sekolah_id = " + mySekolah.getId() + " or cs.sekolah_id = "
										+ mySekolah.getId() + ")  \n")

						+ (namaAkun.isEmpty() ? "" : " and (b3.nama_pembayaran ilike '%" + namaAkun + "%') \n")

						+ (siswa.getValue().trim().isEmpty() ? ""
								: " and (c.nomor_induk ilike '%" + siswa.getValue().trim()
										+ "%' or c.nama_siswa ilike '%" + siswa.getValue().trim()
										+ "%' or cs.nomor_induk ilike '%" + siswa.getValue().trim()
										+ "%' or cs.nama_siswa ilike '%" + siswa.getValue().trim() + "%') \n")

						+ (myKelasSiswa == null ? "" : " and c.current_kelas_id = " + myKelasSiswa.getId() + " \n")
						+ (myAsramaSiswa == null ? "" : " and c.asrama_id = " + myAsramaSiswa.getId() + " \n")
						+ (angkatan == null ? ""
								: " and (c.tahun_masuk = " + angkatan + " or cs.tahun_masuk = " + angkatan + ") \n")
						+ (tahun == null ? "" : " and b.tahun = " + tahun + " \n")
						+ (bulan == null ? "" : " and b.bulan = " + bulan + " \n")

						+ (ta == null ? "" : " and p.tahunajaran = '" + ta + "' \n")

				;

				Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser != null && tbmuser.getOrangTua() != null
						&& !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
					String ids = "";
					for (Long id : tbmuser.getOrangTua().ambilAnakSiswa()) {
						ids += ids.isEmpty() ? id.toString() : "," + id;
					}

					sql1 += " and b.siswa_id in (" + ids + ") ";
				}

				sql1 += " group by d.id\n" + "order by d.nama";

				System.out.println(sql1);

				Session session = HibernateUtil.currentSession();

				List<Map> itemBiayas = session.createSQLQuery(sql1)
						.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).list();

				ukuran.setValue(itemBiayas.size());

				List<List> datas = new ArrayList<List>();
				ArrayList arrayList = new ArrayList();
				arrayList.add("Rekap Pembayaran");
				datas.add(arrayList);

				arrayList = new ArrayList();
				arrayList.add("No.");
				arrayList.add("NIS");
				arrayList.add("Nama");
				arrayList.add("Kelas");
				for (Map itemBiaya : itemBiayas) {
					arrayList.add(itemBiaya.get("nama"));
				}
				arrayList.add("Total");
				arrayList.add("Cara Pembayaran");
				datas.add(arrayList);

				String sql = "select\n"
						+ "(case when c.nomor_induk is null then cs.nomor_induk else c.nomor_induk end) as nomor_induk,\n"
						+ "(case when c.nama_siswa is null then cs.nama_siswa else c.nama_siswa end) as nama_siswa,\n"
						+ "max(case when b1.nama is null then '-' else b1.nama end) as kelas,\n";

				for (Map itemBiaya : itemBiayas) {
					sql += "sum(case when a.item_biaya_id=" + itemBiaya.get("id")
							+ " then a.nominal else 0 end) as item" + itemBiaya.get("id") + ",";
				}

				sql += "sum(a.nominal) as total,\n"
						+ "(case when b2.nama is null then b3.nama_pembayaran else b2.nama end) as cara, max(b.siswa_id) as siswa_id, max(p.tahunajaran) as tahunajaran \n"
						+ "from sekolah.pembayaran_siswa_detail a\n"
						+ "inner join sekolah.pembayaran_siswa b on (a.pembayaran_siswa_id = b.id)\n"
						+ " left join sekolah.siswa c on (b.siswa_id = c.id)\n"
						+ " left join sekolah.calon_siswa cs on (b.calon_siswa_id = cs.id)\n"
						+ "inner join sekolah.item_biaya_sekolah d on (d.id = a.item_biaya_id)\n"
						+ "left join bank_host b2 on (b2.id = b.bank_host_id)\n"
						+ "left join sekolah.akun_pembayaran_siswa b3 on (b3.id = b.akun_pembayaran_siswa_id)\n "
						+ "left join sekolah.deposit_siswa e on (e.pembayaran_siswa_id=b.id)\n " + "\n"
						+ " inner join sekolah.tagihan t on (t.pembayaran_siswa_detail_id=a.id) "
						+ " inner join sekolah.pengaturan_biaya p on (t.pengaturan_biaya = p.id) \n"
						+ " left join sekolah.kelas b1 on (b1.id = t.kelas_siswa_id)\n"
						+ "where  b.nominal > 0.1 "
						+ " and date(b.tanggal) between date('" + Common.databaseDateFormat.get().format(mulai.getValue())
						+ "') and date('" + Common.databaseDateFormat.get().format(sampai.getValue()) + "')"
						+ (myAkunPembayaranSiswa == null ? ""
								: " and b.akun_pembayaran_siswa_id = " + myAkunPembayaranSiswa.getId() + " \n")
						+ (myJenisBiayaSekolah == null ? ""
								: " and b.jenis_biaya_id = " + myJenisBiayaSekolah.getId() + " \n")
						+ (myYayasan == null ? ""
								: " and (c.yayasan_id = " + myYayasan.getId() + " or cs.yayasan_id = "
										+ myYayasan.getId() + ")  \n")
						+ (mySekolah == null ? ""
								: " and (c.sekolah_id = " + mySekolah.getId() + " or cs.sekolah_id = "
										+ mySekolah.getId() + ")  \n")
						+ (siswa.getValue().trim().isEmpty() ? ""
								: " and (c.nomor_induk ilike '%" + siswa.getValue().trim()
										+ "%' or c.nama_siswa ilike '%" + siswa.getValue().trim()
										+ "%' or cs.nomor_induk ilike '%" + siswa.getValue().trim()
										+ "%' or cs.nama_siswa ilike '%" + siswa.getValue().trim() + "%') \n")

						+ (namaAkun.isEmpty() ? "" : " and (b3.nama_pembayaran ilike '%" + namaAkun + "%') \n")

						+ (myKelasSiswa == null ? "" : " and t.kelas_siswa_id = " + myKelasSiswa.getId() + " \n")
						+ (myAsramaSiswa == null ? "" : " and c.asrama_id = " + myAsramaSiswa.getId() + " \n")
						+ (angkatan == null ? ""
								: " and (c.tahun_masuk = " + angkatan + " or cs.tahun_masuk = " + angkatan + ") \n")
						+ (tahun == null ? "" : " and b.tahun = " + tahun + " \n")
						+ (bulan == null ? "" : " and b.bulan = " + bulan + " \n") + "\n"
						+ (ta == null ? "" : " and p.tahunajaran = '" + ta + "' \n");

				if (tbmuser != null && tbmuser.getOrangTua() != null
						&& !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
					String ids = "";
					for (Long id : tbmuser.getOrangTua().ambilAnakSiswa()) {
						ids += ids.isEmpty() ? id.toString() : "," + id;
					}

					sql += " and b.siswa_id in (" + ids + ") ";
				}

				sql += "group by c.id,cs.id,(case when b2.nama is null then b3.nama_pembayaran else b2.nama end)\n"
						+ "order by (case when c.nomor_induk is null then cs.nomor_induk else c.nomor_induk end)";

				System.out.println(sql);

				Collection<Map<String, Object>> dataArray = session.createSQLQuery(sql)
						.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).list();

				int index = 1;
				Double total = 0.0;
				Map<Object, Double> totals = new HashMap<Object, Double>();
				for (Map<String, Object> map : dataArray) {
					arrayList = new ArrayList();
					arrayList.add(index);
					arrayList.add(map.get("nomor_induk"));
					arrayList.add(map.get("nama_siswa"));

					String kelas = map.get("kelas") + "";
					try {
						if (kelas == null || kelas.trim().equalsIgnoreCase("-")) {

							Long siswa_id = Long.parseLong(map.get("siswa_id") + "");
							String tahunajaran = map.get("tahunajaran") + "";

							System.out.println("siswa_id -> " + siswa_id + " tahunajaran -> " + tahunajaran);

							kelas = (String) session.createCriteria(KelasSiswaPunyaSiswa.class)
									.createAlias("kelasSiswa", "kelasSiswa")
									.add(Restrictions.eq("kelasSiswa.aktif", true))
									.add(Restrictions.eq("siswa.id", siswa_id))
									.add(Restrictions.eq("kelasSiswa.tahunAjaran", tahunajaran)).setMaxResults(1)
									.setProjection(Projections.max("kelasSiswa.nama")).uniqueResult();
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRincianPembayaranSiswa.java:718");
						// TODO: handle exception
					}
					arrayList.add(kelas);

					for (Map itemBiaya : itemBiayas) {
						Object key = itemBiaya.get("id");
						Number nilai = (Number) map.get("item" + key);
						arrayList.add(nilai == null ? "" : Common.numberFormat.get().format(nilai));

						if (totals.containsKey(key)) {
							Double semebelumya = totals.get(key);
							totals.put(key, (nilai == null ? 0.0 : nilai.doubleValue()) + semebelumya);
						} else {
							totals.put(key, (nilai == null ? 0.0 : nilai.doubleValue()));
						}
					}

					Number nilai = (Number) map.get("total");
					arrayList.add(nilai == null ? "" : Common.numberFormat.get().format(nilai));
					total += nilai == null ? 0 : nilai.doubleValue();
					arrayList.add(map.get("cara"));

					datas.add(arrayList);
					index++;
				}

				arrayList = new ArrayList();
				arrayList.add("");
				arrayList.add("");
				arrayList.add("");
				arrayList.add("Total");
				for (Map itemBiaya : itemBiayas) {
					Object key = itemBiaya.get("id");
					Double nilai = totals.get(key);
					arrayList.add(nilai == null ? "" : Common.numberFormat.get().format(nilai));
				}
				arrayList.add(Common.numberFormat.get().format(total));
				arrayList.add("");
				datas.add(arrayList);

				label.setAttribute("datas", datas);
				ais.action.report.helper.LoadingReportUtil.selesai(label);
			}
		}).start();

		SaptoUtil.displayWorksheet(label, "data_umum", center, 30 + ukuran.getValue());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void generatePerItemVa(Center center) {

		label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
		final Intbox ukuran = new Intbox(16);

		new Thread(new Runnable() {

			@Override
			public void run() {
				Sekolah mySekolah = (Sekolah) (sekolah.getSelectedItem() == null
						|| sekolah.getSelectedItem().getValue() == null ? null : sekolah.getSelectedItem().getValue());
				Yayasan myYayasan = (Yayasan) (yayasan.getSelectedItem() == null
						|| yayasan.getSelectedItem().getValue() == null ? null : yayasan.getSelectedItem().getValue());
				JenisBiayaSekolah myJenisBiayaSekolah = (JenisBiayaSekolah) (jenisBiayaSekolah.getSelectedItem() == null
						|| jenisBiayaSekolah.getSelectedItem().getValue() == null ? null
								: jenisBiayaSekolah.getSelectedItem().getValue());

				String ta = (String) (tahunAjaran.getSelectedItem() == null
						|| tahunAjaran.getSelectedItem().getValue() == null ? null
								: tahunAjaran.getSelectedItem().getValue());

				AkunPembayaranSiswa myAkunPembayaranSiswa = (AkunPembayaranSiswa) (akunPembayaranSiswa
						.getSelectedItem() == null || akunPembayaranSiswa.getSelectedItem().getValue() == null ? null
								: akunPembayaranSiswa.getSelectedItem().getValue());

				String namaAkun = namaAkunPembayaranSiswa.getValue().trim();

				KelasSiswa myKelasSiswa = (KelasSiswa) kelas.getAttribute("kelas");
				AsramaSiswa myAsramaSiswa = (AsramaSiswa) asrama.getAttribute("asrama");

				Integer tahun = (Integer) (LaporanRincianPembayaranSiswa.this.tahun.getSelectedItem() == null
						|| LaporanRincianPembayaranSiswa.this.tahun.getSelectedItem().getValue() == null ? null
								: LaporanRincianPembayaranSiswa.this.tahun.getSelectedItem().getValue());
				Integer bulan = (Integer) (LaporanRincianPembayaranSiswa.this.bulan.getSelectedItem() == null
						|| LaporanRincianPembayaranSiswa.this.bulan.getSelectedItem().getValue() == null ? null
								: LaporanRincianPembayaranSiswa.this.bulan.getSelectedItem().getValue());

				Integer angkatan = (Integer) (LaporanRincianPembayaranSiswa.this.angkatan.getSelectedItem() == null
						|| LaporanRincianPembayaranSiswa.this.angkatan.getSelectedItem().getValue() == null ? null
								: LaporanRincianPembayaranSiswa.this.angkatan.getSelectedItem().getValue());

				String sql1 = "select \n" + "d.id,d.nama\n" + "from sekolah.pembayaran_siswa_detail a \n"
						+ "inner join sekolah.pembayaran_siswa b on (a.pembayaran_siswa_id = b.id)\n"
						+ " left join sekolah.siswa c on (b.siswa_id = c.id)\n"
						+ " left join sekolah.calon_siswa cs on (b.calon_siswa_id = cs.id)\n"
						+ " inner join sekolah.item_biaya_sekolah d on (d.id = a.item_biaya_id)\n " + "\n"
						+ " left join sekolah.akun_pembayaran_siswa b3 on (b3.id = b.akun_pembayaran_siswa_id)\n "
						+ " inner join sekolah.tagihan t on (t.pembayaran_siswa_detail_id=a.id) "
						+ " inner join sekolah.pengaturan_biaya p on (t.pengaturan_biaya = p.id) \n"
						+ "where b.nominal > 0.1 and date(b.tanggal) between date('"
						+ Common.databaseDateFormat.get().format(mulai.getValue()) + "') and date('"
						+ Common.databaseDateFormat.get().format(sampai.getValue()) + "')"
						+ (myAkunPembayaranSiswa == null ? ""
								: " and b.akun_pembayaran_siswa_id = " + myAkunPembayaranSiswa.getId() + " \n")
						+ (myJenisBiayaSekolah == null ? ""
								: " and b.jenis_biaya_id = " + myJenisBiayaSekolah.getId() + " \n")
						+ (myYayasan == null ? ""
								: " and (c.yayasan_id = " + myYayasan.getId() + " or cs.yayasan_id = "
										+ myYayasan.getId() + ")  \n")
						+ (mySekolah == null ? ""
								: " and (c.sekolah_id = " + mySekolah.getId() + " or cs.sekolah_id = "
										+ mySekolah.getId() + ")  \n")

						+ (namaAkun.isEmpty() ? "" : " and (b3.nama_pembayaran ilike '%" + namaAkun + "%') \n")

						+ (siswa.getValue().trim().isEmpty() ? ""
								: " and (c.nomor_induk ilike '%" + siswa.getValue().trim()
										+ "%' or c.nama_siswa ilike '%" + siswa.getValue().trim()
										+ "%' or cs.nomor_induk ilike '%" + siswa.getValue().trim()
										+ "%' or cs.nama_siswa ilike '%" + siswa.getValue().trim() + "%') \n")

						+ (myKelasSiswa == null ? "" : " and c.current_kelas_id = " + myKelasSiswa.getId() + " \n")
						+ (myAsramaSiswa == null ? "" : " and c.asrama_id = " + myAsramaSiswa.getId() + " \n")
						+ (angkatan == null ? ""
								: " and (c.tahun_masuk = " + angkatan + " or cs.tahun_masuk = " + angkatan + ") \n")
						+ (tahun == null ? "" : " and b.tahun = " + tahun + " \n")
						+ (bulan == null ? "" : " and b.bulan = " + bulan + " \n")
						+ (ta == null ? "" : " and p.tahunajaran = '" + ta + "' \n");

				Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser != null && tbmuser.getOrangTua() != null
						&& !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
					String ids = "";
					for (Long id : tbmuser.getOrangTua().ambilAnakSiswa()) {
						ids += ids.isEmpty() ? id.toString() : "," + id;
					}

					sql1 += " and b.siswa_id in (" + ids + ") ";
				}

				sql1 += " group by d.id\n" + "order by d.nama";

				System.out.println(sql1);

				Session session = HibernateUtil.currentSession();

				List<Map> itemBiayas = session.createSQLQuery(sql1)
						.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).list();

				ukuran.setValue(itemBiayas.size());

				List<List> datas = new ArrayList<List>();
				ArrayList arrayList = new ArrayList();
				arrayList.add("Rekap Pembayaran");
				datas.add(arrayList);

				arrayList = new ArrayList();
				arrayList.add("No.");
				arrayList.add("Kode Trx");
				arrayList.add("Nama Trx");
				arrayList.add("Waktu Byr");
				arrayList.add("NIS");
				arrayList.add("Nama");
				arrayList.add("Kelas");
				for (Map itemBiaya : itemBiayas) {
					arrayList.add(itemBiaya.get("nama"));
				}
				arrayList.add("Total");
				arrayList.add("Cara Pembayaran");
				datas.add(arrayList);

				String sql = "select\n"
						+ "(case when c.nomor_induk is null then cs.nomor_induk else c.nomor_induk end) as nomor_induk,\n"
						+ "(case when c.nama_siswa is null then cs.nama_siswa else c.nama_siswa end) as nama_siswa,\n"
						+ "max(case when b1.nama is null then '-' else b1.nama end) as kelas,\n";

				for (Map itemBiaya : itemBiayas) {
					sql += "sum(case when a.item_biaya_id=" + itemBiaya.get("id")
							+ " then a.nominal else 0 end) as item" + itemBiaya.get("id") + ",";
				}

				sql += "sum(a.nominal) as total,\n"
						+ "max(case when b2.nama is null then b3.nama_pembayaran else b2.nama end) as cara, \n"
						+ "max(case when v.kode is null then v1.va else v.kode end) as kode_va, \n" 
						+ "max(case when v.waktubayar is null then v1.tanggal_dirubah else v.waktubayar end) as tanggal_va, max(case when v.nama is null then v1.session_id else v.nama end) as nama_va \n"
						+ "from sekolah.pembayaran_siswa_detail a\n"
						+ "inner join sekolah.pembayaran_siswa b on (a.pembayaran_siswa_id = b.id)\n"
						+ " left join virtual_account_bank v on (v.id = b.virtual_account_bank)\n "
						+ " left join bni_request v1 on (v1.id = b.bni_request_id)\n "
						+ " left join sekolah.siswa c on (b.siswa_id = c.id)\n"
						+ " left join sekolah.calon_siswa cs on (b.calon_siswa_id = cs.id)\n"
						+ "inner join sekolah.item_biaya_sekolah d on (d.id = a.item_biaya_id)\n"
						+ "left join bank_host b2 on (b2.id = b.bank_host_id)\n"
						+ "left join sekolah.akun_pembayaran_siswa b3 on (b3.id = b.akun_pembayaran_siswa_id)\n "
						+ "left join sekolah.deposit_siswa e on (e.pembayaran_siswa_id=b.id)\n " + "\n"
						+ " inner join sekolah.tagihan t on (t.pembayaran_siswa_detail_id=a.id) \n"
						+ " inner join sekolah.pengaturan_biaya p on (t.pengaturan_biaya = p.id) \n"
						+ " left join sekolah.kelas b1 on (b1.id = t.kelas_siswa_id)\n"
						+ "where b.nominal > 0.1 "
						+ " and date(b.tanggal) between date('" + Common.databaseDateFormat.get().format(mulai.getValue())
						+ "') and date('" + Common.databaseDateFormat.get().format(sampai.getValue()) + "')"
						+ (myAkunPembayaranSiswa == null ? ""
								: " and b.akun_pembayaran_siswa_id = " + myAkunPembayaranSiswa.getId() + " \n")
						+ (myJenisBiayaSekolah == null ? ""
								: " and b.jenis_biaya_id = " + myJenisBiayaSekolah.getId() + " \n")
						+ (myYayasan == null ? ""
								: " and (c.yayasan_id = " + myYayasan.getId() + " or cs.yayasan_id = "
										+ myYayasan.getId() + ")  \n")
						+ (mySekolah == null ? ""
								: " and (c.sekolah_id = " + mySekolah.getId() + " or cs.sekolah_id = "
										+ mySekolah.getId() + ")  \n")
						+ (siswa.getValue().trim().isEmpty() ? ""
								: " and (c.nomor_induk ilike '%" + siswa.getValue().trim()
										+ "%' or c.nama_siswa ilike '%" + siswa.getValue().trim()
										+ "%' or cs.nomor_induk ilike '%" + siswa.getValue().trim()
										+ "%' or cs.nama_siswa ilike '%" + siswa.getValue().trim() + "%') \n")

						+ (namaAkun.isEmpty() ? "" : " and (b3.nama_pembayaran ilike '%" + namaAkun + "%') \n")

						+ (myKelasSiswa == null ? "" : " and t.kelas_siswa_id = " + myKelasSiswa.getId() + " \n")
						+ (myAsramaSiswa == null ? "" : " and c.asrama_id = " + myAsramaSiswa.getId() + " \n")
						+ (angkatan == null ? ""
								: " and (c.tahun_masuk = " + angkatan + " or cs.tahun_masuk = " + angkatan + ") \n")
						+ (tahun == null ? "" : " and b.tahun = " + tahun + " \n")
						+ (bulan == null ? "" : " and b.bulan = " + bulan + " \n") + "\n"
						+ (ta == null ? "" : " and p.tahunajaran = '" + ta + "' \n");

				if (tbmuser != null && tbmuser.getOrangTua() != null
						&& !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
					String ids = "";
					for (Long id : tbmuser.getOrangTua().ambilAnakSiswa()) {
						ids += ids.isEmpty() ? id.toString() : "," + id;
					}

					sql += " and b.siswa_id in (" + ids + ") ";
				}

				sql += "group by (case when v.id is null then v1.id else v.id end),c.id,cs.id\n order by max(case when v.waktubayar is null then v1.tanggal_dirubah else v.waktubayar end),c.id,cs.id";

				System.out.println(sql);

				Collection<Map<String, Object>> dataArray = session.createSQLQuery(sql)
						.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).list();

				int index = 1;
				Double total = 0.0;
				Map<Object, Double> totals = new HashMap<Object, Double>();
				for (Map<String, Object> map : dataArray) {
					arrayList = new ArrayList();
					arrayList.add(index);
					arrayList.add(map.get("kode_va"));
					arrayList.add(map.get("nama_va"));
					arrayList
							.add(map.get("tanggal_va") == null ? "" : Common.dateFormat3.get().format(map.get("tanggal_va")));
					arrayList.add(map.get("nomor_induk"));
					arrayList.add(map.get("nama_siswa"));
					arrayList.add(map.get("kelas"));
					for (Map itemBiaya : itemBiayas) {
						Object key = itemBiaya.get("id");
						Number nilai = (Number) map.get("item" + key);
						arrayList.add(nilai == null ? "" : Common.numberFormat.get().format(nilai));

						if (totals.containsKey(key)) {
							Double semebelumya = totals.get(key);
							totals.put(key, (nilai == null ? 0.0 : nilai.doubleValue()) + semebelumya);
						} else {
							totals.put(key, (nilai == null ? 0.0 : nilai.doubleValue()));
						}
					}

					Number nilai = (Number) map.get("total");
					arrayList.add(nilai == null ? "" : Common.numberFormat.get().format(nilai));
					total += nilai == null ? 0 : nilai.doubleValue();
					arrayList.add(map.get("cara"));

					datas.add(arrayList);
					index++;
				}

				arrayList = new ArrayList();
				arrayList.add("");
				arrayList.add("");
				arrayList.add("");
				arrayList.add("");
				arrayList.add("");
				arrayList.add("");
				arrayList.add("Total");
				for (Map itemBiaya : itemBiayas) {
					Object key = itemBiaya.get("id");
					Double nilai = totals.get(key);
					arrayList.add(nilai == null ? "" : Common.numberFormat.get().format(nilai));
				}
				arrayList.add(Common.numberFormat.get().format(total));
				arrayList.add("");
				datas.add(arrayList);

				label.setAttribute("datas", datas);
				ais.action.report.helper.LoadingReportUtil.selesai(label);
			}
		}).start();

		SaptoUtil.displayWorksheet(label, "data_umum", center, 30 + ukuran.getValue());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void generatePerItemRinci(Center center) {

		label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

		new Thread(new Runnable() {

			@Override
			public void run() {
				Sekolah mySekolah = (Sekolah) (sekolah.getSelectedItem() == null
						|| sekolah.getSelectedItem().getValue() == null ? null : sekolah.getSelectedItem().getValue());
				Yayasan myYayasan = (Yayasan) (yayasan.getSelectedItem() == null
						|| yayasan.getSelectedItem().getValue() == null ? null : yayasan.getSelectedItem().getValue());
				JenisBiayaSekolah myJenisBiayaSekolah = (JenisBiayaSekolah) (jenisBiayaSekolah.getSelectedItem() == null
						|| jenisBiayaSekolah.getSelectedItem().getValue() == null ? null
								: jenisBiayaSekolah.getSelectedItem().getValue());

				AkunPembayaranSiswa myAkunPembayaranSiswa = (AkunPembayaranSiswa) (akunPembayaranSiswa
						.getSelectedItem() == null || akunPembayaranSiswa.getSelectedItem().getValue() == null ? null
								: akunPembayaranSiswa.getSelectedItem().getValue());

				String namaAkun = namaAkunPembayaranSiswa.getValue().trim();

				String ta = (String) (tahunAjaran.getSelectedItem() == null
						|| tahunAjaran.getSelectedItem().getValue() == null ? null
								: tahunAjaran.getSelectedItem().getValue());

				KelasSiswa myKelasSiswa = (KelasSiswa) kelas.getAttribute("kelas");
				AsramaSiswa myAsramaSiswa = (AsramaSiswa) asrama.getAttribute("asrama");

				Integer tahun = (Integer) (LaporanRincianPembayaranSiswa.this.tahun.getSelectedItem() == null
						|| LaporanRincianPembayaranSiswa.this.tahun.getSelectedItem().getValue() == null ? null
								: LaporanRincianPembayaranSiswa.this.tahun.getSelectedItem().getValue());
				Integer bulan = (Integer) (LaporanRincianPembayaranSiswa.this.bulan.getSelectedItem() == null
						|| LaporanRincianPembayaranSiswa.this.bulan.getSelectedItem().getValue() == null ? null
								: LaporanRincianPembayaranSiswa.this.bulan.getSelectedItem().getValue());

				Integer angkatan = (Integer) (LaporanRincianPembayaranSiswa.this.angkatan.getSelectedItem() == null
						|| LaporanRincianPembayaranSiswa.this.angkatan.getSelectedItem().getValue() == null ? null
								: LaporanRincianPembayaranSiswa.this.angkatan.getSelectedItem().getValue());

				Session session = HibernateUtil.currentSession();

				List<List> datas = new ArrayList<List>();
				ArrayList arrayList = new ArrayList();
				arrayList.add("Rekap Pembayaran");
				datas.add(arrayList);

				arrayList = new ArrayList();
				arrayList.add("No.");
				arrayList.add("Kode Item");
				arrayList.add("Nama Item");
				arrayList.add("NIS");
				arrayList.add("Nama");
				arrayList.add("Kelas");
				arrayList.add("Nilai");
				arrayList.add("Cara Pembayaran");
				datas.add(arrayList);

				String sql = "select\n"
						+ "(case when c.nomor_induk is null then cs.nomor_induk else c.nomor_induk end) as nomor_induk,\n"
						+ "(case when c.nama_siswa is null then cs.nama_siswa else c.nama_siswa end) as nama_siswa,\n"
						+ "max(case when b1.nama is null then '-' else b1.nama end) as kelas,\n";

				sql += "sum(a.nominal) as total,\n"
						+ "(case when b2.nama is null then b3.nama_pembayaran else b2.nama end) as cara, \n"
						+ "d.kode as kode_va, \n" + "d.nama as nama_va \n" + "from sekolah.pembayaran_siswa_detail a\n"
						+ "inner join sekolah.pembayaran_siswa b on (a.pembayaran_siswa_id = b.id)\n"
						+ " left join sekolah.siswa c on (b.siswa_id = c.id)\n"
						+ " left join sekolah.calon_siswa cs on (b.calon_siswa_id = cs.id)\n"
						+ "inner join sekolah.item_biaya_sekolah d on (d.id = a.item_biaya_id)\n"
						+ "left join bank_host b2 on (b2.id = b.bank_host_id)\n"
						+ "left join sekolah.akun_pembayaran_siswa b3 on (b3.id = b.akun_pembayaran_siswa_id)\n "
						+ "left join sekolah.deposit_siswa e on (e.pembayaran_siswa_id=b.id)\n " + "\n"
						+ " inner join sekolah.tagihan t on (t.pembayaran_siswa_detail_id=a.id) \n"
						+ " inner join sekolah.pengaturan_biaya p on (t.pengaturan_biaya = p.id) \n"
						+ " left join sekolah.kelas b1 on (b1.id = t.kelas_siswa_id)\n"
						+ "where b.nominal > 0.1 "
						+ " and date(b.tanggal) between date('" + Common.databaseDateFormat.get().format(mulai.getValue())
						+ "') and date('" + Common.databaseDateFormat.get().format(sampai.getValue()) + "')"
						+ (myAkunPembayaranSiswa == null ? ""
								: " and b.akun_pembayaran_siswa_id = " + myAkunPembayaranSiswa.getId() + " \n")
						+ (myJenisBiayaSekolah == null ? ""
								: " and b.jenis_biaya_id = " + myJenisBiayaSekolah.getId() + " \n")
						+ (myYayasan == null ? ""
								: " and (c.yayasan_id = " + myYayasan.getId() + " or cs.yayasan_id = "
										+ myYayasan.getId() + ")  \n")
						+ (mySekolah == null ? ""
								: " and (c.sekolah_id = " + mySekolah.getId() + " or cs.sekolah_id = "
										+ mySekolah.getId() + ")  \n")
						+ (siswa.getValue().trim().isEmpty() ? ""
								: " and (c.nomor_induk ilike '%" + siswa.getValue().trim()
										+ "%' or c.nama_siswa ilike '%" + siswa.getValue().trim()
										+ "%' or cs.nomor_induk ilike '%" + siswa.getValue().trim()
										+ "%' or cs.nama_siswa ilike '%" + siswa.getValue().trim() + "%') \n")

						+ (namaAkun.isEmpty() ? "" : " and (b3.nama_pembayaran ilike '%" + namaAkun + "%') \n")

						+ (myKelasSiswa == null ? "" : " and t.kelas_siswa_id = " + myKelasSiswa.getId() + " \n")
						+ (myAsramaSiswa == null ? "" : " and c.asrama_id = " + myAsramaSiswa.getId() + " \n")
						+ (angkatan == null ? ""
								: " and (c.tahun_masuk = " + angkatan + " or cs.tahun_masuk = " + angkatan + ") \n")
						+ (tahun == null ? "" : " and b.tahun = " + tahun + " \n")
						+ (bulan == null ? "" : " and b.bulan = " + bulan + " \n") + "\n"
						+ (ta == null ? "" : " and p.tahunajaran = '" + ta + "' \n");

				Tbmuser tbmuser = Common.getCurrentUser();

				if (tbmuser != null && tbmuser.getOrangTua() != null
						&& !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
					String ids = "";
					for (Long id : tbmuser.getOrangTua().ambilAnakSiswa()) {
						ids += ids.isEmpty() ? id.toString() : "," + id;
					}

					sql += " and b.siswa_id in (" + ids + ") ";
				}

				sql += "group by d.id,c.id,cs.id,(case when b2.nama is null then b3.nama_pembayaran else b2.nama end)\n"
						+ "order by d.nama,(case when c.nomor_induk is null then cs.nomor_induk else c.nomor_induk end)";

				System.out.println(sql);

				Collection<Map<String, Object>> dataArray = session.createSQLQuery(sql)
						.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).list();

				int index = 1;
				Double total = 0.0;
				for (Map<String, Object> map : dataArray) {
					arrayList = new ArrayList();
					arrayList.add(index);
					arrayList.add(map.get("kode_va"));
					arrayList.add(map.get("nama_va"));
					arrayList.add(map.get("nomor_induk"));
					arrayList.add(map.get("nama_siswa"));
					arrayList.add(map.get("kelas"));

					Number nilai = (Number) map.get("total");
					arrayList.add(nilai == null ? "" : Common.numberFormat.get().format(nilai));
					total += nilai == null ? 0 : nilai.doubleValue();
					arrayList.add(map.get("cara"));

					datas.add(arrayList);
					index++;
				}

				arrayList = new ArrayList();
				arrayList.add("");
				arrayList.add("");
				arrayList.add("");
				arrayList.add("");
				arrayList.add("");
				arrayList.add("Total");
				arrayList.add(Common.numberFormat.get().format(total));
				arrayList.add("");
				datas.add(arrayList);

				label.setAttribute("datas", datas);
				ais.action.report.helper.LoadingReportUtil.selesai(label);
			}
		}).start();

		SaptoUtil.displayWorksheet(label, "data_umum", center, 15);
	}

	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void generatePerItemRinciDetail(Center center) {

	    label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

	    new Thread(new Runnable() {

	        @Override
	        public void run() {
	            Sekolah mySekolah = (Sekolah) (sekolah.getSelectedItem() == null
	                    || sekolah.getSelectedItem().getValue() == null ? null : sekolah.getSelectedItem().getValue());
	            Yayasan myYayasan = (Yayasan) (yayasan.getSelectedItem() == null
	                    || yayasan.getSelectedItem().getValue() == null ? null : yayasan.getSelectedItem().getValue());
	            JenisBiayaSekolah myJenisBiayaSekolah = (JenisBiayaSekolah) (jenisBiayaSekolah.getSelectedItem() == null
	                    || jenisBiayaSekolah.getSelectedItem().getValue() == null ? null
	                            : jenisBiayaSekolah.getSelectedItem().getValue());

	            String ta = (String) (tahunAjaran.getSelectedItem() == null
	                    || tahunAjaran.getSelectedItem().getValue() == null ? null
	                            : tahunAjaran.getSelectedItem().getValue());

	            AkunPembayaranSiswa myAkunPembayaranSiswa = (AkunPembayaranSiswa) (akunPembayaranSiswa
	                    .getSelectedItem() == null || akunPembayaranSiswa.getSelectedItem().getValue() == null ? null
	                            : akunPembayaranSiswa.getSelectedItem().getValue());

	            String namaAkun = namaAkunPembayaranSiswa.getValue().trim();

	            KelasSiswa myKelasSiswa = (KelasSiswa) kelas.getAttribute("kelas");
	            AsramaSiswa myAsramaSiswa = (AsramaSiswa) asrama.getAttribute("asrama");

	            Integer tahun = (Integer) (LaporanRincianPembayaranSiswa.this.tahun.getSelectedItem() == null
	                    || LaporanRincianPembayaranSiswa.this.tahun.getSelectedItem().getValue() == null ? null
	                    : LaporanRincianPembayaranSiswa.this.tahun.getSelectedItem().getValue());
	            Integer bulan = (Integer) (LaporanRincianPembayaranSiswa.this.bulan.getSelectedItem() == null
	                    || LaporanRincianPembayaranSiswa.this.bulan.getSelectedItem().getValue() == null ? null
	                    : LaporanRincianPembayaranSiswa.this.bulan.getSelectedItem().getValue());

	            Integer angkatan = (Integer) (LaporanRincianPembayaranSiswa.this.angkatan.getSelectedItem() == null
	                    || LaporanRincianPembayaranSiswa.this.angkatan.getSelectedItem().getValue() == null ? null
	                    : LaporanRincianPembayaranSiswa.this.angkatan.getSelectedItem().getValue());

	            Session session = HibernateUtil.currentSession();

	            List<List> datas = new ArrayList<List>();
	            ArrayList arrayList = new ArrayList();
	            arrayList.add("Rekap Pembayaran Per Item Rinci");
	            datas.add(arrayList);

	            arrayList = new ArrayList();
	            arrayList.add("No.");
	            arrayList.add("Kode Item");
	            arrayList.add("Nama Item");
	            arrayList.add("Tahun Ajaran"); // Tambah Header
	            arrayList.add("NIS");
	            arrayList.add("Nama");
	            arrayList.add("Kelas");
	            arrayList.add("Bulan/Tahun");
	            arrayList.add("Tanggal Bayar");
	            arrayList.add("Nilai");
	            arrayList.add("Cara Pembayaran");
	            datas.add(arrayList);

	            // Menambahkan p.tahunajaran pada SELECT
	            String sql = "select\n"
	                    + "(case when c.nomor_induk is null then cs.nomor_induk else c.nomor_induk end) as nomor_induk,\n"
	                    + "(case when c.nama_siswa is null then cs.nama_siswa else c.nama_siswa end) as nama_siswa,\n"
	                    + "(case when b1.nama is null then '-' else b1.nama end) as kelas,\n"
	                    + "p.tahunajaran, \n"; // Ambil kolom tahunajaran dari tabel sekolah.pengaturan_biaya (alias p)

	            sql += "a.nominal as total,\n"
	                    + "(case when b2.nama is null then b3.nama_pembayaran else b2.nama end) as cara, \n"
	                    + "d.kode as kode_va, \n" + "d.nama as nama_va, t.tahunbulan, t.bayarke, b.tanggal \n"
	                    + "from sekolah.pembayaran_siswa_detail a\n"
	                    + "inner join sekolah.pembayaran_siswa b on (a.pembayaran_siswa_id = b.id)\n"
	                    + " left join sekolah.siswa c on (b.siswa_id = c.id)\n"
	                    + " left join sekolah.calon_siswa cs on (b.calon_siswa_id = cs.id)\n"
	                    + "inner join sekolah.item_biaya_sekolah d on (d.id = a.item_biaya_id)\n"
	                    + "left join bank_host b2 on (b2.id = b.bank_host_id)\n"
	                    + "left join sekolah.akun_pembayaran_siswa b3 on (b3.id = b.akun_pembayaran_siswa_id)\n "
	                    + "left join sekolah.deposit_siswa e on (e.pembayaran_siswa_id=b.id)\n " + "\n"
	                    + " inner join sekolah.tagihan t on (t.pembayaran_siswa_detail_id=a.id) \n"
	                    + " inner join sekolah.pengaturan_biaya p on (t.pengaturan_biaya = p.id) \n"
	                    + " left join sekolah.kelas b1 on (b1.id = t.kelas_siswa_id)\n"
	                    + "where b.nominal > 0.1 "
	                    + " and date(b.tanggal) between date('" + Common.databaseDateFormat.get().format(mulai.getValue())
	                    + "') and date('" + Common.databaseDateFormat.get().format(sampai.getValue()) + "')"
	                    + (myAkunPembayaranSiswa == null ? ""
	                            : " and b.akun_pembayaran_siswa_id = " + myAkunPembayaranSiswa.getId() + " \n")
	                    + (myJenisBiayaSekolah == null ? ""
	                            : " and b.jenis_biaya_id = " + myJenisBiayaSekolah.getId() + " \n")
	                    + (myYayasan == null ? ""
	                            : " and (c.yayasan_id = " + myYayasan.getId() + " or cs.yayasan_id = "
	                                    + myYayasan.getId() + ")  \n")
	                    + (mySekolah == null ? ""
	                            : " and (c.sekolah_id = " + mySekolah.getId() + " or cs.sekolah_id = "
	                                    + mySekolah.getId() + ")  \n")
	                    + (siswa.getValue().trim().isEmpty() ? ""
	                            : " and (c.nomor_induk ilike '%" + siswa.getValue().trim()
	                                    + "%' or c.nama_siswa ilike '%" + siswa.getValue().trim()
	                                    + "%' or cs.nomor_induk ilike '%" + siswa.getValue().trim()
	                                    + "%' or cs.nama_siswa ilike '%" + siswa.getValue().trim() + "%') \n")

	                    + (namaAkun.isEmpty() ? "" : " and (b3.nama_pembayaran ilike '%" + namaAkun + "%') \n")

	                    + (myKelasSiswa == null ? "" : " and t.kelas_siswa_id = " + myKelasSiswa.getId() + " \n")
	                    + (myAsramaSiswa == null ? "" : " and c.asrama_id = " + myAsramaSiswa.getId() + " \n")
	                    + (angkatan == null ? ""
	                            : " and (c.tahun_masuk = " + angkatan + " or cs.tahun_masuk = " + angkatan + ") \n")
	                    + (tahun == null ? "" : " and b.tahun = " + tahun + " \n")
	                    + (bulan == null ? "" : " and b.bulan = " + bulan + " \n") + "\n"
	                    + (ta == null ? "" : " and p.tahunajaran = '" + ta + "' \n");

	            Tbmuser tbmuser = Common.getCurrentUser();

	            if (tbmuser != null && tbmuser.getOrangTua() != null
	                    && !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
	                String ids = "";
	                for (Long id : tbmuser.getOrangTua().ambilAnakSiswa()) {
	                    ids += ids.isEmpty() ? id.toString() : "," + id;
	                }

	                sql += " and b.siswa_id in (" + ids + ") ";
	            }

	            sql += " order by b.tanggal,d.nama,(case when c.nomor_induk is null then cs.nomor_induk else c.nomor_induk end)";

	            System.out.println(sql);

	            Collection<Map<String, Object>> dataArray = session.createSQLQuery(sql)
	                    .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).list();

	            int index = 1;
	            Double total = 0.0;
	            for (Map<String, Object> map : dataArray) {
	                arrayList = new ArrayList();
	                arrayList.add(index);
	                arrayList.add(map.get("kode_va"));
	                arrayList.add(map.get("nama_va"));
	                arrayList.add(map.get("tahunajaran")); // Memasukkan data Tahun Ajaran
	                arrayList.add(map.get("nomor_induk"));
	                arrayList.add(map.get("nama_siswa"));
	                arrayList.add(map.get("kelas"));

	                arrayList.add((map.get("tahunbulan") == null ? "" : map.get("tahunbulan")) + ""
	                        + (map.get("bayarke") == null || map.get("bayarke").toString().equalsIgnoreCase("1") ? ""
	                                : " ke-" + map.get("bayarke")));
	                arrayList.add(map.get("tanggal") == null ? "" : Common.dateFormat5.get().format(map.get("tanggal")));

	                Number nilai = (Number) map.get("total");
	                arrayList.add(nilai == null ? "" : Common.numberFormat.get().format(nilai));
	                total += nilai == null ? 0 : nilai.doubleValue();
	                arrayList.add(map.get("cara"));

	                datas.add(arrayList);
	                index++;
	            }

	            // Bagian Total (disesuaikan urutan kolomnya)
	            arrayList = new ArrayList();
	            arrayList.add("");
	            arrayList.add("");
	            arrayList.add("");
	            arrayList.add("");
	            arrayList.add("");
	            arrayList.add("");
	            arrayList.add("Total");
	            arrayList.add(Common.numberFormat.get().format(total));
	            arrayList.add("");
	            datas.add(arrayList);

	            label.setAttribute("datas", datas);
	            ais.action.report.helper.LoadingReportUtil.selesai(label);
	        }
	    }).start();

	    // MaxColumns disesuaikan menjadi 11 karena ada penambahan kolom Tahun Ajaran
	    SaptoUtil.displayWorksheet(label, "data_umum", center, 11);
	}


	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void generatePerItemRinciTagihan(final Center center) {

		label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

		new Thread(new Runnable() {

			@Override
			public void run() {
				// 1. Pengambilan Parameter dari UI
				Sekolah mySekolah = (Sekolah) (sekolah.getSelectedItem() == null
						|| sekolah.getSelectedItem().getValue() == null ? null : sekolah.getSelectedItem().getValue());
				Yayasan myYayasan = (Yayasan) (yayasan.getSelectedItem() == null
						|| yayasan.getSelectedItem().getValue() == null ? null : yayasan.getSelectedItem().getValue());
				JenisBiayaSekolah myJenisBiayaSekolah = (JenisBiayaSekolah) (jenisBiayaSekolah.getSelectedItem() == null
						|| jenisBiayaSekolah.getSelectedItem().getValue() == null ? null
								: jenisBiayaSekolah.getSelectedItem().getValue());

				String ta = (String) (tahunAjaran.getSelectedItem() == null
						|| tahunAjaran.getSelectedItem().getValue() == null ? null
								: tahunAjaran.getSelectedItem().getValue());

				AkunPembayaranSiswa myAkunPembayaranSiswa = (AkunPembayaranSiswa) (akunPembayaranSiswa
						.getSelectedItem() == null || akunPembayaranSiswa.getSelectedItem().getValue() == null ? null
								: akunPembayaranSiswa.getSelectedItem().getValue());

				String namaAkun = namaAkunPembayaranSiswa.getValue().trim();

				KelasSiswa myKelasSiswa = (KelasSiswa) kelas.getAttribute("kelas");
				AsramaSiswa myAsramaSiswa = (AsramaSiswa) asrama.getAttribute("asrama");

				Integer tahunSel = (Integer) (LaporanRincianPembayaranSiswa.this.tahun.getSelectedItem() == null
						|| LaporanRincianPembayaranSiswa.this.tahun.getSelectedItem().getValue() == null ? null
								: LaporanRincianPembayaranSiswa.this.tahun.getSelectedItem().getValue());
				Integer bulanSel = (Integer) (LaporanRincianPembayaranSiswa.this.bulan.getSelectedItem() == null
						|| LaporanRincianPembayaranSiswa.this.bulan.getSelectedItem().getValue() == null ? null
								: LaporanRincianPembayaranSiswa.this.bulan.getSelectedItem().getValue());

				Integer angkatanSel = (Integer) (LaporanRincianPembayaranSiswa.this.angkatan.getSelectedItem() == null
						|| LaporanRincianPembayaranSiswa.this.angkatan.getSelectedItem().getValue() == null ? null
								: LaporanRincianPembayaranSiswa.this.angkatan.getSelectedItem().getValue());

				Session session = HibernateUtil.currentSession();

				// 2. Inisialisasi Header Excel/Worksheet
				List<List> datas = new ArrayList<List>();
				ArrayList arrayList = new ArrayList();
				arrayList.add("Rekap Pembayaran Per Item");
				datas.add(arrayList);

				arrayList = new ArrayList();
				arrayList.add("No.");
				arrayList.add("Kode Item");
				arrayList.add("Nama Item");
				arrayList.add("Bulan/Ke");
				arrayList.add("NIS");
				arrayList.add("Nama");
				arrayList.add("Kelas");
				arrayList.add("Nilai Tagihan");
				arrayList.add("Nilai Dibayar");
				arrayList.add("Porsi Tabungan"); // Kolom Baru
				arrayList.add("Nilai Sisa");
				arrayList.add("Cara Pembayaran");
				arrayList.add("Waktu Pembayaran");
				datas.add(arrayList);

				// 3. Penyusunan Query SQL dengan StringBuilder
				StringBuilder sql = new StringBuilder();
				sql.append("SELECT \n")
				   .append("(CASE WHEN c.nomor_induk IS NULL THEN cs.nomor_induk ELSE c.nomor_induk END) as nomor_induk, \n")
				   .append("(CASE WHEN c.nama_siswa IS NULL THEN cs.nama_siswa ELSE c.nama_siswa END) as nama_siswa, \n")
				   .append("(CASE WHEN b1.nama IS NULL THEN '-' ELSE b1.nama END) as kelas, \n")
				   .append("(CASE WHEN a.nominal IS NULL THEN 0 ELSE a.nominal END) as total, \n")
				   .append("(t.nominal) as total_tagihan, \n")
				   // Logika: Ambil daritabungan dibagi jumlah detail per ID pembayaran (Window Function)
				   .append("(COALESCE(b.daritabungan, 0) / NULLIF(COUNT(*) OVER (PARTITION BY b.id), 0)) as porsi_tabungan, \n")
				   .append("(CASE WHEN b2.nama IS NULL THEN b3.nama_pembayaran ELSE b2.nama END) as cara, \n")
				   .append("d.kode as kode_va, d.nama as nama_va, t.tahunbulan, t.bayarke, b.tanggal \n")
				   .append("FROM sekolah.tagihan t \n")
				   .append("INNER JOIN sekolah.pengaturan_biaya p ON (t.pengaturan_biaya = p.id AND (p.aktif OR p.aktif IS NULL)) \n")
				   .append("LEFT JOIN sekolah.pembayaran_siswa_detail a ON (t.pembayaran_siswa_detail_id = a.id) \n")
				   .append("LEFT JOIN sekolah.pembayaran_siswa b ON (a.pembayaran_siswa_id = b.id) \n")
				   .append("LEFT JOIN sekolah.siswa c ON (t.siswa_id = c.id) \n")
				   .append("LEFT JOIN sekolah.calon_siswa cs ON (t.calon_siswa_id = cs.id) \n")
				   .append("INNER JOIN sekolah.item_biaya_sekolah d ON (d.id = t.item_biaya_id) \n")
				   .append("LEFT JOIN bank_host b2 ON (b2.id = b.bank_host_id) \n")
				   .append("LEFT JOIN sekolah.akun_pembayaran_siswa b3 ON (b3.id = b.akun_pembayaran_siswa_id) \n")
				   .append("LEFT JOIN sekolah.kelas b1 ON (b1.id = t.kelas_siswa_id) \n")
				   .append("WHERE (c.aktif OR c.aktif IS NULL) AND t.nominal > 0.1 \n");

				// Filter Tanggal
				sql.append(" AND (b.tanggal IS NULL OR date(b.tanggal) BETWEEN date('")
				   .append(Common.databaseDateFormat.get().format(mulai.getValue())).append("') AND date('")
				   .append(Common.databaseDateFormat.get().format(sampai.getValue())).append("')) \n");

				// Filter Dinamis
				if (myAkunPembayaranSiswa != null) sql.append(" AND b.akun_pembayaran_siswa_id = ").append(myAkunPembayaranSiswa.getId()).append(" \n");
				if (myJenisBiayaSekolah != null) sql.append(" AND b.jenis_biaya_id = ").append(myJenisBiayaSekolah.getId()).append(" \n");
				if (myYayasan != null) sql.append(" AND (c.yayasan_id = ").append(myYayasan.getId()).append(" OR cs.yayasan_id = ").append(myYayasan.getId()).append(") \n");
				if (mySekolah != null) sql.append(" AND (c.sekolah_id = ").append(mySekolah.getId()).append(" OR cs.sekolah_id = ").append(mySekolah.getId()).append(") \n");
				
				if (!siswa.getValue().trim().isEmpty()) {
					String keyword = siswa.getValue().trim();
					sql.append(" AND (c.nomor_induk ILIKE '%").append(keyword).append("%' OR c.nama_siswa ILIKE '%").append(keyword)
					   .append("%' OR cs.nomor_induk ILIKE '%").append(keyword).append("%' OR cs.nama_siswa ILIKE '%").append(keyword).append("%') \n");
				}
				
				if (!namaAkun.isEmpty()) sql.append(" AND (b3.nama_pembayaran ILIKE '%").append(namaAkun).append("%') \n");
				if (myKelasSiswa != null) sql.append(" AND t.kelas_siswa_id = ").append(myKelasSiswa.getId()).append(" \n");
				if (myAsramaSiswa != null) sql.append(" AND c.asrama_id = ").append(myAsramaSiswa.getId()).append(" \n");
				if (angkatanSel != null) sql.append(" AND (c.tahun_masuk = ").append(angkatanSel).append(" OR cs.tahun_masuk = ").append(angkatanSel).append(") \n");
				if (tahunSel != null) sql.append(" AND t.tahun = ").append(tahunSel).append(" \n");
				if (bulanSel != null) sql.append(" AND t.bulan = ").append(bulanSel).append(" \n");
				if (ta != null) sql.append(" AND p.tahunajaran = '").append(ta).append("' \n");

				// Filter Anak (Role Orang Tua)
				Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser != null && tbmuser.getOrangTua() != null && !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
					StringBuilder ids = new StringBuilder();
					for (Long id : tbmuser.getOrangTua().ambilAnakSiswa()) {
						if (ids.length() > 0) ids.append(",");
						ids.append(id);
					}
					sql.append(" AND t.siswa_id IN (").append(ids).append(") ");
				}

				sql.append("ORDER BY b.tanggal, d.nama, nomor_induk, t.tahunbulan");

				// 4. Eksekusi Query
				Collection<Map<String, Object>> dataArray = session.createSQLQuery(sql.toString())
						.setResultTransformer(org.hibernate.transform.Transformers.ALIAS_TO_ENTITY_MAP).list();

				// 5. Pengolahan Data Hasil Query ke List
				int index = 1;
				Double totalDibayar = 0.0;
				Double totalTagihan_ = 0.0;
				Double totalTabungan_ = 0.0;

				for (Map<String, Object> map : dataArray) {
					arrayList = new ArrayList();
					arrayList.add(index);
					arrayList.add(map.get("kode_va"));
					arrayList.add(map.get("nama_va"));
					
					String per = (map.get("tahunbulan") == null ? "" : map.get("tahunbulan").toString());
					String ke = (map.get("bayarke") == null || map.get("bayarke").toString().equalsIgnoreCase("1") ? "" 
							: " ke-" + map.get("bayarke"));
					arrayList.add(per + ke);
					
					arrayList.add(map.get("nomor_induk"));
					arrayList.add(map.get("nama_siswa"));
					arrayList.add(map.get("kelas"));

					Number nilaiDibayar = (Number) map.get("total");
					Number tagihan = (Number) map.get("total_tagihan");
					Number porsiTabungan = (Number) map.get("porsi_tabungan");

					Double valDibayar = nilaiDibayar == null ? 0.0 : nilaiDibayar.doubleValue();
					Double valTagihan = tagihan == null ? 0.0 : tagihan.doubleValue();
					Double valTabungan = porsiTabungan == null ? 0.0 : porsiTabungan.doubleValue();
					
					valDibayar = valDibayar - valTabungan;

					arrayList.add(Common.numberFormat.get().format(valTagihan));
					arrayList.add(Common.numberFormat.get().format(valDibayar));
					arrayList.add(Common.numberFormat.get().format(valTabungan));

					// Akumulasi Total
					totalDibayar += valDibayar;
					totalTagihan_ += valTagihan;
					totalTabungan_ += valTabungan;

					// Sisa = Tagihan - (Dibayar + Tabungan)
					Double sisa = valTagihan - valDibayar - valTabungan;
					arrayList.add(Common.numberFormat.get().format(sisa));

					arrayList.add(map.get("cara"));
					arrayList.add(map.get("tanggal") == null ? "" : Common.dateFormat5.get().format(map.get("tanggal")));

					datas.add(arrayList);
					index++;
				}

				// 6. Baris Total (Footer)
				arrayList = new ArrayList();
				arrayList.add("");
				arrayList.add("");
				arrayList.add("");
				arrayList.add("");
				arrayList.add("");
				arrayList.add("");
				arrayList.add("GRAND TOTAL");
				arrayList.add(Common.numberFormat.get().format(totalTagihan_));
				arrayList.add(Common.numberFormat.get().format(totalDibayar));
				arrayList.add(Common.numberFormat.get().format(totalTabungan_));
				arrayList.add(Common.numberFormat.get().format(totalTagihan_ - totalDibayar - totalTabungan_));
				arrayList.add("");
				arrayList.add("");
				datas.add(arrayList);

				// Kembalikan ke UI
				label.setAttribute("datas", datas);
				ais.action.report.helper.LoadingReportUtil.selesai(label);
			}
		}).start();

		SaptoUtil.displayWorksheet(label, "data_umum", center, 18);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void generatePerItemSummary(Center center) {

		label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
		final Intbox ukuran = new Intbox(16);

		new Thread(new Runnable() {

			@Override
			public void run() {
				Sekolah mySekolah = (Sekolah) (sekolah.getSelectedItem() == null
						|| sekolah.getSelectedItem().getValue() == null ? null : sekolah.getSelectedItem().getValue());
				Yayasan myYayasan = (Yayasan) (yayasan.getSelectedItem() == null
						|| yayasan.getSelectedItem().getValue() == null ? null : yayasan.getSelectedItem().getValue());
				JenisBiayaSekolah myJenisBiayaSekolah = (JenisBiayaSekolah) (jenisBiayaSekolah.getSelectedItem() == null
						|| jenisBiayaSekolah.getSelectedItem().getValue() == null ? null
								: jenisBiayaSekolah.getSelectedItem().getValue());

				String ta = (String) (tahunAjaran.getSelectedItem() == null
						|| tahunAjaran.getSelectedItem().getValue() == null ? null
								: tahunAjaran.getSelectedItem().getValue());

				AkunPembayaranSiswa myAkunPembayaranSiswa = (AkunPembayaranSiswa) (akunPembayaranSiswa
						.getSelectedItem() == null || akunPembayaranSiswa.getSelectedItem().getValue() == null ? null
								: akunPembayaranSiswa.getSelectedItem().getValue());
				String namaAkun = namaAkunPembayaranSiswa.getValue().trim();
				KelasSiswa myKelasSiswa = (KelasSiswa) kelas.getAttribute("kelas");
				AsramaSiswa myAsramaSiswa = (AsramaSiswa) asrama.getAttribute("asrama");

				Integer tahun = (Integer) (LaporanRincianPembayaranSiswa.this.tahun.getSelectedItem() == null
						|| LaporanRincianPembayaranSiswa.this.tahun.getSelectedItem().getValue() == null ? null
								: LaporanRincianPembayaranSiswa.this.tahun.getSelectedItem().getValue());
				Integer bulan = (Integer) (LaporanRincianPembayaranSiswa.this.bulan.getSelectedItem() == null
						|| LaporanRincianPembayaranSiswa.this.bulan.getSelectedItem().getValue() == null ? null
								: LaporanRincianPembayaranSiswa.this.bulan.getSelectedItem().getValue());

				Integer angkatan = (Integer) (LaporanRincianPembayaranSiswa.this.angkatan.getSelectedItem() == null
						|| LaporanRincianPembayaranSiswa.this.angkatan.getSelectedItem().getValue() == null ? null
								: LaporanRincianPembayaranSiswa.this.angkatan.getSelectedItem().getValue());

				List<List> datas = new ArrayList<List>();
				ArrayList arrayList = new ArrayList();
				arrayList.add("Rekap Pembayaran");
				datas.add(arrayList);

				arrayList = new ArrayList();
				arrayList.add("No.");
				arrayList.add("NIS");
				arrayList.add("Nama");
				arrayList.add("Total");
				datas.add(arrayList);

				String sql = "select\n" + "d.id,d.nama,sum(a.nominal) as nilai\n"
						+ "from sekolah.pembayaran_siswa_detail a\n"
						+ " inner join sekolah.pembayaran_siswa b on (a.pembayaran_siswa_id = b.id)\n"
						+ " left join sekolah.siswa c on (b.siswa_id = c.id)\n"
						+ " left join sekolah.calon_siswa cs on (b.calon_siswa_id = cs.id)\n"
						+ " inner join sekolah.item_biaya_sekolah d on (d.id = a.item_biaya_id) "
						+ " inner join sekolah.tagihan t on (t.pembayaran_siswa_detail_id=a.id) \n"
						+ " inner join sekolah.pengaturan_biaya p on (t.pengaturan_biaya = p.id) \n"
						+ " left join sekolah.akun_pembayaran_siswa b3 on (b3.id = b.akun_pembayaran_siswa_id)\n "
						+ "where b.nominal > 0.1 "
						+ " and date(b.tanggal) between date('" + Common.databaseDateFormat.get().format(mulai.getValue())
						+ "') and date('" + Common.databaseDateFormat.get().format(sampai.getValue()) + "')"
						+ (myAkunPembayaranSiswa == null ? ""
								: " and b.akun_pembayaran_siswa_id = " + myAkunPembayaranSiswa.getId() + " \n")
						+ (myJenisBiayaSekolah == null ? ""
								: " and b.jenis_biaya_id = " + myJenisBiayaSekolah.getId() + " \n")
						+ (myYayasan == null ? ""
								: " and (c.yayasan_id = " + myYayasan.getId() + " or cs.yayasan_id = "
										+ myYayasan.getId() + ")  \n")
						+ (mySekolah == null ? ""
								: " and (c.sekolah_id = " + mySekolah.getId() + " or cs.sekolah_id = "
										+ mySekolah.getId() + ")  \n")
						+ (siswa.getValue().trim().isEmpty() ? ""
								: " and (c.nomor_induk ilike '%" + siswa.getValue().trim()
										+ "%' or c.nama_siswa ilike '%" + siswa.getValue().trim()
										+ "%' or cs.nomor_induk ilike '%" + siswa.getValue().trim()
										+ "%' or cs.nama_siswa ilike '%" + siswa.getValue().trim() + "%') \n")

						+ (namaAkun.isEmpty() ? "" : " and (b3.nama_pembayaran ilike '%" + namaAkun + "%') \n")

						+ (myKelasSiswa == null ? "" : " and c.current_kelas_id = " + myKelasSiswa.getId() + " \n")
						+ (myAsramaSiswa == null ? "" : " and c.asrama_id = " + myAsramaSiswa.getId() + " \n")
						+ (angkatan == null ? ""
								: " and (c.tahun_masuk = " + angkatan + " or cs.tahun_masuk = " + angkatan + ") \n")
						+ (ta == null ? "" : " and p.tahunajaran = '" + ta + "' \n");

				Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser != null && tbmuser.getOrangTua() != null
						&& !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
					String ids = "";
					for (Long id : tbmuser.getOrangTua().ambilAnakSiswa()) {
						ids += ids.isEmpty() ? id.toString() : "," + id;
					}

					sql += " and b.siswa_id in (" + ids + ") ";
				}

				sql += (tahun == null ? "" : " and b.tahun = " + tahun + " \n")
						+ (bulan == null ? "" : " and b.bulan = " + bulan + " \n") + "\n" + "group by d.id";
				Session session = HibernateUtil.currentSession();
				Collection<Map<String, Object>> dataArray = session.createSQLQuery(sql)
						.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).list();

				int index = 1;
				Double total = 0.0;
				for (Map<String, Object> map : dataArray) {
					Number nilai = (Number) map.get("nilai");
					arrayList = new ArrayList();
					arrayList.add(index);
					arrayList.add(map.get("nama"));
					arrayList.add(nilai == null ? "" : Common.numberFormat.get().format(nilai));
					total += nilai == null ? 0 : nilai.doubleValue();
					datas.add(arrayList);
					index++;
				}

				arrayList = new ArrayList();
				arrayList.add("");
				arrayList.add("Total");
				arrayList.add(Common.numberFormat.get().format(total));
				datas.add(arrayList);

				label.setAttribute("datas", datas);
				ais.action.report.helper.LoadingReportUtil.selesai(label);
			}
		}).start();

		SaptoUtil.displayWorksheet(label, "data_umum", center, 36 + ukuran.getValue());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void generatePerSiswaSummary(Center center) {

		label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
		final Intbox ukuran = new Intbox(16);

		new Thread(new Runnable() {

			@Override
			public void run() {
				Sekolah mySekolah = (Sekolah) (sekolah.getSelectedItem() == null
						|| sekolah.getSelectedItem().getValue() == null ? null : sekolah.getSelectedItem().getValue());
				Yayasan myYayasan = (Yayasan) (yayasan.getSelectedItem() == null
						|| yayasan.getSelectedItem().getValue() == null ? null : yayasan.getSelectedItem().getValue());
				JenisBiayaSekolah myJenisBiayaSekolah = (JenisBiayaSekolah) (jenisBiayaSekolah.getSelectedItem() == null
						|| jenisBiayaSekolah.getSelectedItem().getValue() == null ? null
								: jenisBiayaSekolah.getSelectedItem().getValue());

				String ta = (String) (tahunAjaran.getSelectedItem() == null
						|| tahunAjaran.getSelectedItem().getValue() == null ? null
								: tahunAjaran.getSelectedItem().getValue());

				AkunPembayaranSiswa myAkunPembayaranSiswa = (AkunPembayaranSiswa) (akunPembayaranSiswa
						.getSelectedItem() == null || akunPembayaranSiswa.getSelectedItem().getValue() == null ? null
								: akunPembayaranSiswa.getSelectedItem().getValue());
				String namaAkun = namaAkunPembayaranSiswa.getValue().trim();
				KelasSiswa myKelasSiswa = (KelasSiswa) kelas.getAttribute("kelas");
				AsramaSiswa myAsramaSiswa = (AsramaSiswa) asrama.getAttribute("asrama");

				Integer tahun = (Integer) (LaporanRincianPembayaranSiswa.this.tahun.getSelectedItem() == null
						|| LaporanRincianPembayaranSiswa.this.tahun.getSelectedItem().getValue() == null ? null
								: LaporanRincianPembayaranSiswa.this.tahun.getSelectedItem().getValue());
				Integer bulan = (Integer) (LaporanRincianPembayaranSiswa.this.bulan.getSelectedItem() == null
						|| LaporanRincianPembayaranSiswa.this.bulan.getSelectedItem().getValue() == null ? null
								: LaporanRincianPembayaranSiswa.this.bulan.getSelectedItem().getValue());

				Integer angkatan = (Integer) (LaporanRincianPembayaranSiswa.this.angkatan.getSelectedItem() == null
						|| LaporanRincianPembayaranSiswa.this.angkatan.getSelectedItem().getValue() == null ? null
								: LaporanRincianPembayaranSiswa.this.angkatan.getSelectedItem().getValue());

				List<List> datas = new ArrayList<List>();
				ArrayList arrayList = new ArrayList();
				arrayList.add("Rekap Pembayaran");
				datas.add(arrayList);

				arrayList = new ArrayList();
				arrayList.add("No.");
				arrayList.add("NIS");
				arrayList.add("Nama");
				arrayList.add("Total Tagihan");
				arrayList.add("Dibayar");
				arrayList.add("Belum Dibayar");
				datas.add(arrayList);

				String sql = "select max(case when c.nomor_induk is null then cs.nomor_induk else c.nomor_induk end) as nis,"
						+ "max(case when c.nama_siswa is null then cs.nama_siswa else c.nama_siswa end) as nama,"
						+ "sum(t.nominal) as nilai, "
						+ "sum(case when a.nominal is null then 0 else a.nominal end) as bayar "

						+ " from  sekolah.tagihan t  "
						+ " inner join sekolah.pengaturan_biaya p on (t.pengaturan_biaya = p.id and (p.aktif or p.aktif is null)) \n"
						+ " left join sekolah.pembayaran_siswa_detail a on (t.pembayaran_siswa_detail_id=a.id) \n"
						+ " left join sekolah.pembayaran_siswa b on (a.pembayaran_siswa_id = b.id)\n"
						+ " left join sekolah.siswa c on (t.siswa_id = c.id)\n"
						+ " left join sekolah.calon_siswa cs on (t.calon_siswa_id = cs.id)\n"
						+ " inner join sekolah.item_biaya_sekolah d on (d.id = t.item_biaya_id)\n"
						+ " left join bank_host b2 on (b2.id = b.bank_host_id)\n"
						+ " left join sekolah.akun_pembayaran_siswa b3 on (b3.id = b.akun_pembayaran_siswa_id)\n "
						+ " left join sekolah.deposit_siswa e on (e.pembayaran_siswa_id=b.id) "

						+ "where (c.aktif or c.aktif is null) and t.nominal > 0.1 "
						+ " and (b.tanggal is null or date(b.tanggal) between date('"
						+ Common.databaseDateFormat.get().format(mulai.getValue()) + "') and date('"
						+ Common.databaseDateFormat.get().format(sampai.getValue()) + "')) "

						+ (myAkunPembayaranSiswa == null ? ""
								: " and b.akun_pembayaran_siswa_id = " + myAkunPembayaranSiswa.getId() + " \n")
						+ (myJenisBiayaSekolah == null ? ""
								: " and b.jenis_biaya_id = " + myJenisBiayaSekolah.getId() + " \n")
						+ (myYayasan == null ? ""
								: " and (c.yayasan_id = " + myYayasan.getId() + " or cs.yayasan_id = "
										+ myYayasan.getId() + ")  \n")
						+ (mySekolah == null ? ""
								: " and (c.sekolah_id = " + mySekolah.getId() + " or cs.sekolah_id = "
										+ mySekolah.getId() + ")  \n")
						+ (siswa.getValue().trim().isEmpty() ? ""
								: " and (c.nomor_induk ilike '%" + siswa.getValue().trim()
										+ "%' or c.nama_siswa ilike '%" + siswa.getValue().trim()
										+ "%' or cs.nomor_induk ilike '%" + siswa.getValue().trim()
										+ "%' or cs.nama_siswa ilike '%" + siswa.getValue().trim() + "%') \n")

						+ (namaAkun.isEmpty() ? "" : " and (b3.nama_pembayaran ilike '%" + namaAkun + "%') \n")

						+ (myKelasSiswa == null ? "" : " and t.kelas_siswa_id = " + myKelasSiswa.getId() + " \n")
						+ (myAsramaSiswa == null ? "" : " and c.asrama_id = " + myAsramaSiswa.getId() + " \n")
						+ (angkatan == null ? ""
								: " and (c.tahun_masuk = " + angkatan + " or cs.tahun_masuk = " + angkatan + ") \n")
						+ (tahun == null ? "" : " and t.tahun = " + tahun + " \n")
						+ (bulan == null ? "" : " and t.bulan = " + bulan + " \n") + "\n"
						+ (ta == null ? "" : " and p.tahunajaran = '" + ta + "' \n");

				Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser != null && tbmuser.getOrangTua() != null
						&& !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
					String ids = "";
					for (Long id : tbmuser.getOrangTua().ambilAnakSiswa()) {
						ids += ids.isEmpty() ? id.toString() : "," + id;
					}

					sql += " and b.siswa_id in (" + ids + ") ";
				}

				sql += (tahun == null ? "" : " and b.tahun = " + tahun + " \n")
						+ (bulan == null ? "" : " and b.bulan = " + bulan + " \n") + "\n" + ""
						+ " group by (case when c.id is null then cs.id else c.id end) "
						+ " order by max(case when c.nomor_induk is null then cs.nomor_induk else c.nomor_induk end), max(case when c.nama_siswa is null then cs.nama_siswa else c.nama_siswa end)";
				Session session = HibernateUtil.currentSession();
				Collection<Map<String, Object>> dataArray = session.createSQLQuery(sql)
						.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).list();

				int index = 1;
				Double total = 0.0;
				Double totalbayar = 0.0;
				Double totalblmbayar = 0.0;
				for (Map<String, Object> map : dataArray) {
					Number nilai = (Number) map.get("nilai");
					Number bayar = (Number) map.get("bayar");
					Number blmbayar = (nilai == null ? 0.0 : nilai.doubleValue())
							- (bayar == null ? 0.0 : bayar.doubleValue());
					arrayList = new ArrayList();
					arrayList.add(index);
					arrayList.add(map.get("nis"));
					arrayList.add(map.get("nama"));
					arrayList.add(nilai == null ? "" : Common.numberFormat.get().format(nilai));
					arrayList.add(bayar == null ? "" : Common.numberFormat.get().format(bayar));
					arrayList.add(blmbayar == null ? "" : Common.numberFormat.get().format(blmbayar));
					total += nilai == null ? 0 : nilai.doubleValue();
					totalbayar += bayar == null ? 0 : bayar.doubleValue();
					totalblmbayar += blmbayar == null ? 0 : blmbayar.doubleValue();
					datas.add(arrayList);
					index++;
				}

				arrayList = new ArrayList();
				arrayList.add("");
				arrayList.add("");
				arrayList.add("Total");
				arrayList.add(Common.numberFormat.get().format(total));
				arrayList.add(Common.numberFormat.get().format(totalbayar));
				arrayList.add(Common.numberFormat.get().format(totalblmbayar));
				datas.add(arrayList);

				label.setAttribute("datas", datas);
				ais.action.report.helper.LoadingReportUtil.selesai(label);
			}
		}).start();

		SaptoUtil.displayWorksheet(label, "data_umum", center, 36 + ukuran.getValue());
	}
}

package ais.action.report.format1.sekolah;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.LayoutRegion;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataAsramaBanbox;
import ais.action.master.sekolah.helper.AmbilDataKelasSiswaBanbox;
import ais.action.master.sekolah.helper.TagihanUtilCalonSiswa;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.AsramaSiswa;
import ais.database.model.sekolah.ItemBiayaSekolah;
import ais.database.model.sekolah.JenisBiayaSekolah;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Tagihan;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.EcampusUtil;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanRekapPembayaranSiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Combobox yayasan;
	private Combobox sekolah;
	private Combobox jenisBiayaSekolah;
	private Textbox siswa;
	private Spreadsheet excelku;

	private AmbilDataKelasSiswaBanbox kelas;
	private AmbilDataAsramaBanbox asrama;

	private Siswa selectedSiswa = null;

	private Combobox angkatan;
	private Combobox angkatanSd;

	private Center center;

	private Combobox ta;

	private Combobox itemBiayaSekolah;

	private MyToolbarbuttonConfig printAmbil;

	public LaporanRekapPembayaranSiswa() throws Exception {
		super();
		init();
	}

	public LaporanRekapPembayaranSiswa(String title, String border, boolean closable) throws Exception {
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
			west.setWidth("150px");
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
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Item Pembayaran"));
		vbox.appendChild(itemBiayaSekolah = new Combobox());
		itemBiayaSekolah.setCols(5);
		itemBiayaSekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran"));
		vbox.appendChild(ta = new Combobox());
		ta.setCols(5);
		ta.setReadonly(true);

		Common.generateTahunAjaran(ta);

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

		vbox.appendChild(new MyLabelConfig(" sd "));

		vbox.appendChild(angkatanSd = new Combobox());
		angkatanSd.setReadonly(true);
		angkatanSd.setCols(5);

		for (int i = currTahun - 20; i < currTahun + 5; i++) {
			comboitem = new Comboitem(i + "");
			comboitem.setValue(i);
			angkatanSd.appendChild(comboitem);
		}

		comboitem = new Comboitem("Semua");
		comboitem.setValue(null);
		angkatanSd.appendChild(comboitem);
		angkatanSd.setSelectedItem(comboitem);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());

				Common.insertComboDanSemua(jenisBiayaSekolah, new String[] { "kode", "nama", "periode" }, "sekolah",
						JenisBiayaSekolah.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
				Common.selectComboItem(jenisBiayaSekolah, null);

				Common.insertComboDanSemua(itemBiayaSekolah, new String[] { "kode", "nama", "periode" }, "sekolah",
						ItemBiayaSekolah.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
				Common.selectComboItem(itemBiayaSekolah, null);

			}
		};

		sekolah.addEventListener("onChange", eventListener);
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

		row = new MyFormRow();
		row.setParent(rows);

		Vbox toolbar = new Vbox();
		toolbar.setParent(row);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(null);
			}
		});
		print.setParent(toolbar);

		printAmbil = new MyToolbarbuttonConfig("Ambil File", "/img/excel.png");
		printAmbil.setVisible(false);
		printAmbil.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					excelku.getBook().write(bout);
					bout.close();
					Filedownload.save(bout.toByteArray(),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
							"tagihan_dan_realisasi.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRekapPembayaranSiswa.java:323");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Rekap Pembayaran Siswa", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
						new String[] {
							"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
							"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});

				}
			}
		});
		printAmbil.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

	}

	@SuppressWarnings("rawtypes")
	private List<List> datas = null;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetak(Event event) {

		try {

			Common.clear(center);

			final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

			new Thread(new Runnable() {

				@Override
				public void run() {

					Sekolah mySekolah = selectedSiswa != null ? selectedSiswa.getSekolah()
							: (Sekolah) (sekolah.getSelectedItem() == null
									|| sekolah.getSelectedItem().getValue() == null ? null
											: sekolah.getSelectedItem().getValue());
					Yayasan myYayasan = selectedSiswa != null ? selectedSiswa.getYayasan()
							: (Yayasan) (yayasan.getSelectedItem() == null
									|| yayasan.getSelectedItem().getValue() == null ? null
											: yayasan.getSelectedItem().getValue());

					String nmSiswa = siswa.getValue().trim();

					String ta = (String) LaporanRekapPembayaranSiswa.this.ta.getSelectedItem().getValue();

					JenisBiayaSekolah myJenisBiayaSekolah = (JenisBiayaSekolah) (jenisBiayaSekolah
							.getSelectedItem() == null || jenisBiayaSekolah.getSelectedItem().getValue() == null ? null
									: jenisBiayaSekolah.getSelectedItem().getValue());

					ItemBiayaSekolah myItemBiayaSekolah = (ItemBiayaSekolah) (itemBiayaSekolah.getSelectedItem() == null
							|| itemBiayaSekolah.getSelectedItem().getValue() == null ? null
									: itemBiayaSekolah.getSelectedItem().getValue());

					KelasSiswa myKelasSiswa = (KelasSiswa) kelas.getAttribute("kelas");
					AsramaSiswa myAsramaSiswa = (AsramaSiswa) asrama.getAttribute("asrama");

					Integer angkatan = (Integer) (LaporanRekapPembayaranSiswa.this.angkatan.getSelectedItem() == null
							|| LaporanRekapPembayaranSiswa.this.angkatan.getSelectedItem().getValue() == null ? null
									: LaporanRekapPembayaranSiswa.this.angkatan.getSelectedItem().getValue());

					Integer angkatanSd = (Integer) (LaporanRekapPembayaranSiswa.this.angkatanSd
							.getSelectedItem() == null
							|| LaporanRekapPembayaranSiswa.this.angkatanSd.getSelectedItem().getValue() == null ? null
									: LaporanRekapPembayaranSiswa.this.angkatanSd.getSelectedItem().getValue());

//					Map<Long, GeneralValueObject> dataMhs = ConstantValues.ambilBerdasarClass(Siswa.class);

					Session sessionBaru = ais.action.report.Report.openNativeSession();
					List<Long> dataMhs = sessionBaru.createCriteria(Siswa.class)

							.setProjection(Projections.property("id"))

							.add(Restrictions.eq("aktif", true))

							.add(nmSiswa.isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.or(Restrictions.ilike("namaSiswa", nmSiswa, MatchMode.ANYWHERE),
											Restrictions.or(
													Restrictions.ilike("nomorIndukNasional", nmSiswa,
															MatchMode.ANYWHERE),
													Restrictions.ilike("nomorInduk", nmSiswa, MatchMode.ANYWHERE))))

							.add(mySekolah == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("sekolah", mySekolah))

							.add(myYayasan == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("yayasan", myYayasan))

							.list();

					System.out.println("dataMhs -> " + dataMhs.size());

					datas = new ArrayList<List>();

					Set<Long> siswas = new HashSet<Long>();

					for (Long generalValueObjectid : dataMhs) {
						Siswa siswa = (Siswa) ConstantValues.ambil(Siswa.class.getName(), generalValueObjectid);
						try {

							KelasSiswa kelasSiswa = myKelasSiswa == null ? null : Siswa.ambilKelas(siswa, ta);

							if (myKelasSiswa == null
									|| (kelasSiswa != null && myKelasSiswa.getId().equals(kelasSiswa.getId()))) {

								if (myAsramaSiswa == null || (siswa.getAsrama() != null
										&& myAsramaSiswa.getId().equals(siswa.getAsrama().getId()))) {

									if (angkatan == null || angkatan <= siswa.getTahunMasuk()) {

										if (angkatanSd == null || angkatanSd >= siswa.getTahunMasuk()) {

											siswas.add(generalValueObjectid);

										}
									}
								}
							}

						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sekolah/LaporanRekapPembayaranSiswa.java:440");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Pembayaran Siswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});
						}

					}

					List<JenisBiayaSekolah> jenisBiayaSekolahs = ConstantValues
							.simpleList(sessionBaru.createCriteria(Tagihan.class)

									.add(myItemBiayaSekolah == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("itemBiayaSekolah", myItemBiayaSekolah))

									.add(siswas.isEmpty() ? Restrictions.sqlRestriction("false")
											: Restrictions.in("siswa.id", siswas))

									.createAlias("nominalBiaya", "nominalBiaya")

									.createAlias("nominalBiaya.pengaturanBiaya", "pengaturanBiaya")

									.add(Restrictions.eq("pengaturanBiaya.aktif", true))

									.add(Restrictions.eq("pengaturanBiaya.tahunAjaran", ta))

									.add(myJenisBiayaSekolah == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("pengaturanBiaya.jenisBiayaSekolah", myJenisBiayaSekolah))

									.setProjection(Projections.groupProperty("pengaturanBiaya.jenisBiayaSekolah.id"))

									, JenisBiayaSekolah.class, false);

					System.out.println("Siswa -> " + siswas.size() + ", " + jenisBiayaSekolahs);

					for (JenisBiayaSekolah jenisBiayaSekolah : jenisBiayaSekolahs) {

						int nomor = 1;

						Double totalTagihan = 0.0;
						Double totalDibayar = 0.0;
						Double totalSisa = 0.0;

						if (!siswas.isEmpty()) {
							List<Tagihan> tagihanstemp = sessionBaru.createCriteria(Tagihan.class)

									.createAlias("siswa", "siswa").addOrder(Order.asc("siswa.namaSiswa"))

									.add(myItemBiayaSekolah == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("itemBiayaSekolah", myItemBiayaSekolah))

									.add(Restrictions.in("siswa.id", siswas))

									.createAlias("pengaturanBiaya", "pengaturanBiaya")

									.add(Restrictions.eq("pengaturanBiaya.aktif", true))

									.add(Restrictions.eq("pengaturanBiaya.jenisBiayaSekolah", jenisBiayaSekolah))

									.add(Restrictions.eq("pengaturanBiaya.tahunAjaran", ta))

									.list();

							System.out.println("jenisBiayaSekolah -> " + jenisBiayaSekolah.getNama()
									+ " -> tagihans 1 -> " + tagihanstemp.size());

							List<Tagihan> tagihans = TagihanUtilCalonSiswa.saring(tagihanstemp, sessionBaru);

							System.out.println("jenisBiayaSekolah -> " + jenisBiayaSekolah.getNama()
									+ " -> tagihans 2 -> " + tagihans.size());

//							Collections.sort(tagihans);

//							System.out.println("jenisBiayaSekolah -> " + jenisBiayaSekolah.getNama()
//									+ " -> tagihans 2 -> " + tagihans.size());

							if (!tagihans.isEmpty()) {

								try {
									List sub = new ArrayList();

									sub.add("");
									sub.add(jenisBiayaSekolah.getNama());
									sub.add("");
									sub.add("");
									sub.add("");
									sub.add("");
									sub.add("");
									sub.add("");
									sub.add("");
									sub.add("");
									sub.add("");
									sub.add("");
									sub.add("");

									datas.add(sub);

									sub = new ArrayList();

									sub.add("**No.");
									sub.add("**Nama");
									sub.add("**NIS");
									sub.add("**Kelas");
									sub.add("**Biaya");
									sub.add("**Bayar-ke");
									sub.add("**Bulan");
									sub.add("**Tahun");
									sub.add("**Tagihan");
									sub.add("**Realisasi");
									sub.add("**Tunggakan");
									sub.add("**Waktu Realisasi");
									datas.add(sub);

								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRekapPembayaranSiswa.java:549");
									// TODO: handle exception
								}

								for (Tagihan tagihan : tagihans) {

									if (((tagihan.getAktif() &&  !tagihan.ambilBukanTagihanData()) && !tagihan.getNominalBiaya().getBukanTagihan())) {
										List sub = new ArrayList();

										try {

											Siswa siswa = tagihan.getSiswa();

											label.setValue("Sedang memproses data " + siswa.toString());

											sub.add(nomor);
											sub.add(siswa.getNama());
											sub.add(siswa.getNomorInduk());
											sub.add(tagihan.getKelasSiswa() == null ? ""
													: tagihan.getKelasSiswa().getNama());

											String nama = tagihan.getItemBiayaSekolah() == null ? ""
													: tagihan.getItemBiayaSekolah().getNama();

											sub.add(nama);
											sub.add(tagihan.getBayarKe());
											sub.add(tagihan.getBulan());
											sub.add(tagihan.getTahun() == null ? "" : tagihan.getTahun().toString());

											sub.add(tagihan.getNominal());
											sub.add(tagihan.getPembayaranSiswaDetail() == null ? 0.0
													: tagihan.getPembayaranSiswaDetail().getNominal());
											Double sisa = tagihan.getNominal()
													- (tagihan.getPembayaranSiswaDetail() == null ? 0.0
															: tagihan.getPembayaranSiswaDetail().getNominal());

											sub.add(sisa);

											sub.add(tagihan.getPembayaranSiswaDetail() == null
													|| tagihan.getPembayaranSiswaDetail()

															.getPembayaranSiswa() == null
													|| tagihan.getPembayaranSiswaDetail().getPembayaranSiswa()
															.getTanggalBayar() == null
																	? ""
																	: Common.dateFormat3.get().format(tagihan
																			.getPembayaranSiswaDetail()
																			.getPembayaranSiswa().getTanggalBayar()));

											totalTagihan += tagihan.getNominal();
											totalDibayar += (tagihan.getPembayaranSiswaDetail() == null ? 0.0
													: tagihan.getPembayaranSiswaDetail().getNominal());
											totalSisa += sisa;

											System.out.println("sub =>" + sub);
											datas.add(sub);

											nomor++;

										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sekolah/LaporanRekapPembayaranSiswa.java:609");
											PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Pembayaran Siswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
												new String[] {
													"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
													"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
													"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
												});
										}
									}
								}
								tagihanstemp.clear();
								tagihanstemp = null;
								tagihans.clear();
								tagihans = null;

								List sub = new ArrayList();

								try {

									label.setValue("Sedang memproses data " + siswa.toString());

									sub.add("");
									sub.add("");
									sub.add("");
									sub.add("");
									sub.add("");
									sub.add("");
									sub.add("");

									sub.add("TOTAL");
									sub.add(totalTagihan);
									sub.add(totalDibayar);
									sub.add(totalSisa);
									sub.add("");

									datas.add(sub);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Pembayaran Siswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});
								}

								sub = new ArrayList();

								try {

									label.setValue("Sedang memproses data " + siswa.toString());

									sub.add("");
									sub.add("");
									sub.add("");
									sub.add("");
									sub.add("");
									sub.add("");
									sub.add("");
									sub.add("");
									sub.add("");
									sub.add("");
									sub.add("");
									sub.add("");

									datas.add(sub);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Pembayaran Siswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});
								}
							}
						}
					}

					sessionBaru.disconnect();
					sessionBaru.close();

					ais.action.report.Report.closeCurrentSessionQuietly();
					ais.action.report.helper.LoadingReportUtil.selesai(label);

				}
			}).start();

			final Timer timer = new Timer(1000);
			timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			timer.setRepeats(true);
			ais.action.report.helper.LoadingReportUtil.showBusy(label);
			timer.addEventListener("onTimer", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					ais.action.report.helper.LoadingReportUtil.showBusy(label);
					if (ais.action.report.helper.LoadingReportUtil.isSelesai(label)) {
						ais.action.report.helper.LoadingReportUtil.clearBusy();
						excelku = new ais.ui.util.MySpreadsheet();
						center.appendChild(excelku);
						EcampusUtil.tampilkan(datas, excelku);
						// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
						ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(excelku);
						printAmbil.setVisible(true);
						ais.action.report.helper.LoadingReportUtil.stopAndDetach(timer);
					}

				}
			});
			timer.start();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Rekap Pembayaran Siswa", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

}

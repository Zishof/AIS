package ais.action.master.recruitment.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.AMedia;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonEmail;
import ais.common.CommonMedia;
import ais.common.CommonPegawai;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CommonVO;
import ais.database.model.Jurusan;
import ais.database.model.file.LampiranLain;
import ais.database.model.recruitment.CalonPegawai;
import ais.database.model.recruitment.CalonPegawaiPunyaVerifikasiBerkas;
import ais.database.model.recruitment.GelombangPendaftaranPegawai;
import ais.database.model.recruitment.JadwalUjianPegawai;
import ais.database.model.recruitment.RuangGelombangPendaftaranPegawaiPegawai;
import ais.database.model.recruitment.RuangPegawai;
import ais.database.model.recruitment.VerifikasiKelengkapanCalonPegawai;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyIframe;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class CommonReportPegawai {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static boolean onCetakKartuUjianPegawai(CalonPegawai calonPegawai, String nomorUjian, File bio)
			throws Exception {

		try {

			Session session = HibernateUtil.currentSession();

			RuangGelombangPendaftaranPegawaiPegawai ruangGelombangPendaftaranPegawaiPegawai = (RuangGelombangPendaftaranPegawaiPegawai) session
					.createCriteria(RuangGelombangPendaftaranPegawaiPegawai.class)
					.add(Restrictions.eq("calonPegawai", calonPegawai)).setMaxResults(1).uniqueResult();

			if (ruangGelombangPendaftaranPegawaiPegawai == null && calonPegawai.getNoUjian() != null
					&& !calonPegawai.getNoUjian().trim().isEmpty()) {
				ruangGelombangPendaftaranPegawaiPegawai = CommonPegawai.dapatkanRuangUjian(calonPegawai);
			}

			if (ruangGelombangPendaftaranPegawaiPegawai == null) {
				MyMessageboxConfig.show(
						"Calon pegawai belum mendapatkan ruang ujian.. harap generate terlebih dahulu nomor ujian-nya",
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}

			if (calonPegawai != null) {
				session.refresh(calonPegawai);
				calonPegawai.setCetakKartu(1);
				Common.refreshUpdate(session, calonPegawai);
			}

			Map parameters = ais.common.HashMapGenerator.getRand();
			parameters.put("biodata_id", calonPegawai == null ? "" : calonPegawai.getId());
			parameters.put("nomorUjian", nomorUjian);

			calonPegawai.putPhoto(parameters);

			Report.generatePDFReport(Report.PDF, parameters, "recruitment/KartuUjianSpsbMandiri",
					ais.ui.util.WaktuUtil.getDate());

			File file = Report.generateDownloadReport(Report.PDF, parameters, "recruitment/KartuUjianSpsbMandiri", null,
					ais.ui.util.WaktuUtil.getDate());
			if (bio != null) {
				CommonEmail.infoDaftarUjianPegawai(calonPegawai, new File[] { file, bio });
			} else {
				CommonEmail.infoDaftarUjianPegawai(calonPegawai, new File[] { file });
			}
			return true;
		} catch (Exception e1) {
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/recruitment/helper/CommonReportPegawai.java:102");
			return false;
		}
	}

	@SuppressWarnings({})
	public static MyWindow onCetakAbsensiPegawaiFoto() throws Exception {
		return onCetakAbsensiPegawaiFoto(null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static MyWindow onCetakAbsensiPegawaiFoto(final RuangPegawai ruang) throws Exception {

		String tahunAkademikPenerimaanPegawaiBaru = Common
				.getKonfigurasi("tahunAkademikPenerimaanPegawaiBaru", Common.getCurrentTahunAkademik()).getNilai();

		final Combobox pilihanProdi = new Combobox();
		pilihanProdi.setReadonly(true);
		MyComboitemConfig comboitem = new MyComboitemConfig(" Pilihan 1");
		comboitem.setValue("1");
		pilihanProdi.appendChild(comboitem);

		comboitem = new MyComboitemConfig(" Pilihan 2");
		comboitem.setValue("2");
		pilihanProdi.appendChild(comboitem);

		comboitem = new MyComboitemConfig(" Pilihan 3");
		comboitem.setValue("3");
		pilihanProdi.appendChild(comboitem);

		comboitem = new MyComboitemConfig(" Pilihan 4");
		comboitem.setValue("4");
		pilihanProdi.appendChild(comboitem);

		comboitem = new MyComboitemConfig(" Pilihan 5");
		comboitem.setValue("5");
		pilihanProdi.appendChild(comboitem);

		pilihanProdi.setSelectedIndex(0);

		final Combobox pilihanJadwal = new Combobox();
		pilihanJadwal.setReadonly(true);

		final Combobox jurusan = new Combobox();
		Common.insertCombo(jurusan, "nama", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (!jurusan.getChildren().isEmpty()) {
			jurusan.setSelectedIndex(0);
		}
		jurusan.setReadonly(true);

		final MyCheckboxConfig gabungSemua = new MyCheckboxConfig("Gabung Semua");
		gabungSemua.setChecked(ruang == null);
		gabungSemua.setVisible(ruang == null);

		final Combobox tahunAkademik = Common.generateTahunAjaran(null);
		final Combobox searchGelombang = new Combobox();
		searchGelombang.setReadonly(true);
		tahunAkademik.setReadonly(true);
		Common.selectComboItem(tahunAkademik, tahunAkademikPenerimaanPegawaiBaru);

		List<JadwalUjianPegawai> jadwalUjianPegawais = HibernateUtil.currentSession()
				.createCriteria(JadwalUjianPegawai.class).createAlias("ujianPegawai", "ujianPegawai")
				.add(Restrictions.eq("ujianPegawai.tahunAkademik", tahunAkademik.getSelectedItem().getValue()))
				.add(ruang == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("ujianPegawai", ruang.getUjianPegawai()))
				.add(ruang == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.eq("gelombangPendaftaranPegawai", ruang.getGelombangPendaftaranPegawai()),
								Restrictions.isNull("gelombangPendaftaranPegawai")))
				.addOrder(Order.asc("waktuMulai")).list();
		for (JadwalUjianPegawai jadwalUjianPegawai : jadwalUjianPegawais) {
			String waktu = Common.dateFormat51.get().format(jadwalUjianPegawai.getWaktuMulai()) + " s.d "
					+ Common.timeFormat.get().format(jadwalUjianPegawai.getWaktuSampai()) + " / "
					+ jadwalUjianPegawai.getNama() + " / " + jadwalUjianPegawai.getUjianPegawai().getNama();
			comboitem = new MyComboitemConfig(waktu);
			comboitem.setValue(jadwalUjianPegawai);
			pilihanJadwal.appendChild(comboitem);
		}

		if (!pilihanJadwal.getChildren().isEmpty()) {
			pilihanJadwal.setSelectedIndex(0);
		}

		MyWindow window = new MyWindow("Laporan", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("90%");
		window.setWidth("900px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		final MyIframe include = new MyIframe();
		include.setParent(center);
		include.setWidth("100%");
		include.setHeight("100%");

		final String formatLaporan = "pdf";
		final String file = "AbsensiPegawaiPilihanProdi";
		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("ujian",
				ruang == null || ruang.getUjianPegawai() == null ? -1L : ruang.getUjianPegawai().getId());
		parameters.put("ruang", ruang == null || ruang.getId() == null ? -1 : ruang.getId());
		parameters.put("gelombang_pendaftaran",
				ruang == null || ruang.getUjianPegawai() == null
						|| ruang.getUjianPegawai().getGelombangPendaftaranPegawai() == null ? ""
								: ruang.getUjianPegawai().getGelombangPendaftaranPegawai().getNama());
		parameters.put("ket_ruang", ruang == null ? "" : ruang.getNama() + " ( " + ruang.getGedung().getNama() + " )");

		parameters.put("gelombangPendaftaranPegawai",
				ruang == null ? "" : ruang.getGelombangPendaftaranPegawai().getNama());

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						String bar = Common.getGeneratedBarCode();
						List<Map<String, Object>> maps = getDataAlbumPegawaiAdmin(ruang,
								"prodi" + pilihanProdi.getSelectedItem().getValue(),
								(JadwalUjianPegawai) (pilihanJadwal.getSelectedItem() == null ? null
										: pilihanJadwal.getSelectedItem().getValue()),
								(String) tahunAkademik.getSelectedItem().getValue(),
								(GelombangPendaftaranPegawai) (searchGelombang.getSelectedItem() == null ? null
										: searchGelombang.getSelectedItem().getValue()),
								(Jurusan) (jurusan.getSelectedItem() == null
										|| jurusan.getSelectedItem().getValue() == null ? null
												: jurusan.getSelectedItem().getValue()),
								gabungSemua.isChecked());
						File myfile = Report.generateFileReport(formatLaporan, parameters, file,
								ais.ui.util.WaktuUtil.getDate(), maps, bar, (Locale) arg0.getData());
						String path = "/report/" + myfile.getName();
						include.setSrc(path);
					}
				});
			}
		};

		South south = new South();
		south.setParent(borderlayout);
		Toolbar toolbar;
		south.appendChild(toolbar = new Toolbar());

		toolbar.appendChild(pilihanProdi);
		pilihanProdi.addEventListener("onChange", eventListener);

		toolbar.appendChild(jurusan);
		jurusan.addEventListener("onChange", eventListener);

		toolbar.appendChild(tahunAkademik);

		final EventListener eventListenerTahunAkademik = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(pilihanJadwal);
				List<JadwalUjianPegawai> jadwalUjianPegawais = HibernateUtil.currentSession()
						.createCriteria(JadwalUjianPegawai.class).createAlias("ujianPegawai", "ujianPegawai")
						.add(Restrictions.eq("ujianPegawai.tahunAkademik", tahunAkademik.getSelectedItem().getValue()))
						.add(searchGelombang.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("ujianPegawai.gelombangPendaftaranPegawai",
										searchGelombang.getSelectedItem().getValue()))
						.add(ruang == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("ujianPegawai", ruang.getUjianPegawai()))
						.add(ruang == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(
										Restrictions.eq("gelombangPendaftaranPegawai",
												ruang.getGelombangPendaftaranPegawai()),
										Restrictions.isNull("gelombangPendaftaranPegawai")))
						.addOrder(Order.asc("waktuMulai")).list();
				for (JadwalUjianPegawai jadwalUjianPegawai : jadwalUjianPegawais) {
					String waktu = Common.dateFormat51.get().format(jadwalUjianPegawai.getWaktuMulai()) + " s.d "
							+ Common.timeFormat.get().format(jadwalUjianPegawai.getWaktuSampai()) + " / "
							+ jadwalUjianPegawai.getNama() + " / " + jadwalUjianPegawai.getUjianPegawai().getNama();
					MyComboitemConfig comboitem = new MyComboitemConfig(waktu);
					comboitem.setValue(jadwalUjianPegawai);
					pilihanJadwal.appendChild(comboitem);
				}

				if (!pilihanJadwal.getChildren().isEmpty()) {
					pilihanJadwal.setSelectedIndex(0);
				} else {
					pilihanJadwal.setSelectedItem(null);
				}
				eventListener.onEvent(null);
			}
		};

		tahunAkademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.insertCombo(searchGelombang, "nama", "tahunAkademik", GelombangPendaftaranPegawai.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
								tahunAkademik.getSelectedItem() == null
										|| tahunAkademik.getSelectedItem().getValue() == null
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("tahunAkademik",
														tahunAkademik.getSelectedItem().getValue())));
				if (!searchGelombang.getChildren().isEmpty()) {
					searchGelombang.setSelectedIndex(0);
				}
				eventListenerTahunAkademik.onEvent(null);
			}
		});

		toolbar.appendChild(searchGelombang);

		toolbar.appendChild(pilihanJadwal);
		pilihanJadwal.addEventListener("onChange", eventListener);

		searchGelombang.addEventListener("onChange", eventListenerTahunAkademik);

		toolbar.appendChild(gabungSemua);
		gabungSemua.addEventListener("onClick", eventListener);

		MyButtonConfig toolbarbutton = new MyButtonConfig("XLS");
		toolbarbutton.setParent(toolbar);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						String bar = Common.getGeneratedBarCode();
						List<Map<String, Object>> maps = getDataAlbumPegawaiAdmin(ruang,
								"prodi" + pilihanProdi.getSelectedItem().getValue(),
								(JadwalUjianPegawai) (pilihanJadwal.getSelectedItem() == null ? null
										: pilihanJadwal.getSelectedItem().getValue()),
								(String) tahunAkademik.getSelectedItem().getValue(),
								(GelombangPendaftaranPegawai) (searchGelombang.getSelectedItem() == null ? null
										: searchGelombang.getSelectedItem().getValue()),
								(Jurusan) (jurusan.getSelectedItem() == null
										|| jurusan.getSelectedItem().getValue() == null ? null
												: jurusan.getSelectedItem().getValue()),
								gabungSemua.isChecked());
						File myfile = Report.generateFileReport(Report.XLS, parameters, file,
								ais.ui.util.WaktuUtil.getDate(), maps, bar, (Locale) arg0.getData());
						final AMedia amedia = new AMedia(file + ".xlsx", formatLaporan,
								"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
								new FileInputStream(myfile));
						Filedownload.save(amedia);
					}
				});

			}
		});

		Common.insertCombo(searchGelombang, "nama", "tahunAkademik", GelombangPendaftaranPegawai.class,
				Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("tahunAkademik", tahunAkademik.getSelectedItem().getValue())));
		if (!searchGelombang.getChildren().isEmpty()) {
			searchGelombang.setSelectedIndex(0);
		}
		eventListenerTahunAkademik.onEvent(null);

		return window;
	}

	@SuppressWarnings("unchecked")
	public static List<Map<String, Object>> getDataAlbumPegawaiAdmin(RuangPegawai ruang, String pilihan,
			JadwalUjianPegawai jadwalUjianPegawai, String tahunAkademik,
			GelombangPendaftaranPegawai gelombangPendaftaranPegawai, Jurusan jurusan, Boolean gabungSemua)
			throws Exception {
		String waktu = jadwalUjianPegawai == null ? ""
				: (Common.dateFormat51.get().format(jadwalUjianPegawai.getWaktuMulai()) + " s.d "
						+ Common.timeFormat.get().format(jadwalUjianPegawai.getWaktuSampai()));
		String tanggalUjian = jadwalUjianPegawai == null ? ""
				: Common.dateFormat2.get().format(jadwalUjianPegawai.getWaktuMulai());

		Session session = HibernateUtil.currentSession();
		List<RuangGelombangPendaftaranPegawaiPegawai> listPendaftaranWisuda;
		if (ruang != null) {
			listPendaftaranWisuda = session.createCriteria(RuangGelombangPendaftaranPegawaiPegawai.class)
					.createAlias("calonPegawai", "calonPegawai").add(Restrictions.ne("calonPegawai.nomorInduk", ""))
					.add(Restrictions.isNotNull("calonPegawai.nomorInduk"))
					.add(gelombangPendaftaranPegawai == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("calonPegawai.gelombangPendaftaranPegawai", gelombangPendaftaranPegawai))
					.add(Restrictions.eq("calonPegawai." + pilihan, jurusan))
					.addOrder(Order.asc("calonPegawai." + pilihan)).addOrder(Order.asc("calonPegawai.nomorInduk"))
					.add(Restrictions.eq("ruangPegawai", ruang)).list();
		} else {
			listPendaftaranWisuda = session.createCriteria(RuangGelombangPendaftaranPegawaiPegawai.class)
					.createAlias("ruangPegawai", "ruangPegawai").createAlias("calonPegawai", "calonPegawai")
					.add(Restrictions.ne("calonPegawai.nomorInduk", ""))
					.add(Restrictions.isNotNull("calonPegawai.nomorInduk"))
					.add(gelombangPendaftaranPegawai == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("calonPegawai.gelombangPendaftaranPegawai", gelombangPendaftaranPegawai))
					.add(gabungSemua || jadwalUjianPegawai == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("ruangPegawai.ujianPegawai", jadwalUjianPegawai.getUjianPegawai()))
					.add(Restrictions.eq("calonPegawai." + pilihan, jurusan))
					.addOrder(Order.asc("calonPegawai." + pilihan)).addOrder(Order.asc("calonPegawai.nomorInduk"))
					.list();

		}

		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		Iterator<?> itr = listPendaftaranWisuda.iterator();

		try {

			while (itr.hasNext()) {
				RuangGelombangPendaftaranPegawaiPegawai beanPendaftaranWisuda = (RuangGelombangPendaftaranPegawaiPegawai) itr
						.next();
				Map<String, Object> map = new java.util.HashMap<String, Object>();
				map.put("waktu", waktu);
				map.put("tanggalUjian", tanggalUjian);
				map.put("nama", beanPendaftaranWisuda.getCalonPegawai().getNama().toUpperCase());
				map.put("no_ujian", beanPendaftaranWisuda.getCalonPegawai().getNomorInduk());
				map.put("gelombangPendaftaranPegawai", beanPendaftaranWisuda.getRuangPegawai() == null
						|| beanPendaftaranWisuda.getRuangPegawai().getGelombangPendaftaranPegawai() == null ? ""
								: beanPendaftaranWisuda.getRuangPegawai().getGelombangPendaftaranPegawai().getNama());
				if (gabungSemua) {
					map.put("jadwal_ujian_psb", jadwalUjianPegawai.getNama());
				} else {
					map.put("jadwal_ujian_psb",
							(beanPendaftaranWisuda.getRuangPegawai() == null
									|| beanPendaftaranWisuda.getRuangPegawai().getUjianPegawai() == null ? ""
											: beanPendaftaranWisuda.getRuangPegawai().getUjianPegawai().getNama())
									+ " / " + (jadwalUjianPegawai == null ? "" : jadwalUjianPegawai.getNama()));
				}
				map.put("pilihanke", pilihan.replaceAll("prodi", ""));
				map.put("ruang", beanPendaftaranWisuda.getRuangPegawai() == null ? ""
						: beanPendaftaranWisuda.getRuangPegawai().getNama());
				map.put("gedung",
						beanPendaftaranWisuda.getRuangPegawai() == null
								|| beanPendaftaranWisuda.getRuangPegawai().getGedung() == null ? ""
										: beanPendaftaranWisuda.getRuangPegawai().getGedung().getNama());
				//
				// if (pilihan.equalsIgnoreCase("prodi1")) {
				// map.put("prodi",
				// beanPendaftaranWisuda.getCalonPegawai().getProdi1().getNama());
				// } else if (pilihan.equalsIgnoreCase("prodi2")) {
				// map.put("prodi",
				// beanPendaftaranWisuda.getCalonPegawai().getProdi2().getNama());
				// } else if (pilihan.equalsIgnoreCase("prodi3")) {
				// map.put("prodi",
				// beanPendaftaranWisuda.getCalonPegawai().getProdi3().getNama());
				// } else if (pilihan.equalsIgnoreCase("prodi4")) {
				// map.put("prodi",
				// beanPendaftaranWisuda.getCalonPegawai().getProdi4().getNama());
				// } else if (pilihan.equalsIgnoreCase("prodi5")) {
				// map.put("prodi",
				// beanPendaftaranWisuda.getCalonPegawai().getProdi5().getNama());
				// }

				map.put("ttl", beanPendaftaranWisuda.getCalonPegawai().getTempatLahir().toUpperCase() + " / "
						+ Common.dateFormat2.get().format(beanPendaftaranWisuda.getCalonPegawai().getTanggalLahir()));
				map.put("kelamin", beanPendaftaranWisuda.getCalonPegawai().getJenisKelamin());

				map.put("alamat", beanPendaftaranWisuda.getCalonPegawai().getAlamatPegawai());

				map.put("tahunakademik", tahunAkademik);
				map.put("gelombang_pendaftaran",
						beanPendaftaranWisuda.getRuangPegawai() == null
								|| beanPendaftaranWisuda.getRuangPegawai().getUjianPegawai() == null
								|| beanPendaftaranWisuda.getRuangPegawai().getUjianPegawai()
										.getGelombangPendaftaranPegawai() == null ? ""
												: beanPendaftaranWisuda.getRuangPegawai().getUjianPegawai()
														.getGelombangPendaftaranPegawai().getNama());
				beanPendaftaranWisuda.getCalonPegawai().putPhoto(map);

				maps.add(map);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return maps;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onCetakAbsensiPegawai(RuangPegawai ruangPegawai) throws Exception {

		if (ruangPegawai.getUjianPegawai() == null) {
			return;
		}

		List<Map> maps = new ArrayList<Map>();
		List<String> names = new ArrayList<String>();
		List<String> fileNames = new ArrayList<String>();

		for (int i = 1; i <= ruangPegawai.getUjianPegawai().getJumlahHariUjian(); i++) {
			final Map parameters = ais.common.HashMapGenerator.getRand();
			parameters.put("ruang", ruangPegawai.getId());
			parameters.put("tanggalKe", i);
			maps.add(parameters);
			names.add("Hari ke-" + i);
			fileNames.add("AbsensiPegawai_day1");
		}

		Report.generatePDFReport(Report.PDF, maps.toArray(new Map[] {}), fileNames.toArray(new String[] {}),
				names.toArray(new String[] {}), ais.ui.util.WaktuUtil.getDate());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onCetakPernyataanOrtu(CalonPegawai calonPegawai) throws Exception {

		List<Map> maps = new ArrayList<Map>();

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("calon", calonPegawai.getId());
		parameters.put("ortu", calonPegawai.getNamaAyah());
		parameters.put("pegawai", calonPegawai.getNama());
		parameters.put("ibu", calonPegawai.getNamaIbu());

		maps.add(parameters);

		Report.generatePDFReport(Report.PDF, parameters, "recruitment/surat_peryataan_ortu",
				ais.ui.util.WaktuUtil.getDate(), maps);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onCetakPernyataanPegawai(CalonPegawai calonPegawai) throws Exception {

		List<Map> maps = new ArrayList<Map>();

		final Map parameters = ais.common.HashMapGenerator.getRand();

		parameters.put("calon", calonPegawai.getId());
		parameters.put("ttl", calonPegawai.getTempatLahir() + ", " + (calonPegawai.getTanggalLahir() == null ? ""
				: Common.dateFormat2.get().format(calonPegawai.getTanggalLahir())));
		parameters.put("ortu", calonPegawai.getNamaAyah());
		parameters.put("pegawai", calonPegawai.getNama());
		parameters.put("ibu", calonPegawai.getNamaIbu());

		parameters.put("alamat", calonPegawai.getAlamatPegawai());

		maps.add(parameters);

		Report.generatePDFReport(Report.PDF, parameters, "recruitment/surat_peryataan_pegawai",
				ais.ui.util.WaktuUtil.getDate(), maps);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onCetakVerifikasiPegawai(RuangPegawai ruangPegawai) throws Exception {

		if (ruangPegawai.getUjianPegawai() == null) {
			return;
		}

		List<Map> maps = new ArrayList<Map>();

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("ruang", ruangPegawai.getId());
		maps.add(parameters);

		Report.generatePDFReport(Report.PDF, parameters, "ValidasiPegawai", ais.ui.util.WaktuUtil.getDate());
	}

	public static File onCetakCalonPegawai(final CalonPegawai calonPegawai) throws Exception {

		Map<String, Serializable> parameters = ais.common.HashMapGenerator.getRandStringSerializable();
		parameters.put("biodata_id", calonPegawai.getId());

		calonPegawai.putPhoto(parameters);

		List<String> temporary1 = new ArrayList<String>();
		temporary1.add("Nomor Pendaftaran" + "<=>" + calonPegawai.getNomorInduk());

		temporary1.add("Tanggal Pendaftaran" + "<=>" + (calonPegawai.getTanggalPendaftaran() == null ? ""
				: Common.dateFormat6.get().format(calonPegawai.getTanggalPendaftaran())));
		temporary1.add("Gelombang Pendaftaran" + "<=>" + (calonPegawai.getGelombangPendaftaranPegawai() == null ? ""
				: calonPegawai.getGelombangPendaftaranPegawai().getNama()));

		List<String> temporary2 = new ArrayList<String>();
		temporary2.add("Nama Lengkap" + "<=>" + calonPegawai.getNamaPegawai());
		temporary2.add("Tempat Lahir" + "<=>" + calonPegawai.getTempatLahir());
		temporary2.add("Tanggal Lahir" + "<=>" + (calonPegawai.getTanggalLahir() == null ? ""
				: Common.dateFormat2.get().format(calonPegawai.getTanggalLahir())));
		temporary2.add("Email" + "<=>" + calonPegawai.getAlamatEmail());
		temporary2.add("Jenis Kelamin" + "<=>" + calonPegawai.getJenisKelamin());

		temporary2.add("Agama" + "<=>" + (calonPegawai.getAgama() == null ? "" : calonPegawai.getAgama().getNama()));
		temporary2.add("Kewarganegaraan" + "<=>" + calonPegawai.getKewarganegaraan());
		temporary2.add("Asal Negara" + "<=>"
				+ (calonPegawai.getNegara() == null ? "" : calonPegawai.getNegara().getNamaNegara()));
		temporary2.add("Alamat Rumah" + "<=>" + calonPegawai.getAlamatPegawai());
		temporary2.add("Dusun / Kampung" + "<=>" + calonPegawai.getDusunCalon());
		temporary2.add("RT" + "<=>" + calonPegawai.getRt());
		temporary2.add("RW" + "<=>" + calonPegawai.getRw());
		temporary2.add("Kode Pos" + "<=>" + calonPegawai.getKodePos());
		temporary2.add("Kelurahan / Desa" + "<=>" + calonPegawai.getKelurahanCalon());
		temporary2.add("Kecamatan" + "<=>"
				+ (calonPegawai.getKecamatanCalon() == null ? "" : calonPegawai.getKecamatanCalon().getNama()));

		temporary2.add("Kota/Kabupaten" + "<=>"
				+ (calonPegawai.getKotaCalon() == null ? "" : calonPegawai.getKotaCalon().getNama()));
		temporary2.add("Propinsi" + "<=>"
				+ (calonPegawai.getPropinsiCalon() == null ? "" : calonPegawai.getPropinsiCalon().getNama()));
		temporary2.add("Telepon (atau HP) / No. WA " + "<=>" + calonPegawai.getTeleponPegawai());

		List<String> temporary3 = new ArrayList<String>();
		temporary3.add("Nama Pendidikan Sebelumnya" + "<=>" + calonPegawai.getSekolahAsal());
		temporary3.add("Alamat Pendidikan Sebelumnya" + "<=>" + calonPegawai.getAlamatSekolahAsal());

		List<String> temporary4 = new ArrayList<String>();
		temporary4.add("Alamat Orang Tua" + "<=>" + calonPegawai.getAlamatOrangTua());
		temporary4.add("Telepon Rumah Orang Tua" + "<=>" + calonPegawai.getTeleponOrangTua());

		List<Map<String, String>> maps = new ArrayList<Map<String, String>>();
		for (String key : temporary1) {
			Map<String, String> map = new java.util.HashMap<String, String>();
			map.put("grup", "");
			String[] value = key.split("<=>");
			String lab = value.length > 0 ? value[0].trim() : "";
			String val = value.length > 1 ? value[1].trim() : "";
			if (!val.trim().isEmpty() && !val.trim().equalsIgnoreCase("null")) {
				map.put("label", lab);
				map.put("nilai", val);
				maps.add(map);
			}
		}
		for (String key : temporary2) {
			Map<String, String> map = new java.util.HashMap<String, String>();
			map.put("grup", "I. Data Calon Pegawai");
			String[] value = key.split("<=>");
			String lab = value.length > 0 ? value[0].trim() : "";
			String val = value.length > 1 ? value[1].trim() : "";
			if (!val.trim().isEmpty() && !val.trim().equalsIgnoreCase("null")) {
				map.put("label", lab);
				map.put("nilai", val);
				maps.add(map);
			}
		}
		for (String key : temporary3) {
			Map<String, String> map = new java.util.HashMap<String, String>();
			map.put("grup", "II. Data Pendidikan Asal");
			String[] value = key.split("<=>");
			String lab = value.length > 0 ? value[0].trim() : "";
			String val = value.length > 1 ? value[1].trim() : "";
			if (!val.trim().isEmpty() && !val.trim().equalsIgnoreCase("null")) {
				map.put("label", lab);
				map.put("nilai", val);
				maps.add(map);
			}
		}
		for (String key : temporary4) {
			Map<String, String> map = new java.util.HashMap<String, String>();
			map.put("grup", "III. Data Orang Tua/Wali");
			String[] value = key.split("<=>");
			String lab = value.length > 0 ? value[0].trim() : "";
			String val = value.length > 1 ? value[1].trim() : "";
			if (!val.trim().isEmpty() && !val.trim().equalsIgnoreCase("null")) {
				map.put("label", lab);
				map.put("nilai", val);
				maps.add(map);
			}
		}

		for (CommonVO commonVO : calonPegawai.ambilDataParameterTambahan()) {
			String lbl = commonVO.getName();
			String url = commonVO.getName2();
			String val = commonVO.getName1();
			try {
				String[] d = StringUtils.split(val, ":");
				if (Common.isNumber(d[1].trim())) {
					val = d[0];
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/recruitment/helper/CommonReportPegawai.java:674");
			}

			if ((!val.trim().isEmpty() && !val.trim().equalsIgnoreCase("null")) || !url.trim().isEmpty()) {

				String[] param = lbl.split("->");
				Map<String, String> map = new java.util.HashMap<String, String>();
				map.put("grup", param[0]);
				map.put("label", param.length > 1 ? param[1] : "");
				map.put("nilai", val);
				map.put("url", url);
				maps.add(map);
			}
		}

		Session session = HibernateUtil.currentSession();

		GelombangPendaftaranPegawai gel = (GelombangPendaftaranPegawai) session
				.createCriteria(GelombangPendaftaranPegawai.class)
				.add(Restrictions.idEq(calonPegawai.getGelombangPendaftaranPegawai().getId())).uniqueResult();
		Set<VerifikasiKelengkapanCalonPegawai> verifikasiKelengkapanCalonPegawais = new TreeSet<VerifikasiKelengkapanCalonPegawai>(
				gel.getVerifikasiKelengkapanCalonPegawais());
		for (VerifikasiKelengkapanCalonPegawai verifikasiKelengkapanCalonPegawai : verifikasiKelengkapanCalonPegawais) {
			if (verifikasiKelengkapanCalonPegawai.getAktif()) {
				CalonPegawaiPunyaVerifikasiBerkas berkas = (CalonPegawaiPunyaVerifikasiBerkas) session
						.createCriteria(CalonPegawaiPunyaVerifikasiBerkas.class)
						.add(Restrictions.eq("verifikasiKelengkapanCalonPegawai", verifikasiKelengkapanCalonPegawai))
						.add(Restrictions.eq("calonPegawai", calonPegawai)).setMaxResults(1).uniqueResult();

				if (berkas == null) {
					berkas = new CalonPegawaiPunyaVerifikasiBerkas();
					berkas.setCalonPegawai(calonPegawai);
					berkas.setVerifikasiKelengkapanCalonPegawai(verifikasiKelengkapanCalonPegawai);
					Common.refreshSaveOrUpdate(session, berkas);
				}

				Map<String, String> map = new java.util.HashMap<String, String>();
				map.put("grup", "Verifikasi Kelengkapan Berkas");
				map.put("label", berkas.getVerifikasiKelengkapanCalonPegawai().getNama());
				map.put("nilai", (berkas.getVerified() ? "Telah sesuai" : "Belum Diverifikasi")
						+ (berkas.getKeterangan().isEmpty() ? "" : ", " + berkas.getKeterangan()));

				LampiranLain lampiranLain = LampiranLain.ambil(berkas.getId(),
						CalonPegawaiPunyaVerifikasiBerkas.class.getName());

				if (lampiranLain != null) {
					String url = lampiranLain.getGdrive() != null ? lampiranLain.forwardGDriveUrl()
							: CommonMedia.getFile(lampiranLain.getId(), LampiranLain.class.getName());
					map.put("url", url);
				}

				maps.add(map);
			}
		}

		parameters.put("nama", calonPegawai.getNama());

		System.out.println("maps => " + maps);

		File file = Report.generatePDFReport(Report.PDF, parameters, "recruitment/Biodata_Calon_Pegawai",
				ais.ui.util.WaktuUtil.getDate(), maps);
		return file;

	}

	@SuppressWarnings({})
	public static boolean onCetakKartuUjianPegawai(CalonPegawai calonPegawai, String nomorUjian) throws Exception {
		return onCetakKartuUjianPegawai(calonPegawai, nomorUjian, null);
	}

}

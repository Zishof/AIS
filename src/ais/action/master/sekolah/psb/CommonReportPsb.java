package ais.action.master.sekolah.psb;

import java.io.File;
import java.io.FileInputStream;
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
import ais.common.CommonPSB;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CommonVO;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.CalonSiswaPunyaVerifikasiBerkas;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.JadwalUjianPSB;
import ais.database.model.sekolah.RuangGelombangPendaftaranPsbPSB;
import ais.database.model.sekolah.RuangPSB;
import ais.database.model.sekolah.VerifikasiKelengkapanCalonSiswa;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyIframe;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class CommonReportPsb {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static boolean onCetakKartuUjianPSB(CalonSiswa calonSiswa, String nomorUjian, File bio) throws Exception {

		try {

			if (!VerifikasiKelengkapanCalonSiswa.checkBerkas(calonSiswa)) {
				return false;
			}

			if (Common.bolehKonfigurasi("calon_siswa_harus_melakukan_pembayaran_sebelum_bisa_login")) {

				if (!GelombangPendaftaranPsb.chekSyaratBayar(calonSiswa)) {
					return false;
				}

			}

			Session session = HibernateUtil.currentSession();
			RuangGelombangPendaftaranPsbPSB ruangGelombangPendaftaranPsbPSB = (RuangGelombangPendaftaranPsbPSB) session
					.createCriteria(RuangGelombangPendaftaranPsbPSB.class)
					.add(Restrictions.eq("calonSiswa", calonSiswa)).setMaxResults(1).uniqueResult();

			if (ruangGelombangPendaftaranPsbPSB == null && calonSiswa.getNoUjian() != null
					&& !calonSiswa.getNoUjian().trim().isEmpty()) {
				ruangGelombangPendaftaranPsbPSB = CommonPSB.dapatkanRuangUjian(calonSiswa);
			}

			if (ruangGelombangPendaftaranPsbPSB == null) {
				MyMessageboxConfig.show(
						"Calon siswa belum mendapatkan ruang ujian.. harap generate terlebih dahulu nomor ujian-nya",
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}

			Map parameters = ais.common.HashMapGenerator.getRand();
			parameters.put("biodata_id", calonSiswa == null ? "" : calonSiswa.getId());
			parameters.put("sekolah_id", calonSiswa == null ? -1L : calonSiswa.getSekolah().getId());
			parameters.put("nomorUjian", nomorUjian);
			GelombangPendaftaranPsb gel = calonSiswa.getGelombangPendaftaranPsb();
			LampiranLain kop = LampiranLain.ambil(gel.getId(), LampiranLain.KOP_GELOMBANG_PSB);
			if (kop != null) {
				try {
					parameters.put("kop_file", kop.ambilFile().getAbsolutePath());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/psb/CommonReportPsb.java:97");
					// TODO: handle exception
				}
			}

			calonSiswa.putPhoto(parameters);

			Report.generatePDFReport(Report.PDF, parameters, "sekolah/KartuUjianSpsbMandiri",
					ais.ui.util.WaktuUtil.getDate());

			File file = Report.generateDownloadReport(Report.PDF, parameters, "sekolah/KartuUjianSpsbMandiri", null,
					ais.ui.util.WaktuUtil.getDate());
			if (bio != null) {
				CommonEmail.infoDaftarUjianSiswa(calonSiswa, new File[] { file, bio });
			} else {
				CommonEmail.infoDaftarUjianSiswa(calonSiswa, new File[] { file });
			}
			return true;
		} catch (Exception e1) {
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sekolah/psb/CommonReportPsb.java:116");
			return false;
		}
	}

	@SuppressWarnings({})
	public static MyWindow onCetakAbsensiPSBFoto() throws Exception {
		return onCetakAbsensiPSBFoto(null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static MyWindow onCetakAbsensiPSBFoto(final RuangPSB ruang) throws Exception {

		String tahunAkademikPenerimaanSiswaBaru = Common
				.getKonfigurasi("tahunAkademikPenerimaanSiswaBaru", Common.getCurrentTahunAkademik()).getNilai();

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
		Common.selectComboItem(tahunAkademik, tahunAkademikPenerimaanSiswaBaru);

		List<JadwalUjianPSB> jadwalUjianPSBs = HibernateUtil.currentSession().createCriteria(JadwalUjianPSB.class)
				.createAlias("ujianPSB", "ujianPSB")
				.add(Restrictions.eq("ujianPSB.tahunAkademik", tahunAkademik.getSelectedItem().getValue()))
				.add(ruang == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("ujianPSB", ruang.getUjianPSB()))
				.add(ruang == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.eq("gelombangPendaftaranPsb", ruang.getGelombangPendaftaranPsb()),
								Restrictions.isNull("gelombangPendaftaranPsb")))
				.addOrder(Order.asc("waktuMulai")).list();
		for (JadwalUjianPSB jadwalUjianPSB : jadwalUjianPSBs) {
			String waktu = Common.dateFormat51.get().format(jadwalUjianPSB.getWaktuMulai()) + " s.d "
					+ Common.timeFormat.get().format(jadwalUjianPSB.getWaktuSampai()) + " / " + jadwalUjianPSB.getNama()
					+ " / " + jadwalUjianPSB.getUjianPSB().getNama();
			comboitem = new MyComboitemConfig(waktu);
			comboitem.setValue(jadwalUjianPSB);
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
		final String file = "AbsensiPSBPilihanProdi";
		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("ujian", ruang == null || ruang.getUjianPSB() == null ? -1L : ruang.getUjianPSB().getId());
		parameters.put("ruang", ruang == null || ruang.getId() == null ? -1 : ruang.getId());
		parameters.put("tahunakademik", ruang == null ? "" : ruang.getTahunAkademik());
		parameters.put("gelombang_pendaftaran",
				ruang == null || ruang.getUjianPSB() == null || ruang.getUjianPSB().getGelombangPendaftaranPsb() == null
						? ""
						: ruang.getUjianPSB().getGelombangPendaftaranPsb().getNama());
		parameters.put("ket_ruang", ruang == null ? "" : ruang.getNama() + " ( " + ruang.getGedung().getNama() + " )");

		parameters.put("gelombangPendaftaranPsb", ruang == null ? "" : ruang.getGelombangPendaftaranPsb().getNama());

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						String bar = Common.getGeneratedBarCode();
						List<Map<String, Object>> maps = getDataAlbumPSBAdmin(ruang,
								"prodi" + pilihanProdi.getSelectedItem().getValue(),
								(JadwalUjianPSB) (pilihanJadwal.getSelectedItem() == null ? null
										: pilihanJadwal.getSelectedItem().getValue()),
								(String) tahunAkademik.getSelectedItem().getValue(),
								(GelombangPendaftaranPsb) (searchGelombang.getSelectedItem() == null ? null
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
				List<JadwalUjianPSB> jadwalUjianPSBs = HibernateUtil.currentSession()
						.createCriteria(JadwalUjianPSB.class).createAlias("ujianPSB", "ujianPSB")
						.add(Restrictions.eq("ujianPSB.tahunAkademik", tahunAkademik.getSelectedItem().getValue()))
						.add(searchGelombang.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("ujianPSB.gelombangPendaftaranPsb",
										searchGelombang.getSelectedItem().getValue()))
						.add(ruang == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("ujianPSB", ruang.getUjianPSB()))
						.add(ruang == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(
										Restrictions.eq("gelombangPendaftaranPsb", ruang.getGelombangPendaftaranPsb()),
										Restrictions.isNull("gelombangPendaftaranPsb")))
						.addOrder(Order.asc("waktuMulai")).list();
				for (JadwalUjianPSB jadwalUjianPSB : jadwalUjianPSBs) {
					String waktu = Common.dateFormat51.get().format(jadwalUjianPSB.getWaktuMulai()) + " s.d "
							+ Common.timeFormat.get().format(jadwalUjianPSB.getWaktuSampai()) + " / "
							+ jadwalUjianPSB.getNama() + " / " + jadwalUjianPSB.getUjianPSB().getNama();
					MyComboitemConfig comboitem = new MyComboitemConfig(waktu);
					comboitem.setValue(jadwalUjianPSB);
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
				Common.insertCombo(searchGelombang, "nama", "tahunAkademik", GelombangPendaftaranPsb.class,
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
						List<Map<String, Object>> maps = getDataAlbumPSBAdmin(ruang,
								"prodi" + pilihanProdi.getSelectedItem().getValue(),
								(JadwalUjianPSB) (pilihanJadwal.getSelectedItem() == null ? null
										: pilihanJadwal.getSelectedItem().getValue()),
								(String) tahunAkademik.getSelectedItem().getValue(),
								(GelombangPendaftaranPsb) (searchGelombang.getSelectedItem() == null ? null
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

		Common.insertCombo(searchGelombang, "nama", "tahunAkademik", GelombangPendaftaranPsb.class,
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
	public static List<Map<String, Object>> getDataAlbumPSBAdmin(RuangPSB ruang, String pilihan,
			JadwalUjianPSB jadwalUjianPSB, String tahunAkademik, GelombangPendaftaranPsb gelombangPendaftaranPsb,
			Jurusan jurusan, Boolean gabungSemua) throws Exception {
		String waktu = jadwalUjianPSB == null ? ""
				: (Common.dateFormat51.get().format(jadwalUjianPSB.getWaktuMulai()) + " s.d "
						+ Common.timeFormat.get().format(jadwalUjianPSB.getWaktuSampai()));
		String tanggalUjian = jadwalUjianPSB == null ? "" : Common.dateFormat2.get().format(jadwalUjianPSB.getWaktuMulai());

		Session session = HibernateUtil.currentSession();
		List<RuangGelombangPendaftaranPsbPSB> listPendaftaranWisuda;
		if (ruang != null) {
			listPendaftaranWisuda = session.createCriteria(RuangGelombangPendaftaranPsbPSB.class)
					.createAlias("calonSiswa", "calonSiswa").add(Restrictions.ne("calonSiswa.nomorInduk", ""))
					.add(Restrictions.isNotNull("calonSiswa.nomorInduk"))
					.add(gelombangPendaftaranPsb == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("calonSiswa.gelombangPendaftaranPsb", gelombangPendaftaranPsb))
					.add(Restrictions.eq("calonSiswa." + pilihan, jurusan)).addOrder(Order.asc("calonSiswa." + pilihan))
					.addOrder(Order.asc("calonSiswa.nomorInduk")).add(Restrictions.eq("ruangPSB", ruang)).list();
		} else {
			listPendaftaranWisuda = session.createCriteria(RuangGelombangPendaftaranPsbPSB.class)
					.createAlias("ruangPSB", "ruangPSB").createAlias("calonSiswa", "calonSiswa")
					.add(Restrictions.ne("calonSiswa.nomorInduk", ""))
					.add(Restrictions.isNotNull("calonSiswa.nomorInduk"))
					.add(gelombangPendaftaranPsb == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("calonSiswa.gelombangPendaftaranPsb", gelombangPendaftaranPsb))
					.add(gabungSemua || jadwalUjianPSB == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("ruangPSB.ujianPSB", jadwalUjianPSB.getUjianPSB()))
					.add(Restrictions.eq("calonSiswa." + pilihan, jurusan)).addOrder(Order.asc("calonSiswa." + pilihan))
					.addOrder(Order.asc("calonSiswa.nomorInduk")).list();

		}

		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		Iterator<?> itr = listPendaftaranWisuda.iterator();

		try {

			while (itr.hasNext()) {
				RuangGelombangPendaftaranPsbPSB beanPendaftaranWisuda = (RuangGelombangPendaftaranPsbPSB) itr.next();
				Map<String, Object> map = new java.util.HashMap<String, Object>();
				map.put("waktu", waktu);
				map.put("tanggalUjian", tanggalUjian);
				map.put("nama", beanPendaftaranWisuda.getCalonSiswa().getNama().toUpperCase());
				map.put("no_ujian", beanPendaftaranWisuda.getCalonSiswa().getNomorInduk());
				map.put("gelombangPendaftaranPsb",
						beanPendaftaranWisuda.getRuangPSB() == null
								|| beanPendaftaranWisuda.getRuangPSB().getGelombangPendaftaranPsb() == null ? ""
										: beanPendaftaranWisuda.getRuangPSB().getGelombangPendaftaranPsb().getNama());
				if (gabungSemua) {
					map.put("jadwal_ujian_psb", jadwalUjianPSB.getNama());
				} else {
					map.put("jadwal_ujian_psb",
							(beanPendaftaranWisuda.getRuangPSB() == null
									|| beanPendaftaranWisuda.getRuangPSB().getUjianPSB() == null ? ""
											: beanPendaftaranWisuda.getRuangPSB().getUjianPSB().getNama())
									+ " / " + (jadwalUjianPSB == null ? "" : jadwalUjianPSB.getNama()));
				}
				map.put("pilihanke", pilihan.replaceAll("prodi", ""));
				map.put("ruang", beanPendaftaranWisuda.getRuangPSB() == null ? ""
						: beanPendaftaranWisuda.getRuangPSB().getNama());
				map.put("gedung",
						beanPendaftaranWisuda.getRuangPSB() == null
								|| beanPendaftaranWisuda.getRuangPSB().getGedung() == null ? ""
										: beanPendaftaranWisuda.getRuangPSB().getGedung().getNama());
				//
				// if (pilihan.equalsIgnoreCase("prodi1")) {
				// map.put("prodi",
				// beanPendaftaranWisuda.getCalonSiswa().getProdi1().getNama());
				// } else if (pilihan.equalsIgnoreCase("prodi2")) {
				// map.put("prodi",
				// beanPendaftaranWisuda.getCalonSiswa().getProdi2().getNama());
				// } else if (pilihan.equalsIgnoreCase("prodi3")) {
				// map.put("prodi",
				// beanPendaftaranWisuda.getCalonSiswa().getProdi3().getNama());
				// } else if (pilihan.equalsIgnoreCase("prodi4")) {
				// map.put("prodi",
				// beanPendaftaranWisuda.getCalonSiswa().getProdi4().getNama());
				// } else if (pilihan.equalsIgnoreCase("prodi5")) {
				// map.put("prodi",
				// beanPendaftaranWisuda.getCalonSiswa().getProdi5().getNama());
				// }

				map.put("ttl", beanPendaftaranWisuda.getCalonSiswa().getTempatLahir().toUpperCase() + " / "
						+ Common.dateFormat2.get().format(beanPendaftaranWisuda.getCalonSiswa().getTanggalLahir()));
				map.put("kelamin", beanPendaftaranWisuda.getCalonSiswa().getJenisKelamin());

				map.put("alamat", beanPendaftaranWisuda.getCalonSiswa().getAlamatSiswa());

				map.put("tahunakademik", tahunAkademik);
				map.put("gelombang_pendaftaran", beanPendaftaranWisuda.getRuangPSB() == null
						|| beanPendaftaranWisuda.getRuangPSB().getUjianPSB() == null
						|| beanPendaftaranWisuda.getRuangPSB().getUjianPSB().getGelombangPendaftaranPsb() == null ? ""
								: beanPendaftaranWisuda.getRuangPSB().getUjianPSB().getGelombangPendaftaranPsb()
										.getNama());

				beanPendaftaranWisuda.getCalonSiswa().putPhoto(map);

				maps.add(map);
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return maps;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onCetakAbsensiPSB(RuangPSB ruangPSB) throws Exception {

		if (ruangPSB.getUjianPSB() == null) {
			return;
		}

		List<Map> maps = new ArrayList<Map>();
		List<String> names = new ArrayList<String>();
		List<String> fileNames = new ArrayList<String>();

		for (int i = 1; i <= ruangPSB.getUjianPSB().getJumlahHariUjian(); i++) {
			final Map parameters = ais.common.HashMapGenerator.getRand();
			parameters.put("ruang", ruangPSB.getId());
			parameters.put("tanggalKe", i);
			parameters.put("sekolah_id", ruangPSB.getGelombangPendaftaranPsb().getSekolah().getId());
			maps.add(parameters);
			names.add("Hari ke-" + i);
			fileNames.add("AbsensiPSB_day1");
		}

		Report.generatePDFReport(Report.PDF, maps.toArray(new Map[] {}), fileNames.toArray(new String[] {}),
				names.toArray(new String[] {}), ais.ui.util.WaktuUtil.getDate());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onCetakPernyataanOrtu(CalonSiswa calonSiswa) throws Exception {

		List<Map> maps = new ArrayList<Map>();

		Map parameters = ais.common.HashMapGenerator.getRand();
		GelombangPendaftaranPsb gel = calonSiswa.getGelombangPendaftaranPsb();
		LampiranLain kop = LampiranLain.ambil(gel.getId(), LampiranLain.KOP_GELOMBANG_PSB);
		if (kop != null) {
			try {
				parameters.put("kop_file", kop.ambilFile().getAbsolutePath());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/psb/CommonReportPsb.java:528");
				// TODO: handle exception
			}
		}
		parameters.put("calon", calonSiswa.getId());
		parameters.put("ta", calonSiswa.getGelombangPendaftaranPsb().getTahunAjaran());
		parameters.put("sekolah", calonSiswa.getSekolah().getNama());
		parameters.put("ortu", calonSiswa.getNamaAyah());
		parameters.put("siswa", calonSiswa.getNama());
		parameters.put("ibu", calonSiswa.getNamaIbu());
		parameters.put("jurusan",
				calonSiswa.getPenjurusanSekolah() == null ? "" : calonSiswa.getPenjurusanSekolah().getNama());
		parameters.put("sekolah_asal", calonSiswa.getSekolahAsal());

		maps.add(parameters);

		Report.generatePDFReport(Report.PDF, parameters, "sekolah/surat_peryataan_ortu",
				ais.ui.util.WaktuUtil.getDate(), maps);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onCetakPernyataanSiswa(CalonSiswa calonSiswa) throws Exception {

		List<Map> maps = new ArrayList<Map>();

		Map parameters = ais.common.HashMapGenerator.getRand();
		GelombangPendaftaranPsb gel = calonSiswa.getGelombangPendaftaranPsb();
		LampiranLain kop = LampiranLain.ambil(gel.getId(), LampiranLain.KOP_GELOMBANG_PSB);
		if (kop != null) {
			try {
				parameters.put("kop_file", kop.ambilFile().getAbsolutePath());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/psb/CommonReportPsb.java:560");
				// TODO: handle exception
			}
		}
		parameters.put("kelas",
				calonSiswa.getSiswa() == null || calonSiswa.getSiswa().getKelas() == null ? "(belum ada kelas)"
						: calonSiswa.getSiswa().getKelas().getNama());
		parameters.put("calon", calonSiswa.getId());
		parameters.put("ttl", calonSiswa.getTempatLahir() + ", " + (calonSiswa.getTanggalLahir() == null ? ""
				: Common.dateFormat2.get().format(calonSiswa.getTanggalLahir())));
		parameters.put("ta", calonSiswa.getGelombangPendaftaranPsb().getTahunAjaran());
		parameters.put("sekolah", calonSiswa.getSekolah().getNama());
		parameters.put("ortu", calonSiswa.getNamaAyah());
		parameters.put("siswa", calonSiswa.getNama());
		parameters.put("ibu", calonSiswa.getNamaIbu());
		parameters.put("jurusan",
				calonSiswa.getPenjurusanSekolah() == null ? "" : calonSiswa.getPenjurusanSekolah().getNama());
		parameters.put("sekolah_asal", calonSiswa.getSekolahAsal());
		parameters.put("alamat", calonSiswa.getAlamatSiswa());

		parameters.put("nama_kepala_sekolah", calonSiswa.getSekolah().getNamaKepalaSekolah());
		parameters.put("nip_kepala_sekolah", calonSiswa.getSekolah().getNipKepalaSekolah());

		maps.add(parameters);

		Report.generatePDFReport(Report.PDF, parameters, "sekolah/surat_peryataan_siswa",
				ais.ui.util.WaktuUtil.getDate(), maps);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onCetakVerifikasiPSB(RuangPSB ruangPSB) throws Exception {

		if (ruangPSB.getUjianPSB() == null) {
			return;
		}

		List<Map> maps = new ArrayList<Map>();

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("ruang", ruangPSB.getId());
		parameters.put("sekolah_id", ruangPSB.getGelombangPendaftaranPsb().getSekolah().getId());
		maps.add(parameters);

		Report.generatePDFReport(Report.PDF, parameters, "ValidasiPSB", ais.ui.util.WaktuUtil.getDate());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static File onCetakCalonSiswa(final CalonSiswa calonSiswa) throws Exception {

		@SuppressWarnings({})
		Map parameters = ais.common.HashMapGenerator.getRand();
		GelombangPendaftaranPsb gel = calonSiswa.getGelombangPendaftaranPsb();
		LampiranLain kop = LampiranLain.ambil(gel.getId(), LampiranLain.KOP_GELOMBANG_PSB);
		if (kop != null) {
			try {
				parameters.put("kop_file", kop.ambilFile().getAbsolutePath());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/psb/CommonReportPsb.java:617");
				// TODO: handle exception
			}
		}

		parameters.put("biodata_id", calonSiswa.getId());
		parameters.put("sekolah_id", calonSiswa.getSekolah().getId());

		calonSiswa.putPhoto(parameters);

		List<String> temporary1 = new ArrayList<String>();
		temporary1.add("Nomor Pendaftaran" + "<=>" + calonSiswa.getNomorInduk());

		temporary1.add("Tanggal Pendaftaran" + "<=>" + (calonSiswa.getTanggalPendaftaran() == null ? ""
				: Common.dateFormat6.get().format(calonSiswa.getTanggalPendaftaran())));
		temporary1.add("Tahun Akademik" + "<=>" + calonSiswa.getGelombangPendaftaranPsb().getTahunAjaran());
		temporary1.add("Gelombang Pendaftaran" + "<=>" + (calonSiswa.getGelombangPendaftaranPsb() == null ? ""
				: calonSiswa.getGelombangPendaftaranPsb().getNama()));

		List<String> temporary2 = new ArrayList<String>();
		temporary2.add("Nama Lengkap" + "<=>" + calonSiswa.getNamaSiswa());
		temporary2.add("Tempat Lahir" + "<=>" + calonSiswa.getTempatLahir());
		temporary2.add("Tanggal Lahir" + "<=>" + (calonSiswa.getTanggalLahir() == null ? ""
				: Common.dateFormat2.get().format(calonSiswa.getTanggalLahir())));
		temporary2.add("Email" + "<=>" + calonSiswa.getAlamatEmail());
		temporary2.add("Jenis Kelamin" + "<=>" + calonSiswa.getJenisKelamin());

		temporary2.add("Agama" + "<=>" + (calonSiswa.getAgama() == null ? "" : calonSiswa.getAgama().getNama()));
		temporary2.add("Kewarganegaraan" + "<=>" + calonSiswa.getKewarganegaraan());
		temporary2.add(
				"Asal Negara" + "<=>" + (calonSiswa.getNegara() == null ? "" : calonSiswa.getNegara().getNamaNegara()));
		temporary2.add("Alamat Rumah" + "<=>" + calonSiswa.getAlamatSiswa());
		temporary2.add("Dusun / Kampung" + "<=>" + calonSiswa.getDusunCalon());
		temporary2.add("RT" + "<=>" + calonSiswa.getRt());
		temporary2.add("RW" + "<=>" + calonSiswa.getRw());
		temporary2.add("Kode Pos" + "<=>" + calonSiswa.getKodePos());
		temporary2.add("Kelurahan / Desa" + "<=>" + calonSiswa.getKelurahanCalon());
		temporary2.add("Kecamatan" + "<=>"
				+ (calonSiswa.getKecamatanCalon() == null ? "" : calonSiswa.getKecamatanCalon().getNama()));

		temporary2.add("Kota/Kabupaten" + "<=>"
				+ (calonSiswa.getKotaCalon() == null ? "" : calonSiswa.getKotaCalon().getNama()));
		temporary2.add("Propinsi" + "<=>"
				+ (calonSiswa.getPropinsiCalon() == null ? "" : calonSiswa.getPropinsiCalon().getNama()));
		temporary2.add("Telepon (atau HP) / No. WA " + "<=>" + calonSiswa.getTeleponSiswa());

		List<String> temporary3 = new ArrayList<String>();
		temporary3.add("Nama Pendidikan Sebelumnya" + "<=>" + calonSiswa.getSekolahAsal());
		temporary3.add("Alamat Pendidikan Sebelumnya" + "<=>" + calonSiswa.getAlamatSekolahAsal());

		List<String> temporary4 = new ArrayList<String>();
		temporary4.add("Nama Ayah" + "<=>" + calonSiswa.getNamaAyah());
		temporary2.add("Tempat Lahir Ayah" + "<=>" + calonSiswa.getTempatLahirAyah());
		temporary2.add("Tanggal Lahir" + "<=>" + (calonSiswa.getTanggalLahirAyah() == null ? ""
				: Common.dateFormat2.get().format(calonSiswa.getTanggalLahirAyah())));
		temporary4.add("Pendidikan Ayah" + "<=>"
				+ (calonSiswa.getPendidikanAyah() == null ? "" : calonSiswa.getPendidikanAyah().getNama()));
		temporary4.add("Pekerjaan Ayah" + "<=>"
				+ (calonSiswa.getPekerjaanAyah() == null ? "" : calonSiswa.getPekerjaanAyah().getNama()));
		temporary4.add("Pendapatan Ayah" + "<=>"
				+ (calonSiswa.getPenghasilanAyah() == null ? ""
						: "Rp. " + Common.numberFormat.get().format(calonSiswa.getPenghasilanAyah().getBatasBawah())
								+ " - Rp. "
								+ Common.numberFormat.get().format(calonSiswa.getPenghasilanAyah().getBatasAtas())));

		temporary4.add("Telepon / HP Ayah" + "<=>" + calonSiswa.getHp1ayah()
				+ (calonSiswa.getHp2ayah().isEmpty() ? "" : ", " + calonSiswa.getHp2ayah())
				+ (calonSiswa.getHp3ayah().isEmpty() ? "" : ", " + calonSiswa.getHp3ayah()));

		temporary4.add("Nama Ibu" + "<=>" + calonSiswa.getNamaIbu());
		temporary2.add("Tempat Lahir Ibu" + "<=>" + calonSiswa.getTempatLahirIbu());
		temporary2.add("Tanggal Lahir" + "<=>" + (calonSiswa.getTanggalLahirIbu() == null ? ""
				: Common.dateFormat2.get().format(calonSiswa.getTanggalLahirIbu())));
		temporary4.add("Pendidikan Ibu" + "<=>"
				+ (calonSiswa.getPendidikanIbu() == null ? "" : calonSiswa.getPendidikanIbu().getNama()));
		temporary4.add("Pekerjaan Ibu" + "<=>"
				+ (calonSiswa.getPekerjaanIbu() == null ? "" : calonSiswa.getPekerjaanIbu().getNama()));
		temporary4.add("Pendapatan Ibu" + "<=>"
				+ (calonSiswa.getPenghasilanIbu() == null ? ""
						: "Rp. " + Common.numberFormat.get().format(calonSiswa.getPenghasilanIbu().getBatasBawah())
								+ " - Rp. "
								+ Common.numberFormat.get().format(calonSiswa.getPenghasilanIbu().getBatasAtas())));

		temporary4.add("Telepon / HP Ibu" + "<=>" + calonSiswa.getHp1ibu()
				+ (calonSiswa.getHp2ibu().isEmpty() ? "" : ", " + calonSiswa.getHp2ibu())
				+ (calonSiswa.getHp3ibu().isEmpty() ? "" : ", " + calonSiswa.getHp3ibu()));

		temporary4.add("Nama Wali" + "<=>" + calonSiswa.getNamaWali());
		temporary2.add("Tempat Lahir Wali" + "<=>" + calonSiswa.getTempatLahirWali());
		temporary2.add("Tanggal Lahir" + "<=>" + (calonSiswa.getTanggalLahirWali() == null ? ""
				: Common.dateFormat2.get().format(calonSiswa.getTanggalLahirWali())));
		temporary4.add("Pendidikan Wali" + "<=>"
				+ (calonSiswa.getPendidikanWali() == null ? "" : calonSiswa.getPendidikanWali().getNama()));
		temporary4.add("Pekerjaan Wali" + "<=>"
				+ (calonSiswa.getPekerjaanWali() == null ? "" : calonSiswa.getPekerjaanWali().getNama()));
		temporary4.add("Pendapatan Wali" + "<=>"
				+ (calonSiswa.getPenghasilanWali() == null ? ""
						: "Rp. " + Common.numberFormat.get().format(calonSiswa.getPenghasilanWali().getBatasBawah())
								+ " - Rp. "
								+ Common.numberFormat.get().format(calonSiswa.getPenghasilanWali().getBatasAtas())));

		temporary4.add("Telepon / HP Wali" + "<=>" + calonSiswa.getHp1wali()
				+ (calonSiswa.getHp2wali().isEmpty() ? "" : ", " + calonSiswa.getHp2wali())
				+ (calonSiswa.getHp3wali().isEmpty() ? "" : ", " + calonSiswa.getHp3wali()));

		temporary4.add("Alamat Orang Tua" + "<=>" + calonSiswa.getAlamatOrangTua());
		temporary4.add("Telepon Rumah Orang Tua" + "<=>" + calonSiswa.getTeleponOrangTua());

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
			map.put("grup", "I. Data Calon Siswa");
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

		for (CommonVO commonVO : calonSiswa.ambilDataParameterTambahan()) {
			String lbl = commonVO.getName();
			String url = commonVO.getName2();
			String val = commonVO.getName1();
			try {
				String[] d = StringUtils.split(val, ":");
				if (Common.isNumber(d[1].trim())) {
					val = d[0];
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/psb/CommonReportPsb.java:784");
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

		gel = (GelombangPendaftaranPsb) session.createCriteria(GelombangPendaftaranPsb.class)
				.add(Restrictions.idEq(calonSiswa.getGelombangPendaftaranPsb().getId())).uniqueResult();
		Set<VerifikasiKelengkapanCalonSiswa> verifikasiKelengkapanCalonSiswas = new TreeSet<VerifikasiKelengkapanCalonSiswa>(
				gel.getVerifikasiKelengkapanCalonSiswas());
		for (VerifikasiKelengkapanCalonSiswa verifikasiKelengkapanCalonSiswa : verifikasiKelengkapanCalonSiswas) {
			if (verifikasiKelengkapanCalonSiswa.getAktif()) {
				CalonSiswaPunyaVerifikasiBerkas berkas = (CalonSiswaPunyaVerifikasiBerkas) session
						.createCriteria(CalonSiswaPunyaVerifikasiBerkas.class)
						.add(Restrictions.eq("verifikasiKelengkapanCalonSiswa", verifikasiKelengkapanCalonSiswa))
						.add(Restrictions.eq("calonSiswa", calonSiswa)).setMaxResults(1).uniqueResult();

				if (berkas == null) {
					berkas = new CalonSiswaPunyaVerifikasiBerkas();
					berkas.setCalonSiswa(calonSiswa);
					berkas.setVerifikasiKelengkapanCalonSiswa(verifikasiKelengkapanCalonSiswa);
					Common.refreshSaveOrUpdate(session, berkas);
				}

				Map<String, String> map = new java.util.HashMap<String, String>();
				map.put("grup", "Verifikasi Kelengkapan Berkas");
				map.put("label", berkas.getVerifikasiKelengkapanCalonSiswa().getNama());
				map.put("nilai", (berkas.getVerified() ? "Telah sesuai" : "Belum Diverifikasi")
						+ (berkas.getKeterangan().isEmpty() ? "" : ", " + berkas.getKeterangan()));

				LampiranLain lampiranLain = LampiranLain.ambil(berkas.getId(),
						CalonSiswaPunyaVerifikasiBerkas.class.getName());

				if (lampiranLain != null) {
					String url = lampiranLain.getGdrive() != null ? lampiranLain.forwardGDriveUrl()
							: CommonMedia.getFile(lampiranLain.getId(), LampiranLain.class.getName());
					map.put("url", url);
				}

				maps.add(map);
			}
		}

		parameters.put("nama", calonSiswa.getNama());
		Common.insertProperty(CalonSiswa.class, calonSiswa, parameters, "");
		System.out.println("maps => " + maps);
		parameters.put("maps", maps);

		File file = Report.generatePDFReport(Report.PDF, parameters, "sekolah/Biodata_Calon_Siswa",
				ais.ui.util.WaktuUtil.getDate(), maps);
		return file;

	}

	@SuppressWarnings({})
	public static boolean onCetakKartuUjianPSB(CalonSiswa calonSiswa, String nomorUjian) throws Exception {
		return onCetakKartuUjianPSB(calonSiswa, nomorUjian, null);
	}

}

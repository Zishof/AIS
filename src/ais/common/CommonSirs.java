package ais.common;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.LongType;
import org.hibernate.type.Type;
import org.zkoss.calendar.Calendars;
import org.zkoss.calendar.impl.SimpleCalendarEvent;
import org.zkoss.calendar.impl.SimpleCalendarModel;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.action.master.sirs.CetakKartuPasienAction;
import ais.action.master.sirs.util.CommonPendaftaranUtil;
import ais.action.master.sirs.util.CommonTarifItem;
import ais.action.report.Report;
import ais.action.report.format1.sirs.helper.PemeriksaanReportHelper;
import ais.action.report.helper.CommonReport;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.AlatMedis;
import ais.database.model.sirs.AlatMedisDiagnosaPenyakit;
import ais.database.model.sirs.Asuransi;
import ais.database.model.sirs.CetakKartuPasien;
import ais.database.model.sirs.DetailTransaksiLayanan;
import ais.database.model.sirs.DiagnosaPenyakit;
import ais.database.model.sirs.Diskon;
import ais.database.model.sirs.DiskonDetail;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.HargaJualItem;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.JadwalDokter;
import ais.database.model.sirs.JenisPasien;
import ais.database.model.sirs.JenisTindakan;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.Komunitas;
import ais.database.model.sirs.Minggu;
import ais.database.model.sirs.PajakDetail;
import ais.database.model.sirs.PajakMedis;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pemeriksaan;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.Poly;
import ais.database.model.sirs.Racikan;
import ais.database.model.sirs.RacikanDetail;
import ais.database.model.sirs.ResepDetail;
import ais.database.model.sirs.Shift;
import ais.database.model.sirs.Tindakan;
import ais.database.model.sirs.TindakanDiagnosaPenyakit;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

public class CommonSirs {

	@SuppressWarnings("unchecked")
	public static List<Shift> getCurrentShift(Lokasi lokasi, Boolean allTime) {

		Double sekarang = Double.parseDouble(Common.timeFormat2.get().format(new Date()));

		String sql = sekarang + " between mulaid and sampaid";

		Criterion crit = Restrictions.sqlRestriction(sql);

		List<Shift> shifts = HibernateUtil.currentSession().createCriteria(Shift.class)
				.add(Restrictions.eq("lokasi", lokasi)).add(allTime ? Restrictions.sqlRestriction("true") : crit)
				.list();

		if (!allTime && shifts.isEmpty()) {
			sekarang += +24.0;
			sql = sekarang + " between mulaid and sampaid";

			crit = Restrictions.sqlRestriction(sql);

			shifts = HibernateUtil.currentSession().createCriteria(Shift.class).add(Restrictions.eq("lokasi", lokasi))
					.add(allTime ? Restrictions.sqlRestriction("true") : crit).list();
		}

		return shifts;
	}

	public static void initLokasiDanShift(Lokasi myLokasi, Rows rows, final EventListener myEventListener)
			throws Exception {
		final Label tokodata = new Label();
		final Row rowToko = new Row();
		Row row = new Row();
		row.setAttribute("hide", "no");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Lokasi")));
		final Combobox lokasi;
		row.appendChild(lokasi = new Combobox());
		Common.insertCombo(lokasi, "nama", Lokasi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.selectComboItem(lokasi, myLokasi);
		lokasi.setDisabled(myLokasi != null);
		lokasi.setWidth("90%");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Lokasi myLokasi = (Lokasi) (lokasi.getSelectedItem() == null ? Common.getCurrentLokasi()
						: lokasi.getSelectedItem().getValue());

				rowToko.setVisible(myLokasi != null && myLokasi.getToko() != null);

				myEventListener.onEvent(new Event("", lokasi, new Object[] { myLokasi }));
				tokodata.setValue(myLokasi == null ? "" : myLokasi.getToko().getNama());
			}
		};
		lokasi.addEventListener("onChange", eventListener);

		Common.selectComboItem(lokasi, myLokasi);
		eventListener.onEvent(null);

		lokasi.setReadonly(true);
		if (lokasi.getSelectedItem() == null && lokasi.getChildren().size() == 1) {
			lokasi.setSelectedIndex(0);
			myLokasi = (Lokasi) (lokasi.getSelectedItem() == null ? Common.getCurrentLokasi()
					: lokasi.getSelectedItem().getValue());

			myEventListener.onEvent(new Event("", lokasi, new Object[] { myLokasi }));
		}

		rowToko.setStyle("border:0px;background: transparent;");
		rowToko.setParent(rows);
		rowToko.appendChild(new Label(ais.common.Common.getBahasaConfig("Toko")));
		rowToko.appendChild(tokodata);

	}

	@SuppressWarnings("deprecation")
	public static void initLokasiDanShift(Lokasi myLokasi, Shift myShift, Rows rows,
			final EventListener myEventListener) throws Exception {
		Row row = new Row();
		row.setAttribute("hide", "no");
		ais.ui.util.ZkCompat.setSpans(row, "4");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Html("<hr>"));

		row = new Row();
		row.setAttribute("hide", "no");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Lokasi")));

		final Combobox lokasi;
		row.appendChild(lokasi = new Combobox());
		Common.insertCombo(lokasi, "nama", Lokasi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Shift")));
		final Combobox shift;
		row.appendChild(shift = new Combobox());

		Common.selectComboItem(lokasi, myLokasi);
		lokasi.setDisabled(myLokasi != null);
		lokasi.setWidth("90%");
		shift.setWidth("90%");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Lokasi myLokasi = (Lokasi) (lokasi.getSelectedItem() == null ? Common.getCurrentLokasi()
						: lokasi.getSelectedItem().getValue());

				shift.setSelectedItem(null);
				Common.clear(shift);

				if (myLokasi != null) {
					Common.insertComboItems(shift, "nama", "jenisShift", CommonSirs.getCurrentShift(myLokasi, false));

					if (!shift.getChildren().isEmpty()) {
						shift.setSelectedIndex(0);
					}
				}

				Shift myShift = (Shift) (shift.getSelectedItem() == null ? null : shift.getSelectedItem().getValue());

				myEventListener.onEvent(new Event("", lokasi, new Object[] { myLokasi, myShift }));
			}
		};
		lokasi.addEventListener("onChange", eventListener);

		EventListener shiftEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Lokasi myLokasi = (Lokasi) (lokasi.getSelectedItem() == null ? null
						: lokasi.getSelectedItem().getValue());
				Shift myShift = (Shift) (shift.getSelectedItem() == null ? null : shift.getSelectedItem().getValue());
				myEventListener.onEvent(new Event("", lokasi, new Object[] { myLokasi, myShift }));

			}
		};

		shift.addEventListener("onChange", shiftEventListener);

		if (myShift != null) {
			lokasi.setDisabled(true);
			shift.setDisabled(true);
			Common.insertCombo(shift, "nama", Shift.class);

			Common.selectComboItem(lokasi, myLokasi);
			Common.selectComboItem(shift, myShift);
		} else {
			eventListener.onEvent(null);
		}

		lokasi.setReadonly(true);
		if (lokasi.getSelectedItem() == null && lokasi.getChildren().size() == 1) {
			lokasi.setSelectedIndex(0);
			myLokasi = (Lokasi) (lokasi.getSelectedItem() == null ? null : lokasi.getSelectedItem().getValue());
			myShift = (Shift) (shift.getSelectedItem() == null ? null : shift.getSelectedItem().getValue());
			myEventListener.onEvent(new Event("", lokasi, new Object[] { myLokasi, myShift }));
		}

		shiftEventListener.onEvent(null);
	}

	public static void initCalendarModel(Lokasi myLokasi, Dokter myDokter, Poly myPoly, Calendars calendars) {
		initCalendarModel(myLokasi, myDokter, myPoly, calendars, false, null);
	}

	@SuppressWarnings("unchecked")
	public static void initCalendarModel(Lokasi myLokasi, Dokter myDokter, Poly myPoly, Calendars calendars,
			Boolean sesuaikan, String jenis) {

		Session session = HibernateUtil.currentSession();

		SimpleCalendarModel cm = new SimpleCalendarModel();
		Calendar current = Calendar.getInstance();
		current.setTime(calendars.getBeginDate());

		int minjam = 23;
		int maxjam = 0;

		while (current.getTime().before(calendars.getEndDate())) {

			String currHari = Common.haris[current.get(Calendar.DAY_OF_WEEK) - 1];
			List<JadwalDokter> jadwalDokter = session.createCriteria(JadwalDokter.class)
					.createAlias("poly", "poly", Criteria.LEFT_JOIN)
					.add(jenis == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("poly.jenis", jenis))

					.add(Restrictions.eq("hari", currHari))
					.add(Restrictions.or(Restrictions.isNull("jadwalDokterDimulai"),
							Restrictions.le("jadwalDokterDimulai", current.getTime())))
					.add(Restrictions.or(Restrictions.isNull("jadwalDokterSampai"),
							Restrictions.ge("jadwalDokterSampai", current.getTime())))
					.add(myLokasi == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("lokasi", myLokasi))
					.add(myDokter == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("dokter", myDokter))
					.add(myPoly == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("poly", myPoly))

					.list();
			for (JadwalDokter myJadwalDokter : jadwalDokter) {
				cm.add(CommonSirs.createSimpleCalendarEvent(myJadwalDokter, current));

				if (myJadwalDokter.getShift() != null && sesuaikan) {
					Calendar dimulai = Calendar.getInstance();
					dimulai.setTime(myJadwalDokter.getShift().getMulai());

					if (dimulai.get(Calendar.HOUR_OF_DAY) <= minjam) {
						minjam = dimulai.get(Calendar.HOUR_OF_DAY);
					}

					Calendar sampai = Calendar.getInstance();
					sampai.setTime(myJadwalDokter.getShift().getSampai());

					if (sampai.get(Calendar.HOUR_OF_DAY) >= maxjam) {
						maxjam = sampai.get(Calendar.HOUR_OF_DAY);
					}
				}

			}

			current.set(Calendar.DATE, current.get(Calendar.DATE) + 1);
		}

		calendars.setModel(cm);

		if (sesuaikan && cm.size() > 0 && minjam < maxjam) {
			try {
				calendars.setBeginTime(minjam);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonSirs.java:307");
			}
			try {
				calendars.setEndTime(maxjam);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonSirs.java:311");
			}
		}
	}

	public static SimpleCalendarEvent createSimpleCalendarEvent(JadwalDokter myJadwalDokter, Calendar current) {
		Calendar m = Calendar.getInstance();
		m.setTime(myJadwalDokter.getShift().getMulai());
		Calendar s = Calendar.getInstance();
		s.setTime(myJadwalDokter.getShift().getSampai());

		Calendar dimulai = Calendar.getInstance();
		dimulai.setTime(current.getTime());
		dimulai.set(Calendar.HOUR_OF_DAY, m.get(Calendar.HOUR_OF_DAY));
		dimulai.set(Calendar.MINUTE, m.get(Calendar.MINUTE));
		dimulai.set(Calendar.SECOND, m.get(Calendar.SECOND));

		Calendar sampai = Calendar.getInstance();
		sampai.setTime(current.getTime());
		sampai.set(Calendar.HOUR_OF_DAY, s.get(Calendar.HOUR_OF_DAY));
		sampai.set(Calendar.MINUTE, s.get(Calendar.MINUTE));
		sampai.set(Calendar.SECOND, s.get(Calendar.SECOND));

		SimpleCalendarEvent sce = new SimpleCalendarEvent();
		sce.setLocked(true);
		sce.setTitle(myJadwalDokter.getId() + "");

		if (dimulai.getTime().after(sampai.getTime())) {
			sce.setBeginDate(dimulai.getTime());
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(sampai.getTime());
			calendar.set(Calendar.HOUR_OF_DAY, 23);
			calendar.set(Calendar.MINUTE, 59);
			calendar.set(Calendar.SECOND, 59);
			sce.setEndDate(calendar.getTime());
		} else {
			sce.setBeginDate(dimulai.getTime());
			sce.setEndDate(sampai.getTime());
		}

		if (myJadwalDokter.getWarna() != null) {
			try {
				String[] colors = ((String) myJadwalDokter.getWarna()).split(",");
				sce.setHeaderColor(colors[0]);
				sce.setContentColor(colors[1]);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonSirs.java:356");
			}
		}

		Dokter dokter = myJadwalDokter.getDokter();
		Lokasi lokasi = myJadwalDokter.getLokasi();
		String poly = myJadwalDokter.getPoly() == null ? "" : myJadwalDokter.getPoly().getNama();
		sce.setContent((dokter == null ? "" : (dokter == null ? "" : dokter.getNama())) + " -> " + poly + " -> "
				+ (lokasi == null ? "" : (lokasi == null ? "" : lokasi.getNama())) + " "
				+ (myJadwalDokter.getJadwalDokterDimulai() == null ? ""
						: " -> Berlaku " + Common.dateFormat2.get().format(myJadwalDokter.getJadwalDokterDimulai()))

				+ (myJadwalDokter.getJadwalDokterSampai() == null ? ""
						: " s.d " + Common.dateFormat2.get().format(myJadwalDokter.getJadwalDokterSampai()))

				+ "");
		return sce;
	}

	@SuppressWarnings({})
	public static void onCetakKartuPasien(Pendaftaran pendaftaran) throws Exception {
		Session session = HibernateUtil.currentSession();
		CetakKartuPasien cetakKartuPasien = (CetakKartuPasien) session.createCriteria(CetakKartuPasien.class)
				.add(Restrictions.eq("pendaftaran", pendaftaran)).setMaxResults(1).uniqueResult();

		if (cetakKartuPasien == null) {
			cetakKartuPasien = new CetakKartuPasien();
			cetakKartuPasien.setTanggal(new Date());
			cetakKartuPasien.setPasien(pendaftaran.getPasien());
			String mykode = Common.generateCode(CetakKartuPasien.class, 10, "CETAK-KARTU", pendaftaran.getLokasi());
			cetakKartuPasien.setKode(mykode);
			cetakKartuPasien.setKeterangan("");
			cetakKartuPasien.setPendaftaran(pendaftaran);
			if (cetakKartuPasien.getLokasi() == null) {
				cetakKartuPasien.setLokasi(pendaftaran.getLokasi());
			}
			session.save(cetakKartuPasien);

			CommonSirs.simpanTransaksiTindakan(cetakKartuPasien.getPasien(), ConstantValues.PEMBUATAN_KARTU,
					ConstantValues.kelasNormal, pendaftaran.getLokasi(), 1.0, pendaftaran, cetakKartuPasien);
		}

		CetakKartuPasienAction.onCetakKartu(pendaftaran.getPasien());

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onCetakStatusPasien(Pasien pasien) {

		try {

			File myfile = new File(Sessions.getCurrent().getWebApp().getRealPath("/report/temp") + "/barcode_"
					+ pasien.getKode() + ".png");
			myfile.getParentFile().mkdirs();
			myfile.createNewFile();

			BarcodeCommon.generateCRCode(pasien.getKode(), myfile);

			String barcode = myfile.getAbsolutePath();

			Map parameters = new HashMap();
			Common.insertProperty(Pasien.class, pasien, parameters, "");
			parameters.put("mybarcode", barcode);
			parameters.put("rm", pasien.getKode());
			parameters.put("keluarga", pasien.getNama_penanggungjawab());
			parameters.put("kesatuan", pasien.getJenisPasienDinas() == null ? ""
					: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AD.getId()) ? Pasien.TNI_AD.getName()
							: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AL.getId())
									? Pasien.TNI_AL.getName()
									: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AU.getId())
											? Pasien.TNI_AU.getName()
											: pasien.getJenisPasienDinas().trim().equals(Pasien.PNS.getId())
													? Pasien.PNS.getName()
													: "");
			parameters.put("pangkat", pasien.getPangkat() == null ? "" : pasien.getPangkat());
			parameters.put("nip", pasien.getNip() == null ? "" : pasien.getNip());
			parameters.put("telp", (pasien.getNoTelp() == null ? "" : pasien.getNoTelp()) + " / "
					+ (pasien.getNoHp() == null ? "" : pasien.getNoHp()));
			parameters.put("status_perkawinan", pasien.getStatusPerkawinan());
			parameters.put("jenis_kelamin", pasien.getJenisKelamin());
			parameters.put("agama", pasien.getAgama() == null ? "" : pasien.getAgama().getNama());
			parameters.put("pendidikan", pasien.getPendidikan() == null ? "" : pasien.getPendidikan().getNama());
			parameters.put("pekerjaan", pasien.getPekerjaan());

			Date tangggalKunjunganpertama = (Date) HibernateUtil.currentSession().createCriteria(Pendaftaran.class)
					.add(Restrictions.eq("pasien", pasien)).setProjection(Projections.min("tanggalPendaftaran"))
					.setMaxResults(1).uniqueResult();

			parameters.put("kunjungan",
					tangggalKunjunganpertama == null ? "" : Common.dateFormat3.get().format(tangggalKunjunganpertama));
			parameters.put("ttd", "Jakarta, " + Common.dateFormat2.get().format(new Date()));
			parameters.put("nama", pasien.getNama() == null ? "" : pasien.getNama().trim());
			parameters.put("ttl", (pasien.getTempatLahir() == null ? "" : pasien.getTempatLahir()) + " / "
					+ (pasien.getTanggalLahir() == null ? "" : Common.dateFormat2.get().format(pasien.getTanggalLahir())));
			parameters.put("alamat", pasien.getAlamatLengkap());
			parameters.put("wkt_reg", pasien.getTanggalRegistrasi() == null ? ""
					: Common.dateFormat3.get().format(pasien.getTanggalRegistrasi()));

			File file = Report.generateFileReport("sirs/data_identitas_pasien", Report.PDF, parameters,
					"sirs/data_identitas_pasien", new Date(), Sessions.getCurrent().getWebApp());

			Report.tampil(file, parameters, "sirs/data_identitas_pasien");

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonSirs.java:460");
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onCetakHasilDiagnosaPasienRawatInap(DiagnosaPenyakit diagnosaPenyakit) {

		Pendaftaran pendaftaran = diagnosaPenyakit.getPendaftaran();

		try {
			Pasien pasien = pendaftaran.getPasien();
			File myfile = new File(Sessions.getCurrent().getWebApp().getRealPath("/report/temp") + "/barcode_"
					+ pasien.getKode() + ".png");
			myfile.getParentFile().mkdirs();
			myfile.createNewFile();

			BarcodeCommon.generateCRCode(pasien.getKode(), myfile);

			String barcode = myfile.getAbsolutePath();
			System.out.println("barcode = " + barcode);

			Map parameters = new HashMap();
			Common.insertProperty(Pasien.class, pasien, parameters, "");
			Common.insertProperty(DiagnosaPenyakit.class, diagnosaPenyakit, parameters, "diagnosa");
			parameters.put("mybarcode", barcode);
			parameters.put("rm", pasien.getKode());
			parameters.put("keluarga", pasien.getNama_penanggungjawab());
			parameters.put("kesatuan", pasien.getJenisPasienDinas() == null ? ""
					: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AD.getId()) ? Pasien.TNI_AD.getName()
							: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AL.getId())
									? Pasien.TNI_AL.getName()
									: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AU.getId())
											? Pasien.TNI_AU.getName()
											: pasien.getJenisPasienDinas().trim().equals(Pasien.PNS.getId())
													? Pasien.PNS.getName()
													: "");
			parameters.put("pangkat", pasien.getPangkat() == null ? "" : pasien.getPangkat());
			parameters.put("nip", pasien.getNip() == null ? "" : pasien.getNip());
			parameters.put("telp", (pasien.getNoTelp() == null ? "" : pasien.getNoTelp()) + " / "
					+ (pasien.getNoHp() == null ? "" : pasien.getNoHp()));
			parameters.put("status_perkawinan", pasien.getStatusPerkawinan());
			parameters.put("jenis_kelamin", pasien.getJenisKelamin());
			parameters.put("agama", pasien.getAgama() == null ? "" : pasien.getAgama().getNama());
			parameters.put("pendidikan", pasien.getPendidikan() == null ? "" : pasien.getPendidikan().getNama());
			parameters.put("pekerjaan", pasien.getPekerjaan());

			Date tangggalKunjunganpertama = (Date) HibernateUtil.currentSession().createCriteria(Pendaftaran.class)
					.add(Restrictions.eq("pasien", pasien)).setProjection(Projections.min("tanggalPendaftaran"))
					.setMaxResults(1).uniqueResult();

			parameters.put("kunjungan",
					tangggalKunjunganpertama == null ? "" : Common.dateFormat3.get().format(tangggalKunjunganpertama));
			parameters.put("ttd", "Jakarta, " + Common.dateFormat2.get().format(new Date()));
			parameters.put("nama", pasien.getNama() == null ? "" : pasien.getNama().trim());
			parameters.put("ttl", (pasien.getTempatLahir() == null ? "" : pasien.getTempatLahir()) + " / "
					+ (pasien.getTanggalLahir() == null ? "" : Common.dateFormat2.get().format(pasien.getTanggalLahir())));
			parameters.put("alamat", pasien.getAlamatLengkap());
			parameters.put("wkt_reg", pendaftaran.getTanggalPendaftaran() == null ? ""
					: Common.dateFormat3.get().format(pendaftaran.getTanggalPendaftaran()));

			parameters.put("noreg", pendaftaran.getKode());
			parameters.put("kelas",
					pendaftaran.getKelasPerawatan() == null ? "" : pendaftaran.getKelasPerawatan().getNama());
			parameters.put("ruang",
					pendaftaran.getRuangPerawatan() == null ? "" : pendaftaran.getRuangPerawatan().getNama());
			parameters.put("kamar",
					pendaftaran.getKamarPerawatan() == null ? "" : pendaftaran.getKamarPerawatan().getNama());
			parameters.put("bed", pendaftaran.getTempatTidur() == null ? "" : pendaftaran.getTempatTidur().getNama());

			parameters.put("id", diagnosaPenyakit.getId());

			parameters.put("dokter_pemeriksa",
					diagnosaPenyakit.getDokter() == null ? "-" : diagnosaPenyakit.getDokter().toString());

			parameters.put("poli_diperiksa",
					diagnosaPenyakit.getPoly() == null ? "-" : diagnosaPenyakit.getPoly().toString());

			parameters.put("waktu_diperiksa", diagnosaPenyakit.getTanggal() == null ? ""
					: Common.dateFormat3.get().format(diagnosaPenyakit.getTanggal()));

			List dataPemeriksaan = new PemeriksaanReportHelper(diagnosaPenyakit, Pemeriksaan.JENIS_KELUHAN).getHasil();

			dataPemeriksaan.add(new HashMap());
			dataPemeriksaan.addAll(new PemeriksaanReportHelper(diagnosaPenyakit, Pemeriksaan.JENIS_RIWAYAT).getHasil());

			dataPemeriksaan.add(new HashMap());
			dataPemeriksaan.addAll(new PemeriksaanReportHelper(diagnosaPenyakit, Pemeriksaan.JENIS_PERIKSA).getHasil());

			JRMapCollectionDataSource data_keluhan = new JRMapCollectionDataSource(dataPemeriksaan);

			parameters.put("data_keluhan", data_keluhan);

			parameters.put("kode_rm", diagnosaPenyakit.getKode());
			parameters.put("menular", diagnosaPenyakit.getApakahMenular());
			parameters.put("d1",
					diagnosaPenyakit.getDiagnosaAwal1() == null ? "" : diagnosaPenyakit.getDiagnosaAwal1().toString());
			parameters.put("d2", diagnosaPenyakit.getDiagnosaAkhir1() == null ? ""
					: diagnosaPenyakit.getDiagnosaAkhir1().toString());
			parameters.put("d3",
					diagnosaPenyakit.getDiagnosaAwal2() == null ? "" : diagnosaPenyakit.getDiagnosaAwal2().toString());
			parameters.put("d4", diagnosaPenyakit.getDiagnosaAkhir2() == null ? ""
					: diagnosaPenyakit.getDiagnosaAkhir2().toString());
			parameters.put("d5",
					diagnosaPenyakit.getDiagnosaAwal3() == null ? "" : diagnosaPenyakit.getDiagnosaAwal3().toString());
			parameters.put("d6", diagnosaPenyakit.getDiagnosaAkhir3() == null ? ""
					: diagnosaPenyakit.getDiagnosaAkhir3().toString());

			Session session = HibernateUtil.currentSession();
			List<ResepDetail> resepDetails = session.createCriteria(ResepDetail.class).addOrder(Order.desc("id"))
					.createAlias("resep", "resep").add(Restrictions.eq("resep.diagnosaPenyakit", diagnosaPenyakit))
					.list();

			List<String> strings = new ArrayList<String>();
			for (ResepDetail resepDetail : resepDetails) {
				if (resepDetail.getItem() != null) {
					strings.add(
							resepDetail.getItem().getNama() + ": " + Common.numberFormat.get().format(resepDetail.getJumlah())
									+ " " + (resepDetail.getItem().getSatuanItem() == null ? ""
											: resepDetail.getItem().getSatuanItem().getNama()));
				} else if (resepDetail.getRacikan() != null) {
					List<RacikanDetail> racikanDetails = session.createCriteria(RacikanDetail.class)
							.add(Restrictions.eq("racikan", resepDetail.getRacikan())).list();
					for (RacikanDetail racikanDetail : racikanDetails) {
						strings.add(racikanDetail.getItem().getNama() + ": "
								+ Common.numberFormat.get().format(racikanDetail.getJumlah()) + " "
								+ (racikanDetail.getItem().getSatuanItem() == null ? ""
										: racikanDetail.getItem().getSatuanItem().getNama()));
					}
				}
			}

			parameters.put("resep", strings.toString());

			List<TindakanDiagnosaPenyakit> tindakanDiagnosaPenyakits = session
					.createCriteria(TindakanDiagnosaPenyakit.class).addOrder(Order.desc("id"))
					.add(Restrictions.eq("diagnosaPenyakit", diagnosaPenyakit)).list();
			strings = new ArrayList<String>();
			for (TindakanDiagnosaPenyakit tindakanDiagnosaPenyakit : tindakanDiagnosaPenyakits) {
				if (tindakanDiagnosaPenyakit.getTindakan() != null) {
					strings.add(tindakanDiagnosaPenyakit.getTindakan().getNama());
				}
			}
			parameters.put("tindakan", strings.toString());

			List<AlatMedisDiagnosaPenyakit> alatMedisDiagnosaPenyakits = session
					.createCriteria(AlatMedisDiagnosaPenyakit.class).addOrder(Order.desc("id"))
					.add(Restrictions.eq("diagnosaPenyakit", diagnosaPenyakit)).list();
			strings = new ArrayList<String>();
			for (AlatMedisDiagnosaPenyakit alatMedisDiagnosaPenyakit : alatMedisDiagnosaPenyakits) {
				if (alatMedisDiagnosaPenyakit.getAlatMedis() != null) {
					strings.add(alatMedisDiagnosaPenyakit.getAlatMedis().getNama());
				}
			}
			parameters.put("alatMedis", strings.toString());

			parameters.put("catatan", diagnosaPenyakit.getKeterangan());

			File file = Report.generateFileReport("sirs/diagnosa_pasien_rawat_inap_satuan", Report.PDF, parameters,
					"sirs/diagnosa_pasien_rawat_inap_satuan", new Date(), Sessions.getCurrent().getWebApp());

			Report.tampil(file, parameters, "sirs/diagnosa_pasien_rawat_inap_satuan"); 

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonSirs.java:623");
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onCetakHasilDiagnosaPasien(DiagnosaPenyakit diagnosaPenyakit) {

		try {

			Pasien pasien = diagnosaPenyakit.getPasien();

			final File myfile = new File(Sessions.getCurrent().getWebApp().getRealPath("/report/temp") + "/barcode_"
					+ pasien.getKode() + ".png");
			myfile.getParentFile().mkdirs();
			myfile.createNewFile();

			BarcodeCommon.generateCRCode(pasien.getKode(), myfile);

			String barcode = myfile.getAbsolutePath();
			System.out.println("barcode = " + barcode);

			Map parameters = new HashMap();
			Common.insertProperty(Pasien.class, pasien, parameters, "");
			Common.insertProperty(DiagnosaPenyakit.class, diagnosaPenyakit, parameters, "diagnosa");
			parameters.put("rm", pasien.getKode());
			parameters.put("keluarga", pasien.getNama_penanggungjawab());
			parameters.put("kesatuan", pasien.getJenisPasienDinas() == null ? ""
					: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AD.getId()) ? Pasien.TNI_AD.getName()
							: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AL.getId())
									? Pasien.TNI_AL.getName()
									: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AU.getId())
											? Pasien.TNI_AU.getName()
											: pasien.getJenisPasienDinas().trim().equals(Pasien.PNS.getId())
													? Pasien.PNS.getName()
													: "");
			parameters.put("pangkat", pasien.getPangkat() == null ? "" : pasien.getPangkat());
			parameters.put("nip", pasien.getNip() == null ? "" : pasien.getNip());
			parameters.put("telp", (pasien.getNoTelp() == null ? "" : pasien.getNoTelp()) + " / "
					+ (pasien.getNoHp() == null ? "" : pasien.getNoHp()));
			parameters.put("status_perkawinan", pasien.getStatusPerkawinan());
			parameters.put("jenis_kelamin", pasien.getJenisKelamin());
			parameters.put("agama", pasien.getAgama() == null ? "" : pasien.getAgama().getNama());
			parameters.put("pendidikan", pasien.getPendidikan() == null ? "" : pasien.getPendidikan().getNama());
			parameters.put("pekerjaan", pasien.getPekerjaan());

			Date tangggalKunjunganpertama = (Date) HibernateUtil.currentSession().createCriteria(Pendaftaran.class)
					.add(Restrictions.eq("pasien", pasien)).setProjection(Projections.min("tanggalPendaftaran"))
					.setMaxResults(1).uniqueResult();

			parameters.put("kunjungan",
					tangggalKunjunganpertama == null ? "" : Common.dateFormat3.get().format(tangggalKunjunganpertama));
			parameters.put("ttd", "Jakarta, " + Common.dateFormat2.get().format(new Date()));
			parameters.put("nama", pasien.getNama() == null ? "" : pasien.getNama().trim());
			parameters.put("ttl", (pasien.getTempatLahir() == null ? "" : pasien.getTempatLahir()) + " / "
					+ (pasien.getTanggalLahir() == null ? "" : Common.dateFormat2.get().format(pasien.getTanggalLahir())));
			parameters.put("alamat", pasien.getAlamatLengkap());
			parameters.put("wkt_reg", pasien.getTanggalRegistrasi() == null ? ""
					: Common.dateFormat3.get().format(pasien.getTanggalRegistrasi()));

			parameters.put("id", diagnosaPenyakit.getId());

			parameters.put("dokter_pemeriksa",
					diagnosaPenyakit.getDokter() == null ? "-" : diagnosaPenyakit.getDokter().toString());

			parameters.put("poli_diperiksa",
					diagnosaPenyakit.getPoly() == null ? "-" : diagnosaPenyakit.getPoly().toString());

			parameters.put("waktu_diperiksa", diagnosaPenyakit.getTanggal() == null ? ""
					: Common.dateFormat3.get().format(diagnosaPenyakit.getTanggal()));

			List dataPemeriksaan = new PemeriksaanReportHelper(diagnosaPenyakit, Pemeriksaan.JENIS_KELUHAN).getHasil();

			dataPemeriksaan.add(new HashMap());
			dataPemeriksaan.addAll(new PemeriksaanReportHelper(diagnosaPenyakit, Pemeriksaan.JENIS_RIWAYAT).getHasil());

			dataPemeriksaan.add(new HashMap());
			dataPemeriksaan.addAll(new PemeriksaanReportHelper(diagnosaPenyakit, Pemeriksaan.JENIS_PERIKSA).getHasil());

			JRMapCollectionDataSource data_keluhan = new JRMapCollectionDataSource(dataPemeriksaan);

			parameters.put("data_keluhan", data_keluhan);

			parameters.put("kode_rm", diagnosaPenyakit.getKode());
			parameters.put("menular", diagnosaPenyakit.getApakahMenular());
			parameters.put("d1",
					diagnosaPenyakit.getDiagnosaAwal1() == null ? "" : diagnosaPenyakit.getDiagnosaAwal1().toString());
			parameters.put("d2", diagnosaPenyakit.getDiagnosaAkhir1() == null ? ""
					: diagnosaPenyakit.getDiagnosaAkhir1().toString());
			parameters.put("d3",
					diagnosaPenyakit.getDiagnosaAwal2() == null ? "" : diagnosaPenyakit.getDiagnosaAwal2().toString());
			parameters.put("d4", diagnosaPenyakit.getDiagnosaAkhir2() == null ? ""
					: diagnosaPenyakit.getDiagnosaAkhir2().toString());
			parameters.put("d5",
					diagnosaPenyakit.getDiagnosaAwal3() == null ? "" : diagnosaPenyakit.getDiagnosaAwal3().toString());
			parameters.put("d6", diagnosaPenyakit.getDiagnosaAkhir3() == null ? ""
					: diagnosaPenyakit.getDiagnosaAkhir3().toString());

			Session session = HibernateUtil.currentSession();
			List<ResepDetail> resepDetails = session.createCriteria(ResepDetail.class).addOrder(Order.desc("id"))
					.createAlias("resep", "resep").add(Restrictions.eq("resep.diagnosaPenyakit", diagnosaPenyakit))
					.list();

			List<String> strings = new ArrayList<String>();
			for (ResepDetail resepDetail : resepDetails) {
				if (resepDetail.getItem() != null) {
					strings.add(
							resepDetail.getItem().getNama() + ": " + Common.numberFormat.get().format(resepDetail.getJumlah())
									+ " " + (resepDetail.getItem().getSatuanItem() == null ? ""
											: resepDetail.getItem().getSatuanItem().getNama()));
				} else if (resepDetail.getRacikan() != null) {
					List<RacikanDetail> racikanDetails = session.createCriteria(RacikanDetail.class)
							.add(Restrictions.eq("racikan", resepDetail.getRacikan())).list();
					for (RacikanDetail racikanDetail : racikanDetails) {
						strings.add(racikanDetail.getItem().getNama() + ": "
								+ Common.numberFormat.get().format(racikanDetail.getJumlah()) + " "
								+ (racikanDetail.getItem().getSatuanItem() == null ? ""
										: racikanDetail.getItem().getSatuanItem().getNama()));
					}
				}
			}

			parameters.put("resep", strings.toString());

			List<TindakanDiagnosaPenyakit> tindakanDiagnosaPenyakits = session
					.createCriteria(TindakanDiagnosaPenyakit.class).addOrder(Order.desc("id"))
					.add(Restrictions.eq("diagnosaPenyakit", diagnosaPenyakit)).list();
			strings = new ArrayList<String>();
			for (TindakanDiagnosaPenyakit tindakanDiagnosaPenyakit : tindakanDiagnosaPenyakits) {
				if (tindakanDiagnosaPenyakit.getTindakan() != null) {
					strings.add(tindakanDiagnosaPenyakit.getTindakan().getNama());
				}
			}
			parameters.put("tindakan", strings.toString());

			List<AlatMedisDiagnosaPenyakit> alatMedisDiagnosaPenyakits = session
					.createCriteria(AlatMedisDiagnosaPenyakit.class).addOrder(Order.desc("id"))
					.add(Restrictions.eq("diagnosaPenyakit", diagnosaPenyakit)).list();
			strings = new ArrayList<String>();
			for (AlatMedisDiagnosaPenyakit alatMedisDiagnosaPenyakit : alatMedisDiagnosaPenyakits) {
				if (alatMedisDiagnosaPenyakit.getAlatMedis() != null) {
					strings.add(alatMedisDiagnosaPenyakit.getAlatMedis().getNama());
				}
			}
			parameters.put("alatMedis", strings.toString());

			parameters.put("catatan", diagnosaPenyakit.getKeterangan());

			File file = Report.generateFileReport("sirs/diagnosa_pasien", Report.PDF, parameters,
					"sirs/diagnosa_pasien", new Date(), Sessions.getCurrent().getWebApp());

			Report.tampil(file, parameters, "sirs/diagnosa_pasien");

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonSirs.java:777");
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onCetakTracer(Pendaftaran pendaftaran) {

		try {

			File myfile = new File(Sessions.getCurrent().getWebApp().getRealPath("/report/temp") + "/barcode_"
					+ pendaftaran.getKode() + ".png");
			myfile.getParentFile().mkdirs();
			myfile.createNewFile();

			BarcodeCommon.generateCRCode(pendaftaran.getKode(), myfile);

			String barcode = myfile.getAbsolutePath();

			Map parameters = new HashMap();
			parameters.put("pendaftaran", pendaftaran.getId());
			parameters.put("mybarcode", barcode);
			parameters.put("tanggalpendaftaran", pendaftaran.getTanggalPendaftaran());
			parameters.put("mr", pendaftaran.getPasien() == null ? "" : pendaftaran.getPasien().getKode());

			parameters.put("nama_pasien", pendaftaran.getPasien() == null ? "" : pendaftaran.getPasien().getNama());
			parameters.put("nama_poli", pendaftaran.getPoly() == null ? "" : pendaftaran.getPoly().getNama());
			parameters.put("nomor_antrian", pendaftaran.getNomorAntrian());
			parameters.put("nama_dokter", pendaftaran.getDokter() == null ? "" : pendaftaran.getDokter().getNama());

			Common.insertProperty(Pendaftaran.class, pendaftaran, parameters, "");

			List<Map> maps = new ArrayList<Map>();
			maps.add(parameters);

			parameters.put("maps", maps);

			File file = Report.generateFileReport("sirs/tracer_pasien", Report.PDF, parameters, "sirs/tracer_pasien",
					new Date(), Sessions.getCurrent().getWebApp());

			Report.tampil(file, parameters, "sirs/tracer_pasien");

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonSirs.java:819");
		}
	}

	public static Double getTotalDiskonDalamPersen(ItemMedis item, Tindakan tindakan, AlatMedis alatMedis,
			Integer jumlah, Date tanggal, Asuransi asuransi, Set<Komunitas> komunitas) {
		Double total = 0.0;
		List<Diskon> diskons = getDiskonSekarang(item, tindakan, alatMedis, jumlah, tanggal, asuransi, komunitas);
		for (Diskon diskon : diskons) {
			total += diskon.getJumlah();
		}
		diskons = null;
		return total;
	}

	public static Double getTotalPajakDalamPersen(ItemMedis item, Tindakan tindakan, AlatMedis alatMedis,
			Asuransi asuransi, Set<Komunitas> komunitas) {
		Double total = 0.0;
		List<PajakMedis> pajaks = getPajakSekarang(item, tindakan, alatMedis, asuransi, komunitas);
		for (PajakMedis pajak : pajaks) {
			total += pajak.getJumlah();
		}
		pajaks = null;
		return total;
	}

	@SuppressWarnings("unchecked")
	public static List<Diskon> getDiskonSekarang(ItemMedis item, Tindakan tindakan, AlatMedis alatMedis, Integer jumlah,
			Date tanggal, Asuransi asuransi, Set<Komunitas> komunitas) {
		List<Diskon> diskons = HibernateUtil.currentSession().createCriteria(DiskonDetail.class)
				.createAlias("diskon", "diskon").add(Restrictions.eq("diskon.aktif", true))
				.add(asuransi == null ? Restrictions.isNull("diskon.asuransi")
						: Restrictions.eq("diskon.asuransi", asuransi))

				.add(komunitas == null || komunitas.isEmpty() ? Restrictions.isNull("diskon.komunitas")
						: Restrictions.in("diskon.komunitas", komunitas))

				.add(Restrictions.le("diskon.jumlahMinimal", jumlah))
				.add(Restrictions.ge("diskon.jumlahMaksimal", jumlah))

				.add(Restrictions.le("diskon.mulai", tanggal))
				.add(Restrictions.or(Restrictions.isNull("diskon.sampai"), Restrictions.ge("diskon.sampai", tanggal)))
				.add(item == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("item", item))
				.add(tindakan == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("tindakan", tindakan))
				.add(alatMedis == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("alatMedis", alatMedis))
				.setProjection(Projections.groupProperty("diskon")).list();
		return diskons;
	}

	@SuppressWarnings("unchecked")
	public static List<PajakMedis> getPajakSekarang(ItemMedis item, Tindakan tindakan, AlatMedis alatMedis,
			Asuransi asuransi, Set<Komunitas> komunitas) {
		List<PajakMedis> pajaks = HibernateUtil.currentSession().createCriteria(PajakDetail.class)
				.createAlias("pajak", "pajak")
				.add(asuransi == null ? Restrictions.isNull("pajak.asuransi")
						: Restrictions.eq("pajak.asuransi", asuransi))

				.add(komunitas == null || komunitas.isEmpty() ? Restrictions.isNull("pajak.komunitas")
						: Restrictions.in("pajak.komunitas", komunitas))

				.add(Restrictions.eq("pajak.aktif", true)).add(Restrictions.le("pajak.mulai", new Date()))
				.add(Restrictions.or(Restrictions.isNull("pajak.sampai"), Restrictions.ge("pajak.sampai", new Date())))
				.add(item == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("item", item))
				.add(tindakan == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("tindakan", tindakan))
				.add(alatMedis == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("alatMedis", alatMedis))
				.setProjection(Projections.groupProperty("pajak")).list();
		return pajaks;
	}

	@SuppressWarnings("unchecked")
	public static List<Tindakan> populateJasaRacik() {

		Session session = HibernateUtil.currentSession();
		JenisTindakan jenisTindakan = (JenisTindakan) session.createCriteria(JenisTindakan.class)
				.add(Restrictions.eq("nama", JenisTindakan.JASA_RACIK)).setMaxResults(1).uniqueResult();
		if (jenisTindakan == null) {
			jenisTindakan = new JenisTindakan();
			jenisTindakan.setNama(JenisTindakan.JASA_RACIK);
			session.save(jenisTindakan);
		}

		List<Tindakan> tindakansJasaRaciks = session.createCriteria(Tindakan.class)
				.add(Restrictions.eq("jenisTindakan", jenisTindakan)).list();

		if (tindakansJasaRaciks.isEmpty()) {
			Tindakan tindakan = new Tindakan();
			tindakan.setNama("Bubuk");
			tindakan.setJenisTindakan(jenisTindakan);
			session.save(tindakan);
			tindakansJasaRaciks.add(tindakan);

			tindakan = new Tindakan();
			tindakan.setNama("Sirup");
			tindakan.setJenisTindakan(jenisTindakan);
			session.save(tindakan);
			tindakansJasaRaciks.add(tindakan);

			tindakan = new Tindakan();
			tindakan.setNama("Krim");
			tindakan.setJenisTindakan(jenisTindakan);
			session.save(tindakan);
			tindakansJasaRaciks.add(tindakan);
		}

		return tindakansJasaRaciks;
	}

	@SuppressWarnings("unchecked")
	public static Double hitungHargaJualRacikan(Racikan racikan, KelasPerawatan kelasPerawatan, Dokter dokter,
			Asuransi asuransi, Set<Komunitas> komunitas, Pasien pasien) throws Exception {
		Session session = HibernateUtil.currentSession();
		List<RacikanDetail> racikanDetails = session.createCriteria(RacikanDetail.class)
				.add(Restrictions.eq("racikan", racikan)).list();
		Double harJual = 0.0;

		for (RacikanDetail racikanDetail : racikanDetails) {
			if (racikanDetail.getItem() == null) {
				continue;
			}

			HargaJualItem hargaJualItem = CommonTarifItem.getHargaJualItem(racikanDetail.getItem(), kelasPerawatan,
					dokter, asuransi, komunitas, pasien);

			harJual += (hargaJualItem == null || hargaJualItem.getHargaJual() == null ? 0.0
					: hargaJualItem.getHargaJual())
					* (racikanDetail.getJumlah() == null ? 0.0 : racikanDetail.getJumlah());
		}
		return harJual;
	}

	@SuppressWarnings("unchecked")
	public static Double hitungDiskonRacikan(Racikan racikan, KelasPerawatan kelasPerawatan, Date tanggal,
			Dokter dokter, Asuransi asuransi, Set<Komunitas> komunitas, Pasien pasien) throws Exception {
		Session session = HibernateUtil.currentSession();
		List<RacikanDetail> racikanDetails = session.createCriteria(RacikanDetail.class)
				.add(Restrictions.eq("racikan", racikan)).list();
		Double totalDiskon = 0.0;

		for (RacikanDetail racikanDetail : racikanDetails) {
			if (racikanDetail.getItem() == null) {
				continue;
			}

			final Double diskon = CommonSirs.getTotalDiskonDalamPersen(racikanDetail.getItem(), null, null,
					racikanDetail.getJumlah().intValue(), tanggal, asuransi, komunitas);

			HargaJualItem hargaJualItem = CommonTarifItem.getHargaJualItem(racikanDetail.getItem(), kelasPerawatan,
					dokter, asuransi, komunitas, pasien);
			totalDiskon += ((hargaJualItem == null || hargaJualItem.getHargaJual() == null ? 0.0
					: hargaJualItem.getHargaJual()) * (diskon / 100.0))
					* (racikanDetail.getJumlah() == null ? 0.0 : racikanDetail.getJumlah());
		}
		return totalDiskon;
	}

	@SuppressWarnings("unchecked")
	public static Double hitungPajakRacikan(Racikan racikan, KelasPerawatan kelasPerawatan, Dokter dokter,
			Asuransi asuransi, Set<Komunitas> komunitas, Pasien pasien) throws Exception {
		Session session = HibernateUtil.currentSession();
		List<RacikanDetail> racikanDetails = session.createCriteria(RacikanDetail.class)
				.add(Restrictions.eq("racikan", racikan)).list();
		Double totalPajak = 0.0;

		for (RacikanDetail racikanDetail : racikanDetails) {
			if (racikanDetail.getItem() == null) {
				continue;
			}

			final Double pajak = CommonSirs.getTotalPajakDalamPersen(racikanDetail.getItem(), null, null, asuransi,
					komunitas);

			HargaJualItem hargaJualItem = CommonTarifItem.getHargaJualItem(racikanDetail.getItem(), kelasPerawatan,
					dokter, asuransi, komunitas, pasien);

			totalPajak += ((hargaJualItem == null || hargaJualItem.getHargaJual() == null ? 0.0
					: hargaJualItem.getHargaJual()) * (pajak / 100.0))
					* (racikanDetail.getJumlah() == null ? 0.0 : racikanDetail.getJumlah());
		}
		return totalPajak;
	}

	public static Double hitungHPP(ItemMedis item, Session session) {
		String sql = "select sum(a.amount)/sum(a.qty) from sirs.detail_transaksi_pasien a where a.item = "
				+ (item == null || item.getId() == null ? -1 : item.getId()) + " and a.kode_transaksi in (" + ConstantValues.saldoAwal.getId()
				+ "," + ConstantValues.beliMasuk.getId() + ")";
		session = (session == null ? HibernateUtil.currentSession() : session);
		Number hpp = (Number) session.createSQLQuery(sql).uniqueResult();
		return hpp == null ? 0.0 : hpp.doubleValue();
	}

	public static Double hitungHargaBeli(ItemMedis item, Session session) {
		String sql = "select sum(a.amount)/sum(a.qty) from sirs.detail_transaksi_pasien a where a.item = "
				+ (item == null || item.getId() == null ? -1 : item.getId()) + " and a.kode_transaksi in (" + ConstantValues.beliMasuk.getId()
				+ ")";
		session = (session == null ? HibernateUtil.currentSession() : session);
		Number hb = (Number) session.createSQLQuery(sql).uniqueResult();
		return hb == null ? 0.0 : hb.doubleValue();
	}

	public static DetailTransaksiLayanan simpanTransaksiTindakan(Pasien pasien, Tindakan tindakan,
			KelasPerawatan kelasPerawatan, Lokasi lokasi, Double qty) throws Exception {
		return simpanTransaksiTindakan(pasien, tindakan, kelasPerawatan, lokasi, qty, null, null);
	}

	public static DetailTransaksiLayanan simpanTransaksiTindakan(Pasien pasien, Tindakan tindakan,
			KelasPerawatan kelasPerawatan, Lokasi lokasi, Double qty, Pendaftaran pendaftaran,
			CetakKartuPasien cetakKartuPasien) throws Exception {
		if (pasien == null) {
			Messagebox.show("Pasien harus diisi", "Peringatan", Messagebox.OK, Messagebox.EXCLAMATION);
			return null;
		}
		if (tindakan == null) {
//			Messagebox.show("Data layanan harus diisi", "Peringatan", Messagebox.OK, Messagebox.EXCLAMATION);
			return null;
		}

		Session session = HibernateUtil.currentSession();

		if (pendaftaran != null && pendaftaran.getId() != null) {
			session.createSQLQuery("delete from sirs.detail_transaksi_layanan where pendaftaran = "
					+ pendaftaran.getId() + " and lunas = false;").executeUpdate();
		}

		if (cetakKartuPasien != null && cetakKartuPasien.getId() != null) {
			session.createSQLQuery("delete from sirs.detail_transaksi_layanan where cetak_kartu_pasien = "
					+ cetakKartuPasien.getId() + " and lunas = false;").executeUpdate();
		}

		DetailTransaksiLayanan detailTransaksiLayanan = new DetailTransaksiLayanan();
		detailTransaksiLayanan.setDiskon(0.0);
		detailTransaksiLayanan.setKeterangan(tindakan.getNama());
		detailTransaksiLayanan.setLokasi(lokasi);
		detailTransaksiLayanan.setPajak(0.0);
		detailTransaksiLayanan.setPasien(pasien);
		detailTransaksiLayanan.setQty(qty == null ? 0.0 : qty);
		detailTransaksiLayanan.setQtyBonus(0.0);
		detailTransaksiLayanan.setTanggal(new Date());
		detailTransaksiLayanan.setTindakan(tindakan);
		detailTransaksiLayanan.setPendaftaran(pendaftaran);
		detailTransaksiLayanan.setCetakKartuPasien(cetakKartuPasien);

		return CommonPendaftaranUtil.setDetailBiaya(detailTransaksiLayanan, kelasPerawatan);
	}

	public static List<Minggu> CURRENT_MINGGUS;

	@SuppressWarnings("unchecked")
	public static List<Minggu> getMinggu(Integer bulan, Integer tahun) {
		bulan = bulan == null ? (Calendar.getInstance().get(Calendar.MONTH) + 1) : bulan;
		tahun = tahun == null ? Calendar.getInstance().get(Calendar.YEAR) : tahun;
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
		List<Minggu> SELEDTED_MINGGUS = session.createCriteria(Minggu.class).add(Restrictions.eq("bulan", bulan))
				.add(Restrictions.eq("tahun", tahun)).list();

		if (SELEDTED_MINGGUS.size() == 0) {
			SELEDTED_MINGGUS = CommonReport.getMinggu(bulan, tahun);
			for (Minggu minggu : SELEDTED_MINGGUS) {
				minggu.setBulan(bulan);
				minggu.setTahun(tahun);
				minggu.setTbmuser(Common.getCurrentUser());
				session.getTransaction().begin();
				session.save(minggu);
				session.getTransaction().commit();
			}
		}
		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		return SELEDTED_MINGGUS;
	} finally {
			// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
			// finally menjamin penutupan walau terjadi exception (idempoten via isOpen()).
			if (session != null && session.isOpen()) {
				try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/common/CommonSirs.java:1095");}
				try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/common/CommonSirs.java:1096");}
			}
		}
	}

	public static Long generateMaxByJenisPasien(JenisPasien jenisPasien) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
		Long strmax = (Long) session.createCriteria(Pasien.class)
				// .createAlias("propinsi", "propinsi", Criteria.LEFT_JOIN)
				// .createAlias("kecamatan", "kecamatan", Criteria.LEFT_JOIN)
				// .createAlias("kota", "kota", Criteria.LEFT_JOIN)
				// .createAlias("kelurahan", "kelurahan", Criteria.LEFT_JOIN)
				// .createAlias("jenisPasien", "jenisPasien",
				// Criteria.LEFT_JOIN)
				// .createAlias("agama", "agama", Criteria.LEFT_JOIN)
				// .createAlias("pendidikan", "pendidikan", Criteria.LEFT_JOIN)
				// .createAlias("prioritasPasien", "prioritasPasien",
				// Criteria.LEFT_JOIN)

				.add(Restrictions.not(Restrictions.ilike("kode", "SS", MatchMode.ANYWHERE)))
				.add(Restrictions.not(Restrictions.ilike("kode", "DD", MatchMode.ANYWHERE)))
				.add(Restrictions.not(Restrictions.ilike("kode", "L", MatchMode.ANYWHERE)))

				.add(Restrictions.gt("id", 0L)).add(Restrictions.gt("index", 0L)).add(Restrictions.isNotNull("index"))

				.add(jenisPasien == null || !jenisPasien.getId().equals(ConstantValues.PASIEN_DINAS.getId())
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.and(Restrictions.ilike("kode", "D", MatchMode.ANYWHERE),
								Restrictions.eq("jenisPasien", ConstantValues.PASIEN_DINAS)))

				.add(jenisPasien == null || !jenisPasien.getId().equals(ConstantValues.PASIEN_SISWA.getId())
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.and(Restrictions.ilike("kode", "D", MatchMode.ANYWHERE),
								Restrictions.and(Restrictions.ilike("kode", "S", MatchMode.ANYWHERE),
										Restrictions.eq("jenisPasien", ConstantValues.PASIEN_SISWA))))

				// .add(jenisPasien == null
				// || !jenisPasien.getId().equals(
				// ConstantValues.PASIEN_UMUM.getId()) ? Restrictions
				// .sqlRestriction("1=1") : Restrictions.or(Restrictions
				// .eq("jenisPasien", ConstantValues.PASIEN_ASURANSI),
				// Restrictions.eq("jenisPasien",
				// ConstantValues.PASIEN_UMUM)))

				.add(jenisPasien == null || !jenisPasien.getId().equals(ConstantValues.PASIEN_UMUM.getId())
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.and(Restrictions.not(Restrictions.ilike("kode", "S", MatchMode.END)),
								Restrictions.and(Restrictions.not(Restrictions.ilike("kode", "L", MatchMode.END)),
										Restrictions.and(
												Restrictions.not(Restrictions.ilike("kode", "D", MatchMode.START)),
												Restrictions.and(
														Restrictions.ne("jenisPasien", ConstantValues.PASIEN_SISWA),
														Restrictions.ne("jenisPasien", ConstantValues.PASIEN_DINAS))))))
				.setProjection(Projections.sqlProjection(
						"max(to_number(case when trim(replace(replace(replace(kode,'D',''), 'L', ''),'S','')) = '' then '0' else trim(replace(replace(replace(kode,'D',''), 'L', ''),'S','')) end,'99999999999999')) as maksimal",
						new String[] { "maksimal" }, new Type[] { new LongType() }))
				.uniqueResult();

		// Projections.sql

		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}

		if (strmax == null) {
			strmax = 0L;
		}

		// Long max = Long.parseLong(strmax.replaceAll("D", "")
		// .replaceAll("S", "").replaceAll("L", ""));

		return strmax;
	} finally {
			// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
			// finally menjamin penutupan walau terjadi exception (idempoten via isOpen()).
			if (session != null && session.isOpen()) {
				try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/common/CommonSirs.java:1175");}
				try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/common/CommonSirs.java:1176");}
			}
		}
	}

	public static String generateCodePasien(int panjang, String awalan, String akhiran, JenisPasien jenisPasien) {
		return generateCodePasien(panjang, awalan, akhiran, jenisPasien, 1L);
	}

	public static String generateCodePasien(int panjang, String awalan, String akhiran, JenisPasien jenisPasien,
			Long penambahan) {
		Long max = null;
		if (jenisPasien == null) {
			max = (Long) HibernateUtil.currentSession().createCriteria(Pasien.class)
					.setProjection(Projections.max("id")).uniqueResult();
		} else {
			max = generateMaxByJenisPasien(jenisPasien);
		}
		if (max == null) {
			max = 0L;
		}
		String mykode = "00000000000000000000000000000" + (penambahan + max);
		mykode = (awalan == null || awalan.trim().equals("") ? "" : awalan)
				+ mykode.substring(mykode.length() - panjang, mykode.length());
		String hasil = mykode + akhiran;

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
		Integer count = ((Number) session.createCriteria(Pasien.class).add(Restrictions.eq("kode", hasil))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}

		System.out.println("hasil = " + hasil + ", count = " + count);

		if (count.equals(0)) {
			return hasil;
		} else {
			return generateCodePasien(panjang, awalan, akhiran, jenisPasien, ++penambahan);
		}

	} finally {
			// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
			// finally menjamin penutupan walau terjadi exception (idempoten via isOpen()).
			if (session != null && session.isOpen()) {
				try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/common/CommonSirs.java:1224");}
				try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/common/CommonSirs.java:1225");}
			}
		}
	}
}

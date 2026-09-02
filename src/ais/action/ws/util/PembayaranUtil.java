/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ais.action.ws.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.apache.axis.MessageContext;
import org.apache.axis.transport.http.HTTPConstants;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.action.master.KegiatanAction;
import ais.action.master.SetingBiayaAction;
import ais.action.master.helper.PembayaranUtilHelper;
import ais.action.master.helper.PengecualianTagihanList;
import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.CommonEmail;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AfiliasiCalonMahasiswa;
import ais.database.model.BankHost;
import ais.database.model.Beasiswa;
import ais.database.model.BeasiswaPunyaItemBiayaTambahan;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DendaPembayaran;
import ais.database.model.DendaPembayaranNominal;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.GeneralValueObject;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisSeleksi;
import ais.database.model.JenisTinggalMahasiswa;
import ais.database.model.Jenjang;
import ais.database.model.SettingBiaya;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.LogHostToHost;
import ais.database.model.LogPembayaran;
import ais.database.model.Mahasiswa;
import ais.database.model.Paket;
import ais.database.model.PembayaranMahasiswa;
import ais.database.model.PendaftaranCutiMahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.TunggakanMahasiswa;

/**
 * 
 * @author Fauzi
 */
public class PembayaranUtil {

	public static Long increments = 0L;

	/** Menyimpan stack trace exception terakhir dari simpanPembayaranMahasiswa per thread. */
	private static final ThreadLocal<String> lastSimpanException = new ThreadLocal<String>();

	private void tutupSessionSetelahPengecualian(Session session) {
		if (session != null && session.isOpen()) {
			try {
				session.disconnect();
			} catch (Exception ignored) {
				ais.common.ErrorAuditUtil.record(ignored,
						"Pengecualian tagihan: gagal disconnect session");
			}
			try {
				session.close();
			} catch (Exception ignored) {
				ais.common.ErrorAuditUtil.record(ignored,
						"Pengecualian tagihan: gagal close session");
			}
		}
		HibernateUtil.closeSession();
	}

	/** Ambil dan hapus stack trace exception dari simpanPembayaranMahasiswa (dipanggil oleh PaymentLogic). */
	public static String popLastSimpanException() {
		String e = lastSimpanException.get();
		lastSimpanException.remove();
		return e;
	}

	private PembayaranUtil() {

	}

	private static PembayaranUtil pembayaranUtil = new PembayaranUtil();

	public static PembayaranUtil getInstance() {
		return pembayaranUtil;
	}

	public Object[] ambilDataMahasiswa(String nim) {

		String bulan = null;
		String kode = null;
		boolean perbulan = Common.bolehKonfigurasi("aktifkan_biaya_host_to_host_per_bulan", Konfigurasi.TIDAK_AKTIF);
		Mahasiswa mahasiswa = null;

		if (perbulan) {
			try {
				int panjang = 2;

				bulan = nim.substring(nim.length() - panjang);
				String nimBaru = nim.substring(0, nim.length() - panjang);

				mahasiswa = ConstantValues.ambilByNim(nimBaru);
				if (mahasiswa == null) {
					mahasiswa = null;
					bulan = null;
				}

				if (!Common.isNumber(bulan)) {
					mahasiswa = null;
					bulan = null;
				}

				if (bulan != null && Common.isNumber(bulan)
						&& (Integer.parseInt(bulan.trim()) < 1 || Integer.parseInt(bulan.trim()) > 12)) {
					mahasiswa = null;
					bulan = null;
				}

				// System.out.println"aktifkan_biaya_host_to_host_per_bulan = "
				// + perbulan + ", bulan = " + bulan
				// + ", nim = " + nim + ", nimBaru = " + nimBaru + ", mahasiswa
				// = " + mahasiswa);

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		} else {

			boolean sisipkan = Common.bolehKonfigurasi("sisipkan_kode_pembayaran_di_akhir_pada_saat_pembayaran_via_h2h", Konfigurasi.TIDAK_AKTIF);

			if (sisipkan) {
				try {
					int panjang = Integer.parseInt(Common
							.getKonfigurasi("panjang_sisipkan_kode_pembayaran_di_akhir_pada_saat_pembayaran_via_h2h",
									"3")
							.getNilai().trim());

					kode = nim.substring(nim.length() - panjang);
					String nimBaru = nim.substring(0, nim.length() - panjang);

					mahasiswa = ConstantValues.ambilByNim(nimBaru);
					if (mahasiswa == null) {
						mahasiswa = ConstantValues.ambilByNim(nim);
						kode = null;
					}

					// System.out.println"sisipkan_kode_pembayaran_di_akhir_pada_saat_pembayaran_via_h2h
					// = " + sisipkan
					// + ", kode = " + kode + ", nim = " + nim + ", nimBaru = "
					// + nimBaru);

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			} else {
				mahasiswa = ConstantValues.ambilByNim(nim);
			}
		}

		return new Object[] { mahasiswa, kode, bulan };
	}

	public TunggakanMahasiswa updateTunggakan(Kegiatan kegiatan, Session session) {

		if (session == null) {
			session = HibernateUtil.currentNativeSession();
			TunggakanMahasiswa tunggakanMahasiswa = (TunggakanMahasiswa) session
					.createCriteria(TunggakanMahasiswa.class).add(Restrictions.eq("semester", kegiatan.getSemster()))
					.add(Restrictions.eq("jenisKegiatan", kegiatan.getJenisKegiatan()))
					.add(Restrictions.eq("mahasiswa", kegiatan.getMahasiswa())).setMaxResults(1).uniqueResult();

			if (kegiatan != null && tunggakanMahasiswa != null && tunggakanMahasiswa.getJumlahTunggakan() != null
					&& kegiatan.getAmount() != null) {
				Boolean dianggapLunas = kegiatan.getAmount().intValue() >= tunggakanMahasiswa.getJumlahTunggakan()
						.intValue();
				// System.out.println"==================================
				// mahasiswa ini sekalian membayar tunggakan "
				// + kegiatan.getMahasiswa().getNim() + ", lunas = " +
				// dianggapLunas + ", tunggakan = "
				// + tunggakanMahasiswa.getJumlahTunggakan() + ", dibayar = " +
				// kegiatan.getAmount()
				// + " ===================================");
				tunggakanMahasiswa.setDianggapLunas(dianggapLunas);
				tunggakanMahasiswa.setKegiatan(kegiatan);
				session.getTransaction().begin();
				session.update(tunggakanMahasiswa);
				session.getTransaction().commit();
			}

			HibernateUtil.closeSession();
			return tunggakanMahasiswa;
		} else {
			TunggakanMahasiswa tunggakanMahasiswa = (TunggakanMahasiswa) session
					.createCriteria(TunggakanMahasiswa.class).add(Restrictions.eq("semester", kegiatan.getSemster()))
					.add(Restrictions.eq("jenisKegiatan", kegiatan.getJenisKegiatan()))
					.add(Restrictions.eq("mahasiswa", kegiatan.getMahasiswa())).setMaxResults(1).uniqueResult();
			if (kegiatan != null && tunggakanMahasiswa != null && tunggakanMahasiswa.getJumlahTunggakan() != null
					&& kegiatan.getAmount() != null) {
				Boolean dianggapLunas = kegiatan.getAmount().intValue() >= tunggakanMahasiswa.getJumlahTunggakan()
						.intValue();
				// System.out.println"==================================
				// mahasiswa ini sekalian membayar tunggakan "
				// + kegiatan.getMahasiswa().getNim() + ", lunas = " +
				// dianggapLunas + ", tunggakan = "
				// + tunggakanMahasiswa.getJumlahTunggakan() + ", dibayar = " +
				// kegiatan.getAmount()
				// + " ===================================");
				tunggakanMahasiswa.setDianggapLunas(dianggapLunas);
				tunggakanMahasiswa.setKegiatan(kegiatan);
				session.update(tunggakanMahasiswa);
			}
			return tunggakanMahasiswa;
		}

	}

	public List<TunggakanMahasiswa> getTunggakanMahasiswa(JenisKegiatan[] jenisKegiatans, Mahasiswa mahasiswa,
			Session session) {
		Boolean ganjil = CommonUtil.isNowSemensterGanjil();
		Integer currentSemester = CommonUtil.getSemester(mahasiswa.getTahunangkatan(), ganjil,
				mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
		return getTunggakanMahasiswa(jenisKegiatans, mahasiswa, currentSemester, session);
	}

	@SuppressWarnings("unchecked")
	public List<TunggakanMahasiswa> getTunggakanMahasiswa(JenisKegiatan[] jenisKegiatans, Mahasiswa mahasiswa,
			Integer currentSemester, Session session) {
		if (session == null) {
			session = HibernateUtil.currentNativeSession();
			List<TunggakanMahasiswa> tunggakanMahasiswas = session.createCriteria(TunggakanMahasiswa.class)
					.add(Restrictions.ne("semester", currentSemester)).add(Restrictions.ge("jumlahTunggakan", 1.0))
					.add(Restrictions.or(Restrictions.eq("dianggapLunas", false), Restrictions.isNull("kegiatan")))
					.add(Restrictions.in("jenisKegiatan", jenisKegiatans)).add(Restrictions.eq("mahasiswa", mahasiswa))
					.list();

			HibernateUtil.closeSession();
			return tunggakanMahasiswas;
		} else {
			List<TunggakanMahasiswa> tunggakanMahasiswas = session.createCriteria(TunggakanMahasiswa.class)
					.createAlias("kegiatan", "kegiatan", Criteria.LEFT_JOIN)
					.add(Restrictions.ne("semester", currentSemester)).add(Restrictions.ge("jumlahTunggakan", 1.0))
					.add(Restrictions.or(Restrictions.eq("dianggapLunas", false), Restrictions.isNull("kegiatan")))
					.add(Restrictions.in("jenisKegiatan", jenisKegiatans)).add(Restrictions.eq("mahasiswa", mahasiswa))
					.list();
			return tunggakanMahasiswas;
		}

	}

	public Serializable[] getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(Date tanggal, JenisKegiatan jenisKegiatan,
			Jenjang jenjang, String tahunAkademik, Boolean ganjil, JenisSeleksi jenisSeleksi, String program,
			String nim, GelombangPendaftaran gelombangPendaftaran) {

		Session session = HibernateUtil.currentNativeSession();
		@SuppressWarnings("unchecked")
		List<JadwalPembayaran> jadwalPembayarans = session.createCriteria(JadwalPembayaran.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

						.add(jenisSeleksi == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("jenisSeleksi"),
										Restrictions.eq("jenisSeleksi", jenisSeleksi)))

						.add(gelombangPendaftaran == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("gelombangPendaftaran"),
										Restrictions.eq("gelombangPendaftaran", gelombangPendaftaran)))

						.add(jenjang == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("jenjang"), Restrictions.eq("jenjang", jenjang)))

						.add(program == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("program"), Restrictions.eq("program", program)))

						.add(restrictionKhususUntukNim(nim))

						.add(Restrictions.or(Restrictions.isNull("ganjil"), Restrictions.eq("ganjil", ganjil)))

						.add(Restrictions.or(Restrictions.isNull("tahunAkademik"),
								Restrictions.eq("tahunAkademik", tahunAkademik)))

						.add(Restrictions.eq("jenisKegiatan", jenisKegiatan))
						.add(tanggal == null ? Restrictions.sqlRestriction("true")
								: Restrictions.le("startDate", tanggal))
						.add(tanggal == null ? Restrictions.sqlRestriction("true")
								: Restrictions.ge("endDate", tanggal))
						.addOrder(Order.desc("id")).setMaxResults(100).list();
		JadwalPembayaran jadwalPembayaran = pilihJadwalPembayaranPalingSesuai(jadwalPembayarans, jenisSeleksi,
				gelombangPendaftaran, jenjang, program, nim, tahunAkademik, ganjil);

		// PERMINTAAN: bila TIDAK ADA Jadwal Pembayaran yang cocok PERSIS (termasuk Jenis Seleksi/
		// Gelombang Pendaftaran/Prodi), coba lagi dengan MENGABAIKAN SEPENUHNYA ketiga field itu
		// (bukan lagi "kosong ATAU sama" seperti pencarian ketat di atas) -- selama kriteria INTI
		// (Jenis Kegiatan, Jenjang, Tahun Akademik, Ganjil/Genap, rentang tanggal, & pembatasan NIM
		// khusus bila ada) tetap cocok. Ini mencegah mahasiswa gagal total membayar hanya karena
		// jadwal yang tersedia dikonfigurasi utk Jenis Seleksi/Jalur/Gelombang yang BERBEDA (mis.
		// jadwal "...Jalur Kerjasama" tidak otomatis mencakup mahasiswa jalur "Reguler"), padahal
		// jadwal itu semestinya tetap relevan utk jenjang+periode yang sama. Pencarian KETAT di
		// atas tetap diutamakan (dijalankan lebih dulu) -- pencarian longgar ini HANYA jadi
		// cadangan terakhir bila tidak ada satu pun jadwal yang cocok persis.
		if (jadwalPembayaran == null) {
			session = HibernateUtil.currentNativeSession();
			JadwalPembayaran jadwalLonggar = (JadwalPembayaran) ConstantValues.simpleObject(
				session.createCriteria(JadwalPembayaran.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

					.add(jenjang == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("jenjang"), Restrictions.eq("jenjang", jenjang)))

					.add(restrictionKhususUntukNim(nim))

					.add(Restrictions.or(Restrictions.isNull("ganjil"), Restrictions.eq("ganjil", ganjil)))

					.add(Restrictions.or(Restrictions.isNull("tahunAkademik"),
						Restrictions.eq("tahunAkademik", tahunAkademik)))

					.add(Restrictions.eq("jenisKegiatan", jenisKegiatan))
					.add(tanggal == null ? Restrictions.sqlRestriction("true")
						: Restrictions.le("startDate", tanggal))
					.add(tanggal == null ? Restrictions.sqlRestriction("true")
						: Restrictions.ge("endDate", tanggal))
					.addOrder(Order.desc("id")).setMaxResults(1),
				JadwalPembayaran.class);
			if (jadwalLonggar != null) {
				System.out.println("[Pembayaran] Jadwal dipakai via pencarian LONGGAR (Jenis Seleksi/Gelombang Pendaftaran/Prodi diabaikan): jadwalId="
							+ jadwalLonggar.getId() + " jenisKegiatan="
							+ (jenisKegiatan == null ? "null" : jenisKegiatan.getId()) + " jenjang="
							+ (jenjang == null ? "null" : jenjang.getId()) + " jenisSeleksiDiminta="
							+ (jenisSeleksi == null ? "null" : jenisSeleksi.getId()) + " gelombangDiminta="
							+ (gelombangPendaftaran == null ? "null" : gelombangPendaftaran.getId())
							+ " programDiminta=" + program + " nim=" + nim);
				jadwalPembayaran = jadwalLonggar;
			}
		}

		DendaPembayaran dendaPembayaran = null;
		DendaPembayaranNominal dendaPembayaranNominal = null;
		if (jadwalPembayaran == null) {
			// ConstantValues.simpleObject / helper bersarang bisa menutup native session
			// ThreadLocal — ambil ulang session yang DIJAMIN open sebelum dipakai lagi
			// (currentNativeSession mengembalikan session sama bila masih open, atau
			// membuka baru bila sudah tertutup).
			session = HibernateUtil.currentNativeSession();
			JadwalPembayaran myjadwalPembayaran = (JadwalPembayaran) ConstantValues.simpleObject(session
					.createCriteria(JadwalPembayaran.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

					.add(jenisSeleksi == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.isNull("jenisSeleksi"),
									Restrictions.eq("jenisSeleksi", jenisSeleksi)))

					.add(gelombangPendaftaran == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.isNull("gelombangPendaftaran"),
									Restrictions.eq("gelombangPendaftaran", gelombangPendaftaran)))

					.add(program == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.isNull("program"), Restrictions.eq("program", program)))

					.add(restrictionKhususUntukNim(nim))

					.add(jenjang == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.isNull("jenjang"), Restrictions.eq("jenjang", jenjang)))

					.add(Restrictions.or(Restrictions.isNull("ganjil"), Restrictions.eq("ganjil", ganjil)))
					.add(Restrictions.or(Restrictions.isNull("tahunAkademik"),
							Restrictions.eq("tahunAkademik", tahunAkademik)))

					.add(Restrictions.eq("jenisKegiatan", jenisKegiatan))
					.add(tanggal == null ? Restrictions.sqlRestriction("true") : Restrictions.le("startDate", tanggal))
					.addOrder(Order.desc("endDate")).addOrder(Order.desc("id")).setMaxResults(1),
					JadwalPembayaran.class);

			dendaPembayaran = getDendaPembayaran(tanggal, myjadwalPembayaran);
			if (dendaPembayaran != null || dendaPembayaranNominal != null) {
				jadwalPembayaran = dendaPembayaran.getJadwalPembayaran();
			}

			dendaPembayaranNominal = getDendaPembayaranNominal(tanggal, myjadwalPembayaran);
			if (dendaPembayaranNominal != null) {
				jadwalPembayaran = dendaPembayaranNominal.getJadwalPembayaran();
			}
		}

		if (jadwalPembayaran == null) {
			try {
				Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getMahasiswa() == null) {

					// getDendaPembayaran/getDendaPembayaranNominal/getCurrentUser di atas dapat
					// menutup native session ThreadLocal → "Session is closed!" di sini.
					// Ambil ulang session yang DIJAMIN open sebelum dipakai.
					session = HibernateUtil.currentNativeSession();
					jadwalPembayaran = (JadwalPembayaran) ConstantValues.simpleObject(session
							.createCriteria(JadwalPembayaran.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

							.add(jenisSeleksi == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(Restrictions.isNull("jenisSeleksi"),
											Restrictions.eq("jenisSeleksi", jenisSeleksi)))

							.add(gelombangPendaftaran == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(Restrictions.isNull("gelombangPendaftaran"),
											Restrictions.eq("gelombangPendaftaran", gelombangPendaftaran)))

							.add(jenjang == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(Restrictions.isNull("jenjang"),
											Restrictions.eq("jenjang", jenjang)))

							.add(program == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(Restrictions.isNull("program"),
											Restrictions.eq("program", program)))

							.add(restrictionKhususUntukNim(nim))

							.add(Restrictions.or(Restrictions.isNull("ganjil"), Restrictions.eq("ganjil", ganjil)))

							.add(Restrictions.or(Restrictions.isNull("tahunAkademik"),
									Restrictions.eq("tahunAkademik", tahunAkademik)))

							.add(Restrictions.eq("jenisKegiatan", jenisKegiatan))
							.add(Restrictions.eq("adminBolehMembayarkanDiluarjadwal", true))

							.addOrder(Order.desc("id")).setMaxResults(1), JadwalPembayaran.class);

				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/util/PembayaranUtil.java:402");
			}
		}
		// Native session (currentNativeSession) bisa SUDAH ditutup oleh helper bersarang
		// (getDendaPembayaran/simpleObject memakai ThreadLocal yang sama). disconnect() pada session
		// yang sudah tertutup melempar "Session is closed!". Karena itu cek isOpen dulu, lalu lepaskan
		// ThreadLocal lewat HibernateUtil.closeSession() yang idempoten.
		try {
			if (session != null && session.isOpen()) {
				session.disconnect();
				session.close();
			}
		} catch (Exception eClose) { ais.common.ErrorAuditUtil.record(eClose, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranUtil.java:414");
			// abaikan: session bisa sudah ditutup di tempat lain.
		}
		HibernateUtil.closeSession();
		return new Serializable[] { jadwalPembayaran, dendaPembayaran, dendaPembayaranNominal };
	}

	private Criterion restrictionKhususUntukNim(String nim) {
		if (nim == null || nim.trim().isEmpty()) {
			return Restrictions.sqlRestriction("1=1");
		}
		Criterion kosong = Restrictions.or(Restrictions.isNull("khususUntukNim"),
				Restrictions.in("khususUntukNim", new String[] { "", ",", ",,", ",,," }));
		return Restrictions.or(kosong, Restrictions.ilike("khususUntukNim", "," + nim.trim() + ",",
				MatchMode.ANYWHERE));
	}

	private JadwalPembayaran pilihJadwalPembayaranPalingSesuai(List<JadwalPembayaran> jadwals,
			JenisSeleksi jenisSeleksi, GelombangPendaftaran gelombangPendaftaran, Jenjang jenjang, String program,
			String nim, String tahunAkademik, Boolean ganjil) {
		if (jadwals == null || jadwals.isEmpty()) {
			return null;
		}
		JadwalPembayaran terbaik = null;
		int skorTerbaik = -1;
		for (JadwalPembayaran jadwal : jadwals) {
			if (jadwal == null) {
				continue;
			}
			int skor = 0;
			if (nim != null && jadwal.getKhususUntukNim() != null
					&& jadwal.getKhususUntukNim().contains("," + nim.trim() + ",")) {
				skor += 64;
			}
			if (jenisSeleksi != null && jadwal.getJenisSeleksi() != null
					&& jadwal.getJenisSeleksi().getId().equals(jenisSeleksi.getId())) {
				skor += 32;
			}
			if (gelombangPendaftaran != null && jadwal.getGelombangPendaftaran() != null
					&& jadwal.getGelombangPendaftaran().getId().equals(gelombangPendaftaran.getId())) {
				skor += 16;
			}
			if (program != null && jadwal.getProgram() != null
					&& jadwal.getProgram().trim().equalsIgnoreCase(program.trim())) {
				skor += 8;
			}
			if (jenjang != null && jadwal.getJenjang() != null && jadwal.getJenjang().getId().equals(jenjang.getId())) {
				skor += 4;
			}
			if (tahunAkademik != null && jadwal.getTahunAkademik() != null
					&& jadwal.getTahunAkademik().trim().equalsIgnoreCase(tahunAkademik.trim())) {
				skor += 2;
			}
			if (ganjil != null && jadwal.getGanjil() != null && jadwal.getGanjil().equals(ganjil)) {
				skor += 1;
			}
			if (skor > skorTerbaik) {
				skorTerbaik = skor;
				terbaik = jadwal;
			}
		}
		return terbaik;
	}

	public Boolean checkApakahBulanAdaDiJadwal(String bulan) {
		Boolean bulanSemesterGanjil = null;
		if (bulan != null && !bulan.trim().isEmpty() && Common.isNumber(bulan)) {
			int bln = Integer.parseInt(bulan);
			for (int b = 1; b <= 6; b++) {
				int realBulan = (ConstantValues.pembayaranSemesterGenapMulaiDiBulan - 1) + b;
				if (realBulan > 12) {
					realBulan = realBulan % 12;
				}
				if (realBulan == bln) {
					bulanSemesterGanjil = false;
					break;
				}
			}

			if (bulanSemesterGanjil == null) {
				for (int b = 1; b <= 6; b++) {
					int realBulan = (ConstantValues.pembayaranSemesterGanjilMulaiDiBulan - 1) + b;
					if (realBulan > 12) {
						realBulan = realBulan % 12;
					}
					if (realBulan == bln) {
						bulanSemesterGanjil = true;
						break;
					}
				}
			}
		}

		// System.out.println"bulan = " + bulan + " bulanSemesterGanil = " +
		// bulanSemesterGanjil);
		return bulanSemesterGanjil;
	}

	public Serializable[] getJadwalPembayaranDanDendaHanyaBerdasarJenisKegiatan(Date tanggal,
			JenisKegiatan jenisKegiatan, Jenjang jenjang, String bulan, JenisSeleksi jenisSeleksi, String program,
			String nim) {

		Boolean bulanSemesterGanjil = checkApakahBulanAdaDiJadwal(bulan);
		Session session = HibernateUtil.currentNativeSession();
		JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) ConstantValues
				.simpleObject(session.createCriteria(JadwalPembayaran.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(jenisSeleksi == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("jenisSeleksi"),
										Restrictions.eq("jenisSeleksi", jenisSeleksi)))
						.add(program == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("program"), Restrictions.eq("program", program)))
						.add(nim == null || nim.trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("khususUntukNim"),
										Restrictions.ilike("khususUntukNim", "," + nim + ",", MatchMode.ANYWHERE)))
						.add(jenjang == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("jenjang"), Restrictions.eq("jenjang", jenjang)))
						.add(Restrictions.eq("jenisKegiatan", jenisKegiatan)).add(Restrictions.le("startDate", tanggal))
						.add(Restrictions.ge("endDate", tanggal))
						.add(bulanSemesterGanjil == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("ganjil"),
										Restrictions.eq("ganjil", bulanSemesterGanjil)))
						.addOrder(Order.desc("endDate")).setMaxResults(1), JadwalPembayaran.class);
		// System.out.println"Current date : " + tanggal);

		DendaPembayaran dendaPembayaran = null;
		DendaPembayaranNominal dendaPembayaranNominal = null;
		if (jadwalPembayaran == null) {
			// ConstantValues.simpleObject / helper bersarang bisa menutup native session
			// ThreadLocal — ambil ulang session yang DIJAMIN open sebelum dipakai lagi
			// (currentNativeSession mengembalikan session sama bila masih open, atau
			// membuka baru bila sudah tertutup).
			session = HibernateUtil.currentNativeSession();
			JadwalPembayaran myjadwalPembayaran = (JadwalPembayaran) ConstantValues.simpleObject(session
					.createCriteria(JadwalPembayaran.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(jenisSeleksi == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.isNull("jenisSeleksi"),
									Restrictions.eq("jenisSeleksi", jenisSeleksi)))
					.add(program == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.isNull("program"), Restrictions.eq("program", program)))
					.add(nim == null || nim.trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.isNull("khususUntukNim"),
									Restrictions.ilike("khususUntukNim", "," + nim + ",", MatchMode.ANYWHERE)))
					.add(jenjang == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.isNull("jenjang"), Restrictions.eq("jenjang", jenjang)))
					.add(Restrictions.eq("jenisKegiatan", jenisKegiatan)).add(Restrictions.le("startDate", tanggal))
					.add(bulanSemesterGanjil == null ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.isNull("ganjil"),
									Restrictions.eq("ganjil", bulanSemesterGanjil)))
					.addOrder(Order.desc("endDate")).addOrder(Order.desc("endDate")).addOrder(Order.desc("startDate"))
					.setMaxResults(1), JadwalPembayaran.class);

			dendaPembayaran = getDendaPembayaran(tanggal, myjadwalPembayaran);
			if (dendaPembayaran != null || dendaPembayaranNominal != null) {
				jadwalPembayaran = dendaPembayaran.getJadwalPembayaran();
			}

			dendaPembayaranNominal = getDendaPembayaranNominal(tanggal, myjadwalPembayaran);
			if (dendaPembayaranNominal != null) {
				jadwalPembayaran = dendaPembayaranNominal.getJadwalPembayaran();
			}
		}

		if (jadwalPembayaran == null) {
			try {
				Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getMahasiswa() == null) {

					// getDendaPembayaran/getDendaPembayaranNominal/getCurrentUser di atas dapat
					// menutup native session ThreadLocal → "Session is closed!" di sini.
					// Ambil ulang session yang DIJAMIN open sebelum dipakai.
					session = HibernateUtil.currentNativeSession();
					jadwalPembayaran = (JadwalPembayaran) ConstantValues.simpleObject(session
							.createCriteria(JadwalPembayaran.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(jenisSeleksi == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(Restrictions.isNull("jenisSeleksi"),
											Restrictions.eq("jenisSeleksi", jenisSeleksi)))
							.add(program == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(Restrictions.isNull("program"),
											Restrictions.eq("program", program)))
							.add(nim == null || nim.trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(Restrictions.isNull("khususUntukNim"),
											Restrictions.ilike("khususUntukNim", "," + nim + ",", MatchMode.ANYWHERE)))
							.add(jenjang == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(Restrictions.isNull("jenjang"),
											Restrictions.eq("jenjang", jenjang)))
							.add(Restrictions.eq("jenisKegiatan", jenisKegiatan))

							.add(Restrictions.eq("adminBolehMembayarkanDiluarjadwal", true))

							.add(bulanSemesterGanjil == null ? Restrictions.sqlRestriction("true")
									: Restrictions.or(Restrictions.isNull("ganjil"),
											Restrictions.eq("ganjil", bulanSemesterGanjil)))
							.addOrder(Order.desc("endDate")).setMaxResults(1), JadwalPembayaran.class);

				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/util/PembayaranUtil.java:556");
			}
		}

		HibernateUtil.closeSession();
		return new Serializable[] { jadwalPembayaran, dendaPembayaran, dendaPembayaranNominal };
	}

	public Serializable[] getJadwalPembayaranDanDendaIgnoreStart(Date tanggal, JenisKegiatan jenisKegiatan,
			Jenjang jenjang, String tahunAkademik, Boolean ganjil, JenisSeleksi jenisSeleksi, String program,
			String nim) {
		Session session = HibernateUtil.currentNativeSession();

		Number jumahJadwalPembayaran = (Number) session.createCriteria(JadwalPembayaran.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(jenisSeleksi == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("jenisSeleksi"),
								Restrictions.eq("jenisSeleksi", jenisSeleksi)))
				.add(jenjang == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("jenjang"), Restrictions.eq("jenjang", jenjang)))
				.add(Restrictions.or(Restrictions.isNull("ganjil"), Restrictions.eq("ganjil", ganjil)))
				.add(Restrictions.or(Restrictions.isNull("tahunAkademik"),
						Restrictions.eq("tahunAkademik", tahunAkademik)))
				.add(Restrictions.eq("jenisKegiatan", jenisKegiatan)).setProjection(Projections.rowCount())
				.uniqueResult();
		// System.out.println"jumahJadwalPembayaran : " +
		// jumahJadwalPembayaran);

		if (jumahJadwalPembayaran.intValue() > 0) {

			JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) session.createCriteria(JadwalPembayaran.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(jenisSeleksi == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.isNull("jenisSeleksi"),
									Restrictions.eq("jenisSeleksi", jenisSeleksi)))
					.add(program == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.isNull("program"), Restrictions.eq("program", program)))
					.add(nim == null || nim.trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.isNull("khususUntukNim"),
									Restrictions.ilike("khususUntukNim", "," + nim + ",", MatchMode.ANYWHERE)))
					.add(jenjang == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.isNull("jenjang"), Restrictions.eq("jenjang", jenjang)))
					.add(Restrictions.or(Restrictions.isNull("ganjil"), Restrictions.eq("ganjil", ganjil)))
					.add(Restrictions.or(Restrictions.isNull("tahunAkademik"),
							Restrictions.eq("tahunAkademik", tahunAkademik)))
					.add(Restrictions.eq("jenisKegiatan", jenisKegiatan)).add(Restrictions.ge("endDate", tanggal))
					.setMaxResults(1).addOrder(Order.asc("startDate")).uniqueResult();
			// System.out.println"Current date : " + tanggal);

			DendaPembayaran dendaPembayaran = null;
			DendaPembayaranNominal dendaPembayaranNominal = null;
			if (jadwalPembayaran == null) {
				JadwalPembayaran myjadwalPembayaran = (JadwalPembayaran) session.createCriteria(JadwalPembayaran.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(program == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("program"), Restrictions.eq("program", program)))
						.add(nim == null || nim.trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("khususUntukNim"),
										Restrictions.ilike("khususUntukNim", "," + nim + ",", MatchMode.ANYWHERE)))
						.add(jenisSeleksi == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("jenisSeleksi"),
										Restrictions.eq("jenisSeleksi", jenisSeleksi)))
						.add(jenjang == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("jenjang"), Restrictions.eq("jenjang", jenjang)))
						.add(Restrictions.or(Restrictions.isNull("ganjil"), Restrictions.eq("ganjil", ganjil)))
						.add(Restrictions.or(Restrictions.isNull("tahunAkademik"),
								Restrictions.eq("tahunAkademik", tahunAkademik)))
						.add(Restrictions.eq("jenisKegiatan", jenisKegiatan)).add(Restrictions.le("startDate", tanggal))
						.addOrder(Order.desc("endDate")).setMaxResults(1).uniqueResult();
				dendaPembayaran = getDendaPembayaran(tanggal, myjadwalPembayaran);
				if (dendaPembayaran != null || dendaPembayaranNominal != null) {
					jadwalPembayaran = dendaPembayaran.getJadwalPembayaran();
				}

				dendaPembayaranNominal = getDendaPembayaranNominal(tanggal, myjadwalPembayaran);
				if (dendaPembayaranNominal != null) {
					jadwalPembayaran = dendaPembayaranNominal.getJadwalPembayaran();
				}
			}

			HibernateUtil.closeSession();
			return new Serializable[] { jadwalPembayaran, dendaPembayaran, dendaPembayaranNominal };
		} else {
			return new Serializable[] { new JadwalPembayaran(), null, null };
		}
	}

	public JadwalPembayaran getJadwalPembayaranTanpaDibatasiWaktu(JenisKegiatan jenisKegiatan, String tahunAkademik,
			Boolean ganjil, JenisSeleksi jenisSeleksi, String program, String nim) {
		Session session = HibernateUtil.currentNativeSession();
		JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) session.createCriteria(JadwalPembayaran.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(jenisSeleksi == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("jenisSeleksi"),
								Restrictions.eq("jenisSeleksi", jenisSeleksi)))
				.add(program == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("program"), Restrictions.eq("program", program)))
				.add(nim == null || nim.trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("khususUntukNim"),
								Restrictions.ilike("khususUntukNim", "," + nim + ",", MatchMode.ANYWHERE)))
				.add(Restrictions.eq("jenisKegiatan", jenisKegiatan))
				.add(Restrictions.or(Restrictions.eq("ganjil", ganjil), Restrictions.isNull("ganjil")))
				.add(Restrictions.or(Restrictions.eq("tahunAkademik", tahunAkademik),
						Restrictions.isNull("tahunAkademik")))
				.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
		// System.out.println"Current date : " +
		// ais.ui.util.WaktuUtil.getDate());

		HibernateUtil.closeSession();
		return jadwalPembayaran;
	}

	private DendaPembayaran getDendaPembayaran(Date tanggal, JadwalPembayaran jadwalPembayaran) {
		// System.out
		// .println(jadwalPembayaran == null ? "" :
		// jadwalPembayaran.getEndDate() + " jadwal pembayaran selesai");
		if (jadwalPembayaran == null) {
			return null;
		}

		Date expiryDate = jadwalPembayaran.getEndDate();
		Date currentDate = tanggal;
		long diff = 0L;
		// Error D: `tanggal` (currentDate) bisa null -- dipanggil dari alur sinkronisasi
		// batch (CetakRegistrasiAction.singkronkanDenganPembayaran via
		// KegiatanHelper.checkKegiatanCalonMahasiswa -> getJadwalPembayaranDanDendaBerdasarkanTahunAkademik
		// dengan tanggal=null secara sengaja). Sebelumnya currentDate.getTime() NPE
		// langsung. Perlakukan sama seperti expiryDate null: diff tetap 0 (default),
		// TIDAK mengubah hasil saat kedua tanggal tersedia.
		if (expiryDate != null && currentDate != null) {
			diff = currentDate.getTime() - expiryDate.getTime();
			diff = (diff / (1000 * 60 * 60 * 24));
		}
		// System.out.println"jadwalPembayaran " +
		// (jadwalPembayaran.getJenisKegiatan() == null ? ""
		// : jadwalPembayaran.getJenisKegiatan().getNamaKegiatan() + " tgl " +
		// jadwalPembayaran.getEndDate()));
		// System.out.println"Keterlambatan " + diff + " hari");
		Session session = HibernateUtil.currentNativeSession();
		DendaPembayaran dendaPembayaran = (DendaPembayaran) session.createCriteria(DendaPembayaran.class)
				.add(Restrictions.le("mulai", new Long(diff).intValue()))
				.add(Restrictions.ge("sampai", new Long(diff).intValue()))
				.add(Restrictions.eq("jadwalPembayaran", jadwalPembayaran)).setMaxResults(1).uniqueResult();
		// System.out.println"dendaPembayaran " + dendaPembayaran);
		if (dendaPembayaran != null) {
			// System.out.println"Mulai " + dendaPembayaran.getMulai());
			// System.out.println"Sampai " + dendaPembayaran.getSampai());
			// System.out.println"Prosentase Denda " +
			// dendaPembayaran.getDenda());
		}

		HibernateUtil.closeSession();
		return dendaPembayaran;
	}

	private DendaPembayaranNominal getDendaPembayaranNominal(Date tanggal, JadwalPembayaran jadwalPembayaran) {

		if (jadwalPembayaran == null || tanggal == null) {
			return null;
		}

		Date expiryDate = jadwalPembayaran.getEndDate();
		Date currentDate = tanggal;
		long diff = 0L;
		if (expiryDate != null) {
			diff = currentDate.getTime() - expiryDate.getTime();
			diff = (diff / (1000 * 60 * 60 * 24));
		}

		Session session = HibernateUtil.currentNativeSession();
		DendaPembayaranNominal dendaPembayaranNominal = (DendaPembayaranNominal) session
				.createCriteria(DendaPembayaranNominal.class).add(Restrictions.le("mulai", new Long(diff).intValue()))
				.add(Restrictions.ge("sampai", new Long(diff).intValue()))
				.add(Restrictions.eq("jadwalPembayaran", jadwalPembayaran)).setMaxResults(1).uniqueResult();

		HibernateUtil.closeSession();
		return dendaPembayaranNominal;
	}

	@SuppressWarnings("unchecked")
	public void constructDetailBiayaFromBeasiswa(JenisKegiatan jenisKegiatan, Beasiswa beasiswa,
			Collection<DetailBiaya> detailBiayas) {
		if (detailBiayas.size() == 0) {
			return;
		}
		Double total = 0.0;
		DetailBiaya defaultBiaya = null;

		for (DetailBiaya biaya : detailBiayas) {
			defaultBiaya = biaya;
			Double nilai = biaya.hitungTotal();
			total += (nilai);
		}

		Session session = HibernateUtil.currentNativeSession();
		List<BeasiswaPunyaItemBiayaTambahan> itemBiaya = session.createCriteria(BeasiswaPunyaItemBiayaTambahan.class)
				.add(Restrictions.eq("beasiswa", beasiswa)).addOrder(Order.asc("id")).list();
		for (BeasiswaPunyaItemBiayaTambahan beasiswaPunyaItemBiayaTambahan : itemBiaya) {

			DetailBiaya detailBiaya = new DetailBiaya();
			detailBiaya.setAngkatan(defaultBiaya.getAngkatan());
			detailBiaya.setFakultas(defaultBiaya.getFakultas());
			detailBiaya.setJenisSeleksi(defaultBiaya.getJenisSeleksi());
			detailBiaya.setJenjang(defaultBiaya.getJenjang());
			detailBiaya.setWnaAtauWni(defaultBiaya.getWnaAtauWni() + "___BEASISWA");
			detailBiaya.setJurusan(defaultBiaya.getJurusan());
			detailBiaya.setProgram(defaultBiaya.getProgram());
			detailBiaya.setSemester(defaultBiaya.getSemester());
			detailBiaya.setStatusMahasiswa(defaultBiaya.getStatusMahasiswa());
			detailBiaya.setTahunAkademik(defaultBiaya.getTahunAkademik());

			detailBiaya.setItemBiaya(beasiswaPunyaItemBiayaTambahan.getItemBiaya());
			detailBiaya.setJenisKegiatan(jenisKegiatan);
			detailBiaya.setNama("Untuk pembayaran Denda");
			detailBiaya.setNilaiBiaya(beasiswaPunyaItemBiayaTambahan.getJumlah());

			session.getTransaction().begin();
			session.save(detailBiaya);
			session.getTransaction().commit();
			detailBiayas.add(detailBiaya);
		}

		HibernateUtil.closeSession();

	}

	public BankHost getBankHost() {
		return getBankHost(null);
	}

	public BankHost getBankHost(HttpServletRequest request) {
		if (request != null) {
			String ipAdd = request.getRemoteAddr();

			try {
				if (request != null && request.getHeader("Cf-Connecting-Ip") != null) {
					ipAdd = request.getHeader("Cf-Connecting-Ip");
				} else if (request != null && request.getHeader("CF-Connecting-IP") != null) {
					ipAdd = request.getHeader("CF-Connecting-IP");
				} else if (request != null && request.getHeader("X-Forwarded-For") != null) {
					ipAdd = request.getHeader("X-Forwarded-For");
				} else if (request != null && request.getHeader("X-Real-IP") != null) {
					ipAdd = request.getHeader("X-Real-IP");
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranUtil.java:794");
				// TODO: handle exception
			}

			return getBankHost(ipAdd, "Default Bank");
		} else {
			try {
				String ipAdd = ((HttpServletRequest) MessageContext.getCurrentContext()
						.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST)).getRemoteAddr();

				try {
					if (((HttpServletRequest) MessageContext.getCurrentContext()
							.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST)).getHeader("Cf-Connecting-Ip") != null) {
						ipAdd = ((HttpServletRequest) MessageContext.getCurrentContext()
								.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST)).getHeader("Cf-Connecting-Ip");
					} else if (((HttpServletRequest) MessageContext.getCurrentContext()
							.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST)).getHeader("CF-Connecting-IP") != null) {
						ipAdd = ((HttpServletRequest) MessageContext.getCurrentContext()
								.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST)).getHeader("CF-Connecting-IP");
					} else if (((HttpServletRequest) MessageContext.getCurrentContext()
							.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST)).getHeader("X-Forwarded-For") != null) {
						ipAdd = ((HttpServletRequest) MessageContext.getCurrentContext()
								.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST)).getHeader("X-Forwarded-For");
					} else if (((HttpServletRequest) MessageContext.getCurrentContext()
							.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST)).getHeader("X-Real-IP") != null) {
						ipAdd = ((HttpServletRequest) MessageContext.getCurrentContext()
								.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST)).getHeader("X-Real-IP");
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranUtil.java:822");
					// TODO: handle exception
				}

				return getBankHost(ipAdd, "Default Bank");
			} catch (Exception e) {
				return getBankHost("127.0.0.1", "Default Bank");
			}
		}
	}

	public BankHost getBankHost(String ipAdd, String nama) {
		if (ipAdd == null || ipAdd.trim().isEmpty()) {
			return null;
		}
		Session session = HibernateUtil.currentNativeSession();
		BankHost bankHost = (BankHost) session.createCriteria(BankHost.class).add(Restrictions.eq("ip", ipAdd))
				.setMaxResults(1).uniqueResult();

		if (bankHost == null
				&& Common.bolehKonfigurasi("apabila_bank_host_tidak_ditemukan_buat_data_bank_otomatis")) {
			bankHost = new BankHost();
			bankHost.setIp(ipAdd);
			bankHost.setNama(nama);
			session.getTransaction().begin();
			session.save(bankHost);
			session.getTransaction().commit();
		}

		if (bankHost == null) {
			bankHost = (BankHost) session.createCriteria(BankHost.class).add(Restrictions.eq("ip", "0.0.0.0"))
					.setMaxResults(1).uniqueResult();
		}

		HibernateUtil.closeSession();
		return bankHost;
	}

	public BiodataCalonMahasiswa getCalonMahasiswaByNoPendaftaran(String noRegistrasi) {
		Session session = HibernateUtil.currentNativeSession();
		BiodataCalonMahasiswa biodataCalonMahasiswa = null;
		try {
			biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues.simpleObject(session
					.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.ne("noRegistrasi", "0"))
					.add(Restrictions.isNotNull("noRegistrasi"))
					.add(Restrictions.ilike("noRegistrasi", noRegistrasi.trim(), MatchMode.EXACT)).setMaxResults(1),
					BiodataCalonMahasiswa.class);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		HibernateUtil.closeSession();
		return biodataCalonMahasiswa;
	}

	public BiodataCalonMahasiswa getCalonMahasiswaByNoUjian(String noUjian) {
		if (noUjian == null || noUjian.trim().length() == 0) {
			return null;
		}

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			@SuppressWarnings("unchecked")
			List<BiodataCalonMahasiswa> daftar = session.createCriteria(BiodataCalonMahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.ne("noUjian", "0"))
					.add(Restrictions.ne("noUjian", ""))
					.add(Restrictions.isNotNull("noUjian"))
					.add(Restrictions.isNotNull("prodiLulus"))
					.add(Restrictions.ilike("noUjian", noUjian.trim(), MatchMode.EXACT))
					.addOrder(Order.desc("id")).list();

			if (daftar == null || daftar.isEmpty()) {
				return null;
			}

			/*
			 * Nomor ujian secara historis dapat dipakai ulang pada tahun/gelombang
			 * berbeda. Query lama memakai setMaxResults(1) tanpa urutan sehingga H2H
			 * dapat mengembalikan calon mahasiswa lain. Prioritaskan pemilik tagihan
			 * daftar ulang yang nyata, lalu data pada tahun akademik berjalan. Urutan ID
			 * menurun di atas menjadi pemutus terakhir agar hasil selalu deterministik.
			 */
			String tahunAkademikAktif = Common.getCurrentTahunAkademik();
			BiodataCalonMahasiswa terbaik = null;
			int skorTerbaik = Integer.MIN_VALUE;
			for (BiodataCalonMahasiswa calon : daftar) {
				int skor = 0;
				Kegiatan tagihanDaftarUlang = calon.getPembayaranDaftarUlang();
				if (tagihanDaftarUlang != null && tagihanDaftarUlang.getId() != null) {
					skor += 200;
					if (tagihanDaftarUlang.getTagihan() != null && tagihanDaftarUlang.getTagihan().doubleValue() > 0.01d) {
						skor += 1000;
					}
				}
				String tahunAkademikCalon = calon.getTahunAkademik();
				if (tahunAkademikAktif != null && tahunAkademikAktif.equals(tahunAkademikCalon)) {
					skor += 100;
				}
				if (terbaik == null || skor > skorTerbaik) {
					terbaik = calon;
					skorTerbaik = skor;
				}
			}
			return terbaik;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit PembayaranUtil.getCalonMahasiswaByNoUjian - resolusi nomor ujian H2H");
			return null;
		} finally {
			if (session != null) {
				try {
					session.clear();
				} catch (Exception ignored) {
				}
				try {
					session.disconnect();
				} catch (Exception ignored) {
				}
				try {
					session.close();
				} catch (Exception ignored) {
				}
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public int countBulanan(Session session, Mahasiswa mahasiswa, JenisKegiatan jenisKegiatan, Integer semester,
			Collection detailBiayas, boolean reload, boolean comitManual) {
		return countBulanan(session, mahasiswa, null, jenisKegiatan, semester, detailBiayas, reload, comitManual);
	}

	@SuppressWarnings("rawtypes")
	public int countBulanan(Session session, BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan,
			Integer semester, Collection detailBiayas, boolean reload, boolean comitManual) {
		return countBulanan(session, null, biodataCalonMahasiswa, jenisKegiatan, semester, detailBiayas, reload,
				comitManual);
	}

	/**
	 * Menghitung jumlah baris bulanan ({@link PengaturanPembayaranBulanan} aktif) yang
	 * <b>benar-benar ada</b> untuk kombinasi jenis kegiatan + jenjang + SEMESTER tertentu —
	 * sumber kebenaran tunggal untuk memutuskan apakah sebuah semester bermode bulanan.
	 * <p>
	 * Latar belakang: flag "hanya berupa angsuran" pada {@link JenisKegiatan} berlaku
	 * per-jenjang (jenjangAngsuranJson), sedangkan konfigurasi billing di Setting Biaya
	 * dibuat PER SEMESTER — contoh nyata: program S2 semester 1-3 ditagih bulanan namun
	 * semester 4 ditagih sekali. Keputusan mode tidak boleh berhenti di aturan jenjang;
	 * ia harus diverifikasi terhadap kenyataan data billing semester tersebut. Method ini
	 * dipakai oleh {@code countBulanan} (sisi WS ini maupun kembaran
	 * {@code PembayaranUtilHelper.countBulanan}) dan oleh guard varian default di
	 * {@code getDetailBiayaMahasiswadariDatabase}.
	 * <p>
	 * Dua jalur pencarian: (1) bila pemanggil sudah memegang daftar {@link DetailBiaya}
	 * non-default milik semester berjalan, hitung PPB yang menempel pada daftar itu
	 * (paling akurat); (2) bila daftar kosong (mis. varian default sedang dikosongkan
	 * oleh guard angsuran), cari langsung berdasarkan jenis kegiatan + jenjang + semester
	 * (baris ber-semester {@code null} dianggap berlaku untuk semua semester). Seluruh
	 * baris bulanan aktif dihitung apa adanya — status "Tagihan Default" pada SettingBiaya
	 * TIDAK menyaring hitungan (revert 07-17 atas permintaan pengguna).
	 * Session lokal dibuka hanya bila session kiriman tidak dapat dipakai dan selalu
	 * ditutup di blok {@code finally}.
	 *
	 * @param session               session aktif (boleh {@code null}/tertutup — dibuka lokal)
	 * @param jenisKegiatan         jenis pembayaran yang sedang diproses (wajib)
	 * @param jenjang               jenjang mahasiswa/calon; {@code null} = tanpa filter jenjang
	 * @param semester              semester yang sedang dilihat; {@code null} = tanpa filter
	 * @param angkatan              tahun angkatan mahasiswa/calon; {@code null} = tanpa filter
	 *                              (baris ber-angkatan {@code null} dianggap semua angkatan)
	 * @param detailBiayasNonDefault daftar DetailBiaya milik semester ini (boleh kosong)
	 * @return jumlah baris bulanan nyata (0 = semester ini BUKAN bulanan), atau {@code -1}
	 *         bila pengecekan gagal — pemanggil wajib mempertahankan perilaku lamanya
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static int hitungBarisBulananSemester(Session session, JenisKegiatan jenisKegiatan, Jenjang jenjang,
			Integer semester, Integer angkatan, java.util.Collection detailBiayasNonDefault) {
		if (jenisKegiatan == null || jenisKegiatan.getId() == null) {
			return -1;
		}
		Session sesi = session;
		boolean sesiLokal = false;
		try {
			if (sesi == null || !sesi.isOpen()) {
				sesi = HibernateUtil.getSessionFactory().openSession();
				sesiLokal = true;
			}

			// Jalur 1: daftar DetailBiaya semester ini sudah di tangan → paling akurat.
			java.util.List<DetailBiaya> daftar = new java.util.ArrayList<DetailBiaya>();
			if (detailBiayasNonDefault != null) {
				for (Object o : detailBiayasNonDefault) {
					if (o instanceof DetailBiaya && ((DetailBiaya) o).getId() != null) {
						daftar.add((DetailBiaya) o);
					}
				}
			}

			Criteria criteria = sesi.createCriteria(PengaturanPembayaranBulanan.class)
					.createAlias("detailBiaya", "db")
					.createAlias("db.itemBiaya", "dbItem")
					.add(Restrictions.eq("aktif", true))
					.add(Restrictions.or(
							Restrictions.eq("dbItem.penghitungan", ItemBiaya.DIKALI_NILAI_MINUS),
							Restrictions.gt("nominal", 0.01)));
			if (!daftar.isEmpty()) {
				criteria.add(Restrictions.in("detailBiaya", daftar));
			} else {
				// Jalur 2: cari langsung per jenis+jenjang+semester+angkatan
				// (nilai null pada baris billing berarti berlaku untuk semua).
				criteria.add(Restrictions.eq("db.jenisKegiatan", jenisKegiatan));
				if (jenjang != null && jenjang.getId() != null) {
					criteria.add(Restrictions.eq("db.jenjang", jenjang));
				}
				if (semester != null) {
					criteria.add(Restrictions.or(Restrictions.eq("db.semester", semester),
							Restrictions.isNull("db.semester")));
				}
				if (angkatan != null) {
					criteria.add(Restrictions.or(Restrictions.eq("db.angkatan", angkatan),
							Restrictions.isNull("db.angkatan")));
				}
			}

			// (revert 07-17) Status "Tagihan Default" TIDAK lagi menyaring hitungan —
			// seluruh baris bulanan aktif dihitung apa adanya.
			java.util.List<PengaturanPembayaranBulanan> hasil = criteria.list();
			return hasil == null ? 0 : hasil.size();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "hitungBarisBulananSemester gagal; jk="
					+ jenisKegiatan.getId() + ", jenjang=" + (jenjang == null ? null : jenjang.getId())
					+ ", smt=" + semester);
			return -1;
		} finally {
			if (sesiLokal && sesi != null && sesi.isOpen()) {
				try {
					sesi.close();
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "hitungBarisBulananSemester: gagal tutup session lokal");
				}
			}
		}
	}

	@SuppressWarnings({ "rawtypes" })
	public int countBulanan(Session session, Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa,
			JenisKegiatan jenisKegiatan, Integer semester, Collection detailBiayas, boolean reload,
			boolean comitManual) {
		if (PengecualianTagihanList.adalah(detailBiayas)) {
			return 0;
		}

		if (jenisKegiatan != null && jenisKegiatan.getHanyaBerupaAngsuran()) {
			// Aturan "hanya berupa angsuran" berlaku PER-JENJANG (jenjangAngsuranJson) —
			// selaras kembaran PembayaranUtilHelper.countBulanan. Flag global saja pernah
			// memaksa SEMUA jenjang ke mode angsuran, padahal aturannya bisa hanya untuk
			// jenjang tertentu (mis. S2) — jenjang lain harus tetap dihitung dari database.
			Jenjang jenjangMhs = null;
			if (mahasiswa != null) {
				jenjangMhs = mahasiswa.getJurusan() != null
						? mahasiswa.getJurusan().getJenjang() : mahasiswa.getJenjang();
			} else if (biodataCalonMahasiswa != null) {
				jenjangMhs = biodataCalonMahasiswa.getJenjang();
			}
			Integer angkatanMhs = mahasiswa != null ? mahasiswa.getTahunangkatan()
					: (biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getTahun() : null);
			Boolean modeAngsuranJenjang = jenisKegiatan.modeAngsuranUntukJenjang(jenjangMhs, semester, angkatanMhs);
			if (Boolean.TRUE.equals(modeAngsuranJenjang)) {
				// PER-SEMESTER: aturan per-jenjang tidak boleh menimpa kenyataan billing.
				// Konfigurasi bulanan dibuat per semester (contoh nyata: S2 smt 1-3 bulanan,
				// smt 4 sekali tagih) — maka hitung baris bulanan yang BENAR-BENAR ada untuk
				// semester ini; 0 berarti semester ini bukan bulanan meski jenjang ditandai
				// harus angsuran. -1 = pengecekan gagal → pertahankan perilaku lama (paksa 1).
				// CATATAN (revert 07-17): status "Tagihan Default" di SettingBiaya TIDAK lagi
				// memaksa mode menjadi bukan-bulanan.
				int nyata = hitungBarisBulananSemester(session, jenisKegiatan, jenjangMhs, semester, angkatanMhs,
						detailBiayas);
				if (nyata >= 0) {
					return nyata;
				}
				return 1;
			}
			if (Boolean.FALSE.equals(modeAngsuranJenjang) && detailBiayas != null && !detailBiayas.isEmpty()) {
				// Jenjang ini EKSPLISIT bukan-angsuran dan billing reguler tersedia →
				// jangan hitung baris bulanan yang mungkin terlanjur ada.
				return 0;
			}
			// modeAngsuranJenjang == null (jenjang tak terkena aturan) → hitung dari DB di bawah
		}

		String key = (biodataCalonMahasiswa != null ? "cln_mhs_" + biodataCalonMahasiswa.getId()
				: "mhs_" + mahasiswa.getId()) + "_" + jenisKegiatan.getId() + "_" + semester;

		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject = Common.getJSONTemporary(biodataCalonMahasiswa != null ? biodataCalonMahasiswa : mahasiswa,
					key);
			if (!reload) {
				boolean ada = !jsonObject.isNull(key);
				// System.out.println"countBulanan = " + key + ", ada = " + ada
				// + " " + jsonObject);
				if (ada) {
					return jsonObject.getInt(key);
				}

			}
		} catch (Exception e1) {
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/ws/util/PembayaranUtil.java:937");
		}
		// (revert 07-17) Status "Tagihan Default" di SettingBiaya TIDAK lagi menyaring
		// penghitung mode angsuran — seluruh DetailBiaya dihitung apa adanya.
		int countPengaturanBulanan = 0;
		try {
			countPengaturanBulanan = ((Number) (detailBiayas == null || detailBiayas.isEmpty() ? 0
					: session.createCriteria(PengaturanPembayaranBulanan.class)
							.createAlias("detailBiaya", "detailBiaya").createAlias("detailBiaya.itemBiaya", "itemBiaya")
							.add(Restrictions.eq("aktif", true)).add(Restrictions.in("detailBiaya", detailBiayas))
							.add(Restrictions.or(
									Restrictions.eq("tetapDitampilkanWalaupunNol", true),
									Restrictions.or(
											Restrictions.eq("itemBiaya.penghitungan", ItemBiaya.DIKALI_NILAI_MINUS),
											Restrictions.gt("nominal", 0.01))))
							.setProjection(Projections.rowCount()).uniqueResult()))
					.intValue();

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/util/PembayaranUtil.java:961");
		}

		System.out.println("detailBiayas = " + (detailBiayas == null ? null : detailBiayas.size())
				+ ", countPengaturanBulanan = " + countPengaturanBulanan);

		try {
			jsonObject.put(key, countPengaturanBulanan);

			Common.setJSONTemporary(biodataCalonMahasiswa != null ? biodataCalonMahasiswa : mahasiswa, key, jsonObject);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/util/PembayaranUtil.java:973");
		}
		return countPengaturanBulanan;
	}

	/**
	 * Apakah sebuah {@link DetailBiaya} merupakan "tagihan default" — berasal dari
	 * {@link SettingBiaya} yang dicentang <i>"Gunakan Nilai Tagihan Default, jika dipilih,
	 * maka tagihan ini tidak perlu diinputkan di menu billing/angsuran"</i>. Tagihan
	 * seperti ini ditagih langsung dari nilai setting dan TIDAK boleh diperlakukan sebagai
	 * tagihan bulanan/angsuran. Relasi setting dicek dari FK langsung {@code settingBiaya}
	 * maupun via {@code detailSettingBiaya}; DetailBiaya tanpa keterkaitan setting (mis.
	 * dibuat manual dari menu billing) dianggap BUKAN default sehingga tetap mengikuti
	 * alur angsuran.
	 */
	public static boolean apakahTagihanDefault(DetailBiaya detailBiaya) {
		try {
			if (detailBiaya == null) {
				return false;
			}
			SettingBiaya settingBiaya = detailBiaya.getSettingBiayaEfektif();
			return settingBiaya != null && Boolean.TRUE.equals(settingBiaya.getGunakanBiayaDefault());
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Apakah sebuah item tagihan BOLEH DIANGSUR/DICICIL (nominal boleh diubah saat bayar).
	 * Mengikuti flag pada ItemBiaya: {@code mahasiswaBolehMencicilkan} (pengguna
	 * mahasiswa) / {@code adminBolehMencicilkan} (pengguna admin) — sebagaimana
	 * checkout JSP.
	 * <p>
	 * CATATAN (revert 07-17, permintaan user): override lama "item milik SettingBiaya
	 * ber-Tagihan Default = Ya SELALU wajib dibayar penuh" sudah DICABUT — layar
	 * DaftarUlangMahasiswa*Action memang menerima pembayaran bertahap untuk item
	 * tersebut, sehingga penguncian di sini membuat wizard/JSP bertentangan dengan
	 * perilaku layar utama ("saya cek boleh diangsur, tapi di sini tidak boleh").
	 * Keputusan boleh/tidaknya dicicil kini murni dari flag ItemBiaya.
	 *
	 * @param adminMode true bila yang membayar adalah admin/kasir (bukan akun mahasiswa)
	 */
	public static boolean bolehDiangsur(DetailBiaya detailBiaya, boolean adminMode) {
		if (detailBiaya == null) {
			return false;
		}
		try {
			ItemBiaya itemBiaya = detailBiaya.getItemBiaya();
			if (itemBiaya == null) {
				return false;
			}
			return adminMode ? Boolean.TRUE.equals(itemBiaya.getAdminBolehMencicilkan())
					: Boolean.TRUE.equals(itemBiaya.getMahasiswaBolehMencicilkan());
		} catch (Exception e) {
			return false;
		}
	}

	@SuppressWarnings({ "rawtypes" })
	public Collection getDetailBiayaMahasiswa(Mahasiswa mahasiswa, Integer semester, JenisKegiatan jenisKegiatan,
			boolean reload) {
		return getDetailBiayaMahasiswa(mahasiswa, semester, jenisKegiatan, null, reload);
	}

	@SuppressWarnings({ "rawtypes" })
	public Collection getDetailBiayaMahasiswa(Mahasiswa mahasiswa, Integer semester, JenisKegiatan jenisKegiatan,
			String bulan, boolean reload) {
		return getDetailBiayaMahasiswa(mahasiswa, semester, jenisKegiatan, bulan, false, reload);
	}

	@SuppressWarnings({ "rawtypes" })
	public Collection getDetailBiayaMahasiswa(Mahasiswa mahasiswa, Integer semester, JenisKegiatan jenisKegiatan,
			String bulan, Boolean untukBulananTampilkanMeskipunSudahDibayar, boolean reload) {
		Collection d = getDetailBiayaMahasiswadariDatabase(mahasiswa, semester, jenisKegiatan, bulan,
				untukBulananTampilkanMeskipunSudahDibayar, reload);
		return d;
	}

	/**
	 * Menghitung rincian tagihan mahasiswa untuk jalur layanan pembayaran dan host-to-host.
	 * Hasil method ini harus konsisten dengan layar Pembayaran Mahasiswa karena inquiry bank,
	 * pembuatan virtual account, dan validasi nominal memakai sumber data yang sama.
	 *
	 * <p><b>Urutan penting:</b> status akademik asli terlebih dahulu diubah menjadi status
	 * pembayaran efektif melalui
	 * {@link PembayaranUtilHelper#statusMahasiswaPembayaranEfektif}. Mesin kemudian memilih daftar
	 * item dan satu {@code SettingBiaya} berdasarkan profil mahasiswa. Sesudah root criteria
	 * {@code DetailBiaya} terbentuk, method wajib memanggil
	 * {@link PembayaranUtilHelper#batasiPembacaanDetailBiayaKeSettingTerpilih}. Helper tersebut
	 * menerima tiga bentuk relasi modern serta baris Pengaturan Tagihan legacy yang seluruh relasi
	 * setting-nya kosong. Filter item, semester, periode, angkatan, jenjang, prodi, program, status,
	 * status awal, semester mulai, kewarganegaraan, kelas, tempat tinggal, dan parameter tambahan
	 * tetap diterapkan sesudahnya.</p>
	 *
	 * <p>Jangan membuat implementasi pembatas setting tersendiri di kelas H2H. Perbedaan sekecil
	 * apa pun dapat membuat nominal pada layar admin benar tetapi inquiry bank bernilai nol. Jangan
	 * pula memakai status pembayaran efektif sebagai nilai display atau menyimpannya ke riwayat
	 * akademik; nilai tersebut hanya untuk pencocokan billing. Parameter {@code reload} mengatur
	 * cache tagihan, bukan mengubah aturan pemilihan sumber.</p>
	 *
	 * @param mahasiswa mahasiswa yang tagihannya dibaca
	 * @param semester semester pembayaran yang sedang diinquiry
	 * @param jenisKegiatan jenis pembayaran/tagihan
	 * @param bulan bulan khusus jalur tagihan bulanan; kosong untuk tagihan reguler
	 * @param untukBulananTampilkanMeskipunSudahDibayar apakah bulan lunas tetap ikut dibaca
	 * @param reload apakah cache dilewati dan data dibaca ulang
	 * @return detail biaya yang cocok dan aman dipakai menghitung nominal layanan pembayaran
	 * @see PembayaranUtilHelper#batasiPembacaanDetailBiayaKeSettingTerpilih
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Collection getDetailBiayaMahasiswadariDatabase(Mahasiswa mahasiswa, Integer semester,
			JenisKegiatan jenisKegiatan, String bulan, boolean untukBulananTampilkanMeskipunSudahDibayar,
			boolean reload) {

		if (mahasiswa.getPindahKeKampusIniMasukSemester() != null && mahasiswa.getPindahKeKampusIniMasukSemester() > 0
				&& semester != null && mahasiswa.getPindahKeKampusIniMasukSemester() > semester) {
			return new TreeSet();
		}

		if (bulan == null) {
			// Aturan "hanya berupa angsuran" berlaku PER-JENJANG dan PER-SEMESTER
			// (jenjangAngsuranJson + peta "Berlaku di smt"). CATATAN (revert 07-17):
			// status "Tagihan Default" di SettingBiaya TIDAK lagi ikut menentukan
			// pengosongan varian default — keputusan murni dari aturan jenjang/semester
			// dan keberadaan baris bulanan nyata di billing.
			Boolean modeAngsuranJenjang = null;
			Jenjang jenjangMhsUntukAngsuran = null;
			if (jenisKegiatan != null && jenisKegiatan.getHanyaBerupaAngsuran()) {
				jenjangMhsUntukAngsuran = mahasiswa.getJurusan() != null
						? mahasiswa.getJurusan().getJenjang() : mahasiswa.getJenjang();
				modeAngsuranJenjang = jenisKegiatan.modeAngsuranUntukJenjang(jenjangMhsUntukAngsuran, semester,
						mahasiswa.getTahunangkatan());
			}
			if (Boolean.TRUE.equals(modeAngsuranJenjang)) {
				// PER-SEMESTER: kosongkan varian default HANYA bila semester ini memang
				// punya baris bulanan yang akan melayaninya. Bila tidak ada sama sekali,
				// pengosongan justru membuat tagihan lenyap dari layar. 0 = bukan bulanan
				// → billing reguler tetap diproses; >0 atau -1 (cek gagal) = varian
				// default kosong (tagihan dilayani via angsuran, perilaku lama).
				int barisBulanan = hitungBarisBulananSemester(null, jenisKegiatan,
						jenjangMhsUntukAngsuran, semester, mahasiswa.getTahunangkatan(), null);
				if (barisBulanan != 0) {
					return new TreeSet();
				}
			}
		}

		if (semester != null && mahasiswa.getStatusKeluar() != null
				&& ((mahasiswa.getSemesterLulus() != null && mahasiswa.getSemesterLulus() < semester))) {

			if (!jenisKegiatan.getTagihanJugaUntukAlumni()) {
				return new TreeSet();
			}
		}

		String key = "tagihan_mhs_" + mahasiswa.getId() + "_" + jenisKegiatan.getId() + "_" + semester
				+ ((bulan == null || bulan.trim().isEmpty()) ? "" : "_" + bulan);

		if (!reload) {

			try {
				String s = mahasiswa.retreive(key);
				JSONObject data = s == null || s.trim().isEmpty() ? null : new JSONObject(s);
				if (data != null) {
					boolean smtSalah = false;
					List d = new ArrayList();
					Iterator<String> iter = data.keys();
					while (iter.hasNext()) {
						try {
							String keyIter = iter.next();
							String value = data.getString(keyIter);
							if (value.equalsIgnoreCase("1")) {
								DetailBiaya detailBiaya1 = (DetailBiaya) GeneralValueObject.ambilData(DetailBiaya.class,
										keyIter, true);
								detailBiaya1.updateKeterangan(mahasiswa, semester);

								d.add(detailBiaya1);

								if (!detailBiaya1.getSemester().equals(semester)) {
									smtSalah = true;
									break;
								}
							} else if (value.equalsIgnoreCase("2")) {
								PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) GeneralValueObject
										.ambilData(PengaturanPembayaranBulanan.class, keyIter, true);

								d.add(pengaturanPembayaranBulanan);
								if (!pengaturanPembayaranBulanan.getDetailBiaya().getSemester().equals(semester)) {
									smtSalah = true;
									break;
								}
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranUtil.java:1162");
//							e.printStackTrace();
						}
					}

					try {
						Collections.sort(d);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranUtil.java:1169");
//						e.printStackTrace();
					}

					if (!smtSalah) {
						return d;
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranUtil.java:1177");
//				e.printStackTrace();
			}

		}

		Jurusan jurusan = mahasiswa.getJurusan();
		Jenjang jenjang = jurusan != null ? jurusan.getJenjang() : mahasiswa.getJenjang();

//		String program = mahasiswa.getProgram();
		Integer angkatan = mahasiswa.getTahunangkatan();
		String warganegara = mahasiswa.getWarganegara();

		Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
		Integer semesterMulai = mahasiswa.getPindahKeKampusIniMasukSemester();
		Integer tahunAkademikMulai = Common.getTahunAkademik(semester, tahunAngkatanMhs, semesterMulai,
				mahasiswa.getSemesterMulai());

		String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
		Integer tahap = PengaturanPembayaranBulanan.hitungTahap(mahasiswa, semester,
				Common.BULAN[ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH)]);
		String mulaiBelajarDiSemester = mahasiswa.getSemesterMulai();
		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahap,
				jenisKegiatan != null && jenisKegiatan.getUntukBayarSP() ? Perkuliahan.SEMESTER_PENDEK : null, reload);

		StatusMahasiswa statusMahasiswa = Common.currentStatus(krsMahasiswa).getStatusMahasiswa();

		HistoryStatusMahasiswa tempHistoryStatusMahasiswa = Common.getHistoryStatusMahasiswa(krsMahasiswa, reload);
		String program = tempHistoryStatusMahasiswa.getProgram();
		String kelamin = mahasiswa.getKelamin();
		StatusAwalMahasiswa statusAwalMahasiswa = tempHistoryStatusMahasiswa.getStatusAwalMahasiswa();

		System.out.println("mahasiswa warganegara = " + warganegara);
		System.out.println("mahasiswa semester = " + semester);
		System.out.println("mahasiswa jenjang = " + jenjang);
		System.out.println("mahasiswa jurusan = " + jurusan);
		System.out.println("mahasiswa program = " + program);
		System.out.println("mahasiswa angkatan = " + angkatan);
		System.out.println("mahasiswa kelamin = " + kelamin);
		System.out.println("jenis Kegiatan = " + jenisKegiatan);
		System.out.println("mahasiswa mulai Belajar Di Semester = " + mulaiBelajarDiSemester);

		System.out.println("Pembayaran di bulan = " + bulan);

		// System.out.println"Pembayaran di tahap = " + tahap);

		System.out.println("mahasiswa status awal mahasiswa = "
				+ (statusAwalMahasiswa == null ? null : statusAwalMahasiswa.getNama()));

		if (((statusMahasiswa != null && ConstantValues.LULUS != null
				&& ConstantValues.LULUS.getId().equals(statusMahasiswa.getId())) || mahasiswa.getStatusKeluar() != null)
				&& mahasiswa.getSemesterLulus() != null && mahasiswa.getSemesterLulus().equals(semester)) {
			statusMahasiswa = ConstantValues.AKTIF;
		}

		PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = mahasiswa.ambilCuti(semester, tahap, false);
		if (pendaftaranCutiMahasiswa != null && pendaftaranCutiMahasiswa.getPersetujuan()) {
			statusMahasiswa = ConstantValues.CUTI;
		}

		statusMahasiswa = PembayaranUtilHelper.statusMahasiswaPembayaranEfektif(statusMahasiswa);

		System.out.println("statusMahasiswa = " + statusMahasiswa);
//		System.out
//				.println("mahasiswa statusMahasiswa = " + (statusMahasiswa == null ? null : statusMahasiswa.getNama()));

		String filterKelas = Common
				.getKonfigurasi("tampilkan_filter_kelas_pada_billing_pembayaran", Konfigurasi.TIDAK_AKTIF).getNilai();
		String filterJenisTempatTinggalMahasiswa = Common
				.getKonfigurasi("tampilkan_filter_jenis_tempat_tinggal_mahasiswa_pada_billing_pembayaran",
						Konfigurasi.TIDAK_AKTIF)
				.getNilai();

		String id_smt = (tahunAkademik == null || tahunAkademik.trim().isEmpty() ? "0" : tahunAkademik.split("/")[0])
				+ (semester == null ? "0" : (semester % 2 == 0) ? "2" : "1");
		Integer ta = 0;
		try {
			ta = Integer.parseInt(id_smt.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranUtil.java:1284");

		}

		System.out.println("Pembayaran di ta = " + ta);

		Session session = HibernateUtil.currentNativeSession();

		List<DetailBiaya> biayaDefault = SetingBiayaAction.getDetailBiayaDefault(session, mahasiswa, jenisKegiatan,
				semester, ta);
		if (PengecualianTagihanList.adalah(biayaDefault)) {
			tutupSessionSetelahPengecualian(session);
			return PengecualianTagihanList.kosong();
		}
		AfiliasiCalonMahasiswa afiliasiCalonMahasiswa = null;
		if (biayaDefault == null || biayaDefault.isEmpty()) {
			Paket paket = null;
			try {
				BiodataCalonMahasiswa biodataCalonMahasiswa = mahasiswa.getBiodataCalonMahasiswaData();
				paket = biodataCalonMahasiswa == null ? null : biodataCalonMahasiswa.getPaket();
				afiliasiCalonMahasiswa = biodataCalonMahasiswa == null ? null
						: biodataCalonMahasiswa.getAfiliasiCalonMahasiswa();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranUtil.java:1302");
				// TODO: handle exception
			}

			biayaDefault = SetingBiayaAction.getDetailBiayaDefault(session, angkatan, jenjang, semester, jenisKegiatan,
					statusAwalMahasiswa, statusMahasiswa, mahasiswa.getJenisSeleksi(),
					mahasiswa.getGelombangPendaftaran(), paket, jurusan, program, kelamin, afiliasiCalonMahasiswa, ta,
					mahasiswa.getNim());
			if (PengecualianTagihanList.adalah(biayaDefault)) {
				tutupSessionSetelahPengecualian(session);
				return PengecualianTagihanList.kosong();
			}
		}

		if (biayaDefault != null && !biayaDefault.isEmpty()) {
			if (mahasiswa != null) {
				for (DetailBiaya detailBiaya : biayaDefault) {
					detailBiaya.updateKeterangan(mahasiswa, semester);
				}
			}

			JSONObject data = new JSONObject();
			for (DetailBiaya detailBiaya : biayaDefault) {
				try {
					data.put(detailBiaya.getId().toString(), "1");
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/util/PembayaranUtil.java:1323");
				}
				GeneralValueObject.masukkanData(DetailBiaya.class, detailBiaya);
			}
			mahasiswa.put(data.toString(), key);
			session.disconnect();
			session.close();
			HibernateUtil.closeSession();
			return biayaDefault;
		}

		String kelasStr = null;
		if (filterKelas.equals(Konfigurasi.AKTIF)) {
			kelasStr = mahasiswa.getKelas();
		}
		// System.out.println"kelas = " + kelasStr);

		JenisTinggalMahasiswa jenisTinggalMahasiswa = null;
		if (filterJenisTempatTinggalMahasiswa.equals(Konfigurasi.AKTIF)) {
			jenisTinggalMahasiswa = (JenisTinggalMahasiswa) session.createCriteria(BiodataMahasiswa.class)
					.setProjection(Projections.property("jenisTinggalMahasiswa"))
					.add(Restrictions.eq("mahasiswa", mahasiswa)).addOrder(Order.desc("id")).setMaxResults(1)
					.uniqueResult();
		}

		Paket paket = null;

		try {
			BiodataCalonMahasiswa biodataCalonMahasiswa = mahasiswa.getBiodataCalonMahasiswaData();
			paket = biodataCalonMahasiswa == null ? null : biodataCalonMahasiswa.getPaket();
			afiliasiCalonMahasiswa = biodataCalonMahasiswa == null ? null
					: biodataCalonMahasiswa.getAfiliasiCalonMahasiswa();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranUtil.java:1368");
			// TODO: handle exception
		}

		List<ItemBiaya> detailSettingBiayas = SetingBiayaAction.getItemBiaya(session, angkatan, jenjang, semester,
				jenisKegiatan, statusAwalMahasiswa, statusMahasiswa, mahasiswa.getJenisSeleksi(),
				mahasiswa.getGelombangPendaftaran(), paket, jurusan, program, kelamin, afiliasiCalonMahasiswa, ta,
				mahasiswa.getNim());
		if (detailSettingBiayas == null) {
			tutupSessionSetelahPengecualian(session);
			return PengecualianTagihanList.kosong();
		}
		SettingBiaya settingBiayaTerpilih = SetingBiayaAction.getSettingBiayaTerpilih(session, angkatan, jenjang,
				semester, jenisKegiatan, statusAwalMahasiswa, statusMahasiswa, mahasiswa.getJenisSeleksi(),
				mahasiswa.getGelombangPendaftaran(), paket, jurusan, program, kelamin, afiliasiCalonMahasiswa, ta,
				mahasiswa.getNim(), false);

		Criteria criteria = session.createCriteria(DetailBiaya.class);

		if (bulan != null && !bulan.trim().isEmpty() && Common.isNumber(bulan)) {

			List<PengaturanPembayaranBulanan> yangSudahDibayarBulanans = untukBulananTampilkanMeskipunSudahDibayar
					? null
					: session.createCriteria(CicilanPembayaran.class).createAlias("kegiatan", "kegiatan")
							.add(Restrictions.eq("kegiatan.mahasiswa", mahasiswa))
							.add(Restrictions.eq("kegiatan.jenisKegiatan", jenisKegiatan))
							.add(Restrictions.eq("kegiatan.semster", semester))
							.setProjection(Projections.groupProperty("pengaturanPembayaranBulanan"))
							.add(Restrictions.isNotNull("pengaturanPembayaranBulanan")).list();

			String sqlQuery = "(realbulan,item_biaya) not in (";
			String sql = "";
			if (yangSudahDibayarBulanans != null) {
				for (PengaturanPembayaranBulanan p : yangSudahDibayarBulanans) {
					sql = sql.trim().isEmpty()
							? "(" + p.getRealBulan() + "," + p.getDetailBiaya().getItemBiaya().getId() + ")"
							: ",(" + p.getRealBulan() + "," + p.getDetailBiaya().getItemBiaya().getId() + ")";
					sqlQuery += sql;
				}
			}
			sqlQuery += ")";

			if (Common.bolehKonfigurasi("tagihan_pembayaran_host_to_host_per_bulan_dihitung_berdasarkan_akumulasi_bulanan_yg_belum_dibayar", Konfigurasi.TIDAK_AKTIF)) {

				Integer bln = bulan.trim().equals("-1") ? null
						: (Integer) session.createCriteria(PengaturanPembayaranBulanan.class)
								.add(Restrictions.eq("realBulan", Integer.parseInt(bulan.trim())))
								.setProjection(Projections.property("bulan")).setMaxResults(1)
								.addOrder(Order.desc("id")).uniqueResult();

				criteria = session.createCriteria(PengaturanPembayaranBulanan.class)
						.add(sql.trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.sqlRestriction(sqlQuery))
						.add(bulan.trim().equals("-1") ? Restrictions.sqlRestriction("true")
								: bln != null ? Restrictions.le("bulan", bln)
										: Restrictions.eq("realBulan", Integer.parseInt(bulan.trim())))
						.createCriteria("detailBiaya");

			} else {

				criteria = session.createCriteria(PengaturanPembayaranBulanan.class)
						.add(sql.trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.sqlRestriction(sqlQuery))
						.add(bulan.trim().equals("-1") ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("realBulan", Integer.parseInt(bulan.trim())))
						.createCriteria("detailBiaya");
			}

		}

		criteria = PembayaranUtilHelper.batasiPembacaanDetailBiayaKeSettingTerpilih(criteria,
				settingBiayaTerpilih);
		filterCriteriaDenganNilaiTambahan(criteria, session, mahasiswa, null);

		System.out.println("detailSettingBiayas > " + detailSettingBiayas);

		if (kelasStr != null) {
			criteria.createAlias("kelas", "kelas").add(Restrictions.eq("kelas.nama", kelasStr));
		} else {
			criteria.add(Restrictions.isNull("kelas"));
		}

		java.util.Collection detailBiaya = criteria

				.add(detailSettingBiayas.isEmpty() ? Restrictions.sqlRestriction("false")
						: Restrictions.in("itemBiaya", detailSettingBiayas))

				.add(Restrictions.or(Restrictions.eq("merupakanPembayaran", false),
						Restrictions.isNull("merupakanPembayaran")))

				.addOrder(Order.desc("id"))

				.add(jenisTinggalMahasiswa == null ? Restrictions.isNull("jenisTinggalMahasiswa")
						: Restrictions.eq("jenisTinggalMahasiswa", jenisTinggalMahasiswa))

				.add(Restrictions.eq("tahunAkademik", tahunAkademik))
				.add(Restrictions.eq("statusMahasiswa", statusMahasiswa))
				.add(Restrictions.eq("statusAwalMahasiswa", statusAwalMahasiswa))
				.add(Restrictions.eq("mulaiBelajarDiSemester", mulaiBelajarDiSemester))
				.add(Restrictions.eq("jenisKegiatan", jenisKegiatan))
				.add(Restrictions.ilike("wnaAtauWni", warganegara, MatchMode.EXACT))
				.add(Restrictions.eq("jenjang", jenjang)).add(Restrictions.eq("jurusan", jurusan))
				.add(Restrictions.ilike("program", program, MatchMode.EXACT)).add(Restrictions.eq("semester", semester))
				.add(Restrictions.between("semester", jenisKegiatan.getMinSmt(), jenisKegiatan.getMaxSmt()))
				.add(Restrictions.eq("angkatan", angkatan)).list();

		List<DetailBiaya> biayaDefaultBiaya = SetingBiayaAction.getDetailBiayaBukanDefaultBiaya(session, angkatan,
				jenjang, semester, jenisKegiatan, statusAwalMahasiswa, statusMahasiswa, mahasiswa.getJenisSeleksi(),
				mahasiswa.getGelombangPendaftaran(), paket, jurusan, program, kelamin, afiliasiCalonMahasiswa, ta,
				mahasiswa.getNim());
		if (!biayaDefaultBiaya.isEmpty()) {
			if (mahasiswa != null) {
				for (DetailBiaya detailBiayaDefault : biayaDefaultBiaya) {
					detailBiayaDefault.updateKeterangan(mahasiswa, semester);
					detailBiaya.add(detailBiayaDefault);
				}
			}
		}

		System.out.println("sebelum di filter detailBiaya > " + detailBiaya + " bulan " + bulan);

		if (bulan != null && !bulan.trim().isEmpty() && Common.isNumber(bulan)) {

			List<PengaturanPembayaranBulanan> d = saringPengaturanPembayaranBulanan(
					(List<GeneralValueObject>) detailBiaya);

			JSONObject data = new JSONObject();
			try {
				for (PengaturanPembayaranBulanan p : d) {
					GeneralValueObject.masukkanData(PengaturanPembayaranBulanan.class, p);
					data.put(p.getId().toString(), "2");
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/util/PembayaranUtil.java:1507");
			}
			mahasiswa.put(data.toString(), key);

			System.out.println("setelah di filter detailBiaya > " + d);

			session.disconnect();
			session.close();
			HibernateUtil.closeSession();
			return d;
		} else {

			Map<Long, Long> ids = new HashMap<Long, Long>();

			Map<Long, Object> maps = new HashMap<Long, Object>();
			for (Object o : detailBiaya) {
				try {
					if (o instanceof DetailBiaya) {
						DetailBiaya biaya = (DetailBiaya) o;
						Long value = ids.get(biaya.getItemBiaya().getId());
						if (value == null || value < biaya.getId()) {
							ids.put(biaya.getItemBiaya().getId(), biaya.getId());
							maps.put(biaya.getItemBiaya().getId(), biaya);
						}
					} else if (o instanceof PengaturanPembayaranBulanan) {
						PengaturanPembayaranBulanan biaya = (PengaturanPembayaranBulanan) o;
						if (biaya.getNominal().intValue() != 0 && !maps.containsKey(biaya.getId())) {
							maps.put(biaya.getId(), biaya);
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/util/PembayaranUtil.java:1538");
				}
			}

			for (Object o : detailBiaya) {
				try {
					if (o instanceof DetailBiaya) {
						DetailBiaya biaya = (DetailBiaya) o;
						if (!maps.containsKey(biaya.getItemBiaya().getId())) {
							maps.put(biaya.getItemBiaya().getId(), biaya);
						}
					} else if (o instanceof PengaturanPembayaranBulanan) {
						PengaturanPembayaranBulanan biaya = (PengaturanPembayaranBulanan) o;
						if (!maps.containsKey(biaya.getId())) {
							maps.put(biaya.getId(), biaya);
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/util/PembayaranUtil.java:1556");
				}
			}

			TreeSet treeSet = new TreeSet(maps.values());

			JSONObject data = new JSONObject();
			try {
				for (Object o : treeSet) {
					try {
						if (o instanceof DetailBiaya) {
							DetailBiaya detailBiaya1 = (DetailBiaya) o;
							detailBiaya1.updateKeterangan(mahasiswa, semester);
							data.put(detailBiaya1.getId().toString(), "1");
							GeneralValueObject.masukkanData(DetailBiaya.class, detailBiaya1);
						} else if (o instanceof PengaturanPembayaranBulanan) {
							PengaturanPembayaranBulanan biaya = (PengaturanPembayaranBulanan) o;
							data.put(biaya.getId().toString(), "2");
							GeneralValueObject.masukkanData(PengaturanPembayaranBulanan.class, biaya);
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/util/PembayaranUtil.java:1577");
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/util/PembayaranUtil.java:1581");
			}

			mahasiswa.put(data.toString(), key);

			System.out.println("setelah di filter detailBiaya > " + treeSet);

			session.disconnect();
			session.close();
			HibernateUtil.closeSession();

			return treeSet;
		}
	}

	public void filterCriteriaDenganNilaiTambahan(Criteria criteria, Session session, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa) {
		Konfigurasi konfigurasiTambahan1 = Common.getKonfigurasi("tambah_dan_aktifkan_filter_ke_1_paramater_tambahan",
				Konfigurasi.TIDAK_AKTIF, "-1", "", "");
		Konfigurasi konfigurasiTambahan2 = Common.getKonfigurasi("tambah_dan_aktifkan_filter_ke_2_paramater_tambahan",
				Konfigurasi.TIDAK_AKTIF, "-1", "", "");
		Konfigurasi konfigurasiTambahan3 = Common.getKonfigurasi("tambah_dan_aktifkan_filter_ke_3_paramater_tambahan",
				Konfigurasi.TIDAK_AKTIF, "-1", "", "");

		List<String> nilaiTambahan = null;
		if (konfigurasiTambahan1.getNilai().equals(Konfigurasi.AKTIF)
				|| konfigurasiTambahan2.getNilai().equals(Konfigurasi.AKTIF)
				|| konfigurasiTambahan3.getNilai().equals(Konfigurasi.AKTIF)) {

			String parameterTambahanInds = null;

			if (mahasiswa != null && mahasiswa.getId() != null) {
				parameterTambahanInds = (String) session.createCriteria(BiodataMahasiswa.class)
						.add(Restrictions.eq("mahasiswa", mahasiswa)).addOrder(Order.desc("id")).setMaxResults(1)
						.setProjection(Projections.property("parameterTambahanInds")).uniqueResult();
			} else if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getParameterTambahanInds() != null) {
				parameterTambahanInds = biodataCalonMahasiswa.getParameterTambahanInds();
			}

			if (parameterTambahanInds != null && !parameterTambahanInds.trim().isEmpty()) {
				nilaiTambahan = new ArrayList<String>();
				String[] spl = parameterTambahanInds.split("\n");
				for (String d : spl) {
					String[] value = d.split("<=>");
					String lbl = value.length > 0 ? value[0].trim() : "";
					String val = value.length > 1 ? value[1].trim() : "";
					if (!val.isEmpty()) {
						try {
							nilaiTambahan.add(lbl.split("->")[1].trim() + "<=>" + val);
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
				}

			}

		}

//		System.out.println("nilaiTambahan => " + nilaiTambahan);

		if (nilaiTambahan != null && !nilaiTambahan.isEmpty()) {
			if (konfigurasiTambahan1.getNilai().equals(Konfigurasi.AKTIF)) {
				Criterion criterion = Restrictions.eq("nilaiTambahan1", nilaiTambahan.get(0));
				for (int i = 1; i < nilaiTambahan.size(); i++) {
					criterion = Restrictions.or(criterion, Restrictions.eq("nilaiTambahan1", nilaiTambahan.get(i)));
				}
				criteria.add(criterion);
			}
			if (konfigurasiTambahan2.getNilai().equals(Konfigurasi.AKTIF)) {
				Criterion criterion = Restrictions.eq("nilaiTambahan2", nilaiTambahan.get(0));
				for (int i = 1; i < nilaiTambahan.size(); i++) {
					criterion = Restrictions.or(criterion, Restrictions.eq("nilaiTambahan2", nilaiTambahan.get(i)));
				}
				criteria.add(criterion);
			}
			if (konfigurasiTambahan3.getNilai().equals(Konfigurasi.AKTIF)) {
				Criterion criterion = Restrictions.eq("nilaiTambahan3", nilaiTambahan.get(0));
				for (int i = 1; i < nilaiTambahan.size(); i++) {
					criterion = Restrictions.or(criterion, Restrictions.eq("nilaiTambahan3", nilaiTambahan.get(i)));
				}
				criteria.add(criterion);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public Collection getDetailBiayaMahasiswaBerdasarkanJenisKegiatan(Mahasiswa mahasiswa, JenisKegiatan jenisKegiatan,
			String bulan, boolean reload) {
		Boolean ganjil = CommonUtil.isNowSemensterGanjil();
		Integer semester = CommonUtil.getSemester(mahasiswa.getTahunangkatan(), ganjil,
				mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
		return getDetailBiayaMahasiswa(mahasiswa, semester, jenisKegiatan, bulan, reload);
	}

	@SuppressWarnings("rawtypes")
	public Collection getDetailBiayaMahasiswa(Mahasiswa mahasiswa, JadwalPembayaran jadwalPembayaran, String bulan,
			boolean reload) {
		Boolean ganjil = jadwalPembayaran.getGanjil() == null ? Common.isNowSemensterGanjil()
				: jadwalPembayaran.getGanjil();
		Integer semester = Common.getSemester(mahasiswa.getTahunangkatan(), jadwalPembayaran.getTahunAkademik(),
				ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP, mahasiswa.getPindahKeKampusIniMasukSemester(),
				mahasiswa.getSemesterMulai());
		return getDetailBiayaMahasiswa(mahasiswa, semester, jadwalPembayaran.getJenisKegiatan(), bulan, reload);
	}

	@SuppressWarnings("unchecked")
	public List<DetailKegiatan> getDetailKegiatanMahasiswa(Mahasiswa mahasiswa, BiodataCalonMahasiswa calonMahasiswa,
			JenisKegiatan jenisKegiatan) {
		Session session = HibernateUtil.currentNativeSession();
		// System.out.println"jenis Kegiatan = " + jenisKegiatan);
		List<DetailKegiatan> detailBiaya = session.createCriteria(DetailKegiatan.class).createCriteria("kegiatan")
				.add(Restrictions.eq("jenisKegiatan", jenisKegiatan))
				.add(Restrictions.or(
						mahasiswa == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("mahasiswa", mahasiswa),
						calonMahasiswa == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("calonMahasiswa", calonMahasiswa)))
				.list();
		// System.out.println"jumlah detailBiaya = " + detailBiaya.size());

		HibernateUtil.closeSession();
		return detailBiaya;
	}

	public Collection<DetailBiaya> getDetailBiayaCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa,
			JenisKegiatan jenisKegiatan, Jurusan jurusan, boolean reload) {
		return getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, jurusan, null, reload);
	}

	/**
	 * Mengambil sumber DetailBiaya dari rincian tagihan yang sudah benar-benar
	 * terbentuk pada Kegiatan. Jalur ini dipakai H2H sebagai pemulihan apabila
	 * pencarian ulang template biaya kosong (misalnya semester masuk mahasiswa
	 * RPL tidak sama dengan semester default template), sementara layar billing
	 * sudah memiliki tagihan aktual.
	 */
	public Collection<DetailBiaya> getDetailBiayaDariKegiatan(Kegiatan kegiatan) {
		List<DetailBiaya> hasil = new ArrayList<DetailBiaya>();
		if (kegiatan == null || kegiatan.getId() == null) {
			return hasil;
		}

		Map<Long, DetailBiaya> unik = new HashMap<Long, DetailBiaya>();
		try {
			Collection<DetailKegiatan> rincian = kegiatan.ambilDetailKegiatan(true);
			if (rincian != null) {
				for (DetailKegiatan detailKegiatan : rincian) {
					DetailBiaya detailBiaya = detailKegiatan == null ? null : detailKegiatan.getDetailBiaya();
					if (detailBiaya != null && detailBiaya.getId() != null
							&& detailBiaya.getItemBiaya() != null) {
						unik.put(detailBiaya.getId(), detailBiaya);
					}
				}
			}
			hasil.addAll(unik.values());
			try {
				Collections.sort(hasil);
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit(sort) PembayaranUtil.getDetailBiayaDariKegiatan");
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit PembayaranUtil.getDetailBiayaDariKegiatan");
		}
		return hasil;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Collection<DetailBiaya> getDetailBiayaCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa,
			JenisKegiatan jenisKegiatan, Jurusan jurusan, Integer semester, boolean reload) {

		// Satu sumber kebenaran dengan layar Pembayaran Daftar Ulang. Implementasi
		// lama di kelas WS memiliki filter/cache yang berbeda sehingga UI dapat
		// menampilkan tagihan sementara inquiry H2H menghasilkan total 0.
		if (biodataCalonMahasiswa != null && jenisKegiatan != null) {
			return PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa,
					jenisKegiatan, jurusan, semester, reload);
		}

		String key = "tagihan_cal_mhs_" + biodataCalonMahasiswa.getId() + "_" + jenisKegiatan.getId() + "_" + semester;

		if (!reload) {

			try {
				String s = biodataCalonMahasiswa.retreive(key);
				JSONObject data = s == null || s.trim().isEmpty() ? null : new JSONObject(s);
				if (data != null) {
					List d = new ArrayList();
					Iterator<String> iter = data.keys();
					while (iter.hasNext()) {
						String keyIter = iter.next();
						String value = data.getString(keyIter);

//						System.out.println("keyIter -> " + keyIter + ", value -> " + value);
						if (value.equalsIgnoreCase("1")) {
							DetailBiaya detailBiaya1 = (DetailBiaya) GeneralValueObject.ambilData(DetailBiaya.class,
									keyIter, true);

							if (biodataCalonMahasiswa.getMahasiswa() != null) {
								detailBiaya1.updateKeterangan(biodataCalonMahasiswa.getMahasiswa(), semester);
							}

							d.add(detailBiaya1);
						} else if (value.equalsIgnoreCase("2")) {
							PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) GeneralValueObject
									.ambilData(PengaturanPembayaranBulanan.class, keyIter, true);
							d.add(pengaturanPembayaranBulanan);
						}
					}

					try {
						Collections.sort(d);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/util/PembayaranUtil.java:1749");
					}

					return d;
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/util/PembayaranUtil.java:1755");
			}

		}

		Jenjang jenjang = jurusan != null ? jurusan.getJenjang() : biodataCalonMahasiswa.getJenjang();
		JenisSeleksi jenisSeleksi = biodataCalonMahasiswa.getJenisSeleksi();
		String program = biodataCalonMahasiswa.getProgram();
		Integer angkatan = biodataCalonMahasiswa.getTahun();
		Paket paket = biodataCalonMahasiswa.getPaket();
		GelombangPendaftaran gelombangPendaftaran = biodataCalonMahasiswa.getGelombangPendaftaran();
		System.out.println("jenis Seleksi = "
				+ (jenisSeleksi == null ? "" : jenisSeleksi.getNama() + ", jenisSeleksi = " + jenisSeleksi.getId()));
		String warganegara = biodataCalonMahasiswa.getKewarganegaraan();
		String kelamin = biodataCalonMahasiswa.getJenisKelamin();
		AfiliasiCalonMahasiswa afiliasiCalonMahasiswa = biodataCalonMahasiswa.getAfiliasiCalonMahasiswa();

		System.out.println("calon mahasiswa afiliasiCalonMahasiswa = " + afiliasiCalonMahasiswa);
		System.out.println("calon mahasiswa semester = " + semester);
		System.out.println("calon mahasiswa jenis = " + jenisKegiatan);
		System.out.println("calon mahasiswa warganegara = " + warganegara);
		System.out.println("calon mahasiswa jenjang = " + jenjang);
		System.out.println("calon mahasiswa jurusan = " + jurusan);
		System.out.println("calon mahasiswa program = " + program);
		System.out.println("calon mahasiswa angkatan = " + angkatan);
		System.out.println("calon mahasiswa kelamin = " + kelamin);
		System.out.println("calon mahasiswa paket = " + paket);
		System.out.println("calon mahasiswa gelombangPendaftaran = " + gelombangPendaftaran);
		System.out.println("calon mahasiswa status awal = " + biodataCalonMahasiswa.getStatusAwalMahasiswa());
		System.out.println("calon mahasiswa mulai semester = " + biodataCalonMahasiswa.getSemesterMulai());

		String tahunAkademik = biodataCalonMahasiswa.getTahunAkademik();

		String id_smt = (tahunAkademik == null || tahunAkademik.trim().isEmpty() ? "0" : tahunAkademik.split("/")[0])
				+ (semester == null ? "0" : (semester % 2 == 0) ? "2" : "1");
		Integer ta = 0;
		try {
			ta = Integer.parseInt(id_smt.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranUtil.java:1793");

		}

		System.out.println("Pembayaran di ta = " + ta);

		Session session = HibernateUtil.currentNativeSession();
		List<DetailBiaya> biayaDefault = SetingBiayaAction.getDetailBiayaDefault(session, biodataCalonMahasiswa,
				jenisKegiatan, semester, ta);
		if (PengecualianTagihanList.adalah(biayaDefault)) {
			tutupSessionSetelahPengecualian(session);
			return PengecualianTagihanList.kosong();
		}

		if (biayaDefault == null || biayaDefault.isEmpty()) {
			biayaDefault = SetingBiayaAction.getDetailBiayaDefault(session, angkatan, jenjang, semester, jenisKegiatan,
					biodataCalonMahasiswa.getStatusAwalMahasiswa(), ConstantValues.AKTIF,
					biodataCalonMahasiswa.getJenisSeleksi(), biodataCalonMahasiswa.getGelombangPendaftaran(),
					biodataCalonMahasiswa.getPaket(), jurusan, program, kelamin, afiliasiCalonMahasiswa, ta,
					biodataCalonMahasiswa.getNim());
			if (PengecualianTagihanList.adalah(biayaDefault)) {
				tutupSessionSetelahPengecualian(session);
				return PengecualianTagihanList.kosong();
			}
		}
		if (biayaDefault != null && !biayaDefault.isEmpty()) {
			if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getMahasiswa() != null) {
				for (DetailBiaya detailBiaya : biayaDefault) {
					detailBiaya.updateKeterangan(biodataCalonMahasiswa.getMahasiswa(), semester);
				}
			}

			JSONObject data = new JSONObject();
			for (DetailBiaya detailBiaya : biayaDefault) {
				try {
					data.put(detailBiaya.getId().toString(), "1");
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/util/PembayaranUtil.java:1821");
				}
				GeneralValueObject.masukkanData(DetailBiaya.class, detailBiaya);
			}
			biodataCalonMahasiswa.put(data.toString(), key);
			session.disconnect();
			session.close();
			HibernateUtil.closeSession();
			return biayaDefault;
		}

		if (jurusan == null) {
			jurusan = (Jurusan) session.createCriteria(Jurusan.class).add(Restrictions.eq("jenjang", jenjang))
					.setMaxResults(1).uniqueResult();
			// System.out.println"query lagi jurusan --> calon mahasiswa jurusan
			// = "
			// + (jurusan == null ? "" : jurusan.getId() + " - " +
			// jurusan.getNama()));
		}

		List<ItemBiaya> detailSettingBiayas = SetingBiayaAction.getItemBiaya(session, angkatan, jenjang, semester,
				jenisKegiatan, biodataCalonMahasiswa.getStatusAwalMahasiswa(), ConstantValues.AKTIF,
				biodataCalonMahasiswa.getJenisSeleksi(), biodataCalonMahasiswa.getGelombangPendaftaran(),
				biodataCalonMahasiswa.getPaket(), jurusan, program, kelamin, afiliasiCalonMahasiswa, ta,
				biodataCalonMahasiswa.getNim());
		if (detailSettingBiayas == null) {
			tutupSessionSetelahPengecualian(session);
			return PengecualianTagihanList.kosong();
		}
		SettingBiaya settingBiayaTerpilih = SetingBiayaAction.getSettingBiayaTerpilih(session, angkatan, jenjang,
				semester, jenisKegiatan, biodataCalonMahasiswa.getStatusAwalMahasiswa(), ConstantValues.AKTIF,
				biodataCalonMahasiswa.getJenisSeleksi(), biodataCalonMahasiswa.getGelombangPendaftaran(),
				biodataCalonMahasiswa.getPaket(), jurusan, program, kelamin, afiliasiCalonMahasiswa, ta,
				biodataCalonMahasiswa.getNim(), false);

		System.out.println("calon mahasiswa jenis kegiatan = " + jenisKegiatan.getNamaKegiatan() + " default "
				+ ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU + " min " + jenisKegiatan.getMinSmt() + " max "
				+ jenisKegiatan.getMaxSmt());
		System.out.println("calon mahasiswa detailSettingBiayas = " + detailSettingBiayas);

		// Mode angsuran calon: keputusan per-jenjang + per-semester + per-angkatan
		// ("Berlaku di smt", format TAHUN:SMT) — BUKAN flag global. Lalu verifikasi
		// terhadap kenyataan billing: bila tidak ada satu pun baris bulanan untuk
		// kombinasi ini, JANGAN paksa jalur angsuran — kueri via PengaturanPembayaranBulanan
		// akan kosong dan tagihan reguler yang sebenarnya ada ikut lenyap (gejala nyata:
		// inquiry bank error 07 "Jumlah pembayaran tidak sesuai" dengan total_amount=0
		// padahal tagihan daftar-ulang calon terpasang).
		boolean pakaiJalurAngsuran = Boolean.TRUE
				.equals(jenisKegiatan.modeAngsuranUntukJenjang(jenjang, semester, angkatan));
		if (pakaiJalurAngsuran) {
			int barisBulanan = hitungBarisBulananSemester(session, jenisKegiatan, jenjang, semester, angkatan, null);
			if (barisBulanan == 0) {
				pakaiJalurAngsuran = false;
			}
		}
		Criteria criteria = pakaiJalurAngsuran
				? session.createCriteria(PengaturanPembayaranBulanan.class)
						.setProjection(Projections.property("detailBiaya")).createCriteria("detailBiaya")
				: session.createCriteria(DetailBiaya.class);
		criteria = PembayaranUtilHelper.batasiPembacaanDetailBiayaKeSettingTerpilih(criteria,
				settingBiayaTerpilih);
		filterCriteriaDenganNilaiTambahan(criteria, session, null, biodataCalonMahasiswa);

		criteria = criteria

				.add(paket == null ? Restrictions.isNull("paket") : Restrictions.eq("paket", paket))

				.add(detailSettingBiayas.isEmpty() ? Restrictions.sqlRestriction("false")
						: Restrictions.in("itemBiaya", detailSettingBiayas))

				.add(Restrictions.or(Restrictions.eq("merupakanPembayaran", false),
						Restrictions.isNull("merupakanPembayaran")))

				.add(paket != null && paket.getBiayaPendaftaranSemuaGelombangSama()
						? Restrictions.isNull("gelombangPendaftaran")
						: jenisKegiatan.getNamaKegiatan()
								.equalsIgnoreCase(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU)
										? Restrictions.eq("gelombangPendaftaran", gelombangPendaftaran)
										: Restrictions.or(Restrictions.eq("gelombangPendaftaran", gelombangPendaftaran),
												Restrictions.isNull("gelombangPendaftaran")))

				.add(semester == null ? Restrictions.in("semester", new Integer[] { 0, 1 })
						: Restrictions.eq("semester", semester))

				.add(Restrictions.ge("semester", jenisKegiatan.getMinSmt()))
				.add(Restrictions.le("semester", jenisKegiatan.getMaxSmt()))

				.add(Restrictions.eq("statusAwalMahasiswa", biodataCalonMahasiswa.getStatusAwalMahasiswa()))
				.add(Restrictions.eq("statusMahasiswa", ConstantValues.AKTIF))
				.add(Restrictions.ilike("wnaAtauWni", warganegara, MatchMode.EXACT))
				.add(Restrictions.eq("jenisKegiatan", jenisKegiatan)).add(Restrictions.eq("jenisSeleksi", jenisSeleksi))
				.add(Restrictions.eq("jenjang", jenjang)).add(Restrictions.eq("jurusan", jurusan))
				.add(Restrictions.ilike("program", program, MatchMode.EXACT)).add(Restrictions.eq("angkatan", angkatan))
				.add(Restrictions.ilike("mulaiBelajarDiSemester", biodataCalonMahasiswa.getSemesterMulai(),
						MatchMode.EXACT));

		criteria.addOrder(Order.desc("id"));

		List<DetailBiaya> detailBiaya = criteria.list();

		if (pakaiJalurAngsuran) {
			List<DetailBiaya> biayaDefaultBiaya = SetingBiayaAction.getDetailBiayaBukanDefaultBiaya(session, angkatan,
					jenjang, semester, jenisKegiatan, biodataCalonMahasiswa.getStatusAwalMahasiswa(),
					ConstantValues.AKTIF, biodataCalonMahasiswa.getJenisSeleksi(),
					biodataCalonMahasiswa.getGelombangPendaftaran(), biodataCalonMahasiswa.getPaket(), jurusan, program,
					kelamin, afiliasiCalonMahasiswa, ta, biodataCalonMahasiswa.getNim());
			if (!biayaDefaultBiaya.isEmpty()) {
				for (DetailBiaya detailBiayaDefault : biayaDefaultBiaya) {
					detailBiaya.add(detailBiayaDefault);
				}
			}
		}

		System.out.println("jumlah detailBiaya = " + detailBiaya);

		Map<Long, DetailBiaya> maps = new HashMap<Long, DetailBiaya>();

		Map<Long, Long> ids = new HashMap<Long, Long>();

		for (DetailBiaya biaya : detailBiaya) {
			Long value = ids.get(biaya.getItemBiaya().getId());
			if (value == null || value < biaya.getId()) {
				ids.put(biaya.getItemBiaya().getId(), biaya.getId());
				maps.put(biaya.getItemBiaya().getId(), biaya);
			}
		}

		System.out.println("jumlah detailBiaya = " + maps);

		TreeSet d = new TreeSet(maps.values());

		JSONObject data = new JSONObject();
		try {
			for (Object o : d) {
				try {
					if (o instanceof DetailBiaya) {
						DetailBiaya detailBiaya1 = (DetailBiaya) o;
						data.put(detailBiaya1.getId().toString(), "1");

						if (biodataCalonMahasiswa.getMahasiswa() != null) {
							detailBiaya1.updateKeterangan(biodataCalonMahasiswa.getMahasiswa(), semester);
						}

						GeneralValueObject.masukkanData(DetailBiaya.class, detailBiaya1);
					} else if (o instanceof PengaturanPembayaranBulanan) {
						PengaturanPembayaranBulanan biaya = (PengaturanPembayaranBulanan) o;
						data.put(biaya.getId().toString(), "2");
						GeneralValueObject.masukkanData(PengaturanPembayaranBulanan.class, biaya);
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/util/PembayaranUtil.java:1944");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/util/PembayaranUtil.java:1948");
		}

		biodataCalonMahasiswa.put(data.toString(), key);

		HibernateUtil.closeSession();
		return d;
	}

	public Collection<DetailBiaya> getDetailBiayaMahasiswaBaru(BiodataCalonMahasiswa biodataCalonMahasiswa,
			JenisKegiatan jenisKegiatan) {
		Jurusan jurusan = biodataCalonMahasiswa.getProdiLulus();
		return getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, jurusan, false);
	}

	@SuppressWarnings("unchecked")
	public boolean dropKegiatan(Kegiatan kegiatan, String bulan, String kodeAsli) {
		if (kegiatan == null || kegiatan.getId() == null) {
			return false;
		}

		boolean hasil = true;
		try {
			Session session = HibernateUtil.currentNativeSession();

			Integer jumlahCicilan = bulan == null || bulan.trim().isEmpty() ? null
					: ((Number) session.createCriteria(CicilanPembayaran.class)
							.add(Restrictions.eq("kegiatan", kegiatan)).setProjection(Projections.rowCount())
							.uniqueResult()).intValue();

			if (jumlahCicilan != null && jumlahCicilan.intValue() > 0) {

				try {
					CommonEmail.infoBatalBayar(kegiatan, kodeAsli);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				Integer bln = Integer.parseInt(bulan.trim());
				List<CicilanPembayaran> cicilanPembayarans = session.createCriteria(CicilanPembayaran.class)
						.createAlias("pengaturanPembayaranBulanan", "pengaturanPembayaranBulanan")
						.add(Restrictions.or(Restrictions.eq("kodeRequest", kodeAsli),
								Restrictions.eq("pengaturanPembayaranBulanan.realBulan", bln)))
						.add(Restrictions.eq("kegiatan", kegiatan)).list();

				// System.out.println"==== Hapus => " + cicilanPembayarans + ",
				// kegiatan " + kegiatan);
				session.getTransaction().begin();
				for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
					Common.refreshDelete(session, cicilanPembayaran);
				}
				session.getTransaction().commit();

				Double amountTotal = 0.0;
				Number jumlahYangSudahDibayar = null;
				jumlahYangSudahDibayar = (Number) session.createCriteria(CicilanPembayaran.class)
						.add(Restrictions.eq("kegiatan", kegiatan)).setProjection(Projections.sum("nilai"))
						.uniqueResult();
				if (jumlahYangSudahDibayar != null) {
					amountTotal += jumlahYangSudahDibayar.doubleValue();
				}
				kegiatan.setAmount(amountTotal);
				kegiatan.setJumlahTelahDibayar(amountTotal);

				try {
					if (kegiatan.getMahasiswa() != null) {
						Map<Long, DetailBiaya> map = new java.util.HashMap<Long, DetailBiaya>();
						Collection<DetailBiaya> mydetailBiayas = pembayaranUtil.getDetailBiayaMahasiswa(
								kegiatan.getMahasiswa(), kegiatan.getSemster(), kegiatan.getJenisKegiatan(), false);
						for (Object o : mydetailBiayas) {
							if (o instanceof DetailBiaya) {
								DetailBiaya detailBiaya = (DetailBiaya) o;
								map.put(detailBiaya.getId(), detailBiaya);
							} else if (o instanceof PengaturanPembayaranBulanan) {
								PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
								DetailBiaya detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
								map.put(detailBiaya.getId(), detailBiaya);
							}
						}

						Double nilaiBiayaHarusDiBayars = 0.0;
						for (DetailBiaya detailBiaya : map.values()) {
							nilaiBiayaHarusDiBayars += Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);
						}

						kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars - amountTotal);
						kegiatan.setAmount(amountTotal);
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, kegiatan);
				session.getTransaction().commit();

			} else {

				if (Common.bolehKonfigurasi("bank_bisa_melakukan_reversal_data_kegiatan_pembayaran", Konfigurasi.TIDAK_AKTIF)) {

					try {
						CommonEmail.infoBatalBayar(kegiatan, kodeAsli);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}

					session.getTransaction().begin();

					String sql = "update tunggakan_mahasiswa set kegiatan = null where kegiatan = " + kegiatan.getId()
							+ ";";

					// System.out.printlnsql);
					session.createSQLQuery(sql).executeUpdate();

					PembayaranUtil.getInstance().getResetCicilanOld(session, kegiatan.getCalonMahasiswa(),
							kegiatan.getMahasiswa(), kegiatan.getSemster(), kegiatan.getJenisKegiatan(), kegiatan);

					// System.out.printlnsql);
					session.createSQLQuery(sql).executeUpdate();

					session.getTransaction().commit();

					HibernateUtil.closeSession();

					session = HibernateUtil.currentNativeSession();
					List<LogHostToHost> hostToHosts = session.createCriteria(LogHostToHost.class)
							.add(Restrictions.eq("kegiatan", kegiatan)).list();
					List<DetailKegiatan> detailKegiatans = session.createCriteria(DetailKegiatan.class)
							.add(Restrictions.eq("kegiatan", kegiatan)).list();
					session.getTransaction().begin();
					for (LogHostToHost hostToHost : hostToHosts) {
						hostToHost.setNama(
								"pembayaran telah ditarik kembali (reversal) dari request = " + hostToHost.getNama());
						hostToHost.setKegiatan(null);
						Common.refreshSaveOrUpdate(session, hostToHost);
					}
					for (DetailKegiatan detailKegiatan : detailKegiatans) {
						Common.refreshDelete(session, (detailKegiatan));
					}
					Common.refreshDelete(session, (kegiatan));
					session.getTransaction().commit();

				}
			}

			HibernateUtil.closeSession();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			hasil = false;
		}

		return hasil;
	}

	@SuppressWarnings("unchecked")
	public boolean dropKegiatanLangsung(Kegiatan kegiatan) {
		if (kegiatan == null || kegiatan.getId() == null) {
			return false;
		}

		boolean hasil = true;
		Session session = null;
		Long kegiatanId = kegiatan.getId();
		try {

			if (kegiatan != null && kegiatan.getId() != null && kegiatan.getJenisKegiatan() != null
					&& Boolean.TRUE.equals(kegiatan.getJenisKegiatan().getDigunakanSyaratKeaktifan()) && kegiatan.getMahasiswa() != null) {
				try {
					boolean terlambarLangsungTidakAktif = Common
							.getKonfigurasi("mhs_all_lambat_bayar_langsung_tidak_aktif", "", kegiatan.getSemster(),
									kegiatan.getMahasiswa().getTahunangkatan(), kegiatan.getMahasiswa().getJurusan(),
									kegiatan.getMahasiswa().getProgram(),
									kegiatan.getMahasiswa().getStatusAwalMahasiswa())
							.getNilai().equals(Konfigurasi.AKTIF);
					if (terlambarLangsungTidakAktif) {

						Mahasiswa mahasiswa = kegiatan.getMahasiswa();
						if (mahasiswa != null) {
							KegiatanAction.updateBatasStudiMahasiswa(mahasiswa, null, kegiatan.getSemster(), false);
						}
						HistoryStatusMahasiswa historyStatusMahasiswa = Common.currentStatus(kegiatan.getMahasiswa(),
								kegiatan.getTahunAkademik(), kegiatan.getSemster());
						if (historyStatusMahasiswa != null) {
							historyStatusMahasiswa.put("false", "checkStatusPembayaranMahasiswa");
							historyStatusMahasiswa.setStatusMahasiswa(ConstantValues.TIDAK_AKTIF);
						}
						System.out.println(
								"mahasiswa " + kegiatan.getMahasiswa() + ", checkStatusPembayaranMahasiswa false");
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/util/PembayaranUtil.java:2135");
				}
			}

			try {
				CommonEmail.infoBatalBayar(kegiatan, kegiatan.getRefNumber());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			session = HibernateUtil.currentNativeSession();

			session.getTransaction().begin();

			String sql = "update tunggakan_mahasiswa set kegiatan = null where kegiatan = " + kegiatan.getId() + ";";
			String sqlVirtualAccount = "update virtual_account_bank set kegiatan = null where kegiatan = "
					+ kegiatan.getId() + ";";

			// System.out.printlnsql);
			session.createSQLQuery(sql).executeUpdate();
			// Pertahankan histori virtual account, tetapi lepaskan referensinya sebelum
			// kegiatan pembayaran dihapus. Sebagian database lama belum memakai FK
			// ON DELETE CASCADE sehingga reversal sebelumnya dapat gagal di sini.
			session.createSQLQuery(sqlVirtualAccount).executeUpdate();

			PembayaranUtil.getInstance().getResetCicilanOld(session, kegiatan.getCalonMahasiswa(),
					kegiatan.getMahasiswa(), kegiatan.getSemster(), kegiatan.getJenisKegiatan(), kegiatan);

			// System.out.printlnsql);
			session.createSQLQuery(sql).executeUpdate();

			session.getTransaction().commit();

			HibernateUtil.closeSession();
			session = null;

			session = HibernateUtil.currentNativeSession();
			/* Jangan muat seluruh graph Kegiatan/DetailKegiatan ke persistence context.
			 * Pada data lama tertentu snapshot Hibernate mengandung state null dan proses
			 * dirty-check saat commit melempar NPE di TypeHelper.findDirty. Operasi reversal
			 * ini memang operasi set-based; jalankan langsung dengan parameter agar histori
			 * host tetap dipertahankan dan FK dilepas sebelum kegiatan dihapus. */
			session.getTransaction().begin();
			int kegiatanAda = ((Number) session.createSQLQuery(
					"select count(*) from kegiatan where id=:kegiatanId")
					.setLong("kegiatanId", kegiatanId.longValue()).uniqueResult()).intValue();
			if (kegiatanAda == 0) {
				session.getTransaction().commit();
				return true;
			}
			session.createSQLQuery("update log_host_to_host set nama="
					+ "'pembayaran telah ditarik kembali (reversal) dari request = ' || coalesce(nama,''), "
					+ "kegiatan=null where kegiatan=:kegiatanId")
					.setLong("kegiatanId", kegiatanId.longValue()).executeUpdate();
			session.createSQLQuery("delete from detail_kegiatan where kegiatan=:kegiatanId")
					.setLong("kegiatanId", kegiatanId.longValue()).executeUpdate();
			session.createSQLQuery("delete from kegiatan where id=:kegiatanId")
					.setLong("kegiatanId", kegiatanId.longValue()).executeUpdate();
			session.getTransaction().commit();

			HibernateUtil.closeSession();
			session = null;

		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"reversal pembayaran manual gagal, kegiatanId=" + kegiatanId);
			Common.tampilErrorJikaAdmin(e);
			if (session != null) {
				try {
					if (session.getTransaction() != null && session.getTransaction().isActive()) {
						session.getTransaction().rollback();
					}
				} catch (Exception rollbackError) {
					ais.common.ErrorAuditUtil.record(rollbackError,
							"rollback reversal pembayaran manual gagal, kegiatanId=" + kegiatanId);
				}
			}
			hasil = false;
		} finally {
			if (session != null) {
				try {
					session.clear();
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "clear session reversal pembayaran manual");
				}
				try {
					if (session.isConnected()) {
						session.disconnect();
					}
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "disconnect session reversal pembayaran manual");
				}
				try {
					if (session.isOpen()) {
						session.close();
					}
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "close session reversal pembayaran manual");
				}
			}
		}

		return hasil;
	}

	@SuppressWarnings("rawtypes")
	public Kegiatan simpanPembayaranMahasiswa(BankHost bankHost, JadwalPembayaran jadwalPembayaran,
			JenisKegiatan jenisKegiatan, Mahasiswa mahasiswa, Collection detailBiayas, Double amount, String bulan,
			String kodeAsli) {
		return simpanPembayaranMahasiswa(bankHost, jadwalPembayaran, jenisKegiatan, mahasiswa, detailBiayas, amount, "",
				bulan, kodeAsli);
	}

	@SuppressWarnings({ "rawtypes" })
	public Kegiatan simpanPembayaranMahasiswa(BankHost bankHost, JadwalPembayaran jadwalPembayaran,
			JenisKegiatan jenisKegiatan, Mahasiswa mahasiswa, Collection detailBiayas, Double amount,
			String tambahanKode, String bulan, String kodeAsli) {
		try {

			Boolean ganjil = jadwalPembayaran.getGanjil() == null ? Common.isNowSemensterGanjil()
					: jadwalPembayaran.getGanjil();
			Integer semester = Common.getSemester(mahasiswa.getTahunangkatan(), jadwalPembayaran.getTahunAkademik(),
					ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP, mahasiswa.getPindahKeKampusIniMasukSemester(),
					mahasiswa.getSemesterMulai());

			Kegiatan kegiatan = mahasiswa.ambilKegiatans(semester, jenisKegiatan, true);

			Session session = HibernateUtil.currentNativeSession();
			if (kegiatan != null && kegiatan.getId() != null) {
				session.refresh(kegiatan);
			} else {
				kegiatan = new Kegiatan();
			}
			session.refresh(mahasiswa);

			Double amountTotal = kegiatan.getAmount();

			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null);

			StatusMahasiswa statusMahasiswa = Common.currentStatus(krsMahasiswa).getStatusMahasiswa();
			statusMahasiswa = PembayaranUtilHelper.statusMahasiswaPembayaranEfektif(statusMahasiswa);

			kegiatan.setStatusMahasiswa(statusMahasiswa);
			kegiatan.setJadwalPembayaran(jadwalPembayaran);
			kegiatan.setMahasiswa(mahasiswa);
			kegiatan.setSemster(semester);
			kegiatan.setTahunAkademik(jadwalPembayaran.getTahunAkademik());
			kegiatan.setTanggal(ais.ui.util.WaktuUtil.getDate());
			kegiatan.setValidated(1);
			kegiatan.setValidator(bankHost == null ? "" : bankHost.getNama());
			kegiatan.setJenisKegiatan(jenisKegiatan);
			kegiatan.setAmount(amountTotal);
			kegiatan.setJumlahTelahDibayar(amountTotal);

			if (tambahanKode != null) {
				kegiatan.setTambahanKodeUnik(tambahanKode);
			}

			// Ambil ulang session native: helper bersarang (singkronkanKrsMahasiswa /
			// currentStatus / ambilKegiatans) dapat menutup session -> cegah "Session is closed!".
			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, kegiatan);
			// KE-18: refreshSaveOrUpdate() bisa self-heal via session INTERNAL berbeda
			// (getSafeSession) bila session di atas jadi tak valid di tengah proses --
			// perubahan itu tak terlihat oleh variabel lokal ini. Ambil ulang TEPAT
			// sebelum commit (pola sama dgn re-acquire di atas) agar tak commit ke
			// referensi session yang sudah closed -> "Session is closed!".
			session = HibernateUtil.currentNativeSession();
			try {
				session.getTransaction().commit();
			} catch (org.hibernate.SessionException sessionClosedEx) {
				// KE-FIX (lanjutan KE-18): meski di-ambil ulang TEPAT sebelum commit, sesi native
				// ThreadLocal bisa tetap tertutup di tengah alur pembayaran VA yang panjang
				// (dipanggil bertingkat dari Va servlet -> PembayaranAction -> PaymentLogic).
				// Jangan biarkan pembayaran mahasiswa gagal total karena race sesi murni --
				// buka sesi native BARU, simpan ulang "kegiatan" (belum ter-commit sebelumnya
				// karena flush di atas gagal), lalu commit sekali lagi.
				ais.common.ErrorAuditUtil.record(sessionClosedEx,
						"auto-audit src/ais/action/ws/util/PembayaranUtil.java:commit-retry-session-closed");
				session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, kegiatan);
				session.getTransaction().commit();
			}

			Collection<DetailKegiatan> detailKegiatans = kegiatan == null || kegiatan.getId() == null ? null
					: kegiatan.ambilDetailKegiatan();

			for (Object o : detailBiayas) {
				DetailBiaya detailBiaya = null;
				PengaturanPembayaranBulanan pengaturanPembayaranBulanan = null;
				if (o instanceof DetailBiaya) {
					detailBiaya = (DetailBiaya) o;
				} else {
					pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
					detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
				}

				DetailKegiatan detailKegiatan = pengaturanPembayaranBulanan != null
						? kegiatan.ambilSatuDetailKegiatan(pengaturanPembayaranBulanan, detailKegiatans)
						: kegiatan.ambilSatuDetailKegiatan(detailBiaya);
				if (detailKegiatan == null) {
					Double nilai = detailBiaya.hitungTotalKegiatan(kegiatan, session);
					detailKegiatan = new DetailKegiatan();
					detailKegiatan.setBiaya(nilai);
					detailKegiatan.setDetailBiaya(detailBiaya);
					detailKegiatan.setKeterangan(detailBiaya.getKeterangan());
					detailKegiatan.setKegiatan(kegiatan);

					// Ambil ulang session native: hitungTotalKegiatan() di atas dapat menutup session.
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.save(detailKegiatan);
					session.getTransaction().commit();
				}

			}

			if (bulan != null && !bulan.trim().isEmpty() && Common.isNumber(bulan)) {
				for (Object o : detailBiayas) {
					PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
					Double nominal = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa, semester);
					if (nominal > 0.1) {
						ItemBiaya itemBiaya = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya();

						// Ambil ulang session native (bisa ditutup helper bersarang) sebelum createCriteria.
						session = HibernateUtil.currentNativeSession();
						CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
								.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("kodeRequest", kodeAsli))
								.add(Restrictions.eq("kegiatan", kegiatan))
								.add(Restrictions.eq("bayarKe",
										pengaturanPembayaranBulanan.getDetailBiaya().getBayarKe()))
								.add(itemBiaya == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("itemBiaya", itemBiaya))
								.add(pengaturanPembayaranBulanan == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("pengaturanPembayaranBulanan", pengaturanPembayaranBulanan))
								.add(Restrictions.eq("nilai", nominal)).setMaxResults(1).uniqueResult();
						if (cicilanPembayaran == null) {
							cicilanPembayaran = new CicilanPembayaran(pengaturanPembayaranBulanan.getDetailBiaya());
						}

						cicilanPembayaran.setKodeRequest(kodeAsli);
						cicilanPembayaran.setKe(1);
						cicilanPembayaran.setKegiatan(kegiatan);
						cicilanPembayaran.setValidator(bankHost == null ? "" : bankHost.getNama());
						cicilanPembayaran.setItemBiaya(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya());
						cicilanPembayaran.setNilai(nominal);
						cicilanPembayaran.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
						cicilanPembayaran.setTanggal(ais.ui.util.WaktuUtil.getDate());
						cicilanPembayaran.setJenisPembayaran(bankHost.getJenisPembayaran());

						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, cicilanPembayaran);
						session.getTransaction().commit();
					}
				}

			} else {

				int indexKe = 1;
				for (Object o : detailBiayas) {
					DetailBiaya detailBiaya = null;

					PengaturanPembayaranBulanan pengaturanPembayaranBulanan = null;
					if (o instanceof DetailBiaya) {
						detailBiaya = (DetailBiaya) o;
					} else {
						pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
						detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
					}

					Double biaya = detailBiaya == null ? 0.0
							: detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
									: detailBiaya.getNilaiBiayaBaru();
					ItemBiaya itemBiaya = detailBiaya == null ? null : detailBiaya.getItemBiaya();

					// Session native bisa SUDAH ditutup helper bersarang (hitungTotalKegiatan /
					// refreshSaveOrUpdate / ambilKegiatans). Ambil ulang sebelum createCriteria agar
					// tidak "Session is closed!".
					session = HibernateUtil.currentNativeSession();
					CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
							.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("kodeRequest", kodeAsli))
							.add(Restrictions.eq("kegiatan", kegiatan))
							.add(Restrictions.eq("bayarKe", detailBiaya.getBayarKe()))
							.add(itemBiaya == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("itemBiaya", itemBiaya))
							.add(pengaturanPembayaranBulanan == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("pengaturanPembayaranBulanan", pengaturanPembayaranBulanan))
							.add(Restrictions.eq("nilai", biaya)).setMaxResults(1).uniqueResult();
					if (cicilanPembayaran == null) {
						cicilanPembayaran = new CicilanPembayaran(detailBiaya);
					}

					cicilanPembayaran.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
					cicilanPembayaran.setKodeRequest(kodeAsli);
					cicilanPembayaran.setValidator(bankHost == null ? "" : bankHost.getNama());
					cicilanPembayaran.setKe(indexKe);
					cicilanPembayaran.setKegiatan(kegiatan);
					cicilanPembayaran.setKeterangan("");
					cicilanPembayaran.setItemBiaya(itemBiaya);
					cicilanPembayaran.setNilai(biaya);
					cicilanPembayaran.setTanggal(ais.ui.util.WaktuUtil.getDate());
					cicilanPembayaran.setJenisPembayaran(bankHost.getJenisPembayaran());

					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, cicilanPembayaran);
					session.getTransaction().commit();
					indexKe++;
				}

			}

			if (amount != null && amount > 0.1) {
				LogPembayaran logPembayaran = new LogPembayaran();
				logPembayaran.setKegiatan(kegiatan);
				logPembayaran.setNominal(amount);
				// Ambil ulang session native: loop cicilan (refreshSaveOrUpdate) dapat menutup session.
				session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, logPembayaran);
				session.getTransaction().commit();
			}

			try {
				mahasiswa.reInitCicilan(session);
				mahasiswa.reInitDeposit(session);
				mahasiswa.reInitPengeluaranMahasiswa(session);
				mahasiswa.reInitDetailKegiatan(session);
				Double[] d = kegiatan.hitungTotalDanDendaFromCicilan();
				amountTotal = d[0];
				Double denda = d[1];

				kegiatan.setDenda(denda);
				kegiatan.setAmount(amountTotal);
				kegiatan.setJumlahTelahDibayar(amountTotal);
				detailKegiatans = kegiatan == null || kegiatan.getId() == null ? null : kegiatan.ambilDetailKegiatan();
				Double nilaiBiayaHarusDiBayars = 0.0;
				try {

					Collection mydetailBiayas = PembayaranUtil.getInstance().getDetailBiayaMahasiswa(mahasiswa,
							semester, jadwalPembayaran.getJenisKegiatan(), true);
					// Ambil ulang session native SEBELUM countBulanan: getDetailBiayaMahasiswa()
					// (dan reInit*/ambilDetailKegiatan sebelumnya) dapat menutup/mengganti session
					// native ThreadLocal -> variabel "session" lokal jadi stale/closed, menyebabkan
					// "Session is closed!" pada createCriteria di dalam countBulanan.
					session = HibernateUtil.currentNativeSession();
					int countPengaturanBulanan = PembayaranUtil.getInstance().countBulanan(session, mahasiswa,
							jadwalPembayaran.getJenisKegiatan(), semester, mydetailBiayas, true, true);
					if (countPengaturanBulanan > 0) {
						mydetailBiayas = PembayaranUtil.getInstance().getDetailBiayaMahasiswa(mahasiswa, semester,
								jadwalPembayaran.getJenisKegiatan(), "-1", true);
					}

					for (Object o : mydetailBiayas) {
						if (o instanceof DetailBiaya) {
							DetailBiaya detailBiaya = (DetailBiaya) o;
							nilaiBiayaHarusDiBayars += Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);
						} else if (o instanceof PengaturanPembayaranBulanan) {
							PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
							nilaiBiayaHarusDiBayars += Kegiatan.ambilJumlahTagihan(kegiatan, detailKegiatans, mahasiswa,
									semester, pengaturanPembayaranBulanan);
						}
					}

					System.out
							.println("mahasiswa " + mahasiswa + ", nilaiBiayaHarusDiBayars " + nilaiBiayaHarusDiBayars);

					if (countPengaturanBulanan > 0) {
						kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars);
					} else {
						kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars - (amountTotal + denda));
					}
					kegiatan.setAmount(amountTotal);

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				// Ambil ulang session native: reInit*/countBulanan/getDetailBiayaMahasiswa di atas
				// dapat menutup session -> cegah "Session is closed!" pada commit akhir.
				session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, kegiatan);
				session.getTransaction().commit();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/util/PembayaranUtil.java:2455");
			}

			HibernateUtil.closeSession();

			// System.out.println"================================== Simpan
			// Pembayaran untuk nim " + mahasiswa.getNim()
			// + ", kegiatan = " + kegiatan.getId() + " sukses dilakukan
			// ===================================");

			updateTunggakan(kegiatan, null);

			CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatan, true);

			return kegiatan;
		} catch (Exception e) {
			java.io.StringWriter sw = new java.io.StringWriter();
			e.printStackTrace(new java.io.PrintWriter(sw));
			lastSimpanException.set(sw.toString());
			Common.tampilErrorJikaAdmin(e);
			return null;
		}
	}

	public Kegiatan simpanPembayaranCalonMahasiswa(BankHost bankHost, JadwalPembayaran jadwalPembayaran,
			JenisKegiatan jenisKegiatan, BiodataCalonMahasiswa biodataCalonMahasiswa,
			java.util.Collection<DetailBiaya> detailBiayas, Double amount) {
		return simpanPembayaranCalonMahasiswa(bankHost, jadwalPembayaran, jenisKegiatan, biodataCalonMahasiswa,
				detailBiayas, amount, "");
	}

	@SuppressWarnings({})
	public Kegiatan simpanPembayaranCalonMahasiswa(BankHost bankHost, JadwalPembayaran jadwalPembayaran,
			JenisKegiatan jenisKegiatan, BiodataCalonMahasiswa biodataCalonMahasiswa,
			java.util.Collection<DetailBiaya> detailBiayas, Double amount, String tambahanKode) {
		Session session = null;
		try {

			Integer semester = jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()) ? 0 : 1;

			// Gunakan semester kegiatan/tagihan yang sudah ada. Ini menjaga pembayaran
			// H2H konsisten dengan layar ketika daftar ulang dikonfigurasi di semester 0.
			Kegiatan kegiatan = biodataCalonMahasiswa.ambilKegiatans(null, jenisKegiatan);
			if (kegiatan != null) {
				semester = kegiatan.getSemster();
			} else if (detailBiayas != null) {
				for (DetailBiaya detailBiaya : detailBiayas) {
					if (detailBiaya != null && detailBiaya.getSemester() != null) {
						semester = detailBiaya.getSemester();
						break;
					}
				}
				kegiatan = biodataCalonMahasiswa.ambilKegiatans(semester, jenisKegiatan);
			}

			session = HibernateUtil.currentNativeSession();

			Double amountTotal = amount;
			Number jumlahYangSudahDibayar = null;
			if (kegiatan == null || kegiatan.getId() == null) {
				kegiatan = new Kegiatan();
			} else {
				jumlahYangSudahDibayar = (Number) session.createCriteria(CicilanPembayaran.class)
						.add(Restrictions.eq("kegiatan", kegiatan)).setProjection(Projections.sum("nilai"))
						.uniqueResult();
				if (jumlahYangSudahDibayar != null) {
					amountTotal += jumlahYangSudahDibayar.doubleValue();
				}
			}

			kegiatan.setStatusMahasiswa(ConstantValues.AKTIF);
			kegiatan.setJadwalPembayaran(jadwalPembayaran);
			kegiatan.setCalonMahasiswa(biodataCalonMahasiswa);
			kegiatan.setSemster(semester);
			kegiatan.setTahunAkademik(biodataCalonMahasiswa.getTahunAkademik());
			kegiatan.setTanggal(ais.ui.util.WaktuUtil.getDate());
			kegiatan.setValidated(1);
			kegiatan.setValidator(bankHost == null ? "" : bankHost.getNama());
			kegiatan.setJenisKegiatan(jenisKegiatan);
			kegiatan.setAmount(amount);
			kegiatan.setJumlahTelahDibayar(amountTotal);

			if (tambahanKode != null) {
				kegiatan.setTambahanKodeUnik(tambahanKode);
			}
			Double nilaiBiayaHarusDiBayars = 0.0;
			try {

				for (DetailBiaya detailBiaya : detailBiayas) {
					Double nilai = detailBiaya.hitungTotalKegiatan(kegiatan, session);
					nilaiBiayaHarusDiBayars += Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);
				}
				kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars - amountTotal);
				kegiatan.setAmount(amountTotal);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			// System.out.println"================================== simpan
			// Pembayaran "
			// + biodataCalonMahasiswa.getNoRegistrasi() + "
			// ===================================");
			// Ambil ulang native session sebelum transaksi: helper di atas dapat menutup native
			// session ThreadLocal yang sama, sehingga begin()/commit() melempar
			// "Transaction not successfully started". Re-acquire menjamin session terbuka.
			session = HibernateUtil.currentNativeSession();
			try {
				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, kegiatan);
				if (session.getTransaction().isActive()) {
					session.getTransaction().commit();
				}
			} catch (org.hibernate.TransactionException txEx) {
				try { if (session.getTransaction().isActive()) session.getTransaction().rollback(); } catch (Exception re) { ais.common.ErrorAuditUtil.record(re, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranUtil.java:2554");}
				throw txEx;
			}

			for (DetailBiaya detailBiaya : detailBiayas) {
				DetailKegiatan detailKegiatan = kegiatan.ambilSatuDetailKegiatan(detailBiaya, session);
				if (detailKegiatan == null) {
					detailKegiatan = new DetailKegiatan();
					detailKegiatan.setBiaya(detailBiaya.hitungTotalKegiatan(kegiatan, session));
					detailKegiatan.setDetailBiaya(detailBiaya);
					detailKegiatan.setKeterangan(detailBiaya.getKeterangan());
					detailKegiatan.setKegiatan(kegiatan);

					try {
						session.getTransaction().begin();
						session.save(detailKegiatan);
						if (session.getTransaction().isActive()) {
							session.getTransaction().commit();
						}
					} catch (org.hibernate.TransactionException txEx2) {
						try { if (session.getTransaction().isActive()) session.getTransaction().rollback(); } catch (Exception re) { ais.common.ErrorAuditUtil.record(re, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranUtil.java:2574");}
					}
				}

			}

			// Ambil ulang session native: ambilCicilan() di atas dapat menutup/mengganti
			// session native ThreadLocal (dipakai lewat helper terpisah) -> cegah stale ref.
			session = HibernateUtil.currentNativeSession();
			Number jumlah = kegiatan.ambilCicilan().size();

			int indexKe = jumlah.intValue() + 1;
			for (Object o : detailBiayas) {
				DetailBiaya detailBiaya = null;

				PengaturanPembayaranBulanan pengaturanPembayaranBulanan = null;
				if (o instanceof DetailBiaya) {
					detailBiaya = (DetailBiaya) o;
				} else {
					pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
					detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
				}

				Double biaya = detailBiaya == null ? 0.0
						: detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
								: detailBiaya.getNilaiBiayaBaru();
				ItemBiaya itemBiaya = detailBiaya == null ? null : detailBiaya.getItemBiaya();

				// Ambil ulang session native TIAP iterasi: iterasi sebelumnya (refreshSaveOrUpdate/
				// commit) bisa self-heal via session internal berbeda sehingga variabel lokal ini
				// jadi stale/closed -> "Session is closed!" pada createCriteria berikut ini
				// (root cause ERROR A: PembayaranUtil.simpanPembayaranCalonMahasiswa).
				session = HibernateUtil.currentNativeSession();
				CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
						.createCriteria(CicilanPembayaran.class)
						.add(Restrictions.eq("bayarKe", detailBiaya.getBayarKe()))
						.add(Restrictions.eq("kodeRequest", biodataCalonMahasiswa.getNoRegistrasi()))
						.add(Restrictions.eq("kegiatan", kegiatan))
						.add(itemBiaya == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("itemBiaya", itemBiaya))
						.add(pengaturanPembayaranBulanan == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("pengaturanPembayaranBulanan", pengaturanPembayaranBulanan))
						.add(Restrictions.eq("nilai", biaya)).setMaxResults(1).uniqueResult();
				if (cicilanPembayaran == null) {
					cicilanPembayaran = new CicilanPembayaran(detailBiaya);
				}

				cicilanPembayaran.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
				cicilanPembayaran.setKodeRequest(biodataCalonMahasiswa.getNoRegistrasi());
				cicilanPembayaran.setValidator(bankHost == null ? "" : bankHost.getNama());
				cicilanPembayaran.setKe(indexKe);
				cicilanPembayaran.setKegiatan(kegiatan);
				cicilanPembayaran.setKeterangan("");
				cicilanPembayaran.setItemBiaya(itemBiaya);
				cicilanPembayaran.setNilai(biaya);
				cicilanPembayaran.setTanggal(ais.ui.util.WaktuUtil.getDate());
				cicilanPembayaran.setJenisPembayaran(bankHost.getJenisPembayaran());

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, cicilanPembayaran);
				session.getTransaction().commit();
				indexKe++;
			}

			if (amount != null && amount > 0.1) {
				LogPembayaran logPembayaran = new LogPembayaran();
				logPembayaran.setKegiatan(kegiatan);
				logPembayaran.setNominal(amount);
				// Ambil ulang session native: loop cicilan di atas dapat menutup session.
				session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, logPembayaran);
				session.getTransaction().commit();
			}
			// System.out.println"================================== Simpan
			// Pembayaran untuk No registrasi "
			// + biodataCalonMahasiswa.getNoRegistrasi()
			// + " sukses dilakukan ===================================");

			CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kegiatan, true);

			return kegiatan;
		} catch (Exception e) {
			// System.out.println"================================== Simpan
			// Pembayaran untuk No registrasi "
			// + biodataCalonMahasiswa.getNoRegistrasi() + " gagal dilakukan
			// ===================================");
			Common.tampilErrorJikaAdmin(e);
			return null;
		} finally {
			// Lock-timeout PostgreSQL menandai transaksi sebagai aborted. Selalu rollback
			// dan buang native session di boundary ini agar PaymentLogic tidak mewarisi
			// transaksi gagal ketika menyimpan log respons bank.
			if (session != null) {
				// Helper yang dipanggil di atas dapat lebih dulu menutup native session
				// ThreadLocal. Jangan memanggil getTransaction()/clear pada session tertutup.
				try { if (session.isOpen() && session.getTransaction() != null && session.getTransaction().isActive()) session.getTransaction().rollback(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "rollback pembayaran calon mahasiswa"); }
				try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "clear pembayaran calon mahasiswa"); }
				try { if (session.isOpen() && session.isConnected()) session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "disconnect pembayaran calon mahasiswa"); }
				try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "close pembayaran calon mahasiswa"); }
			}
			HibernateUtil.closeSession();
		}
	}

	public JenisKegiatan generateJenisKegiatan(String jenisKegiatanName) {

		return Common.getJenisKegiatan(jenisKegiatanName);
	}

	public JenisKegiatan generateJenisKegiatanByKode(String kodeJenisKegiatan) {
		return Common.getJenisKodeKegiatan(kodeJenisKegiatan);
	}

	@SuppressWarnings("rawtypes")
	public boolean checkApakahSudahMembayarCicilanUntukTagihan(Mahasiswa mahasiswa,
			PengaturanPembayaranBulanan pengaturanPembayaranBulanan, Collection detailBiayas, Integer semester) {
		if (pengaturanPembayaranBulanan == null) {
			return false;
		}

		int count = 0;
		for (Object o : detailBiayas) {
			if (o instanceof DetailBiaya) {
				count++;
			} else if (o instanceof PengaturanPembayaranBulanan) {
				PengaturanPembayaranBulanan biaya = (PengaturanPembayaranBulanan) o;
				Double nilai = biaya.ambilNominalModifikasi(mahasiswa, semester);
				if (nilai > 0.01) {
					count++;
				}
			}
		}

		return count == 0;
	}

	public PembayaranMahasiswa checkPembayaranMahasiswa(Mahasiswa mahasiswa, Integer semester,
			JenisKegiatan jenisKegiatan) {
		Session session = HibernateUtil.currentNativeSession();
		PembayaranMahasiswa pembayaranMahasiswa = (PembayaranMahasiswa) session
				.createCriteria(PembayaranMahasiswa.class).add(Restrictions.eq("mahasiswa", mahasiswa))
				.add(Restrictions.eq("jenisKegiatan", jenisKegiatan)).add(Restrictions.eq("semster", semester))
				.setMaxResults(1).uniqueResult();

		HibernateUtil.closeSession();
		return pembayaranMahasiswa;
	}

	// public Kegiatan checkKegiatanMahasiswaBerdasarJadwalPembayaran(Mahasiswa
	// mahasiswa,
	// JadwalPembayaran jadwalPembayaran, Integer semester) {
	// Kegiatan kegiatan = checkKegiatanMahasiswa(mahasiswa, semester,
	// jadwalPembayaran.getJenisKegiatan(),
	// jadwalPembayaran, false);
	//
	// return kegiatan;
	// }

	// public Kegiatan checkKegiatanCalonMahasiswa(BiodataCalonMahasiswa
	// calonMahasiswa, JenisKegiatan jenisKegiatan,
	// boolean reload) {
	// return checkKegiatanCalonMahasiswa(calonMahasiswa, jenisKegiatan, null,
	// reload);
	// }
	//
	// public Kegiatan checkKegiatanCalonMahasiswa(BiodataCalonMahasiswa
	// calonMahasiswa, JenisKegiatan jenisKegiatan,
	// Integer semester, boolean reload) {
	// return checkKegiatanCalonMahasiswa(calonMahasiswa, semester,
	// jenisKegiatan, reload);
	// }

	@SuppressWarnings("rawtypes")
	public List<PengaturanPembayaranBulanan> saringPengaturanPembayaranBulanan(List pengaturanPembayaranBulanans) {
		Map<String, PengaturanPembayaranBulanan> map = new java.util.HashMap<String, PengaturanPembayaranBulanan>();
		for (Object valueObject : pengaturanPembayaranBulanans) {
			try {
				if (valueObject instanceof PengaturanPembayaranBulanan) {
					PengaturanPembayaranBulanan pembayaranBulanan = (PengaturanPembayaranBulanan) valueObject;
					String bulan = pembayaranBulanan.getRealBulan() + "-"
							+ pembayaranBulanan.getDetailBiaya().getItemBiaya().getId();
					if (!map.containsKey(bulan)) {
						map.put(bulan, pembayaranBulanan);
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/util/PembayaranUtil.java:2742");
			}
		}
		pengaturanPembayaranBulanans = null;
		List<PengaturanPembayaranBulanan> bulanans = new ArrayList<PengaturanPembayaranBulanan>(map.values());
		Collections.sort(bulanans);
		return bulanans;
	}

	public void getResetCicilanOld(Session session, BiodataCalonMahasiswa biodataCalonMahasiswa, Mahasiswa mahasiswa,
			Integer semester, JenisKegiatan jenisKegiatan, Kegiatan kegiatan) {
		String key = PembayaranUtil.getInstance().getKeyPengaturanPembayaranBulananTanpaTampilYangSudahDibayar(
				biodataCalonMahasiswa, mahasiswa, semester, jenisKegiatan);
		CommonUtil.reset(key);
		if (kegiatan != null && kegiatan.getId() != null) {
			String sql = "delete from cicilan_pembayaran where kegiatan = " + kegiatan.getId();
			int hapus = session.createSQLQuery(sql).executeUpdate();
			System.out.println("sql " + sql + " " + hapus);
		}
	}

	public String getKeyPengaturanPembayaranBulananTanpaTampilYangSudahDibayar(
			BiodataCalonMahasiswa biodataCalonMahasiswa, Mahasiswa mahasiswa, Integer semester,
			JenisKegiatan jenisKegiatan) {
		String key = "getPengaturanPembayaranBulananTanpaTampilYangSudahDibayar_"
				+ (mahasiswa == null ? "" : "mhs_" + mahasiswa.getId())
				+ (biodataCalonMahasiswa == null ? "" : "calon_mhs_" + biodataCalonMahasiswa.getId()) + "_"
				+ jenisKegiatan.getId() + "_" + semester;
		return key;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Collection<PengaturanPembayaranBulanan> getPengaturanPembayaranBulananTanpaTampilYangSudahDibayarOld(
			Session session, BiodataCalonMahasiswa biodataCalonMahasiswa, Mahasiswa mahasiswa, Integer semester,
			JenisKegiatan jenisKegiatan, Kegiatan kegiatan, Collection<DetailBiaya> detailBiayas, boolean reload) {

		String key = getKeyPengaturanPembayaranBulananTanpaTampilYangSudahDibayar(biodataCalonMahasiswa, mahasiswa,
				semester, jenisKegiatan);

		if (!reload) {

			try {
				String s = mahasiswa != null ? mahasiswa.retreive(key) : biodataCalonMahasiswa.retreive(key);
				JSONObject data = s == null || s.trim().isEmpty() ? null : new JSONObject(s);
				if (data != null) {
					List d = new ArrayList();
					Iterator<String> iter = data.keys();
					while (iter.hasNext()) {
						String keyIter = iter.next();
						String value = data.getString(keyIter);

//						System.out.println("keyIter -> " + keyIter + ", value -> " + value);
						if (value.equalsIgnoreCase("1")) {
							DetailBiaya detailBiaya1 = (DetailBiaya) GeneralValueObject.ambilData(DetailBiaya.class,
									keyIter, true);
							if (mahasiswa != null) {
								detailBiaya1.updateKeterangan(mahasiswa, semester);
							}
							d.add(detailBiaya1);
						} else if (value.equalsIgnoreCase("2")) {
							PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) GeneralValueObject
									.ambilData(PengaturanPembayaranBulanan.class, keyIter, true);
							d.add(pengaturanPembayaranBulanan);
						}
					}

					try {
						Collections.sort(d);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/util/PembayaranUtil.java:2811");
					}

					return d;
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/util/PembayaranUtil.java:2817");
			}

		}

		String sqlQuery = kegiatan == null || kegiatan.getId() == null || kegiatan.getAmount() < 0.01 ? "1=1"
				: "(realbulan,item_biaya) not in (select b.realbulan,c.item_biaya from cicilan_pembayaran a inner join pengaturan_pembayaran_bulanan b on (a.pengaturan_pembayaran_bulanan=b.id) inner join detail_biaya c on (b.detail_biaya=c.id) inner join item_biaya d on (d.id=c.item_biaya) where a.kegiatan="
						+ kegiatan.getId()
						+ " and d.nilaibisadiubah=false group by b.realbulan,c.item_biaya  having sum(a.nilai) >= max(a.nilaiasli))";

		List<GeneralValueObject> pengaturanPembayaranBulanans = session
				.createCriteria(PengaturanPembayaranBulanan.class).createAlias("detailBiaya", "detailBiaya")
				.createAlias("detailBiaya.itemBiaya", "itemBiaya").add(Restrictions.in("detailBiaya", detailBiayas))

				.add(Restrictions.or(Restrictions.eq("tetapDitampilkanWalaupunNol", true),
						Restrictions.gt("nominal", 0.01)))

				.addOrder(Order.asc("bulan"))

				.add(Restrictions.sqlRestriction(sqlQuery))

				.addOrder(Order.asc("id")).list();

		List<PengaturanPembayaranBulanan> d = saringPengaturanPembayaranBulanan(pengaturanPembayaranBulanans);

		try {
			JSONObject data = new JSONObject();
			for (PengaturanPembayaranBulanan p : d) {
				data.put(p.getId().toString(), "2");
				GeneralValueObject.masukkanData(PengaturanPembayaranBulanan.class, p);
			}
			if (mahasiswa != null) {
				mahasiswa.put(data.toString(), key);
			}
			if (biodataCalonMahasiswa != null) {
				biodataCalonMahasiswa.put(data.toString(), key);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/util/PembayaranUtil.java:2855");
		}

		return d;
	}

	public Collection<PengaturanPembayaranBulanan> getPengaturanPembayaranSemua(Mahasiswa mahasiswa, Session session,
			Integer semester, JenisKegiatan jenisKegiatan, Collection<DetailBiaya> detailBiayas, boolean reload,
			boolean comitManual) {
		return getPengaturanPembayaranSemua(mahasiswa, null, semester, session, jenisKegiatan, detailBiayas, null,
				reload, comitManual);
	}

	public Collection<PengaturanPembayaranBulanan> getPengaturanPembayaranSemua(
			BiodataCalonMahasiswa biodataCalonMahasiswa, Session session, Integer semester, JenisKegiatan jenisKegiatan,
			Collection<DetailBiaya> detailBiayas, boolean reload, boolean comitManual) {
		return getPengaturanPembayaranSemua(null, biodataCalonMahasiswa, semester, session, jenisKegiatan, detailBiayas,
				null, reload, comitManual);
	}

	public Collection<PengaturanPembayaranBulanan> getPengaturanPembayaranSemua(Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, Integer semester, Session session, JenisKegiatan jenisKegiatan,
			Collection<DetailBiaya> detailBiayas, boolean reload, boolean comitManual) {
		return getPengaturanPembayaranSemua(mahasiswa, biodataCalonMahasiswa, semester, session, jenisKegiatan,
				detailBiayas, null, reload, comitManual);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Collection<PengaturanPembayaranBulanan> getPengaturanPembayaranSemua(Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, Integer semester, Session session, JenisKegiatan jenisKegiatan,
			Collection<DetailBiaya> detailBiayas, Integer bulan, boolean reload, boolean comitManual) {

		String key = "getPengaturanPembayaranSemua_"
				+ (biodataCalonMahasiswa != null ? "bulanan_cln_mhs_" + biodataCalonMahasiswa.getId()
						: "bulanan_mhs_" + mahasiswa.getId())
				+ "_" + jenisKegiatan.getId() + "_" + bulan + "_" + semester;

		if (!reload) {

			try {
				String s = biodataCalonMahasiswa != null ? biodataCalonMahasiswa.retreive(key)
						: mahasiswa.retreive(key);
				JSONObject data = s == null || s.trim().isEmpty() ? null : new JSONObject(s);
				if (data != null) {
					List d = new ArrayList();
					Iterator<String> iter = data.keys();
					while (iter.hasNext()) {
						String keyIter = iter.next();
						String value = data.getString(keyIter);

//						System.out.println("keyIter -> " + keyIter + ", value -> " + value);
						if (value.equalsIgnoreCase("1")) {
							DetailBiaya detailBiaya1 = (DetailBiaya) GeneralValueObject.ambilData(DetailBiaya.class,
									keyIter, true);
							if (biodataCalonMahasiswa != null) {

							} else if (mahasiswa != null) {
								detailBiaya1.updateKeterangan(mahasiswa, semester);
							}
							d.add(detailBiaya1);
						} else if (value.equalsIgnoreCase("2")) {
							PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) GeneralValueObject
									.ambilData(PengaturanPembayaranBulanan.class, keyIter, true);
							d.add(pengaturanPembayaranBulanan);
						}
					}

					try {
						Collections.sort(d);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/util/PembayaranUtil.java:2925");
					}

					return d;
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/util/PembayaranUtil.java:2931");
			}

		}

		List<GeneralValueObject> pengaturanPembayaranBulanans = session
				.createCriteria(PengaturanPembayaranBulanan.class)

				.add(bulan != null ? Restrictions.eq("realBulan", bulan) : Restrictions.sqlRestriction("true"))

				.createAlias("detailBiaya", "detailBiaya")

				.createAlias("detailBiaya.itemBiaya", "itemBiaya")

				.add(detailBiayas.isEmpty() ? Restrictions.sqlRestriction("false")
						: Restrictions.in("detailBiaya", detailBiayas))

				.add(Restrictions.or(Restrictions.eq("tetapDitampilkanWalaupunNol", true),
						Restrictions.or(Restrictions.eq("itemBiaya.nilaiBisaDiubah", true),
								Restrictions.or(Restrictions.eq("itemBiaya.penghitungan", ItemBiaya.DIKALI_NILAI_MINUS),
										Restrictions.gt("nominal", 0.01)))))

				.addOrder(Order.asc("bulan")).addOrder(Order.asc("id")).list();

		List<PengaturanPembayaranBulanan> d = saringPengaturanPembayaranBulanan(pengaturanPembayaranBulanans);
		try {
			JSONObject data = new JSONObject();
			for (PengaturanPembayaranBulanan p : d) {
				data.put(p.getId().toString(), "2");
				GeneralValueObject.masukkanData(PengaturanPembayaranBulanan.class, p);
			}
			if (mahasiswa != null) {
				mahasiswa.put(data.toString(), key);
			}
			if (biodataCalonMahasiswa != null) {
				biodataCalonMahasiswa.put(data.toString(), key);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/util/PembayaranUtil.java:2969");
		}

		return d;
	}

	@SuppressWarnings({})
	public Double[] getTotalDanDendaFromCicilan(Session session, Kegiatan kegiatan) {
		Object[] s = (Object[]) session.createCriteria(CicilanPembayaran.class).add(Restrictions.isNotNull("itemBiaya"))
				.add(Restrictions.eq("kegiatan", kegiatan))
				.setProjection(Projections.projectionList().add(Projections.sum("nilai")).add(Projections.sum("denda")))
				.uniqueResult();

		Double jumlah = 0.0;
		Double denda = 0.0;

		try {
			jumlah = s[0] == null ? 0.0 : Double.parseDouble(s[0].toString());
			denda = s[1] == null ? 0.0 : Double.parseDouble(s[1].toString());
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/util/PembayaranUtil.java:2989");
		}

		return new Double[] { jumlah, denda };
	}

	// @SuppressWarnings({})
	// public List<CicilanPembayaran> cicilanMahasiswa(Kegiatan kegiatan,
	// VOMahasiswa mahasiswa, boolean reload) {
	// return mahasiswa.ambilCicilanPembayaran(kegiatan);
	// }

}

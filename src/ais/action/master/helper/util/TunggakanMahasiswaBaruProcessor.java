package ais.action.master.helper.util;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.TimerTask;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.action.ws.util.ConstantUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.DetailBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.TunggakanMahasiswa;
import ais.database.model.TunggakanMahasiswaDetail;

public class TunggakanMahasiswaBaruProcessor extends TimerTask {

	public static List<Long> mahasiswas = new ArrayList<Long>();
	public static List<Long> mahasiswasSudah = new ArrayList<Long>();

	private Boolean bersihkanDulu = false;
	private Boolean bersihkanDuluDetail = true;
	private String tahunAkademik = "";

	private String localIp = "";

	private Boolean executeSekarang = false;

	public TunggakanMahasiswaBaruProcessor() {
		InetAddress thisIp;
		try {
			thisIp = InetAddress.getLocalHost();
			localIp = thisIp.getHostName();
			System.out.println("IP:" + localIp);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public TunggakanMahasiswaBaruProcessor(Boolean bersihkanDulu, Boolean bersihkanDuluDetail, Boolean executeSekarang,
			String tahunAkademik) {
		InetAddress thisIp;
		try {
			thisIp = InetAddress.getLocalHost();
			localIp = thisIp.getHostName();
			System.out.println("IP:" + localIp);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
		this.bersihkanDuluDetail = bersihkanDuluDetail;
		this.executeSekarang = executeSekarang;
		this.bersihkanDulu = bersihkanDulu;
		this.tahunAkademik = tahunAkademik;
	}

	@Override
	public void run() {

		System.out.println("mahasiswas = " + mahasiswas.size() + ", mahasiswasSudah = " + mahasiswasSudah.size());
		if (mahasiswas.size() != 0 && mahasiswas.size() != mahasiswasSudah.size()) {
			return;
		}
		mahasiswas = new ArrayList<Long>();
		mahasiswasSudah = new ArrayList<Long>();
		doProcess();
	}

	@SuppressWarnings("unchecked")
	private void doProcess() {

		Konfigurasi auto_proses_tunggakan = Common.getKonfigurasi("auto_proses_tunggakan_mhs_b", Konfigurasi.AKTIF,
				"2010", "", "");

		boolean ketemuIp = (auto_proses_tunggakan.getInfo1() != null
				&& auto_proses_tunggakan.getInfo1().trim().equals(localIp.trim()))
				|| (auto_proses_tunggakan.getInfo2() != null
						&& auto_proses_tunggakan.getInfo2().trim().equals(localIp.trim()))
				|| (auto_proses_tunggakan.getInfo3() != null
						&& auto_proses_tunggakan.getInfo3().trim().equals(localIp.trim()));

		System.out.println("IP Ketemu untuk TunggakanMahasiswaDaftarUlangProcessor ==> " + ketemuIp);

		if (executeSekarang || (auto_proses_tunggakan.getNilai().equals(Konfigurasi.AKTIF) && ketemuIp)) {

			PembayaranUtil pembayaranUtil;
			JenisKegiatan jenisKegiatan;

			pembayaranUtil = PembayaranUtil.getInstance();
			jenisKegiatan = pembayaranUtil.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU);

			Session mysession = HibernateUtil.currentNativeSession();

			if (bersihkanDulu) {

				mysession.getTransaction().begin();

				if (tahunAkademik.trim().equals("") && tahunAkademik.trim().equals("")) {
					String sql = "truncate tunggakan_mahasiswa cascade;";
					System.out.println(sql);
					mysession.createSQLQuery(sql).executeUpdate();
				} else {
					String sql = "delete from tunggakan_mahasiswa_detail where tunggakan_mahasiswa in (select id from tunggakan_mahasiswa where jenis_kegiatan = "
							+ jenisKegiatan.getId() + " and case when '" + tahunAkademik.trim()
							+ "' = '' then true else tahun_akademik = '" + tahunAkademik.trim()
							+ "'  end and semester = 1;";

					System.out.println(sql);
					mysession.createSQLQuery(sql).executeUpdate();

					sql = "delete from tunggakan_mahasiswa where jenis_kegiatan = " + jenisKegiatan.getId()
							+ " and case when '" + tahunAkademik.trim() + "' = '' then true else tahun_akademik = '"
							+ tahunAkademik.trim() + "'  end and semester = 1;";

					System.out.println(sql);
					mysession.createSQLQuery(sql).executeUpdate();
					mysession.getTransaction().commit();
				}
			}

			Integer tahunAngkatan = Common.getTahunAngkatan(1, Perkuliahan.GANJIL, tahunAkademik);

			mahasiswas = mysession.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("tahunangkatan", tahunAngkatan))
					.setProjection(Projections.property("id")).list();

			System.out.println("Jumlah mahasiswa ==> " + mahasiswas.size());

			Random random = new Random();
			for (int i = 0; i < mahasiswas.size(); i++) {
				int randomPosition = random.nextInt(mahasiswas.size());
				Long temp = mahasiswas.get(i);
				mahasiswas.set(i, mahasiswas.get(randomPosition));
				mahasiswas.set(randomPosition, temp);
			}

			// my
			HibernateUtil.closeSession();
	

			for (Long mahasiswaId : mahasiswas) {

				try {
					Session session = HibernateUtil.currentNativeSession();
					Mahasiswa mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.idEq(mahasiswaId)).uniqueResult();
					BiodataCalonMahasiswa calonMahasiswa = (BiodataCalonMahasiswa) session
							.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nim", mahasiswa.getNim()))
							.setMaxResults(1).uniqueResult();

					HibernateUtil.closeSession();

					mahasiswasSudah.add(mahasiswaId);
					if (calonMahasiswa == null || calonMahasiswa.getJenjang() == null) {
						continue;
					}

					int semester = 1;

					Integer tahunAkademikMulai = calonMahasiswa.getTahun();

					String kodeUnik = "calon_" + calonMahasiswa.getId() + "_" + semester + "_"
							+ (jenisKegiatan == null ? "" : jenisKegiatan.getId());

					Kegiatan kegiatan = calonMahasiswa.ambilKegiatans(semester, jenisKegiatan);

					mysession = null;
					mysession = HibernateUtil.currentNativeSession();
					TunggakanMahasiswa tunggakanMahasiswa = (TunggakanMahasiswa) mysession
							.createCriteria(TunggakanMahasiswa.class).add(Restrictions.eq("kodeUnik", kodeUnik))
							.setMaxResults(1).uniqueResult();

					if (tunggakanMahasiswa != null) {

						Collection<DetailBiaya> detailBiayas = pembayaranUtil.getDetailBiayaCalonMahasiswa(
								calonMahasiswa, jenisKegiatan, mahasiswa.getJurusan(), semester, false);

						Double totalBiaya = 0.0;
						for (DetailBiaya detailBiaya : detailBiayas) {
							Double nilai = detailBiaya.hitungTotalKegiatan(kegiatan, session);
							totalBiaya += nilai;
						}

						if (kegiatan != null && kegiatan.getAmount() != null && detailBiayas.size() != 0) {
							tunggakanMahasiswa
									.setDianggapLunas(kegiatan.getAmount().intValue() >= totalBiaya.intValue());
						}

						tunggakanMahasiswa.setKegiatan(kegiatan);
						tunggakanMahasiswa.setJumlahTunggakan(totalBiaya);
						mysession.getTransaction().begin();
						if (bersihkanDuluDetail) {
							insertTunggakanMahasiswaDetail(mysession, tunggakanMahasiswa, detailBiayas);
						}
						mysession.update(tunggakanMahasiswa);
						mysession.getTransaction().commit();
						// my
						HibernateUtil.closeSession();
				
						continue;
					} else {
						// my
						HibernateUtil.closeSession();
				
					}

					String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);

					System.out.print(
							"Terdapat mahasiswa belum bayar kodeUnik = " + kodeUnik + ". Check untuk tunggakan mhs = "
									+ calonMahasiswa.getNama() + ", nim : " + calonMahasiswa.getNim() + ", semester = "
									+ semester + ", tahunAkademik = " + tahunAkademik + ", tunggakanMahasiswa = "
									+ (tunggakanMahasiswa == null ? " Tidak ada" : " Sudah ada"));

					if (tunggakanMahasiswa == null) {

						try {
							tunggakanMahasiswa = new TunggakanMahasiswa();
							tunggakanMahasiswa.setKegiatan(kegiatan);
							tunggakanMahasiswa.setJenisKegiatan(jenisKegiatan);
							tunggakanMahasiswa.setKeterangan("");
							tunggakanMahasiswa.setKodeUnik(kodeUnik);
							tunggakanMahasiswa.setMahasiswa(mahasiswa);
							tunggakanMahasiswa.setSemester(semester);
							tunggakanMahasiswa.setTahunAkademik(tahunAkademik);

							Collection<DetailBiaya> detailBiayas = pembayaranUtil.getDetailBiayaCalonMahasiswa(
									calonMahasiswa, jenisKegiatan, mahasiswa.getJurusan(), semester, false);

							Double totalBiaya = 0.0;
							for (DetailBiaya detailBiaya : detailBiayas) {
								Double nilai = detailBiaya.hitungTotalKegiatan(kegiatan, session);
								totalBiaya += nilai;
							}

							if (kegiatan != null && kegiatan.getAmount() != null && detailBiayas.size() != 0) {
								tunggakanMahasiswa
										.setDianggapLunas(kegiatan.getAmount().intValue() >= totalBiaya.intValue());
							}

							tunggakanMahasiswa.setJumlahTunggakan(totalBiaya);
							session = null;
							session = HibernateUtil.currentNativeSession();
							session.getTransaction().begin();
							session.save(tunggakanMahasiswa);

							insertTunggakanMahasiswaDetail(session, tunggakanMahasiswa, detailBiayas);

							session.getTransaction().commit();

							HibernateUtil.closeSession();
						} catch (HibernateException e) {
							try {
								session.getTransaction().rollback();

								HibernateUtil.closeSession();
							} catch (Exception e1) {
								e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/util/TunggakanMahasiswaBaruProcessor.java:271");
							}
							Common.tampilErrorJikaAdmin(e);
						}

						// HibernateUtil.closeSession();
					}

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		}
	}

	public void insertTunggakanMahasiswaDetail(Session session, TunggakanMahasiswa tunggakanMahasiswa,
			Collection<DetailBiaya> detailBiayas) {
		session.createSQLQuery(
				"delete from tunggakan_mahasiswa_detail where tunggakan_mahasiswa = " + tunggakanMahasiswa.getId())
				.executeUpdate();
		for (DetailBiaya detailBiaya : detailBiayas) {

			TunggakanMahasiswaDetail tunggakanMahasiswaDetail = new TunggakanMahasiswaDetail();
			tunggakanMahasiswaDetail.setAngkatan(detailBiaya.getAngkatan());
			tunggakanMahasiswaDetail.setFakultas(detailBiaya.getFakultas());
			tunggakanMahasiswaDetail.setItemBiaya(detailBiaya.getItemBiaya());
			tunggakanMahasiswaDetail.setJenisKegiatan(detailBiaya.getJenisKegiatan());
			tunggakanMahasiswaDetail.setJenisSeleksi(detailBiaya.getJenisSeleksi());
			tunggakanMahasiswaDetail.setJenjang(detailBiaya.getJenjang());
			tunggakanMahasiswaDetail.setJurusan(detailBiaya.getJurusan());
			tunggakanMahasiswaDetail.setNama(detailBiaya.getNama());
			tunggakanMahasiswaDetail.setNilaiBiaya(detailBiaya.getNilaiBiaya());
			tunggakanMahasiswaDetail.setProgram(detailBiaya.getProgram());
			tunggakanMahasiswaDetail.setSemester(detailBiaya.getSemester());
			tunggakanMahasiswaDetail.setStatusMahasiswa(detailBiaya.getStatusMahasiswa());
			tunggakanMahasiswaDetail.setTahunAkademik(detailBiaya.getTahunAkademik());
			tunggakanMahasiswaDetail.setTunggakanMahasiswa(tunggakanMahasiswa);
			tunggakanMahasiswaDetail.setWnaAtauWni(detailBiaya.getWnaAtauWni());
			session.save(tunggakanMahasiswaDetail);
		}

	}

}

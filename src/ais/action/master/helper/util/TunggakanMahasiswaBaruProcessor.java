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

/**
 * Tugas terjadwal ({@link TimerTask}) yang menghitung/menyegarkan tunggakan pembayaran
 * pendaftaran ulang untuk mahasiswa baru (kegiatan
 * {@link ais.action.ws.util.ConstantUtil#PENDAFTARAN_ULANG_MAHASISWA_BARU}), disimpan sebagai
 * baris {@link TunggakanMahasiswa} beserta rinciannya ({@link TunggakanMahasiswaDetail}).
 *
 * <p>
 * State progres ({@link #mahasiswas}, {@link #mahasiswasSudah}) adalah <b>static</b>
 * (dibagi lintas seluruh instance/eksekusi timer): {@link #run()} akan melewati (skip)
 * eksekusi baru selama batch sebelumnya belum selesai diproses seluruhnya (menghindari dua
 * batch berjalan tumpang tindih), lalu mereset kedua daftar sebelum memulai batch baru. Daftar
 * mahasiswa diacak urutannya ({@link Random}) sebelum diproses satu per satu agar beban tidak
 * selalu dimulai dari mahasiswa dengan id terkecil.
 * </p>
 *
 * <p>
 * Proses hanya berjalan bila {@code executeSekarang} bernilai {@code true} (dipaksa jalan,
 * dipakai untuk pemicu manual/administratif) ATAU konfigurasi
 * {@code auto_proses_tunggakan_mhs_b} aktif DAN hostname mesin ini cocok salah satu dari tiga
 * alamat IP yang terdaftar pada konfigurasi tersebut (pola gerbang IP yang sama seperti pada
 * {@link RadiusProcessor} dan {@link AutoNotActivatingMahasiswaS2Processor} — nilai default IP
 * pada {@code Common.getKonfigurasi} kelas ini kosong, jadi gerbang IP hanya berlaku bila
 * konfigurasi sudah diisi manual di basis data, bukan tertanam di kode ini).
 * </p>
 *
 * <p>
 * Bila {@code bersihkanDulu} true, tabel {@code tunggakan_mahasiswa} (dan cascade-nya) dibersihkan
 * lebih dulu lewat SQL native langsung — baik <b>truncate seluruh tabel</b> (bila
 * {@code tahunAkademik} kosong) maupun {@code delete} tersaring per jenis kegiatan + tahun
 * akademik + semester 1. Untuk setiap mahasiswa baru (difilter berdasar tahun angkatan yang
 * dihitung dari {@code tahunAkademik}), method menghitung total tagihan lewat
 * {@link PembayaranUtil#getDetailBiayaCalonMahasiswa} dan membandingkannya dengan jumlah yang
 * sudah dibayar ({@code kegiatan.getAmount()}) untuk menentukan status lunas.
 * </p>
 */
public class TunggakanMahasiswaBaruProcessor extends TimerTask {

	/** Daftar id mahasiswa pada batch berjalan saat ini (state bersama lintas eksekusi timer). */
	public static List<Long> mahasiswas = new ArrayList<Long>();
	/** Subset {@link #mahasiswas} yang sudah selesai diproses pada batch berjalan; dipakai {@link #run()} untuk mendeteksi batch yang belum tuntas. */
	public static List<Long> mahasiswasSudah = new ArrayList<Long>();

	private Boolean bersihkanDulu = false;
	private Boolean bersihkanDuluDetail = true;
	private String tahunAkademik = "";

	private String localIp = "";

	private Boolean executeSekarang = false;

	/** Konstruktor default: tidak membersihkan data lebih dulu, tidak dipaksa jalan (bergantung gerbang IP+konfigurasi), tanpa filter tahun akademik. */
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

	/**
	 * @param bersihkanDulu        bila {@code true}, data tunggakan lama dihapus/di-truncate lebih dulu
	 * @param bersihkanDuluDetail  bila {@code true}, rincian tunggakan mahasiswa yang sudah ada dihapus & ditulis ulang saat diperbarui
	 * @param executeSekarang      bila {@code true}, proses dipaksa jalan tanpa mengecek gerbang IP/konfigurasi
	 * @param tahunAkademik        tahun akademik yang menjadi filter/dasar perhitungan tahun angkatan; kosong berarti tanpa filter
	 */
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

	/**
	 * Dipanggil oleh {@link java.util.Timer} sesuai jadwal. Melewati (skip) eksekusi ini bila
	 * batch sebelumnya masih berjalan (jumlah {@link #mahasiswas} belum sama dengan jumlah
	 * {@link #mahasiswasSudah}), lalu mereset kedua daftar statis dan mendelegasikan ke
	 * {@link #doProcess()}.
	 */
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

	/** Logika inti penghitungan/penyegaran tunggakan mahasiswa baru; lihat javadoc kelas untuk alur lengkapnya. */
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
							totalBiaya += Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);
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
								totalBiaya += Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);
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

	/**
	 * Mengganti seluruh rincian tunggakan ({@link TunggakanMahasiswaDetail}) milik
	 * {@code tunggakanMahasiswa}: menghapus rincian lama lewat SQL native, lalu menyimpan satu
	 * baris detail baru untuk tiap {@link DetailBiaya} pada {@code detailBiayas}, menyalin
	 * atribut biaya (angkatan, fakultas, item biaya, jenjang, jurusan, nilai, dsb.) apa adanya.
	 *
	 * @param session          sesi Hibernate aktif tempat operasi dijalankan (dalam transaksi pemanggil)
	 * @param tunggakanMahasiswa induk tunggakan yang rinciannya akan diganti
	 * @param detailBiayas     komponen biaya sumber yang akan disalin menjadi rincian tunggakan
	 */
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

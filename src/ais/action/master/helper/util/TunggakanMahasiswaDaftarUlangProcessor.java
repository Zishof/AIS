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

import ais.action.master.helper.PembayaranUtilHelper;
import ais.action.ws.util.CommonUtil;
import ais.action.ws.util.ConstantUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.TunggakanMahasiswa;
import ais.database.model.TunggakanMahasiswaDetail;

/**
 * Tugas terjadwal ({@link TimerTask}) yang menghitung ulang <b>tunggakan daftar ulang</b>
 * (jenis kegiatan {@link ConstantUtil#PENDAFTARAN_MAHASISWA_LAMA}) untuk seluruh mahasiswa aktif,
 * per semester sejak semester 2 hingga semester berjalan mahasiswa tersebut, dan menyimpan/
 * memperbarui hasilnya sebagai baris {@link TunggakanMahasiswa} (+ rincian
 * {@link TunggakanMahasiswaDetail} per item biaya).
 *
 * <p>
 * <b>Kendali eksekusi ganda (multi-instance/cluster)</b>: konfigurasi {@code auto_proses_tunggakan}
 * menyimpan hingga tiga hostname (kolom {@code info1}/{@code info2}/{@code info3}) yang diizinkan
 * menjalankan proses ini; {@link #localIp} (sebenarnya hostname lokal, diambil dari
 * {@link InetAddress#getLocalHost()} saat konstruksi) dicocokkan terhadap ketiganya sebelum
 * {@link #doProcess()} benar-benar berjalan — mencegah beberapa node aplikasi memproses tunggakan
 * secara bersamaan. Kolom {@code info1} juga dipakai ganda sebagai tahun akademik mulai
 * (default 2010) pemrosesan.
 * </p>
 *
 * <p>
 * <b>Kendali tumpang tindih antar-pemanggilan timer</b>: field statis {@link #mahasiswas} (daftar
 * id mahasiswa yang harus diproses siklus berjalan) dan {@link #mahasiswasSudah} (id yang sudah
 * selesai diproses) dibagi lintas instance; {@link #run()} membatalkan diri (tidak memproses apa
 * pun) bila siklus sebelumnya belum selesai (jumlah keduanya belum sama), mencegah dua siklus
 * berjalan bertumpuk. Urutan pemrosesan mahasiswa diacak ({@link Random}) setiap siklus agar beban
 * tidak selalu jatuh pada mahasiswa yang sama lebih dulu bila proses terhenti di tengah jalan.
 * </p>
 *
 * <p>
 * Bila {@link #bersihkanDulu} aktif, data tunggakan lama (untuk kombinasi tahun akademik/jenis
 * semester yang diberikan, atau SELURUH tabel via {@code TRUNCATE ... CASCADE} bila keduanya
 * kosong) dihapus lebih dulu lewat SQL native sebelum dihitung ulang. Nilai tunggakan per semester
 * dihitung dari {@link PembayaranUtilHelper#getDetailBiayaMahasiswa} dikurangi jumlah yang sudah
 * dibayarkan pada {@link Kegiatan} terkait ({@code kegiatan.getAmount()}); mahasiswa dianggap lunas
 * bila jumlah dibayar sudah mencakup seluruh total biaya.
 * </p>
 */
public class TunggakanMahasiswaDaftarUlangProcessor extends TimerTask {

	/** Daftar id mahasiswa yang harus diproses pada siklus berjalan; dibagi statis untuk mendeteksi tumpang tindih antar-pemanggilan (lihat javadoc kelas). */
	public static List<Long> mahasiswas = new ArrayList<Long>();
	/** Daftar id mahasiswa yang sudah selesai diproses pada siklus berjalan; dibandingkan ukurannya dengan {@link #mahasiswas} di {@link #run()}. */
	public static List<Long> mahasiswasSudah = new ArrayList<Long>();

	private Boolean bersihkanDulu = false;
	private String tahunAkademik = "";
	private String jenisSemester = "";
	private Boolean bersihkanDuluDetail = true;

	private String localIp = "";

	private Boolean executeSekarang = false;

	/** Konstruktor mode terjadwal biasa: memproses sesuai gating IP/konfigurasi {@code auto_proses_tunggakan}, tanpa membersihkan data lama lebih dulu, tanpa filter tahun akademik/jenis semester. */
	public TunggakanMahasiswaDaftarUlangProcessor() {
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
	 * Konstruktor mode terkontrol penuh, dipakai mis. untuk pemicuan manual dari layar admin.
	 *
	 * @param bersihkanDulu       hapus data {@link TunggakanMahasiswa} lama (untuk filter
	 *                            {@code tahunAkademik}/{@code jenisSemester}, atau seluruhnya bila
	 *                            keduanya kosong) sebelum dihitung ulang
	 * @param bersihkanDuluDetail hapus & tulis ulang {@link TunggakanMahasiswaDetail} setiap kali
	 *                            baris {@link TunggakanMahasiswa} yang sudah ada diperbarui
	 * @param executeSekarang     lewati gating IP/konfigurasi {@code auto_proses_tunggakan} dan
	 *                            jalankan proses sekarang juga
	 * @param tahunAkademik       filter tahun akademik untuk pembersihan data lama, boleh kosong
	 * @param jenisSemester       filter jenis semester ({@link Perkuliahan#GENAP} atau ganjil)
	 *                            untuk pembersihan data lama, boleh kosong
	 */
	public TunggakanMahasiswaDaftarUlangProcessor(Boolean bersihkanDulu, Boolean bersihkanDuluDetail,
			Boolean executeSekarang, String tahunAkademik, String jenisSemester) {
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
		this.jenisSemester = jenisSemester;
	}

/**
	 * Dipanggil oleh timer/scheduler. Membatalkan diri tanpa melakukan apa pun bila siklus
	 * sebelumnya (dilacak lewat {@link #mahasiswas}/{@link #mahasiswasSudah}) belum selesai;
	 * jika tidak, mengosongkan kedua daftar statis lalu mendelegasikan ke {@link #doProcess()}.
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

	/**
	 * Implementasi inti satu siklus perhitungan ulang tunggakan daftar ulang. Lihat javadoc kelas
	 * untuk uraian lengkap gating eksekusi, pembersihan data lama, dan logika perhitungan nilai
	 * tunggakan per semester. Galat per mahasiswa ditangkap dan dilaporkan lewat
	 * {@code Common.tampilErrorJikaAdmin} — satu mahasiswa gagal tidak menghentikan pemrosesan
	 * mahasiswa lain.
	 */
	@SuppressWarnings("unchecked")
	private void doProcess() {

		Konfigurasi auto_proses_tunggakan = Common.getKonfigurasi("auto_proses_tunggakan", Konfigurasi.AKTIF, "2010",
				"", "");

		boolean ketemuIp = (auto_proses_tunggakan.getInfo1() != null
				&& auto_proses_tunggakan.getInfo1().trim().equals(localIp.trim()))
				|| (auto_proses_tunggakan.getInfo2() != null
						&& auto_proses_tunggakan.getInfo2().trim().equals(localIp.trim()))
				|| (auto_proses_tunggakan.getInfo3() != null
						&& auto_proses_tunggakan.getInfo3().trim().equals(localIp.trim()));

		System.out.println("IP Ketemu untuk TunggakanMahasiswaDaftarUlangProcessor ==> " + ketemuIp);

		if (executeSekarang || (auto_proses_tunggakan.getNilai().equals(Konfigurasi.AKTIF) && ketemuIp)) {

			Integer tahunMulai = 2010;
			try {
				tahunMulai = Integer.parseInt(auto_proses_tunggakan.getInfo1().trim());
			} catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/master/helper/util/TunggakanMahasiswaDaftarUlangProcessor.java:106");
			}

			PembayaranUtil pembayaranUtil;
			JenisKegiatan jenisKegiatan;

			pembayaranUtil = PembayaranUtil.getInstance();
			jenisKegiatan = pembayaranUtil.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA);

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
							+ "'  end and case when '" + jenisSemester.trim() + "' = '' then true else semester % 2 = "
							+ (jenisSemester.trim().equals(Perkuliahan.GENAP) ? "0" : "1") + " end);";

					System.out.println(sql);
					mysession.createSQLQuery(sql).executeUpdate();

					sql = "delete from tunggakan_mahasiswa where jenis_kegiatan = " + jenisKegiatan.getId()
							+ " and case when '" + tahunAkademik.trim() + "' = '' then true else tahun_akademik = '"
							+ tahunAkademik.trim() + "'  end and case when '" + jenisSemester.trim()
							+ "' = '' then true else semester % 2 = "
							+ (jenisSemester.trim().equals(Perkuliahan.GENAP) ? "0" : "1") + " end;";

					System.out.println(sql);
					mysession.createSQLQuery(sql).executeUpdate();
					mysession.getTransaction().commit();
				}
			}

			mahasiswas = mysession.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.property("id")).list();

			System.out.println("Jumlah mahasiswa ==> " + mahasiswas.size());

			Random random = new Random();
			for (int i = 0; i < mahasiswas.size(); i++) {
				int randomPosition = random.nextInt(mahasiswas.size());
				Long temp = mahasiswas.get(i);
				mahasiswas.set(i, mahasiswas.get(randomPosition));
				mahasiswas.set(randomPosition, temp);
			}

			// myHibernateUtil.closeSession();

			for (Long mahasiswaId : mahasiswas) {

				try {
					Session session = HibernateUtil.currentNativeSession();
					Mahasiswa mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.idEq(mahasiswaId)).uniqueResult();

					HibernateUtil.closeSession();

					mahasiswasSudah.add(mahasiswaId);
					if (mahasiswa.getJenjang() == null) {
						continue;
					}

					Boolean ganjil = CommonUtil.isNowSemensterGanjil();
					Integer semesterSaatIni = CommonUtil.getSemester(mahasiswa.getTahunangkatan(), ganjil,
							mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

					for (int semester = 2; semester <= semesterSaatIni; semester++) {

						Integer tahunAkademikMulai = Common.getTahunAkademik(semester, mahasiswa.getTahunangkatan(),
								mahasiswa.getSemesterMulai());

						if (tahunAkademikMulai < tahunMulai) {
							continue;
						}

						String kodeUnik = mahasiswa.getId() + "_" + semester + "_"
								+ (jenisKegiatan == null ? "" : jenisKegiatan.getId());

						Kegiatan kegiatan = mahasiswa.ambilKegiatans(semester, jenisKegiatan);

						mysession = null;
						mysession = HibernateUtil.currentNativeSession();
						TunggakanMahasiswa tunggakanMahasiswa = (TunggakanMahasiswa) mysession
								.createCriteria(TunggakanMahasiswa.class).add(Restrictions.eq("kodeUnik", kodeUnik))
								.setMaxResults(1).uniqueResult();

						if (tunggakanMahasiswa != null) {

							Collection<DetailBiaya> detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa,
									semester, jenisKegiatan, null, false);

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

						System.out.print("Terdapat mahasiswa belum bayar. Check untuk tunggakan mhs = "
								+ mahasiswa.getNama() + ", nim : " + mahasiswa.getNim() + ", semester = " + semester
								+ ", tahunAkademik = " + tahunAkademik + ", tunggakanMahasiswa = "
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

								Collection<DetailBiaya> detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa,
										semester, jenisKegiatan, null, false);
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
								} catch (HibernateException e1) {
									e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/util/TunggakanMahasiswaDaftarUlangProcessor.java:281");
								}
								Common.tampilErrorJikaAdmin(e);
							}

						}
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}
			}
		}
	}

	/**
	 * Menulis ulang rincian {@link TunggakanMahasiswaDetail} milik satu {@code tunggakanMahasiswa}:
	 * menghapus seluruh baris detail lama miliknya (SQL native), lalu menyisipkan satu baris detail
	 * baru per {@link DetailBiaya}, menyalin atribut kategori (jenjang, jurusan, fakultas, angkatan,
	 * dst.) dan nilai biaya apa adanya dari sumbernya.
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

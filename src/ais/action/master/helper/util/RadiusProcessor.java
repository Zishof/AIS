package ais.action.master.helper.util;

import java.net.InetAddress;
import java.util.List;
import java.util.TimerTask;

import org.hibernate.Session;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.RadiusHibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.radius.Radcheck;

/**
 * Tugas terjadwal ({@link TimerTask}) yang mensinkronkan kredensial login AIS ({@link Tbmuser},
 * {@link Mahasiswa}, termasuk akun orang tua mahasiswa) ke tabel {@code radcheck} pada basis
 * data server RADIUS ({@link RadiusHibernateUtil}), agar akun yang sama dapat dipakai untuk
 * autentikasi jaringan (mis. Wi-Fi kampus) via protokol RADIUS.
 *
 * <p>
 * <b>Keamanan — WASPADAI</b>:
 * </p>
 * <ul>
 * <li>Method {@link #doProcess()} men-<b>dekripsi</b> password pengguna yang tersimpan
 * terenkripsi di basis data AIS ({@code Common.desEncrypter.get().decrypt(...)}) menjadi
 * teks polos, lalu menuliskannya <b>sebagai teks polos</b> ke kolom {@code value} pada tabel
 * {@code radcheck} (atribut {@code "Password"}, operator {@code "=="}) — ini konvensi umum
 * RADIUS/FreeRADIUS untuk PAP, namun berarti password plaintext pengguna (termasuk password
 * akun orang tua siswa, {@code userOrtu}/{@code passOrtu}) disalin dan disimpan permanen di
 * basis data RADIUS terpisah, memperluas permukaan risiko bila basis data RADIUS itu bocor.
 * <b>Belum diperbaiki di sini</b> — beralih dari PAP ke CHAP/MS-CHAP di sisi FreeRADIUS TIDAK
 * menghilangkan kebutuhan ini: kedua protokol tersebut tetap mengharuskan server RADIUS
 * menyimpan sesuatu yang setara dengan password reversibel (plaintext atau NT-hash, yang
 * fungsinya identik sebagai kredensial autentikasi langsung) agar bisa memvalidasi
 * challenge-response — bukan sekadar pergantian kolom/atribut di {@code radcheck}. Migrasi
 * yang benar-benar menghilangkan kebutuhan ini (mis. WPA2/WPA3-Enterprise dengan EAP-TLS
 * berbasis sertifikat per perangkat) adalah proyek infrastruktur jaringan tersendiri (konfigurasi
 * ulang access point/FreeRADIUS, distribusi sertifikat ke seluruh perangkat pengguna) — di luar
 * cakupan perubahan kode ini; mitigasi jangka pendek yang realistis adalah membatasi akses ke
 * basis data RADIUS itu sendiri (isolasi jaringan/firewall, kredensial DB berhak akses minimal).</li>
 * <li><b>Diperbaiki</b>: proses ini digerbangi oleh konfigurasi {@code radius_syncrhonizer} yang
 * nilai default {@code info1}/{@code info2}/{@code info3}-nya sebelumnya memuat daftar
 * <b>alamat IP produksi tertanam langsung di kode</b> ({@code 116.66.206.181},
 * {@code 171.27.27.10}, {@code 172.27.18.181}) — kini nilai seed default tersebut dikosongkan
 * (bila baris {@link Konfigurasi} belum ada di database, dibuat kosong; harus diisi eksplisit
 * lewat menu Konfigurasi agar sinkronisasi berjalan di suatu host). Baris {@link Konfigurasi}
 * yang sudah ada di database produksi (skenario paling umum) tidak terpengaruh — nilai
 * {@code info1}/{@code info2}/{@code info3} tersimpan di database yang dibaca lebih dulu,
 * bukan dari default di kode ini; lihat {@link Common#getKonfigurasi(String, String, String,
 * String, String)}. Sinkronisasi tetap hanya berjalan bila hostname mesin saat ini cocok dengan
 * salah satu IP terkonfigurasi (dibandingkan sebagai string, bukan validasi IP sesungguhnya).</li>
 * </ul>
 */
public class RadiusProcessor extends TimerTask {

	/** Hostname mesin lokal, dipakai untuk mencocokkan gerbang IP pada {@link #doProcess()}. */
	private String localIp = "";

	/** Menentukan hostname mesin lokal (untuk gerbang IP); kegagalan resolusi host ditampilkan lewat {@link Common#tampilErrorJikaAdmin(Exception)}. */
	public RadiusProcessor() {
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

	/** Dipanggil oleh {@link java.util.Timer} sesuai jadwal; mendelegasikan ke {@link #doProcess()}. */
	@Override
	public void run() {
		doProcess();
	}

	/**
	 * Bila konfigurasi {@code radius_syncrhonizer} aktif DAN hostname mesin ini cocok dengan
	 * salah satu IP yang terdaftar di konfigurasi tersebut (lihat catatan keamanan pada kelas):
	 * mengambil seluruh {@link Tbmuser} dan {@link Mahasiswa} aktif, mendekripsi passwordnya,
	 * lalu untuk tiap akun membuat atau memperbarui baris {@link Radcheck} yang bersangkutan di
	 * basis data RADIUS hanya bila nilainya belum ada atau berubah (menghindari update
	 * berlebihan). Akun orang tua mahasiswa ({@code userOrtu}/{@code passOrtu}) diproses secara
	 * terpisah dengan {@code kodeUniq} berawalan {@code "_ortu_"}. Setiap baris diproses dalam
	 * transaksi Hibernate sendiri terhadap {@link RadiusHibernateUtil}; kedua sesi (AIS dan
	 * RADIUS) ditutup di akhir proses.
	 */
	@SuppressWarnings("unchecked")
	private void doProcess() {

		Konfigurasi auto_proses_tunggakan = Common.getKonfigurasi(
				"radius_syncrhonizer", Konfigurasi.AKTIF, "",
				"", "");

		boolean ketemuIp = (auto_proses_tunggakan.getInfo1() != null && auto_proses_tunggakan
				.getInfo1().trim().equals(localIp.trim()))
				|| (auto_proses_tunggakan.getInfo2() != null && auto_proses_tunggakan
						.getInfo2().trim().equals(localIp.trim()))
				|| (auto_proses_tunggakan.getInfo3() != null && auto_proses_tunggakan
						.getInfo3().trim().equals(localIp.trim()));

		System.out.println("IP Ketemu untuk RadiusProcessor ==> " + ketemuIp);

		if (auto_proses_tunggakan.getNilai().equals(Konfigurasi.AKTIF)
				&& ketemuIp) {

			System.out.println("=========== RADIUS PROCESS ==========");

			Session session = HibernateUtil.currentNativeSession();
			Session radiusSession = RadiusHibernateUtil.getInstance()
					.currentSession();

			ProjectionList projectionList = Projections.projectionList();
			projectionList.add(Projections.property("userId"));
			projectionList.add(Projections.property("userPassword"));

			List<Object[]> tbmusers = session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(projectionList).list();

			System.out.println("=========== PROSES " + tbmusers.size()
					+ " data pengguna ==========");

			for (Object[] tbmuser : tbmusers) {

				String userId = tbmuser[0] == null ? "" : tbmuser[0].toString();
				String userPassword = tbmuser[1] == null ? "" : tbmuser[1]
						.toString();

				if (userId == null || userId.trim().equals("")) {
					continue;
				}
				String pass = "";
				try {
					pass = Common.desEncrypter.get().decrypt(userPassword);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e); 
				}

				Integer count = ((Number) radiusSession
						.createCriteria(Radcheck.class)
						.setProjection(Projections.rowCount())
						.add(Restrictions.eq("value", pass))
						.add(Restrictions.eq("kodeUniq", tbmuser.getClass()
								.getName() + "_users_" + userId))
						.setMaxResults(1).uniqueResult()).intValue();

				if (count.equals(0)) {
					Radcheck radcheck = (Radcheck) radiusSession
							.createCriteria(Radcheck.class)
							.add(Restrictions.eq("kodeUniq", tbmuser.getClass()
									.getName() + "_users_" + userId))
							.setMaxResults(1).uniqueResult();
					System.out.println("radcheck baru / ganti password ==> "
							+ radcheck);
					if (radcheck == null) {
						radcheck = new Radcheck();
					}

					if (radcheck.getValue() == null
							|| radcheck.getValue().trim().equals("")
							|| !radcheck.getValue().equals(pass)) {
						radcheck.setAttribute("Password");
						radcheck.setKodeUniq(tbmuser.getClass().getName()
								+ "_users_" + userId);
						radcheck.setOp("==");
						radcheck.setUsername(userId.trim());
						radcheck.setValue(pass);
						radiusSession.getTransaction().begin();
						radiusSession.saveOrUpdate(radcheck);
						radiusSession.getTransaction().commit();
					}
					radcheck = null;
				}
			}

			tbmusers = null;

			projectionList = Projections.projectionList();
			projectionList.add(Projections.property("id"));
			projectionList.add(Projections.property("nim"));
			projectionList.add(Projections.property("pass"));
			projectionList.add(Projections.property("userOrtu"));
			projectionList.add(Projections.property("passOrtu"));

			List<Object[]> mahasiswas = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(projectionList).list();

			System.out.println("=========== PROSES " + mahasiswas.size()
					+ " data mahasiswa ==========");

			for (Object[] mahasiswa : mahasiswas) {

				Object id = mahasiswa[0];
				Object nim = mahasiswa[1];
				Object mypass = mahasiswa[2];
				Object userOrtu = mahasiswa[3];
				Object passOrtu = mahasiswa[4];

				if (nim == null || nim.toString().trim().equals("")) {
					continue;
				}

				String pass = "";
				try {
					pass = Common.desEncrypter.get().decrypt(mypass.toString());
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e); 
				}
				Integer count = ((Number) radiusSession
						.createCriteria(Radcheck.class)
						.setProjection(Projections.rowCount())
						.add(Restrictions.eq("value", pass))
						.add(Restrictions.eq("kodeUniq", mahasiswa.getClass()
								.getName() + "_" + id)).setMaxResults(1)
						.uniqueResult()).intValue();

				if (count.equals(0)) {

					Radcheck radcheck = (Radcheck) radiusSession
							.createCriteria(Radcheck.class)
							.add(Restrictions.eq("kodeUniq", mahasiswa
									.getClass().getName() + "_" + id))
							.setMaxResults(1).uniqueResult();
					System.out.println("radcheck baru / ganti password ==> "
							+ radcheck);
					if (radcheck == null) {
						radcheck = new Radcheck();
					}

					if (radcheck.getValue() == null
							|| radcheck.getValue().trim().equals("")
							|| !radcheck.getValue().equals(pass)) {
						radcheck.setAttribute("Password");
						radcheck.setKodeUniq(mahasiswa.getClass().getName()
								+ "_" + id);
						radcheck.setOp("==");
						radcheck.setUsername(nim.toString().trim());
						radcheck.setValue(pass + "");
						radiusSession.getTransaction().begin();
						radiusSession.saveOrUpdate(radcheck);
						radiusSession.getTransaction().commit();
					}
					radcheck = null;
				}

				if (userOrtu != null && !userOrtu.toString().trim().equals("")) {
					pass = "";
					try {
						pass = Common.desEncrypter.get().decrypt(passOrtu.toString());
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e); 
					}

					count = ((Number) radiusSession
							.createCriteria(Radcheck.class)
							.setProjection(Projections.rowCount())
							.add(Restrictions.eq("value", pass))
							.add(Restrictions.eq("kodeUniq", mahasiswa
									.getClass().getName() + "_ortu_" + id))
							.setMaxResults(1).uniqueResult()).intValue();

					if (count.equals(0)) {
						Radcheck radcheck = (Radcheck) radiusSession
								.createCriteria(Radcheck.class)
								.add(Restrictions.eq("kodeUniq", mahasiswa
										.getClass().getName() + "_ortu_" + id))
								.setMaxResults(1).uniqueResult();
						System.out
								.println("radcheck baru / ganti password ==> "
										+ radcheck);
						if (radcheck == null) {
							radcheck = new Radcheck();
						}
						if (radcheck.getValue() == null
								|| radcheck.getValue().trim().equals("")
								|| !radcheck.getValue().equals(pass)) {
							radcheck.setAttribute("Password");
							radcheck.setKodeUniq(mahasiswa.getClass().getName()
									+ "_ortu_" + id);
							radcheck.setOp("==");
							radcheck.setUsername(userOrtu.toString().trim());
							radcheck.setValue(pass);
							radiusSession.getTransaction().begin();
							radiusSession.saveOrUpdate(radcheck);
							radiusSession.getTransaction().commit();
						}
						radcheck = null;
					}
				}
			}

			mahasiswas = null;

			
			HibernateUtil.closeSession();
			RadiusHibernateUtil.getInstance().closeSession();

		}
	}
}

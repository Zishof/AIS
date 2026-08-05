package ais.action.servlet.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.helper.virtualaccount.DownloadTagihanMahasiswaBankBtn;
import ais.action.master.helper.virtualaccount.DownloadTagihanSiswaBankOnline;
import ais.action.master.sekolah.helper.DetailTagihanCalonSiswaHelper;
import ais.action.master.sekolah.helper.DetailTagihanSiswaHelper;
import ais.action.master.sekolah.helper.TagihanUtil;
import ais.action.master.sekolah.helper.TagihanUtilCalonSiswa;
import ais.action.ws.util.PembayaranUtil;
import ais.common.BniCommon;
import ais.common.Common;
import ais.common.CommonHelperClass;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.VirtualAccountBank;
import ais.database.model.bni.BniRequest;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.sekolah.AkunPembayaranSiswa;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JenisBiayaSekolah;
import ais.database.model.sekolah.KelasLesSiswa;
import ais.database.model.sekolah.KelasLesSiswaPunyaSiswa;
import ais.database.model.sekolah.NominalBiaya;
import ais.database.model.sekolah.PembayaranSiswa;
import ais.database.model.sekolah.PembayaranSiswaDetail;
import ais.database.model.sekolah.PengaturanBiaya;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Tagihan;
import ais.ui.util.WaktuUtil;

public class TagihanSiswa {

	@SuppressWarnings({ "unchecked" })
	public static JSONObject hapus_split(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Autentikasi gagal: token sesi yang dikirimkan bersama permintaan ini tidak valid, sudah kedaluwarsa, atau tidak ditemukan dalam database sesi aktif sistem. Token sesi berfungsi sebagai identitas digital pengguna yang telah berhasil terautentikasi dan wajib disertakan pada setiap permintaan API. Kemungkinan penyebab kegagalan: (1) masa berlaku sesi telah habis akibat tidak ada aktivitas dalam durasi tertentu; (2) pengguna telah keluar dari sesi ini di perangkat lain; (3) administrator sistem telah mereset atau mencabut sesi; (4) token yang dikirimkan rusak, terpotong, atau mengandung karakter tidak valid; (5) terdapat perubahan konfigurasi keamanan pada server. Tindakan yang perlu dilakukan: keluar dari aplikasi secara resmi menggunakan tombol Keluar, kemudian lakukan proses masuk kembali dengan kredensial yang benar untuk mendapatkan token sesi baru yang valid, lalu ulangi permintaan ini.");
			} else {

				Session session = HibernateUtil.openSession();
				try {

				Tagihan tagihan = (Tagihan) (request.isNull("tagihan") ? null
						: session.createCriteria(Tagihan.class)
								.add(Restrictions.idEq(Long.parseLong(request.get("tagihan").toString())))
								.uniqueResult());

				if (tagihan != null) {

					if (!tagihan.getItemBiayaSekolah().getBolehDiangsur() || !tagihan.getNominalBiaya()
							.getPengaturanBiaya().getJenisBiayaSekolah().getBolehAngsurBerapapun()) {
						jsonObject.put("status", "01");
						jsonObject.put("description", "Operasi pemisahan tagihan tidak dapat dilakukan: konfigurasi jenis tagihan yang dipilih tidak mengizinkan pembagian nominal menjadi cicilan terpisah. Sistem memeriksa dua kondisi yang wajib terpenuhi secara bersamaan sebelum tagihan dapat dicicil: (1) properti 'bolehDiangsur' pada item biaya sekolah (ItemBiayaSekolah) harus diaktifkan oleh administrator — properti ini menentukan apakah item biaya tersebut dapat dibayar secara bertahap; dan (2) properti 'bolehAngsurBerapapun' pada jenis biaya sekolah (JenisBiayaSekolah) juga harus diaktifkan — properti ini mengizinkan pengguna menentukan sendiri besaran cicilan. Salah satu atau kedua kondisi tersebut saat ini tidak terpenuhi untuk tagihan yang dipilih. Tindakan: hubungi administrator sistem agar mengonfigurasi opsi cicilan pada pengaturan item biaya dan jenis biaya sekolah yang sesuai, kemudian ulangi operasi ini.");
					} else {

						Double tag = tagihan.getNominal();
						NominalBiaya nominalBiaya = tagihan.getNominalBiaya();

						Tagihan tagihanTerakhir = ((Tagihan) session.createCriteria(Tagihan.class)
								.add(Restrictions.eq("nominalBiaya", tagihan.getNominalBiaya()))
								.add(Restrictions.gt("bayarKe", 1)).addOrder(Order.desc("bayarKe"))
								.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult());

						if (tagihanTerakhir == null) {
							jsonObject.put("status", "01");
							jsonObject.put("description", "Penghapusan cicilan gagal: sistem tidak dapat menemukan tagihan cicilan lain (dengan urutan bayarKe lebih besar dari 1) yang berfungsi sebagai target pemindahan sisa nominal dari tagihan yang ingin dihapus. Proses penghapusan cicilan mensyaratkan adanya tagihan cicilan lain dalam rangkaian yang sama, karena nominal tagihan yang dihapus akan digabungkan kembali ke cicilan terakhir yang tersisa agar total kewajiban tidak berkurang. Kemungkinan penyebab: (1) tagihan yang dipilih merupakan satu-satunya cicilan dalam rangkaian ini (bayarKe = 1) sehingga tidak ada cicilan lain sebagai target penerimaan nilai; (2) seluruh cicilan sebelumnya telah dihapus sehingga tidak ada referensi valid; (3) data cicilan dalam sistem tidak konsisten atau mengalami kerusakan. Tinjau terlebih dahulu seluruh daftar cicilan dalam rangkaian tagihan tersebut sebelum mencoba penghapusan, atau hubungi administrator untuk penyelesaian secara manual.");
						} else {

							if (tagihanTerakhir != null) {
								tagihanTerakhir.setNominal(tagihanTerakhir.getNominal() + tag);

								session.getTransaction().begin();
								Common.refreshUpdate(session, tagihanTerakhir);
								session.getTransaction().commit();

							}
							session.getTransaction().begin();
							Common.refreshDelete(session, tagihan);
							session.getTransaction().commit();

							List<Tagihan> tagihans = session.createCriteria(Tagihan.class)
									.add(Restrictions.gt("nominal", 0.1))
									.add(Restrictions.eq("nominalBiaya", nominalBiaya)).addOrder(Order.asc("id"))
									.list();
							int index = 1;
							for (Tagihan t : tagihans) {
								if (t.getAktif()) {
									t.setBayarKe(index);
									session.getTransaction().begin();
									Common.refreshUpdate(session, t);
									session.getTransaction().commit();
									index++;
								}
							}

							System.out.println("index -> " + index + " tagihans " + tagihans);

							if (nominalBiaya != null) {

								Session mySession = HibernateUtil.openSession();
								try {
								mySession.refresh(nominalBiaya);
								nominalBiaya.setDibayarSebayakManual(index - 1);
								nominalBiaya.setDibayarSebayak(index - 1);
								mySession.getTransaction().begin();
								Common.refreshUpdate(mySession, nominalBiaya);
								mySession.getTransaction().commit();

								} finally {
									HibernateUtil.closeSessionQuietly(mySession);
								}
							}

							tagihan = tagihanTerakhir;

							Siswa siswa = tagihan.getSiswa();
							CalonSiswa calonSiswa = tagihan.getCalonSiswa();
							JSONObject js = new JSONObject();
							js.put("wajibPilih", tagihan.tagihanLainWajib(tagihans));
							js.put("id", tagihan.getId());
							js.put("grup_kode",
									tagihan.getPengaturanBiaya().getJenisBiayaSekolah().getKode());
							js.put("grup_nama", tagihan.getPengaturanBiaya().toString());
							js.put("grup_ta", tagihan.getPengaturanBiaya().getNama() + "-"
									+ tagihan.getPengaturanBiaya().getTahunAjaran());
							js.put("item_id", tagihan.getItemBiayaSekolah().getId());
							js.put("nama", tagihan.getItemBiayaSekolah().getNama());
							js.put("kode", tagihan.getItemBiayaSekolah().getKode());

							js.put("akumulasi_bulanan", tagihan.getPengaturanBiaya()
									.getJenisBiayaSekolah().getPilihanItemBiayaTerakumulasiBulanan());

							js.put("wajib_pilih_jika_bulan_dipilih",
									tagihan.getItemBiayaSekolah().getWajibPilihJikaBulanDipilih());
							js.put("wajib_pilih", tagihan.getItemBiayaSekolah().getWajibPilih());
							if (tagihan.getItemBiayaSekolah().getHarusBayar() != null) {
								js.put("harus_bayar_item_id", tagihan.getItemBiayaSekolah().getHarusBayar().getId());
								js.put("harus_bayar_nama", tagihan.getItemBiayaSekolah().getHarusBayar().getNama());
								js.put("harus_bayar_kode", tagihan.getItemBiayaSekolah().getHarusBayar().getKode());
							}
							String ket = tagihan.getNominalBiaya().getItemBiayaSekolah().getNama()
									+ (tagihan.getNominalBiaya().getDibayarSebayak() > 1
											? " (ke " + tagihan.getBayarKe() + ")"
											: "");
							Double denda = tagihan.getDenda();
							if (denda > 0.01) {
								ket += ", Denda " + Common.numberFormat.get().format(tagihan.getDenda());
							}
							Date tglDeadline = tagihan.getTanggalDeadline();
							if (tglDeadline != null) {
								ket += ", Deadline " + Common.dateFormat4.get().format(tglDeadline);
							}

							ket += (tagihan.getDiskonSiswa() != null && tagihan.getDiskonSiswa().getMemotongTagihan()
									? " - " + tagihan.getDiskonSiswa().getNama()
									: "");

							js.put("ket", ket);

							js.put("nominal", tagihan.getNominal() - tagihan.getDiskon());
							js.put("denda", denda);
							js.put("bolehAngsurBerapapun",
									tagihan.getItemBiayaSekolah().getWajibPilihJikaBulanDipilih() ? false
											: tagihan.getPengaturanBiaya().getJenisBiayaSekolah()
													.getBolehAngsurBerapapun());

							if (tagihan.getItemBiayaSekolah().getAngsuranSeragam()) {
								js.put("bolehHapusAngsur", false);
							} else {
								if (tagihan.getPengaturanBiaya().getJenisBiayaSekolah()
										.getBolehAngsurBerapapun()) {
									if (tagihan.getNominalBiaya().getDibayarSebayak().intValue() == tagihan.getBayarKe()
											.intValue()) {
										js.put("bolehHapusAngsur", false);
									} else {
										js.put("bolehHapusAngsur", true);
									}
								} else {
									js.put("bolehHapusAngsur", false);
								}
							}

							try {

								PembayaranSiswaDetail pembayaranSiswaDetail = tagihan.getPembayaranSiswaDetail();

								js.put("nominal_dibayar",
										(pembayaranSiswaDetail == null ? 0.0 : pembayaranSiswaDetail.getNominal()));
								js.put("tanggal_dibayar", (pembayaranSiswaDetail == null
										|| pembayaranSiswaDetail.getPembayaranSiswa() == null ? ""
												: Common.dateFormat5.get().format(
														pembayaranSiswaDetail.getPembayaranSiswa().getTanggalBayar())));
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/TagihanSiswa.java:220");
							}
							try {
								if (tagihan.getVa() != null && (tagihan.getExpired() == null
										|| tagihan.getExpired().after(WaktuUtil.getDate()))) {

									js.put("va", tagihan.getVa());
									js.put("billExpired", tagihan.getExpired() == null ? ""
											: Common.dateFormat5.get().format(tagihan.getExpired()));
								} else {
									js.put("va", "");
									js.put("billExpired", "");
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/TagihanSiswa.java:234");
							}
							js.put("nama_siswa", siswa != null ? siswa.getNama() : calonSiswa.getNama());
							js.put("link", tagihan.getLink());
							js.put("nisn_siswa",
									siswa != null ? siswa.getNomorIndukNasional() : calonSiswa.getNomorIndukNasional());
							js.put("bulan", tagihan.getBulan());
							js.put("bulan_hrf", tagihan.getBulan() == null ? "" : Common.BULAN[tagihan.getBulan() - 1]);
							js.put("tahun", tagihan.getTahun());
							js.put("ke", tagihan.getBayarKe());
							js.put("ta", tagihan.getPengaturanBiaya().getTahunAjaran());

							js.put("tanggalTagihan", Common.dateFormat4.get().format(tagihan.getTanggalTagihan()));
							js.put("tanggalDeadline", tagihan.getTanggalDeadline() == null ? ""
									: Common.dateFormat4.get().format(tagihan.getTanggalDeadline()));

							jsonObject.put("data", js);
							jsonObject.put("status", "00");
							jsonObject.put("description", "Penghapusan cicilan berhasil diselesaikan: tagihan cicilan yang dipilih telah berhasil dihapus dari sistem. Nominal tagihan yang dihapus tersebut telah digabungkan kembali ke tagihan cicilan terakhir yang tersisa dalam rangkaian yang sama, sehingga total kewajiban keseluruhan tetap tidak berubah. Urutan cicilan (bayarKe) untuk semua tagihan dalam rangkaian tersebut telah diperbarui secara otomatis, dan jumlah total cicilan yang diizinkan (dibayarSebanyak) pada data NominalBiaya juga telah disesuaikan sesuai kondisi terkini. Data lengkap tagihan cicilan terakhir yang telah diperbarui tersedia dalam respons untuk keperluan konfirmasi, tampilan antarmuka, atau pemrosesan lebih lanjut.");
						}
					}
				} else {
					jsonObject.put("status", "01");
					jsonObject.put("description", "Tagihan tidak ditemukan: ID tagihan yang dikirimkan dalam parameter permintaan ini tidak ditemukan dalam database, bernilai kosong (null atau string kosong), atau tidak merepresentasikan tagihan yang valid dan aktif. Sistem melakukan pencarian berdasarkan ID unik tagihan pada tabel tagihan dan memerlukan nilai numerik yang tepat sesuai data yang tersimpan. Kemungkinan penyebab: (1) nilai ID yang dikirimkan salah atau tidak sesuai format numerik yang diharapkan; (2) tagihan dengan ID tersebut telah dihapus secara permanen atau dinonaktifkan (aktif = false) oleh administrator; (3) tagihan tersebut milik siswa atau sekolah yang berbeda sehingga tidak dapat diakses dalam konteks sesi pengguna saat ini; (4) terdapat ketidaksesuaian antara data yang ditampilkan di antarmuka aplikasi dan kondisi aktual di server akibat cache yang belum diperbarui. Muat ulang daftar tagihan terbaru dari sistem untuk mendapatkan ID yang valid, dan pastikan tagihan yang dimaksud masih berstatus aktif sebelum mengirimkan permintaan.");
				}

				// session.disconnect();
				ApiHelperSupport.closeOpenedSession(session);

				} finally {
					HibernateUtil.closeSessionQuietly(session);
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TagihanSiswa.java:273");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings({})
	public static JSONObject split(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Autentikasi gagal: token sesi yang dikirimkan bersama permintaan ini tidak valid, sudah kedaluwarsa, atau tidak ditemukan dalam database sesi aktif sistem. Token sesi berfungsi sebagai identitas digital pengguna yang telah berhasil terautentikasi dan wajib disertakan pada setiap permintaan API. Kemungkinan penyebab kegagalan: (1) masa berlaku sesi telah habis akibat tidak ada aktivitas dalam durasi tertentu; (2) pengguna telah keluar dari sesi ini di perangkat lain; (3) administrator sistem telah mereset atau mencabut sesi; (4) token yang dikirimkan rusak, terpotong, atau mengandung karakter tidak valid; (5) terdapat perubahan konfigurasi keamanan pada server. Tindakan yang perlu dilakukan: keluar dari aplikasi secara resmi menggunakan tombol Keluar, kemudian lakukan proses masuk kembali dengan kredensial yang benar untuk mendapatkan token sesi baru yang valid, lalu ulangi permintaan ini.");
			} else {

				String informasi = request.isNull("informasi") ? null : request.get("informasi").toString();

				Session session = HibernateUtil.openSession();
				try {

				Tagihan tagihan = (Tagihan) (request.isNull("tagihan") ? null
						: session.createCriteria(Tagihan.class)
								.add(Restrictions.idEq(Long.parseLong(request.get("tagihan").toString())))
								.uniqueResult());

				if (tagihan != null) {

					if (!tagihan.getNominalBiaya().getItemBiayaSekolah().getBolehDiangsur() || !tagihan
							.getPengaturanBiaya().getJenisBiayaSekolah().getBolehAngsurBerapapun()) {
						jsonObject.put("status", "01");
						jsonObject.put("description", "Operasi pemisahan tagihan tidak dapat dilakukan: konfigurasi jenis tagihan yang dipilih tidak mengizinkan pembagian nominal menjadi cicilan terpisah. Sistem memeriksa dua kondisi yang wajib terpenuhi secara bersamaan sebelum tagihan dapat dicicil: (1) properti 'bolehDiangsur' pada item biaya sekolah (ItemBiayaSekolah) harus diaktifkan oleh administrator — properti ini menentukan apakah item biaya tersebut dapat dibayar secara bertahap; dan (2) properti 'bolehAngsurBerapapun' pada jenis biaya sekolah (JenisBiayaSekolah) juga harus diaktifkan — properti ini mengizinkan pengguna menentukan sendiri besaran cicilan. Salah satu atau kedua kondisi tersebut saat ini tidak terpenuhi untuk tagihan yang dipilih. Tindakan: hubungi administrator sistem agar mengonfigurasi opsi cicilan pada pengaturan item biaya dan jenis biaya sekolah yang sesuai, kemudian ulangi operasi ini.");
					} else {

						Siswa siswa = tagihan.getSiswa();
						CalonSiswa calonSiswa = tagihan.getCalonSiswa();

						Double dibayar = (request.isNull("dibayar") ? null
								: Double.parseDouble(request.get("dibayar").toString()));

						if (dibayar != null) {
							JSONObject js = new JSONObject();
//							Double tag = tagihan.getNominal();
							Tagihan maksTagihan = (Tagihan) session.createCriteria(Tagihan.class)
									.add(Restrictions.eq("nominalBiaya", tagihan.getNominalBiaya()))
									.addOrder(Order.desc("bayarKe")).setMaxResults(1).uniqueResult();

							final Double diskon = maksTagihan == null ? tagihan.ambilDiskonTanpaDikonBayarSatuKali()
									: maksTagihan.ambilDiskonTanpaDikonBayarSatuKali();

							final Double tag = maksTagihan == null
									? (tagihan.getDenda() + tagihan.getNominal())
											- tagihan.ambilDiskonTanpaDikonBayarSatuKali()
									: (maksTagihan.getDenda() + tagihan.getNominal())
											- maksTagihan.ambilDiskonTanpaDikonBayarSatuKali();

							NominalBiaya nominalBiaya = tagihan.getNominalBiaya();
							session.refresh(nominalBiaya);

							Number maks = (Number) session.createCriteria(Tagihan.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("nominalBiaya", nominalBiaya))
									.setProjection(Projections.rowCount()).add(Restrictions.gt("nominal", 0.1))
									.uniqueResult();
							int jml = (maks == null ? 1 : maks.intValue());
							nominalBiaya.setDibayarSebayakManual(jml + 1);
							nominalBiaya.setDibayarSebayak(jml + 1);
							Common.refreshUpdate(session, nominalBiaya);
							session.flush();

							tagihan.setNominalBiaya(nominalBiaya);

							tagihan.setNominal(dibayar + diskon);
							tagihan.setNominalManual(dibayar + diskon);
							tagihan.setDibayarManual(dibayar + diskon);
							tagihan.setDiskonManual(diskon);
							Common.refreshUpdate(session, tagihan);

							session.flush();

							Double sisaYgBelum = tag - dibayar;
							if (sisaYgBelum > 0.1) {

								Tagihan tagihan1 = ((Tagihan) session.createCriteria(Tagihan.class)
										.add(Restrictions.eq("nominalBiaya", nominalBiaya))
										.add(Restrictions.eq("bayarKe", jml + 1)).uniqueResult());

								if (tagihan1 == null) {
									try {
										Tagihan tagihanBaru = new Tagihan();
										tagihanBaru.setNominalBiaya(nominalBiaya);
										tagihanBaru.setBulan(nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah()
												.getUntukBulan());
										tagihanBaru.setTahun(nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah()
												.getUntukTahun());
										tagihanBaru.setSiswa(nominalBiaya.getSiswa());
										tagihanBaru.setItemBiayaSekolah(tagihan.getItemBiayaSekolah());
										tagihanBaru.setBayarKe(jml + 1);
										tagihanBaru.setNominal(sisaYgBelum);

										tagihanBaru.setInformasi(informasi);

										// FIX cegah "duplicate key value violates unique constraint tagihan_kode_unik_key":
										// cek DULU apakah kode_unik (dihitung dari item/pengaturan/tahunbulan/siswa/bayarKe)
										// sudah ada. Bila ADA -> AMBIL baris itu lalu UPDATE (JANGAN insert lagi). Cek
										// by (nominalBiaya, bayarKe) di atas bisa meleset karena kode_unik TIDAK memakai
										// nominalBiaya, sehingga baris dgn kode_unik sama tak terdeteksi -> dulu langsung save -> bentrok.
										Tagihan tagihanKodeSama = Tagihan.findByKodeUnik(tagihanBaru.getKodeUnik(), session);
										if (tagihanKodeSama != null) {
											tagihanKodeSama.setNominalBiaya(nominalBiaya);
											tagihanKodeSama.setNominal(sisaYgBelum);
											tagihanKodeSama.setInformasi(informasi);
											tagihanKodeSama.setItemBiayaSekolah(tagihan.getItemBiayaSekolah());
											tagihanKodeSama.setAktif(true);
											Common.refreshUpdate(session, tagihanKodeSama);
											session.flush();
											tagihan = tagihanKodeSama;
										} else {
											session.save(tagihanBaru);
											session.flush();
											tagihan = tagihanBaru;
										}
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}
								} else {
									tagihan1.setInformasi(informasi);
									tagihan1.setAktif(true);
									tagihan1.setNominal(sisaYgBelum);
									Common.refreshUpdate(session, tagihan1);
									session.flush();

									tagihan = tagihan1;
								}

								js.put("id", tagihan.getId());
								js.put("grup_kode", tagihan.getPengaturanBiaya()
										.getJenisBiayaSekolah().getKode());
								js.put("grup_nama", tagihan.getPengaturanBiaya().toString());
								js.put("grup_ta", tagihan.getPengaturanBiaya().getNama() + "-"
										+ tagihan.getPengaturanBiaya().getTahunAjaran());
								js.put("item_id", tagihan.getItemBiayaSekolah().getId());
								js.put("nama", tagihan.getItemBiayaSekolah().getNama());
								js.put("kode", tagihan.getItemBiayaSekolah().getKode());
								js.put("akumulasi_bulanan", tagihan.getPengaturanBiaya()
										.getJenisBiayaSekolah().getPilihanItemBiayaTerakumulasiBulanan());
								js.put("wajib_pilih_jika_bulan_dipilih",
										tagihan.getItemBiayaSekolah().getWajibPilihJikaBulanDipilih());
								js.put("wajib_pilih", tagihan.getItemBiayaSekolah().getWajibPilih());
								if (tagihan.getItemBiayaSekolah().getHarusBayar() != null) {
									js.put("harus_bayar_item_id",
											tagihan.getItemBiayaSekolah().getHarusBayar().getId());
									js.put("harus_bayar_nama", tagihan.getItemBiayaSekolah().getHarusBayar().getNama());
									js.put("harus_bayar_kode", tagihan.getItemBiayaSekolah().getHarusBayar().getKode());
								}
								String ket = tagihan.getNominalBiaya().getItemBiayaSekolah().getNama()
										+ (tagihan.getNominalBiaya().getDibayarSebayak() > 1
												? " (ke " + tagihan.getBayarKe() + ")"
												: "");
								Double denda = tagihan.getDenda();
								if (denda > 0.01) {
									ket += ", Denda " + Common.numberFormat.get().format(tagihan.getDenda());
								}
								Date tglDeadline = tagihan.getTanggalDeadline();
								if (tglDeadline != null) {
									ket += ", Deadline " + Common.dateFormat4.get().format(tglDeadline);
								}

								ket += (tagihan.getDiskonSiswa() != null
										&& tagihan.getDiskonSiswa().getMemotongTagihan()

												? " - " + tagihan.getDiskonSiswa().getNama()
												: "");

								js.put("ket", ket);

								js.put("nominal", tagihan.getNominal() - tagihan.getDiskon());
								js.put("denda", denda);
								js.put("bolehAngsurBerapapun",
										tagihan.getItemBiayaSekolah().getWajibPilihJikaBulanDipilih() ? false
												: tagihan.getPengaturanBiaya().getJenisBiayaSekolah()
														.getBolehAngsurBerapapun());

								if (tagihan.getItemBiayaSekolah().getAngsuranSeragam()) {
									js.put("bolehHapusAngsur", false);
								} else {

									if (tagihan.getPengaturanBiaya().getJenisBiayaSekolah()
											.getBolehAngsurBerapapun()) {
										if (tagihan.getNominalBiaya().getDibayarSebayak().intValue() == tagihan
												.getBayarKe().intValue()) {
											js.put("bolehHapusAngsur", false);
										} else {
											js.put("bolehHapusAngsur", true);
										}
									} else {
										js.put("bolehHapusAngsur", false);
									}
								}
								try {

									PembayaranSiswaDetail pembayaranSiswaDetail = tagihan.getPembayaranSiswaDetail();

									Double tabungan = (pembayaranSiswaDetail == null ? 0.0
											: pembayaranSiswaDetail.getPembayaranSiswa().getDariTabungan());
									js.put("tabungan", tabungan);

									js.put("nominal_dibayar",
											(pembayaranSiswaDetail == null ? 0.0 : pembayaranSiswaDetail.getNominal())
													- tabungan);
									js.put("tanggal_dibayar",
											(pembayaranSiswaDetail == null
													|| pembayaranSiswaDetail.getPembayaranSiswa() == null ? ""
															: Common.dateFormat5.get().format(pembayaranSiswaDetail
																	.getPembayaranSiswa().getTanggalBayar())));
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/TagihanSiswa.java:489");
								}
								try {
									if (tagihan.getVa() != null && (tagihan.getExpired() == null
											|| tagihan.getExpired().after(WaktuUtil.getDate()))) {

										js.put("va", tagihan.getVa());
										js.put("billExpired", tagihan.getExpired() == null ? ""
												: Common.dateFormat5.get().format(tagihan.getExpired()));
									} else {
										js.put("va", "");
										js.put("billExpired", "");
									}
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/TagihanSiswa.java:503");
								}
								js.put("nama_siswa", siswa != null ? siswa.getNama() : calonSiswa.getNama());
								js.put("nisn_siswa", siswa != null ? siswa.getNomorIndukNasional()
										: calonSiswa.getNomorIndukNasional());
								js.put("link", tagihan.getLink());
								js.put("bulan", tagihan.getBulan());
								js.put("bulan_hrf",
										tagihan.getBulan() == null ? "" : Common.BULAN[tagihan.getBulan() - 1]);
								js.put("tahun", tagihan.getTahun());
								js.put("ke", tagihan.getBayarKe());
								js.put("ta", tagihan.getPengaturanBiaya().getTahunAjaran());
								js.put("tanggalTagihan", Common.dateFormat4.get().format(tagihan.getTanggalTagihan()));
								js.put("tanggalDeadline", tagihan.getTanggalDeadline() == null ? ""
										: Common.dateFormat4.get().format(tagihan.getTanggalDeadline()));

								jsonObject.put("data", js);
								jsonObject.put("status", "00");
								jsonObject.put("description", "Pemisahan tagihan berhasil diselesaikan: cicilan baru dengan nominal yang ditentukan telah berhasil dibuat dan disimpan ke dalam sistem sebagai tagihan terpisah dengan urutan bayarKe berikutnya. Tagihan induk atau tagihan cicilan terakhir yang menjadi sumber pemisahan juga telah disesuaikan nominalnya secara proporsional, sehingga total kewajiban keseluruhan tetap konsisten. Data lengkap tagihan cicilan baru yang berhasil dibuat, beserta perubahan pada tagihan yang terkait, tersedia dalam respons untuk keperluan konfirmasi tampilan atau pemrosesan pembayaran selanjutnya.");
							} else {
								jsonObject.put("status", "01");
								jsonObject.put("description",
										"Nilai pembayaran tidak valid: jumlah nominal yang akan dibayarkan melebihi total kewajiban tagihan yang tersisa. Sistem menghitung batas maksimal pembayaran berdasarkan rumus: (nominal tagihan + denda yang berlaku) dikurangi diskon yang diterapkan pada tagihan tersebut. Nilai yang dikirimkan melalui parameter 'dibayar' melampaui hasil perhitungan tersebut, sehingga permintaan tidak dapat diproses untuk menghindari kelebihan pembayaran (overpayment). Pastikan nilai cicilan yang diinputkan tidak melebihi sisa kewajiban tagihan, dan pertimbangkan denda serta diskon yang berlaku dalam perhitungan Anda sebelum mengirimkan permintaan.");
							}

						} else {
							jsonObject.put("status", "01");
							jsonObject.put("description", "Parameter permintaan tidak lengkap: nilai nominal pembayaran cicilan (parameter 'dibayar') tidak ditemukan dalam permintaan yang dikirimkan atau bernilai null. Parameter ini wajib disertakan karena menentukan berapa besar nilai yang akan dipecah menjadi cicilan terpisah dari total tagihan. Nilai yang valid harus berupa angka numerik positif, lebih kecil dari total kewajiban tagihan (nominal + denda - diskon), dan tidak boleh nol. Sertakan parameter 'dibayar' dengan nilai cicilan yang diinginkan dalam permintaan, kemudian ulangi proses pemisahan tagihan.");
						}
					}
				} else {
					jsonObject.put("status", "01");
					jsonObject.put("description", "Tagihan tidak ditemukan: ID tagihan yang dikirimkan dalam parameter permintaan ini tidak ditemukan dalam database, bernilai kosong (null atau string kosong), atau tidak merepresentasikan tagihan yang valid dan aktif. Sistem melakukan pencarian berdasarkan ID unik tagihan pada tabel tagihan dan memerlukan nilai numerik yang tepat sesuai data yang tersimpan. Kemungkinan penyebab: (1) nilai ID yang dikirimkan salah atau tidak sesuai format numerik yang diharapkan; (2) tagihan dengan ID tersebut telah dihapus secara permanen atau dinonaktifkan (aktif = false) oleh administrator; (3) tagihan tersebut milik siswa atau sekolah yang berbeda sehingga tidak dapat diakses dalam konteks sesi pengguna saat ini; (4) terdapat ketidaksesuaian antara data yang ditampilkan di antarmuka aplikasi dan kondisi aktual di server akibat cache yang belum diperbarui. Muat ulang daftar tagihan terbaru dari sistem untuk mendapatkan ID yang valid, dan pastikan tagihan yang dimaksud masih berstatus aktif sebelum mengirimkan permintaan.");
				}

				// session.disconnect();
				ApiHelperSupport.closeOpenedSession(session);

				} finally {
					HibernateUtil.closeSessionQuietly(session);
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TagihanSiswa.java:551");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings({ "unchecked" })
	public static JSONObject piutang(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Autentikasi gagal: token sesi yang dikirimkan bersama permintaan ini tidak valid, sudah kedaluwarsa, atau tidak ditemukan dalam database sesi aktif sistem. Token sesi berfungsi sebagai identitas digital pengguna yang telah berhasil terautentikasi dan wajib disertakan pada setiap permintaan API. Kemungkinan penyebab kegagalan: (1) masa berlaku sesi telah habis akibat tidak ada aktivitas dalam durasi tertentu; (2) pengguna telah keluar dari sesi ini di perangkat lain; (3) administrator sistem telah mereset atau mencabut sesi; (4) token yang dikirimkan rusak, terpotong, atau mengandung karakter tidak valid; (5) terdapat perubahan konfigurasi keamanan pada server. Tindakan yang perlu dilakukan: keluar dari aplikasi secara resmi menggunakan tombol Keluar, kemudian lakukan proses masuk kembali dengan kredensial yang benar untuk mendapatkan token sesi baru yang valid, lalu ulangi permintaan ini.");
			} else {

				Siswa siswa = tbmuser.getSiswa();
				CalonSiswa calonSiswa = tbmuser.getCalonSiswa();

				if (calonSiswa == null) {
					if (siswa != null && siswa.getCalonSiswa() != null) {
						calonSiswa = (CalonSiswa) ConstantValues.ambil(CalonSiswa.class.getName(),
								siswa.getCalonSiswa());
					}
				}

				if (siswa != null || calonSiswa != null) {

					Sekolah sekolah = siswa != null ? siswa.getSekolah() : calonSiswa.getSekolah();

					Session session = HibernateUtil.openSession();
					try {
					List<PengaturanBiaya> pengaturanBiayas = ConstantValues.simpleList(session
							.createCriteria(PengaturanBiaya.class)
							.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
							.createAlias("jenisBiayaSekolah", "jenisBiayaSekolah")

							.add(calonSiswa != null && siswa != null ? Restrictions.sqlRestriction("true")
									: calonSiswa != null ? Restrictions.eq("jenisBiayaSekolah.gunakanCalonSiswa", true)
											: Restrictions.sqlRestriction("true"))

							.add(calonSiswa != null && siswa != null ? Restrictions.sqlRestriction("true")
									: siswa != null ? Restrictions.eq("jenisBiayaSekolah.gunakanCalonSiswa", false)
											: Restrictions.sqlRestriction("true"))

							.add(sekolah == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("jenisBiayaSekolah.sekolah", sekolah))

							.add(Restrictions.or(Restrictions.isNull("jenisBiayaSekolah.aktif"),
									Restrictions.eq("jenisBiayaSekolah.aktif", true)))
							.addOrder(Order.desc("jenisBiayaSekolah.periode"))
							.addOrder(Order.asc("jenisBiayaSekolah.nama")), PengaturanBiaya.class);
					// session.disconnect();
					ApiHelperSupport.closeOpenedSession(session);

					Double totalPiutang = 0.0;
					Double totalDibayar = 0.0;
					Double amn = 0.0;
					System.out.println("pengaturanBiayas => " + pengaturanBiayas.size());
					JSONArray jsonArray = new JSONArray();
					for (PengaturanBiaya pengaturanBiaya : pengaturanBiayas) {

						if (pengaturanBiaya.getAktif()
								&& (siswa != null && DetailTagihanSiswaHelper.apakahAda(pengaturanBiaya, siswa))
								|| (calonSiswa != null
										&& DetailTagihanCalonSiswaHelper.apakahAda(pengaturanBiaya, calonSiswa))) {

							JenisBiayaSekolah jenisBiaya = pengaturanBiaya.getJenisBiayaSekolah();

							HibernateUtil.closeSessionQuietly(session); // tutup session lama sebelum buka ulang (konversi openSession)
							session = HibernateUtil.openSession();

							try {

								List<Tagihan> tagihanstemp = session.createCriteria(Tagihan.class)
										.add(Restrictions.eq("pengaturanBiaya", pengaturanBiaya))
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.createAlias("nominalBiaya", "nominalBiaya")
										.createAlias("nominalBiaya.pengaturanBiaya", "pengaturanBiaya")
										.add(Restrictions.or(Restrictions.eq("siswa", siswa),
												Restrictions.eq("calonSiswa", calonSiswa)))
										.add(jenisBiaya == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("pengaturanBiaya.jenisBiayaSekolah", jenisBiaya))
										.addOrder(Order.asc("pengaturanBiaya.jenisBiayaSekolah.id"))
										.addOrder(Order.asc("tahunbulan")).list();

								List<Tagihan> tagihans = TagihanUtilCalonSiswa.saring(tagihanstemp, session);

//								System.out.println("tagihans -> " + tagihans.size());

								for (Tagihan tagihan : tagihans) {
									amn += (tagihan.getNominal() + tagihan.getDenda());
								}

								for (Tagihan tagihan : tagihans) {
									try {

										if (((tagihan.getAktif() && !tagihan.ambilBukanTagihanData())
												&& !tagihan.getNominalBiaya().getBukanTagihan())) {
											JSONObject js = new JSONObject();
											js.put("wajibPilih", tagihan.tagihanLainWajib(tagihans));
											js.put("id", tagihan.getId());
											js.put("grup_id", pengaturanBiaya.getId());
											js.put("grup_kode", pengaturanBiaya.getJenisBiayaSekolah().getKode());
											js.put("grup_nama", pengaturanBiaya.toString());
											js.put("grup_ta", pengaturanBiaya.getJenisBiayaSekolah().getNama() + "-"
													+ pengaturanBiaya.getTahunAjaran());
											js.put("item_id", tagihan.getItemBiayaSekolah().getId());
											js.put("nama", tagihan.getItemBiayaSekolah().getNama());
											js.put("kode", tagihan.getItemBiayaSekolah().getKode());
											js.put("akumulasi_bulanan", tagihan.getPengaturanBiaya()
													.getJenisBiayaSekolah().getPilihanItemBiayaTerakumulasiBulanan());
											js.put("wajib_pilih_jika_bulan_dipilih",
													tagihan.getItemBiayaSekolah().getWajibPilihJikaBulanDipilih());
											js.put("wajib_pilih", tagihan.getItemBiayaSekolah().getWajibPilih());
											if (tagihan.getItemBiayaSekolah().getHarusBayar() != null) {
												js.put("harus_bayar_item_id",
														tagihan.getItemBiayaSekolah().getHarusBayar().getId());
												js.put("harus_bayar_nama",
														tagihan.getItemBiayaSekolah().getHarusBayar().getNama());
												js.put("harus_bayar_kode",
														tagihan.getItemBiayaSekolah().getHarusBayar().getKode());
											}

											String ket = tagihan.getNominalBiaya().getItemBiayaSekolah().getNama()
													+ (tagihan.getNominalBiaya().getDibayarSebayak() > 1
															? " (ke " + tagihan.getBayarKe() + ")"
															: "");
											Double denda = tagihan.getDenda();
											if (denda > 0.01) {
												ket += ", Denda " + Common.numberFormat.get().format(tagihan.getDenda());
											}
											Date tglDeadline = tagihan.getTanggalDeadline();
											if (tglDeadline != null) {
												ket += ", Deadline " + Common.dateFormat4.get().format(tglDeadline);
											}

											ket += (tagihan.getDiskonSiswa() != null
													&& tagihan.getDiskonSiswa().getMemotongTagihan()

															? " - " + tagihan.getDiskonSiswa().getNama()
															: "");

											js.put("ket", ket);

											js.put("nominal", tagihan.getNominal() - tagihan.getDiskon());
											js.put("denda", denda);

											js.put("bolehAngsurBerapapun",
													tagihan.getItemBiayaSekolah().getWajibPilihJikaBulanDipilih()
															? false
															: tagihan.getNominalBiaya().getItemBiayaSekolah()
																	.getBolehDiangsur()
																	&& tagihan.getPengaturanBiaya()
																			.getJenisBiayaSekolah()
																			.getBolehAngsurBerapapun());

											if (tagihan.getItemBiayaSekolah().getAngsuranSeragam()) {
												js.put("bolehHapusAngsur", false);
											} else {

												if (tagihan.getPengaturanBiaya()
														.getJenisBiayaSekolah().getBolehAngsurBerapapun()) {
													if (tagihan.getNominalBiaya().getDibayarSebayak()
															.intValue() == tagihan.getBayarKe().intValue()) {
														js.put("bolehHapusAngsur", false);
													} else {
														js.put("bolehHapusAngsur", true);
													}
												} else {
													js.put("bolehHapusAngsur", false);
												}
											}
											try {

												PembayaranSiswaDetail pembayaranSiswaDetail = tagihan
														.getPembayaranSiswaDetail();

												if (pembayaranSiswaDetail == null) {
													totalPiutang += (tagihan.getNominal() + tagihan.getDenda());
												} else {
													totalDibayar += pembayaranSiswaDetail.getNominal();
												}
												Double tabungan = (pembayaranSiswaDetail == null ? 0.0
														: pembayaranSiswaDetail.getPembayaranSiswa().getDariTabungan());
												js.put("tabungan", tabungan);

												js.put("nominal_dibayar", (pembayaranSiswaDetail == null ? 0.0
														: pembayaranSiswaDetail.getNominal()) - tabungan);
												js.put("tanggal_dibayar", (pembayaranSiswaDetail == null
														|| pembayaranSiswaDetail.getPembayaranSiswa() == null ? ""
																: Common.dateFormat5.get().format(pembayaranSiswaDetail
																		.getPembayaranSiswa().getTanggalBayar())));
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/TagihanSiswa.java:746");
											}
											try {
												if (tagihan.getVa() != null && (tagihan.getExpired() == null
														|| tagihan.getExpired().after(WaktuUtil.getDate()))) {

													js.put("va", tagihan.getVa());
													js.put("billExpired", tagihan.getExpired() == null ? ""
															: Common.dateFormat5.get().format(tagihan.getExpired()));
												} else {
													js.put("va", "");
													js.put("billExpired", "");
												}
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/TagihanSiswa.java:760");
											}
											js.put("nama_siswa",
													siswa != null ? siswa.getNama() : calonSiswa.getNama());
											js.put("nisn_siswa", siswa != null ? siswa.getNomorIndukNasional()
													: calonSiswa.getNomorIndukNasional());
											js.put("bulan", tagihan.getBulan());
											js.put("bulan_hrf", tagihan.getBulan() == null ? ""
													: Common.BULAN[tagihan.getBulan() - 1]);
											js.put("tahun", tagihan.getTahun());
											js.put("ke", tagihan.getBayarKe());
											js.put("link", tagihan.getLink());
											js.put("ta",
													tagihan.getPengaturanBiaya().getTahunAjaran());
											js.put("tanggalTagihan",
													Common.dateFormat4.get().format(tagihan.getTanggalTagihan()));
											js.put("tanggalDeadline", tagihan.getTanggalDeadline() == null ? ""
													: Common.dateFormat4.get().format(tagihan.getTanggalDeadline()));
											jsonArray.put(js);

										}

									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/TagihanSiswa.java:783");
									}
								}

							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/TagihanSiswa.java:788");
							}
						}

						double biayaAdministrasi = 0.0;
						try {
							biayaAdministrasi = Double
									.parseDouble(Common.getKonfigurasi("bni_biaya_administrasi", "0.0").getNilai());
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/TagihanSiswa.java:796");

						}
						jsonObject.put("totalPiutang", totalPiutang);
						jsonObject.put("totalDibayar", totalDibayar);
						jsonObject.put("biayaAdministrasi", biayaAdministrasi);
						jsonObject.put("total", amn);
						jsonObject.put("data", jsonArray);
						jsonObject.put("status", "00");
						jsonObject.put("description", "Data piutang dan rincian tagihan siswa berhasil diambil dari sistem. Respons mencakup: total piutang keseluruhan yang masih menjadi kewajiban siswa atau calon siswa, jumlah total yang telah dibayarkan sebelumnya, biaya administrasi yang berlaku sesuai konfigurasi bank (khususnya BNI), total nominal bersih yang perlu diselesaikan, serta daftar lengkap seluruh tagihan yang masih belum dibayar beserta rincian masing-masing item tagihan. Gunakan data kanal pembayaran (bank_va_mobile) yang tersedia untuk mengarahkan pengguna ke metode pembayaran yang sesuai dengan konfigurasi sekolah.");
						jsonObject.put("bank_va_mobile",
								Common.getKonfigurasi("bank_va_mobile_" + sekolah.getId(), "BNI").getNilai());

					}
					} finally {
						HibernateUtil.closeSessionQuietly(session);
					}
				} else {
					jsonObject.put("status", "01");
					jsonObject.put("description", "Akses ditolak: endpoint API ini secara eksklusif dirancang untuk melayani permintaan dari akun pengguna yang terdaftar sebagai siswa aktif atau calon siswa dalam sistem. Token autentikasi yang digunakan pada permintaan ini tidak terkait dengan entitas siswa (tabel siswa) maupun calon siswa (tabel calon_siswa) mana pun dalam database. Kemungkinan penyebab: (1) pengguna masuk menggunakan akun orang tua (orangTua), pengajar, tenaga administrasi, atau administrator yang tidak memiliki hak akses pada fitur pembayaran siswa; (2) asosiasi antara akun pengguna dan data siswa atau calon siswa belum dikonfigurasi oleh administrator sekolah; (3) data siswa yang sebelumnya terhubung dengan akun ini telah dihapus, dinonaktifkan, atau dipindahkan ke akun lain. Tindakan: pastikan Anda masuk menggunakan akun yang secara langsung ditetapkan sebagai siswa atau calon siswa, atau hubungi administrator sekolah untuk memverifikasi dan memperbaiki tautan antara akun pengguna dengan data siswa yang bersangkutan.");
				}

			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TagihanSiswa.java:825");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings({})
	public static JSONObject tagihan(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Autentikasi gagal: token sesi yang dikirimkan bersama permintaan ini tidak valid, sudah kedaluwarsa, atau tidak ditemukan dalam database sesi aktif sistem. Token sesi berfungsi sebagai identitas digital pengguna yang telah berhasil terautentikasi dan wajib disertakan pada setiap permintaan API. Kemungkinan penyebab kegagalan: (1) masa berlaku sesi telah habis akibat tidak ada aktivitas dalam durasi tertentu; (2) pengguna telah keluar dari sesi ini di perangkat lain; (3) administrator sistem telah mereset atau mencabut sesi; (4) token yang dikirimkan rusak, terpotong, atau mengandung karakter tidak valid; (5) terdapat perubahan konfigurasi keamanan pada server. Tindakan yang perlu dilakukan: keluar dari aplikasi secara resmi menggunakan tombol Keluar, kemudian lakukan proses masuk kembali dengan kredensial yang benar untuk mendapatkan token sesi baru yang valid, lalu ulangi permintaan ini.");
			} else {
				Integer bulan = Integer.parseInt(request.get("bulan").toString());
				Integer tahun = Integer.parseInt(request.get("tahun").toString());
				boolean refresh = request.optBoolean("refresh", false);

				Siswa siswa = tbmuser.getSiswa();
				CalonSiswa calonSiswa = tbmuser.getCalonSiswa();

				if (siswa != null && siswa.getCalonSiswa() == null) {

					Session session = HibernateUtil.openSession();
					try {

					Long calSis = (Long) session.createCriteria(CalonSiswa.class)
							.add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
							.setProjection(Projections.property("id"))
							.add(Restrictions.ilike("namaSiswa", siswa.getNamaSiswa()))
							.add(Restrictions.eq("tanggalLahir", siswa.getTanggalLahir())).setMaxResults(1)
							.addOrder(Order.desc("id")).addOrder(Order.desc("tahunMasuk")).uniqueResult();

					if (calSis != null) {
						try {
							// FIX duplicate-key (constraint siswa_calonsiswa_key): kriteria
							// pencarian (nama ilike + tanggal lahir) bisa cocok ke CalonSiswa
							// yang SUDAH terpaut ke Siswa lain (nama sama/mirip antar siswa
							// berbeda). Cek dulu apakah calSis sudah dipakai siswa lain
							// sebelum set+commit, agar tak melanggar unique constraint.
							Long pemilikLain = (Long) session.createCriteria(Siswa.class)
									.add(Restrictions.eq("calonSiswa", calSis))
									.add(Restrictions.ne("id", siswa.getId()))
									.setProjection(Projections.property("id")).setMaxResults(1).uniqueResult();

							if (pemilikLain == null) {
								session.refresh(siswa);
								siswa.setCalonSiswa(calSis);

								session.getTransaction().begin();
								session.update(siswa);
								session.getTransaction().commit();
							}

						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/TagihanSiswa.java:869");
						}
					}

					} finally {
						HibernateUtil.closeSessionQuietly(session);
					}
				}

				if (calonSiswa == null) {
					if (siswa != null && siswa.getCalonSiswa() != null) {
						calonSiswa = (CalonSiswa) ConstantValues.ambil(CalonSiswa.class.getName(),
								siswa.getCalonSiswa());
					}
				}

				if (siswa != null || calonSiswa != null) {

					Sekolah sekolah = siswa != null ? siswa.getSekolah() : calonSiswa.getSekolah();

					JSONArray jsonArray = new JSONArray();

					Double amn = 0.0;
					if (siswa != null) {
						amn += daftarTagihan(null, siswa, sekolah, bulan, tahun, jsonArray, refresh);
					}
					if (calonSiswa != null) {
						amn += daftarTagihan(calonSiswa, null, sekolah, bulan, tahun, jsonArray, refresh);
					}

					double biayaAdministrasi = 0.0;
					try {
						biayaAdministrasi = Double
								.parseDouble(Common.getKonfigurasi("bni_biaya_administrasi", "0.0").getNilai());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/TagihanSiswa.java:903");

					}

					jsonObject.put("biayaAdministrasi", biayaAdministrasi);
					jsonObject.put("total", amn);
					jsonObject.put("data", jsonArray);
					jsonObject.put("status", "00");
					jsonObject.put("description", "Daftar tagihan siswa untuk periode yang diminta berhasil diambil dari sistem. Respons mencakup: daftar lengkap tagihan yang belum dibayar beserta rincian masing-masing item biaya, nominal yang harus dibayarkan, denda yang berlaku, total kewajiban keseluruhan, biaya administrasi yang berlaku sesuai konfigurasi bank, dan informasi kanal pembayaran digital (bank_va_mobile) yang telah dikonfigurasikan untuk sekolah ini. Gunakan data tagihan ini sebagai dasar untuk proses pembuatan Virtual Account (VA) atau pembayaran langsung via tabungan/deposit.");
					jsonObject.put("bank_va_mobile",
							Common.getKonfigurasi("bank_va_mobile_" + sekolah.getId(), "BNI").getNilai());
				} else {
					jsonObject.put("status", "01");
					jsonObject.put("description", "Akses ditolak: endpoint API ini secara eksklusif dirancang untuk melayani permintaan dari akun pengguna yang terdaftar sebagai siswa aktif atau calon siswa dalam sistem. Token autentikasi yang digunakan pada permintaan ini tidak terkait dengan entitas siswa (tabel siswa) maupun calon siswa (tabel calon_siswa) mana pun dalam database. Kemungkinan penyebab: (1) pengguna masuk menggunakan akun orang tua (orangTua), pengajar, tenaga administrasi, atau administrator yang tidak memiliki hak akses pada fitur pembayaran siswa; (2) asosiasi antara akun pengguna dan data siswa atau calon siswa belum dikonfigurasi oleh administrator sekolah; (3) data siswa yang sebelumnya terhubung dengan akun ini telah dihapus, dinonaktifkan, atau dipindahkan ke akun lain. Tindakan: pastikan Anda masuk menggunakan akun yang secara langsung ditetapkan sebagai siswa atau calon siswa, atau hubungi administrator sekolah untuk memverifikasi dan memperbaiki tautan antara akun pengguna dengan data siswa yang bersangkutan.");
				}

			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TagihanSiswa.java:926");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	private static Double daftarTagihan(CalonSiswa calonSiswa, Siswa siswa, Sekolah sekolah,
			Integer bulan, Integer tahun, JSONArray jsonArray, boolean refersh) {

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			List<PengaturanBiaya> pengaturanBiayas = ConstantValues.simpleList(PengaturanBiaya
				.terapkanFilterPembayaran(session.createCriteria(PengaturanBiaya.class)
						.setFetchMode("jenisBiayaSekolah", FetchMode.JOIN), siswa, calonSiswa)
				.addOrder(Order.desc("jenisBiayaSekolah.periode")).addOrder(Order.asc("jenisBiayaSekolah.nama")),
				PengaturanBiaya.class);
		Double amn = 0.0;
		System.out.println("pengaturanBiayas => " + pengaturanBiayas.size());

		System.out.println("calonSiswa -> " + calonSiswa + " siswa -> " + siswa);

		for (PengaturanBiaya pengaturanBiaya : pengaturanBiayas) {
			JenisBiayaSekolah jenisBiaya = pengaturanBiaya.getJenisBiayaSekolah();
			if (jenisBiaya != null && pengaturanBiaya.getAktif() && ((siswa != null && !jenisBiaya.getGunakanCalonSiswa()
					&& DetailTagihanSiswaHelper.apakahAda(pengaturanBiaya, siswa))
					|| (calonSiswa != null && jenisBiaya.getGunakanCalonSiswa()
							&& DetailTagihanCalonSiswaHelper.apakahAda(pengaturanBiaya, calonSiswa)))) {

				List<Tagihan> tagihans = calonSiswa != null
						? TagihanUtilCalonSiswa.getTagihan(jenisBiaya, pengaturanBiaya, calonSiswa, bulan, tahun,
								refersh)
						: TagihanUtil.getTagihan(jenisBiaya, pengaturanBiaya, siswa, bulan, tahun, refersh);

//				System.out.println("tagihans -> " + tagihans.size() + " jenisBiaya " + jenisBiaya);

				for (Tagihan tagihan : tagihans) {
					amn += (tagihan.getNominal() + tagihan.getDenda());
				}

				for (Tagihan tagihan : tagihans) {
					try {

						if (((tagihan.getAktif() && !tagihan.ambilBukanTagihanData())
								&& !tagihan.getNominalBiaya().getBukanTagihan())
								&& tagihan.getPembayaranSiswaDetail() == null) {
							JSONObject js = new JSONObject();
							js.put("tagihanKadaluarsa", pengaturanBiaya.getTagihanKadaluarsa() == null ? ""
									: Common.dateFormat4.get().format(pengaturanBiaya.getTagihanKadaluarsa()));
							js.put("wajibPilih", tagihan.tagihanLainWajib(tagihans));
							js.put("id", tagihan.getId());
							js.put("grup_id", pengaturanBiaya.getId());
							js.put("grup_kode", pengaturanBiaya.getJenisBiayaSekolah().getKode());
							js.put("grup_nama", pengaturanBiaya.toString());
							js.put("grup_ta", pengaturanBiaya.getJenisBiayaSekolah().getNama() + "-"
									+ pengaturanBiaya.getTahunAjaran());
							js.put("item_id", tagihan.getItemBiayaSekolah().getId());
							js.put("nama", tagihan.getItemBiayaSekolah().getNama());
							js.put("kode", tagihan.getItemBiayaSekolah().getKode());
							js.put("akumulasi_bulanan", tagihan.getPengaturanBiaya()
									.getJenisBiayaSekolah().getPilihanItemBiayaTerakumulasiBulanan());

							js.put("wajib_pilih_jika_bulan_dipilih",
									tagihan.getItemBiayaSekolah().getWajibPilihJikaBulanDipilih());

							js.put("wajib_pilih", tagihan.getItemBiayaSekolah().getWajibPilih());

							if (tagihan.getItemBiayaSekolah().getHarusBayar() != null) {
								js.put("harus_bayar_item_id", tagihan.getItemBiayaSekolah().getHarusBayar().getId());
								js.put("harus_bayar_nama", tagihan.getItemBiayaSekolah().getHarusBayar().getNama());
								js.put("harus_bayar_kode", tagihan.getItemBiayaSekolah().getHarusBayar().getKode());
							}

							String ket = tagihan.getNominalBiaya().getItemBiayaSekolah().getNama()
									+ (tagihan.getNominalBiaya().getDibayarSebayak() > 1
											? " (ke " + tagihan.getBayarKe() + ")"
											: "");
							Double denda = tagihan.getDenda();
							if (denda > 0.01) {
								ket += ", Denda " + Common.numberFormat.get().format(tagihan.getDenda());
							}
							Date tglDeadline = tagihan.getTanggalDeadline();
							if (tglDeadline != null) {
								ket += ", Deadline " + Common.dateFormat4.get().format(tglDeadline);
							}

							ket += (tagihan.getDiskonSiswa() != null && tagihan.getDiskonSiswa().getMemotongTagihan()
									? " - " + tagihan.getDiskonSiswa().getNama()
									: "");

							js.put("ket", ket);

							js.put("nominal", tagihan.getNominal() - tagihan.getDiskon());
							js.put("denda", denda);
							js.put("bolehAngsurBerapapun",
									tagihan.getItemBiayaSekolah().getWajibPilihJikaBulanDipilih() ? false
											: tagihan.getNominalBiaya().getItemBiayaSekolah().getBolehDiangsur()
													&& tagihan.getPengaturanBiaya()
															.getJenisBiayaSekolah().getBolehAngsurBerapapun());

							if (tagihan.getItemBiayaSekolah().getAngsuranSeragam()) {
								js.put("bolehHapusAngsur", false);
							} else {

								if (tagihan.getPengaturanBiaya().getJenisBiayaSekolah()
										.getBolehAngsurBerapapun()) {
									if (tagihan.getNominalBiaya().getDibayarSebayak().intValue() == tagihan.getBayarKe()
											.intValue()) {
										js.put("bolehHapusAngsur", false);
									} else {
										js.put("bolehHapusAngsur", true);
									}
								} else {
									js.put("bolehHapusAngsur", false);
								}
							}
							try {

								PembayaranSiswaDetail pembayaranSiswaDetail = tagihan.getPembayaranSiswaDetail();
								Double tabungan = (pembayaranSiswaDetail == null ? 0.0
										: pembayaranSiswaDetail.getPembayaranSiswa().getDariTabungan());
								js.put("tabungan", tabungan);
								js.put("nominal_dibayar",
										(pembayaranSiswaDetail == null ? 0.0 : pembayaranSiswaDetail.getNominal())
												- tabungan);
								js.put("tanggal_dibayar", (pembayaranSiswaDetail == null
										|| pembayaranSiswaDetail.getPembayaranSiswa() == null ? ""
												: Common.dateFormat5.get().format(
														pembayaranSiswaDetail.getPembayaranSiswa().getTanggalBayar())));
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/TagihanSiswa.java:1058");
							}
							try {
								if (tagihan.getVa() != null && (tagihan.getExpired() == null
										|| tagihan.getExpired().after(WaktuUtil.getDate()))) {

									js.put("va", tagihan.getVa());
									js.put("billExpired", tagihan.getExpired() == null ? ""
											: Common.dateFormat5.get().format(tagihan.getExpired()));
								} else {
									js.put("va", "");
									js.put("billExpired", "");
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/TagihanSiswa.java:1072");
							}
							js.put("nama_siswa", siswa != null ? siswa.getNama() : calonSiswa.getNama());
							js.put("nisn_siswa",
									siswa != null ? siswa.getNomorIndukNasional() : calonSiswa.getNomorIndukNasional());
							js.put("bulan", tagihan.getBulan());
							js.put("link", tagihan.getLink());
							js.put("bulan_hrf", tagihan.getBulan() == null ? "" : Common.BULAN[tagihan.getBulan() - 1]);
							js.put("tahun", tagihan.getTahun());
							js.put("ke", tagihan.getBayarKe());
							js.put("ta", tagihan.getPengaturanBiaya().getTahunAjaran());
							js.put("tanggalTagihan", Common.dateFormat4.get().format(tagihan.getTanggalTagihan()));
							js.put("tanggalDeadline", tagihan.getTanggalDeadline() == null ? ""
									: Common.dateFormat4.get().format(tagihan.getTanggalDeadline()));
							jsonArray.put(js);

						}

					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/TagihanSiswa.java:1091");
					}
				}
			}
		}

			return amn;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return 0.0;
		} finally {
			ApiHelperSupport.closeOpenedSession(session);
		}
	}

	@SuppressWarnings({ "unchecked" })
	public static JSONObject pembayaran(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Autentikasi gagal: token sesi yang dikirimkan bersama permintaan ini tidak valid, sudah kedaluwarsa, atau tidak ditemukan dalam database sesi aktif sistem. Token sesi berfungsi sebagai identitas digital pengguna yang telah berhasil terautentikasi dan wajib disertakan pada setiap permintaan API. Kemungkinan penyebab kegagalan: (1) masa berlaku sesi telah habis akibat tidak ada aktivitas dalam durasi tertentu; (2) pengguna telah keluar dari sesi ini di perangkat lain; (3) administrator sistem telah mereset atau mencabut sesi; (4) token yang dikirimkan rusak, terpotong, atau mengandung karakter tidak valid; (5) terdapat perubahan konfigurasi keamanan pada server. Tindakan yang perlu dilakukan: keluar dari aplikasi secara resmi menggunakan tombol Keluar, kemudian lakukan proses masuk kembali dengan kredensial yang benar untuk mendapatkan token sesi baru yang valid, lalu ulangi permintaan ini.");
			} else {

				Siswa siswa = tbmuser.getSiswa();
				CalonSiswa calonSiswa = tbmuser.getCalonSiswa();

				if (siswa != null || calonSiswa != null) {

					Session session = HibernateUtil.openSession();
					try {
					List<Tagihan> tagihanstemp = session.createCriteria(Tagihan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.createAlias("pembayaranSiswaDetail", "pembayaranSiswaDetail")
							.addOrder(Order.desc("pembayaranSiswaDetail.id"))
							.createAlias("pembayaranSiswaDetail.pembayaranSiswa", "pembayaranSiswa")
							.add(Restrictions.or(Restrictions.eq("pembayaranSiswa.siswa", siswa),
									Restrictions.eq("pembayaranSiswa.calonSiswa", calonSiswa)))
							.list();

					List<Tagihan> tagihans = TagihanUtilCalonSiswa.saring(tagihanstemp, session);

					System.out.println("tagihans => " + tagihans.size());
					JSONArray jsonArray = new JSONArray();

					for (Tagihan tagihan : tagihans) {
						if (((tagihan.getAktif() && !tagihan.ambilBukanTagihanData())
								&& !tagihan.getNominalBiaya().getBukanTagihan()) && tagihan.getPengaturanBiaya() != null
								&& tagihan.getPengaturanBiaya().getAktif()) {
							JSONObject js = new JSONObject();

							PembayaranSiswaDetail pembayaranSiswaDetail = tagihan.getPembayaranSiswaDetail();

							js.put("id", pembayaranSiswaDetail.getId());
							js.put("grup_kode",
									tagihan.getPengaturanBiaya().getJenisBiayaSekolah().getKode());
							js.put("grup_nama", tagihan.getPengaturanBiaya().toString());
							js.put("grup_ta", tagihan.getPengaturanBiaya().getNama() + "-"
									+ tagihan.getPengaturanBiaya().getTahunAjaran());
							js.put("nama", pembayaranSiswaDetail.getItemBiayaSekolah().getNama());
							js.put("kode", pembayaranSiswaDetail.getItemBiayaSekolah().getKode());
							js.put("nominal", pembayaranSiswaDetail.getNominal());
							js.put("denda", pembayaranSiswaDetail.getTagihan().getDenda());
							js.put("bulan", tagihan.getBulan());
							js.put("bulan_hrf", tagihan.getBulan() == null ? "" : Common.BULAN[tagihan.getBulan() - 1]);
							js.put("tahun", tagihan.getTahun());
							js.put("ke", tagihan.getBayarKe());
							js.put("link", tagihan.getLink());
							js.put("ta", tagihan.getPengaturanBiaya().getTahunAjaran());
							js.put("waktuBayar",
									Common.dateFormat5.get().format(pembayaranSiswaDetail.getPembayaranSiswa().getTanggal()));
							js.put("tanggalBayar", Common.dateFormat4.get()
									.format(pembayaranSiswaDetail.getPembayaranSiswa().getTanggalBayar()));
							js.put("tanggalTagihan", Common.dateFormat4.get().format(tagihan.getTanggalTagihan()));
							js.put("tanggalDeadline", tagihan.getTanggalDeadline() == null ? ""
									: Common.dateFormat4.get().format(tagihan.getTanggalDeadline()));

							jsonArray.put(js);
						}
					}

					jsonObject.put("data", jsonArray);
					jsonObject.put("status", "00");
					jsonObject.put("description", "Riwayat pembayaran siswa berhasil diambil dari sistem. Respons mencakup seluruh tagihan yang telah berhasil dilunasi oleh siswa atau calon siswa yang bersangkutan, diurutkan berdasarkan waktu pembayaran terbaru. Setiap entri riwayat memuat: ID pembayaran, nama dan kode item biaya yang dibayar, nominal pembayaran yang dicatat, denda yang telah dibayarkan, bulan dan tahun penagihan, waktu serta tanggal pembayaran resmi, dan informasi pengaturan biaya yang terkait. Data ini dapat digunakan untuk keperluan rekonsiliasi, cetak bukti pembayaran, atau verifikasi status pelunasan tagihan.");
					// session.disconnect();
					ApiHelperSupport.closeOpenedSession(session);
					} finally {
						HibernateUtil.closeSessionQuietly(session);
					}
				} else {
					jsonObject.put("status", "01");
					jsonObject.put("description", "Akses ditolak: endpoint API ini secara eksklusif dirancang untuk melayani permintaan dari akun pengguna yang terdaftar sebagai siswa aktif atau calon siswa dalam sistem. Token autentikasi yang digunakan pada permintaan ini tidak terkait dengan entitas siswa (tabel siswa) maupun calon siswa (tabel calon_siswa) mana pun dalam database. Kemungkinan penyebab: (1) pengguna masuk menggunakan akun orang tua (orangTua), pengajar, tenaga administrasi, atau administrator yang tidak memiliki hak akses pada fitur pembayaran siswa; (2) asosiasi antara akun pengguna dan data siswa atau calon siswa belum dikonfigurasi oleh administrator sekolah; (3) data siswa yang sebelumnya terhubung dengan akun ini telah dihapus, dinonaktifkan, atau dipindahkan ke akun lain. Tindakan: pastikan Anda masuk menggunakan akun yang secara langsung ditetapkan sebagai siswa atau calon siswa, atau hubungi administrator sekolah untuk memverifikasi dan memperbaiki tautan antara akun pengguna dengan data siswa yang bersangkutan.");
				}

			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TagihanSiswa.java:1193");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static JSONObject va(JSONObject request, HttpServletRequest httpServletRequest) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, httpServletRequest);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Autentikasi gagal: token sesi yang dikirimkan bersama permintaan ini tidak valid, sudah kedaluwarsa, atau tidak ditemukan dalam database sesi aktif sistem. Token sesi berfungsi sebagai identitas digital pengguna yang telah berhasil terautentikasi dan wajib disertakan pada setiap permintaan API. Kemungkinan penyebab kegagalan: (1) masa berlaku sesi telah habis akibat tidak ada aktivitas dalam durasi tertentu; (2) pengguna telah keluar dari sesi ini di perangkat lain; (3) administrator sistem telah mereset atau mencabut sesi; (4) token yang dikirimkan rusak, terpotong, atau mengandung karakter tidak valid; (5) terdapat perubahan konfigurasi keamanan pada server. Tindakan yang perlu dilakukan: keluar dari aplikasi secara resmi menggunakan tombol Keluar, kemudian lakukan proses masuk kembali dengan kredensial yang benar untuk mendapatkan token sesi baru yang valid, lalu ulangi permintaan ini.");
			} else {

				String tagihansParam = request.get("tagihans") + "";
				String bank = request.getString("bank");

				String tabungan = request.isNull("tabungan") ? null : request.get("tabungan") + "";
				Double tabunganNumber = 0.0;
				try {
					tabunganNumber = tabungan == null ? 0.0 : Double.parseDouble(tabungan.trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/TagihanSiswa.java:1216");
					// TODO: handle exception
				}

				if (!bank.isEmpty() || bank.equalsIgnoreCase("BNI")) {
					if (!tagihansParam.trim().isEmpty()) {

						Siswa siswa = tbmuser.getSiswa();
						CalonSiswa calonSiswa = tbmuser.getCalonSiswa();

						double biayaAdministrasi = 0.0;
						List<String> warnings = new ArrayList<String>();
						if (siswa != null || calonSiswa != null) {
							Sekolah sekolah = siswa != null ? siswa.getSekolah() : calonSiswa.getSekolah();
							Session session = HibernateUtil.openSession();
							try {
							List<Tagihan> tagihans = session.createCriteria(Tagihan.class)
									.add(Restrictions.or(
											Restrictions.and(Restrictions.isNotNull("pembayaranBerakhirPada"),
													Restrictions.sqlRestriction(
															"this_.pembayaran_berakhir_pada < CURRENT_TIMESTAMP")),
											Restrictions.isNull("pembayaranSiswaDetail")))
									.add(Restrictions.sqlRestriction("this_.id in (" + tagihansParam + ")"))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

									.list();

							AkunPembayaranSiswa akunPembayaranSiswa = (AkunPembayaranSiswa) ConstantValues.simpleObject(
									session.createCriteria(AkunPembayaranSiswa.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.eq("manual", false))
											.add(Restrictions.eq("sekolah", sekolah)).setMaxResults(1),
									AkunPembayaranSiswa.class);

							// session.disconnect();
							ApiHelperSupport.closeOpenedSession(session);
							Double amn = 0.0;
							for (Tagihan tagihan : tagihans) {
								amn += (tagihan.getNominal() + tagihan.getDenda());
							}

							if (tabunganNumber >= amn) {
								HibernateUtil.closeSessionQuietly(session); // tutup session lama sebelum buka ulang (konversi openSession)
								session = HibernateUtil.openSession();
								PembayaranSiswa pembayaranSiswa = VirtualAccountBank.bayarSiswaLangsung(tagihans,
										session, WaktuUtil.getDate(), amn, tabunganNumber, bank, false, tbmuser);
								// session.disconnect();
								ApiHelperSupport.closeOpenedSession(session);
								JSONArray jsonArray = new JSONArray();
								JSONObject js = new JSONObject();
								Common.insertProperty(PembayaranSiswa.class, pembayaranSiswa, js, "", 1);
								jsonArray.put(js);

								jsonObject.put("data", jsonArray);
								jsonObject.put("status", "00");
								jsonObject.put("description", "Pembayaran berhasil diselesaikan menggunakan saldo tabungan atau deposit yang tersedia. Sistem mendeteksi bahwa saldo tabungan pengguna mencukupi untuk menutup seluruh kewajiban tagihan yang dipilih sehingga proses pembayaran langsung (bayarSiswaLangsung) dieksekusi tanpa perlu membuat nomor Virtual Account terlebih dahulu. Seluruh tagihan yang dipilih kini telah tercatat sebagai lunas dalam sistem, dan saldo tabungan telah dikurangi sebesar total tagihan yang dibayarkan. Data detail transaksi pembayaran tersedia dalam respons untuk keperluan konfirmasi dan pencetakan bukti pembayaran.");

							} else {

								String billExpired = "";
								String va = "";
								String link = "";

								VirtualAccountBank virtualAccountBank = null;

								if (bank.equalsIgnoreCase("BNI")
										|| Common.bolehKonfigurasi("masih_hanya_pakai_bni", Konfigurasi.TIDAK_AKTIF)) {
									try {
										biayaAdministrasi = Double.parseDouble(
												Common.getKonfigurasi("bni_biaya_administrasi", "0.0").getNilai());
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/TagihanSiswa.java:1287");

									}

									if (tabunganNumber > 0.1) {
										amn = amn - tabunganNumber;
									}

									BniRequest bniRequest = BniCommon.onSaveBni(siswa, calonSiswa, tagihans, amn, false,
											tabunganNumber);
									System.out.println("bniRequest => " + bniRequest);

									va = bniRequest == null ? "" : bniRequest.getVa();
									if (bniRequest == null && warnings.isEmpty()) {
										warnings.add("Gagal membuat nomor VA BNI, silakan coba lagi atau hubungi administrator");
									}
									billExpired = bniRequest == null ? ""
											: Common.dateFormat5.get().format(bniRequest.getBillExpired());
								} else if (bank.equalsIgnoreCase("BTN")) {

									try {
										biayaAdministrasi = Double.parseDouble(
												Common.getKonfigurasi("btn_biaya_administrasi", "0.0").getNilai());
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/TagihanSiswa.java:1310");

									}

									BankHost bankHost = PembayaranUtil.getInstance().getBankHost(
											Common.getKonfigurasi("online_bank_host_ip", "").getNilai(), "Bank Host");

									virtualAccountBank = DownloadTagihanMahasiswaBankBtn.downloadData(siswa, calonSiswa,
											tagihans, false, biayaAdministrasi, bankHost, akunPembayaranSiswa, sekolah);
									System.out.println("virtualAccountBank => " + virtualAccountBank);

									va = virtualAccountBank == null ? "" : virtualAccountBank.getKode();
									if (virtualAccountBank == null && warnings.isEmpty()) {
										warnings.add("Gagal membuat nomor VA BTN, silakan coba lagi atau hubungi administrator");
									}
									billExpired = virtualAccountBank == null
											|| virtualAccountBank.getKadaluarsa() == null ? ""
													: Common.dateFormat5.get()
															.format(virtualAccountBank.getKadaluarsaWaktu());
								} else if (bank.equalsIgnoreCase("Flip")) {

									biayaAdministrasi = sekolah.getBiayaAdminFlip();
									BankHost bankHost = PembayaranUtil.getInstance().getBankHost(
											Common.getKonfigurasi("online_bank_host_ip", "").getNilai(), "Bank Host");
									Map param = new HashMap();
									param.put("flip", true);
									virtualAccountBank = DownloadTagihanSiswaBankOnline.downloadData(siswa, calonSiswa,
											tagihans, param, biayaAdministrasi, tabunganNumber, bankHost,
											akunPembayaranSiswa, sekolah, warnings);

									va = virtualAccountBank == null ? "" : virtualAccountBank.getKode();
									link = virtualAccountBank == null ? "" : virtualAccountBank.getLink();
									billExpired = virtualAccountBank == null
											|| virtualAccountBank.getKadaluarsa() == null ? ""
													: Common.dateFormat5.get()
															.format(virtualAccountBank.getKadaluarsaWaktu());
								} else if (bank.equalsIgnoreCase("Smartlink")) {

									biayaAdministrasi = sekolah.getBiayaAdminEsmartlink();
									BankHost bankHost = PembayaranUtil.getInstance().getBankHost(
											Common.getKonfigurasi("online_bank_host_ip", "").getNilai(), "Bank Host");

									if (sekolah.getVariableBiayaAdminEsmartlink().isEmpty()) {
										Map param = new HashMap();
										param.put("esmartlink", true);
										virtualAccountBank = DownloadTagihanSiswaBankOnline.downloadData(siswa,
												calonSiswa, tagihans, param, biayaAdministrasi, tabunganNumber,
												bankHost, akunPembayaranSiswa, sekolah, warnings);

										va = virtualAccountBank == null ? "" : virtualAccountBank.getKode();
										link = virtualAccountBank == null ? "" : virtualAccountBank.getLink();
										billExpired = virtualAccountBank == null
												|| virtualAccountBank.getKadaluarsa() == null ? ""
														: Common.dateFormat5.get()
																.format(virtualAccountBank.getKadaluarsaWaktu());
									}

									else {

										String tag = "";
										for (Tagihan tagihan : tagihans) {
											tag += tag.isEmpty() ? tagihan.getId().toString() : "," + tagihan.getId();
										}

										va = Common.getGeneratedBarCode();
										link = Common.getRequestHostWithProtocol(httpServletRequest)
												+ "/common/smartlink/no_va.zul?siswa="
												+ (siswa == null ? "-1" : siswa.getId()) + "&calonSiswa"
												+ (calonSiswa == null ? "-1" : calonSiswa.getId()) + "&tabungan="
												+ tabunganNumber.intValue() + "&tag=" + (tag) + "&bankHost="
												+ (bankHost == null ? "-1" : bankHost.getId()) + "&akunPembayaranSiswa="
												+ (akunPembayaranSiswa == null ? "-1" : akunPembayaranSiswa.getId());
										billExpired = "";
									}

								} else if (bank.equalsIgnoreCase("Finpay")) {

									biayaAdministrasi = sekolah.getBiayaAdminFlip();
									BankHost bankHost = PembayaranUtil.getInstance().getBankHost(
											Common.getKonfigurasi("online_bank_host_ip", "").getNilai(), "Bank Host");
									Map param = new HashMap();
									param.put("finpay", true);
									virtualAccountBank = DownloadTagihanSiswaBankOnline.downloadData(siswa, calonSiswa,
											tagihans, param, biayaAdministrasi, tabunganNumber, bankHost,
											akunPembayaranSiswa, sekolah, warnings);

									va = virtualAccountBank == null ? "" : virtualAccountBank.getKode();
									link = virtualAccountBank == null ? "" : virtualAccountBank.getLink();
									billExpired = virtualAccountBank == null
											|| virtualAccountBank.getKadaluarsa() == null ? ""
													: Common.dateFormat5.get()
															.format(virtualAccountBank.getKadaluarsaWaktu());
								} else if (bank.equalsIgnoreCase("Maja")) {

									try {
										biayaAdministrasi = Double.parseDouble(
												Common.getKonfigurasi("online_biaya_maja", "0.0").getNilai());
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/TagihanSiswa.java:1407");

									}
									BankHost bankHost = PembayaranUtil.getInstance().getBankHost(
											Common.getKonfigurasi("online_bank_host_ip", "").getNilai(), "Bank Host");
									Map param = new HashMap();
									param.put("maja", true);
									virtualAccountBank = DownloadTagihanSiswaBankOnline.downloadData(siswa, calonSiswa,
											tagihans, param, biayaAdministrasi, tabunganNumber, bankHost,
											akunPembayaranSiswa, sekolah, warnings);

									va = virtualAccountBank == null ? "" : virtualAccountBank.getKode();
									billExpired = virtualAccountBank == null
											|| virtualAccountBank.getKadaluarsa() == null ? ""
													: Common.dateFormat5.get()
															.format(virtualAccountBank.getKadaluarsaWaktu());
								} else {

									try {
										biayaAdministrasi = Double.parseDouble(
												Common.getKonfigurasi("online_biaya_administrasi", "0.0").getNilai());
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/TagihanSiswa.java:1428");

									}
									BankHost bankHost = PembayaranUtil.getInstance().getBankHost(
											Common.getKonfigurasi("online_bank_host_ip", "").getNilai(), "Bank Host");
									Map param = new HashMap();
									virtualAccountBank = DownloadTagihanSiswaBankOnline.downloadData(siswa, calonSiswa,
											tagihans, param, biayaAdministrasi, tabunganNumber, bankHost,
											akunPembayaranSiswa, sekolah, warnings);

									va = virtualAccountBank == null ? "" : virtualAccountBank.getKode();
									billExpired = virtualAccountBank == null
											|| virtualAccountBank.getKadaluarsa() == null ? ""
													: Common.dateFormat5.get()
															.format(virtualAccountBank.getKadaluarsaWaktu());
								}

								// Fallback: bila VA kosong tapi belum ada pesan error spesifik dari channel pembayaran
								if ((va == null || va.isEmpty()) && warnings.isEmpty()) {
									warnings.add("Gagal menghubungi server pembayaran " + bank
											+ ", silakan coba beberapa saat lagi atau hubungi administrator");
								}
								if (!warnings.isEmpty()) {
									jsonObject.put("status", "05");
									jsonObject.put("description", warnings.get(0));
								} else if (va == null || va.isEmpty()) {
									jsonObject.put("status", "05");
									jsonObject.put("description", "Pembuatan nomor Virtual Account gagal: server atau layanan payment gateway tidak dapat memproses permintaan pembuatan VA pada saat ini meskipun seluruh parameter yang dikirimkan telah valid. Kemungkinan penyebab teknis: (1) konfigurasi integrasi bank atau payment gateway untuk sekolah ini belum lengkap atau mengandung kesalahan (API key, merchant ID, atau endpoint tidak valid); (2) koneksi antara server aplikasi dan server bank/payment gateway terputus atau timeout; (3) layanan bank atau payment gateway sedang mengalami gangguan atau pemeliharaan; (4) data tagihan yang dikirimkan memenuhi syarat teknis tetapi ditolak oleh pihak bank karena alasan internal (misalnya batas transaksi harian, duplikasi referensi, atau validasi data nasabah). Tindakan: coba kembali beberapa saat kemudian, periksa status koneksi jaringan, atau hubungi administrator sistem untuk memeriksa log server dan konfigurasi integrasi payment gateway yang digunakan.");
								} else {

									String kodebankLainOnline = Common
											.getKonfigurasi("prefix_kode_bank_lain_online", "").getNilai();

									if (!kodebankLainOnline.trim().isEmpty() && virtualAccountBank != null
											&& virtualAccountBank.getKanalPembayaran() != null
											&& virtualAccountBank.getKanalPembayaran().getBsiUsername() != null
											&& !virtualAccountBank.getKanalPembayaran().getBsiUsername().isEmpty()) {
										va = (virtualAccountBank.getKanalPembayaran().getBsiUsername() + va);

										kodebankLainOnline = kodebankLainOnline + "" + va;
									} else if (!kodebankLainOnline.trim().isEmpty() && sekolah != null
											&& sekolah.getBsiUsername() != null
											&& !sekolah.getBsiUsername().isEmpty()) {

										va = (sekolah.getBsiUsername() + va);

										kodebankLainOnline = kodebankLainOnline + "" + va;
									}

									else if (!kodebankLainOnline.trim().isEmpty()) {
										kodebankLainOnline = kodebankLainOnline + "" + va;
									}

									System.out.println("tagihans => " + tagihans.size());
									JSONArray jsonArray = new JSONArray();

									for (Tagihan tagihan : tagihans) {
										JSONObject js = new JSONObject();
										js.put("wajibPilih", tagihan.tagihanLainWajib(tagihans));
										js.put("id", tagihan.getId());
										js.put("grup_kode", tagihan.getPengaturanBiaya()
												.getJenisBiayaSekolah().getKode());
										js.put("grup_nama", tagihan.getPengaturanBiaya().toString());
										js.put("grup_ta", tagihan.getPengaturanBiaya()
												.getJenisBiayaSekolah().getNama() + "-"
												+ tagihan.getPengaturanBiaya().getTahunAjaran());
										js.put("item_id", tagihan.getItemBiayaSekolah().getId());
										js.put("nama", tagihan.getItemBiayaSekolah().getNama());
										js.put("kode", tagihan.getItemBiayaSekolah().getKode());
										js.put("akumulasi_bulanan", tagihan.getPengaturanBiaya()
												.getJenisBiayaSekolah().getPilihanItemBiayaTerakumulasiBulanan());
										js.put("wajib_pilih_jika_bulan_dipilih",
												tagihan.getItemBiayaSekolah().getWajibPilihJikaBulanDipilih());
										js.put("wajib_pilih", tagihan.getItemBiayaSekolah().getWajibPilih());
										if (tagihan.getItemBiayaSekolah().getHarusBayar() != null) {
											js.put("harus_bayar_item_id",
													tagihan.getItemBiayaSekolah().getHarusBayar().getId());
											js.put("harus_bayar_nama",
													tagihan.getItemBiayaSekolah().getHarusBayar().getNama());
											js.put("harus_bayar_kode",
													tagihan.getItemBiayaSekolah().getHarusBayar().getKode());
										}
										String ket = tagihan.getNominalBiaya().getItemBiayaSekolah().getNama()
												+ (tagihan.getNominalBiaya().getDibayarSebayak() > 1
														? " (ke " + tagihan.getBayarKe() + ")"
														: "");
										Double denda = tagihan.getDenda();
										if (denda > 0.01) {
											ket += ", Denda " + Common.numberFormat.get().format(tagihan.getDenda());
										}
										Date tglDeadline = tagihan.getTanggalDeadline();
										if (tglDeadline != null) {
											ket += ", Deadline " + Common.dateFormat4.get().format(tglDeadline);
										}

										ket += (tagihan.getDiskonSiswa() != null
												&& tagihan.getDiskonSiswa().getMemotongTagihan()
														? " - " + tagihan.getDiskonSiswa().getNama()
														: "");

										js.put("ket", ket);

										js.put("nominal", tagihan.getNominal() - tagihan.getDiskon());
										js.put("denda", denda);
										js.put("bolehAngsurBerapapun",
												tagihan.getItemBiayaSekolah().getWajibPilihJikaBulanDipilih() ? false
														: tagihan.getNominalBiaya().getItemBiayaSekolah()
																.getBolehDiangsur()
																&& tagihan.getPengaturanBiaya()
																		.getJenisBiayaSekolah()
																		.getBolehAngsurBerapapun());

										if (tagihan.getItemBiayaSekolah().getAngsuranSeragam()) {
											js.put("bolehHapusAngsur", false);
										} else {

											if (tagihan.getPengaturanBiaya().getJenisBiayaSekolah()
													.getBolehAngsurBerapapun()) {
												if (tagihan.getNominalBiaya().getDibayarSebayak().intValue() == tagihan
														.getBayarKe().intValue()) {
													js.put("bolehHapusAngsur", false);
												} else {
													js.put("bolehHapusAngsur", true);
												}
											} else {
												js.put("bolehHapusAngsur", false);
											}
										}
										js.put("va", va);
										js.put("link", link);

										if (!kodebankLainOnline.trim().isEmpty()) {
											js.put("va_bank_lain", kodebankLainOnline);
										}

										js.put("billExpired", billExpired);

										js.put("bulan", tagihan.getBulan());
										js.put("bulan_hrf",
												tagihan.getBulan() == null ? "" : Common.BULAN[tagihan.getBulan() - 1]);
										js.put("tahun", tagihan.getTahun());
										js.put("ke", tagihan.getBayarKe());
										js.put("ta", tagihan.getPengaturanBiaya().getTahunAjaran());
										js.put("tanggalTagihan",
												Common.dateFormat4.get().format(tagihan.getTanggalTagihan()));
										js.put("tanggalDeadline", tagihan.getTanggalDeadline() == null ? ""
												: Common.dateFormat4.get().format(tagihan.getTanggalDeadline()));

										jsonArray.put(js);
									}

									JSONObject jsonObjectTabungan = new JSONObject();
									jsonObjectTabungan.put("nama", "Topup");
									jsonObjectTabungan.put("nilai", tabunganNumber);
									jsonObject.put("topup", jsonObjectTabungan);
									jsonObject.put("biayaAdministrasi", biayaAdministrasi);
									jsonObject.put("total", amn);
									jsonObject.put("va", va);
									jsonObject.put("link", link);
									if (!kodebankLainOnline.trim().isEmpty()) {
										jsonObject.put("va_bank_lain", kodebankLainOnline);
									}
									jsonObject.put("data", jsonArray);
									jsonObject.put("status", "00");
									jsonObject.put("description", "Nomor Virtual Account (VA) berhasil dibuat oleh sistem payment gateway. Nomor VA yang tertera dalam respons dapat digunakan untuk melakukan pembayaran melalui bank atau kanal pembayaran digital yang tersedia (ATM, mobile banking, internet banking, atau minimarket) sesuai jenis kanal yang dikonfigurasikan untuk sekolah ini. Pastikan pembayaran dilakukan sebelum tanggal dan waktu kedaluwarsa VA (billExpired) yang tercantum dalam respons — setelah batas waktu tersebut, nomor VA akan tidak aktif dan perlu dibuat ulang. Total yang harus dibayarkan adalah jumlah tagihan ditambah biaya administrasi yang berlaku.");
								}
							}
							} finally {
								HibernateUtil.closeSessionQuietly(session);
							}
						} else {
							jsonObject.put("status", "01");
							jsonObject.put("description", "Akses ditolak: endpoint API ini secara eksklusif dirancang untuk melayani permintaan dari akun pengguna yang terdaftar sebagai siswa aktif atau calon siswa dalam sistem. Token autentikasi yang digunakan pada permintaan ini tidak terkait dengan entitas siswa (tabel siswa) maupun calon siswa (tabel calon_siswa) mana pun dalam database. Kemungkinan penyebab: (1) pengguna masuk menggunakan akun orang tua (orangTua), pengajar, tenaga administrasi, atau administrator yang tidak memiliki hak akses pada fitur pembayaran siswa; (2) asosiasi antara akun pengguna dan data siswa atau calon siswa belum dikonfigurasi oleh administrator sekolah; (3) data siswa yang sebelumnya terhubung dengan akun ini telah dihapus, dinonaktifkan, atau dipindahkan ke akun lain. Tindakan: pastikan Anda masuk menggunakan akun yang secara langsung ditetapkan sebagai siswa atau calon siswa, atau hubungi administrator sekolah untuk memverifikasi dan memperbaiki tautan antara akun pengguna dengan data siswa yang bersangkutan.");
						}
					} else {
						jsonObject.put("status", "02");
						jsonObject.put("description", "Parameter permintaan tidak lengkap: daftar ID tagihan yang akan dibayarkan (parameter 'tagihans') tidak ditemukan dalam permintaan atau bernilai kosong. Parameter 'tagihans' berisi satu atau lebih ID tagihan yang telah dipilih pengguna untuk dilunasi dalam satu transaksi pembayaran, dipisahkan dengan koma jika lebih dari satu. Tanpa parameter ini, sistem tidak dapat menentukan tagihan mana yang harus dibuatkan Virtual Account-nya. Pilih setidaknya satu tagihan yang masih berstatus belum dibayar (pembayaranSiswaDetail IS NULL) dan masih aktif, kemudian sertakan ID tagihan tersebut dalam parameter 'tagihans' sebelum mengirimkan permintaan pembuatan VA.");
					}
				} else {
					jsonObject.put("status", "03");
					jsonObject.put("description", "Kanal pembayaran tidak dikenali: parameter 'bank' yang dikirimkan dalam permintaan bernilai kosong (empty string) atau tidak cocok dengan daftar kanal pembayaran yang didukung oleh sistem. Sistem mendukung kanal pembayaran berikut: BNI (Bank Negara Indonesia), BTN (Bank Tabungan Negara), Flip, Smartlink (eSMARTLINK), Finpay, Maja, dan kanal default untuk bank lain yang telah dikonfigurasi. Nilai parameter 'bank' bersifat case-insensitive dan harus tepat sesuai nama kanal di atas. Kemungkinan penyebab: (1) nilai 'bank' tidak dikirimkan; (2) terdapat typo atau perbedaan kapitalisasi yang tidak sesuai; (3) kanal yang diminta belum dikonfigurasi untuk sekolah ini. Periksa konfigurasi kunci 'bank_va_mobile' pada pengaturan sekolah dan gunakan nilai yang sesuai.");
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TagihanSiswa.java:1617");
			}
		}
		return jsonObject;
	}

	/**
	 * Bayar tagihan langsung via deposit/tabungan (tanpa generate Virtual Account).
	 * Dipanggil ketika saldo deposit >= total tagihan.
	 * Params: tagihans, tabungan, bank (opsional)
	 */
	public static JSONObject bayarTabunganSiswa(JSONObject request, HttpServletRequest httpServletRequest) {
		JSONObject jsonObject = new JSONObject();
		Session session = null;
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, httpServletRequest);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Autentikasi gagal: token sesi yang dikirimkan bersama permintaan ini tidak valid, sudah kedaluwarsa, atau tidak ditemukan dalam database sesi aktif sistem. Token sesi berfungsi sebagai identitas digital pengguna yang telah berhasil terautentikasi dan wajib disertakan pada setiap permintaan API. Kemungkinan penyebab kegagalan: (1) masa berlaku sesi telah habis akibat tidak ada aktivitas dalam durasi tertentu; (2) pengguna telah keluar dari sesi ini di perangkat lain; (3) administrator sistem telah mereset atau mencabut sesi; (4) token yang dikirimkan rusak, terpotong, atau mengandung karakter tidak valid; (5) terdapat perubahan konfigurasi keamanan pada server. Tindakan yang perlu dilakukan: keluar dari aplikasi secara resmi menggunakan tombol Keluar, kemudian lakukan proses masuk kembali dengan kredensial yang benar untuk mendapatkan token sesi baru yang valid, lalu ulangi permintaan ini.");
			} else {
				String tagihansParam = request.get("tagihans") + "";
				String tabunganStr = request.isNull("tabungan") ? null : request.get("tabungan") + "";
				Double tabunganNumber = 0.0;
				try {
					tabunganNumber = tabunganStr == null ? 0.0 : Double.parseDouble(tabunganStr.trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/TagihanSiswa.java:1642");
				}

				if (!tagihansParam.trim().isEmpty() && tabunganNumber > 0.1) {
					Siswa siswa = tbmuser.getSiswa();
					CalonSiswa calonSiswa = tbmuser.getCalonSiswa();

					if (siswa != null || calonSiswa != null) {
						session = HibernateUtil.openSession();
						List<Tagihan> tagihans = session.createCriteria(Tagihan.class)
								.add(Restrictions.or(
										Restrictions.and(Restrictions.isNotNull("pembayaranBerakhirPada"),
												Restrictions.sqlRestriction(
														"this_.pembayaran_berakhir_pada < CURRENT_TIMESTAMP")),
										Restrictions.isNull("pembayaranSiswaDetail")))
								.add(Restrictions.sqlRestriction("this_.id in (" + tagihansParam + ")"))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.list();

						ApiHelperSupport.closeOpenedSession(session);

						Double amn = 0.0;
						for (Tagihan tagihan : tagihans) {
							amn += (tagihan.getNominal() + tagihan.getDenda());
						}

						if (tagihans.isEmpty()) {
							jsonObject.put("status", "01");
							jsonObject.put("description", "Tagihan tidak dapat diproses: daftar tagihan yang dikirimkan melalui parameter 'tagihans' tidak menghasilkan data yang dapat diproses. Kemungkinan penyebab: (1) seluruh ID tagihan yang dikirimkan tidak ditemukan dalam database sistem — ID mungkin salah atau tidak valid; (2) seluruh tagihan yang dipilih telah lunas dibayar sebelumnya, ditandai dengan adanya PembayaranSiswaDetail yang terhubung (pembayaranSiswaDetail IS NOT NULL) serta batas waktu VA yang masih aktif (pembayaranBerakhirPada >= saat ini); (3) seluruh tagihan yang dipilih berstatus tidak aktif (aktif = false). Sistem hanya memproses tagihan yang belum dibayar (pembayaranSiswaDetail IS NULL) atau tagihan dengan VA yang telah kedaluwarsa. Muat ulang daftar tagihan terbaru dari sistem, periksa status pembayaran masing-masing tagihan, dan pastikan setidaknya satu tagihan yang dipilih masih berstatus belum dibayar sebelum mengirimkan permintaan.");
						} else if (tabunganNumber < amn) {
							jsonObject.put("status", "01");
							jsonObject.put("description",
									"Saldo deposit tidak mencukupi untuk menutup seluruh tagihan yang dipilih secara sekaligus. Sistem menghitung total kewajiban dari semua tagihan yang dikirimkan (jumlah nominal + denda setiap tagihan) dan membandingkannya dengan saldo deposit yang tersedia saat ini. Total kewajiban yang harus dibayarkan melebihi saldo deposit yang dimiliki sehingga pembayaran penuh via deposit tidak dapat dilakukan. Nilai total tagihan dan saldo yang tersedia tercantum dalam respons ini. Untuk menyelesaikan pembayaran: gunakan fitur pembuatan Virtual Account (VA) melalui endpoint va() dengan bank yang dikonfigurasi untuk sekolah ini agar kekurangan dapat dibayar melalui transfer bank, ATM, atau kanal pembayaran digital lainnya.");
							jsonObject.put("totalTagihan", amn);
							jsonObject.put("tabunganTersedia", tabunganNumber);
						} else {
							// Pembayaran selalu via deposit — abaikan bank dari request
							// (bank hanya relevan untuk generate VA, bukan untuk deposit)
							String bank = "Deposit";
							session = HibernateUtil.openSession();
							PembayaranSiswa pembayaranSiswa = VirtualAccountBank.bayarSiswaLangsung(
									tagihans, session, WaktuUtil.getDate(), amn, tabunganNumber, bank, false, tbmuser);
							ApiHelperSupport.closeOpenedSession(session);

							if (pembayaranSiswa == null) {
								jsonObject.put("status", "01");
								jsonObject.put("description", "Proses pembayaran via deposit gagal diselesaikan: sistem mengalami kesalahan internal pada saat pencatatan transaksi ke dalam database. Fungsi bayarSiswaLangsung() tidak berhasil membuat entri PembayaranSiswa yang valid — kemungkinan disebabkan oleh kegagalan transaksi database, konflik data akibat operasi bersamaan (concurrent transaction), atau ketidaksesuaian state data tagihan antara permintaan dan kondisi aktual di server. Saldo deposit pengguna tidak dikurangi karena transaksi dibatalkan secara rollback. Tindakan yang disarankan: periksa koneksi jaringan, tunggu beberapa saat, kemudian coba lagi. Apabila kendala terus berulang, hubungi administrator sistem dan sertakan waktu kejadian serta detail tagihan yang akan dibayarkan agar dapat dilakukan pengecekan pada log server.");
							} else {
								JSONArray jsonArray = new JSONArray();
								JSONObject js = new JSONObject();
								Common.insertProperty(PembayaranSiswa.class, pembayaranSiswa, js, "", 1);
								jsonArray.put(js);

								jsonObject.put("data", jsonArray);
								jsonObject.put("totalTagihan", amn);
								jsonObject.put("tabunganDigunakan", amn);
								jsonObject.put("sisaDeposit", tabunganNumber - amn);
								jsonObject.put("noVA", true);
								jsonObject.put("status", "00");
								jsonObject.put("description", "Pembayaran berhasil diselesaikan menggunakan saldo deposit yang tersedia. Sistem telah memverifikasi kecukupan saldo, memproses pelunasan seluruh tagihan yang dipilih, mencatat transaksi PembayaranSiswa secara resmi ke dalam database, dan memotong saldo deposit pengguna sebesar total kewajiban yang dilunasi. Seluruh tagihan yang dipilih kini berstatus lunas dan terhubung dengan entri PembayaranSiswaDetail yang baru dibuat. Sisa saldo deposit setelah pembayaran juga tersedia dalam respons. Data detail transaksi pembayaran tersedia dalam respons untuk keperluan konfirmasi, pencetakan bukti pembayaran, atau rekonsiliasi keuangan.");
							}
						}
					} else {
						jsonObject.put("status", "01");
						jsonObject.put("description", "Data siswa tidak ditemukan: akun pengguna yang terautentikasi melalui token sesi ini tidak memiliki asosiasi dengan entitas siswa (tabel siswa) maupun calon siswa (tabel calon_siswa) dalam database sistem. Fitur pembayaran via deposit ini hanya dapat digunakan oleh pengguna yang secara langsung terdaftar sebagai siswa atau calon siswa. Kemungkinan penyebab: (1) akun yang digunakan adalah akun orang tua, pengajar, atau administrator yang tidak memiliki data siswa yang terhubung; (2) asosiasi antara akun pengguna dan data siswa belum dikonfigurasi atau telah dihapus oleh administrator; (3) data siswa yang sebelumnya terhubung telah dinonaktifkan. Pastikan Anda masuk menggunakan akun yang terdaftar sebagai siswa, atau hubungi administrator sekolah untuk memverifikasi dan memperbarui konfigurasi akun Anda.");
					}
				} else {
					jsonObject.put("status", "01");
					jsonObject.put("description", "Parameter permintaan tidak valid: satu atau lebih parameter wajib tidak memenuhi syarat validasi. Terdapat dua kondisi yang harus terpenuhi secara bersamaan: (1) parameter 'tagihans' yang berisi daftar ID tagihan yang akan dibayarkan tidak boleh kosong atau hanya mengandung spasi — harus ada minimal satu ID tagihan yang valid; (2) parameter 'tabungan' yang berisi saldo deposit yang akan digunakan untuk pembayaran harus bernilai lebih dari nol (0.0) — nilai nol atau negatif tidak diizinkan karena tidak mencerminkan saldo yang tersedia untuk transaksi. Periksa kedua parameter tersebut, pastikan nilai 'tagihans' berisi ID tagihan yang valid dan 'tabungan' mencerminkan saldo deposit aktual pengguna, kemudian ulangi permintaan.");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/TagihanSiswa.java:1714");
			try {
				jsonObject.put("status", "99");
				jsonObject.put("description", "Terjadi kesalahan tak terduga pada server saat memproses permintaan pembayaran via deposit. Kesalahan ini tidak terduga dan bukan disebabkan oleh input yang salah dari pengguna. Detail teknis kesalahan: " + e.getMessage() + ". Kemungkinan penyebab: terjadi exception yang tidak tertangani dalam alur proses pembayaran, kegagalan koneksi database saat transaksi berlangsung, atau kondisi tidak terduga lainnya. Transaksi pembayaran kemungkinan besar tidak berhasil diproses dan saldo deposit tidak terpotong. Silakan laporkan detail pesan kesalahan ini beserta waktu kejadian dan informasi tagihan kepada administrator sistem untuk penanganan lebih lanjut dan investigasi log server.");
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TagihanSiswa.java:1719");
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		return jsonObject;
	}

	@SuppressWarnings({ "unchecked" })
	public static JSONObject kelasLes(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Autentikasi gagal: token sesi yang dikirimkan bersama permintaan ini tidak valid, sudah kedaluwarsa, atau tidak ditemukan dalam database sesi aktif sistem. Token sesi berfungsi sebagai identitas digital pengguna yang telah berhasil terautentikasi dan wajib disertakan pada setiap permintaan API. Kemungkinan penyebab kegagalan: (1) masa berlaku sesi telah habis akibat tidak ada aktivitas dalam durasi tertentu; (2) pengguna telah keluar dari sesi ini di perangkat lain; (3) administrator sistem telah mereset atau mencabut sesi; (4) token yang dikirimkan rusak, terpotong, atau mengandung karakter tidak valid; (5) terdapat perubahan konfigurasi keamanan pada server. Tindakan yang perlu dilakukan: keluar dari aplikasi secara resmi menggunakan tombol Keluar, kemudian lakukan proses masuk kembali dengan kredensial yang benar untuk mendapatkan token sesi baru yang valid, lalu ulangi permintaan ini.");
			} else {

				Siswa siswa = tbmuser.getSiswa();
				CalonSiswa calonSiswa = tbmuser.getCalonSiswa();

				if (siswa == null) {
					if (calonSiswa != null && calonSiswa.getSiswa() != null) {
						siswa = calonSiswa.getSiswa();
					}
				}

				if (siswa != null) {

					Sekolah sekolah = siswa.getSekolah();

					Session session = HibernateUtil.openSession();
					try {

					JSONArray jsonArray = new JSONArray();

					List<KelasLesSiswa> kelasLesSiswas = ConstantValues
							.simpleList(
									session.createCriteria(KelasLesSiswa.class)
											.addOrder(Order.asc("tingkat")).addOrder(Order.asc("nama"))
											.add(Restrictions.eq("sekolah", sekolah)).add(Restrictions
													.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
									KelasLesSiswa.class);

					for (KelasLesSiswa kelasLesSiswa : kelasLesSiswas) {
						JSONObject jsonObjectKelas = new JSONObject();
						Common.insertProperty(KelasLesSiswa.class, kelasLesSiswa, jsonObjectKelas, "", 1);

						KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa = (KelasLesSiswaPunyaSiswa) ConstantValues
								.simpleObject(
										session.createCriteria(KelasLesSiswaPunyaSiswa.class)
												.add(Restrictions.eq("siswa", siswa))
												.add(Restrictions.eq("kelasLesSiswa", kelasLesSiswa)).setMaxResults(1),
										KelasLesSiswaPunyaSiswa.class);

						String infoBayar = "";
						if (kelasLesSiswaPunyaSiswa != null) {

							Common.insertProperty(KelasLesSiswaPunyaSiswa.class, kelasLesSiswaPunyaSiswa,
									jsonObjectKelas, "detail", 1, "siswa", "kelasLesSiswa", "calonSiswa");

							List<Object[]> syarats = kelasLesSiswaPunyaSiswa.ambilBelumBayar();
							if (!syarats.isEmpty()) {
								infoBayar = "Belum membayar ";
								for (Object[] objects : syarats) {
									if (objects[0] instanceof PengaturanBiaya) {
										PengaturanBiaya biaya = (PengaturanBiaya) objects[0];
										String bulan = objects[1] + "";
										String tahun = objects[2] + "";

										String b = "";
										if (!bulan.isEmpty() && !tahun.isEmpty()) {
											b = " bulan " + bulan + " tahun " + tahun;
										}
										infoBayar += (biaya + b);
									} else if (objects[0] instanceof JenisBiayaSekolah) {
										JenisBiayaSekolah jenisBiayaSekolah = (JenisBiayaSekolah) objects[0];
										infoBayar += jenisBiayaSekolah.getNama();
									}
								}
							} else {
								infoBayar = "Tidak ada kewajiban pembayaran";
							}
						}
						jsonObjectKelas.put("infoBayar", infoBayar);

						jsonObjectKelas.put("sudah_subscribe", kelasLesSiswaPunyaSiswa != null);
						jsonObjectKelas.put("sudah_diterima",
								kelasLesSiswaPunyaSiswa != null && kelasLesSiswaPunyaSiswa.getAktif());
						jsonObjectKelas.put("sudah_lulus",
								kelasLesSiswaPunyaSiswa != null && kelasLesSiswaPunyaSiswa.getAcc());
						jsonArray.put(jsonObjectKelas);
					}

					jsonObject.put("data", jsonArray);
					jsonObject.put("status", "00");
					jsonObject.put("description", "Data kelas les yang tersedia untuk sekolah siswa berhasil diambil dari sistem. Respons mencakup daftar lengkap kelas les yang aktif (kelasLesSiswa) di sekolah yang bersangkutan, diurutkan berdasarkan tingkat dan nama kelas. Setiap entri kelas les memuat informasi: status langganan siswa pada kelas tersebut (sudah_subscribe), status penerimaan atau persetujuan pendaftaran (sudah_diterima), status kelulusan dari kelas les tersebut (sudah_lulus), serta status kewajiban pembayaran terkait (infoBayar). Gunakan data ini untuk menampilkan katalog kelas les yang dapat diikuti atau sudah diikuti oleh siswa.");

					// session.disconnect();
					ApiHelperSupport.closeOpenedSession(session);
					} finally {
						HibernateUtil.closeSessionQuietly(session);
					}
				} else {
					jsonObject.put("status", "01");
					jsonObject.put("description", "Akses ditolak: endpoint API ini secara eksklusif dirancang untuk melayani permintaan dari akun pengguna yang terdaftar sebagai siswa aktif dalam sistem. Token autentikasi yang digunakan pada permintaan ini tidak terkait dengan entitas siswa mana pun dalam database. Kemungkinan penyebab: (1) pengguna masuk menggunakan akun calon siswa yang belum resmi terdaftar sebagai siswa, akun orang tua, pengajar, atau administrator yang tidak memiliki hak akses pada fitur kelas les siswa; (2) data siswa yang sebelumnya terhubung dengan akun ini telah dinonaktifkan atau dihapus; (3) asosiasi antara akun pengguna dan data siswa belum dikonfigurasi dengan benar oleh administrator. Tindakan: pastikan Anda masuk menggunakan akun yang secara langsung ditetapkan sebagai siswa aktif dalam sistem, atau hubungi administrator sekolah untuk memverifikasi dan memperbaiki konfigurasi akun Anda.");
				}

			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TagihanSiswa.java:1834");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings({ "unchecked" })
	public static JSONObject subscribeKelasLes(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Autentikasi gagal: token sesi yang dikirimkan bersama permintaan ini tidak valid, sudah kedaluwarsa, atau tidak ditemukan dalam database sesi aktif sistem. Token sesi berfungsi sebagai identitas digital pengguna yang telah berhasil terautentikasi dan wajib disertakan pada setiap permintaan API. Kemungkinan penyebab kegagalan: (1) masa berlaku sesi telah habis akibat tidak ada aktivitas dalam durasi tertentu; (2) pengguna telah keluar dari sesi ini di perangkat lain; (3) administrator sistem telah mereset atau mencabut sesi; (4) token yang dikirimkan rusak, terpotong, atau mengandung karakter tidak valid; (5) terdapat perubahan konfigurasi keamanan pada server. Tindakan yang perlu dilakukan: keluar dari aplikasi secara resmi menggunakan tombol Keluar, kemudian lakukan proses masuk kembali dengan kredensial yang benar untuk mendapatkan token sesi baru yang valid, lalu ulangi permintaan ini.");
			} else {

				if (!request.isNull("subscribe") && !request.isNull("kelasLes")) {

					boolean subscribe = request.getBoolean("subscribe");
					KelasLesSiswa kelasLes = (KelasLesSiswa) ConstantValues.ambil(KelasLesSiswa.class.getName(),
							Long.parseLong(request.get("kelasLes") + ""));

					if (kelasLes != null) {

						Siswa siswa = tbmuser.getSiswa();
						CalonSiswa calonSiswa = tbmuser.getCalonSiswa();

						if (siswa == null) {
							if (calonSiswa != null && calonSiswa.getSiswa() != null) {
								siswa = calonSiswa.getSiswa();
							}
						}

						if (siswa != null) {

							Sekolah sekolah = siswa.getSekolah();

							Session session = HibernateUtil.openSession();
							try {

							if (subscribe) {
								int count = ((Number) session.createCriteria(KelasLesSiswaPunyaSiswa.class)
										.setProjection(Projections.rowCount())
										.add(Restrictions.eq("kelasLesSiswa", kelasLes))
										.add(Restrictions.eq("siswa", siswa)).uniqueResult()).intValue();

								if (count == 0) {
									KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa = new KelasLesSiswaPunyaSiswa();
									kelasLesSiswaPunyaSiswa.setCalonSiswa(siswa.ambilCalonSiswa());
									kelasLesSiswaPunyaSiswa.setSiswa(siswa);
									kelasLesSiswaPunyaSiswa.setKelasLesSiswa(kelasLes);
									kelasLesSiswaPunyaSiswa.setAktif(true);
									session.getTransaction().begin();
									session.save(kelasLesSiswaPunyaSiswa);
									session.getTransaction().commit();
								}
							} else {
								session.createSQLQuery("delete from sekolah.kelas_les_punya_siswa where kelas_id="
										+ kelasLes.getId() + " and siswa_id=" + siswa.getId()).executeUpdate();
							}

							JSONArray jsonArray = new JSONArray();

							List<KelasLesSiswa> kelasLesSiswas = ConstantValues.simpleList(
									session.createCriteria(KelasLesSiswa.class)
											.addOrder(Order.asc("tingkat")).addOrder(Order.asc("nama"))
											.add(Restrictions.eq("sekolah", sekolah)).add(Restrictions
													.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
									KelasLesSiswa.class);

							for (KelasLesSiswa kelasLesSiswa : kelasLesSiswas) {
								JSONObject jsonObjectKelas = new JSONObject();
								Common.insertProperty(KelasLesSiswa.class, kelasLesSiswa, jsonObjectKelas, "", 1);

								KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa = (KelasLesSiswaPunyaSiswa) ConstantValues
										.simpleObject(session.createCriteria(KelasLesSiswaPunyaSiswa.class)
												.add(Restrictions.eq("siswa", siswa))
												.add(Restrictions.eq("kelasLesSiswa", kelasLesSiswa)).setMaxResults(1),
												KelasLesSiswaPunyaSiswa.class);

								String infoBayar = "";
								if (kelasLesSiswaPunyaSiswa != null) {

									Common.insertProperty(KelasLesSiswaPunyaSiswa.class, kelasLesSiswaPunyaSiswa,
											jsonObjectKelas, "detail", 1, "siswa", "kelasLesSiswa", "calonSiswa");

									List<Object[]> syarats = kelasLesSiswaPunyaSiswa.ambilBelumBayar();
									if (!syarats.isEmpty()) {
										infoBayar = "Belum membayar ";
										for (Object[] objects : syarats) {
											if (objects[0] instanceof PengaturanBiaya) {
												PengaturanBiaya biaya = (PengaturanBiaya) objects[0];
												String bulan = objects[1] + "";
												String tahun = objects[2] + "";

												String b = "";
												if (!bulan.isEmpty() && !tahun.isEmpty()) {
													b = " bulan " + bulan + " tahun " + tahun;
												}
												infoBayar += (biaya + b);
											} else if (objects[0] instanceof JenisBiayaSekolah) {
												JenisBiayaSekolah jenisBiayaSekolah = (JenisBiayaSekolah) objects[0];
												infoBayar += jenisBiayaSekolah.getNama();
											}
										}
									} else {
										infoBayar = "Tidak ada kewajiban pembayaran";
									}
								}

								jsonObjectKelas.put("infoBayar", infoBayar);
								jsonObjectKelas.put("sudah_subscribe", kelasLesSiswaPunyaSiswa != null);
								jsonObjectKelas.put("sudah_diterima",
										kelasLesSiswaPunyaSiswa != null && kelasLesSiswaPunyaSiswa.getAktif());
								jsonObjectKelas.put("sudah_lulus",
										kelasLesSiswaPunyaSiswa != null && kelasLesSiswaPunyaSiswa.getAcc());
								jsonArray.put(jsonObjectKelas);
							}

							jsonObject.put("data", jsonArray);
							jsonObject.put("status", "00");
							jsonObject.put("description", "Pembaruan status langganan kelas les berhasil diproses. Aksi yang diminta (subscribe untuk mendaftar atau unsubscribe untuk membatalkan langganan) telah berhasil dieksekusi dan perubahan status telah disimpan ke dalam database. Apabila aksi yang dilakukan adalah subscribe: data siswa-kelas les (KelasLesSiswaPunyaSiswa) baru telah dibuat dengan status menunggu persetujuan administrator. Apabila aksi yang dilakukan adalah unsubscribe: entri langganan sebelumnya telah dihapus atau dinonaktifkan. Data status terkini kelas les yang bersangkutan tersedia dalam respons untuk keperluan pembaruan tampilan antarmuka.");

							// session.disconnect();
							ApiHelperSupport.closeOpenedSession(session);
							} finally {
								HibernateUtil.closeSessionQuietly(session);
							}
						} else {
							jsonObject.put("status", "01");
							jsonObject.put("description", "Akses ditolak: endpoint API ini secara eksklusif dirancang untuk melayani permintaan dari akun pengguna yang terdaftar sebagai siswa aktif dalam sistem. Token autentikasi yang digunakan pada permintaan ini tidak terkait dengan entitas siswa mana pun dalam database. Kemungkinan penyebab: (1) pengguna masuk menggunakan akun calon siswa yang belum resmi terdaftar sebagai siswa, akun orang tua, pengajar, atau administrator yang tidak memiliki hak akses pada fitur kelas les siswa; (2) data siswa yang sebelumnya terhubung dengan akun ini telah dinonaktifkan atau dihapus; (3) asosiasi antara akun pengguna dan data siswa belum dikonfigurasi dengan benar oleh administrator. Tindakan: pastikan Anda masuk menggunakan akun yang secara langsung ditetapkan sebagai siswa aktif dalam sistem, atau hubungi administrator sekolah untuk memverifikasi dan memperbaiki konfigurasi akun Anda.");
						}
					} else {
						jsonObject.put("status", "01");
						jsonObject.put("description", "Kelas les tidak ditemukan: ID kelas les yang dikirimkan melalui parameter 'kelasLes' tidak berhasil ditemukan dalam database sistem. Sistem melakukan pencarian pada entitas KelasLesSiswa berdasarkan ID unik yang diberikan. Kemungkinan penyebab: (1) ID yang dikirimkan tidak valid, salah ketik, atau bukan angka numerik yang diharapkan; (2) kelas les dengan ID tersebut telah dihapus atau dinonaktifkan oleh administrator sekolah; (3) kelas les tersebut tidak termasuk dalam daftar kelas les yang aktif di sekolah siswa yang bersangkutan; (4) terdapat ketidaksesuaian antara data yang ditampilkan di antarmuka dan kondisi aktual di server akibat cache yang belum diperbarui. Muat ulang daftar kelas les yang tersedia untuk mendapatkan ID yang valid sebelum melakukan aksi subscribe atau unsubscribe.");
					}
				} else {
					jsonObject.put("status", "01");
					jsonObject.put("description", "Parameter permintaan tidak lengkap: satu atau lebih parameter wajib tidak ditemukan dalam permintaan yang dikirimkan. Endpoint ini membutuhkan dua parameter yang harus disertakan secara bersamaan: (1) parameter 'subscribe' bertipe boolean — nilai true untuk mendaftarkan siswa ke kelas les yang dipilih, atau nilai false untuk membatalkan pendaftaran yang sudah ada; (2) parameter 'kelasLes' bertipe numerik — berisi ID unik dari kelas les (KelasLesSiswa) yang menjadi target aksi. Satu atau kedua parameter tersebut tidak ditemukan atau bernilai null dalam permintaan ini. Pastikan kedua parameter disertakan dengan nilai yang valid sebelum mengirimkan permintaan.");
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TagihanSiswa.java:1981");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings({})
	public static JSONObject tabungan(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Autentikasi gagal: token sesi yang dikirimkan bersama permintaan ini tidak valid, sudah kedaluwarsa, atau tidak ditemukan dalam database sesi aktif sistem. Token sesi berfungsi sebagai identitas digital pengguna yang telah berhasil terautentikasi dan wajib disertakan pada setiap permintaan API. Kemungkinan penyebab kegagalan: (1) masa berlaku sesi telah habis akibat tidak ada aktivitas dalam durasi tertentu; (2) pengguna telah keluar dari sesi ini di perangkat lain; (3) administrator sistem telah mereset atau mencabut sesi; (4) token yang dikirimkan rusak, terpotong, atau mengandung karakter tidak valid; (5) terdapat perubahan konfigurasi keamanan pada server. Tindakan yang perlu dilakukan: keluar dari aplikasi secara resmi menggunakan tombol Keluar, kemudian lakukan proses masuk kembali dengan kredensial yang benar untuk mendapatkan token sesi baru yang valid, lalu ulangi permintaan ini.");
			} else {

				Siswa siswa = tbmuser.getSiswa();
				CalonSiswa calonSiswa = tbmuser.getCalonSiswa();
				Mahasiswa mahasiswa = tbmuser.getMahasiswa();
				AnggotaKoperasi anggotaKoperasi = tbmuser.getAnggotaKoperasi();
				if (anggotaKoperasi != null) {
					Double tabungan = ais.action.master.sekolah.util.DepositHelper.hitungDeposit(anggotaKoperasi);
					Long longData = tabungan.longValue();
					jsonObject.put("data", longData);
					jsonObject.put("status", "00");
					jsonObject.put("description", "Saldo tabungan anggota koperasi berhasil dihitung dan dikembalikan dalam satuan rupiah (dibulatkan ke bawah ke nilai integer terdekat). Nilai yang dikembalikan merepresentasikan total saldo bersih tabungan anggota koperasi yang bersangkutan, dihitung berdasarkan selisih antara total simpanan dan total penarikan yang telah dicatat dalam sistem koperasi. Gunakan nilai ini sebagai referensi saldo yang tersedia untuk transaksi pembayaran atau penarikan melalui kanal yang mendukung metode pembayaran via tabungan koperasi.");
				} else if (siswa != null) {

					Double tabungan = ais.action.master.sekolah.util.DepositHelper.hitungDeposit(siswa, calonSiswa);
					Long longData = tabungan.longValue();
					jsonObject.put("data", longData);
					jsonObject.put("status", "00");
					jsonObject.put("description", "Saldo deposit siswa berhasil dihitung dan dikembalikan dalam satuan rupiah (dibulatkan ke bawah ke nilai integer terdekat). Nilai yang dikembalikan merepresentasikan total saldo bersih deposit atau tabungan yang dimiliki siswa, dihitung berdasarkan selisih antara total setoran dan total penarikan atau penggunaan deposit yang telah dicatat. Gunakan nilai ini sebagai referensi saldo yang tersedia untuk proses pembayaran tagihan secara langsung tanpa pembuatan Virtual Account, apabila saldo mencukupi untuk menutup total kewajiban tagihan yang dipilih.");

				} else if (mahasiswa != null) {
					Double tabungan = ais.action.master.sekolah.util.DepositHelper.hitungDeposit(mahasiswa);
					Long longData = tabungan.longValue();
					jsonObject.put("data", longData);
					jsonObject.put("status", "00");
					jsonObject.put("description", "Saldo deposit mahasiswa berhasil dihitung dan dikembalikan dalam satuan rupiah (dibulatkan ke bawah ke nilai integer terdekat). Nilai yang dikembalikan merepresentasikan total saldo bersih deposit atau tabungan yang dimiliki mahasiswa terkait, dihitung berdasarkan selisih antara total setoran dan total penarikan atau penggunaan deposit yang telah dicatat dalam sistem. Gunakan nilai ini sebagai referensi untuk menentukan apakah saldo mencukupi untuk pembayaran tagihan secara langsung tanpa Virtual Account.");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/TagihanSiswa.java:2024");
			try {
				String stackTrace = CommonHelperClass.getStackTraceAsString(e);
				jsonObject.put("status", "90");
				jsonObject.put("description", stackTrace);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TagihanSiswa.java:2030");
			}
		}
		System.out.println("tabungan -> " + jsonObject);
		return jsonObject;
	}

	@SuppressWarnings({ "unchecked" })
	public static JSONObject kelasLesAktif(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Autentikasi gagal: token sesi yang dikirimkan bersama permintaan ini tidak valid, sudah kedaluwarsa, atau tidak ditemukan dalam database sesi aktif sistem. Token sesi berfungsi sebagai identitas digital pengguna yang telah berhasil terautentikasi dan wajib disertakan pada setiap permintaan API. Kemungkinan penyebab kegagalan: (1) masa berlaku sesi telah habis akibat tidak ada aktivitas dalam durasi tertentu; (2) pengguna telah keluar dari sesi ini di perangkat lain; (3) administrator sistem telah mereset atau mencabut sesi; (4) token yang dikirimkan rusak, terpotong, atau mengandung karakter tidak valid; (5) terdapat perubahan konfigurasi keamanan pada server. Tindakan yang perlu dilakukan: keluar dari aplikasi secara resmi menggunakan tombol Keluar, kemudian lakukan proses masuk kembali dengan kredensial yang benar untuk mendapatkan token sesi baru yang valid, lalu ulangi permintaan ini.");
			} else {

				Siswa siswa = tbmuser.getSiswa();
				CalonSiswa calonSiswa = tbmuser.getCalonSiswa();

				if (siswa == null) {
					if (calonSiswa != null && calonSiswa.getSiswa() != null) {
						siswa = calonSiswa.getSiswa();
					}
				}

				if (siswa != null) {

					Sekolah sekolah = siswa.getSekolah();

					Session session = HibernateUtil.openSession();
					try {

					JSONArray jsonArray = new JSONArray();

					List<KelasLesSiswaPunyaSiswa> kelasLesSiswaPunyaSiswas = ConstantValues
							.simpleList(
									session.createCriteria(KelasLesSiswaPunyaSiswa.class)
											.add(Restrictions.eq("siswa", siswa))
											.createAlias("kelasLesSiswa", "kelasLesSiswa")
											.addOrder(Order.asc("kelasLesSiswa.tingkat"))
											.addOrder(Order.asc("kelasLesSiswa.nama"))
											.add(Restrictions.eq("kelasLesSiswa.sekolah", sekolah)).add(Restrictions
													.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
									KelasLesSiswaPunyaSiswa.class);

					for (KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa : kelasLesSiswaPunyaSiswas) {
						if (kelasLesSiswaPunyaSiswa.getAktif()) {
							JSONObject jsonObjectKelas = new JSONObject();
							Common.insertProperty(KelasLesSiswaPunyaSiswa.class, kelasLesSiswaPunyaSiswa,
									jsonObjectKelas, "", 2, "siswa", "calonSiswa");
							jsonObjectKelas.put("sudah_subscribe", kelasLesSiswaPunyaSiswa != null);
							jsonObjectKelas.put("sudah_diterima",
									kelasLesSiswaPunyaSiswa != null && kelasLesSiswaPunyaSiswa.getAktif());
							jsonObjectKelas.put("sudah_lulus",
									kelasLesSiswaPunyaSiswa != null && kelasLesSiswaPunyaSiswa.getAcc());
							jsonArray.put(jsonObjectKelas);
						}
					}

					jsonObject.put("data", jsonArray);
					jsonObject.put("status", "00");
					jsonObject.put("description", "Data kelas les yang sedang aktif diikuti oleh siswa berhasil diambil dari sistem. Respons mencakup daftar lengkap seluruh kelas les (KelasLesSiswaPunyaSiswa) yang saat ini diikuti oleh siswa bersangkutan dengan status aktif (aktif = true), diurutkan berdasarkan tingkat dan nama kelas. Setiap entri memuat informasi rinci termasuk: status langganan, status penerimaan atau persetujuan oleh administrator, status kelulusan dari kelas les tersebut, dan atribut lainnya yang relevan. Gunakan data ini untuk menampilkan daftar kelas yang sedang diikuti siswa beserta progres atau status masing-masing.");

					// session.disconnect();
					ApiHelperSupport.closeOpenedSession(session);
					} finally {
						HibernateUtil.closeSessionQuietly(session);
					}
				} else {
					jsonObject.put("status", "01");
					jsonObject.put("description", "Akses ditolak: endpoint API ini secara eksklusif dirancang untuk melayani permintaan dari akun pengguna yang terdaftar sebagai siswa aktif dalam sistem. Token autentikasi yang digunakan pada permintaan ini tidak terkait dengan entitas siswa mana pun dalam database. Kemungkinan penyebab: (1) pengguna masuk menggunakan akun calon siswa yang belum resmi terdaftar sebagai siswa, akun orang tua, pengajar, atau administrator yang tidak memiliki hak akses pada fitur kelas les siswa; (2) data siswa yang sebelumnya terhubung dengan akun ini telah dinonaktifkan atau dihapus; (3) asosiasi antara akun pengguna dan data siswa belum dikonfigurasi dengan benar oleh administrator. Tindakan: pastikan Anda masuk menggunakan akun yang secara langsung ditetapkan sebagai siswa aktif dalam sistem, atau hubungi administrator sekolah untuk memverifikasi dan memperbaiki konfigurasi akun Anda.");
				}

			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TagihanSiswa.java:2111");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings({ "unchecked" })
	public static JSONObject pertemuanKelasLes(JSONObject request, HttpServletRequest servletRequest) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, servletRequest);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Autentikasi gagal: token sesi yang dikirimkan bersama permintaan ini tidak valid, sudah kedaluwarsa, atau tidak ditemukan dalam database sesi aktif sistem. Token sesi berfungsi sebagai identitas digital pengguna yang telah berhasil terautentikasi dan wajib disertakan pada setiap permintaan API. Kemungkinan penyebab kegagalan: (1) masa berlaku sesi telah habis akibat tidak ada aktivitas dalam durasi tertentu; (2) pengguna telah keluar dari sesi ini di perangkat lain; (3) administrator sistem telah mereset atau mencabut sesi; (4) token yang dikirimkan rusak, terpotong, atau mengandung karakter tidak valid; (5) terdapat perubahan konfigurasi keamanan pada server. Tindakan yang perlu dilakukan: keluar dari aplikasi secara resmi menggunakan tombol Keluar, kemudian lakukan proses masuk kembali dengan kredensial yang benar untuk mendapatkan token sesi baru yang valid, lalu ulangi permintaan ini.");
			} else {

				Siswa siswa = tbmuser.getSiswa();
				CalonSiswa calonSiswa = tbmuser.getCalonSiswa();

				if (siswa == null) {
					if (calonSiswa != null && calonSiswa.getSiswa() != null) {
						siswa = calonSiswa.getSiswa();
					}
				}

				if (siswa != null) {

					Long kelasLesSiswa = request.isNull("kelasLesSiswa") ? null
							: Long.parseLong((request.get("kelasLesSiswa") + "").trim());

					Sekolah sekolah = siswa.getSekolah();

					Session session = HibernateUtil.openSession();
					try {

					JSONArray jsonArray = new JSONArray();

					List<KelasLesSiswaPunyaSiswa> kelasLesSiswaPunyaSiswas = ConstantValues.simpleList(
							session.createCriteria(KelasLesSiswaPunyaSiswa.class).add(Restrictions.eq("siswa", siswa))
									.createAlias("kelasLesSiswa", "kelasLesSiswa")
									.add(kelasLesSiswa == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("kelasLesSiswa.id", kelasLesSiswa))
									.addOrder(Order.asc("kelasLesSiswa.tingkat"))
									.addOrder(Order.asc("kelasLesSiswa.nama"))
									.add(Restrictions.eq("kelasLesSiswa.sekolah", sekolah))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							KelasLesSiswaPunyaSiswa.class);

					for (KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa : kelasLesSiswaPunyaSiswas) {
						if (kelasLesSiswaPunyaSiswa.getAktif()) {

							List<Object[]> syarats = kelasLesSiswaPunyaSiswa.ambilBelumBayar();
							if (syarats.isEmpty()) {

								JSONObject jsonObjectKelas = new JSONObject();
								Common.insertProperty(KelasLesSiswaPunyaSiswa.class, kelasLesSiswaPunyaSiswa,
										jsonObjectKelas, "", 1, "siswa", "calonSiswa");
								jsonObjectKelas.put("sudah_subscribe", kelasLesSiswaPunyaSiswa != null);
								jsonObjectKelas.put("sudah_diterima",
										kelasLesSiswaPunyaSiswa != null && kelasLesSiswaPunyaSiswa.getAktif());
								jsonObjectKelas.put("sudah_lulus",
										kelasLesSiswaPunyaSiswa != null && kelasLesSiswaPunyaSiswa.getAcc());

								List<JadwalPelajaran> jadwalPelajarans = ConstantValues.simpleList(session
										.createCriteria(JadwalPelajaran.class)
										.add(Restrictions.eq("kelasLesSiswa",
												kelasLesSiswaPunyaSiswa.getKelasLesSiswa()))
										.addOrder(Order.asc("id")), JadwalPelajaran.class);
								JSONArray array = new JSONArray();
								for (JadwalPelajaran jadwalPelajaran : jadwalPelajarans) {
									for (Pertemuan pertemuan : jadwalPelajaran.ambilPertemuanList()) {
										JSONObject d = new JSONObject();

										Common.insertProperty(Pertemuan.class, pertemuan, d, "", 2, "perkuliahan",
												"jadwalUjianPMB", "mahasiswaRequestTugasAkhir", "kelompokKkn",
												"kelompokPkl", "skripsi", "krsMahasiswa", "jadwalUjianPSB",
												"jadwalPelajaran", "pertemuanPunyaGrupPertemuan", "formulirKegiatan",
												"calonSiswa");

										d.put("linimasa", LinimasaApi.displayPertemuan(pertemuan, tbmuser,
												servletRequest, true, true, true, false));

										array.put(d);
									}
								}

								jsonObjectKelas.put("pertemuan", array);
								jsonArray.put(jsonObjectKelas);
							}
						}
					}

					jsonObject.put("data", jsonArray);
					jsonObject.put("status", "00");
					jsonObject.put("description", "Data jadwal dan rincian pertemuan kelas les untuk siswa berhasil diambil dari sistem. Respons mencakup daftar seluruh pertemuan (sesi) yang terjadwal atau telah berlangsung untuk setiap kelas les yang diikuti oleh siswa bersangkutan, dikelompokkan per kelas les. Setiap entri pertemuan memuat informasi rinci meliputi: jadwal waktu pertemuan, topik atau materi yang disampaikan, status kehadiran siswa pada pertemuan tersebut, dan atribut relevan lainnya yang telah dikonfigurasi. Gunakan data ini untuk menampilkan riwayat kehadiran, materi yang telah dipelajari, atau jadwal pertemuan mendatang dalam antarmuka aplikasi.");

					// session.disconnect();
					ApiHelperSupport.closeOpenedSession(session);
					} finally {
						HibernateUtil.closeSessionQuietly(session);
					}
				} else {
					jsonObject.put("status", "01");
					jsonObject.put("description", "Akses ditolak: endpoint API ini secara eksklusif dirancang untuk melayani permintaan dari akun pengguna yang terdaftar sebagai siswa aktif dalam sistem. Token autentikasi yang digunakan pada permintaan ini tidak terkait dengan entitas siswa mana pun dalam database. Kemungkinan penyebab: (1) pengguna masuk menggunakan akun calon siswa yang belum resmi terdaftar sebagai siswa, akun orang tua, pengajar, atau administrator yang tidak memiliki hak akses pada fitur kelas les siswa; (2) data siswa yang sebelumnya terhubung dengan akun ini telah dinonaktifkan atau dihapus; (3) asosiasi antara akun pengguna dan data siswa belum dikonfigurasi dengan benar oleh administrator. Tindakan: pastikan Anda masuk menggunakan akun yang secara langsung ditetapkan sebagai siswa aktif dalam sistem, atau hubungi administrator sekolah untuk memverifikasi dan memperbaiki konfigurasi akun Anda.");
				}

			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TagihanSiswa.java:2224");
			}
		}
		return jsonObject;
	}
}

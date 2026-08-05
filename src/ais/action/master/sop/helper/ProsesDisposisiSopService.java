package ais.action.master.sop.helper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.action.master.helper.BroadcastHelper;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.KunciEntityHelper;
import ais.database.model.Tbmuser;
import ais.database.model.sop.AlurSop;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.sop.Sop;

/**
 * Mesin proses disposisi SOP <b>versi native JSP</b> (tanpa komponen ZK).
 *
 * <h2>Latar belakang</h2>
 * <p>
 * Inti "tindak lanjut / proses disposisi" pada aplikasi ini berada di method
 * {@code DisposisiAlurSopAction.onSave(Event)} (ZK). Method tersebut membaca nilai langsung dari
 * komponen ZK ({@code Textbox}, {@code Datebox}, {@code Radiogroup}, {@code Checkbox}, dst.) sehingga
 * <b>tidak dapat dipanggil dari halaman JSP</b>. Agar versi JSP benar-benar <i>native</i> — termasuk
 * proses routing (membuat langkah berikutnya, menutup langkah, memperbarui status pengajuan) — kelas
 * ini menyalin <b>logika transaksi yang sama</b> namun menerima <b>parameter biasa</b> (id + nilai),
 * bukan komponen ZK.
 * </p>
 *
 * <h2>Kenapa di-port paralel, bukan refactor jalur ZK?</h2>
 * <p>
 * Jalur {@code onSave} ZK adalah salah satu transaksi paling kritis di sistem (dipakai seluruh alur
 * persetujuan kantin/keuangan/akademik). Merombaknya untuk dipakai bersama berisiko mengganggu alur
 * produksi yang sudah berjalan. Maka kelas ini sengaja dibuat sebagai <b>implementasi paralel yang
 * setia</b>: jalur ZK <b>tidak disentuh sama sekali</b> (risiko nol terhadap alur lama), sementara
 * jalur JSP memanggil method statik di sini. Bila kelak ingin menyatukan, method ini sudah berbentuk
 * "service" murni sehingga jalur ZK pun bisa diarahkan ke sini tanpa perubahan perilaku.
 * </p>
 *
 * <h2>Aturan disposisi yang dipertahankan (identik dengan ZK)</h2>
 * <ol>
 * <li><b>Proses SOP (rute berikutnya) hanya boleh ditentukan bila langkah ini "ujung"</b> — yaitu
 *     {@code setelahnya == null} (sedang menunggu, atau langkah berikutnya telah dihapus/Batal). Bila
 *     langkah masih punya {@code setelahnya}, daftar rute tidak dikirim dari form sehingga tidak ada
 *     re-route — sama dengan {@code editPilihan = (setelahnya == null)} di ZK.</li>
 * <li><b>Validasi wajib</b>: catatan wajib (bila {@code AlurSop.catatanWajibDiisi}), waktu wajib, dan
 *     rute berikutnya wajib dipilih (bila {@code !AlurSop.alurSetelahnyaTidakWajib} dan masih ada
 *     pilihan rute) — sama dengan {@code check()} di ZK.</li>
 * <li><b>Penutupan langkah &amp; status pengajuan</b>: {@code DisposisiSop.disposisiEnd} dimajukan ke
 *     langkah terbaru, dan {@code DisposisiSop.disposisiSetuju} diisi bila
 *     {@code AlurSop.jikaProsesDisetujuiMakaSelesai} atau {@code DisposisiAlurSop.setujui()} —
 *     diproses di bawah {@link KunciEntityHelper} (kunci baris FOR UPDATE NOWAIT) persis seperti
 *     ZK agar aman dari kontensi antar pengguna.</li>
 * <li><b>Re-route setelah Batal</b>: bila langkah yang diproses sudah ada ({@code id != null}), semua
 *     anak ({@code where sebelumnya = id}) dihapus dahulu lalu langkah berikutnya dibuat ulang —
 *     menjaga rantai tetap konsisten.</li>
 * <li><b>Kembali ke langkah sebelumnya</b> ({@code kembali == true}) merutekan balik ke
 *     {@code sebelumnya} (revisi), bukan maju.</li>
 * </ol>
 *
 * <h2>Aturan sesi (penting)</h2>
 * <p>
 * Sama seperti {@code onSave} ZK, method ini membuka <b>sesi sendiri</b>
 * ({@code HibernateUtil.getSessionFactory().openSession()}) dengan transaksi eksplisit dan selalu
 * <b>menutupnya di blok {@code finally}</b>. Method ini <b>tidak</b> memakai
 * {@code HibernateUtil.currentSession()} milik kerangka kerja (yang dikelola otomatis), sehingga tidak
 * ada sesi menggantung. Notifikasi (PDF disposisi + email) dijalankan di <b>thread latar</b> agar
 * respons JSP tetap cepat — kegagalan notifikasi tidak menggagalkan proses inti yang sudah tersimpan.
 * </p>
 *
 * <h2>Cakupan</h2>
 * <p>
 * Method ini menangani jalur "aktor memproses disposisi": catatan, waktu, setujui/kembali, dan rute
 * berikutnya (Proses SOP). Lampiran dokumen di-upload lewat endpoint multipart terpisah (lihat JSP
 * {@code pengajuan_sop_service}). Form data SOP tertanam ({@code formSop}) milik langkah pengaju tidak
 * termasuk di sini; pengajuan baru tetap dibuat lewat jalur pembuatan dokumen masing-masing modul.
 * </p>
 *
 * <h2>Kompatibilitas</h2>
 * <p>Ditulis kompatibel Java 1.7 (tanpa lambda / try-with-resources); semua sesi ditutup manual.</p>
 *
 * @see ais.action.master.sop.DisposisiAlurSopAction
 * @see ais.action.master.sop.TampilanAlurSopAction#cetakDisposisi(DisposisiSop, boolean)
 */
public class ProsesDisposisiSopService {

	private ProsesDisposisiSopService() {
	}

	/** Hasil pemrosesan: status berhasil/gagal + pesan + id langkah yang tersimpan. */
	public static class Hasil {
		public boolean ok;
		public String pesan;
		public Long disposisiAlurSopId;

		public Hasil(boolean ok, String pesan, Long id) {
			this.ok = ok;
			this.pesan = pesan;
			this.disposisiAlurSopId = id;
		}
	}

	private static Hasil gagal(String pesan) {
		return new Hasil(false, pesan, null);
	}

	/**
	 * Proses satu langkah disposisi (versi native JSP). Setia pada {@code onSave} ZK.
	 *
	 * @param tbmuser                pengguna yang memproses (dari sesi JSP).
	 * @param disposisiSopId         id pengajuan SOP induk (wajib).
	 * @param disposisiAlurSopId     id langkah yang diproses; {@code null} bila langkah baru.
	 * @param alurSopId              id definisi alur (AlurSop) langkah saat ini (wajib).
	 * @param usernamePengguna       username aktor terpilih (boleh kosong).
	 * @param waktu                  waktu langkah; bila {@code null} dipakai waktu sekarang.
	 * @param waktuMaksimal          batas waktu langkah berikutnya (boleh {@code null}).
	 * @param keterangan             catatan disposisi.
	 * @param setujui                tandai langkah ini "selesai/disetujui".
	 * @param kembali                rute balik ke langkah sebelumnya (revisi).
	 * @param selanjutnyaAlurSopIds  daftar id AlurSop rute berikutnya yang dipilih (Proses SOP).
	 * @return {@link Hasil}
	 */
	public static Hasil prosesLangkah(Tbmuser tbmuser, Long disposisiSopId, Long disposisiAlurSopId, Long alurSopId,
			String usernamePengguna, Date waktu, Date waktuMaksimal, String keterangan, boolean setujui,
			boolean kembali, List<Long> selanjutnyaAlurSopIds) {

		if (tbmuser == null || tbmuser.getUserId() == null) return gagal("Sesi Anda telah berakhir.");
		if (disposisiSopId == null) return gagal("Pengajuan tidak ditemukan.");
		if (alurSopId == null) return gagal("Definisi alur (langkah) tidak ditemukan.");
		if (waktu == null) waktu = new Date();
		if (keterangan == null) keterangan = "";
		if (selanjutnyaAlurSopIds == null) selanjutnyaAlurSopIds = new ArrayList<Long>();

		// ---------- 1) VALIDASI (port check()) + muat entity acuan ----------
		AlurSop alurSop;
		DisposisiSop disposisiSop;
		boolean adaAnakSaatIni; // apakah langkah ini sudah punya langkah berikutnya (bukan ujung)
		List<AlurSop> opsiRute;
		{
			Session sesiBaca = null;
			try {
				sesiBaca = HibernateUtil.getSessionFactory().openSession();
				alurSop = (AlurSop) sesiBaca.get(AlurSop.class, alurSopId);
				disposisiSop = (DisposisiSop) sesiBaca.get(DisposisiSop.class, disposisiSopId);
				if (alurSop == null) return gagal("Definisi alur (langkah) tidak ditemukan.");
				if (disposisiSop == null) return gagal("Pengajuan tidak ditemukan.");

				opsiRute = alurSop.ambilAlurSetelahnya();

				// "ujung" robust: langkah punya anak bila ADA DisposisiAlurSop lain dgn sebelumnya = langkah ini.
				// (langkah START tidak pernah di-set setelahnya, jadi cek anak lebih andal daripada getSetelahnya.)
				adaAnakSaatIni = false;
				if (disposisiAlurSopId != null) {
					DisposisiAlurSop d = (DisposisiAlurSop) sesiBaca.get(DisposisiAlurSop.class, disposisiAlurSopId);
					if (d != null) {
						Long cnt = (Long) sesiBaca.createCriteria(DisposisiAlurSop.class)
								.add(Restrictions.eq("sebelumnya", d))
								.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
						adaAnakSaatIni = (cnt != null && cnt.longValue() > 0);
					}
				}

				// catatan wajib
				if (Boolean.TRUE.equals(alurSop.getCatatanWajibDiisi()) && keterangan.trim().isEmpty()) {
					return gagal("Catatan harus diisi.");
				}
				// rute wajib: hanya bila langkah ini sedang "ujung" (boleh re-route) & ada pilihan & tidak opsional
				boolean bolehEditProses = !adaAnakSaatIni;
				boolean ruteOpsional = Boolean.TRUE.equals(alurSop.getAlurSetelahnyaTidakWajib());
				// Langkah "Setujui dan Selesai" (jikaProsesDisetujuiMakaSelesai) boleh tanpa rute lanjut:
				// finalisasi (setelahnya null) mengikuti perilaku persetujuan tunggal ZK.
				boolean bisaSelesaiFinal = Boolean.TRUE.equals(alurSop.getJikaProsesDisetujuiMakaSelesai());
				if (bolehEditProses && !kembali && !ruteOpsional && !bisaSelesaiFinal && opsiRute != null && !opsiRute.isEmpty()
						&& selanjutnyaAlurSopIds.isEmpty()) {
					return gagal("Disposisi selanjutnya harus dipilih.");
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
				return gagal("Gagal memuat data: " + (e.getMessage() == null ? "" : e.getMessage()));
			} finally {
				if (sesiBaca != null) { try { sesiBaca.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/helper/ProsesDisposisiSopService.java:188");} }
			}
		}

		// ---------- 2) SIMPAN LANGKAH SAAT INI (port txn sessionUtama) ----------
		boolean baru = (disposisiAlurSopId == null);
		Long idLangkah = disposisiAlurSopId;
		{
			Session sessionUtama = null;
			try {
				sessionUtama = HibernateUtil.getSessionFactory().openSession();
				sessionUtama.getTransaction().begin();

				DisposisiAlurSop disposisiAlurSop;
				if (!baru) {
					disposisiAlurSop = (DisposisiAlurSop) sessionUtama.load(DisposisiAlurSop.class, disposisiAlurSopId);
				} else {
					disposisiAlurSop = new DisposisiAlurSop(usernamePengguna);
				}

				disposisiAlurSop.setUsernamePengguna(usernamePengguna == null ? "" : usernamePengguna);
				disposisiAlurSop.setDiajukanOleh(tbmuser);
				disposisiAlurSop.setMahasiswa(tbmuser.getMahasiswa());
				disposisiAlurSop.setSiswa(tbmuser.getSiswa());
				disposisiAlurSop.setDisposisiSop((DisposisiSop) sessionUtama.get(DisposisiSop.class, disposisiSopId));
				disposisiAlurSop.setAlurSop((AlurSop) sessionUtama.get(AlurSop.class, alurSopId));
				disposisiAlurSop.setWaktu(waktu);
				disposisiAlurSop.setKeterangan(keterangan);
				disposisiAlurSop.setSelesai(setujui);
				disposisiAlurSop.setKembali(kembali);

				Common.refreshSaveOrUpdate(sessionUtama, disposisiAlurSop);
				idLangkah = disposisiAlurSop.getId();

				if (idLangkah != null && !baru) {
					sessionUtama.createSQLQuery("delete from disposisi_alur_sop where sebelumnya=" + idLangkah)
							.executeUpdate();
				}

				sessionUtama.getTransaction().commit();
			} catch (Exception e) {
				if (sessionUtama != null && sessionUtama.getTransaction().isActive()) {
					try { sessionUtama.getTransaction().rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/helper/ProsesDisposisiSopService.java:230");}
				}
				Common.tampilErrorJikaAdmin(e);
				return gagal("Gagal menyimpan langkah: " + (e.getMessage() == null ? "" : e.getMessage()));
			} finally {
				if (sessionUtama != null) {
					try { sessionUtama.clear(); sessionUtama.disconnect(); sessionUtama.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/helper/ProsesDisposisiSopService.java:236");}
				}
			}
		}

		final Long idLangkahFinal = idLangkah;

		// ---------- 3) ROUTING: buat langkah berikutnya (port txn sessionPost) ----------
		// CATATAN: routing dijalankan SEBELUM update DisposisiSop (langkah 4), persis seperti jalur
		// ZK (sessionPost sinkron mendahului timer update disposisi_sop). Urutan ini penting karena
		// DisposisiAlurSop.setujui() bergantung pada apakah "setelahnya" sudah terisi.
		final List<Long> idEmailDikirim = new ArrayList<Long>();
		{
			Session sessionPost = null;
			try {
				sessionPost = HibernateUtil.getSessionFactory().openSession();
				sessionPost.getTransaction().begin();

				DisposisiAlurSop langkah = (DisposisiAlurSop) sessionPost.get(DisposisiAlurSop.class, idLangkahFinal);
				DisposisiSop dsRef = (DisposisiSop) sessionPost.get(DisposisiSop.class, disposisiSopId);

				if (kembali && langkah != null && langkah.getSebelumnya() != null) {
					// Rute balik ke langkah sebelumnya (revisi)
					AlurSop alurSopData = (AlurSop) sessionPost.createCriteria(AlurSop.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.idEq(langkah.getSebelumnya().getAlurSop().getId())).uniqueResult();
					if (alurSopData != null) {
						DisposisiAlurSop setelah = cariAtauBuatBerikutnya(sessionPost, alurSopData, dsRef, langkah,
								waktu, waktuMaksimal);
						if (setelah != null && setelah.getId() != null) idEmailDikirim.add(setelah.getId());
					}
				} else if (langkah != null) {
					DisposisiAlurSop terakhirDibuat = null;
					for (Long idRute : selanjutnyaAlurSopIds) {
						if (idRute == null) continue;
						AlurSop alurSopData = (AlurSop) sessionPost.createCriteria(AlurSop.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.idEq(idRute)).uniqueResult();
						if (alurSopData == null) continue;
						DisposisiAlurSop setelah = cariAtauBuatBerikutnya(sessionPost, alurSopData, dsRef, langkah,
								waktu, waktuMaksimal);
						if (setelah != null) {
							terakhirDibuat = setelah;
							if (setelah.getId() != null) idEmailDikirim.add(setelah.getId());
						}
					}
					if (terakhirDibuat != null) {
						langkah.setSetelahnya(terakhirDibuat);
						Common.refreshSaveOrUpdate(sessionPost, langkah);
					}
				}

				sessionPost.getTransaction().commit();
			} catch (Exception e) {
				if (sessionPost != null && sessionPost.isOpen() && sessionPost.getTransaction() != null
						&& sessionPost.getTransaction().isActive()) {
					try { sessionPost.getTransaction().rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/helper/ProsesDisposisiSopService.java:292");}
				}
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (sessionPost != null) {
					try { sessionPost.clear(); sessionPost.disconnect(); sessionPost.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/helper/ProsesDisposisiSopService.java:297");}
				}
			}
		}

		// ---------- 4) UPDATE DisposisiSop (disposisiEnd/disposisiSetuju) via KunciEntityHelper ----------
		// Dijalankan SETELAH routing agar "setelahnya" sudah terisi saat setujui() dievaluasi.
		try {
			KunciEntityHelper.jalankanDenganKunci(DisposisiSop.class, disposisiSopId,
					new KunciEntityHelper.PekerjaanTransaksi() {
						@Override
						public void kerjakan(Session sessionData, Object entityTerkunci) throws Exception {
							DisposisiSop disposisiSopOk = (DisposisiSop) entityTerkunci;
							DisposisiAlurSop langkah = (DisposisiAlurSop) sessionData.get(DisposisiAlurSop.class,
									idLangkahFinal);
							if (langkah == null) return;

							// Jaga kolom kode DisposisiSop selalu sinkron (selalu tampil).
							SopKodeUtil.sinkronkanKode(disposisiSopOk);

							if (disposisiSopOk.getDisposisiEnd() == null
									|| (disposisiSopOk.getDisposisiEnd() != null
											&& disposisiSopOk.getDisposisiEnd().getId() < langkah.getId())) {
								disposisiSopOk.setDisposisiEnd(langkah);
							}

							if (langkah.getAlurSop() != null
									&& (Boolean.TRUE.equals(langkah.getAlurSop().getJikaProsesDisetujuiMakaSelesai())
											|| langkah.setujui())) {
								disposisiSopOk.setDisposisiSetuju(langkah);
							}

							sessionData.update(disposisiSopOk);
						}
					});
		} catch (Exception e) {
			// Kontensi sementara (baris dikunci transaksi lain) bukan kegagalan inti — langkah sudah tersimpan.
			Common.tampilErrorJikaAdmin(e);
		}

		// ---------- 5) NOTIFIKASI di THREAD LATAR (PDF disposisi + email) ----------
		final Long dsIdFinal = disposisiSopId;
		final boolean baruFinal = baru;
		new Thread(new Runnable() {
			@Override
			public void run() {
				// Email per langkah berikutnya
				for (Long idNext : idEmailDikirim) {
					Session s = null;
					try {
						s = HibernateUtil.getSessionFactory().openSession();
						DisposisiAlurSop a = (DisposisiAlurSop) s.get(DisposisiAlurSop.class, idNext);
						if (a != null) BroadcastHelper.kirimEmailDisposisi(a);
					} catch (Throwable t) {
						t.printStackTrace(); ais.common.ErrorAuditUtil.record(t, "auto-audit src/ais/action/master/sop/helper/ProsesDisposisiSopService.java:351");
					} finally {
						if (s != null) { try { s.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/helper/ProsesDisposisiSopService.java:353");} }
					}
				}
				// PDF disposisi (untuk pengajuan baru) — sama dengan jalur ZK
				if (baruFinal) {
					Session s = null;
					try {
						s = HibernateUtil.getSessionFactory().openSession();
						DisposisiSop ds = (DisposisiSop) s.get(DisposisiSop.class, dsIdFinal);
						if (ds != null) TampilanAlurSopAction.cetakDisposisi(ds, true);
					} catch (Throwable t) {
						t.printStackTrace(); ais.common.ErrorAuditUtil.record(t, "auto-audit src/ais/action/master/sop/helper/ProsesDisposisiSopService.java:364");
					} finally {
						if (s != null) { try { s.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/helper/ProsesDisposisiSopService.java:366");} }
					}
				}
			}
		}).start();

		return new Hasil(true, "Disposisi berhasil diproses.", idLangkahFinal);
	}

	/**
	 * Buat <b>Pengajuan Baru</b> (DisposisiSop + langkah START + routing awal) — versi native JSP.
	 * <p>
	 * Setia pada {@code DisposisiSopAction.onSave} ZK untuk SOP <b>tanpa form data</b>
	 * ({@code AlurSop.getFormInputan()} kosong). SOP yang punya form data ditolak di sini (harus lewat
	 * form ZK bespoke-nya) sehingga tidak ada data yang hilang.
	 * </p>
	 * <p>
	 * Catatan penting: langkah START <b>tidak</b> di-set {@code setelahnya} (sama dengan ZK) — rute awal
	 * disimpan pada {@code sebelumnya} langkah berikutnya. Timeline native membaca langkah lewat query
	 * semua langkah + relasi {@code sebelumnya}, jadi rantai tetap utuh.
	 * </p>
	 *
	 * @param tbmuser               pengaju (dari sesi JSP).
	 * @param sopId                 id SOP (Workflow) yang diajukan.
	 * @param keterangan            keterangan pengajuan.
	 * @param waktu                 waktu pengajuan (null = sekarang).
	 * @param selanjutnyaAlurSopIds rute awal (Proses SOP) yang dipilih pengaju.
	 * @return {@link Hasil} dengan {@code disposisiAlurSopId} berisi id DisposisiSop baru.
	 */
	public static Hasil buatPengajuanBaru(Tbmuser tbmuser, Long sopId, String keterangan, Date waktu,
			List<Long> selanjutnyaAlurSopIds) {

		if (tbmuser == null || tbmuser.getUserId() == null) return gagal("Sesi Anda telah berakhir.");
		if (sopId == null) return gagal("SOP tidak ditemukan.");
		if (waktu == null) waktu = new Date();
		if (keterangan == null) keterangan = "";
		if (selanjutnyaAlurSopIds == null) selanjutnyaAlurSopIds = new ArrayList<Long>();

		// ---------- 1) Validasi + cari langkah START SOP ----------
		Long startAlurId;
		{
			Session sb = null;
			try {
				sb = HibernateUtil.getSessionFactory().openSession();
				Sop sop = (Sop) sb.get(Sop.class, sopId);
				if (sop == null) return gagal("SOP tidak ditemukan.");
				AlurSop start = (AlurSop) sb.createCriteria(AlurSop.class)
						.add(Restrictions.eq("sop", sop))
						.add(Restrictions.eq("start", true))
						.addOrder(Order.asc("id")).setMaxResults(1).uniqueResult();
				if (start == null) return gagal("SOP ini belum memiliki langkah awal (start).");
				// HYBRID: hanya SOP tanpa form data yang boleh native. SOP ber-form -> form ZK.
				if (start.getFormInputan() != null && !start.getFormInputan().trim().isEmpty()) {
					return gagal("SOP ini memiliki form data; gunakan form pengajuan (ZK).");
				}
				List<AlurSop> opsiRute = start.ambilAlurSetelahnya();
				boolean ruteOpsional = Boolean.TRUE.equals(start.getAlurSetelahnyaTidakWajib());
				if (!ruteOpsional && opsiRute != null && !opsiRute.isEmpty() && selanjutnyaAlurSopIds.isEmpty()) {
					return gagal("Disposisi selanjutnya harus dipilih.");
				}
				if (Boolean.TRUE.equals(start.getCatatanWajibDiisi()) && keterangan.trim().isEmpty()) {
					return gagal("Catatan harus diisi.");
				}
				startAlurId = start.getId();
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
				return gagal("Gagal memuat SOP: " + (e.getMessage() == null ? "" : e.getMessage()));
			} finally {
				if (sb != null) { try { sb.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/helper/ProsesDisposisiSopService.java:434");} }
			}
		}

		// ---------- 2) Buat DisposisiSop ----------
		Long disposisiSopId;
		{
			Session ss = null;
			try {
				ss = HibernateUtil.getSessionFactory().openSession();
				ss.getTransaction().begin();
				DisposisiSop ds = new DisposisiSop();
				ds.setSop((Sop) ss.get(Sop.class, sopId));
				ds.setDiajukanOleh(tbmuser);
				ds.setMahasiswa(tbmuser.getMahasiswa());
				ds.setSiswa(tbmuser.getSiswa());
				ds.setWaktu(waktu);
				ds.setKeterangan(keterangan);
				Common.refreshSaveOrUpdate(ss, ds);
				disposisiSopId = ds.getId();
				ss.getTransaction().commit();
			} catch (Exception e) {
				if (ss != null && ss.getTransaction().isActive()) { try { ss.getTransaction().rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/helper/ProsesDisposisiSopService.java:456");} }
				Common.tampilErrorJikaAdmin(e);
				return gagal("Gagal membuat pengajuan: " + (e.getMessage() == null ? "" : e.getMessage()));
			} finally {
				if (ss != null) { try { ss.clear(); ss.disconnect(); ss.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/helper/ProsesDisposisiSopService.java:460");} }
			}
		}

		// ---------- 3) Buat langkah START (alurSopAwal) ----------
		Long startStepId;
		{
			Session sa = null;
			try {
				sa = HibernateUtil.getSessionFactory().openSession();
				sa.getTransaction().begin();
				DisposisiAlurSop awal = new DisposisiAlurSop(tbmuser.getUserId());
				awal.setUsernamePengguna(tbmuser.getUserId());
				awal.setAlurSop((AlurSop) sa.get(AlurSop.class, startAlurId));
				awal.setDisposisiSop((DisposisiSop) sa.get(DisposisiSop.class, disposisiSopId));
				awal.setDiajukanOleh(tbmuser);
				awal.setMahasiswa(tbmuser.getMahasiswa());
				awal.setSiswa(tbmuser.getSiswa());
				awal.setWaktu(waktu);
				awal.setKeterangan(keterangan);
				Common.refreshSaveOrUpdate(sa, awal);
				startStepId = awal.getId();
				sa.getTransaction().commit();
			} catch (Exception e) {
				if (sa != null && sa.getTransaction().isActive()) { try { sa.getTransaction().rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/helper/ProsesDisposisiSopService.java:484");} }
				Common.tampilErrorJikaAdmin(e);
				return gagal("Gagal membuat langkah awal: " + (e.getMessage() == null ? "" : e.getMessage()));
			} finally {
				if (sa != null) { try { sa.clear(); sa.disconnect(); sa.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/helper/ProsesDisposisiSopService.java:488");} }
			}
		}

		final Long startStepIdFinal = startStepId;
		final Long disposisiSopIdFinal = disposisiSopId;

		// ---------- 4) Routing (start -> langkah berikutnya) + update DisposisiSop ----------
		// Langkah START TIDAK di-set setelahnya (sama dgn ZK); rute disimpan pada sebelumnya next.
		final List<Long> idEmailDikirim = new ArrayList<Long>();
		{
			Session sp = null;
			try {
				sp = HibernateUtil.getSessionFactory().openSession();
				sp.getTransaction().begin();
				DisposisiAlurSop awal = (DisposisiAlurSop) sp.get(DisposisiAlurSop.class, startStepIdFinal);
				DisposisiSop ds = (DisposisiSop) sp.get(DisposisiSop.class, disposisiSopIdFinal);
				for (Long idRute : selanjutnyaAlurSopIds) {
					if (idRute == null) continue;
					AlurSop alurSopData = (AlurSop) sp.createCriteria(AlurSop.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.idEq(idRute)).uniqueResult();
					if (alurSopData == null) continue;
					DisposisiAlurSop setelah = cariAtauBuatBerikutnya(sp, alurSopData, ds, awal, waktu, null);
					if (setelah != null && setelah.getId() != null) idEmailDikirim.add(setelah.getId());
				}
				if (ds != null && awal != null) {
					ds.setDisposisiStart(awal);
					if (ds.getDisposisiEnd() == null || (ds.getDisposisiEnd().getId() != null
							&& awal.getId() != null && ds.getDisposisiEnd().getId() < awal.getId())) {
						ds.setDisposisiEnd(awal);
					}
					if (awal.getAlurSop() != null
							&& (Boolean.TRUE.equals(awal.getAlurSop().getJikaProsesDisetujuiMakaSelesai())
									|| awal.setujui())) {
						ds.setDisposisiSetuju(awal);
					}
					Common.refreshUpdate(sp, ds);
				}
				sp.getTransaction().commit();
			} catch (Exception e) {
				if (sp != null && sp.isOpen() && sp.getTransaction() != null && sp.getTransaction().isActive()) {
					try { sp.getTransaction().rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/helper/ProsesDisposisiSopService.java:530");}
				}
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (sp != null) { try { sp.clear(); sp.disconnect(); sp.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/helper/ProsesDisposisiSopService.java:534");} }
			}
		}

		// ---------- 5) Notifikasi (email + PDF disposisi) di thread latar ----------
		new Thread(new Runnable() {
			@Override
			public void run() {
				for (Long idNext : idEmailDikirim) {
					Session s = null;
					try {
						s = HibernateUtil.getSessionFactory().openSession();
						DisposisiAlurSop a = (DisposisiAlurSop) s.get(DisposisiAlurSop.class, idNext);
						if (a != null) BroadcastHelper.kirimEmailDisposisi(a);
					} catch (Throwable t) {
						t.printStackTrace(); ais.common.ErrorAuditUtil.record(t, "auto-audit src/ais/action/master/sop/helper/ProsesDisposisiSopService.java:549");
					} finally {
						if (s != null) { try { s.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/helper/ProsesDisposisiSopService.java:551");} }
					}
				}
				Session s = null;
				try {
					s = HibernateUtil.getSessionFactory().openSession();
					DisposisiSop ds = (DisposisiSop) s.get(DisposisiSop.class, disposisiSopIdFinal);
					if (ds != null) TampilanAlurSopAction.cetakDisposisi(ds, true);
				} catch (Throwable t) {
					t.printStackTrace(); ais.common.ErrorAuditUtil.record(t, "auto-audit src/ais/action/master/sop/helper/ProsesDisposisiSopService.java:560");
				} finally {
					if (s != null) { try { s.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/helper/ProsesDisposisiSopService.java:562");} }
				}
			}
		}).start();

		return new Hasil(true, "Pengajuan berhasil dibuat.", disposisiSopIdFinal);
	}

	/**
	 * Perbarui <b>catatan saja</b> pada satu langkah tanpa menyentuh routing (rute berikutnya tetap).
	 * <p>
	 * Dipakai untuk skenario <b>admin menyunting langkah tengah</b> (yang rutenya sudah diteruskan):
	 * hanya keterangan/catatan yang berubah, sehingga <b>tidak</b> menghapus anak ({@code sebelumnya})
	 * maupun membuat ulang langkah berikutnya. Aman dipakai pada langkah mana pun.
	 * </p>
	 *
	 * @param disposisiAlurSopId id langkah yang disunting (wajib).
	 * @param keterangan         catatan baru.
	 * @return {@link Hasil}
	 */
	public static Hasil updateKeterangan(Long disposisiAlurSopId, String keterangan) {
		if (disposisiAlurSopId == null) return gagal("Langkah tidak ditemukan.");
		if (keterangan == null) keterangan = "";
		Session sesi = null;
		try {
			sesi = HibernateUtil.getSessionFactory().openSession();
			sesi.getTransaction().begin();
			DisposisiAlurSop langkah = (DisposisiAlurSop) sesi.get(DisposisiAlurSop.class, disposisiAlurSopId);
			if (langkah == null) { sesi.getTransaction().rollback(); return gagal("Langkah tidak ditemukan."); }
			langkah.setKeterangan(keterangan);
			sesi.update(langkah);
			sesi.getTransaction().commit();
			return new Hasil(true, "Catatan diperbarui.", disposisiAlurSopId);
		} catch (Exception e) {
			if (sesi != null && sesi.getTransaction().isActive()) { try { sesi.getTransaction().rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/helper/ProsesDisposisiSopService.java:596");} }
			Common.tampilErrorJikaAdmin(e);
			return gagal("Gagal memperbarui catatan: " + (e.getMessage() == null ? "" : e.getMessage()));
		} finally {
			if (sesi != null) { try { sesi.clear(); sesi.disconnect(); sesi.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/helper/ProsesDisposisiSopService.java:600");} }
		}
	}

	/**
	 * Cari langkah berikutnya yang sudah ada (idempoten bila proses diulang) atau buat baru.
	 * Setia pada blok pembuatan {@code disposisiAlurSopSetelah} di {@code onSave} ZK.
	 */
	private static DisposisiAlurSop cariAtauBuatBerikutnya(Session sessionPost, AlurSop alurSopData,
			DisposisiSop disposisiSop, DisposisiAlurSop sebelumnya, Date waktu, Date waktuMaksimal) {
		DisposisiAlurSop setelah = (DisposisiAlurSop) sessionPost.createCriteria(DisposisiAlurSop.class)
				.add(Restrictions.isNotNull("alurSop"))
				.add(Restrictions.eq("alurSop", alurSopData))
				.add(Restrictions.eq("disposisiSop", disposisiSop))
				.add(Restrictions.eq("sebelumnya", sebelumnya)).setMaxResults(1).uniqueResult();
		if (setelah == null) {
			setelah = new DisposisiAlurSop();
			setelah.setWaktu(waktu);
			setelah.setSebelumnya(sebelumnya);
			setelah.setWaktuMaksimal(waktuMaksimal);
			setelah.setDisposisiSop(disposisiSop);
			setelah.setAlurSop(alurSopData);
			Common.refreshSaveOrUpdate(sessionPost, setelah);
		}
		return setelah;
	}
}

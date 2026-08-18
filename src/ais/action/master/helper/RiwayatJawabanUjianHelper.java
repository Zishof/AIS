package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankSoal;
import ais.database.model.BankSoalDetail;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.HasilUjianMahasiswaDetail;
import ais.ui.util.MyArrayList;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h2>Riwayat Jawaban Peserta Ujian</h2>
 *
 * <p><b>Tujuan (untuk pengguna).</b> Menampilkan riwayat jawaban seorang peserta ujian untuk
 * SETIAP soal — baik soal pilihan ganda maupun essay — beserta jawaban terbaru yang pernah ia
 * berikan (revisi paling akhir yang benar-benar dijawab). Berguna terutama ketika peserta
 * melakukan "ujian ulang" sehingga jawaban di layar tampak kosong padahal sebelumnya sudah
 * pernah diisi: melalui layar ini jawaban lama dapat dilihat kembali dan, bila perlu,
 * dikembalikan (restore) ke jawaban terbaru yang pernah diisi.</p>
 *
 * <h3>Fitur</h3>
 * <ul>
 *   <li><b>Daftar per soal:</b> menampilkan soal, jenis (Pilihan Ganda / Essay), jawaban yang
 *       tersimpan saat ini, dan jawaban terbaru dari riwayat perubahan.</li>
 *   <li><b>Filter "Hanya yang sudah dijawab":</b> menyembunyikan soal yang tidak pernah dijawab
 *       (baik sekarang maupun di riwayat), sehingga daftar lebih ringkas.</li>
 *   <li><b>Tombol "Restore ke Jawaban Terbaru":</b> untuk setiap soal yang jawaban saat ini
 *       KOSONG namun di riwayat pernah dijawab, jawaban dikembalikan ke revisi terakhir yang
 *       dijawab. Soal yang memang tidak pernah dijawab (essay maupun pilihan ganda) DIABAIKAN,
 *       dan soal yang jawabannya sudah terisi tidak diubah. Setelah restore, nilai peserta
 *       dihitung ulang otomatis.</li>
 * </ul>
 *
 * <h3>Sumber data &amp; keamanan</h3>
 * <p>Riwayat diambil dari audit Hibernate Envers atas entitas {@link HasilUjianMahasiswaDetail}
 * (beranotasi {@code @Audited}). Setiap baris jawaban peserta memiliki jejak revisi otomatis;
 * kelas ini membaca revisi tersebut (terurut dari yang terbaru) lalu memilih revisi terbaru yang
 * "terjawab". Proses restore HANYA menyalin nilai dari salah satu revisi milik baris jawaban itu
 * sendiri — tidak pernah mengambil dari data peserta/soal lain — sehingga aman: pada kondisi
 * terburuk (mis. baris jawaban tidak memiliki riwayat terjawab) restore tidak melakukan apa pun.
 * Karena {@link HasilUjianMahasiswaDetail} diaudit, perubahan restore pun otomatis tercatat pada
 * riwayat sehingga tetap dapat ditelusuri/dikembalikan lewat tombol "History".</p>
 *
 * <h3>Manajemen sesi Hibernate</h3>
 * <p>Pemuatan riwayat memakai sesi terdedikasi ({@code HibernateUtil.openSession()}) yang selalu
 * ditutup pada blok {@code finally}. Proses restore memakai sesi + transaksi terdedikasi yang
 * juga ditutup pada {@code finally} (rollback bila gagal). Kompatibel Java 1.7 dan pola
 * try/catch 1.6 (tanpa multi-catch / try-with-resources).</p>
 *
 * @author AIS
 */
public class RiwayatJawabanUjianHelper {

	/** Satu baris tampilan = satu soal + jawaban sekarang + jawaban terbaru dari riwayat. */
	private static class BarisRiwayat {
		private Long detailId;
		private int nomor;
		private String soal;
		private String jenis;
		private boolean terjawabSekarang;
		private String ringkasSekarang;
		private boolean adaRiwayatTerjawab;
		private String ringkasTerbaru;
		private String jawabanTerbaru;
		private Long bankSoalDetailTerbaruId;
		private Date waktuTerbaru;
	}

	private RiwayatJawabanUjianHelper() {
	}

	/**
	 * Membuka jendela modal "Riwayat Jawaban" untuk seorang peserta ujian.
	 *
	 * @param hasilUjianMahasiswa hasil ujian peserta yang diperiksa (tidak boleh {@code null}).
	 * @param onRestored          listener opsional yang dipanggil setelah restore berhasil (mis.
	 *                            untuk me-refresh tampilan induk); boleh {@code null}.
	 */
	public static void buka(final HasilUjianMahasiswa hasilUjianMahasiswa, final EventListener onRestored)
			throws Exception {
		if (hasilUjianMahasiswa == null || hasilUjianMahasiswa.getId() == null) {
			MyMessageboxConfig.show("Data hasil ujian tidak ditemukan.", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		final MyWindow window = new MyWindow("Riwayat Jawaban Peserta — " + hasilUjianMahasiswa.toString(), "none",
				true);
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.setWidth("92%");
		window.setHeight("92%");

		MyBorderlayout borderlayout = new MyBorderlayout();
		borderlayout.setParent(window);

		North north = new North();
		north.setBorder("none");
		north.setParent(borderlayout);
		final Toolbar toolbar = new Toolbar();
		toolbar.setParent(north);

		final Center center = new Center();
		center.setBorder("none");
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		final MyCheckboxConfig hanyaTerjawab = new MyCheckboxConfig("Hanya yang sudah dijawab");
		hanyaTerjawab.setChecked(true);

		final MyToolbarbuttonConfig restore = new MyToolbarbuttonConfig("Restore ke Jawaban Terbaru",
				"/img/refresh.gif");
		restore.setTooltiptext(
				"Kembalikan jawaban yang saat ini kosong ke revisi terakhir yang pernah dijawab (soal tak pernah dijawab diabaikan)");

		MyToolbarbuttonConfig tutup = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		tutup.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});

		toolbar.appendChild(tutup);
		toolbar.appendChild(hanyaTerjawab);
		toolbar.appendChild(restore);

		/*
		 * Muat data dipisah ke Timer agar jendela tampil dulu (riwayat bisa berisi puluhan soal
		 * dengan satu kueri audit per soal, sehingga sedikit memakan waktu).
		 */
		final List<BarisRiwayat> semuaBaris = new ArrayList<BarisRiwayat>();

		final EventListener render = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.clear(center);
				Grid grid = new Grid();
				grid.setSclass("dgrid");
				grid.setWidth("100%");
				grid.setHeight("100%");
				grid.setParent(center);

				Columns columns = new Columns();
				columns.setParent(grid);
				new Column("No.", null, "45px").setParent(columns);
				new Column("Soal").setParent(columns);
				new Column("Jenis", null, "110px").setParent(columns);
				new Column("Jawaban Sekarang", null, "24%").setParent(columns);
				new Column("Jawaban Terbaru (Riwayat)", null, "24%").setParent(columns);
				new Column("Status", null, "150px").setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				boolean hanya = hanyaTerjawab.isChecked();
				int tampil = 0;
				for (int i = 0; i < semuaBaris.size(); i++) {
					BarisRiwayat b = semuaBaris.get(i);
					boolean pernahDijawab = b.terjawabSekarang || b.adaRiwayatTerjawab;
					if (hanya && !pernahDijawab) {
						continue;
					}
					tampil++;

					Row row = new Row();
					row.setValign("top");
					row.setParent(rows);

					new Label(String.valueOf(b.nomor)).setParent(row);
					new Html("<div style='white-space:normal;'>" + esc(b.soal) + "</div>")
							.setParent(row);
					new Label(b.jenis).setParent(row);

					Html now = new Html("<div style='white-space:normal;"
							+ (b.terjawabSekarang ? "" : "color:#b91c1c;") + "'>"
							+ esc(b.ringkasSekarang) + "</div>");
					now.setParent(row);

					Html last = new Html("<div style='white-space:normal;"
							+ (b.adaRiwayatTerjawab ? "color:#166534;" : "color:#94a3b8;") + "'>"
							+ esc(b.ringkasTerbaru) + "</div>");
					last.setParent(row);

					String status;
					String warna;
					if (b.terjawabSekarang) {
						status = "Sudah terisi";
						warna = "#334155";
					} else if (b.adaRiwayatTerjawab) {
						status = "Akan direstore";
						warna = "#166534";
					} else {
						status = "Tak pernah dijawab";
						warna = "#94a3b8";
					}
					new Html("<span style='font-weight:700;color:" + warna + ";'>" + status + "</span>")
							.setParent(row);
				}

				if (tampil == 0) {
					Row row = new Row();
					row.setParent(rows);
					Label kosong = new Label(hanya
							? "Belum ada satu pun soal yang pernah dijawab oleh peserta ini."
							: "Tidak ada data soal.");
					kosong.setParent(row);
				}
			}
		};

		hanyaTerjawab.addEventListener("onCheck", render);

		restore.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				int dapatDirestore = 0;
				for (int i = 0; i < semuaBaris.size(); i++) {
					BarisRiwayat b = semuaBaris.get(i);
					if (!b.terjawabSekarang && b.adaRiwayatTerjawab) {
						dapatDirestore++;
					}
				}
				if (dapatDirestore == 0) {
					MyMessageboxConfig.show(
							"Tidak ada jawaban yang perlu dikembalikan. Semua soal yang pernah dijawab jawabannya sudah terisi.",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}
				final int jumlah = dapatDirestore;
				MyMessageboxConfig.show(
						"Kembalikan " + jumlah
								+ " jawaban yang saat ini kosong ke revisi terakhir yang pernah dijawab? "
								+ "Soal yang tidak pernah dijawab diabaikan. Perubahan tetap tercatat pada riwayat.",
						"Konfirmasi Restore", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
						MyMessageboxConfig.QUESTION, new EventListener() {
							@Override
							public void onEvent(Event ev) throws Exception {
								if (Integer.parseInt(ev.getData().toString()) != MyMessageboxConfig.OK) {
									return;
								}
								int diproses = lakukanRestore(hasilUjianMahasiswa, semuaBaris);
								// Muat ulang data agar kolom "sekarang" mengikuti hasil restore.
								semuaBaris.clear();
								semuaBaris.addAll(muatBaris(hasilUjianMahasiswa));
								render.onEvent(null);
								if (onRestored != null) {
									onRestored.onEvent(new Event("", null, hasilUjianMahasiswa));
								}
								MyMessageboxConfig.show(diproses + " jawaban berhasil dikembalikan dan nilai dihitung ulang.",
										"Selesai", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							}
						});
			}
		});

		window.setVisible(true);
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				semuaBaris.clear();
				semuaBaris.addAll(muatBaris(hasilUjianMahasiswa));
				render.onEvent(null);
			}
		});

		window.onModal();
	}

	/**
	 * Memuat daftar {@link BarisRiwayat} untuk seluruh soal milik peserta: jawaban saat ini serta
	 * revisi terbaru yang terjawab (dari audit Envers). Memakai sesi terdedikasi yang ditutup di
	 * {@code finally}.
	 *
	 * @param hum hasil ujian peserta.
	 * @return daftar baris (satu per {@link HasilUjianMahasiswaDetail}), terurut naik menurut id.
	 */
	@SuppressWarnings("unchecked")
	private static List<BarisRiwayat> muatBaris(HasilUjianMahasiswa hum) {
		List<BarisRiwayat> hasil = new ArrayList<BarisRiwayat>();
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			AuditReader reader = AuditReaderFactory.get(session);

			List<HasilUjianMahasiswaDetail> details = session.createCriteria(HasilUjianMahasiswaDetail.class)
					.add(Restrictions.eq("hasilUjianMahasiswa", hum)).addOrder(Order.asc("id")).list();

			int nomor = 0;
			for (int i = 0; i < details.size(); i++) {
				HasilUjianMahasiswaDetail detail = details.get(i);
				nomor++;

				BarisRiwayat b = new BarisRiwayat();
				b.detailId = detail.getId();
				b.nomor = nomor;

				BankSoal bankSoal = null;
				try {
					bankSoal = detail.getBankSoal();
				} catch (Exception e) {
					bankSoal = null;
				}
				b.soal = potong(teksSoal(bankSoal), 140);
				b.jenis = (bankSoal != null && BankSoal.PILIHAN_GANDA.equals(bankSoal.getJenis())) ? "Pilihan Ganda"
						: "Essay";

				b.terjawabSekarang = terjawab(detail);
				b.ringkasSekarang = ringkas(session, detail.getJawaban(), idBankSoalDetail(detail));

				// Cari revisi TERBARU yang terjawab dari audit Envers.
				HasilUjianMahasiswaDetail revTerjawab = ambilRevisiTerbaruTerjawab(reader, detail.getId());
				if (revTerjawab != null) {
					b.adaRiwayatTerjawab = true;
					b.jawabanTerbaru = revTerjawab.getJawaban();
					b.bankSoalDetailTerbaruId = idBankSoalDetail(revTerjawab);
					try {
						b.waktuTerbaru = revTerjawab.getTanggal_dirubah();
					} catch (Exception e) {
						b.waktuTerbaru = null;
					}
					b.ringkasTerbaru = ringkas(session, b.jawabanTerbaru, b.bankSoalDetailTerbaruId);
				} else {
					b.adaRiwayatTerjawab = false;
					b.ringkasTerbaru = "(tidak pernah dijawab)";
				}

				hasil.add(b);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"memuat riwayat jawaban ujian mahasiswa",
					e, new String[] {
							"Muat ulang (refresh) halaman ini lalu coba kembali.",
							"Periksa apakah data hasil ujian mahasiswa terkait masih tersedia.",
							"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
					});
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RiwayatJawabanUjianHelper.java:369");
				}
			}
		}
		return hasil;
	}

	/**
	 * Mengambil snapshot revisi TERBARU yang "terjawab" dari audit Envers untuk satu id
	 * {@link HasilUjianMahasiswaDetail}. Revisi diurutkan dari nomor terbesar; snapshot pertama
	 * yang terjawab dikembalikan. Bila tidak ada yang terjawab, mengembalikan {@code null}.
	 *
	 * @param reader   AuditReader aktif dari sesi pemuat.
	 * @param detailId id baris jawaban.
	 * @return snapshot terjawab paling baru, atau {@code null}.
	 */
	@SuppressWarnings("unchecked")
	private static HasilUjianMahasiswaDetail ambilRevisiTerbaruTerjawab(AuditReader reader, Long detailId) {
		if (reader == null || detailId == null) {
			return null;
		}
		try {
			AuditQuery query = reader.createQuery()
					.forRevisionsOfEntity(HasilUjianMahasiswaDetail.class, true, true)
					.add(AuditEntity.id().eq(detailId)).addOrder(AuditEntity.revisionNumber().desc());
			List<HasilUjianMahasiswaDetail> revisi = query.getResultList();
			for (int i = 0; i < revisi.size(); i++) {
				HasilUjianMahasiswaDetail snap = revisi.get(i);
				if (terjawab(snap)) {
					return snap;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RiwayatJawabanUjianHelper.java:401");
			// Audit bisa saja belum tersedia untuk id tertentu → anggap tak ada riwayat.
		}
		return null;
	}

	/**
	 * Menjalankan restore: untuk setiap baris yang jawaban SEKARANG kosong namun punya revisi
	 * terjawab, salin jawaban revisi terbaru ke baris jawaban saat ini. Lalu hitung ulang nilai
	 * peserta. Dibungkus satu transaksi terdedikasi; ditutup di {@code finally}.
	 *
	 * @param hum        hasil ujian peserta.
	 * @param semuaBaris daftar baris (hasil {@link #muatBaris}).
	 * @return jumlah baris yang benar-benar direstore.
	 */
	private static int lakukanRestore(HasilUjianMahasiswa hum, List<BarisRiwayat> semuaBaris) {
		int diproses = 0;
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();

			for (int i = 0; i < semuaBaris.size(); i++) {
				BarisRiwayat b = semuaBaris.get(i);
				if (b.terjawabSekarang || !b.adaRiwayatTerjawab || b.detailId == null) {
					continue; // sudah terisi / tak pernah dijawab → diabaikan
				}
				HasilUjianMahasiswaDetail detail = (HasilUjianMahasiswaDetail) session
						.get(HasilUjianMahasiswaDetail.class, b.detailId);
				if (detail == null) {
					continue;
				}
				detail.setJawaban(b.jawabanTerbaru);
				if (b.bankSoalDetailTerbaruId != null) {
					BankSoalDetail bsd = (BankSoalDetail) session.get(BankSoalDetail.class,
							b.bankSoalDetailTerbaruId);
					detail.setBankSoalDetail(bsd);
				} else {
					detail.setBankSoalDetail(null);
				}
				session.update(detail);
				diproses++;
			}

			if (diproses > 0) {
				// Hitung ulang nilai peserta setelah jawaban dikembalikan.
				HasilUjianMahasiswa humRefresh = (HasilUjianMahasiswa) session.get(HasilUjianMahasiswa.class,
						hum.getId());
				if (humRefresh != null && humRefresh.getPertemuanPunyaUjian() != null) {
					MyArrayList<Long> ujianPunyaSoals = humRefresh.ambilUjianPunyaSoals(
							humRefresh.getPertemuanPunyaUjian().getJmlDitampilkan(), new Label(), true);
					Map<Long, Set<Long>> detailMap = humRefresh.ambilHasilUjianMahasiswaDetail(
							humRefresh.getPertemuanPunyaUjian().getJmlDitampilkan(), ujianPunyaSoals, true);
					ProsesUjianHelper.hitungPilihanGanda(humRefresh, detailMap);
					session.update(humRefresh);
				}
			}

			tx.commit();
		} catch (Exception e) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/RiwayatJawabanUjianHelper.java:465");
				}
			}
			Common.tampilErrorJikaAdmin(e);
			diproses = 0;
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RiwayatJawabanUjianHelper.java:474");
				}
			}
		}
		return diproses;
	}

	/**
	 * Apakah sebuah baris jawaban dianggap "terjawab": ada pilihan yang dipilih (pilihan ganda)
	 * ATAU ada teks jawaban tidak kosong (essay/pilihan ganda berbasis huruf).
	 */
	private static boolean terjawab(HasilUjianMahasiswaDetail detail) {
		if (detail == null) {
			return false;
		}
		if (idBankSoalDetail(detail) != null) {
			return true;
		}
		String jwb = detail.getJawaban();
		return jwb != null && jwb.trim().length() > 0;
	}

	/** Id {@link BankSoalDetail} yang dipilih pada baris jawaban, atau {@code null} bila aman gagal. */
	private static Long idBankSoalDetail(HasilUjianMahasiswaDetail detail) {
		try {
			BankSoalDetail bsd = detail.getBankSoalDetail();
			return bsd == null ? null : bsd.getId();
		} catch (Exception e) {
			return null;
		}
	}

	/** Ringkasan tampilan jawaban: pilihan (huruf + teks opsi) atau teks essay, atau penanda kosong. */
	private static String ringkas(Session session, String jawaban, Long bankSoalDetailId) {
		try {
			if (bankSoalDetailId != null && session != null) {
				BankSoalDetail bsd = (BankSoalDetail) session.get(BankSoalDetail.class, bankSoalDetailId);
				if (bsd != null) {
					String huruf = bsd.getHuruf() == null ? "" : bsd.getHuruf();
					String opsi = potong(teksBersih(bsd.getJawaban()), 90);
					return (huruf.isEmpty() ? "" : huruf + ". ") + opsi;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RiwayatJawabanUjianHelper.java:517");
		}
		String teks = teksBersih(jawaban);
		if (teks.trim().length() == 0) {
			return "(tidak dijawab)";
		}
		return potong(teks, 160);
	}

	/** Ambil teks soal (di-strip dari HTML). */
	private static String teksSoal(BankSoal bankSoal) {
		if (bankSoal == null) {
			return "(soal tidak ditemukan)";
		}
		try {
			return teksBersih(bankSoal.getSoal());
		} catch (Exception e) {
			return "(soal tidak ditemukan)";
		}
	}

	/** Buang tag HTML dari sebuah string, aman terhadap null. */
	private static String teksBersih(String html) {
		if (html == null) {
			return "";
		}
		try {
			return org.jsoup.Jsoup.parse(html).text();
		} catch (Exception e) {
			return html;
		}
	}

	/** Escape karakter HTML dasar agar teks aman ditampilkan di dalam komponen Html. */
	private static String esc(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	/** Potong string ke panjang maksimum dengan penanda elipsis. */
	private static String potong(String teks, int maks) {
		if (teks == null) {
			return "";
		}
		String t = teks.trim();
		if (t.length() <= maks) {
			return t;
		}
		return t.substring(0, maks - 1) + "…";
	}
}

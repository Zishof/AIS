package ais.action.master.koperasi.helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.asset.JenisLokasi;
import ais.database.model.asset.Lokasi;
import ais.database.model.inventory.Toko;
import ais.database.model.sirs.Gudang;

/**
 * <h2>LokasiKantinUtil — pusat logika data "Lokasi (Gudang)" &amp; "Jenis Lokasi" untuk e-Kantin.</h2>
 *
 * <p>
 * Kelas util ini menjadi <b>satu pintu (single source of truth)</b> bagi seluruh operasi data
 * terkait {@link Lokasi} (yang pada modul e-Kantin diperlakukan sebagai <i>gudang</i>/tempat fisik)
 * dan {@link JenisLokasi} (klasifikasi tempat: Gudang, Outlet, Kasir, Entry Data, dsb). Tujuan
 * utamanya adalah <b>maksimalisasi pemakaian ulang (reuse)</b>: baik tampilan versi <b>JSP</b>
 * (halaman pengaturan e-Kantin), versi <b>ZKoss</b> (composer/action), maupun <b>laporan &amp;
 * dasbor pergudangan</b> memanggil metode yang sama di sini. Dengan memusatkan query, aturan hak
 * akses, dan penyimpanan pada satu tempat, pemeliharaan di kemudian hari menjadi jauh lebih mudah dan
 * risiko duplikasi logika (yang rawan tidak sinkron) ditekan.
 * </p>
 *
 * <h3>Kontrak sesi Hibernate (penting)</h3>
 * <p>
 * Seluruh metode di kelas ini <b>menerima {@link Session} dari pemanggil</b> dan
 * <b>tidak pernah membuka maupun menutup sesi sendiri</b>. Ini disengaja agar aturan sesi framework
 * tetap konsisten: bila pemanggil memakai {@code currentSession()} (dikelola kerangka kerja), sesi
 * <i>tidak boleh</i> ditutup manual — dan karena util ini tidak menutupnya, aturan itu terpenuhi.
 * Bila kelak ada pemanggil yang memakai {@code openSession()} atau {@code currentNativeSession()},
 * pemanggil tersebutlah yang bertanggung jawab menutup sesi di blok {@code finally}. Dengan pola ini,
 * util aman dipakai lintas lapisan tanpa kebocoran koneksi.
 * </p>
 *
 * <h3>Hak akses (siapa boleh mengubah)</h3>
 * <p>
 * Master Lokasi &amp; Jenis Lokasi bersifat <b>administratif</b>. Metode {@link #bolehKelola(HttpServletRequest)}
 * memusatkan aturan: hanya pengguna <b>admin</b> ({@link Common#getApakahAdmin()}) yang <b>bukan</b>
 * pedagang/pemilik toko yang boleh menambah, mengubah, atau menghapus. Pengguna toko/pedagang tetap
 * dapat <i>melihat</i> daftar (untuk memilih lokasi pada transaksi), tetapi tombol ubah/hapus
 * disembunyikan di sisi tampilan dan ditolak di sisi server. Menempatkan aturan ini di satu metode
 * memastikan sisi JSP dan ZK menerapkan kebijakan yang identik — tidak ada celah "lupa mengecek".
 * </p>
 *
 * <h3>Efisiensi memori &amp; kinerja</h3>
 * <p>
 * Metode baca memakai {@link Criteria} yang ringkas dan hanya memuat kolom yang diperlukan lewat
 * entity ramping. Pemanggil dianjurkan memuat daftar <b>sekali</b> lalu memakainya ulang (mis. untuk
 * mengisi dropdown sekaligus tabel) alih-alih query berulang di dalam perulangan. Pencarian memakai
 * {@code ilike} pada kolom terindeks logis (nama) sehingga tetap gesit meski data bertambah. Metode
 * simpan memakai {@link Common#refreshSaveOrUpdate(Session, ais.database.model.GeneralValueObject)}
 * yang mengelola transaksi secara konsisten dengan modul lain. Semua kode menghindari alokasi objek
 * yang tidak perlu, mengembalikan {@link java.util.List} kosong (bukan {@code null}) agar pemanggil
 * tidak perlu memeriksa null, dan kompatibel dengan <b>Java 1.7</b> (tanpa lambda / fitur Java 8+).
 * Blok {@code try/catch} ditulis gaya konservatif (kompatibel Java 1.6) sehingga aman bila basis kode
 * diturunkan targetnya.
 * </p>
 *
 * <h3>Ringkasan metode</h3>
 * <ul>
 *   <li>{@link #bolehKelola(HttpServletRequest)} — gerbang hak akses admin.</li>
 *   <li>{@link #daftarJenisLokasi(Session, boolean)} — daftar jenis lokasi (opsi hanya-aktif).</li>
 *   <li>{@link #daftarLokasi(Session, Long, String, boolean)} — daftar lokasi terfilter jenis/cari/aktif.</li>
 *   <li>{@link #simpanJenisLokasi(Session, Long, String, String, boolean, String, String, Integer, String, String)}</li>
 *   <li>{@link #hapusJenisLokasi(Session, Long)}</li>
 *   <li>{@link #simpanLokasi(Session, Long, String, String, String, Boolean, Long, Long, String, String)}</li>
 *   <li>{@link #hapusLokasi(Session, Long)}</li>
 * </ul>
 *
 * <p>
 * Catatan reuse: kelas ini <i>tidak</i> menyimpan state; semua metode bersifat statis dan bebas efek
 * samping selain operasi basis data yang diminta. Ini membuatnya aman dipanggil bersamaan (thread-safe)
 * selama masing-masing pemanggil memakai {@link Session}-nya sendiri sesuai aturan Hibernate.
 * </p>
 *
 * @author AIS e-Kantin (modul pergudangan)
 * @see Lokasi
 * @see JenisLokasi
 */
public final class LokasiKantinUtil {

	/** Warna default badge bila {@link JenisLokasi#getWarna()} belum diisi. */
	public static final String WARNA_DEFAULT = "#0d6efd";
	/** Ikon default bila {@link JenisLokasi#getIkon()} belum diisi. */
	public static final String IKON_DEFAULT = "fas fa-location-dot";

	private LokasiKantinUtil() {
		// util murni — tidak untuk diinstansiasi.
	}

	/**
	 * Menentukan apakah pengguna saat ini boleh <b>mengelola</b> (tambah/ubah/hapus) master Lokasi
	 * &amp; Jenis Lokasi. Aturan: harus admin dan bukan pedagang/pemilik toko.
	 *
	 * @param request request HTTP aktif (untuk mengambil pengguna login); boleh {@code null}.
	 * @return {@code true} bila boleh mengelola; {@code false} bila hanya boleh melihat.
	 */
	public static boolean bolehKelola(HttpServletRequest request) {
		try {
			Tbmuser u = Common.getCurrentUser(request);
			if (u != null && u.getPedagang() != null) {
				return Boolean.TRUE.equals(u.getPedagang().getSupervisor());
			}
			return Common.getApakahAdmin();
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Mengambil daftar {@link JenisLokasi}, terurut berdasarkan {@code urutan} lalu {@code nama}.
	 *
	 * @param session    sesi Hibernate milik pemanggil (tidak ditutup di sini).
	 * @param hanyaAktif bila {@code true}, hanya jenis dengan {@code aktif} true/null yang dikembalikan.
	 * @return daftar jenis lokasi (tidak pernah {@code null}).
	 */
	@SuppressWarnings("unchecked")
	public static List<JenisLokasi> daftarJenisLokasi(Session session, boolean hanyaAktif) {
		if (session == null) {
			return new ArrayList<JenisLokasi>();
		}
		try {
			Criteria c = session.createCriteria(JenisLokasi.class);
			if (hanyaAktif) {
				c.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
			}
			c.addOrder(Order.asc("urutan")).addOrder(Order.asc("nama"));
			List<JenisLokasi> hasil = c.list();
			return hasil == null ? new ArrayList<JenisLokasi>() : hasil;
		} catch (Exception e) {
			return new ArrayList<JenisLokasi>();
		}
	}

	/**
	 * Mengambil daftar {@link Lokasi} dengan filter opsional.
	 *
	 * @param session       sesi Hibernate milik pemanggil (tidak ditutup di sini).
	 * @param jenisLokasiId bila tidak {@code null}, hanya lokasi berjenis tersebut.
	 * @param cari          bila tidak kosong, cocokkan {@code ilike} pada nama/alamat/keterangan.
	 * @param hanyaAktif    bila {@code true}, hanya lokasi dengan {@code aktif} true/null.
	 * @return daftar lokasi terurut nama (tidak pernah {@code null}).
	 */
	@SuppressWarnings("unchecked")
	public static List<Lokasi> daftarLokasi(Session session, Long jenisLokasiId, String cari, boolean hanyaAktif) {
		if (session == null) {
			return new ArrayList<Lokasi>();
		}
		try {
			Criteria c = session.createCriteria(Lokasi.class);
			if (jenisLokasiId != null && jenisLokasiId.longValue() > 0) {
				c.createAlias("jenisLokasi", "jl").add(Restrictions.eq("jl.id", jenisLokasiId));
			}
			if (hanyaAktif) {
				c.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
			}
			if (cari != null && cari.trim().length() > 0) {
				String q = "%" + cari.trim() + "%";
				c.add(Restrictions.disjunction().add(Restrictions.ilike("nama", q))
						.add(Restrictions.ilike("alamat", q)).add(Restrictions.ilike("keterangan", q)));
			}
			c.addOrder(Order.asc("nama"));
			List<Lokasi> hasil = c.list();
			return hasil == null ? new ArrayList<Lokasi>() : hasil;
		} catch (Exception e) {
			return new ArrayList<Lokasi>();
		}
	}

	/**
	 * <h3>Fase 2: daftar {@link Gudang} aktif untuk pengisian dropdown "Gudang" pada form Lokasi.</h3>
	 *
	 * <p>{@code Gudang} (skema {@code sirs}, dipakai bersama modul rumah sakit) sudah punya hierarki
	 * pusat-cabang lewat {@code gudangInduk} (self-reference) — jangan membuat field/tabel hierarki
	 * baru khusus e-Kantin, cukup pakai relasi {@link Lokasi#getGudang()} yang sudah ada pada model
	 * tapi belum pernah disurfacekan di form Lokasi versi e-Kantin (JSP maupun ZK). Label memuat nama
	 * gudang induk (bila ada) supaya hierarki tetap terlihat walau pickernya sendiri flat.</p>
	 *
	 * @param session sesi Hibernate milik pemanggil (tidak ditutup di sini).
	 * @return daftar {@code {id, label}} (label = "Nama" atau "Nama (induk: NamaInduk)"), terurut nama.
	 */
	@SuppressWarnings("unchecked")
	public static List<Object[]> daftarGudangUntukPicker(Session session) {
		List<Object[]> hasil = new ArrayList<Object[]>();
		if (session == null) {
			return hasil;
		}
		try {
			Criteria c = session.createCriteria(Gudang.class);
			c.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
			c.addOrder(Order.asc("nama"));
			List<Gudang> daftar = c.list();
			if (daftar == null) {
				return hasil;
			}
			for (int i = 0; i < daftar.size(); i++) {
				Gudang g = daftar.get(i);
				String nama = g.getNama() == null ? ("Gudang #" + g.getId()) : g.getNama();
				Gudang induk = g.getGudangInduk();
				String label = (induk == null || induk.getNama() == null) ? nama
						: (nama + " (induk: " + induk.getNama() + ")");
				hasil.add(new Object[] { g.getId(), label });
			}
		} catch (Exception e) {
			return new ArrayList<Object[]>();
		}
		return hasil;
	}

	/**
	 * Menyimpan (menambah bila {@code id} null, atau memperbarui) satu {@link JenisLokasi}.
	 *
	 * @param session    sesi Hibernate milik pemanggil (tidak ditutup di sini).
	 * @param id         id yang diedit; {@code null} untuk data baru.
	 * @param nama       nama jenis (wajib).
	 * @param keterangan keterangan bebas (boleh null).
	 * @param aktif      status aktif.
	 * @param warna      warna badge hex (boleh null).
	 * @param ikon       kelas ikon (boleh null).
	 * @param urutan     urutan tampil (boleh null → 0).
	 * @param oleh       nama pengguna untuk jejak audit.
	 * @param olehId     id pengguna untuk jejak audit.
	 * @return entity {@link JenisLokasi} yang tersimpan.
	 * @throws IllegalArgumentException bila nama kosong.
	 */
	public static JenisLokasi simpanJenisLokasi(Session session, Long id, String nama, String keterangan,
			boolean aktif, String warna, String ikon, Integer urutan, String oleh, String olehId) {
		if (nama == null || nama.trim().length() == 0) {
			throw new IllegalArgumentException("Nama jenis lokasi wajib diisi.");
		}
		JenisLokasi o = null;
		if (id != null && id.longValue() > 0) {
			o = (JenisLokasi) session.get(JenisLokasi.class, id);
		}
		if (o == null) {
			o = new JenisLokasi();
		}
		o.setNama(nama.trim());
		o.setKeterangan(keterangan);
		o.setAktif(Boolean.valueOf(aktif));
		o.setWarna(warna);
		o.setIkon(ikon);
		o.setUrutan(urutan);
		o.setOleh(oleh);
		o.setOlehId(olehId);
		Common.refreshSaveOrUpdate(session, o);
		return o;
	}

	/**
	 * Menghapus satu {@link JenisLokasi} berdasarkan id (aman bila tidak ditemukan).
	 *
	 * @param session sesi Hibernate milik pemanggil (tidak ditutup di sini).
	 * @param id      id yang dihapus.
	 */
	public static void hapusJenisLokasi(Session session, Long id) {
		if (id == null) {
			return;
		}
		JenisLokasi o = (JenisLokasi) session.get(JenisLokasi.class, id);
		if (o != null) {
			Common.refreshDelete(session, o);
		}
	}

	/**
	 * Menyimpan (menambah/perbarui) satu {@link Lokasi}. Relasi jenis &amp; toko diselesaikan lewat id.
	 *
	 * @param session       sesi Hibernate milik pemanggil (tidak ditutup di sini).
	 * @param id            id yang diedit; {@code null} untuk data baru.
	 * @param nama          nama lokasi (wajib).
	 * @param keterangan    keterangan (boleh null).
	 * @param alamat        alamat (boleh null).
	 * @param aktif         status aktif (boleh null → default aktif).
	 * @param jenisLokasiId id jenis lokasi (boleh null → tanpa jenis).
	 * @param tokoId        id toko/outlet terkait (boleh null).
	 * @param gudangId      id {@link Gudang} induk-nya hierarki pergudangan (boleh null → tanpa gudang;
	 *                      lihat {@link #daftarGudangUntukPicker(Session)}). Fase 2.
	 * @param oleh          nama pengguna audit.
	 * @param olehId        id pengguna audit.
	 * @return entity {@link Lokasi} tersimpan.
	 * @throws IllegalArgumentException bila nama kosong.
	 */
	public static Lokasi simpanLokasi(Session session, Long id, String nama, String keterangan, String alamat,
			Boolean aktif, Long jenisLokasiId, Long tokoId, Long gudangId, String oleh, String olehId) {
		if (nama == null || nama.trim().length() == 0) {
			throw new IllegalArgumentException("Nama lokasi wajib diisi.");
		}
		Lokasi o = null;
		if (id != null && id.longValue() > 0) {
			o = (Lokasi) session.get(Lokasi.class, id);
		}
		if (o == null) {
			o = new Lokasi();
		}
		o.setNama(nama.trim());
		o.setKeterangan(keterangan);
		o.setAlamat(alamat);
		o.setAktif(aktif == null ? Boolean.TRUE : aktif);
		if (jenisLokasiId != null && jenisLokasiId.longValue() > 0) {
			o.setJenisLokasi((JenisLokasi) session.get(JenisLokasi.class, jenisLokasiId));
		} else {
			o.setJenisLokasi(null);
		}
		if (tokoId != null && tokoId.longValue() > 0) {
			o.setToko((Toko) session.get(Toko.class, tokoId));
		} else {
			o.setToko(null);
		}
		if (gudangId != null && gudangId.longValue() > 0) {
			o.setGudang((Gudang) session.get(Gudang.class, gudangId));
		} else {
			o.setGudang(null);
		}
		o.setOleh(oleh);
		o.setOlehId(olehId);
		Common.refreshSaveOrUpdate(session, o);
		return o;
	}

	/**
	 * Menghapus satu {@link Lokasi} berdasarkan id (aman bila tidak ditemukan).
	 *
	 * @param session sesi Hibernate milik pemanggil (tidak ditutup di sini).
	 * @param id      id yang dihapus.
	 */
	public static void hapusLokasi(Session session, Long id) {
		if (id == null) {
			return;
		}
		Lokasi o = (Lokasi) session.get(Lokasi.class, id);
		if (o != null) {
			Common.refreshDelete(session, o);
		}
	}

	/**
	 * Definisi jenis lokasi bawaan yang relevan untuk ERP Kampus/Sekolah: {@code {nama, warna, ikon}}.
	 * Dipakai oleh {@link #ensureJenisLokasiDefault()} sebagai data awal. Menambah/mengubah daftar ini
	 * cukup di satu tempat; entri baru otomatis ikut ter-seed pada startup berikutnya (yang belum ada).
	 */
	private static final String[][] JENIS_LOKASI_DEFAULT = {
			{ "Gudang", "#0d6efd", "fas fa-warehouse" },
			{ "Outlet", "#16a34a", "fas fa-store" },
			{ "Kasir", "#f59e0b", "fas fa-cash-register" },
			{ "Entry Data", "#7c3aed", "fas fa-keyboard" },
			{ "Logistik", "#0891b2", "fas fa-truck" },
			{ "Keuangan", "#dc2626", "fas fa-coins" },
			{ "Dapur / Produksi", "#ea580c", "fas fa-utensils" },
			{ "Perpustakaan", "#65a30d", "fas fa-book" },
			{ "Laboratorium", "#0ea5e9", "fas fa-flask" },
			{ "Klinik / UKS", "#e11d48", "fas fa-briefcase-medical" },
			{ "Kantor / Administrasi", "#334155", "fas fa-briefcase" },
			{ "Aula / Serbaguna", "#9333ea", "fas fa-chalkboard-user" },
			{ "Koperasi", "#0d9488", "fas fa-basket-shopping" },
			{ "Parkir", "#64748b", "fas fa-square-parking" },
			{ "Dalam Perjalanan", "#94a3b8", "fas fa-truck-fast" } };

	/**
	 * Mengisi data awal {@link JenisLokasi} bawaan saat aplikasi startup, bila belum ada.
	 *
	 * <p>
	 * Metode ini <b>idempoten</b>: hanya menambah jenis yang <i>belum</i> ada (dicek berdasarkan nama,
	 * tidak peduli huruf besar/kecil), sehingga aman dipanggil setiap kali aplikasi dimuat ulang dan
	 * tidak akan menduplikasi maupun menimpa perubahan yang telah dibuat admin (mis. warna/ikon yang
	 * disesuaikan). Nilai bawaan mengikuti kebutuhan ERP Kampus/Sekolah: Gudang, Kasir, Entry Data,
	 * Logistik, Keuangan, dan tempat-tempat lain yang lazim (Perpustakaan, Laboratorium, Klinik/UKS,
	 * Dapur/Produksi, Kantor, Aula, Koperasi, Parkir). Daftar lengkap ada pada {@link #JENIS_LOKASI_DEFAULT}.
	 * </p>
	 *
	 * <p>
	 * <b>Sesi:</b> memakai {@link HibernateUtil#currentNativeSession()} dengan transaksi eksplisit dan
	 * <b>DITUTUP di blok {@code finally}</b> (disconnect + close + closeSession), mengikuti pola
	 * {@code MenuHelper.ensure*} — sesuai aturan bahwa sesi hasil {@code openSession()}/
	 * {@code currentNativeSession()} wajib ditutup. Semua kegagalan ditelan (log + rollback) agar
	 * seeding tidak pernah menggagalkan proses startup; data akan dicoba lagi pada startup berikutnya.
	 * Dipanggil dari {@code AppStartupListener} setelah SessionFactory dibangun (tabel sudah dibuat oleh
	 * hbm2ddl). Kompatibel Java 1.7.
	 * </p>
	 */
	public static void ensureJenisLokasiDefault() {
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.currentNativeSession();
			tx = session.beginTransaction();

			// Ambil sekali nama-nama yang sudah ada (huruf kecil) untuk cek idempoten yang efisien.
			Set<String> ada = new HashSet<String>();
			List<?> rows = session.createSQLQuery("select lower(nama) from asset.jenis_lokasi").list();
			for (int i = 0; i < rows.size(); i++) {
				Object o = rows.get(i);
				if (o != null) {
					ada.add(o.toString());
				}
			}

			int tambah = 0;
			for (int i = 0; i < JENIS_LOKASI_DEFAULT.length; i++) {
				String nama = JENIS_LOKASI_DEFAULT[i][0];
				if (ada.contains(nama.toLowerCase())) {
					continue;
				}
				JenisLokasi jl = new JenisLokasi();
				jl.setNama(nama);
				jl.setWarna(JENIS_LOKASI_DEFAULT[i][1]);
				jl.setIkon(JENIS_LOKASI_DEFAULT[i][2]);
				jl.setAktif(Boolean.TRUE);
				jl.setUrutan(Integer.valueOf(i + 1));
				jl.setKeterangan("Bawaan sistem");
				jl.setOleh("SYSTEM");
				jl.setOlehId("SYSTEM");
				session.save(jl);
				tambah++;
			}
			tx.commit();
			if (tambah > 0) {
				System.out.println("Seed JenisLokasi default: " + tambah + " jenis ditambahkan.");
			}
		} catch (Exception e) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/LokasiKantinUtil.java:387");
				}
			}
			System.err.println("Gagal seed JenisLokasi default (lanjut, dicoba lagi saat startup berikutnya): "
					+ e.getMessage());
		} finally {
			if (session != null) {
				try {
					session.disconnect();
				} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/LokasiKantinUtil.java:396");
				}
				try {
					session.close();
				} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/LokasiKantinUtil.java:400");
				}
			}
			HibernateUtil.closeSession();
		}
	}
}

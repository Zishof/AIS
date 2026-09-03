package ais.action.servlet.api;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.Type;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;

/**
 * <h3>API JSON riwayat revisi Envers per baris data (AuditTrails) utk klien POS
 * Desktop/Android -- padanan ringan {@code GenericRevisiHelper} (ZK).</h3>
 *
 * <p>Aksi:</p>
 * <ul>
 *   <li>{@code revisi_daftar} {entitas, id} -- daftar revisi baris (rev, tanggal,
 *       tipe ADD/MOD/DEL, oleh) urut terbaru; semua user login boleh melihat.</li>
 *   <li>{@code revisi_detail} {entitas, id, rev} -- nilai properti sederhana pada
 *       revisi tsb (utk tampilan banding/pratinjau restore).</li>
 *   <li>{@code revisi_pulihkan} {entitas, id, rev} -- RESTORE nilai revisi ke data
 *       hidup. HANYA {@code Common.getApakahAdminLain} (paritas tombol Restore di
 *       GenericRevisiHelper yang memang layar admin). Restore DANGKAL: properti
 *       sederhana + relasi ManyToOne di-resolve ke baris LIVE ber-id sama;
 *       koleksi dilewati (pola restoreOneProperty, bukan deep-restore).</li>
 * </ul>
 *
 * <p>Entitas dibatasi WHITELIST {@link #ENTITAS} (kode stabil -> kelas) -- klien
 * tidak pernah bisa menyebut kelas sembarangan. Menambah dukungan = tambah satu
 * baris peta (entitas wajib @Audited).</p>
 */
public final class RevisiApiHelper {

	private RevisiApiHelper() {
	}

	/** kode stabil (dipakai klien) -> kelas entitas @Audited. */
	static final Map<String, Class> ENTITAS = new LinkedHashMap<String, Class>();
	static {
		ENTITAS.put("produk", ais.database.model.inventory.Produk.class);
		ENTITAS.put("jenis_produk", ais.database.model.inventory.JenisProduk.class);
		ENTITAS.put("grup_produk", ais.database.model.inventory.GrupProduk.class);
		ENTITAS.put("toko", ais.database.model.inventory.Toko.class);
		ENTITAS.put("penyedia", ais.database.model.library.Penyedia.class);
		ENTITAS.put("anggota", ais.database.model.koperasi.AnggotaKoperasi.class);
		ENTITAS.put("jenis_anggota", ais.database.model.koperasi.JenisAnggotaKoperasi.class);
		ENTITAS.put("tipe_anggota", ais.database.model.koperasi.TipeAnggotaKoperasi.class);
		ENTITAS.put("cara_bayar", ais.database.model.koperasi.CaraPembayaranKoperasi.class);
		ENTITAS.put("diskon", ais.database.model.koperasi.AturanDiskon.class);
		// Riwayat transaksi (header nota) -- baca-saja utk penelusuran; restore
		// tetap dimungkinkan admin (kasus salah-void diperiksa manual).
		ENTITAS.put("transaksi", ais.database.model.koperasi.PembelianAnggotaKoperasi.class);
		// Gelombang 2 (2026-08-19): KebijakanRetur & GrupAturanDiskon kini @Audited
		// (revisi mulai terekam sejak deploy ini); sisanya sudah lama ter-audit.
		ENTITAS.put("kebijakan_retur", ais.database.model.inventory.KebijakanRetur.class);
		ENTITAS.put("diskon_grup", ais.database.model.koperasi.GrupAturanDiskon.class);
		ENTITAS.put("produk_batch", ais.database.model.inventory.ProdukBatch.class);
		ENTITAS.put("si_customer", ais.database.model.koperasi.CustomerInventoryProfile.class);
		ENTITAS.put("hotel_properti", ais.database.model.hotel.PropertiHotel.class);
		ENTITAS.put("hotel_kontrak", ais.database.model.hotel.KontrakPemilik.class);
		ENTITAS.put("hotel_tamu", ais.database.model.hotel.Tamu.class);
		ENTITAS.put("hotel_tipe_kamar", ais.database.model.hotel.TipeKamar.class);
		ENTITAS.put("hotel_kamar", ais.database.model.hotel.Kamar.class);
		// Gelombang 3 (2026-08-19): melengkapi layar yang sudah baca lokal-dulu
		// tapi belum punya tombol Riwayat (master Sales/Supplier IS, pencairan
		// diskon, profil item apotik).
		ENTITAS.put("si_sales", ais.database.model.koperasi.SalesInventory.class);
		ENTITAS.put("si_supplier", ais.database.model.koperasi.SupplierInventoryProfile.class);
		ENTITAS.put("pencairan_diskon", ais.database.model.koperasi.PencairanDiskon.class);
		ENTITAS.put("apotik_item", ais.database.model.sirs.ApotikItemProfile.class);
		// Gelombang 4 (2026-08-21): entitas PESANAN kantin. Ditambahkan setelah
		// pesanan Agustus hilang dari layar Pesanan -- baris yang terhapus hanya
		// dapat ditelusuri lewat tabel audit, dan tanpa entri ini kode entitasnya
		// tidak dikenal oleh API riwayat.
		ENTITAS.put("pesanan", ais.database.model.koperasi.DraftPembelianAnggotaKoperasi.class);
		ENTITAS.put("pesanan_item", ais.database.model.inventory.DraftPembelian.class);
		ENTITAS.put("pembelian", ais.database.model.inventory.Pembelian.class);
		// Gelombang 5 (2026-08-21): empat tahap PENGADAAN. Tombol Riwayat pada layar
		// PR/PO/BAST/Bayar sudah lama mengirim kode entitas ini, tetapi kodenya tidak
		// pernah terdaftar di sini sehingga selalu dijawab "Entitas tidak dikenal".
		// Keempat kelasnya sudah @Audited, jadi riwayatnya memang tersedia.
		ENTITAS.put("pengadaan_pr", ais.database.model.asset.PermintaanPengadaanMasterAsset.class);
		ENTITAS.put("pengadaan_po", ais.database.model.asset.PemesananPengadaanMasterAsset.class);
		ENTITAS.put("pengadaan_bast", ais.database.model.asset.PenerimaanPengadaanMasterAsset.class);
		ENTITAS.put("pengadaan_bayar", ais.database.model.asset.PembayaranTerminMasterAsset.class);
		// Gelombang 6 (2026-08-21): UJIAN dan SOAL-nya. Ujian yang terhapus atau
		// tersunting keliru sulit disusun ulang secara manual -- satu ujian bisa
		// memuat puluhan soal. Keduanya sudah @Audited, jadi riwayatnya memang
		// tersimpan; yang kurang hanya pendaftarannya di sini.
		ENTITAS.put("ujian", ais.database.model.Ujian.class);
		ENTITAS.put("ujian_soal", ais.database.model.UjianPunyaSoal.class);
		// Gelombang 7 (2026-08-29): seluruh CRUD operasional POS yang sudah
		// @Audited harus dapat dipantau dari satu menu Riwayat Perubahan Data.
		// Registry tetap eksplisit (bukan menerima nama kelas dari request) agar
		// model internal/sensitif di luar POS tidak ikut terbuka lewat API generik.
		ENTITAS.put("satuan_produk", ais.database.model.inventory.SatuanProduk.class);
		ENTITAS.put("pemasok_produk", ais.database.model.inventory.PemasokProduk.class);
		ENTITAS.put("pengadaan_faktur", ais.database.model.inventory.PengadaanFaktur.class);
		ENTITAS.put("pengadaan_produk", ais.database.model.inventory.PengadaanProduk.class);
		ENTITAS.put("stok_opname", ais.database.model.inventory.StokOpname.class);
		ENTITAS.put("sesi_stok_opname", ais.database.model.inventory.SesiStokOpname.class);
		ENTITAS.put("mutasi_stok", ais.database.model.inventory.MutasiStokToko.class);
		ENTITAS.put("retur_penjualan", ais.database.model.inventory.ReturPenjualan.class);
		ENTITAS.put("retur_pembelian", ais.database.model.inventory.ReturPembelian.class);
		ENTITAS.put("produksi", ais.database.model.inventory.ProduksiKantin.class);
		ENTITAS.put("pemakaian_bahan_baku", ais.database.model.inventory.PemakaianBahanBaku.class);
		ENTITAS.put("sesi_kas", ais.database.model.inventory.SesiKasKasir.class);
		ENTITAS.put("calon_anggota", ais.database.model.koperasi.CalonAnggotaKoperasi.class);
		ENTITAS.put("jenis_identitas_anggota", ais.database.model.koperasi.JenisIdentitasAnggotaKoperasi.class);
		ENTITAS.put("pengajuan_limit_member", ais.database.model.koperasi.PengajuanLimitTransaksiMember.class);
		ENTITAS.put("pembayaran_anggota", ais.database.model.koperasi.PembayaranAnggotaKoperasi.class);
		ENTITAS.put("penyesuaian_saldo_anggota", ais.database.model.koperasi.PenyesuaianSaldoAnggota.class);
		ENTITAS.put("pembayaran_hutang_supplier", ais.database.model.koperasi.PembayaranHutangSupplier.class);
		ENTITAS.put("penerimaan_piutang_customer", ais.database.model.koperasi.PenerimaanPiutangCustomer.class);
		ENTITAS.put("harga_jual_customer", ais.database.model.koperasi.HargaJualCustomer.class);
		ENTITAS.put("harga_beli_supplier", ais.database.model.koperasi.HargaBeliSupplier.class);
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	/**
	 * Kunci menu yang mengatur siapa boleh MELIHAT riwayat sebuah entitas.
	 *
	 * <p>Riwayat satu baris ({@code revisi_daftar}/{@code revisi_detail}) semula
	 * terbuka untuk SETIAP pengguna yang login. Aturan itu masuk akal ketika
	 * daftar putih entitasnya masih berisi data produk; ia ikut terbawa ketika
	 * daftar itu tumbuh memuat data pribadi. Snapshot {@code anggota} membawa
	 * alamat, telepon, HP, dan surel; {@code hotel_tamu} membawa nomor identitas.
	 * Penyaring {@link #propertiSensitif} tidak menutupinya, dan memang tidak
	 * seharusnya -- tugasnya menyaring KREDENSIAL, bukan data pribadi.
	 *
	 * <p>Aturannya kini: boleh melihat riwayat baris yang memang boleh dilihat.
	 * Kunci di sini hanya yang kode entitasnya PERSIS sama dengan kunci menu --
	 * tidak ada yang ditebak. Entitas lain mempertahankan perilaku lama sampai
	 * pemetaannya diputuskan; lihat docs/pos/105.
	 *
	 * <p>{@code hotel_tamu} adalah satu-satunya pengecualian yang dipetakan
	 * secara eksplisit: data tamu tidak punya menu sendiri, tetapi hanya berguna
	 * bagi yang mengerjakan salah satu layar Hotel. Karena itu ia menerima kunci
	 * hotel mana pun -- lebih longgar daripada satu kunci, jauh lebih sempit
	 * daripada "setiap pengguna login".
	 */
	private static final Map<String, String[]> KUNCI_MENU_ENTITAS =
		new LinkedHashMap<String, String[]>();
	static {
		String[] satu;
		for (String kode : new String[] { "produk", "grup_produk", "penyedia", "anggota",
			"diskon", "hotel_properti", "hotel_kamar", "pesanan", "pengadaan_pr",
			"pengadaan_po", "pengadaan_bast", "produksi" }) {
			satu = new String[] { kode };
			KUNCI_MENU_ENTITAS.put(kode, satu);
		}
		KUNCI_MENU_ENTITAS.put("hotel_tamu", new String[] { "hotel_reservasi",
			"hotel_checkin", "hotel_folio", "hotel_kamar", "hotel_properti" });
		// Empat berikut dipetakan berdasar BUKTI, bukan kemiripan nama: ketiga
		// entitas si_* dipakai layar yang namanya sendiri menyebut kuncinya
		// (master_customer_screen.dart dst), dan snapshot-nya membawa NOMOR
		// REKENING (noRekening, atasNama, bank, alamatBank) yang tidak disaring
		// propertiSensitif. calon_anggota membawa kodeIdentitas, nama, alamat,
		// telp, hp, dan surel; ia tidak dipakai dialog riwayat di klien sama
		// sekali, jadi menggerbanginya tidak memutus alur mana pun.
		KUNCI_MENU_ENTITAS.put("si_customer", new String[] { "master_customer" });
		KUNCI_MENU_ENTITAS.put("si_supplier", new String[] { "master_supplier" });
		KUNCI_MENU_ENTITAS.put("si_sales", new String[] { "master_sales" });
		KUNCI_MENU_ENTITAS.put("calon_anggota", new String[] { "anggota" });
	}

	/**
	 * Entitas yang riwayatnya mengikuti gerbang ADMIN, bukan kunci menu.
	 *
	 * <p>{@code toko} tidak punya kunci menu sendiri di {@code DAFTAR}; seluruh
	 * mutasinya admin-only di keempat kanal ({@code TokoApiHelper}: "Seluruh
	 * mutasi admin-only ... padanan gate isAdmin di JSP dan checkbox admin-only
	 * ZK"). Snapshot-nya membawa alamat, telepon, surel, nama &amp; HP PIC, serta
	 * NPWP -- riwayatnya karena itu mengikuti gerbang yang sama dengan datanya.
	 */
	private static final java.util.Set<String> ENTITAS_ADMIN_SAJA =
			new java.util.LinkedHashSet<String>(java.util.Arrays.asList("toko"));

	/**
	 * True bila pengguna boleh membuka riwayat entitas ini. Entitas yang belum
	 * dipetakan mempertahankan perilaku lama (semua pengguna login).
	 */
	private static boolean bolehLihatRiwayat(Tbmuser tbmuser, String kode) {
		if (ENTITAS_ADMIN_SAJA.contains(kode)) {
			return Common.getApakahAdminLain(tbmuser);
		}
		String[] kunci = (String[]) KUNCI_MENU_ENTITAS.get(kode);
		if (kunci == null) {
			return true;
		}
		if (Common.getApakahAdminLain(tbmuser)) {
			return true;
		}
		ais.database.model.Tbmrole role = tbmuser == null ? null : tbmuser.hakAkses();
		if (role == null) {
			// Sejalan dgn PosApi.bolehAksesActionKantin: tanpa role sama sekali,
			// perilakunya tidak dipersempit oleh perubahan ini.
			return true;
		}
		org.json.JSONObject menu = ais.common.EbisnisMenuKatalog
			.urai(role.getEbisnisMenu()).optJSONObject("menu");
		if (menu == null) {
			return true;
		}
		for (int i = 0; i < kunci.length; i++) {
			if (menu.optBoolean(kunci[i], true)) {
				return true;
			}
		}
		return false;
	}

	private static Class kelasDari(JSONObject request, JSONObject hasil) throws Exception {
		String kode = request == null ? "" : request.optString("entitas", "").trim();
		Class clazz = (Class) ENTITAS.get(kode);
		if (clazz == null) {
			tolak(hasil, "Entitas '" + kode + "' tidak dikenal utk riwayat revisi.");
		}
		return clazz;
	}

	private static Long idDari(JSONObject request, JSONObject hasil) throws Exception {
		if (request == null || request.isNull("id")) {
			tolak(hasil, "Parameter id wajib diisi.");
			return null;
		}
		return Long.valueOf((request.get("id") + "").trim());
	}

	/** Nilai properti -> bentuk JSON yang aman (tipe sederhana; relasi -> label). */
	private static Object nilaiRingkas(Object nilai) {
		if (nilai == null) return JSONObject.NULL;
		if (nilai instanceof String || nilai instanceof Number || nilai instanceof Boolean) {
			return nilai;
		}
		if (nilai instanceof Date) {
			return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date) nilai);
		}
		// Relasi/objek lain: coba label getNama() dulu, jatuh ke toString ringkas.
		try {
			java.lang.reflect.Method m = nilai.getClass().getMethod("getNama");
			Object nama = m.invoke(nilai);
			if (nama != null) return String.valueOf(nama);
		} catch (Throwable abaikan) {
			// tidak semua punya getNama -- bukan masalah.
		}
		String s = String.valueOf(nilai);
		return s.length() > 120 ? s.substring(0, 120) : s;
	}

	/** Properti teknis audit tidak dicampur dengan perubahan bisnis produk. */
	private static boolean propertiTeknis(String nama) {
		return "oleh".equals(nama) || "olehId".equals(nama)
				|| "tanggal_dirubah".equals(nama) || "kunciUnik".equals(nama);
	}

	/** Kredensial tidak pernah boleh keluar lewat endpoint audit generik. */
	private static boolean propertiSensitif(String nama) {
		if (nama == null) return false;
		String n = nama.toLowerCase(java.util.Locale.ENGLISH);
		return "pin".equals(n) || "pass".equals(n) || "password".equals(n)
				|| n.endsWith("hash") || n.endsWith("salt")
				|| n.indexOf("password") >= 0 || n.indexOf("token") >= 0
				|| n.indexOf("secret") >= 0;
	}

	private static String jenisNilai(Type tipe) {
		if (tipe == null) return "Data";
		if (tipe.isEntityType()) return "Referensi master";
		Class kelas = tipe.getReturnedClass();
		if (kelas == null) return "Data";
		if (Boolean.class.isAssignableFrom(kelas) || Boolean.TYPE.equals(kelas)) return "Status Ya/Tidak";
		if (Number.class.isAssignableFrom(kelas)) return "Angka";
		if (Date.class.isAssignableFrom(kelas)) return "Tanggal/Waktu";
		return "Teks";
	}

	private static boolean nilaiSama(Object kiri, Object kanan) {
		if (kiri == null && kanan == null) return true;
		if (kiri == null || kanan == null) return false;
		return String.valueOf(kiri).equals(String.valueOf(kanan));
	}

	/**
	 * Membandingkan dua snapshot Envers. Nilai relasi diringkas menjadi nama
	 * master agar pengguna membaca "Pcs -> Botol", bukan nama kelas/proxy.
	 */
	private static JSONArray perubahan(ClassMetadata meta, Object sebelum,
			Object sesudah) throws Exception {
		JSONArray arr = new JSONArray();
		String[] props = meta.getPropertyNames();
		Type[] tipe = meta.getPropertyTypes();
		for (int i = 0; i < props.length; i++) {
			if (tipe[i].isCollectionType() || propertiTeknis(props[i])
					|| propertiSensitif(props[i])) continue;
			Object lama = null;
			Object baru = null;
			try {
				if (sebelum != null) lama = nilaiRingkas(
						meta.getPropertyValue(sebelum, props[i], EntityMode.POJO));
				if (sesudah != null) baru = nilaiRingkas(
						meta.getPropertyValue(sesudah, props[i], EntityMode.POJO));
			} catch (Throwable abaikan) {
				continue;
			}
			if (nilaiSama(lama, baru)) continue;
			JSONObject j = new JSONObject();
			j.put("field", props[i]);
			j.put("jenisData", jenisNilai(tipe[i]));
			j.put("dari", lama == null ? JSONObject.NULL : lama);
			j.put("menjadi", baru == null ? JSONObject.NULL : baru);
			arr.put(j);
		}
		return arr;
	}

	@SuppressWarnings("unchecked")
	public static void daftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (tbmuser == null) { tolak(hasil, "Sesi tidak dikenali."); return; }
		Class clazz = kelasDari(request, hasil);
		Long id = clazz == null ? null : idDari(request, hasil);
		if (clazz == null || id == null) return;
		// Riwayat satu baris mengikuti hak melihat barisnya sendiri -- snapshot
		// anggota/tamu membawa alamat, telepon, surel, dan nomor identitas.
		if (!bolehLihatRiwayat(tbmuser, request.optString("entitas", "").trim())) {
			tolak(hasil, "Riwayat data ini hanya untuk pengguna yang berhak membuka menunya.");
			return;
		}
		int batas = Math.min(100, Math.max(1, request.optInt("batas", 30)));
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			AuditReader reader = AuditReaderFactory.get(session);
			List baris = reader.createQuery().forRevisionsOfEntity(clazz, false, true)
					.add(AuditEntity.id().eq(id))
					.addOrder(AuditEntity.revisionNumber().desc())
					// Satu snapshot tambahan dipakai sebagai nilai "sebelum" untuk
					// revisi terakhir yang ditampilkan dan tidak ikut dikirim ke klien.
					.setMaxResults(batas + 1).getResultList();
			ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(clazz);
			JSONArray arr = new JSONArray();
			for (int i = 0; i < baris.size() && i < batas; i++) {
				Object[] b = (Object[]) baris.get(i);
				Object entitas = b[0];
				org.hibernate.envers.DefaultRevisionEntity rev =
						(org.hibernate.envers.DefaultRevisionEntity) b[1];
				RevisionType tipe = (RevisionType) b[2];
				JSONObject j = new JSONObject();
				j.put("rev", rev.getId());
				j.put("tanggal", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
						.format(rev.getRevisionDate()));
				j.put("tipe", tipe == RevisionType.ADD ? "TAMBAH"
						: tipe == RevisionType.DEL ? "HAPUS" : "UBAH");
				Object snapshotSebelum = i + 1 < baris.size()
						? ((Object[]) baris.get(i + 1))[0] : null;
				Object sebelum = tipe == RevisionType.ADD ? null : snapshotSebelum;
				Object sesudah = tipe == RevisionType.DEL ? null : entitas;
				j.put("perubahan", perubahan(meta, sebelum, sesudah));
				// "oleh" diambil dari kolom audit entitas itu sendiri (GeneralValueObject
				// menyimpannya per revisi); DefaultRevisionEntity tidak memuat user.
				try {
					Object oleh = meta.getPropertyValue(entitas, "oleh", EntityMode.POJO);
					if (oleh != null) j.put("oleh", String.valueOf(oleh));
				} catch (Throwable abaikan) {
					// entitas tanpa properti oleh -- kolom dikosongkan saja.
				}
				try {
					Object nama = meta.getPropertyValue(entitas, "nama", EntityMode.POJO);
					if (nama != null) j.put("nama", String.valueOf(nama));
				} catch (Throwable abaikan) {
					// idem.
				}
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("bolehPulihkan", Common.getApakahAdminLain(tbmuser));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void detail(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (tbmuser == null) { tolak(hasil, "Sesi tidak dikenali."); return; }
		Class clazz = kelasDari(request, hasil);
		Long id = clazz == null ? null : idDari(request, hasil);
		if (clazz == null || id == null) return;
		// Riwayat satu baris mengikuti hak melihat barisnya sendiri -- snapshot
		// anggota/tamu membawa alamat, telepon, surel, dan nomor identitas.
		if (!bolehLihatRiwayat(tbmuser, request.optString("entitas", "").trim())) {
			tolak(hasil, "Riwayat data ini hanya untuk pengguna yang berhak membuka menunya.");
			return;
		}
		int rev = request.optInt("rev", -1);
		if (rev < 0) { tolak(hasil, "Parameter rev wajib diisi."); return; }
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			AuditReader reader = AuditReaderFactory.get(session);
			Object snapshot = reader.find(clazz, id, Integer.valueOf(rev));
			if (snapshot == null) { tolak(hasil, "Revisi tidak ditemukan."); return; }
			ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(clazz);
			String[] props = meta.getPropertyNames();
			Type[] tipe = meta.getPropertyTypes();
			JSONObject nilai = new JSONObject();
			for (int i = 0; i < props.length; i++) {
				if (tipe[i].isCollectionType() || propertiSensitif(props[i])) continue;
				// koleksi di luar riwayat baris; kredensial sengaja tidak diekspos.
				try {
					nilai.put(props[i], nilaiRingkas(
							meta.getPropertyValue(snapshot, props[i], EntityMode.POJO)));
				} catch (Throwable abaikan) {
					// proxy audit yang tak bisa dibaca -- lewati properti itu saja.
				}
			}
			hasil.put("status", "00");
			hasil.put("nilai", nilai);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Restore DANGKAL nilai satu revisi ke baris hidup (admin-only). Baris yang
	 * sudah terhapus dihidupkan lagi lewat instance baru ber-id sama. Relasi
	 * ManyToOne diambil ulang dari data LIVE ber-id sama (bukan proxy audit);
	 * relasi yang barisnya sudah tidak ada dilewati (fail-soft per properti).
	 */
	public static void pulihkan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (tbmuser == null || !Common.getApakahAdminLain(tbmuser)) {
			tolak(hasil, "Hanya admin sistem yang boleh me-restore data dari riwayat.");
			return;
		}
		Class clazz = kelasDari(request, hasil);
		Long id = clazz == null ? null : idDari(request, hasil);
		if (clazz == null || id == null) return;
		int rev = request.optInt("rev", -1);
		if (rev < 0) { tolak(hasil, "Parameter rev wajib diisi."); return; }
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			AuditReader reader = AuditReaderFactory.get(session);
			Object snapshot = reader.find(clazz, id, Integer.valueOf(rev));
			if (snapshot == null) { tolak(hasil, "Revisi tidak ditemukan."); return; }
			ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(clazz);
			// Jalur penyalinan SATU-SATUNYA, dipakai bersama restore massal. Kalau
			// dipisah, keduanya akan menyimpang pelan-pelan dan "Pulihkan" satuan
			// tidak lagi berarti hal yang sama dengan "Pulihkan semua".
			JSONObject lapor = salinKeLive(session, meta, clazz, snapshot, id);
			if (!lapor.optBoolean("ok", false)) {
				tolak(hasil, "Restore gagal: " + lapor.optString("pesan", "tidak diketahui"));
				return;
			}
			int dilewati = lapor.optInt("propertiDilewati", 0);
			hasil.put("status", "00");
			hasil.put("dihidupkanLagi", lapor.optBoolean("dihidupkanLagi", false));
			hasil.put("propertiDilewati", dilewati);
			hasil.put("description", "Data dipulihkan dari revisi " + rev
					+ (dilewati > 0 ? " (" + dilewati + " properti dilewati)." : "."));
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "RevisiApiHelper.pulihkan rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Nama properti relasi yang layak ditampilkan sbg penanda baris saat jelajah. */
	private static final String[] RELASI_PENANDA = new String[] {
			"toko", "anggotaKoperasi", "tbmuser", "mejaKantin", "caraPembayaranKoperasi", "lunas" };

	/**
	 * Batas rentang jelajah. {@code akhirHari} mendorong jam ke 23:59:59.999 supaya
	 * "sampai 31 Agustus" benar-benar memuat seluruh 31 Agustus -- tanpa itu batas
	 * atas jatuh di tengah malam dan data seharian penuh ikut hilang dari hasil.
	 */
	private static Date batasTanggal(JSONObject request, String kunci, boolean akhirHari,
			JSONObject hasil) throws Exception {
		String s = request == null ? "" : request.optString(kunci, "").trim();
		if (s.length() == 0) {
			tolak(hasil, "Rentang tanggal (" + kunci + ") wajib diisi.");
			return null;
		}
		java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd");
		f.setLenient(false);
		Date d;
		try {
			d = f.parse(s.length() > 10 ? s.substring(0, 10) : s);
		} catch (Exception e) {
			tolak(hasil, "Tanggal " + s + " tidak dikenali (format yyyy-MM-dd).");
			return null;
		}
		java.util.Calendar c = java.util.Calendar.getInstance();
		c.setTime(d);
		c.set(java.util.Calendar.HOUR_OF_DAY, akhirHari ? 23 : 0);
		c.set(java.util.Calendar.MINUTE, akhirHari ? 59 : 0);
		c.set(java.util.Calendar.SECOND, akhirHari ? 59 : 0);
		c.set(java.util.Calendar.MILLISECOND, akhirHari ? 999 : 0);
		return c.getTime();
	}

	private static RevisionType tipeRevisi(String kode) {
		if ("TAMBAH".equalsIgnoreCase(kode)) return RevisionType.ADD;
		if ("UBAH".equalsIgnoreCase(kode)) return RevisionType.MOD;
		if ("HAPUS".equalsIgnoreCase(kode)) return RevisionType.DEL;
		return null; // "SEMUA" atau kosong -> tanpa saringan tipe.
	}

	/** Daftar kode entitas yang boleh dijelajah -- dipakai klien utk mengisi combo. */
	public static void entitas(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (tbmuser == null) { tolak(hasil, "Sesi tidak dikenali."); return; }
		JSONArray arr = new JSONArray();
		java.util.Iterator it = ENTITAS.keySet().iterator();
		while (it.hasNext()) {
			String kode = (String) it.next();
			JSONObject j = new JSONObject();
			j.put("kode", kode);
			Class c = (Class) ENTITAS.get(kode);
			j.put("kelas", c == null ? "" : c.getSimpleName());
			arr.put(j);
		}
		hasil.put("status", "00");
		hasil.put("data", arr);
		hasil.put("bolehJelajah", Common.getApakahAdminLain(tbmuser));
	}

	/**
	 * <h3>Jelajah tabel audit LINTAS baris -- padanan tab "Semua" pada
	 * {@code GenericRevisiHelper} (ZK).</h3>
	 *
	 * <p>Berbeda dari {@link #daftar} yang menuntut satu {@code id}: di sini id-nya
	 * justru yang dicari. Baris yang <b>sudah terhapus</b> tidak lagi muncul di layar
	 * mana pun, sehingga tidak ada tombol riwayat yang bisa diklik untuknya -- satu-
	 * satunya jalan menemukannya kembali adalah menyapu tabel audit menurut rentang
	 * tanggal. Itulah kasus yang melahirkan aksi ini.</p>
	 *
	 * <p>Rentang tanggal <b>wajib</b>, sama seperti versi ZK: tabel audit menyimpan
	 * seluruh sejarah dan menyapunya tanpa batas akan menarik jutaan baris.</p>
	 *
	 * <p>Karena keluarannya memuat data yang sudah dihapus dari seluruh toko --
	 * melewati pembatasan toko/pendaftar yang berlaku di layar biasa -- aksi ini
	 * dibatasi ADMINISTRATOR. Riwayat per baris ({@code revisi_daftar}) tetap terbuka
	 * untuk semua pengguna.</p>
	 */
	@SuppressWarnings("unchecked")
	public static void jelajah(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (tbmuser == null) { tolak(hasil, "Sesi tidak dikenali."); return; }
		if (!Common.getApakahAdminLain(tbmuser)) {
			tolak(hasil, "Jelajah riwayat lintas-baris hanya untuk ADMINISTRATOR, karena "
					+ "menampilkan data terhapus dari seluruh toko. Riwayat satu baris tetap "
					+ "bisa dibuka lewat tombol jam pada tabel.");
			return;
		}
		Class clazz = kelasDari(request, hasil);
		if (clazz == null) return;
		Date dari = batasTanggal(request, "dari", false, hasil);
		if (dari == null) return;
		Date sampai = batasTanggal(request, "sampai", true, hasil);
		if (sampai == null) return;
		if (dari.after(sampai)) {
			tolak(hasil, "Tanggal awal melewati tanggal akhir.");
			return;
		}
		int batas = Math.min(300, Math.max(1, request.optInt("batas", 100)));
		int mulai = Math.max(0, request.optInt("mulai", 0));

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			AuditReader reader = AuditReaderFactory.get(session);
			ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(clazz);
			org.hibernate.envers.query.AuditQuery q =
					siapkanQuery(reader, clazz, meta, request, dari, sampai, hasil);
			if (q == null) return;
			q.addOrder(AuditEntity.revisionNumber().desc());
			q.setFirstResult(mulai);
			// +1 baris semata utk mengetahui masih ada halaman berikutnya; tidak dikirim.
			q.setMaxResults(batas + 1);
			List baris = q.getResultList();

			boolean adaLagi = baris.size() > batas;
			java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String[] props = meta.getPropertyNames();
			Type[] tipeProp = meta.getPropertyTypes();
			JSONArray arr = new JSONArray();
			for (int i = 0; i < baris.size() && arr.length() < batas; i++) {
				Object[] b = (Object[]) baris.get(i);
				Object entitasBaris = b[0];
				org.hibernate.envers.DefaultRevisionEntity rev =
						(org.hibernate.envers.DefaultRevisionEntity) b[1];
				RevisionType tipe = (RevisionType) b[2];
				// revAwal adalah revisi PADA-ATAU-SEBELUM tanggal awal, jadi batas bawah
				// tadi sedikit longgar. Saring ulang di sini supaya hasil benar-benar
				// berada dalam rentang yang diminta.
				Date waktu = rev.getRevisionDate();
				if (waktu == null || waktu.before(dari) || waktu.after(sampai)) continue;

				JSONObject j = new JSONObject();
				try {
					j.put("id", meta.getIdentifier(entitasBaris, EntityMode.POJO));
				} catch (Throwable abaikan) {
					// tanpa id baris tetap ditampilkan; kolomnya saja yang kosong.
				}
				j.put("rev", rev.getId());
				j.put("tanggal", fmt.format(waktu));
				j.put("tipe", tipe == RevisionType.ADD ? "TAMBAH"
						: tipe == RevisionType.DEL ? "HAPUS" : "UBAH");

				JSONObject ringkas = new JSONObject();
				for (int p = 0; p < props.length; p++) {
					if (tipeProp[p].isCollectionType() || tipeProp[p].isEntityType()
							|| propertiSensitif(props[p])) continue;
					try {
						Object nilai = meta.getPropertyValue(entitasBaris, props[p], EntityMode.POJO);
						if (nilai != null) ringkas.put(props[p], nilaiRingkas(nilai));
					} catch (Throwable abaikan) {
						// satu properti tak terbaca tidak boleh menggagalkan seluruh baris.
					}
				}
				// Relasi sengaja dibatasi beberapa nama saja: membuka SELURUH relasi utk
				// ratusan baris berarti ratusan query proxy, sedangkan yang dibutuhkan
				// penelusuran hanyalah penanda "punya siapa / di toko mana".
				for (int r = 0; r < RELASI_PENANDA.length; r++) {
					String nama = RELASI_PENANDA[r];
					if (!punyaProperti(meta, nama)) continue;
					try {
						Object relasi = meta.getPropertyValue(entitasBaris, nama, EntityMode.POJO);
						if (relasi == null) continue;
						ClassMetadata metaRelasi = HibernateUtil.getSessionFactory()
								.getClassMetadata(relasi.getClass());
						if (metaRelasi == null) continue;
						Serializable idRelasi = metaRelasi.getIdentifier(relasi, EntityMode.POJO);
						if (idRelasi != null) ringkas.put(nama, idRelasi);
					} catch (Throwable abaikan) {
						// relasi yang barisnya sudah lenyap -- dilewati, bukan digagalkan.
					}
				}
				j.put("ringkas", ringkas);
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("adaLagi", adaLagi ? Boolean.TRUE : Boolean.FALSE);
			hasil.put("bolehPulihkan", Common.getApakahAdminLain(tbmuser));
			hasil.put("kolom", kolomTersedia(meta));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static int indeksProperti(ClassMetadata meta, String nama) {
		if (meta == null || nama == null) return -1;
		String[] props = meta.getPropertyNames();
		for (int i = 0; i < props.length; i++) {
			if (props[i].equals(nama)) return i;
		}
		return -1;
	}

	private static boolean punyaProperti(ClassMetadata meta, String nama) {
		return indeksProperti(meta, nama) >= 0;
	}

	/** Properti bertipe teks -- satu-satunya yang aman dipakai LIKE. */
	private static boolean propertiTeks(ClassMetadata meta, String nama) {
		int i = indeksProperti(meta, nama);
		if (i < 0) return false;
		Type t = meta.getPropertyTypes()[i];
		return !t.isCollectionType() && !t.isEntityType()
				&& CharSequence.class.isAssignableFrom(t.getReturnedClass());
	}

	/**
	 * Kriteria pencarian kata kunci: OR dari LIKE pada properti TEKS saja.
	 *
	 * <p>Pembatasan ke properti teks bukan kerapian, melainkan syarat kebenaran --
	 * pelajaran yang sudah dibayar di versi ZK ({@code buildKeywordCriterion}).
	 * Envers tetap bersedia membangun {@code ... LIKE ?} untuk kolom Integer/Long,
	 * lalu Hibernate mem-binding parameternya memakai tipe kolom asli dan meledak
	 * dengan ClassCastException -- bukan saat query disusun, melainkan jauh
	 * kemudian saat dieksekusi. Properti non-teks dilewati di sini supaya kriteria
	 * yang pasti gagal tidak pernah lahir.</p>
	 */
	private static org.hibernate.envers.query.criteria.AuditCriterion kriteriaKataKunci(
			ClassMetadata meta, String kata) {
		if (meta == null || kata == null || kata.trim().length() == 0) return null;
		String bersih = kata.trim();
		org.hibernate.envers.query.criteria.AuditCriterion kriteria = null;
		String[] props = meta.getPropertyNames();
		for (int i = 0; i < props.length; i++) {
			if (!propertiTeks(meta, props[i]) || propertiSensitif(props[i])) continue;
			org.hibernate.envers.query.criteria.AuditCriterion satu = AuditEntity
					.property(props[i]).like(bersih, org.hibernate.criterion.MatchMode.ANYWHERE);
			kriteria = kriteria == null ? satu : AuditEntity.or(kriteria, satu);
		}
		return kriteria;
	}

	/** Kriteria "kolom tertentu bernilai X", tipe menentukan cara membandingkan. */
	private static org.hibernate.envers.query.criteria.AuditCriterion kriteriaKolom(
			ClassMetadata meta, String kolom, String nilai) {
		if (meta == null || kolom == null || kolom.trim().length() == 0) return null;
		if (nilai == null || nilai.trim().length() == 0) return null;
		String k = kolom.trim();
		String v = nilai.trim();
		if (propertiSensitif(k)) return null;
		int i = indeksProperti(meta, k);
		if (i < 0) return null;
		Type t = meta.getPropertyTypes()[i];
		try {
			if (t.isEntityType()) {
				// Relasi disaring lewat id-nya, bukan isinya.
				return AuditEntity.relatedId(k).eq(Long.valueOf(v));
			}
			Class kelas = t.getReturnedClass();
			if (CharSequence.class.isAssignableFrom(kelas)) {
				return AuditEntity.property(k).like(v, org.hibernate.criterion.MatchMode.ANYWHERE);
			}
			if (Boolean.class.isAssignableFrom(kelas) || boolean.class.equals(kelas)) {
				return AuditEntity.property(k).eq(Boolean.valueOf(
						"true".equalsIgnoreCase(v) || "1".equals(v) || "ya".equalsIgnoreCase(v)));
			}
			if (Date.class.isAssignableFrom(kelas)) {
				java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd");
				f.setLenient(false);
				Date d = f.parse(v.length() > 10 ? v.substring(0, 10) : v);
				java.util.Calendar c = java.util.Calendar.getInstance();
				c.setTime(d);
				c.set(java.util.Calendar.HOUR_OF_DAY, 23);
				c.set(java.util.Calendar.MINUTE, 59);
				c.set(java.util.Calendar.SECOND, 59);
				c.set(java.util.Calendar.MILLISECOND, 999);
				return AuditEntity.property(k).between(d, c.getTime());
			}
			if (Long.class.isAssignableFrom(kelas) || long.class.equals(kelas)) {
				return AuditEntity.property(k).eq(Long.valueOf(v));
			}
			if (Integer.class.isAssignableFrom(kelas) || int.class.equals(kelas)) {
				return AuditEntity.property(k).eq(Integer.valueOf(v));
			}
			if (Double.class.isAssignableFrom(kelas) || double.class.equals(kelas)) {
				return AuditEntity.property(k).eq(Double.valueOf(v));
			}
		} catch (Exception nilaiTakCocok) {
			// Nilai yang tidak bisa dikonversi ke tipe kolom lebih baik diabaikan
			// daripada dipaksakan menjadi query yang pasti meledak saat dieksekusi.
			return null;
		}
		return null;
	}

	/**
	 * Menyusun AuditQuery bersama untuk jelajah dan restore massal, supaya
	 * "yang terlihat di layar" dan "yang akan dipulihkan" tidak pernah berasal dari
	 * saringan yang berbeda -- perbedaan sekecil apa pun di antara keduanya berarti
	 * pengguna menekan Restore atas dasar daftar yang bukan itu.
	 */
	private static org.hibernate.envers.query.AuditQuery siapkanQuery(AuditReader reader,
			Class clazz, ClassMetadata meta, JSONObject request, Date dari, Date sampai,
			JSONObject hasil) throws Exception {
		// Rentang TANGGAL diterjemahkan lebih dulu ke rentang NOMOR revisi. Nomor
		// revisi terindeks, sedangkan menyaring tanggal di sisi Java berarti menarik
		// seluruh tabel audit ke memori.
		Number revAwal;
		try {
			revAwal = reader.getRevisionNumberForDate(dari);
		} catch (Exception belumAda) {
			// Belum ada revisi apa pun sebelum tanggal awal -> mulai dari paling awal.
			revAwal = Integer.valueOf(0);
		}
		Number revAkhir;
		try {
			revAkhir = reader.getRevisionNumberForDate(sampai);
		} catch (Exception belumAda) {
			// Tidak ada satu pun revisi sampai tanggal akhir. Dibuat mustahil terpenuhi
			// alih-alih dikembalikan null, supaya pemanggil tidak perlu punya jalur
			// khusus "kosong" yang bisa lupa disamakan perilakunya.
			revAkhir = Integer.valueOf(-1);
			revAwal = Integer.valueOf(0);
		}

		org.hibernate.envers.query.AuditQuery q = reader.createQuery()
				.forRevisionsOfEntity(clazz, false, true)
				.add(AuditEntity.revisionNumber().ge(revAwal))
				.add(AuditEntity.revisionNumber().le(revAkhir));

		RevisionType tipeSaring = tipeRevisi(request.optString("tipe", ""));
		if (tipeSaring != null) q.add(AuditEntity.revisionType().eq(tipeSaring));

		// Saringan satu baris. Adanya parameter ini membuat "Pulihkan baris ini"
		// memakai JALUR YANG SAMA PERSIS dengan "Pulihkan semua" -- hanya dengan
		// himpunan yang menyusut jadi satu. Tanpa itu, restore satuan dan massal
		// akan punya aturan pemilihan revisi sendiri-sendiri, dan cepat atau lambat
		// keduanya berbeda arti bagi orang yang menekannya.
		if (request != null && !request.isNull("id")) {
			String idSatu = (request.get("id") + "").trim();
			if (idSatu.length() > 0 && !"null".equals(idSatu)) {
				q.add(AuditEntity.id().eq(Long.valueOf(idSatu)));
			}
		}

		if (request != null && !request.isNull("toko")) {
			String t = (request.get("toko") + "").trim();
			if (t.length() > 0 && !"null".equals(t) && punyaProperti(meta, "toko")) {
				q.add(AuditEntity.relatedId("toko").eq(Long.valueOf(t)));
			}
		}

		org.hibernate.envers.query.criteria.AuditCriterion kunci =
				kriteriaKataKunci(meta, request.optString("kataKunci", ""));
		if (kunci != null) q.add(kunci);

		org.hibernate.envers.query.criteria.AuditCriterion kolom = kriteriaKolom(meta,
				request.optString("kolom", ""), request.optString("nilai", ""));
		if (kolom != null) q.add(kolom);

		return q;
	}

	/** Daftar kolom yang bisa dipakai menyaring -- dipakai klien utk mengisi combo. */
	private static JSONArray kolomTersedia(ClassMetadata meta) throws Exception {
		JSONArray arr = new JSONArray();
		if (meta == null) return arr;
		String[] props = meta.getPropertyNames();
		Type[] tipe = meta.getPropertyTypes();
		for (int i = 0; i < props.length; i++) {
			if (tipe[i].isCollectionType() || propertiSensitif(props[i])) continue;
			JSONObject j = new JSONObject();
			j.put("nama", props[i]);
			j.put("teks", propertiTeks(meta, props[i]));
			j.put("relasi", tipe[i].isEntityType());
			arr.put(j);
		}
		return arr;
	}

	/**
	 * Menyalin satu cuplikan revisi ke baris hidup, di dalam SATU transaksi
	 * tersendiri. Dipakai baik oleh restore satuan maupun restore massal supaya
	 * keduanya tidak pernah berbeda perilaku.
	 *
	 * <p>Transaksi sengaja per baris, bukan satu transaksi besar: pada restore
	 * massal, satu baris bermasalah tidak boleh menyeret seluruh sisanya ikut
	 * batal. Ini juga pola yang dipakai versi ZK.</p>
	 */
	private static JSONObject salinKeLive(Session session, ClassMetadata meta, Class clazz,
			Object snapshot, Serializable id) {
		JSONObject lapor = new JSONObject();
		org.hibernate.Transaction tx = null;
		try {
			Object target = session.get(clazz, id);
			boolean hidupkanLagi = (target == null);
			if (hidupkanLagi) {
				target = clazz.newInstance();
				meta.setIdentifier(target, id, EntityMode.POJO);
			}
			String[] props = meta.getPropertyNames();
			Type[] tipeProp = meta.getPropertyTypes();
			int dilewati = 0;
			for (int i = 0; i < props.length; i++) {
				if (tipeProp[i].isCollectionType() || propertiSensitif(props[i])) continue;
				try {
					Object nilai = meta.getPropertyValue(snapshot, props[i], EntityMode.POJO);
					if (tipeProp[i].isEntityType() && nilai != null) {
						Class kelasRelasi = tipeProp[i].getReturnedClass();
						ClassMetadata metaRelasi =
								HibernateUtil.getSessionFactory().getClassMetadata(kelasRelasi);
						Serializable idRelasi = metaRelasi.getIdentifier(nilai, EntityMode.POJO);
						nilai = idRelasi == null ? null : session.get(kelasRelasi, idRelasi);
						if (nilai == null && idRelasi != null) { dilewati++; continue; }
					}
					meta.setPropertyValue(target, props[i], nilai, EntityMode.POJO);
				} catch (Throwable abaikan) {
					dilewati++; // satu properti bermasalah tidak menggagalkan restore.
				}
			}
			tx = session.beginTransaction();
			if (hidupkanLagi) {
				session.replicate(target, org.hibernate.ReplicationMode.OVERWRITE);
			} else {
				session.saveOrUpdate(target);
			}
			tx.commit();
			lapor.put("ok", true);
			lapor.put("dihidupkanLagi", hidupkanLagi);
			lapor.put("propertiDilewati", dilewati);
		} catch (Exception e) {
			try {
				if (tx != null && tx.isActive()) tx.rollback();
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "RevisiApiHelper.salinKeLive rollback");
			}
			try {
				lapor.put("ok", false);
				lapor.put("pesan", e.getMessage() == null ? e.toString() : e.getMessage());
			} catch (Exception abaikan) {
				// menyusun laporan kegagalan tidak boleh ikut gagal.
			}
		}
		return lapor;
	}

	/**
	 * <h3>Restore MASSAL "data terbaru" untuk seluruh baris yang cocok dengan
	 * saringan -- meniru tombol "Restore Terbaru" pada {@code GenericRevisiHelper}
	 * (ZK).</h3>
	 *
	 * <p>Aturan pemilihan cuplikan sama persis dengan versi ZK, dan perlu dipahami
	 * sebelum dipakai:</p>
	 * <ol>
	 *   <li>revisi diurutkan dari yang TERBARU;</li>
	 *   <li>revisi bertipe HAPUS <b>dilewati</b>, lalu;</li>
	 *   <li>revisi pertama yang tersisa untuk tiap id dipakai.</li>
	 * </ol>
	 * <p>Artinya yang dipulihkan adalah keadaan terakhir baris itu <b>sebelum</b>
	 * dihapus -- bukan cuplikan penghapusannya. Itulah yang dimaksud "data terakhir
	 * yang dipakai".</p>
	 *
	 * <p><b>Beda yang disengaja dari versi ZK.</b> Bawaannya hanya menghidupkan
	 * baris yang benar-benar SUDAH TIDAK ADA. Baris yang masih hidup dilewati,
	 * kecuali {@code timpaYangMasihAda=true} diminta secara sadar. Versi ZK menimpa
	 * semuanya; untuk sapuan lintas ratusan baris lewat API, menimpa data yang masih
	 * dipakai orang adalah kerugian yang tidak bisa dibatalkan, sedangkan
	 * menghidupkan yang hilang tidak merusak apa pun.</p>
	 *
	 * <p><b>Batas yang harus diketahui.</b> Restore ini DANGKAL: hanya baris pada
	 * entitas yang dipilih. Versi ZK menelusuri dependensi secara rekursif; di sini
	 * tidak. Untuk data berinduk-anak (mis. pesanan dan itemnya), induknya dipulihkan
	 * lebih dulu, lalu anaknya dijalankan sebagai sapuan tersendiri -- relasi anak
	 * mencari baris hidup ber-id sama, jadi urutan itu memang berhasil.</p>
	 */
	@SuppressWarnings("unchecked")
	public static void pulihkanMassal(Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		if (tbmuser == null) { tolak(hasil, "Sesi tidak dikenali."); return; }
		if (!Common.getApakahAdminLain(tbmuser)) {
			tolak(hasil, "Restore massal hanya untuk ADMINISTRATOR.");
			return;
		}
		Class clazz = kelasDari(request, hasil);
		if (clazz == null) return;
		Date dari = batasTanggal(request, "dari", false, hasil);
		if (dari == null) return;
		Date sampai = batasTanggal(request, "sampai", true, hasil);
		if (sampai == null) return;
		if (dari.after(sampai)) { tolak(hasil, "Tanggal awal melewati tanggal akhir."); return; }
		boolean timpa = request.optBoolean("timpaYangMasihAda", false);
		// Mode hitung-dulu. Restore massal tidak punya tombol "batal": begitu baris
		// tertulis, keadaan sebelumnya hanya bisa dikejar lewat revisi baru. Jadi
		// pemanggil harus bisa menanyakan "berapa yang akan tersentuh" tanpa
		// menyentuh apa pun.
		boolean simulasi = request.optBoolean("simulasi", false);
		// Batas atas jumlah baris yang boleh dipulihkan dalam satu panggilan.
		// Tanpa batas, satu klik keliru pada rentang lebar bisa menulis ulang
		// ribuan baris -- dan tidak ada tombol "batal" untuk itu.
		int batas = Math.min(500, Math.max(1, request.optInt("batas", 200)));

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			AuditReader reader = AuditReaderFactory.get(session);
			ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(clazz);
			org.hibernate.envers.query.AuditQuery q = siapkanQuery(reader, clazz, meta, request,
					dari, sampai, hasil);
			if (q == null) return;
			q.addOrder(AuditEntity.revisionNumber().desc());
			// Disapu lebih lebar daripada batas restore: revisi HAPUS dan revisi
			// berulang pada id yang sama ikut terbaca dulu, baru disaring.
			q.setMaxResults(5000);
			List baris = q.getResultList();

			java.util.LinkedHashMap terpilih = new java.util.LinkedHashMap();
			for (int i = 0; i < baris.size(); i++) {
				Object[] b = (Object[]) baris.get(i);
				Object entitasBaris = b[0];
				org.hibernate.envers.DefaultRevisionEntity rev =
						(org.hibernate.envers.DefaultRevisionEntity) b[1];
				RevisionType tipe = (RevisionType) b[2];
				Date waktu = rev.getRevisionDate();
				if (waktu == null || waktu.before(dari) || waktu.after(sampai)) continue;
				// Cuplikan HAPUS dilewati: yang dicari keadaan terakhir SEBELUM dihapus.
				if (tipe == RevisionType.DEL || entitasBaris == null) continue;
				Serializable id;
				try {
					id = meta.getIdentifier(entitasBaris, EntityMode.POJO);
				} catch (Throwable takTerbaca) {
					continue;
				}
				if (id == null || terpilih.containsKey(id)) continue; // urut terbaru -> yg pertama menang.
				terpilih.put(id, entitasBaris);
			}

			int berhasil = 0;
			int dihidupkan = 0;
			int dilewatiMasihAda = 0;
			JSONArray gagal = new JSONArray();
			JSONArray rincian = new JSONArray();
			java.util.Iterator it = terpilih.entrySet().iterator();
			int diproses = 0;
			boolean terpotong = false;
			while (it.hasNext()) {
				if (diproses >= batas) { terpotong = true; break; }
				java.util.Map.Entry e = (java.util.Map.Entry) it.next();
				Serializable id = (Serializable) e.getKey();
				Object snapshot = e.getValue();
				if (!timpa && session.get(clazz, id) != null) {
					dilewatiMasihAda++;
					continue;
				}
				diproses++;
				if (simulasi) {
					JSONObject rs = new JSONObject();
					rs.put("id", id);
					rs.put("status", "AKAN DIPULIHKAN");
					rincian.put(rs);
					continue;
				}
				JSONObject lapor = salinKeLive(session, meta, clazz, snapshot, id);
				JSONObject r = new JSONObject();
				r.put("id", id);
				if (lapor.optBoolean("ok", false)) {
					berhasil++;
					if (lapor.optBoolean("dihidupkanLagi", false)) dihidupkan++;
					r.put("status", lapor.optBoolean("dihidupkanLagi", false) ? "DIHIDUPKAN" : "DIPERBARUI");
					r.put("propertiDilewati", lapor.optInt("propertiDilewati", 0));
				} else {
					r.put("status", "GAGAL");
					r.put("pesan", lapor.optString("pesan", "tidak diketahui"));
					gagal.put(r);
				}
				rincian.put(r);
				try {
					session.clear(); // baris berikutnya tidak boleh mewarisi keadaan baris ini.
				} catch (Exception abaikan) {
					// membersihkan sesi gagal bukan alasan menghentikan sapuan.
				}
			}

			hasil.put("status", "00");
			hasil.put("kandidat", terpilih.size());
			hasil.put("berhasil", berhasil);
			hasil.put("dihidupkan", dihidupkan);
			hasil.put("dilewatiMasihAda", dilewatiMasihAda);
			hasil.put("gagal", gagal.length());
			hasil.put("rincian", rincian);
			hasil.put("terpotong", terpotong ? Boolean.TRUE : Boolean.FALSE);
			hasil.put("simulasi", simulasi ? Boolean.TRUE : Boolean.FALSE);
			hasil.put("akanDipulihkan", simulasi ? diproses : berhasil);
			StringBuffer pesan = new StringBuffer();
			if (simulasi) {
				pesan.append("Simulasi: ").append(diproses).append(" baris AKAN dipulihkan");
			} else {
				pesan.append(berhasil).append(" baris dipulihkan (").append(dihidupkan)
						.append(" dihidupkan kembali)");
			}
			if (dilewatiMasihAda > 0) {
				pesan.append("; ").append(dilewatiMasihAda).append(" dilewati karena datanya masih ada");
			}
			if (gagal.length() > 0) {
				pesan.append("; ").append(gagal.length()).append(" gagal");
			}
			if (terpotong) {
				pesan.append("; dihentikan pada batas ").append(batas)
						.append(" baris -- jalankan lagi untuk sisanya");
			}
			pesan.append(".");
			hasil.put("description", pesan.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Dispatcher aksi revisi_*. */
	public static boolean proses(String action, Tbmuser tbmuser, JSONObject request,
			JSONObject hasil) throws Exception {
		if ("revisi_daftar".equals(action)) { daftar(tbmuser, request, hasil); return true; }
		if ("revisi_detail".equals(action)) { detail(tbmuser, request, hasil); return true; }
		if ("revisi_pulihkan".equals(action)) { pulihkan(tbmuser, request, hasil); return true; }
		if ("revisi_jelajah".equals(action)) { jelajah(tbmuser, request, hasil); return true; }
		if ("revisi_entitas".equals(action)) { entitas(tbmuser, request, hasil); return true; }
		if ("revisi_pulihkan_massal".equals(action)) { pulihkanMassal(tbmuser, request, hasil); return true; }
		return false;
	}
}

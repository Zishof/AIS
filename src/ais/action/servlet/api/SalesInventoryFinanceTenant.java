package ais.action.servlet.api;

/**
 * <h3>Jalur schema tenant untuk Kas/Jurnal &amp; Laba-Rugi (P4, helper keempat).</h3>
 *
 * <p>Jalur legacy di {@link SalesInventoryFinanceHelper} tidak diubah; kelas ini menyediakan
 * potongan SQL penggantinya dengan kolom keluaran yang sama dan berurutan sama.</p>
 *
 * <h4>Helper pertama yang menyentuh schema KETIGA</h4>
 * <p>Tiga helper sebelumnya hanya merujuk {@code koperasi} dan {@code library}. Helper ini juga
 * memakai <b>{@code akunting}</b> -- bagan akun dan jurnal. Di model tenant ketiganya menyatu ke
 * dalam satu schema tenant, sehingga bagan akun pun tidak lagi bercampur antar tenant.</p>
 *
 * <h4>Pemetaan</h4>
 * <table border="1">
 * <tr><th>Legacy</th><th>Tenant</th></tr>
 * <tr><td>{@code akunting.akun}</td><td>{@code akun}</td></tr>
 * <tr><td>{@code akunting.transaksi} (satu baris = satu baris jurnal)</td>
 *     <td>{@code jurnal} (kepala) + {@code jurnal_detail} (baris)</td></tr>
 * <tr><td>{@code sales_inventory}</td><td>{@code salesperson}</td></tr>
 * <tr><td>{@code anggota_koperasi}</td><td>{@code customer}</td></tr>
 * <tr><td>{@code piutang_customer_doc}</td><td>{@code piutang_customer}</td></tr>
 * <tr><td>{@code alokasi_penerimaan_piutang_customer}</td><td>{@code alokasi_penerimaan_piutang}</td></tr>
 * <tr><td>{@code nota_sales_biaya} + {@code kategori_biaya_sales}</td><td>{@code sales_trip_biaya}</td></tr>
 * <tr><td>{@code sales_order_lapangan(_item)} untuk HPP</td>
 *     <td>{@code faktur_penjualan(_detail)}</td></tr>
 * </table>
 *
 * <h4>Kategori biaya: join yang lenyap, bukan hilang</h4>
 * <p>Legacy menyimpan kategori biaya sebagai tabel tersendiri dan menautkannya
 * ({@code JOIN kategori_biaya_sales kb ON b.kategori = kb.id}). Model tenant menaruh
 * kategorinya <b>langsung</b> pada {@code sales_trip_biaya.kategori} bertipe
 * {@code varchar(64)}. Join-nya karena itu tidak perlu -- bukan tabel yang hilang, melainkan
 * normalisasi yang memang ditiadakan.</p>
 *
 * <h4>HPP ada di FAKTUR, bukan di order</h4>
 * <p>Legacy menyimpan {@code hpp_snapshot} pada baris <b>order</b> lapangan, lalu menyaring
 * {@code o.status IN ('SIAP_TAGIH','LUNAS')} untuk mendapatkan yang sudah layak dihitung.</p>
 * <p>Model tenant memisahkan order dari fakturnya: {@code sales_order_detail} <b>tidak punya</b>
 * kolom harga pokok sama sekali, sedangkan {@code faktur_penjualan_detail} punya
 * {@code harga_beli} per baris dan {@code faktur_penjualan} punya {@code hpp} di kepalanya.</p>
 * <p>Karena itu laba kotor dihitung dari <b>faktur</b>, bukan dari order. Menghitungnya dari
 * order akan menghasilkan nol harga pokok pada seluruh baris -- laba kotor yang sama dengan
 * omzet, dan terlihat wajar sampai ada yang memeriksanya.</p>
 *
 * <h4>Penjualan tunai: faktur TANPA piutang</h4>
 * <p>Legacy memisahkan omzet kredit ({@code piutang_customer_doc}) dari omzet tunai
 * ({@code nota_sales_kas} ber-{@code jenis = 'CASH_SALE'}). Model tenant tidak punya kolom
 * {@code jenis} pada setoran, dan memang tidak memerlukannya: penjualan tunai adalah
 * {@code faktur_penjualan} yang <b>tidak melahirkan</b> baris {@code piutang_customer}.</p>
 * <p>Polanya sama persis dengan sisi pembelian -- pembelian tunai adalah {@code pembelian}
 * tanpa {@code hutang_supplier}. Lihat {@code SalesInventoryPayableTenant}.</p>
 */
final class SalesInventoryFinanceTenant {

	private SalesInventoryFinanceTenant() {
	}

	/** Benar bila aktor ini dilayani schema tenant. */
	static boolean aktif(EbisnisActorContextResolver.ActorContext aktor) {
		return SalesInventoryTenantSchema.aktif(aktor);
	}

	/** Prefiks schema berikut titiknya. */
	static String skema(EbisnisActorContextResolver.ActorContext aktor) {
		return SalesInventoryTenantSchema.skema(aktor.tenant);
	}

	// ------------------------------------------------------------------ bagan akun

	/**
	 * Bagan akun. Kolom berurutan sama dengan legacy: id, kode, nama, keterangan, debitKredit,
	 * indukKode, indukNama.
	 *
	 * <p>{@code keterangan} tidak ada pada {@code akun} model tenant; kolomnya dikembalikan
	 * kosong agar bentuk JSON tetap. {@code parent} menjadi {@code induk_id}.</p>
	 *
	 * <h4>Kolom kelima wajib berupa BILANGAN, bukan huruf</h4>
	 * <p>Pembacanya memanggil {@code rs.getInt(5)}. {@code saldo_normal} pada model tenant
	 * bertipe {@code varchar}, dan {@code getInt} atas kolom teks berisi {@code 'D'}
	 * <b>melempar</b> {@code PSQLException: Bad value for type int} — juga saat kolomnya kosong.
	 * Karena itu pemetaannya dilakukan di SQL, bukan diserahkan ke driver.</p>
	 * <p>Sandinya mengikuti {@code Akun.DEBET = 1} dan {@code Akun.CREDIT = -1}, yakni pasangan
	 * yang dipakai laporan keuangan untuk <b>mengalikan</b> saldo dengan saldo normalnya. Ada
	 * jalur legacy lain ({@code KodeAkunApiHelper}) yang menulis {@code 2} untuk kredit ke kolom
	 * yang sama; perselisihan itu tidak dibawa masuk ke model tenant. Akun tanpa saldo normal
	 * memberi {@code 0} — persis yang dikembalikan {@code getInt} atas kolom legacy yang
	 * {@code NULL}.</p>
	 */
	static String selectCoa() {
		return "SELECT a.id, a.kode, a.nama, '', " + kolomSaldoNormal("a") + ", "
				+ "COALESCE(p.kode,''), COALESCE(p.nama,'')";
	}

	/** {@code saldo_normal} sebagai bilangan bersandi legacy: D=1, K=-1, tak diisi=0. */
	static String kolomSaldoNormal(String alias) {
		return "CASE WHEN " + alias + ".saldo_normal = '" + SALDO_DEBET + "' THEN 1"
				+ " WHEN " + alias + ".saldo_normal = '" + SALDO_KREDIT + "' THEN -1"
				+ " ELSE 0 END";
	}

	static String dasarCoa(String skema, String where) {
		return " FROM " + skema + "akun a LEFT JOIN " + skema + "akun p ON a.induk_id = p.id"
				+ where + " ORDER BY a.kode LIMIT 500";
	}

	// ------------------------------------------------------- kelas akun dan saldo normalnya

	/** Aset. Bertambah di debet. */
	static final String TIPE_ASET = "ASET";
	/** Kewajiban. Bertambah di kredit. */
	static final String TIPE_KEWAJIBAN = "KEWAJIBAN";
	/** Ekuitas. Bertambah di kredit. */
	static final String TIPE_EKUITAS = "EKUITAS";
	/** Pendapatan. Bertambah di kredit. */
	static final String TIPE_PENDAPATAN = "PENDAPATAN";
	/** Beban. Bertambah di debet. */
	static final String TIPE_BEBAN = "BEBAN";

	static final String SALDO_DEBET = "D";
	static final String SALDO_KREDIT = "K";

	/**
	 * Kelas akun yang diterima layar Master Akun tenant.
	 *
	 * <p>Nilainya adalah <b>kelas akuntansi</b>, bukan sandi tipe vendor. Bagan akun yang
	 * diunggah dari Accurate memakai sandi lain ({@code BANK}, {@code AREC}, {@code OEXP} …)
	 * yang disimpan jalur legacy pada {@code akun.tipe_akun}. Menerima kedua kosakata pada satu
	 * kolom akan membuat indeks {@code idx_akun_tipe} memuat campuran, dan laporan mana pun yang
	 * mengelompokkan menurut kelas akun tidak lagi punya dasar. Layar ini karena itu menolak
	 * nilai di luar daftar ini dan menyebutkan daftarnya pada pesan penolakan.</p>
	 */
	static final String[] TIPE_SAH = { TIPE_ASET, TIPE_KEWAJIBAN, TIPE_EKUITAS, TIPE_PENDAPATAN,
			TIPE_BEBAN };

	/** Benar bila {@code tipe} termasuk kelas akun yang diterima. */
	static boolean tipeSah(String tipe) {
		for (int i = 0; i < TIPE_SAH.length; i++) {
			if (TIPE_SAH[i].equals(tipe)) {
				return true;
			}
		}
		return false;
	}

	/** Daftar kelas akun untuk pesan penolakan. */
	static String daftarTipe() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < TIPE_SAH.length; i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(TIPE_SAH[i]);
		}
		return sb.toString();
	}

	/**
	 * Saldo normal bawaan menurut kelas akunnya.
	 *
	 * <p><b>Ini bawaan, bukan turunan.</b> {@code saldo_normal} tetap disimpan justru karena
	 * kelas akun tidak menentukannya: akun lawan seperti Akumulasi Penyusutan berkelas
	 * {@code ASET} tetapi bersaldo normal KREDIT. Repositori ini sudah memperlakukannya begitu —
	 * lihat {@code KodeAkunApiHelper.posisiDariTipeAccurate}, yang menempatkan {@code DEPR} pada
	 * sisi kredit bersama kewajiban dan ekuitas. Menurunkan saldo normal dari kelasnya akan
	 * membalik tanda setiap akun lawan pada laporan keuangan.</p>
	 */
	static String saldoNormalBawaan(String tipe) {
		return TIPE_ASET.equals(tipe) || TIPE_BEBAN.equals(tipe) ? SALDO_DEBET : SALDO_KREDIT;
	}

	/**
	 * Terjemahan {@code debet_credit} permintaan menjadi saldo normal tenant, atau {@code null}
	 * bila sandinya tidak dikenali.
	 *
	 * <p>Sisi kredit menerima {@code -1} maupun {@code 2}. Keduanya benar-benar beredar pada
	 * kolom legacy yang sama: {@code Akun.CREDIT} bernilai {@code -1}, sedangkan
	 * {@code KodeAkunApiHelper} menulis {@code 2}. Perselisihan itu milik schema bersama;
	 * di sini keduanya diterima pada masukan dan disimpan sebagai satu huruf.</p>
	 */
	static String saldoNormalDariSandi(int sandi) {
		if (sandi == 1) {
			return SALDO_DEBET;
		}
		if (sandi == -1 || sandi == 2) {
			return SALDO_KREDIT;
		}
		return null;
	}

	// ------------------------------------------------------------------ simpan bagan akun

	/** Keberadaan satu akun DI DALAM schema tenant, untuk jalur simpan. */
	static String adaAkun(String skema) {
		return "SELECT COUNT(*) FROM " + skema + "akun WHERE id = ?";
	}

	/** SATU kolom: {@code tipe} akun yang dirujuk. Dipakai mewarisi kelas dari induknya. */
	static String tipeAkun(String skema) {
		return "SELECT tipe FROM " + skema + "akun WHERE id = ?";
	}

	/**
	 * Pemakaian kode akun oleh baris LAIN. Dua parameter: kode, dan id yang dikecualikan
	 * (pakai {@code 0} saat menyisipkan, karena {@code id} tabel ini selalu positif).
	 *
	 * <p>Pemeriksaan ini hanya untuk pesan yang enak dibaca. Jaminannya tetap pada
	 * {@code uq_..._akun_kode}; dua permintaan serentak tetap berakhir pada pelanggaran
	 * batasan, bukan pada dua akun berkode sama.</p>
	 */
	static String kodeAkunDipakai(String skema) {
		return "SELECT nama FROM " + skema + "akun WHERE kode = ? AND id <> ? LIMIT 1";
	}

	/**
	 * Benar bila {@code induk} yang diusulkan berada di dalam keturunan {@code akun} — yaitu
	 * bila memasangnya akan membentuk lingkaran. Dua parameter: id akunnya, id induk usulan.
	 *
	 * <p>Kedalamannya dibatasi 64 supaya penelusuran tetap berhenti walau tabelnya sudah
	 * terlanjur berlingkar karena sebab lain.</p>
	 */
	static String indukBerlingkar(String skema) {
		return "WITH RECURSIVE turunan(id, dalam) AS ("
				+ " SELECT id, 1 FROM " + skema + "akun WHERE induk_id = ?"
				+ " UNION ALL"
				+ " SELECT a.id, t.dalam + 1 FROM " + skema + "akun a"
				+ " JOIN turunan t ON a.induk_id = t.id WHERE t.dalam < 64)"
				+ " SELECT COUNT(*) FROM turunan WHERE id = ?";
	}

	/**
	 * Penyisipan akun. ENAM parameter: kode, nama, tipe, indukId, saldoNormal, oleh.
	 *
	 * <p><b>{@code level} sengaja tidak diisi.</b> Ia adalah kedalaman pada pohon
	 * {@code induk_id} — turunan penuh, tanpa kekecualian, tidak seperti {@code saldo_normal}
	 * yang punya akun lawan. Menyimpannya berarti menanggung kebenarannya selamanya: memindahkan
	 * satu akun ke induk lain membuat level seluruh keturunannya salah, dan salahnya tidak
	 * kelihatan sampai ada yang menggambar pohonnya. Kolomnya dibiarkan {@code NULL}; pembaca
	 * yang memerlukan kedalaman menghitungnya dari {@code induk_id}.</p>
	 */
	static String sisipAkun(String skema) {
		return "INSERT INTO " + skema + "akun (kode, nama, tipe, induk_id, saldo_normal,"
				+ " posting_diizinkan, aktif, dibuat_pada, oleh)"
				+ " VALUES (?, ?, ?, ?, ?, true, true, now(), ?) RETURNING id";
	}

	/**
	 * Pembaruan akun pada jalur tenant. ENAM parameter: kode, nama, indukId, saldoNormal, oleh,
	 * id. Entitas {@code Akun} mematok {@code @Table(schema = "akunting")}, sehingga
	 * {@code session.saveOrUpdate()} akan menulis ke bagan akun bersama berapa pun tenant yang
	 * aktif.
	 *
	 * <p>{@code saldo_normal} dan {@code induk_id} memakai pola "ganti bila diberi": parameternya
	 * {@code NULL} berarti permintaan tidak menyebut kolom itu, dan nilainya dipertahankan.
	 * Tanpa itu, permintaan yang hanya mengubah nama akan menghapus induk dan saldo normal
	 * akun — jalur legacy pun hanya menyentuh kolom yang benar-benar dikirim.</p>
	 *
	 * <p>{@code tipe} tidak ikut diperbarui di sini. Kelas akun menentukan letak sebuah akun
	 * pada laporan keuangan; mengubahnya sesudah ada jurnal yang menunjuknya memindahkan angka
	 * yang sudah dilaporkan tanpa jejak apa pun. Pemindahan kelas adalah pekerjaan penataan
	 * bagan akun, bukan penyuntingan satu baris.</p>
	 */
	static String ubahAkun(String skema) {
		return "UPDATE " + skema + "akun SET kode = ?, nama = ?,"
				+ " induk_id = COALESCE(?, induk_id),"
				+ " saldo_normal = COALESCE(?, saldo_normal),"
				+ " tanggal_dirubah = now(), oleh = ? WHERE id = ?";
	}

	// ------------------------------------------------------------------ kas & jurnal

	/**
	 * Kas/Jurnal. Legacy membaca {@code akunting.transaksi} yang satu barisnya sudah berupa satu
	 * baris jurnal; model tenant memisahkan kepala ({@code jurnal}) dari barisnya
	 * ({@code jurnal_detail}), sehingga keduanya digabung kembali di sini.
	 *
	 * <p>Kolom berurutan sama dengan legacy: id, kode, jenisJurnal, tanggal, keterangan,
	 * akunKode, akunNama, debet, kredit, tanggalPosting.</p>
	 *
	 * <p>{@code id} yang dikembalikan adalah id <b>baris</b>, bukan id kepala -- sebab satu baris
	 * legacy setara satu baris jurnal, dan memakai id kepala akan membuat beberapa baris
	 * berbagi id yang sama.</p>
	 */
	static String selectJurnal() {
		return "SELECT d.id, COALESCE(j.nomor_dokumen,''), COALESCE(j.sumber_tipe,''), j.tanggal, "
				+ "COALESCE(NULLIF(d.keterangan,''), COALESCE(j.keterangan,'')), "
				+ "COALESCE(a.kode,''), COALESCE(a.nama,''), COALESCE(d.debit,0), "
				+ "COALESCE(d.kredit,0), j.diposting_pada";
	}

	static String dasarJurnal(String skema, String where) {
		return " FROM " + skema + "jurnal_detail d"
				+ " JOIN " + skema + "jurnal j ON d.jurnal_id = j.id"
				+ " LEFT JOIN " + skema + "akun a ON d.akun_id = a.id" + where
				+ " ORDER BY j.tanggal DESC, d.id DESC LIMIT 500";
	}

	/** Nama kolom tanggal jurnal pada model tenant, untuk menyusun saringan. */
	static String kolomTanggalJurnal() {
		return "j.tanggal";
	}

	/** Nama kolom akun pada baris jurnal, untuk menyusun saringan. */
	static String kolomAkunJurnal() {
		return "d.akun_id";
	}

	// ------------------------------------------------------------------ parameter laporan

	/** Daftar sales aktif untuk pemilih laporan. */
	static String sqlSalesperson(String skema) {
		return "SELECT s.id, s.kode, s.nama FROM " + skema + "salesperson s"
				+ " WHERE COALESCE(s.aktif,true) = true ORDER BY s.kode LIMIT 200";
	}

	// ------------------------------------------------------------------ laba kotor

	/**
	 * Laba kotor dari FAKTUR, bukan dari order -- lihat catatan kelas. Harga pokok diambil per
	 * baris ({@code harga_beli * kuantitas}), bukan dari {@code faktur_penjualan.hpp}, supaya
	 * pengelompokan per produk tetap mungkin.
	 */
	static String dasarLabaKotor(String skema, String joinGrup, String where) {
		return " FROM " + skema + "faktur_penjualan_detail i"
				+ " JOIN " + skema + "faktur_penjualan o ON i.faktur_penjualan_id = o.id"
				+ joinGrup + where;
	}

	static String joinCustomer(String skema) {
		return " JOIN " + skema + "customer c ON o.customer_id = c.id";
	}

	static String joinSalesperson(String skema) {
		return " LEFT JOIN " + skema + "salesperson s ON o.salesperson_id = s.id";
	}

	/** Harga pokok per baris faktur. */
	static String hppBaris() {
		return "COALESCE(i.harga_beli,0) * COALESCE(i.kuantitas,0)";
	}

	/**
	 * Saringan faktur yang layak masuk laporan. Legacy memakai
	 * {@code o.status IN ('SIAP_TAGIH','LUNAS')} atas ORDER; padanan tenant adalah faktur yang
	 * tidak dibatalkan -- keberadaan fakturnya sendiri sudah berarti transaksinya jadi.
	 */
	static String whereFakturSah() {
		return " WHERE COALESCE(o.dibatalkan,false) = false";
	}

	/**
	 * Pengelompokan per produk menuntut join ke master produk: baris faktur model tenant
	 * menyimpan {@code produk_id} saja, tanpa salinan namanya.
	 *
	 * <p>Legacy menyimpan {@code nama_produk} pada barisnya -- salinan yang membeku saat
	 * transaksi. Konsekuensinya nyata: bila produk berganti nama, laporan legacy tetap
	 * menampilkan nama lama sedangkan laporan tenant menampilkan nama sekarang. Lebih konsisten
	 * dengan master, tetapi berbeda -- dicatat, bukan disembunyikan.</p>
	 */
	static String joinProduk(String skema) {
		return " JOIN " + skema + "produk pr ON i.produk_id = pr.id";
	}

	/** Kolom pengelompokan dengan nama kolom tenant. */
	static String kolomGrup(String grup) {
		if ("customer".equals(grup)) {
			return "c.id, c.nama";
		}
		if ("sales".equals(grup)) {
			return "COALESCE(s.id,0), COALESCE(s.nama,'(tanpa sales)')";
		}
		return "i.produk_id, pr.nama";
	}

	/** Nama kolom sales pada faktur, untuk saringan. */
	static String kolomSales() {
		return "o.salesperson_id";
	}

	/** Nama kolom toko pada faktur, untuk saringan lingkup. */
	static String kolomToko() {
		return "o.toko_id";
	}

	/** Kuantitas, nilai jual, dan harga pokok teragregasi per kelompok. */
	static String kolomUkuran() {
		return "SUM(COALESCE(i.kuantitas,0)), SUM(COALESCE(i.total,0)), SUM(" + hppBaris() + ")";
	}

	// ------------------------------------------------------------------ laba rugi

	/**
	 * Omzet KREDIT: piutang yang masih tercatat.
	 *
	 * <p>{@code piutang_customer} model tenant <b>tidak punya</b> kolom toko, sedangkan jalur
	 * legacy menyaringnya langsung. Fakturnya di-{@code LEFT JOIN} supaya saringan lingkup toko
	 * tetap dapat ditegakkan -- membuang saringan itu berarti menyajikan piutang seluruh toko
	 * kepada pengguna yang lingkupnya satu toko.</p>
	 */
	static String sqlOmzetKredit(String skema, String where) {
		return "SELECT COALESCE(SUM(d.nilai),0), COUNT(*) FROM " + skema + "piutang_customer d"
				+ " LEFT JOIN " + skema + "faktur_penjualan f ON d.faktur_penjualan_id = f.id"
				+ where;
	}

	/** Nama kolom sales dan toko untuk saringan omzet kredit. */
	static String kolomSalesPiutang() {
		return "d.salesperson_id";
	}

	static String kolomTokoPiutang() {
		return "f.toko_id";
	}

	/**
	 * Omzet TUNAI: faktur yang tidak melahirkan piutang. Lihat catatan kelas -- model tenant
	 * tidak punya penanda {@code CASH_SALE}; ketiadaan piutangnya yang menjadi penandanya.
	 */
	static String sqlOmzetTunai(String skema, String dari, String sampai, String saringan) {
		return "SELECT COALESCE(SUM(f.total),0) FROM " + skema + "faktur_penjualan f"
				+ " LEFT JOIN " + skema + "piutang_customer p ON p.faktur_penjualan_id = f.id"
				+ " WHERE p.id IS NULL AND COALESCE(f.dibatalkan,false) = false"
				+ " AND f.tanggal >= " + dari + " AND f.tanggal < (" + sampai + " + 1)" + saringan;
	}

	/** Harga pokok penjualan pada periode, dari baris faktur. */
	static String sqlHpp(String skema, String where) {
		return "SELECT COALESCE(SUM(" + hppBaris() + "),0)"
				+ " FROM " + skema + "faktur_penjualan_detail i"
				+ " JOIN " + skema + "faktur_penjualan o ON i.faktur_penjualan_id = o.id" + where;
	}

	/**
	 * Beban per kategori. Tanpa join sama sekali: kategorinya kolom langsung pada model tenant.
	 */
	static String sqlBeban(String skema, String dari, String sampai) {
		return "SELECT COALESCE(NULLIF(b.kategori,''),'(tanpa kategori)'), COALESCE(SUM(b.nilai),0)"
				+ " FROM " + skema + "sales_trip_biaya b"
				+ " WHERE b.tanggal >= " + dari + " AND b.tanggal < (" + sampai + " + 1)"
				+ " GROUP BY 1 ORDER BY 2 DESC";
	}

	// ------------------------------------------------------------------ rincian laba rugi

	/**
	 * Sisa piutang satu dokumen, dihitung dari alokasinya -- bukan dibaca dari {@code d.sisa}.
	 * Alasannya sama dengan sisi hutang: kolom ringkasan bisa basi, dan piutang yang basi berarti
	 * menagih pelanggan yang sudah membayar.
	 *
	 * <p>Legacy mengurangi {@code d.dibayar_awal} secara terpisah; pada model tenant uang muka
	 * sudah berupa alokasi penerimaan biasa, sehingga tercakup penjumlahan alokasi.</p>
	 */
	static String sisaPiutang(String skema) {
		return "(COALESCE(d.nilai,0) - COALESCE((SELECT SUM(a.nilai) FROM " + skema
				+ "alokasi_penerimaan_piutang a WHERE a.piutang_customer_id = d.id),0))";
	}

	/** Laba per baris faktur: nilai jual dikurangi harga pokoknya. */
	static String labaBaris() {
		return "(COALESCE(i.total,0) - " + hppBaris() + ")";
	}

	/**
	 * Rincian laba rugi per baris faktur. Kolom berurutan sama dengan legacy: salesNama, tanggal,
	 * nomor, namaProduk, jumlah, hpp, hargaSatuan, subtotal, laba, customerNama, sisaPiutang.
	 *
	 * <p>Nama produk ditarik lewat join -- baris faktur tenant tidak menyimpan salinannya.</p>
	 */
	static String selectRincian(String skema) {
		return "SELECT COALESCE(s.nama,'(tanpa sales)'), o.tanggal, "
				+ "COALESCE(NULLIF(d.nomor_faktur,''), COALESCE(o.nomor_dokumen,'')), "
				+ "pr.nama, COALESCE(i.kuantitas,0), COALESCE(i.harga_beli,0), "
				+ "COALESCE(i.harga_satuan,0), COALESCE(i.total,0), " + labaBaris() + ", "
				+ "COALESCE(c.nama,''), " + sisaPiutang(skema);
	}

	static String dasarRincian(String skema, String where) {
		return " FROM " + skema + "faktur_penjualan_detail i"
				+ " JOIN " + skema + "faktur_penjualan o ON i.faktur_penjualan_id = o.id"
				+ " JOIN " + skema + "produk pr ON i.produk_id = pr.id"
				+ " JOIN " + skema + "customer c ON o.customer_id = c.id"
				+ " LEFT JOIN " + skema + "salesperson s ON o.salesperson_id = s.id"
				+ " LEFT JOIN " + skema + "piutang_customer d ON d.faktur_penjualan_id = o.id"
				+ where + " ORDER BY o.tanggal, o.id, i.id LIMIT 500";
	}

	// ------------------------------------------------------------------ pemblokir

	/**
	 * <h4>Riwayat audit tidak dapat dilayani pada tenant berschema</h4>
	 *
	 * <p>{@code auditHistory} membaca lewat {@code org.hibernate.envers.AuditReader}. Envers
	 * menaruh seluruh barisnya pada satu schema yang ditetapkan
	 * {@code org.hibernate.envers.default_schema=new_audit} -- <b>statis per SessionFactory</b>,
	 * tidak dapat diarahkan per permintaan.</p>
	 *
	 * <p>Membiarkannya berjalan berarti menyajikan riwayat perubahan milik seluruh instalasi
	 * kepada satu tenant. Itu kebocoran, bukan sekadar hasil yang salah. Karena itu ditolak.</p>
	 *
	 * <p>Penggantinya adalah {@code TenantAuditWriter} beserta {@code <schema>__audit}, yang
	 * sudah berdiri tetapi belum punya pemanggil (P3, opsi audit tulis-tangan).</p>
	 */
	static boolean dukungRiwayatAudit() {
		return false;
	}
}

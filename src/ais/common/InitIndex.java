package ais.common;

public class InitIndex {

	private static final java.util.Set<String> NAMA_INDEX_SUDAH_DIPROSES = java.util.Collections
			.synchronizedSet(new java.util.HashSet<String>());

	private static final java.util.Set<String> SIGNATURE_INDEX_SUDAH_DIPROSES = java.util.Collections
			.synchronizedSet(new java.util.HashSet<String>());
	private static final Object KEBIJAKAN_RETUR_PRODUK_LOCK = new Object();

	// ── Eksekusi DDL PARALEL (best performance) ──────────────────────────────
	// Saat initEksekusiQueryIndex() berjalan, DDL_POOL aktif sehingga setiap eksekusiSql*
	// MENYUBMIT pekerjaan ke pool kecil (bounded) alih-alih sinkron. Gate dedup (nama+signature)
	// & pemilihan executor tetap dilakukan SEKUENSIAL di thread pemanggil (tidak ada race), hanya
	// eksekusi DB ke database yang berjalan paralel. CREATE INDEX IF NOT EXISTS bersifat idempoten,
	// jadi tetap aman. Ukuran pool sengaja kecil agar tidak menghabiskan pool koneksi / memicu
	// kontensi lock tabel saat banyak CREATE INDEX serentak.
	private static final int DDL_PARALEL_THREAD = 4;
	private static volatile java.util.concurrent.ExecutorService DDL_POOL = null;
	private static final java.util.List<java.util.concurrent.Future<?>> DDL_FUTURES = java.util.Collections
			.synchronizedList(new java.util.ArrayList<java.util.concurrent.Future<?>>());

	/** Submit satu pekerjaan DDL: ke pool bila paralel aktif, selain itu jalankan sinkron. */
	private static void submitDdl(Runnable tugas) {
		java.util.concurrent.ExecutorService pool = DDL_POOL;
		if (pool != null) {
			try {
				DDL_FUTURES.add(pool.submit(tugas));
				return;
			} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/InitIndex.java:30");
			}
		}
		tugas.run();
	}

	/** Tunggu seluruh DDL yang ter-submit selesai (dipanggil di akhir initEksekusiQueryIndex). */
	private static void tungguSemuaDdlSelesai() {
		java.util.List<java.util.concurrent.Future<?>> snapshot;
		synchronized (DDL_FUTURES) {
			snapshot = new java.util.ArrayList<java.util.concurrent.Future<?>>(DDL_FUTURES);
		}
		for (java.util.concurrent.Future<?> f : snapshot) {
			try {
				f.get();
			} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/InitIndex.java:45");
			}
		}
	}


	private static String normalisasiSqlIndex(String sql) {
		if (sql == null) {
			return "";
		}
		String normalized = sql.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ').trim();
		while (normalized.indexOf("  ") >= 0) {
			normalized = normalized.replace("  ", " ");
		}
		return normalized;
	}

	private static String ambilNamaIndex(String sql) {
		String normalized = normalisasiSqlIndex(sql);
		if (normalized.length() == 0) {
			return null;
		}

		String lower = normalized.toLowerCase(java.util.Locale.ENGLISH);
		String marker = "create index if not exists ";
		int pos = lower.indexOf(marker);
		if (pos < 0) {
			marker = "create unique index if not exists ";
			pos = lower.indexOf(marker);
		}
		if (pos < 0) {
			return null;
		}

		String rest = normalized.substring(pos + marker.length()).trim();
		if (rest.length() == 0) {
			return null;
		}

		int end = 0;
		while (end < rest.length()) {
			char c = rest.charAt(end);
			if (Character.isWhitespace(c) || c == '(') {
				break;
			}
			end++;
		}
		if (end <= 0) {
			return null;
		}

		String name = rest.substring(0, end).replace("\"", "").trim();
		int dot = name.lastIndexOf('.');
		if (dot >= 0 && dot < name.length() - 1) {
			name = name.substring(dot + 1);
		}

		return name.length() == 0 ? null : name.toLowerCase(java.util.Locale.ENGLISH);
	}


	private static String ambilSignatureIndex(String sql) {
		String normalized = normalisasiSqlIndex(sql);
		if (normalized.length() == 0) {
			return null;
		}

		String lower = normalized.toLowerCase(java.util.Locale.ENGLISH);
		String marker = "create index if not exists ";
		int pos = lower.indexOf(marker);
		if (pos < 0) {
			marker = "create unique index if not exists ";
			pos = lower.indexOf(marker);
		}
		if (pos < 0) {
			return null;
		}

		String beforeName = normalized.substring(0, pos + marker.length()).trim();
		String rest = normalized.substring(pos + marker.length()).trim();
		if (rest.length() == 0) {
			return null;
		}

		int end = 0;
		while (end < rest.length()) {
			char c = rest.charAt(end);
			if (Character.isWhitespace(c) || c == '(') {
				break;
			}
			end++;
		}
		if (end <= 0 || end >= rest.length()) {
			return null;
		}

		String afterName = rest.substring(end).trim();
		if (afterName.length() == 0) {
			return null;
		}

		return normalisasiSqlIndex(beforeName + " <index_name> " + afterName)
				.toLowerCase(java.util.Locale.ENGLISH);
	}

	private static boolean bolehEksekusiSqlIndex(String sql) {
		String namaIndex = ambilNamaIndex(sql);
		if (namaIndex == null) {
			return true;
		}

		/*
		 * CREATE INDEX IF NOT EXISTS memang aman terhadap index yang sudah ada di
		 * database. Namun jika satu file InitIndex memiliki nama index yang sama lebih
		 * dari satu kali, PostgreSQL hanya akan membuat yang pertama dan melewati yang
		 * berikutnya. Guard ini mencegah bentrokan nama index internal di InitIndex.
		 *
		 * Selain nama index, signature definisi index juga dicek. Tujuannya agar dua
		 * index dengan nama berbeda tetapi definisi tabel/kolom/where yang sama tidak
		 * dibuat ganda di file ini, karena index kembar akan memperlambat INSERT/UPDATE,
		 * termasuk update no_ujian dan cetak_kartu PMB.
		 */
		if (!NAMA_INDEX_SUDAH_DIPROSES.add(namaIndex)) {
			return false;
		}

		String signatureIndex = ambilSignatureIndex(sql);
		if (signatureIndex != null && !SIGNATURE_INDEX_SUDAH_DIPROSES.add(signatureIndex)) {
			return false;
		}

		return true;
	}

	private static final String[] TABEL_HIBERNATE_STREAMING = new String[] {
			"galeri_foto_image",
			"lampiran_lain",
			"lampiran_beasiswa_mahasiswa",
			"lampiran_kkn_mahasiswa",
			"lampiran_pkl_mahasiswa",
			"lampiran_lain_mahasiswa",
			"lampiran_lain_biodata_calon_mahasiswa",
			"upload_biodata_calon_mahasiswa_file_content",
			"upload_virtual_account_file_content",
			"audio_pertemuan",
			"video_pertemuan",
			"file_buku_bahan_ajar",
			"foto_mahasiswa",
			"foto_siswa",
			"foto_calon_siswa",
			"foto_calon_pegawai",
			"foto_mahasiswa_lulus",
			"foto_dosen",
			"foto_guru",
			"foto_admin",
			"foto_biodata_mahasiswa",
			"foto_biodata_calon_mahasiswa",
			"foto_pegawai",
			"tugas_file_content",
			"pertemuan_file_content",
			"report_history",
			"surat_jrxml_file",
			"file_buku_bahan_ajar_text",
			"foto_buku",
			"foto_bukti_checklist_laporan",
			"foto_item",
			"foto_gambar_item",
			"foto_gambar_produk",
			"foto_informasi_perpustakaan",
			"foto_gambar_kop_surat",
			"foto_gambar_tanda_tangan_pejabat",
			"foto_gambar_tanda_tangan_surat_keluar",
			"foto_gambar_surat_masuk",
			"foto_gambar_surat_keluar",
			"foto_informasi_rab",
			"foto_lampiran_pegawai",
			"foto_image_per_halaman_item" };


	/*
	 * MODE KOMPATIBILITAS POSTGRESQL LAMA
	 *
	 * PostgreSQL 11 ke atas mendukung CREATE INDEX ... INCLUDE (...).
	 * PostgreSQL 10 ke bawah akan error:
	 * ERROR: syntax error at or near "INCLUDE"
	 *
	 * Agar InitIndex tetap bisa jalan di database lama dan tidak membuat koneksi
	 * masuk status transaction aborted, semua SQL index dinormalisasi dulu sebelum
	 * dieksekusi. Default dibuat kompatibel PostgreSQL lama.
	 *
	 * Jika suatu saat semua database sudah PostgreSQL 11+, mode ini bisa dimatikan
	 * lewat JVM option:
	 * -Dais.initindex.pg.old.compat=false
	 */
	private static final boolean MODE_INDEX_POSTGRES_LAMA_TANPA_INCLUDE = !"false"
			.equalsIgnoreCase(System.getProperty("ais.initindex.pg.old.compat", "true"));

	private static boolean isKarakterTokenSql(char c) {
		return Character.isLetterOrDigit(c) || c == '_';
	}

	private static int indexOfKataIgnoreCase(String sumber, String kata, int mulai) {
		if (sumber == null || kata == null || kata.length() == 0) {
			return -1;
		}
		String lowerSumber = sumber.toLowerCase(java.util.Locale.ENGLISH);
		String lowerKata = kata.toLowerCase(java.util.Locale.ENGLISH);
		int posisi = mulai < 0 ? 0 : mulai;

		while (posisi < lowerSumber.length()) {
			int idx = lowerSumber.indexOf(lowerKata, posisi);
			if (idx < 0) {
				return -1;
			}

			boolean batasKiri = idx == 0 || !isKarakterTokenSql(lowerSumber.charAt(idx - 1));
			int akhir = idx + lowerKata.length();
			boolean batasKanan = akhir >= lowerSumber.length() || !isKarakterTokenSql(lowerSumber.charAt(akhir));

			if (batasKiri && batasKanan) {
				return idx;
			}
			posisi = idx + lowerKata.length();
		}
		return -1;
	}

	private static int cariTutupKurungSql(String sql, int posisiBukaKurung) {
		if (sql == null || posisiBukaKurung < 0 || posisiBukaKurung >= sql.length()
				|| sql.charAt(posisiBukaKurung) != '(') {
			return -1;
		}

		int kedalaman = 0;
		boolean dalamPetikSatu = false;
		boolean dalamPetikDua = false;

		for (int i = posisiBukaKurung; i < sql.length(); i++) {
			char c = sql.charAt(i);

			if (c == '\'' && !dalamPetikDua) {
				/*
				 * PostgreSQL escape quote dalam string: ''.
				 * Lewati pasangan quote agar status string tidak terbalik dua kali.
				 */
				if (dalamPetikSatu && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
					i++;
					continue;
				}
				dalamPetikSatu = !dalamPetikSatu;
			} else if (c == '"' && !dalamPetikSatu) {
				dalamPetikDua = !dalamPetikDua;
			}

			if (dalamPetikSatu || dalamPetikDua) {
				continue;
			}

			if (c == '(') {
				kedalaman++;
			} else if (c == ')') {
				kedalaman--;
				if (kedalaman == 0) {
					return i;
				}
			}
		}
		return -1;
	}

	private static String hapusIncludeIndexPostgresLama(String sql) {
		if (sql == null || sql.length() == 0) {
			return sql;
		}

		String lower = sql.toLowerCase(java.util.Locale.ENGLISH);
		if (lower.indexOf("include") < 0) {
			return sql;
		}

		StringBuffer hasil = new StringBuffer();
		int posisiBaca = 0;
		boolean berubah = false;

		while (posisiBaca < sql.length()) {
			int idxInclude = indexOfKataIgnoreCase(sql, "include", posisiBaca);
			if (idxInclude < 0) {
				hasil.append(sql.substring(posisiBaca));
				break;
			}

			int posisiKurung = idxInclude + "include".length();
			while (posisiKurung < sql.length() && Character.isWhitespace(sql.charAt(posisiKurung))) {
				posisiKurung++;
			}

			/*
			 * Hanya hapus klausa INCLUDE yang benar-benar berbentuk:
			 * INCLUDE (...)
			 * Jika kata include muncul pada komentar/string biasa, biarkan aman.
			 */
			if (posisiKurung >= sql.length() || sql.charAt(posisiKurung) != '(') {
				hasil.append(sql.substring(posisiBaca, idxInclude + "include".length()));
				posisiBaca = idxInclude + "include".length();
				continue;
			}

			int posisiTutup = cariTutupKurungSql(sql, posisiKurung);
			if (posisiTutup < 0) {
				hasil.append(sql.substring(posisiBaca));
				break;
			}

			hasil.append(sql.substring(posisiBaca, idxInclude));
			posisiBaca = posisiTutup + 1;
			berubah = true;
		}

		if (!berubah) {
			return sql;
		}

		return normalisasiSqlIndex(hasil.toString());
	}

	private static String sqlKompatibelPostgresLama(String sql) {
		if (!MODE_INDEX_POSTGRES_LAMA_TANPA_INCLUDE) {
			return sql;
		}
		return hapusIncludeIndexPostgresLama(sql);
	}

	/*
	 * MODE KOMPATIBILITAS "IF NOT EXISTS" PADA CREATE INDEX (PostgreSQL < 9.5)
	 *
	 * Klausa CREATE INDEX ... IF NOT EXISTS / CREATE UNIQUE INDEX ... IF NOT EXISTS baru
	 * didukung PostgreSQL 9.5 ke atas. Pada database yang lebih lama (mis. PostgreSQL 9.3,
	 * lihat catatan kompatibilitas to_regclass di modul lain), statement ini gagal dengan
	 * "ERROR: syntax error at or near NOT" persis pada posisi kata NOT -- parser lama tidak
	 * mengenal klausa tsb sama sekali.
	 *
	 * FIX: tulis ulang statement "CREATE [UNIQUE] INDEX IF NOT EXISTS <nama> ON ..." menjadi
	 * blok DO $$ ... $$ yang mengecek keberadaan index lewat pg_class/pg_namespace lebih
	 * dulu (tersedia sejak PostgreSQL lama), baru menjalankan CREATE INDEX polos (tanpa IF
	 * NOT EXISTS) via EXECUTE bila index memang belum ada -- perilaku akhir tetap idempoten
	 * persis seperti IF NOT EXISTS asli, hanya kompatibel ke belakang.
	 *
	 * PENTING: transform ini HANYA dipakai tepat sebelum SQL dikirim ke database (di dalam
	 * eksekusiSql*), BUKAN sebelum bolehEksekusiSqlIndex()/ambilNamaIndex() -- fungsi dedup
	 * tsb butuh melihat teks asli "create index if not exists" untuk mendeteksi nama index,
	 * supaya guard anti-bentrok nama/definisi index di file ini tetap jalan.
	 *
	 * Bisa dimatikan lewat JVM option -Dais.initindex.pg.old.ifnotexists.compat=false jika
	 * suatu saat semua database sudah PostgreSQL 9.5+.
	 */
	private static final boolean MODE_INDEX_POSTGRES_LAMA_TANPA_IF_NOT_EXISTS = !"false"
			.equalsIgnoreCase(System.getProperty("ais.initindex.pg.old.ifnotexists.compat", "true"));

	private static String sqlKompatibelIndexIfNotExistsPostgresLama(String sql) {
		if (!MODE_INDEX_POSTGRES_LAMA_TANPA_IF_NOT_EXISTS || sql == null) {
			return sql;
		}

		String normalized = normalisasiSqlIndex(sql);
		String lower = normalized.toLowerCase(java.util.Locale.ENGLISH);

		// WAJIB startsWith (bukan indexOf/contains): sebagian kecil statement di file ini
		// sudah berupa blok "DO $$ ... EXECUTE 'CREATE INDEX IF NOT EXISTS ...' ... $$" --
		// marker itu MUNCUL DI DALAM string literal EXECUTE, bukan di awal statement. Kalau
		// dicocokkan dengan indexOf, hasil substring-nya akan memotong dari tengah string
		// literal ber-quote tersebut dan menghasilkan SQL yang rusak/tidak seimbang tanda
		// kutipnya. Statement DO-block semacam itu sengaja DIBIARKAN tidak diubah di sini
		// (kasusnya sedikit dan sudah dibungkus try-catch "index opsional" di pemanggilnya).
		boolean unique = false;
		String marker = "create index if not exists ";
		boolean cocok = lower.startsWith(marker);
		if (!cocok) {
			marker = "create unique index if not exists ";
			cocok = lower.startsWith(marker);
			unique = true;
		}
		if (!cocok) {
			// Bukan pola CREATE [UNIQUE] INDEX IF NOT EXISTS di awal statement (mis. sudah
			// berupa blok DO $$ ... $$, atau DROP INDEX) -- biarkan apa adanya.
			return sql;
		}
		int pos = 0;

		String namaIndex = ambilNamaIndex(normalized);
		if (namaIndex == null) {
			// Tidak bisa mengekstrak nama index dengan aman -- jangan diubah, lebih aman
			// biarkan gagal apa adanya drpd salah tulis ulang SQL.
			return sql;
		}

		String bareCreate = (unique ? "CREATE UNIQUE INDEX " : "CREATE INDEX ")
				+ normalized.substring(pos + marker.length());
		String bareCreateEscaped = bareCreate.replace("'", "''");
		String namaIndexEscaped = namaIndex.replace("'", "''");

		return "DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n "
				+ "ON n.oid = c.relnamespace WHERE c.relname = '" + namaIndexEscaped
				+ "' AND c.relkind = 'i') THEN EXECUTE '" + bareCreateEscaped + "'; END IF; END $$;";
	}

	private static boolean containsSqlTableName(String lowerSql, String tableName) {
		if (lowerSql == null || tableName == null || tableName.length() == 0) {
			return false;
		}
		return lowerSql.indexOf(" " + tableName + " ") >= 0
				|| lowerSql.indexOf(" " + tableName + "(") >= 0
				|| lowerSql.indexOf(" " + tableName + ")") >= 0
				|| lowerSql.indexOf("." + tableName + " ") >= 0
				|| lowerSql.indexOf("." + tableName + "(") >= 0
				|| lowerSql.indexOf("." + tableName + ")") >= 0
				|| lowerSql.endsWith(" " + tableName)
				|| lowerSql.endsWith("." + tableName);
	}

	private static boolean isOutOfMemory(Throwable e) {
		Throwable t = e;
		int guard = 0;
		while (t != null && guard++ < 12) {
			String msg = t.getMessage() == null ? "" : t.getMessage().toLowerCase(java.util.Locale.ENGLISH);
			if (msg.indexOf("out of memory") >= 0) {
				return true;
			}
			t = t.getCause();
		}
		return false;
	}

	private static boolean isSqlTabelHibernateStreaming(String sql) {
		if (sql == null) {
			return false;
		}
		String lower = normalisasiSqlIndex(sql).toLowerCase(java.util.Locale.ENGLISH);
		for (int i = 0; i < TABEL_HIBERNATE_STREAMING.length; i++) {
			if (containsSqlTableName(lower, TABEL_HIBERNATE_STREAMING[i])) {
				return true;
			}
		}
		return false;
	}

	private static void eksekusiSql(String sql) throws Exception {
		final String s = sqlKompatibelPostgresLama(sql);
		if (bolehEksekusiSqlIndex(s)) {
			final String sEksekusi = sqlKompatibelIndexIfNotExistsPostgresLama(s);
			final boolean streaming = isSqlTabelHibernateStreaming(s);
			submitDdl(new Runnable() {
				@Override
				public void run() {
					try {
						if (streaming) {
							ais.common.Common.updateSqlStreaming(sEksekusi);
						} else {
							ais.common.Common.updateSql(sEksekusi);
						}
					} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/InitIndex.java:417");
					}
				}
			});
		}
	}

	private static void eksekusiSql10Menit(String sql) throws Exception {
		final String s = sqlKompatibelPostgresLama(sql);
		if (bolehEksekusiSqlIndex(s)) {
			final String sEksekusi = sqlKompatibelIndexIfNotExistsPostgresLama(s);
			final boolean streaming = isSqlTabelHibernateStreaming(s);
			submitDdl(new Runnable() {
				@Override
				public void run() {
					try {
						if (streaming) {
							ais.common.Common.updateSqlStreaming(sEksekusi);
						} else {
							ais.common.Common.updateSql10Menit(sEksekusi);
						}
					} catch (Throwable abaikan) {
						// ANALYZE tabel besar (mis. pertemuan/detailperkuliahan): meski timeout sudah
						// diperpanjang jadi 10 menit (lihat updateSql10Menit), tetap bisa gagal pada
						// instalasi dengan tabel sangat besar/beban tinggi. Jangan hentikan startup
						// aplikasi lain di thread ini — cukup log peringatan yang informatif supaya
						// admin tahu statistik planner tabel tsb belum ter-update.
						if (sEksekusi != null && sEksekusi.trim().toUpperCase(java.util.Locale.ENGLISH).startsWith("ANALYZE")) {
							System.err.println("PERINGATAN: " + sEksekusi
									+ " gagal walau statement_timeout sudah diperpanjang (10 menit). "
									+ "Statistik planner tabel ini mungkin belum ter-update. Penyebab: "
									+ abaikan.getMessage());
						}
						// KE-FIX (PSQLException "out of memory" saat CREATE INDEX ... USING gin (... gin_trgm_ops)
						// pada tabel besar, mis. disposisi_alur_sop.properti): ini keterbatasan resource
						// server Postgres (maintenance_work_mem terlalu kecil utk membangun index GIN
						// trigram di kolom tsb), BUKAN bug kode -- statement-nya sendiri sudah benar & sudah
						// idempoten (IF NOT EXISTS). Tetap ditelan (jangan hentikan startup aplikasi lain di
						// thread ini) tapi beri diagnosis yang jelas & actionable, drpd cuma masuk log generik
						// yg sulit ditelusuri operator.
						if (isOutOfMemory(abaikan)) {
							System.err.println("PERINGATAN: " + sEksekusi
									+ " gagal karena server PostgreSQL kehabisan memori saat membangun index. "
									+ "Index ini TIDAK terbentuk (pencarian pada kolom terkait akan tetap berjalan, "
									+ "hanya lebih lambat). Solusi: naikkan parameter 'maintenance_work_mem' pada "
									+ "konfigurasi server PostgreSQL (postgresql.conf) lalu jalankan ulang statement "
									+ "ini secara manual, atau restart aplikasi setelah parameter dinaikkan.");
						}
						ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/InitIndex.java:437");
					}
				}
			});
		}
	}

	private static void eksekusiSqlStreaming(String sql) throws Exception {
		final String s = sqlKompatibelPostgresLama(sql);
		if (bolehEksekusiSqlIndex(s)) {
			final String sEksekusi = sqlKompatibelIndexIfNotExistsPostgresLama(s);
			submitDdl(new Runnable() {
				@Override
				public void run() {
					try {
						ais.common.Common.updateSqlStreaming(sEksekusi);
					} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/InitIndex.java:452");
					}
				}
			});
		}
	}

	private static void eksekusiSqlAmanDdl(String sql) throws Exception {
		final String s = sqlKompatibelPostgresLama(sql);
		if (bolehEksekusiSqlIndex(s)) {
			final String sEksekusi = sqlKompatibelIndexIfNotExistsPostgresLama(s);
			submitDdl(new Runnable() {
				@Override
				public void run() {
					try {
						ais.common.Common.updateSql(sEksekusi, 600, true);
					} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/InitIndex.java:467");
					}
				}
			});
		}
	}

	private static void initIndexPerpustakaanCoverDanPmbKuota() {
		/*
		 * INDEX TAMBAHAN:
		 * 1. Perpustakaan: mempercepat LibraryUtil.generateImage(item) mengambil cover
		 *    FotoGambarItem pertama berdasarkan item dan id paling awal.
		 * 2. PMB: mempercepat validasi kuota PaketJurusanPmb berdasarkan paket, prodi,
		 *    tahun akademik, dan gelombang pendaftaran.
		 *
		 * Nama index dibuat unik dan dicek melalui bolehEksekusiSqlIndex(...) agar tidak
		 * bentrok dengan CREATE INDEX IF NOT EXISTS lain di file ini.
		 */
		String[] indexQueries = new String[] {
				"CREATE INDEX IF NOT EXISTS idx_foto_gambar_item_item_id ON public.foto_gambar_item (item, id)",

				// REDUNDAN (prefix dari idx_paket_has_jurusan_paket_kuota (paket,jurusan,kuota)) → di-DROP.
				"DROP INDEX IF EXISTS idx_paket_has_jurusan_paket_jurusan",

				"CREATE INDEX IF NOT EXISTS idx_paket_has_jurusan_paket_kuota "
						+ "ON public.paket_has_jurusan (paket, jurusan, kuota)",

				"CREATE INDEX IF NOT EXISTS idx_bcm_kuota_paket_ta_gel "
						+ "ON public.biodata_calon_mahasiswa (paket_registrasi_mahasiswa, tahunakademik, gelombang_pendaftaran, aktif) "
						+ "WHERE (aktif = true OR aktif IS NULL) AND (ditolak = false OR ditolak IS NULL) AND (mundur = false OR mundur IS NULL)",

				"CREATE INDEX IF NOT EXISTS idx_bcm_kuota_prodi1 "
						+ "ON public.biodata_calon_mahasiswa (prodi_1, paket_registrasi_mahasiswa, tahunakademik, gelombang_pendaftaran) "
						+ "WHERE prodi_1 IS NOT NULL AND (aktif = true OR aktif IS NULL) AND (ditolak = false OR ditolak IS NULL) AND (mundur = false OR mundur IS NULL)",

				"CREATE INDEX IF NOT EXISTS idx_bcm_kuota_prodi2 "
						+ "ON public.biodata_calon_mahasiswa (prodi_2, paket_registrasi_mahasiswa, tahunakademik, gelombang_pendaftaran) "
						+ "WHERE prodi_2 IS NOT NULL AND (aktif = true OR aktif IS NULL) AND (ditolak = false OR ditolak IS NULL) AND (mundur = false OR mundur IS NULL)",

				"CREATE INDEX IF NOT EXISTS idx_bcm_kuota_prodi3 "
						+ "ON public.biodata_calon_mahasiswa (prodi3, paket_registrasi_mahasiswa, tahunakademik, gelombang_pendaftaran) "
						+ "WHERE prodi3 IS NOT NULL AND (aktif = true OR aktif IS NULL) AND (ditolak = false OR ditolak IS NULL) AND (mundur = false OR mundur IS NULL)",

				"CREATE INDEX IF NOT EXISTS idx_bcm_kuota_prodi4 "
						+ "ON public.biodata_calon_mahasiswa (prodi4, paket_registrasi_mahasiswa, tahunakademik, gelombang_pendaftaran) "
						+ "WHERE prodi4 IS NOT NULL AND (aktif = true OR aktif IS NULL) AND (ditolak = false OR ditolak IS NULL) AND (mundur = false OR mundur IS NULL)",

				"CREATE INDEX IF NOT EXISTS idx_bcm_kuota_prodi5 "
						+ "ON public.biodata_calon_mahasiswa (prodi5, paket_registrasi_mahasiswa, tahunakademik, gelombang_pendaftaran) "
						+ "WHERE prodi5 IS NOT NULL AND (aktif = true OR aktif IS NULL) AND (ditolak = false OR ditolak IS NULL) AND (mundur = false OR mundur IS NULL)",

				"CREATE INDEX IF NOT EXISTS idx_bcm_kuota_prodi_lulus "
						+ "ON public.biodata_calon_mahasiswa (prodi_lulus, paket_registrasi_mahasiswa, tahunakademik, gelombang_pendaftaran) "
						+ "WHERE prodi_lulus IS NOT NULL AND (aktif = true OR aktif IS NULL) AND (ditolak = false OR ditolak IS NULL) AND (mundur = false OR mundur IS NULL)" };

		for (String sql : indexQueries) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:525");
				/*
				 * Optional index: beberapa database lama mungkin belum selesai schema update
				 * Hibernate untuk kolom kuota/kuota_berlaku_per_gelombang. Jangan blokir
				 * startup aplikasi.
				 */
			}
		}
	}


	private static void initIndexPmbPortalDanNomorUjianSuperFast() {
		/*
		 * INDEX KHUSUS PMB/SPMB
		 *
		 * Fokus optimasi:
		 * 1. Generator nomor ujian yang biasanya mencari nomor terakhir berdasarkan
		 *    tahun, gelombang, paket, dan prodi.
		 * 2. Cetak kartu ujian, status cetak kartu, dan lookup no_ujian.
		 * 3. Halaman sukses login PMB yang membaca status pembayaran, biodata, berkas,
		 *    parameter verifikasi, dan ruang ujian.
		 * 4. Mengurangi full scan pada tabel biodata_calon_mahasiswa saat dashboard atau
		 *    validasi PMB memfilter tahun/gelombang/prodi/status.
		 *
		 * Catatan:
		 * - Update row by id tetap mengandalkan primary key. Index tambahan tidak dapat
		 *   menghilangkan row-lock jika ada dua transaksi menulis row yang sama.
		 * - Karena no_ujian dan cetak_kartu ikut di-update, index pada dua kolom ini
		 *   dibuat selektif/terarah saja supaya manfaat baca tetap besar tetapi biaya
		 *   tulis tidak berlebihan.
		 * - Nama index memakai prefix idx_pmb_fast_* agar tidak bentrok dengan index
		 *   dashboard/deposit/kuota yang sudah ada.
		 */
		String[] indexQueries = new String[] {
				// Lookup langsung nomor ujian dan pencarian ulang jika peserta mencetak kartu.
				"CREATE INDEX IF NOT EXISTS idx_pmb_fast_bcm_no_ujian_notnull "
						+ "ON public.biodata_calon_mahasiswa (no_ujian) "
						+ "WHERE no_ujian IS NOT NULL",

				// Generator nomor ujian berbasis tahun.
				"CREATE INDEX IF NOT EXISTS idx_pmb_fast_bcm_tahun_no_ujian_desc "
						+ "ON public.biodata_calon_mahasiswa (tahun, no_ujian DESC, id DESC) "
						+ "WHERE no_ujian IS NOT NULL",

				// Generator nomor ujian berbasis prodi diterima + tahun.
				"CREATE INDEX IF NOT EXISTS idx_pmb_fast_bcm_lulus_tahun_no_ujian_desc "
						+ "ON public.biodata_calon_mahasiswa (prodi_lulus, tahun, no_ujian DESC, id DESC) "
						+ "WHERE prodi_lulus IS NOT NULL AND no_ujian IS NOT NULL",

				// Generator nomor ujian berbasis gelombang + prodi diterima.
				"CREATE INDEX IF NOT EXISTS idx_pmb_fast_bcm_gel_lulus_no_ujian_desc "
						+ "ON public.biodata_calon_mahasiswa (gelombang_pendaftaran, prodi_lulus, no_ujian DESC, id DESC) "
						+ "WHERE gelombang_pendaftaran IS NOT NULL AND prodi_lulus IS NOT NULL AND no_ujian IS NOT NULL",

				// Generator nomor ujian berbasis paket + gelombang.
				"CREATE INDEX IF NOT EXISTS idx_pmb_fast_bcm_paket_gel_no_ujian_desc "
						+ "ON public.biodata_calon_mahasiswa (paket_registrasi_mahasiswa, gelombang_pendaftaran, no_ujian DESC, id DESC) "
						+ "WHERE paket_registrasi_mahasiswa IS NOT NULL AND gelombang_pendaftaran IS NOT NULL AND no_ujian IS NOT NULL",

				// Fallback jika prodi_lulus belum terisi dan generator memakai pilihan prodi pertama.
				"CREATE INDEX IF NOT EXISTS idx_pmb_fast_bcm_prodi1_tahun_no_ujian_desc "
						+ "ON public.biodata_calon_mahasiswa (prodi_1, tahun, no_ujian DESC, id DESC) "
						+ "WHERE prodi_1 IS NOT NULL AND no_ujian IS NOT NULL",

				// Status cetak kartu per periode/gelombang.
				"CREATE INDEX IF NOT EXISTS idx_pmb_fast_bcm_cetak_tahun_gel "
						+ "ON public.biodata_calon_mahasiswa (tahun, gelombang_pendaftaran, cetak_kartu, id DESC) "
						+ "WHERE cetak_kartu IS NOT NULL",

				// Filter daftar peserta PMB aktif per periode, gelombang, paket, dan status lulus.
				"CREATE INDEX IF NOT EXISTS idx_pmb_fast_bcm_filter_peserta "
						+ "ON public.biodata_calon_mahasiswa (tahun, gelombang_pendaftaran, paket_registrasi_mahasiswa, status_lulus, id DESC) "
						+ "WHERE (aktif = true OR aktif IS NULL) AND (ditolak = false OR ditolak IS NULL) AND (mundur = false OR mundur IS NULL)",

				// Lookup status pembayaran registrasi/daftar ulang dari profil peserta.
				"CREATE INDEX IF NOT EXISTS idx_pmb_fast_bcm_pembayaran_registrasi "
						+ "ON public.biodata_calon_mahasiswa (pembayaran_registrasi, id) "
						+ "WHERE pembayaran_registrasi IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_pmb_fast_bcm_pembayaran_daftar_ulang "
						+ "ON public.biodata_calon_mahasiswa (pembayaran_daftar_ulang, id) "
						+ "WHERE pembayaran_daftar_ulang IS NOT NULL",

				// Login/lookup peserta PMB. no_registrasi biasanya sudah unique, index ini untuk
				// variasi query yang turut membawa pin.
				"CREATE INDEX IF NOT EXISTS idx_pmb_fast_bcm_noreg_pin "
						+ "ON public.biodata_calon_mahasiswa (no_registrasi, pin) "
						+ "WHERE no_registrasi IS NOT NULL",

				// Pembayaran calon mahasiswa.
				"CREATE INDEX IF NOT EXISTS idx_pmb_fast_kegiatan_calon_lunas "
						+ "ON public.kegiatan (calon_mahasiswa, lunas, tahun_akademik, jenis_kegiatan, id DESC) "
						+ "WHERE calon_mahasiswa IS NOT NULL",

				// Relasi detail pembayaran calon mahasiswa.
				"CREATE INDEX IF NOT EXISTS idx_pmb_fast_detail_kegiatan_kegiatan_item "
						+ "ON public.detail_kegiatan (kegiatan, item_biaya, id) "
						+ "WHERE kegiatan IS NOT NULL",

				// Verifikasi berkas PMB di halaman sukses login dan validasi cetak kartu.
				"CREATE INDEX IF NOT EXISTS idx_pmb_fast_verberkas_bcm_status "
						+ "ON public.biodata_calon_mahasiswa_punya_verifikasi_berkas "
						+ "(biodata_calon_mahasiswa, uploaded, verified, id)",
				"CREATE INDEX IF NOT EXISTS idx_pmb_fast_verberkas_master_bcm "
						+ "ON public.biodata_calon_mahasiswa_punya_verifikasi_berkas "
						+ "(verifikasi_kelengkapan_calon_mahasiswa, biodata_calon_mahasiswa, id)",

				// Verifikasi parameter tambahan PMB.
				"CREATE INDEX IF NOT EXISTS idx_pmb_fast_verparam_bcm_status "
						+ "ON public.biodata_calon_mahasiswa_punya_verifikasi_parameter "
						+ "(biodata_calon_mahasiswa, verified, id)",
				"CREATE INDEX IF NOT EXISTS idx_pmb_fast_verparam_parameter_bcm "
						+ "ON public.biodata_calon_mahasiswa_punya_verifikasi_parameter "
						+ "(parameter_verifikasi_calon_mahasiswa, biodata_calon_mahasiswa, id)",

				// Verifikasi nilai mata pelajaran PMB.
				"CREATE INDEX IF NOT EXISTS idx_pmb_fast_vermapel_bcm_mapel "
						+ "ON public.biodata_calon_mahasiswa_punya_verifikasi_matapelajaran "
						+ "(biodata_calon_mahasiswa, matapelajaran_sekolah, id)",

				// Ruang ujian PMB. Kolom biodata_calon_mahasiswa tidak ada pada sebagian skema,
				// sehingga index dibuat dengan pengecekan metadata agar startup tidak memunculkan error 42703.
				"DO $$ BEGIN "
						+ "IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'ruang_paket_pmb' AND column_name = 'biodata_calon_mahasiswa') "
						+ "AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'ruang_paket_pmb' AND column_name = 'id') THEN "
						+ "EXECUTE 'CREATE INDEX IF NOT EXISTS idx_pmb_fast_ruang_paket_bcm ON public.ruang_paket_pmb (biodata_calon_mahasiswa, id)'; "
						+ "END IF; END $$",

				// Lampiran umum yang sering dipakai untuk preview/download berkas.
				"CREATE INDEX IF NOT EXISTS idx_pmb_fast_lampiran_ref_jenis_id "
						+ "ON public.lampiran_lain (ref, jenis, id DESC)",

				// Audit Envers: optional. Tabel audit tidak selalu aktif, sehingga dicek dulu.
				"DO $$ BEGIN "
						+ "IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'biodata_calon_mahasiswa_aud') "
						+ "AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'biodata_calon_mahasiswa_aud' AND column_name = 'id') "
						+ "AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'biodata_calon_mahasiswa_aud' AND column_name = 'rev') THEN "
						+ "EXECUTE 'CREATE INDEX IF NOT EXISTS idx_pmb_fast_bcm_aud_id_rev ON public.biodata_calon_mahasiswa_aud (id, rev DESC)'; "
						+ "END IF; END $$" };

		for (String sql : indexQueries) {
			try {
				String normalized = normalisasiSqlIndex(sql).toLowerCase(java.util.Locale.ENGLISH);
				if (normalized.startsWith("do $$")) {
					eksekusiSqlAmanDdl(sql);
				} else {
					eksekusiSql10Menit(sql);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:672");
				/*
				 * Optional index: beberapa instalasi lama mungkin belum memiliki sebagian tabel
				 * PMB/verifikasi/ruang ujian. Jangan blokir startup aplikasi.
				 */
			}
		}

		String[] analyzeQueries = new String[] { "ANALYZE public.biodata_calon_mahasiswa",
				"ANALYZE public.kegiatan", "ANALYZE public.detail_kegiatan",
				"ANALYZE public.biodata_calon_mahasiswa_punya_verifikasi_berkas",
				"ANALYZE public.biodata_calon_mahasiswa_punya_verifikasi_parameter",
				"ANALYZE public.biodata_calon_mahasiswa_punya_verifikasi_matapelajaran",
				"ANALYZE public.ruang_paket_pmb", "ANALYZE public.lampiran_lain" };
		for (String sql : analyzeQueries) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:689");
			}
		}
	}


	private static void initIndexDashboardStatistikPmbSuperFast() {
		/*
		 * INDEX DASBOR STATISTIK PMB/SPMB
		 *
		 * Disusun dari query di ais.action.master.pmb.statistik.*:
		 * - filter utama selalu tahunAkademik => kolom fisik tahunakademik.
		 * - semesterMulai opsional, tetapi saat dipilih menjadi filter kedua.
		 * - panel dashboard melakukan banyak COUNT/GROUP BY untuk prodi pilihan,
		 *   prodi diterima, NIM, no_ujian, tanggal daftar, gelombang, jalur masuk,
		 *   gender, propinsi, paket, dan asal sekolah.
		 *
		 * Catatan redundansi:
		 * - idx_bcm_dashboard_stats (prodi_lulus, aktif, tahun DESC) kurang cocok
		 *   untuk dashboard PMB baru karena query memimpin tahunakademik, bukan tahun.
		 *   Untuk pola prodi_lulus/tahun lama sudah ada idx_bcm_dash_pt_prodi_lulus_tahun.
		 */
		String[] sqlDropRedundan = new String[] {
				"DROP INDEX IF EXISTS idx_bcm_dashboard_stats"
		};
		for (String sql : sqlDropRedundan) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e,
						"auto-audit(empty-catch) src/ais/common/InitIndex.java:initIndexDashboardStatistikPmbSuperFast-drop");
			}
		}

		String[] indexQueries = new String[] {
				// Count dasar KPI: total pendaftar per tahun akademik dan semester.
				"CREATE INDEX IF NOT EXISTS idx_pmb_dash_bcm_ta_sem "
						+ "ON public.biodata_calon_mahasiswa (tahunakademik, semester_mulai, id)",

				// KPI peserta ujian, lulus/diterima, dan sudah menjadi mahasiswa.
				"CREATE INDEX IF NOT EXISTS idx_pmb_dash_bcm_ta_sem_no_ujian "
						+ "ON public.biodata_calon_mahasiswa (tahunakademik, semester_mulai, no_ujian) "
						+ "WHERE no_ujian IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_pmb_dash_bcm_ta_sem_prodi_lulus "
						+ "ON public.biodata_calon_mahasiswa (tahunakademik, semester_mulai, prodi_lulus) "
						+ "WHERE prodi_lulus IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_pmb_dash_bcm_ta_sem_mahasiswa "
						+ "ON public.biodata_calon_mahasiswa (tahunakademik, semester_mulai, mahasiswa) "
						+ "WHERE mahasiswa IS NOT NULL",

				// Grafik/top agregasi utama.
				"CREATE INDEX IF NOT EXISTS idx_pmb_dash_bcm_ta_sem_prodi1 "
						+ "ON public.biodata_calon_mahasiswa (tahunakademik, semester_mulai, prodi_1) "
						+ "WHERE prodi_1 IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_pmb_dash_bcm_ta_sem_tanggal "
						+ "ON public.biodata_calon_mahasiswa (tahunakademik, semester_mulai, tanggal_daftar) "
						+ "WHERE tanggal_daftar IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_pmb_dash_bcm_ta_sem_gelombang "
						+ "ON public.biodata_calon_mahasiswa (tahunakademik, semester_mulai, gelombang_pendaftaran) "
						+ "WHERE gelombang_pendaftaran IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_pmb_dash_bcm_ta_sem_propinsi "
						+ "ON public.biodata_calon_mahasiswa (tahunakademik, semester_mulai, propinsi_calon) "
						+ "WHERE propinsi_calon IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_pmb_dash_bcm_ta_sem_gender "
						+ "ON public.biodata_calon_mahasiswa (tahunakademik, semester_mulai, jenis_kelamin) "
						+ "WHERE jenis_kelamin IS NOT NULL",

				// Subtab jalur, paket, asal sekolah, dan filter rekap PMB.
				"CREATE INDEX IF NOT EXISTS idx_pmb_dash_bcm_ta_sem_jalur "
						+ "ON public.biodata_calon_mahasiswa (tahunakademik, semester_mulai, jenis_seleksi) "
						+ "WHERE jenis_seleksi IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_pmb_dash_bcm_ta_sem_jalur_pilih "
						+ "ON public.biodata_calon_mahasiswa (tahunakademik, semester_mulai, jenis_seleksi_pilih) "
						+ "WHERE jenis_seleksi_pilih IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_pmb_dash_bcm_ta_sem_paket "
						+ "ON public.biodata_calon_mahasiswa (tahunakademik, semester_mulai, paket_registrasi_mahasiswa) "
						+ "WHERE paket_registrasi_mahasiswa IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_pmb_dash_bcm_ta_sem_nama_sekolah "
						+ "ON public.biodata_calon_mahasiswa (tahunakademik, semester_mulai, nama_sekolah_asal) "
						+ "WHERE nama_sekolah_asal IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_pmb_dash_bcm_ta_sem_asal_sma "
						+ "ON public.biodata_calon_mahasiswa (tahunakademik, semester_mulai, asal_sma) "
						+ "WHERE asal_sma IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_pmb_dash_bcm_ta_sem_filter "
						+ "ON public.biodata_calon_mahasiswa (tahunakademik, semester_mulai, program, jenis_seleksi, gelombang_pendaftaran, paket_registrasi_mahasiswa) "
						+ "WHERE (aktif = true OR aktif IS NULL)"
		};

		for (String sql : indexQueries) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e,
						"auto-audit(empty-catch) src/ais/common/InitIndex.java:initIndexDashboardStatistikPmbSuperFast");
			}
		}

		try {
			eksekusiSql10Menit("ANALYZE public.biodata_calon_mahasiswa");
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/common/InitIndex.java:initIndexDashboardStatistikPmbSuperFast-analyze");
		}
	}


	private static void initAlterTableParameterTambahanAngketUmum() {
		// Perubahan kolom/constraint (mis. lepas NOT NULL grup_checklist_penilaian_umum &
		// jadwal_checklist_penilaian_umum) tidak lagi di-ALTER manual di sini — diserahkan
		// ke Hibernate (hbm2ddl.auto=update). Method ini kini hanya membuat index pendukung.

		String[] indexQueries = new String[] {
				"CREATE INDEX IF NOT EXISTS idx_iapu_jadwal_umum "
						+ "ON public.isi_angket_parameter_umum (jadwal_checklist_penilaian_umum)",

				"CREATE INDEX IF NOT EXISTS idx_iapu_checklist_dosen "
						+ "ON public.isi_angket_parameter_umum (checklist_baru_penilaian_dosen_oleh_mahasiswa)",

				"CREATE INDEX IF NOT EXISTS idx_iapu_checklist_guru "
						+ "ON public.isi_angket_parameter_umum (checklist_baru_penilaian_guru_oleh_siswa)",

				"CREATE INDEX IF NOT EXISTS idx_pta_grup_umum "
						+ "ON public.parameter_tambahan_angket_umum (grup_checklist_penilaian_umum)",

				"CREATE INDEX IF NOT EXISTS idx_pta_grup_dosen "
						+ "ON public.parameter_tambahan_angket_umum (grup_checklist_penilaian_dosen)",

				"CREATE INDEX IF NOT EXISTS idx_pta_grup_guru "
						+ "ON public.parameter_tambahan_angket_umum (grup_checklist_penilaian_guru)",

				"CREATE INDEX IF NOT EXISTS idx_pta_parameter_tambahan "
						+ "ON public.parameter_tambahan_angket_umum (parameter_tambahan)" };

		for (String sql : indexQueries) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitIndex.java:726");
			}
		}
	}


	private static void initDropConstraintNoRegistrasiBiodataCalonMahasiswa() {
		// DROP CONSTRAINT unique no_registrasi (biodata_calon_mahasiswa) tidak lagi
		// dijalankan di sini — pengelolaan constraint diserahkan ke Hibernate / manual.
	}

	/**
	 * Migrasi: konsolidasi 26 kolom Boolean {@code akses_*}/{@code kantin_member_landing_page} milik
	 * {@code public.tbmrole} jadi satu kolom JSON {@code ebisnis_menu} (lihat JavaDoc
	 * {@code Tbmrole.getEbisnisMenu()} &amp; {@code ais.common.EbisnisMenuKatalog}). Kolom lama TIDAK
	 * pernah terpakai UI/dispatcher apa pun sebelum dihapus (diverifikasi lewat pencarian penuh
	 * codebase) -- DROP di sini tidak kehilangan data. Idempoten (IF EXISTS/IF NOT EXISTS), aman
	 * dijalankan berulang tiap start server -- mengikuti pola {@code migrasi_ebisnis_menu_konsolidasi
	 * .sql} (berkas itu tetap dipertahankan sbg dokumentasi/fallback manual, method ini yang benar-
	 * benar dieksekusi otomatis saat startup).
	 */
	private static void initKonsolidasiEbisnisMenuTbmrole() {
		try {
			eksekusiSql("ALTER TABLE public.tbmrole "
					+ "DROP COLUMN IF EXISTS akses_supervisor_kantin, "
					+ "DROP COLUMN IF EXISTS akses_kasir, "
					+ "DROP COLUMN IF EXISTS akses_beranda_kantin, "
					+ "DROP COLUMN IF EXISTS akses_ringkasan, "
					+ "DROP COLUMN IF EXISTS akses_pesanan, "
					+ "DROP COLUMN IF EXISTS akses_anggota, "
					+ "DROP COLUMN IF EXISTS akses_produk, "
					+ "DROP COLUMN IF EXISTS akses_stok_opname, "
					+ "DROP COLUMN IF EXISTS akses_kulakan, "
					+ "DROP COLUMN IF EXISTS akses_diskon, "
					+ "DROP COLUMN IF EXISTS akses_laporan_transaksi, "
					+ "DROP COLUMN IF EXISTS akses_laporan, "
					+ "DROP COLUMN IF EXISTS akses_riwayat_sinkronisasi, "
					+ "DROP COLUMN IF EXISTS akses_log_error, "
					+ "DROP COLUMN IF EXISTS akses_konfigurasi, "
					+ "DROP COLUMN IF EXISTS akses_pembayaran, "
					+ "DROP COLUMN IF EXISTS akses_pedagang, "
					+ "DROP COLUMN IF EXISTS akses_meja, "
					+ "DROP COLUMN IF EXISTS akses_penyedia, "
					+ "DROP COLUMN IF EXISTS akses_kas_kasir, "
					+ "DROP COLUMN IF EXISTS akses_setoran_tenant, "
					+ "DROP COLUMN IF EXISTS akses_jadwal_opname, "
					+ "DROP COLUMN IF EXISTS akses_stok_expired, "
					+ "DROP COLUMN IF EXISTS akses_limit_kredit, "
					+ "DROP COLUMN IF EXISTS akses_mutasi_rekening, "
					+ "DROP COLUMN IF EXISTS akses_produksi, "
					+ "DROP COLUMN IF EXISTS akses_pengaturan_laporan, "
					+ "DROP COLUMN IF EXISTS akses_riwayat_penjualan, "
					+ "DROP COLUMN IF EXISTS akses_retur_penjualan, "
					+ "DROP COLUMN IF EXISTS kantin_member_landing_page, "
					+ "ADD COLUMN IF NOT EXISTS ebisnis_menu text");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit InitIndex.initKonsolidasiEbisnisMenuTbmrole");
		}
	}

	/**
	 * Migrasi backfill SATU-KALI: isi {@code ebisnis_menu} role global "Kantin" (dibuat {@code
	 * InitDataHelper}) dgn default kasir dasar ({@code EbisnisMenuKatalog.defaultMenuKantinJson()} --
	 * hanya Kasir/Ringkasan/Pesanan/Anggota/Produk/Stok Opname/Kulakan/Aturan Diskon/Riwayat
	 * Sinkronisasi/Log Error/Konfigurasi aktif) utk instalasi LAMA yang role "Kantin"-nya sudah ada
	 * sejak sebelum kolom {@code ebisnis_menu} dibuat (jadi masih NULL = "semua menu tampil" bawaan
	 * {@code EbisnisMenuKatalog.urai}). Guard {@code ebisnis_menu IS NULL} membuat ini AMAN dijalankan
	 * berulang tiap start server -- begitu admin mengedit lewat "Tambah Grup Pengguna" (kolom jadi
	 * terisi), migrasi ini tidak akan pernah menimpanya lagi.
	 */
	private static void initDefaultMenuKantin() {
		try {
			String json = ais.common.EbisnisMenuKatalog.defaultMenuKantinJson().replace("'", "''");
			eksekusiSql("UPDATE public.tbmrole SET ebisnis_menu = '" + json + "' "
					+ "WHERE roleid = 'Kantin' AND (ebisnis_menu IS NULL OR btrim(ebisnis_menu) = '')");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit InitIndex.initDefaultMenuKantin");
		}
	}

	/**
	 * Role bawaan fitur Grup Produk (harga terpusat lintas toko). Kunci {@code grup_produk}
	 * fail-closed ({@code EbisnisMenuKatalog.KUNCI_DEFAULT_NONAKTIF}) sehingga TIDAK ADA role
	 * existing yang otomatis mendapatkannya -- role ini disediakan agar admin tinggal menugaskan
	 * pengguna kantor pusat tanpa merakit JSON menu manual. {@code menu.produk=true} hanya untuk
	 * MELIHAT katalog (tanpa CRUD produk); seluruh aksi CRUD {@code grup_produk} dinyalakan.
	 * INSERT idempoten (WHERE NOT EXISTS) dan role yang sudah ada tidak pernah ditimpa --
	 * suntingan admin lewat TbmroleAction dihormati.
	 */
	private static void initRoleGrupProduk() {
		try {
			String json = ("{\"supervisor\":false,"
					+ "\"menu\":{\"grup_produk\":true,\"produk\":true},"
					+ "\"crud\":{\"grup_produk\":{\"create\":true,\"update\":true,\"delete\":true,"
					+ "\"approve\":true,\"reject\":true}}}").replace("'", "''");
			eksekusiSql("INSERT INTO public.tbmrole (roleid, rolename, aktif, ebisnis_menu) "
					+ "SELECT 'manajemen_harga_pusat', 'Manajemen Harga Pusat (Grup Produk)', true, '" + json + "' "
					+ "WHERE NOT EXISTS (SELECT 1 FROM public.tbmrole WHERE roleid = 'manajemen_harga_pusat')");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit InitIndex.initRoleGrupProduk");
		}
	}


	private static void initFkCascadeDiskonSiswaItemBiaya() {
		// FK ON DELETE CASCADE diskon_siswa_item_biaya tidak lagi di-ALTER di sini —
		// pengelolaan foreign key diserahkan ke Hibernate / manual.
	}


	private static void initIndexVirtualAccountPaymentSuperFast() {
		/*
		 * INDEX KHUSUS VIRTUAL ACCOUNT BANK + CEK ULANG PEMBAYARAN
		 *
		 * Bottleneck yang diperbaiki:
		 * 1. ambilVa(kode, nominal, bankHost)   → seq-scan public.virtual_account_bank
		 *    karena kolom `kode` sama sekali belum diindeks.
		 * 2. ambilLink(link, bankHost)           → seq-scan pada kolom `link`.
		 * 3. bayarSiswa guard baru               → COUNT(*) sekolah.pembayaran_siswa
		 *    WHERE virtual_account_bank = ?  (belum ada index pada kolom ini).
		 * 4. bayarVa guard baru                  → SUM(nilai) cicilan_pembayaran
		 *    WHERE ref_va = ?  (ref_va belum diindeks sama sekali).
		 * 5. Grid "Pembayaran Online" filter/sort → scan VAB by siswa, mahasiswa,
		 *    bank_host, waktu_bayar.
		 *
		 * Anti-duplikat (dicek manual vs seluruh isi file ini):
		 * - idx_cicilan_ref   ON cicilan_pembayaran (ref)  → BEDA kolom (ref vs ref_va) ✓
		 * - idx_pemsis_siswa_cover ON pembayaran_siswa (siswa_id) → BEDA kolom ✓
		 * - idx_keg_mhs_reinit_cover / idx_keg_calon_reinit_cover → tabel kegiatan ✓
		 * - SEMUA idx_vab_*, idx_pemsis_vab*, idx_cicilan_ref_va* → BARU, belum ada ✓
		 */
		String[] indexQueries = new String[] {

				// ── virtual_account_bank ─────────────────────────────────────────────────────
				// (1) Lookup utama ambilVa: WHERE kode = ? ORDER BY id DESC LIMIT 1
				// kode NOT NULL (updatable=false, nullable=false) → tanpa partial WHERE.
				"CREATE INDEX IF NOT EXISTS idx_vab_kode_id "
						+ "ON public.virtual_account_bank (kode, id DESC)",

				// (2) Fallback ambilLink: WHERE link = ? ORDER BY id DESC LIMIT 1
				"CREATE INDEX IF NOT EXISTS idx_vab_link_id "
						+ "ON public.virtual_account_bank (link, id DESC) "
						+ "WHERE link IS NOT NULL",

				// (3) Filter/display grid per siswa (versi Sekolah)
				"CREATE INDEX IF NOT EXISTS idx_vab_siswa_id "
						+ "ON public.virtual_account_bank (siswa, id DESC) "
						+ "WHERE siswa IS NOT NULL",

				// (4) Filter/display grid per mahasiswa (versi Kampus)
				"CREATE INDEX IF NOT EXISTS idx_vab_mahasiswa_id "
						+ "ON public.virtual_account_bank (mahasiswa, id DESC) "
						+ "WHERE mahasiswa IS NOT NULL",

				// (5) Filter grid per bank_host (channel: Flip, BSI, BNI …)
				"CREATE INDEX IF NOT EXISTS idx_vab_bankhost_id "
						+ "ON public.virtual_account_bank (bank_host, id DESC) "
						+ "WHERE bank_host IS NOT NULL",

				// (6) Tab "Sudah dibayar" — sort/filter by waktuBayar
				// Kolom fisik "waktubayar" (tanpa underscore) — field Java waktuBayar di
				// VirtualAccountBank.java TIDAK punya @Column(name=...) eksplisit, jadi
				// Hibernate memakai nama default lowercase tanpa underscore, BUKAN snake_case.
				"CREATE INDEX IF NOT EXISTS idx_vab_waktu_bayar_id "
						+ "ON public.virtual_account_bank (waktubayar DESC, id DESC) "
						+ "WHERE waktubayar IS NOT NULL",

				// ── sekolah.pembayaran_siswa ─────────────────────────────────────────────────
				// (7) Guard baru bayarSiswa (Cek Ulang versi Sekolah):
				//     SELECT COUNT(*) … WHERE virtual_account_bank = ?
				//     Tidak ada index sebelumnya pada kolom ini.
				"CREATE INDEX IF NOT EXISTS idx_pemsis_vab "
						+ "ON sekolah.pembayaran_siswa (virtual_account_bank, id DESC) "
						+ "WHERE virtual_account_bank IS NOT NULL",

				// ── cicilan_pembayaran ───────────────────────────────────────────────────────
				// (8) Guard baru bayarVa (Cek Ulang versi Mahasiswa):
				//     SELECT SUM(nilai) … WHERE ref_va = ?
				//     INCLUDE (nilai) = covering index agar SUM tidak perlu heap fetch.
				//     MODE_INDEX_POSTGRES_LAMA_TANPA_INCLUDE akan menghapus INCLUDE otomatis
				//     pada PG < 11 — index tetap dibuat, hanya tidak covering.
				"CREATE INDEX IF NOT EXISTS idx_cicilan_ref_va "
						+ "ON public.cicilan_pembayaran (ref_va) INCLUDE (nilai) "
						+ "WHERE ref_va IS NOT NULL",
		};

		for (String sql : indexQueries) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:820");
				/* Optional — jangan blokir startup bila kolom belum ada di skema lama. */
			}
		}

		String[] analyzeQueries = new String[] {
				"ANALYZE public.virtual_account_bank",
				"ANALYZE sekolah.pembayaran_siswa",
				"ANALYZE public.cicilan_pembayaran",
		};
		for (String sql : analyzeQueries) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:833");
			}
		}
	}


	private static void initIndexDepositTabunganSuperFast() {
		/*
		 * INDEX KHUSUS DEPOSIT / TABUNGAN
		 * Dipakai oleh DepositAction, LaporanDeposit, deposit.jrxml, dan query mutasi
		 * gabungan deposit + cicilan_pembayaran + pengeluaran_mahasiswa + pembayaran_siswa.
		 */
		String[] indexQueries = new String[] {
				"CREATE INDEX IF NOT EXISTS idx_deposit_mhs_waktu_id ON public.deposit (mahasiswa, waktu DESC, id DESC) WHERE mahasiswa IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_deposit_bcm_waktu_id ON public.deposit (biodata_calon_mahasiswa, waktu DESC, id DESC) WHERE biodata_calon_mahasiswa IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_deposit_siswa_waktu_id ON public.deposit (siswa, waktu DESC, id DESC) WHERE siswa IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_deposit_calonsiswa_waktu_id ON public.deposit (calon_siswa, waktu DESC, id DESC) WHERE calon_siswa IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_deposit_anggota_waktu_id ON public.deposit (anggota_koperasi, waktu DESC, id DESC) WHERE anggota_koperasi IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_deposit_jenis_tabungan_waktu ON public.deposit (jenis_tabungan, waktu DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_deposit_jenis_pembayaran_waktu ON public.deposit (jenis_pembayaran, waktu DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_deposit_waktu_id_desc ON public.deposit (waktu DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_deposit_keterangan_trgm ON public.deposit USING gin (keterangan gin_trgm_ops)",

				"CREATE INDEX IF NOT EXISTS idx_cicilan_deposit_kegiatan_tanggal ON public.cicilan_pembayaran (kegiatan, tanggal DESC, id DESC) WHERE deposit > 0.1",
				"CREATE INDEX IF NOT EXISTS idx_cicilan_deposit_tanggal ON public.cicilan_pembayaran (tanggal DESC, id DESC) WHERE deposit > 0.1",
				"CREATE INDEX IF NOT EXISTS idx_kegiatan_deposit_mhs_calon ON public.kegiatan (id, mahasiswa, calon_mahasiswa)",
				"CREATE INDEX IF NOT EXISTS idx_kegiatan_mhs_calon_jenis_ta_smt ON public.kegiatan (mahasiswa, calon_mahasiswa, jenis_kegiatan, tahun_akademik, semster, id)",

				"CREATE INDEX IF NOT EXISTS idx_pengeluaran_mhs_waktu ON public.pengeluaran_mahasiswa (mahasiswa, waktu DESC, id DESC) WHERE mahasiswa IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_pengeluaran_calon_waktu ON public.pengeluaran_mahasiswa (calon_mahasiswa, waktu DESC, id DESC) WHERE calon_mahasiswa IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_pengeluaran_siswa_waktu ON public.pengeluaran_mahasiswa (siswa, waktu DESC, id DESC) WHERE siswa IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_pengeluaran_calonsiswa_waktu ON public.pengeluaran_mahasiswa (calon_siswa, waktu DESC, id DESC) WHERE calon_siswa IS NOT NULL",

				"CREATE INDEX IF NOT EXISTS idx_pembayaran_siswa_tabungan_siswa_tgl ON sekolah.pembayaran_siswa (siswa_id, tanggal DESC, id DESC) WHERE daritabungan > 0.1",
				"CREATE INDEX IF NOT EXISTS idx_pembayaran_siswa_tabungan_calon_tgl ON sekolah.pembayaran_siswa (calon_siswa_id, tanggal DESC, id DESC) WHERE daritabungan > 0.1",
				"CREATE INDEX IF NOT EXISTS idx_pembayaran_siswa_tabungan_tgl ON sekolah.pembayaran_siswa (tanggal DESC, id DESC) WHERE daritabungan > 0.1",

				"CREATE INDEX IF NOT EXISTS idx_mhs_jurusan_nama_nim ON public.mahasiswa (jurusan, nama, nim, id)",

				// REDUNDAN: definisi identik dgn idx_trgm_mhs_nama/idx_trgm_mhs_nim yang sudah
				// ada di initEksekusiQueryIndex() (blok "INDEKS PENCARIAN TEKS GIN"), cuma beda
				// nama & kualifikasi skema ("mahasiswa" vs "public.mahasiswa" -- tabel FISIK sama,
				// jadi guard dedup nama+signature di file ini tidak menangkapnya) -> 2 index GIN
				// kembar utk kolom sama memperlambat INSERT/UPDATE tanpa manfaat baca tambahan.
				// Di-DROP di sini, dipertahankan yang sudah ada (idx_trgm_mhs_nama/idx_trgm_mhs_nim).
				"DROP INDEX IF EXISTS idx_mhs_nama_trgm_deposit",
				"DROP INDEX IF EXISTS idx_mhs_nim_trgm_deposit",
				"CREATE INDEX IF NOT EXISTS idx_bcm_prodi1_lulus_nama ON public.biodata_calon_mahasiswa (prodi_1, prodi_lulus, nama, no_registrasi, id)",
				"CREATE INDEX IF NOT EXISTS idx_bcm_nama_trgm_deposit ON public.biodata_calon_mahasiswa USING gin (nama gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_bcm_noreg_trgm_deposit ON public.biodata_calon_mahasiswa USING gin (no_registrasi gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_siswa_deposit_filter ON sekolah.siswa (sekolah_id, yayasan_id, nama_siswa, nomor_induk, id)",
				"CREATE INDEX IF NOT EXISTS idx_siswa_nama_trgm_deposit ON sekolah.siswa USING gin (nama_siswa gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_siswa_nomor_trgm_deposit ON sekolah.siswa USING gin (nomor_induk gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_calonsiswa_deposit_filter ON sekolah.calon_siswa (sekolah_id, yayasan_id, nama_siswa, nomor_induk, id)",
				"CREATE INDEX IF NOT EXISTS idx_calonsiswa_nama_trgm_deposit ON sekolah.calon_siswa USING gin (nama_siswa gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_calonsiswa_nomor_trgm_deposit ON sekolah.calon_siswa USING gin (nomor_induk gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_anggota_koperasi_nama_trgm_deposit ON koperasi.anggota_koperasi USING gin (nama gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_anggota_koperasi_kode_trgm_deposit ON koperasi.anggota_koperasi USING gin (kode gin_trgm_ops)",
					"CREATE INDEX IF NOT EXISTS idx_anggota_koperasi_fk_filter_deposit ON koperasi.anggota_koperasi (aktif, koperasi, mahasiswa, siswa, id)",
					"CREATE INDEX IF NOT EXISTS idx_anggota_koperasi_calon_siswa_deposit ON koperasi.anggota_koperasi (calon_siswa, id) WHERE calon_siswa IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_jurusan_fakultas_deposit ON public.jurusan (fakultas, aktif, id)" };

		for (String sql : indexQueries) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:891");
				/* Optional index: beberapa instalasi lama mungkin belum memiliki semua tabel/kolom. */
			}
		}

		String[] analyzeQueries = new String[] { "ANALYZE public.deposit", "ANALYZE public.cicilan_pembayaran",
				"ANALYZE public.kegiatan", "ANALYZE public.pengeluaran_mahasiswa",
				"ANALYZE sekolah.pembayaran_siswa" };
		for (String sql : analyzeQueries) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:902");
			}
		}
	}


	private static void initIndexDaftarUlangPembayaranSuperFast() {
		/*
		 * INDEX KHUSUS DAFTAR ULANG, INFORMASI PEMBAYARAN, DAN PEMBAYARAN ONLINE PSB
		 *
		 * Bottleneck yang diperbaiki:
		 * 1. DaftarUlangMahasiswaLamaAction / DaftarUlangMahasiswaBaruAction:
		 *    a. CicilanPembayaran WHERE kegiatan = ? ORDER BY tanggal ASC, ke ASC
		 *       (grid cetak/print) — idx_cicilan_kegiatan_id_cover hanya cover ORDER BY id,
		 *       tidak cover ORDER BY tanggal, ke.
		 *    b. KegiatanTemporary WHERE mahasiswa/calon_mahasiswa = ?, jenis_kegiatan = ?,
		 *       semster = ?, kegiatan IS NULL — belum ada index sama sekali.
		 *    c. DELETE detail_kegiatan WHERE kegiatan_temporary = ? AND posting_history IS NULL
		 *       — idx_dk_kegiatan_id_cover cover kolom kegiatan, bukan kegiatan_temporary.
		 *    d. DELETE cicilan_pembayaran WHERE kegiatan_temporary = ?
		 *       — belum ada index pada kolom kegiatan_temporary.
		 *    e. JenisPembayaran WHERE defaultPembayaran=true AND (aktif IS NULL OR aktif=true)
		 *       — lookup 2x per file, belum ada index.
		 *    f. JenisPembayaran WHERE jenis_tabungan IS NOT NULL AND (aktif IS NULL OR aktif=true)
		 *       — belum ada index.
		 * 2. VirtualAccountBankAction (tab Pembayaran Online) userAccessRestriction():
		 *    a. siswa dan mahasiswa sudah di-cover initIndexVirtualAccountPaymentSuperFast.
		 *    b. calon_siswa (PSB sekolah) dan biodata_calon_mahasiswa (PMB) belum ada index.
		 * 3. PembayaranOnlineAction (login PSB):
		 *    a. CalonSiswa WHERE gelombangPendaftaranPsb IS NOT NULL
		 *       AND (noRegistrasi ILIKE ? OR noUjian ILIKE ?)
		 *       — perlu trigram GIN agar ILIKE '%?%' bisa pakai index.
		 *
		 * Anti-duplikat (dicek manual vs seluruh isi file ini):
		 * - idx_cicilan_kegiatan_id_cover  ON cicilan_pembayaran (kegiatan, id)  → beda kolom ORDER ✓
		 * - idx_dk_kegiatan_id_cover       ON detail_kegiatan (kegiatan, id)     → kolom kegiatan ≠ kegiatan_temporary ✓
		 * - idx_calonsiswa_nama_trgm_deposit / nomor_induk_trgm → kolom beda (nama_siswa, nomor_induk) ✓
		 * - idx_vab_siswa_id / idx_vab_mahasiswa_id → sudah ada, TIDAK diulang di sini ✓
		 */
		String[] indexQueries = new String[] {

				// ── cicilan_pembayaran ──────────────────────────────────────────────────────
				// (1) Grid cetak DaftarUlang: ORDER BY tanggal ASC, ke ASC
				//     Berbeda dari idx_cicilan_kegiatan_id_cover yang cover ORDER BY id.
				"CREATE INDEX IF NOT EXISTS idx_cicilan_kegiatan_tgl_ke "
						+ "ON public.cicilan_pembayaran (kegiatan, tanggal ASC, ke ASC)",

				// (5) DELETE cicilan_pembayaran WHERE kegiatan_temporary = ?
				"CREATE INDEX IF NOT EXISTS idx_cicilan_keg_temp "
						+ "ON public.cicilan_pembayaran (kegiatan_temporary) "
						+ "WHERE kegiatan_temporary IS NOT NULL",

				// ── kegiatan_temporary ──────────────────────────────────────────────────────
				// (2a) DaftarUlangLama: cart lookup WHERE mahasiswa = ? AND jenis_kegiatan = ?
				//      AND semster = ? AND kegiatan IS NULL
				"CREATE INDEX IF NOT EXISTS idx_keg_temp_mhs_jk_smt "
						+ "ON public.kegiatan_temporary (mahasiswa, jenis_kegiatan, semster) "
						+ "WHERE kegiatan IS NULL",

				// (2b) DaftarUlangBaru: cart lookup WHERE calon_mahasiswa = ? AND ...
				"CREATE INDEX IF NOT EXISTS idx_keg_temp_calon_jk_smt "
						+ "ON public.kegiatan_temporary (calon_mahasiswa, jenis_kegiatan, semster) "
						+ "WHERE kegiatan IS NULL",

				// ── detail_kegiatan ─────────────────────────────────────────────────────────
				// (3) DELETE WHERE kegiatan_temporary = ? AND posting_history IS NULL
				"CREATE INDEX IF NOT EXISTS idx_dk_keg_temp "
						+ "ON public.detail_kegiatan (kegiatan_temporary) "
						+ "WHERE kegiatan_temporary IS NOT NULL AND posting_history IS NULL",

				// ── jenis_pembayaran ────────────────────────────────────────────────────────
				// (4a) Lookup default payment: WHERE defaultPembayaran=true AND aktif
				// Kolom fisik "defaultpembayaran" (tanpa underscore) — field Java
				// defaultPembayaran di JenisPembayaran.java TIDAK punya @Column(name=...)
				// eksplisit, jadi Hibernate memakai nama default lowercase tanpa underscore.
				"CREATE INDEX IF NOT EXISTS idx_jenis_pem_default "
						+ "ON public.jenis_pembayaran (defaultpembayaran) "
						+ "WHERE defaultpembayaran = true AND (aktif IS NULL OR aktif = true)",

				// (4b) Lookup savings/tabungan payment: WHERE jenis_tabungan IS NOT NULL AND aktif
				"CREATE INDEX IF NOT EXISTS idx_jenis_pem_tabungan "
						+ "ON public.jenis_pembayaran (jenis_tabungan) "
						+ "WHERE jenis_tabungan IS NOT NULL AND (aktif IS NULL OR aktif = true)",

				// ── virtual_account_bank (pelengkap initIndexVirtualAccountPaymentSuperFast) ──
				// (6a) userAccessRestriction() filter calon_siswa (PSB Sekolah)
				"CREATE INDEX IF NOT EXISTS idx_vab_calon_siswa_id "
						+ "ON public.virtual_account_bank (calon_siswa, id DESC) "
						+ "WHERE calon_siswa IS NOT NULL",

				// (6b) userAccessRestriction() filter biodata_calon_mahasiswa (PMB Kampus)
				"CREATE INDEX IF NOT EXISTS idx_vab_bcm_id "
						+ "ON public.virtual_account_bank (biodata_calon_mahasiswa, id DESC) "
						+ "WHERE biodata_calon_mahasiswa IS NOT NULL",

				// ── sekolah.calon_siswa (PSB login PembayaranOnlineAction) ────────────────
				// (7a) Pre-filter gelombang PSB sebelum ILIKE pada no_registrasi / no_ujian
				"CREATE INDEX IF NOT EXISTS idx_calonsiswa_gelombang_psb "
						+ "ON sekolah.calon_siswa (current_gelombang_pendaftaran_psb_id) "
						+ "WHERE current_gelombang_pendaftaran_psb_id IS NOT NULL",

				// (7b/7c) DIHAPUS - kolom "no_registrasi"/"no_ujian" TIDAK ADA di sekolah.calon_siswa
				// (SQLState 42703 undefined_column). CalonSiswa.noRegistrasi dipetakan ke kolom
				// "nomor_induk" (@Column name="nomor_induk"); trigram GIN pada nomor_induk SUDAH ada di
				// idx_calonsiswa_nomor_trgm_deposit. "noUjian" TIDAK dipetakan ke kolom (getNoUjian tanpa
				// @Column) -> tak bisa diindeks. ILIKE noreg PSB pakai HQL noRegistrasi -> nomor_induk -> index itu.
		};

		for (String sql : indexQueries) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:1010");
				/* Optional — jangan blokir startup bila kolom belum ada di skema lama. */
			}
		}

		String[] analyzeQueries = new String[] {
				"ANALYZE public.cicilan_pembayaran",
				"ANALYZE public.kegiatan_temporary",
				"ANALYZE public.detail_kegiatan",
				"ANALYZE public.jenis_pembayaran",
				"ANALYZE public.virtual_account_bank",
				"ANALYZE sekolah.calon_siswa",
		};
		for (String sql : analyzeQueries) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:1026");
			}
		}
	}


	private static void initIndexAlurSopWorkflowSuperFast() {
		/*
		 * INDEX KHUSUS MASTER ALUR SOP, DIAGRAM ALUR SOP, DAN WORKFLOW SOP
		 * Fokus:
		 * - Filter SOP terpilih lalu load seluruh alur secara cepat.
		 * - Render diagram tanpa N+1 query dokumen/parameter.
		 * - Join alur_sop_has_dokumen -> dokumen_alur_sop seperti query diagram.
		 * - Histori disposisi_alur_sop pada TampilanAlurSopAction.
		 */
		String[] indexQueries = new String[] {
				"CREATE INDEX IF NOT EXISTS idx_alur_sop_diag_sop_aktif_nomor_kode "
						+ "ON public.alur_sop (sop, aktif, nomor, kode, nama, id)",
				"CREATE INDEX IF NOT EXISTS idx_alur_sop_diag_sop_nomor_kode "
						+ "ON public.alur_sop (sop, nomor, kode, nama, id)",
				"CREATE INDEX IF NOT EXISTS idx_alur_sop_diag_sop_start "
						+ "ON public.alur_sop (sop, start, aktif, nomor, id)",
				"CREATE INDEX IF NOT EXISTS idx_alur_sop_diag_sop_sebelumnya "
						+ "ON public.alur_sop (sop, sebelumnya, aktif, nomor, id)",
				"CREATE INDEX IF NOT EXISTS idx_alur_sop_diag_aktor_sop "
						+ "ON public.alur_sop (aktor_sop, sop, aktif, nomor, id)",
				"CREATE INDEX IF NOT EXISTS idx_alur_sop_diag_after_1 "
						+ "ON public.alur_sop (setelahnya, sop, aktif, nomor, id) WHERE setelahnya IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_alur_sop_diag_after_2 "
						+ "ON public.alur_sop (setelahnya2, sop, aktif, nomor, id) WHERE setelahnya2 IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_alur_sop_diag_after_3 "
						+ "ON public.alur_sop (setelahnya3, sop, aktif, nomor, id) WHERE setelahnya3 IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_alur_sop_diag_after_4 "
						+ "ON public.alur_sop (setelahnya4, sop, aktif, nomor, id) WHERE setelahnya4 IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_alur_sop_diag_after_5 "
						+ "ON public.alur_sop (setelahnya5, sop, aktif, nomor, id) WHERE setelahnya5 IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_ashd_diag_alur_dok "
						+ "ON public.alur_sop_has_dokumen (alur_sop, dokumen)",
				"CREATE INDEX IF NOT EXISTS idx_ashd_diag_dok_alur "
						+ "ON public.alur_sop_has_dokumen (dokumen, alur_sop)",
				"CREATE INDEX IF NOT EXISTS idx_dok_alur_sop_diag_active_id "
						+ "ON public.dokumen_alur_sop (id, kode, nama) WHERE (aktif = true OR aktif IS NULL)",
				"CREATE INDEX IF NOT EXISTS idx_dok_alur_sop_diag_sop_active "
						+ "ON public.dokumen_alur_sop (sop, aktif, kode, nama, id)",
				"CREATE INDEX IF NOT EXISTS idx_ashp_diag_alur_param "
						+ "ON public.alur_sop_has_parameter (alur_sop, parameter)",
				"CREATE INDEX IF NOT EXISTS idx_ashp_diag_param_alur "
						+ "ON public.alur_sop_has_parameter (parameter, alur_sop)",
				"CREATE INDEX IF NOT EXISTS idx_kptas_diag_active_nama_id "
						+ "ON public.kelompok_parameter_tambahan_alur_sop (aktif, nama, id)",
				"CREATE INDEX IF NOT EXISTS idx_ptas_diag_kelompok_param "
						+ "ON public.parameter_tambahan_alur_sop (kelompok_parameter_tambahan_alur_sop, parameter_tambahan)",
				"CREATE INDEX IF NOT EXISTS idx_aktor_sop_diag_active_kode_nama "
						+ "ON public.aktor_sop (aktif, kode, nama, id)",
				"CREATE INDEX IF NOT EXISTS idx_aktor_sop_diag_username_trgm "
						+ "ON public.aktor_sop USING gin (username_pengguna gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_aktor_sop_diag_jenis_trgm "
						+ "ON public.aktor_sop USING gin (jenis_pengguna gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_sop_diag_aktif_nama_kode "
						+ "ON public.sop (aktif, nama, kode, id)",
				"CREATE INDEX IF NOT EXISTS idx_sop_diag_unit_filter "
						+ "ON public.sop (aktif, jurusan, fakultas, yayasan, sekolah, satuan_kerja, id)",
				"CREATE INDEX IF NOT EXISTS idx_das_diag_disposisi_alur_urut "
						+ "ON public.disposisi_alur_sop (disposisi_sop, id, alur_sop)",
				"CREATE INDEX IF NOT EXISTS idx_das_diag_alur_disposisi "
						+ "ON public.disposisi_alur_sop (alur_sop, disposisi_sop, id)",
				"CREATE INDEX IF NOT EXISTS idx_das_diag_deadline "
						+ "ON public.disposisi_alur_sop (waktumaksimal, disposisi_sop, alur_sop, id) WHERE waktumaksimal IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_ds_diag_sop_aktif_id "
						+ "ON public.disposisi_sop (sop, aktif, id)" };

		for (String sql : indexQueries) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:1100");
				/* Optional index: lanjutkan agar index lain tetap dibuat. */
			}
		}

		String[] analyzeQueries = new String[] { "ANALYZE public.sop", "ANALYZE public.alur_sop",
				"ANALYZE public.alur_sop_has_dokumen", "ANALYZE public.dokumen_alur_sop",
				"ANALYZE public.alur_sop_has_parameter", "ANALYZE public.kelompok_parameter_tambahan_alur_sop",
				"ANALYZE public.parameter_tambahan_alur_sop", "ANALYZE public.aktor_sop",
				"ANALYZE public.disposisi_sop", "ANALYZE public.disposisi_alur_sop" };
		for (String sql : analyzeQueries) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:1113");
			}
		}
	}

	private static void initIndexPengaturanBiayaSuperFast() {
		/*
		 * INDEX KHUSUS PENGATURAN BIAYA & NOMINAL BIAYA (DETAIL TAGIHAN SISWA)
		 *
		 * Bottleneck: setiap klik detail PengaturanBiaya memicu ribuan lookup per
		 * siswa/item karena sekolah.nominal_biaya tidak punya index sama sekali
		 * (kecuali unique constraint kodeUnik). Untuk 3000+ siswa x 2 item = 6000+
		 * query — masing-masing seq-scan → halaman bisa loading > 30 menit.
		 *
		 * Selain itu FK nominal_biaya_id di sekolah.tagihan juga tidak diindeks,
		 * sehingga setiap JOIN tagihan → nominal_biaya = seq-scan lagi.
		 *
		 * Double-check anti-duplikat:
		 * - Tidak ada index "idx_nb_*" manapun di file ini sebelumnya.
		 * - idx_tagihan_nominal_biaya_fk belum ada; yang sudah ada hanya
		 *   idx_tagihan_siswa_fk, idx_tagihan_pengaturan_biaya_fk, dll.
		 * - idx_pbps_*, idx_kps_*, idx_aps_* semuanya baru.
		 */
		String[] indexQueries = new String[] {

				// --- sekolah.nominal_biaya ---
				// Step-3 (last-resort): cari NominalBiaya by (PB + item + siswa) tanpa filter
				// tahunbulan. Ini adalah query paling sering dipanggil saat generate tagihan
				// dan saat buka detail PengaturanBiaya.
				"CREATE INDEX IF NOT EXISTS idx_nb_pb_item_siswa "
						+ "ON sekolah.nominal_biaya (pengaturan_biaya_id, item_biaya_sekolah_id, siswa_id) "
						+ "WHERE siswa_id IS NOT NULL",

				// Step-3 untuk CalonSiswa
				"CREATE INDEX IF NOT EXISTS idx_nb_pb_item_calon "
						+ "ON sekolah.nominal_biaya (pengaturan_biaya_id, item_biaya_sekolah_id, calon_siswa_id) "
						+ "WHERE calon_siswa_id IS NOT NULL",

				// Step-2: (PB + item + siswa + tahunbulan)
				"CREATE INDEX IF NOT EXISTS idx_nb_pb_item_siswa_tb "
						+ "ON sekolah.nominal_biaya (pengaturan_biaya_id, item_biaya_sekolah_id, siswa_id, tahunbulan) "
						+ "WHERE siswa_id IS NOT NULL AND tahunbulan IS NOT NULL",

				// Step-2 untuk CalonSiswa
				"CREATE INDEX IF NOT EXISTS idx_nb_pb_item_calon_tb "
						+ "ON sekolah.nominal_biaya (pengaturan_biaya_id, item_biaya_sekolah_id, calon_siswa_id, tahunbulan) "
						+ "WHERE calon_siswa_id IS NOT NULL AND tahunbulan IS NOT NULL",

				// Daftar semua NominalBiaya milik satu PengaturanBiaya (urut id)
				"CREATE INDEX IF NOT EXISTS idx_nb_pb_id "
						+ "ON sekolah.nominal_biaya (pengaturan_biaya_id, id ASC)",

				// Lookup dari sisi siswa: untuk PembayaranSiswaAction fallback step-3
				"CREATE INDEX IF NOT EXISTS idx_nb_siswa_pb_item "
						+ "ON sekolah.nominal_biaya (siswa_id, pengaturan_biaya_id, item_biaya_sekolah_id) "
						+ "WHERE siswa_id IS NOT NULL",

				// Lookup dari sisi calon_siswa
				"CREATE INDEX IF NOT EXISTS idx_nb_calon_pb_item "
						+ "ON sekolah.nominal_biaya (calon_siswa_id, pengaturan_biaya_id, item_biaya_sekolah_id) "
						+ "WHERE calon_siswa_id IS NOT NULL",

				// --- sekolah.tagihan ---
				// FK nominal_biaya_id BELUM diindeks; kritis untuk:
				// - createAlias("nominalBiaya",...) di DetailTagihanSiswaHelper
				// - .eq("nominalBiaya", nb) di PembayaranSiswaAction
				// REDUNDAN (prefix dari idx_tagihan_nb_siswa_paid (nominal_biaya_id,siswa_id,...)) → di-DROP.
				"DROP INDEX IF EXISTS sekolah.idx_tagihan_nominal_biaya_fk",

				// Paid-check composite: nominal_biaya + siswa + detail bayar
				"CREATE INDEX IF NOT EXISTS idx_tagihan_nb_siswa_paid "
						+ "ON sekolah.tagihan (nominal_biaya_id, siswa_id, pembayaran_siswa_detail_id)",

				// Paid-check untuk calon_siswa
				"CREATE INDEX IF NOT EXISTS idx_tagihan_nb_calon_paid "
						+ "ON sekolah.tagihan (nominal_biaya_id, calon_siswa_id, pembayaran_siswa_detail_id) "
						+ "WHERE calon_siswa_id IS NOT NULL",

				// --- sekolah.pengaturan_biaya_punya_siswa ---
				// Subquery di DetailTagihanSiswaHelper: filter siswa by pengaturan_biaya
				"CREATE INDEX IF NOT EXISTS idx_pbps_pb_siswa "
						+ "ON sekolah.pengaturan_biaya_punya_siswa (pengaturan_biaya, siswa) "
						+ "WHERE siswa IS NOT NULL",

				"CREATE INDEX IF NOT EXISTS idx_pbps_pb_calon "
						+ "ON sekolah.pengaturan_biaya_punya_siswa (pengaturan_biaya, calon_siswa) "
						+ "WHERE calon_siswa IS NOT NULL",

				// --- sekolah.kelas_punya_siswa (KelasSiswaPunyaSiswa) ---
				// Subquery: filter by kelas_id untuk membangun daftar siswa per kelas
				"CREATE INDEX IF NOT EXISTS idx_kps_kelas_siswa_aktif "
						+ "ON sekolah.kelas_punya_siswa (kelas_id, siswa_id, aktif) "
						+ "WHERE siswa_id IS NOT NULL",

				// --- sekolah.asrama_punya_siswa (AsramaSiswaPunyaSiswa) ---
				// Subquery: cari siswa dalam asrama tertentu
				"CREATE INDEX IF NOT EXISTS idx_aps_asrama_siswa "
						+ "ON sekolah.asrama_punya_siswa (asrama_id, siswa_id) "
						+ "WHERE siswa_id IS NOT NULL" };

		for (String sql : indexQueries) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:1216");
				/* Optional: instalasi lama mungkin belum punya kolom/tabel tertentu */
			}
		}

		String[] analyzeQueries = new String[] {
				"ANALYZE sekolah.nominal_biaya",
				"ANALYZE sekolah.tagihan",
				"ANALYZE sekolah.pengaturan_biaya_punya_siswa",
				"ANALYZE sekolah.kelas_punya_siswa",
				"ANALYZE sekolah.asrama_punya_siswa" };
		for (String sql : analyzeQueries) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:1230");
			}
		}
	}


	/*
	 * INIT_INDEX_DASBOR_PERGURUAN_TINGGI_TERPADU_BIMBINGAN_LULUSAN_FAST_2026_05_30
	 */
	/* INIT_INDEX_DASBOR_AKTIVITAS_MAHASISWA_FAST_2026_05_30 */
	/* INIT_INDEX_PENGADAAN_VENDOR_ASET_FAST_2026_05_30 */
	/**
	 * INDEX e-Learning SISI SEKOLAH (guru/siswa/admin) — melengkapi blok {@code idx_dash_el_*} yang selama ini
	 * HANYA menutup sisi Perguruan Tinggi (perkuliahan/pertemuan/detailperkuliahan). Menutup celah PEMINDAIAN
	 * PENUH saat memuat e-Learning untuk peran SEKOLAH (hasil deep-analysis 3 sub-agent atas query loader):
	 * <ul>
	 * <li><b>GURU</b>: {@code sekolah.jadwal_pelajaran} disaring {@code guru_id} (pengampu utama). Kolom
	 * guru2..guru12 (team-teaching, jarang) tetap dipersempit index filter sekolah+TA+semester yang sudah ada.</li>
	 * <li><b>SISWA / admin per-kelas</b>: {@code jadwal_pelajaran} disaring {@code kelas_id}; PLUS resolusi
	 * "kelas milik siswa" dari tabel jembatan {@code kelas_punya_siswa}/{@code kelas_les_punya_siswa} yang
	 * index-nya selama ini DIPIMPIN {@code kelas_id} (arah guru/admin) sehingga penyaringan {@code siswa_id}
	 * (arah siswa) memindai — kini ada index berpimpin {@code siswa_id}.</li>
	 * <li><b>Timeline pertemuan (semua peran, terutama ADMIN)</b>: filter {@code date(tanggal) BETWEEN ..}
	 * tak tersargable oleh index {@code tanggal} biasa → index EKSPRESI {@code (date(tanggal))} (aman: kolom
	 * {@code tanggal} bertipe timestamp — sama seperti {@code idx_pertemuan_mulai_coalesce} yang sudah ada).</li>
	 * </ul>
	 * Sudah dibandingkan manual ke seluruh InitIndex → TAK ada duplikasi (tabel/kolom sekolah ini belum
	 * ter-index). Anak-tabel per-pertemuan (tugas_pertemuan/pertemuan_file_content/video_pertemuan/... ) SUDAH
	 * ter-cover {@code idx_dash_el_*_(pertemuan, id)} sehingga tak diulang; index SINGLE-COLUMN {@code (pertemuan)}
	 * yang redundan pada tabel-tabel itu di-DROP (didominasi komposit tsb) — lihat idx_tugaspert_pertemuan_fk /
	 * idx_pfc_pertemuan / idx_vp_pertemuan / idx_ap_pertemuan yang kini jadi {@code DROP INDEX IF EXISTS}.
	 */
	private static void initIndexSekolahElearningSuperFast() {
		String[] indeksSekolah = new String[] {
				// GURU — pengampu utama (jadwal_pelajaran.guru_id) + TA/semester + id (paging/tampil).
				"CREATE INDEX IF NOT EXISTS idx_dash_el_jp_guru "
						+ "ON sekolah.jadwal_pelajaran (guru_id, tahun_ajaran, semester, id)",
				// SISWA / admin-per-kelas — jadwal_pelajaran.kelas_id + TA/semester + id.
				"CREATE INDEX IF NOT EXISTS idx_dash_el_jp_kelas "
						+ "ON sekolah.jadwal_pelajaran (kelas_id, tahun_ajaran, semester, id)",
				// JOIN mata pelajaran (kata kunci kode/nama) — matapelajaran_id.
				"CREATE INDEX IF NOT EXISTS idx_dash_el_jp_mapel "
						+ "ON sekolah.jadwal_pelajaran (matapelajaran_id, id)",
				// SISWA — resolusi kelas dari siswa (arah siswa_id; index lama dipimpin kelas_id).
				"CREATE INDEX IF NOT EXISTS idx_dash_el_kps_siswa "
						+ "ON sekolah.kelas_punya_siswa (siswa_id, aktif, kelas_id)",
				"CREATE INDEX IF NOT EXISTS idx_dash_el_klps_siswa "
						+ "ON sekolah.kelas_les_punya_siswa (siswa_id, aktif, kelas_id)",
				// TIMELINE pertemuan — filter date(tanggal) BETWEEN (semua peran; paling menolong admin).
				"CREATE INDEX IF NOT EXISTS idx_dash_el_pertemuan_date_tanggal " + "ON pertemuan (DATE(tanggal))",
				// SINKRON/BLOCKING saat loadMenu() init (SEMUA peran, termasuk admin — memblok progres awal
				// "Menyiapkan tampilan..."): 2 query formulir_kegiatan (jenis_formulir_kegiatan IS NULL / IS NOT
				// NULL + aktif) memindai tabel → index (jenis_formulir_kegiatan, aktif) menyanggupi keduanya.
				"CREATE INDEX IF NOT EXISTS idx_dash_el_fk_jenis_aktif "
						+ "ON formulir_kegiatan (jenis_formulir_kegiatan, aktif)" };
		for (String sql : indeksSekolah) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:1288");
				// Index opsional: instalasi tanpa modul sekolah tetap boleh jalan.
			}
		}
		// ANALYZE pertemuan bisa memindai tabel besar (banyak instalasi) — timeout default
		// (mengikuti statement_timeout bawaan koneksi, seringkali sangat pendek) membuat
		// ANALYZE selalu dibatalkan (SQLState 57014) sebelum statistik planner sempat
		// diperbarui. Pakai eksekusiSql10Menit (SET LOCAL statement_timeout ke 600 detik,
		// hanya berlaku untuk transaksi ANALYZE ini) agar ANALYZE sempat selesai.
		String[] analyzeSekolah = new String[] { "ANALYZE sekolah.jadwal_pelajaran", "ANALYZE sekolah.kelas_punya_siswa",
				"ANALYZE sekolah.kelas_les_punya_siswa", "ANALYZE pertemuan", "ANALYZE formulir_kegiatan" };
		for (String sql : analyzeSekolah) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:1297");
			}
		}
	}

	/**
	 * Index untuk halaman "Informasi Pembayaran Mahasiswa" ({@code InformasiPembayaranMahasiswaAction})
	 * yang lambat dimuat. Beban utama: {@code PembayaranUtilHelper.getDetailBiayaMahasiswadariDatabase}
	 * (fan-out ~8-15 query) dipanggil (jumlah jenisKegiatan × jumlah semester) kali pada task-loop
	 * {@code TagihanUIBuilder}, plus rekap dashboard per-kegiatan.
	 *
	 * <p><b>Analisis NON-REDUNDAN</b> (agar tidak menduplikasi index yang sudah ada):</p>
	 * <ul>
	 *   <li>{@code detail_biaya}: query utama memfilter angkatan/jurusan/semester/jenis_kegiatan/jenjang
	 *       (semuanya EQ) → SUDAH dilayani {@code idx_detail_biaya_kompleks_mhs}. TIDAK ditambah lagi.</li>
	 *   <li>{@code detail_setting_biaya (setting_biaya,item_biaya)} → SUDAH {@code idx_detail_setting_biaya_sb}
	 *       (prefix). {@code setting_biaya (ta,jenis_kegiatan,…)} → SUDAH {@code idx_setting_biaya_core}.</li>
	 *   <li>{@code cicilan_pembayaran (kegiatan,…)} & {@code detail_kegiatan (kegiatan,…)} → SUDAH
	 *       {@code idx_cicilan_kegiatan_id_cover} / {@code idx_dk_kegiatan_id_cover}.</li>
	 *   <li>{@code setting_biaya_detail}: {@code idx_setting_biaya_detail_lookup} MEMIMPIN dgn
	 *       {@code biodata_calon_mahasiswa} → tak terpakai untuk filter {@code mahasiswa = ?} (jalur
	 *       mahasiswa, bukan calon). Perlu index yang MEMIMPIN {@code mahasiswa}. BARU.</li>
	 *   <li>{@code biodata_mahasiswa}: query proyeksi ({@code mahasiswa=?}, order id desc, maxResults 1)
	 *       untuk {@code jenisTinggalMahasiswa} & {@code parameterTambahanInds} — BELUM ada index. BARU.</li>
	 * </ul>
	 */
	private static void initIndexInformasiPembayaranMahasiswaSuperFast() {
		String[] indexes = new String[] {
				// SettingBiayaDetail jalur MAHASISWA (idx_setting_biaya_detail_lookup memimpin
				// biodata_calon_mahasiswa → tak dipakai untuk mahasiswa=?).
				"CREATE INDEX IF NOT EXISTS idx_sbd_mahasiswa_sb ON setting_biaya_detail (mahasiswa, setting_biaya)",
				// BiodataMahasiswa: proyeksi jenisTinggalMahasiswa & parameterTambahanInds per fan-out.
				"CREATE INDEX IF NOT EXISTS idx_biodata_mhs_mahasiswa ON biodata_mahasiswa (mahasiswa, id DESC)" };
		for (String sql : indexes) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitIndex.java:1334");
			}
		}
		String[] analyze = new String[] { "ANALYZE setting_biaya_detail", "ANALYZE biodata_mahasiswa" };
		for (String sql : analyze) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:1341");
			}
		}
	}

	/**
	 * Index untuk fallback DB baru di {@code ConstantValues.ambilByNim} (2026-08-06).
	 *
	 * <p><b>Konteks.</b> {@code ambilByNim} sebelumnya HANYA scan cache in-memory
	 * ({@code MemoryCacheUtil}), yang di-warm-start terbatas mahasiswa 3 tahun angkatan
	 * terakhir ({@code InitDataHelper}) — mahasiswa lebih lama SELALU "tidak ditemukan" di
	 * inquiry H2H bank (insiden BSI, mahasiswa angkatan 2022 semester 9) walau datanya valid.
	 * Fix menambah fallback query DB langsung: {@code Restrictions.eq("nim", nim).ignoreCase()}
	 * dan (fallback kedua) {@code Restrictions.eq("nama", nim).ignoreCase()} pada
	 * {@code public.mahasiswa}.</p>
	 *
	 * <p><b>Kenapa index BARU diperlukan.</b> Hibernate {@code ignoreCase()} menghasilkan
	 * {@code WHERE lower(nim) = ?} / {@code WHERE lower(nama) = ?} — index BIASA pada kolom
	 * {@code nim}/{@code nama} TIDAK bisa dipakai planner untuk bentuk ekspresi {@code lower(...)}
	 * ini; wajib index FUNGSIONAL {@code (lower(kolom))}. Index GIN trigram yang sudah ada
	 * ({@code idx_trgm_mhs_nim}/{@code idx_trgm_mhs_nama}, utk {@code LIKE '%..%'}) juga TIDAK
	 * membantu query kesetaraan (=) ini — beda bentuk operasi, beda tipe index.</p>
	 *
	 * <p><b>Anti-duplikat.</b> Dicek: tidak ada index {@code (lower(nim))}/{@code (lower(nama))}
	 * di tempat lain pada file ini sebelum penambahan ini.</p>
	 */
	private static void initIndexAmbilByNimFallbackSuperFast() {
		String[] indexes = new String[] {
				"CREATE INDEX IF NOT EXISTS idx_mhs_lower_nim ON public.mahasiswa (lower(nim))",
				"CREATE INDEX IF NOT EXISTS idx_mhs_lower_nama ON public.mahasiswa (lower(nama))" };
		for (String sql : indexes) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e,
						"auto-audit(empty-catch) src/ais/common/InitIndex.java:initIndexAmbilByNimFallbackSuperFast");
			}
		}
		try {
			eksekusiSql10Menit("ANALYZE public.mahasiswa");
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/common/InitIndex.java:initIndexAmbilByNimFallbackSuperFast-analyze");
		}
	}

	/**
	 * Index untuk UPLOAD / DOWNLOAD / HAPUS perencanaan anggaran (RAB) di
	 * {@code WorkspaceRevisiAction} / {@code WorkspaceRevisiBulananAction}.
	 *
	 * <p><b>Masalah hapus SANGAT lambat:</b> {@code onDeleteWorkspace} untuk SETIAP workspace
	 * menjalankan 4 {@code DELETE FROM rab.workspace_punya_* WHERE workspace = <id>}. Tanpa index
	 * pada kolom {@code workspace} di keempat tabel anak, tiap DELETE = seq-scan → hapus macet
	 * ("Processing…"). Indeks {@code (workspace)} membuatnya instan.</p>
	 *
	 * <p><b>Filter utama upload/download/hapus/max-revisi</b> pada {@code rab.workspace} memfilter
	 * {@code satuan_kerja + sumber_dana + revisi + tahun_workspace} (urut {@code kode}). Index yang
	 * ADA memimpin {@code tahun_workspace} (idx_workspace_dashboard_realisasi_main) → tidak melayani
	 * filter yang memimpin satuan_kerja. Ditambah composite memimpin {@code satuan_kerja} (NON-REDUNDAN).</p>
	 */
	private static void initIndexRabWorkspaceUploadHapus() {
		String[] indexes = new String[] {
				// Filter upload/download/hapus/max-revisi (lead satuan_kerja; beda dari index dashboard).
				"CREATE INDEX IF NOT EXISTS idx_workspace_sk_sd_thn_rev_kode ON rab.workspace (satuan_kerja, sumber_dana, tahun_workspace, revisi, kode)",
				// HAPUS anggaran: DELETE tabel anak WHERE workspace=id (tanpa index = seq-scan lambat).
				"CREATE INDEX IF NOT EXISTS idx_wp_indikator_workspace ON rab.workspace_punya_indikator (workspace)",
				"CREATE INDEX IF NOT EXISTS idx_wp_jenis_parameter_workspace ON rab.workspace_punya_jenis_parameter (workspace)",
				"CREATE INDEX IF NOT EXISTS idx_wp_pegawai_workspace ON rab.workspace_punya_pegawai (workspace)",
				"CREATE INDEX IF NOT EXISTS idx_wp_sasaran_workspace ON rab.workspace_punya_sasaran (workspace)" };
		for (String sql : indexes) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitIndex.java:1373");
			}
		}
		String[] analyze = new String[] { "ANALYZE rab.workspace", "ANALYZE rab.workspace_punya_indikator",
				"ANALYZE rab.workspace_punya_jenis_parameter", "ANALYZE rab.workspace_punya_pegawai",
				"ANALYZE rab.workspace_punya_sasaran" };
		for (String sql : analyze) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:1382");
			}
		}
	}

	/**
	 * Gap-closure "Katalog Barang banyak yang double" (POS/Kantin, laporan Toko Al-Bahjah) --
	 * {@code koperasi.produk.kunci_unik} (kolom baru, dihitung otomatis oleh
	 * {@code Produk.hitungKunciUnik()} SETIAP baris disimpan lewat jalur mana pun -- POS Desktop/
	 * Android, batch Unggah Excel, form JSP/ZK lama -- lihat JavaDoc lengkap
	 * {@code ais.common.ProdukKunciUnikUtil}) diberi UNIQUE INDEX SUNGGUHAN di sini supaya
	 * database SENDIRI yang menolak baris duplikat, bukan cuma diandalkan lewat validasi aplikasi.
	 *
	 * <p><b>Kenapa PARTIAL (klausa WHERE), bukan unique index polos:</b> baris lama yang belum
	 * pernah tersentuh sejak kolom ini ada, atau baris tanpa kode/nama (kunci_unik dikosongkan
	 * sengaja, lihat JavaDoc kolom itu), akan ber-{@code kunci_unik} NULL/kosong -- Postgres SUDAH
	 * memperbolehkan banyak NULL pada unique index biasa, tapi STRING KOSONG {@code ''} tidak
	 * dianggap NULL, jadi tetap perlu dikecualikan eksplisit lewat WHERE supaya baris2 semacam itu
	 * tidak saling bentrok satu sama lain.</p>
	 *
	 * <p><b>Index OPSIONAL, sengaja gagal-toleran:</b> kalau MASIH ADA baris duplikat (kunci_unik
	 * sama) tersisa di database saat statement ini jalan -- mis. toko yang belum pernah menekan
	 * tombol "Cari Duplikat: Kunci Unik (Disarankan)" di layar Katalog Barang -- CREATE UNIQUE
	 * INDEX ini GAGAL (perilaku normal Postgres, bukan bug) dan cuma dicatat via
	 * ErrorAuditUtil, TIDAK menghentikan startup aplikasi lain. Index otomatis TERPASANG dengan
	 * sendirinya pada restart PERTAMA setelah data toko itu selesai dibersihkan -- tidak perlu
	 * migrasi manual terpisah.</p>
	 */
	private static void initIndexProdukKunciUnik() {
		String[] indexes = new String[] {
				"CREATE UNIQUE INDEX IF NOT EXISTS idx_produk_kunci_unik ON koperasi.produk (kunci_unik) "
						+ "WHERE kunci_unik IS NOT NULL AND kunci_unik <> ''" };
		for (String sql : indexes) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:initIndexProdukKunciUnik");
			}
		}
	}

	private static void initIndexRevisiEnversGenerik() {
		// PENTING: seluruh isi LOOP dan blok revinfo di bawah DIBUNGKUS "BEGIN ... EXCEPTION WHEN
		// OTHERS THEN ... END;" (savepoint implisit PL/pgSQL) PER TABEL. Sebelumnya TIDAK ada
		// penanganan exception di dalam LOOP: satu tabel _aud yang gagal (paling mungkin lock
		// timeout/kontensi -- tabel _aud Envers ditulis TERUS-MENERUS oleh trafik hidup setiap kali
		// entitas apa pun diubah, sedangkan CREATE INDEX butuh SHARE lock yang bentrok dengan
		// penulisan) membuat SELURUH statement DO $$ ini di-ROLLBACK, sehingga index utk SEMUA tabel
		// _aud LAIN yang sebenarnya sukses diproses lebih dulu di iterasi LOOP yang sama ikut hilang
		// (gejala persis "Transaction di-rollback untuk mencegah status 'aborted' pada koneksi" yang
		// dilaporkan). Dengan blok BEGIN/EXCEPTION per tabel, kegagalan pada satu tabel di-skip (via
		// RAISE WARNING, tercatat di log server) tanpa membatalkan index tabel _aud lainnya dan tanpa
		// mengubah hasil akhir untuk kasus normal (masih idempoten, CREATE INDEX IF NOT EXISTS).
		final String sql = ""
				+ "DO $$ "
				+ "DECLARE r record; nama_rev text; nama_revtype text; nama_id_rev text; "
				+ "BEGIN "
				+ "FOR r IN SELECT table_schema, table_name FROM information_schema.tables "
				+ "WHERE table_schema = 'public' AND table_name LIKE '%\\_aud' ESCAPE '\\' LOOP "
				+ "BEGIN "
				// Hibernate 3 mem-parsing ':' sebagai awalan named parameter bahkan di blok
				// PL/pgSQL. PostgreSQL menerima '=' sebagai operator assignment yang setara
				// dengan ':=', jadi hindari ':' agar DO block lolos ParameterParser Hibernate.
				+ "nama_rev = 'idx_aud_rev_' || substr(md5(r.table_name || '_rev'), 1, 14); "
				+ "nama_revtype = 'idx_aud_rvt_' || substr(md5(r.table_name || '_revtype'), 1, 14); "
				+ "nama_id_rev = 'idx_aud_idr_' || substr(md5(r.table_name || '_id_rev'), 1, 14); "
				+ "IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = r.table_schema AND table_name = r.table_name AND column_name = 'rev') THEN "
				+ "EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (rev)', nama_rev, r.table_schema, r.table_name); "
				+ "END IF; "
				+ "IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = r.table_schema AND table_name = r.table_name AND column_name = 'revtype') "
				+ "AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = r.table_schema AND table_name = r.table_name AND column_name = 'rev') THEN "
				+ "EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (revtype, rev)', nama_revtype, r.table_schema, r.table_name); "
				+ "END IF; "
				+ "IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = r.table_schema AND table_name = r.table_name AND column_name = 'id') "
				+ "AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = r.table_schema AND table_name = r.table_name AND column_name = 'rev') THEN "
				+ "EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (id, rev DESC)', nama_id_rev, r.table_schema, r.table_name); "
				+ "END IF; "
				+ "EXCEPTION WHEN OTHERS THEN "
				+ "RAISE WARNING 'initIndexRevisiEnversGenerik - gagal membuat index audit utk %.% - %', r.table_schema, r.table_name, SQLERRM; "
				+ "END; "
				+ "END LOOP; "
				+ "BEGIN "
				+ "IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'revinfo') THEN "
				+ "EXECUTE 'CREATE INDEX IF NOT EXISTS idx_revinfo_rev_revtstmp ON public.revinfo (rev, revtstmp)'; "
				+ "END IF; "
				+ "EXCEPTION WHEN OTHERS THEN "
				+ "RAISE WARNING 'initIndexRevisiEnversGenerik - gagal membuat index revinfo - %', SQLERRM; "
				+ "END; "
				+ "END $$;";
		submitDdl(new Runnable() {
			@Override
			public void run() {
				try {
					ais.common.Common.updateSql10Menit(sql);
				} catch (Throwable e) {
					ais.common.ErrorAuditUtil.record(e,
							"auto-audit(empty-catch) src/ais/common/InitIndex.java:initIndexRevisiEnversGenerik");
				}
			}
		});
	}

	/**
	 * Sinkronkan skema instalasi lama dengan model {@code AturanDiskon}: produk
	 * memang opsional, dan nilai NULL berarti promo berlaku untuk semua produk.
	 *
	 * Perintah yang sama dahulu hanya ada di webapp/cascade.sql. Berkas itu tidak
	 * dijalankan oleh alur build/deploy Tomcat, sehingga database produksi lama
	 * tetap memiliki NOT NULL dan menolak promo global dengan SQLState 23502.
	 * ALTER COLUMN ... DROP NOT NULL idempoten serta tidak membuat index/constraint
	 * redundan. Dijalankan sebelum DDL pool aktif agar migrasi selesai sebelum
	 * inisialisasi startup dilanjutkan.
	 */
	static void initAturanDiskonProdukNullable() {
		String[] migrasi = new String[] {
				"ALTER TABLE koperasi.aturan_diskon ALTER COLUMN produk DROP NOT NULL",
				"ALTER TABLE koperasi.aturan_diskon ADD COLUMN IF NOT EXISTS prioritas integer DEFAULT 100",
				"ALTER TABLE koperasi.aturan_diskon ADD COLUMN IF NOT EXISTS dapat_digabung boolean DEFAULT false",
				"ALTER TABLE koperasi.aturan_diskon ADD COLUMN IF NOT EXISTS dasar_perhitungan varchar(30) DEFAULT 'SETELAH_DISKON'",
				"ALTER TABLE koperasi.aturan_diskon ADD COLUMN IF NOT EXISTS grup_eksklusif varchar(100)" };
		for (int i = 0; i < migrasi.length; i++) {
			try {
				ais.common.Common.updateSql(migrasi[i]);
			} catch (Exception e) {
				e.printStackTrace();
				ais.common.ErrorAuditUtil.record(e,
						"auto-audit InitIndex.initAturanDiskonProdukNullable");
			}
		}
	}

	/** Menyamakan lebar kolom tabel idempotensi instalasi lama secara aman. */
	static void initRetailRequestIdempotencyColumns() {
		org.hibernate.Session session = null; org.hibernate.Transaction tx = null;
		java.sql.PreparedStatement cek = null; java.sql.ResultSet rs = null; java.sql.Statement ddl = null;
		try {
			session = ais.database.hibernate.HibernateUtil.getSessionFactory().openSession(); tx = session.beginTransaction();
			cek = session.connection().prepareStatement("SELECT 1 FROM information_schema.tables WHERE table_schema=? AND table_name=?");
			cek.setString(1, "public"); cek.setString(2, "retail_request_idempotency"); rs = cek.executeQuery();
			if (!rs.next()) { tx.commit(); return; }
			ddl = session.connection().createStatement();
			ddl.executeUpdate("ALTER TABLE public.retail_request_idempotency ALTER COLUMN action TYPE varchar(80)");
			ddl.executeUpdate("ALTER TABLE public.retail_request_idempotency ALTER COLUMN idempotency_key TYPE varchar(160)");
			ddl.executeUpdate("ALTER TABLE public.retail_request_idempotency ALTER COLUMN request_hash TYPE varchar(64)");
			ddl.executeUpdate("ALTER TABLE public.retail_request_idempotency ALTER COLUMN status TYPE varchar(20)");
			ddl.executeUpdate("ALTER TABLE public.retail_request_idempotency ALTER COLUMN result_reference TYPE varchar(160)");
			tx.commit();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) try { tx.rollback(); } catch (Exception rollback) { ErrorAuditUtil.record(rollback, "initRetailRequestIdempotencyColumns-rollback"); }
			ErrorAuditUtil.record(e, "auto-audit InitIndex.initRetailRequestIdempotencyColumns");
		} finally {
			try { if (rs != null) rs.close(); } catch (Exception e) { ErrorAuditUtil.record(e, "initRetailRequestIdempotencyColumns-rs-close"); }
			try { if (cek != null) cek.close(); } catch (Exception e) { ErrorAuditUtil.record(e, "initRetailRequestIdempotencyColumns-cek-close"); }
			try { if (ddl != null) ddl.close(); } catch (Exception e) { ErrorAuditUtil.record(e, "initRetailRequestIdempotencyColumns-ddl-close"); }
			if (session != null) {
				try { session.clear(); } catch (Exception e) { ErrorAuditUtil.record(e, "initRetailRequestIdempotencyColumns-clear"); }
				try { session.disconnect(); } catch (Exception e) { ErrorAuditUtil.record(e, "initRetailRequestIdempotencyColumns-disconnect"); }
				try { session.close(); } catch (Exception e) { ErrorAuditUtil.record(e, "initRetailRequestIdempotencyColumns-close"); }
			}
		}
	}

	/**
	 * Seed dua Jenis Reimbursement default saat bootstrap Tomcat:
	 * (1) "Menggunakan Anggaran" — pengaju wajib memilih Anggaran (Workspace);
	 * (2) "Tanpa Anggaran" — akun biaya ditentukan admin pada jenis (bukan per
	 * pengajuan). Idempoten: tabel dibuat bila belum ada (kolom lengkap
	 * disempurnakan hbm2ddl) dan seed hanya berjalan saat tabel masih KOSONG,
	 * sehingga perubahan admin tidak pernah tertimpa restart.
	 */
	static void initDefaultJenisReimbursement() {
		org.hibernate.Session session = null;
		org.hibernate.Transaction tx = null;
		java.sql.Statement ddl = null;
		try {
			session = ais.database.hibernate.HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			ddl = session.connection().createStatement();
			ddl.executeUpdate("CREATE TABLE IF NOT EXISTS akunting.jenis_reimbursement ("
					+ "id bigserial PRIMARY KEY, nama varchar(255), keterangan text, "
					+ "menggunakan_anggaran boolean, akun int8, satuan_kerja int8, "
					+ "aktif boolean, tanggal_dirubah timestamp without time zone)");
			ddl.executeUpdate("INSERT INTO akunting.jenis_reimbursement "
					+ "(nama, keterangan, menggunakan_anggaran, aktif, tanggal_dirubah) "
					+ "SELECT x.nama, x.ket, x.mg, true, now() FROM (VALUES "
					+ "('Menggunakan Anggaran', 'Pengaju wajib memilih Anggaran (Workspace) pada form pengajuan', true), "
					+ "('Tanpa Anggaran', 'Akun biaya ditentukan pada jenis ini oleh admin - pengaju tidak memilih akun', false)"
					+ ") AS x(nama, ket, mg) "
					+ "WHERE NOT EXISTS (SELECT 1 FROM akunting.jenis_reimbursement)");
			tx.commit();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) try { tx.rollback(); } catch (Exception rollback) {
				ErrorAuditUtil.record(rollback, "initDefaultJenisReimbursement-rollback");
			}
			ErrorAuditUtil.record(e, "auto-audit InitIndex.initDefaultJenisReimbursement");
		} finally {
			try { if (ddl != null) ddl.close(); } catch (Exception e) {
				ErrorAuditUtil.record(e, "initDefaultJenisReimbursement-ddl-close");
			}
			if (session != null) {
				try { session.clear(); } catch (Exception e) { ErrorAuditUtil.record(e, "initDefaultJenisReimbursement-clear"); }
				try { session.disconnect(); } catch (Exception e) { ErrorAuditUtil.record(e, "initDefaultJenisReimbursement-disconnect"); }
				try { session.close(); } catch (Exception e) { ErrorAuditUtil.record(e, "initDefaultJenisReimbursement-close"); }
			}
		}
	}

	/**
	 * Seed master "Jenis Pengeluaran" reimbursement saat bootstrap Tomcat: ±55 jenis
	 * pengeluaran yang lazim di-reimburse di lapangan. Akun biaya per jenis
	 * dilengkapi admin lewat tab "Jenis Pengeluaran" (tidak di-seed karena kode akun
	 * berbeda-beda per tenant). Idempoten: tabel dibuat bila belum ada dan seed
	 * hanya berjalan saat tabel masih KOSONG.
	 */
	static void initDefaultJenisPengeluaran() {
		org.hibernate.Session session = null;
		org.hibernate.Transaction tx = null;
		java.sql.Statement ddl = null;
		try {
			session = ais.database.hibernate.HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			ddl = session.connection().createStatement();
			ddl.executeUpdate("CREATE TABLE IF NOT EXISTS akunting.jenis_pengeluaran ("
					+ "id bigserial PRIMARY KEY, nama varchar(255), keterangan text, "
					+ "akun int8, jenis_asset int8, aktif boolean, "
					+ "tanggal_dirubah timestamp without time zone)");

			String[] jenis = new String[] {
					"BBM / Bensin", "Parkir", "Tol", "Transportasi Online (Ojek/Taksi Online)", "Taksi / Angkutan Umum",
					"Tiket Kereta Api", "Tiket Pesawat", "Tiket Bus / Travel", "Penginapan / Hotel", "Uang Makan Perjalanan Dinas",
					"Konsumsi Rapat", "Snack / Kudapan Rapat", "Konsumsi Tamu", "Air Minum / Galon", "Gas LPG",
					"ATK (Alat Tulis Kantor)", "Fotokopi / Penggandaan", "Penjilidan / Cetak Dokumen", "Materai", "Kertas",
					"Toner / Tinta Printer", "Pengiriman Dokumen / Kurir / Ekspedisi", "Pulsa / Paket Data", "Langganan Internet", "Token / Tagihan Listrik",
					"Perbaikan Kendaraan Dinas", "Servis Rutin Kendaraan", "Oli / Ban Kendaraan", "Perbaikan AC", "Perbaikan Komputer / Laptop",
					"Sparepart Komputer / Aksesoris", "Perbaikan Printer", "Perbaikan Instalasi Listrik", "Perbaikan Ledeng / Sanitasi", "Perbaikan Gedung Minor",
					"Cat / Bahan Bangunan Minor", "Lampu / Alat Listrik", "Kunci / Duplikat Kunci", "Peralatan Kebersihan", "Peralatan Dapur",
					"Perkakas / Tools", "Obat-obatan / P3K", "Biaya Medis Ringan", "Seragam / Atribut", "Spanduk / Banner / Percetakan",
					"Dekorasi Acara", "Sewa Sound System / Proyektor", "Sewa Kendaraan", "Sewa Tempat / Ruangan", "Karangan Bunga / Bingkisan",
					"Sumbangan Duka / Sosial", "Biaya Pelatihan / Workshop", "Biaya Seminar / Pendaftaran", "Buku / Referensi", "Langganan Software / Lisensi",
					"Domain / Hosting", "Notaris / Legalisir", "Perizinan / Administrasi Pemerintah", "Biaya Bank / Admin Transfer", "Retribusi / Iuran Lingkungan" };

			StringBuilder values = new StringBuilder();
			for (int i = 0; i < jenis.length; i++) {
				if (values.length() > 0) {
					values.append(", ");
				}
				values.append("('").append(jenis[i].replace("'", "''")).append("')");
			}
			ddl.executeUpdate("INSERT INTO akunting.jenis_pengeluaran (nama, aktif, tanggal_dirubah) "
					+ "SELECT x.nama, true, now() FROM (VALUES " + values + ") AS x(nama) "
					+ "WHERE NOT EXISTS (SELECT 1 FROM akunting.jenis_pengeluaran)");
			tx.commit();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) try { tx.rollback(); } catch (Exception rollback) {
				ErrorAuditUtil.record(rollback, "initDefaultJenisPengeluaran-rollback");
			}
			ErrorAuditUtil.record(e, "auto-audit InitIndex.initDefaultJenisPengeluaran");
		} finally {
			try { if (ddl != null) ddl.close(); } catch (Exception e) {
				ErrorAuditUtil.record(e, "initDefaultJenisPengeluaran-ddl-close");
			}
			if (session != null) {
				try { session.clear(); } catch (Exception e) { ErrorAuditUtil.record(e, "initDefaultJenisPengeluaran-clear"); }
				try { session.disconnect(); } catch (Exception e) { ErrorAuditUtil.record(e, "initDefaultJenisPengeluaran-disconnect"); }
				try { session.close(); } catch (Exception e) { ErrorAuditUtil.record(e, "initDefaultJenisPengeluaran-close"); }
			}
		}
	}

	/** Menambahkan penanda cara bayar hutang pada instalasi lama secara idempoten. */
	static void initCaraPembayaranMasukSebagaiHutang() {
		try {
			ais.common.Common.updateSql("ALTER TABLE koperasi.cara_pembayaran_koperasi "
					+ "ADD COLUMN IF NOT EXISTS masuk_sebagai_hutang boolean DEFAULT false");
		} catch (Exception e) {
			e.printStackTrace();
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit InitIndex.initCaraPembayaranMasukSebagaiHutang");
		}
	}

	/** Membuat master Kebijakan Retur dan relasinya ke Produk secara idempoten. */
	static void initKebijakanReturProduk() {
		synchronized (KEBIJAKAN_RETUR_PRODUK_LOCK) {
		try {
			ais.common.Common.updateSql("CREATE TABLE IF NOT EXISTS koperasi.kebijakan_retur ("
					+ "id bigserial PRIMARY KEY, nama varchar(255) NOT NULL, keterangan text, "
					+ "aktif boolean DEFAULT true, oleh varchar(255), oleh_id varchar(255), tanggal_dirubah timestamp without time zone DEFAULT now())");
			ais.common.Common.updateSql("CREATE UNIQUE INDEX IF NOT EXISTS kebijakan_retur_nama_uq ON koperasi.kebijakan_retur (lower(btrim(nama)))");
			ais.common.Common.updateSql("INSERT INTO koperasi.kebijakan_retur(nama,keterangan,aktif) SELECT 'Tanpa Kebijakan Retur','Produk tidak menerima retur, kecuali diwajibkan oleh ketentuan yang berlaku.',true WHERE NOT EXISTS (SELECT 1 FROM koperasi.kebijakan_retur WHERE lower(btrim(nama))=lower('Tanpa Kebijakan Retur'))");
			ais.common.Common.updateSql("ALTER TABLE koperasi.produk ADD COLUMN IF NOT EXISTS kebijakan_retur bigint");
			ais.common.Common.updateSql("UPDATE koperasi.produk SET kebijakan_retur=(SELECT id FROM koperasi.kebijakan_retur WHERE lower(btrim(nama))=lower('Tanpa Kebijakan Retur') ORDER BY id LIMIT 1) WHERE kebijakan_retur IS NULL");
			ais.common.Common.updateSql("CREATE INDEX IF NOT EXISTS produk_kebijakan_retur_idx ON koperasi.produk(kebijakan_retur)");
			if (!constraintKebijakanReturSudahAda()) {
				try {
					ais.common.Common.updateSql(
							"ALTER TABLE koperasi.produk ADD CONSTRAINT produk_kebijakan_retur_fk FOREIGN KEY (kebijakan_retur) REFERENCES koperasi.kebijakan_retur(id)");
				} catch (Exception eAlter) {
					if (!constraintKebijakanReturSudahAda(eAlter)) {
						throw eAlter;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			ais.common.ErrorAuditUtil.record(e, "auto-audit InitIndex.initKebijakanReturProduk");
		}
		}
	}

	private static boolean constraintKebijakanReturSudahAda() {
		java.util.List constraint = ais.common.Common.ambilSql(
				"SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema='koperasi' AND table_schema='koperasi' AND table_name='produk' AND constraint_name='produk_kebijakan_retur_fk'");
		return constraint != null && !constraint.isEmpty();
	}

	private static boolean constraintKebijakanReturSudahAda(Throwable e) {
		Throwable t = e;
		while (t != null) {
			String pesan = t.getMessage();
			if (pesan != null) {
				String lower = pesan.toLowerCase(java.util.Locale.ENGLISH);
				if (lower.indexOf("produk_kebijakan_retur_fk") >= 0
						&& (lower.indexOf("already exists") >= 0 || lower.indexOf("duplicate") >= 0
								|| lower.indexOf("sudah ada") >= 0)) {
					return true;
				}
			}
			t = t.getCause();
		}
		return false;
	}

	public static void initEksekusiQueryIndex() {
		// Migrasi kompatibilitas skema harus selesai secara sinkron sebelum pool
		// pekerjaan index paralel diaktifkan.
		initAturanDiskonProdukNullable();
		initCaraPembayaranMasukSebagaiHutang();
		initKebijakanReturProduk();
		initDefaultJenisReimbursement();
		initDefaultJenisPengeluaran();

		// 1. EKSTENSI TRIGRAM (WAJIB) — dijalankan SINKRON (sebelum pool paralel aktif) karena
		// seluruh index GIN trigram bergantung pada ekstensi ini; harus tersedia lebih dulu.
		try {
			eksekusiSql("CREATE EXTENSION IF NOT EXISTS pg_trgm");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:1392");
		}

		// Aktifkan eksekusi DDL PARALEL (pool kecil daemon) untuk sisa indeks -> jauh lebih cepat
		// daripada satu-per-satu, namun tetap aman terhadap pool koneksi & lock tabel.
		final java.util.concurrent.ExecutorService ddlPool = java.util.concurrent.Executors.newFixedThreadPool(
				DDL_PARALEL_THREAD, new java.util.concurrent.ThreadFactory() {
					private final java.util.concurrent.atomic.AtomicInteger no = new java.util.concurrent.atomic.AtomicInteger(
							1);

					@Override
					public Thread newThread(Runnable r) {
						Thread t = new Thread(r, "init-index-ddl-" + no.getAndIncrement());
						t.setDaemon(true);
						return t;
					}
				});
		DDL_POOL = ddlPool;
		DDL_FUTURES.clear();
		try {
			initAlterTableParameterTambahanAngketUmum();
			initDropConstraintNoRegistrasiBiodataCalonMahasiswa();
			initFkCascadeDiskonSiswaItemBiaya();
			initKonsolidasiEbisnisMenuTbmrole();
			initDefaultMenuKantin();
			initRoleGrupProduk();
		initIndexPerpustakaanCoverDanPmbKuota();
		initIndexPmbPortalDanNomorUjianSuperFast();
		initIndexDashboardStatistikPmbSuperFast();
		initIndexDepositTabunganSuperFast();
		initIndexVirtualAccountPaymentSuperFast();
		initIndexDaftarUlangPembayaranSuperFast();
		initIndexAlurSopWorkflowSuperFast();
		initIndexSekolahElearningSuperFast();
		initIndexInformasiPembayaranMahasiswaSuperFast();
		initIndexAmbilByNimFallbackSuperFast();
		initIndexRevisiEnversGenerik();
		initIndexRabWorkspaceUploadHapus();
		initIndexProdukKunciUnik();

		// 2. INDEKS PENCARIAN TEKS GIN (LIKE '%keyword%')
		String[] ginIndexes = { "CREATE INDEX IF NOT EXISTS idx_trgm_dosen_nama ON dosen USING gin (nama gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_mhs_nama ON mahasiswa USING gin (nama gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_mhs_nim ON mahasiswa USING gin (nim gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_mhs_kelas ON mahasiswa USING gin (kelas gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_mk_nama ON matakuliah USING gin (nama gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_mk_kode ON matakuliah USING gin (kode gin_trgm_ops)",
				// e-Learning: filter "kelas" panel kiri (admin) memakai ILIKE ANYWHERE pada perkuliahan.kelas
				// (initCriteria) — non-sargable tanpa trigram. Konsisten dgn idx_trgm_mhs_kelas yang sudah ada.
				"CREATE INDEX IF NOT EXISTS idx_trgm_perkuliahan_kelas ON perkuliahan USING gin (kelas gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_mp_nama ON sekolah.matapelajaran USING gin (nama gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_mp_kode ON sekolah.matapelajaran USING gin (kode gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_skripsi_judul ON skripsi USING gin (judul gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_skripsi_keyword ON skripsi USING gin (keyword gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_reqta_judul ON mahasiswa_request_tugas_akhir USING gin (judul gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_pertemuan_judultugas ON pertemuan USING gin (judultugas gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_tugaspert_judultugas ON tugas_pertemuan USING gin (judultugas gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_pert_siswas ON pertemuan USING gin (siswas gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_pert_mhs ON pertemuan USING gin (mahasiswas gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_pert_dsn ON pertemuan USING gin (dosens gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_pert_guru ON pertemuan USING gin (gurus gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_pert_topik ON pertemuan USING gin (topik gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_pert_catatan ON pertemuan USING gin (catatan gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_pert_indikator ON pertemuan USING gin (indikator gin_trgm_ops)" };
		for (String sql : ginIndexes) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:1452");
			}
		}

		// 3. INDEKS KOMPOSIT B-TREE (FILTER PRESISI & PAGING / ORDER BY)
		String[] btreeIndexes = {
				// Perkuliahan
				"CREATE INDEX IF NOT EXISTS idx_perkuliahan_filter ON perkuliahan (tahun_ajaran, semester, aktif)",
				"CREATE INDEX IF NOT EXISTS idx_perkuliahan_sorting ON perkuliahan (tahun_ajaran DESC, status_semesterpendek ASC, semester_perkuliahan DESC, ganjil_genap DESC, hari, waktu_mulai_d)",
				"CREATE INDEX IF NOT EXISTS idx_perkuliahan_jurusan ON perkuliahan (jurusan)",
				// Jadwal Pelajaran
				"CREATE INDEX IF NOT EXISTS idx_jadwalpelajaran_filter ON sekolah.jadwal_pelajaran (sekolah_id, yayasan_id, tahun_ajaran, semester)",
				"CREATE INDEX IF NOT EXISTS idx_jadwalpelajaran_sorting ON sekolah.jadwal_pelajaran (tahun_ajaran DESC, semester ASC, hari, waktumulai)",
				// KRS & Skripsi
				"CREATE INDEX IF NOT EXISTS idx_krs_mahasiswa_filter ON krs_mahasiswa (mahasiswa, tahunakademik, semester, semesterpendek)",
				"CREATE INDEX IF NOT EXISTS idx_krs_mahasiswa_sorting ON krs_mahasiswa (tahunakademik DESC, semesterpendek ASC, semester DESC)",
				"CREATE INDEX IF NOT EXISTS idx_skripsi_sorting ON skripsi (tahun_akademik DESC, semester DESC, mahasiswa)",
				"CREATE INDEX IF NOT EXISTS idx_reqta_sorting ON mahasiswa_request_tugas_akhir (tahun_akademik DESC, semester DESC, status)",
				// Kegiatan
				"CREATE INDEX IF NOT EXISTS idx_kegiatan_sorting ON formulir_kegiatan (tahunakademik DESC, semester DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_wisuda_sorting ON wisuda (wisuda_ke DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_kkn_sorting ON kelompok_kkn (kkn, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_pkl_sorting ON kelompok_pkl (pkl, id DESC)",
				// Pertemuan (Poly-relational Indexing)
				"CREATE INDEX IF NOT EXISTS idx_pertemuan_perkuliahan ON pertemuan (perkuliahan, aktif, tanggal, pertemuan_ke, id)",
				"CREATE INDEX IF NOT EXISTS idx_pertemuan_kkn ON pertemuan (kelompok_kkn, aktif, tanggal, pertemuan_ke, id)",
				"CREATE INDEX IF NOT EXISTS idx_pertemuan_pkl ON pertemuan (kelompok_pkl, aktif, tanggal, pertemuan_ke, id)",
				"CREATE INDEX IF NOT EXISTS idx_pertemuan_skripsi ON pertemuan (skripsi, aktif, tanggal, pertemuan_ke, id)",
				"CREATE INDEX IF NOT EXISTS idx_pertemuan_req_ta ON pertemuan (mahasiswa_request_tugas_akhir, aktif, tanggal, pertemuan_ke, id)",
				"CREATE INDEX IF NOT EXISTS idx_pertemuan_krs ON pertemuan (krs_mahasiswa, aktif, tanggal, pertemuan_ke, id)",
				"CREATE INDEX IF NOT EXISTS idx_pertemuan_formulir ON pertemuan (formulir_kegiatan, aktif, tanggal, pertemuan_ke, id)",
				"CREATE INDEX IF NOT EXISTS idx_pertemuan_jadwalpelajaran ON pertemuan (jadwal_pelajaran, aktif, tanggal, pertemuan_ke, id)",
				"CREATE INDEX IF NOT EXISTS idx_pertemuan_wisuda ON pertemuan (wisuda, aktif, tanggal, pertemuan_ke, id)",
				"CREATE INDEX IF NOT EXISTS idx_pertemuan_uji_pmb ON pertemuan (jadwal_ujian_pmb, aktif, tanggal, pertemuan_ke, id)",
				"CREATE INDEX IF NOT EXISTS idx_pertemuan_uji_psb ON pertemuan (jadwal_ujian_psb, aktif, tanggal, pertemuan_ke, id)",
				"CREATE INDEX IF NOT EXISTS idx_pertemuan_uji_pegawai ON pertemuan (jadwal_ujian_pegawai, aktif, tanggal, pertemuan_ke, id)",
				"CREATE INDEX IF NOT EXISTS idx_pertemuan_grup ON pertemuan (pertemuan_punya_grup_pertemuan, aktif, tanggal, pertemuan_ke, id)",
				"CREATE INDEX IF NOT EXISTS idx_pertemuan_kursus ON pertemuan (komponen_data_produk_kursus, aktif, tanggal, pertemuan_ke, id)",
				// Linimasa & Relasi Lain
				"CREATE INDEX IF NOT EXISTS idx_ppu_pertemuan_ujian ON pertemuan_punya_ujian (pertemuan, ujian, id)",
				"CREATE INDEX IF NOT EXISTS idx_pertemuan_mulai_coalesce ON pertemuan (DATE(COALESCE(mulai, tanggal)))",
				"CREATE INDEX IF NOT EXISTS idx_tugaspert_mulai_coalesce ON tugas_pertemuan (DATE(mulai))",
				// REDUNDAN → di-DROP: didominasi idx_dash_el_tugas_pertemuan_pertemuan_id (pertemuan, id).
				"DROP INDEX IF EXISTS idx_tugaspert_pertemuan_fk",
				"CREATE INDEX IF NOT EXISTS idx_pert_ta_smt_tgl ON pertemuan (ta, smt, aktif, tanggal)",
				"CREATE INDEX IF NOT EXISTS idx_pert_jurusan_tgl ON pertemuan (jurusan, aktif, tanggal)",
				"CREATE INDEX IF NOT EXISTS idx_pert_sekolah_tgl ON pertemuan (sekolah, aktif, tanggal)",
				// REDUNDAN (prefix dari idx_jurusan_dashboard_join (fakultas,aktif,id,nama)) → DROP.
				"DROP INDEX IF EXISTS idx_jurusan_fakultas",
				"CREATE INDEX IF NOT EXISTS idx_sekolah_yayasan ON sekolah.sekolah (yayasan_id)",
				"CREATE INDEX IF NOT EXISTS idx_pert_aktif_tgl ON pertemuan (aktif, tanggal)" };
		for (String sql : btreeIndexes) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:1506");
			}
		}

		// ===================================================================================
		// INDEX DATABASE OPTIMIZATION UNTUK QUERY TAGIHAN & PENGATURAN BIAYA (LENGKAP)
		// ===================================================================================
		String[] INDEX_QUERIES_TAGIHAN = new String[] {

				// 0. Extension Dasar untuk pencarian string (LIKE %...%) di PostgreSQL
				"CREATE EXTENSION IF NOT EXISTS pg_trgm",

				// 1. Indeks Jalur Relasi (JOIN) & Filter Pembayaran
				// REDUNDAN (prefix dari idx_pemsis_filter_tagihan (siswa_id,jenis_biaya_id,tahun,bulan)) → DROP.
				"DROP INDEX IF EXISTS sekolah.idx_pemsis_siswa_cover",
				"CREATE INDEX IF NOT EXISTS idx_pemsis_calon_cover ON sekolah.pembayaran_siswa (calon_siswa_id)",

				// Indeks FK dan sub-query SUM nilai bayar pada detail pembayaran
				"CREATE INDEX IF NOT EXISTS idx_pemsis_detail_fk ON sekolah.pembayaran_siswa_detail (pembayaran_siswa_id, id DESC)",

				"CREATE INDEX IF NOT EXISTS idx_psd_pembsiswa_tagihan ON sekolah.pembayaran_siswa_detail (pembayaran_siswa_id, tagihan)",

				"CREATE INDEX IF NOT EXISTS idx_psd_pembsiswa_tagihan_sum ON sekolah.pembayaran_siswa_detail (pembayaran_siswa_id, tagihan) INCLUDE (nominal)",

				// 2. Indeks pada Tabel Tagihan (Join, Filter, & Sorting)
				"CREATE INDEX IF NOT EXISTS idx_tagihan_siswa_fk ON sekolah.tagihan (siswa_id)",
				"CREATE INDEX IF NOT EXISTS idx_tagihan_calon_siswa_fk ON sekolah.tagihan (calon_siswa_id)",
				"CREATE INDEX IF NOT EXISTS idx_tagihan_item_biaya_fk ON sekolah.tagihan (item_biaya_id)",
				// REDUNDAN (prefix dari idx_tagihan_sorting_complex (pengaturan_biaya,tahunbulan,bayarke)) → DROP.
				"DROP INDEX IF EXISTS sekolah.idx_tagihan_pengaturan_biaya_fk",
				"CREATE INDEX IF NOT EXISTS idx_tagihan_detail_fk ON sekolah.tagihan (pembayaran_siswa_detail_id, aktif)",
				"CREATE INDEX IF NOT EXISTS idx_tagihan_sorting_complex ON sekolah.tagihan (pengaturan_biaya DESC, tahunbulan ASC, bayarke ASC)",

				// 3. Indeks Filter & Pengurutan Item Biaya Sekolah
				"CREATE INDEX IF NOT EXISTS idx_itembiaya_aktif_nama ON sekolah.item_biaya_sekolah (aktif, nama ASC)",

				// 4. Indeks Pengaturan Biaya & Jenis Biaya Sekolah
				"CREATE INDEX IF NOT EXISTS idx_pb_jbs_sort ON sekolah.pengaturan_biaya (jenis_biaya_sekolah_id, id DESC, aktif)",

				// OPTIMASI: Indeks GIN untuk pencarian LIKE '%...%' pada kolom tahunajaran
				"CREATE INDEX IF NOT EXISTS idx_pb_tahunajaran_trgm ON sekolah.pengaturan_biaya USING GIN (tahunajaran gin_trgm_ops)",

				"CREATE INDEX IF NOT EXISTS idx_jbs_periode_nama ON sekolah.jenis_biaya_sekolah (periode DESC, nama ASC)",

				// 5. Indeks Master Siswa
				"CREATE INDEX IF NOT EXISTS idx_siswa_aktif_sekolah_id ON sekolah.siswa (aktif, sekolah_id)" };

		// Eksekusi Indeks
		for (String sql : INDEX_QUERIES_TAGIHAN) {
			try {
				// Menggunakan metode updateSql10Menit agar eksekusi query panjang tidak timeout
				eksekusiSql10Menit(sql);
			} catch (Exception e) {
				// Tetap log error jika terjadi kegagalan (misal: keterbatasan hak akses DB)
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitIndex.java:1560");
			}
		}

		// 4. INDEKS MODUL PEMBAYARAN, DASHBOARD, DAN LAIN-LAIN
		String[] otherIndexes = {
				// --- 1. MODUL PEMBAYARAN & KEGIATAN ---
				// REDUNDAN (prefix dari idx_pembayaran_siswa_tabungan_siswa_tgl, WHERE sama) → DROP.
				"DROP INDEX IF EXISTS sekolah.idx_pemsis_siswa_tabungan_partial",
				// REDUNDAN (prefix dari idx_pembayaran_siswa_tabungan_calon_tgl, WHERE sama) → DROP.
				"DROP INDEX IF EXISTS sekolah.idx_pemsis_calon_tabungan_partial",
				// REDUNDAN (prefix dari idx_cicilan_kegiatan_id_cover (kegiatan,id)) → DROP.
				"DROP INDEX IF EXISTS idx_cp_kegiatan",
				"CREATE INDEX IF NOT EXISTS idx_keg_ta_jk ON kegiatan (tahun_akademik, jenis_kegiatan)",
				"CREATE INDEX IF NOT EXISTS idx_cicilan_kegiatan_id_cover ON cicilan_pembayaran (kegiatan, id)",
				// REDUNDAN (prefix dari idx_keg_mhs_reinit_cover (mahasiswa,semster,jenis_kegiatan,id,aktif)) → DROP.
				"DROP INDEX IF EXISTS idx_keg_mahasiswa",
				// REDUNDAN (prefix dari idx_keg_calon_reinit_cover (calon_mahasiswa,semster,jenis_kegiatan,id,aktif)) → DROP.
				"DROP INDEX IF EXISTS idx_keg_calon_mhs",
				"CREATE INDEX IF NOT EXISTS idx_cicilan_kegiatan_agregat ON cicilan_pembayaran (kegiatan, nilai, denda) WHERE item_biaya IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_cicilan_ref ON cicilan_pembayaran (ref)",
				"CREATE INDEX IF NOT EXISTS idx_dk_kegiatan_id_cover ON detail_kegiatan (kegiatan, id)",
				"CREATE INDEX IF NOT EXISTS idx_dk_kegiatan_item ON detail_kegiatan (kegiatan, item_biaya)",
				"CREATE INDEX IF NOT EXISTS idx_keg_mhs_reinit_cover ON kegiatan (mahasiswa, semster, jenis_kegiatan, id, aktif)",
				"CREATE INDEX IF NOT EXISTS idx_keg_calon_reinit_cover ON kegiatan (calon_mahasiswa, semster, jenis_kegiatan, id, aktif)",
				"CREATE INDEX IF NOT EXISTS idx_tagihan_pb_kodeunik_notnull ON sekolah.tagihan (pengaturan_biaya) WHERE kode_unik IS NOT NULL",

				// --- 2. MODUL DASHBOARD STATISTIK ---
				"CREATE INDEX IF NOT EXISTS idx_mhs_dashboard_stats ON mahasiswa (jurusan, aktif, tahunangkatan DESC)",
				// "CREATE INDEX IF NOT EXISTS idx_mhs_lulus_dashboard ON mahasiswa
				// (status_keluar, jurusan, tahun_akademik DESC, ganjil_genap)",
				"CREATE INDEX IF NOT EXISTS idx_dosen_dashboard_join ON dosen (jurusan, aktif)",
				"CREATE INDEX IF NOT EXISTS idx_bcm_dashboard_stats ON biodata_calon_mahasiswa (prodi_lulus, aktif, tahun DESC)",
				"CREATE INDEX IF NOT EXISTS idx_cuti_dashboard_stats ON pendaftaran_cuti_mahasiswa (persetujuan, mahasiswa, tahun_akademik DESC, ganjil_genap ASC)",
				"CREATE INDEX IF NOT EXISTS idx_mhs_dashboard_cuti ON mahasiswa (id, aktif, jurusan)",
				"CREATE INDEX IF NOT EXISTS idx_jurusan_dashboard_join ON jurusan (fakultas, aktif, id, nama)",
				"CREATE INDEX IF NOT EXISTS idx_fakultas_dashboard_join ON fakultas (perguruan_tinggi, id)",
				"CREATE INDEX IF NOT EXISTS idx_mhs_dashboard_lulus_thn ON mahasiswa (status_keluar, jurusan, aktif, tahunlulus DESC)",

				// --- 3. MODUL BROADCAST & EMAIL ---
				"CREATE INDEX IF NOT EXISTS idx_tbmuser_broadcast_filter ON tbmuser (aktif, dosen, guru, pegawai, email, userid)",
				"CREATE INDEX IF NOT EXISTS idx_mhs_broadcast_filter ON mahasiswa (aktif, status_keluar, jurusan, program, email, nim)",
				"CREATE INDEX IF NOT EXISTS idx_calon_mhs_broadcast_filter ON biodata_calon_mahasiswa (aktif, tahunakademik, prodi_lulus, program, email)",
				"CREATE INDEX IF NOT EXISTS idx_siswa_broadcast_filter ON sekolah.siswa (sekolah_id, yayasan_id, nama_siswa, alamat_email, nomor_induk_nasional)",
				"CREATE INDEX IF NOT EXISTS idx_cuti_mhs_broadcast_filter ON pendaftaran_cuti_mahasiswa (tahun_akademik, persetujuan, mahasiswa, semester)",

				// --- 4. MODUL PENGUMUMAN & DISKUSI ---
				"CREATE INDEX IF NOT EXISTS idx_pengumuman_akademis_filter ON pengumuman_akademis (aktif, diperuntukkan, tanggal, sampai, sekolah, yayasan, kategori_pengumuman)",
				"CREATE INDEX IF NOT EXISTS idx_diskusi_pengumuman_ref ON diskusi_pengumuman_akademis (pengumuman_akademis)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_pengumuman_hanyauntuk_user ON pengumuman_akademis USING gin (hanyaUntukUsername gin_trgm_ops)",

				// --- 5. MODUL ASET & VENDOR ---
				// "CREATE INDEX IF NOT EXISTS idx_perbaikan_asset_vendor ON
				// asset.perbaikan_asset (asset_detail, status_perbaikan, selesai)",
				"CREATE INDEX IF NOT EXISTS idx_penyedia_asset_kode ON asset.penyedia_asset (kode, aktif)",

				// --- 6. MODUL PARAMETER TAMBAHAN ---
				// "CREATE INDEX IF NOT EXISTS idx_param_tambahan_catatan_admin ON
				// parameter_tambahan_catatan_siswa
				// (kelompok_parameter_tambahan_catatan_administrasi)",
				// "CREATE INDEX IF NOT EXISTS idx_param_tambahan_perbaikan_asset ON
				// parameter_tambahan_catatan_siswa
				// (kelompok_parameter_tambahan_perbaikan_asset)",

				// --- 7. MODUL PERSURATAN & DISPOSISI (Kecuali tabel foto/file) ---
				"CREATE INDEX IF NOT EXISTS idx_alur_surat_keluar_status ON surat.alur_persetujuan_surat_keluar_status (surat_keluar, kodeunik, pejabat)",
				"CREATE INDEX IF NOT EXISTS idx_alur_surat_masuk_status ON surat.alur_persetujuan_surat_masuk_status (surat_masuk, kodeunik, pejabat)" };

		for (String sql : otherIndexes) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitIndex.java:1632");
			}
		}

		// ===================================================================================
		// INDEKS GRID "Catatan Pembayaran Mahasiswa" (KegiatanAction.initCriteria) — keluhan lambat
		// ===================================================================================
		// Kolom yang benar-benar dipakai KegiatanAction.initCriteria() sebagai leading filter, TAPI
		// sebelumnya TIDAK PERNAH jadi kolom PERTAMA di indeks manapun pada tabel kegiatan/mahasiswa
		// (semua indeks kegiatan yang ada mensyaratkan kolom join mahasiswa/calon_mahasiswa/tahun_akademik
		// LEBIH DULU, yang tidak ada di query filter status/tanggal/program/dosen ini) — makanya lambat:
		//   - "Hanya yang aktif" (default TERCENTANG di setiap pencarian) -> aktif IS NULL OR aktif=true
		//   - checkbox Lunas/Blm-lunas -> lunas = ?
		//   - checkbox Membayar       -> amount >= 0.1
		//   - checkbox Kelebihan      -> amount_terhutang < -0.1
		//   - checkbox Blm byr        -> amount <= 0.099 AND amount_terhutang >= 0.099
		//   - combo Program           -> program = ?
		//   - rentang tanggal (SELALU aktif, default 7-tahun-lalu s.d. besok, dibungkus date(...))
		//   - filter PA/Dosen         -> mahasiswa.dosen = ? (mahasiswa SAMA SEKALI belum ada indeks utk dosen)
		// Semua indeks di bawah memakai WHERE (aktif=true OR aktif IS NULL) yang identik dengan predikat
		// "Hanya yang aktif" di query -> Postgres bisa memakainya (bukan cuma prefix, predikat sama persis),
		// sekaligus indeks jadi lebih kecil (baris aktif=false dikeluarkan). Kolom "id DESC" di akhir agar
		// ORDER BY this_.id DESC (satu-satunya urutan grid ini) tidak perlu sort terpisah.
		String[] INDEX_QUERIES_GRID_KEGIATAN = new String[] {
				"CREATE INDEX IF NOT EXISTS idx_keg_grid_aktif_lunas_id ON kegiatan (lunas, id DESC) "
						+ "WHERE (aktif = true OR aktif IS NULL)",
				"CREATE INDEX IF NOT EXISTS idx_keg_grid_aktif_amount_id ON kegiatan (amount, id DESC) "
						+ "WHERE (aktif = true OR aktif IS NULL)",
				"CREATE INDEX IF NOT EXISTS idx_keg_grid_aktif_terhutang_id ON kegiatan (amount_terhutang, id DESC) "
						+ "WHERE (aktif = true OR aktif IS NULL)",
				"CREATE INDEX IF NOT EXISTS idx_keg_grid_aktif_program_id ON kegiatan (program, id DESC) "
						+ "WHERE (aktif = true OR aktif IS NULL)",
				// Indeks EKSPRESI: predikat query membungkus kolom dengan date(this_.tanggal), yang
				// TIDAK bisa dilayani indeks biasa pada kolom tanggal mentah — wajib indeks pada
				// ekspresi date(tanggal) itu sendiri agar predikat rentang tanggal bisa pakai indeks.
				"CREATE INDEX IF NOT EXISTS idx_keg_tanggal_date_expr ON kegiatan (date(tanggal))",
				// Filter PA/Dosen (mahasiswa.dosen = ?) belum punya indeks sama sekali di tabel mahasiswa.
				"CREATE INDEX IF NOT EXISTS idx_mhs_dosen ON mahasiswa (dosen)",

				// --- Grid "Pembayaran Angsuran" (CicilanPembayaranAction.initCriteria) — pola sama ---
				// Query dasarnya SELECT dari cicilan_pembayaran (bukan kegiatan), dengan predikat yang
				// SELALU aktif: nilai > 0.01 OR nilai < -0.01 (leading, belum ada indeksnya), dan rentang
				// tanggal yang JUGA dibungkus date(this_.tanggal) (kolom cicilan_pembayaran.tanggal, beda
				// dari kegiatan.tanggal di atas). Indeks tanggal/deposit yang sudah ada semua ber-WHERE
				// deposit>0.1, tidak match saat checkbox deposit tidak dicentang (kasus paling umum).
				"CREATE INDEX IF NOT EXISTS idx_cicilan_nilai_id ON cicilan_pembayaran (nilai, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_cicilan_tanggal_date_expr ON cicilan_pembayaran (date(tanggal))" };

		for (String sql : INDEX_QUERIES_GRID_KEGIATAN) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e,
						"auto-audit src/ais/common/InitIndex.java:INDEX_QUERIES_GRID_KEGIATAN");
			}
		}

		String[] INDEX_QUERIES_BUAT_TAGIHAN = new String[] {
				// 1. Mempercepat filter utama pada PembayaranSiswaDetail (Filter: nominal,
				// item_biaya, dan Join id DESC)
				// Menyaring detail secara instan tanpa perlu membaca disk.
				"CREATE INDEX IF NOT EXISTS idx_pemsisdetail_tagihan_lookup ON sekolah.pembayaran_siswa_detail (nominal_biaya_id, item_biaya_id, pembayaran_siswa_id, tagihan, id DESC)",

				// 2. Mempercepat filter pada tabel relasi PembayaranSiswa (Filter: siswa,
				// jenis, tahun, bulan)
				// Hibernate akan menggunakan indeks ini saat menjalankan alias klausa
				// 'ps.siswa' dsb.
				"CREATE INDEX IF NOT EXISTS idx_pemsis_filter_tagihan ON sekolah.pembayaran_siswa (siswa_id, jenis_biaya_id, tahun, bulan)",

				// 3. Mempercepat Join ke tabel Tagihan (Mencari Tagihan berdasarkan bayarKe dan
				// relasi ID)
				"CREATE INDEX IF NOT EXISTS idx_tagihan_bayarke_lookup ON sekolah.tagihan (id, bayarke)",

				// 4. Mempercepat Lookup Cache DB berdasarkan Kode Unik Tagihan
				// Berguna untuk baris: Tagihan.findByKodeUnik(kodeUnik, session);
				"CREATE INDEX IF NOT EXISTS idx_tagihan_kodeunik ON sekolah.tagihan (kode_unik)",

				"CREATE INDEX IF NOT EXISTS idx_tk_pertemuan ON tugas_kelompok (pertemuan)",
				"CREATE INDEX IF NOT EXISTS idx_loglogin_nama_login_id_desc ON public.log_login (nama, login DESC, id DESC)" };

		for (String sql : INDEX_QUERIES_BUAT_TAGIHAN) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitIndex.java:1663");
			}
		}

		// --- 8. INDEKS KHUSUS TABEL FOTO/FILE (MENGGUNAKAN STREAMING SESSION) ---
		String[] streamingIndexes = {
				"CREATE INDEX IF NOT EXISTS idx_foto_surat_keluar_ref ON foto_gambar_surat_keluar (surat_keluar, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_foto_surat_masuk_ref ON foto_gambar_surat_masuk (surat_masuk, id DESC)",
				// REDUNDAN → di-DROP: didominasi idx_dash_el_{pfc,video,audio}_pertemuan_id (pertemuan, id).
				"DROP INDEX IF EXISTS idx_pfc_pertemuan",
				"DROP INDEX IF EXISTS idx_vp_pertemuan",
				"DROP INDEX IF EXISTS idx_ap_pertemuan",
				"CREATE INDEX IF NOT EXISTS idx_lampiran_lain_jenis_ref_id ON lampiran_lain (jenis, ref, id)" };

		for (String sql : streamingIndexes) {
			try {
				eksekusiSqlStreaming(sql);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitIndex.java:1681");
			}
		}

		// ===================================================================================
		// INDEX DATABASE OPTIMIZATION UNTUK MODUL HELPER DETAIL BIAYA PEMBAYARAN
		// ===================================================================================
		String[] INDEX_QUERIES_PEMBAYARAN_UTIL = new String[] {
				// 1. Index untuk tabel Detail Biaya yang menopang filter kompleks MHS
				// (Mencakup pencarian berdasarkan angkatan, jurusan, semester, jenis_kegiatan,
				// dan status aktif)
				"CREATE INDEX IF NOT EXISTS idx_detail_biaya_kompleks_mhs ON detail_biaya (angkatan, jurusan, semester, jenis_kegiatan, jenjang)",

				// 2. Index untuk tabel Detail Biaya yang menopang filter kompleks CALON MHS
				// (Mencakup pencarian berdasarkan paket dan gelombang_pendaftaran)
				"CREATE INDEX IF NOT EXISTS idx_detail_biaya_kompleks_calon ON detail_biaya (angkatan, jurusan, semester, jenis_kegiatan, paket, gelombang_pendaftaran)",

				// 3. Index untuk tabel PengaturanPembayaranBulanan
				// Mencegah full table scan saat query mencari cicilan/pembayaran di bulan
				// tertentu
				"CREATE INDEX IF NOT EXISTS idx_pb_bulanan_detail_realbulan ON pengaturan_pembayaran_bulanan (detail_biaya, realbulan, aktif)",
				"CREATE INDEX IF NOT EXISTS idx_pb_bulanan_bulan ON pengaturan_pembayaran_bulanan (bulan)",

				// 4. Index untuk tabel Cicilan Pembayaran
				// Mempercepat filter saat query join antara cicilan, kegiatan, dan bulanan
				"CREATE INDEX IF NOT EXISTS idx_cicilan_kegiatan_pb_bulanan ON cicilan_pembayaran (kegiatan, pengaturan_pembayaran_bulanan)",

				// 5. Index untuk tabel Detail Kegiatan Lookup
				// Mempercepat getDetailKegiatanMahasiswa saat memfilter berdasarkan mahasiswa /
				// calon mahasiswa
				// REDUNDAN (prefix dari idx_dk_kegiatan_id_cover (kegiatan,id)) → DROP.
				"DROP INDEX IF EXISTS idx_detail_kegiatan_lookup" };

		for (String sql : INDEX_QUERIES_PEMBAYARAN_UTIL) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitIndex.java:1718");
			}
		}

		// Eksekusi ini bersama dengan index pembayaran lainnya
		String sqlCountBulanan = "CREATE INDEX IF NOT EXISTS idx_pb_bulanan_detail_aktif_nominal ON pengaturan_pembayaran_bulanan (detail_biaya, aktif, nominal)";

		try {
			eksekusiSql(sqlCountBulanan);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitIndex.java:1728");
		}

		String[] OPTIMIZATION_INDEXES = new String[] {
				"CREATE INDEX IF NOT EXISTS idx_setting_biaya_core ON setting_biaya (ta, jenis_kegiatan, khususbuatmahasiswatertentu, gunakanbiayadefault)",
				"CREATE INDEX IF NOT EXISTS idx_item_biaya_semester ON item_biaya (aktif, tidakditagihdismtgenap, tidakditagihdismtganjil, minsmt, maxsmt)",
				"CREATE INDEX IF NOT EXISTS idx_detail_setting_biaya_sb ON detail_setting_biaya (setting_biaya, item_biaya, bayarke)",
				"CREATE INDEX IF NOT EXISTS idx_detail_biaya_composite1 ON detail_biaya (semester, detail_setting_biaya, setting_biaya, jurusan, program)",
				"CREATE INDEX IF NOT EXISTS idx_detail_biaya_composite2 ON detail_biaya (semester, bayarke, item_biaya, jurusan, program, angkatan, jenjang)",
				"CREATE INDEX IF NOT EXISTS idx_setting_biaya_detail_lookup ON setting_biaya_detail (biodata_calon_mahasiswa, mahasiswa, setting_biaya)",

				"CREATE INDEX IF NOT EXISTS idx_cuti_izin_setujui_peg_mulai ON payroll.cuti_dan_izin (setujui, pegawai, mulai)",
				"CREATE INDEX IF NOT EXISTS idx_pengajuan_pegawai_setujui_peg_waktu ON public.pengajuan_pegawai (setujui, pegawai, waktu)",
				"CREATE INDEX IF NOT EXISTS idx_jenis_pengajuan_aktif ON public.jenis_pengajuan_pegawai (aktif)",

				"CREATE INDEX IF NOT EXISTS idx_kehadiran_peg_tgl ON public.status_kehadiran_karyawan_harian (pegawai, tanggal)",
				"CREATE INDEX IF NOT EXISTS idx_kehadiran_guru_tgl ON public.status_kehadiran_karyawan_harian (guru, tanggal)",
				"CREATE INDEX IF NOT EXISTS idx_kehadiran_dosen_tgl ON public.status_kehadiran_karyawan_harian (dosen, tanggal)",
				"CREATE INDEX IF NOT EXISTS idx_kehadiran_tanggal_only ON public.status_kehadiran_karyawan_harian (tanggal)",
				"CREATE INDEX IF NOT EXISTS idx_status_kehadiran_absensi ON status_kehadiran_karyawan_harian (statusabsensi)",
				"CREATE INDEX IF NOT EXISTS idx_cuti_bersama_tahun ON public.cuti_bersama (tahun)",

				"CREATE INDEX IF NOT EXISTS idx_pegawai_status_aktif_sort ON public.pegawai (status_pegawai, aktif, satuan_kerja, dosen, guru, nama)",
				"CREATE INDEX IF NOT EXISTS idx_pegawai_lookup_guru ON public.pegawai (guru)",
				"CREATE INDEX IF NOT EXISTS idx_pegawai_lookup_dosen ON public.pegawai (dosen)",
				"CREATE INDEX IF NOT EXISTS idx_pegawai_tipe ON public.pegawai (tipe_pegawai)",
				"CREATE UNIQUE INDEX IF NOT EXISTS idx_kegiatan_kodeunik ON kegiatan (kodeunik)",

				"CREATE INDEX IF NOT EXISTS idx_absen_piket_rapor ON sekolah.absen_piket (sekolah_id, tahun_ajaran, semester, tanggal)",
				"CREATE INDEX IF NOT EXISTS idx_kelompok_matapelajaran_rapor ON sekolah.kelompok_matapelajaran (sekolah_id, induk, aktif, nomorurut)",
				"CREATE INDEX IF NOT EXISTS idx_catatan_kelas_siswa_rapor ON sekolah.catatan_kelas_siswa (kelas_siswa, nama)",
				"CREATE INDEX IF NOT EXISTS idx_catatan_siswa_rapor ON sekolah.catatan_siswa (tahun_ajaran, semester, siswa, nama)",
				"CREATE INDEX IF NOT EXISTS idx_formulir_kegiatan_peserta_rapor ON public.formulir_kegiatan_peserta (acc, formulir_kegiatan, siswa)",
				"CREATE INDEX IF NOT EXISTS idx_prestasi_siswa_rapor ON sekolah.prestasi_siswa (status, tahunakademik, jenissemester, siswa, tanggal)",
				"CREATE INDEX IF NOT EXISTS idx_kegiatan_siswa_rapor ON sekolah.kegiatan_siswa (ta, siswa_id, waktu)",
				"CREATE INDEX IF NOT EXISTS idx_pelanggaran_siswa_rapor ON sekolah.pelanggaran_siswa (ta, siswa_id, waktu)",
				"CREATE INDEX IF NOT EXISTS idx_apresiasi_siswa_rapor ON sekolah.apresiasi_siswa (ta, siswa_id, waktu)",

				"CREATE INDEX IF NOT EXISTS idx_deposit_mahasiswa_id ON deposit (mahasiswa)",
				"CREATE INDEX IF NOT EXISTS idx_deposit_siswa_id ON deposit (siswa)",
				"CREATE INDEX IF NOT EXISTS idx_deposit_biodata_id ON deposit (biodata_calon_mahasiswa)",
				"CREATE INDEX IF NOT EXISTS idx_deposit_waktu ON deposit (waktu ASC)",
				"CREATE INDEX IF NOT EXISTS idx_detailperkuliahan_mhs_semester_id ON detailperkuliahan (mahasiswa, semester, id)",
				// REDUNDAN (prefix dari idx_detailperkuliahan_mhs_semester_id (mahasiswa,semester,id)) → DROP.
				"DROP INDEX IF EXISTS idx_dp_mahasiswa",
				"CREATE INDEX IF NOT EXISTS idx_online_users_mahasiswa ON online_users (mahasiswa)",
				"CREATE INDEX IF NOT EXISTS idx_log_login_mahasiswa ON log_login (mahasiswa)",
				// Dashboard "Statistik Kunjungan Pengguna" (StatistikKunjunganDashboardUtil
				// .loadDailyRows/loadLabelRows) memfilter success_status=true DAN login dalam rentang
				// tanggal (predikat sargable: login >= mulai AND login < sampai+1hari). Composite
				// (success_status, login) mencegah seq-scan tabel log_login -> hindari statement timeout.
				"CREATE INDEX IF NOT EXISTS idx_log_login_success_login ON public.log_login (success_status, login)",
				// REDUNDAN (prefix dari idx_cuti_izin_dash_hrd_peg_setujui_mulai_sampai (pegawai,setujui,mulai,sampai)) → DROP.
				"DROP INDEX IF EXISTS payroll.idx_cuti_izin_pegawai_setujui_mulai",
				// REDUNDAN (prefix dari idx_cuti_izin_dash_hrd_peg_setujui_sampai_mulai (pegawai,setujui,sampai,mulai)) → DROP.
				"DROP INDEX IF EXISTS payroll.idx_cuti_izin_pegawai_setujui_sampai",
				"CREATE INDEX IF NOT EXISTS idx_konfigurasi_nama_id ON konfigurasi (nama, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_konfigurasi_nama_tahun ON konfigurasi (nama, tahun_akademik)",
				// Buang index btree LAMA yang keliru (dibuat manual di luar app): mengindeks kolom
				// TEKS BESAR "nilai". INSERT/UPDATE konfigurasi dengan nilai > 2704 byte GAGAL
				// (SQLState 54000: "index row size exceeds btree version 4 maximum 2704"). Kolom
				// "nilai" tidak perlu diindeks; lookup by "nama" sudah dilayani idx_konfigurasi_nama_id.
				// DROP dijalankan self-heal saat startup agar INSERT/UPDATE konfigurasi kembali sukses.
				"DROP INDEX IF EXISTS idx_dash_el_konfigurasi_nama_nilai",
				"CREATE INDEX IF NOT EXISTS idx_kalender_akademik_tanggal ON kalender_akademik (tanggal_mulai DESC, tanggal_selesai)",
				"CREATE INDEX IF NOT EXISTS idx_kalender_akademik_fakultas ON kalender_akademik (fakultas)",
				"CREATE INDEX IF NOT EXISTS idx_kalender_akademik_jurusan ON kalender_akademik (jurusan)",
				"CREATE INDEX IF NOT EXISTS idx_kalender_akademik_sekolah ON kalender_akademik (sekolah)",
				"CREATE INDEX IF NOT EXISTS idx_kalender_akademik_yayasan ON kalender_akademik (yayasan)",
				"CREATE INDEX IF NOT EXISTS idx_kka_konfigurasi ON konfigurasi_kalender_akademik (konfigurasi)",
				"CREATE INDEX IF NOT EXISTS idx_kka_kalender_akademik ON konfigurasi_kalender_akademik (kalender_akademik)",
				"CREATE INDEX IF NOT EXISTS idx_hsm_mhs_smt_tahap ON history_status_mahasiswa (mahasiswa, semester, tahap, sp)",
				// InformasiPembayaranMahasiswaAction.tampilkanHistoryStatusMahasiswa:
				// WHERE mahasiswa = ? ORDER BY semester, tahunakademik, sp.
				// idx_hsm_mhs_smt_tahap tidak redundant karena kolom tahap berada sebelum sp
				// dan tidak memuat tahunakademik, sehingga tidak bisa melayani ORDER BY ini penuh.
				"CREATE INDEX IF NOT EXISTS idx_hsm_mhs_smt_ta_sp ON history_status_mahasiswa (mahasiswa, semester, tahunakademik, sp)",
				"CREATE INDEX IF NOT EXISTS idx_hsm_mhs_status ON history_status_mahasiswa (mahasiswa, status_mahasiswa, semester)",
				// REDUNDAN (prefix dari idx_hsm_mhs_smt_tahap (mahasiswa,semester,tahap,sp)) → DROP.
				"DROP INDEX IF EXISTS idx_hsm_thn_akademik",
				"CREATE INDEX IF NOT EXISTS idx_status_mahasiswa_nama ON status_mahasiswa (nama)",
				"CREATE INDEX IF NOT EXISTS idx_aps_keluar_parent_id ON surat.alur_persetujuan_surat_keluar_status (surat_keluar, id)",
				"CREATE INDEX IF NOT EXISTS idx_aps_masuk_parent_id ON surat.alur_persetujuan_surat_masuk_status (surat_masuk, id)" };

		for (String sql : OPTIMIZATION_INDEXES) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitIndex.java:1811");
			}
		}

		// Eksekusi ini bersama dengan index dashboard realisasi anggaran lainnya
		String[] sqlIndexesDashboardRealisasi = new String[] {

				// Index yang sebelumnya sudah ada
				"CREATE INDEX IF NOT EXISTS idx_pa_workspace_aktif_waktu_inc_nilai "
						+ "ON rab.penggunaan_anggaran (workspace, aktif, waktu) INCLUDE (nilai)",

				// Index utama rab.workspace untuk filter tahun, satuan kerja, aktif, leaf,
				// harga_total, dan order by id
				"CREATE INDEX IF NOT EXISTS idx_workspace_dashboard_realisasi_main "
						+ "ON rab.workspace (tahun_workspace, satuan_kerja, id) " + "INCLUDE (kode, nama, harga_total) "
						+ "WHERE (aktif = true OR aktif IS NULL) " + "AND harga_total > 0 " + "AND leaf = true",

				// Index rab.workspace saat filter satuan kerja kosong
				"CREATE INDEX IF NOT EXISTS idx_workspace_dashboard_realisasi_tahun_id "
						+ "ON rab.workspace (tahun_workspace, id) " + "INCLUDE (kode, nama, harga_total, satuan_kerja) "
						+ "WHERE (aktif = true OR aktif IS NULL) " + "AND harga_total > 0 " + "AND leaf = true",

				// Index pencarian ILIKE kode workspace
				"CREATE INDEX IF NOT EXISTS idx_workspace_dashboard_realisasi_kode_trgm " + "ON rab.workspace "
						+ "USING gin (kode gin_trgm_ops) " + "WHERE (aktif = true OR aktif IS NULL) "
						+ "AND harga_total > 0 " + "AND leaf = true",

				// Index pencarian ILIKE nama workspace
				"CREATE INDEX IF NOT EXISTS idx_workspace_dashboard_realisasi_nama_trgm " + "ON rab.workspace "
						+ "USING gin (nama gin_trgm_ops) " + "WHERE (aktif = true OR aktif IS NULL) "
						+ "AND harga_total > 0 " + "AND leaf = true",

				// Index utama penggunaan_anggaran berdasarkan workspaceIds
				// Dipakai untuk query:
				// SELECT id, aktif, workspace FROM rab.penggunaan_anggaran WHERE workspace IN
				// (:workspaceIds)
				"CREATE INDEX IF NOT EXISTS idx_penggunaan_anggaran_workspace_id "
						+ "ON rab.penggunaan_anggaran (workspace, id) " + "INCLUDE (aktif)",

				// Index tambahan untuk sorting/filter waktu realisasi per workspace
				"CREATE INDEX IF NOT EXISTS idx_penggunaan_anggaran_workspace_waktu_id "
						+ "ON rab.penggunaan_anggaran (workspace, waktu, id) " + "INCLUDE (aktif, nilai)",

				// Index pencarian ILIKE kode penggunaan anggaran
				"CREATE INDEX IF NOT EXISTS idx_penggunaan_anggaran_kode_trgm " + "ON rab.penggunaan_anggaran "
						+ "USING gin (kode gin_trgm_ops)",

				// Index pencarian ILIKE nama penggunaan anggaran
				"CREATE INDEX IF NOT EXISTS idx_penggunaan_anggaran_nama_trgm " + "ON rab.penggunaan_anggaran "
						+ "USING gin (nama gin_trgm_ops)",

				// Index pencarian ILIKE keterangan penggunaan anggaran
				"CREATE INDEX IF NOT EXISTS idx_penggunaan_anggaran_keterangan_trgm " + "ON rab.penggunaan_anggaran "
						+ "USING gin (keterangan gin_trgm_ops)",

				// Update statistik table agar query planner memakai index terbaru
				// "ANALYZE rab.workspace",

				// "ANALYZE rab.penggunaan_anggaran"
		};

		for (String sqlIndex : sqlIndexesDashboardRealisasi) {
			try {
				eksekusiSql(sqlIndex);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitIndex.java:1876");
			}
		}

		// ===================================================================================
		// INDEX DATABASE OPTIMIZATION KHUSUS DASBOR PERGURUAN TINGGI TERPADU
		// -----------------------------------------------------------------------------------
		// Disusun dari pola query DasborPerguruanTinggiTerpadu + dashboard turunan:
		// Mahasiswa/KRS/Nilai/IPK/SKS, PMB/SPMB, Kurikulum, Kemahasiswaan,
		// Dosen, Perkuliahan, Kehadiran, Penelitian, Publikasi, dan Buku Ajar.
		// Catatan double-check:
		// - idx_pertemuan_perkuliahan single-column TIDAK dibuat ulang karena sudah
		// tercakup
		// oleh idx_pertemuan_perkuliahan (perkuliahan, aktif, tanggal, pertemuan_ke,
		// id).
		// - idx_ppu_pertemuan dan idx_tp_pertemuan TIDAK dibuat ulang karena sudah
		// tercakup
		// oleh idx_ppu_pertemuan_ujian dan idx_tugaspert_pertemuan_fk.
		// - idx_pertemuan_aktif_tgl TIDAK dibuat ulang karena sudah ada
		// idx_pert_aktif_tgl.
		// ===================================================================================
		String[] INDEX_QUERIES_DASBOR_PERGURUAN_TINGGI_TERPADU = new String[] {
				// --- Master Fakultas/Prodi/Kurikulum ---
				"CREATE INDEX IF NOT EXISTS idx_fakultas_dash_pt_aktif_id ON fakultas (aktif, id)",
				"CREATE INDEX IF NOT EXISTS idx_jurusan_dash_pt_fak_aktif_nama ON jurusan (fakultas, aktif, nama, id)",
				"CREATE INDEX IF NOT EXISTS idx_kurikulum_dash_pt_tahun_jurusan ON kurikulum (tahun, jurusan) WHERE (aktif = true OR aktif IS NULL)",

				// --- Mahasiswa aktif per angkatan/prodi/program ---
				"CREATE INDEX IF NOT EXISTS idx_mhs_dash_pt_tahun_program_jur ON mahasiswa (tahunangkatan, program, jurusan) WHERE (aktif = true OR aktif IS NULL)",
				"CREATE INDEX IF NOT EXISTS idx_mhs_dash_pt_jur_tahun_program ON mahasiswa (jurusan, tahunangkatan, program) WHERE (aktif = true OR aktif IS NULL)",
				"CREATE INDEX IF NOT EXISTS idx_mhs_dash_pt_program_tahun ON mahasiswa (program, tahunangkatan) WHERE (aktif = true OR aktif IS NULL)",

				// --- PMB/SPMB: tahun, program, pilihan prodi, prodi lulus, konversi NIM ---
				"CREATE INDEX IF NOT EXISTS idx_bcm_dash_pt_tahun_program ON biodata_calon_mahasiswa (tahun, program) WHERE (aktif = true OR aktif IS NULL)",
				"CREATE INDEX IF NOT EXISTS idx_bcm_dash_pt_prodi_lulus_tahun ON biodata_calon_mahasiswa (prodi_lulus, tahun, program) WHERE (aktif = true OR aktif IS NULL)",
				"CREATE INDEX IF NOT EXISTS idx_bcm_dash_pt_prodi1_tahun ON biodata_calon_mahasiswa (prodi_1, tahun, program) WHERE (aktif = true OR aktif IS NULL)",
				"CREATE INDEX IF NOT EXISTS idx_bcm_dash_pt_prodi2_tahun ON biodata_calon_mahasiswa (prodi_2, tahun, program) WHERE (aktif = true OR aktif IS NULL)",
				"CREATE INDEX IF NOT EXISTS idx_bcm_dash_pt_prodi3_tahun ON biodata_calon_mahasiswa (prodi3, tahun, program) WHERE (aktif = true OR aktif IS NULL)",
				"CREATE INDEX IF NOT EXISTS idx_bcm_dash_pt_prodi4_tahun ON biodata_calon_mahasiswa (prodi4, tahun, program) WHERE (aktif = true OR aktif IS NULL)",
				"CREATE INDEX IF NOT EXISTS idx_bcm_dash_pt_prodi5_tahun ON biodata_calon_mahasiswa (prodi5, tahun, program) WHERE (aktif = true OR aktif IS NULL)",
				"CREATE INDEX IF NOT EXISTS idx_bcm_dash_pt_mahasiswa_tahun ON biodata_calon_mahasiswa (mahasiswa, tahun) WHERE mahasiswa IS NOT NULL AND (aktif = true OR aktif IS NULL)",
				"CREATE INDEX IF NOT EXISTS idx_bcm_dash_pt_pembayaran_du ON biodata_calon_mahasiswa (pembayaran_daftar_ulang, tahun) WHERE pembayaran_daftar_ulang IS NOT NULL AND (aktif = true OR aktif IS NULL)",

				// --- KRS, IPK, SKS Kumulatif, Detail KRS/Nilai ---
				"CREATE INDEX IF NOT EXISTS idx_krs_dash_pt_ta_sem_mhs ON krs_mahasiswa (tahunakademik, semester, mahasiswa) INCLUDE (ipk, sksk)",
				"CREATE INDEX IF NOT EXISTS idx_krs_dash_pt_ipk ON krs_mahasiswa (tahunakademik, semester, ipk) WHERE semesterpendek IS NULL AND ipk > 0",
				"CREATE INDEX IF NOT EXISTS idx_krs_dash_pt_sksk ON krs_mahasiswa (tahunakademik, semester, sksk) WHERE semesterpendek IS NULL AND sksk > 0",
				"CREATE INDEX IF NOT EXISTS idx_detailperkuliahan_dash_pt_ta_sem_mhs ON detailperkuliahan (tahunakademik, semester, mahasiswa)",
				"CREATE INDEX IF NOT EXISTS idx_detailperkuliahan_dash_pt_nilai ON detailperkuliahan (tahunakademik, persetujuan, nilai_huruf, mahasiswa)",
				"CREATE INDEX IF NOT EXISTS idx_detailperkuliahan_dash_pt_perkul_pers ON detailperkuliahan (perkuliahan, persetujuan, mahasiswa)",

				// --- Kemahasiswaan: kegiatan, organisasi, prestasi mahasiswa ---
				"CREATE INDEX IF NOT EXISTS idx_kkpm_dash_pt_keg_mhs ON kegiatan_kemahasiswaan_punya_mahasiswa (kegiatan_kemahasiswaan, mahasiswa)",
				"CREATE INDEX IF NOT EXISTS idx_kegiatan_kemahasiswaan_dash_pt_ta ON kegiatan_kemahasiswaan (tahunakademik)",
				"CREATE INDEX IF NOT EXISTS idx_oikpm_dash_pt_tahun_mhs ON organisasi_intra_kampus_punya_mahasiswa (tahun, mahasiswa)",
				"CREATE INDEX IF NOT EXISTS idx_prestasi_mhs_dash_pt_ta_mhs ON prestasi_mahasiswa (tahunakademik, mahasiswa)",

				// --- Dosen dan beban perkuliahan ---
				"CREATE INDEX IF NOT EXISTS idx_dosen_dash_pt_fak_jur_aktif ON dosen (fakultas, jurusan, aktif, id)",
				"CREATE INDEX IF NOT EXISTS idx_perkuliahan_dash_pt_main ON perkuliahan (tahun_ajaran, jurusan, program, status_semesterpendek, id) WHERE (aktif = true OR aktif IS NULL) AND perkuliahan_paralel IS NULL",
				"CREATE INDEX IF NOT EXISTS idx_perkuliahan_dash_pt_jur_ta ON perkuliahan (jurusan, tahun_ajaran, program, ganjil_genap) WHERE (aktif = true OR aktif IS NULL) AND perkuliahan_paralel IS NULL",
				"CREATE INDEX IF NOT EXISTS idx_perkuliahan_dash_pt_dosen ON perkuliahan (dosen1, tahun_ajaran) WHERE (aktif = true OR aktif IS NULL)",

				// --- Pertemuan dan absensi/pencapaian perkuliahan ---
				/*
				 * Kolom pertemuan.absensi dapat berisi teks/HTML/JSON panjang. Jangan jadikan
				 * kolom tersebut sebagai key B-Tree karena PostgreSQL akan menolak jika
				 * ukuran satu entry index melebihi 8191 byte. Index lama dihapus lalu
				 * diganti dengan index partial yang hanya memakai kolom kecil.
				 */
				"DROP INDEX IF EXISTS idx_pertemuan_dash_pt_perkul_tgl_absensi_id",
				"DROP INDEX IF EXISTS idx_pertemuan_dash_pt_perkul_absensi",
				"CREATE INDEX IF NOT EXISTS idx_pertemuan_dash_pt_perkul_tgl_id ON pertemuan (perkuliahan, aktif, tanggal, id)",
				"CREATE INDEX IF NOT EXISTS idx_pertemuan_dash_pt_perkul_absensi_notnull ON pertemuan (perkuliahan, aktif, tanggal, id) WHERE absensi IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_pertemuan_dash_pt_perkul_absensi_null ON pertemuan (perkuliahan, aktif, tanggal, id) WHERE absensi IS NULL",

				// --- Aktivitas dosen, organisasi dosen, prestasi/karya dosen ---
				"CREATE INDEX IF NOT EXISTS idx_kkpd_dash_pt_keg_dosen ON kegiatan_kedosenan_punya_dosen (kegiatan_kedosenan, dosen)",
				"CREATE INDEX IF NOT EXISTS idx_kegiatan_kedosenan_dash_pt_ta ON kegiatan_kedosenan (tahunakademik)",
				"CREATE INDEX IF NOT EXISTS idx_org_dosen_punya_dash_pt_tahun_dosen ON organisasi_dosen_punya_dosen (tahun, dosen)",
				"CREATE INDEX IF NOT EXISTS idx_prestasi_dosen_dash_pt_ta_dosen ON prestasi_dosen (tahunakademik, dosen)",
				"CREATE INDEX IF NOT EXISTS idx_penghargaan_dosen_dash_pt_ta_dosen ON penghargaan_dosen (tahunakademik, dosen)",

				// --- Publikasi, penelitian/pengabdian, dan buku ajar ---
				"CREATE INDEX IF NOT EXISTS idx_artikel_dash_pt_tahun_user ON penelitiandanpengabdian.artikel (tahun, tbmuser)",
				"CREATE INDEX IF NOT EXISTS idx_buku_bahan_ajar_dash_pt_tahun_dosen ON buku_bahan_ajar (tahun, dosen_pengarang1)",
				"CREATE INDEX IF NOT EXISTS idx_ppp_dash_pt_penelitian_user ON penelitiandanpengabdian.pengajuan_penelitian_dan_pengabdian (penelitian_dan_pengabdian, tbmuser)",
				"CREATE INDEX IF NOT EXISTS idx_penelitian_pengabdian_dash_pt_tahun_tipe ON penelitiandanpengabdian.penelitian_dan_pengabdian (tahun, tipe_penelitian_dan_pengabdian)" };

		for (String sql : INDEX_QUERIES_DASBOR_PERGURUAN_TINGGI_TERPADU) {
			try {
				// Optional dashboard indexes: beberapa instalasi lama mungkin belum punya
				// modul/tabel tertentu.
				// Jika tabel/kolom belum ada, abaikan agar bootstrap index modul lain tetap
				// berjalan.
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:1972");
			}
		}

		// ===================================================================================
		// INDEX DATABASE OPTIMIZATION KHUSUS DASBOARD AKTIVITAS MAHASISWA
		// -----------------------------------------------------------------------------------
		// Target query class:
		// ais.action.master.dashboard.admin.DasboardAktivitasMahasiswa
		// Pola query utama:
		// - kegiatan_kemahasiswaan_punya_mahasiswa : mahasiswa, kegiatan_kemahasiswaan,
		// mulai, keterangan; status diambil dari kegiatan_kemahasiswaan.status.
		// - organisasi_intra_kampus_punya_mahasiswa : mahasiswa,
		// organisasi_intra_kampus,
		// mulai, keterangan.
		// - prestasi_mahasiswa : mahasiswa, tanggal, status, nama,
		// penyelenggara, capaian.
		// - penghargaan_mahasiswa : mahasiswa, tanggal, status, nama,
		// capaian.
		// - catatan_mahasiswa : mahasiswa, waktu, jenis_catatan_mahasiswa,
		// keterangan.
		//
		// Double-check anti redundant:
		// - idx_trgm_mhs_nama dan idx_trgm_mhs_nim sudah ada di blok GIN awal, jadi
		// tidak
		// dibuat ulang.
		// - idx_mhs_dashboard_stats / idx_mhs_dashboard_cuti dan
		// idx_jurusan_dashboard_join
		// sudah cukup untuk filter prodi/fakultas, jadi tidak dibuat ulang.
		// - idx_kkpm_dash_pt_keg_mhs, idx_oikpm_dash_pt_tahun_mhs, dan
		// idx_prestasi_mhs_dash_pt_ta_mhs yang sudah ada tetap dipertahankan, namun
		// belum
		// menutup pola dashboard baru yang banyak memakai filter mahasiswa + tanggal +
		// paging.
		// ===================================================================================
		String[] INDEX_QUERIES_DASBOARD_AKTIVITAS_MAHASISWA = new String[] {
				// --- Kegiatan Mahasiswa: root query berada di tabel relasi peserta ---
				"CREATE INDEX IF NOT EXISTS idx_kkpm_dash_act_mhs_mulai_id "
						+ "ON kegiatan_kemahasiswaan_punya_mahasiswa (mahasiswa, mulai DESC, id DESC) "
						+ "INCLUDE (kegiatan_kemahasiswaan) WHERE mahasiswa IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_kkpm_dash_act_mulai_id "
						+ "ON kegiatan_kemahasiswaan_punya_mahasiswa (mulai DESC, id DESC) "
						+ "INCLUDE (mahasiswa, kegiatan_kemahasiswaan)",
				"CREATE INDEX IF NOT EXISTS idx_kkpm_dash_act_keg_mulai_id "
						+ "ON kegiatan_kemahasiswaan_punya_mahasiswa (kegiatan_kemahasiswaan, mulai DESC, id DESC) "
						+ "INCLUDE (mahasiswa)",
				"CREATE INDEX IF NOT EXISTS idx_kegiatan_kemahasiswaan_dash_act_status_id "
						+ "ON kegiatan_kemahasiswaan (status, id)",

				// --- Organisasi Mahasiswa: root query berada di tabel relasi anggota
				// organisasi ---
				"CREATE INDEX IF NOT EXISTS idx_oikpm_dash_act_mhs_mulai_id "
						+ "ON organisasi_intra_kampus_punya_mahasiswa (mahasiswa, mulai DESC, id DESC) "
						+ "INCLUDE (organisasi_intra_kampus) WHERE mahasiswa IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_oikpm_dash_act_mulai_id "
						+ "ON organisasi_intra_kampus_punya_mahasiswa (mulai DESC, id DESC) "
						+ "INCLUDE (mahasiswa, organisasi_intra_kampus)",
				"CREATE INDEX IF NOT EXISTS idx_oikpm_dash_act_org_mulai_id "
						+ "ON organisasi_intra_kampus_punya_mahasiswa (organisasi_intra_kampus, mulai DESC, id DESC) "
						+ "INCLUDE (mahasiswa)",

				// --- Prestasi Mahasiswa: filter status, mahasiswa, tanggal, dan paging ---
				"CREATE INDEX IF NOT EXISTS idx_prestasi_mhs_dash_act_mhs_tgl_id "
						+ "ON prestasi_mahasiswa (mahasiswa, tanggal DESC, id DESC) "
						+ "INCLUDE (status, cabang_prestasi_mahasiswa, kategori_prestasi_mahasiswa)",
				"CREATE INDEX IF NOT EXISTS idx_prestasi_mhs_dash_act_status_tgl_id "
						+ "ON prestasi_mahasiswa (status, tanggal DESC, id DESC) INCLUDE (mahasiswa)",
				"CREATE INDEX IF NOT EXISTS idx_prestasi_mhs_dash_act_tgl_id "
						+ "ON prestasi_mahasiswa (tanggal DESC, id DESC) "
						+ "INCLUDE (mahasiswa, status, cabang_prestasi_mahasiswa, kategori_prestasi_mahasiswa)",

				// --- Karya/Penghargaan Mahasiswa ---
				"CREATE INDEX IF NOT EXISTS idx_penghargaan_mhs_dash_act_mhs_tgl_id "
						+ "ON penghargaan_mahasiswa (mahasiswa, tanggal DESC, id DESC) "
						+ "INCLUDE (status, kategori_penghargaan)",
				"CREATE INDEX IF NOT EXISTS idx_penghargaan_mhs_dash_act_status_tgl_id "
						+ "ON penghargaan_mahasiswa (status, tanggal DESC, id DESC) INCLUDE (mahasiswa)",
				"CREATE INDEX IF NOT EXISTS idx_penghargaan_mhs_dash_act_tgl_id "
						+ "ON penghargaan_mahasiswa (tanggal DESC, id DESC) "
						+ "INCLUDE (mahasiswa, status, kategori_penghargaan)",

				// --- Catatan Mahasiswa ---
				"CREATE INDEX IF NOT EXISTS idx_catatan_mhs_dash_act_mhs_waktu_id "
						+ "ON catatan_mahasiswa (mahasiswa, waktu DESC, id DESC) "
						+ "INCLUDE (jenis_catatan_mahasiswa)",
				"CREATE INDEX IF NOT EXISTS idx_catatan_mhs_dash_act_waktu_id "
						+ "ON catatan_mahasiswa (waktu DESC, id DESC) "
						+ "INCLUDE (mahasiswa, jenis_catatan_mahasiswa)",
				"CREATE INDEX IF NOT EXISTS idx_catatan_mhs_dash_act_jenis_waktu_id "
						+ "ON catatan_mahasiswa (jenis_catatan_mahasiswa, waktu DESC, id DESC) INCLUDE (mahasiswa)",

				// --- Keyword search ILIKE '%keyword%' selain nama/nim mahasiswa yang sudah ada
				// ---
				"CREATE INDEX IF NOT EXISTS idx_trgm_kegiatan_kemahasiswaan_nama "
						+ "ON kegiatan_kemahasiswaan USING gin (nama gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_kkpm_keterangan "
						+ "ON kegiatan_kemahasiswaan_punya_mahasiswa USING gin (keterangan gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_organisasi_intra_nama "
						+ "ON organisasi_intra_kampus USING gin (nama gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_oikpm_keterangan "
						+ "ON organisasi_intra_kampus_punya_mahasiswa USING gin (keterangan gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_prestasi_mhs_nama "
						+ "ON prestasi_mahasiswa USING gin (nama gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_prestasi_mhs_penyelenggara "
						+ "ON prestasi_mahasiswa USING gin (penyelenggara gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_prestasi_mhs_capaian "
						+ "ON prestasi_mahasiswa USING gin (capaian gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_penghargaan_mhs_nama "
						+ "ON penghargaan_mahasiswa USING gin (nama gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_penghargaan_mhs_capaian "
						+ "ON penghargaan_mahasiswa USING gin (capaian gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_catatan_mhs_keterangan "
						+ "ON catatan_mahasiswa USING gin (keterangan gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_jenis_catatan_mhs_nama "
						+ "ON jenis_catatan_mahasiswa USING gin (nama gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_jenis_catatan_mhs_aktif_nama_id "
						+ "ON jenis_catatan_mahasiswa (aktif, nama, id)" };

		for (String sql : INDEX_QUERIES_DASBOARD_AKTIVITAS_MAHASISWA) {
			try {
				// Optional index: beberapa instalasi lama mungkin belum punya kolom/tabel
				// tertentu.
				// Jika gagal, dashboard lain tetap tidak ikut terblokir.
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:2096");
			}
		}

		// ===================================================================================
		// INDEX PENGADAAN + VENDOR + ASET/INVENTARIS
		// ===================================================================================
		// INIT_INDEX_PENGADAAN_VENDOR_ASET_FAST_2026_05_30
		//
		// Dasar penyusunan:
		// - DasboardPengadaan: PR, PO, pembayaran, PKS, pembelian langsung,
		// penerimaan, detail PR, dan popup detail paging.
		// - DasboardAnalisisVendor: vendor, produk, tagihan vendor,
		// PR/PO/BAST/detail, dokumen vendor, retur, dan pengadaan produk.
		// - DashboardRekapAset: asset_detail -> asset -> master_asset dengan filter
		// tanggal/status dan group by jenis/kelompok/penyedia/lokasi/ruang/satker.
		//
		// Catatan double-check:
		// - Tidak memakai nama index lama idx_penyedia_asset_kode agar tidak bentrok.
		// - Semua nama index memakai prefix idx_dvpa_ (Dashboard Vendor Pengadaan
		// Aset).
		// - Beberapa index bersifat optional terhadap skema lama; bila kolom/tabel
		// belum ada,
		// exception ditelan agar startup aplikasi tetap aman.
		String[] INDEX_QUERIES_PENGADAAN_VENDOR_ASET_FAST = new String[] {
				// ---------------------------------------------------------------------------
				// 1. PURCHASE REQUEST / PR - asset.permintaan_pengadaan_master_asset
				// ---------------------------------------------------------------------------
				"CREATE INDEX IF NOT EXISTS idx_dvpa_pr_tgl_id ON asset.permintaan_pengadaan_master_asset (tanggal_pembuatan DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_pr_sat_tgl_id ON asset.permintaan_pengadaan_master_asset (satuan_kerja, tanggal_pembuatan DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_pr_pending ON asset.permintaan_pengadaan_master_asset (tanggal_pembuatan DESC, id DESC) WHERE disetujui_oleh IS NULL AND ditolak_oleh IS NULL AND (aktif IS NULL OR aktif = true)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_pr_app ON asset.permintaan_pengadaan_master_asset (tanggal_persetujuan DESC, satuan_kerja, id DESC) WHERE disetujui_oleh IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_pr_rej ON asset.permintaan_pengadaan_master_asset (tanggal_ditolak DESC, satuan_kerja, id DESC) WHERE ditolak_oleh IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_pr_po_fk ON asset.permintaan_pengadaan_master_asset (pemesanan_pengadaan_master_asset, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_pr_disposisi ON asset.permintaan_pengadaan_master_asset (disposisi_sop, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_pr_kode ON asset.permintaan_pengadaan_master_asset USING gin (kode gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_pr_ket ON asset.permintaan_pengadaan_master_asset USING gin (keterangan gin_trgm_ops)",

				// ---------------------------------------------------------------------------
				// 2. DETAIL PR - asset.permintaan_pengadaan_master_asset_detail
				// ---------------------------------------------------------------------------
				"CREATE INDEX IF NOT EXISTS idx_dvpa_prd_tgl_id ON asset.permintaan_pengadaan_master_asset_detail (tanggal_pembuatan DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_prd_sat_tgl_id ON asset.permintaan_pengadaan_master_asset_detail (satuan_kerja, tanggal_pembuatan DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_prd_pr_fk ON asset.permintaan_pengadaan_master_asset_detail (permintaan_pengadaan_master_asset, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_prd_master ON asset.permintaan_pengadaan_master_asset_detail (masterasset, tanggal_pembuatan DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_prd_um_fk ON asset.permintaan_pengadaan_master_asset_detail (uang_muka, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_prd_pod_fk ON asset.permintaan_pengadaan_master_asset_detail (pemesanan_pengadaan_master_asset_detail, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_prd_asset_fk ON asset.permintaan_pengadaan_master_asset_detail (asset, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_prd_real_um ON asset.permintaan_pengadaan_master_asset_detail (uang_muka, tanggal_pembuatan DESC, id DESC) WHERE uang_muka IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_prd_real_po ON asset.permintaan_pengadaan_master_asset_detail (pemesanan_pengadaan_master_asset_detail, tanggal_pembuatan DESC, id DESC) WHERE pemesanan_pengadaan_master_asset_detail IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_prd_belum_real ON asset.permintaan_pengadaan_master_asset_detail (tanggal_pembuatan DESC, id DESC) WHERE uang_muka IS NULL AND pemesanan_pengadaan_master_asset_detail IS NULL",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_prd_ket ON asset.permintaan_pengadaan_master_asset_detail USING gin (keterangan gin_trgm_ops)",

				// ---------------------------------------------------------------------------
				// 3. PURCHASE ORDER / PO - asset.pemesanan_pengadaan_master_asset
				// ---------------------------------------------------------------------------
				"CREATE INDEX IF NOT EXISTS idx_dvpa_po_tgl_id ON asset.pemesanan_pengadaan_master_asset (tanggal_pembuatan DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_po_sat_tgl_id ON asset.pemesanan_pengadaan_master_asset (satuan_kerja, tanggal_pembuatan DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_po_vendor_tgl_id ON asset.pemesanan_pengadaan_master_asset (penyedia, tanggal_pembuatan DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_po_pending ON asset.pemesanan_pengadaan_master_asset (tanggal_pembuatan DESC, id DESC) WHERE disetujui_oleh IS NULL AND ditolak_oleh IS NULL",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_po_app ON asset.pemesanan_pengadaan_master_asset (tanggal_persetujuan DESC, penyedia, satuan_kerja, id DESC) WHERE disetujui_oleh IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_po_rej ON asset.pemesanan_pengadaan_master_asset (tanggal_ditolak DESC, penyedia, satuan_kerja, id DESC) WHERE ditolak_oleh IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_po_direct ON asset.pemesanan_pengadaan_master_asset (tanggal_pembuatan DESC, penyedia, id DESC) WHERE pembelianlangsung = true",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_po_deadline ON asset.pemesanan_pengadaan_master_asset (pengiriman_paling_lambat, penyedia, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_po_disposisi ON asset.pemesanan_pengadaan_master_asset (disposisi_sop, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_po_kode ON asset.pemesanan_pengadaan_master_asset USING gin (kode gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_po_ket ON asset.pemesanan_pengadaan_master_asset USING gin (keterangan gin_trgm_ops)",

				// ---------------------------------------------------------------------------
				// 4. DETAIL PO - asset.pemesanan_pengadaan_master_asset_detail
				// ---------------------------------------------------------------------------
				"CREATE INDEX IF NOT EXISTS idx_dvpa_pod_po_fk ON asset.pemesanan_pengadaan_master_asset_detail (pemesanan_pengadaan_master_asset, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_pod_prd_fk ON asset.pemesanan_pengadaan_master_asset_detail (permintaan_pengadaan_master_asset_detail, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_pod_bastd_fk ON asset.pemesanan_pengadaan_master_asset_detail (penerimaan_pengadaan_master_asset_detail, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_pod_master ON asset.pemesanan_pengadaan_master_asset_detail (masterasset, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_pod_open ON asset.pemesanan_pengadaan_master_asset_detail (pemesanan_pengadaan_master_asset, id DESC) WHERE penerimaan_pengadaan_master_asset_detail IS NULL",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_pod_ket ON asset.pemesanan_pengadaan_master_asset_detail USING gin (keterangan gin_trgm_ops)",

				// ---------------------------------------------------------------------------
				// 5. TERIMA BARANG / BAST - asset.penerimaan_pengadaan_master_asset
				// ---------------------------------------------------------------------------
				"CREATE INDEX IF NOT EXISTS idx_dvpa_bast_tgl_id ON asset.penerimaan_pengadaan_master_asset (tanggal_pembuatan DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_bast_vendor_tgl_id ON asset.penerimaan_pengadaan_master_asset (penyedia, tanggal_pembuatan DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_bast_po_fk ON asset.penerimaan_pengadaan_master_asset (pemesanan_pengadaan_master_asset, tanggal_pembuatan DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_bast_sat_tgl_id ON asset.penerimaan_pengadaan_master_asset (satuan_kerja, tanggal_pembuatan DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_bast_tagihan ON asset.penerimaan_pengadaan_master_asset (kodetagihan, tanggaltagihan, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_bast_app ON asset.penerimaan_pengadaan_master_asset (tanggal_persetujuan DESC, penyedia, id DESC) WHERE disetujui_oleh IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_bast_saldo_fk ON asset.penerimaan_pengadaan_master_asset (saldo_awal_master_asset, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_bast_kode ON asset.penerimaan_pengadaan_master_asset USING gin (kode gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_bast_ket ON asset.penerimaan_pengadaan_master_asset USING gin (keterangan gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_bast_kodetag ON asset.penerimaan_pengadaan_master_asset USING gin (kodetagihan gin_trgm_ops)",

				// ---------------------------------------------------------------------------
				// 6. DETAIL BAST - asset.penerimaan_pengadaan_master_asset_detail
				// ---------------------------------------------------------------------------
				"CREATE INDEX IF NOT EXISTS idx_dvpa_bastd_bast_fk ON asset.penerimaan_pengadaan_master_asset_detail (penerimaan_pengadaan_master_asset, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_bastd_pod_fk ON asset.penerimaan_pengadaan_master_asset_detail (pemesanan_pengadaan_master_asset_detail, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_bastd_saldod_fk ON asset.penerimaan_pengadaan_master_asset_detail (saldo_awal_master_asset_detail, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_bastd_prd_fk ON asset.penerimaan_pengadaan_master_asset_detail (permintaan_pengadaan_master_asset_detail, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_bastd_master ON asset.penerimaan_pengadaan_master_asset_detail (masterasset, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_bastd_selisih ON asset.penerimaan_pengadaan_master_asset_detail (penerimaan_pengadaan_master_asset, id DESC) WHERE diterima < jumlah",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_bastd_kondisi ON asset.penerimaan_pengadaan_master_asset_detail USING gin (kondisi gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_bastd_ket ON asset.penerimaan_pengadaan_master_asset_detail USING gin (keterangan gin_trgm_ops)",

				// ---------------------------------------------------------------------------
				// 7. TERIMA TAGIHAN VENDOR - asset.saldo_awal_master_asset
				// ---------------------------------------------------------------------------
				"CREATE INDEX IF NOT EXISTS idx_dvpa_saldo_tgl_id ON asset.saldo_awal_master_asset (tanggal_pembuatan DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_saldo_vendor_tgl_id ON asset.saldo_awal_master_asset (penyedia, tanggal_pembuatan DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_saldo_vendor_tag ON asset.saldo_awal_master_asset (penyedia, tanggaltagihan DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_saldo_penerimaan_fk ON asset.saldo_awal_master_asset (penerimaan_pengadaan_master_asset, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_saldo_app ON asset.saldo_awal_master_asset (tanggal_persetujuan DESC, penyedia, id DESC) WHERE disetujui_oleh IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_saldo_lunas ON asset.saldo_awal_master_asset (lunas, penyedia, tanggal_pembuatan DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_saldo_sat_tgl ON asset.saldo_awal_master_asset (satuan_kerja, tanggal_pembuatan DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_saldo_kode ON asset.saldo_awal_master_asset USING gin (kode gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_saldo_ket ON asset.saldo_awal_master_asset USING gin (keterangan gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_saldo_kodetag ON asset.saldo_awal_master_asset USING gin (kodetagihan gin_trgm_ops)",

				// ---------------------------------------------------------------------------
				// 8. DETAIL TAGIHAN - asset.saldo_awal_master_asset_detail
				// ---------------------------------------------------------------------------
				"CREATE INDEX IF NOT EXISTS idx_dvpa_saldod_saldo_fk ON asset.saldo_awal_master_asset_detail (saldo_awal_master_asset, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_saldod_master ON asset.saldo_awal_master_asset_detail (master_asset, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_saldod_bastd_fk ON asset.saldo_awal_master_asset_detail (penerimaan_pengadaan_master_asset_detail, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_saldod_sat_fk ON asset.saldo_awal_master_asset_detail (satuan_kerja, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_saldod_asset_fk ON asset.saldo_awal_master_asset_detail (asset, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_saldod_ket ON asset.saldo_awal_master_asset_detail USING gin (keterangan gin_trgm_ops)",

				// ---------------------------------------------------------------------------
				// 9. PEMBAYARAN VENDOR: pengadaan/dp/termin + detail pembayaran pengadaan
				// ---------------------------------------------------------------------------
				"CREATE INDEX IF NOT EXISTS idx_dvpa_pay_vendor_tgl ON asset.pembayaran_pengadaan_master_asset (penyedia, tanggal_persetujuan DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_pay_app ON asset.pembayaran_pengadaan_master_asset (tanggal_persetujuan DESC, id DESC) WHERE disetujui_oleh IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_pay_kode ON asset.pembayaran_pengadaan_master_asset USING gin (kode gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_pay_ket ON asset.pembayaran_pengadaan_master_asset USING gin (keterangan gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_payd_pay_fk ON asset.pembayaran_pengadaan_master_asset_detail (pembayaran_pengadaan_master_asset, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_payd_bast_fk ON asset.pembayaran_pengadaan_master_asset_detail (penerimaan_pengadaan_master_asset, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_payd_join ON asset.pembayaran_pengadaan_master_asset_detail (pembayaran_pengadaan_master_asset, penerimaan_pengadaan_master_asset, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_paydp_vendor_tgl ON asset.pembayaran_dp_master_asset (penyedia, tanggal_persetujuan DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_paydp_app ON asset.pembayaran_dp_master_asset (tanggal_persetujuan DESC, id DESC) WHERE disetujui_oleh IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_payterm_vendor_tgl ON asset.pembayaran_termin_master_asset (penyedia, tanggal_persetujuan DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_payterm_app ON asset.pembayaran_termin_master_asset (tanggal_persetujuan DESC, id DESC) WHERE disetujui_oleh IS NOT NULL",

				// ---------------------------------------------------------------------------
				// 10. PKS, RETUR, DAN DOKUMEN VENDOR
				// ---------------------------------------------------------------------------
				"CREATE INDEX IF NOT EXISTS idx_dvpa_pks_vendor_tgl ON asset.perjanjian_kerjasama_master_asset (penyedia, tanggal_pembuatan DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_pks_sat_tgl ON asset.perjanjian_kerjasama_master_asset (satuan_kerja, tanggal_pembuatan DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_pks_app ON asset.perjanjian_kerjasama_master_asset (tanggal_persetujuan DESC, penyedia, id DESC) WHERE disetujui_oleh IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_pks_kode ON asset.perjanjian_kerjasama_master_asset USING gin (kode gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_pks_ket ON asset.perjanjian_kerjasama_master_asset USING gin (keterangan gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_retur_vendor_tgl ON asset.retur_pengadaan_master_asset (penyedia, tanggal_pembuatan DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_retur_bast_fk ON asset.retur_pengadaan_master_asset (penerimaan_pengadaan_master_asset, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_retur_app ON asset.retur_pengadaan_master_asset (tanggal_persetujuan DESC, penyedia, id DESC) WHERE disetujui_oleh IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_doc_vendor_status ON asset.penyedia_asset_punya_dokumen (penyedia_asset, status, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_doc_dok_status ON asset.penyedia_asset_punya_dokumen (dokumen_penyedia_asset, status, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_doc_belum ON asset.penyedia_asset_punya_dokumen (penyedia_asset, id DESC) WHERE status IS NULL OR status <> 'Terverifikasi'",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_doc_ket ON asset.penyedia_asset_punya_dokumen USING gin (keterangan gin_trgm_ops)",

				// ---------------------------------------------------------------------------
				// 11. MASTER VENDOR & MASTER PRODUK/ASSET
				// ---------------------------------------------------------------------------
				"CREATE INDEX IF NOT EXISTS idx_dvpa_vendor_aktif_status ON asset.penyedia_asset (aktif, status_penyedia_asset, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_vendor_jenis_kat ON asset.penyedia_asset (jenis_penyedia_asset, kategori_penyedia_asset, aktif, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_vendor_nama ON asset.penyedia_asset USING gin (nama gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_vendor_kode ON asset.penyedia_asset USING gin (kode gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_vendor_npwp ON asset.penyedia_asset USING gin (npwp gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_vendor_email ON asset.penyedia_asset USING gin (email gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_vendor_kontak ON asset.penyedia_asset USING gin (kontak gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_master_default_vendor ON asset.master_asset (default_penyedia, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_master_jenis_kelompok ON asset.master_asset (jenis_asset, kelompok_asset, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_master_nama ON asset.master_asset USING gin (nama gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_master_kode ON asset.master_asset USING gin (kode gin_trgm_ops)",

				// ---------------------------------------------------------------------------
				// 12. REKAP ASET / INVENTARIS: asset_detail, asset, master_asset
				// ---------------------------------------------------------------------------
				"CREATE INDEX IF NOT EXISTS idx_dvpa_assetdet_tgl_status ON asset.asset_detail (tanggalbeli, status_asset, asset, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_assetdet_asset_tgl ON asset.asset_detail (asset, tanggalbeli, status_asset, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_asset_master ON asset.asset (master_asset, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_asset_pemilik ON asset.asset (pemilik_asset, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_asset_lokasi ON asset.asset (lokasi, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_asset_ruang ON asset.asset (ruang, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_asset_saldo_detail ON asset.asset (saldo_awal_master_asset_detail, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_asset_pr_detail ON asset.asset (permintaan_pengadaan_master_asset_detail, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_asset_satker ON asset.asset (satuan_kerja, id DESC)",

				// ---------------------------------------------------------------------------
				// 13. INVENTORY/KOPERASI: koperasi.pengadaan_produk
				// ---------------------------------------------------------------------------
				"CREATE INDEX IF NOT EXISTS idx_dvpa_kop_prod_supplier_tgl ON koperasi.pengadaan_produk (supplier, waktupengadaan DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_kop_prod_produk_tgl ON koperasi.pengadaan_produk (produk, waktupengadaan DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_kop_prod_toko_tgl ON koperasi.pengadaan_produk (toko, waktupengadaan DESC, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_kop_nomorfaktur ON koperasi.pengadaan_produk USING gin (nomorfaktur gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dvpa_trgm_kop_namasupplier ON koperasi.pengadaan_produk USING gin (namasupplier gin_trgm_ops)" };

		for (String sql : INDEX_QUERIES_PENGADAAN_VENDOR_ASET_FAST) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:2295");
			}
		}

		// Refresh statistik planner untuk tabel-tabel yang dipakai dashboard
		// pengadaan/vendor/aset. ANALYZE dibuat optional karena sebagian instalasi
		// mungkin belum memiliki seluruh modul/tabel terkait.
		String[] ANALYZE_PENGADAAN_VENDOR_ASET_FAST = new String[] { "ANALYZE asset.permintaan_pengadaan_master_asset",
				"ANALYZE asset.permintaan_pengadaan_master_asset_detail",
				"ANALYZE asset.pemesanan_pengadaan_master_asset",
				"ANALYZE asset.pemesanan_pengadaan_master_asset_detail",
				"ANALYZE asset.penerimaan_pengadaan_master_asset",
				"ANALYZE asset.penerimaan_pengadaan_master_asset_detail", "ANALYZE asset.saldo_awal_master_asset",
				"ANALYZE asset.saldo_awal_master_asset_detail", "ANALYZE asset.pembayaran_pengadaan_master_asset",
				"ANALYZE asset.pembayaran_pengadaan_master_asset_detail", "ANALYZE asset.pembayaran_dp_master_asset",
				"ANALYZE asset.pembayaran_termin_master_asset", "ANALYZE asset.perjanjian_kerjasama_master_asset",
				"ANALYZE asset.retur_pengadaan_master_asset", "ANALYZE asset.penyedia_asset",
				"ANALYZE asset.penyedia_asset_punya_dokumen", "ANALYZE asset.master_asset", "ANALYZE asset.asset",
				"ANALYZE asset.asset_detail", "ANALYZE koperasi.pengadaan_produk" };
		for (String sql : ANALYZE_PENGADAAN_VENDOR_ASET_FAST) {
			try {
				eksekusiSql(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:2317");
			}
		}

		// ===================================================================================
		// INDEX DATABASE OPTIMIZATION KHUSUS DASBOARD SOP / WORKFLOW
		// -----------------------------------------------------------------------------------
		// Target query class: ais.action.master.sop.helper.DasboardSop
		// Pola query utama yang dioptimalkan:
		// - DisposisiSop: jumlah data pengajuan user login, filter waktu, SOP, keyword,
		// dan popup detail "Jumlah Data Pengajuan Anda".
		// - DisposisiAlurSop: menunggu disposisi saya, sudah disposisi, selesai,
		// menunggu aktor/tahap, deadline, funnel workflow, aging/backlog, dan grid
		// popup.
		// - AlurSop/AktorSop/Sop: join actor workflow, start node, filter SOP, dan
		// pencarian nama/kode/aktor.
		//
		// Double-check anti redundant:
		// - Belum ada index khusus tabel SOP di schema default/public pada blok
		// sebelumnya; yang ada hanya
		// asset.*.disposisi_sop untuk modul pengadaan/vendor/aset.
		// - Tidak membuat index FK single-column generik yang sudah tertutup composite
		// lebih spesifik seperti (disposisi_sop, id DESC) atau (alur_sop, id DESC).
		// - Semua nama index memakai prefix idx_dsop_ agar tidak bentrok dengan index
		// modul Surat/Pengadaan/Aset yang sudah ada.
		// ===================================================================================
		String[] INDEX_QUERIES_DASHBOARD_SOP_FAST = new String[] {
				// ---------------------------------------------------------------------------
				// 1. DISPOSISI SOP: root pengajuan user, filter tanggal, SOP, dan keyword
				// ---------------------------------------------------------------------------
				"CREATE INDEX IF NOT EXISTS idx_dsop_disposisi_user_waktu_id "
						+ "ON disposisi_sop (diajukan_oleh, waktu DESC, id DESC) "
						+ "INCLUDE (sop, mahasiswa, siswa) WHERE (aktif = true OR aktif IS NULL)",
				"CREATE INDEX IF NOT EXISTS idx_dsop_disposisi_mhs_waktu_id "
						+ "ON disposisi_sop (mahasiswa, waktu DESC, id DESC) "
						+ "INCLUDE (sop, diajukan_oleh, siswa) WHERE mahasiswa IS NOT NULL AND (aktif = true OR aktif IS NULL)",
				"CREATE INDEX IF NOT EXISTS idx_dsop_disposisi_siswa_waktu_id "
						+ "ON disposisi_sop (siswa, waktu DESC, id DESC) "
						+ "INCLUDE (sop, diajukan_oleh, mahasiswa) WHERE siswa IS NOT NULL AND (aktif = true OR aktif IS NULL)",
				"CREATE INDEX IF NOT EXISTS idx_dsop_disposisi_sop_waktu_id "
						+ "ON disposisi_sop (sop, waktu DESC, id DESC) "
						+ "INCLUDE (diajukan_oleh, mahasiswa, siswa) WHERE (aktif = true OR aktif IS NULL)",
				"CREATE INDEX IF NOT EXISTS idx_dsop_disposisi_waktu_id "
						+ "ON disposisi_sop (waktu DESC, id DESC) WHERE (aktif = true OR aktif IS NULL)",
				"CREATE INDEX IF NOT EXISTS idx_dsop_trgm_disposisi_properti "
						+ "ON disposisi_sop USING gin (properti gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dsop_trgm_disposisi_keterangan "
						+ "ON disposisi_sop USING gin (keterangan gin_trgm_ops)",

				// ---------------------------------------------------------------------------
				// 2. DISPOSISI ALUR SOP: join utama + paging popup detail
				// ---------------------------------------------------------------------------
				"CREATE INDEX IF NOT EXISTS idx_dsop_das_disposisi_id "
						+ "ON disposisi_alur_sop (disposisi_sop, id DESC) "
						+ "INCLUDE (alur_sop, waktu, waktumaksimal, selesai, sebelumnya, setelahnya) "
						+ "WHERE (aktif = true OR aktif IS NULL)",
				"CREATE INDEX IF NOT EXISTS idx_dsop_das_alur_id " + "ON disposisi_alur_sop (alur_sop, id DESC) "
						+ "INCLUDE (disposisi_sop, waktu, waktumaksimal, selesai) "
						+ "WHERE (aktif = true OR aktif IS NULL)",
				"CREATE INDEX IF NOT EXISTS idx_dsop_das_recent_active " + "ON disposisi_alur_sop (id DESC) "
						+ "INCLUDE (disposisi_sop, alur_sop, waktu, waktumaksimal, selesai, sebelumnya, setelahnya) "
						+ "WHERE (aktif = true OR aktif IS NULL)",

				// ---------------------------------------------------------------------------
				// 3. Antrian: Proses yang sedang menunggu Disposisi Anda
				// Criteria: aktif, belum selesai, diajukan_oleh/mahasiswa/siswa null,
				// setelahnya null, sebelumnya not null, order id / deadline.
				// ---------------------------------------------------------------------------
				"CREATE INDEX IF NOT EXISTS idx_dsop_das_menunggu_saya_id " + "ON disposisi_alur_sop (id DESC) "
						+ "INCLUDE (disposisi_sop, alur_sop, sebelumnya, waktumaksimal) "
						+ "WHERE (aktif = true OR aktif IS NULL) " + "AND (selesai = false OR selesai IS NULL) "
						+ "AND diajukan_oleh IS NULL AND mahasiswa IS NULL AND siswa IS NULL "
						+ "AND setelahnya IS NULL AND sebelumnya IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_dsop_das_menunggu_saya_deadline "
						+ "ON disposisi_alur_sop (waktumaksimal ASC, id DESC) "
						+ "INCLUDE (disposisi_sop, alur_sop, sebelumnya) " + "WHERE (aktif = true OR aktif IS NULL) "
						+ "AND (selesai = false OR selesai IS NULL) "
						+ "AND diajukan_oleh IS NULL AND mahasiswa IS NULL AND siswa IS NULL "
						+ "AND setelahnya IS NULL AND sebelumnya IS NOT NULL AND waktumaksimal IS NOT NULL",

				// ---------------------------------------------------------------------------
				// 4. Riwayat disposisi user login dan pengajuan selesai
				// ---------------------------------------------------------------------------
				"CREATE INDEX IF NOT EXISTS idx_dsop_das_root_user_waktu_id "
						+ "ON disposisi_alur_sop (diajukan_oleh, waktu DESC, id DESC) "
						+ "INCLUDE (disposisi_sop, alur_sop, selesai) "
						+ "WHERE diajukan_oleh IS NOT NULL AND (aktif = true OR aktif IS NULL)",
				"CREATE INDEX IF NOT EXISTS idx_dsop_das_root_mhs_waktu_id "
						+ "ON disposisi_alur_sop (mahasiswa, waktu DESC, id DESC) "
						+ "INCLUDE (disposisi_sop, alur_sop, selesai) "
						+ "WHERE mahasiswa IS NOT NULL AND (aktif = true OR aktif IS NULL)",
				"CREATE INDEX IF NOT EXISTS idx_dsop_das_root_siswa_waktu_id "
						+ "ON disposisi_alur_sop (siswa, waktu DESC, id DESC) "
						+ "INCLUDE (disposisi_sop, alur_sop, selesai) "
						+ "WHERE siswa IS NOT NULL AND (aktif = true OR aktif IS NULL)",
				"CREATE INDEX IF NOT EXISTS idx_dsop_das_selesai_waktu_id "
						+ "ON disposisi_alur_sop (waktu DESC, id DESC) "
						+ "INCLUDE (disposisi_sop, alur_sop, setelahnya, selesai) "
						+ "WHERE (aktif = true OR aktif IS NULL) AND waktu IS NOT NULL",

				// ---------------------------------------------------------------------------
				// 5. Menunggu aktor/tahap, deadline analytics, dan metadata quality
				// ---------------------------------------------------------------------------
				"CREATE INDEX IF NOT EXISTS idx_dsop_das_menunggu_aktor_id "
						+ "ON disposisi_alur_sop (disposisi_sop, id DESC) "
						+ "INCLUDE (alur_sop, waktumaksimal, selesai) " + "WHERE (aktif = true OR aktif IS NULL) "
						+ "AND diajukan_oleh IS NULL AND mahasiswa IS NULL AND siswa IS NULL",
				"CREATE INDEX IF NOT EXISTS idx_dsop_das_deadline_active "
						+ "ON disposisi_alur_sop (waktumaksimal ASC, id DESC) "
						+ "INCLUDE (disposisi_sop, alur_sop, selesai) "
						+ "WHERE (aktif = true OR aktif IS NULL) AND waktumaksimal IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_dsop_das_no_deadline " + "ON disposisi_alur_sop (id DESC) "
						+ "INCLUDE (disposisi_sop, alur_sop, selesai) "
						+ "WHERE (aktif = true OR aktif IS NULL) AND waktumaksimal IS NULL",
				"CREATE INDEX IF NOT EXISTS idx_dsop_trgm_das_properti "
						+ "ON disposisi_alur_sop USING gin (properti gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dsop_trgm_das_keyword "
						+ "ON disposisi_alur_sop USING gin (keyword gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dsop_trgm_das_keterangan "
						+ "ON disposisi_alur_sop USING gin (keterangan gin_trgm_ops)",

				// ---------------------------------------------------------------------------
				// 6. ALUR SOP dan AKTOR SOP: start node, actor lookup, tahap, dan keyword
				// ---------------------------------------------------------------------------
				"CREATE INDEX IF NOT EXISTS idx_dsop_alur_start_aktor_sop "
						+ "ON alur_sop (start, aktor_sop, sop, id) WHERE start = true",
				"CREATE INDEX IF NOT EXISTS idx_dsop_alur_sop_start_id " + "ON alur_sop (sop, start, id)",
				"CREATE INDEX IF NOT EXISTS idx_dsop_alur_setelahnya_chain "
						+ "ON alur_sop (setelahnya, setelahnya2, setelahnya3, setelahnya4, setelahnya5, id)",
				"CREATE INDEX IF NOT EXISTS idx_dsop_trgm_alur_nama " + "ON alur_sop USING gin (nama gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dsop_trgm_alur_aktor " + "ON alur_sop USING gin (aktor gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dsop_trgm_alur_kode " + "ON alur_sop USING gin (kode gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dsop_trgm_aktor_jenis_pengguna "
						+ "ON aktor_sop USING gin (jenis_pengguna gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dsop_trgm_aktor_username_pengguna "
						+ "ON aktor_sop USING gin (username_pengguna gin_trgm_ops)",

				// ---------------------------------------------------------------------------
				// 7. MASTER SOP: filter combobox, group by, dan pencarian nama/kode/keterangan
				// ---------------------------------------------------------------------------
				"CREATE INDEX IF NOT EXISTS idx_dsop_sop_aktif_nama_id " + "ON sop (aktif, nama, id)",
				"CREATE INDEX IF NOT EXISTS idx_dsop_sop_satker_aktif_id " + "ON sop (satuan_kerja, aktif, id)",
				"CREATE INDEX IF NOT EXISTS idx_dsop_sop_jur_fak_aktif_id " + "ON sop (jurusan, fakultas, aktif, id)",
				"CREATE INDEX IF NOT EXISTS idx_dsop_sop_yayasan_sekolah_aktif_id "
						+ "ON sop (yayasan, sekolah, aktif, id)",
				"CREATE INDEX IF NOT EXISTS idx_dsop_trgm_sop_nama " + "ON sop USING gin (nama gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dsop_trgm_sop_kode " + "ON sop USING gin (kode gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_dsop_trgm_sop_keterangan "
						+ "ON sop USING gin (keterangan gin_trgm_ops)",

				// ---------------------------------------------------------------------------
				// 8. Pendukung global Satker filter: DisposisiSop -> Tbmuser -> Pegawai
				// Existing idx_tbmuser_broadcast_filter diawali kolom aktif, sehingga tidak
				// ideal untuk join langsung by pegawai pada filter satker dashboard SOP.
				// ---------------------------------------------------------------------------
				"CREATE INDEX IF NOT EXISTS idx_dsop_tbmuser_pegawai_userid "
						+ "ON tbmuser (pegawai, userid) WHERE pegawai IS NOT NULL" };

		for (String sql : INDEX_QUERIES_DASHBOARD_SOP_FAST) {
			try {
				// Optional index: beberapa instalasi lama mungkin belum memiliki tabel/kolom
				// SOP tertentu
				// atau belum memiliki seluruh kolom dashboard workflow. Jika salah satu gagal,
				// index lain tetap lanjut.
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:2482");
			}
		}

		// Refresh statistik planner untuk tabel-tabel yang dipakai DasboardSop.
		String[] ANALYZE_DASHBOARD_SOP_FAST = new String[] { "ANALYZE disposisi_sop", "ANALYZE disposisi_alur_sop",
				"ANALYZE alur_sop", "ANALYZE aktor_sop", "ANALYZE sop", "ANALYZE tbmuser", "ANALYZE pegawai" };
		for (String sql : ANALYZE_DASHBOARD_SOP_FAST) {
			try {
				eksekusiSql(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:2492");
			}
		}

		// Refresh statistik planner setelah index dashboard besar dibuat.
		// CATATAN TIMEOUT: detailperkuliahan & pertemuan adalah tabel besar (banyak instalasi
		// eCampus) — ANALYZE dengan timeout default (mengikuti statement_timeout bawaan koneksi,
		// sering kali sangat pendek) selalu gagal "canceling statement due to statement timeout"
		// (SQLState 57014) sebelum sempat menyelesaikan pembaruan statistik. eksekusiSql10Menit
		// men-SET LOCAL statement_timeout=600s HANYA untuk transaksi ANALYZE ini (tidak bocor ke
		// operasi lain di session/connection lain karena SET LOCAL otomatis berakhir saat
		// transaksi commit/rollback, dan tiap query di sini memakai session/transaksi baru).
		String[] ANALYZE_DASBOR_PERGURUAN_TINGGI_TERPADU = new String[] { "ANALYZE fakultas", "ANALYZE jurusan",
				"ANALYZE kurikulum", "ANALYZE mahasiswa", "ANALYZE biodata_calon_mahasiswa", "ANALYZE krs_mahasiswa",
				"ANALYZE detailperkuliahan", "ANALYZE perkuliahan", "ANALYZE pertemuan", "ANALYZE dosen",
				"ANALYZE kegiatan_kemahasiswaan_punya_mahasiswa", "ANALYZE kegiatan_kemahasiswaan",
				"ANALYZE organisasi_intra_kampus_punya_mahasiswa", "ANALYZE organisasi_intra_kampus",
				"ANALYZE prestasi_mahasiswa", "ANALYZE penghargaan_mahasiswa", "ANALYZE catatan_mahasiswa",
				"ANALYZE jenis_catatan_mahasiswa" };
		for (String sql : ANALYZE_DASBOR_PERGURUAN_TINGGI_TERPADU) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:2507");
			}
		}

		// ===================================================================================
		// INDEX DATABASE OPTIMIZATION UNTUK DASBOR PERGURUAN TINGGI TERPADU
		// Tambahan: Bimbingan, Sidang, Wisuda, Lulusan, Tracer, Masa Studi, Semester
		// Lulus
		// ===================================================================================
		String[] INDEX_QUERIES_DASBOR_PT_BIMBINGAN_LULUSAN = new String[] {
				// Mahasiswa Request Tugas Akhir / DashboardBimbinganMahasiswa
				// Existing idx_reqta_sorting sudah ada untuk sorting umum, index ini khusus
				// join mahasiswa + filter status.
				"CREATE INDEX IF NOT EXISTS idx_dpt2_reqta_ta_mhs_status ON mahasiswa_request_tugas_akhir (tahun_akademik, mahasiswa, status, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dpt2_reqta_mhs_ta ON mahasiswa_request_tugas_akhir (mahasiswa, tahun_akademik, id DESC)",

				// Skripsi / DashboardSidangMahasiswa
				// Existing idx_skripsi_sorting tetap dipakai, index ini menambah kolom status
				// sidang untuk dashboard ringkasan.
				"CREATE INDEX IF NOT EXISTS idx_dpt2_skripsi_ta_mhs_sidang ON skripsi (tahun_akademik, mahasiswa, telah_sidang, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dpt2_skripsi_mhs_ta ON skripsi (mahasiswa, tahun_akademik, id DESC)",

				// Pendaftaran Wisuda / DashboardWisudaMahasiswa
				"CREATE INDEX IF NOT EXISTS idx_dpt2_pendaftaran_wisuda_skripsi_mhs ON pendaftaran_wisuda (skripsi, mahasiswa, persetujuan_wisuda, wisuda, id DESC)",
				"CREATE INDEX IF NOT EXISTS idx_dpt2_pendaftaran_wisuda_mhs ON pendaftaran_wisuda (mahasiswa, id DESC)",

				// Lulusan dan tracer alumni / DashboardLulusan + dashboard turunannya
				// Existing idx_mhs_dashboard_lulus_thn memiliki leading status_keluar; index
				// berikut leading tahunlulus
				// agar query rentang tahun + prodi/program lebih cepat.
				"CREATE INDEX IF NOT EXISTS idx_dpt2_mhs_lulusan_filter ON mahasiswa (tahunlulus, jurusan, program, aktif, status_keluar)",
				"CREATE INDEX IF NOT EXISTS idx_dpt2_mhs_lulusan_predikat ON mahasiswa (tahunlulus, jurusan, program, aktif, predikat_kelulusan)",
				"CREATE INDEX IF NOT EXISTS idx_dpt2_mhs_lulusan_tracer_status ON mahasiswa (tahunlulus, jurusan, program, aktif, status_setelah_lulus)",
				"CREATE INDEX IF NOT EXISTS idx_dpt2_mhs_lulusan_tracer_pekerjaan ON mahasiswa (tahunlulus, jurusan, program, aktif, status_pekerjaan_setelah_lulus)",
				"CREATE INDEX IF NOT EXISTS idx_dpt2_mhs_lulusan_tracer_domisili ON mahasiswa (tahunlulus, jurusan, program, aktif, status_domisili_setelah_lulus)",
				"CREATE INDEX IF NOT EXISTS idx_dpt2_mhs_lulusan_masa_studi ON mahasiswa (tahunlulus, jurusan, program, aktif, tahunangkatan, semesterlulus)",

				// Lookup dan grouping master relasi lulusan; dibuat ringan, tidak mengganti
				// index existing jurusan/fakultas.
				"CREATE INDEX IF NOT EXISTS idx_dpt2_mhs_lulusan_status_keluar_notnull ON mahasiswa (jurusan, tahunlulus, id DESC) WHERE status_keluar IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_dpt2_mhs_lulusan_predikat_notnull ON mahasiswa (jurusan, tahunlulus, id DESC) WHERE predikat_kelulusan IS NOT NULL",
				"CREATE INDEX IF NOT EXISTS idx_dpt2_mhs_lulusan_semester_notnull ON mahasiswa (jurusan, tahunlulus, id DESC) WHERE semesterlulus IS NOT NULL" };

		for (String sql : INDEX_QUERIES_DASBOR_PT_BIMBINGAN_LULUSAN) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitIndex.java:2554");
			}
		}

		String[] ANALYZE_DASBOR_PT_BIMBINGAN_LULUSAN = new String[] { "ANALYZE mahasiswa_request_tugas_akhir",
				"ANALYZE skripsi", "ANALYZE pendaftaran_wisuda", "ANALYZE mahasiswa" };
		for (String sql : ANALYZE_DASBOR_PT_BIMBINGAN_LULUSAN) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitIndex.java:2564");
			}
		}

		// ===================================================================================
		// INDEX DATABASE OPTIMIZATION KHUSUS DASHBOARD KEHADIRAN PEGAWAI / HRD
		// -----------------------------------------------------------------------------------
		// Target query class:
		// - ais.action.master.payroll.helper.DashboardKehadiranExpert
		// - ais.action.master.payroll.helper.DashboardKehadiranTambahan
		//
		// Double-check anti redundant:
		// - idx_kehadiran_peg_tgl (pegawai, tanggal) sudah ada, sehingga tidak dibuat
		// ulang.
		// - idx_kehadiran_tanggal_only (tanggal) sudah ada, tetapi belum menutup pola
		// filter
		// tanggal + statusabsensi + pegawai untuk pencarian pegawai dashboard.
		// - idx_cuti_izin_setujui_peg_mulai, idx_cuti_izin_pegawai_setujui_mulai, dan
		// idx_cuti_izin_pegawai_setujui_sampai sudah ada; index baru hanya menutup
		// query
		// periode yang memakai OR mulai/sampai dan daftar pegawai besar.
		// - idx_pengajuan_pegawai_setujui_peg_waktu sudah ada; index baru menutup pola
		// waktu + waktusampai pada pengajuan yang disetujui.
		// - idx_pegawai_status_aktif_sort dan idx_pegawai_tipe sudah ada; index baru
		// hanya
		// menambah jalur dashboard status + aktif + satker + tipe + id.
		// ===================================================================================
		String[] INDEX_QUERIES_DASHBOARD_KEHADIRAN_HRD = new String[] {
				// Root query pencarian pegawai dashboard: filter tanggal presensi + status
				// bukan BELUM_ABSEN.
				"CREATE INDEX IF NOT EXISTS idx_skh_dash_hrd_tgl_peg_notbelum "
						+ "ON public.status_kehadiran_karyawan_harian (tanggal, pegawai) "
						+ "WHERE pegawai IS NOT NULL AND statusabsensi <> 5",

				// Lookup harian detail presensi: existing (pegawai,tanggal) tetap dipakai,
				// index ini membantu paging/order dan join tambahan.
				"CREATE INDEX IF NOT EXISTS idx_skh_dash_hrd_peg_tgl_status_id "
						+ "ON public.status_kehadiran_karyawan_harian (pegawai, tanggal, statusabsensi, id DESC) "
						+ "WHERE pegawai IS NOT NULL",

				// Filter master pegawai aktif, satuan kerja, tipe pegawai presensi/lembur.
				"CREATE INDEX IF NOT EXISTS idx_pegawai_dash_hrd_status_aktif_satker_tipe_id "
						+ "ON public.pegawai (status_pegawai, aktif, satuan_kerja, tipe_pegawai, id)",

				// Keyword cari pegawai: nama / mycode / code menggunakan ilike '%keyword%'.
				"CREATE INDEX IF NOT EXISTS idx_trgm_pegawai_dash_hrd_nama "
						+ "ON public.pegawai USING gin (nama gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_pegawai_dash_hrd_mycode "
						+ "ON public.pegawai USING gin (mycode gin_trgm_ops)",
				"CREATE INDEX IF NOT EXISTS idx_trgm_pegawai_dash_hrd_code "
						+ "ON public.pegawai USING gin (code gin_trgm_ops)",

				// Cuti dan izin: dashboard mengambil cuti disetujui berdasarkan pegawai dan
				// periode mulai/sampai.
				"CREATE INDEX IF NOT EXISTS idx_cuti_izin_dash_hrd_peg_setujui_mulai_sampai "
						+ "ON payroll.cuti_dan_izin (pegawai, setujui, mulai, sampai)",
				"CREATE INDEX IF NOT EXISTS idx_cuti_izin_dash_hrd_peg_setujui_sampai_mulai "
						+ "ON payroll.cuti_dan_izin (pegawai, setujui, sampai, mulai)",

				// Pengajuan pegawai: dashboard mengambil pengajuan disetujui berdasarkan
				// pegawai, waktu, dan waktu sampai.
				"CREATE INDEX IF NOT EXISTS idx_pengajuan_dash_hrd_peg_setujui_waktu_sampai "
						+ "ON public.pengajuan_pegawai (pegawai, setujui, waktu, waktusampai)",
				"CREATE INDEX IF NOT EXISTS idx_pengajuan_dash_hrd_peg_setujui_sampai_waktu "
						+ "ON public.pengajuan_pegawai (pegawai, setujui, waktusampai, waktu)" };

		for (String sql : INDEX_QUERIES_DASHBOARD_KEHADIRAN_HRD) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitIndex.java:2634");
			}
		}

		String[] ANALYZE_DASHBOARD_KEHADIRAN_HRD = new String[] { "ANALYZE public.status_kehadiran_karyawan_harian",
				"ANALYZE public.pegawai", "ANALYZE payroll.cuti_dan_izin", "ANALYZE public.pengajuan_pegawai",
				"ANALYZE public.cuti_bersama" };
		for (String sql : ANALYZE_DASHBOARD_KEHADIRAN_HRD) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitIndex.java:2645");
			}
		}

		// ===================================================================================
		// INDEX DATABASE OPTIMIZATION KHUSUS DASBOR E-LEARNING SUPER FAST
		// -----------------------------------------------------------------------------------
		// Target query class:
		// - DashboardTrenAktivitasPerkuliahan
		// - DashboardRekapPertemuanPerkuliahan
		// - DashboardTimelinePertemuan.buildDashboardSummary(...)
		//
		// Double-check anti redundant:
		// - idx_pertemuan_perkuliahan (perkuliahan, aktif, tanggal, pertemuan_ke, id)
		// sudah ada,
		// sehingga index single-column/perkuliahan-id tidak dibuat ulang.
		// - idx_detailperkuliahan_dash_pt_perkul_pers sudah ada, sehingga pola
		// (perkuliahan, persetujuan, mahasiswa) tidak dibuat ulang.
		// - idx_ppu_pertemuan_ujian sudah ada; idx_tugaspert_pertemuan_fk (single-col) kini DI-DROP karena
		// REDUNDAN — komposit (pertemuan, id) di bawah menutup agregasi/paging/join sekaligus.
		// - idx_pfc_pertemuan, idx_vp_pertemuan, idx_ap_pertemuan (single-col) kini DI-DROP; digantikan komposit
		// idx_dash_el_{pfc,video,audio}_pertemuan_id (pertemuan, id) untuk query count/join dashboard modern.
		// ===================================================================================
		String[] INDEX_QUERIES_DASHBOARD_ELEARNING_FAST = new String[] {
				// Filter utama perkuliahan sesuai generateWhere/generateWhereCount dashboard
				// rekap.
				"CREATE INDEX IF NOT EXISTS idx_dash_el_perkuliahan_filter_full "
						+ "ON perkuliahan (tahun_ajaran, semester, jurusan, program, id) "
						+ "WHERE (aktif = true OR aktif IS NULL)",
				"CREATE INDEX IF NOT EXISTS idx_dash_el_perkuliahan_dosen1_ta_sem "
						+ "ON perkuliahan (dosen1, tahun_ajaran, semester, id) "
						+ "WHERE (aktif = true OR aktif IS NULL)",
				"CREATE INDEX IF NOT EXISTS idx_dash_el_perkuliahan_dosen2_ta_sem "
						+ "ON perkuliahan (dosen2, tahun_ajaran, semester, id) "
						+ "WHERE (aktif = true OR aktif IS NULL)",

				// Detail KRS mahasiswa untuk filter mahasiswa dan agregasi jumlah peserta.
				"CREATE INDEX IF NOT EXISTS idx_dash_el_detailperkuliahan_mhs_perkul_pers "
						+ "ON detailperkuliahan (mahasiswa, perkuliahan, persetujuan)",

				// Pertemuan untuk aggregate ID, tanggal, absensi, dan join ke perkuliahan.
				"CREATE INDEX IF NOT EXISTS idx_dash_el_pertemuan_perkul_absensi_notempty "
						+ "ON pertemuan (perkuliahan, id) WHERE perkuliahan IS NOT NULL AND absensi IS NOT NULL AND absensi <> ''",
				"CREATE INDEX IF NOT EXISTS idx_dash_el_pertemuan_perkul_ta_smt "
						+ "ON pertemuan (ta, smt, perkuliahan, id) WHERE perkuliahan IS NOT NULL",

				// Agregasi materi, audio, video, tugas, ujian, diskusi.
				"CREATE INDEX IF NOT EXISTS idx_dash_el_tugas_pertemuan_pertemuan_id ON tugas_pertemuan (pertemuan, id)",
				"CREATE INDEX IF NOT EXISTS idx_dash_el_ppu_pertemuan_id ON pertemuan_punya_ujian (pertemuan, id)",
				"CREATE INDEX IF NOT EXISTS idx_dash_el_diskusi_pertemuan_id ON pertemuan_punya_diskusi (pertemuan, id)" };

		for (String sql : INDEX_QUERIES_DASHBOARD_ELEARNING_FAST) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:2699");
				// Optional index: beberapa instalasi lama mungkin belum memiliki tabel file
				// tertentu.
				// Jangan blokir inisialisasi aplikasi hanya karena satu index gagal dibuat.
			}
		}

		INDEX_QUERIES_DASHBOARD_ELEARNING_FAST = new String[] {
				// Dokumen perkuliahan: menggantikan pemanggilan LampiranLain.ambil(...)
				// berulang.
				"CREATE INDEX IF NOT EXISTS idx_dash_el_lampiran_ref_nama_id ON lampiran_lain (ref, nama, id)",
				"CREATE INDEX IF NOT EXISTS idx_dash_el_lampiran_nama_ref_id ON lampiran_lain (nama, ref, id)",
				"CREATE INDEX IF NOT EXISTS idx_dash_el_pfc_pertemuan_id ON pertemuan_file_content (pertemuan, id)",
				"CREATE INDEX IF NOT EXISTS idx_dash_el_video_pertemuan_id ON video_pertemuan (pertemuan, id)",
				"CREATE INDEX IF NOT EXISTS idx_dash_el_audio_pertemuan_id ON audio_pertemuan (pertemuan, id)",
				"CREATE INDEX IF NOT EXISTS idx_dash_el_tugas_file_pertemuan_id ON tugas_file_content (pertemuan, id)" };

		for (String sql : INDEX_QUERIES_DASHBOARD_ELEARNING_FAST) {
			try {
				eksekusiSqlStreaming(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:2719");
				// Optional index: beberapa instalasi lama mungkin belum memiliki tabel file
				// tertentu.
				// Jangan blokir inisialisasi aplikasi hanya karena satu index gagal dibuat.
			}
		}

		String[] ANALYZE_DASHBOARD_ELEARNING_FAST = new String[] { "ANALYZE perkuliahan", "ANALYZE pertemuan",
				"ANALYZE detailperkuliahan" };
		for (String sql : ANALYZE_DASHBOARD_ELEARNING_FAST) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:2731");
			}
		}

		ANALYZE_DASHBOARD_ELEARNING_FAST = new String[] { "ANALYZE pertemuan_file_content", "ANALYZE video_pertemuan",
				"ANALYZE audio_pertemuan", "ANALYZE lampiran_lain" };
		for (String sql : ANALYZE_DASHBOARD_ELEARNING_FAST) {
			try {
				eksekusiSqlStreaming(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:2740");
			}
		}

		initIndexPengaturanBiayaSuperFast();

		// ---------------------------------------------------------------------------
		// 14. GAP-CLOSURE: koperasi.produk/koperasi.pembelian TANPA index sama sekali
		// pada kolom `toko`/`produk` -- ketahuan lewat keluhan lapangan "Bersihkan Produk
		// Duplikat" (KantinHelper.cariGrupDuplikat) timeout terus pada toko dgn puluhan
		// ribu produk, padahal query itu HANYA salah satu dari BANYAK query lain yang
		// juga memfilter `WHERE toko = ?` (katalog Kasir/PriceTagUtil.listProduk, laporan,
		// dsb.) -- FK `produk.toko`/`pembelian.produk` TIDAK otomatis diindeks Postgres
		// (beda dari constraint-nya sendiri), jadi tiap query begini selama ini full-scan.
		// ---------------------------------------------------------------------------
		String[] INDEX_QUERIES_KOPERASI_PRODUK_DUPLIKAT_FAST = new String[] {
				"CREATE INDEX IF NOT EXISTS idx_kop_produk_toko ON koperasi.produk (toko, id)",
				"CREATE INDEX IF NOT EXISTS idx_kop_produk_toko_kode ON koperasi.produk (toko, kode)",
				"CREATE INDEX IF NOT EXISTS idx_kop_produk_toko_barcode ON koperasi.produk (toko, barcode)",
				"CREATE INDEX IF NOT EXISTS idx_kop_produk_toko_nama_norm ON koperasi.produk (toko, LOWER(TRIM(nama)))",
				"CREATE INDEX IF NOT EXISTS idx_kop_produk_toko_kode_barcode_nama ON koperasi.produk (toko, kode, barcode, LOWER(TRIM(nama)))",
				"CREATE INDEX IF NOT EXISTS idx_kop_pembelian_produk ON koperasi.pembelian (produk)" };
		for (String sql : INDEX_QUERIES_KOPERASI_PRODUK_DUPLIKAT_FAST) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:koperasi-produk-duplikat");
			}
		}
		String[] ANALYZE_KOPERASI_PRODUK_DUPLIKAT_FAST = new String[] { "ANALYZE koperasi.produk", "ANALYZE koperasi.pembelian" };
		for (String sql : ANALYZE_KOPERASI_PRODUK_DUPLIKAT_FAST) {
			try {
				eksekusiSql10Menit(sql);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitIndex.java:koperasi-produk-duplikat-analyze");
			}
		}

		// Relasi transaksi ke sesi kas membuat rekonsiliasi akurat, termasuk saat nama
		// kasir sama atau ada beberapa perangkat. Semua DDL idempoten untuk instalasi lama.
		String[] DDL_RELASI_TRANSAKSI_SESI_KAS = new String[] {
				"ALTER TABLE koperasi.sesi_kas_kasir ADD COLUMN IF NOT EXISTS id_perangkat varchar(128)",
				"ALTER TABLE koperasi.sesi_kas_kasir ADD COLUMN IF NOT EXISTS nama_perangkat varchar(150)",
				"ALTER TABLE koperasi.sesi_kas_kasir ADD COLUMN IF NOT EXISTS laporan_tutup_json text",
				"ALTER TABLE koperasi.pembelian_anggota_koperasi ALTER COLUMN keterangan TYPE text",
				"ALTER TABLE koperasi.pembelian_anggota_koperasi ADD COLUMN IF NOT EXISTS sesi_kas_kasir bigint",
				"ALTER TABLE koperasi.pembelian_anggota_koperasi ADD COLUMN IF NOT EXISTS id_perangkat varchar(128)",
				"DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_pak_sesi_kas') THEN ALTER TABLE koperasi.pembelian_anggota_koperasi ADD CONSTRAINT fk_pak_sesi_kas FOREIGN KEY (sesi_kas_kasir) REFERENCES koperasi.sesi_kas_kasir(id) ON DELETE SET NULL; END IF; END $$",
				"CREATE INDEX IF NOT EXISTS idx_pak_sesi_kas ON koperasi.pembelian_anggota_koperasi (sesi_kas_kasir, tanggal_pembayaran)",
				"CREATE INDEX IF NOT EXISTS idx_pak_perangkat_waktu ON koperasi.pembelian_anggota_koperasi (id_perangkat, tanggal_pembayaran DESC) WHERE id_perangkat IS NOT NULL",
				// Nama indeks/constraint generasi awal memakai aturan "aktif" yang tidak lagi
				// sama dengan status BUKA/TUTUP saat ini. Hapus keduanya sebelum memasang
				// indeks kanonik agar pemeriksaan aplikasi dan database selalu identik.
				"ALTER TABLE koperasi.sesi_kas_kasir DROP CONSTRAINT IF EXISTS uk_sesi_kas_satu_aktif_per_user",
				"DROP INDEX IF EXISTS koperasi.uk_sesi_kas_satu_aktif_per_user",
				"DROP INDEX IF EXISTS koperasi.uq_sesi_kas_akun_toko_buka",
				"DROP INDEX IF EXISTS koperasi.uq_sesi_kas_perangkat_toko_buka",
				"CREATE UNIQUE INDEX IF NOT EXISTS uq_sesi_kas_akun_buka ON koperasi.sesi_kas_kasir (COALESCE(kasir_user_id,kasir_nama)) WHERE status='BUKA' OR status IS NULL",
				"CREATE UNIQUE INDEX IF NOT EXISTS uq_sesi_kas_perangkat_buka ON koperasi.sesi_kas_kasir (id_perangkat) WHERE id_perangkat IS NOT NULL AND (status='BUKA' OR status IS NULL)" };
		for (String sql : DDL_RELASI_TRANSAKSI_SESI_KAS) {
			try { eksekusiSqlAmanDdl(sql); }
			catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "init relasi transaksi sesi kas"); }
		}

		// Alasan keranjang ditahan disimpan per toko sebagai JSON. Kolom TEXT
		// menjaga konfigurasi mudah ditambah tanpa membuat dua puluh kolom tetap.
		try {
			eksekusiSqlAmanDdl("ALTER TABLE koperasi.toko ADD COLUMN IF NOT EXISTS alasan_tahan_json text");
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "init alasan transaksi tahan");
		}
		// Kebijakan oversell harus terisolasi per toko. NULL dari instalasi lama diperlakukan
		// OFF oleh entity; DEFAULT false memastikan toko baru juga aman tanpa konfigurasi manual.
		try {
			eksekusiSqlAmanDdl("ALTER TABLE koperasi.toko ADD COLUMN IF NOT EXISTS boleh_transaksi_stok_habis boolean DEFAULT false");
			eksekusiSqlAmanDdl("UPDATE koperasi.toko SET boleh_transaksi_stok_habis=false WHERE boleh_transaksi_stok_habis IS NULL");
			eksekusiSqlAmanDdl("DO $$ BEGIN IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='new_audit' AND table_name='toko__audit') THEN ALTER TABLE new_audit.toko__audit ADD COLUMN IF NOT EXISTS boleh_transaksi_stok_habis boolean; END IF; END $$");
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "init kebijakan stok habis per toko");
		}
		// Gerbang eksplisit toko demo. Default false menjaga seluruh toko lama/produksi
		// tetap tidak pernah menampilkan maupun menjalankan provisioning data beban.
		try {
			eksekusiSqlAmanDdl("ALTER TABLE koperasi.toko ADD COLUMN IF NOT EXISTS toko_demo boolean DEFAULT false");
			eksekusiSqlAmanDdl("UPDATE koperasi.toko SET toko_demo=false WHERE toko_demo IS NULL");
			eksekusiSqlAmanDdl("DO $$ BEGIN IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='new_audit' AND table_name='toko__audit') THEN ALTER TABLE new_audit.toko__audit ADD COLUMN IF NOT EXISTS toko_demo boolean; END IF; END $$");
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "init penanda toko demo");
		}

		// Promo grup: satu header, banyak produk, snapshot JSON untuk audit, serta
		// kriteria multi jenis/tipe member dalam JSON. DDL sengaja idempoten agar
		// instalasi lama dan instalasi baru bergerak ke skema yang sama.
		String[] DDL_GRUP_ATURAN_DISKON = new String[] {
				"CREATE TABLE IF NOT EXISTS koperasi.grup_aturan_diskon (id bigserial PRIMARY KEY, nama_grup varchar(255) NOT NULL, keterangan text, toko bigint, jenis_anggota bigint, tipe_anggota bigint, berlaku_semua_member boolean DEFAULT true, khusus_member boolean DEFAULT false, jenis_member_json text, tipe_member_json text, persentase double precision DEFAULT 0, maksimal_potongan double precision DEFAULT 0, nominal double precision DEFAULT 0, cashback double precision DEFAULT 0, prioritas integer DEFAULT 100, dapat_digabung boolean DEFAULT false, dasar_perhitungan varchar(30) DEFAULT 'SETELAH_DISKON', grup_eksklusif varchar(100), potongan_langsung boolean DEFAULT true, tanggal_mulai timestamp, tanggal_selesai timestamp, hari_aktif varchar(20), aktif boolean DEFAULT true, detail_json text, oleh varchar(255), oleh_id varchar(255), tanggal_dirubah timestamp DEFAULT now())",
				"CREATE TABLE IF NOT EXISTS koperasi.grup_aturan_diskon_detail (id bigserial PRIMARY KEY, grup_aturan_diskon bigint NOT NULL, produk bigint NOT NULL, aktif boolean DEFAULT true, oleh varchar(255), oleh_id varchar(255), tanggal_dirubah timestamp DEFAULT now())",
				"ALTER TABLE koperasi.grup_aturan_diskon ADD COLUMN IF NOT EXISTS khusus_member boolean DEFAULT false",
				"ALTER TABLE koperasi.grup_aturan_diskon ADD COLUMN IF NOT EXISTS jenis_member_json text",
				"ALTER TABLE koperasi.grup_aturan_diskon ADD COLUMN IF NOT EXISTS tipe_member_json text",
				"ALTER TABLE koperasi.grup_aturan_diskon ADD COLUMN IF NOT EXISTS cashback double precision DEFAULT 0",
				"ALTER TABLE koperasi.grup_aturan_diskon ADD COLUMN IF NOT EXISTS prioritas integer DEFAULT 100",
				"ALTER TABLE koperasi.grup_aturan_diskon ADD COLUMN IF NOT EXISTS dapat_digabung boolean DEFAULT false",
				"ALTER TABLE koperasi.grup_aturan_diskon ADD COLUMN IF NOT EXISTS dasar_perhitungan varchar(30) DEFAULT 'SETELAH_DISKON'",
				"ALTER TABLE koperasi.grup_aturan_diskon ADD COLUMN IF NOT EXISTS grup_eksklusif varchar(100)",
				"ALTER TABLE koperasi.aturan_diskon ADD COLUMN IF NOT EXISTS prioritas integer DEFAULT 100",
				"ALTER TABLE koperasi.aturan_diskon ADD COLUMN IF NOT EXISTS dapat_digabung boolean DEFAULT false",
				"ALTER TABLE koperasi.aturan_diskon ADD COLUMN IF NOT EXISTS dasar_perhitungan varchar(30) DEFAULT 'SETELAH_DISKON'",
				"ALTER TABLE koperasi.aturan_diskon ADD COLUMN IF NOT EXISTS grup_eksklusif varchar(100)",
				"DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_grup_diskon_detail_header') THEN ALTER TABLE koperasi.grup_aturan_diskon_detail ADD CONSTRAINT fk_grup_diskon_detail_header FOREIGN KEY (grup_aturan_diskon) REFERENCES koperasi.grup_aturan_diskon(id) ON DELETE CASCADE; END IF; END $$",
				"DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_grup_diskon_detail_produk') THEN ALTER TABLE koperasi.grup_aturan_diskon_detail ADD CONSTRAINT fk_grup_diskon_detail_produk FOREIGN KEY (produk) REFERENCES koperasi.produk(id) ON DELETE CASCADE; END IF; END $$",
				"CREATE UNIQUE INDEX IF NOT EXISTS uq_grup_diskon_produk ON koperasi.grup_aturan_diskon_detail (grup_aturan_diskon, produk)",
				"CREATE INDEX IF NOT EXISTS idx_grup_diskon_aktif_toko_periode ON koperasi.grup_aturan_diskon (toko, tanggal_mulai, tanggal_selesai) WHERE aktif=true",
				"CREATE INDEX IF NOT EXISTS idx_grup_diskon_detail_produk_aktif ON koperasi.grup_aturan_diskon_detail (produk, grup_aturan_diskon) WHERE aktif=true" };
		for (String sql : DDL_GRUP_ATURAN_DISKON) {
			try { eksekusiSqlAmanDdl(sql); }
			catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "init grup aturan diskon"); }
		}

		} finally {
			// Tunggu SEMUA DDL paralel selesai, lalu tutup pool & reset state (idempoten,
			// aman bila dipanggil ulang). Eksekusi DB kembali sinkron di luar metode ini.
			tungguSemuaDdlSelesai();
			DDL_POOL = null;
			try {
				ddlPool.shutdown();
			} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/InitIndex.java:2753");
			}
			DDL_FUTURES.clear();
		}
	}
}

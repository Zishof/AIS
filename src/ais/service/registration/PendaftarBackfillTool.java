package ais.service.registration;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

/**
 * <h3>Tool backfill Pendaftar existing → {@code pendaftar_tenant_profile} (§16.4 dokumen master).</h3>
 *
 * <p>Dijalankan OPERATOR terhadap DB deployment (bukan otomatis saat startup):</p>
 * <pre>
 * java -cp &lt;classes&gt;;&lt;postgresql.jar&gt; ais.service.registration.PendaftarBackfillTool \
 *      jdbc:postgresql://host:5432/db user pass docs/pendaftaran-tenant/migration-exceptions.csv [apply]
 * </pre>
 *
 * <p>Aturan yang DISEPAKATI (docs/pendaftaran-tenant/09-migration.md):
 * (1) profile HANYA utk akun self-service TERBUKTI ({@code password_hash IS NOT NULL}) yang belum
 * ber-profile; (2) TIDAK mengarang jenis usaha -- kolom {@code jenis_bisnis} lama hanya
 * DIKLASIFIKASIKAN pada report (DIKENAL:kode katalog / LAINNYA_RAW:teks); (3) TIDAK membuat
 * tenant registry (butuh persetujuan pemilik produk); (4) duplikat email / email kosong-invalid
 * TIDAK dipaksa -- masuk report exception; (5) password tidak disentuh. Mode default DRY-RUN
 * (hanya report); argumen ke-5 {@code apply} menulis profile sungguhan (idempoten -- baris
 * existing dilewati).</p>
 */
public final class PendaftarBackfillTool {

	/** Kelas utilitas eksekusi baris perintah murni; konstruktor privat mencegah instansiasi. */
	private PendaftarBackfillTool() {
	}

	/**
	 * Titik masuk baris perintah tunggal tool backfill: memindai {@code public.pendaftar} existing
	 * dan membuat baris {@code pendaftar_tenant_profile} untuk akun self-service yang belum
	 * memilikinya (§16.4 dokumen master, aturan disepakati di
	 * {@code docs/pendaftaran-tenant/09-migration.md}), sekaligus menghasilkan laporan CSV berisi
	 * baris yang DILEWATI/butuh penanganan manual.
	 *
	 * <p>
	 * Argumen: {@code args[0]} URL JDBC, {@code args[1]} user DB, {@code args[2]} password DB,
	 * {@code args[3]} path file CSV keluaran, {@code args[4]} opsional literal {@code "apply"}
	 * (case-insensitive) untuk keluar dari mode default DRY-RUN. Kurang dari 4 argumen membuat
	 * tool mencetak pesan pemakaian dan keluar dengan {@code System.exit(2)} tanpa menyentuh
	 * database sama sekali.
	 * </p>
	 *
	 * <p>
	 * Alur kerja per baris {@code public.pendaftar} (diurutkan berdasar {@code id}), di dalam SATU
	 * transaksi ({@code setAutoCommit(false)}) untuk keseluruhan proses:
	 * </p>
	 * <ol>
	 * <li>Bila kolom {@code jenis_bisnis} lama terisi, diklasifikasikan (report saja, TIDAK pernah
	 * ditulis ke tabel mana pun): dicocokkan case-insensitive terhadap katalog
	 * {@code public.jenis_usaha_tenant.code} yang sudah dimuat lebih dulu ke memori; token
	 * pertama sebelum koma juga dicoba bila nilai gabungan tidak cocok. Cocok -&gt; dihitung
	 * {@code jenis_bisnis_dikenal}; tidak cocok -&gt; dicatat ke CSV sebagai
	 * {@code JENIS_BISNIS_LAINNYA} dengan keterangan {@code LAINNYA_RAW:<teks asli>} dan dihitung
	 * {@code jenis_bisnis_lainnya}.</li>
	 * <li>Baris yang {@code password_hash} kosong/{@code null} dianggap akun buatan staf (BUKAN
	 * self-service TERBUKTI) -&gt; dilewati sepenuhnya (aturan #1), dihitung
	 * {@code bukan_self_service}, TIDAK masuk CSV exception.</li>
	 * <li>Baris yang sudah punya {@code pendaftar_tenant_profile} (dicek lewat subquery
	 * {@code COUNT(*)} pada query utama) dilewati, dihitung {@code dilewati_sudah_ada} -- inilah
	 * yang membuat mode {@code apply} idempoten terhadap eksekusi berulang.</li>
	 * <li>Email kosong atau tidak memuat {@code '@'} pada posisi valid dicatat ke CSV sebagai
	 * {@code EMAIL_KOSONG_ATAU_INVALID} (aturan #4), dihitung {@code exception}.</li>
	 * <li>Email yang sudah dipakai oleh profile lain (dicek terhadap set yang dimuat dari
	 * {@code pendaftar_tenant_profile} DITAMBAH email yang baru saja "dipakai" dalam batch berjalan
	 * ini, sehingga duplikat ANTAR baris pendaftar dalam satu eksekusi juga tertangkap) dicatat
	 * sebagai {@code EMAIL_DUPLIKAT}, dihitung {@code exception}. Baris ini TIDAK pernah dipaksa
	 * dibuatkan profile (aturan #4).</li>
	 * <li>Baris yang lolos semua pengecualian di atas dihitung {@code profile_dibuat}; bila mode
	 * {@code apply} aktif, dieksekusi {@code INSERT} ke {@code pendaftar_tenant_profile} dengan
	 * {@code account_status='ACTIVE'}, {@code registration_source='BACKFILL'}, algoritma password
	 * PBKDF2WithHmacSHA256/120000 iterasi TANPA MENGUBAH hash password existing sama sekali
	 * (aturan #5 -- kolom password sendiri tidak disentuh, hanya baris profile pendamping yang
	 * dibuat) dan {@code must_change_password=false}. Tool ini juga TIDAK PERNAH membuat baris
	 * tenant registry (aturan #3 -- perlu persetujuan terpisah dari pemilik produk).</li>
	 * </ol>
	 *
	 * <p>
	 * Di akhir: mode {@code apply} melakukan {@code conn.commit()}; mode DRY-RUN (default) selalu
	 * {@code conn.rollback()} sehingga TIDAK ADA perubahan data tersimpan walau statement
	 * {@code INSERT} sempat disiapkan/dieksekusi di working transaksi. File CSV dan koneksi selalu
	 * ditutup lewat {@code finally}. Ringkasan angka ({@code MODE}, {@code profile_dibuat},
	 * {@code dilewati_sudah_ada}, {@code bukan_self_service}, {@code exception},
	 * {@code jenis_bisnis_dikenal}, {@code jenis_bisnis_lainnya}, {@code csv}) dicetak ke
	 * {@code System.out} sebagai baris {@code key=value} agar mudah di-parse operator/skrip CI.
	 * </p>
	 *
	 * @param args argumen baris perintah: {@code [jdbcUrl, user, pass, csvKeluaran, apply?]}
	 * @throws Exception diteruskan apa adanya dari kegagalan driver JDBC, koneksi database, atau
	 *                    I/O penulisan file CSV -- tool ini sengaja tidak menangkap galat tak
	 *                    terduga secara halus karena dijalankan manual oleh operator yang akan
	 *                    melihat stack trace langsung di konsol
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			System.out.println("Pemakaian: PendaftarBackfillTool <jdbcUrl> <user> <pass> <csvKeluaran> [apply]");
			System.exit(2);
		}
		String jdbcUrl = args[0];
		String user = args[1];
		String pass = args[2];
		String csvPath = args[3];
		boolean apply = args.length > 4 && "apply".equalsIgnoreCase(args[4]);

		Class.forName("org.postgresql.Driver");
		Connection conn = DriverManager.getConnection(jdbcUrl, user, pass);
		PrintWriter csv = new PrintWriter(new OutputStreamWriter(new FileOutputStream(csvPath), "UTF-8"));
		int dibuat = 0, dilewatiSudahAda = 0, bukanSelfService = 0, exceptionBaris = 0, klasifikasiDikenal = 0,
				klasifikasiLainnya = 0;
		try {
			conn.setAutoCommit(false);
			csv.println("pendaftar_id,nama,email,jenis_kasus,keterangan");

			// Katalog kode jenis usaha utk klasifikasi jenis_bisnis lama.
			Set<String> kodeKatalog = new HashSet<String>();
			PreparedStatement psKatalog = conn.prepareStatement("SELECT code FROM public.jenis_usaha_tenant");
			ResultSet rsKatalog = psKatalog.executeQuery();
			while (rsKatalog.next()) {
				kodeKatalog.add(rsKatalog.getString(1).toUpperCase());
			}
			rsKatalog.close();
			psKatalog.close();

			// Email yang sudah ber-profile (normalized) + penghitung duplikat dalam batch ini.
			Set<String> emailTerpakai = new HashSet<String>();
			PreparedStatement psProfil = conn
					.prepareStatement("SELECT normalized_email FROM public.pendaftar_tenant_profile");
			ResultSet rsProfil = psProfil.executeQuery();
			while (rsProfil.next()) {
				emailTerpakai.add(rsProfil.getString(1));
			}
			rsProfil.close();
			psProfil.close();

			PreparedStatement psInsert = conn.prepareStatement(
					"INSERT INTO public.pendaftar_tenant_profile (pendaftar_id, normalized_email, "
							+ "account_status, registration_source, password_algorithm, password_version, "
							+ "password_iterations, must_change_password, created_at, version, oleh, olehid) "
							+ "VALUES (?, ?, 'ACTIVE', 'BACKFILL', 'PBKDF2WithHmacSHA256', 1, 120000, false, "
							+ "now(), 0, 'backfill', 'backfill')");

			PreparedStatement ps = conn.prepareStatement(
					"SELECT p.id, p.nama, p.email, p.password_hash, p.jenis_bisnis, "
							+ "(SELECT COUNT(*) FROM public.pendaftar_tenant_profile f WHERE f.pendaftar_id = p.id) "
							+ "FROM public.pendaftar p ORDER BY p.id");
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				long id = rs.getLong(1);
				String nama = aman(rs.getString(2));
				String email = rs.getString(3) == null ? "" : rs.getString(3).trim().toLowerCase();
				boolean selfService = rs.getString(4) != null && rs.getString(4).trim().length() > 0;
				String jenisBisnis = rs.getString(5) == null ? "" : rs.getString(5).trim();
				boolean sudahAdaProfil = rs.getLong(6) > 0;

				// Klasifikasi jenis_bisnis lama (report saja, TIDAK menulis data).
				if (jenisBisnis.length() > 0) {
					String kandidat = jenisBisnis.toUpperCase().replace(' ', '_');
					boolean dikenal = kodeKatalog.contains(kandidat);
					if (!dikenal && kandidat.indexOf(',') >= 0) {
						dikenal = kodeKatalog.contains(kandidat.split(",")[0].trim());
					}
					if (dikenal) {
						klasifikasiDikenal++;
					} else {
						klasifikasiLainnya++;
						csv.println(id + "," + aman(nama) + "," + aman(email) + ",JENIS_BISNIS_LAINNYA,"
								+ aman("LAINNYA_RAW:" + jenisBisnis));
					}
				}

				if (!selfService) {
					bukanSelfService++;
					continue; // akun buatan staf: TIDAK di-backfill (aturan #1)
				}
				if (sudahAdaProfil) {
					dilewatiSudahAda++;
					continue;
				}
				if (email.length() == 0 || email.indexOf('@') <= 0) {
					exceptionBaris++;
					csv.println(id + "," + aman(nama) + "," + aman(email) + ",EMAIL_KOSONG_ATAU_INVALID,"
							+ "self-service tanpa email valid -- perlu penanganan manual");
					continue;
				}
				if (emailTerpakai.contains(email)) {
					exceptionBaris++;
					csv.println(id + "," + aman(nama) + "," + aman(email) + ",EMAIL_DUPLIKAT,"
							+ "email sudah dipakai profile/pendaftar lain -- perlu penanganan manual");
					continue;
				}
				emailTerpakai.add(email);
				if (apply) {
					psInsert.setLong(1, id);
					psInsert.setString(2, email);
					psInsert.executeUpdate();
				}
				dibuat++;
			}
			rs.close();
			ps.close();
			psInsert.close();

			if (apply) {
				conn.commit();
			} else {
				conn.rollback();
			}
		} finally {
			csv.close();
			conn.close();
		}

		System.out.println("MODE=" + (apply ? "APPLY" : "DRY-RUN"));
		System.out.println("profile_dibuat=" + dibuat);
		System.out.println("dilewati_sudah_ada=" + dilewatiSudahAda);
		System.out.println("bukan_self_service=" + bukanSelfService);
		System.out.println("exception=" + exceptionBaris);
		System.out.println("jenis_bisnis_dikenal=" + klasifikasiDikenal);
		System.out.println("jenis_bisnis_lainnya=" + klasifikasiLainnya);
		System.out.println("csv=" + csvPath);
	}

	/** Escape CSV minimal: koma/kutip dibungkus kutip ganda. */
	private static String aman(String s) {
		if (s == null) {
			return "";
		}
		if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0) {
			return "\"" + s.replace("\"", "\"\"") + "\"";
		}
		return s;
	}
}

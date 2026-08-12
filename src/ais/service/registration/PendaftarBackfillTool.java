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

	private PendaftarBackfillTool() {
	}

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

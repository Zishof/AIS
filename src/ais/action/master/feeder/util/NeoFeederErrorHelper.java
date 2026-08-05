package ais.action.master.feeder.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

/**
 * {@code NeoFeederErrorHelper} — utilitas bersama untuk (1) <b>membersihkan/memotong</b> nilai field
 * yang melebihi batas kolom Neo Feeder sebelum record dikirim, dan (2) menghasilkan <b>diagnosa
 * error yang rinci &amp; dapat ditindaklanjuti</b> ketika server Feeder menolak sebuah record.
 *
 * <h3>Latar belakang</h3>
 * Neo Feeder sering mengembalikan pesan penolakan yang sangat generik, mis. <i>"Ada kesalahan pada
 * JSON yang dikirim"</i>, tanpa menyebut field mana yang bermasalah. Bagi operator hal ini
 * membingungkan. Kelas ini menutup celah tersebut dengan menganalisa record yang dikirim dan
 * menerjemahkan pesan generik menjadi kemungkinan penyebab konkret: field terlalu panjang, field
 * wajib yang kosong, referensi/ID yang belum terdaftar, atau data duplikat — lengkap dengan saran.
 *
 * <h3>Dipakai di titik pusat</h3>
 * Kedua fungsi dipanggil dari {@code FeederConnector.insertOrUpdateRecordBaru} — satu-satunya jalur
 * tulis JSON ke Feeder (InsertUpdate*). Dengan begitu SEMUA operasi kirim (aktivitas mahasiswa,
 * mata kuliah, kurikulum, dsb.) otomatis mendapat pemotongan preventif + diagnosa tanpa duplikasi.
 *
 * <h3>Pemotongan preventif ({@link #sanitasiRecord(JSONObject)})</h3>
 * Hanya field teks yang namanya dikenal (mis. {@code judul}, {@code keterangan}, {@code lokasi},
 * {@code sk_tugas}) yang dipotong, dengan batas <b>konservatif</b> (aman — hanya memangkas
 * kelebihan yang jelas berlebihan, bukan data normal). ID, tanggal, dan angka TIDAK pernah disentuh.
 * Setiap pemotongan dikembalikan sebagai catatan agar transparan (tidak ada data hilang diam-diam).
 *
 * <h3>Kompatibilitas</h3>
 * Java 1.7.
 *
 * @author Tim AIS
 */
public class NeoFeederErrorHelper {

	/** Kelas utilitas statis — tidak untuk diinstansiasi. */
	private NeoFeederErrorHelper() {
	}

	/**
	 * Pesan standar &amp; <b>actionable</b> untuk kondisi "sebuah data belum terdaftar di Feeder"
	 * (belum punya kode Feeder: {@code id_registrasi}, {@code id_dosen}, {@code id_prodi}, dll.).
	 * Dipakai bersama oleh seluruh proses kirim agar operator langsung tahu apa yang harus dilakukan,
	 * bukan sekadar "gagal".
	 *
	 * @param jenis jenis data, mis. {@code "Mahasiswa"}, {@code "Dosen pembimbing"}, {@code "Program Studi"}
	 * @param nama  identitas data, mis. {@code "225090036 - Ridho Alfian"}
	 * @param aksi  tindakan yang harus dilakukan operator (kalimat imperatif, tanpa titik akhir)
	 * @return pesan siap tampil/di-log
	 */
	public static String belumTerdaftar(String jenis, String nama, String aksi) {
		return "[BELUM TERDAFTAR DI FEEDER] " + jenis + ": " + (nama == null || nama.trim().isEmpty() ? "-" : nama)
				+ ". Penyebab: data ini belum memiliki kode Feeder (belum pernah dikirim/sinkron). Tindakan: "
				+ (aksi == null || aksi.trim().isEmpty() ? "kirim/sinkronkan data induk ini ke Feeder lebih dulu" : aksi)
				+ ".";
	}

	/**
	 * Batas panjang (konservatif) untuk field teks Neo Feeder yang namanya dikenal. Nilai dipilih
	 * cukup longgar agar tidak memangkas data normal, namun tetap mencegah kelebihan ekstrem yang
	 * pasti ditolak Feeder. Sesuaikan bila skema Feeder Anda berbeda.
	 */
	private static final Map<String, Integer> BATAS = new HashMap<String, Integer>();
	static {
		BATAS.put("judul", 500);
		BATAS.put("keterangan", 2000);
		BATAS.put("lokasi", 500);
		BATAS.put("sk_tugas", 255);
		BATAS.put("nama_kegiatan", 500);
		BATAS.put("bahasan", 200);
	}

	/**
	 * Memotong (in-place) field teks {@code record} yang melebihi batas kolom Feeder yang dikenal.
	 * Hanya field bernama jelas yang disentuh; ID/angka/tanggal diabaikan.
	 *
	 * @param record record yang akan dikirim (boleh {@code null})
	 * @return catatan pemotongan yang siap tampil ({@code ""} bila tak ada yang dipotong)
	 */
	public static String sanitasiRecord(JSONObject record) {
		if (record == null) {
			return "";
		}
		StringBuilder note = new StringBuilder();
		for (Map.Entry<String, Integer> e : BATAS.entrySet()) {
			String key = e.getKey();
			int max = e.getValue().intValue();
			if (record.isNull(key)) {
				continue;
			}
			Object v = record.opt(key);
			if (!(v instanceof String)) {
				continue;
			}
			String s = (String) v;
			if (s.length() > max) {
				try {
					record.put(key, s.substring(0, max));
				} catch (Exception ex) {
					continue;
				}
				if (note.length() > 0) {
					note.append("; ");
				}
				note.append(key).append(" ").append(s.length()).append("->").append(max).append(" kar");
			}
		}
		if (note.length() == 0) {
			return "";
		}
		return "Catatan: teks terlalu panjang dipotong otomatis sebelum dikirim (" + note.toString() + ").";
	}

	/**
	 * Menghasilkan diagnosa rinci atas penolakan Feeder: menerjemahkan pesan generik menjadi
	 * kemungkinan penyebab konkret berdasarkan isi {@code record}, plus saran tindak lanjut.
	 *
	 * @param act       nama fungsi Feeder (mis. {@code "InsertAktivitasMahasiswa"})
	 * @param record    record yang dikirim (boleh {@code null})
	 * @param errorDesc pesan {@code error_desc} dari server Feeder
	 * @return teks diagnosa multi-baris yang siap ditampilkan/di-log
	 */
	public static String diagnosa(String act, JSONObject record, String errorDesc) {
		String ed = (errorDesc == null) ? "" : errorDesc.trim();
		String edl = ed.toLowerCase();

		StringBuilder sb = new StringBuilder();
		sb.append(">>> DIAGNOSA OTOMATIS <<<\n");
		sb.append("Fungsi   : ").append(act == null ? "-" : act).append("\n");
		sb.append("Pesan    : ").append(ed.isEmpty() ? "(kosong)" : ed).append("\n");

		boolean tipeJson = edl.indexOf("json") >= 0;
		boolean tipeDup = edl.indexOf("sudah ada") >= 0 || edl.indexOf("duplicate") >= 0
				|| edl.indexOf("unik") >= 0 || edl.indexOf("unique") >= 0
				|| edl.indexOf("sudah digunakan") >= 0 || edl.indexOf("sudah terdaftar") >= 0
				|| edl.indexOf("sudah dipakai") >= 0 || edl.indexOf("telah digunakan") >= 0
				|| edl.indexOf("telah terdaftar") >= 0;
		boolean tipeRef = edl.indexOf("tidak ditemukan") >= 0 || edl.indexOf("tidak terdaftar") >= 0
				|| edl.indexOf("tidak valid") >= 0 || edl.indexOf("not found") >= 0 || edl.indexOf("invalid") >= 0;
		boolean tipePanjang = edl.indexOf("panjang") >= 0 || edl.indexOf("melebihi") >= 0
				|| edl.indexOf("too long") >= 0 || edl.indexOf("value too long") >= 0;
		boolean tipeToken = edl.indexOf("token") >= 0 || edl.indexOf("otentikasi") >= 0
				|| edl.indexOf("login") >= 0;
		// FIX "user lebih faham apa yang kurang": pesan Feeder paling umum ("<field> tidak boleh
		// kosong", "<field> wajib diisi", "<field> harus diisi") sebelumnya TIDAK terklasifikasi
		// sama sekali -- baris "Arti" kosong sehingga operator tidak tahu field mana yang kurang.
		String frasaKosong = null;
		if (edl.indexOf("tidak boleh kosong") >= 0) {
			frasaKosong = "tidak boleh kosong";
		} else if (edl.indexOf("wajib diisi") >= 0) {
			frasaKosong = "wajib diisi";
		} else if (edl.indexOf("harus diisi") >= 0) {
			frasaKosong = "harus diisi";
		} else if (edl.indexOf("tidak boleh null") >= 0) {
			frasaKosong = "tidak boleh null";
		} else if (edl.indexOf("must not be empty") >= 0) {
			frasaKosong = "must not be empty";
		} else if (edl.indexOf("required") >= 0) {
			frasaKosong = "required";
		}
		boolean tipeKosong = frasaKosong != null;
		String fieldKosong = null;
		if (tipeKosong) {
			int idx = edl.indexOf(frasaKosong);
			fieldKosong = ed.substring(0, idx).trim();
			// Feeder biasanya menyebut nama field mentah (mis. "nisn", "id_mahasiswa") di awal pesan;
			// buang kata pengantar umum bila ada supaya nama field yang tersisa bersih.
			fieldKosong = fieldKosong.replaceAll("(?i)^(field|kolom|parameter)\\s+", "").trim();
		}

		if (tipeToken) {
			sb.append("Arti     : Masalah TOKEN/otentikasi. Uji ulang koneksi & login Feeder pada Pengaturan Koneksi.\n");
		} else if (tipeKosong) {
			sb.append("Arti     : FIELD WAJIB KOSONG — Neo Feeder mensyaratkan field")
					.append(fieldKosong == null || fieldKosong.isEmpty() ? " ini" : " \"" + fieldKosong + "\"")
					.append(" diisi, tetapi nilainya kosong/tidak ada pada data yang dikirim. ")
					.append("Lengkapi data field tersebut pada menu Biodata/Data terkait, lalu kirim ulang ke Feeder.\n");
		} else if (tipeDup) {
			sb.append("Arti     : Data DUPLIKAT — nilai (mis. NIK/NISN/NPWP) atau kunci record ini sudah "
					+ "digunakan/terdaftar pada data LAIN di Feeder. Periksa kembali apakah nilainya salah "
					+ "ketik/tertukar dengan mahasiswa lain, atau data ini memang sudah pernah tersinkron "
					+ "(gunakan Update, atau anggap sudah tersinkron).\n");
		} else if (tipePanjang) {
			sb.append("Arti     : DATA TERLALU PANJANG untuk kolom Feeder. Field terpanjang di bawah dipotong "
					+ "otomatis pada pengiriman berikutnya.\n");
		} else if (tipeRef) {
			sb.append("Arti     : REFERENSI tidak valid — ada ID (id_prodi/id_semester/id_matkul/id_dosen/"
					+ "id_jenis_*) yang belum terdaftar di Feeder. Sinkronkan data induk lebih dulu.\n");
		} else if (tipeJson) {
			sb.append("Arti     : Feeder menolak isi record (pesan generik). Umumnya salah satu field: "
					+ "(a) melebihi batas panjang kolom, (b) nilai/tipe tidak valid, (c) field tak dikenal untuk "
					+ "fungsi ini, atau (d) referensi ID tidak terdaftar.\n");
		}

		if (record != null) {
			List<String[]> fields = new ArrayList<String[]>();
			List<String> kosong = new ArrayList<String>();
			List<String> refs = new ArrayList<String>();

			Iterator<?> it = record.keys();
			while (it.hasNext()) {
				String k = String.valueOf(it.next());
				Object vo = record.isNull(k) ? null : record.opt(k);
				String v = (vo == null) ? "" : String.valueOf(vo);
				fields.add(new String[] { k, v, String.valueOf(v.length()) });
				if (v.trim().length() == 0) {
					kosong.add(k);
				}
				if (k.startsWith("id_") || k.equals("kode") || k.endsWith("_id")) {
					refs.add(k + "=" + preview(v, 40));
				}
			}

			Collections.sort(fields, new Comparator<String[]>() {
				@Override
				public int compare(String[] a, String[] b) {
					return Integer.parseInt(b[2]) - Integer.parseInt(a[2]);
				}
			});

			// Titik terang untuk operator: tunjuk LANGSUNG field yang disebut pesan Feeder sebagai
			// wajib-tapi-kosong, dan bedakan "ada tapi kosong" vs "tidak disertakan sama sekali dalam
			// data yang dikirim" (kasus terakhir sering terjadi bila field sumbernya null di Java --
			// org.json otomatis membuang key ber-nilai null saat JSONObject.put dipanggil).
			if (tipeKosong && fieldKosong != null && !fieldKosong.isEmpty()) {
				boolean ditemukan = false;
				for (int i = 0; i < fields.size(); i++) {
					String[] f = fields.get(i);
					if (f[0].equalsIgnoreCase(fieldKosong)) {
						ditemukan = true;
						sb.append("=> Field \"").append(fieldKosong).append("\" ADA di data tapi nilainya KOSONG.\n");
						break;
					}
				}
				if (!ditemukan) {
					sb.append("=> Field \"").append(fieldKosong)
							.append("\" TIDAK DISERTAKAN SAMA SEKALI dalam data yang dikirim (kemungkinan nilainya ")
							.append("null di aplikasi sebelum dikirim -- cek data sumbernya).\n");
				}
			}

			sb.append("Field (urut terpanjang):\n");
			boolean adaPanjang = false;
			int shown = 0;
			for (int i = 0; i < fields.size(); i++) {
				String[] f = fields.get(i);
				int len = Integer.parseInt(f[2]);
				Integer batas = BATAS.get(f[0]);
				String flag = "";
				if (batas != null && len > batas.intValue()) {
					flag = "  <-- MELEBIHI batas ~" + batas + ", akan dipotong";
					adaPanjang = true;
				} else if (batas == null && len > 250) {
					flag = "  <-- cukup panjang, cek batas kolom Feeder";
					adaPanjang = true;
				}
				sb.append("  - ").append(f[0]).append(" (").append(len).append(" kar): \"")
						.append(preview(f[1], 50)).append("\"").append(flag).append("\n");
				if (++shown >= 8) {
					break;
				}
			}
			if (!adaPanjang && tipeJson) {
				sb.append("  (Panjang field tampak WAJAR — panjang kemungkinan BUKAN penyebab; "
						+ "fokus ke referensi/field tak dikenal.)\n");
			}
			if (!kosong.isEmpty()) {
				sb.append("Field KOSONG (cek apakah wajib diisi): ").append(gabung(kosong)).append("\n");
			}
			if (!refs.isEmpty()) {
				sb.append("Referensi/ID (harus terdaftar di Feeder): ").append(gabung(refs)).append("\n");
			}
		}

		sb.append("Saran    :\n");
		sb.append("  1) Pastikan semua id_* valid & sudah tersinkron di Feeder (kirim data induk lebih dulu).\n");
		sb.append("  2) Teks yang terlalu panjang sudah/akan dipotong otomatis; isi field wajib yang kosong bila perlu.\n");
		sb.append("  3) Bila pesan 'sudah ada', gunakan Update atau anggap sudah tersinkron.\n");
		sb.append("  4) Bila tetap gagal, cocokkan field record dengan kamus fungsi Feeder (menu 'Cek Versi API').\n");
		return sb.toString();
	}

	/** Potong teks untuk pratinjau (maksimal {@code max} karakter + elipsis). */
	private static String preview(String s, int max) {
		if (s == null) {
			return "";
		}
		String t = s.replace("\n", " ").replace("\r", " ");
		return t.length() <= max ? t : t.substring(0, max) + "...";
	}

	/** Gabung daftar string dengan koma. */
	private static String gabung(List<String> list) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(list.get(i));
		}
		return sb.toString();
	}
}

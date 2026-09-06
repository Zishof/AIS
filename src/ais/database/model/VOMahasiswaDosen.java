package ais.database.model;

import java.util.TreeMap;

import org.zkoss.zul.Label;

/**
 * Value object kontrak bersama untuk entitas yang merepresentasikan pihak dalam relasi
 * pembelajaran mahasiswa-dosen (mis. mahasiswa peserta dan dosen pengampu suatu mata kuliah),
 * bukan entitas Hibernate — hanya antarmuka yang menyeragamkan cara mengambil identitas
 * ({@link #ambilKode()}/{@link #getNama()}) dan materi pertemuan terkait.
 *
 * <h3>Siapa saja yang mengimplementasikannya</h3>
 * <p>Empat entity dari empat cabang hierarki yang sama sekali berbeda:</p>
 * <ul>
 * <li>{@link Mahasiswa} — {@code extends VOMahasiswa} (jalur value object mahasiswa);</li>
 * <li>{@link Dosen} — {@code extends Karyawan} (jalur kepegawaian);</li>
 * <li>{@code ais.database.model.sekolah.Siswa} — {@code extends VOSiswa} (jalur persekolahan);</li>
 * <li>{@code ais.database.model.sekolah.Guru} — {@code extends GeneralValueObject} langsung.</li>
 * </ul>
 * <p>Justru karena keempatnya tidak punya kelas induk bersama selain
 * {@link GeneralValueObject}, antarmuka inilah satu-satunya tempat kontrak bersama mereka dapat
 * dinyatakan. Kode linimasa/e-learning ({@code TampilanELearningAction},
 * {@code FormulirKegiatanPesertaHelper}, {@code ElearningApiUtil}) memakai tipe ini untuk
 * memperlakukan pengajar dan peserta secara seragam tanpa peduli apakah konteksnya perguruan
 * tinggi (mahasiswa/dosen) atau sekolah (siswa/guru).</p>
 *
 * <h3>Yang sengaja TIDAK dijanjikan</h3>
 * <p>Antarmuka ini tidak menjanjikan identitas ({@code equals}/{@code hashCode}), tidak menjanjikan
 * penyaringan kepemilikan/tenant, dan tidak menjanjikan bahwa pemanggilnya sudah berwenang melihat
 * materi yang dikembalikan {@link #ambilMateri(TreeMap, boolean, Label)}. Otorisasi tetap menjadi
 * tanggung jawab Action/service yang memanggil, bukan implementasi antarmuka ini. Karena satu
 * daftar bisa memuat campuran keempat tipe implementor, jangan pernah menyimpulkan hak akses dari
 * tipe elemen daftar.</p>
 *
 * @see Mahasiswa
 * @see Dosen
 */
public interface VOMahasiswaDosen {
	/**
	 * Mengambil kode identitas pihak ini (mis. NIM mahasiswa atau NIDN/kode dosen).
	 *
	 * <p>Kode yang dikembalikan bersifat identitas <b>domain</b>, bukan kunci primer basis data:
	 * setiap implementor memetakannya ke kolom yang berbeda (NIM untuk mahasiswa, NIS untuk siswa,
	 * kode/NIDN untuk dosen dan guru). Karena itu dua objek dari implementor berbeda dapat
	 * mengembalikan kode yang sama tanpa berarti keduanya pihak yang sama. Bila kode dipakai
	 * sebagai kunci pada {@link java.util.Map} yang mencampur peserta dan pengajar, sertakan
	 * pembeda tipe pada kuncinya.</p>
	 *
	 * <p>Nilai balik boleh {@code null} atau kosong pada data yang belum lengkap; jangan
	 * memanggil method {@link String} apa pun atas hasilnya tanpa pemeriksaan.</p>
	 *
	 * @return kode identitas domain pihak ini; bisa {@code null}
	 */
	public String ambilKode();

	/**
	 * Mengambil nama tampil pihak ini.
	 *
	 * <p>Dinamai mengikuti konvensi JavaBean ({@code getNama}) — bukan {@code ambilNama} seperti
	 * dua method lain di antarmuka ini — justru supaya komponen ZK dan mesin laporan dapat
	 * mengikatnya sebagai properti {@code "nama"} tanpa adaptor. Konsekuensinya, pada implementor
	 * yang sudah punya field nama sendiri dengan nama kolom berbeda (mis. {@code namaGuru} pada
	 * {@code Guru}), method ini menjadi salinan hanya-baca yang meneruskan ke field aslinya, dan
	 * pada implementor lain ia justru merupakan getter properti terpetakan Hibernate. Jadi
	 * memanggilnya bisa berarti sekadar membaca field, atau bisa memicu inisialisasi proxy lazy —
	 * bergantung implementor.</p>
	 *
	 * <p>Nilai balik boleh {@code null} bila nama belum terisi; pemanggil yang menampilkannya di
	 * grid harus menyiapkan nilai pengganti sendiri.</p>
	 *
	 * @return nama tampil pihak ini; bisa {@code null}
	 */
	public String getNama();

	/**
	 * Mengambil peta materi per pertemuan untuk keperluan tampilan (mis. rekap kehadiran/materi
	 * kuliah), dengan {@code pertemuans} sebagai daftar pertemuan yang diminta, {@code refresh}
	 * untuk memaksa pengambilan ulang dari sumber data (melewati cache bila ada), dan
	 * {@code label} komponen ZK yang mungkin diperbarui langsung (mis. indikator progres) selama
	 * proses pengambilan.
	 *
	 * <p><b>Bentuk hasil.</b> Kunci peta adalah kunci pertemuan yang sama dengan kunci
	 * {@code pertemuans} yang dikirim masuk, sehingga pemanggil dapat menyandingkan baris hasil
	 * dengan baris permintaannya. Nilainya berupa {@code Object[]} — larik posisional yang isinya
	 * ditentukan oleh implementor, bukan oleh antarmuka ini. Tidak ada konstanta indeks di sini,
	 * jadi setiap pembacaan hasil di sisi pemanggil terikat pada implementor tertentu; menambah
	 * kolom di satu implementor tanpa menyesuaikan implementor lain akan lolos kompilasi dan baru
	 * gagal saat dijalankan.</p>
	 *
	 * <p><b>Ini bukan getter murni.</b> Kontrak method ini secara eksplisit mengizinkan efek
	 * samping: mengakses berkas materi di penyimpanan, membuka session/kueri basis data untuk
	 * pertemuan yang belum ada di cache, menulis ulang berkas cache, dan memperbarui komponen ZK
	 * {@code label} di tengah proses. Karena komponen ZK hanya boleh disentuh dari thread yang
	 * memegang desktop-nya, jangan memanggil method ini dari thread latar tanpa mekanisme
	 * pembaruan asinkron ZK yang sesuai.</p>
	 *
	 * <p><b>Otorisasi.</b> Antarmuka ini tidak menjanjikan penyaringan hak akses apa pun terhadap
	 * materi yang dikembalikan. Pemanggil bertanggung jawab memastikan pengguna yang sedang
	 * berjalan memang berhak melihat pertemuan yang diminta sebelum memanggil.</p>
	 *
	 * @param pertemuans daftar pertemuan yang diminta, dipetakan dari kunci tampilan ke id
	 *                   pertemuan; kunci inilah yang muncul kembali pada peta hasil
	 * @param refresh    {@code true} untuk memaksa pengambilan ulang dari sumber data dan melewati
	 *                   cache berkas; {@code false} untuk memakai cache bila tersedia
	 * @param label      komponen ZK opsional yang diperbarui sebagai indikator progres; boleh
	 *                   {@code null} bila pemanggil tidak menampilkan progres
	 * @return peta materi per pertemuan; implementor umumnya mengembalikan peta kosong, bukan
	 *         {@code null}, bila tidak ada materi
	 */
	public TreeMap<String, Object[]> ambilMateri(TreeMap<String, Long> pertemuans, boolean refresh, Label label);
}

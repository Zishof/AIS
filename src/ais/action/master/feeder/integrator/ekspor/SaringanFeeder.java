package ais.action.master.feeder.integrator.ekspor;

import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Kurikulum;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Program;
import ais.database.model.StatusMahasiswa;

/**
 * Nilai saringan layar penyiapan berkas Feeder, sebagai data biasa.
 *
 * <p>Satu tipe untuk seluruh panel, bukan satu per panel. Survei atas kedua
 * puluh tiga panel menunjukkan permukaan saringannya berulang: sebagian besar
 * memakai kombinasi dari fakultas/jurusan/program, tahun akademik, semester,
 * dan penyaring mahasiswa. Tipe terpisah per panel hanya akan menghasilkan dua
 * puluh tiga kelas yang saling menyalin, sedangkan pemanggilnya — controller
 * native maupun panel ZK — harus menyusunnya dengan cara yang sama juga.</p>
 *
 * <p>Tiap panel memakai field yang relevan baginya dan mengabaikan sisanya;
 * field yang tidak diisi berarti "tanpa saringan", persis seperti kotak pilihan
 * yang dibiarkan kosong pada layar lama.</p>
 */
public final class SaringanFeeder {

    /** Kelas perkuliahan/rombel; kosong berarti seluruhnya. */
    public String kelas = "";
    /** Penggalan NIM. */
    public String nim = "";
    /** Penggalan nama. */
    public String nama = "";

    public Jurusan jurusan;
    public Fakultas fakultas;
    public Program program;
    /** Tahun angkatan mahasiswa. */
    public Integer angkatan;
    public StatusMahasiswa status;
    public MasaPerkuliahan masaPerkuliahan;
    /** Kurikulum, pada layar yang menyaring menurutnya. */
    public Kurikulum kurikulum;

    /** Tahun akademik, mis. {@code 2026/2027}. */
    public String tahunAkademik = "";
    /**
     * Jenis semester sebagaimana disimpan combobox layar (teks).
     *
     * <p>Beda dengan {@link #semesterKe}. Panel-panel lain memakai combobox
     * {@code searchsemester} untuk memilih Ganjil/Genap, sedangkan panel KRS dan
     * Nilai memakai combobox bernama sama untuk memilih semester ke berapa.
     * Nama widget-nya kebetulan sama; artinya tidak. Menyatukan keduanya dalam
     * satu field berarti nilai "3" dan "Ganjil" bersaing di tempat yang sama.</p>
     */
    public String semester = "";
    /** Semester ke berapa (1..14), pada panel KRS dan Nilai. */
    public Integer semesterKe;
    /** Tahun ajaran pada layar yang memisahkannya dari tahun akademik. */
    public String tahunAjaran = "";
    /** Penggalan kode mata kuliah, pada layar KRS/Nilai. */
    public String kodeMatakuliah = "";
    /**
     * Batas pengambilan pada layar AKM: baris ke-{@code mulai} sebanyak
     * {@code sampai}. Namanya mengikuti kotak isian layar lama supaya operator
     * yang terbiasa dengannya tidak perlu menebak artinya berubah.
     */
    public Integer mulai;
    public Integer sampai;

    /** Layar Nilai: hanya yang sudah dinilai. */
    public boolean telahDinilai;
    /** Layar Nilai: hanya yang belum dinilai. */
    public boolean belumDinilai;
    /** Layar AKM: hitung ulang nilai, bukan memakai yang tersimpan. */
    public boolean hitungUlang;

    /** Jenis semester (Ganjil/Genap/Pendek) pada layar yang memakainya. */
    public String jenisSemester = "";

    /**
     * Nama program sebagai teks, bukan entitas.
     *
     * <p>Beda dengan {@link #program}. Panel unggah Ajar Dosen memakai combobox
     * {@code searchprogram} yang menyimpan <i>nama</i> program sebagai String,
     * sedangkan panel unduh memakai combobox bernama sama yang menyimpan entitas
     * {@code Program}. Perangkap yang sama dengan {@link #semester} dan
     * {@link #semesterKe}: nama widget kebetulan sama, artinya tidak. Dipisahkan
     * supaya keliru pakai menjadi galat kompilasi, bukan saringan yang
     * diam-diam tidak cocok dengan apa pun.</p>
     */
    public String namaProgram = "";

    /**
     * Samakan bentuk nilai teks dengan apa yang dibaca layar lama dari widget.
     *
     * <p>Kotak isian ZK tidak pernah mengembalikan {@code null} untuk teks
     * kosong, sedangkan JSON dari klien bisa. Tanpa penyeragaman ini, saringan
     * yang "kosong" dari jalur native akan meledakkan kueri dengan
     * {@code NullPointerException}, sementara jalur ZK berjalan mulus — beda
     * perilaku untuk masukan yang secara maksud sama.</p>
     *
     * <p>{@code kelas} dipangkas spasinya karena layar lama memang memanggil
     * {@code kelas.getValue().trim()}; sisanya dibiarkan apa adanya, juga
     * mengikuti layar lama, supaya hasil ekspornya identik.</p>
     */
    public void rapikan() {
        if (kelas == null) kelas = ""; else kelas = kelas.trim();
        if (nim == null) nim = "";
        if (nama == null) nama = "";
        if (tahunAkademik == null) tahunAkademik = "";
        if (semester == null) semester = "";
        if (tahunAjaran == null) tahunAjaran = "";
        if (jenisSemester == null) jenisSemester = "";
        if (kodeMatakuliah == null) kodeMatakuliah = "";
        if (namaProgram == null) namaProgram = "";
    }
}

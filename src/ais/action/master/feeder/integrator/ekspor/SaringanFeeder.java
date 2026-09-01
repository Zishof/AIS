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
    /** Semester sebagaimana disimpan combobox layar (teks). */
    public String semester = "";
    /** Tahun ajaran pada layar yang memisahkannya dari tahun akademik. */
    public String tahunAjaran = "";
    /** Jenis semester (Ganjil/Genap/Pendek) pada layar yang memakainya. */
    public String jenisSemester = "";
}

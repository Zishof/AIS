package ais.action.master.feeder.util;

import java.util.Calendar;
import java.util.Date;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ais.common.Common;
import ais.database.model.Agama;
import ais.database.model.BiodataDosen;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GrupJurusan;
import ais.database.model.IkatanKerjaDosen;
import ais.database.model.JenisEvaluasi;
import ais.database.model.JenisPendidikDanTenagaKependidikan;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.KebutuhanKhusus;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.LembagaPengangkat;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.NilaiHuruf;
import ais.database.model.Pekerjaan;
import ais.database.model.Penghasilan;
import ais.database.model.PenugasanDosenMengajar;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusKeluar;
import ais.database.model.StatusKepegawaian;
import ais.database.model.StatusPegawai;
import ais.database.model.SumberGaji;
import ais.database.model.Wilayah;
import ais.database.model.employ.Golongan;

/**
 * Konverter XML PDDIKTI/Dapodik Feeder ke entitas AIS: kumpulan method statis, satu per jenis
 * entitas, yang masing-masing mem-parsing satu {@link Node} XML respons Feeder (satu elemen data,
 * mis. hasil query {@code GetPerguruanTinggi}, {@code GetDosen}, {@code GetMahasiswa}) menjadi
 * entitas domain AIS terkait. Seluruh method mengikuti pola yang sama: bila {@code result} tidak
 * memiliki anak node, mengembalikan {@code null} (dianggap data kosong/tidak valid); selain itu,
 * membuat entitas baru dan mengisi field-nya dengan mencocokkan nama tag XML (case-insensitive)
 * terhadap daftar field yang diketahui — tag yang tidak dikenali diabaikan begitu saja. Kegagalan
 * parsing satu field (mis. format tanggal/angka tidak valid) dicatat ke audit dan tidak
 * menghentikan pemrosesan field lain.
 *
 * <p>
 * Varian method yang menerima parameter {@code session} (Hibernate) tidak hanya mengisi field
 * skalar, tetapi juga me-resolve relasi ke entitas AIS lain yang sudah tersimpan — baik lewat kode
 * Feeder milik entitas terkait ({@link FeederUtil#getDataByFeeder}) maupun lewat pencarian
 * langsung berdasarkan kolom {@code feeder} pada entitas tujuan. Method-method ini dipakai saat
 * proses integrasi/sinkronisasi data Feeder ke database AIS berjalan (bukan sekadar pratinjau),
 * karena membutuhkan akses database untuk resolusi relasi tersebut.
 * </p>
 */
public class FeederConverter {

	/** Mengonversi satu elemen data perguruan tinggi (respons {@code GetPerguruanTinggi}) menjadi entitas {@link PerguruanTinggi} — identitas, alamat, kontak, legalitas pendirian/izin operasi, data rekening, dan luas tanah. Mengembalikan {@code null} bila node kosong. */
	public static PerguruanTinggi perguruanTinggi(Node result) {
		if (result.hasChildNodes()) {
			PerguruanTinggi perguruanTinggi = new PerguruanTinggi();
			NodeList nodeList = result.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node.getNodeName().equalsIgnoreCase("id_sp")) {
					perguruanTinggi.setFeeder(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("nm_lemb")) {
					perguruanTinggi.setNama(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("nss")) {
					perguruanTinggi.setKodePerguruanTinggi(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("npsn")) {
					perguruanTinggi.setKodeYayasan(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("nm_singkat")) {
					perguruanTinggi.setNamaSingkat(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("jln")) {
					perguruanTinggi.setAlamat1(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("rt")) {
					perguruanTinggi.setRt(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("rw")) {
					perguruanTinggi.setRw(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("nm_dsn")) {
					perguruanTinggi.setDusun(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("ds_kel")) {
					perguruanTinggi.setKelurahan(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("kode_pos")) {
					perguruanTinggi.setKodePos(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("no_tel")) {
					perguruanTinggi.setTelepon(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("no_fax")) {
					perguruanTinggi.setFaksimili(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("email")) {
					perguruanTinggi.setEmail(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("website")) {
					perguruanTinggi.setWebsite(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("sk_pendirian_sp")) {
					perguruanTinggi.setNomorAkta(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("tgl_berdiri")) {
					try {
						perguruanTinggi
								.setTanggalAwalPendirian(Common.databaseDateFormat.get().parse(node.getTextContent().trim()));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:103");

					}
				}
				if (node.getNodeName().equalsIgnoreCase("tgl_sk_pendirian_sp")) {
					try {
						perguruanTinggi.setTanggalAkta(Common.databaseDateFormat.get().parse(node.getTextContent().trim()));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:110");

					}
				}
				if (node.getNodeName().equalsIgnoreCase("tgl_sk_izin_operasi")) {
					try {
						perguruanTinggi
								.setTglSkIzinOperasi(Common.databaseDateFormat.get().parse(node.getTextContent().trim()));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:118");

					}
				}
				if (node.getNodeName().equalsIgnoreCase("sk_izin_operasi")) {
					perguruanTinggi.setSkIzinOperasi(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("no_rek")) {
					perguruanTinggi.setNoRek(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("nm_bank")) {
					perguruanTinggi.setNmBank(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("unit_cabang")) {
					perguruanTinggi.setUnitCabang(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("nm_rek")) {
					perguruanTinggi.setNmRek(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("luas_tanah_milik")) {
					try {
						perguruanTinggi.setLuasTanahMilik(Double.parseDouble(node.getTextContent().trim()));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:140");

					}
				}
				if (node.getNodeName().equalsIgnoreCase("luas_tanah_bukan_milik")) {
					try {
						perguruanTinggi.setLuasTanahBukanMilik(Double.parseDouble(node.getTextContent().trim()));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:147");

					}
				}

			}

			return perguruanTinggi == null || perguruanTinggi.getId() == null ? null : perguruanTinggi;
		} else {
			return null;
		}
	}

	/** Mengonversi satu elemen data agama Feeder menjadi entitas {@link Agama} (kode dan nama). Mengembalikan {@code null} bila node kosong. */
	public static Agama agama(Node result) {
		if (result.hasChildNodes()) {
			Agama agama = new Agama();
			NodeList nodeList = result.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node.getNodeName().equalsIgnoreCase("id_agama")) {
					agama.setFeeder(Long.parseLong(node.getTextContent().trim()));
				}
				if (node.getNodeName().equalsIgnoreCase("nm_agama")) {
					agama.setNama(node.getTextContent().trim());
				}
			}

			return agama;
		} else {
			return null;
		}
	}

	/** Mengonversi satu elemen data jenis evaluasi Feeder menjadi entitas {@link JenisEvaluasi}. Mengembalikan {@code null} bila node kosong. */
	public static JenisEvaluasi jenisEvaluasi(Node result) {
		if (result.hasChildNodes()) {
			JenisEvaluasi jenisEvaluasi = new JenisEvaluasi();
			NodeList nodeList = result.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node.getNodeName().equalsIgnoreCase("id_jns_eval")) {
					jenisEvaluasi.setFeeder(Long.parseLong(node.getTextContent().trim()));
				}
				if (node.getNodeName().equalsIgnoreCase("nm_jns_eval")) {
					jenisEvaluasi.setNama(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("ket_jns_eval")) {
					jenisEvaluasi.setKeterangan(node.getTextContent().trim());
				}
			}

			return jenisEvaluasi;
		} else {
			return null;
		}
	}

	/** Mengonversi satu elemen data status awal mahasiswa (jalur masuk) Feeder menjadi entitas {@link StatusAwalMahasiswa}. Mengembalikan {@code null} bila node kosong. */
	public static StatusAwalMahasiswa statusAwalMahasiswa(Node result) {
		if (result.hasChildNodes()) {
			StatusAwalMahasiswa statusAwalMahasiswa = new StatusAwalMahasiswa();
			NodeList nodeList = result.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node.getNodeName().equalsIgnoreCase("id_jns_daftar")) {
					statusAwalMahasiswa.setFeeder(Long.parseLong(node.getTextContent().trim()));
				}
				if (node.getNodeName().equalsIgnoreCase("nm_jns_daftar")) {
					statusAwalMahasiswa.setNama(node.getTextContent().trim());
				}
			}

			return statusAwalMahasiswa;
		} else {
			return null;
		}
	}

	/** Mengonversi satu elemen data pekerjaan Feeder menjadi entitas {@link Pekerjaan}. Mengembalikan {@code null} bila node kosong. */
	public static Pekerjaan pekerjaan(Node result) {
		if (result.hasChildNodes()) {
			Pekerjaan pekerjaan = new Pekerjaan();
			NodeList nodeList = result.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node.getNodeName().equalsIgnoreCase("id_pekerjaan")) {
					pekerjaan.setFeeder(Long.parseLong(node.getTextContent().trim()));
				}
				if (node.getNodeName().equalsIgnoreCase("nm_pekerjaan")) {
					pekerjaan.setNama(node.getTextContent().trim());
				}
			}

			return pekerjaan;
		} else {
			return null;
		}
	}

	/** Mengonversi satu elemen data rentang penghasilan Feeder menjadi entitas {@link Penghasilan}. Mengembalikan {@code null} bila node kosong. */
	public static Penghasilan penghasilan(Node result) {
		if (result.hasChildNodes()) {
			Penghasilan penghasilan = new Penghasilan();
			NodeList nodeList = result.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node.getNodeName().equalsIgnoreCase("id_penghasilan")) {
					penghasilan.setFeeder(Long.parseLong(node.getTextContent().trim()));
				}
				if (node.getNodeName().equalsIgnoreCase("nm_penghasilan")) {
					penghasilan.setNama(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("batas_bawah")) {
					try {
						penghasilan.setBatasBawah(Double.parseDouble(node.getTextContent().trim()));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:258");

					}
				}
				if (node.getNodeName().equalsIgnoreCase("batas_atas")) {
					try {
						penghasilan.setBatasAtas(Double.parseDouble(node.getTextContent().trim()));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:265");

					}
				}
			}

			return penghasilan;
		} else {
			return null;
		}
	}

	/** Mengonversi satu elemen data wilayah administratif Feeder menjadi entitas {@link Wilayah}. Mengembalikan {@code null} bila node kosong. */
	public static Wilayah wilayah(Node result) {
		if (result.hasChildNodes()) {
			Wilayah wilayah = new Wilayah();
			NodeList nodeList = result.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node.getNodeName().equalsIgnoreCase("id_wil")) {
					wilayah.setFeeder(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("nm_wil")) {
					wilayah.setNama(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("id_induk_wilayah")) {
					wilayah.setInduk(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("id_level_wil")) {
					wilayah.setLevel(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("id_negara")) {
					wilayah.setNegara(node.getTextContent().trim());
				}
			}

			return wilayah;
		} else {
			return null;
		}
	}

	/** Mengonversi satu elemen data jenis kebutuhan khusus (disabilitas) Feeder menjadi entitas {@link KebutuhanKhusus}. Mengembalikan {@code null} bila node kosong. */
	public static KebutuhanKhusus kebutuhanKhusus(Node result) {
		if (result.hasChildNodes()) {
			KebutuhanKhusus kebutuhanKhusus = new KebutuhanKhusus();
			NodeList nodeList = result.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node.getNodeName().equalsIgnoreCase("id_kk")) {
					kebutuhanKhusus.setFeeder(Long.parseLong(node.getTextContent().trim()));
				}
				if (node.getNodeName().equalsIgnoreCase("nm_kk")) {
					kebutuhanKhusus.setNama(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("a_kk_a")) {
					kebutuhanKhusus.setA_kk_a(node.getTextContent().trim().equalsIgnoreCase("1"));
				}

				if (node.getNodeName().equalsIgnoreCase("a_kk_b")) {
					kebutuhanKhusus.setA_kk_b(node.getTextContent().trim().equalsIgnoreCase("1"));
				}
				if (node.getNodeName().equalsIgnoreCase("a_kk_c")) {
					kebutuhanKhusus.setA_kk_c(node.getTextContent().trim().equalsIgnoreCase("1"));
				}
				if (node.getNodeName().equalsIgnoreCase("a_kk_c1")) {
					kebutuhanKhusus.setA_kk_c1(node.getTextContent().trim().equalsIgnoreCase("1"));
				}
				if (node.getNodeName().equalsIgnoreCase("a_kk_d")) {
					kebutuhanKhusus.setA_kk_d(node.getTextContent().trim().equalsIgnoreCase("1"));
				}
				if (node.getNodeName().equalsIgnoreCase("a_kk_d1")) {
					kebutuhanKhusus.setA_kk_d1(node.getTextContent().trim().equalsIgnoreCase("1"));
				}
				if (node.getNodeName().equalsIgnoreCase("a_kk_e")) {
					kebutuhanKhusus.setA_kk_e(node.getTextContent().trim().equalsIgnoreCase("1"));
				}
				if (node.getNodeName().equalsIgnoreCase("a_kk_f")) {
					kebutuhanKhusus.setA_kk_f(node.getTextContent().trim().equalsIgnoreCase("1"));
				}
				if (node.getNodeName().equalsIgnoreCase("a_kk_h")) {
					kebutuhanKhusus.setA_kk_h(node.getTextContent().trim().equalsIgnoreCase("1"));
				}
				if (node.getNodeName().equalsIgnoreCase("a_kk_i")) {
					kebutuhanKhusus.setA_kk_i(node.getTextContent().trim().equalsIgnoreCase("1"));
				}
				if (node.getNodeName().equalsIgnoreCase("a_kk_j")) {
					kebutuhanKhusus.setA_kk_j(node.getTextContent().trim().equalsIgnoreCase("1"));
				}
				if (node.getNodeName().equalsIgnoreCase("a_kk_k")) {
					kebutuhanKhusus.setA_kk_k(node.getTextContent().trim().equalsIgnoreCase("1"));
				}
				if (node.getNodeName().equalsIgnoreCase("a_kk_n")) {
					kebutuhanKhusus.setA_kk_n(node.getTextContent().trim().equalsIgnoreCase("1"));
				}
				if (node.getNodeName().equalsIgnoreCase("a_kk_o")) {
					kebutuhanKhusus.setA_kk_o(node.getTextContent().trim().equalsIgnoreCase("1"));
				}
				if (node.getNodeName().equalsIgnoreCase("a_kk_p")) {
					kebutuhanKhusus.setA_kk_p(node.getTextContent().trim().equalsIgnoreCase("1"));
				}
				if (node.getNodeName().equalsIgnoreCase("a_kk_q")) {
					kebutuhanKhusus.setA_kk_q(node.getTextContent().trim().equalsIgnoreCase("1"));
				}

			}

			return kebutuhanKhusus;
		} else {
			return null;
		}
	}

	/** Mengonversi satu elemen data jenjang pendidikan Feeder menjadi entitas {@link Jenjang}. Mengembalikan {@code null} bila node kosong. */
	public static Jenjang jenjang(Node result) {
		if (result.hasChildNodes()) {
			Jenjang jenjang = new Jenjang();
			NodeList nodeList = result.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node.getNodeName().equalsIgnoreCase("id_jenj_didik")) {
					jenjang.setFeeder(Long.parseLong(node.getTextContent().trim()));
				}
				if (node.getNodeName().equalsIgnoreCase("nm_jenj_didik")) {
					jenjang.setNama(node.getTextContent().trim());
				}
			}

			return jenjang;
		} else {
			return null;
		}
	}

	/** Mengonversi satu elemen data jenis ikatan kerja dosen Feeder menjadi entitas {@link IkatanKerjaDosen}. Mengembalikan {@code null} bila node kosong. */
	public static IkatanKerjaDosen ikatanKerjaDosen(Node result) {
		if (result.hasChildNodes()) {
			IkatanKerjaDosen ikatanKerjaDosen = new IkatanKerjaDosen();
			NodeList nodeList = result.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node.getNodeName().equalsIgnoreCase("id_ikatan_kerja")) {
					ikatanKerjaDosen.setFeeder(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("nm_ikatan_kerja")) {
					ikatanKerjaDosen.setNama(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("ket_ikatan_kerja")) {
					ikatanKerjaDosen.setKeterangan(node.getTextContent().trim());
				}
			}

			return ikatanKerjaDosen;
		} else {
			return null;
		}
	}

	/** Mengonversi satu elemen data status kepegawaian Feeder menjadi entitas {@link StatusKepegawaian}. Mengembalikan {@code null} bila node kosong. */
	public static StatusKepegawaian statusKepegawaian(Node result) {
		if (result.hasChildNodes()) {
			StatusKepegawaian statusKepegawaian = new StatusKepegawaian();
			NodeList nodeList = result.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node.getNodeName().equalsIgnoreCase("id_stat_pegawai")) {
					statusKepegawaian.setFeeder(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("nm_stat_pegawai")) {
					statusKepegawaian.setNama(node.getTextContent().trim());
				}

			}

			return statusKepegawaian;
		} else {
			return null;
		}
	}

	/** Mengonversi satu elemen data lembaga pengangkat pegawai Feeder menjadi entitas {@link LembagaPengangkat}. Mengembalikan {@code null} bila node kosong. */
	public static LembagaPengangkat lembagaPengangkat(Node result) {
		if (result.hasChildNodes()) {
			LembagaPengangkat lembagaPengangkat = new LembagaPengangkat();
			NodeList nodeList = result.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node.getNodeName().equalsIgnoreCase("id_lemb_angkat")) {
					lembagaPengangkat.setFeeder(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("nm_lemb_angkat")) {
					lembagaPengangkat.setNama(node.getTextContent().trim());
				}

			}

			return lembagaPengangkat;
		} else {
			return null;
		}
	}

	/** Mengonversi satu elemen data status pegawai Feeder menjadi entitas {@link StatusPegawai}. Mengembalikan {@code null} bila node kosong. */
	public static StatusPegawai statusPegawai(Node result) {
		if (result.hasChildNodes()) {
			StatusPegawai statusPegawai = new StatusPegawai();
			NodeList nodeList = result.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node.getNodeName().equalsIgnoreCase("id_stat_aktif")) {
					statusPegawai.setFeeder(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("nm_stat_aktif")) {
					statusPegawai.setNama(node.getTextContent().trim());
				}

			}

			return statusPegawai;
		} else {
			return null;
		}
	}

	/** Mengonversi satu elemen data golongan kepegawaian Feeder menjadi entitas {@link Golongan}. Mengembalikan {@code null} bila node kosong. */
	public static Golongan golongan(Node result) {
		if (result.hasChildNodes()) {
			Golongan golongan = new Golongan();
			NodeList nodeList = result.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node.getNodeName().equalsIgnoreCase("id_pangkat_gol")) {
					golongan.setFeeder(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("kode_gol")) {
					golongan.setNama(node.getTextContent().trim());
				}

				if (node.getNodeName().equalsIgnoreCase("nm_pangkat")) {
					golongan.setPangkat(node.getTextContent().trim());
				}

			}

			return golongan;
		} else {
			return null;
		}
	}

	/** Mengonversi satu elemen data konversi nilai huruf Feeder menjadi entitas {@link NilaiHuruf}, me-resolve relasi terkait (mis. jenjang) lewat {@code session} berdasarkan kode Feeder. Mengembalikan {@code null} bila node kosong. */
	public static NilaiHuruf nilaiHuruf(Node result, Session session) {
		if (result.hasChildNodes()) {
			NilaiHuruf nilaiHuruf = new NilaiHuruf();
			NodeList nodeList = result.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node.getNodeName().equalsIgnoreCase("kode_bobot_nilai")) {
					nilaiHuruf.setFeeder(node.getTextContent().trim());
				}
				if (node.getNodeName().equalsIgnoreCase("id_sms")) {
					Jurusan jurusan = FeederUtil.getDataByFeeder(session, node.getTextContent().trim(), Jurusan.class,
							Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
					if (jurusan != null) {
						nilaiHuruf.setJurusan(jurusan);
						nilaiHuruf.setFakultas(jurusan.getFakultas());
					}
				}

				if (node.getNodeName().equalsIgnoreCase("nilai_huruf")) {
					nilaiHuruf.setNilaiHuruf(node.getTextContent().trim());
				}

				if (node.getNodeName().equalsIgnoreCase("bobot_nilai_min")) {
					try {
						nilaiHuruf.setMulai(Double.parseDouble(node.getTextContent().trim()));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:532");

					}
				}

				if (node.getNodeName().equalsIgnoreCase("bobot_nilai_maks")) {
					try {
						nilaiHuruf.setSampai(Double.parseDouble(node.getTextContent().trim()));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:540");

					}
				}

				if (node.getNodeName().equalsIgnoreCase("nilai_indeks")) {
					try {
						nilaiHuruf.setNilaiDiIPK(Double.parseDouble(node.getTextContent().trim()));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:548");

					}
				}

				if (node.getNodeName().equalsIgnoreCase("tgl_mulai_efektif")) {
					try {

						Date date = Common.databaseDateFormat.get().parse(node.getTextContent().trim());
						Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
						calendar.setTime(date);
						nilaiHuruf.setTahunAngkatan(calendar.get(Calendar.YEAR));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:560");

					}
				}
			}

			return nilaiHuruf;
		} else {
			return null;
		}
	}

	/** Mengonversi satu elemen data program studi Feeder menjadi entitas {@link Jurusan}: identitas, kode prodi, dan resolusi relasi {@link Fakultas}/{@link GrupJurusan} induk serta {@link Jenjang} lewat {@code session} berdasarkan kode Feeder. Mengembalikan {@code null} bila node kosong. */
	public static Jurusan jurusan(Node result, Session session) {
		if (result.hasChildNodes()) {
			Jurusan jurusan = new Jurusan();
			NodeList nodeList = result.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				try {
					Node node = nodeList.item(i);

					if (node.getTextContent().trim().isEmpty()) {
						continue;
					}
					if (node.getNodeName().equalsIgnoreCase("id_induk_sms")) {
						jurusan.setFakultas(
								FeederUtil.getDataByFeeder(session, node.getTextContent().trim(), Fakultas.class));
					}
					if (node.getNodeName().equalsIgnoreCase("id_induk_sms")) {
						jurusan.setGrupJurusan(
								FeederUtil.getDataByFeeder(session, node.getTextContent().trim(), GrupJurusan.class));
					}

					if (node.getNodeName().equalsIgnoreCase("id_sms")) {
						jurusan.setFeeder(node.getTextContent().trim());
					}
					if (node.getNodeName().equalsIgnoreCase("nm_lemb")) {
						jurusan.setNama(node.getTextContent().trim());
					}
					if (node.getNodeName().equalsIgnoreCase("kode_prodi")) {
						jurusan.setKodeEpsbed(node.getTextContent().trim());
					}
					if (node.getNodeName().equalsIgnoreCase("id_jenj_didik")) {
						jurusan.setJenjang((Jenjang) session.createCriteria(Jenjang.class)
								.add(Restrictions.eq("feeder", Long.parseLong(node.getTextContent().trim().trim())))
								.setMaxResults(1).uniqueResult());
					}

				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:607");

				}
			}

			return jurusan;
		} else {
			return null;
		}
	}

	/** Mengonversi satu elemen data fakultas Feeder menjadi entitas {@link Fakultas}, me-resolve relasi terkait lewat {@code session} berdasarkan kode Feeder. Mengembalikan {@code null} bila node kosong. */
	public static Fakultas fakultas(Node result, Session session) {
		if (result.hasChildNodes()) {
			Fakultas fakultas = new Fakultas();
			NodeList nodeList = result.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				try {
					Node node = nodeList.item(i);
					if (node.getNodeName().equalsIgnoreCase("id_sms")) {
						fakultas.setFeeder(node.getTextContent().trim());
					}
					if (node.getNodeName().equalsIgnoreCase("nm_lemb")) {
						fakultas.setNama(node.getTextContent().trim());
					}
					if (node.getNodeName().equalsIgnoreCase("kode_prodi")) {
						fakultas.setKode(node.getTextContent().trim());
					}

				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:635");

				}
			}

			return fakultas;
		} else {
			return null;
		}
	}

	/** Mengonversi satu elemen data grup jurusan/rumpun ilmu Feeder menjadi entitas {@link GrupJurusan}, me-resolve relasi terkait lewat {@code session} berdasarkan kode Feeder. Mengembalikan {@code null} bila node kosong. */
	public static GrupJurusan grupJurusan(Node result, Session session) {
		if (result.hasChildNodes()) {
			GrupJurusan grupJurusan = new GrupJurusan();
			NodeList nodeList = result.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {

				try {

					Node node = nodeList.item(i);
					if (node.getTextContent().trim().isEmpty()) {
						continue;
					}
					if (node.getNodeName().equalsIgnoreCase("id_induk_sms")) {
						grupJurusan.setFakultas(
								FeederUtil.getDataByFeeder(session, node.getTextContent().trim(), Fakultas.class));
					}
					if (node.getNodeName().equalsIgnoreCase("id_sms")) {
						grupJurusan.setFeeder(node.getTextContent().trim());
					}
					if (node.getNodeName().equalsIgnoreCase("nm_lemb")) {
						grupJurusan.setNama(node.getTextContent().trim());
					}
					if (node.getNodeName().equalsIgnoreCase("kode_prodi")) {
						grupJurusan.setKode(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("id_induk_sms")) {
						grupJurusan.setFakultas(
								FeederUtil.getDataByFeeder(session, node.getTextContent().trim(), Fakultas.class));
					}

				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:677");

				}
			}

			return grupJurusan;
		} else {
			return null;
		}
	}

	/** Mengonversi satu elemen data mata kuliah Feeder menjadi entitas {@link Matakuliah}: identitas, SKS, dan resolusi relasi (mis. jurusan pemilik) lewat {@code session} berdasarkan kode Feeder. Mengembalikan {@code null} bila node kosong. */
	public static Matakuliah matakuliah(Node result, Session session) {
		if (result.hasChildNodes()) {
			Matakuliah matakuliah = new Matakuliah();
			NodeList nodeList = result.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				try {
					Node node = nodeList.item(i);
					if (node.getTextContent() == null) {
						continue;
					}
					if (node.getNodeName().equalsIgnoreCase("id_mk")) {
						matakuliah.setFeeder(node.getTextContent().trim());
					}
					if (node.getNodeName().equalsIgnoreCase("nm_mk")) {
						matakuliah.setNama(node.getTextContent().trim());
					}
					if (node.getNodeName().equalsIgnoreCase("kode_mk")) {
						matakuliah.setKode(node.getTextContent().trim());
					}
					if (node.getNodeName().equalsIgnoreCase("id_sms")) {
						matakuliah.setJurusan(
								FeederUtil.getDataByFeeder(session, node.getTextContent().trim(), Jurusan.class));
					}
					if (node.getNodeName().equalsIgnoreCase("sks_mk")) {
						matakuliah.setSks(Integer.parseInt(node.getTextContent().trim()));
					}
					if (node.getNodeName().equalsIgnoreCase("sks_tm")) {
						matakuliah.setSksDiskusi(Integer.parseInt(node.getTextContent().trim()));
					}
					if (node.getNodeName().equalsIgnoreCase("sks_prak")) {
						matakuliah.setSksPraktek(Integer.parseInt(node.getTextContent().trim()));
					}
					if (node.getNodeName().equalsIgnoreCase("sks_prak_lap")) {
						matakuliah.setSksPraktekLapangan(Integer.parseInt(node.getTextContent().trim()));
					}
					if (node.getNodeName().equalsIgnoreCase("sks_sim")) {
						matakuliah.setSksSimulasi(Integer.parseInt(node.getTextContent().trim()));
					}
					if (node.getNodeName().equalsIgnoreCase("metode_pelaksanaan_kuliah")) {
						matakuliah.setMetodeKuliah((node.getTextContent().trim().trim()));
					}
					if (node.getNodeName().equalsIgnoreCase("a_sap")) {
						matakuliah.setAdaSap(node.getTextContent().trim().equals("1"));
					}
					if (node.getNodeName().equalsIgnoreCase("a_silabus")) {
						matakuliah.setAdaSilabus(node.getTextContent().trim().equals("1"));
					}
					if (node.getNodeName().equalsIgnoreCase("a_bahan_ajar")) {
						matakuliah.setAdaBahanAjar(node.getTextContent().trim().equals("1"));
					}
					if (node.getNodeName().equalsIgnoreCase("acara_prak")) {
						matakuliah.setAdaAcaraPraktek(node.getTextContent().trim().trim().equals("1"));
					}
					if (node.getNodeName().equalsIgnoreCase("a_diktat")) {
						matakuliah.setAdaDiktat(node.getTextContent().trim().equals("1"));
					}
					if (node.getNodeName().equalsIgnoreCase("tgl_mulai_efektif")) {
						try {
							matakuliah.setTanggalMulai(Common.databaseDateFormat.get().parse(node.getTextContent().trim()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:747");

						}
					}
					if (node.getNodeName().equalsIgnoreCase("tgl_akhir_efektif")) {
						try {
							matakuliah.setTanggalSampai(Common.databaseDateFormat.get().parse(node.getTextContent().trim()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:754");

						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:758");

				}
			}

			return matakuliah;
		} else {
			return null;
		}
	}

	/** Mengonversi satu elemen data kurikulum Feeder menjadi entitas {@link Kurikulum}, me-resolve relasi jurusan pemilik lewat {@code session} berdasarkan kode Feeder. Mengembalikan {@code null} bila node kosong. */
	public static Kurikulum kurikulum(Node result, Session session) {
		if (result.hasChildNodes()) {
			Kurikulum kurikulum = new Kurikulum();
			NodeList nodeList = result.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				try {
					Node node = nodeList.item(i);
					if (node.getTextContent() == null) {
						continue;
					}
					if (node.getNodeName().equalsIgnoreCase("id_kurikulum_sp")) {
						kurikulum.setFeeder(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("id_kurikulum_sp")) {
						kurikulum.setFeeder(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("id_sms")) {
						kurikulum.setJurusan(
								FeederUtil.getDataByFeeder(session, node.getTextContent().trim(), Jurusan.class));
					}

					if (node.getNodeName().equalsIgnoreCase("id_smt_berlaku")) {
						try {
							kurikulum.setTahun(Integer.parseInt(node.getTextContent().trim().substring(0, 4)));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:795");

						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:799");

				}
			}

			return kurikulum;
		} else {
			return null;
		}
	}

	/** Mengonversi satu elemen data relasi kurikulum-matakuliah Feeder menjadi entitas {@link KurikulumPunyaMatakuliah}, me-resolve {@link Kurikulum} dan {@link Matakuliah} terkait lewat {@code session} berdasarkan kode Feeder. Mengembalikan {@code null} bila node kosong. */
	public static KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah(Node result, Session session) {
		if (result.hasChildNodes()) {
			KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = new KurikulumPunyaMatakuliah();
			NodeList nodeList = result.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				try {
					Node node = nodeList.item(i);
					if (node.getTextContent() == null) {
						continue;
					}
					if (node.getNodeName().equalsIgnoreCase("id_kurikulum_sp")) {
						kurikulumPunyaMatakuliah.setKurikulum(FeederUtil.getDataByFeeder(session,
								node.getTextContent().trim().trim(), Kurikulum.class));
					}

					if (node.getNodeName().equalsIgnoreCase("id_mk")) {
						kurikulumPunyaMatakuliah.setMatakuliah(FeederUtil.getDataByFeeder(session,
								node.getTextContent().trim().trim(), Matakuliah.class));
					}

					if (node.getNodeName().equalsIgnoreCase("smt")) {
						kurikulumPunyaMatakuliah.setSemester(Integer.parseInt(node.getTextContent().trim()));
					}

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			return kurikulumPunyaMatakuliah;
		} else {
			return null;
		}
	}

	/**
	 * Mengonversi satu elemen data mahasiswa Feeder (respons mis. {@code GetMahasiswa}) menjadi
	 * entitas {@link BiodataMahasiswa}: biodata lengkap (identitas, kelahiran, alamat, kontak,
	 * kebutuhan khusus), status akademik (jenjang, status awal, program), dan resolusi seluruh
	 * relasi rujukan (jurusan, agama, pekerjaan orang tua, penghasilan, wilayah, dsb) lewat
	 * {@code session} berdasarkan kode Feeder. Mahasiswa ini adalah method konversi terbesar dan
	 * paling banyak field di kelas ini. Mengembalikan {@code null} bila node kosong.
	 */
	public static BiodataMahasiswa mahasiswa(Node result, Session session) {
		if (result.hasChildNodes()) {
			Mahasiswa mahasiswa = new Mahasiswa();
			BiodataMahasiswa biodataMahasiswa = new BiodataMahasiswa();
			NodeList nodeList = result.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				try {
					Node node = nodeList.item(i);
					if (node.getTextContent() == null || node.getTextContent().trim().isEmpty()) {
						continue;
					}

					if (node.getNodeName().equalsIgnoreCase("id_reg_pd")) {
						mahasiswa.setIdRegPd(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("id_pd")) {
						mahasiswa.setFeeder(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("nm_pd")) {
						mahasiswa.setNama(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("nipd")) {
						mahasiswa.setNim(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("id_sms")) {
						mahasiswa.setJurusan(
								FeederUtil.getDataByFeeder(session, node.getTextContent().trim(), Jurusan.class));
					}

					if (node.getNodeName().equalsIgnoreCase("id_jns_daftar")) {
						mahasiswa.setStatusAwalMahasiswa(FeederUtil.getDataByFeeder(session,
								Long.parseLong(node.getTextContent().trim()), StatusAwalMahasiswa.class));
					}

					if (node.getNodeName().equalsIgnoreCase("tgl_masuk_sp")) {
						try {
							mahasiswa.setTanggalMasuk(Common.databaseDateFormat.get().parse(node.getTextContent().trim()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:886");

						}
					}

					if (node.getNodeName().equalsIgnoreCase("id_jns_keluar")) {

						StatusKeluar statusKeluar = FeederUtil.getDataByFeeder(session, node.getTextContent().trim(),
								StatusKeluar.class);
						Integer semesterLulus = Mahasiswa.hitungSmtLulus(statusKeluar, mahasiswa);
						mahasiswa.setSemesterLulus(semesterLulus);
						mahasiswa.setStatusKeluar(statusKeluar);
					}

					if (node.getNodeName().equalsIgnoreCase("tgl_keluar")) {
						try {
							mahasiswa.setTanggalLulus(Common.databaseDateFormat.get().parse(node.getTextContent().trim()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:903");

						}
					}

					if (node.getNodeName().equalsIgnoreCase("ket")) {
						mahasiswa.setKeterangan(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("a_pernah_paud")) {
						biodataMahasiswa.setApakahPernahPaud(node.getTextContent().trim().equalsIgnoreCase("1"));
					}

					// if (node.getNodeName().equalsIgnoreCase("skhun")) {
					// mahasiswa.setskhun(node.getTextContent().trim());
					// }

					if (node.getNodeName().equalsIgnoreCase("a_pernah_tk")) {
						biodataMahasiswa.setApakahPernahTk(node.getTextContent().trim().equalsIgnoreCase("1"));
					}

					if (node.getNodeName().equalsIgnoreCase("mulai_smt")) {
						try {
							mahasiswa.setTahunangkatan(Integer.parseInt(node.getTextContent().trim().substring(0, 4)));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:927");

						}

						try {
							mahasiswa.setSemesterMulai(
									node.getTextContent().trim().trim().substring(4).equalsIgnoreCase("1")
											? Perkuliahan.GANJIL
											: Perkuliahan.GENAP);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:936");

						}
					}

					if (node.getNodeName().equalsIgnoreCase("judul_skripsi")) {
						mahasiswa.setJudulSkripsi(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("bln_awal_bimbingan")) {
						try {
							mahasiswa
									.setBlnAwalBimbingan(Common.databaseDateFormat.get().parse(node.getTextContent().trim()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:949");

						}
					}

					if (node.getNodeName().equalsIgnoreCase("bln_akhir_bimbingan")) {
						try {
							mahasiswa.setBlnAkhirBimbingan(
									Common.databaseDateFormat.get().parse(node.getTextContent().trim()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:958");

						}
					}

					if (node.getNodeName().equalsIgnoreCase("sk_yudisium")) {
						mahasiswa.setNoAkta1(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("tgl_sk_yudisium")) {
						try {
							mahasiswa.setTanggalYudisium(Common.databaseDateFormat.get().parse(node.getTextContent().trim()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:970");

						}
					}

					if (node.getNodeName().equalsIgnoreCase("no_seri_ijazah")) {
						mahasiswa.setNoIjazah1(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("nm_pt_asal")) {
						mahasiswa.setPindahanPerguruanTinggi(node.getTextContent().trim());
					}

					// if (node.getNodeName().equalsIgnoreCase("nm_prodi_asal"))
					// {
					// mahasiswa.setPindahanPerguruanTinggi(node
					// .getTextContent().trim());
					// }

				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:989");

				}
			}

			biodataMahasiswa.setMahasiswa(mahasiswa);

			return biodataMahasiswa;
		} else {
			return null;
		}
	}

	/**
	 * Mengonversi satu elemen data dosen Feeder (respons mis. {@code GetDosen}) menjadi entitas
	 * {@link BiodataDosen}: biodata lengkap, status kepegawaian, ikatan kerja, golongan, lembaga
	 * pengangkat, sumber gaji, dan resolusi seluruh relasi rujukan lewat {@code session}
	 * berdasarkan kode Feeder. Bersama {@link #mahasiswa(Node, Session)}, ini adalah salah satu
	 * method konversi terbesar di kelas ini. Mengembalikan {@code null} bila node kosong.
	 */
	public static BiodataDosen dosen(Node result, Session session) {
		if (result.hasChildNodes()) {
			Dosen dosen = new Dosen();
			BiodataDosen biodataDosen = new BiodataDosen();
			NodeList nodeList = result.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				try {
					Node node = nodeList.item(i);
					if (node.getTextContent() == null || node.getTextContent().trim().isEmpty()) {
						continue;
					}

					if (node.getNodeName().equalsIgnoreCase("id_ptk")) {
						dosen.setFeeder(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("id_reg_ptk")) {
						dosen.setIdRegPtk(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("id_ikatan_kerja")) {
						dosen.setIkatanKerjaDosen(FeederUtil.getDataByFeeder(session, node.getTextContent().trim(),
								IkatanKerjaDosen.class));
					}

					if (node.getNodeName().equalsIgnoreCase("nm_ptk")) {
						dosen.setNama(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("nidn")) {
						dosen.setNidn(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("nip")) {
						dosen.setCode(node.getTextContent().trim());
						dosen.setMycode(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("jk")) {
						dosen.setKelamin(node.getTextContent() == null ? ""
								: node.getTextContent().trim().equalsIgnoreCase("L") ? "Laki-laki" : "Perempuan");
					}

					if (node.getNodeName().equalsIgnoreCase("tmpt_lahir")) {
						dosen.setTempatlahir(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("tgl_lahir")) {
						try {
							dosen.setTanggallahir(Common.databaseDateFormat.get().parse(node.getTextContent().trim()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:1052");

						}
					}

					if (node.getNodeName().equalsIgnoreCase("nik")) {
						dosen.setKtp(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("niy_nigk")) {
						dosen.setNiyNigk(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("nuptk")) {
						dosen.setNuptk(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("id_stat_pegawai")) {
						dosen.setStatusKepegawaian(FeederUtil.getDataByFeeder(session, node.getTextContent().trim(),
								StatusKepegawaian.class));
					}

					if (node.getNodeName().equalsIgnoreCase("id_jns_ptk")) {
						dosen.setJenisPendidikDanTenagaKependidikan(FeederUtil.getDataByFeeder(session,
								node.getTextContent().trim(), JenisPendidikDanTenagaKependidikan.class));
					}

					if (node.getNodeName().equalsIgnoreCase("id_agama")) {
						biodataDosen.setAgama(FeederUtil.getDataByFeeder(session,
								Long.parseLong(node.getTextContent().trim()), Agama.class,
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
					}

					if (node.getNodeName().equalsIgnoreCase("jln")) {
						dosen.setAlamat(node.getTextContent().trim());
						biodataDosen.setAlamat(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("rt")) {
						biodataDosen.setRt(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("rw")) {
						biodataDosen.setRw(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("nm_dsn")) {
						biodataDosen.setDusun(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("ds_kel")) {
						biodataDosen.setKelurahan(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("id_wil")) {
						biodataDosen.setKecamatan(
								FeederUtil.getDataByFeeder(session, node.getTextContent().trim(), Wilayah.class));
					}

					if (node.getNodeName().equalsIgnoreCase("kode_pos")) {
						biodataDosen.setKodepos(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("no_tel_rmh")) {
						biodataDosen.setTeleponRumah(node.getTextContent().trim());
						dosen.setTelp(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("no_hp")) {
						biodataDosen.setHp(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("email")) {
						dosen.setEmail(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("id_sp")) {
						dosen.setPerguruanTinggi(FeederUtil.getDataByFeeder(session, node.getTextContent().trim(),
								PerguruanTinggi.class));
					}

					if (node.getNodeName().equalsIgnoreCase("id_stat_aktif")) {
						dosen.setStatusPegawai(
								FeederUtil.getDataByFeeder(session, node.getTextContent().trim(), StatusPegawai.class));
					}

					if (node.getNodeName().equalsIgnoreCase("sk_cpns")) {
						dosen.setSkCpns(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("tgl_sk_cpns")) {
						try {
							dosen.setTglSkCpns(Common.databaseDateFormat.get().parse(node.getTextContent().trim()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:1145");

						}
					}

					if (node.getNodeName().equalsIgnoreCase("sk_angkat")) {
						dosen.setSkAngkat(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("tmt_sk_angkat")) {
						try {
							dosen.setTmtSkAngkat(Common.databaseDateFormat.get().parse(node.getTextContent().trim()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:1157");

						}
					}

					if (node.getNodeName().equalsIgnoreCase("id_lemb_angkat")) {
						dosen.setLembagaPengangkat(FeederUtil.getDataByFeeder(session, node.getTextContent().trim(),
								LembagaPengangkat.class));
					}

					if (node.getNodeName().equalsIgnoreCase("id_pangkat_gol")) {
						dosen.setGolonganPegawai(
								FeederUtil.getDataByFeeder(session, node.getTextContent().trim(), Golongan.class));
					}

					if (node.getNodeName().equalsIgnoreCase("id_sumber_gaji")) {
						dosen.setSumberGaji(
								FeederUtil.getDataByFeeder(session, node.getTextContent().trim(), SumberGaji.class));
					}

					if (node.getNodeName().equalsIgnoreCase("nm_ibu_kandung")) {
						biodataDosen.setNamaIbu(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("stat_kawin")) {
						biodataDosen.setStatusNikah(Integer.parseInt(node.getTextContent().trim()));
					}

					if (node.getNodeName().equalsIgnoreCase("nm_suami_istri")) {
						biodataDosen.setNamaSuamiIstri(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("nip_suami_istri")) {
						biodataDosen.setNipSuamiIstri(node.getTextContent().trim());
					}

					if (node.getNodeName().equalsIgnoreCase("tmt_pns")) {
						try {
							dosen.setTmtPns(Common.databaseDateFormat.get().parse(node.getTextContent().trim()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:1196");

						}
					}

					if (node.getNodeName().equalsIgnoreCase("a_lisensi_kepsek")) {
						dosen.setaLisensiKepsek(node.getTextContent().trim().equals("1"));
					}

					if (node.getNodeName().equalsIgnoreCase("jml_sekolah_binaan")) {
						dosen.setJmlSekolahBinaan(Integer.parseInt(node.getTextContent().trim()));
					}

					if (node.getNodeName().equalsIgnoreCase("a_diklat_awas")) {
						dosen.setaDiklatAwas(node.getTextContent().trim().equals("1"));
					}

					if (node.getNodeName().equalsIgnoreCase("akta_ijin_ajar")) {
						dosen.setAktaIjinAjar((node.getTextContent().trim()));
					}

					if (node.getNodeName().equalsIgnoreCase("nira")) {
						dosen.setNira((node.getTextContent().trim()));
					}

					if (node.getNodeName().equalsIgnoreCase("a_braille")) {
						dosen.setaBraille(node.getTextContent().trim().equals("1"));
					}

					if (node.getNodeName().equalsIgnoreCase("a_bhs_isyarat")) {
						dosen.setaBhsIsyarat(node.getTextContent().trim().equals("1"));
					}

					if (node.getNodeName().equalsIgnoreCase("npwp")) {
						dosen.setNpwp((node.getTextContent().trim()));
					}

					if (node.getNodeName().equalsIgnoreCase("kewarganegaraan")) {
						biodataDosen.setKewarganegaraanFeeder((node.getTextContent().trim()));
					}

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			biodataDosen.setDosen(dosen);

			return biodataDosen;
		} else {
			return null;
		}
	}

	/** Mengonversi satu elemen data penugasan mengajar dosen (ajar dosen) Feeder menjadi entitas {@link PenugasanDosenMengajar}, me-resolve dosen dan mata kuliah terkait lewat {@code session} berdasarkan kode Feeder. Mengembalikan {@code null} bila node kosong. */
	public static PenugasanDosenMengajar penugasanDosenMengajar(Node result, Session session) {
		if (result.hasChildNodes()) {
			PenugasanDosenMengajar penugasanDosenMengajar = new PenugasanDosenMengajar();
			NodeList nodeList = result.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node.getNodeName().equalsIgnoreCase("id_reg_ptk")) {
					penugasanDosenMengajar.setFeeder((node.getTextContent().trim()));
				}
				if (node.getNodeName().equalsIgnoreCase("id_ptk")) {
					penugasanDosenMengajar
							.setDosen(FeederUtil.getDataByFeeder(session, node.getTextContent().trim(), Dosen.class));
				}
				if (node.getNodeName().equalsIgnoreCase("id_sms")) {
					penugasanDosenMengajar
							.setJurusan(FeederUtil.getDataByFeeder(session, node.getTextContent().trim(), Jurusan.class,
									Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
				}
				if (node.getNodeName().equalsIgnoreCase("no_srt_tgs")) {
					penugasanDosenMengajar.setKode(node.getTextContent().trim());
				}

				if (node.getNodeName().equalsIgnoreCase("tgl_srt_tgs")) {
					try {
						penugasanDosenMengajar
								.setTanggalSuratTugas(Common.databaseDateFormat.get().parse(node.getTextContent().trim()));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:1276");

					}
				}

				if (node.getNodeName().equalsIgnoreCase("id_thn_ajaran")) {
					try {
						penugasanDosenMengajar.setTahunAkademik(Integer.parseInt(node.getTextContent().trim()) + "/"
								+ (1 + Integer.parseInt(node.getTextContent().trim())));
						penugasanDosenMengajar.setSemester(Perkuliahan.GANJIL);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:1286");

					}
				}

				if (node.getNodeName().equalsIgnoreCase("tmt_srt_tgs")) {
					try {
						penugasanDosenMengajar
								.setTmtSuratTugas(Common.databaseDateFormat.get().parse(node.getTextContent().trim()));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:1295");

					}
				}
			}

			return penugasanDosenMengajar;
		} else {
			return null;
		}
	}

	/** Utilitas umum: mencari anak node dari {@code result} yang nama tag-nya cocok (case-insensitive) dengan {@code key} dan mengembalikan teks isinya (di-trim); mengembalikan {@code null} bila tidak ditemukan atau node kosong. Dipakai untuk membaca satu field XML Feeder tanpa perlu menulis method konversi entitas penuh. */
	public static String value(Node result, String key) {
		if (result.hasChildNodes()) {
			NodeList nodeList = result.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				try {
					Node node = nodeList.item(i);
					if (node.getTextContent() == null || node.getTextContent().trim().isEmpty()) {
						continue;
					}
					if (node.getNodeName().equalsIgnoreCase(key)) {
						return node.getTextContent().trim();
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConverter.java:1319");

				}
			}
		}
		return null;
	}
}

package ais.action.master.resources;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.spi.resource.Singleton;

import ais.action.master.feeder.util.FeederExporterGenerator;
import ais.action.master.resources.model.CommonID;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Kegiatan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.PenugasanDosenMengajar;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Skripsi;

@Path("/feeder")
@Singleton

/**
 * Titik akhir REST (Jersey/JAX-RS) untuk integrasi PDDikti Feeder: menyediakan data akademik dalam
 * format yang siap dipetakan ke skema Feeder (mahasiswa baru/lama, mata kuliah, kurikulum, dosen,
 * perkuliahan/KRS per kelas maupun per mahasiswa, nilai transfer, dan AKM/angka kredit mengajar per
 * angkatan), serta satu endpoint penulisan generik ({@link #update}) yang menandai baris data lokal
 * dengan id hasil sinkronisasi Feeder ({@code feeder}/{@code id_reg_pd}/{@code feeder_kode}, tergantung
 * tabel). Sebagian besar parameter path (nama, fakultas, jurusan, dsb.) di-URL-decode setelah karakter
 * {@code "_"} dibuang, mengikuti pola encoding khusus yang dipakai skrip sinkronisasi Feeder.
 *
 * <p>
 * <b>Catatan keamanan:</b>
 * </p>
 * <ul>
 * <li><b>CELAH SQL INJECTION DITUTUP (2026-09-01):</b> {@link #update(String, String, String, String)}
 * untuk cabang tabel {@code "penugasan_dosen_mengajar"}, {@code "detailperkuliahan"}, dan
 * {@code "nilai_transfer"}/{@code "nilaitransfer"} sebelumnya menyusun perintah SQL dengan
 * MENYAMBUNG LANGSUNG parameter {@code data1}/{@code data2}/{@code key} dari URL ke dalam string
 * SQL — celah SQL injection yang jelas, dieksploitasi lewat parameter path URL tanpa perlu login
 * sama sekali. Ketiga cabang ini kini memakai parameter binding ({@code SQLQuery#setParameter})
 * alih-alih konkatenasi string; perilaku update untuk pemanggil yang sah tidak berubah.</li>
 * <li><b>MASIH TERBUKA — TIDAK ADA AUTENTIKASI:</b> {@link #update(String, String, String, String)}
 * dan seluruh endpoint GET lain di kelas ini (data mahasiswa/dosen/matakuliah/kurikulum/
 * perkuliahan/nilai) masih TIDAK memeriksa autentikasi/otorisasi apa pun — siapa pun yang
 * mengetahui URL dapat memanggilnya, termasuk memicu {@code update} untuk baris data mana pun
 * (meski sudah tidak lagi rentan SQL injection). Ini sengaja TIDAK ditambal pada perbaikan ini
 * karena menambah pemeriksaan kredensial baru berisiko memutus integrasi sinkronisasi PDDikti
 * Feeder yang sudah berjalan tanpa koordinasi dengan pemanggilnya; direkomendasikan menambahkan
 * pembatasan di level jaringan (IP allow-list/reverse proxy) atau kredensial bersama, dikoordinasikan
 * dengan pemilik skrip sinkronisasi Feeder.</li>
 * </ul>
 */
/**
 * Tipe khusus untuk feeder resource. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code getSystemTime()}, {@code
 * getMahasiswa()}, {@code getMahasiswa()}, {@code getMahasiswaBaru()}, {@code getMatakuliah()}, {@code
 * getMatakuliah_Baru()}); mutasi data ({@code update()}); operasi domain lain ({@code convertMahasiswa()},
 * {@code convertRiwayatMahasiswa()}, {@code convertRiwayatMahasiswaLulus()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 */
public class FeederResource {

	/** Mengembalikan waktu server saat ini (epoch millis). */
	public long getSystemTime() {
		return System.currentTimeMillis();
	}

	@GET
	@Path("test/{nama}")
	@Produces({ MediaType.APPLICATION_JSON })
	/** Endpoint uji coba sederhana yang menggemakan (echo) parameter {@code nama} yang diberikan. */
	public CommonID getMahasiswa(@PathParam("nama") String nama) {
		CommonID commonID = new CommonID();
		commonID.setInfo1(nama);
		return commonID;
	}

	@GET
	@Path("update/{tabel}/{key}/{data1}/{data2}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Menandai satu baris data lokal sebagai sudah disinkronkan ke PDDikti Feeder: tabel target
	 * dipilih lewat percabangan string {@code tabel} (mahasiswa/mahasiswabynim/matakuliah/kurikulum/
	 * kurikulum_punya_matakuliah/penugasan_dosen_mengajar/perkuliahan/detailperkuliahan/
	 * nilai_transfer/nilaitransfer, dan kemungkinan tabel lain — lihat badan method untuk daftar
	 * lengkap), menuliskan {@code data1}/{@code data2} ke kolom {@code feeder}/{@code id_reg_pd}/
	 * {@code feeder_kode} sesuai tabel.
	 *
	 * <p>
	 * <b>PERINGATAN KEAMANAN:</b> tidak ada pemeriksaan autentikasi (lihat catatan keamanan pada
	 * javadoc kelas — sengaja belum ditambal pada perbaikan ini). Celah SQL injection yang
	 * sebelumnya ada pada cabang {@code "penugasan_dosen_mengajar"}/{@code "detailperkuliahan"}/
	 * {@code "nilai_transfer"} (konkatenasi string langsung ke SQL) SUDAH DITUTUP — ketiga cabang
	 * itu kini memakai parameter binding ({@code SQLQuery#setParameter}).
	 * </p>
	 *
	 * @param tabel nama tabel/entitas tujuan (menentukan cabang logika yang dijalankan)
	 * @param key   id baris tujuan (atau NIM untuk cabang {@code mahasiswabynim})
	 * @param data1 nilai utama yang dituliskan (makna tergantung tabel, mis. kode Feeder)
	 * @param data2 nilai kedua yang dituliskan (makna tergantung tabel, mis. id_reg_pd)
	 * @return status hasil (jumlah baris terpengaruh via {@code info1})
	 */
	public CommonID update(@PathParam("tabel") String tabel, @PathParam("key") String key,
			@PathParam("data1") String data1, @PathParam("data2") String data2) {

		Session session = HibernateUtil.currentNativeSession();
		try {

			tabel = URLDecoder.decode(tabel.replaceAll("_", ""), "UTF-8");
			key = URLDecoder.decode(key.replaceAll("_", ""), "UTF-8");
			data1 = URLDecoder.decode(data1.replaceAll("_", ""), "UTF-8");
			data2 = URLDecoder.decode(data2.replaceAll("_", ""), "UTF-8");

			System.out.println("tabel = " + tabel + ", key = " + key + ", data1 => " + data1 + ", data2 => " + data2);

			String sql = "";
			int hasil = 0;

			if (tabel.equalsIgnoreCase("mahasiswa")) {
				Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), Long.parseLong(key));
				if (mahasiswa != null) {
					session.refresh(mahasiswa);
					mahasiswa.setFeeder(data1);
					mahasiswa.setIdRegPd(data2);

					session.getTransaction().begin();
					Common.refreshUpdate(session, mahasiswa);
					session.getTransaction().commit();
					hasil = 1;
				}
			} else if (tabel.equalsIgnoreCase("mahasiswabynim")) {
				Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.ambilByNim(key);
				if (mahasiswa != null) {
					session.refresh(mahasiswa);
					mahasiswa.setFeeder(data1);
					mahasiswa.setIdRegPd(data2);

					session.getTransaction().begin();
					Common.refreshUpdate(session, mahasiswa);
					session.getTransaction().commit();
					hasil = 1;
				}
			} else if (tabel.equalsIgnoreCase("matakuliah")) {

				Matakuliah matakuliah = (Matakuliah) ConstantValues.ambil(Matakuliah.class.getName(),
						Long.parseLong(key));
				if (matakuliah != null) {
					session.refresh(matakuliah);
					matakuliah.setFeeder(data1);

					session.getTransaction().begin();
					Common.refreshUpdate(session, matakuliah);
					session.getTransaction().commit();
					hasil = 1;
				}

			} else if (tabel.equalsIgnoreCase("kurikulum")) {

				Kurikulum kurikulum = (Kurikulum) ConstantValues.ambil(Kurikulum.class.getName(), Long.parseLong(key));
				if (kurikulum != null) {
					session.refresh(kurikulum);
					kurikulum.setFeeder(data1);

					session.getTransaction().begin();
					Common.refreshUpdate(session, kurikulum);
					session.getTransaction().commit();
					hasil = 1;
				}

			} else if (tabel.equalsIgnoreCase("kurikulum_punya_matakuliah")) {

				KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) ConstantValues
						.ambil(KurikulumPunyaMatakuliah.class.getName(), Long.parseLong(key));
				if (kurikulumPunyaMatakuliah != null) {
					session.refresh(kurikulumPunyaMatakuliah);
					kurikulumPunyaMatakuliah.setFeeder(data1);

					session.getTransaction().begin();
					Common.refreshUpdate(session, kurikulumPunyaMatakuliah);
					session.getTransaction().commit();
					hasil = 1;
				}

			} else if (tabel.equalsIgnoreCase("penugasan_dosen_mengajar")) {
				sql = "update penugasan_dosen_mengajar set feeder=:data1 where id=:key";
				hasil = session.createSQLQuery(sql).setParameter("data1", data1)
						.setParameter("key", Long.parseLong(key.trim())).executeUpdate();
			}

			else if (tabel.equalsIgnoreCase("perkuliahan")) {

				Perkuliahan perkuliahan = (Perkuliahan) ConstantValues.ambil(Perkuliahan.class.getName(),
						Long.parseLong(key));
				if (perkuliahan != null) {
					session.refresh(perkuliahan);
					perkuliahan.setFeeder(data1);

					session.getTransaction().begin();
					Common.refreshUpdate(session, perkuliahan);
					session.getTransaction().commit();
					hasil = 1;
				}

			} else if (tabel.equalsIgnoreCase("detailperkuliahan")) {
				sql = "update detailperkuliahan set id_kls=:data1, id_reg_pd = :data2 where id=:key";
				hasil = session.createSQLQuery(sql).setParameter("data1", data1).setParameter("data2", data2)
						.setParameter("key", Long.parseLong(key.trim())).executeUpdate();
			} else if (tabel.equalsIgnoreCase("nilai_transfer") || tabel.equalsIgnoreCase("nilaitransfer")) {
				sql = "update detailperkuliahan set feeder_kode=:data1 where id=:key";
				hasil = session.createSQLQuery(sql).setParameter("data1", data1)
						.setParameter("key", Long.parseLong(key.trim())).executeUpdate();
			}

			System.out.println("sql = " + sql + ", hasil => " + hasil);

			CommonID commonID = new CommonID();
			commonID.setInfo1(hasil + "");
			commonID.setInfo2(sql);

			HibernateUtil.closeSession();
			return commonID;
		} catch (Exception e) {
			HibernateUtil.closeSession();
			Common.tampilErrorJikaAdmin(e);
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}

	@GET
	@Path("mahasiswa/{nama}/{ta}/{fakultas}/{jurusan}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Mengambil data mahasiswa (existing) yang cocok dengan filter nama/tahun akademik/fakultas/
	 * jurusan, diformat siap dipetakan ke skema PDDikti Feeder. Tidak memeriksa autentikasi.
	 *
	 * @param nama     filter nama mahasiswa (URL-encoded, garis bawah di-strip sebelum decode)
	 * @param ta       tahun akademik/angkatan filter
	 * @param fakultas filter fakultas (URL-encoded)
	 * @param jurusan  filter jurusan (URL-encoded)
	 * @return data mahasiswa yang cocok dalam struktur {@link CommonID}
	 */
	public CommonID getMahasiswa(@PathParam("nama") String nama, @PathParam("ta") String ta,
			@PathParam("fakultas") String fakultas, @PathParam("jurusan") String jurusan) {
		Session session = HibernateUtil.currentNativeSession();
		try {

			System.out.println(
					"jurusan = " + jurusan + ", fakultas = " + fakultas + ", ta => " + ta + ", nama => " + nama);

			fakultas = URLDecoder.decode(fakultas.replaceAll("_", ""), "UTF-8");
			jurusan = URLDecoder.decode(jurusan.replaceAll("_", ""), "UTF-8");
			ta = URLDecoder.decode(ta.replaceAll("_", ""), "UTF-8");
			nama = URLDecoder.decode(nama.replaceAll("_", ""), "UTF-8");

			Integer tahunangkatan = ta == null || ta.isEmpty() ? null : Integer.parseInt(ta.trim());
			@SuppressWarnings("unchecked")
			List<Mahasiswa> mahasiswas = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

					.add(nama == null || nama.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("nim", nama, MatchMode.ANYWHERE),
									Restrictions.ilike("nama", nama, MatchMode.ANYWHERE)))

					.add(tahunangkatan == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("tahunangkatan", tahunangkatan))

					.add(Restrictions.ne("nim", "")).add(Restrictions.ne("nama", ""))

					.createAlias("jurusan", "jurusan").createAlias("jurusan.fakultas", "fakultas")
					.add(fakultas == null || fakultas.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("fakultas.nama", fakultas.trim(), MatchMode.ANYWHERE))

					.add(jurusan == null || jurusan.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("jurusan.nama", jurusan.trim(), MatchMode.ANYWHERE),
									Restrictions.or(
											Restrictions.ilike("jurusan.kode", jurusan.trim(), MatchMode.ANYWHERE),
											Restrictions.ilike("jurusan.kodeEpsbed", jurusan.trim(),
													MatchMode.ANYWHERE))))

					.createAlias("fakultas.perguruanTinggi", "perguruanTinggi")
					.add(Restrictions.isNotNull("jurusan.feeder")).add(Restrictions.ne("jurusan.feeder", ""))
					.add(Restrictions.isNotNull("perguruanTinggi.feeder"))
					.add(Restrictions.ne("perguruanTinggi.feeder", "")).addOrder(Order.desc("nim")).list();

			System.out.println("mahasiswas size => " + mahasiswas.size() + ", jurusan = " + jurusan + ", fakultas = "
					+ fakultas + ", tahunangkatan => " + tahunangkatan + ", nama => " + nama);

			JSONArray array = new JSONArray();
			JSONArray arrayPt = new JSONArray();
			for (Mahasiswa mahasiswa : mahasiswas) {
				BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();

				JSONObject jsonObject = FeederExporterGenerator.mahasiswa(mahasiswa, biodataMahasiswa);
				JSONObject json = new JSONObject();
				json.put("data", jsonObject);
				json.put("class", Mahasiswa.class.getName());
				json.put("keyName", "id_pd");
				json.put("id", mahasiswa.getId());
				if (mahasiswa.getFeeder() != null && !mahasiswa.getFeeder().trim().isEmpty()) {
					Map<String, Object> dataKey = new HashMap<String, Object>();
					dataKey.put("id_pd", mahasiswa.getFeeder().trim());
					JSONObject jsonObjectKey = new JSONObject(dataKey);
					json.put("key", jsonObjectKey);
				}
				array.put(json);

				jsonObject = FeederExporterGenerator.mahasiswa_pt(mahasiswa, biodataMahasiswa, session);
				jsonObject.put("nipd", Common.maxPanjang(mahasiswa.getNim(), 18));

				json = new JSONObject();
				json.put("class", Mahasiswa.class.getName());
				json.put("id", mahasiswa.getId());
				json.put("keyName", "id_reg_pd");
				json.put("data", jsonObject);
				if (mahasiswa.getIdRegPd() != null && !mahasiswa.getIdRegPd().trim().isEmpty()) {
					Map<String, Object> dataKey = new HashMap<String, Object>();
					dataKey.put("id_reg_pd", mahasiswa.getIdRegPd().trim());
					JSONObject jsonObjectKey = new JSONObject(dataKey);
					json.put("key", jsonObjectKey);
				}
				arrayPt.put(json);
			}

			CommonID commonID = new CommonID();
			commonID.setInfo1(array.toString());

			commonID.setInfo2(arrayPt.toString());

			HibernateUtil.closeSession();

			return commonID;
		} catch (Exception e) {
			HibernateUtil.closeSession();
			Common.tampilErrorJikaAdmin(e);
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}

	public static JSONObject convertMahasiswa(Mahasiswa mahasiswa) {
		BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();

		JSONObject data = new JSONObject();
		try {
			data.put("email", Common.maxPanjang(biodataMahasiswa.getEmail().split(",")[0], 50));
			data.put("id_kebutuhan_khusus_ibu", 0);
			data.put("id_kebutuhan_khusus_mahasiswa", 0);
			data.put("id_kebutuhan_khusus_ayah", 0);
			data.put("nama_mahasiswa", Common.maxPanjang(mahasiswa.getNama(), 100));
			data.put("jenis_kelamin", mahasiswa.getKelamin() == null ? "*"
					: mahasiswa.getKelamin().trim().equalsIgnoreCase("Laki-laki") ? "L" : "P");
			data.put("tempat_lahir", Common.maxPanjang(mahasiswa.getTempatlahir(), 50));
			if (mahasiswa.getTanggallahir() != null) {
				data.put("tanggal_lahir", Common.databaseDateFormat11.get().format(mahasiswa.getTanggallahir()));
			}
			if (biodataMahasiswa.getAgama() != null) {
				data.put("id_agama", biodataMahasiswa.getAgama().getFeeder());
			} else if (mahasiswa.getAgama() != null) {
				data.put("id_agama", mahasiswa.getAgama().getFeeder());
			} else {
				data.put("id_agama", 98);
			}

			if (!biodataMahasiswa.getNoIdentitas().isEmpty())
				data.put("nik", Common.maxPanjang(biodataMahasiswa.getNoIdentitas(), 16));

			if (!biodataMahasiswa.getNisn().isEmpty())
				data.put("nisn", Common.maxPanjang(biodataMahasiswa.getNisn(), 16));

			if (!biodataMahasiswa.getNpwp().isEmpty())
				data.put("npwp", Common.maxPanjang(biodataMahasiswa.getNpwp(), 16));

			data.put("kewarganegaraan",
					mahasiswa.getNegara() == null || mahasiswa.getNegara().getNama() == null
							|| mahasiswa.getNegara().getNama().trim().equalsIgnoreCase("Indonesia") ? "ID"
									: mahasiswa.getNegara().getKode());

			data.put("jalan", Common.maxPanjang(mahasiswa.getAlamat(), 80));

			data.put("rt", Common.maxPanjangNumeric(biodataMahasiswa.getRt(), 2));

			data.put("rw", Common.maxPanjangNumeric(biodataMahasiswa.getRw(), 2));
			data.put("dusun", Common.maxPanjang(biodataMahasiswa.getDusun(), 60));
			data.put("kelurahan", Common.maxPanjang(biodataMahasiswa.getKelurahan(), 60));
			data.put("kode_pos", Common.maxPanjang(biodataMahasiswa.getKodepos(), 5));

			if (biodataMahasiswa.getKecamatan() != null && biodataMahasiswa.getKecamatan().getFeeder() != null
					&& !biodataMahasiswa.getKecamatan().getFeeder().trim().isEmpty()) {
				data.put("id_wilayah", biodataMahasiswa.getKecamatan().getFeeder());
			} else {
				data.put("id_wilayah", "000000");
			}

			if (biodataMahasiswa.getJenisTinggalMahasiswa() != null
					&& biodataMahasiswa.getJenisTinggalMahasiswa().getFeeder() != null) {
				data.put("id_jenis_tinggal", biodataMahasiswa.getJenisTinggalMahasiswa().getFeeder());
			}

			if (biodataMahasiswa.getAlatTransportasiMahasiswa() != null
					&& biodataMahasiswa.getAlatTransportasiMahasiswa().getFeeder() != null) {
				data.put("id_alat_transportasi", biodataMahasiswa.getAlatTransportasiMahasiswa().getFeeder());
			}

			data.put("telepon", Common.maxPanjang(biodataMahasiswa.getTeleponRumah(), 20));
			data.put("handphone", Common.maxPanjang(biodataMahasiswa.getHp(), 20));

			data.put("penerima_kps", 0);

			data.put("nik_ayah", Common.maxPanjang(biodataMahasiswa.getNikAyah(), 16));
			data.put("nama_ayah", Common.maxPanjang(biodataMahasiswa.getNamaAyah(), 50));
			if (biodataMahasiswa.getTanggalLahirAyah() != null) {
				data.put("tanggal_lahir_ayah",
						Common.databaseDateFormat11.get().format(biodataMahasiswa.getTanggalLahirAyah()));
			}

			if (biodataMahasiswa.getJenjangPendidikanAyah() != null
					&& biodataMahasiswa.getJenjangPendidikanAyah().getFeeder() != null) {
				data.put("id_pendidikan_ayah", biodataMahasiswa.getJenjangPendidikanAyah().getFeeder());
			}

			if (biodataMahasiswa.getJenisPekerjaanAyah() != null
					&& biodataMahasiswa.getJenisPekerjaanAyah().getFeeder() != null) {
				data.put("id_pekerjaan_ayah", biodataMahasiswa.getJenisPekerjaanAyah().getFeeder());
			}

			if (biodataMahasiswa.getJenisPenghasilanAyah() != null
					&& biodataMahasiswa.getJenisPenghasilanAyah().getFeeder() != null) {
				data.put("id_penghasilan_ayah", biodataMahasiswa.getJenisPenghasilanAyah().getFeeder());
			}

			data.put("nik_ibu", Common.maxPanjang(biodataMahasiswa.getNikIbu(), 16));
			data.put("nama_ibu_kandung", Common.maxPanjang(biodataMahasiswa.getNamaIbu(), 50));
			if (biodataMahasiswa.getTanggalLahirIbu() != null) {
				data.put("tanggal_lahir_ibu",
						Common.databaseDateFormat11.get().format(biodataMahasiswa.getTanggalLahirIbu()));
			}

			if (biodataMahasiswa.getJenjangPendidikanIbu() != null
					&& biodataMahasiswa.getJenjangPendidikanIbu().getFeeder() != null) {
				data.put("id_pendidikan_ibu", biodataMahasiswa.getJenjangPendidikanIbu().getFeeder());
			}

			if (biodataMahasiswa.getJenisPekerjaanIbu() != null
					&& biodataMahasiswa.getJenisPekerjaanIbu().getFeeder() != null) {
				data.put("id_pekerjaan_ibu", biodataMahasiswa.getJenisPekerjaanIbu().getFeeder());
			}

			if (biodataMahasiswa.getJenisPenghasilanIbu() != null
					&& biodataMahasiswa.getJenisPenghasilanIbu().getFeeder() != null) {
				data.put("id_penghasilan_ibu", biodataMahasiswa.getJenisPenghasilanIbu().getFeeder());
			}

			data.put("nama_wali", Common.maxPanjang(biodataMahasiswa.getNamaWali(), 30));
			if (biodataMahasiswa.getTanggalLahirWali() != null) {
				data.put("tanggal_lahir_wali",
						Common.databaseDateFormat11.get().format(biodataMahasiswa.getTanggalLahirWali()));
			}

			if (biodataMahasiswa.getJenjangPendidikanWali() != null
					&& biodataMahasiswa.getJenjangPendidikanWali().getFeeder() != null) {
				data.put("id_pendidikan_wali", biodataMahasiswa.getJenjangPendidikanWali().getFeeder());
			}

			if (biodataMahasiswa.getJenisPekerjaanWali() != null
					&& biodataMahasiswa.getJenisPekerjaanWali().getFeeder() != null) {
				data.put("id_pekerjaan_wali", biodataMahasiswa.getJenisPekerjaanWali().getFeeder());
			}

			if (biodataMahasiswa.getJenisPenghasilanWali() != null
					&& biodataMahasiswa.getJenisPenghasilanWali().getFeeder() != null) {
				data.put("id_penghasilan_wali", biodataMahasiswa.getJenisPenghasilanWali().getFeeder());
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/resources/FeederResource.java:431");
		}
		return data;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static JSONObject convertRiwayatMahasiswa(Mahasiswa mahasiswa) {
		// Mahasiswa Prodi
		JSONObject data = new JSONObject();
		try {
			data.put("nim", mahasiswa.getNim());
			data.put("id_mahasiswa", mahasiswa.getFeeder());
			if (mahasiswa.getStatusAwalMahasiswa() != null) {
				data.put("id_jenis_daftar", mahasiswa.getStatusAwalMahasiswa().getFeeder());
			}
			if (mahasiswa.getTahunangkatan() != null) {
				data.put("id_periode_masuk", mahasiswa.getTahunangkatan()
						+ (mahasiswa.getSemesterMulai().equals(Perkuliahan.GANJIL) ? "1" : "2"));
			}
			if (mahasiswa.getTanggalMasuk() != null) {
				data.put("tanggal_daftar", Common.databaseDateFormat11.get().format(mahasiswa.getTanggalMasuk()));
			}
			data.put("id_perguruan_tinggi", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getFeeder());
			data.put("id_prodi", mahasiswa.getJurusan().getFeeder());
			if (mahasiswa.getMerupakanPindahan()) {
				data.put("sks_diakui", mahasiswa.getSksYangDiakui());
			} else if (mahasiswa.getMerupakanAlihProdi()) {
				data.put("sks_diakui", mahasiswa.getSksYangDiakuiPindahProdi());
			}
			if (mahasiswa.getPindahanDari() != null) {
				data.put("id_perguruan_tinggi_asal", mahasiswa.getPindahanDari().getFeeder());
				data.put("nama_perguruan_tinggi_asal", Common.maxPanjang(mahasiswa.getPindahanDari().getNama(), 100));
			}
			if (mahasiswa.getNamaProdiPindah() != null && !mahasiswa.getNamaProdiPindah().isEmpty()) {
				data.put("nama_prodi_asal", Common.maxPanjang(mahasiswa.getNamaProdiPindah(), 100));
			}
			data.put("id_pembiayaan", mahasiswa.getJenisPembiayaanMahasiswa().getFeeder());

			if (mahasiswa.getJenisSeleksi() != null && mahasiswa.getJenisSeleksi().getFeeder() != null) {
				data.put("id_jalur_daftar", mahasiswa.getJenisSeleksi().getFeeder());
			}

			Session session = HibernateUtil.currentNativeSession();
			try {

				BiodataCalonMahasiswa calonMahasiswa = (BiodataCalonMahasiswa) session
						.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("mahasiswa", mahasiswa))
						.setMaxResults(1).uniqueResult();
				if (calonMahasiswa != null) {
					ArrayList detailBiayas = new ArrayList();
					java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtil.getInstance()
							.getDetailBiayaCalonMahasiswa(calonMahasiswa,
									ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU, mahasiswa.getJurusan(), 1, false);
					detailBiayas.addAll(detailBiayas1);

					int countPengaturanBulanan = PembayaranUtil.getInstance().countBulanan(session, calonMahasiswa,
							ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU, 1, detailBiayas, false, false);

					Collection biayaBulanan = null;
					if (countPengaturanBulanan > 0) {
						biayaBulanan = PembayaranUtil.getInstance().getPengaturanPembayaranSemua(calonMahasiswa,
								session, 1, ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU, detailBiayas, false,
								false);
					}
					Collection dataBTagihan = biayaBulanan != null ? biayaBulanan : detailBiayas;
					Double biaya = 0.0;
					for (Object o : dataBTagihan) {
						Kegiatan kegiatan = calonMahasiswa.ambilKegiatans(1,
								ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
						if (o instanceof PengaturanPembayaranBulanan) {
							PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
							Double jumlah = pengaturanPembayaranBulanan.getNominal();
							biaya += jumlah;
						} else if (o instanceof DetailBiaya) {
							DetailBiaya detailBiaya = (DetailBiaya) o;

							Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya, true);
							biaya += jumlah;
						}
					}
					data.put("biaya_masuk", biaya < 0.01 ? 1.0 : biaya);
				} else {
					int semester = 1;
					Collection detailBiayas = PembayaranUtil.getInstance().getDetailBiayaMahasiswa(mahasiswa, semester,
							ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, false);

					int countPengaturanBulanan = PembayaranUtil.getInstance().countBulanan(session, mahasiswa,
							ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, semester, detailBiayas, false, false);

					if (countPengaturanBulanan > 0) {

						detailBiayas = PembayaranUtil.getInstance().getDetailBiayaMahasiswa(mahasiswa, semester,
								ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, countPengaturanBulanan > 0 ? "-1" : null,
								true, false);

					}

					if (!detailBiayas.isEmpty()) {
						Kegiatan kegiatan = mahasiswa.ambilKegiatans(semester,
								ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);
						Collection<DetailKegiatan> detailKegiatans = kegiatan == null || kegiatan.getId() == null ? null
								: kegiatan.ambilDetailKegiatan(true);
						Double biaya = 0.0;
						for (Object o : detailBiayas) {
							if (o instanceof PengaturanPembayaranBulanan) {
								PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
								Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailKegiatans, mahasiswa,
										semester, pengaturanPembayaranBulanan);
								biaya += jumlah;
							} else if (o instanceof DetailBiaya) {
								DetailBiaya detailBiaya = (DetailBiaya) o;

								Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya, true);
								biaya += jumlah;
							}
						}
						data.put("biaya_masuk", biaya < 0.01 ? 1.0 : biaya);
					} else {
						data.put("biaya_masuk", 1.0);
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/resources/FeederResource.java:553");
				data.put("biaya_masuk", 1.0);
			}
			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSession();

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/resources/FeederResource.java:561");
		}
		return data;
	}

	@SuppressWarnings({})
	public static JSONObject convertRiwayatMahasiswaLulus(Mahasiswa mahasiswa) {
		// Mahasiswa Prodi
		JSONObject data = new JSONObject();
		try {
			data.put("id_registrasi_mahasiswa", mahasiswa.getIdRegPd());
			data.put("keterangan", mahasiswa.getKeterangan());
			data.put("id_jenis_keluar", mahasiswa.getStatusKeluar().getFeeder());
			if (mahasiswa.getTanggalLulus() != null) {
				data.put("tanggal_keluar", Common.databaseDateFormat11.get().format(mahasiswa.getTanggalLulus()));
			} else {
				data.put("tanggal_keluar", "");
			}

			if (mahasiswa.getKelompokStatusKeluarMahasiswa() != null) {
				String tahunAkademikMulai = mahasiswa.getKelompokStatusKeluarMahasiswa().getTahunAkademik()
						.split("/")[0];
				data.put("id_periode_keluar", tahunAkademikMulai
						+ (mahasiswa.getKelompokStatusKeluarMahasiswa().getSemester().equals(Perkuliahan.GENAP) ? "2"
								: "1"));
			} else if (mahasiswa.getSemesterLulus() != null) {

				Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
				Integer semesterMulai = mahasiswa.getPindahKeKampusIniMasukSemester();
				Integer tahunAkademikMulai = Common.getTahunAkademik(mahasiswa.getSemesterLulus(), tahunAngkatanMhs,
						semesterMulai, mahasiswa.getSemesterMulai());

				data.put("id_periode_keluar", tahunAkademikMulai + (mahasiswa.getSemesterLulus() % 2 == 0 ? "2" : "1"));
			}

			data.put("nomor_sk_yudisium", Common.maxPanjang(
					mahasiswa.getNoAkta2().isEmpty() ? mahasiswa.getNoAkta1() : mahasiswa.getNoAkta2(), 30));

			if (mahasiswa.getTanggalSkRektor() != null) {
				data.put("tanggal_sk_yudisium", Common.databaseDateFormat.get().format(mahasiswa.getTanggalSkRektor()));
			} else if (mahasiswa.getTanggalYudisium() != null) {
				data.put("tanggal_sk_yudisium", Common.databaseDateFormat.get().format(mahasiswa.getTanggalYudisium()));
			} else {
				data.put("tanggal_sk_yudisium", "");
			}

			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);
			if (krsMahasiswa != null && krsMahasiswa.getIpk() != null) {
				data.put("ipk", krsMahasiswa.getIpk());
			} else {
				data.put("ipk", 0.0);
			}

			data.put("nomor_ijazah", Common.maxPanjang(mahasiswa.getNoIjazah1(), 40));

			Session session = HibernateUtil.currentNativeSession();
			Skripsi skripsi = (Skripsi) session.createCriteria(Skripsi.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa)).addOrder(Order.desc("id")).setMaxResults(1)
					.uniqueResult();

			if (skripsi != null) {
				data.put("judul_skripsi", Common.maxPanjang(skripsi.getJudul(), 500));

				if (skripsi.getAwalBimbingan() != null) {
					data.put("bulan_awal_bimbingan", Common.databaseDateFormat.get().format(skripsi.getAwalBimbingan()));
				} else {
					if (mahasiswa.getBlnAwalBimbingan() != null) {
						data.put("bulan_awal_bimbingan",
								Common.databaseDateFormat.get().format(mahasiswa.getBlnAwalBimbingan()));
					} else {
						data.put("bulan_awal_bimbingan", "");
					}
				}

				if (skripsi.getAkhirBimbingan() != null) {
					data.put("bulan_akhir_bimbingan", Common.databaseDateFormat.get().format(skripsi.getAkhirBimbingan()));
				} else {
					if (mahasiswa.getBlnAkhirBimbingan() != null) {
						data.put("bulan_akhir_bimbingan",
								Common.databaseDateFormat.get().format(mahasiswa.getBlnAkhirBimbingan()));
					} else {
						data.put("bulan_akhir_bimbingan", "");
					}
				}

			} else {
				data.put("judul_skripsi", Common.maxPanjang(mahasiswa.getJudulSkripsi(), 500));

				if (mahasiswa.getBlnAwalBimbingan() != null) {
					data.put("bulan_awal_bimbingan", Common.databaseDateFormat.get().format(mahasiswa.getBlnAwalBimbingan()));
				} else {
					data.put("bulan_awal_bimbingan", "");
				}

				if (mahasiswa.getBlnAkhirBimbingan() != null) {
					data.put("bulan_akhir_bimbingan",
							Common.databaseDateFormat.get().format(mahasiswa.getBlnAkhirBimbingan()));
				} else {
					data.put("bulan_akhir_bimbingan", "");
				}
			}

			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSession();

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/resources/FeederResource.java:668");
		}
		return data;
	}

	@SuppressWarnings({ "unchecked" })
	@GET
	@Path("mahasiswa_baru/{nama}/{ta}/{fakultas}/{jurusan}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Seperti {@link #getMahasiswa(String, String, String, String)}, dikhususkan untuk mahasiswa
	 * BARU (belum tersinkron ke Feeder) yang cocok dengan filter. Tidak memeriksa autentikasi.
	 *
	 * @param nama     filter nama mahasiswa (URL-encoded)
	 * @param ta       tahun akademik/angkatan filter
	 * @return data mahasiswa baru yang cocok dalam struktur {@link CommonID}
	 */
	public CommonID getMahasiswaBaru(@PathParam("nama") String nama, @PathParam("ta") String ta,
			@PathParam("fakultas") String fakultas, @PathParam("jurusan") String jurusan) {
		Session session = HibernateUtil.currentNativeSession();
		try {

			System.out.println(
					"jurusan = " + jurusan + ", fakultas = " + fakultas + ", ta => " + ta + ", nama => " + nama);

			fakultas = URLDecoder.decode(fakultas.replaceAll("_", ""), "UTF-8");
			jurusan = URLDecoder.decode(jurusan.replaceAll("_", ""), "UTF-8");
			ta = URLDecoder.decode(ta.replaceAll("_", ""), "UTF-8");
			nama = URLDecoder.decode(nama.replaceAll("_", ""), "UTF-8");

			Integer tahunangkatan = ta == null || ta.isEmpty() ? null : Integer.parseInt(ta.trim());
			List<Mahasiswa> mahasiswas = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

					.add(nama == null || nama.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("nim", nama, MatchMode.ANYWHERE),
									Restrictions.ilike("nama", nama, MatchMode.ANYWHERE)))

					.add(tahunangkatan == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("tahunangkatan", tahunangkatan))

					.add(Restrictions.ne("nim", "")).add(Restrictions.ne("nama", ""))

					.createAlias("jurusan", "jurusan").createAlias("jurusan.fakultas", "fakultas")
					.add(fakultas == null || fakultas.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("fakultas.nama", fakultas.trim(), MatchMode.ANYWHERE))
					.add(jurusan == null || jurusan.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("jurusan.nama", jurusan.trim(), MatchMode.ANYWHERE),
									Restrictions.or(
											Restrictions.ilike("jurusan.kode", jurusan.trim(), MatchMode.ANYWHERE),
											Restrictions.ilike("jurusan.kodeEpsbed", jurusan.trim(),
													MatchMode.ANYWHERE))))

					.createAlias("fakultas.perguruanTinggi", "perguruanTinggi")
					.add(Restrictions.isNotNull("jurusan.feeder")).add(Restrictions.ne("jurusan.feeder", ""))
					.add(Restrictions.isNotNull("perguruanTinggi.feeder"))
					.add(Restrictions.ne("perguruanTinggi.feeder", "")).addOrder(Order.desc("nim")).list();

			System.out.println("mahasiswas size => " + mahasiswas.size() + ", jurusan = " + jurusan + ", fakultas = "
					+ fakultas + ", tahunangkatan => " + tahunangkatan + ", nama => " + nama);

			JSONArray array = new JSONArray();
			JSONArray array1 = new JSONArray();

			for (Mahasiswa mahasiswa : mahasiswas) {
				JSONObject data = convertMahasiswa(mahasiswa);

				JSONObject json = new JSONObject();
				json.put("data", data);
				json.put("class", Mahasiswa.class.getName());
				json.put("keyName", "id_pd");
				json.put("id", mahasiswa.getId());
				if (mahasiswa.getIdRegPd() != null && !mahasiswa.getIdRegPd().trim().isEmpty()) {
					Map<String, Object> dataKey = new HashMap<String, Object>();
					dataKey.put("id_reg_pd", mahasiswa.getIdRegPd().trim());
					dataKey.put("id_pd", mahasiswa.getFeeder().trim());
					JSONObject jsonObjectKey = new JSONObject(dataKey);
					json.put("key", jsonObjectKey);
				}
				array.put(json);

				data = convertRiwayatMahasiswa(mahasiswa);

				json = new JSONObject();
				json.put("data", data);
				json.put("class", Mahasiswa.class.getName());
				json.put("keyName", "id_reg_pd");
				json.put("id", mahasiswa.getId());
				if (mahasiswa.getIdRegPd() != null && !mahasiswa.getIdRegPd().trim().isEmpty()) {
					Map<String, Object> dataKey = new HashMap<String, Object>();
					dataKey.put("id_reg_pd", mahasiswa.getIdRegPd().trim());
					dataKey.put("id_pd", mahasiswa.getFeeder().trim());
					JSONObject jsonObjectKey = new JSONObject(dataKey);
					json.put("key", jsonObjectKey);
				}
				array1.put(json);
			}

			CommonID commonID = new CommonID();
			commonID.setInfo1(array.toString());
			commonID.setInfo2(array1.toString());

			HibernateUtil.closeSession();

			return commonID;
		} catch (Exception e) {
			HibernateUtil.closeSession();
			Common.tampilErrorJikaAdmin(e);
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}

	@GET
	@Path("matakuliah/{nama}/{fakultas}/{jurusan}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Mengambil data mata kuliah (existing) yang cocok dengan filter nama/fakultas/jurusan, diformat
	 * siap dipetakan ke skema PDDikti Feeder. Tidak memeriksa autentikasi.
	 *
	 * @param nama     filter nama mata kuliah (URL-encoded)
	 * @param fakultas filter fakultas (URL-encoded)
	 * @return data mata kuliah yang cocok dalam struktur {@link CommonID}
	 */
	public CommonID getMatakuliah(@PathParam("nama") String nama, @PathParam("fakultas") String fakultas,
			@PathParam("jurusan") String jurusan) {
		Session session = HibernateUtil.currentNativeSession();
		try {

			System.out.println("jurusan = " + jurusan + ", fakultas = " + fakultas + ", nama => " + nama);

			fakultas = URLDecoder.decode(fakultas.replaceAll("_", ""), "UTF-8");
			jurusan = URLDecoder.decode(jurusan.replaceAll("_", ""), "UTF-8");
			nama = URLDecoder.decode(nama.replaceAll("_", ""), "UTF-8");

			@SuppressWarnings("unchecked")
			List<Matakuliah> matakuliahs = session.createCriteria(Matakuliah.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(nama == null || nama.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("kode", nama, MatchMode.ANYWHERE),
									Restrictions.ilike("nama", nama, MatchMode.ANYWHERE)))

					.createAlias("jurusan", "jurusan").createAlias("jurusan.fakultas", "fakultas")
					.add(fakultas == null || fakultas.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("fakultas.nama", fakultas.trim(), MatchMode.ANYWHERE))
					.add(jurusan == null || jurusan.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("jurusan.nama", jurusan.trim(), MatchMode.ANYWHERE),
									Restrictions.or(
											Restrictions.ilike("jurusan.kode", jurusan.trim(), MatchMode.ANYWHERE),
											Restrictions.ilike("jurusan.kodeEpsbed", jurusan.trim(),
													MatchMode.ANYWHERE))))

					.add(Restrictions.ne("kode", "")).add(Restrictions.ne("nama", ""))
					.add(Restrictions.isNotNull("jurusan.feeder")).add(Restrictions.ne("jurusan.feeder", "")).list();

			System.out.println("matakuliahs size => " + matakuliahs.size() + ", jurusan = " + jurusan + ", fakultas = "
					+ fakultas + ", nama => " + nama);

			JSONArray array = new JSONArray();
			for (Matakuliah matakuliah : matakuliahs) {
				JSONObject jsonObject = FeederExporterGenerator.matakuliah(matakuliah);

				JSONObject json = new JSONObject();
				json.put("data", jsonObject);
				json.put("class", Matakuliah.class.getName());
				json.put("id", matakuliah.getId());
				json.put("keyName", "id_mk");
				if (matakuliah.getFeeder() != null && !matakuliah.getFeeder().trim().isEmpty()) {
					Map<String, Object> dataKey = new HashMap<String, Object>();
					dataKey.put("id_mk", matakuliah.getFeeder().trim());
					JSONObject jsonObjectKey = new JSONObject(dataKey);
					json.put("key", jsonObjectKey);
				}
				array.put(json);
			}

			CommonID commonID = new CommonID();
			commonID.setInfo1(array.toString());

			HibernateUtil.closeSession();

			return commonID;
		} catch (Exception e) {
			HibernateUtil.closeSession();
			Common.tampilErrorJikaAdmin(e);
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}

	@GET
	@Path("matakuliah_baru/{nama}/{fakultas}/{jurusan}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Seperti {@link #getMatakuliah(String, String, String)}, dikhususkan untuk mata kuliah BARU
	 * (belum tersinkron ke Feeder) yang cocok dengan filter. Tidak memeriksa autentikasi.
	 */
	public CommonID getMatakuliah_Baru(@PathParam("nama") String nama, @PathParam("fakultas") String fakultas,
			@PathParam("jurusan") String jurusan) {
		Session session = HibernateUtil.currentNativeSession();
		try {

			System.out.println("jurusan = " + jurusan + ", fakultas = " + fakultas + ", nama => " + nama);

			fakultas = URLDecoder.decode(fakultas.replaceAll("_", ""), "UTF-8");
			jurusan = URLDecoder.decode(jurusan.replaceAll("_", ""), "UTF-8");
			nama = URLDecoder.decode(nama.replaceAll("_", ""), "UTF-8");

			@SuppressWarnings("unchecked")
			List<Matakuliah> matakuliahs = session.createCriteria(Matakuliah.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(nama == null || nama.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("kode", nama, MatchMode.ANYWHERE),
									Restrictions.ilike("nama", nama, MatchMode.ANYWHERE)))

					.createAlias("jurusan", "jurusan").createAlias("jurusan.fakultas", "fakultas")
					.add(fakultas == null || fakultas.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("fakultas.nama", fakultas.trim(), MatchMode.ANYWHERE))
					.add(jurusan == null || jurusan.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("jurusan.nama", jurusan.trim(), MatchMode.ANYWHERE),
									Restrictions.or(
											Restrictions.ilike("jurusan.kode", jurusan.trim(), MatchMode.ANYWHERE),
											Restrictions.ilike("jurusan.kodeEpsbed", jurusan.trim(),
													MatchMode.ANYWHERE))))

					.add(Restrictions.ne("kode", "")).add(Restrictions.ne("nama", ""))
					.add(Restrictions.isNotNull("jurusan.feeder")).add(Restrictions.ne("jurusan.feeder", "")).list();

			System.out.println("matakuliahs size => " + matakuliahs.size() + ", jurusan = " + jurusan + ", fakultas = "
					+ fakultas + ", nama => " + nama);

			JSONArray array = new JSONArray();
			for (Matakuliah matakuliah : matakuliahs) {
				JSONObject data = new JSONObject();
				data.put("id_prodi", matakuliah.getJurusan().getFeeder());
				data.put("nama_mata_kuliah", matakuliah.getNama());
				data.put("kode_mata_kuliah", matakuliah.getKode());
				data.put("sks_mata_kuliah", matakuliah.getSks());
				data.put("sks_tatap_muka", matakuliah.getSksDiskusi());
				data.put("sks_praktek", matakuliah.getSksPraktek());
				data.put("sks_praktek_lapangan", matakuliah.getSksPraktekLapangan());
				data.put("sks_simulasi", matakuliah.getSksSimulasi());
				data.put("metode_kuliah", matakuliah.getMetodeKuliah());

				data.put("ada_sap", matakuliah.getAdaSap() ? 1 : 0);
				data.put("ada_silabus", matakuliah.getAdaSilabus() ? 1 : 0);
				data.put("ada_bahan_ajar", matakuliah.getAdaBahanAjar() ? 1 : 0);
				data.put("ada_acara_praktek", matakuliah.getAdaAcaraPraktek() ? 1 : 0);
				data.put("ada_diktat", matakuliah.getAdaDiktat() ? 1 : 0);

				if (matakuliah.getTanggalMulai() != null) {
					data.put("tanggal_mulai_efektif", Common.databaseDateFormat.get().format(matakuliah.getTanggalMulai()));
				}
				if (matakuliah.getTanggalSampai() != null) {
					data.put("tanggal_akhir_efektif", Common.databaseDateFormat.get().format(matakuliah.getTanggalSampai()));
				}

				JSONObject json = new JSONObject();
				json.put("data", data);
				json.put("class", Matakuliah.class.getName());
				json.put("id", matakuliah.getId());
				json.put("keyName", "id_mk");
				if (matakuliah.getFeeder() != null && !matakuliah.getFeeder().trim().isEmpty()) {
					Map<String, Object> dataKey = new HashMap<String, Object>();
					dataKey.put("id_mk", matakuliah.getFeeder().trim());
					JSONObject jsonObjectKey = new JSONObject(dataKey);
					json.put("key", jsonObjectKey);
				}
				array.put(json);
			}

			CommonID commonID = new CommonID();
			commonID.setInfo1(array.toString());

			HibernateUtil.closeSession();

			return commonID;
		} catch (Exception e) {
			HibernateUtil.closeSession();
			Common.tampilErrorJikaAdmin(e);
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}

	@GET
	@Path("kurikulum/{nama}/{fakultas}/{jurusan}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Mengambil data kurikulum yang cocok dengan filter nama/fakultas/jurusan, diformat siap
	 * dipetakan ke skema PDDikti Feeder. Tidak memeriksa autentikasi.
	 */
	public CommonID getKurikulum(@PathParam("nama") String nama, @PathParam("fakultas") String fakultas,
			@PathParam("jurusan") String jurusan) {

		Session session = HibernateUtil.currentNativeSession();
		try {

			nama = URLDecoder.decode(nama.replaceAll("_", ""), "UTF-8");
			fakultas = URLDecoder.decode(fakultas.replaceAll("_", ""), "UTF-8");
			jurusan = URLDecoder.decode(jurusan.replaceAll("_", ""), "UTF-8");

			@SuppressWarnings("unchecked")
			List<Kurikulum> kurikulums = session.createCriteria(Kurikulum.class)

					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

					.add(nama == null || nama.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("keterangan", nama, MatchMode.ANYWHERE),
									Restrictions.ilike("nama", nama, MatchMode.ANYWHERE)))

					.createAlias("jurusan", "jurusan").createAlias("jurusan.fakultas", "fakultas")
					.add(fakultas == null || fakultas.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("fakultas.nama", fakultas.trim(), MatchMode.ANYWHERE))
					.add(jurusan == null || jurusan.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("jurusan.nama", jurusan.trim(), MatchMode.ANYWHERE),
									Restrictions.or(
											Restrictions.ilike("jurusan.kode", jurusan.trim(), MatchMode.ANYWHERE),
											Restrictions.ilike("jurusan.kodeEpsbed", jurusan.trim(),
													MatchMode.ANYWHERE))))

					.add(Restrictions.isNotNull("jurusan.feeder")).add(Restrictions.ne("jurusan.feeder", "")).list();

			JSONArray array = new JSONArray();
			JSONArray arrayDetail = new JSONArray();
			for (Kurikulum kurikulum : kurikulums) {

				JSONObject jsonObject = FeederExporterGenerator.kurikulum(kurikulum);

				JSONObject json = new JSONObject();
				json.put("class", Kurikulum.class.getName());
				json.put("id", kurikulum.getId());
				json.put("keyName", "id_kurikulum_sp");
				json.put("data", jsonObject);
				if (kurikulum.getFeeder() != null && !kurikulum.getFeeder().trim().isEmpty()) {
					Map<String, Object> dataKey = new HashMap<String, Object>();
					dataKey.put("id_kurikulum_sp", kurikulum.getFeeder().trim());
					JSONObject jsonObjectKey = new JSONObject(dataKey);
					json.put("key", jsonObjectKey);
				}
				array.put(json);

				@SuppressWarnings("unchecked")
				List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = session
						.createCriteria(KurikulumPunyaMatakuliah.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("kurikulum", kurikulum)).createAlias("matakuliah", "matakuliah")
						.add(Restrictions.isNotNull("matakuliah.feeder")).add(Restrictions.ne("matakuliah.feeder", ""))
						.list();
				JSONArray arrayS = new JSONArray();
				for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {
					jsonObject = FeederExporterGenerator.kurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah);

					json = new JSONObject();
					json.put("class", KurikulumPunyaMatakuliah.class.getName());
					json.put("id", kurikulumPunyaMatakuliah.getId());
					json.put("keyName", "id_kurikulum_sp;id_mk");
					json.put("data", jsonObject);
					Map<String, Object> dataKey = new HashMap<String, Object>();
					dataKey.put("id_kurikulum_sp", kurikulumPunyaMatakuliah.getKurikulum().getFeeder());
					dataKey.put("id_mk", kurikulumPunyaMatakuliah.getMatakuliah().getFeeder());
					JSONObject jsonObjectKey = new JSONObject(dataKey);
					json.put("key", jsonObjectKey);

					arrayS.put(json);
				}
				arrayDetail.put(arrayS);
			}

			CommonID commonID = new CommonID();
			commonID.setInfo1(array.toString());
			commonID.setInfo2(arrayDetail.toString());

			HibernateUtil.closeSession();

			return commonID;
		} catch (Exception e) {
			HibernateUtil.closeSession();
			Common.tampilErrorJikaAdmin(e);
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}

	@GET
	@Path("dosen/{ta}/{nama}/{fakultas}/{jurusan}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Mengambil data dosen yang cocok dengan filter tahun akademik/nama/fakultas/jurusan, diformat
	 * siap dipetakan ke skema PDDikti Feeder. Tidak memeriksa autentikasi.
	 */
	public CommonID getDosen(@PathParam("ta") String ta, @PathParam("nama") String nama,
			@PathParam("fakultas") String fakultas, @PathParam("jurusan") String jurusan) {
		Session session = HibernateUtil.currentNativeSession();
		try {

			System.out.println(
					"jurusan = " + jurusan + ", fakultas = " + fakultas + ", nama => " + nama + ", ta => " + ta);

			fakultas = URLDecoder.decode(fakultas.replaceAll("_", ""), "UTF-8");
			jurusan = URLDecoder.decode(jurusan.replaceAll("_", ""), "UTF-8");
			nama = URLDecoder.decode(nama.replaceAll("_", ""), "UTF-8");
			ta = URLDecoder.decode(ta.replaceAll("_", "").trim(), "UTF-8");

			Integer semesterPendek = null;
			String tahunAkademik = Common.getCurrentTahunAkademik();
			String semesters = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GANJIL;
			if (ta != null && ta.length() == 5) {
				Integer mulai = Integer.parseInt(ta.toString().substring(0, 4));
				tahunAkademik = mulai + "/" + (mulai + 1);
				Integer s = Integer.parseInt(ta.toString().substring(4, 5));
				semesters = s.equals(1) ? Perkuliahan.GANJIL : Perkuliahan.GENAP;

				if (s.equals(3)) {
					semesterPendek = Perkuliahan.SEMESTER_PENDEK;
				}
			}

			@SuppressWarnings("unchecked")
			List<Dosen> dosens = session.createCriteria(Dosen.class)
					.add(nama == null || nama.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("nidn", fakultas.trim(), MatchMode.ANYWHERE),
									Restrictions.ilike("nama", fakultas.trim(), MatchMode.ANYWHERE)))

					.add(Restrictions.ne("nidn", "")).add(Restrictions.isNotNull("nidn"))
					.add(Restrictions.ne("nama", ""))

					.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)
					.createAlias("jurusan.fakultas", "fakultas", Criteria.LEFT_JOIN)
					.add(fakultas == null || fakultas.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("fakultas.nama", fakultas.trim(), MatchMode.ANYWHERE))
					.add(jurusan == null || jurusan.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("jurusan.nama", jurusan.trim(), MatchMode.ANYWHERE),
									Restrictions.or(
											Restrictions.ilike("jurusan.kode", jurusan.trim(), MatchMode.ANYWHERE),
											Restrictions.ilike("jurusan.kodeEpsbed", jurusan.trim(),
													MatchMode.ANYWHERE))))

					.list();

			System.out.println("dosens size => " + dosens.size() + ", jurusan = " + jurusan + ", fakultas = " + fakultas
					+ ", nama => " + nama + ", ta => " + ta);

			JSONArray array = new JSONArray();
			JSONArray arrayDetail = new JSONArray();
			for (Dosen dosen : dosens) {

				String sql = "select \n" + "max(c.nama) as fakultas, \n" + "max(b.nama) as jurusan, \n"
						+ "a.program, \n" + "sum(d.sks) as jumlah_sks,\n" + "max(b.id) as jurusanId \n" + "from (\n"
						+ "\tselect max(bb.jurusan) jurusan, max(bb.matakuliah) matakuliah, max(bb.program) as program \n"
						+ "\tfrom detailperkuliahan aa inner join perkuliahan bb on aa.perkuliahan=bb.id \n"
						+ "\twhere persetujuan=1 \n" + "\tand (bb.dosen1 = " + (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
						+ " or bb.dosen2 = " + (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen3 = "
						+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen4 = "
						+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen5 = "
						+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen6 = "
						+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen7 = "
						+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen8 = "
						+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen9 = "
						+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen10 = "
						+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + ") \n" + "\tand bb.tahun_ajaran = '" + tahunAkademik
						+ "' \n"
						+ (semesterPendek != null && semesterPendek.equals(Perkuliahan.SEMESTER_PENDEK)
								? "\tand bb.status_semesterpendek=" + semesterPendek + " "
								: "\tand bb.ganjil_genap='" + semesters.replace("'", "''") + "'")
						+ "\n" + "\tgroup by perkuliahan\n" + ") a\n" + "left join jurusan b on (a.jurusan = b.id) \n"
						+ "left join fakultas c on (b.fakultas = c.id) \n"
						+ "left join matakuliah d on (a.matakuliah = d.id)   \n" + "group by b.fakultas, a.jurusan";

				System.out.println("sql => " + sql);

				@SuppressWarnings("unchecked")
				List<Object[]> hasils = session.createSQLQuery(sql).list();

				for (Object[] objects : hasils) {

					Long idJurusan = ((Number) objects[4]).longValue();

					// String program = (String) objects[2];

					String program = "Reguler";
					Integer sks = ((Number) (objects[3] == null ? 0 : objects[3])).intValue();
					final PenugasanDosenMengajar penugasanDosenMengajar = Common.getPenugasanDosenMengajar(idJurusan,
							program, tahunAkademik, semesters, sks, dosen);
					if (penugasanDosenMengajar.getDosen() == null) {
						penugasanDosenMengajar.setDosen(dosen);
						session.getTransaction().begin();
						session.update(penugasanDosenMengajar);
						session.getTransaction().commit();

					}
					JSONObject jsonObject = FeederExporterGenerator.dosen_pt(penugasanDosenMengajar, session);

					JSONObject json = new JSONObject();
					json.put("class", PenugasanDosenMengajar.class.getName());
					json.put("id", penugasanDosenMengajar.getId());
					json.put("nidn", dosen.getNidn());

					json.put("keyName", "id_reg_ptk");
					json.put("data", jsonObject);
					if (penugasanDosenMengajar.getFeeder() != null
							&& !penugasanDosenMengajar.getFeeder().trim().isEmpty()) {
						Map<String, Object> dataKey = new HashMap<String, Object>();
						dataKey.put("id_reg_ptk", penugasanDosenMengajar.getFeeder().trim());
						JSONObject jsonObjectKey = new JSONObject(dataKey);
						json.put("key", jsonObjectKey);
					}
					array.put(json);

					Criterion criterion = Restrictions.eq("dosen1", dosen);
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen2", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen7", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen8", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen9", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen10", dosen));

					@SuppressWarnings("unchecked")
					List<Perkuliahan> perkuliahans = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(criterion)
							.add(Restrictions.eq("jurusan.id", idJurusan))

							.createAlias("matakuliah", "matakuliah")
							.add(tahunAkademik == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("tahunAjaran", tahunAkademik))
							.add(semesters == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("ganjilGenap", semesters))
							.add(Restrictions.isNotNull("matakuliah.feeder"))
							.add(Restrictions.ne("matakuliah.feeder", ""))

							.list();

					JSONArray arrayS = new JSONArray();
					for (Perkuliahan perkuliahan : perkuliahans) {
						Integer jumlahPertemuan = ((Number) session.createCriteria(Pertemuan.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.setProjection(Projections.rowCount()).add(Restrictions.eq("perkuliahan", perkuliahan))
								.uniqueResult()).intValue();

						jsonObject = FeederExporterGenerator.ajar_dosen(dosen, perkuliahan, jumlahPertemuan);

						json = new JSONObject();
						json.put("data", jsonObject);
						json.put("class", PenugasanDosenMengajar.class.getName());
						json.put("id", penugasanDosenMengajar.getId());
						json.put("keyName", "id_ajar");
						arrayS.put(json);
					}
					arrayDetail.put(arrayS);

				}
			}

			CommonID commonID = new CommonID();
			commonID.setInfo1(array.toString());
			commonID.setInfo2(arrayDetail.toString());

			HibernateUtil.closeSession();

			return commonID;
		} catch (Exception e) {
			HibernateUtil.closeSession();
			Common.tampilErrorJikaAdmin(e);
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}

	@GET
	@Path("perkuliahan/{ta}/{nama}/{kelas}/{fakultas}/{jurusan}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Mengambil data penawaran perkuliahan (kelas) yang cocok dengan filter tahun akademik/nama
	 * mata kuliah/kelas/fakultas/jurusan, diformat siap dipetakan ke skema PDDikti Feeder. Tidak
	 * memeriksa autentikasi.
	 */
	public CommonID getPerkuliahan(@PathParam("ta") String ta, @PathParam("nama") String nama,
			@PathParam("kelas") String kelas, @PathParam("fakultas") String fakultas,
			@PathParam("jurusan") String jurusan) {
		return getPerkuliahanMhs(ta, nama, kelas, fakultas, jurusan, "");
	}

	@GET
	@Path("perkuliahan_mhs/{ta}/{nama}/{kelas}/{fakultas}/{jurusan}/{mhs}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Mengambil data KRS/perkuliahan per mahasiswa untuk satu kelas yang cocok dengan filter tahun
	 * akademik/nama mata kuliah/kelas/fakultas/jurusan/mahasiswa, diformat siap dipetakan ke skema
	 * PDDikti Feeder. Dipakai juga oleh {@link #getPerkuliahan} dengan filter mahasiswa kosong.
	 * Tidak memeriksa autentikasi.
	 */
	public CommonID getPerkuliahanMhs(@PathParam("ta") String ta, @PathParam("nama") String nama,
			@PathParam("kelas") String kelas, @PathParam("fakultas") String fakultas,
			@PathParam("jurusan") String jurusan, @PathParam("mhs") String mhs) {
		Session session = HibernateUtil.currentNativeSession();
		try {

			fakultas = URLDecoder.decode(fakultas.replaceAll("_", ""), "UTF-8");
			jurusan = URLDecoder.decode(jurusan.replaceAll("_", ""), "UTF-8");
			nama = URLDecoder.decode(nama.replaceAll("_", ""), "UTF-8");
			ta = URLDecoder.decode(ta.replaceAll("_", "").trim(), "UTF-8");
			kelas = URLDecoder.decode(kelas.replaceAll("_", "").trim(), "UTF-8");
			mhs = mhs == null ? "" : URLDecoder.decode(mhs.replaceAll("_", "").trim(), "UTF-8");

			Integer semesterPendek = null;
			String tahunAkademik = Common.getCurrentTahunAkademik();
			String semesters = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GANJIL;
			if (ta != null && ta.length() == 5) {
				Integer mulai = Integer.parseInt(ta.toString().substring(0, 4));
				tahunAkademik = mulai + "/" + (mulai + 1);
				Integer s = Integer.parseInt(ta.toString().substring(4, 5));
				semesters = s.equals(1) ? Perkuliahan.GANJIL : Perkuliahan.GENAP;

				if (s.equals(3)) {
					semesterPendek = Perkuliahan.SEMESTER_PENDEK;
				}
			}

			System.out.println("jurusan = " + jurusan + ", fakultas = " + fakultas + ", nama => " + nama + ", ta => "
					+ ta + " len " + ta.length() + ", kelas => " + kelas + ", tahunAkademik=>" + tahunAkademik
					+ ", semesters=>" + semesters + ", semesterPendek = " + semesterPendek + ", mhs = " + mhs);

			Criterion criterionMhs = Restrictions.sqlRestriction("true");
			if (mhs != null && !mhs.trim().isEmpty()) {

				String sql = "this_.id in (select perkuliahan from detailperkuliahan a inner join mahasiswa b on (a.mahasiswa = b.id) where perkuliahan is not null and (b.nama ilike '%"
						+ mhs.trim() + "%' or b.nim ilike '%" + mhs.trim() + "%') group by perkuliahan)";
				criterionMhs = Restrictions.sqlRestriction(sql);
			}

			@SuppressWarnings("unchecked")
			List<Perkuliahan> perkuliahans = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(criterionMhs)
					.createAlias("matakuliah", "matakuliah")
					.createAlias("masaPerkuliahan", "masaPerkuliahan", Criteria.LEFT_JOIN)

					.add(tahunAkademik == null ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.eq("tahunAjaran", tahunAkademik),
									Restrictions.eq("masaPerkuliahan.nama", ta)))

					.add(semesterPendek != null && semesterPendek.equals(Perkuliahan.SEMESTER_PENDEK)
							? Restrictions.eq("statusSemesterPendek", semesterPendek)
							: (semesters == null ? Restrictions.sqlRestriction("true")
									: Restrictions.or(Restrictions.eq("masaPerkuliahan.nama", ta),
											Restrictions.eq("ganjilGenap", semesters))))

					.add(kelas == null || kelas.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("kelas", kelas, MatchMode.ANYWHERE))

					.add(nama == null || nama.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("matakuliah.kode", nama, MatchMode.ANYWHERE),
									Restrictions.ilike("matakuliah.nama", nama, MatchMode.ANYWHERE)))

					.createAlias("jurusan", "jurusan").createAlias("jurusan.fakultas", "fakultas")
					.add(fakultas == null || fakultas.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("fakultas.nama", fakultas.trim(), MatchMode.ANYWHERE))
					.add(jurusan == null || jurusan.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("jurusan.nama", jurusan.trim(), MatchMode.ANYWHERE),
									Restrictions.or(
											Restrictions.ilike("jurusan.kode", jurusan.trim(), MatchMode.ANYWHERE),
											Restrictions.ilike("jurusan.kodeEpsbed", jurusan.trim(),
													MatchMode.ANYWHERE))))

					.add(Restrictions.isNotNull("matakuliah.feeder")).add(Restrictions.ne("matakuliah.feeder", ""))

					.add(Restrictions.isNotNull("jurusan.feeder")).add(Restrictions.ne("jurusan.feeder", "")).list();

			System.out.println("perkuliahans size => " + perkuliahans.size() + ", jurusan = " + jurusan
					+ ", fakultas = " + fakultas + ", nama => " + nama + ", ta => " + ta);

			JSONArray array = new JSONArray();
			JSONArray arrayDetail = new JSONArray();
			JSONArray arrayDetailN = new JSONArray();
			JSONArray arrayDetailD = new JSONArray();
			JSONArray arrayDetailP = new JSONArray();
			for (Perkuliahan perkuliahan : perkuliahans) {
				JSONObject jsonObject = FeederExporterGenerator.perkuliahan(perkuliahan);
				JSONObject json = new JSONObject();
				json.put("data", jsonObject);
				json.put("class", Perkuliahan.class.getName());
				json.put("id", perkuliahan.getId());
				json.put("keyName", "id_kls");
				if (perkuliahan.getFeeder() != null && !perkuliahan.getFeeder().trim().isEmpty()) {
					Map<String, Object> dataKey = new HashMap<String, Object>();
					dataKey.put("id_kls", perkuliahan.getFeeder().trim());
					JSONObject jsonObjectKey = new JSONObject(dataKey);
					json.put("key", jsonObjectKey);
				}
				array.put(json);

				JSONArray arrayD = new JSONArray();
				JSONArray arrayP = new JSONArray();
				for (Dosen dosen : perkuliahan.populateDosenBuNama()) {
					if (!dosen.getNidn().isEmpty()) {
						jsonObject = FeederExporterGenerator.perkuliahan_dosen(perkuliahan, dosen, session);
						json = new JSONObject();
						json.put("data", jsonObject);

						arrayD.put(json);

						PenugasanDosenMengajar penugasanDosenMengajar = Common.getPenugasanDosenMengajar(
								perkuliahan.getJurusan().getId(), perkuliahan.getProgram(), tahunAkademik, semesters,
								perkuliahan.getMatakuliah().getSks(), dosen);
						if (dosen.getPerguruanTinggi() != null && dosen.getPerguruanTinggi().getFeeder() != null) {
							jsonObject = FeederExporterGenerator.dosen_pt(penugasanDosenMengajar, session);
							json = new JSONObject();
							json.put("class", PenugasanDosenMengajar.class.getName());
							json.put("id", penugasanDosenMengajar.getId());
							json.put("keyName", "id_reg_ptk");
							json.put("data", jsonObject);
							if (penugasanDosenMengajar.getFeeder() != null
									&& !penugasanDosenMengajar.getFeeder().trim().isEmpty()) {
								Map<String, Object> dataKey = new HashMap<String, Object>();
								dataKey.put("id_reg_ptk", penugasanDosenMengajar.getFeeder().trim());
								JSONObject jsonObjectKey = new JSONObject(dataKey);
								json.put("key", jsonObjectKey);
							}
							arrayP.put(json);
						}
					}
				}
				arrayDetailD.put(arrayD);
				arrayDetailP.put(arrayP);

				@SuppressWarnings("unchecked")
				List<Detailperkuliahan> detailperkuliahans = session.createCriteria(Detailperkuliahan.class)
						.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
						.add(Restrictions.or(Restrictions.isNull("internal"), Restrictions.eq("internal", false)))

						.createAlias("mahasiswa", "mahasiswa").add(Restrictions.isNotNull("mahasiswa.idRegPd"))
						.add(Restrictions.ne("mahasiswa.idRegPd", "")).add(Restrictions.eq("perkuliahan", perkuliahan))

						.add(mhs == null || mhs.trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.ilike("mahasiswa.nim", mhs, MatchMode.ANYWHERE),
										Restrictions.ilike("mahasiswa.nama", mhs, MatchMode.ANYWHERE)))

						.list();

				JSONArray arrayS = new JSONArray();
				JSONArray arrayN = new JSONArray();
				for (Detailperkuliahan detailperkuliahan : detailperkuliahans) {

					jsonObject = FeederExporterGenerator.krs(detailperkuliahan);
					json = new JSONObject();
					json.put("data", jsonObject);
					json.put("class", Detailperkuliahan.class.getName());
					json.put("id", detailperkuliahan.getId());
					json.put("keyName", "id_kls;id_reg_pd");

					arrayS.put(json);

					if (detailperkuliahan.getTotalNilai() > 0.1) {
						jsonObject = FeederExporterGenerator.nilai(detailperkuliahan);
						json = new JSONObject();
						json.put("data", jsonObject);
						json.put("class", Detailperkuliahan.class.getName());
						json.put("id", detailperkuliahan.getId());
						json.put("keyName", "id_kls;id_reg_pd");
						arrayN.put(json);
					}
				}
				arrayDetail.put(arrayS);
				arrayDetailN.put(arrayN);
			}

			CommonID commonID = new CommonID();
			commonID.setInfo1(array.toString());
			commonID.setInfo2(arrayDetail.toString());
			commonID.setInfo3(arrayDetailN.toString());
			commonID.setInfo4(arrayDetailD.toString());
			commonID.setInfo5(arrayDetailP.toString());

			HibernateUtil.closeSession();

			return commonID;
		} catch (Exception e) {
			HibernateUtil.closeSession();
			Common.tampilErrorJikaAdmin(e);
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}

	@GET
	@Path("nilai_transfer/{mhs}/{nama}/{fakultas}/{jurusan}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Mengambil data nilai transfer (konversi nilai dari kampus asal) yang cocok dengan filter
	 * mahasiswa/nama/fakultas/jurusan, diformat siap dipetakan ke skema PDDikti Feeder. Tidak
	 * memeriksa autentikasi.
	 */
	public CommonID getNilaiTransfer(@PathParam("mhs") String mhs, @PathParam("nama") String nama,
			@PathParam("fakultas") String fakultas, @PathParam("jurusan") String jurusan) {

		Session session = HibernateUtil.currentNativeSession();
		try {

			System.out.println(
					"jurusan = " + jurusan + ", fakultas = " + fakultas + ", nama => " + nama + ", mhs => " + mhs);

			fakultas = URLDecoder.decode(fakultas.replaceAll("_", ""), "UTF-8");
			jurusan = URLDecoder.decode(jurusan.replaceAll("_", ""), "UTF-8");
			nama = URLDecoder.decode(nama.replaceAll("_", ""), "UTF-8");
			mhs = URLDecoder.decode(mhs.replaceAll("_", "").trim(), "UTF-8");

			@SuppressWarnings("unchecked")
			List<Detailperkuliahan> detailperkuliahans = session.createCriteria(Detailperkuliahan.class)
					.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
					.add(Restrictions.gt("totalNilai", 0.1))
					.add(Restrictions.or(Restrictions.isNull("internal"), Restrictions.eq("internal", false)))

					.createAlias("mahasiswa", "mahasiswa").createAlias("matakuliahKonversi", "matakuliahKonversi")

					.add(mhs.isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("mahasiswa.nim", mhs, MatchMode.ANYWHERE),
									Restrictions.ilike("mahasiswa.nama", mhs, MatchMode.ANYWHERE)))

					.add(nama.isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("matakuliahKonversi.kode", nama, MatchMode.ANYWHERE),
									Restrictions.ilike("matakuliahKonversi.nama", nama, MatchMode.ANYWHERE)))

					.createAlias("mahasiswa.jurusan", "jurusan").createAlias("jurusan.fakultas", "fakultas")
					.add(fakultas == null || fakultas.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("fakultas.nama", fakultas.trim(), MatchMode.ANYWHERE))
					.add(jurusan == null || jurusan.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("jurusan.nama", jurusan.trim(), MatchMode.ANYWHERE),
									Restrictions.or(
											Restrictions.ilike("jurusan.kode", jurusan.trim(), MatchMode.ANYWHERE),
											Restrictions.ilike("jurusan.kodeEpsbed", jurusan.trim(),
													MatchMode.ANYWHERE))))

					.add(Restrictions.isNotNull("mahasiswa.idRegPd")).add(Restrictions.ne("mahasiswa.idRegPd", ""))
					.add(Restrictions.isNotNull("matakuliahKonversi.feeder"))
					.add(Restrictions.ne("matakuliahKonversi.feeder", "")).list();

			JSONArray array = new JSONArray();
			for (Detailperkuliahan detailperkuliahan : detailperkuliahans) {
				JSONObject jsonObject = FeederExporterGenerator.nilai_transfer(detailperkuliahan);
				JSONObject json = new JSONObject();
				json.put("data", jsonObject);
				json.put("class", Detailperkuliahan.class.getName());
				json.put("id", detailperkuliahan.getId());
				json.put("keyName", "id_ekuivalensi");
				if (detailperkuliahan.getFeeder() != null && !detailperkuliahan.getFeeder().trim().isEmpty()) {
					Map<String, Object> dataKey = new HashMap<String, Object>();
					dataKey.put("id_ekuivalensi", detailperkuliahan.getFeeder().trim());
					JSONObject jsonObjectKey = new JSONObject(dataKey);
					json.put("key", jsonObjectKey);
				}
				array.put(json);
			}

			CommonID commonID = new CommonID();
			commonID.setInfo1(array.toString());

			HibernateUtil.closeSession();

			return commonID;
		} catch (Exception e) {
			HibernateUtil.closeSession();
			Common.tampilErrorJikaAdmin(e);
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}

	@GET
	@Path("akm/{mhs}/{ta}/{fakultas}/{jurusan}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Mengambil data AKM (Angka Kredit Mengajar/Aktivitas Kuliah Mahasiswa, tergantung konteks
	 * Feeder) untuk satu mahasiswa pada tahun akademik tertentu, difilter fakultas/jurusan. Tidak
	 * memeriksa autentikasi.
	 */
	public CommonID getAkm(@PathParam("mhs") String mhs, @PathParam("ta") String ta,
			@PathParam("fakultas") String fakultas, @PathParam("jurusan") String jurusan) {
		return getAkmAngkatan(mhs, fakultas, fakultas, jurusan, "_");
	}

	@GET
	@Path("akm_angkatan/{mhs}/{ta}/{fakultas}/{jurusan}/{angkatan}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Seperti {@link #getAkm(String, String, String, String)}, dipersempit lagi ke satu angkatan
	 * spesifik. Tidak memeriksa autentikasi.
	 */
	public CommonID getAkmAngkatan(@PathParam("mhs") String mhs, @PathParam("ta") String ta,
			@PathParam("fakultas") String fakultas, @PathParam("jurusan") String jurusan,
			@PathParam("angkatan") String angkatan) {

		Session session = HibernateUtil.currentNativeSession();
		try {

			fakultas = URLDecoder.decode(fakultas.replaceAll("_", ""), "UTF-8");
			jurusan = URLDecoder.decode(jurusan.replaceAll("_", ""), "UTF-8");
			ta = URLDecoder.decode(ta.replaceAll("_", ""), "UTF-8");
			mhs = URLDecoder.decode(mhs.replaceAll("_", "").trim(), "UTF-8");
			angkatan = URLDecoder.decode(angkatan.replaceAll("_", "").trim(), "UTF-8");

			Integer semesterPendek = null;
			String tahunAkademik = Common.getCurrentTahunAkademik();
			String semesters = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GANJIL;
			if (ta != null && ta.length() == 5) {
				Integer mulai = Integer.parseInt(ta.toString().substring(0, 4));
				tahunAkademik = mulai + "/" + (mulai + 1);
				Integer s = Integer.parseInt(ta.toString().substring(4, 5));
				semesters = s.equals(1) ? Perkuliahan.GANJIL : Perkuliahan.GENAP;

				if (s.equals(3)) {
					semesterPendek = Perkuliahan.SEMESTER_PENDEK;
				}
			}

			System.out.println("jurusan = " + jurusan + ", fakultas = " + fakultas + ", ta => " + ta + ", mhs => " + mhs
					+ ", angkatan => " + angkatan + ", semesterPendek = " + semesterPendek);

			@SuppressWarnings("unchecked")
			List<Mahasiswa> mahasiswas = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

					.add(angkatan.trim().isEmpty() || !Common.isNumber(angkatan) ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("tahunangkatan", Integer.parseInt(angkatan.trim())))

					.add(mhs == null || mhs.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("nim", mhs, MatchMode.ANYWHERE),
									Restrictions.ilike("nama", mhs, MatchMode.ANYWHERE)))

					.add(Restrictions.ne("nim", "")).add(Restrictions.ne("nama", ""))

					.createAlias("jurusan", "jurusan").createAlias("jurusan.fakultas", "fakultas")
					.add(fakultas == null || fakultas.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("fakultas.nama", fakultas.trim(), MatchMode.ANYWHERE))
					.add(jurusan == null || jurusan.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("jurusan.nama", jurusan.trim(), MatchMode.ANYWHERE),
									Restrictions.or(
											Restrictions.ilike("jurusan.kode", jurusan.trim(), MatchMode.ANYWHERE),
											Restrictions.ilike("jurusan.kodeEpsbed", jurusan.trim(),
													MatchMode.ANYWHERE))))

					.add(Restrictions.isNotNull("idRegPd")).add(Restrictions.ne("idRegPd", ""))

					.createAlias("fakultas.perguruanTinggi", "perguruanTinggi")
					.add(Restrictions.isNotNull("jurusan.feeder")).add(Restrictions.ne("jurusan.feeder", ""))
					.add(Restrictions.isNotNull("perguruanTinggi.feeder"))
					.add(Restrictions.ne("perguruanTinggi.feeder", "")).addOrder(Order.desc("nim")).list();

			JSONArray array = new JSONArray();
			for (Mahasiswa mahasiswa : mahasiswas) {
				int tahunAngkatanMhs = mahasiswa.getTahunangkatan();
				int semester = Common.getSemester(tahunAngkatanMhs, tahunAkademik, semesters,
						mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
				KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, semesterPendek,
						true);
				JSONObject jsonObject = FeederExporterGenerator.kuliah_mahasiswa(session, mahasiswa, semester,
						semesterPendek, krsMahasiswa);
				JSONObject json = new JSONObject();
				json.put("data", jsonObject);
				json.put("class", Mahasiswa.class.getName());
				json.put("id", mahasiswa.getId());
				json.put("keyName", "id_smt;id_reg_pd");

				String id_smt = jsonObject.getString("id_smt");

				Map<String, Object> dataKey = new HashMap<String, Object>();
				dataKey.put("id_smt", id_smt.trim());
				dataKey.put("id_reg_pd", mahasiswa.getIdRegPd().trim());
				JSONObject jsonObjectKey = new JSONObject(dataKey);
				json.put("key", jsonObjectKey);
				array.put(json);
			}

			CommonID commonID = new CommonID();
			commonID.setInfo1(array.toString());

			HibernateUtil.closeSession();

			return commonID;
		} catch (Exception e) {
			HibernateUtil.closeSession();
			Common.tampilErrorJikaAdmin(e);
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}
}

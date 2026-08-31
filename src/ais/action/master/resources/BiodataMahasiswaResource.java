package ais.action.master.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;




import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;

import com.sun.jersey.api.NotFoundException;

import com.sun.jersey.spi.resource.Singleton;

@Path("/biodataMahasiswa")
@Singleton



/**
 * Titik akhir REST (Jersey/JAX-RS) hanya-baca untuk data {@link BiodataMahasiswa} (biodata detail
 * mahasiswa, bukan data induk {@code Mahasiswa} itu sendiri), dipakai integrasi eksternal (mis.
 * aplikasi mobile/portal pihak ketiga). Setiap permintaan diautentikasi ulang per-request lewat
 * {@code username}/{@code password} yang disertakan sebagai segmen path URL dan divalidasi via
 * {@link Common#checkLogin} — pola yang sama dipakai seluruh keluarga {@code *Resource} di paket
 * ini (lihat {@code DataResource} sebagai basis generik CRUD-baca). Memperluas
 * {@link DataResource} untuk mewarisi operasi {@code getData}/{@code getAllData} generik, dengan
 * tambahan pencarian berbasis NIM mahasiswa ({@link #getDataByNIM}).
 *
 * <p>
 * <b>Catatan keamanan:</b> username dan password dikirim sebagai bagian dari path URL pada
 * permintaan GET, bukan header/body — pola ini rawan tercatat di log akses server, cache proxy,
 * riwayat browser, dan header {@code Referer}. Ini berlaku pada seluruh method publik kelas ini
 * yang menerima {@code @PathParam("password")}.
 * </p>
 */
/**
 * Tipe khusus untuk biodata mahasiswa resource. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * DataResource}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan
 * yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code getXml()}, {@code getData()}, {@code
 * getDataByNIM()}, {@code getAllData()}, {@code getAllData()}, {@code getAllData()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see DataResource
 */
/**
 * Tipe khusus untuk biodata mahasiswa resource. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * DataResource}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan
 * yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code getXml()}, {@code getData()}, {@code
 * getDataByNIM()}, {@code getAllData()}, {@code getAllData()}, {@code getAllData()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see DataResource
 */
/**
 * Tipe khusus untuk biodata mahasiswa resource. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * DataResource}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan
 * yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code getXml()}, {@code getData()}, {@code
 * getDataByNIM()}, {@code getAllData()}, {@code getAllData()}, {@code getAllData()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see DataResource
 */
/**
 * Tipe khusus untuk biodata mahasiswa resource. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * DataResource}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan
 * yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code getXml()}, {@code getData()}, {@code
 * getDataByNIM()}, {@code getAllData()}, {@code getAllData()}, {@code getAllData()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see DataResource
 */
/**
 * Tipe khusus untuk biodata mahasiswa resource. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * DataResource}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan
 * yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code getXml()}, {@code getData()}, {@code
 * getDataByNIM()}, {@code getAllData()}, {@code getAllData()}, {@code getAllData()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see DataResource
 */
public class BiodataMahasiswaResource extends DataResource<BiodataMahasiswa> {

	/** Membuat resource yang terikat ke entitas {@link BiodataMahasiswa}. */
	public BiodataMahasiswaResource() {
		super(BiodataMahasiswa.class);
	}

	/** Mengembalikan diri sendiri (self) sebagai representasi JSON kosong — dipakai untuk pemeriksaan endpoint dasar. */
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public BiodataMahasiswaResource getXml() {
		return this;
	}

	/**
	 * Mengambil satu {@link BiodataMahasiswa} berdasarkan id, setelah autentikasi username/password.
	 *
	 * @param username username untuk autentikasi (dikirim via path URL)
	 * @param password password untuk autentikasi (dikirim via path URL)
	 * @param id       id baris {@link BiodataMahasiswa} yang diminta
	 * @return data biodata mahasiswa yang diminta
	 */
	@GET
	@Path("load/{username}/{password}/{id}/")
	@Produces({ MediaType.APPLICATION_JSON
			 })
	public BiodataMahasiswa getData(@PathParam("username") String username,
			@PathParam("password") String password, @PathParam("id") String id) {
		return super.getData(username, password, id);
	}

	/**
	 * Mengambil satu {@link BiodataMahasiswa} berdasarkan NIM mahasiswa terkait, setelah autentikasi
	 * username/password.
	 *
	 * @param username username untuk autentikasi (dikirim via path URL)
	 * @param password password untuk autentikasi (dikirim via path URL)
	 * @param nim      NIM mahasiswa yang biodatanya dicari
	 * @return data biodata mahasiswa yang cocok
	 * @throws NotFoundException bila autentikasi gagal, data tidak ditemukan, atau terjadi galat internal
	 */
	@GET
	@Path("getDataByNIM/{username}/{password}/{nim}/")
	@Produces({ MediaType.APPLICATION_JSON
			 })
	public BiodataMahasiswa getDataByNIM(
			@PathParam("username") String username,
			@PathParam("password") String password, @PathParam("nim") String nim) {
		if (!Common.checkLogin(username, password))
			throw new NotFoundException("fobidden access");

		try {
			Session session = HibernateUtil.currentNativeSession();
			BiodataMahasiswa generalValueObject = (BiodataMahasiswa) session
					.createCriteria(BiodataMahasiswa.class)
					.createAlias("mahasiswa", "mahasiswa")
					.add(Restrictions.eq("mahasiswa.nim", nim.trim()))
					.setMaxResults(1).uniqueResult();
			
			HibernateUtil.closeSession();
			if (generalValueObject == null) {
				throw new NotFoundException("data detail mahasiswa dengan NIM "
						+ nim + " tidak ditemukan");
			}
			return generalValueObject;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}

	/**
	 * Mengambil seluruh data {@link BiodataMahasiswa} tanpa filter pencarian, setelah autentikasi.
	 *
	 * @param username username untuk autentikasi (dikirim via path URL)
	 * @param password password untuk autentikasi (dikirim via path URL)
	 * @return daftar seluruh biodata mahasiswa
	 */
	@GET
	@Path("search/{username}/{password}/")
	@Produces({ MediaType.APPLICATION_JSON
			 })
	public List<BiodataMahasiswa> getAllData(
			@PathParam("username") String username,
			@PathParam("password") String password) {
		return super.getAllData(username, password);
	}

	/**
	 * Mengambil data {@link BiodataMahasiswa} yang cocok dengan satu kata kunci pencarian, setelah
	 * autentikasi. Logika pencocokan kata kunci diwarisi dari {@link DataResource#getAllData}.
	 *
	 * @param username username untuk autentikasi (dikirim via path URL)
	 * @param password password untuk autentikasi (dikirim via path URL)
	 * @param search   kata kunci pencarian
	 * @return daftar biodata mahasiswa yang cocok
	 */
	@GET
	@Path("search/{username}/{password}/{search}/")
	@Produces({ MediaType.APPLICATION_JSON
			 })
	public List<BiodataMahasiswa> getAllData(
			@PathParam("username") String username,
			@PathParam("password") String password,
			@PathParam("search") String search) {
		return super.getAllData(username, password, search);
	}

	/**
	 * Mengambil data {@link BiodataMahasiswa} yang cocok dengan dua kata kunci pencarian, setelah
	 * autentikasi. Logika pencocokan kata kunci diwarisi dari {@link DataResource#getAllData}.
	 *
	 * @param username username untuk autentikasi (dikirim via path URL)
	 * @param password password untuk autentikasi (dikirim via path URL)
	 * @param search   kata kunci pencarian pertama
	 * @param search1  kata kunci pencarian kedua
	 * @return daftar biodata mahasiswa yang cocok
	 */
	@GET
	@Path("search/{username}/{password}/{search}/{search1}/")
	@Produces({ MediaType.APPLICATION_JSON
			 })
	public List<BiodataMahasiswa> getAllData(
			@PathParam("username") String username,
			@PathParam("password") String password,
			@PathParam("search") String search,
			@PathParam("search1") String search1) {
		return super.getAllData(username, password, search, search1);
	}

}

package ais.database.model.file;

/**
 * Objek parameter (bukan entitas Hibernate — POJO biasa, tidak beranotasi {@code @Entity}) yang
 * mendeskripsikan cara menampilkan/mengelola satu field foto/media milik suatu entitas lain,
 * dipakai oleh komponen media (lihat pemakaian di {@code ais.common.CommonMedia} dan kelas-kelas
 * {@code Foto*} pada paket {@link ais.database.model.file}, mis. {@code FotoSiswa},
 * {@code FotoPegawai}, {@code FotoDosen}, {@code FotoGuru}). Kombinasi {@link #getClazz()} +
 * {@link #getProperty()} menunjuk (lewat reflection) ke entitas dan properti target yang memuat
 * data foto, sementara {@link #getFilePropertyName()}/{@link #getMediaPropertyName()} menentukan
 * nama properti yang dipakai untuk menampilkan file fisik dan metadata medianya di UI.
 */
public class MediaParameter {

	/** Identitas/label baris data (biasanya hasil {@code toString()} entitas pemilik foto), dipakai untuk penamaan tampilan. */
	private String id;
	/** Nama properti pada entitas target yang menyimpan path/nama file. */
	private String filePropertyName;
	/** Nama properti pada entitas target yang menyimpan objek media terkait. */
	private String mediaPropertyName;
	/** Kelas entitas target yang memiliki field foto/media ini (dipakai untuk reflection). */
	private Class<?> clazz;
	/** Nama properti pada {@link #clazz} yang menjadi acuan pencarian foto (mis. relasi ke entitas induk). */
	private String property;
	private Integer height;
	private Integer width;
	/** Menandakan foto ini adalah foto utama/profil (true) atau foto tambahan/lampiran (false). */
	private Boolean fotoUtama = false;
	/** ID foto spesifik yang dirujuk, dipakai bersama {@link #usingId}. */
	private Long fotoId = null;
	/** Menentukan apakah pencarian foto dilakukan berdasarkan {@link #fotoId} (true) atau berdasarkan entitas pemilik (false/default). */
	private Boolean usingId;

	public MediaParameter(String id, String filePropertyName,
			String mediaPropertyName, Class<?> clazz, String property) {
		super();
		this.id = id;
		this.filePropertyName = filePropertyName;
		this.mediaPropertyName = mediaPropertyName;
		this.clazz = clazz;
		this.property = property;
	}

	public MediaParameter(String id, String filePropertyName,
			String mediaPropertyName, Class<?> clazz, String property,
			Integer height, Integer width) {
		super();
		this.id = id;
		this.filePropertyName = filePropertyName;
		this.mediaPropertyName = mediaPropertyName;
		this.clazz = clazz;
		this.property = property;
		this.height = height;
		this.width = width;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFilePropertyName() {
		return filePropertyName;
	}

	public void setFilePropertyName(String filePropertyName) {
		this.filePropertyName = filePropertyName;
	}

	public String getMediaPropertyName() {
		return mediaPropertyName;
	}

	public void setMediaPropertyName(String mediaPropertyName) {
		this.mediaPropertyName = mediaPropertyName;
	}

	public Class<?> getClazz() {
		return clazz;
	}

	public void setClazz(Class<?> clazz) {
		this.clazz = clazz;
	}

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

	public Integer getHeight() {
		return height;
	}

	public void setHeight(Integer height) {
		this.height = height;
	}

	public Integer getWidth() {
		return width;
	}

	public void setWidth(Integer width) {
		this.width = width;
	}

	public Boolean getFotoUtama() {
		return fotoUtama;
	}

	public void setFotoUtama(Boolean fotoUtama) {
		this.fotoUtama = fotoUtama;
	}

	public Long getFotoId() {
		return fotoId;
	}

	public void setFotoId(Long fotoId) {
		this.fotoId = fotoId;
	}

	public Boolean getUsingId() {
		return usingId == null ? false : usingId;
	}

	public void setUsingId(Boolean usingId) {
		this.usingId = usingId;
	}

}

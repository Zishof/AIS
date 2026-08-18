package ais.database.model.file;

public class MediaParameter {

	private String id;
	private String filePropertyName;
	private String mediaPropertyName;
	private Class<?> clazz;
	private String property;
	private Integer height;
	private Integer width;
	private Boolean fotoUtama = false;
	private Long fotoId = null;
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

package ais.database.model;

/**
 * Kontrak (mixin) bukan entitas — mendefinisikan kolom umum terkait akun
 * media sosial/login sosial yang diimplementasikan oleh berbagai entitas
 * pengguna di {@code ais.database.model} (mis. entitas mahasiswa/pegawai/user)
 * yang mendukung login via OAuth pihak ketiga. Menyimpan ID akun eksternal
 * pengguna pada masing-masing penyedia ({@code facebookId}, {@code googleId},
 * {@code twitterId}, {@code linkedinId}) serta tautan profil media sosial
 * bebas ({@code socialMediaProfile}). Kelas entitas yang mengimplementasikan
 * antarmuka ini memetakan tiap metode ke kolom tabelnya masing-masing.
 */
public interface SocialMediaCommonModel {

	/**
	 * @return id akun Facebook milik pengguna (hasil login OAuth), atau null bila
	 *         belum pernah dihubungkan.
	 */
	public String getFacebookId();

	/**
	 * @param facebookId id akun Facebook untuk ditautkan ke pengguna ini.
	 */
	public void setFacebookId(String facebookId);

	/**
	 * @return id akun Google milik pengguna (hasil login OAuth), atau null bila
	 *         belum pernah dihubungkan.
	 */
	public String getGoogleId();

	/**
	 * @param googleId id akun Google untuk ditautkan ke pengguna ini.
	 */
	public void setGoogleId(String googleId);

	/**
	 * @return id akun Twitter milik pengguna (hasil login OAuth), atau null bila
	 *         belum pernah dihubungkan.
	 */
	public String getTwitterId();

	/**
	 * @param twitterId id akun Twitter untuk ditautkan ke pengguna ini.
	 */
	public void setTwitterId(String twitterId);

	/**
	 * @return id akun LinkedIn milik pengguna (hasil login OAuth), atau null bila
	 *         belum pernah dihubungkan.
	 */
	public String getLinkedinId();

	/**
	 * @param linkedinId id akun LinkedIn untuk ditautkan ke pengguna ini.
	 */
	public void setLinkedinId(String linkedinId);

	/**
	 * @return tautan (URL) profil media sosial bebas milik pengguna, atau null
	 *         bila belum diisi.
	 */
	public String getSocialMediaProfile();

	/**
	 * @param socialMediaProfile tautan profil media sosial bebas.
	 */
	public void setSocialMediaProfile(String socialMediaProfile);
}

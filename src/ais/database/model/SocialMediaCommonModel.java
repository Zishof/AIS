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

	public String getFacebookId();

	public void setFacebookId(String facebookId);

	public String getGoogleId();

	public void setGoogleId(String googleId);

	public String getTwitterId();

	public void setTwitterId(String twitterId);

	public String getLinkedinId();

	public void setLinkedinId(String linkedinId);

	public String getSocialMediaProfile();

	public void setSocialMediaProfile(String socialMediaProfile);
}

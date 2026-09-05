package ais.action.master.sop.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Label;
import org.zkoss.zul.Vbox;

import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.model.Dosen;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.employ.JenisJabatan;
import ais.database.model.rab.Pejabat;
import ais.database.model.sop.AlurSop;
import ais.database.model.sop.DisposisiSop;

/**
 * Tipe khusus untuk sop util. Kelas ini memberi nama dan batas tanggung jawab yang eksplisit pada
 * perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah inisialisasi/lifecycle ({@code buatKartuAktor()});
 * pembacaan/pencarian ({@code tampilAktor()}, {@code tampilAktor()}, {@code ambilNama()}, {@code ambilNama()},
 * {@code ambilPegawaiPengaju()}, {@code ambilRolesIdLowerSafe()}); validasi/perhitungan ({@code hitungAktor()});
 * mutasi data ({@code resetAktor()}); penghapusan/pembatalan ({@code hapusDisposisi()}); pelaporan/ekspor
 * ({@code renderAktor()}, {@code renderAktorTunggal()}); operasi domain lain ({@code resolveAktor()}, {@code
 * resolveAktor()}, {@code wadahAktor()}, {@code gabungUsernamePengguna()}, {@code perbaruiJumlahAktor()}, {@code
 * addUsersByJenisJabatan()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut
 * di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 */
public class SopUtil {

	public static boolean hapusDisposisi(Session session, DisposisiSop disposisiSop) {

		if (session == null || disposisiSop == null || disposisiSop.getId() == null) {
			return true;
		}

		try {
			String sql = "delete from akunting.daftar_pengajuan_transfer where disposisi_sop=:disposisiId";
			session.createSQLQuery(sql).setLong("disposisiId", disposisiSop.getId().longValue()).executeUpdate();

			sql = "delete from disposisi_alur_sop where disposisi_sop=:disposisiId";
			session.createSQLQuery(sql).setLong("disposisiId", disposisiSop.getId().longValue()).executeUpdate();

			// PengajuanMahasiswaAction memanggil helper ini sebelum menghapus record
			// pengajuannya. Lepaskan FK pemilik lebih dulu; urutan lama mencoba menghapus
			// disposisi yang masih direferensikan dan membuat transaksi PostgreSQL abort.
			sql = "update public.pengajuan_mahasiswa set disposisi_sop=null where disposisi_sop=:disposisiId";
			session.createSQLQuery(sql).setLong("disposisiId", disposisiSop.getId().longValue()).executeUpdate();

			// penggunaan_anggaran masih mereferensi disposisi_sop (FK fkef7f694ebc255eb8).
			// Kolomnya nullable, jadi LEPASKAN tautan (set null) — bukan dihapus — agar
			// data realisasi/penggunaan anggaran TETAP ada, namun penghapusan disposisi
			// tidak melanggar foreign key.
			sql = "update rab.penggunaan_anggaran set disposisi_sop=null where disposisi_sop=:disposisiId";
			session.createSQLQuery(sql).setLong("disposisiId", disposisiSop.getId().longValue()).executeUpdate();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "SopUtil.hapusDisposisi: gagal melepas relasi disposisi");
			return false;
		}

		try {
			session.delete(disposisiSop);
			session.flush();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "SopUtil.hapusDisposisi: gagal menghapus disposisi");
			return false;
		}

		return true;
	}

	/**
	 * Menampilkan daftar aktor SOP dengan cara yang lebih hemat proses.
	 *
	 * Perbaikan utama dibanding versi lama:
	 * 1. Tidak lagi melakukan loop seluruh Tbmuser berulang-ulang untuk setiap jenis aktor.
	 * 2. Daftar user aktif, user per pegawai, user per dosen, dan user per jabatan dibuat sekali secara lazy.
	 * 3. Pengecekan aktor khusus, role, atasan langsung, pejabat, kaprodi, dekan, dan dosen PA memakai lookup lokal.
	 * 4. Tetap mempertahankan output lama: komponen foto/nama aktor dan attribute "usernamePengguna" pada hbox.
	 */
	public static boolean tampilAktor(Tbmuser tbmuserCurrent, String khususUser, String jenisPengguna,
			DisposisiSop disposisiSop, AlurSop alur, Component hbox) {
		return tampilAktor(tbmuserCurrent, khususUser, jenisPengguna, disposisiSop, alur, hbox, null);
	}

	/**
	 * Varian yang menerima {@link AktorLookup} bersama (sharedLookup) agar daftar
	 * user aktif &amp; resolusi role hanya dihitung SEKALI lintas baris — mis. saat
	 * render grid Alur SOP, cukup buat satu AktorLookup lalu teruskan ke setiap
	 * baris. Bila sharedLookup null, dibuat lookup baru (perilaku lama).
	 */
	public static boolean tampilAktor(Tbmuser tbmuserCurrent, String khususUser, String jenisPengguna,
			DisposisiSop disposisiSop, AlurSop alur, Component hbox, AktorLookup sharedLookup) {
		AktorHitung hasil = hitungAktor(tbmuserCurrent, khususUser, jenisPengguna, disposisiSop, alur, sharedLookup);
		renderAktor(hasil.datasAktor, hbox);
		hasil.datasAktor.clear();
		return hasil.ada;
	}

	/**
	 * Varian headless dari {@link #tampilAktor} untuk konteks TANPA UI ZK sama sekali (mis.
	 * endpoint REST API mobile) — mengembalikan boolean "ada" yang identik PLUS daftar
	 * {@link Tbmuser} aktor yang berhasil diresolusi, tanpa membangun komponen UI apa pun.
	 * Memakai ulang persis logika pencocokan role/khususUser/atasan/kaprodi/dekan/dosenPa yang
	 * sama via {@link #hitungAktor}, sehingga hasilnya selalu konsisten dengan tampilan web.
	 */
	public static AktorResolusi resolveAktor(Tbmuser tbmuserCurrent, String khususUser, String jenisPengguna,
			DisposisiSop disposisiSop, AlurSop alur) {
		return resolveAktor(tbmuserCurrent, khususUser, jenisPengguna, disposisiSop, alur, null);
	}

	public static AktorResolusi resolveAktor(Tbmuser tbmuserCurrent, String khususUser, String jenisPengguna,
			DisposisiSop disposisiSop, AlurSop alur, AktorLookup sharedLookup) {
		AktorHitung hasil = hitungAktor(tbmuserCurrent, khususUser, jenisPengguna, disposisiSop, alur, sharedLookup);
		AktorResolusi resolusi = new AktorResolusi();
		resolusi.ada = hasil.ada;
		for (Tbmuser tbmuser : hasil.datasAktor.values()) {
			if (isAktif(tbmuser)) {
				resolusi.aktors.add(tbmuser);
			}
		}
		hasil.datasAktor.clear();
		return resolusi;
	}

	/** Hasil {@link #resolveAktor}: boolean "ada" (user saat ini termasuk aktor) + daftar aktor aktif. */
	public static class AktorResolusi {
		public boolean ada;
		public List<Tbmuser> aktors = new ArrayList<Tbmuser>();
	}

	/**
	 * Gerbang penegak kewenangan EKSPLISIT di lapisan mesin alur SOP -- lihat javadoc kelas
	 * {@link AlurSop}, bagian "pemisahan deklarasi kewenangan dari penegakan kewenangan". Sebelum
	 * method ini ada, tidak ada satu pun titik di mesin generik (entitas maupun helper) yang
	 * menjawab pertanyaan "bolehkah {@code tbmuser} memproses {@code alurSop} SEKARANG" --
	 * {@code tampilAktor}/{@code resolveAktor} hanya menjawab "siapa saja yang berwenang", dan
	 * pemanggil bebas mengabaikan hasilnya (seperti yang selama ini terjadi pada jalur ZK).
	 *
	 * <p><b>FAIL-CLOSED:</b> argumen {@code null} mana pun, resolusi aktor yang kosong, maupun
	 * galat tak terduga selama resolusi SELALU menghasilkan {@code false} (ditolak) -- tidak pernah
	 * meloloskan secara diam-diam.</p>
	 *
	 * <p><b>Satu pengecualian yang disengaja:</b> tahap ber-{@link AlurSop#getKembaliKePengaju()}
	 * tidak memiliki {@link AlurSop#getAktorSop()} (getter tahap tersebut sengaja mengosongkannya --
	 * lihat javadocnya), sehingga {@link #resolveAktor} tidak akan pernah menemukan aktor apa pun
	 * untuknya. Pada tahap demikian yang berwenang memprosesnya adalah PENGAJU pengajuan ini
	 * sendiri ({@code disposisiSop.getDiajukanOleh()}) -- identitas yang sama dengan yang sudah
	 * dipakai jalur render sedia ada ({@link #renderAktorTunggal} dipanggil dengan
	 * {@code disposisiSop.getDiajukanOleh()} setiap kali cabang ini dipilih). Tanpa pengecualian
	 * ini, gerbang fail-closed akan MEMBEKUKAN seluruh langkah revisi/kembali-ke-pengaju yang
	 * selama ini berjalan sah.</p>
	 *
	 * <p><b>Tidak menimbang status admin.</b> Pemanggil yang ingin mengizinkan admin memproses atas
	 * nama pihak lain (jalur yang sudah ada dan disengaja -- lihat
	 * {@code Common.getApakahAdmin()} di {@code DisposisiAlurSopAction.onSave()}) harus memeriksanya
	 * SENDIRI sebelum memanggil method ini; digabungkan di sini akan membuat jalur admin tidak
	 * eksplisit bagi pembaca lain yang memanggil gerbang ini tanpa menyadari adanya bypass.</p>
	 *
	 * @param tbmuser      pengguna yang mencoba memproses tahap ini; {@code null} selalu ditolak
	 * @param disposisiSop kasus/pengajuan SOP yang sedang berjalan; {@code null} selalu ditolak
	 * @param alurSop      definisi tahap yang sedang diproses; {@code null} selalu ditolak
	 * @return {@code true} hanya bila {@code tbmuser} terbukti termasuk aktor berwenang pada tahap
	 *         ini untuk pengajuan ini; {@code false} untuk seluruh keadaan lain
	 */
	public static boolean pastikanBerwenang(Tbmuser tbmuser, DisposisiSop disposisiSop, AlurSop alur) {
		if (tbmuser == null || disposisiSop == null || alur == null) {
			return false;
		}
		try {
			if (Boolean.TRUE.equals(alur.getKembaliKePengaju())) {
				Tbmuser pengaju = disposisiSop.getDiajukanOleh();
				return pengaju != null && isSameUser(tbmuser, pengaju);
			}
			String jenisPengguna = alur.getAktorSop() != null ? alur.getAktorSop().getJenisPengguna() : "";
			AktorResolusi resolusi = resolveAktor(tbmuser, alur.getKhususUsername(), jenisPengguna, disposisiSop, alur);
			return resolusi.ada;
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "SopUtil.pastikanBerwenang");
			return false;
		}
	}

	/**
	 * Menegakkan bahwa {@code tujuanId} yang diklaim pemanggil (mis. dari parameter request JSON,
	 * form JSP, atau state ZK) benar-benar salah satu cabang SAH dari {@code alurSopSaatIni} --
	 * lihat javadoc kelas {@link AlurSop}, bagian "Urutan jenjang tidak ditegakkan": tanpa
	 * pemeriksaan ini, pemanggil yang mengirim ulang id tahap tujuan sembarang (bukan salah satu
	 * yang benar-benar dirender sebagai pilihan) dapat melompati jenjang atau memalsukan rute.
	 *
	 * @param alurSopSaatIni tahap yang sedang diproses; {@code null} selalu ditolak
	 * @param tujuanId       id tahap tujuan yang diklaim pemanggil; {@code null} selalu ditolak
	 * @return {@code true} hanya bila {@code tujuanId} cocok dengan salah satu id pada
	 *         {@link AlurSop#ambilAlurSetelahnya()} milik {@code alurSopSaatIni}
	 */
	public static boolean validasiTransisi(AlurSop alurSopSaatIni, Long tujuanId) {
		if (alurSopSaatIni == null || tujuanId == null) {
			return false;
		}
		try {
			for (AlurSop next : alurSopSaatIni.ambilAlurSetelahnya()) {
				if (next != null && tujuanId.equals(next.getId())) {
					return true;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "SopUtil.validasiTransisi");
		}
		return false;
	}

	/**
	 * Varian jamak dari {@link #validasiTransisi(AlurSop, Long)} untuk pemanggil yang mengizinkan
	 * beberapa cabang dipilih sekaligus (mis. checkbox multi-pilih di ZK). Daftar kosong/{@code
	 * null} dianggap tidak ada yang perlu ditolak (validasi "wajib pilih" adalah urusan pemanggil
	 * terpisah) -- method ini hanya menolak bila ADA id yang bukan cabang sah.
	 *
	 * @param alurSopSaatIni tahap yang sedang diproses
	 * @param tujuanIds      daftar id tahap tujuan yang diklaim pemanggil
	 * @return {@code true} bila seluruh id pada {@code tujuanIds} adalah cabang sah dari
	 *         {@code alurSopSaatIni} (atau daftarnya kosong/{@code null})
	 */
	public static boolean validasiTransisi(AlurSop alurSopSaatIni, List<Long> tujuanIds) {
		if (tujuanIds == null || tujuanIds.isEmpty()) {
			return true;
		}
		for (Long tujuanId : tujuanIds) {
			if (!validasiTransisi(alurSopSaatIni, tujuanId)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Menjawab: apakah {@code tbmuser} yang mencoba memproses tahap ini SEKALIGUS adalah pengaju
	 * pengajuan ({@code disposisiSop.getDiajukanOleh()}) -- yaitu, apakah ia akan menyetujui
	 * pengajuannya sendiri?
	 *
	 * <p><b>TIDAK dipanggil di titik mutasi mana pun secara default</b> -- lihat javadoc kelas
	 * {@link AlurSop}, bagian "self approval": mesin generik ini memang tidak pernah memiliki
	 * mekanisme pencegahan self-approval, dan banyak modul turunan SENGAJA belum ditambal untuk
	 * kasus ini di modul masing-masing. Mengaktifkan method ini sebagai gerbang wajib akan mengubah
	 * perilaku lintas seluruh modul yang memakai mesin ini sekaligus, sehingga keputusan itu perlu
	 * diangkat ke pengguna/pemilik produk dahulu -- bukan diputuskan sepihak di sini. Method ini
	 * disediakan agar pemanggil yang SUDAH diinstruksikan mengaktifkan pencegahan self-approval
	 * punya satu implementasi konsisten untuk dipakai, bukan menulis perbandingan identitas
	 * sendiri-sendiri di tiap Action/servis.</p>
	 *
	 * @param tbmuser      pengguna yang mencoba memproses tahap ini
	 * @param disposisiSop kasus/pengajuan SOP yang sedang berjalan
	 * @return {@code true} bila {@code tbmuser} adalah pengaju pengajuan ini sendiri
	 */
	public static boolean apakahSelfApproval(Tbmuser tbmuser, DisposisiSop disposisiSop) {
		if (tbmuser == null || disposisiSop == null) {
			return false;
		}
		try {
			return isSameUser(tbmuser, disposisiSop.getDiajukanOleh());
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Tipe implementasi bersarang {@link AktorHitung} milik {@link SopUtil}. Kelas ini memberi nama pada state
	 * atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link SopUtil}. Dependensi
	 * yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini merupakan
	 * detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code LinkedHashMap datasAktor}, {@code
	 * boolean ada}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see SopUtil
	 */
	private static class AktorHitung {
		LinkedHashMap<String, Tbmuser> datasAktor;
		boolean ada;
	}

	/** Resolusi inti (role/khususUser/atasan/kaprodi/dekan/dosenPa) dipakai bersama oleh tampilAktor & resolveAktor. */
	private static AktorHitung hitungAktor(Tbmuser tbmuserCurrent, String khususUser, String jenisPengguna,
			DisposisiSop disposisiSop, AlurSop alur, AktorLookup sharedLookup) {
		LinkedHashMap<String, Tbmuser> datasAktor = new LinkedHashMap<String, Tbmuser>();
		boolean ada = false;

		khususUser = nullSafe(khususUser);
		jenisPengguna = nullSafe(jenisPengguna);

		AktorLookup lookup = sharedLookup != null ? sharedLookup : new AktorLookup();

		try {
			String[] roles = splitComma(jenisPengguna);
			if (roles.length > 0) {
				String currentRoleId = getCurrentRoleId(tbmuserCurrent);
				for (Tbmuser tbmuser : lookup.getActiveUsers()) {
					String userRoles = lookup.getRolesLower(tbmuser);
					if (userRoles.length() == 0) {
						continue;
					}
					for (int i = 0; i < roles.length; i++) {
						String role = roles[i];
						if (role.length() == 0) {
							continue;
						}
						if (userRoles.indexOf("," + role.toLowerCase() + ",") >= 0) {
							putAktor(datasAktor, tbmuser);
							if (role.equalsIgnoreCase(currentRoleId) || isSameUser(tbmuserCurrent, tbmuser)) {
								ada = true;
							}
							break;
						}
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:158");
		}

		try {
			String[] khususUsers = splitComma(khususUser);
			for (int i = 0; i < khususUsers.length; i++) {
				String username = khususUsers[i];
				if (username.length() == 0) {
					continue;
				}
				Tbmuser tbmuser = lookup.getUserByUserId(username);
				if (tbmuser != null && isAktif(tbmuser)) {
					putAktor(datasAktor, tbmuser);
					if (isSameUser(tbmuserCurrent, tbmuser)
							|| (tbmuserCurrent != null && username.equalsIgnoreCase(nullSafe(tbmuserCurrent.getUserId())))) {
						ada = true;
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:177");
		}

		if (disposisiSop != null && alur != null && alur.getAktorSop() != null) {
			try {
				if (alur.getAktorSop().getSemuaAtasanPejabat()) {
					Pegawai pegawaiPengaju = ambilPegawaiPengaju(disposisiSop);
					if (pegawaiPengaju != null) {
						ada = addUsersByJenisJabatan(datasAktor, lookup, tbmuserCurrent, pegawaiPengaju.getAtasan()) || ada;
						ada = addUsersByJenisJabatan(datasAktor, lookup, tbmuserCurrent, pegawaiPengaju.getAtasanPendukung()) || ada;
						ada = addUsersByJenisJabatan(datasAktor, lookup, tbmuserCurrent, pegawaiPengaju.getAtasanPendukungCadangan()) || ada;
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:190");
			}

			try {
				if (alur.getAktorSop().getSemuaAtasanLangsungPegawai()) {
					Pegawai pegawaiPengaju = ambilPegawaiPengaju(disposisiSop);
					if (pegawaiPengaju != null) {
						ada = addUserByPegawai(datasAktor, lookup, tbmuserCurrent, pegawaiPengaju.getAtasanlangsung()) || ada;
						ada = addUserByPegawai(datasAktor, lookup, tbmuserCurrent, pegawaiPengaju.getAtasanlangsung2()) || ada;
						ada = addUserByPegawai(datasAktor, lookup, tbmuserCurrent, pegawaiPengaju.getAtasanlangsung3()) || ada;
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:202");
			}

			try {
				if (alur.getAktorSop().getKaprodiPengajuMahasiswa() && disposisiSop.getMahasiswa() != null
						&& disposisiSop.getMahasiswa().getJurusan() != null) {
					ada = addUserByDosen(datasAktor, lookup, tbmuserCurrent,
							disposisiSop.getMahasiswa().getJurusan().getKaprodi()) || ada;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:211");
			}

			try {
				if (alur.getAktorSop().getDosenPaPengajuMahasiswa() && disposisiSop.getMahasiswa() != null) {
					ada = addUserByDosen(datasAktor, lookup, tbmuserCurrent,
							disposisiSop.getMahasiswa().getDosenPa()) || ada;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:219");
			}

			try {
				if (alur.getAktorSop().getDekanPengajuMahasiswa() && disposisiSop.getMahasiswa() != null
						&& disposisiSop.getMahasiswa().getJurusan() != null
						&& disposisiSop.getMahasiswa().getJurusan().getFakultas() != null) {
					ada = addUserByDosen(datasAktor, lookup, tbmuserCurrent,
							disposisiSop.getMahasiswa().getJurusan().getFakultas().getDekan()) || ada;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:229");
			}

			try {
				if (alur.getAktorSop().getKaprodiPengajuDosen() && disposisiSop.getDiajukanOleh() != null
						&& disposisiSop.getDiajukanOleh().getDosen() != null
						&& disposisiSop.getDiajukanOleh().getDosen().getJurusan() != null) {
					ada = addUserByDosen(datasAktor, lookup, tbmuserCurrent,
							disposisiSop.getDiajukanOleh().getDosen().getJurusan().getKaprodi()) || ada;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:239");
			}

			try {
				if (alur.getAktorSop().getDekanPengajuDosen() && disposisiSop.getDiajukanOleh() != null
						&& disposisiSop.getDiajukanOleh().getDosen() != null
						&& disposisiSop.getDiajukanOleh().getDosen().getJurusan() != null
						&& disposisiSop.getDiajukanOleh().getDosen().getJurusan().getFakultas() != null) {
					ada = addUserByDosen(datasAktor, lookup, tbmuserCurrent,
							disposisiSop.getDiajukanOleh().getDosen().getJurusan().getFakultas().getDekan()) || ada;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:250");
			}
		}

		AktorHitung hasil = new AktorHitung();
		hasil.datasAktor = datasAktor;
		hasil.ada = ada;
		return hasil;
	}

	public static String ambilNama(String khususUser, String jenisPengguna) {
		return ambilNama(khususUser, jenisPengguna, new AktorLookup());
	}

	/**
	 * Versi yang memakai-ulang satu {@link AktorLookup}. Saat dipanggil berkali-kali dalam satu
	 * proses (mis. perulangan daftar pejabat di "Disposisi ke"), pemanggil cukup membuat SATU
	 * {@code AktorLookup} lalu meneruskannya ke setiap pemanggilan — sehingga daftar pengguna aktif
	 * hanya dipindai sekali, bukan diulang untuk tiap pejabat (menghindari lambat saat form dibuka).
	 */
	public static String ambilNama(String khususUser, String jenisPengguna, AktorLookup lookup) {
		LinkedHashMap<String, Tbmuser> datasAktor = new LinkedHashMap<String, Tbmuser>();
		khususUser = nullSafe(khususUser);
		jenisPengguna = nullSafe(jenisPengguna);
		if (lookup == null) {
			lookup = new AktorLookup();
		}

		try {
			String[] roles = splitComma(jenisPengguna);
			if (roles.length > 0) {
				for (Tbmuser tbmuser : lookup.getActiveUsers()) {
					String userRoles = lookup.getRolesLower(tbmuser);
					if (userRoles.length() == 0) {
						continue;
					}
					for (int i = 0; i < roles.length; i++) {
						String role = roles[i];
						if (role.length() > 0 && userRoles.indexOf("," + role.toLowerCase() + ",") >= 0) {
							putAktor(datasAktor, tbmuser);
							break;
						}
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:295");
		}

		try {
			String[] khususUsers = splitComma(khususUser);
			for (int i = 0; i < khususUsers.length; i++) {
				Tbmuser tbmuser = lookup.getUserByUserId(khususUsers[i]);
				if (tbmuser != null && isAktif(tbmuser)) {
					putAktor(datasAktor, tbmuser);
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:306");
		}

		StringBuilder nama = new StringBuilder();
		for (Tbmuser tbmuser : datasAktor.values()) {
			if (isAktif(tbmuser)) {
				if (nama.length() > 0) {
					nama.append(", ");
				}
				nama.append(nullSafe(tbmuser.getUserNama()));
			}
		}
		datasAktor.clear();
		return nama.toString();
	}

	private static void renderAktor(LinkedHashMap<String, Tbmuser> datasAktor, Component hbox) {
		StringBuilder usernamePengguna = new StringBuilder();
		Component wadah = hbox == null ? null : wadahAktor(hbox);
		for (Tbmuser tbmuser : datasAktor.values()) {
			if (!isAktif(tbmuser)) {
				continue;
			}
			if (wadah != null) {
				buatKartuAktor(wadah, tbmuser, null, null);
			}
			if (hasText(tbmuser.getUserId())) {
				if (usernamePengguna.length() > 0) {
					usernamePengguna.append(';');
				}
				usernamePengguna.append(tbmuser.getUserId());
			}
		}
		if (hbox != null) {
			// GABUNG (bukan timpa): saat beberapa alur berikutnya dicentang sekaligus,
			// tampilAktor dipanggil per alur ke komponen yang sama — daftar penerima
			// yang tersimpan harus mencakup SEMUA langkah terpilih, bukan hanya yang
			// terakhir. Reset dilakukan lewat resetAktor() oleh pemanggil.
			gabungUsernamePengguna(hbox, usernamePengguna.toString());
			perbaruiJumlahAktor(hbox);
		}
	}

	/**
	 * Wadah daftar penerima disposisi: container flex-wrap yang DIPAKAI ULANG lintas
	 * pemanggilan tampilAktor pada komponen induk yang sama, sehingga foto penerima
	 * membungkus ke bawah (wrap) dengan tinggi terbatas + scroll — tidak lagi
	 * memanjang horizontal dan terpotong saat penerima sangat banyak.
	 */
	public static Component wadahAktor(Component induk) {
		if (induk == null) {
			return null;
		}
		Object ada = induk.getAttribute("wadahAktor");
		if (ada instanceof Component && ((Component) ada).getParent() != null) {
			return (Component) ada;
		}
		Vbox bungkus = new Vbox();
		bungkus.setWidth("100%");
		bungkus.setParent(induk);
		Label jumlah = new Label("");
		jumlah.setStyle("font-size:11px;color:#64748b;font-weight:bold;");
		jumlah.setParent(bungkus);
		org.zkoss.zul.Div wadah = new org.zkoss.zul.Div();
		wadah.setStyle("display:flex;flex-wrap:wrap;gap:6px;align-items:flex-start;width:100%;"
				+ "max-height:240px;overflow-y:auto;padding:4px;border:1px solid #e2e8f0;"
				+ "border-radius:6px;background:#f8fafc;");
		wadah.setParent(bungkus);
		induk.setAttribute("wadahAktor", wadah);
		induk.setAttribute("lblJumlahAktor", jumlah);
		return wadah;
	}

	/** Kartu satu penerima (foto + nama) di dalam wadah flex-wrap. */
	private static void buatKartuAktor(Component wadah, Tbmuser tbmuser, Object mahasiswa, Object siswa) {
		Vbox vbox1 = new Vbox();
		vbox1.setWidth("78px");
		vbox1.setStyle("text-align:center;");
		vbox1.setParent(wadah);
		String namaTampil = "";
		try {
			if (tbmuser != null) {
				CommonMedia.tampilkanGambarKecil(tbmuser).setParent(vbox1);
				namaTampil = nullSafe(tbmuser.getUserNama());
			} else if (mahasiswa instanceof ais.database.model.Mahasiswa) {
				CommonMedia.tampilkanGambarKecil((ais.database.model.Mahasiswa) mahasiswa).setParent(vbox1);
				namaTampil = nullSafe(((ais.database.model.Mahasiswa) mahasiswa).getNama());
			} else if (siswa instanceof ais.database.model.sekolah.Siswa) {
				CommonMedia.tampilkanGambarKecil((ais.database.model.sekolah.Siswa) siswa).setParent(vbox1);
				namaTampil = nullSafe(((ais.database.model.sekolah.Siswa) siswa).getNama());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:buatKartuAktor");
		}
		Label nmLbl = new Label(namaTampil);
		nmLbl.setStyle("font-size:10px;white-space:normal;word-wrap:break-word;width:74px;display:block;");
		vbox1.appendChild(nmLbl);
	}

	/**
	 * Render SATU penerima (pengaju/khusus) ke wadah yang sama dengan tampilAktor,
	 * sekaligus ikut menyumbang ke attribute "usernamePengguna" (sebelumnya kartu
	 * kembali-ke-pengaju TIDAK terhitung sebagai penerima tersimpan).
	 */
	public static void renderAktorTunggal(Tbmuser user, ais.database.model.Mahasiswa mahasiswa,
			ais.database.model.sekolah.Siswa siswa, Component induk) {
		if (induk == null) {
			return;
		}
		Component wadah = wadahAktor(induk);
		buatKartuAktor(wadah, user, mahasiswa, siswa);
		String username = null;
		try {
			if (user != null) {
				username = user.getUserId();
			} else if (mahasiswa != null) {
				username = mahasiswa.getNim();
			} else if (siswa != null) {
				username = siswa.getNomorIndukNasional();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:renderAktorTunggal");
		}
		if (username != null && !username.trim().isEmpty()) {
			gabungUsernamePengguna(induk, username.trim());
		}
		perbaruiJumlahAktor(induk);
		if (induk.getParent() != null) {
			induk.getParent().setVisible(true);
		}
	}

	/** Kosongkan wadah aktor + attribute penerima — dipanggil listener sebelum render ulang. */
	public static void resetAktor(Component induk) {
		if (induk == null) {
			return;
		}
		try {
			java.util.List<Component> anak = new java.util.ArrayList<Component>(induk.getChildren());
			for (Component component : anak) {
				component.detach();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:resetAktor");
		}
		induk.setAttribute("wadahAktor", null);
		induk.setAttribute("lblJumlahAktor", null);
		induk.setAttribute("usernamePengguna", "");
	}

	private static void gabungUsernamePengguna(Component induk, String tambahan) {
		java.util.LinkedHashSet<String> gabungan = new java.util.LinkedHashSet<String>();
		Object lama = induk.getAttribute("usernamePengguna");
		if (lama != null) {
			for (String u : lama.toString().split(";")) {
				if (!u.trim().isEmpty()) {
					gabungan.add(u.trim());
				}
			}
		}
		if (tambahan != null) {
			for (String u : tambahan.split(";")) {
				if (!u.trim().isEmpty()) {
					gabungan.add(u.trim());
				}
			}
		}
		StringBuilder sb = new StringBuilder();
		for (String u : gabungan) {
			if (sb.length() > 0) {
				sb.append(';');
			}
			sb.append(u);
		}
		induk.setAttribute("usernamePengguna", sb.toString());
	}

	private static void perbaruiJumlahAktor(Component induk) {
		Object wadah = induk.getAttribute("wadahAktor");
		Object lbl = induk.getAttribute("lblJumlahAktor");
		if (wadah instanceof Component && lbl instanceof Label) {
			int n = ((Component) wadah).getChildren().size();
			((Label) lbl).setValue(n + " penerima disposisi");
		}
	}

	private static boolean addUsersByJenisJabatan(LinkedHashMap<String, Tbmuser> datasAktor, AktorLookup lookup,
			Tbmuser tbmuserCurrent, JenisJabatan jenisJabatan) {
		boolean ada = false;
		if (jenisJabatan == null || jenisJabatan.getId() == null) {
			return false;
		}
		List<Tbmuser> users = lookup.getUsersByJenisJabatanId(jenisJabatan.getId());
		for (int i = 0; i < users.size(); i++) {
			Tbmuser user = users.get(i);
			putAktor(datasAktor, user);
			if (isSameUser(tbmuserCurrent, user)) {
				ada = true;
			}
		}
		return ada;
	}

	private static boolean addUserByPegawai(LinkedHashMap<String, Tbmuser> datasAktor, AktorLookup lookup,
			Tbmuser tbmuserCurrent, Pegawai pegawai) {
		if (pegawai == null || pegawai.getId() == null) {
			return false;
		}
		Tbmuser user = lookup.getUserByPegawaiId(pegawai.getId());
		if (user == null) {
			return false;
		}
		putAktor(datasAktor, user);
		return isSameUser(tbmuserCurrent, user);
	}

	private static boolean addUserByDosen(LinkedHashMap<String, Tbmuser> datasAktor, AktorLookup lookup,
			Tbmuser tbmuserCurrent, Dosen dosen) {
		if (dosen == null || dosen.getId() == null) {
			return false;
		}
		Tbmuser user = lookup.getUserByDosenId(dosen.getId());
		if (user == null) {
			return false;
		}
		putAktor(datasAktor, user);
		return isSameUser(tbmuserCurrent, user);
	}

	private static void putAktor(Map<String, Tbmuser> datasAktor, Tbmuser tbmuser) {
		if (datasAktor == null || tbmuser == null || !isAktif(tbmuser)) {
			return;
		}
		datasAktor.put(createUserKey(tbmuser), tbmuser);
	}

	private static String createUserKey(Tbmuser tbmuser) {
		try {
			if (tbmuser.getPegawai() != null && tbmuser.getPegawai().getId() != null) {
				return "P:" + tbmuser.getPegawai().getId();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:404");
		}
		try {
			if (tbmuser.getMahasiswa() != null && tbmuser.getMahasiswa().getId() != null) {
				return "M:" + tbmuser.getMahasiswa().getId();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:410");
		}
		try {
			if (tbmuser.getSiswa() != null && tbmuser.getSiswa().getId() != null) {
				return "S:" + tbmuser.getSiswa().getId();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:416");
		}
		try {
			if (tbmuser.getDosen() != null && tbmuser.getDosen().getId() != null) {
				return "D:" + tbmuser.getDosen().getId();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:422");
		}
		return "U:" + nullSafe(tbmuser.getUserId());
	}

	private static Pegawai ambilPegawaiPengaju(DisposisiSop disposisiSop) {
		try {
			return disposisiSop == null || disposisiSop.getDiajukanOleh() == null ? null
					: disposisiSop.getDiajukanOleh().getPegawai();
		} catch (Exception e) {
			return null;
		}
	}

	private static boolean isSameUser(Tbmuser current, Tbmuser target) {
		if (current == null || target == null) {
			return false;
		}
		try {
			if (hasText(current.getUserId()) && hasText(target.getUserId())
					&& current.getUserId().equalsIgnoreCase(target.getUserId())) {
				return true;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:445");
		}
		try {
			if (current.getPegawai() != null && target.getPegawai() != null && current.getPegawai().getId() != null
					&& current.getPegawai().getId().equals(target.getPegawai().getId())) {
				return true;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:452");
		}
		try {
			if (current.getDosen() != null && target.getDosen() != null && current.getDosen().getId() != null
					&& current.getDosen().getId().equals(target.getDosen().getId())) {
				return true;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:459");
		}
		try {
			if (current.getMahasiswa() != null && target.getMahasiswa() != null && current.getMahasiswa().getId() != null
					&& current.getMahasiswa().getId().equals(target.getMahasiswa().getId())) {
				return true;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:466");
		}
		try {
			if (current.getSiswa() != null && target.getSiswa() != null && current.getSiswa().getId() != null
					&& current.getSiswa().getId().equals(target.getSiswa().getId())) {
				return true;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:473");
		}
		return false;
	}

	private static String ambilRolesIdLowerSafe(Tbmuser tbmuser) {
		try {
			java.util.Set<String> roles = tbmuser == null ? null : tbmuser.ambilRolesIdLower();
			if (roles == null || roles.isEmpty()) {
				return "";
			}
			StringBuilder sb = new StringBuilder();
			for (String role : roles) {
				if (role != null && role.trim().length() > 0) {
					sb.append(',').append(role.trim().toLowerCase()).append(',');
				}
			}
			return sb.toString();
		} catch (Exception e) {
			return "";
		}
	}

	private static String getCurrentRoleId(Tbmuser tbmuserCurrent) {
		try {
			return tbmuserCurrent != null && tbmuserCurrent.hakAkses() != null ? nullSafe(tbmuserCurrent.hakAkses().getRoleId())
					: "";
		} catch (Exception e) {
			return "";
		}
	}

	private static boolean isAktif(Tbmuser tbmuser) {
		try {
			return tbmuser != null && tbmuser.getAktif();
		} catch (Exception e) {
			return false;
		}
	}

	private static String[] splitComma(String value) {
		if (!hasText(value)) {
			return new String[0];
		}
		String[] raw = value.split(",");
		List<String> result = new ArrayList<String>();
		for (int i = 0; i < raw.length; i++) {
			String item = raw[i] == null ? "" : raw[i].trim();
			if (item.length() > 0) {
				result.add(item);
			}
		}
		return result.toArray(new String[result.size()]);
	}

	private static boolean hasText(String value) {
		return value != null && value.trim().length() > 0;
	}

	private static String nullSafe(String value) {
		return value == null ? "" : value.trim();
	}

	/**
	 * Cache pencarian aktor (pengguna aktif, indeks per userId/pegawai/dosen/jenis jabatan).
	 * Dibuat sekali lalu dipakai-ulang agar daftar pengguna tidak dipindai berulang. Bersifat
	 * sesaat (satu kali render) — JANGAN disimpan lintas-request karena bisa basi.
	 */
	public static class AktorLookup {
		private List<Tbmuser> activeUsers;
		private Map<String, Tbmuser> userByUserId;
		private Map<Long, Tbmuser> userByPegawaiId;
		private Map<Long, Tbmuser> userByDosenId;
		private Map<Long, List<Tbmuser>> usersByJenisJabatanId;
		private Map<Long, String> rolesLowerByUserId;

		/**
		 * Role-id (lowercase, dibungkus koma ",role,") per user — dihitung SEKALI
		 * lalu di-cache pada lookup ini. Saat satu AktorLookup dipakai bersama
		 * lintas baris (mis. render grid Alur SOP), resolusi role tiap user tidak
		 * diulang untuk setiap baris sehingga jauh lebih cepat.
		 */
		String getRolesLower(Tbmuser user) {
			if (user == null) {
				return "";
			}
			Long id = null;
			try {
				id = user.getId();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:562");
			}
			if (id == null) {
				return ambilRolesIdLowerSafe(user);
			}
			if (rolesLowerByUserId == null) {
				rolesLowerByUserId = new HashMap<Long, String>();
			}
			String cached = rolesLowerByUserId.get(id);
			if (cached != null) {
				return cached;
			}
			// L1 (peta lookup ini) → L3 (cache bersama 30 mnt, nilai String aman
			// lintas sesi) → hitung. Role jarang berubah sehingga aman di-cache.
			final Tbmuser u = user;
			String v;
			try {
				v = ais.ui.util.DashboardCache.l3("sop.rolesLower." + id,
						new ais.ui.util.DashboardCache.Loader<String>() {
							public String load() {
								return ambilRolesIdLowerSafe(u);
							}
						});
			} catch (Exception e) {
				v = ambilRolesIdLowerSafe(user);
			}
			if (v == null) {
				v = "";
			}
			rolesLowerByUserId.put(id, v);
			return v;
		}

		@SuppressWarnings("rawtypes")
		List<Tbmuser> getActiveUsers() {
			if (activeUsers != null) {
				return activeUsers;
			}
			activeUsers = new ArrayList<Tbmuser>();
			try {
				Collection values = ConstantValues.ambilBerdasarClassValues(Tbmuser.class);
				for (Object o : values) {
					try {
						Tbmuser user = (Tbmuser) o;
						if (isAktif(user)) {
							activeUsers.add(user);
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:609");
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:612");
			}
			return activeUsers;
		}

		Tbmuser getUserByUserId(String userId) {
			if (!hasText(userId)) {
				return null;
			}
			if (userByUserId == null) {
				userByUserId = new HashMap<String, Tbmuser>();
				for (Tbmuser user : getActiveUsers()) {
					if (hasText(user.getUserId())) {
						userByUserId.put(user.getUserId().toLowerCase(), user);
					}
				}
			}
			Tbmuser user = userByUserId.get(userId.trim().toLowerCase());
			if (user == null) {
				try {
					user = ConstantValues.ambilTbmuser(userId.trim());
					if (user != null && isAktif(user)) {
						userByUserId.put(userId.trim().toLowerCase(), user);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:636");
				}
			}
			return user;
		}

		Tbmuser getUserByPegawaiId(Long pegawaiId) {
			if (pegawaiId == null) {
				return null;
			}
			if (userByPegawaiId == null) {
				userByPegawaiId = new HashMap<Long, Tbmuser>();
				for (Tbmuser user : getActiveUsers()) {
					try {
						if (user.getPegawai() != null && user.getPegawai().getId() != null) {
							userByPegawaiId.put(user.getPegawai().getId(), user);
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:653");
					}
				}
			}
			return userByPegawaiId.get(pegawaiId);
		}

		Tbmuser getUserByDosenId(Long dosenId) {
			if (dosenId == null) {
				return null;
			}
			if (userByDosenId == null) {
				userByDosenId = new HashMap<Long, Tbmuser>();
				for (Tbmuser user : getActiveUsers()) {
					try {
						if (user.getDosen() != null && user.getDosen().getId() != null) {
							userByDosenId.put(user.getDosen().getId(), user);
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:671");
					}
				}
			}
			return userByDosenId.get(dosenId);
		}

		@SuppressWarnings("rawtypes")
		List<Tbmuser> getUsersByJenisJabatanId(Long jenisJabatanId) {
			if (jenisJabatanId == null) {
				return new ArrayList<Tbmuser>();
			}
			if (usersByJenisJabatanId == null) {
				usersByJenisJabatanId = new HashMap<Long, List<Tbmuser>>();
				try {
					Collection pejabatValues = ConstantValues.ambilBerdasarClassValues(Pejabat.class);
					for (Object oo : pejabatValues) {
						try {
							Pejabat pejabat = (Pejabat) oo;
							if (pejabat == null || pejabat.getPegawai() == null || pejabat.getJenisJabatan() == null
									|| pejabat.getJenisJabatan().getId() == null || !pejabat.getAktif()) {
								continue;
							}
							Tbmuser user = getUserByPegawaiId(pejabat.getPegawai().getId());
							if (user == null) {
								continue;
							}
							Long key = pejabat.getJenisJabatan().getId();
							List<Tbmuser> users = usersByJenisJabatanId.get(key);
							if (users == null) {
								users = new ArrayList<Tbmuser>();
								usersByJenisJabatanId.put(key, users);
							}
							if (!users.contains(user)) {
								users.add(user);
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:707");
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopUtil.java:710");
				}
			}
			List<Tbmuser> users = usersByJenisJabatanId.get(jenisJabatanId);
			return users == null ? new ArrayList<Tbmuser>() : users;
		}
	}
}

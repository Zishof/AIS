package ais.action.mobile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import ais.action.master.TampilanPengumumanAkademisAction;
import ais.action.master.helper.PertemuanHelper;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.master.surat.AlurPersetujuanSuratKeluarStatusAction;
import ais.action.master.surat.AlurPersetujuanSuratMasukStatusAction;
import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.JsonCompatUtil;
import ais.common.NotifikasiCache;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Kegiatan;
import ais.database.model.PengumumanAkademis;
import ais.database.model.Pertemuan;
import ais.database.model.sop.DataSop;
import ais.database.model.surat.AlurPersetujuanSuratKeluarStatus;
import ais.database.model.surat.AlurPersetujuanSuratMasukStatus;

/**
 * <h1>MobileNotifHelper — Pemuat &amp; pembuka notifikasi untuk tampilan MOBILE</h1>
 *
 * <p>
 * Kelas utilitas ini menjadi <b>satu sumber terpusat</b> bagi tampilan mobile untuk dua hal:
 * (1) <b>memuat</b> daftar notifikasi "lonceng" pengguna dari cache, dan (2) <b>membuka</b>
 * objek yang ditunjuk sebuah notifikasi ketika diketuk (disposisi SOP, persuratan, pengumuman,
 * bukti pembayaran, pertemuan kuliah, atau tautan halaman langsung). Logika pembukaan dibuat
 * <b>setia</b> dengan perilaku lonceng pada tampilan desktop
 * ({@code ais.action.maintenance.MainAction#notif}) sehingga pengalaman pengguna konsisten di
 * kedua versi, namun dirapikan menjadi method statis tanpa-status agar mudah dipakai-ulang dan
 * hemat memori.
 * </p>
 *
 * <h2>Mengapa cache, bukan basis data langsung?</h2>
 * <p>
 * Daftar notifikasi dibaca dari {@link NotifikasiCache#keteranganLonceng(String, int)} yang sudah
 * dihangatkan (warm) di memori. Dengan begitu, membuka layar notifikasi di ponsel <b>tidak</b>
 * menembak basis data setiap kali — ringan, cepat, dan aman dipakai berulang.
 * </p>
 *
 * <h2>Mengapa objek di-"attach" ulang?</h2>
 * <p>
 * {@link ConstantValues#ambil(String, long)} mengembalikan objek dari cache/sesi read-only yang
 * sudah ditutup (objek <i>detached</i>). Bila relasi malas (lazy) objek itu diakses saat dibuka,
 * akan muncul {@code LazyInitializationException}. Karena itu objek diambil ulang pada
 * <b>sesi aktif</b> melalui {@code HibernateUtil.currentSession().get(...)} sebelum dibuka.
 * Catatan penting sesuai pedoman proyek: {@code currentSession()} <b>tidak</b> ditutup manual di
 * sini karena akan ditutup otomatis oleh kerangka kerja di akhir permintaan.
 * </p>
 *
 * <h2>Kompatibilitas</h2>
 * <p>Java 1.7 (tanpa lambda) dan ZK 5.x.</p>
 *
 * @author e-Campus UI Team
 * @see ais.action.maintenance.MobileAction
 */
public final class MobileNotifHelper {

	private MobileNotifHelper() {
		/* utilitas */
	}

	/**
	 * <h3>Model ringan satu notifikasi</h3>
	 *
	 * <p>Menyimpan hanya bidang yang diperlukan tampilan: judul, keterangan objek, target ZK,
	 * serta nama kelas + id objek tujuan. Sengaja tidak menyimpan entitas Hibernate apa pun agar
	 * bebas dari masalah sesi tertutup dan hemat memori.</p>
	 */
	public static final class NotifItem {
		/** Judul/subjek notifikasi (mis. "Disposisi Surat Masuk"). */
		public final String subject;
		/** Keterangan singkat objek terkait (mis. nomor surat); boleh kosong. */
		public final String objKet;
		/** URL/halaman ZK yang langsung dibuka bila notifikasi memilikinya; boleh kosong. */
		public final String bukaZk;
		/** Nama kelas entitas tujuan (untuk pengambilan ulang); boleh kosong. */
		public final String className;
		/** Id entitas tujuan; 0 bila tidak ada. */
		public final long id;

		NotifItem(String subject, String objKet, String bukaZk, String className, long id) {
			this.subject = subject == null ? "" : subject;
			this.objKet = objKet == null ? "" : objKet;
			this.bukaZk = bukaZk == null ? "" : bukaZk;
			this.className = className == null ? "" : className;
			this.id = id;
		}

		/**
		 * @return judul tampilan = subjek + keterangan objek dalam kurung (bila ada).
		 */
		public String displayTitle() {
			return subject + (objKet.trim().length() == 0 ? "" : " (" + objKet + ")");
		}
	}

	/**
	 * Memuat daftar notifikasi pengguna dari cache lonceng, sudah ter-dedup berdasarkan subjek
	 * dan dibatasi jumlah maksimumnya.
	 *
	 * @param userId id pengguna (boleh null → daftar kosong).
	 * @param max    jumlah maksimum notifikasi yang dikembalikan.
	 * @return daftar {@link NotifItem} (tidak pernah null).
	 */
	public static List<NotifItem> load(String userId, int max) {
		List<NotifItem> hasil = new ArrayList<NotifItem>();
		if (userId == null) {
			return hasil;
		}
		List<String> mentah = new ArrayList<String>();
		try {
			mentah.addAll(NotifikasiCache.keteranganLonceng(userId, max));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		Set<String> keys = new HashSet<String>();
		for (int i = 0; i < mentah.size() && hasil.size() < max; i++) {
			try {
				JSONObject json = new JSONObject(mentah.get(i));
				JSONObject classData = json.getJSONObject("classData");
				String subject = json.optString("subject", "");
				if (keys.contains(subject)) {
					continue;
				}
				keys.add(subject);
				String objKet = classData.optString("object_keterangan", "");
				String bukaJsp = json.optString("bukaJsp", "").trim();
				String bukaZkRaw = json.optString("bukaZk", "").trim();
				// Pilih URL JSP jika tersedia; bukaZk hanya dipakai bila bukan file .zul
				String urlTarget = !bukaJsp.isEmpty() ? bukaJsp
						: (bukaZkRaw.toLowerCase().endsWith(".zul") ? "" : bukaZkRaw);
				String name = classData.optString("name", "");
				long id = JsonCompatUtil.optLong(classData, "id", 0L);
				hasil.add(new NotifItem(subject, objKet, urlTarget, name, id));
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		keys.clear();
		mentah.clear();
		return hasil;
	}

	/**
	 * Menghitung jumlah notifikasi pengguna (untuk lencana merah di lonceng &amp; navigasi bawah).
	 *
	 * @param userId id pengguna (boleh null).
	 * @param max    batas hitung.
	 * @return jumlah notifikasi unik (0..max).
	 */
	public static int count(String userId, int max) {
		return load(userId, max).size();
	}

	/**
	 * Membuat sebuah {@link EventListener} yang membuka objek tujuan notifikasi saat diketuk.
	 * Disediakan sebagai listener agar mudah dipasang pada baris notifikasi di UI.
	 *
	 * @param item notifikasi yang akan dibuka.
	 * @return listener {@code onClick} yang aman dipasang ke komponen.
	 */
	public static EventListener openListener(final NotifItem item) {
		return new EventListener() {
			public void onEvent(Event event) throws Exception {
				open(item, event == null ? null : event.getTarget());
			}
		};
	}

	/**
	 * Membuka objek yang ditunjuk sebuah notifikasi. Bila notifikasi memiliki {@code bukaZk},
	 * halaman tersebut ditampilkan dalam jendela penuh. Selain itu, objek diambil ulang pada
	 * sesi aktif lalu dibuka sesuai tipenya (SOP, surat, pengumuman, pembayaran, pertemuan).
	 *
	 * @param item   notifikasi (boleh null → tidak melakukan apa-apa).
	 * @param target komponen pemicu (untuk konteks scroll pada alur SOP); boleh null.
	 */
	public static void open(NotifItem item, Component target) {
		if (item == null) {
			return;
		}
		try {
			/* 1) Notifikasi dengan tautan halaman langsung → buka jendela penuh. */
			if (item.bukaZk != null && item.bukaZk.trim().length() > 0) {
				String url = item.bukaZk.trim();
				if (!url.toLowerCase().startsWith("http")) {
					url = Common.getRequestHostWithProtocol() + (url.startsWith("/") ? "" : "/") + url;
				}
				Common.displayWindowIframe(url, true, "96%", "96%", item.displayTitle());
				return;
			}

			/* 2) Tanpa tautan → ambil ulang objek pada sesi aktif lalu buka sesuai jenisnya. */
			if (item.id <= 0 || item.className == null || item.className.trim().length() == 0) {
				return;
			}
			GeneralValueObject gvo = ConstantValues.ambil(item.className, item.id);
			if (gvo == null || gvo.getId() == null) {
				return;
			}
			try {
				String entityName = gvo.getClass().getName().split("_")[0];
				Object fresh = HibernateUtil.currentSession().get(entityName, gvo.getId());
				if (fresh instanceof GeneralValueObject) {
					gvo = (GeneralValueObject) fresh;
				}
			} catch (Exception eAttach) { ais.common.ErrorAuditUtil.record(eAttach, "auto-audit(empty-catch) src/ais/action/mobile/MobileNotifHelper.java:222");
				/* tetap pakai objek detached bila gagal re-attach */
			}

			if (gvo instanceof DataSop) {
				DataSop ds = (DataSop) gvo;
				if (ds.getDisposisiSop() != null) {
					TampilanAlurSopAction.prosess(ds.getDisposisiSop().getId(), null, null, true, target);
				}
			} else if (gvo instanceof ais.database.model.sop.DisposisiAlurSop) {
				ais.database.model.sop.DisposisiAlurSop das = (ais.database.model.sop.DisposisiAlurSop) gvo;
				if (das.getDisposisiSop() != null) {
					TampilanAlurSopAction.prosess(das.getDisposisiSop().getId(), null, null, true, target);
				}
			} else if (gvo instanceof ais.database.model.sop.DisposisiSop) {
				TampilanAlurSopAction.prosess(((ais.database.model.sop.DisposisiSop) gvo).getId(), null, null, true,
						target);
			} else if (gvo instanceof Kegiatan) {
				Kegiatan kg = (Kegiatan) gvo;
				if (kg.getMahasiswa() != null) {
					CommonReportHelper.cetakBuktipembayaranMahasiswa(kg, false);
				} else {
					CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kg, false);
				}
			} else if (gvo instanceof Pertemuan) {
				new PertemuanHelper(Common.getCurrentUser() == null ? null : Common.getCurrentUser().getMahasiswa(),
						Common.getCurrentUser() == null ? null : Common.getCurrentUser().getBiodataCalonMahasiswa())
						.display((Pertemuan) gvo, new DataLoader() {
							public void loadData(Object value) {
							}
						}, 1);
			} else if (gvo instanceof AlurPersetujuanSuratKeluarStatus) {
				AlurPersetujuanSuratKeluarStatusAction.onAddExternal(new EventListener() {
					public void onEvent(Event arg0) {
					}
				}, (AlurPersetujuanSuratKeluarStatus) gvo);
			} else if (gvo instanceof AlurPersetujuanSuratMasukStatus) {
				AlurPersetujuanSuratMasukStatusAction.onAddExternal(new EventListener() {
					public void onEvent(Event arg0) {
					}
				}, (AlurPersetujuanSuratMasukStatus) gvo);
			} else if (gvo instanceof PengumumanAkademis) {
				TampilanPengumumanAkademisAction.prosess(((PengumumanAkademis) gvo).getId(), null, null, true, null);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}
}

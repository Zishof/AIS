package ais.action.master.kursus.helper;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.kursus.JenisIdentitasPeserta;
import ais.database.model.kursus.JenisPeserta;
import ais.database.model.kursus.KomponenDataProdukKursus;
import ais.database.model.kursus.KomponenProdukKursus;
import ais.database.model.kursus.TipePeserta;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Utilitas statis modul Kursus: (1) menyediakan konstanta referensi baku ({@link TipePeserta},
 * {@link JenisIdentitasPeserta}, {@link JenisPeserta}) yang dijamin ada di database — dimuat/dibuat
 * sekali lewat blok inisialisasi statis kelas (pola "get-or-create" saat kelas pertama diakses); dan
 * (2) membangun editor formula harga produk kursus berjenjang dua tingkat (komponen produk ->
 * komponen data produk) berbasis JSON dinamis pada {@link Row} ZK.
 *
 * <p>
 * Formula harga disimpan sebagai {@link JSONArray} bersarang: level luar berisi objek
 * {@code {key, komponenProdukKursus, harga, arrayDetail}} per {@link KomponenProdukKursus}, dan
 * {@code arrayDetail} berisi objek {@code {key, komponenDataProdukKursus, harga}} per
 * {@link KomponenDataProdukKursus} di bawahnya. Entri "dihapus" dengan mengganti elemen array pada
 * indeksnya menjadi {@link JSONObject} kosong (tanpa {@code key}) — bukan dipangkas dari array —
 * sehingga indeks elemen lain tetap stabil; method render melewati entri tanpa {@code key} tersebut.
 * Setiap perubahan memicu {@link EventListener} berantai yang menghitung ulang total harga dari
 * level detail ke level komponen lalu ke total keseluruhan.
 * </p>
 */
public class KursusUtil {

	/** Jenis peserta baku "Peserta Reguler" (dibuat otomatis bila belum ada). */
	public static JenisPeserta ANGGOTA_REGULER;
	public static JenisIdentitasPeserta EMAIL;
	public static JenisIdentitasPeserta NIM;
	public static JenisIdentitasPeserta NIDN;
	public static JenisIdentitasPeserta NIK;
	public static JenisIdentitasPeserta NIS;
	public static TipePeserta UMUM;
	public static TipePeserta MAHASISWA;
	public static TipePeserta DOSEN;
	public static TipePeserta PEGAWAI;
	public static TipePeserta SISWA;

	/** Memuat (atau membuat bila belum ada) seluruh konstanta {@link TipePeserta}/{@link JenisIdentitasPeserta}/{@link JenisPeserta} baku saat kelas ini pertama kali diakses. */
	static {
		Session session = HibernateUtil.currentNativeSession();

		UMUM = (TipePeserta) session.createCriteria(TipePeserta.class).add(Restrictions.eq("nama", "Umum"))
				.setMaxResults(1).uniqueResult();
		if (UMUM == null) {
			UMUM = new TipePeserta();
			UMUM.setKeterangan("Umum");
			UMUM.setNama("Umum");
			session.getTransaction().begin();
			session.save(UMUM);
			session.getTransaction().commit();
		}

		MAHASISWA = (TipePeserta) session.createCriteria(TipePeserta.class).add(Restrictions.eq("nama", "Mahasiswa"))
				.setMaxResults(1).uniqueResult();
		if (MAHASISWA == null) {
			MAHASISWA = new TipePeserta();
			MAHASISWA.setKeterangan("Mahasiswa");
			MAHASISWA.setNama("Mahasiswa");
			session.getTransaction().begin();
			session.save(MAHASISWA);
			session.getTransaction().commit();
		}

		DOSEN = (TipePeserta) session.createCriteria(TipePeserta.class).add(Restrictions.eq("nama", "Dosen"))
				.setMaxResults(1).uniqueResult();
		if (DOSEN == null) {
			DOSEN = new TipePeserta();
			DOSEN.setKeterangan("Dosen");
			DOSEN.setNama("Dosen");
			session.getTransaction().begin();
			session.save(DOSEN);
			session.getTransaction().commit();
		}

		PEGAWAI = (TipePeserta) session.createCriteria(TipePeserta.class).add(Restrictions.eq("nama", "Pegawai"))
				.setMaxResults(1).uniqueResult();
		if (PEGAWAI == null) {
			PEGAWAI = new TipePeserta();
			PEGAWAI.setKeterangan("Pegawai");
			PEGAWAI.setNama("Pegawai");
			session.getTransaction().begin();
			session.save(PEGAWAI);
			session.getTransaction().commit();
		}

		SISWA = (TipePeserta) session.createCriteria(TipePeserta.class).add(Restrictions.eq("nama", "Siswa"))
				.setMaxResults(1).uniqueResult();
		if (SISWA == null) {
			SISWA = new TipePeserta();
			SISWA.setKeterangan("Siswa");
			SISWA.setNama("Siswa");
			session.getTransaction().begin();
			session.save(SISWA);
			session.getTransaction().commit();
		}

		EMAIL = (JenisIdentitasPeserta) session.createCriteria(JenisIdentitasPeserta.class)
				.add(Restrictions.eq("nama", "Email")).setMaxResults(1).uniqueResult();
		if (EMAIL == null) {
			EMAIL = new JenisIdentitasPeserta();
			EMAIL.setKeterangan("Email");
			EMAIL.setNama("Email");
			session.getTransaction().begin();
			session.save(EMAIL);
			session.getTransaction().commit();
		}

		NIM = (JenisIdentitasPeserta) session.createCriteria(JenisIdentitasPeserta.class)
				.add(Restrictions.eq("nama", "NIM")).setMaxResults(1).uniqueResult();
		if (NIM == null) {
			NIM = new JenisIdentitasPeserta();
			NIM.setKeterangan("NIM");
			NIM.setNama("NIM");
			session.getTransaction().begin();
			session.save(NIM);
			session.getTransaction().commit();
		}

		NIS = (JenisIdentitasPeserta) session.createCriteria(JenisIdentitasPeserta.class)
				.add(Restrictions.eq("nama", "NIS")).setMaxResults(1).uniqueResult();
		if (NIS == null) {
			NIS = new JenisIdentitasPeserta();
			NIS.setKeterangan("NIS");
			NIS.setNama("NIS");
			session.getTransaction().begin();
			session.save(NIS);
			session.getTransaction().commit();
		}

		NIDN = (JenisIdentitasPeserta) session.createCriteria(JenisIdentitasPeserta.class)
				.add(Restrictions.eq("nama", "NIDN")).setMaxResults(1).uniqueResult();
		if (NIDN == null) {
			NIDN = new JenisIdentitasPeserta();
			NIDN.setKeterangan("NIDN");
			NIDN.setNama("NIDN");
			session.getTransaction().begin();
			session.save(NIDN);
			session.getTransaction().commit();
		}

		NIK = (JenisIdentitasPeserta) session.createCriteria(JenisIdentitasPeserta.class)
				.add(Restrictions.eq("nama", "NIK")).setMaxResults(1).uniqueResult();
		if (NIK == null) {
			NIK = new JenisIdentitasPeserta();
			NIK.setKeterangan("NIK");
			NIK.setNama("NIK");
			session.getTransaction().begin();
			session.save(NIK);
			session.getTransaction().commit();
		}

		ANGGOTA_REGULER = (JenisPeserta) session.createCriteria(JenisPeserta.class)
				.add(Restrictions.eq("nama", "Peserta Reguler")).setMaxResults(1).uniqueResult();
		if (ANGGOTA_REGULER == null) {
			ANGGOTA_REGULER = new JenisPeserta();
			ANGGOTA_REGULER.setKeterangan("Peserta Reguler");
			ANGGOTA_REGULER.setNama("Peserta Reguler");
			session.getTransaction().begin();
			session.save(ANGGOTA_REGULER);
			session.getTransaction().commit();
		}
	}

	/**
	 * Titik masuk utama: menambahkan tombol "Tambah Komponen" ke {@code rowFormula} (yang menyisipkan
	 * entri baru ber-{@code key} acak ke {@code array}) dan merender level komponen produk lewat
	 * {@link #reloadDataFormula} ke baris baru yang ditempel setelah {@code rowFormula}.
	 */
	public static void reloadFormula(final Row rowFormula, final JSONArray array) throws Exception {
		final MyFormRow rowU = new MyFormRow();

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Komponen", "/img/svg/addthis.svg");
		button.setTooltiptext("Tambah Komponen");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				JSONObject jsonObject = new JSONObject();
				Long key = Math.abs(Common.randLong());
				jsonObject.put("key", key);

				array.put(jsonObject);

				reloadDataFormula(rowU, array);
			}
		});
		button.setParent(rowFormula);

		rowU.setParent(rowFormula.getParent());

		reloadDataFormula(rowU, array);

	}

	/**
	 * Merender level detail ({@code arrayDetail}, komponen data produk) di dalam {@code detail}:
	 * satu baris per entri ber-{@code key}, berisi banbox pilihan {@link KomponenDataProdukKursus}
	 * dan kotak harga (default diambil dari {@code getHargaIkutDefault()} — mengikuti harga komponen
	 * induk bila true, atau harga sendiri bila false) beserta tombol hapus. Setiap perubahan memicu
	 * {@code eventListener} pemanggil untuk menghitung ulang total level komponen. Tidak merender
	 * apa pun (kembali {@code 0}) bila tidak ada entri ber-{@code key} yang valid.
	 *
	 * @return jumlah entri detail yang valid/dirender
	 */
	public static int reloadDetail(final EventListener eventListener, final MyDetail detail, final JSONArray arrayDetail,
			final String komponenProdukKursus, final KomponenProdukKursus komponenProdukKursusBiaya) throws Exception {
		Common.clear(detail);

		int size = 0;
		for (int i = 0; i < arrayDetail.length(); i++) {
			JSONObject jsonObject = arrayDetail.getJSONObject(i);
			Long key = null;
			if (!jsonObject.isNull("key")) {
				key = ais.common.CommonJSONUtil.ambilLong(jsonObject,"key");
			}
			if (key != null) {
				size++;
			}
		}
		if (size == 0) {
			return size;
		}

		MyGroupboxStyled groupboxStyled = new MyGroupboxStyled();
		groupboxStyled.setParent(detail);

		groupboxStyled.appendChild(new MyCaptionStyled("Komponen " + komponenProdukKursus));

		Grid grid = new Grid();grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(groupboxStyled);
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Komponen Produk");
		column.setParent(columns);
		column.setWidth("70%");

		column = new MyColumnConfig("Harga");
		column.setParent(columns);
		column.setWidth("16%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		for (int i = 0; i < arrayDetail.length(); i++) {
			final int index = i;
			final JSONObject jsonObject = arrayDetail.getJSONObject(i);
			Long key = null;
			if (!jsonObject.isNull("key")) {
				key = ais.common.CommonJSONUtil.ambilLong(jsonObject,"key");
			}

			if (key != null) {
				KomponenDataProdukKursus komponenDataProdukKursusBiaya = (KomponenDataProdukKursus) (jsonObject
						.isNull("komponenDataProdukKursus") ? null
								: ConstantValues.ambil(KomponenDataProdukKursus.class.getName(),
										ais.common.CommonJSONUtil.ambilLong(jsonObject,"komponenDataProdukKursus")));

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);

				final AmbilDataKomponenDataProdukKursusBanbox ambilDataKomponenDataProdukKursusBanbox = new AmbilDataKomponenDataProdukKursusBanbox(
						komponenProdukKursus);
				ambilDataKomponenDataProdukKursusBanbox.setAttribute("komponenDataProdukKursus",
						komponenDataProdukKursusBiaya);
				ambilDataKomponenDataProdukKursusBanbox.setValue(
						komponenDataProdukKursusBiaya == null ? "" : komponenDataProdukKursusBiaya.toString());
				ambilDataKomponenDataProdukKursusBanbox.setWidth("95%");

				row.appendChild(ambilDataKomponenDataProdukKursusBanbox);

				Double hargaDefault = komponenDataProdukKursusBiaya == null ? 0.0
						: (komponenDataProdukKursusBiaya.getHargaIkutDefault()
								? (komponenProdukKursusBiaya == null ? 0.0 : komponenProdukKursusBiaya.getHarga())
								: komponenDataProdukKursusBiaya.getHarga());

				Double hrg = jsonObject.isNull("harga") ? hargaDefault : jsonObject.getDouble("harga");
				final MyDoublebox hargaTotal = new MyDoublebox(hrg);

				final EventListener eventListenerData = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						KomponenDataProdukKursus komponenDataProdukKursusBiayaPilih = (KomponenDataProdukKursus) ambilDataKomponenDataProdukKursusBanbox
								.getAttribute("komponenDataProdukKursus");

						jsonObject.put("komponenDataProdukKursus", komponenDataProdukKursusBiayaPilih == null ? null
								: komponenDataProdukKursusBiayaPilih.getId());
						jsonObject.put("harga", hargaTotal.getValue());

						eventListener.onEvent(null);
					}
				};

				hargaTotal.setWidth("95%");
				row.appendChild(hargaTotal);

				hargaTotal.addEventListener("onChange", eventListenerData);
				ambilDataKomponenDataProdukKursusBanbox.setEventListener(eventListenerData);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {
												arrayDetail.put(index, new JSONObject());

												reloadDetail(eventListener, detail, arrayDetail, komponenProdukKursus,
														komponenProdukKursusBiaya);

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(
														"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
																+ e.getMessage());
											}

										}

									}
								});

					}
				});

				button.setParent(row);
			}
		}

		return size;
	}

	/**
	 * Merender level komponen produk ({@code array}) ke dalam {@code rowU}: grid dengan footer total
	 * (dihitung dari jumlah {@code harga} seluruh entri valid), satu baris {@link MyDetail} per
	 * {@link KomponenProdukKursus} berisi banbox pilihan komponen (dikunci bila komponen tersebut
	 * sudah punya rincian detail — {@link #reloadDetail} mengembalikan {@code size > 0}) dan label
	 * harga totalnya, plus tombol tambah-detail dan hapus. Harga per komponen dihitung dari total
	 * {@code arrayDetail}-nya bila ada detail, atau dari {@code KomponenProdukKursus.getHarga()}
	 * bila belum. Dipanggil ulang secara rekursif setiap kali struktur berubah (tambah/hapus baris)
	 * agar grid selalu konsisten dengan {@code array} terbaru.
	 */
	public static void reloadDataFormula(final Row rowU, final JSONArray array) throws Exception {
		Common.clear(rowU);

		Grid grid = new Grid();grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(rowU);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Foot foot = new Foot();
		foot.setParent(grid);

		Footer footer = new Footer("Total");
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		final Footer footerTotal = new Footer();
		foot.appendChild(footerTotal);

		footer = new Footer();
		foot.appendChild(footer);

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("0px");

		column = new MyColumnConfig("Komponen");
		column.setParent(columns);
		column.setWidth("70%");

		column = new MyColumnConfig("Harga Total");
		column.setParent(columns);
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);

		final EventListener eventListenerdata = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Double d = 0.0;
				for (int i = 0; i < array.length(); i++) {
					JSONObject jsonObject = array.getJSONObject(i);
					Long key = null;
					if (!jsonObject.isNull("key")) {
						key = ais.common.CommonJSONUtil.ambilLong(jsonObject,"key");
					}

					if (key != null) {
						Double hrg = jsonObject.isNull("harga") ? 0.0 : jsonObject.getDouble("harga");
						d += (hrg);
					}
				}

				footerTotal.setLabel(Common.numberFormat.get().format(d));
			}
		};

		eventListenerdata.onEvent(null);

		Rows rows = new Rows();
		rows.setParent(grid);

		for (int i = 0; i < array.length(); i++) {
			final int index = i;
			final JSONObject jsonObject = array.getJSONObject(i);
			Long key = null;
			if (!jsonObject.isNull("key")) {
				key = ais.common.CommonJSONUtil.ambilLong(jsonObject,"key");
			}

			if (key != null) {

				final KomponenProdukKursus komponenProdukKursusBiaya = (KomponenProdukKursus) (jsonObject
						.isNull("komponenProdukKursus") ? null
								: ConstantValues.ambil(KomponenProdukKursus.class.getName(),
										ais.common.CommonJSONUtil.ambilLong(jsonObject,"komponenProdukKursus")));
				Double hrg = jsonObject.isNull("harga")
						? (komponenProdukKursusBiaya == null ? 0.0 : komponenProdukKursusBiaya.getHarga())
						: jsonObject.getDouble("harga");

				final String komponenProdukKursus = komponenProdukKursusBiaya == null ? ""
						: komponenProdukKursusBiaya.getNama();

				final JSONArray arrayDetail = jsonObject.isNull("arrayDetail") ? new JSONArray()
						: jsonObject.getJSONArray("arrayDetail");

				if (jsonObject.isNull("arrayDetail")) {
					jsonObject.put("arrayDetail", arrayDetail);
				}

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);

				final MyDetail detail = new MyDetail();
				row.appendChild(detail);
				detail.setOpen(true);
				final Label hargaTotal = new Label(Common.numberFormat.get().format(hrg));
				final AmbilDataKomponenProdukKursusBanbox komponenProdukKursusBanbox = new AmbilDataKomponenProdukKursusBanbox();

				final EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Double hrg = 0.0;
						for (int i = 0; i < arrayDetail.length(); i++) {
							JSONObject jsonObject1 = arrayDetail.getJSONObject(i);
							Long key = null;
							if (!jsonObject1.isNull("key")) {
								key = ais.common.CommonJSONUtil.ambilLong(jsonObject1,"key");
							}

							if (key != null) {

								Double harga = jsonObject1.isNull("harga") ? 0.0 : jsonObject1.getDouble("harga");

								hrg += harga;

							}
						}

						KomponenProdukKursus komponenProdukKursusPilih = (KomponenProdukKursus) komponenProdukKursusBanbox
								.getAttribute("komponenProdukKursus");

						jsonObject.put("komponenProdukKursus",
								komponenProdukKursusPilih == null ? null : komponenProdukKursusPilih.getId());
						jsonObject.put("harga", hrg);

						jsonObject.put("hargaTotal", (hrg));

						hargaTotal.setValue(Common.numberFormat.get().format(hrg));

						eventListenerdata.onEvent(null);
					}
				};

				int size = reloadDetail(eventListener, detail, arrayDetail, komponenProdukKursus,
						komponenProdukKursusBiaya);

				komponenProdukKursusBanbox.setAttribute("komponenProdukKursus", komponenProdukKursusBiaya);
				komponenProdukKursusBanbox
						.setValue(komponenProdukKursusBiaya == null ? "" : komponenProdukKursusBiaya.toString());
				komponenProdukKursusBanbox.setWidth("95%");
				komponenProdukKursusBanbox.setDisabled(size > 0);

				if (size > 0) {
					row.appendChild(new Label(komponenProdukKursusBiaya == null ? "" : komponenProdukKursusBiaya.getNama()));
				} else {
					row.appendChild(komponenProdukKursusBanbox);
				}

				row.appendChild(hargaTotal);

				komponenProdukKursusBanbox.setEventListener(eventListener);

				Hbox hbox = new Hbox();
				hbox.setParent(row);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/addthis.svg");
				button.setTooltiptext("Tambah Komponen");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						JSONObject jsonObject = new JSONObject();
						Long key = Math.abs(Common.randLong());
						jsonObject.put("key", key);

						arrayDetail.put(jsonObject);

						reloadDataFormula(rowU, array);
					}
				});

				button.setParent(hbox);

				button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {
												array.put(index, new JSONObject());

												reloadDataFormula(rowU, array);

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(
														"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
																+ e.getMessage());
											}

										}

									}
								});

					}
				});

				button.setParent(hbox);
			}
		}
	}

}

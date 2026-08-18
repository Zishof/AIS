package ais.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Box;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class InitComboUtil {

	// ========================================================================
	// HELPER METHODS (Mencegah NPE dan Mengurangi Duplikasi)
	// ========================================================================

	private static Fakultas getFakultasUser(Tbmuser user) {
		if (user == null)
			return null;
		if (user.ambilFakultas() != null)
			return user.ambilFakultas();
		if (user.getMahasiswa() != null && user.getMahasiswa().getJurusan() != null) {
			return user.getMahasiswa().getJurusan().getFakultas();
		}
		return null;
	}

	private static Jurusan getJurusanUser(Tbmuser user) {
		if (user == null)
			return null;
		if (user.ambilJurusan() != null)
			return user.ambilJurusan();
		if (user.getMahasiswa() != null)
			return user.getMahasiswa().getJurusan();
		return null;
	}

	private static Yayasan getYayasanUser(Tbmuser user) {
		if (user == null)
			return null;
		if (user.ambilYayasan() != null)
			return user.ambilYayasan();
		if (user.getSiswa() != null && user.getSiswa().getSekolah() != null) {
			return user.getSiswa().getSekolah().getYayasan();
		}
		return null;
	}

	private static Sekolah getSekolahUser(Tbmuser user) {
		if (user == null)
			return null;
		if (user.ambilSekolah() != null)
			return user.ambilSekolah();
		if (user.getSiswa() != null)
			return user.getSiswa().getSekolah();
		return null;
	}

	private static Criterion getAktifCriterion() {
		return Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true));
	}

	private static Criterion getPTCriterion(PerguruanTinggi pt) {
		if (pt == null || pt.getId() == null || pt.getFeeder() == null) {
			return Restrictions.sqlRestriction("true");
		}
		return Restrictions.or(Restrictions.isNull("perguruanTinggi"), Restrictions.eq("perguruanTinggi", pt));
	}

	private static Criterion getPTFakultasCriterion(PerguruanTinggi pt) {
		if (pt == null || pt.getId() == null || pt.getFeeder() == null) {
			return Restrictions.sqlRestriction("true");
		}
		return Restrictions.sqlRestriction(
				"this_.fakultas in (select id from fakultas where perguruan_tinggi=" + pt.getId() + ")");
	}


	private static void setReadonlySafe(Combobox combo, boolean readonly) {
		if (combo != null) {
			combo.setReadonly(readonly);
		}
	}

	private static void setDisabledSafe(Combobox combo, boolean disabled) {
		if (combo != null) {
			combo.setDisabled(disabled);
		}
	}

	private static void hideComboAndParent(Combobox combo) {
		if (combo == null) {
			return;
		}
		combo.setVisible(false);
		if (combo.getParent() != null) {
			combo.getParent().setVisible(false);
		}
	}

	private static void showComboAndParent(Combobox combo) {
		if (combo == null) {
			return;
		}
		combo.setVisible(true);
		if (combo.getParent() != null) {
			combo.getParent().setVisible(true);
		}
	}

	private static void clearComboSafe(Combobox combo) {
		if (combo == null) {
			return;
		}
		Common.clear(combo);
		combo.setSelectedItem(null);
	}

	private static void selectComboSafe(Combobox combo, Object value, boolean tambahJikaTidakAda) {
		if (combo == null) {
			return;
		}
		if (value != null) {
			Common.selectComboItem(tambahJikaTidakAda, combo, value);
		} else if (!combo.isDisabled()) {
			Common.selectComboItem(combo, null);
		}
	}

	private static Object getSingleRealComboValue(Combobox combo) {
		if (combo == null) {
			return null;
		}
		Object value = null;
		int count = 0;
		List<Comboitem> items = combo.getItems();
		for (Comboitem item : items) {
			if (item != null && item.getValue() != null) {
				value = item.getValue();
				count++;
				if (count > 1) {
					return null;
				}
			}
		}
		return count == 1 ? value : null;
	}

	private static Object autoSelectSingleRealItem(Combobox combo, boolean disableAfterSelected) {
		Object value = getSingleRealComboValue(combo);
		if (value != null) {
			selectComboSafe(combo, value, true);
			if (disableAfterSelected) {
				setDisabledSafe(combo, true);
			}
		}
		return value;
	}

	private static void insertJurusanByFakultas(Combobox combo, PerguruanTinggi pt, Fakultas fakultas,
			String labelTidakDipilih) {
		if (combo == null) {
			return;
		}
		clearComboSafe(combo);
		if (fakultas != null) {
			Common.insertComboDanSemua(combo, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					labelTidakDipilih, getAktifCriterion(), getPTFakultasCriterion(pt), Restrictions.eq("fakultas", fakultas));
		} else {
			Common.insertComboDanSemua(combo, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					labelTidakDipilih, getAktifCriterion(), getPTFakultasCriterion(pt));
		}
	}

	private static void insertSekolahByYayasan(Combobox combo, Yayasan yayasan, String labelTidakDipilih) {
		if (combo == null) {
			return;
		}
		clearComboSafe(combo);
		if (yayasan != null) {
			Common.insertComboDanSemua(combo, new String[] { "nama", "jenisSekolah" }, "yayasan", Sekolah.class,
					labelTidakDipilih, getAktifCriterion(), Restrictions.eq("yayasan", yayasan));
		} else {
			Common.insertComboDanSemua(combo, new String[] { "nama", "jenisSekolah" }, "yayasan", Sekolah.class,
					labelTidakDipilih, getAktifCriterion());
		}
	}

	/**
	 * Memuat daftar Sekolah milik {@code yayasan} ke dalam combo {@code sekolah}.
	 *
	 * <p>Dipakai oleh form yang memilih Yayasan SECARA PROGRAMATIS (mis. saat membuka
	 * data lama / edit). {@code Common.selectComboItem} hanya men-set item terpilih dan
	 * TIDAK memicu event {@code onChange}, sehingga listener pengisi sekolah tidak jalan
	 * dan combo Sekolah jadi kosong. Pemanggil harus memuat sekolah sendiri lewat method
	 * ini SETELAH memilih yayasan, lalu baru memilih sekolah.</p>
	 *
	 * <p>Bila {@code yayasan} null, semua sekolah (aktif/aktif-null) dimuat.</p>
	 *
	 * @param sekolah combobox tujuan (boleh null → tidak melakukan apa-apa).
	 * @param yayasan yayasan penyaring; null = semua sekolah.
	 */
	public static void muatSekolahMilikYayasan(Combobox sekolah, Yayasan yayasan) {
		if (sekolah == null) {
			return;
		}
		insertSekolahByYayasan(sekolah, yayasan, "=" + Common.getBahasaConfig("sekolah") + "=");
	}

	/**
	 * Memuat daftar Jurusan milik {@code fakultas} ke dalam combo {@code jurusan} (analog
	 * {@link #muatSekolahMilikYayasan} untuk perguruan tinggi). Dipakai oleh form yang memilih
	 * Fakultas SECARA PROGRAMATIS (mis. saat membuka data lama / edit); karena
	 * {@code Common.selectComboItem} tidak memicu {@code onChange}, listener pengisi jurusan
	 * tidak jalan sehingga combo Jurusan tampil kosong walau Fakultas sudah terpilih. Method ini
	 * menyaring HANYA berdasarkan fakultas (bukan konteks perguruan tinggi aktif) agar tetap benar
	 * saat mengedit data lintas-PT. Bila {@code fakultas} null, semua jurusan (aktif) dimuat.
	 *
	 * @param jurusan combobox tujuan (boleh null → tidak melakukan apa-apa).
	 * @param fakultas fakultas penyaring; null = semua jurusan.
	 */
	public static void muatJurusanMilikFakultas(Combobox jurusan, Fakultas fakultas) {
		if (jurusan == null) {
			return;
		}
		clearComboSafe(jurusan);
		String label = "=" + Common.getBahasaConfig("Semua") + "=";
		if (fakultas != null) {
			Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					label, getAktifCriterion(), Restrictions.eq("fakultas", fakultas));
		} else {
			Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					label, getAktifCriterion());
		}
	}

	// ========================================================================
	// MAIN METHODS
	// ========================================================================

	public static void initFakultasDanJurusan(final Combobox fakultas, final Combobox jurusan,
			final Combobox searchfakultas, final Combobox searchjurusan) {
		initFakultasDanJurusanDanSemua(fakultas, jurusan, searchfakultas, searchjurusan);
	}

	public static void initFakultasDanJurusanDanSemua(final Combobox fakultas, final Combobox jurusan,
			final Combobox searchfakultas, final Combobox searchjurusan) {
		initFakultasDanJurusanDanSemua(fakultas, jurusan, searchfakultas, searchjurusan, true);
	}

	public static void initFakultasDanJurusanDanSemua(final Combobox fakultas, final Combobox jurusan,
			final Combobox searchfakultas, final Combobox searchjurusan, final boolean pilih) {

		try {
			final PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();
			final Tbmuser user = Common.getCurrentUser();
			final boolean isAdmin = Common.getApakahAdmin();

			/*
			 * Prioritas pilihan harus berasal langsung dari user login.
			 * Catatan: nama parameter combobox adalah "jurusan", jadi variabel model
			 * dibuat "selectedJurusan" agar tidak bentrok dan tetap Java 1.7 compatible.
			 */
			final Fakultas selectedFakultas = user == null ? null : user.ambilFakultas();
			final Jurusan selectedJurusan = user == null ? null : user.ambilJurusan();
			final Fakultas effectiveFakultas = selectedFakultas != null ? selectedFakultas : getFakultasUser(user);
			final Jurusan effectiveJurusan = selectedJurusan != null ? selectedJurusan : getJurusanUser(user);
			final String labelSemua = "=" + Common.getBahasaConfig("Semua") + "=";

			if (fakultas != null && jurusan != null) {
				setReadonlySafe(fakultas, true);
				setReadonlySafe(jurusan, true);
				setDisabledSafe(fakultas, false);
				setDisabledSafe(jurusan, false);

				Common.insertComboDanSemua(fakultas, new String[] { "nama", "kode" }, "perguruanTinggi", Fakultas.class,
						labelSemua, Restrictions.and(getPTCriterion(pt), getAktifCriterion()));

				fakultas.addEventListener("onChange", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						Fakultas selected = null;
						if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() instanceof Fakultas) {
							selected = (Fakultas) fakultas.getSelectedItem().getValue();
						}
						insertJurusanByFakultas(jurusan, pt, selected, labelSemua);
						selectComboSafe(jurusan, null, false);
					}
				});

				if (pilih && effectiveFakultas != null) {
					selectComboSafe(fakultas, effectiveFakultas, true);
				}

				Fakultas filterFakultas = null;
				if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() instanceof Fakultas) {
					filterFakultas = (Fakultas) fakultas.getSelectedItem().getValue();
				} else if (pilih && effectiveFakultas != null) {
					filterFakultas = effectiveFakultas;
				}

				insertJurusanByFakultas(jurusan, pt, filterFakultas, labelSemua);

				if (pilih && effectiveJurusan != null) {
					selectComboSafe(jurusan, effectiveJurusan, true);
				}

				if (pilih && effectiveFakultas != null && !isAdmin && fakultas.getSelectedItem() != null
						&& fakultas.getSelectedItem().getValue() != null) {
					setDisabledSafe(fakultas, true);
				}
				if (pilih && effectiveJurusan != null && !isAdmin && jurusan.getSelectedItem() != null
						&& jurusan.getSelectedItem().getValue() != null) {
					setDisabledSafe(jurusan, true);
				}

				if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
					selectComboSafe(jurusan, null, false);
				}
			}

			if (searchfakultas != null && searchjurusan != null) {
				setReadonlySafe(searchfakultas, true);
				setReadonlySafe(searchjurusan, true);
				setDisabledSafe(searchfakultas, false);
				setDisabledSafe(searchjurusan, false);

				Common.insertComboDanSemua(searchfakultas, new String[] { "nama", "kode" }, "perguruanTinggi",
						Fakultas.class, labelSemua, Restrictions.and(getPTCriterion(pt), getAktifCriterion()));

				searchfakultas.addEventListener("onChange", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						Fakultas selected = null;
						if (searchfakultas.getSelectedItem() != null
								&& searchfakultas.getSelectedItem().getValue() instanceof Fakultas) {
							selected = (Fakultas) searchfakultas.getSelectedItem().getValue();
						}
						insertJurusanByFakultas(searchjurusan, pt, selected, labelSemua);
						selectComboSafe(searchjurusan, null, false);
					}
				});

				if (pilih && effectiveFakultas != null) {
					selectComboSafe(searchfakultas, effectiveFakultas, true);
				}

				Fakultas filterSearchFakultas = null;
				if (searchfakultas.getSelectedItem() != null && searchfakultas.getSelectedItem().getValue() instanceof Fakultas) {
					filterSearchFakultas = (Fakultas) searchfakultas.getSelectedItem().getValue();
				} else if (pilih && effectiveFakultas != null) {
					filterSearchFakultas = effectiveFakultas;
				}

				insertJurusanByFakultas(searchjurusan, pt, filterSearchFakultas, labelSemua);

				if (pilih && effectiveJurusan != null) {
					selectComboSafe(searchjurusan, effectiveJurusan, true);
				}

				if (pilih && effectiveFakultas != null && !isAdmin && searchfakultas.getSelectedItem() != null
						&& searchfakultas.getSelectedItem().getValue() != null) {
					setDisabledSafe(searchfakultas, true);
				}
				if (pilih && effectiveJurusan != null && !isAdmin && searchjurusan.getSelectedItem() != null
						&& searchjurusan.getSelectedItem().getValue() != null) {
					setDisabledSafe(searchjurusan, true);
				}

				if (pilih && (searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null)) {
					selectComboSafe(searchjurusan, null, false);
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}


	public static void initYayasanDanSekolahDanSemua(Combobox yayasan, Combobox sekolah, Combobox searchyayasan,
			Combobox searchsekolah) {
		initYayasanDanSekolahDanSemua(yayasan, sekolah, searchyayasan, searchsekolah, true, false);
	}

	public static void initYayasanDanSekolahDanSemua(final Combobox yayasan, final Combobox sekolah,
			final Combobox searchyayasan, final Combobox searchsekolah, final boolean pilih,
			final boolean otomatisPilih) {

		try {
			/*
			 * Prioritas utama harus mengikuti konteks sekolah/yayasan aktif.
			 * Pemilihan dilakukan langsung setelah isi combo dimuat, tanpa timer tambahan.
			 */
			final Sekolah selectedSekolah = SekolahUtil.getSekolah();
			final Yayasan selectedYayasanFromContext = SekolahUtil.getYayasan();
			final Yayasan selectedYayasan = selectedSekolah != null && selectedSekolah.getYayasan() != null
					? selectedSekolah.getYayasan()
					: selectedYayasanFromContext;
			final Tbmuser user = Common.getCurrentUser();
			final boolean isAdmin = Common.getApakahAdmin();
			final String labelYayasan = "=" + Common.getBahasaConfig("yayasan") + "=";
			final String labelSekolah = "=" + Common.getBahasaConfig("sekolah") + "=";

			final Yayasan userYayasan = selectedYayasan != null ? selectedYayasan : getYayasanUser(user);
			final Sekolah userSekolah = selectedSekolah != null ? selectedSekolah : getSekolahUser(user);

			if (yayasan != null && sekolah != null) {
				setReadonlySafe(yayasan, true);
				setReadonlySafe(sekolah, true);
				setDisabledSafe(yayasan, false);
				setDisabledSafe(sekolah, false);

				if (selectedSekolah != null && selectedSekolah.getId() != null) {
					selectComboSafe(yayasan, selectedYayasan, true);
					selectComboSafe(sekolah, selectedSekolah, true);
					if (pilih) {
						hideComboAndParent(yayasan);
						hideComboAndParent(sekolah);
					}
				} else if (selectedYayasan != null && selectedYayasan.getId() != null) {
					selectComboSafe(yayasan, selectedYayasan, true);
					insertSekolahByYayasan(sekolah, selectedYayasan, labelSekolah);
					if (pilih) {
						hideComboAndParent(yayasan);
					}
					if (userSekolah != null) {
						selectComboSafe(sekolah, userSekolah, true);
					}
				} else {
					showComboAndParent(yayasan);
					showComboAndParent(sekolah);

					Common.insertComboDanSemua(yayasan, new String[] { "nama", "keterangan" }, "alamat",
							Yayasan.class, labelYayasan, getAktifCriterion());

					yayasan.addEventListener("onChange", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							Yayasan selected = null;
							if (yayasan.getSelectedItem() != null && yayasan.getSelectedItem().getValue() instanceof Yayasan) {
								selected = (Yayasan) yayasan.getSelectedItem().getValue();
							}
							insertSekolahByYayasan(sekolah, selected, labelSekolah);
							selectComboSafe(sekolah, null, false);
						}
					});

					if (pilih && userYayasan != null) {
						selectComboSafe(yayasan, userYayasan, true);
					} else if (otomatisPilih) {
						Object autoYayasan = autoSelectSingleRealItem(yayasan, !isAdmin);
						if (autoYayasan instanceof Yayasan) {
							insertSekolahByYayasan(sekolah, (Yayasan) autoYayasan, labelSekolah);
						}
					}

					Yayasan filterYayasan = null;
					if (yayasan.getSelectedItem() != null && yayasan.getSelectedItem().getValue() instanceof Yayasan) {
						filterYayasan = (Yayasan) yayasan.getSelectedItem().getValue();
					} else if (pilih && userYayasan != null) {
						filterYayasan = userYayasan;
					}

					if (sekolah.getChildren().isEmpty()) {
						insertSekolahByYayasan(sekolah, filterYayasan, labelSekolah);
					}

					if (pilih && userSekolah != null) {
						selectComboSafe(sekolah, userSekolah, true);
					} else if (otomatisPilih) {
						autoSelectSingleRealItem(sekolah, !isAdmin);
					}

					if (pilih && userYayasan != null && !isAdmin && yayasan.getSelectedItem() != null
							&& yayasan.getSelectedItem().getValue() != null) {
						setDisabledSafe(yayasan, true);
					}
					if (pilih && userSekolah != null && !isAdmin && sekolah.getSelectedItem() != null
							&& sekolah.getSelectedItem().getValue() != null) {
						setDisabledSafe(sekolah, true);
					}

					if (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null) {
						selectComboSafe(sekolah, null, false);
					}
				}
			}

			if (searchyayasan != null && searchsekolah != null) {
				setReadonlySafe(searchyayasan, true);
				setReadonlySafe(searchsekolah, true);
				setDisabledSafe(searchyayasan, false);
				setDisabledSafe(searchsekolah, false);

				if (selectedSekolah != null && selectedSekolah.getId() != null) {
					selectComboSafe(searchyayasan, selectedYayasan, true);
					selectComboSafe(searchsekolah, selectedSekolah, true);
					if (!isAdmin) {
						setDisabledSafe(searchyayasan, true);
						setDisabledSafe(searchsekolah, true);
					}
				} else if (selectedYayasan != null && selectedYayasan.getId() != null) {
					selectComboSafe(searchyayasan, selectedYayasan, true);
					insertSekolahByYayasan(searchsekolah, selectedYayasan, labelSekolah);
					if (!isAdmin) {
						setDisabledSafe(searchyayasan, true);
					}
					if (userSekolah != null) {
						selectComboSafe(searchsekolah, userSekolah, true);
						if (!isAdmin) {
							setDisabledSafe(searchsekolah, true);
						}
					}
				} else {
					Common.insertComboDanSemua(searchyayasan, new String[] { "nama", "keterangan" }, "alamat",
							Yayasan.class, labelYayasan, getAktifCriterion());

					searchyayasan.addEventListener("onChange", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							Yayasan selected = null;
							if (searchyayasan.getSelectedItem() != null
									&& searchyayasan.getSelectedItem().getValue() instanceof Yayasan) {
								selected = (Yayasan) searchyayasan.getSelectedItem().getValue();
							}
							insertSekolahByYayasan(searchsekolah, selected, labelSekolah);
							selectComboSafe(searchsekolah, null, false);
						}
					});

					if (pilih && userYayasan != null) {
						selectComboSafe(searchyayasan, userYayasan, true);
					} else if (otomatisPilih) {
						Object autoYayasan = autoSelectSingleRealItem(searchyayasan, !isAdmin);
						if (autoYayasan instanceof Yayasan) {
							insertSekolahByYayasan(searchsekolah, (Yayasan) autoYayasan, labelSekolah);
						}
					}

					Yayasan filterSearchYayasan = null;
					if (searchyayasan.getSelectedItem() != null
							&& searchyayasan.getSelectedItem().getValue() instanceof Yayasan) {
						filterSearchYayasan = (Yayasan) searchyayasan.getSelectedItem().getValue();
					} else if (pilih && userYayasan != null) {
						filterSearchYayasan = userYayasan;
					}

					if (searchsekolah.getChildren().isEmpty()) {
						insertSekolahByYayasan(searchsekolah, filterSearchYayasan, labelSekolah);
					}

					if (pilih && userSekolah != null) {
						selectComboSafe(searchsekolah, userSekolah, true);
					} else if (otomatisPilih) {
						autoSelectSingleRealItem(searchsekolah, !isAdmin);
					}

					if (pilih && userYayasan != null && !isAdmin && searchyayasan.getSelectedItem() != null
							&& searchyayasan.getSelectedItem().getValue() != null) {
						setDisabledSafe(searchyayasan, true);
					}
					if (pilih && userSekolah != null && !isAdmin && searchsekolah.getSelectedItem() != null
							&& searchsekolah.getSelectedItem().getValue() != null) {
						setDisabledSafe(searchsekolah, true);
					}

					if (pilih && (searchsekolah.getSelectedItem() == null
							|| searchsekolah.getSelectedItem().getValue() == null)) {
						selectComboSafe(searchsekolah, null, false);
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}


	public static void initJurusanDanSemua(final Combobox jurusan, final Jenjang jenjang) {
		initJurusanDanSemua(jurusan, jenjang, "== Klik disini untuk pilih ==");
	}

	public static void initJurusanDanSemua(final Combobox jurusan, final Jenjang jenjang, String label) {
		try {
			if (jurusan != null) {
				Criterion filterJenjang = (jenjang == null) ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jenjang", jenjang);

				Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						label, Restrictions.and(Restrictions.eq("aktif", true), filterJenjang));

				Tbmuser user = Common.getCurrentUser();
				Jurusan jUser = getJurusanUser(user);

				if (user != null) {
					if (jUser != null) {
						Common.pilihJurusan(jurusan, jUser);
						jurusan.setDisabled(true);
					} else {
						jurusan.setDisabled(false);
					}

					if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
						Common.pilihJurusan(jurusan, null);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitComboUtil.java:643");
		} finally {
			// TODO: Tutup session di sini
		}
	}

	// ========================================================================
	// PUBLIC METHODS (Wrapper untuk handling Session / Finally block)
	// ========================================================================

	public static void freeze(Component comp, boolean freeze) {
		try {
			doFreeze(comp, freeze);
		} finally {
			// TODO: Tutup session di sini
			// Contoh: HibernateUtil.getSessionFactory().getCurrentSession().close();
		}
	}

	public static void freezeGanti(Component... components) {
		try {
			for (Component component : components) {
				if (component.getAttribute("janganDisabled") != null) {
					continue;
				}
				try {
					if (component instanceof Box) {
						doFreeze(component, true);
					} else {
						doReplaceWithLabel(component);
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitComboUtil.java:675"); // Cegah error tertelan
				}
			}
		} finally {
			// TODO: Tutup session di sini
		}
	}

	public static void freezeGanti(Component comp, boolean freeze) {
		try {
			doFreezeGanti(comp, freeze);
		} finally {
			// TODO: Tutup session di sini
		}
	}

	public static void masukkanListener(Component comp, EventListener eventListener) {
		try {
			doMasukkanListener(comp, eventListener);
		} finally {
			// TODO: Tutup session di sini
		}
	}

	// ========================================================================
	// PRIVATE RECURSIVE METHODS (Logika Inti)
	// ========================================================================

	@SuppressWarnings("unchecked")
	private static void doFreeze(Component comp, boolean freeze) {
		if (comp == null || comp.getChildren() == null)
			return;

		List<Component> components = comp.getChildren();
		for (Component component : components) {
			if (component.getAttribute("janganDisabled") != null)
				continue;

			// Casting aman karena ZK InputElement/Disable handling
			if (component instanceof Textbox)
				((Textbox) component).setDisabled(freeze);
			else if (component instanceof Timebox)
				((Timebox) component).setDisabled(freeze);
			else if (component instanceof Radio)
				((Radio) component).setDisabled(freeze);
			else if (component instanceof Checkbox)
				((Checkbox) component).setDisabled(freeze);
			else if (component instanceof Doublebox)
				((Doublebox) component).setDisabled(freeze);
			else if (component instanceof Combobox)
				((Combobox) component).setDisabled(freeze);
			else if (component instanceof Intbox)
				((Intbox) component).setDisabled(freeze);
			else if (component instanceof Datebox)
				((Datebox) component).setDisabled(freeze);
			else if (component instanceof Bandbox)
				((Bandbox) component).setDisabled(freeze);
			else if (component instanceof Button)
				((Button) component).setDisabled(freeze);
			else if (component instanceof Toolbarbutton)
				((Toolbarbutton) component).setDisabled(freeze);
			else if (component instanceof Decimalbox)
				((Decimalbox) component).setDisabled(freeze);
			else
				doFreeze(component, freeze); // Teruskan rekursif
		}
	}

	@SuppressWarnings("unchecked")
	private static void doFreezeGanti(Component comp, boolean freeze) {
		if (comp == null || comp.getChildren() == null)
			return;

		// PENTING: Gunakan ArrayList baru untuk menghindari
		// ConcurrentModificationException
		// karena kita memanipulasi susunan (menghapus) UI component saat proses
		// iterasi.
		List<Component> components = new ArrayList<Component>(comp.getChildren());

		for (Component component : components) {
			if (component.getAttribute("janganDisabled") != null)
				continue;

			try {
				if (component instanceof Box) {
					doFreeze(component, true); // Perbaikan bug: sebelumnya doFreeze(comp)
				} else if (!doReplaceWithLabel(component)) {
					doFreezeGanti(component, freeze); // Teruskan rekursif jika belum diganti
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitComboUtil.java:765");
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void doMasukkanListener(Component comp, EventListener eventListener) {
		if (comp == null || comp.getChildren() == null)
			return;

		List<Component> components = comp.getChildren();
		for (Component component : components) {
			// MyCkEditor punya struktur berbeda
			if (component instanceof MyCkEditor) {
				((MyCkEditor) component).cKeditor.addEventListener("onChange", eventListener);
			}
			// Kelompok Komponen Input (On Change) -> Tidak perlu casting spesifik
			else if (component instanceof Textbox || component instanceof Doublebox || component instanceof Combobox
					|| component instanceof Intbox || component instanceof Datebox || component instanceof Bandbox
					|| component instanceof Decimalbox) {
				component.addEventListener("onChange", eventListener);
			}
			// Kelompok Komponen Klik (On Click)
			else if (component instanceof Radio || component instanceof Checkbox
					|| component instanceof MyCheckboxConfig || component instanceof MyRadioConfig
					|| component instanceof MyButtonConfig || component instanceof MyToolbarbuttonConfig) {
				component.addEventListener("onClick", eventListener);
			}
			// Teruskan rekursif
			else {
				doMasukkanListener(component, eventListener);
			}
		}
	}

	// ========================================================================
	// HELPER METHOD PENGGANTI KOMPONEN (Mengurangi Duplikasi)
	// ========================================================================

	/**
	 * Mengganti input dengan teks label static. Returns true jika berhasil
	 * dikonversi, false jika tidak masuk kategori.
	 */
	private static boolean doReplaceWithLabel(Component component) {
		Component parent = component.getParent();
		if (parent == null)
			return false;

		String s = "";
		Component newComp = null;

		if (component instanceof MyCkEditor) {
			s = ((MyCkEditor) component).getValue();
			newComp = new Html(s == null ? "" : s);
		} else if (component instanceof Textbox) {
			s = ((Textbox) component).getValue();
			newComp = new Label(s == null ? "" : s);
		} else if (component instanceof Radiogroup) {
			Radio c = ((Radiogroup) component).getSelectedItem();
			String val = (c == null || c.getValue() == null) ? "" : c.getValue().toString();
			String lbl = (c == null || c.getLabel() == null || c.getLabel().trim().isEmpty()) ? ""
					: c.getLabel() + " = ";
			newComp = new Label(lbl + val);
		} else if (component instanceof Checkbox) {
			Checkbox cb = (Checkbox) component;
			String val = cb.isChecked() ? "Ya" : "Tidak";
			String lbl = (cb.getLabel() == null || cb.getLabel().trim().isEmpty()) ? "" : cb.getLabel() + " = ";
			newComp = new Label(lbl + val);
		} else if (component instanceof Doublebox) {
			Double val = ((Doublebox) component).getValue();
			s = (val == null) ? "" : Common.numberFormat.get().format(val);
			newComp = new Label(s);
		} else if (component instanceof Timebox) {
			Date val = ((Timebox) component).getValue();
			s = (val == null) ? "" : Common.timeFormat.get().format(val);
			newComp = new Label(s);
		} else if (component instanceof Combobox) {
			Comboitem item = ((Combobox) component).getSelectedItem();
			s = (item == null || item.getValue() == null) ? "" : item.getValue().toString();
			newComp = new Label(s);
		} else if (component instanceof Intbox) {
			Integer val = ((Intbox) component).getValue();
			s = (val == null) ? "" : Common.numberFormat.get().format(val);
			newComp = new Label(s);
		} else if (component instanceof Datebox) {
			Date val = ((Datebox) component).getValue();
			s = (val == null) ? "" : Common.dateFormat.get().format(val).replaceAll("00:00", "");
			newComp = new Label(s);
		} else if (component instanceof Button) {
			component.setVisible(false);
			return true; // Selesai diproses tanpa ditimpa Label
		} else {
			return false; // Bukan komponen target
		}

		if (newComp != null) {
			parent.removeChild(component);
			newComp.setParent(parent);
		}

		return true;
	}
}

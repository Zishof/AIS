package ais.action.master.sekolah.psb.nis;

import java.util.List;

import ais.database.model.sekolah.CalonSiswa;

public interface NisGenerator {

	public String generateNis(CalonSiswa calonSiswa);

	public String generateNis(CalonSiswa calonSiswa, List<String> nimPengecualian);

}

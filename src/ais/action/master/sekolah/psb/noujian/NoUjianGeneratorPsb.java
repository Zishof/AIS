package ais.action.master.sekolah.psb.noujian;

import java.util.List;

import ais.database.model.sekolah.CalonSiswa;

public interface NoUjianGeneratorPsb {

	public String generateNoUjian(CalonSiswa calonSiswa)
			throws Exception;

	public String generateNoUjian(CalonSiswa calonSiswa,
			List<String> noRegPengecualian) throws Exception;

}

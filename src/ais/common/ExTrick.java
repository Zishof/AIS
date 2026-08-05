package ais.common;

public class ExTrick {
	public static void main(String args[]) {
		new ExTrick().callMeAnyTime();
	}

	void callMeAnyTime() {
		try {
			throw new Exception("Who called me?");
		} catch (Exception e) {
			System.out.println("I was called by "
					+ e.getStackTrace()[1].getClassName() + "."
					+ e.getStackTrace()[1].getMethodName() + "()!");
		}
	}
}

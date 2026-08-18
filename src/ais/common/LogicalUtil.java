package ais.common;

import java.util.ArrayList;
import java.util.List;

import net.objecthunter.exp4j.function.Function;
import net.objecthunter.exp4j.operator.Operator;

public class LogicalUtil {

	/**
	 * Logical "if" function.
	 */
	private static final Function IF = new Function("if", 3) { // named "if", takes 3 arguments

		@Override
		public double apply(double... args) {
			if (((int) args[0]) == 1) {
				return args[1];
			} else {
				return args[2];
			}
		}
	};

	private static final Function UPPER = new Function("upper", 1) {

		@Override
		public double apply(double... args) {
			int hasil = (int) Math.ceil(args[0]);
			return hasil;
		}
	};

	private static final Function LOWER = new Function("lower", 1) {

		@Override
		public double apply(double... args) {
			int hasil = (int) Math.floor(args[0]);
			return hasil;
		}
	};

	private static final Function PEMBULATAN = new Function("round", 1) {

		@Override
		public double apply(double... args) {
			int hasil = (int) Math.round(args[0]);
			return hasil;
		}
	};

	private static final Function ROUNDUP1000 = new Function("roundup", 2) {

		@Override
		public double apply(double... args) {
			int hasil = (int) (Math.round(args[0] / args[1]) * args[1]);
			return hasil;
		}
	};

	private static final Function ROUNDDOWN1000 = new Function("rounddown", 2) {

		@Override
		public double apply(double... args) {
			int hasil = (int) (Math.floor(args[0] / args[1]) * args[1]);
			return hasil;
		}
	};

	/**
	 * Greater than operator.
	 */
	private static final Operator GT = new Operator(">", 2, true, Operator.PRECEDENCE_ADDITION - 1) {

		@Override
		public double apply(double... values) {
			return values[0] > values[1] ? 1d : 0d;
		}
	};

	/**
	 * Greater than operator.
	 */
	private static final Operator EQ = new Operator("=", 2, true, Operator.PRECEDENCE_ADDITION - 1) {

		@Override
		public double apply(double... values) {
			return values[0] == values[1] ? 1d : 0d;
		}
	};

	/**
	 * Greater than operator.
	 */
	private static final Operator LT = new Operator("<", 2, true, Operator.PRECEDENCE_ADDITION - 1) {

		@Override
		public double apply(double... values) {
			return values[0] < values[1] ? 1d : 0d;
		}
	};

	/**
	 * Greater than operator.
	 */
	private static final Operator GE = new Operator(">=", 2, true, Operator.PRECEDENCE_ADDITION - 1) {

		@Override
		public double apply(double... values) {
			return values[0] >= values[1] ? 1d : 0d;
		}
	};

	/**
	 * Greater than operator.
	 */
	private static final Operator LE = new Operator("<=", 2, true, Operator.PRECEDENCE_ADDITION - 1) {

		@Override
		public double apply(double... values) {
			return values[0] <= values[1] ? 1d : 0d;
		}
	};

	public static final List<Function> MAX = new ArrayList<Function>();
	public static final List<Function> MIN = new ArrayList<Function>();
	public static final List<Function> AVG = new ArrayList<Function>();
	public static final List<Function> SUM = new ArrayList<Function>();

	public static final List<Function> ALL_FUNCTION = new ArrayList<Function>();
	public static final List<Operator> ALL_OPERATOR = new ArrayList<Operator>();
	static {

		ALL_OPERATOR.add(GT);
		ALL_OPERATOR.add(LT);
		ALL_OPERATOR.add(GE);
		ALL_OPERATOR.add(LE);
		ALL_OPERATOR.add(EQ);

		for (int i = 1; i < 100; i++) {
			Function MAX3 = new Function("max" + i, i) {
				@Override
				public double apply(double... args) {
					double max = Double.MIN_VALUE;

					for (double arg : args) {
//						System.out.println("max arg = " + arg);
						if (max < arg) {
							max = arg;
						}
					}

					if (max == Double.MIN_VALUE) {
						max = 0L;
					}

					return args.length == 0 ? 0.0 : max;
				}
			};
			MAX.add(MAX3);
		}

		for (int i = 1; i < 100; i++) {
			Function MIN3 = new Function("min" + i, i) {
				@Override
				public double apply(double... args) {
					double max = Double.MAX_VALUE;

					for (double arg : args) {
//						System.out.println("min arg = " + arg);
						if (max > arg) {
							max = arg;
						}
					}

					if (max == Double.MAX_VALUE) {
						max = 0L;
					}

					return args.length == 0 ? 0.0 : max;
				}
			};
			MIN.add(MIN3);
		}

		for (int i = 1; i < 100; i++) {
			Function MIN3 = new Function("minnotnol" + i, i) {
				@Override
				public double apply(double... args) {
					double max = Double.MAX_VALUE;

					for (double arg : args) {
//						System.out.println("min arg = " + arg);
						if (arg > 0 && max > arg) {
							max = arg;
						}
					}

					if (max == Double.MAX_VALUE) {
						max = 0L;
					}

					return args.length == 0 ? 0.0 : max;
				}
			};
			MIN.add(MIN3);
		}

		for (int i = 1; i < 100; i++) {
			Function AVG3 = new Function("avg" + i, i) {

				@Override
				public double apply(double... args) {
					double sum = 0;
					for (double arg : args) {
						sum += arg;
					}
					return sum / args.length;
				}
			};
			AVG.add(AVG3);
		}

		for (int i = 1; i < 100; i++) {
			Function AVG3 = new Function("ratanotnol" + i, i) {

				@Override
				public double apply(double... args) {
					double sum = 0;
					for (double arg : args) {
						sum += arg;
					}

					int jml = 0;
					for (double arg : args) {
						try {
							if (arg > 0.1) {
								jml++;
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/LogicalUtil.java:242");
						}
					}

					return jml == 0 ? 0.0 : (sum / jml);
				}
			};
			AVG.add(AVG3);
		}

		for (int i = 1; i < 100; i++) {
			Function AVG3 = new Function("ratanotblmsetuju" + i, i * 2) {

				@Override
				public double apply(double... args) {
					double sum = 0;
					int size = args.length / 2;

					int index = 0;
					int jml = 0;
					for (double arg : args) {
						if (index < size && args[size + index] > 0) {
							sum += arg;
							jml++;
						}
						index++;
					}

					return jml == 0 ? 0.0 : (sum / jml);
				}
			};
			AVG.add(AVG3);
		}

		for (int i = 1; i < 100; i++) {
			Function SUM3 = new Function("sum" + i, i) {

				@Override
				public double apply(double... args) {
					double sum = 0;
					for (double arg : args) {
						sum += arg;
					}
					return sum;
				}
			};
			SUM.add(SUM3);
		}

		ALL_FUNCTION.addAll(MAX);
		ALL_FUNCTION.addAll(MIN);
		ALL_FUNCTION.addAll(AVG);
		ALL_FUNCTION.addAll(SUM);
		ALL_FUNCTION.add(IF);
		ALL_FUNCTION.add(UPPER);
		ALL_FUNCTION.add(LOWER);
		ALL_FUNCTION.add(PEMBULATAN);

		ALL_FUNCTION.add(ROUNDDOWN1000);

		ALL_FUNCTION.add(ROUNDUP1000);
	}

}

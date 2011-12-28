package org.hydra.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.hydra.util.Lists.MapFunc;
import org.hydra.util.Lists.Pair;
import org.hydra.util.Lists.Predicate;
import org.objectweb.asm.ClassReader;

public class ExtractorMain {
	public static void main(String[] args) {
		String dir = "/Users/argan/Opensource/myown/binary-refactor/jars";
		File[] jarFiles = new File(dir).listFiles(new FilenameFilter() {

			public boolean accept(File arg0, String arg1) {
				return arg1.matches(".+jar$");
			}
		});

		String jar2 = "/Users/argan/crack/jrebel-4.0/jrebel/jrebel.jar";

		try {
			List<ClassSignature> result2 = extract(new File(jar2));
			result2 = filter(result2);
			for (File jar : jarFiles) {
				List<ClassSignature> result = extract(jar);

				result = filter(result);

				// print(result);
				System.out.println("========== " + jar.getName());
				// print(result2);

				find(result2, result);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static List<ClassSignature> filter(List<ClassSignature> result) {
		result = Lists.filter(result, new Predicate<ClassSignature>() {

			public boolean apply(ClassSignature t) {
				return !Types.isUnchangableType(t.getName()) && t.getName().indexOf("[") == -1;
				// && (t.getName().startsWith("org.") ||
				// t.getName().startsWith("com.zero")
			}
		});
		return result;
	}

	/**
	 * 找出完全匹配的集合
	 * 
	 * @param result
	 *            已知的集合
	 * @param result2
	 *            待分析的集合
	 */
	private static void find(List<ClassSignature> result, List<ClassSignature> result2) {
		List<Pair<String, ClassSignature>> level0Sig = Lists.map(result2,
		        new MapFunc<ClassSignature, Lists.Pair<String, ClassSignature>>() {

			        public Pair<String, ClassSignature> apply(ClassSignature in) {
				        return new Pair<String, ClassSignature>(in.getLevel0Sig(), in);
			        }
		        });
		int count = 0;
		for (ClassSignature c : result) {
			final String sig = c.getLevel0Sig();
			List<Pair<String, ClassSignature>> matches = Lists.filter(level0Sig,
			        new Predicate<Pair<String, ClassSignature>>() {

				        public boolean apply(Pair<String, ClassSignature> in) {
					        return in.getLeft().equals(sig);
				        }
			        });
			if (matches.size() == 1) {
				count++;
				System.out.println(c.getName() + " = " + matches.get(0).getRight().getName());
				matchMethodsAndFields(c, matches.get(0).getRight());
			} else if (matches.size() > 1) {
				// System.out.println(c.getName()+ " matches multi classes." );
				// for (Pair<String, ClassSignature> p : matches) {
				// System.out.println(">>>" + p.getRight().getName());
				// }
				// System.out.println();
			}
		}
		System.out.println("Match count:" + count);
	}

	/**
	 * 将method等的对应关系找出来
	 * 
	 * @param c
	 * @param pair
	 */
	private static void matchMethodsAndFields(ClassSignature c, ClassSignature pair) {
		for (int i = 0; i < c.getFields().size(); i++) {
			FieldSignature f1 = c.getFields().get(i);
			FieldSignature f2 = pair.getFields().get(i);
			assert f1.getOriginDesc() == f2.getOriginDesc();
			System.out.println("\tf " + f1.getName() + " " + f1.getOriginDesc() + " = " + f2.getName() + " "
			        + f2.getOriginDesc());
		}
		for (int i = 0; i < c.getMethods().size(); i++) {
			MethodSignature m1 = c.getMethods().get(i);
			MethodSignature m2 = pair.getMethods().get(i);
			assert m1.getOriginDesc() == m2.getOriginDesc();
			System.out.println("\tm " + m1.getName() + " " + m1.getOriginDesc() + " = " + m2.getName());
		}
		// comment old version
		// for (MethodSignature ms : c.getMethods()) {
		// final String sig = ms.getLevel0Sig();
		// List<MethodSignature> matches = Lists.filter(pair.getMethods(), new
		// Predicate<MethodSignature>() {
		// public boolean apply(MethodSignature in) {
		// return sig.equals(in.getLevel0Sig());
		// }
		// });
		// if (matches.size() == 1) {
		// System.out.println("\t" + ms.getName() + " " + ms.getOriginDesc() +
		// " = " + matches.get(0).getName()
		// + " " + matches.get(0).getOriginDesc());
		// } else {
		// System.out.println("\t" + ms.getName() + " " + ms.getOriginDesc() +
		// " matches multi methods.");
		// }
		// }
	}

	private static void print(List<ClassSignature> result) {
		for (ClassSignature t : result) {
			if (!Types.isUnchangableType(t.getName()) && t.getName().indexOf("[") == -1) {
				System.out.println(t.getName());
				System.out.println(t.getLevel0Sig());
				System.out.println();
			}
		}
	}

	private static List<ClassSignature> extract(File jar) throws IOException {
		ClassSignatureExtractor extractor = new ClassSignatureExtractor();
		JarFile jarFile = new JarFile(jar);
		for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements();) {
			JarEntry entry = entries.nextElement();
			if (entry.getName().endsWith(".class")) {
				ClassReader reader = new ClassReader(jarFile.getInputStream(entry));
				reader.accept(extractor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
			}
		}
		// get the result
		Map<String, ClassSignature> result = extractor.getResult();
		return new ArrayList<ClassSignature>(result.values());
	}
}
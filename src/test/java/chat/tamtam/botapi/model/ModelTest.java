package chat.tamtam.botapi.model;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.junit.Test;

import com.jparams.verifier.tostring.NameStyle;
import com.jparams.verifier.tostring.ToStringVerifier;
import com.jparams.verifier.tostring.preset.IntelliJPreset;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author alexandrchuprin
 */
public class ModelTest {
    @Test
    public void testAllModels() throws Exception {
        for (Class aClass : getClasses("chat.tamtam.botapi.model")) {
            if (aClass.getName().endsWith("Test")) {
                continue;
            }

            if (aClass.isInterface()) {
                continue;
            }

            if (aClass.isAnonymousClass()) {
                continue;
            }

            String classPath = aClass.getProtectionDomain().getCodeSource().getLocation().getPath();
            if (classPath.contains("test-classes"))
                continue;

            if (aClass.isEnum()) {
                testEnum((Class<? extends Enum<?>>) aClass);
            }

            if (!aClass.isEnum()) {
                ToStringVerifier.forClass(aClass)
                        .withClassName(NameStyle.SIMPLE_NAME)
                        .withPreset(new IntelliJPreset())
                        .verify();
            }

            EqualsVerifier.forClass(aClass)
                    .suppress(Warning.NONFINAL_FIELDS, Warning.INHERITED_DIRECTLY_FROM_OBJECT)
                    .usingGetClass()
                    .verify();
        }
    }

    private void testEnum(Class<? extends Enum<?>> anEnum) {
        Enum<?>[] enumConstants = anEnum.getEnumConstants();
        for (Enum<?> enumConstant : enumConstants) {
            if (enumConstant instanceof TamTamEnum) {
                assertThat(TamTamEnum.create(enumConstant.getDeclaringClass(), ((TamTamEnum) enumConstant).getValue()), is(enumConstant));
            }
        }
    }

    private static Class[] getClasses(String packageName) throws IOException, ClassNotFoundException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }

        ArrayList<Class> classes = new ArrayList<>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }

        return classes.toArray(new Class[0]);
    }

    private static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }

        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                classes.add(
                        Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }

        return classes;
    }
}
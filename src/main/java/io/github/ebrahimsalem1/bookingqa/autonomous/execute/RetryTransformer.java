package io.github.ebrahimsalem1.bookingqa.autonomous.execute;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/** Attaches {@link TransientRetryAnalyzer} to every test, so no test has to remember to. */
public class RetryTransformer implements IAnnotationTransformer {

    @Override
    @SuppressWarnings("rawtypes")
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        annotation.setRetryAnalyzer(TransientRetryAnalyzer.class);
    }
}

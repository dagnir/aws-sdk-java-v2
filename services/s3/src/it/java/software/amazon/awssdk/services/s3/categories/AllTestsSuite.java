package software.amazon.awssdk.services.s3.categories;

import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.extensions.cpsuite.ClasspathSuite.ClassnameFilters;
import org.junit.runner.RunWith;

@RunWith(value = ClasspathSuite.class)
@ClassnameFilters("software.amazon.awssdk.services.s3.*")
public class AllTestsSuite {
}

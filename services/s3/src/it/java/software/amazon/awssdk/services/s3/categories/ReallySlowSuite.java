package software.amazon.awssdk.services.s3.categories;

import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Categories.class)
@IncludeCategory(S3Categories.ReallySlow.class)
@SuiteClasses(AllTestsSuite.class)
public class ReallySlowSuite {
}

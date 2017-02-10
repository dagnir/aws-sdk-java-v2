package software.amazon.awssdk.services.s3.categories;

import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(value = Categories.class)
@IncludeCategory(value = S3Categories.Slow.class)
@SuiteClasses(AllTestsSuite.class)
public class SlowSuite {
}

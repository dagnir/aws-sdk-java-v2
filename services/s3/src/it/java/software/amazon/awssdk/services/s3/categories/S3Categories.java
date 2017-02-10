package software.amazon.awssdk.services.s3.categories;

/**
 * Junit Test Categories for the S3 Client. Categories are just marker interfaces to group tests
 * under common criteria like fast or slow, unit or integration, feature a or feature b, etc
 */
public class S3Categories {

    /**
     * Marker category for slow tests. Slow is roughly defined as greater than 30 seconds and less
     * than five minutes
     */
    public interface Slow {
    }

    /**
     * Marker category for really slow tests. ReallySlow is roughly defined as greater than five
     * minutes
     */
    public interface ReallySlow {
    }
}
